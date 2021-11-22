/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tourbook.common.UI;

import org.junit.jupiter.api.Test;

public class StringUtilsTests {

   @Test
   void testHasContent() {

      assertTrue(StringUtils.hasContent("string"));
      assertFalse(StringUtils.hasContent(" "));
      assertFalse(StringUtils.hasContent(""));
      assertFalse(StringUtils.hasContent(null));
   }

   @Test
   void testJoin() {

      final String[] stringArray = { "1", "2", "3" };
      assertEquals(StringUtils.join(stringArray, ","), "1,2,3");
      assertEquals(StringUtils.join(new String[] { "1" }, ","), "1");
   }

   @Test
   void testSanitizeFileName() {

      final String fileName = "\\$%#filename.txt";
      assertEquals(StringUtils.sanitizeFileName(fileName), "----filename.txt");
      assertNull(StringUtils.sanitizeFileName(null));
      assertEquals(StringUtils.sanitizeFileName(""), UI.EMPTY_STRING);
   }
}
