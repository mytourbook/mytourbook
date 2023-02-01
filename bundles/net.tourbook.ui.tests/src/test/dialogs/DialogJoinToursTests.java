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

import java.util.Date;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.tour.TourLogManager;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotDateTime;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;
import views.WorkbenchTests;

public class DialogJoinToursTests extends UITest {

   private void deleteConcatenatedTour(final SWTBotTreeItem tour) {

      tour.contextMenu(Messages.Tour_Book_Action_delete_selected_tours_menu).menu(Messages.Tour_Book_Action_delete_selected_tours_menu).menu(
            Messages.Tour_Book_Action_delete_selected_tours).click();
      Utils.clickOkButton(bot);
      Utils.clickOkButton(bot);

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "2/1/21, 9:11 AM")));//$NON-NLS-1$

      //Check that the tour was successfully deleted
      final SWTBotTreeItem[] allItems = bot.tree().getAllItems();
      assertEquals("2021   3", allItems[4].getText()); //$NON-NLS-1$
   }

   @Test
   void joinTours() {

      openDialogJoinTours();

      bot.checkBox(Messages.Dialog_JoinTours_Checkbox_IncludeMarkerWaypoints).deselect();
      bot.checkBox(Messages.Dialog_JoinTours_Checkbox_InsertPauses).deselect();

      Utils.clickOkButton(bot);

      //Check that the concatenated tour exists
      final SWTBotTreeItem tour = bot.tree().getTreeItem("2021   4").expand() //$NON-NLS-1$
            .getNode("Feb   1").expand().select().getNode("1").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      deleteConcatenatedTour(tour);
   }

   @Test
   void joinTours_WithMarkersAndPauses() {

      openDialogJoinTours();

      bot.checkBox(Messages.Dialog_JoinTours_Checkbox_IncludeMarkerWaypoints).select();
      bot.checkBox(Messages.Dialog_JoinTours_Checkbox_InsertPauses).select();

      bot.comboBox(1).setSelection(Messages.Dialog_JoinTours_ComboText_TourTitleCustom);
      assertTrue(bot.textWithTooltip(Messages.Dialog_SplitTour_Label_TourTitle_Tooltip).isEnabled());

      bot.comboBox(1).setSelection(Messages.Dialog_JoinTours_ComboText_TourTitleFromTour);
      assertFalse(bot.textWithTooltip(Messages.Dialog_SplitTour_Label_TourTitle_Tooltip).isEnabled());

      Utils.clickOkButton(bot);

      //Check that the concatenated tour exists
      final SWTBotTreeItem tour = bot.tree().getTreeItem("2021   4").expand() //$NON-NLS-1$
            .getNode("Feb   1").expand().select().getNode("1").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      //Check that the markers exist and that their time values are correct

      //Open the Tour Marker View
      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.TOUR_PROPERTIES).expand().getNode(Utils.TOURMARKERS_VIEW_NAME).select();
      bot.button("Open").click(); //$NON-NLS-1$

      final SWTBotTable tableMarkers = bot.table();

      //Make sure that the tour contains 3 markers
      assertEquals(4, tableMarkers.rowCount());

      /// assertEquals("0:00", tableMarkers.cell(0, 1)); //$NON-NLS-1$
      // assertEquals("4:15:46", tableMarkers.cell(1, 1)); //$NON-NLS-1$

      deleteConcatenatedTour(tour);
   }

   private void openDialogJoinTours() {

      final SWTBotTree yearTree = bot.tree();
      final SWTBotTreeItem monthTreeItem = yearTree.expandNode("2021   3").getNode("Jan   3").expand(); //$NON-NLS-1$ //$NON-NLS-2$
      monthTreeItem.select("2", "30"); //$NON-NLS-1$ //$NON-NLS-2$

      //Action
      yearTree.contextMenu(Messages.App_Action_JoinTours).click();

      //Options
      bot.checkBox(Messages.Dialog_SplitTour_Checkbox_KeepTime).deselect();

      final SWTBotDateTime tourDateTime = bot.dateTimeWithLabel(Messages.Dialog_JoinTours_Label_TourDate);
      assertNotNull(tourDateTime);
      tourDateTime.setDate(new Date(1612221767000L));
      bot.comboBox(0).setSelection(Messages.Dialog_JoinTours_ComboText_ConcatenateTime);
   }
}
