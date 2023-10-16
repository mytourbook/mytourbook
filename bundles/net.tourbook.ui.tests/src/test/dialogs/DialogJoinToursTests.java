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
package dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.GregorianCalendar;

import net.tourbook.Messages;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotDateTime;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;
import views.WorkbenchTests;

public class DialogJoinToursTests extends UITest {

   @Test
   void joinTours_TestDialog() {

      openDialogJoinTours();

      //Assert the UI behavior

      //Concatenate method
      bot.comboBox(0).setSelection(Messages.Dialog_JoinTours_ComboText_KeepTime);
      assertFalse(bot.label(Messages.Dialog_JoinTours_Label_TourDate).isEnabled());
      assertFalse(bot.dateTime(0).isEnabled());
      assertFalse(bot.label(Messages.Dialog_JoinTours_Label_TourTime).isEnabled());
      assertFalse(bot.dateTime(1).isEnabled());

      bot.comboBox(0).setSelection(Messages.Dialog_JoinTours_ComboText_ConcatenateTime);
      assertTrue(bot.label(Messages.Dialog_JoinTours_Label_TourDate).isEnabled());
      assertTrue(bot.dateTime(0).isEnabled());
      assertTrue(bot.label(Messages.Dialog_JoinTours_Label_TourTime).isEnabled());
      assertTrue(bot.dateTime(1).isEnabled());

      //Title
      bot.comboBox(1).setSelection(Messages.Dialog_JoinTours_ComboText_TourTitleFromTour);
      assertFalse(bot.textWithTooltip(Messages.Dialog_SplitTour_Label_TourTitle_Tooltip).isEnabled());

      bot.comboBox(1).setSelection(Messages.Dialog_JoinTours_ComboText_TourTitleCustom);
      assertTrue(bot.textWithTooltip(Messages.Dialog_SplitTour_Label_TourTitle_Tooltip).isEnabled());

      //Tour Type
      bot.comboBox(2).setSelection(Messages.Dialog_JoinTours_ComboText_TourTypeFromTour);
      assertFalse(bot.link(0).isEnabled());
      bot.comboBox(2).setSelection(Messages.Dialog_JoinTours_ComboText_TourTypePrevious);
      assertFalse(bot.link(0).isEnabled());
      bot.comboBox(2).setSelection(Messages.Dialog_JoinTours_ComboText_TourTypeCustom);
      assertTrue(bot.link(0).isEnabled());

      //Create marker at the start of each tour
      bot.checkBox(Messages.Dialog_JoinTours_Checkbox_CreateTourMarker).select();
      assertTrue(bot.comboBox(4).isEnabled());
      assertEquals(3, bot.comboBox(4).itemCount());
      bot.checkBox(Messages.Dialog_JoinTours_Checkbox_CreateTourMarker).deselect();
      assertFalse(bot.comboBox(4).isEnabled());

      //Delete source tours
      assertFalse(bot.checkBox(Messages.Dialog_JoinTours_Checkbox_DeleteSourceTours).isEnabled());

      bot.button(Messages.Dialog_ModifyTours_Button_UnlockMultipleToursSelection_Text).click();
      assertTrue(bot.checkBox(Messages.Dialog_JoinTours_Checkbox_DeleteSourceTours).isEnabled());

      bot.checkBox(Messages.Dialog_JoinTours_Checkbox_DeleteSourceTours).select();

      bot.button(Messages.Dialog_ModifyTours_Button_LockMultipleToursSelection_Text).click();
      assertFalse(bot.checkBox(Messages.Dialog_JoinTours_Checkbox_DeleteSourceTours).isEnabled());

      Utils.clickCancelButton(bot);
   }

   @Test
   void joinTours_WithMarkers() {

      openDialogJoinTours();

      bot.checkBox(Messages.Dialog_JoinTours_Checkbox_IncludeMarkerWaypoints).select();
      bot.checkBox(Messages.Dialog_JoinTours_Checkbox_CreateTourMarker).select();

      Utils.clickOkButton(bot);

      //Check that the concatenated tour exists
      final SWTBotTreeItem tour = bot.tree().getTreeItem("2021   3").expand() //$NON-NLS-1$
            .getNode("Feb   1").expand().select().getNode("1").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      //Check that the markers exist and that their time values are correct

      //Open the Tour Marker View
      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.TOUR_PROPERTIES).expand().getNode(Utils.VIEW_NAME_TOURMARKERS).select();
      bot.button("Open").click(); //$NON-NLS-1$

      final SWTBotTable tableMarkers = bot.table();

      //Make sure that the tour contains all the markers
      assertEquals(2, tableMarkers.rowCount());

      assertEquals("0:00", tableMarkers.cell(0, 1)); //$NON-NLS-1$
      assertEquals("10:09", tableMarkers.cell(1, 1)); //$NON-NLS-1$

      Utils.deleteTour(bot, tour);
   }

   @Test
   void joinTours_WithoutMarkers() {

      openDialogJoinTours();

      bot.checkBox(Messages.Dialog_JoinTours_Checkbox_IncludeMarkerWaypoints).deselect();
      bot.checkBox(Messages.Dialog_JoinTours_Checkbox_CreateTourMarker).deselect();

      Utils.clickOkButton(bot);

      //Check that the concatenated tour exists
      final SWTBotTreeItem tour = bot.tree().getTreeItem("2021   3").expand() //$NON-NLS-1$
            .getNode("Feb   1").expand().select().getNode("1").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      assertEquals("3:47 PM", tour.cell(tourBookView_StartTime_Column_Index)); //$NON-NLS-1$
      assertEquals("0:57", tour.cell(tourBookView_ElapsedTime_Column_Index)); //$NON-NLS-1$
      assertEquals("0:57", tour.cell(tourBookView_RecordedTime_Column_Index)); //$NON-NLS-1$
      assertEquals("", tour.cell(tourBookView_PausedTime_Column_Index)); //$NON-NLS-1$

      //Open the Tour Marker View
      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.TOUR_PROPERTIES).expand().getNode(Utils.VIEW_NAME_TOURMARKERS).select();
      bot.button("Open").click(); //$NON-NLS-1$

      final SWTBotTable tableMarkers = bot.table();

      //Make sure that the tour doesn't contain any markers
      assertEquals(0, tableMarkers.rowCount());

      //Open the Tour Pauses View
      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.TOUR_PROPERTIES).expand().getNode(Utils.VIEW_NAME_TOURPAUSES).select();
      bot.button("Open").click(); //$NON-NLS-1$

      Utils.deleteTour(bot, tour);
   }

   private void openDialogJoinTours() {

      final SWTBotTree yearTree = bot.tree();
      final SWTBotTreeItem monthTreeItem = yearTree.expandNode("2021   2").getNode("Jan   2").expand(); //$NON-NLS-1$ //$NON-NLS-2$
      monthTreeItem.select("30", "31"); //$NON-NLS-1$ //$NON-NLS-2$

      //Action
      yearTree.contextMenu(Messages.App_Action_JoinTours).click();

      //Options
      setNewTourTime();
   }

   private void setNewTourTime() {

      bot.comboBox(0).setSelection(Messages.Dialog_JoinTours_ComboText_ConcatenateTime);
      final SWTBotDateTime tourDateTime = bot.dateTimeWithLabel(Messages.Dialog_JoinTours_Label_TourDate);
      assertNotNull(tourDateTime);

      final GregorianCalendar tourStartTimeCalendar = new GregorianCalendar();
      //February 1, 2021
      tourStartTimeCalendar.set(2021, 1, 1);
      tourDateTime.setDate(tourStartTimeCalendar.getTime());
   }
}
