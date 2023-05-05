/*******************************************************************************
 * Copyright (C) 2018, 2021 Frédéric Bard
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Suunto9_DeviceDataReader extends TourbookDevice {

   /**
    * This field MUST be accessed only, when {@link ImportState_Process#isJUnitTest()} == true, it
    * is NOT save for the concurrent tour import
    */
   private HashMap<TourData, List<TimeData>> _processedActivities = new HashMap<>();

   @Override
   public String buildFileNameFromRawData(final String rawDataFileName) {
      return null;
   }

   @Override
   public boolean checkStartSequence(final int byteIndex, final int newByte) {
      return true;
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
   String GetJsonContentFromGZipFile(final String gzipFilePath, final boolean isValidatingFile) {

      String jsonFileContent = null;

      try (FileInputStream fileInputStream = new FileInputStream(gzipFilePath);
            final GZIPInputStream gzip = new GZIPInputStream(fileInputStream);
            final InputStreamReader inputStreamReader = new InputStreamReader(gzip);
            final BufferedReader br = new BufferedReader(inputStreamReader)) {

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

      if (StringUtils.isNullOrEmpty(jsonFileContent)) {
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

      return false;
   }

   @Override
   public void processDeviceData(final String importFilePath,
                                 final DeviceData deviceData,
                                 final Map<Long, TourData> alreadyImportedTours,
                                 final Map<Long, TourData> newlyImportedTours,
                                 final ImportState_File importState_File,
                                 final ImportState_Process importState_Process) {

      new Suunto9_DeviceData(

            importFilePath,
            alreadyImportedTours,
            newlyImportedTours,

            _processedActivities,

            importState_File,
            importState_Process,

            this

      ).processFile();

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
