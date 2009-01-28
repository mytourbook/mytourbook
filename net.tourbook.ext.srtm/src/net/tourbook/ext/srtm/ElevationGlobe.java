package net.tourbook.ext.srtm;

import java.io.File;

// Minimum/Maximum values
// 
//           | W179:59:30 | W089:59:30 | E000:00:00 | E090:00:00 |
//           | W090:00:00 | W000:00:00 | E089:59:30 | E179:59:30 |
// ----------+------------+------------+------------+------------+
// N89:59:30 | a          | b          | c          | d          |
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

	public ElevationGlobe() {
		for (int i = 0; i < 16; i++)
			initialized[i] = false;
		gridLat.setGradeMinutesSecondsDirection(0, 0, 30, 'N');
		gridLon.setGradeMinutesSecondsDirection(0, 0, 30, 'E');
	}

	public short getElevation(GeoLat lat, GeoLon lon) {
		int i = 0;

		if (lat.getTertia() != 0)
			return getElevationGrid(lat, lon);
		if (lon.getTertia() != 0)
			return getElevationGrid(lat, lon);
		if (lat.getSeconds() % 30 != 0)
			return getElevationGrid(lat, lon);
		if (lon.getSeconds() % 30 != 0)
			return getElevationGrid(lat, lon);

		// calculate globe fileindex (a-p ~ 0-15)
		if (lat.isSouth()) {
			i += 8;
			if (lat.getGrade() >= 50)
				i += 4;
		} else if (lat.getGrade() < 50)
			i += 4;
		if (lon.isEast()) {
			i += 2;
			if (lon.getGrade() >= 90)
				i++;
		} else if (lon.getGrade() < 90)
			i++;

		if (initialized[i] == false) {
			initialized[i] = true;
			fGlobei[i] = new GlobeI(i); // first time only !!
		}

		return fGlobei[i].getElevation(lat, lon);
	}

	public double getElevationDouble(GeoLat lat, GeoLon lon) {

		if (lat.getDecimal() == 0 && lon.getDecimal() == 0)
			return 0.;
		if (lat.getTertia() != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.getTertia() != 0)
			return getElevationGridDouble(lat, lon);
		if (lat.getSeconds() % 30 != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.getSeconds() % 30 != 0)
			return getElevationGridDouble(lat, lon);
		return (double) getElevation(lat, lon);
	}

	public short getSecDiff() {
		// number of grade seconds between two data points
		return 30;
	}

	public String getName() {
		return "GLOBE";
	}

	private class GlobeI {
		private GeoLat	minLat	= new GeoLat();
		private GeoLon	minLon	= new GeoLon();
		GeoLat			offLat	= new GeoLat();
		GeoLon			offLon	= new GeoLon();
		ElevationFile	elevationFile;

		private GlobeI(int i) {

			final String globeDataPath = getElevationDataPath("globe");
			final String globeSuffix = "10g";
			char c = (char) ('a' + i);
			String fileName = new String(globeDataPath + File.separator + c + globeSuffix);

			try {
				elevationFile = new ElevationFile(fileName, Constants.ELEVATION_TYPE_GLOBE);
			} catch (Exception e) {
				System.out.println("GlobeI: Error: " + e.getMessage()); // NOT File not found
				// dont return exception
			}

			switch (i) {
			case 0:
			case 4:
			case 8:
			case 12:
				minLon.setGradeMinutesSecondsDirection(179, 59, 30, 'W');
				break;
			case 1:
			case 5:
			case 9:
			case 13:
				minLon.setGradeMinutesSecondsDirection(89, 59, 30, 'W');
				break;
			case 2:
			case 6:
			case 10:
			case 14:
				minLon.setGradeMinutesSecondsDirection(0, 0, 0, 'E');
				break;
			case 3:
			case 7:
			case 11:
			case 15:
				minLon.setGradeMinutesSecondsDirection(90, 0, 0, 'E');
				break;
			default:
				break;
			}
			switch (i) {
			case 0:
			case 1:
			case 2:
			case 3:
				minLat.setGradeMinutesSecondsDirection(89, 59, 30, 'N');
				break;
			case 4:
			case 5:
			case 6:
			case 7:
				minLat.setGradeMinutesSecondsDirection(49, 59, 30, 'N');
				break;
			case 8:
			case 9:
			case 10:
			case 11:
				minLat.setGradeMinutesSecondsDirection(0, 0, 0, 'S');
				break;
			case 12:
			case 13:
			case 14:
			case 15:
				minLat.setGradeMinutesSecondsDirection(50, 0, 0, 'S');
				break;
			default:
				break;
			}
		}

		/**
		 * Byte swap a single short value.
		 * 
		 * @param value
		 *            Value to byte swap.
		 * @return Byte swapped representation.
		 */
		private short swap(short value) {
			int lat1 = value & 0xff;
			int lat2 = (value >> 8) & 0xff;

			return (short) (lat1 << 8 | lat2 << 0);
		}

		public short getElevation(GeoLat lat, GeoLon lon) {

			short elev = elevationFile.get(offset(lat, lon));
			return swap(elev);
		}

		//    Offset in the Globe-File
		public int offset(GeoLat lat, GeoLon lon) {

			offLat.sub(minLat, lat);
			offLon.sub(minLon, lon);
			return offLat.getGrade() * 1296000 // 360*60*60
					+ offLat.getMinutes()
					* 21600 // 360*60
					+ offLat.getSeconds()
					* 360
					+ offLon.getGrade()
					* 120
					+ offLon.getMinutes()
					* 2
					+ offLon.getSeconds()
					/ 30;
		}

	}

	public static void main(String[] args) {}
}
