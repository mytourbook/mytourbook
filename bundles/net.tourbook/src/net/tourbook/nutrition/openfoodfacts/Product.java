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
package net.tourbook.nutrition.openfoodfacts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.viewers.ISelection;

/**
 * Source: https://world.openfoodfacts.org/data
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product implements ISelection {

   @JsonProperty("nutrition_data_per")
   public NutritionDataPer nutritionDataPer;

   public Nutriments       nutriments;

   @JsonProperty("nutriscore_data")
   public NutriScoreData   nutriScoreData;

   public String           brands;

   @JsonProperty("product_name")
   public String           productName;

   /*
    * The size in g or ml for the whole product. It's a normalized version of the quantity field.
    */
   @JsonProperty("product_quantity")
   public String productQuantity;

   /*
    * Quantity and Unit.
    */
   public String quantity;

   /*
    * Normalized version of serving_size.
    */
   @JsonProperty("serving_quantity")
   public String servingQuantity;

   public String code;

   @Override
   public boolean isEmpty() {
      return StringUtils.isNullOrEmpty(productName + code);
   }
}
