/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.map25.Map25Provider;
import net.tourbook.map25.Map25ProviderManager;
import net.tourbook.map25.TileEncoding;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageMap25MapsforgeProvider extends PreferencePage implements IWorkbenchPreferencePage {

// SET_FORMATTING_OFF

	public static final String				ID									= "net.tourbook.preferences.PrefPage_Map25_Provider";		//$NON-NLS-1$

	private static final String				STATE_LAST_SELECTED_MAP_PROVIDER	= "STATE_LAST_SELECTED_MAP_PROVIDER";					//$NON-NLS-1$
	
	private static final TileEncodingData[] _allTileEncoding					= new TileEncodingData[] {

		new TileEncodingData(TileEncoding.MVT, Messages.Pref_Map25_Encoding_Mapzen),
		new TileEncodingData(TileEncoding.VTM, Messages.Pref_Map25_Encoding_OpenScienceMap),
		new TileEncodingData(TileEncoding.MF, "Local Mapsforgefile")
	};

	
// SET_FORMATTING_ON

	private final IDialogSettings			_state								= TourbookPlugin.getState(ID);

	private ArrayList<Map25Provider>		_allMapProvider;
	//
	private ModifyListener					_defaultModifyListener;
	private SelectionListener				_defaultSelectionListener;
	//
	private Map25Provider					_newProvider;
	private Map25Provider					_selectedMapProvider;
	//
	private boolean							_isModified;
	private boolean							_isMapProviderModified;
	private boolean							_isInUpdateUI;
	//
	/*
	 * UI Controls
	 */
	private TableViewer						_mapProviderViewer;
	//
	private Button							_chkIsEnabled;
	//
	private Button							_btnAddProvider;
	private Button							_btnCancel;
	private Button							_btnDeleteProvider;
	private Button							_btnUpdateProvider;
	//
	private Text							_txtAPIKey;
	private Text							_txtDescription;
	private Text							_txtProviderName;
	private Text							_txtTilePath;
	private Text							_txtTileUrl;
	private Text							_txtUrl;
	private String							_styles = "";
	//
	private Combo							_comboTileEncoding;

	private class MapProvider_ContentProvider implements IStructuredContentProvider {

		public MapProvider_ContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {
			return _allMapProvider.toArray(new Map25Provider[_allMapProvider.size()]);
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {

		}
	}

	public static class TileEncodingData {

		private TileEncoding	__encoding;
		private String			__text;

		public TileEncodingData(final TileEncoding encoding, final String text) {
			__encoding = encoding;
			__text = text;
		}
	}

	private void addPrefListener() {
		// TODO Auto-generated method stub

	}

	@Override
	protected Control createContents(final Composite parent) {

		initUI(parent);

		final Composite container = createUI(parent);

		// update viewer
		_allMapProvider = createMapProviderClone();
		_mapProviderViewer.setInput(new Object());

		// reselect previous map provider
		restoreState();

		enableControls();
		addPrefListener();

		return container;
	}

	private ArrayList<Map25Provider> createMapProviderClone() {

		/*
		 * Clone original data
		 */
		final ArrayList<Map25Provider> clonedMapProvider = new ArrayList<>();

		for (final Map25Provider mapProvider : Map25ProviderManager.getAllMapProviders()) {
			clonedMapProvider.add((Map25Provider) mapProvider.clone());
		}

		/*
		 * Sort by name
		 */
		Collections.sort(clonedMapProvider, new Comparator<Map25Provider>() {

			@Override
			public int compare(final Map25Provider mp1, final Map25Provider mp2) {
				return mp1.name.compareTo(mp2.name);
			}
		});

		return clonedMapProvider;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				//				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{

			final Label label = new Label(container, SWT.WRAP);
			label.setText(Messages.Pref_Map25_Provider_Label_Title);

			final Composite innerContainer = new Composite(container, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					.grab(true, true)
					.applyTo(innerContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(innerContainer);
			{
				createUI_10_Provider_Viewer(innerContainer);
				createUI_20_Provider_Actions(innerContainer);

				createUI_30_Details(innerContainer);
				createUI_40_Details_Actions(innerContainer);
			}

			// placeholder
			new Label(container, SWT.NONE);
		}

		return container;
	}

	private void createUI_10_Provider_Viewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory
				.fillDefaults() //
				.grab(true, true)
				.hint(convertWidthInCharsToPixels(70), convertHeightInCharsToPixels(10))
				.applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(
				layoutContainer,
				(SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));

		table.setHeaderVisible(true);

		_mapProviderViewer = new TableViewer(table);
		defineAllColumns(tableLayout);

		_mapProviderViewer.setUseHashlookup(true);
		_mapProviderViewer.setContentProvider(new MapProvider_ContentProvider());

		_mapProviderViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {

				// compare by name

				final Map25Provider p1 = (Map25Provider) e1;
				final Map25Provider p2 = (Map25Provider) e2;

				return p1.name.compareTo(p2.name);
			}
		});

		_mapProviderViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onProvider_Select();
			}
		});

		_mapProviderViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

				_txtProviderName.setFocus();
				_txtProviderName.selectAll();
			}
		});

	}

	private void createUI_20_Provider_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				//				.grab(false, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				/*
				 * Button: Add
				 */
				_btnAddProvider = new Button(container, SWT.NONE);
				_btnAddProvider.setText(Messages.App_Action_Add);
				setButtonLayoutData(_btnAddProvider);
				_btnAddProvider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProvider_Add();
					}
				});
			}
			{
				/*
				 * Button: Delete
				 */
				_btnDeleteProvider = new Button(container, SWT.NONE);
				_btnDeleteProvider.setText(Messages.App_Action_Delete);
				setButtonLayoutData(_btnDeleteProvider);
				_btnDeleteProvider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProvider_Delete();
					}
				});
			}
		}
	}

	private void createUI_30_Details(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{

			{
				/*
				 * Checkbox: Is enabled
				 */
				// spacer
				new Label(container, SWT.NONE);

				_chkIsEnabled = new Button(container, SWT.CHECK);
				_chkIsEnabled.setText(Messages.Pref_Map25_Provider_Checkbox_IsEnabled);
				_chkIsEnabled.setToolTipText(Messages.Pref_Map25_Provider_Checkbox_IsEnabled_Tooltip);
				_chkIsEnabled.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						//						.span(2, 1)
						.applyTo(_chkIsEnabled);
			}
			{
				/*
				 * Field: Provider name
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Pref_Map25_Provider_Label_ProviderName);

				_txtProviderName = new Text(container, SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_txtProviderName);
				_txtProviderName.addModifyListener(_defaultModifyListener);
			}
			{
				/*
				 * Field: Mapsforge Map path
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText("Mapsforge Map path");

				_txtUrl = new Text(container, SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_txtUrl);
				_txtUrl.addModifyListener(_defaultModifyListener);
			}

			{
				/*
				 * Field: Mapsforge Theme path
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText("Mapsforge Theme Path");

				_txtTilePath = new Text(container, SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_txtTilePath);
				_txtTilePath.addModifyListener(_defaultModifyListener);
			}
			{
				/*
				 * Field: Styles
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText("Availible Styles");

				_txtTileUrl = new Text(container, SWT.READ_ONLY);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_txtTileUrl);
			}
			{
				/*
				 * Field: Tile Encoding
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Pref_Map25_Provider_Label_TileEncoding);

				_comboTileEncoding = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
//				GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(_comboTileEncoding);
				_comboTileEncoding.setVisibleItemCount(20);
				_comboTileEncoding.addSelectionListener(_defaultSelectionListener);

				// fill combobox
				for (final TileEncodingData encodingData : _allTileEncoding) {
					_comboTileEncoding.add(encodingData.__text);
				}
			}
			{
				/*
				 * Field: Mapsforge Theme Renderstyle Name
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText("Renderstyle");

				_txtAPIKey = new Text(container, SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_txtAPIKey);
				_txtAPIKey.addModifyListener(_defaultModifyListener);
			}
			{
				/*
				 * Field: Description
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Pref_Map25_Provider_Label_Description);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.applyTo(label);

				_txtDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
				GridDataFactory
						.fillDefaults()//
						.hint(convertWidthInCharsToPixels(20), convertHeightInCharsToPixels(8))
						.grab(true, false)
						.applyTo(_txtDescription);
				_txtDescription.addModifyListener(_defaultModifyListener);
			}
		}

	}

	private void createUI_40_Details_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				//				.grab(false, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				/*
				 * Button: Update
				 */
				_btnUpdateProvider = new Button(container, SWT.NONE);
				_btnUpdateProvider.setText(Messages.app_action_update);
				_btnUpdateProvider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProvider_Update();
					}
				});
				setButtonLayoutData(_btnUpdateProvider);
			}
			{
				/*
				 * Button: Cancel
				 */
				_btnCancel = new Button(container, SWT.NONE);
				_btnCancel.setText(Messages.App_Action_Cancel);
				_btnCancel.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProvider_Cancel();
					}
				});
				setButtonLayoutData(_btnCancel);

