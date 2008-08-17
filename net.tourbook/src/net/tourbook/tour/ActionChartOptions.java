/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.util.Map;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;

public class ActionChartOptions extends Action implements IMenuCreator {

	private Menu			fMenu	= null;

	private TourChart		fTourChart;

	private ToolBarManager	fTBM;

	public ActionChartOptions(final TourChart tourChart, final ToolBarManager tbm) {

		super(null, Action.AS_DROP_DOWN_MENU);

		fTourChart = tourChart;
		fTBM = tbm;

		setToolTipText(Messages.Tour_Action_chart_options_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__tour_options));

		setMenuCreator(this);
	}

	private void addItem(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(fMenu, -1);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(final Control parent) {

		final Map<String, TCActionProxy> actionProxies = fTourChart.fActionProxies;

		fMenu = new Menu(parent);

		addItem(actionProxies.get(TourChart.COMMAND_ID_SHOW_START_TIME).getAction());
		(new Separator()).fill(fMenu, -1);

		addItem(actionProxies.get(TourChart.COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER).getAction());
		addItem(actionProxies.get(TourChart.COMMAND_ID_CAN_MOVE_SLIDERS_WHN_ZOOMED).getAction());

		return fMenu;
	}

	public Menu getMenu(final Menu parent) {
		return null;
	}

	@Override
	public void runWithEvent(final Event event) {

		// show the drop-down menu, this only works in the runWithEvent not in the run method
		getMenuCreator().getMenu(fTBM.getControl()).setVisible(true);
	}

}
