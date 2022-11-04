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
   void tourReimport_WhenAllToursAreSelected_FilesCannotBeFound() {

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(Messages.Dialog_ReimportTours_Action_OpenDialog).click();

      bot.button(0).click();//Unlock the radio button below
      bot.radio(Messages.Dialog_ModifyTours_Radio_AllTours).click();

      bot.radio(Messages.Dialog_ReimportTours_Checkbox_EntireTour).click();
      bot.checkBox(Messages.Dialog_ReimportTours_Checkbox_SkipToursWithImportFileNotFound).click();

      bot.checkBox(Messages.Tour_Log_Checkbox_LogDetails).click();
      bot.button(Messages.Dialog_ReimportTours_Button_ReImport).click();

      bot.button("Yes").click(); //$NON-NLS-1$
      Utils.clickOkButton(bot);

      bot.sleep(10000);
   }
}
