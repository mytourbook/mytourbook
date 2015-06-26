package net.tourbook.device.garmin.fit.listeners;

import java.util.Date;

import net.tourbook.data.TourMarker;
import net.tourbook.device.garmin.fit.FitContext;

import com.garmin.fit.DateTime;
import com.garmin.fit.LapMesg;
import com.garmin.fit.LapMesgListener;

/**
 * A {@link TourMarker} is set for each lap.
 */
public class LapMesgListenerImpl extends AbstractMesgListener implements LapMesgListener {

	private int	_lapCounter;

	public LapMesgListenerImpl(final FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(final LapMesg lapMesg) {

		context.mesgLap_10_Before();

		setMarker(lapMesg);

		context.mesgLap_20_After();
	}

	private void setMarker(final LapMesg lapMesg) {

		final Integer messageIndex = getLapMessageIndex(lapMesg);
		final TourMarker tourMarker = getTourMarker();

		tourMarker.setLabel(messageIndex == null //
				? Integer.toString(++_lapCounter)
				: messageIndex.toString());

		float lapDistance = -1;
		final Float totalDistance = lapMesg.getTotalDistance();
		if (totalDistance != null) {

			lapDistance = context.getLapDistance();
			lapDistance += totalDistance;

			context.setLapDistance(lapDistance);
			tourMarker.setDistance(lapDistance);
		}

		int lapTime = -1;
		final Float totalElapsedTime = lapMesg.getTotalElapsedTime();
		if (totalElapsedTime != null) {

			lapTime = context.getLapTime();
//			lapTime += Math.round(totalElapsedTime);
			lapTime += totalElapsedTime;

			context.setLapTime(lapTime);

			// the correct absolute time will be set later
			tourMarker.setTime(lapTime, Long.MIN_VALUE);
		}

		/*
		 * Set lap time, later the time slice position (serie index) will be set.
		 */
		final DateTime garminTime = lapMesg.getTimestamp();
		final Date linuxTime = garminTime.getDate();

		tourMarker.setDeviceLapTime(linuxTime.getTime());
	}
}
