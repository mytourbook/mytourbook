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
package net.tourbook.ui.tourChart.action;

import java.util.Map;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;

public class ActionHrZoneDropDownMenu extends Action implements IMenuCreator {

	private TourChart	_tourChart;
	private Menu		_menu;

	private TitleAction	_actionTitle	= new TitleAction(Messages.Tour_Action_HrZone_Title);

	private class TitleAction extends Action {

		public TitleAction(final String text) {
			super(text);
			setEnabled(false);
		}
	}

	public ActionHrZoneDropDownMenu(final TourChart tourChart) {

		super(null, Action.AS_DROP_DOWN_MENU);

		_tourChart = tourChart;

		setToolTipText(Messages.Tour_Action_ShowHrZones_Tooltip);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__PulseZones));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__PulseZones_Disabled));

		setMenuCreator(this);
	}

	private void addItem(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(_menu, -1);
	}

	public void dispose() {
		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	public Menu getMenu(final Control parent) {

		final Map<String, TCActionProxy> actionProxies = _tourChart.getActionProxies();

		_menu = new Menu(parent);

		addItem(_actionTitle);

		(new Separator()).fill(_menu, -1);
		addItem(actionProxies.get(TourChart.COMMAND_ID_HR_ZONE_STYLE_NO_GRADIENT).getAction());
		addItem(actionProxies.get(TourChart.COMMAND_ID_HR_ZONE_STYLE_WHITE_TOP).getAction());
		addItem(actionProxies.get(TourChart.COMMAND_ID_HR_ZONE_STYLE_WHITE_BOTTOM).getAction());
		addItem(actionProxies.get(TourChart.COMMAND_ID_HR_ZONE_STYLE_GRAPH_TOP).getAction());

		return _menu;
	}

	public Menu getMenu(final Menu parent) {
		return null;
	}

	@Override
	public void runWithEvent(final Event event) {
		_tourChart.actionShowHrZones();
	}
}
