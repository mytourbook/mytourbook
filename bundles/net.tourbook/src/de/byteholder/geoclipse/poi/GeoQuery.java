/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.poi;

import de.byteholder.gpx.PointOfInterest;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;

import net.tourbook.application.ApplicationVersion;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.XmlUtils;
import net.tourbook.web.WEB;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class GeoQuery implements Runnable {

// private final static String   URL         = "http://www.frankieandshadow.com/osm/search.xml?find="; //                   //$NON-NLS-1$
// private final static String   SEARCH_URL  = "http://gazetteer.openstreetmap.org/namefinder/search.xml?find="; //         //$NON-NLS-1$
   private static final String   SEARCH_URL  = "https://nominatim.openstreetmap.org/search?format=xml&addressdetails=0&q="; //$NON-NLS-1$

   private static final String   USER_AGENT  = String.format(

         "MyTourbook - %s - Version %s - https://mytourbook.sourceforge.io",                                                //$NON-NLS-1$
         System.getProperty("os.name"),                                                                                     //$NON-NLS-1$
         ApplicationVersion.getVersionSimple());

   private static HttpClient     _httpClient = HttpClient

         .newBuilder()
         .connectTimeout(Duration.ofMinutes(1))
         .build();

   private List<PointOfInterest> _allPOIs    = new ArrayList<>();

   private String                _query;

   private PropertyChangeSupport _propertyChangeSupport;

   public GeoQuery() {

      _propertyChangeSupport = new PropertyChangeSupport(this);
   }

   void addPropertyChangeListener(final PropertyChangeListener propertyChangeListener) {

      _propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
   }

   void asyncFind(final String query) {

      _query = query;

      final Job job = new Job(Messages.job_name_searchingPOI) {

         @Override
         protected IStatus run(final IProgressMonitor arg0) {
            GeoQuery.this.run();
            return Status.OK_STATUS;
         }
      };

      job.schedule();
   }

   void removePropertyChangeListener(final PropertyChangeListener propertyChangeListener) {

      _propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
   }

   @Override
   public void run() {

      List<PointOfInterest> allOldPOIs = List.copyOf(_allPOIs);

      try {

         _allPOIs.clear();

         final String searchURI = SEARCH_URL + URLEncoder.encode(_query, "utf8"); //$NON-NLS-1$

         final SAXParser saxParser = XmlUtils.initializeParser();
         final GeoQuerySAXHandler saxHandler = new GeoQuerySAXHandler(_allPOIs);

         final HttpRequest request = HttpRequest
               .newBuilder()
               .GET()
               .header(WEB.HTTP_HEADER_USER_AGENT, USER_AGENT)
               .uri(URI.create(searchURI))
               .build();

         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK

               && StringUtils.hasContent(response.body())) {

            final String xmlString = response.body();

            saxParser.parse(new InputSource(new StringReader(xmlString)), saxHandler);

            // Sending an old value of null will trigger the firing.
            // Otherwise, empty values for both old and new values will not and as
            // a consequence, will leave the POIView waiting for a response forever.
            if (allOldPOIs.isEmpty() && _allPOIs.isEmpty()) {

               allOldPOIs = null;
            }

            _propertyChangeSupport.firePropertyChange("_searchResult", allOldPOIs, _allPOIs); //$NON-NLS-1$

            return;

         } else {

            StatusUtil.logError(response.body());
         }

      } catch (IOException | InterruptedException | SAXException e) {

         StatusUtil.log(e);

         Thread.currentThread().interrupt();
      }
   }
}
