package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TourMarker;
import net.tourbook.device.garmin.fit.FitContext;

import com.garmin.fit.DateTime;
import com.garmin.fit.LapMesg;
import com.garmin.fit.LapMesgListener;

public class LapMesgListenerImpl extends AbstractMesgListener implements LapMesgListener {

	private int	_lapCounter;

	public LapMesgListenerImpl(final FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(final LapMesg lapMesg) {

		context.mesgLap_10_Before();

		onMesg_SetupMarker(lapMesg);

		context.mesgLap_20_After();
	}

	private void onMesg_SetupMarker(final LapMesg lapMesg) {

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
			lapTime += Math.round(totalElapsedTime);

			context.setLapTime(lapTime);

			// the correct absolute time will be set later
			tourMarker.setTime(lapTime, Long.MIN_VALUE);
		}

		/*
		 * Set time slice position
		 */
		final DateTime timestamp = lapMesg.getTimestamp();
		final long absoluteTime = timestamp.getTimestamp();

		int serieIndex = context.getSerieIndex(absoluteTime, lapDistance) - 1;

		// check bounds
		if (serieIndex < 0) {
			serieIndex = 0;
		}
		tourMarker.setSerieIndex(serieIndex);
	}
}
