/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.chart;

import java.util.HashMap;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorCache {

	private Display					_display;

	private HashMap<String, Color>	_colors	= new HashMap<String, Color>();

	public ColorCache() {
		_display = Display.getCurrent();
	}

	/**
	 * Dispose all colors in the color cache
	 */
	public void dispose() {
		for (final Color color : _colors.values()) {
			(color).dispose();
		}
		_colors.clear();
	}

	/**
	 * @param colorKey
	 * @return Returns the color for the <code>colorKey</code> from the color cache or
	 *         <code>null</code> when the color is not available
	 */
	public Color get(final String colorKey) {
		return _colors.get(colorKey);
	}

	/**
	 * @param rgb
	 *            RGB value
	 * @return Returns the color from the color cache with the RGB value as the key.
	 *         <p>
	 *         The color must not be disposed this is done when the cache is disposed.
	 */
	public Color getColor(final RGB rgb) {

// !!! this is a performance bottleneck !!!
//		final String colorKey = rgb.toString();

		final String colorKey = Integer.toString(rgb.hashCode());
		final Color color = _colors.get(colorKey);

		if (color == null) {
			return getColor(colorKey, rgb);
		} else {
			return color;
		}
	}

	/**
	 * Creates a color in the color cache when the color is not yet created.
	 * 
	 * @param colorKey
	 * @param rgb
	 * @return Returns the created color
	 */
	public Color getColor(final String colorKey, final RGB rgb) {

		// check if key already exists
		if (_colors.containsKey(colorKey)) {
			return _colors.get(colorKey);
		}

		final Color color = new Color(_display, rgb);

		_colors.put(colorKey, color);

		return color;
	}

	/**
	 * Replace a color in the color cache.
	 * 
	 * @param colorKey
	 * @param rgb
	 * @return Returns the created color
	 */
	public Color replaceColor(final String colorKey, final RGB rgb) {

		// check if key already exists
		final Color oldColor = _colors.get(colorKey);
		if (oldColor != null) {
			oldColor.dispose();
		}

		final Color color = new Color(_display, rgb);

		_colors.put(colorKey, color);
		
		return color;
	}

	/**
	 * Creates a color in the color cache when the color is not yet created.
	 * 
	 * @param colorKey
	 * @param rgb
	 */
	public void setColor(final String colorKey, final RGB rgb) {

		// check if key already exists
		if (_colors.containsKey(colorKey)) {
			return;
		}

		final Color color = new Color(_display, rgb);

		_colors.put(colorKey, color);
	}

}
