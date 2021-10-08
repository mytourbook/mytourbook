/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
package net.tourbook.cloud.strava;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.MultiPartBodyPublisher;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.FilesUtils;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.export.ExportTourTCX;
import net.tourbook.export.TourExporter;
import net.tourbook.ext.velocity.VelocityService;
import net.tourbook.extension.upload.TourbookCloudUploader;
import net.tourbook.tour.TourLogManager;

import org.apache.http.HttpHeaders;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;

public class StravaUploader extends TourbookCloudUploader {

   private static final String     LOG_CLOUDACTION_END           = net.tourbook.cloud.Messages.Log_CloudAction_End;
   private static final String     LOG_CLOUDACTION_INVALIDTOKENS = net.tourbook.cloud.Messages.Log_CloudAction_InvalidTokens;

   private static final String     StravaBaseUrl                 = "https://www.strava.com/api/v3";                                      //$NON-NLS-1$

   private static HttpClient       _httpClient                   = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();
   private static IPreferenceStore _prefStore                    = Activator.getDefault().getPreferenceStore();
   private static TourExporter     _tourExporter                 = new TourExporter(ExportTourTCX.TCX_2_0_TEMPLATE);

   // Source : https://support.strava.com/hc/en-us/articles/216919407-Supported-Activity-Types-on-Strava
   private static final List<String> StravaActivityTypes = List.of(
         "Ride", //$NON-NLS-1$
         "Run", //$NON-NLS-1$
         "Swim", //$NON-NLS-1$
         "Walk", //$NON-NLS-1$
         "Hike", //$NON-NLS-1$
         "Alpine Ski", //$NON-NLS-1$
         "Backcountry Ski", //$NON-NLS-1$
         "Canoe", //$NON-NLS-1$
         "Crossfit", //$NON-NLS-1$
         "E-Bike Ride", //$NON-NLS-1$
         "Elliptical", //$NON-NLS-1$
         "Handcycle", //$NON-NLS-1$
         "Ice Skate", //$NON-NLS-1$
         "Inline Skate", //$NON-NLS-1$
         "Kayak", //$NON-NLS-1$
         "Kitesurf Session", //$NON-NLS-1$
         "Nordic Ski", //$NON-NLS-1$
         "Rock Climb", //$NON-NLS-1$
         "Roller Ski", //$NON-NLS-1$
         "Row", //$NON-NLS-1$
         "Snowboard", //$NON-NLS-1$
         "Snowshoe", //$NON-NLS-1$
         "Stair Stepper", //$NON-NLS-1$
         "Stand Up Paddle", //$NON-NLS-1$
         "Surf", //$NON-NLS-1$
         "Virtual Ride", //$NON-NLS-1$
         "Virtual Run", //$NON-NLS-1$
         "Weight Training", //$NON-NLS-1$
         "Windsurf Session", //$NON-NLS-1$
         "Wheelchair", //$NON-NLS-1$
         "Workout", //$NON-NLS-1$
         "Yoga" //$NON-NLS-1$
   );

   public StravaUploader() {

      super("STRAVA", Messages.VendorName_Strava); //$NON-NLS-1$

      _tourExporter.setUseDescription(true);

      VelocityService.init();
   }

   private static ActivityUpload convertResponseToUpload(final HttpResponse<String> response, final String tourDate) {

      ActivityUpload activityUpload = new ActivityUpload();

      if (response.statusCode() == HttpURLConnection.HTTP_CREATED && StringUtils.hasContent(response.body())) {

         final ObjectMapper mapper = new ObjectMapper();
         try {
            activityUpload = mapper.readValue(response.body(), ActivityUpload.class);
         } catch (final JsonProcessingException e) {
            StatusUtil.log(e);
         }
      } else {
         activityUpload.setError(response.body());
      }

      activityUpload.setTourDate(tourDate);

      return activityUpload;
   }

