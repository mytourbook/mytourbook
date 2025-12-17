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
import net.tourbook.data.EquipmentService;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Service extends TVIEquipmentView_Item {

   private Equipment        _equipment;
   private EquipmentService _service;

   private long             _serviceID;

   public TVIEquipmentView_Service(final TVIEquipmentView_Equipment tviEquipmentView_Equipment,
                                   final EquipmentService equipmentService,
                                   final TreeViewer treeViewer) {

      super(treeViewer);

      setParentItem(tviEquipmentView_Equipment);

      _equipment = tviEquipmentView_Equipment.getEquipment();
      _service = equipmentService;
      _serviceID = equipmentService.getServiceId();
   }

   @Override
   protected void fetchChildren() {

      // there are no children for a part
   }

   public Equipment getEquipment() {
      return _equipment;
   }

   public long getServiceID() {
      return _serviceID;
   }

   public EquipmentService getService() {
      return _service;
   }

   @Override
   public boolean hasChildren() {

      return false;
   }

}
