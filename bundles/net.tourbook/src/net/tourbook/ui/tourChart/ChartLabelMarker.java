/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.ui.tourChart;

import net.tourbook.common.UI;
import net.tourbook.photo.Photo;

import org.eclipse.swt.graphics.Rectangle;

public class ChartLabelMarker extends ChartLabel {

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

   public String           markerLabel            = UI.EMPTY_STRING;

   /**
    * marker type, this can be <code>TourMarker.MARKER_TYPE_DEVICE</code> or
    * <code>TourMarker.MARKER_TYPE_CUSTOM</code>
    */
   public int              type;

   public Photo            markerSignPhoto;

   /*
    * Painted label positions
    */
   public int       devXMarker;

   public int       devYMarker;

   /**
    * Bounds where the marker sign image is painted.
    */
   public Rectangle devMarkerSignImageBounds;

   ChartLabelMarker() {}

   /**
    * @return Returns <code>true</code> when the marker is created with the device.
    */
   public boolean isDeviceMarker() {

      return type == MARKER_TYPE_DEVICE;
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
