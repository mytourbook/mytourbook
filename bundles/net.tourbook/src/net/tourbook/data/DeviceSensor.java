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
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

/**
 */
@Entity
public class DeviceSensor {

   private static final String NL             = UI.NEW_LINE;

   public static final int     DB_LENGTH_NAME = 80;

   /**
    * Manually created marker or imported marker create a unique id to identify them, saved marker
    * are compared with the marker id
    */
   private static int          _createCounter = 0;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                sensorId       = TourDatabase.ENTITY_IS_NOT_SAVED;

   private int                 manufacturerNumber;
   private String              manufacturerName;

   private int                 productNumber;
   private String              productName;

   private String              serialNumber   = UI.EMPTY_STRING;

   @Transient
   private long                _createId      = 0;

   /**
    * Default constructor used in EJB
    */
   public DeviceSensor() {}

   public DeviceSensor(final int manufacturerNumber,
                       final String manufacturerName,

                       final int productNumber,
                       final String productName,

                       final String serialNumber) {

      _createId = ++_createCounter;

      this.manufacturerNumber = manufacturerNumber;
      this.manufacturerName = manufacturerName;

      this.productNumber = productNumber;
      this.productName = productName;

      this.serialNumber = serialNumber;
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof DeviceSensor)) {
         return false;
      }

      final DeviceSensor other = (DeviceSensor) obj;

      if (_createId == 0) {

         // sensor is from the database
         if (sensorId != other.sensorId) {
            return false;
         }

      } else {

         // sensor is create
         if (_createId != other._createId) {
            return false;
         }
      }

      return true;
   }

   public boolean equals1(final Object obj) {

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

   public String getManufacturerName() {
      return manufacturerName;
   }

   public int getManufacturerNumber() {
      return manufacturerNumber;
   }

   public String getProductName() {
      return productName;
   }

   public int getProductNumber() {
      return productNumber;
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

      return Objects.hash(sensorId, _createId);
   }

   public void setSerialNumber(final String serialNumber) {
      this.serialNumber = serialNumber;
   }

   @Override
   public String toString() {

      return "DeviceSensor" + NL //                                     //$NON-NLS-1$

            + "[" + NL //                                               //$NON-NLS-1$

            + "sensorId             = " + sensorId + NL //              //$NON-NLS-1$
            + "manufacturerNumber   = " + manufacturerNumber + NL //    //$NON-NLS-1$
            + "manufacturerName     = " + manufacturerName + NL //      //$NON-NLS-1$
            + "productNumber        = " + productNumber + NL //         //$NON-NLS-1$
            + "productName          = " + productName + NL //           //$NON-NLS-1$
            + "serialNumber         = " + serialNumber + NL //          //$NON-NLS-1$

            + "]" + NL; //                                              //$NON-NLS-1$
   }
}
