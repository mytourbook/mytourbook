/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

public class ActionChartOptions extends Action implements IMenuCreator {

	private Menu		_menu	= null;

	private TourChart	_tourChart;

	public ActionChartOptions(final TourChart tourChart) {

		super(null, Action.AS_DROP_DOWN_MENU);

		_tourChart = tourChart;

		setToolTipText(Messages.Tour_Action_chart_options_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__tour_options));

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

		addItem(actionProxies.get(TourChart.COMMAND_ID_SHOW_START_TIME).getAction());
		addItem(actionProxies.get(TourChart.COMMAND_ID_SHOW_SRTM_DATA).getAction());
		(new Separator()).fill(_menu, -1);

		addItem(actionProxies.get(TourChart.COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER).getAction());
		addItem(actionProxies.get(TourChart.COMMAND_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED).getAction());

		return _menu;
	}

	public Menu getMenu(final Menu parent) {
		return null;
	}

	@Override
	public void runWithEvent(final Event event) {

		// open and position drop down menu below the action button
		final Widget item = event.widget;
		if (item instanceof ToolItem) {

			final ToolItem toolItem = (ToolItem) item;

			final IMenuCreator mc = getMenuCreator();
			if (mc != null) {

				final ToolBar toolBar = toolItem.getParent();

				final Menu menu = mc.getMenu(toolBar);
				if (menu != null) {

					final Rectangle toolItemBounds = toolItem.getBounds();
					Point topLeft = new Point(toolItemBounds.x, toolItemBounds.y + toolItemBounds.height);
					topLeft = toolBar.toDisplay(topLeft);

					menu.setLocation(topLeft.x, topLeft.y);
					menu.setVisible(true);
				}
			}
		}

	}

}
