/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

package net.tourbook.mapping;

import net.tourbook.plugin.TourbookPlugin;

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

public class TourMapPropertyView extends ViewPart {

	public static final String	ID	= "net.tourbook.mapping.TourMapPropertyView";	//$NON-NLS-1$

	private Button				_chkTileInfo;
	private Button				_chkTileBorder;

	private Scale				_scaleDimMap;

	private void createLayout(final Composite parent) {

		Label label;

		final Composite infoContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(infoContainer);
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
			GridDataFactory.fillDefaults().grab(true, false).hint(1, SWT.DEFAULT).applyTo(_scaleDimMap);
			_scaleDimMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeProperty();
				}
			});

			/*
			 * tile info
			 */
			_chkTileInfo = new Button(infoContainer, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkTileInfo);
			_chkTileInfo.setText(Messages.Map_Properties_ShowTileInfo);
			_chkTileInfo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					onChangeProperty();
				}
			});

			/*
			 * tile border
			 */
			_chkTileBorder = new Button(infoContainer, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkTileBorder);
			_chkTileBorder.setText(Messages.Map_Properties_ShowTileBorder);
			_chkTileBorder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					onChangeProperty();
				}
			});
		}
	}

	@Override
	public void createPartControl(final Composite parent) {

		createLayout(parent);

		restoreSettings();
	}

	/**
	 * Property was changed, fire a property change event
	 */
	private void onChangeProperty() {

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		// set new values in the pref store

		// tile info/border
		store.setValue(TourMapView.PREF_SHOW_TILE_INFO, _chkTileInfo.getSelection());
		store.setValue(TourMapView.PREF_SHOW_TILE_BORDER, _chkTileBorder.getSelection());

		// dim level
		store.setValue(TourMapView.PREF_DEBUG_MAP_DIM_LEVEL, _scaleDimMap.getSelection());
	}

	private void restoreSettings() {

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		// get values from pref store

		// tile info/border
		_chkTileInfo.setSelection(store.getBoolean(TourMapView.PREF_SHOW_TILE_INFO));
		_chkTileBorder.setSelection(store.getBoolean(TourMapView.PREF_SHOW_TILE_BORDER));

		// dim map
		_scaleDimMap.setSelection(store.getInt(TourMapView.PREF_DEBUG_MAP_DIM_LEVEL));
	}

	@Override
	public void setFocus() {}

}
