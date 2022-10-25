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
package preferences;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageMap3Tests extends UITest {

   /**
    *
    */
   @Test
   void openPreferencePage() {

      Utils.openPreferences(bot);
      SWTBotTreeItem threeDMapTreeItem = bot.tree().getTreeItem("3D Map").select();

      threeDMapTreeItem = threeDMapTreeItem.expand();
      assertNotNull(threeDMapTreeItem);

      //Test fails with the below error:
      // java.lang.UnsatisfiedLinkError: Can't load library:
      // D:\a\mytourbook-BUILD-autocreated\core\net.tourbook.ui.tests\natives\windows-amd64\\gluegen_rt.dll
//      threeDMapTreeItem.getNode("Layer").select(); //$NON-NLS-1$
//
//      bot.sleep(3000);

      Utils.clickApplyAndCloseButton(bot);
   }
}
