/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.device.InvalidDeviceSAXException;
import net.tourbook.device.Messages;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.TagWithNotes;
import net.tourbook.importdata.TourTypeWrapper;
import net.tourbook.ui.tourChart.ChartLabelMarker;

import org.eclipse.osgi.util.NLS;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FitLog_SAXHandler extends DefaultHandler {

   private static final String                  TAG_ACTIVITY                           = "Activity";                           //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CADENCE                   = "Cadence";                            //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CALORIES                  = "Calories";                           //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CATEGORY                  = "Category";                           //$NON-NLS-1$
   static final String                          TAG_ACTIVITY_CUSTOM_DATA_FIELD         = "CustomDataField";                    //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CUSTOM_DATA_FIELDS        = TAG_ACTIVITY_CUSTOM_DATA_FIELD + "s"; //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_DURATION                  = "Duration";                           //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_DISTANCE                  = "Distance";                           //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_ELEVATION                 = "Elevation";                          //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_EQUIPMENT_ITEM            = "EquipmentItem";                      //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_HAS_START_TIME            = "HasStartTime";                       //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_HEART_RATE                = "HeartRate";                          //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_LOCATION                  = "Location";                           //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_NAME                      = "Name";                               //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_NOTES                     = "Notes";                              //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_POWER                     = "Power";                              //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_TIMEZONE_UTC_OFFSET       = "TimeZoneUtcOffset";                  //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_WEATHER                   = "Weather";                            //$NON-NLS-1$
   static final String                          TAG_EQUIPMENT_DATE_PURCHASED           = "DatePurchased";                      //$NON-NLS-1$
   static final String                          TAG_EQUIPMENT_EXPECTED_LIFE_KILOMETERS = "ExpectedLifeKilometers";             //$NON-NLS-1$
   static final String                          TAG_EQUIPMENT_IN_USE                   = "InUse";                              //$NON-NLS-1$
   static final String                          TAG_EQUIPMENT_NOTES                    = "Notes";                              //$NON-NLS-1$
   static final String                          TAG_EQUIPMENT_PURCHASE_LOCATION        = "PurchaseLocation";                   //$NON-NLS-1$
   static final String                          TAG_EQUIPMENT_PURCHASE_PRICE           = "PurchasePrice";                      //$NON-NLS-1$
   static final String                          TAG_EQUIPMENT_TYPE                     = "Type";                               //$NON-NLS-1$
   static final String                          TAG_EQUIPMENT_WEIGHT_KILOGRAMS         = "WeightKilograms";                    //$NON-NLS-1$

   private static final String                  ATTRIB_CUSTOM_DATA_FIELD_NAME          = "name";                               //$NON-NLS-1$
   private static final String                  ATTRIB_CUSTOM_DATA_FIELD_VALUE         = "v";                                  //$NON-NLS-1$
   private static final String                  ATTRIB_DURATION_SECONDS                = "DurationSeconds";                    //$NON-NLS-1$
   static final String                          ATTRIB_EQUIPMENT_ID                    = "Id";                                 //$NON-NLS-1$
   private static final String                  ATTRIB_NAME                            = "Name";                               //$NON-NLS-1$
   private static final String                  ATTRIB_START_TIME                      = "StartTime";                          //$NON-NLS-1$
   private static final String                  ATTRIB_END_TIME                        = "EndTime";                            //$NON-NLS-1$
   private static final String                  ATTRIB_TOTAL_SECONDS                   = "TotalSeconds";                       //$NON-NLS-1$
   private static final String                  ATTRIB_TOTAL_METERS                    = "TotalMeters";                        //$NON-NLS-1$
   private static final String                  ATTRIB_TOTAL_CAL                       = "TotalCal";                           //$NON-NLS-1$
   private static final String                  ATTRIB_ASCEND_METERS                   = "AscendMeters";                       //$NON-NLS-1$
   private static final String                  ATTRIB_DESCEND_METERS                  = "DescendMeters";                      //$NON-NLS-1$
   private static final String                  ATTRIB_AVERAGE_BPM                     = "AverageBPM";                         //$NON-NLS-1$
   private static final String                  ATTRIB_MAXIMUM_BPM                     = "MaximumBPM";                         //$NON-NLS-1$
   private static final String                  ATTRIB_AVERAGE_WATTS                   = "AverageWatts";                       //$NON-NLS-1$
   private static final String                  ATTRIB_MAXIMUM_WATTS                   = "MaximumWatts";                       //$NON-NLS-1$
   private static final String                  ATTRIB_AVERAGE_RPM                     = "AverageRPM";                         //$NON-NLS-1$
   private static final String                  ATTRIB_WEATHER_TEMP                    = "Temp";                               //$NON-NLS-1$
   private static final String                  ATTRIB_WEATHER_CONDITIONS              = "Conditions";                         //$NON-NLS-1$
   //
   private static final String                  TAG_TRACK                              = "Track";                              //$NON-NLS-1$
   private static final String                  TAG_TRACK_PT                           = "pt";                                 //$NON-NLS-1$
   private static final String                  ATTRIB_PT_CADENCE                      = "cadence";                            //$NON-NLS-1$
   private static final String                  ATTRIB_PT_DIST                         = "dist";                               //$NON-NLS-1$
   private static final String                  ATTRIB_PT_ELE                          = "ele";                                //$NON-NLS-1$
   private static final String                  ATTRIB_PT_HR                           = "hr";                                 //$NON-NLS-1$
   private static final String                  ATTRIB_PT_LAT                          = "lat";                                //$NON-NLS-1$
   private static final String                  ATTRIB_PT_LON                          = "lon";                                //$NON-NLS-1$
   private static final String                  ATTRIB_PT_POWER                        = "power";                              //$NON-NLS-1$
   private static final String                  ATTRIB_PT_TEMP                         = "temp";                               //$NON-NLS-1$
   private static final String                  ATTRIB_PT_TM                           = "tm";                                 //$NON-NLS-1$
   //
   private static final String                  TAG_LAPS                               = "Laps";                               //$NON-NLS-1$
   private static final String                  TAG_LAP                                = "Lap";                                //$NON-NLS-1$
   private static final String                  TAG_TRACK_CLOCK                        = "TrackClock";                         //$NON-NLS-1$
   private static final String                  TAG_PAUSE                              = "Pause";                              //$NON-NLS-1$

   private static final String                  SUB_ATTRIB_WIND_SPEED                  = "Wind Speed:";                        //$NON-NLS-1$
   //
   private static final char                    NL                                     = UI.NEW_LINE;
   private static final String                  COLON_SPACE                            = ": ";                                 //$NON-NLS-1$

   private static final HashMap<String, String> _weatherId                             = new HashMap<>();
   static {

      /*
       * Entries which are marked with *) have not a corresponding id/image within MyTourbook
       */

// SET_FORMATTING_OFF

      _weatherId.put("Clear",          IWeather.WEATHER_ID_CLEAR); //                     //$NON-NLS-1$
      _weatherId.put("ScatterClouds",  IWeather.WEATHER_ID_PART_CLOUDS); //         *)    //$NON-NLS-1$
      _weatherId.put("PartClouds",     IWeather.WEATHER_ID_PART_CLOUDS); //               //$NON-NLS-1$
      _weatherId.put("Overcast",       IWeather.WEATHER_ID_OVERCAST); //                  //$NON-NLS-1$
      _weatherId.put("MostClouds",     IWeather.WEATHER_ID_OVERCAST); //            *)    //$NON-NLS-1$
      _weatherId.put("Clouds",         IWeather.WEATHER_ID_OVERCAST); //            *)    //$NON-NLS-1$
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
   //
   private String                          _importFilePath;
   private ImportState_File                _importState_File;
   private ImportState_Process             _importState_Process;
   private FitLogDeviceDataReader          _device;

   private Map<Long, TourData>             _alreadyImportedTours;
   private Map<Long, TourData>             _newlyImportedTours;

   private Activity                        _currentActivity;
   private double                          _prevLatitude;
   private double                          _prevLongitude;

   private double                          _distanceAbsolute;

   /**
    * Is currently disabled because it should work for all cases
    */
   //private boolean                         _isReimport;
   private boolean                         _isInActivity;
   private boolean                         _isInCustomDataFields;
   private boolean                         _isInHasStartTime;
   private boolean                         _isInLaps;
   private boolean                         _isInName;
   private boolean                         _isInNotes;
   private boolean                         _isInPauses;
   private boolean                         _isInTimeZoneUtcOffset;
   private boolean                         _isInTrack;
   private boolean                         _isInWeather;

   private Map<String, Integer>            _customDataFieldDefinitions;

   private StringBuilder                   _characters       = new StringBuilder(100);

   /**
    * Key is the tag name + contained id, all in UPPERCASE
    */
   private final Map<String, TagWithNotes> _allTagsWithNotes = new HashMap<>();

   private class Activity {

      private List<TimeData>  timeSlices         = new ArrayList<>();
      private List<Lap>       laps               = new ArrayList<>();
      private List<Pause>     pauses             = new ArrayList<>();
      private List<Equipment> equipments         = new ArrayList<>();

      private ZonedDateTime   tourStartTime;
      private long            tourStartTimeMills = Long.MIN_VALUE;

      private String          location;
      private String          name;
      private String          notes;
      private String          categoryName;

      private int             calories;
      private int             duration;
      private int             distance;

      private int             elevationUp;
      private int             elevationDown;

      private int             avgPulse;
      private int             maxPulse;

      private float           avgPower;
      private float           maxPower;

      private int             timeZoneUtcOffset;
      private boolean         hasTimeZoneUtcOffset;
      private boolean         hasStartTime;

      private boolean         hasGpsData;

      private int             avgCadence;
//    private int             maxCadence;      is not yet supported

      private String                        weatherText;
      private String                        weatherConditions;
      private float                         weatherTemperature = Float.MIN_VALUE;
      private int                           weatherWindSpeed   = Integer.MIN_VALUE;

      private LinkedHashMap<String, String> customDataFields   = new LinkedHashMap<>();
   }

   public static class Equipment {

      String Id;
      String Name;

      String DatePurchased;
      String ExpectedLifeKilometers;
      String InUse;
      String Notes;
      String PurchaseLocation;
      String PurchasePrice;
      String Type;
      String WeightKilograms;

      // Properties only used to generate the equipment name
      String Brand;
      String Model;

      public String generateNotes() {

         final StringBuilder notes = new StringBuilder(ATTRIB_EQUIPMENT_ID + "(SportTracks): " + Id); //$NON-NLS-1$

// SET_FORMATTING_OFF

         if (StringUtils.hasContent(DatePurchased)) {
            notes.append(NL + TAG_EQUIPMENT_DATE_PURCHASED           + COLON_SPACE + DatePurchased);
         }
         if (StringUtils.hasContent(ExpectedLifeKilometers)) {
            notes.append(NL + TAG_EQUIPMENT_EXPECTED_LIFE_KILOMETERS + COLON_SPACE + ExpectedLifeKilometers);
         }
         if (StringUtils.hasContent(InUse)) {
            notes.append(NL + TAG_EQUIPMENT_IN_USE                   + COLON_SPACE + InUse);
         }
         if (StringUtils.hasContent(PurchaseLocation)) {
            notes.append(NL + TAG_EQUIPMENT_PURCHASE_LOCATION        + COLON_SPACE + PurchaseLocation);
         }
         if (StringUtils.hasContent(PurchasePrice)) {
            notes.append(NL + TAG_EQUIPMENT_PURCHASE_PRICE           + COLON_SPACE + PurchasePrice);
         }
         if (StringUtils.hasContent(Type)) {
            notes.append(NL + TAG_EQUIPMENT_TYPE                     + COLON_SPACE + Type);
         }
         if (StringUtils.hasContent(WeightKilograms)
               && !WeightKilograms.equals("0.000")) { //$NON-NLS-1$
            notes.append(NL + TAG_EQUIPMENT_WEIGHT_KILOGRAMS         + COLON_SPACE + WeightKilograms);
         }
         if (StringUtils.hasContent(Notes)) {
            notes.append(NL + TAG_EQUIPMENT_NOTES                    + COLON_SPACE + Notes);
         }

// SET_FORMATTING_ON

         return notes.toString();
      }

      public String getName() {

         // use name when available
         if (StringUtils.hasContent(Name)) {
            return Name;
         }

         // create a name from: Brand + Model

         final StringBuilder name = new StringBuilder();
         if (StringUtils.hasContent(Brand)) {
            name.append(Brand);
         }
         if (StringUtils.hasContent(Model)) {
            if (name.length() > 0) {
               name.append(UI.DASH_WITH_SPACE + Model);
            } else {
               name.append(Model);
            }
         }

         if (name.length() == 0) {

            // Yes, it's crazy but I tested and an equipment can have no model and brand!
            name.append(Messages.FitLog_Equipment_Name_Not_Available);
         }

         return name.toString();
      }

      @Override
      public String toString() {

         return UI.EMPTY_STRING

               + "Equipment" + NL //                                             //$NON-NLS-1$

               + "[" + NL //                                                     //$NON-NLS-1$

               + "Id                      =" + Id + NL //                        //$NON-NLS-1$
               + "Name                    =" + Name + NL //                      //$NON-NLS-1$
               + "DatePurchased           =" + DatePurchased + NL //             //$NON-NLS-1$
               + "ExpectedLifeKilometers  =" + ExpectedLifeKilometers + NL //    //$NON-NLS-1$
               + "InUse                   =" + InUse + NL //                     //$NON-NLS-1$
               + "Notes                   =" + Notes + NL //                     //$NON-NLS-1$
               + "PurchaseLocation        =" + PurchaseLocation + NL //          //$NON-NLS-1$
               + "PurchasePrice           =" + PurchasePrice + NL //             //$NON-NLS-1$
               + "Type                    =" + Type + NL //                      //$NON-NLS-1$
               + "WeightKilograms         =" + WeightKilograms + NL //           //$NON-NLS-1$
               + "Brand                   =" + Brand + NL //                     //$NON-NLS-1$
               + "Model                   =" + Model + NL //                     //$NON-NLS-1$

               + "]" + NL //                                                     //$NON-NLS-1$
         ;
      }
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

   public FitLog_SAXHandler(final String importFilePath,
                            final Map<Long, TourData> alreadyImportedTours,
                            final Map<Long, TourData> newlyImportedTours,
                            final boolean isFitLogExFile,

                            final ImportState_File importState_File,
                            final ImportState_Process importState_Process,

                            final FitLogDeviceDataReader device) {

      _importFilePath = importFilePath;
      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;

      _importState_File = importState_File;
      _importState_Process = importState_Process;

      _device = device;

//      _isReimport = importState_Process.isReimport();

      if (isFitLogExFile) {

         // We parse the custom field definitions and equipments
         // separately as they can be anywhere in the file

         final FitLogEx_SAXHandler fitLogEx_SaxHandler = new FitLogEx_SAXHandler();

         try {

            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, UI.EMPTY_STRING);
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, UI.EMPTY_STRING);

            parser.parse("file:" + importFilePath, fitLogEx_SaxHandler);//$NON-NLS-1$

         } catch (final InvalidDeviceSAXException e) {
            StatusUtil.log(e);
         } catch (final Exception e) {
            StatusUtil.log("Error parsing file: " + importFilePath, e); //$NON-NLS-1$
         }

         _customDataFieldDefinitions = fitLogEx_SaxHandler.getCustomDataFieldDefinitions();

//         if (!_isReimport) {

         // normal import

         // create a list with all equipments which can be used to create TourTag's

         final List<Equipment> allEquipments_FromFitLogEx = fitLogEx_SaxHandler.getEquipments();

         for (final Equipment equipment : allEquipments_FromFitLogEx) {

            final String name = equipment.getName();
            final String id = equipment.Id;

            final String key = (name + UI.DASH + id).toUpperCase();

            _allTagsWithNotes.put(key,

                  new TagWithNotes(

                        name,
                        equipment.generateNotes(),
                        id

                  ));
         }
//         }
      }
   }

   private long addPauseTimeInLap(final Lap lap, long startTimeDiff) {

      for (final Pause pause : _currentActivity.pauses) {

         //We need to make sure to only add the pauses that are
         //within the current lap time interval.
         if (pause.startTime < lap.endTime && pause.endTime > lap.startTime) {
            startTimeDiff += pause.duration;
         }
      }
      return startTimeDiff;
   }

   /**
    * Updates a given time based on the tour start's time zone
    *
    * @param epochTime
    * @return The adjusted time
    */
   private long adjustTime(final long epochTime) {

      long convertedEpochTime;
      ZonedDateTime zonedDateTimeWithUTCOffset;

      if (_currentActivity.hasTimeZoneUtcOffset) {

         zonedDateTimeWithUTCOffset = Instant.ofEpochMilli(epochTime)
               .atOffset(ZoneOffset.ofHours(_currentActivity.timeZoneUtcOffset))
               .toZonedDateTime();
      } else {

         zonedDateTimeWithUTCOffset = TimeTools.getZonedDateTimeWithUTC(epochTime);
      }

      convertedEpochTime = zonedDateTimeWithUTCOffset.withZoneSameInstant(_currentActivity.tourStartTime.getZone()).toInstant().toEpochMilli();

      return convertedEpochTime;
   }

   private void adjustTourTimeZoneId(final TourData tourData, final ZonedDateTime tourStartTime_FromImport) {

      if ((tourData.latitudeSerie != null && tourData.latitudeSerie.length != 0) || !_currentActivity.hasTimeZoneUtcOffset
            || !_currentActivity.hasStartTime) {

         // No need to set the timezone Id if the activity has GPS coordinates (as it was already done
         // when the time series were created) or if the activity has no time zone UTC offset or no start time.
         return;
      }

      final int offSet = tourStartTime_FromImport.getOffset().getTotalSeconds() * 1000;
      final String[] ids = TimeZone.getAvailableIDs(offSet);
      /*
       * Based on this information
       * https://stackoverflow.com/questions/57468423/java-8-time-zone-zonerulesexception-unknown
       * -time-zone-id-est
       * We intersect the list of ids found based on the tour offset with the list of ZoneIds
       * because, ultimately, a ZoneId is expected {@link TourData#getTimeZoneIdWithDefault}
       */
      final List<String> finalZoneIds = Arrays.stream(ids)
            .distinct()
            .filter(ZoneId.getAvailableZoneIds()::contains)
            .collect(Collectors.toList());
      //We set the first found time zone that corresponds to the activity offset
      if (finalZoneIds.size() > 0) {
         tourData.setTimeZoneId(finalZoneIds.get(0));
      }
   }

   @Override
   public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

      if (_isInTimeZoneUtcOffset || _isInHasStartTime || _isInName || _isInNotes || _isInWeather) {

         _characters.append(chars, startIndex, length);
      }
   }

   private TourData createTour() {

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
      tourData.setWeather_Clouds(_weatherId.get(_currentActivity.weatherConditions));

      final float weatherTemperature = _currentActivity.weatherTemperature;
      if (weatherTemperature != Float.MIN_VALUE) {
         tourData.setWeather_Temperature_Average(weatherTemperature);
      }

      if (_currentActivity.weatherWindSpeed != Integer.MIN_VALUE) {
         tourData.setWeather_Wind_Speed(_currentActivity.weatherWindSpeed);
      }

      if (_currentActivity.customDataFields.size() > 0) {

         final StringBuilder tourNotes = new StringBuilder(tourData.getTourDescription());

         if (!tourNotes.toString().trim().isEmpty()) {
            tourNotes.append(UI.NEW_LINE2);
         }

         tourNotes.append(Messages.FitLog_CustomDataFields_Label);
         _currentActivity.customDataFields.forEach((key, value) -> {
            if (!tourNotes.toString().trim().isEmpty()) {
               //If there is already content in the notes fields, then we insert a new line
               tourNotes.append(UI.NEW_LINE);
            }
            tourNotes.append("\"" + key + "\" : \"" + value + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         });

         tourData.setTourDescription(tourNotes.toString());
      }

      tourData.setImportFilePath(_importFilePath);

      tourData.setDeviceTimeInterval((short) -1);
      if (_currentActivity.timeSlices.isEmpty()) {

         // tour do not contain a track

         tourData.setTourDistance(_currentActivity.distance);

         tourData.setTourDeviceTime_Elapsed(_currentActivity.duration);
         tourData.setTourComputedTime_Moving(_currentActivity.duration);

         tourData.setTourAltUp(_currentActivity.elevationUp);
         tourData.setTourAltDown(_currentActivity.elevationDown);

         // We set the tour as manual since it was a manual tour created in SportTracks in the first place.
         tourData.setDeviceId(TourData.DEVICE_ID_FOR_MANUAL_TOUR);

      } else {

         // create 'normal' tour

         tourData.createTimeSeries(_currentActivity.timeSlices, false);

         // If the activity doesn't have GPS data but contains a distance value,
         // we set the distance manually
         if (!_currentActivity.hasGpsData && _currentActivity.distance > 0) {
            tourData.setTourDistance(_currentActivity.distance);
         }

         /*
          * The tour start time timezone is set from lat/lon in createTimeSeries()
          */
         final ZonedDateTime tourStartTime_FromLatLon = tourData.getTourStartTime();

         if (!_currentActivity.tourStartTime.equals(tourStartTime_FromLatLon)) {

            // time zone is different -> fix tour start components with adjusted time zone
            tourData.setTourStartTime_YYMMDD(tourStartTime_FromLatLon);
         }

         tourData.setDeviceId(_device.deviceId);
      }

      return tourData;
   }

   private TourMarker createTourMarker(final TourData tourData,
                                       final String label,
                                       final int lapRelativeTime,
                                       final int serieIndex) {

      final float[] altitudeSerie = tourData.altitudeSerie;
      final float[] distanceSerie = tourData.distanceSerie;
      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      final TourMarker tourMarker = new TourMarker(tourData, ChartLabelMarker.MARKER_TYPE_DEVICE);

      tourMarker.setLabel(label);
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
      return tourMarker;
   }

   @Override
   public void endElement(final String uri, final String localName, final String name) throws SAXException {

      /*
       * get values
       */
      if (_isInTimeZoneUtcOffset || _isInHasStartTime || _isInName || _isInNotes || _isInWeather) {
         parseActivity_02_End();
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

      } else if (name.equals(TAG_ACTIVITY)) {

         // activity/tour ends
         _isInActivity = false;

         finalizeTour();
      }
   }

   private void finalizeTour() {

      // create data object for each tour
      final TourData tourData = createTour();

      final ZonedDateTime tourStartTime_FromImport = _currentActivity.tourStartTime;

      if (_currentActivity.avgPower != 0) {
         tourData.setPower_Avg(_currentActivity.avgPower);
      } else {
         final float[] powerSerie = tourData.getPowerSerie();
         if (powerSerie != null) {
            tourData.setPower_Avg(tourData.computeAvg_FromValues(powerSerie, 0, powerSerie.length - 1));
         }
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

      if (_currentActivity.pauses.size() > 0) {

         final List<Long> _pausedTime_Start = new ArrayList<>();
         final List<Long> _pausedTime_End = new ArrayList<>();

         for (final Pause element : _currentActivity.pauses) {

            element.startTime = adjustTime(element.startTime);
            element.endTime = adjustTime(element.endTime);

            _pausedTime_Start.add(element.startTime);
            _pausedTime_End.add(element.endTime);
         }

         tourData.finalizeTour_TimerPauses(_pausedTime_Start, _pausedTime_End, null);
      }

      adjustTourTimeZoneId(tourData, tourStartTime_FromImport);

      tourData.setDeviceName(_device.visibleName);

      // after all data are added, the tour id can be created because it depends on the tour distance
      final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_SPORT_TRACKS_FITLOG);
      final Long tourId = tourData.createTourId(uniqueId);

      // check if the tour is already imported
      if (!_alreadyImportedTours.containsKey(tourId)) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, tourData);

         // create additional data
         if (_currentActivity.timeSlices.isEmpty()) {
            tourData.computeTourMovingTime();
         }

         tourData.setTourDeviceTime_Recorded(tourData.getTourDeviceTime_Elapsed() - tourData.getTourDeviceTime_Paused());
         tourData.computeAltitudeUpDown();
         tourData.computeComputedValues();

//         if (!_isReimport) {

         // normal import

         finalizeTour_10_SetTourType(tourData);
         finalizeTour_20_SetTags(tourData);
//         }

         finalizeTour_30_CreateMarkers(tourData);
      }

      // cleanup
      _currentActivity.timeSlices.clear();
      _currentActivity.laps.clear();
      _currentActivity.equipments.clear();

      _importState_File.isFileImportedWithValidData = true;
   }

   /**
    * Set tour type from category field
    *
    * @param tourData
    */
   private void finalizeTour_10_SetTourType(final TourData tourData) {

      final String categoryName = _currentActivity.categoryName;

      if (StringUtils.isNullOrEmpty(categoryName)) {
         return;
      }

      final TourTypeWrapper tourTypeWrapper = RawDataManager.setTourType(tourData, categoryName);

      if (tourTypeWrapper != null && tourTypeWrapper.isNewTourType) {
         _importState_Process.isCreated_NewTourType().set(true);
      }
   }

   /**
    * Set tags from all equipments by using it's names AND equipment IDs
    *
    * @param tourData
    */
   private void finalizeTour_20_SetTags(final TourData tourData) {

      final boolean isNewTourTag = RawDataManager.setTourTags(tourData, _allTagsWithNotes);

      if (isNewTourTag) {
         _importState_Process.isCreated_NewTag().set(true);
      }
   }

   private void finalizeTour_30_CreateMarkers(final TourData tourData) {

      final List<Lap> _laps = _currentActivity.laps;
      if (_laps.isEmpty()) {
         return;
      }

      final int[] timeSerie = tourData.timeSerie;
      if (timeSerie == null || timeSerie.length == 0) {
         // fixed bug: http://sourceforge.net/support/tracker.php?aid=3232030
         return;
      }

      /*
       * tour and track can have different start times
       */
      final long tourStartTime = _currentActivity.tourStartTimeMills;
//      final long tour2sliceTimeDiff = _currentActivity.trackTourStartTime - tourStartTime;

      int lapCounter = 1;
      final Set<TourMarker> tourMarkers = tourData.getTourMarkers();
      for (final Lap lap : _laps) {

         lap.startTime = adjustTime(lap.startTime);
         lap.endTime = adjustTime(lap.endTime);

         long startTimeDiff = lap.endTime - tourStartTime;// - tour2sliceTimeDiff;

         // If present, we add the total pause time
         startTimeDiff = addPauseTimeInLap(lap, startTimeDiff);

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

         final TourMarker tourMarker = createTourMarker(tourData,
               Integer.toString(lapCounter),
               lapRelativeTime,
               serieIndex);

         tourMarkers.add(tourMarker);

         lapCounter++;
      }
   }

   /**
    * Format a given custom data field value if this field has a formatting rule in the
    * custom data field definitions
    * Example : Double with 2 decimals
    * Name="cts/mile" GroupAggregation="Average" Options="#$x02|2|0">
    *
    * @param activity
    *           The activity for which the custom data field value needs to be formatted
    * @param customDataFieldName
    *           The custom field name
    * @param customDataFieldValue
    *           The custom field value to be formatted
    */
   private void formatCustomDataFieldValue(final Activity activity, final String customDataFieldName, final String customDataFieldValue) {

      // If there is no custom data field definition for the current field, we add its value as-is.
      if (!_customDataFieldDefinitions.containsKey(customDataFieldName)) {
         activity.customDataFields.put(customDataFieldName, customDataFieldValue);
         return;
      }

      //Otherwise, we round the value to the number of specified decimals
      final int numberOfDecimals = _customDataFieldDefinitions.get(customDataFieldName);
      try {
         final String format = "%." + numberOfDecimals + "f"; //$NON-NLS-1$ //$NON-NLS-2$
         final String formattedNumber = String.format(format, (double) Math.round(Double.valueOf(customDataFieldValue)));

         if (activity.customDataFields.containsKey(customDataFieldName)) {
            activity.customDataFields.replace(customDataFieldName, formattedNumber);
         } else {
            activity.customDataFields.put(customDataFieldName, formattedNumber);
         }

      } catch (final NumberFormatException e) {
         //The value parsed was not a number
      }

   }

   private void initTour(final Attributes attributes) {

      _currentActivity = new Activity();

      _distanceAbsolute = 0;

      _prevLatitude = Double.MIN_VALUE;
      _prevLongitude = Double.MIN_VALUE;

      final String startTime = attributes.getValue(ATTRIB_START_TIME);
      if (StringUtils.hasContent(startTime)) {

         final ZonedDateTime tourDateTime = ZonedDateTime.parse(startTime);

         _currentActivity.tourStartTime = tourDateTime;
         _currentActivity.tourStartTimeMills = tourDateTime.toInstant().toEpochMilli();
      }
   }

   private void parseActivity_01_Start(final String name, final Attributes attributes) {

      switch (name) {
      case TAG_ACTIVITY_NAME:
         _isInName = true;
         break;
      case TAG_ACTIVITY_NOTES:
         _isInNotes = true;
         break;
      case TAG_ACTIVITY_LOCATION:
         _currentActivity.location = attributes.getValue(ATTRIB_NAME);
         break;
      case TAG_ACTIVITY_CATEGORY:
         _currentActivity.categoryName = attributes.getValue(ATTRIB_NAME);
         break;

      case TAG_ACTIVITY_EQUIPMENT_ITEM:

         final Equipment newEquipment = new Equipment();

         newEquipment.Name = attributes.getValue(ATTRIB_NAME);
         newEquipment.Id = attributes.getValue(ATTRIB_EQUIPMENT_ID);

         _currentActivity.equipments.add(newEquipment);
         break;

      case TAG_ACTIVITY_CALORIES:
         // Converting from Calories to calories
         _currentActivity.calories = Math.round(Util.parseFloat0(attributes, ATTRIB_TOTAL_CAL) * 1000f);
         break;
      case TAG_ACTIVITY_DURATION:
         //      <xs:element name="Duration">
         //         <xs:complexType>
         //            <xs:attribute name="TotalSeconds" type="xs:decimal" use="optional"/>
         //         </xs:complexType>
         //      </xs:element>
         _currentActivity.duration = Util.parseInt0(attributes, ATTRIB_TOTAL_SECONDS);
         break;
      case TAG_ACTIVITY_DISTANCE:
         //      <xs:element name="Distance">
         //         <xs:complexType>
         //            <xs:attribute name="TotalMeters" type="xs:decimal" use="optional"/>
         //         </xs:complexType>
         //      </xs:element>
         _currentActivity.distance = Util.parseInt0(attributes, ATTRIB_TOTAL_METERS);
         break;
      case TAG_ACTIVITY_ELEVATION:
         //      <xs:element name="Elevation">
         //         <xs:complexType>
         //            <xs:attribute name="DescendMeters" type="xs:decimal" use="optional"/>
         //            <xs:attribute name="AscendMeters" type="xs:decimal" use="optional"/>
         //         </xs:complexType>
         //      </xs:element>
         _currentActivity.elevationUp = Util.parseInt0(attributes, ATTRIB_ASCEND_METERS);
         _currentActivity.elevationDown = Util.parseInt0(attributes, ATTRIB_DESCEND_METERS);
         break;
      case TAG_ACTIVITY_HEART_RATE:
         //      <xs:element name="HeartRate">
         //         <xs:complexType>
         //            <xs:attribute name="AverageBPM" type="xs:decimal" use="optional"/>
         //            <xs:attribute name="MaximumBPM" type="xs:decimal" use="optional"/>
         //         </xs:complexType>
         //      </xs:element>
         _currentActivity.avgPulse = Util.parseInt0(attributes, ATTRIB_AVERAGE_BPM);
         _currentActivity.maxPulse = Util.parseInt0(attributes, ATTRIB_MAXIMUM_BPM);
         break;
      case TAG_ACTIVITY_POWER:
         _currentActivity.avgPower = Util.parseFloat0(attributes, ATTRIB_AVERAGE_WATTS);
         _currentActivity.maxPower = Util.parseFloat0(attributes, ATTRIB_MAXIMUM_WATTS);
         break;
      case TAG_ACTIVITY_TIMEZONE_UTC_OFFSET:
         _isInTimeZoneUtcOffset = true;
         break;
      case TAG_ACTIVITY_HAS_START_TIME:
         _isInHasStartTime = true;
         break;
      case TAG_ACTIVITY_CADENCE:
         //      <xs:element name="Cadence">
         //         <xs:complexType>
         //            <xs:attribute name="AverageRPM" type="xs:decimal" use="optional"/>
         //            <xs:attribute name="MaximumRPM" type="xs:decimal" use="optional "/>
         //         </xs:complexType>
         //      </xs:element>
         _currentActivity.avgCadence = Util.parseInt0(attributes, ATTRIB_AVERAGE_RPM);
//   !!! not yet supported !!!
//         _currentActivity.maxCadence = Util.parseInt0(attributes, ATTRIB_MAXIMUM_RPM);
         break;
      case TAG_ACTIVITY_WEATHER:
         _isInWeather = true;
         _currentActivity.weatherTemperature = Util.parseFloat(attributes, ATTRIB_WEATHER_TEMP);
         _currentActivity.weatherConditions = attributes.getValue(ATTRIB_WEATHER_CONDITIONS);
         break;
      default:
         return;
      }

      _characters.delete(0, _characters.length());
   }

   private void parseActivity_02_End() {

      if (_isInName) {

         _isInName = false;
         _currentActivity.name = _characters.toString();

      } else if (_isInNotes) {

         _isInNotes = false;
         _currentActivity.notes = _characters.toString();

      } else if (_isInWeather) {

         _isInWeather = false;
         _currentActivity.weatherText = _characters.toString().trim();
         _currentActivity.weatherWindSpeed = parseWindSpeed(_characters.toString());

      } else if (_isInTimeZoneUtcOffset) {

         _isInTimeZoneUtcOffset = false;
         _currentActivity.hasTimeZoneUtcOffset = false;

         if (StringUtils.isNullOrEmpty(_characters.toString())) {
            return;
         }

         final int timeZoneUtcOffset = Integer.parseInt(_characters.toString());

         _currentActivity.hasTimeZoneUtcOffset = true;
         _currentActivity.timeZoneUtcOffset = timeZoneUtcOffset / 3600;

         //We update the tour start time with the retrieved UTC offset
         final ZonedDateTime tourStartTimeWithUTCOffset = _currentActivity.tourStartTime.toInstant()
               .atOffset(ZoneOffset.ofHours(
                     _currentActivity.timeZoneUtcOffset))
               .toZonedDateTime();
         _currentActivity.tourStartTime = tourStartTimeWithUTCOffset;
         _currentActivity.tourStartTimeMills = tourStartTimeWithUTCOffset.toInstant().toEpochMilli();

      } else if (_isInHasStartTime) {

         _isInHasStartTime = false;

         _currentActivity.hasStartTime = Boolean.parseBoolean(_characters.toString());

         if (!_currentActivity.hasStartTime) {
            //We remove the hour from the start time
            final ZonedDateTime tourDateTime = ZonedDateTime.parse(String.format("%d-%02d-%02dT00:00:00.000Z", //$NON-NLS-1$
                  _currentActivity.tourStartTime.getYear(),
                  _currentActivity.tourStartTime.getMonthValue(),
                  _currentActivity.tourStartTime.getDayOfMonth()));

            _currentActivity.tourStartTime = tourDateTime;
         }
      }
   }

   private void parseCustomDataFields(final String name, final Attributes attributes) {

      if (name.equals(TAG_ACTIVITY_CUSTOM_DATA_FIELD)) {

         final String customFieldName = attributes.getValue(ATTRIB_CUSTOM_DATA_FIELD_NAME);
         final String customFieldValue = attributes.getValue(ATTRIB_CUSTOM_DATA_FIELD_VALUE);

         final boolean isCustomDataFieldValid = StringUtils.hasContent(customFieldName) &&
               StringUtils.hasContent(customFieldValue);

         if (isCustomDataFieldValid) {
            formatCustomDataFieldValue(_currentActivity, customFieldName, customFieldValue);
         }
      }
   }

   private void parseLaps(final String name, final Attributes attributes) {

      if (name.equals(TAG_LAP)) {

         final String startTime = attributes.getValue(ATTRIB_START_TIME);
         final String durationSeconds = attributes.getValue(ATTRIB_DURATION_SECONDS);

         if (StringUtils.hasContent(startTime)) {

            final Lap lap = new Lap();

            final long lapDurationSeconds = (long) Float.parseFloat(durationSeconds);

            final ZonedDateTime startZonedDateTime = ZonedDateTime.parse(startTime);
            final ZonedDateTime lapEndTime = startZonedDateTime.plusSeconds(lapDurationSeconds);
            lap.startTime = startZonedDateTime.toInstant().toEpochMilli();
            lap.endTime = lapEndTime.toInstant().toEpochMilli();

            _currentActivity.laps.add(lap);
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

   private void parsePauses(final String name, final Attributes attributes) {

      if (name.equals(TAG_PAUSE)) {

         final String startTime = attributes.getValue(ATTRIB_START_TIME);
         final String endTime = attributes.getValue(ATTRIB_END_TIME);

         if (StringUtils.hasContent(startTime)) {

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

   private void parseTrackPoints(final String name, final Attributes attributes) throws InvalidDeviceSAXException {

      if (name.equals(TAG_TRACK_PT)) {

         if (_currentActivity.tourStartTimeMills == Long.MIN_VALUE) {
            throw new InvalidDeviceSAXException(NLS.bind(Messages.FitLog_Error_InvalidStartTime, _importFilePath));
         }

         final TimeData timeSlice = new TimeData();

         final double tpDistance = Util.parseDouble(attributes, ATTRIB_PT_DIST);
         final double latitude = Util.parseDouble(attributes, ATTRIB_PT_LAT);
         final double longitude = Util.parseDouble(attributes, ATTRIB_PT_LON);

         if (tpDistance != Double.MIN_VALUE) {

            _distanceAbsolute = tpDistance;
         } else if (latitude != Double.MIN_VALUE
               && longitude != Double.MIN_VALUE
               && _prevLatitude != Double.MIN_VALUE
               && _prevLongitude != Double.MIN_VALUE) {

            // get distance from lat/lon when it's not set
            _distanceAbsolute += MtMath.distanceVincenty(_prevLatitude, _prevLongitude, latitude, longitude);
         }

         if (latitude != Double.MIN_VALUE && longitude != Double.MIN_VALUE) {

            //if it's the first time that we see lat/lon data,
            //we update the tour start time with the correct time zone
            if (!_currentActivity.hasGpsData) {

               final int timeZoneIndex = TimeTools.getTimeZoneIndex(latitude, longitude);
               final ZoneId zoneIdFromLatLon = ZoneId.of(TimeTools.getTimeZone_ByIndex(timeZoneIndex).zoneId);
               _currentActivity.tourStartTime = _currentActivity.tourStartTime.withZoneSameInstant(zoneIdFromLatLon);
               _currentActivity.tourStartTimeMills = _currentActivity.tourStartTime.toInstant().toEpochMilli();
            }

            _prevLatitude = latitude;
            _prevLongitude = longitude;
            _currentActivity.hasGpsData = true;
         }

         // relative time in seconds
         final long tmValue = Util.parseLong(attributes, ATTRIB_PT_TM);
         if (tmValue != Long.MIN_VALUE) {

            timeSlice.absoluteTime = _currentActivity.tourStartTimeMills + (tmValue * 1000);
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
   "15.5003">Min./Max.: 55.6 °F/63.9 °F; Pressure: 1004.5 mbar; Humidity: 74.9%; Dew point: 49.0 °F; Wind Speed: 1.9 mph; Precipitation: 0.0mm</Weather>
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

      }
   }

}
