/*******************************************************************************
 * Copyright (C) 2020, 2024 Frédéric Bard
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
package utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import net.tourbook.common.UI;
import net.tourbook.common.util.FileUtils;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;

import org.skyscreamer.jsonassert.ArrayValueMatcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

public class Comparison {

   private static final String JSON = ".json"; //$NON-NLS-1$

   private Comparison() {}

   public static void compareFitAgainstControl(final String controlTourFilePathFit,
                                               final String testTourFilePathFit) {

      //Convert the test FIT file to CSV for a human readable comparison
      convertFitToCsvFile(testTourFilePathFit);

      final String testTourFilePathCsv = testTourFilePathFit.replace(".fit", ".csv"); //$NON-NLS-1$ //$NON-NLS-2$
      final Path testTourAbsoluteFilePathCsv = Paths.get(utils.FilesUtils.getAbsoluteFilePath(testTourFilePathCsv));
      assertTrue(Files.exists(testTourAbsoluteFilePathCsv));

      final String controlTourFilePathCsv = controlTourFilePathFit.replace(".fit", ".csv"); //$NON-NLS-1$ //$NON-NLS-2$
      final Path controlTourAbsoluteFilePathCsv = Paths.get(utils.FilesUtils.getAbsoluteFilePath(controlTourFilePathCsv));
      assertTrue(Files.exists(controlTourAbsoluteFilePathCsv));

      try {

         final List<String> testFileContentArray = Files.readAllLines(testTourAbsoluteFilePathCsv, StandardCharsets.UTF_8);
         final List<String> controlFileContentArray = Files.readAllLines(controlTourAbsoluteFilePathCsv, StandardCharsets.UTF_8);

         // Modify the test and control files to ignore the software version
         final String genericSoftwareVersion = "software_version,"; //$NON-NLS-1$
         final String genericApplicationVersion = "application_version,"; //$NON-NLS-1$

         controlFileContentArray.replaceAll(line -> line = line.replace("software_version,\"24.1\"", genericSoftwareVersion)); //$NON-NLS-1$
         controlFileContentArray.replaceAll(line -> line = line.replace("application_version,\"2410\"", genericApplicationVersion)); //$NON-NLS-1$

         testFileContentArray.replaceAll(line -> line.replaceFirst("software_version,\"\\d\\d\\.\\d\"", genericSoftwareVersion)); //$NON-NLS-1$
         testFileContentArray.replaceAll(line -> line.replaceFirst("application_version,\"\\d\\d\\d\\d\"", genericApplicationVersion)); //$NON-NLS-1$

         // Modify the session/activity messages to remove/ignore their creation timestamps
         // since it will be different at every test run
         String timeCreatedData = "Data,0,activity,num_sessions,";

         List<String> timeCreatedLine = controlFileContentArray.stream().filter(s -> s.startsWith(timeCreatedData))
               .toList();
         // Retrieve the value of "time_created"
         String controlTimeCreatedValue = timeCreatedLine.get(0).split(",")[7];
         //Replace all the values by an empty string
         controlFileContentArray.replaceAll(line -> line = line.replace(
               controlTimeCreatedValue,
               UI.EMPTY_STRING));

         timeCreatedLine = testFileContentArray.stream().filter(s -> s.startsWith(timeCreatedData)).toList();
         // Retrieve the value of "time_created"
         String testTimeCreatedValue = timeCreatedLine.get(0).split(",")[7];
         //Replace all the values by an empty string
         testFileContentArray.replaceAll(line -> line = line.replace(
               testTimeCreatedValue,
               UI.EMPTY_STRING));

         //Compare with the control file
         if (!controlFileContentArray.equals(testFileContentArray)) {

            final String testFileContent = String.join(UI.SYSTEM_NEW_LINE, testFileContentArray);
            writeErroneousFiles(controlTourFilePathCsv.replace(".csv", "-GeneratedFromTests.csv"), testFileContent); //$NON-NLS-1$ //$NON-NLS-2$
         }
         assertLinesMatch(controlFileContentArray, testFileContentArray);

      } catch (final IOException e) {
         StatusUtil.log(e);
      }
   }

   /**
    * Compares a test tour against a control tour.
    *
    * @param testTourData
    *           The generated test TourData object.
    * @param controlFileName
    *           The control's file name.
    */
   public static void compareTourDataAgainstControl(final TourData testTourData,
                                                    final String controlFileName) {

      final ArrayValueMatcher<Object> tourMarkersValueMatcher = new ArrayValueMatcher<>(
            new CustomComparator(
                  JSONCompareMode.STRICT,
                  new Customization("tourMarkers[*].altitude", (o1, o2) -> true), //$NON-NLS-1$
                  new Customization("tourMarkers[*].distance20", (o1, o2) -> true), //$NON-NLS-1$
                  new Customization("tourMarkers[*].serieIndex", (o1, o2) -> true), //$NON-NLS-1$
                  new Customization("tourMarkers[*].time", (o1, o2) -> true), //$NON-NLS-1$
                  new Customization("tourMarkers[*].tourTime", (o1, o2) -> true))); //$NON-NLS-1$

      final CustomComparator customArrayValueComparator = new CustomComparator(
            JSONCompareMode.STRICT,
            new Customization("tourMarkers", tourMarkersValueMatcher), //$NON-NLS-1$
            new Customization("tourType.createId", (o1, o2) -> true), //$NON-NLS-1$
            new Customization("tourId", (o1, o2) -> true)); //$NON-NLS-1$

      final String controlDocument = readFileContent(controlFileName + JSON);

      testTourData.getTourMarkersSorted();
      final String testJson = convertTourDataToJson(testTourData);

      final JSONCompareResult result = JSONCompare.compareJSON(controlDocument, testJson, customArrayValueComparator);

      if (result.failed()) {
         writeErroneousFiles(controlFileName + "-GeneratedFromTests" + JSON, testJson); //$NON-NLS-1$
      }

      assertTrue(result.passed(), result.getMessage());
   }

   public static void compareXmlAgainstControl(final String controlTourFilePath,
                                               final String testTourFilePath,
                                               final List<String> nodesToFilter,
                                               final List<String> attributesToFilter) {

      final String controlTour = Comparison.readFileContent(controlTourFilePath);
      final String testTour = Comparison.readFileContent(testTourFilePath);

      final DiffBuilder documentDiffBuilder = DiffBuilder
            .compare(controlTour)
            .withTest(testTour)
            .ignoreWhitespace();

      if (!nodesToFilter.isEmpty()) {
         documentDiffBuilder.withNodeFilter(node -> !nodesToFilter.contains(node.getNodeName()));
      }

      if (!attributesToFilter.isEmpty()) {
         documentDiffBuilder.withAttributeFilter(attribute -> !attributesToFilter.contains(attribute.getName()));
      }

      final Diff documentDiff = documentDiffBuilder.build();

      if (documentDiff.hasDifferences()) {
         writeErroneousFiles(controlTourFilePath, testTour);
      }

      assertFalse(documentDiff.hasDifferences(), documentDiff.toString());
   }

   private static void convertFitToCsvFile(final String fitFilePath) {

      final File fileToConvert = new File(fitFilePath);

      final String fitCsvToolFilePath = FilesUtils.getAbsoluteFilePath(
            FilesUtils.rootPath + "utils/files/FitCSVTool.jar"); //$NON-NLS-1$

      final ProcessBuilder processBuilder = new ProcessBuilder(
            "java", //$NON-NLS-1$
            "-jar", //$NON-NLS-1$
            fitCsvToolFilePath,
            fileToConvert.getAbsolutePath());
      try {
         final Process process = processBuilder.start();
         process.waitFor();

      } catch (final IOException | InterruptedException e) {
         Thread.currentThread().interrupt();
         StatusUtil.log(e);
      }
   }

   private static String convertTourDataToJson(final TourData tourData) {

      final ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.setSerializationInclusion(Include.NON_NULL);
      objectMapper.setSerializationInclusion(Include.NON_EMPTY);
      objectMapper.setConfig(objectMapper.getSerializationConfig()
            .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
      objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
      objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.NONE);
      objectMapper.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
      objectMapper.setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);
      objectMapper.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);

      String jsonString = UI.EMPTY_STRING;
      try {
         jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tourData);
      } catch (final JsonProcessingException e) {
         e.printStackTrace();
      }
      return jsonString;
   }

   public static String readFileContent(final String controlDocumentFileName) {

      final String controlDocumentFilePath = utils.FilesUtils.getAbsoluteFilePath(controlDocumentFileName);

      return FileUtils.readFileContentString(controlDocumentFilePath);
   }

   public static TourData retrieveImportedTour(final Map<Long, TourData> newlyImportedTours) {

      return newlyImportedTours.entrySet().iterator().next().getValue();
   }

   /**
    * Code useful when the tests fail and one wants to be able to compare the expected vs actual
    * file
    *
    * @param controlFilePath
    * @param testContent
    */
   private static void writeErroneousFiles(final String controlFilePath, final String testContent) {

      final File myFile = new File(controlFilePath);

      try (Writer writer = new FileWriter(myFile);
            BufferedWriter bufferedWriter = new BufferedWriter(writer)) {

         bufferedWriter.write(testContent);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }
}
