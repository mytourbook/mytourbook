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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.tourbook.common.UI;
import net.tourbook.data.Equipment;

public class EquipmentGroup {

   private static final char NL           = UI.NEW_LINE;

   public String             id           = UUID.randomUUID().toString();

   public String             name;

   public Set<Equipment>     allEquipment = new HashSet<>();

   public EquipmentGroup() {}

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (getClass() != obj.getClass()) {
         return false;
      }

      final EquipmentGroup other = (EquipmentGroup) obj;

      return id == other.id;
   }

   @Override
   public int hashCode() {

      return Objects.hash(id);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "EquipmentGroup" + NL //                      //$NON-NLS-1$

            + " name         = " + name + NL //             //$NON-NLS-1$
            + " allEquipment = " + allEquipment + NL //     //$NON-NLS-1$

      ;
   }

}
