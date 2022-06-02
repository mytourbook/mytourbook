
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BasicTests {

   private static SWTBot bot;

   // todo fb, redo the workspace in english

   @BeforeEach
   public void beforeClass() {
      bot = new SWTBot();
      SWTBotPreferences.TIMEOUT = 20000;

//		final SWTBotButton myTourbookMenu = bot.button("OK").click();
//		assertNotNull(myTourbookMenu);
//		final SWTBotButton measurementSystemMenu = bot.button("OK").click();
//		assertNotNull(measurementSystemMenu);
//		final SWTBotButton peopleMenu = bot.button("Apply and Close").click();
//		assertNotNull(peopleMenu);
//		final SWTBotButton firstStartMenu = bot.button("OK").click();
//		assertNotNull(firstStartMenu);

   }

   @Test
   void testOpenPreferences() {

      bot.toolbarButtonWithTooltip("Preferences (Ctrl+Shift+P)").click();
      bot.button("Apply and Close").click();
   }

//	@Test
//	void testStatisticsView() {
//
//		bot.sleep(5000);
//		final SWTBotMenu statisticsView = bot.menu("Statistiques");// todo fb
//																	// rename in
//																	// english
//		assertNotNull(statisticsView);
//		bot.sleep(5000);
//	}

   @Test
   void testTourMarkerView() {

      // This is the equivalent of
      // bot.viewByTitle("Calendar").show();
      // for SWTBot.
      bot.menu("Tour Editor");

      bot.tree().getTreeItem("2020   3").expand();
      bot.tree().getTreeItem("2020   3").getNode("May   2").expand();
      bot.tree().getTreeItem("2020   3").getNode("May   2").select();
      bot.tree().getTreeItem("2020   3").getNode("May   2").getNode("23").select();
      final SWTBotMenu tourMarkerView = bot.menu("Tour Markers");
      assertNotNull(tourMarkerView);
      bot.table().select(0);
      bot.table().select(1);
   }
}
