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
package dialogs;

import net.tourbook.Messages;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class DialogQuickEditTests extends UITest {

   @Test
   void testEditWeatherDescription() {

      Utils.showTourBookView(bot);

      bot.toolbarButtonWithTooltip(Messages.App_Action_CollapseAll).click();

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(Messages.app_action_quick_edit).click();
      bot.textWithLabel(Messages.Tour_Action_Weather).setText(net.tourbook.common.Messages.Weather_Clouds_Sunny);
      bot.button(Messages.App_Action_Save).click();

      //Necessary otherwise the subsequent tests fail.
      //Not sure why but my hunch is that the window is not fully closed for the
      //subsequent tests.
      bot.sleep(3000);
   }
}
