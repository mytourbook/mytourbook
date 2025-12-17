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
import net.tourbook.common.util.StringUtils;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;

@Entity
public class EquipmentPart implements Cloneable, Comparable<Object>, Serializable {

   private static final char          NL               = UI.NEW_LINE;

   private static final long          serialVersionUID = 1L;

   private static final AtomicInteger _createCounter   = new AtomicInteger();

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       partId           = TourDatabase.ENTITY_IS_NOT_SAVED;

   /**
    * Brand/name for the equipment, e.g. Continental
    */
   private String                     brand;

   /**
    * Model/subname for the equipment, e.g. Grand Prix 4000 S II
    */
   private String                     model;

   /**
    * e.g. Faltreifen
    */
   private String                     type;

   /**
    * e.g. 700*22-23
    */
   private String                     size;

   /**
    * Description/notes for the equipment
    */
   private String                     description;

   /**
    * Website
    */
   private String                     urlAddress;

   /**
    * When <code>true</code> then this part is included in collated parts
    */
   private boolean                    isCollate        = true;

   /**
    * When the part was firstly used, in milliseconds since 1970-01-01T00:00:00Z
    */
   private long                       date;

   /**
    * When the part was created/build, in milliseconds since 1970-01-01T00:00:00Z
    */
   private long                       dateBuilt;

   /**
    * When the part was retired/sold, in milliseconds since 1970-01-01T00:00:00Z
    */
   private long                       dateRetired;

   /**
    * When the part usage was finished, in milliseconds since 1970-01-01T00:00:00Z.
    */
   private long                       dateUntil;

   /**
    * Weight of the equipment, in kg
    */
   private float                      weight;

   /**
    * Price
    */
   private float                      price;

   /**
    * Price unit
    */
   private String                     priceUnit;

   /**
    * Initial distance, in meter
    */
   private float                      distanceFirstUse;

   /** One equipment can have multiple parts */
   @ManyToOne(optional = false)
   private Equipment                  equipment;

   @Transient
   private long                       _createId        = 0;

   @Transient
   private LocalDateTime              _date;

   @Transient
   private LocalDateTime              _dateBuilt;

   @Transient
   private LocalDateTime              _dateRetired;

   @Transient
   private LocalDateTime              _dateUntil;

   @Transient
   private String                     _partName;

   /**
    * Default constructor used in EJB
    */
   public EquipmentPart() {}

   @Override
   public EquipmentPart clone() {

      EquipmentPart clonedPart = null;

      try {

         clonedPart = (EquipmentPart) super.clone();

      } catch (final CloneNotSupportedException e) {

         e.printStackTrace();
      }

      clonedPart.partId = TourDatabase.ENTITY_IS_NOT_SAVED;
      clonedPart._createId = _createCounter.incrementAndGet();

      return clonedPart;
   }

