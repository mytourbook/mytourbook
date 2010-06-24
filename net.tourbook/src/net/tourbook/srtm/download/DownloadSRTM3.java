/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.srtm.download;

import java.io.File;
import java.io.FileNotFoundException;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.srtm.GeoLat;
import net.tourbook.srtm.GeoLon;
import net.tourbook.srtm.IPreferences;
import net.tourbook.srtm.PrefPageSRTM;

import org.eclipse.jface.preference.IPreferenceStore;

public class DownloadSRTM3 {

	public static final String		URL_SEPARATOR		= "/"; //$NON-NLS-1$

	private final static int		DirEurasia			= 0;
	private final static int		DirNorth_America	= 1;
	private final static int		DirSouth_America	= 2;
	private final static int		DirAfrica			= 3;
	private final static int		DirAustralia		= 4;
	private final static int		DirIslands			= 5;

	private static String[]			dirs				= { "/SRTM3/Eurasia", // 0 //$NON-NLS-1$
			"/SRTM3/North_America", //	1 //$NON-NLS-1$
			"/SRTM3/South_America", //	2 //$NON-NLS-1$
			"/SRTM3/Africa", //			3 //$NON-NLS-1$
			"/SRTM3/Australia", //		4 //$NON-NLS-1$
			"/SRTM3/Islands" //			5 //$NON-NLS-1$
														};

	private static FTPDownloader	fFtpDownloader		= null;

	/**
	 * @param remoteFileName
	 * @param localZipName
	 * @throws Exception
	 */
	public static void get(final String remoteFileName, final String localZipName) throws Exception {

		final String remoteFilePath = getDir(remoteFileName);

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		final boolean isFtp = prefStore.getBoolean(IPreferences.STATE_IS_SRTM3_FTP);
		if (isFtp) {

			// download from FTP server

			if (fFtpDownloader == null) {
				fFtpDownloader = new FTPDownloader("anonymous", UI.EMPTY_STRING);//$NON-NLS-1$
			}

			// set ftp host from url which contains the protocol ftp://
			String ftpUrl = prefStore.getString(IPreferences.STATE_SRTM3_FTP_URL);
			ftpUrl = ftpUrl.substring(PrefPageSRTM.PROTOCOL_FTP.length());
			fFtpDownloader.setHost(ftpUrl);

			fFtpDownloader.get(remoteFilePath, remoteFileName, localZipName);

		} else {

			// download from HTTP server

			String baseUrl = prefStore.getString(IPreferences.STATE_SRTM3_HTTP_URL);

			// remove separator at the end
			if (baseUrl.endsWith(URL_SEPARATOR)) {
				baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
			}

			final String url = new StringBuilder().append(baseUrl)
					.append(remoteFilePath)
					.append(URL_SEPARATOR)
					.toString();

			HTTPDownloader.get(url, remoteFileName, localZipName);
		}
	}

	/**
	 * @param pathName
	 * @return
	 * @throws Exception
	 */
	private static String getDir(final String pathName) throws Exception {

		final String fileName = pathName.substring(pathName.lastIndexOf(File.separator) + 1);
		final String latString = fileName.substring(0, 3) + ":00"; // e.g. N50 //$NON-NLS-1$
		final String lonString = fileName.substring(3, 7) + ":00"; // e.g. E006 //$NON-NLS-1$
		final GeoLat lat = new GeoLat(latString);
		final GeoLon lon = new GeoLon(lonString);

		if (lat.greaterThen(new GeoLat("N60:00"))) {
			throw (new FileNotFoundException());
		}
		if (lat.lessThen(new GeoLat("S56:00"))) {
			throw (new FileNotFoundException());
		}

		// order important!
		// compare map ftp://e0srp01u.ecs.nasa.gov/srtm/version2/Documentation/Continent_def.gif
		if (isIn(lat, lon, "S56", "S28", "E165", "E179")) {
			return dirs[DirIslands];
		}
		if (isIn(lat, lon, "S56", "S55", "E158", "E159")) {
			return dirs[DirIslands];
		}
		if (isIn(lat, lon, "N15", "N30", "W180", "W155")) {
			return dirs[DirIslands];
		}
		if (isIn(lat, lon, "S44", "S05", "W030", "W006")) {
			return dirs[DirIslands];
		}
		if (isIn(lat, lon, "S56", "S45", "W039", "E060")) {
			return dirs[DirIslands];
		}
		if (isIn(lat, lon, "N35", "N39", "W040", "W020")) {
			return dirs[DirAfrica];
		}
		if (isIn(lat, lon, "S20", "S20", "E063", "E063")) {
			return dirs[DirAfrica];
		}
		if (isIn(lat, lon, "N10", "N10", "W110", "W110")) {
			return dirs[DirNorth_America];
		}
		if (isIn(lat, lon, "S10", "N14", "W180", "W139")) {
			return dirs[DirEurasia];
		}
		if (isIn(lat, lon, "S13", "S11", "E096", "E105")) {
			return dirs[DirEurasia];
		}
		if (isIn(lat, lon, "S44", "S11", "E112", "E179")) {
			return dirs[DirAustralia];
		}
		if (isIn(lat, lon, "S28", "S11", "W180", "W106")) {
			return dirs[DirAustralia];
		}
		if (isIn(lat, lon, "S35", "N34", "W030", "E059")) {
			return dirs[DirAfrica];
		}
		if (isIn(lat, lon, "N35", "N60", "W011", "E059")) {
			return dirs[DirEurasia];
		}
		if (isIn(lat, lon, "S10", "N60", "E060", "E179")) {
			return dirs[DirEurasia];
		}
		if (isIn(lat, lon, "N15", "N60", "W180", "W043")) {
			return dirs[DirNorth_America];
		}
		if (isIn(lat, lon, "S56", "N14", "W093", "W033")) {
			return dirs[DirSouth_America];
		}

		return dirs[DirIslands];
	}

	private static boolean isIn(final GeoLat lat,
								final GeoLon lon,
								final String latMin,
								final String latMax,
								final String lonMin,
								final String lonMax) {

		if (lat.lessThen(new GeoLat(latMin + ":00"))) {
			return false;
		}
		if (lat.greaterThen(new GeoLat(latMax + ":00"))) {
			return false;
		}
		if (lon.lessThen(new GeoLon(lonMin + ":00"))) {
			return false;
		}
		if (lon.greaterThen(new GeoLon(lonMax + ":00"))) {
			return false;
		}
		return true;
	}
}
