/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Add or removes a tag from the selected tours
 */
public class ActionSetTourTag extends Action implements IMenuCreator {

	private Menu					fMenu;

	private final ISelectedTours	fTourProvider;
	private final boolean			fIsAddMode;

	/**
	 * contains the tags for all selected tours in the viewer
	 */
	private Set<TourTag>			fSelectedTags;

	private ArrayList<TourData>		fSelectedTours;

	public class ActionTagCategory extends Action implements IMenuCreator {

		private Menu			fCategoryMenu;
		private TourTagCategory	fTagCategory;

		public ActionTagCategory(final TourTagCategory tagCategory) {

			super(tagCategory.getCategoryName(), AS_DROP_DOWN_MENU);
			setMenuCreator(this);

			fTagCategory = tagCategory;
		}

		public void dispose() {
			if (fCategoryMenu != null) {
				fCategoryMenu.dispose();
				fCategoryMenu = null;
			}
		}

		public Menu getMenu(final Control parent) {
			return null;
		}

		public Menu getMenu(final Menu parent) {

			dispose();
			fCategoryMenu = new Menu(parent);

			// Add listener to repopulate the menu each time
			fCategoryMenu.addMenuListener(new MenuAdapter() {
				@Override
				public void menuShown(final MenuEvent e) {

					final Menu menu = (Menu) e.widget;

					// dispose old items
					final MenuItem[] items = menu.getItems();
					for (final MenuItem item : items) {
						item.dispose();
					}

					final TagCollection tagCollection = TourDatabase.getTagEntries(fTagCategory.getCategoryId());

					// add actions
					createCategoryActions(tagCollection, fCategoryMenu);
					createTagActions(tagCollection, fCategoryMenu);
				}
			});

			return fCategoryMenu;
		}

	}

	private class ActionTourTag extends Action {

		private TourTag	fTourTag;

		public ActionTourTag(final TourTag tourTag) {

			super(tourTag.getTagName(), AS_CHECK_BOX);

			fTourTag = tourTag;
		}

		@Override
		public void run() {
			TagManager.setTagIntoTour(fTourTag, fTourProvider, fIsAddMode);
		}

	}

	public ActionSetTourTag(final ISelectedTours tourProvider, final boolean isAddMode) {

		super(UI.IS_NOT_INITIALIZED, AS_DROP_DOWN_MENU);

		fTourProvider = tourProvider;
		fIsAddMode = isAddMode;

		setText(isAddMode ? Messages.action_tag_add : Messages.action_tag_remove);
		setMenuCreator(this);
	}

	private void addActionToMenu(final Menu menu, final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	private void createCategoryActions(final TagCollection tagCollection, final Menu menu) {

		// add tag categories
		for (final TourTagCategory tagCategory : tagCollection.tourTagCategories) {
			addActionToMenu(menu, new ActionTagCategory(tagCategory));
		}
	}

	private void createTagActions(final TagCollection tagCollection, final Menu menu) {

		final ArrayList<TourTag> tourTags = tagCollection.tourTags;
		if (tourTags == null) {
			return;
		}

		// add tag items
		for (final TourTag menuTourTag : tourTags) {

			// check the tag when it's set in the tour
			final ActionTourTag actionTourTag = new ActionTourTag(menuTourTag);

			boolean isTagChecked = false;
			final boolean isOneTour = fSelectedTours != null && fSelectedTours.size() == 1;
			if (fSelectedTags != null && //
					(isOneTour || fIsAddMode == false)) {

				/*
				 * only when one tour is selected check the tag otherwise it's confusing, a
				 * three-state check could solve this problem but is not available
				 */

				final long tagId = menuTourTag.getTagId();

				for (final TourTag checkTourTag : fSelectedTags) {
					if (checkTourTag.getTagId() == tagId) {
						isTagChecked = true;
						break;
					}
				}
			}
			actionTourTag.setChecked(isTagChecked);

			// disable tags which are not tagged
			if (isOneTour) {
				if (fIsAddMode) {
					actionTourTag.setEnabled(!isTagChecked);
				} else {
					actionTourTag.setEnabled(isTagChecked);
				}
			} else if (fIsAddMode == false) {
				actionTourTag.setEnabled(isTagChecked);
			}

			addActionToMenu(menu, actionTourTag);
		}
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		dispose();
		fMenu = new Menu(parent);

		// Add listener to repopulate the menu each time
		fMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(final MenuEvent e) {

				final Menu menu = (Menu) e.widget;

				// dispose old items
				final MenuItem[] items = menu.getItems();
				for (final MenuItem item : items) {
					item.dispose();
				}

				// check if a tour is selected
				fSelectedTours = fTourProvider.getSelectedTours();
				if (fSelectedTours == null || fSelectedTours.size() == 0) {
					// a tour is not selected
					return;
				}

				// get all tags for all tours
				fSelectedTags = new HashSet<TourTag>();
				for (final TourData tourData : fSelectedTours) {
					final Set<TourTag> tags = tourData.getTourTags();
					if (tags != null) {
						fSelectedTags.addAll(tags);
					}
				}

				if (fIsAddMode) {

					// add tags, create actions for the root tags

					final TagCollection rootTagCollection = TourDatabase.getRootTags();

					createCategoryActions(rootTagCollection, fMenu);
					createTagActions(rootTagCollection, fMenu);

				} else {

					// remove tags, create actions for all tags of all selected tours

					final ArrayList<TourTag> sortedTags = new ArrayList<TourTag>(fSelectedTags);
					Collections.sort(sortedTags);

					createTagActions(new TagCollection(sortedTags), fMenu);
				}
			}
		});

		return fMenu;
	}

	@Override
	public void setEnabled(final boolean enabled) {

		// ensure tags are available 
		final HashMap<Long, TourTag> allTags = TourDatabase.getAllTourTags();

		super.setEnabled(enabled && allTags.size() > 0);
	}

}
