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

import org.eclipse.swt.graphics.Rectangle;

public abstract class ChartLabel {

   public boolean   isVisible;
   public boolean   isDescription;

   /**
    * x-position in graph units
    */
   public double    graphX;

   /**
    * x-position in graph units
    */
   public double    graphXEnd;

   /**
    * index in the data serie
    */
   public int       serieIndex;

   /**
    * visual position in the chart
    */
   public int       visualPosition;

   public int       labelXOffset;

   public int       labelYOffset;
   public int       visualType;

   /**
    * Painted position.
    */
   public Rectangle paintedLabel;

   public int       devHoverSize;
   public int       devPointSize;

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

   @Override
   public String toString() {
      return "ChartLabel [" // //$NON-NLS-1$
//				+ ("serieIndex=" + serieIndex + ", ")
//				+ ("graphX=" + graphX + ", ")
            + "]"; //$NON-NLS-1$
   }

}
