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
import utils.Utils;

public class PrefPage_Map25_ProvidersTests extends UITest {

   private void openMapProviderPreferencePage() {

      bot.tree().getTreeItem("2.5D Map").expand().getNode("Map Provider").select(); //$NON-NLS-1$
   }

   @Test
   void openPreferencePage() {

      Utils.openPreferences(bot);
      openMapProviderPreferencePage();

      Utils.clickApplyAndCloseButton(bot);
   }
}
