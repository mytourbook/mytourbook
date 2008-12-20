package net.tourbook.ui;

import java.util.ArrayList;

import net.tourbook.data.TourData;

public interface ITourProvider2 extends ITourProvider {

	/**
	 * @param modifiedTours
	 * @return a list with {@link TourData} which have been modified
	 */
	void toursAreModified(ArrayList<TourData> modifiedTours);

}
