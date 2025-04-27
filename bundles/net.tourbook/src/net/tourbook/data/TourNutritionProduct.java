/*******************************************************************************
 * Copyright (C) 2024, 2025 Frédéric Bard
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.database.TourDatabase;
import net.tourbook.nutrition.QuantityType;
import net.tourbook.nutrition.openfoodfacts.NutriScoreData;
import net.tourbook.nutrition.openfoodfacts.Nutriments;
import net.tourbook.nutrition.openfoodfacts.NutritionDataPer;
import net.tourbook.nutrition.openfoodfacts.Product;
import net.tourbook.tour.TourLogManager;

import org.eclipse.osgi.util.NLS;

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
    * - An empty string if it's a user's custom product
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
    * Carbohydrates per product.
    * Unit: g
    */
   private int                        carbohydrates;

   /**
    * Carbohydrates per serving.
    * Unit: g
    */
   private int                        carbohydrates_Serving;

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

   private void buildProductInfoDifference(final String updatedInfo,
                                           final String existingInfo,
                                           final List<String> previousData,
                                           final List<String> newData,
                                           final String unit) {

      previousData.add(UI.EMPTY_STRING

            + existingInfo + unit);

      newData.add(UI.EMPTY_STRING

            + updatedInfo + unit);
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

         setCalories(Math.round(nutriments.carbohydratesServing * numberOfServings));
         setCalories_Serving(Math.round(nutriments.carbohydratesServing));

         setCarbohydrates(Math.round(nutriments.carbohydratesServing * numberOfServings));
         setCarbohydrates_Serving(Math.round(nutriments.carbohydratesServing));

         setSodium(Math.round(nutriments.sodiumServing * numberOfServings * 1000));
         setSodium_Serving(Math.round(nutriments.sodiumServing * 1000));

         // We store the quantity ONLY if the beverage is in liquid form (mL)
         if (quantity.trim().toUpperCase().endsWith(UI.UNIT_FLUIDS_L)) {

            beverageQuantity = Math.round(productQuantity);
            setBeverageQuantity_Serving(Math.round(servingQuantity));
         }

      } else {

         setCalories_Serving(Math.round(nutriments.energyKcalServing));
         setCarbohydrates_Serving(Math.round(nutriments.carbohydratesServing));
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
            setCarbohydrates(Math.round((nutriments.carbohydrates100g * productQuantity) / 100f));
            setSodium(Math.round(nutriments.sodium100g * productQuantity * 10));

         } else if (product.nutritionDataPer == NutritionDataPer.SERVING) {

            final float numServingsPerProduct = servingQuantity == 0
                  ? 0 : productQuantity / servingQuantity;

            setCalories(Math.round(getCalories_Serving() * numServingsPerProduct));
            setCarbohydrates(Math.round(getCarbohydrates_Serving() * numServingsPerProduct));
            setSodium(Math.round(getSodium_Serving() * numServingsPerProduct));
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

   public int getCarbohydrates() {
      return carbohydrates;
   }

   public int getCarbohydrates_Serving() {
      return carbohydrates_Serving;
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

   public long getProductId() {
      return productId;
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

   public void setCarbohydrates(final int carbohydrates) {
      this.carbohydrates = carbohydrates;
   }

   public void setCarbohydrates_Serving(final int carbohydrates_Serving) {
      this.carbohydrates_Serving = carbohydrates_Serving;
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

   /**
    * Updates the product information with the new values, if any.
    * <p>
    * The method compares the existing product information with the
    * updated product information and updates the fields accordingly. It also
    * logs the changes made to the product information.
    *
    * @param updatedProduct
    *           The updated product information.
    *
    * @return
    */
   boolean updateProductInfo(final TourNutritionProduct updatedProduct) {

      final List<String> previousData = new ArrayList<>();
      final List<String> newData = new ArrayList<>();

      if (!this.getName().equals(updatedProduct.getName())) {

         buildProductInfoDifference(
               updatedProduct.getName(),
               this.getName(),
               previousData,
               newData,
               UI.EMPTY_STRING);

         this.setName(updatedProduct.getName());
      }

      if (this.getCalories() != updatedProduct.getCalories()) {

         buildProductInfoDifference(
               String.valueOf(updatedProduct.getCalories()),
               String.valueOf(this.getCalories()),
               previousData,
               newData,
               OtherMessages.VALUE_UNIT_K_CALORIES);

         this.setCalories(updatedProduct.getCalories());
         this.setCalories_Serving(updatedProduct.getCalories_Serving());
      }

      if (this.getCarbohydrates() != updatedProduct.getCarbohydrates()) {

         buildProductInfoDifference(
               String.valueOf(updatedProduct.getCarbohydrates()),
               String.valueOf(this.getCarbohydrates()),
               previousData,
               newData,
               UI.UNIT_WEIGHT_G + UI.SPACE + Messages.Tour_Nutrition_Label_Carbohydrates);

         this.setCarbohydrates(updatedProduct.getCarbohydrates());
         this.setCarbohydrates_Serving(updatedProduct.getCarbohydrates_Serving());
      }

      if (!Objects.equals(this.getSodium(), updatedProduct.getSodium())) {

         buildProductInfoDifference(
               String.valueOf(updatedProduct.getSodium()),
               String.valueOf(this.getSodium()),
               previousData,
               newData,
               UI.UNIT_WEIGHT_MG + UI.SPACE + Messages.Tour_Nutrition_Label_Sodium);

         this.setSodium(updatedProduct.getSodium());
         this.setSodium_Serving(updatedProduct.getSodium_Serving());
      }

      if (previousData.isEmpty() && newData.isEmpty()) {

         return false;
      }

      final String previousDataJoined = StringUtils
            .join(previousData.stream().toArray(String[]::new), UI.COMMA_SPACE);

      final String newDataJoined = StringUtils
            .join(newData.stream().toArray(String[]::new), UI.COMMA_SPACE);

      TourLogManager.subLog_INFO(
            this.getName() + UI.SPACE + "→" + UI.SPACE + //$NON-NLS-1$
                  NLS.bind(
                        Messages.Log_ModifiedTour_Old_Data_Vs_New_Data,
                        previousDataJoined,
                        newDataJoined));

      return true;
   }
}
