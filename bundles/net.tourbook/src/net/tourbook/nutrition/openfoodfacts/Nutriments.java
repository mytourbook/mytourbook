package net.tourbook.nutrition.openfoodfacts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Nutriments {

   @JsonProperty("energy-kcal_100g")
   public int   energyKcal100g;

   @JsonProperty("product_quantity")
   public int   productQuantity;

   @JsonProperty("sodium_100g")
   public float sodium100g;
}
