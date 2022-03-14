/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.map.event;

import net.tourbook.common.UI;

public class MapHoveredTourEvent {

   private static final char NL = UI.NEW_LINE;

   public Long               hoveredTourId;
   public int                hoveredValuePointIndex;

   /**
    * Mouse x position relative to the owner control
    */
   public int                mousePositionX;
   public int                mousePositionY;

   public MapHoveredTourEvent(final Long hoveredTourId,
                              final int hoveredValuePointIndex,
                              final int mousePositionX,
                              final int mousePositionY) {

      this.hoveredTourId = hoveredTourId;
      this.hoveredValuePointIndex = hoveredValuePointIndex;

      this.mousePositionX = mousePositionX;
      this.mousePositionY = mousePositionY;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "MapHoveredTourEvent" + NL //                                      //$NON-NLS-1$

            + "[" + NL //                                                        //$NON-NLS-1$

            + "hoveredTourId           =" + hoveredTourId + NL //                //$NON-NLS-1$
            + "hoveredValuePointIndex  =" + hoveredValuePointIndex + NL //       //$NON-NLS-1$

            + "]"; //                                                            //$NON-NLS-1$
   }
}
