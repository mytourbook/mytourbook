/*******************************************************************************
 * Copyright (C) 2023, 2025 Frédéric Bard
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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Paths;
import java.time.Duration;
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
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
            Activator.getImageDescriptor(CloudImages.Cloud_Suunto_Logo));
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

   private WorkoutUpload initializeWorkoutUpload(final TourData tourData) {

      final JSONObject body = new JSONObject();
      body.put("description", tourData.getTourTitle()); //$NON-NLS-1$
      body.put("comment", tourData.getTourDescription()); //$NON-NLS-1$

      final HttpRequest request = HttpRequest.newBuilder()
            .uri(OAuth2Utils.createOAuthPasseurUri("/suunto/workout/upload"))//$NON-NLS-1$
            .header("Content-Type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
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

      Display.getDefault().asyncExec(() -> TourLogManager.subLog_ERROR(NLS.bind(
            Messages.Log_UploadWorkoutsToSuunto_002_RetrievalError,
            TourManager.getTourDateTimeShort(tour),
            exceptionMessage)));
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
               .uri(URI.create(workoutUpload.url()))
               .header("x-ms-blob-type", workoutUpload.headers().xmsBlobType()) //$NON-NLS-1$
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
    *
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
            (StringUtils.isNullOrEmpty(workoutUpload.status()) ||
                  workoutUpload.status().equalsIgnoreCase("new"))) { //$NON-NLS-1$

         workoutUpload = checkUploadStatus(tourData, workoutUpload.id());
      }

      return workoutUpload;
   }

   @Override
   public void uploadTours(final List<TourData> selectedTours) {

      final int numberOfTours = selectedTours.size();
      final int[] numberOfUploadedTours = new int[1];
      final String[] notificationText = new String[1];

      final Job job = new Job(NLS.bind(Messages.Dialog_UploadWorkoutsToSuunto_Task, numberOfTours)) {

         @Override
         public IStatus run(final IProgressMonitor monitor) {

            monitor.beginTask(UI.EMPTY_STRING, numberOfTours * 2);

            monitor.subTask(Messages.Dialog_ValidatingSuuntoTokens_SubTask);

            if (!SuuntoTokensRetrievalHandler.getValidTokens(_useActivePerson, _useAllPeople)) {
               Display.getDefault().asyncExec(() -> TourLogManager.log_ERROR(LOG_CLOUDACTION_INVALIDTOKENS));
               notificationText[0] = LOG_CLOUDACTION_INVALIDTOKENS;
               return Status.CANCEL_STATUS;
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

            monitor.done();

            return Status.OK_STATUS;
         }
      };

      final long start = System.currentTimeMillis();

      TourLogManager.log_TITLE(NLS.bind(Messages.Log_UploadWorkoutsToSuunto_001_Start, numberOfTours));

      job.setPriority(Job.INTERACTIVE);
      job.schedule();
      TourLogManager.log_TITLE(String.format(LOG_CLOUDACTION_END, (System.currentTimeMillis() - start) / 1000.0));

      job.addJobChangeListener(new JobChangeAdapter() {
         @Override
         public void done(final IJobChangeEvent event) {

            if (!PlatformUI.isWorkbenchRunning()) {
               return;
            }

            PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {

               final String infoText = event.getResult().isOK()
                     ? NLS.bind(Messages.Dialog_UploadWorkoutsToSuunto_Message,
                           numberOfUploadedTours[0],
                           numberOfTours - numberOfUploadedTours[0])
                     : notificationText[0];

               TourLogManager.log_TITLE(String.format(LOG_CLOUDACTION_END, (System.currentTimeMillis() - start) / 1000.0));

               UI.openNotificationPopup(Messages.Dialog_UploadWorkoutsToSuunto_Title,
                     Activator.getImageDescriptor(CloudImages.Cloud_Suunto_Logo),
                     infoText);
            });
         }
      });
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
               workoutUpload.status().equalsIgnoreCase("error")) { //$NON-NLS-1$

            final String message = workoutUpload == null ? UI.EMPTY_STRING : workoutUpload.message();
            final String workoutId = workoutUpload == null ? UI.EMPTY_STRING : workoutUpload.id();

            Display.getDefault().asyncExec(() -> TourLogManager.log_ERROR(NLS.bind(
                  Messages.Log_UploadWorkoutsToSuunto_004_UploadError,
                  new Object[] { TourManager.getTourDateTimeShort(tourData), workoutId, message })));

         } else {

            Display.getDefault().asyncExec(() -> TourLogManager.log_OK(NLS.bind(
                  Messages.Log_UploadWorkoutsToSuunto_003_UploadStatus,
                  TourManager.getTourDateTimeShort(tourData),
                  workoutUpload.id())));

            ++numberOfUploadedTours;
         }
      }

      deleteTemporaryFitFiles(tours);

      return numberOfUploadedTours;
   }
}
