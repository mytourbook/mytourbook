package net.tourbook.ext.srtm;

import java.io.File;

public class ElevationBase {

	final public GeoLat	bGrid;
	final public GeoLon	lGrid;
	final public GeoLat	bFirst;
	final public GeoLat	bLast;
	final public GeoLon	lFirst;
	final public GeoLon	lLast;

	public static void main(final String[] args) {}

	public ElevationBase() {
		bGrid = new GeoLat();
		lGrid = new GeoLon();
		bFirst = new GeoLat();
		bLast = new GeoLat();
		lFirst = new GeoLon();
		lLast = new GeoLon();
	}

	// dummy
	public short getElevation(final GeoLat lat, final GeoLon lon) {
		return 0;
	}

	// dummy
	public double getElevationDouble(final GeoLat lat, final GeoLon lon) {
		return 0.;
	}

	public short getElevationGrid(final GeoLat lat, final GeoLon lon) {
		return (short) getElevationGridDouble(lat, lon);
	}

	/**
	 * @param b
	 * @param l
	 * @return Returns {@link Short#MIN_VALUE} when the altitude is invalid, this can happen when
	 *         the altitude cannot be read from a file or the file cannot be retrieved from the SRTM
	 *         host.
	 */
	public double getElevationGridDouble(final GeoLat lat, final GeoLon lon) {

		short h1, h2, h3, h4;
		double p, q;
		short ok = 0;
		double hm;

		bFirst.toLeft(lat, bGrid);
		bLast.toRight(lat, bGrid);
		lFirst.toLeft(lon, lGrid);
		lLast.toRight(lon, lGrid);

		h1 = this.getElevation(bLast, lFirst);
		h2 = this.getElevation(bLast, lLast);
		h3 = this.getElevation(bFirst, lFirst);
		h4 = this.getElevation(bFirst, lLast);

		// ungueltige Werte ausgleichen
		final boolean is_valid_h1 = is_valid(h1);
		final boolean is_valid_h2 = is_valid(h2);
		final boolean is_valid_h3 = is_valid(h3);
		final boolean is_valid_h4 = is_valid(h4);

		ok = 0;
		if (is_valid_h1)
			ok++;
		if (is_valid_h2)
			ok++;
		if (is_valid_h3)
			ok++;
		if (is_valid_h4)
			ok++;
		if (ok != 4) {

			//FileLog.println(this, "ElevationBase: " + ok + " " + h1 + " " + h2 + " " + h3 + " " + h4 + " " + ec++);

			if (ok == 0) {
				hm = (h1 + h2 + h3 + h4) / 4;
//            return hm; 
				return Short.MIN_VALUE;
			}
			hm = 0;
			if (is_valid_h1)
				hm += h1;
			if (is_valid_h2)
				hm += h2;
			if (is_valid_h3)
				hm += h3;
			if (is_valid_h4)
				hm += h4;
			hm /= ok;
			if (!is_valid_h1)
				h1 = (short) hm;
			if (!is_valid_h2)
				h2 = (short) hm;
			if (!is_valid_h3)
				h3 = (short) hm;
			if (!is_valid_h4)
				h4 = (short) hm;
		}

		p = lat.getDezimal() - bFirst.getDezimal();
		p /= bLast.getDezimal() - bFirst.getDezimal();
		q = lon.getDezimal() - lFirst.getDezimal();
		q /= lLast.getDezimal() - lFirst.getDezimal();

		return ((1 - q) * p * h1 + q * p * h2 + (1 - q) * (1 - p) * h3 + q * (1 - p) * h4 + 0.5);
	}

	// dummy
	public String getName() {
		return "FILETYP-DUMMY";
	}

	// dummy
	public short getSekDiff() {
		// Anzahl Gradsekunden zwischen zwei Datenpunkten
		return 42;
	}

	public boolean is_valid(final double elev) {
		return is_valid((short) elev);
	}

	public boolean is_valid(final short elev) {
		if (elev >= -11000 && elev < 8850)
			return true;
		return false;
	}
	
	public String getElevationDataPath() {

		String elevationDataPath;

		final String prefDataPath = Activator.getDefault()
				.getPreferenceStore()
				.getString(IPreferences.SRTM_DATA_FILEPATH); // TODO umbenennen

		if (prefDataPath.length() == 0 || new File(prefDataPath).exists() == false) {
			elevationDataPath = (String) System.getProperties().get("user.home");
		} else {
			elevationDataPath = prefDataPath;
		}

		return elevationDataPath;
	}
	
}
