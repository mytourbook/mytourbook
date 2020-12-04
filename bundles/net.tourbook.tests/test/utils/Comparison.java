/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.byteholder.geoclipse.map.UI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;

import org.skyscreamer.jsonassert.ArrayValueMatcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

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

      // When using Java 11, convert the line below to the Java 11 method
      //String controlDocument = Files.readString(controlDocumentFilePath, StandardCharsets.US_ASCII);

      final String controlDocumentFilePath = Paths.get(controlFileName + JSON).toAbsolutePath().toString();
      final String controlDocument = readFile(controlDocumentFilePath, StandardCharsets.US_ASCII);

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
            new Customization("tourId", (o1, o2) -> true), //$NON-NLS-1$
            new Customization("startTimeOfDay", (o1, o2) -> true)); //$NON-NLS-1$

      final JSONCompareResult result = JSONCompare.compareJSON(controlDocument, testJson, customArrayValueComparator);

      if (result.failed()) {
         writeErroneousFiles(controlFileName, testJson);
      }

      assertEquals(true, result.passed(), result.getMessage());
   }

   private static String readFile(final String path, final Charset encoding) {

      if (StringUtils.isNullOrEmpty(path)) {
         return UI.EMPTY_STRING;
      }

      byte[] encoded = null;
      try {
         encoded = Files.readAllBytes(Paths.get(path));
         return new String(encoded, encoding);
      } catch (final IOException e) {
         e.printStackTrace();
      }

      return UI.EMPTY_STRING;
   }

   public static TourData retrieveImportedTour(final Map<Long, TourData> newlyImportedTours) {
      final Map.Entry<Long, TourData> entry = newlyImportedTours.entrySet().iterator().next();
      return entry.getValue();
   }

   /**
    * Code useful when the tests fail and one wants to be able to compare the expected vs actual
    * file
    *
    * @param controlFileName
    * @param testJson
    */
   private static void writeErroneousFiles(final String controlFileName, final String testJson) {

      final File myFile = new File(
            controlFileName + ".json"); //$NON-NLS-1$

      try (Writer writer = new FileWriter(myFile);
            BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
         bufferedWriter.write(testJson);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }
}
