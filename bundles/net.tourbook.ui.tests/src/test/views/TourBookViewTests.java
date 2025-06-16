/*******************************************************************************
 * Copyright (C) 2022, 2025 Frédéric Bard
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

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.PluginProperties;
import net.tourbook.common.UI;
import net.tourbook.common.util.FileUtils;
import net.tourbook.common.util.StringUtils;
import net.tourbook.tour.TourLogManager;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.viewport.command.ShowColumnInViewportCommand;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarToggleButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourBookViewTests extends UITest {

   @Test
   void adjustTourValues_RetrieveWeatherData_OpenWeatherMap() {

      // Select OpenWeatherMap to make sure the air quality is retrieved

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("Weather").select(); //$NON-NLS-1$

      bot.comboBox().setSelection(1);

      // Select to display the full weather information
      bot.checkBox(Messages.Pref_Weather_Check_DisplayFullLog).click();

      Utils.clickApplyAndCloseButton(bot);

      // Select a tour
      Utils.showTourBookView(bot);
      final SWTBotTreeItem tour = Utils.getTour(bot);
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues)
            .menu(Messages.tour_editor_section_weather)
            .menu(Messages.Tour_Action_RetrieveWeatherData).click();

      bot.sleep(5000);

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(log -> log.toString()).anyMatch(log -> log.contains(
            "Data retrieved in ")));//$NON-NLS-1$
      assertTrue(logs.stream().map(log -> log.toString()).anyMatch(log -> log.contains(
            "1/31/2021, 7:15 AM:")));//$NON-NLS-1$
      assertTrue(logs.stream().map(log -> log.toString()).anyMatch(log -> log.contains(
            "air quality Fair")));//$NON-NLS-1$
   }

   @Test
   void adjustTourValues_SetTimeZone_AllChoices() {

      SWTBotTreeItem tour = Utils.duplicateAndGetTour(bot);
      assertEquals("11:00 AM", tour.cell(tourBookView_StartTime_Column_Index)); //$NON-NLS-1$
      assertEquals("America/Los_Angeles", tour.cell(tourBookView_TimeZone_Column_Index)); //$NON-NLS-1$

      //Adjust the tour time zone
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues).menu(Messages.Tour_Action_SetTimeZone).click();
      bot.comboBox().setSelection("-07:00    -06:00    US/Mountain   -   DST - 1 h - N"); //$NON-NLS-1$
      bot.button(Messages.Dialog_SetTimeZone_Button_AdjustTimeZone).click();

      //Assert
      tour = Utils.selectDuplicatedTour(bot);
      assertEquals("12:00 PM", tour.cell(tourBookView_StartTime_Column_Index)); //$NON-NLS-1$
      assertEquals("US/Mountain", tour.cell(tourBookView_TimeZone_Column_Index)); //$NON-NLS-1$

      //Adjust the tour time zone to the default value set in the preferences
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues).menu(Messages.Tour_Action_SetTimeZone).click();
      bot.link(0).click();
      bot.button(Messages.Dialog_SetTimeZone_Button_AdjustTimeZone).click();

      //Assert
      tour = Utils.selectDuplicatedTour(bot);
      assertEquals("8:00 PM", tour.cell(tourBookView_StartTime_Column_Index)); //$NON-NLS-1$
      assertEquals("Europe/Paris", tour.cell(tourBookView_TimeZone_Column_Index)); //$NON-NLS-1$

      Utils.deleteTour(bot, tour);
   }

   @BeforeEach
   void setUp() {

      tourBookView = Utils.showTourBookView(bot);
   }

   @Test
   void testComputeTourDistance() {

      SWTBotTreeItem tour = Utils.duplicateAndGetTour(bot);

      //Check the original distance
      assertEquals("19.377", tour.cell(tourBookView_Distance_Column_Index)); //$NON-NLS-1$

      //Compute the tour distance
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues).menu(Messages.TourEditor_Action_ComputeDistanceValuesFromGeoPosition).click();
      Utils.clickOkButton(bot);

      //Check the new computed distance
      tour = Utils.selectDuplicatedTour(bot);
      assertEquals("19.379", tour.cell(tourBookView_Distance_Column_Index)); //$NON-NLS-1$

      Utils.deleteTour(bot, tour);
   }

   /**
    * This test doesn't work because SWTBot doesn't support native dialogs
    * https://wiki.eclipse.org/SWTBot/FAQ#How_do_I_use_SWTBot_to_test_native_dialogs_.28File_Dialogs.2C_Color_Dialogs.2C_etc.29.3F
    */
   @Disabled
   @Test
   void testExportTourBookView() {

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(Messages.App_Action_ExportViewCSV).click();

      bot.button("Save").click(); //$NON-NLS-1$

      final Path csvFilePath = Paths.get(Utils.WORKING_DIRECTORY, "TourBook_2022-08-30_21-39-05.csv"); //$NON-NLS-1$
      assertTrue(Files.exists(csvFilePath));

      FileUtils.deleteIfExists(csvFilePath);
      assertTrue(!Files.exists(csvFilePath));
   }

   @Test
   void testMultiplyTourCalories() {

      //Select a tour that contains a calories value
      SWTBotTreeItem tour = Utils.duplicateAndGetTour(bot);

      //Check the original calories value
      assertEquals("2", tour.cell(tourBookView_Calories_Column_Index)); //$NON-NLS-1$

      //Multiply the calories by 1000
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues).menu(Messages.Tour_Action_MultiplyCaloriesBy1000).click();
      bot.button(Messages.Tour_Action_MultiplyCaloriesBy1000_Apply).click();

      //Check the new calories value
      tour = Utils.selectDuplicatedTour(bot);
      assertEquals("2,336", tour.cell(tourBookView_Calories_Column_Index)); //$NON-NLS-1$

      Utils.deleteTour(bot, tour);
   }

   @Test
   void testNatTable() {

      //Activating the NatTable
      bot.toolbarButtonWithTooltip(Messages.Tour_Book_Action_ToggleViewLayout_Tooltip).click();
      bot.toolbarButtonWithTooltip(Messages.Tour_Book_Action_ToggleViewLayout_Tooltip).click();

      // NatTable is slow to appear so we wait a bit otherwise the test will fail
      bot.sleep(3000);

      final SWTBotNatTable botNatTable = new SWTBotNatTable(
            tourBookView.bot().widget(widgetOfType(NatTable.class)));
      assertTrue(botNatTable.rowCount() > 0);

      assertTrue(StringUtils.hasContent(botNatTable.getCellDataValueByPosition(2, 4)));

      final int numberVisibleColumns = 4;
      int visibleColumnIndex = 1;
      for (int columnIndex = 1; columnIndex < 92; ++columnIndex, ++visibleColumnIndex) {

         if (visibleColumnIndex == numberVisibleColumns) {

            // Scrolling the NatTable horizontally
            botNatTable.widget.doCommand(new ShowColumnInViewportCommand(columnIndex + numberVisibleColumns));
            visibleColumnIndex = 1;
         }

         botNatTable.click(1, visibleColumnIndex);
      }

      //Deactivating the NatTable
      bot.toolbarButtonWithTooltip(Messages.Tour_Book_Action_ToggleViewLayout_Tooltip).click();
   }

   @Test
   void testSetElevationValuesFromSRTM() {

      SWTBotTreeItem tour = Utils.duplicateAndGetTour(bot);

      //Check the original elevation value
      assertEquals("658", tour.cell(tourBookView_ElevationGain_Column_Index)); //$NON-NLS-1$

      //Set elevation from SRTM
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues)
            .menu(Messages.Tour_SubMenu_Elevation)
            .menu(Messages.TourEditor_Action_SetAltitudeValuesFromSRTM)
            .click();
      Utils.clickOkButton(bot);

      //Check the new elevation value
      tour = Utils.selectDuplicatedTour(bot);
      assertEquals("1,008", tour.cell(tourBookView_ElevationGain_Column_Index)); //$NON-NLS-1$

      Utils.deleteTour(bot, tour);
   }

   @Test
   void testTourFilters() {

      // Activate the tour filter
      final SWTBotToolbarToggleButton tourFilterButton = bot.toolbarToggleButtonWithTooltip(
            Messages.Tour_Filter_Action_Tooltip +
                  UI.NEW_LINE2 +
                  net.tourbook.common.Messages.Slideout_Dialog_Action_ResetSlideoutLocation_Tooltip);
      assertNotNull(tourFilterButton);
      tourFilterButton.click();

      // Deactivate the tour filter
      tourFilterButton.click();

      // Activate the tour geo filter
      final SWTBotToolbarToggleButton tourGeoFilterButton = bot.toolbarToggleButtonWithTooltip(
            Messages.Tour_GeoFilter_Action_Tooltip +
                  UI.NEW_LINE2 +
                  net.tourbook.common.Messages.Slideout_Dialog_Action_ResetSlideoutLocation_Tooltip);
      assertNotNull(tourGeoFilterButton);
      tourGeoFilterButton.click();

      // Deactivate the tour geo filter
      tourGeoFilterButton.click();

      // Activate the tour tag filter
      final SWTBotToolbarToggleButton tourTagFilterButton = bot.toolbarToggleButtonWithTooltip(
            Messages.Tour_Tag_Filter_Action_Tooltip +
                  UI.NEW_LINE2 +
                  net.tourbook.common.Messages.Slideout_Dialog_Action_ResetSlideoutLocation_Tooltip);
      assertNotNull(tourTagFilterButton);
      tourTagFilterButton.click();

      // Deactivate the tour tag filter
      tourTagFilterButton.click();

      // Activate the tour photo filter
      final SWTBotToolbarToggleButton tourPhotoFilterButton = bot.toolbarToggleButtonWithTooltip(
            PluginProperties.getText("Action_TourPhotoFilter_Tooltip")); //$NON-NLS-1$
      assertNotNull(tourPhotoFilterButton);
      tourPhotoFilterButton.click();

      // Deactivate the tour photo filter
      tourPhotoFilterButton.click();
   }
}
