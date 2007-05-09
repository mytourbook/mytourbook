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
package net.tourbook.ui.views.tourMap;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.jface.viewers.ViewerSorter;
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

	public static final int		COLUMN_REF_TOUR			= 0;

	// dialog settings
	private static final String	REF_TOUR_CHECKED		= "RefTour.checkedTours";	//$NON-NLS-1$
	private static final String	REF_TOUR_VIEWER_WIDTH	= "RefTour.viewerWidth";	//$NON-NLS-1$

	private ViewerDetailForm	fViewerDetailForm;
	private Composite			fRefContainer;

	private CheckboxTableViewer	fRefTourViewer;
	private Group				fChartGroup;
	private Chart				fRefTourChart;

	private Object[]			fRefTours;

	private class RefTourContentProvider implements IStructuredContentProvider {

		public RefTourContentProvider() {}
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}
		public void dispose() {}

		public Object[] getElements(Object parent) {
			fRefTours = ReferenceTourManager.getInstance().getReferenceTours();
			return fRefTours;
		}
	}

	private class RefTourLabelProvider extends LabelProvider implements ITableLabelProvider {

		public String getColumnText(Object obj, int index) {

			TourReference tourRef = ((TourReference) obj);

			switch (index) {
			case COLUMN_REF_TOUR:
				return tourRef.getLabel();
			}
			return ""; //$NON-NLS-1$
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	protected WizardPageReferenceTour(String pageName) {
		super(pageName);
		setTitle(Messages.TourMapWizard_Page_reference_tour_title);
	}

	public void createControl(Composite parent) {

		GridLayout gridLayout;

		Composite pageContainer = new Composite(parent, SWT.NONE);
		pageContainer.setLayout(new GridLayout());

		Label label = new Label(pageContainer, SWT.NONE);
		label.setText(Messages.TourMapWizard_Label_reference_tour);

		// fSashContainer = new SashForm(pageContainer, SWT.HORIZONTAL);
		// fSashContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
		// true));

		Composite masterDetailContainer = new Composite(pageContainer, SWT.NONE);
		masterDetailContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// reference tours / select buttons
		fRefContainer = new Composite(masterDetailContainer, SWT.NONE);
		gridLayout = new GridLayout(2, false);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		fRefContainer.setLayout(gridLayout);

		createRefTourTableViewer(fRefContainer);
		createRefTourButtons(fRefContainer);

		final Sash sash = new Sash(masterDetailContainer, SWT.VERTICAL);

		// chart group
		fChartGroup = new Group(masterDetailContainer, SWT.NONE);
		fChartGroup.setLayout(new GridLayout());
		fChartGroup.setEnabled(false);

		fRefTourChart = new Chart(fChartGroup, SWT.NONE);
		fRefTourChart.setBackgroundColor(parent.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_BACKGROUND));

		fViewerDetailForm = new ViewerDetailForm(
				masterDetailContainer,
				fRefContainer,
				sash,
				fChartGroup);

		restoreDialogSettings();

		// control must be set, otherwise nothing is displayed
		setControl(pageContainer);

		validatePage();
	}

	private void createRefTourTableViewer(Composite parent) {

		TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
		GridData gridData = new GridData(GridData.FILL_BOTH);
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
		column.setText(Messages.TourMapWizard_Column_tour);
		layouter.addColumnData(new ColumnWeightData(1, false));

		fRefTourViewer = new CheckboxTableViewer(table);
		fRefTourViewer.setContentProvider(new RefTourContentProvider());
		fRefTourViewer.setLabelProvider(new RefTourLabelProvider());

		fRefTourViewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				return collator.compare(((TourReference) e1).getLabel(), ((TourReference) e2)
						.getLabel());
			}
		});

		fRefTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				showReferenceTour(event);
			}
		});

		fRefTourViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				validatePage();
			}
		});

		fRefTourViewer.setInput(this);
	}

	private void createRefTourButtons(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginRight = 15;
		container.setLayout(gridLayout);

		Button buttonSelectAll = new Button(container, SWT.NONE);
		buttonSelectAll.setText(Messages.TourMapWizard_Action_select_all);
		setButtonLayoutData(buttonSelectAll);
		buttonSelectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRefTourViewer.setAllChecked(true);
				validatePage();
			}
		});

		Button buttonDeselectAll = new Button(container, SWT.NONE);
		buttonDeselectAll.setText(Messages.TourMapWizard_Action_deselect_all);
		setButtonLayoutData(buttonDeselectAll);
		buttonDeselectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRefTourViewer.setAllChecked(false);
				validatePage();
			}
		});
	}

	private void showReferenceTour(SelectionChangedEvent event) {

		IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		TourReference refTour = (TourReference) selection.getFirstElement();

		if (refTour != null) {

			// get tour data from the database
			TourData tourData = refTour.getTourData();

			// set the altitude visible
			TourChartConfiguration chartConfig = new TourChartConfiguration();
			chartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

			ChartDataModel chartDataModel = TourManager.getInstance().createChartDataModel(
					tourData,
					chartConfig);

			ChartDataXSerie xData = chartDataModel.getXData();

			xData.setMarkerValueIndex(refTour.getStartValueIndex(), refTour.getEndValueIndex());

			fChartGroup.setText(NLS.bind(refTour.getLabel()
					+ ": " //$NON-NLS-1$
					+ Messages.TourMapWizard_Group_chart_title, TourManager.getTourDate(tourData)));

			fRefTourChart.setChartDataModel(chartDataModel);

		} else {

			// hide the chart
			fRefTourChart.setChartDataModel(null);
			fChartGroup.setText(""); //$NON-NLS-1$
		}
	}

	private void restoreDialogSettings() {

		IDialogSettings wizardSettings = getDialogSettings();

		// restore viewer width
		Integer viewerWidth = null;
		try {
			viewerWidth = wizardSettings.getInt(REF_TOUR_VIEWER_WIDTH);
		} catch (NumberFormatException e) {
			viewerWidth = 200;
		}
		fViewerDetailForm.setViewerWidth(viewerWidth);

		// restore checked reference tours
		String[] persistedTourIds = wizardSettings.getArray(REF_TOUR_CHECKED);

		if (persistedTourIds != null) {
			for (int refTourIndex = 0; refTourIndex < fRefTours.length; refTourIndex++) {
				final TourReference tourReference = (TourReference) fRefTours[refTourIndex];
				// final String refTourId = Long.toString(tourReference
				// .getTourData()
				// .getTourId());

				final String refId = Long.toString(tourReference.getGeneratedId());

				for (String persistedRefId : persistedTourIds) {
					if (persistedRefId.compareTo(refId) == 0) {
						fRefTourViewer.setChecked(tourReference, true);
						break;
					}
				}
			}
		}
	}

	void persistDialogSettings() {

		IDialogSettings wizardSettings = getDialogSettings();

		// save the viewer width
		wizardSettings.put(REF_TOUR_VIEWER_WIDTH, fRefContainer.getSize().x);

		// save the checked tours
		Object[] checkedElements = fRefTourViewer.getCheckedElements();
		String[] refTourIds = new String[checkedElements.length];

		for (int tourIndex = 0; tourIndex < checkedElements.length; tourIndex++) {
			refTourIds[tourIndex] = Long.toString(((TourReference) (checkedElements[tourIndex]))
					.getGeneratedId()/*
										 * .getTourData() .getTourId()
										 */);
		}
		wizardSettings.put(REF_TOUR_CHECKED, refTourIds);
	}

	private boolean validatePage() {

		final Object[] checkedElements = fRefTourViewer.getCheckedElements();

		if (checkedElements.length == 0) {
			setPageComplete(false);
			setErrorMessage(Messages.TourMapWizard_Error_select_reference_tours);
			return false;
		} else {
			setPageComplete(true);
			setErrorMessage(null);
			setMessage(Messages.TourMapWizard_Msg_select_reference_tour);
			return true;
		}
	}

	public TourReference[] getReferenceTours() {

		// convert the Object[] into a TourReference[]
		Object[] checked = fRefTourViewer.getCheckedElements();
		TourReference[] refTours = new TourReference[checked.length];
		System.arraycopy(checked, 0, refTours, 0, checked.length);

		return refTours;
	}

}
