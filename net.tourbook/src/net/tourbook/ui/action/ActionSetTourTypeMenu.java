/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ActionSetTourTypeMenu extends Action implements IMenuCreator {

	private Menu	fMenu;

	ITourProvider	fTourProvider;

	/**
	 * Adds all tour types to the menu manager
	 * 
	 * @param menuMgr
	 * @param tourProvider
	 * @param isSaveTour
	 *            when <code>true</code> the tour will be saved and a
	 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise {@link TourData} from
	 *            the tour provider is only modified
	 */
	public static void fillMenu(final IMenuManager menuMgr, final ITourProvider tourProvider, final boolean isSaveTour) {

		// get tours which tour type should be changed
		final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();
		if (selectedTours == null) {
			return;
		}

		// get tour type which will be checked in the menu
		TourType checkedTourType = null;
		if (selectedTours.size() == 1) {
			checkedTourType = selectedTours.get(0).getTourType();
		}

		// add all tour types to the menu
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		for (final TourType tourType : tourTypes) {

			boolean isChecked = false;

			if (checkedTourType != null && checkedTourType.getTypeId() == tourType.getTypeId()) {
				isChecked = true;
			}

			final ActionSetTourType actionTourType = new ActionSetTourType(tourType, tourProvider, isSaveTour);
			actionTourType.setChecked(isChecked);

			menuMgr.add(actionTourType);
		}
	}

	public ActionSetTourTypeMenu(final ITourProvider tourProvider) {

		super(Messages.App_Action_set_tour_type, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		fTourProvider = tourProvider;
	}

	private void addActionToMenu(final Action action, final Menu menu) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	private void fillMenu(final Menu menu) {

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
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		for (final TourType tourType : tourTypes) {

			boolean isChecked = false;

			if (checkedTourType != null && checkedTourType.getTypeId() == tourType.getTypeId()) {
				isChecked = true;
			}

			final ActionSetTourType actionTourType = new ActionSetTourType(tourType, fTourProvider, true);
			actionTourType.setChecked(isChecked);

			addActionToMenu(actionTourType, menu);
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

				// dispose old menu items	
				for (final MenuItem menuItem : ((Menu) e.widget).getItems()) {
					menuItem.dispose();
				}

				fillMenu(fMenu);
			}
		});

		return fMenu;
	}

}
