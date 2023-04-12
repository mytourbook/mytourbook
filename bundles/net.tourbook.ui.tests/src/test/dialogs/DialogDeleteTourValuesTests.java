/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tourbook.Messages;
import net.tourbook.common.UI;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class DialogDeleteTourValuesTests extends UITest {

   @Test
   void deleteTourValues_Calories_Weather() {

      //Check the initial values
      SWTBotTreeItem tour = Utils.getTourWithSRTM(bot);
      assertEquals("2", tour.cell(tourBookView_Calories_Column_Index)); //$NON-NLS-1$

      tour.contextMenu(Messages.Dialog_DeleteTourValues_Action_OpenDialog).click();
      bot.checkBox(Messages.Dialog_ModifyTours_Checkbox_Calories).click();
      bot.checkBox(Messages.Dialog_ModifyTours_Checkbox_WeatherValues).click();

      bot.button(Messages.Dialog_DeleteTourValues_Button_Delete).click();
      Utils.clickOkButton(bot);

      bot.sleep(1000);

      //Setting the focus again on the Tourbook view
      Utils.showTourBookView(bot);

      //Check that the calories were deleted
      tour = Utils.getTourWithSRTM(bot);
      assertEquals(UI.EMPTY_STRING, tour.cell(tourBookView_Calories_Column_Index));
   }
}
