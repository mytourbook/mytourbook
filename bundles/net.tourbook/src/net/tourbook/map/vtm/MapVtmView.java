/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map.vtm;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Frame;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.map.vtm.action.ActionMapProviderOpenSciMap;
import net.tourbook.map2.view.SelectionMapPosition;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import de.byteholder.gpx.PointOfInterest;

public class MapVtmView extends ViewPart {

	public static final String				ID		= "net.tourbook.map.vtm.MapVtmView";	//$NON-NLS-1$

	private static final IDialogSettings	_state	= TourbookPlugin.getState(ID);

	private VtmMap							_vtmMap;

	protected boolean						_isPartVisible;

	private IPartListener2					_partListener;
	private ITourEventListener				_tourEventListener;

	private ISelection						_lastHiddenSelection;
	private ISelection						_selectionWhenHidden;

	private ActionMapProviderOpenSciMap		_action_MapProvider_OpenScience;
	private ActionMapProviderOpenSciMap		_action_MapProvider_Custom;

	public void action_MapProvider_OpenScience() {
		// TODO Auto-generated method stub

	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == MapVtmView.this) {
//					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == MapVtmView.this) {
					_isPartVisible = false;
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == MapVtmView.this) {

					_isPartVisible = true;

					if (_lastHiddenSelection != null) {

						onSelectionChanged(_lastHiddenSelection);

						_lastHiddenSelection = null;
					}
				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == MapVtmView.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

//					resetMap();

				} else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

//					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
//					if ((modifiedTours != null) && (modifiedTours.size() > 0)) {
//
//						_allTourData.clear();
//						_allTourData.addAll(modifiedTours);
//
//						resetMap();
//					}

				} else if (eventId == TourEventId.UPDATE_UI || eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

//					clearView();

				} else if (eventId == TourEventId.MARKER_SELECTION) {

//					if (eventData instanceof SelectionTourMarker) {
//
//						onSelectionChanged_TourMarker((SelectionTourMarker) eventData, false);
//					}

				} else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);

				} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {
		// TODO Auto-generated method stub

	}

	private void createActions() {

		_action_MapProvider_OpenScience = new ActionMapProviderOpenSciMap(this);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		createActions();
		fillActionBars();

		addPartListener();
		addTourEventListener();
	}

	private void createUI(final Composite parent) {

		final Composite swtContainer = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		final Frame awtContainer = SWT_AWT.new_Frame(swtContainer);

		final Canvas awtCanvas = new Canvas();
		awtContainer.setLayout(new BorderLayout());
		awtCanvas.setIgnoreRepaint(true);

		awtContainer.add(awtCanvas);
		awtCanvas.setFocusable(true);
		awtCanvas.requestFocus();

		_vtmMap = new VtmMap(_state);
		_vtmMap.run(awtCanvas);
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);

		_vtmMap.stop();

		super.dispose();
	}

	private void enableActions() {
		// TODO Auto-generated method stub

	}

	private void fillActionBars() {

		/*
		 * fill view toolbar
		 */
//		final IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();
//
//		viewTbm.add(_action_MapProvider_OpenScience);
//		viewTbm.add(new Separator());

		/*
		 * fill view menu
		 */
//		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

//		fillMapContextMenu(menuMgr);
	}

	private void onSelectionChanged(final ISelection selection) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tonSelectionChanged: " + selection));
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (_isPartVisible == false) {

			if (selection instanceof SelectionTourData
					|| selection instanceof SelectionTourId
					|| selection instanceof SelectionTourIds) {

				// keep only selected tours
				_selectionWhenHidden = selection;
			}
			return;
		}

		if (selection instanceof SelectionTourData) {

//			final SelectionTourData selectionTourData = (SelectionTourData) selection;
//			final TourData tourData = selectionTourData.getTourData();
//
//			paintTours_20_One(tourData, false);
//			paintPhotoSelection(selection);
//
//			enableActions();

		} else if (selection instanceof SelectionTourId) {

//			final SelectionTourId tourIdSelection = (SelectionTourId) selection;
//			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());
//
//			paintTours_20_One(tourData, false);
//			paintPhotoSelection(selection);
//
//			enableActions();

		} else if (selection instanceof SelectionTourIds) {

			// paint all selected tours

//			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
//			if (tourIds.size() == 0) {
//
//				// history tour (without tours) is displayed
//
//				final ArrayList<Photo> allPhotos = paintPhotoSelection(selection);
//
//				if (allPhotos.size() > 0) {
//
////					centerPhotos(allPhotos, false);
//					showDefaultMap(true);
//
//					enableActions();
//				}
//
//			} else if (tourIds.size() == 1) {
//
//				// only 1 tour is displayed, synch with this tour !!!
//
//				final TourData tourData = TourManager.getInstance().getTourData(tourIds.get(0));
//
//				paintTours_20_One(tourData, false);
//				paintPhotoSelection(selection);
//
//				enableActions();
//
//			} else {
//
//				// paint multiple tours
//
//				paintTours(tourIds);
//				paintPhotoSelection(selection);
//
//				enableActions(true);
//			}

		} else if (selection instanceof SelectionChartInfo) {

//			final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;
//
//			TourData tourData = null;
//
//			final Chart chart = chartInfo.getChart();
//			if (chart instanceof TourChart) {
//				final TourChart tourChart = (TourChart) chart;
//				tourData = tourChart.getTourData();
//			}
//
//			if (tourData != null && tourData.isMultipleTours()) {
//
//				// multiple tours are selected
//
//			} else {
//
//				// use old behaviour
//
//				final ChartDataModel chartDataModel = chartInfo.chartDataModel;
//				if (chartDataModel != null) {
//
//					final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
//					if (tourId instanceof Long) {
//
//						tourData = TourManager.getInstance().getTourData((Long) tourId);
//						if (tourData == null) {
//
//							// tour is not in the database, try to get it from the raw data manager
//
//							final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
//							tourData = rawData.get(tourId);
//						}
//					}
//				}
//			}
//
//			if (tourData != null) {
//
//				positionMapTo_TourSliders(
//						tourData,
//						chartInfo.leftSliderValuesIndex,
//						chartInfo.rightSliderValuesIndex,
//						chartInfo.selectedSliderValuesIndex,
//						null);
//
//				enableActions();
//			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

//			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;
//			final Chart chart = xSliderPos.getChart();
//			if (chart == null) {
//				return;
//			}
//
//			final Object customData = xSliderPos.getCustomData();
//			if (customData instanceof SelectedTourSegmenterSegments) {
//
//				/*
//				 * This event is fired in the tour chart when a toursegmenter segment is selected
//				 */
//
//				selectTourSegments((SelectedTourSegmenterSegments) customData);
//
//			} else {
//
//				final ChartDataModel chartDataModel = chart.getChartDataModel();
//				final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
//
//				if (tourId instanceof Long) {
//
//					final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
//					if (tourData != null) {
//
//						final int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
//						int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();
//
//						rightSliderValueIndex =
//								rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
//										? leftSliderValueIndex
//										: rightSliderValueIndex;
//
//						positionMapTo_TourSliders(//
//								tourData,
//								leftSliderValueIndex,
//								rightSliderValueIndex,
//								leftSliderValueIndex,
//								null);
//
//						enableActions();
//					}
//				}
//			}

		} else if (selection instanceof SelectionTourMarker) {

//			final SelectionTourMarker markerSelection = (SelectionTourMarker) selection;
//
//			onSelectionChanged_TourMarker(markerSelection, true);

		} else if (selection instanceof SelectionMapPosition) {

//			final SelectionMapPosition mapPositionSelection = (SelectionMapPosition) selection;
//
//			final int valueIndex1 = mapPositionSelection.getSlider1ValueIndex();
//			int valueIndex2 = mapPositionSelection.getSlider2ValueIndex();
//
//			valueIndex2 = valueIndex2 == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
//					? valueIndex1
//					: valueIndex2;
//
//			positionMapTo_TourSliders(//
//					mapPositionSelection.getTourData(),
//					valueIndex1,
//					valueIndex2,
//					valueIndex1,
//					null);
//
//			enableActions();

		} else if (selection instanceof PointOfInterest) {

//			_isTourOrWayPoint = false;
//
//			clearView();
//
//			final PointOfInterest poi = (PointOfInterest) selection;
//
//			_poiPosition = poi.getPosition();
//			_poiName = poi.getName();
//
//			final String boundingBox = poi.getBoundingBox();
//			if (boundingBox == null) {
//				_poiZoomLevel = _map.getZoom();
//			} else {
//				_poiZoomLevel = _map.getZoom(boundingBox);
//			}
//
//			if (_poiZoomLevel == -1) {
//				_poiZoomLevel = _map.getZoom();
//			}
//
//			_map.setPoi(_poiPosition, _poiZoomLevel, _poiName);
//
//			_actionShowPOI.setChecked(true);
//
//			enableActions();

		} else if (selection instanceof StructuredSelection) {

//			final StructuredSelection structuredSelection = (StructuredSelection) selection;
//			final Object firstElement = structuredSelection.getFirstElement();
//
//			if (firstElement instanceof TVICatalogComparedTour) {
//
//				final TVICatalogComparedTour comparedTour = (TVICatalogComparedTour) firstElement;
//				final long tourId = comparedTour.getTourId();
//
//				final TourData tourData = TourManager.getInstance().getTourData(tourId);
//				paintTours_20_One(tourData, false);
//
//			} else if (firstElement instanceof TVICompareResultComparedTour) {
//
//				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
//				final TourData tourData = TourManager.getInstance().getTourData(
//						compareResultItem.getComparedTourData().getTourId());
//				paintTours_20_One(tourData, false);
//
//			} else if (firstElement instanceof TourWayPoint) {
//
//				final TourWayPoint wp = (TourWayPoint) firstElement;
//
//				final TourData tourData = wp.getTourData();
//
//				paintTours_20_One(tourData, false);
//
//				_map.setPOI(_wayPointToolTipProvider, wp);
//
//				enableActions();
//			}
//
//			enableActions();

		} else if (selection instanceof PhotoSelection) {

//			paintPhotos(((PhotoSelection) selection).galleryPhotos);
//
//			enableActions();

		} else if (selection instanceof SelectionTourCatalogView) {

//			// show reference tour
//
//			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;
//
//			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
//			if (refItem != null) {
//
//				final TourData tourData = TourManager.getInstance().getTourData(refItem.getTourId());
//
//				paintTours_20_One(tourData, false);
//
//				enableActions();
//			}

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}
	}

	@Override
	public void setFocus() {}

}
