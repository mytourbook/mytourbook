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

import net.tourbook.ui.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

public class ToolTipValuePointMenuManager implements IMenuCreator {

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

	private ToolTipValuePointUI		_valuePointToolTipUI;

	private Menu					_menu					= null;

	/**
	 * Item which parent is the parent for the tooltip menu.
	 */
	private ToolItem				_menuParentItem;

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

	private class ActionTooltipGraphItem extends Action {

		private int	_actionId;

		public ActionTooltipGraphItem(final String name, final int actionId) {

			super(name, AS_CHECK_BOX);

			_actionId = actionId;
		}

		@Override
		public void run() {
			displayItem(_actionId, isChecked());
		}
	}

	public ToolTipValuePointMenuManager(final ToolTipValuePointUI valuePointToolTipUI) {

		_valuePointToolTipUI = valuePointToolTipUI;

		createActions();
	}

	private void addItem(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(_menu, -1);
	}

	private void createActions() {

		_actionGraphAltitude = new ActionTooltipGraphItem(Messages.Action_Tooltip_Altitude, ACTION_ID_ALTITUDE);
		_actionGraphAltimeter = new ActionTooltipGraphItem(Messages.Action_Tooltip_Altimeter, ACTION_ID_ALTIMETER);
		_actionGraphCadence = new ActionTooltipGraphItem(Messages.Action_Tooltip_Cadence, ACTION_ID_CADENCE);
		_actionGraphDistance = new ActionTooltipGraphItem(Messages.Action_Tooltip_Distance, ACTION_ID_DISTANCE);
		_actionGraphGradient = new ActionTooltipGraphItem(Messages.Action_Tooltip_Gradient, ACTION_ID_GRADIENT);
		_actionGraphPace = new ActionTooltipGraphItem(Messages.Action_Tooltip_Pace, ACTION_ID_PACE);
		_actionGraphPower = new ActionTooltipGraphItem(Messages.Action_Tooltip_Power, ACTION_ID_POWER);
		_actionGraphPulse = new ActionTooltipGraphItem(Messages.Action_Tooltip_Pulse, ACTION_ID_PULSE);
		_actionGraphSpeed = new ActionTooltipGraphItem(Messages.Action_Tooltip_Speed, ACTION_ID_SPEED);
		_actionGraphTemperature = new ActionTooltipGraphItem(Messages.Action_Tooltip_Temperature, ACTION_ID_TEMPERATURE);
		_actionGraphTime = new ActionTooltipGraphItem(Messages.Action_Tooltip_Time, ACTION_ID_TIME);

	}

	private void displayItem(final int actionId, final boolean isChecked) {
		openToolTipMenu10();
	}

	public void dispose() {
		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
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

			openToolTipMenu10();
		}
	}

	private void openToolTipMenu10() {

		final ToolBar menuParentControl = _menuParentItem.getParent();
		final Menu menu = getMenu(menuParentControl);

		final Rectangle toolItemBounds = _menuParentItem.getBounds();
		Point topLeft = new Point(toolItemBounds.x, toolItemBounds.y + toolItemBounds.height);
		topLeft = menuParentControl.toDisplay(topLeft);

		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}
}
