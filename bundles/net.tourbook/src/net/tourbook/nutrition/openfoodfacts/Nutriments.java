package net.tourbook.nutrition.openfoodfacts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Nutriments {

   @JsonProperty("energy-kcal")
   public int   energyKcal;

   @JsonProperty("sodium")
   public float sodium;
}
