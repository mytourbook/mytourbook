package net.tourbook.ext.srtm;

import java.io.File;

public final class ElevationEtopo extends ElevationBase {

	static private EtopoI	fEtopoi	= null;

	public ElevationEtopo() {
		gridLat.setGradeMinutesSecondsDirection(0, 5, 0, 'N');
		gridLon.setGradeMinutesSecondsDirection(0, 5, 0, 'E');
	}

	public short getElevation(GeoLat lat, GeoLon lon) {

		if (lat.getTertia() != 0)
			return getElevationGrid(lat, lon);
		if (lon.getTertia() != 0)
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
		if (lat.getTertia() != 0)
			return getElevationGridDouble(lat, lon);
		if (lon.getTertia() != 0)
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
		// number of grade seconds between two data points
		return 300;
	}

	public String getName() {
		return "ETOPO";
	}

	private final class EtopoI {

		private GeoLat	minLat	= new GeoLat();
		private GeoLon	minLon	= new GeoLon();
		GeoLat			offLat	= new GeoLat();
		GeoLon			offLon	= new GeoLon();
		ElevationFile	elevationFile;

		private EtopoI() {

			final String etopoDataPath = getElevationDataPath("etopo");
			final String etopoFilename = "ETOPO5.DAT";
			String fileName = new String(etopoDataPath + File.separator + etopoFilename);

			try {
				elevationFile = new ElevationFile(fileName, Constants.ELEVATION_TYPE_ETOPO);
			} catch (Exception e) {
				System.out.println("EtopoI: Error: " + e.getMessage()); // NOT File not found
				// dont return exception
			}

			minLon.setGradeMinutesSecondsDirection(360, 0, 0, 'W');
			minLat.setGradeMinutesSecondsDirection(89, 55, 0, 'N');
		}

		public short getElevation(GeoLat lat, GeoLon lon) {

			return elevationFile.get(offset(lat, lon));
		}

		// Offset in the Etopo-File
		public int offset(GeoLat lat, GeoLon lon) {

			offLat.sub(minLat, lat);
			offLon.sub(lon, minLon);
			return offLat.getGrade() * 51840 // 360*12*12       
					+ offLat.getMinutes()
					* 864 // 360*12/5      
					+ offLon.getGrade()
					* 12
					+ offLon.getMinutes()
					/ 5;
		}
	}

	public static void main(String[] args) {}
}
