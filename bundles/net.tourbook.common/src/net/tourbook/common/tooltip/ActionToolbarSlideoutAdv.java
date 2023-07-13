/*******************************************************************************
 * Copyright (C) 2017, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.common.tooltip;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Action to open a slideout in a toolbar.
 */
public abstract class ActionToolbarSlideoutAdv extends ContributionItem implements IOpeningDialog {

   private String           _dialogId          = getClass().getCanonicalName();

   private ToolItem         _actionToolItem;

   private AdvancedSlideout _toolbarSlideout;

   /**
    * When <code>true</code> then the action can be toggeled, default is <code>false</code>.
    */
   protected boolean        isToggleAction;

   /**
    * This tooltip will be displayed when the action is not selected which causes that the slideout
    * is not displayed.
    */
   public String            notSelectedTooltip = UI.EMPTY_STRING;

   private boolean          _isImageCreated_EnabledDisabled;
   private boolean          _isImageCreated_Selected;

   /*
    * UI controls
    */
   private Image _imageEnabled;
   private Image _imageDisabled;
   private Image _imageSelected;

   public ActionToolbarSlideoutAdv() {

      _imageEnabled = CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions).createImage();
      _imageDisabled = CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions_Disabled).createImage();

      _isImageCreated_EnabledDisabled = true;
   }

   public ActionToolbarSlideoutAdv(final Image actionImage, final Image actionImageDisabled) {

      _imageEnabled = actionImage;
      _imageDisabled = actionImageDisabled;
   }

   public ActionToolbarSlideoutAdv(final ImageDescriptor actionImage, final ImageDescriptor actionImageDisabled) {

      _imageEnabled = actionImage.createImage();
      _imageDisabled = actionImageDisabled.createImage();

      _isImageCreated_EnabledDisabled = true;
   }

   public ActionToolbarSlideoutAdv(final ImageDescriptor actionImage_Enabled,
                                   final ImageDescriptor actionImage_Disabled,
                                   final ImageDescriptor actionImage_Selected) {

      _imageEnabled = actionImage_Enabled.createImage();
      _imageDisabled = actionImage_Disabled.createImage();
      _imageSelected = actionImage_Selected.createImage();

      _isImageCreated_EnabledDisabled = true;
      _isImageCreated_Selected = true;
   }

   protected abstract AdvancedSlideout createSlideout(ToolItem toolItem);

   @Override
   public void fill(final ToolBar toolbar, final int index) {

      if (_imageEnabled == null || _imageEnabled.isDisposed()
            || _imageDisabled == null || _imageDisabled.isDisposed()) {

         return;
      }

      if ((_actionToolItem == null || _actionToolItem.isDisposed()) && toolbar != null) {

         toolbar.addDisposeListener(disposeEvent -> onDispose());

         if (isToggleAction) {
            _actionToolItem = new ToolItem(toolbar, SWT.CHECK);
         } else {
            _actionToolItem = new ToolItem(toolbar, SWT.PUSH);
         }

         _actionToolItem.setImage(_imageEnabled);
         _actionToolItem.setDisabledImage(_imageDisabled);
         _actionToolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelect(e);
            }
         });

         toolbar.addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(final MouseEvent e) {

               final Point mousePosition = new Point(e.x, e.y);
               final ToolItem hoveredItem = toolbar.getItem(mousePosition);

               onMouseMove(hoveredItem, e);
            }
         });

         _toolbarSlideout = createSlideout(_actionToolItem);

         updateUI_ToolItem_Tooltip();
      }
   }

   public ToolItem getActionToolItem() {
      return _actionToolItem;
   }

   @Override
   public String getDialogId() {
      return _dialogId;
   }

   /**
    * @return Returns <code>true</code> when the action is selected, otherwise <code>false</code>.
    */
   public boolean getSelection() {
      return _actionToolItem.getSelection();
   }

   @Override
   public void hideDialog() {
      _toolbarSlideout.hideNow();
   }

   /**
    * Is called before the slideout is opened, this allows to close other dialogs, default is doing
    * nothing.
    */
   protected void onBeforeOpenSlideout() {}

   private void onDispose() {

      if (_actionToolItem != null) {

         _actionToolItem.dispose();
         _actionToolItem = null;
      }

      /**
       * Dispose ONLY those images, which were created here, otherwise an exception is thrown !!!
       * <p>
       * Found finally a solution for this very old leak.
       */

      if (_isImageCreated_EnabledDisabled && _imageEnabled != null && _imageEnabled.isDisposed() == false) {
         
         _imageEnabled.dispose();
      }

      if (_isImageCreated_EnabledDisabled && _imageDisabled != null && _imageDisabled.isDisposed() == false) {
         
         _imageDisabled.dispose();
      }

      if (_isImageCreated_Selected && _imageSelected != null && _imageSelected.isDisposed() == false) {

         _imageSelected.dispose();
      }
   }

   private void onMouseMove(final ToolItem hoveredItem, final MouseEvent mouseEvent) {

      // ignore other items in the toolbar
      if (hoveredItem != _actionToolItem) {

         // hide slideout when other tool items are hovered

         if (_toolbarSlideout != null) {
            _toolbarSlideout.hideNow();
         }

         return;
      }

      // ignore when disabled
      if (_actionToolItem.isEnabled() == false) {
         return;
      }

      // ignore when not selected
      if (isToggleAction && _actionToolItem.getSelection() == false) {
         return;
      }

      openSlideout(true);
   }

   /**
    * Is called when the action item is selected or deselected. This will open/close the slideout,
    * the selection state is available with {@link #getSelection()}.
    *
    * @param e
    */
   protected void onSelect(final SelectionEvent selectionEvent) {

      // ignore when it cannot toggle
      if (isToggleAction == false) {
         return;
      }

      updateUI_ToolItem_Image();
      updateUI_ToolItem_Tooltip();

      if (_toolbarSlideout.isVisible() == false) {

         // tooltip is hidden, open it

         openSlideout(false);

      } else {

         _toolbarSlideout.close();
      }
   }

   private void openSlideout(final boolean isOpenDelayed) {

      // get tooltip position
      final Rectangle itemBounds = _actionToolItem.getBounds();

      // update position, relative -> absolute
      final Point itemDisplayPosition = _actionToolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);
      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      // ensure other dialogs are closed
      onBeforeOpenSlideout();

      _toolbarSlideout.open(isOpenDelayed);
   }

   public void setEnabled(final boolean isEnabled) {

      if (_actionToolItem == null) {
         // this can occure when the toolbar is not yet fully created
         return;
      }

      _actionToolItem.setEnabled(isEnabled);

      if (isEnabled && _actionToolItem.getSelection() == false) {

         // show default icon
         _actionToolItem.setImage(_imageEnabled);
      }
   }

   public void setSelection(final boolean isSelected) {

      if (_actionToolItem == null) {
         // this happened
         return;
      }

      _actionToolItem.setSelection(isSelected);

      updateUI_ToolItem_Image();
      updateUI_ToolItem_Tooltip();
   }

   @Override
   public void update() {

      if (_toolbarSlideout != null) {
         return;
      }
   }

   /**
    * Show other image when selected
    *
    * @param _actionToolItem2
    */
   protected void updateUI_ToolItem_Image() {

      if (_imageSelected != null) {

         // selected image is available

         if (_actionToolItem.getSelection()) {
            _actionToolItem.setImage(_imageSelected);
         } else {
            _actionToolItem.setImage(_imageEnabled);
         }
      }
   }

   private void updateUI_ToolItem_Tooltip() {

      if (_actionToolItem.getSelection()) {

         // hide tooltip because the slideout is displayed

         _actionToolItem.setToolTipText(UI.EMPTY_STRING);

      } else {

         _actionToolItem.setToolTipText(notSelectedTooltip);
      }
   }
}
