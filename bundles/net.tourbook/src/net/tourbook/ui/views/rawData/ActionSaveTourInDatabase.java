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
package net.tourbook.ui.views.rawData;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.importdata.DeviceManager;
import net.tourbook.importdata.ExternalDevice;
import net.tourbook.ui.ResizeableListDialog;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;

public class ActionSaveTourInDatabase extends Action {

	private static final String		STATE_SELECTED_PERSON	= "selectedPerson";										//$NON-NLS-1$

	private final IDialogSettings	_state					= TourbookPlugin.getDefault()//
																	.getDialogSettingsSection("DialogSelectPerson");	//$NON-NLS-1$

	private RawDataView				_rawDataView;

	private ArrayList<TourPerson>	_people;
	private TourPerson				_tourPerson;

	private List<ExternalDevice>	_deviceList;

	private class PeopleContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _people.toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private class PeopleLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(final Object element, final int columnIndex) {

			final TourPerson person = (TourPerson) element;
			switch (columnIndex) {
			case 0:
				return person.getName() + getPersonDevice(person);
			}
			return null;
		}
	}

	public ActionSaveTourInDatabase(final RawDataView viewPart, final boolean isWithPerson) {

		_rawDataView = viewPart;

		setImageDescriptor(isWithPerson //
				? TourbookPlugin.getImageDescriptor(Messages.Image__save_tour)
				: TourbookPlugin.getImageDescriptor(Messages.Image__database_other_person));

		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__save_tour_disabled));

		// setToolTipText("Save tour(s) in the database so it can be viewed in
		// other views");
		setEnabled(false);
	}

	private List<ExternalDevice> getDeviceList() {

		if (_deviceList == null) {

			// lazy initialization
			_deviceList = DeviceManager.getExternalDeviceList();
		}

		return _deviceList;
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
			for (final ExternalDevice device : getDeviceList()) {
				if (deviceId.equals(device.deviceId)) {
					return " (" + device.visibleName + ")";//$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		return UI.EMPTY_STRING;
	}

	/**
	 * force the people list to be recreated the next time when it's used
	 */
	public void resetPeopleList() {
		_people = null;
	}

	/**
	 * Store the tour permanently in the tour database
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run() {

		final TourPerson person;

		// get the person, when not set
		if (_tourPerson == null) {
			person = selectPersonInDialog();
			if (person == null) {
				return;
			}
		} else {
			person = _tourPerson;
		}

		_rawDataView.actionSaveTour(person);
	}

	private TourPerson selectPersonInDialog() {

		if (_people == null) {
			_people = PersonManager.getTourPeople();
		}

		final ResizeableListDialog dialog = new ResizeableListDialog(_rawDataView.getSite().getShell());

		dialog.setContentProvider(new PeopleContentProvider());
		dialog.setLabelProvider(new PeopleLabelProvider());

		dialog.setTitle(Messages.import_data_dlg_save_tour_title);
		dialog.setMessage(Messages.import_data_dlg_save_tour_msg);
		dialog.setDialogBoundsSettings(_state, Dialog.DIALOG_PERSISTLOCATION | Dialog.DIALOG_PERSISTSIZE);

		// select last person
		try {
			final long personId = _state.getLong(STATE_SELECTED_PERSON);
			for (final TourPerson person : _people) {
				if (person.getPersonId() == personId) {
					dialog.setInitialSelections(new TourPerson[] { person });
					break;
				}
			}
		} catch (final NumberFormatException e) {}

		dialog.setInput(this);
		dialog.create();

		// disable ok button when no people are available
		if (_people.size() == 0) {
			dialog.getOkButton().setEnabled(false);
		}

		if (dialog.open() != Window.OK) {
			return null;
		}

		final Object[] people = dialog.getResult();
		if (people != null && people.length > 0) {
			final TourPerson selectedPerson = (TourPerson) people[0];

			_state.put(STATE_SELECTED_PERSON, selectedPerson.getPersonId());

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
		_tourPerson = person;
	}

}
