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
package net.tourbook.ui.views.tourCatalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Formatter;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TreeViewerItem;
import net.tourbook.ui.UI;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.PageBook;

public class WizardPageCompareTour extends WizardPage {

	// dialog settings
	private static final String		COMP_TOUR_VIEWER_WIDTH		= "CompTour.viewerWidth";			//$NON-NLS-1$
	private static final String		COMP_TOUR_SELECT_ALL		= "CompTour.selectAll";			//$NON-NLS-1$

	// tree columns
	static final int				COLUMN_DATE					= 0;
	static final int				COLUMN_DISTANCE				= 1;
	static final int				COLUMN_UP					= 2;
	static final int				COLUMN_RECORDING			= 3;

	private Button					fCheckSelectAll;
	private PageBook				fPageBook;
	private Label					fPageTourIsNotSelected;

	private CheckboxTreeViewer		fTourViewer;
	private TVIWizardCompareRoot	fRootItem;

	private TourChart				fTourChart;
	private ViewerDetailForm		fViewerDetailForm;

	private boolean					fIsTourViewerInitialized	= false;

	private NumberFormat			fNf							= NumberFormat.getNumberInstance();

	private Group					fChartGroup;

	private class TourContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		public Object[] getElements(final Object inputElement) {
			return fRootItem.getFetchedChildrenAsArray();
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	class TourLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}

