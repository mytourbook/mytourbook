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

import net.tourbook.common.UI;
import net.tourbook.database.TourDatabase;
import net.tourbook.nutrition.openfoodfacts.NutriScoreData;
import net.tourbook.nutrition.openfoodfacts.Nutriments;
import net.tourbook.nutrition.openfoodfacts.Product;

@Entity
public class TourNutritionProduct {

   public static final int            DB_LENGTH_CODE           = 50;
   public static final int            DB_LENGTH_NAME           = 1024;
   /**
    * manually created marker or imported marker create a unique id to identify them, saved marker
    * are compared with the marker id
    */
   private static final AtomicInteger _createCounter           = new AtomicInteger();

   private static final String        CUSTOMPRODUCTCODE_PREFIX = "MTCUSTOMPRODUCT-";              //$NON-NLS-1$

   /**
    * Unique id for manually created markers because the {@link #productCode} is 0 when the marker
    * is not persisted
    */
   @Transient
   private long                       _createId                = 0;

   /**
    * Unique id for the {@link TourNutritionProduct} entity
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       productId                = TourDatabase.ENTITY_IS_NOT_SAVED;

   /**
    * The code identifying the product:
    * - The bar code (https://en.wikipedia.org/wiki/Barcode)
    * OR
    * - A string following the below pattern if it's a user's custom product "MTCUSTOMPRODUCT-XX"
    */
   private String                     productCode;

   @ManyToOne(optional = false)
   private TourData                   tourData;

   /**
    * The name of the product
    */
   private String                     name;

   /**
    * Calories per product.
    * Unit: kcal
    */
   private int                        calories;

   /**
    * Sodium amount per product.
    * Unit: mg
    */
   private int                        sodium;

   /**
    * The number of products consumed.
    */
   private float                      productsConsumed;

   /**
    * Indicates if the nutrition product is a beverage itself or used in a
    * beverage.c
    */
   private boolean                    isBeverage;

   /**
    * If the product is a beverage, the amount of liquid per product.
    * Unit: mL
    */
   private int                        beverageQuantity;

   /**
    * product_quantity: string
    *
    * The size in g or ml for the whole product. It's a normalized version of the quantity field.
    *
    */

   @ManyToOne
   private TourBeverageContainer      tourBeverageContainer;

   /**
    * The total number of beverage containers consumed
    */
   private float                      containersConsumed;

   public TourNutritionProduct() {}

   public TourNutritionProduct(final TourData tourData, final boolean isCustomProduct) {

      this.tourData = tourData;
      _createId = _createCounter.incrementAndGet();
      productCode = isCustomProduct ? CUSTOMPRODUCTCODE_PREFIX + _createId : UI.EMPTY_STRING;
   }

   public TourNutritionProduct(final TourData tourData, final Product product) {

      this(tourData, false);
      name = product.productName;
      productCode = product.code;

      computeNutrimentsPerProduct(product);

      productsConsumed = 1f;
   }

   /**
    * Sets the total amount of calories and sodium, if provided.
    * Otherwise, computed the calories and sodium using the amount per serving along with the
    * number of servings per products.
    *
    * @param product
    * @param nutriments
    *
    * @return
    */
   private void computeNutrimentsPerProduct(final Product product) {

      final Nutriments nutriments = product.nutriments;
      if (nutriments == null) {
         return;
      }

      final NutriScoreData nutriScoreData = product.nutriScoreData;
      final String productQuantity = product.productQuantity;
      final String servingQuantity = product.servingQuantity;

      if (nutriScoreData != null && nutriScoreData.isBeverage()) {

         isBeverage = product.nutriScoreData != null;

         if (productQuantity != null && servingQuantity != null) {

            final int numberOfServings = Math.round(Float.valueOf(productQuantity) / Float.valueOf(servingQuantity));
            calories = nutriments.energyKcalServing * numberOfServings;
            sodium = Math.round(nutriments.sodiumServing * numberOfServings * 1000);
            beverageQuantity = Integer.valueOf(product.productQuantity);

         }
      } else {

         if (productQuantity != null) {

            calories = Math.round((nutriments.energyKcal100g * Float.valueOf(product.productQuantity)) / 100f);
            sodium = Math.round(nutriments.sodium100g * 1000);

         } else {

            // In this case, we can only assume that the product only contains 1 serving
            calories = nutriments.energyKcalServing;
            sodium = Math.round(nutriments.sodiumServing * 1000);

         }
      }
   }

   public int getBeverageQuantity() {
      return beverageQuantity;
   }

   public int getCalories() {
      return calories;
   }

   public float getContainersConsumed() {
      return containersConsumed;
   }

   public String getName() {
      return name;
   }

   public String getProductCode() {
      return productCode;
   }

   public float getProductsConsumed() {
      return productsConsumed;
   }

   public int getSodium() {
      return sodium;
   }

   public TourBeverageContainer getTourBeverageContainer() {
      return tourBeverageContainer;
   }

   public String getTourBeverageContainerCapacity() {

      if (tourBeverageContainer != null) {
         return String.valueOf(tourBeverageContainer.getCapacity());
      }

      return UI.EMPTY_STRING;
   }

   public String getTourBeverageContainerName() {

      if (tourBeverageContainer != null) {
         return tourBeverageContainer.getName();
      }

      return UI.EMPTY_STRING;
   }

   public TourData getTourData() {
      return tourData;
   }

   public boolean isBeverage() {
      return isBeverage;
   }

   public void setBeverageQuantity(final int beverageQuantity) {
      this.beverageQuantity = beverageQuantity;
   }

   public void setContainersConsumed(final float containersConsumed) {
      this.containersConsumed = containersConsumed;
   }

   public void setIsBeverage(final boolean isBeverage) {
      this.isBeverage = isBeverage;
   }

   public void setName(final String name) {
      this.name = name;

   }

   public void setProductCode(final String productCode) {
      this.productCode = productCode;
   }

   public void setProductsConsumed(final float servingsConsumed) {
      this.productsConsumed = servingsConsumed;
   }

   public void setTourBeverageContainer(final TourBeverageContainer tourBeverageContainer) {
      this.tourBeverageContainer = tourBeverageContainer;
   }

   void setupDeepClone(final TourData tourDataFromClone) {

      _createId = _createCounter.incrementAndGet();

      productId = TourDatabase.ENTITY_IS_NOT_SAVED;

      tourData = tourDataFromClone;
   }

}
