package net.tourbook.device.garmin.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.chart.ChartLabel;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;

/**
 * Wrapper for tour data used by {@link FitActivityContext}.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class FitActivityContextData {

	private final List<ContextTourData>							contextTourDataList		= new ArrayList<ContextTourData>();

	private final Map<ContextTourData, List<ContextTimeData>>	contextTimeDataMap		= new HashMap<ContextTourData, List<ContextTimeData>>();

	private final Map<ContextTourData, List<ContextTourMarker>>	contextTourMarkerMap	= new HashMap<FitActivityContextData.ContextTourData, List<ContextTourMarker>>();

	private ContextTourData										currentContextTourData;

	private ContextTimeData										currentContextTimeData;

	private ContextTourMarker									currentContextTourMarker;

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

	public void initializeTourMarker() {
		initializeTourData();

		List<ContextTourMarker> currentContextTourMarkerList = contextTourMarkerMap.get(currentContextTourData);
		if (currentContextTourMarkerList == null) {
			currentContextTourMarkerList = new ArrayList<ContextTourMarker>();
			contextTourMarkerMap.put(currentContextTourData, currentContextTourMarkerList);
		}

		currentContextTourMarker = new ContextTourMarker();
		currentContextTourMarker.data = new TourMarker(getCurrentTourData(), ChartLabel.MARKER_TYPE_DEVICE);

		currentContextTourMarkerList.add(currentContextTourMarker);
	}

	public void finalizeTourMarker() {
		currentContextTourMarker = null;
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

	public TourMarker getCurrentTourMarker() {
		if (currentContextTourMarker == null) {
			throw new IllegalArgumentException("Tour marker is not initialized");
		}

		return currentContextTourMarker.data;
	}

	public void processData(FitActivityContextDataHandler handler) {
		for (ContextTourData contextTourData : contextTourDataList) {
			List<ContextTimeData> contextTimeDataList = contextTimeDataMap.get(contextTourData);

			ArrayList<TimeData> timeDataList = new ArrayList<TimeData>(contextTimeDataList.size());
			for (ContextTimeData contextTimeData : contextTimeDataList) {
				timeDataList.add(contextTimeData.data);
			}

			List<ContextTourMarker> contextTourMarkerList = contextTourMarkerMap.get(contextTourData);
			Set<TourMarker> tourMarkerSet = new HashSet<TourMarker>(contextTourMarkerList.size());
			for (ContextTourMarker contextTourMarker : contextTourMarkerList) {
				tourMarkerSet.add(contextTourMarker.data);
			}

			handler.handleTour(contextTourData.data, timeDataList, tourMarkerSet);
		}
	}

	private class ContextTourData {
		private TourData	data;
	}

	private class ContextTimeData {
		private TimeData	data;
	}

	private class ContextTourMarker {
		private TourMarker	data;
	}

}
