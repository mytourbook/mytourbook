/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin.fit;

import com.garmin.fit.GarminProduct;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.SessionMesg;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.FileUtils;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.DeviceSensor;
import net.tourbook.data.DeviceSensorImport;
import net.tourbook.data.DeviceSensorValue;
import net.tourbook.data.DeviceSensorValueImport;
import net.tourbook.data.GearData;
import net.tourbook.data.GearDataType;
import net.tourbook.data.SwimData;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.garmin.fit.listeners.MesgListener_DeviceInfo;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.TourTypeWrapper;
import net.tourbook.tour.TourLogManager;
import net.tourbook.ui.tourChart.ChartLabelMarker;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Collects all data from a fit file
 */
public class FitData {

   private static final Integer                 DEFAULT_MESSAGE_INDEX     = Integer.valueOf(0);

   private IPreferenceStore                     _prefStore                = Activator.getDefault().getPreferenceStore();

   private String                               _fitImportTourTypeMode;
   private boolean                              _isFitImportTourType;
   private boolean                              _isIgnoreLastMarker;
   private boolean                              _isLogSensorValues;
   private boolean                              _isSetLastMarker;
   private boolean                              _isSetTourTitleFromFileName;
   private int                                  _lastMarkerTimeSlices;

   public boolean                               isComputeAveragePower;

   private FitDataReader                        _fitDataReader;
   private String                               _importFilePathName;

   private Map<Long, TourData>                  _alreadyImportedTours;
   private Map<Long, TourData>                  _newlyImportedTours;

   private TourData                             _tourData                 = new TourData();

   private String                               _deviceId;
   private String                               _manufacturer;
   private String                               _garminProduct;
   private String                               _softwareVersion;

   private String                               _sessionIndex;
   private ZonedDateTime                        _sessionStartTime;

   private String                               _profileName              = UI.EMPTY_STRING;
   private String                               _sessionSportProfileName  = UI.EMPTY_STRING;
   private String                               _sportName                = UI.EMPTY_STRING;
   private String                               _subSportName             = UI.EMPTY_STRING;

   private final List<TimeData>                 _allTimeData              = new ArrayList<>();
   private final List<Long>                     _pausedTime_Start         = new ArrayList<>();
   private final List<Long>                     _pausedTime_End           = new ArrayList<>();
   private final List<Long>                     _pausedTime_Data          = new ArrayList<>();

   private final List<Long>                     _allBatteryTime           = new ArrayList<>();
   private final List<Short>                    _allBatteryPercentage     = new ArrayList<>();
   private final List<GearData>                 _allGearData              = new ArrayList<>();
   private final List<SwimData>                 _allSwimData              = new ArrayList<>();
   private final List<TourMarker>               _allTourMarker            = new ArrayList<>();

   /**
    * All collected devices and sensor data from the import file, key is the device index.
    * <p>
    * Some sensor values, e.g. serial number are not always available in the first device info
    * message
    */
   private final Map<Short, DeviceSensorImport> _allImportedDeviceSensors = new HashMap<>();

   private TimeData                             _current_TimeData;
   private TimeData                             _lastAdded_TimeData;
   private TimeData                             _previous_TimeData;

   private TourMarker                           _current_TourMarker;
   private long                                 _timeDiffMS;

   private ImportState_Process                  _importState_Process;

   private MesgListener_DeviceInfo              _deviceInfoListener;

   private GearDataType                         _gearDataType;

   public FitData(final FitDataReader fitDataReader,
                  final String importFilePath,
                  final Map<Long, TourData> alreadyImportedTours,
                  final Map<Long, TourData> newlyImportedTours,
                  final ImportState_Process importState_Process) {

// SET_FORMATTING_OFF

      _fitDataReader          = fitDataReader;
      _importFilePathName     = importFilePath;
      _alreadyImportedTours   = alreadyImportedTours;
      _newlyImportedTours     = newlyImportedTours;
      _importState_Process    = importState_Process;

      _isIgnoreLastMarker           = _prefStore.getBoolean(IPreferences.FIT_IS_IGNORE_LAST_MARKER);
      _isSetLastMarker              = _isIgnoreLastMarker == false;
      _lastMarkerTimeSlices         = _prefStore.getInt(IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES);
      _isFitImportTourType          = _prefStore.getBoolean(IPreferences.FIT_IS_SET_TOURTYPE_DURING_IMPORT);
      _fitImportTourTypeMode        = _prefStore.getString(IPreferences.FIT_IMPORT_TOURTYPE_MODE);
      _isSetTourTitleFromFileName   = _prefStore.getBoolean(IPreferences.FIT_IS_SET_TOUR_TITLE_FROM_FILE_NAME);
      _isLogSensorValues            = _prefStore.getBoolean(IPreferences.FIT_IS_LOG_SENSOR_VALUES);

// SET_FORMATTING_ON
   }

