/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Action to open a slideout in a toolbar.
 */
public abstract class ActionToolbarSlideout extends ContributionItem implements IOpeningDialog {

   private String          _dialogId               = getClass().getCanonicalName();

   private ToolBar         _toolBar;
   private ToolItem        _actionToolItem;

   private ToolbarSlideout _toolbarSlideout;

   /**
    * When <code>true</code> then the action can be toggeled, default is <code>false</code>.
    */
   protected boolean       isToggleAction;

   /**
    * When <code>true</code> then the slideout is always displayed when mouse is hovering the
    * action.
    */
   protected boolean       isShowSlideoutAlways;

   /**
    * This tooltip will be displayed when the action is not selected.
    */
   protected String        notSelectedTooltip      = UI.EMPTY_STRING;

   private boolean         _stateActionSelection;

   /**
    * When <code>true</code> then the images must be disposed, otherwise they must not be disposed,
    * default is <code>true</code>
    */
   private boolean         _canDisposeActionImages = true;

   private ImageDescriptor _actionImageDescriptor_Enabled;
   private ImageDescriptor _actionImageDescriptor_Disabled;

   /*
    * UI controls
    */
   private Image _actionImage_Enabled;
   private Image _actionImage_Disabled;

   // additional enabled images
   private ArrayList<Image>           _allOtherEnabledImages             = new ArrayList<>();
   private ArrayList<ImageDescriptor> _allOtherEnabledImages_Descriptors = new ArrayList<>();

