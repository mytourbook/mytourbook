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
package net.tourbook.ui.tourChart;

import net.tourbook.common.color.GraphColorManager;
import net.tourbook.tour.TourManager;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

public class ConfigGraphSegment {

   float[]             segmentDataSerie;

   /**
    * When <code>null</code> then the value is painted with 1 digit.
    */
   IValueLabelProvider labelProvider;

   /**
    * Is <code>true</code> when negative values can occure, e.g. gradient.
    */
   boolean             canHaveNegativeValues;

   /**
    * Position of the painted segment values which is used to get a hovered segment.
    */
   Rectangle[]         paintedValues;

   RGB                 segmentLineRGB;

   double              minValueAdjustment;

   /**
    * @param graphColorName
    */
   public ConfigGraphSegment(final String graphColorName) {

      final String prefColorText = net.tourbook.common.UI.IS_DARK_THEME
            ? GraphColorManager.PREF_COLOR_TEXT_DARK
            : GraphColorManager.PREF_COLOR_TEXT_LIGHT;

      segmentLineRGB = TourManager.getGraphColor(graphColorName, prefColorText);
   }

}