   /**
    * Creates a tour type when it do not yet exist for the provided label
    *
    * @param tourData
    * @param parsedTourTypeLabel
    */
   private void applyTour_Type(final TourData tourData, final String parsedTourTypeLabel) {

      // ignore empty tour type label
      if (UI.EMPTY_STRING.equals(parsedTourTypeLabel)) {
         return;
      }

      final TourTypeWrapper tourTypeWrapper = RawDataManager.setTourType(tourData, parsedTourTypeLabel);

      if (tourTypeWrapper != null && tourTypeWrapper.isNewTourType) {
         _importState_Process.isCreated_NewTourType().set(true);
      }
   }

   public void finalizeTour() {

      // log device infos
      _deviceInfoListener.logDeviceData();

      // reset speed at first position
      if (_allTimeData.size() > 0) {
         _allTimeData.get(0).speed = Float.MIN_VALUE;
      }

// disabled, this is annoying
//    tourData.setTourTitle(getTourTitle());
//    tourData.setTourDescription(getTourDescription());

      _tourData.setImportFilePath(_importFilePathName);

      _tourData.setDeviceId(_deviceId);
      _tourData.setDeviceName(getDeviceName());
      _tourData.setDeviceFirmwareVersion(_softwareVersion);
      _tourData.setDeviceTimeInterval((short) -1);

      long recordStartTime;
      if (_allTimeData.size() > 0) {

         // this is the normal case

         recordStartTime = _allTimeData.get(0).absoluteTime;

      } else if (_sessionStartTime != null) {

         // fallback case 1

         recordStartTime = _sessionStartTime.toInstant().toEpochMilli();

         TourLogManager.subLog_INFO(String.format(
               "[FIT] %s - There are no time data, using session date/time %s", //$NON-NLS-1$
               _importFilePathName,
               TimeTools.getZonedDateTime(recordStartTime).format(TimeTools.Formatter_DateTime_S)));

      } else {

         // fallback case 2

         recordStartTime = TimeTools.now().toEpochSecond();

         TourLogManager.subLog_INFO(String.format(
               "[FIT] %s - There are no time data and there is no session date/time, using %s", //$NON-NLS-1$
               _importFilePathName,
               TimeTools.getZonedDateTime(recordStartTime).format(TimeTools.Formatter_DateTime_S)));
      }

      if (_sessionStartTime != null) {

         final long sessionStartTime = _sessionStartTime.toInstant().toEpochMilli();

         if (recordStartTime != sessionStartTime) {

// too much noise

//            final String message =
//                  "Import file %s has other session start time, sessionStartTime=%s recordStartTime=%s, Difference=%d sec";//$NON-NLS-1$
//
//            TourLogManager.subLog_Info(
//                  String.format(
//                        message,
//                        _importFilePathName,
//                        TimeTools.getZonedDateTime(sessionStartTime).format(TimeTools.Formatter_DateTime_M),
//                        TimeTools.getZonedDateTime(recordStartTime).format(TimeTools.Formatter_DateTime_M),
//                        (recordStartTime - sessionStartTime) / 1000));
         }
      }

      final ZonedDateTime zonedStartTime = TimeTools.getZonedDateTime(recordStartTime);

      _tourData.setTourStartTime(zonedStartTime);

      _tourData.createTimeSeries(_allTimeData, false, _importState_Process);

      _tourData.finalizeTour_TimerPauses(_pausedTime_Start, _pausedTime_End, _pausedTime_Data);

      //We set the recorded time again as the elapsed time might have changed (+- few seconds)
      //after the time series were created.
      _tourData.setTourDeviceTime_Recorded(_tourData.getTourDeviceTime_Elapsed() - _tourData.getTourDeviceTime_Paused());

      // after all data are added, the tour id can be created
      final String uniqueId = _fitDataReader.createUniqueId(_tourData, Util.UNIQUE_ID_SUFFIX_GARMIN_FIT);
      final Long tourId = _tourData.createTourId(uniqueId);

      /*
       * The tour start time timezone is set from lat/lon in createTimeSeries()
       */
      final ZonedDateTime tourStartTime_FromLatLon = _tourData.getTourStartTime();

      if (zonedStartTime.equals(tourStartTime_FromLatLon) == false) {

         // time zone is different -> fix tour start components with adjusted time zone
         _tourData.setTourStartTime_YYMMDD(tourStartTime_FromLatLon);
      }

      if (_alreadyImportedTours.containsKey(tourId)) {

         // this can happen when interpolated values are reimported

         if (_tourData.interpolatedValueSerie != null) {

            // -> update just the interpolated values

            final TourData tourData = _alreadyImportedTours.get(tourId);

            tourData.interpolatedValueSerie = _tourData.interpolatedValueSerie;

            // this is needed to update the UI
            _newlyImportedTours.put(tourId, tourData);
         }

      } else {

         // add new tour to the map
         _newlyImportedTours.put(tourId, _tourData);

         // create additional data
         _tourData.computeComputedValues();
         _tourData.computeAltimeterGradientSerie();

         /*
          * In the case where the power was retrieved from a developer field, the fit file didn't
          * contain the average power and we need to compute it ourselves.
          */
         if (isComputeAveragePower) {
            final float[] powerSerie = _tourData.getPowerSerie();
            if (powerSerie != null) {
               _tourData.setPower_Avg(_tourData.computeAvg_FromValues(powerSerie, 0, powerSerie.length - 1));
            }
         }

         finalizeTour_Title(_tourData);
         finalizeTour_Elevation(_tourData);
         finalizeTour_Battery(_tourData);
         finalizeTour_Sensor(_tourData);

         // must be called after time series are created
         finalizeTour_Gears(_tourData);

         finalizeTour_Marker(_tourData, _allTourMarker);
         _tourData.finalizeTour_SwimData(_tourData, _allSwimData);

         finalizeTour_Type(_tourData);
      }
   }

