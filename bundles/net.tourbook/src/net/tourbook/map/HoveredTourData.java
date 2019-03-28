/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.map;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

public class HoveredTourData {

   ArrayList<Long> allHoveredTours;

   /**
    * Point in a tour which is hovered
    */
   Point           devHoveredTourPoint;

   Rectangle       devHoveredTourRect;

   public HoveredTourData(final ArrayList<Long> allHoveredTours,
                          final Point devHoveredTourPoint,
                          final Rectangle devHoveredTourRect) {

      this.allHoveredTours = allHoveredTours;

      this.devHoveredTourPoint = devHoveredTourPoint;
      this.devHoveredTourRect = devHoveredTourRect;
   }
}
