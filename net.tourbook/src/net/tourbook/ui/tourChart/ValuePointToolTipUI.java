/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
import java.util.Formatter;

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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * This tooltip is displayed when the mouse is hovered over a value point in a line graph and
 * displays value point information.
 */
public class ValuePointToolTipUI extends ValuePointToolTipShell implements IValuePointToolTip {

	private final IPreferenceStore			_prefStore						= TourbookPlugin.getDefault() //
																					.getPreferenceStore();

	private IPropertyChangeListener			_prefChangeListener;

	private TourData						_tourData;

	private ValuePointToolTipMenuManager	_ttMenuMgr;
	private ActionOpenTooltipMenu			_actionOpenTooltipMenu;

	private int								_devXMouse;
	private int								_devYMouse;

	/**
	 * Global state if the tooltip is visible.
	 */
	private boolean							_isToolTipVisible;
	private int								_currentValueIndex;
	private int								_valueUnitDistance;
	private double							_chartZoomFactor;

	private int[]							_updateCounter					= new int[] { 0 };
	private long							_lastUpdateUITime;
	private boolean							_isHorizontal;

	private final NumberFormat				_nf1							= NumberFormat.getNumberInstance();
	private final NumberFormat				_nf1min							= NumberFormat.getNumberInstance();
	private final NumberFormat				_nf1NoGroup						= NumberFormat.getNumberInstance();
	private final NumberFormat				_nf3							= NumberFormat.getNumberInstance();
	private final NumberFormat				_nf3NoGroup						= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf1min.setMinimumFractionDigits(1);
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
	 * Contains all graph id's which are set to be visible but can be unavailable
	 */
	private int								_allVisibleValueIds;
	private int								_allVisibleAndAvailableValueIds;
	private int								_allVisibleAndAvailableValueCounter;

	private boolean							_isVisibleAndAvailableAltimeter;
	private boolean							_isVisibleAndAvailableAltitude;
	private boolean							_isVisibleAndAvailableCadence;
	private boolean							_isVisibleAndAvailableChartZoomFactor;
	private boolean							_isVisibleAndAvailableDistance;
	private boolean							_isVisibleAndAvailableGradient;
	private boolean							_isVisibleAndAvailablePace;
	private boolean							_isVisibleAndAvailablePower;
	private boolean							_isVisibleAndAvailablePulse;
	private boolean							_isVisibleAndAvailableSpeed;
	private boolean							_isVisibleAndAvailableTemperature;
	private boolean							_isVisibleAndAvailableTimeDuration;
	private boolean							_isVisibleAndAvailableTimeOfDay;
	private boolean							_isVisibleAndAvailableTimeSlice;

	/*
	 * UI resources
	 */
	private Color							_fgColor;
	private Color							_fgBorder;
	private Color							_bgColor;
	private final ColorCache				_colorCache						= new ColorCache();
	private final GraphColorProvider		_colorProvider					= GraphColorProvider.getInstance();

	private final ArrayList<Control>		_firstColumnControls			= new ArrayList<Control>();
	private final ArrayList<Control>		_firstColumnContainerControls	= new ArrayList<Control>();

	/*
	 * UI controls
	 */
	private Composite						_shellContainer;
	private ToolBar							_toolbarControl;

