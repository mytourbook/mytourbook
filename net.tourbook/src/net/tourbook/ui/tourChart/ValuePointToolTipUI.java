/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.ui.tourChart;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ColorCache;
import net.tourbook.chart.ITooltipOwner;
import net.tourbook.chart.IValuePointToolTip;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;
import net.tourbook.util.Util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * This tooltip is displayed when the mouse is hovered over a value point in a line graph and
 * displays value point information.
 */
public class ValuePointToolTipUI extends ValuePointToolTipShell implements IValuePointToolTip {

	private final IPreferenceStore			_prefStore						= TourbookPlugin
																					.getDefault()
																					.getPreferenceStore();

	private IPropertyChangeListener			_prefChangeListener;

	private TourData						_tourData;

	private ValuePointToolTipMenuManager	_ttMenuMgr;
	private ActionOpenTooltipMenu			_actionOpenTooltipMenu;

	private int								_devXMouse;
	private int								_devYMouse;

	private boolean							_isToolTipVisible;
	private int								_currentValueIndex;
	private int								_valueUnitDistance;

	private boolean							_isHorizontal;

	private final DateTimeFormatter			_dtFormatter					= DateTimeFormat.mediumDateTime();
	private final NumberFormat				_nf1							= NumberFormat.getNumberInstance();
	private final NumberFormat				_nf1NoGroup						= NumberFormat.getNumberInstance();
	private final NumberFormat				_nf3							= NumberFormat.getNumberInstance();
	private final NumberFormat				_nf3NoGroup						= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);

		_nf1NoGroup.setMinimumFractionDigits(1);
		_nf1NoGroup.setMaximumFractionDigits(1);
		_nf1NoGroup.setGroupingUsed(false);

		_nf3NoGroup.setMinimumFractionDigits(3);
		_nf3NoGroup.setMaximumFractionDigits(3);
		_nf3NoGroup.setGroupingUsed(false);
	}

	/**
	 * Contains all visible graph id's
	 */
	private int								_visibleGraphs;
	private int								_visibleGraphsCount;

	private int								_visibleAltimeterId;
	private int								_visibleAltitudeId;
	private int								_visibleCadenceId;
	private int								_visibleDistanceId;
	private int								_visibleGradientId;
	private int								_visiblePaceId;
	private int								_visiblePowerId;
	private int								_visiblePulseId;
	private int								_visibleSpeedId;
	private int								_visibleTemperatureId;
	private int								_visibleTimeDurationId;
	private int								_visibleTimeOfDayId;

	private int								_visibleTimeSliceId;

	/*
	 * UI resources
	 */
	private Color							_fgColor;
	private Color							_bgColor;
	private Color							_fgToolbar;
	private Color							_fgBorder;
