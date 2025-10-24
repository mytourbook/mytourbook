/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment;

import net.tourbook.data.Equipment;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipment extends TVIEquipItem {

   private Equipment _equipment;

   public TVIEquipment(final TreeViewer equipViewer, final Equipment equipment) {

      super(equipViewer);

      _equipment = equipment;
   }

   @Override
   protected void fetchChildren() {
      // a tag has no children
   }

   public Equipment getEquipment() {
      return _equipment;
   }

   @Override
   public boolean hasChildren() {
      return false;
   }

   @Override
   public String toString() {

      return "TVIEquip [" //$NON-NLS-1$

            + "_equipment = " + _equipment //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }

}
