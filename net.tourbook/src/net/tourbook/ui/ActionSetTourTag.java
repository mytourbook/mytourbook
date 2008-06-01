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

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
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

	private void addActionToMenu(final Action action) {

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

				// get tours which tags should be changed
				final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();
				if (selectedTours == null || selectedTours.size() == 0) {
					return;
				}

				for (final TourTag tourTag : TourDatabase.getTourTags()) {

					final ActionTourTag actionTourTag = new ActionTourTag(tourTag);
//					actionTourType.setChecked(isChecked);

					addActionToMenu(actionTourTag);
				}
			}
		});

		return fMenu;
	}

}
