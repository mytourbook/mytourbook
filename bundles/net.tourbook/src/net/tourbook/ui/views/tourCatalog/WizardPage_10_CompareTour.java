/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.common.UI;
import net.tourbook.common.form.SashLeftFixedForm;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

public class WizardPage_10_CompareTour extends WizardPage {

	private static final String		COMP_TOUR_VIEWER_WIDTH		= "CompTour.viewerWidth";			//$NON-NLS-1$
	private static final String		COMP_TOUR_SELECT_ALL		= "CompTour.selectAll";			//$NON-NLS-1$

	final IPreferenceStore			_prefStore					= TourbookPlugin.getPrefStore();

	private TVIWizardCompareRoot	_rootItem;
	private SashLeftFixedForm		_viewerDetailForm;

	private boolean					_isTourViewerInitialized	= false;

	private NumberFormat			_nf1						= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	/*
	 * UI controls
	 */
	private PageBook				_pageBook;
	private Label					_pageTourIsNotSelected;

	private Button					_chkSelectAll;
	private Chart					_compareTourChart;
	private Group					_groupChart;
	private CheckboxTreeViewer		_tourViewer;

	private class TourContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _rootItem.getFetchedChildrenAsArray();
		}

		@Override
		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		@Override
		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	WizardPage_10_CompareTour() {

		super("compare-tour");//$NON-NLS-1$

		setTitle(Messages.tourCatalog_wizard_Page_compared_tours_title);
	}

	@Override
	public void createControl(final Composite parent) {

		final Composite pageContainer = createUI(parent);

		restoreState();

		// set the control, otherwise nothing is displayed
		setControl(pageContainer);

		validatePage();
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			/*
			 * create master detail layout
			 */
			final Composite detailContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(detailContainer);
//			detailContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			{
				final Control viewer = createUI_10_TourViewer(detailContainer);
				final Sash sash = new Sash(detailContainer, SWT.VERTICAL);
				final Composite tourChart = createUI_50_TourChart(parent, detailContainer);

				_viewerDetailForm = new SashLeftFixedForm(detailContainer, viewer, sash, tourChart);
			}
			{
				_chkSelectAll = new Button(container, SWT.CHECK);
				_chkSelectAll.setText(Messages.tourCatalog_wizard_Action_select_all_tours);
				_chkSelectAll.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						enableTours(_chkSelectAll.getSelection());
						validatePage();
					}
				});
			}
		}

		return container;
	}

	private Control createUI_10_TourViewer(final Composite parent) {

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

		_tourViewer = new ContainerCheckedTreeViewer(tree);

		defineAllColumns(treeLayout);

		/*
		 * Setup viewer
		 */
		_tourViewer.setContentProvider(new TourContentProvider());
		_tourViewer.setUseHashlookup(true);

		_tourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				showCompareTour(event);
			}
		});

		_tourViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(final CheckStateChangedEvent event) {
				validatePage();
			}
		});

		_tourViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) _tourViewer.getSelection()).getFirstElement();
				if (selection != null) {

					// expand/collapse current item

					if (_tourViewer.getExpandedState(selection)) {
						_tourViewer.collapseToLevel(selection, 1);
					} else {
						_tourViewer.expandToLevel(selection, 1);
					}
				}
			}
		});

		return layoutContainer;
	}

	private Composite createUI_50_TourChart(final Composite parent, final Composite detailContainer) {

		// chart group
		_groupChart = new Group(detailContainer, SWT.NONE);
		_groupChart.setLayout(new GridLayout());
		_groupChart.setText(Messages.tourCatalog_wizard_Group_selected_tour);
		_groupChart.setEnabled(false);
		{
			/*
			 * create pagebook with the chart and the no-chart page
			 */
			_pageBook = new PageBook(_groupChart, SWT.NONE);
			_pageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			{
				_compareTourChart = new Chart(_pageBook, SWT.NONE);
				_compareTourChart.setBackgroundColor(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

				net.tourbook.ui.UI.updateChartProperties(
						_compareTourChart,
						WizardTourComparer.GRID_PREFIX_REF_TOUR_COMPARE_TOUR);

				// show title
				_compareTourChart.getChartTitleSegmentConfig().isShowSegmentTitle = true;

				_pageTourIsNotSelected = new Label(_pageBook, SWT.NONE);
				_pageTourIsNotSelected.setText(Messages.tourCatalog_wizard_Label_a_tour_is_not_selected);
				_pageTourIsNotSelected.setEnabled(false);
			}
		}

		return _groupChart;
	}

	/**
	 * Create all columns.
	 */
	private void defineAllColumns(final TreeColumnLayout treeLayout) {

		defineColumn_Date(treeLayout);
		defineColumn_Distance(treeLayout);
		defineColumn_AltitudeUp(treeLayout);
		defineColumn_RecordingTime(treeLayout);
	}

	private void defineColumn_AltitudeUp(final TreeColumnLayout treeLayout) {

		final TreeViewerColumn tvc = new TreeViewerColumn(_tourViewer, SWT.TRAIL);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIWizardCompareTour) {

					final TVIWizardCompareTour tourItem = (TVIWizardCompareTour) element;

					final long value = (long) (tourItem.colAltitudeUp / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE);
					cell.setText(Long.toString(value));

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		final TreeColumn tc = tvc.getColumn();
		tc.setText(UI.UNIT_LABEL_ALTITUDE);
		tc.setToolTipText(Messages.tourCatalog_wizard_Column_altitude_up_tooltip);
		treeLayout.setColumnData(tc, new ColumnWeightData(10));
	}

	private void defineColumn_Date(final TreeColumnLayout treeLayout) {

		final TreeViewerColumn tvc = new TreeViewerColumn(_tourViewer, SWT.LEAD);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIWizardCompareYear) {
					cell.setText(((TVIWizardCompareYear) element).treeColumn);
				} else if (element instanceof TVIWizardCompareMonth) {
					cell.setText(((TVIWizardCompareMonth) element).treeColumn);
				} else if (element instanceof TVIWizardCompareTour) {
					cell.setText(((TVIWizardCompareTour) element).treeColumn);
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		final TreeColumn tc = tvc.getColumn();
		tc.setText(Messages.tourCatalog_wizard_Column_tour);
		treeLayout.setColumnData(tc, new ColumnPixelData(convertWidthInCharsToPixels(20)));
	}

	private void defineColumn_Distance(final TreeColumnLayout treeLayout) {

		final TreeViewerColumn tvc = new TreeViewerColumn(_tourViewer, SWT.TRAIL);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIWizardCompareTour) {

					final TVIWizardCompareTour tourItem = (TVIWizardCompareTour) element;

					final float distance = (tourItem.colDistance) / (1000 * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);

					cell.setText(_nf1.format(distance));

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		final TreeColumn tc = tvc.getColumn();
		tc.setText(UI.UNIT_LABEL_DISTANCE);
		tc.setToolTipText(Messages.tourCatalog_wizard_Column_distance_tooltip);
		treeLayout.setColumnData(tc, new ColumnWeightData(10));
	}

	private void defineColumn_RecordingTime(final TreeColumnLayout treeLayout) {

		final TreeViewerColumn tvc = new TreeViewerColumn(_tourViewer, SWT.TRAIL);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIWizardCompareTour) {

					final TVIWizardCompareTour tourItem = (TVIWizardCompareTour) element;

					final long recordingTime = tourItem.colRecordingTime;

					cell.setText(String.format(//
							Messages.Format_hhmm,
							(recordingTime / 3600),
							((recordingTime % 3600) / 60)));

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		final TreeColumn tc = tvc.getColumn();

		tc.setText(Messages.tourCatalog_wizard_Column_h);
		tc.setToolTipText(Messages.tourCatalog_wizard_Column_h_tooltip);
		treeLayout.setColumnData(tc, new ColumnWeightData(10));
	}

	/**
	 * enables/disables the controls which belong to the tour
	 * 
	 * @param isChecked
	 */
	private void enableTours(final boolean isChecked) {

		final boolean isEnabled = !isChecked;

		// load tour data into the viewer if not yet done
		if (isEnabled && _isTourViewerInitialized == false) {

			BusyIndicator.showWhile(null, new Runnable() {
				@Override
				public void run() {

					// initialize the data before the view input is set
					_rootItem = new TVIWizardCompareRoot();
					_tourViewer.setInput(this);

					_isTourViewerInitialized = true;
				}
			});

		}

		_tourViewer.getControl().setEnabled(isEnabled);
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
			net.tourbook.ui.UI.showSQLException(e);
		}

		return allTourIds.toArray(new Long[allTourIds.size()]);
	}

	/**
	 * @return return all checked tours
	 */
	public Object[] getComparedTours() {

		if (_chkSelectAll.getSelection()) {

			// return all tours

			return getAllTourIds();

		} else {

			return _tourViewer.getCheckedElements();
		}
	}

	private void restoreState() {

		final IDialogSettings wizardSettings = getDialogSettings();

		// restore viewer width
		Integer viewerWidth = null;
		try {
			viewerWidth = wizardSettings.getInt(COMP_TOUR_VIEWER_WIDTH);
		} catch (final NumberFormatException e) {}
		_viewerDetailForm.setViewerWidth(viewerWidth);

		// restore checkbox: select all tours
		final boolean isSelectAllTours = wizardSettings.getBoolean(COMP_TOUR_SELECT_ALL);
		_chkSelectAll.setSelection(isSelectAllTours);

		enableTours(isSelectAllTours);
	}

	void saveState() {

		final IDialogSettings wizardSettings = getDialogSettings();

		// save the viewer width
		wizardSettings.put(COMP_TOUR_VIEWER_WIDTH, _tourViewer.getTree().getSize().x);

		wizardSettings.put(COMP_TOUR_SELECT_ALL, _chkSelectAll.getSelection());
	}

	@Override
	public void setVisible(final boolean visible) {

		super.setVisible(visible);

		final boolean isSelectAll = _chkSelectAll.getSelection();

		if (isSelectAll) {

			_chkSelectAll.setFocus();

		} else {

			_tourViewer.getTree().setFocus();
		}
	}

	private void showCompareTour(final SelectionChangedEvent event) {

		final IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		if (selection == null) {
			return;
		}

		final Object firstElement = selection.getFirstElement();
		if (firstElement instanceof TVIWizardCompareTour) {

			final TVIWizardCompareTour tourItem = (TVIWizardCompareTour) firstElement;

			// get tour data from the database
			final TourData tourData = TourManager.getInstance().getTourData(tourItem.tourId);

			// set altitude visible
			final TourChartConfiguration chartConfig = new TourChartConfiguration(true);
			chartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

//				fTourChart.updateTourChart(tourData, chartConfig, false);

			final ChartDataModel chartDataModel = TourManager.getInstance().createChartDataModel(tourData, chartConfig);

			// set grid size
//				final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
//				fTourChart.setGridDistance(prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
//						prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

			_compareTourChart.updateChart(chartDataModel, false);

			_groupChart.setText(NLS.bind(
					Messages.tourCatalog_wizard_Group_selected_tour_2,
					TourManager.getTourDateShort(tourData)));

			_pageBook.showPage(_compareTourChart);

		} else {

			_pageBook.showPage(_pageTourIsNotSelected);
			_groupChart.setText(UI.EMPTY_STRING);
		}
	}

	private boolean validatePage() {

		setMessage(Messages.tourCatalog_wizard_Label_page_message);

		if (_chkSelectAll.getSelection()) {

			setPageComplete(true);
			setErrorMessage(null);
			return true;

		} else {

			final Object[] checkedElements = _tourViewer.getCheckedElements();

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
