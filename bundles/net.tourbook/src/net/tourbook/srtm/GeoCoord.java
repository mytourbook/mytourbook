/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
/**
 * @author Alfred Barten
 */
package net.tourbook.srtm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.common.UI;

/***********************************************************************************
 * <pre>
 *
 * Zur Erkennung von Strings: zunächst werden Ersetzungen vorgenommen:
 *
 * blank       -> (nichts)
 * "           -> (nichts)
 * [SW] vorne  -> - vorne
 * [SW] hinten -> - vorne
 * [NEO]       -> (nichts)
 * ,           -> .
 * °           -> :
 * '           -> :
 * : hinten    -> (nichts)
 *
 * danach bleiben folgende Fälle übrig:
 *
 * Symbolisch RegEx
 *
 * -ggg:mm           ([-+]?)([0-9]{1,3}):([0-9]{1,2})
 * -gggMM            ([-+]?)([0-9]{1,3})([0-9]{2})
 * -ggg:mm:ss        ([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2})
 * -gggMMSS          ([-+]?)([0-9]{1,3})([0-9]{2})([0-9]{2})
 * -ggg:mm:ss:tt     ([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})
 * -ggg.ggggg        ([-+]?)([0-9]{1,3}\\.[0-9]+)
 * -ggg:mm.mmmmm     ([-+]?)([0-9]{1,3}):([0-9]{1,2}\\.[0-9]+)
 * -gggMM.mmmmm      ([-+]?)([0-9]{1,3})([0-9]{2}\\.[0-9]+)
 * -ggg:mm:ss.sssss  ([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2}\\.[0-9]+)
 * -gggMMSS.sssss    ([-+]?)([0-9]{1,3})([0-9]{2})([0-9]{2}\\.[0-9]+)
 *
 * dabei bedeutet:
 *
 * - ^         = plus, minus oder nichts
 * ggg ^       = ein bis drei Stellen
 * mm,ss,tt ^  = ein oder zwei Stellen
 * MM,SS ^     = exakt zwei Stellen
 * .* ^        = ein oder mehrere Nachkommastellen
 * </pre>
 ***********************************************************************************/
public class GeoCoord {

   static final int             FACTOR_SECONDS = 60;
   static final int             FACTOR_MINUTES = 60 * 60;
   static final int             FACTOR_DEGREES = 60 * 60 * 60;

   private static final int     _numPatterns;
   private static final Pattern _allCompiledPatterns[];
   private static Matcher       _matcher       = null;

   static {

      // Kommentar s. o.

      final String allRegexPatterns[] = {

            "([-+]?)([0-9]{1,3}):([0-9]{1,2})", //                            //$NON-NLS-1$
            "([-+]?)([0-9]{1,3})([0-9]{2})", //                               //$NON-NLS-1$
            "([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2})", //               //$NON-NLS-1$
            "([-+]?)([0-9]{1,3})([0-9]{2})([0-9]{2})", //                     //$NON-NLS-1$
            "([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})", //  //$NON-NLS-1$
            "([-+]?)([0-9]{1,3}\\.[0-9]+)", //                                //$NON-NLS-1$
            "([-+]?)([0-9]{1,3}):([0-9]{1,2}\\.[0-9]+)", //                   //$NON-NLS-1$
            "([-+]?)([0-9]{1,3})([0-9]{2}\\.[0-9]+)", //                      //$NON-NLS-1$
            "([-+]?)([0-9]{1,3}):([0-9]{1,2}):([0-9]{1,2}\\.[0-9]+)", //      //$NON-NLS-1$
            "([-+]?)([0-9]{1,3})([0-9]{2})([0-9]{2}\\.[0-9]+)", //            //$NON-NLS-1$
      };

      _numPatterns = allRegexPatterns.length;

      _allCompiledPatterns = new Pattern[_numPatterns];

      // compile pattern
      for (int patternIndex = 0; patternIndex < _numPatterns; patternIndex++) {
         _allCompiledPatterns[patternIndex] = Pattern.compile(allRegexPatterns[patternIndex]);
      }
   }

   char          direction;

   int           degrees;  // 1 Degrees in NS-Direction = 110.946 km
   int           minutes;  // 1 Min. in NS-Direction = 1.852 km
   int           seconds;  // 1 Sek. in NS-Direction = 30.68 m
   int           tertias;  // sechzigstel Seconds  // 1 Trz. in NS-Direction =  0.51 m

   protected int decimal;  // nur Subklassen (GeoLat, GeoLon) kennen die Variable

