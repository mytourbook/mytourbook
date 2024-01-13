/*******************************************************************************
 * Copyright (C) 2022, 2024 Frédéric Bard
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

import de.byteholder.geoclipse.poi.Messages;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PoiViewTests extends UITest {

   @Test
   void testPoiSearch() {

      // Arrange
      bot.toolbarButtonWithTooltip("Search for places and show them on the map (Ctrl+L)").click(); //$NON-NLS-1$
      final SWTBotView searchPlacesView = bot.viewByTitle(Utils.VIEW_NAME_SEARCHPLACES);
      searchPlacesView.show();

      final SWTBot poiViewBot = searchPlacesView.bot();
      poiViewBot.comboBox(0).setText("dehfbjewgjhrhgrg"); //$NON-NLS-1$

      // Act
      poiViewBot.button(Messages.Poi_View_Button_Search).click();

      bot.sleep(5000);

      SWTBotTable tablePois = poiViewBot.table();

      // Assert
      //Make sure that there are no POIs found
      assertTrue(tablePois.rowCount() == 0);

      poiViewBot.comboBox(0).setText("refuge"); //$NON-NLS-1$

      // Act
      poiViewBot.button(Messages.Poi_View_Button_Search).click();

      bot.sleep(5000);

      tablePois = poiViewBot.table();

      // Assert
      //Make sure that there are POIs found
      assertTrue(tablePois.rowCount() > 0);

      searchPlacesView.close();
   }
}
