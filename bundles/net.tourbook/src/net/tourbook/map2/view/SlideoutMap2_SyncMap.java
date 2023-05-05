/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout to sync map with...
 */
public class SlideoutMap2_SyncMap extends ToolbarSlideout {

   /*
    * UI controls
    */
   private Map2View _map2View;

   public SlideoutMap2_SyncMap(final Control ownerControl,
                                final ToolBar toolBar,
                                final Map2View map2View) {

      super(ownerControl, toolBar);

      _map2View = map2View;
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      final Composite ui = createUI(parent);

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         createUI_10_Title(shellContainer);
         createUI_20_Graphs(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(Messages.Slideout_Map_SyncMap_Label_Title);
      MTFont.setBannerFont(label);
   }

   private void createUI_20_Graphs(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
         final ToolBarManager tbm = new ToolBarManager(toolbar);

         // actions are managed in the map2 view but they are displayed in this slideout

         tbm.add(_map2View.getAction_MapSync(MapSyncId.SyncMapWith_Tour));
         tbm.add(_map2View.getAction_MapSync(MapSyncId.SyncMapWith_Slider_One));
         tbm.add(_map2View.getAction_MapSync(MapSyncId.SyncMapWith_Slider_Centered));
         tbm.add(_map2View.getAction_MapSync(MapSyncId.SyncMapWith_ValuePoint));
         tbm.add(_map2View.getAction_MapSync(MapSyncId.SyncMapWith_Photo));
         tbm.add(_map2View.getAction_MapSync(MapSyncId.SyncMapWith_OtherMap));

         tbm.update(true);
      }
   }

   @Override
   protected boolean isCenterHorizontal() {
      return true;
   }

   @Override
   protected void onDispose() {

   }

}
