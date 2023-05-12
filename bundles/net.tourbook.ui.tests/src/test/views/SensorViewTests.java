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

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class SensorViewTests extends UITest {

   @Test
   void selectSensors() {

      Utils.showViewFromMenu(bot, Utils.DIRECTORY, "Sensor Chart"); //$NON-NLS-1$
      final SWTBotView sensorChartView = Utils.showView(bot, "Sensor Chart"); //$NON-NLS-1$

      Utils.showViewFromMenu(bot, Utils.DIRECTORY, "Se&nsor"); //$NON-NLS-1$
      final SWTBotView sensorView = Utils.showView(bot, "Sensor"); //$NON-NLS-1$

      final SWTBotTable sensorViewTable = sensorView.bot().table();
      assertEquals(4, sensorViewTable.rowCount());
      sensorViewTable.select(0);
      //This sensor should trigger the graph drawing in the sensor chart view
      sensorViewTable.select(1);
      sensorViewTable.select(2);
      sensorViewTable.select(3);

      sensorChartView.close();
      sensorView.close();
   }

}
