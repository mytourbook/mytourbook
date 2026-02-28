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
import net.tourbook.equipment.EquipmentManager;

@Entity
public class EquipmentPart implements Cloneable, Comparable<Object>, Serializable {

   private static final char          NL                     = UI.NEW_LINE;

   private static final long          serialVersionUID       = 1L;

   private static final AtomicInteger _createCounter         = new AtomicInteger();

   public static final short          ITEM_TYPE_PART         = 0;
   public static final short          ITEM_TYPE_SERVICE      = 1;

   public static final short          COLLATED_WITH_PREVIOUS = 0;
   public static final short          COLLATED_WITH_NEXT     = 1;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       partId                 = TourDatabase.ENTITY_IS_NOT_SAVED;

   /**
    * How this part is used and displayed, it can be e.g.
    * <p>
    * {@link #ITEM_TYPE_PART}<br>
    * {@link #ITEM_TYPE_SERVICE}
    */
   private short                      itemType;

   /**
    * Defines how this part/service is collating tours, to the previous or next part/service of the
    * same
    * {@link #type} and {@link #dateFrom}
    * <p>
    * {@link #COLLATED_WITH_PREVIOUS}<br>
    * {@link #COLLATED_WITH_NEXT}
    */
   private short                      collateWith;

   /**
    * Brand/name for the equipment, e.g. Continental
    */
   private String                     brand;

   /**
    * Model/subname for the equipment, e.g. Grand Prix 4000 S II
    */
   private String                     model;

   /**
    * Part type, e.g. Faltreifen
    */
   private String                     type;

   /**
    * Name for the service
    */
   private String                     name;

   /**
    * Company which did the service
    */
   private String                     company;

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
    *
    */
   private String                     imageFilePath;

   /**
    * When <code>true</code> then this part is included in collated parts
    */
   private boolean                    isCollate              = true;

   /**
    * When the part was firstly used, in milliseconds since 1970-01-01T00:00:00Z
    */
   private long                       dateFrom;

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
    * <p>
    * This value is computed from the previous part.
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

   /**
    * When a part is expanded in the equipment viewer, the tours can be displayed in different
    * structures
    * <p>
    * <li>0 ... EXPAND_TYPE_FLAT</li>
    * <li>1 ... EXPAND_TYPE_YEAR_TOUR</li>
    * <li>2 ... EXPAND_TYPE_YEAR_MONTH_TOUR</li>
    */
   private short                      expandType             = EquipmentManager.EXPAND_TYPE_FLAT;

   /** One equipment can have multiple parts */
   @ManyToOne(optional = false)
   private Equipment                  equipment;

   @Transient
   private long                       _createId              = 0;

   @Transient
   private LocalDateTime              _dateFrom;

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

   /**
    * @param itemType
    *           How this part is used and displayed, it can be e.g.
    *           <p>
    *           {@link #ITEM_TYPE_PART}<br>
    *           {@link #ITEM_TYPE_SERVICE}
    */
   public EquipmentPart(final short itemType) {

      this.itemType = itemType;
   }

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

   /**
    * @return Returns {@link #collateWith}
    */
   public short getCollateWith() {
      return collateWith;
   }

