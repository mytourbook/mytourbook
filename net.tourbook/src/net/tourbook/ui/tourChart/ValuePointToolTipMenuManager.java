/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

public class ValuePointToolTipMenuManager {

	static final int					VALUE_ID_ALTIMETER							= 1 << 1;
	static final int					VALUE_ID_ALTITUDE							= 1 << 2;
	static final int					VALUE_ID_CADENCE							= 1 << 3;
	static final int					VALUE_ID_DISTANCE							= 1 << 4;
	static final int					VALUE_ID_GRADIENT							= 1 << 5;
	static final int					VALUE_ID_PACE								= 1 << 6;
	static final int					VALUE_ID_POWER								= 1 << 7;
	static final int					VALUE_ID_PULSE								= 1 << 8;
	static final int					VALUE_ID_SPEED								= 1 << 9;
	static final int					VALUE_ID_TEMPERATURE						= 1 << 10;
	static final int					VALUE_ID_TIME_DURATION						= 1 << 11;
	static final int					VALUE_ID_TIME_OF_DAY						= 1 << 12;
	static final int					VALUE_ID_TIME_SLICES						= 1 << 13;
	static final int					VALUE_ID_CHART_ZOOM_FACTOR					= 1 << 14;
	static final int					VALUE_ID_GEARS								= 1 << 15;

	static final String					STATE_VALUE_POINT_TOOLTIP_VISIBLE_GRAPHS	= "ValuePoint_ToolTip_VisibleGraphs";		//$NON-NLS-1$
	static final String					STATE_VALUE_POINT_TOOLTIP_ORIENTATION		= "ValuePoint_ToolTip_Orientation";		//$NON-NLS-1$

	static final int					DEFAULT_GRAPHS								= VALUE_ID_TIME_SLICES
																							| VALUE_ID_TIME_DURATION
																							| VALUE_ID_DISTANCE
																					//
																					;
	static ValuePointToolTipOrientation	DEFAULT_ORIENTATION							= ValuePointToolTipOrientation.Horizontal;

	private IDialogSettings				_state;
	private TourData					_tourData;

	private ValuePointToolTipUI			_valuePointToolTipUI;

	private Menu						_menu										= null;

	private int							_allVisibleValueIds;
	private boolean						_isHorizontal;

	/**
	 * Parent of this tool item is the parent for the tooltip menu.
	 */
	private ToolItem					_menuParentItem;

	private ActionHideToolTip			_actionHideToolTip;
	private ActionSetDefaults			_actionSetDefaults;
	private ActionCloseTTContextMenu	_actionCloseTTContextMenu;

	private ActionOrientation			_actionHorizontalOrientation;
	private ActionOrientation			_actionVerticalOrientation;

	private ActionValueItem				_actionValueAltimeter;
	private ActionValueItem				_actionValueAltitude;
	private ActionValueItem				_actionValueCadence;
	private ActionValueItem				_actionValueChartZoomFactor;
	private ActionValueItem				_actionValueDistance;
	private ActionValueItem				_actionValueGears;
	private ActionValueItem				_actionValueGradient;
	private ActionValueItem				_actionValueHeader;
	private ActionValueItem				_actionValuePace;
	private ActionValueItem				_actionValuePower;
	private ActionValueItem				_actionValuePulse;
	private ActionValueItem				_actionValueSpeed;
	private ActionValueItem				_actionValueTemperature;
	private ActionValueItem				_actionValueTimeDuration;
	private ActionValueItem				_actionValueTimeOfDay;
	private ActionValueItem				_actionValueTimeSlices;

	private Action						_actionPinLocationHeader;
	private ActionPinLocation			_actionPinLocationScreen;
	private ActionPinLocation			_actionPinLocationTopRight;
	private ActionPinLocation			_actionPinLocationTopLeft;
	private ActionPinLocation			_actionPinLocationBottomLeft;
	private ActionPinLocation			_actionPinLocationBottomRight;
	private ActionPinLocation			_actionPinLocationMouseXPosition;

	private final class ActionCloseTTContextMenu extends Action {

