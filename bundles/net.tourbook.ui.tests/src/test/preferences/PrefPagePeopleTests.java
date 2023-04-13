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

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.measurement_system.MeasurementSystem_Manager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPagePeopleTests extends UITest {

   @AfterAll
   static void cleanUp() {
      setMetricSystem();
   }

   private static void setMetricSystem() {

      MeasurementSystem_Manager.setActiveSystemProfileIndex(0, true);
      UI.updateUnits();
   }

   @Test
   void addHRZones() {

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("People").select(); //$NON-NLS-1$
      bot.cTabItem(Messages.Pref_People_Tab_HRZone).activate();

      bot.comboBox(1).setSelection(1);
      bot.button(Messages.Dialog_HRZone_Button_EditHrZones).click();
      Utils.clickOkButton(bot);
      Utils.clickApplyAndCloseButton(bot);
      bot.button("Yes").click(); //$NON-NLS-1$
      Utils.clickOkButton(bot);
   }

   @Test
   void openPeoplePage() {

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("People").select(); //$NON-NLS-1$

      bot.cTabItem(Messages.Pref_People_Tab_Person).activate();
      bot.cTabItem(Messages.Pref_People_Tab_HRZone).activate();
      bot.cTabItem(Messages.Pref_People_Tab_DataTransfer).activate();

      Utils.clickApplyAndCloseButton(bot);
   }

   private void setDistanceNauticalMile() {

      MeasurementSystem_Manager.setActiveSystemProfileIndex(2, true);
      UI.updateUnits();
   }

   private void setImperialSystem() {

      MeasurementSystem_Manager.setActiveSystemProfileIndex(1, true);
      UI.updateUnits();
   }

   @Test
   @DisplayName("Verify the BMI")
   void testPeopleBmi() {

      //Metric system
      setMetricSystem();

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("People").select(); //$NON-NLS-1$

      bot.cTabItem(Messages.Pref_People_Tab_Person).activate();

      //70kg
      bot.spinner(0).setSelection(700);
      //1.80m
      bot.spinner(1).setSelection(180);

      //21.6 BMI
      assertEquals("21.6", bot.text(3).getText()); //$NON-NLS-1$

//      bot.cTabItem(Messages.Pref_People_Tab_HRZone).activate();
//      bot.cTabItem(Messages.Pref_People_Tab_DataTransfer).activate();

      Utils.clickApplyAndCloseButton(bot);

      // Imperial system
      setImperialSystem();

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("People").select(); //$NON-NLS-1$

      bot.cTabItem(Messages.Pref_People_Tab_Person).activate();
      //22.9 BMI
      assertEquals("22.9", bot.text(3).getText()); //$NON-NLS-1$

      Utils.clickApplyAndCloseButton(bot);

      // Nautical mile system
      setDistanceNauticalMile();

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("People").select(); //$NON-NLS-1$

      bot.cTabItem(Messages.Pref_People_Tab_Person).activate();
      //21.6 BMI
      assertEquals("21.6", bot.text(3).getText()); //$NON-NLS-1$

      Utils.clickApplyAndCloseButton(bot);
   }
}
