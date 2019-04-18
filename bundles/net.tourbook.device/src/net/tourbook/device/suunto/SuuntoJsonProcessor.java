package net.tourbook.device.suunto;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.swimming.SwimStroke;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.LengthType;
import net.tourbook.data.SwimData;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;

public class SuuntoJsonProcessor {

	private final float				Kelvin				= 273.1499938964845f;
	private ArrayList<TimeData>	_sampleList;
	private int							_lapCounter;
	final IPreferenceStore			_prefStore			= TourbookPlugin.getDefault().getPreferenceStore();

	public static final String		TAG_SAMPLES			= "Samples";													//$NON-NLS-1$
	public static final String		TAG_SAMPLE			= "Sample";														//$NON-NLS-1$
	public static final String		TAG_TIMEISO8601	= "TimeISO8601";												//$NON-NLS-1$
	public static final String		TAG_ATTRIBUTES		= "Attributes";												//$NON-NLS-1$
	public static final String		TAG_SOURCE			= "Source";														//$NON-NLS-1$
	private static final String	TAG_SUUNTOSML		= "suunto/sml";												//$NON-NLS-1$
	private static final String	TAG_LAP				= "Lap";															//$NON-NLS-1$
	private static final String	TAG_MANUAL			= "Manual";														//$NON-NLS-1$
	private static final String	TAG_DISTANCE		= "Distance";													//$NON-NLS-1$
	public static final String		TAG_GPSALTITUDE	= "GPSAltitude";												//$NON-NLS-1$
	public static final String		TAG_LATITUDE		= "Latitude";													//$NON-NLS-1$
	public static final String		TAG_LONGITUDE		= "Longitude";													//$NON-NLS-1$
	private static final String	TAG_TYPE				= "Start";														//$NON-NLS-1$
	private static final String	TAG_START			= "Type";														//$NON-NLS-1$
	private static final String	TAG_PAUSE			= "Pause";														//$NON-NLS-1$
	private static final String	TAG_HR				= "HR";															//$NON-NLS-1$
	private static final String	TAG_RR				= "R-R";															//$NON-NLS-1$
	private static final String	TAG_DATA				= "Data";														//$NON-NLS-1$
	private static final String	TAG_SPEED			= "Speed";														//$NON-NLS-1$
	private static final String	TAG_CADENCE			= "Cadence";													//$NON-NLS-1$
	public static final String		TAG_ALTITUDE		= "Altitude";													//$NON-NLS-1$
	private static final String	TAG_POWER			= "Power";														//$NON-NLS-1$
	private static final String	TAG_TEMPERATURE	= "Temperature";

	// Swimming
	private static final String	Swimming					= "Swimming";
	private static final String	Breaststroke			= "Breaststroke";
	private static final String	Freestyle				= "Freestyle";
	private static final String	PoolLengthDuration	= "PrevPoolLengthDuration";
	private static final String	PoolLengthStyle		= "PrevPoolLengthStyle";
	private static final String	Stroke					= "Stroke";
	private static final String	TotalLengths			= "TotalLengths";
	private static final String	Turn						= "Turn";
	private static final String	Type						= "Type";

