/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tourbook.Messages;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class AllTourMarkersViewTests extends UITest {

   @Test
   void testDeleteTourMarker() {

      // Open the All Tour Markers View
      Utils.showViewFromMenu(bot, Utils.DIRECTORY, Utils.VIEW_NAME_ALLTOURMARKERS);
      final SWTBotView allTourMarkersView = Utils.showView(bot, Utils.VIEW_NAME_ALLTOURMARKERS);

      SWTBotTreeItem tour = Utils.duplicateAndGetTour(bot);

      allTourMarkersView.show();

      final SWTBotTable tableMarkersTable = allTourMarkersView.bot().table();

      // assert initial state
      final int initialTableRowCount = tableMarkersTable.rowCount();
      // Make sure that the table contains several markers
      assertTrue(initialTableRowCount > 0);

      //todo fb fix when the duplicate of markers is fixed
//      final int rowIndex = tableMarkersTable.searchText("2/1/09");
      final int rowIndex = tableMarkersTable.searchText("5/18/13");
      assertTrue(rowIndex != -1);
      tableMarkersTable.select(rowIndex);
      tableMarkersTable.contextMenu(Messages.App_Action_DeleteTourMarker).click();
      Utils.clickYesButton(bot);

      // Make sure that the table contains 1 less marker
      assertEquals(initialTableRowCount - 1, tableMarkersTable.rowCount());

      tour = Utils.selectDuplicatedTour(bot);
      Utils.deleteTour(bot, tour);
   }

}
