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

import utils.UITest;

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
}
