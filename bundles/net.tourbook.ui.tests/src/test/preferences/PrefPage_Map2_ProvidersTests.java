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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.byteholder.geoclipse.preferences.Messages;

import net.tourbook.common.UI;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPage_Map2_ProvidersTests extends UITest {

   @Test
   void createCustom() {

      Utils.openPreferences(bot);

      selectMapProviderPreferencePage();

      bot.button(Messages.Pref_Map_Button_AddMapProviderCustom).click();
      bot.textWithLabel(Messages.Pref_Map_Label_MapProvider).setText("Custom Profile 1"); //$NON-NLS-1$
      bot.button(Messages.Pref_Map_Button_UpdateMapProvider).click();
      bot.button(Messages.Pref_Map_Button_Edit).click();
      bot.button(de.byteholder.geoclipse.Messages.Dialog_MapConfig_Button_Save).click();

      Utils.clickApplyAndCloseButton(bot);
   }

   @Test
   void createProfile() {

      Utils.openPreferences(bot);

      selectMapProviderPreferencePage();

      bot.button(Messages.Pref_Map_Button_AddMapProfile).click();
      bot.textWithLabel(Messages.Pref_Map_Label_MapProvider).setText("Profile 1"); //$NON-NLS-1$
      bot.button(Messages.Pref_Map_Button_UpdateMapProvider).click();
      bot.button(Messages.Pref_Map_Button_Edit).click();
      bot.tree().getTreeItem("OpenStreetMap").select(); //$NON-NLS-1$
      Utils.clickCancelButton(bot);

      Utils.clickApplyAndCloseButton(bot);
   }

   @Test
   void importWMS() {

      Utils.openPreferences(bot);

      selectMapProviderPreferencePage();

      //Check the number of providers before adding a new one
      SWTBotTable providersTable = bot.table();
      final int providersTableCount = 2;
      assertEquals(providersTableCount, providersTable.rowCount());

      //Add a new WMS provider
      bot.button(Messages.Pref_Map_Button_AddMapProviderWms).click();
      bot.textWithLabel(Messages.Pref_Map_Dialog_WmsInput_Message).setText("https://ahocevar.com/geoserver/wms?SERVICE=WMS&REQUEST=GetCapabilities"); //$NON-NLS-1$
      Utils.clickOkButton(bot);

      providersTable = bot.table();

      //Check the new number of providers after adding a new one
      assertEquals(providersTableCount + 1, providersTable.rowCount());
      assertEquals("GeoServer Web Map Service", providersTable.cell(0, 0)); //$NON-NLS-1$

      providersTable.select(0);
      bot.button(Messages.Pref_Map_Button_Edit).click();

      final SWTBotTable wmsMapProviderTable = bot.table();
      wmsMapProviderTable.getTableItem("Natural Earth Base Map").check(); //$NON-NLS-1$
      assertEquals(12, wmsMapProviderTable.rowCount());
      bot.button(de.byteholder.geoclipse.Messages.Dialog_WmsConfig_Button_UpdateMap).click();
      bot.button(de.byteholder.geoclipse.Messages.Dialog_MapConfig_Button_ShowOsmMap).click();

      final SWTBotCheckBox showTileInfoCheckBox = bot.checkBox(de.byteholder.geoclipse.Messages.Dialog_MapConfig_Button_ShowTileInfo);
      assertFalse(showTileInfoCheckBox.isChecked());
      showTileInfoCheckBox.click();
      assertTrue(showTileInfoCheckBox.isChecked());
      final SWTBotCheckBox showTileLogCheckBox = bot.checkBox(de.byteholder.geoclipse.Messages.Dialog_MapConfig_Button_ShowTileLog);
      assertFalse(showTileLogCheckBox.isChecked());
      showTileLogCheckBox.click();
      assertTrue(showTileLogCheckBox.isChecked());

      if (UI.IS_WIN) {
         //The test below fails in Linux. It seems like the window becomes
         //out of focus and any future action is dismissed
         final SWTBotCheckBox loadTransparentImagesCheckBox = bot.checkBox(
               de.byteholder.geoclipse.Messages.Dialog_WmsConfig_Button_GetTransparentMap);
         assertFalse(loadTransparentImagesCheckBox.isChecked());
         loadTransparentImagesCheckBox.click();
         assertTrue(loadTransparentImagesCheckBox.isChecked());
      }

      wmsMapProviderTable.getTableItem("NE1_HR_LC_SR_W_DR").check(); //$NON-NLS-1$
      bot.comboBox(2).setSelection(0);

      Utils.clickCancelButton(bot);

      //Deleting the new WMS provider
      providersTable = bot.table();
      providersTable.select(0);
      bot.button(Messages.Pref_Map_Button_DeleteMapProvider).click();
      Utils.clickOkButton(bot);

      Utils.clickApplyAndCloseButton(bot);
   }

   @Test
   void openMapProviderPreferencePage() {

      Utils.openPreferences(bot);
      selectMapProviderPreferencePage();

      Utils.clickApplyAndCloseButton(bot);
   }

//   @Test
//   void openOfflineMapPreferencePage() {
//
//      Utils.openPreferences(bot);
//      selectOfflineMapPreferencePage();
//
//      Utils.clickCancelButton(bot);
//   }

   private void selectMapProviderPreferencePage() {

      bot.tree().getTreeItem("2D Map").expand().getNode("Map Provider").select(); //$NON-NLS-1$ //$NON-NLS-2$
   }

//   private void selectOfflineMapPreferencePage() {
//
//      bot.tree().getTreeItem("2D Map").expand().getNode("Offline Map").select(); //$NON-NLS-1$
//   }
}
