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

import de.byteholder.geoclipse.preferences.Messages;

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
      bot.button("&Save").click(); //$NON-NLS-1$

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
      bot.button("Cancel").click(); //$NON-NLS-1$

      Utils.clickApplyAndCloseButton(bot);
   }

   @Test
   void importWMS() {

      Utils.openPreferences(bot);

      selectMapProviderPreferencePage();

      SWTBotTable providersTable = bot.table();
      assertEquals(2, providersTable.rowCount());

      bot.button(Messages.Pref_Map_Button_AddMapProviderWms).click();
      bot.textWithLabel(Messages.Pref_Map_Dialog_WmsInput_Message).setText("https://ahocevar.com/geoserver/wms?SERVICE=WMS&REQUEST=GetCapabilities"); //$NON-NLS-1$
      Utils.clickOkButton(bot);

      providersTable = bot.table();

      assertEquals(3, providersTable.rowCount());
      assertEquals("GeoServer Web Map Service", providersTable.cell(0, 0)); //$NON-NLS-1$

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
