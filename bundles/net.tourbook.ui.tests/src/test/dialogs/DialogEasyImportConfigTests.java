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
package dialogs;

import net.tourbook.Messages;

import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class DialogEasyImportConfigTests extends UITest {

   @Test
   void configureEasyImport() {

      bot.toolbarButtonWithTooltip("Tour Import (Ctrl+Shift+I)").click(); //$NON-NLS-1$
      Utils.showView(bot, "Tour Import"); //$NON-NLS-1$

      bot.toolbarButtonWithTooltip(Messages.Import_Data_Action_SetupEasyImport_Tooltip).click();

      bot.cTabItem(Messages.Dialog_ImportConfig_Tab_Launcher).activate();

      bot.checkBox(Messages.Dialog_ImportConfig_Checkbox_TourType).select();
      bot.comboBox(0).setSelection(Messages.Import_Data_TourTypeConfig_BySpeed);
      bot.checkBox(Messages.Dialog_ImportConfig_Checkbox_LastMarker).select();
      bot.checkBox(Messages.Dialog_ImportConfig_Checkbox_AdjustTemperature).select();
      bot.checkBox(Messages.Dialog_ImportConfig_Checkbox_ReplaceFirstTimeSliceElevation).select();
      bot.checkBox(Messages.Dialog_ImportConfig_Checkbox_ReplaceElevationFromSRTM).select();
      bot.checkBox(Messages.Dialog_ImportConfig_Checkbox_RetrieveWeatherData).select();
      bot.checkBox(Messages.Dialog_ImportConfig_Checkbox_SaveTour).select();

      Utils.clickOkButton(bot);
   }

   @Test
   void openEasyImportConfig() {

      bot.toolbarButtonWithTooltip("Tour Import (Ctrl+Shift+I)").click(); //$NON-NLS-1$
      Utils.showView(bot, "Tour Import"); //$NON-NLS-1$

      bot.toolbarButtonWithTooltip(Messages.Import_Data_Action_SetupEasyImport_Tooltip).click();

      bot.cTabItem(Messages.Dialog_ImportConfig_Tab_Configuration).activate();
      bot.cTabItem(Messages.Dialog_ImportConfig_Tab_Options).activate();

      Utils.clickOkButton(bot);
   }
}
