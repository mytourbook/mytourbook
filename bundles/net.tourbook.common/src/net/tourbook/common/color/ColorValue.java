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
package net.tourbook.common.color;

import net.tourbook.common.util.StatusUtil;

public class ColorValue implements Cloneable {

	public float	value;

	public int		red;
	public int		green;
	public int		blue;

	public ColorValue(final float value, final int red, final int green, final int blue) {

		this.value = value;

		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	@Override
	protected ColorValue clone() throws CloneNotSupportedException {

		ColorValue clonedObject = null;

		try {

			clonedObject = (ColorValue) super.clone();

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	@Override
	public String toString() {
		return "ColorValue(" + value + ", " + red + ", " + green + ", " + blue + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

}
