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
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 09.07.2007

/**
 * Shows the selected tour in a chart
 */
public class TourChartView extends ViewPart implements ITourChartViewer {

	public static final String		ID	= "net.tourbook.views.TourChartView";	//$NON-NLS-1$

	private TourChart				fTourChart;
	private TourChartConfiguration	fTourChartConfig;
	TourData						fTourData;

	private ISelectionListener		fPostSelectionListener;
	private IPropertyChangeListener	fPrefChangeListener;

	private PostSelectionProvider	fPostSelectionProvider;
	private ITourEventListener		fTourEventListener;

	private PageBook				fPageBook;
	private Label					fPageNoChart;

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

				} else if (property.equals(ITourbookPreferences.GRAPH_MOUSE_MODE)) {

					fTourChart.setMouseMode(event.getNewValue());
				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == TourChartView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourEventListener() {

		fTourEventListener = new ITourEventListener() {

			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TourChartView.this) {
					return;
				}

				if (eventId == TourEventId.SEGMENT_LAYER_CHANGED) {

					fTourChart.updateSegmentLayer((Boolean) eventData);

				} else if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

					fTourChart.updateTourChart(true, true);

				} else if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					if (fTourData == null || part == TourChartView.this) {
						return;
					}

					// get modified tours
					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						final long chartTourId = fTourData.getTourId();

						// update chart with the modified tour
						for (final TourData tourData : modifiedTours) {

							if (tourData == null) {

								/*
								 * tour is not set, this can be the case when a manual tour is
								 * discarded
								 */
								fPageBook.showPage(fPageNoChart);

								// removed old tour data from the selection provider
								fPostSelectionProvider.clearSelection();

								return;
							}

							if (tourData.getTourId() == chartTourId) {

								updateChart(tourData);

								// removed old tour data from the selection provider
								fPostSelectionProvider.clearSelection();

								return;
							}
						}
					}

				} else if (eventId == TourEventId.UPDATE_UI) {

					// check if this tour data editor contains a tour which must be updated

					// update editor
					if (UI.containsTourId(eventData, fTourData.getTourId()) != null) {

						// reload tour data and update chart
						updateChart(TourManager.getInstance().getTourData(fTourData.getTourId()));
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourEventListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		fTourChart = new TourChart(fPageBook, SWT.FLAT, true);
		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);
		fTourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);
		fTourChart.setContextProvider(new TourChartContextProvicer(this));

		fTourChart.addDoubleClickListener(new Listener() {
			public void handleEvent(final Event event) {
				if (fTourData.getTourPerson() != null) {
					TourManager.getInstance().openTourInEditor(fTourData.getTourId());
				}
			}
		});

		fTourChartConfig = TourManager.createTourChartConfiguration();

		// set chart title
		fTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel chartDataModel) {
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
		addTourEventListener();

		// set this view part as selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (fTourData == null) {
			showTourFromTourProvider();
		}
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getSite().getPage();

		page.removePostSelectionListener(fPostSelectionListener);

		TourManager.getInstance().removePropertyListener(fTourEventListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	/**
	 * fire slider move event when the chart is drawn the first time or when the focus gets the
	 * chart, this will move the sliders in the map to the correct position
	 */
	private void fireSliderPosition() {
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				TourManager.fireEvent(TourEventId.SLIDER_POSITION_CHANGED,
						fTourChart.getChartInfo(),
						TourChartView.this);
			}
		});
	}

	public ArrayList<TourData> getSelectedTours() {

		if (fTourData == null) {
			return null;
		}

		final ArrayList<TourData> tourList = new ArrayList<TourData>();
		tourList.add(fTourData);

		return tourList;
	}

	public TourChart getTourChart() {
		return fTourChart;
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final TourData selectionTourData = ((SelectionTourData) selection).getTourData();
			if (selectionTourData != null) {

				// prevent loading the same tour
				if (fTourData != null && fTourData.equals(selectionTourData)) {
					return;
				}

				updateChart(selectionTourData);
			}

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId selectionTourId = (SelectionTourId) selection;
			final Long tourId = selectionTourId.getTourId();

			updateChart(tourId);

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
					if (tourData != null) {

						if (fTourData == null || fTourData.equals(tourData) == false) {
							updateChart(tourData);
						}

						final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

						// set slider position
						fTourChart.setXSliderPosition(new SelectionChartXSliderPosition(fTourChart,
								chartInfo.leftSliderValuesIndex,
								chartInfo.rightSliderValuesIndex));
					}
				}
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPosition = (SelectionChartXSliderPosition) selection;

			final Chart chart = xSliderPosition.getChart();
			if (chart != fTourChart) {

				// it's not the same chart, check if it's the same tour

				final Object tourId = chart.getChartDataModel().getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
					if (tourData != null) {

						if (fTourData.equals(tourData)) {

							// it's the same tour, overwrite chart

							xSliderPosition.setChart(fTourChart);
						}
					}
				}
			}

			fTourChart.setXSliderPosition(xSliderPosition);

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {

				updateChart(((TVICatalogComparedTour) firstElement).getTourId());

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
				final TourData tourData = TourManager.getInstance().getTourData(compareResultItem.getComparedTourData()
						.getTourId());
				updateChart(tourData);
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				updateChart(refItem.getTourId());
			}

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor instanceof TourEditor) {

				final TourData editorTourData = ((TourEditor) editor).getTourChart().getTourData();

				// prevent loading the same tour when it is not modified
				if (editor.isDirty() == false //
						&& fTourData != null
						&& fTourData.equals(editorTourData)) {
					return;
				}

				updateChart(editorTourData);
			}

		} else if (selection instanceof SelectionDeletedTours) {

			fTourData = null;

			fPageBook.showPage(fPageNoChart);
		}
	}

	@Override
	public void setFocus() {
		fTourChart.setFocus();

		/*
		 * fire tour selection
		 */
		fPostSelectionProvider.setSelection(new SelectionTourData(fTourChart, fTourData));

		fireSliderPosition();
	}

	private void showTourFromTourProvider() {

		fPageBook.showPage(fPageNoChart);

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// validate widget
				if (fPageBook.isDisposed()) {
					return;
				}

				/*
				 * check if tour was set from a selection provider
				 */
				if (fTourData != null) {
					return;
				}

				final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
				if (selectedTours != null && selectedTours.size() > 0) {
					updateChart(selectedTours.get(0));
				}
			}
		});
	}

	private void updateChart() {

		if (fTourData == null) {
			// nothing to do
			return;
		}

		TourManager.getInstance().setActiveTourChart(fTourChart);

		fTourChart.updateTourChart(fTourData, fTourChartConfig, false);

		fPageBook.showPage(fTourChart);

		// set application window title
		setTitleToolTip(TourManager.getTourDateShort(fTourData));
	}

	private void updateChart(final Long tourId) {

		if (fTourData != null && fTourData.getTourId() == tourId) {
			// optimize
			return;
		}

		updateChart(TourManager.getInstance().getTourData(tourId));

		fireSliderPosition();
	}

	private void updateChart(final TourData tourData) {

		if (tourData == null) {
			// nothing to do
			return;
		}

		fTourData = tourData;

		updateChart();
	}

}
