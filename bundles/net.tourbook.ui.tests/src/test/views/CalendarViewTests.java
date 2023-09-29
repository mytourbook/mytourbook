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
import net.tourbook.common.UI;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class CalendarViewTests extends UITest {

   @Test
   void calendarView_Basic() {

      bot.toolbarButtonWithTooltip("Calendar (Ctrl+Shift+C)").click(); //$NON-NLS-1$
      final SWTBotView calendarView = Utils.showView(bot, "Calendar"); //$NON-NLS-1$

      // Select the calendar profiles combo box
      final SWTBotCombo comboBoxCalendarProfile = bot.comboBox(2);
      assertNotNull(comboBoxCalendarProfile);

      comboBoxCalendarProfile.setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Compact));
      comboBoxCalendarProfile.setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Compact_II));
      comboBoxCalendarProfile.setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Compact_III));
      comboBoxCalendarProfile.setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Year));
      comboBoxCalendarProfile.setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Year_II));
      comboBoxCalendarProfile.setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Year_III));
      comboBoxCalendarProfile.setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Classic));
      comboBoxCalendarProfile.setSelection(getCalendarProfileName(Messages.Calendar_Profile_Name_Default));

      // Activate the "Link with other views"
      final SWTBotToolbarButton linkWithOtherViewsButton = Utils.getToolbarButton(calendarView, Messages.Calendar_View_Action_LinkWithOtherViews);
      assertNotNull(linkWithOtherViewsButton);
      linkWithOtherViewsButton.click();

      // Open the Tour Book View to put a tour in focus on the calendar view
      Utils.showTourBookView(bot);
      Utils.getTour(bot);

      calendarView.show();

      calendarView.close();
   }

   private String getCalendarProfileName(final String profileName) {

      return Messages.Slideout_CalendarOptions_Label_AppPrefix + UI.SPACE + UI.SYMBOL_COLON + UI.SPACE + profileName;
   }
}
