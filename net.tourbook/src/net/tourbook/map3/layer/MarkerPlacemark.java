/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.PointPlacemark;
import net.tourbook.map3.view.Map3View;

public class MarkerPlacemark extends PointPlacemark {

	String	markerText;
	double	absoluteAltitude;

	public MarkerPlacemark(final Position position) {

		super(position);

		this.absoluteAltitude = position.elevation;
	}

	/**
	 * Recognize altitude offset when height is computed.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	protected void computePlacemarkPoints(final DrawContext dc) {

		this.placePoint = null;
		this.terrainPoint = null;
		this.screenPoint = null;

		final Position pos = this.getPosition();
		if (pos == null) {
			return;
		}

		if (this.altitudeMode == WorldWind.CLAMP_TO_GROUND) {

			this.placePoint = dc.computeTerrainPoint(pos.getLatitude(), pos.getLongitude(), 0);

		} else if (this.altitudeMode == WorldWind.RELATIVE_TO_GROUND) {

			this.placePoint = dc.computeTerrainPoint(pos.getLatitude(), pos.getLongitude(), pos.getAltitude());

		} else {

			// ABSOLUTE

			final double height = pos.getElevation()
					* (this.isApplyVerticalExaggeration() ? dc.getVerticalExaggeration() : 1);

			// get altitude offset
			final double altitudeOffset = Map3View.getAltitudeOffset(dc.getView().getEyePosition());

			this.placePoint = dc.getGlobe().computePointFromPosition(
					pos.getLatitude(),
					pos.getLongitude(),
					height + altitudeOffset);
		}

		if (this.placePoint == null) {
			return;
		}

		// Compute a terrain point if needed.
		if (this.isLineEnabled() && this.altitudeMode != WorldWind.CLAMP_TO_GROUND) {
			this.terrainPoint = dc.computeTerrainPoint(pos.getLatitude(), pos.getLongitude(), 0);
		}

		// Compute the placemark point's screen location.
		this.screenPoint = dc.getView().project(this.placePoint);
		this.eyeDistance = this.placePoint.distanceTo3(dc.getView().getEyePoint());
	}

	public void setMarkerText(final String markerText) {

		this.markerText = markerText;
	}

}