   // Variable doubleValue wird ausschliesslich für GPS-Dateifiles verwendet
   // (dort, damit keine Rundungsfehler beim Splitten eines großen HST-Files in
   // viele kleine entstehen)
   // Variable muss mit set(double) gesetzt und mit toStringDouble*() gelesen werden
   // _kein_ Update in updateDecimal und updateDegrees,
   // d.h. add etc. fkt. nicht!
   private double doubleValue = 0.;

   public GeoCoord() {

      degrees = 0;
      minutes = 0;
      seconds = 0;
      decimal = 0;
      tertias = 0;

      direction = directionPlus();
   }

   public double acos() {
      return Math.acos(this.toRadians());
   }

   public void add(final double d) {
      decimal += d;

      updateDegrees();
   }

   public void add(final GeoCoord a) {
      decimal += a.decimal;

      updateDegrees();
   }

   public void add(final GeoCoord c, final GeoCoord a) {
      decimal = c.decimal;
      this.add(a);
   }

   public void addSecond(final int n) {
      this.add(n * FACTOR_SECONDS);
   }

//   public int getHashkey() {
//       // tertias-genau
//      if (decimal >= 0)
//         return decimal;
//      return decimal + 134217728; // = 2^27  > 77760000 = 360 * 60 * 60 * 60
//      }

   public double asin() {
      return Math.asin(this.toRadians());
   }

   public double atan() {
      return Math.atan(this.toRadians());
   }

   public double cos() {
      return Math.cos(this.toRadians());
   }

   public char directionMinus() {
      return '?';
   }

   // dummies; siehe GeoLon / GeoLat
   public char directionPlus() {
      return '!';
   }

   public void div(final double faktor) {
      decimal /= faktor;

      updateDegrees();
   }

   public boolean equalTo(final GeoCoord c) {

      return (decimal == c.decimal);
   }

   public int getHashkeyDist() {
      // Minutes-genau; Wert < 21600; 21600^2 < 2^30
      // absichtlich grob, damit "benachbarte" Punkte in gleiche "Toepfe" fallen
      if (direction == 'N') {
         return 60 * (89 - degrees) + minutes;
      }
      if (direction == 'S') {
         return 60 * (90 + degrees) + minutes;
      }
      if (direction == 'W') {
         return 60 * (179 - degrees) + minutes;
      }
      return 60 * (180 + degrees) + minutes;

   }

   public boolean greaterOrEqual(final GeoCoord c) {

      return (decimal >= c.decimal);
   }

   public boolean greaterThen(final GeoCoord c) {

      return (decimal > c.decimal);
   }

   public boolean lessOrEqual(final GeoCoord c) {

      return (decimal <= c.decimal);
   }

   public boolean lessThen(final GeoCoord c) {

      return (decimal < c.decimal);
   }

   public void mult(final double faktor) {
      decimal *= faktor;

      updateDegrees();
   }

   private String normalize(String s) {

      // Kommentar s. o.

      //    blank       -> (nichts)
      //    "           -> (nichts)
      //    [NEO]       -> (nichts)
      s = s.replace(UI.SPACE1, UI.EMPTY_STRING)
            .replace("\"", UI.EMPTY_STRING) //$NON-NLS-1$
            .replace("N", UI.EMPTY_STRING) //$NON-NLS-1$
            .replace("E", UI.EMPTY_STRING) //$NON-NLS-1$
            .replace("O", UI.EMPTY_STRING); //$NON-NLS-1$

      //  [SW] vorne  -> - vorne
      //  [SW] hinten -> - vorne
      if (s.startsWith("S")) { //$NON-NLS-1$
         s = UI.DASH + s.substring(1);
      } else if (s.startsWith("W")) { //$NON-NLS-1$
         s = UI.DASH + s.substring(1);
      } else if (s.endsWith("S")) { //$NON-NLS-1$
         s = UI.DASH + s.substring(0, s.length() - 1);
      } else if (s.endsWith("W")) { //$NON-NLS-1$
         s = UI.DASH + s.substring(0, s.length() - 1);
      }

      //  ,           -> .
      //  °           -> :
      //  '           -> :
      s = s.replace(',', '.').replace('\u00B0', ':') // degree sign
            .replace('\'', ':');

      //  : hinten    -> (nichts)
      if (s.endsWith(UI.SYMBOL_COLON)) {
         s = s.substring(0, s.length() - 1);
      }

      return s;
   }

   public boolean notEqualTo(final GeoCoord c) {

      return (decimal != c.decimal);
   }

