/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
import net.tourbook.ui.tourChart.SlideoutTourChartPauses;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class ActionTourChartPauses extends ContributionItem implements IOpeningDialog {

   private String                  _dialogId = getClass().getCanonicalName();

   private TourChart               _tourChart;

   private ToolBar                 _toolBar;
   private ToolItem                _actionToolItem;

   private SlideoutTourChartPauses _slideoutTourChartPause;

   /*
    * UI controls
    */
   private Control _parent;

   private Image   _imageEnabled;
   private Image   _imageDisabled;

   public ActionTourChartPauses(final TourChart tourChart, final Control parent) {

      _tourChart = tourChart;
      _parent = parent;

      _imageEnabled = TourbookPlugin.getImageDescriptor(Images.TourPauses).createImage();
      _imageDisabled = TourbookPlugin.getImageDescriptor(Images.TourPauses_Disabled).createImage();
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

         _slideoutTourChartPause = new SlideoutTourChartPauses(_parent, _toolBar, _tourChart);

         updateUI();
      }
   }

   @Override
   public String getDialogId() {
      return _dialogId;
   }

   @Override
   public void hideDialog() {
      _slideoutTourChartPause.hideNow();
   }

   private void onAction() {

      updateUI();

      final boolean areTourPausesVisible = _actionToolItem.getSelection();

      if (areTourPausesVisible) {

         openSlideout(true);

      } else {

         _slideoutTourChartPause.close();
      }

      _tourChart.actionShowTourPauses(areTourPausesVisible);
   }

   private void onMouseMove(final ToolItem item) {

      // ignore other items
      if (item != _actionToolItem) {
         return;
      }

      if (_actionToolItem.getSelection() == false || _actionToolItem.isEnabled() == false) {

         // pauses are not displayed
         return;
      }

      openSlideout(true);
   }

   private void openSlideout(final boolean isOpenDelayed) {

      final Rectangle itemBounds = _actionToolItem.getBounds();

      final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      openSlideout(itemBounds, isOpenDelayed);
   }

   private void openSlideout(final Rectangle itemBounds, final boolean isOpenDelayed) {

      // ensure other dialogs are closed
      _tourChart.closeOpenedDialogs(this);

      _slideoutTourChartPause.open(itemBounds, isOpenDelayed);
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
         // this happened before
         return;
      }

      _actionToolItem.setSelection(isSelected);

      updateUI();
   }

   private void updateUI() {

      if (_actionToolItem.getSelection()) {

         // hide tooltip because the tour pause options slideout is displayed

         _actionToolItem.setToolTipText(UI.EMPTY_STRING);

      } else {

         _actionToolItem.setToolTipText(Messages.Tour_Action_ShowTourPauses_Tooltip);
      }
   }
}
