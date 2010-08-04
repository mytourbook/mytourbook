package net.tourbook.device.garmin.fit.listeners;

import java.util.Calendar;

import net.tourbook.data.TourData;
import net.tourbook.device.garmin.fit.FitDataReaderException;

import com.garmin.fit.DateTime;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SessionMesgListener;
import com.garmin.fit.Sport;

public class SessionMesgListenerImpl extends AbstractTourDataMesgListener implements SessionMesgListener {

    public SessionMesgListenerImpl(TourData tourData) {
	super(tourData);
    }

    @Override
    public void onMesg(SessionMesg mesg) {
	DateTime startTime = mesg.getStartTime();
	if (startTime == null) {
	    throw new FitDataReaderException("Missing session start date");
	}

	// TODO: jodatime
	Calendar calendar = Calendar.getInstance();

	calendar.setTime(startTime.getDate());

	tourData.setStartSecond(calendar.get(Calendar.SECOND));
	tourData.setStartMinute((short) calendar.get(Calendar.MINUTE));
	tourData.setStartHour((short) calendar.get(Calendar.HOUR_OF_DAY));
	tourData.setStartDay((short) calendar.get(Calendar.DAY_OF_MONTH));
	tourData.setStartMonth((short) (calendar.get(Calendar.MONTH) + 1));
	tourData.setStartYear((short) calendar.get(Calendar.YEAR));

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
	    // TODO
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

    }
}