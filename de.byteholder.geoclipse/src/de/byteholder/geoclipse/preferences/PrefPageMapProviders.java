/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.preferences;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.util.StatusUtil;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.progress.UIJob;
import org.geotools.data.ows.OperationType;
import org.geotools.data.ows.Service;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.ows.WMSRequest;
import org.joda.time.DateTime;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.map.TileImageCache;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.mapprovider.DialogMP;
import de.byteholder.geoclipse.mapprovider.DialogMPCustom;
import de.byteholder.geoclipse.mapprovider.DialogMPProfile;
import de.byteholder.geoclipse.mapprovider.DialogMPWms;
import de.byteholder.geoclipse.mapprovider.IOfflineInfoListener;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MPCustom;
import de.byteholder.geoclipse.mapprovider.MPPlugin;
import de.byteholder.geoclipse.mapprovider.MPProfile;
import de.byteholder.geoclipse.mapprovider.MPWms;
import de.byteholder.geoclipse.mapprovider.MPWrapper;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.mapprovider.MapProviderNavigator;
import de.byteholder.geoclipse.ui.MessageDialogNoClose;
import de.byteholder.geoclipse.util.PixelConverter;
import de.byteholder.geoclipse.util.Util;

public class PrefPageMapProviders extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String					CHARACTER_0						= "0";															//$NON-NLS-1$

	public static final String					PREF_PAGE_MAP_PROVIDER_ID		= "de.byteholder.geoclipse.preferences.PrefPageMapProvider";	//$NON-NLS-1$

	private static final String					XML_EXTENSION					= ".xml";														//$NON-NLS-1$

	/**
	 * max lenghth for map provider id and offline folder
	 */
	private static final int					MAX_ID_LENGTH					= 24;

	private static final String					IMPORT_FILE_PATH				= "MapProvider_ImportFilePath";								//$NON-NLS-1$
	private static final String					EXPORT_FILE_PATH				= "MapProvider_ExportFilePath";								//$NON-NLS-1$

	private MapProviderManager					_mpMgr							= MapProviderManager.getInstance();

	/**
	 * contains all visible map providers
	 */
	private ArrayList<MP>						_visibleMp;

	/**
	 * map provider's which are used when getting offline info
	 */
	private ArrayList<MP>						_offlineJobMapProviders			= new ArrayList<MP>();

	private IOfflineInfoListener				_offlineJobInfoListener;

	private Job									_offlineJobGetInfo;
	private MP									_offlineJobMp;
	private int									_offlineJobFileCounter;
	private int									_offlineJobFileSize;
	private int									_offlineJobFileCounterUIUpdate;

	/**
	 * is <code>true</code> when the job is canceled
	 */
	private boolean								_isOfflineJobCanceled			= true;

	private boolean								_isOfflineJobRunning			= false;

	private boolean								_isDisableModifyListener		= false;

	private IPreferenceStore					_prefStore						= Activator
																						.getDefault()
																						.getPreferenceStore();

	/**
	 * map provider which is currently selected in the list
	 */
	private MP									_selectedMapProvider;

	private MP									_newMapProvider;

	/*
	 * UI controls
	 */
	private TableViewer							_mpViewer;
	private Group								_groupDetails;

	private Text								_txtOfflineInfoTotal;
	private Text								_txtMapProviderName;
	private Text								_txtMapProviderId;
	private Text								_txtMapProviderType;
	private Label								_lblDescription;
	private Text								_txtDescription;
	private Composite							_offlineContainer;
	private Text								_txtOfflineFolder;
	private Label								_lblOfflineFolderInfo;
	private Text								_txtUrl;

	private Label								_lblDropTarget;
	private Label								_lblMpDropTarget;
	private DropTarget							_wmsDropTarget;
	private DropTarget							_mpDropTarget;

	private Button								_btnAddMapProviderCustom;
	private Button								_btnAddMapProviderWms;
	private Button								_btnAddMapProviderMapProfile;
	private Button								_btnDeleteMapProvider;
	private Button								_btnDeleteOfflineMap;
	private Button								_btnUpdate;
	private Button								_btnCancel;
	private Button								_btnEdit;
	private Button								_btnImport;
	private Button								_btnExport;

	private boolean								_isNewMapProvider				= false;
	private boolean								_isModifiedMapProvider			= false;
	private boolean								_isModifiedMapProviderList		= false;
	private boolean								_isForceUpdateMapProviderList	= false;
	private boolean								_isValid						= true;

	private final ModifyListener				_modifyListener;

	private boolean								_isModifiedOfflineFolder;
	private boolean								_isModifiedMapProviderId;
	private boolean								_isDeleteError;

	private ActionRefreshOfflineInfoSelected	_actionRefreshSelected;
	private ActionRefreshOfflineInfoAll			_actionRefreshAll;
	private ActionCancelRefreshOfflineInfo		_actionCancelRefresh;
	private ActionRefreshOfflineInfoNotAssessed	_actionRefreshNotAssessed;

	final static NumberFormat					_nf								= NumberFormat.getNumberInstance();

	{
		_nf.setMinimumFractionDigits(2);
		_nf.setMaximumFractionDigits(2);
		_nf.setMinimumIntegerDigits(1);
	}

	private class MapContentProvider implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return _visibleMp.toArray();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public PrefPageMapProviders() {

		noDefaultAndApplyButton();

		_modifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (_isDisableModifyListener == false) {
					setMapProviderModified();
				}
			}
		};
	}

	void actionCancelRefreshOfflineInfo() {
		stopJobOfflineInfo();
	}

	void actionRefreshOfflineInfo(final boolean isRefreshSelectedMapProvider) {

		MP updateMapProvider = null;

		if (isRefreshSelectedMapProvider) {

			/*
			 * refresh selected map provider
			 */

			updateMapProvider = _selectedMapProvider;

		} else {

			/*
			 * refresh all map providers
			 */

			for (final MP mapProvider : _visibleMp) {
				mapProvider.setStateToReloadOfflineCounter();
			}

			_mpViewer.update(_visibleMp.toArray(), null);
		}

		startJobOfflineInfo(updateMapProvider);
	}

	void actionRefreshOfflineInfoNotAssessed() {
		startJobOfflineInfo(null);
	}

	private void addListener() {

		_offlineJobInfoListener = new IOfflineInfoListener() {

			public void offlineInfoIsDirty(final MP mapProvider) {

				Display.getDefault().asyncExec(new Runnable() {
					public void run() {

						if (_mpViewer == null || _mpViewer.getTable().isDisposed()) {
							return;
						}

						_mpViewer.update(mapProvider, null);

						updateUIOfflineInfoTotal();
					}
				});
			}
		};

		MP.addOfflineInfoListener(_offlineJobInfoListener);
	}

