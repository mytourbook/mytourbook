/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.LocalSelectionTransfer;
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
import org.eclipse.swt.graphics.Font;
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
public class DialogDeviceImportConfig extends TitleAreaDialog implements ITourViewer {

	private static final int				VERTICAL_GROUP_DISTANCE				= 10;
	private static final String				ID									= "net.tourbook.importdata.DialogDeviceImportConfig";	//$NON-NLS-1$
	//
	private static final String				STATE_BACKUP_DEVICE_HISTORY_ITEMS	= "STATE_BACKUP_DEVICE_HISTORY_ITEMS";					//$NON-NLS-1$
	private static final String				STATE_BACKUP_FOLDER_HISTORY_ITEMS	= "STATE_BACKUP_FOLDER_HISTORY_ITEMS";					//$NON-NLS-1$
	private static final String				STATE_DEVICE_DEVICE_HISTORY_ITEMS	= "STATE_DEVICE_DEVICE_HISTORY_ITEMS";					//$NON-NLS-1$
	private static final String				STATE_DEVICE_FOLDER_HISTORY_ITEMS	= "STATE_DEVICE_FOLDER_HISTORY_ITEMS";					//$NON-NLS-1$
	private static final String				STATE_IMPORT_LAUNCHER				= "STATE_IMPORT_LAUNCHER";								//$NON-NLS-1$
	//
	private static final String				DATA_KEY_TOUR_TYPE_ID				= "DATA_KEY_TOUR_TYPE_ID";								//$NON-NLS-1$
	private static final String				DATA_KEY_VERTEX_INDEX				= "DATA_KEY_VERTEX_INDEX";								//$NON-NLS-1$
	//
	private static final int				CONTROL_DECORATION_WIDTH			= 6;
	//
	private final IDialogSettings			_state								= TourbookPlugin.getState(ID);
	//
	private MouseWheelListener				_defaultMouseWheelListener;
	private SelectionAdapter				_defaultSelectionListener;
	private FocusListener					_folderFocusListener;
	private KeyAdapter						_folderKeyListener;
	private ModifyListener					_folderModifyListener;
	private SelectionAdapter				_liveUpdateListener;
	private MouseWheelListener				_liveUpdateMouseWheelListener;
	private SelectionAdapter				_vertexTourTypeListener;
	//
	private ActionOpenPrefDialog			_actionOpenTourTypePrefs;
	private ActionAddVertex					_actionTTSpeed_Add;
	private ActionDeleteVertex[]			_actionTTSpeed_Delete;
	private ActionSortVertices				_actionTTSpeed_Sort;
	//

	private PixelConverter					_pc;

	private final static SpeedTourType[]	DEFAULT_VERTICES;

	static {

		DEFAULT_VERTICES = new SpeedTourType[] {
			//
			new SpeedTourType(10),
			new SpeedTourType(30),
			new SpeedTourType(150),
			new SpeedTourType(300)
		//
		};
	}

	/** Model for all configs. */
	private ImportConfig					_dialogConfig;

	/** Model for the currently selected config. */
	private DeviceImportLauncher			_currentIL;

	private RawDataView						_rawDataView;

	private TableViewer						_ilViewer;

	private ColumnManager					_columnManager;
	private TableColumnDefinition			_colDefProfileImage;
	private int								_columnIndexConfigImage;

	private DeviceImportLauncher			_initialImportLauncher;

	private HashMap<Long, Image>			_configImages						= new HashMap<>();
	private HashMap<Long, Integer>			_configImageHash					= new HashMap<>();

	private HistoryItems					_deviceHistoryItems					= new HistoryItems();
	private HistoryItems					_backupHistoryItems					= new HistoryItems();

	private long							_dragStart;
	private int								_leftPadding;

	/*
	 * UI resources
	 */
	private Font							_boldFont;

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
	private Button							_chkTTSetTourType;
	private Button							_btnSelectBackupFolder;
	private Button							_btnSelectDeviceFolder;
	private Button							_btnTTConfig_Duplicate;
	private Button							_btnTTConfig_New;
	private Button							_btnTTConfig_NewOne;
	private Button							_btnTTConfig_Remove;

	private Combo							_comboBackupFolder;
	private Combo							_comboDeviceFolder;
	private Combo							_comboTTConfig;

	private Label							_lblBackupFolder;
	private Label							_lblLocalFolderPath;
	private Label							_lblDeviceFolder;
	private Label							_lblDeviceFolderPath;
	private Label							_lblTTConfigDescription;
	private Label							_lblTTConfigName;
	private Label							_lblTTHint;
	private Label							_lblTTOne_TourTypeIcon;
	private Label							_lblTTTourType;
	private Label[]							_lblTTSpeed_SpeedUnit;
	private Label[]							_lblTTSpeed_TourTypeIcon;

	private Link[]							_linkTTSpeed_TourType;
	private Link							_linkTTOne_TourType;

	private Spinner							_spinnerAnimationCrazinessFactor;
	private Spinner							_spinnerAnimationDuration;
	private Spinner							_spinnerBgOpacity;
	private Spinner							_spinnerNumHTiles;
	private Spinner							_spinnerTileSize;
	private Spinner[]						_spinnerTTVertex_AvgSpeed;

	private Text							_txtTTConfigDescription;
	private Text							_txtTTConfigName;

	private class ActionAddVertex extends Action {

