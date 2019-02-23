/* *****************************************************************************
 *  Copyright (C) 2008 Joshua Marinacci, Michael Kanis and others
 *
 *  This file is part of Geoclipse.
 *
 *  Geoclipse is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Geoclipse is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Geoclipse.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package de.byteholder.geoclipse.map;

import de.byteholder.geoclipse.mapprovider.MP;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import net.tourbook.common.map.GeoPosition;

/**
 * A utility class of methods that help when dealing with standard Mercator projections.
 *
 * @author Joshua Marinacci joshua.marinacci@sun.com
 * @author Michael Kanis
 */
public final class Mercator extends Projection {

   private static final String PROJECTION_ID = "merca"; //$NON-NLS-1$

//	// WGS-84 Ellipsoid
//	private static final double	HALBACHSE_A		= 6378.137;
//	private static final double	HALBACHSE_B		= 6356.7523142;
//
//	// = 1/298.2572229328709613   1/298.257223563 // ca. (A-B)/A
//	private static final double	ABPLATTUNG_F	= (HALBACHSE_A - HALBACHSE_B) / HALBACHSE_A;

   //http://www.kowoma.de/gps/geo/mapdatum/mapdatums.php
   //EUROPEAN 1950/1979, Western Europe
   //static final double HALBACHSE_A = 6378.388;
   //static final double HALBACHSE_B = 6356.911946;
   //static final double ABPLATTUNG_F = 1/297....;

//	private GeoLat				fLat1			= new GeoLat();
//	private GeoLon				fLong1			= new GeoLon();
//	private GeoLat				fLat2			= new GeoLat();
//	private GeoLon				fLong2			= new GeoLon();

   public static int latToY(final double latitudeDegrees, final double radius) {
      final double latitude = Math.toRadians(latitudeDegrees);
      final double y = radius / 2.0 * Math.log((1.0 + Math.sin(latitude)) / (1.0 - Math.sin(latitude)));
      return (int) y;
   }

   public static int longToX(final double longitudeDegrees, final double radius) {
      final double longitude = Math.toRadians(longitudeDegrees);
      return (int) (radius * longitude);
   }

   public static double xToLong(final int x, final double radius) {
      final double longRadians = x / radius;
      double longDegrees = Math.toDegrees(longRadians);

      // Because the world map does not start with 0 degrees but with -180
      longDegrees -= 180;

      /*
       * The user could have panned around the world a lot of times. Lat long goes from -180 to 180.
       * So every time a user gets to 181 we want to subtract 360 degrees. Every time a user gets to
       * -181 we want to add 360 degrees.
       */

      final double longitude = longDegrees/* - (rotations 360) */;
      return longitude;
   }

   public static double yToLat(final int y, final double radius) {
      final double latitude = (Math.PI / 2) - (2 * Math.atan(Math.exp(-1.0 * y / radius)));
      return Math.toDegrees(latitude);
   }

