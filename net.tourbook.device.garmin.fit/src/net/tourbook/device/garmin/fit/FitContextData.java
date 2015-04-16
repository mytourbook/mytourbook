package net.tourbook.device.garmin.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private final List<TourContext>						_allTourContext	= new ArrayList<TourContext>();

	private final Map<TourContext, List<GearData>>		_allGearData	= new HashMap<TourContext, List<GearData>>();
	private final Map<TourContext, List<TimeData>>		_allTimeData	= new HashMap<TourContext, List<TimeData>>();
	private final Map<TourContext, List<TourMarker>>	_allTourMarker	= new HashMap<TourContext, List<TourMarker>>();

	private TourContext									_currentTourContext;

	private TimeData									_currentTimeData;
	private List<TimeData>								_currentTimeDataList;

	private TourMarker									_currentTourMarker;

	/**
	 * Tour context is a little bit tricky and cannot be replaced with just a {@link TourData}
	 * instance. {@link TourContext} is the key to other tour values.
	 */
	private class TourContext {

		private TourData	__tourData;

		public TourContext(final TourData tourData) {
			__tourData = tourData;
		}

		@Override
		public String toString() {
			return "TourContext [__tourData=" + __tourData + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Gear data are available in the common {@link EventMesg}.
	 * 
	 * @param mesg
	 */
	public void ctxEventMesg(final EventMesg mesg) {

		// ensure a tour is setup
		ctxTour_10_Initialize();

		final Long gearChangeData = mesg.getGearChangeData();

		// check if gear data are available, it can be null
		if (gearChangeData != null) {

			// get gear list for current tour
			List<GearData> tourGears = _allGearData.get(_currentTourContext);

			if (tourGears == null) {
				tourGears = new ArrayList<GearData>();
				_allGearData.put(_currentTourContext, tourGears);
			}

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

	public void ctxMarker_10_Initialize() {

		ctxTour_10_Initialize();

		List<TourMarker> tourMarkers = _allTourMarker.get(_currentTourContext);

		if (tourMarkers == null) {
			tourMarkers = new ArrayList<TourMarker>();
			_allTourMarker.put(_currentTourContext, tourMarkers);
		}

		_currentTourMarker = new TourMarker(getCurrentTourData(), ChartLabel.MARKER_TYPE_DEVICE);
		tourMarkers.add(_currentTourMarker);
	}

	public void ctxMarker_20_Finalize() {

		_currentTourMarker = null;
	}

	public void ctxTime_10_Initialize() {

		ctxTour_10_Initialize();

//		_currentTimeDataList = _allTimeData.get(_currentTourData);

		if (_currentTimeDataList == null) {
			_currentTimeDataList = new ArrayList<TimeData>();
			_allTimeData.put(_currentTourContext, _currentTimeDataList);
		}

		_currentTimeData = new TimeData();
		_currentTimeDataList.add(_currentTimeData);
	}

	public void ctxTime_20_Finalize() {

		_currentTimeData = null;
	}

	public void ctxTour_10_Initialize() {

		if (_currentTourContext == null) {

			final TourData currentTourData = new TourData();

			_currentTourContext = new TourContext(currentTourData);

			_allTourContext.add(_currentTourContext);
		}
	}

	public void ctxTour_20_Finalize() {

		ctxTime_20_Finalize();

		_currentTourContext = null;
		_currentTimeDataList = null;
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

		if (_currentTourContext == null) {
			throw new IllegalArgumentException("Tour data is not initialized"); //$NON-NLS-1$
		}

		return _currentTourContext.__tourData;
	}

	public TourMarker getCurrentTourMarker() {

		if (_currentTourMarker == null) {
			throw new IllegalArgumentException("Tour marker is not initialized"); //$NON-NLS-1$
		}

		return _currentTourMarker;
	}

	public void processAllTours(final FitContextDataHandler handler) {

		for (final TourContext tourContext : _allTourContext) {

			final List<TimeData> timeDataList = _allTimeData.get(tourContext);

			// ensure data are avaialble
			if (timeDataList == null) {
				// nothing is imported
				continue;
			}

			final TourData tourData = tourContext.__tourData;

			final List<TourMarker> tourMarkers = _allTourMarker.get(tourContext);
			final List<GearData> tourGears = _allGearData.get(tourContext);

			handler.finalizeTour(tourData, timeDataList, tourMarkers, tourGears);
		}
	}

}
