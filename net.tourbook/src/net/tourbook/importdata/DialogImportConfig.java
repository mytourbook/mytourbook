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
 * This is a template for a title area dialog
 */
public class DialogImportConfig extends TitleAreaDialog implements ITourViewer {

	private static final String			ID									= "net.tourbook.importdata.DialogAutomatedImportConfig";	//$NON-NLS-1$
	//
	private static final String			STATE_BACKUP_DEVICE_HISTORY_ITEMS	= "STATE_BACKUP_DEVICE_HISTORY_ITEMS";						//$NON-NLS-1$
	private static final String			STATE_BACKUP_FOLDER_HISTORY_ITEMS	= "STATE_BACKUP_FOLDER_HISTORY_ITEMS";						//$NON-NLS-1$
	private static final String			STATE_DEVICE_DEVICE_HISTORY_ITEMS	= "STATE_DEVICE_DEVICE_HISTORY_ITEMS";						//$NON-NLS-1$
	private static final String			STATE_DEVICE_FOLDER_HISTORY_ITEMS	= "STATE_DEVICE_FOLDER_HISTORY_ITEMS";						//$NON-NLS-1$
	private static final String			STATE_TOUR_TYPE_ITEM				= "STATE_TOUR_TYPE_ITEM";									//$NON-NLS-1$
	//
	private static final String			DATA_KEY_TOUR_TYPE_ID				= "DATA_KEY_TOUR_TYPE_ID";									//$NON-NLS-1$
	private static final String			DATA_KEY_VERTEX_INDEX				= "DATA_KEY_VERTEX_INDEX";									//$NON-NLS-1$
	//
	private static final int			CONTROL_DECORATION_WIDTH			= 6;
	//
	private final IDialogSettings		_state								= TourbookPlugin.getState(ID);
	//
	private MouseWheelListener			_defaultMouseWheelListener;
	private KeyAdapter					_folderKeyListener;
	private ModifyListener				_folderModifyListener;
	private SelectionAdapter			_liveUpdateListener;
	private MouseWheelListener			_liveUpdateMouseWheelListener;
	private SelectionAdapter			_vertexTourTypeListener;
	//
	private ActionAddVertex				_action_VertexAdd;
	private ActionDeleteVertex[]		_actionVertex_Delete;
	private ActionOpenPrefDialog		_actionOpenTourTypePrefs;
	private ActionSortVertices			_action_VertexSort;
	//

	private PixelConverter				_pc;

	private final static SpeedVertex[]	DEFAULT_VERTICES;

	static {

		DEFAULT_VERTICES = new SpeedVertex[] {
			//
			new SpeedVertex(10),
			new SpeedVertex(30),
			new SpeedVertex(150),
			new SpeedVertex(300)
		//
		};
	}

	/** Model for all configs. */
	private ImportConfig				_dialogConfig;

	/** Model for the currently selected config. */
	private TourTypeItem				_currentTTItem;

	private RawDataView					_rawDataView;

	private TableViewer					_ttItemViewer;

	private ColumnManager				_columnManager;
	private TableColumnDefinition		_colDefProfileImage;
	private int							_columnIndexConfigImage;

	private TourTypeItem				_initialTourTypeItem;

	private HashMap<Long, Image>		_configImages						= new HashMap<>();
	private HashMap<Long, Integer>		_configImageHash					= new HashMap<>();

	private HistoryItems				_deviceHistoryItems					= new HistoryItems();
	private HistoryItems				_backupHistoryItems					= new HistoryItems();

	private long						_dragStart;

	/*
	 * UI controls
	 */
	private Composite					_parent;

	private Composite					_vertexOuterContainer;
	private Composite					_vertexContainer;
	private Composite					_viewerContainer;

	private ScrolledComposite			_vertexScrolledContainer;

	private PageBook					_pagebookTourType;
	private Label						_pageTourType_NotUsed;
	private Composite					_pageTourType_OneForAll;
	private Composite					_pageTourType_BySpeed;

	private Button						_btnConfig_Duplicate;
	private Button						_btnConfig_New;
	private Button						_btnConfig_NewOne;
	private Button						_btnConfig_Remove;
	private Button						_btnSelectBackupFolder;
	private Button						_btnSelectDeviceFolder;
	private Button						_chkLiveUpdate;

	private Combo						_comboBackupFolder;
	private Combo						_comboDeviceFolder;
	private Combo						_comboTourTypeConfig;

	private Label						_lblBackupFolder;
	private Label						_lblBackupFolderPath;
	private Label						_lblConfigDescription;
	private Label						_lblConfigName;
	private Label						_lblDeviceFolder;
	private Label						_lblDeviceFolderPath;
	private Label						_lblOneTourTypeIcon;
	private Label						_lblTourType;
	private Label[]						_lblVertex_SpeedUnit;
	private Label[]						_lblVertex_TourTypeIcon;

	private Link[]						_linkVertex_TourType;
	private Link						_linkOneTourType;

	private Spinner						_spinnerBgOpacity;
	private Spinner						_spinnerTileSize;
	private Spinner						_spinnerNumHTiles;
	private Spinner[]					_spinnerVertex_AvgSpeed;

	private Text						_txtTTConfigDescription;
	private Text						_txtTTConfigName;

	private class ActionAddVertex extends Action {

		public ActionAddVertex() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_ImportConfig_Action_AddSpeed_Tooltip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__App_Add));
		}

		@Override
		public void run() {
			onVertex_Add();
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
			onVertex_Remove(_vertexIndex);
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

			final ArrayList<TourTypeItem> configItems = _dialogConfig.tourTypeItems;

			return configItems.toArray(new TourTypeItem[configItems.size()]);
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {

		}
	}

	public DialogImportConfig(final Shell parentShell, final ImportConfig importConfig, final RawDataView rawDataView) {

		super(parentShell);

		_rawDataView = rawDataView;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__options).createImage());

		setupConfigs(importConfig);
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

		_ttItemViewer.setInput(new Object());

		final Table table = _ttItemViewer.getTable();
