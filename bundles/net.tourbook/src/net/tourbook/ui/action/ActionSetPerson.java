/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Set person for a tour
 */
public class ActionSetPerson extends Action implements IMenuCreator {

	private Menu			_menu;

	private ITourProvider	_tourProvider;

	private class ActionPerson extends Action {

		private TourPerson	__person;

		public ActionPerson(final TourPerson person, final boolean isChecked) {

			super(person.getName(), AS_CHECK_BOX);

			__person = person;

			setChecked(isChecked);
		}

		@Override
		public void run() {

			final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();
			if (selectedTours == null) {
				return;
			}

			for (final TourData tourData : selectedTours) {
				tourData.setTourPerson(__person);
			}

			TourManager.saveModifiedTours(selectedTours);
		}
	}

	public ActionSetPerson(final ITourProvider tourProvider) {

		super(Messages.App_Action_SetPerson, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		_tourProvider = tourProvider;
	}

	private void addActionToMenu(final Action action, final Menu menu) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	public void dispose() {
		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	private void fillMenu(final Menu menu) {

		// get tours which tour type should be changed
		final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();
		if (selectedTours == null) {
			return;
		}

		// get tour type which will be checked in the menu
		TourPerson checkedPerson = null;
		if (selectedTours.size() == 1) {
			checkedPerson = selectedTours.get(0).getTourPerson();
		}

		// add all tour types to the menu
		final ArrayList<TourPerson> people = PersonManager.getTourPeople();

		for (final TourPerson person : people) {

			boolean isChecked = false;
			if (checkedPerson != null && checkedPerson.getPersonId() == person.getPersonId()) {
				isChecked = true;
			}

			addActionToMenu(new ActionPerson(person, isChecked), menu);
		}
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		dispose();
		_menu = new Menu(parent);

		// Add listener to repopulate the menu each time
		_menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(final MenuEvent e) {

				// dispose old menu items
				for (final MenuItem menuItem : ((Menu) e.widget).getItems()) {
					menuItem.dispose();
				}

				fillMenu(_menu);
			}
		});

		return _menu;
	}

}
