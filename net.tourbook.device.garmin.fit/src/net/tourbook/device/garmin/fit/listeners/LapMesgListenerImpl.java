package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitActivityContext;
import net.tourbook.ui.tourChart.ChartLabel;

import com.garmin.fit.LapMesg;
import com.garmin.fit.LapMesgListener;

public class LapMesgListenerImpl extends AbstractMesgListener implements LapMesgListener {

	private int	_lapCounter;

	public LapMesgListenerImpl(final FitActivityContext context) {
		super(context);
	}

	@Override
	public void onMesg(final LapMesg mesg) {
		context.beforeLap();

		final Integer messageIndex = getLapMessageIndex(mesg);

		getTourMarker().setLabel(messageIndex == null ? Integer.toString(++_lapCounter) : messageIndex.toString());
		getTourMarker().setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

		getTourMarker().setSerieIndex(context.getSerieIndex() - 1);

		final Float totalDistance = mesg.getTotalDistance();
		if (totalDistance != null) {
			float lapDistance = context.getLapDistance();
			lapDistance += totalDistance;
			context.setLapDistance(lapDistance);
			getTourMarker().setDistance(lapDistance);
		}

		final Float totalElapsedTime = mesg.getTotalElapsedTime();
		if (totalElapsedTime != null) {
			int lapTime = context.getLapTime();
			lapTime += Math.round(totalElapsedTime);
			context.setLapTime(lapTime);
			getTourMarker().setTime(lapTime);
		}

		context.afterLap();
	}
}
