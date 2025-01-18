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

/**
 * Curve x/y values 0...1
 */
public class CurveValues {

   /**
    * 0...1
    */
   public float[] allXValues;

   /**
    * 0...1
    */
   public float[] allYValues;

   public CurveValues() {

      allXValues = new float[] { 0, 1 };
      allYValues = new float[] { 0, 1 };
   }

   public CurveValues(final CurveValues curve) {

      allXValues = curve.allXValues.clone();
      allYValues = curve.allYValues.clone();
   }

   public int addKnot(final float kx, final float ky) {

      int pos = -1;
      final int numKnots = allXValues.length;

      final float[] nx = new float[numKnots + 1];
      final float[] ny = new float[numKnots + 1];

      int j = 0;

      for (int i = 0; i < numKnots; i++) {

         if (pos == -1 && allXValues[i] > kx) {

            pos = j;

            nx[j] = kx;
            ny[j] = ky;

            j++;
         }

         nx[j] = allXValues[i];
         ny[j] = allYValues[i];

         j++;
      }

      if (pos == -1) {

         pos = j;
         nx[j] = kx;
         ny[j] = ky;
      }

      allXValues = nx;
      allYValues = ny;

      return pos;
   }

   public int findKnotPos(final float kx) {

      final int numKnots = allXValues.length;

      for (int i = 0; i < numKnots; i++) {

         if (allXValues[i] > kx) {
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

      final int numKnots = allXValues.length;

      final float[] nx = new float[numKnots + 2];
      final float[] ny = new float[numKnots + 2];

      System.arraycopy(allXValues, 0, nx, 1, numKnots);
      System.arraycopy(allYValues, 0, ny, 1, numKnots);

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

   public void removeKnot(final int n) {

      final int numKnots = allXValues.length;

      if (numKnots <= 2) {
         return;
      }

      final float[] nx = new float[numKnots - 1];
      final float[] ny = new float[numKnots - 1];

      int j = 0;

      for (int i = 0; i < numKnots - 1; i++) {

         if (i == n) {
            j++;
         }

         nx[i] = allXValues[j];
         ny[i] = allYValues[j];

         j++;
      }

      allXValues = nx;
      allYValues = ny;
   }
}
