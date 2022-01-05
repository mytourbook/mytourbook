/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import de.byteholder.geoclipse.map.Map2;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.common.tooltip.ActionToolbarSlideoutAdv;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.common.util.Util;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.filter.geo.SlideoutTourGeoFilter;
import net.tourbook.tour.filter.geo.TourGeoFilter;
import net.tourbook.tour.filter.geo.TourGeoFilter_Manager;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IViewPart;

public class ActionTourGeoFilter extends ActionToolbarSlideoutAdv {

   private static final ImageDescriptor _actionImageDescriptor = TourbookPlugin.getThemedImageDescriptor(Images.TourGeoFilter);

   private SlideoutTourGeoFilter       _slideoutTourGeoFilter;

   public ActionTourGeoFilter() {

      super(_actionImageDescriptor, _actionImageDescriptor);

      isToggleAction = true;
      notSelectedTooltip = Messages.Tour_GeoFilter_Action_Tooltip;
   }

   @Override
   protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

      _slideoutTourGeoFilter = new SlideoutTourGeoFilter(toolItem);
      _slideoutTourGeoFilter.setSlideoutLocation(SlideoutLocation.ABOVE_CENTER);

      return _slideoutTourGeoFilter;
   }

   @Override
   protected void onSelect(final SelectionEvent selectionEvent) {

      super.onSelect(selectionEvent);

      final boolean isSelected = getSelection();

      // activate/deactivate fast painting
      final IViewPart view = Util.getView(Map2View.ID);
      if (view instanceof Map2View) {

         final Map2View map2View = (Map2View) view;
         final Map2 map = map2View.getMap();

         map.setIsFastMapPainting_Active(isSelected);
      }

      // update tour geo filter
      TourGeoFilter_Manager.setFilterEnabled(isSelected);
   }

   public void showSlideout(final TourGeoFilter selectedFilter) {

      // open immediately
      _slideoutTourGeoFilter.open(false);

      // delay to be sure that the slideout is opened
      Display.getCurrent().asyncExec(() -> _slideoutTourGeoFilter.refreshViewer(selectedFilter));
   }

   /**
    * @param isOpenState
    * @param isSelectPreviousGeoFilter
    */
   public void showSlideoutWithState(final boolean isOpenState, final boolean isSelectPreviousGeoFilter) {

      _slideoutTourGeoFilter.setIsKeepSlideoutOpen_DuringUIAction(isOpenState);
      _slideoutTourGeoFilter.setIsSelectPreviousGeoFilter(isSelectPreviousGeoFilter);

      if (isOpenState) {
         _slideoutTourGeoFilter.open(false);
      }
   }

}
