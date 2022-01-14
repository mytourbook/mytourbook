/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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

import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

public class TourWeatherToolTipProvider implements ITourToolTipProvider {

   private static final int HOVER_AREA_POSITION_X = 2;
   private static final int HOVER_AREA_POSITION_Y = 2;

   /**
    * Tour for which the weather icon is displayed
    */
   private TourData         _tourData;

   /**
    * Icon image position
    */
   private int              _xPositionIconImage;
   private int              _yPositionIconImage;

   public TourWeatherToolTipProvider() {

      this(HOVER_AREA_POSITION_X, HOVER_AREA_POSITION_Y);
   }

   public TourWeatherToolTipProvider(final int xPos, final int yPos) {

      setIconPosition(xPos, yPos);
   }

   @Override
   public void afterHideToolTip() {}

   @Override
   public Composite createToolTipContentArea(final Event event, final Composite parent) {
      return null;
   }

   private Image getCloudImage(final int weatherIndex) {

      final String cloudKey = IWeather.cloudIcon[weatherIndex];
      return UI.IMAGE_REGISTRY.get(cloudKey);
   }

   @Override
   public void hideToolTip() {}

   @Override
   public void paint(final GC gc, final Rectangle clientArea) {

      if (_tourData == null) {
         return;
      }

      final Image tourInfoImage = getCloudImage(_tourData.getWeatherIndex());

      // paint static image
      gc.drawImage(tourInfoImage, _xPositionIconImage, _yPositionIconImage);
   }

   @Override
   public boolean setHoveredLocation(final int x, final int y) {
      return false;
   }

   public void setIconPosition(final int tooltipDevX, final int tooltipDevY) {

      _xPositionIconImage = tooltipDevX;
      _yPositionIconImage = tooltipDevY;
   }

   public void setTourData(final TourData tourData) {

      _tourData = tourData;
   }

   public void setTourDataList(final List<TourData> tourDataList) {

      if (tourDataList == null || tourDataList.isEmpty()) {
         _tourData = null;
      } else {
         _tourData = tourDataList.get(0);
      }
   }

   @Override
   public void setTourToolTip(final TourToolTip tourToolTip) {}

   @Override
   public void show(final Point point) {}
}
