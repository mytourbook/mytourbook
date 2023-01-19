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
package views;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class WorkbenchTests extends UITest {

   public static final String TOUR_PROPERTIES = "2. Tour Properties"; //$NON-NLS-1$
   public static final String COMPARE_TOURS   = "4. Compare Tours";   //$NON-NLS-1$
   public static final String PHOTO           = "5. Photo";           //$NON-NLS-1$

   @BeforeClass
   public static void beforeClass() {

      SWTBotPreferences.TIMEOUT = 10000;
   }

   @Disabled
   //Disabled because of this error
   //com.badlogic.gdx.utils.GdxRuntimeException: Couldn't load shared library 'vtm-jni64.dll' for target: Windows Server 2022, 64-bit
   //My hunch is that the build machine has no 3D graphics capabilities
   @Test
   void open25DMap() {

      Utils.showViewFromMenu(bot, "Map", "2.5D Tour Map"); //$NON-NLS-1$ //$NON-NLS-2$
      final SWTBotView twoFiveDMapView = Utils.showView(bot, "2.5D Tour Map"); //$NON-NLS-1$
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      twoFiveDMapView.close();
   }

   @Disabled
   //Disabled because of this error
   //java.lang.UnsatisfiedLinkError: Can't load library: D:\a\mytourbook-BUILD-autocreated\core\net.tourbook.ui.tests\natives\windows-amd64\\gluegen_rt.dll
   //My hunch is that the build machine has no 3D graphics capabilities
   @Test
   void open3DMap() {

      Utils.showViewFromMenu(bot, "Map", "3D Tour Map"); //$NON-NLS-1$ //$NON-NLS-2$
      final SWTBotView threeDMapView = Utils.showView(bot, "3D Tour Map"); //$NON-NLS-1$
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      threeDMapView.close();
   }

   @Test
   void testOpenViews() {

      //Select a tour so that the selected views contain information
      Utils.showTourBookView(bot);

      Utils.getTourWithSRTM(bot);

      bot.toolbarButtonWithTooltip("Tour Import (Ctrl+Shift+I)").click(); //$NON-NLS-1$
      Utils.showView(bot, "Tour Import"); //$NON-NLS-1$

      bot.toolbarButtonWithTooltip("Shows tour in 2D map").click(); //$NON-NLS-1$
      Utils.showView(bot, "2D Tour Map"); //$NON-NLS-1$
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      final SWTBotView tourLogView = Utils.showView(bot, "Tour Log"); //$NON-NLS-1$

      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.TOUR_PROPERTIES).expand().getNode("Waypoints").select(); //$NON-NLS-1$
      bot.button("Open").click(); //$NON-NLS-1$
      final SWTBotView waypointsView = Utils.showView(bot, "Waypoints"); //$NON-NLS-1$

      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.TOUR_PROPERTIES).expand().getNode("Tour Data").select(); //$NON-NLS-1$
      bot.button("Open").click(); //$NON-NLS-1$
      final SWTBotView tourDataView = Utils.showView(bot, "Tour Data"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, Utils.TOOLS, "Tour Analyzer"); //$NON-NLS-1$
      final SWTBotView tourAnalyzerView = Utils.showView(bot, "Tour Analyzer"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, Utils.TOOLS, "Compare Geo Tour"); //$NON-NLS-1$
      final SWTBotView geoCompareView = Utils.showView(bot, "Geo Compare"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, Utils.TOOLS, "Tour Chart Smoothing"); //$NON-NLS-1$
      final SWTBotView tourChartSmoothingView = Utils.showView(bot, "Tour Chart Smoothing"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, Utils.TOOLS, "Statistic Values"); //$NON-NLS-1$
      final SWTBotView statisticValuesView = Utils.showView(bot, "Statistic Values"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, Utils.TOOLS, "Training"); //$NON-NLS-1$
      final SWTBotView trainingView = Utils.showView(bot, "Training"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, Utils.TOOLS, "Heart Rate Variability"); //$NON-NLS-1$
      final SWTBotView heartRateVariabilityView = Utils.showView(bot, "Heart Rate Variability"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, Utils.DIRECTORY, "Photos"); //$NON-NLS-1$
      final SWTBotView photosView = Utils.showView(bot, "Photos"); //$NON-NLS-1$
      //Sleeping 3 seconds as the view can be slow to display
      bot.sleep(3000);

      bot.toolbarButtonWithTooltip("Search for tours, marker and waypoints (Ctrl+K)").click(); //$NON-NLS-1$
      final SWTBotView searchToursView = Utils.showView(bot, "Search Tours"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, Utils.DIRECTORY, "Tour Marker"); //$NON-NLS-1$
      final SWTBotView tourMarkerView = Utils.showView(bot, "Tour Marker"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, Utils.DIRECTORY, "Collated Tours"); //$NON-NLS-1$
      final SWTBotView collatedToursView = Utils.showView(bot, "Collated Tours"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, Utils.DIRECTORY, "Reference Tours"); //$NON-NLS-1$
      final SWTBotView referenceToursView = Utils.showView(bot, "Reference Tours"); //$NON-NLS-1$

//      Utils.showViewFromMenu(bot, "Help", "Error Log"); //$NON-NLS-1$ //$NON-NLS-2$
//      bot.sleep(3000);
//      Utils.showView(bot, "Error Log"); //$NON-NLS-1$

      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.COMPARE_TOURS).expand().getNode("Comparison Results").select(); //$NON-NLS-1$
      bot.button("Open").click(); //$NON-NLS-1$
      final SWTBotView comparisonResultsView = Utils.showView(bot, "Comparison Results"); //$NON-NLS-1$

      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.PHOTO).expand().getNode("Photos + Tours").select(); //$NON-NLS-1$
      bot.button("Open").click(); //$NON-NLS-1$
      final SWTBotView photosAndToursView = Utils.showView(bot, "Photos + Tours"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, "Tour", "Tour &Photos"); //$NON-NLS-1$ //$NON-NLS-2$
      final SWTBotView tourPhotosView = Utils.showView(bot, "Tour Photos"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, "Map", "Map &Bookmark"); //$NON-NLS-1$ //$NON-NLS-2$
      final SWTBotView mapBookmarkView = Utils.showView(bot, "Map Bookmark"); //$NON-NLS-1$

      bot.sleep(3000);

      //Close the opened views
      tourLogView.close();
      tourChartSmoothingView.close();
      statisticValuesView.close();
      trainingView.close();
      heartRateVariabilityView.close();
      photosView.close();
      searchToursView.close();
      tourMarkerView.close();
      waypointsView.close();
      tourDataView.close();
      tourAnalyzerView.close();
      geoCompareView.close();
      collatedToursView.close();
      referenceToursView.close();
      comparisonResultsView.close();
      photosAndToursView.close();
      tourPhotosView.close();
      mapBookmarkView.close();
   }
}
