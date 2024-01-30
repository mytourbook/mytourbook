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
import net.tourbook.database.TourDatabase;
import net.tourbook.nutrition.QuantityType;
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
    * The name of the product's brand
    */
   private String                     brand;

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
   private QuantityType               quantityType             = QuantityType.Products;

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

   public TourNutritionProduct() {}

   public TourNutritionProduct(final TourData tourData, final boolean isCustomProduct) {

      this.tourData = tourData;
      _createId = _createCounter.incrementAndGet();
      productCode = isCustomProduct ? CUSTOMPRODUCTCODE_PREFIX + _createId : UI.EMPTY_STRING;
      consumedQuantity = 1f;
   }

   public TourNutritionProduct(final TourData tourData, final Product product) {

      this(tourData, false);
      brand = product.brands;
      name = product.productName;
      productCode = product.code;

      computeNutrimentsPerProduct(product);
   }

   /**
    * Sets the total amount of calories and sodium, if provided.
    * Otherwise, computed the calories and sodium using the amount per serving along with the
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
      final String productQuantity = product.productQuantity;
      final String quantity = product.quantity;
      final String servingQuantity = product.servingQuantity;

      if (nutriScoreData != null && nutriScoreData.isBeverage()) {

         isBeverage = product.nutriScoreData != null;

         if (quantity != null && productQuantity != null && servingQuantity != null) {

            final int numberOfServings = Math.round(Float.valueOf(productQuantity) / Float.valueOf(servingQuantity));
            setCalories(nutriments.energyKcalServing * numberOfServings);
            setCalories_Serving(nutriments.energyKcalServing);

            setSodium(Math.round(nutriments.sodiumServing * numberOfServings * 1000));
            setSodium_Serving(Math.round(nutriments.sodiumServing * 1000));

            // We store the quantity ONLY if the beverage is in liquid form (mL)
            if (quantity.trim().toUpperCase().endsWith("L")) {

               beverageQuantity = Integer.valueOf(productQuantity);
               setBeverageQuantity_Serving(Integer.valueOf(servingQuantity.trim()));
            }

         }
      } else {

         if (productQuantity != null) {

            setCalories(Math.round((nutriments.energyKcal100g * Float.valueOf(productQuantity)) / 100f));
            setCalories_Serving(nutriments.energyKcalServing);

            setSodium(Math.round(nutriments.sodium100g * 1000));
            setSodium_Serving(Math.round(nutriments.sodiumServing * 1000));

         } else {

            // In this case, we can only assume that the product only contains 1 serving
            setCalories(nutriments.energyKcalServing);
            setCalories_Serving(nutriments.energyKcalServing);
            setSodium(Math.round(nutriments.sodiumServing * 1000));
            setSodium_Serving(Math.round(nutriments.sodiumServing * 1000));
         }
      }
   }

   public int getBeverageQuantity() {
      return beverageQuantity;
   }

   public int getBeverageQuantity_Serving() {
      return beverageQuantity_Serving;
   }

   public String getBrand() {
      return brand;
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
