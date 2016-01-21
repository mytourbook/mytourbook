/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
/**
 * @author Alfred Barten
 */
package net.tourbook.srtm;

public class GeoLon extends GeoCoord {

	static final char	DIRECTION_WEST	= 'W';
	static final char	DIRECTION_EAST	= 'E';

	public static void main(final String[] args) {}

	public GeoLon() {
		super();
	}

	public GeoLon(final double d) {
		super();
		set(d);
	}

	public GeoLon(final GeoLon lon) {
		super();
		set(lon);
	}

	public GeoLon(final String s) {
		super();
		set(s);
	}

	public void add(final GeoLon lon) {

		decimal += lon.decimal;
		if (decimal > 180 * faktg)
			decimal -= 360 * faktg;

		updateDegrees();
	}

	public void add(final GeoLon lon, final GeoLon a) {
		decimal = lon.decimal;
		this.add(a);
	}

	@Override
	public char directionMinus() {
		return DIRECTION_WEST;
	}

	@Override
	public char directionPlus() {
		return DIRECTION_EAST;
	}

	public void set(final GeoLon lon) {
		super.set(lon);
	}

	public void sub(final GeoLon lon) {

		decimal -= lon.decimal;
		if (decimal < -180 * faktg)
			decimal += 360 * faktg;

		updateDegrees();
	}

	public void sub(final GeoLon lon, final GeoLon s) {
		decimal = lon.decimal;
		this.sub(s);
	}
}