	/**
	 * Processes and imports a Suunto activity (from a Suunto 9 or Spartan watch).
	 * 
	 * @param jsonFileContent
	 *           The Suunto's file content in JSON format.
	 * @param activityToReUse
	 *           If provided, the activity to concatenate the provided file to.
	 * @param sampleListToReUse
	 *           If provided, the activity's data from the activity to reuse.
	 * @param isUnitTest
	 *           True if the method is run for unit test purposes.
	 * @return The created tour.
	 */
	public TourData ImportActivity(	String jsonFileContent,
												TourData activityToReUse,
												ArrayList<TimeData> sampleListToReUse,
												boolean isUnitTest) {
		_sampleList = new ArrayList<TimeData>();

		JSONArray samples = null;
		try {
			JSONObject jsonContent = new JSONObject(jsonFileContent);
			samples = (JSONArray) jsonContent.get(TAG_SAMPLES);
		} catch (JSONException ex) {
			StatusUtil.log(ex);
			return null;
		}

		JSONObject firstSample = (JSONObject) samples.get(0);

		TourData tourData = InitializeActivity(firstSample, activityToReUse, sampleListToReUse);

		//tourData.swim_Time
		//see finalizeTour_SwimData

		if (tourData == null)
			return null;

		boolean isIndoorTour = !jsonFileContent.contains(TAG_GPSALTITUDE);

		boolean isPaused = false;

		boolean reusePreviousTimeEntry;
		ArrayList<Integer> rrIntervalsList = new ArrayList<Integer>();
		Instant minInstant = Instant.ofEpochMilli(Long.MIN_VALUE);
		ZonedDateTime pauseStartTime = minInstant.atZone(ZoneOffset.UTC);
		int previousTotalLength = 0;

		List<SwimData> _allSwimData = new ArrayList<>();

		for (int i = 0; i < samples.length(); ++i) {
			String currentSampleSml;
			String currentSampleData;
			String sampleTime;
			try {
				JSONObject sample = samples.getJSONObject(i);
				if (!sample.toString().contains(TAG_TIMEISO8601))
					continue;

				String attributesContent = sample.get(TAG_ATTRIBUTES).toString();
				if (attributesContent == null || attributesContent == "") //$NON-NLS-1$
					continue;

				JSONObject currentSampleAttributes = new JSONObject(sample.get(TAG_ATTRIBUTES).toString());
				currentSampleSml = currentSampleAttributes.get(TAG_SUUNTOSML).toString();

				if (currentSampleSml.contains(TAG_SAMPLE))
					currentSampleData = new JSONObject(currentSampleSml).get(TAG_SAMPLE).toString();
				else
					currentSampleData = "{}";

				sampleTime = sample.get(TAG_TIMEISO8601).toString();
			} catch (Exception e) {
				StatusUtil.log(e);
				continue;
			}

			boolean wasDataPopulated = false;
			reusePreviousTimeEntry = false;
			TimeData timeData = null;

			ZonedDateTime currentZonedDateTime = ZonedDateTime.parse(sampleTime);
			currentZonedDateTime = currentZonedDateTime.truncatedTo(ChronoUnit.SECONDS);
			// Rounding to the nearest second
			if (Character.getNumericValue(sampleTime.charAt(20)) >= 5)
				currentZonedDateTime = currentZonedDateTime.plusSeconds(1);

			long currentTime = currentZonedDateTime.toInstant().toEpochMilli();
			if (currentTime <= tourData.getTourStartTimeMS())
				continue;

			if (_sampleList.size() > 0) {
				// Looking in the last 10 entries to see if their time is identical to the
				// current sample's time
				for (int index = _sampleList.size() - 1; index > _sampleList.size() - 11 && index >= 0; --index) {
					if (_sampleList.get(index).absoluteTime == currentTime) {
						timeData = _sampleList.get(index);
						reusePreviousTimeEntry = true;
						break;
					}
				}
			}

			if (!reusePreviousTimeEntry) {
				timeData = new TimeData();
				timeData.absoluteTime = currentTime;
			}

			if (currentSampleData.contains(TAG_PAUSE)) {
				if (!isPaused) {
					if (currentSampleData.contains(Boolean.TRUE.toString())) {
						isPaused = true;
						pauseStartTime = currentZonedDateTime;
					}
				} else {
					if (currentSampleData.contains(Boolean.FALSE.toString())) {
						isPaused = false;
					}
				}
			}

			//We check if the current sample date is greater/less than
			//the pause date because in the JSON file, the samples are
			//not necessarily in chronological order and we could have
			//the potential to miss data
			if (isPaused && currentZonedDateTime.isAfter(pauseStartTime))
				continue;
			if (currentSampleData.contains(TAG_RR))
				System.out.println("dd");
			if (currentSampleData.contains(TAG_LAP) &&
					(currentSampleData.contains(TAG_MANUAL) ||
							currentSampleData.contains(TAG_DISTANCE))) {
				timeData.marker = 1;
				timeData.markerLabel = Integer.toString(++_lapCounter);
				if (!reusePreviousTimeEntry)
					_sampleList.add(timeData);
			}

			// GPS point
			if (currentSampleData.contains(TAG_GPSALTITUDE) && currentSampleData.contains(TAG_LATITUDE)
					&& currentSampleData.contains(TAG_LONGITUDE)) {
				wasDataPopulated |= TryAddGpsData(new JSONObject(currentSampleData), timeData, isUnitTest);
			}

			// Heart Rate
			wasDataPopulated |= TryAddHeartRateData(new JSONObject(currentSampleData), timeData);
			wasDataPopulated |= TryComputeHeartRateData(rrIntervalsList, new JSONObject(currentSampleSml), timeData);

			// Speed
			wasDataPopulated |= TryAddSpeedData(new JSONObject(currentSampleData), timeData);

			// Cadence
			wasDataPopulated |= TryAddCadenceData(new JSONObject(currentSampleData), timeData);

			// Barometric Altitude
			if (_prefStore.getInt(IPreferences.ALTITUDE_DATA_SOURCE) == 1 ||
					isIndoorTour ||
					isUnitTest) {
				wasDataPopulated |= TryAddAltitudeData(new JSONObject(currentSampleData), timeData);
			}

			// Power
			wasDataPopulated |= TryAddPowerData(new JSONObject(currentSampleData), timeData);

			// Distance
			if (_prefStore.getInt(IPreferences.DISTANCE_DATA_SOURCE) == 1 ||
					isIndoorTour ||
					isUnitTest) {
				wasDataPopulated |= TryAddDistanceData(new JSONObject(currentSampleData), timeData);
			}

			// Temperature
			wasDataPopulated |= TryAddTemperatureData(new JSONObject(currentSampleData), timeData);

			//Swimming data
			if (currentSampleData.contains(Swimming)) {
				wasDataPopulated |= TryAddSwimmingData(
						_allSwimData,
						new JSONObject(currentSampleData),
						currentZonedDateTime,
						timeData.absoluteTime,
						previousTotalLength);

				// if (previousTotalLength == 0) => It is likely to be
				// a Stroke
/*
 * if (previousTotalLength > 0 && previousTotalLength == currentTotalLength) { // If the current
 * total length equals the previous // total length, it was likely a "rest" and we retrieve // the
 * very last pool length in order to create a // rest lap. IPoolLengthInfo lastPoolLengthInfo =
 * null; foreach (ILapInfo laps in activity.Laps) { foreach (IPoolLengthInfo lapsPoolLength in
 * laps.PoolLengths) { lastPoolLengthInfo = lapsPoolLength; } } //activity.Laps.Add( //
 * currentSampleDate, // currentSampleDate - toto.StartTime.ToLocalTime()); previousLapStartTime =
 * currentSampleDate; } if (currentTotalLength > 0) previousTotalLength = currentTotalLength;
 */
			}

			if (wasDataPopulated && !reusePreviousTimeEntry)
				_sampleList.add(timeData);
		}

		// We clean-up the data series ONLY if we're not in a swimming activity
		if (_allSwimData.size() == 0) {
			// Cleaning-up the processed entries as there should only be entries
			// every x seconds, no entries should be in between (entries with milliseconds).
			// Also, we need to make sure that they truly are in chronological order.
			Iterator<TimeData> sampleListIterator = _sampleList.iterator();
			long previousAbsoluteTime = 0;
			while (sampleListIterator.hasNext()) {
				TimeData currentTimeData = sampleListIterator.next();

				// Removing the entries that don't have GPS data
				// In the case where the activity is an indoor tour,
				// we remove the entries that don't have altitude data
				if (currentTimeData.marker == 0 &&
						(!isIndoorTour && currentTimeData.longitude == Double.MIN_VALUE && currentTimeData.latitude == Double.MIN_VALUE) ||
						(isIndoorTour && currentTimeData.absoluteAltitude == Float.MIN_VALUE) ||
						currentTimeData.absoluteTime <= previousAbsoluteTime)
					sampleListIterator.remove();
				else
					previousAbsoluteTime = currentTimeData.absoluteTime;
			}
		}

		tourData.createTimeSeries(_sampleList, true);

		finalizeTour_SwimData(tourData, _allSwimData);

		return tourData;
	}

