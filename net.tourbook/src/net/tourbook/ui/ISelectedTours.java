package net.tourbook.ui;

import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.tour.TourEditor;

public interface ISelectedTours {

	/**
	 * Returns the tours which are selected or <code>null</code> when no tour is selected
	 */
	ArrayList<TourData> getSelectedTours();

	/**
	 * @return Returns <code>true</code> when {@link ISelectedTours#getSelectedTours()} is created
	 *         in a {@link TourEditor}
	 */
	boolean isFromTourEditor();

}
