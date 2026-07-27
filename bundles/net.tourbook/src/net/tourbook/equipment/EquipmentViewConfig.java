/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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

import java.util.UUID;

import net.tourbook.common.UI;

public class EquipmentViewConfig {

   private static final char NL               = UI.NEW_LINE;

   String                    id               = UUID.randomUUID().toString();

   String                    defaultId        = EquipmentConfigManager.CONFIG_DEFAULT_ID_1;
   String                    name             = EquipmentConfigManager.CONFIG_DEFAULT_ID_1;

   SortField                 equipmentSort1   = SortField.None;
   SortField                 equipmentSort2   = SortField.None;
   SortField                 equipmentSort3   = SortField.None;
   SortField                 equipmentSort4   = SortField.None;

   SortField                 partServiceSort1 = SortField.None;
   SortField                 partServiceSort2 = SortField.None;
   SortField                 partServiceSort3 = SortField.None;
   SortField                 partServiceSort4 = SortField.None;

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "EquipmentViewConfig"

            + " id           = " + id + NL //                              //$NON-NLS-1$
            + " defaultId    = " + defaultId + NL //                       //$NON-NLS-1$
            + " name         = " + name + NL //                            //$NON-NLS-1$

            + " equipmentSort1   = " + equipmentSort1 + NL //              //$NON-NLS-1$
            + " equipmentSort2   = " + equipmentSort2 + NL //              //$NON-NLS-1$
            + " equipmentSort3   = " + equipmentSort3 + NL //              //$NON-NLS-1$
            + " equipmentSort4   = " + equipmentSort4 + NL //              //$NON-NLS-1$

            + " partServiceSort1 = " + partServiceSort1 + NL //            //$NON-NLS-1$
            + " partServiceSort2 = " + partServiceSort2 + NL //            //$NON-NLS-1$
            + " partServiceSort3 = " + partServiceSort3 + NL //            //$NON-NLS-1$
            + " partServiceSort4 = " + partServiceSort4 + NL //            //$NON-NLS-1$
      ;
   }
}
