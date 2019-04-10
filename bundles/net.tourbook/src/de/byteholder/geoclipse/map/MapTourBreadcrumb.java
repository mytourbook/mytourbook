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

import de.byteholder.geoclipse.Messages;

import java.util.ArrayList;

import net.tourbook.data.TourData;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class MapTourBreadcrumb {

   private static final String        CRUMB_SEPARATOR    = " >";                                                                //$NON-NLS-1$

   private ArrayList<ArrayList<Long>> _allTours          = new ArrayList<>();
   private ArrayList<Rectangle>       _allCrumbs         = new ArrayList<>();

   private Rectangle                  _outline;

   private int                        _hoveredCrumbIndex = -1;

   private Font                       _boldFont          = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

   public MapTourBreadcrumb() {}

   /**
    * Reset bread crumbs to the hovered crumb, all following crums are removed
    *
    * @return Returns crumb tour id's or <code>null</code> when crumbs are not hovered
    */
   public ArrayList<Long> getCrumbToursAndReset() {

      if (_hoveredCrumbIndex == -1) {
         return null;
      }

      final ArrayList<Long> crumbTours = _allTours.get(_hoveredCrumbIndex);
      final int numCrumbs = _allCrumbs.size();

      for (int crumbIndex = numCrumbs - 1; crumbIndex >= _hoveredCrumbIndex; crumbIndex--) {

         _allTours.remove(crumbIndex);
         _allCrumbs.remove(crumbIndex);
      }

      _hoveredCrumbIndex = -1;

      return crumbTours;
   }

   /**
    * @param devMousePosition
    * @return Returns <code>true</code> when a bread crumb is hit
    */
   public boolean onMouseDown(final Point devMousePosition) {

      if (_hoveredCrumbIndex == -1) {
         return false;
      }

      if (_outline != null && _outline.contains(devMousePosition)) {

         // get hovered crumb

         for (int crumbIndex = 0; crumbIndex < _allCrumbs.size(); crumbIndex++) {

            final Rectangle crumb = _allCrumbs.get(crumbIndex);

            if (crumb.contains(devMousePosition)) {
               _hoveredCrumbIndex = crumbIndex;
               return true;
            }
         }
      }

      return false;
   }

   public void onMouseExit() {

      // reset hovered crumb
      _hoveredCrumbIndex = -1;
   }

   /**
    * @param devMousePosition
    * @return Returns <code>true</code> when the breadcrumb is hovered and the map must be repainted
    */
   public boolean onMouseMove(final Point devMousePosition) {

      final int oldHoveredCrumbIndex = _hoveredCrumbIndex;

      _hoveredCrumbIndex = -1;

      if (_outline != null && _outline.contains(devMousePosition)) {

         // get crumb which is hovered

         final int numCrumbs = _allCrumbs.size();

         // 1. crumb can only be selected when there are multiple crumbs
         // last crumb can never be selected
         if (numCrumbs > 1) {

            for (int crumbIndex = 0; crumbIndex < numCrumbs - 1; crumbIndex++) {

               final Rectangle crumb = _allCrumbs.get(crumbIndex);

               if (crumb.contains(devMousePosition)) {

                  _hoveredCrumbIndex = crumbIndex;
                  break;
               }
            }

            return true;
         }
      }

      if (oldHoveredCrumbIndex != _hoveredCrumbIndex) {

         // index has changed, repaint map

         return true;
      }

      return false;
   }

   public void paint(final GC gc, final boolean isTourPaintMethodEnhanced) {

      if (_allTours.size() == 0) {
         return;
      }

      final int marginVertical = 2;
      final int marginHorizontal_Crumb = 6;
      final int marginHorizontal_Separator = 1;

      int devX = 0;
      final int devY = 0;

      final Color bgColor = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
      final Color bgColorHovered = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
      final Color fgColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
      final Color fgColorHovered = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

      // this is VERY important otherwise hovered tours have another spacing !!!
      gc.setAntialias(SWT.ON);

      final String crumbSepText = CRUMB_SEPARATOR;
      final Point crumbSepSize = gc.textExtent(crumbSepText);
      final int crumbHeight = crumbSepSize.y + 2 * marginVertical;

      _allCrumbs.clear();

      /*
       * Draw breadcrumbs
       */
      for (int crumbIndex = 0; crumbIndex < _allTours.size(); crumbIndex++) {

         if (crumbIndex > 0) {

            /*
             * Paint separator between crumbs
             */

            final Rectangle crumbSeparator = new Rectangle(
                  devX,
                  devY,
                  crumbSepSize.x + 2 * marginHorizontal_Separator,
                  crumbHeight);

            gc.setBackground(bgColor);
            gc.fillRectangle(crumbSeparator);

            gc.setForeground(fgColor);
            gc.drawString(crumbSepText,
                  devX + marginHorizontal_Separator,
                  devY + marginVertical);

            devX += crumbSeparator.width;
         }

         /*
          * Paint crumb
          */

         final ArrayList<Long> tourDataCrumb = _allTours.get(crumbIndex);
         final String numTourDataCrumbs = Integer.toString(tourDataCrumb.size());

         final String crumbText = crumbIndex == 0
               ? Messages.Map2_TourBreadcrumb_Label_Tours + UI.SPACE + UI.SPACE + UI.SPACE + numTourDataCrumbs
               : numTourDataCrumbs;

         final Point contentSize = gc.textExtent(crumbText);

         final Rectangle crumbRectangle = new Rectangle(
               devX,
               devY,
               contentSize.x + 2 * marginHorizontal_Crumb,
               contentSize.y + 2 * marginVertical);

         _allCrumbs.add(crumbRectangle);

         final Color crumbFgColor = crumbIndex == _hoveredCrumbIndex ? fgColorHovered : fgColor;
         final Color crumbBgColor = crumbIndex == _hoveredCrumbIndex ? bgColorHovered : bgColor;

         gc.setBackground(crumbBgColor);
         gc.fillRectangle(crumbRectangle);

         gc.setForeground(crumbFgColor);
         gc.drawString(crumbText,
               devX + marginHorizontal_Crumb,
               devY + marginVertical);

         devX += crumbRectangle.width;
      }

      if (isTourPaintMethodEnhanced) {

         // show message that enhanced painting method is used and a tour cannot be selected

         final Font oldFont = gc.getFont();
         gc.setFont(_boldFont);
         gc.setAntialias(SWT.OFF);

         final String warningText = Messages.Map2_TourBreadcrumb_Info_EnhancedPaintingWarning;
         final Point warningSize = gc.textExtent(warningText);

         final int devXWarning = devX + 0;
         final int devYWarning = 0;


         gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         gc.fillRectangle(
               devXWarning,
               devYWarning,
               warningSize.x + 2 * marginHorizontal_Crumb,
               warningSize.y + 2 * marginVertical);

         gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
         gc.drawString(
               warningText,
               devXWarning + marginHorizontal_Crumb,
               devYWarning + marginVertical);

         gc.setFont(oldFont);
      }

      _outline = new Rectangle(0, 0, devX, crumbHeight);
   }

   public void resetTours() {

      _allTours.clear();
      _allCrumbs.clear();

      _outline = null;
   }

   public void setTours(final ArrayList<TourData> allTourData) {

      // keep only tour id's, allTourData list will be reused !!!

      final int numNewTours = allTourData.size();
      final int numAllOldTours = _allTours.size();

      int numPreviousTours = -1;

      if (numAllOldTours > 0) {
         numPreviousTours = _allTours.get(numAllOldTours - 1).size();
      }

      if (numNewTours == numPreviousTours) {

         // prevent to show the same bread crumb again and again

         return;
      }

      final ArrayList<Long> allTourIds = new ArrayList<>(numNewTours);
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
