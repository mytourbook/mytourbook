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

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.byteholder.geoclipse.map.UI;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.common.util.FilesUtils;
import net.tourbook.tour.TourLogManager;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourBookViewTests extends UITest {

   @Test
   void adustTourValues_RetrieveWeatherData_OutsideOfAllowedRange() {

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(Messages.Tour_Action_AdjustTourValues)
            .menu(Messages.tour_editor_section_weather)
            .menu(Messages.Tour_Action_RetrieveWeatherData).click();

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "1/31/21, 7:15 AM -> Error while retrieving the weather data: \"{\"cod\":\"400\",\"message\":\"requested time is out of allowed range of 5 days back\"}\"")));//$NON-NLS-1$
   }

   @Test
   void adustTourValues_SetTimeZone_AllChoices() {

      //Select a tour
      SWTBotTreeItem tour = bot.tree().getTreeItem("2015   1").expand() //$NON-NLS-1$
            .getNode("May   1").expand().select().getNode("31").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);
      assertEquals("9:51 AM", tour.cell(tourBookView_StartTime_Column_Index)); //$NON-NLS-1$
      assertEquals("America/Los_Angeles", tour.cell(tourBookView_TimeZone_Column_Index)); //$NON-NLS-1$

      //Adjust the tour time zone
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues).menu(Messages.Tour_Action_SetTimeZone).click();
      bot.comboBox().setSelection("-07:00    -06:00    US/Mountain   -   DST - 1 h - N"); //$NON-NLS-1$
      bot.button(Messages.Dialog_SetTimeZone_Button_AdjustTimeZone).click();

      //Assert
      tour = bot.tree().getTreeItem("2015   1").expand() //$NON-NLS-1$
            .getNode("May   1").expand().select().getNode("31").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertEquals("10:51 AM", tour.cell(tourBookView_StartTime_Column_Index)); //$NON-NLS-1$
      assertEquals("US/Mountain", tour.cell(tourBookView_TimeZone_Column_Index)); //$NON-NLS-1$

      //Adjust the tour time zone to the default value set in the preferences
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues).menu(Messages.Tour_Action_SetTimeZone).click();
      bot.link(0).click();
      bot.button(Messages.Dialog_SetTimeZone_Button_AdjustTimeZone).click();

      //Assert
      tour = bot.tree().getTreeItem("2015   1").expand() //$NON-NLS-1$
            .getNode("May   1").expand().select().getNode("31").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertEquals("6:51 PM", tour.cell(tourBookView_StartTime_Column_Index)); //$NON-NLS-1$
      assertEquals("Europe/Paris", tour.cell(tourBookView_TimeZone_Column_Index)); //$NON-NLS-1$
   }

   @BeforeEach
   void InitializeEach() {

      tourBookView = Utils.showTourBookView(bot);
   }

   @Test
   void testComputeTourDistance() {

      //Check the original distance
      SWTBotTreeItem tour = Utils.getTour(bot);
      assertEquals("0.542", tour.cell(tourBookView_Distance_Column_Index)); //$NON-NLS-1$

      //Compute the tour distance
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues).menu(Messages.TourEditor_Action_ComputeDistanceValuesFromGeoPosition).click();
      Utils.clickOkButton(bot);

      //Check the new computed distance
      tour = Utils.getTour(bot);
      assertEquals("0.551", tour.cell(tourBookView_Distance_Column_Index)); //$NON-NLS-1$
   }

   @Test
   void testDeleteTourCalories() {

      //Check the initial calories value
      SWTBotTreeItem tour = Utils.getTourWithSRTM(bot);
      assertEquals("2", tour.cell(tourBookView_Calories_Column_Index)); //$NON-NLS-1$

      //Delete the calories value
      tour.contextMenu(Messages.Dialog_DeleteTourValues_Action_OpenDialog).click();
      bot.checkBox(Messages.Dialog_ModifyTours_Checkbox_Calories).click();
      bot.button(Messages.Dialog_DeleteTourValues_Button_Delete).click();
      Utils.clickOkButton(bot);

      bot.sleep(1000);

      //Setting the focus again on the Tourbook view
      tourBookView = Utils.showTourBookView(bot);

      //Check that the calories were deleted
      tour = Utils.getTourWithSRTM(bot);
      assertEquals(UI.EMPTY_STRING, tour.cell(tourBookView_Calories_Column_Index));
   }

   @Test
   void testDuplicateAndDeleteTour() {

      // Get a tour that can be duplicated
      SWTBotTreeItem tour = bot.tree().getTreeItem("2014   1").expand() //$NON-NLS-1$
            .getNode("Jan   1").expand().select().getNode("1").select(); //$NON-NLS-1$ //$NON-NLS-2$

      // Duplicate the tour
      tour.contextMenu("Duplicate Tour...").click(); //$NON-NLS-1$

      // Set a different date than today's date
      bot.dateTime(0).setDate(new Date(1420117200000L));
      // Set a different time than the current's time
      bot.dateTime(1).setDate(new Date(1420117200000L));

      //Save the tour
      bot.toolbarButtonWithTooltip(Utils.SAVE_MODIFIED_TOUR).click();

      tour = bot.tree().getTreeItem("2015   2").expand() //$NON-NLS-1$
            .getNode("Jan   1").expand().select().getNode("1").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      //Delete the tour
      tour.contextMenu(Messages.Tour_Book_Action_delete_selected_tours_menu).menu(Messages.Tour_Book_Action_delete_selected_tours_menu).menu(
            Messages.Tour_Book_Action_delete_selected_tours).click();
      Utils.clickOkButton(bot);
      Utils.clickOkButton(bot);

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "1/1/15, 1:00 PM")));//$NON-NLS-1$

      //Check that the tour was successfully deleted
      final SWTBotTreeItem[] allItems = bot.tree().getAllItems();
      assertEquals("2015   1", allItems[2].getText()); //$NON-NLS-1$
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

      bot.button("Save").click();

      final Path csvFilePath = Paths.get(Utils.workingDirectory, "TourBook_2022-08-30_21-39-05.csv");
      assertTrue(Files.exists(csvFilePath));

      FilesUtils.deleteIfExists(csvFilePath);
      assertTrue(!Files.exists(csvFilePath));
   }

   @Test
   void testMultiplyTourCalories() {

      //Select a tour that contains a calories value
      SWTBotTreeItem tour = bot.tree().getTreeItem("2020   3").expand() //$NON-NLS-1$
            .getNode("May   2").expand().select().getNode("23").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      //Check the original calories value
      assertEquals("1,073", tour.cell(tourBookView_Calories_Column_Index)); //$NON-NLS-1$

      //Multiply the calories by 1000
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues).menu(Messages.Tour_Action_MultiplyCaloriesBy1000).click();
      bot.button(Messages.Tour_Action_MultiplyCaloriesBy1000_Apply).click();

      //Check the new calories value
      tour = bot.tree().getTreeItem("2020   3").expand() //$NON-NLS-1$
            .getNode("May   2").expand().select().getNode("23").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);
      assertEquals("1,073,000", tour.cell(tourBookView_Calories_Column_Index)); //$NON-NLS-1$
   }

   @Test
   void testNatTable() {

      //Activating the NatTable
      bot.toolbarButtonWithTooltip(Messages.Tour_Book_Action_ToggleViewLayout_Tooltip).click();
      bot.toolbarButtonWithTooltip(Messages.Tour_Book_Action_ToggleViewLayout_Tooltip).click();

      final SWTBotNatTable botNatTable = new SWTBotNatTable(
            tourBookView.bot().widget(widgetOfType(NatTable.class)));
      assertEquals(10, botNatTable.rowCount());

      botNatTable.click(1, 0);
      botNatTable.click(2, 0);
      //FIXME org.opentest4j.AssertionFailedError: expected: <0:10> but was: <>
      //assertEquals("0:10", botNatTable.getCellDataValueByPosition(2, 4)); //$NON-NLS-1$

      //Deactivating the NatTable
      bot.toolbarButtonWithTooltip(Messages.Tour_Book_Action_ToggleViewLayout_Tooltip).click();
   }

   @Test
   void testSetElevationValuesFromSRTM() {

      SWTBotTreeItem tour = Utils.getTourWithSRTM(bot);

      //Check the original elevation value
      assertEquals("2,578", tour.cell(tourBookView_ElevationGain_Column_Index)); //$NON-NLS-1$

      //Set elevation from SRTM
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues)
            .menu(Messages.Tour_SubMenu_Elevation)
            .menu(Messages.TourEditor_Action_SetAltitudeValuesFromSRTM)
            .click();
      Utils.clickOkButton(bot);

      //Check the new elevation value
      tour = Utils.getTourWithSRTM(bot);
      assertEquals("1,008", tour.cell(tourBookView_ElevationGain_Column_Index)); //$NON-NLS-1$
   }
}
