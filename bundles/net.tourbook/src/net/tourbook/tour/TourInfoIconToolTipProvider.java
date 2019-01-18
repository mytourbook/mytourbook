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
package net.tourbook.tour;

import java.util.ArrayList;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.HoveredAreaContext;
import net.tourbook.common.util.IHoveredArea;
import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.data.TourData;
import net.tourbook.ui.IInfoToolTipProvider;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.Messages;

public class TourInfoIconToolTipProvider implements ITourToolTipProvider, IInfoToolTipProvider, IHoveredArea, ITourProvider {

   private static final int   HOVER_AREA_POSITION = 2;

   private static Image       _tourInfoImage;
   private static Image       _tourInfoImageHovered;
   private static Rectangle   _tourInfoImageSize;

   private TourToolTip        _tourToolTip;
   
   /**
    * Tour which is displayed in the tool tip
    */
   private TourData           _tourData;
   private long               _tourId             = -1;

   private final TourInfoUI   _tourInfoUI         = new TourInfoUI();

   private HoveredAreaContext _tourInfoHoveredAreaContext;

   /**
    * is <code>true</code> when the mouse is hovering a hovered location
    */
   private boolean            _isHovered          = false;

   public TourInfoIconToolTipProvider() {

      createInfoIcon();

      _tourInfoHoveredAreaContext = new HoveredAreaContext(
            this,
            this,
            HOVER_AREA_POSITION,
            HOVER_AREA_POSITION,
            _tourInfoImageSize.width,
            _tourInfoImageSize.height);
   }

   @Override
   public void afterHideToolTip() {
      _isHovered = false;
   }

   private void createInfoIcon() {

      if (_tourInfoImage != null) {
         return;
      }

      final ImageRegistry imageRegistry = TourbookPlugin.getDefault().getImageRegistry();

      imageRegistry.put(
            Messages.Image_ToolTip_TourInfo,
            TourbookPlugin.getImageDescriptor(Messages.Image_ToolTip_TourInfo));

      imageRegistry.put(
            Messages.Image_ToolTip_TourInfo_Hovered,
            TourbookPlugin.getImageDescriptor(Messages.Image_ToolTip_TourInfo_Hovered));

      _tourInfoImage = imageRegistry.get(Messages.Image_ToolTip_TourInfo);
      _tourInfoImageHovered = imageRegistry.get(Messages.Image_ToolTip_TourInfo_Hovered);

      _tourInfoImageSize = _tourInfoImage.getBounds();
   }

   @Override
   public Composite createToolTipContentArea(final Event event, final Composite parent) {

      Composite ui;

      if (_tourId != -1) {
         // first get data from the tour id when it is set
         _tourData = TourManager.getInstance().getTourData(_tourId);
      }

      if (_tourData == null) {

         // there are no data available

         ui = _tourInfoUI.createUI_NoData(parent);

      } else {

         // tour data is available

         ui = _tourInfoUI.createContentArea(parent, _tourData, this, this);

         // allow the actions to be selected
         if (_tourToolTip != null) {
            _tourToolTip.setHideOnMouseDown(false);
         }
      }

      parent.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            _tourInfoUI.dispose();
         }
      });

      return ui;
   }

   @Override
   public HoveredAreaContext getHoveredContext(final int devMouseX, final int devMouseY) {

      /*
       * hovered area which is hit by the mouse is extendet in the width
       */
      if (devMouseX >= 0 //HOVER_AREA_POSITION
            && devMouseX <= HOVER_AREA_POSITION + _tourInfoImageSize.width + 2
            && devMouseY >= 0 //HOVER_AREA_POSITION
            && devMouseY <= HOVER_AREA_POSITION + _tourInfoImageSize.height + 2) {

         _isHovered = true;

         return _tourInfoHoveredAreaContext;
      }

      _isHovered = false;

      return null;
   }

   @Override
   public Image getHoveredImage() {
      return _tourInfoImageHovered;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final ArrayList<TourData> selectedTour = new ArrayList<>();

      if (_tourData == null) {
         _tourData = TourManager.getInstance().getTourData(_tourId);
      }

      selectedTour.add(_tourData);

      return selectedTour;
   }

   @Override
   public void hideToolTip() {
      if (_tourToolTip != null) {
         _tourToolTip.hide();
      }
   }

   @Override
   public void paint(final GC gc, final Rectangle clientArea) {

      final Image tourInfoImage = _isHovered ? _tourInfoImageHovered : _tourInfoImage;

      // paint static image
      gc.drawImage(tourInfoImage, HOVER_AREA_POSITION, HOVER_AREA_POSITION);
   }

   private void resetToolTip() {

      if (_tourToolTip != null) {
         _tourToolTip.resetToolTip();
      }
   }

   /**
    * Enable/disable tour edit actions, actions are disabled by default
    *
    * @param isEnabled
    */
   public void setActionsEnabled(final boolean isEnabled) {
      _tourInfoUI.setActionsEnabled(isEnabled);
   }

   @Override
   public boolean setHoveredLocation(final int x, final int y) {

      HoveredAreaContext hoveredContext = null;

      if (_tourToolTip != null) {

         // set hovered context from this tooltip provider into the tooltip control

         hoveredContext = getHoveredContext(x, y);

         _tourToolTip.setHoveredContext(hoveredContext);
      }

      _isHovered = hoveredContext != null;

      return _isHovered;
   }

   /**
    * Set {@link TourData} for the tooltip provider. When set to <code>null</code> the tour tooltip
    * is not displayed.
    *
    * @param tourData
    */
   public void setTourData(final TourData tourData) {

      _tourData = tourData;

      // reset id
      _tourId = -1;

      resetToolTip();
   }

   /**
    * Set {@link TourData} from the first item in the list for the tooltip provider, When set to
    * <code>null</code> the tour tooltip is not displayed.
    *
    * @param tourData
    */
   public void setTourDataList(final ArrayList<TourData> tourDataList) {

      if (tourDataList == null || tourDataList.size() == 0) {
         _tourData = null;
      } else {
         _tourData = tourDataList.get(0);
      }

      // reset id
      _tourId = -1;

      resetToolTip();
   }

   /**
    * Set tour id or <code>-1</code> to disable tooltip
    *
    * @param tourId
    */
   public void setTourId(final long tourId) {

      _tourId = tourId;
      _tourData = null;

      resetToolTip();
   }

   @Override
   public void setTourToolTip(final TourToolTip tourToolTip) {
      _tourToolTip = tourToolTip;
   }

   @Override
   public void show(final Point point) {

      if (_tourToolTip != null) {
         _tourToolTip.show(point);
      }
   }

}
