/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.tileinfo.TileInfoManager;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;
import net.tourbook.srtm.Messages;
import net.tourbook.tour.TourLogManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class HTTPDownloader {

   public final void get(final String baseUrl,
                         final String remoteFileName,
                         final String localFilePathName)
         throws Exception {

      final TileInfoManager tileInfoMgr = TileInfoManager.getInstance();
      final long[] numBytes_Written = new long[1];

      if (Display.getCurrent() != null) {

         /*
          * Display info in the status line when this is running in the UI thread because the
          * download will be blocking the UI thread until the download is finished
          */
         tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_START_LOADING, remoteFileName, -99);
      }

      final Job monitorJob = new Job(Messages.job_name_downloadMonitor) {
         @Override
         protected IStatus run(final IProgressMonitor monitor) {

            tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_LOADING_MONITOR, remoteFileName, numBytes_Written[0]);

            // update every 500ms
            this.schedule(500);

            return Status.OK_STATUS;
         }
      };

      final String jobName = remoteFileName + UI.DASH_WITH_SPACE + Messages.job_name_httpDownload;

      final Job downloadJob = new Job(jobName) {
         @Override
         protected IStatus run(final IProgressMonitor monitor) {

            tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_START_LOADING, remoteFileName, 0);

            InputStream inputStream = null;

            try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFilePathName))) {

               long startTime = System.currentTimeMillis();

               monitor.beginTask(jobName, IProgressMonitor.UNKNOWN);

               final String resourceUrl = baseUrl + remoteFileName;

               // show log in log view that a download durting tour import is visible
               TourLogManager.log_INFO(NLS.bind(Messages.Log_SRTM_DownloadingResource, resourceUrl));

               System.out.println(HTTPDownloader.class.getCanonicalName() + " - downloading: " + resourceUrl); //$NON-NLS-1$

               inputStream = getInputStream(resourceUrl);

               final byte[] buffer = new byte[1024];
               int numBytes_Read;

               while ((numBytes_Read = inputStream.read(buffer)) != -1) {

                  outputStream.write(buffer, 0, numBytes_Read);
                  numBytes_Written[0] += numBytes_Read;

                  // show number of received bytes every second
                  final long currentTime = System.currentTimeMillis();
                  if (currentTime > startTime + 1000) {

                     startTime = currentTime;

                     monitor.setTaskName(UI.EMPTY_STRING + numBytes_Written[0]);
                  }
               }

               System.out.println(HTTPDownloader.class.getCanonicalName() + " - # Bytes localName = " + numBytes_Written[0]); //$NON-NLS-1$

            } catch (final UnknownHostException e) {

               return new Status(
                     IStatus.ERROR,
                     TourbookPlugin.PLUGIN_ID,
                     IStatus.ERROR,
                     NLS.bind(Messages.error_message_cannotConnectToServer, baseUrl),
                     e);

            } catch (final SocketTimeoutException e) {

               return new Status(
                     IStatus.ERROR,
                     TourbookPlugin.PLUGIN_ID,
                     IStatus.ERROR,
                     NLS.bind(Messages.error_message_timeoutWhenConnectingToServer, baseUrl),
                     e);

            } catch (final Exception e) {

               return new Status(
                     IStatus.ERROR,
                     TourbookPlugin.PLUGIN_ID,
                     IStatus.ERROR,
                     e.getMessage(),
                     e);

            } finally {

               Util.close(inputStream);

               tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_END_LOADING, remoteFileName, 0);
            }

            System.out.println(HTTPDownloader.class.getCanonicalName() + " - get " + remoteFileName + " -> " + localFilePathName + " ..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

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
         Thread.currentThread().interrupt();
      }

      // stop monitor job
      try {
         monitorJob.cancel();
         monitorJob.join();
      } catch (final InterruptedException e) {
         e.printStackTrace();
         Thread.currentThread().interrupt();
      }

      // throw exception when it occured during the download
      final IStatus result = downloadJob.getResult();
      if (result != null) {
         final Exception e = (Exception) result.getException();
         if (e != null) {
            throw (e);
         }
      }

   }

   protected InputStream getInputStream(final String urlAddress) throws Exception {

      final URL url = new URL(urlAddress);
      final URLConnection urlConnection = url.openConnection();
      final InputStream inputStream = urlConnection.getInputStream();

      return inputStream;
   }
}
