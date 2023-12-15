/*******************************************************************************
 * Copyright (C) 2022, 2024 Frédéric Bard
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

   public static final String TOUR_DIRECTORIES = "1. Tour Directories"; //$NON-NLS-1$
   public static final String TOUR_PROPERTIES  = "2. Tour Properties";  //$NON-NLS-1$
   public static final String COMPARE_TOURS    = "4. Compare Tours";    //$NON-NLS-1$
   public static final String PHOTO            = "5. Photo";            //$NON-NLS-1$
   public static final String SYSTEM           = "99. System";          //$NON-NLS-1$

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

      Utils.showViewFromMenu(bot, "Map", Utils.VIEW_NAME_TOURMAP25); //$NON-NLS-1$
      final SWTBotView twoFiveDMapView = Utils.showView(bot, Utils.VIEW_NAME_TOURMAP25);
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

      Utils.showViewFromMenu(bot, "Map", Utils.VIEW_NAME_TOURMAP3); //$NON-NLS-1$
      final SWTBotView threeDMapView = Utils.showView(bot, Utils.VIEW_NAME_TOURMAP3);
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      threeDMapView.close();
   }

   @Test
   void testOpenViews() {

      //Select a tour so that the selected views contain information
      Utils.showTourBookView(bot);

      Utils.getTourWithSRTM(bot);

      bot.toolbarButtonWithTooltip("Show tour in 2D map").click(); //$NON-NLS-1$
      Utils.showView(bot, "2D Tour Map"); //$NON-NLS-1$
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      //Commenting because of this error
      //java.lang.UnsatisfiedLinkError: Can't load library: D:\a\mytourbook-BUILD-autocreated\core\net.tourbook.ui.tests\natives\windows-amd64\\gluegen_rt.dll
      // Utils.showViewFromMenu(bot, "Map", "3D Tour Map"); //$NON-NLS-1$ //$NON-NLS-2$
      //My hunch is that the build machine has no 3D graphics capabilities
      //Sleeping 3 seconds as the map can be slow to display
      //bot.sleep(3000);

      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.TOUR_PROPERTIES).expand().getNode(Utils.VIEW_NAME_WAYPOINTS).select();
      bot.button("Open").click(); //$NON-NLS-1$
      final SWTBotView waypointsView = Utils.showView(bot, Utils.VIEW_NAME_WAYPOINTS);

      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.TOUR_PROPERTIES).expand().getNode(Utils.VIEW_NAME_TOURDATA).select();
      bot.button("Open").click(); //$NON-NLS-1$
      final SWTBotView tourDataView = Utils.showView(bot, Utils.VIEW_NAME_TOURDATA);

      Utils.showViewFromMenu(bot, Utils.TOOLS, "Compare Geo Tour"); //$NON-NLS-1$
      final SWTBotView geoCompareView = Utils.showView(bot, Utils.VIEW_NAME_GEOCOMPARE);

      Utils.showViewFromMenu(bot, Utils.TOOLS, Utils.VIEW_NAME_TOURCHARTSMOOTHING);
      final SWTBotView tourChartSmoothingView = Utils.showView(bot, Utils.VIEW_NAME_TOURCHARTSMOOTHING);

      Utils.showViewFromMenu(bot, Utils.TOOLS, Utils.VIEW_NAME_STATISTICVALUES);
      final SWTBotView statisticValuesView = Utils.showView(bot, Utils.VIEW_NAME_STATISTICVALUES);

      Utils.showViewFromMenu(bot, Utils.TOOLS, Utils.VIEW_NAME_TRAINING);
      final SWTBotView trainingView = Utils.showView(bot, Utils.VIEW_NAME_TRAINING);

      Utils.showViewFromMenu(bot, Utils.TOOLS, Utils.VIEW_NAME_HEARTRATEVARIABILITY);
      final SWTBotView heartRateVariabilityView = Utils.showView(bot, Utils.VIEW_NAME_HEARTRATEVARIABILITY);

      Utils.showViewFromMenu(bot, Utils.DIRECTORY, "Photos"); //$NON-NLS-1$
      final SWTBotView photosView = Utils.showView(bot, "Photos"); //$NON-NLS-1$
      //Sleeping 3 seconds as the view can be slow to display
      bot.sleep(3000);

      bot.toolbarButtonWithTooltip("Search for tours, marker and waypoints (Ctrl+K)").click(); //$NON-NLS-1$
      final SWTBotView searchToursView = Utils.showView(bot, Utils.VIEW_NAME_SEARCHALL);

      Utils.showViewFromMenu(bot, Utils.DIRECTORY, Utils.VIEW_NAME_ALLTOURMARKERS);
      final SWTBotView tourMarkerView = Utils.showView(bot, Utils.VIEW_NAME_ALLTOURMARKERS);

      Utils.showViewFromMenu(bot, Utils.DIRECTORY, Utils.VIEW_NAME_COLLATEDTOURS);
      final SWTBotView collatedToursView = Utils.showView(bot, Utils.VIEW_NAME_COLLATEDTOURS);

      Utils.showViewFromMenu(bot, Utils.DIRECTORY, Utils.VIEW_NAME_REFERENCETOURS);
      final SWTBotView referenceToursView = Utils.showView(bot, Utils.VIEW_NAME_REFERENCETOURS);

//      Utils.showViewFromMenu(bot, "Help", "Error Log"); //$NON-NLS-1$ //$NON-NLS-2$
//      bot.sleep(3000);
//      Utils.showView(bot, "Error Log"); //$NON-NLS-1$

      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.COMPARE_TOURS).expand().getNode(Utils.VIEW_NAME_ELEVATIONCOMPARE).select();
      bot.button("Open").click(); //$NON-NLS-1$
      final SWTBotView comparisonResultsView = Utils.showView(bot, Utils.VIEW_NAME_ELEVATIONCOMPARE);

      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.TOUR_DIRECTORIES).expand().getNode(Utils.VIEW_NAME_TOURCOMPARISONTIMELINE).select();
      bot.button("Open").click(); //$NON-NLS-1$
      final SWTBotView yearStatisticView = Utils.showView(bot, Utils.VIEW_NAME_TOURCOMPARISONTIMELINE);

      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.PHOTO).expand().getNode(Utils.VIEW_NAME_PHOTOSANDTOURS).select();
      bot.button("Open").click(); //$NON-NLS-1$
      final SWTBotView photosAndToursView = Utils.showView(bot, Utils.VIEW_NAME_PHOTOSANDTOURS);

      Utils.showViewFromMenu(bot, Utils.TOUR, Utils.VIEW_NAME_PHOTOSTOURSPHOTOS);
      final SWTBotView tourPhotosView = Utils.showView(bot, Utils.VIEW_NAME_PHOTOSTOURSPHOTOS);

      Utils.showViewFromMenu(bot, "Map", Utils.VIEW_NAME_MAPBOOKMARK); //$NON-NLS-1$
      final SWTBotView mapBookmarkView = Utils.showView(bot, Utils.VIEW_NAME_MAPBOOKMARK);

      Utils.showViewFromMenu(bot, "Map", Utils.VIEW_NAME_MODELPLAYER); //$NON-NLS-1$
      final SWTBotView modelPlayerView = Utils.showView(bot, Utils.VIEW_NAME_MODELPLAYER);

      bot.sleep(3000);

      //Close the opened views
      tourChartSmoothingView.close();
      statisticValuesView.close();
      trainingView.close();
      heartRateVariabilityView.close();
      photosView.close();
      searchToursView.close();
      tourMarkerView.close();
      waypointsView.close();
      tourDataView.close();
      geoCompareView.close();
      collatedToursView.close();
      referenceToursView.close();
      comparisonResultsView.close();
      photosAndToursView.close();
      tourPhotosView.close();
      mapBookmarkView.close();
      modelPlayerView.close();
      yearStatisticView.close();
   }
}
