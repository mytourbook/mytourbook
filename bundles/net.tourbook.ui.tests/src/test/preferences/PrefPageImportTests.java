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

import net.tourbook.device.garmin.fit.Messages;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageImportTests extends UITest {

   private void openFitPage(final SWTBotTreeItem importTreeItem) {

      Utils.openVendorPage(importTreeItem, "Fit"); //$NON-NLS-1$

      bot.cTabItem(Messages.PrefPage_Fit_Group_Speed).activate();
      bot.cTabItem(Messages.PrefPage_Fit_Group_AdjustTemperature).activate();
      bot.cTabItem(Messages.PrefPage_Fit_Group_IgnoreLastMarker).activate();
      bot.cTabItem(Messages.PrefPage_Fit_Group_ReplaceTimeSlice).activate();
      bot.cTabItem(Messages.PrefPage_Fit_Group_Power).activate();
      bot.cTabItem(Messages.PrefPage_Fit_Group_TourType).activate();
   }

   @Test
   void openImportPages() {

      Utils.openPreferences(bot);
      final SWTBotTreeItem importTreeItem = bot.tree().getTreeItem("Import").select(); //$NON-NLS-1$
      importTreeItem.expand();

      Utils.openVendorPage(importTreeItem, "Daum Ergometer"); //$NON-NLS-1$
      openFitPage(importTreeItem);
      Utils.openVendorPage(importTreeItem, "GPX"); //$NON-NLS-1$
      Utils.openVendorPage(importTreeItem, "HAC 4/5"); //$NON-NLS-1$
      Utils.openVendorPage(importTreeItem, "Polar"); //$NON-NLS-1$
      Utils.openVendorPage(importTreeItem, "Suunto Spartan/9"); //$NON-NLS-1$
      Utils.openVendorPage(importTreeItem, "TCX"); //$NON-NLS-1$

      Utils.clickApplyAndCloseButton(bot);
   }
}
