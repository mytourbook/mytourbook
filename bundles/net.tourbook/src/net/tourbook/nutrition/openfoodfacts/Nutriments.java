package net.tourbook.nutrition.openfoodfacts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Nutriments {

   @JsonProperty("energy-kcal_serving")
   public int   energyKcalServing;

   @JsonProperty("sodium_serving")
   public float sodiumServing;
}
