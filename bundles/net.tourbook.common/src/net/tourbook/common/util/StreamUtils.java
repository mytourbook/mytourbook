/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
package net.tourbook.common.util;

import java.util.OptionalDouble;
import java.util.stream.IntStream;

public final class StreamUtils {

   /**
    * Computes the average value for a given tour serie array of floats
    *
    * @param serie
    *           The Tour serie
    * @return The average value as a {@link Double}
    */
   public static double computeAverage(final float[] serie) {

      double averageValue = 0;

      if (serie == null) {
         return averageValue;
      }

      averageValue = computeAverage(serie, 0, serie.length);

      return averageValue;
   }

   /**
    * Computes the average value for a given tour serie array of floats,
    * start index and end index
    *
    * @param serie
    *           The Tour serie
    * @param startIndex
    *           The start index
    * @param endIndex
    *           The end index
    * @return The average value as a {@link Double}
    */
   public static double computeAverage(final float[] serie, final int startIndex, final int endIndex) {

      double averageValue = 0;

      if (serie == null) {
         return averageValue;
      }

      final OptionalDouble averageDouble = IntStream.range(startIndex, endIndex).mapToDouble(i -> serie[i]).average();

      if (averageDouble.isPresent()) {
         averageValue = averageDouble.getAsDouble();
      }

      return averageValue;
   }

}
