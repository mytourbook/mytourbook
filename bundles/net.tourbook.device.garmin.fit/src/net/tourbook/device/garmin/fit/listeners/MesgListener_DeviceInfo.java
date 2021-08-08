/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
import com.garmin.fit.BodyLocation;
import com.garmin.fit.DateTime;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DeviceInfoMesgListener;
import com.garmin.fit.SourceType;

import net.tourbook.common.UI;
import net.tourbook.device.garmin.fit.FitData;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class MesgListener_DeviceInfo extends AbstractMesgListener implements DeviceInfoMesgListener {

   private static final String            FORMAT_FLOAT_10_3 = "%10.3f";                                         //$NON-NLS-1$

   private static final DateTimeFormatter _dtFormatter      = DateTimeFormat.forPattern("yyyy-MM-dd HH-mm-ss"); //$NON-NLS-1$

   public MesgListener_DeviceInfo(final FitData fitData) {
      super(fitData);
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

   @Override
   public void onMesg(final DeviceInfoMesg mesg) {

      final Short deviceType = mesg.getDeviceType();

      final boolean isLogDeviceData = false;

      if (isLogDeviceData) {

         final DateTime timestamp = mesg.getTimestamp();

         final Integer manufacturer = mesg.getManufacturer();
         final Integer product = mesg.getProduct();
         final String productName = mesg.getProductName();
         final Integer garminProduct = mesg.getGarminProduct();
         final String descriptor = mesg.getDescriptor();

         final Short batteryStatus = mesg.getBatteryStatus();
         final Float batteryVoltage = mesg.getBatteryVoltage();
         final Long cumOperatingTime = mesg.getCumOperatingTime();

         final Long serialNumber = mesg.getSerialNumber();
         final Short deviceIndex = mesg.getDeviceIndex();
         final Short hardwareVersion = mesg.getHardwareVersion();
         final Float softwareVersion = mesg.getSoftwareVersion();

         final AntNetwork antNetwork = mesg.getAntNetwork();
         final Short antDeviceType = mesg.getAntDeviceType();
         final Integer antDeviceNumber = mesg.getAntDeviceNumber();
         final Short antplusDeviceType = mesg.getAntplusDeviceType();
         final Short antTransmissionType = mesg.getAntTransmissionType();

         final SourceType sourceType = mesg.getSourceType();
         final BodyLocation sensorPosition = mesg.getSensorPosition();

         if (true

//            && serialNumber != null

               && batteryVoltage != null

         ) {

            final long javaTime = (timestamp.getTimestamp() * 1000) + com.garmin.fit.DateTime.OFFSET;

            final String separator = "\t";

            System.out.println(String.format(UI.EMPTY_STRING

                  + "%s" + separator //         date time

                  + "   prod:" + separator
                  + " %10s" + separator //      manufacturer
                  + " %10s" + separator //      product
                  + " %10s" + separator //      prduct name
                  + " %10s" + separator //      garmin product
                  + " %10s" + separator //      descriptor

                  + "   ser:" + separator
                  + " %10s" + separator //      serial no
                  + " %10s" + separator //      device index
                  + " %10s" + separator //      hardware version
                  + " %10s" + separator //      software version

                  + "   bat:" + separator
                  + " %10s" + separator //      battery status
                  + " %10s" + separator //      battery voltage
                  + " %10s" + separator //      cummulated operating time

                  + "  ant:" + separator
                  + " %10s" + separator //      source type
                  + " %10s" + separator //      ant network
                  + " %10s" + separator //      ant device type
                  + " %10s" + separator //      ant device nummer
                  + " %10s" + separator //      ant plus
                  + " %10s" + separator //      ant transmision type

//               + "   src:"
//               + " %10s"

                  ,

                  _dtFormatter.print(javaTime),

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
                  removeNull(batteryStatus),
                  removeNull_3(batteryVoltage),
                  removeNull(cumOperatingTime),

                  // ant
                  sourceType,
                  removeNull(antNetwork),
                  removeNull(antDeviceType),
                  removeNull(antDeviceNumber),
                  removeNull(antplusDeviceType),
                  removeNull(antTransmissionType)

            // src
//               sensorPosition

            //
            ));
         }
      }

      if (deviceType != null) {

         final boolean hasSpeedSensor = hasSpeedSensor(deviceType);
         final boolean hasHeartRateSensor = hasHeartRateSensor(deviceType);
         final boolean hasPowerSensor = hasPowerSensor(deviceType);
         final boolean hasStrideSensor = hasStrideSensor(deviceType);

         /*
          * This event occures several times and can set a true to false, therefore only true is
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

//      if (deviceType != null || antDeviceType != null || antplusDeviceType != null) {
//
//         System.out.println(String.format("\n"//
//               + "DeviceInfoMesg" //
//               + "\tdev %d" //
//               + "\tantDev %d" //
//               + "\tantplusDev %d"
//               + "\n", //$NON-NLS-1$
////
//               deviceType,
//               antDeviceType,
//               antplusDeviceType
//         //
//               ));
//      }
   }

   private String removeNull(final AntNetwork value) {

      return value == null
            ? UI.EMPTY_STRING
            : value.toString();
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

   private String removeNull(final Short value) {

      return value == null
            ? UI.EMPTY_STRING
            : value.toString();
   }

   private String removeNull(final String value) {

      return value == null
            ? UI.EMPTY_STRING
            : value.toString();
   }

   private String removeNull_3(final Float value) {

      return value == null
            ? UI.EMPTY_STRING
            : String.format(FORMAT_FLOAT_10_3, value);
   }

}
