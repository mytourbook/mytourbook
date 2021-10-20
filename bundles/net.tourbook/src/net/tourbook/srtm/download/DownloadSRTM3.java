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

import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.srtm.IPreferences;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Parts of this code is from
 * <p>
 * {@link https://wiki.earthdata.nasa.gov/display/EL/How+To+Access+Data+With+Java }
 */
public class DownloadSRTM3 {

   /**
    * Prefix used to identify redirects to URS for the purpose of adding authentication headers. For
    * test, this should be: https://uat.urs.earthdata.nasa.gov for test
    */
   private static final String           HTTP_URS_EARTHDATA_NASA_GOV = "https://urs.earthdata.nasa.gov"; //$NON-NLS-1$

   /**
    * SRTM example file
    * http://e4ftl01.cr.usgs.gov/MEASURES/SRTMGL3.003/2000.02.11/N13E016.SRTMGL3.hgt.zip
    */
   private static final String           URL_BASE_PATH               = "http://e4ftl01.cr.usgs.gov/MEASURES/SRTMGL3.003/2000.02.11/"; //$NON-NLS-1$

   private static final int              MAX_REDIRECTS               = 10;
   private static final int              MAX_REDIRECTED_LOGS         = MAX_REDIRECTS + 5;

   private static int                    _numRedirectedLogs;

   private static final IPreferenceStore _prefStore                  = TourbookPlugin.getPrefStore();

   private class SRTM3_HTTPDownloader extends HTTPDownloader {

      @Override
      protected InputStream getInputStream(final String urlAddress) throws Exception {

         final String password = _prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_PASSWORD);
         final String username = _prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_USER_NAME);

         return getResource(urlAddress, username, password);
      }
   }

   /**
    * @param remoteFileName
    * @param localZipName
    * @throws Exception
    */
   public void get(final String remoteFileName, final String localZipName) throws Exception {

      new SRTM3_HTTPDownloader().get(URL_BASE_PATH, remoteFileName, localZipName);
   }

   /**
    * Returns an input stream for a designated resource on a URS
    * authenticated remote server.
    *
    * @param resourceUrl
    * @param username
    * @param password
    * @return
    * @throws Exception
    */
   public InputStream getResource(String resourceUrl,
                                  final String username,
                                  final String password) throws Exception {

      /*
       * Set up a cookie handler to maintain session cookies. A custom
       * CookiePolicy could be used to limit cookies to just the resource
       * server and URS.
       */
      CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

      int redirects = 0;

      /* Place an upper limit on the number of redirects we will follow */
      while (redirects < MAX_REDIRECTS) {

         ++redirects;

         /*
          * Configure a connection to the resource server and submit the
          * request for our resource.
          */
         final URL url = new URL(resourceUrl);
         final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

         connection.setRequestMethod("GET"); //$NON-NLS-1$
         connection.setInstanceFollowRedirects(false);
         connection.setUseCaches(false);
         connection.setDoInput(true);

         /*
          * If this is the URS server, add in the authentication header.
          */
         if (resourceUrl.startsWith(HTTP_URS_EARTHDATA_NASA_GOV)) {

            final byte[] authorizationData = (username + ":" + password).getBytes(); //$NON-NLS-1$

            connection.setDoOutput(true);
            connection.setRequestProperty(
                  "Authorization", //$NON-NLS-1$
                  "Basic " + Base64.getEncoder().encodeToString(authorizationData)); //$NON-NLS-1$
         }

         /*
          * Execute the request and get the response status code.
          * A return status of 200 means is good - it means that we
          * have got our resource. We can return the input stream
          * so it can be read (may want to return additional header
          * information, such as the mime type or size ).
          */
         final int status = connection.getResponseCode();
         if (status == 200) {

            return connection.getInputStream();
         }

         /*
          * Any value other than 302 (a redirect) will need some custom
          * handling. A 401 from URS means that the credentials
          * are invalid, while a 403 means that the user has not authorized
          * the application.
          */
         if (status != 302) {

            final String message = connection.getResponseMessage();

            throw new Exception("Response from server - " + status + ": " + message); //$NON-NLS-1$ //$NON-NLS-2$
         }

         /*
          * Get the redirection location and continue. This should really
          * have a null check, just in case.
          */
         resourceUrl = connection.getHeaderField("Location"); //$NON-NLS-1$

         // log just a few redirects in case it is not working any more
         if (_numRedirectedLogs < MAX_REDIRECTED_LOGS) {

            _numRedirectedLogs++;

            System.out.println(this.getClass().getCanonicalName() + " - Redirecting to: " + resourceUrl); //$NON-NLS-1$
         }
      }

      /*
       * If we get here, we exceeded our redirect limit. This is most likely
       * a configuration problem somewhere in the remote server.
       */
      throw new Exception(String.format("Redirection limit %d exceeded", MAX_REDIRECTED_LOGS)); //$NON-NLS-1$
   }

}
