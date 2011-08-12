/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.math;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This is a java implementation of the C source code provided by Didier Jamet
 * 
 * <pre>
 * 
 * #include <stdio.h>
 * #include <stdlib.h>
 * #include <math.h>
 * 
 * #define SIZE 1528
 * #define pi 3.1415926535897932384626433832795
 * 
 * /////////////////////////////////////////////////
 * // Filters the high frequencies of field(time) //
 * /////////////////////////////////////////////////
 * 
 * //==========================
 * // Main filtering function
 * //==========================
 * void smoothing1(double* time, double* field, double* field_sc, double tau)
 * {
 *   int i;
 *   double field_sf[SIZE], field_sb[SIZE];
 *   double dt;
 * 
 * // Forward smoothing
 * //------------------
 *   field_sf[0] = field[0];
 *   for (i=1; i<SIZE; i++)
 *     {
 *       dt = (time[i] - time[i-1]) / tau;
 *       field_sf[i] = ( field_sf[i-1] + dt * field[i] ) / (1. + dt);
 *     }
 * // Backward smoothing
 * //-------------------
 *   field_sb[SIZE-1] = field[SIZE-1];
 *   for (i=2; i<SIZE+1; i++)
 *     {
 *       dt = (time[SIZE-i+1] - time[SIZE-i]) / tau;
 *       field_sb[SIZE-i] = ( field_sb[SIZE-i+1] + dt * field[SIZE-i] ) / (1. + dt);
 *     }
 * // Centered smoothing
 * //-------------------
 *   for (i=0; i<SIZE; i++)
 *     {
 *       field_sc[i] = (field_sf[i] + field_sb[i])/2.;
 *     }
 * }
 * 
 * //=================================================
 * // Filters twice:
 * //    1. a first time with relaxation time tau
 * //    2. a second time with relaxation time tau/4
 * //=================================================
 * void smoothing(double* time, double* field, double* field_sc, double tau, int keep_start_end)
 * {
 *   int i;
 *   double field_sc1[SIZE];
 *   double Delta_start, Delta_end;
 * 
 * // First smoothing with tau
 * //-------------------------
 *   smoothing1(time, field, field_sc1, tau);
 * 
 * // Second smoothing with tau/4
 * //----------------------------
 *   smoothing1(time, field_sc1, field_sc, tau/4);
 * 
 * // Keep start and end values
 * //--------------------------
 *   if (keep_start_end == 1)
 *     {
 *       Delta_start = field[0] - field_sc[0];
 *       Delta_end = field[SIZE-1] - field_sc[SIZE-1];
 *       for (i=0; i<SIZE; i++)
 *         {
 *           field_sc[i] = field_sc[i] + Delta_start + (Delta_end-Delta_start) / (double)(SIZE-1) * (double)(i);
 *         }
 *     }
 * }
 * 
 * ////////////////////////////////////////////////
 * // Computes the distance between 2 GPS points //
 * ////////////////////////////////////////////////
 * double distance_gps(double lat1, double lat2, double lon1, double lon2)
 * {
 *   double earth_radius = 6366000.; // earth radius in meters
 *   double lat1_rad = lat1*pi/180.;
 *   double lat2_rad = lat2*pi/180.;
 *   double lon1_rad = lon1*pi/180.;
 *   double lon2_rad = lon2*pi/180.;
 * 
 *   return( earth_radius * 2. * asin( sqrt( (sin((lat1_rad-lat2_rad)/2.)) * (sin((lat1_rad-lat2_rad)/2.)) + cos(lat1_rad) * cos(lat2_rad) * (sin((lon1_rad-lon2_rad)/2.)) * (sin((lon1_rad-lon2_rad)/2.)) ) ) );
 * }
 * 
 * 
 * main()
 * {
 *   int i;
 *   double time[SIZE];
 *   double latitude[SIZE], longitude[SIZE];
 *   double altitude[SIZE], altitude_sc[SIZE];
 *   double heart_rate[SIZE], heart_rate_sc[SIZE];
 *   double distance[SIZE], distance_sc[SIZE];
 *   double Vh_ini[SIZE], Vh[SIZE], Vh_sc[SIZE];
 *   double Vv_ini[SIZE], Vv[SIZE], Vv_sc[SIZE];
 *   double slope_sc[SIZE];
 *   double tau=30.;
 * 
 *   FILE *file;
 * 
 * // Initialization
 * //===============
 *   for(i=0; i<SIZE; i++)
 *     {
 *       time[i] = -1.;
 *       latitude[i] = -1.;
 *       longitude[i] = -1.;
 *       altitude[i] = -1.;
 *       heart_rate[i] = -1.;
 *     }
 * 
 * // Reading raw data in a text file
 * //================================
 *   file = fopen("example.txt", "r");
 *   for(i=0; i<SIZE; i++)
 *     {
 *       fscanf(file, "%lf\t%lf\t%lf\t%lf\t%lf\n", &time[i], &latitude[i], &longitude[i], &altitude[i], &heart_rate[i]);
 *     }
 *   fclose(file);
 * 
 * // Modify data so that no value is invalid (i.e. equal -1)
 * //========================================================
 * // We look for the first valid value (i.e. different from -1) and set the initial value to that first valid value
 * //---------------------------------------------------------------------------------------------------------------
 * // Latitude
 * //.........
 *   if (latitude[0]==-1)
 *     {
 *       i=0;
 *       do
 *         i++;
 *       while (latitude[i]==-1);
 *       latitude[0]=latitude[i];
 *     }
 * // Longitude
 * //..........
 *   if (longitude[0]==-1)
 *     {
 *       i=0;
 *       do
 *         i++;
 *       while (longitude[i]==-1);
 *       longitude[0]=longitude[i];
 *     }
 * // Altitude
 * //.........
 *   if (altitude[0]==-1)
 *     {
 *       i=0;
 *       do
 *         i++;
 *       while (altitude[i]==-1);
 *       altitude[0]=altitude[i];
 *     }
 * // Heart Rate
 * //...........
 *   if (heart_rate[0]==-1)
 *     {
 *       i=0;
 *       do
 *         i++;
 *       while (heart_rate[i]==-1);
 *       heart_rate[0]=heart_rate[i];
 *     }
 * 
 * // If the value is invalid (i.e. equal -1), we set it to the previous value (in time)
 * //-----------------------------------------------------------------------------------
 *   for (i=1; i<SIZE; i++)
 *     {
 *       if (latitude[i]==-1)
 *         latitude[i]=latitude[i-1];
 *       if (longitude[i]==-1)
 *         longitude[i]=longitude[i-1];
 *       if (altitude[i]==-1)
 *         altitude[i]=altitude[i-1];
 *       if (heart_rate[i]==-1)
 *         heart_rate[i]=heart_rate[i-1];
 *     }
 * 
 * // Compute the distance from latitude and longitude data
 * //======================================================
 *   distance[0] = 0.;
 *   for (i=1; i<SIZE; i++)
 *     {
 *       distance[i] = distance[i-1] + distance_gps(latitude[i], latitude[i-1], longitude[i], longitude[i-1]);
 *     }
 * 
 * // Compute the horizontal and vertical speeds from the raw distance and altitude data
 * //===================================================================================
 *  for (i=0; i<SIZE-1; i++)
 *     {
 *       if (time[i+1] == time[i])
 *         {
 *           if (i==0)
 *             {
 *               Vh_ini[i] = 0.;
 *               Vv_ini[i] = 0.;
 *             }
 *           else
 *             {
 *               Vh_ini[i] = Vh_ini[i-1];
 *               Vv_ini[i] = Vv_ini[i-1];
 *             }
 *         }
 *       else
 *         {
 *           Vh_ini[i] = (distance[i+1] - distance[i]) / (time[i+1] - time[i]);
 *           Vv_ini[i] = (altitude[i+1] - altitude[i]) / (time[i+1] - time[i]);
 *         }
 *     }
 *   Vh_ini[SIZE-1] = Vh_ini[SIZE-2];
 *   Vv_ini[SIZE-1] = Vv_ini[SIZE-2];
 * 
 * // Smooth out the time variations of the distance, the altitude and the heart rate
 * //================================================================================
 *   smoothing(time, distance, distance_sc, tau, 0);
 *   smoothing(time, altitude, altitude_sc, tau, 0);
 *   smoothing(time, heart_rate, heart_rate_sc, tau, 0);
 * 
 * // Compute the horizontal and vertical speeds from the smoothed distance and altitude
 * //===================================================================================
 *  for (i=0; i<SIZE-1; i++)
 *     {
 *       if (time[i+1] == time[i])
 *         {
 *           if (i==0)
 *             {
 *               Vh[i] = 0.;
 *               Vv[i] = 0.;
 *             }
 *           else
 *             {
 *               Vh[i] = Vh[i-1];
 *               Vv[i] = Vv[i-1];
 *             }
 *         }
 *       else
 *         {
 *           Vh[i] = (distance_sc[i+1] - distance_sc[i]) / (time[i+1] - time[i]);
 *           Vv[i] = (altitude_sc[i+1] - altitude_sc[i]) / (time[i+1] - time[i]);
 *         }
 *     }
 *   Vh[SIZE-1] = Vh[SIZE-2];
 *   Vv[SIZE-1] = Vv[SIZE-2];
 * 
 * 
 * // Smooth out the time variations of the horizontal and vertical speeds
 * //=====================================================================
 *   smoothing(time, Vh, Vh_sc, tau, 0);
 *   smoothing(time, Vv, Vv_sc, tau, 0);
 * 
 * // Compute the terrain slope
 * //==========================
 *   for (i=0; i<SIZE; i++)
 *     {
 *       slope_sc[i] = Vv_sc[i] / Vh_sc[i] *100.;
 *     }
 * 
 *   file = fopen("smooth_simple_ini.txt", "w");
 *   for(i=0; i<SIZE; i++)
 *     {
 *       fprintf(file, "%g\t%g\t%g\t%g\t%g\t%g\n", time[i], distance[i], altitude[i], Vh_ini[i], Vv_ini[i], heart_rate[i]);
 *     }
 *   fclose(file);
 * 
 *   file = fopen("smooth_simple_sc.txt", "w");
 *   for(i=0; i<SIZE; i++)
 *     {
 *       fprintf(file, "%g\t%g\t%g\t%g\t%g\t%g\t%g\n", time[i], distance_sc[i], altitude_sc[i], Vh_sc[i], Vv_sc[i], slope_sc[i], heart_rate_sc[i]);
 *     }
 *   fclose(file);
 * }
 * 
 * 
 * </pre>
 */

