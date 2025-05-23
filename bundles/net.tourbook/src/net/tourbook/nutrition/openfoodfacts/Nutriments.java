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
package net.tourbook.nutrition.openfoodfacts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Nutriments {

   @JsonProperty("energy-kcal_100g")
   public float energyKcal100g;

   @JsonProperty("energy-kcal_serving")
   public float energyKcalServing;

   @JsonProperty("carbohydrates_100g")
   public float carbohydrates100g;

   @JsonProperty("carbohydrates_serving")
   public float carbohydratesServing;

   @JsonProperty("sodium_100g")
   public float sodium100g;

   @JsonProperty("sodium_serving")
   public float sodiumServing;
}
