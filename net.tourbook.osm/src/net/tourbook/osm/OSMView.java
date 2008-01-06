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
package net.tourbook.osm;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.data.TourData;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
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

	final public static String	ID	= "net.tourbook.osm.OSMViewID"; //$NON-NLS-1$

	private ISelectionListener	fPostSelectionListener;

	private Map					fMap;

	private TourData			fCurrentTourData;

	private ActionSynchWithTour	fActionSynchWithTour;
	private ActionZoomIn		fActionZoomIn;
	private ActionZoomOut		fActionZoomOut;
	private ActionZoomShowAll	fActionZoomShowAll;

	private boolean				fIsMapSynchedWithTour;

	public OSMView() {}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onChangeSelection(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void createActions() {

		fActionSynchWithTour = new ActionSynchWithTour(this);
		fActionZoomIn = new ActionZoomIn(this);
		fActionZoomOut = new ActionZoomOut(this);
		fActionZoomShowAll = new ActionZoomShowAll(this);

		/*
		 * fill view toolbar
		 */
		IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();

		viewTbm.add(fActionSynchWithTour);
		viewTbm.add(new Separator());
		viewTbm.add(fActionZoomIn);
		viewTbm.add(fActionZoomOut);
		viewTbm.add(fActionZoomShowAll);
	}

	@Override
	public void createPartControl(final Composite parent) {

		fMap = new Map(parent);

		GeoClipseExtensions.getInstance().readExtensions(fMap);

		createActions();
		addSelectionListener();
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);

		super.dispose();
	}

	private Rectangle2D getPositionRect(final Set<GeoPosition> positions, int zoom) {

		final TileFactory tileFactory = fMap.getTileFactory();
		Point2D point1 = tileFactory.geoToPixel(positions.iterator().next(), zoom);
		Rectangle2D rect = new Rectangle2D.Double(point1.getX(), point1.getY(), 0, 0);

		for (GeoPosition pos : positions) {
			Point2D point = tileFactory.geoToPixel(pos, zoom);
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
			double latitude = latitudeSerie[serieIndex];
			final double longitude = longitudeSerie[serieIndex];

			minLatitude = Math.min(minLatitude, latitude);
			maxLatitude = Math.max(maxLatitude, latitude);

			minLongitude = Math.min(minLongitude, longitude);
			maxLongitude = Math.max(maxLongitude, longitude);

			if (minLatitude == 0) {
				minLatitude = -180D;
			}
		}

		Set<GeoPosition> mapPositions = new HashSet<GeoPosition>();
		mapPositions.add(new GeoPosition(minLatitude, minLongitude));
		mapPositions.add(new GeoPosition(maxLatitude, maxLongitude));

		return mapPositions;
	}

	private void onChangeSelection(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();
			paintTour(tourData);

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId tourIdSelection = (SelectionTourId) selection;

			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			paintTour(tourData);

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor instanceof TourEditor) {
				final TourEditor fTourEditor = (TourEditor) editor;
				final TourChart fTourChart = fTourEditor.getTourChart();
				paintTour(fTourChart.getTourData());
			}
		}
	}

	private void paintTour(final TourData tourData) {

		if (tourData == null) {
			return;
		}

		final double[] longitudeSerie = tourData.longitudeSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;
		if (longitudeSerie == null || longitudeSerie.length == 0 || latitudeSerie == null || latitudeSerie.length == 0) {
			return;
		}

		final PaintManager paintManager = PaintManager.getInstance();
		paintManager.setTourData(tourData);

		final Set<GeoPosition> tourBounds = getTourBounds(tourData);
		paintManager.setTourBounds(tourBounds);

		if (fIsMapSynchedWithTour) {

//		if (fPreviousTourData != null) { 
//
//			/*
//			 * keep map configuration for the prvious tour
//			 */
//			fPreviousTourData.mapZoomLevel = fMap.getZoom();
//
//			final GeoPosition centerPosition = fMap.getCenterPosition();
//			fPreviousTourData.mapCenterPositionLatitude = centerPosition.getLatitude();
//			fPreviousTourData.mapCenterPositionLongitude = centerPosition.getLongitude();
//		}

			if (tourData.mapCenterPositionLatitude == Double.MIN_VALUE) {
				// position tour to the default position
				showTourInMap(tourBounds);
			} else {
				// position tour to the previous or position
//			fMap.setZoom(tourData.mapZoomLevel);
//			fMap.setCenterPosition(new GeoPosition(tourData.mapCenterPositionLatitude,
//					tourData.mapCenterPositionLongitude));
			}
		}
		fCurrentTourData = tourData;

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
	 */
	public void showTourInMap(Set<GeoPosition> positions) {

		if (positions.size() < 2) {
			return;
		}

		final TileFactory tileFactory = fMap.getTileFactory();
		final TileFactoryInfo tileInfo = tileFactory.getInfo();

		final int maximumZoomLevel = tileInfo.getMaximumZoomLevel();
		int zoom = tileInfo.getMinimumZoomLevel();

		Rectangle2D positionRect = getPositionRect(positions, zoom);
		Rectangle viewport = fMap.getViewport();

		while (!viewport.contains(positionRect)) {

			// center position in the map 
			Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2, positionRect.getY()
					+ positionRect.getHeight()
					/ 2);
			GeoPosition px = tileFactory.pixelToGeo(center, zoom);
			fMap.setCenterPosition(px);

			// check zoom level
			if (++zoom >= maximumZoomLevel) {
				break;
			}
			fMap.setZoom(zoom);

			positionRect = getPositionRect(positions, zoom);
			viewport = fMap.getViewport();
		}

		while (positionRect.getWidth() < viewport.width && positionRect.getHeight() < viewport.height) {

			// center position in the map 
			Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2, positionRect.getY()
					+ positionRect.getHeight()
					/ 2);
			GeoPosition px = tileFactory.pixelToGeo(center, zoom);
			fMap.setCenterPosition(px);

			// check zoom level
			if (++zoom >= maximumZoomLevel) {
				break;
			}
			fMap.setZoom(zoom);

			positionRect = getPositionRect(positions, zoom);
			viewport = fMap.getViewport();
		}

		// the algorithm generated a larger zoom level as needed
		fMap.setZoom(--zoom);
	}

	void synchWithTour() {

		fIsMapSynchedWithTour = fActionSynchWithTour.isChecked();

		if (fIsMapSynchedWithTour) {
			paintTour(fCurrentTourData);
		}
	}

	void zoomIn() {

		final int zoom = fMap.getZoom() + 1;
		fMap.setZoom(zoom);

		// center position in the map 
		Rectangle2D positionRect = getPositionRect(PaintManager.getInstance().getTourBounds(), zoom);
		Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2, positionRect.getY()
				+ positionRect.getHeight()
				/ 2);
		GeoPosition px = fMap.getTileFactory().pixelToGeo(center, zoom);
		fMap.setCenterPosition(px);

		fMap.queueRedraw();
	}

	void zoomOut() {
		fMap.setZoom(fMap.getZoom() - 1);
		fMap.queueRedraw();
	}

	void zoomShowAll() {
		fMap.setZoom(fMap.getTileFactory().getInfo().getMinimumZoomLevel());
		fMap.queueRedraw();
	}

}
