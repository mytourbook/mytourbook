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

import net.tourbook.Messages;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class Map2ViewTests extends UITest {

   @Disabled
   @Test
   public void testExportImageAsImageFile() {

      Utils.showTourBookView(bot);

      bot.toolbarButtonWithTooltip(Messages.App_Action_CollapseAll).click();

      Utils.getTour(bot);

      bot.toolbarButtonWithTooltip("Show tour in 2D map").click(); //$NON-NLS-1$
      final SWTBotView map2ViewBot = Utils.showView(bot, "2D Tour Map"); //$NON-NLS-1$
      //Sleeping 3 seconds as the map can be slow to display
      bot.sleep(3000);

      map2ViewBot.bot().tree().contextMenu(net.tourbook.map2.Messages.Map_Action_Export_Map_View).menu(
            net.tourbook.map2.Messages.Map_Action_Export_Map_View_Image).click();
   }
}
