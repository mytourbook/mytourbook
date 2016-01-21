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
package net.tourbook.common.util;

/**
 * String utilities.
 */
public final class StringUtils {

	/**
	 * @param stringArray
	 * @param separator
	 * @return Join a strings with a separator.
	 */
	public static String join(final String[] stringArray, final String separator) {

		final StringBuilder sb = new StringBuilder();

		for (int arrayIndex = 0; arrayIndex < stringArray.length; arrayIndex++) {

			if (arrayIndex > 0) {
				sb.append(separator);
			}

			sb.append(stringArray[arrayIndex]);
		}

		return sb.toString();
	}
}
