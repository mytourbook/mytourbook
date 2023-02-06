/*******************************************************************************
 * Copyright (C) 2021, 2022 Frédéric Bard
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

package net.tourbook.common.util;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.tourbook.common.UI;

public final class FilesUtils {

   private FilesUtils() {}

   public static String createTemporaryFile(final String fileName, final String extension) {

      String absoluteFilePath = UI.EMPTY_STRING;

      try {
         deleteIfExists(Paths.get(fileName + UI.SYMBOL_DOT + extension));

         absoluteFilePath = Files.createTempFile(fileName, UI.SYMBOL_DOT + extension).toString();

      } catch (final IOException e) {
         StatusUtil.log(e);
      }

      return absoluteFilePath;
   }

   public static void deleteIfExists(final Path filePath) {

      try {
         Files.deleteIfExists(filePath);
      } catch (final IOException e) {
         StatusUtil.log(e);
      }
   }

   public static String readFileContentString(final String filePath) {

      String fileContent = UI.EMPTY_STRING;
      try {

         fileContent = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);

      } catch (final MalformedInputException e) {

         try {

            fileContent = Files.readString(Paths.get(filePath), StandardCharsets.ISO_8859_1);

         } catch (final Exception iso_8859_1_exception) {
            StatusUtil.log(iso_8859_1_exception);
         }

      } catch (final IOException ioException) {
         StatusUtil.log(ioException);
      }

      return fileContent;
   }

   public static String removeExtensions(String fileName) {

      final int extensionPosition = fileName.lastIndexOf('.');
      if (extensionPosition != -1) {
         fileName = fileName.substring(0, extensionPosition);
      }
      return fileName;
   }
}
