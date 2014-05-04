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
package net.tourbook.sign;

import net.tourbook.data.TourSign;
import net.tourbook.data.TourSignCategory;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class SignMenuManager {

	private class ActionTourSign extends Action {

		private final TourSign	__tourSign;

		public ActionTourSign(final TourSign tourSign) {

			super(tourSign.getSignName(), AS_PUSH_BUTTON);

			__tourSign = tourSign;

			// setup sign image
			setImageDescriptor(ImageDescriptor.createFromFile(null, tourSign.getImageFilePathName()));
		}

		@Override
		public void run() {

		}
	}

	private class ActionTourSignCategory extends Action implements IMenuCreator {

		private final TourSignCategory	__signCategory;

		private Menu					__categoryMenu;

		public ActionTourSignCategory(final TourSignCategory signCategory) {

			super(signCategory.getCategoryName(), AS_DROP_DOWN_MENU);

			__signCategory = signCategory;

			setMenuCreator(this);
		}

		public void dispose() {

			if (__categoryMenu != null) {
				__categoryMenu.dispose();
				__categoryMenu = null;
			}
		}

		public Menu getMenu(final Control parent) {
			return null;
		}

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

					final SignCollection signCollection = SignManager.getSignEntries(__signCategory.getCategoryId());

					// add actions
					createSignCategoryActions(signCollection, __categoryMenu);
					createSignActions(signCollection, __categoryMenu);
				}
			});

			return __categoryMenu;
		}
	}

	private void addActionToMenu(final Menu menu, final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	private void createSignActions(final SignCollection signCollection, final IMenuManager menuMgr) {

		// add sign items
		for (final TourSign menuTourSign : signCollection.tourSigns) {
			menuMgr.add(new ActionTourSign(menuTourSign));
		}
	}

	private void createSignActions(final SignCollection signCollection, final Menu menu) {

		// add sign items
		for (final TourSign menuTourSign : signCollection.tourSigns) {
			addActionToMenu(menu, new ActionTourSign(menuTourSign));
		}
	}

	private void createSignCategoryActions(final SignCollection signCollection, final IMenuManager menuMgr) {

		// add category items
		for (final TourSignCategory tourSignCategory : signCollection.tourSignCategories) {
			menuMgr.add(new ActionTourSignCategory(tourSignCategory));
		}
	}

	private void createSignCategoryActions(final SignCollection signCollection, final Menu menu) {

		// add category items
		for (final TourSignCategory tourSignCategory : signCollection.tourSignCategories) {
			addActionToMenu(menu, new ActionTourSignCategory(tourSignCategory));
		}
	}

	/**
	 * @param menuMgr
	 */
	public void fillSignMenu(final IMenuManager menuMgr) {

		createSignCategoryActions(SignManager.getRootSigns(), menuMgr);
		createSignActions(SignManager.getRootSigns(), menuMgr);
	}
}
