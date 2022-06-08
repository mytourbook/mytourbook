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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.Utils;

public class DialogExportTourTests {

   private static final String EXPORT_TOUR      = "Export Tour";

   private static final String EXPORT           = "Export";

   private SWTWorkbenchBot     bot              = new SWTWorkbenchBot();

   private String              workingDirectory = System.getProperty("user.dir");

   @Test
   void testExportGpx() {

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(EXPORT_TOUR).menu("GPX").click();

      bot.checkBox("Description and Title").click();
      bot.checkBox("Markers and WayPoints").click();
      bot.checkBox("Tour fields").click();
      bot.checkBox("Only surfed Waves").click();
      bot.checkBox("Camouflage Speed").click();
      bot.checkBox("\"creator\" \" with barometer\"").click();
      bot.checkBox("Overwrite existing file(s)").click();

      final String fileName = bot.comboBox(0).getText() + ".gpx";
      bot.comboBox(1).setText(workingDirectory);
      bot.button(EXPORT).click();

      final Path gpxFilePath = Paths.get(workingDirectory, fileName);
      assertTrue(Files.exists(gpxFilePath));
   }

   @Test
   void testExportMt() {

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(EXPORT_TOUR).menu("MyTourbook (.mt)").click();
      final String fileName = bot.comboBox(0).getText() + ".mt";
      bot.comboBox(1).setText(workingDirectory);
      bot.button(EXPORT).click();

      final Path mtFilePath = Paths.get(workingDirectory, fileName);
      assertTrue(Files.exists(mtFilePath));
   }

   @Test
   void testExportTcx() {

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(EXPORT_TOUR).menu("TCX").click();
      bot.radio("Activities").click();
      bot.checkBox("Description").click();
      bot.checkBox("Camouflage Speed").click();

      final String fileName = bot.comboBox(2).getText() + ".tcx";
      bot.comboBox(3).setText(workingDirectory);
      bot.button(EXPORT).click();

      final Path tcxFilePath = Paths.get(workingDirectory, fileName);
      assertTrue(Files.exists(tcxFilePath));
   }
}
