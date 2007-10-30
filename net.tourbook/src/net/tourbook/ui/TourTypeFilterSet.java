package net.tourbook.ui;

import net.tourbook.data.TourType;

public class TourTypeFilterSet {

	private String		fName;

	/**
	 * contains the tour types {@link TourType} for this filter
	 */
	private Object[]	fTourTypes;

	public String getName() {
		return fName;
	}

	/**
	 * @return Returns an array with {@link TourType} objects
	 */
	public Object[] getTourTypes() {
		return fTourTypes;
	}

	public void setName(String name) {
		this.fName = name;
	}

	/**
	 * Set tour types {@link TourType} for this filter
	 * 
	 * @param objects
	 */
	public void setTourTypes(Object[] objects) {
		fTourTypes = objects;
	}
}
