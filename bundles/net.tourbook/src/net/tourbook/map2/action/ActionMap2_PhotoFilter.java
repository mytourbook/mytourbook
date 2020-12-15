/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.ActionToolbarSlideoutAdv;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.common.util.Util;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.photo.Slideout_Map2_PhotoFilter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.ToolItem;

public class ActionMap2_PhotoFilter extends ActionToolbarSlideoutAdv {

   private static final String          STATE_IS_PHOTO_FILTER_ACTIVE = "STATE_IS_PHOTO_FILTER_ACTIVE";                                              //$NON-NLS-1$

   private static final ImageDescriptor _actionImageDescriptor       = TourbookPlugin.getImageDescriptor(Messages.image_action_change_tile_factory);

   private IDialogSettings              _state;

   private Slideout_Map2_PhotoFilter    _slideoutPhotoFilter;

   private Map2View                     _map2View;

   /*
    * UI resources
    */
   private Image _imageEnabled;
   private Image _imageEnabledNoPhotos;
   private Image _imageEnabledWithPhotos;
   private Image _imageDisabled;

   public ActionMap2_PhotoFilter(final Map2View map2View, final IDialogSettings state) {

      super(_actionImageDescriptor, _actionImageDescriptor);

      _map2View = map2View;
      _state = state;

      isToggleAction = true;
      notSelectedTooltip = Messages.Map_Action_PhotoFilter2_Tooltip;

      _imageEnabled = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_FILTER);
      _imageEnabledNoPhotos = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_FILTER_NO_PHOTOS);
      _imageEnabledWithPhotos = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_FILTER_WITH_PHOTOS);
      _imageDisabled = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_FILTER_DISABLED);
   }

   @Override
   protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

      _slideoutPhotoFilter = new Slideout_Map2_PhotoFilter(toolItem, _map2View, _state);
      _slideoutPhotoFilter.setSlideoutLocation(SlideoutLocation.BELOW_RIGHT);

      return _slideoutPhotoFilter;
   }

   @Override
   protected void onBeforeOpenSlideout() {

      _map2View.closeOpenedDialogs(this);
   }

   @Override
   protected void onSelect(final SelectionEvent selectionEvent) {

      super.onSelect(selectionEvent);

   }

   public void restoreState() {

      setSelection(Util.getStateBoolean(_state, STATE_IS_PHOTO_FILTER_ACTIVE, false));

      _slideoutPhotoFilter.restoreState();

//      updateUI_ActionTooltip();
//
//      _mapView.actionPhotoProperties(_isFilterActive);
   }

   public void saveState() {

      _state.put(STATE_IS_PHOTO_FILTER_ACTIVE, getSelection());

      _slideoutPhotoFilter.saveState();
   }
}
