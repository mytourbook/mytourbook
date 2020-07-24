/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.device.suunto;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Suunto9DeviceDataReader extends TourbookDevice {

   // Make sure that the smoothing value is 10 (speed and gradient)
   public static final String                     IMPORT_FILE_PATH      = "/net/tourbook/device/suunto/testFiles/"; //$NON-NLS-1$
   private HashMap<TourData, ArrayList<TimeData>> _processedActivities  = new HashMap<>();

   private HashMap<Long, TourData>                _newlyImportedTours   = new HashMap<>();
   private HashMap<Long, TourData>                _alreadyImportedTours = new HashMap<>();

   @Override
   public String buildFileNameFromRawData(final String rawDataFileName) {
      // NEXT Auto-generated method stub
      return null;
   }

   @Override
   public boolean checkStartSequence(final int byteIndex, final int newByte) {
      return true;
   }

   /**
    * At the end of the import process, we clean the accumulated data so that the next round will
    * begin clean.
    */
   private void cleanUpActivities() {
      _processedActivities.clear();
   }

   @Override
   public String getDeviceModeName(final int profileId) {
      return UI.EMPTY_STRING;
   }

   /**
    * Retrieves the JSON content from a GZip Suunto file.
    *
    * @param gzipFilePath
    *           The absolute file path of the Suunto file.
    * @param isValidatingFile
    * @return Returns the JSON content.
    */
   private String GetJsonContentFromGZipFile(final String gzipFilePath, final boolean isValidatingFile) {
      
      String jsonFileContent = null;
      try (FileInputStream fis = new FileInputStream(gzipFilePath);
            GZIPInputStream gzip = new GZIPInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(gzip))) {

         jsonFileContent = br.readLine();

      } catch (final IOException e) {

         /*
          * Log only when reading the zip file, during a validation, an exception can be very
          * likely and should not be displayed
          */
         if (!isValidatingFile) {
            StatusUtil.log(e);
         }

         return UI.EMPTY_STRING;
      }

      return jsonFileContent;
   }

   @Override
   public SerialParameters getPortParameters(final String portName) {
      return null;
   }

   @Override
   public int getStartSequenceSize() {
      return 0;
   }

   @Override
   public int getTransferDataSize() {
      return -1;
   }

   /**
    * Checks if the file is a valid Suunto Spartan/9 activity.
    *
    * @param jsonFileContent
    *           The content to check.
    * @return Returns <code>true</code> when the file contains content of a valid activity.
    */
   protected boolean isValidActivity(final String jsonFileContent) {

      try {

         if (jsonFileContent == null ||
               jsonFileContent == UI.EMPTY_STRING) {
            return false;
         }

         try {
            final JSONObject jsonContent = new JSONObject(jsonFileContent);
            final JSONArray samples = (JSONArray) jsonContent.get(SuuntoJsonProcessor.TAG_SAMPLES);

            for (int index = 0; index < samples.length(); ++index) {
               final String currentSample = samples.getJSONObject(index).toString();
               if (currentSample.contains(SuuntoJsonProcessor.TAG_SAMPLE) &&
                     (currentSample.contains(SuuntoJsonProcessor.TAG_GPSALTITUDE) ||
                           currentSample.contains(SuuntoJsonProcessor.TAG_LONGITUDE) ||
                           currentSample.contains(SuuntoJsonProcessor.TAG_LATITUDE) ||
                           currentSample.contains(SuuntoJsonProcessor.TAG_ALTITUDE))) {
                  return true;
               }
            }

         } catch (final JSONException ex) {
            StatusUtil.log(ex);
            return false;
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
         return false;
      }

      return false;
   }

   @Override
   public boolean processDeviceData(final String importFilePath,
                                    final DeviceData deviceData,
                                    final HashMap<Long, TourData> alreadyImportedTours,
                                    final HashMap<Long, TourData> newlyImportedTours) {

      _newlyImportedTours = newlyImportedTours;
      _alreadyImportedTours = alreadyImportedTours;

      // When a new import is started, we need to clean the previous saved activities
      if (newlyImportedTours.size() == 0 && alreadyImportedTours.size() == 0) {
         cleanUpActivities();
      }

      final String jsonFileContent =
            GetJsonContentFromGZipFile(importFilePath, false);

      return ProcessFile(importFilePath, jsonFileContent);
   }

   /**
    * Checks if an activity has already been processed.
    *
    * @param tourId
    *           The tour ID of the activity.
    * @return True if the activity has already been processed, false otherwise.
    */
   private boolean processedActivityExists(final long tourId) {

      return _processedActivities.entrySet().stream().anyMatch(entry -> entry.getKey().getTourId() == tourId);
   }

   /**
    * For a given Suunto activity file, the function processes it and imports it as a tour.
    *
    * @param filePath
    *           The absolute full path of a given activity.
    * @param jsonFileContent
    *           The JSON content of the activity file.
    * @return The Suunto activity as a tour.
    */
   private boolean ProcessFile(final String filePath, final String jsonFileContent) {

      final SuuntoJsonProcessor suuntoJsonProcessor = new SuuntoJsonProcessor();

      String fileName =
            FilenameUtils.removeExtension(filePath);

      if (fileName.substring(fileName.length() - 5, fileName.length()) == ".json") { //$NON-NLS-1$
         fileName = FilenameUtils.removeExtension(fileName);
      }

      final TourData activity = suuntoJsonProcessor.ImportActivity(jsonFileContent);

      if (activity == null) {
         return false;
      }

      final String uniqueId = this.createUniqueId(activity, Util.UNIQUE_ID_SUFFIX_SUUNTO9);
      activity.createTourId(uniqueId);

      if (!processedActivityExists(activity.getTourId())) {
         _processedActivities.put(activity, suuntoJsonProcessor.getSampleList());
      }

      activity.setImportFilePath(filePath);

      TryFinalizeTour(activity);

      return true;
   }

   /**
    * Attempting to finalize an activity. If it doesn't contain a tourId, it is not a final
    * activity.
    *
    * @param tourData
    *           The tour to finalize.
    */
   private void TryFinalizeTour(final TourData tourData) {

      tourData.setDeviceId(deviceId);

      long tourId;
      try {

         tourId = tourData.getTourId();

      } catch (final NullPointerException e) {
         StatusUtil.log(e);
         tourId = -1;
      }

      if (tourId != -1) {
         // check if the tour is already imported
         if (_alreadyImportedTours.containsKey(tourId)) {
            _alreadyImportedTours.remove(tourId);
         }

         // add new tour to other tours
         if (_newlyImportedTours.containsKey(tourId)) {
            _newlyImportedTours.remove(tourId);
         }
         _newlyImportedTours.put(tourId, tourData);

         tourData.setDeviceName(SuuntoJsonProcessor.DeviceName);

         tourData.computeAltitudeUpDown();
         tourData.computeTourDrivingTime();
         tourData.computeComputedValues();
      }
   }

   @Override
   public boolean validateRawData(final String fileName) {
      if (!fileName.toLowerCase().endsWith(".json.gz")) { //$NON-NLS-1$
         return false;
      }

      final String jsonFileContent = GetJsonContentFromGZipFile(fileName, true);
      return isValidActivity(jsonFileContent);
   }
}
