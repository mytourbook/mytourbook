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

import net.tourbook.common.util.FileUtils;
import net.tourbook.printing.Messages;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class DialogPrintTourTests extends UITest {

   @Test
   void testPrintTour() {

      final SWTBotTreeItem tour = Utils.getTour(bot);

      tour.contextMenu(net.tourbook.Messages.action_print_tour).menu("PDF").click(); //$NON-NLS-1$
      bot.checkBox(Messages.Dialog_Print_Chk_PrintMarkers).click();
      bot.checkBox(Messages.Dialog_Print_Chk_PrintNotes).click();

      final String fileName = bot.comboBox(2).getText() + ".pdf"; //$NON-NLS-1$
      bot.comboBox(3).setText(Utils.workingDirectory);
      bot.button(Messages.Dialog_Print_Btn_Print).click();

      final Path pdfFilePath = Paths.get(Utils.workingDirectory, fileName);
      assertTrue(Files.exists(pdfFilePath));

      FileUtils.deleteIfExists(pdfFilePath);
      assertTrue(!Files.exists(pdfFilePath));
   }
}
