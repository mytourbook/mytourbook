/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ext.srtm;

import java.util.Locale;
import java.text.*;

public final class NumberForm {
   
   static NumberFormat ln1;
   static NumberFormat ln2;
   static NumberFormat ln3;
   static NumberFormat ln4;
   static NumberFormat ln5;
   static NumberFormat ln6;
   static NumberFormat ln7;

   static NumberFormat lf0;
   static NumberFormat lf1;
   static NumberFormat lf2;
   static NumberFormat lf3;
   static NumberFormat lf4;
   static NumberFormat lf5;
   static NumberFormat lf6;
   static NumberFormat lf7;
   
   static NumberFormat ln2f3;
   
   public NumberForm() {
      
      ln1 = new DecimalFormat("0"); //$NON-NLS-1$
      ln2 = new DecimalFormat("00"); //$NON-NLS-1$
      ln3 = new DecimalFormat("000"); //$NON-NLS-1$
      ln4 = new DecimalFormat("0000"); //$NON-NLS-1$
      ln5 = new DecimalFormat("00000"); //$NON-NLS-1$
      ln6 = new DecimalFormat("000000"); //$NON-NLS-1$
      ln7 = new DecimalFormat("0000000"); //$NON-NLS-1$

      // NumberFormat lf0 = new DecimalFormat("0.000000"); => Komma, weil current Locale GERMAN ist
      // Damit es Punkte und keine Kommas werden:
      lf0 = NumberFormat.getInstance(Locale.ENGLISH);
      lf1 = NumberFormat.getInstance(Locale.ENGLISH);
      lf2 = NumberFormat.getInstance(Locale.ENGLISH);
      lf3 = NumberFormat.getInstance(Locale.ENGLISH);
      lf4 = NumberFormat.getInstance(Locale.ENGLISH);
      lf5 = NumberFormat.getInstance(Locale.ENGLISH);
      lf6 = NumberFormat.getInstance(Locale.ENGLISH);
      lf7 = NumberFormat.getInstance(Locale.ENGLISH);

      lf0.setMaximumFractionDigits(0); lf0.setMinimumFractionDigits(0); lf0.setGroupingUsed(false);
      lf1.setMaximumFractionDigits(1); lf1.setMinimumFractionDigits(1); lf1.setGroupingUsed(false);
      lf2.setMaximumFractionDigits(2); lf2.setMinimumFractionDigits(2); lf2.setGroupingUsed(false);
      lf3.setMaximumFractionDigits(3); lf3.setMinimumFractionDigits(3); lf3.setGroupingUsed(false);
      lf4.setMaximumFractionDigits(4); lf4.setMinimumFractionDigits(4); lf4.setGroupingUsed(false);
      lf5.setMaximumFractionDigits(5); lf5.setMinimumFractionDigits(5); lf5.setGroupingUsed(false);
      lf6.setMaximumFractionDigits(6); lf6.setMinimumFractionDigits(6); lf6.setGroupingUsed(false);
      lf7.setMaximumFractionDigits(7); lf7.setMinimumFractionDigits(7); lf7.setGroupingUsed(false);
      
      ln2f3 = NumberFormat.getInstance(Locale.ENGLISH);
      ln2f3.setMinimumIntegerDigits(2);   ln2f3.setMaximumIntegerDigits(2);
      ln2f3.setMaximumFractionDigits(3);  ln2f3.setMinimumFractionDigits(3);
      
     }
   
   // Ganzzahlige Formate 
   static public String n1(int number) { return ln1.format(number); }
   static public String n2(int number) { return ln2.format(number); }
   static public String n3(int number) { return ln3.format(number); }
   static public String n4(int number) { return ln4.format(number); }
   static public String n5(int number) { return ln5.format(number); }
   static public String n6(int number) { return ln6.format(number); }
   static public String n7(int number) { return ln7.format(number); }
   
   static public String n1(double number) { return ln1.format(number); }
   static public String n2(double number) { return ln2.format(number); }
   static public String n3(double number) { return ln3.format(number); }
   static public String n4(double number) { return ln4.format(number); }
   static public String n5(double number) { return ln5.format(number); }
   static public String n6(double number) { return ln6.format(number); }
   static public String n7(double number) { return ln7.format(number); }
   
   // Formate für Nachkommastellen
   static public String f0(double number) { return lf0.format(number); }
   static public String f1(double number) { return lf1.format(number); }
   static public String f2(double number) { return lf2.format(number); }
   static public String f3(double number) { return lf3.format(number); }
   static public String f4(double number) { return lf4.format(number); }
   static public String f5(double number) { return lf5.format(number); }
   static public String f6(double number) { return lf6.format(number); }
   static public String f7(double number) { return lf7.format(number); }
   
   // speziell für GeoCoord nn.fff
   static public String n2f3(double number) { return ln2f3.format(number); }
      
//   public static void main(String[] args) {
//
//      NumberForm numberForm = new NumberForm();
//      double test = Math.PI;
//      int test2 = 42;
//      
//      System.out.println(NumberForm.n1(test2));
//      System.out.println(NumberForm.n2(test2));
//      System.out.println(NumberForm.n3(test2));
//      System.out.println(NumberForm.n4(test2));
//      System.out.println(NumberForm.n5(test2));
//      System.out.println(NumberForm.n6(test2));
//      System.out.println(NumberForm.n7(test2));
//      
//      System.out.println(NumberForm.n1(test));
//      System.out.println(NumberForm.n2(test));
//      System.out.println(NumberForm.n3(test));
//      System.out.println(NumberForm.n4(test));
//      System.out.println(NumberForm.n5(test));
//      System.out.println(NumberForm.n6(test));
//      System.out.println(NumberForm.n7(test));
//      
//      System.out.println(NumberForm.f0(test));
//      System.out.println(NumberForm.f1(test));
//      System.out.println(NumberForm.f2(test));
//      System.out.println(NumberForm.f3(test));
//      System.out.println(NumberForm.f4(test));
//      System.out.println(NumberForm.f5(test));
//      System.out.println(NumberForm.f6(test));
//      System.out.println(NumberForm.f7(test));
//   }

}