//				final GridData gd = (GridData) _btnCancel.getLayoutData();
//				gd.verticalAlignment = SWT.BOTTOM;
//				gd.grabExcessVerticalSpace = true;
			}
		}
	}

	private void defineAllColumns(final TableColumnLayout tableLayout) {

		final int minWidth = convertWidthInCharsToPixels(5);

		TableViewerColumn tvc;
		TableColumn tc;

		{
			/*
			 * Column: Can be used
			 */
			tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Pref_Map25_Provider_Column_Enabled);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final boolean isEnabled = ((Map25Provider) cell.getElement()).isEnabled;

					cell.setText(isEnabled ? Messages.App_Label_BooleanYes : Messages.App_Label_BooleanNo);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(4, minWidth));
		}
		{
			/*
			 * Column: Provider name
			 */
			tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Pref_Map25_Provider_Column_ProviderName);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					cell.setText(((Map25Provider) cell.getElement()).name);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(5, minWidth));
		}
		{
			/*
			 * Column: Tile encoding
			 */
			tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Pref_Map25_Provider_Column_TileEncoding);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					cell.setText(((Map25Provider) cell.getElement()).tileEncoding.name());
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(4, minWidth));
		}
		{
			/*
			 * Column: Mapsforgemapfile
			 */
			tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText("Map Path");
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					cell.setText(((Map25Provider) cell.getElement()).url);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(7, minWidth));
		}
		{
			/*
			 * Column: Themefile path
			 */
			tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText("Theme Path");
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					cell.setText(((Map25Provider) cell.getElement()).tilePath);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(5, minWidth));
		}
		{
			/*
			 * Column: Renderstyle
			 */
			tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText("Renderstyle");
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					cell.setText(((Map25Provider) cell.getElement()).apiKey);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(4, minWidth));
		}
	}

	private void deleteOfflineMapFiles(final Map25Provider map25Provider) {

//		if (MapProviderManager.deleteOfflineMap(map25Provider, false)) {
//
//			map25Provider.setStateToReloadOfflineCounter();
//
//			// update viewer
//			_mpViewer.update(map25Provider, null);
//
//			updateUIOfflineInfoTotal();
//
//			// clear map image cache
//			map25Provider.disposeTileImages();
//		}
	}

	private void enableControls() {

		final boolean isSelected = _selectedMapProvider != null;
		final boolean isEnabled = isSelected ? _selectedMapProvider.isEnabled : false;
		final boolean isDefault = _selectedMapProvider.isDefault;
		final boolean isNew = _newProvider != null;
		final boolean canEdit = isEnabled && (isSelected || isNew);
		final boolean isNotDefault = isDefault == false;

		final boolean isValid = isDataValid();

		_mapProviderViewer.getTable().setEnabled(!_isMapProviderModified && isValid);

		_btnAddProvider.setEnabled(!_isMapProviderModified && isValid);
		_btnUpdateProvider.setEnabled(_isMapProviderModified && isValid);
		_btnCancel.setEnabled(_isMapProviderModified);
		_btnDeleteProvider.setEnabled(isSelected && isNotDefault);

		_chkIsEnabled.setEnabled((isSelected || isNew) && isNotDefault);
		_comboTileEncoding.setEnabled(canEdit);
		_txtAPIKey.setEnabled(canEdit);
		_txtDescription.setEnabled(canEdit);
		_txtProviderName.setEnabled(canEdit);
		_txtTilePath.setEnabled(canEdit);
		_txtUrl.setEnabled(canEdit);
	}

	private int getEncodingIndex(final TileEncoding tileEncoding) {

		for (int encodingIndex = 0; encodingIndex < _allTileEncoding.length; encodingIndex++) {

			final TileEncodingData tileEncodingData = _allTileEncoding[encodingIndex];

			if (tileEncoding.equals(tileEncodingData.__encoding)) {
				return encodingIndex;
			}
		}

		/*
		 * return default, open science map
		 */
		int defaultIndex = 0;

		for (int encodingIndex = 0; encodingIndex < _allTileEncoding.length; encodingIndex++) {

			final TileEncodingData tileEncodingData = _allTileEncoding[encodingIndex];

			if (tileEncodingData.__encoding.equals(TileEncoding.VTM)) {
				defaultIndex = encodingIndex;
				break;
			}
		}

		return defaultIndex;
	}

	private TileEncoding getSelectedEncoding() {

		final int selectedIndex = _comboTileEncoding.getSelectionIndex();

		if (selectedIndex < 0) {

			// return default
			return TileEncoding.VTM;
		}

		return _allTileEncoding[selectedIndex].__encoding;
	}

	@Override
	public void init(final IWorkbench workbench) {

		noDefaultAndApplyButton();

		_defaultModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				onProvider_Modify();
			}
		};

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onProvider_Modify();
			}
		};
	}

	private void initUI(final Composite parent) {

	}

	/**
	 * @return Returns <code>true</code> when person is valid, otherwise <code>false</code>.
	 */
	private boolean isDataValid() {
		//String styles ="Aviable Styles: ";
		final boolean isNewProvider = _newProvider != null;
		
		if (isNewProvider || _isMapProviderModified) {

			if (_txtProviderName.getText().trim().equals(UI.EMPTY_STRING)) {
				setErrorMessage(Messages.Pref_Map25_Provider_Error_ProviderNameIsRequired);
				return false;

			} else if (_txtUrl.getText().trim().equals(UI.EMPTY_STRING)) {
				setErrorMessage("Mapfile (.map) is required, cant be empty");
				return false;
				
			} else if (checkFile(_txtUrl.getText().trim()).equals("1")) {  //TO DO: call checkFile() only once per event
				setErrorMessage("Mapfile does not exist");
				return false;
				
			} else if (checkFile(_txtUrl.getText().trim()).equals("2")) {
				setErrorMessage("Mapfile is not a file");
				return false;

			} else if (checkFile(_txtUrl.getText().trim()).equals("3")) {
				setErrorMessage("can not read the map file");
				return false;
	
			} else if (_txtTilePath.getText().trim().equals(UI.EMPTY_STRING)) {
				setErrorMessage("Themefile (.xml) is required");
				this._styles ="";
				return false;
				
			} else if (checkFile(_txtTilePath.getText().trim()).equals("1")) {
				setErrorMessage("Themefile does not exist");
				this._styles ="";
				return false;
				
			} else if (checkFile(_txtTilePath.getText().trim()).equals("2")) {
				setErrorMessage("Themefile is not a file");
				this._styles ="";
				return false;
				
			} else if (checkFile(_txtTilePath.getText().trim()).equals("3")) {
				setErrorMessage("can not read the theme file");
				this._styles ="";
				return false;
			}

			MapsforgeStyleParser mf_style_parser = new MapsforgeStyleParser();
			//java.util.List<Item> mf_styles = mf_style_parser.readConfig("C:\\Users\\top\\BTSync\\oruxmaps\\mapstyles\\ELV4\\Elevate.xml");
			java.util.List<Style> mf_styles = mf_style_parser.readXML(_txtTilePath.getText().trim());
			//System.out.println("####### PrefPageMap25MapsforgeProvider Stylecount: " + mf_styles.size());
			this._styles ="Aviable Styles: ";
			for (Style style : mf_styles) {
				this._styles += style.getXmlLayer();
				this._styles += " (";
				this._styles += style.getName(Locale.getDefault().toString());
				this._styles += "),";
				//System.out.println(item.getXmlLayer());
			}
			this._styles += "all";
			//System.out.println("####### PrefPageMap25MapsforgeProvider isDataValid: " + styles);
			
			/*
			 * Check that at least 1 map provider is enabled
			 */
			final boolean isCurrentEnabled = _chkIsEnabled.getSelection();
			int numEnabledOtherMapProviders = 0;

			for (final Map25Provider map25Provider : _allMapProvider) {

				if (map25Provider.isEnabled && map25Provider != _selectedMapProvider) {
					numEnabledOtherMapProviders++;
				}
			}

			if (isCurrentEnabled || numEnabledOtherMapProviders > 0) {
				// at least one is enabled
			} else {
				setErrorMessage(Messages.Pref_Map25_Provider_Error_EnableMapProvider);

				return false;
			} //end if at least one is enabled
		} // end if modified

		setErrorMessage(null);

		return true;
	} //end function isDataValid

	private boolean isSaveMapProvider() {

		return (MessageDialog.openQuestion(
				Display.getCurrent().getActiveShell(),
				Messages.Pref_Map25_Provider_Dialog_SaveModifiedProvider_Title,
				NLS.bind(
						Messages.Pref_Map25_Provider_Dialog_SaveModifiedProvider_Message,

						// use name from the ui because it could be modified
						_txtProviderName.getText())) == false);
	}

	@Override
	public boolean okToLeave() {

		if (_isMapProviderModified && isDataValid()) {

			updateModelAndUI();
			saveMapProviders(true);
		}

		saveState();

		return super.okToLeave();
	}

	private void onProvider_Add() {

		_newProvider = new Map25Provider();

		_isModified = true;
		_isMapProviderModified = true;

		updateUI_FromProvider(_newProvider);
		enableControls();

		// edit name
		_txtProviderName.setFocus();
	}

	private void onProvider_Cancel() {

		_newProvider = null;
		_isMapProviderModified = false;

		updateUI_FromProvider(_selectedMapProvider);
		enableControls();

		_mapProviderViewer.getTable().setFocus();
	}

	private void onProvider_Delete() {

//		Delete Map Provider
//		Are you sure to delete the map provider "{0}" and all it's offline images?

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.Pref_Map25_Provider_Dialog_ConfirmDeleteMapProvider_Title,
				NLS.bind(
						Messages.Pref_Map25_Provider_Dialog_ConfirmDeleteMapProvider_Message,
						_selectedMapProvider.name)) == false) {
			return;
		}

		_isModified = true;
		_isMapProviderModified = false;

		// get map provider which will be selected when the current will be removed
		final int selectionIndex = _mapProviderViewer.getTable().getSelectionIndex();
		Object nextSelectedMapProvider = _mapProviderViewer.getElementAt(selectionIndex + 1);
		if (nextSelectedMapProvider == null) {
			nextSelectedMapProvider = _mapProviderViewer.getElementAt(selectionIndex - 1);
		}

		// delete offline files
		deleteOfflineMapFiles(_selectedMapProvider);

		// remove from model
		_allMapProvider.remove(_selectedMapProvider);

		// remove from viewer
		_mapProviderViewer.remove(_selectedMapProvider);

		if (nextSelectedMapProvider == null) {

			_selectedMapProvider = null;

			updateUI_FromProvider(_selectedMapProvider);

		} else {

			// select another map provider at the same position

			_mapProviderViewer.setSelection(new StructuredSelection(nextSelectedMapProvider));
			_mapProviderViewer.getTable().setFocus();
		}

		enableControls();
	}

	private void onProvider_Modify() {

		if (_isInUpdateUI) {
			return;
		}

		_isModified = true;
		_isMapProviderModified = true;

		updateUI_Data();

		enableControls();
	}

	private void onProvider_Select() {

		final IStructuredSelection selection = (IStructuredSelection) _mapProviderViewer.getSelection();
		final Map25Provider mapProvider = (Map25Provider) selection.getFirstElement();

		if (mapProvider != null) {

			_selectedMapProvider = mapProvider;

			updateUI_FromProvider(_selectedMapProvider);

		} else {
			// irgnore, this can happen when a refresh() of the table viewer is done
		}

		enableControls();
	}

	private void onProvider_Update() {

		if (isDataValid() == false) {
			return;
		}

		updateModelAndUI();
		enableControls();

		_mapProviderViewer.getTable().setFocus();
	}

	@Override
	public boolean performCancel() {

		saveState();

		return super.performCancel();
	}

	@Override
	public boolean performOk() {

//		final boolean isModified = _isModelModified;

		updateModelAndUI();
		saveMapProviders(false);

//		if (isModified) {
//			/*
//			 * map providers are saved, keep dialog open because this situation happened several
//			 * times during development of this part
//			 */
//			return false;
//		}

		saveState();

		return true;
	}

	private void restoreState() {

		/*
		 * select last selected map provider
		 */
		final String lastMapProviderUrl = Util.getStateString(_state, STATE_LAST_SELECTED_MAP_PROVIDER, null);
		Map25Provider lastMapProvider = null;
		for (final Map25Provider mapProvider : _allMapProvider) {
			if (mapProvider.url.equals(lastMapProviderUrl)) {
				lastMapProvider = mapProvider;
				break;
			}
		}
		if (lastMapProvider != null) {
			_mapProviderViewer.setSelection(new StructuredSelection(lastMapProvider));
		} else if (_allMapProvider.size() > 0) {
			_mapProviderViewer.setSelection(new StructuredSelection(_allMapProvider.get(0)));
		} else {
			// nothing can be selected
		}

		// set focus to selected map provider
		final Table table = _mapProviderViewer.getTable();
		table.setSelection(table.getSelectionIndex());
	}

	/**
	 * @param isAskToSave
	 * @return Returns <code>false</code> when map provider is not saved.
	 */
	private void saveMapProviders(final boolean isAskToSave) {

		if (!_isModified) {

			// nothing is to save
			return;
		}

		boolean isSaveIt = true;

		if (isAskToSave) {
			isSaveIt = isSaveMapProvider();
		}

		if (isSaveIt) {
			Map25ProviderManager.saveMapProvider(_allMapProvider);
			_isModified = false;
		}
	}

	private void saveState() {

		// selected map provider
		if (_selectedMapProvider != null) {
			_state.put(
					STATE_LAST_SELECTED_MAP_PROVIDER,
					_selectedMapProvider.url);
		}
	}

	/**
	 */
	private void updateModelAndUI() {

		final boolean isNewProvider = _newProvider != null;
		final Map25Provider currentMapProvider = isNewProvider ? _newProvider : _selectedMapProvider;

		if (_isMapProviderModified && isDataValid()) {

			updateModelData(currentMapProvider);

			// update ui
			if (isNewProvider) {
				_allMapProvider.add(currentMapProvider);
				_mapProviderViewer.add(currentMapProvider);

			} else {
				// !!! refreshing a map provider do not resort the table when sorting has changed so we refresh the viewer !!!
				_mapProviderViewer.refresh();
			}

			// select updated/new map provider
			_mapProviderViewer.setSelection(new StructuredSelection(currentMapProvider), true);
		}

		// update state
		_isMapProviderModified = false;
		_newProvider = null;
	}

	private void updateModelData(final Map25Provider mapProvider) {

		/*
		 * Update map provider
		 */
		mapProvider.apiKey = _txtAPIKey.getText();
		mapProvider.description = _txtDescription.getText();
		mapProvider.name = _txtProviderName.getText();
		mapProvider.tilePath = _txtTilePath.getText();
		mapProvider.url = _txtUrl.getText();
		mapProvider.isEnabled = _chkIsEnabled.getSelection();

		mapProvider.tileEncoding = getSelectedEncoding();
	}

	private void updateUI_Data() {
		//final String tileUrl = _txtUrl.getText() + _txtTilePath.getText();
		//_txtTileUrl.setText(tileUrl);
		_txtTileUrl.setText(_styles);
	}

	private void updateUI_FromProvider(final Map25Provider mapProvider) {

		_isInUpdateUI = true;
		{
			if (mapProvider == null) {

				_chkIsEnabled.setSelection(false);

				_txtAPIKey.setText(UI.EMPTY_STRING);
				_txtDescription.setText(UI.EMPTY_STRING);
				_txtProviderName.setText(UI.EMPTY_STRING);
				_txtUrl.setText(UI.EMPTY_STRING);
				_txtTilePath.setText(UI.EMPTY_STRING);

			} else {

				_chkIsEnabled.setSelection(mapProvider.isEnabled);

				_txtAPIKey.setText(mapProvider.apiKey);
				_txtDescription.setText(mapProvider.description);
				_txtProviderName.setText(mapProvider.name);
				_txtUrl.setText(mapProvider.url);
				_txtTilePath.setText(mapProvider.tilePath);

				_comboTileEncoding.select(getEncodingIndex(mapProvider.tileEncoding));
			}

			updateUI_Data();
		}
		_isInUpdateUI = false;
	}

	public static String checkFile(String FilePath) {  	
		File file = new File(FilePath);
		if (!file.exists()) {
			System.out.println("############# file not exist: " +  file.getAbsolutePath());
			return "1";
		} else if (!file.isFile()) {
			System.out.println("############# is not a file: " +  file.getAbsolutePath());
			return "2";
		} else if (!file.canRead()) {
			System.out.println("############# can not read file: " +  file.getAbsolutePath());
			return "3";
		}
		return file.getAbsolutePath();
	}	
	
	
}
