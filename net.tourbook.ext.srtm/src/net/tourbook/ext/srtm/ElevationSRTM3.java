package net.tourbook.ext.srtm;

import java.io.File;
import java.util.HashMap;

public final class ElevationSRTM3 extends ElevationBase {

	static private SRTM3I							srtm3I;
	static final private HashMap<Integer, SRTM3I>	hm	= new HashMap<Integer, SRTM3I>();	// default initial 16 Files

	private class SRTM3I {

		ElevationFile	elevationFile;

		private SRTM3I(final GeoLat lat, final GeoLon lon) {

			final String srtm3DataPath = getElevationDataPath("srtm3");
			final String srtm3Suffix = ".hgt";

			String fileName = new String(srtm3DataPath
					+ File.separator
					+ lat.getDirection()
					+ NumberForm.n2(lat.isNorth() ? lat.getDegrees() : lat.getDegrees() + 1)
					+ lon.getDirection()
					+ NumberForm.n3(lon.isEast() ? lon.getDegrees() : lon.getDegrees() + 1)
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

			if (lat.isSouth()) {
				if (lon.isEast()) {
					return 1201
							* (lat.getMinutes() * 20 + lat.getSeconds() / 3)
							+ lon.getMinutes()
							* 20
							+ lon.getSeconds()
							/ 3;
				} else {
					return 1201
							* (lat.getMinutes() * 20 + lat.getSeconds() / 3)
							+ 1199
							- lon.getMinutes()
							* 20
							- lon.getSeconds()
							/ 3;
				}
			} else {
				if (lon.isEast()) {
					return 1201
							* (1199 - lat.getMinutes() * 20 - lat.getSeconds() / 3)
							+ lon.getMinutes()
							* 20
							+ lon.getSeconds()
							/ 3;
				} else {
					return 1201
							* (1199 - lat.getMinutes() * 20 - lat.getSeconds() / 3)
							+ 1199
							- lon.getMinutes()
							* 20
							- lon.getSeconds()
							/ 3;
				}
			}
		}
	}

	public ElevationSRTM3() {
		gridLat.setDegreesMinutesSecondsDirection(0, 0, 3, 'N');
		gridLon.setDegreesMinutesSecondsDirection(0, 0, 3, 'E');
	}

	@Override
	public short getElevation(final GeoLat lat, final GeoLon lon) {

		if (lat.getTertias() != 0)
			return getElevationGrid(lat, lon);
		if (lon.getTertias() != 0)
			return getElevationGrid(lat, lon);
		if (lat.getSeconds() % 3 != 0)
			return getElevationGrid(lat, lon);
		if (lon.getSeconds() % 3 != 0)
			return getElevationGrid(lat, lon);

		int i = lon.getDegrees();
		if (lon.isWest())
			i += 256;
		i *= 1024;
		i += lat.getDegrees();
		if (lat.isSouth())
			i += 256;
		final Integer ii = new Integer(i);
		srtm3I = hm.get(ii);

		if (srtm3I == null) {
			// first time only
			srtm3I = new SRTM3I(lat, lon);
			hm.put(ii, srtm3I);
		}

		return srtm3I.getElevation(lat, lon);

	}

	@Override
	public double getElevationDouble(final GeoLat lat, final GeoLon lon) {

		if (lat.getDecimal() == 0 && lon.getDecimal() == 0)
			return 0.;
		if (lat.getTertias() != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.getTertias() != 0)
			return getElevationGridDouble(lat, lon);
		if (lat.getSeconds() % 3 != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.getSeconds() % 3 != 0)
			return getElevationGridDouble(lat, lon);
		return getElevation(lat, lon);
	}

	@Override
	public String getName() {
		return "SRTM3";
	}

	@Override
	public short getSecDiff() {
		// number of degrees seconds between two data points
		return 3;
	}
}
