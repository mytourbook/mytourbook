package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TourData;
import net.tourbook.device.garmin.fit.DataConverters;
import net.tourbook.device.garmin.fit.FitContext;
import net.tourbook.device.garmin.fit.FitDataReaderException;

import com.garmin.fit.DateTime;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SessionMesgListener;
import com.garmin.fit.Sport;

public class SessionMesgListenerImpl extends AbstractMesgListener implements SessionMesgListener {

	public SessionMesgListenerImpl(final FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(final SessionMesg mesg) {

		context.mesgSession_10_Before();

		final Integer messageIndex = getMessageIndex(mesg);
		context.setSessionIndex(messageIndex.toString());

		final DateTime startTime = mesg.getStartTime();
		if (startTime == null) {
			throw new FitDataReaderException("Missing session start date"); //$NON-NLS-1$
		}

		final TourData tourData = getTourData();

// since FIT SDK > 12 the tour start time is different with the records, therefore the tour start time is set later
//
// !!!!
//      This problem is corrected in FIT SDK 14.10 but it took me several days to investigate it
//		and then came the idea to check for a new FIT SDK which solved this problem.
// !!!!
		final org.joda.time.DateTime tourStartTime = new org.joda.time.DateTime(startTime.getDate());
		context.setSessionStartTime(tourStartTime);
		tourData.setTourStartTime(tourStartTime);

		final Sport sport = mesg.getSport();
		if (sport != null) {
			tourData.setDeviceModeName(sport.name());
		}

		final Short avgHeartRate = mesg.getAvgHeartRate();
		if (avgHeartRate != null) {
			tourData.setAvgPulse(avgHeartRate);
		}

		final Short avgCadence = mesg.getAvgCadence();
		if (avgCadence != null) {
			tourData.setAvgCadence(avgCadence);
		}

		final Float avgSpeed = mesg.getAvgSpeed();
		if (avgSpeed != null) {
			tourData.setDeviceAvgSpeed(DataConverters.convertSpeed(avgSpeed));
		}

		final Integer totalCalories = mesg.getTotalCalories();
		if (totalCalories != null) {
			tourData.setCalories(totalCalories);
		}

		final Float totalDistance = mesg.getTotalDistance();
		if (totalDistance != null) {
			tourData.setTourDistance(Math.round(totalDistance));
		}

		final Integer totalAscent = mesg.getTotalAscent();
		if (totalAscent != null) {
			tourData.setTourAltUp(totalAscent);
		}

		final Integer totalDescent = mesg.getTotalDescent();
		if (totalDescent != null) {
			tourData.setTourAltDown(totalDescent);
		}

		final Float totalElapsedTime = mesg.getTotalElapsedTime();
		if (totalElapsedTime != null) {
			tourData.setTourRecordingTime(Math.round(totalElapsedTime));
		}

		final Float totalTimerTime = mesg.getTotalTimerTime();
		if (totalTimerTime != null) {
			tourData.setTourDrivingTime(Math.round(totalTimerTime));
		}

		context.mesgSession_20_After();
	}
}
