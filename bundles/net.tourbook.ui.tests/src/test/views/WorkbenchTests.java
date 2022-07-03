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
package views;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

import utils.Utils;

public class WorkbenchTests {

   private static final String DIRECTORY = "Directory";
   private static final String TOOLS     = "Tools ";
   private SWTWorkbenchBot     bot       = new SWTWorkbenchBot();

   @BeforeClass
   public static void beforeClass() {

      SWTBotPreferences.TIMEOUT = 10000;
   }

   @Test
   void testOpenPreferences() {

      bot.toolbarButtonWithTooltip("Preferences (Ctrl+Shift+P)").click();
      bot.button("Apply and Close").click();
   }

   @Test
   void testOpenViews() {

      //Select a tour so that the selected views contain information
      Utils.showTourBookView(bot);

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2013   1").expand()
            .getNode("May   1").expand().select().getNode("18").select();
      assertNotNull(tour);

      Utils.showView(bot, "Tour Import");

      Utils.showView(bot, "Statistics");

      Utils.showView(bot, "Calendar");

      Utils.showView(bot, "2D Tour Map");
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      Utils.showViewFromMenu(bot, "Map", "2.5D Tour Map");
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      Utils.showViewFromMenu(bot, "Map", "3D Tour Map");
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      Utils.showView(bot, "Tour Log");

      Utils.showView(bot, "Waypoints");

      Utils.showView(bot, "Tour Editor");

      Utils.showViewFromMenu(bot, TOOLS, "Tour Segmenter");
      Utils.showView(bot, "Tour Segmenter");

      Utils.showViewFromMenu(bot, TOOLS, "Tour Analyzer");
      Utils.showView(bot, "Tour Analyzer");

      Utils.showViewFromMenu(bot, TOOLS, "Compare Geo Tour");
      Utils.showView(bot, "Geo Compare");

      Utils.showViewFromMenu(bot, TOOLS, "Tour Chart Smoothing");
      Utils.showView(bot, "Tour Chart Smoothing");

      Utils.showViewFromMenu(bot, TOOLS, "Statistic Values");
      Utils.showView(bot, "Statistic Values");

      Utils.showViewFromMenu(bot, TOOLS, "Training");
      Utils.showView(bot, "Training");

      Utils.showViewFromMenu(bot, TOOLS, "Conconi Test");
      Utils.showView(bot, "Conconi Test");

      Utils.showViewFromMenu(bot, TOOLS, "Heart Rate Variability");
      Utils.showView(bot, "Heart Rate Variability");

      Utils.showViewFromMenu(bot, DIRECTORY, "Sensor");
      Utils.showView(bot, "Sensor");

      Utils.showViewFromMenu(bot, DIRECTORY, "Sensor Chart");
      Utils.showView(bot, "Sensor Chart");

      Utils.showViewFromMenu(bot, DIRECTORY, "Photos");
      Utils.showView(bot, "Photos");
      //Sleeping 3 seconds as the view can be slow to display
      bot.sleep(3000);
   }
}
