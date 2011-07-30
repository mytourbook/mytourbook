/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.export;

import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpoint;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointAdapter;

public class GarminTrackpointAdapterExtended extends GarminTrackpointAdapter {

	private double	_temperature	= Double.NaN;

	public GarminTrackpointAdapterExtended(final GarminTrackpoint trackpoint) {
		super(trackpoint);
	}

	/**
	 * @return Returns temperature or {@link Double#NaN} when temperature is not set
	 */
	public double getTemperature() {
		return _temperature;
	}

	public boolean hasValidExtention() {

		if (hasValidTemperature()) {
			return true;
		}

		if (hasValidHeartrate()) {
			return true;
		}

		if (hasValidCadence()) {
			return true;
		}

		if (hasValidDistance()) {
			return true;
		}

		return false;
	}

	/**
	 * Returns <code>true</code> if the temperature of this waypoint is valid. This is equal to the
	 * expression <code>!Double.isNaN(getTemperature())</code>.
	 * 
	 * @return Returns <code>true</code> if waypoint has valid temperature.
	 */
	public boolean hasValidTemperature() {
		return (!Double.isNaN(_temperature));
	}

	public void setTemperature(final double temperature) {
		_temperature = temperature;
	}

}
