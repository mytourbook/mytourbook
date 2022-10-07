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

import de.byteholder.geoclipse.poi.Messages;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.jupiter.api.Test;

import utils.UITest;

public class PoiViewTests extends UITest {

   @Test
   void testPoiSearch() {

      //Open the POI view
      bot.toolbarButtonWithTooltip("Search for places and show them on the map (Ctrl+L)").click(); //$NON-NLS-1$
      final SWTBotView viewByTitle = bot.viewByTitle("Search Places "); //$NON-NLS-1$
      viewByTitle.show();

      final SWTBot poiViewBot = viewByTitle.bot();

      poiViewBot.comboBox(0).setText("refuge"); //$NON-NLS-1$
      poiViewBot.button(Messages.Poi_View_Button_Search).click();

      bot.sleep(3000);

      final SWTBotTable tablePois = poiViewBot.table();

      //Make sure that there are POIs found
      assertEquals(10, tablePois.rowCount());
   }
}
