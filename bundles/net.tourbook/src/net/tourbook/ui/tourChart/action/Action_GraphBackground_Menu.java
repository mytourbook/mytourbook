/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.tourChart.TourChart;

public class Action_GraphBackground_Menu extends Action implements IMenuCreator {

	private TourChart		_tourChart;
	private Menu			_menu;

	private TitleAction	_titleAction_GraphBackground_Style	= new TitleAction(Messages.Tour_Action_Title_GraphBackground_Style);
	private TitleAction	_titleAction_GraphBackground_Source	= new TitleAction(Messages.Tour_Action_Title_GraphBackground_Source);

	private class TitleAction extends Action {

		public TitleAction(final String text) {
			super(text);
			setEnabled(false);
		}
	}

	public Action_GraphBackground_Menu(final TourChart tourChart) {

		super(null, Action.AS_DROP_DOWN_MENU);

		_tourChart = tourChart;

		setToolTipText(Messages.Tour_Action_ShowGraphBackground_Tooltip);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__PulseZones));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__PulseZones_Disabled));

		setMenuCreator(this);
	}

	private void addItem(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(_menu, -1);
	}

	@Override
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

		final Map<String, Action> actionProxies = _tourChart.getTourChartActions();

		addItem(_titleAction_GraphBackground_Source);

		addItem(actionProxies.get(TourChart.ACTION_ID_GRAPH_BG_SOURCE_HR_ZONE));
		addItem(actionProxies.get(TourChart.ACTION_ID_GRAPH_BG_SOURCE_SWIM_STYLE));

		(new Separator()).fill(_menu, -1);
		addItem(_titleAction_GraphBackground_Style);

		addItem(actionProxies.get(TourChart.ACTION_ID_GRAPH_BG_STYLE_NO_GRADIENT));
		addItem(actionProxies.get(TourChart.ACTION_ID_GRAPH_BG_STYLE_WHITE_TOP));
		addItem(actionProxies.get(TourChart.ACTION_ID_GRAPH_BG_STYLE_WHITE_BOTTOM));
		addItem(actionProxies.get(TourChart.ACTION_ID_GRAPH_BG_STYLE_GRAPH_TOP));

		return _menu;
	}

	@Override
	public Menu getMenu(final Menu parent) {
		return null;
	}

	@Override
	public void runWithEvent(final Event event) {
		_tourChart.action_ShowGraphBgStyle();
	}
}
