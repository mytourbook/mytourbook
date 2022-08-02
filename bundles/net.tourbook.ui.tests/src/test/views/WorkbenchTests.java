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

   private static final String DIRECTORY = "Directory"; //$NON-NLS-1$
   private static final String TOOLS     = "Tools "; //$NON-NLS-1$
   private SWTWorkbenchBot     bot       = new SWTWorkbenchBot();

   @BeforeClass
   public static void beforeClass() {

      SWTBotPreferences.TIMEOUT = 10000;
   }

   @Test
   void testOpenPreferences() {

      bot.toolbarButtonWithTooltip("Preferences (Ctrl+Shift+P)").click(); //$NON-NLS-1$
      bot.button("Apply and Close").click(); //$NON-NLS-1$
   }

   @Test
   void testOpenViews() {

      //Select a tour so that the selected views contain information
      Utils.showTourBookView(bot);

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2013   1").expand() //$NON-NLS-1$
            .getNode("May   1").expand().select().getNode("18").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      Utils.showView(bot, "Tour Import"); //$NON-NLS-1$

      Utils.showView(bot, "Statistics"); //$NON-NLS-1$

      Utils.showView(bot, "Calendar"); //$NON-NLS-1$

      Utils.showView(bot, "2D Tour Map"); //$NON-NLS-1$
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      Utils.showViewFromMenu(bot, "Map", "2.5D Tour Map"); //$NON-NLS-1$ //$NON-NLS-2$
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      Utils.showViewFromMenu(bot, "Map", "3D Tour Map"); //$NON-NLS-1$ //$NON-NLS-2$
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      Utils.showView(bot, "Tour Log"); //$NON-NLS-1$

      Utils.showView(bot, "Waypoints"); //$NON-NLS-1$

      Utils.showView(bot, "Tour Editor"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, TOOLS, "Tour Segmenter"); //$NON-NLS-1$
      Utils.showView(bot, "Tour Segmenter"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, TOOLS, "Tour Analyzer"); //$NON-NLS-1$
      Utils.showView(bot, "Tour Analyzer"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, TOOLS, "Compare Geo Tour"); //$NON-NLS-1$
      Utils.showView(bot, "Geo Compare"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, TOOLS, "Tour Chart Smoothing"); //$NON-NLS-1$
      Utils.showView(bot, "Tour Chart Smoothing"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, TOOLS, "Statistic Values"); //$NON-NLS-1$
      Utils.showView(bot, "Statistic Values"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, TOOLS, "Training"); //$NON-NLS-1$
      Utils.showView(bot, "Training"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, TOOLS, "Conconi Test"); //$NON-NLS-1$
      Utils.showView(bot, "Conconi Test"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, TOOLS, "Heart Rate Variability"); //$NON-NLS-1$
      Utils.showView(bot, "Heart Rate Variability"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, DIRECTORY, "Sensor"); //$NON-NLS-1$
      Utils.showView(bot, "Sensor"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, DIRECTORY, "Sensor Chart"); //$NON-NLS-1$
      Utils.showView(bot, "Sensor Chart"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, DIRECTORY, "Photos"); //$NON-NLS-1$
      Utils.showView(bot, "Photos"); //$NON-NLS-1$
      //Sleeping 3 seconds as the view can be slow to display
      bot.sleep(3000);
   }
}
