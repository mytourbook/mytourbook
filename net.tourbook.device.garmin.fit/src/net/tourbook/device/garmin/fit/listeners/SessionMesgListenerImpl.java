package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TourData;
import net.tourbook.device.garmin.fit.DataConverters;
import net.tourbook.device.garmin.fit.FitActivityContext;
import net.tourbook.device.garmin.fit.FitActivityReaderException;

import com.garmin.fit.DateTime;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SessionMesgListener;
import com.garmin.fit.Sport;

public class SessionMesgListenerImpl extends AbstractMesgListener implements SessionMesgListener {

	public SessionMesgListenerImpl(final FitActivityContext context) {
		super(context);
	}

	@Override
	public void onMesg(final SessionMesg mesg) {
		context.beforeSession();

		final Integer messageIndex = mesg.getMessageIndex();
		if (messageIndex == null) {
			throw new FitActivityReaderException("Missing session message index"); //$NON-NLS-1$
		}
		context.setSessionIndex(messageIndex.toString());

		final DateTime startTime = mesg.getStartTime();
		if (startTime == null) {
			throw new FitActivityReaderException("Missing session start date"); //$NON-NLS-1$
		}

		final TourData tourData = getTourData();

		tourData.setTourStartTime(new org.joda.time.DateTime(startTime.getDate()));

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

		context.afterSession();
	}
}