   public static StravaTokens getTokens(final String authorizationCode, final boolean isRefreshToken, final String refreshToken) {

      final JSONObject body = new JSONObject();
      String grantType;
      if (isRefreshToken) {
         body.put(OAuth2Constants.PARAM_REFRESH_TOKEN, refreshToken);
         grantType = OAuth2Constants.PARAM_REFRESH_TOKEN;
      } else {
         body.put(OAuth2Constants.PARAM_CODE, authorizationCode);
         grantType = OAuth2Constants.PARAM_AUTHORIZATION_CODE;
      }

      body.put(OAuth2Constants.PARAM_GRANT_TYPE, grantType);

      final HttpRequest request = HttpRequest.newBuilder()
            .header(OAuth2Constants.CONTENT_TYPE, "application/json") //$NON-NLS-1$
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .uri(URI.create(OAuth2Constants.HEROKU_APP_URL + "/strava/token"))//$NON-NLS-1$
            .build();

      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_CREATED && StringUtils.hasContent(response.body())) {
            return new ObjectMapper().readValue(response.body(), StravaTokens.class);
         } else {
            StatusUtil.logError(response.body());
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return null;
   }

   private static String gzipFile(final String file) {

      final String compressedFilePath = file + ".gz"; //$NON-NLS-1$

      try (final FileInputStream fileInputStream = new FileInputStream(file);
            final FileOutputStream fileOutputStream = new FileOutputStream(compressedFilePath);
            final GZIPOutputStream gzipOS = new GZIPOutputStream(fileOutputStream)) {

         final byte[] buffer = new byte[1024];
         int length;
         while ((length = fileInputStream.read(buffer)) != -1) {
            gzipOS.write(buffer, 0, length);
         }
      } catch (final IOException e) {
         StatusUtil.log(e);
         return UI.EMPTY_STRING;
      }

      return compressedFilePath;
   }

   private void createCompressedTcxTourFile(final IProgressMonitor monitor,
                                            final Map<String, TourData> toursWithTimeSeries,
                                            final TourData tourData) {

      final String absoluteTourFilePath = FilesUtils.createTemporaryFile(String.valueOf(tourData.getTourId()), "tcx"); //$NON-NLS-1$

      toursWithTimeSeries.put(exportTcxGzFile(tourData, absoluteTourFilePath), tourData);

      FilesUtils.deleteIfExists(Paths.get(absoluteTourFilePath));

      monitor.worked(1);
   }

   private void deleteTemporaryTourFiles(final Map<String, TourData> tourFiles) {

      tourFiles.keySet().forEach(tourFilePath -> FilesUtils.deleteIfExists(Paths.get(
            tourFilePath)));
   }

   private String exportTcxGzFile(final TourData tourData, final String absoluteTourFilePath) {

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

      _tourExporter.export(absoluteTourFilePath);

      return gzipFile(absoluteTourFilePath);
   }

   private String getAccessToken() {
      return _prefStore.getString(Preferences.STRAVA_ACCESSTOKEN);
   }

   private long getAccessTokenExpirationDate() {
      return _prefStore.getLong(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT);
   }

   private String getRefreshToken() {
      return _prefStore.getString(Preferences.STRAVA_REFRESHTOKEN);
   }

   private boolean getValidTokens() {

      if (!OAuth2Utils.isAccessTokenExpired(getAccessTokenExpirationDate())) {
         return true;
      }

      final StravaTokens newTokens = getTokens(UI.EMPTY_STRING, true, getRefreshToken());

      boolean isTokenValid = false;
      if (newTokens != null) {
         setAccessTokenExpirationDate(newTokens.getExpires_at());
         setRefreshToken(newTokens.getRefresh_token());
         setAccessToken(newTokens.getAccess_token());
         isTokenValid = true;
      }

      return isTokenValid;
   }

   @Override
   protected boolean isReady() {
      return StringUtils.hasContent(getAccessToken()) && StringUtils.hasContent(getRefreshToken());
   }

   private boolean logUploadResult(final ActivityUpload activityUpload) {

      boolean isTourUploaded = false;

      if (StringUtils.hasContent(activityUpload.getError())) {

         TourLogManager.log_ERROR(NLS.bind(
               Messages.Log_UploadToursToStrava_004_UploadError,
               activityUpload.getTourDate(),
               activityUpload.getError()));

      } else {

         isTourUploaded = true;

         if (StringUtils.hasContent(activityUpload.getName())) {

            TourLogManager.log_OK(NLS.bind(
                  Messages.Log_UploadToursToStrava_003_ActivityLink,
                  activityUpload.getTourDate(),
                  activityUpload.getId()));
         } else {

            TourLogManager.log_OK(NLS.bind(
                  Messages.Log_UploadToursToStrava_003_UploadStatus,
                  new Object[] {
                        activityUpload.getTourDate(),
                        activityUpload.getId(),
                        activityUpload.getStatus() }));
         }
      }

      return isTourUploaded;
   }

