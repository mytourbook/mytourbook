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

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.logging.StatusUtil;
import de.byteholder.geoclipse.map.TileFactory_OLD;
import de.byteholder.geoclipse.map.TileImageCache;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.mapprovider.DialogMP;
import de.byteholder.geoclipse.mapprovider.DialogMPCustom;
import de.byteholder.geoclipse.mapprovider.DialogMPProfile;
import de.byteholder.geoclipse.mapprovider.DialogMPWms;
import de.byteholder.geoclipse.mapprovider.IOfflineInfoListener;
import de.byteholder.geoclipse.mapprovider.MP_OLD;
import de.byteholder.geoclipse.mapprovider.MPCustom;
import de.byteholder.geoclipse.mapprovider.MPPlugin;
import de.byteholder.geoclipse.mapprovider.MPProfile;
import de.byteholder.geoclipse.mapprovider.MPWms;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.mapprovider.MapProviderNavigator;
import de.byteholder.geoclipse.mapprovider.MapProviderWrapper;
import de.byteholder.geoclipse.ui.MessageDialogNoClose;
import de.byteholder.geoclipse.util.PixelConverter;

public class PrefPageMapProviders extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String					PREF_PAGE_MAP_PROVIDER_ID	= "de.byteholder.geoclipse.preferences.PrefPageMapProvider";	//$NON-NLS-1$

	private static final String					XML_EXTENSION				= ".xml";														//$NON-NLS-1$

	/**
	 * max lenghth for map provider id and offline folder
	 */
	private static final int					MAX_ID_LENGTH				= 24;

	private static final String					IMPORT_FILE_PATH			= "MapProvider_ImportFilePath";								//$NON-NLS-1$
	private static final String					EXPORT_FILE_PATH			= "MapProvider_ExportFilePath";								//$NON-NLS-1$

	private MapProviderManager					fMapProviderMgr				= MapProviderManager.getInstance();

	/**
	 * contains all visible map providers
	 */
	private ArrayList<MP_OLD>						fMapProviders;

	/**
	 * map provider's which are used when getting offline info
	 */
	private ArrayList<MP_OLD>						fOfflineJobMapProviders		= new ArrayList<MP_OLD>();

	private IOfflineInfoListener				fOfflineJobInfoListener;

	private Job									fOfflineJobGetInfo;
	private MP_OLD									fOfflineJobMapProvider;
	private int									fOfflineJobFileCounter;
	private int									fOfflineJobFileSize;
	private int									fOfflineJobFileCounterUIUpdate;

	/**
	 * is <code>true</code> when the job is canceled
	 */
	private boolean								fIsOfflineJobCanceled		= true;

	private boolean								fIsOfflineJobRunning		= false;

	private boolean								fIsDisableModifyListener	= false;

	private IPreferenceStore					fPrefStore					= Activator
																					.getDefault()
																					.getPreferenceStore();

	/**
	 * map provider which is currently selected in the list
	 */
	private MP_OLD									fSelectedMapProvider;

	private MP_OLD									fNewMapProvider;

	/*
	 * UI controls
	 */
	private TableViewer							fMapProviderViewer;

	private Text								fTxtOfflineInfoTotal;
	private Text								fTxtMapProviderName;
	private Text								fTxtMapProviderId;
	private Text								fTxtMapProviderType;
	private Label								fLblDescription;
	private Text								fTxtDescription;
	private Composite							fOfflineContainer;
	private Text								fTxtOfflineFolder;
	private Label								fLblOfflineFolderInfo;
	private Text								fTxtUrl;

	private Label								fLblDropTarget;
	private DropTarget							fWmsDropTarget;
	private DropTarget							fMpDropTarget;

	private Button								fBtnAddMapProviderCustom;
	private Button								fBtnAddMapProviderWms;
	private Button								fBtnAddMapProviderMapProfile;
	private Button								fBtnDeleteMapProvider;
	private Button								fBtnDeleteOfflineMap;
	private Button								fBtnUpdate;
	private Button								fBtnCancel;
	private Button								fBtnEdit;

	private boolean								fIsNewMapProvider			= false;
	private boolean								fIsModifiedMapProvider		= false;
	private boolean								fIsModifiedMapProviderList	= false;
	private boolean								fForceUpdateMapProviderList	= false;
	private boolean								fIsValid					= true;

	private final SelectionAdapter				fSelectionListener;
	private final ModifyListener				fModifyListener;

	private boolean								fIsModifiedOfflineFolder;
	private boolean								fIsModifiedMapProviderId;
	private boolean								fIsDeleteError;

	private ActionRefreshOfflineInfoSelected	fActionRefreshSelected;
	private ActionRefreshOfflineInfoAll			fActionRefreshAll;
	private ActionCancelRefreshOfflineInfo		fActionCancelRefresh;
	private ActionRefreshOfflineInfoNotAssessed	fActionRefreshNotAssessed;

	private Group								fGroupDetails;

	private Button								fBtnImport;

	private Button								fBtnExport;

	private Label								fLblMpDropTarget;

	final static NumberFormat					fNf							= NumberFormat.getNumberInstance();

	{
		fNf.setMinimumFractionDigits(2);
		fNf.setMaximumFractionDigits(2);
		fNf.setMinimumIntegerDigits(1);
	}

	private class MapContentProvider implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return fMapProviders.toArray();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public PrefPageMapProviders() {

		noDefaultAndApplyButton();

		fModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (fIsDisableModifyListener == false) {
					setMapProviderModified();
				}
			}
		};

		fSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (fIsDisableModifyListener == false) {
					setMapProviderModified();
				}
			}
		};
	}

	void actionCancelRefreshOfflineInfo() {
		stopJobOfflineInfo();
	}

	void actionRefreshOfflineInfo(final boolean isRefreshSelectedMapProvider) {

		MP_OLD updateMapProvider = null;

		if (isRefreshSelectedMapProvider) {

			/*
			 * refresh selected map provider
			 */

			updateMapProvider = fSelectedMapProvider;

		} else {

			/*
			 * refresh all map providers
			 */

			for (final MP_OLD mapProvider : fMapProviders) {
				mapProvider.setStateToReloadOfflineCounter();
			}

			fMapProviderViewer.update(fMapProviders.toArray(), null);
		}

		startJobOfflineInfo(updateMapProvider);
	}

	void actionRefreshOfflineInfoNotAssessed() {
		startJobOfflineInfo(null);
	}

	private void addListener() {

		fOfflineJobInfoListener = new IOfflineInfoListener() {

			public void offlineInfoIsDirty(final MP_OLD mapProvider) {

				Display.getDefault().asyncExec(new Runnable() {
					public void run() {

						if (fMapProviderViewer == null || fMapProviderViewer.getTable().isDisposed()) {
							return;
						}

						fMapProviderViewer.update(mapProvider, null);

						updateUIOfflineInfoTotal();
					}
				});
			}
		};

		MP_OLD.addOfflineInfoListener(fOfflineJobInfoListener);
	}

	private String checkBaseUrl(final String baseUrl) {

		if (baseUrl == null || baseUrl.length() == 0) {
			return Messages.pref_map_validationError_baseUrlIsRequired;
		} else {

			try {
				new URL(baseUrl);
			} catch (final MalformedURLException e) {
				return Messages.pref_map_validationError_invalidUrl;
			}
		}

		return null;
	}

	private String checkMapProviderId(final String factoryId) {

		String error = null;
		if (factoryId == null || factoryId.length() == 0) {
			error = Messages.pref_map_validationError_factoryIdIsRequired;
		} else {

			// check if the id is already used

			for (final MP_OLD mapProvider : fMapProviders) {

				final String otherFactoryId = mapProvider.getId();

				if (fIsNewMapProvider) {

					// new map provider: another id with the same name is not allowed

					if (factoryId.equalsIgnoreCase(otherFactoryId)) {
						error = Messages.pref_map_validationError_factoryIdIsAlreadyUsed;
						break;
					}

				} else {

					// check existing map providers

					if (fSelectedMapProvider.equals(mapProvider) == false) {

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

			for (final MP_OLD mapProvider : fMapProviders) {

				if (fIsNewMapProvider) {

					// new map provider: folder with the same name is not allowed

					if (offlineFolder.equalsIgnoreCase(mapProvider.getOfflineFolder())) {
						error = Messages.pref_map_validationError_offlineFolderIsAlreadyUsed;
						break;
					}

				} else {

					// existing map provider

					if (fSelectedMapProvider.equals(mapProvider) == false) {

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

		if (fIsNewMapProvider == false) {
			return;
		}

		if (fIsModifiedMapProviderId && fIsModifiedOfflineFolder) {
			return;
		}

		final String name = fTxtMapProviderName.getText().trim().toLowerCase();
		final String validText = UI.createIdFromName(name, MAX_ID_LENGTH);

		fIsDisableModifyListener = true;
		{
			if (fIsModifiedMapProviderId == false) {
				fTxtMapProviderId.setText(validText);
			}

			if (fIsModifiedOfflineFolder == false) {
				fTxtOfflineFolder.setText(validText);
			}
		}
		fIsDisableModifyListener = false;
	}

	@Override
	protected Control createContents(final Composite parent) {

		fMapProviders = fMapProviderMgr.getAllMapProviders(true);

		addListener();

		initializeDialogUnits(parent);
		final Composite container = createUI(parent);

		// load viewer
		fMapProviderViewer.setInput(new Object());

		restoreState();

		updateUIOfflineInfoTotal();

		fMapProviderViewer.getTable().setFocus();

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

			fActionRefreshNotAssessed = new ActionRefreshOfflineInfoNotAssessed(this);
			fActionRefreshSelected = new ActionRefreshOfflineInfoSelected(this);
			fActionRefreshAll = new ActionRefreshOfflineInfoAll(this);
			fActionCancelRefresh = new ActionCancelRefreshOfflineInfo(this);

			tbm.add(fActionRefreshNotAssessed);
			tbm.add(fActionRefreshSelected);
			tbm.add(fActionRefreshAll);
			tbm.add(fActionCancelRefresh);

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

		fMapProviderViewer = new TableViewer(table);
		fMapProviderViewer.setUseHashlookup(true);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tc;

		// column: server type
		tvc = new TableViewerColumn(fMapProviderViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setToolTipText(Messages.Pref_Map_Viewer_Column_Lbl_ServerType_Tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MP_OLD mapProvider = (MP_OLD) cell.getElement();

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
		tvc = new TableViewerColumn(fMapProviderViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map_Viewer_Column_Lbl_MapProvider);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MP_OLD mapProvider = (MP_OLD) cell.getElement();

				cell.setText(mapProvider.getName());
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnWeightData(20));

		// column: offline path
		tvc = new TableViewerColumn(fMapProviderViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map_Viewer_Column_Lbl_OfflinePath);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MP_OLD mapProvider = (MP_OLD) cell.getElement();

				cell.setText(mapProvider.getOfflineFolder());
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnWeightData(10));

		// column: layer
		tvc = new TableViewerColumn(fMapProviderViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map_Viewer_Column_Lbl_Layer);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				String layer = UI.EMPTY_STRING;

				final MP_OLD mapProvider = (MP_OLD) cell.getElement();
				if (mapProvider instanceof MPWms) {

					final MPWms wmsMapProvider = (MPWms) mapProvider;

					final StringBuilder sb = new StringBuilder();
//					sb.append(" (");//$NON-NLS-1$
					sb.append(wmsMapProvider.getAvailableLayers());
//					sb.append(")");//$NON-NLS-1$

					layer = sb.toString();
				}
				cell.setText(layer);
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(10)));

		// column: offline file counter
		tvc = new TableViewerColumn(fMapProviderViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map_Viewer_Column_Lbl_OfflineFileCounter);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int offlineTileCounter = ((MP_OLD) cell.getElement()).getOfflineFileCounter();
				if (offlineTileCounter == MP_OLD.OFFLINE_INFO_NOT_READ) {
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
		tvc = new TableViewerColumn(fMapProviderViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map_Viewer_Column_Lbl_OfflineFileSize);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final long offlineTileSize = ((MP_OLD) cell.getElement()).getOfflineFileSize();
				if (offlineTileSize == MP_OLD.OFFLINE_INFO_NOT_READ) {
					cell.setText(Messages.pref_map_lable_NA);
				} else if (offlineTileSize > 0) {
					cell.setText(fNf.format((float) offlineTileSize / 1024 / 1024));
				} else {
					cell.setText(UI.DASH_WITH_SPACE);
				}
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(12)));

		/*
		 * create table viewer
		 */
		fMapProviderViewer.setContentProvider(new MapContentProvider());
		fMapProviderViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {

				final MP_OLD mp1 = (MP_OLD) e1;
				final MP_OLD mp2 = (MP_OLD) e2;

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

		fMapProviderViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectMapProvider(event);
			}
		});
		fMapProviderViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				if (fSelectedMapProvider instanceof MPWms) {

					openConfigDialogWms();

				} else if (fSelectedMapProvider instanceof MPCustom) {

					openConfigDialogCustom();

				} else if (fSelectedMapProvider instanceof MPProfile) {

					openConfigDialogMapProfile();

				} else {

					// select name
					fTxtMapProviderName.setFocus();
					fTxtMapProviderName.selectAll();
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
			fBtnEdit = new Button(btnContainer, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.END, SWT.FILL)
					.applyTo(fBtnEdit);
			fBtnEdit.setText(Messages.Pref_Map_Button_Edit);
			fBtnEdit.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					openConfigDialog();
				}
			});
			setButtonLayoutData(fBtnEdit);

			// button: add custom map provider
			fBtnAddMapProviderCustom = new Button(btnContainer, SWT.NONE);
			fBtnAddMapProviderCustom.setText(Messages.Pref_Map_Button_AddMapProviderCustom);
			fBtnAddMapProviderCustom.setToolTipText(Messages.Pref_Map_Button_AddMapProviderCustom_Tooltip);
			fBtnAddMapProviderCustom.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					setEmptyMapProviderUI(new MPCustom());
				}
			});
			setButtonLayoutData(fBtnAddMapProviderCustom);

			// button: add wms map provider
			fBtnAddMapProviderWms = new Button(btnContainer, SWT.NONE);
			fBtnAddMapProviderWms.setText(Messages.Pref_Map_Button_AddMapProviderWms);
			fBtnAddMapProviderWms.setToolTipText(Messages.Pref_Map_Button_AddMapProviderWms_Tooltip);
			fBtnAddMapProviderWms.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onAddWmsMapProvider();
				}
			});
			setButtonLayoutData(fBtnAddMapProviderWms);

			// button: add profile map provider
			fBtnAddMapProviderMapProfile = new Button(btnContainer, SWT.NONE);
			fBtnAddMapProviderMapProfile.setText(Messages.Pref_Map_Button_AddMapProfile);
			fBtnAddMapProviderMapProfile.setToolTipText(Messages.Pref_Map_Button_AddMapProfile_Tooltip);
			fBtnAddMapProviderMapProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					final MPProfile mapProfile = new MPProfile();
					mapProfile.synchronizeMPWrapper();

					setEmptyMapProviderUI(mapProfile);
				}
			});
			setButtonLayoutData(fBtnAddMapProviderMapProfile);

			// wms drag&drop target
			fLblDropTarget = new Label(btnContainer, SWT.BORDER | SWT.WRAP | SWT.CENTER);
			GridDataFactory.fillDefaults().applyTo(fLblDropTarget);
			fLblDropTarget.setText(Messages.Pref_Map_Label_WmsDropTarget);
			fLblDropTarget.setToolTipText(Messages.Pref_Map_Label_WmsDropTarget_Tooltip);
			setWmsDropTarget(fLblDropTarget);

			// button: delete offline map
			fBtnDeleteOfflineMap = new Button(btnContainer, SWT.NONE);
			fBtnDeleteOfflineMap.setText(Messages.Pref_Map_Button_DeleteOfflineMap);
			fBtnDeleteOfflineMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					deleteOfflineMap(fSelectedMapProvider);
				}
			});
			setButtonLayoutData(fBtnDeleteOfflineMap);

			// button: import
			fBtnImport = new Button(btnContainer, SWT.NONE);
			fBtnImport.setText(Messages.Pref_Map_Button_ImportMP);
			fBtnImport.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onImportMP();
				}
			});
			setButtonLayoutData(fBtnImport);
			GridData gd = setButtonLayoutData(fBtnImport);
			gd.verticalIndent = 20;

			// button: export
			fBtnExport = new Button(btnContainer, SWT.NONE);
			fBtnExport.setText(Messages.Pref_Map_Button_ExportMP);
			fBtnExport.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onExportMP();
				}
			});
			setButtonLayoutData(fBtnExport);

			/*
			 * drop target: map provider
			 */
			fLblMpDropTarget = new Label(btnContainer, SWT.BORDER | SWT.WRAP | SWT.CENTER);
			GridDataFactory.fillDefaults().applyTo(fLblMpDropTarget);
			fLblMpDropTarget.setText(Messages.Pref_Map_Label_MapProviderDropTarget);
			fLblMpDropTarget.setToolTipText(Messages.Pref_Map_Label_MapProviderDropTarget_Tooltip);
			fMpDropTarget = new DropTarget(fLblMpDropTarget, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);

			fMpDropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance(), FileTransfer.getInstance() });

			fMpDropTarget.addDropListener(new DropTargetAdapter() {

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
			 * button: delete map provider
			 */
			fBtnDeleteMapProvider = new Button(btnContainer, SWT.NONE);
			fBtnDeleteMapProvider.setText(Messages.Pref_Map_Button_DeleteMapProvider);
			fBtnDeleteMapProvider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onDeleteMapProvider();
				}
			});
			gd = setButtonLayoutData(fBtnDeleteMapProvider);
			gd.grabExcessVerticalSpace = true;
			gd.verticalAlignment = SWT.END;
			gd.verticalIndent = 20;

		}
	}

	private void createUI40ReadTileSize(final Composite parent) {

		/*
		 * text: offline info
		 */
		fTxtOfflineInfoTotal = new Text(parent, SWT.READ_ONLY | SWT.TRAIL);
		GridDataFactory.fillDefaults().applyTo(fTxtOfflineInfoTotal);
	}

	private void createUI50Details(final Composite parent) {

		fGroupDetails = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).span(2, 1).applyTo(fGroupDetails);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(fGroupDetails);
		fGroupDetails.setText(Messages.Pref_Map_Group_Detail_SelectedMapProvider);
		{
			createUI52DetailsDetails(fGroupDetails);
			createUI54DetailsButtons(fGroupDetails);
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
			fTxtMapProviderName = new Text(detailContainer, SWT.BORDER);
			GridDataFactory.fillDefaults().applyTo(fTxtMapProviderName);
			fTxtMapProviderName.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					if (fIsDisableModifyListener) {
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
			fLblDescription = new Label(detailContainer, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(fLblDescription);
			fLblDescription.setText(Messages.Pref_Map_Label_Description);

			// text: description
			fTxtDescription = new Text(detailContainer, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
			GridDataFactory.fillDefaults()//
					.hint(pixCon.convertWidthInCharsToPixels(20), pixCon.convertHeightInCharsToPixels(5))
					.grab(true, false)
					.applyTo(fTxtDescription);
			fTxtDescription.addModifyListener(fModifyListener);

			/*
			 * offline folder
			 */
			// label: offline folder
			label = new Label(detailContainer, SWT.NONE);
			label.setText(Messages.Pref_Map_Lable_OfflineFolder);

			fOfflineContainer = new Composite(detailContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fOfflineContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true).applyTo(fOfflineContainer);
			{
				// text: offline folder
				fTxtOfflineFolder = new Text(fOfflineContainer, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.hint(pixCon.convertWidthInCharsToPixels(20), SWT.DEFAULT)
						.applyTo(fTxtOfflineFolder);
				fTxtOfflineFolder.setTextLimit(MAX_ID_LENGTH);
				fTxtOfflineFolder.addVerifyListener(verifyListener);

				fTxtOfflineFolder.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {

						if (fIsDisableModifyListener) {
							return;
						}

						fIsModifiedOfflineFolder = true;
						setMapProviderModified();

						// force offline folder to be reloaded
						fSelectedMapProvider.setStateToReloadOfflineCounter();
					}
				});

				// label: offline info
				fLblOfflineFolderInfo = new Label(fOfflineContainer, SWT.TRAIL);
				GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.CENTER).applyTo(
						fLblOfflineFolderInfo);
			}

			/*
			 * map provider id
			 */
			label = new Label(detailContainer, SWT.NONE);
			label.setText(Messages.Pref_Map_Lable_MapProviderId);

			// text: map provider id
			fTxtMapProviderId = new Text(detailContainer, SWT.BORDER);
			GridDataFactory.fillDefaults().applyTo(fTxtMapProviderId);
			fTxtMapProviderId.setTextLimit(MAX_ID_LENGTH);
			fTxtMapProviderId.addVerifyListener(verifyListener);

			fTxtMapProviderId.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					if (fIsDisableModifyListener) {
						return;
					}

					fIsModifiedMapProviderId = true;
					setMapProviderModified();
				}
			});

			/*
			 * map provider type
			 */
			label = new Label(detailContainer, SWT.NONE);
			label.setText(Messages.Pref_Map_Lable_MapProviderType);

			// text: map provider type
			fTxtMapProviderType = new Text(detailContainer, SWT.BORDER | SWT.READ_ONLY);
			GridDataFactory.fillDefaults().applyTo(fTxtMapProviderType);

			/*
			 * url
			 */
			label = new Label(detailContainer, SWT.NONE);
			label.setText(Messages.Pref_Map_Label_Url);

			// text: map provider type
			fTxtUrl = new Text(detailContainer, SWT.BORDER | SWT.READ_ONLY);
			GridDataFactory.fillDefaults().hint(200, SWT.DEFAULT).applyTo(fTxtUrl);
		}
	}

	private void createUI54DetailsButtons(final Group parent) {

		final Composite btnContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(10, 0).applyTo(btnContainer);
		GridLayoutFactory.fillDefaults().applyTo(btnContainer);
		{
			// button: update
			fBtnUpdate = new Button(btnContainer, SWT.NONE);
			fBtnUpdate.setText(Messages.Pref_Map_Button_UpdateMapProvider);
			fBtnUpdate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onUpdateMapProvider();
				}
			});
			setButtonLayoutData(fBtnUpdate);

			// button: cancel
			fBtnCancel = new Button(btnContainer, SWT.NONE);
			fBtnCancel.setText(Messages.Pref_Map_Button_CancelMapProvider);
			fBtnCancel.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onCancelMapProvider();
				}
			});
			setButtonLayoutData(fBtnCancel);
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

			fIsNewMapProvider = false;

			fIsModifiedMapProvider = false;
			fIsModifiedMapProviderId = false;
			fIsModifiedOfflineFolder = false;

			// update model
			wmsMapProvider.setMapProviderId(uniqueId);
			wmsMapProvider.setName(providerName);
			wmsMapProvider.setDescription(wmsAbstract);
			wmsMapProvider.setOfflineFolder(uniqueId);

			wmsMapProvider.setGetMapUrl(getMapUrl);
			wmsMapProvider.setCapabilitiesUrl(capsUrl);

			fMapProviderMgr.addMapProvider(wmsMapProvider);

			fMapProviders.add(wmsMapProvider);
			fIsModifiedMapProviderList = true;

			// update viewer
			fMapProviderViewer.add(wmsMapProvider);

			// select map provider in the viewer this will display the wms server in the UI
			fMapProviderViewer.setSelection(new StructuredSelection(wmsMapProvider), true);

			fMapProviderViewer.getTable().setFocus();

		} else {

			/*
			 * data validation displays an error message, show data in the UI but do not create a
			 * map provider, this simulates the situation when the user presses the new button and
			 * enters the values
			 */

			fIsNewMapProvider = true;
			fNewMapProvider = wmsMapProvider;

			fIsModifiedMapProvider = true;
			fIsModifiedMapProviderId = false;
			fIsModifiedOfflineFolder = false;

			fIsDisableModifyListener = true;
			{
				/*
				 * set map provider fields
				 */

				fTxtMapProviderName.setText(providerName);
				fTxtMapProviderId.setText(uniqueId);
				fTxtOfflineFolder.setText(uniqueId);
				fTxtDescription.setText(wmsAbstract);
				fTxtUrl.setText(capsUrl);

				fLblOfflineFolderInfo.setText(UI.EMPTY_STRING);
				fOfflineContainer.layout(true);
			}
			fIsDisableModifyListener = false;

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
					fIsDeleteError = true;
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

	public void deleteOfflineMap(final MP_OLD mapProvider) {

		deleteOfflineMapFiles(mapProvider);

		// disable delete offline button
		enableControls();

		// reselect map provider, set focus to map provider
		fMapProviderViewer.setSelection(fMapProviderViewer.getSelection());
		fMapProviderViewer.getTable().setFocus();
	}

	private void deleteOfflineMapFiles(final MP_OLD mapProvider) {

		// reset state that offline images are available
		final TileFactory_OLD tileFactory = mapProvider.getTileFactory(false);
		if (tileFactory != null) {
			tileFactory.resetTileImageAvailability();
		}

		// check base path
		IPath tileCacheBasePath = getTileCachePath();
		if (tileCacheBasePath == null) {
			return;
		}

		// check map provider offline folder
		final String tileOSFolder = mapProvider.getOfflineFolder();
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

			mapProvider.setStateToReloadOfflineCounter();

			// update viewer
			fMapProviderViewer.update(mapProvider, null);

			updateUIOfflineInfoTotal();

			// clear map image cache
			mapProvider.disposeCachedImages();
		}
	}

	private void deleteOfflineMapFilesDir(final File tileCacheDir) {

		fIsDeleteError = false;

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				deleteDir(tileCacheDir);
			}
		});

		if (fIsDeleteError) {
			StatusUtil.log(NLS.bind(Messages.pref_map_error_deleteTiles_message, tileCacheDir), new Exception());
		}
	}

	@Override
	public void dispose() {

		stopJobOfflineInfo();

		if (fWmsDropTarget != null) {
			fWmsDropTarget.dispose();
		}
		if (fMpDropTarget != null) {
			fMpDropTarget.dispose();
		}

		MP_OLD.removeOfflineInfoListener(fOfflineJobInfoListener);

		super.dispose();
	}

	private void doImportMP(final String importFilePath) {

		// create map provider from xml file
		final ArrayList<MP_OLD> importedMPs = MapProviderManager.getInstance().importMapProvider(importFilePath);

		// select imported map provider
		if (importedMPs != null) {

			fForceUpdateMapProviderList = true;

			// update model
			fMapProviders.addAll(importedMPs);

			// update viewer
			fMapProviderViewer.add(importedMPs.toArray(new MP_OLD[importedMPs.size()]));

			// select map provider in the viewer
			fMapProviderViewer.setSelection(new StructuredSelection(importedMPs.get(0)), true);
		}

		fMapProviderViewer.getTable().setFocus();
	}

	private void enableControls() {

		// focus get's lost when a map provider is modified the first time
		final Control focusControl = Display.getCurrent().getFocusControl();

		/*
		 * validate UI data
		 */
		validateMapProvider(fTxtMapProviderName.getText().trim(), fTxtMapProviderId.getText().trim(), fTxtOfflineFolder
				.getText()
				.trim());

		final boolean isMapProvider = fSelectedMapProvider != null;

		final boolean isExistingMapProvider = isMapProvider
				&& fIsNewMapProvider == false
				&& fIsModifiedMapProvider == false;

		final boolean isOfflineJobStopped = fIsOfflineJobRunning == false;

		final boolean canDeleteOfflineMap = isMapProvider
				&& fSelectedMapProvider.getOfflineFileCounter() > 0
				&& isOfflineJobStopped;

		final boolean isOfflinePath = getTileCachePath() != null;

		final boolean isCustomMapProvider = fSelectedMapProvider instanceof MPCustom;
		final boolean isMapProfile = fSelectedMapProvider instanceof MPProfile;
		final boolean isWmsMapProvider = fSelectedMapProvider instanceof MPWms;

		final boolean isNonePluginMapProvider = isCustomMapProvider || isWmsMapProvider || isMapProfile;
		final boolean canEditFields = fIsNewMapProvider || isNonePluginMapProvider;

		fMapProviderViewer.getTable().setEnabled(isExistingMapProvider);

		fTxtMapProviderName.setEnabled(canEditFields);
		fTxtMapProviderId.setEnabled(canEditFields);
		fTxtOfflineFolder.setEnabled(canEditFields);
		fTxtDescription.setEnabled(canEditFields);

		// map provider list actions
		fBtnAddMapProviderCustom.setEnabled(isExistingMapProvider);
		fBtnAddMapProviderWms.setEnabled(isExistingMapProvider);
		fBtnAddMapProviderMapProfile.setEnabled(isExistingMapProvider);
		fBtnEdit.setEnabled(isExistingMapProvider && isNonePluginMapProvider);
		fBtnDeleteMapProvider.setEnabled(isExistingMapProvider && isNonePluginMapProvider);
		fBtnDeleteOfflineMap.setEnabled(isExistingMapProvider && canDeleteOfflineMap);
		fBtnImport.setEnabled(isExistingMapProvider);
		fBtnExport.setEnabled(isExistingMapProvider & isNonePluginMapProvider);

		fLblDropTarget.setEnabled(fIsModifiedMapProvider == false && fIsNewMapProvider == false);
		fLblMpDropTarget.setEnabled(fIsModifiedMapProvider == false && fIsNewMapProvider == false);

		// map provider detail actions
		fBtnUpdate.setEnabled(fIsValid && fIsModifiedMapProvider);
		fBtnCancel.setEnabled(fIsNewMapProvider || fIsModifiedMapProvider);

		fActionRefreshAll.setEnabled(isOfflinePath && isOfflineJobStopped);
		fActionRefreshSelected.setEnabled(isOfflinePath && isOfflineJobStopped);
		fActionRefreshNotAssessed.setEnabled(isOfflinePath && isOfflineJobStopped);
		fActionCancelRefresh.setEnabled(isOfflinePath && !isOfflineJobStopped);

		if (fIsNewMapProvider) {
			fGroupDetails.setText(Messages.Pref_Map_Group_Detail_NewMapProvider);
		} else {
			if (fIsModifiedMapProvider) {
				fGroupDetails.setText(Messages.Pref_Map_Group_Detail_ModifiedMapProvider);
			} else {
				fGroupDetails.setText(Messages.Pref_Map_Group_Detail_SelectedMapProvider);
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

		if (fIsOfflineJobCanceled) {
			return;
		}

		// update UI
		if (fOfflineJobFileCounter > fOfflineJobFileCounterUIUpdate + 1000) {
			fOfflineJobFileCounterUIUpdate = fOfflineJobFileCounter;
			updateUIOfflineInfo();
		}

		for (int fileIndex = 0; fileIndex < listOfFiles.length; fileIndex++) {
			final File file = listOfFiles[fileIndex];
			if (file.isFile()) {

				// file

				fOfflineJobFileCounter++;
				fOfflineJobFileSize += file.length();

			} else if (file.isDirectory()) {

				// directory

				getFilesInfo(file.listFiles());

				if (fIsOfflineJobCanceled) {
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

		final Table table = fMapProviderViewer.getTable();
		final int selectionIndex = table.getSelectionIndex();
		final int itemCount = table.getItemCount();
		int isNextNext = -1;

		for (int itemIndex = selectionIndex + 1; itemIndex < itemCount; itemIndex++) {

			final Object mapProvider = fMapProviderViewer.getElementAt(itemIndex);
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
		fMapProviderViewer.setSelection(new StructuredSelection(nextMapProvider));

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

		final Table table = fMapProviderViewer.getTable();
		final int selectionIndex = table.getSelectionIndex();
		int isNextNext = -1;

		for (int itemIndex = selectionIndex - 1; itemIndex > -1; itemIndex--) {

			final Object tableItem = fMapProviderViewer.getElementAt(itemIndex);
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
		fMapProviderViewer.setSelection(new StructuredSelection(prevMapProvider));

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
		return fIsValid;
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

		fIsNewMapProvider = false;
		fIsModifiedMapProvider = false;
		fIsModifiedMapProviderId = false;
		fIsModifiedOfflineFolder = false;

		setErrorMessage(null);

		// reselect map provider
		fMapProviderViewer.setSelection(fMapProviderViewer.getSelection());

		enableControls();

		fMapProviderViewer.getTable().setFocus();
	}

	private void onDeleteMapProvider() {

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.pref_map_dlg_confirmDeleteMapProvider_title,
				NLS.bind(Messages.pref_map_dlg_confirmDeleteMapProvider_message, fSelectedMapProvider.getName())) == false) {
			return;
		}

		// get map provider which will be selected when the current will be removed 
		final int selectionIndex = fMapProviderViewer.getTable().getSelectionIndex();
		Object nextSelectedMapProvider = fMapProviderViewer.getElementAt(selectionIndex + 1);
		if (nextSelectedMapProvider == null) {
			nextSelectedMapProvider = fMapProviderViewer.getElementAt(selectionIndex - 1);
		}

		// delete offline files
		deleteOfflineMapFiles(fSelectedMapProvider);

		// remove from viewer
		fMapProviderViewer.remove(fSelectedMapProvider);

		// remove from model
		fMapProviderMgr.remove(fSelectedMapProvider);
		fMapProviders.remove(fSelectedMapProvider);

		// select another map provider at the same position
		if (nextSelectedMapProvider != null) {
			fMapProviderViewer.setSelection(new StructuredSelection(nextSelectedMapProvider));
			fMapProviderViewer.getTable().setFocus();
		}

		// custom map provider list must be updated
		fForceUpdateMapProviderList = true;
	}

	private void onExportMP() {

		final FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setText(Messages.Pref_Map_Dialog_Export_Title);

		dialog.setFilterPath(fPrefStore.getString(EXPORT_FILE_PATH));

		dialog.setFilterExtensions(new String[] { "*.*", "xml" });//$NON-NLS-1$ //$NON-NLS-2$
		dialog.setFilterNames(new String[] {
				Messages.PrefPageMapProviders_Pref_Map_FileDialog_AllFiles,
				Messages.PrefPageMapProviders_Pref_Map_FileDialog_XmlFiles });

		dialog.setFileName(fSelectedMapProvider.getId() + XML_EXTENSION);//$NON-NLS-1$

		final String selectedFilePath = dialog.open();
		if (selectedFilePath == null) {
			// dialog is canceled
			return;
		}

		final File exportFilePath = new Path(selectedFilePath).toFile();

		// keep path
		fPrefStore.setValue(EXPORT_FILE_PATH, exportFilePath.getPath());

		if (exportFilePath.exists()) {
			if (UI.confirmOverwrite(exportFilePath) == false) {
				// don't overwrite file, nothing more to do
				return;
			}
		}

		MapProviderManager.getInstance().exportMapProvider(fSelectedMapProvider, exportFilePath);

		fMapProviderViewer.getTable().setFocus();
	}

	private void onImportMP() {

		final FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);

		dialog.setText(Messages.Pref_Map_Dialog_Import_Title);
		dialog.setFilterPath(fPrefStore.getString(IMPORT_FILE_PATH));

		dialog.setFilterExtensions(new String[] { "*.*", "xml" });//$NON-NLS-1$ //$NON-NLS-2$
		dialog.setFilterNames(new String[] {
				Messages.PrefPageMapProviders_Pref_Map_FileDialog_AllFiles,
				Messages.PrefPageMapProviders_Pref_Map_FileDialog_XmlFiles });

		dialog.setFileName(fSelectedMapProvider.getId() + XML_EXTENSION);//$NON-NLS-1$

		final String selectedFilePath = dialog.open();

		if (selectedFilePath == null) {
			// dialog is canceled
			return;
		}

		// keep path
		fPrefStore.setValue(IMPORT_FILE_PATH, selectedFilePath);

		doImportMP(selectedFilePath);
	}

	private void onSelectMapProvider(final SelectionChangedEvent event) {

		final Object firstElement = ((StructuredSelection) event.getSelection()).getFirstElement();
		if (firstElement instanceof MP_OLD) {

			final MP_OLD mapProvider = (MP_OLD) firstElement;

			fSelectedMapProvider = mapProvider;

			fIsDisableModifyListener = true;
			{

				// update UI
				fTxtMapProviderName.setText(mapProvider.getName());
				fTxtMapProviderId.setText(mapProvider.getId());
				fTxtDescription.setText(mapProvider.getDescription());

				// offline folder
				final String tileOSFolder = mapProvider.getOfflineFolder();
				if (tileOSFolder == null) {
					fTxtOfflineFolder.setText(UI.EMPTY_STRING);
				} else {
					fTxtOfflineFolder.setText(tileOSFolder);
				}

				if (mapProvider instanceof MPWms) {

					/*
					 * wms map provider
					 */
					final MPWms wmsMapProvider = (MPWms) mapProvider;

					fTxtMapProviderType.setText(Messages.Pref_Map_ProviderType_Wms);
					fTxtUrl.setText(wmsMapProvider.getCapabilitiesUrl());

				} else if (mapProvider instanceof MPCustom) {

					/*
					 * custom map provider
					 */
					final MPCustom customMapProvider = (MPCustom) mapProvider;

					fTxtMapProviderType.setText(Messages.Pref_Map_ProviderType_Custom);
					fTxtUrl.setText(customMapProvider.getCustomUrl());

				} else if (mapProvider instanceof MPProfile) {

					/*
					 * map profile
					 */

					fTxtMapProviderType.setText(Messages.Pref_Map_ProviderType_MapProfile);
					fTxtUrl.setText(UI.EMPTY_STRING);

				} else if (mapProvider instanceof MPPlugin) {

					/*
					 * plugin map provider
					 */
					final MPPlugin pluginMapProvider = (MPPlugin) mapProvider;

					fTxtMapProviderType.setText(Messages.Pref_Map_ProviderType_Plugin);
					fTxtUrl.setText(pluginMapProvider.getTileFactory(true).getInfo().getBaseURL());
				}

				updateUIOfflineInfoDetail(mapProvider);
			}
			fIsDisableModifyListener = false;

			enableControls();
		}
	}

	private void onUpdateMapProvider() {
		updateModelFromUI();
		enableControls();
	}

	private void openConfigDialog() {

		if (fIsModifiedMapProvider) {
			// update is necessary when the map provider is modified
			if (updateModelFromUI() == false) {
				return;
			}
		}

		if (fSelectedMapProvider instanceof MPWms) {

			openConfigDialogWms();

		} else if (fSelectedMapProvider instanceof MPCustom) {

			openConfigDialogCustom();

		} else if (fSelectedMapProvider instanceof MPProfile) {

			openConfigDialogMapProfile();
		}

		// set focus back to table
		fMapProviderViewer.getTable().setFocus();
	}

	private void openConfigDialogCustom() {

		try {

			// clone mapprovider
			final MPCustom dialogMapProvider = (MPCustom) ((MPCustom) fSelectedMapProvider).clone();

			// map images are likely to be downloaded
			dialogMapProvider.setStateToReloadOfflineCounter();

			final DialogMPCustom dialog = new DialogMPCustom(
					Display.getCurrent().getActiveShell(),
					this,
					dialogMapProvider);

			if (dialog.open() == Window.OK) {

				fIsModifiedMapProvider = true;

				fSelectedMapProvider = dialogMapProvider;

				// update model's
				MapProviderManager.replaceMapProvider(dialogMapProvider);
				fMapProviders = fMapProviderMgr.getAllMapProviders(true);

				updateModelFromUI();
			}

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}
	}

	private void openConfigDialogMapProfile() {

		try {

			final MPProfile mpProfile = (MPProfile) fSelectedMapProvider;

			/*
			 * update map providers in the profile to reflect renaming, adding/deleting of map
			 * providers
			 */
			mpProfile.synchronizeMPWrapper();

			// clone mapprovider
			final MPProfile dialogMapProfile = (MPProfile) mpProfile.clone();

			// it is likely that map images are downloaded
			dialogMapProfile.setStateToReloadOfflineCounter();

			final DialogMPProfile dialog = new DialogMPProfile(
					Display.getCurrent().getActiveShell(),
					this,
					dialogMapProfile);

			if (dialog.open() == Window.OK) {

				fIsModifiedMapProvider = true;

				fSelectedMapProvider = dialogMapProfile;

				// delete profile offline images, not the child images
				deleteOfflineMapFiles(dialogMapProfile);

				// update model's
				MapProviderManager.replaceMapProvider(dialogMapProfile);
				fMapProviders = fMapProviderMgr.getAllMapProviders(true);

				updateModelFromUI();
			}

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}
	}

	private void openConfigDialogWms() {

		try {

			final MPWms wmsMapProvider = (MPWms) fSelectedMapProvider;

			// load WMS caps
			if (MapProviderManager.checkWms(wmsMapProvider, null) == null) {
				return;
			}

			// enable all wms map providers that they can be selected with next/previous
			for (final MP_OLD mapProvider : fMapProviders) {

				if (mapProvider instanceof MPWms) {
					((MPWms) mapProvider).setWmsEnabled(true);
				}

				if (mapProvider instanceof MPProfile) {

					final MPProfile mpProfile = (MPProfile) mapProvider;

					for (final MapProviderWrapper mpWrapper : mpProfile.getAllWrappers()) {
						if (mpWrapper.getMapProvider() instanceof MPWms) {
							((MPWms) mpWrapper.getMapProvider()).setWmsEnabled(true);
						}
					}
				}
			}

			// clone mapprovider
			final MPWms dialogMapProvider = (MPWms) wmsMapProvider.clone();

			final DialogMP dialog = new DialogMPWms(Display.getCurrent().getActiveShell(), this, dialogMapProvider);

			if (dialog.open() == Window.OK) {

				fIsModifiedMapProvider = true;

				fSelectedMapProvider = dialogMapProvider;

				// update model
				MapProviderManager.replaceMapProvider(dialogMapProvider);
				fMapProviders = fMapProviderMgr.getAllMapProviders(true);

				updateModelFromUI();
			}

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}
	}

	@Override
	public boolean performCancel() {

		if (fForceUpdateMapProviderList == false) {

			/*
			 * check if the map provider list is modified and ask the user to save it
			 */
			if (fIsModifiedMapProviderList || fIsModifiedMapProvider) {
				if (MessageDialogNoClose.openConfirm(
						Display.getCurrent().getActiveShell(),
						Messages.pref_map_dlg_cancelModifiedMapProvider_title,
						Messages.pref_map_dlg_cancelModifiedMapProvider_message)) {

					if (fIsModifiedMapProvider) {

						// current map provider is modified

						if (updateModelFromUI() == false) {
							return false;
						}
					}

					fForceUpdateMapProviderList = true;
				}
			}
		}

		if (fForceUpdateMapProviderList) {
			saveMapProviders(true);
		}

		saveState();

		return super.performCancel();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {

			final boolean wasModified = fIsModifiedMapProvider;

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
		final boolean isReadTileInfo = fPrefStore.getBoolean(IMappingPreferences.MAP_FACTORY_IS_READ_TILE_SIZE);
//		fChkReadTileSize.setSelection(isReadTileInfo);

		if (isReadTileInfo) {
			startJobOfflineInfo(null);
		}

		/*
		 * select last selected map provider
		 */
		final String lastMapProviderId = fPrefStore
				.getString(IMappingPreferences.MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER);
		MP_OLD lastMapProvider = null;
		for (final MP_OLD mapProvider : fMapProviders) {
			if (mapProvider.getId().equals(lastMapProviderId)) {
				lastMapProvider = mapProvider;
				break;
			}
		}
		if (lastMapProvider == null) {
			fMapProviderViewer.setSelection(new StructuredSelection(fMapProviders.get(0)));
		} else {
			fMapProviderViewer.setSelection(new StructuredSelection(lastMapProvider));
		}

		// set focus to selected map provider
		final Table table = fMapProviderViewer.getTable();
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
			isSaveNeeded = fIsModifiedMapProvider || fIsModifiedMapProviderList;
		} else {

			if (fIsModifiedMapProvider) {

				isSaveMapProvider = MessageDialogNoClose.openQuestion(
						Display.getCurrent().getActiveShell(),
						Messages.pref_map_dlg_saveModifiedMapProvider_title,
						Messages.pref_map_dlg_saveModifiedMapProvider_message);

				if (isSaveMapProvider) {
					// ignore errors, errors should not happen
					updateModelFromUI();
				}
			}

			if (fIsModifiedMapProviderList && isSaveMapProvider == false) {

				isSaveOtherMapProviders = MessageDialogNoClose.openQuestion(
						Display.getCurrent().getActiveShell(),
						Messages.pref_map_dlg_saveModifiedMapProvider_title,
						Messages.pref_map_dlg_saveOtherMapProvider_message);
			}
		}

		if (fForceUpdateMapProviderList || isSaveNeeded || isSaveMapProvider || isSaveOtherMapProviders) {

			MapProviderManager.getInstance().writeMapProviderXml();

			fIsModifiedMapProviderList = false;
			fForceUpdateMapProviderList = false;

			return true;
		}

		return false;
	}

	private void saveState() {

		// offline info
//		fPrefStore.setValue(IMappingPreferences.MAP_FACTORY_IS_READ_TILE_SIZE, fChkReadTileSize.getSelection());

		// selected map provider
		if (fSelectedMapProvider != null) {
			fPrefStore.setValue(//
					IMappingPreferences.MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER,
					fSelectedMapProvider.getId());
		}
	}

	private void setEmptyMapProviderUI(final MP_OLD mapProvider) {

		fIsNewMapProvider = true;
		fNewMapProvider = mapProvider;

		fIsDisableModifyListener = true;
		{
			/*
			 * set map provider fields empty
			 */
			fTxtMapProviderName.setText(UI.EMPTY_STRING);
			fTxtMapProviderId.setText(UI.EMPTY_STRING);
			fTxtOfflineFolder.setText(UI.EMPTY_STRING);
			fTxtDescription.setText(UI.EMPTY_STRING);
			fTxtUrl.setText(UI.EMPTY_STRING);

			// map provider type
			if (mapProvider instanceof MPCustom) {
				fTxtMapProviderType.setText(Messages.Pref_Map_ProviderType_Custom);
			} else if (mapProvider instanceof MPWms) {
				fTxtMapProviderType.setText(Messages.Pref_Map_ProviderType_Wms);
			} else if (mapProvider instanceof MPProfile) {
				fTxtMapProviderType.setText(Messages.Pref_Map_ProviderType_MapProfile);
			}

			fLblOfflineFolderInfo.setText(UI.EMPTY_STRING);
			fOfflineContainer.layout(true);
		}
		fIsDisableModifyListener = false;

		enableControls();

		fTxtMapProviderName.setFocus();
	}

	private void setMapProviderModified() {

		fIsModifiedMapProvider = true;

		enableControls();
	}

	private void setWmsDropTarget(final Label label) {

		fWmsDropTarget = new DropTarget(label, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
		fWmsDropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance(), TextTransfer.getInstance() });

		fWmsDropTarget.addDropListener(new DropTargetAdapter() {
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
	private void startJobOfflineInfo(final MP_OLD updateMapProvider) {

		stopJobOfflineInfo();

		fOfflineJobMapProviders.clear();

		if (updateMapProvider == null) {

			// check if offline info is already read
			for (final MP_OLD mapProvider : fMapProviders) {
				if (mapProvider.getOfflineFileCounter() == MP_OLD.OFFLINE_INFO_NOT_READ) {
					fOfflineJobMapProviders.add(mapProvider);
				}
			}
		} else {

			fOfflineJobMapProviders.add(updateMapProvider);
		}

		if (fOfflineJobMapProviders.size() == 0) {
			// nothing to do
			return;
		}

		// check cache path
		final IPath tileCacheBasePath = getTileCachePath();
		if (tileCacheBasePath == null) {
			return;
		}

		// disable delete offline button
		fIsOfflineJobRunning = true;
		enableControls();

		// remove total tile info
		updateUIOfflineInfoTotal();

		fOfflineJobGetInfo = new Job(Messages.Pref_Map_JobName_ReadMapFactoryOfflineInfo) {

			@Override
			protected IStatus run(final IProgressMonitor monitor) {

				fIsOfflineJobCanceled = false;

				for (final MP_OLD mapProvider : fOfflineJobMapProviders) {

					final String tileOSFolder = mapProvider.getOfflineFolder();
					if (tileOSFolder == null) {
						continue;
					}

					fOfflineJobMapProvider = mapProvider;
					fOfflineJobFileCounter = 0;
					fOfflineJobFileSize = 0;
					fOfflineJobFileCounterUIUpdate = 0;

					final IPath basePath = tileCacheBasePath.addTrailingSeparator();
					boolean skipReading = false;

					File tileCacheDir = basePath.append(tileOSFolder).toFile();
					if (tileCacheDir.exists()) {
						getFilesInfo(tileCacheDir.listFiles());
					} else {
						skipReading = true;
					}

					tileCacheDir = basePath.append(MPProfile.WMS_CUSTOM_TILE_PATH).append(tileOSFolder).toFile();
					if (tileCacheDir.exists() && fIsOfflineJobCanceled == false) {
						getFilesInfo(tileCacheDir.listFiles());
					} else {
						skipReading = true;
					}

					if (skipReading) {

						// prevent reading files again

						updateUIOfflineInfo();
						continue;
					}

					if (fIsOfflineJobCanceled) {
						// set result invalid
						fOfflineJobFileCounter = MP_OLD.OFFLINE_INFO_NOT_READ;
						fOfflineJobFileSize = MP_OLD.OFFLINE_INFO_NOT_READ;
					}

					updateUIOfflineInfo();

					if (fIsOfflineJobCanceled) {
						break;
					}
				}

				fIsOfflineJobRunning = false;

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

		fOfflineJobGetInfo.schedule();
	}

	private void stopJobOfflineInfo() {

		if (fOfflineJobGetInfo == null) {
			return;
		}

		fOfflineJobGetInfo.cancel();
		fIsOfflineJobCanceled = true;

		try {
			fOfflineJobGetInfo.join();
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

		if (fIsModifiedMapProvider == false) {
			// nothing to do
			return true;
		}

		/*
		 * validate map provider fields
		 */
		final String factoryId = fTxtMapProviderId.getText().trim();
		final String factoryName = fTxtMapProviderName.getText().trim();
		final String offlineFolder = fTxtOfflineFolder.getText().trim();

		final Control errorControl = validateMapProvider(factoryName, factoryId, offlineFolder);
		if (errorControl != null) {
			return false;
		}

		/*
		 * get/create map provider
		 */
		final MP_OLD mapProvider;
		String oldFactoryId = null;
		String oldOfflineFolder = null;

		if (fIsNewMapProvider) {

			fIsNewMapProvider = false;

			// get new map provider
			mapProvider = fNewMapProvider;

			// update model
			fMapProviders.add(mapProvider);
			fMapProviderMgr.addMapProvider(mapProvider);

		} else {
			mapProvider = fSelectedMapProvider;
			oldFactoryId = mapProvider.getId();
			oldOfflineFolder = mapProvider.getOfflineFolder();
		}

		// check if offline folder has changed
		if (oldOfflineFolder != null && oldOfflineFolder.equals(offlineFolder) == false) {

			// offline folder has changed, delete files in the old offline folder

			deleteOfflineMapFiles(mapProvider);
		}

		// check if id is modified
		if (oldFactoryId != null && oldFactoryId.equals(factoryId) == false) {

			// id is modified, update all profiles with the new id

			for (final MP_OLD mp : MapProviderManager.getInstance().getAllMapProviders()) {

				if (mp instanceof MPProfile) {
					for (final MapProviderWrapper mpWrapper : ((MPProfile) mp).getAllWrappers()) {

						if (mpWrapper.getMapProviderId().equals(oldFactoryId)) {
							mpWrapper.setMapProviderId(factoryId);
						}
					}
				}
			}
		}

		// update fields
		mapProvider.setMapProviderId(factoryId);
		mapProvider.setName(factoryName);
		mapProvider.setDescription(fTxtDescription.getText().trim());
		mapProvider.setOfflineFolder(offlineFolder);

		// update viewer
		if (fIsNewMapProvider) {

			fMapProviderViewer.add(mapProvider);

		} else {
			/*
			 * !!! update must be done when a map provider was cloned !!!
			 */
			fMapProviderViewer.update(mapProvider, null);

			// do a resort because the name could be modified, this can be optimized
			fMapProviderViewer.refresh();
		}

		fIsModifiedMapProviderList = true;

		fIsModifiedMapProvider = false;
		fIsModifiedMapProviderId = false;
		fIsModifiedOfflineFolder = false;

		// select map provider in the viewer
		fMapProviderViewer.setSelection(new StructuredSelection(mapProvider), true);
		fMapProviderViewer.getTable().setFocus();

		return true;
	}

	private void updateUIOfflineInfo() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {

				// check if UI is available
				if (fMapProviderViewer.getTable().isDisposed() || fOfflineJobMapProvider == null) {
					return;
				}

				// update model
				fOfflineJobMapProvider.setOfflineFileCounter(fOfflineJobFileCounter);
				fOfflineJobMapProvider.setOfflineFileSize(fOfflineJobFileSize);

				// update viewer
				fMapProviderViewer.update(fOfflineJobMapProvider, null);

				// update info detail when the selected map provider is currently in the job
				if (fSelectedMapProvider != null && fSelectedMapProvider.equals(fOfflineJobMapProvider)) {
					updateUIOfflineInfoDetail(fSelectedMapProvider);
				}
			}
		});
	}

	/**
	 * update offline info detail
	 */
	private void updateUIOfflineInfoDetail(final MP_OLD mapProvider) {

		final int offlineTileCounter = mapProvider.getOfflineFileCounter();
		final long offlineTileSize = mapProvider.getOfflineFileSize();

		final StringBuilder sb = new StringBuilder();

		if (offlineTileCounter == MP_OLD.OFFLINE_INFO_NOT_READ) {

			sb.append(Messages.Pref_Map_Lable_NotRetrieved);

		} else if (offlineTileCounter > 0 && offlineTileSize > 0) {

			sb.append(Integer.toString(offlineTileCounter));
			sb.append(UI.SPACE);
			sb.append(Messages.Pref_Map_Lable_Files);
			sb.append(UI.DASH_WITH_SPACE);
			sb.append(fNf.format((float) offlineTileSize / 1024 / 1024));
			sb.append(UI.SPACE);
			sb.append(UI.MBYTES);

		} else {

			sb.append(Messages.Pref_Map_Lable_NotAvailable);
		}

		fLblOfflineFolderInfo.setText(sb.toString());
		fOfflineContainer.layout(true);
	}

	private void updateUIOfflineInfoTotal() {

		if (fTxtOfflineInfoTotal == null || fTxtOfflineInfoTotal.isDisposed()) {
			return;
		}

		final StringBuilder sbTotal = new StringBuilder();

		if (fMapProviders.size() > 0) {

			int tileCounter = 0;
			long tileSize = 0;
			boolean isNA = false;

			for (final MP_OLD mapProvider : fMapProviders) {
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
				sbTotal.append(fNf.format((float) tileSize / 1024 / 1024));
				sbTotal.append(UI.SPACE);
				sbTotal.append(UI.MBYTES);
			}
		}

		fTxtOfflineInfoTotal.setText(sbTotal.toString());
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
			errorControl = fTxtMapProviderName;
		}

		// check offline folder
		if (error == null) {
			error = checkOfflineFolder(offlineFolder);
			if (error != null) {
				errorControl = fTxtOfflineFolder;
			}
		}

		// check id
		if (error == null) {
			error = checkMapProviderId(mapProviderId);
			if (error != null) {
				errorControl = fTxtMapProviderId;
			}
		}

		setErrorMessage(error);

		// set validation state
		final boolean isValid = error == null;
		fIsValid = isValid;
		setValid(isValid);

		return errorControl;
	}

}
