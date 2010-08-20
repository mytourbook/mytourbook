package net.tourbook.device.garmin.fit;

import java.util.ArrayList;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;

/**
 * Defines operation for handling tour data created during activity reading process.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
interface FitActivityContextDataHandler {

	void handleTour(TourData tourData, ArrayList<TimeData> timeDataList);

}
