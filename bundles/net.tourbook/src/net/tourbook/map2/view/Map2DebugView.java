/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.part.ViewPart;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.map2.Messages;

public class Map2DebugView extends ViewPart {

   public static final String ID = "net.tourbook.map2.view.Map2DebugView"; //$NON-NLS-1$

   private Button             _chkGeoGridBorder;
   private Button             _chkTileInfo;
   private Button             _chkTileBorder;

   private Scale              _scaleDimMap;

   private void createUI(final Composite parent) {

      Label label;

      final Composite infoContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(infoContainer);
      {
         /*
          * scale: dim map
          */
         label = new Label(infoContainer, SWT.NONE);
         label.setText(Messages.map_properties_map_dim_level);

         _scaleDimMap = new Scale(infoContainer, SWT.NONE);
         _scaleDimMap.setIncrement(1);
         _scaleDimMap.setPageIncrement(10);
         _scaleDimMap.setMinimum(0);
         _scaleDimMap.setMaximum(100);
         _scaleDimMap.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onChangeProperty();
            }
         });
         GridDataFactory.fillDefaults().grab(true, false).hint(1, SWT.DEFAULT).applyTo(_scaleDimMap);

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

   @Override
   public void createPartControl(final Composite parent) {

      createUI(parent);

      restoreSettings();
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

      // dim level
      store.setValue(Map2View.PREF_DEBUG_MAP_DIM_LEVEL, _scaleDimMap.getSelection());
   }

   private void restoreSettings() {

      final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

      // get values from pref store

      // tile info/border
      _chkGeoGridBorder.setSelection(store.getBoolean(Map2View.PREF_DEBUG_MAP_SHOW_GEO_GRID));
      _chkTileInfo.setSelection(store.getBoolean(Map2View.PREF_SHOW_TILE_INFO));
      _chkTileBorder.setSelection(store.getBoolean(Map2View.PREF_SHOW_TILE_BORDER));

      // dim map
      _scaleDimMap.setSelection(store.getInt(Map2View.PREF_DEBUG_MAP_DIM_LEVEL));
   }

   @Override
   public void setFocus() {}

}
