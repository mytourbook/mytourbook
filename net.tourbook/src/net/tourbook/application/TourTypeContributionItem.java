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

import net.tourbook.data.TourType;
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;

public class TourTypeContributionItem extends CustomControlContribution {

	private static final String		ID		= "net.tourbook.tourtype-selector";

	static TourbookPlugin			plugin	= TourbookPlugin.getDefault();

	private ArrayList<TourType>		fTourTypes;
	private IPropertyChangeListener	fPrefChangeListener;

	private Combo					fComboTourType;

	public TourTypeContributionItem() {
		this(ID);
	}

	protected TourTypeContributionItem(String id) {
		super(id);
	}

	/**
	 * listen for changes in the person list
	 */
	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {
					fillTourTypeComboBox();

					reselectTourType(plugin.getActiveTourType().getTypeId());
				}
			}

		};
		// register the listener
		plugin.getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}
	protected Control createControl(Composite parent) {

		if (PlatformUI.getWorkbench().isClosing()) {
			return new Label(parent,SWT.NONE);
		}
		
		Composite container = createTourTypeComboBox(parent);

		addPrefListener();
		reselectLastTourType();

		return container;
	}

	private Composite createTourTypeComboBox(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		final GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.marginTop = 3;
		container.setLayout(gl);

		fComboTourType = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboTourType.setVisibleItemCount(10);
		fComboTourType.setToolTipText("Show tours for the selected tour type");

		fComboTourType.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				plugin.getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
			}
		});

		fComboTourType.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				setActiveTourType();

				// fire change event
				plugin.getPreferenceStore().setValue(
						ITourbookPreferences.APP_NEW_DATA_FILTER,
						Math.random());

			}
		});

		// add tour type names to the ui list
		fillTourTypeComboBox();

		return container;
	}

	/**
	 * reads the tour types from the db, set the fTourTypes list and fill the
	 * combo box
	 */
	private void fillTourTypeComboBox() {

		fTourTypes = new ArrayList<TourType>();

		// add entry where the tour type will be ignored
		fTourTypes.add(new TourType("- All Types -", TourType.TOUR_TYPE_ID_ALL));

		// add tour type for tours where the tour type is not defined
		fTourTypes.add(new TourType("- Type Not Defined -", TourType.TOUR_TYPE_ID_NOT_DEFINED));

		/*
		 * get tour types from the db
		 */
		ArrayList<TourType> tourTypesFromDb = TourDatabase.getTourTypes();

		if (tourTypesFromDb == null) {
			return;
		}

		// sort tour types list
		// Collections.sort(tourTypesFromDb, new Comparator<TourType>() {
		// public int compare(TourType tt1, TourType tt2) {
		// return tt1.getName().compareTo(tt2.getName());
		// }
		// });

		for (TourType tourTypeFromDb : tourTypesFromDb) {
			fTourTypes.add(tourTypeFromDb);
		}

		// update combo box
		fComboTourType.removeAll();
		for (TourType tourType : fTourTypes) {
			fComboTourType.add(tourType.getName());
		}

		plugin.setTourTypes(fTourTypes);
	}

	void saveState(IMemento memento) {

		// save: selected tour type
		int selectionIndex = fComboTourType.getSelectionIndex();
		if (selectionIndex != -1) {
			plugin.getDialogSettings().put(
					ITourbookPreferences.APP_LAST_SELECTED_TOUR_TYPE_ID,
					fTourTypes.get(selectionIndex).getTypeId());
		}
	}

	/**
	 * set the selected tour type
	 */
	private void setActiveTourType() {

		int selectionIndex = fComboTourType.getSelectionIndex();
		if (selectionIndex == -1) {
			// nothing is selected, select first entry
			selectionIndex = 0;
			fComboTourType.select(selectionIndex);
		}

		plugin.setTourTypes(fTourTypes);
		plugin.setActiveTourType(fTourTypes.get(selectionIndex));
	}

	/**
	 * reselect the tour type in the combo box and set the active tour type in
	 * the plugin
	 * 
	 * @param lastTourTypeId
	 */
	private void reselectTourType(Long lastTourTypeId) {

		// if (fTourTypes == null) {
		// fComboTourType.select(0);
		// return;
		// }

		TourType activeTourType = null;
		long activeTourTypeId = TourType.TOUR_TYPE_ID_ALL;

		// find the tour type in the combobox
		int tourTypeIndex = 0;

		for (TourType tourType : fTourTypes) {
			if (tourType.getTypeId() == lastTourTypeId) {
				// reselect last tour type
				activeTourTypeId = lastTourTypeId;
				activeTourType = tourType;
				fComboTourType.select(tourTypeIndex);
				break;
			}
			tourTypeIndex++;
		}

		if (activeTourTypeId == TourType.TOUR_TYPE_ID_ALL) {
			// the last tour type was not found, select first entry
			fComboTourType.select(0);
			activeTourType = fTourTypes.get(0);
		}

		// if (activeTourType != null) {
		plugin.setActiveTourType(activeTourType);
		// }
	}

	private void reselectLastTourType() {

		Long lastTourTypeId;
		try {

			lastTourTypeId = plugin.getDialogSettings().getLong(
					ITourbookPreferences.APP_LAST_SELECTED_TOUR_TYPE_ID);

		} catch (NumberFormatException e) {
			// last tour type id was not found, select all
			lastTourTypeId = 0L;
			// fComboTourType.select(0);
		}

		// try to reselect the last person
		reselectTourType(lastTourTypeId);
	}
}
