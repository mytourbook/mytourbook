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

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourBlogViewTests extends UITest {

   private SWTBotView getTourBlogView() {

      Utils.showViewFromMenu(bot, "Tour", Utils.VIEW_NAME_TOURBLOG); //$NON-NLS-1$
      return Utils.showView(bot, Utils.VIEW_NAME_TOURBLOG);
   }

   @Test
   void testBlogView_Basic() {

      Utils.getTour(bot);

      final SWTBotView tourBlogView = getTourBlogView();
      tourBlogView.show();

      //Change the measurement system to imperial
      Utils.changeMeasurementSystem(bot, net.tourbook.common.Messages.Measurement_System_Profile_Imperial);

      bot.sleep(5000);

      //Change back the measurement system to metric
      Utils.changeMeasurementSystem(bot, net.tourbook.common.Messages.Measurement_System_Profile_Metric);

      tourBlogView.close();
   }
}
