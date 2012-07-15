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
package net.tourbook.ui.views.tourCatalog;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.common.form.ViewerDetailForm;
import net.tourbook.common.util.TableLayoutComposite;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.IReferenceTourProvider;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class WizardPageReferenceTour extends WizardPage {

	public static final int					COLUMN_REF_TOUR			= 0;

	// dialog settings
	private static final String				REF_TOUR_CHECKED		= "RefTour.checkedTours";	//$NON-NLS-1$
	private static final String				REF_TOUR_VIEWER_WIDTH	= "RefTour.viewerWidth";	//$NON-NLS-1$

	private ViewerDetailForm				_viewerDetailForm;
	private Composite						_refContainer;

	private CheckboxTableViewer				_refTourViewer;
	private Group							_chartGroup;
	private Chart							_refTourChart;

	private Object[]						_refTours;

	private final IReferenceTourProvider	_refTourProvider;

	private class RefTourContentProvider implements IStructuredContentProvider {

		public RefTourContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			return _refTours = ReferenceTourManager.getInstance().getAllReferenceTours();
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private class RefTourLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}

		public String getColumnText(final Object obj, final int index) {

			final TourReference tourRef = ((TourReference) obj);

			switch (index) {
			case COLUMN_REF_TOUR:
				return tourRef.getLabel();
			}
			return UI.EMPTY_STRING;
		}
	}

	protected WizardPageReferenceTour(final IReferenceTourProvider refTourProvider) {

		super("reference-tour");//$NON-NLS-1$

		_refTourProvider = refTourProvider;

		setTitle(Messages.tourCatalog_wizard_Page_reference_tour_title);
	}

	public void createControl(final Composite parent) {

		GridLayout gridLayout;

		final Composite pageContainer = new Composite(parent, SWT.NONE);
		pageContainer.setLayout(new GridLayout());

		final Label label = new Label(pageContainer, SWT.NONE);
		label.setText(Messages.tourCatalog_wizard_Label_reference_tour);

		// fSashContainer = new SashForm(pageContainer, SWT.HORIZONTAL);
		// fSashContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
		// true));

		final Composite masterDetailContainer = new Composite(pageContainer, SWT.NONE);
		masterDetailContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// reference tours / select buttons
		_refContainer = new Composite(masterDetailContainer, SWT.NONE);
		gridLayout = new GridLayout(2, false);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		_refContainer.setLayout(gridLayout);

		createRefTourTableViewer(_refContainer);
		createRefTourButtons(_refContainer);

		final Sash sash = new Sash(masterDetailContainer, SWT.VERTICAL);

		// chart group
		_chartGroup = new Group(masterDetailContainer, SWT.NONE);
		_chartGroup.setLayout(new GridLayout());
		_chartGroup.setEnabled(false);

		_refTourChart = new Chart(_chartGroup, SWT.NONE);
		_refTourChart.setBackgroundColor(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		_viewerDetailForm = new ViewerDetailForm(masterDetailContainer, _refContainer, sash, _chartGroup);

		restoreDialogSettings();

		// control must be set, otherwise nothing is displayed
		setControl(pageContainer);

		validatePage();
	}

	private void createRefTourButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginRight = 15;
		container.setLayout(gridLayout);

		final Button buttonSelectAll = new Button(container, SWT.NONE);
		buttonSelectAll.setText(Messages.tourCatalog_wizard_Action_select_all);
		setButtonLayoutData(buttonSelectAll);
		buttonSelectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				_refTourViewer.setAllChecked(true);
				validatePage();
			}
		});

		final Button buttonDeselectAll = new Button(container, SWT.NONE);
		buttonDeselectAll.setText(Messages.tourCatalog_wizard_Action_deselect_all);
		setButtonLayoutData(buttonDeselectAll);
		buttonDeselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				_refTourViewer.setAllChecked(false);
				validatePage();
			}
		});
	}

	private void createRefTourTableViewer(final Composite parent) {

		final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
		final GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = convertWidthInCharsToPixels(30);
		layouter.setLayoutData(gridData);

		final Table table = new Table(layouter, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.MULTI
				| SWT.BORDER
				| SWT.FULL_SELECTION
				| SWT.CHECK);
		table.setLinesVisible(false);

		TableColumn column;
		column = new TableColumn(table, SWT.NONE);
		column.setText(Messages.tourCatalog_wizard_Column_tour);
		layouter.addColumnData(new ColumnWeightData(1, false));

		_refTourViewer = new CheckboxTableViewer(table);
		_refTourViewer.setContentProvider(new RefTourContentProvider());
		_refTourViewer.setLabelProvider(new RefTourLabelProvider());

		_refTourViewer.setComparator(new ViewerComparator() {
			@SuppressWarnings("unchecked")
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {
				return getComparator().compare(((TourReference) e1).getLabel(), ((TourReference) e2).getLabel());
			}
		});

		_refTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				showReferenceTour(event);
			}
		});

		_refTourViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				validatePage();
			}
		});

		_refTourViewer.setInput(this);
	}

	public TourReference[] getReferenceTours() {

		// convert the Object[] into a TourReference[]
		final Object[] checked = _refTourViewer.getCheckedElements();
		final TourReference[] refTours = new TourReference[checked.length];
		System.arraycopy(checked, 0, refTours, 0, checked.length);

		return refTours;
	}

	void persistDialogSettings() {

		final IDialogSettings wizardSettings = getDialogSettings();

		// save the viewer width
		wizardSettings.put(REF_TOUR_VIEWER_WIDTH, _refContainer.getSize().x);

		// save the checked tours
		final Object[] checkedElements = _refTourViewer.getCheckedElements();
		final String[] refTourIds = new String[checkedElements.length];

		for (int tourIndex = 0; tourIndex < checkedElements.length; tourIndex++) {
			refTourIds[tourIndex] = Long.toString(((TourReference) (checkedElements[tourIndex])).getRefId());
		}
		wizardSettings.put(REF_TOUR_CHECKED, refTourIds);
	}

	private void restoreDialogSettings() {

		final IDialogSettings wizardSettings = getDialogSettings();

		// restore viewer width
		Integer viewerWidth = null;
		try {
			viewerWidth = wizardSettings.getInt(REF_TOUR_VIEWER_WIDTH);
		} catch (final NumberFormatException e) {
			viewerWidth = 200;
		}
		_viewerDetailForm.setViewerWidth(viewerWidth);

		if (_refTourProvider == null) {

			// restore checked reference tours
			final String[] persistedTourIds = wizardSettings.getArray(REF_TOUR_CHECKED);

			if (persistedTourIds != null) {
				for (final Object refTour : _refTours) {
					final TourReference tourReference = (TourReference) refTour;
					// final String refTourId = Long.toString(tourReference
					// .getTourData()
					// .getTourId());

					final String refId = Long.toString(tourReference.getRefId());

					for (final String persistedRefId : persistedTourIds) {
						if (persistedRefId.compareTo(refId) == 0) {
							_refTourViewer.setChecked(tourReference, true);
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

						final TourReference tourReference = (TourReference) refTour;

						if (selectedRefTourId.equals(tourReference.getRefId())) {
							_refTourViewer.setChecked(tourReference, true);
							break;
						}
					}
				}
			}
		}
	}

	private void showReferenceTour(final SelectionChangedEvent event) {

		final IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		final TourReference refTour = (TourReference) selection.getFirstElement();

		if (refTour != null) {

			// get tour data from the database
			final TourData tourData = refTour.getTourData();

			// set the altitude visible
			final TourChartConfiguration chartConfig = new TourChartConfiguration(false);
			chartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

			final ChartDataModel chartDataModel = TourManager.getInstance().createChartDataModel(tourData, chartConfig);

			final ChartDataXSerie xData = chartDataModel.getXData();

			xData.setSynchMarkerValueIndex(refTour.getStartValueIndex(), refTour.getEndValueIndex());

			_chartGroup.setText(NLS.bind(refTour.getLabel() + ": " //$NON-NLS-1$
					+ Messages.tourCatalog_wizard_Group_chart_title, TourManager.getTourDateShort(tourData)));

			// set grid size
			final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
			_refTourChart.setGrid(
					prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
					prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE),
					prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES),
					prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES));

			_refTourChart.updateChart(chartDataModel, false);

		} else {

			// hide the chart
			_refTourChart.updateChart(null, false);
			_chartGroup.setText(UI.EMPTY_STRING);
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
