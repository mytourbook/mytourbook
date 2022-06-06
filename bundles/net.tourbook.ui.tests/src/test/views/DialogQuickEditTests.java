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
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

public class DialogQuickEditTests {

   private SWTWorkbenchBot bot = new SWTWorkbenchBot();

   @Test
   void testEditWeatherDescription() {

      final SWTBotView tourBookView = bot.viewByTitle("Tour Book");
      assertNotNull(tourBookView);
      tourBookView.show();

      bot.toolbarButtonWithTooltip("&Collapse All").click();

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2021   2").expand()
            .getNode("Jan   2").expand().select().getNode("31").select();
      assertNotNull(tour);

      tour.contextMenu("Quick Edit...").click();
      bot.textWithLabel("&Weather").setText("Sunny");
      bot.button("Save ").click();

      bot.sleep(3000);
   }
}
