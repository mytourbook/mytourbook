package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.chart.ChartLabel;
import net.tourbook.device.garmin.fit.DataConverters;
import net.tourbook.device.garmin.fit.FitActivityContext;
import net.tourbook.device.garmin.fit.FitActivityReaderException;

import com.garmin.fit.LapMesg;
import com.garmin.fit.LapMesgListener;

public class LapMesgListenerImpl extends AbstractMesgListener implements LapMesgListener {

	public LapMesgListenerImpl(FitActivityContext context) {
		super(context);
	}

	@Override
	public void onMesg(LapMesg mesg) {
		context.beforeLap();

		Integer messageIndex = mesg.getMessageIndex();
		if (messageIndex == null) {
			throw new FitActivityReaderException("Lap message index is missing");
		}
		getTourMarker().setLabel(messageIndex.toString());
		getTourMarker().setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

		getTourMarker().setSerieIndex(context.getSerieIndex() - 1);

		Float totalDistance = mesg.getTotalDistance();
		if (totalDistance != null) {
			int lapDistance = context.getLapDistance();
			lapDistance += DataConverters.convertDistance(totalDistance);
			context.setLapDistance(lapDistance);
			getTourMarker().setDistance(lapDistance);
		}

		Float totalElapsedTime = mesg.getTotalElapsedTime();
		if (totalElapsedTime != null) {
			int lapTime = context.getLapTime();
			lapTime += Math.round(totalElapsedTime);
			context.setLapTime(lapTime);
			getTourMarker().setTime(lapTime);
		}

		context.afterLap();
	}
}
