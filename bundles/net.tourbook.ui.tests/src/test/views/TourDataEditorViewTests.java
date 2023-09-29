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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.Date;

import net.tourbook.Messages;
import net.tourbook.common.UI;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotDateTime;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotSpinner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourDataEditorViewTests extends UITest {

   @Test
   void testNewTour() {

      bot.toolbarButtonWithTooltip("Create new tour/event (Ctrl+N)").click(); //$NON-NLS-1$

      bot.cTabItem(Messages.tour_editor_tabLabel_tour).activate();

      final String newTourTitle = "New Tour Title"; //$NON-NLS-1$

      final SWTBot tourDataEditorViewBot = Utils.showView(bot, Utils.VIEW_NAME_TOUREDITOR).bot();

      tourDataEditorViewBot.comboBox(0).setText(newTourTitle);
      bot.toolbarButtonWithTooltip(Utils.SAVE_MODIFIED_TOUR).click();

      final SWTBotCombo titleCombo = tourDataEditorViewBot.comboBox(0);
      assertNotNull(titleCombo);
      assertEquals(newTourTitle, titleCombo.getText());

      final SWTBotDateTime tourDateTime = bot.dateTimeWithLabel(Messages.tour_editor_label_tour_date);
      assertNotNull(tourDateTime);
      tourDateTime.setDate(Date.from(Instant.now()));

      final SWTBotSpinner tourHours = bot.spinnerWithTooltip(Messages.Tour_Editor_Label_Hours_Tooltip);
      assertNotNull(tourHours);
      tourHours.setSelection(2);

      final SWTBotSpinner tourRestPulse = bot.spinnerWithLabel(Messages.tour_editor_label_rest_pulse);
      assertNotNull(tourRestPulse);
      tourRestPulse.setSelection(60);

      bot.toolbarButtonWithTooltip(Utils.SAVE_MODIFIED_TOUR).click();
   }

   @Test
   void testRemoveTimeSlice() {

      Utils.showTourBookView(bot);
      final SWTBotTreeItem tour = bot.tree().getTreeItem("2015   1").expand() //$NON-NLS-1$
            .getNode("May   1").expand().select().getNode("31").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      final SWTBot tourEditorViewBot = Utils.showView(bot, Utils.VIEW_NAME_TOUREDITOR).bot();

      bot.cTabItem(Messages.tour_editor_tabLabel_tour_data).activate();

      SWTBotTable timeSlicesTable = tourEditorViewBot.table();

      final int timeSlicesTableCount = 16829;
      assertEquals(timeSlicesTableCount, timeSlicesTable.rowCount());

      timeSlicesTable.select(3);

      timeSlicesTable.contextMenu(Messages.action_tour_editor_delete_time_slices_keep_time).click();
      Utils.clickOkButton(bot);

      timeSlicesTable = tourEditorViewBot.table();
      timeSlicesTable.contextMenu(Messages.action_tour_editor_delete_time_slices_keep_time).click();

      bot.toolbarButtonWithTooltip(Utils.SAVE_MODIFIED_TOUR).click();

      //Ensuring that the time slice was deleted
      assertEquals(timeSlicesTableCount - 1, timeSlicesTable.rowCount());
   }

   @Test
   void testTimeSlicesTab() {

      final SWTBot tourEditorViewBot = Utils.showView(bot, Utils.VIEW_NAME_TOUREDITOR).bot();

      bot.cTabItem(Messages.tour_editor_tabLabel_tour_data).activate();

      final SWTBotTable timeSlicesTable = tourEditorViewBot.table();

      // Sort the time slices by pace
      timeSlicesTable.header(UI.UNIT_LABEL_PACE).click();

      // Revert the time slices sorting by index
      timeSlicesTable.header(UI.SYMBOL_NUMBER_SIGN).click();
   }

   @Test
   void testViewTabs() {

      Utils.showView(bot, Utils.VIEW_NAME_TOUREDITOR);

      bot.cTabItem(Messages.tour_editor_tabLabel_tour_data).activate();
      bot.cTabItem(Messages.Tour_Editor_TabLabel_SwimSlices).activate();
   }
}
