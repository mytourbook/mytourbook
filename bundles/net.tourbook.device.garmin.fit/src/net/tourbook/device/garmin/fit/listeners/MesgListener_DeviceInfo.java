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
package net.tourbook.device.garmin.fit.listeners;

import com.garmin.fit.AntplusDeviceType;
import com.garmin.fit.BleDeviceType;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DeviceInfoMesgListener;
import com.garmin.fit.GarminProduct;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.SourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.DeviceSensor;
import net.tourbook.data.DeviceSensorImport;
import net.tourbook.data.DeviceSensorValue;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.garmin.fit.FitData;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.RawDataManager;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class MesgListener_DeviceInfo extends AbstractMesgListener implements DeviceInfoMesgListener {

   private static final String COLUMN_CATEGORY_PRODUCT  = "PROD";     //$NON-NLS-1$
   private static final String COLUMN_CATEGORY_VERSION  = "VERSION";  //$NON-NLS-1$
   private static final String COLUMN_CATEGORY_BATTERIE = "BATTERIE"; //$NON-NLS-1$
   private static final String COLUMN_CATEGORY_ANT      = "ANT";      //$NON-NLS-1$
   private static final String COLUMN_CATEGORY_BLE      = "BLE";      //$NON-NLS-1$
   private static final String COLUMN_CATEGORY_SRC      = "SRC";      //$NON-NLS-1$

   private static final String FORMAT_STRING_6          = " %6s";     //$NON-NLS-1$
   private static final String FORMAT_STRING_10         = " %10s";    //$NON-NLS-1$
   private static final String FORMAT_STRING_10_LEFT    = "%-10s ";   //$NON-NLS-1$
   private static final String FORMAT_STRING_15_LEFT    = "%-15s ";   //$NON-NLS-1$
   private static final String FORMAT_STRING_20_LEFT    = "%-20s ";   //$NON-NLS-1$
   private static final String FORMAT_STRING_30_LEFT    = "%-30s ";   //$NON-NLS-1$
   private static final String FORMAT_FLOAT_10_3        = "%10.3f";   //$NON-NLS-1$
   private static final String LOG_SEPARATOR            = "\t";       //$NON-NLS-1$

// SET_FORMATTING_OFF
   
   // the format width is adjusted to all tested .fit files

   private static final LogItem[]             ALL_LOGS              = {

      new LogItem("Date - Time",             "",            FORMAT_STRING_20_LEFT),       //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Device",                  "Index",       FORMAT_STRING_6),             //$NON-NLS-1$ //$NON-NLS-2$

      new LogItem(COLUMN_CATEGORY_PRODUCT,   "",            FORMAT_STRING_6),             //$NON-NLS-1$
      new LogItem("Manu-",                   "facturer",    FORMAT_STRING_15_LEFT),       //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Product",                 "No",          FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Product",                 "Name",        FORMAT_STRING_20_LEFT),       //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Garmin",                  "Prod No",     FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Garmin",                  "Name",        FORMAT_STRING_30_LEFT),       //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Device",                  "Type",        FORMAT_STRING_6),             //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Ant",                     "Plus",        FORMAT_STRING_15_LEFT),       //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Descriptor",              "",            FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$

      new LogItem(COLUMN_CATEGORY_VERSION,   "",            FORMAT_STRING_10),            //$NON-NLS-1$
      new LogItem("Serial",                  "Number",      FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Hardware",                "Version",     FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Software",                "Version",     FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$

      new LogItem(COLUMN_CATEGORY_BATTERIE,  "",            FORMAT_STRING_10),            //$NON-NLS-1$
      new LogItem("Batterie",                "Level",       FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Batterie",                "Status",      FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Batterie",                "Voltage",     FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Cummulated",              "Op Time",     FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$

      new LogItem(COLUMN_CATEGORY_ANT,       "",            FORMAT_STRING_10),            //$NON-NLS-1$
      new LogItem("Source",                  "Type",        FORMAT_STRING_10_LEFT),       //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Network",                 "",            FORMAT_STRING_10_LEFT),       //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Device",                  "Type",        FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Device",                  "No",          FORMAT_STRING_6),             //$NON-NLS-1$ //$NON-NLS-2$
      new LogItem("Transport",               "Type",        FORMAT_STRING_10),            //$NON-NLS-1$ //$NON-NLS-2$

      // ble = (B)luetooth (L)ow (E)nergy
      new LogItem(COLUMN_CATEGORY_BLE,       "",            FORMAT_STRING_6),             //$NON-NLS-1$
      new LogItem("Device",                  "Type",        FORMAT_STRING_10_LEFT),       //$NON-NLS-1$ //$NON-NLS-2$

      new LogItem(COLUMN_CATEGORY_SRC,       "",            FORMAT_STRING_10),            //$NON-NLS-1$
      new LogItem("Sensor",                  "Position",    FORMAT_STRING_10_LEFT),       //$NON-NLS-1$ //$NON-NLS-2$
   };

// SET_FORMATTING_ON

   private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH-mm-ss"); //$NON-NLS-1$

   private boolean                        _isLogDeviceData;

   private List<String>                   _allHeader1;
   private List<String>                   _allHeader2;
   private String                         _logFormats;

   private final List<DeviceInfoMesg>     _allDeviceInfoMesg  = new ArrayList<>();

   private static class LogItem {

      String header1;
      String header2;
      String format;

      public LogItem(final String header1, final String header2, final String format) {

         this.header1 = header1;
         this.header2 = header2;
         this.format = format;
      }
   }

   public MesgListener_DeviceInfo(final FitData fitData) {

      super(fitData);

      _isLogDeviceData = false;
      _isLogDeviceData = true;

      fitData.setDeviceInfoListener(this);
   }

   private String getManufacturerName(final Integer manufacturerNumber) {

      String manufacturerName = UI.EMPTY_STRING;

      if (manufacturerNumber != null) {
         manufacturerName = Manufacturer.getStringFromValue(manufacturerNumber);
      }

      if (manufacturerName.length() == 0 && manufacturerNumber != null) {
         manufacturerName = manufacturerNumber.toString();
      }

      return manufacturerName;
   }

   private String getProductNameCombined(final Integer productNumber,
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

   private boolean hasHeartRateSensor(final Short deviceType) {

      return deviceType.equals(AntplusDeviceType.HEART_RATE);
   }

   private boolean hasPowerSensor(final Short deviceType) {

      return deviceType.equals(AntplusDeviceType.BIKE_POWER);
   }

   private boolean hasSpeedSensor(final Short deviceType) {

      return deviceType.equals(AntplusDeviceType.BIKE_SPEED)
            || deviceType.equals(AntplusDeviceType.BIKE_SPEED_CADENCE)
            || deviceType.equals(AntplusDeviceType.STRIDE_SPEED_DISTANCE);
   }

   private boolean hasStrideSensor(final Short deviceType) {

      return deviceType.equals(AntplusDeviceType.STRIDE_SPEED_DISTANCE);
   }

   public void logDeviceData() {

      if (_isLogDeviceData == false) {
         return;
      }

      // sort messages by device number
      _allDeviceInfoMesg.sort((final DeviceInfoMesg devInfo1, final DeviceInfoMesg devInfo2) -> {

         final short deviceIndex1 = devInfo1.getDeviceIndex();
         final short deviceIndex2 = devInfo2.getDeviceIndex();

         final int returnValue = (deviceIndex1 < deviceIndex2)
               ? -1
               : ((deviceIndex1 == deviceIndex2)
                     ? 0
                     : 1);

         return returnValue;
      });

      logDeviceData_1_Header();

      short prevDevIndex = -1;

      for (final DeviceInfoMesg deviceInfoMesg : _allDeviceInfoMesg) {

         final short currentDeviceIndex = deviceInfoMesg.getDeviceIndex();

         // log empty line between devices
         if (currentDeviceIndex != prevDevIndex) {

            System.out.println();

            prevDevIndex = currentDeviceIndex;
         }

         logDeviceData_2_Content(deviceInfoMesg);
      }
   }

   private void logDeviceData_1_Header() {

      // print log header

      System.out.println();
      System.out.println();
      System.out.println();
      System.out.println();

      if (_allHeader1 == null) {

         _allHeader1 = new ArrayList<>();
         _allHeader2 = new ArrayList<>();

         final StringBuilder sbFormats = new StringBuilder();

         for (final LogItem log : ALL_LOGS) {

            _allHeader1.add(log.header1);
            _allHeader2.add(log.header2);

            sbFormats.append(log.format);
            sbFormats.append(LOG_SEPARATOR);
         }

         _logFormats = sbFormats.toString();
      }

      System.out.println(_logFormats.formatted(_allHeader1.toArray()));
      System.out.println(_logFormats.formatted(_allHeader2.toArray()));

   }

   private void logDeviceData_2_Content(final DeviceInfoMesg mesg) {

// SET_FORMATTING_OFF

      final Integer     mesgManufacturer_Number    = mesg.getManufacturer();
      final Integer     mesgProduct_Number         = mesg.getProduct();
      final String      mesgProduct_Name           = mesg.getProductName();
      final Integer     mesgGarminProduct_Number   = mesg.getGarminProduct();
      final Short       mesgAntPlusDeviceType      = mesg.getAntplusDeviceType();
      final SourceType  mesgSourceType             = mesg.getSourceType();

      final String manufacturerName       = getManufacturerName(mesgManufacturer_Number);
      final String antplusDeviceTypeName  = AntplusDeviceType.getStringFromValue(mesgAntPlusDeviceType);

      final String sourceTypeText         = mesgSourceType == null
                                                   ? UI.EMPTY_STRING
                                                   : SourceType.getStringFromValue(mesgSourceType);

      final String garminProductName      = mesgGarminProduct_Number == null
                                                   ? UI.EMPTY_STRING
                                                   : GarminProduct.getStringFromValue(mesgGarminProduct_Number);

// SET_FORMATTING_ON

      System.out.println(_logFormats.formatted(

//          Thread.currentThread().getName(),

            // device
            DATE_TIME_FORMATTER.print(mesg.getTimestamp().getDate().getTime()),
            removeNull(mesg.getDeviceIndex()),

            // product
            COLUMN_CATEGORY_PRODUCT,
            removeNull(manufacturerName),
            removeNull(mesgProduct_Number),
            removeNull(mesgProduct_Name),
            removeNull(mesgGarminProduct_Number),
            garminProductName,
            removeNull(mesg.getDeviceType()),
            antplusDeviceTypeName,
            removeNull(mesg.getDescriptor()),

            // serial
            COLUMN_CATEGORY_VERSION,
            removeNull(mesg.getSerialNumber()),
            removeNull(mesg.getHardwareVersion()),
            removeNull_3(mesg.getSoftwareVersion()),

            // battery
            COLUMN_CATEGORY_BATTERIE,
            removeNull(mesg.getBatteryLevel()),
            removeNull(mesg.getBatteryStatus()),
            removeNull_3(mesg.getBatteryVoltage()),
            removeNull(mesg.getCumOperatingTime()),

            // ant
            COLUMN_CATEGORY_ANT,
            sourceTypeText,
            removeNull(mesg.getAntNetwork()),
            removeNull(mesg.getAntDeviceType()),
            removeNull(mesg.getAntDeviceNumber()),
            removeNull(mesg.getAntTransmissionType()),

            // ble = (B)luetooth (L)ow (E)nergy
            COLUMN_CATEGORY_BLE,
            BleDeviceType.getStringFromValue(mesg.getBleDeviceType()),

            // src
            COLUMN_CATEGORY_SRC,
            removeNull(mesg.getSensorPosition())

      ));
   }

   @Override
   public void onMesg(final DeviceInfoMesg mesg) {

      final Short deviceType = mesg.getDeviceType();

      if (deviceType != null) {

         final boolean hasSpeedSensor = hasSpeedSensor(deviceType);
         final boolean hasHeartRateSensor = hasHeartRateSensor(deviceType);
         final boolean hasPowerSensor = hasPowerSensor(deviceType);
         final boolean hasStrideSensor = hasStrideSensor(deviceType);

         /*
          * This event occurs several times and can set a true to false, therefore only true is
          * set, false is the default.
          */

         if (hasSpeedSensor) {
            fitData.setSpeedSensorPresent(hasSpeedSensor);
         }

         if (hasHeartRateSensor) {
            fitData.setHeartRateSensorPresent(hasHeartRateSensor);
         }

         if (hasPowerSensor) {
            fitData.setPowerSensorPresent(hasPowerSensor);
         }

         if (hasStrideSensor) {
            fitData.setStrideSensorPresent(hasStrideSensor);
         }
      }

      setSensorData(mesg);

      if (_isLogDeviceData) {
         _allDeviceInfoMesg.add(mesg);
      }
   }

   private String removeNull(final Integer value) {

      return value == null
            ? UI.EMPTY_STRING
            : value.toString();
   }

   private String removeNull(final Long value) {

      return value == null
            ? UI.EMPTY_STRING
            : value.toString();
   }

   private Object removeNull(final Object value) {

      return value == null
            ? UI.EMPTY_STRING
            : value.toString();
   }

   private String removeNull(final Short value) {

      return value == null
            ? UI.EMPTY_STRING
            : value.toString();
   }

   private String removeNull(final String value) {

      return value == null ? UI.EMPTY_STRING : value;
   }

   private String removeNull_3(final Float value) {

      return value == null
            ? UI.EMPTY_STRING
            : String.format(FORMAT_FLOAT_10_3, value);
   }

   private void setSensorData(final DeviceInfoMesg mesg) {
      // TODO Auto-generated method stub

// SET_FORMATTING_OFF

//       final DateTime       timestamp                  = mesg.getTimestamp();

         final Integer        mesgManufacturerNumber     = mesg.getManufacturer();
         final Integer        mesgProductNumber          = mesg.getProduct();
         final String         mesgProductName            = mesg.getProductName();
         final Integer        mesgGarminProductNumber    = mesg.getGarminProduct();
//       final String         mesgDescriptor             = mesg.getDescriptor();

         final Short          mesgBatteryStatus          = mesg.getBatteryStatus();
         final Float          mesgBatteryVoltage         = mesg.getBatteryVoltage();
         final Short          mesgBatteryLevel           = mesg.getBatteryLevel();
//       final Long           mesgCumOperatingTime       = mesg.getCumOperatingTime();

         final Short          mesgDeviceIndex            = mesg.getDeviceIndex();
//       final Short          mesgHardwareVersion        = mesg.getHardwareVersion();
//       final Float          mesgSoftwareVersion        = mesg.getSoftwareVersion();
//
//       final AntNetwork     mesgAntNetwork             = mesg.getAntNetwork();
//       final Short          mesgAntDeviceType          = mesg.getAntDeviceType();
//       final Integer        mesgAntDeviceNumber        = mesg.getAntDeviceNumber();
         final Short          mesgAntplusDeviceType      = mesg.getAntplusDeviceType();
//       final Short          mesgAntTransmissionType    = mesg.getAntTransmissionType();
//
//       final SourceType     mesgSourceType             = mesg.getSourceType();
//       final BodyLocation   mesgSensorPosition         = mesg.getSensorPosition();

         /*
          * Gear shifting (Di2) battery, the hack is from
          * https://forums.garmin.com/developer/fit-sdk/f/discussion/276245/di2-battery-level
          */
//       final Short          mesgBatteryLevel           = mesg.getFieldShortValue(32, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD);

// SET_FORMATTING_ON

      // key is the device index
      final Map<Short, DeviceSensorImport> allDeviceSensors = fitData.getAllDeviceSensors();

      DeviceSensorImport deviceSensorRaw = allDeviceSensors.get(mesgDeviceIndex);

      if (deviceSensorRaw == null) {

         deviceSensorRaw = new DeviceSensorImport(mesgDeviceIndex);
      }

      /*
       * Set/update device sensor
       */
      if (mesgManufacturerNumber != null) {
         deviceSensorRaw.manufacturerNumber = mesgManufacturerNumber;
      }

      if (mesgProductNumber != null) {
         deviceSensorRaw.productNumber = mesgProductNumber;
      }

      if (StringUtils.hasContent(mesgProductName)) {
         deviceSensorRaw.productName = mesgProductName;
      }

      if (mesgGarminProductNumber != null) {
         deviceSensorRaw.garminProductNumber = mesgGarminProductNumber;
      }

      final String antPlusDeviceType = AntplusDeviceType.getStringFromValue(mesgAntplusDeviceType);
      if (StringUtils.hasContent(antPlusDeviceType)) {
         deviceSensorRaw.antPlusDeviceType = antPlusDeviceType;
      }

   }

   private void setSensorData_WithName(final DeviceInfoMesg mesg) {

// SET_FORMATTING_OFF

      final Integer  mesgManufacturerNumber     = mesg.getManufacturer();
      final Integer  mesgProductNumber          = mesg.getProduct();
      final String   mesgProductName            = mesg.getProductName();

      final Short    antplusDeviceType          = mesg.getAntplusDeviceType();

      final Short    mesgBatteryLevel           = mesg.getBatteryLevel();     // %
      final Float    mesgBatteryVoltage         = mesg.getBatteryVoltage();   // Volt
      final Short    mesgBatteryStatus          = mesg.getBatteryStatus();    // OK, ...

      final String   manufacturerName           = getManufacturerName(mesgManufacturerNumber);
      final String   antplusDeviceTypeName      = AntplusDeviceType.getStringFromValue(antplusDeviceType);
      final String   productName                = getProductNameCombined(mesgProductNumber, mesgProductName, null, antplusDeviceTypeName);

// SET_FORMATTING_ON

      /*
       * Get sensor
       */
      final Map<Long, DeviceSensor> allDbSensors = TourDatabase.getAllDeviceSensors_BySensorID();

      DeviceSensor sensor = null;

      for (final DeviceSensor dbSensor : allDbSensors.values()) {

         final String dbManufacturerName = dbSensor.getManufacturerName();
         final String dbProductName = dbSensor.getProductName();

         if (dbManufacturerName != null && dbManufacturerName.equals(manufacturerName)
               && dbProductName != null && dbProductName.equals(productName)) {

            sensor = dbSensor;

            break;
         }
      }

      if (sensor == null) {

         // create a new sensor

         sensor = RawDataManager.createDeviceSensor(

               mesgManufacturerNumber == null ? -1 : mesgManufacturerNumber,
               manufacturerName,

               mesgProductNumber == null ? -1 : mesgProductNumber,
               productName,

               null // serialNumber is not available
         );
      }

      /*
       * Get sensor value
       */
      final List<DeviceSensorValue> allImportedSensorValues = fitData.getDeviceSensorValues();
      DeviceSensorValue sensorValue = null;

      for (final DeviceSensorValue importedSensorValue : allImportedSensorValues) {

         final DeviceSensor importedSensor = importedSensorValue.getDeviceSensor();

         final String dbManufacturerName = importedSensor.getManufacturerName();
         final String dbProductName = importedSensor.getProductName();

         if (dbManufacturerName != null && dbManufacturerName.equals(manufacturerName)
               && dbProductName != null && dbProductName.equals(productName)) {

            // sensor found in sensor values

            sensorValue = importedSensorValue;

            sensorValue.setBattery_Level(mesgBatteryLevel);
            sensorValue.setBattery_Status(mesgBatteryStatus);
            sensorValue.setBattery_Voltage(mesgBatteryVoltage);

            break;
         }
      }

      if (sensorValue == null) {

         // create new sensor value -> set start values

         sensorValue = new DeviceSensorValue(sensor);

         allImportedSensorValues.add(sensorValue);

         sensorValue.setBattery_Level(mesgBatteryLevel);
         sensorValue.setBattery_Status(mesgBatteryStatus);
         sensorValue.setBattery_Voltage(mesgBatteryVoltage);
      }

   }

   private void setSensorData_WithSerialNo(final DeviceInfoMesg mesg, final Long serialNumber) {

// SET_FORMATTING_OFF

//    final DateTime       timestamp                  = mesg.getTimestamp();

      final Integer        mesgManufacturerNumber     = mesg.getManufacturer();
      final Integer        mesgProductNumber          = mesg.getProduct();
      final String         mesgProductName            = mesg.getProductName();
      final Integer        mesgGarminProductNumber    = mesg.getGarminProduct();
//    final String         mesgDescriptor             = mesg.getDescriptor();

      final Short          mesgBatteryStatus          = mesg.getBatteryStatus();
      final Float          mesgBatteryVoltage         = mesg.getBatteryVoltage();
//    final Long           mesgCumOperatingTime       = mesg.getCumOperatingTime();

//    final Short          mesgDeviceIndex            = mesg.getDeviceIndex();
//    final Short          mesgHardwareVersion        = mesg.getHardwareVersion();
//    final Float          mesgSoftwareVersion        = mesg.getSoftwareVersion();
//
//    final AntNetwork     mesgAntNetwork             = mesg.getAntNetwork();
//    final Short          mesgAntDeviceType          = mesg.getAntDeviceType();
//    final Integer        mesgAntDeviceNumber        = mesg.getAntDeviceNumber();
      final Short          mesgAntplusDeviceType      = mesg.getAntplusDeviceType();
//    final Short          mesgAntTransmissionType    = mesg.getAntTransmissionType();
//
//    final SourceType     mesgSourceType             = mesg.getSourceType();
//    final BodyLocation   mesgSensorPosition         = mesg.getSensorPosition();

      /*
       * Gear shifting (Di2) battery, the hack is from
       * https://forums.garmin.com/developer/fit-sdk/f/discussion/276245/di2-battery-level
       */
//    final Short          mesgBatteryLevel           = mesg.getFieldShortValue(32, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD);
      final Short          mesgBatteryLevel           = mesg.getBatteryLevel();

// SET_FORMATTING_ON

      final String sensorSerialNumberKey = serialNumber.toString();
      final String antplusDeviceTypeName = AntplusDeviceType.getStringFromValue(mesgAntplusDeviceType);

      /*
       * Get sensor
       */
      final Map<String, DeviceSensor> allDbSensors = TourDatabase.getAllDeviceSensors_BySerialNum();

      DeviceSensor sensor = allDbSensors.get(sensorSerialNumberKey);

      if (sensor == null) {

         // create sensor

         final String manufacturerName = getManufacturerName(mesgManufacturerNumber);
         final String productName = getProductNameCombined(mesgProductNumber, mesgProductName, mesgGarminProductNumber, antplusDeviceTypeName);

         sensor = RawDataManager.createDeviceSensor(

               mesgManufacturerNumber == null ? -1 : mesgManufacturerNumber,
               manufacturerName,

               mesgProductNumber == null ? -1 : mesgProductNumber,
               productName,

               sensorSerialNumberKey);
      }

      updateSensorNames(

            sensor,

            mesgManufacturerNumber,
            mesgProductNumber,
            mesgProductName,
            mesgGarminProductNumber,
            antplusDeviceTypeName);

      /*
       * Get sensor value
       */
      final List<DeviceSensorValue> allImportedSensorValues = fitData.getDeviceSensorValues();
      DeviceSensorValue sensorValue = null;

      for (final DeviceSensorValue importedSensorValue : allImportedSensorValues) {

         final DeviceSensor importedSensor = importedSensorValue.getDeviceSensor();

         final String importedSerialNumber = importedSensor.getSerialNumber();

         if (StringUtils.hasContent(importedSerialNumber)
               && importedSerialNumber.equals(sensorSerialNumberKey)) {

            // sensor found in sensor values

            sensorValue = importedSensorValue;

            sensorValue.setBattery_Level(mesgBatteryLevel);
            sensorValue.setBattery_Status(mesgBatteryStatus);
            sensorValue.setBattery_Voltage(mesgBatteryVoltage);

            break;
         }
      }

      if (sensorValue == null) {

         // create new sensor value -> set start values

         sensorValue = new DeviceSensorValue(sensor);

         allImportedSensorValues.add(sensorValue);

         sensorValue.setBattery_Level(mesgBatteryLevel);
         sensorValue.setBattery_Status(mesgBatteryStatus);
         sensorValue.setBattery_Voltage(mesgBatteryVoltage);
      }
   }

   /**
    * It is possible that a manufacturer/product number is null/-1, try to set the sensor
    * manufacturer/product number/name from another tour
    *
    * @param sensor
    * @param mesgManufacturerNumber
    * @param mesgProductNumber
    * @param mesgProductName
    * @param mesgGarminProductNumber
    * @param antplusDeviceTypeName
    */
   private void updateSensorNames(final DeviceSensor sensor,
                                  final Integer mesgManufacturerNumber,
                                  final Integer mesgProductNumber,
                                  final String mesgProductName,
                                  final Integer mesgGarminProductNumber,
                                  final String antplusDeviceTypeName) {

      boolean isProductUpdated = false;
      boolean isManufacturerUpdated = false;

      if (true

            // manufacturer number is available
            && mesgManufacturerNumber != null &&

            // manufacturer number is not yet set in the sensor
            sensor.getManufacturerNumber() == -1) {

         /*
          * Update sensor (saved or not saved) entity
          */
         sensor.setManufacturerNumber(mesgManufacturerNumber);
         sensor.setManufacturerName(getManufacturerName(mesgManufacturerNumber));

         isManufacturerUpdated = true;
      }

      if (true

            // product number is available
            && mesgProductNumber != null &&

            // product number is not yet set in the sensor
            sensor.getProductNumber() == -1) {

         /*
          * Update sensor (saved or not saved) entity
          */
         sensor.setProductNumber(mesgProductNumber);
         sensor.setProductName(getProductNameCombined(mesgProductNumber, mesgProductName, mesgGarminProductNumber, antplusDeviceTypeName));

         isProductUpdated = true;
      }

      if (isProductUpdated || isManufacturerUpdated) {

         if (sensor.getSensorId() == TourDatabase.ENTITY_IS_NOT_SAVED) {

            /*
             * Nothing to do, sensor will be saved when a tour is saved which contains this sensor
             * in net.tourbook.database.TourDatabase.checkUnsavedTransientInstances_Sensors()
             */

         } else {

            /*
             * Notify post process to update the sensor in the db
             */
            final ImportState_Process importState_Process = fitData.getImportState_Process();
            final ConcurrentHashMap<String, DeviceSensor> allDeviceSensorsToBeUpdated = importState_Process.getAllDeviceSensorsToBeUpdated();

            allDeviceSensorsToBeUpdated.put(sensor.getSerialNumber(), sensor);
         }
      }
   }

}
