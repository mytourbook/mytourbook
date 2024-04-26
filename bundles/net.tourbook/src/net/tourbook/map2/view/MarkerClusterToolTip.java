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
package net.tourbook.map2.view;

import de.byteholder.geoclipse.map.Map2;
import de.byteholder.geoclipse.map.PaintedMarkerCluster;

import java.util.Collection;

import net.tourbook.common.UI;
import net.tourbook.common.util.ToolTip;
import net.tourbook.map25.layer.marker.MapMarker;
import net.tourbook.map25.layer.marker.algorithm.distance.StaticCluster;

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
import org.eclipse.swt.widgets.Label;

/**
 * Tour info tooltip, implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
public class MarkerClusterToolTip extends ToolTip {

   private Map2                 _map2;

   private PaintedMarkerCluster _hoveredItem;

   public MarkerClusterToolTip(final Map2 map2) {

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

      final StaticCluster<?> markerCluster = _hoveredItem.markerCluster;
      final Collection<?> allClusterItems = markerCluster.getItems();

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).spacing(5, 3).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         int itemIndex = 0;

         for (final Object clusterItem : allClusterItems) {

            if (clusterItem instanceof final MapMarker mapMarker) {

               if (itemIndex++ >= 10) {

                  final Label label = new Label(container, SWT.NONE);
                  label.setText("\n... %d additional markers".formatted(allClusterItems.size() - itemIndex + 1));

                  break;

               } else {

                  String title = mapMarker.title;
                  if (UI.IS_SCRAMBLE_DATA) {
                     title = UI.scrambleText(title);
                  }

                  final Label label = new Label(container, SWT.NONE);
                  label.setText(title);
               }
            }
         }
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
         final int devMouseY = devMouse.y;

         final Rectangle clusterBounds = _hoveredItem.clusterRectangle;
         final int clusterWidth = clusterBounds.width;
         final int clusterWidth2 = clusterWidth / 2;
         final int clusterHeight = clusterBounds.height;
         final int clusterHeight2 = clusterBounds.height / 2;

         // center horizontally to the mouse position
         int devX = devMouseX - tipSizeWidth / 2;
         int devY = clusterBounds.y + clusterHeight;

         // offset that the tooltip can be hovered with the mouse
         final int grabOffset = Math.min(10, clusterWidth2);

         /*
          * Set x/y depending on the mouse position, that the tooltip do not cover the side from
          * where the cluster is hovered
          */
         if (devMouseX < clusterBounds.x + clusterWidth2) {

            // show on the right side
            devX = clusterBounds.x + clusterWidth - grabOffset;

         } else if (devMouseX > clusterBounds.x + clusterWidth2) {

            // show on the left side
            devX = clusterBounds.x - tipSizeWidth + grabOffset;
         }

         if (devMouseY < clusterBounds.y + clusterHeight2) {

            // show on the right side
            devY = clusterBounds.y + clusterHeight - grabOffset;

         } else if (devMouseY > clusterBounds.y + clusterHeight2) {

            // show on the left side
            devY = clusterBounds.y - tipSizeHeight + grabOffset;
         }

         /*
          * Check if the tooltip is outside of the parent
          */
         final Rectangle mapBounds = _map2.getBounds();
         boolean isDevXAdjusted = false;

         if (devX >= mapBounds.width) {
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

               ttDisplayLocation = _map2.toDisplay(devMouseX - tipSizeWidth / 2 - clusterWidth2 + 20 - tipSizeWidth, devY);

            } else {

               ttDisplayLocation.x = ttDisplayLocation.x - tipSizeWidth;
            }
         }

         if (ttDisplayLocation.y + tipSizeHeight > displayBounds.height) {

            /*
             * Adjust vertical position, it is outside of the display, prevent default
             * repositioning
             */

            ttDisplayLocation.y = ttDisplayLocation.y - tipSizeHeight - clusterHeight;
         }

         return fixupDisplayBoundsWithMonitor(tipSize, ttDisplayLocation);
      }

      return super.getLocation(tipSize, event);
   }

   @Override
   protected Object getToolTipArea(final Event event) {

      _hoveredItem = _map2.getHoveredClusterMarker();

      return _hoveredItem;
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