//
//		// prevent that the horizontal scrollbar is visible
//		table.getParent().layout();

		// select first config
		_ttItemViewer.setSelection(new StructuredSelection(_initialTourTypeItem));

		// ensure that the selected also has the focus, these are 2 different things
		table.setSelection(table.getSelectionIndex());

		// set focus
		_comboDeviceFolder.setFocus();
	}

	private void createActions() {

		_action_VertexAdd = new ActionAddVertex();
		_action_VertexSort = new ActionSortVertices();

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

		final Table table = _ttItemViewer.getTable();
		final Menu tableContextMenu = menuMgr.createContextMenu(table);

		table.setMenu(tableContextMenu);

		_columnManager.createHeaderContextMenu(table, tableContextMenu);
	}

	/**
	 * Creates a configuration from the {@link #DEFAULT_VERTICES}.
	 * 
	 * @return Returns the created config.
	 */
	private TourTypeItem createDefaultConfig() {

		final TourTypeItem defaultConfig = new TourTypeItem();
		final ArrayList<SpeedVertex> speedVertices = defaultConfig.speedVertices;

		for (final SpeedVertex speedVertex : DEFAULT_VERTICES) {
			speedVertices.add(speedVertex.clone());
		}

		_dialogConfig.tourTypeItems.add(defaultConfig);

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
		final Menu ttContextMenu = menuMgr.createContextMenu(_linkOneTourType);
		_linkOneTourType.setMenu(ttContextMenu);
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */

	private void createUI(final Composite parent) {

		final Composite containerUI = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(containerUI);
		GridLayoutFactory.swtDefaults()//
				.applyTo(containerUI);
		{
			createUI_20_Folder(containerUI);
			createUI_30_Dashboard(containerUI);
			createUI_50_TourType(containerUI);
		}
	}

	private void createUI_20_Folder(final Composite parent) {
		final Group groupConfig = new Group(parent, SWT.NONE);
		groupConfig.setText(Messages.Dialog_ImportConfig_Group_TourFile);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(groupConfig);
		GridLayoutFactory.swtDefaults()//
				.numColumns(2)
				.applyTo(groupConfig);
//		groupConfig.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_21_DeviceFolder(groupConfig);
			createUI_22_BackupFolder(groupConfig);
		}
	}

	private void createUI_21_DeviceFolder(final Composite parent) {
		/*
		 * Label: device folder
		 */
		_lblDeviceFolder = new Label(parent, SWT.NONE);
		_lblDeviceFolder.setText(Messages.Dialog_ImportConfig_Label_DeviceFolder);
		_lblDeviceFolder.setToolTipText(Messages.Dialog_ImportConfig_Label_DeviceFolder_Tooltip);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lblDeviceFolder);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.extendedMargins(CONTROL_DECORATION_WIDTH, 0, 0, 0)
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
			_comboDeviceFolder.setData(_deviceHistoryItems);
			_comboDeviceFolder.setToolTipText(Messages.Dialog_ImportConfig_Combo_Folder_Tooltip);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
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

		// left column
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

	private void createUI_22_BackupFolder(final Composite parent) {

		/*
		 * Label: Backup folder
		 */
		_lblBackupFolder = new Label(parent, SWT.NONE);
		_lblBackupFolder.setText(Messages.Dialog_ImportConfig_Label_BackupFolder);
		_lblBackupFolder.setToolTipText(Messages.Dialog_ImportConfig_Label_BackupFolder_Tooltip);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.indent(0, convertVerticalDLUsToPixels(8))
				.applyTo(_lblBackupFolder);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.indent(0, convertVerticalDLUsToPixels(8))
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.extendedMargins(CONTROL_DECORATION_WIDTH, 0, 0, 0)
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
			_comboBackupFolder.setData(_backupHistoryItems);
			_comboBackupFolder.setToolTipText(Messages.Dialog_ImportConfig_Combo_Folder_Tooltip);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
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

		// left column
		new Label(parent, SWT.NONE);

		/*
		 * Label: device folder absolute path
		 */
		_lblBackupFolderPath = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.indent(CONTROL_DECORATION_WIDTH + convertHorizontalDLUsToPixels(4), 0)
				.applyTo(_lblBackupFolderPath);

		_backupHistoryItems.setControls(_comboBackupFolder, _lblBackupFolderPath);
	}

	private void createUI_30_Dashboard(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_ImportConfig_Group_Dashboard);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.indent(0, 10)
				.applyTo(group);
		GridLayoutFactory.swtDefaults()//
				.numColumns(4)
				.spacing(30, 5)
				.applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{

			createUI_90_Dashboard_1(group);
			createUI_90_Dashboard_2(group);
			createUI_92_Dashboard_3(group);
			createUI_99_Dashboard_LiveUpdate(group);
		}
	}

	private void createUI_50_TourType(final Composite parent) {

		final Group groupTourType = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.indent(0, 10)
				.applyTo(groupTourType);
		GridLayoutFactory.swtDefaults()//
				.numColumns(2)
				.applyTo(groupTourType);
		groupTourType.setText(Messages.Dialog_ImportConfig_Group_TourType);
//			groupTourType.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_CYAN));
		{
			createUI_51_TourTypeViewer_Container(groupTourType);

			final Composite container = new Composite(groupTourType, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.indent(convertWidthInCharsToPixels(3), 0)
					.applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				createUI_57_TourTypeName(container);
				createUI_80_SelectTourType(container);
			}

			createUI_54_TourTypeActions(groupTourType);
			createUI_56_DragDropHint(groupTourType);
		}
	}

	private void createUI_51_TourTypeViewer_Container(final Composite parent) {

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(SWT.DEFAULT, convertVerticalDLUsToPixels(200))
				.applyTo(_viewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
//		_viewerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_52_TourTypeViewer_Table(_viewerContainer);
		}
	}

	private void createUI_52_TourTypeViewer_Table(final Composite parent) {

		/*
		 * Create tree
		 */
		final Table table = new Table(parent, //
				SWT.H_SCROLL //
						| SWT.V_SCROLL
//						| SWT.BORDER
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
		_ttItemViewer = new TableViewer(table);

		_columnManager.createColumns(_ttItemViewer);

		_columnIndexConfigImage = _colDefProfileImage.getCreateIndex();

		_ttItemViewer.setUseHashlookup(true);
		_ttItemViewer.setContentProvider(new ClientsContentProvider());

		_ttItemViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectConfig(event.getSelection());
			}
		});

		_ttItemViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				onTourType_DblClick();
			}
		});

		createContextMenu();

		/*
		 * set drag adapter
		 */
		_ttItemViewer.addDragSupport(
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
						final ISelection selection = _ttItemViewer.getSelection();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(_dragStart = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		/*
		 * set drop adapter
		 */
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_ttItemViewer) {

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

					if (selection.getFirstElement() instanceof TourTypeItem) {

						final TourTypeItem filterItem = (TourTypeItem) selection.getFirstElement();

						final int location = getCurrentLocation();
						final Table filterTable = _ttItemViewer.getTable();

						/*
						 * check if drag was startet from this filter, remove the filter item before
						 * the new filter is inserted
						 */
						if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dragStart) {
							_ttItemViewer.remove(filterItem);
						}

						int filterIndex;

						if (_tableItem == null) {

							_ttItemViewer.add(filterItem);
							filterIndex = filterTable.getItemCount() - 1;

						} else {

							// get index of the target in the table
							filterIndex = filterTable.indexOf((TableItem) _tableItem);
							if (filterIndex == -1) {
								return false;
							}

							if (location == LOCATION_BEFORE) {
								_ttItemViewer.insert(filterItem, filterIndex);
							} else if (location == LOCATION_AFTER || location == LOCATION_ON) {
								_ttItemViewer.insert(filterItem, ++filterIndex);
							}
						}

						// reselect filter item
						_ttItemViewer.setSelection(new StructuredSelection(filterItem));

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

		_ttItemViewer.addDropSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);
	}

	private void createUI_54_TourTypeActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * Button: New one tour type
			 */
			_btnConfig_NewOne = new Button(container, SWT.NONE);
			_btnConfig_NewOne.setImage(net.tourbook.ui.UI.getInstance().getTourTypeImage(-2));
			_btnConfig_NewOne.setToolTipText(Messages.Dialog_ImportConfig_Action_NewOneTourType_Tooltip);
			_btnConfig_NewOne.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					UI.openControlMenu(_btnConfig_NewOne);
				}
			});

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
			final Menu ttContextMenu = menuMgr.createContextMenu(_btnConfig_NewOne);
			_btnConfig_NewOne.setMenu(ttContextMenu);

			/*
			 * Button: New
			 */
			_btnConfig_New = new Button(container, SWT.NONE);
			_btnConfig_New.setText(Messages.App_Action_New);
			_btnConfig_New.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onTourType_Add(false);
				}
			});
			setButtonLayoutData(_btnConfig_New);

			/*
			 * Button: Duplicate
			 */
			_btnConfig_Duplicate = new Button(container, SWT.NONE);
			_btnConfig_Duplicate.setText(Messages.App_Action_Duplicate);
			_btnConfig_Duplicate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onTourType_Add(true);
				}
			});
			setButtonLayoutData(_btnConfig_Duplicate);

			/*
			 * button: remove
			 */
			_btnConfig_Remove = new Button(container, SWT.NONE);
			_btnConfig_Remove.setText(Messages.App_Action_Remove_Immediate);
			_btnConfig_Remove.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onTourType_Remove();
				}
			});
			setButtonLayoutData(_btnConfig_Remove);

			// align to the bottom
			final GridData gd = (GridData) _btnConfig_Remove.getLayoutData();
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = SWT.END;
		}
	}

	private void createUI_56_DragDropHint(final Composite parent) {

		final Label label = new Label(parent, SWT.WRAP);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(label);
		label.setText(Messages.Dialog_ImportConfig_Info_ConfigDragDrop);
	}

	private void createUI_57_TourTypeName(final Composite parent) {

		{
			/*
			 * Config name
			 */

			// label
			_lblConfigName = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblConfigName);
			_lblConfigName.setText(Messages.Dialog_ImportConfig_Label_ConfigName);

			// text
			_txtTTConfigName = new Text(parent, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.applyTo(_txtTTConfigName);

			_txtTTConfigName.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					onTourType_ModifyName();
				}
			});
		}

		{
			/*
			 * Config description
			 */

			// label
			_lblConfigDescription = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_lblConfigDescription);
			_lblConfigDescription.setText(Messages.Dialog_ImportConfig_Label_ConfigDescription);

			// text
			_txtTTConfigDescription = new Text(parent, //
					SWT.BORDER | //
							SWT.WRAP
							| SWT.MULTI
							| SWT.V_SCROLL
							| SWT.H_SCROLL);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.hint(SWT.DEFAULT, convertHeightInCharsToPixels(3))
					.applyTo(_txtTTConfigDescription);
		}
	}

	private void createUI_80_SelectTourType(final Composite parent) {

		_lblTourType = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lblTourType);
		_lblTourType.setText(Messages.Dialog_ImportConfig_Label_TourType);

		_comboTourTypeConfig = new Combo(parent, SWT.READ_ONLY);
		_comboTourTypeConfig.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectTourTypeItem();
			}
		});

		// fill combo
		for (final ComboEnumEntry<?> tourTypeItem : RawDataManager.ALL_IMPORT_TOUR_TYPE_CONFIG) {
			_comboTourTypeConfig.add(tourTypeItem.label);
		}

		// fill left column
		new Label(parent, SWT.NONE);

		_pagebookTourType = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
