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
package dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tourbook.Messages;
import net.tourbook.ui.UI;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class DialogDatabaseTests extends UITest {

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
            "✓" + UI.SPACE2 + "USER.TOURDATA" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURPERSON" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURPERSONHRZONE" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURTYPE" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURMARKER" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURPHOTO" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURREFERENCE" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURCOMPARED" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURBIKE" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURGEOPARTS" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.DEVICESENSOR" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.DEVICESENSORVALUE" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.DBVERSION" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.DB_VERSION_DATA" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURTAG" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURDATA_TOURTAG" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURTAGCATEGORY" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURTAGCATEGORY_TOURTAG" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURTAGCATEGORY_TOURTAGCATEGORY" + UI.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
            "✓" + UI.SPACE2 + "USER.TOURWAYPOINT" + UI.NEW_LINE; //$NON-NLS-1$ //$NON-NLS-2$

      assertEquals(message, shell.bot().label(message).getText());

      Utils.clickOkButton(bot);
   }
}
