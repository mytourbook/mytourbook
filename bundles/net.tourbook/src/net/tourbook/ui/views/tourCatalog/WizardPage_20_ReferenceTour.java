/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.common.form.SashLeftFixedForm;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.IReferenceTourProvider;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.part.PageBook;

public class WizardPage_20_ReferenceTour extends WizardPage {

	public static final int					COLUMN_REF_TOUR			= 0;

	// dialog settings
	private static final String				REF_TOUR_CHECKED		= "RefTour.checkedTours";			//$NON-NLS-1$
	private static final String				REF_TOUR_VIEWER_WIDTH	= "RefTour.viewerWidth";			//$NON-NLS-1$

	final IPreferenceStore					_prefStore				= TourbookPlugin.getPrefStore();

	private ArrayList<RefTourItem>			_refTours				= new ArrayList<RefTourItem>();
	private final IReferenceTourProvider	_refTourProvider;

	/*
	 * UI controls
	 */
	private PageBook						_pageBook;
	private SashLeftFixedForm				_viewerDetailForm;
	private Composite						_refContainer;
	private Chart							_refTourChart;
	private CheckboxTableViewer				_refTourViewer;
	private Group							_groupChart;
	private Label							_pageTourIsNotSelected;

	private final class RefTourComparator extends ViewerComparator {
		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			final RefTourItem refTourItem1 = (RefTourItem) e1;
			final RefTourItem refTourItem2 = (RefTourItem) e2;

			return refTourItem1.label.compareTo(refTourItem2.label);
		}
	}

	private class RefTourContentProvider implements IStructuredContentProvider {

		public RefTourContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {
			return _refTours.toArray();
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	protected WizardPage_20_ReferenceTour(final IReferenceTourProvider refTourProvider) {

		super("reference-tour");//$NON-NLS-1$

		_refTourProvider = refTourProvider;

		setTitle(Messages.tourCatalog_wizard_Page_reference_tour_title);
	}

	@Override
	public void createControl(final Composite parent) {

		initializeDialogUnits(parent);

		final Composite pageContainer = createUI(parent);

		// control must be set, otherwise nothing is displayed
		setControl(pageContainer);

		loadRefTours();
		_refTourViewer.setInput(this);

		restoreState();

		validatePage();

		/*
		 * After 1 hour to try different solutions, I found a solution to remove the vertical
		 * scrollbar from the ref tour table, only an async layout removes it.
		 */
		parent.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				final Shell shell = parent.getShell();

				if (shell.isDisposed() == false) {
					shell.layout(true, true);
				}
			}
		});
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
//			final Label label = new Label(container, SWT.NONE);
//			label.setText(Messages.tourCatalog_wizard_Label_reference_tour);

			final Composite masterDetailContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(masterDetailContainer);

			/*
			 * Reference tours
			 */
			{
				_refContainer = new Composite(masterDetailContainer, SWT.NONE);
				GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_refContainer);
				{
					createUI_10_RefTourTableViewer(_refContainer);
					createUI_20_Actions(_refContainer);
				}
			}

			/*
			 * Sash
			 */
			final Sash sash = new Sash(masterDetailContainer, SWT.VERTICAL);

			/*
			 * Chart
			 */
			{
				_groupChart = new Group(masterDetailContainer, SWT.NONE);
				_groupChart.setLayout(new GridLayout());
				_groupChart.setEnabled(false);
				{
					/*
					 * create pagebook with the chart and the no-chart page
					 */
					_pageBook = new PageBook(_groupChart, SWT.NONE);
					_pageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
					{
						_refTourChart = new Chart(_pageBook, SWT.NONE);
						_refTourChart.setBackgroundColor(parent
								.getDisplay()
								.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

						_pageTourIsNotSelected = new Label(_pageBook, SWT.NONE);
						_pageTourIsNotSelected.setText(Messages.tourCatalog_wizard_Label_a_tour_is_not_selected);
						_pageTourIsNotSelected.setEnabled(false);
					}
				}
			}

			_viewerDetailForm = new SashLeftFixedForm(masterDetailContainer, _refContainer, sash, _groupChart);
		}

		return container;
	}

	private void createUI_10_RefTourTableViewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(layoutContainer);

		final Table table = new Table(layoutContainer, 0
//				SWT.H_SCROLL //
//				| SWT.V_SCROLL //
				| SWT.MULTI
				| SWT.BORDER
				| SWT.FULL_SELECTION
				| SWT.CHECK);

		table.setLinesVisible(false);

		_refTourViewer = new CheckboxTableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;

		// column: reference tour
		tvc = new TableViewerColumn(_refTourViewer, SWT.LEAD);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final RefTourItem refTour = (RefTourItem) cell.getElement();

				cell.setText(refTour.label);
			}
		});

		tableLayout.setColumnData(tvc.getColumn(), new ColumnWeightData(100));

		/*
		 * Setup viewer
		 */
		_refTourViewer.setContentProvider(new RefTourContentProvider());
		_refTourViewer.setComparator(new RefTourComparator());

		_refTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				showReferenceTour(event);
			}
		});

		_refTourViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(final CheckStateChangedEvent event) {
				onCheckRefTour(event);
			}
		});
	}

	private void createUI_20_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults()//
