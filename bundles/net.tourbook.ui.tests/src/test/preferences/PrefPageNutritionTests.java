/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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
package preferences;

import net.tourbook.Messages;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageNutritionTests extends UITest {

   @Test
   void openCloudPages() {

      // Arrange
      Utils.openPreferences(bot);
      SWTBotTreeItem nutritionTreeItem = bot.tree().getTreeItem("Nutrition").select(); //$NON-NLS-1$

      nutritionTreeItem = nutritionTreeItem.expand();

      nutritionTreeItem.getNode("Beverage Containers").select(); //$NON-NLS-1$

      bot.button(Messages.PrefPage_TourBeverageContainers_Button_Add);

      // Act

      //Name
      bot.text(0).setText("Salomon flask"); //$NON-NLS-1$
      //Capacity
      bot.text(1).setText("0.5"); //$NON-NLS-1$

      Utils.clickOkButton(bot);

      // Assert
      // todo fb

      Utils.clickApplyAndCloseButton(bot);
   }
}
