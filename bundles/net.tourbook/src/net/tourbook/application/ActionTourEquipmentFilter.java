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
package net.tourbook.application;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.common.tooltip.ActionToolbarSlideoutAdv;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.equipment.tour.filter.SlideoutTourEquipmentFilter;
import net.tourbook.equipment.tour.filter.TourEquipmentFilterManager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.ToolItem;

public class ActionTourEquipmentFilter extends ActionToolbarSlideoutAdv {

   private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.application.ActionTourEquipmentFilter"); //$NON-NLS-1$

   private AdvancedSlideout             _slideoutTourEquipmentFilter;

   public ActionTourEquipmentFilter() {

      super(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Filter));

      isToggleAction = true;
      notSelectedTooltip = Messages.Equipment_Action_EquipmentFilterInfo_Tooltip;
   }

   @Override
   protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

      _slideoutTourEquipmentFilter = new SlideoutTourEquipmentFilter(toolItem, _state);
      _slideoutTourEquipmentFilter.setSlideoutLocation(SlideoutLocation.ABOVE_CENTER);

      return _slideoutTourEquipmentFilter;
   }

   @Override
   protected void onSelect(final SelectionEvent selectionEvent) {

      super.onSelect(selectionEvent);

      // update tour equipment filter
      TourEquipmentFilterManager.setFilterEnabled(getSelection());
   }

}
