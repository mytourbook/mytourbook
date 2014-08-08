package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitContext;
import net.tourbook.device.garmin.fit.FitDataReaderException;

import com.garmin.fit.ActivityMesg;
import com.garmin.fit.ActivityMesgListener;

public class ActivityMesgListenerImpl extends AbstractMesgListener implements ActivityMesgListener {

	public ActivityMesgListenerImpl(FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(ActivityMesg mesg) {
		Integer numSessions = mesg.getNumSessions();
		if (numSessions == null || numSessions < 1) {
			throw new FitDataReaderException("Invalid number of sessions: " //$NON-NLS-1$
					+ numSessions
					+ ", expected at least one session."); //$NON-NLS-1$
		}
	}

}
