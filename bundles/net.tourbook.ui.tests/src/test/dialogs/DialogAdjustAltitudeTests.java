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
package dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tourbook.Messages;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class DialogAdjustAltitudeTests extends UITest {

   private void adjustAltitude(final String newElevationValue, final String adjustmentMethod) {

      Utils.getTourWithSRTM(bot);

      bot.viewByTitle(Utils.TOUREDITOR_VIEW_NAME).show();
      bot.toolbarButtonWithTooltip(Messages.app_action_edit_adjust_altitude).click();

      bot.comboBox(0).setSelection(adjustmentMethod);

      bot.button(Messages.adjust_altitude_btn_reset_altitude).click();
      bot.button(Messages.adjust_altitude_btn_update_altitude).click();

      if (adjustmentMethod.equals(Messages.adjust_altitude_type_adjust_height)) {
         bot.spinner(2).setSelection(300);
      } else if (adjustmentMethod.equals(Messages.adjust_altitude_type_adjust_end)) {
         bot.spinner(1).setSelection(200);
      } else if (adjustmentMethod.equals(Messages.adjust_altitude_type_adjust_whole_tour)) {
         bot.spinner(0).setSelection(1000);
      }

      bot.button(Messages.adjust_altitude_btn_update_modified_tour).click();
      bot.toolbarButtonWithTooltip(Utils.SAVE_MODIFIED_TOUR).click();

      final SWTBotTreeItem tour = Utils.getTourWithSRTM(bot);
      assertEquals(newElevationValue, tour.cell(tourBookView_ElevationGain_Column_Index));
   }

   @Test
   void testAdjustAltitude() {

      Utils.showTourBookView(bot);

      adjustAltitude("658", Messages.adjust_altitude_type_adjust_whole_tour); //$NON-NLS-1$
      adjustAltitude("687", Messages.adjust_altitude_type_start_and_end); //$NON-NLS-1$
      adjustAltitude("2,960", Messages.adjust_altitude_type_adjust_height); //$NON-NLS-1$
      adjustAltitude("2,578", Messages.adjust_altitude_type_adjust_end); //$NON-NLS-1$

      // This is necessary as otherwise the subsequent tests will fail with
      // org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException: Could not find menu bar for shell: Shell with text {}
      Utils.getTour(bot);
   }
}
