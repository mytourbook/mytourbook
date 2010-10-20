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
package net.tourbook.preferences;

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourBike;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.InputFieldFloat;
import net.tourbook.ui.UI;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.jface.preference.PreferencePage;
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
import org.eclipse.swt.SWT;
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

public class PrefPageBikes extends PreferencePage implements IWorkbenchPreferencePage {

	private static final int	COLUMN_IS_MODIFIED	= 0;
	private static final int	COLUMN_NAME			= 1;
	private static final int	COLUMN_TYPE			= 2;
	private static final int	COLUMN_WEIGHT		= 3;

	private TableViewer			_bikeViewer;
	private Button				_btnAdd;

	/*
	 * disabled 30.12.2007 because deleted bikes causes errors then the tourbike is stored in
	 * TourData
	 */
//	private Button				fButtonDelete;
	private Text				_txtBikeName;
	private Text				_txtWeight;
	private Combo				_cboBikeType;
	private Combo				_cboFrontTyre;
	private Combo				_cboRearTyre;

	private ArrayList<TourBike>	_bikes;
	private TourBike			_currentBike;
	private boolean				_isBikeModified;
	private boolean				_isBikeListModified	= false;

	private final boolean		_isMetricSystem		= true;

	private class BikeContentProvider implements IStructuredContentProvider {

		public BikeContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			if (_bikes == null) {
				_bikes = TourDatabase.getTourBikes();
			}
			if (_bikes == null) {
				return new Object[0];
			} else {
				return _bikes.toArray(new TourBike[_bikes.size()]);
			}
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private class BikeLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}

