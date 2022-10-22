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

import net.tourbook.Messages;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageGeneralTests extends UITest {

   private SWTBotTreeItem getGeneralTreeItem() {

      return bot.tree().getTreeItem("General");
   }

   @Test
   void openComputedValuesPage() {

      Utils.openPreferences(bot);
      getGeneralTreeItem().expand().getNode("Computed Values").select();

      bot.cTabItem(Messages.Compute_Values_Group_Smoothing).activate();
      bot.cTabItem(Messages.Compute_BreakTime_Group_BreakTime).activate();
      bot.cTabItem(Messages.Pref_Appearance_Group_PaceAndSpeedDisplay).activate();
      bot.cTabItem(Messages.compute_tourValueElevation_group_computeTourAltitude).activate();
      bot.cTabItem(Messages.Compute_CadenceZonesTimes_Group).activate();
      bot.cTabItem(Messages.Compute_HrZone_Group).activate();

      Utils.clickApplyAndCloseButton(bot);
   }

   @Test
   void openGeneralPage() {

      Utils.openPreferences(bot);
      getGeneralTreeItem().select();

      bot.cTabItem(Messages.Pref_general_system_measurement).activate();
      bot.cTabItem(Messages.Pref_General_Group_TimeZone).activate();
      bot.cTabItem(Messages.Pref_General_CalendarWeek).activate();
      bot.cTabItem(Messages.Pref_General_CalendarWeek).activate();

      Utils.clickApplyAndCloseButton(bot);
   }

   @Test
   void openTourPage() {

      Utils.openPreferences(bot);
      getGeneralTreeItem().expand().getNode("Tour").select();

      Utils.clickApplyAndCloseButton(bot);
   }
}
