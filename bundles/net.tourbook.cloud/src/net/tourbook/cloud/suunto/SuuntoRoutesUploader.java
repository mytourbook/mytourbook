/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.cloud.suunto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.FilesUtils;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.export.ExportTourGPX;
import net.tourbook.export.TourExporter;
import net.tourbook.ext.velocity.VelocityService;
import net.tourbook.extension.upload.TourbookCloudUploader;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogState;

import org.apache.http.HttpHeaders;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;

public class SuuntoRoutesUploader extends TourbookCloudUploader {

   private static final String     ICON__CHECK                   = net.tourbook.cloud.Messages.Icon__Check;
   private static final String     ICON__HOURGLASS               = net.tourbook.cloud.Messages.Icon__Hourglass;
   private static final String     LOG_CLOUDACTION_END           = net.tourbook.cloud.Messages.Log_CloudAction_End;
   private static final String     LOG_CLOUDACTION_INVALIDTOKENS = net.tourbook.cloud.Messages.Log_CloudAction_InvalidTokens;

   private static HttpClient       _httpClient                   = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();
   private static IPreferenceStore _prefStore                    = Activator.getDefault().getPreferenceStore();
   private static TourExporter     _tourExporter                 = new TourExporter(ExportTourGPX.GPX_1_0_TEMPLATE);

   public SuuntoRoutesUploader() {
      super("SUUNTO", Messages.VendorName_Suunto_Routes); //$NON-NLS-1$

      _tourExporter.setUseDescription(true);

      VelocityService.init();
   }

   private RouteUpload convertResponseToUpload(final HttpResponse<String> routeUploadResponse, final String tourDate) {

      RouteUploads routeUploads = new RouteUploads();
      routeUploads.items = new ArrayList<>();
      routeUploads.items.add(new RouteUpload());

      final String routeUploadContent = routeUploadResponse.body();
      if (routeUploadResponse.statusCode() == HttpURLConnection.HTTP_CREATED && StringUtils.hasContent(routeUploadContent)) {

         final ObjectMapper mapper = new ObjectMapper();
         try {
            routeUploads = mapper.readValue(routeUploadContent, RouteUploads.class);

         } catch (final JsonProcessingException e) {
            StatusUtil.log(e);
         }
      } else {
         routeUploads.items.get(0).setError(routeUploadContent);
      }

      routeUploads.items.get(0).setTourDate(tourDate);

      return routeUploads.items.get(0);
   }

   /**
    * Activities by Suunto
    * https://apimgmtstfbqznm5nc6zmvgx.blob.core.windows.net/content/MediaLibrary/docs/Suunto%20Watches-%20SuuntoApp%20-Movescount-FIT-Activities.pdf
    *
    * @param tourData
    * @return
    */
   private String convertTourToGpx(final TourData tourData) {

      final String absoluteTourFilePath = FilesUtils.createTemporaryFile(String.valueOf(tourData.getTourId()), "gpx"); //$NON-NLS-1$

      _tourExporter.useTourData(tourData);

      final TourType tourType = tourData.getTourType();

      boolean useActivityType = false;
      String activityName = UI.EMPTY_STRING;
      if (tourType != null) {
         useActivityType = true;
         activityName = tourType.getName();
      }
      _tourExporter.setUseActivityType(useActivityType);
      _tourExporter.setActivityType(activityName);

      if (tourData.timeSerie == null || tourData.timeSerie.length == 0 || tourData.getTourDeviceTime_Elapsed() == 0) {
         _tourExporter.setIsCamouflageSpeed(true);
         _tourExporter.setCamouflageSpeed(10);
      } else {
         _tourExporter.setIsCamouflageSpeed(false);
      }

      _tourExporter.export(absoluteTourFilePath);

      final String tourGpx = FilesUtils.readFileContentString(absoluteTourFilePath);

      FilesUtils.deleteIfExists(Paths.get(absoluteTourFilePath));

      return tourGpx;
   }

   private String getAccessToken() {
      return _prefStore.getString(Preferences.SUUNTO_ACCESSTOKEN);
   }

   private String getRefreshToken() {
      return _prefStore.getString(Preferences.SUUNTO_REFRESHTOKEN);
   }

   @Override
   protected boolean isReady() {
      return StringUtils.hasContent(getAccessToken() + getRefreshToken());
   }

   private boolean logUploadResult(final RouteUpload routeUpload) {

      boolean isRouteUploaded = false;

      if (StringUtils.hasContent(routeUpload.getError())) {

         TourLogManager.logError(NLS.bind(Messages.Log_UploadToursToSuunto_004_UploadError, routeUpload.getTourDate(), routeUpload.getError()));

      } else {

         isRouteUploaded = true;

         TourLogManager.addLog(TourLogState.IMPORT_OK,
               NLS.bind(Messages.Log_UploadToursToSuunto_003_UploadStatus, routeUpload.getTourDate(), routeUpload.getId()));

      }

      return isRouteUploaded;
   }

