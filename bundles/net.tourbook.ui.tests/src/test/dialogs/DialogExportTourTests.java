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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.tourbook.Messages;
import net.tourbook.common.util.FilesUtils;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class DialogExportTourTests extends UITest {

   @Test
   void testExportGpx() {

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(Messages.action_export_tour).menu("GPX").click(); //$NON-NLS-1$

      bot.checkBox(Messages.Dialog_Export_Checkbox_Description).click();
      bot.checkBox(Messages.dialog_export_chk_exportMarkers).click();
      bot.checkBox(Messages.Dialog_Export_Checkbox_TourFields).click();
      bot.checkBox(Messages.Dialog_Export_Checkbox_SurfingWaves).click();
      bot.checkBox(Messages.dialog_export_chk_camouflageSpeed).click();
      bot.checkBox(Messages.Dialog_Export_Checkbox_WithBarometer).click();
      bot.checkBox(Messages.dialog_export_chk_overwriteFiles).click();

      final String fileName = bot.comboBox(0).getText() + ".gpx"; //$NON-NLS-1$
      bot.comboBox(1).setText(Utils.workingDirectory);
      bot.button(Messages.dialog_export_btn_export).click();

      final Path gpxFilePath = Paths.get(Utils.workingDirectory, fileName);
      assertTrue(Files.exists(gpxFilePath));

      FilesUtils.deleteIfExists(gpxFilePath);
      assertTrue(!Files.exists(gpxFilePath));
   }

   @Test
   void testExportMt() {

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(Messages.action_export_tour).menu("MyTourbook (.mt)").click(); //$NON-NLS-1$
      final String fileName = bot.comboBox(0).getText() + ".mt"; //$NON-NLS-1$
      bot.comboBox(1).setText(Utils.workingDirectory);
      bot.button(Messages.dialog_export_btn_export).click();

      final Path mtFilePath = Paths.get(Utils.workingDirectory, fileName);
      assertTrue(Files.exists(mtFilePath));

      FilesUtils.deleteIfExists(mtFilePath);
      assertTrue(!Files.exists(mtFilePath));
   }

   @Test
   void testExportTcx() {

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(Messages.action_export_tour).menu("TCX").click(); //$NON-NLS-1$
      bot.radio(Messages.Dialog_Export_Radio_TCX_Activities).click();
      bot.checkBox(Messages.dialog_export_chk_exportNotes).click();
      bot.checkBox(Messages.dialog_export_chk_camouflageSpeed).click();

      final String fileName = bot.comboBox(2).getText() + ".tcx"; //$NON-NLS-1$
      bot.comboBox(3).setText(Utils.workingDirectory);
      bot.button(Messages.dialog_export_btn_export).click();

      final Path tcxFilePath = Paths.get(Utils.workingDirectory, fileName);
      assertTrue(Files.exists(tcxFilePath));

      FilesUtils.deleteIfExists(tcxFilePath);
      assertTrue(!Files.exists(tcxFilePath));
   }
}
