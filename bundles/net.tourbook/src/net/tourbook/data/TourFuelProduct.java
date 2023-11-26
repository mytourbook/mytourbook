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

import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;

import pl.coderion.model.Nutriments;
import pl.coderion.model.Product;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "productCode")
public class TourFuelProduct {

   // todo fb rename to TourNutritionItem ? TourNutritionProduct ?

   /**
    * manually created marker or imported marker create a unique id to identify them, saved marker
    * are compared with the marker id
    */
   private static final AtomicInteger _createCounter = new AtomicInteger();

   /**
    * Unique id for manually created markers because the {@link #productCode} is 0 when the marker
    * is
    * not persisted
    */
   @Transient
   private long                       _createId      = 0;
   /**
    * Unique id for the {@link TourFuelProduct} entity
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @JsonProperty
   private long                       productCode    = TourDatabase.ENTITY_IS_NOT_SAVED;

   @ManyToOne(optional = false)
   private TourData                   tourData;

   private String                     name;

   private int                        calories;
   private float                      fluid;
   private float                      sodium;

   //link it to an object defined by the user ?
//   private double                     containerName;
//
   // private double caffeine;

   public TourFuelProduct() {}

   public TourFuelProduct(final TourData tourData, final Product product) {

      this.tourData = tourData;
      name = product.getProductName();

      final Nutriments nutriments = product.getNutriments();
      calories = nutriments.getEnergyKcalServing();
      sodium = nutriments.getSodiumServing();
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
      return calories;
   }

   public float getFluid() {
      return fluid;
   }

   public String getName() {
      return name;
   }

   public int getServingsConsumed() {
      //todo fb
      return 1;
   }

   public float getSodium() {
      return Math.round(sodium / 1000);
   }

   public TourData getTourData() {
      return tourData;
   }

   public void setupDeepClone(final TourData tourDataFromClone) {

      _createId = _createCounter.incrementAndGet();

      productCode = TourDatabase.ENTITY_IS_NOT_SAVED;

      tourData = tourDataFromClone;
   }

}
