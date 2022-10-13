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

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourMarkerViewTests extends UITest {

   @Test
   void testAddAndDeleteTourMarker() {

      Utils.showTourBookView(bot);

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2020   3").expand() //$NON-NLS-1$
            .getNode("May   2").expand().select().getNode("23").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      final SWTBotView tourEditorView = Utils.showView(bot, Utils.TOUREDITOR_VIEW_NAME);

      final SWTBotCTabItem timeSlicesTab = bot.cTabItem(Messages.tour_editor_tabLabel_tour_data).activate();
      assertNotNull(timeSlicesTab);

      final SWTBotTable timeSlicesTable = tourEditorView.bot().table();
      assertEquals(16775, timeSlicesTable.rowCount());
      timeSlicesTable.select(0);
      timeSlicesTable.contextMenu(Messages.tourCatalog_view_action_create_marker).click();
      bot.button("Save ").click(); //$NON-NLS-1$
      bot.toolbarButtonWithTooltip(Utils.SAVE_MODIFIED_TOUR).click();

      //Open the Tour Marker View
      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.TOUR_PROPERTIES).expand().getNode(Utils.TOURMARKERS_VIEW_NAME).select();
      bot.button("Open").click(); //$NON-NLS-1$

      final SWTBotTable tableMarkers = bot.table();

      //Make sure that the tour contains 3 markers
      assertEquals(3, tableMarkers.rowCount());

      tableMarkers.select(0);
      tableMarkers.select(1);
      tableMarkers.select(2);

      tableMarkers.select(0);
      tableMarkers.contextMenu(Messages.App_Action_DeleteTourMarker).click();
      bot.button("Yes").click(); //$NON-NLS-1$

      //Make sure that the tour contains 2 markers
      assertEquals(2, tableMarkers.rowCount());
   }

}
