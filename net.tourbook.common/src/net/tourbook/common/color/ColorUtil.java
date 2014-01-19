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

import java.awt.Color;

import org.eclipse.swt.graphics.Device;

public class ColorUtil {

	public static org.eclipse.swt.graphics.Color getContrastColor(final Device device, final int rgbValue) {

		final byte blue = (byte) ((rgbValue & 0xFF0000) >> 16);
		final byte green = (byte) ((rgbValue & 0xFF00) >> 8);
		final byte red = (byte) ((rgbValue & 0xFF) >> 0);

		return getContrastColor(device, red & 0xFF, green & 0xFF, blue & 0xFF);
	}

	/**
	 * Compute a background color that contrasts with the text color.
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @return Returns white or black that contrasts with the background color.
	 */
	public static org.eclipse.swt.graphics.Color getContrastColor(	final Device display,
																	final int red,
																	final int green,
																	final int blue) {

		final int yiq = ((red * 299) + (green * 587) + (blue * 114)) / 1000;

		if (yiq >= 128) {
			return new org.eclipse.swt.graphics.Color(display, 0, 0, 0);
		} else {
			return new org.eclipse.swt.graphics.Color(display, 0xff, 0xff, 0xff);
		}
	}

	/**
	 * Compute a background color that contrasts with the text color.
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @return Returns white or black that contrasts with the background color.
	 */
	public static Color getContrastColorAWT(final int red, final int green, final int blue, final int alpha) {

		final int yiq = ((red * 299) + (green * 587) + (blue * 114)) / 1000;

		final float newAlpha = (float) alpha / 0xff;

		if (yiq >= 128) {
			return new Color(0, 0, 0, newAlpha);
		} else {
			return new Color(1, 1, 1, newAlpha);
		}
	}
}