   /**
    * Set recording device battery data
    *
    * @param tourData
    */
   private void finalizeTour_Battery(final TourData tourData) {

      final int numBatteryItems = _allBatteryTime.size();

      if (numBatteryItems == 0) {
         return;
      }

      final long tourStartTime = tourData.getTourStartTimeMS();

      final int[] allBatteryTime = new int[numBatteryItems];
      final short[] allBatteryPercentage = new short[numBatteryItems];

      for (int serieIndex = 0; serieIndex < numBatteryItems; serieIndex++) {

         // convert absolute time --> relative time
         final long absoluteTime = _allBatteryTime.get(serieIndex);
         final int relativeTime = (int) (absoluteTime - tourStartTime);

         allBatteryPercentage[serieIndex] = _allBatteryPercentage.get(serieIndex);
         allBatteryTime[serieIndex] = relativeTime;
      }

      tourData.setBattery_Time(allBatteryTime);
      tourData.setBattery_Percentage(allBatteryPercentage);

      tourData.setBattery_Percentage_Start(allBatteryPercentage[0]);
      tourData.setBattery_Percentage_End(allBatteryPercentage[numBatteryItems - 1]);
   }

   /**
    * Compute elevation up/down values when com.garmin.fit.SessionMesg.getTotalAscent() is
    * <code>null</code> -> elevation up/down == 0
    *
    * @param tourData
    */
   private void finalizeTour_Elevation(final TourData tourData) {

      if (tourData.getTourAltUp() == 0 && tourData.getTourAltDown() == 0) {

         tourData.computeAltitudeUpDown();
      }
   }

