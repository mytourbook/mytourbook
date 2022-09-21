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
package views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tourbook.Messages;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourSegmenterViewTests extends UITest {

   @Test
   void testSegmenterAlgorithms_All() {

      Utils.getTour(bot);

      Utils.showViewFromMenu(bot, Utils.TOOLS, "Tour Segmenter"); //$NON-NLS-1$
      final SWTBot tourSegmenterViewBot = Utils.showView(bot, "Tour Segmenter").bot(); //$NON-NLS-1$

      final SWTBotTable tableSegments = tourSegmenterViewBot.table();

      final SWTBotCombo segmenterMethodCombo = tourSegmenterViewBot.comboBox(0);

      segmenterMethodCombo.setSelection(Messages.tour_segmenter_type_byAltitude);
      assertEquals("0:01", tableSegments.cell(0, 0));

      segmenterMethodCombo.setSelection(Messages.Tour_Segmenter_Type_ByAltitude_Merged);
      assertEquals("0:35", tableSegments.cell(0, 0));

      segmenterMethodCombo.setSelection(Messages.tour_segmenter_type_byDistance);
      assertEquals("0:47", tableSegments.cell(0, 0));

      segmenterMethodCombo.setSelection(Messages.Tour_Segmenter_Type_ByBreakTime);
      assertEquals("0:20", tableSegments.cell(0, 0));

      segmenterMethodCombo.setSelection(Messages.tour_segmenter_type_byPower);
      assertEquals("0:01", tableSegments.cell(0, 0));

      segmenterMethodCombo.setSelection(Messages.tour_segmenter_type_byComputedAltiUpDown);
      assertEquals("0:02", tableSegments.cell(0, 0));

      segmenterMethodCombo.setSelection(Messages.Tour_Segmenter_Type_Surfing);
      assertEquals("0:47", tableSegments.cell(0, 0));
   }
}
