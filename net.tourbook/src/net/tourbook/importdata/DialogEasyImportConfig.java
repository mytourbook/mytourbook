/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ComboEnumEntry;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.PageBook;

/**
 * Dialog to configure the device import.
 */
public class DialogEasyImportConfig extends TitleAreaDialog implements ITourViewer {

	private static final String				ID									= "DialogEasyImportConfig";			//$NON-NLS-1$
	//
	private static final String				STATE_BACKUP_DEVICE_HISTORY_ITEMS	= "STATE_BACKUP_DEVICE_HISTORY_ITEMS";	//$NON-NLS-1$
	private static final String				STATE_BACKUP_FOLDER_HISTORY_ITEMS	= "STATE_BACKUP_FOLDER_HISTORY_ITEMS";	//$NON-NLS-1$
	private static final String				STATE_DEVICE_DEVICE_HISTORY_ITEMS	= "STATE_DEVICE_DEVICE_HISTORY_ITEMS";	//$NON-NLS-1$
	private static final String				STATE_DEVICE_FOLDER_HISTORY_ITEMS	= "STATE_DEVICE_FOLDER_HISTORY_ITEMS";	//$NON-NLS-1$
	private static final String				STATE_IMPORT_LAUNCHER				= "STATE_IMPORT_LAUNCHER";				//$NON-NLS-1$
	//
	private static final String				DATA_KEY_TOUR_TYPE_ID				= "DATA_KEY_TOUR_TYPE_ID";				//$NON-NLS-1$
	private static final String				DATA_KEY_SPEED_TOUR_TYPE_INDEX		= "DATA_KEY_SPEED_TOUR_TYPE_INDEX";	//$NON-NLS-1$
	//
	private static final int				CONTROL_DECORATION_WIDTH			= 6;
	private static final int				VERTICAL_GROUP_DISTANCE				= 0;
	//
	private final IPreferenceStore			_prefStore							= TourbookPlugin.getPrefStore();
	private final IDialogSettings			_state								= TourbookPlugin.getState(ID);
	//
	private IPropertyChangeListener			_prefChangeListener;
	//
	private MouseWheelListener				_defaultMouseWheelListener;
	private SelectionAdapter				_defaultSelectionListener;
	private FocusListener					_folderFocusListener;
	private KeyAdapter						_folderKeyListener;
	private ModifyListener					_folderModifyListener;
	private ModifyListener					_importLauncherModifyListener;
	private SelectionAdapter				_importLauncherSelectionListener;
	private SelectionAdapter				_liveUpdateListener;
	private MouseWheelListener				_liveUpdateMouseWheelListener;
	private SelectionAdapter				_speedTourTypeListener;

	private ActionOpenPrefDialog			_actionOpenTourTypePrefs;
	private ActionSpeedTourType_Add			_actionTTSpeed_Add;
	private ActionSpeedTourType_Delete[]	_actionTTSpeed_Delete;
	private ActionSpeedTourType_Sort		_actionTTSpeed_Sort;
	//
	private PixelConverter					_pc;

	/** Model for all configs. */
	private ImportConfig					_dialogConfig;

	/** Model for the currently selected config. */
	private ImportLauncher					_currentIL;

	private RawDataView						_rawDataView;

	private TableViewer						_ilViewer;

	private ColumnManager					_columnManager;
	private TableColumnDefinition			_colDefProfileImage;
	private int								_columnIndexConfigImage;

	private ImportLauncher					_initialImportLauncher;

	private HashMap<Long, Image>			_configImages						= new HashMap<>();
	private HashMap<Long, Integer>			_configImageHash					= new HashMap<>();

	private HistoryItems					_deviceHistoryItems					= new HistoryItems();
	private HistoryItems					_backupHistoryItems					= new HistoryItems();

	private long							_dragStart;
	private int								_leftPadding;

	private boolean							_isInUIUpdate;

	private final NumberFormat				_nf1								= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}
	/*
	 * UI resources
	 */
//	private Font							_boldFont;

	/*
	 * UI controls
	 */
	private Composite						_parent;

	private Composite						_speedTourType_OuterContainer;
	private Composite						_speedTourType_Container;
	private Composite						_viewerContainer;

	private ScrolledComposite				_speedTourType_ScrolledContainer;

	private PageBook						_pagebookTourType;
	private Label							_pageTourType_NoTourType;
	private Composite						_pageTourType_OneForAll;
	private Composite						_pageTourType_BySpeed;

	private Button							_chkCreateBackup;
	private Button							_chkImportFiles;
	private Button							_chkLiveUpdate;
	private Button							_chkIL_SetLastMarker;
	private Button							_chkIL_SaveTour;
	private Button							_chkIL_SetTourType;
	private Button							_chkIL_ShowInDashboard;

	private Button							_btnSelectBackupFolder;
	private Button							_btnSelectDeviceFolder;
	private Button							_btnIL_Duplicate;
	private Button							_btnIL_New;
	private Button							_btnIL_NewOne;
	private Button							_btnIL_Remove;

	private Combo							_comboBackupFolder;
	private Combo							_comboDeviceFolder;
	private Combo							_comboIL_TourType;

	private Label							_lblBackupFolder;
	private Label							_lblLocalFolderPath;
	private Label							_lblDeviceFolder;
	private Label							_lblDeviceFolderPath;
	private Label							_lblIL_ConfigDescription;
	private Label							_lblIL_ConfigName;
	private Label							_lblIL_Hint;
	private Label							_lblIL_LastMarker;
	private Label							_lblIL_LastMarkerDistanceUnit;
	private Label							_lblIL_LastMarkerText;
	private Label							_lblIL_One_TourTypeIcon;
//	private Label							_lblIL_OtherImportActions;
	private Label[]							_lblTT_Speed_SpeedUnit;
	private Label[]							_lblTT_Speed_TourTypeIcon;

	private Link[]							_linkTT_Speed_TourType;
	private Link							_linkTT_One_TourType;

	private Spinner							_spinnerDash_AnimationCrazinessFactor;
	private Spinner							_spinnerDash_AnimationDuration;
	private Spinner							_spinnerDash_BgOpacity;
	private Spinner							_spinnerDash_NumHTiles;
	private Spinner							_spinnerDash_TileSize;
	private Spinner							_spinnerIL_LastMarkerDistance;
	private Spinner[]						_spinnerTT_Speed_AvgSpeed;

	private Text							_txtIL_ConfigDescription;
	private Text							_txtIL_ConfigName;
	private Text							_txtIL_LastMarker;

	private class ActionIL_NewOneTourType extends Action {

		private TourType	_tourType;

		/**
		 * @param tourType
		 */
		public ActionIL_NewOneTourType(final TourType tourType) {

			super(tourType.getName(), AS_CHECK_BOX);

			// show image when tour type can be selected, disabled images look ugly on win
			final Image tourTypeImage = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
			setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));

			_tourType = tourType;
		}

		@Override
		public void run() {
			onIL_AddOne(_tourType);
		}
	}

	private class ActionIL_SetOneTourType extends Action {

		private TourType	__tourType;

		/**
		 * @param tourType
		 */
		public ActionIL_SetOneTourType(final TourType tourType, final boolean isChecked) {

			super(tourType.getName(), AS_CHECK_BOX);

			if (isChecked == false) {

				// show image when tour type can be selected, disabled images look ugly on win
				final Image tourTypeImage = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
				setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));
			}

			setChecked(isChecked);
			setEnabled(isChecked == false);

			__tourType = tourType;
		}

		@Override
		public void run() {
			updateUI_OneTourType(__tourType);
		}
	}

	private class ActionSpeedTourType_Add extends Action {

		public ActionSpeedTourType_Add() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_ImportConfig_Action_AddSpeed_Tooltip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__App_Add));
		}

		@Override
		public void run() {
			onSpeedTT_Add();
		}
	}

	private class ActionSpeedTourType_Delete extends Action {

		private int	_speedTTIndex;

		public ActionSpeedTourType_Delete() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_ImportConfig_Action_RemoveSpeed_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash_Disabled));
		}

		@Override
		public void run() {
			onSpeedTT_Remove(_speedTTIndex);
		}

		public void setData(final String key, final int speedTTIndex) {

			_speedTTIndex = speedTTIndex;
		}
	}

	private class ActionSpeedTourType_SetInMenu extends Action {

		private int			_speedTTIndex;
		private TourType	_tourType;

		/**
		 * @param tourType
		 * @param speedTTIndex
		 */
		public ActionSpeedTourType_SetInMenu(final TourType tourType, final boolean isChecked, final int speedTTIndex) {

			super(tourType.getName(), AS_CHECK_BOX);

			_speedTTIndex = speedTTIndex;

			if (isChecked == false) {

				// show image when tour type can be selected, disabled images look ugly on win
				final Image tourTypeImage = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
				setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));
			}

			setChecked(isChecked);
			setEnabled(isChecked == false);

			_tourType = tourType;
		}

		@Override
		public void run() {
			onSpeedTT_SetTourType(_speedTTIndex, _tourType);
		}
	}

	private class ActionSpeedTourType_Sort extends Action {

		public ActionSpeedTourType_Sort() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_ImportConfig_Action_SortBySpeed_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__App_Sort));
			setDisabledImageDescriptor(TourbookPlugin
					.getImageDescriptor(net.tourbook.Messages.Image__App_Sort_Disabled));
		}

		@Override
		public void run() {
			onSpeedTT_Sort();
		}
	}

	private class ImportLauncherContentProvider implements IStructuredContentProvider {

		public ImportLauncherContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {

			final ArrayList<ImportLauncher> configItems = _dialogConfig.importLaunchers;

			return configItems.toArray(new ImportLauncher[configItems.size()]);
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {

		}
	}

	public DialogEasyImportConfig(	final Shell parentShell,
									final ImportConfig importConfig,
									final RawDataView rawDataView) {

		super(parentShell);

		_rawDataView = rawDataView;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__options).createImage());

		cloneImportConfig(importConfig);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// tour type images can have been changed

					_ilViewer.refresh(true);
					update_UI_From_Model_TourTypes();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * Clone original configs, only the backup will be modified in the dialog.
	 * 
	 * @param importConfig
	 */
	private void cloneImportConfig(final ImportConfig importConfig) {

		_dialogConfig = new ImportConfig();

		final ArrayList<ImportLauncher> importLaunchers = _dialogConfig.importLaunchers = new ArrayList<>();

		for (final ImportLauncher launcher : importConfig.importLaunchers) {
			importLaunchers.add(launcher.clone());
		}

		_dialogConfig.isLiveUpdate = importConfig.isLiveUpdate;

		_dialogConfig.animationCrazinessFactor = importConfig.animationCrazinessFactor;
		_dialogConfig.animationDuration = importConfig.animationDuration;
		_dialogConfig.backgroundOpacity = importConfig.backgroundOpacity;
		_dialogConfig.numHorizontalTiles = importConfig.numHorizontalTiles;
		_dialogConfig.tileSize = importConfig.tileSize;

		_dialogConfig.isCreateBackup = importConfig.isCreateBackup;
		_dialogConfig.isLastLauncherRemoved = importConfig.isLastLauncherRemoved;

		_dialogConfig.setBackupFolder(importConfig.getBackupFolder());
		_dialogConfig.setDeviceFolder(importConfig.getDeviceFolder());
	}

	@Override
	public boolean close() {

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_ImportConfig_Dialog_Title);

