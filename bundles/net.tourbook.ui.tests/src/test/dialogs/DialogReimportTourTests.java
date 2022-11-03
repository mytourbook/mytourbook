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

public class DialogReimportTourTests extends UITest {

   @Test
   void TourReimport_WhenAllToursAreSelected_FilesCannotBeFound() {

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(Messages.Dialog_ReimportTours_Action_OpenDialog).click();

      bot.radio(Messages.Dialog_ModifyTours_Radio_SelectedTours).click();
      bot.button("Unlock").click();
      bot.radio("All saved tours").click();
      bot.radio("Entire tour").click();
      bot.checkBox("Skip tours for which the file is not found").click();
      bot.checkBox("Do detailed logging, this can significantly increase the time").click();
      bot.button("Re-import").click();
      bot.button("Yes").click();
      bot.button("OK").click();
      bot.tree().getTreeItem("2015   1").expand();
      bot.tree().getTreeItem("2015   1").getNode("May   1").expand();
      bot.tree().getTreeItem("2015   1").expand();
      bot.tree().getTreeItem("2015   1").getNode("May   1").expand();
   }
}