//				.hint(SWT.DEFAULT, convertVerticalDLUsToPixels(96))
				.indent(0, 8)
				.applyTo(_pagebookTourType);
		{
			_pageTourType_NotUsed = createUI_81_Page_NotUsed(_pagebookTourType);
			_pageTourType_OneForAll = createUI_82_Page_OneForAll(_pagebookTourType);
			_pageTourType_BySpeed = createUI_83_Page_BySpeed(_pagebookTourType);
		}
	}

	/**
	 * Page: Not used
	 * 
	 * @return
	 */
	private Label createUI_81_Page_NotUsed(final PageBook parent) {

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
			_lblOneTourTypeIcon = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.hint(16, 16)
					.applyTo(_lblOneTourTypeIcon);
			_lblOneTourTypeIcon.setText(UI.EMPTY_STRING);

			/*
			 * tour type
			 */
			_linkOneTourType = new Link(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_linkOneTourType);
			_linkOneTourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
			_linkOneTourType.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					net.tourbook.common.UI.openControlMenu(_linkOneTourType);
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

			createUI_84_Vertices(container);
			createUI_86_VertexActions(container);
		}

		return container;
	}

	private void createUI_84_Vertices(final Composite parent) {

		/*
		 * vertex fields container
		 */
		_vertexOuterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
//				.indent(_pc.convertHorizontalDLUsToPixels(10), 0)
				.applyTo(_vertexOuterContainer);

		GridLayoutFactory.fillDefaults().applyTo(_vertexOuterContainer);
//		_vertexOuterContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		createUI_88_VertexFields();
	}

	private void createUI_86_VertexActions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_action_VertexAdd);
		tbm.add(_action_VertexSort);

		tbm.update(true);
	}

	/**
	 * Create the vertex fields from the vertex list
	 * 
	 * @param parent
	 */
	private void createUI_88_VertexFields() {

		if (_currentTTItem == null) {

			updateUI_ClearVertices();

			return;
		}

		final int vertexSize = _currentTTItem.speedVertices.size();

		// check if required vertex fields are already available
		if (_spinnerVertex_AvgSpeed != null && _spinnerVertex_AvgSpeed.length == vertexSize) {
			return;
		}

		Point scrollOrigin = null;

		// dispose previous content
		if (_vertexScrolledContainer != null) {

			// get current scroll position
			scrollOrigin = _vertexScrolledContainer.getOrigin();

			_vertexScrolledContainer.dispose();
		}

		_vertexContainer = createUI_89_VertexScrolledContainer(_vertexOuterContainer);
//		_vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

		/*
		 * fields
		 */
		_actionVertex_Delete = new ActionDeleteVertex[vertexSize];
		_lblVertex_TourTypeIcon = new Label[vertexSize];
		_lblVertex_SpeedUnit = new Label[vertexSize];
		_linkVertex_TourType = new Link[vertexSize];
		_spinnerVertex_AvgSpeed = new Spinner[vertexSize];

		_vertexContainer.setRedraw(false);
		{
			for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

				/*
				 * Spinner: Speed value
				 */
				final Spinner spinnerValue = new Spinner(_vertexContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(spinnerValue);
				spinnerValue.setMinimum(RawDataManager.CONFIG_SPEED_MIN);
				spinnerValue.setMaximum(RawDataManager.CONFIG_SPEED_MAX);
				spinnerValue.setToolTipText(Messages.Dialog_ImportConfig_Spinner_Speed_Tooltip);
				spinnerValue.addMouseWheelListener(_defaultMouseWheelListener);

				/*
				 * Label: Speed unit
				 */
				final Label lblUnit = new Label(_vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(lblUnit);
				lblUnit.setText(UI.UNIT_LABEL_SPEED);

				/*
				 * Label with icon: Tour type (CLabel cannot be disabled !!!)
				 */
				final Label lblTourTypeIcon = new Label(_vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.hint(16, 16)
						.applyTo(lblTourTypeIcon);

				/*
				 * Link: Tour type
				 */
				final Link linkTourType = new Link(_vertexContainer, SWT.NONE);
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
				createUI_ActionButton(_vertexContainer, actionDeleteVertex);

				/*
				 * Keep vertex controls
				 */
				_actionVertex_Delete[vertexIndex] = actionDeleteVertex;
				_lblVertex_TourTypeIcon[vertexIndex] = lblTourTypeIcon;
				_lblVertex_SpeedUnit[vertexIndex] = lblUnit;
				_linkVertex_TourType[vertexIndex] = linkTourType;
				_spinnerVertex_AvgSpeed[vertexIndex] = spinnerValue;
			}
		}
		_vertexContainer.setRedraw(true);

		_vertexOuterContainer.layout(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_vertexScrolledContainer.setOrigin(scrollOrigin);
		}
	}

	private Composite createUI_89_VertexScrolledContainer(final Composite parent) {

		// scrolled container
		_vertexScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_vertexScrolledContainer);
		_vertexScrolledContainer.setExpandVertical(true);
		_vertexScrolledContainer.setExpandHorizontal(true);

		// vertex container
		final Composite vertexContainer = new Composite(_vertexScrolledContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(vertexContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(5)
				.applyTo(vertexContainer);
//		vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));

		_vertexScrolledContainer.setContent(vertexContainer);
		_vertexScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_vertexScrolledContainer.setMinSize(vertexContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		return vertexContainer;
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
				 * Item size
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
		}
	}

	private void createUI_90_Dashboard_2(final Group parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
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
				_spinnerNumHTiles.setMinimum(RawDataView.NUM_HTILES_MIN);
				_spinnerNumHTiles.setMaximum(RawDataView.NUM_HTILES_MAX);
				_spinnerNumHTiles.addSelectionListener(_liveUpdateListener);
				_spinnerNumHTiles.addMouseWheelListener(_liveUpdateMouseWheelListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerNumHTiles);
			}
		}
	}

	private void createUI_92_Dashboard_3(final Group parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Background opacity
				 */
				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Dialog_ImportConfig_Label_ImportBackgroundOpacity);
				label.setToolTipText(Messages.Dialog_ImportConfig_Label_ImportBackgroundOpacity_Tooltip);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				// spinner
				_spinnerBgOpacity = new Spinner(container, SWT.BORDER);
				_spinnerBgOpacity.setMinimum(0);
				_spinnerBgOpacity.setMaximum(100);
				_spinnerBgOpacity.addSelectionListener(_liveUpdateListener);
				_spinnerBgOpacity.addMouseWheelListener(_liveUpdateMouseWheelListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerBgOpacity);
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
	}

	/**
	 * column: profile name
	 */
	private void defineColumn_10_ProfileName() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "configName", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Name);
		colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Name);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));
		colDef.setColumnWeightData(new ColumnWeightData(10));

		colDef.setIsDefaultColumn();
		colDef.setIsColumnMoveable(false);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				cell.setText(((TourTypeItem) cell.getElement()).name);
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

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));
		colDef.setColumnWeightData(new ColumnWeightData(3));

		colDef.setIsDefaultColumn();
		colDef.setIsColumnMoveable(false);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {

			// !!! set dummy label provider, otherwise an error occures !!!
			@Override
			public void update(final ViewerCell cell) {}
		});

		colDef.setControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
