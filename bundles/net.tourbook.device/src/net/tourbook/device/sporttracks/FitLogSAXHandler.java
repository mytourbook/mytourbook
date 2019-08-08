/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.device.sporttracks;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Set;

import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.InvalidDeviceSAXException;
import net.tourbook.device.Messages;
import net.tourbook.preferences.TourTypeColorDefinition;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.ChartLabel;

import org.eclipse.osgi.util.NLS;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FitLogSAXHandler extends DefaultHandler {

   private static final String                  TAG_ACTIVITY                                = "Activity";                   //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CADENCE                        = "Cadence";                    //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CALORIES                       = "Calories";                   //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CATEGORY                       = "Category";                   //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CUSTOM_DATA_FIELDS             = "CustomDataFields";           //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CUSTOM_DATA_FIELD              = "CustomDataField";            //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITIONS  = "CustomDataFieldDefinitions"; //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITION   = "CustomDataFieldDefinition";  //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_DURATION                       = "Duration";                   //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_DISTANCE                       = "Distance";                   //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_ELEVATION                      = "Elevation";                  //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_EQUIPMENT_ITEM                 = "EquipmentItem";              //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_HEART_RATE                     = "HeartRate";                  //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_LOCATION                       = "Location";                   //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_NAME                           = "Name";                       //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_NOTES                          = "Notes";                      //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_POWER                          = "Power";                      //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_WEATHER                        = "Weather";                    //$NON-NLS-1$

   private static final String                  ATTRIB_DURATION_SECONDS                     = "DurationSeconds";            //$NON-NLS-1$
   private static final String                  ATTRIB_NAME                                 = "Name";                       //$NON-NLS-1$
   private static final String                  ATTRIB_START_TIME                           = "StartTime";                  //$NON-NLS-1$
   private static final String                  ATTRIB_END_TIME                             = "EndTime";                    //$NON-NLS-1$
   private static final String                  ATTRIB_TOTAL_SECONDS                        = "TotalSeconds";               //$NON-NLS-1$
   private static final String                  ATTRIB_TOTAL_METERS                         = "TotalMeters";                //$NON-NLS-1$
   private static final String                  ATTRIB_TOTAL_CAL                            = "TotalCal";                   //$NON-NLS-1$
   private static final String                  ATTRIB_ASCEND_METERS                        = "AscendMeters";               //$NON-NLS-1$
   private static final String                  ATTRIB_CUSTOM_DATA_FIELD_DEFINITION_NAME    = "Name";                       //$NON-NLS-1$
   private static final String                  ATTRIB_CUSTOM_DATA_FIELD_DEFINITION_OPTIONS = "Options";                    //$NON-NLS-1$
   private static final String                  ATTRIB_CUSTOM_DATA_FIELD_NAME               = "name";                       //$NON-NLS-1$
   private static final String                  ATTRIB_CUSTOM_DATA_FIELD_VALUE              = "v";                          //$NON-NLS-1$
   private static final String                  ATTRIB_DESCEND_METERS                       = "DescendMeters";              //$NON-NLS-1$
   private static final String                  ATTRIB_AVERAGE_BPM                          = "AverageBPM";                 //$NON-NLS-1$
   private static final String                  ATTRIB_MAXIMUM_BPM                          = "MaximumBPM";                 //$NON-NLS-1$
   private static final String                  ATTRIB_AVERAGE_WATTS                        = "AverageWatts";               //$NON-NLS-1$
   private static final String                  ATTRIB_MAXIMUM_WATTS                        = "MaximumWatts";               //$NON-NLS-1$
   private static final String                  ATTRIB_AVERAGE_RPM                          = "AverageRPM";                 //$NON-NLS-1$
   private static final String                  ATTRIB_WEATHER_TEMP                         = "Temp";                       //$NON-NLS-1$
   private static final String                  ATTRIB_WEATHER_CONDITIONS                   = "Conditions";                 //$NON-NLS-1$
   //
   private static final String                  TAG_TRACK                                   = "Track";                      //$NON-NLS-1$
   private static final String                  TAG_TRACK_PT                                = "pt";                         //$NON-NLS-1$
   private static final String                  ATTRIB_PT_CADENCE                           = "cadence";                    //$NON-NLS-1$
   private static final String                  ATTRIB_PT_DIST                              = "dist";                       //$NON-NLS-1$
   private static final String                  ATTRIB_PT_ELE                               = "ele";                        //$NON-NLS-1$
   private static final String                  ATTRIB_PT_HR                                = "hr";                         //$NON-NLS-1$
   private static final String                  ATTRIB_PT_LAT                               = "lat";                        //$NON-NLS-1$
   private static final String                  ATTRIB_PT_LON                               = "lon";                        //$NON-NLS-1$
   private static final String                  ATTRIB_PT_POWER                             = "power";                      //$NON-NLS-1$
   private static final String                  ATTRIB_PT_TEMP                              = "temp";                       //$NON-NLS-1$
   private static final String                  ATTRIB_PT_TM                                = "tm";                         //$NON-NLS-1$
   //
   private static final String                  TAG_LAPS                                    = "Laps";                       //$NON-NLS-1$
   private static final String                  TAG_LAP                                     = "Lap";                        //$NON-NLS-1$
   private static final String                  TAG_TRACK_CLOCK                             = "TrackClock";                 //$NON-NLS-1$
   private static final String                  TAG_PAUSE                                   = "Pause";                      //$NON-NLS-1$

   private static final String                  SUB_ATTRIB_WIND_SPEED                       = "Wind Speed:";                //$NON-NLS-1$
   private static final HashMap<String, String> _weatherId                                  = new HashMap<>();
   //
   private String                               _importFilePath;
   private FitLogDeviceDataReader               _device;
   private HashMap<Long, TourData>              _alreadyImportedTours;
   private HashMap<Long, TourData>              _newlyImportedTours;

   private Activity                             _currentActivity;
   private double                               _prevLatitude;
   private double                               _prevLongitude;

   private double                               _distanceAbsolute;
   private boolean                              _isImported                                 = false;
   private boolean                              _isNewTag                                   = false;

   private boolean                              _isNewTourType                              = false;
   private boolean                              _isInActivity;
   private boolean                              _isInTrack;

   private boolean                              _hasCustomDataFields;
   private boolean                              _isInCustomDataFields;
   private boolean                              _isInCustomDataFieldDefinitions;
   private boolean                              _isInName;
   private boolean                              _isInNotes;
   private boolean                              _isInWeather;

   private StringBuilder                        _characters                                 = new StringBuilder(100);
   private boolean                              _isInLaps;
   private boolean                              _isInPauses;

   private ArrayList<TourType>                  _allTourTypes;
   {
      /*
       * Entries which are marked with *) have not a corresponding id/image within MyTourbook
       */
// SET_FORMATTING_OFF
      _weatherId.put("Clear",          IWeather.WEATHER_ID_CLEAR); //                     //$NON-NLS-1$
      _weatherId.put("ScatterClouds",  IWeather.WEATHER_ID_PART_CLOUDS); //         *)    //$NON-NLS-1$
      _weatherId.put("PartClouds",     IWeather.WEATHER_ID_PART_CLOUDS); //               //$NON-NLS-1$
      _weatherId.put("Overcast",       IWeather.WEATHER_ID_OVERCAST); //                  //$NON-NLS-1$
      _weatherId.put("MostClouds",     IWeather.WEATHER_ID_OVERCAST); //            *)    //$NON-NLS-1$
      _weatherId.put("Clouds",         IWeather.WEATHER_ID_PART_CLOUDS); //         *)    //$NON-NLS-1$
      _weatherId.put("ChanceRain",     IWeather.WEATHER_ID_SCATTERED_SHOWERS); //   *)    //$NON-NLS-1$
      _weatherId.put("LightDrizzle",   IWeather.WEATHER_ID_SCATTERED_SHOWERS); //   *)    //$NON-NLS-1$
      _weatherId.put("LightRain",      IWeather.WEATHER_ID_SCATTERED_SHOWERS); //         //$NON-NLS-1$
      _weatherId.put("Rain",           IWeather.WEATHER_ID_RAIN); //                      //$NON-NLS-1$
      _weatherId.put("HeavyRain",      IWeather.WEATHER_ID_RAIN); //                *)    //$NON-NLS-1$
      _weatherId.put("ChanceThunder",  IWeather.WEATHER_ID_LIGHTNING); //           *)    //$NON-NLS-1$
      _weatherId.put("Thunder",        IWeather.WEATHER_ID_LIGHTNING); //                 //$NON-NLS-1$
      _weatherId.put("Snow",           IWeather.WEATHER_ID_SNOW); //                      //$NON-NLS-1$
      _weatherId.put("Haze",           IWeather.WEATHER_ID_PART_CLOUDS); //         *)    //$NON-NLS-1$
// SET_FORMATTING_ON
   }

   private class Activity {

      private ArrayList<TimeData> timeSlices         = new ArrayList<>();
      private ArrayList<Lap>      laps               = new ArrayList<>();
      private ArrayList<Pause>    pauses             = new ArrayList<>();
      private ArrayList<String>   equipmentName      = new ArrayList<>();

      private ZonedDateTime       tourStartTime;
      private long                tourStartTimeMills = Long.MIN_VALUE;
//      private DateTime         trackTourDateTime;
//      private long            trackTourStartTime   = Long.MIN_VALUE;

      private String location;
      private String name;
      private String notes;
      private String categoryName;

      private int    calories;
      private int    duration;
      private int    distance;

      private int    elevationUp;
      private int    elevationDown;

      private int    avgPulse;
      private int    maxPulse;

      private float  avgPower;
      private float  maxPower;

      private int    avgCadence;
//      private int               maxCadence;      is not yet supported

      private String                        weatherText;
      private String                        weatherConditions;
      private float                         weatherTemperature = Float.MIN_VALUE;
      private int                           weatherWindSpeed   = Integer.MIN_VALUE;

      private LinkedHashMap<String, String> customDataFields   = new LinkedHashMap<>();
   }

   private class Lap {

      private long startTime;
      private long endTime;
   }

   private class Pause {

      private long startTime;
      private long endTime;
      private long duration;
   }

   public FitLogSAXHandler(final FitLogDeviceDataReader device,
                           final String importFilePath,
                           final HashMap<Long, TourData> alreadyImportedTours,
                           final HashMap<Long, TourData> newlyImportedTours) {

      _device = device;
      _importFilePath = importFilePath;
      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;

      _allTourTypes = TourDatabase.getAllTourTypes();
   }

   @Override
   public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

      if (_isInName || _isInNotes || _isInWeather) {

         _characters.append(chars, startIndex, length);
      }
   }

   @Override
   public void endElement(final String uri, final String localName, final String name) throws SAXException {

      /*
       * get values
       */
      if (_isInName || _isInNotes || _isInWeather) {
         parseActivity_02_End(name);
      }

      /*
       * set state
       */
      if (name.equals(TAG_TRACK)) {

         _isInTrack = false;

      } else if (name.equals(TAG_LAPS)) {

         _isInLaps = false;

      } else if (name.equals(TAG_TRACK_CLOCK)) {

         _isInPauses = false;

      } else if (name.equals(TAG_ACTIVITY_CUSTOM_DATA_FIELDS)) {

         _isInCustomDataFields = false;

      } else if (name.equals(TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITIONS)) {

         _isInCustomDataFieldDefinitions = false;

         finalizeTour();

      } else if (name.equals(TAG_ACTIVITY) &&
            _isInCustomDataFieldDefinitions == false) {

         // activity/tour ends

         _isInActivity = false;

         // If the activity has custom fields, we need to wait to have
         // imported all of them before finalizing the tour
         if (!_hasCustomDataFields) {
            finalizeTour();
         }

      }
   }

   private void finalizeTour() {

      boolean isComputeDrivingTime = true;

      // create data object for each tour
      final TourData tourData = new TourData();

      /*
       * set tour start date/time
       */
//      DateTime tourDateTime = _currentActivity.trackTourDateTime;
//      final long trackStartTime = _currentActivity.trackTourStartTime;
//      if (trackStartTime != Long.MIN_VALUE && trackStartTime < 0) {
//
//         // this case occurred, e.g. year was 0002
//         tourDateTime = _currentActivity.tourStartTime;
//
//      } else if (tourDateTime == null) {
//
//         // this case can occur when a tour do not have a track
//         tourDateTime = _currentActivity.tourStartTime;
//      }

      tourData.setTourStartTime(_currentActivity.tourStartTime);

      tourData.setTourTitle(_currentActivity.name);
      tourData.setTourDescription(_currentActivity.notes);
      tourData.setTourStartPlace(_currentActivity.location);

      tourData.setCalories(_currentActivity.calories);

      /*
       * weather
       */
      tourData.setWeather(_currentActivity.weatherText);
      tourData.setWeatherClouds(_weatherId.get(_currentActivity.weatherConditions));

      final float weatherTemperature = _currentActivity.weatherTemperature;
      if (weatherTemperature != Float.MIN_VALUE) {
         tourData.setAvgTemperature(weatherTemperature);
      }

      if (_currentActivity.weatherWindSpeed != Integer.MIN_VALUE) {
         tourData.setWeatherWindSpeed(_currentActivity.weatherWindSpeed);
      }

      if (_currentActivity.customDataFields.size() > 0) {
         final StringBuilder tourNotes = new StringBuilder(tourData.getTourDescription());

         if (!tourNotes.toString().trim().isEmpty()) {
            tourNotes.append(System.getProperty("line.separator")); //$NON-NLS-1$
            tourNotes.append(System.getProperty("line.separator")); //$NON-NLS-1$
         }

         tourNotes.append(Messages.FitLog_CustomDataFields_Label);
         _currentActivity.customDataFields.forEach((key, value) -> {
            if (!tourNotes.toString().trim().isEmpty()) {
               //If there is already content in the notes fields, then we insert a new line
               tourNotes.append(System.getProperty("line.separator")); //$NON-NLS-1$
            }
            tourNotes.append("\"" + key + "\" : \"" + value + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         });

         tourData.setTourDescription(tourNotes.toString());
      }

      tourData.setImportFilePath(_importFilePath);

      tourData.setDeviceTimeInterval((short) -1);

      if (_currentActivity.timeSlices.size() == 0) {

         // tour do not contain a track

         tourData.setTourDistance(_currentActivity.distance);

         tourData.setTourRecordingTime(_currentActivity.duration);
         tourData.setTourDrivingTime(_currentActivity.duration);
         isComputeDrivingTime = false;

         tourData.setTourAltUp(_currentActivity.elevationUp);
         tourData.setTourAltDown(_currentActivity.elevationDown);

      } else {

         // create 'normal' tour

         tourData.createTimeSeries(_currentActivity.timeSlices, false);
      }

      if (_currentActivity.avgPower != 0) {
         tourData.setPower_Avg(_currentActivity.avgPower);
      }
      if (_currentActivity.maxPower != 0) {
         tourData.setPower_Max((int) (_currentActivity.maxPower + 0.5));
      }

      if (tourData.pulseSerie == null) {
         tourData.setAvgPulse(_currentActivity.avgPulse);
         tourData.setMaxPulse(_currentActivity.maxPulse);
      }

      if (tourData.getCadenceSerie() == null) {
         tourData.setAvgCadence(_currentActivity.avgCadence);
      }

      tourData.setDeviceId(_device.deviceId);
      tourData.setDeviceName(_device.visibleName);

      // after all data are added, the tour id can be created because it depends on the tour distance
      final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_SPORT_TRACKS_FITLOG);
      final Long tourId = tourData.createTourId(uniqueId);

      // check if the tour is already imported
      if (_alreadyImportedTours.containsKey(tourId) == false) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, tourData);

         // create additional data
         if (isComputeDrivingTime) {
            tourData.computeTourDrivingTime();
         }

         tourData.computeAltitudeUpDown();
         tourData.computeComputedValues();

         finalizeTour_10_SetTourType(tourData);
         finalizeTour_20_SetTags(tourData);
         finalizeTour_30_CreateMarkers(tourData);
      }

      // cleanup
      _currentActivity.timeSlices.clear();
      _currentActivity.laps.clear();
      _currentActivity.equipmentName.clear();

      _isImported = true;
   }

   /**
    * Set tour type from category field
    *
    * @param tourData
    */
   private void finalizeTour_10_SetTourType(final TourData tourData) {

      final String categoryName = _currentActivity.categoryName;

      if (categoryName == null) {
         return;
      }

      TourType tourType = null;

      // find tour type in existing tour types
      for (final TourType mapTourType : _allTourTypes) {
         if (categoryName.equalsIgnoreCase(mapTourType.getName())) {
            tourType = mapTourType;
            break;
         }
      }

      TourType newSavedTourType = null;

      if (tourType == null) {

         // create new tour type

         final TourType newTourType = new TourType(categoryName);

         final TourTypeColorDefinition newColorDefinition = new TourTypeColorDefinition(
               newTourType,
               Long.toString(newTourType.getTypeId()),
               newTourType.getName());

         newTourType.setColorBright(newColorDefinition.getGradientBright_Default());
         newTourType.setColorDark(newColorDefinition.getGradientDark_Default());
         newTourType.setColorLine(newColorDefinition.getLineColor_Default());
         newTourType.setColorText(newColorDefinition.getTextColor_Default());

         // save new entity
         newSavedTourType = TourDatabase.saveEntity(newTourType, newTourType.getTypeId(), TourType.class);
         if (newSavedTourType != null) {
            tourType = newSavedTourType;
            _allTourTypes.add(tourType);
         }
      }

      tourData.setTourType(tourType);

      _isNewTourType |= newSavedTourType != null;
   }

   private void finalizeTour_20_SetTags(final TourData tourData) {

      final ArrayList<String> equipmentNames = _currentActivity.equipmentName;
      if (equipmentNames.size() == 0) {
         return;
      }

      boolean isNewTag = false;

      final Set<TourTag> tourTags = new HashSet<>();

      HashMap<Long, TourTag> tourTagMap = TourDatabase.getAllTourTags();
      TourTag[] allTourTags = tourTagMap.values().toArray(new TourTag[tourTagMap.size()]);

      try {

         for (final String tagLabel : equipmentNames) {

            boolean isTagAvailable = false;

            for (final TourTag tourTag : allTourTags) {
               if (tourTag.getTagName().equals(tagLabel)) {

                  // existing tag is found

                  isTagAvailable = true;

                  tourTags.add(tourTag);
                  break;
               }
            }

            if (isTagAvailable == false) {

               // create a new tag

               final TourTag tourTag = new TourTag(tagLabel);
               tourTag.setRoot(true);

               // persist tag
               final TourTag savedTag = TourDatabase.saveEntity(
                     tourTag,
                     TourDatabase.ENTITY_IS_NOT_SAVED,
                     TourTag.class);

               if (savedTag != null) {

                  tourTags.add(savedTag);

                  // reload tour tag list

                  TourDatabase.clearTourTags();

                  tourTagMap = TourDatabase.getAllTourTags();
                  allTourTags = tourTagMap.values().toArray(new TourTag[tourTagMap.size()]);

                  isNewTag = true;
               }
            }
         }

      } catch (final NoSuchElementException e) {
         // no further tokens
      } finally {

         tourData.setTourTags(tourTags);
      }

      _isNewTag |= isNewTag;
   }

   private void finalizeTour_30_CreateMarkers(final TourData tourData) {

      final ArrayList<Lap> _laps = _currentActivity.laps;
      if (_laps.size() == 0) {
         return;
      }

      final int[] timeSerie = tourData.timeSerie;
      if (timeSerie == null || timeSerie.length == 0) {
         // fixed bug: http://sourceforge.net/support/tracker.php?aid=3232030
         return;
      }

      final Set<TourMarker> tourMarkers = tourData.getTourMarkers();
      final float[] altitudeSerie = tourData.altitudeSerie;
      final float[] distanceSerie = tourData.distanceSerie;
      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      /*
       * tour and track can have different start times
       */
      final long tourStartTime = _currentActivity.tourStartTimeMills;
//      final long tour2sliceTimeDiff = _currentActivity.trackTourStartTime - tourStartTime;

      int lapCounter = 1;

      for (final Lap lap : _laps) {

         long startTimeDiff = lap.endTime - tourStartTime;// - tour2sliceTimeDiff;

         // If present, we add the total pause time
         for (final Pause pause : _currentActivity.pauses) {

            //We need to make sure to only add the pauses that are
            //within the current lap time interval.
            if (pause.startTime < lap.endTime &&
                  pause.endTime > lap.startTime) {
               startTimeDiff += pause.duration;
            }
         }
         int lapRelativeTime = (int) (startTimeDiff / 1000);
         int serieIndex = 0;

         // get serie index
         for (final int tourRelativeTime : timeSerie) {
            if (tourRelativeTime >= lapRelativeTime) {
               break;
            }
            serieIndex++;
         }

         if (lapRelativeTime < 0) {
            // this case occurred
            lapRelativeTime = 0;
         }

         // check array bounds
         if (serieIndex >= timeSerie.length) {
            serieIndex = timeSerie.length - 1;
         }

         final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_DEVICE);

         tourMarker.setLabel(Integer.toString(lapCounter));
         tourMarker.setSerieIndex(serieIndex);
         tourMarker.setTime(lapRelativeTime, tourData.getTourStartTimeMS() + (lapRelativeTime * 1000));

         if (distanceSerie != null) {
            tourMarker.setDistance(distanceSerie[serieIndex]);
         }

         if (altitudeSerie != null) {
            tourMarker.setAltitude(altitudeSerie[serieIndex]);
         }

         if (latitudeSerie != null) {
            tourMarker.setGeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]);
         }

         tourMarkers.add(tourMarker);

         lapCounter++;
      }
   }

   private void initTour(final Attributes attributes) {

      _currentActivity = new Activity();

      _distanceAbsolute = 0;

      _prevLatitude = Double.MIN_VALUE;
      _prevLongitude = Double.MIN_VALUE;

      final String startTime = attributes.getValue(ATTRIB_START_TIME);
      if (startTime != null) {

         final ZonedDateTime tourDateTime = ZonedDateTime.parse(startTime);

         _currentActivity.tourStartTime = tourDateTime;
         _currentActivity.tourStartTimeMills = tourDateTime.toInstant().toEpochMilli();
      }
   }

   public boolean isImported() {
      return _isImported;
   }

   public boolean isNewTag() {
      return _isNewTag;
   }

   public boolean isNewTourType() {
      return _isNewTourType;
   }

   private void parseActivity_01_Start(final String name, final Attributes attributes) {

      if (name.equals(TAG_ACTIVITY_NAME)) {

         _isInName = true;

      } else if (name.equals(TAG_ACTIVITY_NOTES)) {

         _isInNotes = true;

      } else if (name.equals(TAG_ACTIVITY_LOCATION)) {

         _currentActivity.location = attributes.getValue(ATTRIB_NAME);

      } else if (name.equals(TAG_ACTIVITY_CATEGORY)) {

         _currentActivity.categoryName = attributes.getValue(ATTRIB_NAME);
      } else if (name.equals(TAG_ACTIVITY_EQUIPMENT_ITEM)) {

         _currentActivity.equipmentName.add(attributes.getValue(ATTRIB_NAME));

      } else if (name.equals(TAG_ACTIVITY_CALORIES)) {

         //      <xs:element name="Calories">
         //         <xs:complexType>
         //            <xs:attribute name="TotalCal" type="xs:decimal" use="optional"/>
         //         </xs:complexType>
         //      </xs:element>
         _currentActivity.calories = Util.parseInt0(attributes, ATTRIB_TOTAL_CAL);

      } else if (name.equals(TAG_ACTIVITY_DURATION)) {

         //      <xs:element name="Duration">
         //         <xs:complexType>
         //            <xs:attribute name="TotalSeconds" type="xs:decimal" use="optional"/>
         //         </xs:complexType>
         //      </xs:element>
         _currentActivity.duration = Util.parseInt0(attributes, ATTRIB_TOTAL_SECONDS);

      } else if (name.equals(TAG_ACTIVITY_DISTANCE)) {

         //      <xs:element name="Distance">
         //         <xs:complexType>
         //            <xs:attribute name="TotalMeters" type="xs:decimal" use="optional"/>
         //         </xs:complexType>
         //      </xs:element>
         _currentActivity.distance = Util.parseInt0(attributes, ATTRIB_TOTAL_METERS);

      } else if (name.equals(TAG_ACTIVITY_ELEVATION)) {

         //      <xs:element name="Elevation">
         //         <xs:complexType>
         //            <xs:attribute name="DescendMeters" type="xs:decimal" use="optional"/>
         //            <xs:attribute name="AscendMeters" type="xs:decimal" use="optional"/>
         //         </xs:complexType>
         //      </xs:element>
         _currentActivity.elevationUp = Util.parseInt0(attributes, ATTRIB_ASCEND_METERS);
         _currentActivity.elevationDown = Util.parseInt0(attributes, ATTRIB_DESCEND_METERS);

      } else if (name.equals(TAG_ACTIVITY_HEART_RATE)) {

         //      <xs:element name="HeartRate">
         //         <xs:complexType>
         //            <xs:attribute name="AverageBPM" type="xs:decimal" use="optional"/>
         //            <xs:attribute name="MaximumBPM" type="xs:decimal" use="optional"/>
         //         </xs:complexType>
         //      </xs:element>
         _currentActivity.avgPulse = Util.parseInt0(attributes, ATTRIB_AVERAGE_BPM);
         _currentActivity.maxPulse = Util.parseInt0(attributes, ATTRIB_MAXIMUM_BPM);

      } else if (name.equals(TAG_ACTIVITY_POWER)) {

         _currentActivity.avgPower = Util.parseFloat0(attributes, ATTRIB_AVERAGE_WATTS);
         _currentActivity.maxPower = Util.parseFloat0(attributes, ATTRIB_MAXIMUM_WATTS);

      } else if (name.equals(TAG_ACTIVITY_CADENCE)) {

         //      <xs:element name="Cadence">
         //         <xs:complexType>
         //            <xs:attribute name="AverageRPM" type="xs:decimal" use="optional"/>
         //            <xs:attribute name="MaximumRPM" type="xs:decimal" use="optional "/>
         //         </xs:complexType>
         //      </xs:element>
         _currentActivity.avgCadence = Util.parseInt0(attributes, ATTRIB_AVERAGE_RPM);
//   !!! not yet supported !!!
//         _currentActivity.maxCadence = Util.parseInt0(attributes, ATTRIB_MAXIMUM_RPM);

      } else if (name.equals(TAG_ACTIVITY_WEATHER)) {

         _isInWeather = true;
         _currentActivity.weatherTemperature = Util.parseFloat(attributes, ATTRIB_WEATHER_TEMP);
         _currentActivity.weatherConditions = attributes.getValue(ATTRIB_WEATHER_CONDITIONS);

      } else {
         return;
      }

      _characters.delete(0, _characters.length());
   }

   private void parseActivity_02_End(final String name) {

      if (_isInName) {

         _isInName = false;
         _currentActivity.name = _characters.toString();

      } else if (_isInNotes) {

         _isInNotes = false;
         _currentActivity.notes = _characters.toString();

      } else if (_isInWeather) {

         _isInWeather = false;
         _currentActivity.weatherText = _characters.toString();
         _currentActivity.weatherWindSpeed = parseWindSpeed(_characters.toString());
      }
   }

   private void parseCustomDataFieldDefinitions(final String name, final Attributes attributes) {

      if (name.equals(TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITION)) {
         final String customFieldName = attributes.getValue(ATTRIB_CUSTOM_DATA_FIELD_DEFINITION_NAME);

         final boolean isCustomDataFieldImported = _currentActivity.customDataFields.containsKey(customFieldName);
         if (isCustomDataFieldImported) {
            final String customFieldOptions = attributes.getValue(ATTRIB_CUSTOM_DATA_FIELD_DEFINITION_OPTIONS);

            if (customFieldOptions != null && customFieldOptions.trim().length() != 0) {

               final String[] tokens = customFieldOptions.split("\\|"); //$NON-NLS-1$
               if (tokens.length < 2) {
                  return;
               }

               final int numberOfDecimals = Integer.parseInt(tokens[1]);

               final String customFieldValue = _currentActivity.customDataFields.get(customFieldName);
               try {
                  final String format = "%." + numberOfDecimals + "f"; //$NON-NLS-1$ //$NON-NLS-2$
                  final String formattedNumber = String.format(format, Double.valueOf(customFieldValue));

                  _currentActivity.customDataFields.replace(customFieldName, formattedNumber);
               } catch (final NumberFormatException e) {
                  //The value parsed was not a number
               }

            }
         }
      }
   }

   private void parseCustomDataFields(final String name, final Attributes attributes) {

      if (name.equals(TAG_ACTIVITY_CUSTOM_DATA_FIELD)) {

         final String customFieldName = attributes.getValue(ATTRIB_CUSTOM_DATA_FIELD_NAME);
         final String customFieldValue = attributes.getValue(ATTRIB_CUSTOM_DATA_FIELD_VALUE);

         final boolean isCustomDataFieldValid = customFieldName != null && !customFieldName.trim().isEmpty() &&
               customFieldValue != null && !customFieldValue.trim().isEmpty();
         if (isCustomDataFieldValid) {
            _currentActivity.customDataFields.put(
                  customFieldName,
                  customFieldValue);
         }
      }
   }

   private void parseLaps(final String name, final Attributes attributes) {

      if (name.equals(TAG_LAP)) {

         final String startTime = attributes.getValue(ATTRIB_START_TIME);
         final String durationSeconds = attributes.getValue(ATTRIB_DURATION_SECONDS);

         if (startTime != null) {

            final Lap lap = new Lap();

            final long lapDurationSeconds = (long) Float.parseFloat(durationSeconds);

            final ZonedDateTime lapEndTime = ZonedDateTime.parse(startTime).plusSeconds(lapDurationSeconds);
            lap.startTime = ZonedDateTime.parse(startTime).toInstant().toEpochMilli();
            lap.endTime = lapEndTime.toInstant().toEpochMilli();

            _currentActivity.laps.add(lap);
         }
      }
   }

   private void parsePauses(final String name, final Attributes attributes) {

      if (name.equals(TAG_PAUSE)) {

         final String startTime = attributes.getValue(ATTRIB_START_TIME);
         final String endTime = attributes.getValue(ATTRIB_END_TIME);

         if (startTime != null) {

            final Pause pause = new Pause();

            final ZonedDateTime lapStartTime = ZonedDateTime.parse(startTime);
            pause.startTime = lapStartTime.toInstant().toEpochMilli();
            final ZonedDateTime lapEndTime = ZonedDateTime.parse(endTime);
            pause.endTime = lapEndTime.toInstant().toEpochMilli();
            pause.duration = Duration.between(lapStartTime, lapEndTime).toMillis();

            _currentActivity.pauses.add(pause);
         }
      }
   }

