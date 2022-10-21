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

import net.tourbook.cloud.Messages;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageCloudTests extends UITest {

   @Test
   void openCloudPages() {

      Utils.openPreferences(bot);
      final SWTBotTreeItem cloudTreeItem = bot.tree().getTreeItem("Cloud").select(); //$NON-NLS-1$
      cloudTreeItem.expand();

      Utils.openVendorPage(cloudTreeItem, "Dropbox"); //$NON-NLS-1$
      openSuuntoPage(cloudTreeItem);
      Utils.openVendorPage(cloudTreeItem, "Strava"); //$NON-NLS-1$

      Utils.clickApplyAndCloseButton(bot);
   }

   private void openSuuntoPage(final SWTBotTreeItem cloudTreeItem) {

      Utils.openVendorPage(cloudTreeItem, "Suunto"); //$NON-NLS-1$

      bot.cTabItem(Messages.SuuntoCloud_Group_AccountInformation).activate();
      bot.cTabItem(Messages.SuuntoCloud_Group_FileNameCustomization).activate();
   }
}