		public ActionAddVertex() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_ImportConfig_Action_AddSpeed_Tooltip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__App_Add));
		}

		@Override
		public void run() {
			onSpeedTourType_Add();
		}
	}

	private class ActionDeleteVertex extends Action {

		private int	_vertexIndex;

		public ActionDeleteVertex() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_ImportConfig_Action_RemoveSpeed_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash_Disabled));
		}

		@Override
		public void run() {
			onSpeedTourType_Remove(_vertexIndex);
		}

		public void setData(final String key, final int vertexIndex) {

			_vertexIndex = vertexIndex;
		}
	}

	private class ActionNewTourTypeOne extends Action {

		private TourType	_tourType;

		/**
		 * @param tourType
		 * @param vertexIndex
		 * @param isSaveTour
		 *            when <code>true</code> the tour will be saved and a
		 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise the
		 *            {@link TourData} from the tour provider is only updated
		 */
		public ActionNewTourTypeOne(final TourType tourType) {

			super(tourType.getName(), AS_CHECK_BOX);

			// show image when tour type can be selected, disabled images look ugly on win
			final Image tourTypeImage = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
			setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));

			_tourType = tourType;
		}

		@Override
		public void run() {
			onTourType_AddOne(_tourType);
		}
	}

	private class ActionSetOneTourType extends Action {

		private TourType	__tourType;

		/**
		 * @param tourType
		 * @param vertexIndex
		 * @param isSaveTour
		 *            when <code>true</code> the tour will be saved and a
		 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise the
		 *            {@link TourData} from the tour provider is only updated
		 */
		public ActionSetOneTourType(final TourType tourType, final boolean isChecked) {

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

	private class ActionSetSpeedTourType extends Action {

		private int			_vertexIndex;
		private TourType	_tourType;

		/**
		 * @param tourType
		 * @param vertexIndex
		 * @param isSaveTour
		 *            when <code>true</code> the tour will be saved and a
		 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise the
		 *            {@link TourData} from the tour provider is only updated
		 */
		public ActionSetSpeedTourType(final TourType tourType, final boolean isChecked, final int vertexIndex) {

			super(tourType.getName(), AS_CHECK_BOX);

			_vertexIndex = vertexIndex;

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
			onVertex_SetTourType(_vertexIndex, _tourType);
		}
	}

	private class ActionSortVertices extends Action {

		public ActionSortVertices() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_ImportConfig_Action_SortVertices_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__App_Sort));
			setDisabledImageDescriptor(TourbookPlugin
					.getImageDescriptor(net.tourbook.Messages.Image__App_Sort_Disabled));
		}

		@Override
		public void run() {
			onVertex_Sort();
		}
	}

	private class ClientsContentProvider implements IStructuredContentProvider {

		public ClientsContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {

			final ArrayList<DeviceImportLauncher> configItems = _dialogConfig.deviceImportLaunchers;

			return configItems.toArray(new DeviceImportLauncher[configItems.size()]);
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {

		}
	}

	public DialogDeviceImportConfig(final Shell parentShell,
									final ImportConfig importConfig,
									final RawDataView rawDataView) {

		super(parentShell);

		_rawDataView = rawDataView;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__options).createImage());

		cloneImportConfig(importConfig);
	}

	/**
	 * Clone original configs, only the backup will be modified in the dialog.
	 * 
	 * @param importConfig
	 */
	private void cloneImportConfig(final ImportConfig importConfig) {

		_dialogConfig = new ImportConfig();

		final ArrayList<DeviceImportLauncher> importLaunchers = _dialogConfig.deviceImportLaunchers = new ArrayList<>();

		for (final DeviceImportLauncher launcher : importConfig.deviceImportLaunchers) {
			importLaunchers.add(launcher.clone());
		}

		_dialogConfig.isLiveUpdate = importConfig.isLiveUpdate;

		_dialogConfig.animationCrazinessFactor = importConfig.animationCrazinessFactor;
		_dialogConfig.animationDuration = importConfig.animationDuration;
		_dialogConfig.backgroundOpacity = importConfig.backgroundOpacity;
		_dialogConfig.numHorizontalTiles = importConfig.numHorizontalTiles;
		_dialogConfig.tileSize = importConfig.tileSize;

		_dialogConfig.isCreateBackup = importConfig.isCreateBackup;
		_dialogConfig.backupFolder = importConfig.backupFolder;
		_dialogConfig.deviceFolder = importConfig.deviceFolder;
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
		enableTTControls();

		// set focus
		_comboBackupFolder.setFocus();
	}

	private void createActions() {

		_actionTTSpeed_Add = new ActionAddVertex();
		_actionTTSpeed_Sort = new ActionSortVertices();

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

	/**
	 * Creates a configuration from the {@link #DEFAULT_VERTICES}.
	 * 
	 * @return Returns the created config.
	 */
	private DeviceImportLauncher createDefaultConfig() {

		final DeviceImportLauncher defaultConfig = new DeviceImportLauncher();
		final ArrayList<SpeedTourType> speedVertices = defaultConfig.speedTourTypes;

		for (final SpeedTourType speedVertex : DEFAULT_VERTICES) {
			speedVertices.add(speedVertex.clone());
		}

		_dialogConfig.deviceImportLaunchers.add(defaultConfig);

		return defaultConfig;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		_parent = parent;

		final Composite ui = (Composite) super.createDialogArea(parent);

		initUI(ui);
		createActions();

		createUI(ui);
		createMenus();

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
		final Menu ttContextMenu = menuMgr.createContextMenu(_linkTTOne_TourType);
		_linkTTOne_TourType.setMenu(ttContextMenu);
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
			createUI_90_Dashboard(container);
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
//				.indent(0, convertVerticalDLUsToPixels(8))
				.applyTo(_chkCreateBackup);

		/*
		 * Label: Local folder
		 */
		_lblBackupFolder = new Label(parent, SWT.NONE);
		_lblBackupFolder.setText(Messages.Dialog_ImportConfig_Label_BackupFolder);
		_lblBackupFolder.setToolTipText(Messages.Dialog_ImportConfig_Label_BackupFolder_Tooltip);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.indent(_leftPadding, 0)
				.applyTo(_lblBackupFolder);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
//				.extendedMargins(CONTROL_DECORATION_WIDTH, 0, 0, 0)
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
				.indent(0, convertVerticalDLUsToPixels(8))
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

			createUI_51_ILViewer(group);
			createUI_60_ILActions(group);
			createUI_70_ILDetail(group);

			createUI_53_DragDropHint(group);
		}
	}

	private void createUI_51_ILViewer(final Composite parent) {

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
			createUI_52_ILViewer_Table(_viewerContainer);
		}
	}

	private void createUI_52_ILViewer_Table(final Composite parent) {

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
		_ilViewer.setContentProvider(new ClientsContentProvider());

		_ilViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectImportLauncher(event.getSelection());
			}
		});

		_ilViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				onTourType_DblClick();
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

					if (selection.getFirstElement() instanceof DeviceImportLauncher) {

						final DeviceImportLauncher filterItem = (DeviceImportLauncher) selection.getFirstElement();

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

	private void createUI_53_DragDropHint(final Composite parent) {

		_lblTTHint = new Label(parent, SWT.WRAP);
		_lblTTHint.setText(Messages.Dialog_ImportConfig_Info_ConfigDragDrop);
		GridDataFactory.fillDefaults()//
				.span(3, 1)
				.applyTo(_lblTTHint);
	}

	private void createUI_60_ILActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * Button: New one tour type
			 */
			_btnTTConfig_NewOne = new Button(container, SWT.NONE);
			_btnTTConfig_NewOne.setImage(//
					net.tourbook.ui.UI.getInstance().getTourTypeImage(TourType.IMAGE_KEY_DIALOG_SELECTION));
			_btnTTConfig_NewOne.setText(Messages.Dialog_ImportConfig_Action_NewOneTourType);
			_btnTTConfig_NewOne.setToolTipText(Messages.Dialog_ImportConfig_Action_NewOneTourType_Tooltip);
			_btnTTConfig_NewOne.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					UI.openControlMenu(_btnTTConfig_NewOne);
				}
			});
			setButtonLayoutData(_btnTTConfig_NewOne);

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
			final Menu ttContextMenu = menuMgr.createContextMenu(_btnTTConfig_NewOne);
			_btnTTConfig_NewOne.setMenu(ttContextMenu);

			/*
			 * Button: New
			 */
			_btnTTConfig_New = new Button(container, SWT.NONE);
			_btnTTConfig_New.setText(Messages.App_Action_New);
			_btnTTConfig_New.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onTourType_Add(false);
				}
			});
			setButtonLayoutData(_btnTTConfig_New);

			/*
			 * Button: Duplicate
			 */
			_btnTTConfig_Duplicate = new Button(container, SWT.NONE);
			_btnTTConfig_Duplicate.setText(Messages.App_Action_Duplicate);
			_btnTTConfig_Duplicate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onTourType_Add(true);
				}
			});
			setButtonLayoutData(_btnTTConfig_Duplicate);

			/*
			 * button: remove
			 */
			_btnTTConfig_Remove = new Button(container, SWT.NONE);
			_btnTTConfig_Remove.setText(Messages.App_Action_Remove_Immediate);
			_btnTTConfig_Remove.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onTourType_Remove();
				}
			});
			setButtonLayoutData(_btnTTConfig_Remove);

			// align to the end
			final GridData gd = (GridData) _btnTTConfig_Remove.getLayoutData();
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = SWT.END;
		}
	}

	private void createUI_70_ILDetail(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_ImportConfig_Group_ImportLauncherConfig);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.indent(convertWidthInCharsToPixels(3), 0)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			createUI_72_ILName(group);
			createUI_80_ILTourType(group);
		}
	}

	private void createUI_72_ILName(final Composite parent) {

		{
			/*
			 * Config name
			 */

			// label
			_lblTTConfigName = new Label(parent, SWT.NONE);
			_lblTTConfigName.setText(Messages.Dialog_ImportConfig_Label_ConfigName);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblTTConfigName);

			// text
			_txtTTConfigName = new Text(parent, SWT.BORDER);
			_txtTTConfigName.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					onTourType_ModifyName();
				}
			});
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.applyTo(_txtTTConfigName);

		}

		{
			/*
			 * Config description
			 */

			// label
			_lblTTConfigDescription = new Label(parent, SWT.NONE);
			_lblTTConfigDescription.setText(Messages.Dialog_ImportConfig_Label_ConfigDescription);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_lblTTConfigDescription);

			// text
			_txtTTConfigDescription = new Text(parent, //
					SWT.BORDER | //
							SWT.WRAP
							| SWT.MULTI
							| SWT.V_SCROLL
							| SWT.H_SCROLL);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.hint(convertWidthInCharsToPixels(40), convertHeightInCharsToPixels(5))
					.applyTo(_txtTTConfigDescription);
		}
	}

	private void createUI_80_ILTourType(final Composite parent) {

		final SelectionAdapter ttListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectTourTypeConfig();
			}
		};

		/*
		 * Label: Additonal launcher actions
		 */
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Dialog_ImportConfig_Info_MoreImportActions);
		label.setFont(_boldFont);
		GridDataFactory.fillDefaults()//
				.span(2, 1)
				.indent(0, VERTICAL_GROUP_DISTANCE)
				.applyTo(label);

		/*
		 * Checkbox: Set tour type
		 */
		_chkTTSetTourType = new Button(parent, SWT.CHECK);
		_chkTTSetTourType.setText(Messages.Dialog_ImportConfig_Checkbox_TourType);
		_chkTTSetTourType.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_TourType_Tooltip);
		_chkTTSetTourType.addSelectionListener(ttListener);
		GridDataFactory.fillDefaults()//
				.span(2, 1)
				.indent(0, VERTICAL_GROUP_DISTANCE / 2)
				.applyTo(_chkTTSetTourType);

		/*
		 * Label: Tour type
		 */
		_lblTTTourType = new Label(parent, SWT.NONE);
		_lblTTTourType.setText(Messages.Dialog_ImportConfig_Label_TourType);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.indent(convertHorizontalDLUsToPixels(11), 0)
				.applyTo(_lblTTTourType);

		_comboTTConfig = new Combo(parent, SWT.READ_ONLY);
		_comboTTConfig.addSelectionListener(ttListener);

		// fill combo
		for (final ComboEnumEntry<?> tourTypeItem : RawDataManager.ALL_IMPORT_TOUR_TYPE_CONFIG) {
			_comboTTConfig.add(tourTypeItem.label);
		}

		// fill left column
