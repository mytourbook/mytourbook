/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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

import java.awt.Color;

public class ColorUtil {

	/**
	 * Compute a background color that contrasts with the text color.
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @return Returns white or black that contrasts with the background color.
	 */
	public static Color getContrastColor(final int red, final int green, final int blue) {

		final int yiq = ((red * 299) + (green * 587) + (blue * 114)) / 1000;

		if (yiq >= 128) {
			return new Color(0, 0, 0, 0.99f);
		} else {
			return new Color(1, 1, 1, 0.99f);
		}
	}
}
