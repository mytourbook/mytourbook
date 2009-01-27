package net.tourbook.ext.srtm;

import java.io.File;
import java.util.HashMap;

public final class ElevationSRTM3 extends ElevationBase {

	static private SRTM3I							srtm3I;
	static final private HashMap<Integer, SRTM3I>	hm	= new HashMap<Integer, SRTM3I>();	// default initial 16 Files

	private class SRTM3I {

		ElevationFile	elevationFile;

		private SRTM3I(final GeoLat lat, final GeoLon lon) {

			final String elevationDataPath = getElevationDataPath();
			final String srtm3Dir = elevationDataPath + File.separator + "srtm3"; // Lokale Lage der SRTM3-Files!!!
			final String srtm3Suffix = ".hgt";

			String fileName = new String();
			fileName = srtm3Dir
					+ File.separator
					+ lat.getRichtung()
					+ NumberForm.n2(lat.isNorden() ? lat.getGrad() : lat.getGrad() + 1)
					+ lon.getRichtung()
					+ NumberForm.n3(lon.isOsten() ? lon.getGrad() : lon.getGrad() + 1)
					+ srtm3Suffix;

			try {
				elevationFile = new ElevationFile(fileName, Constants.ELEVATION_TYPE_SRTM3);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		public short getElevation(final GeoLat lat, final GeoLon lon) {
			return elevationFile.get(srtmFileOffset(lat, lon));
		}

		//    Offset im SRTM3-File
		public int srtmFileOffset(final GeoLat lat, final GeoLon lon) {

			if (lat.isSueden()) {
				if (lon.isOsten()) {
					return 1201
							* (lat.getMinuten() * 20 + lat.getSekunden() / 3)
							+ lon.getMinuten()
							* 20
							+ lon.getSekunden()
							/ 3;
				} else {
					return 1201
							* (lat.getMinuten() * 20 + lat.getSekunden() / 3)
							+ 1199
							- lon.getMinuten()
							* 20
							- lon.getSekunden()
							/ 3;
				}
			} else {
				if (lon.isOsten()) {
					return 1201
							* (1199 - lat.getMinuten() * 20 - lat.getSekunden() / 3)
							+ lon.getMinuten()
							* 20
							+ lon.getSekunden()
							/ 3;
				} else {
					return 1201
							* (1199 - lat.getMinuten() * 20 - lat.getSekunden() / 3)
							+ 1199
							- lon.getMinuten()
							* 20
							- lon.getSekunden()
							/ 3;
				}
			}
		}
	}

	public ElevationSRTM3() {
		// FileLog.println(this, "ElevationSRTM3 Konstructor!");

		// Map mit benutzten Files anlegen
		// um den File zu finden, wird eine Schluesselzahl berechnet und gemerkt
		// hm = new HashMap<Integer,SRTM3I>(); // default initial 16 Files
		bGrid.setGradMinutenSekundenRichtung(0, 0, 3, 'N');
		lGrid.setGradMinutenSekundenRichtung(0, 0, 3, 'E');
	}

	@Override
	public short getElevation(final GeoLat lat, final GeoLon lon) {

		if (lat.getTertia() != 0)
			return getElevationGrid(lat, lon);
		if (lon.getTertia() != 0)
			return getElevationGrid(lat, lon);
		if (lat.getSekunden() % 3 != 0)
			return getElevationGrid(lat, lon);
		if (lon.getSekunden() % 3 != 0)
			return getElevationGrid(lat, lon);

		int i = lon.getGrad();
		if (lon.isWesten())
			i += 256;
		i *= 1024;
		i += lat.getGrad();
		if (lat.isSueden())
			i += 256;
		final Integer ii = new Integer(i);
		srtm3I = hm.get(ii);

		if (srtm3I == null) {
			// nur beim jeweils ersten Mal
			// FileLog.println(this, "Index ElevationSRTM3 " + ii);
			srtm3I = new SRTM3I(lat, lon);
			hm.put(ii, srtm3I);
		}

		return srtm3I.getElevation(lat, lon);

	}

	@Override
	public double getElevationDouble(final GeoLat lat, final GeoLon lon) {

		if (lat.getDezimal() == 0 && lon.getDezimal() == 0)
			return 0.;
		if (lat.getTertia() != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.getTertia() != 0)
			return getElevationGridDouble(lat, lon);
		if (lat.getSekunden() % 3 != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.getSekunden() % 3 != 0)
			return getElevationGridDouble(lat, lon);
		return getElevation(lat, lon);
	}

	@Override
	public String getName() {
		return "SRTM3";
	}

	@Override
	public short getSekDiff() {
		// Anzahl Gradsekunden zwischen zwei Datenpunkten
		return 3;
	}
}
