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

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.map3.Messages;
import net.tourbook.map3.view.Map3Manager;

import org.eclipse.jface.dialogs.IDialogSettings;

/**
 */
public class TourTrackLayerWithPaths extends RenderableLayer {

	public static final String			MAP3_LAYER_ID	= "TourTrackLayer"; //$NON-NLS-1$

	private IDialogSettings				_state;

	private final TourPositionColors	_positionColors;

	private final TourTrackConfig		_tourTrackConfig;

	public TourTrackLayerWithPaths(final IDialogSettings state) {

		_state = state;

		_tourTrackConfig = new TourTrackConfig(state);

		_positionColors = new TourPositionColors();

		addPropertyChangeListener(this);
	}

	public TourTrackConfig getConfig() {
		return _tourTrackConfig;
	}

	@Override
	public String getName() {
		return Messages.TourTrack_Layer_Name;
	}

	public void onModifyConfig() {

		for (final Renderable renderable : getRenderables()) {

			if (renderable instanceof Path) {

				final Path path = (Path) renderable;

				path.setAltitudeMode(_tourTrackConfig.altitudeMode);
				path.setFollowTerrain(_tourTrackConfig.isFollowTerrain);
				path.setPathType(_tourTrackConfig.pathType);
			}
		}

		Map3Manager.getWWCanvas().redraw();
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {

		System.out.println(UI.timeStampNano() + " " + getClass() + " \t" + evt.getPropertyName() + " \t" + evt);
		// TODO remove SYSTEM.OUT
	}

	public void saveState() {

		_tourTrackConfig.saveState(_state);

	}

	public void showTours(final ArrayList<TourData> allTours) {

//		final long start = System.currentTimeMillis();

		removeAllRenderables();

		for (final TourData oneTour : allTours) {

			final double[] latSerie = oneTour.latitudeSerie;
			final double[] allLon = oneTour.longitudeSerie;
			final float[] allAlti = oneTour.altitudeSerie;

			if (latSerie == null) {
				continue;
			}

			/*
			 * create positions for all slices
			 */
			final ArrayList<Position> positions = new ArrayList<Position>();

			for (int serieIndex = 0; serieIndex < latSerie.length; serieIndex++) {

				final double lat = latSerie[serieIndex];
				final double lon = allLon[serieIndex];

				float alti = 0;

				if (allAlti != null) {
					alti = allAlti[serieIndex] + 1;
				}

				positions.add(new Position(LatLon.fromDegrees(lat, lon), alti));
			}

			/*
			 * create one path for each tour
			 */
//			final MultiResolutionPath tourPath = new MTMultiResPath(positions);
			final Path tourPath = new Path(positions);

			tourPath.setPathType(AVKey.LINEAR);
//			tourPath.setValue(AVKey.DISPLAY_NAME, MAP3_LAYER_ID);

			// Show how to make the colors vary along the paths.
			tourPath.setPositionColors(_positionColors);

			// Indicate that dots are to be drawn at each specified path position.
//			tourPath.setShowPositions(true);
//			tourPath.setShowPositionsScale(2);

//			tourPath.setSkipCountComputer(tourPath.getSkipCountComputer());

			// Indicate that the dots be drawn only when the path is less than 5 KM from the eye point.
//			tourPath.setShowPositionsThreshold(5e3);

			// Override generic view-distance geometry regeneration because multi-res Paths handle that themselves.
//			tourPath.setViewDistanceExpiration(false);

//			tourPath.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
//			tourPath.setAltitudeMode(WorldWind.ABSOLUTE);
			tourPath.setAltitudeMode(_tourTrackConfig.altitudeMode);
			tourPath.setFollowTerrain(_tourTrackConfig.isFollowTerrain);

			// Create attributes for the Path.
			final ShapeAttributes attrs = new BasicShapeAttributes();
			attrs.setOutlineWidth(6);

			attrs.setDrawInterior(false);
			attrs.setOutlineMaterial(Material.RED);

			tourPath.setAttributes(attrs);

			addRenderable(tourPath);
		}

		_positionColors.updateColors(allTours);

//		System.out.println(UI.timeStampNano() + " showTour\t" + (System.currentTimeMillis() - start) + " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN
	}
}
