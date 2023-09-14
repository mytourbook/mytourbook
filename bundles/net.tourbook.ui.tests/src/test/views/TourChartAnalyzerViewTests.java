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
package views;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourChartAnalyzerViewTests extends UITest {

   private SWTBotView getTourAnalyzerView() {

      Utils.showViewFromMenu(bot, Utils.TOOLS, Utils.TOURANALYZER_VIEW_NAME);
      return Utils.showView(bot, Utils.TOURANALYZER_VIEW_NAME);
   }

   @Test
   void testAnalyzerView_Basic() {

      // It's important to select the tour first, as otherwise, the Tour Analyzer
      // view might not detect the tour
      final SWTBotTreeItem tour = bot.tree().getTreeItem("2015   1").expand() //$NON-NLS-1$
            .getNode("May   1").expand().select().getNode("31").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      Utils.showView(bot, "Tour Chart"); //$NON-NLS-1$

      final SWTBotView tourAnalyzerView = getTourAnalyzerView();

      //Change the measurement system to imperial
      Utils.changeMeasurementSystem(bot, net.tourbook.common.Messages.Measurement_System_Profile_Imperial);

      bot.sleep(5000);

      //Change back the measurement system to metric
      Utils.changeMeasurementSystem(bot, net.tourbook.common.Messages.Measurement_System_Profile_Metric);

      tourAnalyzerView.close();
   }
}
