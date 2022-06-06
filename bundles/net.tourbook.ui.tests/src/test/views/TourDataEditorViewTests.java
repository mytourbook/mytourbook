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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.junit.jupiter.api.Test;

public class TourDataEditorViewTests {

   private SWTWorkbenchBot bot = new SWTWorkbenchBot();

   @Test
   void testNewTour() {

      bot.toolbarButtonWithTooltip("Create new tour/event (Ctrl+N)").click();

      bot.cTabItem("Tour").activate();

      final String newTourTitle = "New Tour Title";

      bot.comboBox().setText(newTourTitle);
      bot.toolbarButtonWithTooltip("Save modified tour (Ctrl+S)").click();

      final SWTBotView tourEditorView = bot.viewByTitle("Tour Editor");
      assertNotNull(tourEditorView);
      tourEditorView.show();

//      final SWTBotCombo titleCombo = bot.comboBox(newTourTitle);
//      assertNotNull(titleCombo);
//      assertEquals(newTourTitle, titleCombo.getText());
   }

   @Test
   void testViewTabs() {

      final SWTBotView tourEditorView = bot.viewByTitle("Tour Editor");
      assertNotNull(tourEditorView);
      tourEditorView.show();

      bot.cTabItem("Time Slices").activate();
      bot.cTabItem("Swim Slices").activate();
   }
}
