/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.chart;

import org.eclipse.swt.graphics.GC;

public interface IFillPainter {

   /**
    * @param gcGraph
    * @param graphDrawingData
    * @param chart
    * @param devXPositions
    * @param xPos_FirstIndex
    * @param xPos_LastIndex
    * @param isVariableXYValues
    */
   public void draw(GC gcGraph,
                    GraphDrawingData graphDrawingData,
                    Chart chart,
                    long[] devXPositions,
                    final int xPos_FirstIndex,
                    final int xPos_LastIndex,
                    boolean isVariableXYValues);

}
