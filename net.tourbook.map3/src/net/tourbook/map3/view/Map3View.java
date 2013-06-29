/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.view;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;

import java.awt.BorderLayout;

import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class Map3View extends ViewPart {

	public static final String					ID			= "net.tourbook.map3.Map3View"; //$NON-NLS-1$

	private static final WorldWindowGLCanvas	_wwCanvas	= Map3Manager.getWWCanvas();

	private ActionOpenMap3Properties			_actionOpenMap3Properties;

	private IPartListener2						_partListener;
	private ISelectionListener					_postSelectionListener;
	private ITourEventListener					_tourEventListener;

	private boolean								_isPartVisible;
	private ISelection							_selectionWhenHidden;

	private static int							_renderCounter;

	public Map3View() {}

	private void addMap3Listener() {

		// Register a rendering listener that's notified when exceptions occur during rendering.
		_wwCanvas.addRenderingListener(new RenderingListener() {

			@Override
			public void stageChanged(final RenderingEvent event) {

				Display.getDefault().asyncExec(new Runnable() {
					public void run() {

						System.out.println(UI.timeStampNano() + " is rendered: " + _renderCounter++);
						// TODO remove SYSTEM.OUT.PRINTLN

					}
				});
			}
		});
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map3View.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map3View.this) {
					_isPartVisible = false;
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map3View.this) {

					_isPartVisible = true;

					if (_selectionWhenHidden != null) {

						onSelectionChanged(_selectionWhenHidden);

						_selectionWhenHidden = null;
					}
				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == Map3View.this) {
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

				if (part == Map3View.this) {
					return;
				}

//				if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {
//
//					resetMap();
//
//				} else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {
//
//					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
//					if ((modifiedTours != null) && (modifiedTours.size() > 0)) {
//
//						_allTourData.clear();
//						_allTourData.addAll(modifiedTours);
//
//						resetMap();
//					}
//
//				} else if (eventId == TourEventId.UPDATE_UI || eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {
//
//					clearView();
//
//				} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED) {
//					onSelectionChanged((ISelection) eventData);
//				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void createActions() {

		_actionOpenMap3Properties = new ActionOpenMap3Properties();

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionOpenMap3Properties);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addPartListener();
		addSelectionListener();
		addTourEventListener();
//		addMap3Listener();
		createActions();

		restoreState();
		Map3Manager.setMap3View(this);
	}

	private void createUI(final Composite parent) {

		// set parent griddata, this must be done AFTER the content is created, otherwise it fails !!!
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);

		// build GUI: container(SWT) -> Frame(AWT) -> Panel(AWT) -> WorldWindowGLCanvas(AWT)
		final Composite container = new Composite(parent, SWT.EMBEDDED);
		GridDataFactory.fillDefaults().applyTo(container);
		{
			final java.awt.Frame awtFrame = SWT_AWT.new_Frame(container);
			final java.awt.Panel awtPanel = new java.awt.Panel(new java.awt.BorderLayout());

			awtFrame.add(awtPanel);
			awtPanel.add(_wwCanvas, BorderLayout.CENTER);
		}

		parent.layout();
	}

	@Override
	public void dispose() {

		Map3Manager.setMap3View(null);

		super.dispose();
	}

	private void enableActions() {
		// TODO Auto-generated method stub

	}

	private void onSelectionChanged(final ISelection selection) {

//		System.out.println(net.tourbook.common.UI.timeStampNano() + " Map::onSelectionChanged\t" + selection);
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
//			paintTours_20_One(tourData, selectionTourData.isForceRedraw(), true);
//			paintPhotoSelection(selection);
//
//			enableActions();

		} else if (selection instanceof SelectionTourId) {

			final Long tourId = ((SelectionTourId) selection).getTourId();
			final TourData tourData = TourManager.getInstance().getTourData(tourId);

			showTour(tourData);
//			paintPhotoSelection(selection);

			enableActions();

//		} else if (selection instanceof SelectionTourIds) {
//
//			// paint all selected tours
//
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
//				paintTours_20_One(tourData, false, true);
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
//
//		} else if (selection instanceof SelectionChartInfo) {
//
//			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
//			if (chartDataModel != null) {
//
//				final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
//				if (tourId instanceof Long) {
//
//					TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
//					if (tourData == null) {
//
//						// tour is not in the database, try to get it from the raw data manager
//
//						final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
//						tourData = rawData.get(tourId);
//					}
//
//					if (tourData != null) {
//
//						final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;
//
//						paintTourSliders(
//								tourData,
//								chartInfo.leftSliderValuesIndex,
//								chartInfo.rightSliderValuesIndex,
//								chartInfo.selectedSliderValuesIndex);
//
//						enableActions();
//					}
//				}
//			}
//
//		} else if (selection instanceof SelectionChartXSliderPosition) {
//
//			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;
//			final Chart chart = xSliderPos.getChart();
//			if (chart == null) {
//				return;
//			}
//
//			final ChartDataModel chartDataModel = chart.getChartDataModel();
//
//			final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
//			if (tourId instanceof Long) {
//
//				final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
//				if (tourData != null) {
//
//					final int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
//					int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();
//
//					rightSliderValueIndex = rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
//							? leftSliderValueIndex
//							: rightSliderValueIndex;
//
//					paintTourSliders(tourData, leftSliderValueIndex, rightSliderValueIndex, leftSliderValueIndex);
//
//					enableActions();
//				}
//			}
//
//		} else if (selection instanceof SelectionMapPosition) {
//
//			final SelectionMapPosition mapPositionSelection = (SelectionMapPosition) selection;
//
//			final int valueIndex1 = mapPositionSelection.getSlider1ValueIndex();
//			int valueIndex2 = mapPositionSelection.getSlider2ValueIndex();
//
//			valueIndex2 = valueIndex2 == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
//					? valueIndex1
//					: valueIndex2;
//
//			paintTourSliders(mapPositionSelection.getTourData(), valueIndex1, valueIndex2, valueIndex1);
//
//			enableActions();
//
//		} else if (selection instanceof PointOfInterest) {
//
//			_isTourOrWayPoint = false;
//
//			clearView();
//
//			final PointOfInterest poi = (PointOfInterest) selection;
//
//			_poiPosition = poi.getPosition();
//			_poiName = poi.getName();
//
//			_poiZoomLevel = poi.getRecommendedZoom();
//			if (_poiZoomLevel == -1) {
//				_poiZoomLevel = _map.getZoom();
//			}
//
//			_map.setPoi(_poiPosition, _poiZoomLevel, _poiName);
//
//			_actionShowPOI.setChecked(true);
//
//			enableActions();
//
//		} else if (selection instanceof StructuredSelection) {
//
//			final StructuredSelection structuredSelection = (StructuredSelection) selection;
//			final Object firstElement = structuredSelection.getFirstElement();
//
//			if (firstElement instanceof TVICatalogComparedTour) {
//
//				final TVICatalogComparedTour comparedTour = (TVICatalogComparedTour) firstElement;
//				final long tourId = comparedTour.getTourId();
//
//				final TourData tourData = TourManager.getInstance().getTourData(tourId);
//				paintTours_20_One(tourData, false, true);
//
//			} else if (firstElement instanceof TVICompareResultComparedTour) {
//
//				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
//				final TourData tourData = TourManager.getInstance().getTourData(
//						compareResultItem.getComparedTourData().getTourId());
//				paintTours_20_One(tourData, false, true);
//
//			} else if (firstElement instanceof TourWayPoint) {
//
//				final TourWayPoint wp = (TourWayPoint) firstElement;
//
//				_map.setPOI(_wayPointToolTipProvider, wp);
//			}
//
//			enableActions();
//
//		} else if (selection instanceof PhotoSelection) {
//
//			paintPhotos(((PhotoSelection) selection).galleryPhotos);
//
//			enableActions();
//
//		} else if (selection instanceof SelectionTourCatalogView) {
//
//			// show reference tour
//
//			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;
//
//			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
//			if (refItem != null) {
//
//				final TourData tourData = TourManager.getInstance().getTourData(refItem.getTourId());
//
//				paintTours_20_One(tourData, false, true);
//
//				enableActions();
//			}
		}
	}

	private void restoreState() {
		// TODO Auto-generated method stub

	}

	private void saveState() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFocus() {

	}

	private void showTour(final TourData tourData) {

		Map3Manager.getTourLayer().showTour(tourData);

		_wwCanvas.redraw();

	}

}
