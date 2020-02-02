package net.tourbook.math;

import de.byteholder.geoclipse.map.UI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

/*
*   Class   Fmath
*
*   USAGE:  Mathematical class that supplements java.lang.Math and contains:
*               the main physical constants
*               trigonemetric functions absent from java.lang.Math
*               some useful additional mathematical functions
*               some conversion functions
*
*   WRITTEN BY: Dr Michael Thomas Flanagan
*
*   DATE:    June 2002
*   AMENDED: 6 January 2006, 12 April 2006, 5 May 2006, 28 July 2006, 27 December 2006,
*            29 March 2007, 29 April 2007, 2,9,15 & 26 June 2007, 20 October 2007, 4-6 December 2007
*            27 February 2008, 25 April 2008, 26 April 2008, 13 May 2008, 25/26 May 2008, 3-7 July 2008
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's Java library on-line web pages:
*   http://www.ee.ucl.ac.uk/~mflanaga/java/
*   http://www.ee.ucl.ac.uk/~mflanaga/java/Fmath.html
*
*   Copyright (c) 2002 - 2008
*
*   PERMISSION TO COPY:
*   Permission to use, copy and modify this software and its documentation for
*   NON-COMMERCIAL purposes is granted, without fee, provided that an acknowledgement
*   to the author, Michael Thomas Flanagan at www.ee.ucl.ac.uk/~mflanaga, appears in all copies.
*
*   Dr Michael Thomas Flanagan makes no representations about the suitability
*   or fitness of the software for any or for a particular purpose.
*   Michael Thomas Flanagan shall not be liable for any damages suffered
*   as a result of using, modifying or distributing this software or its derivatives.
*
***************************************************************************************/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Fmath{

        // PHYSICAL CONSTANTS

        public static final double N_AVAGADRO = 6.0221419947e23;        /*      mol^-1          */
        public static final double K_BOLTZMANN = 1.380650324e-23;       /*      J K^-1          */
        public static final double H_PLANCK = 6.6260687652e-34;         /*      J s             */
        public static final double H_PLANCK_RED = H_PLANCK/(2*Math.PI); /*      J s             */
        public static final double C_LIGHT = 2.99792458e8;              /*      m s^-1          */
        public static final double R_GAS = 8.31447215;                  /*      J K^-1 mol^-1   */
        public static final double F_FARADAY = 9.6485341539e4;          /*      C mol^-1        */
        public static final double T_ABS = -273.15;                     /*      Celsius         */
        public static final double Q_ELECTRON = -1.60217646263e-19;     /*      C               */
        public static final double M_ELECTRON = 9.1093818872e-31;       /*      kg              */
        public static final double M_PROTON = 1.6726215813e-27;         /*      kg              */
        public static final double M_NEUTRON = 1.6749271613e-27;        /*      kg              */
        public static final double EPSILON_0 = 8.854187817e-12;         /*      F m^-1          */
        public static final double MU_0 = Math.PI*4e-7;                 /*      H m^-1 (N A^-2) */

        // MATHEMATICAL CONSTANTS
        public static final double EULER_CONSTANT_GAMMA = 0.5772156649015627;
        public static final double PI = Math.PI;                        /*  3.141592653589793D  */
        public static final double E = Math.E;                          /*  2.718281828459045D  */

        // HashMap for 'arithmetic integer' recognition nmethod
        private static final Map<Object,Object> integers = new HashMap<>();
        static{
            integers.put(Integer.class, BigDecimal.valueOf(Integer.MAX_VALUE));
            integers.put(Long.class, BigDecimal.valueOf(Long.MAX_VALUE));
            integers.put(Byte.class, BigDecimal.valueOf(Byte.MAX_VALUE));
            integers.put(Short.class, BigDecimal.valueOf(Short.MAX_VALUE));
            integers.put(BigInteger.class, BigDecimal.valueOf(-1));
        }

        // METHODS

        // Inverse cosine
        // Fmath.asin Checks limits - Java Math.asin returns NaN if without limits
        public static double acos(final double a){
            if(a<-1.0D || a>1.0D)
             {
               throw new IllegalArgumentException("Fmath.acos argument (" + a + ") must be >= -1.0 and <= 1.0"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return Math.acos(a);
        }

        // Inverse hyperbolic cosine of a double number
        public static double acosh(final double a){
            if(a<1.0D)
             {
               throw new IllegalArgumentException("acosh real number argument (" + a + ") must be >= 1"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return Math.log(a+Math.sqrt(a*a-1.0D));
        }

        // Inverse cotangent
        public static double acot(final double a){
            return Math.atan(1.0D/a);
        }

        // Inverse cotangent - ratio numerator and denominator provided
        public static double acot2(final double a, final double b){
            return Math.atan2(b, a);
        }

        // Inverse hyperbolic cotangent of a double number
        public static double acoth(double a){
            double sgn = 1.0D;
            if(a<0.0D){
                sgn = -1.0D;
                a = -a;
            }
            if(a<1.0D)
             {
               throw new IllegalArgumentException("acoth real number argument (" + sgn*a + ") must be <= -1 or >= 1"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return 0.5D*sgn*(Math.log(1.0D + a)-Math.log(a - 1.0D));
        }

        // Inverse coversine
        public static double acovers(final double a){
            if(a<0.0D && a>2.0D)
             {
               throw new IllegalArgumentException("acovers argument (" + a + ") must be <= 2 and >= 0"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return Math.asin(1.0D - a);
        }

        // Inverse cosecant
        public static double acsc(final double a){
            if(a<1.0D && a>-1.0D)
             {
               throw new IllegalArgumentException("acsc argument (" + a + ") must be >= 1 or <= -1"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return Math.asin(1.0/a);
        }

        // Inverse hyperbolic cosecant of a double number
        public static double acsch(double a){
            double sgn = 1.0D;
            if(a<0.0D){
                sgn = -1.0D;
                a = -a;
            }
            return 0.5D*sgn*(Math.log(1.0/a + Math.sqrt(1.0D/(a*a) + 1.0D)));
        }

        // Inverse exsecant
        public static double aexsec(final double a){
            if(a<0.0D && a>-2.0D)
             {
               throw new IllegalArgumentException("aexsec argument (" + a + ") must be >= 0.0 and <= -2"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return Math.asin(1.0D/(1.0D + a));
        }

        // Inverse haversine
        public static double ahav(final double a){
            if(a<0.0D && a>1.0D)
             {
               throw new IllegalArgumentException("ahav argument (" + a + ") must be >= 0 and <= 1"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return Fmath.acos(1.0D - 2.0D*a);
        }

        // Angle (in radians) between sides sideA and sideB given all side lengths of a triangle
        public static double angle(final double sideAC, final double sideBC, final double sideAB){

            final double ccos = Fmath.cos(sideAC, sideBC, sideAB);
            return Math.acos(ccos);
        }

        // Angle (in radians) subtended at coordinate C
        // given x, y coordinates of all apices, A, B and C, of a triangle
        public static double angle(final double xAtA, final double yAtA, final double xAtB, final double yAtB, final double xAtC, final double yAtC){

            final double ccos = Fmath.cos(xAtA, yAtA, xAtB, yAtB, xAtC, yAtC);
            return Math.acos(ccos);
        }

        // Base e antilog of a double
        public static double antilog(final double x){
            return Math.exp(x);
        }

        // Base e antilog of a float
        public static float antilog(final float x){
            return (float)Math.exp(x);
        }

        // Base 10 antilog of a double
        public static double antilog10(final double x){
            return Math.pow(10.0D, x);
        }

        // Base 10 antilog of a float
        public static float antilog10(final float x){
            return (float)Math.pow(10.0D, x);
        }

        // Base 2 antilog of a double
        public static double antilog2(final double x){
            return Math.pow(2.0D, x);
        }

        // Base 2 antilog of a float
        public static float antilog2(final float x){
            return (float)Math.pow(2.0D, x);
        }

        // ABSOLUTE VALUE OF ARRAY ELEMENTS  (deprecated - see ArryMaths class)
        // return absolute values of an array of doubles
        public static double[] arrayAbs(final double[] aa){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
               bb[i] = Math.abs(aa[i]);
            }
            return bb;
        }

        // return absolute values of an array of floats
        public static float[] arrayAbs(final float[] aa){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++){
               bb[i] = Math.abs(aa[i]);
            }
            return bb;
        }

        // return absolute values of an array of int
        public static int[] arrayAbs(final int[] aa){
            final int n = aa.length;
            final int[] bb = new int[n];
            for(int i=0; i<n; i++){
               bb[i] = Math.abs(aa[i]);
            }
            return bb;
        }

        // return absolute values of an array of long
        public static long[] arrayAbs(final long[] aa){
            final int n = aa.length;
            final long[] bb = new long[n];
            for(int i=0; i<n; i++){
               bb[i] = Math.abs(aa[i]);
            }
            return bb;
        }

        // MULTIPLY ARRAY ELEMENTS BY A CONSTANT  (deprecated - see ArryMaths class)
        // multiply all elements by a constant double[] by double -> double[]
        public static double[] arrayMultByConstant(final double[] aa, final double constant){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i]*constant;
            }
            return bb;
        }

        // multiply all elements by a constant double[] by int -> double[]
        public static double[] arrayMultByConstant(final double[] aa, final int constant){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i]*constant;
            }
            return bb;
        }

        // multiply all elements by a constant int[] by double -> double[]
        public static double[] arrayMultByConstant(final int[] aa, final double constant){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i]*constant;
            }
            return bb;
        }

        // multiply all elements by a constant int[] by int -> double[]
        public static double[] arrayMultByConstant(final int[] aa, final int constant){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i]*constant;
            }
            return bb;
        }

        // Sum of all positive array elements - long array
        public static long arrayPositiveElementsSum(final long[]array){
            long sum = 0L;
            for(final long i:array) {
               if(i>0) {
                  sum += i;
               }
            }
            return sum;
        }



        // PRODUCT OF ALL ELEMENTS  (deprecated - see ArryMaths class)
        // Product of all array elements - double array
        public static double arrayProduct(final double[]array){
            double product = 1.0D;
            for(final double i:array) {
               product *= i;
            }
            return product;
        }

        // Product of all array elements - float array
        public static float arrayProduct(final float[]array){
            float product = 1.0F;
            for(final float i:array) {
               product *= i;
            }
            return product;
        }

        // Product of all array elements - int array
        public static int arrayProduct(final int[]array){
            int product = 1;
            for(final int i:array) {
               product *= i;
            }
            return product;
        }


        // Product of all array elements - long array
        public static long arrayProduct(final long[]array){
            long product = 1L;
            for(final long i:array) {
               product *= i;
            }
            return product;
        }

        // SUM OF ALL ELEMENTS  (deprecated - see ArryMaths class)
        // Sum of all array elements - double array
        public static double arraySum(final double[]array){
            double sum = 0.0D;
            for(final double i:array) {
               sum += i;
            }
            return sum;
        }

        // Sum of all array elements - float array
        public static float arraySum(final float[]array){
            float sum = 0.0F;
            for(final float i:array) {
               sum += i;
            }
            return sum;
        }

        // Sum of all array elements - int array
        public static int arraySum(final int[]array){
            int sum = 0;
            for(final int i:array) {
               sum += i;
            }
            return sum;
        }

        // ADDITIONAL TRIGONOMETRIC FUNCTIONS

        // Sum of all array elements - long array
        public static long arraySum(final long[]array){
            long sum = 0L;
            for(final long i:array) {
               sum += i;
            }
            return sum;
        }

        // Inverse secant
        public static double asec(final double a){
            if(a<1.0D && a>-1.0D)
             {
               throw new IllegalArgumentException("asec argument (" + a + ") must be >= 1 or <= -1"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return Math.acos(1.0/a);
        }

        // Inverse hyperbolic secant of a double number
        public static double asech(final double a){
            if(a>1.0D || a<0.0D)
             {
               throw new IllegalArgumentException("asech real number argument (" + a + ") must be >= 0 and <= 1"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return 0.5D*(Math.log(1.0D/a + Math.sqrt(1.0D/(a*a) - 1.0D)));
        }

        // Inverse sine
        // Fmath.asin Checks limits - Java Math.asin returns NaN if without limits
        public static double asin(final double a){
            if(a<-1.0D && a>1.0D)
             {
               throw new IllegalArgumentException("Fmath.asin argument (" + a + ") must be >= -1.0 and <= 1.0"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return Math.asin(a);
        }

        // Inverse hyperbolic sine of a double number
        public static double asinh(double a){
            double sgn = 1.0D;
            if(a<0.0D){
                sgn = -1.0D;
                a = -a;
            }
            return sgn*Math.log(a+Math.sqrt(a*a+1.0D));
        }

        // Inverse tangent
        // for completion - returns Math.atan(arg)
        public static double atan(final double a){
            return Math.atan(a);
        }

        // Inverse tangent - ratio numerator and denominator provided
        // for completion - returns Math.atan2(arg)
        public static double atan2(final double a, final double b){
            return Math.atan2(a, b);
        }

        // Inverse hyperbolic tangent of a double number
        public static double atanh(double a){
            double sgn = 1.0D;
            if(a<0.0D){
                sgn = -1.0D;
                a = -a;
            }
            if(a>1.0D)
             {
               throw new IllegalArgumentException("atanh real number argument (" + sgn*a + ") must be >= -1 and <= 1"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return 0.5D*sgn*(Math.log(1.0D + a)-Math.log(1.0D - a));
        }

        // Inverse  versine
        public static double avers(final double a){
            if(a<0.0D && a>2.0D)
             {
               throw new IllegalArgumentException("avers argument (" + a + ") must be <= 2 and >= 0"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return Math.acos(1.0D - a);
        }

        // recast an array of byte as double
        public static double[] byteTOdouble(final byte[] aa){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

         // recast an array of byte as float
        public static float[] byteTOfloat(final byte[] aa){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // recast an array of byte as int
        public static int[] byteTOint(final byte[] aa){
            final int n = aa.length;
            final int[] bb = new int[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // recast an array of byte as long
        public static long[] byteTOlong(final byte[] aa){
            final int n = aa.length;
            final long[] bb = new long[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // recast an array of byte as short
        public static short[] byteTOshort(final byte[] aa){
            final int n = aa.length;
            final short[] bb = new short[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // Calculates body mass index (BMI) from height (ft) and weight (lbs)
        public static double calcBMIimperial(double height, double weight){
                height = Fmath.footToMetre(height);
                weight = Fmath.poundToKg(weight);
            return  weight/(height*height);
        }

        // Calculates body mass index (BMI) from height (m) and weight (kg)
        public static double calcBMImetric(final double height, final double weight){
            return  weight/(height*height);
        }

        // Calculates weight (lbs) to give a specified BMI for a given height (ft)
        public static double calcWeightFromBMIimperial(final double bmi, double height){
            height = Fmath.footToMetre(height);
            double weight = bmi*height*height;
            weight = Fmath.kgToPound(weight);
            return  weight;
        }

        // Calculates weight (kg) to give a specified BMI for a given height (m)
        public static double calcWeightFromBMImetric(final double bmi, final double height){
            return bmi*height*height;
        }

        // Converts calories to Joules
        public static double calorieToJoule(final double cal){
            return  cal*4.1868;
        }

        // Converts Celsius to Fahrenheit
        public static double celsiusToFahren(final double cels){
            return  cels*(9.0/5.0)+32.0;
        }

        // Converts Celsius to Kelvin
        public static double celsiusToKelvin(final double cels){
            return  cels-T_ABS;
        }

        // Returns 0 if x == y
        // Returns -1 if x < y
        // Returns 1 if x > y
        // x and y are short
        public static int compare(final byte x, final byte y){
            final Byte X = new Byte(x);
            final Byte Y = new Byte(y);
            return X.compareTo(Y);
        }

        // COMPARISONS
        // Returns 0 if x == y
        // Returns -1 if x < y
        // Returns 1 if x > y
        // x and y are double
        public static int compare(final double x, final double y){
            final Double X = new Double(x);
            final Double Y = new Double(y);
            return X.compareTo(Y);
        }

        // Returns 0 if x == y
        // Returns -1 if x < y
        // Returns 1 if x > y
        // x and y are float
        public static int compare(final float x, final float y){
            final Float X = new Float(x);
            final Float Y = new Float(y);
            return X.compareTo(Y);
        }

        // Returns 0 if x == y
        // Returns -1 if x < y
        // Returns 1 if x > y
        // x and y are int
        public static int compare(final int x, final int y){
            final Integer X = new Integer(x);
            final Integer Y = new Integer(y);
            return X.compareTo(Y);
        }

        // Returns 0 if x == y
        // Returns -1 if x < y
        // Returns 1 if x > y
        // x and y are long
        public static int compare(final long x, final long y){
            final Long X = new Long(x);
            final Long Y = new Long(y);
            return X.compareTo(Y);
        }

        // Returns 0 if x == y
        // Returns -1 if x < y
        // Returns 1 if x > y
        // x and y are short
        public static int compare(final short x, final short y){
            final Short X = new Short(x);
            final Short Y = new Short(y);
            return X.compareTo(Y);
        }

        // Concatenate two byte arrays
        public static byte[] concatenate(final byte[] aa, final byte[] bb){
            final int aLen = aa.length;
            final int bLen = bb.length;
            final int cLen = aLen + bLen;
            final byte[] cc = new byte[cLen];
            for(int i=0; i<aLen; i++){
                cc[i] = aa[i];
            }
            for(int i=0; i<bLen; i++){
                cc[i+aLen] = bb[i];
            }

            return cc;
        }

        // Concatenate two char arrays
        public static char[] concatenate(final char[] aa, final char[] bb){
            final int aLen = aa.length;
            final int bLen = bb.length;
            final int cLen = aLen + bLen;
            final char[] cc = new char[cLen];
            for(int i=0; i<aLen; i++){
                cc[i] = aa[i];
           }
            for(int i=0; i<bLen; i++){
                cc[i+aLen] = bb[i];
            }

            return cc;
        }

        // CONCATENATE TWO ARRAYS  (deprecated - see ArryMaths class)
        // Concatenate two double arrays
        public static double[] concatenate(final double[] aa, final double[] bb){
            final int aLen = aa.length;
            final int bLen = bb.length;
            final int cLen = aLen + bLen;
            final double[] cc = new double[cLen];
            for(int i=0; i<aLen; i++){
                cc[i] = aa[i];
            }
            for(int i=0; i<bLen; i++){
                cc[i+aLen] = bb[i];
            }
            return cc;
        }

        // Concatenate two float arrays
        public static float[] concatenate(final float[] aa, final float[] bb){
            final int aLen = aa.length;
            final int bLen = bb.length;
            final int cLen = aLen + bLen;
            final float[] cc = new float[cLen];
            for(int i=0; i<aLen; i++){
                cc[i] = aa[i];
            }
            for(int i=0; i<bLen; i++){
                cc[i+aLen] = bb[i];
            }

            return cc;
        }

        // Concatenate two int arrays
        public static int[] concatenate(final int[] aa, final int[] bb){
            final int aLen = aa.length;
            final int bLen = bb.length;
            final int cLen = aLen + bLen;
            final int[] cc = new int[cLen];
            for(int i=0; i<aLen; i++){
                cc[i] = aa[i];
            }
            for(int i=0; i<bLen; i++){
                cc[i+aLen] = bb[i];
            }

            return cc;
        }

        // Concatenate two long arrays
        public static long[] concatenate(final long[] aa, final long[] bb){
            final int aLen = aa.length;
            final int bLen = bb.length;
            final int cLen = aLen + bLen;
            final long[] cc = new long[cLen];
            for(int i=0; i<aLen; i++){
                cc[i] = aa[i];
            }
            for(int i=0; i<bLen; i++){
                cc[i+aLen] = bb[i];
            }

            return cc;
        }

        // Concatenate two Object arrays
        public static Object[] concatenate(final Object[] aa, final Object[] bb){
            final int aLen = aa.length;
            final int bLen = bb.length;
            final int cLen = aLen + bLen;
            final Object[] cc = new Object[cLen];
            for(int i=0; i<aLen; i++){
                cc[i] = aa[i];
             }
            for(int i=0; i<bLen; i++){
                cc[i+aLen] = bb[i];
            }

            return cc;
        }

        // Concatenate two short arrays
        public static short[] concatenate(final short[] aa, final short[] bb){
            final int aLen = aa.length;
            final int bLen = bb.length;
            final int cLen = aLen + bLen;
            final short[] cc = new short[cLen];
            for(int i=0; i<aLen; i++){
                cc[i] = aa[i];
            }
            for(int i=0; i<bLen; i++){
                cc[i+aLen] = bb[i];
            }
            return cc;
        }

        // Concatenate two String arrays
        public static String[] concatenate(final String[] aa, final String[] bb){
            final int aLen = aa.length;
            final int bLen = bb.length;
            final int cLen = aLen + bLen;
            final String[] cc = new String[cLen];
            for(int i=0; i<aLen; i++){
                cc[i] = aa[i];
            }
            for(int i=0; i<bLen; i++){
                cc[i+aLen] = bb[i];
            }

            return cc;
        }

        // COPY OF AN OBJECT (deprecated - see Conv class)
        // Returns a copy of the object
        // An exception will be thrown if an attempt to copy a non-serialisable object is made.
        // Taken, with minor changes,  from { Java Techniques }
        // http://javatechniques.com/blog/
        public static Object copyObject(final Object obj) {
            Object objCopy = null;
            try {
                // Write the object out to a byte array
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(obj);
                oos.flush();
                oos.close();
                // Make an input stream from the byte array and
                // read a copy of the object back in.
                final ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray()));
                objCopy = ois.readObject();
            }
            catch(final IOException e) {
                e.printStackTrace();
            }
            catch(final ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            }
            return objCopy;
        }

        // Cosine given angle in radians
         // for completion - returns Java Math.cos(arg)
        public static double cos(final double arg){
            return Math.cos(arg);
        }

        // Cosine of angle between sides sideA and sideB given all side lengths of a triangle
        public static double cos(final double sideAC, final double sideBC, final double sideAB){
            return 0.5D*(sideAC/sideBC + sideBC/sideAC - (sideAB/sideAC)*(sideAB/sideBC));
        }

        // Cosine of angle subtended at coordinate C
        // given x, y coordinates of all apices, A, B and C, of a triangle
        public static double cos(final double xAtA, final double yAtA, final double xAtB, final double yAtB, final double xAtC, final double yAtC){
            final double sideAC = Fmath.hypot(xAtA - xAtC, yAtA - yAtC);
            final double sideBC = Fmath.hypot(xAtB - xAtC, yAtB - yAtC);
            final double sideAB = Fmath.hypot(xAtA - xAtB, yAtA - yAtB);
            return Fmath.cos(sideAC, sideBC, sideAB);
        }

        //Hyperbolic cosine of a double number
        public static double cosh(final double a){
            return 0.5D*(Math.exp(a)+Math.exp(-a));
        }

        // Cotangent
        public static double cot(final double a){
            return 1.0D/Math.tan(a);
        }

        //Hyperbolic cotangent of a double number
        public static double coth(final double a){
            return 1.0D/tanh(a);
        }

        // Coversine
        public static double covers(final double a){
            return (1.0D - Math.sin(a));
        }

        // Cosecant
        public static double csc(final double a){
            return 1.0D/Math.sin(a);
        }

        //Hyperbolic cosecant of a double number
        public static double csch(final double a){
                return 1.0D/sinh(a);
        }

    // Converts American cup to UK pint
     public static double cupUStoPintUK(final double cupUS){
         return  cupUS*0.417;
     }

    // COMPUTER TIME
     // Returns milliseconds since 0 hours 0 minutes 0 seconds on 1 Jan 1970
     public static long dateToJavaMilliS(final int year, final int month, final int day, final int hour, final int min, final int sec){

         final long[] monthDays = {0L, 31L, 28L, 31L, 30L, 31L, 30L, 31L, 31L, 30L, 31L, 30L, 31L};
         long ms = 0L;

         long yearDiff = 0L;
         int yearTest = year-1;
         while(yearTest>=1970){
             yearDiff += 365;
             if(Fmath.leapYear(yearTest)) {
               yearDiff++;
            }
             yearTest--;
         }
         yearDiff *= 24L*60L*60L*1000L;

         long monthDiff = 0L;
         int monthTest = month -1;
         while(monthTest>0){
             monthDiff += monthDays[monthTest];
             if(Fmath.leapYear(year)) {
               monthDiff++;
            }
             monthTest--;
         }

         monthDiff *= 24L*60L*60L*1000L;

         ms = yearDiff + monthDiff + day*24L*60L*60L*1000L + hour*60L*60L*1000L + min*60L*1000L + sec*1000L;

         return ms;
     }

    // Converts degrees to radians
     public static double degToRad(final double deg){
         return  deg*Math.PI/180.0D;
     }

        // recast an array of double as int
        // BEWARE OF LOSS OF PRECISION
        public static int[] doubleTOint(final double[] aa){
            final int n = aa.length;
            final int[] bb = new int[n];
            for(int i=0; i<n; i++){
               bb[i] = (int)aa[i];
            }
            return bb;
        }

        // Converts electron volts(eV) to corresponding wavelength in nm
        public static double evToNm(final double ev){
            return  1e+9*C_LIGHT/(-ev*Q_ELECTRON/H_PLANCK);
        }

        // Exsecant
        public static double exsec(final double a){
            return (1.0/Math.cos(a)-1.0D);
        }

        // factorial of n
        // Argument is of type BigDecimal but must be, numerically, an integer
        public static BigDecimal factorial(final BigDecimal n){
            if(n.compareTo(BigDecimal.ZERO)==-1 || !Fmath.isInteger(n))
             {
               throw new IllegalArgumentException("\nn must be a positive integer\nIs a Gamma funtion [Fmath.gamma(x)] more appropriate?"); //$NON-NLS-1$
            }
            BigDecimal one = BigDecimal.ONE;
            BigDecimal f = one;
            BigDecimal iCount = new BigDecimal(2.0D);
            while(iCount.compareTo(n)!=1){
                f = f.multiply(iCount);
                iCount = iCount.add(one);
            }
            one = null;
            iCount = null;
            return f;
        }

        // factorial of n
        // Argument is of type BigInteger
        public static BigInteger factorial(final BigInteger n){
            if(n.compareTo(BigInteger.ZERO)==-1)
             {
               throw new IllegalArgumentException("\nn must be a positive integer\nIs a Gamma funtion [Fmath.gamma(x)] more appropriate?"); //$NON-NLS-1$
            }
            BigInteger one = BigInteger.ONE;
            BigInteger f = one;
            BigInteger iCount = new BigInteger("2"); //$NON-NLS-1$
            while(iCount.compareTo(n)!=1){
                f = f.multiply(iCount);
                iCount = iCount.add(one);
            }
            one = null;
            iCount = null;
            return f;
        }

        // factorial of n
        // Argument is of type double but must be, numerically, an integer
        // factorial returned as double but is, numerically, should be an integer
        // numerical rounding may makes this an approximation after n = 21
        public static double factorial(final double n){
            if(n<0.0 || (n-Math.floor(n))!=0)
             {
               throw new IllegalArgumentException("\nn must be a positive integer\nIs a Gamma funtion [Fmath.gamma(x)] more appropriate?"); //$NON-NLS-1$
            }
            double f = 1.0D;
            double iCount = 2.0D;
            while(iCount<=n){
                f*=iCount;
                iCount += 1.0D;
            }
            return f;
        }


        // FACTORIALS
        // factorial of n
        // argument and return are integer, therefore limited to 0<=n<=12
        // see below for long and double arguments
        public static int factorial(final int n){
            if(n<0)
             {
               throw new IllegalArgumentException("n must be a positive integer"); //$NON-NLS-1$
            }
            if(n>12)
             {
               throw new IllegalArgumentException("n must less than 13 to avoid integer overflow\nTry long or double argument"); //$NON-NLS-1$
            }
            int f = 1;
            for(int i=2; i<=n; i++) {
               f*=i;
            }
            return f;
        }

        // factorial of n
        // argument and return are long, therefore limited to 0<=n<=20
        // see below for double argument
        public static long factorial(final long n){
            if(n<0)
             {
               throw new IllegalArgumentException("n must be a positive integer"); //$NON-NLS-1$
            }
            if(n>20)
             {
               throw new IllegalArgumentException("n must less than 21 to avoid long integer overflow\nTry double argument"); //$NON-NLS-1$
            }
            long f = 1;
            long iCount = 2L;
            while(iCount<=n){
                f*=iCount;
                iCount += 1L;
            }
            return f;
        }

        // Converts Fahrenheit to Celsius
        public static double fahrenToCelsius(final double fahr){
            return  (fahr-32.0)*5.0/9.0;
        }

        // RECAST ARRAY TYPE  (deprecated - see Conv class)
        // recast an array of float as doubles
        public static double[] floatTOdouble(final float[] aa){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // Converts UK fluid ounce to American fluid ounce
        public static double fluidOunceUKtoUS(final double flOzUK){
            return  flOzUK*0.961;
        }

        // Converts American fluid ounce to UK fluid ounce
        public static double fluidOunceUStoUK(final double flOzUS){
            return  flOzUS*1.041;
        }

        // Converts feet to metres
        public static double footToMetre(final double ft){
            return  ft*0.3048;
        }

        // Converts frequency (Hz) to radial frequency
        public static double frequencyToRadialFrequency(final double frequency){
            return  2.0D*Math.PI*frequency;
        }

        // Converts UK gallons per mile to litres per kilometre
        public static double gallonPerMileToLitrePerKm(final double gallPmile){
            return  gallPmile*2.825;
        }

        // Converts UK gallons to litres
        public static double gallonToLitre(final double gall){
            return  gall*4.546;
        }

        // Converts UK gallon to American gallon
        public static double gallonUKtoUS(final double gallonUK){
            return  gallonUK*1.201;
        }

        // Converts American gallon to UK gallon
        public static double gallonUStoUK(final double gallonUS){
            return  gallonUS*0.833;
        }

        // Converts grams to ounces
        public static double gramToOunce(final double gm){
            return  gm/28.3459;
        }


        // Haversine
        public static double hav(final double a){
            return 0.5D*Fmath.vers(a);
        }

        // Returns the length of the hypotenuse of a and b
        // i.e. sqrt(a*a+b*b) [without unecessary overflow or underflow]
        // double version
        public static double hypot(final double aa, final double bb){
            final double amod=Math.abs(aa);
            final double bmod=Math.abs(bb);
            double cc = 0.0D, ratio = 0.0D;
            if(amod==0.0){
                cc=bmod;
            }
            else{
                if(bmod==0.0){
                    cc=amod;
                }
                else{
                    if(amod>=bmod){
                        ratio=bmod/amod;
                        cc=amod*Math.sqrt(1.0 + ratio*ratio);
                    }
                    else{
                        ratio=amod/bmod;
                        cc=bmod*Math.sqrt(1.0 + ratio*ratio);
                    }
                }
            }
            return cc;
        }

        // Returns the length of the hypotenuse of a and b
        // i.e. sqrt(a*a+b*b) [without unecessary overflow or underflow]
        // float version
        public static float hypot(final float aa, final float bb){
            return (float) hypot((double) aa, (double) bb);
        }

        // Converts inches to millimetres
        public static double inchToMillimetre(final double in){
            return  in*25.4;
        }

        // finds the index of the first occurence of the element equal to a given value in an array of bytes
        // returns -1 if none found
        public static int indexOf(final byte[] array, final byte value){
            int index = -1;
            boolean test = true;
            int counter = 0;
            while(test){
                if(array[counter]==value){
                    index = counter;
                    test = false;
                }
                else{
                    counter++;
                    if(counter>=array.length) {
                     test = false;
                  }
                }
            }
            return index;
        }

        // finds the index of the first occurence of the element equal to a given value in an array of chars
        // returns -1 if none found
        public static int indexOf(final char[] array, final char value){
            int index = -1;
            boolean test = true;
            int counter = 0;
            while(test){
                if(array[counter]==value){
                    index = counter;
                    test = false;
                }
                else{
                    counter++;
                    if(counter>=array.length) {
                     test = false;
                  }
                }
            }
            return index;
        }

        // FIND FIRST INDEX OF ARRAY ELEMENT EQUAL TO A VALUE  (deprecated - see ArryMaths class)
        // finds the index of the first occurence of the element equal to a given value in an array of doubles
        // returns -1 if none found
        public static int indexOf(final double[] array, final double value){
            int index = -1;
            boolean test = true;
            int counter = 0;
            while(test){
                if(array[counter]==value){
                    index = counter;
                    test = false;
                }
                else{
                    counter++;
                    if(counter>=array.length) {
                     test = false;
                  }
                }
            }
            return index;
        }

        // finds the index of the first occurence of the element equal to a given value in an array of floats
        // returns -1 if none found
        public static int indexOf(final float[] array, final float value){
            int index = -1;
            boolean test = true;
            int counter = 0;
            while(test){
                if(array[counter]==value){
                    index = counter;
                    test = false;
                }
                else{
                    counter++;
                    if(counter>=array.length) {
                     test = false;
                  }
                }
            }
            return index;
        }

        // finds the index of the first occurence of the element equal to a given value in an array of ints
        // returns -1 if none found
        public static int indexOf(final int[] array, final int value){
            int index = -1;
            boolean test = true;
            int counter = 0;
            while(test){
                if(array[counter]==value){
                    index = counter;
                    test = false;
                }
                else{
                    counter++;
                    if(counter>=array.length) {
                     test = false;
                  }
                }
            }
            return index;
        }

        // finds the index of the first occurence of the element equal to a given value in an array of longs
        // returns -1 if none found
        public static int indexOf(final long[] array, final long value){
            int index = -1;
            boolean test = true;
            int counter = 0;
            while(test){
                if(array[counter]==value){
                    index = counter;
                    test = false;
                }
                else{
                    counter++;
                    if(counter>=array.length) {
                     test = false;
                  }
                }
            }
            return index;
        }

        // finds the index of the first occurence of the element equal to a given value in an array of Objects
        // returns -1 if none found
        public static int indexOf(final Object[] array, final Object value){
            int index = -1;
            boolean test = true;
            int counter = 0;
            while(test){
                if(array[counter].equals(value)){
                    index = counter;
                    test = false;
                }
                else{
                    counter++;
                    if(counter>=array.length) {
                     test = false;
                  }
                }
            }
            return index;
        }

        // finds the index of the first occurence of the element equal to a given value in an array of shorts
        // returns -1 if none found
        public static int indexOf(final short[] array, final short value){
            int index = -1;
            boolean test = true;
            int counter = 0;
            while(test){
                if(array[counter]==value){
                    index = counter;
                    test = false;
                }
                else{
                    counter++;
                    if(counter>=array.length) {
                     test = false;
                  }
                }
            }
            return index;
        }

        // finds the index of the first occurence of the element equal to a given value in an array of Strings
        // returns -1 if none found
        public static int indexOf(final String[] array, final String value){
            int index = -1;
            boolean test = true;
            int counter = 0;
            while(test){
                if(array[counter].equals(value)){
                    index = counter;
                    test = false;
                }
                else{
                    counter++;
                    if(counter>=array.length) {
                     test = false;
                  }
                }
            }
            return index;
        }

        // finds the indices of the elements equal to a given value in an array of bytes
        // returns null if none found
        public static int[] indicesOf(final byte[] array, final byte value){
            int[] indices = null;
            int numberOfIndices = 0;
            final ArrayList<Integer> arrayl = new ArrayList<>();
            for(int i=0; i<array.length; i++){
                if(array[i]==value){
                    numberOfIndices++;
                    arrayl.add(new Integer(i));
                }
            }
            if(numberOfIndices!=0){
                indices = new int[numberOfIndices];
                for(int i=0; i<numberOfIndices; i++){
                    indices[i] = (arrayl.get(i)).intValue();
                }
            }
            return indices;
        }

        // finds the indices of the elements equal to a given value in an array of chars
        // returns null if none found
        public static int[] indicesOf(final char[] array, final char value){
            int[] indices = null;
            int numberOfIndices = 0;
            final ArrayList<Integer> arrayl = new ArrayList<>();
            for(int i=0; i<array.length; i++){
                if(array[i]==value){
                    numberOfIndices++;
                    arrayl.add(new Integer(i));
                }
            }
            if(numberOfIndices!=0){
                indices = new int[numberOfIndices];
                for(int i=0; i<numberOfIndices; i++){
                    indices[i] = (arrayl.get(i)).intValue();
                }
            }
            return indices;
        }

        // FIND INDICES OF ARRAY ELEMENTS EQUAL TO A VALUE  (deprecated - see ArryMaths class)
        // finds the indices of the elements equal to a given value in an array of doubles
        // returns null if none found
        public static int[] indicesOf(final double[] array, final double value){
            int[] indices = null;
            int numberOfIndices = 0;
            final ArrayList<Integer> arrayl = new ArrayList<>();
            for(int i=0; i<array.length; i++){
                if(array[i]==value){
                    numberOfIndices++;
                    arrayl.add(new Integer(i));
                }
            }
            if(numberOfIndices!=0){
                indices = new int[numberOfIndices];
                for(int i=0; i<numberOfIndices; i++){
                    indices[i] = (arrayl.get(i)).intValue();
                }
            }
            return indices;
        }

        // finds the indices of the elements equal to a given value in an array of floats
        // returns null if none found
        public static int[] indicesOf(final float[] array, final float value){
            int[] indices = null;
            int numberOfIndices = 0;
            final ArrayList<Integer> arrayl = new ArrayList<>();
            for(int i=0; i<array.length; i++){
                if(array[i]==value){
                    numberOfIndices++;
                    arrayl.add(new Integer(i));
                }
            }
            if(numberOfIndices!=0){
                indices = new int[numberOfIndices];
                for(int i=0; i<numberOfIndices; i++){
                    indices[i] = (arrayl.get(i)).intValue();
                }
            }
            return indices;
        }

        // finds the indices of the elements equal to a given value in an array of ints
        // returns null if none found
        public static int[] indicesOf(final int[] array, final int value){
            int[] indices = null;
            int numberOfIndices = 0;
            final ArrayList<Integer> arrayl = new ArrayList<>();
            for(int i=0; i<array.length; i++){
                if(array[i]==value){
                    numberOfIndices++;
                    arrayl.add(new Integer(i));
                }
            }
            if(numberOfIndices!=0){
                indices = new int[numberOfIndices];
                for(int i=0; i<numberOfIndices; i++){
                    indices[i] = (arrayl.get(i)).intValue();
                }
            }
            return indices;
        }


        // finds the indices of the elements equal to a given value in an array of longs
        // returns null if none found
        public static int[] indicesOf(final long[] array, final long value){
            int[] indices = null;
            int numberOfIndices = 0;
            final ArrayList<Integer> arrayl = new ArrayList<>();
            for(int i=0; i<array.length; i++){
                if(array[i]==value){
                    numberOfIndices++;
                    arrayl.add(new Integer(i));
                }
            }
            if(numberOfIndices!=0){
                indices = new int[numberOfIndices];
                for(int i=0; i<numberOfIndices; i++){
                    indices[i] = (arrayl.get(i)).intValue();
                }
            }
            return indices;
        }

        // finds the indices of the elements equal to a given value in an array of Objectss
        // returns null if none found
        public static int[] indicesOf(final Object[] array, final Object value){
            int[] indices = null;
            int numberOfIndices = 0;
            final ArrayList<Integer> arrayl = new ArrayList<>();
            for(int i=0; i<array.length; i++){
                if(array[i].equals(value)){
                    numberOfIndices++;
                    arrayl.add(new Integer(i));
                }
            }
            if(numberOfIndices!=0){
                indices = new int[numberOfIndices];
                for(int i=0; i<numberOfIndices; i++){
                    indices[i] = (arrayl.get(i)).intValue();
                }
            }
            return indices;
        }

        // finds the indices of the elements equal to a given value in an array of shorts
        // returns null if none found
        public static int[] indicesOf(final short[] array, final short value){
            int[] indices = null;
            int numberOfIndices = 0;
            final ArrayList<Integer> arrayl = new ArrayList<>();
            for(int i=0; i<array.length; i++){
                if(array[i]==value){
                    numberOfIndices++;
                    arrayl.add(new Integer(i));
                }
            }
            if(numberOfIndices!=0){
                indices = new int[numberOfIndices];
                for(int i=0; i<numberOfIndices; i++){
                    indices[i] = (arrayl.get(i)).intValue();
                }
            }
            return indices;
        }

        // finds the indices of the elements equal to a given value in an array of Strings
        // returns null if none found
        public static int[] indicesOf(final String[] array, final String value){
            int[] indices = null;
            int numberOfIndices = 0;
            final ArrayList<Integer> arrayl = new ArrayList<>();
            for(int i=0; i<array.length; i++){
                if(array[i].equals(value)){
                    numberOfIndices++;
                    arrayl.add(new Integer(i));
                }
            }
            if(numberOfIndices!=0){
                indices = new int[numberOfIndices];
                for(int i=0; i<numberOfIndices; i++){
                    indices[i] = (arrayl.get(i)).intValue();
                }
            }
            return indices;
        }

        // recast an array of int as double
        public static double[] intTOdouble(final int[] aa){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // recast an array of int as float
        public static float[] intTOfloat(final int[] aa){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // recast an array of int as long
        public static long[] intTOlong(final int[] aa){
            final int n = aa.length;
            final long[] bb = new long[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // INVERT ARRAY ELEMENTS  (deprecated - see ArryMaths class)
        // invert all elements of an array of doubles
        public static double[] invertElements(final double[] aa){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++) {
               bb[i] = 1.0D/aa[i];
            }
            return bb;
        }

        // invert all elements of an array of floats
        public static float[] invertElements(final float[] aa){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++) {
               bb[i] = 1.0F/aa[i];
            }
            return bb;
        }

        // Returns true if x equals y
        // x and y are char
        public static boolean isEqual(final char x, final char y){
            boolean test=false;
            if(x==y) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y
        // x and y are double
        // x may be float within range, PLUS_INFINITY, NEGATIVE_INFINITY, or NaN
        // NB!! This method treats two NaNs as equal
        public static boolean isEqual(final double x, final double y){
            boolean test=false;
            if(Fmath.isNaN(x)){
                if(Fmath.isNaN(y)) {
                  test=true;
               }
            }
            else{
                if(Fmath.isPlusInfinity(x)){
                    if(Fmath.isPlusInfinity(y)) {
                     test=true;
                  }
                }
                else{
                    if(Fmath.isMinusInfinity(x)){
                        if(Fmath.isMinusInfinity(y)) {
                           test=true;
                        }
                    }
                    else{
                        if(x==y) {
                           test=true;
                        }
                    }
                }
            }
            return test;
        }

        // DEPRECATED METHODS
        // Several methods have been revised and moved to classes ArrayMaths, Conv or PrintToScreen

        // Returns true if x equals y
        // x and y are float
        // x may be float within range, PLUS_INFINITY, NEGATIVE_INFINITY, or NaN
        // NB!! This method treats two NaNs as equal
        public static boolean isEqual(final float x, final float y){
            boolean test=false;
            if(Fmath.isNaN(x)){
                if(Fmath.isNaN(y)) {
                  test=true;
               }
            }
            else{
                if(Fmath.isPlusInfinity(x)){
                    if(Fmath.isPlusInfinity(y)) {
                     test=true;
                  }
                }
                else{
                    if(Fmath.isMinusInfinity(x)){
                        if(Fmath.isMinusInfinity(y)) {
                           test=true;
                        }
                    }
                    else{
                        if(x==y) {
                           test=true;
                        }
                    }
                }
            }
            return test;
        }

        // Returns true if x equals y
        // x and y are int
        public static boolean isEqual(final int x, final int y){
            boolean test=false;
            if(x==y) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y
        // x and y are Strings
        public static boolean isEqual(final String x, final String y){
            boolean test=false;
            if(x.equals(y)) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y within limits plus or minus limit
        // x and y are BigDecimal
        public static boolean isEqualWithinLimits(final BigDecimal x, final BigDecimal y, final BigDecimal limit){
            boolean test=false;
            if(((x.subtract(y)).abs()).compareTo(limit.abs())<=0) {
               test = true;
            }
            return test;
        }

        // Returns true if x equals y within limits plus or minus limit
        // x and y are BigInteger
        public static boolean isEqualWithinLimits(final BigInteger x, final BigInteger y, final BigInteger limit){
            boolean test=false;
            if(((x.subtract(y)).abs()).compareTo(limit.abs())<=0) {
               test = true;
            }
            return test;
        }

        // IS EQUAL WITHIN LIMITS
        // Returns true if x equals y within limits plus or minus limit
        // x and y are double
        public static boolean isEqualWithinLimits(final double x, final double y, final double limit){
            boolean test=false;
            if(Math.abs(x-y)<=Math.abs(limit)) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y within limits plus or minus limit
        // x and y are float
        public static boolean isEqualWithinLimits(final float x, final float y, final float limit){
            boolean test=false;
            if(Math.abs(x-y)<=Math.abs(limit)) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y within limits plus or minus limit
        // x and y are int
        public static boolean isEqualWithinLimits(final int x, final int y, final int limit){
            boolean test=false;
            if(Math.abs(x-y)<=Math.abs(limit)) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y within limits plus or minus limit
        // x and y are long
        public static boolean isEqualWithinLimits(final long x, final long y, final long limit){
            boolean test=false;
            if(Math.abs(x-y)<=Math.abs(limit)) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y within a percentage of the mean
        // x and y are BigDecimal
        public static boolean isEqualWithinPerCent(final BigDecimal x, final BigDecimal y, final BigDecimal perCent){
            boolean test=false;
            BigDecimal limit = (x.add(y)).multiply(perCent).multiply(new BigDecimal("0.005")); //$NON-NLS-1$
            if(((x.subtract(y)).abs()).compareTo(limit.abs())<=0) {
               test = true;
            }
            limit = null;
            return test;
        }

        // Returns true if x equals y within a percentage of the mean
        // x and y are BigDInteger, percentage provided as BigDecimal
        public static boolean isEqualWithinPerCent(final BigInteger x, final BigInteger y, final BigDecimal perCent){
            boolean test=false;
            BigDecimal xx = new BigDecimal(x);
            BigDecimal yy = new BigDecimal(y);
            BigDecimal limit = (xx.add(yy)).multiply(perCent).multiply(new BigDecimal("0.005")); //$NON-NLS-1$
            if(((xx.subtract(yy)).abs()).compareTo(limit.abs())<=0) {
               test = true;
            }
            limit = null;
            xx = null;
            yy = null;
            return test;
        }

        // Returns true if x equals y within a percentage of the mean
        // x and y are BigDInteger, percentage provided as BigInteger
        public static boolean isEqualWithinPerCent(final BigInteger x, final BigInteger y, final BigInteger perCent){
            boolean test=false;
            BigDecimal xx = new BigDecimal(x);
            BigDecimal yy = new BigDecimal(y);
            BigDecimal pc = new BigDecimal(perCent);
            BigDecimal limit = (xx.add(yy)).multiply(pc).multiply(new BigDecimal("0.005")); //$NON-NLS-1$
            if(((xx.subtract(yy)).abs()).compareTo(limit.abs())<=0) {
               test = true;
            }
            limit = null;
            xx = null;
            yy = null;
            pc = null;
            return test;
        }


        // IS EQUAL WITHIN A PERCENTAGE
        // Returns true if x equals y within a percentage of the mean
        // x and y are double
        public static boolean isEqualWithinPerCent(final double x, final double y, final double perCent){
            boolean test=false;
            final double limit = Math.abs((x+y)*perCent/200.0D);
            if(Math.abs(x-y)<=limit) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y within a percentage of the mean
        // x and y are float
        public static boolean isEqualWithinPerCent(final float x, final float y, final float perCent){
            boolean test=false;
            final double limit = Math.abs((x+y)*perCent/200.0F);
            if(Math.abs(x-y)<=limit) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y within a percentage of the mean
        // x and y are int, percentage provided as double
        public static boolean isEqualWithinPerCent(final int x, final int y, final double perCent){
            boolean test=false;
            final double limit = Math.abs((x+y)*perCent/200.0D);
            if(Math.abs(x-y)<=limit) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y within a percentage of the mean
        // x and y are int, percentage provided as int
        public static boolean isEqualWithinPerCent(final int x, final int y, final int perCent){
            boolean test=false;
            final double limit = Math.abs((double)(x+y)*(double)perCent/200.0D);
            if(Math.abs(x-y)<=limit) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y within a percentage of the mean
        // x and y are long, percentage provided as double
        public static boolean isEqualWithinPerCent(final long x, final long y, final double perCent){
            boolean test=false;
            final double limit = Math.abs((x+y)*perCent/200.0D);
            if(Math.abs(x-y)<=limit) {
               test=true;
            }
            return test;
        }

        // Returns true if x equals y within a percentage of the mean
        // x and y are long, percentage provided as int
        public static boolean isEqualWithinPerCent(final long x, final long y, final long perCent){
            boolean test=false;
            final double limit = Math.abs((double)(x+y)*(double)perCent/200.0D);
            if(Math.abs(x-y)<=limit) {
               test=true;
            }
            return test;
        }

        // Returns true if x is an even number, false if x is an odd number
        // x is double but must hold an integer value
        public static boolean isEven(final double x){
            double y=Math.floor(x);
            if((x - y)!= 0.0D)
             {
               throw new IllegalArgumentException("the argument is not an integer"); //$NON-NLS-1$
            }
            boolean test=false;
            y=Math.floor(x/2.0F);
            if((x/2.0D-y) == 0.0D) {
               test=true;
            }
            return test;
        }

        // Returns true if x is an even number, false if x is an odd number
        // x is float but must hold an integer value
        public static boolean isEven(final float x){
            double y=Math.floor(x);
            if((x - y)!= 0.0D)
             {
               throw new IllegalArgumentException("the argument is not an integer"); //$NON-NLS-1$
            }
            boolean test=false;
            y=Math.floor(x/2.0F);
            if((x/2.0F-y) == 0.0D) {
               test=true;
            }
            return test;
        }

        // IS EVEN
        // Returns true if x is an even number, false if x is an odd number
        // x is int
        public static boolean isEven(final int x){
            boolean test=false;
            if(x%2 == 0.0D) {
               test=true;
            }
            return test;
        }

        // Returns true if x is infinite, i.e. is equal to either plus or minus infinity
        // x is double
        public static boolean isInfinity(final double x){
            boolean test=false;
            if(x==Double.POSITIVE_INFINITY || x==Double.NEGATIVE_INFINITY) {
               test=true;
            }
            return test;
        }

        // Returns true if x is infinite, i.e. is equal to either plus or minus infinity
        // x is float
        public static boolean isInfinity(final float x){
            boolean test=false;
            if(x==Float.POSITIVE_INFINITY || x==Float.NEGATIVE_INFINITY) {
               test=true;
            }
            return test;
        }

        // IS AN INTEGER
        // Returns true if x is, arithmetically, an integer
        // Returns false if x is not, arithmetically, an integer
        public static boolean isInteger(final double x){
            boolean retn = false;
            final double xfloor = Math.floor(x);
            if((x - xfloor)==0.0D) {
               retn = true;
            }
            return retn;
        }

        // Returns true if all elements in the array x are, arithmetically, integers
        // Returns false if any element in the array x is not, arithmetically, an integer
        public static boolean isInteger(final double[] x){
            boolean retn = true;
            boolean test = true;
            int ii = 0;
            while(test){
                final double xfloor = Math.floor(x[ii]);
                if((x[ii] - xfloor)!=0.0D){
                    retn = false;
                    test = false;
                }
                else{
                    ii++;
                    if(ii==x.length) {
                     test=false;
                  }
                }
            }
            return retn;
        }

        // Returns true if x is, arithmetically, an integer
        // Returns false if x is not, arithmetically, an integer
        public static boolean isInteger(final float x){
            boolean ret = false;
            final float xfloor = (float)Math.floor(x);
            if((x - xfloor)==0.0F) {
               ret = true;
            }
            return ret;
        }

        // Returns true if all elements in the array x are, arithmetically, integers
        // Returns false if any element in the array x is not, arithmetically, an integer
        public static boolean isInteger(final float[] x){
            boolean retn = true;
            boolean test = true;
            int ii = 0;
            while(test){
                final float xfloor = (float)Math.floor(x[ii]);
                if((x[ii] - xfloor)!=0.0D){
                    retn = false;
                    test = false;
                }
                else{
                    ii++;
                    if(ii==x.length) {
                     test=false;
                  }
                }
            }
            return retn;
        }
        public static boolean isInteger (final Number numberAsObject){
            boolean test = integers.containsKey(numberAsObject.getClass());
            if(!test){
                if(numberAsObject instanceof Double){
                    final double dd = numberAsObject.doubleValue();
                    test = Fmath.isInteger(dd);
                }
                if(numberAsObject instanceof Float){
                    final float dd = numberAsObject.floatValue();
                    test = Fmath.isInteger(dd);
                }
                if(numberAsObject instanceof BigDecimal){
                    final double dd = numberAsObject.doubleValue();
                    test = Fmath.isInteger(dd);
                }
            }
            return test;
        }

        public static boolean isInteger (final Number[] numberAsObject){
            boolean testall = true;
            for (final Number element : numberAsObject) {
                boolean test = integers.containsKey(element.getClass());
                if(!test){
                    if(element instanceof Double){
                        final double dd = element.doubleValue();
                        test = Fmath.isInteger(dd);
                        if(!test) {
                           testall = false;
                        }
                    }
                    if(element instanceof Float){
                        final float dd = element.floatValue();
                        test = Fmath.isInteger(dd);
                        if(!test) {
                           testall = false;
                        }
                    }
                    if(element instanceof BigDecimal){
                        final double dd = element.doubleValue();
                        test = Fmath.isInteger(dd);
                        if(!test) {
                           testall = false;
                        }
                    }
                }
            }
            return testall;
        }

        // Returns true if x is minus infinity
        // x is double
        public static boolean isMinusInfinity(final double x){
            boolean test=false;
            if(x==Double.NEGATIVE_INFINITY) {
               test=true;
            }
            return test;
        }

         // Returns true if x is minus infinity
        // x is float
        public static boolean isMinusInfinity(final float x){
            boolean test=false;
            if(x==Float.NEGATIVE_INFINITY) {
               test=true;
            }
            return test;
        }

        // Returns true if x is 'Not a Number' (NaN)
        // x is double
        public static boolean isNaN(final double x){
            boolean test=false;
            if(x!=x) {
               test=true;
            }
            return test;
        }

         // Returns true if x is 'Not a Number' (NaN)
        // x is float
        public static boolean isNaN(final float x){
            boolean test=false;
            if(x!=x) {
               test=true;
            }
            return test;
        }

        // Returns true if x is an odd number, false if x is an even number
        // x is double but must hold an integer value
        public static boolean isOdd(final double x){
            double y=Math.floor(x);
            if((x - y)!= 0.0D)
             {
               throw new IllegalArgumentException("the argument is not an integer"); //$NON-NLS-1$
            }
            boolean test=true;
            y=Math.floor(x/2.0F);
            if((x/2.0D-y) == 0.0D) {
               test=false;
            }
            return test;
        }

         // Returns true if x is an odd number, false if x is an even number
        // x is float but must hold an integer value
        public static boolean isOdd(final float x){
            double y=Math.floor(x);
            if((x - y)!= 0.0D)
             {
               throw new IllegalArgumentException("the argument is not an integer"); //$NON-NLS-1$
            }
            boolean test=true;
            y=Math.floor(x/2.0F);
            if((x/2.0F-y) == 0.0D) {
               test=false;
            }
            return test;
        }

        // IS ODD
        // Returns true if x is an odd number, false if x is an even number
        // x is int
        public static boolean isOdd(final int x){
            boolean test=true;
            if(x%2 == 0.0D) {
               test=false;
            }
            return test;
        }

        // Returns true if x is plus infinity
        // x is double
        public static boolean isPlusInfinity(final double x){
            boolean test=false;
            if(x==Double.POSITIVE_INFINITY) {
               test=true;
            }
            return test;
        }

        // Returns true if x is plus infinity
        // x is float
        public static boolean isPlusInfinity(final float x){
            boolean test=false;
            if(x==Float.POSITIVE_INFINITY) {
               test=true;
            }
            return test;
        }

        // Converts Joules to calories
        public static double jouleToCalorie(final double joule){
            return  joule*0.23884;
        }

        // Converts Kelvin to Celsius
        public static double kelvinToCelsius(final double kelv){
            return  kelv+T_ABS;
        }

        // Converts kilograms to pounds
        public static double kgToPound(final double kg){
            return  kg/0.4536;
        }


        // Converts kilograms to tons
        public static double kgToTon(final double kg){
            return  kg/1016.05;
        }

        // Converts kilometres per litre to miles per UK gallons
        public static double kmPerLitreToMilePerGallon(final double kmPlitre){
            return  kmPlitre/0.354;
        }

        // Converts kilometres to miles
        public static double kmToMile(final double km){
            return  km/1.6093;
        }

        // LEAP YEAR
        // Returns true if year (argument) is a leap year
        public static boolean leapYear(final int year){
            boolean test = false;

            if(year%4 != 0){
                 test = false;
            }
            else{
                if(year%400 == 0){
                    test=true;
                }
                else{
                    if(year%100 == 0){
                        test=false;
                    }
                    else{
                        test=true;
                    }
                }
            }
            return test;
        }

        // Converts litres per kilometre to UK gallons per mile
        public static double litrePerKmToGallonPerMile(final double litrePkm){
            return  litrePkm/2.825;
        }

        // Converts litres to UK gallons
        public static double litreToGallon(final double litre){
            return  litre/4.546;
        }

        // Converts litres to UK pints
        public static double litreToPint(final double litre){
            return  litre/0.568;
        }

        // Converts litres to UK quarts
        public static double litreToQuart(final double litre){
            return  litre/1.137;
        }

        // NATURAL LOG OF ARRAY ELEMENTS  (deprecated - see ArryMaths class)
        // Log to base e of all elements of an array of doubles
        public static double[] lnElements(final double[] aa){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++) {
               bb[i] = Math.log10(aa[i]);
            }
            return bb;
        }

        // Log to base e of all elements of an array of floats
        public static float[] lnElements(final float[] aa){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++) {
               bb[i] = (float)Math.log10(aa[i]);
            }
            return bb;
        }

        // Log to base e of a double number
        public static double log(final double a){
            return Math.log(a);
        }

        // Log to base e of a float number
        public static float log(final float a){
            return (float)Math.log(a);
        }

        // LOGARITHMS
        // Log to base 10 of a double number
        public static double log10(final double a){
            return Math.log(a)/Math.log(10.0D);
        }

        // Log to base b of a double number and double base
        public static double log10(final double a, final double b){
            return Math.log(a)/Math.log(b);
        }

        // Log to base b of a double number and int base
        public static double log10(final double a, final int b){
            return Math.log(a)/Math.log(b);
        }

        // Log to base 10 of a float number
        public static float log10(final float a){
            return (float) (Math.log(a)/Math.log(10.0D));
        }

        // Log to base b of a float number and flaot base
        public static float log10(final float a, final float b){
            return (float) (Math.log(a)/Math.log(b));
        }

        // Log to base b of a float number and int base
        public static float log10(final float a, final int b){
            return (float) (Math.log(a)/Math.log(b));
        }

        // LOG10 OF ARRAY ELEMENTS  (deprecated - see ArryMaths class)
        // Log to base 10 of all elements of an array of doubles
        public static double[] log10Elements(final double[] aa){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++) {
               bb[i] = Math.log10(aa[i]);
            }
            return bb;
        }

        // Log to base 10 of all elements of an array of floats
        public static float[] log10Elements(final float[] aa){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++) {
               bb[i] = (float)Math.log10(aa[i]);
            }
            return bb;
        }

        // Log to base 2 of a double number
        public static double log2(final double a){
            return Math.log(a)/Math.log(2.0D);
        }

        // Log to base 2 of a float number
        public static float log2(final float a){
            return (float) (Math.log(a)/Math.log(2.0D));
        }

        // log to base e of the factorial of n
        // Argument is of type double but must be, numerically, an integer
        // log[e](factorial) returned as double
        // numerical rounding may makes this an approximation
        public static double logFactorial(final double n){
            if(n<0 || (n-Math.floor(n))!=0)
             {
               throw new IllegalArgumentException("\nn must be a positive integer\nIs a Gamma funtion [Fmath.gamma(x)] more appropriate?"); //$NON-NLS-1$
            }
            double f = 0.0D;
            double iCount = 2.0D;
            while(iCount<=n){
                f+=Math.log(iCount);
                iCount += 1.0D;
            }
            return f;
        }

        // log to base e of the factorial of n
        // log[e](factorial) returned as double
        // numerical rounding may makes this an approximation
        public static double logFactorial(final int n){
            if(n<0)
             {
               throw new IllegalArgumentException("\nn must be a positive integer\nIs a Gamma funtion [Fmath.gamma(x)] more appropriate?"); //$NON-NLS-1$
            }
            double f = 0.0D;
            for(int i=2; i<=n; i++) {
               f+=Math.log(i);
            }
            return f;
        }


        // log to base e of the factorial of n
        // Argument is of type double but must be, numerically, an integer
        // log[e](factorial) returned as double
        // numerical rounding may makes this an approximation
        public static double logFactorial(final long n){
            if(n<0L)
             {
               throw new IllegalArgumentException("\nn must be a positive integer\nIs a Gamma funtion [Fmath.gamma(x)] more appropriate?"); //$NON-NLS-1$
            }
            double f = 0.0D;
            long iCount = 2L;
            while(iCount<=n){
                f+=Math.log(iCount);
                iCount += 1L;
            }
            return f;
        }

        // recast an array of long as double
        // BEWARE POSSIBLE LOSS OF PRECISION
        public static double[] longTOdouble(final long[] aa){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // recast an array of long as float
        // BEWARE POSSIBLE LOSS OF PRECISION
        public static float[] longTOfloat(final long[] aa){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // ARRAY MAXIMUM  (deprecated - see ArryMaths class)
        // Maximum of a 1D array of doubles, aa
        public static double maximum(final double[] aa){
            final int n = aa.length;
            double aamax=aa[0];
            for(int i=1; i<n; i++){
                if(aa[i]>aamax) {
                  aamax=aa[i];
               }
            }
            return aamax;
        }

        // Maximum of a 1D array of floats, aa
        public static float maximum(final float[] aa){
            final int n = aa.length;
            float aamax=aa[0];
            for(int i=1; i<n; i++){
                if(aa[i]>aamax) {
                  aamax=aa[i];
               }
            }
            return aamax;
        }

        // Maximum of a 1D array of ints, aa
        public static int maximum(final int[] aa){
            final int n = aa.length;
            int aamax=aa[0];
            for(int i=1; i<n; i++){
                if(aa[i]>aamax) {
                  aamax=aa[i];
               }
            }
            return aamax;
        }

        // Maximum of a 1D array of longs, aa
        public static long maximum(final long[] aa){
            final long n = aa.length;
            long aamax=aa[0];
            for(int i=1; i<n; i++){
                if(aa[i]>aamax) {
                  aamax=aa[i];
               }
            }
            return aamax;
        }

        // MAXIMUM DISTANCE BETWEEN ARRAY ELEMENTS  (deprecated - see ArryMaths class)
        // Maximum distance between elements of a 1D array of doubles, aa
        public static double maximumDifference(final double[] aa){
            return Fmath.maximum(aa) - Fmath.minimum(aa);
        }

        // Maximum distance between elements of a 1D array of floats, aa
        public static float maximumDifference(final float[] aa){
            return Fmath.maximum(aa) - Fmath.minimum(aa);
        }

        // Maximum distance between elements of a 1D array of ints, aa
        public static int maximumDifference(final int[] aa){
            return Fmath.maximum(aa) - Fmath.minimum(aa);
        }

        // Maximum distance between elements of a 1D array of long, aa
        public static long maximumDifference(final long[] aa){
            return Fmath.maximum(aa) - Fmath.minimum(aa);
        }


        // Converts metres to feet
        public static double metreToFoot(final double metre){
            return  metre/0.3048;
        }

        // Converts metres to yards
        public static double metreToYard(final double metre){
            return  metre/0.9144;
        }

        // Converts miles per UK gallons to kilometres per litre
        public static double milePerGallonToKmPerLitre(final double milePgall){
            return  milePgall*0.354;
        }

        // Converts miles to kilometres
        public static double mileToKm(final double mile){
            return  mile*1.6093;
        }

        // Converts millimetres to inches
        public static double millimetreToInch(final double mm){
            return  mm/25.4;
        }

        // Minimum of a 1D array of doubles, aa
        public static double minimum(final double[] aa){
            final int n = aa.length;
            double aamin=aa[0];
            for(int i=1; i<n; i++){
                if(aa[i]<aamin) {
                  aamin=aa[i];
               }
            }
            return aamin;
        }

        // Minimum of a 1D array of floats, aa
        public static float minimum(final float[] aa){
            final int n = aa.length;
            float aamin=aa[0];
            for(int i=1; i<n; i++){
                if(aa[i]<aamin) {
                  aamin=aa[i];
               }
            }
            return aamin;
        }

        // ARRAY MINIMUM (deprecated - see ArryMaths class)
        // Minimum of a 1D array of ints, aa
        public static int minimum(final int[] aa){
            final int n = aa.length;
            int aamin=aa[0];
            for(int i=1; i<n; i++){
                if(aa[i]<aamin) {
                  aamin=aa[i];
               }
            }
            return aamin;
        }

        // Minimum of a 1D array of longs, aa
        public static long minimum(final long[] aa){
            final long n = aa.length;
            long aamin=aa[0];
            for(int i=1; i<n; i++){
                if(aa[i]<aamin) {
                  aamin=aa[i];
               }
            }
            return aamin;
        }

        // MINIMUM DISTANCE BETWEEN ARRAY ELEMENTS  (deprecated - see ArryMaths class)
        // Minimum distance between elements of a 1D array of doubles, aa
        public static double minimumDifference(final double[] aa){
            final double[] sorted = Fmath.selectionSort(aa);
            final double n = aa.length;
            double diff = sorted[1] - sorted[0];
            double minDiff = diff;
            for(int i=1; i<n-1; i++){
                diff = sorted[i+1] - sorted[i];
                if(diff<minDiff) {
                  minDiff = diff;
               }
            }
            return minDiff;
        }

        // Minimum distance between elements of a 1D array of floats, aa
        public static float minimumDifference(final float[] aa){
            final float[] sorted = Fmath.selectionSort(aa);
            final float n = aa.length;
            float diff = sorted[1] - sorted[0];
            float minDiff = diff;
            for(int i=1; i<n-1; i++){
                diff = sorted[i+1] - sorted[i];
                if(diff<minDiff) {
                  minDiff = diff;
               }
            }
            return minDiff;
        }

        // Minimum distance between elements of a 1D array of ints, aa
        public static int minimumDifference(final int[] aa){
            final int[] sorted = Fmath.selectionSort(aa);
            final int n = aa.length;
            int diff = sorted[1] - sorted[0];
            int minDiff = diff;
            for(int i=1; i<n-1; i++){
                diff = sorted[i+1] - sorted[i];
                if(diff<minDiff) {
                  minDiff = diff;
               }
            }
            return minDiff;
        }

        // Minimum distance between elements of a 1D array of longs, aa
        public static long minimumDifference(final long[] aa){
            final long[] sorted = Fmath.selectionSort(aa);
            final long n = aa.length;
            long diff = sorted[1] - sorted[0];
            long minDiff = diff;
            for(int i=1; i<n-1; i++){
                diff = sorted[i+1] - sorted[i];
                if(diff<minDiff) {
                  minDiff = diff;
               }
            }
            return minDiff;
        }

        // Converts moles per litre to percentage weight by volume
        public static double molarToPercentWeightByVol(final double molar, final double molWeight){
            return  molar*molWeight/10.0D;
        }

        // finds the index of nearest element value in array to the argument value
        public static int nearestElementIndex(final double[] array, final double value){
            double diff = Math.abs(array[0] - value);
            int nearest = 0;
            for(int i=1; i<array.length; i++){
                if(Math.abs(array[i] - value)<diff){
                    diff = Math.abs(array[i] - value);
                    nearest = i;
                }
            }
            return nearest;
        }

        // finds the index of nearest element value in array to the argument value
        public static int nearestElementIndex(final int[] array, final int value){
            int diff = Math.abs(array[0] - value);
            int nearest = 0;
            for(int i=1; i<array.length; i++){
                if(Math.abs(array[i] - value)<diff){
                    diff = Math.abs(array[i] - value);
                    nearest = i;
                }
            }
            return nearest;
        }

        // FIND  VALUE OF AND FIND VALUE OF ARRAY ELEMENTS NEAREST TO A VALUE  (deprecated - see ArryMaths class)
        // finds the value of nearest element value in array to the argument value
        public static double nearestElementValue(final double[] array, final double value){
            double diff = Math.abs(array[0] - value);
            double nearest = array[0];
            for(int i=1; i<array.length; i++){
                if(Math.abs(array[i] - value)<diff){
                    diff = Math.abs(array[i] - value);
                    nearest = array[i];
                }
            }
            return nearest;
        }

        // finds the value of nearest element value in array to the argument value
        public static int nearestElementValue(final int[] array, final int value){
            int diff = Math.abs(array[0] - value);
            int nearest = array[0];
            for(int i=1; i<array.length; i++){
               if(Math.abs(array[i] - value)<diff){
                    diff = Math.abs(array[i] - value);
                    nearest = array[i];
                }
            }
            return nearest;
        }

        // finds the index of nearest higher element value in array to the argument value
        public static int nearestHigherElementIndex(final double[] array, final double value){
            double diff0 = 0.0D;
            double diff1 = 0.0D;
            int nearest = 0;
            int ii = 0;
            boolean test = true;
            double max = array[0];
            int maxI = 0;
            while(test){
                if(array[ii]>max){
                    max = array[ii];
                    maxI = ii;
                }
                if((array[ii] - value )>=0.0D){
                    diff0 = value - array[ii];
                    nearest = ii;
                    test = false;
                }
                else{
                    ii++;
                    if(ii>array.length-1){
                        nearest = maxI;
                        diff0 = value - max;
                        test = false;
                    }
                }
            }
            for(int i=0; i<array.length; i++){
                diff1 = array[i]- value;
                if(diff1>=0.0D && diff1<diff0 ){
                    diff0 = diff1;
                    nearest = i;
                }
            }
            return nearest;
        }

        // finds the index of nearest higher element value in array to the argument value
        public static int nearestHigherElementIndex(final int[] array, final int value){
            int diff0 = 0;
            int diff1 = 0;
            int nearest = 0;
            int ii = 0;
            boolean test = true;
            int max = array[0];
            int maxI = 0;
            while(test){
                if(array[ii]>max){
                    max = array[ii];
                    maxI = ii;
                }
                if((array[ii] - value )>=0){
                    diff0 = value - array[ii];
                    nearest = ii;
                    test = false;
                }
                else{
                    ii++;
                    if(ii>array.length-1){
                        nearest = maxI;
                        diff0 = value - max;
                        test = false;
                    }
                }
            }
            for(int i=0; i<array.length; i++){
                diff1 = array[i]- value;
                if(diff1>=0 && diff1<diff0 ){
                    diff0 = diff1;
                    nearest = i;
                }
            }
            return nearest;
        }

         // finds the value of nearest higher element value in array to the argument value
        public static double nearestHigherElementValue(final double[] array, final double value){
            double diff0 = 0.0D;
            double diff1 = 0.0D;
            double nearest = 0.0D;
            int ii = 0;
            boolean test = true;
            double max = array[0];
            while(test){
                if(array[ii]>max) {
                  max = array[ii];
               }
                if((array[ii] - value )>=0.0D){
                    diff0 = value - array[ii];
                    nearest = array[ii];
                    test = false;
                }
                else{
                    ii++;
                    if(ii>array.length-1){
                        nearest = max;
                        diff0 = value - max;
                        test = false;
                    }
                }
            }
            for (final double element : array) {
                diff1 = element- value;
                if(diff1>=0.0D && diff1<diff0 ){
                    diff0 = diff1;
                    nearest = element;
                }
            }
            return nearest;
        }

        // finds the value of nearest higher element value in array to the argument value
        public static int nearestHigherElementValue(final int[] array, final int value){
            int diff0 = 0;
            int diff1 = 0;
            int nearest = 0;
            int ii = 0;
            boolean test = true;
            int max = array[0];
            while(test){
                if(array[ii]>max) {
                  max = array[ii];
               }
                if((array[ii] - value )>=0){
                    diff0 = value - array[ii];
                    nearest = array[ii];
                    test = false;
                }
                else{
                    ii++;
                    if(ii>array.length-1){
                        nearest = max;
                        diff0 = value - max;
                        test = false;
                    }
                }
            }
            for (final int element : array) {
                diff1 = element- value;
                if(diff1>=0 && diff1<diff0 ){
                    diff0 = diff1;
                    nearest = element;
                }
            }
            return nearest;
        }

        // finds the index of nearest lower element value in array to the argument value
        public static int nearestLowerElementIndex(final double[] array, final double value){
            double diff0 = 0.0D;
            double diff1 = 0.0D;
            int nearest = 0;
            int ii = 0;
            boolean test = true;
            double min = array[0];
            int minI = 0;
            while(test){
                if(array[ii]<min){
                    min = array[ii];
                    minI = ii;
                }
                if((value - array[ii])>=0.0D){
                    diff0 = value - array[ii];
                    nearest = ii;
                    test = false;
                }
                else{
                    ii++;
                    if(ii>array.length-1){
                        nearest = minI;
                        diff0 = min - value;
                        test = false;
                    }
                }
            }
            for(int i=0; i<array.length; i++){
                diff1 = value - array[i];
                if(diff1>=0.0D && diff1<diff0 ){
                    diff0 = diff1;
                    nearest = i;
                }
            }
            return nearest;
        }

        // finds the index of nearest lower element value in array to the argument value
        public static int nearestLowerElementIndex(final int[] array, final int value){
            int diff0 = 0;
            int diff1 = 0;
            int nearest = 0;
            int ii = 0;
            boolean test = true;
            int min = array[0];
            int minI = 0;
            while(test){
                if(array[ii]<min){
                    min = array[ii];
                    minI = ii;
                }
                if((value - array[ii])>=0){
                    diff0 = value - array[ii];
                    nearest = ii;
                    test = false;
                }
                else{
                    ii++;
                    if(ii>array.length-1){
                        nearest = minI;
                        diff0 = min - value;
                        test = false;
                    }
                }
            }
            for(int i=0; i<array.length; i++){
                diff1 = value - array[i];
                if(diff1>=0 && diff1<diff0 ){
                    diff0 = diff1;
                    nearest = i;
                }
            }
            return nearest;
        }

        // finds the value of nearest lower element value in array to the argument value
        public static double nearestLowerElementValue(final double[] array, final double value){
            double diff0 = 0.0D;
            double diff1 = 0.0D;
            double nearest = 0.0D;
            int ii = 0;
            boolean test = true;
            double min = array[0];
            while(test){
                if(array[ii]<min) {
                  min = array[ii];
               }
                if((value - array[ii])>=0.0D){
                    diff0 = value - array[ii];
                    nearest = array[ii];
                    test = false;
                }
                else{
                    ii++;
                    if(ii>array.length-1){
                        nearest = min;
                        diff0 = min - value;
                        test = false;
                    }
                }
            }
            for (final double element : array) {
                diff1 = value - element;
                if(diff1>=0.0D && diff1<diff0 ){
                    diff0 = diff1;
                    nearest = element;
                }
            }
            return nearest;
        }

        // finds the value of nearest lower element value in array to the argument value
        public static int nearestLowerElementValue(final int[] array, final int value){
            int diff0 = 0;
            int diff1 = 0;
            int nearest = 0;
            int ii = 0;
            boolean test = true;
            int min = array[0];
            while(test){
                if(array[ii]<min) {
                  min = array[ii];
               }
                if((value - array[ii])>=0){
                    diff0 = value - array[ii];
                    nearest = array[ii];
                    test = false;
                }
                else{
                    ii++;
                    if(ii>array.length-1){
                        nearest = min;
                        diff0 = min - value;
                        test = false;
                    }
                }
            }
            for (final int element : array) {
                diff1 = value - element;
                if(diff1>=0 && diff1<diff0 ){
                    diff0 = diff1;
                    nearest = element;
                }
            }
            return nearest;
        }

        // Converts wavelength in nm to matching energy in eV
        public static double nmToEv(final double nm)
        {
            return  C_LIGHT/(-nm*1e-9)*H_PLANCK/Q_ELECTRON;
        }

        // Normalised sinc (normalised sine cardinal)  sin(pi.x)/(pi.x)
        public static double nsinc(final double a){
            if(Math.abs(a)<1e-40){
                return 1.0D;
            }
            else{
                return Math.sin(Math.PI*a)/(Math.PI*a);
            }
        }

        // Converts ounces to grams
        public static double ounceToGram(final double oz){
            return  oz*28.3459;
        }

        // Converts percentage weight by volume to moles per litre
        public static double percentWeightByVolToMolar(final double perCent, final double molWeight){
            return  perCent*10.0D/molWeight;
        }

        // Converts UK pints to litres
        public static double pintToLitre(final double pint){
            return  pint*0.568;
        }

        // Converts UK pint to American cup
        public static double pintUKtoCupUS(final double pintUK){
            return  pintUK/0.417;
        }

        // Converts UK pint to American liquid pint
        public static double pintUKtoUS(final double pintUK){
            return  pintUK*1.201;
        }

        // Converts American liquid pint to UK pint
        public static double pintUStoUK(final double pintUS){
            return  pintUS*0.833;
        }

        // Converts pounds to kilograms
        public static double poundToKg(final double pds){
            return  pds*0.4536;
        }

        // print an array of bytes to screen
        // No line returns except at the end
        public static void print(final byte[] aa){
            for (final byte element : aa) {
                System.out.print(element+"   "); //$NON-NLS-1$
            }
            System.out.println();
        }

        // print an array of char to screen
        // No line returns except at the end
        public static void print(final char[] aa){
            for (final char element : aa) {
                System.out.print(element+"   "); //$NON-NLS-1$
            }
            System.out.println();
        }

        // PRINT ARRAY TO SCREEN (deprecated - see PrintToScreen class)
        // print an array of doubles to screen
        // No line returns except at the end
        public static void print(final double[] aa){
            for (final double element : aa) {
                System.out.print(element+"   "); //$NON-NLS-1$
            }
            System.out.println();
        }

        // print a 2D array of doubles to screen
        public static void print(final double[][] aa){
            for (final double[] element : aa) {
                Fmath.print(element);
            }
        }

        // print an array of floats to screen
        // No line returns except at the end
        public static void print(final float[] aa){
            for (final float element : aa) {
                System.out.print(element+"   "); //$NON-NLS-1$
            }
            System.out.println();
        }

        // print an array of ints to screen
        // No line returns except at the end
        public static void print(final int[] aa){
            for (final int element : aa) {
                System.out.print(element+"   "); //$NON-NLS-1$
            }
            System.out.println();
        }

        // print an array of longs to screen
        // No line returns except at the end
        public static void print(final long[] aa){
            for (final long element : aa) {
                System.out.print(element+"   "); //$NON-NLS-1$
            }
            System.out.println();
        }

        // print an array of shorts to screen
        // No line returns except at the end
        public static void print(final short[] aa){
            for (final short element : aa) {
                System.out.print(element+"   "); //$NON-NLS-1$
            }
            System.out.println();
        }

        // print an array of String to screen
        // No line returns except at the end
        public static void print(final String[] aa){
            for (final String element : aa) {
                System.out.print(element+"   "); //$NON-NLS-1$
            }
            System.out.println();
        }

        // print an array of bytes to screen
        // with line returns
        public static void println(final byte[] aa){
            for (final byte element : aa) {
                System.out.println(element+"   "); //$NON-NLS-1$
            }
        }


        // print an array of char to screen
        // with line returns
        public static void println(final char[] aa){
            for (final char element : aa) {
                System.out.println(element+"   "); //$NON-NLS-1$
            }
        }

        // print an array of doubles to screen
        // with line returns
        public static void println(final double[] aa){
            for (final double element : aa) {
                System.out.println(element+"   "); //$NON-NLS-1$
            }
        }


        // print an array of floats to screen
        // with line returns
        public static void println(final float[] aa){
            for (final float element : aa) {
                System.out.println(element+"   "); //$NON-NLS-1$
            }
        }

        // print an array of ints to screen
        // with line returns
        public static void println(final int[] aa){
            for (final int element : aa) {
                System.out.println(element+"   "); //$NON-NLS-1$
            }
        }

        // print an array of longs to screen
        // with line returns
        public static void println(final long[] aa){
            for (final long element : aa) {
                System.out.println(element+"   "); //$NON-NLS-1$
            }
        }

        // print an array of shorts to screen
        // with line returns
        public static void println(final short[] aa){
            for (final short element : aa) {
                System.out.println(element+"   "); //$NON-NLS-1$
            }
        }

        // print an array of Strings to screen
        // with line returns
        public static void println(final String[] aa){
            for (final String element : aa) {
                System.out.println(element+"   "); //$NON-NLS-1$
            }
        }

        // Converts UK quarts to litres
        public static double quartToLitre(final double quart){
            return  quart*1.137;
        }

        // Converts UK quart to American liquid quart
        public static double quartUKtoUS(final double quartUK){
            return  quartUK*1.201;
        }

        // Converts American liquid quart to UK quart
        public static double quartUStoUK(final double quartUS){
            return  quartUS*0.833;
        }

        // Converts radial frequency to frequency (Hz)
        public static double radialFrequencyToFrequency(final double radial){
            return  radial/(2.0D*Math.PI);
        }

        // Converts radians to degrees
        public static double radToDeg(final double rad){
            return  rad*180.0D/Math.PI;
        }

        // POWER OF ARRAY ELEMENTS  (deprecated - see ArryMaths class)
        // Raise all elements of an array of doubles to a double power
        public static double[] raiseElementsToPower(final double[] aa, final double power){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++) {
               bb[i] = Math.pow(aa[i], power);
            }
            return bb;
        }

        // Raise all elements of an array of doubles to an int power
        public static double[] raiseElementsToPower(final double[] aa, final int power){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++) {
               bb[i] = Math.pow(aa[i], power);
            }
            return bb;
        }

        // Raise all elements of an array of floats to a float power
        public static float[] raiseElementsToPower(final float[] aa, final float power){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++) {
               bb[i] = (float)Math.pow(aa[i], power);
            }
            return bb;
        }

        // Raise all elements of an array of floats to an int power
        public static float[] raiseElementsToPower(final float[] aa, final int power){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++) {
               bb[i] = (float)Math.pow(aa[i], power);
            }
            return bb;
        }

        // Reverse the order of the elements of a 1D array of char, aa
           public static char[] reverseArray(final char[] aa){
               final int n = aa.length;
               final char[] bb = new char[n];
               for(int i=0; i<n; i++){
                  bb[i] = aa[n-1-i];
               }
               return bb;
           }

        // REVERSE ORDER OF ARRAY ELEMENTS  (deprecated - see ArryMaths class)
        // Reverse the order of the elements of a 1D array of doubles, aa
        public static double[] reverseArray(final double[] aa){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[n-1-i];
            }
            return bb;
        }


        // Reverse the order of the elements of a 1D array of floats, aa
        public static float[] reverseArray(final float[] aa){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[n-1-i];
            }
            return bb;
        }

        // Reverse the order of the elements of a 1D array of ints, aa
        public static int[] reverseArray(final int[] aa){
            final int n = aa.length;
            final int[] bb = new int[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[n-1-i];
            }
            return bb;
        }

        // UNIT CONVERSIONS (deprecated - see Conv class)

        // Reverse the order of the elements of a 1D array of longs, aa
        public static long[] reverseArray(final long[] aa){
            final int n = aa.length;
            final long[] bb = new long[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[n-1-i];
            }
            return bb;
        }

        // Secant
        public static double sec(final double a){
            return 1.0/Math.cos(a);
        }

        //Hyperbolic secant of a double number
        public static double sech(final double a){
                return 1.0D/cosh(a);
        }

        // sort elements in an array of doubles into ascending order
        // using selection sort method
        public static double[] selectionSort(final double[] aa){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            double hold = 0.0D;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
                bb[i]=aa[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(bb[i]<bb[index]){
                        index=i;
                    }
                }
                lastIndex++;
                hold=bb[index];
                bb[index]=bb[lastIndex];
                bb[lastIndex]=hold;
            }
            return bb;
        }

        // sort the elements of an array of doubles into ascending order with matching switches in an array of the length
        // using selection sort method
        // array determining the order is the first argument
        // matching array  is the second argument
        // sorted arrays returned as third and fourth arguments respectively
        public static void selectionSort(final double[] aa, final double[] bb, final double[] cc, final double[] dd){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            final int m = bb.length;
            if(n!=m)
             {
               throw new IllegalArgumentException("First argument array, aa, (length = " + n + ") and the second argument array, bb, (length = " + m + ") should be the same length"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int nn = cc.length;
            if(nn<n)
             {
               throw new IllegalArgumentException("The third argument array, cc, (length = " + nn + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int mm = dd.length;
            if(mm<m)
             {
               throw new IllegalArgumentException("The fourth argument array, dd, (length = " + mm + ") should be at least as long as the second argument array, bb, (length = " + m + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            double holdx = 0.0D;
            double holdy = 0.0D;


            for(int i=0; i<n; i++){
                cc[i]=aa[i];
                dd[i]=bb[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(cc[i]<cc[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdx=cc[index];
                cc[index]=cc[lastIndex];
                cc[lastIndex]=holdx;
                holdy=dd[index];
                dd[index]=dd[lastIndex];
                dd[lastIndex]=holdy;
            }
        }

        // sort elements in an array of doubles into ascending order
        // using selection sort method
        // aa - the original array - not altered
        // bb - the sorted array
        // indices - an array of the original indices of the sorted array
        public static void selectionSort(final double[] aa, final double[] bb, final int[] indices){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            double holdb = 0.0D;
            int holdi = 0;
            for(int i=0; i<n; i++){
                bb[i]=aa[i];
                indices[i]=i;
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(bb[i]<bb[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdb=bb[index];
                bb[index]=bb[lastIndex];
                bb[lastIndex]=holdb;
                holdi=indices[index];
                indices[index]=indices[lastIndex];
                indices[lastIndex]=holdi;
            }
        }

        // sort the elements of an array of doubles into ascending order with matching switches in an array of int of the length
        // using selection sort method
        // array determining the order is the first argument
        // matching array  is the second argument
        // sorted arrays returned as third and fourth arguments respectively
       public static void selectionSort(final double[] aa, final int[] bb, final double[] cc, final int[] dd){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            final int m = bb.length;
            if(n!=m)
             {
               throw new IllegalArgumentException("First argument array, aa, (length = " + n + ") and the second argument array, bb, (length = " + m + ") should be the same length"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int nn = cc.length;
            if(nn<n)
             {
               throw new IllegalArgumentException("The third argument array, cc, (length = " + nn + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int mm = dd.length;
            if(mm<m)
             {
               throw new IllegalArgumentException("The fourth argument array, dd, (length = " + mm + ") should be at least as long as the second argument array, bb, (length = " + m + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            double holdx = 0.0D;
            int holdy = 0;


            for(int i=0; i<n; i++){
                cc[i]=aa[i];
                dd[i]=bb[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(cc[i]<cc[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdx=cc[index];
                cc[index]=cc[lastIndex];
                cc[lastIndex]=holdx;
                holdy=dd[index];
                dd[index]=dd[lastIndex];
                dd[lastIndex]=holdy;
            }
       }

        // sort the elements of an array of doubles into ascending order with matching switches in an array of long of the length
        // using selection sort method
        // array determining the order is the first argument
        // matching array  is the second argument
        // sorted arrays returned as third and fourth arguments respectively
       public static void selectionSort(final double[] aa, final long[] bb, final double[] cc, final long[] dd){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            final int m = bb.length;
            if(n!=m)
             {
               throw new IllegalArgumentException("First argument array, aa, (length = " + n + ") and the second argument array, bb, (length = " + m + ") should be the same length"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int nn = cc.length;
            if(nn<n)
             {
               throw new IllegalArgumentException("The third argument array, cc, (length = " + nn + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int mm = dd.length;
            if(mm<m)
             {
               throw new IllegalArgumentException("The fourth argument array, dd, (length = " + mm + ") should be at least as long as the second argument array, bb, (length = " + m + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            double holdx = 0.0D;
            long holdy = 0L;


            for(int i=0; i<n; i++){
                cc[i]=aa[i];
                dd[i]=bb[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(cc[i]<cc[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdx=cc[index];
                cc[index]=cc[lastIndex];
                cc[lastIndex]=holdx;
                holdy=dd[index];
                dd[index]=dd[lastIndex];
                dd[lastIndex]=holdy;
            }
        }

        // sort elements in an array of floats into ascending order
        // using selection sort method
        public static float[] selectionSort(final float[] aa){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            float hold = 0.0F;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++){
                bb[i]=aa[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(bb[i]<bb[index]){
                        index=i;
                    }
                }
                lastIndex++;
                hold=bb[index];
                bb[index]=bb[lastIndex];
                bb[lastIndex]=hold;
            }
            return bb;
        }

        // sort the elements of an array of floats into ascending order with matching switches in an array of the length
        // using selection sort method
        // array determining the order is the first argument
        // matching array  is the second argument
        // sorted arrays returned as third and fourth arguments respectively
        public static void selectionSort(final float[] aa, final float[] bb, final float[] cc, final float[] dd){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            final int m = bb.length;
            if(n!=m)
             {
               throw new IllegalArgumentException("First argument array, aa, (length = " + n + ") and the second argument array, bb, (length = " + m + ") should be the same length"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int nn = cc.length;
            if(nn<n)
             {
               throw new IllegalArgumentException("The third argument array, cc, (length = " + nn + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int mm = dd.length;
            if(mm<m)
             {
               throw new IllegalArgumentException("The fourth argument array, dd, (length = " + mm + ") should be at least as long as the second argument array, bb, (length = " + m + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            float holdx = 0.0F;
            float holdy = 0.0F;


            for(int i=0; i<n; i++){
                cc[i]=aa[i];
                dd[i]=bb[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(cc[i]<cc[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdx=cc[index];
                cc[index]=cc[lastIndex];
                cc[lastIndex]=holdx;
                holdy=dd[index];
                dd[index]=dd[lastIndex];
                dd[lastIndex]=holdy;
            }
       }

        // sort elements in an array of ints into ascending order
        // using selection sort method
        public static int[] selectionSort(final int[] aa){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            int hold = 0;
            final int[] bb = new int[n];
            for(int i=0; i<n; i++){
                bb[i]=aa[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(bb[i]<bb[index]){
                        index=i;
                    }
                }
                lastIndex++;
                hold=bb[index];
                bb[index]=bb[lastIndex];
                bb[lastIndex]=hold;
            }
            return bb;
        }

        // sort the elements of an array of int into ascending order with matching switches in an array of double of the length
        // using selection sort method
        // array determining the order is the first argument
        // matching array  is the second argument
        // sorted arrays returned as third and fourth arguments respectively
       public static void selectionSort(final int[] aa, final double[] bb, final int[] cc, final double[] dd){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            final int m = bb.length;
            if(n!=m)
             {
               throw new IllegalArgumentException("First argument array, aa, (length = " + n + ") and the second argument array, bb, (length = " + m + ") should be the same length"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int nn = cc.length;
            if(nn<n)
             {
               throw new IllegalArgumentException("The third argument array, cc, (length = " + nn + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int mm = dd.length;
            if(mm<m)
             {
               throw new IllegalArgumentException("The fourth argument array, dd, (length = " + mm + ") should be at least as long as the second argument array, bb, (length = " + m + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            int holdx = 0;
            double holdy = 0.0D;


            for(int i=0; i<n; i++){
                cc[i]=aa[i];
                dd[i]=bb[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(cc[i]<cc[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdx=cc[index];
                cc[index]=cc[lastIndex];
                cc[lastIndex]=holdx;
                holdy=dd[index];
                dd[index]=dd[lastIndex];
                dd[lastIndex]=holdy;
            }
       }

        // sort the elements of an array of ints into ascending order with matching switches in an array of the length
        // using selection sort method
        // array determining the order is the first argument
        // matching array  is the second argument
        // sorted arrays returned as third and fourth arguments respectively
       public static void selectionSort(final int[] aa, final int[] bb, final int[] cc, final int[] dd){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            final int m = bb.length;
            if(n!=m)
             {
               throw new IllegalArgumentException("First argument array, aa, (length = " + n + ") and the second argument array, bb, (length = " + m + ") should be the same length"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int nn = cc.length;
            if(nn<n)
             {
               throw new IllegalArgumentException("The third argument array, cc, (length = " + nn + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int mm = dd.length;
            if(mm<m)
             {
               throw new IllegalArgumentException("The fourth argument array, dd, (length = " + mm + ") should be at least as long as the second argument array, bb, (length = " + m + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            int holdx = 0;
            int holdy = 0;


            for(int i=0; i<n; i++){
                cc[i]=aa[i];
                dd[i]=bb[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(cc[i]<cc[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdx=cc[index];
                cc[index]=cc[lastIndex];
                cc[lastIndex]=holdx;
                holdy=dd[index];
                dd[index]=dd[lastIndex];
                dd[lastIndex]=holdy;
            }
       }

        // sort the elements of an array of int into ascending order with matching switches in an array of long of the length
        // using selection sort method
        // array determining the order is the first argument
        // matching array  is the second argument
        // sorted arrays returned as third and fourth arguments respectively
        public static void selectionSort(final int[] aa, final long[] bb, final int[] cc, final long[] dd){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            final int m = bb.length;
            if(n!=m)
             {
               throw new IllegalArgumentException("First argument array, aa, (length = " + n + ") and the second argument array, bb, (length = " + m + ") should be the same length"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int nn = cc.length;
            if(nn<n)
             {
               throw new IllegalArgumentException("The third argument array, cc, (length = " + nn + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int mm = dd.length;
            if(mm<m)
             {
               throw new IllegalArgumentException("The fourth argument array, dd, (length = " + mm + ") should be at least as long as the second argument array, bb, (length = " + m + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            int holdx = 0;
            long holdy = 0L;


            for(int i=0; i<n; i++){
                cc[i]=aa[i];
                dd[i]=bb[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(cc[i]<cc[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdx=cc[index];
                cc[index]=cc[lastIndex];
                cc[lastIndex]=holdx;
                holdy=dd[index];
                dd[index]=dd[lastIndex];
                dd[lastIndex]=holdy;
            }
       }

        // sort elements in an array of longs into ascending order
        // using selection sort method
        public static long[] selectionSort(final long[] aa){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            long hold = 0L;
            final long[] bb = new long[n];
            for(int i=0; i<n; i++){
                bb[i]=aa[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(bb[i]<bb[index]){
                        index=i;
                    }
                }
                lastIndex++;
                hold=bb[index];
                bb[index]=bb[lastIndex];
                bb[lastIndex]=hold;
            }
            return bb;
        }

        // sort the elements of an array of long into ascending order with matching switches in an array of double of the length
        // using selection sort method
        // array determining the order is the first argument
        // matching array  is the second argument
        // sorted arrays returned as third and fourth arguments respectively
        public static void selectionSort(final long[] aa, final double[] bb, final long[] cc, final double[] dd){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            final int m = bb.length;
            if(n!=m)
             {
               throw new IllegalArgumentException("First argument array, aa, (length = " + n + ") and the second argument array, bb, (length = " + m + ") should be the same length"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int nn = cc.length;
            if(nn<n)
             {
               throw new IllegalArgumentException("The third argument array, cc, (length = " + nn + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int mm = dd.length;
            if(mm<m)
             {
               throw new IllegalArgumentException("The fourth argument array, dd, (length = " + mm + ") should be at least as long as the second argument array, bb, (length = " + m + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            long holdx = 0L;
            double holdy = 0.0D;


            for(int i=0; i<n; i++){
                cc[i]=aa[i];
                dd[i]=bb[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(cc[i]<cc[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdx=cc[index];
                cc[index]=cc[lastIndex];
                cc[lastIndex]=holdx;
                holdy=dd[index];
                dd[index]=dd[lastIndex];
                dd[lastIndex]=holdy;
            }
       }

        // sort the elements of an array of long into ascending order with matching switches in an array of int of the length
      // using selection sort method
      // array determining the order is the first argument
      // matching array  is the second argument
      // sorted arrays returned as third and fourth arguments respectively
      public static void selectionSort(final long[] aa, final int[] bb, final long[] cc, final int[] dd){
         int index = 0;
         int lastIndex = -1;
         final int n = aa.length;
         final int m = bb.length;
         if(n!=m)
          {
            throw new IllegalArgumentException("First argument array, aa, (length = " + n + ") and the second argument array, bb, (length = " + m + ") should be the same length"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         }
         final int nn = cc.length;
         if(nn<n)
          {
            throw new IllegalArgumentException("The third argument array, cc, (length = " + nn + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         }
         final int mm = dd.length;
         if(mm<m)
          {
            throw new IllegalArgumentException("The fourth argument array, dd, (length = " + mm + ") should be at least as long as the second argument array, bb, (length = " + m + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         }

         long holdx = 0L;
         int holdy = 0;


         for(int i=0; i<n; i++){
             cc[i]=aa[i];
             dd[i]=bb[i];
         }

         while(lastIndex != n-1){
             index = lastIndex+1;
             for(int i=lastIndex+2; i<n; i++){
                 if(cc[i]<cc[index]){
                     index=i;
                 }
             }
             lastIndex++;
             holdx=cc[index];
             cc[index]=cc[lastIndex];
             cc[lastIndex]=holdx;
             holdy=dd[index];
             dd[index]=dd[lastIndex];
             dd[lastIndex]=holdy;
         }
      }

        // sort the elements of an longs of doubles into ascending order with matching switches in an array of the length
        // using selection sort method
        // array determining the order is the first argument
        // matching array  is the second argument
        // sorted arrays returned as third and fourth arguments respectively
       public static void selectionSort(final long[] aa, final long[] bb, final long[] cc, final long[] dd){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            final int m = bb.length;
            if(n!=m)
             {
               throw new IllegalArgumentException("First argument array, aa, (length = " + n + ") and the second argument array, bb, (length = " + m + ") should be the same length"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int nn = cc.length;
            if(nn<n)
             {
               throw new IllegalArgumentException("The third argument array, cc, (length = " + nn + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int mm = dd.length;
            if(mm<m)
             {
               throw new IllegalArgumentException("The fourth argument array, dd, (length = " + mm + ") should be at least as long as the second argument array, bb, (length = " + m + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            long holdx = 0L;
            long holdy = 0L;


            for(int i=0; i<n; i++){
                cc[i]=aa[i];
                dd[i]=bb[i];
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(cc[i]<cc[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdx=cc[index];
                cc[index]=cc[lastIndex];
                cc[lastIndex]=holdx;
                holdy=dd[index];
                dd[index]=dd[lastIndex];
                dd[lastIndex]=holdy;
            }
       }

        // sort elements in an array of doubles (first argument) into ascending order
        // using selection sort method
        // returns the sorted array as second argument
        //  and an array of the indices of the sorted array as the third argument
        // same as corresponding selectionSort - retained for backward compatibility
        public static void selectSort(final double[] aa, final double[] bb, final int[] indices){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            final int m = bb.length;
            if(m<n)
             {
               throw new IllegalArgumentException("The second argument array, bb, (length = " + m + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int k = indices.length;
            if(m<n)
             {
               throw new IllegalArgumentException("The third argument array, indices, (length = " + k + ") should be at least as long as the first argument array, aa, (length = " + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            double holdb = 0.0D;
            int holdi = 0;
            for(int i=0; i<n; i++){
                bb[i]=aa[i];
                indices[i]=i;
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(bb[i]<bb[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdb=bb[index];
                bb[index]=bb[lastIndex];
                bb[lastIndex]=holdb;
                holdi=indices[index];
                indices[index]=indices[lastIndex];
                indices[lastIndex]=holdi;
            }
        }

        // sort elements in an array of doubles into ascending order
        // using selection sort method
        // returns ArrayList containing the original array, the sorted array
        //  and an array of the indices of the sorted array
        public static ArrayList<Object> selectSortArrayList(final double[] aa){
            int index = 0;
            int lastIndex = -1;
            final int n = aa.length;
            double holdb = 0.0D;
            int holdi = 0;
            final double[] bb = new double[n];
            final int[] indices = new int[n];
            for(int i=0; i<n; i++){
                bb[i]=aa[i];
                indices[i]=i;
            }

            while(lastIndex != n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(bb[i]<bb[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdb=bb[index];
                bb[index]=bb[lastIndex];
                bb[lastIndex]=holdb;
                holdi=indices[index];
                indices[index]=indices[lastIndex];
                indices[lastIndex]=holdi;
            }
            final ArrayList<Object> arrayl = new ArrayList<>();
            arrayl.add(aa);
            arrayl.add(bb);
            arrayl.add(indices);
            return arrayl;
        }

        // SORT ELEMENTS OF ARRAY  (deprecated - see ArryMaths class)
        // sort elements in an array of doubles into ascending order
        // using selection sort method
        // returns Vector containing the original array, the sorted array
        //  and an array of the indices of the sorted array
        public static Vector<Object> selectSortVector(final double[] aa){
            final ArrayList<Object> list = Fmath.selectSortArrayList(aa);
            Vector<Object> ret = null;
            if(list!=null){
                final int n = list.size();
                ret = new Vector<>(n);
                for(int i=0; i<n; i++) {
                  ret.addElement(list.get(i));
               }
            }
            return ret;
        }

        // recast an array of short as double
        public static double[] shortTOdouble(final short[] aa){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // recast an array of short as float
        public static float[] shortTOfloat(final short[] aa){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // recast an array of short as int
        public static int[] shortTOint(final short[] aa){
            final int n = aa.length;
            final int[] bb = new int[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // recast an array of short as long
        public static long[] shortTOlong(final short[] aa){
            final int n = aa.length;
            final long[] bb = new long[n];
            for(int i=0; i<n; i++){
               bb[i] = aa[i];
            }
            return bb;
        }

        // SIGN
        /*      returns -1 if x < 0 else returns 1   */
        //  double version
        public static double sign(final double x){
            if (x<0.0){
                return -1.0;
            }
            else{
                return 1.0;
            }
        }

        /*      returns -1 if x < 0 else returns 1   */
        //  float version
        public static float sign(final float x){
            if (x<0.0F){
                return -1.0F;
            }
            else{
                return 1.0F;
            }
        }

        /*      returns -1 if x < 0 else returns 1   */
        //  int version
        public static int sign(final int x){
            if (x<0){
                return -1;
            }
            else{
                return 1;
            }
        }

        /*      returns -1 if x < 0 else returns 1   */
        // long version
        public static long sign(final long x){
            if (x<0){
                return -1;
            }
            else{
                return 1;
            }
        }

        // Sine given angle in radians
        // for completion - returns Math.sin(arg)
        public static double sin(final double arg){
            return Math.sin(arg);
        }

        // Sine of angle between sides sideA and sideB given all side lengths of a triangle
        public static double sin(final double sideAC, final double sideBC, final double sideAB){
            final double angle = Fmath.angle(sideAC, sideBC, sideAB);
            return Math.sin(angle);
        }

        // Sine of angle subtended at coordinate C
        // given x, y coordinates of all apices, A, B and C, of a triangle
        public static double sin(final double xAtA, final double yAtA, final double xAtB, final double yAtB, final double xAtC, final double yAtC){
            final double angle = Fmath.angle(xAtA, yAtA, xAtB, yAtB, xAtC, yAtC);
            return Math.sin(angle);
        }

        // Unnormalised sinc (unnormalised sine cardinal)   sin(x)/x
        public static double sinc(final double a){
            if(Math.abs(a)<1e-40){
                return 1.0D;
            }
            else{
                return Math.sin(a)/a;
            }
        }

        //Hyperbolic sine of a double number
        public static double sinh(final double a){
            return 0.5D*(Math.exp(a)-Math.exp(-a));
        }

        // Square of a BigDecimal number
        public static BigDecimal square(final BigDecimal a){
            return a.multiply(a);
        }

        // Square of a BigInteger number
        public static BigInteger square(final BigInteger a){
            return a.multiply(a);
        }

        // SQUARES
        // Square of a double number
        public static double square(final double a){
            return a*a;
        }

        // Square of a float number
        public static float square(final float a){
            return a*a;
        }

        // Square of an int number
        public static int square(final int a){
            return a*a;
        }

        // Square of a long number
        public static long square(final long a){
            return a*a;
        }

        // SQUARE ROOT OF ARRAY ELEMENTS  (deprecated - see ArryMaths class)
        // Square root all elements of an array of doubles
        public static double[] squareRootElements(final double[] aa){
            final int n = aa.length;
            final double[] bb = new double[n];
            for(int i=0; i<n; i++) {
               bb[i] = Math.sqrt(aa[i]);
            }
            return bb;
        }

        // Square root all elements of an array of floats
        public static float[] squareRootElements(final float[] aa){
            final int n = aa.length;
            final float[] bb = new float[n];
            for(int i=0; i<n; i++) {
               bb[i] = (float)Math.sqrt(aa[i]);
            }
            return bb;
        }

        // Tangent given angle in radians
        // for completion - returns Math.tan(arg)
        public static double tan(final double arg){
            return Math.tan(arg);
        }

        // Tangent of angle between sides sideA and sideB given all side lengths of a triangle
        public static double tan(final double sideAC, final double sideBC, final double sideAB){
            final double angle = Fmath.angle(sideAC, sideBC, sideAB);
            return Math.tan(angle);
        }

        // Tangent of angle subtended at coordinate C
        // given x, y coordinates of all apices, A, B and C, of a triangle
        public static double tan(final double xAtA, final double yAtA, final double xAtB, final double yAtB, final double xAtC, final double yAtC){
            final double angle = Fmath.angle(xAtA, yAtA, xAtB, yAtB, xAtC, yAtC);
            return Math.tan(angle);
        }

        //Hyperbolic tangent of a double number
        public static double tanh(final double a){
            return sinh(a)/cosh(a);
        }

        // Converts tons to kilograms
        public static double tonToKg(final double tons){
            return  tons*1016.05;
        }

        // MANTISSA ROUNDING (TRUNCATING)
       // returns a value of xDouble truncated to trunc decimal places
       public static double truncate(final double xDouble, final int trunc){
           double xTruncated = xDouble;
           if(!Fmath.isNaN(xDouble)){
               if(!Fmath.isPlusInfinity(xDouble)){
                   if(!Fmath.isMinusInfinity(xDouble)){
                       if(xDouble!=0.0D){
                           final String xString = ((new Double(xDouble)).toString()).trim();
                           xTruncated = Double.parseDouble(truncateProcedure(xString, trunc));
                       }
                   }
               }
           }
           return xTruncated;
       }

        // returns a value of xFloat truncated to trunc decimal places
       public static float truncate(final float xFloat, final int trunc){
           float xTruncated = xFloat;
           if(!Fmath.isNaN(xFloat)){
               if(!Fmath.isPlusInfinity(xFloat)){
                   if(!Fmath.isMinusInfinity(xFloat)){
                       if(xFloat!=0.0D){
                           final String xString = ((new Float(xFloat)).toString()).trim();
                           xTruncated = Float.parseFloat(truncateProcedure(xString, trunc));
                       }
                   }
               }
           }
           return xTruncated;
       }

        // private method for truncating a float or double expressed as a String
       private static String truncateProcedure(final String xValue, final int trunc){

           String xTruncated = xValue;
           String xWorking = xValue;
           String exponent = " "; //$NON-NLS-1$
           String first = "+"; //$NON-NLS-1$
           int expPos = xValue.indexOf('E');
           int dotPos = xValue.indexOf('.');
           final int minPos = xValue.indexOf('-');

           if(minPos!=-1){
               if(minPos==0){
                   xWorking = xWorking.substring(1);
                   first = "-"; //$NON-NLS-1$
                   dotPos--;
                   expPos--;
               }
           }
           if(expPos>-1){
               exponent = xWorking.substring(expPos);
               xWorking = xWorking.substring(0,expPos);
           }
           String xPreDot = null;
           String xPostDot = "0"; //$NON-NLS-1$
           String xDiscarded = null;
           String tempString = null;
           double tempDouble = 0.0D;
           if(dotPos>-1){
               xPreDot = xWorking.substring(0,dotPos);
               xPostDot = xWorking.substring(dotPos+1);
               final int xLength = xPostDot.length();
               if(trunc<xLength){
                   xDiscarded = xPostDot.substring(trunc);
                   tempString = xDiscarded.substring(0,1) + "."; //$NON-NLS-1$
                   if(xDiscarded.length()>1){
                       tempString += xDiscarded.substring(1);
                   }
                   else{
                       tempString += "0"; //$NON-NLS-1$
                   }
                   tempDouble = Math.round(Double.parseDouble(tempString));

                   if(trunc>0){
                       if(tempDouble>=5.0){
                           final int[] xArray = new int[trunc+1];
                           xArray[0] = 0;
                           for(int i=0; i<trunc; i++){
                               xArray[i+1] = Integer.parseInt(xPostDot.substring(i,i+1));
                           }
                           boolean test = true;
                           int iCounter = trunc;
                           while(test){
                               xArray[iCounter] += 1;
                               if(iCounter>0){
                                   if(xArray[iCounter]<10){
                                       test = false;
                                   }
                                   else{
                                       xArray[iCounter]=0;
                                       iCounter--;
                                   }
                               }
                               else{
                                   test = false;
                               }
                           }
                           int preInt = Integer.parseInt(xPreDot);
                           preInt += xArray[0];
                           xPreDot = (new Integer(preInt)).toString();
                           tempString = UI.EMPTY_STRING;
                           for(int i=1; i<=trunc; i++){
                               tempString += (new Integer(xArray[i])).toString();
                           }
                           xPostDot = tempString;
                       }
                       else{
                           xPostDot = xPostDot.substring(0, trunc);
                       }
                   }
                   else{
                       if(tempDouble>=5.0){
                           int preInt = Integer.parseInt(xPreDot);
                           preInt++;
                           xPreDot = (new Integer(preInt)).toString();
                       }
                       xPostDot = "0"; //$NON-NLS-1$
                   }
               }
               xTruncated = first + xPreDot.trim() + "." + xPostDot.trim() + exponent; //$NON-NLS-1$
           }
           return xTruncated.trim();
       }

        // Versine
        public static double vers(final double a){
            return (1.0D - Math.cos(a));
        }

        // Converts yards to metres
        public static double yardToMetre(final double yd){
            return  yd*0.9144;
        }
}