//   private void parseTrack(final Attributes attributes) {
//
//      final String startTime = attributes.getValue(ATTRIB_START_TIME);
//
//      if (startTime != null) {
//         _currentActivity.trackTourDateTime = _dtParser.parseDateTime(startTime);
//         _currentActivity.trackTourStartTime = _currentActivity.trackTourDateTime.getMillis();
//      }
//   }

   private void parseTrackPoints(final String name, final Attributes attributes) throws InvalidDeviceSAXException {

      if (name.equals(TAG_TRACK_PT)) {

         if (_currentActivity.tourStartTimeMills == Long.MIN_VALUE) {
            throw new InvalidDeviceSAXException(NLS.bind(Messages.FitLog_Error_InvalidStartTime, _importFilePath));
         }

         final TimeData timeSlice = new TimeData();

         // relative time in seconds
         final long tmValue = Util.parseLong(attributes, ATTRIB_PT_TM);
         if (tmValue != Long.MIN_VALUE) {
            timeSlice.absoluteTime = _currentActivity.tourStartTimeMills + (tmValue * 1000);
         }

         final double tpDistance = Util.parseDouble(attributes, ATTRIB_PT_DIST);
         final double latitude = Util.parseDouble(attributes, ATTRIB_PT_LAT);
         final double longitude = Util.parseDouble(attributes, ATTRIB_PT_LON);

         if (tpDistance != Double.MIN_VALUE) {
            _distanceAbsolute = tpDistance;
         } else if (tpDistance == Double.MIN_VALUE
               && latitude != Double.MIN_VALUE
               && longitude != Double.MIN_VALUE
               && _prevLatitude != Double.MIN_VALUE
               && _prevLongitude != Double.MIN_VALUE) {

            // get distance from lat/lon when it's not set
            _distanceAbsolute += MtMath.distanceVincenty(_prevLatitude, _prevLongitude, latitude, longitude);
         }

         if (latitude != Double.MIN_VALUE && longitude != Double.MIN_VALUE) {
            _prevLatitude = latitude;
            _prevLongitude = longitude;
         }

         timeSlice.absoluteDistance = (float) _distanceAbsolute;
         timeSlice.absoluteAltitude = Util.parseFloat(attributes, ATTRIB_PT_ELE);
         timeSlice.cadence = Util.parseFloat(attributes, ATTRIB_PT_CADENCE);
         timeSlice.pulse = Util.parseFloat(attributes, ATTRIB_PT_HR);
         timeSlice.power = Util.parseFloat(attributes, ATTRIB_PT_POWER);
         timeSlice.temperature = Util.parseFloat(attributes, ATTRIB_PT_TEMP);
         timeSlice.latitude = latitude;
         timeSlice.longitude = longitude;

         _currentActivity.timeSlices.add(timeSlice);
      }
   }

   /**
    * @param weatherText
    *           Example:
    *
    *           <pre>
    *              <Weather Conditions="Clear" Temp=
   "15.5003">Min./Max.: 55.6 �F/63.9 �F; Pressure: 1004.5 mbar; Humidity: 74.9%; Dew point: 49.0 �F; Wind Speed: 1.9 mph; Precipitation: 0.0mm</Weather>
    *           </pre>
    *
    * @return
    */
   private int parseWindSpeed(final String weatherText) {

      if (!weatherText.contains(SUB_ATTRIB_WIND_SPEED) ||
            !weatherText.contains(net.tourbook.common.UI.UNIT_SPEED_MPH)) {
         return Integer.MIN_VALUE;
      }

      final int windSpeedIndex = weatherText.indexOf(SUB_ATTRIB_WIND_SPEED) + SUB_ATTRIB_WIND_SPEED.length();
      final int windSpeedUnitIndex = weatherText.indexOf(net.tourbook.common.UI.UNIT_SPEED_MPH);

      final String windSpeed = weatherText.substring(windSpeedIndex, windSpeedUnitIndex);
      float windSpeedValue = Float.parseFloat(windSpeed);

      // Converting to the current unit
      windSpeedValue *= UI.UNIT_VALUE_DISTANCE;

      return Math.round(windSpeedValue);
   }

   @Override
   public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
         throws SAXException {

      if (_isInActivity) {

         if (_isInTrack) {
            parseTrackPoints(name, attributes);
         } else if (_isInLaps) {
            parseLaps(name, attributes);
         } else if (_isInPauses) {
            parsePauses(name, attributes);
         } else if (_isInCustomDataFields) {
            parseCustomDataFields(name, attributes);
         } else {
            parseActivity_01_Start(name, attributes);
         }
      }

      if (name.equals(TAG_TRACK)) {

         _isInTrack = true;

//         parseTrack(attributes);

      } else if (name.equals(TAG_LAPS)) {

         _isInLaps = true;

      } else if (name.equals(TAG_TRACK_CLOCK)) {

         _isInPauses = true;

      } else if (name.equals(TAG_ACTIVITY)) {

         /*
          * a new exercise/tour starts
          */

         _isInActivity = true;

         initTour(attributes);
      } else if (name.equals(TAG_ACTIVITY_CUSTOM_DATA_FIELDS)) {
         _isInCustomDataFields = true;
         _hasCustomDataFields = true;

      } else if (name.equals(TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITIONS)) {
         _isInCustomDataFieldDefinitions = true;

      } else if (name.equals(TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITION)) {
         parseCustomDataFieldDefinitions(name, attributes);

      }
   }

}
