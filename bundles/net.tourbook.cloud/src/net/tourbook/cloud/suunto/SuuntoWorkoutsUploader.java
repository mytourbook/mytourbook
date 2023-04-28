/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.CloudImages;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.util.FileUtils;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.export.TourExporter;
import net.tourbook.extension.upload.TourbookCloudUploader;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;

public class SuuntoWorkoutsUploader extends TourbookCloudUploader {

   private static final String LOG_CLOUDACTION_END           = net.tourbook.cloud.Messages.Log_CloudAction_End;
   private static final String LOG_CLOUDACTION_INVALIDTOKENS = net.tourbook.cloud.Messages.Log_CloudAction_InvalidTokens;

   private static TourExporter _tourExporter                 = new TourExporter("fit");                                  //$NON-NLS-1$

   private static String       CLOUD_UPLOADER_ID             = "Suunto";                                                 //$NON-NLS-1$
   private boolean             _useActivePerson;
   private boolean             _useAllPeople;

   public SuuntoWorkoutsUploader() {

      super(CLOUD_UPLOADER_ID,
            Messages.VendorName_Suunto_Workouts,
            Activator.getImageDescriptor(CloudImages.Cloud_Suunto));
   }

   private WorkoutUpload checkUploadStatus(final TourData tourData, final String workoutUploadId) {

      final HttpRequest request = HttpRequest.newBuilder()
            .uri(OAuth2Utils.createOAuthPasseurUri("/suunto/workout/upload/" + workoutUploadId))//$NON-NLS-1$
            .header(OAuth2Constants.AUTHORIZATION, OAuth2Constants.BEARER + getAccessToken())
            .timeout(Duration.ofMinutes(5))
            .GET()
            .build();

      final HttpResponse<String> response = sendAsyncRequest(tourData, request);
      if (response == null) {
         return null;
      }

      return serializeWorkoutUpload(response);
   }

   private String createTourFile_Fit(final TourData tourData) {

      final String absoluteTourFilePath = FileUtils.createTemporaryFile(
            String.valueOf(tourData.getTourId()),
            "fit"); //$NON-NLS-1$

      exportToFitFile(tourData, absoluteTourFilePath);

      return absoluteTourFilePath;
   }

   private void deleteTemporaryFitFiles(final Map<String, TourData> tourFiles) {

      tourFiles.keySet().forEach(tourFilePath -> FileUtils.deleteIfExists(Paths.get(
            tourFilePath)));
   }

   private void deleteTemporaryTourFiles(final Map<String, TourData> tourFiles) {

      tourFiles.keySet().forEach(tourFilePath -> FileUtils.deleteIfExists(Paths.get(
            tourFilePath)));
   }

   private void exportToFitFile(final TourData tourData, final String absoluteTourFilePath) {

      _tourExporter.useTourData(tourData).export(absoluteTourFilePath);
   }

   private String getAccessToken() {

      if (_useActivePerson) {
         return SuuntoTokensRetrievalHandler.getAccessToken_ActivePerson();
      } else if (_useAllPeople) {
         return SuuntoTokensRetrievalHandler.getAccessToken_AllPeople();
      }

      return UI.EMPTY_STRING;
   }

   private String getRefreshToken() {

      if (_useActivePerson) {
         return SuuntoTokensRetrievalHandler.getRefreshToken_ActivePerson();
      } else if (_useAllPeople) {
         return SuuntoTokensRetrievalHandler.getRefreshToken_AllPeople();
      }
      return UI.EMPTY_STRING;
   }

   @Override
   public List<TourTypeFilter> getTourTypeFilters() {

      return new ArrayList<>();
   }

   private WorkoutUpload initializeWorkoutUpload(final TourData tourData) {

      final JSONObject body = new JSONObject();
      body.put("description", tourData.getTourTitle()); //$NON-NLS-1$
      body.put("comment", tourData.getTourDescription()); //$NON-NLS-1$

      final HttpRequest request = HttpRequest.newBuilder()
            .uri(OAuth2Utils.createOAuthPasseurUri("/suunto/workout/upload"))//$NON-NLS-1$
            .header(OAuth2Constants.AUTHORIZATION, OAuth2Constants.BEARER + getAccessToken())
            .timeout(Duration.ofMinutes(5))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

      final HttpResponse<String> response = sendAsyncRequest(tourData, request);
      if (response == null) {
         return null;
      }

      return serializeWorkoutUpload(response);
   }

