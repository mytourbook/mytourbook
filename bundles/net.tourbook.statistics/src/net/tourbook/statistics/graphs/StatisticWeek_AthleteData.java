/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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
package net.tourbook.statistics.graphs;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartType;

public class StatisticWeek_AthleteData extends StatisticWeek {

   @Override
   ChartDataModel getChartDataModel() {

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.LINE);

      createXData_Week(chartDataModel);
      createYData_AthleteBodyWeight(chartDataModel);
      createYData_AthleteBodyFat(chartDataModel);

      return chartDataModel;
   }

   @Override
   protected String getGridPrefPrefix() {
      return GRID_WEEK_ATHLETEDATA;
   }
}
