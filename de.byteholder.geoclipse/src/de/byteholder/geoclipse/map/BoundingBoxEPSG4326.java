/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

package de.byteholder.geoclipse.map;

/**
 * these formulas are copied from
 * <a href=
 * "http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#compute_bounding_box_for_tile_number"
 * >OpenStreetMap</a>
 */
public class BoundingBoxEPSG4326 {

	private static int			TILE_SIZE		= 256;

	private static final double	MAX_LATITUDE	= 85.0511;

	public double				top;
	public double				bottom;
	public double				right;
	public double				left;

	public static BoundingBoxEPSG4326 tile2boundingBox(final int x, final int y, final int zoom) {

		final BoundingBoxEPSG4326 bb = new BoundingBoxEPSG4326();

		bb.left = OsmMercator.XToLon(x * TILE_SIZE, zoom);
		bb.bottom = OsmMercator.YToLat((y + 1) * TILE_SIZE, zoom);

		bb.right = OsmMercator.XToLon((x + 1) * TILE_SIZE, zoom);
		bb.top = OsmMercator.YToLat(y * TILE_SIZE, zoom);

//		bb.top = tile2lat(y, zoom);
//		bb.bottom = tile2lat(y + 1, zoom);
//
//		bb.left = tile2lon(x, zoom);
//		bb.right = tile2lon(x + 1, zoom);

//		System.out.println(("x: " + x)
//				+ ("\ty: " + y)
//				+ ("\tleft: " + bb.left)
//				+ ("\tbot: " + bb.bottom)
//				+ ("\tright: " + bb.right)
//				+ ("\ttop: " + bb.top)
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

		return bb;
	}

//	private Point Mercator(double lon, double lat) {
//		/*
//		 * spherical mercator for Google, VE, Yahoo etc
//		 * epsg:900913 R= 6378137
//		 * x = longitude
//		 * y= R*ln(tan(pi/4 + latitude/2)
//		 */
//		double x = 6378137.0 * Math.PI / 180 * lon;
//		double y = 6378137.0 * Math.log(Math.tan(Math.PI / 180 * (45 + lat / 2.0)));
//		
//		return new Point(x, y);
//	}

	static double tile2lat(final int y, final int z) {

		final double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);

		double latitude = Math.toDegrees(Math.atan(Math.sinh(n)));

		if (latitude > MAX_LATITUDE) {
			latitude = MAX_LATITUDE;
		} else if (latitude < -MAX_LATITUDE) {
			latitude = -MAX_LATITUDE;
		}

		return latitude;
	}

	static double tile2lon(final int x, final int z) {
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}

}