//	private Font							_boldFont;
	private PixelConverter					_pc;
	private final ColorCache				_colorCache						= new ColorCache();
	private final GraphColorProvider		_colorProvider					= GraphColorProvider.getInstance();

	private final ArrayList<Control>		_firstColumnControls			= new ArrayList<Control>();
	private final ArrayList<Control>		_firstColumnContainerControls	= new ArrayList<Control>();

	/*
	 * UI controls
	 */
	private Composite						_shellContainer;
	private ToolBar							_toolbarControl;

	private Label							_lblDataSerieCurrent;
	private Label							_lblDataSerieMax;

	private Label							_lblAltimeter;
	private Label							_lblAltitude;
	private Label							_lblCadence;
	private Label							_lblDistance;
	private Label							_lblGradient;
	private Label							_lblPace;
	private Label							_lblPower;
	private Label							_lblPulse;
	private Label							_lblSpeed;
	private Label							_lblTemperature;
	private Label							_lblTimeDuration;
	private Label							_lblTimeOfDay;

	private Label							_lblAltitudeUnit;
	private Label							_lblAltimeterUnit;
	private Label							_lblDistanceUnit;
	private Label							_lblPaceUnit;
	private Label							_lblSpeedUnit;
	private Label							_lblTemperatureUnit;

	private class ActionOpenTooltipMenu extends Action {

		public ActionOpenTooltipMenu(final ValuePointToolTipMenuManager tooltipMenuManager) {
			super(null, Action.AS_PUSH_BUTTON);

			setToolTipText(Messages.Tooltip_ValuePoint_Action_OpenToolTipMenu_ToolTip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__tour_options));
		}

		@Override
		public void runWithEvent(final Event event) {
			_ttMenuMgr.openToolTipMenu(event, _tourData);
		}
	}

	public ValuePointToolTipUI(final ITooltipOwner tooltipOwner, final IDialogSettings state) {

		super(tooltipOwner, state);

		_isToolTipVisible = _prefStore.getBoolean(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE);

		addPrefListener();
	}

	void actionHideToolTip() {

		_prefStore.setValue(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE, false);

		_isToolTipVisible = false;

		hide();
	}

	void actionOrientation(final ValuePointToolTipOrientation orientation) {

		_isHorizontal = orientation == ValuePointToolTipOrientation.Horizontal;

		reopenTT();
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * create a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE)
				//
				) {
					_isToolTipVisible = (Boolean) event.getNewValue();

					if (_isToolTipVisible) {
						show(new Point(_devXMouse, _devYMouse));
					} else {
						hide();
					}

				} else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					// dispose old colors
					_colorCache.dispose();

					reopenTT();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void createActions() {

		_ttMenuMgr = new ValuePointToolTipMenuManager(this, state);

		_actionOpenTooltipMenu = new ActionOpenTooltipMenu(_ttMenuMgr);
	}

	@Override
	protected Composite createToolTipContentArea(final Event event, final Composite parent) {

		Composite shell;

		if (_tourData == null || _tourData.timeSerie == null || _tourData.timeSerie.length == 0) {

			// there are no data available

			shell = createUI999NoData(parent);

		} else {

			// tour data is available

			createActions();

			shell = createUI(parent);
		}

		return shell;
	}

	private Composite createUI(final Composite parent) {

		final Display display = parent.getDisplay();

		_bgColor = _colorCache.getColor(new RGB(0xff, 0xff, 0xf0));
		_fgBorder = _colorCache.getColor(new RGB(0xe5, 0xe5, 0xcb));
		_fgToolbar = _colorCache.getColor(new RGB(0xf7, 0xf7, 0xe5));
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
//		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		_pc = new PixelConverter(parent);

		_valueUnitDistance = _isHorizontal ? 2 : 5;

		_firstColumnControls.clear();
		_firstColumnContainerControls.clear();

		final Composite shell = createUI010Shell(parent);

		updateUI(_currentValueIndex, true);

		if (_isHorizontal == false) {

			// compute width for all controls and equalize column width for the different sections
			_shellContainer.layout(true, true);
			UI.setEqualizeColumWidths(_firstColumnControls);

			_shellContainer.layout(true, true);
			UI.setEqualizeColumWidths(_firstColumnContainerControls);
		}

		return shell;

	}

	private Composite createUI010Shell(final Composite parent) {

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.spacing(0, 0)
				.numColumns(2)
				// set margin to draw the border
				.extendedMargins(1, 1, 1, 1)
				.applyTo(_shellContainer);
		_shellContainer.setForeground(_fgColor);
		_shellContainer.setBackground(_bgColor);
		_shellContainer.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent e) {
				onPaintShellContainer(e);
			}
		});
