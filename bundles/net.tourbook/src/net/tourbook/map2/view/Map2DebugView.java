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
package net.tourbook.map2.view;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.map2.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class Map2DebugView extends ViewPart {

   public static final String ID = "net.tourbook.map2.view.Map2DebugView"; //$NON-NLS-1$

   private Button             _chkGeoGridBorder;
   private Button             _chkTileInfo;
   private Button             _chkTileBorder;

   @Override
   public void createPartControl(final Composite parent) {

      createUI(parent);

      restoreSettings();
   }

   private void createUI(final Composite parent) {

      final Composite infoContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(infoContainer);
      {
         /*
          * tile info
          */
         _chkTileInfo = new Button(infoContainer, SWT.CHECK);
         _chkTileInfo.setText(Messages.Map_Properties_ShowTileInfo);
         _chkTileInfo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
               onChangeProperty();
            }
         });
         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkTileInfo);

         /*
          * tile border
          */
         _chkTileBorder = new Button(infoContainer, SWT.CHECK);
         _chkTileBorder.setText(Messages.Map_Properties_ShowTileBorder);
         _chkTileBorder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
               onChangeProperty();
            }
         });
         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkTileBorder);

         /*
          * Geo grid border
          */
         _chkGeoGridBorder = new Button(infoContainer, SWT.CHECK);
         _chkGeoGridBorder.setText(Messages.Map_Properties_ShowGeoGrid);
         _chkGeoGridBorder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
               onChangeProperty();
            }
         });
         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkGeoGridBorder);
      }
   }

   /**
    * Property was changed, fire a property change event
    */
   private void onChangeProperty() {

      final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

      // set new values in the pref store

      // tile info/border
      store.setValue(Map2View.PREF_DEBUG_MAP_SHOW_GEO_GRID, _chkGeoGridBorder.getSelection());
      store.setValue(Map2View.PREF_SHOW_TILE_INFO, _chkTileInfo.getSelection());
      store.setValue(Map2View.PREF_SHOW_TILE_BORDER, _chkTileBorder.getSelection());
   }

   private void restoreSettings() {

      final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

      // get values from pref store

      // tile info/border
      _chkGeoGridBorder.setSelection(store.getBoolean(Map2View.PREF_DEBUG_MAP_SHOW_GEO_GRID));
      _chkTileInfo.setSelection(store.getBoolean(Map2View.PREF_SHOW_TILE_INFO));
      _chkTileBorder.setSelection(store.getBoolean(Map2View.PREF_SHOW_TILE_BORDER));
   }

   @Override
   public void setFocus() {}

}
