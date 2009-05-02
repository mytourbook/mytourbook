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

// Minimum/Maximum values
// 
//           | W179:59:30 | W089:59:30 | E000:00:00 | E090:00:00 |
//           | W090:00:00 | W000:00:00 | E089:59:30 | E179:59:30 |
// ----------+------------+------------+------------+------------+
// N89:59:30 | a          | c          | c          | d          |
// N50:00:00 |            |            |            |            |
// ----------+------------+------------+------------+------------+
// N49:59:30 | e          | f          | g          | h          |
// N00:00:00 |            |            |            |            |
// ----------+------------+------------+------------+------------+
// S00:00:00 | i          | j          | k          | l          |
// S49:59:30 |            |            |            |            |
// ----------+------------+------------+------------+------------+
// S50:00:00 | m          | n          | o          | p          |
// S89:59:30 |            |            |            |            |
// ----------+------------+------------+------------+------------+

public final class ElevationGlobe extends ElevationBase {

	final static private GlobeI		fGlobei[]		= new GlobeI[16];
	final static private boolean	initialized[]	= new boolean[16];

	private class GlobeI {
		private GeoLat	minLat	= new GeoLat();
		private GeoLon	minLon	= new GeoLon();
		GeoLat			offLat	= new GeoLat();
		GeoLon			offLon	= new GeoLon();
		ElevationFile	elevationFile;

		private GlobeI(final int i) {

			final String globeDataPath = getElevationDataPath("globe"); //$NON-NLS-1$
			final String globeSuffix = "10g"; //$NON-NLS-1$
			final char c = (char) ('a' + i);
			final String fileName = new String(globeDataPath + File.separator + c + globeSuffix);

			try {
				elevationFile = new ElevationFile(fileName, Constants.ELEVATION_TYPE_GLOBE);
			} catch (final Exception e) {
				System.out.println("GlobeI: Error: " + e.getMessage()); // NOT File not found //$NON-NLS-1$
				// dont return exception
			}

			switch (i) {
			case 0:
			case 4:
			case 8:
			case 12:
				minLon.setDegreesMinutesSecondsDirection(179, 59, 30, 'W');
				break;
			case 1:
			case 5:
			case 9:
			case 13:
				minLon.setDegreesMinutesSecondsDirection(89, 59, 30, 'W');
				break;
			case 2:
			case 6:
			case 10:
			case 14:
				minLon.setDegreesMinutesSecondsDirection(0, 0, 0, 'E');
				break;
			case 3:
			case 7:
			case 11:
			case 15:
				minLon.setDegreesMinutesSecondsDirection(90, 0, 0, 'E');
				break;
			default:
				break;
			}
			switch (i) {
			case 0:
			case 1:
			case 2:
			case 3:
				minLat.setDegreesMinutesSecondsDirection(89, 59, 30, 'N');
				break;
			case 4:
			case 5:
			case 6:
			case 7:
				minLat.setDegreesMinutesSecondsDirection(49, 59, 30, 'N');
				break;
			case 8:
			case 9:
			case 10:
			case 11:
				minLat.setDegreesMinutesSecondsDirection(0, 0, 0, 'S');
				break;
			case 12:
			case 13:
			case 14:
			case 15:
				minLat.setDegreesMinutesSecondsDirection(50, 0, 0, 'S');
				break;
			default:
				break;
			}
		}

		public short getElevation(final GeoLat lat, final GeoLon lon) {

			final short elev = elevationFile.get(offset(lat, lon));
			return swap(elev);
		}

		//    Offset in the Globe-File
		public int offset(final GeoLat lat, final GeoLon lon) {

			offLat.sub(minLat, lat);
			offLon.sub(minLon, lon);
			return offLat.degrees * 1296000 // 360*60*60
					+ offLat.minutes
					* 21600 // 360*60
					+ offLat.seconds
					* 360
					+ offLon.degrees
					* 120
					+ offLon.minutes
					* 2
					+ offLon.seconds
					/ 30;
		}

		/**
		 * Byte swap a single short value.
		 * 
		 * @param value
		 *            Value to byte swap.
		 * @return Byte swapped representation.
		 */
		private short swap(final short value) {
			final int lat1 = value & 0xff;
			final int lat2 = (value >> 8) & 0xff;

			return (short) (lat1 << 8 | lat2 << 0);
		}

	}

	public static void main(final String[] args) {}

	public ElevationGlobe() {
		for (int i = 0; i < 16; i++)
			initialized[i] = false;
		gridLat.setDegreesMinutesSecondsDirection(0, 0, 30, 'N');
		gridLon.setDegreesMinutesSecondsDirection(0, 0, 30, 'E');
	}

	@Override
	public short getElevation(final GeoLat lat, final GeoLon lon) {
		int i = 0;

		if (lat.tertias != 0)
			return getElevationGrid(lat, lon);
		if (lon.tertias != 0)
			return getElevationGrid(lat, lon);
		if (lat.seconds % 30 != 0)
			return getElevationGrid(lat, lon);
		if (lon.seconds % 30 != 0)
			return getElevationGrid(lat, lon);

		// calculate globe fileindex (a-p ~ 0-15)
		if (lat.direction == GeoLat.DIRECTION_SOUTH) {
			i += 8;
			if (lat.degrees >= 50)
				i += 4;
		} else if (lat.degrees < 50)
			i += 4;
		if (lon.direction == GeoLon.DIRECTION_EAST) {
			i += 2;
			if (lon.degrees >= 90)
				i++;
		} else if (lon.degrees < 90)
			i++;

		if (initialized[i] == false) {
			initialized[i] = true;
			fGlobei[i] = new GlobeI(i); // first time only !!
		}

		return fGlobei[i].getElevation(lat, lon);
	}

	@Override
	public double getElevationDouble(final GeoLat lat, final GeoLon lon) {

		if (lat.decimal == 0 && lon.decimal == 0)
			return 0.;
		if (lat.tertias != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.tertias != 0)
			return getElevationGridDouble(lat, lon);
		if (lat.seconds % 30 != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.seconds % 30 != 0)
			return getElevationGridDouble(lat, lon);
		
		return getElevation(lat, lon);
	}

	@Override
	public String getName() {
		return "GLOBE"; //$NON-NLS-1$
	}

	@Override
	public short getSecDiff() {
		// number of degrees seconds between two data points
		return 30;
	}
}
