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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.Date;

import net.tourbook.Messages;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotDateTime;
import org.junit.jupiter.api.Test;

import utils.Utils;

public class TourDataEditorViewTests {

   private SWTWorkbenchBot bot = new SWTWorkbenchBot();

   @Test
   void testNewTour() {

      bot.toolbarButtonWithTooltip("Create new tour/event (Ctrl+N)").click(); //$NON-NLS-1$

      bot.cTabItem(Messages.tour_editor_tabLabel_tour).activate();

      final String newTourTitle = "New Tour Title"; //$NON-NLS-1$

      bot.comboBox().setText(newTourTitle);
      bot.toolbarButtonWithTooltip("Save modified tour (Ctrl+S)").click(); //$NON-NLS-1$

      Utils.showView(bot, "Tour Editor"); //$NON-NLS-1$

      final SWTBotCombo titleCombo = bot.comboBox(newTourTitle);
      assertNotNull(titleCombo);
      assertEquals(newTourTitle, titleCombo.getText());

      final SWTBotDateTime tourDateTime = bot.dateTimeWithLabel(Messages.tour_editor_label_tour_date);
      tourDateTime.setDate(Date.from(Instant.now()));
      assertNotNull(tourDateTime);

      bot.toolbarButtonWithTooltip("Save modified tour (Ctrl+S)").click(); //$NON-NLS-1$
   }

   @Test
   void testViewTabs() {

      Utils.showView(bot, "Tour Editor"); //$NON-NLS-1$

      bot.cTabItem(Messages.tour_editor_tabLabel_tour_data).activate();
      bot.cTabItem(Messages.Tour_Editor_TabLabel_SwimSlices).activate();
   }
}
