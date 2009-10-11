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
package net.tourbook.ext.srtm.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.UnknownHostException;

import net.tourbook.ext.srtm.GeoLat;
import net.tourbook.ext.srtm.GeoLon;
import net.tourbook.ext.srtm.Messages;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPMessageCollector;
import com.enterprisedt.net.ftp.FTPProgressMonitor;
import com.enterprisedt.net.ftp.FTPTransferType;

import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.tileinfo.TileInfoManager;

public final class DownloadSRTM3OLD {

	final static String	host				= "e0srp01u.ecs.nasa.gov";	//$NON-NLS-1$
	final static String	user				= "anonymous";				//$NON-NLS-1$
	final static String	password			= "";						//$NON-NLS-1$

	final static int	DirEurasia			= 0;
	final static int	DirNorth_America	= 1;
	final static int	DirSouth_America	= 2;
	final static int	DirAfrica			= 3;
	final static int	DirAustralia		= 4;
	final static int	DirIslands			= 5;

	static String[]		dirs				= { "/srtm/version2/SRTM3/Eurasia", // 0 //$NON-NLS-1$
			"/srtm/version2/SRTM3/North_America", // 1 //$NON-NLS-1$
			"/srtm/version2/SRTM3/South_America", // 2 //$NON-NLS-1$
			"/srtm/version2/SRTM3/Africa", // 3 //$NON-NLS-1$
			"/srtm/version2/SRTM3/Australia", // 4 //$NON-NLS-1$
			"/srtm/version2/SRTM3/Islands"	};							// 5 //$NON-NLS-1$

	public final static void get(final String remoteName, final String localName) {

		final FTPClient ftp = new FTPClient();
		final FTPMessageCollector listener = new FTPMessageCollector();

		final String remoteDirName[] = new String[1];

		try {
			remoteDirName[0] = getDir(remoteName);
			final String localDirName = localName.substring(0, localName.lastIndexOf(File.separator));
			System.out.println("ftp:"); //$NON-NLS-1$
			System.out.println("   remoteDir " + remoteDirName[0]); //$NON-NLS-1$
			System.out.println("   localDir " + localDirName); //$NON-NLS-1$

			final File localDir = new File(localDirName);
			if (!localDir.exists()) {
				System.out.println("   create Dir " + localDirName); //$NON-NLS-1$
				localDir.mkdir();
			}

			// get connection to the host before the progress monitor is started
			ftp.setRemoteHost(host);
			ftp.setMessageListener(listener);

		} catch (final UnknownHostException e) {

			showConnectError();

			return;

		} catch (final Exception e) {
			e.printStackTrace();
		}

		final TileInfoManager tileInfoMgr = TileInfoManager.getInstance();

		final Job downloadJob = new Job("downloadJob") { //$NON-NLS-1$
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {

					showTileInfo(remoteName, -1);
					System.out.println("   connect " + host); //$NON-NLS-1$
					ftp.connect();

					showTileInfo(remoteName, -2);
					System.out.println("   login " + user + " " + password); //$NON-NLS-1$ //$NON-NLS-2$
					ftp.login(user, password);

					System.out.println("   set passive mode"); //$NON-NLS-1$
					ftp.setConnectMode(FTPConnectMode.PASV);

					System.out.println("   set type binary"); //$NON-NLS-1$
					ftp.setType(FTPTransferType.BINARY);

					showTileInfo(remoteName, -3);
					System.out.println("   chdir " + remoteDirName[0]); //$NON-NLS-1$
					ftp.chdir(remoteDirName[0]);

					ftp.setProgressMonitor(new FTPProgressMonitor() {
						public void bytesTransferred(final long count) {
							tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_LOADING_MONITOR, remoteName, count);
						}
					});

					showTileInfo(remoteName, -4);
					System.out.println("   get " + remoteName + " -> " + localName + " ..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					ftp.get(localName, remoteName);

					System.out.println("   quit"); //$NON-NLS-1$
					ftp.quit();

				} catch (final Exception e) {
					
					// ignore this error because the data can not be available
					
					e.printStackTrace();
					tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_ERROR_LOADING, remoteName, 0);

				} finally {
					tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_END_LOADING, remoteName, 0);
				}

