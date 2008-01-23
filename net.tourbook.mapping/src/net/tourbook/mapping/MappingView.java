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

package net.tourbook.mapping;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.ViewPart;

import de.byteholder.geoclipse.GeoclipseExtensions;
import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.map.TileFactoryInfo;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.gpx.GeoPosition;
import de.byteholder.gpx.ext.PointOfInterest;

/**
 * @author Wolfgang Schramm
 * @since 1.3
 */
public class MappingView extends ViewPart {

	final public static String			ID									= "net.tourbook.mapping.mappingViewID";		//$NON-NLS-1$

	private static final String			MEMENTO_ZOOM_CENTERED				= "mapping.view.zoom-centered";
	private static final String			MEMENTO_SYNCH_WITH_SELECTED_TOUR	= "mapping.view.synch-with-selected-tour";
	private static final String			MEMENTO_SYNCH_TOUR_ZOOM_LEVEL		= "mapping.view.synch-tour-zoom-level";
	private static final String			MEMENTO_CURRENT_FACTORY_ID			= "mapping.view.current.factory-id";

	private static final String			MEMENTO_DEFAULT_POSITION_ZOOM		= "mapping.view.default.position.zoom-level";
	private static final String			MEMENTO_DEFAULT_POSITION_LATITUDE	= "mapping.view.default.position.latitude";
	private static final String			MEMENTO_DEFAULT_POSITION_LONGITUDE	= "mapping.view.default.position.longitude";

	final static String					SHOW_TILE_INFO						= "show.tile-info";

	private static IMemento				fSessionMemento;

	private Map							fMap;

	private ISelectionListener			fPostSelectionListener;
	private IPropertyChangeListener		fPrefChangeListener;
	private IPartListener2				fPartListener;

	private TourData					fTourData;
	private TourData					fPreviousTourData;

	private ActionZoomIn				fActionZoomIn;
	private ActionZoomOut				fActionZoomOut;
	private ActionZoomCentered			fActionZoomCentered;
	private ActionZoomShowAll			fActionZoomShowAll;
	private ActionZoomShowEntireTour	fActionZoomShowEntireTour;
	private ActionSynchWithTour			fActionSynchWithTour;
	private ActionSynchTourZoomLevel	fActionSynchTourZoomLevel;
	private ActionChangeTileFactory		fActionChangeTileFactory;
	private ActionSaveDefaultPosition	fActionSaveDefaultPosition;
	private ActionSetDefaultPosition	fActionSetDefaultPosition;

	private boolean						fIsMapSynchedWithTour;

	private boolean						fIsPositionCentered;

	private List<TileFactory>			fTileFactories;

	private int							fDefaultZoom;
	private GeoPosition					fDefaultPosition					= null;

	private boolean						fIsTour;

	/**
	 * Position for the current point of interest
	 */
	private GeoPosition					fPOIPosition;

	public MappingView() {}