		public String getColumnText(final Object obj, final int index) {

			final TourBike bike = ((TourBike) obj);

			switch (index) {
			case COLUMN_IS_MODIFIED:
				return _isBikeModified ? "*" : UI.EMPTY_STRING; //$NON-NLS-1$

			case COLUMN_NAME:
				return bike.getName();

			case COLUMN_TYPE:
				return IBikeDefinitions.bikeType[bike.getTypeId()];

			case COLUMN_WEIGHT:
				return Float.toString(bike.getWeight());
			}
			return UI.EMPTY_STRING;
		}
	}

	private void createBikeDetails(final Composite parent) {

		GridLayout gl;
		GridData gd;
		Label lbl;

		// group: units for the x-axis
		final Group groupBikeInfo = new Group(parent, SWT.NONE);
		groupBikeInfo.setText("Bike Data"); //$NON-NLS-1$
		groupBikeInfo.setLayout(new GridLayout(1, false));
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		groupBikeInfo.setLayoutData(gd);

		final Composite container = new Composite(groupBikeInfo, SWT.NONE);
		gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		container.setLayout(gl);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		container.setLayoutData(gd);

		/*
		 * field: bike name
		 */
		lbl = new Label(container, SWT.NONE);
		lbl.setText("&Name:"); //$NON-NLS-1$
		_txtBikeName = new Text(container, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		_txtBikeName.setLayoutData(gd);
		_txtBikeName.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (_currentBike != null) {
					final String name = ((Text) (e.widget)).getText();
					if (!name.equals(_currentBike.getName())) {
						_isBikeModified = true;

						_currentBike.setName(name);
						_bikeViewer.update(_currentBike, null);
					}
				}
			}
		});

		/*
		 * field: bike type
		 */
		lbl = new Label(container, SWT.NONE);
		lbl.setText("&Type:"); //$NON-NLS-1$
		_cboBikeType = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
		_cboBikeType.setVisibleItemCount(20);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.widthHint = 50;
		_cboBikeType.setLayoutData(gd);
		_cboBikeType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				final int selectedIndex = _cboBikeType.getSelectionIndex();

				// select tyres
				_cboFrontTyre.select(IBikeDefinitions.i_tireF[selectedIndex]);
				_cboRearTyre.select(IBikeDefinitions.i_tireR[selectedIndex]);

				// set new weight
				final float weight = IBikeDefinitions.def_mr[selectedIndex] * (_isMetricSystem ? 1 : 2.2f);
				_txtWeight.setText(Float.toString(weight));

				if (_currentBike != null) {

					_currentBike.setWeight(weight);

					// update viewer
					_bikeViewer.update(_currentBike, null);
				}
			}
		});

		_cboBikeType.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (_currentBike != null) {
					final int index = ((Combo) (e.widget)).getSelectionIndex();
					if (index != _currentBike.getTypeId()) {
						_currentBike.setTypeId(index);
						_currentBike.setFrontTyreId(_cboFrontTyre.getSelectionIndex());
						_currentBike.setRearTyreId(_cboRearTyre.getSelectionIndex());

						_isBikeModified = true;
						_bikeViewer.update(_currentBike, null);
					}
				}
			}
		});
		for (final String bikeType : IBikeDefinitions.bikeType) {
			_cboBikeType.add(bikeType);
		}

		/*
		 * field: weight
		 */
		final InputFieldFloat floatInput = new InputFieldFloat(container, "&Weight (kg):", //$NON-NLS-1$
				convertHorizontalDLUsToPixels(40));

		_txtWeight = floatInput.getTextField();
		_txtWeight.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (_currentBike != null) {
					final Text control = (Text) e.widget;
					try {
						final float value = Float.parseFloat(((Text) (e.widget)).getText());
						if (value != _currentBike.getWeight()) {
							_currentBike.setWeight(value);

							_isBikeModified = true;
							_bikeViewer.update(_currentBike, null);
						}
						UI.setDefaultColor(control);
					} catch (final NumberFormatException e1) {
						UI.setErrorColor(control);
					}
				}
			}
		});

		/*
		 * field: front tyre
		 */
		lbl = new Label(container, SWT.NONE);
		lbl.setText("&Front Tyre:"); //$NON-NLS-1$
		_cboFrontTyre = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
		_cboFrontTyre.setVisibleItemCount(20);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.widthHint = 50;
		_cboFrontTyre.setLayoutData(gd);
		_cboFrontTyre.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (_currentBike != null) {
					final int index = ((Combo) (e.widget)).getSelectionIndex();
					if (index != _currentBike.getFrontTyreId()) {
						_currentBike.setFrontTyreId(index);

						_isBikeModified = true;
						_bikeViewer.update(_currentBike, null);
					}
				}
			}
		});
		for (final String tyre : IBikeDefinitions.tyreType) {
			_cboFrontTyre.add(tyre);
		}

		/*
		 * field: rear tyre
		 */
		lbl = new Label(container, SWT.NONE);
		lbl.setText("&Rear Tyre:"); //$NON-NLS-1$
		_cboRearTyre = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
		_cboRearTyre.setVisibleItemCount(20);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.widthHint = 50;
		_cboRearTyre.setLayoutData(gd);
		_cboRearTyre.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (_currentBike != null) {
					final int index = ((Combo) (e.widget)).getSelectionIndex();
					if (index != _currentBike.getRearTyreId()) {
						_currentBike.setRearTyreId(index);

						_isBikeModified = true;
						_bikeViewer.update(_currentBike, null);
					}
				}
			}
		});
		for (final String tyre : IBikeDefinitions.tyreType) {
			_cboRearTyre.add(tyre);
		}

		// placeholder
		lbl = new Label(parent, SWT.NONE);
		lbl.setText(UI.EMPTY_STRING);
		lbl = new Label(parent, SWT.NONE);
		lbl.setText(UI.EMPTY_STRING);
		lbl = new Label(parent, SWT.NONE);
		lbl.setText(UI.EMPTY_STRING);
	}

	private void createBikeViewer(final Composite container) {

		final TableLayoutComposite layouter = new TableLayoutComposite(container, SWT.NONE);
		final GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = convertWidthInCharsToPixels(20);
		layouter.setLayoutData(gridData);

		final Table table = new Table(
				layouter,
				(SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn tc;

		tc = new TableColumn(table, SWT.NONE);
		layouter.addColumnData(new ColumnWeightData(1, convertWidthInCharsToPixels(1), false));

		tc = new TableColumn(table, SWT.NONE);
		tc.setText("Name"); //$NON-NLS-1$
		layouter.addColumnData(new ColumnWeightData(8));

		tc = new TableColumn(table, SWT.NONE);
		tc.setText("Type"); //$NON-NLS-1$
		layouter.addColumnData(new ColumnWeightData(16));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText("kg"); //$NON-NLS-1$
		layouter.addColumnData(new ColumnWeightData(4));

		_bikeViewer = new TableViewer(table);

		_bikeViewer.setContentProvider(new BikeContentProvider());
		_bikeViewer.setLabelProvider(new BikeLabelProvider());

		_bikeViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {
				return ((TourBike) e1).getName().compareTo(((TourBike) e2).getName());
			}

		});

		_bikeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				selectBike();
			}
		});

		_bikeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				_txtBikeName.setFocus();
				_txtBikeName.selectAll();
			}
		});
	}

	private void createBikeViewerButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginRight = 10;
		container.setLayout(gridLayout);

		// button: add
		_btnAdd = new Button(container, SWT.NONE);
		_btnAdd.setText("&Add..."); //$NON-NLS-1$
		setButtonLayoutData(_btnAdd);
		_btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onAddBike();
			}
		});

		// button: delete
