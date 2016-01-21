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
package net.tourbook.common.util;

/**
 * MyTourbook Mathematics
 */
public class MtMath {

	/**
	 * !!! ORIGINAL CODE !!!
	 * <p>
	 * javascript source location: http://www.movable-type.co.uk/scripts/latlong-vincenty.html
	 * 
	 * <pre>
	 * 
	 * 	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  *
	 * 	/* Vincenty Inverse Solution of Geodesics on the Ellipsoid (c) Chris Veness 2002-2010             *
	 * 	/*                                                                                                *
	 * 	/* from: Vincenty inverse formula - T Vincenty, "Direct and Inverse Solutions of Geodesics on the *
	 * 	/*       Ellipsoid with application of nested equations", Survey Review, vol XXII no 176, 1975    *
	 * 	/*       http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf                                             *
	 * 	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  *
	 * 
	 * 	/**
	 * Calculates geodetic distance between two points specified by latitude/longitude using
	 * Vincenty inverse formula for ellipsoids
	 * 
	 * @param   {Number} lat1, lon1: first point in decimal degrees
	 * @param   {Number} lat2, lon2: second point in decimal degrees
	 * @returns (Number} distance in metres between points
	 * 
	 * 	function distVincenty(lat1, lon1, lat2, lon2) {
	 * 	  var a = 6378137, b = 6356752.314245,  f = 1/298.257223563;  // WGS-84 ellipsoid params
	 * 	  var L = (lon2-lon1).toRad();
	 * 	  var U1 = Math.atan((1-f) * Math.tan(lat1.toRad()));
	 * 	  var U2 = Math.atan((1-f) * Math.tan(lat2.toRad()));
	 * 	  var sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
	 * 	  var sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);
	 * 
	 * 	  var lambda = L, lambdaP, iterLimit = 100;
	 * 	  do {
	 * 	    var sinLambda = Math.sin(lambda), cosLambda = Math.cos(lambda);
	 * 	    var sinSigma = Math.sqrt((cosU2*sinLambda) * (cosU2*sinLambda) +
	 * 	      (cosU1*sinU2-sinU1*cosU2*cosLambda) * (cosU1*sinU2-sinU1*cosU2*cosLambda));
	 * 	    if (sinSigma==0) return 0;  // co-incident points
	 * 	    var cosSigma = sinU1*sinU2 + cosU1*cosU2*cosLambda;
	 * 	    var sigma = Math.atan2(sinSigma, cosSigma);
	 * 	    var sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
	 * 	    var cosSqAlpha = 1 - sinAlpha*sinAlpha;
	 * 	    var cos2SigmaM = cosSigma - 2*sinU1*sinU2/cosSqAlpha;
	 * 	    if (isNaN(cos2SigmaM)) cos2SigmaM = 0;  // equatorial line: cosSqAlpha=0 (§6)
	 * 	    var C = f/16*cosSqAlpha*(4+f*(4-3*cosSqAlpha));
	 * 	    lambdaP = lambda;
	 * 	    lambda = L + (1-C) * f * sinAlpha *
	 * 	      (sigma + C*sinSigma*(cos2SigmaM+C*cosSigma*(-1+2*cos2SigmaM*cos2SigmaM)));
	 * 	  } while (Math.abs(lambda-lambdaP) > 1e-12 && --iterLimit>0);
	 * 
	 * 	  if (iterLimit==0) return NaN  // formula failed to converge
	 * 
	 * 	  var uSq = cosSqAlpha * (a*a - b*b) / (b*b);
	 * 	  var A = 1 + uSq/16384*(4096+uSq*(-768+uSq*(320-175*uSq)));
	 * 	  var B = uSq/1024 * (256+uSq*(-128+uSq*(74-47*uSq)));
	 * 	  var deltaSigma = B*sinSigma*(cos2SigmaM+B/4*(cosSigma*(-1+2*cos2SigmaM*cos2SigmaM)-
	 * 	    B/6*cos2SigmaM*(-3+4*sinSigma*sinSigma)*(-3+4*cos2SigmaM*cos2SigmaM)));
	 * 	  var s = b*A*(sigma-deltaSigma);
	 * 
	 * 	  s = s.toFixed(3); // round to 1mm precision
	 * 	  return s;
	 * 
	 * 	  // note: to return initial/final bearings in addition to distance, use something like:
	 * 	  var fwdAz = Math.atan2(cosU2*sinLambda,  cosU1*sinU2-sinU1*cosU2*cosLambda);
	 * 	  var revAz = Math.atan2(cosU1*sinLambda, -sinU1*cosU2+cosU1*sinU2*cosLambda);
	 * 	  return { distance: s, initialBearing: fwdAz.toDeg(), finalBearing: revAz.toDeg() };
	 * 	}
	 * 
	 * 	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  *
	 * 
	 * </pre>
	 */

