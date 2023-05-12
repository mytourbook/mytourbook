package net.tourbook.device.garmin.fit;

import java.math.BigDecimal;

/**
 * Utility class with various data converters between Garmin and MT format.
 *
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class DataConverters {

   private DataConverters() {}

   public static String convertSoftwareVersion(final int softwareVersion) {

      return BigDecimal.valueOf(softwareVersion, 2).toPlainString();
   }

   /**
    * Convert m/s -> km/h
    *
    * @param speed
    * @return
    */
   public static float convertSpeed(final float speed) {

      return 3.6f * speed;
   }
}
