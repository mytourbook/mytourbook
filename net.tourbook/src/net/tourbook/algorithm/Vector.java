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


public class Vector extends Point {

	public Vector(int x, int y, int serieIndex) {
		super(x, y, serieIndex);
	}


	/**
	 * Vector product
	 * 
	 * @param vector
	 * @return
	 */
	public long dot(Vector vector) {
		return (long) x * vector.x + (long) y * vector.y;
	}


	/**
	 * Scalar Product
	 * 
	 * @param scalar
	 * @return
	 */
	public Vector dot(float scalar) {
		return new Vector((int) (scalar * x), (int) (scalar * y), serieIndex);
	}


}
