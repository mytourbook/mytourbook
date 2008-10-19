/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.part.ViewPart;

public class TourMapPropertyView extends ViewPart {

	public static final String	ID	= "net.tourbook.mapping.TourMapPropertyView";	//$NON-NLS-1$

	private Button				fRadioTileInfoNo;
	private Button				fRadioTileInfoYes;

	private Scale				fScaleDimMap;

	private void createLayout(final Composite parent) {

		Label label;

		final ScrolledComposite scrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolledContainer.setExpandVertical(true);
		scrolledContainer.setExpandHorizontal(true);

		final Composite viewContainer = new Composite(scrolledContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(viewContainer);

		scrolledContainer.setContent(viewContainer);
		scrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				scrolledContainer.setMinSize(viewContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		final Composite infoContainer = new Composite(viewContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(infoContainer);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(infoContainer);

		{
			/*
			 * tile info
			 */
			label = new Label(infoContainer, SWT.NONE);
			label.setText(Messages.map_properties_show_tile_info);

			// group: yes/no
			final Composite groupChartType = new Composite(infoContainer, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(groupChartType);

			{
				// radio: no
				fRadioTileInfoNo = new Button(groupChartType, SWT.RADIO);
				fRadioTileInfoNo.setText(Messages.map_properties_show_tile_info_no);
				fRadioTileInfoNo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {
						onChangeProperty();
					}
				});

				// radio: yes
				fRadioTileInfoYes = new Button(groupChartType, SWT.RADIO);
				fRadioTileInfoYes.setText(Messages.map_properties_show_tile_info_yes);
				fRadioTileInfoYes.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {
						onChangeProperty();
					}
				});
			}

			/*
			 * scale: dim map
			 */
			label = new Label(infoContainer, SWT.NONE);
			label.setText(Messages.map_properties_map_dim_level);

			fScaleDimMap = new Scale(infoContainer, SWT.NONE);
			fScaleDimMap.setIncrement(1);
			fScaleDimMap.setPageIncrement(10);
			fScaleDimMap.setMinimum(0);
			fScaleDimMap.setMaximum(100);
			GridDataFactory.fillDefaults().grab(true, false).hint(1, SWT.DEFAULT).applyTo(fScaleDimMap);
			fScaleDimMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
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

		// radio: tile info
		final boolean isShowTileInfo = fRadioTileInfoYes.getSelection();
		store.setValue(MappingView.PREF_SHOW_TILE_INFO, isShowTileInfo);

		// dim level
		store.setValue(MappingView.PREF_DEBUG_MAP_DIM_LEVEL, fScaleDimMap.getSelection());

	}

	private void restoreSettings() {

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		// get values from pref store

		// tile info
		final boolean isShowTileInfo = store.getBoolean(MappingView.PREF_SHOW_TILE_INFO);
		if (isShowTileInfo) {
			fRadioTileInfoYes.setSelection(true);
		} else {
			fRadioTileInfoNo.setSelection(true);
		}

		// dim map
		fScaleDimMap.setSelection(store.getInt(MappingView.PREF_DEBUG_MAP_DIM_LEVEL));
	}

	@Override
	public void setFocus() {}

}
