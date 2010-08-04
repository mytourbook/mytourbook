package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TourData;
import net.tourbook.device.garmin.fit.FitDataReaderException;

import com.garmin.fit.ActivityMesg;
import com.garmin.fit.ActivityMesgListener;

public class ActivityMesgListenerImpl extends AbstractTourDataMesgListener implements ActivityMesgListener {

    public ActivityMesgListenerImpl(TourData tourData) {
	super(tourData);
    }

    @Override
    public void onMesg(ActivityMesg mesg) {
	Integer numSessions = mesg.getNumSessions();
	if (numSessions == null || numSessions != 1) {
	    throw new FitDataReaderException("Invalid number of sessions: " + numSessions + ", expected: 1");
	}
    }

}