//		shell.addListener(SWT.Resize, new Listener() {
//			@Override
//			public void handleEvent(final Event event) {
//
//				// ensure that the dialog is not smaller than the default size
//
//				final Point shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
//
//				final Point shellSize = shell.getSize();
//
//				shellSize.x = shellSize.x < shellDefaultSize.x ? shellDefaultSize.x : shellSize.x;
//				shellSize.y = shellSize.y < shellDefaultSize.y ? shellDefaultSize.y : shellSize.y;
//
//				shell.setSize(shellSize);
//			}
//		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.Dialog_ImportConfig_Dialog_Title);
		setMessage(Messages.Dialog_ImportConfig_Dialog_Message);

		restoreState();

		_ilViewer.setInput(new Object());

		// select first config
		if (_initialImportLauncher != null) {

			final Table table = _ilViewer.getTable();

			_ilViewer.setSelection(new StructuredSelection(_initialImportLauncher));

			// ensure that the selected also has the focus, these are 2 different things
			table.setSelection(table.getSelectionIndex());
		}

		enableControls();
		enableILControls();

		// set focus
		_comboBackupFolder.setFocus();
	}

	private void createActions() {

		_actionTTSpeed_Add = new ActionSpeedTourType_Add();
		_actionTTSpeed_Sort = new ActionSpeedTourType_Sort();

		_actionOpenTourTypePrefs = new ActionOpenPrefDialog(
				Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

//		menuMgr.addMenuListener(new IMenuListener() {
//			public void menuAboutToShow(final IMenuManager menuMgr2) {
////				fillContextMenu(menuMgr2);
//			}
//		});

		final Table table = _ilViewer.getTable();
		final Menu tableContextMenu = menuMgr.createContextMenu(table);

		table.setMenu(tableContextMenu);

		_columnManager.createHeaderContextMenu(table, tableContextMenu);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		_parent = parent;

		final Composite ui = (Composite) super.createDialogArea(parent);

		initUI(ui);
		createActions();

		createUI(ui);
		createMenus();

		addPrefListener();

		return ui;
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */
	private void createMenus() {

		/*
		 * Context menu: Tour type
		 */
		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {
				fillTourTypeMenu(menuMgr);
			}
		});
		final Menu ttContextMenu = menuMgr.createContextMenu(_linkTT_One_TourType);
		_linkTT_One_TourType.setMenu(ttContextMenu);
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_30_ImportActions(container);
			createUI_50_ImportLauncher(container);
			createUI_900_Dashboard(container);
		}
	}

	private void createUI_30_ImportActions(final Composite parent) {

		final Group groupConfig = new Group(parent, SWT.NONE);
		groupConfig.setText(Messages.Dialog_ImportConfig_Group_ImportActions);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(groupConfig);
		GridLayoutFactory.swtDefaults()//
				.numColumns(2)
				.applyTo(groupConfig);
//		groupConfig.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_32_BackupFolder(groupConfig);
			createUI_34_DeviceFolder(groupConfig);
		}
	}

	private void createUI_32_BackupFolder(final Composite parent) {

		/*
		 * Checkbox: Create backup
		 */
		{
			_chkCreateBackup = new Button(parent, SWT.CHECK);
			_chkCreateBackup.setText(Messages.Dialog_ImportConfig_Checkbox_CreateBackup);
			_chkCreateBackup.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_CreateBackup_Tooltip);
			_chkCreateBackup.addSelectionListener(_defaultSelectionListener);
			_chkCreateBackup.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (_chkCreateBackup.getSelection()) {
						_comboBackupFolder.setFocus();
					}
				}
			});
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.applyTo(_chkCreateBackup);
		}

		/*
		 * Label: Local folder
		 */
		{
			_lblBackupFolder = new Label(parent, SWT.NONE);
			_lblBackupFolder.setText(Messages.Dialog_ImportConfig_Label_BackupFolder);
			_lblBackupFolder.setToolTipText(Messages.Dialog_ImportConfig_Label_BackupFolder_Tooltip);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(_leftPadding, 0)
					.applyTo(_lblBackupFolder);
		}

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.applyTo(container);
		{
			/*
			 * Combo: path
			 */
			_comboBackupFolder = new Combo(container, SWT.SINGLE | SWT.BORDER);
			_comboBackupFolder.setVisibleItemCount(44);
			_comboBackupFolder.addVerifyListener(net.tourbook.common.UI.verifyFilePathInput());
			_comboBackupFolder.addModifyListener(_folderModifyListener);
			_comboBackupFolder.addKeyListener(_folderKeyListener);
			_comboBackupFolder.addFocusListener(_folderFocusListener);
			_comboBackupFolder.setData(_backupHistoryItems);
			_comboBackupFolder.setToolTipText(Messages.Dialog_ImportConfig_Combo_Folder_Tooltip);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.indent(CONTROL_DECORATION_WIDTH, 0)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_comboBackupFolder);

			/*
			 * Button: browse...
			 */
			_btnSelectBackupFolder = new Button(container, SWT.PUSH);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_btnSelectBackupFolder);
			_btnSelectBackupFolder.setText(Messages.app_btn_browse);
			_btnSelectBackupFolder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectFolder_Backup();
				}
			});
			setButtonLayoutData(_btnSelectBackupFolder);
		}

		/*
		 * Backup folder info
		 */
		{
			// fill left column
			new Label(parent, SWT.NONE);

			/*
			 * Label: local folder absolute path
			 */
			_lblLocalFolderPath = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.indent(CONTROL_DECORATION_WIDTH + convertHorizontalDLUsToPixels(4), 0)
					.applyTo(_lblLocalFolderPath);

			_backupHistoryItems.setControls(_comboBackupFolder, _lblLocalFolderPath);
		}
	}

	private void createUI_34_DeviceFolder(final Composite parent) {

		/*
		 * Checkbox: Import tour files
		 */
		_chkImportFiles = new Button(parent, SWT.CHECK);
		_chkImportFiles.setText(Messages.Dialog_ImportConfig_Checkbox_ImportFiles);
		_chkImportFiles.addSelectionListener(_defaultSelectionListener);
		GridDataFactory.fillDefaults()//
				.span(2, 1)
				.indent(0, convertVerticalDLUsToPixels(4))
				.applyTo(_chkImportFiles);

		// this control is just for info and to have a consistent UI
		_chkImportFiles.setSelection(true);
		_chkImportFiles.setEnabled(false);

		/*
		 * Label: device folder
		 */
		_lblDeviceFolder = new Label(parent, SWT.NONE);
		_lblDeviceFolder.setText(Messages.Dialog_ImportConfig_Label_DeviceFolder);
		_lblDeviceFolder.setToolTipText(Messages.Dialog_ImportConfig_Label_DeviceFolder_Tooltip);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.indent(_leftPadding, 0)
				.applyTo(_lblDeviceFolder);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
//				.extendedMargins(-CONTROL_DECORATION_WIDTH, 0, 0, 0)
				.applyTo(container);
		{
			/*
			 * Combo: path
			 */
			_comboDeviceFolder = new Combo(container, SWT.SINGLE | SWT.BORDER);
			_comboDeviceFolder.setVisibleItemCount(44);
			_comboDeviceFolder.addVerifyListener(net.tourbook.common.UI.verifyFilePathInput());
			_comboDeviceFolder.addModifyListener(_folderModifyListener);
			_comboDeviceFolder.addKeyListener(_folderKeyListener);
			_comboDeviceFolder.addFocusListener(_folderFocusListener);
			_comboDeviceFolder.setData(_deviceHistoryItems);
			_comboDeviceFolder.setToolTipText(Messages.Dialog_ImportConfig_Combo_Folder_Tooltip);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.indent(CONTROL_DECORATION_WIDTH, 0)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_comboDeviceFolder);

			/*
			 * Button: browse...
			 */
			_btnSelectDeviceFolder = new Button(container, SWT.PUSH);
			_btnSelectDeviceFolder.setText(Messages.app_btn_browse);
			_btnSelectDeviceFolder.setData(_comboDeviceFolder);
			_btnSelectDeviceFolder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectFolder_Device();
				}
			});
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_btnSelectDeviceFolder);
			setButtonLayoutData(_btnSelectDeviceFolder);
		}

		/*
		 * Backup folder info
		 */
		{
			// fill left column
			new Label(parent, SWT.NONE);

			/*
			 * Label: device folder absolute path
			 */
			_lblDeviceFolderPath = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.indent(CONTROL_DECORATION_WIDTH + convertHorizontalDLUsToPixels(4), 0)
					.applyTo(_lblDeviceFolderPath);

			_deviceHistoryItems.setControls(_comboDeviceFolder, _lblDeviceFolderPath);
		}
	}

	private void createUI_50_ImportLauncher(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.indent(0, VERTICAL_GROUP_DISTANCE)
				.applyTo(group);
		GridLayoutFactory.swtDefaults()//
				.numColumns(3)
				.applyTo(group);
		group.setText(Messages.Dialog_ImportConfig_Group_ImportLauncher);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			final Label label = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.applyTo(label);
			label.setText(Messages.Dialog_ImportConfig_Label_ImportLauncher);

			createUI_51_IL_Viewer(group);
			createUI_53_IL_Actions(group);
			createUI_600_IL_Detail(group);

			createUI_59_DragDropHint(group);
		}
	}

	private void createUI_51_IL_Viewer(final Composite parent) {

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(SWT.DEFAULT, convertHeightInCharsToPixels(20))
				.applyTo(_viewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
//		_viewerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_52_IL_ViewerTable(_viewerContainer);
		}
	}

	private void createUI_52_IL_ViewerTable(final Composite parent) {

		/*
		 * Create tree
		 */
		final Table table = new Table(parent, //
				SWT.H_SCROLL //
						| SWT.V_SCROLL
						| SWT.BORDER
						| SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		table.setHeaderVisible(true);

		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
		 * critical for performance that these methods be as efficient as possible.
		 */
		final Listener paintListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {

				if (event.type == SWT.MeasureItem || event.type == SWT.PaintItem) {
					onPaintViewer(event);
				}
			}
		};
		table.addListener(SWT.MeasureItem, paintListener);
		table.addListener(SWT.PaintItem, paintListener);

		/*
		 * Create tree viewer
		 */
		_ilViewer = new TableViewer(table);

		_columnManager.createColumns(_ilViewer);

		_columnIndexConfigImage = _colDefProfileImage.getCreateIndex();

		_ilViewer.setUseHashlookup(true);
		_ilViewer.setContentProvider(new ImportLauncherContentProvider());

		_ilViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectIL(event.getSelection());
			}
		});

		_ilViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				onIL_DblClick();
			}
		});

		createContextMenu();

		/*
		 * set drag adapter
		 */
		_ilViewer.addDragSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceListener() {

					@Override
					public void dragFinished(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

						if (event.doit == false) {
							return;
						}

						transfer.setSelection(null);
						transfer.setSelectionSetTime(0);
					}

					@Override
					public void dragSetData(final DragSourceEvent event) {
						// data are set in LocalSelectionTransfer
					}

					@Override
					public void dragStart(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
						final ISelection selection = _ilViewer.getSelection();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(_dragStart = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		/*
		 * set drop adapter
		 */
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_ilViewer) {

			private Widget	_tableItem;

			@Override
			public void dragOver(final DropTargetEvent dropEvent) {

				// keep table item
				_tableItem = dropEvent.item;

				super.dragOver(dropEvent);
			}

			@Override
			public boolean performDrop(final Object data) {

				if (data instanceof StructuredSelection) {
					final StructuredSelection selection = (StructuredSelection) data;

					if (selection.getFirstElement() instanceof ImportLauncher) {

						final ImportLauncher filterItem = (ImportLauncher) selection.getFirstElement();

						final int location = getCurrentLocation();
						final Table filterTable = _ilViewer.getTable();

						/*
						 * check if drag was startet from this filter, remove the filter item before
						 * the new filter is inserted
						 */
						if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dragStart) {
							_ilViewer.remove(filterItem);
						}

						int filterIndex;

						if (_tableItem == null) {

							_ilViewer.add(filterItem);
							filterIndex = filterTable.getItemCount() - 1;

						} else {

							// get index of the target in the table
							filterIndex = filterTable.indexOf((TableItem) _tableItem);
							if (filterIndex == -1) {
								return false;
							}

							if (location == LOCATION_BEFORE) {
								_ilViewer.insert(filterItem, filterIndex);
							} else if (location == LOCATION_AFTER || location == LOCATION_ON) {
								_ilViewer.insert(filterItem, ++filterIndex);
							}
						}

						// reselect filter item
						_ilViewer.setSelection(new StructuredSelection(filterItem));

						// set focus to selection
						filterTable.setSelection(filterIndex);
						filterTable.setFocus();

						return true;
					}
				}

				return false;
			}

			@Override
			public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

				final ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
				if (selection instanceof StructuredSelection) {
					final Object dragFilter = ((StructuredSelection) selection).getFirstElement();
					if (target == dragFilter) {
						return false;
					}
				}

				if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType) == false) {
					return false;
				}

				return true;
			}

		};

		_ilViewer.addDropSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);
	}

	private void createUI_53_IL_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * Button: New one tour type
			 */
			_btnIL_NewOne = new Button(container, SWT.NONE);
			_btnIL_NewOne.setImage(//
					net.tourbook.ui.UI.getInstance().getTourTypeImage(TourType.IMAGE_KEY_DIALOG_SELECTION));
			_btnIL_NewOne.setText(Messages.Dialog_ImportConfig_Action_NewOneTourType);
			_btnIL_NewOne.setToolTipText(Messages.Dialog_ImportConfig_Action_NewOneTourType_Tooltip);
			_btnIL_NewOne.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					UI.openControlMenu(_btnIL_NewOne);
				}
			});
			setButtonLayoutData(_btnIL_NewOne);

			/*
			 * Context menu: Tour type
			 */
			final MenuManager menuMgr = new MenuManager();
			menuMgr.setRemoveAllWhenShown(true);
			menuMgr.addMenuListener(new IMenuListener() {
				@Override
				public void menuAboutToShow(final IMenuManager menuMgr) {
					fillTourTypeOneMenu(menuMgr);
				}
			});
			final Menu ttContextMenu = menuMgr.createContextMenu(_btnIL_NewOne);
			_btnIL_NewOne.setMenu(ttContextMenu);

			/*
			 * Button: New
			 */
			_btnIL_New = new Button(container, SWT.NONE);
			_btnIL_New.setText(Messages.App_Action_New);
			_btnIL_New.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onIL_Add(false);
				}
			});
			setButtonLayoutData(_btnIL_New);

			/*
			 * Button: Duplicate
			 */
			_btnIL_Duplicate = new Button(container, SWT.NONE);
			_btnIL_Duplicate.setText(Messages.App_Action_Duplicate);
			_btnIL_Duplicate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onIL_Add(true);
				}
			});
			setButtonLayoutData(_btnIL_Duplicate);

			/*
			 * button: remove
			 */
			_btnIL_Remove = new Button(container, SWT.NONE);
			_btnIL_Remove.setText(Messages.App_Action_Remove_Immediate);
			_btnIL_Remove.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onIL_Remove();
				}
			});
			setButtonLayoutData(_btnIL_Remove);

			// align to the end
			final GridData gd = (GridData) _btnIL_Remove.getLayoutData();
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = SWT.END;
		}
	}

	private void createUI_59_DragDropHint(final Composite parent) {

		_lblIL_Hint = new Label(parent, SWT.WRAP);
		_lblIL_Hint.setText(Messages.Dialog_ImportConfig_Info_ConfigDragDrop);
		GridDataFactory.fillDefaults()//
				.span(3, 1)
				.applyTo(_lblIL_Hint);
	}

	private void createUI_600_IL_Detail(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_ImportConfig_Group_ImportLauncherConfig);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.indent(convertWidthInCharsToPixels(3), 0)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI_610_IL_Name(group);
			createUI_800_IL_TourType(group);
			createUI_620_IL_LastMarker(group);
			createUI_699_IL_Save(group);
		}
	}

	private void createUI_610_IL_Name(final Composite parent) {

		{
			/*
			 * Config name
			 */

			// label
			_lblIL_ConfigName = new Label(parent, SWT.NONE);
			_lblIL_ConfigName.setText(Messages.Dialog_ImportConfig_Label_ConfigName);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblIL_ConfigName);

			// text
			_txtIL_ConfigName = new Text(parent, SWT.BORDER);
			_txtIL_ConfigName.addModifyListener(_importLauncherModifyListener);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.applyTo(_txtIL_ConfigName);
		}

		{
			/*
			 * Config description
			 */

			// label
			_lblIL_ConfigDescription = new Label(parent, SWT.NONE);
			_lblIL_ConfigDescription.setText(Messages.Dialog_ImportConfig_Label_ConfigDescription);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_lblIL_ConfigDescription);

			// text
			_txtIL_ConfigDescription = new Text(parent, //
					SWT.BORDER | //
							SWT.WRAP
							| SWT.MULTI
							| SWT.V_SCROLL
							| SWT.H_SCROLL);
			_txtIL_ConfigDescription.addModifyListener(_importLauncherModifyListener);

			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.hint(convertWidthInCharsToPixels(40), convertHeightInCharsToPixels(2))
					.applyTo(_txtIL_ConfigDescription);
		}

