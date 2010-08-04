package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TourData;

import com.garmin.fit.LapMesg;
import com.garmin.fit.LapMesgListener;

public class LapMesgListenerImpl extends AbstractTourDataMesgListener implements LapMesgListener {

    public LapMesgListenerImpl(TourData tourData) {
	super(tourData);
    }

    @Override
    public void onMesg(LapMesg mesg) {
    }
}