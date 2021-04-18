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
/**
 * @author Alfred Barten
 */
package net.tourbook.srtm.download;

public class DownloadSRTM3 {

   private static final String URL_BASE_PATH = "http://e4ftl01.cr.usgs.gov/MEASURES/SRTMGL3.003/2000.02.11/"; //$NON-NLS-1$
   //                              SRTM file = "http://e4ftl01.cr.usgs.gov/MEASURES/SRTMGL3.003/2000.02.11/N13E016.SRTMGL3.hgt.zip/"; //$NON-NLS-1$

   /**
    * @param remoteFileName
    * @param localZipName
    * @throws Exception
    */
   public static void get(final String remoteFileName, final String localZipName) throws Exception {

      HTTPDownloader.get(URL_BASE_PATH, remoteFileName, localZipName);
   }

}
