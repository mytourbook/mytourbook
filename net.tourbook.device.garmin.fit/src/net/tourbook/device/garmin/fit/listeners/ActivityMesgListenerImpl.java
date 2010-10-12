package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitActivityContext;
import net.tourbook.device.garmin.fit.FitActivityReaderException;

import com.garmin.fit.ActivityMesg;
import com.garmin.fit.ActivityMesgListener;

public class ActivityMesgListenerImpl extends AbstractMesgListener implements ActivityMesgListener {

	public ActivityMesgListenerImpl(FitActivityContext context) {
		super(context);
	}

	@Override
	public void onMesg(ActivityMesg mesg) {
		Integer numSessions = mesg.getNumSessions();
		if (numSessions == null || numSessions < 1) {
			throw new FitActivityReaderException("Invalid number of sessions: "
					+ numSessions
					+ ", expected at least one session.");
		}
	}

}
