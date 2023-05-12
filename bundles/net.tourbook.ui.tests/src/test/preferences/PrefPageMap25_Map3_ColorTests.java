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
package preferences;

import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageMap25_Map3_ColorTests extends UITest {

   @Test
   void openPreferencePage() {

      Utils.openPreferences(bot);

      bot.tree().getTreeItem("2.5D + 3D Map").expand().getNode("Color").select(); //$NON-NLS-1$ //$NON-NLS-2$

      Utils.clickApplyAndCloseButton(bot);
   }
}