   @Override
   protected boolean isReady() {

      _useActivePerson = SuuntoTokensRetrievalHandler.isReady_ActivePerson();

      _useAllPeople = false;
      if (!_useActivePerson) {
         _useAllPeople = SuuntoTokensRetrievalHandler.isReady_AllPeople();
      }

      return StringUtils.hasContent(getAccessToken() + getRefreshToken());
   }

   private void logVendorError(final String exceptionMessage, final TourData tour) {

      TourLogManager.subLog_ERROR(NLS.bind(
            Messages.Log_UploadWorkoutsToSuunto_002_RetrievalError,
            TourManager.getTourDateTimeShort(tour),
            exceptionMessage));
   }

   private void processTours(final List<TourData> selectedTours,
                             final Map<String, TourData> toursToUpload,
                             final IProgressMonitor monitor) {

      for (final TourData tourData : selectedTours) {

         if (monitor.isCanceled()) {
            return;
         }

         toursToUpload.put(createTourFile_Fit(tourData), tourData);
      }
   }

   private HttpResponse<String> sendAsyncRequest(final TourData tourData, final HttpRequest request) {

      HttpResponse<String> response = null;
      try {

         response = OAuth2Utils.httpClient.send(request, BodyHandlers.ofString());

         if (response.statusCode() != HttpURLConnection.HTTP_OK &&
               response.statusCode() != HttpURLConnection.HTTP_CREATED) {
            logVendorError(response.body(), tourData);
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return response;
   }

   private WorkoutUpload serializeWorkoutUpload(final HttpResponse<String> response) {

      WorkoutUpload workoutUpload = null;
      try {
         workoutUpload = new ObjectMapper().readValue(response.body(), WorkoutUpload.class);
      } catch (final IllegalArgumentException | JsonProcessingException e) {
         StatusUtil.log(e);
      }
      return workoutUpload;
   }

   private void uploadFitFile(final TourData tourData,
                              final String tourAbsoluteFilePath,
                              final WorkoutUpload workoutUpload) {

      HttpRequest request = null;
      try {
         request = HttpRequest.newBuilder()
               .uri(URI.create(workoutUpload.getUrl()))
               .header("x-ms-blob-type", workoutUpload.getHeaders().getXMsBlobType()) //$NON-NLS-1$
               .header(OAuth2Constants.CONTENT_TYPE, "application/octet-stream") //$NON-NLS-1$
               .timeout(Duration.ofMinutes(5))
               .PUT(HttpRequest.BodyPublishers.ofFile(Paths.get(tourAbsoluteFilePath)))
               .build();
      } catch (final FileNotFoundException e) {
         StatusUtil.log(e);
      }

      sendAsyncRequest(tourData, request);
   }

   /**
    * https://apizone.suunto.com/how-to-workout-upload
    *
    * @param tourAbsoluteFilePath
    * @param tourData
    * @return
    */
   private WorkoutUpload uploadTour(final String tourAbsoluteFilePath,
                                    final TourData tourData) {

      // Initialize workout upload
      WorkoutUpload workoutUpload = initializeWorkoutUpload(tourData);

      if (workoutUpload == null) {
         return null;
      }

      // Upload FIT file
      uploadFitFile(tourData, tourAbsoluteFilePath, workoutUpload);

      // Check upload status
      while (workoutUpload != null &&
            (StringUtils.isNullOrEmpty(workoutUpload.getStatus()) ||
                  workoutUpload.getStatus().equalsIgnoreCase("new"))) { //$NON-NLS-1$

         workoutUpload = checkUploadStatus(tourData, workoutUpload.getId());
      }

      return workoutUpload;
   }

   @Override
   public void uploadTours(final List<TourData> selectedTours) {

      final int numberOfTours = selectedTours.size();
      final int[] numberOfUploadedTours = new int[1];

      final IRunnableWithProgress runnable = new IRunnableWithProgress() {

         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            monitor.beginTask(NLS.bind(Messages.Dialog_UploadWorkoutsToSuunto_Task, numberOfTours), numberOfTours * 2);

            monitor.subTask(Messages.Dialog_ValidatingSuuntoTokens_SubTask);

            if (!SuuntoTokensRetrievalHandler.getValidTokens(_useActivePerson, _useAllPeople)) {
               TourLogManager.log_ERROR(LOG_CLOUDACTION_INVALIDTOKENS);
               return;
            }

            monitor.subTask(NLS.bind(Messages.Dialog_UploadWorkoutsToSuunto_SubTask, UI.SYMBOL_HOURGLASS_WITH_FLOWING_SAND, UI.EMPTY_STRING));

            final Map<String, TourData> toursToUpload = new HashMap<>();
            processTours(selectedTours, toursToUpload, monitor);

            if (monitor.isCanceled()) {
               deleteTemporaryTourFiles(toursToUpload);
            }

            monitor.subTask(NLS.bind(Messages.Dialog_UploadWorkoutsToSuunto_SubTask,
                  UI.SYMBOL_WHITE_HEAVY_CHECK_MARK,
                  UI.SYMBOL_HOURGLASS_WITH_FLOWING_SAND));

            numberOfUploadedTours[0] = uploadTours(toursToUpload, monitor);

            monitor.worked(toursToUpload.size());

            monitor.subTask(NLS.bind(Messages.Dialog_UploadWorkoutsToSuunto_SubTask,
                  UI.SYMBOL_WHITE_HEAVY_CHECK_MARK,
                  UI.SYMBOL_WHITE_HEAVY_CHECK_MARK));
         }
      };

      try {
         final long start = System.currentTimeMillis();

         TourLogManager.showLogView();
         TourLogManager.log_TITLE(NLS.bind(Messages.Log_UploadWorkoutsToSuunto_001_Start, numberOfTours));

         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, runnable);

         TourLogManager.log_TITLE(String.format(LOG_CLOUDACTION_END, (System.currentTimeMillis() - start) / 1000.0));

         MessageDialog.openInformation(
               Display.getDefault().getActiveShell(),
               Messages.Dialog_UploadWorkoutsToSuunto_Title,
               NLS.bind(Messages.Dialog_UploadToursToSuunto_Message, numberOfUploadedTours[0], numberOfTours - numberOfUploadedTours[0]));

      } catch (final InvocationTargetException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }
   }

