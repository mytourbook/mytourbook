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
import static javax.persistence.FetchType.LAZY;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;

@Entity
public class Equipment implements Cloneable, Serializable {

   private static final long   serialVersionUID      = 1L;

   private static final char   NL                    = UI.NEW_LINE;

   public static final int     DB_LENGTH_NAME        = 1000;
   public static final int     DB_LENGTH_DESCRIPTION = 32000;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                equipmentId           = TourDatabase.ENTITY_IS_NOT_SAVED;

   /**
    * Name or brand for the equipment
    */
   private String              name;

   /**
    * Model/subname for the equipment
    */
   private String              model;

   /**
    * Description/notes for the equipment
    */
   private String              description;

   /**
    *
    */
   @Enumerated(EnumType.STRING)
   private EquipmentType       equipmentType         = EquipmentType.NONE;

   /**
    * When the equipment was created/build, in UTC milliseconds
    */
   private long                dateBuilt;

   /**
    * When the equipment was bought or firstly used, in UTC milliseconds
    */
   private long                dateFirstUse;

   /**
    * When the equipment was retired/sold, in UTC milliseconds
    */
   private long                dateRetired;

   /**
    * Weight of the equipment, in kg
    */
   private float               weight;

   /**
    * Initial distance, in meter
    */
   private float               distanceFirstUse;

   /**
    * Contains all tours which are associated with this equipment
    */
   @ManyToMany(mappedBy = "equipments", cascade = ALL, fetch = LAZY)
   private final Set<TourData> tourData              = new HashSet<>();

//   Collection of services
//   Collection of parts

   @Transient
   private long      _createId = 0;

   @Transient
   private LocalDate _dateBuilt;

   @Transient
   private LocalDate _dateFirstUse;

   @Transient
   private LocalDate _dateRetired;

   /**
    * Default constructor used in EJB
    */
   public Equipment() {}

   @Override
   public Equipment clone() {

      Equipment newEquipment = null;

      try {

         newEquipment = (Equipment) super.clone();

      } catch (final CloneNotSupportedException e) {

         e.printStackTrace();
      }

      return newEquipment;
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

         // equipment is create
         if (_createId != other._createId) {
            return false;
         }
      }

      return true;
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
      return model;
   }

   /**
    * @return Returns the equipment name or an empty string when not available
    */
   public String getName() {

      if (name == null) {
         return UI.EMPTY_STRING;
      }

      return name;
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
            DB_LENGTH_DESCRIPTION,
            Messages.Db_Field_EquipmentDescription);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         description = description.substring(0, DB_LENGTH_DESCRIPTION);
      }

      return true;
   }

   public void setDateBuilt(final long dateBuilt) {

      this.dateBuilt = dateBuilt;

      _dateBuilt = null;
   }

   public void setDateFirstUse(final long dateFirstUse) {
      this.dateFirstUse = dateFirstUse;
   }

   public void setDateRetired(final long dateRetired) {
      this.dateRetired = dateRetired;
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

}
