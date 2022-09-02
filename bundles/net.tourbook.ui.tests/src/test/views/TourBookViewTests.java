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

import net.tourbook.Messages;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourBookViewTests extends UITest {

   /**
    * This test doesn't work because SWTBot doesn't support native dialogs
    * https://wiki.eclipse.org/SWTBot/FAQ#How_do_I_use_SWTBot_to_test_native_dialogs_.28File_Dialogs.2C_Color_Dialogs.2C_etc.29.3F
    */
//   @Test
//   void testExportTourBookView() {
//
//      Utils.showTourBookView(bot);
//
//      final SWTBotTreeItem tour = Utils.getTour(bot);
//
//      tour.contextMenu(Messages.App_Action_ExportViewCSV).click();
//
//      bot.button("Save").click();
//
//      final Path csvFilePath = Paths.get(Utils.workingDirectory, "TourBook_2022-08-30_21-39-05.csv");
//      assertTrue(Files.exists(csvFilePath));
//
//      FilesUtils.deleteIfExists(csvFilePath);
//      assertTrue(!Files.exists(csvFilePath));
//   }

   @Test
   void testComputeTourDistance() {

      Utils.showTourBookView(bot);

      //Check the original distance
      SWTBotTreeItem tour = Utils.getTour(bot);
      assertEquals("0.542", tour.cell(9)); //$NON-NLS-1$

      //Compute the tour distance
      tour.contextMenu(Messages.Tour_Action_AdjustTourValues).menu(Messages.TourEditor_Action_ComputeDistanceValuesFromGeoPosition).click();
      bot.button("OK").click(); //$NON-NLS-1$

      //Check the new computed distance
      tour = Utils.getTour(bot);
      assertEquals("0.551", tour.cell(9)); //$NON-NLS-1$
   }
}