//		fButtonDelete = new Button(container, SWT.NONE);
//		fButtonDelete.setText("&Delete"); //$NON-NLS-1$
//		GridData gd = setButtonLayoutData(fButtonDelete);
//		gd.verticalIndent = 10;
//		fButtonDelete.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				onDeleteBike();
//				enableButtons();
//			}
//		});
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Label label = new Label(parent, SWT.WRAP);
//		label.setText("Bikes are used to calculate the power."); //$NON-NLS-1$
		label.setText("This feature is currently not used for any calculation !!!"); //$NON-NLS-1$

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		final GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		container.setLayout(gl);
		container.setLayoutData(new GridData(SWT.NONE, SWT.FILL, true, true));

		createBikeViewer(container);
		createBikeViewerButtons(container);
		createBikeDetails(container);

		_bikeViewer.setInput(this);

		// select first bike
		_bikeViewer.getTable().setSelection(0);
		selectBike();

		// update the bike details
		// updateBikeInfo();

		return container;
	}

//	/**
//	 * Delete bike from the the database
//	 *
//	 * @param bike
//	 * @return
//	 */
//	private boolean deleteBike(final TourBike bike) {
//
//		if (deleteBikeFromPerson(bike)) {
//			if (deleteBikeFromDb(bike)) {
//				return true;
//			}
//		}
//
//		return false;
//	}