//		_shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{

			if (_visibleGraphs != 0) {
				createUI020AllValues(_shellContainer);
			}

			/*
			 * action toolbar in the top right corner
			 */
			createUI030Actions(_shellContainer);
		}

		return _shellContainer;
	}

	private void createUI020AllValues(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.applyTo(container);

		if (_isHorizontal) {
			GridLayoutFactory.fillDefaults()//
					.numColumns(_visibleGraphsCount)
					.spacing(5, 0)
					.extendedMargins(3, 2, 0, 0)
					.applyTo(container);
		} else {
			GridLayoutFactory.fillDefaults()//
					.spacing(5, 0)
					.applyTo(container);
		}

		container.setBackground(_bgColor);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI285TimeSlices(container);
			createUI255TimeDuration(container);
			createUI260TimeOfDay(container);
			createUI215Distance(container);
			createUI205Altitude(container);
			createUI235Pulse(container);
			createUI240Speed(container);
			createUI225Pace(container);
			createUI230Power(container);
			createUI250Temperature(container);
			createUI220Gradient(container);
			createUI200Altimeter(container);
			createUI210Cadence(container);
		}
	}

	private void createUI030Actions(final Composite parent) {

		/*
		 * create toolbar
		 */
		_toolbarControl = new ToolBar(parent, SWT.FLAT);
		GridDataFactory.fillDefaults()//
//				.align(SWT.END, SWT.FILL)
				.align(SWT.END, SWT.BEGINNING)
				.applyTo(_toolbarControl);
		_toolbarControl.setForeground(_fgColor);
		_toolbarControl.setBackground(_bgColor);
//		_toolbarControl.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
//		_toolbarControl.setBackground(_fgToolbar);

		final ToolBarManager tbm = new ToolBarManager(_toolbarControl);

		tbm.add(_actionOpenTooltipMenu);

		tbm.update(true);
	}

	private void createUI200Altimeter(final Composite parent) {

		if (_visibleAltimeterId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblAltimeter = createUILabelValue(
					container,
					SWT.TRAIL,
					6,
					Messages.Graph_Label_Altimeter,
					GraphColorProvider.PREF_GRAPH_ALTIMETER);

			_lblAltimeterUnit = createUILabelValue(
					container,
					SWT.LEAD,
					Messages.Graph_Label_Altimeter,
					GraphColorProvider.PREF_GRAPH_ALTIMETER);
		}
		_firstColumnControls.add(_lblAltimeter);
		_firstColumnContainerControls.add(container);
	}

	private void createUI205Altitude(final Composite parent) {

		if (_visibleAltitudeId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblAltitude = createUILabelValue(
					container,
					SWT.TRAIL,
					8,
					Messages.Graph_Label_Altitude,
					GraphColorProvider.PREF_GRAPH_ALTITUDE);

			_lblAltitudeUnit = createUILabelValue(
					container,
					SWT.LEAD,
					Messages.Graph_Label_Altitude,
					GraphColorProvider.PREF_GRAPH_ALTITUDE);
		}
		_firstColumnControls.add(_lblAltitude);
		_firstColumnContainerControls.add(container);
	}

	private void createUI210Cadence(final Composite parent) {

		if (_visibleCadenceId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblCadence = createUILabelValue(
					container,
					SWT.TRAIL,
					3,
					Messages.Graph_Label_Cadence,
					GraphColorProvider.PREF_GRAPH_CADENCE);

			createUILabel(
					container,
					Messages.Graph_Label_Cadence_unit,
					Messages.Graph_Label_Cadence,
					GraphColorProvider.PREF_GRAPH_CADENCE);
		}
		_firstColumnControls.add(_lblCadence);
		_firstColumnContainerControls.add(container);
	}

	private void createUI215Distance(final Composite parent) {

		if (_visibleDistanceId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblDistance = createUILabelValue(
					container,
					SWT.TRAIL,
					8,
					Messages.Graph_Label_Distance,
					GraphColorProvider.PREF_GRAPH_DISTANCE);

			_lblDistanceUnit = createUILabelValue(
					container,
					SWT.LEAD,
					Messages.Graph_Label_Distance,
					GraphColorProvider.PREF_GRAPH_DISTANCE);
		}
		_firstColumnControls.add(_lblDistance);
		_firstColumnContainerControls.add(container);
	}

	private void createUI220Gradient(final Composite parent) {

		if (_visibleGradientId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblGradient = createUILabelValue(
					container,
					SWT.TRAIL,
					5,
					Messages.Graph_Label_Gradient,
					GraphColorProvider.PREF_GRAPH_GRADIENT);

			createUILabel(
					container,
					Messages.Graph_Label_Gradiend_unit,
					Messages.Graph_Label_Gradient,
					GraphColorProvider.PREF_GRAPH_GRADIENT);
		}
		_firstColumnControls.add(_lblGradient);
		_firstColumnContainerControls.add(container);
	}

	private void createUI225Pace(final Composite parent) {

		if (_visiblePaceId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblPace = createUILabelValue(
					container,
					SWT.TRAIL,
					4,
					Messages.Graph_Label_Pace,
					GraphColorProvider.PREF_GRAPH_PACE);

			_lblPaceUnit = createUILabelValue(
					container,
					SWT.LEAD,
					Messages.Graph_Label_Pace,
					GraphColorProvider.PREF_GRAPH_PACE);
		}
		_firstColumnControls.add(_lblPace);
		_firstColumnContainerControls.add(container);
	}

	private void createUI230Power(final Composite parent) {

		if (_visiblePowerId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblPower = createUILabelValue(
					container,
					SWT.TRAIL,
					4,
					Messages.Graph_Label_Power,
					GraphColorProvider.PREF_GRAPH_POWER);

			createUILabel(
					container,
					Messages.Graph_Label_Power_unit,
					Messages.Graph_Label_Power,
					GraphColorProvider.PREF_GRAPH_POWER);
		}
		_firstColumnControls.add(_lblPower);
		_firstColumnContainerControls.add(container);
	}

	private void createUI235Pulse(final Composite parent) {

		if (_visiblePulseId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblPulse = createUILabelValue(
					container,
					SWT.TRAIL,
					4,
					Messages.Graph_Label_Heartbeat,
					GraphColorProvider.PREF_GRAPH_HEARTBEAT);

			createUILabel(
					container,
					Messages.Graph_Label_Heartbeat_unit,
					Messages.Graph_Label_Heartbeat,
					GraphColorProvider.PREF_GRAPH_HEARTBEAT);
		}
		_firstColumnControls.add(_lblPulse);
		_firstColumnContainerControls.add(container);
	}

	private void createUI240Speed(final Composite parent) {

		if (_visibleSpeedId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblSpeed = createUILabelValue(
					container,
					SWT.TRAIL,
					5,
					Messages.Graph_Label_Speed,
					GraphColorProvider.PREF_GRAPH_SPEED);

			_lblSpeedUnit = createUILabelValue(
					container,
					SWT.LEAD,
					Messages.Graph_Label_Speed,
					GraphColorProvider.PREF_GRAPH_SPEED);
		}
		_firstColumnControls.add(_lblSpeed);
		_firstColumnContainerControls.add(container);
	}

	private void createUI250Temperature(final Composite parent) {

		if (_visibleTemperatureId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblTemperature = createUILabelValue(
					container,
					SWT.TRAIL,
					5,
					Messages.Graph_Label_Temperature,
					GraphColorProvider.PREF_GRAPH_TEMPTERATURE);

			_lblTemperatureUnit = createUILabelValue(
					container,
					SWT.LEAD,
					Messages.Graph_Label_Temperature,
					GraphColorProvider.PREF_GRAPH_TEMPTERATURE);
		}
		_firstColumnControls.add(_lblTemperature);
		_firstColumnContainerControls.add(container);
	}

	private void createUI255TimeDuration(final Composite parent) {

		if (_visibleTimeDurationId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblTimeDuration = createUILabelValue(
					container,
					SWT.TRAIL,
					8,
					Messages.Graph_Label_TimeDuration,
					GraphColorProvider.PREF_GRAPH_TIME);

			createUILabel(
					container,
					UI.UNIT_LABEL_TIME,
					Messages.Graph_Label_TimeDuration,
					GraphColorProvider.PREF_GRAPH_TIME);
		}

		_firstColumnControls.add(_lblTimeDuration);
		_firstColumnContainerControls.add(container);
	}

	private void createUI260TimeOfDay(final Composite parent) {

		if (_visibleTimeOfDayId == 0) {
			return;
		}

		final Composite container = createUIValueContainer(parent);
		{
			_lblTimeOfDay = createUILabelValue(
					container,
					SWT.TRAIL,
					8,
					Messages.Graph_Label_TimeOfDay,
					GraphColorProvider.PREF_GRAPH_TIME);

			createUILabel(
					container,
					UI.UNIT_LABEL_TIME,
					Messages.Graph_Label_TimeOfDay,
					GraphColorProvider.PREF_GRAPH_TIME);
		}

		_firstColumnControls.add(_lblTimeOfDay);
		_firstColumnContainerControls.add(container);
	}

	private void createUI285TimeSlices(final Composite parent) {

		if (_visibleTimeSliceId == 0) {
			return;
		}

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.CENTER, SWT.FILL)
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(3)
				.spacing(2, 0)
				.applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{

			// label: current value
			_lblDataSerieCurrent = createUILabelValue(
					container,
					SWT.TRAIL,
					5,
					Messages.Tooltip_ValuePoint_Label_SlicesCurrent_Tooltip,
					null);

			// label: separator
			createUILabel(container, ":", null, null); //$NON-NLS-1$

			// label: max value
			_lblDataSerieMax = createUILabelValue(
					container,
					SWT.LEAD,
					5,
					Messages.Tooltip_ValuePoint_Label_SlicesMax_Tooltip,
					null);
		}
	}

	private Composite createUI999NoData(final Composite parent) {

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
		shellContainer.setForeground(_fgColor);
		shellContainer.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{

			final Composite container = new Composite(shellContainer, SWT.NONE);
			container.setForeground(_fgColor);
			container.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults()//
					.margins(5, 5)
					.applyTo(container);
			{
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Tooltip_ValuePoint_Label_NoTour);
				label.setForeground(_fgColor);
				label.setBackground(_bgColor);
			}
		}

		return shellContainer;
	}

	private Label createUILabel(final Composite parent,
								final String labelText,
								final String tooltip,
								final String colorId) {

		final Label label = new Label(parent, SWT.NONE);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		if (labelText != null) {
			label.setText(labelText);
		}

		if (tooltip != null) {
			label.setToolTipText(tooltip);
		}

		if (colorId != null) {

			final Color fgColor = _colorCache.getColor(//
					colorId, //
					_colorProvider.getGraphColorDefinition(colorId).getTextColor());

			label.setForeground(fgColor);
		}

		return label;
	}

	/**
	 * @param parent
	 * @param style
	 * @param chars
	 *            Hint for the width in characters.
	 * @param tooltip
	 * @param colorId
	 * @return
	 */
	private Label createUILabelValue(	final Composite parent,
										final int style,
										final int chars,
										final String tooltip,
										final String colorId) {

		final int charsWidth = chars == SWT.DEFAULT ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(chars);

		final Label label = new Label(parent, style);
		GridDataFactory.fillDefaults()//
				.hint(charsWidth, SWT.DEFAULT)
				.applyTo(label);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		if (tooltip != null) {
			label.setToolTipText(tooltip);
		}

		if (colorId != null) {

			final Color fgColor = _colorCache.getColor(//
					colorId, //
					_colorProvider.getGraphColorDefinition(colorId).getTextColor());

			label.setForeground(fgColor);
		}

		return label;
	}

	private Label createUILabelValue(final Composite parent, final int style, final String tooltip, final String colorId) {
		return createUILabelValue(parent, style, SWT.DEFAULT, tooltip, colorId);
	}

	private Composite createUIValueContainer(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(_valueUnitDistance, 0).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		return container;
	}

	public boolean isVisible() {
		return _isToolTipVisible;
	}

	@Override
	void onDispose() {

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		_colorCache.dispose();
		_ttMenuMgr.dispose();

		_firstColumnControls.clear();
		_firstColumnContainerControls.clear();

		super.onDispose();
	}

	private void onPaintShellContainer(final PaintEvent event) {

		final GC gc = event.gc;
		final Point shellSize = _shellContainer.getSize();

		// draw border
		gc.setForeground(_fgBorder);
		gc.drawRectangle(0, 0, shellSize.x - 1, shellSize.y - 1);

// this is not working correctly because a new paint needs to be done
//		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
//		switch (pinnedLocation) {
//		case TopLeft:
//			gc.drawPoint(0, 0);
//			break;
//		case TopRight:
//			gc.drawPoint(shellSize.x - 1, 0);
//			break;
//		case BottomLeft:
//			gc.drawPoint(0, shellSize.y - 1);
//			break;
//		case BottomRight:
//			gc.drawPoint(shellSize.x - 1, shellSize.y - 1);
//			break;
//		}
	}

	private void reopenTT() {

		// hide and recreate it
		hide();
		show(new Point(_devXMouse, _devXMouse));
	}

	@Override
	public void setChartMargins(final int marginTop, final int marginBottom) {

		this.chartMarginTop = marginTop;
		this.chartMarginBottom = marginBottom;
	}

	/**
	 * @param tourData
	 *            When <code>null</code> the tooltip will be hidden.
	 */
	void setTourData(final TourData tourData) {

		_tourData = tourData;
		_currentValueIndex = 0;

		if (tourData == null) {
			hide();
			return;
		}

		/*
		 * hide tool tip context menu because new tour data can change the available graphs which
		 * can be selected in the context menu
		 */
		if (_ttMenuMgr != null) {
			_ttMenuMgr.hideContextMenu();
		}

		final int visibleGraphsBackup = _visibleGraphs;

		updateUIStates();

		// prevent flickering when reopen
		if (visibleGraphsBackup != _visibleGraphs) {

			// reopen when other tour data are set which has other graphs
			reopenTT();
		}
	}

	@Override
	public void setValueIndex(	final int valueIndex,
								final int devXMouseMove,
								final int devYMouseMove,
								final Point valueDevPosition) {

		if (_tourData == null || _isToolTipVisible == false) {
			return;
		}

		_devXMouse = devXMouseMove;
		_devYMouse = devYMouseMove;

		if (_shellContainer == null || _shellContainer.isDisposed()) {

			/*
			 * tool tip is disposed, this happens on a mouse exit, display the tooltip again
			 */
			show(new Point(devXMouseMove, devYMouseMove));
		}

		// check again
		if (_shellContainer != null && !_shellContainer.isDisposed()) {

			setTTShellLocation(devXMouseMove, devYMouseMove, valueDevPosition);

			updateUI(valueIndex, false);
		}
	}

	@Override
	protected boolean shouldCreateToolTip(final Event event) {

		if (_tourData == null) {
			return false;
		}

		return super.shouldCreateToolTip(event);
	}

	@Override
	public void show(final Point location) {

		if (_isToolTipVisible == false) {
			return;
		}

		super.show(location);
	}

	ToolItem updateUI() {

		// update graph state
		updateUIStates();

		reopenTT();

		/**
		 * Get item which is opening the value point tooltip
		 * <p>
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 * This is a hack because the toolbar contains only one item, hopefully this will not
		 * change. <br>
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */
		final ToolItem toolItem = _toolbarControl.getItem(0);

		return toolItem;
	}

	private void updateUI(int valueIndex, boolean isForceUpdate) {

		final int[] timeSerie = _tourData.timeSerie;

		// check bounds
		if (valueIndex >= timeSerie.length || valueIndex < 0) {
			valueIndex = timeSerie.length;
		}

		// optimize update
		if (isForceUpdate == false && valueIndex == _currentValueIndex) {
			return;
		}

		isForceUpdate = false;
		_currentValueIndex = valueIndex;

		if (_visibleTimeSliceId != 0) {
			_lblDataSerieCurrent.setText(Integer.toString(_currentValueIndex));
			_lblDataSerieMax.setText(Integer.toString(timeSerie.length - 1));
		}

		if (_visibleTimeDurationId != 0) {
			_lblTimeDuration.setText(UI.format_hhh_mm_ss(timeSerie[valueIndex]));
		}

		if (_visibleTimeOfDayId != 0) {
			_lblTimeOfDay.setText(UI.format_hhh_mm_ss(_tourData.getStartTimeOfDay() + timeSerie[valueIndex]));
		}

		if (_visibleDistanceId != 0) {

			final float distance = _tourData.distanceSerie[valueIndex] / 1000 / UI.UNIT_VALUE_DISTANCE;

			_lblDistance.setText(_nf3NoGroup.format(distance));
			_lblDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);
		}

		if (_visibleAltitudeId != 0) {
			_lblAltitude.setText(_nf3NoGroup.format(//
					_tourData.getAltitudeSmoothedSerie()[valueIndex] / UI.UNIT_VALUE_ALTITUDE));
			_lblAltitudeUnit.setText(UI.UNIT_LABEL_ALTITUDE);
		}

		if (_visiblePulseId != 0) {
			_lblPulse.setText(Integer.toString((int) _tourData.pulseSerie[valueIndex]));
		}

		if (_visibleSpeedId != 0) {
			_lblSpeed.setText(_nf1.format(_tourData.getSpeedSerie()[valueIndex]));
			_lblSpeedUnit.setText(UI.UNIT_LABEL_SPEED);
		}

		if (_visiblePaceId != 0) {
			_lblPace.setText(_nf1.format(_tourData.getPaceSerie()[valueIndex]));
			_lblPaceUnit.setText(UI.UNIT_LABEL_PACE);
		}

		if (_visiblePowerId != 0) {
			_lblPower.setText(Integer.toString((int) _tourData.getPowerSerie()[valueIndex]));
		}

		if (_visibleTemperatureId != 0) {

			float temperature = _tourData.temperatureSerie[valueIndex];

			if (UI.UNIT_VALUE_TEMPERATURE != 1) {
				// get imperial temperature
				temperature = temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD;
			}

			_lblTemperature.setText(_nf1.format(temperature));
			_lblTemperatureUnit.setText(UI.UNIT_LABEL_TEMPERATURE);
		}

		if (_visibleGradientId != 0) {
			_lblGradient.setText(_nf1.format(_tourData.getGradientSerie()[valueIndex]));
		}

		if (_visibleAltimeterId != 0) {
			_lblAltimeter.setText(Integer.toString((int) _tourData.getAltimeterSerie()[valueIndex]));
			_lblAltimeterUnit.setText(UI.UNIT_LABEL_ALTIMETER);
		}

		if (_visibleCadenceId != 0) {
			_lblCadence.setText(Integer.toString((int) _tourData.cadenceSerie[valueIndex]));
		}

	}

	/**
	 * Sets state which graphs can be displayed.
	 */
	private void updateUIStates() {

		/*
		 * orientation
		 */
		final String stateOrientation = Util.getStateString(
				state,
				ValuePointToolTipMenuManager.STATE_VALUE_POINT_TOOLTIP_ORIENTATION,
				ValuePointToolTipMenuManager.DEFAULT_ORIENTATION.name());

		_isHorizontal = ValuePointToolTipOrientation.valueOf(stateOrientation) == ValuePointToolTipOrientation.Horizontal;

		/*
		 * visible graphs
		 */
		final int ttVisibleValues = Util.getStateInt(
				state,
				ValuePointToolTipMenuManager.STATE_VALUE_POINT_TOOLTIP_VISIBLE_GRAPHS,
				ValuePointToolTipMenuManager.DEFAULT_GRAPHS);

		_visibleAltimeterId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_ALTIMETER) > 0
				&& _tourData.getAltimeterSerie() != null ? ValuePointToolTipMenuManager.VALUE_ID_ALTIMETER : 0;

		_visibleAltitudeId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_ALTITUDE) > 0
				&& _tourData.getAltitudeSerie() != null ? ValuePointToolTipMenuManager.VALUE_ID_ALTITUDE : 0;

		_visibleCadenceId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_CADENCE) > 0
				&& _tourData.cadenceSerie != null ? ValuePointToolTipMenuManager.VALUE_ID_CADENCE : 0;

		_visibleDistanceId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_DISTANCE) > 0
				&& _tourData.distanceSerie != null ? ValuePointToolTipMenuManager.VALUE_ID_DISTANCE : 0;

		_visibleGradientId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_GRADIENT) > 0
				&& _tourData.getGradientSerie() != null ? ValuePointToolTipMenuManager.VALUE_ID_GRADIENT : 0;

		_visiblePaceId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_PACE) > 0
				&& _tourData.getPaceSerie() != null ? ValuePointToolTipMenuManager.VALUE_ID_PACE : 0;

		_visiblePowerId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_POWER) > 0
				&& _tourData.getPowerSerie() != null ? ValuePointToolTipMenuManager.VALUE_ID_POWER : 0;

		_visiblePulseId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_PULSE) > 0 //
				&& _tourData.pulseSerie != null ? ValuePointToolTipMenuManager.VALUE_ID_PULSE : 0;

		_visibleSpeedId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_SPEED) > 0
				&& _tourData.getSpeedSerie() != null ? ValuePointToolTipMenuManager.VALUE_ID_SPEED : 0;

		_visibleTemperatureId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_TEMPERATURE) > 0
				&& _tourData.temperatureSerie != null ? ValuePointToolTipMenuManager.VALUE_ID_TEMPERATURE : 0;

		_visibleTimeDurationId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_TIME_DURATION) > 0 //
				&& _tourData.timeSerie != null ? ValuePointToolTipMenuManager.VALUE_ID_TIME_DURATION : 0;

		_visibleTimeOfDayId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_TIME_OF_DAY) > 0 //
				&& _tourData.timeSerie != null ? ValuePointToolTipMenuManager.VALUE_ID_TIME_OF_DAY : 0;

		_visibleTimeSliceId = (ttVisibleValues & ValuePointToolTipMenuManager.VALUE_ID_TIME_SLICES) > 0 //
				? ValuePointToolTipMenuManager.VALUE_ID_TIME_SLICES
				: 0;

		_visibleGraphsCount = (_visibleAltimeterId > 0 ? 1 : 0)
				+ (_visibleAltitudeId > 0 ? 1 : 0)
				+ (_visibleCadenceId > 0 ? 1 : 0)
				+ (_visibleDistanceId > 0 ? 1 : 0)
				+ (_visibleGradientId > 0 ? 1 : 0)
				+ (_visiblePaceId > 0 ? 1 : 0)
				+ (_visiblePowerId > 0 ? 1 : 0)
				+ (_visiblePulseId > 0 ? 1 : 0)
				+ (_visibleSpeedId > 0 ? 1 : 0)
				+ (_visibleTemperatureId > 0 ? 1 : 0)
				+ (_visibleTimeDurationId > 0 ? 1 : 0)
				+ (_visibleTimeOfDayId > 0 ? 1 : 0)
				+ (_visibleTimeSliceId > 0 ? 1 : 0);

		_visibleGraphs = _visibleAltimeterId
				+ _visibleAltitudeId
				+ _visibleCadenceId
				+ _visibleDistanceId
				+ _visibleGradientId
				+ _visiblePaceId
				+ _visiblePowerId
				+ _visiblePulseId
				+ _visibleSpeedId
				+ _visibleTemperatureId
				+ _visibleTimeDurationId
				+ _visibleTimeOfDayId
				+ _visibleTimeSliceId;
	}

}
