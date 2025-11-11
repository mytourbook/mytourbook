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

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.EAGER;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;

import org.hibernate.annotations.Cascade;

@Entity
public class Equipment implements Cloneable, Serializable {

   private static final long          serialVersionUID = 1L;

   private static final AtomicInteger _createCounter   = new AtomicInteger();

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       equipmentId      = TourDatabase.ENTITY_IS_NOT_SAVED;

   /**
    * Name/brand for the equipment
    */
   private String                     brand;

   /**
    * Model/subname for the equipment
    */
   private String                     model;

   /**
    * Description/notes for the equipment
    */
   private String                     description;

   /**
    *
    */
   @Enumerated(EnumType.STRING)
   private EquipmentType              equipmentType    = EquipmentType.NONE;

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
    * Initial distance, in meter
    */
   private float                      distanceFirstUse;

//   /**
//    * Contains all parts which are associated with this equipment, e.g.
//    * <ul>
//    * <li></li>
//    * </ul>
//    */
//   @ManyToMany(mappedBy = "parts", cascade = ALL, fetch = LAZY)
//   private final Set<EquipmentPart>    parts                 = new HashSet<>();

   /**
    * Contains all services which are associated with this equipment, e.g.
    * <ul>
    * <li></li>
    * </ul>
    */
   @OneToMany(fetch = EAGER, cascade = ALL, mappedBy = "equipment")
   @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
   private Set<EquipmentService> services  = new HashSet<>();

   @Transient
   private long                  _createId = 0;

   @Transient
   private LocalDate             _dateBuilt;

   @Transient
   private LocalDate             _dateFirstUse;

   @Transient
   private LocalDate             _dateRetired;

   /**
    * Default constructor used in EJB
    */
   public Equipment() {}

   @Override
   public Equipment clone() {

      Equipment clonedEquipment = null;

      try {

         clonedEquipment = (Equipment) super.clone();

      } catch (final CloneNotSupportedException e) {

         e.printStackTrace();
      }

      clonedEquipment._createId = _createCounter.incrementAndGet();

      return clonedEquipment;
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof Equipment)) {
         return false;
      }

      final Equipment other = (Equipment) obj;

      if (_createId == 0) {

         // equipment is from the database
         if (equipmentId != other.equipmentId) {
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
         _dateBuilt = TimeTools.toLocalDate(dateBuilt);
      }

      return _dateBuilt;
   }

   public LocalDate getDateFirstUse() {

      if (_dateFirstUse == null) {
         _dateFirstUse = TimeTools.toLocalDate(dateFirstUse);
      }

      return _dateFirstUse;
   }

   public LocalDate getDateRetired() {

      if (_dateRetired == null) {
         _dateRetired = TimeTools.toLocalDate(dateRetired);
      }

      return _dateRetired;
   }

   public String getDescription() {

      if (description == null) {
         return UI.EMPTY_STRING;
      }

      return description;
   }

   public float getDistanceBought() {
      return distanceFirstUse;
   }

   /**
    * @return Returns the primary key for a {@link Equipment} entity
    */
   public long getEquipmentId() {
      return equipmentId;
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

      return sb.toString();
   }

   public float getWeight() {
      return weight;
   }

   @Override
   public int hashCode() {

      return Objects.hash(equipmentId, _createId);
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

   public void setBrand(final String brand) {
      this.brand = brand;
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

   public void setModel(final String model) {
      this.model = model;
   }

   public void setWeight(final float weight) {
      this.weight = weight;
   }

   public void updateFromOther(final Equipment otherEquipment) {

      brand = otherEquipment.getBrand();
      model = otherEquipment.getModel();
      description = otherEquipment.getDescription();

      setDateBuilt(otherEquipment.getDateBuilt().toEpochDay());
      setDateFirstUse(otherEquipment.getDateRetired().toEpochDay());
      setDateRetired(otherEquipment.getDateRetired().toEpochDay());
   }

}
