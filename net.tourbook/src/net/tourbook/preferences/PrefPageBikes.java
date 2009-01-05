/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourBike;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.InputFieldFloat;
import net.tourbook.ui.UI;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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

public class PrefPageBikes extends PreferencePage implements IWorkbenchPreferencePage {

	private static final int	COLUMN_IS_MODIFIED	= 0;
	private static final int	COLUMN_NAME			= 1;
	private static final int	COLUMN_TYPE			= 2;
	private static final int	COLUMN_WEIGHT		= 3;

	private TableViewer			fBikeViewer;
	private Button				fButtonAdd;

	/*
	 * disabled 30.12.2007 because deleted bikes causes errors then the tourbike is stored in
	 * TourData
	 */
//	private Button				fButtonDelete;
	private Text				fTextBikeName;
	private Text				fTextWeight;
	private Combo				fComboBikeType;
	private Combo				fComboFrontTyre;
	private Combo				fComboRearTyre;

	private ArrayList<TourBike>	fBikes;
	private TourBike			fCurrentBike;
	private boolean				fIsBikeModified;
	private boolean				fIsBikeListModified	= false;

	private boolean				isMetricSystem		= true;

	private class BikeContentProvider implements IStructuredContentProvider {

		public BikeContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			if (fBikes == null) {
				fBikes = TourDatabase.getTourBikes();
			}
			if (fBikes == null) {
				return new Object[0];
			} else {
				return fBikes.toArray(new TourBike[fBikes.size()]);
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
				return fIsBikeModified ? "*" : ""; //$NON-NLS-1$ //$NON-NLS-2$

			case COLUMN_NAME:
				return bike.getName();

			case COLUMN_TYPE:
				return IBikeDefinitions.bikeType[bike.getTypeId()];

			case COLUMN_WEIGHT:
				return Float.toString(bike.getWeight());
			}
			return ""; //$NON-NLS-1$
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
		fTextBikeName = new Text(container, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		fTextBikeName.setLayoutData(gd);
		fTextBikeName.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (fCurrentBike != null) {
					final String name = ((Text) (e.widget)).getText();
					if (!name.equals(fCurrentBike.getName())) {
						fIsBikeModified = true;

						fCurrentBike.setName(name);
						fBikeViewer.update(fCurrentBike, null);
					}
				}
			}
		});

		/*
		 * field: bike type
		 */
		lbl = new Label(container, SWT.NONE);
		lbl.setText("&Type:"); //$NON-NLS-1$
		fComboBikeType = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
		fComboBikeType.setVisibleItemCount(20);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.widthHint = 50;
		fComboBikeType.setLayoutData(gd);
		fComboBikeType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				final int selectedIndex = fComboBikeType.getSelectionIndex();

				// select tyres
				fComboFrontTyre.select(IBikeDefinitions.i_tireF[selectedIndex]);
				fComboRearTyre.select(IBikeDefinitions.i_tireR[selectedIndex]);

				// set new weight
				final float weight = IBikeDefinitions.def_mr[selectedIndex] * (isMetricSystem ? 1 : 2.2f);
				fTextWeight.setText(Float.toString(weight));

				if (fCurrentBike != null) {

					fCurrentBike.setWeight(weight);

					// update viewer
					fBikeViewer.update(fCurrentBike, null);
				}
			}
		});

		fComboBikeType.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (fCurrentBike != null) {
					final int index = ((Combo) (e.widget)).getSelectionIndex();
					if (index != fCurrentBike.getTypeId()) {
						fCurrentBike.setTypeId(index);
						fCurrentBike.setFrontTyreId(fComboFrontTyre.getSelectionIndex());
						fCurrentBike.setRearTyreId(fComboRearTyre.getSelectionIndex());

						fIsBikeModified = true;
						fBikeViewer.update(fCurrentBike, null);
					}
				}
			}
		});
		for (final String bikeType : IBikeDefinitions.bikeType) {
			fComboBikeType.add(bikeType);
		}

		/*
		 * field: weight
		 */
		final InputFieldFloat floatInput = new InputFieldFloat(container, "&Weight (kg):", //$NON-NLS-1$
				convertHorizontalDLUsToPixels(40));

		fTextWeight = floatInput.getTextField();
		fTextWeight.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (fCurrentBike != null) {
					final Text control = (Text) e.widget;
					try {
						final float value = Float.parseFloat(((Text) (e.widget)).getText());
						if (value != fCurrentBike.getWeight()) {
							fCurrentBike.setWeight(value);

							fIsBikeModified = true;
							fBikeViewer.update(fCurrentBike, null);
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
		fComboFrontTyre = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
		fComboFrontTyre.setVisibleItemCount(20);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.widthHint = 50;
		fComboFrontTyre.setLayoutData(gd);
		fComboFrontTyre.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (fCurrentBike != null) {
					final int index = ((Combo) (e.widget)).getSelectionIndex();
					if (index != fCurrentBike.getFrontTyreId()) {
						fCurrentBike.setFrontTyreId(index);

						fIsBikeModified = true;
						fBikeViewer.update(fCurrentBike, null);
					}
				}
			}
		});
		for (final String tyre : IBikeDefinitions.tyreType) {
			fComboFrontTyre.add(tyre);
		}

		/*
		 * field: rear tyre
		 */
		lbl = new Label(container, SWT.NONE);
		lbl.setText("&Rear Tyre:"); //$NON-NLS-1$
		fComboRearTyre = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
		fComboRearTyre.setVisibleItemCount(20);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.widthHint = 50;
		fComboRearTyre.setLayoutData(gd);
		fComboRearTyre.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (fCurrentBike != null) {
					final int index = ((Combo) (e.widget)).getSelectionIndex();
					if (index != fCurrentBike.getRearTyreId()) {
						fCurrentBike.setRearTyreId(index);

						fIsBikeModified = true;
						fBikeViewer.update(fCurrentBike, null);
					}
				}
			}
		});
		for (final String tyre : IBikeDefinitions.tyreType) {
			fComboRearTyre.add(tyre);
		}

		// placeholder
		lbl = new Label(parent, SWT.NONE);
		lbl.setText(""); //$NON-NLS-1$
		lbl = new Label(parent, SWT.NONE);
		lbl.setText(""); //$NON-NLS-1$
		lbl = new Label(parent, SWT.NONE);
		lbl.setText(""); //$NON-NLS-1$
	}

	private void createBikeViewer(final Composite container) {

		final TableLayoutComposite layouter = new TableLayoutComposite(container, SWT.NONE);
		final GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = convertWidthInCharsToPixels(20);
		layouter.setLayoutData(gridData);

		final Table table = new Table(layouter,
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

		fBikeViewer = new TableViewer(table);

		fBikeViewer.setContentProvider(new BikeContentProvider());
		fBikeViewer.setLabelProvider(new BikeLabelProvider());

		fBikeViewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {
				return collator.compare(((TourBike) e1).getName(), ((TourBike) e2).getName());
			}
		});

		fBikeViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {
				return ((TourBike) e1).getName().compareTo(((TourBike) e2).getName());
			}

		});

		fBikeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				selectBike();
			}
		});

		fBikeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				fTextBikeName.setFocus();
				fTextBikeName.selectAll();
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
		fButtonAdd = new Button(container, SWT.NONE);
		fButtonAdd.setText("&Add..."); //$NON-NLS-1$
		setButtonLayoutData(fButtonAdd);
		fButtonAdd.addSelectionListener(new SelectionAdapter() {
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

		fBikeViewer.setInput(this);

		// select first bike
		fBikeViewer.getTable().setSelection(0);
		selectBike();

		// update the bike details
		// updateBikeInfo();

		return container;
	}

	/**
	 * Delete bike from the the database
	 * 
	 * @param bike
	 * @return
	 */
	private boolean deleteBike(final TourBike bike) {

		if (deleteBikeFromPerson(bike)) {
			if (deleteBikeFromDb(bike)) {
				return true;
			}
		}

		return false;
	}

	private boolean deleteBikeFromDb(final TourBike bike) {

		boolean returnResult = false;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();

		try {
			final TourBike entity = em.find(TourBike.class, bike.getBikeId());

			if (entity != null) {
				ts.begin();
				em.remove(entity);
				ts.commit();
			}

		} catch (final Exception e) {
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

	private boolean deleteBikeFromPerson(final TourBike bike) {

		boolean returnResult = false;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery("SELECT TourPerson " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_PERSON + " TourPerson ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" WHERE TourPerson.tourBike.bikeId=" + bike.getBikeId())); //$NON-NLS-1$

			final ArrayList<TourPerson> people = (ArrayList<TourPerson>) query.getResultList();

			if (people.size() > 0) {

				final EntityTransaction ts = em.getTransaction();

				try {

					ts.begin();

					// remove bike from all persons
					for (final TourPerson person : people) {
						person.setTourBike(null);
						em.merge(person);
					}

					ts.commit();

				} catch (final Exception e) {
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

	private void enableButtons() {
		final IStructuredSelection selection = (IStructuredSelection) fBikeViewer.getSelection();
//		fButtonDelete.setEnabled(!selection.isEmpty());
	}

	private void fireBikeListModifyEvent() {
		if (fIsBikeListModified) {

			// fire bike list modify event
			getPreferenceStore().setValue(ITourbookPreferences.TOUR_BIKE_LIST_IS_MODIFIED, Math.random());

			fIsBikeListModified = false;
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

		fCurrentBike = new TourBike();
		fIsBikeModified = true;
		fCurrentBike.setName("<name>"); //$NON-NLS-1$
		fCurrentBike.setWeight(10);

		fBikes.add(fCurrentBike);
		fIsBikeListModified = true;

		// update ui viewer
		fBikeViewer.add(fCurrentBike);
		fBikeViewer.setSelection(new StructuredSelection(fCurrentBike));

		// edit name field
		fTextBikeName.selectAll();
		fTextBikeName.setFocus();
	}

	private void onDeleteBike() {

		final IStructuredSelection selection = (IStructuredSelection) fBikeViewer.getSelection();
		if (selection.isEmpty()) {
			return;
		}

		// ask for the reference tour name
		final String[] buttons = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };

		final MessageDialog dialog = new MessageDialog(this.getShell(), "Delete Bike", //$NON-NLS-1$
				null,
				"Are you sure to delete the bike(s) and remove them from ALL related persons?", //$NON-NLS-1$
				MessageDialog.QUESTION,
				buttons,
				1);

		if (dialog.open() != Window.OK) {
			return;
		}

		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {

				final Table table = fBikeViewer.getTable();
				final int lastIndex = table.getSelectionIndex();

				for (final Iterator iter = selection.iterator(); iter.hasNext();) {
					final TourBike bike = (TourBike) iter.next();

					deleteBike(bike);

					// remove from data model
					fBikes.remove(bike);
				}

				// remove from ui
				fBikeViewer.remove(selection.toArray());

				// select next bike
				if (lastIndex >= fBikes.size()) {
					table.setSelection(fBikes.size() - 1);
				} else {
					table.setSelection(lastIndex);
				}

				fCurrentBike = null;

				fIsBikeModified = false;
				fIsBikeListModified = true;

				updateBikeDetails();
			}
		});
	}

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

		if (fCurrentBike != null && fIsBikeModified) {
			fCurrentBike.persist();

			// update modify flag
			fIsBikeModified = false;
			fBikeViewer.update(fCurrentBike, null);

			fIsBikeListModified = true;
		}

		fIsBikeModified = false;
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

		final IStructuredSelection selection = (IStructuredSelection) fBikeViewer.getSelection();

		final Object item = selection.getFirstElement();
		boolean isEnabled = true;

		if (item instanceof TourBike) {
			final TourBike bike = (TourBike) item;
			// set the current bike before the fields are updated
			fCurrentBike = bike;

			fTextBikeName.setText(bike.getName());
			fTextWeight.setText(Float.toString(bike.getWeight()));
			UI.setDefaultColor(fTextWeight);

			fComboBikeType.select(bike.getTypeId());
			fComboFrontTyre.select(bike.getFrontTyreId());
			fComboRearTyre.select(bike.getRearTyreId());

		} else {
			isEnabled = false;
			fCurrentBike = null;

			fTextBikeName.setText(""); //$NON-NLS-1$
			fTextWeight.setText(""); //$NON-NLS-1$
			fComboBikeType.select(0);
			fComboFrontTyre.select(0);
			fComboRearTyre.select(0);
		}

		fTextBikeName.setEnabled(isEnabled);
		fTextWeight.setEnabled(isEnabled);
		fComboBikeType.setEnabled(isEnabled);
		fComboFrontTyre.setEnabled(isEnabled);
		fComboRearTyre.setEnabled(isEnabled);
	}

}
