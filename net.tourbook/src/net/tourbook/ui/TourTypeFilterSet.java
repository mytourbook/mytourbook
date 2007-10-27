package net.tourbook.ui;

public class TourTypeFilterSet {

//	private long		fId;
	private String		fName;

	/**
	 * contains the tour types for this filter set
	 */
	private Object[]	fTourTypes;

//	public TourTypeFilterSet(long id) {
//		this.fId = id;
//	}

//	long getId() {
//		return fId;
//	}

	public String getName() {
		return fName;
	}

	public Object[] getTourTypes() {
		return fTourTypes;
	}

	public void setName(String name) {
		this.fName = name;
	}

	public void setTourTypes(Object[] objects) {
		fTourTypes = objects;
	}
}
