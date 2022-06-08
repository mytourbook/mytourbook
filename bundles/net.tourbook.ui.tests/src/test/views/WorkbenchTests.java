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
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
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

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2013   1").expand()
            .getNode("May   1").expand().select().getNode("18").select();
      assertNotNull(tour);

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

      final SWTBotMenu toolsMenu = bot.menu("Tools ");
      final SWTBotMenu tourSegmenterMenu = toolsMenu.menu("Tour Segmenter").click();
      assertNotNull(tourSegmenterMenu);
      final SWTBotView tourSegmenterView = bot.viewByTitle("Tour Segmenter");
      assertNotNull(tourSegmenterView);
      tourSegmenterView.show();

      final SWTBotMenu tourAnalyzerMenu = toolsMenu.menu("Tour Analyzer").click();
      assertNotNull(tourAnalyzerMenu);
      final SWTBotView tourAnalyzerView = bot.viewByTitle("Tour Analyzer");
      assertNotNull(tourAnalyzerView);
      tourAnalyzerView.show();

      final SWTBotMenu compareGeoTourMenu = toolsMenu.menu("Compare Geo Tour").click();
      assertNotNull(compareGeoTourMenu);
      final SWTBotView geoCompareView = bot.viewByTitle("Geo Compare");
      assertNotNull(geoCompareView);
      geoCompareView.show();

      final SWTBotMenu tourChartSmoothingMenu = toolsMenu.menu("Tour Chart Smoothing").click();
      assertNotNull(tourChartSmoothingMenu);
      final SWTBotView tourChartSmoothingView = bot.viewByTitle("Tour Chart Smoothing");
      assertNotNull(tourChartSmoothingView);
      tourChartSmoothingView.show();

      final SWTBotMenu statisticsValuesMenu = toolsMenu.menu("Statistic Values").click();
      assertNotNull(statisticsValuesMenu);
      final SWTBotView statisticsValuesView = bot.viewByTitle("Statistic Values");
      assertNotNull(statisticsValuesView);
      statisticsValuesView.show();

      final SWTBotMenu trainingMenu = toolsMenu.menu("Training").click();
      assertNotNull(trainingMenu);
      final SWTBotView trainingView = bot.viewByTitle("Training");
      assertNotNull(trainingView);
      trainingView.show();

      final SWTBotMenu conconiTestMenu = toolsMenu.menu("Conconi Test").click();
      assertNotNull(conconiTestMenu);
      final SWTBotView conconiTestView = bot.viewByTitle("Conconi Test");
      assertNotNull(conconiTestView);
      conconiTestView.show();

      final SWTBotMenu heartRateVariabilityMenu = toolsMenu.menu("Heart Rate Variability").click();
      assertNotNull(heartRateVariabilityMenu);
      final SWTBotView heartRateVariabilityView = bot.viewByTitle("Heart Rate Variability");
      assertNotNull(heartRateVariabilityView);
      heartRateVariabilityView.show();

      bot.menu("Directory").menu("Sensor").click();
      final SWTBotView sensorView = bot.viewByTitle("Sensor");
      assertNotNull(sensorView);
      sensorView.show();

      bot.menu("Directory").menu("Sensor Chart").click();
      final SWTBotView sensorChartView = bot.viewByTitle("Sensor Chart");
      assertNotNull(sensorChartView);
      sensorChartView.show();
   }
}
