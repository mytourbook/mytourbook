package net.tourbook.device.suunto;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
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
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

public class Suunto9DeviceDataReader extends TourbookDevice {

   // For Unit testing
   private static final boolean                   UNITTESTS             = false;
   // Make sure that the smoothing value is 10 (speed and gradient)
   public static final String                     IMPORT_FILE_PATH      = "/net/tourbook/device/suunto/testFiles/"; //$NON-NLS-1$
   private static Map<String, String>             testFiles             = new HashMap<>();                          // Java 7
   private HashMap<TourData, ArrayList<TimeData>> _processedActivities  = new HashMap<>();

   private HashMap<Long, TourData>                _newlyImportedTours   = new HashMap<>();
   private HashMap<Long, TourData>                _alreadyImportedTours = new HashMap<>();

   /**
    * Compares a test transaction against a control transaction.
    *
    * @param controlDocument
    *           The control Suunto 9 XML file's content.
    * @param xmlTestDocument
    *           The test Suunto 9 GZip file's content.
    * @return True if no differences were found, false otherwise.
    */
   private static boolean CompareAgainstControl(final String controlDocument, final String xmlTestDocument) {
      final Diff myDiff = DiffBuilder.compare(Input.fromString(controlDocument))
            .withTest(Input.fromString(xmlTestDocument))
            .ignoreWhitespace()
            .build();

      return !myDiff.hasDifferences();
   }

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

   /**
    * Retrieves the content from a resource.
    *
    * @param gzipFilePath
    *           The absolute file path of the Suunto file.
    * @param isZipFile
    *           True if the file is a Zip archive, false otherwise.
    * @return Returns the JSON content of the given resource file,
    */
   private String GetContentFromResource(final String resourceFilePath, final boolean isZipFile) {
      String fileContent = null;
      try {
         final InputStream inputStream =
               Suunto9DeviceDataReader.class.getResourceAsStream(resourceFilePath);

         BufferedReader br = null;
         GZIPInputStream gzip = null;
         if (isZipFile) {
            gzip = new GZIPInputStream(inputStream);
            br = new BufferedReader(new InputStreamReader(gzip));
         } else {
            br = new BufferedReader(new InputStreamReader(inputStream));
         }

         fileContent = br.lines().collect(Collectors.joining());

         // close resources
         br.close();
         if (isZipFile) {
            gzip.close();
         }
      } catch (final IOException e) {
         StatusUtil.log(e);
         return ""; //$NON-NLS-1$
      }

      return fileContent;
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
      try {
         final GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(gzipFilePath));
         final BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

         jsonFileContent = br.readLine();

         // close resources
         br.close();
         gzip.close();

      } catch (final IOException e) {

            /*
             * Log only when reading the zip file, during a validation, an exception can be very
             * likely and should not be displayed
             */
         if (!isValidatingFile) {
            StatusUtil.log(e);
         }

         return ""; //$NON-NLS-1$
      }

