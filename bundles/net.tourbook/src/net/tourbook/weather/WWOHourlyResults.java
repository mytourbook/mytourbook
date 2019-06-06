package net.tourbook.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WWOHourlyResults {
   private String time;

   public String gettime() {
      return time;
   }
}
