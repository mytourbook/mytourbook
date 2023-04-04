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

import net.tourbook.Messages;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourLogViewTests extends UITest {

   @Test
   void test_ViewOptions() {

      //Opening the tour log view
      final SWTBotView tourLogView = Utils.showView(bot, "Tour Log"); //$NON-NLS-1$

      SWTBotTreeItem tour = Utils.getTour(bot);

      triggerTourAction(tour);

      //Switching to a different layout
      bot.toolbarButtonWithTooltip(Messages.Tour_Log_Action_TourLogLayout_Tooltip).click();

      tour = Utils.getTour(bot);
      triggerTourAction(tour);

      //Clearing the log view
      bot.toolbarButtonWithTooltip(Messages.Tour_Log_Action_Clear_Tooltip).click();

      //Reverting to the original layout
      bot.toolbarButtonWithTooltip(Messages.Tour_Log_Action_TourLogLayout_Tooltip).click();

      tourLogView.close();
   }

   //Triggering an action to display an entry in the log view
   private void triggerTourAction(final SWTBotTreeItem tour) {

      tour.contextMenu(Messages.Tour_Action_AdjustTourValues)
            .menu(Messages.tour_editor_section_weather)
            .menu(Messages.Tour_Action_RetrieveWeatherData).click();
   }
}