      return jsonFileContent;
   }

   /**
    * Used only for unit tests, it retrieves the last processed activity.
    *
    * @return If any, the last processed activity.
    */
   private TourData GetLastTourDataImported() {
      final Iterator<Entry<TourData, ArrayList<TimeData>>> it = _processedActivities.entrySet().iterator();
      TourData lastTourData = null;
      while (it.hasNext()) {
         lastTourData = it.next().getKey();
      }

      return lastTourData;
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
      final BufferedReader fileReader = null;
      try {

         if (jsonFileContent == null ||
               jsonFileContent == "") { //$NON-NLS-1$
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
                  Util.closeReader(fileReader);
                  return true;
               }
            }

         } catch (final JSONException ex) {
            StatusUtil.log(ex);
            return false;
         }

      } catch (

      final Exception e) {
         StatusUtil.log(e);
         return false;
      } finally {
         Util.closeReader(fileReader);
      }

      return false;
   }

   @Override
   public boolean processDeviceData(final String importFilePath,
                                    final DeviceData deviceData,
                                    final HashMap<Long, TourData> alreadyImportedTours,
                                    final HashMap<Long, TourData> newlyImportedTours) {
      if (UNITTESTS) {
         return testSuuntoFiles();
      }

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
      for (final Map.Entry<TourData, ArrayList<TimeData>> entry : _processedActivities.entrySet()) {
         final TourData key = entry.getKey();
         if (key.getTourId() == tourId) {
            return true;
         }
      }

      return false;
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

      final TourData activity = suuntoJsonProcessor.ImportActivity(
            jsonFileContent,
            UNITTESTS);

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
    * Unit tests for the Suunto Spartan/9 import
    *
    * @return True if all the tests were successful, false otherwise.
    */
   public boolean testSuuntoFiles() {

      boolean testResults = true;

      // City of Rocks, ID
      String filePath =
            IMPORT_FILE_PATH + "1537365846902_183010004848_post_timeline-1.json.gz"; //$NON-NLS-1$
      String controlFilePath = IMPORT_FILE_PATH + "1537365846902_183010004848_post_timeline-1.xml"; //$NON-NLS-1$
      testFiles.put(controlFilePath, filePath);

      //Maxwell, CO

      filePath = IMPORT_FILE_PATH +
            "1536723722706_183010004848_post_timeline-1.json.gz"; //$NON-NLS-1$
      controlFilePath =
            IMPORT_FILE_PATH + "1536723722706_183010004848_post_timeline-1.xml"; //$NON-NLS-1$
      testFiles.put(controlFilePath, filePath);

      //Shoreline - with laps/markers
      filePath = IMPORT_FILE_PATH +
            "1555291925128_183010004848_post_timeline-1.json.gz"; //$NON-NLS-1$
      controlFilePath =
            IMPORT_FILE_PATH + "1555291925128_183010004848_post_timeline-1.xml"; //$NON-NLS-1$
      testFiles.put(controlFilePath, filePath);

      // Reservoir Ridge with MoveSense HR belt (R-R data)
      filePath = IMPORT_FILE_PATH +
            "1549250450458_183010004848_post_timeline-1.json.gz"; //$NON-NLS-1$
      controlFilePath =
            IMPORT_FILE_PATH + "1549250450458_183010004848_post_timeline-1.xml"; //$NON-NLS-1$
      testFiles.put(controlFilePath, filePath);

      // SWIMMING

      // Start -> 100m -> LAP -> LAP -> 100m -> LAP -> LAP -> 100m -> LAP -> LAP -> 100m -> Stop
      filePath = IMPORT_FILE_PATH +
            "1547628896209_184710003036_post_timeline-1.json.gz"; //$NON-NLS-1$
      controlFilePath =
            IMPORT_FILE_PATH + "1547628896209_184710003036_post_timeline-1.xml"; //$NON-NLS-1$
      testFiles.put(controlFilePath, filePath);

      // Start -> 100m -> Stop
      filePath = IMPORT_FILE_PATH +
            "1547628897243_184710003036_post_timeline-1.json.gz"; //$NON-NLS-1$
      controlFilePath =
            IMPORT_FILE_PATH + "1547628897243_184710003036_post_timeline-1.xml"; //$NON-NLS-1$
      testFiles.put(controlFilePath, filePath);

      TourData entry;
      String xml;
      String controlFileContent;

      for (final Map.Entry<String, String> testEntry : testFiles.entrySet()) {
         final String jsonFileContent =
               GetContentFromResource(testEntry.getValue(), true);
         ProcessFile(testEntry.getValue(),
               jsonFileContent);
         entry = GetLastTourDataImported();
         xml = entry.toXml();
         controlFileContent = GetContentFromResource(testEntry.getKey(), false);
         testResults &= CompareAgainstControl(controlFileContent, xml);

         // We clear the history so that it doesn't
         //create conflict in the unit tests as we reuse files
         cleanUpActivities();
      }

      return testResults;

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
      final String jsonFileContent = GetJsonContentFromGZipFile(fileName, true);
      return isValidActivity(jsonFileContent);
   }
}
