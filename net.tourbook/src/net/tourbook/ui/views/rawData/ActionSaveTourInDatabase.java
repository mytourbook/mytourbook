/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourBike;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.DeviceManager;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.plugin.TourbookPlugin;
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

	private static final String			MEMENTO_SELECTED_PERSON	= "action-save-tour.selected-person";	//$NON-NLS-1$

	private RawDataView					fViewPart;

	private TourPerson					fTourPerson;

	private ArrayList<TourbookDevice>	fDeviceList;

	private ArrayList<TourPerson>		fPeople;

	private class PeopleContentProvider implements IStructuredContentProvider {

		public void dispose() {}
		public Object[] getElements(Object inputElement) {
			return fPeople.toArray();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}

	private class PeopleLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {

			TourPerson person = (TourPerson) element;
			switch (columnIndex) {
			case 0:
				return person.getName() + " (" + getPersonDevice(person) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return null;
		}
	}

	public ActionSaveTourInDatabase(RawDataView viewPart) {

		fViewPart = viewPart;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_save_tour));
		setDisabledImageDescriptor(TourbookPlugin
				.getImageDescriptor(Messages.Image_save_tour_disabled));

		// setToolTipText("Save tour(s) in the database so it can be viewed in
		// other views");
		setEnabled(false);

		fDeviceList = DeviceManager.getDeviceList();
	}

	public IDialogSettings getDialogSettings() {

		final String DIALOG_SETTINGS_SECTION = "DialogSelectPerson"; //$NON-NLS-1$

		IDialogSettings pluginSettings = TourbookPlugin.getDefault().getDialogSettings();
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
	private String getPersonDevice(TourPerson person) {

		String deviceId = person.getDeviceReaderId();

		if (deviceId != null) {
			for (TourbookDevice device : fDeviceList) {
				if (deviceId.equals(device.deviceId)) {
					return device.visibleName;
				}
			}
		}

		return Messages.RawData_Label_unknown_device;
	}

	/**
	 * Store the tour permanently in the tour database
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
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

		Runnable runnable = new Runnable() {

			@SuppressWarnings("unchecked")
			public void run() {

				boolean isModified = false;
				boolean saveInDatabase = false;

				// get selected tours
				final IStructuredSelection selection = ((IStructuredSelection) fViewPart
						.getTourViewer()
						.getSelection());

				// loop: all selected tours
				for (Iterator iter = selection.iterator(); iter.hasNext();) {

					Object selObject = iter.next();

					if (selObject instanceof TourData) {

						TourData tourData = (TourData) selObject;

						if (tourData.getTourPerson() == null) {

							tourData.setTourPerson(person);
							tourData.setBikerWeight(person.getWeight());

							saveInDatabase = true;
						}

						if (tourData.getTourBike() == null
								&& tourData.getTourPerson().getTourBike() != null) {
							tourData.setTourBike(bike);

							saveInDatabase = true;
						}

						// save the person and or bike when it's not yet set
						if (saveInDatabase == true) {
							if (TourDatabase.saveTour(tourData)) {
								isModified = true;
							}
						}
					}
				}

				// update viewer, fire selection event
				if (isModified) {

					// update the table viewer
					fViewPart.updateViewer();

					/*
					 * fire event that new tours has been saved in the database
					 */
					SelectionRawData selectionRawData = new SelectionRawData();

					// activate selection
					selectionRawData.setEmpty(false);

					fViewPart.fireSelectionEvent(selectionRawData);

					// deactivate selection
					selectionRawData.setEmpty(true);
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

		ResizeableListDialog dialog = new ResizeableListDialog(fViewPart.getSite().getShell());

		dialog.setContentProvider(new PeopleContentProvider());
		dialog.setLabelProvider(new PeopleLabelProvider());

		dialog.setTitle(Messages.RawData_Dlg_save_tour_title);
		dialog.setMessage(Messages.RawData_Dlg_save_tour_msg);
		dialog.setDialogBoundsSettings(getDialogSettings(), Dialog.DIALOG_PERSISTLOCATION
				| Dialog.DIALOG_PERSISTSIZE);

		// select last person
		IDialogSettings settings = getDialogSettings();
		try {
			long personId = settings.getLong(MEMENTO_SELECTED_PERSON);
			for (TourPerson person : fPeople) {
				if (person.getPersonId() == personId) {
					dialog.setInitialSelections(new TourPerson[] { person });
					break;
				}
			}
		} catch (NumberFormatException e) {}

		dialog.setInput(this);
		dialog.create();

		// disable ok button when no people are available
		if (fPeople.size() == 0) {
			dialog.getOkButton().setEnabled(false);
		}

		if (dialog.open() != Window.OK) {
			return null;
		}

		Object[] people = dialog.getResult();
		if (people != null && people.length > 0) {
			TourPerson selectedPerson = (TourPerson) people[0];

			settings.put(MEMENTO_SELECTED_PERSON, selectedPerson.getPersonId());

			return selectedPerson;
		} else {
			return null;
		}
	}

	/**
	 * Sets the person for which the tour should be saved, when set to
	 * <code>null</code>, the person needs to be selected before the tour is
	 * saved.
	 * 
	 * @param person
	 */
	void setPerson(TourPerson person) {
		fTourPerson = person;
	}

	/**
	 * force the people list to be recreated the next time when it's used
	 */
	public void resetPeopleList() {
		fPeople = null;
	}

}
