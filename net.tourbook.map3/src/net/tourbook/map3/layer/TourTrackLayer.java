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

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;

import java.util.ArrayList;

import net.tourbook.map3.view.Messages;

/**
 */
public class TourTrackLayer {

	public static final String			MAP3_LAYER_ID	= "TourTrackLayer";			//$NON-NLS-1$

	private static ArrayList<Position>	_positions		= new ArrayList<Position>();

	private MarkerLayer					_trackLayer;
	{
		_positions.add(new Position(LatLon.fromDegrees(47.3658, 8.5428), 500));
		_positions.add(new Position(LatLon.fromDegrees(47.3430, 8.6907), 500));
	}

	private MarkerLayer createTracksLayer() {

//		try {
//			final GpxReader reader = new GpxReader();
//			reader.readStream(WWIO.openFileOrResourceStream(TRACK_PATH, this.getClass()));
//			final Iterator<Position> positions = reader.getTrackPositionIterator();

		final BasicMarkerAttributes attrs = new BasicMarkerAttributes(Material.WHITE, BasicMarkerShape.SPHERE, 1d);

		final ArrayList<Marker> markers = new ArrayList<Marker>();
		for (final Position position : _positions) {
			markers.add(new BasicMarker(position, attrs));
		}

		final MarkerLayer layer = new MarkerLayer(markers);
		layer.setOverrideMarkerElevation(true);
		layer.setElevation(0);
		layer.setEnablePickSizeReturn(true);

		return layer;

//		} catch (final ParserConfigurationException e) {
//			e.printStackTrace();
//		} catch (final SAXException e) {
//			e.printStackTrace();
//		} catch (final IOException e) {
//			e.printStackTrace();
//		}
//
//		return null;
	}

	public Layer getLayer() {

		if (_trackLayer == null) {
			_trackLayer = createTracksLayer();
		}

		return _trackLayer;
	}

	public String getName() {
		return Messages.TourTrack_Layer_Name;
	}
}