		public String getColumnText(final Object obj, final int index) {

			if (obj instanceof TVIWizardCompareTour) {

				final TVIWizardCompareTour tourItem = (TVIWizardCompareTour) obj;

				switch (index) {
				case COLUMN_DATE:
					return ((TVIWizardCompareItem) obj).treeColumn;

				case COLUMN_DISTANCE:
					fNf.setMinimumFractionDigits(1);
					fNf.setMaximumFractionDigits(1);
					return fNf.format((tourItem.colDistance) / (1000 * UI.UNIT_VALUE_DISTANCE));

				case COLUMN_RECORDING:
					final long recordingTime = tourItem.colRecordingTime;
					return new Formatter().format(Messages.Format_hhmm,
							(recordingTime / 3600),
							((recordingTime % 3600) / 60)).toString();

				case COLUMN_UP:
					return Long.toString((long) (tourItem.colAltitudeUp / UI.UNIT_VALUE_ALTITUDE));

				default:
				}

			} else {
				if (index == COLUMN_DATE) {
					return ((TVIWizardCompareItem) obj).treeColumn;
				} else {
					return UI.EMPTY_STRING;
				}
			}

			return UI.EMPTY_STRING;
		}
	}

	WizardPageCompareTour() {

		super("compare-tour");//$NON-NLS-1$

		setTitle(Messages.tourCatalog_wizard_Page_compared_tours_title);
	}

	public void createControl(final Composite parent) {

		final Composite pageContainer = createUI(parent);

		restoreDialogSettings();

		// set the control, otherwise nothing is displayed
		setControl(pageContainer);

		validatePage();
	}

	private Control createTourViewer(final Composite parent) {

		initializeDialogUnits(parent);

		final TreeColumnLayout treeLayout = new TreeColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(treeLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		final Tree tree = new Tree(layoutContainer, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.MULTI
				| SWT.BORDER
				| SWT.FULL_SELECTION
				| SWT.CHECK);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		/*
		 * tree columns
		 */
		TreeColumn tc;

		tc = new TreeColumn(tree, SWT.NONE);
		tc.setText(Messages.tourCatalog_wizard_Column_tour);
		treeLayout.setColumnData(tc, new ColumnPixelData(convertWidthInCharsToPixels(20)));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(UI.UNIT_LABEL_DISTANCE);
		tc.setToolTipText(Messages.tourCatalog_wizard_Column_distance_tooltip);
		treeLayout.setColumnData(tc, new ColumnWeightData(10));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(UI.UNIT_LABEL_ALTITUDE);
		tc.setToolTipText(Messages.tourCatalog_wizard_Column_altitude_up_tooltip);
		treeLayout.setColumnData(tc, new ColumnWeightData(10));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.tourCatalog_wizard_Column_h);
		tc.setToolTipText(Messages.tourCatalog_wizard_Column_h_tooltip);
		treeLayout.setColumnData(tc, new ColumnWeightData(10));

		/*
		 * tree viewer
		 */
		fTourViewer = new ContainerCheckedTreeViewer(tree);
		fTourViewer.setContentProvider(new TourContentProvider());
		fTourViewer.setLabelProvider(new TourLabelProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				showCompareTour(event);
			}
		});

		fTourViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				validatePage();
			}
		});

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) fTourViewer.getSelection()).getFirstElement();
				if (selection != null) {

					// expand/collapse current item

					if (fTourViewer.getExpandedState(selection)) {
						fTourViewer.collapseToLevel(selection, 1);
					} else {
						fTourViewer.expandToLevel(selection, 1);
					}
				}
			}
		});

		return layoutContainer;
	}

	private Composite createUI(final Composite parent) {

		final Composite pageContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(pageContainer);

		fCheckSelectAll = new Button(pageContainer, SWT.CHECK);
		fCheckSelectAll.setText(Messages.tourCatalog_wizard_Action_select_all_tours);
		fCheckSelectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableTours(fCheckSelectAll.getSelection());
				validatePage();
			}
		});

		/*
		 * create master detail layout
		 */
		final Composite masterDetailContainer = new Composite(pageContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(masterDetailContainer);

		final Control viewer = createTourViewer(masterDetailContainer);

		final Sash sash = new Sash(masterDetailContainer, SWT.VERTICAL);

		// chart group
		fChartGroup = new Group(masterDetailContainer, SWT.NONE);
		fChartGroup.setLayout(new GridLayout());
		fChartGroup.setText(Messages.tourCatalog_wizard_Group_selected_tour);
		fChartGroup.setEnabled(false);

		fViewerDetailForm = new ViewerDetailForm(masterDetailContainer, viewer, sash, fChartGroup);

		/*
		 * create pagebook with the chart and the no-chart page
		 */
		fPageBook = new PageBook(fChartGroup, SWT.NONE);
		fPageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fTourChart = new TourChart(fPageBook, SWT.NONE, false);
		fTourChart.setBackgroundColor(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		fPageTourIsNotSelected = new Label(fPageBook, SWT.NONE);
		fPageTourIsNotSelected.setText(Messages.tourCatalog_wizard_Label_a_tour_is_not_selected);
		fPageTourIsNotSelected.setEnabled(false);

		return pageContainer;
	}

	/**
	 * enables/disables the controls which belong to the tour
	 * 
	 * @param isChecked
	 */
	private void enableTours(final boolean isChecked) {

		final boolean isEnabled = !isChecked;

		// load tour data into the viewer if not yet done
		if (isEnabled && fIsTourViewerInitialized == false) {

			BusyIndicator.showWhile(null, new Runnable() {
				public void run() {

					// initialize the data before the view input is set
					fRootItem = new TVIWizardCompareRoot();
					fTourViewer.setInput(this);

					fIsTourViewerInitialized = true;
				}
			});

		}

		fTourViewer.getControl().setEnabled(isEnabled);
	}

	private Long[] getAllTourIds() {

		final ArrayList<Long> allTourIds = new ArrayList<Long>();

		try {

			final String sqlString = "SELECT tourId FROM " + TourDatabase.TABLE_TOUR_DATA; //$NON-NLS-1$

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {
				allTourIds.add(result.getLong(1));
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return allTourIds.toArray(new Long[allTourIds.size()]);
	}

	/**
	 * @return return all checked tours
	 */
	public Object[] getComparedTours() {

		if (fCheckSelectAll.getSelection()) {

			// return all tours

			return getAllTourIds();

		} else {

			return fTourViewer.getCheckedElements();
		}
	}

	void persistDialogSettings() {

		final IDialogSettings wizardSettings = getDialogSettings();

		// save the viewer width
		wizardSettings.put(COMP_TOUR_VIEWER_WIDTH, fTourViewer.getTree().getSize().x);

		wizardSettings.put(COMP_TOUR_SELECT_ALL, fCheckSelectAll.getSelection());
	}

	private void restoreDialogSettings() {

		final IDialogSettings wizardSettings = getDialogSettings();

		// restore viewer width
		Integer viewerWidth = null;
		try {
			viewerWidth = wizardSettings.getInt(COMP_TOUR_VIEWER_WIDTH);
		} catch (final NumberFormatException e) {}
		fViewerDetailForm.setViewerWidth(viewerWidth);

		// restore checkbox: select all tours
		final boolean isSelectAllTours = wizardSettings.getBoolean(COMP_TOUR_SELECT_ALL);
		fCheckSelectAll.setSelection(isSelectAllTours);

		enableTours(isSelectAllTours);
	}

	private void showCompareTour(final SelectionChangedEvent event) {

		final IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		if (selection != null) {

			final Object firstElement = selection.getFirstElement();
			if (firstElement instanceof TVIWizardCompareTour) {

				final TVIWizardCompareTour tourItem = (TVIWizardCompareTour) firstElement;

				// get tour data from the database
				final TourData tourData = TourManager.getInstance().getTourData(tourItem.tourId);

				// set altitude visible
				final TourChartConfiguration chartConfig = new TourChartConfiguration(true);
				chartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

				fTourChart.updateTourChart(tourData, chartConfig, false);

				fChartGroup.setText(NLS.bind(Messages.tourCatalog_wizard_Group_selected_tour_2,
						TourManager.getTourDateShort(tourData)));

				fPageBook.showPage(fTourChart);

			} else {
				fPageBook.showPage(fPageTourIsNotSelected);
				fChartGroup.setText(UI.EMPTY_STRING);
			}
		}
	}

	private boolean validatePage() {

		setMessage(Messages.tourCatalog_wizard_Label_page_message);

		if (fCheckSelectAll.getSelection()) {

			setPageComplete(true);
			setErrorMessage(null);
			return true;

		} else {

			final Object[] checkedElements = fTourViewer.getCheckedElements();

			if (checkedElements.length == 0) {
				setPageComplete(false);
				setErrorMessage(Messages.tourCatalog_wizard_Error_tour_must_be_selected);
				return false;

			} else {
				setPageComplete(true);
				setErrorMessage(null);
				return true;
			}
		}
	}

}
