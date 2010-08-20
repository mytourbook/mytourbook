package net.tourbook.device.garmin.fit.listeners;

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
			throw new FitActivityReaderException("Missing session message index");
		}
		context.setSessionIndex(messageIndex.toString());

		DateTime startTime = mesg.getStartTime();
		if (startTime == null) {
			throw new FitActivityReaderException("Missing session start date");
		}

		org.joda.time.DateTime tourDataStartTime = new org.joda.time.DateTime(startTime.getDate());

		getTourData().setStartSecond((short) tourDataStartTime.getSecondOfMinute());
		getTourData().setStartMinute((short) tourDataStartTime.getMinuteOfHour());
		getTourData().setStartHour((short) tourDataStartTime.getHourOfDay());
		getTourData().setStartDay((short) tourDataStartTime.getDayOfMonth());
		getTourData().setStartMonth((short) tourDataStartTime.getMonthOfYear());
		getTourData().setStartYear((short) tourDataStartTime.getYear());

		getTourData().setWeek(getTourData().getStartYear(), getTourData().getStartMonth(), getTourData().getStartDay());

		Sport sport = mesg.getSport();
		if (sport != null) {
			getTourData().setDeviceModeName(sport.name());
		}

		Short avgHeartRate = mesg.getAvgHeartRate();
		if (avgHeartRate != null) {
			getTourData().setAvgPulse(avgHeartRate);
		}

		Short avgCadence = mesg.getAvgCadence();
		if (avgCadence != null) {
			getTourData().setAvgCadence(avgCadence);
		}

		Float avgSpeed = mesg.getAvgSpeed();
		if (avgSpeed != null) {
			// TODO
		}

		Integer totalCalories = mesg.getTotalCalories();
		if (totalCalories != null) {
			getTourData().setCalories(totalCalories);
		}

		Float totalDistance = mesg.getTotalDistance();
		if (totalDistance != null) {
			getTourData().setTourDistance(Math.round(totalDistance));
		}

		Integer totalAscent = mesg.getTotalAscent();
		if (totalAscent != null) {
			getTourData().setTourAltUp(totalAscent);
		}

		Integer totalDescent = mesg.getTotalDescent();
		if (totalDescent != null) {
			getTourData().setTourAltDown(totalDescent);
		}

		Float totalElapsedTime = mesg.getTotalElapsedTime();
		if (totalElapsedTime != null) {
			getTourData().setTourRecordingTime(Math.round(totalElapsedTime));
		}

		Float totalTimerTime = mesg.getTotalTimerTime();
		if (totalTimerTime != null) {
			getTourData().setTourDrivingTime(Math.round(totalTimerTime));
		}

		context.afterSession();
	}
}
