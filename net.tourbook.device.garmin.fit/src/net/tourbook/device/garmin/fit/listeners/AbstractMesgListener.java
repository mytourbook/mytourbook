package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.device.garmin.fit.FitActivityContext;
import net.tourbook.device.garmin.fit.FitActivityContextData;

/**
 * The super class for all message listeners, provides access to the {@link FitActivityContext}.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public abstract class AbstractMesgListener {

	protected final FitActivityContext	context;

	public AbstractMesgListener(FitActivityContext context) {
		this.context = context;
	}

	protected FitActivityContextData getContextData() {
		return context.getContextData();
	}

	protected TourData getTourData() {
		return getContextData().getCurrentTourData();
	}

	protected TimeData getTimeData() {
		return getContextData().getCurrentTimeData();
	}

	protected TourMarker getTourMarker() {
		return getContextData().getCurrentTourMarker();
	}

}
