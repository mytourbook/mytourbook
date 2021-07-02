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
package net.tourbook.map2.action;

import de.byteholder.geoclipse.mapprovider.MP;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.tooltip.ActionToolbarSlideoutAdv;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.common.util.Util;
import net.tourbook.map2.view.Map2View;
import net.tourbook.map2.view.Slideout_Map2_MapProvider;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.ToolItem;

public class ActionMap2_MapProvider extends ActionToolbarSlideoutAdv {

   private static final ImageDescriptor _actionImageDescriptor = TourbookPlugin.getThemedImageDescriptor(Images.MapProvider);

   private IDialogSettings              _state_MapProvider;

   private Slideout_Map2_MapProvider    _slideoutMap2MapProvider;

   private Map2View                     _map2View;

   public ActionMap2_MapProvider(final Map2View map2View, final IDialogSettings state_MapProvider) {

      super(_actionImageDescriptor, _actionImageDescriptor);

      _map2View = map2View;
      _state_MapProvider = state_MapProvider;
   }

   @Override
   protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

      _slideoutMap2MapProvider = new Slideout_Map2_MapProvider(toolItem, _map2View, _state_MapProvider);
      _slideoutMap2MapProvider.setSlideoutLocation(SlideoutLocation.BELOW_RIGHT);

      return _slideoutMap2MapProvider;
   }

   public MP getSelectedMapProvider() {

      return _slideoutMap2MapProvider.getSelectedMapProvider();
   }

   @Override
   protected void onBeforeOpenSlideout() {

      _map2View.closeOpenedDialogs(this);
   }

   @Override
   protected void onSelect(final SelectionEvent selectionEvent) {

      super.onSelect(selectionEvent);

      if (Util.isCtrlKeyPressed(selectionEvent)) {

         // select previous map provider
         _slideoutMap2MapProvider.onSelect_MapProvider_Previous();

      } else {

         // select next map provider
         _slideoutMap2MapProvider.onSelect_MapProvider_Next();
      }
   }

   public void selectMapProvider(final String mapProviderID) {

      if (_slideoutMap2MapProvider == null) {
         return;
      }

      _slideoutMap2MapProvider.selectMapProvider(mapProviderID);
   }

}
