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
package views;

import net.tourbook.Messages;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.junit.jupiter.api.Test;

import utils.Utils;

public class StatisticsViewTests {

   private static final String STATISTICS_VIEW_NAME = "Statistics";

   private SWTWorkbenchBot     bot                  = new SWTWorkbenchBot();

   @Test
   void testStatisticsView() {

      Utils.showView(bot, STATISTICS_VIEW_NAME);

      bot.comboBox(2).setSelection("9");
      bot.viewByTitle(STATISTICS_VIEW_NAME).show();
      bot.comboBox().setSelection("Daytime");
      bot.comboBox().setSelection(Messages.Pref_Statistic_Group_DaySummary);
      bot.comboBox().setSelection(Messages.Pref_Statistic_Group_WeekSummary);
      bot.comboBox().setSelection(Messages.Pref_Statistic_Group_MonthSummary);
      bot.comboBox().setSelection(Messages.Pref_Statistic_Group_YearSummary);
      bot.comboBox().setSelection(Messages.Pref_Statistic_Group_TourFrequency);
      bot.comboBox().setSelection("HR Zones - Week");
      bot.comboBox().setSelection("HR Zones - Month");
      bot.comboBox().setSelection("Training - Line");
      bot.comboBox().setSelection("Training - Bar");
      bot.comboBox().setSelection("Tour Time - Day");
      bot.comboBox().setSelection("Tour Time - Week");
      bot.comboBox().setSelection("Tour Time - Month");
      bot.comboBox().setSelection("Tour Time - Year");
      bot.comboBox().setSelection("Distance - Day");
      bot.comboBox().setSelection("Distance - Week");
      bot.comboBox().setSelection("Distance - Month");
      bot.comboBox().setSelection("Distance - Year");
      bot.comboBox().setSelection("Elevation - Day");
      bot.comboBox().setSelection("Elevation - Week");
      bot.comboBox().setSelection("Elevation - Month");
      bot.comboBox().setSelection("Elevation - Year");
      bot.comboBox().setSelection("Athlete's Data - Day");
      bot.comboBox().setSelection("Athlete's Data - Week");
      bot.comboBox().setSelection("Athlete's Data - Month");
      bot.comboBox().setSelection("Athlete's Data - Year");
      bot.comboBox().setSelection("Battery SoC");
   }
}
