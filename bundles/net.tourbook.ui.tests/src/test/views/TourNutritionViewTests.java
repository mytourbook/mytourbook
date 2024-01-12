/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourNutritionViewTests extends UITest {

   @Test
   void testAddTourNutritionItem() {

      /*
       * Arrange
       */
      // Open the Tour Nutrition View
      Utils.showViewFromMenu(bot, Utils.TOUR, Utils.VIEW_NAME_TOURNUTRITION);
      final SWTBotView tourNutritionView = Utils.showView(bot, Utils.VIEW_NAME_TOURNUTRITION);

      Utils.duplicateAndGetTour(bot);

      tourNutritionView.show();

      final SWTBotTable productsTable = tourNutritionView.bot().table();

      // Assert initial state
      final int initialTableRowCount = productsTable.rowCount();
      // Make sure that the table doesn't contain any products
      assertTrue(initialTableRowCount == 0);

      // Assert that there are 2 more markers
      // initialTableRowCount = initialTableRowCount + 2;
      //  assertEquals(initialTableRowCount, tableMarkersTable.rowCount());

      // Act: Delete the 2 markers
//      int rowIndex = tableMarkersTable.searchText("1/1/09"); //$NON-NLS-1$
//      assertTrue(rowIndex != -1);
//      tableMarkersTable.select(rowIndex);
//      tableMarkersTable.contextMenu(Messages.App_Action_DeleteTourMarker).click();
//      Utils.clickYesButton(bot);
//
//      rowIndex = tableMarkersTable.searchText("1/1/09"); //$NON-NLS-1$
//      assertTrue(rowIndex != -1);
//      tableMarkersTable.select(rowIndex);
//      tableMarkersTable.pressShortcut(KeyStroke.getInstance(0, SWT.DEL));
//      bot.sleep(1000);
//      Utils.clickYesButton(bot);

      // Make sure that the table contains 2 less marker
      // assertEquals(initialTableRowCount - 2, tableMarkersTable.rowCount());

      final SWTBotTreeItem tour = Utils.selectDuplicatedTour(bot);
      Utils.deleteTour(bot, tour);
   }

}
