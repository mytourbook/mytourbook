/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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

import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;
import net.tourbook.nutrition.openfoodfacts.Product;

import pl.coderion.model.Nutriments;

@Entity
public class TourNutritionProduct {

   //todo fb implement swt.del on the table
   // fluidContainerName
   // fluidContainerCapacity
   // fluidQuantityConsumed
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
    * Unique id for the {@link TourNutritionProduct} entity
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       productCode    = TourDatabase.ENTITY_IS_NOT_SAVED;

   @ManyToOne(optional = false)
   private TourData                   tourData;

   //todo fb
   //Caused by: ERROR 42X04: Column 'TOURNUTRIT0_.TOURBEVERAGECONTAINER_CONTAINERCODE' is either not in any table in the FROM list or appears within a join specification and is outside the scope of the join specification or appears in a HAVING clause and is not in the GROUP BY list. If this is a CREATE or ALTER TABLE  statement then 'TOURNUTRIT0_.TOURBEVERAGECONTAINER_CONTAINERCODE' is not a column in the target table.

   @ManyToOne
   private TourBeverageContainer tourBeverageContainer;

   private String                name;

   private int                   calories;
   private float                 sodium;

   private boolean               isBeverage;

   // private double or float quantityBeverageContainerConsumed_Fraction;

   public TourNutritionProduct() {}

   public TourNutritionProduct(final TourData tourData, final Product product) {

      this.tourData = tourData;
      name = product.productName();

      final Nutriments nutriments = product.nutriments();
      calories = nutriments.getEnergyKcalServing();
      sodium = nutriments.getSodiumServing();
      isBeverage = product.nutriScoreData() != null ? product.nutriScoreData().isBeverage() : false;

      _createId = _createCounter.incrementAndGet();
   }

   public int getCalories() {
      return calories;
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

   public boolean isBeverage() {
      return isBeverage;
   }

   public void setIsBeverage(final boolean isBeverage) {
      this.isBeverage = isBeverage;
   }

   public void setupDeepClone(final TourData tourDataFromClone) {

      _createId = _createCounter.incrementAndGet();

      productCode = TourDatabase.ENTITY_IS_NOT_SAVED;

      tourData = tourDataFromClone;
   }

}