   private void finalizeTour_Gears(final TourData tourData) {

      if (_allGearData.size() == 0) {
         return;
      }

      final int[] timeSerie = tourData.timeSerie;
      if (timeSerie == null) {
         return;
      }

      /*
       * Validate gear list
       */
      final long tourStartTime = tourData.getTourStartTimeMS();
      final long tourEndTime = tourStartTime + (timeSerie[timeSerie.length - 1] * 1000);

      final List<GearData> validatedGearList = new ArrayList<>();
      GearData startGear = null;

      for (final GearData gearData : _allGearData) {

         final long gearTime = gearData.absoluteTime;

         // ensure time is valid
         if (gearTime < tourStartTime) {
            startGear = gearData;
         }

         final int rearTeeth = gearData.getRearGearTeeth();

         if (rearTeeth == 0 && _gearDataType == GearDataType.FRONT_GEAR_TEETH__REAR_GEAR_TEETH) {

            /**
             * This case happened but it should not. After checking the raw data they contained the
             * wrong values.
             * <p>
             * <code>
             *
             *  2015-08-30 08:12:50.092'345 [FitContextData]
             *
             *    Gears: GearData [absoluteTime=2015-08-27T17:39:08.000+02:00,
             *          FrontGearNum   = 2,
             *          FrontGearTeeth = 50,
             *          RearGearNum    = 172,   <---
             *          RearGearTeeth  = 0      <---
             * ]
             * </code>
             */

            /**
             * Set valid value but make it visible that the values are wrong, visible value is
             *
             * <code>0x10/ 0x30 = 0.33</code>
             */

            gearData.gears = 0x10_01_30_01;
         }

         if (gearTime >= tourStartTime && gearTime <= tourEndTime) {

            // set initial gears when available
            if (startGear != null) {

               // set time to tour start
               startGear.absoluteTime = tourStartTime;

               validatedGearList.add(startGear);
               startGear = null;
            }

            validatedGearList.add(gearData);
         }
      }

      if (validatedGearList.size() > 0) {

         // set end gear
         final GearData lastGearData = validatedGearList.get(validatedGearList.size() - 1);
         if (lastGearData.absoluteTime < tourEndTime) {

            final GearData lastGear = new GearData();
            lastGear.absoluteTime = tourEndTime;
            lastGear.gears = lastGearData.gears;

            validatedGearList.add(lastGear);
         }

         tourData.setGears(validatedGearList);
      }
   }

   private void finalizeTour_Marker(final TourData tourData, final List<TourMarker> allTourMarkers) {

      if (allTourMarkers == null || allTourMarkers.isEmpty()) {
         return;
      }

      final int[] timeSerie = tourData.timeSerie;
      if (timeSerie == null || timeSerie.length == 0) {
         return;
      }

      final int numTimeslices = timeSerie.length;

      final long absoluteTourStartTime = tourData.getTourStartTimeMS();
      final long absoluteTourEndTime = tourData.getTourEndTimeMS();

      final ArrayList<TourMarker> validatedTourMarkers = new ArrayList<>();
      final int tourMarkerSize = allTourMarkers.size();

      int markerIndex = 0;
      int serieIndex = 0;

      boolean isBreakMarkerLoop = false;

      markerLoop:

      for (; markerIndex < tourMarkerSize; markerIndex++) {

         final TourMarker tourMarker = allTourMarkers.get(markerIndex);
         final long absoluteMarkerTime = tourMarker.getDeviceLapTime();

         boolean isSetMarker = false;

         for (; serieIndex < numTimeslices; serieIndex++) {

            int relativeTourTimeS = timeSerie[serieIndex];
            long absoluteTourTime = absoluteTourStartTime + relativeTourTimeS * 1000;

            final long timeDiffEnd = absoluteTourEndTime - absoluteMarkerTime;
            if (timeDiffEnd < 0) {

               // there cannot be a marker after the tour
               if (markerIndex < tourMarkerSize) {

                  // there are still markers available which are not set in the tour, set a last marker into the last time slice

                  // set values for the last time slice
                  serieIndex = numTimeslices - 1;
                  relativeTourTimeS = timeSerie[serieIndex];
                  absoluteTourTime = absoluteTourStartTime + relativeTourTimeS * 1000;

                  isSetMarker = true;
               }

               isBreakMarkerLoop = true;
            }

            final long timeDiffMarker = absoluteMarkerTime - absoluteTourTime;
            if (timeDiffMarker <= 0) {

               // time for the marker is found

               isSetMarker = true;
            }

            if (isSetMarker) {

               /*
                * A last marker can be set when it's far enough away from the end, this will disable
                * the last tour marker
                */
               final boolean canSetLastMarker = _isIgnoreLastMarker
                     && serieIndex < numTimeslices - _lastMarkerTimeSlices;

               if (_isSetLastMarker || canSetLastMarker) {

                  tourMarker.setTime(relativeTourTimeS, absoluteTourTime);
                  tourMarker.setSerieIndex(serieIndex);

                  tourData.completeTourMarker(tourMarker, serieIndex);

                  validatedTourMarkers.add(tourMarker);
               }

               // check next marker
               break;
            }
         }

         if (isBreakMarkerLoop) {
            break markerLoop;
         }
      }

      final Set<TourMarker> tourTourMarkers = new HashSet<>(validatedTourMarkers);

      tourData.setTourMarkers(tourTourMarkers);
   }

