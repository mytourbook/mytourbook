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
import java.io.InputStream;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.tourbook.common.UI;

public final class FileUtils {

   private FileUtils() {}

   /**
    * @param in
    *           input stream
    * @param default_encoding
    *           default encoding. null or "" => system default
    * @return file encoding..
    * @throws IOException
    */
   ////////////////////////////////////////////////////////////////////////////////
   //
   // ADOBE SYSTEMS INCORPORATED
   // Copyright 2003-2007 Adobe Systems Incorporated
   // All Rights Reserved.
   //
   // NOTICE: Adobe permits you to use, modify, and distribute this file
   // in accordance with the terms of the license agreement accompanying it.
   //
   ////////////////////////////////////////////////////////////////////////////////
   public static final String consumeBOM(final InputStream in, final String default_encoding) throws IOException {

      return consumeBOM(in, default_encoding, false);
   }

   /**
    * @param in
    *           input stream
    * @param default_encoding
    *           default encoding. null or "" => system default
    * @param alwaysConsumeBOM
    *           If true, then consume the UTF-16 BOM.
    *           If false use the previous behavior that consumes
    *           a UTF-8 BOM but not a UTF-16 BOM.
    *           This flag is useful when reading a file into
    *           a string that is then passed to a parser. The parser may
    *           not know to strip out the BOM.
    * @return file encoding..
    * @throws IOException
    */
   ////////////////////////////////////////////////////////////////////////////////
   //
   // ADOBE SYSTEMS INCORPORATED
   // Copyright 2003-2007 Adobe Systems Incorporated
   // All Rights Reserved.
   //
   // NOTICE: Adobe permits you to use, modify, and distribute this file
   // in accordance with the terms of the license agreement accompanying it.
   //
   ////////////////////////////////////////////////////////////////////////////////
   private static final String consumeBOM(final InputStream in,
                                          final String default_encoding,
                                          final boolean alwaysConsumeBOM) throws IOException {
      in.mark(3);
      // Determine file encoding...
      // ASCII - no header (use the supplied encoding)
      // UTF8  - EF BB BF
      // UTF16 - FF FE or FE FF (decoder chooses endian-ness)
      if (in.read() == 0xef && in.read() == 0xbb && in.read() == 0xbf) {

         // UTF-8 reader does not consume BOM, so do not reset
         if (System.getProperty("flex.platform.CLR") != null) //$NON-NLS-1$
         {
            return "UTF8"; //$NON-NLS-1$
         } else {
            return "UTF-8"; //$NON-NLS-1$
         }

      } else {

         in.reset();
         final int b0 = in.read();
         final int b1 = in.read();

         if (b0 == 0xff && b1 == 0xfe || b0 == 0xfe && b1 == 0xff) {

            // If we don't consume the BOM is its assumed a
            // UTF-16 reader will consume BOM
            if (!alwaysConsumeBOM) {
               in.reset();
            }

            if (System.getProperty("flex.platform.CLR") != null) //$NON-NLS-1$
            {
               return "UTF16"; //$NON-NLS-1$
            } else {
               return "UTF-16"; //$NON-NLS-1$
            }

         } else {

            // no BOM found
            in.reset();
            if (default_encoding != null && default_encoding.length() != 0) {
               return default_encoding;
            } else {
               return System.getProperty("file.encoding"); //$NON-NLS-1$
            }
         }
      }
   }

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
