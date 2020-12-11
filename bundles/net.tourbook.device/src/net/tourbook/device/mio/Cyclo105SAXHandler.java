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
package net.tourbook.device.mio;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.ui.UI;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Cyclo105SAXHandler extends DefaultHandler {

   // Review the file one more time to see if I can import more data.

   // root tags
   static final String         TAG_ROOT_MAGELLAN    = "Magellan_Cyclo"; //$NON-NLS-1$
   static final String         TAG_ROOT_TRACKPOINTS = "TrackPoints";    //$NON-NLS-1$
   private static final String TAG_ROOT_TRACKMASTER = "trackmaster";    //$NON-NLS-1$

   // trackmaster tags
   private static final String TAG_ACCRUEDTIME = "AccruedTime"; //$NON-NLS-1$
   private static final String TAG_CALORIES    = "Calories";    //$NON-NLS-1$
   private static final String TAG_STARTTIME   = "StartTime";   //$NON-NLS-1$
   private static final String TAG_TRACKNAME   = "TrackName";   //$NON-NLS-1$

   // TrackPoints tags
   private static final String   TAG_ALTITUDE     = "Altitude";                 //$NON-NLS-1$
   private static final String   TAG_CADENCE      = "Cadence";                  //$NON-NLS-1$
   private static final String   TAG_HEARTRATE    = "HeartRate";                //$NON-NLS-1$
   private static final String   TAG_INTERVALTIME = "IntervalTime";             //$NON-NLS-1$
   private static final String   TAG_LATITUDE     = "Latitude";                 //$NON-NLS-1$
   private static final String   TAG_LONGITUDE    = "Longitude";                //$NON-NLS-1$
   private static final String   TAG_POWER        = "Power";                    //$NON-NLS-1$
   private static final String   TAG_SPEED        = "Speed";                    //$NON-NLS-1$

   private Map<Long, TourData>   _alreadyImportedTours;
   private Map<Long, TourData>   _newlyImportedTours;
   private TourbookDevice        _device;
   private String                _importFilePath;

   private ArrayList<TimeData>   _sampleList      = new ArrayList<>();
   private TimeData              _sampleData;

   private ArrayList<Integer>    _markerList      = new ArrayList<>();

   private boolean               _isImported;
   private StringBuilder         _characters      = new StringBuilder();

   private boolean               _isInRootMagellan;
   private boolean               _isInTrackPoints;
   private boolean               _isInTrackMaster;

   private boolean               _isInAccruedTime;
   private boolean               _isInAltitude;
   private boolean               _isInCalories;
   private boolean               _isInLatitude;
   private boolean               _isInLongitude;
   private boolean               _isInCadence;
   private boolean               _isInHR;
   private boolean               _isInIntervalTime;
   private boolean               _isInPower;
   private boolean               _isInSpeed;
   private boolean               _isInTrackName;
   private boolean               _isInStartTime;

   private int                   _tourCalories;
   private Period                _tourStartTime;
   private LocalDate             _tourStartDate;

   private final PeriodFormatter periodFormatter  = new PeriodFormatterBuilder()
         .appendHours().appendSuffix(UI.SYMBOL_COLON)
         .appendMinutes().appendSuffix(UI.SYMBOL_COLON)
         .appendSeconds()
         .toFormatter();

   public Cyclo105SAXHandler(final TourbookDevice deviceDataReader,
                             final String importFileName,
                             final Map<Long, TourData> alreadyImportedTours,
                             final Map<Long, TourData> newlyImportedTours) {

      _device = deviceDataReader;
      _importFilePath = importFileName;
      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;
   }

   @Override
   public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

      if (_isInTrackName
            || _isInStartTime
            || _isInCalories
            || _isInAccruedTime
            || _isInAltitude
            || _isInCadence
            || _isInLatitude
            || _isInLongitude
            || _isInHR
            || _isInPower
            || _isInSpeed
            || _isInIntervalTime) {
         _characters.append(chars, startIndex, length);
      }
   }

   public void dispose() {

      _sampleList.clear();
      _markerList.clear();
   }

   @Override
   public void endElement(final String uri, final String localName, final String name) throws SAXException {

      if (_isInTrackMaster) {

         endElement_InTrackMaster(name);

      } else if (_isInTrackPoints) {

         endElement_InTrackPoints(name);

      }

      if (name.equals(TAG_ROOT_TRACKPOINTS)) {

         _isInTrackPoints = false;

         _sampleList.add(_sampleData);

      } else if (name.equals(TAG_ROOT_TRACKMASTER)) {

         _isInTrackMaster = false;

      } else if (name.equals(TAG_ROOT_MAGELLAN)) {

         _isInRootMagellan = false;

         finalizeTour();

      }
   }

   private void endElement_InTrackMaster(final String name) {

      if (name.equals(TAG_CALORIES)) {

         _isInCalories = false;

         _tourCalories = Integer.parseInt(_characters.toString());

      } else if (name.equals(TAG_TRACKNAME)) {

         //This data element is called trackname but it contains the activity date, this is not intuitive.
         // ex: <TrackName>2016-7-30</TrackName>
         _isInTrackName = false;

         _tourStartDate = LocalDate.parse(_characters.toString());

      } else if (name.equals(TAG_STARTTIME)) {

         _isInStartTime = false;

         _tourStartTime = periodFormatter.parsePeriod(_characters.toString());

      } else if (name.equals(TAG_ACCRUEDTIME)) {

         _isInAccruedTime = false;

         final Period accruedTime = periodFormatter.parsePeriod(_characters.toString());
         _markerList.add(accruedTime.toStandardSeconds().getSeconds());

      }
   }

   private void endElement_InTrackPoints(final String name) {

      if (name.equals(TAG_LATITUDE)) {

         _isInLatitude = false;

         _sampleData.latitude = Double.parseDouble(_characters.toString().replace(',', '.'));

      } else if (name.equals(TAG_LONGITUDE)) {

         _isInLongitude = false;

         _sampleData.longitude = Double.parseDouble(_characters.toString().replace(',', '.'));

      } else if (name.equals(TAG_ALTITUDE)) {

         _isInAltitude = false;

         _sampleData.absoluteAltitude = Float.parseFloat(_characters.toString());

      } else if (name.equals(TAG_SPEED)) {

         _isInSpeed = false;

         _sampleData.speed = Float.parseFloat(_characters.toString().replace(',', '.'));

      } else if (name.equals(TAG_HEARTRATE)) {

         _isInHR = false;

         _sampleData.pulse = Float.parseFloat(_characters.toString());

      } else if (name.equals(TAG_INTERVALTIME)) {

         _isInIntervalTime = false;

         _sampleData.time = Math.round(Float.parseFloat(_characters.toString().replace(',', '.')));

      } else if (name.equals(TAG_CADENCE)) {

         _isInCadence = false;

         _sampleData.cadence = Float.parseFloat(_characters.toString());

      } else if (name.equals(TAG_POWER)) {

         _isInPower = false;

         _sampleData.power = Float.parseFloat(_characters.toString());

      }
   }

   /**
    * Fills the necessary data for the samples :
    * computes {@see TimeData#absoluteTime} for each {@link TimeData}, merges markers list with the
    * samples
    *
    * @param tourStartTime
    *           The tour start time, in seconds, that is used to compute the
    *           {@see TimeData#absoluteTime}
    */
   private void finalizeSamples(final long tourStartTime) {

      int time = 0;
      int numberOfMarkers = 0;
      for (final TimeData element : _sampleList) {

         final TimeData currentTimeData = element;

         time += currentTimeData.time;
         currentTimeData.absoluteTime = (tourStartTime + time) * 1000;

         if (_markerList.contains(time)) {
            currentTimeData.marker = 1;

            ++numberOfMarkers;
            currentTimeData.markerLabel = String.valueOf(numberOfMarkers);
         }
      }
   }

   private void finalizeTour() {

      // check if data are available
      if (_sampleList.isEmpty()) {
         return;
      }

      // create data object for each tour
      final TourData tourData = new TourData();

      /*
       * set tour start date/time
       */
      final ZonedDateTime tourStartTime = ZonedDateTime.of(
            _tourStartDate.getYear(),
            _tourStartDate.getMonthOfYear(),
            _tourStartDate.getDayOfMonth(),
            _tourStartTime.getHours(),
            _tourStartTime.getMinutes(),
            _tourStartTime.getSeconds(),
            0,
            TimeTools.getDefaultTimeZone());
      tourData.setTourStartTime(tourStartTime);

      finalizeSamples(tourStartTime.toEpochSecond());

      tourData.setImportFilePath(_importFilePath);
      tourData.setCalories(_tourCalories * 1000);

      tourData.setDeviceId(_device.deviceId);
      tourData.setDeviceName(_device.visibleName);

      tourData.createTimeSeries(_sampleList, true);

      // after all data are added, the tour id can be created
      final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_MIO_105);
      final Long tourId = tourData.createTourId(uniqueId);

      // check if the tour is already imported
      if (!_alreadyImportedTours.containsKey(tourId)) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, tourData);

         // create additional data
         tourData.computeAltitudeUpDown();
         tourData.setTourDeviceTime_Recorded(tourData.getTourDeviceTime_Elapsed());
         tourData.computeTourMovingTime();
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

   @Override
   public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
         throws SAXException {

      if (_isInRootMagellan) {

         if (_isInTrackMaster) {

            startElement_InTrackMaster(name);

         } else if (_isInTrackPoints) {

            startElement_InTrackPoints(name);

         } else if (name.equals(TAG_ROOT_TRACKMASTER)) {

            _isInTrackMaster = true;

         } else if (name.equals(TAG_ROOT_TRACKPOINTS)) {

            _isInTrackPoints = true;

            _sampleData = new TimeData();

         }
      } else if (name.equals(TAG_ROOT_MAGELLAN)) {

         _isInRootMagellan = true;

      }
   }

   private void startElement_InTrackMaster(final String name) {

      boolean isData = true;

      if (name.equals(TAG_CALORIES)) {

         _isInCalories = true;

      } else if (name.equals(TAG_TRACKNAME)) {

         _isInTrackName = true;

      } else if (name.equals(TAG_STARTTIME)) {

         _isInStartTime = true;

      } else if (name.equals(TAG_ACCRUEDTIME)) {

         _isInAccruedTime = true;

      } else {
         isData = false;
      }

      if (isData) {

         // clear char buffer
         _characters.delete(0, _characters.length());
      }
   }

   private void startElement_InTrackPoints(final String name) {

      boolean isData = true;

      if (name.equals(TAG_LATITUDE)) {

         _isInLatitude = true;

      } else if (name.equals(TAG_LONGITUDE)) {

         _isInLongitude = true;

      } else if (name.equals(TAG_ALTITUDE)) {

         _isInAltitude = true;

      } else if (name.equals(TAG_SPEED)) {

         _isInSpeed = true;

      } else if (name.equals(TAG_HEARTRATE)) {

         _isInHR = true;

      } else if (name.equals(TAG_INTERVALTIME)) {

         _isInIntervalTime = true;

      } else if (name.equals(TAG_CADENCE)) {

         _isInCadence = true;

      } else if (name.equals(TAG_POWER)) {

         _isInPower = true;

      } else {
         isData = false;
      }

      if (isData) {

         // clear char buffer
         _characters.delete(0, _characters.length());
      }
   }
}
