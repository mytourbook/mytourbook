/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.map;

import net.tourbook.common.UI;
import net.tourbook.map2.view.Map2Point;

import org.eclipse.swt.graphics.Rectangle;

public class PaintedMapPoint {

   private static final char NL = UI.NEW_LINE;

   /**
    * Contains the different map points
    */
   public Map2Point          mapPoint;

   /**
    * Rectangle of the painted label or photo, 4k scaled
    */
   public Rectangle          labelRectangle;

   /**
    * Rectangle of the painted location symbol
    */
   public Rectangle          symbolRectangle;

   public PaintedMapPoint(final Map2Point mapPoint,
                          final Rectangle labelRectangle) {

      this.mapPoint = mapPoint;
      this.labelRectangle = labelRectangle;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "PaintedMapPoint" + NL //                           //$NON-NLS-1$

            + " mapPoint = " + mapPoint //                   //$NON-NLS-1$
//            + " x        = " + mapPoint.geoPointDevX + NL //      //$NON-NLS-1$
//            + " y        = " + mapPoint.geoPointDevY + NL //      //$NON-NLS-1$

      ;
   }
}