   @Override
   public int compareTo(final Object obj) {

      if (obj instanceof final EquipmentPart part) {

         return getName().compareTo(part.getName());
      }

      return 0;
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof EquipmentPart)) {
         return false;
      }

      final EquipmentPart other = (EquipmentPart) obj;

      if (_createId == 0) {

         // equipment is from the database
         if (partId != other.partId) {
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

   /**
    * @return Returns the equipment name or an empty string when not available
    */
   public String getBrand() {

      if (brand == null) {
         return UI.EMPTY_STRING;
      }

      return brand;
   }

   public long getDate() {

      return date;
   }

   /**
    * @return Return the first use date
    */
   public LocalDateTime getDate_Local() {

      if (_date == null) {
         _date = TimeTools.toLocalDateTime(date);
      }

      return _date;
   }

   public long getDateBuilt() {

      return dateBuilt;
   }

   public LocalDateTime getDateBuilt_Local() {

      if (_dateBuilt == null) {
         _dateBuilt = TimeTools.toLocalDateTime(dateBuilt);
      }

      return _dateBuilt;
   }

   public long getDateRetired() {

      return dateRetired;
   }

   public LocalDateTime getDateRetired_Local() {

      if (_dateRetired == null) {
         _dateRetired = TimeTools.toLocalDateTime(dateRetired);
      }

      return _dateRetired;
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

   public float getDistanceFirstUse() {
      return distanceFirstUse;
   }

   public long getDuration() {

      final long duration = dateUntil - date;

      return duration;
   }

   public Equipment getEquipment() {
      return equipment;
   }

   public String getModel() {

      if (model == null) {
         return UI.EMPTY_STRING;
      }

      return model;
   }

   /**
    * @return Returns a combined name of the equipment with "brand - model"
    */
   public String getName() {

      if (_partName != null) {

         return _partName;
      }

      final StringBuilder sb = new StringBuilder();

      if (StringUtils.hasContent(brand)) {
         sb.append(brand);
      }

      if (StringUtils.hasContent(model)) {

         if (sb.length() > 0) {
            sb.append(UI.DASH_WITH_DOUBLE_SPACE);
         }

         sb.append(model);
      }

      _partName = sb.toString();

      return _partName;
   }

   /**
    * @return Returns the primary key for a {@link EquipmentPart} entity
    */
   public long getPartId() {
      return partId;
   }

   public float getPrice() {
      return price;
   }

   public String getPriceUnit() {
      return priceUnit;
   }

   public String getSize() {

      if (size == null) {
         return UI.EMPTY_STRING;
      }

      return size;
   }

   public String getType() {

      if (type == null) {
         return UI.EMPTY_STRING;
      }

      return type;
   }

   public String getUrlAddress() {

      if (urlAddress == null) {
         return UI.EMPTY_STRING;
      }

      return urlAddress;
   }

   public float getWeight() {
      return weight;
   }

   @Override
   public int hashCode() {

      return Objects.hash(partId, _createId);
   }

   public boolean isCollate() {
      return isCollate;
   }

   public boolean isCollatedFieldsModified(final EquipmentPart otherPart) {

      if (isCollate != otherPart.isCollate()
            || date != otherPart.getDate()
            || type.equalsIgnoreCase(otherPart.getType()) == false) {

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

      return true;
   }

   public void resetName() {

      _partName = null;
   }

   public void setBrand(final String brand) {

      this.brand = brand;

      _partName = null;
   }

   public void setDate(final long date) {

      this.date = date;

      _date = null;
   }

   public void setDateBuilt(final long dateBuilt) {

      this.dateBuilt = dateBuilt;

      _dateBuilt = null;
   }

   public void setDateRetired(final long dateRetired) {

      this.dateRetired = dateRetired;

      _dateRetired = null;
   }

   public void setDateUntil(final long dateUntil) {

      this.dateUntil = dateUntil;

      _dateUntil = null;
   }

   public void setDescription(final String description) {
      this.description = description;
   }

   public void setDistanceFirstUse(final float distanceFirstUse) {
      this.distanceFirstUse = distanceFirstUse;
   }

   public void setEquipment(final Equipment partEquipment) {

      equipment = partEquipment;
   }

   public void setIsCollate(final boolean isCollate) {
      this.isCollate = isCollate;
   }

   public void setModel(final String model) {

      this.model = model;

      _partName = null;
   }

   public void setPrice(final float price) {
      this.price = price;
   }

   public void setPriceUnit(final String priceUnit) {
      this.priceUnit = priceUnit;
   }

   public void setSize(final String size) {
      this.size = size;
   }

   public void setType(final String type) {
      this.type = type;
   }

   public void setUrlAddress(final String urlAddress) {

      this.urlAddress = urlAddress;
   }

   public void setWeight(final float weight) {
      this.weight = weight;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "EquipmentPart" + NL //                                      //$NON-NLS-1$

            + " partId           = " + partId + NL //                      //$NON-NLS-1$
            + " brand            = " + brand + NL //                       //$NON-NLS-1$
            + " model            = " + model + NL //                       //$NON-NLS-1$
            + " date             = " + getDate_Local() + NL //             //$NON-NLS-1$
            + " dateUntil        = " + getDateUntil_Local() + NL //        //$NON-NLS-1$

//            + " description      =" + description + NL //                  //$NON-NLS-1$
//            + " equipmentType    =" + equipmentType + NL //                //$NON-NLS-1$
//            + " distanceFirstUse =" + distanceFirstUse + NL //             //$NON-NLS-1$
//
//            + " dateBuilt        =" + dateBuilt + NL //                    //$NON-NLS-1$
//            + " dateFirstUse     =" + dateFirstUse + NL //                 //$NON-NLS-1$
//            + " dateRetired      =" + dateRetired + NL //                  //$NON-NLS-1$
//
//            + " weight           =" + weight + NL //                       //$NON-NLS-1$
      ;
   }

   public void updateFromOther(final EquipmentPart otherPart) {

// SET_FORMATTING_OFF

      setBrand             (otherPart.getBrand());
      setModel             (otherPart.getModel());
      setType              (otherPart.getType());
      setDescription       (otherPart.getDescription());
      setUrlAddress        (otherPart.getUrlAddress());

      setDistanceFirstUse  (otherPart.getDistanceFirstUse());
      setIsCollate         (otherPart.isCollate());
      setPrice             (otherPart.getPrice());
      setPriceUnit         (otherPart.getPriceUnit());
      setSize              (otherPart.getSize());
      setWeight            (otherPart.getWeight());

      setDate              (otherPart.getDate());
      setDateBuilt         (otherPart.getDateBuilt());
      setDateRetired       (otherPart.getDateRetired());

// SET_FORMATTING_ON
   }

   /**
    * Reset {@link #dateUntil} when part is not collated, this makes it easier to see it in the view
    */
   public void updateUntilDate() {

      if (isCollate == false) {

         setDateUntil(0);
      }
   }
}
