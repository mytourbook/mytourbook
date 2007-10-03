/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

public class ArrayListToArray {

	/**
	 * converts an Integer array list into an int array
	 * 
	 * @param list
	 * @return
	 */
	final public static int[] toInt(ArrayList<Integer> list) {

		int[] returnInt = new int[list.size()];

		Object[] valueList = list.toArray();
		int valueIndex = 0;

		for (Object value : valueList) {
			returnInt[valueIndex++] = (Integer) value;
		}

		return returnInt;
	}

	/**
	 * converts an Long array list into a long array
	 * 
	 * @param list
	 * @return
	 */
	final public static long[] toLong(ArrayList<Long> list) {

		long[] returnLong = new long[list.size()];

		Object[] valueList = list.toArray();
		int iValue = 0;

		for (Object value : valueList) {
			if (value == null) {
				returnLong[iValue++] = -1;
			} else {
				returnLong[iValue++] = (Long) value;
			}
		}

		return returnLong;
	}
}
