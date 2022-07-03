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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.tourbook.Messages;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.Utils;

public class TourMarkerViewTests {

   private SWTWorkbenchBot bot = new SWTWorkbenchBot();

   @Test
   void testAddAndDeleteTourMarker() {

      Utils.showView(bot, "Tour Book");

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2020   3").expand()
            .getNode("May   2").expand().select().getNode("23").select();
      assertNotNull(tour);

      final SWTBotView tourEditorView = Utils.showView(bot, "Tour Editor");

      final SWTBotCTabItem timeSlicesTab = bot.cTabItem(Messages.tour_editor_tabLabel_tour_data).activate();
      assertNotNull(timeSlicesTab);

      final SWTBotTable timeSlicesTable = tourEditorView.bot().table();
      assertEquals(16775, timeSlicesTable.rowCount());
      timeSlicesTable.select(0);
      timeSlicesTable.contextMenu(Messages.tourCatalog_view_action_create_marker).click();
      bot.button("Save ").click();
      bot.toolbarButtonWithTooltip("Save modified tour (Ctrl+S)").click();

      //Open the Tour Marker View
      Utils.showView(bot, "Tour Markers");
      final SWTBotTable tableMarkers = bot.table();

      //Make sure that the tour contains 3 markers
      assertEquals(3, tableMarkers.rowCount());

      tableMarkers.select(0);
      tableMarkers.select(1);
      tableMarkers.select(2);

      tableMarkers.select(0);
      tableMarkers.contextMenu(Messages.App_Action_DeleteTourMarker).click();
      bot.button("Yes").click();

      //Make sure that the tour contains 2 markers
      assertEquals(2, tableMarkers.rowCount());
   }

}
