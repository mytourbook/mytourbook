package net.tourbook.ui;

import net.tourbook.data.TourType;

/**
 * Contains a tour type which is not stored in the database
 */
public class TourTypeFilter {

	/**
	 * this is a system filter like all tour types or no tour types
	 */
	public static final int	FILTER_TYPE_SYSTEM				= 1;

	/**
	 * filter contains one tour type which is stored in the database
	 */
	public static final int	FILTER_TYPE_DB					= 2;

	/**
	 * this is a custom filter which contains several tour types
	 */
	public static final int	FILTER_TYPE_CUSTOM				= 3;

	public static final int	SYSTEM_FILTER_ID_ALL			= 1;
	public static final int	SYSTEM_FILTER_ID_NOT_DEFINED	= 2;

	private String			fFilterName;

	private int				fFilterType;

	private int				fSystemFilterId;
//	private long[]			fFilterIds;

	/**
	 * contains the tour type from the database when {@link TourTypeFilter#getFilterType()} is
	 * {@link TourTypeFilter#FILTER_TYPE_DB}
	 */
	private TourType		fTourType;

//	/**
//	 * is <code>true</code> when this filter is copied into the out list
//	 */
//	private boolean			fIsInOutList;

	public TourTypeFilter(int filterType, int filterId, String filterName) {
		fFilterType = filterType;
		fSystemFilterId = filterId;
		fFilterName = filterName;
	}

	public TourTypeFilter(int filterType, TourType tourType) {

		fFilterType = filterType;
		fTourType = tourType;

		fFilterName = fTourType.getName();
	}

	public String getFilterName() {

		String filterName;

		switch (fFilterType) {
		case FILTER_TYPE_SYSTEM:
			filterName = "- " + fFilterName + " -";
			break;

		case FILTER_TYPE_DB:
			filterName = fFilterName;
			break;

		case FILTER_TYPE_CUSTOM:
			filterName = "# :" + fFilterName;
			break;

		default:
			filterName = "?";
			break;
		}

		return filterName;
	}

	public int getFilterType() {
		return fFilterType;
	}

	public int getSystemFilterId() {
		return fSystemFilterId;
	}

	/**
	 * @return Returns the tour type from the database when {@link TourTypeFilter#getFilterType()}
	 *         is {@link TourTypeFilter#FILTER_TYPE_DB}
	 */
	public TourType getTourType() {
		return fTourType;
	}

}
