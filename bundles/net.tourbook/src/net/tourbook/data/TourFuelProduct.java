/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.common.UI;
import net.tourbook.database.TourDatabase;
import net.tourbook.nutrition.Product;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "productCode")
public class TourFuelProduct implements Serializable {

   private static final long          serialVersionUID = 1L;

   /**
    * manually created marker or imported marker create a unique id to identify them, saved marker
    * are compared with the marker id
    */
   private static final AtomicInteger _createCounter   = new AtomicInteger();

   /**
    * Unique id for manually created markers because the {@link #productCode} is 0 when the marker
    * is
    * not persisted
    */
   @Transient
   private long                       _createId        = 0;
   /**
    * Unique id for the {@link TourFuelProduct} entity
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @JsonProperty
   private long                       productCode      = TourDatabase.ENTITY_IS_NOT_SAVED;

   @ManyToOne(optional = false)
   private TourData                   tourData;

   private String                     name;


   private int                        calories;
//
//   private double                     carbohydrates;
//
//   private double                     sodium;
//
//   private double                     fluidVolume;
//   private double                     containerName;
//
//   private double                     caffeine;

   public TourFuelProduct() {}

   public TourFuelProduct(final TourData tourData, final Product product) {

      this.tourData = tourData;
      name = product.getName();
      //calories = product.get
   }

   /**
    * Tourmarker is compared with the {@link TourFuelProduct#markerId} or
    * {@link TourFuelProduct#_createId}
    * <p>
    * <b> {@link #serieIndex} is not used for equals or hashcode because this is modified when
    * markers are deleted</b>
    *
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof TourFuelProduct)) {
         return false;
      }

      final TourFuelProduct otherMarker = (TourFuelProduct) obj;

      if (productCode == TourDatabase.ENTITY_IS_NOT_SAVED) {

         // marker was create or imported

//         if (_createId == otherMarker._createId) {
//            return true;
//         }

      } else {

         // marker is from the database

         if (productCode == otherMarker.productCode) {
            return true;
         }
      }

      return false;
   }

   public int getCalories() {
      //todo fb
      return 10;
   }

   public String getName() {
      return name;
   }

   public int getServingsConsumed() {
      return 1;
   }

   public TourData getTourData() {
      return tourData;
   }

   /**
    * !!!!!!!!!!!!!!!!!<br>
    * serieIndex is not used for equals or hashcode because this is modified when markers are
    * deleted<br>
    * !!!!!!!!!!!!!!!!!<br>
    *
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;

      if (productCode == TourDatabase.ENTITY_IS_NOT_SAVED) {
         result = prime * result + (int) (productCode ^ (productCode >>> 32));
      } else {
         result = prime * result + (int) (productCode ^ (productCode >>> 32));
      }

      return result;
   }

   public void setupDeepClone(final TourData tourDataFromClone) {

      _createId = _createCounter.incrementAndGet();

      productCode = TourDatabase.ENTITY_IS_NOT_SAVED;

      tourData = tourDataFromClone;
   }

   /**
    * This method is called in the "Tour Data" view !!!
    */
   @Override
   public String toString() {

      return UI.EMPTY_STRING

      ; //
   }
}
