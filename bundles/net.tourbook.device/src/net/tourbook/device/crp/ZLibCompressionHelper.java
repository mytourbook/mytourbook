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
import java.util.zip.InflaterInputStream;

import net.tourbook.common.util.StatusUtil;

public class ZLibCompressionHelper {

   private ZLibCompressionHelper() {}

   public static void decompress(final File compressedSourceFile, final File rawDestinationFile) {

      try (InputStream inputStream =
            new InflaterInputStream(new FileInputStream(compressedSourceFile));
            OutputStream outputStream = new FileOutputStream(rawDestinationFile)) {

         final byte[] buffer = new byte[1000];
         int length;

         while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
         }

      } catch (final IOException e) {
         StatusUtil.log(e);
      }
   }
}
