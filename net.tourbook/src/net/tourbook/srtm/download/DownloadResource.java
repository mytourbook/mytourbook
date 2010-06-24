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
package net.tourbook.srtm.download;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.util.UI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.tileinfo.TileInfoManager;

public class DownloadResource {

	public DownloadResource() {
		super();
	}

	protected static final void get(final String urlBase, final String remoteFileName, final String localFilePathName)
			throws Exception {

		final TileInfoManager tileInfoMgr = TileInfoManager.getInstance();
		final long[] numWritten = new long[1];

		final Job monitorJob = new Job("downloadMonitor") { //$NON-NLS-1$
			@Override
			protected IStatus run(final IProgressMonitor monitor) {

				tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_LOADING_MONITOR, remoteFileName, numWritten[0]);

				// rescedule every 200ms
				this.schedule(200);

				return Status.OK_STATUS;
			}
		};

		final Job downloadJob = new Job("downloadJob") { //$NON-NLS-1$
			@Override
			protected IStatus run(final IProgressMonitor monitor) {

				try {
					tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_START_LOADING, remoteFileName, 0);

					final String address = urlBase + remoteFileName;

					System.out.println("load " + address); //$NON-NLS-1$
					OutputStream outputStream = null;
					InputStream inputStream = null;
					try {

						final URL url = new URL(address);
						outputStream = new BufferedOutputStream(new FileOutputStream(localFilePathName));
						final URLConnection urlConnection = url.openConnection();
						inputStream = urlConnection.getInputStream();

						final byte[] buffer = new byte[1024];
						int numRead;

						while ((numRead = inputStream.read(buffer)) != -1) {
							outputStream.write(buffer, 0, numRead);
							numWritten[0] += numRead;
						}

						System.out.println("# Bytes localName = " + numWritten); //$NON-NLS-1$

					} catch (final Exception e) {
						e.printStackTrace();
					} finally {
						try {
							if (inputStream != null) {
								inputStream.close();
							}
							if (outputStream != null) {
								outputStream.close();
							}
						} catch (final IOException ioe) {
							ioe.printStackTrace();
						}
					}

					System.out.println("get " + remoteFileName + " -> " + localFilePathName + " ..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				} catch (final Exception e) {

					System.out.println(e.getMessage());

					tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_ERROR_LOADING, remoteFileName, 0);

					return new Status(IStatus.ERROR, //
							TourbookPlugin.PLUGIN_ID,
							IStatus.ERROR,
							e.getMessage() == null ? UI.EMPTY_STRING : e.getMessage(),
							e);

				} finally {
					tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_END_LOADING, remoteFileName, 0);
				}

				return Status.OK_STATUS;
			}
		};

		monitorJob.schedule();
		downloadJob.schedule();

		// wait until the download job is finished
		try {
			downloadJob.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		// stop monitor job
		try {
			monitorJob.cancel();
			monitorJob.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		// throw exception when it occured during the download
		final Exception e = (Exception) downloadJob.getResult().getException();
		if (e != null) {
			throw (e);
		}

	}

}
