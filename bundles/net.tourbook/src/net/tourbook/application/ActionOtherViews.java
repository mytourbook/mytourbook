/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ContributionItemFactory;

/**
 * Submenu to display other... views
 */
public class ActionOtherViews extends Action implements IMenuCreator {

	private IContributionItem	_actionViewShortList;

	private Menu				_menu;

	/**
	 * @param window
	 */
	public ActionOtherViews(final IWorkbenchWindow window) {

		super(Messages.App_Action_OpenOtherViews, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		_actionViewShortList = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
	}

	public void dispose() {
		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		dispose();

		_menu = new Menu(parent);

		_actionViewShortList.fill(_menu, -1);

		return _menu;
	}

}
