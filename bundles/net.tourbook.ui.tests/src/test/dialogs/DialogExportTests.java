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

public class DialogExportTests {

   private static final String EXPORT_TOUR      = "Export Tour";

   private static final String EXPORT           = "Export";

   private SWTWorkbenchBot     bot              = new SWTWorkbenchBot();

   private String              WorkingDirectory = System.getProperty("user.dir");

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
   void testExportGpx() {

      final SWTBotTreeItem tour = getTour();

      tour.contextMenu(EXPORT_TOUR).menu("GPX").click();
      final String fileName = bot.comboBox(0).getText() + ".gpx";
      bot.comboBox(1).setText(WorkingDirectory);
      bot.button(EXPORT).click();

      final Path gpxFilePath = Paths.get(WorkingDirectory, fileName);
      assertTrue(Files.exists(gpxFilePath));
   }

   @Test
   void testExportMt() {

      final SWTBotTreeItem tour = getTour();

      tour.contextMenu(EXPORT_TOUR).menu("MyTourbook (.mt)").click();
      final String fileName = bot.comboBox(0).getText() + ".mt";
      bot.comboBox(1).setText(WorkingDirectory);
      bot.button(EXPORT).click();

      final Path mtFilePath = Paths.get(WorkingDirectory, fileName);
      assertTrue(Files.exists(mtFilePath));
   }

   @Test
   void testExportTcx() {

      final SWTBotTreeItem tour = getTour();

      tour.contextMenu(EXPORT_TOUR).menu("TCX").click();
      bot.radio("Activities").click();
      final String fileName = bot.comboBox(2).getText() + ".tcx";
      bot.comboBox(3).setText(WorkingDirectory);
      bot.button(EXPORT).click();

      final Path tcxFilePath = Paths.get(WorkingDirectory, fileName);
      assertTrue(Files.exists(tcxFilePath));
   }
}
