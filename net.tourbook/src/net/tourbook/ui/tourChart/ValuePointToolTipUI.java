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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ITooltipOwner;
import net.tourbook.chart.IValuePointToolTip;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.Messages;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * This tooltip is displayed when the mouse is hovered over a value point in a line graph and
 * displays value point information.
 */
public class ValuePointToolTipUI extends ValuePointToolTipShell implements IValuePointToolTip {

	private static final int				SHELL_MARGIN	= 3;

	private final IPreferenceStore			_prefStore		= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean							_isToolTipVisible;

	private TourData						_tourData;

	private boolean							_isAltitude;
	private boolean							_isCadence;
	private boolean							_isDistance;
	private boolean							_isGradient;
	private boolean							_isPace;
	private boolean							_isPower;
	private boolean							_isPulse;
	private boolean							_isTemperature;

	private int								_currentValueIndex;

	private final DateTimeFormatter			_dtFormatter	= DateTimeFormat.mediumDateTime();

	private final NumberFormat				_nf1			= NumberFormat.getNumberInstance();
	private final NumberFormat				_nf1NoGroup		= NumberFormat.getNumberInstance();
	private final NumberFormat				_nf3			= NumberFormat.getNumberInstance();
	private final NumberFormat				_nf3NoGroup		= NumberFormat.getNumberInstance();
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

	private ValuePointToolTipMenuManager	_ttMenuMgr;
	private ActionOpenTooltipMenu			_actionOpenTooltipMenu;

	private int								_devXMouse;
	private int								_devYMouse;

	private IPropertyChangeListener			_prefChangeListener;

	/*
	 * UI resources
	 */
	private Color							_bgColor;
	private Color							_fgColor;
	private Font							_boldFont;
	private PixelConverter					_pc;

	/*
	 * UI controls
	 */
	private Composite						_shell;
	private Composite						_ttContainer;

	private Label							_lblDataSerieCurrent;
	private Label							_lblDataSerieMax;

	private Label							_lblTime;
	private Label							_lblAltitude;
	private Label							_lblAltitudeUnit;
	private Label							_lblDistance;
	private Label							_lblDistanceUnit;

	private class ActionOpenTooltipMenu extends Action {

		public ActionOpenTooltipMenu(final ValuePointToolTipMenuManager tooltipMenuManager) {
			super(null, Action.AS_PUSH_BUTTON);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__tour_options));
		}

		@Override
		public void runWithEvent(final Event event) {
			_ttMenuMgr.openToolTipMenu(event);
		}
	}

	public ValuePointToolTipUI(final ITooltipOwner tooltipOwner) {

		super(tooltipOwner);

		_isToolTipVisible = _prefStore.getBoolean(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE);

		addPrefListener();
	}

	void actionHideToolTip() {

		_prefStore.setValue(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE, false);

		_isToolTipVisible = false;

		hide();
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
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void createActions() {

		_ttMenuMgr = new ValuePointToolTipMenuManager(this);

		_actionOpenTooltipMenu = new ActionOpenTooltipMenu(_ttMenuMgr);
	}

	@Override
	protected Composite createToolTipContentArea(final Event event, final Composite parent) {

		Composite shell;

		if (_tourData == null || _tourData.timeSerie == null || _tourData.timeSerie.length == 0) {

			// there are no data available

			shell = createUI99NoData(parent);

		} else {

			// tour data is available

			createActions();

			shell = createUI(parent);
			_shell = shell;
		}

		return shell;
	}

	private Composite createUI(final Composite parent) {

		final Display display = parent.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		_pc = new PixelConverter(parent);

		final Composite shell = createUI10Shell(parent);

		updateUI(_currentValueIndex, true);

		// compute width for all controls and equalize column width for the different sections
		_ttContainer.layout(true, true);
//		UI.setEqualizeColumWidths(_firstColumnControls, 5);
//		UI.setEqualizeColumWidths(_secondColumnControls);

		return shell;

	}

	private Composite createUI10Shell(final Composite parent) {

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(shellContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(shellContainer);
		shellContainer.setForeground(_fgColor);
		shellContainer.setBackground(_bgColor);
//		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			/*
			 * action toolbar in the top right corner
			 */
			createUI20Header(shellContainer);

			_ttContainer = new Composite(shellContainer, SWT.NONE);
			_ttContainer.setForeground(_fgColor);
			_ttContainer.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults() //
					.margins(SHELL_MARGIN, SHELL_MARGIN)
					.applyTo(_ttContainer);
//			_ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{

				createUI30(_ttContainer);
			}
		}

		return shellContainer;
	}

	private void createUI20Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
