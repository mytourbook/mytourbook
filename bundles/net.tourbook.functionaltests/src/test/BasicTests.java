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
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BasicTests {

   private static SWTBot bot;

   @BeforeEach
   public void beforeClass() {
		bot = new SWTBot();
		SWTBotPreferences.TIMEOUT = 20000;
		// the best would be to copy a database that already has tours

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

@Test
void testTourMarkerView()
	{
		bot.menu("Tour Editor");
		bot.tree().getTreeItem("2020   3").expand();
		bot.tree().getTreeItem("2020   3").getNode("May   2").expand();
		bot.tree().getTreeItem("2020   3").getNode("May   2").select();
		bot.tree().getTreeItem("2020   3").getNode("May   2").getNode("23").select();
		bot.menu("Tour Markers");
		bot.table().select(0);
		bot.table().select(1);
	}
}
