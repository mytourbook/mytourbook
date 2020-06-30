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
import net.tourbook.common.util.ColumnManager;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.sort.event.ColumnHeaderClickEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.NatEventData;
import org.eclipse.swt.events.MouseEvent;

/**
 *
 */
public class ColumnHeaderClickEventMatcher_MT extends ColumnHeaderClickEventMatcher {

   private ColumnManager _columnManager;

   public ColumnHeaderClickEventMatcher_MT(final int stateMask, final int button, final ColumnManager columnManager) {

      super(stateMask, button);

      _columnManager = columnManager;
   }

   /**
    * Not all columns can currently be used to sort the NatTable
    *
    * @param event
    * @return Returns <code>true</code> when the column on the mouse click can be sorted.
    */
   private boolean canSortColumn(final MouseEvent event) {

      if (event.data == null) {
         event.data = NatEventData.createInstanceFromEvent(event);
      }

      final NatEventData natEventData = (NatEventData) event.data;

      final int columnPosition = natEventData.getColumnPosition();
      final int columnIndex = natEventData.getNatTable().getColumnIndexByPosition(columnPosition);

      final ColumnDefinition colDef = _columnManager.getVisibleAndSortedColumns().get(columnIndex);

      return colDef.canSortColumn();
   }

   @Override
   public boolean matches(final NatTable natTable, final MouseEvent event, final LabelStack regionLabels) {

      return super.matches(natTable, event, regionLabels)
            && canSortColumn(event);
   }
}