public class Smooth {

	private final static IPreferenceStore	_prefStore	= TourbookPlugin.getDefault()//
																.getPreferenceStore();

	private static final double				pi			= 3.1415926535897932384626433832795;

//	private static void _main() {
//
//		int i;
//		final int size = 888;
//
//		final double time[] = new double[size];
//		final double latitude[] = new double[size], longitude[] = new double[size];
//		final double altitude[] = new double[size], altitude_sc[] = new double[size];
//		final double heart_rate[] = new double[size], heart_rate_sc[] = new double[size];
//		final double distance[] = new double[size], distance_sc[] = new double[size];
//		final double Vh_ini[] = new double[size], Vh[] = new double[size], Vh_sc[] = new double[size];
//		final double Vv_ini[] = new double[size], Vv[] = new double[size], Vv_sc[] = new double[size];
//		final double slope_sc[] = new double[size];
//		final double tau = 30.;
//
////	  FILE *file;
//
//		// Initialization
//		//===============
//		for (i = 0; i < size; i++) {
//			time[i] = -1.;
//			latitude[i] = -1.;
//			longitude[i] = -1.;
//			altitude[i] = -1.;
//			heart_rate[i] = -1.;
//		}
//
////	// Reading raw data in a text file
////	//================================
////	  file = fopen("example.txt", "r");
////	  for(i=0; i<SIZE; i++)
////	    {
////	      fscanf(file, "%lf\t%lf\t%lf\t%lf\t%lf\n", &time[i], &latitude[i], &longitude[i], &altitude[i], &heart_rate[i]);
////	    }
////	  fclose(file);
//
//		// Modify data so that no value is invalid (i.e. equal -1)
//		//========================================================
//		// We look for the first valid value (i.e. different from -1) and set the initial value to that first valid value
//		//---------------------------------------------------------------------------------------------------------------
//		// Latitude
//		//.........
//		if (latitude[0] == -1) {
//			i = 0;
//			do {
//				i++;
//			}
//			while (latitude[i] == -1);
//			latitude[0] = latitude[i];
//		}
//		// Longitude
//		//..........
//		if (longitude[0] == -1) {
//			i = 0;
//			do {
//				i++;
//			}
//			while (longitude[i] == -1);
//			longitude[0] = longitude[i];
//		}
//		// Altitude
//		//.........
//		if (altitude[0] == -1) {
//			i = 0;
//			do {
//				i++;
//			}
//			while (altitude[i] == -1);
//			altitude[0] = altitude[i];
//		}
//		// Heart Rate
//		//...........
//		if (heart_rate[0] == -1) {
//			i = 0;
//			do {
//				i++;
//			}
//			while (heart_rate[i] == -1);
//			heart_rate[0] = heart_rate[i];
//		}
//
//		// If the value is invalid (i.e. equal -1), we set it to the previous value (in time)
//		//-----------------------------------------------------------------------------------
//		for (i = 1; i < size; i++) {
//			if (latitude[i] == -1) {
//				latitude[i] = latitude[i - 1];
//			}
//			if (longitude[i] == -1) {
//				longitude[i] = longitude[i - 1];
//			}
//			if (altitude[i] == -1) {
//				altitude[i] = altitude[i - 1];
//			}
//			if (heart_rate[i] == -1) {
//				heart_rate[i] = heart_rate[i - 1];
//			}
//		}
//
//		// Compute the distance from latitude and longitude data
//		//======================================================
//		distance[0] = 0.;
//		for (i = 1; i < size; i++) {
//			distance[i] = distance[i - 1] + distance_gps(latitude[i], latitude[i - 1], longitude[i], longitude[i - 1]);
//		}
//
//		// Compute the horizontal and vertical speeds from the raw distance and altitude data
//		//===================================================================================
//		for (i = 0; i < size - 1; i++) {
//			if (time[i + 1] == time[i]) {
//				if (i == 0) {
//					Vh_ini[i] = 0.;
//					Vv_ini[i] = 0.;
//				} else {
//					Vh_ini[i] = Vh_ini[i - 1];
//					Vv_ini[i] = Vv_ini[i - 1];
//				}
//			} else {
//				Vh_ini[i] = (distance[i + 1] - distance[i]) / (time[i + 1] - time[i]);
//				Vv_ini[i] = (altitude[i + 1] - altitude[i]) / (time[i + 1] - time[i]);
//			}
//		}
//		Vh_ini[size - 1] = Vh_ini[size - 2];
//		Vv_ini[size - 1] = Vv_ini[size - 2];
//
//		// Smooth out the time variations of the distance, the altitude and the heart rate
//		//================================================================================
////		smoothing(time, distance, distance_sc, tau, 0);
////		smoothing(time, altitude, altitude_sc, tau, 0);
////		smoothing(time, heart_rate, heart_rate_sc, tau, 0);
//
//		// Compute the horizontal and vertical speeds from the smoothed distance and altitude
//		//===================================================================================
//		for (i = 0; i < size - 1; i++) {
//			if (time[i + 1] == time[i]) {
//				if (i == 0) {
//					Vh[i] = 0.;
//					Vv[i] = 0.;
//				} else {
//					Vh[i] = Vh[i - 1];
//					Vv[i] = Vv[i - 1];
//				}
//			} else {
//				Vh[i] = (distance_sc[i + 1] - distance_sc[i]) / (time[i + 1] - time[i]);
//				Vv[i] = (altitude_sc[i + 1] - altitude_sc[i]) / (time[i + 1] - time[i]);
//			}
//		}
//		Vh[size - 1] = Vh[size - 2];
//		Vv[size - 1] = Vv[size - 2];
//
//		// Smooth out the time variations of the horizontal and vertical speeds
//		//=====================================================================
////		smoothing(time, Vh, Vh_sc, tau, 0);
////		smoothing(time, Vv, Vv_sc, tau, 0);
//
//		// Compute the terrain slope
//		//==========================
//		for (i = 0; i < size; i++) {
//			slope_sc[i] = Vv_sc[i] / Vh_sc[i] * 100.;
//		}
//
////	  file = fopen("smooth_simple_ini.txt", "w");
////	  for(i=0; i<SIZE; i++)
////	    {
////	      fprintf(file, "%g\t%g\t%g\t%g\t%g\t%g\n", time[i], distance[i], altitude[i], Vh_ini[i], Vv_ini[i], heart_rate[i]);
////	    }
////	  fclose(file);
////
////	  file = fopen("smooth_simple_sc.txt", "w");
////	  for(i=0; i<SIZE; i++)
////	    {
////	      fprintf(file, "%g\t%g\t%g\t%g\t%g\t%g\t%g\n", time[i], distance_sc[i], altitude_sc[i], Vh_sc[i], Vv_sc[i], slope_sc[i], heart_rate_sc[i]);
////	    }
////	  fclose(file);
//	}

