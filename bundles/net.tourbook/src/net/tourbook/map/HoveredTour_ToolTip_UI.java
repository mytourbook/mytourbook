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
package net.tourbook.map;

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.PointLong;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.IPinned_ToolTip;
import net.tourbook.common.tooltip.IPinned_Tooltip_Owner;
import net.tourbook.common.tooltip.Pinned_ToolTip_Shell;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * This tooltip is displayed when mouse is hovering a tour in the map.
 */
public class HoveredTour_ToolTip_UI extends Pinned_ToolTip_Shell implements IPinned_ToolTip {

   private final IPreferenceStore  _prefStore     = TourbookPlugin.getPrefStore();

   private IPropertyChangeListener _prefChangeListener;

   private TourData                _tourData;

   private int                     _devXMouse;
   private int                     _devYMouse;

   /**
    * Global state if the tooltip is visible.
    */
   private boolean                 _isToolTipVisible;
   private int                     _currentValueIndex;

   private int[]                   _updateCounter = new int[] { 0 };
   private long                    _lastUpdateUITime;
   private boolean                 _isHorizontal;

   /*
    * UI resources
    */

   private final ArrayList<Control> _firstColumnControls          = new ArrayList<>();
   private final ArrayList<Control> _firstColumnContainerControls = new ArrayList<>();

   /*
    * UI controls
    */
   private Composite _shellContainer;

   public HoveredTour_ToolTip_UI(final IPinned_Tooltip_Owner tooltipOwner, final IDialogSettings state) {

      super(tooltipOwner, state);

      // get state if the tooltip is visible or hidden
      _isToolTipVisible = _prefStore.getBoolean(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE);

      addPrefListener();
   }

   void actionHideToolTip() {

      _prefStore.setValue(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE, false);

      _isToolTipVisible = false;

      hide();
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            /*
             * create a new chart configuration when the preferences has changed
             */
            if (property.equals(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE)
            //
            ) {
               _isToolTipVisible = (Boolean) event.getNewValue();

               if (_isToolTipVisible) {
                  show(new Point(_devXMouse, _devYMouse));
               } else {
                  hide();
               }
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void createActions() {

   }

   @Override
   protected Composite createToolTipContentArea(final Event event, final Composite parent) {

      createActions();

      final Composite shell = createUI(parent);

      return shell;
   }

   private Composite createUI(final Composite parent) {

      _firstColumnControls.clear();
      _firstColumnContainerControls.clear();

      final Composite shell = createUI_010_Shell(parent);

      updateUI(_currentValueIndex);

      if (_isHorizontal == false) {

         // compute width for all controls and equalize column width for the different sections
         _shellContainer.layout(true, true);
         UI.setEqualizeColumWidths(_firstColumnControls);

         _shellContainer.layout(true, true);
         UI.setEqualizeColumWidths(_firstColumnContainerControls);
      }

      return shell;

   }

   private Composite createUI_010_Shell(final Composite parent) {

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory
            .fillDefaults()//
            .spacing(0, 0)
            .numColumns(2)
            // set margin to draw the border
            .extendedMargins(1, 1, 1, 1)
            .applyTo(_shellContainer);
      _shellContainer.addPaintListener(new PaintListener() {
         @Override
         public void paintControl(final PaintEvent e) {
            onPaintShellContainer(e);
         }
      });
//      _shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         final Label label = new Label(parent, SWT.NONE);
         GridDataFactory.fillDefaults().applyTo(label);
         label.setText("Tooltip content");
      }

      return _shellContainer;
   }

   @Override
   public Shell getToolTipShell() {
      return super.getToolTipShell();
   }

   public boolean isVisible() {
      return _isToolTipVisible;
   }

   @Override
   protected void onDispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      _firstColumnControls.clear();
      _firstColumnContainerControls.clear();

      super.onDispose();
   }

   private void onPaintShellContainer(final PaintEvent event) {

//      final GC gc = event.gc;
//      final Point shellSize = _shellContainer.getSize();
//
//      // draw border
//      gc.setForeground(_fgBorder);
//      gc.drawRectangle(0, 0, shellSize.x - 1, shellSize.y - 1);

   }

   /**
    * Reopens the tooltip at the current position, this will not show the tooltip when it is set to
    * be hidden.
    */
   public void reopen() {

      // hide and recreate it
      hide();
      show(new Point(_devXMouse, _devXMouse));
   }

   public void setHoveredTour(final ArrayList<Long> allHoveredTours) {



   }

   @Override
   public void setSnapBorder(final int marginTop, final int marginBottom) {

      this.snapBorder_Top = marginTop;
      this.snapBorder_Bottom = marginBottom;
   }

   /**
    * @param tourData
    *           When <code>null</code> the tooltip will be hidden.
    */
   void setTourData(final TourData tourData) {

      _tourData = tourData;
      _currentValueIndex = 0;

      if (tourData == null) {
         hide();
         return;
      }

      // reopen when other tour data are set which has other graphs
      reopen();
   }

   @Override
   public void setValueIndex(final int valueIndex,
                             final int devXMouseMove,
                             final int devYMouseMove,
                             final PointLong valueDevPosition,
                             final double chartZoomFactor) {

      if (_tourData == null || _isToolTipVisible == false) {
         return;
      }

      _devXMouse = devXMouseMove;
      _devYMouse = devYMouseMove;

      if (_shellContainer == null || _shellContainer.isDisposed()) {

         /*
          * tool tip is disposed, this happens on a mouse exit, display the tooltip again
          */
         show(new Point(devXMouseMove, devYMouseMove));
      }

      // check again
      if (_shellContainer != null && !_shellContainer.isDisposed()) {

         setTTShellLocation(devXMouseMove, devYMouseMove, valueDevPosition);

         updateUI(valueIndex);
      }
   }

   @Override
   protected boolean shouldCreateToolTip(final Event event) {

      if (_tourData == null) {
         return false;
      }

      return super.shouldCreateToolTip(event);
   }

   @Override
   public void show(final Point location) {

      if (_isToolTipVisible) {
         super.show(location);
      }
   }

   private void updateUI(final int valueIndex) {

      // get time when the redraw is requested
      final long requestedRedrawTime = System.currentTimeMillis();

      if (requestedRedrawTime > _lastUpdateUITime + 100) {

         // force a redraw

         updateUI_Runnable(valueIndex);

      } else {

         _updateCounter[0]++;

         _shellContainer.getDisplay().asyncExec(new Runnable() {

            final int __runnableCounter = _updateCounter[0];

            @Override
            public void run() {

               // update UI delayed
               if (__runnableCounter != _updateCounter[0]) {
                  // a new update UI occured
                  return;
               }

               updateUI_Runnable(valueIndex);
            }
         });
      }

   }

   private void updateUI_Runnable(int valueIndex) {

      if (_shellContainer == null || _shellContainer.isDisposed()) {
         return;
      }

      final int[] timeSerie = _tourData.timeSerie;

      if (timeSerie == null) {
         // this happened with .fitlog import files
         return;
      }

      // check bounds
      if (valueIndex < 0 || valueIndex >= timeSerie.length) {
         valueIndex = timeSerie.length - 1;
      }

      _currentValueIndex = valueIndex;

      _lastUpdateUITime = System.currentTimeMillis();
   }

}
