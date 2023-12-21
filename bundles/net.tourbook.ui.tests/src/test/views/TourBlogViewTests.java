/*******************************************************************************
 * Copyright (C) 2022, 2024 Frédéric Bard
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

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tourbook.Messages;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourBlogViewTests extends UITest {

   private void changeTagName(final String currentTagName, final String newTagName) {

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("Tagging").select(); //$NON-NLS-1$

      final SWTBotTreeItem existingTag = bot.tree(1).getTreeItem(currentTagName).select();
      assertNotNull(existingTag);

      //Change the tag name
      bot.button(Messages.Action_Tag_Edit).click();
      bot.textWithLabel(Messages.Dialog_TourTag_Label_TagName).setText(newTagName);
      bot.button(Messages.App_Action_Save).click();

      Utils.clickApplyAndCloseButton(bot);
   }

   private SWTBotView getTourBlogView() {

      Utils.showViewFromMenu(bot, Utils.TOUR, Utils.VIEW_NAME_TOURBLOG);
      return Utils.showView(bot, Utils.VIEW_NAME_TOURBLOG);
   }

   @Test
   void testBlogView_Basic() {

      Utils.getTour(bot);

      final SWTBotView tourBlogView = getTourBlogView();
      tourBlogView.show();

      //Change the measurement system to imperial
      Utils.setMeasurementSystem_Imperial(bot);

      bot.sleep(5000);

      //Change back the measurement system to metric
      Utils.setMeasurementSystem_Metric(bot);

      tourBookView = Utils.showTourBookView(bot);
      //Activating the NatTable
      bot.toolbarButtonWithTooltip(Messages.Tour_Book_Action_ToggleViewLayout_Tooltip).click();
      bot.toolbarButtonWithTooltip(Messages.Tour_Book_Action_ToggleViewLayout_Tooltip).click();

      // NatTable is slow to appear so we wait a bit otherwise the test will fail
      bot.sleep(3000);

      final SWTBotNatTable botNatTable = new SWTBotNatTable(
            tourBookView.bot().widget(widgetOfType(NatTable.class)));
      final int rowCount = botNatTable.rowCount();
      assertTrue(rowCount > 0);

      for (int index = 1; index < rowCount; ++index) {
         botNatTable.click(index, 1);
      }

      //Deactivating the NatTable
      bot.toolbarButtonWithTooltip(Messages.Tour_Book_Action_ToggleViewLayout_Tooltip).click();

      tourBlogView.close();
   }

   /**
    * The below SQL statement was run to attach an image that can be found when
    * running the unit tests on GitHub.
    * This might need to be tweaked if/when GitHub changes the root folder D:\a
    *
    * UPDATE "USER".TOURTAG
    * SET IMAGEFILEPATH =
    * 'D:\a\mytourbook-BUILD-autocreated\core\net.tourbook\icons\application\tourbook32.png'
    * WHERE TAGID = 0
    */
   @Test
   void testBlogView_Tags() {

      Utils.getTourWithTags(bot);

      final SWTBotView tourBlogView = getTourBlogView();
      tourBlogView.show();

      final String currentTagName = "Shoes 2"; //$NON-NLS-1$
      final String newTagName = "Renamed Tag"; //$NON-NLS-1$

      //Change the name of the tag to trigger the update of the tour blog view
      changeTagName(currentTagName, newTagName);

      //Revert to the original name
      changeTagName(newTagName, currentTagName);

      tourBlogView.close();
   }
}
