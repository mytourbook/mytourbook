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
package net.tourbook.preferences;

import java.util.ArrayList;
import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.data.TourBike;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.DeviceManager;
import net.tourbook.device.TourbookDevice;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.InputFieldFloat;
import net.tourbook.ui.UI;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePeople extends PreferencePage implements IWorkbenchPreferencePage {

	private static final int			COLUMN_IS_MODIFIED		= 0;
	private static final int			COLUMN_FIRSTNAME		= 1;
	private static final int			COLUMN_LASTNAME			= 2;
	private static final int			COLUMN_DEVICE			= 3;
	private static final int			COLUMN_HEIGHT			= 4;
	private static final int			COLUMN_WEIGHT			= 5;

	private TableViewer					fPeopleViewer;
	private Button						fButtonAdd;
	private Button						fButtonDelete;

	private Composite					fPersonDetailContainer;
	private Text						fTextFirstName;
	private Text						fTextLastName;
	private Text						fTextHeight;
	private Text						fTextWeight;
	private Combo						fComboDevice;
	private Combo						fComboBike;
	private DirectoryFieldEditor		fRawDataPathEditor;

	private ModifyListener				fTextFirstNameModifyListener;
	private ModifyListener				fTextLastNameModifyListener;
	private ModifyListener				fTextHeightModifyListener;
	private ModifyListener				fTextWeightModifyListener;
	private ModifyListener				fComboDeviceModifyListener;
	private ModifyListener				fComboBikeModifyListener;

	private ArrayList<TourPerson>		fPeople;

	private TourPerson					fCurrentPerson;
	private boolean						fIsPersonModified;
	private TourBike[]					fBikes;
	private ArrayList<TourbookDevice>	fDeviceList;
	private IPropertyChangeListener		fPrefChangeListener;
	private boolean						fIsPersonListModified	= false;
	protected boolean					fIsNewPerson			= false;

	private class ClientsContentProvider implements IStructuredContentProvider {

		public ClientsContentProvider() {}
		public void dispose() {}
		public Object[] getElements(Object parent) {
			if (fPeople == null) {
				fPeople = TourDatabase.getTourPeople();
			}
			if (fPeople == null) {
				return new Object[0];
			} else {
				return fPeople.toArray(new TourPerson[fPeople.size()]);
			}
		}

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}
	}

	private class ClientsLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object obj, int index) {

			TourPerson tourPerson = ((TourPerson) obj);

			switch (index) {
			case COLUMN_IS_MODIFIED:
				return fIsPersonModified ? "*" : ""; //$NON-NLS-1$ //$NON-NLS-2$

			case COLUMN_FIRSTNAME:
				return tourPerson.getFirstName();

			case COLUMN_LASTNAME:
				return tourPerson.getLastName();

			case COLUMN_DEVICE:

				String deviceId = tourPerson.getDeviceReaderId();

				if (deviceId != null) {
					for (TourbookDevice device : fDeviceList) {
						if (device != null && deviceId.equals(device.deviceId)) {
							return device.visibleName;
						}
					}
				}
				break;

			case COLUMN_HEIGHT:
				return Float.toString(tourPerson.getHeight());

			case COLUMN_WEIGHT:
				return Float.toString(tourPerson.getWeight());
			}
			return ""; //$NON-NLS-1$
		}
	}

	private void addModifyListener() {

		fTextFirstName.addModifyListener(fTextFirstNameModifyListener);
		fTextLastName.addModifyListener(fTextLastNameModifyListener);
		fTextHeight.addModifyListener(fTextHeightModifyListener);
		fTextWeight.addModifyListener(fTextWeightModifyListener);

		fComboDevice.addModifyListener(fComboDeviceModifyListener);
		fComboBike.addModifyListener(fComboBikeModifyListener);

		fRawDataPathEditor
				.setPropertyChangeListener(new org.eclipse.jface.util.IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (fCurrentPerson != null) {

							fIsPersonModified = true;
							fPeopleViewer.update(fCurrentPerson, null);

							validatePerson();
						}
					}
				});

	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_BIKE_LIST_IS_MODIFIED)) {

					// create new bike list
					fComboBike.removeAll();
					createBikeList();

					// update person details
					showSelectedPersonDetails();
				}
			}

		};
		// register the listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(
				fPrefChangeListener);
	}

	private void createBikeList() {

		// create bike list
		fComboBike.add(DeviceManager.DEVICE_IS_NOT_SELECTED);

		ArrayList<TourBike> bikes = TourDatabase.getTourBikes();

		if (bikes == null) {
			fBikes = new TourBike[0];
		} else {
			fBikes = bikes.toArray(new TourBike[bikes.size()]);
			for (TourBike bike : fBikes) {
				fComboBike.add(bike.getName());
			}
		}
	}

	protected Control createContents(Composite parent) {

		Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_People_Title);

		// container
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		container.setLayout(gl);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createPeopleViewer(container);
		createPeopleViewerButtons(container);
		createPersonDetails(container);

		// enableButtons();
		addPrefListener();

		fPeopleViewer.setInput(this);

		// select first person
		fPeopleViewer.getTable().select(0);
		showSelectedPersonDetails();
		enableButtons(true);

		return container;
	}

	private void createFieldBike() {
		GridData gd;
		Label lbl;
		/*
		 * field: bike
		 */
		lbl = new Label(fPersonDetailContainer, SWT.NONE);
		lbl.setText(Messages.Pref_People_Label_bike);
		fComboBike = new Combo(fPersonDetailContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
		fComboBike.setVisibleItemCount(10);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		fComboBike.setLayoutData(gd);
		fComboBikeModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fCurrentPerson != null) {
					int selectedIndex = fComboBike.getSelectionIndex();
					if (selectedIndex != -1) {
						TourBike personTourBike = fCurrentPerson.getTourBike();
						if (selectedIndex == 0) {
							if (personTourBike == null) {
								// person had no bike this was not changed
								return;
							} else {
								// person had before a bike which is now
								// removed
								fCurrentPerson.setTourBike(null);
								fIsPersonModified = true;
							}
							return;
						}

						// adjust to correct index in the bike array
						selectedIndex--;

						TourBike selectedBike = fBikes[selectedIndex];

						if (personTourBike == null
								|| (personTourBike.getBikeId() != selectedBike.getBikeId())) {

							fCurrentPerson.setTourBike(selectedBike);
							fIsPersonModified = true;
						}

						if (fIsPersonModified) {
							fPeopleViewer.update(fCurrentPerson, null);
						}
					}
				}
			}
		};

		createBikeList();
		// filler
		lbl = new Label(fPersonDetailContainer, SWT.NONE);
	}

	/**
	 * field: device
	 */
	private void createFieldDevice() {

		Label lbl = new Label(fPersonDetailContainer, SWT.NONE);
		lbl.setText(Messages.Pref_People_Label_device);

		fComboDevice = new Combo(fPersonDetailContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
		fComboDevice.setVisibleItemCount(10);
		GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		fComboDevice.setLayoutData(gd);
		fComboDeviceModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fCurrentPerson != null) {
					int selectedIndex = fComboDevice.getSelectionIndex();
					if (selectedIndex != -1) {
						TourbookDevice device = fDeviceList.get(selectedIndex);
						if (device == null && fCurrentPerson.getDeviceReaderId() == null) {
							return;
						}
						if (device == null
								|| (device.deviceId != null && !device.deviceId
										.equals(fCurrentPerson.getDeviceReaderId()))) {

							fCurrentPerson.setDeviceReaderId(device == null
									? null
									: device.deviceId);
							fIsPersonModified = true;

							fPeopleViewer.update(fCurrentPerson, null);
						}
					}
				}
				validatePerson();
			}
		};

		// filler
		lbl = new Label(fPersonDetailContainer, SWT.NONE);

		// create device list
		fDeviceList = new ArrayList<TourbookDevice>();
		fDeviceList.add(null);
		fDeviceList.addAll(DeviceManager.getDeviceList());

		for (TourbookDevice device : fDeviceList) {
			if (device == null) {
				fComboDevice.add(DeviceManager.DEVICE_IS_NOT_SELECTED);
			} else {
				fComboDevice.add(device.visibleName);
			}
		}
	}

	/**
	 * field: first name
	 */
	private void createFieldFirstName() {

		Label lbl = new Label(fPersonDetailContainer, SWT.NONE);
		lbl.setText(Messages.Pref_People_Label_first_name);

		fTextFirstName = new Text(fPersonDetailContainer, SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		fTextFirstName.setLayoutData(gd);
		fTextFirstNameModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fCurrentPerson != null) {
					String firstName = ((Text) (e.widget)).getText();
					if (!firstName.equals(fCurrentPerson.getFirstName())) {
						fIsPersonModified = true;

						fCurrentPerson.setFirstName(firstName);
						fPeopleViewer.update(fCurrentPerson, null);
					}
				}
				validatePerson();
			}
		};

		// filler
		lbl = new Label(fPersonDetailContainer, SWT.NONE);
	}

	/**
	 * field: height
	 */
	private void createFieldHeight(int floatInputWidth) {
		InputFieldFloat floatInput = new InputFieldFloat(
				fPersonDetailContainer,
				Messages.Pref_People_Label_height,
				floatInputWidth);

		fTextHeight = floatInput.getTextField();
		fTextHeightModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fCurrentPerson != null) {
					Text control = (Text) e.widget;
					try {
						float value = Float.parseFloat(((Text) (e.widget)).getText());
						if (value != fCurrentPerson.getHeight()) {
							fCurrentPerson.setHeight(value);
							fPeopleViewer.update(fCurrentPerson, null);
						}
						UI.setDefaultColor(control);
					} catch (NumberFormatException e1) {
						UI.setErrorColor(control);
					}
					fIsPersonModified = true;
					validatePerson();
				}
			}
		};

		// filler
		Label lbl = new Label(fPersonDetailContainer, SWT.NONE);
	}

	/**
	 * field: last name
	 */
	private void createFieldLastName() {

		Label lbl = new Label(fPersonDetailContainer, SWT.NONE);
		lbl.setText(Messages.Pref_People_Label_last_name);

		fTextLastName = new Text(fPersonDetailContainer, SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		fTextLastName.setLayoutData(gd);
		fTextLastNameModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fCurrentPerson != null) {
					String lastName = ((Text) (e.widget)).getText();
					if (!lastName.equals(fCurrentPerson.getLastName())) {
						fCurrentPerson.setLastName(lastName);
						fIsPersonModified = true;

						fPeopleViewer.update(fCurrentPerson, null);
					}
				}
			}
		};

		// filler
		lbl = new Label(fPersonDetailContainer, SWT.NONE);
	}

	/**
	 * field: weight
	 */
	private void createFieldWeight(int floatInputWidth) {
		InputFieldFloat floatInput = new InputFieldFloat(
				fPersonDetailContainer,
				Messages.Pref_People_Label_weight,
				floatInputWidth);

		fTextWeight = floatInput.getTextField();
		fTextWeightModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fCurrentPerson != null) {
					Text control = (Text) e.widget;
					try {
						float value = Float.parseFloat(((Text) (e.widget)).getText());
						if (value != fCurrentPerson.getWeight()) {
							fCurrentPerson.setWeight(value);
							fPeopleViewer.update(fCurrentPerson, null);
						}
						UI.setDefaultColor(control);
					} catch (NumberFormatException e1) {
						UI.setErrorColor(control);
					}
					fIsPersonModified = true;
					validatePerson();
				}
			}
		};

		// filler
		Label lbl = new Label(fPersonDetailContainer, SWT.NONE);
	}

	// private class PeopleViewer extends TableViewer {
	//
	// public PeopleViewer(Table table) {
	// super(table);
	// }
	//
	// // protected void updateSelection(ISelection selection) {
	// // if (!fIsPersonModified) {
	// // super.updateSelection(selection);
	// // }
	// // }
	//
	// }

	private void createPeopleViewer(Composite container) {

		TableLayoutComposite layouter = new TableLayoutComposite(container, SWT.NONE);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = convertWidthInCharsToPixels(30);
		layouter.setLayoutData(gridData);

		final Table table = new Table(layouter, (SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER
				| SWT.FULL_SELECTION | SWT.MULTI));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn tc;

		tc = new TableColumn(table, SWT.NONE);
		layouter.addColumnData(new ColumnWeightData(1, convertWidthInCharsToPixels(1), false));

		tc = new TableColumn(table, SWT.NONE);
		tc.setText(Messages.Pref_People_Column_first_name);
		layouter.addColumnData(new ColumnWeightData(5, convertWidthInCharsToPixels(5)));

		tc = new TableColumn(table, SWT.NONE);
		tc.setText(Messages.Pref_People_Column_last_name);
		layouter.addColumnData(new ColumnWeightData(5, convertWidthInCharsToPixels(5)));

		tc = new TableColumn(table, SWT.NONE);
		tc.setText(Messages.Pref_People_Column_device);
		layouter.addColumnData(new ColumnWeightData(3, convertWidthInCharsToPixels(3)));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.Pref_People_Column_height);
		layouter.addColumnData(new ColumnWeightData(3, convertWidthInCharsToPixels(3)));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.Pref_People_Column_weight);
		layouter.addColumnData(new ColumnWeightData(3, convertWidthInCharsToPixels(3)));

		fPeopleViewer = new TableViewer(table);

		fPeopleViewer.setContentProvider(new ClientsContentProvider());
		fPeopleViewer.setLabelProvider(new ClientsLabelProvider());

		fPeopleViewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				return collator.compare(((TourPerson) e1).getLastName(), ((TourPerson) e2)
						.getLastName());
			}
		});

		fPeopleViewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				return ((TourPerson) e1).getLastName().compareTo(((TourPerson) e2).getLastName());
			}
		});

		fPeopleViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (fIsNewPerson) {
					fIsNewPerson = false;
				} else {
					savePerson();
				}

				showSelectedPersonDetails();
			}
		});

		fPeopleViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				fTextFirstName.setFocus();
				fTextFirstName.selectAll();
			}
		});
	}

	private void createPeopleViewerButtons(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginRight = 10;
		container.setLayout(gridLayout);

		// button: add
		fButtonAdd = new Button(container, SWT.NONE);
		fButtonAdd.setText(Messages.Pref_People_Button_add_person);
		setButtonLayoutData(fButtonAdd);
		fButtonAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onAddPerson();
				// enableButtons();
			}
		});

		// button: delete

		/*
		 * "Delete" button is disabled because the tours don't display the info
		 * that the person was removed
		 */
		// fButtonDelete = new Button(container, SWT.NONE);
		// fButtonDelete.setText("&Delete");
		// GridData gd = setButtonLayoutData(fButtonDelete);
		// gd.verticalIndent = 10;
		// fButtonDelete.addSelectionListener(new SelectionAdapter() {
		// public void widgetSelected(SelectionEvent e) {
		// onDeletePerson();
		// // enableButtons();
		// }
		// });
	}

	private void createPersonDetails(Composite parent) {

		GridLayout gl;
		GridData gd;
		final int floatInputWidth = convertHorizontalDLUsToPixels(40);

		// person data group
		Group groupPersonInfo = new Group(parent, SWT.NONE);
		groupPersonInfo.setText(Messages.Pref_People_Group_person);
		groupPersonInfo.setLayout(new GridLayout(1, false));
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		groupPersonInfo.setLayoutData(gd);

		fPersonDetailContainer = new Composite(groupPersonInfo, SWT.NONE);
		gl = new GridLayout(3, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		fPersonDetailContainer.setLayout(gl);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		fPersonDetailContainer.setLayoutData(gd);

		createFieldFirstName();
		createFieldLastName();
		createFieldWeight(floatInputWidth);
		createFieldHeight(floatInputWidth);
		createFieldDevice();
		createFieldBike();

		/**
		 * field: path to save raw tour data
		 */
		fRawDataPathEditor = new DirectoryFieldEditor(
				ITourbookPreferences.DUMMY_FIELD,
				Messages.Pref_People_Label_rawdata_path,
				fPersonDetailContainer);
		fRawDataPathEditor.setEmptyStringAllowed(true);

		// placeholder
		Label lbl;
		lbl = new Label(parent, SWT.NONE);
		lbl = new Label(parent, SWT.NONE);
	}

	/**
	 * Delete person from the the database
	 * 
	 * @param person
	 * @return
	 */
	private boolean deletePerson(TourPerson person) {

		if (removePersonFromTourData(person)) {
			if (deletePersonFromDb(person)) {
				return true;
			}
		}

		return false;
	}

	private boolean deletePersonFromDb(TourPerson person) {

		boolean returnResult = false;

		EntityManager em = TourDatabase.getInstance().getEntityManager();
		EntityTransaction ts = em.getTransaction();

		try {
			TourPerson entity = em.find(TourPerson.class, person.getPersonId());

			if (entity != null) {
				ts.begin();
				em.remove(entity);
				ts.commit();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				returnResult = true;
			}
			em.close();
		}

		return returnResult;
	}

	public void dispose() {

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(
				fPrefChangeListener);

		super.dispose();
	}

	private void enableButtons(boolean isPersonValid) {

		IStructuredSelection selection = (IStructuredSelection) fPeopleViewer.getSelection();

		boolean isPersonSelected = !selection.isEmpty();

		fButtonAdd.setEnabled(fCurrentPerson == null || (fCurrentPerson != null && isPersonValid));

		// fButtonDelete.setEnabled(isPersonSelected);
	}

	private void firePersonListModifyEvent() {
		if (fIsPersonListModified) {

			// fire bike list modify event
			getPreferenceStore().setValue(
					ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED,
					Math.random());

			fIsPersonListModified = false;
		}
	}

	public void init(IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	public boolean okToLeave() {
		if (validatePerson() == false) {
			return false;
		}
		savePerson();
		firePersonListModifyEvent();

		return super.okToLeave();
	}

	private void onAddPerson() {

		savePerson();

		fCurrentPerson = new TourPerson();

		fCurrentPerson.setLastName(""); //$NON-NLS-1$
		fCurrentPerson.setFirstName(""); //$NON-NLS-1$
		fCurrentPerson.setHeight(1.77f);
		fCurrentPerson.setWeight(80f);

		fPeople.add(fCurrentPerson);

		fIsPersonModified = true;
		fIsPersonListModified = true;

		// update ui viewer
		fPeopleViewer.add(fCurrentPerson);
		fIsNewPerson = true;
		fPeopleViewer.setSelection(new StructuredSelection(fCurrentPerson));
		validatePerson();

		// edit first name
		fTextFirstName.selectAll();
		fTextFirstName.setFocus();
	}

	private void onDeletePerson() {

		final IStructuredSelection selection = (IStructuredSelection) fPeopleViewer.getSelection();
		if (selection.isEmpty()) {
			return;
		}

		// ask for the reference tour name
		String[] buttons = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };

		MessageDialog dialog = new MessageDialog(
				this.getShell(),
				Messages.Pref_People_Dlg_del_person_title,
				null,
				Messages.Pref_People_Dlg_del_person_message,
				MessageDialog.QUESTION,
				buttons,
				1);

		if (dialog.open() != Window.OK) {
			return;
		}

		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {

				Table table = fPeopleViewer.getTable();
				int lastIndex = table.getSelectionIndex();

				for (Iterator iter = selection.iterator(); iter.hasNext();) {
					TourPerson person = (TourPerson) iter.next();

					deletePerson(person);

					// remove from data model
					fPeople.remove(person);
				}

				// remove from ui
				fPeopleViewer.remove(selection.toArray());

				// select next person
				if (lastIndex >= fPeople.size()) {
					table.setSelection(fPeople.size() - 1);
				} else {
					table.setSelection(lastIndex);
				}

				fCurrentPerson = null;
				fIsPersonModified = false;
				fIsPersonListModified = true;

				showSelectedPersonDetails();
			}
		});
	}

	public boolean performCancel() {
		firePersonListModifyEvent();
		return super.performCancel();
	}

	public boolean performOk() {
		if (validatePerson() == false) {
			return false;
		}
		savePerson();
		firePersonListModifyEvent();

		return super.performOk();
	}

	private void removeModifyListener() {

		fTextFirstName.removeModifyListener(fTextFirstNameModifyListener);
		fTextLastName.removeModifyListener(fTextLastNameModifyListener);
		fTextHeight.removeModifyListener(fTextHeightModifyListener);
		fTextWeight.removeModifyListener(fTextWeightModifyListener);

		fComboDevice.addModifyListener(fComboDeviceModifyListener);
		fComboBike.addModifyListener(fComboBikeModifyListener);

		fRawDataPathEditor.setPropertyChangeListener(null);
	}

	private boolean removePersonFromTourData(TourPerson person) {

		boolean returnResult = false;

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			Query query = em.createQuery("SELECT TourData " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" WHERE TourData.tourPerson.personId=" + person.getPersonId())); //$NON-NLS-1$

			ArrayList<TourData> tourDataList = (ArrayList<TourData>) query.getResultList();

			if (tourDataList.size() > 0) {

				EntityTransaction ts = em.getTransaction();

				try {

					ts.begin();

					// remove person from all saved tour data for this person
					for (TourData tourData : tourDataList) {
						tourData.setTourPerson(null);
						em.merge(tourData);
					}

					ts.commit();

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (ts.isActive()) {
						ts.rollback();
					}
				}
			}

			returnResult = true;
			em.close();
		}

		return returnResult;
	}

	/**
	 * save current person when it was modified
	 */
	private void savePerson() {

		if (fCurrentPerson != null && fIsPersonModified && validatePerson()) {

			fCurrentPerson.setRawDataPath(fRawDataPathEditor.getStringValue());
			fCurrentPerson.persist();

			// update modify flag before the viewer is updated
			fIsPersonModified = false;
			fIsPersonListModified = true;

			fPeopleViewer.update(fCurrentPerson, null);

			fIsPersonModified = false;
		}

	}

	private void selectBike(TourPerson person) {
		/*
		 * select bike in the combo box
		 */

		// select default value
		int bikeIndex = 0;
		TourBike personBike = person.getTourBike();

		if (personBike == null || fBikes == null) {
			fComboBike.select(0);
		} else {
			boolean isBikeFound = false;
			for (TourBike bike : fBikes) {
				if (personBike.getBikeId() == bike.getBikeId()) {
					fComboBike.select(bikeIndex + 1);
					isBikeFound = true;
					break;
				}
				bikeIndex++;
			}

			// when the bike id was not found, select "no selection" entry
			if (!isBikeFound) {
				fComboBike.select(0);
			}
		}
	}
	private void selectDevice(TourPerson person) {
		/*
		 * select device in the combo box
		 */

		int deviceIndex = 0;
		String deviceId = person.getDeviceReaderId();

		if (deviceId == null) {
			fComboDevice.select(0);
		} else {
			for (TourbookDevice device : fDeviceList) {
				if (device != null && deviceId.equals(device.deviceId)) {
					fComboDevice.select(deviceIndex);
					break;
				}
				deviceIndex++;
			}

			// when the device id was not found, select "no selection" entry
			if (deviceIndex == 0) {
				fComboDevice.select(0);
			}
		}
	}

	/**
	 * update person data fields from the selected person in the viewer
	 */
	private void showSelectedPersonDetails() {

		IStructuredSelection selection = (IStructuredSelection) fPeopleViewer.getSelection();

		Object item = selection.getFirstElement();
		boolean isEnabled = true;

		if (item instanceof TourPerson) {

			TourPerson person = (TourPerson) item;
			fCurrentPerson = person;

			removeModifyListener();

			fTextFirstName.setText(person.getFirstName());
			fTextLastName.setText(person.getLastName());
			fTextHeight.setText(Float.toString(person.getHeight()));
			fTextWeight.setText(Float.toString(person.getWeight()));

			selectDevice(person);
			selectBike(person);

			fRawDataPathEditor.setStringValue(person.getRawDataPath());

			addModifyListener();

		} else {
			isEnabled = false;
			fCurrentPerson = null;

			fTextFirstName.setText(""); //$NON-NLS-1$
			fTextLastName.setText(""); //$NON-NLS-1$
			fTextHeight.setText(""); //$NON-NLS-1$
			fTextWeight.setText(""); //$NON-NLS-1$

			fComboDevice.select(0);
			fComboBike.select(0);

			fRawDataPathEditor.setStringValue(null);
		}

		fTextFirstName.setEnabled(isEnabled);
		fTextLastName.setEnabled(isEnabled);
		fTextHeight.setEnabled(isEnabled);
		fTextWeight.setEnabled(isEnabled);

		fComboBike.setEnabled(isEnabled);
		fComboDevice.setEnabled(isEnabled);
		fRawDataPathEditor.setEnabled(isEnabled, fPersonDetailContainer);
	}

	private boolean validatePerson() {

		boolean isValid = false;

		if (fCurrentPerson == null) {
			isValid = true;
		} else if (fTextFirstName.getText().trim().equals("")) { //$NON-NLS-1$
			setErrorMessage(Messages.Pref_People_Error_first_name_is_required);

			// } else if (fComboDevice.getSelectionIndex() <= 0) {
			// setErrorMessage("A device is required");

		} else if (!fRawDataPathEditor.getStringValue().trim().equals("") //$NON-NLS-1$
				&& !fRawDataPathEditor.isValid()) {
			setErrorMessage(Messages.Pref_People_Error_path_is_invalid);

		} else if (true) {
			try {
				Float.parseFloat(fTextHeight.getText());
				Float.parseFloat(fTextWeight.getText());
				isValid = true;
			} catch (NumberFormatException e) {
				setErrorMessage(Messages.Pref_People_Error_invalid_number);
			}
		}

		enableButtons(isValid);

		if (isValid) {
			fPeopleViewer.getTable().setEnabled(true);
			setErrorMessage(null);
			return true;
		} else {

			fPeopleViewer.getTable().setEnabled(false);
			return false;
		}
	}

}
