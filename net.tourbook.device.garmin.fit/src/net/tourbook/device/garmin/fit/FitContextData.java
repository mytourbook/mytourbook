package net.tourbook.device.garmin.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.data.GearData;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.ui.tourChart.ChartLabel;

import com.garmin.fit.EventMesg;

/**
 * Wrapper for tour data used by {@link FitContext}.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class FitContextData {

	private final List<ContextTourData>							_allTourData	= new ArrayList<ContextTourData>();
	private final Map<ContextTourData, List<ContextTimeData>>	_allTimeData	= new HashMap<ContextTourData, List<ContextTimeData>>();
	private final Map<ContextTourData, List<ContextTourMarker>>	_allTourMarker	= new HashMap<FitContextData.ContextTourData, List<ContextTourMarker>>();
	private final Map<ContextTourData, List<GearData>>			_allGearData	= new HashMap<FitContextData.ContextTourData, List<GearData>>();

	private ContextTourData										_currentTourDataCtx;
	private ContextTimeData										_currentTimeDataCtx;
	private ContextTourMarker									_currentTourMarkerCtx;

	private List<ContextTimeData>								_currentContextTimeDataList;

	class ContextTimeData {

		private TimeData	__timeData;

		TimeData getTimeData() {
			return __timeData;
		}

		@Override
		public String toString() {
			return "ContextTimeData [__data=" + __timeData + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private class ContextTourData {

		private TourData	__tourData;

		@Override
		public String toString() {
			return "ContextTourData [__data=" + __tourData + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private class ContextTourMarker {

		private TourMarker	__tourMarker;

		@Override
		public String toString() {
			return "ContextTourMarker [__data=" + __tourMarker + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void ctxGear(final EventMesg mesg) {

		// ensure a tour is setup
		ctxTourData_10_Initialize();

		// get gear list for current tour
		List<GearData> tourGears = _allGearData.get(_currentTourDataCtx);
		if (tourGears == null) {
			tourGears = new ArrayList<GearData>();
			_allGearData.put(_currentTourDataCtx, tourGears);
		}

		final Long gearChangeData = mesg.getGearChangeData();

		// check if gear data are available, it can be null
		if (gearChangeData != null) {

			// create gear data for the current time
			final GearData gearData = new GearData();

			final com.garmin.fit.DateTime garminTime = mesg.getTimestamp();

			// convert garmin time into linux time
			final long garminTimeS = garminTime.getTimestamp();
			final long garminTimeMS = garminTimeS * 1000;
			final long linuxTime = garminTimeMS + com.garmin.fit.DateTime.OFFSET;

			gearData.absoluteTime = linuxTime;
			gearData.gears = gearChangeData;

			tourGears.add(gearData);
		}
	}

	public void ctxTimeData_10_Initialize() {

		ctxTourData_10_Initialize();

		_currentContextTimeDataList = _allTimeData.get(_currentTourDataCtx);
		if (_currentContextTimeDataList == null) {

			_currentContextTimeDataList = new ArrayList<ContextTimeData>();
			_allTimeData.put(_currentTourDataCtx, _currentContextTimeDataList);
		}

		_currentTimeDataCtx = new ContextTimeData();
		_currentTimeDataCtx.__timeData = new TimeData();

		_currentContextTimeDataList.add(_currentTimeDataCtx);
	}

	public void ctxTimeData_20_Finalize() {

		_currentTimeDataCtx = null;
	}

	public void ctxTourData_10_Initialize() {

		if (_currentTourDataCtx == null) {

			_currentTourDataCtx = new ContextTourData();
			_currentTourDataCtx.__tourData = new TourData();

			_allTourData.add(_currentTourDataCtx);
		}
	}

	public void ctxTourData_20_Finalize() {

		ctxTimeData_20_Finalize();

		_currentTourDataCtx = null;
		_currentContextTimeDataList = null;
	}

	public void ctxTourMarker_10_Initialize() {

		ctxTourData_10_Initialize();

		List<ContextTourMarker> currentContextTourMarkerList = _allTourMarker.get(_currentTourDataCtx);
		if (currentContextTourMarkerList == null) {

			currentContextTourMarkerList = new ArrayList<ContextTourMarker>();
			_allTourMarker.put(_currentTourDataCtx, currentContextTourMarkerList);
		}

		_currentTourMarkerCtx = new ContextTourMarker();
		_currentTourMarkerCtx.__tourMarker = new TourMarker(getCurrentTourData(), ChartLabel.MARKER_TYPE_DEVICE);

		currentContextTourMarkerList.add(_currentTourMarkerCtx);
	}

	public void ctxTourMarker_20_Finalize() {
		_currentTourMarkerCtx = null;
	}

	public List<ContextTimeData> getAllTimeData() {
		return _currentContextTimeDataList;
	}

	public TimeData getCurrentTimeData() {

		if (_currentTimeDataCtx == null) {
			throw new IllegalArgumentException("Time data is not initialized"); //$NON-NLS-1$
		}

		return _currentTimeDataCtx.__timeData;

	}

	public TourData getCurrentTourData() {

		if (_currentTourDataCtx == null) {
			throw new IllegalArgumentException("Tour data is not initialized"); //$NON-NLS-1$
		}

		return _currentTourDataCtx.__tourData;
	}

	public TourMarker getCurrentTourMarker() {

		if (_currentTourMarkerCtx == null) {
			throw new IllegalArgumentException("Tour marker is not initialized"); //$NON-NLS-1$
		}

		return _currentTourMarkerCtx.__tourMarker;
	}

	public void processData(final FitContextDataHandler handler) {

		for (final ContextTourData contextTourData : _allTourData) {

			/*
			 * create time data list
			 */
			final List<ContextTimeData> contextTimeDataList = _allTimeData.get(contextTourData);

			final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>(contextTimeDataList.size());
			for (final ContextTimeData contextTimeData : contextTimeDataList) {
				timeDataList.add(contextTimeData.__timeData);
			}

			/*
			 * create tour marker list
			 */
			final int lastSerieIndex = timeDataList.size() - 1;

			final List<ContextTourMarker> contextTourMarkerList = _allTourMarker.get(contextTourData);

			final Set<TourMarker> tourMarkerSet = new HashSet<TourMarker>(contextTourMarkerList.size());
			for (final ContextTourMarker contextTourMarker : contextTourMarkerList) {

				final TourMarker tourMarker = contextTourMarker.__tourMarker;

				/*
				 * Fit devices adds a marker at the end, this is annoing therefore it is removed. It
				 * is not only the last time slice it can also be about the last 5 time slices.
				 */
				if (tourMarker.getSerieIndex() > lastSerieIndex - 5) {
					continue;
				}

				tourMarkerSet.add(tourMarker);
			}

			/*
			 * setup tour
			 */
			final TourData tourData = contextTourData.__tourData;
			final List<GearData> gearList = _allGearData.get(contextTourData);

			handler.setupTour(tourData, timeDataList, tourMarkerSet, gearList);
		}
	}

}
