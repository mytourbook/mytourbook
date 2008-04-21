/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourEditorInput;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

public class ActionSetTourType extends Action implements IMenuCreator {

	private Menu			fMenu;

	private ISelectedTours	fTourProvider;

	private class ActionTourType extends Action {

		private TourType	fTourType;

		public ActionTourType(TourType tourType) {

			super(tourType.getName(), AS_CHECK_BOX);

			Image tourTypeImage = UI.getInstance().getTourTypeImage(tourType.getTypeId());
			setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));

			fTourType = tourType;
		}

		@Override
		public void run() {

			Runnable runnable = new Runnable() {

				public void run() {

					// get tours which tour type should be changed
					ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();

					if (selectedTours == null) {
						return;
					}

					// update tours which are opened in an editor
					ArrayList<TourData> toursInEditor = updateEditors(selectedTours);

					// get all tours which are not opened in an editor
					ArrayList<TourData> saveTours = (ArrayList<TourData>) selectedTours.clone();
					saveTours.removeAll(toursInEditor);

					// update all tours (without tours from an editor) with the new tour type
					for (TourData tourData : saveTours) {

						// set+save the tour type
						tourData.setTourType(fTourType);
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
		 *        contains the tours where the tour type should be changed
		 * @return Returns the tours which are opened in a tour editor
		 */
		private ArrayList<TourData> updateEditors(ArrayList<TourData> selectedTours) {

			ArrayList<IEditorPart> editorParts = UI.getOpenedEditors();

			// list for tours which are updated in the editor
			ArrayList<TourData> updatedTours = new ArrayList<TourData>();

			// check if a tour is in an editor
			for (IEditorPart editorPart : editorParts) {
				if (editorPart instanceof TourEditor) {

					IEditorInput editorInput = editorPart.getEditorInput();

					if (editorInput instanceof TourEditorInput) {

						TourEditor tourEditor = (TourEditor) editorPart;
						long editorTourId = ((TourEditorInput) editorInput).getTourId();

						for (TourData tourData : selectedTours) {
							if (editorTourId == tourData.getTourId()) {

								/*
								 * a tour editor was found containing the current tour
								 */

								tourEditor.getTourChart().getTourData().setTourType(fTourType);

								tourEditor.setTourPropertyIsModified();

								// keep updated tours
								updatedTours.add(tourData);
							}
						}
					}
				}
			}

			// show info that at least one tour is opened in a tour editor
			if (fTourProvider.isFromTourEditor() == false && updatedTours.size() > 0) {

				/*
				 * don't show the message when the tour is from a tour editor
				 */

				MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
						Messages.App_Action_set_tour_type_dlg_title,
						Messages.App_Action_set_tour_type_dlg_message);
			}

			return updatedTours;
		}
	}

	public ActionSetTourType(ISelectedTours tourProvider) {

		super(Messages.App_Action_set_tour_type, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		fTourProvider = tourProvider;
	}

	private void addActionToMenu(Action action) {

		ActionContributionItem item = new ActionContributionItem(action);
		item.fill(fMenu, -1);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(Control parent) {
		return null;
	}

	public Menu getMenu(Menu parent) {

		dispose();
		fMenu = new Menu(parent);

		// Add listener to repopulate the menu each time
		fMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {

				Menu menu = (Menu) e.widget;

				// dispose old items
				MenuItem[] items = menu.getItems();
				for (int i = 0; i < items.length; i++) {
					items[i].dispose();
				}

				// get tours which tour type should be changed
				ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();

				if (selectedTours == null) {
					return;
				}

				// get tour type which will be checked in the menu
				TourType checkedTourType = null;
				if (selectedTours.size() == 1) {
					checkedTourType = selectedTours.get(0).getTourType();
				}

				// add all tour types to the menu
				ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();

				for (TourType tourType : tourTypes) {

					boolean isChecked = false;

					if (checkedTourType != null && checkedTourType.getTypeId() == tourType.getTypeId()) {
						isChecked = true;
					}

					final ActionTourType actionTourType = new ActionTourType(tourType);
					actionTourType.setChecked(isChecked);

					addActionToMenu(actionTourType);
				}
			}
		});

		return fMenu;
	}

}