   private void finalizeTour_Sensor(final TourData tourData) {

      final List<DeviceSensorValue> allDeviceSensorValues = new ArrayList<>();

      finalizeTour_Sensor_10_ImportedSensors(allDeviceSensorValues);

      /*
       * Set tour values into the sensor values
       */
      for (final DeviceSensorValue deviceSensorValue : allDeviceSensorValues) {

         deviceSensorValue.setTourTime_Start(tourData.getTourStartTimeMS());
         deviceSensorValue.setTourTime_End(tourData.getTourEndTimeMS());

         deviceSensorValue.setTourData(tourData);
      }

      /*
       * Set sensor values into the tour data
       */
      final Set<DeviceSensorValue> tourData_SensorValues = tourData.getDeviceSensorValues();

      tourData_SensorValues.clear();
      tourData_SensorValues.addAll(allDeviceSensorValues);
   }

   private void finalizeTour_Sensor_10_ImportedSensors(final List<DeviceSensorValue> allDeviceSensorValues) {

      final List<DeviceSensorImport> allImportedSensors = new ArrayList<>(_allImportedDeviceSensors.values());

      // sort by device index and date/time
      Collections.sort(allImportedSensors, (sensor1, sensor2) -> {

         int compareDeviceIndex = Short.compare(sensor1.deviceIndex, sensor2.deviceIndex);

         if (compareDeviceIndex == 0) {
            compareDeviceIndex = Long.compare(sensor1.dateTime, sensor2.dateTime);
         }

         return compareDeviceIndex;
      });

      // loop: all imported sensors
      for (int sensorIndex = 0; sensorIndex < allImportedSensors.size(); sensorIndex++) {

         final DeviceSensorImport importedSensor = allImportedSensors.get(sensorIndex);

         final DeviceSensor deviceSensor = RawDataManager.getDeviceSensor(importedSensor);

         finalizeTour_Sensor_30_UpdateSensorKeyValues(deviceSensor, importedSensor);

         /*
          * Set sensor values
          */
         final DeviceSensorValueImport importedSensorValues = importedSensor.sensorValues;

         if (importedSensorValues != null) {

            final DeviceSensorValue sensorValue = new DeviceSensorValue(deviceSensor);

            sensorValue.setBatteryLevel_Start(importedSensorValues.batteryLevel_Start);
            sensorValue.setBatteryLevel_End(importedSensorValues.batteryLevel_End);

            sensorValue.setBatteryStatus_Start(importedSensorValues.batteryStatus_Start);
            sensorValue.setBatteryStatus_End(importedSensorValues.batteryLevel_End);

            sensorValue.setBatteryVoltage_Start(importedSensorValues.batteryVoltage_Start);
            sensorValue.setBatteryVoltage_End(importedSensorValues.batteryVoltage_End);

            allDeviceSensorValues.add(sensorValue);
         }
      }
   }

