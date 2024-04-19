/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.map.location;

import de.byteholder.geoclipse.map.Map2;
import de.byteholder.geoclipse.map.PaintedMapLocation;

import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.util.ToolTip;
import net.tourbook.tour.location.TourLocationExtended;
import net.tourbook.tour.location.TourLocationUI;
import net.tourbook.ui.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;

/**
 * Tour info tooltip, implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
public class MapLocationToolTip extends ToolTip {

   private Map2               _map2;

   private PaintedMapLocation _hoveredItem;

   public MapLocationToolTip(final Map2 map2) {

      super(map2, NO_RECREATE, true);

      _map2 = map2;

      // allow the actions to be selected
      setHideOnMouseDown(false);

      setPopupDelay(20);
   }

   @Override
   public Composite createToolTipContentArea(final Event event, final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {
         final Composite _ttContainer = new Composite(shellContainer, SWT.NONE);
         GridLayoutFactory.fillDefaults().margins(SHELL_MARGIN, SHELL_MARGIN).applyTo(_ttContainer);
         {
            createUI(_ttContainer);
         }
      }

      return shellContainer;
   }

   private void createUI(final Composite parent) {

      final TourLocationExtended tourLocationExtended = _hoveredItem.tourLocationExtended;

      final GridDataFactory headerIndent = GridDataFactory.fillDefaults()

            .span(2, 1)

            // indent to the left that this text is aligned with the labels
            .indent(-4, 0);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).spacing(5, 3).applyTo(container);
      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Title
             */

            // using text control that & is not displayed as mnemonic
            final Text headerTitle = new Text(container, SWT.READ_ONLY);
            headerIndent.applyTo(headerTitle);
            MTFont.setBannerFont(headerTitle);

            String headerText = UI.EMPTY_STRING;

            switch (tourLocationExtended.locationType) {
            case Common   -> headerText = Messages.Tour_Location_Label_CommonLocation;
            case Tour      -> headerText = Messages.Tour_Location_Tooltip_Title;
            case TourStart -> headerText = Messages.ColumnFactory_Tour_LocationStart_Title;
            case TourEnd   -> headerText = Messages.ColumnFactory_Tour_LocationEnd_Title;
            }

            headerTitle.setText(headerText);
         }

         UI.createSpacer_Vertical(container, 8, 2);

         {
            /*
             * Display name
             */

            final String displayName = tourLocationExtended.tourLocation.display_name;

            if (displayName != null && displayName.length() > 0) {

               final Text text = new Text(container, SWT.READ_ONLY | SWT.WRAP);
               headerIndent.applyTo(text);

               text.setText(displayName);

               setMaxContentWidth(text);
            }
         }

         UI.createSpacer_Vertical(container, 16, 2);

         /*
          * Location fields
          */
         TourLocationUI.createUI(container, tourLocationExtended.tourLocation);
      }
   }

   @Override
   public Point getLocation(final Point tipSize, final Event event) {

      if (_hoveredItem != null) {

         final Display display = _map2.getDisplay();

         final int tipSizeWidth = tipSize.x;
         final int tipSizeHeight = tipSize.y;

         final Point devMouse = _map2.toControl(display.getCursorLocation());
         final int devMouseX = devMouse.x;

         final Rectangle itemBounds = _hoveredItem.locationRectangle;
         final int itemWidth2 = itemBounds.width / 2;
         final int itemHeight = itemBounds.height;

         // center horizontally to the mouse position
         final int devXDefault = devMouseX - tipSizeWidth / 2;
         final int devY = itemBounds.y + itemHeight;

         /*
          * Check if the tooltip is outside of the parent
          */
         final Rectangle mapBounds = _map2.getBounds();
         boolean isDevXAdjusted = false;
         int devX = devXDefault;

         if (devXDefault >= mapBounds.width) {
            devX = mapBounds.width - 40;
            isDevXAdjusted = true;
         }

         final Rectangle displayBounds = display.getBounds();

         Point ttDisplayLocation = _map2.toDisplay(devX, devY);

         if (ttDisplayLocation.x + tipSizeWidth > displayBounds.width) {

            /*
             * Adjust horizontal position, it is outside of the display, prevent default
             * repositioning
             */

            if (isDevXAdjusted) {

               ttDisplayLocation = _map2.toDisplay(devXDefault - itemWidth2 + 20 - tipSizeWidth, devY);

            } else {

               ttDisplayLocation.x = ttDisplayLocation.x - tipSizeWidth;
            }
         }

         if (ttDisplayLocation.y + tipSizeHeight > displayBounds.height) {

            /*
             * Adjust vertical position, it is outside of the display, prevent default
             * repositioning
             */

            ttDisplayLocation.y = ttDisplayLocation.y - tipSizeHeight - itemHeight;
         }

         return fixupDisplayBoundsWithMonitor(tipSize, ttDisplayLocation);
      }

      return super.getLocation(tipSize, event);
   }

   @Override
   protected Object getToolTipArea(final Event event) {

      final PaintedMapLocation ttArea = _hoveredItem = _map2.getHoveredMapLocation();

      return ttArea;
   }

   private void setMaxContentWidth(final Control control) {

      final int maxContentWidth = 300;

      final GridData gd = (GridData) control.getLayoutData();
      final Point contentSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);

      if (contentSize.x > maxContentWidth) {

         // adjust max width
         gd.widthHint = maxContentWidth;

      } else {

         // reset layout width
         gd.widthHint = SWT.DEFAULT;
      }
   }

   @Override
   protected boolean shouldCreateToolTip(final Event event) {

//      if (_map2.isShowMapLocationTooltip() == false) {
//         return false;
//      }

      if (super.shouldCreateToolTip(event) == false) {
         return false;
      }

      if (_hoveredItem == null) {
         return false;
      }

      return true;
   }
}
