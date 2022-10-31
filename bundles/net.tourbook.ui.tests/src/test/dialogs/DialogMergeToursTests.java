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

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class DialogMergeToursTests extends UITest {

   @Test
   void testMergeTours() {

      final SWTBotTree yearTree = bot.tree();
      final SWTBotTreeItem monthTreeItem = yearTree.expandNode("2021   3").getNode("Jan   3").expand(); //$NON-NLS-1$
      monthTreeItem.select("2", "30");

      yearTree.contextMenu(Messages.App_Action_JoinTours).click();

      Utils.clickOkButton(bot);
   }
}
