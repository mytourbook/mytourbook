/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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

import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageStatisticsTests extends UITest {

   @Test
   void PrefPageStatistics_MoveAndSortStatistics() {

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("Statistics").select(); //$NON-NLS-1$

      bot.table().select(Messages.Pref_Statistic_Group_YearSummary);
      bot.button(Messages.app_action_button_down).click();
      bot.button(Messages.app_action_button_up).click();
      bot.button(Messages.Pref_Statistic_Action_SortByData).click();
      bot.button(Messages.Pref_Statistic_Action_SortByTime).click();

      Utils.clickApplyAndCloseButton(bot);
   }
}