		public ActionCloseTTContextMenu() {
			super(Messages.Tooltip_ValuePoint_Action_CloseContextMenu);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Cancel));
		}

		@Override
		public void run() {
			_menu.setVisible(false);
		}
	}

	private class ActionHideToolTip extends Action {

		public ActionHideToolTip() {
			setText(Messages.Action_ToolTip_Hide);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Cancel));
		}

		@Override
		public void run() {
			_valuePointToolTipUI.actionHideToolTip();
		}
	}

	private final class ActionOrientation extends Action {

		private ValuePointToolTipOrientation	_orientation;

		public ActionOrientation(final ValuePointToolTipOrientation orientation) {

			if (orientation == ValuePointToolTipOrientation.Horizontal) {
				setText(Messages.Tooltip_ValuePoint_Action_Orientation_Horizontal);
			} else {
				setText(Messages.Tooltip_ValuePoint_Action_Orientation_Vertical);
			}

			_orientation = orientation;
		}

		@Override
		public void run() {

			_state.put(STATE_VALUE_POINT_TOOLTIP_ORIENTATION, _orientation.name());

			_valuePointToolTipUI.actionOrientation(_orientation, true);
		}
	}

	private class ActionPinLocation extends Action {

		public ValuePointToolTipPinLocation	_locationId;

		public ActionPinLocation(final ValuePointToolTipPinLocation locationId, final String text) {
			setText(text);
			_locationId = locationId;
		}

		@Override
		public void run() {

			_valuePointToolTipUI.actionPinLocation(_locationId);

			// reopen tooltip menu with the new location
			if (_locationId == ValuePointToolTipPinLocation.TopLeft
					|| _locationId == ValuePointToolTipPinLocation.TopRight
					|| _locationId == ValuePointToolTipPinLocation.BottomLeft
					|| _locationId == ValuePointToolTipPinLocation.BottomRight
			//
			) {
// this is very annoying
//				openToolTipMenu10Reopen();
			}
		}
	}

	private class ActionSetDefaults extends Action {

		public ActionSetDefaults() {
			setText(Messages.Action_ToolTip_SetDefaults);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__undo_edit));
		}

		@Override
		public void run() {

			/*
			 * set defaults into the state
			 */
			final ValuePointToolTipOrientation orientation = ValuePointToolTipMenuManager.DEFAULT_ORIENTATION;
			_state.put(STATE_VALUE_POINT_TOOLTIP_ORIENTATION, orientation.name());

			_allVisibleValueIds = DEFAULT_GRAPHS;
			_state.put(STATE_VALUE_POINT_TOOLTIP_VISIBLE_GRAPHS, _allVisibleValueIds);

			// update tooltip with default values
			_valuePointToolTipUI.actionSetDefaults(_allVisibleValueIds, orientation);
		}
	}

	private class ActionValueItem extends Action {

		private int				_graphId;

		private ImageDescriptor	_graphImage;
		private ImageDescriptor	_graphImageDisabled;

		public ActionValueItem(	final int graphId,
								final String name,
								final String graphImageName,
								final String graphImageNameDisabled) {

			super(name, AS_CHECK_BOX);

			_graphId = graphId;

			if (graphImageName != null) {
				_graphImage = TourbookPlugin.getImageDescriptor(graphImageName);
			}
			if (graphImageNameDisabled != null) {
				_graphImageDisabled = TourbookPlugin.getImageDescriptor(graphImageNameDisabled);
			}
		}

		@Override
		public void run() {
			actionValueItem(_graphId, isChecked());
		}

		private void setState(final boolean isChecked, final boolean isEnabled) {

			setEnabled(isEnabled);

			// show checked state only when also enabled
			setChecked(isChecked && isEnabled);

			setImageDescriptor(isEnabled ? _graphImage : _graphImageDisabled);
		}
	}

	public ValuePointToolTipMenuManager(final ValuePointToolTipUI valuePointToolTipUI, final IDialogSettings state) {

		_valuePointToolTipUI = valuePointToolTipUI;
		_state = state;

		createActions();
	}

	/**
	 * Show/hide value and reopen the tool tip with the new or hidden value.
	 * 
	 * @param graphId
	 *            Graph id which should be displayed/hidden.
	 * @param isChecked
	 */
	private void actionValueItem(final int graphId, final boolean isChecked) {

		final int currentVisibleValues = _allVisibleValueIds;

		if (isChecked) {

			// display additional graph

			_allVisibleValueIds = currentVisibleValues | graphId;

		} else {

			// remove graph

			/**
			 * <pre>
			 * a = 0011
			 * b = 0110
			 * a|b = 0111
			 * a&b = 0010
			 * a^b = 0101
			 * ~a&b|a&~b = 0101
			 * ~a = 1100
			 * </pre>
			 */
			_allVisibleValueIds = (~currentVisibleValues & graphId) | (currentVisibleValues & ~graphId);
		}

		_state.put(STATE_VALUE_POINT_TOOLTIP_VISIBLE_GRAPHS, _allVisibleValueIds);

		// update tooltip with new/removed graphs
		final ToolItem toolItem = _valuePointToolTipUI.actionVisibleValues(_allVisibleValueIds);

		// reopen context menu
		openToolTipMenu10Reopen(toolItem);
	}

	private void addItem(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(_menu, -1);
	}

	private void createActions() {

		_actionHideToolTip = new ActionHideToolTip();
		_actionSetDefaults = new ActionSetDefaults();
		_actionCloseTTContextMenu = new ActionCloseTTContextMenu();
		_actionHorizontalOrientation = new ActionOrientation(ValuePointToolTipOrientation.Horizontal);
		_actionVerticalOrientation = new ActionOrientation(ValuePointToolTipOrientation.Vertical);

		createPinActions();
		createGraphActions();
	}

	private void createGraphActions() {

		_actionValueHeader = new ActionValueItem(//
				-1,
				Messages.Tooltip_ValuePoint_Action_Value_Header,
				null,
				null);

		_actionValueTimeSlices = new ActionValueItem(//
				VALUE_ID_TIME_SLICES,
				Messages.Tooltip_ValuePoint_Action_Value_TimeSlices,
				null,
				null);

		_actionValueChartZoomFactor = new ActionValueItem(//
				VALUE_ID_CHART_ZOOM_FACTOR,
				Messages.Tooltip_ValuePoint_Action_Value_ChartZoomFactor,
				null,
				null);

		_actionValueTimeDuration = new ActionValueItem(//
				VALUE_ID_TIME_DURATION,
				Messages.Tooltip_ValuePoint_Action_Value_TimeDuration,
				null,
				null);

		_actionValueTimeOfDay = new ActionValueItem(//
				VALUE_ID_TIME_OF_DAY,
				Messages.Tooltip_ValuePoint_Action_Value_TimeOfDay,
				null,
				null);

		_actionValueDistance = new ActionValueItem(
				VALUE_ID_DISTANCE,
				Messages.Tooltip_ValuePoint_Action_Value_Distance,
				null,
				null);

		_actionValueAltitude = new ActionValueItem(
				VALUE_ID_ALTITUDE,
				Messages.Tooltip_ValuePoint_Action_Value_Altitude,
				Messages.Image__graph_altitude,
				Messages.Image__graph_altitude_disabled);

		_actionValueAltimeter = new ActionValueItem(
				VALUE_ID_ALTIMETER,
				Messages.Tooltip_ValuePoint_Action_Value_Altimeter,
				Messages.Image__graph_altimeter,
				Messages.Image__graph_altimeter_disabled);

		_actionValueCadence = new ActionValueItem(
				VALUE_ID_CADENCE,
				Messages.Tooltip_ValuePoint_Action_Value_Cadence,
				Messages.Image__graph_cadence,
				Messages.Image__graph_cadence_disabled);

		_actionValueGears = new ActionValueItem(
				VALUE_ID_GEARS,
				Messages.Tooltip_ValuePoint_Action_Value_Gears,
				Messages.Image__Graph_Gears,
				Messages.Image__Graph_Gears_disabled);

		_actionValueGradient = new ActionValueItem(
				VALUE_ID_GRADIENT,
				Messages.Tooltip_ValuePoint_Action_Value_Gradient,
				Messages.Image__graph_gradient,
				Messages.Image__graph_gradient_disabled);

		_actionValuePace = new ActionValueItem(
				VALUE_ID_PACE,
				Messages.Tooltip_ValuePoint_Action_Value_Pace,
				Messages.Image__graph_pace,
				Messages.Image__graph_pace_disabled);

		_actionValuePower = new ActionValueItem(
				VALUE_ID_POWER,
				Messages.Tooltip_ValuePoint_Action_Value_Power,
				Messages.Image__graph_power,
				Messages.Image__graph_power_disabled);

		_actionValuePulse = new ActionValueItem(
				VALUE_ID_PULSE,
				Messages.Tooltip_ValuePoint_Action_Value_Pulse,
				Messages.Image__graph_heartbeat,
				Messages.Image__graph_heartbeat_disabled);

		_actionValueSpeed = new ActionValueItem(
				VALUE_ID_SPEED,
				Messages.Tooltip_ValuePoint_Action_Value_Speed,
				Messages.Image__graph_speed,
				Messages.Image__graph_speed_disabled);

		_actionValueTemperature = new ActionValueItem(
				VALUE_ID_TEMPERATURE,
				Messages.Tooltip_ValuePoint_Action_Value_Temperature,
				Messages.Image__graph_temperature,
				Messages.Image__graph_temperature_disabled);
	}

	private void createPinActions() {

		_actionPinLocationHeader = new Action(Messages.Tooltip_ValuePoint_Action_PinLocation_Header) {};

		_actionPinLocationScreen = new ActionPinLocation(
				ValuePointToolTipPinLocation.Screen,
				Messages.Tooltip_ValuePoint_Action_PinLocation_Screen);

		_actionPinLocationTopLeft = new ActionPinLocation(
				ValuePointToolTipPinLocation.TopLeft,
				Messages.Tooltip_ValuePoint_Action_PinLocation_TopLeft);

		_actionPinLocationTopRight = new ActionPinLocation(
				ValuePointToolTipPinLocation.TopRight,
				Messages.Tooltip_ValuePoint_Action_PinLocation_TopRight);

		_actionPinLocationBottomLeft = new ActionPinLocation(
				ValuePointToolTipPinLocation.BottomLeft,
				Messages.Tooltip_ValuePoint_Action_PinLocation_BottomLeft);

		_actionPinLocationBottomRight = new ActionPinLocation(
				ValuePointToolTipPinLocation.BottomRight,
				Messages.Tooltip_ValuePoint_Action_PinLocation_BottomRight);

		_actionPinLocationMouseXPosition = new ActionPinLocation(
				ValuePointToolTipPinLocation.MouseXPosition,
				Messages.Tooltip_ValuePoint_Action_PinLocation_MouseXPosition);
	}

	void dispose() {
		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	private void enableActions() {

		final ValuePointToolTipPinLocation pinnedLocation = _valuePointToolTipUI.getPinnedLocation();

		_actionPinLocationHeader.setEnabled(false);

		_actionPinLocationScreen.setChecked(pinnedLocation == ValuePointToolTipPinLocation.Screen);
		_actionPinLocationTopLeft.setChecked(pinnedLocation == ValuePointToolTipPinLocation.TopLeft);
		_actionPinLocationTopRight.setChecked(pinnedLocation == ValuePointToolTipPinLocation.TopRight);
		_actionPinLocationBottomLeft.setChecked(pinnedLocation == ValuePointToolTipPinLocation.BottomLeft);
		_actionPinLocationBottomRight.setChecked(pinnedLocation == ValuePointToolTipPinLocation.BottomRight);
		_actionPinLocationMouseXPosition.setChecked(pinnedLocation == ValuePointToolTipPinLocation.MouseXPosition);

		_actionValueHeader.setEnabled(false);

		_actionValueAltimeter.setState(
				(_allVisibleValueIds & VALUE_ID_ALTIMETER) > 0,
				_tourData.getAltimeterSerie() != null);

		_actionValueAltitude.setState( //
				(_allVisibleValueIds & VALUE_ID_ALTITUDE) > 0,
				_tourData.getAltitudeSerie() != null);

		_actionValueCadence.setState( //
				(_allVisibleValueIds & VALUE_ID_CADENCE) > 0,
				_tourData.cadenceSerie != null);

		_actionValueChartZoomFactor.setState( //
				(_allVisibleValueIds & VALUE_ID_CHART_ZOOM_FACTOR) > 0,
				true);

		_actionValueDistance.setState( //
				(_allVisibleValueIds & VALUE_ID_DISTANCE) > 0,
				_tourData.distanceSerie != null);

		_actionValueGears.setState( //
				(_allVisibleValueIds & VALUE_ID_GEARS) > 0,
				_tourData.getGears() != null);

		_actionValueGradient.setState( //
				(_allVisibleValueIds & VALUE_ID_GRADIENT) > 0,
				_tourData.getGradientSerie() != null);

		_actionValuePace.setState( //
				(_allVisibleValueIds & VALUE_ID_PACE) > 0,
				_tourData.getPaceSerie() != null);

		_actionValuePower.setState( //
				(_allVisibleValueIds & VALUE_ID_POWER) > 0,
				_tourData.getPowerSerie() != null);

		_actionValuePulse.setState( //
				(_allVisibleValueIds & VALUE_ID_PULSE) > 0,
				_tourData.pulseSerie != null);

		_actionValueSpeed.setState( //
				(_allVisibleValueIds & VALUE_ID_SPEED) > 0,
				_tourData.getSpeedSerie() != null);

		_actionValueTemperature.setState( //
				(_allVisibleValueIds & VALUE_ID_TEMPERATURE) > 0,
				_tourData.temperatureSerie != null);

		_actionValueTimeDuration.setState( //
				(_allVisibleValueIds & VALUE_ID_TIME_DURATION) > 0,
				_tourData.timeSerie != null);

		_actionValueTimeOfDay.setState( //
				(_allVisibleValueIds & VALUE_ID_TIME_OF_DAY) > 0,
				_tourData.timeSerie != null);

		_actionValueTimeSlices.setState( //
				(_allVisibleValueIds & VALUE_ID_TIME_SLICES) > 0,
				true);
	}

	private Menu getMenu(final Control parent) {

		// recreate menu each time
		if (_menu != null) {
			_menu.dispose();
		}

		// !!! actions must be checked before they are added otherwise they are not checked
		enableActions();

		_menu = new Menu(parent);

//		(new Separator()).fill(_menu, -1);
		addItem(_actionValueHeader);

		addItem(_actionValueTimeSlices);
		addItem(_actionValueTimeDuration);
		addItem(_actionValueTimeOfDay);
		addItem(_actionValueDistance);
		addItem(_actionValueAltitude);
		addItem(_actionValuePulse);
		addItem(_actionValueSpeed);
		addItem(_actionValuePace);
		addItem(_actionValuePower);
		addItem(_actionValueTemperature);
		addItem(_actionValueGradient);
		addItem(_actionValueAltimeter);
		addItem(_actionValueCadence);
		addItem(_actionValueGears);
		addItem(_actionValueChartZoomFactor);
		addItem(_actionCloseTTContextMenu);

		(new Separator()).fill(_menu, -1);
		addItem(_actionPinLocationHeader);
		addItem(_actionPinLocationMouseXPosition);
		addItem(_actionPinLocationTopLeft);
		addItem(_actionPinLocationTopRight);
		addItem(_actionPinLocationBottomLeft);
		addItem(_actionPinLocationBottomRight);
		addItem(_actionPinLocationScreen);

		(new Separator()).fill(_menu, -1);

		// show the other orientation
		if (_isHorizontal) {
			addItem(_actionVerticalOrientation);
		} else {
			addItem(_actionHorizontalOrientation);
		}
		addItem(_actionSetDefaults);
		addItem(_actionHideToolTip);

		return _menu;
	}

	void hideContextMenu() {

		if (_menu != null && !_menu.isDisposed()) {
			_menu.setVisible(false);
		}
	}

	/**
	 * Open tooltip context menu.
	 * 
	 * @param event
	 * @param tourData
	 * @param allVisibleValueIds
	 * @param isHorizontal
	 */
	void openToolTipMenu(	final Event event,
							final TourData tourData,
							final int allVisibleValueIds,
							final boolean isHorizontal) {

		_tourData = tourData;

		_allVisibleValueIds = allVisibleValueIds;
		_isHorizontal = isHorizontal;

		// open and position drop down menu below the action button
		final Widget item = event.widget;
		if (item instanceof ToolItem) {
			openToolTipMenu10Reopen((ToolItem) item);
		}
	}

	private void openToolTipMenu10Reopen(final ToolItem toolItem) {

		_menuParentItem = toolItem;

		final ToolBar menuParentControl = _menuParentItem.getParent();
		final Rectangle toolItemBounds = _menuParentItem.getBounds();

		Point topLeft = new Point(toolItemBounds.x, toolItemBounds.y + toolItemBounds.height);
		topLeft = menuParentControl.toDisplay(topLeft);

		final Menu menu = getMenu(menuParentControl);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}
}