//		{
//			/*
//			 * Label: Other import actions
//			 */
//			_lblIL_OtherImportActions = new Label(parent, SWT.NONE);
//			_lblIL_OtherImportActions.setText(Messages.Dialog_ImportConfig_Info_MoreImportActions);
//			_lblIL_OtherImportActions.setFont(_boldFont);
//			GridDataFactory.fillDefaults()//
//					.span(2, 1)
//					.indent(0, 10)
//					.applyTo(_lblIL_OtherImportActions);
//		}
	}

	private void createUI_620_IL_LastMarker(final Composite parent) {

		final SelectionAdapter lastMarkerSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				onIL_Modified();
				enableILControls();
			}
		};

		final MouseWheelListener lastMarkerMouseWheelListener = new MouseWheelListener() {

			@Override
			public void mouseScrolled(final MouseEvent e) {
				UI.adjustSpinnerValueOnMouseScroll(e);

				onIL_Modified();
				enableILControls();
			}
		};

		{
			/*
			 * Checkbox: Last marker
			 */
			_chkIL_SetLastMarker = new Button(parent, SWT.CHECK);
			_chkIL_SetLastMarker.setText(Messages.Dialog_ImportConfig_Checkbox_LastMarker);
			_chkIL_SetLastMarker.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_LastMarker_Tooltip);
			_chkIL_SetLastMarker.addSelectionListener(lastMarkerSelectionListener);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.indent(0, 5)
					.applyTo(_chkIL_SetLastMarker);
		}

		{
			/*
			 * Last marker distance
			 */
			// label
			_lblIL_LastMarker = new Label(parent, SWT.NONE);
			_lblIL_LastMarker.setText(Messages.Dialog_ImportConfig_Label_LastMarkerDistance);
			_lblIL_LastMarker.setToolTipText(Messages.Dialog_ImportConfig_Label_LastMarkerDistance_Tooltip);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(_leftPadding, 0)
					.applyTo(_lblIL_LastMarker);

			final Composite container = new Composite(parent, SWT.NONE);
//			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				// spinner: distance 1.1 km
				_spinnerIL_LastMarkerDistance = new Spinner(container, SWT.BORDER);
				_spinnerIL_LastMarkerDistance.setMinimum(ImportConfig.LAST_MARKER_DISTANCE_MIN);
				_spinnerIL_LastMarkerDistance.setMaximum(ImportConfig.LAST_MARKER_DISTANCE_MAX);
				_spinnerIL_LastMarkerDistance.setDigits(1);
				_spinnerIL_LastMarkerDistance.addMouseWheelListener(lastMarkerMouseWheelListener);
				_spinnerIL_LastMarkerDistance.addSelectionListener(lastMarkerSelectionListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerIL_LastMarkerDistance);

				// label: unit
				_lblIL_LastMarkerDistanceUnit = new Label(container, SWT.NONE);
				_lblIL_LastMarkerDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblIL_LastMarkerDistanceUnit);
			}
		}

		{
			/*
			 * Marker text
			 */

			// label
			_lblIL_LastMarkerText = new Label(parent, SWT.NONE);
			_lblIL_LastMarkerText.setText(Messages.Dialog_ImportConfig_Label_LastMarkerText);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(_leftPadding, 0)
					.applyTo(_lblIL_LastMarkerText);

			// text
			_txtIL_LastMarker = new Text(parent, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.applyTo(_txtIL_LastMarker);
		}
	}

	private void createUI_699_IL_Save(final Composite parent) {

		{
			/*
			 * Checkbox: Save
			 */
			_chkIL_SaveTour = new Button(parent, SWT.CHECK);
			_chkIL_SaveTour.setText(Messages.Dialog_ImportConfig_Checkbox_SaveTour);
			_chkIL_SaveTour.addSelectionListener(_importLauncherSelectionListener);
			_chkIL_SaveTour.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_SaveTour_Tooltip);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.indent(0, 5)
					.applyTo(_chkIL_SaveTour);
		}
		{
			/*
			 * Checkbox: Show in dashboard
			 */
			_chkIL_ShowInDashboard = new Button(parent, SWT.CHECK);
			_chkIL_ShowInDashboard.setText(Messages.Dialog_ImportConfig_Checkbox_ShowInDashboard);
			_chkIL_ShowInDashboard.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_ShowInDashboard_Tooltip);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.applyTo(_chkIL_ShowInDashboard);
		}
	}

	private void createUI_800_IL_TourType(final Composite parent) {

		final SelectionAdapter ttListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectTourType();
			}
		};

		/*
		 * Checkbox: Set tour type
		 */
		_chkIL_SetTourType = new Button(parent, SWT.CHECK);
		_chkIL_SetTourType.setText(Messages.Dialog_ImportConfig_Checkbox_TourType);
		_chkIL_SetTourType.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_TourType_Tooltip);
		_chkIL_SetTourType.addSelectionListener(ttListener);
		GridDataFactory.fillDefaults()//
				.span(2, 1)
				.indent(0, 5)
				.applyTo(_chkIL_SetTourType);

		/*
		 * Tour type options
		 */
		{
			// combo
			_comboIL_TourType = new Combo(parent, SWT.READ_ONLY);
			_comboIL_TourType.addSelectionListener(ttListener);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.align(SWT.BEGINNING, SWT.FILL)
					.indent(_leftPadding, 0)
					.applyTo(_comboIL_TourType);

			// fill combo
			for (final ComboEnumEntry<?> tourTypeItem : RawDataManager.ALL_IMPORT_TOUR_TYPE_CONFIG) {
				_comboIL_TourType.add(tourTypeItem.label);
			}

			// options
			_pagebookTourType = new PageBook(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.span(2, 1)
					.indent(_leftPadding, 0)
					.hint(SWT.DEFAULT, convertHeightInCharsToPixels(10))
					.applyTo(_pagebookTourType);
			{
				_pageTourType_NoTourType = createUI_810_Page_NoTourType(_pagebookTourType);
				_pageTourType_OneForAll = createUI_820_Page_OneForAll(_pagebookTourType);
				_pageTourType_BySpeed = createUI_830_Page_BySpeed(_pagebookTourType);
			}
		}
	}

	/**
	 * Page: Not used
	 * 
	 * @return
	 */
	private Label createUI_810_Page_NoTourType(final PageBook parent) {

		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(UI.EMPTY_STRING);

		return label;
	}

	private Composite createUI_820_Page_OneForAll(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			_lblIL_One_TourTypeIcon = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.hint(16, 16)
					.applyTo(_lblIL_One_TourTypeIcon);
			_lblIL_One_TourTypeIcon.setText(UI.EMPTY_STRING);

			/*
			 * tour type
			 */
			_linkTT_One_TourType = new Link(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_linkTT_One_TourType);
			_linkTT_One_TourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
			_linkTT_One_TourType.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					net.tourbook.common.UI.openControlMenu(_linkTT_One_TourType);
				}
			});
		}

		return container;
	}

	private Composite createUI_830_Page_BySpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{

			createUI_832_SpeedTourType_Actions(container);
			createUI_834_SpeedTourTypes(container);
		}

		return container;
	}

	private void createUI_832_SpeedTourType_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_actionTTSpeed_Add);
		tbm.add(_actionTTSpeed_Sort);

		tbm.update(true);
	}

	private void createUI_834_SpeedTourTypes(final Composite parent) {

		/*
		 * Speed tour type fields container
		 */
		_speedTourType_OuterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(_speedTourType_OuterContainer);

		GridLayoutFactory.fillDefaults().applyTo(_speedTourType_OuterContainer);

		createUI_835_SpeedTourType_Fields();
	}

	/**
	 * Create the speed tour type fields from a list.
	 * 
	 * @param parent
	 */
	private void createUI_835_SpeedTourType_Fields() {

		if (_currentIL == null) {

			updateUI_ClearSpeedTourTypes();

			return;
		}

		final int speedTTSize = _currentIL.speedTourTypes.size();

		// check if required fields are already available
		if (_spinnerTT_Speed_AvgSpeed != null && _spinnerTT_Speed_AvgSpeed.length == speedTTSize) {
			return;
		}

		Point scrollOrigin = null;

		// dispose previous content
		if (_speedTourType_ScrolledContainer != null) {

			// get current scroll position
			scrollOrigin = _speedTourType_ScrolledContainer.getOrigin();

			_speedTourType_ScrolledContainer.dispose();
		}

		_speedTourType_Container = createUI_836_SpeedTourType_ScrolledContainer(_speedTourType_OuterContainer);

		/*
		 * fields
		 */
		_actionTTSpeed_Delete = new ActionSpeedTourType_Delete[speedTTSize];
		_lblTT_Speed_TourTypeIcon = new Label[speedTTSize];
		_lblTT_Speed_SpeedUnit = new Label[speedTTSize];
		_linkTT_Speed_TourType = new Link[speedTTSize];
		_spinnerTT_Speed_AvgSpeed = new Spinner[speedTTSize];

		_speedTourType_Container.setRedraw(false);
		{
			for (int speedTTIndex = 0; speedTTIndex < speedTTSize; speedTTIndex++) {

				/*
				 * Spinner: Speed value
				 */
				final Spinner spinnerValue = new Spinner(_speedTourType_Container, SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(spinnerValue);
				spinnerValue.setMinimum(ImportConfig.TOUR_TYPE_AVG_SPEED_MIN);
				spinnerValue.setMaximum(ImportConfig.TOUR_TYPE_AVG_SPEED_MAX);
				spinnerValue.setToolTipText(Messages.Dialog_ImportConfig_Spinner_Speed_Tooltip);
				spinnerValue.addMouseWheelListener(_defaultMouseWheelListener);

				/*
				 * Label: Speed unit
				 */
				final Label lblUnit = new Label(_speedTourType_Container, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(lblUnit);
				lblUnit.setText(UI.UNIT_LABEL_SPEED);

				/*
				 * Label with icon: Tour type (CLabel cannot be disabled !!!)
				 */
				final Label lblTourTypeIcon = new Label(_speedTourType_Container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.hint(16, 16)
						.applyTo(lblTourTypeIcon);

				/*
				 * Link: Tour type
				 */
				final Link linkTourType = new Link(_speedTourType_Container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(linkTourType);
				linkTourType.setText(Messages.tour_editor_label_tour_type);
				linkTourType.addSelectionListener(_speedTourTypeListener);

				/*
				 * Context menu: Tour type
				 */
				final MenuManager menuMgr = new MenuManager();
				menuMgr.setRemoveAllWhenShown(true);
				menuMgr.addMenuListener(new IMenuListener() {
					@Override
					public void menuAboutToShow(final IMenuManager menuMgr) {
						fillSpeedTourTypeMenu(menuMgr, linkTourType);
					}
				});
				final Menu ttContextMenu = menuMgr.createContextMenu(linkTourType);
				linkTourType.setMenu(ttContextMenu);

				/*
				 * Action: Delete speed tour type
				 */
				final ActionSpeedTourType_Delete actionDeleteSpeedTT = new ActionSpeedTourType_Delete();
				createUI_ActionButton(_speedTourType_Container, actionDeleteSpeedTT);

				/*
				 * Keep controls
				 */
				_actionTTSpeed_Delete[speedTTIndex] = actionDeleteSpeedTT;
				_lblTT_Speed_TourTypeIcon[speedTTIndex] = lblTourTypeIcon;
				_lblTT_Speed_SpeedUnit[speedTTIndex] = lblUnit;
				_linkTT_Speed_TourType[speedTTIndex] = linkTourType;
				_spinnerTT_Speed_AvgSpeed[speedTTIndex] = spinnerValue;
			}
		}
		_speedTourType_Container.setRedraw(true);

		_speedTourType_OuterContainer.layout(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_speedTourType_ScrolledContainer.setOrigin(scrollOrigin);
		}
	}

	private Composite createUI_836_SpeedTourType_ScrolledContainer(final Composite parent) {

		// scrolled container
		_speedTourType_ScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_speedTourType_ScrolledContainer);
		_speedTourType_ScrolledContainer.setExpandVertical(true);
		_speedTourType_ScrolledContainer.setExpandHorizontal(true);

		// container
		final Composite speedTTContainer = new Composite(_speedTourType_ScrolledContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(speedTTContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(5)
				.applyTo(speedTTContainer);

		_speedTourType_ScrolledContainer.setContent(speedTTContainer);
		_speedTourType_ScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_speedTourType_ScrolledContainer.setMinSize(speedTTContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		return speedTTContainer;
	}

	private void createUI_900_Dashboard(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_ImportConfig_Group_Dashboard);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.indent(0, VERTICAL_GROUP_DISTANCE)
				.applyTo(group);
		GridLayoutFactory.swtDefaults()//
				.numColumns(3)
				.spacing(30, 5)
				.applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{

			createUI_902_Dashboard_1(group);
			createUI_904_Dashboard_2(group);
			createUI_906_Dashboard_3(group);

			createUI_999_Dashboard_LiveUpdate(group);
		}
	}

	private void createUI_902_Dashboard_1(final Group parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Tile size
				 */
				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Dialog_ImportConfig_Label_ConfigTileSize);
				label.setToolTipText(Messages.Dialog_ImportConfig_Label_ConfigTileSize_Tooltip);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				// spinner
				_spinnerDash_TileSize = new Spinner(container, SWT.BORDER);
				_spinnerDash_TileSize.setMinimum(ImportConfig.TILE_SIZE_MIN);
				_spinnerDash_TileSize.setMaximum(ImportConfig.TILE_SIZE_MAX);
				_spinnerDash_TileSize.addSelectionListener(_liveUpdateListener);
				_spinnerDash_TileSize.addMouseWheelListener(_liveUpdateMouseWheelListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerDash_TileSize);
			}
			{
				/*
				 * Number of columns
				 */
				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Dialog_ImportConfig_Label_ImportColumns);
				label.setToolTipText(Messages.Dialog_ImportConfig_Label_ImportColumns_Tooltip);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				// spinner
				_spinnerDash_NumHTiles = new Spinner(container, SWT.BORDER);
				_spinnerDash_NumHTiles.setMinimum(ImportConfig.HORIZONTAL_TILES_MIN);
				_spinnerDash_NumHTiles.setMaximum(ImportConfig.HORIZONTAL_TILES_MAX);
				_spinnerDash_NumHTiles.addSelectionListener(_liveUpdateListener);
				_spinnerDash_NumHTiles.addMouseWheelListener(_liveUpdateMouseWheelListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerDash_NumHTiles);
			}
		}
	}

	private void createUI_904_Dashboard_2(final Group parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Animation duration
				 */
				// label
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Dialog_ImportConfig_Label_AnimationDuration);
				label.setToolTipText(Messages.Dialog_ImportConfig_Label_AnimationDuration_Tooltip);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				// spinner
				_spinnerDash_AnimationDuration = new Spinner(container, SWT.BORDER);
				_spinnerDash_AnimationDuration.setMinimum(ImportConfig.ANIMATION_DURATION_MIN);
				_spinnerDash_AnimationDuration.setMaximum(ImportConfig.ANIMATION_DURATION_MAX);
				_spinnerDash_AnimationDuration.setDigits(1);
				_spinnerDash_AnimationDuration.addSelectionListener(_liveUpdateListener);
				_spinnerDash_AnimationDuration.addMouseWheelListener(_liveUpdateMouseWheelListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerDash_AnimationDuration);

				// label
				label = new Label(container, SWT.NONE);
				label.setText(Messages.App_Unit_Seconds_Small);
			}
			{
				/*
				 * Animation crazy factor
				 */
				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Dialog_ImportConfig_Label_AnimationCrazyFactor);
				label.setToolTipText(Messages.Dialog_ImportConfig_Label_AnimationCrazyFactor_Tooltip);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				// spinner
				_spinnerDash_AnimationCrazinessFactor = new Spinner(container, SWT.BORDER);
				_spinnerDash_AnimationCrazinessFactor.setMinimum(ImportConfig.ANIMATION_CRAZINESS_FACTOR_MIN);
				_spinnerDash_AnimationCrazinessFactor.setMaximum(ImportConfig.ANIMATION_CRAZINESS_FACTOR_MAX);
				_spinnerDash_AnimationCrazinessFactor.addSelectionListener(_liveUpdateListener);
				_spinnerDash_AnimationCrazinessFactor.addMouseWheelListener(_liveUpdateMouseWheelListener);
				GridDataFactory
						.fillDefaults()
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_spinnerDash_AnimationCrazinessFactor);

				// fill column
				new Label(container, SWT.NONE);
			}
		}
	}

	private void createUI_906_Dashboard_3(final Group parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Background opacity
				 */
				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Dialog_ImportConfig_Label_BackgroundOpacity);
				label.setToolTipText(Messages.Dialog_ImportConfig_Label_BackgroundOpacity_Tooltip);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				// spinner
				_spinnerDash_BgOpacity = new Spinner(container, SWT.BORDER);
				_spinnerDash_BgOpacity.setMinimum(ImportConfig.BACKGROUND_OPACITY_MIN);
				_spinnerDash_BgOpacity.setMaximum(ImportConfig.BACKGROUND_OPACITY_MAX);
				_spinnerDash_BgOpacity.addSelectionListener(_liveUpdateListener);
				_spinnerDash_BgOpacity.addMouseWheelListener(_liveUpdateMouseWheelListener);
			}
		}
	}

	private void createUI_999_Dashboard_LiveUpdate(final Composite parent) {

		/*
		 * Checkbox: live update
		 */
		_chkLiveUpdate = new Button(parent, SWT.CHECK);
		_chkLiveUpdate.setText(Messages.Dialog_ImportConfig_Checkbox_LiveUpdate);
		_chkLiveUpdate.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_LiveUpdate_Tooltip);
		_chkLiveUpdate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				doLiveUpdate();
			}
		});
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.FILL)
				.span(3, 1)
				.applyTo(_chkLiveUpdate);
	}

	/**
	 * Creates an action in a toolbar.
	 * 
	 * @param parent
	 * @param action
	 */
	private void createUI_ActionButton(final Composite parent, final Action action) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		final ToolBarManager tbm = new ToolBarManager(toolbar);
		tbm.add(action);
		tbm.update(true);
	}

	private void defineAllColumns() {

		defineColumn_10_ProfileName();
		defineColumn_20_ColorImage();
		defineColumn_30_IsSaveTour();
		defineColumn_40_LastMarkerDistance();
		defineColumn_90_Description();
	}

	/**
	 * Column: Item name
	 */
	private void defineColumn_10_ProfileName() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "configName", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Name);
		colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Name);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(17));
		colDef.setColumnWeightData(new ColumnWeightData(17));

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(((ImportLauncher) cell.getElement()).name);
			}
		});
	}

	/**
	 * Column: Tour type
	 */
	private void defineColumn_20_ColorImage() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "colorImage", SWT.LEAD); //$NON-NLS-1$
		_colDefProfileImage = colDef;

		colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_TourType);
		colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_TourType);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));
		colDef.setColumnWeightData(new ColumnWeightData(12));

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);

		colDef.setLabelProvider(new CellLabelProvider() {

			// !!! set dummy label provider, otherwise an error occures !!!
			@Override
			public void update(final ViewerCell cell) {}
		});
	}

	/**
	 * Column: Is save tour
	 */
	private void defineColumn_30_IsSaveTour() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isSaveTour", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Save_Label);
		colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Save_Header);
		colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Checkbox_SaveTour_Tooltip);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(7));
		colDef.setColumnWeightData(new ColumnWeightData(7));

		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final ImportLauncher importLauncher = (ImportLauncher) cell.getElement();
				cell.setText(importLauncher.isSaveTour //
						? Messages.App_Label_BooleanYes
						: UI.EMPTY_STRING);
			}
		});
	}

	/**
	 * Column: Set last marker
	 */
	private void defineColumn_40_LastMarkerDistance() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isSetLastMarker", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_LastMarker_Lable);
		colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_LastMarker_Header);
		colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Column_LastMarker_Tooltip);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(7));
		colDef.setColumnWeightData(new ColumnWeightData(7));

		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final ImportLauncher importLauncher = (ImportLauncher) cell.getElement();

				if (importLauncher.isSetLastMarker) {

					final double distance = getMarkerDistanceValue(importLauncher);

					cell.setText(_nf1.format(distance));

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * Column: Item description
	 */
	private void defineColumn_90_Description() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "configDescription", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Description);
		colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Description);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(25));
		colDef.setColumnWeightData(new ColumnWeightData(25));

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(((ImportLauncher) cell.getElement()).description);
			}
		});
	}

	private void disposeConfigImages() {

		for (final Image configImage : _configImages.values()) {

			if (configImage != null) {
				configImage.dispose();
			}
		}

		_configImages.clear();
		_configImageHash.clear();
	}

	/**
	 * Do live update for this feature.
	 */
	private void doLiveUpdate() {

		final boolean isLiveUpdate = _chkLiveUpdate.getSelection();
		if (isLiveUpdate) {

			update_Model_From_UI_LiveUpdateValues();

			_rawDataView.doLiveUpdate(this);

		} else {

			// update model that live update is disabled

			_dialogConfig.isLiveUpdate = isLiveUpdate;
		}
	}

	private void enableControls() {

		final boolean isBackup = _chkCreateBackup.getSelection();

		_lblBackupFolder.setEnabled(isBackup);
		_comboBackupFolder.setEnabled(isBackup);
		_btnSelectBackupFolder.setEnabled(isBackup);

		_backupHistoryItems.setIsValidateFolder(isBackup);
		_backupHistoryItems.validateModifiedPath();
	}

	private void enableILControls() {

		final boolean isLauncherAvailable = _dialogConfig.importLaunchers.size() > 0;

		final boolean isILSelected = _currentIL != null;

		final boolean isLastMarkerSelected = isILSelected && _chkIL_SetLastMarker.getSelection();
		boolean isSetTourType = false;

		if (isILSelected) {

			if (isLauncherAvailable) {

				final Enum<TourTypeConfig> selectedTourTypeConfig = getSelectedTourTypeConfig();

				if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(selectedTourTypeConfig)) {

					isSetTourType = true;

					if (_actionTTSpeed_Delete != null) {

						for (final ActionSpeedTourType_Delete action : _actionTTSpeed_Delete) {
							action.setEnabled(isILSelected);
						}

						for (final Spinner spinner : _spinnerTT_Speed_AvgSpeed) {
							spinner.setEnabled(isILSelected);
						}

						for (final Link link : _linkTT_Speed_TourType) {
							link.setEnabled(isILSelected);
						}

						for (final Label label : _lblTT_Speed_SpeedUnit) {
							label.setEnabled(isILSelected);
						}

						for (final Label label : _lblTT_Speed_TourTypeIcon) {

							if (isILSelected) {

								final Integer speedTTIndex = (Integer) label.getData(DATA_KEY_SPEED_TOUR_TYPE_INDEX);

								final SpeedTourType speedTT = _currentIL.speedTourTypes.get(speedTTIndex);
								final long tourTypeId = speedTT.tourTypeId;

								label.setImage(net.tourbook.ui.UI.getInstance().getTourTypeImage(tourTypeId));

							} else {

								// the disabled image looks very ugly
								label.setImage(null);
							}
						}
					}

					_actionTTSpeed_Add.setEnabled(isILSelected);
					_actionTTSpeed_Sort.setEnabled(isILSelected && _spinnerTT_Speed_AvgSpeed.length > 1);

				} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(selectedTourTypeConfig)) {

					isSetTourType = true;

					_linkTT_One_TourType.setEnabled(isILSelected);
				}
			}
		}

		if ((isILSelected && isSetTourType) == false) {

			// a tour type is not selected, hide tour type page
			showTourTypePage(null);
		}

		_btnIL_Duplicate.setEnabled(isILSelected);
		_btnIL_Remove.setEnabled(isILSelected);

		_chkIL_SetLastMarker.setEnabled(isILSelected);
		_chkIL_SaveTour.setEnabled(isILSelected);
		_chkIL_ShowInDashboard.setEnabled(isILSelected);
		_chkIL_SetTourType.setEnabled(isILSelected);

		_comboIL_TourType.setEnabled(isILSelected && isSetTourType);

		_lblIL_ConfigName.setEnabled(isILSelected);
		_lblIL_ConfigDescription.setEnabled(isILSelected);
		_lblIL_Hint.setEnabled(isLauncherAvailable);
		_lblIL_LastMarker.setEnabled(isLastMarkerSelected);
		_lblIL_LastMarkerDistanceUnit.setEnabled(isLastMarkerSelected);
		_lblIL_LastMarkerText.setEnabled(isLastMarkerSelected);