//		new Label(parent, SWT.NONE);

		_pagebookTourType = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.span(2, 1)
				.indent(_leftPadding, 8)
				.applyTo(_pagebookTourType);
		{
			_pageTourType_NoTourType = createUI_81_Page_NoTourType(_pagebookTourType);
			_pageTourType_OneForAll = createUI_82_Page_OneForAll(_pagebookTourType);
			_pageTourType_BySpeed = createUI_83_Page_BySpeed(_pagebookTourType);
		}
	}

	/**
	 * Page: Not used
	 * 
	 * @return
	 */
	private Label createUI_81_Page_NoTourType(final PageBook parent) {

		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(UI.EMPTY_STRING);

		return label;
	}

	private Composite createUI_82_Page_OneForAll(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			_lblTTOne_TourTypeIcon = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.hint(16, 16)
					.applyTo(_lblTTOne_TourTypeIcon);
			_lblTTOne_TourTypeIcon.setText(UI.EMPTY_STRING);

			/*
			 * tour type
			 */
			_linkTTOne_TourType = new Link(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_linkTTOne_TourType);
			_linkTTOne_TourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
			_linkTTOne_TourType.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					net.tourbook.common.UI.openControlMenu(_linkTTOne_TourType);
				}
			});
		}

		return container;
	}

	private Composite createUI_83_Page_BySpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{

			createUI_84_SpeedTourTypes(container);
			createUI_86_SpeedTourType_Actions(container);
		}

		return container;
	}

	private void createUI_84_SpeedTourTypes(final Composite parent) {

		/*
		 * vertex fields container
		 */
		_speedTourType_OuterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
//				.indent(_pc.convertHorizontalDLUsToPixels(10), 0)
				.applyTo(_speedTourType_OuterContainer);

		GridLayoutFactory.fillDefaults().applyTo(_speedTourType_OuterContainer);
//		_vertexOuterContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		createUI_88_SpeedTourType_Fields();
	}

	private void createUI_86_SpeedTourType_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_actionTTSpeed_Add);
		tbm.add(_actionTTSpeed_Sort);

		tbm.update(true);
	}

	/**
	 * Create the vertex fields from the vertex list
	 * 
	 * @param parent
	 */
	private void createUI_88_SpeedTourType_Fields() {

		if (_currentIL == null) {

			updateUI_ClearVertices();

			return;
		}

		final int vertexSize = _currentIL.speedTourTypes.size();

		// check if required vertex fields are already available
		if (_spinnerTTVertex_AvgSpeed != null && _spinnerTTVertex_AvgSpeed.length == vertexSize) {
			return;
		}

		Point scrollOrigin = null;

		// dispose previous content
		if (_speedTourType_ScrolledContainer != null) {

			// get current scroll position
			scrollOrigin = _speedTourType_ScrolledContainer.getOrigin();

			_speedTourType_ScrolledContainer.dispose();
		}

		_speedTourType_Container = createUI_89_SpeedTourType_ScrolledContainer(_speedTourType_OuterContainer);
//		_vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

		/*
		 * fields
		 */
		_actionTTSpeed_Delete = new ActionDeleteVertex[vertexSize];
		_lblTTSpeed_TourTypeIcon = new Label[vertexSize];
		_lblTTSpeed_SpeedUnit = new Label[vertexSize];
		_linkTTSpeed_TourType = new Link[vertexSize];
		_spinnerTTVertex_AvgSpeed = new Spinner[vertexSize];

		_speedTourType_Container.setRedraw(false);
		{
			for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

				/*
				 * Spinner: Speed value
				 */
				final Spinner spinnerValue = new Spinner(_speedTourType_Container, SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(spinnerValue);
				spinnerValue.setMinimum(DeviceImportManager.CONFIG_SPEED_MIN);
				spinnerValue.setMaximum(DeviceImportManager.CONFIG_SPEED_MAX);
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
				linkTourType.addSelectionListener(_vertexTourTypeListener);

				/*
				 * Context menu: Tour type
				 */
				final MenuManager menuMgr = new MenuManager();
				menuMgr.setRemoveAllWhenShown(true);
				menuMgr.addMenuListener(new IMenuListener() {
					@Override
					public void menuAboutToShow(final IMenuManager menuMgr) {
						fillTourTypeMenu(menuMgr, linkTourType);
					}
				});
				final Menu ttContextMenu = menuMgr.createContextMenu(linkTourType);
				linkTourType.setMenu(ttContextMenu);

				/*
				 * Action: Delete vertex
				 */
				final ActionDeleteVertex actionDeleteVertex = new ActionDeleteVertex();
				createUI_ActionButton(_speedTourType_Container, actionDeleteVertex);

				/*
				 * Keep vertex controls
				 */
				_actionTTSpeed_Delete[vertexIndex] = actionDeleteVertex;
				_lblTTSpeed_TourTypeIcon[vertexIndex] = lblTourTypeIcon;
				_lblTTSpeed_SpeedUnit[vertexIndex] = lblUnit;
				_linkTTSpeed_TourType[vertexIndex] = linkTourType;
				_spinnerTTVertex_AvgSpeed[vertexIndex] = spinnerValue;
			}
		}
		_speedTourType_Container.setRedraw(true);

		_speedTourType_OuterContainer.layout(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_speedTourType_ScrolledContainer.setOrigin(scrollOrigin);
		}
	}

	private Composite createUI_89_SpeedTourType_ScrolledContainer(final Composite parent) {

		// scrolled container
		_speedTourType_ScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_speedTourType_ScrolledContainer);
		_speedTourType_ScrolledContainer.setExpandVertical(true);
		_speedTourType_ScrolledContainer.setExpandHorizontal(true);

		// vertex container
		final Composite vertexContainer = new Composite(_speedTourType_ScrolledContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(vertexContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(5)
				.applyTo(vertexContainer);
//		vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));

		_speedTourType_ScrolledContainer.setContent(vertexContainer);
		_speedTourType_ScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_speedTourType_ScrolledContainer.setMinSize(vertexContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		return vertexContainer;
	}

	private void createUI_90_Dashboard(final Composite parent) {

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

			createUI_90_Dashboard_1(group);
			createUI_92_Dashboard_2(group);
			createUI_92_Dashboard_3(group);

			createUI_99_Dashboard_LiveUpdate(group);
		}
	}

	private void createUI_90_Dashboard_1(final Group parent) {

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
				_spinnerTileSize = new Spinner(container, SWT.BORDER);
				_spinnerTileSize.setMinimum(RawDataView.TILE_SIZE_MIN);
				_spinnerTileSize.setMaximum(RawDataView.TILE_SIZE_MAX);
				_spinnerTileSize.addSelectionListener(_liveUpdateListener);
				_spinnerTileSize.addMouseWheelListener(_liveUpdateMouseWheelListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerTileSize);
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
				_spinnerNumHTiles = new Spinner(container, SWT.BORDER);
				_spinnerNumHTiles.setMinimum(RawDataView.NUM_HORIZONTAL_TILES_MIN);
				_spinnerNumHTiles.setMaximum(RawDataView.NUM_HORIZONTAL_TILES_MAX);
				_spinnerNumHTiles.addSelectionListener(_liveUpdateListener);
				_spinnerNumHTiles.addMouseWheelListener(_liveUpdateMouseWheelListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerNumHTiles);
			}
		}
	}

	private void createUI_92_Dashboard_2(final Group parent) {

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
				_spinnerAnimationDuration = new Spinner(container, SWT.BORDER);
				_spinnerAnimationDuration.setMinimum(0);
				_spinnerAnimationDuration.setMaximum(100);
				_spinnerAnimationDuration.setDigits(1);
				_spinnerAnimationDuration.addSelectionListener(_liveUpdateListener);
				_spinnerAnimationDuration.addMouseWheelListener(_liveUpdateMouseWheelListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerAnimationDuration);

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
				_spinnerAnimationCrazinessFactor = new Spinner(container, SWT.BORDER);
				_spinnerAnimationCrazinessFactor.setMinimum(-100);
				_spinnerAnimationCrazinessFactor.setMaximum(100);
				_spinnerAnimationCrazinessFactor.addSelectionListener(_liveUpdateListener);
				_spinnerAnimationCrazinessFactor.addMouseWheelListener(_liveUpdateMouseWheelListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerAnimationCrazinessFactor);

				// fill column
				new Label(container, SWT.NONE);
			}
		}
	}

	private void createUI_92_Dashboard_3(final Group parent) {

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
				_spinnerBgOpacity = new Spinner(container, SWT.BORDER);
				_spinnerBgOpacity.setMinimum(0);
				_spinnerBgOpacity.setMaximum(100);
				_spinnerBgOpacity.addSelectionListener(_liveUpdateListener);
				_spinnerBgOpacity.addMouseWheelListener(_liveUpdateMouseWheelListener);
			}
		}
	}

	private void createUI_99_Dashboard_LiveUpdate(final Composite parent) {

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
		defineColumn_30_Description();
	}

	/**
	 * Column: Item name
	 */
	private void defineColumn_10_ProfileName() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "configName", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Name);
		colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Name);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));
		colDef.setColumnWeightData(new ColumnWeightData(20));

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(((DeviceImportLauncher) cell.getElement()).name);
			}
		});
	}

	/**
	 * Column: Color image
	 */
	private void defineColumn_20_ColorImage() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "colorImage", SWT.LEAD); //$NON-NLS-1$
		_colDefProfileImage = colDef;

		colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_TourType);
		colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_TourType);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(15));
		colDef.setColumnWeightData(new ColumnWeightData(15));

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);

		colDef.setLabelProvider(new CellLabelProvider() {

			// !!! set dummy label provider, otherwise an error occures !!!
			@Override
			public void update(final ViewerCell cell) {}
		});
	}

	/**
	 * Column: Item description
	 */
	private void defineColumn_30_Description() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "configDescription", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Description);
		colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Description);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(25));
		colDef.setColumnWeightData(new ColumnWeightData(25));

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(((DeviceImportLauncher) cell.getElement()).description);
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

	private void enableTTControls() {

		final boolean isLauncherAvailable = _dialogConfig.deviceImportLaunchers.size() > 0;

		final boolean isILSelected = _currentIL != null;
		boolean isSetTourType = false;

		if (isILSelected) {

			if (isLauncherAvailable) {

				final Enum<TourTypeConfig> selectedTourTypeConfig = getSelectedTourTypeConfig();

				if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(selectedTourTypeConfig)) {

					isSetTourType = true;

					if (_actionTTSpeed_Delete != null) {

						for (final ActionDeleteVertex action : _actionTTSpeed_Delete) {
							action.setEnabled(isILSelected);
						}

						for (final Spinner spinner : _spinnerTTVertex_AvgSpeed) {
							spinner.setEnabled(isILSelected);
						}

						for (final Link link : _linkTTSpeed_TourType) {
							link.setEnabled(isILSelected);
						}

						for (final Label label : _lblTTSpeed_SpeedUnit) {
							label.setEnabled(isILSelected);
						}

						for (final Label label : _lblTTSpeed_TourTypeIcon) {

							if (isILSelected) {

								final Integer vertexIndex = (Integer) label.getData(DATA_KEY_VERTEX_INDEX);

								final SpeedTourType vertex = _currentIL.speedTourTypes.get(vertexIndex);
								final long tourTypeId = vertex.tourTypeId;

								label.setImage(net.tourbook.ui.UI.getInstance().getTourTypeImage(tourTypeId));

							} else {

								// the disabled image looks very ugly
								label.setImage(null);
							}
						}
					}

					_actionTTSpeed_Add.setEnabled(isILSelected);
					_actionTTSpeed_Sort.setEnabled(isILSelected && _spinnerTTVertex_AvgSpeed.length > 1);

				} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(selectedTourTypeConfig)) {

					isSetTourType = true;

					_linkTTOne_TourType.setEnabled(isILSelected);
				}
			}
		}

		_btnTTConfig_Duplicate.setEnabled(isILSelected);
		_btnTTConfig_Remove.setEnabled(isILSelected);

		_lblTTConfigName.setEnabled(isILSelected);
		_lblTTConfigDescription.setEnabled(isILSelected);
		_lblTTHint.setEnabled(isLauncherAvailable);

		_txtTTConfigName.setEnabled(isILSelected);
		_txtTTConfigDescription.setEnabled(isILSelected);
		_lblTTTourType.setEnabled(isILSelected && isSetTourType);
		_comboTTConfig.setEnabled(isILSelected && isSetTourType);

		_ilViewer.getTable().setEnabled(isLauncherAvailable);

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

			final ActionSetOneTourType action = new ActionSetOneTourType(tourType, isChecked);

			menuMgr.add(action);
		}

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenTourTypePrefs);
	}

	private void fillTourTypeMenu(final IMenuManager menuMgr, final Link linkTourType) {

		// get tour type which will be checked in the menu
		final TourType checkedTourType = null;

		final int vertexIndex = (int) linkTourType.getData(DATA_KEY_VERTEX_INDEX);

		// add all tour types to the menu
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		for (final TourType tourType : tourTypes) {

			boolean isChecked = false;
			if (checkedTourType != null && checkedTourType.getTypeId() == tourType.getTypeId()) {
				isChecked = true;
			}

			final ActionSetSpeedTourType action = new ActionSetSpeedTourType(tourType, isChecked, vertexIndex);

			menuMgr.add(action);
		}

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenTourTypePrefs);
	}

	private void fillTourTypeOneMenu(final IMenuManager menuMgr) {

		// add all tour types to the menu
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		for (final TourType tourType : tourTypes) {

			final ActionNewTourTypeOne action = new ActionNewTourTypeOne(tourType);

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

	public ImportConfig getModifiedConfig() {
		return _dialogConfig;
	}

	/**
	 * @return Returns the selected tour type configuration or <code>null</code> when a tour type
	 *         will not be set.
	 */
	@SuppressWarnings("unchecked")
	private Enum<TourTypeConfig> getSelectedTourTypeConfig() {

		final boolean isSetTourType = _chkTTSetTourType.getSelection();

		if (isSetTourType) {

			int configIndex = _comboTTConfig.getSelectionIndex();

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
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

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
		 * Vertex listener
		 */
		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
			}
		};

		_vertexTourTypeListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				UI.openControlMenu((Link) event.widget);
			}
		};
	}

	@Override
	protected void okPressed() {

		update_Model_From_UI_SelectedTourTypeItem();
		update_Model_From_UI_TourTypeItems_Sorted();

		update_Model_From_UI_LiveUpdateValues();
		update_Model_From_UI_Folder();

		super.okPressed();
	}

	private void onDispose() {

		disposeConfigImages();
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

	private void onPaintViewer(final Event event) {

		if (event.index != _columnIndexConfigImage) {
			return;
		}

		final TableItem item = (TableItem) event.item;
		final DeviceImportLauncher importConfig = (DeviceImportLauncher) item.getData();

		switch (event.type) {
		case SWT.MeasureItem:

			/*
			 * Set height also for color def, when not set and all is collapsed, the color def size
			 * will be adjusted when an item is expanded.
			 */

			event.width += importConfig.imageWidth;
//			event.height = PROFILE_IMAGE_HEIGHT;

			break;

		case SWT.PaintItem:

			final Image image = _rawDataView.getImportConfigImage(importConfig);

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
				_dialogConfig.backupFolder);

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
				_dialogConfig.deviceFolder);

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

	private void onSelectImportLauncher(final ISelection selection) {

		final DeviceImportLauncher selectedIL = (DeviceImportLauncher) ((StructuredSelection) selection)
				.getFirstElement();

		if (_currentIL == selectedIL) {
			// this is already selected
			return;
		}

		// update model from the old selected config
		update_Model_From_UI_SelectedTourTypeItem();

		// set new model
		_currentIL = selectedIL;

		update_UI_From_Model_TourTypes();

		enableTTControls();
	}

	private void onSelectTourTypeConfig() {

		final Enum<TourTypeConfig> selectedTourTypeItem = getSelectedTourTypeConfig();

		showTourTypePage(selectedTourTypeItem);

		update_Model_From_UI_SelectedTourTypeItem();
		update_UI_From_Model_TourTypes();

		enableTTControls();
	}

	private void onSpeedTourType_Add() {

		update_Model_From_UI_SelectedTourTypeItem();

		final ArrayList<SpeedTourType> speedVertices = _currentIL.speedTourTypes;

		// update model
		speedVertices.add(0, new SpeedTourType());

		// sort vertices by value
		Collections.sort(speedVertices);

		// update UI + model
		update_UI_From_Model_TourTypes();

		enableTTControls();

		// set focus to the new vertex
		_spinnerTTVertex_AvgSpeed[0].setFocus();
	}

	private void onSpeedTourType_Remove(final int vertexIndex) {

		// update model
		update_Model_From_UI_SelectedTourTypeItem();

		final ArrayList<SpeedTourType> speedVertices = _currentIL.speedTourTypes;

		final SpeedTourType removedVertex = speedVertices.get(vertexIndex);

		speedVertices.remove(removedVertex);

		// update UI
		update_UI_From_Model_TourTypes();

		enableTTControls();
	}

	private void onTourType_Add(final boolean isCopy) {

		// keep modifications
		update_Model_From_UI_SelectedTourTypeItem();

		// update model
		final ArrayList<DeviceImportLauncher> configItems = _dialogConfig.deviceImportLaunchers;
		DeviceImportLauncher newConfig;

		if (configItems.size() == 0) {

			/*
			 * Setup default config
			 */

			newConfig = createDefaultConfig();

		} else {

			if (isCopy) {

				newConfig = _currentIL.clone();

				// make the close more visible
				newConfig.name = newConfig.name + UI.SPACE + newConfig.getId();

			} else {

				newConfig = new DeviceImportLauncher();
			}

			configItems.add(newConfig);
		}

		// update UI
		_ilViewer.refresh();

		// prevent that the horizontal scrollbar is visible
		_ilViewer.getTable().getParent().layout();

		_ilViewer.setSelection(new StructuredSelection(newConfig), true);

		_txtTTConfigName.setFocus();

		if (isCopy) {
			_txtTTConfigName.selectAll();
		}
	}

	private void onTourType_AddOne(final TourType tourType) {

		// keep modifications
		update_Model_From_UI_SelectedTourTypeItem();

		// create new tt item
		final DeviceImportLauncher newTTItem = new DeviceImportLauncher();

		newTTItem.tourTypeConfig = TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL;
		newTTItem.oneTourType = tourType;
		newTTItem.name = tourType.getName();

		// update model
		_dialogConfig.deviceImportLaunchers.add(newTTItem);

		// update UI
		_ilViewer.refresh();

		// prevent that the horizontal scrollbar is visible
		_ilViewer.getTable().getParent().layout();

		_ilViewer.setSelection(new StructuredSelection(newTTItem), true);

		_txtTTConfigName.setFocus();
		_txtTTConfigName.selectAll();
	}

	private void onTourType_DblClick() {

		_txtTTConfigName.setFocus();
		_txtTTConfigName.selectAll();
	}

	private void onTourType_ModifyName() {

		if (_currentIL == null) {
			return;
		}

		// update model
		_currentIL.name = _txtTTConfigName.getText();

		// update UI
		_ilViewer.update(_currentIL, null);
	}

	private void onTourType_Remove() {

		final StructuredSelection selection = (StructuredSelection) _ilViewer.getSelection();
		final DeviceImportLauncher selectedConfig = (DeviceImportLauncher) selection.getFirstElement();

		int selectedIndex = -1;
		final ArrayList<DeviceImportLauncher> configItems = _dialogConfig.deviceImportLaunchers;

		// get index of the selected config
		for (int configIndex = 0; configIndex < configItems.size(); configIndex++) {

			final DeviceImportLauncher config = configItems.get(configIndex);

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

			_txtTTConfigName.setText(UI.EMPTY_STRING);
			_txtTTConfigDescription.setText(UI.EMPTY_STRING);

			// remove vertex fields
			createUI_88_SpeedTourType_Fields();

			enableTTControls();

		} else {

			if (selectedIndex >= configItems.size()) {
				selectedIndex--;
			}

			final DeviceImportLauncher nextConfig = configItems.get(selectedIndex);

			_ilViewer.setSelection(new StructuredSelection(nextConfig), true);
		}

		_ilViewer.getTable().setFocus();
	}

	private void onVertex_SetTourType(final int vertexIndex, final TourType tourType) {

		/*
		 * Update UI
		 */
		final Image image = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
		final Label ttIcon = _lblTTSpeed_TourTypeIcon[vertexIndex];
		final Link ttLink = _linkTTSpeed_TourType[vertexIndex];

		ttIcon.setImage(image);

		ttLink.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);
		ttLink.setData(DATA_KEY_TOUR_TYPE_ID, tourType.getTypeId());

		_speedTourType_OuterContainer.layout();
	}

	private void onVertex_Sort() {

		update_Model_From_UI_SelectedTourTypeItem();
		update_UI_From_Model_TourTypes();
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final ISelection selection = _ilViewer.getSelection();

			_ilViewer.getTable().dispose();

			createUI_52_ILViewer_Table(_viewerContainer);
			_viewerContainer.layout();

			// update viewer
			reloadViewer();

			_ilViewer.setSelection(selection);
		}
		_viewerContainer.setRedraw(true);

		return _ilViewer;
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
		final ArrayList<DeviceImportLauncher> importLaunchers = _dialogConfig.deviceImportLaunchers;

		for (final DeviceImportLauncher importLauncher : importLaunchers) {

			if (importLauncher.name.equals(stateILName)) {

				_initialImportLauncher = importLauncher;
				break;
			}
		}

		if (_initialImportLauncher == null && importLaunchers.size() > 0) {
			_initialImportLauncher = importLaunchers.get(0);
		}

		_chkLiveUpdate.setSelection(_dialogConfig.isLiveUpdate);

		_spinnerAnimationCrazinessFactor.setSelection(_dialogConfig.animationCrazinessFactor);
		_spinnerAnimationDuration.setSelection(_dialogConfig.animationDuration);
		_spinnerBgOpacity.setSelection(_dialogConfig.backgroundOpacity);
		_spinnerNumHTiles.setSelection(_dialogConfig.numHorizontalTiles);
		_spinnerTileSize.setSelection(_dialogConfig.tileSize);

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
								_dialogConfig.deviceFolder);

						_backupHistoryItems.restoreState(
								_state.getArray(STATE_BACKUP_FOLDER_HISTORY_ITEMS),
								_state.getArray(STATE_BACKUP_DEVICE_HISTORY_ITEMS),
								_dialogConfig.backupFolder);

						_comboBackupFolder.setText(_dialogConfig.backupFolder);
						_comboDeviceFolder.setText(_dialogConfig.deviceFolder);
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
		_dialogConfig.backupFolder = _comboBackupFolder.getText();
		_dialogConfig.deviceFolder = _comboDeviceFolder.getText();
	}

	private void update_Model_From_UI_LiveUpdateValues() {

		_dialogConfig.isLiveUpdate = _chkLiveUpdate.getSelection();

		_dialogConfig.animationCrazinessFactor = _spinnerAnimationCrazinessFactor.getSelection();
		_dialogConfig.animationDuration = _spinnerAnimationDuration.getSelection();
		_dialogConfig.backgroundOpacity = _spinnerBgOpacity.getSelection();
		_dialogConfig.numHorizontalTiles = _spinnerNumHTiles.getSelection();
		_dialogConfig.tileSize = _spinnerTileSize.getSelection();
	}

	private void update_Model_From_UI_OneTourType() {

		final Object tourTypeId = _linkTTOne_TourType.getData(DATA_KEY_TOUR_TYPE_ID);

		if (tourTypeId instanceof Long) {
			_currentIL.oneTourType = TourDatabase.getTourType((long) tourTypeId);
		} else {

			_currentIL.oneTourType = null;
		}

		_currentIL.setupItemImage();
	}

	/**
	 * Set data from the UI into the model.
	 */
	private void update_Model_From_UI_SelectedTourTypeItem() {

		if (_currentIL == null) {
			return;
		}

		final Enum<TourTypeConfig> selectedTourTypeConfig = getSelectedTourTypeConfig();

		_currentIL.name = _txtTTConfigName.getText();
		_currentIL.description = _txtTTConfigDescription.getText();

		_currentIL.tourTypeConfig = selectedTourTypeConfig;

		/*
		 * Set tour type data
		 */
		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(selectedTourTypeConfig)) {

			final ArrayList<SpeedTourType> speedVertices = _currentIL.speedTourTypes;

			if (_spinnerTTVertex_AvgSpeed != null) {

				final ArrayList<SpeedTourType> newVertices = new ArrayList<SpeedTourType>();

				for (int vertexIndex = 0; vertexIndex < speedVertices.size(); vertexIndex++) {

					/*
					 * create vertices from UI controls
					 */
					final Spinner spinnerAvgSpeed = _spinnerTTVertex_AvgSpeed[vertexIndex];
					final Link linkTourType = _linkTTSpeed_TourType[vertexIndex];

					final SpeedTourType speedVertex = new SpeedTourType();

					speedVertex.avgSpeed = spinnerAvgSpeed.getSelection() * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

					final Object tourTypeId = linkTourType.getData(DATA_KEY_TOUR_TYPE_ID);
					if (tourTypeId instanceof Long) {
						speedVertex.tourTypeId = (long) tourTypeId;
					} else {
						speedVertex.tourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;
					}

					newVertices.add(speedVertex);
				}

				// sort vertices by value
				Collections.sort(newVertices);

				// update model
				speedVertices.clear();
				speedVertices.addAll(newVertices);
			}

			_currentIL.setupItemImage();

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(selectedTourTypeConfig)) {

			update_Model_From_UI_OneTourType();

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

			_currentIL.setupItemImage();
		}
	}

	/**
	 * Create config list in the table sort order
	 */
	private void update_Model_From_UI_TourTypeItems_Sorted() {

		final ArrayList<DeviceImportLauncher> importTiles = _dialogConfig.deviceImportLaunchers;
		importTiles.clear();

		for (final TableItem tableItem : _ilViewer.getTable().getItems()) {

			final Object itemData = tableItem.getData();

			if (itemData instanceof DeviceImportLauncher) {
				importTiles.add((DeviceImportLauncher) itemData);
			}
		}
	}

	private void update_UI_From_Model_TourTypes() {

		if (_currentIL == null) {
			return;
		}

		final Enum<TourTypeConfig> tourTypeConfig = _currentIL.tourTypeConfig;
		final boolean isSetTourType = tourTypeConfig != null;

		_ilViewer.update(_currentIL, null);

		_txtTTConfigName.setText(_currentIL.name);
		_txtTTConfigDescription.setText(_currentIL.description);

		_chkTTSetTourType.setSelection(isSetTourType);

		if (isSetTourType) {
			_comboTTConfig.select(getTourTypeConfigIndex(tourTypeConfig));
		}

		/*
		 * Setup tour type UI
		 */

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

			_speedTourType_OuterContainer.setRedraw(false);
			{
				// check and create vertex fields
				createUI_88_SpeedTourType_Fields();

				final ArrayList<SpeedTourType> speedVertices = _currentIL.speedTourTypes;

				final int vertexSize = speedVertices.size();

				final net.tourbook.ui.UI uiInstance = net.tourbook.ui.UI.getInstance();

				for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

					final SpeedTourType speedTT = speedVertices.get(vertexIndex);
					final long tourTypeId = speedTT.tourTypeId;

					final Spinner spinnerAvgSpeed = _spinnerTTVertex_AvgSpeed[vertexIndex];
					final Link linkTourType = _linkTTSpeed_TourType[vertexIndex];
					final Label labelTourTypeIcon = _lblTTSpeed_TourTypeIcon[vertexIndex];

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
								+ uiInstance.getTourTypeLabel(tourTypeId)
								+ UI.LINK_TAG_END);
						labelTourTypeIcon.setImage(uiInstance.getTourTypeImage(tourTypeId));
					}

					// keep vertex references
					labelTourTypeIcon.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
					linkTourType.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
					spinnerAvgSpeed.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
					_actionTTSpeed_Delete[vertexIndex].setData(DATA_KEY_VERTEX_INDEX, vertexIndex);

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

		/*
		 * Clear UI for the other tour type configs that they do not be displayed when another
		 * import config is selected.
		 */
