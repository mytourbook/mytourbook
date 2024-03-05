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
package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;

import net.tourbook.Messages;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourNutritionViewTests extends UITest {

   @Test
   void testAddTourNutritionItem() {

      /*
       * Arrange
       */
      final SWTBotView tourBlogView = Utils.getTourBlogView(bot);
      tourBlogView.show();

      // Open the Tour Nutrition View
      final SWTBotView tourNutritionView = Utils.getTourNutritionView(bot);

      Utils.duplicateAndGetTour(bot);

      tourNutritionView.show();

      final SWTBotTable productsTable = tourNutritionView.bot().table();

      // Assert initial state
      final int initialTableRowCount = productsTable.rowCount();
      // Make sure that the table doesn't contain any products
      assertTrue(initialTableRowCount == 0);

      bot.button(Messages.Tour_Nutrition_Button_SearchProduct).click();
      final SWTBotTable productsSearchTable = bot.table();

      // Act - Search for a food in the OpenFoodFacts database
      bot.comboBox(0).setText("bobo"); //$NON-NLS-1$
      bot.button(Messages.Dialog_SearchProduct_Button_Search).click();
      bot.sleep(5000);
      bot.button(Messages.Dialog_SearchProduct_Button_Add).click();

      // Sort the products by name
      productsSearchTable.header(Messages.Tour_Nutrition_Column_Name).click();
      productsSearchTable.header(Messages.Tour_Nutrition_Column_Name).click();
      // Sort the products by barcode
      productsSearchTable.header(Messages.Tour_Nutrition_Column_Code).click();
      productsSearchTable.header(Messages.Tour_Nutrition_Column_Code).click();
      // Sort the products by quantity
      productsSearchTable.header(Messages.Tour_Nutrition_Column_Quantity).click();
      productsSearchTable.header(Messages.Tour_Nutrition_Column_Quantity).click();

      // Act - Search for a beverage in the OpenFoodFacts database
      bot.comboBox(0).setText("coca"); //$NON-NLS-1$
      bot.button(Messages.Dialog_SearchProduct_Button_Search).click();
      bot.sleep(5000);
      // Act - Add the product via the button in the context menu
      productsSearchTable.click(0, 1);
      productsSearchTable.contextMenu(Messages.Dialog_SearchProduct_Button_Add).click();

      // Act - Search for a food by code in the OpenFoodFacts database
      bot.comboBox(1).setSelection(Messages.Dialog_SearchProduct_Combo_SearchType_ByCode);
      bot.comboBox(0).setText("829262000333"); //$NON-NLS-1$
      bot.button(Messages.Dialog_SearchProduct_Button_Search).click();
      bot.sleep(5000);
      bot.button(Messages.Dialog_SearchProduct_Button_Add).click();

      // Act - Search for a food by code in the OpenFoodFacts database that
      // has a {@link Product#nutritionDataPer} of {@link NutritionDataPer#SERVING}
      bot.comboBox(1).setSelection(Messages.Dialog_SearchProduct_Combo_SearchType_ByCode);
      bot.comboBox(0).setText("0829262000340"); //$NON-NLS-1$
      bot.button(Messages.Dialog_SearchProduct_Button_Search).click();
      bot.sleep(5000);
      bot.button(Messages.Dialog_SearchProduct_Button_Add).click();

      // Close the {@link DialogSearchProduct}
      final SWTBotShell[] currentShells = bot.shells();
      final Optional<SWTBotShell> dialogSearchProductShell = Arrays.stream(currentShells).filter(shell -> shell.getText().equals(
            Messages.Dialog_SearchProduct_Title)).findFirst();
      if (dialogSearchProductShell.isPresent()) {
         dialogSearchProductShell.get().close();
      }

      // Make sure that the table now contains 4 products
      assertEquals(initialTableRowCount + 4, productsTable.rowCount());

      // Act - Add a manual product
      bot.button(Messages.Tour_Nutrition_Button_AddCustomProduct).click();
      // Name
      bot.text(0).setText("Water"); //$NON-NLS-1$
      // Calories
      bot.text(1).setText("12"); //$NON-NLS-1$
      // Sodium
      bot.text(2).setText("13"); //$NON-NLS-1$
      // Is Beverage
      bot.checkBox(0).click();

      Utils.clickOkButton(bot);

      // Assert
      // Make sure that the table now contains 5 products
      assertEquals(initialTableRowCount + 5, productsTable.rowCount());

      // Sort the products by name
      productsTable.header(Messages.Tour_Nutrition_Column_Name).click();
      productsTable.header(Messages.Tour_Nutrition_Column_Name).click();

      // Act - Delete all the products
      for (int index = 0; index < 5; ++index) {

         productsTable.click(0, 2);
         productsTable.contextMenu(Messages.Tour_Nutrition_Button_DeleteProduct).click();
      }

      // Assert
      // Make sure that the table doesn't contain any products
      assertTrue(productsTable.rowCount() == 0);

      final SWTBotTreeItem tour = Utils.selectDuplicatedTour(bot);
      Utils.deleteTour(bot, tour);
   }

}