	/**
	 * Creates a new activity and initializes all the needed fields.
	 * 
	 * @param firstSample
	 *           The activity start time as a string.
	 * @param activityToReuse
	 *           If provided, the activity to concatenate the current activity with.
	 * @param sampleListToReUse
	 *           If provided, the activity's data from the activity to reuse.
	 * @return If valid, the initialized tour
	 */
	private TourData InitializeActivity(JSONObject firstSample,
													TourData activityToReUse,
													ArrayList<TimeData> sampleListToReUse) {
		TourData tourData = new TourData();
		String firstSampleAttributes = firstSample.get(TAG_ATTRIBUTES).toString();

		if (firstSampleAttributes.contains(TAG_LAP) &&
				firstSampleAttributes.contains(TAG_TYPE) &&
				firstSampleAttributes.contains(TAG_START)) {

			ZonedDateTime startTime = ZonedDateTime.parse(firstSample.get(TAG_TIMEISO8601).toString());
			tourData.setTourStartTime(startTime);

		} else if (activityToReUse != null) {

			Set<TourMarker> tourMarkers = activityToReUse.getTourMarkers();
			for (Iterator<TourMarker> it = tourMarkers.iterator(); it.hasNext();) {
				TourMarker tourMarker = it.next();
				_lapCounter = Integer.valueOf(tourMarker.getLabel());
			}
			activityToReUse.setTourMarkers(new HashSet<TourMarker>());

			tourData = activityToReUse;
			_sampleList = sampleListToReUse;
			tourData.clearComputedSeries();
			tourData.timeSerie = null;

		} else
			return null;

		return tourData;

	}

