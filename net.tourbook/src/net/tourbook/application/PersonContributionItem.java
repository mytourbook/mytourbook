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
package net.tourbook.application;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.CustomControlContribution;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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

	private static final String		ID			= "net.tourbook.clientselector";		//$NON-NLS-1$

	private static TourbookPlugin	_activator	= TourbookPlugin.getDefault();

	private final IDialogSettings	_state		= _activator.getDialogSettings();
	private final IPreferenceStore	_prefStore	= _activator.getPreferenceStore();

	private IPropertyChangeListener	_prefChangeListener;

	private ArrayList<TourPerson>	_allPeople;

	private Combo					_cboPeople;

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

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

					// fill people combobox with modified people list
					fillPeopleComboBox();

					final TourPerson currentPerson = TourbookPlugin.getActivePerson();

					// reselect the person which was selected before
					if (currentPerson == null) {
						_cboPeople.select(0);
					} else {
						// try to set and select the old person
						final long previousPersonId = currentPerson.getPersonId();
						reselectPerson(previousPersonId);
					}
				}
			}
		};
		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	@Override
	protected Control createControl(final Composite parent) {

		Composite content;

		if (UI.IS_OSX) {

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

		_cboPeople = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);

		_cboPeople.setVisibleItemCount(20);
		_cboPeople.setToolTipText(Messages.App_People_tooltip);

		_cboPeople.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				if (_prefChangeListener != null) {
					_prefStore.removePropertyChangeListener(_prefChangeListener);
				}
			}
		});

		_cboPeople.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final int selectedIndex = _cboPeople.getSelectionIndex();
				if (selectedIndex == -1) {
					return;
				}

				if (selectedIndex == 0) {
					// all people are selected
					TourbookPlugin.setActivePerson(null);
				} else {
					// a person is selected
					TourbookPlugin.setActivePerson(_allPeople.get(selectedIndex - 1));
				}

				fireEventNewPersonIsSelected();
			}
		});

		fillPeopleComboBox();

		return _cboPeople;
	}

	private void fillPeopleComboBox() {

		_cboPeople.removeAll();

		/*
		 * removed the dash in the "All People" string because the whole item was not displayed on
		 * mac osx
		 */
		_cboPeople.add(Messages.App_People_item_all);

		_allPeople = PersonManager.getTourPeople();

		if (_allPeople == null) {
			return;
		}

		for (final TourPerson person : _allPeople) {
			String lastName = person.getLastName();
			lastName = lastName.equals(UI.EMPTY_STRING) ? UI.EMPTY_STRING : UI.SPACE + lastName;
			_cboPeople.add(person.getFirstName() + lastName);
		}
	}

	/**
	 * fire event that client has changed
	 */
	void fireEventNewPersonIsSelected() {
		_prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
	}

	/**
	 * select the person which was set in the dialog settings
	 */
	private void reselectLastPerson() {

		try {

			final long lastPersonId = _state.getLong(ITourbookPreferences.APP_LAST_SELECTED_PERSON_ID);

			// try to reselect the last person
			reselectPerson(lastPersonId);

		} catch (final NumberFormatException e) {
			// no last person id, select all
			_cboPeople.select(0);
		}
	}

	private void reselectPerson(final long previousPersonId) {

		if (_allPeople == null) {
			_cboPeople.select(0);
			return;
		}

		TourPerson currentPerson = null;
		int personIndex = 1;

		for (final TourPerson person : _allPeople) {
			if (previousPersonId == person.getPersonId()) {
				// previous person was found
				_cboPeople.select(personIndex);
				currentPerson = person;
				break;
			}
			personIndex++;
		}

		if (currentPerson == null) {
			// old person was not found in the new list
			_cboPeople.select(0);
		}

		TourbookPlugin.setActivePerson(currentPerson);
	}

	/**
	 * save current person id in the dialog settings
	 * 
	 * @param memento
	 */
	public void saveState(final IMemento memento) {

		if (_cboPeople == null || _cboPeople.isDisposed()) {
			StatusUtil.log("cannot save selected person, _cboPeople.isDisposed()");//$NON-NLS-1$
			return;
		}

		final int selectedIndex = _cboPeople.getSelectionIndex();

		long personId = -1;
		if (selectedIndex > 0) {
			personId = _allPeople.get(selectedIndex - 1).getPersonId();
		}

		_state.put(ITourbookPreferences.APP_LAST_SELECTED_PERSON_ID, personId);
	}

}
