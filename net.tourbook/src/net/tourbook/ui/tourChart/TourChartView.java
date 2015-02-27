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
import net.tourbook.data.TourMarker;
import net.tourbook.photo.IPhotoEventListener;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.TourPhotoLink;
import net.tourbook.photo.TourPhotoLinkSelection;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourModifyListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.UI;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 09.07.2007

/**
 * Shows the selected tour in a chart
 */
public class TourChartView extends ViewPart implements ITourChartViewer, IPhotoEventListener, ITourModifyListener {

	public static final String		ID			= "net.tourbook.views.TourChartView";	//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private TourChartConfiguration	_tourChartConfig;
	private TourData				_tourData;
	private TourPhotoLink			_tourPhotoLink;

	/**
	 * Chart update is forced, when previous selection was a photo link or current selection is a
	 * photo link.
	 */
	private boolean					_isForceUpdate;

//	private boolean					_isPartActive;

	private PostSelectionProvider	_postSelectionProvider;
	private ISelectionListener		_postSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;
	private IPartListener2			_partListener;

	private boolean					_isInSliderPositionFired;
	private boolean					_isInSelectionChanged;

	/*
	 * UI controls
	 */
	private PageBook				_pageBook;
	private Label					_pageNoChart;

	private TourChart				_tourChart;