//				.extendedMargins(0, 5, 0, 0)
				.numColumns(2)
				.applyTo(container);
		{
			/*
			 * Action: Select all
			 */
			{
				final Button btn = new Button(container, SWT.NONE);
				btn.setText(Messages.tourCatalog_wizard_Action_select_all);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						_refTourViewer.setAllChecked(true);
						validatePage();
					}
				});
				setButtonLayoutData(btn);
			}

			/*
			 * Action: Deselect all
			 */
			{
				final Button btn = new Button(container, SWT.NONE);
				btn.setText(Messages.tourCatalog_wizard_Action_deselect_all);
				btn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						_refTourViewer.setAllChecked(false);
						validatePage();
					}
				});
				setButtonLayoutData(btn);
			}
		}
	}

	public RefTourItem[] getReferenceTours() {

		// convert the Object[] into a TourReference[]
		final Object[] checked = _refTourViewer.getCheckedElements();
		final RefTourItem[] refTours = new RefTourItem[checked.length];
		System.arraycopy(checked, 0, refTours, 0, checked.length);

		return refTours;
	}

	private void loadRefTours() {

		_refTours.clear();

		final String sql = "" //$NON-NLS-1$
				//
				+ "SELECT" //$NON-NLS-1$
				//
				+ " refId," //$NON-NLS-1$
				+ " TourData_tourId," //$NON-NLS-1$
				//
				+ " label," //$NON-NLS-1$
				+ " startIndex," //$NON-NLS-1$
				+ " endIndex" //$NON-NLS-1$
				//
				+ " FROM " + TourDatabase.TABLE_TOUR_REFERENCE //$NON-NLS-1$
				+ " ORDER BY label"; //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sql);
			final ResultSet result = statement.executeQuery();

			while (result.next()) {

				final RefTourItem refItem = new RefTourItem();

				refItem.refId = result.getLong(1);
				refItem.tourId = result.getLong(2);

				refItem.label = result.getString(3);
				refItem.startIndex = result.getInt(4);
				refItem.endIndex = result.getInt(5);

				_refTours.add(refItem);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	private void onCheckRefTour(final CheckStateChangedEvent event) {

		/*
		 * Select checked ref tour
		 */
		if (event.getChecked()) {

			final Object checkedElement = event.getElement();
			_refTourViewer.setSelection(new StructuredSelection(checkedElement));
		}

		validatePage();
	}

	private void restoreState() {

		final IDialogSettings state = getDialogSettings();

		// restore viewer width
		Integer viewerWidth = null;
		try {
			viewerWidth = state.getInt(REF_TOUR_VIEWER_WIDTH);
		} catch (final NumberFormatException e) {
			viewerWidth = 200;
		}
		_viewerDetailForm.setViewerWidth(viewerWidth);

		RefTourItem firstCheckedRefTour = null;

		if (_refTourProvider == null) {

			// restore checked reference tours
			final long[] persistedTourIds = Util.getStateLongArray(state, REF_TOUR_CHECKED, null);

			if (persistedTourIds != null) {

				for (final RefTourItem refTourItem : _refTours) {

					final long refId = refTourItem.refId;

					for (final long stateRefId : persistedTourIds) {

						if (stateRefId == refId) {

							_refTourViewer.setChecked(refTourItem, true);

							if (firstCheckedRefTour == null) {
								firstCheckedRefTour = refTourItem;
							}

							break;
						}
					}
				}
			}
		} else {

			// check reference tours from the reference tour provider

			final ArrayList<Long> selectedRefTours = _refTourProvider.getSelectedReferenceTours();
			if (selectedRefTours != null) {

				// loop: all selected reference tours
				for (final Long selectedRefTourId : selectedRefTours) {

					// loop: all available reference tours
					for (final Object refTour : _refTours) {

						final RefTourItem tourReference = (RefTourItem) refTour;

						if (selectedRefTourId.equals(tourReference.refId)) {

							_refTourViewer.setChecked(tourReference, true);

							if (firstCheckedRefTour == null) {
								firstCheckedRefTour = tourReference;
							}

							break;
						}
					}
				}

			}
		}

		if (firstCheckedRefTour != null) {

			// select first checked ref tour
			_refTourViewer.setSelection(new StructuredSelection(firstCheckedRefTour), true);

		} else {

			// select first ref tour
			if (_refTours.size() > 0) {
				_refTourViewer.setSelection(new StructuredSelection(_refTours.get(0)), true);
			}
		}
	}

	void saveState() {

		final IDialogSettings state = getDialogSettings();

		// save the viewer width
		state.put(REF_TOUR_VIEWER_WIDTH, _refContainer.getSize().x);

		// save the checked tours
		final Object[] checkedElements = _refTourViewer.getCheckedElements();
		final long[] refTourIds = new long[checkedElements.length];

		for (int tourIndex = 0; tourIndex < checkedElements.length; tourIndex++) {
			refTourIds[tourIndex] = ((RefTourItem) (checkedElements[tourIndex])).refId;
		}

		Util.setState(state, REF_TOUR_CHECKED, refTourIds);
	}

	@Override
	public void setVisible(final boolean visible) {

		super.setVisible(visible);

		_refTourViewer.getTable().setFocus();
	}

	private void showReferenceTour(final SelectionChangedEvent event) {

		final IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		final RefTourItem refTour = (RefTourItem) selection.getFirstElement();

		if (refTour != null) {

			// get tour data from the database
			final TourData tourData = TourManager.getTour(refTour.tourId);

			// set the altitude visible
			final TourChartConfiguration chartConfig = new TourChartConfiguration(false);
			chartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

			final ChartDataModel chartDataModel = TourManager.getInstance().createChartDataModel(tourData, chartConfig);

			final ChartDataXSerie xData = chartDataModel.getXData();

			xData.setSynchMarkerValueIndex(refTour.startIndex, refTour.endIndex);

			_groupChart.setText(NLS.bind(refTour.label + ": " //$NON-NLS-1$
					+ Messages.tourCatalog_wizard_Group_chart_title, TourManager.getTourDateShort(tourData)));

			UI.updateChartProperties(_refTourChart);

			// show title
			_refTourChart.getChartTitleSegmentConfig().isShowSegmentTitle = true;

			_refTourChart.updateChart(chartDataModel, false);

			_pageBook.showPage(_refTourChart);

		} else {

			// hide the chart
			_pageBook.showPage(_pageTourIsNotSelected);
			_groupChart.setText(UI.EMPTY_STRING);
		}
	}

	private boolean validatePage() {

		final Object[] checkedElements = _refTourViewer.getCheckedElements();

		if (checkedElements.length == 0) {

			setPageComplete(false);
			setErrorMessage(Messages.tourCatalog_wizard_Error_select_reference_tours);
			return false;

		} else {

			setPageComplete(true);
			setErrorMessage(null);
			setMessage(Messages.tourCatalog_wizard_Msg_select_reference_tour);
			return true;
		}
	}

}
