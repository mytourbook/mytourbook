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

public class ActionSetTourTag extends Action implements IMenuCreator {

	private Menu			fMenu;

	private ISelectedTours	fTourProvider;

	private int				fSelectedTourCounter;

	/**
	 * When one tour is selected ({@link #fSelectedTourCounter} == 1) in the viewer, this set
	 * contains the tags for the selected tour
	 */
	private Set<TourTag>	fTourTagIds;

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

					final TagCollection categoryTagCollection = TourDatabase.getTagEntries(fTagCategory.getCategoryId());

//					// add category actions
//					for (final TourTagCategory tagCategory : categoryTagCollection.tourTagCategories) {
//						addActionToMenu(fCategoryMenu, new ActionTagCategory(tagCategory));
//					}

					// add tag actions
					addCategoryActions(categoryTagCollection, fCategoryMenu);
					addTagActions(categoryTagCollection, fCategoryMenu);
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

					// get tours which tour type should be changed
					final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();

					if (selectedTours == null) {
						return;
					}

					// update tours which are opened in an editor
					final ArrayList<TourData> toursInEditor = updateEditors(selectedTours);

					// get all tours which are not opened in an editor
					final ArrayList<TourData> saveTours = (ArrayList<TourData>) selectedTours.clone();
					saveTours.removeAll(toursInEditor);

					// add tour tag in all tours (without tours from an editor)
					for (final TourData tourData : saveTours) {

						// set+save the tour tag
						tourData.getTourTags().add(fTourTag);
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

	public ActionSetTourTag(final ISelectedTours tourProvider) {

		super(Messages.app_action_set_tour_tag, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		fTourProvider = tourProvider;
	}

	private void addActionToMenu(final Menu menu, final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	private void addCategoryActions(final TagCollection tagCollection, final Menu menu) {

		// add tag categories
		for (final TourTagCategory tagCategory : tagCollection.tourTagCategories) {
			addActionToMenu(menu, new ActionTagCategory(tagCategory));
		}
	}

	private void addTagActions(final TagCollection tagCollection, final Menu menu) {

		// add tag items
		for (final TourTag menuTourTag : tagCollection.tourTags) {

			// check the tag when it's set in the tour
			final ActionTourTag actionTourTag = new ActionTourTag(menuTourTag);
			boolean isChecked = false;
			if (fSelectedTourCounter == 1) {

				final long tagId = menuTourTag.getTagId();

				for (final TourTag checkTourTag : fTourTagIds) {
					if (checkTourTag.getTagId() == tagId) {
						isChecked = true;
						break;
					}
				}
			}
			actionTourTag.setChecked(isChecked);

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
				final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();
				if (selectedTours == null || selectedTours.size() == 0) {
					// a tour is not selected
					return;
				}
				fSelectedTourCounter = selectedTours.size();

				if (fSelectedTourCounter == 1) {
					fTourTagIds = selectedTours.get(0).getTourTags();
				}

				final TagCollection rootTagCollection = TourDatabase.getRootTags();

				addCategoryActions(rootTagCollection, fMenu);
				addTagActions(rootTagCollection, fMenu);
			}
		});

		return fMenu;
	}

}
