/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
	 * Converts an Integer array list into an float array
	 * 
	 * @param list
	 * @return
	 */
	final public static float[] integerToFloat(final ArrayList<Integer> list) {

		final float[] returnArray = new float[list.size()];
		int valueIndex = 0;

		for (final Object value : list.toArray()) {
			returnArray[valueIndex++] = (Float) value;
		}

		return returnArray;
	}

	/**
	 * Converts an Float array list into an float array
	 * 
	 * @param list
	 * @return
	 */
	final public static float[] toFloat(final ArrayList<Float> list) {
		
		final float[] returnArray = new float[list.size()];
		int valueIndex = 0;
		
		for (final Object value : list.toArray()) {
			returnArray[valueIndex++] = (Float) value;
		}
		
		return returnArray;
	}

	/**
	 * converts an Integer array list into an int array
	 * 
	 * @param list
	 * @return
	 */
	final public static int[] toInt(final ArrayList<Integer> list) {

		final int[] returnInt = new int[list.size()];
		int valueIndex = 0;

		for (final Object value : list.toArray()) {
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
	final public static long[] toLong(final ArrayList<Long> list) {

		final long[] returnLong = new long[list.size()];
		int iValue = 0;

		for (final Object value : list.toArray()) {
			if (value == null) {
				returnLong[iValue++] = -1;
			} else {
				returnLong[iValue++] = (Long) value;
			}
		}

		return returnLong;
	}
}
