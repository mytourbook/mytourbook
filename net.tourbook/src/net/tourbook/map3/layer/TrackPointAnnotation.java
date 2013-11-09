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
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GlobeAnnotation;
import net.tourbook.map3.layer.tourtrack.TourTrackConfig;
import net.tourbook.map3.layer.tourtrack.TourTrackConfigManager;
import net.tourbook.map3.view.Map3View;

public class TrackPointAnnotation extends GlobeAnnotation {

	public float	trackAltitude;
	public LatLon	latLon;

	public TrackPointAnnotation(final String text, final Position position) {
		super(text, position);
	}

	public void setSliderPosition(final DrawContext dc) {

		if (latLon == null) {
			// is not fully initialized, this can happen
			return;
		}

		final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

		float sliderYPosition = 0;

		switch (config.altitudeMode) {
		case WorldWind.ABSOLUTE:

			sliderYPosition = trackAltitude;

			if (config.isAltitudeOffset) {

				// append offset
				sliderYPosition += Map3View.getAltitudeOffset(dc.getView().getEyePosition());
			}

			break;

		case WorldWind.RELATIVE_TO_GROUND:

			sliderYPosition = trackAltitude;
			break;

		default:

			// WorldWind.CLAMP_TO_GROUND -> y position = 0

			break;
		}

		setPosition(new Position(latLon, sliderYPosition));
	}
}
