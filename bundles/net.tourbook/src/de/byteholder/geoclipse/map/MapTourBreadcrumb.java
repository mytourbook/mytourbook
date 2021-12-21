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
package de.byteholder.geoclipse.map;

import de.byteholder.geoclipse.Messages;

import java.util.ArrayList;

import net.tourbook.common.color.ThemeUtil;
import net.tourbook.data.TourData;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class MapTourBreadcrumb {

   private static final String        RESET_BUTTON           = " X ";
   private static final Color         SYSTEM_COLOR_BLUE      = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
   private static final Color         SYSTEM_COLOR_BLACK     = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
   private static final Color         SYSTEM_COLOR_RED       = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
   private static final Color         SYSTEM_COLOR_WHITE     = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

   private static final String        CRUMB_SEPARATOR        = " >";                                                                //$NON-NLS-1$

   private static final int           NOT_HOVERED_INDEX      = -1;

   private ArrayList<ArrayList<Long>> _allCrumbsWithAllTours = new ArrayList<>();
   private ArrayList<Rectangle>       _allCrumbRectangles    = new ArrayList<>();

   /**
    * Contains the reset button and tour crumbs
    */
   private Rectangle                  _crumbOutline;

   private boolean                    _isResetButtonHovered;
   private boolean                    _isResetButtonSelected;
   private Rectangle                  _resetButtonCrumbOutline;

   private int                        _hoveredCrumbIndex     = NOT_HOVERED_INDEX;

   private Font                       _boldFont              = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

   private Map                        _map;

   public MapTourBreadcrumb(final Map map) {

      _map = map;
   }

   /**
    * Add new tours to the existing bread crumbs.
    *
    * @param allTourData
    */
   public void addBreadcrumTours(final ArrayList<TourData> allTourData) {

      // keep only tour id's, allTourData list will be reused !!!

      final int numNewTours = allTourData.size();
      final int numAllOldTours = _allCrumbsWithAllTours.size();

      int numPreviousTours = -1;

      if (numAllOldTours > 0) {
         numPreviousTours = _allCrumbsWithAllTours.get(numAllOldTours - 1).size();
      }

      if (numNewTours == numPreviousTours) {

         // prevent to show the same bread crumb again and again

         return;
      }

      final ArrayList<Long> allTourIds = new ArrayList<>(numNewTours);
      for (final TourData tourData : allTourData) {
         allTourIds.add(tourData.getTourId());
      }

      _allCrumbsWithAllTours.add(allTourIds);
   }

   /**
    * Reset bread crumbs of the hovered crumb, all following crums are removed
    *
    * @return Returns crumb tour id's or <code>null</code> when crumbs are not hovered
    */
   public ArrayList<Long> getHoveredCrumbedTours_WithReset() {

      if (_hoveredCrumbIndex == NOT_HOVERED_INDEX) {
         return null;
      }

      final int numCrumbs = _allCrumbRectangles.size();
      final ArrayList<Long> allCrumbTours = _allCrumbsWithAllTours.get(_hoveredCrumbIndex);

      if (_hoveredCrumbIndex == numCrumbs - 1) {

         /*
          * The last crumb is selected -> reselect this crumb which can be helpful
          */

      } else {

         for (int crumbIndex = numCrumbs - 1; crumbIndex >= _hoveredCrumbIndex; crumbIndex--) {

            _allCrumbsWithAllTours.remove(crumbIndex);
            _allCrumbRectangles.remove(crumbIndex);
         }

         _hoveredCrumbIndex = NOT_HOVERED_INDEX;
      }

      return allCrumbTours;
   }

   private boolean isCollectionContainedInAllBreadcrums(final ArrayList<TourData> allTourData) {

      if (_allCrumbsWithAllTours.isEmpty()) {
         return false;
      }

      // get all tour id's
      final ArrayList<Long> allTourIds = new ArrayList<>(allTourData.size());
      for (final TourData tourData : allTourData) {
         allTourIds.add(tourData.getTourId());
      }

      for (final ArrayList<Long> allToursInOneCrumb : _allCrumbsWithAllTours) {

         if (CollectionUtils.isEqualCollection(allTourIds, allToursInOneCrumb)) {
            return true;
         }
      }

      return false;
   }

   /**
    * @return Returns <code>true</code> when a crumb is hovered, otherwise <code>false</code>
    */
   public boolean isCrumbHovered() {

      return _hoveredCrumbIndex != NOT_HOVERED_INDEX;
   }

   public boolean isResetButtonSelected() {

      return _isResetButtonSelected;
   }

   /**
    * @param devMousePosition
    * @return Returns <code>true</code> when a bread crumb is hit
    */
   public boolean onMouseDown(final Point devMousePosition) {

      _isResetButtonSelected = false;

      if (_resetButtonCrumbOutline != null && _resetButtonCrumbOutline.contains(devMousePosition)) {

         _isResetButtonSelected = true;

         return true;
      }

      if (_hoveredCrumbIndex == NOT_HOVERED_INDEX) {
         return false;
      }

      if (_crumbOutline != null && _crumbOutline.contains(devMousePosition)) {

         // get hovered crumb

         for (int crumbIndex = 0; crumbIndex < _allCrumbRectangles.size(); crumbIndex++) {

            final Rectangle crumb = _allCrumbRectangles.get(crumbIndex);

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
      _hoveredCrumbIndex = NOT_HOVERED_INDEX;
   }

   /**
    * @param devMousePosition
    * @return Returns <code>true</code> when the breadcrumb is hovered and the map must be repainted
    */
   public boolean onMouseMove(final Point devMousePosition) {

      final int oldHoveredCrumbIndex = _hoveredCrumbIndex;

      _hoveredCrumbIndex = NOT_HOVERED_INDEX;
      _isResetButtonHovered = false;

      if (_crumbOutline != null && _crumbOutline.contains(devMousePosition)) {

         if (_resetButtonCrumbOutline != null && _resetButtonCrumbOutline.contains(devMousePosition)) {

            // the reset button is hovered -> repaint map

            _isResetButtonHovered = true;

            return true;
         }

         // get crumb which is hovered

         final int numCrumbs = _allCrumbRectangles.size();

         for (int crumbIndex = 0; crumbIndex < numCrumbs; crumbIndex++) {

            final Rectangle crumb = _allCrumbRectangles.get(crumbIndex);

            if (crumb.contains(devMousePosition)) {

               _hoveredCrumbIndex = crumbIndex;

               return true;
            }
         }
      }

      if (oldHoveredCrumbIndex != _hoveredCrumbIndex) {

         // index has changed -> repaint map

         return true;
      }

      return false;
   }

   public void paint(final GC gc, final boolean isShowTourPaintMethodEnhancedWarning) {

      if (_allCrumbsWithAllTours.isEmpty()) {
         return;
      }

      Color bgColor;
      Color fgColor;
      Color bgColorHovered;
      Color fgColorHovered;

      if (net.tourbook.common.UI.IS_DARK_THEME && _map.isMapBackgroundDark()) {

         bgColor = ThemeUtil.getDefaultBackgroundColor_Table();
         fgColor = ThemeUtil.getDefaultForegroundColor_Table();

      } else {

         if (_map.isMapBackgroundDark()) {

            bgColor = new Color(0x40, 0x40, 0x40);
            fgColor = SYSTEM_COLOR_WHITE;

         } else {

            bgColor = SYSTEM_COLOR_WHITE;
            fgColor = SYSTEM_COLOR_BLACK;
         }
      }

      bgColorHovered = SYSTEM_COLOR_BLUE;
      fgColorHovered = SYSTEM_COLOR_WHITE;

      final int marginVertical = 2;
      final int marginHorizontal_Crumb = 6;
      final int marginHorizontal_Separator = 1;

      int devX = 0;
      final int devY = 0;

      // this is VERY important otherwise hovered tours have another spacing !!!
      gc.setAntialias(SWT.ON);

      final String crumbSepText = CRUMB_SEPARATOR;
      final Point crumbSepSize = gc.textExtent(crumbSepText);
      final int crumbHeight = crumbSepSize.y + 2 * marginVertical;

      /*
       * Draw reset button
       */
      final String crumbResetText = RESET_BUTTON;
      final Point crumbResetSize = gc.textExtent(crumbResetText);
      final Rectangle crumbResetButton = new Rectangle(
            devX,
            devY,
            crumbResetSize.x + 2 * marginHorizontal_Separator,
            crumbHeight);

      gc.setBackground(_isResetButtonHovered ? SYSTEM_COLOR_RED : bgColor);
      gc.fillRectangle(crumbResetButton);

      gc.setForeground(fgColor);
      gc.drawString(crumbResetText,
            devX + marginHorizontal_Separator,
            devY + marginVertical);

      _resetButtonCrumbOutline = crumbResetButton;

      devX += crumbResetButton.width;

      /*
       * Draw breadcrumbs
       */
      _allCrumbRectangles.clear();

      for (int crumbIndex = 0; crumbIndex < _allCrumbsWithAllTours.size(); crumbIndex++) {

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

         final ArrayList<Long> tourDataCrumb = _allCrumbsWithAllTours.get(crumbIndex);
         final String numTourDataCrumbs = Integer.toString(tourDataCrumb.size());

         final String crumbText = crumbIndex == 0

               ? Messages.Map2_TourBreadcrumb_Label_Tours
                     + UI.SPACE
                     + UI.SPACE
                     + UI.SPACE
                     + numTourDataCrumbs

               : numTourDataCrumbs;

         final Point crumTextSize = gc.textExtent(crumbText);

         final Rectangle crumbRectangle = new Rectangle(
               devX,
               devY,
               crumTextSize.x + 2 * marginHorizontal_Crumb,
               crumTextSize.y + 2 * marginVertical);

         _allCrumbRectangles.add(crumbRectangle);

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

      /*
       * Show message that enhanced painting method is used and a tour cannot be selected
       */
      if (isShowTourPaintMethodEnhancedWarning) {

         final Font oldFont = gc.getFont();
         gc.setFont(_boldFont);
         gc.setAntialias(SWT.OFF);

         final String warningText = Messages.Map2_TourBreadcrumb_Info_EnhancedPaintingWarning;
         final Point warningSize = gc.textExtent(warningText);

         final int devXWarning = devX + 0;
         final int devYWarning = 0;

         gc.setBackground(SYSTEM_COLOR_RED);
         gc.fillRectangle(
               devXWarning,
               devYWarning,
               warningSize.x + 2 * marginHorizontal_Crumb,
               warningSize.y + 2 * marginVertical);

         gc.setForeground(SYSTEM_COLOR_WHITE);
         gc.drawString(
               warningText,
               devXWarning + marginHorizontal_Crumb,
               devYWarning + marginVertical);

         gc.setFont(oldFont);
      }

      _crumbOutline = new Rectangle(0, 0, devX, crumbHeight);
   }

   public void resetAllBreadcrumbs() {

      _allCrumbsWithAllTours.clear();
      _allCrumbRectangles.clear();

      _crumbOutline = null;
      _resetButtonCrumbOutline = null;
   }

   public void setBreadcrumbForOneTour(final TourData tourData) {

      final Long tourId = tourData.getTourId();

      // keep breadcrumbs when tour is contained in it

      for (final ArrayList<Long> allToursInOneCrumb : _allCrumbsWithAllTours) {

         if (allToursInOneCrumb.contains(tourId)) {
            return;
         }
      }

      final ArrayList<Long> singleTour = new ArrayList<>();
      singleTour.add(tourId);

      _allCrumbsWithAllTours.clear();
      _allCrumbsWithAllTours.add(singleTour);
   }

   /**
    * Set bread crumb with new tours
    *
    * @param allTourData
    */
   public void setBreadcrumbTours(final ArrayList<TourData> allTourData) {

      // keep all breadcrumbs when a new tour collection is contained in it
      if (isCollectionContainedInAllBreadcrums(allTourData)) {

         return;
      }

      _allCrumbsWithAllTours.clear();

      addBreadcrumTours(allTourData);
   }

}
