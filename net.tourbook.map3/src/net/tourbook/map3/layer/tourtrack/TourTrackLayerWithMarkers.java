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
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.map3.view.Messages;

/**
 */
public class TourTrackLayerWithMarkers extends MarkerLayer implements PropertyChangeListener {

	public static final String			MAP3_LAYER_ID	= "TourTrackLayer";			//$NON-NLS-1$

	private static ArrayList<Position>	_positions		= new ArrayList<Position>();

	private ArrayList<TourData>			_allTours;

	static {
		_positions.add(new Position(LatLon.fromDegrees(47.3658, 8.5428), 500));
		_positions.add(new Position(LatLon.fromDegrees(47.3430, 8.6907), 500));
	}

	public TourTrackLayerWithMarkers() {

		setOverrideMarkerElevation(true);
		setElevation(0);
		setEnablePickSizeReturn(true);

//		setKeepSeparated(false); ????????????

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

	public void setDefaultPositons() {

		final BasicMarkerAttributes attrs = new BasicMarkerAttributes(Material.WHITE, BasicMarkerShape.SPHERE, 1d);

		final ArrayList<Marker> markers = new ArrayList<Marker>();
		for (final Position position : _positions) {
			markers.add(new BasicMarker(position, attrs));
		}

		setMarkers(markers);
	}

	private void showTours() {

		//		final long start = System.currentTimeMillis();

		final ArrayList<Marker> markers = new ArrayList<Marker>();
		final BasicMarkerAttributes attrs = new BasicMarkerAttributes(Material.YELLOW, BasicMarkerShape.SPHERE, 1d);

		for (final TourData oneTour : _allTours) {

			final double[] allLat = oneTour.latitudeSerie;
			final double[] allLon = oneTour.longitudeSerie;
			final float[] allAlti = oneTour.altitudeSerie;

			if (allLat == null) {
				continue;
			}


			for (int serieIndex = 0; serieIndex < allLat.length; serieIndex++) {

				final double lat = allLat[serieIndex];
				final double lon = allLon[serieIndex];

				float alti = 0;

				if (allAlti != null) {
					alti = allAlti[serieIndex];
				}

				markers.add(new BasicMarker(new Position(LatLon.fromDegrees(lat, lon), alti), attrs));
			}
		}

		setMarkers(markers);

		//		System.out.println(UI.timeStampNano() + " showTour\t" + (System.currentTimeMillis() - start) + " ms");
		//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	public void showTours(final ArrayList<TourData> allTours) {

		_allTours = allTours;

		showTours();
	}

}
