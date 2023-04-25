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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourPausesViewTests extends UITest {

   @Test
   void testTourPausesView() {

      Utils.getTour(bot);

      Utils.showView(bot, "Tour Pauses"); //$NON-NLS-1$

      bot.table().select("15:40"); //$NON-NLS-1$
   }

   @Test
   void When_PauseTypeSetToManual_Expect_ExpectedManualPauseType() {

      // arrange
      Utils.getTour(bot);
      Utils.showView(bot, "Tour Pauses"); //$NON-NLS-1$
      final SWTBotTable pausesViewTable = bot.table();
      pausesViewTable.select(0);

      // act
      pausesViewTable.contextMenu("Set pauses type").menu("automatic").click();

      // assert
      assertEquals("", pausesViewTable.cell(0, 0));

   }
}
