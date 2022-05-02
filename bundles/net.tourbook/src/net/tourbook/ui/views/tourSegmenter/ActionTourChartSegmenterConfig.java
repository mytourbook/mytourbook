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
package net.tourbook.ui.views.tourSegmenter;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class ActionTourChartSegmenterConfig extends ContributionItem {

   private TourSegmenterView                    _tourSegmenterView;

   private ToolBar                              _toolBar;
   private ToolItem                             _actionToolItem;

   private SlideoutTourChartSegmenterProperties _slideoutTCSConfig;

   /*
    * UI controls
    */
   private Control _parent;

   private Image   _imageEnabled;
   private Image   _imageDisabled;

   private Boolean _initialIsSelected = null;

   public ActionTourChartSegmenterConfig(final TourSegmenterView tourSegmenterView, final Control parent) {

      _tourSegmenterView = tourSegmenterView;
      _parent = parent;

      _imageEnabled = TourbookPlugin.getThemedImageDescriptor(Images.TourSegments).createImage();
      _imageDisabled = TourbookPlugin.getThemedImageDescriptor(Images.TourSegments_Disabled).createImage();

      _parent.addDisposeListener(disposeEvent -> {

         _imageEnabled.dispose();
         _imageDisabled.dispose();
      });
   }

   @Override
   public void fill(final ToolBar toolbar, final int index) {

      if (_actionToolItem == null && toolbar != null) {

         toolbar.addDisposeListener(disposeEvent -> {
            _actionToolItem.dispose();
            _actionToolItem = null;
         });

         _toolBar = toolbar;

         _actionToolItem = new ToolItem(toolbar, SWT.CHECK);
         _actionToolItem.setImage(_imageEnabled);
         _actionToolItem.setDisabledImage(_imageDisabled);
         _actionToolItem.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onAction()));

         toolbar.addMouseMoveListener(mouseEvent -> {

            final Point mousePosition = new Point(mouseEvent.x, mouseEvent.y);
            final ToolItem hoveredItem = toolbar.getItem(mousePosition);

            onMouseMove(hoveredItem);
         });

         _slideoutTCSConfig = new SlideoutTourChartSegmenterProperties(_parent, _toolBar, _tourSegmenterView);

         /**
          * This is highly complex
          * <p>
          * The initial selection is not working because the toolbar is created 2 times !!!
          */
         if (_initialIsSelected != null) {
            _actionToolItem.setSelection(_initialIsSelected);
         }

         updateUI_Action();
      }
   }

   private void onAction() {

      final boolean isActionSelected = _actionToolItem.getSelection();

      // update state
      TourSegmenterView.getState().put(TourSegmenterView.STATE_IS_SHOW_TOUR_SEGMENTS, isActionSelected);

      updateUI_Action();

      if (isActionSelected) {

         final Rectangle itemBounds = _actionToolItem.getBounds();

         final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

         itemBounds.x = itemDisplayPosition.x;
         itemBounds.y = itemDisplayPosition.y;

         openSlideout(itemBounds, false);

      } else {

         _slideoutTCSConfig.close();
      }

      _tourSegmenterView.fireSegmentLayerChanged();
   }

   private void onMouseMove(final ToolItem item) {

      // ignore other items
      if (item != _actionToolItem) {
         return;
      }

      // check if action is active
      if (_actionToolItem.getSelection() == false || _actionToolItem.isEnabled() == false) {
         return;
      }

      Rectangle itemBounds = null;

      itemBounds = item.getBounds();

      final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      openSlideout(itemBounds, true);
   }

   private void openSlideout(final Rectangle itemBounds, final boolean isOpenDelayed) {

      _slideoutTCSConfig.open(itemBounds, isOpenDelayed);
   }

   public void setEnabled(final boolean isEnabled) {

      _actionToolItem.setEnabled(isEnabled);

      if (isEnabled && _actionToolItem.getSelection() == false) {

         // show default icon
         _actionToolItem.setImage(_imageEnabled);
      }
   }

   public void setSelected(final boolean isSelected) {

      // keep selection
      if (_initialIsSelected == null) {
         _initialIsSelected = isSelected;
      }

      if (_actionToolItem == null) {
         // this happened
         return;
      }

      _actionToolItem.setSelection(isSelected);

      updateUI_Action();
   }

   private void updateUI_Action() {

      if (_actionToolItem.getSelection()) {

         // hide tooltip because the tour info options slideout is displayed

         _actionToolItem.setToolTipText(UI.EMPTY_STRING);

      } else {

         _actionToolItem.setToolTipText(Messages.Tour_Segmenter_Action_ShowHideSegmentsInTourChart_Tooltip);
      }
   }
}
