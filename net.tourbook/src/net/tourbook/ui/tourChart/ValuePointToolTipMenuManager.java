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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

public class ValuePointToolTipMenuManager implements IMenuCreator {

	private static final int		ACTION_ID_ALTITUDE		= 10;

	private static final int		ACTION_ID_ALTIMETER		= 20;
	private static final int		ACTION_ID_CADENCE		= 30;
	private static final int		ACTION_ID_DISTANCE		= 40;
	private static final int		ACTION_ID_GRADIENT		= 50;
	private static final int		ACTION_ID_PACE			= 60;
	private static final int		ACTION_ID_POWER			= 70;
	private static final int		ACTION_ID_PULSE			= 80;
	private static final int		ACTION_ID_SPEED			= 90;
	private static final int		ACTION_ID_TIME			= 100;
	private static final int		ACTION_ID_TEMPERATURE	= 110;

	private ValuePointToolTipUI		_valuePointToolTipUI;

	private Menu					_menu					= null;

	/**
	 * Parent of this tool item is the parent for the tooltip menu.
	 */
	private ToolItem				_menuParentItem;

	private ActionHideToolTip		_actionHideToolTip;

	private ActionTooltipGraphItem	_actionGraphTime;
	private ActionTooltipGraphItem	_actionGraphDistance;

	private ActionTooltipGraphItem	_actionGraphAltitude;
	private ActionTooltipGraphItem	_actionGraphAltimeter;
	private ActionTooltipGraphItem	_actionGraphCadence;
	private ActionTooltipGraphItem	_actionGraphGradient;
	private ActionTooltipGraphItem	_actionGraphPace;
	private ActionTooltipGraphItem	_actionGraphPower;
	private ActionTooltipGraphItem	_actionGraphPulse;
	private ActionTooltipGraphItem	_actionGraphSpeed;
	private ActionTooltipGraphItem	_actionGraphTemperature;

	private ActionPinLocation		_actionPinLocationHeader;
	private ActionPinLocation		_actionPinLocationDisabled;
	private ActionPinLocation		_actionPinLocationTopRight;
	private ActionPinLocation		_actionPinLocationTopLeft;
	private ActionPinLocation		_actionPinLocationBottomLeft;
	private ActionPinLocation		_actionPinLocationBottomRight;

	private class ActionHideToolTip extends Action {

		public ActionHideToolTip() {
			setText(Messages.Action_ToolTip_Hide);
		}

		@Override
		public void run() {
			_valuePointToolTipUI.actionHideToolTip();
		}
	}

	private class ActionPinLocation extends Action {

		private int	_locationId;

		public ActionPinLocation(final String text, final int locationId) {
			setText(text);
			_locationId = locationId;
		}

		@Override
		public void run() {

			_valuePointToolTipUI.actionPinLocation(_locationId);

			// reopen tooltip menu with the new location
			if (_locationId == ValuePointToolTipShell.PIN_LOCATION_TOP_LEFT
					|| _locationId == ValuePointToolTipShell.PIN_LOCATION_TOP_RIGHT
					|| _locationId == ValuePointToolTipShell.PIN_LOCATION_BOTTOM_LEFT
					|| _locationId == ValuePointToolTipShell.PIN_LOCATION_BOTTOM_RIGHT
			//
			) {
// this is very annoying
//				openToolTipMenu10Reopen();
			}
		}
	}

	private class ActionTooltipGraphItem extends Action {

		private int				_actionId;

		private ImageDescriptor	_graphImage;
		private ImageDescriptor	_graphImageDisabled;

		public ActionTooltipGraphItem(	final int actionId,
										final String name,
										final String graphImageName,
										final String graphImageNameDisabled) {

			super(name, AS_CHECK_BOX);

			_actionId = actionId;

			if (graphImageName != null) {
				_graphImage = TourbookPlugin.getImageDescriptor(graphImageName);
			}
			if (graphImageNameDisabled != null) {
				_graphImageDisabled = TourbookPlugin.getImageDescriptor(graphImageNameDisabled);
			}
		}

		@Override
		public void run() {
			displayItem(_actionId, isChecked());
		}
	}

	public ValuePointToolTipMenuManager(final ValuePointToolTipUI valuePointToolTipUI) {

		_valuePointToolTipUI = valuePointToolTipUI;

		createActions();
	}

	private void addItem(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(_menu, -1);
	}

	private void createActions() {

		_actionHideToolTip = new ActionHideToolTip();

		_actionPinLocationHeader = new ActionPinLocation(//
				Messages.Action_ToolTip_PinLocation_Header,
				-1);

		_actionPinLocationDisabled = new ActionPinLocation(
				Messages.Action_ToolTip_PinLocation_Disabled,
				ValuePointToolTipShell.PIN_LOCATION_DISABLED);

		_actionPinLocationTopLeft = new ActionPinLocation(
				Messages.Action_ToolTip_PinLocation_TopLeft,
				ValuePointToolTipShell.PIN_LOCATION_TOP_LEFT);

		_actionPinLocationTopRight = new ActionPinLocation(
				Messages.Action_ToolTip_PinLocation_TopRight,
				ValuePointToolTipShell.PIN_LOCATION_TOP_RIGHT);

		_actionPinLocationBottomLeft = new ActionPinLocation(
				Messages.Action_ToolTip_PinLocation_BottomLeft,
				ValuePointToolTipShell.PIN_LOCATION_BOTTOM_LEFT);

		_actionPinLocationBottomRight = new ActionPinLocation(
				Messages.Action_ToolTip_PinLocation_BottomRight,
				ValuePointToolTipShell.PIN_LOCATION_BOTTOM_RIGHT);

		createGraphActions();
	}

