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
package net.tourbook.map3.layer;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.ShapeAttributes;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.map3.view.Messages;

/**
 */
public class TourTrackLayerWithPaths extends RenderableLayer {

	public static final String	MAP3_LAYER_ID	= "TourTrackLayer"; //$NON-NLS-1$

	private Path				_trackPath;

	public TourTrackLayerWithPaths() {


		_trackPath = new Path();

		_trackPath.setPathType(AVKey.LINEAR);
		_trackPath.setValue(AVKey.DISPLAY_NAME, MAP3_LAYER_ID);

		// Show how to make the colors vary along the paths.
		_trackPath.setPositionColors(new TourPositionColors());

		addRenderable(_trackPath);

		addPropertyChangeListener(this);
	}

	@Override
	public String getName() {
		return Messages.TourTrack_Layer_Name;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {

		System.out.println(UI.timeStampNano() + " \t" + evt);
		// TODO remove SYSTEM.OUT
	}

	public void showTour(final TourData tourData) {

//		final long start = System.currentTimeMillis();

		final double[] allLat = tourData.latitudeSerie;
		final double[] allLon = tourData.longitudeSerie;
		final float[] allAlti = tourData.altitudeSerie;

		if (allLat == null) {
			return;
		}

		final ArrayList<Position> positions = new ArrayList<Position>();

		for (int serieIndex = 0; serieIndex < allLat.length; serieIndex++) {

			final double lat = allLat[serieIndex];
			final double lon = allLon[serieIndex];

			float alti = 0;

			if (allAlti != null) {
				alti = allAlti[serieIndex] + 1;
			}

			positions.add(new Position(LatLon.fromDegrees(lat, lon), alti));
		}

		_trackPath.setPositions(positions);

//		_trackPath.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
		_trackPath.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
//		_trackPath.setAltitudeMode(WorldWind.ABSOLUTE);

		// path shape
		final ShapeAttributes attrs = new BasicShapeAttributes();
		attrs.setOutlineWidth(10);
		_trackPath.setAttributes(attrs);

//		System.out.println(UI.timeStampNano() + " showTour\t" + (System.currentTimeMillis() - start) + " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN
	}
}
