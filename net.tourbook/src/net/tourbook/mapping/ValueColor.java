/*******************************************************************************
 * Copyright (C) 2006, 2008  Wolfgang Schramm
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation; either version 2 of the License, or (at your option) any later 
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/

package net.tourbook.mapping;

public class ValueColor {

	public int	value;

	public int	red;
	public int	green;
	public int	blue;

	public ValueColor(int value, int red, int green, int blue) {
		this.value = value;
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	/**
	 * Create a copy
	 * 
	 * @param valueColor
	 */
	public ValueColor(ValueColor valueColor) {
		value = valueColor.value;
		red = valueColor.red;
		green = valueColor.green;
		blue = valueColor.blue;
	}

	@Override
	public String toString() {

//		return value + "\t" + red + "\t" + green + "\t" + blue;

		return "new ValueColor(" + value + ", " + red + ", " + green + ", " + blue + ")";
	}

}
