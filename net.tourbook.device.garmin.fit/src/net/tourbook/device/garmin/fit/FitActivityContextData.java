package net.tourbook.device.garmin.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;

/**
 * Wrapper for tour data used by {@link FitActivityContext}.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class FitActivityContextData {

	private final List<ContextTourData>							contextTourDataList	= new ArrayList<ContextTourData>();

	private final Map<ContextTourData, List<ContextTimeData>>	contextTimeDataMap	= new HashMap<ContextTourData, List<ContextTimeData>>();

	private ContextTourData										currentContextTourData;

	private ContextTimeData										currentContextTimeData;

	public void initializeTimeData() {
		initializeTourData();

		List<ContextTimeData> currentContextTimeDataList = contextTimeDataMap.get(currentContextTourData);
		if (currentContextTimeDataList == null) {
			currentContextTimeDataList = new ArrayList<ContextTimeData>();
			contextTimeDataMap.put(currentContextTourData, currentContextTimeDataList);
		}

		currentContextTimeData = new ContextTimeData();
		currentContextTimeData.data = new TimeData();

		currentContextTimeDataList.add(currentContextTimeData);
	}

	public void finalizeTimeData() {
		currentContextTimeData = null;
	}

	public void initializeTourData() {
		if (currentContextTourData == null) {
			currentContextTourData = new ContextTourData();
			currentContextTourData.data = new TourData();

			contextTourDataList.add(currentContextTourData);
		}
	}

	public void finalizeTourData() {
		finalizeTimeData();
		currentContextTourData = null;
	}

	public TourData getCurrentTourData() {
		if (currentContextTourData == null) {
			throw new IllegalArgumentException("Tour data is not initialized");
		}

		return currentContextTourData.data;
	}

	public TimeData getCurrentTimeData() {
		if (currentContextTimeData == null) {
			throw new IllegalArgumentException("Time data is not initialized");
		}

		return currentContextTimeData.data;

	}

	public void processData(FitActivityContextDataHandler handler) {
		for (ContextTourData contextTourData : contextTourDataList) {
			List<ContextTimeData> contextTimeDataList = contextTimeDataMap.get(contextTourData);

			ArrayList<TimeData> timeDataList = new ArrayList<TimeData>(contextTimeDataList.size());
			for (ContextTimeData contextTimeData : contextTimeDataList) {
				timeDataList.add(contextTimeData.data);
			}

			handler.handleTour(contextTourData.data, timeDataList);
		}
	}

	private class ContextTourData {
		private TourData	data;
	}

	private class ContextTimeData {
		private TimeData	data;
	}

}