				return Status.OK_STATUS;
			}
		};
		downloadJob.schedule();

		// wait until the job is finished
		try {
			downloadJob.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static String getDir(final String pathName) throws Exception {

		final String fileName = pathName.substring(pathName.lastIndexOf(File.separator) + 1);
		final String latString = fileName.substring(0, 3) + ":00"; // e.g. N50 //$NON-NLS-1$
		final String lonString = fileName.substring(3, 7) + ":00"; // e.g. E006 //$NON-NLS-1$
		final GeoLat lat = new GeoLat(latString);
		final GeoLon lon = new GeoLon(lonString);

		if (lat.greaterThen(new GeoLat("N60:00"))) //$NON-NLS-1$
			throw (new FileNotFoundException());
		if (lat.lessThen(new GeoLat("S56:00"))) //$NON-NLS-1$
			throw (new FileNotFoundException());

		// order important!
		// compare map ftp://e0srp01u.ecs.nasa.gov/srtm/version2/Documentation/Continent_def.gif
		if (isIn(lat, lon, "S56", "S28", "E165", "E179")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirIslands];
		if (isIn(lat, lon, "S56", "S55", "E158", "E159")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirIslands];
		if (isIn(lat, lon, "N15", "N30", "W180", "W155")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirIslands];
		if (isIn(lat, lon, "S44", "S05", "W030", "W006")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirIslands];
		if (isIn(lat, lon, "S56", "S45", "W039", "E060")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirIslands];
		if (isIn(lat, lon, "N35", "N39", "W040", "W020")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirAfrica];
		if (isIn(lat, lon, "S20", "S20", "E063", "E063")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirAfrica];
		if (isIn(lat, lon, "N10", "N10", "W110", "W110")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirNorth_America];
		if (isIn(lat, lon, "S10", "N14", "W180", "W139")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirEurasia];
		if (isIn(lat, lon, "S13", "S11", "E096", "E105")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirEurasia];
		if (isIn(lat, lon, "S44", "S11", "E112", "E179")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirAustralia];
		if (isIn(lat, lon, "S28", "S11", "W180", "W106")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirAustralia];
		if (isIn(lat, lon, "S35", "N34", "W030", "E059")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirAfrica];
		if (isIn(lat, lon, "N35", "N60", "W011", "E059")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirEurasia];
		if (isIn(lat, lon, "S10", "N60", "E060", "E179")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirEurasia];
		if (isIn(lat, lon, "N15", "N60", "W180", "W043")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirNorth_America];
		if (isIn(lat, lon, "S56", "N14", "W093", "W033")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return dirs[DirSouth_America];

		return dirs[DirIslands];
	}

	private static boolean isIn(final GeoLat lat,
								final GeoLon lon,
								final String latMin,
								final String latMax,
								final String lonMin,
								final String lonMax) {

		if (lat.lessThen(new GeoLat(latMin + ":00"))) //$NON-NLS-1$
			return false;
		if (lat.greaterThen(new GeoLat(latMax + ":00"))) //$NON-NLS-1$
			return false;
		if (lon.lessThen(new GeoLon(lonMin + ":00"))) //$NON-NLS-1$
			return false;
		if (lon.greaterThen(new GeoLon(lonMax + ":00"))) //$NON-NLS-1$
			return false;
		return true;
	}

	private static void showConnectError() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						Messages.srtm_transfer_error_title,
						NLS.bind(Messages.srtm_transfer_error_message, host));
			}
		});
	}

	private static void showTileInfo(final String remoteName, final int status) {
		TileInfoManager.getInstance().updateSRTMTileInfo(TileEventId.SRTM_DATA_START_LOADING, remoteName, status);
	}
}
