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
/**
 * @author Wolfgang Schramm
 *
 * created: 19.07.2005
 */
package net.tourbook.chart;

import net.tourbook.common.UI;

public class ChartXSliderLabel {

   private static final char NL = UI.NEW_LINE;

   String                    text;

   int                       width;
   int                       height;

   int                       x;
   int                       y;

   int                       devYGraph;

   public ChartXSliderLabel() {}

   @Override
   public String toString() {

      return "ChartXSliderLabel" + NL //              //$NON-NLS-1$

            + "[" + NL //                             //$NON-NLS-1$

            + "text=" + text + "" + NL //             //$NON-NLS-1$ //$NON-NLS-2$
            + "width=" + width + "" + NL //           //$NON-NLS-1$ //$NON-NLS-2$
            + "height=" + height + "" + NL //         //$NON-NLS-1$ //$NON-NLS-2$
            + "x=" + x + "" + NL //                   //$NON-NLS-1$ //$NON-NLS-2$
            + "y=" + y + "" + NL //                   //$NON-NLS-1$ //$NON-NLS-2$
            + "devYGraph=" + devYGraph + NL //        //$NON-NLS-1$

            + "]"; //                                 //$NON-NLS-1$
   }
}
