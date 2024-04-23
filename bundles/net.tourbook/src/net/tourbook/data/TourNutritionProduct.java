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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.database.TourDatabase;
import net.tourbook.nutrition.QuantityType;
import net.tourbook.nutrition.openfoodfacts.NutriScoreData;
import net.tourbook.nutrition.openfoodfacts.Nutriments;
import net.tourbook.nutrition.openfoodfacts.NutritionDataPer;
import net.tourbook.nutrition.openfoodfacts.Product;

@Entity
public class TourNutritionProduct {

   public static final int            DB_LENGTH_CODE = 20;
   public static final int            DB_LENGTH_NAME = 1024;

   /**
    * manually created product or imported product create a unique id to identify
    * them, saved products are compared with the product id
    */
   private static final AtomicInteger _createCounter = new AtomicInteger();

   /**
    * Unique id for the {@link TourNutritionProduct} entity
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       productId      = TourDatabase.ENTITY_IS_NOT_SAVED;

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
    * Calories per serving.
    * Unit: kcal
    */
   private int                        calories_Serving;

   /**
    * Sodium amount per serving.
    * Unit: mg
    */
   private int                        sodium_Serving;

   /**
    * The quantity consumed, either in number of servings or products (see {@link #quantityType}).
    */
   private float                      consumedQuantity;

   /**
    * The quantity type consumed:
    * - Servings
    * - Number of products.
    */
   @Enumerated(EnumType.STRING)
   private QuantityType               quantityType   = QuantityType.Products;

   /**
    * Indicates if the nutrition product is a beverage itself or used in a
    * beverage.
    */
   private boolean                    isBeverage;

   /**
    * Indicates if the product was created internally by the user.
    */
   private boolean                    isCustomProduct;

   /**
    * If the product is a beverage, the amount of liquid per product.
    * Unit: mL
    */
   private int                        beverageQuantity;

   /**
    * If the product is a beverage, the amount of liquid per serving.
    * Unit: mL
    */
   private int                        beverageQuantity_Serving;

   @ManyToOne
   private TourBeverageContainer      tourBeverageContainer;

   /**
    * The total number of beverage containers consumed
    */
   private float                      containersConsumed;

   /**
    * Unique id for manually created products because the {@link #productCode}
    * is 0 when the product is not persisted
    */
   @Transient
   private long                       _createId      = 0;

   public TourNutritionProduct() {}

   public TourNutritionProduct(final TourData tourData, final boolean isCustomProduct) {

      this.tourData = tourData;
      _createId = _createCounter.incrementAndGet();
      productCode = UI.EMPTY_STRING;
      consumedQuantity = 1f;
      this.isCustomProduct = isCustomProduct;
   }

   public TourNutritionProduct(final TourData tourData, final Product product) {

      this(tourData, false);
      name = product.productName;
      productCode = product.code;
      isBeverage = product.nutriScoreData != null && product.nutriScoreData.isBeverage();

      computeNutrimentsPerProduct(product);
   }

   /**
    * Sets the total amount of calories and sodium, if provided.
    * Otherwise, computes the calories and sodium using the amount per serving along with the
    * number of servings per products.
    *
    * @param product
    *
    * @return
    */
   private void computeNutrimentsPerProduct(final Product product) {

      final Nutriments nutriments = product.nutriments;
      if (nutriments == null) {
         return;
      }

      final NutriScoreData nutriScoreData = product.nutriScoreData;
      float productQuantity = Util.parseFloat(product.productQuantity);
      final String quantity = product.quantity;
      float servingQuantity = Util.parseFloat(product.servingQuantity);

      if (nutriScoreData != null && nutriScoreData.isBeverage() &&
            quantity != null && productQuantity != Float.MIN_VALUE &&
            servingQuantity != Float.MIN_VALUE) {

         final int numberOfServings = Math.round(productQuantity / servingQuantity);
         setCalories(Math.round(nutriments.energyKcalServing * numberOfServings));
         setCalories_Serving(Math.round(nutriments.energyKcalServing));

         setSodium(Math.round(nutriments.sodiumServing * numberOfServings * 1000));
         setSodium_Serving(Math.round(nutriments.sodiumServing * 1000));

         // We store the quantity ONLY if the beverage is in liquid form (mL)
         if (quantity.trim().toUpperCase().endsWith(UI.UNIT_FLUIDS_L)) {

            beverageQuantity = Math.round(productQuantity);
            setBeverageQuantity_Serving(Math.round(servingQuantity));
         }

      } else {

         setCalories_Serving(Math.round(nutriments.energyKcalServing));
         setSodium_Serving(Math.round(nutriments.sodiumServing * 1000));

         // In some cases, the product quantity is not provided, we can only
         // assume that the product only contains 1 serving.
         // When the serving quantity is not provided, we can only assume that
         // the serving contains 1 product.
         if (productQuantity == Float.MIN_VALUE) {

            productQuantity = servingQuantity;

         } else if (servingQuantity == Float.MIN_VALUE) {

            servingQuantity = productQuantity;

         }

         // If the data is for 100g or 1 serving, the product calories and sodium
         // values computation are different
         if (product.nutritionDataPer == NutritionDataPer.HUNDRED_GRAMS) {

            setCalories(Math.round((nutriments.energyKcal100g * productQuantity) / 100f));
            setSodium(Math.round(nutriments.sodium100g * productQuantity * 10));

         } else if (product.nutritionDataPer == NutritionDataPer.SERVING) {

            final int numServingsPerProduct = servingQuantity == 0
                  ? 0 : Math.round(productQuantity / servingQuantity);

            setCalories(getCalories_Serving() * numServingsPerProduct);
            setSodium(getSodium_Serving() * numServingsPerProduct);
         }
      }
   }

   public int getBeverageQuantity() {
      return beverageQuantity;
   }

   public int getBeverageQuantity_Serving() {
      return beverageQuantity_Serving;
   }

   public int getCalories() {
      return calories;
   }

   public int getCalories_Serving() {
      return calories_Serving;
   }

   public float getConsumedQuantity() {
      return consumedQuantity;
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

   public QuantityType getQuantityType() {
      return quantityType;
   }

   public int getSodium() {
      return sodium;
   }

   public int getSodium_Serving() {
      return sodium_Serving;
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

   public boolean isCustomProduct() {
      return isCustomProduct;
   }

   public void setBeverageQuantity(final int beverageQuantity) {
      this.beverageQuantity = beverageQuantity;
   }

   public void setBeverageQuantity_Serving(final int beverageQuantity_Serving) {
      this.beverageQuantity_Serving = beverageQuantity_Serving;
   }

   public void setCalories(final int calories) {
      this.calories = calories;
   }

   public void setCalories_Serving(final int calories_Serving) {
      this.calories_Serving = calories_Serving;
   }

   public void setConsumedQuantity(final float consumedQuantity) {
      this.consumedQuantity = consumedQuantity;
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

   public void setQuantityType(final QuantityType quantityType) {
      this.quantityType = quantityType;
   }

   public void setSodium(final int sodium) {
      this.sodium = sodium;
   }

   public void setSodium_Serving(final int sodium_Serving) {
      this.sodium_Serving = sodium_Serving;
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