   //geodesic distanceString (in km) using Vincenty inverse formula for ellipsoids
   //Source: http://www.movable-type.co.uk/scripts/LatLongVincenty.html
//	private double distance(final GeoLat b1, final GeoLon l1, final GeoLat b2, final GeoLon l2) {
//
//		if (isNull(b1, l1, b2, l2)) {
//			return 0.;
//		}
//
//		final double L = l2.toRadians() - l1.toRadians();
//		final double U1 = Math.atan((1 - ABPLATTUNG_F) * b1.tan());
//		final double U2 = Math.atan((1 - ABPLATTUNG_F) * b2.tan());
//		final double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
//		final double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);
//
//		double lambda = L, lambdaP = 2 * Math.PI;
//		int iterLimit = 20;
//		double cosSqAlpha = 0, sinSigma = 0, cos2SigmaM = 0, cosSigma = 0, sigma = 0;
//
//		while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0) {
//			final double sinLambda = Math.sin(lambda), cosLambda = Math.cos(lambda);
//			sinSigma = Math.sqrt((cosU2 * sinLambda)
//					* (cosU2 * sinLambda)
//					+ (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
//					* (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
//			if (sinSigma == 0)
//				return 0; // co-incident points
//			cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
//			sigma = Math.atan2(sinSigma, cosSigma);
//			final double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
//			cosSqAlpha = 1 - sinAlpha * sinAlpha;
//			if (cosSqAlpha == 0)
//				return Math.abs(HALBACHSE_A * L); // two points on equator  // neu 20.04.2006
//			cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha; // nach Erg. auf Website
//			// cos2SigmaM = cosSigma;
//			// if (cosSqAlpha != 0) cos2SigmaM -= 2*sinU1*sinU2/cosSqAlpha; // Abfrage auf 0 neu 27.02.2005
//			final double C = ABPLATTUNG_F / 16 * cosSqAlpha * (4 + ABPLATTUNG_F * (4 - 3 * cosSqAlpha));
//			lambdaP = lambda;
//			lambda = L
//					+ (1 - C)
//					* ABPLATTUNG_F
//					* sinAlpha
//					* (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
//		}
//
//		if (iterLimit == 0) {
//			return 0; // formula failed to converge
//		}
//
//		final double uSq = cosSqAlpha
//				* (HALBACHSE_A * HALBACHSE_A - HALBACHSE_B * HALBACHSE_B)
//				/ (HALBACHSE_B * HALBACHSE_B);
//		final double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
//		final double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
//		final double deltaSigma = B
//				* sinSigma
//				* (cos2SigmaM + B
//						/ 4
//						* (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B
//								/ 6
//								* cos2SigmaM
//								* (-3 + 4 * sinSigma * sinSigma)
//								* (-3 + 4 * cos2SigmaM * cos2SigmaM)));
//		final double s = HALBACHSE_B * A * (sigma - deltaSigma);
//
//		return s;
//	}

   @Override
   public Point geoToPixel(GeoPosition geoPosition, final int zoomLevel, final MP mp) {

      if (geoPosition == null) {
         geoPosition = new GeoPosition(0.0, 0.0);
      }

      final double latitude = geoPosition.latitude;
      final double longitude = geoPosition.longitude;

//		if (latitude < -85.0) {
//			latitude = -73.333333;
//		}
//		if (latitude > 85.0) {
//			latitude = 73.333333;
//		}

      final Point2D devMapCenter = mp.getMapCenterInPixelsAtZoom(zoomLevel);

      final double x = devMapCenter.getX() + (longitude * mp.getLongitudeDegreeWidthInPixels(zoomLevel));

      double e = Math.sin(latitude * (Math.PI / 180.0));

      if (e > 0.9999) {
         e = 0.9999;
      }
      if (e < -0.9999) {
         e = -0.9999;
      }

      final double y = devMapCenter.getY()
            + 0.5
                  * Math.log((1 + e) / (1 - e))
                  * -1
                  * mp.getLongitudeRadianWidthInPixels(zoomLevel);

//		System.out.println(""//
//				+ " mapsize: "
//				+ (devMapCenter.getX() * 2)
//				+ "\tzoom:"
//				+ zoomLevel
//				+ "\t\tlat:"
//				+ latitude
//				+ "\tlon:"
//				+ longitude
//				+ "\t\tx:"
//				+ (int) x
//				+ "\ty:"
//				+ (int) y
//		//
//		);

      return new Point((int) x, (int) y);
   }

