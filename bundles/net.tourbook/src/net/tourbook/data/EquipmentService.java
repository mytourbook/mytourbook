/*******************************************************************************
 * Copyright (C) 2025, 2026 Wolfgang Schramm and Contributors
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
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;

@Entity
public class EquipmentService implements Cloneable, Serializable {

   private static final char          NL               = UI.NEW_LINE;

   private static final long          serialVersionUID = 1L;

   private static final AtomicInteger _createCounter   = new AtomicInteger();

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       serviceId        = TourDatabase.ENTITY_IS_NOT_SAVED;

   /**
    * Name for the service
    */
   private String                     name;

   /**
    * Company which did the service
    */
   private String                     company;

   /**
    * e.g. Jahresservice
    */
   private String                     type;

   /**
    * Description/notes for the service
    */
   private String                     description;

   /**
    * When <code>true</code> then this service is included in collated services
    */
   private boolean                    isCollate        = true;

   /**
    * When the service was done, in milliseconds since 1970-01-01T00:00:00Z
    */
   private long                       date;

   /**
    * When the service was finished, in milliseconds since 1970-01-01T00:00:00Z.
    */
   private long                       dateUntil;

   /**
    * Price
    */
   private float                      price;

   /**
    * Price unit
    */
   private String                     priceUnit;

   /**
    * One equipment can have multiple services
    */
   @ManyToOne(optional = false)
   private Equipment                  equipment;

   @Transient
   private long                       _createId        = 0;

   @Transient
   private LocalDateTime              _date;

   @Transient
   private LocalDateTime              _dateUntil;

   /**
    * Default constructor used in EJB
    */
   public EquipmentService() {}

   @Override
   public EquipmentService clone() {

      EquipmentService clonedService = null;

      try {

         clonedService = (EquipmentService) super.clone();

      } catch (final CloneNotSupportedException e) {

         e.printStackTrace();
      }

      clonedService.serviceId = TourDatabase.ENTITY_IS_NOT_SAVED;
      clonedService._createId = _createCounter.incrementAndGet();

      return clonedService;
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof EquipmentService)) {
         return false;
      }

      final EquipmentService other = (EquipmentService) obj;

      if (_createId == 0) {

         // equipment is from the database
         if (serviceId != other.serviceId) {
            return false;
         }

      } else {

         // equipment is created
         if (_createId != other._createId) {
            return false;
         }
      }

      return true;
   }

   public String getCompany() {

      if (company == null) {
         return UI.EMPTY_STRING;
      }

      return company;
   }

   public long getDate() {
      return date;
   }

   public LocalDateTime getDate_Local() {

      if (_date == null) {
         _date = TimeTools.toLocalDateTime(date);
      }

      return _date;
   }

   public long getDateUntil() {
      return dateUntil;
   }

   public LocalDateTime getDateUntil_Local() {

      if (_dateUntil == null) {
         _dateUntil = TimeTools.toLocalDateTime(dateUntil);
      }

      return _dateUntil;
   }

   public String getDescription() {

      if (description == null) {
         return UI.EMPTY_STRING;
      }

      return description;
   }

   public long getDuration() {

      final long duration = dateUntil - date;

      return duration;
   }

   public Equipment getEquipment() {
      return equipment;
   }

   /**
    * @return Returns the service name or an empty string when not available
    */
   public String getName() {

      if (name == null) {
         return UI.EMPTY_STRING;
      }

      return name;
   }

   public float getPrice() {
      return price;
   }

   public String getPriceUnit() {
      return priceUnit;
   }

   /**
    * @return Returns the primary key for a {@link EquipmentService} entity
    */
   public long getServiceId() {
      return serviceId;
   }

   public String getType() {

      if (type == null) {
         return UI.EMPTY_STRING;
      }

      return type;
   }

   @Override
   public int hashCode() {

      return Objects.hash(serviceId, _createId);
   }

   public boolean isCollate() {
      return isCollate;
   }

   public boolean isCollatedFieldsModified(final EquipmentService otherService) {

      if (isCollate != otherService.isCollate()
            || date != otherService.getDate()
            || type.equalsIgnoreCase(otherService.getType()) == false) {

         // collated fields are modified

         return true;
      }

      return false;
   }

   /**
    * Checks if VARCHAR fields have the correct length
    *
    * @return Returns <code>true</code> when the data are valid and can be saved
    */
   public boolean isValidForSave() {

      FIELD_VALIDATION fieldValidation;

      /*
       * Check: Description
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            description,
            TourDatabase.DB_LENGTH_DESCRIPTION,
            Messages.Db_Field_Description);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {

         return false;

      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {

         description = description.substring(0, TourDatabase.DB_LENGTH_DESCRIPTION);
      }

      /*
       * Check: Name
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            name,
            TourDatabase.DB_LENGTH_NAME,
            Messages.Db_Field_Name);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {

         return false;

      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {

         name = name.substring(0, TourDatabase.DB_LENGTH_NAME);
      }

      return true;
   }

   public void setCompany(final String company) {
      this.company = company;
   }

   public void setDate(final long date) {

      this.date = date;

      _date = null;
   }

   public void setDateUntil(final long dateUntil) {

      this.dateUntil = dateUntil;

      _dateUntil = null;
   }

   public void setDescription(final String description) {
      this.description = description;
   }

   public void setEquipment(final Equipment partEquipment) {

      equipment = partEquipment;
   }

   public void setIsCollate(final boolean isCollate) {
      this.isCollate = isCollate;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public void setPrice(final float price) {
      this.price = price;
   }

   public void setPriceUnit(final String priceUnit) {
      this.priceUnit = priceUnit;
   }

   public void setType(final String type) {
      this.type = type;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "EquipmentService" + NL //                             //$NON-NLS-1$

            + " serviceId   = " + serviceId + NL //                   //$NON-NLS-1$
            + " name        = " + name + NL //                        //$NON-NLS-1$
            + " company     = " + company + NL //                     //$NON-NLS-1$
      ;
   }

   public void updateFromOther(final EquipmentService otherService) {

// SET_FORMATTING_OFF

      setCompany        (otherService.getCompany());
      setDate           (otherService.getDate());
      setDescription    (otherService.getDescription());
      setIsCollate      (otherService.isCollate());
      setName           (otherService.getName());
      setPrice          (otherService.getPrice());
      setPriceUnit      (otherService.getPriceUnit());
      setType           (otherService.getType());

// SET_FORMATTING_ON
   }

   /**
    * Reset {@link #dateUntil} when service is not collated, this makes it easier to see it in the
    * view
    */
   public void updateUntilDate() {

      if (isCollate == false) {

         setDateUntil(0);
      }
   }
}
