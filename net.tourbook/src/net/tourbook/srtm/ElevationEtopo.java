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
package net.tourbook.srtm;

import java.io.File;

public final class ElevationEtopo extends ElevationBase {

	static private EtopoI	fEtopoi	= null;

	private final class EtopoI {

		private GeoLat	minLat	= new GeoLat();
		private GeoLon	minLon	= new GeoLon();
		GeoLat			offLat	= new GeoLat();
		GeoLon			offLon	= new GeoLon();
		ElevationFile	elevationFile;

		private EtopoI() {

			final String etopoDataPath = getElevationDataPath("etopo"); //$NON-NLS-1$
			final String etopoFilename = "ETOPO5.DAT"; //$NON-NLS-1$
			final String fileName = new String(etopoDataPath + File.separator + etopoFilename);

			try {
				elevationFile = new ElevationFile(fileName, Constants.ELEVATION_TYPE_ETOPO);
			} catch (final Exception e) {
				System.out.println("EtopoI: Error: " + e.getMessage()); // NOT File not found //$NON-NLS-1$
				// dont return exception
			}

			minLon.setDegreesMinutesSecondsDirection(360, 0, 0, 'W');
			minLat.setDegreesMinutesSecondsDirection(89, 55, 0, 'N');
		}

		public short getElevation(final GeoLat lat, final GeoLon lon) {

			return elevationFile.get(offset(lat, lon));
		}

		// Offset in the Etopo-File
		public int offset(final GeoLat lat, final GeoLon lon) {

			offLat.sub(minLat, lat);
			offLon.sub(lon, minLon);
			return offLat.degrees * 51840 // 360*12*12       
					+ offLat.minutes
					* 864 // 360*12/5      
					+ offLon.degrees
					* 12
					+ offLon.minutes
					/ 5;
		}
	}

	public static void main(final String[] args) {}

	public ElevationEtopo() {
		gridLat.setDegreesMinutesSecondsDirection(0, 5, 0, 'N');
		gridLon.setDegreesMinutesSecondsDirection(0, 5, 0, 'E');
	}

	@Override
	public short getElevation(final GeoLat lat, final GeoLon lon) {

		if (lat.tertias != 0)
			return getElevationGrid(lat, lon);
		if (lon.tertias != 0)
			return getElevationGrid(lat, lon);
		if (lat.seconds != 0)
			return getElevationGrid(lat, lon);
		if (lon.seconds != 0)
			return getElevationGrid(lat, lon);
		if (lat.minutes % 5 != 0)
			return getElevationGrid(lat, lon);
		if (lon.minutes % 5 != 0)
			return getElevationGrid(lat, lon);

		if (fEtopoi == null)
			fEtopoi = new EtopoI(); // first time only !!

		return fEtopoi.getElevation(lat, lon);

	}

	@Override
	public double getElevationDouble(final GeoLat lat, final GeoLon lon) {

		if (lat.decimal == 0 && lon.decimal == 0)
			return 0.;
		if (lat.tertias != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.tertias != 0)
			return getElevationGridDouble(lat, lon);
		if (lat.seconds != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.seconds != 0)
			return getElevationGridDouble(lat, lon);
		if (lat.minutes % 5 != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.minutes % 5 != 0)
			return getElevationGridDouble(lat, lon);
		return getElevation(lat, lon);
	}

	@Override
	public String getName() {
		return "ETOPO"; //$NON-NLS-1$
	}

	@Override
	public short getSecDiff() {
		// number of degrees seconds between two data points
		return 300;
	}
}
