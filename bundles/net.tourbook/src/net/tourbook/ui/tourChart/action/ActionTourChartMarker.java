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
package net.tourbook.ui.tourChart.action;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.ui.tourChart.SlideoutTourChartMarker;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class ActionTourChartMarker extends ContributionItem implements IOpeningDialog {

   private final String            _dialogId = getClass().getCanonicalName();

   private TourChart               _tourChart;

   private ToolBar                 _toolBar;
   private ToolItem                _actionToolItem;

   private SlideoutTourChartMarker _slideoutMarkerOptions;

   /*
    * UI controls
    */
   private Control _parent;

   private Image   _imageEnabled;
   private Image   _imageDisabled;

   public ActionTourChartMarker(final TourChart tourChart, final Control parent) {

      _tourChart = tourChart;
      _parent = parent;

      _imageEnabled = TourbookPlugin.getThemedImageDescriptor(Images.TourMarker).createImage();
      _imageDisabled = TourbookPlugin.getImageDescriptor(Images.TourMarker_Disabled).createImage();

      parent.addDisposeListener(disposeEvent -> onDispose());
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

         _slideoutMarkerOptions = new SlideoutTourChartMarker(_parent, _toolBar, _tourChart);

         updateUI();
      }
   }

   @Override
   public String getDialogId() {
      return _dialogId;
   }

   @Override
   public void hideDialog() {
      _slideoutMarkerOptions.hide();
   }

   private void onAction() {

      updateUI();

      final boolean isMarkerVisible = _actionToolItem.getSelection();

      if (isMarkerVisible) {

         final Rectangle itemBounds = _actionToolItem.getBounds();

         final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

         itemBounds.x = itemDisplayPosition.x;
         itemBounds.y = itemDisplayPosition.y;

         openSlideout(itemBounds, false);

      } else {

         _slideoutMarkerOptions.close();
      }

      _tourChart.actionShowTourMarker(isMarkerVisible);
   }

   private void onDispose() {

      if (_imageEnabled != null) {
         _imageEnabled.dispose();
      }

      if (_imageDisabled != null) {
         _imageDisabled.dispose();
      }
   }

   private void onMouseMove(final ToolItem item) {

      // ignore other items
      if (item != _actionToolItem) {
         return;
      }

      if (_actionToolItem.getSelection() == false || _actionToolItem.isEnabled() == false) {

         // marker is not displayed

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

      _tourChart.closeOpenedDialogs(this);
      _slideoutMarkerOptions.open(itemBounds, isOpenDelayed);
   }

   public void setEnabled(final boolean isEnabled) {

      _actionToolItem.setEnabled(isEnabled);

      if (isEnabled && _actionToolItem.getSelection() == false) {

         // show default icon
         _actionToolItem.setImage(_imageEnabled);
      }
   }

   public void setSelected(final boolean isSelected) {

      if (_actionToolItem == null) {
         // this happened
         return;
      }

      _actionToolItem.setSelection(isSelected);

      updateUI();
   }

   private void updateUI() {

      if (_actionToolItem.getSelection()) {

         // hide tooltip because the marker options slideout is displayed

         _actionToolItem.setToolTipText(UI.EMPTY_STRING);

      } else {

         _actionToolItem.setToolTipText(Messages.Tour_Action_MarkerOptions_Tooltip);
      }
   }
}
