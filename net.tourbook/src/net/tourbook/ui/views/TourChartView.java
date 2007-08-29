/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import net.tourbook.Messages;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ActionEditTour;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 09.07.2007

public class TourChartView extends ViewPart {

	public static final String		ID	= "net.tourbook.views.TourChartView";	//$NON-NLS-1$

	private TourChart				fTourChart;
	private TourChartConfiguration	fTourChartConfig;
	private TourData				fTourData;

	private ISelectionListener		fPostSelectionListener;
	private IPropertyChangeListener	fPrefChangeListener;

	private IPartListener2			fPartListener;
	private PostSelectionProvider	fPostSelectionProvider;
	private IPropertyListener		fTourDbListener;
	private ITourPropertyListener	fTourPropertyListener;

	private PageBook				fPageBook;

	private Label					fPageNoChart;

	private class TourChartContextProvicer implements IChartContextProvider {

		public void fillBarChartContextMenu(IMenuManager menuMgr,
											int hoveredBarSerieIndex,
											int hoveredBarValueIndex) {}

		public void fillXSliderContextMenu(	IMenuManager menuMgr,
											ChartXSlider leftSlider,
											ChartXSlider rightSlider) {}

		public void fillContextMenu(IMenuManager menuMgr) {

			final ActionEditTour actionEditTour = new ActionEditTour(TourChartView.this);
			actionEditTour.setEnabled(fTourData != null);

			menuMgr.add(actionEditTour);
		}
	}

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {

			public void partActivated(IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourChartView.this) {
//					fTourChart.activateActionHandlers(getSite());
				}
			}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {}

			public void partClosed(IWorkbenchPartReference partRef) {}

			public void partDeactivated(IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourChartView.this) {
//					fTourChart.deactivateActionHandlers();
				}
			}

			public void partHidden(IWorkbenchPartReference partRef) {}

			public void partInputChanged(IWorkbenchPartReference partRef) {}

			public void partOpened(IWorkbenchPartReference partRef) {}

			public void partVisible(IWorkbenchPartReference partRef) {}
		};

		// register the part listener
		getSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.GRAPH_VISIBLE)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME)) {

					fTourChartConfig = TourManager.createTourChartConfiguration();

					if (fTourChart != null) {
						fTourChart.updateTourChart(fTourData, fTourChartConfig, false);
					}
				}
			}
		};
		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourPropertyListener() {
		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(int propertyId, Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_SEGMENT_LAYER_CHANGE) {
					fTourChart.updateSegmentLayer((Boolean) propertyData);

				} else if (propertyId == TourManager.TOUR_PROPERTY_CHART_IS_MODIFIED) {
					fTourChart.updateTourChart(true, true);
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void addTourDbListener() {

		fTourDbListener = new IPropertyListener() {

			public void propertyChanged(Object source, int propId) {
				if (propId == TourDatabase.TOUR_IS_CHANGED_AND_PERSISTED) {

					if (fTourData == null) {
						return;
					}

					// reload data from the database
					fTourData = TourDatabase.getTourData(fTourData.getTourId());

					updateChart();

				} else if (propId == TourDatabase.TOUR_IS_CHANGED) {

					updateChart();
				}
			}
		};

		TourDatabase.getInstance().addPropertyListener(fTourDbListener);
	}

	@Override
	public void createPartControl(Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		fTourChart = new TourChart(fPageBook, SWT.FLAT, true);
		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);
		fTourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);
		fTourChart.setContextProvider(new TourChartContextProvicer());

		fTourChart.addDoubleClickListener(new Listener() {
			public void handleEvent(Event event) {
				TourManager.getInstance().openTourInEditor(fTourData.getTourId());
			}
		});

		fTourChartConfig = TourManager.createTourChartConfiguration();
//		fTourChart.createTourActionHandlers(fTourChartConfig);

		// set chart title
		fTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(ChartDataModel chartDataModel) {
				chartDataModel.setTitle(TourManager.getTourTitleDetailed(fTourData));
			}
		});

		// fire a slider move selection when a slider was moved in the tour chart
		fTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		addSelectionListener();
		addPrefListener();
		addPartListener();

		addTourDbListener();
		addTourPropertyListener();

		// set this view part as selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		// show current selected chart if there are any
		ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null) {
			onSelectionChanged(selection);
		} else {
			fPageBook.showPage(fPageNoChart);
		}
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getSite().getPage();

		page.removePartListener(fPartListener);
		page.removePostSelectionListener(fPostSelectionListener);

		TourDatabase.getInstance().removePropertyListener(fTourDbListener);
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		getSite().setSelectionProvider(null);

		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	public TourChart getTourChart() {
		return fTourChart;
	}

	private void onSelectionChanged(ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final TourData selectionTourData = ((SelectionTourData) selection).getTourData();

			// check if the tour chart already shows the new tour data
			if (selectionTourData == fTourData) {
				return;
			}

			fTourData = selectionTourData;
			updateChart();

		} else if (selection instanceof SelectionTourId) {

			SelectionTourId tourIdSelection = (SelectionTourId) selection;

			if (fTourData != null) {
				if (fTourData.getTourId().equals(tourIdSelection.getTourId())) {
					// don't reload the same tour
					return;
				}
			}

			final TourData tourData = TourManager.getInstance()
					.getTourData(tourIdSelection.getTourId());

			if (tourData != null) {
				fTourData = tourData;
				updateChart();
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			fTourChart.setXSliderPosition((SelectionChartXSliderPosition) selection);
		}

	}

	@Override
	public void setFocus() {
		fTourChart.setFocus();

		/*
		 * fire tour selection
		 */
		fPostSelectionProvider.setSelection(new SelectionTourData(fTourChart, fTourData));
	}

	private void updateChart() {

		if (fTourData == null) {
			return;
		}

		fTourChart.updateTourChart(fTourData, fTourChartConfig, false);

		fPageBook.showPage(fTourChart);

		// set application window title
		setTitleToolTip(TourManager.getTourDate(fTourData));
	}

}
