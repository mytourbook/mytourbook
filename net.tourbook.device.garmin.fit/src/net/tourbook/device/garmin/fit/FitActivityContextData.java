package net.tourbook.device.garmin.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.ui.tourChart.ChartLabel;

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

	private class ContextTimeData {
		private TimeData	data;
	}

	private class ContextTourData {
		private TourData	data;
	}

	private class ContextTourMarker {
		private TourMarker	data;
	}

	public void finalizeTimeData() {
		currentContextTimeData = null;
	}

	public void finalizeTourData() {
		finalizeTimeData();
		currentContextTourData = null;
	}

	public void finalizeTourMarker() {
		currentContextTourMarker = null;
	}

	public TimeData getCurrentTimeData() {
		if (currentContextTimeData == null) {
			throw new IllegalArgumentException("Time data is not initialized"); //$NON-NLS-1$
		}

		return currentContextTimeData.data;

	}

	public TourData getCurrentTourData() {
		if (currentContextTourData == null) {
			throw new IllegalArgumentException("Tour data is not initialized"); //$NON-NLS-1$
		}

		return currentContextTourData.data;
	}

	public TourMarker getCurrentTourMarker() {
		if (currentContextTourMarker == null) {
			throw new IllegalArgumentException("Tour marker is not initialized"); //$NON-NLS-1$
		}

		return currentContextTourMarker.data;
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

	public void initializeTourData() {
		
		if (currentContextTourData == null) {
			currentContextTourData = new ContextTourData();
			currentContextTourData.data = new TourData();

			contextTourDataList.add(currentContextTourData);
		}
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

	public void processData(final FitActivityContextDataHandler handler) {
		for (final ContextTourData contextTourData : contextTourDataList) {
			final List<ContextTimeData> contextTimeDataList = contextTimeDataMap.get(contextTourData);

			final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>(contextTimeDataList.size());
			for (final ContextTimeData contextTimeData : contextTimeDataList) {
				timeDataList.add(contextTimeData.data);
			}

			final List<ContextTourMarker> contextTourMarkerList = contextTourMarkerMap.get(contextTourData);
			final Set<TourMarker> tourMarkerSet = new HashSet<TourMarker>(contextTourMarkerList.size());
			for (final ContextTourMarker contextTourMarker : contextTourMarkerList) {
				tourMarkerSet.add(contextTourMarker.data);
			}

			handler.handleTour(contextTourData.data, timeDataList, tourMarkerSet);
		}
	}

}
