package net.tourbook.device.garmin.fit.listeners;

import java.time.ZonedDateTime;

import net.tourbook.common.time.TimeTools;
import net.tourbook.data.TourData;
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

		context.onMesgSession_10_Before();

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

		final ZonedDateTime tourStartTime = TimeTools.getZonedDateTime(startTime.getDate().getTime());

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

		final Integer totalCalories = mesg.getTotalCalories();
		if (totalCalories != null) {

			// convert kcal -> cal
			tourData.setCalories(totalCalories * 1000);
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

		// -----------------------POWER -----------------------

		final Integer avgPower = mesg.getAvgPower();
		if (avgPower != null) {
			tourData.setPower_Avg(avgPower);
		}
		final Integer maxPower = mesg.getMaxPower();
		if (maxPower != null) {
			tourData.setPower_Max(maxPower);
		}

		final Integer normalizedPower = mesg.getNormalizedPower();
		if (normalizedPower != null) {
			tourData.setPower_Normalized(normalizedPower);
		}

		final Integer leftRightBalance = mesg.getLeftRightBalance();
		if (leftRightBalance != null) {
			tourData.setPower_PedalLeftRightBalance(leftRightBalance);
		}

		final Float avgLeftTorqueEffectiveness = mesg.getAvgLeftTorqueEffectiveness();
		if (avgLeftTorqueEffectiveness != null) {
			tourData.setPower_AvgLeftTorqueEffectiveness(avgLeftTorqueEffectiveness);
		}

		final Float avgRightTorqueEffectiveness = mesg.getAvgRightTorqueEffectiveness();
		if (avgRightTorqueEffectiveness != null) {
			tourData.setPower_AvgRightTorqueEffectiveness(avgRightTorqueEffectiveness);
		}

		final Float avgLeftPedalSmoothness = mesg.getAvgLeftPedalSmoothness();
		if (avgLeftPedalSmoothness != null) {
			tourData.setPower_AvgLeftPedalSmoothness(avgLeftPedalSmoothness);
		}

		final Float avgRightPedalSmoothness = mesg.getAvgRightPedalSmoothness();
		if (avgRightPedalSmoothness != null) {
			tourData.setPower_AvgRightPedalSmoothness(avgRightPedalSmoothness);
		}

		final Long totalWork = mesg.getTotalWork();
		if (totalWork != null) {
			tourData.setPower_TotalWork(totalWork);
		}

		final Float trainingStressScore = mesg.getTrainingStressScore();
		if (trainingStressScore != null) {
			tourData.setPower_TrainingStressScore(trainingStressScore);
		}

		final Float intensityFactor = mesg.getIntensityFactor();
		if (intensityFactor != null) {
			tourData.setPower_IntensityFactor(intensityFactor);
		}

		final Integer ftp = mesg.getThresholdPower();
		if (ftp != null) {
			tourData.setPower_FTP(ftp);
		}

		context.onMesgSession_20_After();
	}
}
