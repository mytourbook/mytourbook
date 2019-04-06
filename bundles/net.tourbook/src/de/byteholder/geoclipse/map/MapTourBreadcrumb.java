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
package de.byteholder.geoclipse.map;

import java.util.ArrayList;

import net.tourbook.data.TourData;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

public class MapTourBreadcrumb {

   private Map                            _map;

   private ArrayList<ArrayList<Long>> _allTours = new ArrayList<>();

   public MapTourBreadcrumb(final Map map) {

      _map = map;
   }

   public void paint(final GC gc) {

      if (_allTours.size() == 0) {
         return;
      }

      if (_allTours.size() > 1) {
         int a = 0;
         a++;
      }

      int devX = 0;
      final int devY = 0;

      /*
       * Draw breadcrumbs
       */
      for (int crumbIndex = 0; crumbIndex < _allTours.size(); crumbIndex++) {

         final ArrayList<Long> tourDataCrumb = _allTours.get(crumbIndex);

         String crumbText;
         if (crumbIndex == 0) {

            crumbText = "Tours: " + tourDataCrumb.size();
         } else {

            crumbText = "" + tourDataCrumb.size();
         }

         final Point textSize = gc.textExtent(crumbText);

         gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         gc.fillRectangle(devX, devY, textSize.x, textSize.y);

         gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
         gc.drawText(crumbText, devX, devY);

         devX += textSize.x + 5;
      }
   }

   public void resetTours() {

      _allTours.clear();
   }

   public void setTours(final ArrayList<TourData> allTourData) {

      // keep only tour id's, allTourData list will be reused !!!

      final ArrayList<Long> allTourIds = new ArrayList<>(allTourData.size());
      for (final TourData tourData : allTourData) {
         allTourIds.add(tourData.getTourId());
      }

      _allTours.add(allTourIds);
   }

   public void setTours(final TourData tourData) {

      final ArrayList<Long> singleTour = new ArrayList<>();
      singleTour.add(tourData.getTourId());

      _allTours.add(singleTour);
   }

}
