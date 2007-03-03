/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

package net.tourbook.application;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.CustomControlContribution;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;

public class PersonContributionItem extends CustomControlContribution {

	private static final String		ID			= "net.tourbook.clientselector";	//$NON-NLS-1$

	private static final String		ALL_PEOPLE	= Messages.App_People_item_all;

	static TourbookPlugin			plugin		= TourbookPlugin.getDefault();

	private Combo					fComboPeople;

	private IPropertyChangeListener	fPrefChangeListener;
	private ArrayList<TourPerson>	fPeople;

	public PersonContributionItem() {
		this(ID);
	}

	protected PersonContributionItem(String id) {
		super(id);
	}

	/**
	 * listen for changes in the person list
	 */
	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

					// fill people combobox with modified people list
					fillPeopleComboBox();

					TourPerson currentPerson = plugin.getActivePerson();

					// reselect the person which was selected before
					if (currentPerson == null) {
						fComboPeople.select(0);
					} else {
						// try to set and select the old person
						long previousPersonId = currentPerson.getPersonId();
						reselectPerson(previousPersonId);
					}
				}
			}

		};
		// register the listener
		plugin.getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	protected Control createControl(Composite parent) {

		Composite control = createPeopleComboBox(parent);

		addPrefListener();
		reselectLastPerson();

		return control;
	}

	private Composite createPeopleComboBox(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		container.setLayout(gl);

		// container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		fComboPeople = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboPeople.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, true));
		fComboPeople.setVisibleItemCount(20);
		fComboPeople.setToolTipText(Messages.App_People_tooltip);

		fComboPeople.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				plugin.getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
			}
		});

		fComboPeople.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int selectedIndex = fComboPeople.getSelectionIndex();
				if (selectedIndex == -1) {
					return;
				}

				if (selectedIndex == 0) {
					// all people are selected
					plugin.setActivePerson(null);
				} else {
					// a person is selected
					plugin.setActivePerson(fPeople.get(selectedIndex - 1));
				}

				fireEventNewPersonIsSelected();
			}
		});

		fillPeopleComboBox();

		return container;
	}

	private void fillPeopleComboBox() {

		fComboPeople.removeAll();

		fComboPeople.add(ALL_PEOPLE);

		fPeople = TourDatabase.getTourPeople();

		if (fPeople == null) {
			return;
		}

		for (TourPerson person : fPeople) {
			String lastName = person.getLastName();
			lastName = lastName.equals("") ? "" : " " + lastName; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			fComboPeople.add(person.getFirstName() + lastName);
		}
	}

	/**
	 * fire event that client has changed
	 */
	void fireEventNewPersonIsSelected() {
		plugin.getPreferenceStore().setValue(
				ITourbookPreferences.APP_NEW_DATA_FILTER,
				Math.random());
	}

	private void reselectPerson(long previousPersonId) {

		if (fPeople == null) {
			fComboPeople.select(0);
			return;
		}

		TourPerson currentPerson = null;
		int personIndex = 1;

		for (TourPerson person : fPeople) {
			if (previousPersonId == person.getPersonId()) {
				// previous person was found
				fComboPeople.select(personIndex);
				currentPerson = person;
				break;
			}
			personIndex++;
		}

		if (currentPerson == null) {
			// old person was not found in the new list
			fComboPeople.select(0);
		}

		plugin.setActivePerson(currentPerson);
	}

	/**
	 * select the person which was set in the dialog settings
	 */
	private void reselectLastPerson() {

		Long lastPersonId;
		try {

			lastPersonId = plugin.getDialogSettings().getLong(
					ITourbookPreferences.APP_LAST_SELECTED_PERSON_ID);

			// try to reselect the last person
			reselectPerson(lastPersonId);

		} catch (NumberFormatException e) {
			// no last person id, select all
			fComboPeople.select(0);
		}
	}

	/**
	 * save current person id in the dialog settings
	 * 
	 * @param memento
	 */
	private void saveSettings(IMemento memento) {

		int selectedIndex = fComboPeople.getSelectionIndex();

		long personId = -1;
		if (selectedIndex > 0) {
			personId = fPeople.get(selectedIndex - 1).getPersonId();
		}

		plugin.getDialogSettings().put(ITourbookPreferences.APP_LAST_SELECTED_PERSON_ID, personId);
	}

	public void saveState(IMemento memento) {
		saveSettings(memento);
	}
}
