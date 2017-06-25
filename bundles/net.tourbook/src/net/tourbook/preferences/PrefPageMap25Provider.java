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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.map25.Map25Manager;
import net.tourbook.map25.Map25Provider;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageMap25Provider extends PreferencePage implements IWorkbenchPreferencePage {

//	private static final String			ID		= "net.tourbook.preferences.PrefPageMap25Provider";			//$NON-NLS-1$

// SET_FORMATTING_OFF
	
//	private final IDialogSettings		_state	= TourbookPlugin.getDefault().getDialogSettingsSection(ID);
	
// SET_FORMATTING_ON

	private ArrayList<Map25Provider>	_allMapProvider;

	private ModifyListener				_defaultModifyListener;

	private Map25Provider				_newProvider;
	private Map25Provider				_selectedMapProvider;

	private boolean						_isProviderModified;
	private boolean						_isUpdateUI;

	private PixelConverter				_pc;

	/*
	 * UI Controls
	 */
	private TableViewer					_mapProviderViewer;

	private Button						_btnAddProvider;
	private Button						_btnCancel;
	private Button						_btnDeleteProvider;
	private Button						_btnSaveProvider;

	private Text						_txtAPIKey;
	private Text						_txtDescription;
	private Text						_txtProviderName;
	private Text						_txtTilePath;
	private Text						_txtTileUrl;
	private Text						_txtUrl;

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

		enableActions();
		addPrefListener();

		return container;
	}

	private ArrayList<Map25Provider> createMapProviderClone() {

		final ArrayList<Map25Provider> clonedMapProvider = new ArrayList<>();

		for (final Map25Provider mapProvider : Map25Manager.getAllMapProviders()) {
			clonedMapProvider.add((Map25Provider) mapProvider.clone());
		}

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
			label.setText(Messages.Pref_Map25_Label_Title);

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
				.hint(convertWidthInCharsToPixels(30), convertHeightInCharsToPixels(5))
				.applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(
				layoutContainer,
				(SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI));

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
//				_tabFolderPerson.setSelection(0);
//				_txtFirstName.setFocus();
//				_txtFirstName.selectAll();
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
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{

			{
				/*
				 * Field: Provider name
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Pref_Map25_Label_ProviderName);

				_txtProviderName = new Text(container, SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_txtProviderName);
				_txtProviderName.addModifyListener(_defaultModifyListener);
			}
			{
				/*
				 * Field: Description
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Pref_Map25_Label_Description);

				_txtDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
				GridDataFactory
						.fillDefaults()//
						.hint(_pc.convertWidthInCharsToPixels(20), _pc.convertHeightInCharsToPixels(5))
						.grab(true, false)
						.applyTo(_txtDescription);
				_txtDescription.addModifyListener(_defaultModifyListener);
			}

			{
				/*
				 * Field: Url
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Pref_Map25_Label_Url);

				_txtUrl = new Text(container, SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_txtUrl);
				_txtUrl.addModifyListener(_defaultModifyListener);
			}

			{
				/*
				 * Field: Tile path
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Pref_Map25_Label_TilePath);

				_txtTilePath = new Text(container, SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_txtTilePath);
				_txtTilePath.addModifyListener(_defaultModifyListener);
			}

			{
				/*
				 * Field: API key
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Pref_Map25_Label_APIKey);

				_txtAPIKey = new Text(container, SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_txtAPIKey);
				_txtAPIKey.addModifyListener(_defaultModifyListener);
			}
			{
				/*
				 * Field: Tile Url
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Pref_Map25_Label_TileUrl);

				_txtTileUrl = new Text(container, SWT.BORDER | SWT.READ_ONLY);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_txtTileUrl);
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

				final GridData gd = (GridData) _btnCancel.getLayoutData();
				gd.verticalAlignment = SWT.BOTTOM;
				gd.grabExcessVerticalSpace = true;
			}
			{
				/*
				 * Button: Save
				 */
				_btnSaveProvider = new Button(container, SWT.NONE);
				_btnSaveProvider.setText(Messages.App_Action_Save);
				_btnSaveProvider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProvider_Save();
					}
				});
				setButtonLayoutData(_btnSaveProvider);
			}
		}
	}

	private void defineAllColumns(final TableColumnLayout tableLayout) {

		final int minWidth = convertWidthInCharsToPixels(5);

		TableViewerColumn tvc;
		TableColumn tc;

		{
			/*
			 * Column: Provider name
			 */
			tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Pref_Map25_Column_ProviderName);
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
			 * Column: Url
			 */
			tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Pref_Map25_Column_Url);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					cell.setText(((Map25Provider) cell.getElement()).url);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(10, minWidth));
		}
		{
			/*
			 * Column: Tile path
			 */
			tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Pref_Map25_Column_TilePath);
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
			 * Column: API key
			 */
			tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Pref_Map25_Column_APIKey);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					cell.setText(((Map25Provider) cell.getElement()).apiKey);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(4, minWidth));
		}
	}

	private void enableActions() {

		final boolean isSelected = _selectedMapProvider != null;
		final boolean isNew = _newProvider != null;
		final boolean canEdit = isSelected || isNew;

		final boolean isValid = isProviderValid();

		_mapProviderViewer.getTable().setEnabled(!_isProviderModified && isValid);

		_btnAddProvider.setEnabled(!_isProviderModified && isValid);
		_btnSaveProvider.setEnabled(_isProviderModified && isValid);
		_btnCancel.setEnabled(_isProviderModified);
		_btnDeleteProvider.setEnabled(isSelected);

		_txtAPIKey.setEnabled(canEdit);
		_txtProviderName.setEnabled(canEdit);
		_txtTilePath.setEnabled(canEdit);
		_txtUrl.setEnabled(canEdit);
	}

	/**
	 * @return Returns person which is currently displayed, one person is at least available
	 *         therefor this should never return <code>null</code> but it can be <code>null</code>
	 *         when the application is started the first time and people are not yet created.
	 */
	private Map25Provider getCurrentProvider() {

		final boolean isNewPerson = _newProvider != null;
		return isNewPerson ? _newProvider : _selectedMapProvider;
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
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	/**
	 * @return Returns <code>true</code> when person is valid, otherwise <code>false</code>.
	 */
	private boolean isProviderValid() {

		final boolean isNewProvider = _newProvider != null;

		if (isNewProvider || _isProviderModified) {

			if (_txtProviderName.getText().trim().equals(UI.EMPTY_STRING)) {

				setErrorMessage(Messages.Pref_Map25_Error_ProviderNameIsRequired);

				return false;

			} else if (_txtUrl.getText().trim().equals(UI.EMPTY_STRING)) {

				setErrorMessage(Messages.Pref_Map25_Error_UrlIsRequired);

				return false;

			} else if (_txtTilePath.getText().trim().equals(UI.EMPTY_STRING)) {

				setErrorMessage(Messages.Pref_Map25_Error_TilePathIsRequired);

				return false;
			}
		}

		setErrorMessage(null);

		return true;
	}

	private void onProvider_Add() {

		_newProvider = new Map25Provider();
		_isProviderModified = true;

		updateUIFromProvider(_newProvider);
		enableActions();

		// edit name
		_txtProviderName.selectAll();
		_txtProviderName.setFocus();
	}

	private void onProvider_Cancel() {

		_newProvider = null;
		_isProviderModified = false;

		updateUIFromProvider(_selectedMapProvider);
		enableActions();

		_mapProviderViewer.getTable().setFocus();
	}

	private void onProvider_Delete() {

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.Pref_Map25_Dialog_ConfirmDeleteMapProvider_Title,
				NLS.bind(
						Messages.Pref_Map25_Dialog_ConfirmDeleteMapProvider_Message,
						_selectedMapProvider.name)) == false) {
			return;
		}

		// get map provider which will be selected when the current will be removed
		final int selectionIndex = _mapProviderViewer.getTable().getSelectionIndex();
		Object nextSelectedMapProvider = _mapProviderViewer.getElementAt(selectionIndex + 1);
		if (nextSelectedMapProvider == null) {
			nextSelectedMapProvider = _mapProviderViewer.getElementAt(selectionIndex - 1);
		}

		// delete offline files