	private Label							_lblAltimeter;
	private Label							_lblAltitude;
	private Label							_lblCadence;
	private Label							_lblChartZoomFactor;
	private Label							_lblDataSerieCurrent;
	private Label							_lblDataSerieMax;
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
			_ttMenuMgr.openToolTipMenu(event, _tourData, _allVisibleValueIds, _isHorizontal);
		}
	}

	public ValuePointToolTipUI(final ITooltipOwner tooltipOwner, final IDialogSettings state) {

		super(tooltipOwner, state);

		// get state if the tooltip is visible or hidden
		_isToolTipVisible = _prefStore.getBoolean(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE);

		_allVisibleValueIds = Util.getStateInt(
				state,
				ValuePointToolTipMenuManager.STATE_VALUE_POINT_TOOLTIP_VISIBLE_GRAPHS,
				ValuePointToolTipMenuManager.DEFAULT_GRAPHS);

		/*
		 * orientation
		 */
		final String stateOrientation = Util.getStateString(
				state,
				ValuePointToolTipMenuManager.STATE_VALUE_POINT_TOOLTIP_ORIENTATION,
				ValuePointToolTipMenuManager.DEFAULT_ORIENTATION.name());

		_isHorizontal = ValuePointToolTipOrientation.valueOf(stateOrientation) == ValuePointToolTipOrientation.Horizontal;

		addPrefListener();
	}

	void actionHideToolTip() {

		_prefStore.setValue(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE, false);

		_isToolTipVisible = false;

		hide();
	}

	void actionOrientation(final ValuePointToolTipOrientation orientation, final boolean isReopenToolTip) {

		_isHorizontal = orientation == ValuePointToolTipOrientation.Horizontal;

		if (isReopenToolTip) {
			reopen();
		}
	}

	void actionSetDefaults(final int allVisibleValues, final ValuePointToolTipOrientation orientation) {

		actionOrientation(orientation, false);
		actionVisibleValues(allVisibleValues);

		state.put(STATE_VALUE_POINT_PIN_LOCATION, DEFAULT_PIN_LOCATION.name());

		actionPinLocation(DEFAULT_PIN_LOCATION);
	}

	ToolItem actionVisibleValues(final int visibleValues) {

		// update value states
		updateStateVisibleValues(visibleValues);

		reopen();

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

					reopen();
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

//		if (_tourData == null || _tourData.timeSerie == null || _tourData.timeSerie.length == 0) {
//
//			// there are no data available
//
//			shell = createUI999NoData(parent);
//
//		} else {

		// tour data is available

		createActions();

		shell = createUI(parent);
//		}

		return shell;
	}

	private Composite createUI(final Composite parent) {

		final Display display = parent.getDisplay();

		_fgBorder = _colorCache.getColor(new RGB(0xe5, 0xe5, 0xcb));
//		_bgColor = _colorCache.getColor(new RGB(0xff, 0xff, 0xf0));
		_bgColor = _colorCache.getColor(new RGB(0xff, 0xff, 0xff));
		_fgColor = display.getSystemColor(SWT.COLOR_DARK_GRAY);

		_valueUnitDistance = _isHorizontal ? 2 : 5;

		_firstColumnControls.clear();
		_firstColumnContainerControls.clear();

		final Composite shell = createUI010Shell(parent);

		updateUI(_currentValueIndex);

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

			if (_allVisibleAndAvailableValueIds > 0) {

				createUI020AllValues(_shellContainer);
			} else {
				createUI999NoData(_shellContainer);
			}

			// action toolbar in the top right corner
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
					.numColumns(_allVisibleAndAvailableValueCounter)
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
			createUI211ChartZoomFactor(container);
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

		if (_isVisibleAndAvailableAltimeter) {

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

				_lblAltimeterUnit.setText(UI.UNIT_LABEL_ALTIMETER);
			}
			_firstColumnControls.add(_lblAltimeter);
			_firstColumnContainerControls.add(container);
		}
	}

	private void createUI205Altitude(final Composite parent) {

		if (_isVisibleAndAvailableAltitude) {

			final Composite container = createUIValueContainer(parent);
			{
				_lblAltitude = createUILabelValue(
						container,
						SWT.TRAIL,
						6,
						Messages.Graph_Label_Altitude,
						GraphColorProvider.PREF_GRAPH_ALTITUDE);

				_lblAltitudeUnit = createUILabelValue(
						container,
						SWT.LEAD,
						Messages.Graph_Label_Altitude,
						GraphColorProvider.PREF_GRAPH_ALTITUDE);

				_lblAltitudeUnit.setText(UI.UNIT_LABEL_ALTITUDE);
			}
			_firstColumnControls.add(_lblAltitude);
			_firstColumnContainerControls.add(container);
		}
	}

	private void createUI210Cadence(final Composite parent) {

		if (_isVisibleAndAvailableCadence) {

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
	}

	private void createUI211ChartZoomFactor(final Composite parent) {

		if (_isVisibleAndAvailableChartZoomFactor) {

			final Composite container = createUIValueContainer(parent);
			{

				_lblChartZoomFactor = createUILabelValue(
						container,
						SWT.TRAIL,
						8,
						Messages.Tooltip_ValuePoint_Label_ChartZoomFactor_Tooltip,
						null);

				// spacer
				new Label(container, SWT.NONE);
			}

			_firstColumnControls.add(_lblChartZoomFactor);
		}
	}

	private void createUI215Distance(final Composite parent) {

		if (_isVisibleAndAvailableDistance) {

			final Composite container = createUIValueContainer(parent);
			{
				_lblDistance = createUILabelValue(
						container,
						SWT.TRAIL,
						9,
						Messages.Graph_Label_Distance,
						GraphColorProvider.PREF_GRAPH_DISTANCE);

				_lblDistanceUnit = createUILabelValue(
						container,
						SWT.LEAD,
						Messages.Graph_Label_Distance,
						GraphColorProvider.PREF_GRAPH_DISTANCE);

				_lblDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);
			}
			_firstColumnControls.add(_lblDistance);
			_firstColumnContainerControls.add(container);
		}
	}

	private void createUI220Gradient(final Composite parent) {

		if (_isVisibleAndAvailableGradient) {

			final Composite container = createUIValueContainer(parent);
			{
				_lblGradient = createUILabelValue(
						container,
						SWT.TRAIL,
						4,
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
	}

	private void createUI225Pace(final Composite parent) {

		if (_isVisibleAndAvailablePace) {

			final Composite container = createUIValueContainer(parent);
			{
				_lblPace = createUILabelValue(
						container,
						SWT.TRAIL,
						5,
						Messages.Graph_Label_Pace,
						GraphColorProvider.PREF_GRAPH_PACE);

				_lblPaceUnit = createUILabelValue(
						container,
						SWT.LEAD,
						Messages.Graph_Label_Pace,
						GraphColorProvider.PREF_GRAPH_PACE);

				_lblPaceUnit.setText(UI.UNIT_LABEL_PACE);
			}
			_firstColumnControls.add(_lblPace);
			_firstColumnContainerControls.add(container);
		}
	}

	private void createUI230Power(final Composite parent) {

		if (_isVisibleAndAvailablePower) {

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
	}

	private void createUI235Pulse(final Composite parent) {

		if (_isVisibleAndAvailablePulse) {

			final Composite container = createUIValueContainer(parent);
			{
				_lblPulse = createUILabelValue(
						container,
						SWT.TRAIL,
						3,
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
	}

	private void createUI240Speed(final Composite parent) {

		if (_isVisibleAndAvailableSpeed) {

			final Composite container = createUIValueContainer(parent);
			{
				_lblSpeed = createUILabelValue(
						container,
						SWT.TRAIL,
						4,
						Messages.Graph_Label_Speed,
						GraphColorProvider.PREF_GRAPH_SPEED);

				_lblSpeedUnit = createUILabelValue(
						container,
						SWT.LEAD,
						Messages.Graph_Label_Speed,
						GraphColorProvider.PREF_GRAPH_SPEED);

				_lblSpeedUnit.setText(UI.UNIT_LABEL_SPEED);
			}
			_firstColumnControls.add(_lblSpeed);
			_firstColumnContainerControls.add(container);
		}
	}

	private void createUI250Temperature(final Composite parent) {

		if (_isVisibleAndAvailableTemperature) {

			final Composite container = createUIValueContainer(parent);
			{
				_lblTemperature = createUILabelValue(
						container,
						SWT.TRAIL,
						4,
						Messages.Graph_Label_Temperature,
						GraphColorProvider.PREF_GRAPH_TEMPTERATURE);

				_lblTemperatureUnit = createUILabelValue(
						container,
						SWT.LEAD,
						Messages.Graph_Label_Temperature,
						GraphColorProvider.PREF_GRAPH_TEMPTERATURE);

				_lblTemperatureUnit.setText(UI.UNIT_LABEL_TEMPERATURE);
			}
			_firstColumnControls.add(_lblTemperature);
			_firstColumnContainerControls.add(container);
		}
	}

	private void createUI255TimeDuration(final Composite parent) {

		if (_isVisibleAndAvailableTimeDuration) {

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
	}

	private void createUI260TimeOfDay(final Composite parent) {

		if (_isVisibleAndAvailableTimeOfDay) {

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
	}

	private void createUI285TimeSlices(final Composite parent) {

		if (_isVisibleAndAvailableTimeSlice) {

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
						6,
						Messages.Tooltip_ValuePoint_Label_SlicesCurrent_Tooltip,
						null);

				// label: separator
				createUILabel(container, ":", null, null); //$NON-NLS-1$

				// label: max value
				_lblDataSerieMax = createUILabelValue(
						container,
						SWT.LEAD,
						6,
						Messages.Tooltip_ValuePoint_Label_SlicesMax_Tooltip,
						null);
			}
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
					.extendedMargins(5, 5, 0, 0)
					.applyTo(container);
			{
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Tooltip_ValuePoint_Label_NoData);
				label.setToolTipText(Messages.Tooltip_ValuePoint_Label_NoData_Tooltip);
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

		final int charsWidth;
		if (chars == SWT.DEFAULT) {

			charsWidth = SWT.DEFAULT;

		} else {

			final StringBuilder sb = new StringBuilder();
			sb.append('.');
			for (int charIndex = 0; charIndex < chars; charIndex++) {
				sb.append('8');
			}

			final GC gc = new GC(parent);
			charsWidth = gc.textExtent(sb.toString()).x;
			gc.dispose();
		}

		final Label label = new Label(parent, style);
		GridDataFactory.fillDefaults()//
				.hint(charsWidth, SWT.DEFAULT)
				.applyTo(label);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		if (tooltip != null) {
			label.setToolTipText(tooltip);
		}

		if (colorId == null) {
			label.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		} else {
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

	private int getState(final int visibleValues, final int valueId) {
		return (visibleValues & valueId) > 0 ? valueId : 0;
	}

	@Override
	public Shell getToolTipShell() {
		return super.getToolTipShell();
	}

	public boolean isVisible() {
		return _isToolTipVisible;
	}

	@Override
	void onDispose() {

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		_colorCache.dispose();

		if (_ttMenuMgr != null) {
			_ttMenuMgr.dispose();
		}

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

	/**
	 * Reopens the tooltip at the current position, this will not show the tooltip when it is set to
	 * be hidden.
	 */
	public void reopen() {

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

		final int visibleValuesBackup = _allVisibleAndAvailableValueIds;

		updateStateVisibleValues(_allVisibleValueIds);

		// prevent flickering when reopen
		if (visibleValuesBackup != _allVisibleAndAvailableValueIds) {

			// reopen when other tour data are set which has other graphs
			reopen();
		}
	}

	@Override
	public void setValueIndex(	final int valueIndex,
								final int devXMouseMove,
								final int devYMouseMove,
								final Point valueDevPosition,
								final double chartZoomFactor) {

		if (_tourData == null || _isToolTipVisible == false) {
			return;
		}

		_devXMouse = devXMouseMove;
		_devYMouse = devYMouseMove;
		_chartZoomFactor = chartZoomFactor;

		if (_shellContainer == null || _shellContainer.isDisposed()) {

			/*
			 * tool tip is disposed, this happens on a mouse exit, display the tooltip again
			 */
			show(new Point(devXMouseMove, devYMouseMove));
		}

		// check again
		if (_shellContainer != null && !_shellContainer.isDisposed()) {

			setTTShellLocation(devXMouseMove, devYMouseMove, valueDevPosition);

			updateUI(valueIndex);
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

		if (_isToolTipVisible) {
			super.show(location);
		}
	}

	/**
	 * Sets state which graphs can be displayed.
	 * 
	 * @param ttVisibleValues
	 */
	private void updateStateVisibleValues(final int ttVisibleValues) {

		final int visibleIdAltimeter = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_ALTIMETER);
		final int visibleIdAltitude = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_ALTITUDE);
		final int visibleIdCadence = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_CADENCE);
		final int visibleIdChartZoomFactor = getState(
				ttVisibleValues,
				ValuePointToolTipMenuManager.VALUE_ID_CHART_ZOOM_FACTOR);
		final int visibleIdDistance = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_DISTANCE);
		final int visibleIdGradient = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_GRADIENT);
		final int visibleIdPace = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_PACE);
		final int visibleIdPower = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_POWER);
		final int visibleIdPulse = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_PULSE);
		final int visibleIdSpeed = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_SPEED);
		final int visibleIdTemperature = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_TEMPERATURE);
		final int visibleIdTimeDuration = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_TIME_DURATION);
		final int visibleIdTimeOfDay = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_TIME_OF_DAY);
		final int visibleIdTimeSlice = getState(ttVisibleValues, ValuePointToolTipMenuManager.VALUE_ID_TIME_SLICES);

		final boolean isAvailableAltimeter = _tourData.getAltimeterSerie() != null;
		final boolean isAvailableAltitude = _tourData.getAltitudeSerie() != null;
		final boolean isAvailableCadence = _tourData.cadenceSerie != null;
		final boolean isAvailableChartZoomFactor = true;
		final boolean isAvailableDistance = _tourData.distanceSerie != null;
		final boolean isAvailableGradient = _tourData.getGradientSerie() != null;
		final boolean isAvailablePace = _tourData.getPaceSerie() != null;
		final boolean isAvailablePower = _tourData.getPowerSerie() != null;
		final boolean isAvailablePulse = _tourData.pulseSerie != null;
		final boolean isAvailableSpeed = _tourData.getSpeedSerie() != null;
		final boolean isAvailableTemperature = _tourData.temperatureSerie != null;
		final boolean isAvailableTimeDuration = _tourData.timeSerie != null;
		final boolean isAvailableTimeOfDay = _tourData.timeSerie != null;
		final boolean isAvailableTimeSlice = true;

		_allVisibleValueIds = visibleIdAltimeter
				+ visibleIdAltitude
				+ visibleIdCadence
				+ visibleIdChartZoomFactor
				+ visibleIdDistance
				+ visibleIdGradient
				+ visibleIdPace
				+ visibleIdPower
				+ visibleIdPulse
				+ visibleIdSpeed
				+ visibleIdTemperature
				+ visibleIdTimeDuration
				+ visibleIdTimeOfDay
				+ visibleIdTimeSlice;

		_allVisibleAndAvailableValueIds = (isAvailableAltimeter ? visibleIdAltimeter : 0)
				+ (isAvailableAltitude ? visibleIdAltitude : 0)
				+ (isAvailableCadence ? visibleIdCadence : 0)
				+ (isAvailableChartZoomFactor ? visibleIdChartZoomFactor : 0)
				+ (isAvailableDistance ? visibleIdDistance : 0)
				+ (isAvailableGradient ? visibleIdGradient : 0)
				+ (isAvailablePace ? visibleIdPace : 0)
				+ (isAvailablePower ? visibleIdPower : 0)
				+ (isAvailablePulse ? visibleIdPulse : 0)
				+ (isAvailableSpeed ? visibleIdSpeed : 0)
				+ (isAvailableTemperature ? visibleIdTemperature : 0)
				+ (isAvailableTimeDuration ? visibleIdTimeDuration : 0)
				+ (isAvailableTimeOfDay ? visibleIdTimeOfDay : 0)
				+ (isAvailableTimeSlice ? visibleIdTimeSlice : 0);

		_isVisibleAndAvailableAltimeter = isAvailableAltimeter && visibleIdAltimeter > 0;
		_isVisibleAndAvailableAltitude = isAvailableAltitude && visibleIdAltitude > 0;
		_isVisibleAndAvailableCadence = isAvailableCadence && visibleIdCadence > 0;
		_isVisibleAndAvailableChartZoomFactor = isAvailableChartZoomFactor && visibleIdChartZoomFactor > 0;
		_isVisibleAndAvailableDistance = isAvailableDistance && visibleIdDistance > 0;
		_isVisibleAndAvailableGradient = isAvailableGradient && visibleIdGradient > 0;
		_isVisibleAndAvailablePace = isAvailablePace && visibleIdPace > 0;
		_isVisibleAndAvailablePower = isAvailablePower && visibleIdPower > 0;
		_isVisibleAndAvailablePulse = isAvailablePulse && visibleIdPulse > 0;
		_isVisibleAndAvailableSpeed = isAvailableSpeed && visibleIdSpeed > 0;
		_isVisibleAndAvailableTemperature = isAvailableTemperature && visibleIdTemperature > 0;
		_isVisibleAndAvailableTimeDuration = isAvailableTimeDuration && visibleIdTimeDuration > 0;
		_isVisibleAndAvailableTimeOfDay = isAvailableTimeOfDay && visibleIdTimeOfDay > 0;
		_isVisibleAndAvailableTimeSlice = isAvailableTimeSlice && visibleIdTimeSlice > 0;

		_allVisibleAndAvailableValueCounter = (_isVisibleAndAvailableAltimeter ? 1 : 0)
				+ (_isVisibleAndAvailableAltitude ? 1 : 0)
				+ (_isVisibleAndAvailableCadence ? 1 : 0)
				+ (_isVisibleAndAvailableChartZoomFactor ? 1 : 0)
				+ (_isVisibleAndAvailableDistance ? 1 : 0)
				+ (_isVisibleAndAvailableGradient ? 1 : 0)
				+ (_isVisibleAndAvailablePace ? 1 : 0)
				+ (_isVisibleAndAvailablePower ? 1 : 0)
				+ (_isVisibleAndAvailablePulse ? 1 : 0)
				+ (_isVisibleAndAvailableSpeed ? 1 : 0)
				+ (_isVisibleAndAvailableTemperature ? 1 : 0)
				+ (_isVisibleAndAvailableTimeDuration ? 1 : 0)
				+ (_isVisibleAndAvailableTimeOfDay ? 1 : 0)
				+ (_isVisibleAndAvailableTimeSlice ? 1 : 0);
	}

	private void updateUI(final int valueIndex) {

		// get time when the redraw is requested
		final long requestedRedrawTime = System.currentTimeMillis();

		if (requestedRedrawTime > _lastUpdateUITime + 100) {

			// force a redraw

			updateUIRunnable(valueIndex);

		} else {

			_updateCounter[0]++;

			_shellContainer.getDisplay().asyncExec(new Runnable() {

				final int	__runnableCounter	= _updateCounter[0];

				public void run() {

					// update UI delayed
					if (__runnableCounter != _updateCounter[0]) {
						// a new update UI occured
						return;
					}

					updateUIRunnable(valueIndex);
				}
			});
		}

	}

	private void updateUIRunnable(int valueIndex) {

		if (_shellContainer == null || _shellContainer.isDisposed()) {
			return;
		}

		final int[] timeSerie = _tourData.timeSerie;

		// check bounds
		if (valueIndex >= timeSerie.length || valueIndex < 0) {
			valueIndex = timeSerie.length;
		}

		_currentValueIndex = valueIndex;

		if (_isVisibleAndAvailableAltimeter) {
			_lblAltimeter.setText(Integer.toString((int) _tourData.getAltimeterSerie()[valueIndex]));
		}

		if (_isVisibleAndAvailableAltitude) {
			_lblAltitude.setText(_nf1NoGroup.format(//
					_tourData.getAltitudeSmoothedSerie(false)[valueIndex]));
		}

		if (_isVisibleAndAvailableCadence) {
			_lblCadence.setText(Integer.toString((int) _tourData.cadenceSerie[valueIndex]));
		}

		if (_isVisibleAndAvailableChartZoomFactor) {
			_lblChartZoomFactor.setText(_nf1.format(_chartZoomFactor));
		}

		if (_isVisibleAndAvailableDistance) {

			final float distance = _tourData.distanceSerie[valueIndex] / 1000 / UI.UNIT_VALUE_DISTANCE;

			_lblDistance.setText(_nf3NoGroup.format(distance));
		}

		if (_isVisibleAndAvailableGradient) {
			_lblGradient.setText(_nf1.format(_tourData.getGradientSerie()[valueIndex]));
		}

		if (_isVisibleAndAvailablePace) {

			final long pace = (long) _tourData.getPaceSerieSeconds()[valueIndex];

			_lblPace.setText(new Formatter().format(//
					Messages.Tooltip_ValuePoint_Format_Pace,
					pace / 60,
					pace % 60)//
					.toString());
		}

		if (_isVisibleAndAvailablePower) {
			_lblPower.setText(Integer.toString((int) _tourData.getPowerSerie()[valueIndex]));
		}

		if (_isVisibleAndAvailablePulse) {
			_lblPulse.setText(Integer.toString((int) _tourData.pulseSerie[valueIndex]));
		}

		if (_isVisibleAndAvailableSpeed) {
			_lblSpeed.setText(_nf1.format(_tourData.getSpeedSerie()[valueIndex]));
		}

		if (_isVisibleAndAvailableTemperature) {

			float temperature = _tourData.temperatureSerie[valueIndex];

			if (UI.UNIT_VALUE_TEMPERATURE != 1) {
				// get imperial temperature
				temperature = temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD;
			}

			_lblTemperature.setText(_nf1.format(temperature));
		}

		if (_isVisibleAndAvailableTimeDuration) {
			_lblTimeDuration.setText(UI.format_hhh_mm_ss(timeSerie[valueIndex]));
		}

		if (_isVisibleAndAvailableTimeOfDay) {
			_lblTimeOfDay.setText(UI.format_hhh_mm_ss(_tourData.getStartTimeOfDay() + timeSerie[valueIndex]));
		}

		if (_isVisibleAndAvailableTimeSlice) {
			_lblDataSerieCurrent.setText(Integer.toString(_currentValueIndex));
			_lblDataSerieMax.setText(Integer.toString(timeSerie.length - 1));
		}

		_lastUpdateUITime = System.currentTimeMillis();
	}

}