	/**
	 * Computes the distance between 2 GPS points
	 * 
	 * @param lat1
	 * @param lat2
	 * @param lon1
	 * @param lon2
	 * @return
	 */
	private static double distance_gps(final double lat1, final double lat2, final double lon1, final double lon2) {

		final double earth_radius = 6366000.; // earth radius in meters
		final double lat1_rad = lat1 * pi / 180.;
		final double lat2_rad = lat2 * pi / 180.;
		final double lon1_rad = lon1 * pi / 180.;
		final double lon2_rad = lon2 * pi / 180.;

		final double sinLat = Math.sin((lat1_rad - lat2_rad) / 2.);
		final double sinLon = Math.sin((lon1_rad - lon2_rad) / 2.);

		return (earth_radius * 2. * Math.asin(//
				Math.sqrt(sinLat * sinLat + Math.cos(lat1_rad) * Math.cos(lat2_rad) * sinLon * sinLon)));
	}

//	private static void setSpeed(	final int serieIndex,
//									final int speedMetric,
//									final int speedImperial,
//									final int timeDiff,
//									final int distDiff) {
//
////		speedSerie[serieIndex] = speedMetric;
////		speedSerieImperial[serieIndex] = speedImperial;
////
////		maxSpeed = Math.max(maxSpeed, speedMetric);
////
////		/*
////		 * pace (computed with divisor 10)
////		 */
////		float paceMetricSeconds = 0;
////		float paceImperialSeconds = 0;
////		int paceMetricMinute = 0;
////		int paceImperialMinute = 0;
////
////		if ((speedMetric != 0) && (distDiff != 0)) {
////
////			paceMetricSeconds = timeDiff * 10000 / (float) distDiff;
////			paceImperialSeconds = paceMetricSeconds * UI.UNIT_MILE;
////
////			paceMetricMinute = (int) ((paceMetricSeconds / 60));
////			paceImperialMinute = (int) ((paceImperialSeconds / 60));
////		}
////
////		paceSerieMinute[serieIndex] = paceMetricMinute;
////		paceSerieMinuteImperial[serieIndex] = paceImperialMinute;
////
////		paceSerieSeconds[serieIndex] = (int) paceMetricSeconds / 10;
////		paceSerieSecondsImperial[serieIndex] = (int) paceImperialSeconds / 10;
//	}

