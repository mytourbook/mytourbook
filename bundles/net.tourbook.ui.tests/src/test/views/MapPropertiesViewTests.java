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

import net.tourbook.map2.Messages;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class MapPropertiesViewTests extends UITest {

   @Test
   void testMapPropertiesView_Basic() {

      Utils.getTour(bot);

      //Open the Map Properties view
      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.SYSTEM).expand().getNode(Utils.MAPPROPERTIES_VIEW_NAME).select();
      bot.button("Open").click(); //$NON-NLS-1$
      final SWTBotView mapPropertiesView = Utils.showView(bot, Utils.MAPPROPERTIES_VIEW_NAME);

      bot.checkBox(Messages.Map_Properties_ShowTileInfo).select();
      bot.checkBox(Messages.Map_Properties_ShowTileBorder).select();
      bot.checkBox(Messages.Map_Properties_ShowGeoGrid).select();

      mapPropertiesView.close();
   }
}