   @Override
   public Point2D.Double geoToPixelDouble(final GeoPosition geoPosition, final int zoomLevel, final MP mp) {

      final double latitude = geoPosition.latitude;
      final double longitude = geoPosition.longitude;

      final Point2D mapCenterInPixels = mp.getMapCenterInPixelsAtZoom(zoomLevel);

      final double x = mapCenterInPixels.getX() + (longitude * mp.getLongitudeDegreeWidthInPixels(zoomLevel));

      double e = Math.sin(latitude * (Math.PI / 180.0));
      if (e > 0.9999) {
         e = 0.9999;
      }
      if (e < -0.9999) {
         e = -0.9999;
      }

      final double y = mapCenterInPixels.getY()
            + 0.5
                  * Math.log((1 + e) / (1 - e))
                  * -1
                  * mp.getLongitudeRadianWidthInPixels(zoomLevel);

      return new Point2D.Double(x, y);
   }

//	@Override
//	public int getDistance(final GeoPosition position1,
//										final GeoPosition position2,
//										final int zoom,
//										final TileFactoryInfo info) {
//
//		fLat1.set(position1.getLatitude());
//		fLong1.set(position1.getLongitude());
//		fLat2.set(position2.getLatitude());
//		fLong2.set(position2.getLongitude());
//
//		final double distanceKM = distance(fLat1, fLong1, fLat2, fLong2);
//
////		final GeoLat geoLat = new GeoLat(latitude);
////		final double cosGeoLat = geoLat.cos(); // siehe da, da werden die trigon. Fkt. gebraucht
////		final int widthOfOneTileInKm = (int) (ERDRADIUS * 2 * Math.PI / Math.pow(2, zoom) * cosGeoLat);
//		// (Auf Zoom-Level z gibt es zwei hoch z Tiles in einer Richtung)
//
//		return (int) distanceKM;
//	}

   @Override
   public double getHorizontalDistance(final GeoPosition position1,
                                       final GeoPosition position2,
                                       final int zoom,
                                       final MP mp) {

      final Double devPos1 = geoToPixelDouble(position1, zoom, mp);
      final Double devPos2 = geoToPixelDouble(position2, zoom, mp);

      final double devXDiff = devPos1.y - devPos2.y;

      return devXDiff;
   }

   @Override
   public String getId() {
      return PROJECTION_ID;
   }

//	private boolean isNull(final GeoLat b1, final GeoLon l1, final GeoLat b2, final GeoLon l2) {
//
//		// ist eine der beiden Koordinaten (oder beide) der Nullpunkt?
//		if (b1.decimal == 0 && l1.decimal == 0) {
//			return true;
//		}
//
//		if (b2.decimal == 0 && l2.decimal == 0) {
//			return true;
//		}
//
//		return false;
//	}

   @Override
   public GeoPosition pixelToGeo(final Point2D pixelCoordinate, final int zoom, final MP mp) {

      // this reverses geoToPixel

      final double pixelX = pixelCoordinate.getX();
      final double pixelY = pixelCoordinate.getY();

      final Point2D devMapCenter = mp.getMapCenterInPixelsAtZoom(zoom);

      final double lon = (pixelX - devMapCenter.getX()) / mp.getLongitudeDegreeWidthInPixels(zoom);

      final double e1 = (pixelY - devMapCenter.getY()) / (-1 * mp.getLongitudeRadianWidthInPixels(zoom));
      final double e2 = (2 * Math.atan(Math.exp(e1)) - Math.PI / 2) / (Math.PI / 180.0);
      final double lat = e2;

      final GeoPosition wc = new GeoPosition(lat, lon);

      return wc;
   }

   public GeoPosition pixelToGeoTEST(final Point2D pixelCoordinate, final int zoom, final MP mp) {

      // this reverses geoToPixel

      final double devX = pixelCoordinate.getX();
      final double devY = pixelCoordinate.getY();
      final Point2D devMapCenter = mp.getMapCenterInPixelsAtZoom(zoom);

//		function y2lat(a) { return 180/Math.PI * (2 * Math.atan(Math.exp(a*Math.PI/180)) - Math.PI/2); }

      final double a = 0;
      final double lat = 180 / Math.PI * (2 * Math.atan(Math.exp(a * Math.PI / 180)) - Math.PI / 2);

      final double longitude = (devX - devMapCenter.getX()) / mp.getLongitudeDegreeWidthInPixels(zoom);

      final double e1 = (devY - devMapCenter.getY()) / (-1 * mp.getLongitudeRadianWidthInPixels(zoom));

      final double latitude = (2 * Math.atan(Math.exp(e1)) - Math.PI / 2) / (Math.PI / 180.0);

      final GeoPosition wc = new GeoPosition(latitude, longitude);

      return wc;
   }
}
