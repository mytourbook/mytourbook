/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.common.UI;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.CustomTrackDefinition;
import net.tourbook.data.CustomTrackValue;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.InvalidDeviceSAXException;
import net.tourbook.device.Messages;
import net.tourbook.preferences.TourTypeColorDefinition;
import net.tourbook.ui.tourChart.ChartLabel;

import org.eclipse.osgi.util.NLS;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FitLogSAXHandler extends DefaultHandler {

   private class Activity {

      private ArrayList<TimeData>              timeSlices             = new ArrayList<>();
      private ArrayList<Lap>                   laps                   = new ArrayList<>();
      private ArrayList<Pause>                 pauses                 = new ArrayList<>();
      private ArrayList<Equipment>             equipmentNames         = new ArrayList<>();
      private ArrayList<CustomST3TrackDefinition> customTrackDefinitions = new ArrayList<>();

      private ZonedDateTime                    tourStartTime;
      private long                             tourStartTimeMills     = Long.MIN_VALUE;
//      private DateTime         trackTourDateTime;
//      private long            trackTourStartTime   = Long.MIN_VALUE;

      private String  location;
      private String  name;
      private String  notes;
      private String  categoryName;

      private int     calories;
      private int     duration;
      private int     distance;

      private int     elevationUp;
      private int     elevationDown;

      private int     avgPulse;
      private int     maxPulse;

      private float   avgPower;
      private float   maxPower;
      private String  srcPower             = ""; //$NON-NLS-1$

      private int     timeZoneUtcOffset;
      private boolean hasTimeZoneUtcOffset = false;
      private boolean hasStartTime         = false;

      private boolean hasGpsData           = false;

      private int     avgCadence;
//      private int               maxCadence;      is not yet supported

      private String                        weatherText;
      private String                        weatherConditions;
      private float                         weatherTemperature       = Float.MIN_VALUE;
      private float                         weatherTemperatureFeel   = Float.MIN_VALUE;
      private int                           weatherWindSpeed         = Integer.MIN_VALUE;
      private float                         weatherWindDirection     = Float.MIN_VALUE;
      private float                         weatherPressure          = Float.MIN_VALUE;
      private float                         weatherHumidity          = Float.MIN_VALUE;
      private float                         weatherPrecipitation     = Float.MIN_VALUE;

      private LinkedHashMap<String, String> customDataFields         = new LinkedHashMap<>();
      private float                         avgPowerBalance          = Float.MIN_VALUE;
      private float                         avgPowerLeftPedalSmooth  = Float.MIN_VALUE;
      private float                         avgPowerRightPedalSmooth = Float.MIN_VALUE;
      private float                         avgPowerLeftTorqueEff    = Float.MIN_VALUE;
      private float                         avgPowerRightTorqueEff   = Float.MIN_VALUE;
      private float                         powerIntensityFactor     = Float.MIN_VALUE;
      private float                         powerNormalized          = Float.MIN_VALUE;
      private float                         powerTSS                 = Float.MIN_VALUE;      //TrainingStress Score
   }

   private class CustomST3TrackDefinition {
      //Custom Track Definition
      private String Name;
      private String Id;
      private String RefId;
      private String Unit;

      public String getId() {
         return Id;
      }

      public String getName() {
         return Name;
      }

      public String getRefId() {
         return RefId;
      }

      public String getUnit() {
         return Unit;
      }
      public void setId(final String id) {
         Id = id;
      }
      public void setName(final String name) {
         Name = name;
      }
      public void setRefId(final String refId) {
         RefId = refId;
      }
      public void setUnit(final String unit) {
         Unit = unit;
      }
   }

   static public class Equipment {

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

         if (!StringUtils.isNullOrEmpty(DatePurchased)) {
            notes.append(UI.NEW_LINE + FitLogExSAXHandler.TAG_EQUIPMENT_DATE_PURCHASED + ": " + DatePurchased); //$NON-NLS-1$
         }
         if (!StringUtils.isNullOrEmpty(ExpectedLifeKilometers)) {
            notes.append(UI.NEW_LINE + FitLogExSAXHandler.TAG_EQUIPMENT_EXPECTED_LIFE_KILOMETERS + ": " + ExpectedLifeKilometers); //$NON-NLS-1$
         }
         if (!StringUtils.isNullOrEmpty(InUse)) {
            notes.append(UI.NEW_LINE + FitLogExSAXHandler.TAG_EQUIPMENT_IN_USE + ": " + InUse); //$NON-NLS-1$
         }
         if (!StringUtils.isNullOrEmpty(PurchaseLocation)) {
            notes.append(UI.NEW_LINE + FitLogExSAXHandler.TAG_EQUIPMENT_PURCHASE_LOCATION + ": " + PurchaseLocation); //$NON-NLS-1$
         }
         if (!StringUtils.isNullOrEmpty(PurchasePrice)) {
            notes.append(UI.NEW_LINE + FitLogExSAXHandler.TAG_EQUIPMENT_PURCHASE_PRICE + ": " + PurchasePrice); //$NON-NLS-1$
         }
         if (!StringUtils.isNullOrEmpty(Type)) {
            notes.append(UI.NEW_LINE + FitLogExSAXHandler.TAG_EQUIPMENT_TYPE + ": " + Type); //$NON-NLS-1$
         }
         if (!StringUtils.isNullOrEmpty(WeightKilograms) && !WeightKilograms.equals("0.000")) { //$NON-NLS-1$
            notes.append(UI.NEW_LINE + FitLogExSAXHandler.TAG_EQUIPMENT_WEIGHT_KILOGRAMS + ": " + WeightKilograms); //$NON-NLS-1$
         }
         if (!StringUtils.isNullOrEmpty(Notes)) {
            notes.append(UI.NEW_LINE + FitLogExSAXHandler.TAG_EQUIPMENT_NOTES + ": " + Notes); //$NON-NLS-1$
         }

         return notes.toString();
      }

      public String getName() {

         if (!StringUtils.isNullOrEmpty(Name)) {
            return Name;
         }

         final StringBuilder name = new StringBuilder();
         if (!StringUtils.isNullOrEmpty(Brand)) {
            name.append(Brand);
         }
         if (!StringUtils.isNullOrEmpty(Model)) {
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
   private static final String                  TAG_ACTIVITY                = "Activity";            //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CADENCE        = "Cadence";             //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CALORIES       = "Calories";            //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_CATEGORY       = "Category";            //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_DURATION       = "Duration";            //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_DISTANCE       = "Distance";            //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_ELEVATION      = "Elevation";           //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_EQUIPMENT_ITEM = "EquipmentItem";       //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_HEART_RATE     = "HeartRate";           //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_LOCATION       = "Location";            //$NON-NLS-1$

   private static final String TAG_ACTIVITY_CUSTOMTRACKS           = "CustomTracks";               //$NON-NLS-1$
   private static final String TAG_ACTIVITY_CUSTOMTRACK            = "CustomTrack";                //$NON-NLS-1$

   private static final String                  TAG_ACTIVITY_NAME           = "Name";                //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_NOTES          = "Notes";               //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_POWER          = "Power";               //$NON-NLS-1$
   private static final String                  TAG_ACTIVITY_WEATHER        = "Weather";             //$NON-NLS-1$
   private static final String                  ATTRIB_DURATION_SECONDS     = "DurationSeconds";     //$NON-NLS-1$
   static final String                          ATTRIB_EQUIPMENT_ID         = "Id";                  //$NON-NLS-1$
   private static final String                  ATTRIB_NAME                 = "Name";                //$NON-NLS-1$
   private static final String ATTRIB_ID                           = "Id"; //$NON-NLS-1$
   private static final String ATTRIB_REFID                        = "RefId"; //$NON-NLS-1$
   private static final String ATTRIB_START_TIME                   = "StartTime"; //$NON-NLS-1$
   private static final String ATTRIB_UNIT                         = "Unit"; //$NON-NLS-1$
   private static final String ATTRIB_SOURCE                       = "Source"; //$NON-NLS-1$
   private static final String ATTRIB_AVERAGE_LP_WATTS             = "AvgLeftRightBalance"; //$NON-NLS-1$
   private static final String ATTRIB_END_TIME                     = "EndTime"; //$NON-NLS-1$
   private static final String                  ATTRIB_TOTAL_SECONDS        = "TotalSeconds";        //$NON-NLS-1$
   private static final String                  ATTRIB_TOTAL_METERS         = "TotalMeters";         //$NON-NLS-1$
   private static final String                  ATTRIB_TOTAL_CAL            = "TotalCal";            //$NON-NLS-1$
   private static final String                  ATTRIB_ASCEND_METERS        = "AscendMeters";        //$NON-NLS-1$
   private static final String                  ATTRIB_DESCEND_METERS       = "DescendMeters";       //$NON-NLS-1$
   private static final String                  ATTRIB_AVERAGE_BPM          = "AverageBPM";          //$NON-NLS-1$
   private static final String                  ATTRIB_MAXIMUM_BPM          = "MaximumBPM";          //$NON-NLS-1$
   private static final String                  ATTRIB_AVERAGE_WATTS        = "AverageWatts";        //$NON-NLS-1$
   private static final String                  ATTRIB_MAXIMUM_WATTS        = "MaximumWatts";        //$NON-NLS-1$
   private static final String                  ATTRIB_AVERAGE_RPM          = "AverageRPM";          //$NON-NLS-1$
   private static final String ATTRIB_WEATHER_TEMP                 = "Temp";                       //$NON-NLS-1$
   private static final String ATTRIB_WEATHER_TEMPFEEL             = "TempFeel";                   //$NON-NLS-1$
   private static final String ATTRIB_WEATHER_HUMIDITYPERCENT = "HumidityPercent"; //$NON-NLS-1$
   private static final String ATTRIB_WEATHER_WINDDIRECTIONDEGREES = "WindDirectionDegrees"; //$NON-NLS-1$
   private static final String ATTRIB_WEATHER_WINDSPEEDKH          = "WindSpeedKilometersPerHour"; //$NON-NLS-1$
   private static final String ATTRIB_WEATHER_PRESSUREMB           = "Pressure_mb";                //$NON-NLS-1$
   private static final String ATTRIB_WEATHER_PRECIPITATIONMM      = "Precipitation_mm";           //$NON-NLS-1$
   private static final String                  ATTRIB_WEATHER_CONDITIONS   = "Conditions";          //$NON-NLS-1$
   //
   private static final String                  TAG_TRACK                   = "Track";               //$NON-NLS-1$
   private static final String                  TAG_TRACK_PT                = "pt";                  //$NON-NLS-1$
   private static final String                  ATTRIB_PT_CADENCE           = "cadence";             //$NON-NLS-1$
   private static final String                  ATTRIB_PT_DIST              = "dist";                //$NON-NLS-1$
   private static final String                  ATTRIB_PT_ELE               = "ele";                 //$NON-NLS-1$
   private static final String                  ATTRIB_PT_HR                = "hr";                  //$NON-NLS-1$
   private static final String                  ATTRIB_PT_LAT               = "lat";                 //$NON-NLS-1$
   private static final String                  ATTRIB_PT_LON               = "lon";                 //$NON-NLS-1$
   private static final String                  ATTRIB_PT_POWER             = "power";               //$NON-NLS-1$
   private static final String                  ATTRIB_PT_TEMP              = "temp";                //$NON-NLS-1$
   private static final String                  ATTRIB_PT_TM                = "tm";                  //$NON-NLS-1$
   private static final String ATTRIB_PT_GCT                       = "gct";                        //$NON-NLS-1$
   private static final String ATTRIB_PT_LP                        = "lp";                         //$NON-NLS-1$
   private static final String ATTRIB_PT_VO                        = "vo";                         //$NON-NLS-1$
   private static final String ATTRIB_PT_SMO2                      = "smo2";                       //$NON-NLS-1$
   private static final String ATTRIB_PT_THB                       = "thb";                        //$NON-NLS-1$

   //
   private static final String                  TAG_LAPS                    = "Laps";                //$NON-NLS-1$
   private static final String                  TAG_LAP                     = "Lap";                 //$NON-NLS-1$
   private static final String                  TAG_TRACK_CLOCK             = "TrackClock";          //$NON-NLS-1$
   private static final String                  TAG_PAUSE                   = "Pause";               //$NON-NLS-1$
   private static final String                  SUB_ATTRIB_WIND_SPEED       = "Wind Speed:";         //$NON-NLS-1$
   private static final HashMap<String, String> _weatherId                  = new HashMap<>();

   //
   private String                               _importFilePath;
   private FitLogDeviceDataReader               _device;
   private HashMap<Long, TourData>              _alreadyImportedTours;

   private HashMap<Long, TourData>              _newlyImportedTours;
   private Activity                             _currentActivity;
   private double                               _prevLatitude;

   private double                               _prevLongitude;
   private double                               _distanceAbsolute;
   private boolean                              _isImported                 = false;

   private boolean                              _isNewTag                   = false;
   private boolean                              _isNewTourType              = false;
   private boolean                              _isInActivity;
   private boolean                              _isInTrack;
   private LinkedHashMap<String, Integer>       _customDataFieldDefinitions;
   private ArrayList<Equipment>                 _equipments;
   private boolean                              _isInCustomDataFields;
   private boolean                        _isInCustomTrackDefinition;
   private boolean                              _isInHasStartTime;

   private boolean                              _isInName;
   private boolean                              _isInNotes;
   private boolean                              _isInTimeZoneUtcOffset;

   private boolean                              _isInWeather;
   private StringBuilder                        _characters                 = new StringBuilder(100);

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

   public FitLogSAXHandler(final FitLogDeviceDataReader device,
                           final String importFilePath,
                           final HashMap<Long, TourData> alreadyImportedTours,
                           final HashMap<Long, TourData> newlyImportedTours,
                           final boolean isFitLogExFile) {

      _device = device;
      _importFilePath = importFilePath;
      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;

      if (isFitLogExFile) {
         // We parse the custom field definitions and equipments
         // separately as they can be anywhere in the file

         final FitLogExSAXHandler saxHandler = new FitLogExSAXHandler();

         try {

            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

            parser.parse("file:" + importFilePath, saxHandler);//$NON-NLS-1$

         } catch (final InvalidDeviceSAXException e) {
            StatusUtil.log(e);
         } catch (final Exception e) {
            StatusUtil.log("Error parsing file: " + importFilePath, e); //$NON-NLS-1$
         }

         _customDataFieldDefinitions = saxHandler.getCustomDataFieldDefinitions();
         _equipments = saxHandler.getEquipments();
         saveEquipmentsAsTags();
      }

      _allTourTypes = TourDatabase.getAllTourTypes();
   }

   @Override
   public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

      if (_isInTimeZoneUtcOffset || _isInHasStartTime || _isInName || _isInNotes || _isInWeather) {

         _characters.append(chars, startIndex, length);
      }
   }

   @Override
   public void endElement(final String uri, final String localName, final String name) throws SAXException {

      /*
       * get values
       */
      if (_isInTimeZoneUtcOffset || _isInHasStartTime || _isInName || _isInNotes || _isInWeather) {
         parseActivity_02_End(name);
      }

      /*
       * set state
       */
      if (name.equals(TAG_TRACK)) {

         _isInTrack = false;

      } else if (name.equals(TAG_LAPS)) {

         _isInLaps = false;

      } else if (name.equals(TAG_ACTIVITY_CUSTOMTRACKS)) {

         _isInCustomTrackDefinition = false;

      } else if (name.equals(TAG_TRACK_CLOCK)) {

         _isInPauses = false;

      } else if (name.equals(FitLogExSAXHandler.TAG_ACTIVITY_CUSTOM_DATA_FIELDS)) {

         _isInCustomDataFields = false;

      } else if (name.equals(TAG_ACTIVITY)) {

         // activity/tour ends
         _isInActivity = false;

         finalizeTour();

      }
   }

   private void finalizeTour() {

      boolean isComputeMovingTime = true;

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

      final ZonedDateTime tourStartTime_FromImport = _currentActivity.tourStartTime;

      tourData.setTourStartTime(tourStartTime_FromImport);

      tourData.setTourTitle(_currentActivity.name);
      tourData.setTourDescription(_currentActivity.notes);
      tourData.setTourStartPlace(_currentActivity.location);

      tourData.setCalories(_currentActivity.calories);
      tourData.setPower_DataSource(_currentActivity.srcPower);

      /*
       * weather
       */
      tourData.setWeather(_currentActivity.weatherText);
      tourData.setWeatherClouds(_weatherId.get(_currentActivity.weatherConditions));

      final float weatherTemperature = _currentActivity.weatherTemperature;
      if (weatherTemperature != Float.MIN_VALUE) {
         tourData.setAvgTemperature(weatherTemperature);
      }

      if (_currentActivity.weatherTemperatureFeel != Float.MIN_VALUE) {
         tourData.setWeather_Temperature_WindChill(_currentActivity.weatherTemperatureFeel);
      }
      if (_currentActivity.weatherHumidity != Float.MIN_VALUE) {
         tourData.setWeather_Humidity((short) _currentActivity.weatherHumidity);
      }

      if (_currentActivity.weatherWindSpeed != Integer.MIN_VALUE) {
         tourData.setWeatherWindSpeed(_currentActivity.weatherWindSpeed);
      }
      if (_currentActivity.weatherWindDirection != Float.MIN_VALUE) {
         tourData.setWeatherWindDir((int) _currentActivity.weatherWindDirection);
      }
      if (_currentActivity.weatherPressure != Float.MIN_VALUE) {
         tourData.setWeather_Pressure(_currentActivity.weatherPressure);
      }
      if (_currentActivity.weatherPrecipitation != Float.MIN_VALUE) {
         tourData.setWeather_Precipitation(_currentActivity.weatherPrecipitation);
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
         if (_currentActivity.customDataFields.containsKey("NormalizedPower [W]")) { //$NON-NLS-1$
            try {
               final float val = Float.parseFloat(_currentActivity.customDataFields.get("NormalizedPower [W]")); //$NON-NLS-1$
               tourData.setPower_Normalized((int) val);
               _currentActivity.powerNormalized = val;
            } catch (final Exception exc) {
               StatusUtil.log("NormalizedPower [W]" + ": FitlogEx parse error", exc); //$NON-NLS-1$ //$NON-NLS-2$
            }
         }
         if (_currentActivity.customDataFields.containsKey("TSS (Training Stress Score)")) { //$NON-NLS-1$
            try {
               final float val = Float.parseFloat(_currentActivity.customDataFields.get("TSS (Training Stress Score)")); //$NON-NLS-1$
               tourData.setPower_TrainingStressScore(val);
               _currentActivity.powerTSS = val;
            } catch (final Exception exc) {
               StatusUtil.log("TSS (Training Stress Score)" + ": FitlogEx parse error", exc); //$NON-NLS-1$ //$NON-NLS-2$
            }
         }
         if (_currentActivity.customDataFields.containsKey("IF (Intensity Factor)")) { //$NON-NLS-1$
            try {
               final float val = Float.parseFloat(_currentActivity.customDataFields.get("IF (Intensity Factor)")); //$NON-NLS-1$
               tourData.setPower_IntensityFactor(val);
               _currentActivity.powerIntensityFactor = val;
            } catch (final Exception exc) {
               StatusUtil.log("IF (Intensity Factor)" + ": FitlogEx parse error", exc); //$NON-NLS-1$ //$NON-NLS-2$
            }
         }
         if (_currentActivity.customDataFields.containsKey("Left Torque Effectiveness Avg. [%]")) { //$NON-NLS-1$
            try {
               final float val = Float.parseFloat(_currentActivity.customDataFields.get("Left Torque Effectiveness Avg. [%]")); //$NON-NLS-1$
               tourData.setPower_AvgLeftTorqueEffectiveness(val);
               _currentActivity.avgPowerLeftTorqueEff = val;
            } catch (final Exception exc) {
               StatusUtil.log("Left Torque Effectiveness Avg. [%]" + ": FitlogEx parse error", exc); //$NON-NLS-1$ //$NON-NLS-2$
            }
         }
         if (_currentActivity.customDataFields.containsKey("Right Torque Effectiveness Avg. [%]")) { //$NON-NLS-1$
            try {
               final float val = Float.parseFloat(_currentActivity.customDataFields.get("Right Torque Effectiveness Avg. [%]")); //$NON-NLS-1$
               tourData.setPower_AvgRightTorqueEffectiveness(val);
               _currentActivity.avgPowerRightTorqueEff = val;
            } catch (final Exception exc) {
               StatusUtil.log("Right Torque Effectiveness Avg. [%]" + ": FitlogEx parse error", exc); //$NON-NLS-1$ //$NON-NLS-2$
            }
         }
         if (_currentActivity.customDataFields.containsKey("Left Pedal Smoothness Avg. [%]")) { //$NON-NLS-1$
            try {
               final float val = Float.parseFloat(_currentActivity.customDataFields.get("Left Pedal Smoothness Avg. [%]")); //$NON-NLS-1$
               tourData.setPower_AvgLeftPedalSmoothness(val);
               _currentActivity.avgPowerLeftPedalSmooth = val;
            } catch (final Exception exc) {
               StatusUtil.log("Left Pedal Smoothness Avg. [%]" + ": FitlogEx parse error", exc); //$NON-NLS-1$ //$NON-NLS-2$
            }
         }
         if (_currentActivity.customDataFields.containsKey("Right Pedal Smoothness Avg. [%]")) { //$NON-NLS-1$
            try {
               final float val = Float.parseFloat(_currentActivity.customDataFields.get("Right Pedal Smoothness Avg. [%]")); //$NON-NLS-1$
               tourData.setPower_AvgRightPedalSmoothness(val);
               _currentActivity.avgPowerRightPedalSmooth = val;
            } catch (final Exception exc) {
               StatusUtil.log("Right Pedal Smoothness Avg. [%]" + ": FitlogEx parse error", exc); //$NON-NLS-1$ //$NON-NLS-2$
            }
         }
      }


      if (_currentActivity.avgPowerBalance != Float.MIN_VALUE) {
         tourData.setPower_PedalLeftRightBalance((int) _currentActivity.avgPowerBalance);
      }
      if (_currentActivity.powerNormalized != Float.MIN_VALUE && _currentActivity.powerIntensityFactor != Float.MIN_VALUE) {
         tourData.setPower_FTP((int) (_currentActivity.powerNormalized / _currentActivity.powerIntensityFactor));
      }

      tourData.setImportFilePath(_importFilePath);

      tourData.setDeviceTimeInterval((short) -1);

      if (_currentActivity.timeSlices.size() == 0) {

         // tour do not contain a track

         tourData.setTourDistance(_currentActivity.distance);

         tourData.setTourDeviceTime_Elapsed(_currentActivity.duration);
         tourData.setTourComputedTime_Moving(_currentActivity.duration);
         isComputeMovingTime = false;

         tourData.setTourAltUp(_currentActivity.elevationUp);
         tourData.setTourAltDown(_currentActivity.elevationDown);

         // We set the tour as manual since it was a manual tour created in SportTracks in the first place.
         tourData.setDeviceId(TourData.DEVICE_ID_FOR_MANUAL_TOUR);

      } else {

         // create 'normal' tour

         tourData.createTimeSeries(_currentActivity.timeSlices, false);

         // If the activity doesn't have GPS data but contains a distance value,
         // we set the distance manually
         if (!_currentActivity.hasGpsData &&
               _currentActivity.distance > 0) {
            tourData.setTourDistance(_currentActivity.distance);
         }

         /*
          * The tour start time timezone is set from lat/lon in createTimeSeries()
          */
         final ZonedDateTime tourStartTime_FromLatLon = tourData.getTourStartTime();

         if (tourStartTime_FromImport.equals(tourStartTime_FromLatLon) == false) {

            // time zone is different -> fix tour start components with adjusted time zone
            tourData.setTourStartTime_YYMMDD(tourStartTime_FromLatLon);
         }

         tourData.setDeviceId(_device.deviceId);
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

      if (_currentActivity.pauses.size() > 0) {

         final ArrayList<Long> _pausedTime_Start = new ArrayList<>();
         final ArrayList<Long> _pausedTime_End = new ArrayList<>();

         for (final Pause element : _currentActivity.pauses) {
            _pausedTime_Start.add(element.startTime);
            _pausedTime_End.add(element.endTime);
         }

         tourData.finalizeTour_TimerPauses(_pausedTime_Start, _pausedTime_End);
      }

      // No need to set the timezone Id if the activity has GPS coordinates (as it was already done
      // when the time series were created) or if the activity has no time zone UTC offset or no start time.
      if ((tourData.latitudeSerie == null || tourData.latitudeSerie.length == 0) &&
            _currentActivity.hasTimeZoneUtcOffset && _currentActivity.hasStartTime) {

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

      tourData.setDeviceName(_device.visibleName);

      // after all data are added, the tour id can be created because it depends on the tour distance
      final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_SPORT_TRACKS_FITLOG);
      final Long tourId = tourData.createTourId(uniqueId);

      // check if the tour is already imported
      if (_alreadyImportedTours.containsKey(tourId) == false) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, tourData);

         // create additional data
         if (isComputeMovingTime) {
            tourData.computeTourMovingTime();
         }

         tourData.setTourDeviceTime_Recorded(tourData.getTourDeviceTime_Elapsed() - tourData.getTourDeviceTime_Paused());
         tourData.computeAltitudeUpDown();
         tourData.computeComputedValues();

         finalizeTour_10_SetTourType(tourData);
         finalizeTour_20_SetTags(tourData);
         finalizeTour_30_CreateMarkers(tourData);
      }

      for (final CustomST3TrackDefinition element : _currentActivity.customTrackDefinitions) {
         final String idS = element.getId();
         final String nameS = element.getName();
         final String unitS = element.getUnit();
         if (nameS.compareTo("Stride length (recorded)") == 0) {} else if (nameS.compareTo("Ground Contact Time Balance") == 0) {} else if (nameS //$NON-NLS-1$ //$NON-NLS-2$
               .compareTo("Vertical Ratio") == 0) {} else { //$NON-NLS-1$
            final CustomTrackDefinition item = new CustomTrackDefinition();
            item.setId(idS);
            item.setName(nameS);
            item.setUnit(unitS);
            tourData.customTracksDefinition.put(idS, item);
         }
      }

      // cleanup
      _currentActivity.timeSlices.clear();
      _currentActivity.laps.clear();
      _currentActivity.equipmentNames.clear();
      _currentActivity.customTrackDefinitions.clear();

      _isImported = true;
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

      final ArrayList<Equipment> equipmentNames = _currentActivity.equipmentNames;
      if (equipmentNames.size() == 0) {
         return;
      }

      boolean isNewTag = false;

      HashMap<Long, TourTag> tourTagMap = TourDatabase.getAllTourTags();
      TourTag[] allTourTags = tourTagMap.values().toArray(new TourTag[tourTagMap.size()]);

      boolean searchTagById = false;
      // If we are in a FitLogEx file, then we have parsed equipments
      // and we need to map tour tags using each equipment's GUID.
      if (_equipments != null && _equipments.size() > 0) {
         searchTagById = true;
      }

      final Set<TourTag> tourTags = new HashSet<>();

      try {

         for (final Equipment tag : equipmentNames) {

            boolean isTagAvailable = false;

            for (final TourTag tourTag : allTourTags) {
               if ((searchTagById && tourTag.getNotes().contains(tag.Id)) ||
                     (!searchTagById && tourTag.getTagName().equals(tag.getName()))) {
                  isTagAvailable = true;

                  tourTags.add(tourTag);
                  break;
               }
            }

            if (isTagAvailable == false) {

               // create a new tag

               final TourTag tourTag = new TourTag(tag.getName());
               // There is no notes to import as we are here in a FitLog file as
               // FitLogEx files would not have unavailable tags since, at this
               // point, they would be already imported.
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
         //final String formattedNumber = String.format(format, (double) Math.round(Double.valueOf(customDataFieldValue)));
         final String formattedNumber = String.format(format, Double.valueOf(customDataFieldValue).floatValue());

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
      if (!StringUtils.isNullOrEmpty(startTime)) {

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

         final Equipment newEquipment = new Equipment();
         newEquipment.Name = attributes.getValue(ATTRIB_NAME);
         newEquipment.Id = attributes.getValue(ATTRIB_EQUIPMENT_ID);

         _currentActivity.equipmentNames.add(newEquipment);

      } else if (name.equals(TAG_ACTIVITY_CALORIES)) {

         //      <xs:element name="Calories">
         //         <xs:complexType>
         //            <xs:attribute name="TotalCal" type="xs:decimal" use="optional"/>
         //         </xs:complexType>
         //      </xs:element>

         // Converting from Calories to calories
         _currentActivity.calories = Math.round(Util.parseFloat0(attributes, ATTRIB_TOTAL_CAL) * 1000f);

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
         _currentActivity.avgPowerBalance = Util.parseFloat0(attributes, ATTRIB_AVERAGE_LP_WATTS);
         _currentActivity.srcPower = attributes.getValue(ATTRIB_SOURCE);

      } else if (name.equals(FitLogExSAXHandler.TAG_ACTIVITY_TIMEZONE_UTC_OFFSET)) {

         _isInTimeZoneUtcOffset = true;

      } else if (name.equals(FitLogExSAXHandler.TAG_ACTIVITY_HAS_START_TIME)) {

         _isInHasStartTime = true;

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
         _currentActivity.weatherTemperatureFeel = Util.parseFloat(attributes, ATTRIB_WEATHER_TEMPFEEL);
         _currentActivity.weatherHumidity = Util.parseFloat(attributes, ATTRIB_WEATHER_HUMIDITYPERCENT);
         _currentActivity.weatherPrecipitation = Util.parseFloat(attributes, ATTRIB_WEATHER_PRECIPITATIONMM);
         _currentActivity.weatherPressure = Util.parseFloat(attributes, ATTRIB_WEATHER_PRESSUREMB);
         _currentActivity.weatherWindSpeed = (int) Util.parseFloat(attributes, ATTRIB_WEATHER_WINDSPEEDKH);
         _currentActivity.weatherWindDirection = Util.parseFloat(attributes, ATTRIB_WEATHER_WINDDIRECTIONDEGREES);

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
         //_currentActivity.weatherWindSpeed = parseWindSpeed(_characters.toString());

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

      } else if (_isInHasStartTime) {

         _isInHasStartTime = false;

         _currentActivity.hasStartTime = Boolean.parseBoolean(_characters.toString());

         if (_currentActivity.hasStartTime == false) {
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

      if (name.equals(FitLogExSAXHandler.TAG_ACTIVITY_CUSTOM_DATA_FIELD)) {

         final String customFieldName = attributes.getValue(FitLogExSAXHandler.ATTRIB_CUSTOM_DATA_FIELD_NAME);
         final String customFieldValue = attributes.getValue(FitLogExSAXHandler.ATTRIB_CUSTOM_DATA_FIELD_VALUE);

         final boolean isCustomDataFieldValid = !StringUtils.isNullOrEmpty(customFieldName) &&
               !StringUtils.isNullOrEmpty(customFieldValue);

         if (isCustomDataFieldValid) {
            formatCustomDataFieldValue(_currentActivity, customFieldName, customFieldValue);
         }

      }
   }

   private void parseCustomTrackDefinitions(final String name, final Attributes attributes) {

      if (name.equals(TAG_ACTIVITY_CUSTOMTRACK)) {

         final String nameT = attributes.getValue(ATTRIB_NAME);
         final String idT = attributes.getValue(ATTRIB_ID);
         final String refidT = attributes.getValue(ATTRIB_REFID);
         final String unitT = attributes.getValue(ATTRIB_UNIT);

         if (!StringUtils.isNullOrEmpty(nameT) && !StringUtils.isNullOrEmpty(idT)) {

            final CustomST3TrackDefinition custT = new CustomST3TrackDefinition();
            custT.setId(idT);
            custT.setName(nameT);
            custT.setRefId(idT);
            custT.setUnit(unitT);

            _currentActivity.customTrackDefinitions.add(custT);
         }
      }
   }

   private void parseLaps(final String name, final Attributes attributes) {

      if (name.equals(TAG_LAP)) {

         final String startTime = attributes.getValue(ATTRIB_START_TIME);
         final String durationSeconds = attributes.getValue(ATTRIB_DURATION_SECONDS);

         if (!StringUtils.isNullOrEmpty(startTime)) {

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

         if (!StringUtils.isNullOrEmpty(startTime)) {

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

         // relative time in seconds
         final long tmValue = Util.parseLong(attributes, ATTRIB_PT_TM);
         if (tmValue != Long.MIN_VALUE) {
            timeSlice.absoluteTime = _currentActivity.tourStartTimeMills + (tmValue * 1000);
         }

         final double tpDistance = Util.parseDouble(attributes, ATTRIB_PT_DIST);
         final double latitude = Util.parseDouble(attributes, ATTRIB_PT_LAT);
         final double longitude = Util.parseDouble(attributes, ATTRIB_PT_LON);
         final double runD_vo = Util.parseDouble(attributes, ATTRIB_PT_VO);
         final double runD_gct = Util.parseDouble(attributes, ATTRIB_PT_GCT);

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
            _currentActivity.hasGpsData = true;
         }

         if (runD_vo != Double.MIN_VALUE) {
            timeSlice.runDyn_VerticalOscillation = (short) (runD_vo * TourData.RUN_DYN_DATA_MULTIPLIER);
         }
         if (runD_vo != Double.MIN_VALUE) {
            timeSlice.runDyn_StanceTime = (short) runD_gct;
         }
         timeSlice.powerDataSource = _currentActivity.srcPower;
         //timeSlice.customTracks = new CustomTrackValue[_currentActivity.customTrackDefinitions.size()];
         final ArrayList<CustomTrackValue> customTracksT = new ArrayList<>();
         for (final CustomST3TrackDefinition element : _currentActivity.customTrackDefinitions) {
            final String idS = element.getId();
            final String nameS = element.getName();
            if (nameS.compareTo("Stride length (recorded)") == 0) { //$NON-NLS-1$
               final float value = Util.parseFloat(attributes, idS);
               if (value != Float.MIN_VALUE) {
                  timeSlice.runDyn_StepLength = (short) (value * 1000.0f);//meter to mm
               }
            } else if (nameS.compareTo("Ground Contact Time Balance") == 0) { //$NON-NLS-1$
               final float value = Util.parseFloat(attributes, idS);
               if (value != Float.MIN_VALUE) {
                  timeSlice.runDyn_StanceTimeBalance = (short) (value * TourData.RUN_DYN_DATA_MULTIPLIER);
               }
            } else if (nameS.compareTo("Vertical Ratio") == 0) { //$NON-NLS-1$
               final float value = Util.parseFloat(attributes, idS);
               if (value != Float.MIN_VALUE) {
                  timeSlice.runDyn_VerticalRatio = (short) (value * TourData.RUN_DYN_DATA_MULTIPLIER);
               }
            } else {
               final CustomTrackValue item = new CustomTrackValue();
               item.Id = idS;
               item.Value = Util.parseFloat(attributes, idS);
               customTracksT.add(item);
            }
         }
         timeSlice.customTracks = customTracksT.toArray(new CustomTrackValue[customTracksT.size()]);

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

//   private void parseTrack(final Attributes attributes) {
//
//      final String startTime = attributes.getValue(ATTRIB_START_TIME);
//
//      if (startTime != null) {
//         _currentActivity.trackTourDateTime = _dtParser.parseDateTime(startTime);
//         _currentActivity.trackTourStartTime = _currentActivity.trackTourDateTime.getMillis();
//      }
//   }

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

   /**
    * We save the <Equipment> elements in order to be able to create them if they don't
    * already exist in MTB
    *
    * @param importFilePath
    *           The file path of the FitLog or FitLogEx file
    */
   private void saveEquipmentsAsTags() {

      if (_equipments.size() == 0) {
         return;
      }

      final HashMap<Long, TourTag> tourTagMap = TourDatabase.getAllTourTags();
      final TourTag[] allTourTags = tourTagMap.values().toArray(new TourTag[tourTagMap.size()]);

      for (final Equipment equipment : _equipments) {

         boolean tagAlreadyExists = false;
         for (final TourTag tourTag : allTourTags) {
            if (tourTag.getNotes().contains(equipment.Id)) {

               // existing tag is found
               tagAlreadyExists = true;

               break;
            }
         }

         if (!tagAlreadyExists) {
            //We add the tag in the database if it doesn't already exist

            final TourTag tourTag = new TourTag(equipment.getName());
            tourTag.setNotes(equipment.generateNotes());
            tourTag.setRoot(true);

            // persist tag
            TourDatabase.saveEntity(
                  tourTag,
                  TourDatabase.ENTITY_IS_NOT_SAVED,
                  TourTag.class);
         }
      }

      TourDatabase.clearTourTags();
   }

   @Override
   public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
         throws SAXException {

      if (_isInActivity) {

         if (_isInTrack) {
            parseTrackPoints(name, attributes);
         } else if (_isInLaps) {
            parseLaps(name, attributes);
         } else if (_isInCustomTrackDefinition) {
            parseCustomTrackDefinitions(name, attributes);
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

      } else if (name.equals(TAG_ACTIVITY_CUSTOMTRACKS)) {

         _isInCustomTrackDefinition = true;

      } else if (name.equals(TAG_TRACK_CLOCK)) {

         _isInPauses = true;

      } else if (name.equals(TAG_ACTIVITY)) {

         /*
          * a new exercise/tour starts
          */

         _isInActivity = true;

         initTour(attributes);
      } else if (name.equals(FitLogExSAXHandler.TAG_ACTIVITY_CUSTOM_DATA_FIELDS)) {
         _isInCustomDataFields = true;

      }
   }

}
