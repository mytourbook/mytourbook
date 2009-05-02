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
package net.tourbook.ext.srtm;

public class GeoLat extends GeoCoord {

	static final char	DIRECTION_NORTH	= 'N';
	static final char	DIRECTION_SOUTH	= 'S';

	public static void main(final String[] args) {

	}

	public GeoLat() {
		super();
	}

	public GeoLat(final double d) {
		super();
		set(d);
	}

	public GeoLat(final GeoLat lat) {
		super();
		set(lat);
	}

	public GeoLat(final String s) {
		super();
		set(s);
	}

	public void add(final GeoLat lat) {

		decimal += lat.decimal;
		if (decimal > 90 * faktg)
			decimal = 180 * faktg - decimal;

		updateDegrees();
	}

	public void add(final GeoLat lat, final GeoLat a) {
		decimal = lat.decimal;
		this.add(a);
	}

	@Override
	public char directionMinus() {
		return 'S';
	}

	@Override
	public char directionPlus() {
		return 'N';
	}

	public void set(final GeoLat lat) {
		super.set(lat);
	}

	public void sub(final GeoLat lat) {

		decimal -= lat.decimal;
		if (decimal < -90 * faktg)
			decimal = -180 * faktg - decimal;

		updateDegrees();
	}

	public void sub(final GeoLat lat, final GeoLat s) {
		decimal = lat.decimal;
		this.sub(s);
	}

}
