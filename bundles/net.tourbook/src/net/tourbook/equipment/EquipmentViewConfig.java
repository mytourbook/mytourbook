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

public class EquipmentViewConfig {

   String    id        = UUID.randomUUID().toString();

   String    defaultId = EquipmentConfigManager.CONFIG_DEFAULT_ID_1;
   String    name      = EquipmentConfigManager.CONFIG_DEFAULT_ID_1;

   SortField equipmentSort1;
   SortField equipmentSort2;
   SortField equipmentSort3;

   SortField partServiceSort1;
   SortField partServiceSort2;
   SortField partServiceSort3;
}
