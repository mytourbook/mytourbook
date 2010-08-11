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
package net.tourbook.application;

import net.tourbook.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

public class TourTypeDropDownMenu extends Action implements IMenuCreator {

	private Menu						_menu	= null;

	private TourTypeContributionItem	_tourTypeContributionItem;

	public TourTypeDropDownMenu(final TourTypeContributionItem tourTypeContributionItem) {

		super("tour type drop down menu", Action.AS_DROP_DOWN_MENU); //$NON-NLS-1$

		setToolTipText("tour type drop down menu tooltip"); //$NON-NLS-1$
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__tour_options));

		setMenuCreator(this);

		_tourTypeContributionItem = tourTypeContributionItem;

	}

	public void dispose() {
		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	public Menu getMenu(final Control parent) {

		dispose();
		_menu = new Menu(parent);

		// Add listener to repopulate the menu each time
		_menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(final MenuEvent e) {

				// dispose old menu items
				for (final MenuItem menuItem : ((Menu) e.widget).getItems()) {
					menuItem.dispose();
				}

//				fillMenu(_menu);
			}
		});

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
