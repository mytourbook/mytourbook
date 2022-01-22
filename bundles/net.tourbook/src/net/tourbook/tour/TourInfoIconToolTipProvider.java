/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
import java.util.List;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.HoveredAreaContext;
import net.tourbook.common.util.IHoveredArea;
import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.data.TourData;
import net.tourbook.ui.IInfoToolTipProvider;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

public class TourInfoIconToolTipProvider implements ITourToolTipProvider, IInfoToolTipProvider, IHoveredArea, ITourProvider {

   private static final int HOVER_AREA_POSITION_X = 2;
   private static final int HOVER_AREA_POSITION_Y = 2;

   private static Rectangle _tourInfoImageSize;

   private static Image     _tourInfoImage;
   private static Image     _tourInfoImage_Hovered;
   private static Image     _tourInfoImage_Disabled;

   static {

      final ImageRegistry imageRegistry = TourbookPlugin.getDefault().getImageRegistry();

      imageRegistry.put(Images.TourInfo, TourbookPlugin.getImageDescriptor(Images.TourInfo));
      imageRegistry.put(Images.TourInfo_Disabled, TourbookPlugin.getImageDescriptor(Images.TourInfo_Disabled));
      imageRegistry.put(Images.TourInfo_Hovered, TourbookPlugin.getImageDescriptor(Images.TourInfo_Hovered));

      _tourInfoImage = imageRegistry.get(Images.TourInfo);
      _tourInfoImage_Disabled = imageRegistry.get(Images.TourInfo_Disabled);
      _tourInfoImage_Hovered = imageRegistry.get(Images.TourInfo_Hovered);

      _tourInfoImageSize = _tourInfoImage.getBounds();
   }

   private TourToolTip        _tourToolTip;

   /**
    * Tour which is displayed in the tool tip
    */
   private TourData           _tourData;

   private long               _tourId     = -1;

   private final TourInfoUI   _tourInfoUI = new TourInfoUI();
   private HoveredAreaContext _tourInfoHoveredAreaContext;

   /**
    * Is <code>true</code> when the mouse is hovering a hovered location
    */
   private boolean            _isHovered  = false;

   /**
    * Icon image position
    */
   private int                _xPosIconImage;
   private int                _yPosIconImage;

   public TourInfoIconToolTipProvider() {

      this(HOVER_AREA_POSITION_X, HOVER_AREA_POSITION_Y);
   }

   public TourInfoIconToolTipProvider(final int xPos, final int yPos) {

      _xPosIconImage = xPos;
      _yPosIconImage = yPos;

      createInfoIcon();

      setHoveredAreaContext();
   }

   @Override
   public void afterHideToolTip() {
      _isHovered = false;
   }

   private void createInfoIcon() {

//      if (_tourInfoImage != null) {
//         return;
//      }
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

      parent.addDisposeListener(disposeEvent -> _tourInfoUI.dispose());

      return ui;
   }

   @Override
   public HoveredAreaContext getHoveredContext(final int devMouseX, final int devMouseY) {

      /*
       * hovered area which is hit by the mouse is extended in the width
       */
      final int margin = 5;

      if (devMouseX >= _xPosIconImage - margin
            && devMouseX <= _xPosIconImage + _tourInfoImageSize.width + margin
            && devMouseY >= _yPosIconImage - margin
            && devMouseY <= _yPosIconImage + _tourInfoImageSize.height + margin) {

         _isHovered = true;

         return _tourInfoHoveredAreaContext;
      }

      _isHovered = false;

      return null;
   }

   @Override
   public Image getHoveredImage() {
      return _tourInfoImage_Hovered;
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

      final boolean isTourAvailable = _tourData != null || _tourId != -1;

      final Image tourInfoImage =

            isTourAvailable

                  ? _isHovered
                        ? _tourInfoImage_Hovered
                        : _tourInfoImage

                  : _tourInfoImage_Disabled;

      // paint static image
      gc.drawImage(tourInfoImage, _xPosIconImage, _yPosIconImage);
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

   private void setHoveredAreaContext() {

      _tourInfoHoveredAreaContext = new HoveredAreaContext(
            this,
            this,
            _xPosIconImage,
            _yPosIconImage,
            _tourInfoImageSize.width,
            _tourInfoImageSize.height);
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

   public void setIconPosition(final int tooltipDevX, final int tooltipDevY) {

      _xPosIconImage = tooltipDevX;
      _yPosIconImage = tooltipDevY;

      setHoveredAreaContext();
   }

   /**
    * Set text for the tooltip which is displayed when a tour is not available
    *
    * @param noTourTooltip
    */
   public void setNoTourTooltip(final String noTourTooltip) {

      _tourInfoUI.setNoTourTooltip(noTourTooltip);
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
   public void setTourDataList(final List<TourData> tourDataList) {

      if (tourDataList == null || tourDataList.isEmpty()) {
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