	/**
	 * Retrieves the current activity's data.
	 * 
	 * @return The list of data.
	 */
	public ArrayList<TimeData> getSampleList() {
		return _sampleList;
	}

	/**
	 * Attempts to retrieve and add GPS data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 * @param isUnitTest
	 *           True if the method is run for unit test purposes.
	 * @return True if successful, false otherwise.
	 */
	private boolean TryAddGpsData(JSONObject currentSample, TimeData timeData, boolean isUnitTest) {
		try {
			float latitude = Util.parseFloat(currentSample.get(TAG_LATITUDE).toString());
			float longitude = Util.parseFloat(currentSample.get(TAG_LONGITUDE).toString());
			float altitude = Util.parseFloat(currentSample.get(TAG_GPSALTITUDE).toString());

			timeData.latitude = (latitude * 180) / Math.PI;
			timeData.longitude = (longitude * 180) / Math.PI;

			// GPS altitude
			if (_prefStore.getInt(IPreferences.ALTITUDE_DATA_SOURCE) == 0 ||
					isUnitTest) {
				timeData.absoluteAltitude = altitude;
			}

			return true;
		} catch (Exception e) {
			StatusUtil.log(e);
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add HR data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 * @return True if successful, false otherwise.
	 */
	private boolean TryAddHeartRateData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_HR)) != null) {
			timeData.pulse = Util.parseFloat(value) * 60.0f;
			return true;
		}

		return false;
	}

	/**
	 * Attempts to retrieve and add HR data from the MoveSense HR belt to the current activity.
	 * 
	 * @param rrIntervalsList
	 *           The list containing all the R-R intervals being gathered until they are saved in the
	 *           activity's heart rate list.
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param currentSampleDate
	 *           The date of the current data.
	 * @param previousSampleDate
	 *           The last date for which we saved heart rate data from R-R intervals in the current
	 *           activity.
	 */
	private boolean TryComputeHeartRateData(
															ArrayList<Integer> rrIntervalsList,
															JSONObject currentSample,
															TimeData timeData) {
		if (!currentSample.toString().contains(TAG_RR))
			return false;

		ArrayList<Integer> RRValues = TryRetrieveIntegerListElementValue(
				currentSample.getJSONObject(TAG_RR),
				TAG_DATA);

		if (RRValues.size() == 0)
			return false;

		for (int index = 0; index < RRValues.size(); ++index) {
			TimeData specificTimeData = FindSpecificTimeData(timeData.absoluteTime, index);
			if (specificTimeData == null)
				continue;

			// Heart rate (bpm) = 60 / R-R (seconds)
			float convertedNumber = 60 / (RRValues.get(index) / 1000f);
			specificTimeData.pulse = convertedNumber;
		}

		return true;
	}

	/**
	 * Searches a specific time slice.
	 * 
	 * @param absoluteTime
	 *           The absolute time of the time slice to find.
	 * @param offsetSeconds
	 *           The offset in number of seconds.
	 * @return The found time slice, null otherwise.
	 */
	private TimeData FindSpecificTimeData(long absoluteTime, int offsetSeconds) {
		long computedTime = absoluteTime + offsetSeconds * 1000;

		for (int index = 0; index < _sampleList.size(); ++index) {
			if (_sampleList.get(index).absoluteTime == computedTime)
				return _sampleList.get(index);
		}

		return null;
	}

	/**
	 * Attempts to retrieve and add speed data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 * @return True if successful, false otherwise.
	 */
	private boolean TryAddSpeedData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_SPEED)) != null) {
			timeData.speed = Util.parseFloat(value);
			return true;
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add cadence data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 * @return True if successful, false otherwise.
	 */
	private boolean TryAddCadenceData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_CADENCE)) != null) {
			timeData.cadence = Util.parseFloat(value) * 60.0f;
			return true;
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add barometric altitude data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 * @return True if successful, false otherwise.
	 */
	private boolean TryAddAltitudeData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_ALTITUDE)) != null) {
			timeData.absoluteAltitude = Util.parseFloat(value);
			return true;
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add power data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 * @return True if successful, false otherwise.
	 */
	private boolean TryAddPowerData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_POWER)) != null) {
			timeData.power = Util.parseFloat(value);
			return true;
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add power data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 * @return True if successful, false otherwise.
	 */
	private boolean TryAddDistanceData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_DISTANCE)) != null) {
			timeData.absoluteDistance = Util.parseFloat(value);
			return true;
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add power data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 * @return True if successful, false otherwise.
	 */
	private boolean TryAddTemperatureData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_TEMPERATURE)) != null) {
			timeData.temperature = Util.parseFloat(value) - Kelvin;
			return true;
		}
		return false;
	}

	/**
	 * Attempts to retrieve, process and add swimming data into an activity.
	 * 
	 * @param tour
	 *           A given tour.
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param currentSampleDate
	 *           The DateTime of the current data.
	 * @param previousTotalLengths
	 *           The previous SuuntoDataNames.TotalLengths value.
	 * @return The total number of pool lengths
	 */
	private boolean TryAddSwimmingData(
													List<SwimData> allSwimData,
													JSONObject currentSample,
													ZonedDateTime currentSampleDate,
													long absoluteTime, //TODO to rename better
													int previousTotalLengths) {
		boolean wasDataPopulated = false;
		//ILapInfo lap;
		int lapIndex = 0;
		JSONArray Events = (JSONArray) currentSample.get("Events");
		JSONObject array = (JSONObject) Events.get(0);
		JSONObject swimmingSample = (JSONObject) array.get(Swimming);

		SwimData previousSwimData = allSwimData.size() == 0 ? null : allSwimData.get(allSwimData.size() - 1);

		String swimmingType = TryRetrieveStringElementValue(
				swimmingSample,
				Type);

		switch (swimmingType) {
		case Stroke:
			++previousSwimData.swim_Strokes;
			wasDataPopulated = true;
			break;
		case Turn:

			String currentTotalLengthStr = TryRetrieveStringElementValue(
					swimmingSample,
					TotalLengths);

			int currentTotalLength = Integer.parseInt(currentTotalLengthStr);

			if (currentTotalLength > 0 &&
					currentTotalLength == previousTotalLengths)
				return false;

			SwimData swimData = new SwimData();
			swimData.swim_Strokes = 0;
			//If ArrayBegin==0 then we wait till next one

			// TODO ? 
			swimData.swim_LengthType = LengthType.ACTIVE.getValue();

			// Stroke Type
			String poolLengthStyle = TryRetrieveStringElementValue(
					swimmingSample,
					PoolLengthStyle);

			switch (poolLengthStyle) {
			case "Breaststroke":

				swimData.swim_StrokeStyle = SwimStroke.BREASTSTROKE.getValue();
				break;
			}

			// Length duration
			String poolLengthDurationValue = TryRetrieveStringElementValue(
					swimmingSample,
					PoolLengthDuration);

			float poolLengthDuration = Float.parseFloat(poolLengthDurationValue);
			// Converting the pool length duration to ms
			poolLengthDuration *= 1000;

			//TODO Take care of the case where we are on the first one
			long previousPoolLengthAbsoluteTime = 0;
			if (allSwimData.size() == 0)
				previousPoolLengthAbsoluteTime = absoluteTime;
			else
				previousPoolLengthAbsoluteTime = allSwimData.get(allSwimData.size() - 1).absoluteTime;

			swimData.absoluteTime = previousPoolLengthAbsoluteTime + Math.round(poolLengthDuration);
			allSwimData.add(swimData);
			wasDataPopulated = true;
			break;
		}

		//It is very likely that we are at the first lap and hence
		//there is no data to import.
		//if (currentTotalLength == 0)
		//		return currentTotalLength;
		//	if (activity.Laps.Count == 1)
		//	lapIndex = 0;
		//else
		//	lapIndex = activity.Laps.Count - 1;

		//lap = this.activity.Laps[lapIndex];

		// We initialize the total distance otherwise we can't increment
		// the distance
		//	if (lap.PoolLengths.Count == 0)
		//		lap.TotalDistanceMeters = 0f;
/*
 * if (TryRetrieveElementValue( swimmingSample, SuuntoDataNames.Type, out object type)) {
 * IPoolLengthInfo currentPoolLength = GetCurrentPoolLength(activity, lapIndex); switch
 * (type.ToString()) { case SuuntoDataNames.Turn: if (currentPoolLength == null) break; if
 * (TryRetrieveElementValue( swimmingSample, SuuntoDataNames.PoolLengthDuration, out object
 * durationValue)) { currentPoolLength.TotalTime = TimeSpan.Parse($"00:00:{durationValue:F2}"); } if
 * (TryRetrieveElementValue( swimmingSample, SuuntoDataNames.PoolLengthStyle, out object
 * poolLengthStyle)) { switch (poolLengthStyle.ToString()) { case SuuntoDataNames.Breaststroke:
 * currentPoolLength.StrokeType = SwimStroke.Type.Breaststroke; break; case Freestyle:
 * currentPoolLength.StrokeType = SwimStroke.Type.Freestyle; break; //TODO What are all the possible
 * swim styles ??? default: currentPoolLength.StrokeType = SwimStroke.Type.Unspecified; break; } }
 * if (TryRetrieveFloatElementValue( currentSample, SuuntoDataNames.Distance, out float
 * distanceValue)) { currentPoolLength.TotalDistanceMeters += distanceValue -
 * activity.TotalDistanceMetersEntered; activity.TotalDistanceMetersEntered = distanceValue;
 * lap.TotalDistanceMeters += currentPoolLength .TotalDistanceMeters; } break; case
 * SuuntoDataNames.Stroke: // If there is no previous pool length or if the // previous one is
 * finished (total time is not equal to // 0), we create a new one. if (currentPoolLength == null ||
 * currentPoolLength.TotalTime != TimeSpan.Zero) { currentPoolLength = lap.PoolLengths.Add(
 * currentSampleDate, new TimeSpan()); } ++currentPoolLength.StrokeCount; break; } }
 */
		return wasDataPopulated;//currentTotalLength;
	}

	/**
	 * Searches for an element and returns its value as a string.
	 * 
	 * @param token
	 *           The JSON token in which to look for a given element.
	 * @param elementName
	 *           The element name to look for in a JSON content.
	 * @return The element value, if found.
	 */
	private String TryRetrieveStringElementValue(JSONObject token, String elementName) {
		if (!token.toString().contains(elementName))
			return null;

		String result = null;
		try {
			result = token.get(elementName).toString();
		} catch (Exception e) {
		}
		if (result == "null") //$NON-NLS-1$
			return null;

		return result;
	}

	/**
	 * Searches for an element and returns its value as a list of integer.
	 * 
	 * @param token
	 *           The JSON token in which to look for a given element.
	 * @param elementName
	 *           The element name to look for in a JSON content.
	 * @return The element value, if found.
	 */
	private ArrayList<Integer> TryRetrieveIntegerListElementValue(JSONObject token, String elementName) {
		ArrayList<Integer> elementValues = new ArrayList<Integer>();
		String elements = TryRetrieveStringElementValue(token, elementName);

		if (elements == null)
			return elementValues;

		String[] stringValues = elements.split(",");
		for (int index = 0; index < stringValues.length; ++index) {
			Integer rrValue = Integer.parseInt(stringValues[index]);
			elementValues.add(rrValue);
		}
		return elementValues;

	}

	/**
	 * Fill swim data into tourdata.
	 *
	 * @param tourData
	 * @param allTourSwimData
	 */
	private void finalizeTour_SwimData(final TourData tourData, final List<SwimData> allTourSwimData) {

		// check if swim data are available
		if (allTourSwimData == null) {
			return;
		}

		final long tourStartTime = tourData.getTourStartTimeMS();

		final int swimDataSize = allTourSwimData.size();

		final short[] lengthType = new short[swimDataSize];
		final short[] cadence = new short[swimDataSize];
		final short[] strokes = new short[swimDataSize];
		final short[] strokeStyle = new short[swimDataSize];
		final int[] swimTime = new int[swimDataSize];

		tourData.swim_LengthType = lengthType;
		tourData.swim_Cadence = cadence;
		tourData.swim_Strokes = strokes;
		tourData.swim_StrokeStyle = strokeStyle;
		tourData.swim_Time = swimTime;

		boolean isSwimLengthType = false;
		boolean isSwimCadence = false;
		boolean isSwimStrokes = false;
		boolean isSwimStrokeStyle = false;
		boolean isSwimTime = false;

		for (int swimSerieIndex = 0; swimSerieIndex < allTourSwimData.size(); swimSerieIndex++) {

			final SwimData swimData = allTourSwimData.get(swimSerieIndex);

			final long absoluteSwimTime = swimData.absoluteTime;
			final short relativeSwimTime = (short) ((absoluteSwimTime - tourStartTime) / 1000);

			final short swimLengthType = swimData.swim_LengthType;
			short swimCadence = swimData.swim_Cadence;
			short swimStrokes = swimData.swim_Strokes;
			final short swimStrokeStyle = swimData.swim_StrokeStyle;

			/*
			 * Length type
			 */
			if (swimLengthType != Short.MIN_VALUE && swimLengthType > 0) {
				isSwimLengthType = true;
			}

			/*
			 * Cadence
			 */
			if (swimCadence == Short.MIN_VALUE) {
				swimCadence = 0;
			}
			if (swimCadence > 0) {
				isSwimCadence = true;
			}

			/*
			 * Strokes
			 */
			if (swimStrokes == Short.MIN_VALUE) {
				swimStrokes = 0;
			}
			if (swimStrokes > 0) {
				isSwimStrokes = true;
			}

			/*
			 * Stroke style
			 */
			if (swimStrokeStyle != Short.MIN_VALUE && swimStrokeStyle > 0) {
				isSwimStrokeStyle = true;
			}

			/*
			 * Swim time
			 */
			if (relativeSwimTime > 0) {
				isSwimTime = true;
			}

			lengthType[swimSerieIndex] = swimLengthType;
			cadence[swimSerieIndex] = swimCadence;
			strokes[swimSerieIndex] = swimStrokes;
			strokeStyle[swimSerieIndex] = swimStrokeStyle;
			swimTime[swimSerieIndex] = relativeSwimTime;
		}

		/*
		 * Cleanup data series
		 */
		if (isSwimLengthType == false) {
			tourData.swim_LengthType = null;
		}
		if (isSwimStrokes == false) {
			tourData.swim_Strokes = null;
		}
		if (isSwimStrokeStyle == false) {
			tourData.swim_StrokeStyle = null;
		}
		if (isSwimTime == false) {
			tourData.swim_Time = null;
		}

		// cadence is very special
		if (isSwimCadence) {
			// removed 'normal' cadence data serie when swim cadence is available
			tourData.setCadenceSerie(null);
		} else {
			tourData.swim_Cadence = null;
		}
	}
}
