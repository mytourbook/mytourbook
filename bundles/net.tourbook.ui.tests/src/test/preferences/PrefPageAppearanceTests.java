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
package preferences;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageAppearanceTests extends UITest {

   private static final String APPEARANCE = "Appearance"; //$NON-NLS-1$

   @Test
   void openAppearancePages() {

      Utils.openPreferences(bot);
      SWTBotTreeItem appearanceTreeItem = bot.tree().getTreeItem(APPEARANCE).select();

      appearanceTreeItem = appearanceTreeItem.expand();

      appearanceTreeItem.getNode("Colors").select(); //$NON-NLS-1$
      appearanceTreeItem.getNode("Swimming").select(); //$NON-NLS-1$
      appearanceTreeItem.getNode("Tour Chart").select(); //$NON-NLS-1$
      appearanceTreeItem.getNode("Transform Values").select(); //$NON-NLS-1$
      appearanceTreeItem.getNode("Value Format").select(); //$NON-NLS-1$

      Utils.clickApplyAndCloseButton(bot);
   }
}
