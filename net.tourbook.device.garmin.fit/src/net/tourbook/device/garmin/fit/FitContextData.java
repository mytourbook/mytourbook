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

	private TimeData									_previousTimeData;

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
		onMesgSession_Tour_10_Initialize();

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

			// convert garmin time into java time
			final long garminTimeS = garminTime.getTimestamp();
			final long garminTimeMS = garminTimeS * 1000;
			final long javaTime = garminTimeMS + com.garmin.fit.DateTime.OFFSET;

			gearData.absoluteTime = javaTime;
			gearData.gears = gearChangeData;

			tourGears.add(gearData);
		}
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

	public void onMesgLap_Marker_10_Initialize() {

		onMesgSession_Tour_10_Initialize();

		List<TourMarker> tourMarkers = _allTourMarker.get(_currentTourContext);

		if (tourMarkers == null) {

			tourMarkers = new ArrayList<TourMarker>();

			_allTourMarker.put(_currentTourContext, tourMarkers);
		}

		_currentTourMarker = new TourMarker(getCurrentTourData(), ChartLabel.MARKER_TYPE_DEVICE);
		tourMarkers.add(_currentTourMarker);
	}

	public void onMesgLap_Marker_20_Finalize() {

		_currentTourMarker = null;
	}

	public void onMesgRecord_Time_10_Initialize() {
 
		// ensure tour is setup
		onMesgSession_Tour_10_Initialize();

		if (_currentTimeDataList == null) {

			_currentTimeDataList = new ArrayList<TimeData>();

			_allTimeData.put(_currentTourContext, _currentTimeDataList);
		}

		_currentTimeData = new TimeData();
	}

	public void onMesgRecord_Time_20_Finalize() {

		if (_currentTimeData == null) {
			// this occured
			return;
		}

		boolean useThisTimeSlice = true;

		if (_previousTimeData != null) {

			final long prevTime = _previousTimeData.absoluteTime;
			final long currentTime = _currentTimeData.absoluteTime;

			if (prevTime == currentTime) {

				/*
				 * Ignore and merge duplicated records. The device Bryton 210 creates duplicated
				 * enries, to have valid data for this device, they must be merged.
				 */

				useThisTimeSlice = false;

				if (_previousTimeData.absoluteAltitude == Float.MIN_VALUE) {
					_previousTimeData.absoluteAltitude = _currentTimeData.absoluteAltitude;
				}

				if (_previousTimeData.absoluteDistance == Float.MIN_VALUE) {
					_previousTimeData.absoluteDistance = _currentTimeData.absoluteDistance;
				}

				if (_previousTimeData.cadence == Float.MIN_VALUE) {
					_previousTimeData.cadence = _currentTimeData.cadence;
				}

				if (_previousTimeData.latitude == Double.MIN_VALUE) {
					_previousTimeData.latitude = _currentTimeData.latitude;
				}

				if (_previousTimeData.longitude == Double.MIN_VALUE) {
					_previousTimeData.longitude = _currentTimeData.longitude;
				}

				if (_previousTimeData.power == Float.MIN_VALUE) {
					_previousTimeData.power = _currentTimeData.power;
				}

				if (_previousTimeData.pulse == Float.MIN_VALUE) {
					_previousTimeData.pulse = _currentTimeData.pulse;
				}

				if (_previousTimeData.speed == Float.MIN_VALUE) {
					_previousTimeData.speed = _currentTimeData.speed;
				}

				if (_previousTimeData.temperature == Float.MIN_VALUE) {
					_previousTimeData.temperature = _currentTimeData.temperature;
				}
			}
		}

		if (useThisTimeSlice) {
			_currentTimeDataList.add(_currentTimeData);
		}

		_previousTimeData = _currentTimeData;
		_currentTimeData = null;
	}

	public void onMesgSession_Tour_10_Initialize() {

		if (_currentTourContext == null) {

			final TourData currentTourData = new TourData();

			_currentTourContext = new TourContext(currentTourData);

			_allTourContext.add(_currentTourContext);
		}
	}

	public void onMesgSession_Tour_20_Finalize() {

		onMesgRecord_Time_20_Finalize();

		_currentTourContext = null;
		_currentTimeDataList = null;
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