//	private String checkBaseUrl(final String baseUrl) {
//
//		if (baseUrl == null || baseUrl.length() == 0) {
//			return Messages.pref_map_validationError_baseUrlIsRequired;
//		} else {
//
//			try {
//				new URL(baseUrl);
//			} catch (final MalformedURLException e) {
//				return Messages.pref_map_validationError_invalidUrl;
//			}
//		}
//
//		return null;
//	}

	private String checkMapProviderId(final String factoryId) {

		String error = null;
		if (factoryId == null || factoryId.length() == 0) {
			error = Messages.pref_map_validationError_factoryIdIsRequired;
		} else {

			// check if the id is already used

			for (final MP mapProvider : _visibleMp) {

				final String otherFactoryId = mapProvider.getId();

				if (_isNewMapProvider) {

					// new map provider: another id with the same name is not allowed

					if (factoryId.equalsIgnoreCase(otherFactoryId)) {
						error = Messages.pref_map_validationError_factoryIdIsAlreadyUsed;
						break;
					}

				} else {

					// check existing map providers

					if (_selectedMapProvider.equals(mapProvider) == false) {

						// check other map providers but not the same which is selected

						if (factoryId.equalsIgnoreCase(otherFactoryId)) {
							error = Messages.pref_map_validationError_factoryIdIsAlreadyUsed;
							break;
						}
					}
				}
			}
		}

		return error;
	}

	private String checkOfflineFolder(final String offlineFolder) {

		String error = null;

		if (offlineFolder == null || offlineFolder.length() == 0) {

			error = Messages.pref_map_validationError_offlineFolderIsRequired;

		} else {

			if (offlineFolder.equalsIgnoreCase(MPProfile.WMS_CUSTOM_TILE_PATH)) {
				return Messages.Pref_Map_ValidationError_OfflineFolderIsUsedInMapProfile;
			}

			// check if the offline folder is already used

			for (final MP mapProvider : _visibleMp) {

				if (_isNewMapProvider) {

					// new map provider: folder with the same name is not allowed

					if (offlineFolder.equalsIgnoreCase(mapProvider.getOfflineFolder())) {
						error = Messages.pref_map_validationError_offlineFolderIsAlreadyUsed;
						break;
					}

				} else {

					// existing map provider

					if (_selectedMapProvider.equals(mapProvider) == false) {

						// check other map providers but not the same which is selected

						if (offlineFolder.equalsIgnoreCase(mapProvider.getOfflineFolder())) {
							error = Messages.pref_map_validationError_offlineFolderIsAlreadyUsed;
							break;
						}
					}
				}
			}
		}

		/*
		 * check if the filename is valid
		 */
		if (error == null) {

			final IPath tileCacheBasePath = getTileCachePath();
			if (tileCacheBasePath != null) {

				try {
					final File tileCacheDir = tileCacheBasePath.addTrailingSeparator().append(offlineFolder).toFile();
					if (tileCacheDir.exists() == false) {

						final boolean isFileCreated = tileCacheDir.createNewFile();

						// name is correct

						if (isFileCreated) {
							// delete folder because the folder is created for checking validity
							tileCacheDir.delete();
						}
					}

				} catch (final Exception ioe) {
					error = Messages.pref_map_validationError_offlineFolderInvalidCharacters;
				}
			}
		}

		return error;
	}

	/**
	 * create id and folder from the map provider name
	 */
	private void createAutoText() {

		if (_isNewMapProvider == false) {
			return;
		}

		if (_isModifiedMapProviderId && _isModifiedOfflineFolder) {
			return;
		}

		final String name = _txtMapProviderName.getText().trim().toLowerCase();
		final String validText = UI.createIdFromName(name, MAX_ID_LENGTH);

		_isDisableModifyListener = true;
		{
			if (_isModifiedMapProviderId == false) {
				_txtMapProviderId.setText(validText);
			}

			if (_isModifiedOfflineFolder == false) {
				_txtOfflineFolder.setText(validText);
			}
		}
		_isDisableModifyListener = false;
	}

	@Override
	protected Control createContents(final Composite parent) {

		_visibleMp = _mpMgr.getAllMapProviders(true);

		addListener();

		initializeDialogUnits(parent);
		final Composite container = createUI(parent);

		// load viewer
		_mpViewer.setInput(new Object());

		restoreState();

		updateUIOfflineInfoTotal();

		_mpViewer.getTable().setFocus();

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(5, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI10MapViewerHeader(container);
			new Label(container, SWT.NONE);

			createUI20MapViewer(container);
			createUI30Buttons(container);

			createUI40ReadTileSize(container);
			new Label(container, SWT.NONE);

			createUI50Details(container);
		}

		// spacer
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 20).applyTo(label);

		return container;
	}

	private void createUI10MapViewerHeader(final Composite parent) {

		final Composite headerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(headerContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(headerContainer);
//			headerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			/*
			 * label: map provider
			 */
			final Label label = new Label(headerContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.Pref_Map_Label_AvaiablelMapProvider);

			/*
			 * button: refresh
			 */
			final ToolBar toolbar = new ToolBar(headerContainer, SWT.FLAT);
			final ToolBarManager tbm = new ToolBarManager(toolbar);

			_actionRefreshNotAssessed = new ActionRefreshOfflineInfoNotAssessed(this);
			_actionRefreshSelected = new ActionRefreshOfflineInfoSelected(this);
			_actionRefreshAll = new ActionRefreshOfflineInfoAll(this);
			_actionCancelRefresh = new ActionCancelRefreshOfflineInfo(this);

			tbm.add(_actionRefreshNotAssessed);
			tbm.add(_actionRefreshSelected);
			tbm.add(_actionRefreshAll);
			tbm.add(_actionCancelRefresh);

			tbm.update(true);
		}
	}

	private void createUI20MapViewer(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults()//
				.hint(400, pixelConverter.convertHeightInCharsToPixels(10))
				.grab(true, true)
				.applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
		table.setLinesVisible(false);

		_mpViewer = new TableViewer(table);
		_mpViewer.setUseHashlookup(true);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tc;

		// column: server type
		tvc = new TableViewerColumn(_mpViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setToolTipText(Messages.Pref_Map_Viewer_Column_Lbl_ServerType_Tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MP mapProvider = (MP) cell.getElement();

				if (mapProvider instanceof MPWms) {
					cell.setText(Messages.Pref_Map_Viewer_Column_ContentServerTypeWms);
				} else if (mapProvider instanceof MPCustom) {
					cell.setText(Messages.Pref_Map_Viewer_Column_ContentServerTypeCustom);
				} else if (mapProvider instanceof MPProfile) {
					cell.setText(Messages.Pref_Map_Viewer_Column_ContentServerTypeMapProfile);
				} else if (mapProvider instanceof MPPlugin) {
					cell.setText(Messages.Pref_Map_Viewer_Column_ContentServerTypePlugin);
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(4)));

		// column: map provider
		tvc = new TableViewerColumn(_mpViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map_Viewer_Column_Lbl_MapProvider);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MP mapProvider = (MP) cell.getElement();

				cell.setText(mapProvider.getName());
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnWeightData(20));

		// column: offline path
		tvc = new TableViewerColumn(_mpViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map_Viewer_Column_Lbl_OfflinePath);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MP mapProvider = (MP) cell.getElement();

				cell.setText(mapProvider.getOfflineFolder());
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnWeightData(10));

		// column: layer
		tvc = new TableViewerColumn(_mpViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map_Viewer_Column_Lbl_Layer);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				String layer = UI.EMPTY_STRING;

				final MP mapProvider = (MP) cell.getElement();
				if (mapProvider instanceof MPWms) {

					final MPWms wmsMapProvider = (MPWms) mapProvider;

					final StringBuilder sb = new StringBuilder();
					sb.append(wmsMapProvider.getAvailableLayers());

					layer = sb.toString();
				}
				cell.setText(layer);
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(10)));

		// column: offline file counter
		tvc = new TableViewerColumn(_mpViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map_Viewer_Column_Lbl_OfflineFileCounter);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int offlineTileCounter = ((MP) cell.getElement()).getOfflineFileCounter();
				if (offlineTileCounter == MP.OFFLINE_INFO_NOT_READ) {
					cell.setText(Messages.pref_map_lable_NA);
				} else if (offlineTileCounter > 0) {
					cell.setText(Integer.toString(offlineTileCounter));
				} else {
					cell.setText(UI.DASH_WITH_SPACE);
				}
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(10)));

		// column: offline file size
		tvc = new TableViewerColumn(_mpViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map_Viewer_Column_Lbl_OfflineFileSize);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final long offlineTileSize = ((MP) cell.getElement()).getOfflineFileSize();
				if (offlineTileSize == MP.OFFLINE_INFO_NOT_READ) {
					cell.setText(Messages.pref_map_lable_NA);
				} else if (offlineTileSize > 0) {
					cell.setText(_nf.format((float) offlineTileSize / 1024 / 1024));
				} else {
					cell.setText(UI.DASH_WITH_SPACE);
				}
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(12)));

		/*
		 * create table viewer
		 */
		_mpViewer.setContentProvider(new MapContentProvider());
		_mpViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {

				final MP mp1 = (MP) e1;
				final MP mp2 = (MP) e2;

				final boolean thisIsPlugin = e1 instanceof MPPlugin;
				final boolean otherIsPlugin = e2 instanceof MPPlugin;

				if (thisIsPlugin && otherIsPlugin) {
					return mp1.getName().compareTo(mp2.getName());
				} else if (thisIsPlugin) {
					return 1;
				} else if (otherIsPlugin) {
					return -1;
				} else {
					return mp1.getName().compareTo(mp2.getName());
				}
			}
		});

		_mpViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectMapProvider(event);
			}
		});

		_mpViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				if (_selectedMapProvider instanceof MPWms) {

					openConfigDialogWms();

				} else if (_selectedMapProvider instanceof MPCustom) {

					openConfigDialogCustom();

				} else if (_selectedMapProvider instanceof MPProfile) {

					openConfigDialogMapProfile();

				} else {

					// select name
					_txtMapProviderName.setFocus();
					_txtMapProviderName.selectAll();
				}
			}
		});
	}

	private void createUI30Buttons(final Composite container) {

		final Composite btnContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(btnContainer);
		GridLayoutFactory.fillDefaults().applyTo(btnContainer);
		{
			// button: edit
			_btnEdit = new Button(btnContainer, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.END, SWT.FILL)
					.applyTo(_btnEdit);
			_btnEdit.setText(Messages.Pref_Map_Button_Edit);
			_btnEdit.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					openConfigDialog();
				}
			});
			setButtonLayoutData(_btnEdit);

			// button: add custom map provider
			_btnAddMapProviderCustom = new Button(btnContainer, SWT.NONE);
			_btnAddMapProviderCustom.setText(Messages.Pref_Map_Button_AddMapProviderCustom);
			_btnAddMapProviderCustom.setToolTipText(Messages.Pref_Map_Button_AddMapProviderCustom_Tooltip);
			_btnAddMapProviderCustom.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					setEmptyMapProviderUI(new MPCustom());
				}
			});
			setButtonLayoutData(_btnAddMapProviderCustom);

			// button: add wms map provider
			_btnAddMapProviderWms = new Button(btnContainer, SWT.NONE);
			_btnAddMapProviderWms.setText(Messages.Pref_Map_Button_AddMapProviderWms);
			_btnAddMapProviderWms.setToolTipText(Messages.Pref_Map_Button_AddMapProviderWms_Tooltip);
			_btnAddMapProviderWms.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onAddWmsMapProvider();
				}
			});
			setButtonLayoutData(_btnAddMapProviderWms);

			// button: add profile map provider
			_btnAddMapProviderMapProfile = new Button(btnContainer, SWT.NONE);
			_btnAddMapProviderMapProfile.setText(Messages.Pref_Map_Button_AddMapProfile);
			_btnAddMapProviderMapProfile.setToolTipText(Messages.Pref_Map_Button_AddMapProfile_Tooltip);
			_btnAddMapProviderMapProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					final MPProfile mapProfile = new MPProfile();
					mapProfile.synchronizeMPWrapper();

					setEmptyMapProviderUI(mapProfile);
				}
			});
			setButtonLayoutData(_btnAddMapProviderMapProfile);

			// wms drag&drop target
			_lblDropTarget = new Label(btnContainer, SWT.BORDER | SWT.WRAP | SWT.CENTER);
			GridDataFactory.fillDefaults().applyTo(_lblDropTarget);
			_lblDropTarget.setText(Messages.Pref_Map_Label_WmsDropTarget);
			_lblDropTarget.setToolTipText(Messages.Pref_Map_Label_WmsDropTarget_Tooltip);
			setWmsDropTarget(_lblDropTarget);

			// button: delete offline map
			_btnDeleteOfflineMap = new Button(btnContainer, SWT.NONE);
			_btnDeleteOfflineMap.setText(Messages.Pref_Map_Button_DeleteOfflineMap);
			_btnDeleteOfflineMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					deleteOfflineMap(_selectedMapProvider);
				}
			});
			setButtonLayoutData(_btnDeleteOfflineMap);

			// button: import
			_btnImport = new Button(btnContainer, SWT.NONE);
			_btnImport.setText(Messages.Pref_Map_Button_ImportMP);
			_btnImport.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onImportMP();
				}
			});
			setButtonLayoutData(_btnImport);
			GridData gd = setButtonLayoutData(_btnImport);
			gd.verticalIndent = 20;

			// button: export
			_btnExport = new Button(btnContainer, SWT.NONE);
			_btnExport.setText(Messages.Pref_Map_Button_ExportMP);
			_btnExport.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onExportMP();
				}
			});
			setButtonLayoutData(_btnExport);

			/*
			 * drop target: map provider
			 */
			_lblMpDropTarget = new Label(btnContainer, SWT.BORDER | SWT.WRAP | SWT.CENTER);
			GridDataFactory.fillDefaults().applyTo(_lblMpDropTarget);
			_lblMpDropTarget.setText(Messages.Pref_Map_Label_MapProviderDropTarget);
			_lblMpDropTarget.setToolTipText(Messages.Pref_Map_Label_MapProviderDropTarget_Tooltip);
			_mpDropTarget = new DropTarget(_lblMpDropTarget, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);

			_mpDropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance(), FileTransfer.getInstance() });

			_mpDropTarget.addDropListener(new DropTargetAdapter() {

				@Override
				public void drop(final DropTargetEvent event) {

					if (event.data == null) {
						event.detail = DND.DROP_NONE;
						return;
					}

					/*
					 * run async to free the mouse cursor from the drop operation
					 */
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							runnableDropMapProvider(event);
						}
					});
				}
			});

			/*
			 * link: map provider support
			 */
			final Link link = new Link(btnContainer, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.END, SWT.FILL)
					.grab(true, false)
					.applyTo(link);
			link.setText(Messages.Pref_Map_Link_MapProvider);
			link.setToolTipText(Messages.Pref_Map_Link_MapProvider_Tooltip);
			link.setEnabled(true);
			link.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					Util.openLink(Display.getCurrent().getActiveShell(), Messages.External_Link_MapProviders);
				}
			});

			/*
			 * button: delete map provider
			 */
			_btnDeleteMapProvider = new Button(btnContainer, SWT.NONE);
			_btnDeleteMapProvider.setText(Messages.Pref_Map_Button_DeleteMapProvider);
			_btnDeleteMapProvider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onDeleteMapProvider();
				}
			});
			gd = setButtonLayoutData(_btnDeleteMapProvider);
			gd.grabExcessVerticalSpace = true;
			gd.verticalAlignment = SWT.END;
			gd.verticalIndent = 20;

		}
	}

	private void createUI40ReadTileSize(final Composite parent) {

		/*
		 * text: offline info
		 */
		_txtOfflineInfoTotal = new Text(parent, SWT.READ_ONLY | SWT.TRAIL);
		GridDataFactory.fillDefaults().applyTo(_txtOfflineInfoTotal);
	}

	private void createUI50Details(final Composite parent) {

		_groupDetails = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).span(2, 1).applyTo(_groupDetails);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupDetails);
		_groupDetails.setText(Messages.Pref_Map_Group_Detail_SelectedMapProvider);
		{
			createUI52DetailsDetails(_groupDetails);
			createUI54DetailsButtons(_groupDetails);
		}
	}

	private void createUI52DetailsDetails(final Group parent) {

		final PixelConverter pixCon = new PixelConverter(parent);

		final VerifyListener verifyListener = new VerifyListener() {
			public void verifyText(final VerifyEvent e) {
				e.text = UI.createIdFromName(e.text, MAX_ID_LENGTH);
			}
		};

		final Composite detailContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(detailContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(detailContainer);
		{
			/*
			 * map provider name
			 */
			Label label = new Label(detailContainer, SWT.NONE);
			label.setText(Messages.Pref_Map_Lable_MapProvider);

			// text: map provider 
			_txtMapProviderName = new Text(detailContainer, SWT.BORDER);
			GridDataFactory.fillDefaults().applyTo(_txtMapProviderName);
			_txtMapProviderName.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					if (_isDisableModifyListener) {
						return;
					}
					createAutoText();
					setMapProviderModified();
				}
			});

			/*
			 * description
			 */
			// label: description
			_lblDescription = new Label(detailContainer, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(_lblDescription);
			_lblDescription.setText(Messages.Pref_Map_Label_Description);

			// text: description
			_txtDescription = new Text(detailContainer, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
			GridDataFactory.fillDefaults()//
					.hint(pixCon.convertWidthInCharsToPixels(20), pixCon.convertHeightInCharsToPixels(5))
					.grab(true, false)
					.applyTo(_txtDescription);
			_txtDescription.addModifyListener(_modifyListener);

			/*
			 * offline folder
			 */
			// label: offline folder
			label = new Label(detailContainer, SWT.NONE);
			label.setText(Messages.Pref_Map_Lable_OfflineFolder);

			_offlineContainer = new Composite(detailContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_offlineContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true).applyTo(_offlineContainer);
			{
				// text: offline folder
				_txtOfflineFolder = new Text(_offlineContainer, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.hint(pixCon.convertWidthInCharsToPixels(20), SWT.DEFAULT)
						.applyTo(_txtOfflineFolder);
				_txtOfflineFolder.setTextLimit(MAX_ID_LENGTH);
				_txtOfflineFolder.addVerifyListener(verifyListener);

				_txtOfflineFolder.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {

						if (_isDisableModifyListener) {
							return;
						}

						_isModifiedOfflineFolder = true;
						setMapProviderModified();

						// force offline folder to be reloaded
						_selectedMapProvider.setStateToReloadOfflineCounter();
					}
				});

				// label: offline info
				_lblOfflineFolderInfo = new Label(_offlineContainer, SWT.TRAIL);
				GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.CENTER).applyTo(
						_lblOfflineFolderInfo);
			}

			/*
			 * map provider id
			 */
			label = new Label(detailContainer, SWT.NONE);
			label.setText(Messages.Pref_Map_Lable_MapProviderId);

			// text: map provider id
			_txtMapProviderId = new Text(detailContainer, SWT.BORDER);
			GridDataFactory.fillDefaults().applyTo(_txtMapProviderId);
			_txtMapProviderId.setTextLimit(MAX_ID_LENGTH);
			_txtMapProviderId.addVerifyListener(verifyListener);

			_txtMapProviderId.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					if (_isDisableModifyListener) {
						return;
					}

					_isModifiedMapProviderId = true;
					setMapProviderModified();
				}
			});

			/*
			 * map provider type
			 */
			label = new Label(detailContainer, SWT.NONE);
			label.setText(Messages.Pref_Map_Lable_MapProviderType);

			// text: map provider type
			_txtMapProviderType = new Text(detailContainer, SWT.BORDER | SWT.READ_ONLY);
			GridDataFactory.fillDefaults().applyTo(_txtMapProviderType);

			/*
			 * url
			 */
			label = new Label(detailContainer, SWT.NONE);
			label.setText(Messages.Pref_Map_Label_Url);

			// text: url
			_txtUrl = new Text(detailContainer, SWT.BORDER | SWT.READ_ONLY);
			GridDataFactory.fillDefaults().hint(200, SWT.DEFAULT).applyTo(_txtUrl);
		}
	}

	private void createUI54DetailsButtons(final Group parent) {

		final Composite btnContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(10, 0).applyTo(btnContainer);
		GridLayoutFactory.fillDefaults().applyTo(btnContainer);
		{
			// button: update
			_btnUpdate = new Button(btnContainer, SWT.NONE);
			_btnUpdate.setText(Messages.Pref_Map_Button_UpdateMapProvider);
			_btnUpdate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onUpdateMapProvider();
				}
			});
			setButtonLayoutData(_btnUpdate);

			// button: cancel
			_btnCancel = new Button(btnContainer, SWT.NONE);
			_btnCancel.setText(Messages.Pref_Map_Button_CancelMapProvider);
			_btnCancel.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onCancelMapProvider();
				}
			});
			setButtonLayoutData(_btnCancel);
		}
	}

	/**
	 * Creates a {@link MPWms} from the capabilities url
	 * 
	 * @param capsUrl
	 * @throws Exception
	 */
	private void createWmsMapProvider(final String capsUrl) throws Exception {

		// show loading... message
		setMessage(NLS.bind(Messages.pref_map_message_loadingWmsCapabilities, capsUrl), INFORMATION);

		// force message to be displayed
		final Shell shell = getShell();
		shell.redraw();
		shell.update();

		final MPWms wmsMapProvider = MapProviderManager.checkWms(null, capsUrl);
		if (wmsMapProvider == null) {

			// an error occured, exception is already displayed

			// hide loading message
			setMessage(null);

			return;
		}

		/*
		 * get data from the wms
		 */
		final WMSCapabilities wmsCaps = wmsMapProvider.getWmsCaps();
		final Service service = wmsCaps.getService();
		final WMSRequest requests = wmsCaps.getRequest();

		String providerName = service.getTitle();
		String wmsAbstract = service.get_abstract();

		if (providerName == null) {
			providerName = UI.EMPTY_STRING;
		}

		if (wmsAbstract == null) {
			wmsAbstract = UI.EMPTY_STRING;
		}

		// get map request url
		String getMapUrlText = null;
		if (requests != null) {
			final OperationType getMapRequest = requests.getGetMap();
			if (getMapRequest != null) {
				final URL getMapRequestUrl = getMapRequest.getGet();
				getMapUrlText = getMapRequestUrl.toString();
			}
		}

		final String uniqueId = UI.createIdFromName(providerName, MAX_ID_LENGTH);
		final String getMapUrl = getMapUrlText == null ? UI.EMPTY_STRING : getMapUrlText;

		// create an empty map provider in the UI
		setEmptyMapProviderUI(wmsMapProvider);

		// validate wms data
		final Control errorControl = validateMapProvider(providerName, uniqueId, uniqueId);

		if (errorControl == null) {

			/*
			 * data are valid
			 */

			_isNewMapProvider = false;

			_isModifiedMapProvider = false;
			_isModifiedMapProviderId = false;
			_isModifiedOfflineFolder = false;

			// update model
			wmsMapProvider.setId(uniqueId);
			wmsMapProvider.setName(providerName);
			wmsMapProvider.setDescription(wmsAbstract);
			wmsMapProvider.setOfflineFolder(uniqueId);

			wmsMapProvider.setGetMapUrl(getMapUrl);
			wmsMapProvider.setCapabilitiesUrl(capsUrl);

			_mpMgr.addMapProvider(wmsMapProvider);

			_visibleMp.add(wmsMapProvider);
			_isModifiedMapProviderList = true;

			// update viewer
			_mpViewer.add(wmsMapProvider);

			// select map provider in the viewer this will display the wms server in the UI
			_mpViewer.setSelection(new StructuredSelection(wmsMapProvider), true);

			_mpViewer.getTable().setFocus();

		} else {

			/*
			 * data validation displays an error message, show data in the UI but do not create a
			 * map provider, this simulates the situation when the user presses the new button and
			 * enters the values
			 */

			_isNewMapProvider = true;
			_newMapProvider = wmsMapProvider;

			_isModifiedMapProvider = true;
			_isModifiedMapProviderId = false;
			_isModifiedOfflineFolder = false;

			_isDisableModifyListener = true;
			{
				/*
				 * set map provider fields
				 */

				_txtMapProviderName.setText(providerName);
				_txtMapProviderId.setText(uniqueId);
				_txtOfflineFolder.setText(uniqueId);
				_txtDescription.setText(wmsAbstract);
				_txtUrl.setText(capsUrl);

				_lblOfflineFolderInfo.setText(UI.EMPTY_STRING);
				_offlineContainer.layout(true);
			}
			_isDisableModifyListener = false;

			enableControls();

			errorControl.setFocus();
			if (errorControl instanceof Text) {
				((Text) errorControl).selectAll();
			}
		}
	}

	/**
	 * !!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!<br>
	 * <br>
	 * Deletes all files and subdirectories. If a deletion fails, the method stops attempting to
	 * delete and returns false. <br>
	 * <br>
	 * !!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!
	 * 
	 * @param directory
	 * @return Returns <code>true</code> if all deletions were successful
	 */
	private boolean deleteDir(final File directory) {

		if (directory.isDirectory()) {

			final String[] children = directory.list();

			for (int i = 0; i < children.length; i++) {
				final boolean success = deleteDir(new File(directory, children[i]));
				if (success == false) {
					_isDeleteError = true;
				}
			}
		}

		// The directory is now empty so delete it
		return directory.delete();
	}

	private void deleteFile(final String filePath) {

		final File file = new File(filePath);

		if (file.exists()) {
			file.delete();
		}
	}

	public void deleteOfflineMap(final MP mapProvider) {

		deleteOfflineMapFiles(mapProvider);

		// disable delete offline button
		enableControls();

		// reselect map provider, set focus to map provider
		_mpViewer.setSelection(_mpViewer.getSelection());
		_mpViewer.getTable().setFocus();
	}

	private void deleteOfflineMapFiles(final MP mp) {

		// reset state that offline images are available
		mp.resetTileImageAvailability();

		// check base path
		IPath tileCacheBasePath = getTileCachePath();
		if (tileCacheBasePath == null) {
			return;
		}

		// check map provider offline folder
		final String tileOSFolder = mp.getOfflineFolder();
		if (tileOSFolder == null) {
			return;
		}

		tileCacheBasePath = tileCacheBasePath.addTrailingSeparator();

		boolean isUpdateUI = false;

		// delete map provider files
		final File tileCacheDir = tileCacheBasePath.append(tileOSFolder).toFile();
		if (tileCacheDir.exists()) {
			isUpdateUI = true;
			deleteOfflineMapFilesDir(tileCacheDir);
		}

		// delete profile wms files
		final File wmsPath = tileCacheBasePath.append(MPProfile.WMS_CUSTOM_TILE_PATH).append(tileOSFolder).toFile();
		if (wmsPath.exists()) {
			isUpdateUI = true;
			deleteOfflineMapFilesDir(wmsPath);
		}

		if (isUpdateUI) {

			mp.setStateToReloadOfflineCounter();

			// update viewer
			_mpViewer.update(mp, null);

			updateUIOfflineInfoTotal();

			// clear map image cache
			mp.disposeTileImages();
		}
	}

	private void deleteOfflineMapFilesDir(final File tileCacheDir) {

		_isDeleteError = false;

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				deleteDir(tileCacheDir);
			}
		});

		if (_isDeleteError) {
			StatusUtil.log(NLS.bind(Messages.pref_map_error_deleteTiles_message, tileCacheDir), new Exception());
		}
	}

	@Override
	public void dispose() {

		stopJobOfflineInfo();

		if (_wmsDropTarget != null) {
			_wmsDropTarget.dispose();
		}
		if (_mpDropTarget != null) {
			_mpDropTarget.dispose();
		}

		MP.removeOfflineInfoListener(_offlineJobInfoListener);

		super.dispose();
	}

	private void doImportMP(final String importFilePath) {

		// create map provider from xml file
		final ArrayList<MP> importedMPs = MapProviderManager.getInstance().importMapProvider(importFilePath);

		// select imported map provider
		if (importedMPs != null) {

			_isForceUpdateMapProviderList = true;

			// update model
			_visibleMp.addAll(importedMPs);

			// update viewer
			_mpViewer.add(importedMPs.toArray(new MP[importedMPs.size()]));

			// select map provider in the viewer
			_mpViewer.setSelection(new StructuredSelection(importedMPs.get(0)), true);
		}

		_mpViewer.getTable().setFocus();

		if (importedMPs != null) {
			// show the imported map provider in the config dialog
			openConfigDialog();
		}
	}

	private void enableControls() {

		// focus get's lost when a map provider is modified the first time
		final Control focusControl = Display.getCurrent().getFocusControl();

		/*
		 * validate UI data
		 */
		validateMapProvider(_txtMapProviderName.getText().trim(), _txtMapProviderId.getText().trim(), _txtOfflineFolder
				.getText()
				.trim());

		final boolean isMapProvider = _selectedMapProvider != null;

		final boolean isExistingMapProvider = isMapProvider
				&& _isNewMapProvider == false
				&& _isModifiedMapProvider == false;

		final boolean isOfflineJobStopped = _isOfflineJobRunning == false;

		final boolean canDeleteOfflineMap = isMapProvider
				&& _selectedMapProvider.getOfflineFileCounter() > 0
				&& isOfflineJobStopped;

		final boolean isOfflinePath = getTileCachePath() != null;

		final boolean isCustomMapProvider = _selectedMapProvider instanceof MPCustom;
		final boolean isMapProfile = _selectedMapProvider instanceof MPProfile;
		final boolean isWmsMapProvider = _selectedMapProvider instanceof MPWms;

		final boolean isNonePluginMapProvider = isCustomMapProvider || isWmsMapProvider || isMapProfile;
		final boolean canEditFields = _isNewMapProvider || isNonePluginMapProvider;

		_mpViewer.getTable().setEnabled(isExistingMapProvider);

		_txtMapProviderName.setEnabled(canEditFields);
		_txtMapProviderId.setEnabled(canEditFields);
		_txtOfflineFolder.setEnabled(canEditFields);
		_txtDescription.setEnabled(canEditFields);

		// map provider list actions
		_btnAddMapProviderCustom.setEnabled(isExistingMapProvider);
		_btnAddMapProviderWms.setEnabled(isExistingMapProvider);
		_btnAddMapProviderMapProfile.setEnabled(isExistingMapProvider);
		_btnEdit.setEnabled(isExistingMapProvider && isNonePluginMapProvider);
		_btnDeleteMapProvider.setEnabled(isExistingMapProvider && isNonePluginMapProvider);
		_btnDeleteOfflineMap.setEnabled(isExistingMapProvider && canDeleteOfflineMap);
		_btnImport.setEnabled(isExistingMapProvider);
		_btnExport.setEnabled(isExistingMapProvider & isNonePluginMapProvider);

		_lblDropTarget.setEnabled(_isModifiedMapProvider == false && _isNewMapProvider == false);
		_lblMpDropTarget.setEnabled(_isModifiedMapProvider == false && _isNewMapProvider == false);

		// map provider detail actions
		_btnUpdate.setEnabled(_isValid && _isModifiedMapProvider);
		_btnCancel.setEnabled(_isNewMapProvider || _isModifiedMapProvider);

		_actionRefreshAll.setEnabled(isOfflinePath && isOfflineJobStopped);
		_actionRefreshSelected.setEnabled(isOfflinePath && isOfflineJobStopped);
		_actionRefreshNotAssessed.setEnabled(isOfflinePath && isOfflineJobStopped);
		_actionCancelRefresh.setEnabled(isOfflinePath && !isOfflineJobStopped);

		if (_isNewMapProvider) {
			_groupDetails.setText(Messages.Pref_Map_Group_Detail_NewMapProvider);
		} else {
			if (_isModifiedMapProvider) {
				_groupDetails.setText(Messages.Pref_Map_Group_Detail_ModifiedMapProvider);
			} else {
				_groupDetails.setText(Messages.Pref_Map_Group_Detail_SelectedMapProvider);
			}
		}

		if (focusControl != null) {
			focusControl.setFocus();
		}
	}

	/**
	 * !!!!! Recursive funktion to count files/size !!!!!
	 * 
	 * @param listOfFiles
	 */
	private void getFilesInfo(final File[] listOfFiles) {

		if (_isOfflineJobCanceled) {
			return;
		}

		// update UI
		if (_offlineJobFileCounter > _offlineJobFileCounterUIUpdate + 1000) {
			_offlineJobFileCounterUIUpdate = _offlineJobFileCounter;
			updateUIOfflineInfo();
		}

		for (int fileIndex = 0; fileIndex < listOfFiles.length; fileIndex++) {
			final File file = listOfFiles[fileIndex];
			if (file.isFile()) {

				// file

				_offlineJobFileCounter++;
				_offlineJobFileSize += file.length();

			} else if (file.isDirectory()) {

				// directory

				getFilesInfo(file.listFiles());

				if (_isOfflineJobCanceled) {
					return;
				}
			}
		}
	}

	/**
	 * @return Returns the next map provider or <code>null</code> when there is no WMS map
	 *         provider
	 */
	public MapProviderNavigator getNextMapProvider() {

		MPWms nextMapProvider = null;

		final Table table = _mpViewer.getTable();
		final int selectionIndex = table.getSelectionIndex();
		final int itemCount = table.getItemCount();
		int isNextNext = -1;

		for (int itemIndex = selectionIndex + 1; itemIndex < itemCount; itemIndex++) {

			final Object mapProvider = _mpViewer.getElementAt(itemIndex);
			if (mapProvider instanceof MPWms) {

				final MPWms wmsMapProvider = (MPWms) mapProvider;
				if (wmsMapProvider.isWmsAvailable()) {

					if (nextMapProvider == null) {

						if (MapProviderManager.checkWms(wmsMapProvider, null) == null) {
							continue;
						}

						nextMapProvider = wmsMapProvider;

						continue;
					}

					if (isNextNext == -1) {
						isNextNext = 1;
						break;
					}
				}
			}
		}

		if (nextMapProvider == null) {
			return null;
		}

		// select next map provider
		_mpViewer.setSelection(new StructuredSelection(nextMapProvider));

		// set focus to selected item
		table.setSelection(table.getSelectionIndex());

		return new MapProviderNavigator(nextMapProvider, isNextNext == 1);
	}

	/**
	 * @return Returns the previous map provider or <code>null</code> when there is no WMS map
	 *         provider
	 */
	public MapProviderNavigator getPreviousMapProvider() {

		MPWms prevMapProvider = null;

		final Table table = _mpViewer.getTable();
		final int selectionIndex = table.getSelectionIndex();
		int isNextNext = -1;

		for (int itemIndex = selectionIndex - 1; itemIndex > -1; itemIndex--) {

			final Object tableItem = _mpViewer.getElementAt(itemIndex);
			if (tableItem instanceof MPWms) {

				final MPWms wmsMapProvider = (MPWms) tableItem;
				if (wmsMapProvider.isWmsAvailable()) {

					if (prevMapProvider == null) {

						if (MapProviderManager.checkWms(wmsMapProvider, null) == null) {
							continue;
						}

						prevMapProvider = wmsMapProvider;
						continue;
					}

					if (isNextNext == -1) {
						isNextNext = 1;
						break;
					}
				}
			}
		}

		if (prevMapProvider == null) {
			return null;
		}

		// select prev map provider
		_mpViewer.setSelection(new StructuredSelection(prevMapProvider));

		// set focus to selected item
		table.setSelection(table.getSelectionIndex());

		return new MapProviderNavigator(prevMapProvider, isNextNext == 1);
	}

	/**
	 * @return Returns file path for the offline maps or <code>null</code> when offline is not used
	 *         or the path is not valid
	 */
	private IPath getTileCachePath() {

		// get status if the tile is offline cache is activated
		final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
		final boolean useOffLineCache = prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE);

		if (useOffLineCache == false) {
			return null;
		}

		if (useOffLineCache) {

			// check tile cache path
			String workingDirectory;

			final boolean useDefaultLocation = prefStore
					.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_DEFAULT_LOCATION);
			if (useDefaultLocation) {
				workingDirectory = Platform.getInstanceLocation().getURL().getPath();
			} else {
				workingDirectory = prefStore.getString(IMappingPreferences.OFFLINE_CACHE_PATH);
			}

			if (new File(workingDirectory).exists() == false) {
				System.err.println("working directory is not available: " + workingDirectory); //$NON-NLS-1$
				return null;
			}

			// append a unique path so that deleting tiles is not doing it in the wrong directory
			final IPath tileCachePath = new Path(workingDirectory).append(TileImageCache.TILE_OFFLINE_CACHE_OS_PATH);
			if (tileCachePath.toFile().exists() == false) {
				return null;
			}

			return tileCachePath;
		}

		return null;
	}

	public void init(final IWorkbench workbench) {}

	@Override
	public boolean isValid() {
		return _isValid;
	}

	private boolean isXmlFile(final String importFilePath) {

		if (importFilePath.toLowerCase().endsWith(XML_EXTENSION)) {
			return true;
		}

		MessageDialog.openError(
				Display.getDefault().getActiveShell(),
				Messages.Pref_Map_Error_Dialog_DragDropError_Title,
				NLS.bind(Messages.Pref_Map_Error_Dialog_DragDropError_Message, importFilePath));

		return false;
	}

	private boolean loadFile(final String address, final String localFilePathName) {

		OutputStream outputStream = null;
		InputStream inputStream = null;

		try {

			final URL url = new URL(address);
			outputStream = new BufferedOutputStream(new FileOutputStream(localFilePathName));

			final URLConnection urlConnection = url.openConnection();
			inputStream = urlConnection.getInputStream();

			final byte[] buffer = new byte[1024];
			int numRead;

			while ((numRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, numRead);
			}

			return true;

		} catch (final UnknownHostException e) {
			StatusUtil.showStatus(e.getMessage(), e);
		} catch (final SocketTimeoutException e) {
			StatusUtil.showStatus(e.getMessage(), e);
		} catch (final Exception e) {
			StatusUtil.showStatus(e.getMessage(), e);
		} finally {

			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (final IOException e) {
				StatusUtil.log(e);
			}
		}

		return false;
	}

	@Override
	public boolean okToLeave() {

		saveMapProviders(false);
		saveState();

		return super.okToLeave();
	}

	private void onAddWmsMapProvider() {

		final IInputValidator inputValidator = new IInputValidator() {
			public String isValid(final String newText) {
				try {
					// check url
					new URL(newText);
				} catch (final MalformedURLException e) {
					return Messages.Wms_Error_InvalidUrl;
				}

				return null;
			}
		};

		// get the reference tour name
		final InputDialog dialog = new InputDialog(
				Display.getCurrent().getActiveShell(),
				Messages.Pref_Map_Dialog_WmsInput_Title,
				Messages.Pref_Map_Dialog_WmsInput_Message,
				UI.EMPTY_STRING,
				inputValidator);

		if (dialog.open() != Window.OK) {
			return;
		}

		try {
			createWmsMapProvider(dialog.getValue());
		} catch (final Exception e) {
			StatusUtil.showStatus(e.getMessage(), e);
		}
	}

	/**
	 * modify selected map provider or new map provider is canceled
	 */
	private void onCancelMapProvider() {

		_isNewMapProvider = false;
		_isModifiedMapProvider = false;
		_isModifiedMapProviderId = false;
		_isModifiedOfflineFolder = false;

		setErrorMessage(null);

		// reselect map provider
		_mpViewer.setSelection(_mpViewer.getSelection());

		enableControls();

		_mpViewer.getTable().setFocus();
	}

	private void onDeleteMapProvider() {

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.pref_map_dlg_confirmDeleteMapProvider_title,
				NLS.bind(Messages.pref_map_dlg_confirmDeleteMapProvider_message, _selectedMapProvider.getName())) == false) {
			return;
		}

		// get map provider which will be selected when the current will be removed 
		final int selectionIndex = _mpViewer.getTable().getSelectionIndex();
		Object nextSelectedMapProvider = _mpViewer.getElementAt(selectionIndex + 1);
		if (nextSelectedMapProvider == null) {
			nextSelectedMapProvider = _mpViewer.getElementAt(selectionIndex - 1);
		}

		// delete offline files
		deleteOfflineMapFiles(_selectedMapProvider);

		// remove from viewer
		_mpViewer.remove(_selectedMapProvider);

		// remove from model
		_mpMgr.remove(_selectedMapProvider);
		_visibleMp.remove(_selectedMapProvider);

		// select another map provider at the same position
		if (nextSelectedMapProvider != null) {
			_mpViewer.setSelection(new StructuredSelection(nextSelectedMapProvider));
			_mpViewer.getTable().setFocus();
		}

		// custom map provider list must be updated
		_isForceUpdateMapProviderList = true;
	}

	private void onExportMP() {

		final FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setText(Messages.Pref_Map_Dialog_Export_Title);

		dialog.setFilterPath(_prefStore.getString(EXPORT_FILE_PATH));

		dialog.setFilterExtensions(new String[] { "*.*", "xml" });//$NON-NLS-1$ //$NON-NLS-2$
		dialog.setFilterNames(new String[] {
				Messages.PrefPageMapProviders_Pref_Map_FileDialog_AllFiles,
				Messages.PrefPageMapProviders_Pref_Map_FileDialog_XmlFiles });

		final DateTime today = new DateTime();

		// add leading 0 when necessary
		final String month = CHARACTER_0 + Integer.toString(today.getMonthOfYear());
		final String day = CHARACTER_0 + Integer.toString(today.getDayOfMonth());

		final String currentDate = //
		UI.DASH
				+ Integer.toString(today.getYear())
				+ UI.DASH
				+ month.substring(month.length() - 2, month.length())
				+ UI.DASH
				+ day.substring(day.length() - 2, day.length());

		dialog.setFileName(_selectedMapProvider.getId() + currentDate + XML_EXTENSION);//$NON-NLS-1$

		final String selectedFilePath = dialog.open();
		if (selectedFilePath == null) {
			// dialog is canceled
			return;
		}

		final File exportFilePath = new Path(selectedFilePath).toFile();

		// keep path
		_prefStore.setValue(EXPORT_FILE_PATH, exportFilePath.getPath());

		if (exportFilePath.exists()) {
			if (UI.confirmOverwrite(exportFilePath) == false) {
				// don't overwrite file, nothing more to do
				return;
			}
		}

		MapProviderManager.getInstance().exportMapProvider(_selectedMapProvider, exportFilePath);

		_mpViewer.getTable().setFocus();
	}

	private void onImportMP() {

		final FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);

		dialog.setText(Messages.Pref_Map_Dialog_Import_Title);
		dialog.setFilterPath(_prefStore.getString(IMPORT_FILE_PATH));

		dialog.setFilterExtensions(new String[] { "*.*", "xml" });//$NON-NLS-1$ //$NON-NLS-2$
		dialog.setFilterNames(new String[] {
				Messages.PrefPageMapProviders_Pref_Map_FileDialog_AllFiles,
				Messages.PrefPageMapProviders_Pref_Map_FileDialog_XmlFiles });

		dialog.setFileName(_selectedMapProvider.getId() + XML_EXTENSION);//$NON-NLS-1$

		final String selectedFilePath = dialog.open();

		if (selectedFilePath == null) {
			// dialog is canceled
			return;
		}

		// keep path
		_prefStore.setValue(IMPORT_FILE_PATH, selectedFilePath);

		doImportMP(selectedFilePath);
	}

	private void onSelectMapProvider(final SelectionChangedEvent event) {

		final Object firstElement = ((StructuredSelection) event.getSelection()).getFirstElement();
		if (firstElement instanceof MP) {

			final MP mapProvider = (MP) firstElement;

			_selectedMapProvider = mapProvider;

			_isDisableModifyListener = true;
			{

				// update UI
				_txtMapProviderName.setText(mapProvider.getName());
				_txtMapProviderId.setText(mapProvider.getId());
				_txtDescription.setText(mapProvider.getDescription());

				// offline folder
				final String tileOSFolder = mapProvider.getOfflineFolder();
				if (tileOSFolder == null) {
					_txtOfflineFolder.setText(UI.EMPTY_STRING);
				} else {
					_txtOfflineFolder.setText(tileOSFolder);
				}

				if (mapProvider instanceof MPWms) {

					// wms map provider

					final MPWms wmsMapProvider = (MPWms) mapProvider;

					_txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Wms);
					_txtUrl.setText(wmsMapProvider.getCapabilitiesUrl());

				} else if (mapProvider instanceof MPCustom) {

					// custom map provider

					final MPCustom customMapProvider = (MPCustom) mapProvider;

					_txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Custom);
					_txtUrl.setText(customMapProvider.getCustomUrl());

				} else if (mapProvider instanceof MPProfile) {

					// map profile

					_txtMapProviderType.setText(Messages.Pref_Map_ProviderType_MapProfile);
					_txtUrl.setText(UI.EMPTY_STRING);

				} else if (mapProvider instanceof MPPlugin) {

					// plugin map provider

					final MPPlugin pluginMapProvider = (MPPlugin) mapProvider;
					final String baseURL = pluginMapProvider.getBaseURL();

					_txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Plugin);
					_txtUrl.setText(baseURL == null ? UI.EMPTY_STRING : baseURL);
				}

				updateUIOfflineInfoDetail(mapProvider);
			}
			_isDisableModifyListener = false;

			enableControls();
		}
	}

	private void onUpdateMapProvider() {
		updateModelFromUI();
		enableControls();
	}

	private void openConfigDialog() {

		if (_isModifiedMapProvider) {
			// update is necessary when the map provider is modified
			if (updateModelFromUI() == false) {
				return;
			}
		}

		if (_selectedMapProvider instanceof MPWms) {

			openConfigDialogWms();

		} else if (_selectedMapProvider instanceof MPCustom) {

			openConfigDialogCustom();

		} else if (_selectedMapProvider instanceof MPProfile) {

			openConfigDialogMapProfile();
		}

		// set focus back to table
		_mpViewer.getTable().setFocus();
	}

	private void openConfigDialogCustom() {

		try {

			// clone mapprovider
			final MPCustom dialogMapProvider = (MPCustom) ((MPCustom) _selectedMapProvider).clone();

			// map images are likely to be downloaded
			dialogMapProvider.setStateToReloadOfflineCounter();

			final DialogMPCustom dialog = new DialogMPCustom(
					Display.getCurrent().getActiveShell(),
					this,
					dialogMapProvider);

			if (dialog.open() == Window.OK) {

				_isModifiedMapProvider = true;

				_selectedMapProvider = dialogMapProvider;

				// update model's
				MapProviderManager.replaceMapProvider(dialogMapProvider);
				_visibleMp = _mpMgr.getAllMapProviders(true);

				updateModelFromUI();
			}

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}
	}

	private void openConfigDialogMapProfile() {

		try {

			final MPProfile mpProfile = (MPProfile) _selectedMapProvider;

			/*
			 * update map providers in the profile to reflect renaming, adding/deleting of map
			 * providers
			 */
			mpProfile.synchronizeMPWrapper();

			// clone mapprovider
			final MPProfile dialogMapProfile = (MPProfile) mpProfile.clone();

			// it is likely that map images will be downloaded
			dialogMapProfile.setStateToReloadOfflineCounter();

			final DialogMPProfile dialog = new DialogMPProfile(
					Display.getCurrent().getActiveShell(),
					this,
					dialogMapProfile);

			if (dialog.open() == Window.OK) {

				_isModifiedMapProvider = true;

				_selectedMapProvider = dialogMapProfile;

				// delete profile offline images, not the child images
				deleteOfflineMapFiles(dialogMapProfile);

				// update model's
				MapProviderManager.replaceMapProvider(dialogMapProfile);
				_visibleMp = _mpMgr.getAllMapProviders(true);

				updateModelFromUI();
			}

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}
	}

	private void openConfigDialogWms() {

		try {

			final MPWms wmsMapProvider = (MPWms) _selectedMapProvider;

			// load WMS caps
			if (MapProviderManager.checkWms(wmsMapProvider, null) == null) {
				return;
			}

			// enable all wms map providers that they can be selected with next/previous
			for (final MP mapProvider : _visibleMp) {

				if (mapProvider instanceof MPWms) {
					((MPWms) mapProvider).setWmsEnabled(true);
				}

				if (mapProvider instanceof MPProfile) {

					final MPProfile mpProfile = (MPProfile) mapProvider;

					for (final MPWrapper mpWrapper : mpProfile.getAllWrappers()) {
						if (mpWrapper.getMP() instanceof MPWms) {
							((MPWms) mpWrapper.getMP()).setWmsEnabled(true);
						}
					}
				}
			}

			// clone mapprovider
			final MPWms dialogMapProvider = (MPWms) wmsMapProvider.clone();

			final DialogMP dialog = new DialogMPWms(Display.getCurrent().getActiveShell(), this, dialogMapProvider);

			if (dialog.open() == Window.OK) {

				_isModifiedMapProvider = true;

				_selectedMapProvider = dialogMapProvider;

				// update model
				MapProviderManager.replaceMapProvider(dialogMapProvider);
				_visibleMp = _mpMgr.getAllMapProviders(true);

				updateModelFromUI();
			}

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}
	}

	@Override
	public boolean performCancel() {

		if (_isForceUpdateMapProviderList == false) {

			/*
			 * check if the map provider list is modified and ask the user to save it
			 */
			if (_isModifiedMapProviderList || _isModifiedMapProvider) {
				if (MessageDialogNoClose.openConfirm(
						Display.getCurrent().getActiveShell(),
						Messages.pref_map_dlg_cancelModifiedMapProvider_title,
						Messages.pref_map_dlg_cancelModifiedMapProvider_message)) {

					if (_isModifiedMapProvider) {

						// current map provider is modified

						if (updateModelFromUI() == false) {
							return false;
						}
					}

					_isForceUpdateMapProviderList = true;
				}
			}
		}

		if (_isForceUpdateMapProviderList) {
			saveMapProviders(true);
		}

		saveState();

		return super.performCancel();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {

			final boolean wasModified = _isModifiedMapProvider;

			if (updateModelFromUI() == false) {
				/*
				 * this case should not happen because the OK button is disabled when the data are
				 * invalid
				 */
				return false;
			}

			saveMapProviders(true);

			if (wasModified) {
				/*
				 * map providers are saved, keep dialog open because this situation happened several
				 * times during development of this part
				 */
				return false;
			}

			saveState();
		}

		return isOK;
	}

	private void restoreState() {

		/*
		 * offline info
		 */
		final boolean isReadTileInfo = _prefStore.getBoolean(IMappingPreferences.MAP_FACTORY_IS_READ_TILE_SIZE);
//		fChkReadTileSize.setSelection(isReadTileInfo);

		if (isReadTileInfo) {
			startJobOfflineInfo(null);
		}

		/*
		 * select last selected map provider
		 */
		final String lastMapProviderId = _prefStore
				.getString(IMappingPreferences.MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER);
		MP lastMapProvider = null;
		for (final MP mapProvider : _visibleMp) {
			if (mapProvider.getId().equals(lastMapProviderId)) {
				lastMapProvider = mapProvider;
				break;
			}
		}
		if (lastMapProvider == null) {
			_mpViewer.setSelection(new StructuredSelection(_visibleMp.get(0)));
		} else {
			_mpViewer.setSelection(new StructuredSelection(lastMapProvider));
		}

		// set focus to selected map provider
		final Table table = _mpViewer.getTable();
		table.setSelection(table.getSelectionIndex());
	}

	private void runnableDropMapProvider(final DropTargetEvent event) {

		final TransferData transferDataType = event.currentDataType;

		if (FileTransfer.getInstance().isSupportedType(transferDataType)) {

			Assert.isTrue(event.data instanceof String[]);

			final String[] paths = (String[]) event.data;
			Assert.isTrue(paths.length > 0);

			final String importFilePath = paths[0];

			if (isXmlFile(importFilePath)) {
				doImportMP(importFilePath);
			}

		} else if (URLTransfer.getInstance().isSupportedType(transferDataType)) {

			final String url = (String) event.data;

			if (isXmlFile(url) == false) {
				return;
			}

			// create temp file name
			final IPath stateLocation = Platform.getStateLocation(Activator.getDefault().getBundle());
			final String tempFilePath = stateLocation
					.append(Long.toString(System.nanoTime()) + XML_EXTENSION)
					.toOSString();

			// load file from internet
			if (loadFile(url, tempFilePath) == false) {
				deleteFile(tempFilePath);
				return;
			}

			doImportMP(tempFilePath);

			// delete temp file
			deleteFile(tempFilePath);
		}
	}

	private boolean saveMapProviders(final boolean isForceSave) {

		boolean isSaveMapProvider = false;
		boolean isSaveOtherMapProviders = false;
		boolean isSaveNeeded = false;

		if (isForceSave) {
			// check if save is needed
			isSaveNeeded = _isModifiedMapProvider || _isModifiedMapProviderList;
		} else {

			if (_isModifiedMapProvider) {

				isSaveMapProvider = MessageDialogNoClose.openQuestion(
						Display.getCurrent().getActiveShell(),
						Messages.pref_map_dlg_saveModifiedMapProvider_title,
						Messages.pref_map_dlg_saveModifiedMapProvider_message);

				if (isSaveMapProvider) {
					// ignore errors, errors should not happen
					updateModelFromUI();
				}
			}

			if (_isModifiedMapProviderList && isSaveMapProvider == false) {

				isSaveOtherMapProviders = MessageDialogNoClose.openQuestion(
						Display.getCurrent().getActiveShell(),
						Messages.pref_map_dlg_saveModifiedMapProvider_title,
						Messages.pref_map_dlg_saveOtherMapProvider_message);
			}
		}

		if (_isForceUpdateMapProviderList || isSaveNeeded || isSaveMapProvider || isSaveOtherMapProviders) {

			MapProviderManager.getInstance().writeMapProviderXml();

			_isModifiedMapProviderList = false;
			_isForceUpdateMapProviderList = false;

			return true;
		}

		return false;
	}

	private void saveState() {

		// offline info
//		fPrefStore.setValue(IMappingPreferences.MAP_FACTORY_IS_READ_TILE_SIZE, fChkReadTileSize.getSelection());

		// selected map provider
		if (_selectedMapProvider != null) {
			_prefStore.setValue(//
					IMappingPreferences.MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER,
					_selectedMapProvider.getId());
		}
	}

	private void setEmptyMapProviderUI(final MP mapProvider) {

		_isNewMapProvider = true;
		_newMapProvider = mapProvider;

		_isDisableModifyListener = true;
		{
			/*
			 * set map provider fields empty
			 */
			_txtMapProviderName.setText(UI.EMPTY_STRING);
			_txtMapProviderId.setText(UI.EMPTY_STRING);
			_txtOfflineFolder.setText(UI.EMPTY_STRING);
			_txtDescription.setText(UI.EMPTY_STRING);
			_txtUrl.setText(UI.EMPTY_STRING);

			// map provider type
			if (mapProvider instanceof MPCustom) {
				_txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Custom);
			} else if (mapProvider instanceof MPWms) {
				_txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Wms);
			} else if (mapProvider instanceof MPProfile) {
				_txtMapProviderType.setText(Messages.Pref_Map_ProviderType_MapProfile);
			}

			_lblOfflineFolderInfo.setText(UI.EMPTY_STRING);
			_offlineContainer.layout(true);
		}
		_isDisableModifyListener = false;

		enableControls();

		_txtMapProviderName.setFocus();
	}

	private void setMapProviderModified() {

		_isModifiedMapProvider = true;

		enableControls();
	}

	private void setWmsDropTarget(final Label label) {

		_wmsDropTarget = new DropTarget(label, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
		_wmsDropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance(), TextTransfer.getInstance() });

		_wmsDropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void dragEnter(final DropTargetEvent e) {
				if (e.detail == DND.DROP_NONE) {
					e.detail = DND.DROP_LINK;
				}
			}

			@Override
			public void dragOperationChanged(final DropTargetEvent e) {
				if (e.detail == DND.DROP_NONE) {
					e.detail = DND.DROP_LINK;
				}
			}

			@Override
			public void drop(final DropTargetEvent event) {

				if (event.data == null) {
					event.detail = DND.DROP_NONE;
					return;
				}

				/*
				 * run async to free the mouse cursor from the drop operation
				 */
				final UIJob uiJob = new UIJob(Messages.Pref_Map_JobName_DropUrl) {

					@Override
					public IStatus runInUIThread(final IProgressMonitor monitor) {

						try {
							createWmsMapProvider((String) event.data);
						} catch (final Exception e) {
							StatusUtil.showStatus(e.getMessage(), e);
						}

						return Status.OK_STATUS;
					}
				};
				uiJob.schedule(10);
			}
		});
	}

	/**
	 * Get offline info from the file system
	 * 
	 * @param updateMapProvider
	 *            when set this map provider will be updated, when <code>null</code> only the
	 *            offline info of the not updated map providers will be updated
	 */
	private void startJobOfflineInfo(final MP updateMapProvider) {

		stopJobOfflineInfo();

		_offlineJobMapProviders.clear();

		if (updateMapProvider == null) {

			// check if offline info is already read
			for (final MP mapProvider : _visibleMp) {
				if (mapProvider.getOfflineFileCounter() == MP.OFFLINE_INFO_NOT_READ) {
					_offlineJobMapProviders.add(mapProvider);
				}
			}
		} else {

			_offlineJobMapProviders.add(updateMapProvider);
		}

		if (_offlineJobMapProviders.size() == 0) {
			// nothing to do
			return;
		}

		// check cache path
		final IPath tileCacheBasePath = getTileCachePath();
		if (tileCacheBasePath == null) {
			return;
		}

		// disable delete offline button
		_isOfflineJobRunning = true;
		enableControls();

		// remove total tile info
		updateUIOfflineInfoTotal();

		_offlineJobGetInfo = new Job(Messages.Pref_Map_JobName_ReadMapFactoryOfflineInfo) {

			@Override
			protected IStatus run(final IProgressMonitor monitor) {

				_isOfflineJobCanceled = false;

				for (final MP mapProvider : _offlineJobMapProviders) {

					final String tileOSFolder = mapProvider.getOfflineFolder();
					if (tileOSFolder == null) {
						continue;
					}

					_offlineJobMp = mapProvider;
					_offlineJobFileCounter = 0;
					_offlineJobFileSize = 0;
					_offlineJobFileCounterUIUpdate = 0;

					final IPath basePath = tileCacheBasePath.addTrailingSeparator();
					boolean skipReading = false;

					File tileCacheDir = basePath.append(tileOSFolder).toFile();
					if (tileCacheDir.exists()) {
						getFilesInfo(tileCacheDir.listFiles());
					} else {
						skipReading = true;
					}

					tileCacheDir = basePath.append(MPProfile.WMS_CUSTOM_TILE_PATH).append(tileOSFolder).toFile();
					if (tileCacheDir.exists() && _isOfflineJobCanceled == false) {
						getFilesInfo(tileCacheDir.listFiles());
					} else {
						skipReading = true;
					}

					if (skipReading) {

						// prevent reading files again

						updateUIOfflineInfo();
						continue;
					}

					if (_isOfflineJobCanceled) {
						// set result invalid
						_offlineJobFileCounter = MP.OFFLINE_INFO_NOT_READ;
						_offlineJobFileSize = MP.OFFLINE_INFO_NOT_READ;
					}

					updateUIOfflineInfo();

					if (_isOfflineJobCanceled) {
						break;
					}
				}

				_isOfflineJobRunning = false;

				Display.getDefault().syncExec(new Runnable() {
					public void run() {

						// enable offline delete button
						enableControls();

						updateUIOfflineInfoTotal();
					}

				});

				return Status.OK_STATUS;
			}
		};

		_offlineJobGetInfo.schedule();
	}

	private void stopJobOfflineInfo() {

		if (_offlineJobGetInfo == null) {
			return;
		}

		_offlineJobGetInfo.cancel();
		_isOfflineJobCanceled = true;

		try {
			_offlineJobGetInfo.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Update map provider model from the UI
	 * 
	 * @return Returns <code>true</code> when the data are valid, otherwise <code>false</code>
	 */
	private boolean updateModelFromUI() {

		if (_isModifiedMapProvider == false) {
			// nothing to do
			return true;
		}

		/*
		 * validate map provider fields
		 */
		final String mpId = _txtMapProviderId.getText().trim();
		final String mpName = _txtMapProviderName.getText().trim();
		final String offlineFolder = _txtOfflineFolder.getText().trim();

		final Control errorControl = validateMapProvider(mpName, mpId, offlineFolder);
		if (errorControl != null) {
			return false;
		}

		/*
		 * get/create map provider
		 */
		final MP mapProvider;
		String oldFactoryId = null;
		String oldOfflineFolder = null;

		if (_isNewMapProvider) {

			// get new map provider
			mapProvider = _newMapProvider;

		} else {
			mapProvider = _selectedMapProvider;
			oldFactoryId = mapProvider.getId();
			oldOfflineFolder = mapProvider.getOfflineFolder();
		}

		// check if offline folder has changed
		if (oldOfflineFolder != null && oldOfflineFolder.equals(offlineFolder) == false) {

			// offline folder has changed, delete files in the old offline folder

			deleteOfflineMapFiles(mapProvider);
		}

		// check if id is modified
		if (oldFactoryId != null && oldFactoryId.equals(mpId) == false) {

			// id is modified 
			// update all profiles with the new id

			for (final MP mp : MapProviderManager.getInstance().getAllMapProviders()) {

				if (mp instanceof MPProfile) {
					for (final MPWrapper mpWrapper : ((MPProfile) mp).getAllWrappers()) {

						if (mpWrapper.getMapProviderId().equals(oldFactoryId)) {
							mpWrapper.setMapProviderId(mpId);
						}
					}
				}
			}
		}

		// update fields
		mapProvider.setId(mpId);
		mapProvider.setName(mpName);
		mapProvider.setDescription(_txtDescription.getText().trim());
		mapProvider.setOfflineFolder(offlineFolder);

		if (_isNewMapProvider) {

			_isNewMapProvider = false;

			// update model
			_visibleMp.add(mapProvider);
			_mpMgr.addMapProvider(mapProvider);

			// update viewer
			_mpViewer.add(mapProvider);

		} else {
			/*
			 * !!! update must be done when a map provider was cloned !!!
			 */
			_mpViewer.update(mapProvider, null);

			// do a resort because the name could be modified, this can be optimized
			_mpViewer.refresh();
		}

		_isModifiedMapProviderList = true;

		_isModifiedMapProvider = false;
		_isModifiedMapProviderId = false;
		_isModifiedOfflineFolder = false;

		// select map provider in the viewer
		_mpViewer.setSelection(new StructuredSelection(mapProvider), true);
		_mpViewer.getTable().setFocus();

		return true;
	}

	private void updateUIOfflineInfo() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {

				// check if UI is available
				if (_mpViewer.getTable().isDisposed() || _offlineJobMp == null) {
					return;
				}

				// update model
				_offlineJobMp.setOfflineFileCounter(_offlineJobFileCounter);
				_offlineJobMp.setOfflineFileSize(_offlineJobFileSize);

				// update viewer
				_mpViewer.update(_offlineJobMp, null);

				// update info detail when the selected map provider is currently in the job
				if (_selectedMapProvider != null && _selectedMapProvider.equals(_offlineJobMp)) {
					updateUIOfflineInfoDetail(_selectedMapProvider);
				}
			}
		});
	}

	/**
	 * update offline info detail
	 */
	private void updateUIOfflineInfoDetail(final MP mapProvider) {

		final int offlineTileCounter = mapProvider.getOfflineFileCounter();
		final long offlineTileSize = mapProvider.getOfflineFileSize();

		final StringBuilder sb = new StringBuilder();

		if (offlineTileCounter == MP.OFFLINE_INFO_NOT_READ) {

			sb.append(Messages.Pref_Map_Lable_NotRetrieved);

		} else if (offlineTileCounter > 0 && offlineTileSize > 0) {

			sb.append(Integer.toString(offlineTileCounter));
			sb.append(UI.SPACE);
			sb.append(Messages.Pref_Map_Lable_Files);
			sb.append(UI.DASH_WITH_SPACE);
			sb.append(_nf.format((float) offlineTileSize / 1024 / 1024));
			sb.append(UI.SPACE);
			sb.append(UI.MBYTES);

		} else {

			sb.append(Messages.Pref_Map_Lable_NotAvailable);
		}

		_lblOfflineFolderInfo.setText(sb.toString());
		_offlineContainer.layout(true);
	}

	private void updateUIOfflineInfoTotal() {

		if (_txtOfflineInfoTotal == null || _txtOfflineInfoTotal.isDisposed()) {
			return;
		}

		final StringBuilder sbTotal = new StringBuilder();

		if (_visibleMp.size() > 0) {

			int tileCounter = 0;
			long tileSize = 0;
			boolean isNA = false;

			for (final MP mapProvider : _visibleMp) {
				final int offlineFileCounter = mapProvider.getOfflineFileCounter();
				if (offlineFileCounter > 0) {
					tileCounter += offlineFileCounter;
					tileSize += mapProvider.getOfflineFileSize();
				} else {
					if (offlineFileCounter < 0) {
						isNA = true;
					}
				}
			}

			if (tileCounter == 0) {
				sbTotal.append(Messages.Pref_Map_Label_OfflineInfo_NotDone);
			} else {

				if (isNA) {
					sbTotal.append(Messages.Pref_Map_Label_OfflineInfo_Partly);
					sbTotal.append(UI.SPACE);
				} else {
					sbTotal.append(Messages.Pref_Map_Label_OfflineInfo_Total);
					sbTotal.append(UI.SPACE);
				}

				sbTotal.append(Integer.toString(tileCounter));
				sbTotal.append(UI.SPACE);
				sbTotal.append(Messages.Pref_Map_Lable_Files);
				sbTotal.append(UI.DASH_WITH_SPACE);
				sbTotal.append(_nf.format((float) tileSize / 1024 / 1024));
				sbTotal.append(UI.SPACE);
				sbTotal.append(UI.MBYTES);
			}
		}

		_txtOfflineInfoTotal.setText(sbTotal.toString());
	}

	/**
	 * @param mapProviderName
	 * @param mapProviderId
	 * @param offlineFolder
	 * @return Returns the control which causes the error or <code>null</code> when data are valid
	 */
	private Control validateMapProvider(final String mapProviderName,
										final String mapProviderId,
										final String offlineFolder) {

		String error = null;
		Control errorControl = null;

		// check name
		if (mapProviderName == null || mapProviderName.length() == 0) {
			error = Messages.Pref_Map_ValidationError_NameIsRequired;
			errorControl = _txtMapProviderName;
		}

		// check offline folder
		if (error == null) {
			error = checkOfflineFolder(offlineFolder);
			if (error != null) {
				errorControl = _txtOfflineFolder;
			}
		}

		// check id
		if (error == null) {
			error = checkMapProviderId(mapProviderId);
			if (error != null) {
				errorControl = _txtMapProviderId;
			}
		}

		setErrorMessage(error);

		// set validation state
		final boolean isValid = error == null;
		_isValid = isValid;
		setValid(isValid);

		return errorControl;
	}

}
