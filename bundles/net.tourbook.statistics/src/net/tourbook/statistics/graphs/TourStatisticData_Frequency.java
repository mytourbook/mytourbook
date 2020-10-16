/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

/**
 * Data for the frequency statistics
 */
public class TourStatisticData_Frequency {

   int[]   statDistance_GroupValues;
   int[][] statDistance_NumTours_Low;
   int[][] statDistance_NumTours_High;
   int[][] statDistance_NumTours_ColorIndex;
   int[][] statDistance_SumValues_Low;
   int[][] statDistance_SumValues_High;
   int[][] statDistance_SumValues_ColorIndex;

   int[]   statDurationTime_GroupValues;
   int[][] statDurationTime_NumTours_Low;
   int[][] statDurationTime_NumTours_High;
   int[][] statDurationTime_NumTours_ColorIndex;
   int[][] statDurationTime_SumValues_Low;
   int[][] statDurationTime_SumValues_High;
   int[][] statDurationTime_SumValues_ColorIndex;

   int[]   statElevation_GroupValues;
   int[][] statElevation_NumTours_Low;
   int[][] statElevation_NumTours_High;
   int[][] statElevation_NumTours_ColorIndex;
   int[][] statElevation_SumValues_Low;
   int[][] statElevation_SumValues_High;
   int[][] statElevation_SumValues_ColorIndex;
}
