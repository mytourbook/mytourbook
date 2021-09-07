/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.device.suunto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.stream.Stream;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.tour.TourLogManager;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SuuntoQuestSAXHandler extends DefaultHandler {

   // root tags
   private static final String TAG_ROOT_MOVESCOUNT     = "MovesCount";    //$NON-NLS-1$
   private static final String TAG_ROOT_EXERCISE_MODES = "ExerciseModes"; //$NON-NLS-1$
   private static final String TAG_ROOT_DEVICE         = "Device";        //$NON-NLS-1$
   private static final String TAG_ROOT_MOVES          = "Moves";         //$NON-NLS-1$

   // MovesCount
   private static final String ATTR_TIMEZONE = "TimeZone"; //$NON-NLS-1$

   // device tags
   private static final String TAG_DISTANCE_UNIT = "DistanceUnit"; //$NON-NLS-1$
   private static final String TAG_VERSION       = "Version";      //$NON-NLS-1$
   private static final String TAG_WEIGHT        = "Weight";       //$NON-NLS-1$
   private static final String TAG_WEIGHT_UNIT   = "WeightUnit";   //$NON-NLS-1$

   // exercise mode tags
   private static final String TAG_EXERCISE_MODE    = "ExerciseMode";    //$NON-NLS-1$
   private static final String TAG_ACTIVITY         = "Activity";        //$NON-NLS-1$
   private static final String TAG_NAME             = "Name";            //$NON-NLS-1$
   private static final String TAG_SEARCHED_DEVICES = "SearchedDevices"; //$NON-NLS-1$

   // move tags
   // -- samples tags
   private static final String           TAG_MOVE       = "Move";       //$NON-NLS-1$
   private static final String           TAG_SAMPLES    = "Samples";    //$NON-NLS-1$
   private static final String           TAG_CADENCE    = "Cadence";    //$NON-NLS-1$
   private static final String           TAG_DISTANCE   = "Distance";   //$NON-NLS-1$
   private static final String           TAG_HR         = "HR";         //$NON-NLS-1$
   // -- marks tags
   private static final String           TAG_MARKS      = "Marks";      //$NON-NLS-1$
   private static final String           TAG_MARK       = "Mark";       //$NON-NLS-1$
   private static final String           TAG_MARK_INDEX = "Index";      //$NON-NLS-1$
   // -- header tags
   private static final String           TAG_CALORIES   = "Calories";   //$NON-NLS-1$
   private static final String           TAG_HEADER     = "Header";     //$NON-NLS-1$
   private static final String           TAG_SAMPLERATE = "SampleRate"; //$NON-NLS-1$
   private static final String           TAG_TIME       = "Time";       //$NON-NLS-1$

   private static final SimpleDateFormat TIME_FORMAT_SSS;
   static {

      TIME_FORMAT_SSS = new SimpleDateFormat("HH:mm:ss.SSS"); //$NON-NLS-1$
      TIME_FORMAT_SSS.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
   }
   //
   private Map<Long, TourData>     _alreadyImportedTours;
   private Map<Long, TourData>     _newlyImportedTours;
   private TourbookDevice          _device;

   private String                  _importFilePath;
   private ArrayList<TourType>     _allTourTypes;
   //
   private ArrayList<TimeData>     _sampleList        = new ArrayList<>();
   private float[]                 _cadenceData;

   private float[]                 _distanceData;
   private float[]                 _pulseData;
   private TimeData                _markerData;

   private int                     _markIndex;
   private ArrayList<TimeData>     _markerList        = new ArrayList<>();

   private ExerciseMode            _exerciseModeData;
   private ArrayList<ExerciseMode> _exerciseModesList = new ArrayList<>();

   private boolean                 _isImported;
   private StringBuilder           _characters        = new StringBuilder();
   private boolean                 _isInActivity;
   private boolean                 _isInDevice;
   private boolean                 _isInExerciseMode;
   private boolean                 _isInExerciseModes;
   private boolean                 _isInHeader;
   private boolean                 _isInMarks;
   private boolean                 _isInMark;
   private boolean                 _isInMove;
   private boolean                 _isInMoves;
   private boolean                 _isInName;
   private boolean                 _isInSamples;
   private boolean                 _isInRootMovesCount;
   private boolean                 _isInCadence;
   private boolean                 _isInCalories;
   private boolean                 _isInDistance;
   private boolean                 _isInDistanceUnit;
   private boolean                 _isInHR;
   private boolean                 _isInMarkTime;
   private boolean                 _isInSampleRate;
   private boolean                 _isInSearchedDevices;
   private boolean                 _isInTime;
   private boolean                 _isInVersion;

   private boolean                 _isInWeight;
   private boolean                 _isInWeightUnit;
   private int                     _tourActivity;
   private int                     _tourCalories;
   private String                  _deviceVersion;
   private String                  _distanceUnit;
   private short                   _tourSampleRate;
   private LocalDateTime           _tourStartTime;
   private int                     _tourTimezone;

   private float                   _weight;

   private String                  _weightUnit;

   public class ExerciseMode {
      public int    activity;
      public String name;
      public String searchedDevices;
   }

   public SuuntoQuestSAXHandler(final TourbookDevice deviceDataReader,
                                final String importFileName,
                                final Map<Long, TourData> alreadyImportedTours,
                                final Map<Long, TourData> newlyImportedTours) {

      _device = deviceDataReader;
      _importFilePath = importFileName;
      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;
      _allTourTypes = TourDatabase.getAllTourTypes();
   }

   /**
    * Assigns, to a given tour, additional data such as calories, sensors indicators and tour type
    *
    * @param tourData
    *           A given tour data
    */
   private void assignAdditionalData(final TourData tourData) {

      final ExerciseMode exerciseMode = _exerciseModesList.stream()
            .filter(e -> e.activity == _tourActivity)
            .findFirst()
            .orElse(null);

      if (exerciseMode == null) {
         return;
      }

      final TourType tourType = _allTourTypes.stream()
            .filter(t -> t.getName().equalsIgnoreCase(exerciseMode.name))
            .findFirst()
            .orElse(null);

      if (tourType != null) {
         tourData.setTourType(tourType);
      }

      tourData.setIsPulseSensorPresent(exerciseMode.searchedDevices.contains(TAG_HR));
      tourData.setIsDistanceFromSensor(exerciseMode.searchedDevices.contains("POD")); //$NON-NLS-1$
   }

   @Override
   public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

      if (_isInCadence
            || _isInActivity
            || _isInCalories
            || _isInDistance
            || _isInDistanceUnit
            || _isInHR
            || _isInMarkTime
            || _isInName
            || _isInSampleRate
            || _isInSearchedDevices
            || _isInVersion
            || _isInWeight
            || _isInWeightUnit
            || _isInTime
      
      ) {
         _characters.append(chars, startIndex, length);
      }
   }

   private float[] convertStringToFloatArray(final String values) {

      if (StringUtils.isNullOrEmpty(values)) {
         return null;
      }

      final String[] individualValues = values.split(UI.SPACE1);
      final float[] serie = new float[individualValues.length];

      try (Scanner scanner = new Scanner(values)) {

         for (int i = 0; i < serie.length; i++) {
            if (!scanner.hasNextInt()) {
               break;
            }
            serie[i] = scanner.nextFloat();
         }
      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return serie;
   }

   public void dispose() {

      _sampleList.clear();
      _markerList.clear();
      _exerciseModesList.clear();
   }

   @Override
   public void endElement(final String uri, final String localName, final String name) throws SAXException {

      if (_isInDevice) {

         endElement_InDevice(name);

      } else if (_isInExerciseModes) {

         endElement_InExerciseModes(name);

      } else if (_isInSamples) {

         endElement_InSamples(name);

      } else if (_isInMarks) {

         endElement_InMarks(name);

      } else if (_isInHeader) {

         endElement_InHeader(name);

      }

      if (name.equals(TAG_HEADER)) {

         _isInHeader = false;

      } else if (name.equals(TAG_MARK)) {

         _isInMark = false;

         _markerList.add(_markerData);
         _markerData = null;

      } else if (name.equals(TAG_MARKS)) {

         _isInMarks = false;

      } else if (name.equals(TAG_SAMPLES)) {

         _isInSamples = false;

      } else if (name.equals(TAG_MOVE)) {

         _isInMove = false;
         finalizeTour();

         _tourActivity = -1;

      } else if (name.equals(TAG_ROOT_MOVES)) {

         _isInMoves = false;

      } else if (name.equals(TAG_EXERCISE_MODE)) {

         _isInExerciseMode = false;

         _exerciseModesList.add(_exerciseModeData);
         _exerciseModeData = null;

      } else if (name.equals(TAG_ROOT_EXERCISE_MODES)) {

         _isInExerciseModes = false;

      } else if (name.equals(TAG_ROOT_DEVICE)) {

         _isInDevice = false;

      } else if (name.equals(TAG_ROOT_MOVESCOUNT)) {

         _isInRootMovesCount = false;

      }
   }

   private void endElement_InDevice(final String name) {

      if (name.equals(TAG_VERSION)) {

         _isInVersion = false;

         _deviceVersion = _characters.toString();

      } else if (name.equals(TAG_WEIGHT)) {

         _isInWeight = false;

         _weight = Float.valueOf(_characters.toString());

      } else if (name.equals(TAG_WEIGHT_UNIT)) {

         _isInWeightUnit = false;

         _weightUnit = _characters.toString();

      } else if (name.equals(TAG_DISTANCE_UNIT)) {

         _isInDistanceUnit = false;

         _distanceUnit = _characters.toString();

      }
   }

   private void endElement_InExerciseModes(final String name) {
      if (name.equals(TAG_ACTIVITY)) {

         _isInActivity = false;

         _exerciseModeData.activity = Integer.valueOf(_characters.toString());

      } else if (name.equals(TAG_NAME)) {

         _isInName = false;

         _exerciseModeData.name = _characters.toString();

      } else if (name.equals(TAG_SEARCHED_DEVICES)) {

         _isInSearchedDevices = false;

         _exerciseModeData.searchedDevices = _characters.toString();

      }
   }

   private void endElement_InHeader(final String name) {

      if (name.equals(TAG_ACTIVITY)) {

         _isInActivity = false;

         _tourActivity = Integer.valueOf(_characters.toString());

      } else if (name.equals(TAG_CALORIES)) {

         _isInCalories = false;

         _tourCalories = Integer.valueOf(_characters.toString()) * 1000;

      } else if (name.equals(TAG_TIME)) {

         _isInTime = false;

         final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$

         _tourStartTime = LocalDateTime.parse(_characters.toString(), formatter);

      } else if (name.equals(TAG_SAMPLERATE)) {

         _isInSampleRate = false;

         _tourSampleRate = Short.valueOf(_characters.toString());
      }
   }

   private void endElement_InMarks(final String name) {
      if (name.equals(TAG_TIME)) {

         _isInMarkTime = false;

         try {
            final long time = TIME_FORMAT_SSS.parse(_characters.toString()).getTime();
            final int timeInSeconds = (int) time / 1000;
            final float milliSeconds = (float) (time - timeInSeconds * 1000) / 1000;
            _markerData.relativeTime = timeInSeconds + Math.round(milliSeconds);
         } catch (final ParseException e) {
            TourLogManager.log_ERROR(e.getMessage() + " in " + _importFilePath); //$NON-NLS-1$
         }

         //If existing, we need to use the previous marker relative time
         //to determine the current marker's relative time
         if (_markIndex > 0) {
            _markerData.relativeTime += _markerList.get(_markIndex - 1).relativeTime;
         }

      } else if (name.equals(TAG_CADENCE)) {

         _isInCadence = false;

         _markerData.cadence = Float.valueOf(_characters.toString());

      } else if (name.equals(TAG_DISTANCE)) {

         _isInDistance = false;

         _markerData.absoluteDistance = Float.valueOf(_characters.toString());

         //If existing, we need to use the previous marker absolute distance
         //to determine the current marker's absolute distance
         if (_markIndex > 0) {
            _markerData.absoluteDistance += _markerList.get(_markIndex - 1).absoluteDistance;
         }

      } else if (name.equals(TAG_HR)) {

         _isInHR = false;

         _markerData.pulse = Float.valueOf(_characters.toString());

      }
   }

   private void endElement_InSamples(final String name) {

      if (name.equals(TAG_CADENCE)) {

         _isInCadence = false;

         _cadenceData = convertStringToFloatArray(_characters.toString());

      } else if (name.equals(TAG_DISTANCE)) {

         _isInDistance = false;

         _distanceData = convertStringToFloatArray(_characters.toString());

      } else if (name.equals(TAG_HR)) {

         _isInHR = false;

         _pulseData = convertStringToFloatArray(_characters.toString());

      }
   }

   /**
    * Fills the necessary data for the samples :
    * computes {@see TimeData#absoluteTime} for each {@link TimeData}, merges markers with the
    * samples
    * And inserts the data series into the sample list.
    *
    * @param tourStartTime
    *           The tour start time that is used to compute the {@see TimeData#absoluteTime}
    */
   private void finalizeSamples() {

      //Inserting
      int relativeTime = 0;

      int maxIndex = Math.max(_distanceData.length, _cadenceData.length);
      maxIndex = Math.max(maxIndex, _pulseData.length);

      for (int index = 0; index < maxIndex; ++index) {

         final TimeData timeData = new TimeData();

         if (index < _cadenceData.length) {
            timeData.cadence = _cadenceData[index];
         }
         if (index < _pulseData.length) {
            timeData.pulse = _pulseData[index];
         }

         if (index < _distanceData.length) {
            timeData.distance = _distanceData[index];
         }

         if (index > 0) {
            relativeTime += _tourSampleRate;
            timeData.relativeTime = relativeTime;
            timeData.time = _tourSampleRate;
         }

         _sampleList.add(timeData);
      }

      mergeMarkers();

      //Converting the distance data to meters if needed
      if (_distanceUnit.equalsIgnoreCase("mile")) { //$NON-NLS-1$
         _sampleList.forEach(s -> s.distance *= UI.UNIT_MILE);
      }

      //We sort the sample lists as it could be out of order if we added markers above
      Collections.sort(_sampleList, (left, right) -> left.relativeTime - right.relativeTime);
   }

   private void finalizeTour() {

      // check if data are available
      if (_cadenceData == null &&
            _distanceData == null &&
            _pulseData == null) {
         return;
      }

      // create data object for each tour
      final TourData tourData = new TourData();

      /*
       * set tour start date/time
       */
      final ZoneOffset zoneOffset = ZoneOffset.ofHours(_tourTimezone);
      final ZonedDateTime tourStartTime = ZonedDateTime.ofInstant(
            _tourStartTime,
            zoneOffset,
            TimeTools.UTC);
      tourData.setTourStartTime(tourStartTime);

      finalizeSamples();

      assignAdditionalData(tourData);

      tourData.setDeviceTimeInterval(_tourSampleRate);
      tourData.setImportFilePath(_importFilePath);
      tourData.setCalories(_tourCalories);
      tourData.setBodyWeight(_weightUnit.equalsIgnoreCase(UI.UNIT_WEIGHT_KG) ? _weight : _weight / UI.UNIT_KILOGRAM_TO_POUND);

      tourData.setDeviceId(_device.deviceId);
      tourData.setDeviceName(_device.visibleName);
      tourData.setDeviceFirmwareVersion(_deviceVersion == null ? UI.EMPTY_STRING : _deviceVersion);

      tourData.createTimeSeries(_sampleList, true);

      // after all data are added, the tour id can be created
      final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_SUUNTOQUEST);
      final Long tourId = tourData.createTourId(uniqueId);

      // check if the tour is already imported
      if (!_alreadyImportedTours.containsKey(tourId)) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, tourData);

         // create additional data
         tourData.setTourDeviceTime_Recorded(tourData.getTourDeviceTime_Elapsed());
         tourData.computeTourMovingTime();
         tourData.computeComputedValues();
      }

      _isImported = true;

      //We clear the data for the potential next tours
      dispose();
   }

   /**
    * Compute the total distance from the start of the samples to a specific index.
    *
    * @param endIndex
    *           A given index
    * @return
    *         The sum of the distances
    */
   private float getSamplesRangeAbsoluteDistance(final int endIndex) {

      final Stream<TimeData> samplesRange = _sampleList.stream().skip(0).limit(endIndex + 1);

      return samplesRange.map(sample -> sample.distance)
            .reduce(0f, Float::sum);
   }

   /**
    * @return Returns <code>true</code> when a tour was imported
    */
   public boolean isImported() {
      return _isImported;
   }

   /**
    * Merge Marker data into tour data by time.
    * <p>
    * Merge is necessary because there are separate time slices for markers.
    */
   private void mergeMarkers() {

      for (final TimeData marker : _markerList) {
         //We search for a sample that is at the same point in time
         final TimeData equivalentSample = _sampleList.stream()
               .filter(s -> s.relativeTime == marker.relativeTime)
               .findFirst()
               .orElse(null);

         //If it was found, we mark it as a marker
         if (equivalentSample != null) {
            equivalentSample.marker = 1;
            equivalentSample.markerLabel = marker.markerLabel;

            //If the current sample has no distance, we replace it with the one from the marker
            if (equivalentSample.distance == 0f &&
                  marker.absoluteDistance > 0) {
               final int sampleIndex = _sampleList.indexOf(equivalentSample);

               final float totalDistance = getSamplesRangeAbsoluteDistance(sampleIndex - 1);
               equivalentSample.distance = marker.absoluteDistance > totalDistance ? marker.absoluteDistance - totalDistance : 0f;
            }
            if (equivalentSample.pulse == 0) {
               equivalentSample.pulse = marker.pulse;
            }
            if (equivalentSample.cadence == 0) {
               equivalentSample.cadence = marker.cadence;
            }

         } else {
            //If that doesn't exist, we insert the marker in the sample list and update the 2 adjacent elements
            TimeData closestSample = _sampleList.stream()
                  .filter(s -> s.relativeTime > marker.relativeTime)
                  .findFirst()
                  .orElse(null);

            marker.marker = 1;

            if (closestSample == null) {
               //This marker is the last sample
               closestSample = _sampleList.get(_sampleList.size() - 1);
               marker.time = marker.relativeTime - closestSample.relativeTime;
               final float totalDistance = getSamplesRangeAbsoluteDistance(_sampleList.size() - 1);
               marker.distance = marker.absoluteDistance - totalDistance;

               _sampleList.add(marker);
            } else {

               final int closestSampleIndex = _sampleList.indexOf(closestSample);

               closestSample.time = closestSample.relativeTime - marker.relativeTime;

               float totalDistance = getSamplesRangeAbsoluteDistance(closestSampleIndex);
               closestSample.distance = totalDistance - marker.absoluteDistance;

               closestSample = _sampleList.get(closestSampleIndex - 1);
               marker.time = marker.relativeTime - closestSample.relativeTime;

               totalDistance = getSamplesRangeAbsoluteDistance(closestSampleIndex - 1);
               marker.distance = marker.absoluteDistance - totalDistance;

               _sampleList.add(closestSampleIndex, marker);
            }
         }
      }
   }

   @Override
   public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
         throws SAXException {

      if (_isInRootMovesCount) {

         if (_isInDevice) {

            startElement_InDevice(name);

         } else if (_isInExerciseModes) {

            if (_isInExerciseMode) {

               startElement_InExerciseMode(name);

            } else if (name.equals(TAG_EXERCISE_MODE)) {

               _isInExerciseMode = true;

               // create new time items
               _exerciseModeData = new ExerciseMode();

            }

         } else if (_isInMoves) {

            if (_isInMove) {

               if (_isInSamples) {

                  startElement_InSamples(name);

               } else if (_isInMarks) {

                  if (_isInMark) {

                     startElement_InMark(name);

                  } else if (name.equals(TAG_MARK)) {

                     _isInMark = true;

                     // create new time items
                     _markerData = new TimeData();
                     _markIndex = Integer.valueOf(attributes.getValue(TAG_MARK_INDEX));
                     _markerData.markerLabel = String.valueOf(_markIndex + 1);

                  }
               } else if (_isInHeader) {

                  startElement_InHeader(name);

               } else if (name.equals(TAG_SAMPLES)) {

                  _isInSamples = true;

               } else if (name.equals(TAG_MARKS)) {

                  _isInMarks = true;

               } else if (name.equals(TAG_HEADER)) {

                  _isInHeader = true;

               }
            } else if (name.equals(TAG_MOVE)) {

               _isInMove = true;

            }
         } else if (name.equals(TAG_ROOT_DEVICE)) {

            _isInDevice = true;

         } else if (name.equals(TAG_ROOT_EXERCISE_MODES)) {

            _isInExerciseModes = true;

         } else if (name.equals(TAG_ROOT_MOVES)) {

            _isInMoves = true;

         }
      } else if (name.equals(TAG_ROOT_MOVESCOUNT)) {

         _isInRootMovesCount = true;

         // The value is in minutes so we transform it into hours
         _tourTimezone = Integer.valueOf(attributes.getValue(ATTR_TIMEZONE)) / 60;

      }
   }

   private void startElement_InDevice(final String name) {

      boolean isData = true;

      if (name.equals(TAG_VERSION)) {

         _isInVersion = true;

      } else if (name.equals(TAG_WEIGHT)) {

         _isInWeight = true;

      } else if (name.equals(TAG_WEIGHT_UNIT)) {

         _isInWeightUnit = true;

      } else if (name.equals(TAG_DISTANCE_UNIT)) {

         _isInDistanceUnit = true;

      } else {
         isData = false;
      }

      if (isData) {

         // clear char buffer
         _characters.delete(0, _characters.length());
      }
   }

   private void startElement_InExerciseMode(final String name) {
      boolean isData = true;

      if (name.equals(TAG_ACTIVITY)) {

         _isInActivity = true;

      } else if (name.equals(TAG_NAME)) {

         _isInName = true;

      } else if (name.equals(TAG_SEARCHED_DEVICES)) {

         _isInSearchedDevices = true;

      } else {
         isData = false;
      }

      if (isData) {

         // clear char buffer
         _characters.delete(0, _characters.length());
      }

   }

   private void startElement_InHeader(final String name) {

      boolean isData = true;

      if (name.equals(TAG_ACTIVITY)) {

         _isInActivity = true;

      } else if (name.equals(TAG_CALORIES)) {

         _isInCalories = true;

      } else if (name.equals(TAG_TIME)) {

         _isInTime = true;

      } else if (name.equals(TAG_SAMPLERATE)) {

         _isInSampleRate = true;

      } else {
         isData = false;
      }

      if (isData) {

         // clear char buffer
         _characters.delete(0, _characters.length());
      }
   }

   private void startElement_InMark(final String name) {

      boolean isData = true;

      if (name.equals(TAG_TIME)) {

         _isInMarkTime = true;

      } else if (name.equals(TAG_CADENCE)) {

         _isInCadence = true;

      } else if (name.equals(TAG_DISTANCE)) {

         _isInDistance = true;

      } else if (name.equals(TAG_HR)) {

         _isInHR = true;

      } else {
         isData = false;
      }

      if (isData) {

         // clear char buffer
         _characters.delete(0, _characters.length());
      }

   }

   private void startElement_InSamples(final String name) {

      boolean isData = true;

      if (name.equals(TAG_CADENCE)) {

         _isInCadence = true;

      } else if (name.equals(TAG_DISTANCE)) {

         _isInDistance = true;

      } else if (name.equals(TAG_HR)) {

         _isInHR = true;

      } else {
         isData = false;
      }

      if (isData) {

         // clear char buffer
         _characters.delete(0, _characters.length());
      }
   }
}
