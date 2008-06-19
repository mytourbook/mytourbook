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

package net.tourbook.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.TagCollection;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorPart;

/**
 * Add or removes a tag from the selected tours
 */
public class ActionSetTourTag extends Action implements IMenuCreator {

	private Menu					fMenu;

	private final ISelectedTours	fTourProvider;
	private final boolean			fIsAddMode;

	/**
	 * contains the tags for the selected tour when one tour is selected in the viewer
	 */
	private Set<TourTag>			fSelectedTags;

	protected ArrayList<TourData>	fSelectedTours;

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
					for (int i = 0; i < items.length; i++) {
						items[i].dispose();
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

			final Runnable runnable = new Runnable() {

				@SuppressWarnings("unchecked")
				public void run() {

					// get tours which tag should be changed
					final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();
					if (selectedTours == null) {
						return;
					}

					// update tours which are opened in an editor
					final ArrayList<TourData> toursInEditor = updateEditors(selectedTours);

					// get all tours which are not opened in an editor
					final ArrayList<TourData> noneEditorTours = (ArrayList<TourData>) selectedTours.clone();
					noneEditorTours.removeAll(toursInEditor);

					// add tag in all tours (without tours which are opened in an editor)
					for (final TourData tourData : noneEditorTours) {

						// set+save the tour tag
						final Set<TourTag> tourTags = tourData.getTourTags();

						if (fIsAddMode) {
							// add tag to tour
							tourTags.add(fTourTag);
						} else {
							// remove tag from tour
							tourTags.remove(fTourTag);
						}

						// save tour tag
						TourDatabase.saveTour(tourData);
					}

					TourManager.getInstance().firePropertyChange(TourManager.TOUR_PROPERTIES_CHANGED, selectedTours);
				}

			};
			BusyIndicator.showWhile(Display.getCurrent(), runnable);
		}

		/**
		 * Update the tour type in tours which are opened in a tour editor
		 * 
		 * @param selectedTours
		 *            contains the tours where the tour type should be changed
		 * @return Returns the tours which are opened in a tour editor
		 */
		private ArrayList<TourData> updateEditors(final ArrayList<TourData> selectedTours) {

			final ArrayList<IEditorPart> editorParts = UI.getOpenedEditors();

			// list for tours which are updated in the editor
			final ArrayList<TourData> updatedTours = new ArrayList<TourData>();

			// check if a tour is in an editor
//			for (final IEditorPart editorPart : editorParts) {
//				if (editorPart instanceof TourEditor) {
//
//					final IEditorInput editorInput = editorPart.getEditorInput();
//
//					if (editorInput instanceof TourEditorInput) {
//
//						final TourEditor tourEditor = (TourEditor) editorPart;
//						final long editorTourId = ((TourEditorInput) editorInput).getTourId();
//
//						for (final TourData tourData : selectedTours) {
//							if (editorTourId == tourData.getTourId()) {
//
//								/*
//								 * a tour editor was found containing the current tour
//								 */
//
//								tourEditor.getTourChart().getTourData().setTourType(fTourTag);
//
//								tourEditor.setTourPropertyIsModified();
//
//								// keep updated tours
//								updatedTours.add(tourData);
//							}
//						}
//					}
//				}
//			}
//
//			// show info that at least one tour is opened in a tour editor
//			if (fTourProvider.isFromTourEditor() == false && updatedTours.size() > 0) {
//
//				/*
//				 * don't show the message when the tour is from a tour editor
//				 */
//
//				MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
//						Messages.App_Action_set_tour_type_dlg_title,
//						Messages.App_Action_set_tour_type_dlg_message);
//			}

			return updatedTours;
		}
	}

	public ActionSetTourTag(final ISelectedTours tourProvider, final boolean isAddMode) {

		super(UI.IS_NOT_INITIALIZED, AS_DROP_DOWN_MENU);

		fTourProvider = tourProvider;
		fIsAddMode = isAddMode;

		setText(isAddMode ? Messages.app_action_tag_add : Messages.app_action_tag_remove);
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

		// add tag items
		for (final TourTag menuTourTag : tagCollection.tourTags) {

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
			if (fIsAddMode == false) {
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
				for (int i = 0; i < items.length; i++) {
					items[i].dispose();
				}

				// check if tours are selected
				fSelectedTours = fTourProvider.getSelectedTours();
				if (fSelectedTours == null || fSelectedTours.size() == 0) {
					// a tour is not selected
					return;
				}

				fSelectedTags = new HashSet<TourTag>();
				for (final TourData tourData : fSelectedTours) {
					final Set<TourTag> tags = tourData.getTourTags();
					if (tags != null) {
						fSelectedTags.addAll(tags);
					}
				}

				final TagCollection rootTagCollection = TourDatabase.getRootTags();

				createCategoryActions(rootTagCollection, fMenu);
				createTagActions(rootTagCollection, fMenu);
			}
		});

		return fMenu;
	}

}
