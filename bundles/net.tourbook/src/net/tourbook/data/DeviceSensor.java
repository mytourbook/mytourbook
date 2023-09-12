/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.views.sensors.SensorManager;

/**
 */
@Entity
public class DeviceSensor implements Cloneable, Serializable {

   private static final long    serialVersionUID      = 1L;

   private static final char    NL                    = UI.NEW_LINE;

   public static final int      DB_LENGTH_NAME        = 80;
   public static final int      DB_LENGTH_DESCRIPTION = 32000;

   /**
    * Create a unique id to identify imported sensors
    */
   private static AtomicInteger _createCounter        = new AtomicInteger();

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                 sensorId              = TourDatabase.ENTITY_IS_NOT_SAVED;

   /**
    * Contains a customer name because the manufacturer and product name are sometimes cryptic
    */
   private String               sensorName;

   /**
    * Description for the sensor
    */
   private String               description;

   /**
    * The type is displayed in the sensor chart or tour info
    */
   @Enumerated(EnumType.STRING)
   private DeviceSensorType     sensorType            = DeviceSensorType.NONE;

   private String               manufacturerName;
   private int                  manufacturerNumber;

   private String               productName;
   private int                  productNumber;

   private String               serialNumber          = UI.EMPTY_STRING;

   /**
    * Serial number as long value, when parsing fails of {@link #serialNumber}, then
    * {@link Long#MIN_VALUE} is set.
    */
   @Transient
   private long                 _serialNumberLong     = Long.MAX_VALUE;

   /**
    * Time in ms when this sensor was first used
    */
   @Transient
   private long                 usedStartTime;

   /**
    * Time in ms when this sensor was last used
    */
   @Transient
   private long                 usedEndTime;

   @Transient
   private long                 _createId             = 0;

   /**
    * Sensor name which is containg info about type, manufacturer or product
    */
   @Transient
   private String               _label;

   /**
    * Default constructor used in EJB
    */
   public DeviceSensor() {}

   public DeviceSensor(final int manufacturerNumber,
                       final String manufacturerName,

                       final int productNumber,
                       final String productName,

                       final String serialNumber) {

      _createId = _createCounter.incrementAndGet();

      this.manufacturerNumber = manufacturerNumber;
      this.manufacturerName = manufacturerName;

      this.productNumber = productNumber;
      this.productName = productName;

      this.serialNumber = serialNumber;
   }

   @Override
   public DeviceSensor clone() {

      DeviceSensor newSensor = null;

      try {
         newSensor = (DeviceSensor) super.clone();
      } catch (final CloneNotSupportedException e) {
         e.printStackTrace();
      }

      return newSensor;
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

   public String getDescription() {

      if (description == null) {
         return UI.EMPTY_STRING;
      }

      return description;
   }

   /**
    * @return Returns a name which is containg info about the sensor type, manufacturer or product
    */
   public String getLabel() {

      if (_label != null) {
         return _label;
      }

      updateSensorLabel();

      return _label;
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

   /**
    * @return Returns the sensor custom name or an empty string when not available
    */
   public String getSensorName() {

      if (sensorName == null) {
         return UI.EMPTY_STRING;
      }

      return sensorName;
   }

   public DeviceSensorType getSensorType() {
      return sensorType;
   }

   public String getSerialNumber() {
      return serialNumber;
   }

   /**
    * @return Returns serial number as long value or {@link Long#MIN_VALUE} when parsing as long
    *         fails.
    */
   public long getSerialNumberAsLong() {

      if (_serialNumberLong != Long.MAX_VALUE) {
         return _serialNumberLong;
      }

      try {

         _serialNumberLong = Long.parseLong(serialNumber);

      } catch (final NumberFormatException e) {
         _serialNumberLong = Long.MIN_VALUE;
      }

      return _serialNumberLong;
   }

   public long getUsedEndTime() {
      return usedEndTime;
   }

   public long getUsedStartTime() {
      return usedStartTime;
   }

   @Override
   public int hashCode() {

      return Objects.hash(sensorId, _createId);
   }

   /**
    * Checks if VARCHAR fields have the correct length
    *
    * @return Returns <code>true</code> when the data are valid and can be saved
    */
   public boolean isValidForSave() {

      FIELD_VALIDATION fieldValidation;

      /*
       * Check: Name
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            sensorName,
            DB_LENGTH_NAME,
            Messages.Db_Field_SensorName);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         sensorName = sensorName.substring(0, DB_LENGTH_NAME);
      }

      /*
       * Check: Description
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            description,
            DB_LENGTH_DESCRIPTION,
            Messages.Db_Field_SensorDescription);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         description = description.substring(0, DB_LENGTH_DESCRIPTION);
      }

      return true;
   }

   public void setDescription(final String description) {
      this.description = description;
   }

   public void setManufacturerName(final String manufacturerName) {

      this.manufacturerName = manufacturerName;

      updateSensorLabel();
   }

   public void setManufacturerNumber(final int manufacturerNumber) {
      this.manufacturerNumber = manufacturerNumber;
   }

   public void setProductName(final String productName) {

      this.productName = productName;

      updateSensorLabel();
   }

   public void setProductNumber(final int productNumber) {
      this.productNumber = productNumber;
   }

   public void setSensorName(final String label) {

      this.sensorName = label;

      updateSensorLabel();
   }

   public void setSensorType(final DeviceSensorType sensorType) {

      this.sensorType = sensorType;

      updateSensorLabel();
   }

   public void setSerialNumber(final String serialNumber) {
      this.serialNumber = serialNumber;
   }

   public void setUsedEndTime(final long usedEndTime) {
      this.usedEndTime = usedEndTime;
   }

   public void setUsedStartTime(final long usedStartTime) {
      this.usedStartTime = usedStartTime;
   }

   /**
    * This method is called in the MT UI in the "Tour Data" view
    */
   @Override
   public String toString() {

      return "DeviceSensor" + NL //                                     //$NON-NLS-1$

//            + "[" + NL //                                               //$NON-NLS-1$

            + "      sensorName           = " + sensorName + NL //            //$NON-NLS-1$
            + "      sensorId             = " + sensorId + NL //              //$NON-NLS-1$
            + "      manufacturerNumber   = " + manufacturerNumber + NL //    //$NON-NLS-1$
            + "      manufacturerName     = " + manufacturerName + NL //      //$NON-NLS-1$
            + "      productNumber        = " + productNumber + NL //         //$NON-NLS-1$
            + "      productName          = " + productName + NL //           //$NON-NLS-1$
            + "      serialNumber         = " + serialNumber + NL //          //$NON-NLS-1$
            + "      _label               = " + getLabel() + NL //          //$NON-NLS-1$

//            + "]" + NL //                                              //$NON-NLS-1$
      ;
   }

   /**
    * Updates values from a modified {@link DeviceSensor}
    *
    * @param modifiedSensor
    */
   public void updateFromModified(final DeviceSensor modifiedSensor) {

      sensorName = modifiedSensor.sensorName;
      description = modifiedSensor.description;

      sensorType = modifiedSensor.sensorType;
   }

   private void updateSensorLabel() {

      String sensorTypeName = SensorManager.getSensorTypeName(sensorType);
      sensorTypeName = sensorTypeName.length() == 0
            ? sensorTypeName
            : UI.DASH_WITH_SPACE + sensorTypeName;

      final String sensorCustomName = getSensorName();
      final String productManufacturerName = manufacturerName + UI.DASH_WITH_SPACE + productName;

      if (sensorCustomName.length() > 0) {

         _label = sensorCustomName + sensorTypeName;

      } else {

         _label = productManufacturerName + sensorTypeName;
      }
   }
}
