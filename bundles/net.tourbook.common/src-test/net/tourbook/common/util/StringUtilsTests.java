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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tourbook.common.UI;

import org.junit.jupiter.api.Test;

public class StringUtilsTests {

   @Test
   void testHasContent() {

      assertTrue(StringUtils.hasContent("string")); //$NON-NLS-1$
      assertFalse(StringUtils.hasContent(UI.SPACE1));
      assertFalse(StringUtils.hasContent(UI.EMPTY_STRING));
      assertFalse(StringUtils.hasContent(null));
   }

   @Test
   void testJoin() {

      final String[] stringArray = { "1", "2", "3" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      assertEquals("1,2,3", StringUtils.join(stringArray, ",")); //$NON-NLS-1$ //$NON-NLS-2$
      assertEquals("1", StringUtils.join(new String[] { "1" }, ",")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
   }
}
