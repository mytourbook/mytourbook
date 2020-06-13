/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook.natTable;

import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.jface.window.ToolTip;
import org.eclipse.nebula.widgets.nattable.NatTable; 
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

public class NatTable_Header_Tooltip extends NatTableContentTooltip {

   private TourBookView _tourBookView;

   public NatTable_Header_Tooltip(final NatTable natTable, final TourBookView tourBookView) {

      super(natTable, ToolTip.NO_RECREATE, false);

      _tourBookView = tourBookView;

      setPopupDelay(0);
      setShift(new Point(10, 10));

      activate();

      this.natTable = natTable;
      this.tooltipRegions = new String[] { GridRegion.COLUMN_HEADER };
   }

   /**
    * {@inheritDoc}
    * <p>
    * Evaluates the cell for which the tooltip should be rendered and checks
    * the display value. If the display value is empty <code>null</code> will
    * be returned which will result in not showing a tooltip.
    */
   @Override
   protected String getText(final Event event) {

      final ColumnDefinition colDef = _tourBookView.getNatTable_SelectedColumnDefinition(event);
      if (colDef != null) {

         // column found

         return colDef.getColumnHeaderToolTipText();
      }

      return null;
   }
}
