/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorUtil {

	/**
	 * @param color
	 * @param alpha
	 *            0xff is opaque, 0 is transparent
	 * @return
	 */
	public static int getARGB(final RGB color, final int alpha) {

		final int graphColor = ((color.blue & 0xFF) << 0) //
				| ((color.green & 0xFF) << 8)
				| ((color.red & 0xFF) << 16)
				| ((alpha) << 24);

		return graphColor;
	}

	public static RGB getComplimentColor(final Display display, final RGB color) {

		// get compliment color
		final int red = (~color.red) & 0xff;
		final int blue = (~color.blue) & 0xff;
		final int green = (~color.green) & 0xff;

		final double darker = 0.8;

		return new RGB(//
				(int) (red * darker),
				(int) (green * darker),
				(int) (blue * darker));
	}

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

	public static org.eclipse.swt.graphics.Color getContrastColor(final Device device, final RGB rgb) {

		return getContrastColor(device, rgb.red, rgb.green, rgb.blue);
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