	private void createGraphActions() {
		_actionGraphTime = new ActionTooltipGraphItem(//
				ACTION_ID_TIME,
				Messages.Action_ToolTip_Graph_Time,
				null,
				null);

		_actionGraphDistance = new ActionTooltipGraphItem(
				ACTION_ID_DISTANCE,
				Messages.Action_ToolTip_Graph_Distance,
				null,
				null);

		_actionGraphAltitude = new ActionTooltipGraphItem(
				ACTION_ID_ALTITUDE,
				Messages.Action_ToolTip_Graph_Altitude,
				net.tourbook.Messages.Image__graph_altitude,
				net.tourbook.Messages.Image__graph_altitude_disabled);

		_actionGraphAltimeter = new ActionTooltipGraphItem(
				ACTION_ID_ALTIMETER,
				Messages.Action_ToolTip_Graph_Altimeter,
				net.tourbook.Messages.Image__graph_altimeter,
				net.tourbook.Messages.Image__graph_altimeter_disabled);

		_actionGraphCadence = new ActionTooltipGraphItem(
				ACTION_ID_CADENCE,
				Messages.Action_ToolTip_Graph_Cadence,
				net.tourbook.Messages.Image__graph_cadence,
				net.tourbook.Messages.Image__graph_cadence_disabled);

		_actionGraphGradient = new ActionTooltipGraphItem(
				ACTION_ID_GRADIENT,
				Messages.Action_ToolTip_Graph_Gradient,
				net.tourbook.Messages.Image__graph_gradient,
				net.tourbook.Messages.Image__graph_gradient_disabled);

		_actionGraphPace = new ActionTooltipGraphItem(
				ACTION_ID_PACE,
				Messages.Action_ToolTip_Graph_Pace,
				net.tourbook.Messages.Image__graph_pace,
				net.tourbook.Messages.Image__graph_pace_disabled);

		_actionGraphPower = new ActionTooltipGraphItem(
				ACTION_ID_POWER,
				Messages.Action_ToolTip_Graph_Power,
				net.tourbook.Messages.Image__graph_power,
				net.tourbook.Messages.Image__graph_power_disabled);

		_actionGraphPulse = new ActionTooltipGraphItem(
				ACTION_ID_PULSE,
				Messages.Action_ToolTip_Graph_Pulse,
				net.tourbook.Messages.Image__graph_heartbeat,
				net.tourbook.Messages.Image__graph_heartbeat_disabled);

		_actionGraphSpeed = new ActionTooltipGraphItem(
				ACTION_ID_SPEED,
				Messages.Action_ToolTip_Graph_Speed,
				net.tourbook.Messages.Image__graph_speed,
				net.tourbook.Messages.Image__graph_speed_disabled);

		_actionGraphTemperature = new ActionTooltipGraphItem(
				ACTION_ID_TEMPERATURE,
				Messages.Action_ToolTip_Graph_Temperature,
				net.tourbook.Messages.Image__graph_temperature,
				net.tourbook.Messages.Image__graph_temperature_disabled);
	}

	private void displayItem(final int actionId, final boolean isChecked) {
		openToolTipMenu10Reopen();
	}

	public void dispose() {
		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	private void enableActions() {

		_actionPinLocationHeader.setEnabled(false);
	}

	@Override
	public Menu getMenu(final Control parent) {

		// recreate menu each time
		if (_menu != null) {
			_menu.dispose();
		}

		_menu = new Menu(parent);

		addItem(_actionGraphAltimeter);
		addItem(_actionGraphAltitude);
		addItem(_actionGraphCadence);
		addItem(_actionGraphDistance);
		addItem(_actionGraphGradient);
		addItem(_actionGraphPace);
		addItem(_actionGraphPower);
		addItem(_actionGraphPulse);
		addItem(_actionGraphSpeed);
		addItem(_actionGraphTemperature);
		addItem(_actionGraphTime);

		(new Separator()).fill(_menu, -1);
		addItem(_actionPinLocationHeader);
		addItem(_actionPinLocationTopLeft);
		addItem(_actionPinLocationTopRight);
		addItem(_actionPinLocationBottomLeft);
		addItem(_actionPinLocationBottomRight);
		addItem(_actionPinLocationDisabled);

		(new Separator()).fill(_menu, -1);
		addItem(_actionHideToolTip);

		enableActions();

		return _menu;
	}

	@Override
	public Menu getMenu(final Menu parent) {
		// not used
		return null;
	}

	/**
	 * Open tooltip context menu.
	 * 
	 * @param event
	 */
	public void openToolTipMenu(final Event event) {

		// open and position drop down menu below the action button
		final Widget item = event.widget;
		if (item instanceof ToolItem) {

			_menuParentItem = (ToolItem) item;

			openToolTipMenu10Reopen();
		}
	}

	private void openToolTipMenu10Reopen() {

		final ToolBar menuParentControl = _menuParentItem.getParent();
		final Menu menu = getMenu(menuParentControl);

		final Rectangle toolItemBounds = _menuParentItem.getBounds();
		Point topLeft = new Point(toolItemBounds.x, toolItemBounds.y + toolItemBounds.height);
		topLeft = menuParentControl.toDisplay(topLeft);

		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}
}
