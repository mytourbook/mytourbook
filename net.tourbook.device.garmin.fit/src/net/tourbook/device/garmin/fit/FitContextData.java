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

	private final List<ContextTourData>						_allContextTourData	= new ArrayList<ContextTourData>();

	private final Map<ContextTourData, List<TimeData>>		_allTimeData		= new HashMap<ContextTourData, List<TimeData>>();
	private final Map<ContextTourData, List<TourMarker>>	_allTourMarker		= new HashMap<FitContextData.ContextTourData, List<TourMarker>>();
	private final Map<ContextTourData, List<GearData>>		_allGearData		= new HashMap<FitContextData.ContextTourData, List<GearData>>();

	private ContextTourData									_currentTourDataCtx;
	private TimeData										_currentTimeData;
	private TourMarker										_currentTourMarker;

	private List<TimeData>									_currentTimeDataList;

	private class ContextTourData {

		private TourData	__tourData;

		@Override
		public String toString() {
			return "ContextTourData [__data=" + __tourData + "]"; //$NON-NLS-1$ //$NON-NLS-2$
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

		_currentTimeDataList = _allTimeData.get(_currentTourDataCtx);
		if (_currentTimeDataList == null) {

			_currentTimeDataList = new ArrayList<TimeData>();
			_allTimeData.put(_currentTourDataCtx, _currentTimeDataList);
		}

		_currentTimeData = new TimeData();

		_currentTimeDataList.add(_currentTimeData);
	}

	public void ctxTimeData_20_Finalize() {

		_currentTimeData = null;
	}

	public void ctxTourData_10_Initialize() {

		if (_currentTourDataCtx == null) {

			_currentTourDataCtx = new ContextTourData();
			_currentTourDataCtx.__tourData = new TourData();

			_allContextTourData.add(_currentTourDataCtx);
		}
	}

	public void ctxTourData_20_Finalize() {

		ctxTimeData_20_Finalize();

		_currentTourDataCtx = null;
		_currentTimeDataList = null;
	}

	public void ctxTourMarker_10_Initialize() {

		ctxTourData_10_Initialize();

		List<TourMarker> currentTourMarkers = _allTourMarker.get(_currentTourDataCtx);

		if (currentTourMarkers == null) {

			currentTourMarkers = new ArrayList<TourMarker>();
			_allTourMarker.put(_currentTourDataCtx, currentTourMarkers);
		}

		_currentTourMarker = new TourMarker(getCurrentTourData(), ChartLabel.MARKER_TYPE_DEVICE);

		currentTourMarkers.add(_currentTourMarker);
	}

	public void ctxTourMarker_20_Finalize() {
		_currentTourMarker = null;
	}

	public List<TimeData> getAllTimeData() {
		return _currentTimeDataList;
	}

	public TimeData getCurrentTimeData() {

		if (_currentTimeData == null) {
			throw new IllegalArgumentException("Time data is not initialized"); //$NON-NLS-1$
		}

		return _currentTimeData;

	}

	public TourData getCurrentTourData() {

		if (_currentTourDataCtx == null) {
			throw new IllegalArgumentException("Tour data is not initialized"); //$NON-NLS-1$
		}

		return _currentTourDataCtx.__tourData;
	}

	public TourMarker getCurrentTourMarker() {

		if (_currentTourMarker == null) {
			throw new IllegalArgumentException("Tour marker is not initialized"); //$NON-NLS-1$
		}

		return _currentTourMarker;
	}

	public void processData(final FitContextDataHandler handler) {

		for (final ContextTourData contextTourData : _allContextTourData) {

			final List<TimeData> allTimeData = _allTimeData.get(contextTourData);

			/*
			 * create tour marker list
			 */
			final List<TourMarker> tourMarkers = _allTourMarker.get(contextTourData);

			final Set<TourMarker> tourMarkerSet = new HashSet<TourMarker>(tourMarkers.size());
			for (final TourMarker tourMarker : tourMarkers) {
				tourMarkerSet.add(tourMarker);
			}

			/*
			 * setup tour
			 */
			final TourData tourData = contextTourData.__tourData;
			final List<GearData> gears = _allGearData.get(contextTourData);

			handler.setupTour(tourData, allTimeData, tourMarkers, gears);
		}
	}

}
