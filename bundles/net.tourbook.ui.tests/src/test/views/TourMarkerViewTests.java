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

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

public class TourMarkerViewTests {

   private SWTWorkbenchBot bot = new SWTWorkbenchBot();

   @Test
   void testTourMarkerView() {

      final SWTBotView tourBookView = bot.viewByTitle("Tour Book");
      assertNotNull(tourBookView);
      tourBookView.show();

      final SWTBotTreeItem twentyTwentyTreeItem = bot.tree().getTreeItem("2020   3");
      twentyTwentyTreeItem.expand();
      final SWTBotTreeItem mayNode = twentyTwentyTreeItem.getNode("May   2");
      mayNode.expand();
      mayNode.select();
      final SWTBotTreeItem tourTreeitem = mayNode.getNode("23").select();
      assertNotNull(tourTreeitem);

      //Open the Tour Marker View
      final SWTBotView tourMarkerView = bot.viewByTitle("Tour Markers");
      assertNotNull(tourMarkerView);
      tourMarkerView.show();

      final SWTBotTable tableMarkers = bot.table();

      //Make sure that the tour contains 2 markers
      assertEquals(2, tableMarkers.rowCount());

      tableMarkers.select(0);
      tableMarkers.select(1);

      tableMarkers.contextMenu("Delete Markers...").click();
      bot.button("Yes").click();

      //Make sure that the tour now contains 1 marker
      assertEquals(1, tableMarkers.rowCount());
   }
}
