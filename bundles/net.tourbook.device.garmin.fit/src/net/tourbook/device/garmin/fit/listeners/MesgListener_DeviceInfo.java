/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import com.garmin.fit.AntNetwork;
import com.garmin.fit.AntplusDeviceType;
import com.garmin.fit.BleDeviceType;
import com.garmin.fit.BodyLocation;
import com.garmin.fit.DateTime;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DeviceInfoMesgListener;
import com.garmin.fit.Fit;
import com.garmin.fit.GarminProduct;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.SourceType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.tourbook.common.UI;
import net.tourbook.data.DeviceSensor;
import net.tourbook.data.DeviceSensorValue;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.garmin.fit.FitData;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.RawDataManager;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class MesgListener_DeviceInfo extends AbstractMesgListener implements DeviceInfoMesgListener {

   private static final String            FORMAT_STRING_10      = " %10s";                                          //$NON-NLS-1$
   private static final String            FORMAT_STRING_10_LEFT = " %-10s";                                         //$NON-NLS-1$
   private static final String            FORMAT_FLOAT_10_3     = "%10.3f";                                         //$NON-NLS-1$
   private static final String            LOG_SEPARATOR         = "\t";                                             //$NON-NLS-1$

   private static final DateTimeFormatter DATE_TIME_FORMATTER   = DateTimeFormat.forPattern("yyyy-MM-dd HH-mm-ss"); //$NON-NLS-1$

   private boolean                        _isLogDeviceData;

   public MesgListener_DeviceInfo(final FitData fitData) {

      super(fitData);

      _isLogDeviceData = false;
//    _isLogDeviceData = true;

      if (_isLogDeviceData) {
         logDeviceData_1_Header();
      }
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

   private String getProductName(final Integer productNumber, final String productName, final Integer garminProductNumber) {

      String sensorProductName = UI.EMPTY_STRING;

      if (garminProductNumber != null) {
         sensorProductName = GarminProduct.getStringFromValue(garminProductNumber);
      }

      if (sensorProductName.length() == 0 && productName != null) {
         sensorProductName = productName;
      }

      if (sensorProductName.length() == 0 && productNumber != null) {
         sensorProductName = productNumber.toString();
      }

      return sensorProductName;
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

   private void logDeviceData_1_Header() {

      // print log header

// SET_FORMATTING_OFF

      System.out.println(String.format(UI.EMPTY_STRING

            + "date time"                 + LOG_SEPARATOR //$NON-NLS-1$

            + "   prod:"                  + LOG_SEPARATOR //$NON-NLS-1$
            + "manufacturer"              + LOG_SEPARATOR //$NON-NLS-1$
            + "product"                   + LOG_SEPARATOR //$NON-NLS-1$
            + "prduct name"               + LOG_SEPARATOR //$NON-NLS-1$
            + "garmin product"            + LOG_SEPARATOR //$NON-NLS-1$
            + "descriptor"                + LOG_SEPARATOR //$NON-NLS-1$

            + "   ser:"                   + LOG_SEPARATOR //$NON-NLS-1$
            + "serial no"                 + LOG_SEPARATOR //$NON-NLS-1$
            + "device index"              + LOG_SEPARATOR //$NON-NLS-1$
            + "hardware version"          + LOG_SEPARATOR //$NON-NLS-1$
            + "software version"          + LOG_SEPARATOR //$NON-NLS-1$

            + "   bat:"                   + LOG_SEPARATOR //$NON-NLS-1$
            + "battery level"             + LOG_SEPARATOR //$NON-NLS-1$
            + "battery status"            + LOG_SEPARATOR //$NON-NLS-1$
            + "battery voltage"           + LOG_SEPARATOR //$NON-NLS-1$
            + "cummulated operating time" + LOG_SEPARATOR //$NON-NLS-1$

            + "  ant:"                    + LOG_SEPARATOR //$NON-NLS-1$
            + "source type"               + LOG_SEPARATOR //$NON-NLS-1$
            + "ant network"               + LOG_SEPARATOR //$NON-NLS-1$
            + "ant device type"           + LOG_SEPARATOR //$NON-NLS-1$
            + "ant device nummer"         + LOG_SEPARATOR //$NON-NLS-1$
            + "ant plus"                  + LOG_SEPARATOR //$NON-NLS-1$
            + "ant transmision type"      + LOG_SEPARATOR //$NON-NLS-1$

            + "  ble:"                    + LOG_SEPARATOR //$NON-NLS-1$
            + "ble device type"           + LOG_SEPARATOR //$NON-NLS-1$

            + "   src:"                   + LOG_SEPARATOR //$NON-NLS-1$
            + "sensor Position"           + LOG_SEPARATOR //$NON-NLS-1$

         ));

// SET_FORMATTING_ON
   }

   private void logDeviceData_2_Content(final DeviceInfoMesg mesg) {

// SET_FORMATTING_OFF

      final DateTime       timestamp            = mesg.getTimestamp();

      final Integer        manufacturer         = mesg.getManufacturer();
      final Integer        product              = mesg.getProduct();
      final String         productName          = mesg.getProductName();
      final Integer        garminProduct        = mesg.getGarminProduct();
      final String         descriptor           = mesg.getDescriptor();

      final Short          batteryLevel         = mesg.getBatteryLevel();
      final Short          batteryStatus        = mesg.getBatteryStatus();
      final Float          batteryVoltage       = mesg.getBatteryVoltage();
      final Long           cumOperatingTime     = mesg.getCumOperatingTime();

      final Long           serialNumber         = mesg.getSerialNumber();
      final Short          deviceIndex          = mesg.getDeviceIndex();
      final Short          hardwareVersion      = mesg.getHardwareVersion();
      final Float          softwareVersion      = mesg.getSoftwareVersion();

      final AntNetwork     antNetwork           = mesg.getAntNetwork();
      final Short          antDeviceType        = mesg.getAntDeviceType();
      final Integer        antDeviceNumber      = mesg.getAntDeviceNumber();
      final Short          antplusDeviceType    = mesg.getAntplusDeviceType();
      final Short          antTransmissionType  = mesg.getAntTransmissionType();

      // (B)luetooth (L)ow (E)nergy
      final Short          bleDeviceType        = mesg.getBleDeviceType();

      final SourceType     sourceType           = mesg.getSourceType();
      final BodyLocation   sensorPosition       = mesg.getSensorPosition();

// SET_FORMATTING_ON

      if (true
//       && serialNumber != null
//       && batteryVoltage != null
//       && manufacturer != null && manufacturer == 41 // Shimano

      ) {

         final long javaTime = timestamp.getDate().getTime();

         System.out.println(String.format(UI.EMPTY_STRING

               + "%s" + LOG_SEPARATOR //                 date time                     //$NON-NLS-1$

               + "   prod:" + LOG_SEPARATOR //                                         //$NON-NLS-1$
               + FORMAT_STRING_10 + LOG_SEPARATOR //     manufacturer
               + FORMAT_STRING_10 + LOG_SEPARATOR //     product
               + FORMAT_STRING_10 + LOG_SEPARATOR //     product name
               + FORMAT_STRING_10 + LOG_SEPARATOR //     garmin product
               + FORMAT_STRING_10 + LOG_SEPARATOR //     descriptor

               + "   ser:" + LOG_SEPARATOR //                                          //$NON-NLS-1$
               + FORMAT_STRING_10 + LOG_SEPARATOR //     serial no
               + FORMAT_STRING_10 + LOG_SEPARATOR //     device index
               + FORMAT_STRING_10 + LOG_SEPARATOR //     hardware version
               + FORMAT_STRING_10 + LOG_SEPARATOR //     software version

               + "   bat:" + LOG_SEPARATOR //                                          //$NON-NLS-1$
               + FORMAT_STRING_10 + LOG_SEPARATOR //     battery level
               + FORMAT_STRING_10 + LOG_SEPARATOR //     battery status
               + FORMAT_STRING_10 + LOG_SEPARATOR //     battery voltage
               + FORMAT_STRING_10 + LOG_SEPARATOR //     accumulated operating time

               + "  ant:" + LOG_SEPARATOR //                                           //$NON-NLS-1$
               + FORMAT_STRING_10_LEFT + LOG_SEPARATOR // source type
               + FORMAT_STRING_10_LEFT + LOG_SEPARATOR // ant network
               + FORMAT_STRING_10 + LOG_SEPARATOR //     ant device type
               + FORMAT_STRING_10 + LOG_SEPARATOR //     ant device number
               + FORMAT_STRING_10 + LOG_SEPARATOR //     ant plus
               + FORMAT_STRING_10 + LOG_SEPARATOR //     ant transmission type

               + "  ble:" + LOG_SEPARATOR //                                           //$NON-NLS-1$
               + FORMAT_STRING_10 + LOG_SEPARATOR //     ble device type

               + "   src:" //                                                          //$NON-NLS-1$
               + FORMAT_STRING_10 + LOG_SEPARATOR //     sensorPosition

               ,

               DATE_TIME_FORMATTER.print(javaTime),

               // product
               removeNull(manufacturer),
               removeNull(product),
               removeNull(productName),
               removeNull(garminProduct),
               removeNull(descriptor),

               // serial
               removeNull(serialNumber),
               removeNull(deviceIndex),
               removeNull(hardwareVersion),
               removeNull_3(softwareVersion),

               // battery
               removeNull(batteryLevel),
               removeNull(batteryStatus),
               removeNull_3(batteryVoltage),
               removeNull(cumOperatingTime),

               // ant
               SourceType.getStringFromValue(sourceType),
               removeNull(antNetwork),
               removeNull(antDeviceType),
               removeNull(antDeviceNumber),
               AntplusDeviceType.getStringFromValue(antplusDeviceType),
               removeNull(antTransmissionType),

               // ble
               BleDeviceType.getStringFromValue(bleDeviceType),

               // src
               removeNull(sensorPosition)

         ));
      }
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

      /*
       * Sensor data are set only for device sensors, when the serial number is available, otherwise
       * they cannot be easily identified
       */
      final Long serialNumber = mesg.getSerialNumber();
      if (serialNumber != null) {
         setSensorData(mesg, serialNumber);
      }

      if (_isLogDeviceData) {
         logDeviceData_2_Content(mesg);
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

   private void setSensorData(final DeviceInfoMesg mesg, final Long serialNumber) {

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
//    final Short          mesgAntplusDeviceType      = mesg.getAntplusDeviceType();
//    final Short          mesgAntTransmissionType    = mesg.getAntTransmissionType();
//
//    final SourceType     mesgSourceType             = mesg.getSourceType();
//    final BodyLocation   mesgSensorPosition         = mesg.getSensorPosition();

      /*
       * Gear shifting battery
       * https://forums.garmin.com/developer/fit-sdk/f/discussion/276245/di2-battery-level
       */
      final Short          mesgBatteryLevel           = mesg.getFieldShortValue(32, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD);

// SET_FORMATTING_ON

      final String sensorSerialNumberKey = serialNumber.toString();

      /*
       * Get sensor
       */
      final Map<String, DeviceSensor> allDbSensors = TourDatabase.getAllDeviceSensors_BySerialNo();

      DeviceSensor sensor = allDbSensors.get(sensorSerialNumberKey);

      if (sensor == null) {

         // create sensor

         final String manufacturerName = getManufacturerName(mesgManufacturerNumber);
         final String productName = getProductName(mesgProductNumber, mesgProductName, mesgGarminProductNumber);

         sensor = RawDataManager.createDeviceSensor(

               mesgManufacturerNumber == null ? -1 : mesgManufacturerNumber,
               manufacturerName,

               mesgProductNumber == null ? -1 : mesgProductNumber,
               productName,

               sensorSerialNumberKey);
      }

      updateSensorNames(sensor,
            mesgManufacturerNumber,
            mesgProductNumber,
            mesgProductName,
            mesgGarminProductNumber);

      final List<DeviceSensorValue> allImportedSensorValues = fitData.getAllDeviceSensorValues();

      /*
       * Get sensor value
       */
      DeviceSensorValue sensorValue = null;
      for (final DeviceSensorValue importedSensorValue : allImportedSensorValues) {

         final DeviceSensor importedSensor = importedSensorValue.getDeviceSensor();

         if (importedSensor.getSerialNumber().equals(sensorSerialNumberKey)) {

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
    */
   private void updateSensorNames(final DeviceSensor sensor,
                                  final Integer mesgManufacturerNumber,
                                  final Integer mesgProductNumber,
                                  final String mesgProductName,
                                  final Integer mesgGarminProductNumber) {

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
         sensor.setProductName(getProductName(mesgProductNumber, mesgProductName, mesgGarminProductNumber));

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
