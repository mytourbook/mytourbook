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
package basic;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

public class TourDataEditorViewTests {

   private SWTWorkbenchBot bot = new SWTWorkbenchBot();

   @BeforeClass
   public static void beforeClass() {

      SWTBotPreferences.TIMEOUT = 10000;
   }

   @Test
   void testNewTourTitle() {

      final SWTBotView tourEditorView = bot.viewByTitle("Tour Editor");
      assertNotNull(tourEditorView);
      tourEditorView.show();

      bot.comboBox().setText("New Title");
      bot.toolbarButtonWithTooltip("Save modified tour (Ctrl+S)").click();
   }

}
