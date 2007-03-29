/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.util;

import java.util.StringTokenizer;

public class StringToArrayConverter {

	public static final String	STRING_SEPARATOR	= ",";	//$NON-NLS-1$

	public static String[] convertStringToArray(String str) {
		StringTokenizer tok = new StringTokenizer(str, STRING_SEPARATOR);
		int nTokens = tok.countTokens();
		String[] res = new String[nTokens];
		for (int i = 0; i < nTokens; i++) {
			res[i] = tok.nextToken();
		}
		return res;
	}

	public static String convertArrayToString(Object[] array) {
		StringBuffer buf = new StringBuffer();
		for (int arrayIndex = 0; arrayIndex < array.length; arrayIndex++) {
			buf.append(array[arrayIndex]);
			buf.append(STRING_SEPARATOR);
		}
		return buf.toString();
	}
	
}
