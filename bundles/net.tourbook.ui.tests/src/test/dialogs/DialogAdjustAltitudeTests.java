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

   @Test
   void testAdjustAltitude() {

      Utils.showTourBookView(bot);

      //Select a tour for which we have SRTM3 data
      final SWTBotTreeItem tour = bot.tree().getTreeItem("2013   1").expand() //$NON-NLS-1$
            .getNode("May   1").expand().select().getNode("18").select(); //$NON-NLS-1$ //$NON-NLS-2$

      bot.viewByTitle("Tour Editor").show();
      bot.toolbarButtonWithTooltip(Messages.app_action_edit_adjust_altitude).click();

      bot.button(Messages.adjust_altitude_btn_update_modified_tour).click();
      bot.toolbarButtonWithTooltip("Save modified tour (Ctrl+S)").click(); //$NON-NLS-1$

      //Check the new elevation gain value
      assertEquals("0.542", tour.cell(tourBookView_ElevationGain_Column_Index)); //$NON-NLS-1$

      // This is necessary as otherwise the subsequent tests will fail with
      // org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException: Could not find menu bar for shell: Shell with text {}
      Utils.getTour(bot);
   }
}
