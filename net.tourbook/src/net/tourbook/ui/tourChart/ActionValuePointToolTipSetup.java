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
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

public class ActionValuePointToolTipSetup extends Action implements IMenuCreator {

	private ValuePointToolTip	_valuePointToolTip;

	private Menu				_menu	= null;

	public ActionValuePointToolTipSetup(final ValuePointToolTip valuePointToolTip) {
		
		super(null, Action.AS_PUSH_BUTTON);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__tour_options));

		setMenuCreator(this);

		_valuePointToolTip = valuePointToolTip;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Menu getMenu(final Menu parent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();
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
