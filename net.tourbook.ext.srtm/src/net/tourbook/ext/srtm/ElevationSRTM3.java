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
import java.util.HashMap;

public final class ElevationSRTM3 extends ElevationBase {

	static private SRTM3ElevationFile							srtm3I;

	static final private HashMap<Integer, SRTM3ElevationFile>	fileMap	= new HashMap<Integer, SRTM3ElevationFile>();	// default initial 16 Files

	private class SRTM3ElevationFile {

		ElevationFile	elevationFile;

		private SRTM3ElevationFile(final GeoLat lat, final GeoLon lon) {

			final String srtm3DataPath = getElevationDataPath("srtm3"); //$NON-NLS-1$
			final String srtm3Suffix = ".hgt"; //$NON-NLS-1$

			final String fileName = new String(srtm3DataPath
					+ File.separator
					+ lat.direction
					+ NumberForm.n2((lat.direction == GeoLat.DIRECTION_NORTH) ? lat.degrees : lat.degrees + 1)
					+ lon.direction
					+ NumberForm.n3((lon.direction == GeoLon.DIRECTION_EAST) ? lon.degrees : lon.degrees + 1)
					+ srtm3Suffix);

			try {
				elevationFile = new ElevationFile(fileName, Constants.ELEVATION_TYPE_SRTM3);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		public short getElevation(final GeoLat lat, final GeoLon lon) {
			return elevationFile.get(srtmFileOffset(lat, lon));
		}

		//    Offset in the SRTM3-File
		public int srtmFileOffset(final GeoLat lat, final GeoLon lon) {

			if (lat.direction == GeoLat.DIRECTION_SOUTH) {
				if (lon.direction == GeoLon.DIRECTION_EAST) {

					// SOUTH - EAST

					return 1201//
							* (lat.minutes * 20 + lat.seconds / 3)
							+ lon.minutes
							* 20
							+ lon.seconds
							/ 3;
				} else {

					// SOUTH - WEST

					return 1201//
							* (lat.minutes * 20 + lat.seconds / 3)
							+ 1199
							- lon.minutes
							* 20
							- lon.seconds
							/ 3;
				}
			} else {

				if (lon.direction == GeoLon.DIRECTION_EAST) {

					// NORTH -EAST

					return 1201//
							* (1199 - lat.minutes * 20 - lat.seconds / 3)
							+ lon.minutes
							* 20
							+ lon.seconds
							/ 3;
				} else {

					// NORTH - WEST

					return 1201
							* (1199 - lat.minutes * 20 - lat.seconds / 3)
							+ 1199
							- lon.minutes
							* 20
							- lon.seconds
							/ 3;
				}
			}
		}
//		public int srtmFileOffset(final GeoLat lat, final GeoLon lon) {
//			
//			if (lat.isSouth()) {
//				if (lon.isEast()) {
//					return 1201
//					* (lat.getMinutes() * 20 + lat.getSeconds() / 3)
//					+ lon.getMinutes()
//					* 20
//					+ lon.getSeconds()
//					/ 3;
//				} else {
//					return 1201
//					* (lat.getMinutes() * 20 + lat.getSeconds() / 3)
//					+ 1199
//					- lon.getMinutes()
//					* 20
//					- lon.getSeconds()
//					/ 3;
//				}
//			} else {
//				if (lon.isEast()) {
//					return 1201
//					* (1199 - lat.getMinutes() * 20 - lat.getSeconds() / 3)
//					+ lon.getMinutes()
//					* 20
//					+ lon.getSeconds()
//					/ 3;
//				} else {
//					return 1201
//					* (1199 - lat.getMinutes() * 20 - lat.getSeconds() / 3)
//					+ 1199
//					- lon.getMinutes()
//					* 20
//					- lon.getSeconds()
//					/ 3;
//				}
//			}
//		}
	}

	public ElevationSRTM3() {
		gridLat.setDegreesMinutesSecondsDirection(0, 0, 3, 'N');
		gridLon.setDegreesMinutesSecondsDirection(0, 0, 3, 'E');
	}

	@Override
	public short getElevation(final GeoLat lat, final GeoLon lon) {

		if (lat.tertias != 0)
			return getElevationGrid(lat, lon);
		if (lon.tertias != 0)
			return getElevationGrid(lat, lon);
		if (lat.seconds % 3 != 0)
			return getElevationGrid(lat, lon);
		if (lon.seconds % 3 != 0)
			return getElevationGrid(lat, lon);

		int i = lon.degrees;
		if (lon.direction == GeoLon.DIRECTION_WEST) {
			i += 256;
		}
		i *= 1024;
		i += lat.degrees;
		if (lat.direction == GeoLat.DIRECTION_SOUTH) {
			i += 256;
		}

		final Integer ii = new Integer(i);
		srtm3I = fileMap.get(ii);

		if (srtm3I == null) {
			// first time only
			srtm3I = new SRTM3ElevationFile(lat, lon);
			fileMap.put(ii, srtm3I);
		}

		return srtm3I.getElevation(lat, lon);

	}

	@Override
	public double getElevationDouble(final GeoLat lat, final GeoLon lon) {

		if (lat.decimal == 0 && lon.decimal == 0) {
			return 0.;
		}
		if (lat.tertias != 0) {
			return getElevationGridDouble(lat, lon);
		}
		if (lon.tertias != 0) {
			return getElevationGridDouble(lat, lon);
		}
		if (lat.seconds % 3 != 0) {
			return getElevationGridDouble(lat, lon);
		}
		if (lon.seconds % 3 != 0) {
			return getElevationGridDouble(lat, lon);
		}

		return getElevation(lat, lon);
	}

	@Override
	public String getName() {
		return "SRTM3"; //$NON-NLS-1$
	}

	@Override
	public short getSecDiff() {
		// number of degrees seconds between two data points
		return 3;
	}
}