   public ActionToolbarSlideout() {

      _actionImageDescriptor_Enabled = CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions);
      _actionImageDescriptor_Disabled = CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions_Disabled);
   }

   public ActionToolbarSlideout(final Image graphImage, final Image graphImage_Disabled) {

      _actionImage_Enabled = graphImage;
      _actionImage_Disabled = graphImage_Disabled;

      // prevent to dispose the provided images
      _canDisposeActionImages = false;
   }

   public ActionToolbarSlideout(final ImageDescriptor actionImageDescriptor,
                                final ImageDescriptor actionImageDescriptor_Disabled) {

      _actionImageDescriptor_Enabled = actionImageDescriptor;
      _actionImageDescriptor_Disabled = actionImageDescriptor_Disabled;
   }

   public void addOtherEnabledImage(final ImageDescriptor imageDescriptor) {

      // create image placeholder, image is created when necessary
      _allOtherEnabledImages.add(null);

      // keep image descriptor
      _allOtherEnabledImages_Descriptors.add(imageDescriptor);
   }

   protected abstract ToolbarSlideout createSlideout(ToolBar toolbar);

   @Override
   public void dispose() {

      if (_canDisposeActionImages) {

         UI.disposeResource(_actionImage_Enabled);
         UI.disposeResource(_actionImage_Disabled);
      }

      for (final Image image : _allOtherEnabledImages) {
         UI.disposeResource(image);
      }

      _allOtherEnabledImages.clear();

      _actionImage_Enabled = null;
      _actionImage_Disabled = null;
   }

   private void dispose_Toolbar() {

      if (_actionToolItem != null) {

         // keep selected state that later on it can be retrieved
         _stateActionSelection = _actionToolItem.getSelection();

         _actionToolItem.dispose();
         _actionToolItem = null;
      }

      if (_canDisposeActionImages) {

         UI.disposeResource(_actionImage_Enabled);
         UI.disposeResource(_actionImage_Disabled);
      }
   }

   @Override
   public void fill(final ToolBar toolbar, final int index) {

      if (toolbar == null) {
         return;
      }

      if (_actionToolItem == null || _actionToolItem.isDisposed()) {

         toolbar.addDisposeListener(disposeEvent -> dispose_Toolbar());

         _toolBar = toolbar;

         if (isToggleAction) {
            _actionToolItem = new ToolItem(toolbar, SWT.CHECK);
         } else {
            _actionToolItem = new ToolItem(toolbar, SWT.PUSH);
         }

         _actionToolItem.setImage(getActionImage_Enabled());
         _actionToolItem.setDisabledImage(getActionImage_Disabled());

         _actionToolItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect()));

         toolbar.addMouseMoveListener(mouseEvent -> {

            final Point mousePosition = new Point(mouseEvent.x, mouseEvent.y);
            final ToolItem hoveredItem = toolbar.getItem(mousePosition);

            onMouseMove(hoveredItem);
         });

         _toolbarSlideout = createSlideout(toolbar);

         updateUI_Tooltip();
      }
   }

   public Image getActionImage_Disabled() {

      if (_actionImage_Disabled != null && _actionImage_Disabled.isDisposed() == false) {

         return _actionImage_Disabled;
      }

      if (_actionImageDescriptor_Disabled != null) {

         _actionImage_Disabled = _actionImageDescriptor_Disabled.createImage(true);
      }

      return _actionImage_Disabled;
   }

   public Image getActionImage_Enabled() {

      if (_actionImage_Enabled != null && _actionImage_Enabled.isDisposed() == false) {

         return _actionImage_Enabled;
      }

      if (_actionImageDescriptor_Enabled != null) {

         _actionImage_Enabled = _actionImageDescriptor_Enabled.createImage(true);
      }

      return _actionImage_Enabled;
   }

   @Override
   public String getDialogId() {
      return _dialogId;
   }

   /**
    * @return Returns <code>true</code> when the action is selected, otherwise <code>false</code>.
    */
   public boolean getSelection() {

      if (_actionToolItem == null || _actionToolItem.isDisposed()) {

         // this case occured in E4

         return _stateActionSelection;
      }

      return _actionToolItem.getSelection();
   }

   @Override
   public void hideDialog() {

      _toolbarSlideout.hideNow();
   }

   /**
    * Is called before the slideout is opened, this allows to close other slideouts/dialogs
    */
   protected void onBeforeOpenSlideout() {

   }

   private void onMouseMove(final ToolItem hoveredItem) {

      // ignore other items in the toolbar
      if (hoveredItem != _actionToolItem) {
         return;
      }

      // ignore when disabled
      if (_actionToolItem.isEnabled() == false) {
         return;
      }

      // ignore when not selected
      if (isShowSlideoutAlways == false && isToggleAction && _actionToolItem.getSelection() == false) {
         return;
      }

      // get tooltip position
      final Rectangle itemBounds = hoveredItem.getBounds();

      final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      openSlideout(itemBounds, true);
   }

   /**
    * Is called when the action item is selected or deselected. This will open/close the slideout,
    * the selection state is available with {@link #getSelection()}.
    */
   protected void onSelect() {

      // ignore when it can not toggle
      if (isToggleAction == false) {
         return;
      }

      updateUI_Tooltip();

      // toggle slideout visibility
      if (_toolbarSlideout.isToolTipVisible()) {

         // tooltip is visible -> hide

         _toolbarSlideout.close();

      } else {

         // tooltip is hidden -> open it

         final Rectangle itemBounds = _actionToolItem.getBounds();

         final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

         itemBounds.x = itemDisplayPosition.x;
         itemBounds.y = itemDisplayPosition.y;

         openSlideout(itemBounds, false);
      }
   }

   private void openSlideout(final Rectangle itemBounds, final boolean isOpenDelayed) {

      // ensure other dialogs are closed
      onBeforeOpenSlideout();

      _toolbarSlideout.open(itemBounds, isOpenDelayed);
   }

   public void setEnabled(final boolean isEnabled) {

      if (_actionToolItem == null) {
         // this can occure when the toolbar is not yet fully created
         return;
      }

      _actionToolItem.setEnabled(isEnabled);

      if (isEnabled && _actionToolItem.getSelection() == false) {

         // show default icon
         _actionToolItem.setImage(getActionImage_Enabled());
      }
   }

   public void setSelection(final boolean isSelected) {

      if (_actionToolItem == null) {

         // this happened
         return;
      }

      _actionToolItem.setSelection(isSelected);

      updateUI_Tooltip();
   }

   public void setTooltip(final String object) {

      if (_actionToolItem == null) {

         // this happened
         return;
      }

      _actionToolItem.setToolTipText(object);
   }

   public void showDefaultEnabledImage() {

      _actionToolItem.setImage(getActionImage_Enabled());
   }

   /**
    * @param imageNumber
    */
   public void showOtherEnabledImage(final int imageNumber) {

      if (_actionToolItem == null) {

         // this happened
         return;
      }

      Assert.isTrue(imageNumber < _allOtherEnabledImages_Descriptors.size(), "Image number is larger than the available images");//$NON-NLS-1$

      Image image = _allOtherEnabledImages.get(imageNumber);

      if (image == null) {

         // create image

         image = _allOtherEnabledImages_Descriptors.get(imageNumber).createImage();
         _allOtherEnabledImages.set(imageNumber, image);
      }

      _actionToolItem.setImage(image);
   }

   private void updateUI_Tooltip() {

      if (_actionToolItem.getSelection()) {

         // hide tooltip because the slideout is displayed

         _actionToolItem.setToolTipText(UI.EMPTY_STRING);

      } else {

         _actionToolItem.setToolTipText(notSelectedTooltip);
      }
   }
}