   private void finalizeTour_Sensor_30_UpdateSensorKeyValues(final DeviceSensor deviceSensor,
                                                             final DeviceSensorImport importedSensor) {

// SET_FORMATTING_OFF

      final Short    importedDeviceType      = importedSensor.deviceType;
      final String   importedDeviceName      = importedSensor.getDeviceName();
//    final Integer  manufacturerNumber      = importedSensor.manufacturerNumber;
//    final Integer  productNumber           = importedSensor.productNumber;
//    final String   productName             = importedSensor.productName;
//    final String   serialNumber            = importedSensor.serialNumber;

//    final String   manufacturerName        = Manufacturer.getStringFromValue(manufacturerNumber);

// SET_FORMATTING_ON

      boolean isSensorUpdated = false;

      if (deviceSensor.getDeviceType() == -1) {

         /**
          * The sensor device type is not yet set, this sensor can be from a MT version before the
          * device type was introduced
          */

         if (importedDeviceType != null) {

            deviceSensor.setDeviceType(importedDeviceType);

            isSensorUpdated = true;

            TourLogManager.log_INFO("Updating device sensor by setting the device type %-5d into %s".formatted( //$NON-NLS-1$
                  importedDeviceType,
                  deviceSensor.getSensorKey_WithDevType()));
         }
      }

      if (StringUtils.hasContent(deviceSensor.getDeviceName()) == false) {

         /**
          * The sensor device name is not yet set, this sensor can be from a MT version before the
          * device name was introduced
          */

         if (StringUtils.hasContent(importedDeviceName)) {

            deviceSensor.setDeviceName(importedDeviceName);

            isSensorUpdated = true;

            TourLogManager.log_INFO("Updating device sensor by setting the device name '%s' into %s".formatted( //$NON-NLS-1$
                  importedDeviceName,
                  deviceSensor.getSensorKey_WithDevType()));
         }
      }

      if (isSensorUpdated) {

         if (deviceSensor.getSensorId() == TourDatabase.ENTITY_IS_NOT_SAVED) {

            /**
             * Nothing to do, sensor will be saved when a tour is saved which contains this sensor
             * in net.tourbook.database.TourDatabase.checkUnsavedTransientInstances_Sensors()
             */

         } else {

            /*
             * Notify post process to update the sensor in the db
             */
            final ConcurrentHashMap<String, DeviceSensor> allDeviceSensorsToBeUpdated = _importState_Process.getAllDeviceSensorsToBeUpdated();

            allDeviceSensorsToBeUpdated.put(deviceSensor.getSensorKey_WithDevType(), deviceSensor);
         }
      }
   }

   private void finalizeTour_Title(final TourData tourData) {

      if (_isSetTourTitleFromFileName == false) {
         return;
      }

      final Path importFilePath = Paths.get(_importFilePathName);

      final String filename = importFilePath.getFileName().toString();
      final String tourTitle = FileUtils.removeExtensions(filename);

      tourData.setTourTitle(tourTitle);
   }

   private void finalizeTour_Type(final TourData tourData) {

      // If enabled, set Tour Type using FIT file data
      if (_isFitImportTourType == false) {
         return;
      }

      TourLogManager.subLog_INFO(UI.EMPTY_STRING

            + " . . . Set tour type from '%s'".formatted(_fitImportTourTypeMode) //$NON-NLS-1$

            + " - 'sport'  '%-10s'".formatted(_sportName) //$NON-NLS-1$
            + " - 'sub-sport' '%-10s'".formatted(_subSportName) //$NON-NLS-1$

            + " - 'profile' '%-10s'".formatted(_profileName) //$NON-NLS-1$
            + " - 'session profile' '%-10s'".formatted(_sessionSportProfileName)); //$NON-NLS-1$

      switch (_fitImportTourTypeMode) {

      case IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORT:

         applyTour_Type(tourData, _sportName);
         break;

      case IPreferences.FIT_IMPORT_TOURTYPE_MODE_PROFILE:

         applyTour_Type(tourData, _profileName);
         break;

      case IPreferences.FIT_IMPORT_TOURTYPE_MODE_TRY_PROFILE:

         if (!UI.EMPTY_STRING.equals(_profileName)) {
            applyTour_Type(tourData, _profileName);
         } else {
            applyTour_Type(tourData, _sportName);
         }
         break;

      case IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORT_AND_PROFILE:

         String spacerText = UI.EMPTY_STRING;

         // Insert spacer character if Sport Name is present
         if ((!UI.EMPTY_STRING.equals(_sportName)) && (!UI.EMPTY_STRING.equals(_profileName))) {
            spacerText = UI.DASH_WITH_SPACE;
         }

         applyTour_Type(tourData, _sportName + spacerText + _profileName);
         break;

      case IPreferences.FIT_IMPORT_TOURTYPE_MODE_SESSION_SPORT_PROFILE_NAME:

         applyTour_Type(tourData, _sessionSportProfileName);

         break;

      case IPreferences.FIT_IMPORT_TOURTYPE_MODE_LOOKUP_SPORT_AND_SUB_SPORT:

         RawDataManager.setTourType(tourData, _sportName, _subSportName);

         break;
      }
   }

