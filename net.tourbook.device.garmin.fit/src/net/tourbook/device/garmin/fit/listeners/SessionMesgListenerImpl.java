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

	public SessionMesgListenerImpl(FitActivityContext context) {
		super(context);
	}

	@Override
	public void onMesg(SessionMesg mesg) {
		context.beforeSession();

		Integer messageIndex = mesg.getMessageIndex();
		if (messageIndex == null) {
			throw new FitActivityReaderException("Missing session message index"); //$NON-NLS-1$
		}
		context.setSessionIndex(messageIndex.toString());

		DateTime startTime = mesg.getStartTime();
		if (startTime == null) {
			throw new FitActivityReaderException("Missing session start date"); //$NON-NLS-1$
		}

		org.joda.time.DateTime tourDataStartTime = new org.joda.time.DateTime(startTime.getDate());

		TourData tourData = getTourData();
		tourData.setStartSecond((short) tourDataStartTime.getSecondOfMinute());
		tourData.setStartMinute((short) tourDataStartTime.getMinuteOfHour());
		tourData.setStartHour((short) tourDataStartTime.getHourOfDay());
		tourData.setStartDay((short) tourDataStartTime.getDayOfMonth());
		tourData.setStartMonth((short) tourDataStartTime.getMonthOfYear());
		tourData.setStartYear((short) tourDataStartTime.getYear());

		tourData.setWeek(tourData.getStartYear(), tourData.getStartMonth(), tourData.getStartDay());

		Sport sport = mesg.getSport();
		if (sport != null) {
			tourData.setDeviceModeName(sport.name());
		}

		Short avgHeartRate = mesg.getAvgHeartRate();
		if (avgHeartRate != null) {
			tourData.setAvgPulse(avgHeartRate);
		}

		Short avgCadence = mesg.getAvgCadence();
		if (avgCadence != null) {
			tourData.setAvgCadence(avgCadence);
		}

		Float avgSpeed = mesg.getAvgSpeed();
		if (avgSpeed != null) {
			tourData.setDeviceAvgSpeed(DataConverters.convertSpeed(avgSpeed));
		}

		Integer totalCalories = mesg.getTotalCalories();
		if (totalCalories != null) {
			tourData.setCalories(totalCalories);
		}

		Float totalDistance = mesg.getTotalDistance();
		if (totalDistance != null) {
			tourData.setTourDistance(Math.round(totalDistance));
		}

		Integer totalAscent = mesg.getTotalAscent();
		if (totalAscent != null) {
			tourData.setTourAltUp(totalAscent);
		}

		Integer totalDescent = mesg.getTotalDescent();
		if (totalDescent != null) {
			tourData.setTourAltDown(totalDescent);
		}

		Float totalElapsedTime = mesg.getTotalElapsedTime();
		if (totalElapsedTime != null) {
			tourData.setTourRecordingTime(Math.round(totalElapsedTime));
		}

		Float totalTimerTime = mesg.getTotalTimerTime();
		if (totalTimerTime != null) {
			tourData.setTourDrivingTime(Math.round(totalTimerTime));
		}

		context.afterSession();
	}
}