//		deleteOfflineMapFiles(_selectedMapProvider);

		// remove from viewer
		_mapProviderViewer.remove(_selectedMapProvider);

		// remove from model
//		_mpMgr.remove(_selectedMapProvider);
//		_visibleMp.remove(_selectedMapProvider);

		if (nextSelectedMapProvider == null) {

			_selectedMapProvider = null;

		} else {

			// select another map provider at the same position

			_mapProviderViewer.setSelection(new StructuredSelection(nextSelectedMapProvider));
			_mapProviderViewer.getTable().setFocus();
		}

		// custom map provider list must be updated
//		_isForceUpdateMapProviderList = true;

		enableActions();
	}

	private void onProvider_Modify() {

		if (_isUpdateUI) {
			return;
		}

		_isProviderModified = true;

		enableActions();
	}

	private void onProvider_Save() {

		if (isProviderValid() == false) {
			return;
		}

		saveMapProvider(false, false);
		enableActions();

		_mapProviderViewer.getTable().setFocus();
	}

	private void onProvider_Select() {

		final IStructuredSelection selection = (IStructuredSelection) _mapProviderViewer.getSelection();
		final Map25Provider mapProvider = (Map25Provider) selection.getFirstElement();

		if (mapProvider != null) {

			_selectedMapProvider = mapProvider;

			updateUIFromProvider(_selectedMapProvider);

		} else {
			// irgnore, this can happen when a refresh() of the table viewer is done
		}

		enableActions();
	}

	private void restoreState() {
		// TODO Auto-generated method stub

	}

	/**
	 * @param isAskToSave
	 * @param isRevert
	 * @return Returns <code>false</code> when person is not saved, modifications will be reverted.
	 */
	private boolean saveMapProvider(final boolean isAskToSave, final boolean isRevert) {

		final boolean isNewProvider = _newProvider != null;
		final Map25Provider mapProvider = getCurrentProvider();
		_newProvider = null;

		if (_isProviderModified) {

			if (isAskToSave) {

				if (MessageDialog.openQuestion(
						Display.getCurrent().getActiveShell(),
						Messages.Pref_Map25_Dialog_SaveModifiedProvider_Title,
						NLS.bind(
								Messages.Pref_Map25_Dialog_SaveModifiedProvider_Message,

								// use name from the ui because it could be modified
								_txtProviderName.getText())) == false) {

					// revert person

					if (isRevert) {

						// update state
						_isProviderModified = false;

						// update ui from the previous selected person
						updateUIFromProvider(_selectedMapProvider);
					}

					return false;
				}
			}

			updateProviderFromUI(mapProvider);
			_allMapProvider.add(mapProvider);

			Map25Manager.saveMapProvider();

			// update state
//			_isFireModifyEvent = true;
			_isProviderModified = false;

			// update ui
			if (isNewProvider) {
				_mapProviderViewer.add(mapProvider);

			} else {
				// !!! refreshing a map provider do not resort the table when sorting has changed so we refresh the viewer !!!
				_mapProviderViewer.refresh();
			}

			// select updated/new map provider
			_mapProviderViewer.setSelection(new StructuredSelection(mapProvider), true);
		}

		return true;
	}

	private void updateProviderFromUI(final Map25Provider mapProvider) {

		/*
		 * Update provider
		 */
		mapProvider.apiKey = _txtAPIKey.getText();
		mapProvider.name = _txtProviderName.getText();
		mapProvider.tilePath = _txtTilePath.getText();
		mapProvider.url = _txtUrl.getText();
	}

	private void updateUIFromProvider(final Map25Provider mapProvider) {

		_isUpdateUI = true;
		{
			if (mapProvider == null) {

				_txtProviderName.setText(UI.EMPTY_STRING);
				_txtUrl.setText(UI.EMPTY_STRING);
				_txtTilePath.setText(UI.EMPTY_STRING);
				_txtAPIKey.setText(UI.EMPTY_STRING);

			} else {

				_txtProviderName.setText(mapProvider.name);
				_txtUrl.setText(mapProvider.url);
				_txtTilePath.setText(mapProvider.tilePath);
				_txtAPIKey.setText(mapProvider.apiKey);
			}
		}
		_isUpdateUI = false;
	}

}
