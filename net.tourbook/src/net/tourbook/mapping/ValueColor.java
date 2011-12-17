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
package net.tourbook.mapping;

public class ValueColor {

	public float	value;

	public int		red;
	public int		green;
	public int		blue;

	public ValueColor(final float value, final int red, final int green, final int blue) {

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
	public ValueColor(final ValueColor valueColor) {

		value = valueColor.value;

		red = valueColor.red;
		green = valueColor.green;
		blue = valueColor.blue;
	}

	@Override
	public String toString() {
		return "new ValueColor(" + value + ", " + red + ", " + green + ", " + blue + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

}
