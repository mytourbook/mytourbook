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

import net.tourbook.srtm.Messages;

import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageSRTMDataTests extends UITest {

   @Test
   void editSRTMProfile() {

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("SRTM").select(); //$NON-NLS-1$

      bot.button(Messages.prefPage_srtm_profile_edit).click();
      Utils.clickCancelButton(bot);

      bot.button(Messages.prefPage_srtm_btn_adjust_columns).click();
      Utils.clickCancelButton(bot);

      Utils.clickApplyAndCloseButton(bot);
   }

   @Test
   void validateSRTMConnection() {

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("SRTM").expand().getNode("Data").select(); //$NON-NLS-1$ //$NON-NLS-2$

      bot.button(Messages.PrefPage_SRTMData_Button_ValidateDownloadOfSrtmData).click();

      Utils.clickOkButton(bot);

      bot.button(Messages.PrefPage_SRTMData_Button_SrtmDummyValidation).click();

      Utils.clickApplyAndCloseButton(bot);
   }
}
