package net.tourbook.device.garmin.fit;

import com.garmin.fit.EventMesg;
import com.garmin.fit.HrMesg;
import com.garmin.fit.LengthMesg;
import com.garmin.fit.LengthType;
import com.garmin.fit.SwimStroke;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tourbook.data.GearData;
import net.tourbook.data.SwimData;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.tour.TourLogManager;
import net.tourbook.ui.tourChart.ChartLabel;

/**
 * Wrapper for {@link TourData} used by {@link FitContext}.
 *
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class FitContextData {

	private final List<TourContext>							_allTourContext	= new ArrayList<>();

	private final Map<TourContext, List<GearData>>		_allGearData		= new HashMap<>();
	private final Map<TourContext, List<SwimData>>		_allSwimData		= new HashMap<>();
	private final Map<TourContext, List<TimeData>>		_allTimeData		= new HashMap<>();
	private final Map<TourContext, List<TourMarker>>	_allTourMarker		= new HashMap<>();

	private TimeData												_current_TimeData;
	private TimeData												_previous_TimeData;

	private List<TimeData>										_current_AllTimeData;
	private List<TimeData>										_previous_AllTimeData;

	private TourContext											_current_TourContext;
	private TourMarker											_current_TourMarker;

	/**
	 * Tour context is a little bit tricky and cannot be replaced with just a {@link TourData}
	 * instance. {@link TourContext} is the key to other tour values.
	 */
	private class TourContext {

		private TourData __tourData;

		public TourContext(final TourData tourData) {
			__tourData = tourData;
		}

		@Override
		public String toString() {
			return "TourContext [__tourData=" + __tourData + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public TimeData getCurrent_TimeData() {

		if (_current_TimeData == null) {
			throw new IllegalArgumentException("Time data is not initialized"); //$NON-NLS-1$
		}

		return _current_TimeData;

	}

	public TourData getCurrent_TourData() {

		if (_current_TourContext == null) {
			throw new IllegalArgumentException("Tour data is not initialized"); //$NON-NLS-1$
		}

		return _current_TourContext.__tourData;
	}

	public TourMarker getCurrent_TourMarker() {

		if (_current_TourMarker == null) {
			throw new IllegalArgumentException("Tour marker is not initialized"); //$NON-NLS-1$
		}

		return _current_TourMarker;
	}

	/**
	 * Gear data are available in the common {@link EventMesg}.
	 *
	 * @param mesg
	 */
	public void onMesg_Event(final EventMesg mesg) {

		// ensure a tour is setup
		setupSession_Tour_10_Initialize();

		final Long gearChangeData = mesg.getGearChangeData();

		// check if gear data are available, it can be null
		if (gearChangeData != null) {

			// get gear list for current tour
			List<GearData> tourGears = _allGearData.get(_current_TourContext);

			if (tourGears == null) {
				tourGears = new ArrayList<>();
				_allGearData.put(_current_TourContext, tourGears);
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

	/**
	 * This is called AFTER the session is closed. Swimming is producing the hr event separately in
	 * it's own device.
	 *
	 * @param mesg
	 */
	public void onMesg_Hr(final HrMesg mesg) {

		// ensure tour is setup
		setupSession_Tour_10_Initialize();

//		System.out.println(String.format(""
//
//				+ "[%s]"
//
//				+ " NumEventTimestamp %-3d"
//				+ " NumFilteredBpm %-3d"
//
//				+ " Timestamp %-29s"
//				+ " timestamp %-15s"
//				+ " FractionalTimestamp: %-7.5f"
////				+ (" Time256: " + mesg.getTime256())
//
//				+ " EventTimestamp %-90s "
//				+ " FilteredBpm %-45s "
//
//				+ (" NumEventTimestamp12 %-5d")
//				+ (" EventTimestamp12 " + Arrays.toString(mesg.getEventTimestamp12())),
//
//				getClass().getSimpleName(),
//
//				mesg.getNumEventTimestamp(),
//				mesg.getNumFilteredBpm(),
//
//				mesg.getTimestamp(),
//				mesg.getTimestamp() == null ? "" : mesg.getTimestamp().getTimestamp(),
//				mesg.getFractionalTimestamp() == null ? null : mesg.getFractionalTimestamp(),
//
//				Arrays.toString(mesg.getEventTimestamp()),
//				Arrays.toString(mesg.getFilteredBpm()),
//
//				mesg.getNumEventTimestamp12()));
////TODO remove SYSTEM.OUT.PRINTLN

		boolean isTimeAvailable = false;

		final int numEventTimestamp = mesg.getNumEventTimestamp();
		if (numEventTimestamp < 1) {
			return;
		}

		final Float[] allEventTimestamps = mesg.getEventTimestamp();
		final Short[] allFilteredBpm = mesg.getFilteredBpm();

		if (allFilteredBpm.length != allEventTimestamps.length) {

			TourLogManager.logError(String.format("Fit file has different filtered data: EventTimestamp: %d - FilteredBpm: %d",
					allEventTimestamps.length,
					allFilteredBpm.length));

			return;
		}

		for (int timeStampIndex = 0; timeStampIndex < allEventTimestamps.length; timeStampIndex++) {

			final Float eventTimeStamp = allEventTimestamps[timeStampIndex];
			final Short filteredBpm = allFilteredBpm[timeStampIndex];

			final double sliceGarminTimeS = eventTimeStamp + 901846752.0;
			final long sliceGarminTimeMS = (long) (sliceGarminTimeS * 1000);
			final long sliceJavaTime = sliceGarminTimeMS + com.garmin.fit.DateTime.OFFSET;

			// merge HR data into an already existing time data
			for (final TimeData timeData : _previous_AllTimeData) {

				if (timeData.absoluteTime == sliceJavaTime) {

					timeData.pulse = filteredBpm;
					isTimeAvailable = true;

//					System.out.println(String.format(""
//
//							+ "[%s]"
//
//							+ (" eventTimeStamp %-8.2f   ")
//							+ (" sliceJavaTime %d   ")
//							+ (" localDT %s   ")
//							+ (" bpm %d"),
//
//							getClass().getSimpleName(),
//
//							eventTimeStamp,
//							sliceJavaTime,
//							new LocalDateTime(sliceJavaTime),
//							filteredBpm
//
//// TODO remove SYSTEM.OUT.PRINTLN
//					));

					break;
				}
			}

			if (isTimeAvailable == false) {

				// timeslice is not yet created for this heartrate

//				System.out.println(String.format(""
//
//						+ "[%s]"
//
//						+ (" eventTimeStamp %-8.2f   ")
//						+ (" sliceJavaTime %d   ")
//						+ (" localDT %s   ")
//						+ (" bpm %d - no timeslice"),
//
//						getClass().getSimpleName(),
//
//						eventTimeStamp,
//						sliceJavaTime,
//						new LocalDateTime(sliceJavaTime),
//						filteredBpm
//
//// TODO remove SYSTEM.OUT.PRINTLN
//				));
			}
		}
	}

	public void onMesg_Length(final LengthMesg mesg) {

		// ensure tour is setup
//		setupSession_Tour_10_Initialize();

		// get gear list for current tour
		List<SwimData> tourSwimData = _allSwimData.get(_current_TourContext);

		if (tourSwimData == null) {
			tourSwimData = new ArrayList<>();
			_allSwimData.put(_current_TourContext, tourSwimData);
		}

		// create gear data for the current time
		final SwimData swimData = new SwimData();

		tourSwimData.add(swimData);

		final com.garmin.fit.DateTime garminTime = mesg.getTimestamp();

		// convert garmin time into java time
		final long garminTimeS = garminTime.getTimestamp();
		final long garminTimeMS = garminTimeS * 1000;
		final long javaTime = garminTimeMS + com.garmin.fit.DateTime.OFFSET;

		final Short avgSwimmingCadence = mesg.getAvgSwimmingCadence();
		final LengthType lengthType = mesg.getLengthType();
		final SwimStroke swimStrokeStyle = mesg.getSwimStroke();
		final Integer numStrokes = mesg.getTotalStrokes();

		swimData.absoluteTime = javaTime;

		if (lengthType != null) {
			swimData.swim_ActivityType = lengthType.getValue();
		}

		if (avgSwimmingCadence != null) {
			swimData.swim_Cadence = avgSwimmingCadence;
		}

		if (numStrokes != null) {
			swimData.swim_Strokes = numStrokes.shortValue();
		}

		if (swimStrokeStyle != null) {
			swimData.swim_StrokeStyle = swimStrokeStyle.getValue();
		}

//		final long timestamp = mesg.getTimestamp().getDate().getTime();
//
//		System.out.println(String.format(""
//
//				+ "[%s]"
//
//				+ " Timestamp %-23s"
////				+ " StartTime %-23s"
////				+ " Time Diff %-6d"
//
//				+ " LengthType %-10s"
//
//				+ " SwimStroke %-15s"
//				+ " AvgSwimmingCadence %-6s"
//				+ " TotalStrokes %-5s"
//
////				+ " NumStrokeCount %-3d"
////				+ " StrokeCount %-30s"
//
//				,
//
//				getClass().getSimpleName(),
//
//				TimeTools.toLocalDateTime(timestamp),
////				TimeTools.toLocalDateTime(mesg.getStartTime().getDate().getTime()),
////				mesg.getTimestamp().getTimestamp() - mesg.getStartTime().getTimestamp(),
//
//				lengthType,
//
//				swimStrokeStyle == null ? "" : swimStrokeStyle.toString(),
//				avgSwimmingCadence == null ? "" : avgSwimmingCadence.toString(),
//				numStrokes == null ? "" : numStrokes.toString()
//
//		));
////TODO remove SYSTEM.OUT.PRINTLN

//		[FitContextData] Timestamp 2018-09-01T14:51:01     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 24     TotalStrokes 12
//		[FitContextData] Timestamp 2018-09-01T14:51:26     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 23     TotalStrokes 11
//		[FitContextData] Timestamp 2018-09-01T14:52        LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 25     TotalStrokes 13
//		[FitContextData] Timestamp 2018-09-01T14:52:30     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 24     TotalStrokes 12
//		[FitContextData] Timestamp 2018-09-01T14:53        LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 24     TotalStrokes 12
//		[FitContextData] Timestamp 2018-09-01T14:53:19     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 24     TotalStrokes 11
//		[FitContextData] Timestamp 2018-09-01T14:54:34     LengthType IDLE       SwimStroke                 AvgSwimmingCadence        TotalStrokes
//		[FitContextData] Timestamp 2018-09-01T14:55:25     LengthType ACTIVE     SwimStroke BREASTSTROKE    AvgSwimmingCadence 17     TotalStrokes 12
//		[FitContextData] Timestamp 2018-09-01T14:56:09     LengthType ACTIVE     SwimStroke BREASTSTROKE    AvgSwimmingCadence 19     TotalStrokes 13
//		[FitContextData] Timestamp 2018-09-01T14:56:50     LengthType ACTIVE     SwimStroke BREASTSTROKE    AvgSwimmingCadence 19     TotalStrokes 13
//		[FitContextData] Timestamp 2018-09-01T14:57:19     LengthType ACTIVE     SwimStroke BREASTSTROKE    AvgSwimmingCadence 16     TotalStrokes 11
//		[FitContextData] Timestamp 2018-09-01T14:59:05     LengthType IDLE       SwimStroke                 AvgSwimmingCadence        TotalStrokes
//		[FitContextData] Timestamp 2018-09-01T14:59:42     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 23     TotalStrokes 12
//		[FitContextData] Timestamp 2018-09-01T15:00:10     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 26     TotalStrokes 12
//		[FitContextData] Timestamp 2018-09-01T15:00:38     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 25     TotalStrokes 12
//		[FitContextData] Timestamp 2018-09-01T15:01:09     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 26     TotalStrokes 12

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
			final List<SwimData> tourSwimData = _allSwimData.get(tourContext);

			handler.finalizeTour(tourData, timeDataList, tourMarkers, tourGears, tourSwimData);
		}
	}

	public void setupLap_Marker_10_Initialize() {

		setupSession_Tour_10_Initialize();

		List<TourMarker> tourMarkers = _allTourMarker.get(_current_TourContext);

		if (tourMarkers == null) {

			tourMarkers = new ArrayList<>();

			_allTourMarker.put(_current_TourContext, tourMarkers);
		}

		_current_TourMarker = new TourMarker(getCurrent_TourData(), ChartLabel.MARKER_TYPE_DEVICE);
		tourMarkers.add(_current_TourMarker);
	}

	public void setupLap_Marker_20_Finalize() {

		_current_TourMarker = null;
	}

	public void setupRecord_10_Initialize() {

		// ensure tour is setup
		setupSession_Tour_10_Initialize();

		if (_current_AllTimeData == null) {

			_current_AllTimeData = new ArrayList<>();

			_allTimeData.put(_current_TourContext, _current_AllTimeData);
		}

		_current_TimeData = new TimeData();
	}

	public void setupRecord_20_Finalize() {

		if (_current_TimeData == null) {
			// this occured
			return;
		}

		boolean useThisTimeSlice = true;

		if (_previous_TimeData != null) {

			final long prevTime = _previous_TimeData.absoluteTime;
			final long currentTime = _current_TimeData.absoluteTime;

			if (prevTime == currentTime) {

				/*
				 * Ignore and merge duplicated records. The device Bryton 210 creates duplicated enries,
				 * to have valid data for this device, they must be merged.
				 */

				useThisTimeSlice = false;

				if (_previous_TimeData.absoluteAltitude == Float.MIN_VALUE) {
					_previous_TimeData.absoluteAltitude = _current_TimeData.absoluteAltitude;
				}

				if (_previous_TimeData.absoluteDistance == Float.MIN_VALUE) {
					_previous_TimeData.absoluteDistance = _current_TimeData.absoluteDistance;
				}

				if (_previous_TimeData.cadence == Float.MIN_VALUE) {
					_previous_TimeData.cadence = _current_TimeData.cadence;
				}

				if (_previous_TimeData.latitude == Double.MIN_VALUE) {
					_previous_TimeData.latitude = _current_TimeData.latitude;
				}

				if (_previous_TimeData.longitude == Double.MIN_VALUE) {
					_previous_TimeData.longitude = _current_TimeData.longitude;
				}

				if (_previous_TimeData.power == Float.MIN_VALUE) {
					_previous_TimeData.power = _current_TimeData.power;
				}

				if (_previous_TimeData.pulse == Float.MIN_VALUE) {
					_previous_TimeData.pulse = _current_TimeData.pulse;
				}

				if (_previous_TimeData.speed == Float.MIN_VALUE) {
					_previous_TimeData.speed = _current_TimeData.speed;
				}

				if (_previous_TimeData.temperature == Float.MIN_VALUE) {
					_previous_TimeData.temperature = _current_TimeData.temperature;
				}
			}
		}

		if (useThisTimeSlice) {
			_current_AllTimeData.add(_current_TimeData);
		}

		_previous_TimeData = _current_TimeData;
		_current_TimeData = null;
	}

	public void setupSession_Tour_10_Initialize() {

		if (_current_TourContext == null) {

			final TourData currentTourData = new TourData();

			_current_TourContext = new TourContext(currentTourData);

			_allTourContext.add(_current_TourContext);
		}
	}

	public void setupSession_Tour_20_Finalize() {

		setupRecord_20_Finalize();

		_previous_AllTimeData = _current_AllTimeData;

		_current_TourContext = null;
		_current_AllTimeData = null;
	}

}
