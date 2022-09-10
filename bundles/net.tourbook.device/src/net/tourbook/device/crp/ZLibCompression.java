/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.device.crp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.zip.InflaterInputStream;

import net.tourbook.common.UI;

public class ZLibCompression {

   private static void copy(final InputStream inputStream,
                            final OutputStream outputStream) throws IOException {

      final byte[] buffer = new byte[1000];
      int length;

      while ((length = inputStream.read(buffer)) > 0) {
         outputStream.write(buffer, 0, length);
      }
   }

   public static void decompress(final File compressed, final File raw)
         throws IOException {

      try (InputStream inputStream =
            new InflaterInputStream(new FileInputStream(compressed));
            OutputStream outputStream = new FileOutputStream(raw)) {

         copy(inputStream, outputStream);
      }
   }

   public static String decompressToString(final File compressedFile) throws IOException {

      try (InputStream inputStream =
            new InflaterInputStream(new FileInputStream(compressedFile))) {

         return toString(inputStream);
      }
   }

   private static String toString(final InputStream inputStream) {

      try (Scanner scanner = new Scanner(inputStream).useDelimiter("\\A")) { //$NON-NLS-1$

         return scanner.hasNext() ? scanner.next() : UI.EMPTY_STRING;
      }
   }

}
