/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.jhlabs.image;

import java.util.Arrays;

import net.tourbook.common.UI;

/**
 * Curve x/y values 0...1
 */
public class CurveValues {

   private static final char NL = UI.NEW_LINE;

   /**
    * 0...1
    */
   public float[]            allValuesX;

   /**
    * 0...1
    */
   public float[]            allValuesY;

   public CurveValues() {

      allValuesX = new float[] { 0, 1 };
      allValuesY = new float[] { 0, 1 };
   }

   public CurveValues(final CurveValues curve) {

      allValuesX = curve.allValuesX.clone();
      allValuesY = curve.allValuesY.clone();
   }

   public int addKnot(final float kx, final float ky) {

      int pos = -1;
      final int numKnots = allValuesX.length;

      final float[] nx = new float[numKnots + 1];
      final float[] ny = new float[numKnots + 1];

      int j = 0;

      for (int i = 0; i < numKnots; i++) {

         if (pos == -1 && allValuesX[i] > kx) {

            pos = j;

            nx[j] = kx;
            ny[j] = ky;

            j++;
         }

         nx[j] = allValuesX[i];
         ny[j] = allValuesY[i];

         j++;
      }

      if (pos == -1) {

         pos = j;
         nx[j] = kx;
         ny[j] = ky;
      }

      allValuesX = nx;
      allValuesY = ny;

      return pos;
   }

   public int findKnotPos(final float kx) {

      final int numKnots = allValuesX.length;

      for (int i = 0; i < numKnots; i++) {

         if (allValuesX[i] > kx) {
            return i;
         }
      }

      return numKnots;
   }

   /**
    * Creates 0...255 values from the 0...1 knot values
    *
    * @return
    */
   public int[] makeTable() {

      final int numKnots = allValuesX.length;

      final float[] nx = new float[numKnots + 2];
      final float[] ny = new float[numKnots + 2];

      System.arraycopy(allValuesX, 0, nx, 1, numKnots);
      System.arraycopy(allValuesY, 0, ny, 1, numKnots);

      nx[0] = nx[1];
      ny[0] = ny[1];
      nx[numKnots + 1] = nx[numKnots];
      ny[numKnots + 1] = ny[numKnots];

      final int[] table = new int[256];

      // if first knot is > 0 fill the table with y position
      if (nx[0] > 0) {

         final int nxStart = (int) (nx[0] * 255);
         final int nyStart = (int) (ny[0] * 255);

         for (int i = 0; i <= nxStart; i++) {
            table[i] = nyStart;
         }
      }

      // if last knot is < 1 fill the table with y position
      if (nx[numKnots] < 1) {

         final int nxStart = (int) (nx[numKnots] * 255);
         final int nyStart = (int) (ny[numKnots] * 255);

         for (int i = nxStart; i <= 255; i++) {
            table[i] = nyStart;
         }
      }

      for (int i = 0; i < 2048; i++) {

         final float f = i / 2048.0f;

         int x = (int) (255 * ImageMath.splineClamped(f, nx.length, nx) + 0.5f);
         int y = (int) (255 * ImageMath.spline(f, nx.length, ny) + 0.5f);

         x = ImageMath.clamp(x, 0, 255);
         y = ImageMath.clamp(y, 0, 255);

         table[x] = y;
      }

      return table;
   }

   public void removeKnot(final int deleteIndex) {

      final int numKnots = allValuesX.length;

      if (numKnots <= 2) {
         return;
      }

      final float[] newValuesX = new float[numKnots - 1];
      final float[] newValuesY = new float[numKnots - 1];

      int oldIndex = 0;

      for (int newIndex = 0; newIndex < numKnots - 1; newIndex++) {

         if (newIndex == deleteIndex) {
            oldIndex++;
         }

         newValuesX[newIndex] = allValuesX[oldIndex];
         newValuesY[newIndex] = allValuesY[oldIndex];

         oldIndex++;
      }

      allValuesX = newValuesX;
      allValuesY = newValuesY;
   }

//    private void sortKnots() {
//        int numKnots = x.length;
//        for (int i = 1; i < numKnots - 1; i++) {
//            for (int j = 1; j < i; j++) {
//                if (x[i] < x[j]) {
//                    float t = x[i];
//                    x[i] = x[j];
//                    x[j] = t;
//                    t = y[i];
//                    y[i] = y[j];
//                    y[j] = t;
//                }
//            }
//        }
//    }

   @Override
   public String toString() {

      final int maxLen = 5;

      return UI.EMPTY_STRING

            + "CurveValues" + NL //$NON-NLS-1$

            + " allValuesX = " + (allValuesX != null //$NON-NLS-1$
                  ? Arrays.toString(Arrays.copyOf(allValuesX, Math.min(allValuesX.length, maxLen)))
                  : null) + NL

            + " allValuesY = " + (allValuesY != null //$NON-NLS-1$
                  ? Arrays.toString(Arrays.copyOf(allValuesY, Math.min(allValuesY.length, maxLen)))
                  : null)

      ;
   }
}