   public void set(final double d) {
      doubleValue = d;
      decimal = (int) (d * FACTOR_DEGREES);
      updateDegrees();
   }

   public void set(final GeoCoord c) {

      decimal = c.decimal;
      doubleValue = c.doubleValue;

      updateDegrees();
   }

   public void set(String s) {

      int pat;
      s = normalize(s);

      for (pat = 0; pat < _numPatterns; pat++) {
         _matcher = _allCompiledPatterns[pat].matcher(s);
         if (_matcher.matches()) {
            break;
         }
      }

      if (pat == _numPatterns) {
         degrees = minutes = seconds = tertias = 0;
         updateDecimal();
         return;
      }

      switch (pat) {

      case 0:
      case 1: // -ggg:mm oder -gggMM
         degrees = Integer.valueOf(_matcher.group(2));
         minutes = Integer.valueOf(_matcher.group(3));
         seconds = 0;
         tertias = 0;
         break;

      case 2:
      case 3: // -ggg:mm:ss (z.B. von toString) oder -gggMMSS

         degrees = Integer.valueOf(_matcher.group(2));
         minutes = Integer.valueOf(_matcher.group(3));
         seconds = Integer.valueOf(_matcher.group(4));
         tertias = 0;
         break;

      case 4: // -ggg:mm:ss:tt z.B. von toStringFine (mit Tertias, d. h. um Faktor 60 genauer)

         degrees = Integer.valueOf(_matcher.group(2));
         minutes = Integer.valueOf(_matcher.group(3));
         seconds = Integer.valueOf(_matcher.group(4));
         tertias = Integer.valueOf(_matcher.group(5));
         break;

      case 5: // -ggg.ggggg

         final double dg = Double.valueOf(_matcher.group(2));
         degrees = (int) dg;
         final double dgg = Math.abs(dg - degrees);
         minutes = (int) (dgg * FACTOR_SECONDS);
         seconds = (int) (dgg * FACTOR_MINUTES - minutes * FACTOR_SECONDS);
         tertias = (int) (dgg * FACTOR_DEGREES - minutes * FACTOR_MINUTES - seconds * FACTOR_SECONDS + 0.5);
         break;

      case 6:
      case 7: // -ggg:mm.mmmmm oder -gggMM.mmmmm

         degrees = Integer.valueOf(_matcher.group(2));
         final double dm = Double.valueOf(_matcher.group(3));
         minutes = (int) dm;
         final double dmm = Math.abs(dm - minutes);
         seconds = (int) (dmm * FACTOR_SECONDS);
         tertias = (int) (dmm * FACTOR_MINUTES - seconds * FACTOR_SECONDS + 0.5);
         break;

      case 8:
      case 9: // -ggg:mm:ss.sssss oder -gggMMSS.sssss

         degrees = Integer.valueOf(_matcher.group(2));
         minutes = Integer.valueOf(_matcher.group(3));
         final double ds = Double.valueOf(_matcher.group(4));
         seconds = (int) ds;
         final double dss = Math.abs(ds - seconds);
         tertias = (int) (dss * FACTOR_SECONDS + 0.5);
         break;

      default:
         break;
      }

      if (_matcher.group(1).equals(UI.DASH)) {
         direction = directionMinus();
      } else {
         direction = directionPlus();
      }
      updateDecimal();
   }

   public void setDecimal(final int d) {
      decimal = d;
      updateDegrees();
   }

   public void setDegrees(final int d) {
      degrees = d;
      updateDecimal();
   }

   public void setDegreesMinutesSecondsDirection(final int d, final int m, final int s, final char r) {
      degrees = d;
      minutes = m;
      seconds = s;
      direction = r;
      updateDecimal();
   }

   public void setDirection(final char r) {
      direction = r;
      if (direction == 'O') {
         direction = 'E';
      }
      updateDecimal();
   }

   public void setDoubleValue(final double doppel) {
      this.doubleValue = doppel;
   }

   public void setMean(final GeoCoord k1, final GeoCoord k2) {
      // auf Mittelwert von k1 und k2 setzen (1-dimensional!)
      set(k1);
      div(2.);
      final GeoCoord kHelp = new GeoCoord();
      kHelp.set(k2);
      kHelp.div(2.);
      add(kHelp);
   }

   public void setMinutes(final int m) {
      minutes = m;
      updateDecimal();
   }

   public void setSeconds(final int s) {
      seconds = s;
      updateDecimal();
   }

   public void setTertias(final int t) {
      tertias = t;
      updateDecimal();
   }

