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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.Utils;

public class TaggedToursViewTests {

   private SWTWorkbenchBot bot = new SWTWorkbenchBot();

   /**
    * This test could have caught this bug in the 22.3 release
    * https://sourceforge.net/p/mytourbook/discussion/622811/thread/a4d83a6052/
    */
   @Test
   void testTaggedToursView() {

      //Open the Tagged Tours view
      final SWTBotMenu otherMenu = bot.menu("Tools ").menu("All Views").menu("Other...").click();
      assertNotNull(otherMenu);
      bot.tree().getTreeItem("1. Tour Directories").expand().getNode("Tagged Tours").select();
      bot.button("Open").click();

      Utils.getTour(bot);

      Utils.showView(bot, "Tagged Tours");

      final SWTBotTreeItem item = bot.tree(1).getTreeItem("Shoes 2   1").select();
      assertNotNull(item);
      final SWTBotTreeItem node = item.getNode("5/31/15").select();
      assertNotNull(node);
   }
}
