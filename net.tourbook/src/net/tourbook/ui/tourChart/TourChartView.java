/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.data.TourData;
import net.tourbook.photo.MergeTour;
import net.tourbook.photo.TourPhotoSelection;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 09.07.2007

/**
 * Shows the selected tour in a chart
 */
public class TourChartView extends ViewPart implements ITourChartViewer {

	public static final String		ID			= "net.tourbook.views.TourChartView";	//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault()//
														.getPreferenceStore();

	private TourChartConfiguration	_tourChartConfig;
	private TourData				_tourData;
	private MergeTour				_mergedTour;

	private PostSelectionProvider	_postSelectionProvider;
	private ISelectionListener		_postSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;
	private IPartListener2			_partListener;

	/*
	 * UI controls
	 */
	private PageBook				_pageBook;
	private Label					_pageNoChart;

	private TourChart				_tourChart;

	private void addPartListener() {
		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {}

			public void partDeactivated(final IWorkbenchPartReference partRef) {

				// ensure that at each part deactivation the photo tooltip gets hidden
				_tourChart.partIsDeactivated();
			}

			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourChartView.this) {
					_tourChart.partIsHidden();
				}
			}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourChartView.this) {
					_tourChart.partIsVisible();
				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * create a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.GRAPH_VISIBLE)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME)
				//
				//
				) {
					_tourChartConfig = TourManager.createDefaultTourChartConfig();

					if (_tourChart != null) {
						_tourChart.updateTourChart(_tourData, _tourChartConfig, false);
					}

				} else if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

					/*
					 * HR zone colors can be modified and person hash code has changed by saving the
					 * person entity -> tour chart must be recreated
					 */

					clearView();
					showTour();

				} else if (property.equals(ITourbookPreferences.GRAPH_MOUSE_MODE)) {

					_tourChart.setMouseMode(event.getNewValue());
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == TourChartView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {

			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TourChartView.this) {
					return;
				}

				if (eventId == TourEventId.SEGMENT_LAYER_CHANGED) {

					_tourChart.updateLayerSegment((Boolean) eventData);

				} else if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

					_tourChart.updateTourChart(true, true);

				} else if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					if (_tourData == null || part == TourChartView.this) {
						return;
					}

					// get modified tours
					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						final long chartTourId = _tourData.getTourId();

						// update chart with the modified tour
						for (final TourData tourData : modifiedTours) {

							if (tourData == null) {

								/*
								 * tour is not set, this can be the case when a manual tour is
								 * discarded
								 */

								clearView();

								return;
							}

							if (tourData.getTourId() == chartTourId) {

								updateChart(tourData);

								// removed old tour data from the selection provider
								_postSelectionProvider.clearSelection();

								return;
							}
						}
					}

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();

				} else if (eventId == TourEventId.UPDATE_UI) {

					// check if this tour data editor contains a tour which must be updated

					if (_tourData == null) {
						return;
					}

					final Long tourId = _tourData.getTourId();

					// update editor
					if (UI.containsTourId(eventData, tourId) != null) {

						// reload tour data and update chart
						updateChart(TourManager.getInstance().getTourData(tourId));
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		_tourData = null;
		_tourChart.updateChart(null, false);

		_pageBook.showPage(_pageNoChart);

		// removed old tour data from the selection provider
		_postSelectionProvider.clearSelection();
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		restoreState();

		addSelectionListener();
		addPrefListener();
		addTourEventListener();
		addPartListener();

		// set this view part as selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		showTour();
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		_tourChart = new TourChart(_pageBook, SWT.FLAT, true);
		_tourChart.setShowZoomActions(true);
		_tourChart.setShowSlider(true);
		_tourChart.setTourInfoActionsEnabled(true);
		_tourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);
		_tourChart.setContextProvider(new TourChartContextProvicer(this));

		_tourChart.addDoubleClickListener(new Listener() {
			public void handleEvent(final Event event) {
				if (_tourData.getTourPerson() != null) {
					ActionEditQuick.doAction(TourChartView.this);
				}
			}
		});

		_tourChartConfig = TourManager.createDefaultTourChartConfig();

		// set chart title
		_tourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel chartDataModel) {
				chartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));
			}
		});

		// fire a slider move selection when a slider was moved in the tour chart
		_tourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
				_postSelectionProvider.setSelection(chartInfoSelection);
			}
		});
	}

	@Override
	public void dispose() {

		saveState();

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	/**
	 * fire slider move event when the chart is drawn the first time or when the focus gets the
	 * chart, this will move the sliders in the map to the correct position
	 */
	private void fireSliderPosition() {
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				TourManager.fireEventWithCustomData(
						TourEventId.SLIDER_POSITION_CHANGED,
						_tourChart.getChartInfo(),
						TourChartView.this);
			}
		});
	}

	public ArrayList<TourData> getSelectedTours() {

		if (_tourData == null) {
			return null;
		}

		final ArrayList<TourData> tourList = new ArrayList<TourData>();
		tourList.add(_tourData);

		return tourList;
	}

	public TourChart getTourChart() {
		return _tourChart;
	}

	private void onSelectionChanged(final ISelection selection) {

		_mergedTour = null;

		if (selection instanceof SelectionTourData) {

			final TourData selectionTourData = ((SelectionTourData) selection).getTourData();
			if (selectionTourData != null) {

				// prevent loading the same tour
				if (_tourData != null && _tourData.equals(selectionTourData)) {
					return;
				}

				updateChart(selectionTourData);
			}

		} else if (selection instanceof SelectionTourIds) {

			final SelectionTourIds selectionTourId = (SelectionTourIds) selection;
			final ArrayList<Long> tourIds = selectionTourId.getTourIds();
			if (tourIds != null && tourIds.size() > 0) {
				updateChart(tourIds.get(0), false);
			}

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId selectionTourId = (SelectionTourId) selection;
			final Long tourId = selectionTourId.getTourId();

			if (selection instanceof TourPhotoSelection) {

				_mergedTour = ((TourPhotoSelection) selection).mergedTour;

				if (_mergedTour.isHistoryTour()) {

					updateChart(_mergedTour.getHistoryTourData());

// each view selection fires the zoom out -> this is very horrorable
//					_tourChart.zoomOut();
				}
			}

			final boolean isForceUpdate = _mergedTour != null;

			// force update when photo selection occured
			updateChart(tourId, isForceUpdate);

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
					if (tourData != null) {

						if (_tourData == null || _tourData.equals(tourData) == false) {
							updateChart(tourData);
						}

						final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

						// set slider position
						_tourChart.setXSliderPosition(new SelectionChartXSliderPosition(
								_tourChart,
								chartInfo.leftSliderValuesIndex,
								chartInfo.rightSliderValuesIndex));
					}
				}
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPosition = (SelectionChartXSliderPosition) selection;

			final Chart chart = xSliderPosition.getChart();
			if (chart != null && chart != _tourChart) {

				// it's not the same chart, check if it's the same tour

				final Object tourId = chart.getChartDataModel().getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
					if (tourData != null) {

						if (_tourData.equals(tourData)) {

							// it's the same tour, overwrite chart

							xSliderPosition.setChart(_tourChart);
						}
					}
				}
			}

			_tourChart.setXSliderPosition(xSliderPosition);

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {

				updateChart(((TVICatalogComparedTour) firstElement).getTourId(), false);

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
				final TourData tourData = TourManager.getInstance().getTourData(
						compareResultItem.getComparedTourData().getTourId());
				updateChart(tourData);
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				updateChart(refItem.getTourId(), false);
			}

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}
	}

	private void restoreState() {

		_tourChart.restoreState();
	}

	private void saveState() {

		_tourChart.saveState();
	}

	@Override
	public void setFocus() {

		_tourChart.setFocus();

		/*
		 * fire tour selection
		 */
		_postSelectionProvider.setSelection(new SelectionTourData(_tourChart, _tourData));

		fireSliderPosition();
	}

	private void showTour() {

		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {

			_pageBook.showPage(_pageNoChart);
			
			// a tour is not displayed, find a tour provider which provides a tour
			Display.getCurrent().asyncExec(new Runnable() {
				public void run() {
			
					// validate widget
					if (_pageBook.isDisposed()) {
						return;
					}
			
					/*
					 * check if tour was set from a selection provider
					 */
					if (_tourData != null) {
						return;
					}
			
					final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
					if (selectedTours != null && selectedTours.size() > 0) {
						updateChart(selectedTours.get(0));
					}
				}
			});
		}
	}

	private void updateChart() {

		if (_tourData == null) {
			// nothing to do
			return;
		}

		TourManager.getInstance().setActiveTourChart(_tourChart);

		_pageBook.showPage(_tourChart);

		_tourData.mergedTour = _mergedTour;

		_tourChart.updateTourChart(_tourData, _tourChartConfig, false);

		// set application window title tool tip
		setTitleToolTip(TourManager.getTourDateShort(_tourData));
	}

	private void updateChart(final long tourId, final boolean isForceUpdate) {

		if (_tourData != null && isForceUpdate == false && _tourData.getTourId() == tourId) {
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

		_tourData = tourData;

		updateChart();
	}

}
