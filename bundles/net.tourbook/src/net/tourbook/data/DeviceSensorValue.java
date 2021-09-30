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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.ui.UI;

/**
 */
@Entity
public class DeviceSensorValue {

   private static final String NL           = UI.NEW_LINE;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                sensorValueId;

   private long                tourStartTime;

   private float               batteryVoltage_Start;
   private float               batteryVoltage_End;

   private long                cummulatedOperatingTime_Start;
   private long                cummulatedOperatingTime_End;

   @ManyToOne(optional = false)
   private DeviceSensor        deviceSensor;

   @ManyToOne(optional = false)
   private TourData            tourData;

   /**
    * Is used to identify a device by it's device index according to the FIT "device index" field
    */
   @Transient
   private int                 _deviceIndex = -1;

   public DeviceSensorValue() {}

   public DeviceSensorValue(final DeviceSensor sensor) {

      deviceSensor = sensor;
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

      return sensorValueId == other.sensorValueId;
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

   public long getTourStartTime() {
      return tourStartTime;
   }

   @Override
   public int hashCode() {
      return Objects.hash(sensorValueId);
   }

   public void setBatteryVoltage_End(final float batteryVoltage_End) {
      this.batteryVoltage_End = batteryVoltage_End;
   }

   public void setBatteryVoltage_Start(final float batteryVoltage_Start) {
      this.batteryVoltage_Start = batteryVoltage_Start;
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

   public void setTourStartTime(final long tourStartTime) {
      this.tourStartTime = tourStartTime;
   }

   @Override
   public String toString() {

      return "DeviceSensorValue" + NL //                                                     //$NON-NLS-1$

            + "[" + NL //                                                                    //$NON-NLS-1$

            + "sensorId                      = " + sensorValueId + NL //                     //$NON-NLS-1$
            + "batteryVoltage_Start          = " + batteryVoltage_Start + NL //              //$NON-NLS-1$
            + "batteryVoltage_End            = " + batteryVoltage_End + NL //                //$NON-NLS-1$
            + "cummulatedOperatingTime_Start = " + cummulatedOperatingTime_Start + NL //     //$NON-NLS-1$
            + "cummulatedOperatingTime_End   = " + cummulatedOperatingTime_End + NL //       //$NON-NLS-1$
            + "deviceSensor                  = " + deviceSensor + NL //                      //$NON-NLS-1$
            + "tourData                      = " + tourData + NL //                          //$NON-NLS-1$

            + "]" + NL; //                                                                   //$NON-NLS-1$
   }
}