//		_lblIL_OtherImportActions.setEnabled(isILSelected);

		_spinnerIL_LastMarkerDistance.setEnabled(isLastMarkerSelected);

		_txtIL_ConfigName.setEnabled(isILSelected);
		_txtIL_ConfigDescription.setEnabled(isILSelected);
		_txtIL_LastMarker.setEnabled(isLastMarkerSelected);

		_ilViewer.getTable().setEnabled(isLauncherAvailable);

	}

	private void fillSpeedTourTypeMenu(final IMenuManager menuMgr, final Link linkTourType) {

		// get tour type which will be checked in the menu
		final TourType checkedTourType = null;

		final int speedTTIndex = (int) linkTourType.getData(DATA_KEY_SPEED_TOUR_TYPE_INDEX);

		// add all tour types to the menu
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		for (final TourType tourType : tourTypes) {

			boolean isChecked = false;
			if (checkedTourType != null && checkedTourType.getTypeId() == tourType.getTypeId()) {
				isChecked = true;
			}

			final ActionSpeedTourType_SetInMenu action = new ActionSpeedTourType_SetInMenu(
					tourType,
					isChecked,
					speedTTIndex);

			menuMgr.add(action);
		}

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenTourTypePrefs);
	}

	private void fillTourTypeMenu(final IMenuManager menuMgr) {

		// get tour type which will be checked in the menu
		final TourType checkedTourType = _currentIL.oneTourType;

		// add all tour types to the menu
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		for (final TourType tourType : tourTypes) {

			boolean isChecked = false;
			if (checkedTourType != null && checkedTourType.getTypeId() == tourType.getTypeId()) {
				isChecked = true;
			}

			final ActionIL_SetOneTourType action = new ActionIL_SetOneTourType(tourType, isChecked);

			menuMgr.add(action);
		}

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenTourTypePrefs);
	}

	private void fillTourTypeOneMenu(final IMenuManager menuMgr) {

		// add all tour types to the menu
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		for (final TourType tourType : tourTypes) {

			final ActionIL_NewOneTourType action = new ActionIL_NewOneTourType(tourType);

			menuMgr.add(action);
		}

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenTourTypePrefs);
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
//		return null;
	}

	/**
	 * @param importLauncher
	 * @return Returns from the model the last marker distance value in the current measurment
	 *         system.
	 */
	private double getMarkerDistanceValue(final ImportLauncher importLauncher) {

		return importLauncher.lastMarkerDistance / 1000.0 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
	}

	public ImportConfig getModifiedConfig() {
		return _dialogConfig;
	}

	/**
	 * @return Returns the selected distance of the last marker in meters.
	 */
	private int getSelectedLastMarkerDistance() {

		final float lastMarkerDistance = _spinnerIL_LastMarkerDistance.getSelection()
				* net.tourbook.ui.UI.UNIT_VALUE_DISTANCE
				* 100;

		return (int) lastMarkerDistance;
	}

	/**
	 * @return Returns the selected tour type configuration or <code>null</code> when a tour type
	 *         will not be set.
	 */
	@SuppressWarnings("unchecked")
	private Enum<TourTypeConfig> getSelectedTourTypeConfig() {

		final boolean isSetTourType = _chkIL_SetTourType.getSelection();

		if (isSetTourType) {

			int configIndex = _comboIL_TourType.getSelectionIndex();

			if (configIndex == -1) {
				configIndex = 0;
			}

			final ComboEnumEntry<?> selectedItem = RawDataManager.ALL_IMPORT_TOUR_TYPE_CONFIG[configIndex];

			return (Enum<TourTypeConfig>) selectedItem.value;
		}

		return null;
	}

	private int getTourTypeConfigIndex(final Enum<TourTypeConfig> tourTypeConfig) {

		if (tourTypeConfig == null) {
			// this case should not happen
			return -1;
		}

		final ComboEnumEntry<?>[] allImportTourTypeConfig = RawDataManager.ALL_IMPORT_TOUR_TYPE_CONFIG;

		for (int configIndex = 0; configIndex < allImportTourTypeConfig.length; configIndex++) {

			final ComboEnumEntry<?> tourtypeConfig = allImportTourTypeConfig[configIndex];

			if (tourtypeConfig.value.equals(tourTypeConfig)) {
				return configIndex;
			}
		}

		return -1;
	}

	@Override
	public ColumnViewer getViewer() {
		return _ilViewer;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_leftPadding = convertHorizontalDLUsToPixels(11);
//		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableControls();
			}
		};

		_importLauncherModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				onIL_Modified();
			}
		};

		_importLauncherSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onIL_Modified();
			}
		};

		/*
		 * Path listener
		 */
		_folderFocusListener = new FocusAdapter() {
			@Override
			public void focusLost(final FocusEvent e) {
				onFolder_FocusLost(e);
			}
		};
		_folderModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				onFolder_Modified(e);
			}
		};
		_folderKeyListener = new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				onFolder_KeyPressed(e);
			}
		};

		/*
		 * Field listener
		 */
		_liveUpdateListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				doLiveUpdate();
			}
		};
		_liveUpdateMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				doLiveUpdate();
			}
		};

		/*
		 * Default mouse listener
		 */
		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
			}
		};

		_speedTourTypeListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				UI.openControlMenu((Link) event.widget);
			}
		};
	}

	@Override
	protected void okPressed() {

		update_Model_From_UI_Launcher();
		update_Model_From_UI_TourTypeItems_Sorted();

		update_Model_From_UI_LiveUpdateValues();
		update_Model_From_UI_Folder();

		super.okPressed();
	}

	private void onDispose() {

		disposeConfigImages();

		_prefStore.removePropertyChangeListener(_prefChangeListener);
	}

	private void onFolder_FocusLost(final FocusEvent event) {

		final Combo combo = (Combo) event.widget;
		final HistoryItems historyItems = (HistoryItems) combo.getData();

		// keep manually entered folders in the history
		historyItems.updateHistory();
	}

	private void onFolder_KeyPressed(final KeyEvent event) {

		final int keyCode = event.keyCode;
		final boolean isCtrlKey = (event.stateMask & SWT.MOD1) > 0;

		if (isCtrlKey && keyCode == SWT.DEL) {

			// delete this item from the history

			final Combo combo = (Combo) event.widget;
			final HistoryItems historyItems = (HistoryItems) combo.getData();

			historyItems.removeFromHistory(combo.getText());

		} else if (isCtrlKey && (keyCode == 's' || keyCode == 'S')) {

			// sort history by folder name

			final Combo combo = (Combo) event.widget;
			final HistoryItems historyItems = (HistoryItems) combo.getData();

			historyItems.sortHistory();
		}
	}

	private void onFolder_Modified(final ModifyEvent event) {

		final Combo combo = (Combo) event.widget;
		final HistoryItems historyItems = (HistoryItems) combo.getData();

		historyItems.validateModifiedPath();
	}

	private void onIL_Add(final boolean isCopy) {

		// keep modifications
		update_Model_From_UI_Launcher();

		// update model
		final ArrayList<ImportLauncher> configItems = _dialogConfig.importLaunchers;
		ImportLauncher newConfig;

		if (isCopy) {

			newConfig = _currentIL.clone();

			// make the clone more visible
			newConfig.name = newConfig.name + UI.SPACE + newConfig.getId();

		} else {

			newConfig = new ImportLauncher();
		}

		configItems.add(newConfig);

		// update UI
		_ilViewer.refresh();

		// prevent that the horizontal scrollbar is visible
		_ilViewer.getTable().getParent().layout();

		_ilViewer.setSelection(new StructuredSelection(newConfig), true);

		_txtIL_ConfigName.setFocus();

		if (isCopy) {
			_txtIL_ConfigName.selectAll();
		}
	}

	private void onIL_AddOne(final TourType tourType) {

		// keep modifications
		update_Model_From_UI_Launcher();

		// create new tt item
		final ImportLauncher newTTItem = new ImportLauncher();

		newTTItem.tourTypeConfig = TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL;
		newTTItem.oneTourType = tourType;
		newTTItem.name = tourType.getName();

		// update model
		_dialogConfig.importLaunchers.add(newTTItem);

		// update UI
		_ilViewer.refresh();

		// prevent that the horizontal scrollbar is visible
		_ilViewer.getTable().getParent().layout();

		_ilViewer.setSelection(new StructuredSelection(newTTItem), true);

		_txtIL_ConfigName.setFocus();
		_txtIL_ConfigName.selectAll();
	}

	private void onIL_DblClick() {

		_txtIL_ConfigName.setFocus();
		_txtIL_ConfigName.selectAll();
	}

	private void onIL_Modified() {

		if (_isInUIUpdate) {
			return;
		}

		if (_currentIL == null) {
			return;
		}

		// update model which is displayed in the IL viewer
		_currentIL.name = _txtIL_ConfigName.getText();
		_currentIL.description = _txtIL_ConfigDescription.getText();

		_currentIL.isSetLastMarker = _chkIL_SetLastMarker.getSelection();
		_currentIL.lastMarkerDistance = getSelectedLastMarkerDistance();

		_currentIL.isSaveTour = _chkIL_SaveTour.getSelection();

		// update UI
		_ilViewer.update(_currentIL, null);
	}

	private void onIL_Remove() {

		final StructuredSelection selection = (StructuredSelection) _ilViewer.getSelection();
		final ImportLauncher selectedConfig = (ImportLauncher) selection.getFirstElement();

		int selectedIndex = -1;
		final ArrayList<ImportLauncher> configItems = _dialogConfig.importLaunchers;

		// get index of the selected config
		for (int configIndex = 0; configIndex < configItems.size(); configIndex++) {

			final ImportLauncher config = configItems.get(configIndex);

			if (config.equals(selectedConfig)) {
				selectedIndex = configIndex;
				break;
			}
		}

		if (selectedIndex == -1) {

			// item not found which should not happen
			return;
		}

		// update model
		configItems.remove(selectedIndex);

		// update UI
		_ilViewer.refresh();

		// select config at the same position

		if (configItems.size() == 0) {

			// all configs are removed, setup empty UI

			_currentIL = null;

			_txtIL_ConfigName.setText(UI.EMPTY_STRING);
			_txtIL_ConfigDescription.setText(UI.EMPTY_STRING);

			// remove fields
			createUI_835_SpeedTourType_Fields();

			enableILControls();

			_dialogConfig.isLastLauncherRemoved = true;

		} else {

			if (selectedIndex >= configItems.size()) {
				selectedIndex--;
			}

			final ImportLauncher nextConfig = configItems.get(selectedIndex);

			_ilViewer.setSelection(new StructuredSelection(nextConfig), true);
		}

		_ilViewer.getTable().setFocus();
	}

	private void onPaintViewer(final Event event) {

		if (event.index != _columnIndexConfigImage) {
			return;
		}

		final TableItem item = (TableItem) event.item;
		final ImportLauncher importLauncher = (ImportLauncher) item.getData();

		switch (event.type) {
		case SWT.MeasureItem:

			/*
			 * Set height also for color def, when not set and all is collapsed, the color def size
			 * will be adjusted when an item is expanded.
			 */

			event.width += importLauncher.imageWidth;
//			event.height = PROFILE_IMAGE_HEIGHT;

			break;

		case SWT.PaintItem:

			final Image image = _rawDataView.getImportConfigImage(importLauncher);

			if (image != null && !image.isDisposed()) {

				final Rectangle rect = image.getBounds();

				final int x = event.x + event.width;
				final int yOffset = Math.max(0, (event.height - rect.height) / 2);

				event.gc.drawImage(image, x, event.y + yOffset);
			}

			break;
		}
	}

	private void onSelectFolder_Backup() {

		final String filterOSPath = _backupHistoryItems.getOSPath(
				_comboBackupFolder.getText(),
				_dialogConfig.getBackupFolder());

		final DirectoryDialog dialog = new DirectoryDialog(_parent.getShell(), SWT.SAVE);

		dialog.setText(Messages.Dialog_ImportConfig_Dialog_BackupFolder_Title);
		dialog.setMessage(Messages.Dialog_ImportConfig_Dialog_BackupFolder_Message);
		dialog.setFilterPath(filterOSPath);

		final String selectedFolder = dialog.open();

		if (selectedFolder != null) {

			setErrorMessage(null);

			_backupHistoryItems.onSelectFolderInDialog(selectedFolder);
		}
	}

	private void onSelectFolder_Device() {

		final String filterOSPath = _deviceHistoryItems.getOSPath(
				_comboDeviceFolder.getText(),
				_dialogConfig.getDeviceFolder());

		final DirectoryDialog dialog = new DirectoryDialog(_parent.getShell(), SWT.SAVE);

		dialog.setText(Messages.Dialog_ImportConfig_Dialog_DeviceFolder_Title);
		dialog.setMessage(Messages.Dialog_ImportConfig_Dialog_DeviceFolder_Message);
		dialog.setFilterPath(filterOSPath);

		final String selectedFolder = dialog.open();

		if (selectedFolder != null) {

			setErrorMessage(null);

			_deviceHistoryItems.onSelectFolderInDialog(selectedFolder);
		}
	}

	private void onSelectIL(final ISelection selection) {

		final ImportLauncher selectedIL = (ImportLauncher) ((StructuredSelection) selection).getFirstElement();

		if (_currentIL == selectedIL) {
			// this is already selected
			return;
		}

		// update model from the old selected config
		update_Model_From_UI_Launcher();

		// set new model
		_currentIL = selectedIL;

		update_UI_From_Model_TourTypes();

		enableILControls();
	}

	private void onSelectTourType() {

		final Enum<TourTypeConfig> selectedTourTypeItem = getSelectedTourTypeConfig();

		showTourTypePage(selectedTourTypeItem);

		update_Model_From_UI_Launcher();
		update_UI_From_Model_TourTypes();

		enableILControls();

		redrawILViewer();
	}

	private void onSpeedTT_Add() {

		update_Model_From_UI_Launcher();

		final ArrayList<SpeedTourType> speedTourTypes = _currentIL.speedTourTypes;

		// update model
		speedTourTypes.add(0, new SpeedTourType());

		// sort by speed
		Collections.sort(speedTourTypes);

		// update UI + model
		update_UI_From_Model_TourTypes();

		enableILControls();

		// set focus to the speed
		_spinnerTT_Speed_AvgSpeed[0].setFocus();

		redrawILViewer();
	}

	private void onSpeedTT_Remove(final int speedTTIndex) {

		// update model
		update_Model_From_UI_Launcher();

		final ArrayList<SpeedTourType> speedTourTypes = _currentIL.speedTourTypes;

		final SpeedTourType removedSpeedTT = speedTourTypes.get(speedTTIndex);

		speedTourTypes.remove(removedSpeedTT);

		// update UI
		update_UI_From_Model_TourTypes();

		enableILControls();

		redrawILViewer();
	}

	private void onSpeedTT_SetTourType(final int speedTTIndex, final TourType tourType) {

		/*
		 * Update UI
		 */
		final Image image = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
		final Label ttIcon = _lblTT_Speed_TourTypeIcon[speedTTIndex];
		final Link ttLink = _linkTT_Speed_TourType[speedTTIndex];

		ttIcon.setImage(image);

		ttLink.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);
		ttLink.setData(DATA_KEY_TOUR_TYPE_ID, tourType.getTypeId());

		_speedTourType_OuterContainer.layout();

		// update UI with modified tour type
		update_Model_From_UI_Launcher();
		_ilViewer.update(_currentIL, null);

		redrawILViewer();
	}

	private void onSpeedTT_Sort() {

		update_Model_From_UI_Launcher();
		update_UI_From_Model_TourTypes();

		redrawILViewer();
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final ISelection selection = _ilViewer.getSelection();

			_ilViewer.getTable().dispose();

			createUI_52_IL_ViewerTable(_viewerContainer);
			_viewerContainer.layout();

			// update viewer
			reloadViewer();

			_ilViewer.setSelection(selection);
		}
		_viewerContainer.setRedraw(true);

		return _ilViewer;
	}

	private void redrawILViewer() {

		// IL viewer MUST be redrawn to show modified tour type image
		_ilViewer.getTable().redraw();
	}

	@Override
	public void reloadViewer() {

		_ilViewer.setInput(this);
	}

	private void restoreState() {

		/*
		 * Reselect previous selected config when available
		 */
		final String stateILName = Util.getStateString(_state, STATE_IMPORT_LAUNCHER, UI.EMPTY_STRING);
		final ArrayList<ImportLauncher> importLaunchers = _dialogConfig.importLaunchers;

		for (final ImportLauncher importLauncher : importLaunchers) {

			if (importLauncher.name.equals(stateILName)) {

				_initialImportLauncher = importLauncher;
				break;
			}
		}

		if (_initialImportLauncher == null && importLaunchers.size() > 0) {
			_initialImportLauncher = importLaunchers.get(0);
		}

		_chkLiveUpdate.setSelection(_dialogConfig.isLiveUpdate);

		_spinnerDash_AnimationCrazinessFactor.setSelection(_dialogConfig.animationCrazinessFactor);
		_spinnerDash_AnimationDuration.setSelection(_dialogConfig.animationDuration);
		_spinnerDash_BgOpacity.setSelection(_dialogConfig.backgroundOpacity);
		_spinnerDash_NumHTiles.setSelection(_dialogConfig.numHorizontalTiles);
		_spinnerDash_TileSize.setSelection(_dialogConfig.tileSize);

		/*
		 * Loading the volume information can delay the startup of the dialog
		 */
		_chkCreateBackup.setSelection(_dialogConfig.isCreateBackup);
		_comboBackupFolder.setText(Messages.Dialog_ImportConfig_Info_RetrievingVolumeInfo);
		_comboDeviceFolder.setText(Messages.Dialog_ImportConfig_Info_RetrievingVolumeInfo);

		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
					@Override
					public void run() {

						_deviceHistoryItems.restoreState(
								_state.getArray(STATE_DEVICE_FOLDER_HISTORY_ITEMS),
								_state.getArray(STATE_DEVICE_DEVICE_HISTORY_ITEMS),
								_dialogConfig.getDeviceFolder());

						_backupHistoryItems.restoreState(
								_state.getArray(STATE_BACKUP_FOLDER_HISTORY_ITEMS),
								_state.getArray(STATE_BACKUP_DEVICE_HISTORY_ITEMS),
								_dialogConfig.getBackupFolder());

						_comboBackupFolder.setText(_dialogConfig.getBackupFolder());
						_comboDeviceFolder.setText(_dialogConfig.getDeviceFolder());
					}
				});
			}
		});
	}

	private void saveState() {

		_state.put(STATE_IMPORT_LAUNCHER, _currentIL == null ? UI.EMPTY_STRING : _currentIL.name);

		_columnManager.saveState(_state);

		_backupHistoryItems.saveState(_state, STATE_BACKUP_FOLDER_HISTORY_ITEMS, STATE_BACKUP_DEVICE_HISTORY_ITEMS);
		_deviceHistoryItems.saveState(_state, STATE_DEVICE_FOLDER_HISTORY_ITEMS, STATE_DEVICE_DEVICE_HISTORY_ITEMS);
	}

	private void showTourTypePage(final Enum<TourTypeConfig> selectedConfig) {

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(selectedConfig)) {

			_pagebookTourType.showPage(_pageTourType_BySpeed);

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(selectedConfig)) {

			_pagebookTourType.showPage(_pageTourType_OneForAll);

		} else {

			_pagebookTourType.showPage(_pageTourType_NoTourType);
		}
	}

	private void update_Model_From_UI_Folder() {

		_dialogConfig.isCreateBackup = _chkCreateBackup.getSelection();
		_dialogConfig.setBackupFolder(_comboBackupFolder.getText());
		_dialogConfig.setDeviceFolder(_comboDeviceFolder.getText());
	}

	/**
	 * Set data from the UI into the model.
	 */
	private void update_Model_From_UI_Launcher() {

		if (_currentIL == null) {
			return;
		}

		_currentIL.name = _txtIL_ConfigName.getText();
		_currentIL.description = _txtIL_ConfigDescription.getText();
		_currentIL.isSaveTour = _chkIL_SaveTour.getSelection();
		_currentIL.isShowInDashboard = _chkIL_ShowInDashboard.getSelection();

		// last marker
		_currentIL.isSetLastMarker = _chkIL_SetLastMarker.getSelection();
		_currentIL.lastMarkerDistance = getSelectedLastMarkerDistance();
		_currentIL.lastMarkerText = _txtIL_LastMarker.getText();

		final Enum<TourTypeConfig> selectedTourTypeConfig = getSelectedTourTypeConfig();

		_currentIL.tourTypeConfig = selectedTourTypeConfig;

		/*
		 * Set tour type data
		 */
		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(selectedTourTypeConfig)) {

			final ArrayList<SpeedTourType> speedTourTypes = _currentIL.speedTourTypes;

			if (_spinnerTT_Speed_AvgSpeed != null) {

				final ArrayList<SpeedTourType> newSpeedTourTypes = new ArrayList<SpeedTourType>();

				for (int speedTTIndex = 0; speedTTIndex < speedTourTypes.size(); speedTTIndex++) {

					final Spinner spinnerAvgSpeed = _spinnerTT_Speed_AvgSpeed[speedTTIndex];
					final Link linkTourType = _linkTT_Speed_TourType[speedTTIndex];

					final SpeedTourType speedTourType = new SpeedTourType();

					speedTourType.avgSpeed = spinnerAvgSpeed.getSelection() * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

					final Object tourTypeId = linkTourType.getData(DATA_KEY_TOUR_TYPE_ID);
					if (tourTypeId instanceof Long) {
						speedTourType.tourTypeId = (long) tourTypeId;
					} else {
						speedTourType.tourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;
					}

					newSpeedTourTypes.add(speedTourType);
				}

				// sort value
				Collections.sort(newSpeedTourTypes);

				// update model
				speedTourTypes.clear();
				speedTourTypes.addAll(newSpeedTourTypes);
			}

			_currentIL.setupItemImage();

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(selectedTourTypeConfig)) {

			update_Model_From_UI_OneTourType();

		} else {

			// this is the default

			_currentIL.setupItemImage();
		}
	}

	private void update_Model_From_UI_LiveUpdateValues() {

		_dialogConfig.isLiveUpdate = _chkLiveUpdate.getSelection();

		_dialogConfig.animationCrazinessFactor = _spinnerDash_AnimationCrazinessFactor.getSelection();
		_dialogConfig.animationDuration = _spinnerDash_AnimationDuration.getSelection();
		_dialogConfig.backgroundOpacity = _spinnerDash_BgOpacity.getSelection();
		_dialogConfig.numHorizontalTiles = _spinnerDash_NumHTiles.getSelection();
		_dialogConfig.tileSize = _spinnerDash_TileSize.getSelection();
	}

	private void update_Model_From_UI_OneTourType() {

		final Object tourTypeId = _linkTT_One_TourType.getData(DATA_KEY_TOUR_TYPE_ID);

		if (tourTypeId instanceof Long) {
			_currentIL.oneTourType = TourDatabase.getTourType((long) tourTypeId);
		} else {

			_currentIL.oneTourType = null;
		}

		_currentIL.setupItemImage();
	}

	/**
	 * Create config list in the table sort order
	 */
	private void update_Model_From_UI_TourTypeItems_Sorted() {

		final ArrayList<ImportLauncher> importTiles = _dialogConfig.importLaunchers;
		importTiles.clear();

		for (final TableItem tableItem : _ilViewer.getTable().getItems()) {

			final Object itemData = tableItem.getData();

			if (itemData instanceof ImportLauncher) {
				importTiles.add((ImportLauncher) itemData);
			}
		}
	}

	private void update_UI_From_Model_TourTypes() {

		if (_currentIL == null) {
			return;
		}

		_isInUIUpdate = true;

		final Enum<TourTypeConfig> tourTypeConfig = _currentIL.tourTypeConfig;
		final boolean isSetTourType = tourTypeConfig != null;

		final double distance = getMarkerDistanceValue(_currentIL);
		final double distance10 = distance * 10;
		final int distanceValue = (int) (distance10 + 0.5);
		final String lastMarkerText = _currentIL.lastMarkerText;

		_txtIL_ConfigName.setText(_currentIL.name);
		_txtIL_ConfigDescription.setText(_currentIL.description);
		_chkIL_SaveTour.setSelection(_currentIL.isSaveTour);
		_chkIL_ShowInDashboard.setSelection(_currentIL.isShowInDashboard);

		// last marker
		_chkIL_SetLastMarker.setSelection(_currentIL.isSetLastMarker);
		_spinnerIL_LastMarkerDistance.setSelection(distanceValue);
		_txtIL_LastMarker.setText(lastMarkerText == null ? UI.EMPTY_STRING : lastMarkerText);

		_chkIL_SetTourType.setSelection(isSetTourType);

		if (isSetTourType) {
			_comboIL_TourType.select(getTourTypeConfigIndex(tourTypeConfig));
		}

		/*
		 * Setup tour type UI
		 */

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

			_speedTourType_OuterContainer.setRedraw(false);
			{
				// check and create fields
				createUI_835_SpeedTourType_Fields();

				final ArrayList<SpeedTourType> speedTourTypes = _currentIL.speedTourTypes;

				final int speedTTSize = speedTourTypes.size();

				final net.tourbook.ui.UI uiInstance = net.tourbook.ui.UI.getInstance();

				for (int speedTTIndex = 0; speedTTIndex < speedTTSize; speedTTIndex++) {

					final SpeedTourType speedTT = speedTourTypes.get(speedTTIndex);
					final long tourTypeId = speedTT.tourTypeId;

					final Spinner spinnerAvgSpeed = _spinnerTT_Speed_AvgSpeed[speedTTIndex];
					final Link linkTourType = _linkTT_Speed_TourType[speedTTIndex];
					final Label labelTourTypeIcon = _lblTT_Speed_TourTypeIcon[speedTTIndex];

					// update UI
					final double avgSpeed = (speedTT.avgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE) + 0.0001;
					spinnerAvgSpeed.setSelection((int) avgSpeed);

					if (tourTypeId == TourDatabase.ENTITY_IS_NOT_SAVED) {

						// tour type is not yet set

						linkTourType.setData(DATA_KEY_TOUR_TYPE_ID, null);
						linkTourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
						labelTourTypeIcon.setImage(null);

					} else {

						linkTourType.setData(DATA_KEY_TOUR_TYPE_ID, tourTypeId);
						linkTourType.setText(UI.LINK_TAG_START
								+ net.tourbook.ui.UI.getTourTypeLabel(tourTypeId)
								+ UI.LINK_TAG_END);
						labelTourTypeIcon.setImage(uiInstance.getTourTypeImage(tourTypeId));
					}

					// keep references
					labelTourTypeIcon.setData(DATA_KEY_SPEED_TOUR_TYPE_INDEX, speedTTIndex);
					linkTourType.setData(DATA_KEY_SPEED_TOUR_TYPE_INDEX, speedTTIndex);
					spinnerAvgSpeed.setData(DATA_KEY_SPEED_TOUR_TYPE_INDEX, speedTTIndex);
					_actionTTSpeed_Delete[speedTTIndex].setData(DATA_KEY_SPEED_TOUR_TYPE_INDEX, speedTTIndex);

				}
			}
			_speedTourType_OuterContainer.setRedraw(true);

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {

			TourType tourType = null;

			final TourType oneTourType = _currentIL.oneTourType;
			if (oneTourType != null) {

				final long tourTypeId = oneTourType.getTypeId();
				tourType = TourDatabase.getTourType(tourTypeId);
			}

			updateUI_OneTourType(tourType);

		} else {

			// this is the default, a tour type is not set
		}

		showTourTypePage(tourTypeConfig);

		_isInUIUpdate = false;
	}

	private void updateUI_ClearSpeedTourTypes() {

		if (_speedTourType_ScrolledContainer != null) {

			_speedTourType_ScrolledContainer.dispose();
			_speedTourType_ScrolledContainer = null;

			_actionTTSpeed_Delete = null;
			_lblTT_Speed_TourTypeIcon = null;
			_lblTT_Speed_SpeedUnit = null;
			_linkTT_Speed_TourType = null;
			_spinnerTT_Speed_AvgSpeed = null;
		}
	}

	private void updateUI_OneTourType(final TourType tourType) {

		if (tourType == null) {

			_lblIL_One_TourTypeIcon.setImage(null);

			_linkTT_One_TourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
			_linkTT_One_TourType.setData(DATA_KEY_TOUR_TYPE_ID, null);

		} else {

			final Image image = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());

			_lblIL_One_TourTypeIcon.setImage(image);

			_linkTT_One_TourType.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);
			_linkTT_One_TourType.setData(DATA_KEY_TOUR_TYPE_ID, tourType.getTypeId());
		}

		// update the model that the table displays the correct image
		update_Model_From_UI_OneTourType();

		redrawILViewer();
	}

}
