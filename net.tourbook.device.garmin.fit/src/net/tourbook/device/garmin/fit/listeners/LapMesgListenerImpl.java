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

		final Integer messageIndex = getLapMessageIndex(lapMesg);
		final TourMarker tourMarker = getTourMarker();

		tourMarker.setLabel(messageIndex == null ? Integer.toString(++_lapCounter) : messageIndex.toString());

		final Float totalDistance = lapMesg.getTotalDistance();
		float lapDistance = -1;
		if (totalDistance != null) {

			lapDistance = context.getLapDistance();
			lapDistance += totalDistance;

			context.setLapDistance(lapDistance);
			tourMarker.setDistance(lapDistance);
		}

		final Float totalElapsedTime = lapMesg.getTotalElapsedTime();
		int lapTime = -1;
		if (totalElapsedTime != null) {

			lapTime = context.getLapTime();
			lapTime += Math.round(totalElapsedTime);

			context.setLapTime(lapTime);
			tourMarker.setTime(lapTime);
		}

		// set time slices position
		final DateTime timestamp = lapMesg.getTimestamp();
		final long absoluteTime = timestamp.getTimestamp();

		final int serieIndex = context.getSerieIndex(absoluteTime, lapDistance) - 1;
		tourMarker.setSerieIndex(serieIndex);

		context.mesgLap_20_After();
	}
}
