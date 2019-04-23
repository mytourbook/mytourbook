package net.tourbook.device.suunto;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.swimming.SwimStroke;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.LengthType;
import net.tourbook.data.SwimData;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;

import org.eclipse.jface.preference.IPreferenceStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SuuntoJsonProcessor {

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
   private static final String Other           = "Other";
	private static final String	PoolLengthStyle		= "PrevPoolLengthStyle";
   private static final String TotalLengths    = "TotalLengths";
	private static final String	Stroke					= "Stroke";
	private static final String	Turn						= "Turn";
	private static final String	Type						= "Type";
   private static int          previousTotalLengths = 0;

	private ArrayList<TimeData>	_sampleList;
	private int							_lapCounter;
	final IPreferenceStore			_prefStore			= TourbookPlugin.getDefault().getPreferenceStore();

   /**
    * Attempts to retrieve and add HR data from the MoveSense HR belt to the current tour.
    *
    * @param currentSample
    *           The current sample data in JSON format.
    * @param currentSampleDate
    *           The date of the current data.
    */
   private void BuildRRDataList(final List<Integer> rrDataList, final String currentSample) {
      final JSONObject currentSampleJson = new JSONObject(currentSample);
      final ArrayList<Integer> RRValues = TryRetrieveIntegerListElementValue(
            currentSampleJson.getJSONObject(TAG_RR).toString(),
            TAG_DATA);

      if (RRValues.size() == 0) {
         return;
      }

      rrDataList.addAll(RRValues);
   }

	private void cleanUpActivity(final ArrayList<TimeData> activityData, final boolean isIndoorTour)
	{
	// Cleaning-up the processed entries as there should only be entries
      // every x seconds, no entries should be in between (entries with milliseconds).

      // Also, we first need to make sure that they truly are in chronological order.
      Collections.sort(activityData, new Comparator<TimeData>() {
         @Override
         public int compare(final TimeData firstTimeData, final TimeData secondTimeData) {
            // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
            return firstTimeData.absoluteTime > secondTimeData.absoluteTime ? 1 : (firstTimeData.absoluteTime < secondTimeData.absoluteTime) ? -1
                  : 0;
         }
      });

      final Iterator<TimeData> sampleListIterator = activityData.iterator();
      long previousAbsoluteTime = 0;
      while (sampleListIterator.hasNext()) {
         final TimeData currentTimeData = sampleListIterator.next();

         // Removing the entries that don't have GPS data
         // In the case where the activity is an indoor tour,
         // we remove the entries that don't have altitude data
         if (currentTimeData.marker == 0 &&
               (!isIndoorTour && currentTimeData.longitude == Double.MIN_VALUE && currentTimeData.latitude == Double.MIN_VALUE) ||
               (isIndoorTour && currentTimeData.absoluteAltitude == Float.MIN_VALUE) ||
               currentTimeData.absoluteTime == previousAbsoluteTime) {
            sampleListIterator.remove();
         }else {
            previousAbsoluteTime = currentTimeData.absoluteTime;
         }
      }

      // If the activity contains laps, we need to close the last lap.
      if (_lapCounter != 0) {
         final TimeData lastTimeData = activityData.get(activityData.size() - 1);
         lastTimeData.marker = 1;
         lastTimeData.markerLabel = Integer.toString(++_lapCounter);
      }
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
	public TourData ImportActivity(	final String jsonFileContent,
												final TourData activityToReUse,
												final ArrayList<TimeData> sampleListToReUse,
												final boolean isUnitTest) {
		_sampleList = new ArrayList<TimeData>();

		JSONArray samples = null;
		try {
			final JSONObject jsonContent = new JSONObject(jsonFileContent);
			samples = (JSONArray) jsonContent.get(TAG_SAMPLES);
		} catch (final JSONException ex) {
			StatusUtil.log(ex);
			return null;
		}

		final JSONObject firstSample = (JSONObject) samples.get(0);

		final TourData tourData = InitializeActivity(firstSample, activityToReUse, sampleListToReUse);

		if (tourData == null) {
         return null;
      }

		final boolean isIndoorTour = !jsonFileContent.contains(TAG_GPSALTITUDE);

		boolean isPaused = false;

		boolean reusePreviousTimeEntry;
		final Instant minInstant = Instant.ofEpochMilli(Long.MIN_VALUE);
		ZonedDateTime pauseStartTime = minInstant.atZone(ZoneOffset.UTC);
		final List<SwimData> _allSwimData = new ArrayList<>();
      final List<Integer> _allRRData = new ArrayList<>();
      long _rrDataStartTime = Integer.MIN_VALUE;

		for (int i = 0; i < samples.length(); ++i) {
			String currentSampleSml;
			String currentSampleData;
			String sampleTime;
			try {
				final JSONObject sample = samples.getJSONObject(i);
				if (!sample.toString().contains(TAG_TIMEISO8601)) {
               continue;
            }

				final String attributesContent = sample.get(TAG_ATTRIBUTES).toString();
				if (attributesContent == null || attributesContent == "") {
               continue;
            }

				final JSONObject currentSampleAttributes = new JSONObject(sample.get(TAG_ATTRIBUTES).toString());
				currentSampleSml = currentSampleAttributes.get(TAG_SUUNTOSML).toString();

				if (currentSampleSml.contains(TAG_SAMPLE)) {
               currentSampleData = new JSONObject(currentSampleSml).get(TAG_SAMPLE).toString();
            } else {
               currentSampleData = new JSONObject(currentSampleSml).toString();
            }

				sampleTime = sample.get(TAG_TIMEISO8601).toString();
			} catch (final Exception e) {
				StatusUtil.log(e);
				continue;
			}

			boolean wasDataPopulated = false;
			reusePreviousTimeEntry = false;
			TimeData timeData = null;

			ZonedDateTime currentZonedDateTime = ZonedDateTime.parse(sampleTime);
			currentZonedDateTime = currentZonedDateTime.truncatedTo(ChronoUnit.SECONDS);
			// Rounding to the nearest second
			if (Character.getNumericValue(sampleTime.charAt(20)) >= 5) {
            currentZonedDateTime = currentZonedDateTime.plusSeconds(1);
         }

         final long currentTime = currentZonedDateTime.toInstant().toEpochMilli();

         if (currentSampleData.toString().contains(TAG_RR)) {
            BuildRRDataList(_allRRData, currentSampleSml);

            if (_rrDataStartTime == Integer.MIN_VALUE) {
               _rrDataStartTime = currentTime;
            }

            continue;
         }

         // If the current time is before the actual tour start time, we ignore the data
         // Except if it's a R-R interval data as it can contain R-R data that belong to later
         // samples
         if (currentTime <= tourData.getTourStartTimeMS()) {
            continue;
         }

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
			if (isPaused && currentZonedDateTime.isAfter(pauseStartTime)) {
            continue;
         }

			if (currentSampleData.contains(TAG_LAP) &&
					(currentSampleData.contains(TAG_MANUAL) ||
							currentSampleData.contains(TAG_DISTANCE))) {
				timeData.marker = 1;
				timeData.markerLabel = Integer.toString(++_lapCounter);
				if (!reusePreviousTimeEntry) {
               _sampleList.add(timeData);
            }
			}

			// GPS point
			if (currentSampleData.contains(TAG_GPSALTITUDE) && currentSampleData.contains(TAG_LATITUDE)
					&& currentSampleData.contains(TAG_LONGITUDE)) {
				wasDataPopulated |= TryAddGpsData(currentSampleData, timeData, isUnitTest);
			}

			// Heart Rate
         wasDataPopulated |= TryAddHeartRateData(currentSampleData, timeData);

			// Speed
         wasDataPopulated |= TryAddSpeedData(currentSampleData, timeData);

			// Cadence
         wasDataPopulated |= TryAddCadenceData(currentSampleData, timeData);

			// Barometric Altitude
			if (_prefStore.getInt(IPreferences.ALTITUDE_DATA_SOURCE) == 1 ||
					isIndoorTour ||
					isUnitTest) {
            wasDataPopulated |= TryAddAltitudeData(currentSampleData, timeData);
			}

			// Power
         wasDataPopulated |= TryAddPowerData(currentSampleData, timeData);

			// Distance
			if (_prefStore.getInt(IPreferences.DISTANCE_DATA_SOURCE) == 1 ||
					isIndoorTour ||
					isUnitTest) {
            wasDataPopulated |= TryAddDistanceData(currentSampleData, timeData);
			}

			// Temperature
         wasDataPopulated |= TryAddTemperatureData(currentSampleData, timeData);

			//Swimming data
				wasDataPopulated |= TryAddSwimmingData(
						_allSwimData,
                  currentSampleData,
                  timeData.absoluteTime);

			if (wasDataPopulated && !reusePreviousTimeEntry) {
            _sampleList.add(timeData);
         }
		}

		// We clean-up the data series ONLY if we're not in a swimming activity
		if (_allSwimData.size() == 0) {
         cleanUpActivity(_sampleList, isIndoorTour);
		}

      TryComputeHeartRateData(_sampleList, _allRRData, _rrDataStartTime);

		tourData.createTimeSeries(_sampleList, true);

		tourData.finalizeTour_SwimData(tourData, _allSwimData);

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
	private TourData InitializeActivity(final JSONObject firstSample,
													final TourData activityToReUse,
													final ArrayList<TimeData> sampleListToReUse) {
		TourData tourData = new TourData();
		final String firstSampleAttributes = firstSample.get(TAG_ATTRIBUTES).toString();

		if (firstSampleAttributes.contains(TAG_LAP) &&
				firstSampleAttributes.contains(TAG_TYPE) &&
				firstSampleAttributes.contains(TAG_START)) {

			final ZonedDateTime startTime = ZonedDateTime.parse(firstSample.get(TAG_TIMEISO8601).toString());
			tourData.setTourStartTime(startTime);

		} else if (activityToReUse != null) {

			final Set<TourMarker> tourMarkers = activityToReUse.getTourMarkers();
			for (final TourMarker tourMarker : tourMarkers) {
				_lapCounter = Integer.valueOf(tourMarker.getLabel());
			}
			activityToReUse.setTourMarkers(new HashSet<TourMarker>());

			tourData = activityToReUse;
			_sampleList = sampleListToReUse;
			tourData.clearComputedSeries();
			tourData.timeSerie = null;

		} else {
         return null;
      }

		return tourData;

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
   private boolean TryAddAltitudeData(final String currentSample, final TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_ALTITUDE)) != null) {
			timeData.absoluteAltitude = Util.parseFloat(value);
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
   private boolean TryAddCadenceData(final String currentSample, final TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_CADENCE)) != null) {
			timeData.cadence = Util.parseFloat(value) * 60.0f;
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
   private boolean TryAddDistanceData(final String currentSample, final TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_DISTANCE)) != null) {
			timeData.absoluteDistance = Util.parseFloat(value);
			return true;
		}
		return false;
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
   private boolean TryAddGpsData(final String currentSample, final TimeData timeData, final boolean isUnitTest) {
		try {
         final JSONObject currentSampleJson = new JSONObject(currentSample);
         final float latitude = Util.parseFloat(currentSampleJson.get(TAG_LATITUDE).toString());
         final float longitude = Util.parseFloat(currentSampleJson.get(TAG_LONGITUDE).toString());
         final float altitude = Util.parseFloat(currentSampleJson.get(TAG_GPSALTITUDE).toString());

			timeData.latitude = (latitude * 180) / Math.PI;
			timeData.longitude = (longitude * 180) / Math.PI;

			// GPS altitude
			if (_prefStore.getInt(IPreferences.ALTITUDE_DATA_SOURCE) == 0 ||
					isUnitTest) {
				timeData.absoluteAltitude = altitude;
			}

			return true;
		} catch (final Exception e) {
			StatusUtil.log(e);
		}
		return false;
	}

   /**
    * Attempts to retrieve and add HR data (from the optical sensor) to the current tour.
    *
    * @param currentSample
    *           The current sample data in JSON format.
    * @param sampleList
    *           The tour's time serie.
    * @return True if successful, false otherwise.
    */
   private boolean TryAddHeartRateData(final String currentSample, final TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_HR)) != null) {
			timeData.pulse = Util.parseFloat(value) * 60.0f;
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
   private boolean TryAddPowerData(final String currentSample, final TimeData timeData) {
      String value = null;
      if ((value = TryRetrieveStringElementValue(currentSample, TAG_POWER)) != null) {
         timeData.power = Util.parseFloat(value);
         return true;
      }
      return false;
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
   private boolean TryAddSpeedData(final String currentSample, final TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, TAG_SPEED)) != null) {
			timeData.speed = Util.parseFloat(value);
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
	private boolean TryAddSwimmingData(	final List<SwimData> allSwimData,
                                      final String currentSample,
                                      final long currentSampleDate) {

      if (!currentSample.contains(Swimming)) {
         return false;
      }
      boolean wasDataPopulated = false;
      final JSONArray Events = (JSONArray) new JSONObject(currentSample).get("Events");
		final JSONObject array = (JSONObject) Events.get(0);
      final String swimmingSample = ((JSONObject) array.get(Swimming)).toString();

		final SwimData previousSwimData = allSwimData.size() == 0 ? null : allSwimData.get(allSwimData.size() - 1);

		final String swimmingType = TryRetrieveStringElementValue(
            swimmingSample,
				Type);

		switch (swimmingType) {
		case Stroke:
			++previousSwimData.swim_Strokes;
         wasDataPopulated = true;
			break;
		case Turn:
         final String currentTotalLengthsString = TryRetrieveStringElementValue(
               swimmingSample,
               TotalLengths);
         final int currentTotalLengths = Integer.parseInt(currentTotalLengthsString);
         // If the current total length equals the previous
         // total length, it was likely a "rest" and we retrieve
         // the very last pool length in order to create a
         // rest lap.

         if (currentTotalLengths > 0 && currentTotalLengths == previousTotalLengths) {
            if (previousSwimData != null) {
               previousSwimData.swim_LengthType = LengthType.IDLE.getValue();
            }
         }

			final SwimData swimData = new SwimData();
			swimData.swim_Strokes = 0;
			swimData.swim_LengthType = LengthType.ACTIVE.getValue();

         if (previousSwimData != null) {
         // Swimming Type
			final String poolLengthStyle = TryRetrieveStringElementValue(
					swimmingSample,
					PoolLengthStyle);

			switch (poolLengthStyle) {
			case Breaststroke:
               previousSwimData.swim_StrokeStyle = SwimStroke.BREASTSTROKE.getValue();
				break;
			case Freestyle:
               previousSwimData.swim_StrokeStyle = SwimStroke.FREESTYLE.getValue();
				break;
         case Other:
               break;
			}
         }

         swimData.absoluteTime = currentSampleDate;
			allSwimData.add(swimData);

         wasDataPopulated = true;
         previousTotalLengths = currentTotalLengths;
			break;
		}

      return wasDataPopulated;
	}

	/**
    * Attempts to retrieve and add power data to the current tour.
    *
    * @param currentSample
    *           The current sample data.
    * @param sampleList
    *           The tour's time serie.
    * @return True if successful, false otherwise.
    */
	private boolean TryAddTemperatureData(final String currentSample, final TimeData timeData) {
		String value = null;
      if ((value = TryRetrieveStringElementValue(currentSample, TAG_TEMPERATURE)) != null) {
         timeData.temperature = (float) (Util.parseFloat(value) + net.tourbook.math.Fmath.T_ABS);
			return true;
		}
		return false;
	}

   /**
    * Computed the heart rate data from the R-R intervals generated by the
    * MoveSense HR belt to the current tour.
    *
    * @param activityData
    *           The current activity.
    * @param rrDataList
    *           The list of R-R intervals for the given activity.
    */
   private void TryComputeHeartRateData(final ArrayList<TimeData> activityData,
                                        final List<Integer> rrDataList,
                                        final long rrDataStartTime) {
      if (rrDataList.size() == 0) {
         return;
      }

      long currentRRSum = 0;
      int lastRRIndex = 0;
      long RRsum = rrDataStartTime;
      int currentRRindex = -1;

      for (int currentActivityIndex = 0; currentActivityIndex < activityData.size() &&
            currentRRindex < rrDataList.size() - 1; ++currentActivityIndex) {

         for (; RRsum < activityData.get(currentActivityIndex).absoluteTime &&
               currentRRindex < rrDataList.size() - 1;) {
            ++currentRRindex;
            currentRRSum += rrDataList.get(currentRRindex);
            RRsum += rrDataList.get(currentRRindex);
         }

         if (currentRRindex >= lastRRIndex) {
         // Heart rate (bpm) = 60 / R-R (seconds)
         // If the RR value is the sum of several intervals, we average it
         final float convertedNumber = 60 / (currentRRSum / 1000f) * (currentRRindex - lastRRIndex + 1);
         activityData.get(currentActivityIndex).pulse = convertedNumber;

         currentRRSum = rrDataList.get(currentRRindex);
         lastRRIndex = currentRRindex;
         }
      }
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
   private ArrayList<Integer> TryRetrieveIntegerListElementValue(final String token, final String elementName) {
		final ArrayList<Integer> elementValues = new ArrayList<Integer>();
		final String elements = TryRetrieveStringElementValue(token, elementName);

		if (elements == null) {
         return elementValues;
      }

		final String[] stringValues = elements.split(",");
		for (final String stringValue : stringValues) {
			final Integer rrValue = Integer.parseInt(stringValue);
			elementValues.add(rrValue);
		}
		return elementValues;

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
   private String TryRetrieveStringElementValue(final String token, final String elementName) {
		if (!token.toString().contains(elementName)) {
         return null;
      }

		String result = null;
		try {
         result = new JSONObject(token).get(elementName).toString();
		} catch (final Exception e) {
		}
		if (result == "null") {
         return null;
      }

		return result;
	}
}
