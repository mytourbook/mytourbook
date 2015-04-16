package net.tourbook.device.garmin.fit;

import java.util.List;

import net.tourbook.data.GearData;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;

/**
 * Defines operation for handling tour data created during activity reading process.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
interface FitContextDataHandler {

	void finalizeTour(TourData tourData, List<TimeData> allTimeData, List<TourMarker> tourMarkers, List<GearData> gears);

}
