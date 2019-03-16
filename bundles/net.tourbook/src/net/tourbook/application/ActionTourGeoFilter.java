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

import net.tourbook.Messages;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.tour.filter.ActionToolbarSlideoutAdv;
import net.tourbook.tour.filter.geo.Slideout_TourGeoFilter;
import net.tourbook.tour.filter.geo.TourGeoFilter;
import net.tourbook.tour.filter.geo.TourGeoFilter_Manager;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;

public class ActionTourGeoFilter extends ActionToolbarSlideoutAdv {

   private static final ImageDescriptor _actionImageDescriptor = TourbookPlugin.getImageDescriptor(Messages.Image__TourGeoFilter);

   private Slideout_TourGeoFilter       _slideoutTourGeoFilter;

   public ActionTourGeoFilter() {

      super(_actionImageDescriptor, _actionImageDescriptor);

      isToggleAction = true;
      notSelectedTooltip = Messages.Tour_GeoFilter_Action_Tooltip;
   }

   @Override
   protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

      _slideoutTourGeoFilter = new Slideout_TourGeoFilter(toolItem);
      _slideoutTourGeoFilter.setSlideoutLocation(SlideoutLocation.ABOVE_CENTER);

      return _slideoutTourGeoFilter;
   }

   @Override
   protected void onSelect() {

      super.onSelect();

      // update tour geo filter
      TourGeoFilter_Manager.setFilterEnabled(getSelection());
   }

   /**
    * @param isOpenState
    */
   public void setAndOpenGeoFilterSlideout(final boolean isOpenState) {

      _slideoutTourGeoFilter.setIsKeepSlideoutOpen_DuringUIAction(isOpenState);

      if (isOpenState) {
         _slideoutTourGeoFilter.open(false);
      }
   }

   public void showSlideout(final TourGeoFilter selectedFilter) {

      // open immediately
      _slideoutTourGeoFilter.open(false);

      // delay to be sure that the slideout is opened
      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            _slideoutTourGeoFilter.refreshViewer(selectedFilter);
         }
      });
   }

}
