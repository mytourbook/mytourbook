/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

package net.tourbook.algorithm;

public class Point {

	/**
	 * x coordinate
	 */
	public int	x;

	/**
	 * y coordinate
	 */
	public int	y;

	/**
	 * Index of this point in the original time serie
	 */
	public int	serieIndex;

	public Point(final int x, final int y, final int serieIndex) {
		this.x = x;
		this.y = y;
		this.serieIndex = serieIndex;
	}

	/**
	 * Difference
	 * 
	 * @param point
	 * @return
	 */
	public Vector diff(final Point point) {
		return new Vector(x - point.x, y - point.y, serieIndex);
	}

	/**
	 * Squared Distance
	 * 
	 * @param point
	 * @return
	 */
	public long d2(final Point point) {
		final Vector difference = diff(point);
		return difference.dot(difference);
	}

	public Point add(final Vector vector) {
		return new Point(x + vector.x, y + vector.y, serieIndex);
	}

}
