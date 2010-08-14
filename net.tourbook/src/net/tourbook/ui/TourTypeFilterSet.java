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

import java.util.Arrays;

import net.tourbook.data.TourType;

public class TourTypeFilterSet {

	private String		_name;

	/**
	 * contains the tour types {@link TourType} for this filter
	 */
	private Object[]	_tourTypes;

	public String getName() {
		return _name;
	}

	/**
	 * @return Returns an array with {@link TourType} objects
	 */
	public Object[] getTourTypes() {
		return _tourTypes;
	}

	public void setName(final String name) {
		_name = name;
	}

	/**
	 * Set tour types {@link TourType} for this filter
	 * 
	 * @param objects
	 */
	public void setTourTypes(final Object[] objects) {
		_tourTypes = objects;
	}

	@Override
	public String toString() {
		return "TourTypeFilterSet [Name=" + _name + ", TourTypes=" + Arrays.toString(_tourTypes) + "]";
	}
}