//				onResizeImageColumn();
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

		final Object selectedConfig = ((StructuredSelection) _ttItemViewer.getSelection()).getFirstElement();

		final boolean isConfigSelected = selectedConfig != null;

		final Enum<TourTypeConfig> selectedTourTypeConfig = getSelectedTourTypeConfig();
		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(selectedTourTypeConfig)) {

			if (_actionVertex_Delete != null) {

				for (final ActionDeleteVertex action : _actionVertex_Delete) {
					action.setEnabled(isConfigSelected);
				}

				for (final Spinner spinner : _spinnerVertex_AvgSpeed) {
					spinner.setEnabled(isConfigSelected);
				}

				for (final Link link : _linkVertex_TourType) {
					link.setEnabled(isConfigSelected);
				}

				for (final Label label : _lblVertex_SpeedUnit) {
					label.setEnabled(isConfigSelected);
				}

				for (final Label label : _lblVertex_TourTypeIcon) {

					if (isConfigSelected) {

						final Integer vertexIndex = (Integer) label.getData(DATA_KEY_VERTEX_INDEX);

						final SpeedVertex vertex = _currentTTItem.speedVertices.get(vertexIndex);
						final long tourTypeId = vertex.tourTypeId;

						label.setImage(net.tourbook.ui.UI.getInstance().getTourTypeImage(tourTypeId));

					} else {

						// the disabled image looks very ugly
						label.setImage(null);
					}
				}
			}

			_action_VertexAdd.setEnabled(isConfigSelected);
			_action_VertexSort.setEnabled(isConfigSelected && _spinnerVertex_AvgSpeed.length > 1);

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(selectedTourTypeConfig)) {

			_linkOneTourType.setEnabled(isConfigSelected);

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED
		}

		// tour file actions
