package net.tourbook.ext.srtm;

import java.io.File;
import java.util.HashMap;

import org.eclipse.jface.preference.IPreferenceStore;

public final class ElevationSRTM3 extends Elevation {

	static private SRTM3I							srtm3I;
	static final private HashMap<Integer, SRTM3I>	hm	= new HashMap<Integer, SRTM3I>();	// default initial 16 Files

	private class SRTM3I {

		FileShort	fileShort;

		private SRTM3I(final GeoLat b, final GeoLon l) {

			final String homeDir = getSRTMDataPath();
			final String srtm3Dir = homeDir + File.separator + "srtm3"; // Lokale Lage der SRTM3-Files!!!
			final String srtm3Suffix = ".hgt";

			String fileName = new String();
			fileName = srtm3Dir
					+ File.separator
					+ b.getRichtung()
					+ NumberForm.n2(b.isNorden() ? b.getGrad() : b.getGrad() + 1)
					+ l.getRichtung()
					+ NumberForm.n3(l.isOsten() ? l.getGrad() : l.getGrad() + 1)
					+ srtm3Suffix;

			try {
				fileShort = new FileShort(fileName, true); // ggf. unzip und/oder FTP
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		public short getElevation(final GeoLat b, final GeoLon l) {
			return fileShort.get(offset(b, l));
		}

		private String getSRTMDataPath() {

			final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
			final String dataPath = prefStore.getString(IPreferences.SRTM_DATA_FILEPATH);

			if (dataPath.length() == 0 || new File(dataPath).exists() == false) {

			}

			final Object userHomeDir = System.getProperties().get("user.home");
			return (String) userHomeDir;
		}

		//    Offset im SRTM3-File
		public int offset(final GeoLat b, final GeoLon l) {

			if (b.isSueden()) {
				if (l.isOsten()) {
					return 1201
							* (b.getMinuten() * 20 + b.getSekunden() / 3)
							+ l.getMinuten()
							* 20
							+ l.getSekunden()
							/ 3;
				} else {
					return 1201
							* (b.getMinuten() * 20 + b.getSekunden() / 3)
							+ 1199
							- l.getMinuten()
							* 20
							- l.getSekunden()
							/ 3;
				}
			} else {
				if (l.isOsten()) {
					return 1201
							* (1199 - b.getMinuten() * 20 - b.getSekunden() / 3)
							+ l.getMinuten()
							* 20
							+ l.getSekunden()
							/ 3;
				} else {
					return 1201
							* (1199 - b.getMinuten() * 20 - b.getSekunden() / 3)
							+ 1199
							- l.getMinuten()
							* 20
							- l.getSekunden()
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
	public short getElevation(final GeoLat b, final GeoLon l) {

		if (b.getTertia() != 0)
			return getElevationGrid(b, l);
		if (l.getTertia() != 0)
			return getElevationGrid(b, l);
		if (b.getSekunden() % 3 != 0)
			return getElevationGrid(b, l);
		if (l.getSekunden() % 3 != 0)
			return getElevationGrid(b, l);

		int i = l.getGrad();
		if (l.isWesten())
			i += 256;
		i *= 1024;
		i += b.getGrad();
		if (b.isSueden())
			i += 256;
		final Integer ii = new Integer(i);
		srtm3I = hm.get(ii);

		if (srtm3I == null) {
			// nur beim jeweils ersten Mal
			// FileLog.println(this, "Index ElevationSRTM3 " + ii);
			srtm3I = new SRTM3I(b, l);
			hm.put(ii, srtm3I);
		}

		return srtm3I.getElevation(b, l);

	}

	@Override
	public double getElevationDouble(final GeoLat b, final GeoLon l) {

		if (b.getDezimal() == 0 && l.getDezimal() == 0)
			return 0.;
		if (b.getTertia() != 0)
			return getElevationGridDouble(b, l);
		if (l.getTertia() != 0)
			return getElevationGridDouble(b, l);
		if (b.getSekunden() % 3 != 0)
			return getElevationGridDouble(b, l);
		if (l.getSekunden() % 3 != 0)
			return getElevationGridDouble(b, l);
		return getElevation(b, l);
	}

	@Override
	public String getName() {
		return "ElevationSRTM3";
	}

	@Override
	public short getSekDiff() {
		// Anzahl Gradsekunden zwischen zwei Datenpunkten
		return 3;
	}
}
