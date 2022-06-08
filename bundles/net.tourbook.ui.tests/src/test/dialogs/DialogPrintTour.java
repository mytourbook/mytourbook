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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

public class DialogPrintTour {

   private SWTWorkbenchBot bot              = new SWTWorkbenchBot();

   private String          workingDirectory = System.getProperty("user.dir");

   private SWTBotTreeItem getTour() {

      final SWTBotView tourBookView = bot.viewByTitle("Tour Book");
      assertNotNull(tourBookView);
      tourBookView.show();

      bot.toolbarButtonWithTooltip("&Collapse All").click();

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2021   2").expand()
            .getNode("Jan   2").expand().select().getNode("31").select();
      assertNotNull(tour);

      return tour;
   }

   @Test
   void testPrintTour() {

      final SWTBotTreeItem tour = getTour();

      tour.contextMenu("Print Tour").menu("PDF").click();
      bot.checkBox("Print Markers").click();
      bot.checkBox("Print Description").click();

      final String fileName = bot.comboBox(2).getText() + ".pdf";
      bot.comboBox(3).setText(workingDirectory);
      bot.button("Print ").click();

      final Path pdfFilePath = Paths.get(workingDirectory, fileName);
      assertTrue(Files.exists(pdfFilePath));
   }
}