   public double sin() {
      return Math.sin(this.toRadians());
   }

   public void sub(final double d) {
      decimal -= d;

      updateDegrees();
   }

   public void sub(final GeoCoord c) {
      decimal -= c.decimal;

      updateDegrees();
   }

   public void sub(final GeoCoord c, final GeoCoord s) {
      decimal = c.decimal;
      this.sub(s);
   }

   public void subSecond(final int n) {
      this.sub(n * FACTOR_SECONDS);
   }

   public double tan() {
      return Math.tan(this.toRadians());
   }

   public double toDegrees() {
      return ((double) decimal / FACTOR_DEGREES);
   }

   public void toLeft(final GeoCoord r) {
      // auf Rasterrand zur Linken shiften
      final int raster = r.decimal;
      if (decimal < 0) {
         decimal -= raster;
      }
      decimal /= raster;
      decimal *= raster;

      updateDegrees();
   }

   public void toLeft(final GeoCoord c, final GeoCoord r) {
      // c auf Rasterrand zur Linken shiften
      decimal = c.decimal;
      this.toLeft(r);
   }

   public double toRadians() {
      return Math.toRadians((double) decimal / FACTOR_DEGREES);
   }

   public void toRight(final GeoCoord r) {
      // auf Rasterrand zur Rechten shiften
      final int raster = r.decimal;
      decimal /= raster;
      decimal *= raster;
      if (decimal >= 0) {
         decimal += raster;
      }
      updateDegrees();
   }

   public void toRight(final GeoCoord c, final GeoCoord r) {
      // c auf Rasterrand zur Rechten shiften
      decimal = c.decimal;
      this.toRight(r);
   }

   @Override
   public String toString() { // = toStringDegreesMinutesSecondsDirection()

      return UI.EMPTY_STRING
            + NumberForm.n2(degrees)
            + UI.SYMBOL_COLON
            + NumberForm.n2(minutes)
            + UI.SYMBOL_COLON
            + NumberForm.n2(seconds)
            + UI.SPACE1
            + direction;
   }

   public String toStringDegrees() {

      double d = decimal;
      d /= FACTOR_DEGREES;

      return NumberForm.f6(d);
   }

   public String toStringDegreesDirection() {

      double d = decimal;
      d /= FACTOR_DEGREES;
      if (d < 0) {
         d = -d;
      }

      return UI.EMPTY_STRING + NumberForm.f5(d) + UI.SPACE1 + direction;

   }

   public String toStringDegreesMinutesDirection() {

      double m = decimal;
      if (m < 0) {
         m = -m;
      }
      m -= degrees * FACTOR_DEGREES;
      m /= FACTOR_MINUTES;
      return UI.EMPTY_STRING + NumberForm.n2(degrees) + UI.SYMBOL_COLON + NumberForm.n2f3(m) + UI.SPACE1 + direction;
   }

   public String toStringDouble() { // nur für GPS-Datenfiles

      return NumberForm.f6(doubleValue);
   }

   public String toStringFine() { // = toStringDegreesMinutesSecondsTertiasDirection()

      return UI.EMPTY_STRING
            + NumberForm.n2(degrees)
            + UI.SYMBOL_COLON
            + NumberForm.n2(minutes)
            + UI.SYMBOL_COLON
            + NumberForm.n2(seconds)
            + UI.SYMBOL_COLON
            + NumberForm.n2(tertias)
            + UI.SPACE1
            + direction;
   }

   public void updateDecimal() {
      decimal = degrees * FACTOR_DEGREES;
      decimal += minutes * FACTOR_MINUTES;
      decimal += seconds * FACTOR_SECONDS;
      decimal += tertias;

      doubleValue = decimal;
      doubleValue /= FACTOR_DEGREES;

      if (direction == directionMinus()) {
         decimal = -decimal;
         doubleValue = -doubleValue;
      }
   }

   public void updateDegrees() {

      // optimized: dec = Math.abs(decimal);
      int dec = ((decimal < 0) ? -decimal : decimal);
      degrees = dec / FACTOR_DEGREES;

      dec -= degrees * FACTOR_DEGREES;
      minutes = dec / FACTOR_MINUTES;

      dec -= minutes * FACTOR_MINUTES;
      seconds = dec / FACTOR_SECONDS;

      dec -= seconds * FACTOR_SECONDS;
      tertias = dec;

      direction = decimal < 0 ? directionMinus() : directionPlus();

      doubleValue = decimal;
      doubleValue /= FACTOR_DEGREES;
   }

}
