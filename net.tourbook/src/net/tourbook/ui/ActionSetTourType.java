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
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ActionSetTourType extends Action implements IMenuCreator {

	private Menu			fMenu;

	private ISelectedTours	fTourProvider;

	private class ActionTourType extends Action {

		private TourType	fTourType;

		public ActionTourType(final TourType tourType) {

			super(tourType.getName(), AS_CHECK_BOX);

			final Image tourTypeImage = UI.getInstance().getTourTypeImage(tourType.getTypeId());
			setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));

			fTourType = tourType;
		}

		@Override
		public void run() {

			final Runnable runnable = new Runnable() {

				public void run() {

					// get tours which tour type should be changed
					final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();

					if (selectedTours == null || selectedTours.size() == 0) {
						return;
					}

					if (TourManager.saveTourEditors(selectedTours)) {

						// add the tag in all tours (without tours which are opened in an editor)
						for (final TourData tourData : selectedTours) {

							// set tour type
							tourData.setTourType(fTourType);

							// save tour with modified tags
							TourDatabase.saveTour(tourData);
						}

						TourManager.firePropertyChange(TourManager.TOUR_PROPERTIES_CHANGED, selectedTours);
					}
				}
			};

			BusyIndicator.showWhile(Display.getCurrent(), runnable);
		}

	}

	public ActionSetTourType(final ISelectedTours tourProvider) {

		super(Messages.App_Action_set_tour_type, AS_DROP_DOWN_MENU);
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

				// get tours which tour type should be changed
				final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();

				if (selectedTours == null) {
					return;
				}

				// get tour type which will be checked in the menu
				TourType checkedTourType = null;
				if (selectedTours.size() == 1) {
					checkedTourType = selectedTours.get(0).getTourType();
				}

				// add all tour types to the menu
				final ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();

				for (final TourType tourType : tourTypes) {

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
