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
package views;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class ConconiViewTests extends UITest {

   @Test
   void ConconiView_DisplayView_ShouldShowGraph() {

      Utils.showTourBookView(bot);

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2013   1").expand() //$NON-NLS-1$
            .getNode("May   1").expand().select().getNode("18").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      Utils.showViewFromMenu(bot, Utils.TOOLS, "Conconi Test"); //$NON-NLS-1$
      final SWTBotView conconiView = Utils.showView(bot, "Conconi Test"); //$NON-NLS-1$

      conconiView.close();
   }

}
