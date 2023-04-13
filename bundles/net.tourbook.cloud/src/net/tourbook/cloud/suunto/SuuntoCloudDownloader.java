/*******************************************************************************
 * Copyright (C) 2021, 2023 Frédéric Bard
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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.cloud.Activator;
import net.tourbook.cloud.CloudImages;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.suunto.workouts.Payload;
import net.tourbook.cloud.suunto.workouts.Workouts;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.download.TourbookCloudDownloader;
import net.tourbook.tour.TourLogManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class SuuntoCloudDownloader extends TourbookCloudDownloader {

   private static final String     LOG_CLOUDACTION_END           = net.tourbook.cloud.Messages.Log_CloudAction_End;
   private static final String     LOG_CLOUDACTION_INVALIDTOKENS = net.tourbook.cloud.Messages.Log_CloudAction_InvalidTokens;

   private static IPreferenceStore _prefStore                    = Activator.getDefault().getPreferenceStore();
   private int[]                   _numberOfAvailableTours;

   private boolean                 _useActivePerson;
   private boolean                 _useAllPeople;

   public SuuntoCloudDownloader() {

      super("SUUNTO", //$NON-NLS-1$
            Messages.VendorName_Suunto,
            Messages.Import_Data_HTML_SuuntoWorkoutsDownloader_Tooltip,
            Activator.getImageAbsoluteFilePath(CloudImages.Cloud_Suunto));
   }

   private CompletableFuture<WorkoutDownload> downloadFile(final Payload workoutPayload) {

      final HttpRequest request = HttpRequest.newBuilder()
            .uri(OAuth2Utils.createOAuthPasseurUri("/suunto/workout/exportFit?workoutKey=" + workoutPayload.workoutKey))//$NON-NLS-1$
            .header(OAuth2Constants.AUTHORIZATION, OAuth2Constants.BEARER + getAccessToken())
            .GET()
            .build();

      return sendAsyncRequest(workoutPayload, request);
   }

   private int downloadFiles(final List<Payload> newWorkouts, final IProgressMonitor monitor) {

      final List<CompletableFuture<WorkoutDownload>> workoutDownloads = new ArrayList<>();

      newWorkouts.stream().forEach(workout -> {
         if (monitor.isCanceled()) {
            return;
         } else {
            workoutDownloads.add(downloadFile(workout));
         }
      });

      final int[] numberOfDownloadedTours = new int[1];
      workoutDownloads.stream().map(CompletableFuture::join).forEach(workoutDownload -> {
         if (monitor.isCanceled()) {
            return;
         } else if (logDownloadResult(workoutDownload)) {
            ++numberOfDownloadedTours[0];
         }
      });

      return numberOfDownloadedTours[0];
   }

   @Override
   public void downloadTours() {

      _useActivePerson = SuuntoTokensRetrievalHandler.isDownloadReady_ActivePerson();

      _useAllPeople = false;
      if (!_useActivePerson) {
         _useAllPeople = SuuntoTokensRetrievalHandler.isDownloadReady_AllPeople();
      }

      if (!_useActivePerson && !_useAllPeople) {

         final int returnResult = PreferencesUtil.createPreferenceDialogOn(
               Display.getCurrent().getActiveShell(),
               PrefPageSuunto.ID,
               null,
               null).open();

         if (returnResult != 0) {// The OK button was not clicked or if the configuration is still not ready
            return;
         }
      }

      _numberOfAvailableTours = new int[1];
      final int[] numberOfDownloadedTours = new int[1];

      final IRunnableWithProgress runnable = new IRunnableWithProgress() {

         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            monitor.beginTask(Messages.Dialog_DownloadWorkoutsFromSuunto_Task, 2);

            monitor.subTask(Messages.Dialog_ValidatingSuuntoTokens_SubTask);

            if (!SuuntoTokensRetrievalHandler.getValidTokens(_useActivePerson, _useAllPeople)) {
               TourLogManager.log_ERROR(LOG_CLOUDACTION_INVALIDTOKENS);
               return;
            }

            if (StringUtils.isNullOrEmpty(getDownloadFolder())) {
               TourLogManager.log_ERROR(Messages.Log_DownloadWorkoutsFromSuunto_004_NoSpecifiedFolder);
               return;
            }

            monitor.subTask(NLS.bind(Messages.Dialog_DownloadWorkoutsFromSuunto_SubTask,
                  new Object[] {
                        UI.SYMBOL_HOURGLASS_WITH_FLOWING_SAND,
                        UI.EMPTY_STRING,
                        UI.EMPTY_STRING }));

            //Get the list of workouts
            final Workouts workouts = retrieveWorkoutsList();
            if (workouts.payload.isEmpty()) {
               TourLogManager.log_INFO(Messages.Log_DownloadWorkoutsFromSuunto_002_NewWorkoutsNotFound);
               return;
            }

            final List<Long> tourStartTimes = retrieveAllTourStartTimes();

            //Identifying the workouts that have not yet been imported in the tour database
            final List<Payload> newWorkouts = workouts.payload.stream()
                  .filter(suuntoWorkout -> !tourStartTimes.contains(suuntoWorkout.startTime / 1000L * 1000L))
                  .collect(Collectors.toList());

            final int numNewWorkouts = newWorkouts.size();
            if (numNewWorkouts == 0) {
               TourLogManager.log_INFO(Messages.Log_DownloadWorkoutsFromSuunto_003_AllWorkoutsAlreadyExist);
               return;
            }

            _numberOfAvailableTours[0] = numNewWorkouts;

            monitor.worked(1);

            monitor.subTask(NLS.bind(Messages.Dialog_DownloadWorkoutsFromSuunto_SubTask,
                  new Object[] {
                        UI.SYMBOL_WHITE_HEAVY_CHECK_MARK,
                        _numberOfAvailableTours[0],
                        UI.SYMBOL_HOURGLASS_WITH_FLOWING_SAND }));

            numberOfDownloadedTours[0] = downloadFiles(newWorkouts, monitor);

            monitor.worked(1);
         }
      };

      try {
         final long start = System.currentTimeMillis();

         TourLogManager.showLogView();
         TourLogManager.log_TITLE(Messages.Log_DownloadWorkoutsFromSuunto_001_Start);

         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, runnable);

         TourLogManager.log_TITLE(String.format(LOG_CLOUDACTION_END, (System.currentTimeMillis() - start) / 1000.0));

         MessageDialog.openInformation(
               Display.getDefault().getActiveShell(),
               Messages.Dialog_DownloadWorkoutsFromSuunto_Title,
               NLS.bind(Messages.Dialog_DownloadWorkoutsFromSuunto_Message,
                     numberOfDownloadedTours[0],
                     _numberOfAvailableTours[0] - numberOfDownloadedTours[0]));

      } catch (final InvocationTargetException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

   }

   private String getAccessToken() {

      if (_useActivePerson) {
         return SuuntoTokensRetrievalHandler.getAccessToken_ActivePerson();
      } else if (_useAllPeople) {
         return SuuntoTokensRetrievalHandler.getAccessToken_AllPeople();
      }

      return UI.EMPTY_STRING;
   }

   private String getDownloadFolder() {

      if (_useActivePerson) {
         return SuuntoTokensRetrievalHandler.getDownloadFolder_ActivePerson();
      } else if (_useAllPeople) {
         return SuuntoTokensRetrievalHandler.getDownloadFolder_AllPeople();
      }

      return UI.EMPTY_STRING;
   }

   private boolean getSuuntoUseWorkoutFilterEndDate() {

      if (_useActivePerson) {
         return _prefStore.getBoolean(Preferences.getSuuntoUseWorkoutFilterEndDate_Active_Person_String());
      } else if (_useAllPeople) {
         return _prefStore.getBoolean(Preferences.getPerson_SuuntoUseWorkoutFilterEndDate_String(UI.EMPTY_STRING));
      }

      return false;
   }

   private boolean getSuuntoUseWorkoutFilterStartDate() {

      if (_useActivePerson) {
         return _prefStore.getBoolean(Preferences.getSuuntoUseWorkoutFilterStartDate_Active_Person_String());
      } else if (_useAllPeople) {
         return _prefStore.getBoolean(Preferences.getPerson_SuuntoUseWorkoutFilterStartDate_String(UI.EMPTY_STRING));
      }

      return false;
   }

   private long getSuuntoWorkoutFilterEndDate() {

      if (_useActivePerson) {
         return _prefStore.getLong(Preferences.getSuuntoWorkoutFilterEndDate_Active_Person_String());
      } else if (_useAllPeople) {
         return _prefStore.getLong(Preferences.getPerson_SuuntoWorkoutFilterEndDate_String(UI.EMPTY_STRING));
      }

      return 0;
   }

   private long getSuuntoWorkoutFilterStartDate() {

      if (_useActivePerson) {
         return _prefStore.getLong(Preferences.getSuuntoWorkoutFilterStartDate_Active_Person_String());
      } else if (_useAllPeople) {
         return _prefStore.getLong(Preferences.getPerson_SuuntoWorkoutFilterStartDate_String(UI.EMPTY_STRING));
      }

      return 0;
   }

   @Override
   protected boolean isReady() {
      return false;
   }

   private boolean logDownloadResult(final WorkoutDownload workoutDownload) {

      boolean isTourDownloaded = false;

      if (workoutDownload.isSuccessfullyDownloaded()) {

         isTourDownloaded = true;

         TourLogManager.log_OK(
               NLS.bind(Messages.Log_DownloadWorkoutsFromSuunto_005_DownloadStatus,
                     workoutDownload.getWorkoutKey(),
                     workoutDownload.getAbsoluteFilePath()));
      } else {
         TourLogManager.log_ERROR(workoutDownload.getError());
      }

      return isTourDownloaded;
   }

   /**
    * Retrieves a list of all the tour start times currently in the database the current person
    * (if not "All People" is selected).
    */
   private List<Long> retrieveAllTourStartTimes() {

      final List<Long> tourStartTimes = new ArrayList<>();
      try (Connection conn = TourDatabase.getInstance().getConnection();
            Statement stmt = conn.createStatement()) {

         final StringBuilder sqlQuery = new StringBuilder("SELECT tourStartTime FROM " + TourDatabase.TABLE_TOUR_DATA); //$NON-NLS-1$

         final TourPerson activePerson = TourbookPlugin.getActivePerson();

         if (activePerson != null) {
            sqlQuery.append(" WHERE TOURPERSON_PERSONID = " + activePerson.getPersonId()); //$NON-NLS-1$
         }

         final ResultSet result = stmt.executeQuery(sqlQuery.toString());

         while (result.next()) {

            tourStartTimes.add(result.getLong(1));
         }

      } catch (final SQLException e) {
         SQL.showException(e);
      }
      return tourStartTimes;
   }

   private Workouts retrieveWorkoutsList() {

      final StringBuilder queryParameters = new StringBuilder();

      if (getSuuntoUseWorkoutFilterStartDate()) {

         final long startDateFilter = getSuuntoWorkoutFilterStartDate();
         queryParameters.append("since=" + startDateFilter); //$NON-NLS-1$
      }
      if (getSuuntoUseWorkoutFilterEndDate()) {

         final long endDateFilter = getSuuntoWorkoutFilterEndDate();

         if (StringUtils.hasContent(queryParameters.toString())) {
            queryParameters.append('&');
         }

         queryParameters.append("until=" + endDateFilter); //$NON-NLS-1$
      }

      if (StringUtils.hasContent(queryParameters.toString())) {
         queryParameters.insert(0, '?');
      }

      final URI oAuthPasseurAppUri = OAuth2Utils.createOAuthPasseurUri("/suunto/workouts" + queryParameters); //$NON-NLS-1$

      try {

         final HttpRequest request = HttpRequest.newBuilder()
               .uri(oAuthPasseurAppUri)
               .header(OAuth2Constants.AUTHORIZATION, OAuth2Constants.BEARER + getAccessToken())
               .GET()
               .build();

         final HttpResponse<String> response = OAuth2Utils.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK && StringUtils.hasContent(response.body())) {

            return new ObjectMapper().readValue(response.body(), Workouts.class);
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return new Workouts();
   }

   private CompletableFuture<WorkoutDownload> sendAsyncRequest(final Payload workoutPayload,
                                                               final HttpRequest request) {

      final CompletableFuture<WorkoutDownload> workoutDownload =
            OAuth2Utils.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
            .thenApply(response -> writeFileToFolder(workoutPayload, response))
            .exceptionally(e -> {
               final WorkoutDownload erroneousDownload = new WorkoutDownload(workoutPayload.workoutKey);
               erroneousDownload.setError(NLS.bind(Messages.Log_DownloadWorkoutsFromSuunto_007_Error,
                     erroneousDownload.getWorkoutKey(),
                     e.getMessage()));
               erroneousDownload.setSuccessfullyDownloaded(false);
               return erroneousDownload;
            });

      return workoutDownload;
   }

   private WorkoutDownload writeFileToFolder(final Payload workoutPayload,
                                             final HttpResponse<InputStream> response) {

      final WorkoutDownload workoutDownload = new WorkoutDownload(workoutPayload.workoutKey);

      final Optional<String> contentDisposition =
            response.headers().firstValue("Content-Disposition"); //$NON-NLS-1$

      String suuntoFileName = UI.EMPTY_STRING;
      if (contentDisposition.isPresent()) {
         suuntoFileName = contentDisposition
               .get()
               .replaceFirst("(?i)^.*filename=\"([^\"]+)\".*$", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
      }

      final String customizedFileName =
            CustomFileNameBuilder.buildCustomizedFileName(workoutPayload, suuntoFileName);

      final Path filePath = Paths.get(
            getDownloadFolder(),
            StringUtils.sanitizeFileName(customizedFileName));
      workoutDownload.setAbsoluteFilePath(filePath.toAbsolutePath().toString());

      if (filePath.toFile().exists()) {

         workoutDownload.setError(
               NLS.bind(
                     Messages.Log_DownloadWorkoutsFromSuunto_006_FileAlreadyExists,
                     workoutDownload.getWorkoutKey(),
                     filePath.toAbsolutePath().toString()));
         return workoutDownload;
      }

      try (InputStream inputStream = response.body();
            FileOutputStream fileOutputStream = new FileOutputStream(filePath.toFile())) {

         int inputStreamByte;
         while ((inputStreamByte = inputStream.read()) != -1) {
            fileOutputStream.write(inputStreamByte);
         }

      } catch (final IOException e) {
         StatusUtil.log(e);
         workoutDownload.setError(e.getMessage());
         return workoutDownload;
      }

      workoutDownload.setSuccessfullyDownloaded(true);

      return workoutDownload;
   }
}
