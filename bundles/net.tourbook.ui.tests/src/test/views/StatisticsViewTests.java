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
package views;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.tourbook.Messages;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class StatisticsViewTests extends UITest {

   @Test
   public void testStatisticsView() {

      bot.toolbarButtonWithTooltip("Statistics (Ctrl+Shift+S)").click(); //$NON-NLS-1$
      final SWTBotView statisticsView = Utils.showView(bot, Utils.STATISTICS_VIEW_NAME);
      final SWTBot statisticsViewBot = statisticsView.bot();
      bot.sleep(3000);

      final SWTBotCombo statisticsTypeComboBox = statisticsViewBot.comboBox(0);
      assertEquals(31, statisticsTypeComboBox.itemCount());
      assertNotNull(statisticsTypeComboBox);
      statisticsTypeComboBox.setSelection(0);
      assertEquals("Daytime", statisticsTypeComboBox.selection()); //$NON-NLS-1$

      final SWTBotCombo yearComboBox = statisticsViewBot.comboBox(1);

      final SWTBotCombo numYearComboBox = statisticsViewBot.comboBox(2);
      assertAll(
            () -> assertNotNull(numYearComboBox),
            () -> assertEquals(yearComboBox.itemCount(), numYearComboBox.itemCount()));

      numYearComboBox.setSelection("9"); //$NON-NLS-1$
      assertEquals("9", numYearComboBox.selection()); //$NON-NLS-1$

      statisticsTypeComboBox.setSelection(Messages.Slideout_StatisticOptions_Group_DaySummary);
      statisticsTypeComboBox.setSelection(Messages.Slideout_StatisticOptions_Group_WeekSummary);
      statisticsTypeComboBox.setSelection(Messages.Slideout_StatisticOptions_Group_MonthSummary);
      statisticsTypeComboBox.setSelection(Messages.Slideout_StatisticOptions_Group_YearSummary);
      statisticsTypeComboBox.setSelection(Messages.Slideout_StatisticOptions_Group_TourFrequency);
      statisticsTypeComboBox.setSelection("HR Zones - Week"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("HR Zones - Month"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Training - Line"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Training - Bar"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Tour Time - Day"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Tour Time - Week"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Tour Time - Month"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Tour Time - Year"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Distance - Day"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Distance - Week"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Distance - Month"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Distance - Year"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Elevation Up - Day"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Elevation Up - Week"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Elevation Up - Month"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Elevation Up - Year"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Elevation Down - Day"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Elevation Down - Week"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Elevation Down - Month"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Elevation Down - Year"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Athlete's Data - Day"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Athlete's Data - Week"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Athlete's Data - Month"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Athlete's Data - Year"); //$NON-NLS-1$
      statisticsTypeComboBox.setSelection("Battery SoC"); //$NON-NLS-1$

      statisticsView.close();
   }
}
