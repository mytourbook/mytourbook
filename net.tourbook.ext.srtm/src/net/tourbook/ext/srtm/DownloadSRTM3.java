package net.tourbook.ext.srtm;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPMessageCollector;
import com.enterprisedt.net.ftp.FTPTransferType;

public final class DownloadSRTM3 {

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
			System.out.println("remoteDir " + remoteDirName[0]); //$NON-NLS-1$
			System.out.println("localDir " + localDirName); //$NON-NLS-1$

			final File localDir = new File(localDirName);
			if (!localDir.exists()) {
				System.out.println("create Dir " + localDirName); //$NON-NLS-1$
				localDir.mkdir();
			}

			/*
			 * get connection to the host before the progress monitor is started
			 */
			ftp.setRemoteHost(host);
			ftp.setMessageListener(listener);

		} catch (final UnknownHostException e) {

			MessageDialog.openError(Display.getDefault().getActiveShell(),
					Messages.srtm_transfer_error_title,
					NLS.bind(Messages.srtm_transfer_error_message, host));

			return;
			
		} catch (final Exception e) {
			e.printStackTrace();
		}

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				try {

					monitor.beginTask(Messages.srtm_transfer_task, 2);
					monitor.subTask(NLS.bind(Messages.srtm_transfer_initialize, host));

					System.out.println("connect " + host); //$NON-NLS-1$
					ftp.connect();

					System.out.println("login " + user + " " + password); //$NON-NLS-1$ //$NON-NLS-2$
					ftp.login(user, password);

					System.out.println("set passive mode"); //$NON-NLS-1$
					ftp.setConnectMode(FTPConnectMode.PASV);

					System.out.println("set type binary"); //$NON-NLS-1$
					ftp.setType(FTPTransferType.BINARY);

					System.out.println("chdir " + remoteDirName[0]); //$NON-NLS-1$
					ftp.chdir(remoteDirName[0]);

					monitor.worked(1);
					monitor.subTask(NLS.bind(Messages.srtm_transfer_retrieve_file, remoteName));

					System.out.println("get " + remoteName + " -> " + localName + " ..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					ftp.get(localName, remoteName);

					monitor.worked(1);

					System.out.println("quit"); //$NON-NLS-1$
					ftp.quit();

				} catch (final Exception e) {
					throw new InvocationTargetException(e);
				}
			}
		};

		try {
			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, false, runnable);
		} catch (final InvocationTargetException e) {

//			final Throwable cause = e.getCause();

			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static String getDir(final String pathName) throws Exception {

		final String fileName = pathName.substring(pathName.lastIndexOf(File.separator) + 1);
		final String latString = fileName.substring(0, 3) + ":00"; // z.B. N50 //$NON-NLS-1$
		final String lonString = fileName.substring(3, 7) + ":00"; // z.B. E006 //$NON-NLS-1$
		final GeoLat lat = new GeoLat(latString);
		final GeoLon lon = new GeoLon(lonString);

		if (lat.groesser(new GeoLat("N60:00"))) //$NON-NLS-1$
			throw (new FileNotFoundException());
		if (lat.kleiner(new GeoLat("S56:00"))) //$NON-NLS-1$
			throw (new FileNotFoundException());

		// Reihenfolge wichtig!
		// vgl. Grafik ftp://e0srp01u.ecs.nasa.gov/srtm/version2/Documentation/Continent_def.gif
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

		if (lat.kleiner(new GeoLat(latMin + ":00"))) //$NON-NLS-1$
			return false;
		if (lat.groesser(new GeoLat(latMax + ":00"))) //$NON-NLS-1$
			return false;
		if (lon.kleiner(new GeoLon(lonMin + ":00"))) //$NON-NLS-1$
			return false;
		if (lon.groesser(new GeoLon(lonMax + ":00"))) //$NON-NLS-1$
			return false;
		return true;
	}

	public static void main(final String[] args) {

	}
}
