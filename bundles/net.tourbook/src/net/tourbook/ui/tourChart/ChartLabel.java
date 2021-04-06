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
/*
 * Administrator 10.08.2005
 */
package net.tourbook.ui.tourChart;

import net.tourbook.common.UI;
import net.tourbook.photo.Photo;

import org.eclipse.swt.graphics.Rectangle;

public class ChartLabel {

   /**
    * marker was created in the device
    */
   public static final int MARKER_TYPE_DEVICE     = 1;

   /**
    * marker was created in the tourbook application
    */
   public static final int MARKER_TYPE_CUSTOM     = 2;

   public static final int VISIBLE_TYPE_DEFAULT   = 0;
   public static final int VISIBLE_TYPE_TYPE_NEW  = 10;
   public static final int VISIBLE_TYPE_TYPE_EDIT = 20;

   public boolean          isVisible;
   public boolean          isDescription;

   /**
    * x-position in graph units
    */
   public double           graphX;

   /**
    * x-position in graph units
    */
   public double           graphXEnd;

   /**
    * index in the data serie
    */
   public int              serieIndex;

   public String           markerLabel            = UI.EMPTY_STRING;

   public String           pauseDuration          = UI.EMPTY_STRING;

   /**
    * visual position in the chart
    */
   public int              visualPosition;

   /**
    * marker type, this can be <code>TourMarker.MARKER_TYPE_DEVICE</code> or
    * <code>TourMarker.MARKER_TYPE_CUSTOM</code>
    */
   public int              type;

   public int              labelXOffset;

   public int              labelYOffset;
   public int              visualType;

   public Photo            markerSignPhoto;

   /*
    * Painted label positions
    */
   public int       devXMarker;
   public int       devYMarker;

   /**
    * Painted position.
    */
   public Rectangle paintedLabel;

   public int       devHoverSize;

   public int       devMarkerPointSize;
   /**
    * Bounds where the marker sign image is painted.
    */
   public Rectangle devMarkerSignImageBounds;

   /**
    * Is <code>true</code> when the label is drawn vertically.
    */
   public boolean   devIsVertical;

   /**
    * Contains custom data, can be used to keep references to the model.
    */
   public Object    data;

   /*
    * Graph margins
    */
   public int devYBottom;

   public int devYTop;
   public int devGraphWidth;

   ChartLabel() {}

   /**
    * @return Returns <code>true</code> when the marker is created with the device.
    */
   public boolean isDeviceMarker() {

      return type == ChartLabel.MARKER_TYPE_DEVICE;
   }

   @Override
   public String toString() {
      return "ChartLabel [" // //$NON-NLS-1$
//				+ ("serieIndex=" + serieIndex + ", ")
//				+ ("graphX=" + graphX + ", ")
//				+ ("devXMarker=" + devXMarker + ", ")
//				+ ("devYMarker=" + devYMarker + ", ")
            + ("markerLabel=" + markerLabel) //$NON-NLS-1$
            + "]"; //$NON-NLS-1$
   }

}