	// WGS-84 Ellipsoid
	private static final double	HALBACHSE_A		= 6378.137;
	private static final double	HALBACHSE_B		= 6356.7523142;

	// = 1/298.2572229328709613   1/298.257223563 // ca. (A-B)/A
	private static final double	ABPLATTUNG_F	= (HALBACHSE_A - HALBACHSE_B) / HALBACHSE_A;

	/**
	 * Calculates geodetic distance between two points specified by latitude/longitude using
	 * Vincenty inverse formula for ellipsoids
	 * <p>
	 * <p>
	 * Vincenty Inverse Solution of Geodesics on the Ellipsoid (c) Chris Veness 2002-2010
	 * <p>
	 * from: Vincenty inverse formula - T Vincenty, "Direct and Inverse Solutions of Geodesics on
	 * the Ellipsoid with application of nested equations", Survey Review, vol XXII no 176, 1975 *
	 * <p>
	 * http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
	 * 
	 * @param {Number} lat1, lon1: first point in decimal degrees
	 * @param {Number} lat2, lon2: second point in decimal degrees
	 * @returns (Number} distance in metres between points
	 */
	public static double distanceVincenty(final double lat1, final double lon1, final double lat2, final double lon2) {

		final double L = Math.toRadians(lon2 - lon1);
		final double U1 = Math.atan((1 - ABPLATTUNG_F) * Math.tan(Math.toRadians(lat1)));
		final double U2 = Math.atan((1 - ABPLATTUNG_F) * Math.tan(Math.toRadians(lat2)));
		final double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
		final double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

		double lambda = L, lambdaP = 2 * Math.PI;
		int iterLimit = 20;
		double cosSqAlpha = 0, sinSigma = 0, cos2SigmaM = 0, cosSigma = 0, sigma = 0;

		while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0) {

			final double sinLambda = Math.sin(lambda), cosLambda = Math.cos(lambda);

			sinSigma = Math.sqrt((cosU2 * sinLambda)
					* (cosU2 * sinLambda)
					+ (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
					* (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));

			if (sinSigma == 0) {
				return 0; // co-incident points
			}

			cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
			sigma = Math.atan2(sinSigma, cosSigma);
			final double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
			cosSqAlpha = 1 - sinAlpha * sinAlpha;

			if (cosSqAlpha == 0) {
				return Math.abs(HALBACHSE_A * L); // two points on equator  // neu 20.04.2006
			}

			cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha; // nach Erg. auf Website
			// cos2SigmaM = cosSigma;
			// if (cosSqAlpha != 0) cos2SigmaM -= 2*sinU1*sinU2/cosSqAlpha; // Abfrage auf 0 neu 27.02.2005

			final double C = ABPLATTUNG_F / 16 * cosSqAlpha * (4 + ABPLATTUNG_F * (4 - 3 * cosSqAlpha));
			lambdaP = lambda;

			lambda = L
					+ (1 - C)
					* ABPLATTUNG_F
					* sinAlpha
					* (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
		}

		if (iterLimit == 0) {
			return 0; // formula failed to converge
		}

		final double uSq = cosSqAlpha
				* (HALBACHSE_A * HALBACHSE_A - HALBACHSE_B * HALBACHSE_B)
				/ (HALBACHSE_B * HALBACHSE_B);

		final double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
		final double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));

		final double deltaSigma = B
				* sinSigma
				* (cos2SigmaM + B
						/ 4
						* (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B
								/ 6
								* cos2SigmaM
								* (-3 + 4 * sinSigma * sinSigma)
								* (-3 + 4 * cos2SigmaM * cos2SigmaM)));

		final double s = HALBACHSE_B * A * (sigma - deltaSigma);

		return s * 1000;
	}

	/*
	 * vincenty algorithm is much more accurate compared with haversine
	 */
//	/**
//	 * Haversine Formula to calculate distance between 2 geo points
//	 * <p>
//	 * <a href="http://en.wikipedia.org/wiki/Haversine_formula"
//	 * >http://en.wikipedia.org/wiki/Haversine_formula</a>
//	 */
//	public static double distanceHaversine(final double lat1, final double lon1, final double lat2, final double lon2) {
//
//		if (lat1 == lat2 && lon1 == lon2) {
//			return 0;
//		}
//
//		final double dLat = Math.toRadians(lat2 - lat1);
//		final double dLon = Math.toRadians(lon2 - lon1);
//
//		final double a = (Math.sin(dLat / 2))
//				* (Math.sin(dLat / 2))
//				+ (Math.cos(lat1) * Math.cos(lat2) * (Math.sin(dLon / 2)))
//				* (Math.cos(lat1) * Math.cos(lat2) * (Math.sin(dLon / 2)));
//
//		final double c = 2 * Math.asin(Math.min(1.0, Math.sqrt(a)));
//		final double km = EARTH_RADIUS * c;
//
//		return km * 1000;
//	}

}