   public String getCompany() {

      if (company == null) {
         return UI.EMPTY_STRING;
      }

      return company;
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

   public long getDateFrom() {

      return dateFrom;
   }

   /**
    * @return Return the first use date
    */
   public LocalDateTime getDateFrom_Local() {

      if (_dateFrom == null) {
         _dateFrom = TimeTools.toLocalDateTime(dateFrom);
      }

      return _dateFrom;
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

      final long duration = dateUntil - dateFrom;

      return duration;
   }

   /**
    * @return {@link #equipment}
    */
   public Equipment getEquipment() {
      return equipment;
   }

   /**
    * @return {@link #expandType}
    */
   public int getExpandType() {
      return expandType;
   }

   public String getImageFilePath() {
      return imageFilePath;
   }

   /**
    * @return {@link #itemType}
    */
   public int getItemType() {
      return itemType;
   }

   /**
    * @return {@link #model}
    */
   public String getModel() {

      if (model == null) {
         return UI.EMPTY_STRING;
      }

      return model;
   }

   /**
    * @return Returns a combined name of the equipment part with "brand - model" or the service name
    */
   public String getName() {

      if (itemType == ITEM_TYPE_PART) {

         // part

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

      } else if (itemType == ITEM_TYPE_SERVICE) {

         // service

         if (name == null) {
            return UI.EMPTY_STRING;
         }

         return name;
      }

      return UI.EMPTY_STRING;
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

   public String getPartType() {

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
            || collateWith != otherPart.getCollateWith()
            || dateFrom != otherPart.getDateFrom()
            || type.equalsIgnoreCase(otherPart.getPartType()) == false) {

         // collated fields are modified

         return true;
      }

      return false;
   }

   public boolean isItemType_Part() {
      return itemType == ITEM_TYPE_PART;
   }

   public boolean isItemType_Service() {
      return itemType == ITEM_TYPE_SERVICE;
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

   public void resetName() {

      _partName = null;
   }

   public void setBrand(final String brand) {

      this.brand = brand;

      _partName = null;
   }

   /**
    * Set {@link #collateWith}
    *
    * @param collateWith
    */
   public void setCollateWith(final short collateWith) {
      this.collateWith = collateWith;
   }

   public void setCompany(final String company) {
      this.company = company;
   }

   public void setDateBuilt(final long dateBuilt) {

      this.dateBuilt = dateBuilt;

      _dateBuilt = null;
   }

   public void setDateFrom(final long dateFrom) {

      this.dateFrom = dateFrom;

      _dateFrom = null;
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

   public void setExpandType(final short expandType) {
      this.expandType = expandType;
   }

   public void setImageFilePath(final String imageFilePath) {
      this.imageFilePath = imageFilePath;
   }

   public void setIsCollate(final boolean isCollate) {
      this.isCollate = isCollate;
   }

   public void setModel(final String model) {

      this.model = model;

      _partName = null;
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

   public void setSize(final String size) {
      this.size = size;
   }

   public void setPartType(final String type) {
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

            + "EquipmentPart" + NL //                                         //$NON-NLS-1$

            + "  partId           = " + partId + NL //                        //$NON-NLS-1$
            + "  brand            = " + brand + NL //                         //$NON-NLS-1$
            + "  model            = " + model + NL //                         //$NON-NLS-1$
            + "  type				 = " + type + NL //                          //$NON-NLS-1$
            + "  dateFrom         = " + getDateFrom_Local() + NL //               //$NON-NLS-1$
            + "  dateUntil        = " + getDateUntil_Local() + NL //          //$NON-NLS-1$

//            + " description      =" + description + NL //                   //$NON-NLS-1$
//            + " equipmentType    =" + equipmentType + NL //                 //$NON-NLS-1$
//            + " distanceFirstUse =" + distanceFirstUse + NL //              //$NON-NLS-1$
//
//            + " dateBuilt        =" + dateBuilt + NL //                     //$NON-NLS-1$
//            + " dateFirstUse     =" + dateFirstUse + NL //                  //$NON-NLS-1$
//            + " dateRetired      =" + dateRetired + NL //                   //$NON-NLS-1$
//
//            + " weight           =" + weight + NL //                        //$NON-NLS-1$
      ;
   }

   public void updateFromOther(final EquipmentPart otherPart) {

// SET_FORMATTING_OFF

      setBrand             (otherPart.getBrand());
      setModel             (otherPart.getModel());
      setDescription       (otherPart.getDescription());
      setImageFilePath     (otherPart.getImageFilePath());
      setUrlAddress        (otherPart.getUrlAddress());

      setCompany           (otherPart.getCompany());
      setName              (otherPart.getName());

      setIsCollate         (otherPart.isCollate());
      setCollateWith       (otherPart.getCollateWith());
      setPartType          (otherPart.getPartType());

      setDistanceFirstUse  (otherPart.getDistanceFirstUse());
      setPrice             (otherPart.getPrice());
      setPriceUnit         (otherPart.getPriceUnit());
      setSize              (otherPart.getSize());
      setWeight            (otherPart.getWeight());

      setDateFrom          (otherPart.getDateFrom());
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