   private String mapTourType(final TourData manualTour) {

      final String tourTypeName = manualTour.getTourType() != null
            ? manualTour.getTourType().getName()
            : UI.EMPTY_STRING;

      return StravaActivityTypes.stream().filter(
            stravaActivityType -> tourTypeName.toLowerCase().startsWith(stravaActivityType.toLowerCase()))
            .findFirst()
            .orElse(StravaActivityTypes.get(0));
   }

   private void processManualTour(final IProgressMonitor monitor, final List<TourData> manualTours, final TourData tourData) {

      if (StringUtils.isNullOrEmpty(tourData.getTourTitle())) {

         final String tourDate = tourData.getTourStartTime().format(TimeTools.Formatter_DateTime_S);

         TourLogManager.log_ERROR(NLS.bind(Messages.Log_UploadToursToStrava_002_NoTourTitle, tourDate));
         monitor.worked(2);

      } else {

         manualTours.add(tourData);
         monitor.worked(1);
      }
   }

   private CompletableFuture<ActivityUpload> sendAsyncRequest(final TourData tour, final HttpRequest request) {

      final String tourDate = tour.getTourStartTime().format(TimeTools.Formatter_DateTime_S);

      final CompletableFuture<ActivityUpload> activityUpload = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(name -> convertResponseToUpload(name, tourDate))
            .exceptionally(e -> {
               final ActivityUpload errorUpload = new ActivityUpload();
               errorUpload.setTourDate(tourDate);
               errorUpload.setError(e.getMessage());
               return errorUpload;
            });

      return activityUpload;
   }

