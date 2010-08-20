package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.device.garmin.fit.FitActivityContext;

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

	protected TourData getTourData() {
		return context.getTourData();
	}

	protected TimeData getTimeData() {
		return context.getTimeData();
	}

}
