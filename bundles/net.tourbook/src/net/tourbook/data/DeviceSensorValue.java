/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.data;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

/**
 */
@Entity
public class DeviceSensorValue {

   private static final String  NL                            = UI.NEW_LINE;

   /**
    * Create a unique id to identify imported sensor values
    */
   private static AtomicInteger _createCounter                = new AtomicInteger();

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                 sensorValueId                 = TourDatabase.ENTITY_IS_NOT_SAVED;

   private long                 tourStartTime;
   private long                 tourEndTime;

   private float                batteryVoltage_Start          = -1;
   private float                batteryVoltage_End            = -1;

   private long                 cummulatedOperatingTime_Start = -1;
   private long                 cummulatedOperatingTime_End   = -1;

   @ManyToOne(optional = false)
   private DeviceSensor         deviceSensor;

   @ManyToOne(optional = false)
   private TourData             tourData;

   /**
    * Is used to identify a device by it's device index according to the FIT "device index" field
    */
   @Transient
   private int                  _deviceIndex                  = -1;

   @Transient
   private long                 _createId                     = 0;

   public DeviceSensorValue() {}

   public DeviceSensorValue(final DeviceSensor sensor) {

      _createId = _createCounter.incrementAndGet();

      deviceSensor = sensor;
   }

   /**
    * Ensure that the start value is larger than the end value, it happend that they are reverted.
    */
   private void checkBatteryVoltage_StartEnd() {

      final float start = batteryVoltage_Start;
      final float end = batteryVoltage_End;

      if (start == -1 || end == -1) {
         return;
      }

      if (end > start) {

         batteryVoltage_Start = end;
         batteryVoltage_End = start;
      }
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (getClass() != obj.getClass()) {
         return false;
      }

      final DeviceSensorValue other = (DeviceSensorValue) obj;

      if (_createId == 0) {

         // sensor value is from the database
         if (sensorValueId != other.sensorValueId) {
            return false;
         }

      } else {

         // sensor value is create
         if (_createId != other._createId) {
            return false;
         }
      }

      return true;
   }

   public float getBatteryVoltage_End() {
      return batteryVoltage_End;
   }

   public float getBatteryVoltage_Start() {
      return batteryVoltage_Start;
   }

   public long getCummulatedOperatingTime_End() {
      return cummulatedOperatingTime_End;
   }

   public long getCummulatedOperatingTime_Start() {
      return cummulatedOperatingTime_Start;
   }

   /**
    * @return Returns the device index or -1 when the index is not yet set
    */
   public int getDeviceIndex() {
      return _deviceIndex;
   }

   public DeviceSensor getDeviceSensor() {
      return deviceSensor;
   }

   /**
    * @return Returns the primary key for a {@link DeviceSensorValue} entity
    */
   public long getSensorValueId() {
      return sensorValueId;
   }

   public long getTourEndTime() {
      return tourEndTime;
   }

   public long getTourStartTime() {
      return tourStartTime;
   }

   @Override
   public int hashCode() {
      return Objects.hash(sensorValueId);
   }

   public void setBatteryVoltage_End(final float batteryVoltage_End) {

      this.batteryVoltage_End = batteryVoltage_End;

      checkBatteryVoltage_StartEnd();
   }

   public void setBatteryVoltage_Start(final float batteryVoltage_Start) {

      this.batteryVoltage_Start = batteryVoltage_Start;

      checkBatteryVoltage_StartEnd();
   }

   public void setCummulatedOperatingTime_End(final long cummulatedOperatingTime_End) {
      this.cummulatedOperatingTime_End = cummulatedOperatingTime_End;
   }

   public void setCummulatedOperatingTime_Start(final long cummulatedOperatingTime_Start) {
      this.cummulatedOperatingTime_Start = cummulatedOperatingTime_Start;
   }

   public void setDeviceIndex(final int _deviceIndex) {
      this._deviceIndex = _deviceIndex;
   }

   public void setDeviceSensor(final DeviceSensor deviceSensor) {
      this.deviceSensor = deviceSensor;
   }

   public void setTourData(final TourData tourData) {
      this.tourData = tourData;
   }

   public void setTourEndTime(final long tourEndTime) {
      this.tourEndTime = tourEndTime;
   }

   public void setTourStartTime(final long tourStartTime) {
      this.tourStartTime = tourStartTime;
   }

   /**
    * This method is called in the MT UI in the "Tour Data" view
    */
   @Override
   public String toString() {

      return "DeviceSensorValue" + NL //                                                     //$NON-NLS-1$

//            + "[" + NL //                                                                    //$NON-NLS-1$

            + "   sensorValueId                 = " + sensorValueId + NL //                     //$NON-NLS-1$
            + "   batteryVoltage_Start          = " + batteryVoltage_Start + NL //              //$NON-NLS-1$
            + "   batteryVoltage_End            = " + batteryVoltage_End + NL //                //$NON-NLS-1$
            + "   cummulatedOperatingTime_Start = " + cummulatedOperatingTime_Start + NL //     //$NON-NLS-1$
            + "   cummulatedOperatingTime_End   = " + cummulatedOperatingTime_End + NL //       //$NON-NLS-1$
            + "   deviceSensor                  = " + deviceSensor + NL //                      //$NON-NLS-1$
//            + "   tourData                      = " + tourData + NL //                          //$NON-NLS-1$

//            + "]" + NL //                                                                   //$NON-NLS-1$
      ;
   }
}