   private void setAccessToken(final String accessToken) {
      _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN, accessToken);
   }

   private void setAccessTokenExpirationDate(final long expireAt) {
      _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT, expireAt);
   }

   private void setRefreshToken(final String refreshToken) {
      _prefStore.setValue(Preferences.STRAVA_REFRESHTOKEN, refreshToken);
   }

   /**
    * https://developers.strava.com/playground/#/Uploads/createUpload
    *
    * @param compressedTourAbsoluteFilePath
    * @param tourData
    * @return
    */
   private CompletableFuture<ActivityUpload> uploadFile(final String compressedTourAbsoluteFilePath, final TourData tourData) {

      final MultiPartBodyPublisher publisher = new MultiPartBodyPublisher()
            .addPart("data_type", "tcx.gz") //$NON-NLS-1$ //$NON-NLS-2$
            .addPart("name", tourData.getTourTitle()) //$NON-NLS-1$
            .addPart("description", tourData.getTourDescription()) //$NON-NLS-1$
            .addPart("file", Paths.get(compressedTourAbsoluteFilePath)); //$NON-NLS-1$

      final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(StravaBaseUrl + "/uploads")) //$NON-NLS-1$
            .header(HttpHeaders.AUTHORIZATION, OAuth2Constants.BEARER + getAccessToken())
            .header(OAuth2Constants.CONTENT_TYPE, "multipart/form-data; boundary=" + publisher.getBoundary()) //$NON-NLS-1$
            .timeout(Duration.ofMinutes(5))
            .POST(publisher.build())
            .build();

      return sendAsyncRequest(tourData, request);
   }

   /**
    * https://developers.strava.com/playground/#/Activities/createActivity
    *
    * @param manualTour
    * @return
    */
   private CompletableFuture<ActivityUpload> uploadManualTour(final TourData manualTour) {

      final String stravaActivityType = mapTourType(manualTour);

      final boolean isTrainerActivity = manualTour.getTourType() != null && manualTour.getTourType().getName().equalsIgnoreCase("trainer"); //$NON-NLS-1$

      final JSONObject body = new JSONObject();
      body.put("name", manualTour.getTourTitle()); //$NON-NLS-1$
      body.put("type", stravaActivityType); //$NON-NLS-1$
      body.put("start_date_local", manualTour.getTourStartTime().format(DateTimeFormatter.ISO_DATE_TIME)); //$NON-NLS-1$
      body.put("elapsed_time", manualTour.getTourDeviceTime_Elapsed()); //$NON-NLS-1$
      body.put("description", manualTour.getTourDescription()); //$NON-NLS-1$
      body.put("distance", manualTour.getTourDistance()); //$NON-NLS-1$
      body.put("trainer", (isTrainerActivity ? "1" : "0")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(StravaBaseUrl + "/activities")) //$NON-NLS-1$
            .header(HttpHeaders.AUTHORIZATION, OAuth2Constants.BEARER + getAccessToken())
            .header(OAuth2Constants.CONTENT_TYPE, "application/json") //$NON-NLS-1$
            .timeout(Duration.ofMinutes(5))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

      return sendAsyncRequest(manualTour, request);
   }

   @Override
   public void uploadTours(final List<TourData> selectedTours) {

      final int numberOfTours = selectedTours.size();
      final int[] numberOfUploadedTours = new int[1];

      final IRunnableWithProgress runnable = new IRunnableWithProgress() {

         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            monitor.beginTask(NLS.bind(Messages.Dialog_UploadToursToStrava_Task,
                  numberOfTours,
                  _prefStore.getString(Preferences.STRAVA_ATHLETEFULLNAME)),
                  numberOfTours * 2);

            if (!getValidTokens()) {
               TourLogManager.log_ERROR(LOG_CLOUDACTION_INVALIDTOKENS);
               return;
            }

            monitor.subTask(NLS.bind(Messages.Dialog_UploadToursToStrava_SubTask, UI.SYMBOL_HOURGLASS_WITH_FLOWING_SAND, UI.EMPTY_STRING));

            final Map<String, TourData> toursWithTimeSeries = new HashMap<>();
            final List<TourData> manualTours = new ArrayList<>();
            for (int index = 0; index < numberOfTours && !monitor.isCanceled(); ++index) {

               final TourData tourData = selectedTours.get(index);

               if (tourData.timeSerie == null || tourData.timeSerie.length == 0) {

                  processManualTour(monitor, manualTours, tourData);
               } else {

                  createCompressedTcxTourFile(monitor, toursWithTimeSeries, tourData);
               }
            }

            if (monitor.isCanceled()) {
               deleteTemporaryTourFiles(toursWithTimeSeries);
            }

            monitor.subTask(NLS.bind(Messages.Dialog_UploadToursToStrava_SubTask,
                  UI.SYMBOL_WHITE_HEAVY_CHECK_MARK,
                  UI.SYMBOL_HOURGLASS_WITH_FLOWING_SAND));

            numberOfUploadedTours[0] = uploadTours(toursWithTimeSeries, manualTours, monitor);

            monitor.worked(toursWithTimeSeries.size() + manualTours.size());

            monitor.subTask(NLS.bind(Messages.Dialog_UploadToursToStrava_SubTask,
                  UI.SYMBOL_WHITE_HEAVY_CHECK_MARK,
                  UI.SYMBOL_WHITE_HEAVY_CHECK_MARK));
         }

      };

      try {
         final long start = System.currentTimeMillis();

         TourLogManager.showLogView();
         TourLogManager.log_TITLE(NLS.bind(Messages.Log_UploadToursToStrava_001_Start, numberOfTours));

         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, runnable);

         TourLogManager.log_TITLE(String.format(LOG_CLOUDACTION_END, (System.currentTimeMillis() - start) / 1000.0));

         MessageDialog.openInformation(
               Display.getDefault().getActiveShell(),
               Messages.Dialog_UploadToursToStrava_Title,
               NLS.bind(Messages.Dialog_UploadToursToStrava_Message, numberOfUploadedTours[0], numberOfTours - numberOfUploadedTours[0]));

      } catch (final InvocationTargetException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }
   }

   private int uploadTours(final Map<String, TourData> toursWithTimeSeries, final List<TourData> manualTours, final IProgressMonitor monitor) {

      final List<CompletableFuture<ActivityUpload>> activityUploads = new ArrayList<>();

      for (final Map.Entry<String, TourData> tourToUpload : toursWithTimeSeries.entrySet()) {

         if (monitor.isCanceled()) {
            return 0;
         }

         final String compressedTourAbsoluteFilePath = tourToUpload.getKey();
         final TourData tourData = tourToUpload.getValue();

         activityUploads.add(uploadFile(compressedTourAbsoluteFilePath, tourData));
      }

      manualTours.stream().forEach(manualTour -> activityUploads.add(uploadManualTour(manualTour)));

      final int[] numberOfUploadedTours = new int[1];
      activityUploads.stream().map(CompletableFuture::join).forEach(activityUpload -> {
         if (monitor.isCanceled()) {
            return;
         } else if (logUploadResult(activityUpload)) {
            ++numberOfUploadedTours[0];
         }
      });

      deleteTemporaryTourFiles(toursWithTimeSeries);

      return numberOfUploadedTours[0];
   }
}
