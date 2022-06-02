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
package basic;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

public class BasicTests {

   private SWTBot bot = new SWTBot();

   @BeforeClass
   public static void beforeClass() {

      SWTBotPreferences.TIMEOUT = 10000;
   }

   @Test
   void testOpenPreferences() {

      bot.toolbarButtonWithTooltip("Preferences (Ctrl+Shift+P)").click();
      bot.button("Apply and Close").click();
   }

//   @Test
//   void testStatisticsView() {
//
//      final SWTBotMenu statisticsView = bot.menu("Statistics");
//      assertNotNull(statisticsView);
//   }

   @Test
   void testTourMarkerView() {

      // This is the equivalent of
      // bot.viewByTitle("Calendar").show();
      // for SWTBot.
      bot.menu("Tour Editor");

      final String march2020 = "2020   3";
      bot.tree().getTreeItem(march2020).expand();
      final String maySecond = "May   2";
      bot.tree().getTreeItem(march2020).getNode(maySecond).expand();
      bot.tree().getTreeItem(march2020).getNode(maySecond).select();
      bot.tree().getTreeItem(march2020).getNode(maySecond).getNode("23").select();
      final SWTBotMenu tourMarkerView = bot.menu("Tour Markers");
      assertNotNull(tourMarkerView);
      bot.table().select(0);
      bot.table().select(1);
   }
}
