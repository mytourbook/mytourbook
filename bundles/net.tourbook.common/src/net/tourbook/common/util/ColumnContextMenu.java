/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Add tag(s) from the selected tours
 */
public class ColumnContextMenu extends Action implements IMenuCreator {

	private Menu						_menu;
	private ColumnManager				_columnManager;

	private ArrayList<String>			_categories;
	private ArrayList<ColumnDefinition>	_categorizedColumns;

	private class ActionColumn extends Action {

		private ColumnDefinition	__colDef;

		public ActionColumn(final ColumnDefinition colDef, final String menuText) {

			super(menuText, AS_CHECK_BOX);

			__colDef = colDef;
		}

		@Override
		public void run() {
			_columnManager.action_AddColumn(__colDef);
		}
	}

	/**
	 * 
	 */
	private class ActionColumnCategory extends Action implements IMenuCreator {

		private Menu	__categoryMenu;
		private String	__category;

		public ActionColumnCategory(final String category) {

			super(category, AS_DROP_DOWN_MENU);

			__category = category;

			setMenuCreator(this);
		}

		@Override
		public void dispose() {
			if (__categoryMenu != null) {
				__categoryMenu.dispose();
				__categoryMenu = null;
			}
		}

		@Override
		public Menu getMenu(final Control parent) {
			return null;
		}

		@Override
		public Menu getMenu(final Menu parent) {

			dispose();
			__categoryMenu = new Menu(parent);

			// Add listener to repopulate the menu each time
			__categoryMenu.addMenuListener(new MenuAdapter() {
				@Override
				public void menuShown(final MenuEvent e) {

					final Menu menu = (Menu) e.widget;

					// dispose old items
					final MenuItem[] items = menu.getItems();
					for (final MenuItem item : items) {
						item.dispose();
					}

					// add actions
					createColumnActions(__categoryMenu, __category);
				}
			});

			return __categoryMenu;
		}
	}

	public ColumnContextMenu(	final Menu contextMenu,
								final ArrayList<String> categories,
								final ArrayList<ColumnDefinition> categorizedColumns,
								final ColumnManager columnManager) {

		_menu = contextMenu;
		_columnManager = columnManager;

		_categories = categories;
		_categorizedColumns = categorizedColumns;

		for (final String category : _categories) {
			addActionToMenu(_menu, new ActionColumnCategory(category));
		}
	}

	private void addActionToMenu(final Menu menu, final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	private void createColumnActions(final Menu menu, final String category) {

		final ArrayList<ColumnDefinition> categoryColDefs = new ArrayList<>();

		for (final ColumnDefinition colDef : _categorizedColumns) {

			if (category.equals(colDef.getColumnCategory())) {
				categoryColDefs.add(colDef);
			}
		}

		if (categoryColDefs.size() == 0) {
			return;
		}

		/*
		 * Create menu item for each column
		 */
		for (final ColumnDefinition colDef : categoryColDefs) {

			/*
			 * Create column text
			 */
			final String label = colDef.getColumnLabel();
			final String unit = colDef.getColumnUnit();

			final StringBuilder sb = new StringBuilder();

			// add label
			if (label != null) {
				sb.append(label);
			}

			// add unit
			if (unit != null) {

				if (sb.length() > 0) {
					sb.append(ColumnManager.COLUMN_TEXT_SEPARATOR);
				}

				sb.append(unit);
			}

			final ActionColumn columnAction = new ActionColumn(colDef, sb.toString());

			addActionToMenu(menu, columnAction);
		}
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
		return null;
	}

	@Override
	public Menu getMenu(final Menu parent) {
		return null;
	}

}
