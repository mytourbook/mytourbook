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

import java.io.File;

public final class ElevationEtopo extends ElevationBase {

	static private EtopoI	fEtopoi	= null;

	public ElevationEtopo() {
		gridLat.setDegreesMinutesSecondsDirection(0, 5, 0, 'N');
		gridLon.setDegreesMinutesSecondsDirection(0, 5, 0, 'E');
	}

	public short getElevation(GeoLat lat, GeoLon lon) {

		if (lat.getTertias() != 0)
			return getElevationGrid(lat, lon);
		if (lon.getTertias() != 0)
			return getElevationGrid(lat, lon);
		if (lat.getSeconds() != 0)
			return getElevationGrid(lat, lon);
		if (lon.getSeconds() != 0)
			return getElevationGrid(lat, lon);
		if (lat.getMinutes() % 5 != 0)
			return getElevationGrid(lat, lon);
		if (lon.getMinutes() % 5 != 0)
			return getElevationGrid(lat, lon);

		if (fEtopoi == null)
			fEtopoi = new EtopoI(); // first time only !!

		return fEtopoi.getElevation(lat, lon);

	}

	public double getElevationDouble(GeoLat lat, GeoLon lon) {

		if (lat.getDecimal() == 0 && lon.getDecimal() == 0)
			return 0.;
		if (lat.getTertias() != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.getTertias() != 0)
			return getElevationGridDouble(lat, lon);
		if (lat.getSeconds() != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.getSeconds() != 0)
			return getElevationGridDouble(lat, lon);
		if (lat.getMinutes() % 5 != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.getMinutes() % 5 != 0)
			return getElevationGridDouble(lat, lon);
		return (double) getElevation(lat, lon);
	}

	public short getSecDiff() {
		// number of degrees seconds between two data points
		return 300;
	}

	public String getName() {
		return "ETOPO"; //$NON-NLS-1$
	}

	private final class EtopoI {

		private GeoLat	minLat	= new GeoLat();
		private GeoLon	minLon	= new GeoLon();
		GeoLat			offLat	= new GeoLat();
		GeoLon			offLon	= new GeoLon();
		ElevationFile	elevationFile;

		private EtopoI() {

			final String etopoDataPath = getElevationDataPath("etopo"); //$NON-NLS-1$
			final String etopoFilename = "ETOPO5.DAT"; //$NON-NLS-1$
			String fileName = new String(etopoDataPath + File.separator + etopoFilename);

			try {
				elevationFile = new ElevationFile(fileName, Constants.ELEVATION_TYPE_ETOPO);
			} catch (Exception e) {
				System.out.println("EtopoI: Error: " + e.getMessage()); // NOT File not found //$NON-NLS-1$
				// dont return exception
			}

			minLon.setDegreesMinutesSecondsDirection(360, 0, 0, 'W');
			minLat.setDegreesMinutesSecondsDirection(89, 55, 0, 'N');
		}

		public short getElevation(GeoLat lat, GeoLon lon) {

			return elevationFile.get(offset(lat, lon));
		}

		// Offset in the Etopo-File
		public int offset(GeoLat lat, GeoLon lon) {

			offLat.sub(minLat, lat);
			offLon.sub(lon, minLon);
			return offLat.getDegrees() * 51840 // 360*12*12       
					+ offLat.getMinutes()
					* 864 // 360*12/5      
					+ offLon.getDegrees()
					* 12
					+ offLon.getMinutes()
					/ 5;
		}
	}

	public static void main(String[] args) {}
}
