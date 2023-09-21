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

import net.tourbook.Messages;
import net.tourbook.common.UI;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class CalendarViewTests extends UITest {

   private String getCalendarProfileName(final String profileName) {

      return Messages.Slideout_CalendarOptions_Label_AppPrefix + UI.SPACE + UI.SYMBOL_COLON + UI.SPACE + profileName;
   }

   @Test
   void openCalendarView() {

      bot.toolbarButtonWithTooltip("Calendar (Ctrl+Shift+C)").click(); //$NON-NLS-1$
      final SWTBotView calendarView = Utils.showView(bot, "Calendar"); //$NON-NLS-1$

      bot.comboBox(2).setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Default));
      bot.comboBox(2).setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Compact));
      bot.comboBox(2).setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Compact_II));
      bot.comboBox(2).setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Compact_III));
      bot.comboBox(2).setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Year));
      bot.comboBox(2).setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Year_II));
      bot.comboBox(2).setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Year_III));
      bot.comboBox(2).setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Classic));

      calendarView.close();
   }
}
