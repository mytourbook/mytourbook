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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tourbook.Messages;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageNutritionTests extends UITest {

   @Test
   void createBeverageContainer() {

      // Open the Tour Nutrition View
      Utils.getTourNutritionView(bot).show();

      // Arrange
      Utils.openPreferences(bot);
      SWTBotTreeItem nutritionTreeItem = bot.tree().getTreeItem("Nutrition").select(); //$NON-NLS-1$

      nutritionTreeItem = nutritionTreeItem.expand();

      nutritionTreeItem.getNode("Beverage Containers").select(); //$NON-NLS-1$

      // Assert initial state
      final SWTBotTable beverageContainersTable = bot.table();
      final int initialTableRowCount = beverageContainersTable.rowCount();
      // Make sure that the table doesn't contain any products
      assertTrue(initialTableRowCount == 0);

      // Act
      bot.button(Messages.App_Action_Add).click();

      //Name
      bot.text(0).setText("Salomon flas"); //$NON-NLS-1$
      //Capacity
      bot.spinnerWithLabel(Messages.Dialog_BeverageContainer_Label_Capacity).setSelection(100);

      Utils.clickOkButton(bot);
      // Assert
      // Make sure that the table now contains 1 product
      assertEquals(initialTableRowCount + 1, beverageContainersTable.rowCount());

      // Act - Rename the beverage container
      bot.button(Messages.App_Action_Edit).click();

      //Name
      bot.text(0).setText("Salomon flask"); //$NON-NLS-1$

      Utils.clickOkButton(bot);

      // Act
      bot.button(Messages.App_Action_Delete).click();
      Utils.clickOkButton(bot);
      // Assert
      // Make sure that the table doesn't contain any products
      assertTrue(beverageContainersTable.rowCount() == 0);

      Utils.clickApplyAndCloseButton(bot);
   }
}

