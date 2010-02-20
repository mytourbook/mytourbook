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
package net.tourbook.ui;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class ColorCacheInt {

	private Display					_display;

	private HashMap<Integer, Color>	_colors	= new HashMap<Integer, Color>();

	public ColorCacheInt() {
		_display = Display.getCurrent();
	}

	/**
	 * Dispose all colors in the color cache
	 */
	public void dispose() {

		for (final Iterator<Color> color = _colors.values().iterator(); color.hasNext();) {
			(color.next()).dispose();
		}

		_colors.clear();
	}

	/**
	 * @param colorValue
	 * @return Returns the color for the <code>colorValue</code> from the color cache, color is
	 *         created when it is not available
	 */
	public Color get(final int colorValue) {
 
//		Color color = _colors.get(new Integer(colorValue));
		Color color = _colors.get(colorValue);
		if (color != null) {
			return color;
		}

		final int red = (colorValue & 0xFF) >>> 0;
		final int green = (colorValue & 0xFF00) >>> 8;
		final int blue = (colorValue & 0xFF0000) >>> 16;

		color = new Color(_display, red, green, blue);

		_colors.put(colorValue, color);

		return color;
	}

}
