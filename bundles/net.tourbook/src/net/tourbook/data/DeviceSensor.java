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

import net.tourbook.ui.UI;

/**
 */
@Entity
public class DeviceSensor {

   private static final String NL              = UI.NEW_LINE;

   public static final int     DB_LENGTH_LABEL = 80;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                sensorId;

   private String              label           = UI.EMPTY_STRING;

   private String              serialNumber    = UI.EMPTY_STRING;

   public DeviceSensor() {}

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

      final DeviceSensor other = (DeviceSensor) obj;

      return sensorId == other.sensorId;
   }

   /**
    * @return Returns the name for this sensor
    */
   public String getLabel() {
      return label;
   }

   /**
    * @return Returns the primary key for a {@link DeviceSensor} entity
    */
   public long getSensorId() {
      return sensorId;
   }

   public String getSerialNumber() {
      return serialNumber;
   }

   @Override
   public int hashCode() {
      return Objects.hash(sensorId);
   }

   public void setLabel(final String label) {
      this.label = label;
   }

   public void setSerialNumber(final String serialNumber) {
      this.serialNumber = serialNumber;
   }

   @Override
   public String toString() {

      return "DeviceSensor" + NL //$NON-NLS-1$

            + "[" + NL //                             //$NON-NLS-1$

            + "label       = " + label + NL //        //$NON-NLS-1$
            + "sensorId    = " + sensorId + NL //     //$NON-NLS-1$
//            + "tourData    = " + tourData + NL //     //$NON-NLS-1$

            + "]" + NL; //                            //$NON-NLS-1$
   }
}
