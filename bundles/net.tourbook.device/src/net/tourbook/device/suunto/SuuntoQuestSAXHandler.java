/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard and Contributors
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.TourbookDevice;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SuuntoQuestSAXHandler extends DefaultHandler {
//TODO FB isDistanceFromSensor
//TODO time of markers
//TODO FB t with imperial mile distance file (modify manually)

   // root tags
   private static final String TAG_DEVICE          = "Device";     //$NON-NLS-1$
   private static final String TAG_HEADER          = "Header";     //$NON-NLS-1$
   private static final String TAG_ROOT_MOVESCOUNT = "MovesCount"; //$NON-NLS-1$
   private static final String TAG_MARKS           = "Marks";      //$NON-NLS-1$
   private static final String TAG_MARK            = "Mark";       //$NON-NLS-1$
   private static final String TAG_MOVES           = "Moves";      //$NON-NLS-1$
   private static final String TAG_MOVE            = "Move";       //$NON-NLS-1$
   private static final String TAG_SAMPLES         = "Samples";    //$NON-NLS-1$

   // header tags
   private static final String TAG_CALORIES   = "Calories";   //$NON-NLS-1$
   private static final String TAG_SAMPLERATE = "SampleRate"; //$NON-NLS-1$
   private static final String TAG_TIME       = "Time";       //$NON-NLS-1$

   // device tags
   private static final String TAG_DISTANCE_UNIT = "DistanceUnit"; //$NON-NLS-1$
   private static final String TAG_VERSION       = "Version";      //$NON-NLS-1$
   private static final String TAG_WEIGHT        = "Weight";       //$NON-NLS-1$
   private static final String TAG_WEIGHT_UNIT   = "WeightUnit";   //$NON-NLS-1$

   // move tags
   private static final String TAG_CADENCE  = "Cadence";  //$NON-NLS-1$
   private static final String TAG_DISTANCE = "Distance"; //$NON-NLS-1$
   private static final String TAG_HR       = "HR";       //$NON-NLS-1$

   // mark tags
   private static final String TAG_MARK_INDEX = "Index";    //$NON-NLS-1$
   private static final String TAG_MARK_TIME  = "Time";     //$NON-NLS-1$

   private static final String ATTR_TIMEZONE  = "TimeZone"; //$NON-NLS-1$

   //
   private HashMap<Long, TourData> _alreadyImportedTours;
   private HashMap<Long, TourData> _newlyImportedTours;
   private TourbookDevice          _device;
   private String                  _importFilePath;
   //

   private ArrayList<TimeData> _sampleList = new ArrayList<>();
   private float[]             _cadenceData;
   private float[]             _distanceData;
   private float[]             _pulseData;

   private TimeData            _markerData;
   private int                 _markIndex;
   private ArrayList<TimeData> _markerList = new ArrayList<>();

   private boolean             _isImported;
   private StringBuilder       _characters = new StringBuilder();

   private boolean             _isInDevice;
   private boolean             _isInHeader;
   private boolean             _isInMarks;
   private boolean             _isInMark;
   private boolean             _isInMove;
   private boolean             _isInMoves;
   private boolean             _isInSamples;
   private boolean             _isInRootMovesCount;
   private boolean             _isInCadence;
   private boolean             _isInCalories;
   private boolean             _isInDistance;
   private boolean             _isInDistanceUnit;
   private boolean             _isInHR;
   private boolean             _isInMarkTime;
   private boolean             _isInSampleRate;
   private boolean             _isInTime;
   private boolean             _isInVersion;
   private boolean             _isInWeight;
   private boolean             _isInWeightUnit;

   private int                 _tourCalories;
   private String              _deviceVersion;
   private String              _distanceUnit;
   private short               _tourSampleRate;
   private LocalDateTime       _tourStartTime;
   private int                 _tourTimezone;
   private float               _weight;
   private String              _weightUnit;

   public SuuntoQuestSAXHandler(final TourbookDevice deviceDataReader,
                                final String importFileName,
                                final HashMap<Long, TourData> alreadyImportedTours,
                                final HashMap<Long, TourData> newlyImportedTours) {

      _device = deviceDataReader;
      _importFilePath = importFileName;
      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;
   }

   @Override
   public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

      if (_isInCadence //
            || _isInCalories
            || _isInDistance
            || _isInDistanceUnit
            || _isInHR
            || _isInMarkTime
            || _isInSampleRate
            || _isInVersion
            || _isInWeight
            || _isInWeightUnit
            || _isInTime
      //
      ) {
         _characters.append(chars, startIndex, length);
      }
   }

   private float[] convertStringToFloatArray(final String values) {

      if (values.equals(UI.EMPTY_STRING)) {
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
   }

   @Override
   public void endElement(final String uri, final String localName, final String name) throws SAXException {

      if (_isInDevice) {

         endElement_InDevice(name);

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

      } else if (name.equals(TAG_MOVES)) {

         _isInMoves = false;

      } else if (name.equals(TAG_DEVICE)) {

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

   private void endElement_InHeader(final String name) {

      if (name.equals(TAG_CALORIES)) {

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

      if (name.equals(TAG_MARK_TIME)) {

         _isInMarkTime = false;

         final PeriodFormatter periodFormatter =
               new PeriodFormatterBuilder().printZeroAlways()
                     .appendHours()
                     .appendSeparator(UI.SYMBOL_COLON)
                     .appendMinutes()
                     .appendSeparator(UI.SYMBOL_COLON)
                     .appendSeconds()
                     .appendSeparator(UI.SYMBOL_DOT)
                     .appendMillis()
                     .maximumParsedDigits(2)
                     .toFormatter();

         final Period markerPeriod = periodFormatter.parsePeriod(_characters.toString());
         _markerData.relativeTime = markerPeriod.toStandardSeconds().getSeconds();

         //If existing, we need to use the previous marker relative time
         //to determine the current marker's relative time
         if (_markIndex > 0) {
            _markerData.relativeTime += _markerList.get(_markIndex - 1).relativeTime;
         }

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
   private void finalizeSamples(final ZonedDateTime tourStartTime) {

      //Inserting
      int relativeTime = 0;
      int totalDistance = 0;
      for (int index = 0; index < _distanceData.length; ++index) {

         final TimeData timeData = new TimeData();

         timeData.cadence = _cadenceData[index];
         timeData.pulse = _pulseData[index];

         float distance = _distanceData[index];
         if (_distanceUnit.equalsIgnoreCase("mile")) { //$NON-NLS-1$
            distance /= net.tourbook.ui.UI.UNIT_MILE;
         }
         timeData.distance = distance;
         totalDistance += timeData.distance;
         timeData.absoluteDistance += totalDistance;

         relativeTime += _tourSampleRate;
         timeData.relativeTime = relativeTime;
         timeData.absoluteTime = (tourStartTime.toEpochSecond() + relativeTime) * 1000;

         _sampleList.add(timeData);
      }

      for (final TimeData marker : _markerList) {
         marker.absoluteTime = (tourStartTime.toEpochSecond() + marker.relativeTime) * 1000;
      }

      mergeMarkers();
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

      finalizeSamples(tourStartTime);

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
      if (_alreadyImportedTours.containsKey(tourId) == false) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, tourData);

         // create additional data
         tourData.computeTourDrivingTime();
         tourData.computeComputedValues();
      }

      _isImported = true;

      //We clear the data for the potential next tours
      dispose();
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

      final int markerSize = _markerList.size();

      if (markerSize == 0) {
         return;
      }

      int markerIndex = 0;

      long markerTime = _markerList.get(markerIndex).absoluteTime;
      final ArrayList<TimeData> _markersToAdd = new ArrayList<>();

      for (final TimeData sampleData : _sampleList) {

         final long sampleTime = sampleData.absoluteTime;

         if (sampleTime < markerTime) {

            continue;

         } else if (sampleTime == markerTime) {
            //If we find a sample at the same time, we reuse it

            sampleData.marker = 1;
            sampleData.markerLabel = Integer.toString(markerIndex++);

            /*
             * check if another marker is available
             */
            if (markerIndex >= markerSize) {
               break;
            }

            markerTime = _markerList.get(markerIndex).absoluteTime;
         } else if (sampleTime > markerTime) {

            final TimeData newSample = new TimeData();
            newSample.marker = 1;
            newSample.markerLabel = Integer.toString(markerIndex + 1);

            final TimeData currentMarker = _markerList.get(markerIndex);
            newSample.cadence = currentMarker.cadence;

            float distance = currentMarker.distance;
            if (_distanceUnit.equalsIgnoreCase("mile")) { //$NON-NLS-1$
               distance *= net.tourbook.ui.UI.UNIT_MILE;
            }
            newSample.distance = distance;

            newSample.pulse = currentMarker.pulse;
            newSample.absoluteTime = currentMarker.absoluteTime;

            _markersToAdd.add(newSample);

            /*
             * check if another marker is available
             */
            ++markerIndex;
            if (markerIndex >= markerSize) {
               break;
            }

            markerTime = newSample.absoluteTime;
         }
      }

      //Adding the potentially newly created samples that are markers
      _sampleList.addAll(_markersToAdd);

      //We sort the sample lists as it could be out of order if we added markers above
      Collections.sort(_sampleList, (left, right) -> (int) left.absoluteTime - (int) right.absoluteTime);
   }

   @Override
   public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
         throws SAXException {

      if (_isInRootMovesCount) {

         if (_isInMoves) {

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
         } else if (_isInDevice) {

            startElement_InDevice(name);

         } else if (name.equals(TAG_DEVICE)) {

            _isInDevice = true;

         } else if (name.equals(TAG_MOVES)) {

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

   private void startElement_InHeader(final String name) {

      boolean isData = true;

      if (name.equals(TAG_CALORIES)) {

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

      if (name.equals(TAG_MARK_TIME)) {

         _isInMarkTime = true;

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