//		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {
//
//			updateUI_OneTourType(null);
//
//		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {
//
//			updateUI_ClearVertices();
//
//		} else {
//
//			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED
//
//			updateUI_ClearVertices();
//		}

		showTourTypePage(tourTypeConfig);
	}

	private void updateUI_ClearVertices() {

		if (_speedTourType_ScrolledContainer != null) {

			_speedTourType_ScrolledContainer.dispose();
			_speedTourType_ScrolledContainer = null;

			_actionTTSpeed_Delete = null;
			_lblTTSpeed_TourTypeIcon = null;
			_lblTTSpeed_SpeedUnit = null;
			_linkTTSpeed_TourType = null;
			_spinnerTTVertex_AvgSpeed = null;
		}
	}

	private void updateUI_OneTourType(final TourType tourType) {

		if (tourType == null) {

			_lblTTOne_TourTypeIcon.setImage(null);

			_linkTTOne_TourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
			_linkTTOne_TourType.setData(DATA_KEY_TOUR_TYPE_ID, null);

		} else {

			final Image image = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());

			_lblTTOne_TourTypeIcon.setImage(image);

			_linkTTOne_TourType.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);
			_linkTTOne_TourType.setData(DATA_KEY_TOUR_TYPE_ID, tourType.getTypeId());
		}

		// update the model that the table displays the correct image
		update_Model_From_UI_OneTourType();

		_ilViewer.getTable().redraw();
	}

}
