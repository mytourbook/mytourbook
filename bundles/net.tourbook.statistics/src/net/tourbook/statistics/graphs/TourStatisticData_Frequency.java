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

   int[]   statDistance_Units;
   int[]   statElevation_Units;
   int[]   statTime_Units;

   int[][] statDistanceCounter_Low;
   int[][] statDistanceCounter_High;
   int[][] statDistanceCounter_ColorIndex;
   int[][] statDistanceSum_Low;
   int[][] statDistanceSum_High;
   int[][] statDistanceSum_ColorIndex;

   int[][] statElevationCounter_Low;
   int[][] statElevationCounter_High;
   int[][] statElevationCounter_ColorIndex;
   int[][] statElevationSum_Low;
   int[][] statElevationSum_High;
   int[][] statElevationSum_ColorIndex;

   int[][] statTimeCounter_Low;
   int[][] statTimeCounter_High;
   int[][] statTimeCounter_ColorIndex;
   int[][] statTimeSum_Low;
   int[][] statTimeSum_High;
   int[][] statTimeSum_ColorIndex;

}
