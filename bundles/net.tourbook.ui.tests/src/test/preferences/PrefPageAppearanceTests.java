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

import org.junit.jupiter.api.Test;

import utils.UITest;

public class PrefPageAppearanceTests extends UITest {

   @Test
   void openPreferencePage() {

      bot.toolbarButtonWithTooltip("Preferences (Ctrl+Shift+P)").click(); //$NON-NLS-1$
      bot.tree().getTreeItem("Appearance").expand().getNode("Value Format").select(); //$NON-NLS-1$ //$NON-NLS-2$

      bot.button("Apply and Close").click(); //$NON-NLS-1$
   }

   @Test
   void testAppearance() {

      bot.toolbarButtonWithTooltip("Preferences (Ctrl+Shift+P)").click(); //$NON-NLS-1$
      bot.tree().getTreeItem("Appearance").select(); //$NON-NLS-1$
      bot.button("Apply and Close").click(); //$NON-NLS-1$
   }
}
