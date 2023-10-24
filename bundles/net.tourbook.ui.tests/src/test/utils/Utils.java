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
package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
import net.tourbook.application.PluginProperties;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class Utils {

   public static final String DIRECTORY                        = "Directory";                                                     //$NON-NLS-1$
   public static final String SAVE_MODIFIED_TOUR               = "Save modified tour (Ctrl+S)";                                   //$NON-NLS-1$
   public static final String TOOLS                            = "Tools ";                                                        //$NON-NLS-1$

   public static final String VIEW_NAME_COLLATEDTOURS          = PluginProperties.getText("View_Name_CollatedTours");             //$NON-NLS-1$
   public static final String VIEW_NAME_ELEVATIONCOMPARE       = PluginProperties.getText("View_Name_Compare_ByElevation");       //$NON-NLS-1$
   public static final String VIEW_NAME_GEOCOMPARE             = PluginProperties.getText("View_Name_Compare_ByGeoParts");        //$NON-NLS-1$
   public static final String VIEW_NAME_HEARTRATEVARIABILITY   = PluginProperties.getText("View_Name_HeartRateVariabilityChart"); //$NON-NLS-1$
   public static final String VIEW_NAME_MAPBOOKMARK            = PluginProperties.getText("View_Name_MapBookmark");               //$NON-NLS-1$
   public static final String VIEW_NAME_MAPPROPERTIES          = PluginProperties.getText("view_name_map_properties");            //$NON-NLS-1$
   public static final String VIEW_NAME_MODELPLAYER            = PluginProperties.getText("View_Name_ModelPlayer");               //$NON-NLS-1$
   public static final String VIEW_NAME_PHOTOSANDTOURS         = PluginProperties.getText("View_Name_PhotosAndTours");            //$NON-NLS-1$
   public static final String VIEW_NAME_PHOTOSTOURSPHOTOS      = PluginProperties.getText("View_Name_PhotoTourPhotos");           //$NON-NLS-1$
   public static final String VIEW_NAME_REFERENCETOURS         = PluginProperties.getText("View_Name_ReferenceTours");            //$NON-NLS-1$
   public static final String VIEW_NAME_SEARCHALL              = PluginProperties.getText("View_Name_SearchAll");                 //$NON-NLS-1$
   public static final String VIEW_NAME_SEARCHPLACES           = PluginProperties.getText("View_Name_SearchPOI");                 //$NON-NLS-1$
   public static final String VIEW_NAME_STATISTICS             = PluginProperties.getText("view_name_Statistics");                //$NON-NLS-1$
   public static final String VIEW_NAME_STATISTICVALUES        = PluginProperties.getText("View_Name_StatisticValues");           //$NON-NLS-1$
   public static final String VIEW_NAME_TAGGEDTOURS            = PluginProperties.getText("view_name_tagView");                   //$NON-NLS-1$
   public static final String VIEW_NAME_TOURANALYZER           = PluginProperties.getText("view_name_Analyzer");                  //$NON-NLS-1$
   public static final String VIEW_NAME_TOURBLOG               = PluginProperties.getText("View_Name_TourBlog");                  //$NON-NLS-1$
   public static final String VIEW_NAME_TOURBOOK               = PluginProperties.getText("view_name_Book");                      //$NON-NLS-1$
   public static final String VIEW_NAME_TOURCHARTSMOOTHING     = PluginProperties.getText("View_Name_TourChartSmoothing");        //$NON-NLS-1$
   public static final String VIEW_NAME_TOURCOMPARISONTIMELINE = PluginProperties.getText("View_Name_TourComparisonTimeline");    //$NON-NLS-1$
   public static final String VIEW_NAME_TOURDATA               = PluginProperties.getText("View_Name_TourData");                  //$NON-NLS-1$
   public static final String VIEW_NAME_TOUREDITOR             = PluginProperties.getText("command_view_tourEditor");             //$NON-NLS-1$
   public static final String VIEW_NAME_TOURIMPORT             = PluginProperties.getText("view_name_Data_Import");               //$NON-NLS-1$
   public static final String VIEW_NAME_TOURMAP25              = PluginProperties.getText("View_Name_TourMap25");                 //$NON-NLS-1$
   public static final String VIEW_NAME_TOURMAP3               = PluginProperties.getText("View_Name_TourMap3");                  //$NON-NLS-1$
   public static final String VIEW_NAME_ALLTOURMARKERS         = PluginProperties.getText("View_Name_AllTourMarkers");            //$NON-NLS-1$
   public static final String VIEW_NAME_TOURMARKERS            = PluginProperties.getText("view_name_Marker");                    //$NON-NLS-1$
   public static final String VIEW_NAME_TOURPAUSES             = PluginProperties.getText("View_Name_TourPauses");                //$NON-NLS-1$
   public static final String VIEW_NAME_TOURSEGMENTER          = PluginProperties.getText("view_name_Segmenter");                 //$NON-NLS-1$
   public static final String VIEW_NAME_TRAINING               = PluginProperties.getText("View_Name_Training");                  //$NON-NLS-1$
   public static final String VIEW_NAME_WAYPOINTS              = PluginProperties.getText("View_Name_Waypoint");                  //$NON-NLS-1$
   public static final String workingDirectory                 = System.getProperty("user.dir");                                  //$NON-NLS-1$

   public static void changeMeasurementSystem(final SWTWorkbenchBot bot, final String measurementSystem) {

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("General").select(); //$NON-NLS-1$
      bot.cTabItem(Messages.Pref_general_system_measurement).activate();
      bot.comboBox(0).setSelection(measurementSystem);
      Utils.clickApplyAndCloseButton(bot);
   }

   public static void clickApplyAndCloseButton(final SWTWorkbenchBot bot) {

      clickButton("Apply and Close", bot); //$NON-NLS-1$
   }

   private static SWTBotButton clickButton(final String mnemonicText, final SWTWorkbenchBot bot) {

      return bot.button(mnemonicText).click();
   }

   public static void clickCancelButton(final SWTWorkbenchBot bot) {

      clickButton(IDialogConstants.CANCEL_LABEL, bot);
   }

   public static void clickCloseButton(final SWTWorkbenchBot bot) {

      clickButton(IDialogConstants.CLOSE_LABEL, bot);
   }

   public static void clickNoButton(final SWTWorkbenchBot bot) {

      clickButton(IDialogConstants.NO_LABEL, bot);
   }

   public static void clickOkButton(final SWTWorkbenchBot bot) {

      clickButton(IDialogConstants.OK_LABEL, bot);
   }

   public static void clickYesButton(final SWTWorkbenchBot bot) {

      clickButton(IDialogConstants.YES_LABEL, bot);
   }

   public static void deleteTour(final SWTWorkbenchBot bot, final SWTBotTreeItem tour) {

      SWTBotTreeItem[] allItems = bot.tree().getAllItems();
      String[] totalSummary = allItems[allItems.length - 1].getText().split(UI.SPACE3);
      final int initialTotalValue = Integer.parseInt(totalSummary[1]);

      tour.contextMenu(Messages.Tour_Book_Action_delete_selected_tours_menu).menu(Messages.Tour_Book_Action_delete_selected_tours_menu).menu(
            Messages.Tour_Book_Action_delete_selected_tours).click();
      Utils.clickOkButton(bot);
      Utils.clickOkButton(bot);

      //Check that the tour was successfully deleted
      allItems = bot.tree().getAllItems();
      totalSummary = allItems[allItems.length - 1].getText().split(UI.SPACE3);
      final int newTotalValue = Integer.parseInt(totalSummary[1]);

      assertEquals(initialTotalValue - 1, newTotalValue);
   }

   public static void deleteTourWithPauses(final SWTWorkbenchBot bot) {

      Utils.showTourBookView(bot);
      final SWTBotTreeItem tour = bot.tree().getTreeItem("2016   2").expand() //$NON-NLS-1$
            .getNode("Sep   1").expand().select().getNode("19").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      //Delete the tour
      Utils.deleteTour(bot, tour);
   }

   public static void deleteTourWithSensors(final SWTWorkbenchBot bot) {

      Utils.showTourBookView(bot);
      final SWTBotTreeItem tour = bot.tree().getTreeItem("2022   2").expand() //$NON-NLS-1$
            .getNode("Feb   2").expand().select().getNode("4").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      //Delete the tour
      Utils.deleteTour(bot, tour);
   }

   /**
    * Select a tour and duplicates it.
    * The tour is duplicated to a specific date so that it can be retrieved any
    * time if necessary.
    *
    * This is useful for tests that will modify a tour and need to unalter the
    * tests database in order no to impact other tests and keep each test
    * independent from each other.
    */
   public static SWTBotTreeItem duplicateAndGetTour(final SWTWorkbenchBot bot) {

      showTourBookView(bot);

      // Get a tour that can be duplicated
      SWTBotTreeItem tour = bot.tree().getTreeItem("2013   1").expand() //$NON-NLS-1$
            .getNode("May   1").expand().select().getNode("18").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      // Duplicate the tour
      tour.contextMenu(Messages.Tour_Action_DuplicateTour).click();

      bot.sleep(1000);

      final var currentShells = bot.shells();
      // The experimental dialog message only appears once
      if (Arrays.stream(currentShells).anyMatch(shell -> shell.getText().equals("Experimental Feature"))) { //$NON-NLS-1$

         Utils.clickOkButton(bot);
      }
      bot.cTabItem(Messages.tour_editor_tabLabel_tour).activate();

      final GregorianCalendar tourStartTimeCalendar = new GregorianCalendar();
      tourStartTimeCalendar.set(2009, 0, 1);
      // Set a different date than today's date
      bot.dateTime(0).setDate(tourStartTimeCalendar.getTime());

      //Save the tour
      bot.toolbarButtonWithTooltip(Utils.SAVE_MODIFIED_TOUR).click();

      tour = selectDuplicatedTour(bot);
      assertNotNull(tour);

      return tour;
   }

   /**
    * Because the SWTBot function {@link SWTBot#buttonWithTooltip} doesn't appear
    * to work in every case, this function will find a toolbar button for a given
    * bot view based on the tooltip text of the button.
    *
    */
   public static SWTBotToolbarButton getToolbarButton(final SWTBotView botView, final String tooltipText) {

      for (final SWTBotToolbarButton button : botView.getToolbarButtons()) {

         if (tooltipText.equals(button.getToolTipText())) {

            return button;
         }
      }

      return null;
   }

   public static SWTBotTreeItem getTour(final SWTWorkbenchBot bot) {

      showTourBookView(bot);

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2021   2").expand() //$NON-NLS-1$
            .getNode("Jan   2").expand().select().getNode("31").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      return tour;
   }

   public static SWTBotTreeItem getTourWithPauses(final SWTWorkbenchBot bot) {

      importTour(bot, "ParkCity.fitlogEx"); //$NON-NLS-1$

      showTourBookView(bot);

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2016   2").expand() //$NON-NLS-1$
            .getNode("Sep   1").expand().select().getNode("19").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      return tour;
   }

   /**
    * Select a tour for which we have SRTM3 data
    */
   public static SWTBotTreeItem getTourWithSRTM(final SWTWorkbenchBot bot) {

      showTourBookView(bot);

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2013   1").expand() //$NON-NLS-1$
            .getNode("May   1").expand().select().getNode("18").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      return tour;
   }

   public static void importTour(final SWTWorkbenchBot bot, final String fileName) {

      showImportView(bot);

      //Switch between UIs
      bot.toolbarButtonWithTooltip(Messages.Import_Data_Action_ImportUI_Tooltip).click();

      bot.toolbarButton(1).click();

      bot.cTabItem(Messages.Dialog_ImportConfig_Tab_Configuration).activate();

      bot.comboBox(2).setText(Utils.workingDirectory + "\\src\\test\\files"); //$NON-NLS-1$
      bot.checkBox(Messages.Dialog_ImportConfig_Checkbox_CreateBackup).deselect();
      bot.textWithLabel(Messages.Dialog_ImportConfig_Label_DeviceFiles).setText(fileName);

      bot.cTabItem(Messages.Dialog_ImportConfig_Tab_Launcher).activate();
      bot.checkBox(Messages.Dialog_ImportConfig_Checkbox_SaveTour).select();

      Utils.clickOkButton(bot);

      bot.sleep(2000);

      // Launch the import
      bot.table().doubleClick(0, 0);

      bot.sleep(2000);

      bot.toolbarButtonWithTooltip(Messages.import_data_action_clear_view_tooltip).click();

      bot.toolbarButtonWithTooltip(Messages.Import_Data_Action_ImportUI_Tooltip).click();
      bot.toolbarButtonWithTooltip(Messages.Import_Data_Action_ImportUI_Tooltip).click();
   }

   public static void importTourWithSensors(final SWTWorkbenchBot bot) {

      importTour(bot, "2022-02-04-152754-UBERDROID8A2F-9-0.fit"); //$NON-NLS-1$
   }

   public static boolean isUrlReachable(final String url) {

      final HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .build();

      try {
         final HttpResponse<String> response = OAuth2Utils.httpClient.send(request, BodyHandlers.ofString());

         return response.statusCode() == HttpURLConnection.HTTP_OK;
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return false;
   }

   public static void openOtherMenu(final SWTWorkbenchBot bot) {

      final SWTBotMenu otherMenu = bot.menu(TOOLS).menu("All Views").menu("Other...").click(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(otherMenu);
   }

   public static void openPreferences(final SWTWorkbenchBot bot) {

      // The tooltip value is different on Linux because it's encrypted in M1, M2 and M3
      // https://github.com/mytourbook/mytourbook/blob/197f21c34e03728832a415f9368eca43260750bb/bundles/net.tourbook/plugin.properties#L109-L131
      final String preferencesTooltip = UI.IS_LINUX
            ? "Preferences (Shift+Ctrl+P)" //$NON-NLS-1$
            : "Preferences (Ctrl+Shift+P)"; //$NON-NLS-1$
      bot.toolbarButtonWithTooltip(preferencesTooltip).click();
   }

   public static void openVendorPage(final SWTBotTreeItem treeItem, final String vendorName) {

      treeItem.getNode(vendorName).select();
   }

   public static SWTBotTreeItem selectDuplicatedTour(final SWTWorkbenchBot bot) {

      showTourBookView(bot);

      // Get a tour that can be duplicated
      final SWTBotTreeItem tour = bot.tree().getTreeItem("2009   1").expand() //$NON-NLS-1$
            .getNode("Jan   1").expand().select().getNode("1").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      return tour;
   }

   public static SWTBotView showImportView(final SWTWorkbenchBot bot) {

      // The tooltip value is different on Linux because it's encrypted in M1, M2 and M3
      // https://github.com/mytourbook/mytourbook/blob/197f21c34e03728832a415f9368eca43260750bb/bundles/net.tourbook/plugin.properties#L109-L131
      final String tourImportTooltip = UI.IS_LINUX
            ? "Tour Import (Shift+Ctrl+I)"
            : "Tour Import (Ctrl+Shift+I)";
      bot.toolbarButtonWithTooltip(tourImportTooltip).click();
      return Utils.showView(bot, Utils.VIEW_NAME_TOURIMPORT);
   }

   public static SWTBotView showTourBookView(final SWTWorkbenchBot bot) {

      return showView(bot, VIEW_NAME_TOURBOOK);
   }

   public static SWTBotView showView(final SWTWorkbenchBot bot, final String viewName) {

      final SWTBotView view = bot.viewByTitle(viewName);
      assertNotNull(view);
      view.show();

      return view;
   }

   public static void showViewFromMenu(final SWTWorkbenchBot bot, final String menuName, final String viewName) {

      final SWTBotMenu viewMenu = bot.menu(menuName).menu(viewName).click();
      assertNotNull(viewMenu);
   }
}
