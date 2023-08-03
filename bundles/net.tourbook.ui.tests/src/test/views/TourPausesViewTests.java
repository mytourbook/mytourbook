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

import net.tourbook.Messages;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourPausesViewTests extends UITest {

   @Test
   void Given_Pauses_When_DeletePause_Expect_PauseDeleted() {

      // arrange
      Utils.getTourWithSeveralPauses(bot);
      Utils.showView(bot, Utils.TOURPAUSES_VIEW_NAME);
      SWTBotTable pausesViewTable = bot.table();
      pausesViewTable.select(0);

      // assert initial state
      final int tableRowCount = 5;
      assertEquals(tableRowCount, pausesViewTable.rowCount());

      // act

      // Triggering the deletion but not acting yet
      pausesViewTable.pressShortcut(KeyStroke.getInstance(0, SWT.DEL));
      Utils.clickNoButton(bot);

      // Triggering the deletion and performing it
      pausesViewTable
            .contextMenu(Messages.App_Action_DeleteTourPauses)
            .click();
      Utils.clickYesButton(bot);

      // assert new state
      pausesViewTable = bot.table();
      assertEquals(tableRowCount - 1, pausesViewTable.rowCount());
   }

   @Test
   void Given_PauseTypeAutomatic_When_ChangeToManual_Expect_PauseTypeManual() {

      // arrange
      Utils.getTourWithSeveralPauses(bot);
      Utils.showView(bot, Utils.TOURPAUSES_VIEW_NAME);
      SWTBotTable pausesViewTable = bot.table();
      pausesViewTable.select(0);

      // assert initial state
      assertEquals(Messages.Tour_Pauses_Column_TypeValue_Automatic, pausesViewTable.cell(0, 1));

      // act
      pausesViewTable
            .contextMenu(Messages.Action_PauseType_Set)
            .menu(Messages.Action_PauseType_Set_Manual)
            .click();

      // assert new state
      pausesViewTable = bot.table();
      assertEquals(Messages.Tour_Pauses_Column_TypeValue_Manual, pausesViewTable.cell(0, 1));

      // clean-up
      pausesViewTable.select(0);
      pausesViewTable
            .contextMenu(Messages.Action_PauseType_Set)
            .menu(Messages.Action_PauseType_Set_Automatic)
            .click();
      pausesViewTable = bot.table();
      assertEquals(Messages.Tour_Pauses_Column_TypeValue_Automatic, pausesViewTable.cell(0, 1));
   }

   @Test
   void testTourPausesView() {

      Utils.getTour(bot);

      Utils.showView(bot, Utils.TOURPAUSES_VIEW_NAME);

      bot.table().select("15:40"); //$NON-NLS-1$
   }
}