//				.hint(SWT.DEFAULT, 55)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI22DataSerie(container);
			createUI24Actions(container);
		}
	}

	private void createUI22DataSerie(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(4)
				.extendedMargins(SHELL_MARGIN, 0, 0, 2)
				.applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * label: #
			 */
			Label label = createUILabel(container, "# ");
			GridDataFactory.fillDefaults() //
					.align(SWT.FILL, SWT.END)
					.applyTo(label);

			/*
			 * label: current value
			 */
			_lblDataSerieCurrent = createUILabelValue(container, SWT.TRAIL);
			GridDataFactory.fillDefaults() //
					// set default width
					.hint(_pc.convertWidthInCharsToPixels(6), SWT.DEFAULT)
					.grab(false, true)
					.align(SWT.FILL, SWT.END)
					.applyTo(_lblDataSerieCurrent);

			/*
			 * label: separator
			 */
			label = createUILabel(container, ":");
			GridDataFactory.fillDefaults() //
					.align(SWT.FILL, SWT.END)
					.applyTo(label);

			/*
			 * label: max value
			 */
			_lblDataSerieMax = createUILabelValue(container, SWT.LEAD);
			GridDataFactory.fillDefaults() //
					// set default width
					.hint(_pc.convertWidthInCharsToPixels(6), SWT.DEFAULT)
					.grab(false, true)
					.align(SWT.FILL, SWT.END)
					.applyTo(_lblDataSerieMax);
		}
	}

	private void createUI24Actions(final Composite parent) {

		/*
		 * create toolbar
		 */
		final ToolBar toolbarControl = new ToolBar(parent, SWT.FLAT);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.align(SWT.END, SWT.FILL)
				.applyTo(toolbarControl);
		toolbarControl.setForeground(_fgColor);
		toolbarControl.setBackground(_bgColor);
//			toolbar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		final ToolBarManager tbm = new ToolBarManager(toolbarControl);

		tbm.add(_actionOpenTooltipMenu);

		tbm.update(true);
	}

	private void createUI30(final Composite parent) {

//		final GraphColorProvider colorProvider = GraphColorProvider.getInstance();

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
//				.margins(0, 0)
				.extendedMargins(2, 0, 0, 0)
				.spacing(5, 0)
				.numColumns(3)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(container);

		container.setBackground(_bgColor);
		{
			/*
			 * time
			 */
			Label label = createUILabel(container, Messages.Tooltip_Label_Time);
//			_firstColumnControls.add(label);

			_lblTime = createUILabelValue(container, SWT.TRAIL, 10);
//			_secondColumnControls.add(_lblRecordingTime);

			label = createUILabel(container, UI.UNIT_LABEL_TIME);

			// force this column to take the rest of the space
			GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

//			final Color fgColor = colorCache.getColor(//
//					GraphColorProvider.PREF_GRAPH_TIME, //
//					colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_TIME).getTextColor());

			/*
			 * distance
			 */
			label = createUILabel(container, Messages.Tooltip_Label_Distance);
//			_firstColumnControls.add(label);

			_lblDistance = createUILabelValue(container, SWT.TRAIL, 10);
//			_secondColumnControls.add(_lblDistance);

			_lblDistanceUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * altitude up
			 */
			label = createUILabel(container, Messages.Tooltip_Label_Altitude);
//			_firstColumnControls.add(label);

			_lblAltitude = createUILabelValue(container, SWT.TRAIL, 8);
//			_secondColumnControls.add(_lblAltitudeUp);

			_lblAltitudeUnit = createUILabelValue(container, SWT.LEAD);
		}
	}

	private Composite createUI99NoData(final Composite parent) {

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
				label.setText(Messages.Tour_Tooltip_Label_NoTour);
				label.setForeground(_fgColor);
				label.setBackground(_bgColor);
			}
		}

		return shellContainer;
	}

	private Label createUILabel(final Composite parent, final String labelText) {

		final Label label = new Label(parent, SWT.NONE);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		if (labelText != null) {
			label.setText(labelText);
		}

		return label;
	}

	private Label createUILabelValue(final Composite parent, final int style) {
		return createUILabelValue(parent, style, SWT.DEFAULT);
	}

	/**
	 * @param parent
	 * @param style
	 * @param chars
	 *            Hint for the width in characters.
	 * @return
	 */
	private Label createUILabelValue(final Composite parent, final int style, final int chars) {

		final int charsWidth = chars == SWT.DEFAULT ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(chars);

		final Label label = new Label(parent, style);
		GridDataFactory.fillDefaults().hint(charsWidth, SWT.DEFAULT).applyTo(label);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		return label;
	}

	public boolean isVisible() {
		return _isToolTipVisible;
	}

	@Override
	void onDispose() {

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.onDispose();
	}

	@Override
	public void setChartMargins(final int marginTop, final int marginBottom) {

		this.marginTop = marginTop;
		this.marginBottom = marginBottom;
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

		_isAltitude = _tourData.altitudeSerie != null;
		_isCadence = _tourData.cadenceSerie != null;
		_isDistance = _tourData.distanceSerie != null;
		_isGradient = _tourData.gradientSerie != null;
		_isPace = _tourData.getPaceSerie() != null;
		_isPower = _tourData.getPowerSerie() != null;
		_isPulse = _tourData.pulseSerie != null;
		_isTemperature = _tourData.temperatureSerie != null;

	}

	@Override
	public void setValueIndex(final int valueIndex, final int devXMouseMove, final int devYMouseMove) {

		if (_tourData == null || _isToolTipVisible == false) {
			return;
		}

		_devXMouse = devXMouseMove;
		_devYMouse = devYMouseMove;

		if (_ttContainer == null || _ttContainer.isDisposed()) {

			/*
			 * tool tip is disposed, this happens on a mouse exit, display the tooltip again
			 */
			show(new Point(devXMouseMove, devYMouseMove));
		}

		updateUI(valueIndex, false);
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

		_lblDataSerieCurrent.setText(Integer.toString(_currentValueIndex));
		_lblDataSerieMax.setText(Integer.toString(timeSerie.length - 1));

		// time is always available when a chart is painted
		final float time = timeSerie[valueIndex];
		_lblTime.setText(UI.format_hhh_mm_ss((long) time));

		if (_isAltitude) {
			_lblAltitude.setText(_nf3NoGroup.format(_tourData.altitudeSerie[valueIndex] / UI.UNIT_VALUE_ALTITUDE));
			_lblAltitudeUnit.setText(UI.UNIT_LABEL_ALTITUDE);
		}

		if (_isDistance) {

			final float distance = _tourData.distanceSerie[valueIndex] / 1000 / UI.UNIT_VALUE_DISTANCE;

			_lblDistance.setText(_nf3NoGroup.format(distance));
			_lblDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);
		}
	}

}
