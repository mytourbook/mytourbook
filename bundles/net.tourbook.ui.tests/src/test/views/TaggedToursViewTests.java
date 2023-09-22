/*******************************************************************************
 * Copyright (C) 2022, 2023 Frédéric Bard
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.tourbook.Messages;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TaggedToursViewTests extends UITest {

   /**
    * This test could have caught this bug in the 22.3 release
    * https://sourceforge.net/p/mytourbook/discussion/622811/thread/a4d83a6052/
    */
   @Test
   void testTaggedToursView() {

      //Open the Tagged Tours view
      Utils.openOtherMenu(bot);
      bot.tree().getTreeItem(WorkbenchTests.TOUR_DIRECTORIES).expand().getNode(Utils.VIEW_NAME_TAGGEDTOURS).select();
      bot.button("Open").click(); //$NON-NLS-1$

      Utils.getTour(bot);

      final SWTBotView taggedToursView = Utils.showView(bot, Utils.VIEW_NAME_TAGGEDTOURS);

      final SWTBotTreeItem item = bot.tree(1).getTreeItem("Shoes 2   9").select(); //$NON-NLS-1$
      assertNotNull(item);
      final SWTBotTreeItem node = item.getNode("5/31/2015").select(); //$NON-NLS-1$
      assertNotNull(node);

      // Looping across the tag filters
      for (final SWTBotToolbarButton button : taggedToursView.getToolbarButtons()) {

         if (Messages.Tour_Tags_Action_ToggleTagFilter_Tooltip.equals(button.getToolTipText())) {

            button.click();
            button.click();
            button.click();
            break;
         }
      }

      taggedToursView.close();
   }
}
