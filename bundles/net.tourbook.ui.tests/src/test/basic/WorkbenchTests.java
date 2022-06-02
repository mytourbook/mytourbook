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

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

public class WorkbenchTests {

   private SWTWorkbenchBot bot               = new SWTWorkbenchBot();
   private String          tourBookViewTitle = "Tour Book";

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
      final SWTBotView tourBookView = bot.viewByTitle(tourBookViewTitle);
      assertNotNull(tourBookView);
      tourBookView.show();

      final String twentyThirteen = "2013   1";
      bot.tree().getTreeItem(twentyThirteen).expand();
      final String may = "May   1";
      final SWTBotTreeItem mayNode = bot.tree().getTreeItem(twentyThirteen).getNode(may);
      mayNode.expand();
      mayNode.select();
      mayNode.getNode("18").select();

      final SWTBotView tourTourImportView = bot.viewByTitle("Tour Import");
      assertNotNull(tourTourImportView);
      tourTourImportView.show();

      final SWTBotView tourStatisticsView = bot.viewByTitle("Statistics");
      assertNotNull(tourStatisticsView);
      tourStatisticsView.show();

      final SWTBotView tourCalendarView = bot.viewByTitle("Calendar");
      assertNotNull(tourCalendarView);
      tourCalendarView.show();

      final SWTBotView tour2DMapView = bot.viewByTitle("2D Tour Map");
      assertNotNull(tour2DMapView);
      tour2DMapView.show();
      //Sleeping 3 seconds as the 2D map can be slow to display
      bot.sleep(3000);

      final SWTBotView tourLogView = bot.viewByTitle("Tour Log");
      assertNotNull(tourLogView);
      tourLogView.show();

      final SWTBotView tourWaypointsView = bot.viewByTitle("Waypoints");
      assertNotNull(tourWaypointsView);
      tourWaypointsView.show();

      final SWTBotView tourEditorView = bot.viewByTitle("Tour Editor");
      assertNotNull(tourEditorView);
      tourEditorView.show();

      bot.cTabItem("Time Slices").activate();
      bot.cTabItem("Swim Slices").activate();
   }

   @Test
   void testTourMarkerView() {

      final SWTBotView tourBookView = bot.viewByTitle(tourBookViewTitle);
      assertNotNull(tourBookView);
      tourBookView.show();

      final String march2020 = "2020   3";
      bot.tree().getTreeItem(march2020).expand();
      final String maySecond = "May   2";
      final SWTBotTreeItem marchNode = bot.tree().getTreeItem(march2020).getNode(maySecond);
      marchNode.expand();
      marchNode.select();
      marchNode.getNode("23").select();

      final SWTBotView tourMarkerView = bot.viewByTitle("Tour Markers");
      assertNotNull(tourMarkerView);
      tourMarkerView.show();

      bot.table().select(0);
      bot.table().select(1);
   }

   @Test
   void testTourPausesView() {

      final SWTBotView tourBookView = bot.viewByTitle(tourBookViewTitle);
      assertNotNull(tourBookView);
      tourBookView.show();

      final String march2020 = "2021   2";
      bot.tree().getTreeItem(march2020).expand();
      final String january = "Jan   2";
      final SWTBotTreeItem januaryNode = bot.tree().getTreeItem(march2020).getNode(january);
      januaryNode.expand();
      januaryNode.select();
      januaryNode.getNode("31").select();

      final SWTBotView tourPausesView = bot.viewByTitle("Tour Pauses");
      assertNotNull(tourPausesView);
      tourPausesView.show();

      bot.table().select("15:40");
   }

   @Test
   void testTourTagsView() {

      final SWTBotView tourBookView = bot.viewByTitle("Tour Book");
      assertNotNull(tourBookView);
      tourBookView.show();

      final String twentyTwentyOne = "2021   2";
      bot.tree().getTreeItem(twentyTwentyOne).expand();
      final String january = "Jan   2";
      final SWTBotTreeItem januaryNode = bot.tree().getTreeItem(twentyTwentyOne).getNode(january);
      januaryNode.expand();
      januaryNode.select();
      januaryNode.getNode("31").select();

      final SWTBotView tourTagsView = bot.viewByTitle("Tour Tags");
      assertNotNull(tourTagsView);
      tourTagsView.show();
      // bot.tree().getTreeItem("Shoes 2").select();
   }
}
