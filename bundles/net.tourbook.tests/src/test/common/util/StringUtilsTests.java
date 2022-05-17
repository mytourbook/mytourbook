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
package common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StringUtilsTests {

   private static SWTBot bot;

   @BeforeEach
   public void beforeClass() {
      bot = new SWTBot();
      SWTBotPreferences.TIMEOUT = 20000;
   }

   @Test
   void testOpenMyTourbook() {

      final SWTBotButton myTourbookMenu = bot.button("OK").click();
      assertNotNull(myTourbookMenu);
      final SWTBotButton measurementSystemMenu = bot.button("OK").click();
      assertNotNull(measurementSystemMenu);
      final SWTBotButton peopleMenu = bot.button("Apply and Close").click();
      assertNotNull(peopleMenu);
      final SWTBotButton firstStartMenu = bot.button("OK").click();
      assertNotNull(firstStartMenu);
      bot.menu("New").menu("Exit").click();
   }

   /**
    * File name sanitization with a Windows environment.
    * This test should succeed when run with a Windows machine (Example: Github build)
    * as {@link UI#IS_WIN} will be true
    */
   @Test
   void testSanitizeFileNameWindows() {

      final String fileName = "/\\:*?\"<>|filename#_.txt"; //$NON-NLS-1$
      assertEquals("---------filename#_.txt", StringUtils.sanitizeFileName(fileName)); //$NON-NLS-1$
      assertNull(StringUtils.sanitizeFileName(null));
      assertEquals(UI.EMPTY_STRING, StringUtils.sanitizeFileName(UI.EMPTY_STRING));
   }
}
