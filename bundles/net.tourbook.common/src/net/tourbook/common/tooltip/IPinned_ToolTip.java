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
package net.tourbook.common.tooltip;

import org.eclipse.swt.widgets.Shell;

/**
 * This tooltip is displayed when the mouse is hovered over a value point in a line graph and
 * displays value point information.
 */
public interface IPinned_ToolTip {

   /**
    * @return Returns the tool tip shell or <code>null</code> when the tool tip is not visible
    */
   Shell getToolTipShell();

   /**
    * Hide the tooltip.
    */
   void hide();

   /**
    * Mouse has been moved to a new or still the old position.
    *
    * @param devMouseX
    * @param devMouseY
    * @param hoveredData
    */
   void setHoveredData(int devMouseX,
                       int devMouseY,
                       Object hoveredData);

   /**
    * Set chart margins, this is used to position the value point tool tip correctly at the chart
    * border.
    *
    * @param snapBorder_Top
    * @param snapBorder_Bottom
    */
   void setSnapBorder(int snapBorder_Top, int snapBorder_Bottom);

}
