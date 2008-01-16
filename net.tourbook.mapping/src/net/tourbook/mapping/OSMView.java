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

import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.map.TileFactoryInfo;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.gpx.GeoPosition;

/**
 * @author Wolfgang Schramm
 * @since 1.3
 */
public class OSMView extends ViewPart {

	final public static String			ID									= "net.tourbook.mapping.OSMViewID";	//$NON-NLS-1$

	private static final String			MEMENTO_ZOOM_CENTERED				= "osmview.zoom-centered";
	private static final String			MEMENTO_SYNCH_WITH_SELECTED_TOUR	= "osmview.synch-with-selected-tour";
	private static final String			MEMENTO_SYNCH_TOUR_ZOOM_LEVEL		= "osmview.synch-tour-zoom-level";

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

	private boolean						fIsMapSynchedWithTour;

	private boolean						fIsTourCentered;

	public OSMView() {}

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

				if (property.equals(IMappingPreferences.SHOW_TILE_INFO)) {

					// map properties has changed

					IPreferenceStore store = Activator.getDefault().getPreferenceStore();

					boolean isShowTileInfo = store.getBoolean(IMappingPreferences.SHOW_TILE_INFO);

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

		if (fIsTourCentered) {

			final int zoom = fMap.getZoom();

			final Rectangle2D positionRect = getPositionRect(PaintManager.getInstance().getTourBounds(), zoom);
			final Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2,
					positionRect.getY() + positionRect.getHeight() / 2);
			final GeoPosition px = fMap.getTileFactory().pixelToGeo(center, zoom);

			fMap.setCenterPosition(px);
		}
	}

	private void createActions() {

		fActionZoomIn = new ActionZoomIn(this);
		fActionZoomOut = new ActionZoomOut(this);
		fActionZoomCentered = new ActionZoomCentered(this);
		fActionZoomShowAll = new ActionZoomShowAll(this);
		fActionZoomShowEntireTour = new ActionZoomShowEntireTour(this);
		fActionSynchWithTour = new ActionSynchWithTour(this);
		fActionSynchTourZoomLevel = new ActionSynchTourZoomLevel(this);

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

		/*
		 * fill view menu
		 */
		IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(fActionSynchTourZoomLevel);

	}

	@Override
	public void createPartControl(final Composite parent) {

		fMap = new Map(parent);

		GeoClipseExtensions.getInstance().readMapExtensions(fMap);

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

		getViewSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);

		super.dispose();
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
		}
	}

	private void paintTour(final TourData tourData, boolean forceRedraw) {

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
				 * keep map configuration for the prvious tour
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

	private void paintTour(final TourData tourData, final int leftSliderValuesIndex, final int rightSliderValuesIndex) {

		final PaintManager paintManager = PaintManager.getInstance();
		paintManager.setSliderValueIndex(leftSliderValuesIndex, rightSliderValuesIndex);

		fMap.queueRedraw();
	}

	private void restoreSettings() {

		IMemento memento = fSessionMemento;

		if (memento != null) {

			Integer mementoZoomCentered = memento.getInteger(MEMENTO_ZOOM_CENTERED);
			if (mementoZoomCentered != null) {
				final boolean isTourCentered = mementoZoomCentered == 1 ? true : false;
				fActionZoomCentered.setChecked(isTourCentered);
				fIsTourCentered = isTourCentered;
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
		}

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		boolean isShowTileInfo = store.getBoolean(IMappingPreferences.SHOW_TILE_INFO);

		fMap.setDrawTileBorders(isShowTileInfo);

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
		fIsTourCentered = fActionZoomCentered.isChecked();
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
