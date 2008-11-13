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
package net.tourbook.ui.views.rawData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.data.TourBike;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.DeviceManager;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourEventId;
import net.tourbook.ui.EmptySelection;
import net.tourbook.ui.ResizeableListDialog;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ActionSaveTourInDatabase extends Action {

	private static final String		MEMENTO_SELECTED_PERSON	= "action-save-tour.selected-person";	//$NON-NLS-1$

	private RawDataView				fRawDataView;

	private TourPerson				fTourPerson;

	private List<TourbookDevice>	fDeviceList;

	private ArrayList<TourPerson>	fPeople;

	private class PeopleContentProvider implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return fPeople.toArray();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private class PeopleLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}

		public String getColumnText(final Object element, final int columnIndex) {

			final TourPerson person = (TourPerson) element;
			switch (columnIndex) {
			case 0:
				return person.getName() + " (" + getPersonDevice(person) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return null;
		}
	}

	public ActionSaveTourInDatabase(final RawDataView viewPart) {

		fRawDataView = viewPart;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__save_tour));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__save_tour_disabled));

		// setToolTipText("Save tour(s) in the database so it can be viewed in
		// other views");
		setEnabled(false);

		fDeviceList = DeviceManager.getDeviceList();
	}

	public IDialogSettings getDialogSettings() {

		final String DIALOG_SETTINGS_SECTION = "DialogSelectPerson"; //$NON-NLS-1$

		final IDialogSettings pluginSettings = TourbookPlugin.getDefault().getDialogSettings();
		IDialogSettings dialogSettings = pluginSettings.getSection(DIALOG_SETTINGS_SECTION);

		if (dialogSettings == null) {
			dialogSettings = pluginSettings.addNewSection(DIALOG_SETTINGS_SECTION);
		}
		return dialogSettings;
	}

	/**
	 * convert the person device id to the visible device name
	 * 
	 * @param person
	 * @return
	 */
	private String getPersonDevice(final TourPerson person) {

		final String deviceId = person.getDeviceReaderId();

		if (deviceId != null) {
			for (final TourbookDevice device : fDeviceList) {
				if (deviceId.equals(device.deviceId)) {
					return device.visibleName;
				}
			}
		}

		return Messages.import_data_label_unknown_device;
	}

	/**
	 * force the people list to be recreated the next time when it's used
	 */
	public void resetPeopleList() {
		fPeople = null;
	}

	/**
	 * Store the tour permanently in the tour database
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run() {

		final TourPerson person;
		final TourBike bike;

		// get the person, when not set
		if (fTourPerson == null) {
			person = selectPersonDialog();
			if (person == null) {
				return;
			}
		} else {
			person = fTourPerson;
		}

		bike = person.getTourBike();

		final Runnable runnable = new Runnable() {

			public void run() {

				boolean saveInDatabase = false;
				final ArrayList<TourData> savedTours = new ArrayList<TourData>();
				final ArrayList<Long> savedToursIds = new ArrayList<Long>();

				// get selected tours
				final IStructuredSelection selection = ((IStructuredSelection) fRawDataView.getViewer().getSelection());

				// loop: all selected tours
				for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {

					final Object selObject = iter.next();
					if (selObject instanceof TourData) {

						final TourData tourData = (TourData) selObject;
						if (tourData.isTourDeleted == false) {

							if (tourData.getTourPerson() == null) {

								tourData.setTourPerson(person);
								tourData.setBikerWeight(person.getWeight());

								saveInDatabase = true;
							}

							if (tourData.getTourBike() == null && tourData.getTourPerson().getTourBike() != null) {
								tourData.setTourBike(bike);

								saveInDatabase = true;
							}

							// save the person and or bike when it's not yet set
							if (saveInDatabase == true) {

								final TourData savedTour = TourDatabase.saveTour(tourData);

								if (savedTour != null) {
									savedTours.add(savedTour);
									savedToursIds.add(savedTour.getTourId());
								}
							}
						}
					}
				}

				// update viewer, fire selection event
				if (savedToursIds.size() > 0) {

					// update raw data map with the saved tour data 
					final HashMap<Long, TourData> rawDataMap = RawDataManager.getInstance().getTourDataMap();
					for (final TourData tourData : savedTours) {
						rawDataMap.put(tourData.getTourId(), tourData);
					}

					/*
					 * the selection provider can contain old tour data which conflicts with the
					 * tour data in the tour data editor
					 */
					fRawDataView.getSite().getSelectionProvider().setSelection(new EmptySelection());

					// update import viewer
					fRawDataView.reloadViewer();

					/*
					 * notify all views, it is not checked if the tour data editor is dirty because
					 * newly saved tours can not be modified in the tour data editor
					 */
					TourManager.fireEvent(TourEventId.UPDATE_UI, new SelectionTourIds(savedToursIds));
				}
			}
		};
		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}

	private TourPerson selectPersonDialog() {

		if (fPeople == null) {
			// read people list from the db
			fPeople = TourDatabase.getTourPeople();
		}

		final ResizeableListDialog dialog = new ResizeableListDialog(fRawDataView.getSite().getShell());

		dialog.setContentProvider(new PeopleContentProvider());
		dialog.setLabelProvider(new PeopleLabelProvider());

		dialog.setTitle(Messages.import_data_dlg_save_tour_title);
		dialog.setMessage(Messages.import_data_dlg_save_tour_msg);
		dialog.setDialogBoundsSettings(getDialogSettings(), Dialog.DIALOG_PERSISTLOCATION | Dialog.DIALOG_PERSISTSIZE);

		// select last person
		final IDialogSettings settings = getDialogSettings();
		try {
			final long personId = settings.getLong(MEMENTO_SELECTED_PERSON);
			for (final TourPerson person : fPeople) {
				if (person.getPersonId() == personId) {
					dialog.setInitialSelections(new TourPerson[] { person });
					break;
				}
			}
		} catch (final NumberFormatException e) {}

		dialog.setInput(this);
		dialog.create();

		// disable ok button when no people are available
		if (fPeople.size() == 0) {
			dialog.getOkButton().setEnabled(false);
		}

		if (dialog.open() != Window.OK) {
			return null;
		}

		final Object[] people = dialog.getResult();
		if (people != null && people.length > 0) {
			final TourPerson selectedPerson = (TourPerson) people[0];

			settings.put(MEMENTO_SELECTED_PERSON, selectedPerson.getPersonId());

			return selectedPerson;
		} else {
			return null;
		}
	}

	/**
	 * Sets the person for which the tour should be saved, when set to <code>null</code>, the person
	 * needs to be selected before the tour is saved.
	 * 
	 * @param person
	 */
	void setPerson(final TourPerson person) {
		fTourPerson = person;
	}

}