	public static void smoothDataSeries(final int[] timeSerie,
										final int[] distanceSerie,
										final int[] altitudeSerie,
										final int[] altitudeSerieImperial,
										final int[] speedSerie,
										final int[] speedSerieImperial,
										final int[] paceSerieMinute,
										final int[] paceSerieMinuteImperial,
										final int[] paceSerieSeconds,
										final int[] paceSerieSecondsImperial,
										final int[] gradientSerie,
										final int[] altimeterSerie,
										final int[] altimeterSerieImperial,
										final boolean isUseLatLon,
										final double[] latitudeSerie,
										final double[] longitudeSerie) {

		final int size = timeSerie.length;

		final double[] distance = new double[size];
		final double[] distance_sc = new double[size];
		final double altitude[] = new double[size];
		final double altitude_sc[] = new double[size];

		final double Vh_ini[] = new double[size];
		final double Vh[] = new double[size];
		final double Vh_sc[] = new double[size];

		final double Vv_ini[] = new double[size];
		final double Vv[] = new double[size];
		final double Vv_sc[] = new double[size];

		final double tauAltitude = _prefStore.getDouble(ITourbookPreferences.GRAPH_SMOOTHING_ALTITUDE_TAU);
//		final double tauPulse = _prefStore.getDouble(ITourbookPreferences.GRAPH_SMOOTHING_PULSE_TAU);
		final double tauSpeed = _prefStore.getDouble(ITourbookPreferences.GRAPH_SMOOTHING_SPEED_TAU);

		// Compute the distance from latitude and longitude data
		//======================================================
		if (isUseLatLon) {

			distance[0] = 0.;
			for (int serieIndex = 1; serieIndex < size; serieIndex++) {
				distance[serieIndex] = distance[serieIndex - 1]
						+ distance_gps(
								latitudeSerie[serieIndex],
								latitudeSerie[serieIndex - 1],
								longitudeSerie[serieIndex],
								longitudeSerie[serieIndex - 1]);
			}

		} else {

			for (int serieIndex = 0; serieIndex < size; serieIndex++) {
				distance[serieIndex] = distanceSerie[serieIndex];
			}
		}

		for (int serieIndex = 0; serieIndex < size; serieIndex++) {
			altitude[serieIndex] = altitudeSerie[serieIndex];
		}

		// Compute the horizontal and vertical speeds from the raw distance and altitude data
		//===================================================================================
		for (int serieIndex = 0; serieIndex < size - 1; serieIndex++) {

			if (timeSerie[serieIndex + 1] == timeSerie[serieIndex]) {

				if (serieIndex == 0) {
					Vh_ini[serieIndex] = 0.;
					Vv_ini[serieIndex] = 0.;
				} else {
					Vh_ini[serieIndex] = Vh_ini[serieIndex - 1];
					Vv_ini[serieIndex] = Vv_ini[serieIndex - 1];
				}

			} else {

				Vh_ini[serieIndex] = (distance[serieIndex + 1] - distance[serieIndex])
						/ (timeSerie[serieIndex + 1] - timeSerie[serieIndex]);

				Vv_ini[serieIndex] = (altitude[serieIndex + 1] - altitude[serieIndex])
						/ (timeSerie[serieIndex + 1] - timeSerie[serieIndex]);
			}
		}
		Vh_ini[size - 1] = Vh_ini[size - 2];
		Vv_ini[size - 1] = Vv_ini[size - 2];

		// Smooth out the time variations of the distance, the altitude and the heart rate
		//================================================================================
		smoothing(timeSerie, distance, distance_sc, tauSpeed, 0);
		smoothing(timeSerie, altitude, altitude_sc, tauAltitude, 0);
//		smoothing(timeSerie, heart_rate, heart_rate_sc, tau, 0);

		// Compute the horizontal and vertical speeds from the smoothed distance and altitude
		//===================================================================================
		for (int serieIndex = 0; serieIndex < size - 1; serieIndex++) {
			if (timeSerie[serieIndex + 1] == timeSerie[serieIndex]) {
				if (serieIndex == 0) {
					Vh[serieIndex] = 0.;
					Vv[serieIndex] = 0.;
				} else {
					Vh[serieIndex] = Vh[serieIndex - 1];
					Vv[serieIndex] = Vv[serieIndex - 1];
				}
			} else {
				Vh[serieIndex] = (distance_sc[serieIndex + 1] - distance_sc[serieIndex])
						/ (timeSerie[serieIndex + 1] - timeSerie[serieIndex]);
				Vv[serieIndex] = (altitude_sc[serieIndex + 1] - altitude_sc[serieIndex])
						/ (timeSerie[serieIndex + 1] - timeSerie[serieIndex]);
			}
		}
		Vh[size - 1] = Vh[size - 2];
		Vv[size - 1] = Vv[size - 2];

		// Smooth out the time variations of the horizontal and vertical speeds
		//=====================================================================
		smoothing(timeSerie, Vh, Vh_sc, tauSpeed, 0);
		smoothing(timeSerie, Vv, Vv_sc, tauAltitude, 0);

		// Compute the terrain slope
		//==========================
		for (int serieIndex = 0; serieIndex < size; serieIndex++) {
			gradientSerie[serieIndex] = (int) (Vv_sc[serieIndex] / Vh_sc[serieIndex] * 1000.);
			altimeterSerie[serieIndex] = (int) (Vv_sc[serieIndex] * 3600.);
		}

		for (int serieIndex = 0; serieIndex < Vh.length; serieIndex++) {

			final double speedMetric = Vh[serieIndex] * 36;

			speedSerie[serieIndex] = (int) speedMetric;
			speedSerieImperial[serieIndex] = (int) (speedMetric / UI.UNIT_MILE);
		}
	}

