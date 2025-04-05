/*******************************************************************************
 * Copyright (C) 2022, 2025 Frédéric Bard
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

import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class DialogDatabaseTests extends UITest {

   private static final char NL = UI.NEW_LINE;

   @Test
   void testCompressDatabase() {

      bot.menu("Help").menu("Compress Database...").click(); //$NON-NLS-1$ //$NON-NLS-2$

//      bot.button(Messages.App_Db_Compress_Button_CompressByCopying).click();
//      bot.button(Messages.App_Db_Compress_Button_CompressDatabase).click();
//      bot.sleep(5000);

      Utils.clickCloseButton(bot);
   }

   @Test
   void testDatabaseConsistency() {

      bot.menu("Help").menu("Database Consistency Check").click(); //$NON-NLS-1$ //$NON-NLS-2$

      final SWTBotShell shell = bot.shell("Database Consistency Check"); //$NON-NLS-1$

      final String message = Messages.app_db_consistencyCheck_checkIsOK +
            "✓" + UI.SPACE2 + "USER.TOURDATA" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURPERSON" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURPERSONHRZONE" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURTYPE" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURMARKER" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURPHOTO" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURREFERENCE" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURCOMPARED" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURBIKE" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURGEOPARTS" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.DBVERSION" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURTAG" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURDATA_TOURTAG" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURTAGCATEGORY" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURTAGCATEGORY_TOURTAG" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURTAGCATEGORY_TOURTAGCATEGORY" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURWAYPOINT" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.DB_VERSION_DATA" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.DEVICESENSOR" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.DEVICESENSORVALUE" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURLOCATION" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURBEVERAGECONTAINER" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURNUTRITIONPRODUCT" + NL + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURMARKERTYPE" + NL; //$NON-NLS-1$ //$NON-NLS-2$

      final String text = shell.bot().label(message).getText();

      assertEquals(message, text);

      Utils.clickOkButton(bot);
   }
}
