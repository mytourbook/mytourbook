/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import org.eclipse.swt.graphics.RGB;

public class CSS {

	private static final String	CSS_COLOR_VALUE	= "#%02X%02X%02X;"; //$NON-NLS-1$

	/**
	 * Converts a rgb color into an CSS color value, e.g. #f0aa88;
	 * 
	 * @param color
	 * @return CSS color value.
	 */
	public static String color(final RGB color) {

		return String.format(CSS_COLOR_VALUE, color.red, color.green, color.blue);
	}

}