	/**
	 * Filters twice: <br>
	 * 1. first time with relaxation time tau <br>
	 * 2. second time with relaxation time tau/4
	 * 
	 * @param time
	 * @param field
	 * @param field_sc
	 * @param tau
	 * @param keep_start_end
	 */
	private static void smoothing(	final int time[],
									final double field[],
									final double field_sc[],
									final double tau,
									final int keep_start_end) {
		int i;
		final int size = field.length;

		final double field_sc1[] = new double[size];
		double Delta_start, Delta_end;

		// First smoothing with tau
		//-------------------------
		smoothing1(time, field, field_sc1, tau);

		// Second smoothing with tau/4
		//----------------------------
		smoothing1(time, field_sc1, field_sc, tau / 4);

		// Keep start and end values
		//--------------------------
		if (keep_start_end == 1) {

			Delta_start = field[0] - field_sc[0];
			Delta_end = field[size - 1] - field_sc[size - 1];

			for (i = 0; i < size; i++) {
				field_sc[i] = field_sc[i] + Delta_start + (Delta_end - Delta_start) / (size - 1) * (i);
			}
		}
	}

	/**
	 * Main filtering function
	 * 
	 * @param time
	 * @param field
	 * @param field_sc
	 * @param tau
	 */
	private static void smoothing1(final int time[], final double field[], final double field_sc[], final double tau) {

		int i;
		final int size = field.length;

		final double field_sf[] = new double[size];
		final double field_sb[] = new double[size];
		double dt;

		// Forward smoothing
		//------------------
		field_sf[0] = field[0];
		for (i = 1; i < size; i++) {
			dt = (time[i] - time[i - 1]) / tau;
			field_sf[i] = (field_sf[i - 1] + dt * field[i]) / (1. + dt);
		}

		// Backward smoothing
		//-------------------
		field_sb[size - 1] = field[size - 1];
		for (i = 2; i < size + 1; i++) {
			dt = (time[size - i + 1] - time[size - i]) / tau;
			field_sb[size - i] = (field_sb[size - i + 1] + dt * field[size - i]) / (1. + dt);
		}

		// Centered smoothing
		//-------------------
		for (i = 0; i < size; i++) {
			field_sc[i] = (field_sf[i] + field_sb[i]) / 2.;
		}

	}
}