   private int uploadTours(final Map<String, TourData> tours,
                           final IProgressMonitor monitor) {

      int numberOfUploadedTours = 0;
      for (final Map.Entry<String, TourData> tourToUpload : tours.entrySet()) {

         if (monitor.isCanceled()) {
            return 0;
         }

         final String compressedTourAbsoluteFilePath = tourToUpload.getKey();
         final TourData tourData = tourToUpload.getValue();

         final WorkoutUpload workoutUpload = uploadTour(compressedTourAbsoluteFilePath, tourData);

         if (workoutUpload == null ||
               workoutUpload.getStatus().equalsIgnoreCase("error")) { //$NON-NLS-1$

            final String message = workoutUpload == null ? UI.EMPTY_STRING : workoutUpload.getMessage();
            final String workoutId = workoutUpload == null ? UI.EMPTY_STRING : workoutUpload.getId();

            TourLogManager.log_ERROR(NLS.bind(
                  Messages.Log_UploadWorkoutsToSuunto_004_UploadError,
                  new Object[] { TourManager.getTourDateTimeShort(tourData), workoutId, message }));

         } else {

            TourLogManager.log_OK(NLS.bind(
                  Messages.Log_UploadWorkoutsToSuunto_003_UploadStatus,
                  TourManager.getTourDateTimeShort(tourData),
                  workoutUpload.getId()));

            ++numberOfUploadedTours;
         }
      }

      deleteTemporaryFitFiles(tours);

      return numberOfUploadedTours;
   }
}
