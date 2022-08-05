/*******************************************************************************
 * Copyright (C) 2020, 2022 Frédéric Bard
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import net.tourbook.common.util.FilesUtils;
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

   /**
    * Compares a test transaction against a control transaction.
    *
    * @param testTourData
    *           The generated test TourData object.
    * @param controlFileName
    *           The control's file name.
    */
   public static void compareTourDataAgainstControl(final TourData testTourData,
                                                    final String controlFileName) {

      testTourData.getTourMarkersSorted();
      final String testJson = testTourData.toJson();

      final ArrayValueMatcher<Object> arrValMatch = new ArrayValueMatcher<>(new CustomComparator(
            JSONCompareMode.STRICT,
            new Customization("tourMarkers[*].deviceLapTime", (o1, o2) -> true), //$NON-NLS-1$
            new Customization("tourMarkers[*].tourData", (o1, o2) -> true))); //$NON-NLS-1$

      final Customization arrayValueMatchCustomization = new Customization("tourMarkers", arrValMatch); //$NON-NLS-1$
      final CustomComparator customArrayValueComparator = new CustomComparator(
            JSONCompareMode.STRICT,
            arrayValueMatchCustomization,
            new Customization("importFilePath", (o1, o2) -> true), //$NON-NLS-1$
            new Customization("importFilePathName", (o1, o2) -> true), //$NON-NLS-1$
            new Customization("importFilePathNameText", (o1, o2) -> true), //$NON-NLS-1$
            new Customization("tourId", (o1, o2) -> true)); //$NON-NLS-1$

      final String controlDocument = readFileContent(controlFileName + JSON);

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

   public static String readFileContent(final String controlDocumentFileName) {

      final String controlDocumentFilePath = utils.FilesUtils.getAbsoluteFilePath(controlDocumentFileName);

      return FilesUtils.readFileContentString(controlDocumentFilePath);
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
