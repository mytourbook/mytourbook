/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import java.util.StringTokenizer;

public class StringToArrayConverter {

	public static final String	STRING_SEPARATOR	= ",";	//$NON-NLS-1$

	public static String convertArrayToString(final String[] array) {
		return convertArrayToString(array, STRING_SEPARATOR);
	}

	public static String convertArrayToString(final String[] array, final String separator) {
		final StringBuilder buf = new StringBuilder();
		for (final String element : array) {
			buf.append(element);
			buf.append(separator);
		}
		return buf.toString();

	}

	public static String[] convertStringToArray(final String str) {
		return convertStringToArray(str, STRING_SEPARATOR);
	}

	public static String[] convertStringToArray(final String str, final String separator) {
		final StringTokenizer tok = new StringTokenizer(str, separator);
		final int nTokens = tok.countTokens();
		final String[] res = new String[nTokens];
		for (int i = 0; i < nTokens; i++) {
			res[i] = tok.nextToken();
		}
		return res;
	}

}
