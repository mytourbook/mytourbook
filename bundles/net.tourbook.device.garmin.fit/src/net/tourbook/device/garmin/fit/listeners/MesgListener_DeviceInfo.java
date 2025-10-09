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
import com.garmin.fit.DateTime;
import com.garmin.fit.DeviceIndex;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DeviceInfoMesgListener;
import com.garmin.fit.GarminProduct;
import com.garmin.fit.SourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.DeviceSensorImport;
import net.tourbook.data.DeviceSensorValueImport;
import net.tourbook.device.garmin.fit.FitData;
import net.tourbook.tour.TourLogManager;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class MesgListener_DeviceInfo extends AbstractMesgListener implements DeviceInfoMesgListener {

   private static final String COLUMN_CATEGORY_PRODUCT   = "PROD";     //$NON-NLS-1$
   private static final String COLUMN_CATEGORY_VERSION   = "VERSION";  //$NON-NLS-1$
   private static final String COLUMN_CATEGORY_BATTERIE  = "BATTERIE"; //$NON-NLS-1$
   private static final String COLUMN_CATEGORY_ANT       = "ANT";      //$NON-NLS-1$
   private static final String COLUMN_CATEGORY_BLE       = "BLE";      //$NON-NLS-1$
   private static final String COLUMN_CATEGORY_SRC       = "SRC";      //$NON-NLS-1$

   private static final String FORMAT_STRING_6           = " %6s";     //$NON-NLS-1$
   private static final String FORMAT_STRING_10          = " %10s";    //$NON-NLS-1$
   private static final String FORMAT_STRING_10_LEFT     = "%-10s ";   //$NON-NLS-1$
   private static final String FORMAT_STRING_15_LEFT     = "%-15s ";   //$NON-NLS-1$
   private static final String FORMAT_STRING_20_LEFT     = "%-20s ";   //$NON-NLS-1$
   private static final String FORMAT_STRING_30_LEFT     = "%-30s ";   //$NON-NLS-1$
   private static final String FORMAT_FLOAT_10_3         = "%10.3f";   //$NON-NLS-1$

   private static final String LOG_SEPARATOR             = "\t";       //$NON-NLS-1$
   private static final String COLUMN_CATEGORY_SEPARATOR = "|";        //$NON-NLS-1$

// SET_FORMATTING_OFF

   /*
    * The format WIDTHs are adjusted to ALL tested .fit files
    */

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

   private boolean                        _isLogSensorValues;

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

      _isLogSensorValues = fitData.isLogSensorValues();

      fitData.setDeviceInfoListener(this);
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

      if (_isLogSensorValues == false) {
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

         final Short currentDeviceIndex = deviceInfoMesg.getDeviceIndex();

         // log empty line between devices
         if (currentDeviceIndex != prevDevIndex) {

            TourLogManager.log_INFO(UI.EMPTY_STRING);

            prevDevIndex = currentDeviceIndex;
         }

         logDeviceData_2_Content(deviceInfoMesg);
      }
   }

   private void logDeviceData_1_Header() {

      // print log header

      if (_allHeader1 == null) {

         _allHeader1 = new ArrayList<>();
         _allHeader2 = new ArrayList<>();

         final StringBuilder sbFormats = new StringBuilder();

         // add a separator to separate values from the log prefix, e.g. log timestamp
         sbFormats.append(LOG_SEPARATOR);

         for (final LogItem log : ALL_LOGS) {

            _allHeader1.add(log.header1);
            _allHeader2.add(log.header2);

            sbFormats.append(log.format);
            sbFormats.append(LOG_SEPARATOR);
         }

         _logFormats = sbFormats.toString();
      }

      TourLogManager.log_INFO(_logFormats.formatted(_allHeader1.toArray()));
      TourLogManager.log_INFO(_logFormats.formatted(_allHeader2.toArray()));

   }

   private void logDeviceData_2_Content(final DeviceInfoMesg mesg) {

// SET_FORMATTING_OFF

      final long        mesgDateTime               = mesg.getTimestamp().getDate().getTime();
      final Integer     mesgManufacturer_Number    = mesg.getManufacturer();
      final Integer     mesgProduct_Number         = mesg.getProduct();
      final String      mesgProduct_Name           = mesg.getProductName();
      final Integer     mesgGarminProduct_Number   = mesg.getGarminProduct();
      final Short       mesgAntPlusDeviceType      = mesg.getAntplusDeviceType();
      final SourceType  mesgSourceType             = mesg.getSourceType();

      final String manufacturerName       = fitData.getManufacturerName(mesgManufacturer_Number);
      final String antplusDeviceTypeName  = AntplusDeviceType.getStringFromValue(mesgAntPlusDeviceType);

      final String sourceTypeText         = mesgSourceType == null
                                                   ? UI.EMPTY_STRING
                                                   : SourceType.getStringFromValue(mesgSourceType);

      final String garminProductName      = mesgGarminProduct_Number == null
                                                   ? UI.EMPTY_STRING
                                                   : GarminProduct.getStringFromValue(mesgGarminProduct_Number);

      final Short deviceIndex = mesg.getDeviceIndex();
      String deviceIndexText = DeviceIndex.getStringFromValue(deviceIndex);

      deviceIndexText = StringUtils.hasContent(deviceIndexText)
            // CREATOR = 0; // Creator of the file is always device index 0, see com.garmin.fit.DeviceIndex
            ? deviceIndexText
            : Short.toString(deviceIndex);

// SET_FORMATTING_ON

      TourLogManager.log_INFO(_logFormats.formatted(

//          Thread.currentThread().getName(),

            // device
            DATE_TIME_FORMATTER.print(mesgDateTime),
            deviceIndexText,

            // product
            COLUMN_CATEGORY_SEPARATOR,
            removeNull(manufacturerName),
            removeNull(mesgProduct_Number),
            removeNull(mesgProduct_Name),
            removeNull(mesgGarminProduct_Number),
            garminProductName,
            removeNull(mesg.getDeviceType()),
            antplusDeviceTypeName,
            removeNull(mesg.getDescriptor()),

            // serial
            COLUMN_CATEGORY_SEPARATOR,
            removeNull(mesg.getSerialNumber()),
            removeNull(mesg.getHardwareVersion()),
            removeNull_3(mesg.getSoftwareVersion()),

            // battery
            COLUMN_CATEGORY_SEPARATOR,
            removeNull(mesg.getBatteryLevel()),
            removeNull(mesg.getBatteryStatus()),
            removeNull_3(mesg.getBatteryVoltage()),
            removeNull(mesg.getCumOperatingTime()),

            // ant
            COLUMN_CATEGORY_SEPARATOR,
            sourceTypeText,
            removeNull(mesg.getAntNetwork()),
            removeNull(mesg.getAntDeviceType()),
            removeNull(mesg.getAntDeviceNumber()),
            removeNull(mesg.getAntTransmissionType()),

            // ble = (B)luetooth (L)ow (E)nergy
            COLUMN_CATEGORY_SEPARATOR,
            BleDeviceType.getStringFromValue(mesg.getBleDeviceType()),

            // src
            COLUMN_CATEGORY_SEPARATOR,
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

      if (_isLogSensorValues) {

         // keep all messages

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

      final DateTime timestamp = mesg.getTimestamp();

// SET_FORMATTING_OFF

         final long           mesgDateTime               = timestamp == null ? 0 : timestamp.getDate().getTime();
         final Short          mesgDeviceIndex            = mesg.getDeviceIndex();

         final Integer        mesgManufacturer           = mesg.getManufacturer();
         final Integer        mesgProduct_Number         = mesg.getProduct();
         final String         mesgProductName            = mesg.getProductName();
         final Integer        mesgGarminProduct          = mesg.getGarminProduct();
//       final String         mesgDescriptor             = mesg.getDescriptor();
         final Short          mesgDeviceType             = mesg.getDeviceType();

         final Short          mesgBatteryStatus          = mesg.getBatteryStatus();
         final Float          mesgBatteryVoltage         = mesg.getBatteryVoltage();
         final Short          mesgBatteryLevel           = mesg.getBatteryLevel();
//       final Long           mesgCumOperatingTime       = mesg.getCumOperatingTime();

         final Long           mesgSerialNumber           = mesg.getSerialNumber();
         final Float          mesgSoftwareVersion        = mesg.getSoftwareVersion();
//       final Short          mesgHardwareVersion        = mesg.getHardwareVersion();
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

      /**
       * Key is the device index which is unique for each sensor, however this is not always the
       * case but will be ignored for now
       */
      final Map<Short, DeviceSensorImport> allDeviceSensorImports = fitData.getAllDeviceSensorImports();

      DeviceSensorImport deviceSensorImport = allDeviceSensorImports.get(mesgDeviceIndex);

      if (deviceSensorImport == null) {

         deviceSensorImport = new DeviceSensorImport(mesgDeviceIndex);

         allDeviceSensorImports.put(mesgDeviceIndex, deviceSensorImport);
      }

      /*
       * Set/update device sensor
       */
      deviceSensorImport.dateTime = mesgDateTime;

      if (mesgDeviceType != null) {
         deviceSensorImport.deviceType = mesgDeviceType;
      }
      if (mesgManufacturer != null) {
         deviceSensorImport.manufacturerNumber = mesgManufacturer;
      }

      if (mesgProduct_Number != null) {
         deviceSensorImport.productNumber = mesgProduct_Number;
      }

      if (StringUtils.hasContent(mesgProductName)) {
         deviceSensorImport.setProductName(mesgProductName);
      }

      if (mesgGarminProduct != null) {
         deviceSensorImport.garminProductNumber = mesgGarminProduct;
      }

      if (mesgSerialNumber != null) {
         deviceSensorImport.serialNumber = Long.toString(mesgSerialNumber);
      }

      if (mesgAntplusDeviceType != null) {
         deviceSensorImport.antPlusDeviceType = mesgAntplusDeviceType;
      }

      if (mesgSoftwareVersion != null) {
         deviceSensorImport.softwareVersion = mesgSoftwareVersion;
      }

      /*
       * Set sensor values, when available
       */
      if (mesgBatteryLevel != null
            || mesgBatteryStatus != null
            || mesgBatteryVoltage != null) {

         final DeviceSensorValueImport sensorValues = deviceSensorImport.sensorValues;

         sensorValues.setBattery_Level(mesgBatteryLevel);
         sensorValues.setBattery_Status(mesgBatteryStatus);
         sensorValues.setBattery_Voltage(mesgBatteryVoltage);
      }
   }
}