   /**
    * @return All collected devices and sensor data from the import file, key is the device index
    *
    */
   public Map<Short, DeviceSensorImport> getAllDeviceSensorImports() {
      return _allImportedDeviceSensors;
   }

   public List<Short> getBattery_Percentage() {
      return _allBatteryPercentage;
   }

   public List<Long> getBattery_Time() {
      return _allBatteryTime;
   }

   public TimeData getCurrent_TimeData() {

      if (_current_TimeData == null) {
         throw new IllegalArgumentException("Time data is not initialized"); //$NON-NLS-1$
      }

      return _current_TimeData;

   }

   public TourMarker getCurrent_TourMarker() {

      if (_current_TourMarker == null) {
         throw new IllegalArgumentException("Tour marker is not initialized"); //$NON-NLS-1$
      }

      return _current_TourMarker;
   }

   public MesgListener_DeviceInfo getDeviceInfoListener() {

      return _deviceInfoListener;
   }

   private String getDeviceName() {

      final StringBuilder deviceName = new StringBuilder();

      if (_manufacturer != null) {
         deviceName.append(_manufacturer).append(UI.SPACE);
      }

      if (_garminProduct != null) {
         deviceName.append(_garminProduct);
      }

      return deviceName.toString();
   }

   /**
    * @param gearDataType
    *           Type of the gear data which are provided
    *
    * @return
    */
   public List<GearData> getGearData(final GearDataType gearDataType) {

      // this value is set in the message listener constructor
      _gearDataType = gearDataType;

      return _allGearData;
   }

   public String getImportFilePathName() {
      return _importFilePathName;
   }

   public ImportState_Process getImportState_Process() {
      return _importState_Process;
   }

   public TimeData getLastAdded_TimeData() {
      return _lastAdded_TimeData;
   }

   public String getManufacturerName(final Integer manufacturerNumber) {

      String manufacturerName = UI.EMPTY_STRING;

      if (manufacturerNumber != null) {
         manufacturerName = Manufacturer.getStringFromValue(manufacturerNumber);
      }

      if (manufacturerName.length() == 0 && manufacturerNumber != null) {
         manufacturerName = manufacturerNumber.toString();
      }

      return manufacturerName;
   }

   public List<Long> getPausedTime_Data() {
      return _pausedTime_Data;
   }

   public List<Long> getPausedTime_End() {
      return _pausedTime_End;
   }

   public List<Long> getPausedTime_Start() {
      return _pausedTime_Start;
   }

   public String getProductNameCombined(final Integer productNumber,
                                        final String productName,
                                        final Integer garminProductNumber,
                                        final String antplusDeviceTypeName) {

      final StringBuilder sb = new StringBuilder();

      if (garminProductNumber != null) {
         sb.append(GarminProduct.getStringFromValue(garminProductNumber));
      }

      if (sb.isEmpty() && productName != null) {
         sb.append(productName);
      }

      if (sb.isEmpty() && productNumber != null) {
         sb.append(productNumber.toString());
      }

      if (antplusDeviceTypeName != null && antplusDeviceTypeName.length() > 0) {

         if (sb.isEmpty() == false) {
            sb.append(UI.DASH_WITH_SPACE);
         }

         sb.append(antplusDeviceTypeName);
      }

      return sb.toString();
   }

