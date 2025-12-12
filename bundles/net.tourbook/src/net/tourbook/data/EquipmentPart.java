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
import java.time.LocalDate;
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
    * When the equipment was created/build, in epoch days
    */
   private long                       dateBuilt;

   /**
    * When the equipment was bought or firstly used, in epoch days
    */
   private long                       dateFirstUse;

   /**
    * When the equipment was retired/sold, in epoch days
    */
   private long                       dateRetired;

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
   private LocalDate                  _dateBuilt;

   @Transient
   private LocalDate                  _dateFirstUse;

   @Transient
   private LocalDate                  _dateRetired;

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

   public LocalDate getDateBuilt() {

      if (_dateBuilt == null) {
         _dateBuilt = TimeTools.toLocalDate(dateBuilt * TimeTools.DAY_MILLISECONDS);
      }

      return _dateBuilt;
   }

   public long getDateBuilt_Raw() {

      return dateBuilt;
   }

   public LocalDate getDateFirstUse() {

      if (_dateFirstUse == null) {
         _dateFirstUse = TimeTools.toLocalDate(dateFirstUse * TimeTools.DAY_MILLISECONDS);
      }

      return _dateFirstUse;
   }

   public long getDateFirstUse_Raw() {

      return dateFirstUse;
   }

   public LocalDate getDateRetired() {

      if (_dateRetired == null) {
         _dateRetired = TimeTools.toLocalDate(dateRetired * TimeTools.DAY_MILLISECONDS);
      }

      return _dateRetired;
   }

   public long getDateRetired_Raw() {

      return dateRetired;
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

   public LocalDate getUsageDuration() {
      // TODO Auto-generated method stub
      return null;
   }

   public float getWeight() {
      return weight;
   }

   @Override
   public int hashCode() {

      return Objects.hash(partId, _createId);
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

   public void setDateBuilt(final long dateBuilt) {

      this.dateBuilt = dateBuilt;

      _dateBuilt = null;
   }

   public void setDateFirstUse(final long dateFirstUse) {

      this.dateFirstUse = dateFirstUse;

      _dateFirstUse = null;
   }

   public void setDateRetired(final long dateRetired) {

      this.dateRetired = dateRetired;

      _dateRetired = null;
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

   public void setWeight(final float weight) {
      this.weight = weight;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "EquipmentPart" + NL //                                      //$NON-NLS-1$

            + " partId           =" + partId + NL //                       //$NON-NLS-1$
            + " brand            =" + brand + NL //                        //$NON-NLS-1$
            + " model            =" + model + NL //                        //$NON-NLS-1$
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

      brand             = otherPart.getBrand();
      model             = otherPart.getModel();
      type              = otherPart.getType();
      description       = otherPart.getDescription();

      distanceFirstUse  = otherPart.getDistanceFirstUse();
      price             = otherPart.getPrice();
      priceUnit         = otherPart.getPriceUnit();
      size              = otherPart.getSize();
      weight            = otherPart.getWeight();

      setDateBuilt(       otherPart.getDateBuilt_Raw());
      setDateFirstUse(    otherPart.getDateFirstUse_Raw());
      setDateRetired(     otherPart.getDateRetired_Raw());

   // SET_FORMATTING_ON

      _partName = null;
   }
}