	private void addPartListener() {
		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == TourChartView.this) {
//					_isPartActive = true;
				}
			}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == TourChartView.this) {
//					_isPartActive = false;
				}

				// ensure that at EACH part deactivation the photo tooltip gets hidden
				_tourChart.partIsDeactivated();
			}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourChartView.this) {
					_tourChart.partIsHidden();
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
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
			@Override
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
			@Override
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

			@Override
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

				} else if ((eventId == TourEventId.TOUR_SELECTION //
						|| eventId == TourEventId.SLIDER_POSITION_CHANGED)

						&& eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);

				} else if (eventId == TourEventId.MARKER_SELECTION && eventData instanceof SelectionTourMarker) {

					onSelectionChanged_TourMarker((SelectionTourMarker) eventData);

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();

				} else if (eventId == TourEventId.UPDATE_UI) {

					// check if this tour chart contains a tour which must be updated

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
		PhotoManager.addPhotoEventListener(this);

		// set this view part as selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		showTour();
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		_tourChart = new TourChart(_pageBook, SWT.FLAT, this);
		_tourChart.setShowZoomActions(true);
		_tourChart.setShowSlider(true);
		_tourChart.setTourInfoActionsEnabled(true);
		_tourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);
		_tourChart.setContextProvider(new TourChartContextProvider(this), true);

		// allow the marker tooltip in the tour chart to open the marker dialog
		_tourChart.setIsShowMarkerActions(true);

		_tourChartConfig = TourManager.createDefaultTourChartConfig();

		// set chart title
		_tourChart.addDataModelListener(new IDataModelListener() {
			@Override
			public void dataModelChanged(final ChartDataModel chartDataModel) {
				chartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));
			}
		});

		_tourChart.addTourModifyListener(this);

		// fire a slider move selection when a slider was moved in the tour chart
		_tourChart.addSliderMoveListener(new ISliderMoveListener() {
			@Override
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
				fireSliderPosition();
			}
		});
	}

	@Override
	public void dispose() {

		saveState();

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);
		PhotoManager.removePhotoEventListener(this);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	/**
	 * Fire slider move event when the chart is drawn the first time or when the focus gets the
	 * chart, this will move the sliders in the map to the correct position.
	 */
	private void fireSliderPosition() {

		// don't fire an slider event when in selection change event
		if (_isInSelectionChanged) {
			return;
		}

		final SelectionChartInfo chartInfo = _tourChart.getChartInfo();
		if (chartInfo != null) {

			_isInSliderPositionFired = true;
			{
				TourManager.fireEventWithCustomData(//
						TourEventId.SLIDER_POSITION_CHANGED,
						chartInfo,
						TourChartView.this);
			}
			_isInSliderPositionFired = false;
		}
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		if (_tourData == null) {
			return null;
		}

		final ArrayList<TourData> tourList = new ArrayList<TourData>();
		tourList.add(_tourData);

		return tourList;
	}

	@Override
	public TourChart getTourChart() {
		return _tourChart;
	}

	private void onSelectionChanged(final ISelection selection) {

//		System.out.println((net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tonSelectionChanged"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		// prevent to listen to own events
		if (_isInSliderPositionFired) {
			return;
		}

		_isInSelectionChanged = true;
		{
			_isForceUpdate = _tourPhotoLink != null;

			_tourPhotoLink = null;

			if (selection instanceof SelectionTourData) {

				final SelectionTourData tourDataSelection = (SelectionTourData) selection;

				final TourData selectionTourData = tourDataSelection.getTourData();
				if (selectionTourData != null) {

					// prevent loading the same tour
					if (_tourData != null && _tourData.equals(selectionTourData)) {

						// do nothing

					} else {

						updateChart(selectionTourData);

						if (tourDataSelection.isSliderValueIndexAvailable()) {

							// set slider positions

							_tourChart.setXSliderPosition(new SelectionChartXSliderPosition(//
									_tourChart,
									tourDataSelection.getLeftSliderValueIndex(),
									tourDataSelection.getRightSliderValueIndex()));
						}
					}
				}

			} else if (selection instanceof SelectionTourId) {

				final SelectionTourId selectionTourId = (SelectionTourId) selection;
				final Long tourId = selectionTourId.getTourId();

				updateChart(tourId);

			} else if (selection instanceof SelectionTourIds) {

				// only 1 tour can be displayed in the tour chart

				final SelectionTourIds selectionTourId = (SelectionTourIds) selection;
				final ArrayList<Long> tourIds = selectionTourId.getTourIds();

				boolean isChartPainted = false;
				if (selection instanceof TourPhotoLinkSelection) {

					final ArrayList<TourPhotoLink> tourPhotoLinks = ((TourPhotoLinkSelection) selection).tourPhotoLinks;

					if (tourPhotoLinks.size() > 0) {

						_tourPhotoLink = tourPhotoLinks.get(0);

						if (_tourPhotoLink.isHistoryTour()) {

							// paint history tour

							updateChart(_tourPhotoLink.getHistoryTourData());

							isChartPainted = true;
						}
					}
				}

				if (isChartPainted == false && tourIds != null && tourIds.size() > 0) {

					// paint regular tour

					// force update when photo link selection occured
					_isForceUpdate = _tourPhotoLink != null;

					updateChart(tourIds.get(0));
				}

			} else if (selection instanceof SelectionChartInfo) {

				final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;
				final ChartDataModel chartDataModel = chartInfo.chartDataModel;

				if (chartDataModel != null) {

					final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
					if (tourId instanceof Long) {

						final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
						if (tourData != null) {

							if (_tourData == null || _tourData.equals(tourData) == false) {
								updateChart(tourData);
							}

							// set slider position
							final SelectionChartXSliderPosition xSliderPosition = new SelectionChartXSliderPosition(
									_tourChart,
									chartInfo.leftSliderValuesIndex,
									chartInfo.rightSliderValuesIndex);

							_tourChart.selectMarker(xSliderPosition);
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

							if (_tourData != null && _tourData.equals(tourData)) {

								// it's the same tour, overwrite chart

								xSliderPosition.setChart(_tourChart);
							}
						}
					}
				}

				_tourChart.selectMarker(xSliderPosition);

			} else if (selection instanceof StructuredSelection) {

				final Object firstElement = ((StructuredSelection) selection).getFirstElement();
				if (firstElement instanceof TVICatalogComparedTour) {

					updateChart(((TVICatalogComparedTour) firstElement).getTourId());

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
					updateChart(refItem.getTourId());
				}

			} else if (selection instanceof SelectionDeletedTours) {

				clearView();
			}
		}
		_isInSelectionChanged = false;
	}

	private void onSelectionChanged_TourMarker(final SelectionTourMarker markerSelection) {

		_isInSelectionChanged = true;
		{
			final TourData tourData = markerSelection.getTourData();
			final long markerTourId = tourData.getTourId().longValue();

			/*
			 * check if the marker tour is displayed
			 */
			if (_tourData == null || _tourData.getTourId().longValue() != markerTourId) {

				// show tour

				updateChart(tourData);
			}

			/*
			 * set slider position
			 */
			final ArrayList<TourMarker> tourMarker = markerSelection.getSelectedTourMarker();
			final int numberOfTourMarkers = tourMarker.size();

			int leftSliderValueIndex = tourMarker.get(0).getSerieIndex();
			int rightSliderValueIndex = 0;

			if (numberOfTourMarkers == 1) {

				rightSliderValueIndex = leftSliderValueIndex;

			} else if (numberOfTourMarkers > 1) {

				leftSliderValueIndex = tourMarker.get(0).getSerieIndex();
				rightSliderValueIndex = tourMarker.get(numberOfTourMarkers - 1).getSerieIndex();
			}

			final SelectionChartXSliderPosition xSliderPosition = new SelectionChartXSliderPosition(
					_tourChart,
					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION,
					leftSliderValueIndex,
					rightSliderValueIndex,
					true);

			_tourChart.selectMarker(xSliderPosition);
		}
		_isInSelectionChanged = false;
	}

	@Override
	public void photoEvent(final IViewPart viewPart, final PhotoEventId photoEventId, final Object data) {

		if (photoEventId == PhotoEventId.PHOTO_SELECTION && data instanceof TourPhotoLinkSelection) {

			final TourPhotoLinkSelection linkSelection = (TourPhotoLinkSelection) data;

			onSelectionChanged(linkSelection);
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
		if (_tourData == null) {

			_postSelectionProvider.clearSelection();

		} else {

			_postSelectionProvider.setSelection(new SelectionTourData(_tourChart, _tourData));

			fireSliderPosition();
		}
	}

	private void showTour() {

		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {

			_pageBook.showPage(_pageNoChart);

			// a tour is not displayed, find a tour provider which provides a tour
			Display.getCurrent().asyncExec(new Runnable() {
				@Override
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

	@Override
	public void tourIsModified(final TourData tourData) {

		final TourData savedTourData = TourManager.saveModifiedTour(tourData);

		updateChart(savedTourData);
	}

	private void updateChart(final long tourId) {

		if (_tourData != null && _tourData.getTourId() == tourId && _isForceUpdate == false) {
			// optimize
			return;
		}

		final TourData tourData = TourManager.getInstance().getTourData(tourId);

		updateChart(tourData);

		fireSliderPosition();
	}

	private void updateChart(final TourData tourData) {

		if (tourData == null) {
			// nothing to do
			return;
		}

		_tourData = tourData;

		TourManager.getInstance().setActiveTourChart(_tourChart);

		_pageBook.showPage(_tourChart);

		// set or reset photo link
		_tourData.tourPhotoLink = _tourPhotoLink;

		_tourChart.updateTourChart(_tourData, _tourChartConfig, false);

		// set application window title tool tip
		setTitleToolTip(TourManager.getTourDateShort(_tourData));
	}

}
