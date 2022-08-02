/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map25;

import net.tourbook.common.UI;

import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLMatrix;

public class Util extends BucketRenderer {

   private static final String NL = UI.NEW_LINE1;
   private static int          _counter;

   public static void dumpMatrix(final GLMatrix matrix, final boolean isWithCounter) {

      final float[] matrixValues = new float[16];

      matrix.get(matrixValues);

      final StringBuilder sb = new StringBuilder();

      for (int matrixRow = 0; matrixRow < 4; matrixRow++) {

         sb.append(String.format("%9.4f  %9.4f  %9.4f  %9.4f" + NL, //$NON-NLS-1$
               matrixValues[matrixRow + 0],
               matrixValues[matrixRow + 4],
               matrixValues[matrixRow + 8],
               matrixValues[matrixRow + 12]));
      }

      if (isWithCounter) {

         System.out.println(_counter++ + NL + sb);
         // TODO remove SYSTEM.OUT.PRINTLN

      } else {

         System.out.println(sb);
         // TODO remove SYSTEM.OUT.PRINTLN
      }
   }

}
