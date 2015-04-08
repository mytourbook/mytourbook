package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.device.garmin.fit.FitContext;

import com.garmin.fit.ActivityMesg;
import com.garmin.fit.ActivityMesgListener;

public class ActivityMesgListenerImpl extends AbstractMesgListener implements ActivityMesgListener {

	public ActivityMesgListenerImpl(final FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(final ActivityMesg mesg) {

		final Integer numSessions = mesg.getNumSessions();

		if (numSessions == null || numSessions < 1) {

			final String message = context.getTourTitle() + " - Invalid number of sessions: " //$NON-NLS-1$
					+ numSessions
					+ ", expected at least one session.";//$NON-NLS-1$

			StatusUtil.logError(message);

			/*
			 * Do not throw an exception because the import can still be successful.
			 */
//			throw new FitDataReaderException(message); //$NON-NLS-1$
		}

	}

}
