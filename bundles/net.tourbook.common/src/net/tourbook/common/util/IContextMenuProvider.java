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
package net.tourbook.common.util;

import org.eclipse.swt.widgets.Menu;

/**
 * This context menu provider do fix the
 * <p>
 * <b>IllegalArgumentException: Widget has the wrong parent</b>
 * <p>
 * When a view is minimized, then the context menu is already created but has the wrong parent when
 * the view is displayed later.
 *
 * @since version 19.5
 */
public interface IContextMenuProvider {

   /**
    * Disposes the context menu.
    */
   public void disposeContextMenu();

   /**
    * @return Returns the {@link Menu} for the context menu. Can be <code>null</code> which will not
    *         display a context menu.
    */
   public Menu getContextMenu();

   /**
    * @return Returns the recreated context menu, the old context menu must be disposed. Can be
    *         <code>null</code> which will not display a context menu.
    */
   public Menu recreateContextMenu();

}