//		_btnSelectBackupFolder.setEnabled(true);
//		_btnSelectDeviceFolder.setEnabled(true);
//		_comboBackupPath.setEnabled(true);
//		_comboDevicePath.setEnabled(true);
//		_lblBackupFolder.setEnabled(true);
//		_lblDeviceFolder.setEnabled(true);

		_btnConfig_Duplicate.setEnabled(isConfigSelected);
		_btnConfig_Remove.setEnabled(isConfigSelected);

		_comboTourTypeConfig.setEnabled(isConfigSelected);

		_lblConfigName.setEnabled(isConfigSelected);
		_lblTourType.setEnabled(isConfigSelected);

		_txtTTConfigName.setEnabled(isConfigSelected);
		_txtTTConfigDescription.setEnabled(isConfigSelected);
	}

	private void fillTourTypeMenu(final IMenuManager menuMgr) {

		// get tour type which will be checked in the menu
		final TourType checkedTourType = _currentTTItem.oneTourType;

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

	@SuppressWarnings("unchecked")
	private Enum<TourTypeConfig> getSelectedTourTypeConfig() {

		final int configIndex = _comboTourTypeConfig.getSelectionIndex();

		final ComboEnumEntry<?> selectedItem = RawDataManager.ALL_IMPORT_TOUR_TYPE_CONFIG[configIndex];

		return (Enum<TourTypeConfig>) selectedItem.value;
	}

	private int getTourTypeConfigIndex(final Enum<TourTypeConfig> tourTypeConfig) {

		if (tourTypeConfig != null) {

			final ComboEnumEntry<?>[] allImportTourTypeConfig = RawDataManager.ALL_IMPORT_TOUR_TYPE_CONFIG;

			for (int configIndex = 0; configIndex < allImportTourTypeConfig.length; configIndex++) {

				final ComboEnumEntry<?> tourtypeConfig = allImportTourTypeConfig[configIndex];

				if (tourtypeConfig.value.equals(tourTypeConfig)) {
					return configIndex;
				}
			}
		}

		return 0;
	}

	@Override
	public ColumnViewer getViewer() {
		return _ttItemViewer;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		/*
		 * Path listener
		 */
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

	private void onFolder_KeyPressed(final KeyEvent event) {

		final boolean isCtrlKey = (event.stateMask & SWT.MOD1) > 0;

		if (isCtrlKey && event.keyCode == SWT.DEL) {

			// delete this item from the history

			final Combo combo = (Combo) event.widget;
			final HistoryItems historyItems = (HistoryItems) combo.getData();

			historyItems.removeFromHistory(combo.getText());
		}
	}

	private void onFolder_Modified(final ModifyEvent event) {

		final Combo combo = (Combo) event.widget;
		final HistoryItems historyItems = (HistoryItems) combo.getData();

		historyItems.validateModifiedPath(this);
	}

	private void onPaintViewer(final Event event) {

		if (event.index != _columnIndexConfigImage) {
			return;
		}

		final TableItem item = (TableItem) event.item;
		final TourTypeItem importConfig = (TourTypeItem) item.getData();

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

	private void onSelectConfig(final ISelection selection) {

		final TourTypeItem selectedConfig = (TourTypeItem) ((StructuredSelection) selection).getFirstElement();

		if (_currentTTItem == selectedConfig) {
			// this is already selected
			return;
		}

		// update model from the old selected config
		update_Model_From_UI_SelectedTourTypeItem();

		// set new model
		_currentTTItem = selectedConfig;

		update_UI_From_Model_TourTypes();

		enableControls();
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

	private void onSelectTourTypeItem() {

		final Enum<TourTypeConfig> selectedTourTypeItem = getSelectedTourTypeConfig();

		selectTourTypePage(selectedTourTypeItem);

		update_Model_From_UI_SelectedTourTypeItem();
		update_UI_From_Model_TourTypes();
	}

	private void onTourType_Add(final boolean isCopy) {

		// keep modifications
		update_Model_From_UI_SelectedTourTypeItem();

		// update model
		final ArrayList<TourTypeItem> configItems = _dialogConfig.tourTypeItems;
		TourTypeItem newConfig;

		if (configItems.size() == 0) {

			/*
			 * Setup default config
			 */

			newConfig = createDefaultConfig();

		} else {

			if (isCopy) {

				newConfig = _currentTTItem.clone();

				// make the close more visible
				newConfig.name = newConfig.name + UI.SPACE + newConfig.getId();

			} else {

				newConfig = new TourTypeItem();
			}

			configItems.add(newConfig);
		}

		// update UI
		_ttItemViewer.refresh();

		// prevent that the horizontal scrollbar is visible
		_ttItemViewer.getTable().getParent().layout();

		_ttItemViewer.setSelection(new StructuredSelection(newConfig), true);

		_txtTTConfigName.setFocus();

		if (isCopy) {
			_txtTTConfigName.selectAll();
		}
	}

	private void onTourType_AddOne(final TourType tourType) {

		// keep modifications
		update_Model_From_UI_SelectedTourTypeItem();

		// create new tt item
		final TourTypeItem newTTItem = new TourTypeItem();

		newTTItem.tourTypeConfig = TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL;
		newTTItem.oneTourType = tourType;
		newTTItem.name = tourType.getName();

		// update model
		_dialogConfig.tourTypeItems.add(newTTItem);

		// update UI
		_ttItemViewer.refresh();

		// prevent that the horizontal scrollbar is visible
		_ttItemViewer.getTable().getParent().layout();

		_ttItemViewer.setSelection(new StructuredSelection(newTTItem), true);

		_txtTTConfigName.setFocus();
		_txtTTConfigName.selectAll();
	}

	private void onTourType_DblClick() {

		_txtTTConfigName.setFocus();
		_txtTTConfigName.selectAll();
	}

	private void onTourType_ModifyName() {

		if (_currentTTItem == null) {
			return;
		}

		// update model
		_currentTTItem.name = _txtTTConfigName.getText();

		// update UI
		_ttItemViewer.update(_currentTTItem, null);
	}

	private void onTourType_Remove() {

		final StructuredSelection selection = (StructuredSelection) _ttItemViewer.getSelection();
		final TourTypeItem selectedConfig = (TourTypeItem) selection.getFirstElement();

		int selectedIndex = 0;
		final ArrayList<TourTypeItem> configItems = _dialogConfig.tourTypeItems;

		// get index of the selected config
		for (int configIndex = 0; configIndex < configItems.size(); configIndex++) {

			final TourTypeItem config = configItems.get(configIndex);

			if (config.equals(selectedConfig)) {
				selectedIndex = configIndex;
				break;
			}
		}

		// update model
		configItems.remove(selectedIndex);

		// update UI
		_ttItemViewer.refresh();

		// select config at the same position

		if (configItems.size() == 0) {

			// all configs are removed, setup empty UI

			_currentTTItem = null;

			_comboDeviceFolder.setText(UI.EMPTY_STRING);
			_txtTTConfigName.setText(UI.EMPTY_STRING);
			_txtTTConfigDescription.setText(UI.EMPTY_STRING);

			// remove vertex fields
			createUI_88_VertexFields();

			enableControls();

		} else {

			if (selectedIndex >= configItems.size()) {
				selectedIndex--;
			}

			final TourTypeItem nextConfig = configItems.get(selectedIndex);

			_ttItemViewer.setSelection(new StructuredSelection(nextConfig), true);
		}

		_ttItemViewer.getTable().setFocus();
	}

	private void onVertex_Add() {

		update_Model_From_UI_SelectedTourTypeItem();

		final ArrayList<SpeedVertex> speedVertices = _currentTTItem.speedVertices;

		// update model
		speedVertices.add(0, new SpeedVertex());

		// sort vertices by value
		Collections.sort(speedVertices);

		// update UI + model
		update_UI_From_Model_TourTypes();

		enableControls();

		// set focus to the new vertex
		_spinnerVertex_AvgSpeed[0].setFocus();
	}

	private void onVertex_Remove(final int vertexIndex) {

		// update model
		update_Model_From_UI_SelectedTourTypeItem();

		final ArrayList<SpeedVertex> speedVertices = _currentTTItem.speedVertices;

		final SpeedVertex removedVertex = speedVertices.get(vertexIndex);

		speedVertices.remove(removedVertex);

		// update UI
		update_UI_From_Model_TourTypes();

		enableControls();
	}

	private void onVertex_SetTourType(final int vertexIndex, final TourType tourType) {

		/*
		 * Update UI
		 */
		final Image image = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
		final Label ttIcon = _lblVertex_TourTypeIcon[vertexIndex];
		final Link ttLink = _linkVertex_TourType[vertexIndex];

		ttIcon.setImage(image);

		ttLink.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);
		ttLink.setData(DATA_KEY_TOUR_TYPE_ID, tourType.getTypeId());

		_vertexOuterContainer.layout();
	}

	private void onVertex_Sort() {

		update_Model_From_UI_SelectedTourTypeItem();
		update_UI_From_Model_TourTypes();
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final ISelection selection = _ttItemViewer.getSelection();

			_ttItemViewer.getTable().dispose();

			createUI_52_TourTypeViewer_Table(_viewerContainer);
			_viewerContainer.layout();

			// update viewer
			reloadViewer();

			_ttItemViewer.setSelection(selection);
		}
		_viewerContainer.setRedraw(true);

		return _ttItemViewer;
	}

	@Override
	public void reloadViewer() {

		_ttItemViewer.setInput(this);
	}

	private void restoreState() {

		/*
		 * Reselect previous selected config
		 */
		final String stateConfigName = Util.getStateString(_state, STATE_TOUR_TYPE_ITEM, UI.EMPTY_STRING);
		final ArrayList<TourTypeItem> tourTypeItems = _dialogConfig.tourTypeItems;

		for (final TourTypeItem config : tourTypeItems) {

			if (config.name.equals(stateConfigName)) {

				_initialTourTypeItem = config;
				break;
			}
		}

		if (_initialTourTypeItem == null) {
			_initialTourTypeItem = tourTypeItems.get(0);
		}

		_chkLiveUpdate.setSelection(_dialogConfig.isLiveUpdate);

		_spinnerBgOpacity.setSelection(_dialogConfig.backgroundOpacity);
		_spinnerNumHTiles.setSelection(_dialogConfig.numHorizontalTiles);
		_spinnerTileSize.setSelection(_dialogConfig.tileSize);

		/*
		 * Loading the volume information can deleay the startup of the dialog
		 */
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

		_state.put(STATE_TOUR_TYPE_ITEM, _currentTTItem == null ? UI.EMPTY_STRING : _currentTTItem.name);

		_columnManager.saveState(_state);

		_backupHistoryItems.saveState(_state, STATE_BACKUP_FOLDER_HISTORY_ITEMS, STATE_BACKUP_DEVICE_HISTORY_ITEMS);
		_deviceHistoryItems.saveState(_state, STATE_DEVICE_FOLDER_HISTORY_ITEMS, STATE_DEVICE_DEVICE_HISTORY_ITEMS);
	}

	private void selectTourTypePage(final Enum<TourTypeConfig> selectedConfig) {

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(selectedConfig)) {

			_pagebookTourType.showPage(_pageTourType_BySpeed);

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(selectedConfig)) {

			_pagebookTourType.showPage(_pageTourType_OneForAll);

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

			_pagebookTourType.showPage(_pageTourType_NotUsed);
		}
	}

	/**
	 * Clone original configs, only the backup will be modified in the dialog.
	 * 
	 * @param importConfig
	 */
	private void setupConfigs(final ImportConfig importConfig) {

		_dialogConfig = new ImportConfig();
		final ArrayList<TourTypeItem> configItems = _dialogConfig.tourTypeItems = new ArrayList<>();

		for (final TourTypeItem autoImportConfig : importConfig.tourTypeItems) {
			configItems.add(autoImportConfig.clone());
		}

		if (configItems.size() == 0) {

			// Setup default config
			createDefaultConfig();
		}

		_dialogConfig.isLiveUpdate = importConfig.isLiveUpdate;

		_dialogConfig.backgroundOpacity = importConfig.backgroundOpacity;
		_dialogConfig.numHorizontalTiles = importConfig.numHorizontalTiles;
		_dialogConfig.tileSize = importConfig.tileSize;

		_dialogConfig.backupFolder = importConfig.backupFolder;
		_dialogConfig.deviceFolder = importConfig.deviceFolder;
	}

	private void update_Model_From_UI_Folder() {

		_dialogConfig.backupFolder = _comboBackupFolder.getText();
		_dialogConfig.deviceFolder = _comboDeviceFolder.getText();
	}

	private void update_Model_From_UI_LiveUpdateValues() {

		_dialogConfig.isLiveUpdate = _chkLiveUpdate.getSelection();

		_dialogConfig.backgroundOpacity = _spinnerBgOpacity.getSelection();
		_dialogConfig.numHorizontalTiles = _spinnerNumHTiles.getSelection();
		_dialogConfig.tileSize = _spinnerTileSize.getSelection();
	}

	private void update_Model_From_UI_OneTourType() {

		final Object tourTypeId = _linkOneTourType.getData(DATA_KEY_TOUR_TYPE_ID);

		if (tourTypeId instanceof Long) {
			_currentTTItem.oneTourType = TourDatabase.getTourType((long) tourTypeId);
		} else {

			_currentTTItem.oneTourType = null;
		}

		_currentTTItem.setupItemImage();
	}

	/**
	 * Set data from the UI into the model.
	 */
	private void update_Model_From_UI_SelectedTourTypeItem() {

		if (_currentTTItem == null) {
			return;
		}

		final Enum<TourTypeConfig> selectedTourTypeConfig = getSelectedTourTypeConfig();

		_currentTTItem.tourTypeConfig = selectedTourTypeConfig;
		_currentTTItem.name = _txtTTConfigName.getText();
		_currentTTItem.description = _txtTTConfigDescription.getText();

		/*
		 * Set tour type data
		 */
		if (selectedTourTypeConfig.equals(TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED)) {

			final ArrayList<SpeedVertex> speedVertices = _currentTTItem.speedVertices;

			if (_spinnerVertex_AvgSpeed != null) {

				final ArrayList<SpeedVertex> newVertices = new ArrayList<SpeedVertex>();

				for (int vertexIndex = 0; vertexIndex < speedVertices.size(); vertexIndex++) {

					/*
					 * create vertices from UI controls
					 */
					final Spinner spinnerAvgSpeed = _spinnerVertex_AvgSpeed[vertexIndex];
					final Link linkTourType = _linkVertex_TourType[vertexIndex];

					final SpeedVertex speedVertex = new SpeedVertex();

					speedVertex.avgSpeed = spinnerAvgSpeed.getSelection();

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

			_currentTTItem.setupItemImage();

		} else if (selectedTourTypeConfig.equals(TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL)) {

			update_Model_From_UI_OneTourType();

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

			_currentTTItem.setupItemImage();
		}

	}

	/**
	 * Create config list in the table sort order
	 */
	private void update_Model_From_UI_TourTypeItems_Sorted() {

		final ArrayList<TourTypeItem> importTiles = _dialogConfig.tourTypeItems;
		importTiles.clear();

		for (final TableItem tableItem : _ttItemViewer.getTable().getItems()) {

			final Object itemData = tableItem.getData();

			if (itemData instanceof TourTypeItem) {
				importTiles.add((TourTypeItem) itemData);
			}
		}
	}

	private void update_UI_From_Model_TourTypes() {

		if (_currentTTItem == null) {
			return;
		}

		final Enum<TourTypeConfig> tourTypeConfig = _currentTTItem.tourTypeConfig;

		_ttItemViewer.update(_currentTTItem, null);

		_comboTourTypeConfig.select(getTourTypeConfigIndex(tourTypeConfig));
		_txtTTConfigName.setText(_currentTTItem.name);

		/*
		 * Setup tour type UI
		 */
		selectTourTypePage(tourTypeConfig);

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

			_vertexOuterContainer.setRedraw(false);
			{
				// check and create vertex fields
				createUI_88_VertexFields();

				final ArrayList<SpeedVertex> speedVertices = _currentTTItem.speedVertices;

				final int vertexSize = speedVertices.size();

				final net.tourbook.ui.UI uiInstance = net.tourbook.ui.UI.getInstance();

				for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

					final SpeedVertex vertex = speedVertices.get(vertexIndex);
					final long tourTypeId = vertex.tourTypeId;

					final Spinner spinnerAvgSpeed = _spinnerVertex_AvgSpeed[vertexIndex];
					final Link linkTourType = _linkVertex_TourType[vertexIndex];
					final Label labelTourTypeIcon = _lblVertex_TourTypeIcon[vertexIndex];

					// update UI
					spinnerAvgSpeed.setSelection(vertex.avgSpeed);

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
					_actionVertex_Delete[vertexIndex].setData(DATA_KEY_VERTEX_INDEX, vertexIndex);

				}
			}
			_vertexOuterContainer.setRedraw(true);

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {

			TourType tourType = null;

			final TourType oneTourType = _currentTTItem.oneTourType;
			if (oneTourType != null) {

				final long tourTypeId = oneTourType.getTypeId();
				tourType = TourDatabase.getTourType(tourTypeId);
			}

			updateUI_OneTourType(tourType);

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED
		}

		/*
		 * Clear UI for the other tour type configs that they do not be displayed when another
		 * import config is selected.
		 */
		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

			updateUI_OneTourType(null);

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {

			updateUI_ClearVertices();

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

			updateUI_ClearVertices();
			updateUI_OneTourType(null);
		}
	}

	private void updateUI_ClearVertices() {

		if (_vertexScrolledContainer != null) {

			_vertexScrolledContainer.dispose();
			_vertexScrolledContainer = null;

			_actionVertex_Delete = null;
			_lblVertex_TourTypeIcon = null;
			_lblVertex_SpeedUnit = null;
			_linkVertex_TourType = null;
			_spinnerVertex_AvgSpeed = null;
		}
	}

	private void updateUI_OneTourType(final TourType tourType) {

		if (tourType == null) {

			_lblOneTourTypeIcon.setImage(null);

			_linkOneTourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
			_linkOneTourType.setData(DATA_KEY_TOUR_TYPE_ID, null);

		} else {

			final Image image = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());

			_lblOneTourTypeIcon.setImage(image);

			_linkOneTourType.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);
			_linkOneTourType.setData(DATA_KEY_TOUR_TYPE_ID, tourType.getTypeId());
		}

		// update the model that the table displays the correct image
		update_Model_From_UI_OneTourType();

		_ttItemViewer.getTable().redraw();
	}

}
