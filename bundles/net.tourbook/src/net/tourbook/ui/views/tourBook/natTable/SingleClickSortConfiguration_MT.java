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

import net.tourbook.common.util.ColumnManager;

import org.eclipse.nebula.widgets.nattable.sort.action.SortColumnAction;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.swt.SWT;

/**
 * Overwrite original implementation that some columns cannot be sorted. Could not find a better
 * solution to disable sorting for some columns.
 */
public class SingleClickSortConfiguration_MT extends SingleClickSortConfiguration {

   private ColumnManager _columnManager;

   public SingleClickSortConfiguration_MT(final ColumnManager columnManager) {

      _columnManager = columnManager;
   }

   /**
    * Remove the original key bindings and implement new ones.
    * <p>
    * Register new bindings by overwriting existing binding with
    * <code>register<b>First</b>SingleClickBinding</code>
    * instead of <code>registerSingleClickBinding</code>
    */
   @Override
   public void configureUiBindings(final UiBindingRegistry uiBindingRegistry) {

      uiBindingRegistry.registerFirstSingleClickBinding(

            new ColumnHeaderClickEventMatcher_MT(SWT.NONE, 1, _columnManager),
            new SortColumnAction(false));

      uiBindingRegistry.registerFirstSingleClickBinding(

            new ColumnHeaderClickEventMatcher_MT(SWT.MOD3, 1, _columnManager),
            new SortColumnAction(true));
   }
}