//	private boolean deleteBikeFromDb(final TourBike bike) {
//
//		boolean returnResult = false;
//
//		final EntityManager em = TourDatabase.getInstance().getEntityManager();
//		final EntityTransaction ts = em.getTransaction();
//
//		try {
//			final TourBike entity = em.find(TourBike.class, bike.getBikeId());
//
//			if (entity != null) {
//				ts.begin();
//				em.remove(entity);
//				ts.commit();
//			}
//
//		} catch (final Exception e) {
//			e.printStackTrace();
//		} finally {
//			if (ts.isActive()) {
//				ts.rollback();
//			} else {
//				returnResult = true;
//			}
//			em.close();
//		}
//
//		return returnResult;
//	}
//
//	private boolean deleteBikeFromPerson(final TourBike bike) {
//
//		boolean returnResult = false;
//
//		final EntityManager em = TourDatabase.getInstance().getEntityManager();
//
//		if (em != null) {
//
//			final Query query = em.createQuery(//
//					"SELECT tourPerson" //$NON-NLS-1$
//							+ (" FROM TourPerson AS tourPerson") //$NON-NLS-1$
//							+ (" WHERE tourPerson.tourBike.bikeId=" + bike.getBikeId())); //$NON-NLS-1$
//
//			final ArrayList<TourPerson> people = (ArrayList<TourPerson>) query.getResultList();
//
//			if (people.size() > 0) {
//
//				final EntityTransaction ts = em.getTransaction();
//
//				try {
//
//					ts.begin();
//
//					// remove bike from all persons
//					for (final TourPerson person : people) {
//						person.setTourBike(null);
//						em.merge(person);
//					}
//
//					ts.commit();
//
//				} catch (final Exception e) {
//					e.printStackTrace();
//				} finally {
//					if (ts.isActive()) {
//						ts.rollback();
//					}
//				}
//			}
//
//			returnResult = true;
//			em.close();
//		}
//
//		return returnResult;
//	}

	private void enableButtons() {
//		final IStructuredSelection selection = (IStructuredSelection) _bikeViewer.getSelection();
//		fButtonDelete.setEnabled(!selection.isEmpty());
	}

	private void fireBikeListModifyEvent() {
		if (_isBikeListModified) {

			// fire bike list modify event
			getPreferenceStore().setValue(ITourbookPreferences.TOUR_BIKE_LIST_IS_MODIFIED, Math.random());

			_isBikeListModified = false;
		}
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public boolean okToLeave() {
		saveBike();
		fireBikeListModifyEvent();

		return super.okToLeave();
	}

	private void onAddBike() {

		saveBike();

		_currentBike = new TourBike();
		_isBikeModified = true;
		_currentBike.setName("<name>"); //$NON-NLS-1$
		_currentBike.setWeight(10);

		_bikes.add(_currentBike);
		_isBikeListModified = true;

		// update ui viewer
		_bikeViewer.add(_currentBike);
		_bikeViewer.setSelection(new StructuredSelection(_currentBike));

		// edit name field
		_txtBikeName.selectAll();
		_txtBikeName.setFocus();
	}

//	private void onDeleteBike() {
//
//		final IStructuredSelection selection = (IStructuredSelection) _bikeViewer.getSelection();
//		if (selection.isEmpty()) {
//			return;
//		}
//
//		// ask for the reference tour name
//		final String[] buttons = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };
//
//		final MessageDialog dialog = new MessageDialog(this.getShell(), "Delete Bike", //$NON-NLS-1$
//				null,
//				"Are you sure to delete the bike(s) and remove them from ALL related persons?", //$NON-NLS-1$
//				MessageDialog.QUESTION,
//				buttons,
//				1);
//
//		if (dialog.open() != Window.OK) {
//			return;
//		}
//
//		BusyIndicator.showWhile(null, new Runnable() {
//			public void run() {
//
//				final Table table = _bikeViewer.getTable();
//				final int lastIndex = table.getSelectionIndex();
//
//				for (final Iterator iter = selection.iterator(); iter.hasNext();) {
//					final TourBike bike = (TourBike) iter.next();
//
//					deleteBike(bike);
//
//					// remove from data model
//					_bikes.remove(bike);
//				}
//
//				// remove from ui
//				_bikeViewer.remove(selection.toArray());
//
//				// select next bike
//				if (lastIndex >= _bikes.size()) {
//					table.setSelection(_bikes.size() - 1);
//				} else {
//					table.setSelection(lastIndex);
//				}
//
//				_currentBike = null;
//
//				_isBikeModified = false;
//				_isBikeListModified = true;
//
//				updateBikeDetails();
//			}
//		});
//	}

	@Override
	public boolean performCancel() {
		fireBikeListModifyEvent();
		return super.performCancel();
	}

	@Override
	public boolean performOk() {
		saveBike();
		fireBikeListModifyEvent();

		return super.performOk();
	}

	/**
	 * save current bike when it was modified
	 */
	private void saveBike() {

		if (_currentBike != null && _isBikeModified) {
			_currentBike.persist();

			// update modify flag
			_isBikeModified = false;
			_bikeViewer.update(_currentBike, null);

			_isBikeListModified = true;
		}

		_isBikeModified = false;
	}

	private void selectBike() {
		saveBike();
		updateBikeDetails();
		enableButtons();
	}

	/**
	 * update bike fields from selected bike in the viewer
	 */
	private void updateBikeDetails() {

		final IStructuredSelection selection = (IStructuredSelection) _bikeViewer.getSelection();

		final Object item = selection.getFirstElement();
		boolean isEnabled = true;

		if (item instanceof TourBike) {
			final TourBike bike = (TourBike) item;
			// set the current bike before the fields are updated
			_currentBike = bike;

			_txtBikeName.setText(bike.getName());
			_txtWeight.setText(Float.toString(bike.getWeight()));
			UI.setDefaultColor(_txtWeight);

			_cboBikeType.select(bike.getTypeId());
			_cboFrontTyre.select(bike.getFrontTyreId());
			_cboRearTyre.select(bike.getRearTyreId());

		} else {
			isEnabled = false;
			_currentBike = null;

			_txtBikeName.setText(UI.EMPTY_STRING);
			_txtWeight.setText(UI.EMPTY_STRING);
			_cboBikeType.select(0);
			_cboFrontTyre.select(0);
			_cboRearTyre.select(0);
		}

		_txtBikeName.setEnabled(isEnabled);
		_txtWeight.setEnabled(isEnabled);
		_cboBikeType.setEnabled(isEnabled);
		_cboFrontTyre.setEnabled(isEnabled);
		_cboRearTyre.setEnabled(isEnabled);
	}

}