	private void addPartListener() {

		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(SHOW_TILE_INFO)) {

					// map properties has changed

					IPreferenceStore store = Activator.getDefault().getPreferenceStore();

					boolean isShowTileInfo = store.getBoolean(SHOW_TILE_INFO);

					fMap.setDrawTileBorders(isShowTileInfo);
					fMap.queueRedraw();

				}
			}
		};
		Activator.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onChangeSelection(selection);
			}
		};
		getViewSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	/**
	 * Center the tour in the map when action is enabled
	 */
	private void centerTour() {

		if (fIsPositionCentered) {

			final int zoom = fMap.getZoom();

			Set<GeoPosition> positionBounds = null;
			if (fIsTour) {
				positionBounds = PaintManager.getInstance().getTourBounds();
				if (positionBounds == null) {
					return;
				}
			} else {
				if (fPOIPosition == null) {
					return;
				}
				positionBounds = new HashSet<GeoPosition>();
				positionBounds.add(fPOIPosition);
			}

			final Rectangle2D positionRect = getPositionRect(positionBounds, zoom);
			final Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2,
					positionRect.getY() + positionRect.getHeight() / 2);
			final GeoPosition geoPosition = fMap.getTileFactory().pixelToGeo(center, zoom);

			fMap.setCenterPosition(geoPosition);
		}
	}

	/**
	 * Checks if {@link TourData} can be painted
	 * 
	 * @param tourData
	 * @return <code>true</code> when the {@link TourData} can be painted
	 */
	private boolean checkPaintData(final TourData tourData) {

		if (tourData == null) {
			return false;
		}

		// check if coordinates are available
		final double[] longitudeSerie = tourData.longitudeSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;
		if (longitudeSerie == null || longitudeSerie.length == 0 || latitudeSerie == null || latitudeSerie.length == 0) {
			return false;
		}

		return true;
	}

	private void createActions() {

		fActionZoomIn = new ActionZoomIn(this);
		fActionZoomOut = new ActionZoomOut(this);
		fActionZoomCentered = new ActionZoomCentered(this);
		fActionZoomShowAll = new ActionZoomShowAll(this);
		fActionZoomShowEntireTour = new ActionZoomShowEntireTour(this);
		fActionSynchWithTour = new ActionSynchWithTour(this);
		fActionSynchTourZoomLevel = new ActionSynchTourZoomLevel(this);
		fActionChangeTileFactory = new ActionChangeTileFactory(this);
		fActionSetDefaultPosition = new ActionSetDefaultPosition(this);
		fActionSaveDefaultPosition = new ActionSaveDefaultPosition(this);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();

		viewTbm.add(fActionSynchWithTour);
		viewTbm.add(new Separator());
		viewTbm.add(fActionZoomShowAll);
		viewTbm.add(fActionZoomShowEntireTour);
		viewTbm.add(new Separator());
		viewTbm.add(fActionZoomCentered);
		viewTbm.add(fActionZoomIn);
		viewTbm.add(fActionZoomOut);
		viewTbm.add(new Separator());
		viewTbm.add(fActionChangeTileFactory);

		/*
		 * fill view menu
		 */
		IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(fActionSetDefaultPosition);
		menuMgr.add(fActionSaveDefaultPosition);
		menuMgr.add(new Separator());
		menuMgr.add(fActionSynchTourZoomLevel);

	}

	@Override
	public void createPartControl(final Composite parent) {

		fMap = new Map(parent);
		fTileFactories = GeoclipseExtensions.getInstance().readExtensions(fMap);

		createActions();

		addPartListener();
		addPrefListener();
		addSelectionListener();

		restoreSettings();

		// show map from last selection
		onChangeSelection(getSite().getWorkbenchWindow().getSelectionService().getSelection());
	}

	@Override
	public void dispose() {

		// dispose tilefactory resources
		for (TileFactory tileFactory : fTileFactories) {
			tileFactory.dispose();
		}

		getViewSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);

		super.dispose();
	}

	private void enableActions() {

		fActionZoomShowEntireTour.setEnabled(fIsTour);
		fActionSynchTourZoomLevel.setEnabled(fIsTour);
		fActionSynchWithTour.setEnabled(fIsTour);
//		fActionZoomCentered.setEnabled(fIsTour);
	}

	public List<TileFactory> getFactories() {
		return fTileFactories;
	}

	public Map getMap() {
		return fMap;
	}

	private Rectangle2D getPositionRect(final Set<GeoPosition> positions, final int zoom) {

		final TileFactory tileFactory = fMap.getTileFactory();
		final Point2D point1 = tileFactory.geoToPixel(positions.iterator().next(), zoom);
		final Rectangle2D rect = new Rectangle2D.Double(point1.getX(), point1.getY(), 0, 0);

		for (final GeoPosition pos : positions) {
			final Point2D point = tileFactory.geoToPixel(pos, zoom);
			rect.add(point);
		}

		return rect;
	}

	private Set<GeoPosition> getTourBounds(final TourData tourData) {

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		/*
		 * get min/max longitude/latitude
		 */
		double minLatitude = latitudeSerie[0];
		double maxLatitude = latitudeSerie[0];
		double minLongitude = longitudeSerie[0];
		double maxLongitude = longitudeSerie[0];

		for (int serieIndex = 0; serieIndex < latitudeSerie.length; serieIndex++) {
			final double latitude = latitudeSerie[serieIndex];
			final double longitude = longitudeSerie[serieIndex];

			minLatitude = Math.min(minLatitude, latitude);
			maxLatitude = Math.max(maxLatitude, latitude);

			minLongitude = Math.min(minLongitude, longitude);
			maxLongitude = Math.max(maxLongitude, longitude);

			if (minLatitude == 0) {
				minLatitude = -180D;
			}
		}

		final Set<GeoPosition> mapPositions = new HashSet<GeoPosition>();
		mapPositions.add(new GeoPosition(minLatitude, minLongitude));
		mapPositions.add(new GeoPosition(maxLatitude, maxLongitude));

		return mapPositions;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set session memento
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	private void onChangeSelection(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();

			paintTour(tourData, false);

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId tourIdSelection = (SelectionTourId) selection;
			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			paintTour(tourData, false);

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor instanceof TourEditor) {

				final TourEditor fTourEditor = (TourEditor) editor;
				final TourChart fTourChart = fTourEditor.getTourChart();
				final TourData tourData = fTourChart.getTourData();

				paintTour(tourData, false);
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);
			final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

			paintTour(tourData, chartInfo.leftSliderValuesIndex, chartInfo.rightSliderValuesIndex);

		} else if (selection instanceof SelectionChartXSliderPosition) {

			SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;

			final ChartDataModel chartDataModel = xSliderPos.getChart().getChartDataModel();
			final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);

			final int leftSliderValueIndex = xSliderPos.getSlider1ValueIndex();
			int rightSliderValueIndex = xSliderPos.getSlider2ValueIndex();
			rightSliderValueIndex = rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
					? leftSliderValueIndex
					: rightSliderValueIndex;

			paintTour(tourData, leftSliderValueIndex, rightSliderValueIndex);

		} else if (selection instanceof PointOfInterest) {

			fIsTour = false;

			// disable tour data
			fTourData = null;
			fPreviousTourData = null;
			PaintManager.getInstance().setTourData(null);

			PointOfInterest poi = (PointOfInterest) selection;
			fPOIPosition = poi.getPosition();

			fMap.setZoom(poi.getRecommendedZoom());
			fMap.setCenterPosition(fPOIPosition);
			fMap.queueRedraw();
		}

		enableActions();
	}

	private void paintEntireTour() {

		if (checkPaintData(fTourData) == false) {
			return;
		}

		final PaintManager paintManager = PaintManager.getInstance();
		paintManager.setTourData(fTourData);

		// set initial slider position
		paintManager.setSliderValueIndex(0, fTourData.longitudeSerie.length - 1);

		final Set<GeoPosition> tourBounds = getTourBounds(fTourData);
		paintManager.setTourBounds(tourBounds);

		setTourZoomLevel(tourBounds, false);
		fMap.queueRedraw();
	}

	private void paintTour(final TourData tourData, boolean forceRedraw) {

		fIsTour = true;

		if (checkPaintData(tourData) == false) {
			return;
		}

		// prevent loading the same tour
		if (forceRedraw == false && tourData == fTourData) {
			return;
		}

		fTourData = tourData;

		final PaintManager paintManager = PaintManager.getInstance();
		paintManager.setTourData(tourData);

		// set initial slider position
		paintManager.setSliderValueIndex(0, tourData.longitudeSerie.length - 1);

		final Set<GeoPosition> tourBounds = getTourBounds(tourData);
		paintManager.setTourBounds(tourBounds);

		if (fIsMapSynchedWithTour) {

			if (forceRedraw == false && fPreviousTourData != null) {

				/*
				 * keep map configuration for the previous tour
				 */
				fPreviousTourData.mapZoomLevel = fMap.getZoom();

				final GeoPosition centerPosition = fMap.getCenterPosition();
				fPreviousTourData.mapCenterPositionLatitude = centerPosition.getLatitude();
				fPreviousTourData.mapCenterPositionLongitude = centerPosition.getLongitude();
			}
			fPreviousTourData = tourData;

			if (tourData.mapCenterPositionLatitude == Double.MIN_VALUE) {
				// position tour to the default position
				setTourZoomLevel(tourBounds, true);
			} else {
				// position tour to the previous or position
				fMap.setZoom(tourData.mapZoomLevel);
				fMap.setCenterPosition(new GeoPosition(tourData.mapCenterPositionLatitude,
						tourData.mapCenterPositionLongitude));
			}
		}

		fMap.queueRedraw();
	}

	private void paintTour(final TourData tourData, final int leftSliderValuesIndex, final int rightSliderValuesIndex) {

		fIsTour = true;

		PaintManager.getInstance().setSliderValueIndex(leftSliderValuesIndex, rightSliderValuesIndex);

		fMap.queueRedraw();
	}

	private void restoreSettings() {

		IMemento memento = fSessionMemento;

		if (memento != null) {

			Integer mementoZoomCentered = memento.getInteger(MEMENTO_ZOOM_CENTERED);
			if (mementoZoomCentered != null) {
				final boolean isTourCentered = mementoZoomCentered == 1 ? true : false;
				fActionZoomCentered.setChecked(isTourCentered);
				fIsPositionCentered = isTourCentered;
			}

			Integer mementoSynchTour = memento.getInteger(MEMENTO_SYNCH_WITH_SELECTED_TOUR);
			if (mementoSynchTour != null) {

				final boolean isSynchTour = mementoSynchTour == 1 ? true : false;

				fActionSynchWithTour.setChecked(isSynchTour);
				fIsMapSynchedWithTour = isSynchTour;
			}

			Integer zoomLevel = memento.getInteger(MEMENTO_SYNCH_TOUR_ZOOM_LEVEL);
			if (zoomLevel != null) {
				fActionSynchTourZoomLevel.setZoomLevel(zoomLevel);
			}

			// restore: factory ID
			fActionChangeTileFactory.setSelectedFactory(memento.getString(MEMENTO_CURRENT_FACTORY_ID));

			// restore: default position
			Integer mementoZoom = memento.getInteger(MEMENTO_DEFAULT_POSITION_ZOOM);
			if (mementoZoom != null) {
				fDefaultZoom = mementoZoom;
			}
			Float mementoLatitude = memento.getFloat(MEMENTO_DEFAULT_POSITION_LATITUDE);
			Float mementoLongitude = memento.getFloat(MEMENTO_DEFAULT_POSITION_LONGITUDE);
			if (mementoLatitude != null && mementoLongitude != null) {
				fDefaultPosition = new GeoPosition(mementoLatitude, mementoLongitude);
			} else {
				fDefaultPosition = new GeoPosition(0, 0);
			}

		} else {
			fActionChangeTileFactory.setSelectedFactory(null);
		}

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		boolean isShowTileInfo = store.getBoolean(MappingView.SHOW_TILE_INFO);

		fMap.setDrawTileBorders(isShowTileInfo);
		setDefaultPosition();

	}

	void saveDefaultPosition() {
		fDefaultZoom = fMap.getZoom();
		fDefaultPosition = fMap.getCenterPosition();
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("DeviceImportView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		// save checked actions
		memento.putInteger(MEMENTO_ZOOM_CENTERED, fActionZoomCentered.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SYNCH_WITH_SELECTED_TOUR, fActionSynchWithTour.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SYNCH_TOUR_ZOOM_LEVEL, fActionSynchTourZoomLevel.getZoomLevel());

		memento.putString(MEMENTO_CURRENT_FACTORY_ID, fActionChangeTileFactory.getSelectedFactory()
				.getInfo()
				.getFactoryID());

		if (fDefaultPosition == null) {
			memento.putInteger(MEMENTO_DEFAULT_POSITION_ZOOM, fMap.getTileFactory().getInfo().getMinimumZoomLevel());
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LATITUDE, 0.0F);
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LONGITUDE, 0.0F);
		} else {
			memento.putInteger(MEMENTO_DEFAULT_POSITION_ZOOM, fDefaultZoom);
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LATITUDE, (float) fDefaultPosition.getLatitude());
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LONGITUDE, (float) fDefaultPosition.getLongitude());
		}

	}

	void setDefaultPosition() {
		if (fDefaultPosition == null) {
			fMap.setZoom(fMap.getTileFactory().getInfo().getMinimumZoomLevel());
			fMap.setCenterPosition(new GeoPosition(0, 0));
		} else {
			fMap.setZoom(fDefaultZoom);
			fMap.setCenterPosition(fDefaultPosition);
		}
		fMap.queueRedraw();
	}

	@Override
	public void setFocus() {}

	/**
	 * Calculates a zoom level so that all points in the specified set will be visible on screen.
	 * This is useful if you have a bunch of points in an area like a city and you want to zoom out
	 * so that the entire city and it's points are visible without panning.
	 * 
	 * @param positions
	 *        A set of GeoPositions to calculate the new zoom from
	 * @param adjustZoomLevel
	 *        when <code>true</code> the zoom level will be adjusted to user settings
	 */
	private void setTourZoomLevel(final Set<GeoPosition> positions, boolean isAdjustZoomLevel) {

		if (positions.size() < 2) {
			return;
		}

		final TileFactory tileFactory = fMap.getTileFactory();
		final TileFactoryInfo tileInfo = tileFactory.getInfo();

		final int maximumZoomLevel = tileInfo.getMaximumZoomLevel();
		int zoom = tileInfo.getMinimumZoomLevel();

		Rectangle2D positionRect = getPositionRect(positions, zoom);
		Rectangle viewport = fMap.getViewport();

		// zoom until the tour is visible in the map
		while (!viewport.contains(positionRect)) {

			// center position in the map 
			final Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2,
					positionRect.getY() + positionRect.getHeight() / 2);
			final GeoPosition px = tileFactory.pixelToGeo(center, zoom);
			fMap.setCenterPosition(px);

			// check zoom level
			if (++zoom >= maximumZoomLevel) {
				break;
			}
			fMap.setZoom(zoom);

			positionRect = getPositionRect(positions, zoom);
			viewport = fMap.getViewport();
		}

		// zoom in until the tour is larger than the viewport
		while (positionRect.getWidth() < viewport.width && positionRect.getHeight() < viewport.height) {

			// center position in the map 
			final Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2,
					positionRect.getY() + positionRect.getHeight() / 2);
			final GeoPosition px = tileFactory.pixelToGeo(center, zoom);
			fMap.setCenterPosition(px);

			// check zoom level
			if (++zoom >= maximumZoomLevel) {
				break;
			}
			fMap.setZoom(zoom);

			positionRect = getPositionRect(positions, zoom);
			viewport = fMap.getViewport();
		}

		// the algorithm generated a larger zoom level as necessary
		zoom--;

		int adjustedZoomLevel = 0;
		if (isAdjustZoomLevel) {
			adjustedZoomLevel = PaintManager.getInstance().getSynchTourZoomLevel();
		}

		fMap.setZoom(zoom + adjustedZoomLevel);
	}

	void setZoomCentered() {
		fIsPositionCentered = fActionZoomCentered.isChecked();
	}

	void synchWithTour() {

		fIsMapSynchedWithTour = fActionSynchWithTour.isChecked();

		if (fIsMapSynchedWithTour) {
			paintTour(fTourData, true);
		}
	}

	void zoomIn() {
		fMap.setZoom(fMap.getZoom() + 1);
		centerTour();
		fMap.queueRedraw();
	}

	void zoomOut() {
		fMap.setZoom(fMap.getZoom() - 1);
		centerTour();
		fMap.queueRedraw();
	}

	void zoomShowEntireMap() {
		fMap.setZoom(fMap.getTileFactory().getInfo().getMinimumZoomLevel());
		fMap.queueRedraw();
	}

	void zoomShowEntireTour() {
		paintEntireTour();
	}
}
