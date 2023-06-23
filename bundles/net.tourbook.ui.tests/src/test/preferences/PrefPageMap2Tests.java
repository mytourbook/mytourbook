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
package preferences;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageMap2Tests extends UITest {

   @Test
   void openPreferencePage() {

      Utils.openPreferences(bot);
      SWTBotTreeItem twoDMapTreeItem = bot.tree().getTreeItem("2D Map").select(); //$NON-NLS-1$

      twoDMapTreeItem = twoDMapTreeItem.expand();
      assertNotNull(twoDMapTreeItem);

      Utils.clickApplyAndCloseButton(bot);
   }
}
