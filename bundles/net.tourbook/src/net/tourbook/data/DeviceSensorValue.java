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

import net.tourbook.ui.UI;

/**
 */
@Entity
public class DeviceSensorValue {

   private static final String NL = UI.NEW_LINE;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                sensorValueId;

   private long                tourStartTime;

   private float               batteryVoltage_Start;
   private float               batteryVoltage_End;

   @ManyToOne(optional = false)
   private DeviceSensor        deviceSensor;

   @ManyToOne(optional = false)
   private TourData            tourData;

   public DeviceSensorValue() {}

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

   public void setTourStartTime(final long tourStartTime) {
      this.tourStartTime = tourStartTime;
   }

   @Override
   public String toString() {

      return "DeviceSensorValue" + NL //                //$NON-NLS-1$

            + "[" + NL //                                //$NON-NLS-1$

            + "sensorId    = " + sensorValueId + NL //   //$NON-NLS-1$
            + "tourData    = " + tourData + NL //        //$NON-NLS-1$

            + "]" + NL; //                               //$NON-NLS-1$
   }
}