   private CompletableFuture<RouteUpload> sendAsyncRequest(final String tourStartTime, final HttpRequest request) {

      final CompletableFuture<RouteUpload> routeUpload = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(routeUploadResponse -> convertResponseToUpload(routeUploadResponse, tourStartTime))
            .exceptionally(e -> {
               final RouteUpload errorUpload = new RouteUpload();
               errorUpload.setTourDate(tourStartTime);
               errorUpload.setError(e.getMessage());
               return errorUpload;
            });

      return routeUpload;
   }

   private CompletableFuture<RouteUpload> uploadRoute(final String tourStartTime, final String tourGpx) {

      //create a vm template just for that ?
      //https://apizone.suunto.com/route-description
      final JSONObject payload = new JSONObject();
      payload.put("gpxRoute", Base64.getEncoder().encodeToString(tourGpx.getBytes())); //$NON-NLS-1$

      try {
         final HttpRequest request = HttpRequest.newBuilder()
               .uri(URI.create(OAuth2Constants.HEROKU_APP_URL + "/suunto/route/import"))//$NON-NLS-1$
               .header(OAuth2Constants.CONTENT_TYPE, "application/json") //$NON-NLS-1$
               .header(HttpHeaders.AUTHORIZATION, OAuth2Constants.BEARER + getAccessToken()) //     .timeout(Duration.ofMinutes(5))
               .POST(BodyPublishers.ofString(payload.toString()))
               .build();

         return sendAsyncRequest(tourStartTime, request);

      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return null;
   }

   private int uploadRoutes(final Map<String, String> toursWithGpsSeries) {

      final List<CompletableFuture<RouteUpload>> activityUploads = new ArrayList<>();

      for (final Map.Entry<String, String> tourToUpload : toursWithGpsSeries.entrySet()) {

         final String tourStartTime = tourToUpload.getKey();
         final String tourGpx = tourToUpload.getValue();

         activityUploads.add(uploadRoute(tourStartTime, tourGpx));
      }

      final int[] numberOfUploadedTours = new int[1];
      activityUploads.stream().map(CompletableFuture::join).forEach(activityUpload -> {
         if (logUploadResult(activityUpload)) {
            ++numberOfUploadedTours[0];
         }
      });

      return numberOfUploadedTours[0];
   }

   @Override
   public void uploadTours(final List<TourData> selectedTours) {

      final int numberOfTours = selectedTours.size();
      final int[] numberOfUploadedTours = new int[1];

      final IRunnableWithProgress runnable = new IRunnableWithProgress() {

         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            monitor.beginTask(NLS.bind(Messages.Dialog_UploadRoutes_Task, numberOfTours), numberOfTours * 2);

            monitor.subTask(Messages.ValidatingSuuntoTokens_SubTask);

            if (!SuuntoTokensRetrievalHandler.getValidTokens()) {
               TourLogManager.logError(LOG_CLOUDACTION_INVALIDTOKENS);
               return;
            }

            monitor.subTask(NLS.bind(Messages.Dialog_UploadRoutes_SubTask, ICON__HOURGLASS, UI.EMPTY_STRING));

            final Map<String, String> toursWithGpsSeries = new HashMap<>();
            for (int index = 0; index < numberOfTours; ++index) {

               final TourData tourData = selectedTours.get(index);
               final String tourStartTime = tourData.getTourStartTime().format(TimeTools.Formatter_DateTime_S);

               if (tourData.latitudeSerie == null || tourData.latitudeSerie.length == 0) {

                  TourLogManager.logError(NLS.bind(Messages.Log_UploadToursToSuunto_002_NoGpsCoordinate, tourStartTime));
                  monitor.worked(2);

               } else {

                  toursWithGpsSeries.put(tourStartTime, convertTourToGpx(tourData));

                  monitor.worked(1);
               }
            }

            monitor.subTask(NLS.bind(Messages.Dialog_UploadRoutes_SubTask, ICON__CHECK, ICON__HOURGLASS));

            if (SuuntoTokensRetrievalHandler.getValidTokens()) {
               numberOfUploadedTours[0] = uploadRoutes(toursWithGpsSeries);
            } else {
               TourLogManager.logError(LOG_CLOUDACTION_INVALIDTOKENS);
            }

            monitor.worked(toursWithGpsSeries.size());

            monitor.subTask(NLS.bind(Messages.Dialog_UploadRoutes_SubTask, ICON__CHECK, ICON__CHECK));
         }
      };

      try {
         final long start = System.currentTimeMillis();

         TourLogManager.showLogView();
         TourLogManager.logTitle(NLS.bind(Messages.Log_UploadToursToSuunto_001_Start, numberOfTours));

         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, runnable);

         TourLogManager.logTitle(String.format(LOG_CLOUDACTION_END, (System.currentTimeMillis() - start) / 1000.0));

         MessageDialog.openInformation(
               Display.getDefault().getActiveShell(),
               Messages.Dialog_UploadRoutes_Title,
               NLS.bind(Messages.Dialog_UploadRoutes_Message, numberOfUploadedTours[0], numberOfTours - numberOfUploadedTours[0]));

      } catch (final InvocationTargetException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }
   }
}
