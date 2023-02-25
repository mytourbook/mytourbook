/*******************************************************************************
 * Copyright (C) 2022, 2023 Frédéric Bard
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
package common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;

import org.junit.jupiter.api.Test;

public class StringUtilsTests {

   /**
    * File name sanitization with a Windows environment.
    * This test should succeed when run with a Windows machine (Example: Github build)
    * as {@link UI#IS_WIN} will be true
    */
   @Test
   void testSanitizeFileNameWindows() {

      final String fileName = "/\\:*?\"<>|filename#_.txt"; //$NON-NLS-1$

      String expectedValue = "---------filename#_.txt";
      if (UI.IS_LINUX) {
         expectedValue = "-\\:*?\"<>|filename#_.txt";
      }

      assertEquals(expectedValue, StringUtils.sanitizeFileName(fileName));
      assertNull(StringUtils.sanitizeFileName(null));
      assertEquals(UI.EMPTY_STRING, StringUtils.sanitizeFileName(UI.EMPTY_STRING));
   }
}
