package net.tourbook.ui.views.tourMap;

public class TourPropertyCompareTourChanged {

	long	compareId;

	int		startIndex;
	int		endIndex;

	float	speed;

	boolean	isDataSaved;

	Object	comparedTourItem;

	public TourPropertyCompareTourChanged(final long compareId, final int startIndex,
			final int endIndex, final float speed, final boolean isDataSaved,
			Object comparedTourItem) {

		this.compareId = compareId;

		this.startIndex = startIndex;
		this.endIndex = endIndex;

		this.speed = speed;
		this.isDataSaved = isDataSaved;

		this.comparedTourItem = comparedTourItem;
	}

}
