package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitContext;
import net.tourbook.tour.TourLogManager;

import com.garmin.fit.ActivityMesg;
import com.garmin.fit.ActivityMesgListener;

public class Activity_MesgListenerImpl extends AbstractMesgListener implements ActivityMesgListener {

	public Activity_MesgListenerImpl(final FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(final ActivityMesg mesg) {

		final Integer numSessions = mesg.getNumSessions();

		if (numSessions == null || numSessions < 1) {

			final String message = "%s - Invalid number of sessions: %d, expected at least one session."; //$NON-NLS-1$

			TourLogManager.logSubInfo(String.format(
					message,
					context.getTourTitle(),
					numSessions));

			/*
			 * Do not throw an exception because the import can still be successful.
			 */
//			throw new FitDataReaderException(message); //$NON-NLS-1$
		}

	}

}
