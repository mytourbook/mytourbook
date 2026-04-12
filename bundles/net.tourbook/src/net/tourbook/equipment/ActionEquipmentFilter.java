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

import net.tourbook.common.tooltip.ActionToolbarSlideoutAdv;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.ToolItem;

public class ActionEquipmentFilter extends ActionToolbarSlideoutAdv {

   private SlideoutEquipmentFilter _slideoutEquipmentFilter;

   private EquipmentView           _equipmentView;

   /**
    * @param equipmentView
    * @param state
    * @param actionImage
    */
   public ActionEquipmentFilter(final EquipmentView equipmentView,
                                final Image actionImage) {

      /*
       * !!! Needed to create images, otherwise they are disposed sometimes and the action
       * is not displayed in the toolbar, very strange, in other views it works without creating
       * images !!!
       */
      super(actionImage);

      _equipmentView = equipmentView;

      isToggleAction = true;

      notSelectedTooltip = "Enable/disable equipment filter";
   }

   @Override
   protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

      _slideoutEquipmentFilter = new SlideoutEquipmentFilter(toolItem, _equipmentView);
      _slideoutEquipmentFilter.setSlideoutLocation(SlideoutLocation.BELOW_CENTER);

      return _slideoutEquipmentFilter;
   }

   @Override
   protected void onSelect(final SelectionEvent selectionEvent) {

      // show/hide slideout
      super.onSelect(selectionEvent);

      _equipmentView.updateEquipmentFilter_FromAction(getSelection());
   }

}
