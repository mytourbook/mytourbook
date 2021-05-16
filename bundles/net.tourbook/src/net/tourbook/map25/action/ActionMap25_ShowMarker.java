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
package net.tourbook.map25.action;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.util.Util;
import net.tourbook.map25.Map25View;
import net.tourbook.map25.ui.SlideoutMap25_MarkerOptions;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class ActionMap25_ShowMarker extends ContributionItem implements IOpeningDialog {

   private IDialogSettings             _state    = TourbookPlugin.getState("net.tourbook.map25.action.ActionTourMapMarker"); //$NON-NLS-1$

   private final String                _dialogId = getClass().getCanonicalName();

   private Map25View                   _map25View;

   private ToolBar                     _toolBar;
   private ToolItem                    _actionToolItem;

   private SlideoutMap25_MarkerOptions _slideoutMarkerOptions;

   /*
    * UI controls
    */
   private Control _parent;

   private Image   _imageEnabled;
   private Image   _imageDisabled;

   public ActionMap25_ShowMarker(final Map25View map25View, final Control parent) {

      _map25View = map25View;
      _parent = parent;

      _imageEnabled = TourbookPlugin.getThemedImageDescriptor(Images.TourMarker).createImage();
      _imageDisabled = TourbookPlugin.getThemedImageDescriptor(Images.TourMarker_Disabled).createImage();

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
         _actionToolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onAction();
            }
         });

         toolbar.addMouseMoveListener(mouseEvent -> {

            final Point mousePosition = new Point(mouseEvent.x, mouseEvent.y);
            final ToolItem hoveredItem = toolbar.getItem(mousePosition);

            onMouseMove(hoveredItem, mouseEvent);
         });

         _slideoutMarkerOptions = new SlideoutMap25_MarkerOptions(_parent, _toolBar, _state, _map25View);

         updateUI_ActionTooltip();
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

      updateUI_ActionTooltip();

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

      _map25View.actionShowTourMarker(isMarkerVisible);
   }

   private void onDispose() {

      Util.disposeResource(_imageEnabled);
      Util.disposeResource(_imageDisabled);
   }

   private void onMouseMove(final ToolItem item, final MouseEvent mouseEvent) {

      // ignore other items
      if (item != _actionToolItem) {
         return;
      }

      if (_actionToolItem.getSelection() == false || _actionToolItem.isEnabled() == false) {

         // marker is not displayed

         return;
      }

      final boolean isToolItemHovered = item == _actionToolItem;

      Rectangle itemBounds = null;

      if (isToolItemHovered) {

         itemBounds = item.getBounds();

         final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

         itemBounds.x = itemDisplayPosition.x;
         itemBounds.y = itemDisplayPosition.y;
      }

      openSlideout(itemBounds, true);
   }

   private void openSlideout(final Rectangle itemBounds, final boolean isOpenDelayed) {

      _map25View.closeOpenedDialogs(this);
      _slideoutMarkerOptions.open(itemBounds, isOpenDelayed);
   }

   public void setEnabled(final boolean isEnabled) {

      if (_actionToolItem == null) {
         // this happened
         return;
      }

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

      updateUI_ActionTooltip();
   }

   private void updateUI_ActionTooltip() {

      if (_actionToolItem.getSelection()) {

         // hide tooltip because the marker options slideout is displayed

         _actionToolItem.setToolTipText(UI.EMPTY_STRING);

      } else {

         _actionToolItem.setToolTipText(Messages.Tour_Action_MapMarkerOptions_Tooltip);
      }
   }
}
