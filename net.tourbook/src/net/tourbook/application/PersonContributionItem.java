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
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;

public class PersonContributionItem extends CustomControlContribution {

	private static final String		ID		= "net.tourbook.clientselector";		//$NON-NLS-1$

	static TourbookPlugin			plugin	= TourbookPlugin.getDefault();

	private static final boolean	osx		= "carbon".equals(SWT.getPlatform());	//$NON-NLS-1$

	private Combo					fComboPeople;

	private IPropertyChangeListener	fPrefChangeListener;
	private ArrayList<TourPerson>	fPeople;

	public PersonContributionItem() {
		this(ID);
	}

	protected PersonContributionItem(final String id) {
		super(id);
	}

	/**
	 * listen for changes in the person list
	 */
	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

					// fill people combobox with modified people list
					fillPeopleComboBox();

					final TourPerson currentPerson = plugin.getActivePerson();

					// reselect the person which was selected before
					if (currentPerson == null) {
						fComboPeople.select(0);
					} else {
						// try to set and select the old person
						final long previousPersonId = currentPerson.getPersonId();
						reselectPerson(previousPersonId);
					}
				}
			}

		};
		// register the listener
		plugin.getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

//	@Override
//	protected Control createControl(Composite parent) {
//
//		Composite control = createPeopleComboBox(parent);
//
//		addPrefListener();
//		reselectLastPerson();
//
//		return control;
//	}

	@Override
	protected Control createControl(final Composite parent) {

		Composite content;

		if (osx) {

			content = createPeopleComboBox(parent);

		} else {

			/*
			 * on win32 a few pixel above and below the combobox are drawn, wrapping it into a
			 * composite removes the pixels
			 */
			content = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(content);

			final Composite control = createPeopleComboBox(content);
			control.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, true));
		}

		addPrefListener();
		reselectLastPerson();

		return content;
	}

	private Composite createPeopleComboBox(final Composite parent) {

		fComboPeople = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);

		fComboPeople.setVisibleItemCount(20);
		fComboPeople.setToolTipText(Messages.App_People_tooltip);

		fComboPeople.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				plugin.getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
			}
		});

		fComboPeople.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final int selectedIndex = fComboPeople.getSelectionIndex();
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

		return fComboPeople;
	}

	private void fillPeopleComboBox() {

		fComboPeople.removeAll();

		/*
		 * removed the dash in the "All People" string because the whole item was not displayed on
		 * mac osx
		 */
		fComboPeople.add(Messages.App_People_item_all);

		fPeople = TourDatabase.getTourPeople();

		if (fPeople == null) {
			return;
		}

		for (final TourPerson person : fPeople) {
			String lastName = person.getLastName();
			lastName = lastName.equals("") ? "" : " " + lastName; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			fComboPeople.add(person.getFirstName() + lastName);
		}
	}

	/**
	 * fire event that client has changed
	 */
	void fireEventNewPersonIsSelected() {
		plugin.getPreferenceStore().setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
	}

	private void reselectPerson(final long previousPersonId) {

		if (fPeople == null) {
			fComboPeople.select(0);
			return;
		}

		TourPerson currentPerson = null;
		int personIndex = 1;

		for (final TourPerson person : fPeople) {
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

			lastPersonId = plugin.getDialogSettings().getLong(ITourbookPreferences.APP_LAST_SELECTED_PERSON_ID);

			// try to reselect the last person
			reselectPerson(lastPersonId);

		} catch (final NumberFormatException e) {
			// no last person id, select all
			fComboPeople.select(0);
		}
	}

	/**
	 * save current person id in the dialog settings
	 * 
	 * @param memento
	 */
	private void saveSettings(final IMemento memento) {

		final int selectedIndex = fComboPeople.getSelectionIndex();

		long personId = -1;
		if (selectedIndex > 0) {
			personId = fPeople.get(selectedIndex - 1).getPersonId();
		}

		plugin.getDialogSettings().put(ITourbookPreferences.APP_LAST_SELECTED_PERSON_ID, personId);
	}

	public void saveState(final IMemento memento) {
		saveSettings(memento);
	}
}
