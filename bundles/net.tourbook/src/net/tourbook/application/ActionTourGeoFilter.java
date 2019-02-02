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
package net.tourbook.application;


import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;

import net.tourbook.Messages;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.tour.filter.ActionToolbarSlideoutAdv;
import net.tourbook.tour.filter.geo.SlideoutTourGeoFilter;
import net.tourbook.tour.filter.geo.TourGeoFilterManager;

public class ActionTourGeoFilter extends ActionToolbarSlideoutAdv {

   private static final ImageDescriptor _actionImageDescriptor = TourbookPlugin.getImageDescriptor(Messages.Image__TourGeoFilter);

   private static final IDialogSettings _state                 = TourbookPlugin.getState("TourGeoFilter");                        //$NON-NLS-1$

   private SlideoutTourGeoFilter        _slideoutTourGeoFilter;

   public ActionTourGeoFilter() {

      super(_actionImageDescriptor, _actionImageDescriptor);

      isToggleAction = true;
      notSelectedTooltip = Messages.Tour_GeoFilter_Action_Tooltip;
   }

   @Override
   protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

      _slideoutTourGeoFilter = new SlideoutTourGeoFilter(toolItem, _state);
      _slideoutTourGeoFilter.setSlideoutLocation(SlideoutLocation.ABOVE_CENTER);

      return _slideoutTourGeoFilter;
   }

   @Override
   protected void onSelect() {

      super.onSelect();

      // update tour geo filter
      TourGeoFilterManager.setFilterEnabled(getSelection());
   }

   public void showSlideout() {

      // open immediately
      _slideoutTourGeoFilter.open(false);

      // delay to be sure that the slideout is opened
      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            _slideoutTourGeoFilter.refreshViewer();
         }
      });
   }

}
