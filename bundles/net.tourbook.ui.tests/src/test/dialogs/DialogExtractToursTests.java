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
package dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.tour.TourLogManager;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotDateTime;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class DialogExtractToursTests extends UITest {

   @Test
   void extractTour() {

      Utils.getTour(bot);
      SWTBot tourEditorViewBot = Utils.showView(bot, Utils.TOUREDITOR_VIEW_NAME).bot();
      bot.cTabItem(Messages.tour_editor_tabLabel_tour_data).activate();
      bot.toolbarToggleButtonWithTooltip(Messages.app_action_edit_rows_tooltip).click();

      //Actions
      SWTBotTable timeSlicesTable = tourEditorViewBot.table();
      timeSlicesTable.select(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
      timeSlicesTable.contextMenu(Messages.App_Action_ExtractTour).click();

      //Options
      bot.checkBox(Messages.Dialog_SplitTour_Checkbox_KeepTime).click();
      final SWTBotDateTime tourDateTime = bot.dateTimeWithLabel(Messages.Dialog_JoinTours_Label_TourDate);
      assertNotNull(tourDateTime);
      tourDateTime.setDate(new Date(1612221767000L));
      bot.comboBox(0).setSelection(0);
      Utils.clickOkButton(bot);

      //Check that the extracted tour exists
      Utils.showTourBookView(bot);
      final SWTBotTreeItem tour = bot.tree().getTreeItem("2021   4").expand() //$NON-NLS-1$
            .getNode("Feb   1").expand().select().getNode("1").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);
      //Check that it contains 10 time slices
      tourEditorViewBot = Utils.showView(bot, Utils.TOUREDITOR_VIEW_NAME).bot();
      timeSlicesTable = tourEditorViewBot.table();
      assertEquals(10, timeSlicesTable.rowCount());
      bot.toolbarToggleButtonWithTooltip(Messages.app_action_edit_rows_tooltip).click();

      //Delete the tour
      Utils.showTourBookView(bot);
      tour.contextMenu(Messages.Tour_Book_Action_delete_selected_tours_menu).menu(Messages.Tour_Book_Action_delete_selected_tours_menu).menu(
            Messages.Tour_Book_Action_delete_selected_tours).click();
      Utils.clickOkButton(bot);
      Utils.clickOkButton(bot);

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "2/1/21, 7:15 AM")));//$NON-NLS-1$

      //Check that the tour was successfully deleted
      final SWTBotTreeItem[] allItems = bot.tree().getAllItems();
      assertEquals("2021   3", allItems[4].getText()); //$NON-NLS-1$
   }
}