   public List<SwimData> getSwimData() {
      return _allSwimData;
   }

   public List<TimeData> getTimeData() {
      return _allTimeData;
   }

   public long getTimeDiffMS() {
      return _timeDiffMS;
   }

   public TourData getTourData() {
      return _tourData;
   }

   public String getTourTitle() {

      return String.format("%s (Session: %s)", _importFilePathName, _sessionIndex); //$NON-NLS-1$
   }

   public boolean isLogSensorValues() {
      return _isLogSensorValues;
   }

   public void onSetup_Lap_10_Initialize() {

      final List<TourMarker> tourMarkers = _allTourMarker;

      _current_TourMarker = new TourMarker(_tourData, ChartLabelMarker.MARKER_TYPE_DEVICE);

      tourMarkers.add(_current_TourMarker);
   }

   public void onSetup_Lap_20_Finalize() {

      _current_TourMarker = null;
   }

   public void onSetup_Record_10_Initialize() {

      _current_TimeData = new TimeData();
   }

   public void onSetup_Record_20_Finalize() {

      if (_current_TimeData == null) {
         // this occurred
         return;
      }

      boolean useThisTimeSlice = true;

      if (_previous_TimeData != null) {

         final long prevTime = _previous_TimeData.absoluteTime;
         final long currentTime = _current_TimeData.absoluteTime;

         if (prevTime == currentTime) {

            /*
             * Ignore and merge duplicated records. The device Bryton 210 creates duplicated
             * entries, to have valid data for this device, they must be merged.
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
         _allTimeData.add(_current_TimeData);
         _lastAdded_TimeData = _current_TimeData;
      }

      _previous_TimeData = _current_TimeData;
      _current_TimeData = null;
   }

   public void onSetup_Session_20_Finalize() {

      onSetup_Record_20_Finalize();

      _timeDiffMS = Long.MIN_VALUE;
   }

   public void setDeviceId(final String deviceId) {
      _deviceId = deviceId;
   }

   public void setDeviceInfoListener(final MesgListener_DeviceInfo deviceInfoListener) {
      _deviceInfoListener = deviceInfoListener;
   }

   public void setGarminProduct(final String garminProduct) {
      _garminProduct = garminProduct;
   }

   public void setHeartRateSensorPresent(final boolean isHeartRateSensorPresent) {
      _tourData.setIsPulseSensorPresent(isHeartRateSensorPresent);
   }

   public void setManufacturer(final String manufacturer) {
      _manufacturer = manufacturer;
   }

   public void setPowerSensorPresent(final boolean isPowerSensorPresent) {
      _tourData.setIsPowerSensorPresent(isPowerSensorPresent);
   }

   public void setProfileName(final String profileName) {
      _profileName = profileName;
   }

   public void setSessionIndex(final SessionMesg mesg) {

      final Integer fitMessageIndex = mesg.getFieldIntegerValue(254);

      final Integer messageIndex = fitMessageIndex != null ? fitMessageIndex : DEFAULT_MESSAGE_INDEX;

      _sessionIndex = messageIndex.toString();
   }

   public void setSessionStartTime(final ZonedDateTime dateTime) {
      _sessionStartTime = dateTime;
   }

   public void setSoftwareVersion(final String softwareVersion) {
      _softwareVersion = softwareVersion;
   }

   public void setSpeedSensorPresent(final boolean isSpeedSensorPresent) {
      _tourData.setIsDistanceFromSensor(isSpeedSensorPresent);
   }

   public void setSportname(final String sportName) {

      _sportName = sportName;
   }

   public void setSportProfileName(final String sportProfileName) {

      _sessionSportProfileName = sportProfileName;
   }

   public void setStrideSensorPresent(final boolean isStrideSensorPresent) {
      _tourData.setIsStrideSensorPresent(isStrideSensorPresent);
   }

   public void setSubSport(final String subSportName) {

      _subSportName = subSportName;
   }

   public void setTimeDiffMS(final long timeDiffMS) {

      _timeDiffMS = timeDiffMS;
   }
}
