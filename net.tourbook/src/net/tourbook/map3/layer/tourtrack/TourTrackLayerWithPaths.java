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
package net.tourbook.map3.layer.tourtrack;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.color.ILegendProvider;
import net.tourbook.data.TourData;
import net.tourbook.map3.Messages;
import net.tourbook.map3.view.Map3Manager;

import org.eclipse.jface.dialogs.IDialogSettings;

/**
 */
public class TourTrackLayerWithPaths extends RenderableLayer {

	public static final String			MAP3_LAYER_ID	= "TourTrackLayer"; //$NON-NLS-1$

	private IDialogSettings				_state;

	private final TourPositionColors	_tourPositionColors;

	private final TourTrackConfig		_trackConfig;

	public TourTrackLayerWithPaths(final IDialogSettings state) {

		_state = state;

		_trackConfig = new TourTrackConfig(state);

		_tourPositionColors = new TourPositionColors();

		addPropertyChangeListener(this);
	}

	/**
	 * Create a path for each tour.
	 * 
	 * @param allTours
	 * @return
	 */
	public ArrayList<PositionWithTour> createTrackPaths(final ArrayList<TourData> allTours) {

//		final long start = System.currentTimeMillis();

		removeAllRenderables();

		final ArrayList<PositionWithTour> allPositions = new ArrayList<PositionWithTour>();

		for (final TourData tourData : allTours) {

			final double[] latSerie = tourData.latitudeSerie;
			final double[] lonSerie = tourData.longitudeSerie;
			final float[] altiSerie = tourData.altitudeSerie;

			if (latSerie == null) {
				continue;
			}

			/*
			 * create positions for all slices
			 */
			final ArrayList<PositionWithTour> trackPositions = new ArrayList<PositionWithTour>();

			for (int serieIndex = 0; serieIndex < latSerie.length; serieIndex++) {

				final double lat = latSerie[serieIndex];
				final double lon = lonSerie[serieIndex];

				float alti = 0;

				if (altiSerie != null) {
					alti = altiSerie[serieIndex] + 1;
				}

				final float dataSerieValue;

				trackPositions.add(new PositionWithTour(LatLon.fromDegrees(lat, lon), alti, dataSerieValue));
			}

			/*
			 * create one path for each tour
			 */
//			final MultiResolutionPath tourPath = new MTMultiResPath(positions);
			final PathWithTour tourPath = new PathWithTour(trackPositions);

			setPathAttributes(tourPath);

			addRenderable(tourPath);

			// keep all positions which is used to find the outline for ALL selected tours
			allPositions.addAll(trackPositions);
		}

		_tourPositionColors.updateColors(allTours);

//		System.out.println(UI.timeStampNano() + " showTour\t" + (System.currentTimeMillis() - start) + " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN

		return allPositions;
	}

	public TourTrackConfig getConfig() {
		return _trackConfig;
	}

	/**
	 * Set the data serie which is painted
	 * 
	 * @param tourData
	 */
	private float[] getDataSerie(final TourData tourData) {

//		final ILegendProvider legendProvider = _tourPaintConfig.getLegendProvider();
//		if (legendProvider == null) {
//			_dataSerie = null;
//			return;
//		}


		switch (_legendProvider.getTourColorId()) {
		case ILegendProvider.TOUR_COLOR_ALTITUDE:
			_dataSerie = tourData.getAltitudeSerie();
			break;

		case ILegendProvider.TOUR_COLOR_GRADIENT:
			_dataSerie = tourData.getGradientSerie();
			break;

		case ILegendProvider.TOUR_COLOR_PULSE:
			_dataSerie = tourData.pulseSerie;
			break;

		case ILegendProvider.TOUR_COLOR_SPEED:
			_dataSerie = tourData.getSpeedSerie();
			break;

		case ILegendProvider.TOUR_COLOR_PACE:
			_dataSerie = tourData.getPaceSerieSeconds();
			break;

		case ILegendProvider.TOUR_COLOR_HR_ZONE:
			_dataSerie = tourData.pulseSerie;
			break;

		default:
			break;
		}
	}


	@Override
	public String getName() {
		return Messages.TourTrack_Layer_Name;
	}

	public void onModifyConfig() {

		for (final Renderable renderable : getRenderables()) {

			if (renderable instanceof Path) {
				setPathAttributes((Path) renderable);
			}
		}

		Map3Manager.getWWCanvas().redraw();
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \t")
				+ evt.getPropertyName()
				+ " \t"
				+ evt);
		// TODO remove SYSTEM.OUT
	}

	public void saveState() {

		_trackConfig.saveState(_state);
	}

	private void setPathAttributes(final Path path) {

//		tourPath.setValue(AVKey.DISPLAY_NAME, MAP3_LAYER_ID);

		// Indicate that dots are to be drawn at each specified path position.
		path.setShowPositions(_trackConfig.isShowTrackPosition);
		path.setShowPositionsScale(_trackConfig.trackPositionSize);

//		tourPath.setSkipCountComputer(tourPath.getSkipCountComputer());

		// Indicate that the dots be drawn only when the path is less than 5 KM from the eye point.
//		tourPath.setShowPositionsThreshold(5e3);

		// Override generic view-distance geometry regeneration because multi-res Paths handle that themselves.
//		tourPath.setViewDistanceExpiration(false);

		path.setAltitudeMode(_trackConfig.altitudeMode);
		path.setFollowTerrain(_trackConfig.isFollowTerrain);
		path.setPathType(_trackConfig.pathType);

		path.setExtrude(_trackConfig.isExtrudePath);
		path.setDrawVerticals(_trackConfig.isDrawVerticals);

		/*
		 * Create attributes for the Path.
		 */

		final ShapeAttributes shapeAttrs = new BasicShapeAttributes();

		shapeAttrs.setDrawOutline(true);
		shapeAttrs.setOutlineWidth(_trackConfig.outlineWidth);
		shapeAttrs.setOutlineMaterial(Material.GRAY);
		shapeAttrs.setOutlineOpacity(0.5);

		shapeAttrs.setDrawInterior(true);
		shapeAttrs.setInteriorMaterial(Material.YELLOW);
		shapeAttrs.setInteriorOpacity(0.5);

		path.setAttributes(shapeAttrs);

		// Show how to make the colors vary along the paths.
		path.setPositionColors(_tourPositionColors);
	}

	public void updateColors(final ArrayList<TourData> allTours) {

		_tourPositionColors.updateColors(allTours);
	}

	public void updateColors(final ArrayList<TourData> _allTours, final ILegendProvider legendProvider) {

		_tourPositionColors.setColorProvider(legendProvider);
	}
}
