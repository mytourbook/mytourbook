/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;

/**
 * String utilities.
 */
public final class StringUtils {

   /**
    * Tests if a given string is not <code>null</code> and not empty.
    *
    * @param text
    * @return <code>true</code> when text is not <code>null</code> and is not empty
    */
   public static boolean hasContent(final String text) {
      return !isNullOrEmpty(text);
   }

   /**
    * Tests if a given string is null or empty.
    *
    * @param string
    * @return <code>true</code> if null or empty, false otherwise.
    */
   public static boolean isNullOrEmpty(final String string) {

      return string == null || string.trim().isEmpty();
   }

   /**
    * @param stringArray
    * @param separator
    * @return Join a strings with a separator.
    */
   public static String join(final String[] stringArray, final String separator) {

      final StringBuilder sb = new StringBuilder();

      for (int arrayIndex = 0; arrayIndex < stringArray.length; arrayIndex++) {

         if (arrayIndex > 0) {
            sb.append(separator);
         }

         sb.append(stringArray[arrayIndex]);
      }

      return sb.toString();
   }

   /**
    * Sanitizes a file name string by replacing any illegal characters
    * with a '-'.
    *
    * @param fileName
    *           The string of a given file name
    * @return
    *         The sanitized file name
    */
   public static String sanitizeFileName(final String fileName) {

      if (StringUtils.isNullOrEmpty(fileName)) {
         return fileName;
      }

      return fileName.replaceAll("[^a-zA-Z0-9 \\.\\-]", UI.DASH); //$NON-NLS-1$
   }
}
