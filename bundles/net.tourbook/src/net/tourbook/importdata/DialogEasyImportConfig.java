/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;
import static org.eclipse.swt.events.FocusListener.focusLostAdapter;
import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.FileSystemManager;
import net.tourbook.common.NIO;
import net.tourbook.common.TourbookFileSystem;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.EmptyContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ComboEnumEntry;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.CadenceMultiplier;
import net.tourbook.tour.TourManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ComboViewerCadence;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.action.Action;
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
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
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
import org.joda.time.Period;
import org.joda.time.PeriodType;

/**
 * Dialog to configure the device import.
 */
public class DialogEasyImportConfig extends TitleAreaDialog implements IActionResetToDefault {

   public static final String           ID                                = "DialogEasyImportConfig";               //$NON-NLS-1$
   //
   private static final String          STATE_BACKUP_DEVICE_HISTORY_ITEMS = "STATE_BACKUP_DEVICE_HISTORY_ITEMS";    //$NON-NLS-1$
   private static final String          STATE_BACKUP_FOLDER_HISTORY_ITEMS = "STATE_BACKUP_FOLDER_HISTORY_ITEMS";    //$NON-NLS-1$
   private static final String          STATE_DEVICE_DEVICE_HISTORY_ITEMS = "STATE_DEVICE_DEVICE_HISTORY_ITEMS";    //$NON-NLS-1$
   public static final String           STATE_DEVICE_FOLDER_HISTORY_ITEMS = "STATE_DEVICE_FOLDER_HISTORY_ITEMS";    //$NON-NLS-1$
   private static final String          STATE_SELECTED_IMPORT_LAUNCHER    = "STATE_SELECTED_IMPORT_LAUNCHER";       //$NON-NLS-1$
   private static final String          STATE_SELECTED_TAB_FOLDER         = "STATE_SELECTED_TAB_FOLDER";            //$NON-NLS-1$
   //
   private static final String          DATA_KEY_TOUR_TYPE_ID             = "DATA_KEY_TOUR_TYPE_ID";                //$NON-NLS-1$
   private static final String          DATA_KEY_SPEED_TOUR_TYPE_INDEX    = "DATA_KEY_SPEED_TOUR_TYPE_INDEX";       //$NON-NLS-1$
   //
   private static final int             CONTROL_DECORATION_WIDTH          = 6;
   private static final String          CSS_PX                            = "px";                                   //$NON-NLS-1$
   //
   private final IPreferenceStore       _prefStore                        = TourbookPlugin.getPrefStore();
   private final IDialogSettings        _state                            = TourbookPlugin.getState(ID);
   private final IDialogSettings        _stateIC                          = TourbookPlugin.getState(ID + "_IC");    //$NON-NLS-1$
   private final IDialogSettings        _stateIL                          = TourbookPlugin.getState(ID + "_IL");    //$NON-NLS-1$
   private final IDialogSettings        _stateRawDataView                 = TourbookPlugin.getState(RawDataView.ID);
   //
   private IPropertyChangeListener      _prefChangeListener;
   //
   private SelectionListener            _defaultModify_Listener;
   private MouseWheelListener           _defaultModify_MouseWheelListener;
   private MouseWheelListener           _defaultMouseWheelListener;
   private SelectionListener            _icSelectionListener;
   private FocusListener                _ic_FolderFocusListener;
   private KeyListener                  _ic_FolderKeyListener;
   private ModifyListener               _ic_FolderModifyListener;
   private ModifyListener               _icModifyListener;
   private ModifyListener               _ilModifyListener;
   private SelectionListener            _ilSelectionListener;
   private SelectionListener            _liveUpdateListener;
   private MouseWheelListener           _liveUpdateMouseWheelListener;
   private SelectionListener            _speedTourTypeListener;
   //
   private ActionOpenPrefDialog         _actionOpenTourTypePrefs;
   private ActionResetToDefaults        _actionRestoreDefaults;
   private ActionSpeedTourType_Add      _actionTTSpeed_Add;
   private ActionSpeedTourType_Delete[] _actionTTSpeed_Delete;
   private ActionSpeedTourType_Sort     _actionTTSpeed_Sort;
   //
   private PixelConverter               _pc;

   /** Model for all configurations. */
   private EasyConfig                   _dialogEasyConfig;

   /** Model for the currently selected configuration. */
   private ImportConfig                 _selectedIC;
   private ImportLauncher               _selectedIL;
   //
   private RawDataView                  _rawDataView;
   private TableViewer                  _icViewer;
   private TableViewer                  _ilViewer;
   private ICColumnViewer               _icColumnViewer                   = new ICColumnViewer();
   private ILColumnViewer               _ilColumnViewer                   = new ILColumnViewer();
   private ColumnManager                _icColumnManager;
   private ColumnManager                _ilColumnManager;
   private EasyLauncherUtils            _ilEasyLauncherUtils              = new EasyLauncherUtils();
   //
   private int                          _ilColumnIndexConfigImage;
   //
   private HashMap<Long, Image>         _configImages                     = new HashMap<>();
   private HashMap<Long, Integer>       _configImageHash                  = new HashMap<>();
   //
   private HistoryItems                 _deviceHistoryItems               = new HistoryItems();
   private HistoryItems                 _backupHistoryItems               = new HistoryItems();
   //
   private long                         _dragStart;
   private int                          _leftPadding;
   private int                          _defaultPaneWidth;
   private boolean                      _isInUIUpdate;

   private int                          _initialTab;

   private final PeriodType             _durationTemplate                 = PeriodType

         .yearMonthDayTime()

         // hide these components
         .withMillisRemoved();

   /**
    * Contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the different section to the same width
    */
   private final ArrayList<Control>     _firstColumnControls              = new ArrayList<>();

   /*
    * UI controls
    */
   private Composite            _parent;
   private Composite            _speedTourType_OuterContainer;
   private Composite            _speedTourType_Container;
   private Composite            _icViewerContainer;
   private Composite            _ilViewerContainer;
   private ScrolledComposite    _speedTourType_ScrolledContainer;
   //
   private PageBook             _pagebookTourType;
   //
   private Label                _pageTourType_NoTourType;
   private Composite            _pageTourType_OneForAll;
   private Composite            _pageTourType_BySpeed;
   //
   private Button               _chkOptions_DisplayAbsoluteFilePath;
   private Button               _chkOptions_LiveUpdate;
   private Button               _chkOptions_LogDetails;
   //
   private Button               _chkIC_CreateBackup;
   private Button               _chkIC_DeleteDeviceFiles;
   private Button               _chkIC_ImportFiles;
   private Button               _chkIC_TurnOffWatching;
   //
   private Button               _chkIL_AdjustTemperature;
   private Button               _chkIL_ReplaceElevationFromSRTM;
   private Button               _chkIL_ReplaceFirstTimeSliceElevation;
   private Button               _chkIL_RetrieveWeatherData;
   private Button               _chkIL_SaveTour;
   private Button               _chkIL_SetLastMarker;
   private Button               _chkIL_SetTourType;
   private Button               _chkIL_ShowInDashboard;
   //
   private Button               _chkOptions_ShowTile_CloudApps;
   private Button               _chkOptions_ShowTile_Files;
   private Button               _chkOptions_ShowTile_FossilUI;
   private Button               _chkOptions_ShowTile_SerialPort;
   private Button               _chkOptions_ShowTile_SerialPortWithConfig;
   //
   private Button               _btnIC_Duplicate;
   private Button               _btnIC_New;
   private Button               _btnIC_Remove;
   private Button               _btnIC_SelectBackupFolder;
   private Button               _btnIC_SelectDeviceFolder;
   private Button               _btnIL_Duplicate;
   private Button               _btnIL_New;
   private Button               _btnIL_NewOne;
   private Button               _btnIL_Remove;
   //
   private Combo                _comboIC_BackupFolder;
   private Combo                _comboIC_DeviceFolder;
   private Combo                _comboIC_DeviceType;
   private Combo                _comboIL_TourType;
   private ComboViewerCadence   _comboIL_One_TourType_Cadence;
   private ComboViewerCadence[] _comboTT_Cadence;
   //
   private Image                _imageFileSystem;
   //
   private Label                _lblIC_FileSystemImage;
   private Label                _lblIC_ConfigName;
   private Label                _lblIC_BackupFolder;
   private Label                _lblIC_DeleteFilesInfo;
   private Label                _lblIC_DeviceFolder;
   private Label                _lblIL_AvgTemperature;
   private Label                _lblIL_AvgTemperature_Unit;
   private Label                _lblIL_ConfigDescription;
   private Label                _lblIL_ConfigName;
   private Label                _lblIL_LastMarker;
   private Label                _lblIL_LastMarkerDistanceUnit;
   private Label                _lblIL_LastMarkerText;
   private Label                _lblIL_One_TourTypeIcon;
   private Label                _lblIL_One_TourTypeCadenceLabel;
   private Label                _lblIL_TemperatureAdjustmentDuration;
   private Label                _lblIL_TemperatureAdjustmentDuration_Unit;
   private Label[]              _lblTT_Speed_SpeedUnit;
   private Label[]              _lblTT_Speed_TourTypeIcon;
   //
   private Link[]               _linkTT_Speed_TourType;
   private Link                 _linkTT_One_TourType;
   private Link                 _linkIC_LocalFolderPath;
   private Link                 _linkIC_DeviceFolderPath;
   private Link                 _linkIC_ILActions;
   //
   private Spinner              _spinnerDash_AnimationCrazinessFactor;
   private Spinner              _spinnerDash_AnimationDuration;
   private Spinner              _spinnerDash_BgOpacity;
   private Spinner              _spinnerDash_NumHTiles;
   private Spinner              _spinnerDash_StateTooltipWidth;
   private Spinner              _spinnerDash_TileSize;
   private Spinner              _spinnerIL_AvgTemperature;
   private Spinner              _spinnerIL_LastMarkerDistance;
   private Spinner              _spinnerIL_TemperatureAdjustmentDuration;
   private Spinner[]            _spinnerTT_Speed_AvgSpeed;
   //
   private CTabFolder           _tabFolderEasy;
   //
   private Text                 _txtIC_DeviceFiles;
   private Text                 _txtIC_ConfigName;
   private Text                 _txtIL_ConfigDescription;
   private Text                 _txtIL_ConfigName;
   private Text                 _txtIL_LastMarker;

   private class ActionIL_NewOneTourType extends Action {

      private TourType _tourType;

      /**
       * @param tourType
       */
      public ActionIL_NewOneTourType(final TourType tourType) {

         super(tourType.getName(), AS_CHECK_BOX);

         // show image when tour type can be selected, disabled images look ugly on win
         final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourType.getTypeId());
         setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));

         _tourType = tourType;
      }

      @Override
      public void run() {
         onIL_AddOne(_tourType);
      }
   }

   private class ActionIL_SetOneTourType extends Action {

      private TourType __tourType;

      /**
       * @param tourType
       */
      public ActionIL_SetOneTourType(final TourType tourType, final boolean isChecked) {

         super(tourType.getName(), AS_CHECK_BOX);

         if (isChecked == false) {

            // show image when tour type can be selected, disabled images look ugly on win
            final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourType.getTypeId());
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
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Add));
      }

      @Override
      public void run() {
         onSpeed_IL_TT_Add();
      }
   }

   private class ActionSpeedTourType_Delete extends Action {

      private int _speedTTIndex;

      public ActionSpeedTourType_Delete() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.Dialog_ImportConfig_Action_RemoveSpeed_Tooltip);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Trash));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Trash_Disabled));
      }

      @Override
      public void run() {
         onSpeed_IL_TT_Remove(_speedTTIndex);
      }

      public void setData(final int speedTTIndex) {

         _speedTTIndex = speedTTIndex;
      }
   }

   private class ActionSpeedTourType_SetInMenu extends Action {

      private int      _speedTTIndex;
      private TourType _tourType;

      /**
       * @param tourType
       * @param speedTTIndex
       */
      public ActionSpeedTourType_SetInMenu(final TourType tourType, final boolean isChecked, final int speedTTIndex) {

         super(tourType.getName(), AS_CHECK_BOX);

         _speedTTIndex = speedTTIndex;

         if (isChecked == false) {

            // show image when tour type can be selected, disabled images look ugly on win
            final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourType.getTypeId());
            setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));
         }

         setChecked(isChecked);
         setEnabled(isChecked == false);

         _tourType = tourType;
      }

      @Override
      public void run() {
         onSpeed_IL_TT_SetTourType(_speedTTIndex, _tourType);
      }
   }

   private class ActionSpeedTourType_Sort extends Action {

      public ActionSpeedTourType_Sort() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.Dialog_ImportConfig_Action_SortBySpeed_Tooltip);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Sort));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Sort_Disabled));
      }

      @Override
      public void run() {
         onSpeed_IL_TT_Sort();
      }
   }

   public class ICColumnViewer implements ITourViewer {

      @Override
      public ColumnManager getColumnManager() {
         return _icColumnManager;
      }

      @Override
      public ColumnViewer getViewer() {
         return _icViewer;
      }

      @Override
      public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

         _icViewerContainer.setRedraw(false);
         {
            final ISelection selection = _icViewer.getSelection();

            _icViewer.getTable().dispose();

            createUI_212_IC_ViewerTable(_icViewerContainer);
            _icViewerContainer.layout();

            // update viewer
            reloadViewer();

            _icViewer.setSelection(selection);
         }
         _icViewerContainer.setRedraw(true);

         return _icViewer;
      }

      @Override
      public void reloadViewer() {

         _icViewer.setInput(this);
      }

      @Override
      public void updateColumnHeader(final ColumnDefinition colDef) {}
   }

   private class ICContentProvider implements IStructuredContentProvider {

      public ICContentProvider() {}

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {

         final ArrayList<ImportConfig> configItems = _dialogEasyConfig.importConfigs;

         return configItems.toArray(new ImportConfig[configItems.size()]);
      }

      @Override
      public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
   }

   public class ILColumnViewer implements ITourViewer {

      @Override
      public ColumnManager getColumnManager() {
         return _ilColumnManager;
      }

      @Override
      public ColumnViewer getViewer() {
         return _ilViewer;
      }

      @Override
      public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

         _ilViewerContainer.setRedraw(false);
         {
            final ISelection selection = _ilViewer.getSelection();

            _ilViewer.getTable().dispose();

            createUI_512_IL_ViewerTable(_ilViewerContainer);
            _ilViewerContainer.layout();

            // update viewer
            reloadViewer();

            _ilViewer.setSelection(selection);
         }
         _ilViewerContainer.setRedraw(true);

         return _ilViewer;
      }

      @Override
      public void reloadViewer() {

         _ilViewer.setInput(this);
      }

      @Override
      public void updateColumnHeader(final ColumnDefinition colDef) {}
   }

   private class ILContentProvider implements IStructuredContentProvider {

      public ILContentProvider() {}

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {

         final ArrayList<ImportLauncher> configItems = _dialogEasyConfig.importLaunchers;

         return configItems.toArray(new ImportLauncher[configItems.size()]);
      }

      @Override
      public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
   }

   public DialogEasyImportConfig(final Shell parentShell,
                                 final EasyConfig easyConfig,
                                 final RawDataView rawDataView,
                                 final int initialTab) {

      super(parentShell);

      _rawDataView = rawDataView;
      _initialTab = initialTab;

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);

      cloneEasyConfig(easyConfig);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            // tour type images can have been changed

            _ilViewer.refresh(true);
            update_UI_From_Model_IL();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   /**
    * Clone original configs, only the backup will be modified in the dialog.
    *
    * @param easyConfig
    */
   private void cloneEasyConfig(final EasyConfig easyConfig) {

// SET_FORMATTING_OFF

      _dialogEasyConfig = new EasyConfig();

      _dialogEasyConfig.animationCrazinessFactor               = easyConfig.animationCrazinessFactor;
      _dialogEasyConfig.animationDuration                      = easyConfig.animationDuration;
      _dialogEasyConfig.backgroundOpacity                      = easyConfig.backgroundOpacity;
      _dialogEasyConfig.isLiveUpdate                           = easyConfig.isLiveUpdate;
      _dialogEasyConfig.isLogDetails                           = easyConfig.isLogDetails;
      _dialogEasyConfig.isShowTile_CloudApps                   = easyConfig.isShowTile_CloudApps;
      _dialogEasyConfig.isShowTile_Files                       = easyConfig.isShowTile_Files;
      _dialogEasyConfig.isShowTile_FossilUI                    = easyConfig.isShowTile_FossilUI;
      _dialogEasyConfig.isShowTile_SerialPort                  = easyConfig.isShowTile_SerialPort;
      _dialogEasyConfig.isShowTile_SerialPortWithConfig        = easyConfig.isShowTile_SerialPortWithConfig;
      _dialogEasyConfig.numHorizontalTiles                     = easyConfig.numHorizontalTiles;
      _dialogEasyConfig.stateToolTipDisplayAbsoluteFilePath    = easyConfig.stateToolTipDisplayAbsoluteFilePath;
      _dialogEasyConfig.stateToolTipWidth                      = easyConfig.stateToolTipWidth;
      _dialogEasyConfig.tileSize                               = easyConfig.tileSize;

// SET_FORMATTING_ON

      final ImportConfig activeImportConfig = easyConfig.getActiveImportConfig();

      /*
       * Import configs
       */
      final ArrayList<ImportConfig> importConfigs = _dialogEasyConfig.importConfigs = new ArrayList<>();

      for (final ImportConfig importConfig : easyConfig.importConfigs) {

         final ImportConfig clonedConfig = importConfig.clone();

         importConfigs.add(clonedConfig);

         // keep active config but using the clone (id)
         if (importConfig.equals(activeImportConfig)) {
            _dialogEasyConfig.setActiveImportConfig(clonedConfig);
         }
      }

      /*
       * Import launchers
       */
      final ArrayList<ImportLauncher> importLaunchers = _dialogEasyConfig.importLaunchers = new ArrayList<>();

      for (final ImportLauncher launcher : easyConfig.importLaunchers) {
         importLaunchers.add(launcher.clone());
      }
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

//      shell.addListener(SWT.Resize, new Listener() {
//         @Override
//         public void handleEvent(final Event event) {
//
//            // ensure that the dialog is not smaller than the default size
//
//            final Point shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
//
//            final Point shellSize = shell.getSize();
//
//            shellSize.x = shellSize.x < shellDefaultSize.x ? shellDefaultSize.x : shellSize.x;
//            shellSize.y = shellSize.y < shellDefaultSize.y ? shellDefaultSize.y : shellSize.y;
//
//            shell.setSize(shellSize);
//         }
//      });
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_ImportConfig_Dialog_Title);
      setMessage(Messages.Dialog_ImportConfig_Dialog_Message);

      restoreState();

      enable_IC_Controls();
      enable_IL_Controls();

      // set focus
      _comboIC_DeviceFolder.setFocus();
   }

   private void createActions() {

      _actionTTSpeed_Add = new ActionSpeedTourType_Add();
      _actionTTSpeed_Sort = new ActionSpeedTourType_Sort();

      _actionOpenTourTypePrefs = new ActionOpenPrefDialog(
            Messages.action_tourType_modify_tourTypes,
            ITourbookPreferences.PREF_PAGE_TOUR_TYPE);

      _actionRestoreDefaults = new ActionResetToDefaults(this);
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
      menuMgr.addMenuListener(this::fillTourTypeMenu);
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
      {
         _tabFolderEasy = new CTabFolder(container, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .applyTo(_tabFolderEasy);
         {
            // tab: config
            final CTabItem tabConfig = new CTabItem(_tabFolderEasy, SWT.NONE);
            tabConfig.setText(Messages.Dialog_ImportConfig_Tab_Configuration);
            tabConfig.setControl(createUI_200_Tab_ImportActions(_tabFolderEasy));

            // tab: launcher
            final CTabItem tabLauncher = new CTabItem(_tabFolderEasy, SWT.NONE);
            tabLauncher.setText(Messages.Dialog_ImportConfig_Tab_Launcher);
            tabLauncher.setControl(createUI_500_Tab_IL_ImportLauncher(_tabFolderEasy));

            // tab: options
            final CTabItem tabOptions = new CTabItem(_tabFolderEasy, SWT.NONE);
            tabOptions.setText(Messages.Dialog_ImportConfig_Tab_Options);
            tabOptions.setControl(createUI_900_Tab_Options(_tabFolderEasy));
         }
      }

      /*
       * Whithout async, the column is set to 0px width and without forced column with it looks ugly
       */
      _tabFolderEasy.getDisplay().asyncExec(() -> {

         if (_tabFolderEasy.isDisposed()) {
            return;
         }

         // compute width for all controls and equalize column width for the first column
         UI.setEqualizeColumWidths(_firstColumnControls);
         container.layout(true, true);
      });
   }

   private Composite createUI_200_Tab_ImportActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         createUI_202_Title(container);

         final Composite icContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(icContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(icContainer);
         {
            createUI_210_IC_Viewer(icContainer);
            createUI_230_IC_Actions(icContainer);
            createUI_239_IC_DragDropHint(icContainer);

            createUI_240_IC_Detail(icContainer);
         }
      }

      return container;
   }

   private void createUI_202_Title(final Composite parent) {

      final Label label = new Label(parent, SWT.WRAP);
      label.setText(Messages.Dialog_ImportConfig_Info_ImportActions);
      GridDataFactory.fillDefaults()
            .hint(convertWidthInCharsToPixels(30), SWT.DEFAULT)
            .applyTo(label);

   }

   private void createUI_210_IC_Viewer(final Composite parent) {

      // define all columns for the viewer
      _icColumnManager = new ColumnManager(_icColumnViewer, _stateIC);
      defineAll_ICColumns();

      _icViewerContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(_icViewerContainer);
      GridLayoutFactory.fillDefaults().applyTo(_icViewerContainer);
//      _viewerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         createUI_212_IC_ViewerTable(_icViewerContainer);
      }
   }

   private void createUI_212_IC_ViewerTable(final Composite parent) {

      /*
       * Create tree
       */
      final Table table = new Table(parent,
            SWT.H_SCROLL
                  | SWT.V_SCROLL
                  | SWT.BORDER
                  | SWT.FULL_SELECTION);
      table.setHeaderVisible(true);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      /*
       * Create tree viewer
       */
      _icViewer = new TableViewer(table);

      _icColumnManager.createColumns(_icViewer);

      _icViewer.setUseHashlookup(true);
      _icViewer.setContentProvider(new ICContentProvider());

      _icViewer.addSelectionChangedListener(selectionChangedEvent -> onIC_SelectIC(selectionChangedEvent.getSelection()));

      _icViewer.addDoubleClickListener(doubleClickEvent -> onIC_DblClick());

      createUI_213_IC_ContextMenu();
      createUI_214_IC_DragDrop();
   }

   /**
    * create the views context menu
    */
   private void createUI_213_IC_ContextMenu() {

      final Table table = _icViewer.getTable();

      _icColumnManager.createHeaderContextMenu(table, new EmptyContextMenuProvider());
   }

   private void createUI_214_IC_DragDrop() {

      /*
       * set drag adapter
       */
      _icViewer.addDragSupport(
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
                  final ISelection selection = _icViewer.getSelection();

                  transfer.setSelection(selection);
                  transfer.setSelectionSetTime(_dragStart = event.time & 0xFFFFFFFFL);

                  event.doit = !selection.isEmpty();
               }
            });

      /*
       * set drop adapter
       */
      final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_icViewer) {

         private Widget __dragItem;

         @Override
         public void dragOver(final DropTargetEvent dropEvent) {

            // keep table item
            __dragItem = dropEvent.item;

            super.dragOver(dropEvent);
         }

         @Override
         public boolean performDrop(final Object data) {

            if (data instanceof StructuredSelection) {
               final StructuredSelection selection = (StructuredSelection) data;

               if (selection.getFirstElement() instanceof ImportConfig) {

                  final ImportConfig selectedItem = (ImportConfig) selection.getFirstElement();

                  final int location = getCurrentLocation();
                  final Table filterTable = _icViewer.getTable();

                  /*
                   * check if drag was startet from this filter, remove the filter item before
                   * the new filter is inserted
                   */
                  if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dragStart) {
                     _icViewer.remove(selectedItem);
                  }

                  int filterIndex;

                  if (__dragItem == null) {

                     _icViewer.add(selectedItem);
                     filterIndex = filterTable.getItemCount() - 1;

                  } else {

                     // get index of the target in the table
                     filterIndex = filterTable.indexOf((TableItem) __dragItem);
                     if (filterIndex == -1) {
                        return false;
                     }

                     if (location == LOCATION_BEFORE) {
                        _icViewer.insert(selectedItem, filterIndex);
                     } else if (location == LOCATION_AFTER || location == LOCATION_ON) {
                        _icViewer.insert(selectedItem, ++filterIndex);
                     }
                  }

                  // reselect filter item
                  _icViewer.setSelection(new StructuredSelection(selectedItem));

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

      _icViewer.addDropSupport(
            DND.DROP_MOVE,
            new Transfer[] { LocalSelectionTransfer.getTransfer() },
            viewerDropAdapter);
   }

   private void createUI_230_IC_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Button: New
             */
            _btnIC_New = new Button(container, SWT.NONE);
            _btnIC_New.setText(Messages.App_Action_New);
            _btnIC_New.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onIC_Add(false)));
            setButtonLayoutData(_btnIC_New);
         }

         {
            /*
             * Button: Duplicate
             */
            _btnIC_Duplicate = new Button(container, SWT.NONE);
            _btnIC_Duplicate.setText(Messages.App_Action_Duplicate);
            _btnIC_Duplicate.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onIC_Add(true)));
            setButtonLayoutData(_btnIC_Duplicate);
         }

         {
            /*
             * Button: Remove
             */
            _btnIC_Remove = new Button(container, SWT.NONE);
            _btnIC_Remove.setText(Messages.App_Action_Remove_Immediate);
            _btnIC_Remove.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onIC_Remove()));
            setButtonLayoutData(_btnIC_Remove);
         }

//         // align to the end
//         final GridData gd = (GridData) _btnIL_Remove.getLayoutData();
//         gd.grabExcessHorizontalSpace = true;
//         gd.horizontalAlignment = SWT.END;
      }
   }

   private void createUI_239_IC_DragDropHint(final Composite parent) {

      final Label label = new Label(parent, SWT.WRAP);
      label.setText(Messages.Dialog_ImportConfig_Info_ConfigDragDrop);
      GridDataFactory.fillDefaults()
            .span(2, 1)
            .indent(0, -4)
            .applyTo(label);
   }

   private void createUI_240_IC_Detail(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Dialog_ImportConfig_Group_ImportActions);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(0, 8)
            .span(2, 1)
            .applyTo(group);
      GridLayoutFactory.swtDefaults()
            .numColumns(2)
            .applyTo(group);
//      group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         createUI_250_IC_Name(group);
         createUI_252_IC_1_BackupFolder(group);
         createUI_254_IC_2_DeviceFileFolder(group);
         createUI_270_IC_3_99_Actions(group);
         createUI_280_IC_100(group);
      }
   }

   private void createUI_250_IC_Name(final Composite parent) {

      {
         /*
          * Config name
          */

         // label
         _lblIC_ConfigName = new Label(parent, SWT.NONE);
         _lblIC_ConfigName.setText(Messages.Dialog_ImportConfig_Label_ConfigName);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblIC_ConfigName);

         // text
         _txtIC_ConfigName = new Text(parent, SWT.BORDER);
         _txtIC_ConfigName.addModifyListener(_icModifyListener);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .align(SWT.FILL, SWT.FILL)
               .indent(CONTROL_DECORATION_WIDTH, 0)
               .applyTo(_txtIC_ConfigName);
      }
   }

   private void createUI_252_IC_1_BackupFolder(final Composite parent) {

      {
         /*
          * Checkbox: Create backup
          */
         _chkIC_CreateBackup = new Button(parent, SWT.CHECK);
         _chkIC_CreateBackup.setText(Messages.Dialog_ImportConfig_Checkbox_CreateBackup);
         _chkIC_CreateBackup.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_CreateBackup_Tooltip);
         _chkIC_CreateBackup.addSelectionListener(_icSelectionListener);
         _chkIC_CreateBackup.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            if (_chkIC_CreateBackup.getSelection()) {
               _comboIC_BackupFolder.setFocus();
            }
         }));
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .indent(0, 10)
               .applyTo(_chkIC_CreateBackup);
      }

      {
         /*
          * Label: Local folder
          */
         _lblIC_BackupFolder = new Label(parent, SWT.NONE);
         _lblIC_BackupFolder.setText(Messages.Dialog_ImportConfig_Label_BackupFolder);
         _lblIC_BackupFolder.setToolTipText(Messages.Dialog_ImportConfig_Label_BackupFolder_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .indent(_leftPadding, 0)
               .applyTo(_lblIC_BackupFolder);
      }

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * Combo: path
          */
         _comboIC_BackupFolder = new Combo(container, SWT.SINGLE | SWT.BORDER);
         _comboIC_BackupFolder.setVisibleItemCount(44);
         _comboIC_BackupFolder.addVerifyListener(net.tourbook.common.UI.verifyFilePathInput());
         _comboIC_BackupFolder.addModifyListener(_ic_FolderModifyListener);
         _comboIC_BackupFolder.addKeyListener(_ic_FolderKeyListener);
         _comboIC_BackupFolder.addFocusListener(_ic_FolderFocusListener);
         _comboIC_BackupFolder.setData(_backupHistoryItems);
         _comboIC_BackupFolder.setToolTipText(Messages.Dialog_ImportConfig_Combo_Folder_Tooltip);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .indent(CONTROL_DECORATION_WIDTH, 0)
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_comboIC_BackupFolder);

         /*
          * Button: browse...
          */
         _btnIC_SelectBackupFolder = new Button(container, SWT.PUSH);
         _btnIC_SelectBackupFolder.setText(Messages.app_btn_browse);
         _btnIC_SelectBackupFolder.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_IC_Folder_Backup()));
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_btnIC_SelectBackupFolder);
         setButtonLayoutData(_btnIC_SelectBackupFolder);
      }

      {
         /*
          * Backup folder info
          */
         // fill left column
         new Label(parent, SWT.NONE);

         /*
          * Link: local folder absolute path
          */
         _linkIC_LocalFolderPath = new Link(parent, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .indent(CONTROL_DECORATION_WIDTH + convertHorizontalDLUsToPixels(4), 0)
               .applyTo(_linkIC_LocalFolderPath);

         _backupHistoryItems.setControls(_comboIC_BackupFolder, _linkIC_LocalFolderPath);
      }
   }

   private void createUI_254_IC_2_DeviceFileFolder(final Composite parent) {

      final ModifyListener deviceTypeListener = modifyEvent -> onSelectDevice();

      final Composite importContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(importContainer);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(importContainer);
//      importContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         {
            /*
             * Checkbox: Import tour files
             */
            _chkIC_ImportFiles = new Button(importContainer, SWT.CHECK);
            _chkIC_ImportFiles.setText(Messages.Dialog_ImportConfig_Checkbox_ImportFiles);
            _chkIC_ImportFiles.addSelectionListener(_icSelectionListener);
            GridDataFactory.fillDefaults()
//                  .align(SWT.FILL, SWT.CENTER)
                  .indent(0, convertVerticalDLUsToPixels(2))
                  .applyTo(_chkIC_ImportFiles);

            // this control is just for info and to have a consistent UI
            _chkIC_ImportFiles.setSelection(true);
            _chkIC_ImportFiles.setEnabled(false);
         }
         {
            /*
             * File System image
             */
            _lblIC_FileSystemImage = new Label(importContainer, SWT.NONE);
            _imageFileSystem = TourbookPlugin.getImageDescriptor(Images.EasyImport_Harddrive).createImage();
            _lblIC_FileSystemImage.setImage(_imageFileSystem);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblIC_FileSystemImage);
         }
      }

      {
         /*
          * Drop down menu: device type
          */
         _comboIC_DeviceType = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
         _comboIC_DeviceType.setToolTipText(Messages.Dialog_ImportConfig_Label_DeviceType_Tooltip);
         _comboIC_DeviceType.add(Messages.Dialog_ImportConfig_Combo_Device_LocalDevice);
         _comboIC_DeviceType.addModifyListener(deviceTypeListener);

         GridDataFactory.fillDefaults()
               .indent(CONTROL_DECORATION_WIDTH, 0)
               .align(SWT.LEFT, SWT.CENTER)
               .applyTo(_comboIC_DeviceType);

         FileSystemManager.getFileSystemsIds().forEach(_comboIC_DeviceType::add);
      }

      {
         /*
          * Label: device folder
          */
         _lblIC_DeviceFolder = new Label(parent, SWT.NONE);
         _lblIC_DeviceFolder.setText(Messages.Dialog_ImportConfig_Label_DeviceFolder);
         _lblIC_DeviceFolder.setToolTipText(Messages.Dialog_ImportConfig_Label_DeviceFolder_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .indent(_leftPadding, 0)
               .applyTo(_lblIC_DeviceFolder);
      }

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {

         /*
          * Combo: path
          */
         _comboIC_DeviceFolder = new Combo(container, SWT.SINGLE | SWT.BORDER);
         _comboIC_DeviceFolder.setVisibleItemCount(44);
         _comboIC_DeviceFolder.addVerifyListener(net.tourbook.common.UI.verifyFilePathInput());
         _comboIC_DeviceFolder.addModifyListener(_ic_FolderModifyListener);
         _comboIC_DeviceFolder.addKeyListener(_ic_FolderKeyListener);
         _comboIC_DeviceFolder.addFocusListener(_ic_FolderFocusListener);
         _comboIC_DeviceFolder.setData(_deviceHistoryItems);
         _comboIC_DeviceFolder.setToolTipText(Messages.Dialog_ImportConfig_Combo_Folder_Tooltip);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .indent(CONTROL_DECORATION_WIDTH, 0)
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_comboIC_DeviceFolder);

         /*
          * Button: browse...
          */
         _btnIC_SelectDeviceFolder = new Button(container, SWT.PUSH);
         _btnIC_SelectDeviceFolder.setText(Messages.app_btn_browse);
         _btnIC_SelectDeviceFolder.setData(_comboIC_DeviceFolder);
         _btnIC_SelectDeviceFolder.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_IC_Folder_Device()));
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_btnIC_SelectDeviceFolder);
         setButtonLayoutData(_btnIC_SelectDeviceFolder);
      }

      /*
       * Device folder info
       */
      {
         // fill left column
         new Label(parent, SWT.NONE);

         /*
          * Link: device folder absolute path
          */
         _linkIC_DeviceFolderPath = new Link(parent, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .indent(CONTROL_DECORATION_WIDTH + convertHorizontalDLUsToPixels(4), 0)
               .applyTo(_linkIC_DeviceFolderPath);

         _deviceHistoryItems.setControls(_comboIC_DeviceFolder, _linkIC_DeviceFolderPath, _btnIC_SelectDeviceFolder);
      }

      {
         final int topPadding = 10;

         /*
          * Label: file name
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Dialog_ImportConfig_Label_DeviceFiles);
         label.setToolTipText(Messages.Dialog_ImportConfig_Label_DeviceFiles_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .indent(_leftPadding, topPadding)
               .applyTo(label);

         /*
          * Text: file name
          */
         _txtIC_DeviceFiles = new Text(parent, SWT.BORDER);
         _txtIC_DeviceFiles.addModifyListener(_icModifyListener);
         GridDataFactory.fillDefaults()
               .indent(CONTROL_DECORATION_WIDTH, topPadding)
               .applyTo(_txtIC_DeviceFiles);
      }
   }

   private void createUI_270_IC_3_99_Actions(final Composite parent) {

      // V-spacer
      new Label(parent, SWT.NONE);

      {
         _linkIC_ILActions = new Link(parent, SWT.NONE);
         _linkIC_ILActions.setText(Messages.Dialog_ImportConfig_Link_OtherActions);
         _linkIC_ILActions.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_IC_LauncherActions()));
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .span(2, 1)
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_linkIC_ILActions);
      }

      // V-spacer
      new Label(parent, SWT.NONE);
   }

   private void createUI_280_IC_100(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .span(2, 1)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {

         {
            /*
             * Checkbox: Delete device files
             */
            _chkIC_DeleteDeviceFiles = new Button(container, SWT.CHECK);
            _chkIC_DeleteDeviceFiles.setText(Messages.Dialog_ImportConfig_Checkbox_DeleteDeviceFiles);
            _chkIC_DeleteDeviceFiles.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_DeleteDeviceFiles_Tooltip);
            _chkIC_DeleteDeviceFiles.addSelectionListener(_icSelectionListener);
         }
         {
            /*
             * Label: Delete Info
             */
            _lblIC_DeleteFilesInfo = new Label(container, SWT.NONE);
            _lblIC_DeleteFilesInfo.setForeground(ThemeUtil.getErrorColor());
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(convertWidthInCharsToPixels(1), 0)
                  .applyTo(_lblIC_DeleteFilesInfo);
         }
      }

      {
         /*
          * Checkbox: Turn OFF watching
          */
         _chkIC_TurnOffWatching = new Button(parent, SWT.CHECK);
         _chkIC_TurnOffWatching.setText(Messages.Dialog_ImportConfig_Checkbox_DeviceWatching);
         _chkIC_TurnOffWatching.setToolTipText(Messages.Import_Data_HTML_DeviceOff_Tooltip);
         _chkIC_TurnOffWatching.addSelectionListener(_icSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .applyTo(_chkIC_TurnOffWatching);
      }
   }

   private Composite createUI_500_Tab_IL_ImportLauncher(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.swtDefaults()
            .numColumns(3)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
      {
         final Label label = new Label(container, SWT.WRAP);
         label.setText(Messages.Dialog_ImportConfig_Label_ImportLauncher);
         GridDataFactory.fillDefaults()
               .span(3, 1)
               .hint(convertWidthInCharsToPixels(30), SWT.DEFAULT)
               .applyTo(label);

         createUI_510_IL_Viewer(container);
         createUI_530_IL_Actions(container);
         createUI_540_IL_Detail(container);

         createUI_570_IL_DragDropHint(container);
      }

      return container;
   }

   private void createUI_510_IL_Viewer(final Composite parent) {

      // define all columns for the viewer
      _ilColumnManager = new ColumnManager(_ilColumnViewer, _stateIL);
      _ilEasyLauncherUtils.defineAllColumns(_ilColumnManager, _pc);

      _ilViewerContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(_defaultPaneWidth, SWT.DEFAULT)
            .applyTo(_ilViewerContainer);
      GridLayoutFactory.fillDefaults().applyTo(_ilViewerContainer);
//      _viewerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         createUI_512_IL_ViewerTable(_ilViewerContainer);
      }
   }

   private void createUI_512_IL_ViewerTable(final Composite parent) {

      /*
       * Create table
       */
      final Table table = new Table(parent,
            SWT.H_SCROLL
                  | SWT.V_SCROLL
                  | SWT.BORDER
                  | SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);

      /*
       * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
       * critical for performance that these methods be as efficient as possible.
       */
      final Listener paintListener = event -> {

         if (event.type == SWT.MeasureItem || event.type == SWT.PaintItem) {

            onPaintViewer(event);
         }
      };
      table.addListener(SWT.MeasureItem, paintListener);
      table.addListener(SWT.PaintItem, paintListener);

      /*
       * Create viewer
       */
      _ilViewer = new TableViewer(table);

      _ilColumnManager.createColumns(_ilViewer);

      _ilColumnIndexConfigImage = _ilEasyLauncherUtils.getColDef_TourTypeImage().getCreateIndex();

      _ilViewer.setUseHashlookup(true);
      _ilViewer.setContentProvider(new ILContentProvider());

      _ilViewer.addSelectionChangedListener(selectionChangedEvent -> onSelect_IL(selectionChangedEvent.getSelection()));

      _ilViewer.addDoubleClickListener(doubleClickEvent -> onIL_DblClick());

      createUI_513_IL_ContextMenu();
      createUI_514_IL_DragDrop();
   }

   /**
    * create the views context menu
    */
   private void createUI_513_IL_ContextMenu() {

      final Table table = _ilViewer.getTable();

      _ilColumnManager.createHeaderContextMenu(table, new EmptyContextMenuProvider());
   }

   private void createUI_514_IL_DragDrop() {

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

         private Widget __dragItem;

         @Override
         public void dragOver(final DropTargetEvent dropEvent) {

            // keep table item
            __dragItem = dropEvent.item;

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

                  if (__dragItem == null) {

                     _ilViewer.add(filterItem);
                     filterIndex = filterTable.getItemCount() - 1;

                  } else {

                     // get index of the target in the table
                     filterIndex = filterTable.indexOf((TableItem) __dragItem);
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

   private void createUI_530_IL_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
//            .grab(true, false)
//            .hint(1, SWT.DEFAULT)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         /*
          * Button: New one tour type
          */
         _btnIL_NewOne = new Button(container, SWT.NONE);
         _btnIL_NewOne.setImage(TourTypeImage.getTourTypeImage(TourType.IMAGE_KEY_DIALOG_SELECTION));
         _btnIL_NewOne.setText(Messages.Dialog_ImportConfig_Action_NewOneTourType);
         _btnIL_NewOne.setToolTipText(Messages.Dialog_ImportConfig_Action_NewOneTourType_Tooltip);
         _btnIL_NewOne.addSelectionListener(widgetSelectedAdapter(selectionEvent -> UI.openControlMenu(_btnIL_NewOne)));
         setButtonLayoutData(_btnIL_NewOne);

         /*
          * Context menu: Tour type
          */
         final MenuManager menuMgr = new MenuManager();
         menuMgr.setRemoveAllWhenShown(true);
         menuMgr.addMenuListener(this::fillTourTypeOneMenu);
         final Menu ttContextMenu = menuMgr.createContextMenu(_btnIL_NewOne);
         _btnIL_NewOne.setMenu(ttContextMenu);

         /*
          * Button: New
          */
         _btnIL_New = new Button(container, SWT.NONE);
         _btnIL_New.setText(Messages.App_Action_New);
         _btnIL_New.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onIL_Add(false)));
         setButtonLayoutData(_btnIL_New);

         /*
          * Button: Duplicate
          */
         _btnIL_Duplicate = new Button(container, SWT.NONE);
         _btnIL_Duplicate.setText(Messages.App_Action_Duplicate);
         _btnIL_Duplicate.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onIL_Add(true)));
         setButtonLayoutData(_btnIL_Duplicate);

         /*
          * button: remove
          */
         _btnIL_Remove = new Button(container, SWT.NONE);
         _btnIL_Remove.setText(Messages.App_Action_Remove_Immediate);
         _btnIL_Remove.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onIL_Remove()));
         setButtonLayoutData(_btnIL_Remove);

         // align to the end
         final GridData gd = (GridData) _btnIL_Remove.getLayoutData();
         gd.grabExcessHorizontalSpace = true;
         gd.horizontalAlignment = SWT.END;
      }
   }

   private void createUI_540_IL_Detail(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Dialog_ImportConfig_Group_ImportLauncherConfig);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .hint(_defaultPaneWidth, SWT.DEFAULT)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//      group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
      {
         createUI_542_IL_Name(group);
         createUI_550_IL_TourType(group);
         createUI_580_IL_LastMarker(group);
         createUI_590_IL_AdjustTemperature(group);
         createUI_591_IL_AdjustElevation(group);
         createUI_592_IL_SetElevationFromSRTM(group);
         createUI_595_IL_RetrieveWeatherData(group);
         createUI_599_IL_Save(group);
      }
   }

   private void createUI_542_IL_Name(final Composite parent) {

      {
         /*
          * Config name
          */

         // label
         _lblIL_ConfigName = new Label(parent, SWT.NONE);
         _lblIL_ConfigName.setText(Messages.Dialog_ImportConfig_Label_ConfigName);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblIL_ConfigName);

         // text
         _txtIL_ConfigName = new Text(parent, SWT.BORDER);
         _txtIL_ConfigName.addModifyListener(_ilModifyListener);
         GridDataFactory.fillDefaults()
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
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.BEGINNING)
               .applyTo(_lblIL_ConfigDescription);

         // text
         _txtIL_ConfigDescription = new Text(parent,
               SWT.BORDER |
                     SWT.WRAP
                     | SWT.MULTI
                     | SWT.V_SCROLL
                     | SWT.H_SCROLL);
         _txtIL_ConfigDescription.addModifyListener(_ilModifyListener);

         GridDataFactory.fillDefaults()
               .grab(true, false)
               .hint(SWT.DEFAULT, convertHeightInCharsToPixels(2))
               .applyTo(_txtIL_ConfigDescription);
      }
   }

   private void createUI_550_IL_TourType(final Composite parent) {

      final SelectionListener ttListener = widgetSelectedAdapter(selectionEvent -> onSelect_IL_TourType());

      /*
       * Checkbox: Set tour type
       */
      _chkIL_SetTourType = new Button(parent, SWT.CHECK);
      _chkIL_SetTourType.setText(Messages.Dialog_ImportConfig_Checkbox_TourType);
      _chkIL_SetTourType.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_TourType_Tooltip);
      _chkIL_SetTourType.addSelectionListener(ttListener);
      GridDataFactory.fillDefaults()
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
         GridDataFactory.fillDefaults()
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
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .span(2, 1)
               .indent(_leftPadding, 0)
               .hint(SWT.DEFAULT, convertHeightInCharsToPixels(2))
               .applyTo(_pagebookTourType);
         {
            _pageTourType_NoTourType = createUI_552_IL_Page_NoTourType(_pagebookTourType);
            _pageTourType_OneForAll = createUI_554_IL_Page_OneForAll(_pagebookTourType);
            _pageTourType_BySpeed = createUI_560_IL_Page_BySpeed(_pagebookTourType);
         }
      }
   }

   /**
    * Page: Not used
    *
    * @return
    */
   private Label createUI_552_IL_Page_NoTourType(final PageBook parent) {

      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(UI.EMPTY_STRING);

      return label;
   }

   private Composite createUI_554_IL_Page_OneForAll(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         _lblIL_One_TourTypeIcon = new Label(container, SWT.NONE);
         _lblIL_One_TourTypeIcon.setText(UI.EMPTY_STRING);
         GridDataFactory.fillDefaults()
               .hint(16, 16)
               .applyTo(_lblIL_One_TourTypeIcon);

         /*
          * Tour type
          */
         _linkTT_One_TourType = new Link(container, SWT.NONE);
         _linkTT_One_TourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
         _linkTT_One_TourType.addSelectionListener(widgetSelectedAdapter(
               selectionEvent -> net.tourbook.common.UI.openControlMenu(_linkTT_One_TourType)));

         GridDataFactory.fillDefaults().grab(true, false).applyTo(_linkTT_One_TourType);

         _lblIL_One_TourTypeCadenceLabel = new Label(container, SWT.NONE);
         _lblIL_One_TourTypeCadenceLabel.setText(Messages.Tour_Editor_Label_Cadence);

         /*
          * Cadence
          */
         final CadenceMultiplier cadence = (CadenceMultiplier) Util.getStateEnum(_stateRawDataView,
               RawDataView.STATE_DEFAULT_CADENCE_MULTIPLIER,
               RawDataView.STATE_DEFAULT_CADENCE_MULTIPLIER_DEFAULT);

         _comboIL_One_TourType_Cadence = new ComboViewerCadence(container, SWT.READ_ONLY | SWT.DROP_DOWN);
         _comboIL_One_TourType_Cadence.setSelection(cadence);
      }

      return container;
   }

   private Composite createUI_560_IL_Page_BySpeed(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {

         createUI_562_IL_SpeedTourType_Actions(container);
         createUI_564_IL_SpeedTourTypes(container);
      }

      return container;
   }

   private void createUI_562_IL_SpeedTourType_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionTTSpeed_Add);
      tbm.add(_actionTTSpeed_Sort);

      tbm.update(true);
   }

   private void createUI_564_IL_SpeedTourTypes(final Composite parent) {

      /*
       * Speed tour type fields container
       */
      _speedTourType_OuterContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(_speedTourType_OuterContainer);

      GridLayoutFactory.fillDefaults().applyTo(_speedTourType_OuterContainer);

      createUI_566_IL_SpeedTourType_Fields();
   }

   /**
    * Create the speed tour type fields from a list.
    *
    * @param parent
    */
   private void createUI_566_IL_SpeedTourType_Fields() {

      if (_selectedIL == null) {

         updateUI_ClearSpeedTourTypes();

         return;
      }

      final int speedTTSize = _selectedIL.speedTourTypes.size();

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

      _speedTourType_Container = createUI_568_IL_SpeedTourType_ScrolledContainer(_speedTourType_OuterContainer);

      /*
       * fields
       */
      _actionTTSpeed_Delete = new ActionSpeedTourType_Delete[speedTTSize];
      _lblTT_Speed_TourTypeIcon = new Label[speedTTSize];
      _lblTT_Speed_SpeedUnit = new Label[speedTTSize];
      _linkTT_Speed_TourType = new Link[speedTTSize];
      _spinnerTT_Speed_AvgSpeed = new Spinner[speedTTSize];
      _comboTT_Cadence = new ComboViewerCadence[speedTTSize];

      _speedTourType_Container.setRedraw(false);
      {
         for (int speedTTIndex = 0; speedTTIndex < speedTTSize; speedTTIndex++) {

            /*
             * Spinner: Speed value
             */
            final Spinner spinnerValue = new Spinner(_speedTourType_Container, SWT.BORDER);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(spinnerValue);
            spinnerValue.setMaximum(EasyConfig.TOUR_TYPE_AVG_SPEED_MAX);
            spinnerValue.setMinimum(EasyConfig.TOUR_TYPE_AVG_SPEED_MIN);
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
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .hint(16, 16)
                  .applyTo(lblTourTypeIcon);

            /*
             * Link: Tour type
             */
            final Link linkTourType = new Link(_speedTourType_Container, SWT.NONE);
            linkTourType.setText(Messages.tour_editor_label_tour_type);
            linkTourType.addSelectionListener(_speedTourTypeListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(linkTourType);

            /*
             * Combo: Cadence
             */
            final Label lblCadence = new Label(_speedTourType_Container, SWT.NONE);
            lblCadence.setText(Messages.Tour_Editor_Label_Cadence);

            final CadenceMultiplier cadence = (CadenceMultiplier) Util.getStateEnum(_stateRawDataView,
                  RawDataView.STATE_DEFAULT_CADENCE_MULTIPLIER,
                  RawDataView.STATE_DEFAULT_CADENCE_MULTIPLIER_DEFAULT);

            final ComboViewerCadence comboCadence = new ComboViewerCadence(_speedTourType_Container);
            comboCadence.setSelection(cadence);

            /*
             * Context menu: Tour type
             */
            final MenuManager menuMgr = new MenuManager();
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(menuManager -> fillSpeedTourTypeMenu(menuManager, linkTourType));

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
            _comboTT_Cadence[speedTTIndex] = comboCadence;
         }
      }
      _speedTourType_Container.setRedraw(true);

      _speedTourType_OuterContainer.layout(true);

      // set scroll position to previous position
      if (scrollOrigin != null) {
         _speedTourType_ScrolledContainer.setOrigin(scrollOrigin);
      }
   }

   private Composite createUI_568_IL_SpeedTourType_ScrolledContainer(final Composite parent) {

      // scrolled container
      _speedTourType_ScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
      _speedTourType_ScrolledContainer.setExpandVertical(true);
      _speedTourType_ScrolledContainer.setExpandHorizontal(true);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_speedTourType_ScrolledContainer);

      // container
      final Composite speedTTContainer = new Composite(_speedTourType_ScrolledContainer, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(speedTTContainer);
      GridLayoutFactory.fillDefaults()
            .numColumns(7)
            .applyTo(speedTTContainer);

      _speedTourType_ScrolledContainer.setContent(speedTTContainer);
      _speedTourType_ScrolledContainer.addControlListener(controlResizedAdapter(
            ControlEvent -> _speedTourType_ScrolledContainer.setMinSize(
                  speedTTContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT))));

      return speedTTContainer;
   }

   private void createUI_570_IL_DragDropHint(final Composite parent) {

      final Label label = new Label(parent, SWT.WRAP);
      label.setText(Messages.Dialog_ImportConfig_Info_ConfigDragDrop);
      GridDataFactory.fillDefaults()
            .span(3, 1)
            .applyTo(label);
   }

   private void createUI_580_IL_LastMarker(final Composite parent) {

      {
         /*
          * Checkbox: Last marker
          */
         _chkIL_SetLastMarker = new Button(parent, SWT.CHECK);
         _chkIL_SetLastMarker.setText(Messages.Dialog_ImportConfig_Checkbox_LastMarker);
         _chkIL_SetLastMarker.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_LastMarker_Tooltip);
         _chkIL_SetLastMarker.addSelectionListener(_defaultModify_Listener);
         GridDataFactory.fillDefaults()
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
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .indent(_leftPadding, 0)
               .applyTo(_lblIL_LastMarker);

         final Composite container = new Composite(parent, SWT.NONE);
//         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            // spinner: distance 1.1 km
            _spinnerIL_LastMarkerDistance = new Spinner(container, SWT.BORDER);
            _spinnerIL_LastMarkerDistance.setMaximum(EasyConfig.LAST_MARKER_DISTANCE_MAX / 100);
            _spinnerIL_LastMarkerDistance.setMinimum(EasyConfig.LAST_MARKER_DISTANCE_MIN);
            _spinnerIL_LastMarkerDistance.setDigits(1);
            _spinnerIL_LastMarkerDistance.addMouseWheelListener(_defaultModify_MouseWheelListener);
            _spinnerIL_LastMarkerDistance.addSelectionListener(_defaultModify_Listener);
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
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .indent(_leftPadding, 0)
               .applyTo(_lblIL_LastMarkerText);

         // text
         _txtIL_LastMarker = new Text(parent, SWT.BORDER);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .applyTo(_txtIL_LastMarker);
      }
   }

   private void createUI_590_IL_AdjustTemperature(final Composite parent) {

      {
         /*
          * Checkbox: Adjust temperature
          */
         _chkIL_AdjustTemperature = new Button(parent, SWT.CHECK);
         _chkIL_AdjustTemperature.setText(Messages.Dialog_ImportConfig_Checkbox_AdjustTemperature);
         _chkIL_AdjustTemperature.setToolTipText(Messages.Dialog_AdjustTemperature_Label_Info);
         _chkIL_AdjustTemperature.addSelectionListener(_defaultModify_Listener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .indent(0, 5)
               .applyTo(_chkIL_AdjustTemperature);
      }

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .span(2, 1)
            .applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(3)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Label: Adjustment duration
             */
            _lblIL_TemperatureAdjustmentDuration = new Label(container, SWT.NONE);
            _lblIL_TemperatureAdjustmentDuration.setText(Messages.Dialog_AdjustTemperature_Label_TemperatureAdjustmentDuration);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(_leftPadding, 0)
                  .applyTo(_lblIL_TemperatureAdjustmentDuration);

            /*
             * Spinner: Duration
             */
            _spinnerIL_TemperatureAdjustmentDuration = new Spinner(container, SWT.BORDER);
            _spinnerIL_TemperatureAdjustmentDuration.setMinimum(0);
            _spinnerIL_TemperatureAdjustmentDuration.setMaximum(60 * 60 * 24); // 1 day
            _spinnerIL_TemperatureAdjustmentDuration.setPageIncrement(60); // 1 minute
            _spinnerIL_TemperatureAdjustmentDuration.addMouseWheelListener(mouseEvent -> {
               Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
               updateUI_TemperatureAdjustmentDuration();
               onIL_Modified();
            });
            _spinnerIL_TemperatureAdjustmentDuration.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> {
                     updateUI_TemperatureAdjustmentDuration();
                     onIL_Modified();
                  }));
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_spinnerIL_TemperatureAdjustmentDuration);

            // label: h
            _lblIL_TemperatureAdjustmentDuration_Unit = new Label(container, SWT.NONE);
            _lblIL_TemperatureAdjustmentDuration_Unit.setText(UI.UNIT_LABEL_TIME);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_lblIL_TemperatureAdjustmentDuration_Unit);
         }
         {
            /*
             * Avg temperature
             */
            // label
            _lblIL_AvgTemperature = new Label(container, SWT.NONE);
            _lblIL_AvgTemperature.setText(Messages.Dialog_AdjustTemperature_Label_AvgTemperature);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(_leftPadding, 0)
                  .applyTo(_lblIL_AvgTemperature);

            // spinner
            _spinnerIL_AvgTemperature = new Spinner(container, SWT.BORDER);
            _spinnerIL_AvgTemperature.setPageIncrement(5);
            _spinnerIL_AvgTemperature.setMinimum(EasyConfig.TEMPERATURE_AVG_TEMPERATURE_MIN);
            _spinnerIL_AvgTemperature.setMaximum(EasyConfig.TEMPERATURE_AVG_TEMPERATURE_MAX);
            _spinnerIL_AvgTemperature.addMouseWheelListener(_defaultModify_MouseWheelListener);
            _spinnerIL_AvgTemperature.addSelectionListener(_defaultModify_Listener);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_spinnerIL_AvgTemperature);

            // label: �C / �F
            _lblIL_AvgTemperature_Unit = new Label(container, SWT.NONE);
            _lblIL_AvgTemperature_Unit.setText(UI.UNIT_LABEL_TEMPERATURE);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblIL_AvgTemperature_Unit);
         }
      }
   }

   private void createUI_591_IL_AdjustElevation(final Composite parent) {

      /*
       * Checkbox: Adjust Elevation
       */
      _chkIL_ReplaceFirstTimeSliceElevation = new Button(parent, SWT.CHECK);
      _chkIL_ReplaceFirstTimeSliceElevation.setText(Messages.Dialog_ImportConfig_Checkbox_ReplaceFirstTimeSliceElevation);
      _chkIL_ReplaceFirstTimeSliceElevation.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_ReplaceFirstTimeSliceElevation_Tooltip);
      _chkIL_ReplaceFirstTimeSliceElevation.addSelectionListener(_ilSelectionListener);
      GridDataFactory.fillDefaults()
            .span(2, 1)
            .indent(0, 5)
            .applyTo(_chkIL_ReplaceFirstTimeSliceElevation);
   }

   private void createUI_592_IL_SetElevationFromSRTM(final Composite parent) {

      /*
       * Checkbox: Set elevation up/down values from SRTM data
       */
      _chkIL_ReplaceElevationFromSRTM = new Button(parent, SWT.CHECK);
      _chkIL_ReplaceElevationFromSRTM.setText(Messages.Dialog_ImportConfig_Checkbox_ReplaceElevationFromSRTM);
      _chkIL_ReplaceElevationFromSRTM.addSelectionListener(_ilSelectionListener);
      GridDataFactory.fillDefaults()
            .span(2, 1)
            .indent(0, 5)
            .applyTo(_chkIL_ReplaceElevationFromSRTM);
   }

   private void createUI_595_IL_RetrieveWeatherData(final Composite parent) {

      /*
       * Checkbox: Retrieve Weather Data
       */
      _chkIL_RetrieveWeatherData = new Button(parent, SWT.CHECK);
      _chkIL_RetrieveWeatherData.setText(Messages.Dialog_ImportConfig_Checkbox_RetrieveWeatherData);
      _chkIL_RetrieveWeatherData.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_RetrieveWeatherData_Tooltip);
      _chkIL_RetrieveWeatherData.addSelectionListener(_defaultModify_Listener);
      GridDataFactory.fillDefaults()
            .span(2, 1)
            .indent(0, 5)
            .applyTo(_chkIL_RetrieveWeatherData);

   }

   private void createUI_599_IL_Save(final Composite parent) {

      {
         /*
          * Checkbox: Save
          */
         _chkIL_SaveTour = new Button(parent, SWT.CHECK);
         _chkIL_SaveTour.setText(Messages.Dialog_ImportConfig_Checkbox_SaveTour);
         _chkIL_SaveTour.addSelectionListener(_ilSelectionListener);
         _chkIL_SaveTour.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_SaveTour_Tooltip);
         GridDataFactory.fillDefaults()
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
         _chkIL_ShowInDashboard.addSelectionListener(_ilSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .indent(0, convertVerticalDLUsToPixels(10))
               .applyTo(_chkIL_ShowInDashboard);
      }
   }

   private Composite createUI_900_Tab_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.swtDefaults()
            .numColumns(1)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         createUI_902_Dashboard(container);
         createUI_990_Actions(container);
      }

      return container;
   }

   private void createUI_902_Dashboard(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .align(SWT.CENTER, SWT.CENTER)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         createUI_910_Option_Tiles(container);
         createUI_920_Option_StateTooltip(container);
         createUI_930_Option_Dashboard(container);
         createUI_940_Option_SimpleImport(container);

         createUI_980_TourLog(container);
      }
   }

   private void createUI_910_Option_Tiles(final Composite parent) {

      /*
       * Group: Tiles
       */
      final Group groupTiles = new Group(parent, SWT.NONE);
      groupTiles.setText(Messages.Dialog_ImportConfig_Group_Tiles);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(groupTiles);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(groupTiles);
      {
         /*
          * Tile size
          */
         // label
         Label label = new Label(groupTiles, SWT.NONE);
         label.setText(Messages.Dialog_ImportConfig_Label_ConfigTileSize);
         label.setToolTipText(Messages.Dialog_ImportConfig_Label_ConfigTileSize_Tooltip);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
         _firstColumnControls.add(label);

         // spinner
         _spinnerDash_TileSize = new Spinner(groupTiles, SWT.BORDER);
         _spinnerDash_TileSize.setMaximum(EasyConfig.TILE_SIZE_MAX);
         _spinnerDash_TileSize.setMinimum(EasyConfig.TILE_SIZE_MIN);
         _spinnerDash_TileSize.addSelectionListener(_liveUpdateListener);
         _spinnerDash_TileSize.addMouseWheelListener(_liveUpdateMouseWheelListener);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_spinnerDash_TileSize);

         // label: px
         label = new Label(groupTiles, SWT.NONE);
         label.setText(CSS_PX);
      }
      {
         /*
          * Number of columns
          */
         // label
         final Label label = new Label(groupTiles, SWT.NONE);
         label.setText(Messages.Dialog_ImportConfig_Label_ImportColumns);
         label.setToolTipText(Messages.Dialog_ImportConfig_Label_ImportColumns_Tooltip);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
         _firstColumnControls.add(label);

         // spinner
         _spinnerDash_NumHTiles = new Spinner(groupTiles, SWT.BORDER);
         _spinnerDash_NumHTiles.setMaximum(EasyConfig.HORIZONTAL_TILES_MAX);
         _spinnerDash_NumHTiles.setMinimum(EasyConfig.HORIZONTAL_TILES_MIN);
         _spinnerDash_NumHTiles.addSelectionListener(_liveUpdateListener);
         _spinnerDash_NumHTiles.addMouseWheelListener(_liveUpdateMouseWheelListener);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_spinnerDash_NumHTiles);

         // fill 3rd column
         new Label(groupTiles, SWT.NONE);
      }
   }

   private void createUI_920_Option_StateTooltip(final Composite parent) {

      /*
       * Group: State Tooltip
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Dialog_ImportConfig_Group_StateTooltip);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(group);
      {
         {
            /*
             * Width
             */
            // label
            Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Dialog_ImportConfig_Label_StateTooltipWidth);
            GridDataFactory.fillDefaults().applyTo(label);
            _firstColumnControls.add(label);

            // spinner
            _spinnerDash_StateTooltipWidth = new Spinner(group, SWT.BORDER);
            _spinnerDash_StateTooltipWidth.setMaximum(EasyConfig.STATE_TOOLTIP_WIDTH_MAX);
            _spinnerDash_StateTooltipWidth.setMinimum(EasyConfig.STATE_TOOLTIP_WIDTH_MIN);
            _spinnerDash_StateTooltipWidth.addSelectionListener(_liveUpdateListener);
            _spinnerDash_StateTooltipWidth.addMouseWheelListener(_liveUpdateMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerDash_StateTooltipWidth);

            // label: px
            label = new Label(group, SWT.NONE);
            label.setText(CSS_PX);
         }
         {
            /*
             * Display absolute file path
             */
            // Checkbox
            _chkOptions_DisplayAbsoluteFilePath = new Button(group, SWT.CHECK);
            _chkOptions_DisplayAbsoluteFilePath.setText(Messages.Dialog_ImportConfig_Label_StateTooltip_DisplayAbsoluteFilePath);
            _chkOptions_DisplayAbsoluteFilePath.setToolTipText(Messages.Dialog_ImportConfig_Label_StateTooltip_DisplayAbsoluteFilePath_Tooltip);
            _chkOptions_DisplayAbsoluteFilePath.addSelectionListener(_liveUpdateListener);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .applyTo(_chkOptions_DisplayAbsoluteFilePath);
         }
      }
   }

   private void createUI_930_Option_Dashboard(final Composite parent) {

      /*
       * Group: State Tooltip
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Dialog_ImportConfig_Group_Dashboard);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(group);
      {
         /*
          * Animation duration
          */
         // label
         Label label = new Label(group, SWT.NONE);
         label.setText(Messages.Dialog_ImportConfig_Label_AnimationDuration);
         label.setToolTipText(Messages.Dialog_ImportConfig_Label_AnimationDuration_Tooltip);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
         _firstColumnControls.add(label);

         // spinner
         _spinnerDash_AnimationDuration = new Spinner(group, SWT.BORDER);
         _spinnerDash_AnimationDuration.setMaximum(EasyConfig.ANIMATION_DURATION_MAX);
         _spinnerDash_AnimationDuration.setMinimum(EasyConfig.ANIMATION_DURATION_MIN);
         _spinnerDash_AnimationDuration.setDigits(1);
         _spinnerDash_AnimationDuration.addSelectionListener(_liveUpdateListener);
         _spinnerDash_AnimationDuration.addMouseWheelListener(_liveUpdateMouseWheelListener);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_spinnerDash_AnimationDuration);

         // label
         label = new Label(group, SWT.NONE);
         label.setText(Messages.App_Unit_Seconds_Small);
      }
      {
         /*
          * Animation crazy factor
          */
         // label
         final Label label = new Label(group, SWT.NONE);
         label.setText(Messages.Dialog_ImportConfig_Label_AnimationCrazyFactor);
         label.setToolTipText(Messages.Dialog_ImportConfig_Label_AnimationCrazyFactor_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(label);
         _firstColumnControls.add(label);

         // spinner
         _spinnerDash_AnimationCrazinessFactor = new Spinner(group, SWT.BORDER);
         _spinnerDash_AnimationCrazinessFactor.setMaximum(EasyConfig.ANIMATION_CRAZINESS_FACTOR_MAX);
         _spinnerDash_AnimationCrazinessFactor.setMinimum(EasyConfig.ANIMATION_CRAZINESS_FACTOR_MIN);
         _spinnerDash_AnimationCrazinessFactor.addSelectionListener(_liveUpdateListener);
         _spinnerDash_AnimationCrazinessFactor.addMouseWheelListener(_liveUpdateMouseWheelListener);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_spinnerDash_AnimationCrazinessFactor);

         // fill 3rd column
         new Label(group, SWT.NONE);
      }
      {
         /*
          * Background opacity
          */
         // label
         final Label label = new Label(group, SWT.NONE);
         label.setText(Messages.Dialog_ImportConfig_Label_BackgroundOpacity);
         label.setToolTipText(Messages.Dialog_ImportConfig_Label_BackgroundOpacity_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(label);
         _firstColumnControls.add(label);

         // spinner
         _spinnerDash_BgOpacity = new Spinner(group, SWT.BORDER);
         _spinnerDash_BgOpacity.setMaximum(EasyConfig.BACKGROUND_OPACITY_MAX);
         _spinnerDash_BgOpacity.setMinimum(EasyConfig.BACKGROUND_OPACITY_MIN);
         _spinnerDash_BgOpacity.addSelectionListener(_liveUpdateListener);
         _spinnerDash_BgOpacity.addMouseWheelListener(_liveUpdateMouseWheelListener);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_spinnerDash_BgOpacity);

         // fill 3rd column
         new Label(group, SWT.NONE);
      }
   }

   private void createUI_940_Option_SimpleImport(final Composite parent) {

      /*
       * Group: Simple Import
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Dialog_ImportConfig_Group_SimpleImport);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(group);
      {
         {
            /*
             * Label: Show these tiles
             */
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Dialog_ImportConfig_Label_ShowTheseTiles);
         }
         {
            /*
             * Checkbox: Files
             */
            _chkOptions_ShowTile_Files = new Button(group, SWT.CHECK);
            _chkOptions_ShowTile_Files.setText(Messages.Dialog_ImportConfig_Checkbox_ShowTile_Files);
            _chkOptions_ShowTile_Files.addSelectionListener(_liveUpdateListener);
         }
         {
            /*
             * Checkbox: Cloud apps
             */
            _chkOptions_ShowTile_CloudApps = new Button(group, SWT.CHECK);
            _chkOptions_ShowTile_CloudApps.setText(Messages.Dialog_ImportConfig_Checkbox_ShowTile_CloudApps);
            _chkOptions_ShowTile_CloudApps.addSelectionListener(_liveUpdateListener);
         }
         {
            /*
             * Checkbox: Serial port
             */
            _chkOptions_ShowTile_SerialPort = new Button(group, SWT.CHECK);
            _chkOptions_ShowTile_SerialPort.setText(Messages.Dialog_ImportConfig_Checkbox_ShowTile_SerialPort);
            _chkOptions_ShowTile_SerialPort.addSelectionListener(_liveUpdateListener);
         }
         {
            /*
             * Checkbox: Serial port with configuration
             */
            _chkOptions_ShowTile_SerialPortWithConfig = new Button(group, SWT.CHECK);
            _chkOptions_ShowTile_SerialPortWithConfig.setText(Messages.Dialog_ImportConfig_Checkbox_ShowTile_SerialPortWithConfig);
            _chkOptions_ShowTile_SerialPortWithConfig.addSelectionListener(_liveUpdateListener);
         }
         {
            /*
             * Checkbox: Fossil UI
             */
            _chkOptions_ShowTile_FossilUI = new Button(group, SWT.CHECK);
            _chkOptions_ShowTile_FossilUI.setText(Messages.Dialog_ImportConfig_Checkbox_ShowTile_FossilUI);
            _chkOptions_ShowTile_FossilUI.addSelectionListener(_liveUpdateListener);
         }
      }
   }

   private void createUI_980_TourLog(final Composite parent) {

      {
         /*
          * Checkbox: Log Details
          */
         _chkOptions_LogDetails = new Button(parent, SWT.CHECK);
         _chkOptions_LogDetails.setText(Messages.Tour_Log_Checkbox_LogDetails);
         _chkOptions_LogDetails.addSelectionListener(_liveUpdateListener);
         GridDataFactory.fillDefaults().indent(0, 10).applyTo(_chkOptions_LogDetails);
      }
   }

   private void createUI_990_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         {
            /*
             * Checkbox: live update
             */
            _chkOptions_LiveUpdate = new Button(container, SWT.CHECK);
            _chkOptions_LiveUpdate.setText(Messages.Dialog_ImportConfig_Checkbox_LiveUpdate);
            _chkOptions_LiveUpdate.setToolTipText(Messages.Dialog_ImportConfig_Checkbox_LiveUpdate_Tooltip);
            _chkOptions_LiveUpdate.addSelectionListener(_liveUpdateListener);
         }
         {
            /*
             * Restore action
             */
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionRestoreDefaults);

            tbm.update(true);
         }
      }
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

   private String createUIText_MovedFiles() {

      final String moveFilesText = _selectedIC.isCreateBackup && _selectedIC.isDeleteDeviceFiles
            ? Messages.Dialog_ImportConfig_Info_MovedDeviceFiles
            : UI.EMPTY_STRING;

      return moveFilesText;
   }

   private void defineAll_ICColumns() {

      defineColumnIC_10_LauncherName();
      defineColumnIC_20_Backup();
      defineColumnIC_30_DeviceFolder();
      defineColumnIC_32_DeviceFiles();
      defineColumnIC_90_DeviceFiles_Delete();
      defineColumnIC_99_TurnOFF();
   }

   /**
    * Column: Item name
    */
   private void defineColumnIC_10_LauncherName() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_icColumnManager, "configName", SWT.LEAD); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Name);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Name);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(30));
      colDef.setColumnWeightData(new ColumnWeightData(30));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            cell.setText(((ImportConfig) cell.getElement()).name);
         }
      });
   }

   /**
    * Column: Backup
    */
   private void defineColumnIC_20_Backup() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_icColumnManager, "backup", SWT.LEAD); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Backup);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Backup);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(30));
      colDef.setColumnWeightData(new ColumnWeightData(30));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ImportConfig importConfig = (ImportConfig) cell.getElement();

            cell.setText(
                  importConfig.isCreateBackup //
                        ? importConfig.getBackupFolder()
                        : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Device folder
    */
   private void defineColumnIC_30_DeviceFolder() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_icColumnManager, "deviceFolder", SWT.LEAD); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Device);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Device);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(30));
      colDef.setColumnWeightData(new ColumnWeightData(30));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ImportConfig importConfig = (ImportConfig) cell.getElement();

            cell.setText(importConfig.getDeviceFolder());
         }
      });
   }

   /**
    * Column: Device files
    */
   private void defineColumnIC_32_DeviceFiles() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_icColumnManager, "deviceFiles", SWT.LEAD); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_DeviceFiles);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_DeviceFiles);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));
      colDef.setColumnWeightData(new ColumnWeightData(10));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ImportConfig importConfig = (ImportConfig) cell.getElement();

            cell.setText(importConfig.fileGlobPattern);
         }
      });
   }

   /**
    * Column: Delete device files
    */
   private void defineColumnIC_90_DeviceFiles_Delete() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_icColumnManager, "deleteFiles", SWT.CENTER); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_DeleteFiles_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_DeleteFiles_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Column_DeleteFiles_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ImportConfig importConfig = (ImportConfig) cell.getElement();

            final boolean isDeleteDeviceFiles = importConfig.isDeleteDeviceFiles;

            if (importConfig.isCreateBackup) {

               cell.setText(isDeleteDeviceFiles
                     ? Messages.App_Label_BooleanYes
                     : Messages.App_Label_BooleanNo);

               cell.setForeground(isDeleteDeviceFiles
                     ? ThemeUtil.getErrorColor()
                     : ThemeUtil.getDefaultForegroundColor_Table());
            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * Column: Turn watching OFF
    */
   private void defineColumnIC_99_TurnOFF() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_icColumnManager, "turnOFF", SWT.CENTER); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_TurnOFF_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_TurnOFF_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Column_TurnOFF_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));
      colDef.setColumnWeightData(new ColumnWeightData(10));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ImportConfig importConfig = (ImportConfig) cell.getElement();

            cell.setText(
                  importConfig.isTurnOffWatching
                        ? Messages.Dialog_ImportConfig_State_OFF
                        : Messages.Dialog_ImportConfig_State_ON);
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

      UI.disposeResource(_imageFileSystem);
   }

   /**
    * Do live update for this feature
    */
   private void doLiveUpdate() {

      final boolean isLiveUpdate = _chkOptions_LiveUpdate.getSelection();
      if (isLiveUpdate) {

         update_Model_From_UI_LiveUpdateValues();

         _rawDataView.doLiveUpdate(this);

      } else {

         // update model that live update is disabled

         _dialogEasyConfig.isLiveUpdate = isLiveUpdate;
      }
   }

   private void enable_IC_Controls() {

      final int numConfigs = _dialogEasyConfig.importConfigs.size();
      final boolean isBackup = _chkIC_CreateBackup.getSelection();

      _btnIC_SelectBackupFolder.setEnabled(isBackup);
      _comboIC_BackupFolder.setEnabled(isBackup);
      _lblIC_BackupFolder.setEnabled(isBackup);

      _chkIC_DeleteDeviceFiles.setEnabled(isBackup);
      _lblIC_DeleteFilesInfo.setEnabled(isBackup);

      _backupHistoryItems.setIsValidateFolder(isBackup);
      _backupHistoryItems.validateModifiedPath();

      _btnIC_Remove.setEnabled(numConfigs > 1);

      _comboIC_DeviceType.setEnabled(_chkIC_ImportFiles.getSelection());
   }

   private void enable_IL_Controls() {

      final int numLaunchers = _dialogEasyConfig.importLaunchers.size();
      final boolean isLauncherAvailable = numLaunchers > 0;

      final boolean isILSelected = _selectedIL != null;
      final boolean isLastMarkerSelected = isILSelected && _chkIL_SetLastMarker.getSelection();
      final boolean isAdjustTemperature = isILSelected && _chkIL_AdjustTemperature.getSelection();
      final boolean isWeatherRetrievalActivated = TourManager.isWeatherRetrievalActivated();

      boolean isSetTourType = isILSelected && _chkIL_SetTourType.getSelection();

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

                  for (final ComboViewerCadence combo : _comboTT_Cadence) {
                     combo.getCombo().setEnabled(isILSelected);
                  }

                  for (final Label label : _lblTT_Speed_TourTypeIcon) {

                     final Integer speedTTIndex = (Integer) label.getData(DATA_KEY_SPEED_TOUR_TYPE_INDEX);

                     final SpeedTourType speedTT = _selectedIL.speedTourTypes.get(speedTTIndex);
                     final long tourTypeId = speedTT.tourTypeId;

                     label.setImage(TourTypeImage.getTourTypeImage(tourTypeId));
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

// SET_FORMATTING_OFF

      _btnIL_Duplicate              .setEnabled(isILSelected);
      _btnIL_Remove                 .setEnabled(isILSelected && numLaunchers > 1);

      _chkIL_SetLastMarker          .setEnabled(isILSelected);
      _chkIL_SaveTour               .setEnabled(isILSelected);
      _chkIL_ShowInDashboard        .setEnabled(isILSelected);
      _chkIL_SetTourType            .setEnabled(isILSelected);

      _comboIL_TourType             .setEnabled(isILSelected && isSetTourType);

      _lblIL_ConfigName             .setEnabled(isILSelected);
      _lblIL_ConfigDescription      .setEnabled(isILSelected);

      _txtIL_ConfigName             .setEnabled(isILSelected);
      _txtIL_ConfigDescription      .setEnabled(isILSelected);

      // last marker
      _lblIL_LastMarker             .setEnabled(isLastMarkerSelected);
      _lblIL_LastMarkerDistanceUnit .setEnabled(isLastMarkerSelected);
      _lblIL_LastMarkerText         .setEnabled(isLastMarkerSelected);
      _spinnerIL_LastMarkerDistance .setEnabled(isLastMarkerSelected);
      _txtIL_LastMarker             .setEnabled(isLastMarkerSelected);

      // adjust temperature
      _lblIL_AvgTemperature                     .setEnabled(isAdjustTemperature);
      _lblIL_AvgTemperature_Unit                .setEnabled(isAdjustTemperature);
      _lblIL_TemperatureAdjustmentDuration      .setEnabled(isAdjustTemperature);
      _lblIL_TemperatureAdjustmentDuration_Unit .setEnabled(isAdjustTemperature);
      _spinnerIL_AvgTemperature                 .setEnabled(isAdjustTemperature);
      _spinnerIL_TemperatureAdjustmentDuration  .setEnabled(isAdjustTemperature);

      // Retrieve weather data
      _chkIL_RetrieveWeatherData    .setEnabled(isWeatherRetrievalActivated);

      _ilViewer.getTable()          .setEnabled(isLauncherAvailable);

// SET_FORMATTING_ON
   }

   private void fillSpeedTourTypeMenu(final IMenuManager menuMgr, final Link linkTourType) {

      final int speedTTIndex = (int) linkTourType.getData(DATA_KEY_SPEED_TOUR_TYPE_INDEX);
      final SpeedTourType speedTT = _selectedIL.speedTourTypes.get(speedTTIndex);
      final long speedTourTypeId = speedTT.tourTypeId;

      // add all tour types to the menu
      final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

      for (final TourType tourType : tourTypes) {

         boolean isChecked = false;
         if (speedTourTypeId == tourType.getTypeId()) {
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
      final TourType checkedTourType = _selectedIL.oneTourType;

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
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
//      return null;
   }

   /**
    * @param importLauncher
    * @return Returns from the model the last marker distance value in the current measurment
    *         system.
    */
   private double getMarkerDistanceValue(final ImportLauncher importLauncher) {

      return importLauncher.lastMarkerDistance / 1000.0 / UI.UNIT_VALUE_DISTANCE;
   }

   public EasyConfig getModifiedConfig() {
      return _dialogEasyConfig;
   }

   /**
    * @return Returns the selected distance of the last marker in meters.
    */
   private int getSelectedLastMarkerDistance() {

      final float lastMarkerDistance = _spinnerIL_LastMarkerDistance.getSelection()
            * UI.UNIT_VALUE_DISTANCE
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

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _leftPadding = convertHorizontalDLUsToPixels(11);
      _defaultPaneWidth = convertWidthInCharsToPixels(50);

//    FONT_BOLD = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

      parent.addDisposeListener(disposeEvent -> onDispose());

      /*
       * IC listener
       */
      _icSelectionListener = widgetSelectedAdapter(selectionEvent -> {
         onIC_Modified();
         enable_IC_Controls();
      });

      _icModifyListener = modifyEvent -> onIC_Modified();

      /*
       * Path listener
       */
      _ic_FolderFocusListener = focusLostAdapter(this::onIC_Folder_FocusLost);
      _ic_FolderModifyListener = modifyEvent -> {
         onIC_Folder_Modified(modifyEvent);
         onIC_Modified();
      };
      _ic_FolderKeyListener = keyPressedAdapter(this::onIC_Folder_KeyPressed);

      /*
       * IL listener
       */
      _ilModifyListener = modifyEvent -> onIL_Modified();

      _ilSelectionListener = widgetSelectedAdapter(selectionEvent -> onIL_Modified());

      /*
       * Field listener
       */
      _liveUpdateListener = widgetSelectedAdapter(selectionEvent -> doLiveUpdate());
      _liveUpdateMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         doLiveUpdate();
      };

      /*
       * Default mouse listener
       */
      _defaultMouseWheelListener = UI::adjustSpinnerValueOnMouseScroll;

      _speedTourTypeListener = widgetSelectedAdapter(selectionEvent -> UI.openControlMenu((Link) selectionEvent.widget));

      /*
       * Default modify listener
       */
      _defaultModify_Listener = widgetSelectedAdapter(selectionEvent -> {
         onIL_Modified();
         enable_IL_Controls();
      });

      _defaultModify_MouseWheelListener = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);

         onIL_Modified();
         enable_IL_Controls();
      };

   }

   @Override
   protected void okPressed() {

      update_Model_From_UI_IC();
      update_Model_From_UI_IL();
      update_Model_From_UI_SortedLists();
      update_Model_From_UI_LiveUpdateValues();

      super.okPressed();
   }

   private void onDispose() {

      disposeConfigImages();

      _prefStore.removePropertyChangeListener(_prefChangeListener);
   }

   private void onIC_Add(final boolean isCopy) {

      // keep modifications
      update_Model_From_UI_IC();

      // update model
      final ArrayList<ImportConfig> allConfigItems = _dialogEasyConfig.importConfigs;
      ImportConfig newIC;

      if (isCopy) {

         newIC = _selectedIC.clone();

         // make the clone more visible
         newIC.name = newIC.name + UI.SPACE + newIC.getId();

      } else {

         newIC = new ImportConfig();
      }

      allConfigItems.add(newIC);

      // update UI
      _icViewer.refresh();

      // prevent that the horizontal scrollbar is visible
      _icViewer.getTable().getParent().layout();

      _icViewer.setSelection(new StructuredSelection(newIC), true);

      _txtIC_ConfigName.setFocus();

      if (isCopy) {
         _txtIC_ConfigName.selectAll();
      }
   }

   private void onIC_DblClick() {

      /*
       * Set active config and close the dialog.
       */

      final StructuredSelection selection = (StructuredSelection) _icViewer.getSelection();
      final ImportConfig selectedIC = (ImportConfig) selection.getFirstElement();

      _dialogEasyConfig.setActiveImportConfig(selectedIC);

      okPressed();
   }

   private void onIC_Folder_FocusLost(final FocusEvent event) {

      final Combo combo = (Combo) event.widget;
      final HistoryItems historyItems = (HistoryItems) combo.getData();

      // keep manually entered folders in the history
      historyItems.updateHistory();
   }

   private void onIC_Folder_KeyPressed(final KeyEvent event) {

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

   private void onIC_Folder_Modified(final ModifyEvent event) {

      final Combo combo = (Combo) event.widget;
      final HistoryItems historyItems = (HistoryItems) combo.getData();

      historyItems.validateModifiedPath();
   }

   private void onIC_Modified() {

      if (_isInUIUpdate) {
         return;
      }

      if (_selectedIC == null) {
         return;
      }

      // update model which is displayed in the IC viewer
      update_Model_From_UI_IC();

      if (_selectedIC.isCreateBackup == false) {

         // disable deletion, deletion requires backup

         _chkIC_DeleteDeviceFiles.setSelection(false);
      }

      // update UI
      _icViewer.update(_selectedIC, null);
      _lblIC_DeleteFilesInfo.setText(createUIText_MovedFiles());
   }

   private void onIC_Remove() {

      final StructuredSelection selection = (StructuredSelection) _icViewer.getSelection();
      final ImportConfig selectedConfig = (ImportConfig) selection.getFirstElement();

      int selectedIndex = -1;
      final ArrayList<ImportConfig> allConfigItems = _dialogEasyConfig.importConfigs;

      // get index of the selected config
      for (int configIndex = 0; configIndex < allConfigItems.size(); configIndex++) {

         final ImportConfig config = allConfigItems.get(configIndex);

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
      allConfigItems.remove(selectedIndex);

      // update UI
      _icViewer.refresh();

      // select config at the same position
      if (allConfigItems.size() > 0) {

         if (selectedIndex >= allConfigItems.size()) {
            selectedIndex--;
         }

         final ImportConfig nextConfig = allConfigItems.get(selectedIndex);

         _icViewer.setSelection(new StructuredSelection(nextConfig), true);
      }

      _icViewer.getTable().setFocus();

      enable_IC_Controls();
   }

   private void onIC_SelectIC(final ISelection selection) {

      final ImportConfig selectedIC = (ImportConfig) ((StructuredSelection) selection).getFirstElement();

      if (_selectedIC == selectedIC) {
         // this is already selected
         return;
      }

      // update model from the old selected config
      update_Model_From_UI_IC();

      // set new model
      _selectedIC = selectedIC;

      update_UI_From_Model_IC();

      enable_IC_Controls();
   }

   private void onIL_Add(final boolean isCopy) {

      // keep modifications
      update_Model_From_UI_IL();

      // update model
      final ArrayList<ImportLauncher> ILItems = _dialogEasyConfig.importLaunchers;
      ImportLauncher newIL;

      if (isCopy) {

         newIL = _selectedIL.clone();

         // make the clone more visible
         newIL.name = newIL.name + UI.SPACE + newIL.getId();

      } else {

         newIL = new ImportLauncher();
      }

      ILItems.add(newIL);

      // update UI
      _ilViewer.refresh();

      // prevent that the horizontal scrollbar is visible
      _ilViewer.getTable().getParent().layout();

      _ilViewer.setSelection(new StructuredSelection(newIL), true);

      _txtIL_ConfigName.setFocus();

      if (isCopy) {
         _txtIL_ConfigName.selectAll();
      }
   }

   private void onIL_AddOne(final TourType tourType) {

      // keep modifications
      update_Model_From_UI_IL();

      // create new tt item
      final ImportLauncher newTTItem = new ImportLauncher();

      newTTItem.isSetTourType = true;
      newTTItem.tourTypeConfig = TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL;
      newTTItem.oneTourType = tourType;
      newTTItem.name = tourType.getName();

      // update model
      _dialogEasyConfig.importLaunchers.add(newTTItem);

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

      if (_selectedIL == null) {
         return;
      }

// SET_FORMATTING_OFF

      // update model which is displayed in the IL viewer
      _selectedIL.name                                = _txtIL_ConfigName.getText();
      _selectedIL.description                         = _txtIL_ConfigDescription.getText();

      _selectedIL.isSetLastMarker                     = _chkIL_SetLastMarker.getSelection();
      _selectedIL.lastMarkerDistance                  = getSelectedLastMarkerDistance();

      _selectedIL.isAdjustTemperature                 = _chkIL_AdjustTemperature.getSelection();
      _selectedIL.temperatureAdjustmentDuration       = _spinnerIL_TemperatureAdjustmentDuration.getSelection();
      _selectedIL.tourAvgTemperature                  = UI.convertTemperatureToMetric(_spinnerIL_AvgTemperature.getSelection());

      _selectedIL.isReplaceFirstTimeSliceElevation    = _chkIL_ReplaceFirstTimeSliceElevation.getSelection();
      _selectedIL.isRetrieveWeatherData               = _chkIL_RetrieveWeatherData.getSelection();
      _selectedIL.isSaveTour                          = _chkIL_SaveTour.getSelection();
      _selectedIL.isReplaceElevationFromSRTM          = _chkIL_ReplaceElevationFromSRTM.getSelection();
      _selectedIL.isSetTourType                       = _chkIL_SetTourType.getSelection();
      _selectedIL.isShowInDashboard                   = _chkIL_ShowInDashboard.getSelection();

// SET_FORMATTING_ON

      // update UI
      _ilViewer.update(_selectedIL, null);
   }

   private void onIL_Remove() {

      final StructuredSelection selection = (StructuredSelection) _ilViewer.getSelection();
      final ImportLauncher selectedConfig = (ImportLauncher) selection.getFirstElement();

      int selectedIndex = -1;
      final ArrayList<ImportLauncher> configItems = _dialogEasyConfig.importLaunchers;

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

      if (configItems.size() > 0) {

         if (selectedIndex >= configItems.size()) {
            selectedIndex--;
         }

         final ImportLauncher nextConfig = configItems.get(selectedIndex);

         _ilViewer.setSelection(new StructuredSelection(nextConfig), true);
      }

      _ilViewer.getTable().setFocus();
   }

   private void onPaintViewer(final Event event) {

      if (event.index != _ilColumnIndexConfigImage) {
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
//       event.height = PROFILE_IMAGE_HEIGHT;

         break;

      case SWT.PaintItem:

         final Image image = _rawDataView.getImportConfigImage(importLauncher, UI.IS_DARK_THEME);

         if (image != null && !image.isDisposed()) {

            final Rectangle rect = image.getBounds();

            final int x = event.x + event.width;
            final int yOffset = Math.max(0, (event.height - rect.height) / 2);

            event.gc.drawImage(image, x, event.y + yOffset);
         }

         break;
      }
   }

   private void onSelect_IC_Folder_Backup() {

      final String filterOSPath = _backupHistoryItems.getOSPath(
            _comboIC_BackupFolder.getText(),
            _selectedIC.getBackupFolder());

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

   private void onSelect_IC_Folder_Device() {

      final String filterOSPath = _deviceHistoryItems.getOSPath(//
            _comboIC_DeviceFolder.getText(),
            _selectedIC.getDeviceFolder());

      String selectedFolder = null;

      final TourbookFileSystem fileSystem = FileSystemManager.getTourbookFileSystem(filterOSPath);
      if (fileSystem != null) {
         // The current device is an external device (Dropbox...)

         try {
            //We use the retrieved TourbookFileSystem's implementation to select the folder to watch
            selectedFolder = fileSystem.selectFileSystemFolder(_parent.getShell(),
                  filterOSPath.replace(fileSystem.getId(), UI.EMPTY_STRING));
            if (StringUtils.hasContent(selectedFolder)) {
               _comboIC_DeviceFolder.setText(selectedFolder);
            }
         } catch (final Exception e) {
            StatusUtil.log(e);
         }
      } else {
         final DirectoryDialog dialog = new DirectoryDialog(_parent.getShell(), SWT.SAVE);

         dialog.setText(Messages.Dialog_ImportConfig_Dialog_DeviceFolder_Title);
         dialog.setMessage(Messages.Dialog_ImportConfig_Dialog_DeviceFolder_Message);
         dialog.setFilterPath(filterOSPath);

         selectedFolder = dialog.open();

         if (StringUtils.isNullOrEmpty(selectedFolder)) {
            return;
         }

         setErrorMessage(null);

         _deviceHistoryItems.onSelectFolderInDialog(selectedFolder);
      }
   }

   private void onSelect_IC_LauncherActions() {

      // show launcher actions
      _tabFolderEasy.setSelection(1);
   }

   private void onSelect_IL(final ISelection selection) {

      final ImportLauncher selectedIL = (ImportLauncher) ((StructuredSelection) selection).getFirstElement();

      if (_selectedIL == selectedIL) {
         // this is already selected
         return;
      }

      // update model from the old selected config
      update_Model_From_UI_IL();

      // set new model
      _selectedIL = selectedIL;

      update_UI_From_Model_IL();

      enable_IL_Controls();
   }

   private void onSelect_IL_TourType() {

      final Enum<TourTypeConfig> selectedTourTypeItem = getSelectedTourTypeConfig();

      showTourTypePage(selectedTourTypeItem);

      update_Model_From_UI_IL();
      update_UI_From_Model_IL();

      enable_IL_Controls();

      redrawILViewer();
   }

   private void onSelectDevice() {

      if (_comboIC_DeviceType == null) {
         return;
      }

      if (_lblIC_DeviceFolder == null) {
         return;
      }

      final int deviceIndex = _comboIC_DeviceType.getSelectionIndex();
      final boolean isDeviceLocal = deviceIndex == 0; //Local device

      _lblIC_DeviceFolder.setEnabled(isDeviceLocal);
      _comboIC_DeviceFolder.setEnabled(isDeviceLocal);

      String deviceFolder = _selectedIC.getDeviceFolder();

      //We update the file system icon
      UI.disposeResource(_imageFileSystem);
      if (isDeviceLocal) {

         _imageFileSystem = TourbookPlugin.getThemedImageDescriptor(Images.EasyImport_Harddrive).createImage();

      } else if (NIO.isTourBookFileSystem(_comboIC_DeviceType.getText())) {

         _imageFileSystem = FileSystemManager
               .getTourbookFileSystem(_comboIC_DeviceType.getText())
               .getFileSystemImageDescriptor()
               .createImage();
      }

      if (_imageFileSystem != null && !_imageFileSystem.isDisposed()) {

         _lblIC_FileSystemImage.setImage(_imageFileSystem);
      }

      if (isDeviceLocal && NIO.isTourBookFileSystem(deviceFolder)) {

         deviceFolder = UI.EMPTY_STRING;

      } else if (isDeviceLocal == false && NIO.isTourBookFileSystem(deviceFolder) == false) {

         deviceFolder = FileSystemManager.getTourbookFileSystem(_comboIC_DeviceType.getText()).getDisplayId();
      }

      _comboIC_DeviceFolder.setText(deviceFolder);

      _chkIC_CreateBackup.setEnabled(isDeviceLocal);
      _chkIC_DeleteDeviceFiles.setEnabled(isDeviceLocal);

      if (isDeviceLocal) {

         enable_IC_Controls();

      } else {

         _comboIC_BackupFolder.setText(UI.EMPTY_STRING);
         _comboIC_BackupFolder.setEnabled(false);

         _chkIC_CreateBackup.setSelection(false);
         _chkIC_DeleteDeviceFiles.setSelection(false);
         _lblIC_DeleteFilesInfo.setText(UI.EMPTY_STRING);
         _lblIC_BackupFolder.setEnabled(false);
         _btnIC_SelectBackupFolder.setEnabled(false);

         _backupHistoryItems.setIsValidateFolder(false);
         _backupHistoryItems.validateModifiedPath();
      }
   }

   private void onSpeed_IL_TT_Add() {

      update_Model_From_UI_IL();

      final ArrayList<SpeedTourType> speedTourTypes = _selectedIL.speedTourTypes;

      // update model
      speedTourTypes.add(0, new SpeedTourType());

      // sort by speed
      Collections.sort(speedTourTypes);

      // update UI + model
      update_UI_From_Model_IL();

      enable_IL_Controls();

      // set focus to the speed
      _spinnerTT_Speed_AvgSpeed[0].setFocus();

      redrawILViewer();
   }

   private void onSpeed_IL_TT_Remove(final int speedTTIndex) {

      // update model
      update_Model_From_UI_IL();

      final ArrayList<SpeedTourType> speedTourTypes = _selectedIL.speedTourTypes;

      final SpeedTourType removedSpeedTT = speedTourTypes.get(speedTTIndex);

      speedTourTypes.remove(removedSpeedTT);

      // update UI
      update_UI_From_Model_IL();

      enable_IL_Controls();

      redrawILViewer();
   }

   private void onSpeed_IL_TT_SetTourType(final int speedTTIndex, final TourType tourType) {

      /*
       * Update UI
       */
      final Image image = TourTypeImage.getTourTypeImage(tourType.getTypeId());
      final Label ttIcon = _lblTT_Speed_TourTypeIcon[speedTTIndex];
      final Link ttLink = _linkTT_Speed_TourType[speedTTIndex];

      ttIcon.setImage(image);

      ttLink.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);
      ttLink.setData(DATA_KEY_TOUR_TYPE_ID, tourType.getTypeId());

      _speedTourType_OuterContainer.layout();

      // update UI with modified tour type
      update_Model_From_UI_IL();
      _ilViewer.update(_selectedIL, null);

      redrawILViewer();
   }

   private void onSpeed_IL_TT_Sort() {

      update_Model_From_UI_IL();
      update_UI_From_Model_IL();

      redrawILViewer();
   }

   private void redrawILViewer() {

      // IL viewer MUST be redrawn to show modified tour type image
      _ilViewer.getTable().redraw();
   }

   @Override
   public void resetToDefaults() {

// SET_FORMATTING_OFF

      _chkOptions_LiveUpdate                    .setSelection(EasyConfig.IS_LIVE_UPDATE_DEFAULT);
      _chkOptions_LogDetails                    .setSelection(EasyConfig.IS_LOG_DETAILS_DEFAULT);

      _chkOptions_ShowTile_CloudApps            .setSelection(EasyConfig.IS_SHOW_TILE_CLOUD_APPS_DEFAULT);
      _chkOptions_ShowTile_Files                .setSelection(EasyConfig.IS_SHOW_TILE_FILES_DEFAULT);
      _chkOptions_ShowTile_FossilUI             .setSelection(EasyConfig.IS_SHOW_TILE_FOSSIL_UI_DEFAULT);
      _chkOptions_ShowTile_SerialPort           .setSelection(EasyConfig.IS_SHOW_TILE_SERIAL_PORT_DEFAULT);
      _chkOptions_ShowTile_SerialPortWithConfig .setSelection(EasyConfig.IS_SHOW_TILE_SERIAL_PORT_WITH_CONFIG_DEFAULT);

      _spinnerDash_AnimationCrazinessFactor     .setSelection(EasyConfig.ANIMATION_CRAZINESS_FACTOR_DEFAULT);
      _spinnerDash_AnimationDuration            .setSelection(EasyConfig.ANIMATION_DURATION_DEFAULT);
      _spinnerDash_BgOpacity                    .setSelection(EasyConfig.BACKGROUND_OPACITY_DEFAULT);
      _spinnerDash_NumHTiles                    .setSelection(EasyConfig.HORIZONTAL_TILES_DEFAULT);
      _spinnerDash_StateTooltipWidth            .setSelection(EasyConfig.STATE_TOOLTIP_WIDTH_DEFAULT);
      _spinnerDash_TileSize                     .setSelection(EasyConfig.TILE_SIZE_DEFAULT);

// SET_FORMATTING_ON

      doLiveUpdate();
   }

   private void restoreState() {

      /*
       * Tab folder
       */
      final int selectedTab = _initialTab == -1
            ? Util.getStateInt(_state, STATE_SELECTED_TAB_FOLDER, 0)
            : _initialTab;

      _tabFolderEasy.setSelection(selectedTab);

      _icViewer.setInput(new Object());
      _ilViewer.setInput(new Object());

      /*
       * Import Configuration
       */
      _backupHistoryItems.restoreState(
            _state.getArray(STATE_BACKUP_FOLDER_HISTORY_ITEMS),
            _state.getArray(STATE_BACKUP_DEVICE_HISTORY_ITEMS));

      _deviceHistoryItems.restoreState(
            _state.getArray(STATE_DEVICE_FOLDER_HISTORY_ITEMS),
            _state.getArray(STATE_DEVICE_DEVICE_HISTORY_ITEMS));

      // select active config, compare by name because a clone has a different it
      final ImportConfig activeIC = _dialogEasyConfig.getActiveImportConfig();

      final ArrayList<ImportConfig> importConfigs = _dialogEasyConfig.importConfigs;
      ImportConfig initialIC = null;

      for (final ImportConfig importConfig : importConfigs) {
         if (importConfig.name.equals(activeIC.name)) {
            initialIC = importConfig;
            break;
         }
      }

      if (initialIC == null) {
         initialIC = importConfigs.get(0);
      }

      _icViewer.setSelection(new StructuredSelection(initialIC));

      // ensure that the selected also has the focus, these are 2 different things
      final Table icTable = _icViewer.getTable();
      icTable.setSelection(icTable.getSelectionIndex());

      /*
       * Import Launcher
       */
      final String stateILName = Util.getStateString(_state, STATE_SELECTED_IMPORT_LAUNCHER, UI.EMPTY_STRING);
      final ArrayList<ImportLauncher> importLaunchers = _dialogEasyConfig.importLaunchers;

      ImportLauncher initialIL = null;

      for (final ImportLauncher importLauncher : importLaunchers) {
         if (importLauncher.name.equals(stateILName)) {
            initialIL = importLauncher;
            break;
         }
      }

      // select first import launcher
      if (initialIL == null) {
         initialIL = importLaunchers.get(0);
      }

      final Table ilTable = _ilViewer.getTable();

      _ilViewer.setSelection(new StructuredSelection(initialIL));

      // ensure that the selected also has the focus, these are 2 different things
      ilTable.setSelection(ilTable.getSelectionIndex());

// SET_FORMATTING_OFF

      /*
       * Dashboard
       */
      _chkOptions_DisplayAbsoluteFilePath       .setSelection(_dialogEasyConfig.stateToolTipDisplayAbsoluteFilePath);
      _chkOptions_LiveUpdate                    .setSelection(_dialogEasyConfig.isLiveUpdate);
      _chkOptions_LogDetails                    .setSelection(_dialogEasyConfig.isLogDetails);

      _chkOptions_ShowTile_CloudApps            .setSelection(_dialogEasyConfig.isShowTile_CloudApps);
      _chkOptions_ShowTile_Files                .setSelection(_dialogEasyConfig.isShowTile_Files);
      _chkOptions_ShowTile_FossilUI             .setSelection(_dialogEasyConfig.isShowTile_FossilUI);
      _chkOptions_ShowTile_SerialPort           .setSelection(_dialogEasyConfig.isShowTile_SerialPort);
      _chkOptions_ShowTile_SerialPortWithConfig .setSelection(_dialogEasyConfig.isShowTile_SerialPortWithConfig);

      _spinnerDash_AnimationCrazinessFactor     .setSelection(_dialogEasyConfig.animationCrazinessFactor);
      _spinnerDash_AnimationDuration            .setSelection(_dialogEasyConfig.animationDuration);
      _spinnerDash_BgOpacity                    .setSelection(_dialogEasyConfig.backgroundOpacity);
      _spinnerDash_NumHTiles                    .setSelection(_dialogEasyConfig.numHorizontalTiles);
      _spinnerDash_StateTooltipWidth            .setSelection(_dialogEasyConfig.stateToolTipWidth);
      _spinnerDash_TileSize                     .setSelection(_dialogEasyConfig.tileSize);

// SET_FORMATTING_ON
   }

   private void saveState() {

      _state.put(STATE_SELECTED_IMPORT_LAUNCHER, _selectedIL == null ? UI.EMPTY_STRING : _selectedIL.name);

      _icColumnManager.saveState(_stateIC);
      _ilColumnManager.saveState(_stateIL);

      _backupHistoryItems.saveState(_state, STATE_BACKUP_FOLDER_HISTORY_ITEMS, STATE_BACKUP_DEVICE_HISTORY_ITEMS);
      _deviceHistoryItems.saveState(_state, STATE_DEVICE_FOLDER_HISTORY_ITEMS, STATE_DEVICE_DEVICE_HISTORY_ITEMS);

      // selected tab folder
      final int selectedTab = _tabFolderEasy.getSelectionIndex();
      _state.put(STATE_SELECTED_TAB_FOLDER, selectedTab < 0 ? 0 : selectedTab);
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

   private void update_Model_From_UI_IC() {

      if (_selectedIC == null) {
         return;
      }

// SET_FORMATTING_OFF

      _selectedIC.name                 = _txtIC_ConfigName.getText();

      _selectedIC.isCreateBackup       = _chkIC_CreateBackup.getSelection();
      _selectedIC.isDeleteDeviceFiles  = _chkIC_DeleteDeviceFiles.getSelection();
      _selectedIC.isTurnOffWatching    = _chkIC_TurnOffWatching.getSelection();

      _selectedIC.setBackupFolder(     _comboIC_BackupFolder.getText());
      _selectedIC.setDeviceType(       _comboIC_DeviceType.getSelectionIndex());
      _selectedIC.setDeviceFolder(     _comboIC_DeviceFolder.getText());

      _selectedIC.fileGlobPattern      = _txtIC_DeviceFiles.getText();

// SET_FORMATTING_ON
   }

   /**
    * Set data from the UI into the model.
    */
   private void update_Model_From_UI_IL() {

      if (_selectedIL == null) {
         return;
      }

// SET_FORMATTING_OFF

      _selectedIL.name                 = _txtIL_ConfigName.getText();
      _selectedIL.description          = _txtIL_ConfigDescription.getText();
      _selectedIL.isSaveTour           = _chkIL_SaveTour.getSelection();
      _selectedIL.isShowInDashboard    = _chkIL_ShowInDashboard.getSelection();

      // last marker
      _selectedIL.isSetLastMarker      = _chkIL_SetLastMarker.getSelection();
      _selectedIL.lastMarkerDistance   = getSelectedLastMarkerDistance();
      _selectedIL.lastMarkerText       = _txtIL_LastMarker.getText();

      // tour type
      final Enum<TourTypeConfig> selectedTourTypeConfig = getSelectedTourTypeConfig();
      _selectedIL.tourTypeConfig       = selectedTourTypeConfig;
      _selectedIL.isSetTourType        = _chkIL_SetTourType.getSelection();

// SET_FORMATTING_ON

      /*
       * Set tour type data
       */
      if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(selectedTourTypeConfig)) {

         final ArrayList<SpeedTourType> speedTourTypes = _selectedIL.speedTourTypes;

         if (_spinnerTT_Speed_AvgSpeed != null) {

            final ArrayList<SpeedTourType> newSpeedTourTypes = new ArrayList<>();

            for (int speedTTIndex = 0; speedTTIndex < speedTourTypes.size(); speedTTIndex++) {

               final Spinner spinnerAvgSpeed = _spinnerTT_Speed_AvgSpeed[speedTTIndex];
               final Link linkTourType = _linkTT_Speed_TourType[speedTTIndex];
               final ComboViewerCadence comboCadence = _comboTT_Cadence[speedTTIndex];

               final SpeedTourType speedTourType = new SpeedTourType();

               speedTourType.avgSpeed = spinnerAvgSpeed.getSelection() * UI.UNIT_VALUE_DISTANCE;
               speedTourType.cadenceMultiplier = comboCadence.getSelectedCadence();

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

         _selectedIL.setupItemImage();

      } else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(selectedTourTypeConfig)) {

         update_Model_From_UI_OneTourType();

      } else {

         // this is the default

         _selectedIL.setupItemImage();
      }
   }

   private void update_Model_From_UI_LiveUpdateValues() {

// SET_FORMATTING_OFF

      _dialogEasyConfig.isLiveUpdate                           = _chkOptions_LiveUpdate.getSelection();
      _dialogEasyConfig.isLogDetails                           = _chkOptions_LogDetails.getSelection();

      _dialogEasyConfig.isShowTile_CloudApps                   = _chkOptions_ShowTile_CloudApps.getSelection();
      _dialogEasyConfig.isShowTile_Files                       = _chkOptions_ShowTile_Files.getSelection();
      _dialogEasyConfig.isShowTile_FossilUI                    = _chkOptions_ShowTile_FossilUI.getSelection();
      _dialogEasyConfig.isShowTile_SerialPort                  = _chkOptions_ShowTile_SerialPort.getSelection();
      _dialogEasyConfig.isShowTile_SerialPortWithConfig        = _chkOptions_ShowTile_SerialPortWithConfig.getSelection();

      _dialogEasyConfig.animationCrazinessFactor               = _spinnerDash_AnimationCrazinessFactor.getSelection();
      _dialogEasyConfig.animationDuration                      = _spinnerDash_AnimationDuration.getSelection();
      _dialogEasyConfig.backgroundOpacity                      = _spinnerDash_BgOpacity.getSelection();
      _dialogEasyConfig.numHorizontalTiles                     = _spinnerDash_NumHTiles.getSelection();
      _dialogEasyConfig.stateToolTipDisplayAbsoluteFilePath    = _chkOptions_DisplayAbsoluteFilePath.getSelection();
      _dialogEasyConfig.stateToolTipWidth                      = _spinnerDash_StateTooltipWidth.getSelection();
      _dialogEasyConfig.tileSize                               = _spinnerDash_TileSize.getSelection();

// SET_FORMATTING_ON
   }

   private void update_Model_From_UI_OneTourType() {

      final Object tourTypeId = _linkTT_One_TourType.getData(DATA_KEY_TOUR_TYPE_ID);

      if (tourTypeId instanceof Long) {
         _selectedIL.oneTourType = TourDatabase.getTourType((long) tourTypeId);
      } else {

         _selectedIL.oneTourType = null;
      }

      _selectedIL.setupItemImage();
      _selectedIL.oneTourTypeCadence = _comboIL_One_TourType_Cadence.getSelectedCadence();
   }

   /**
    * Create config lists in the table sort order.
    */
   private void update_Model_From_UI_SortedLists() {

      /*
       * Import configs
       */
      final ArrayList<ImportConfig> importConfigs = _dialogEasyConfig.importConfigs;
      importConfigs.clear();

      for (final TableItem tableItem : _icViewer.getTable().getItems()) {

         final Object itemData = tableItem.getData();

         if (itemData instanceof ImportConfig) {
            importConfigs.add((ImportConfig) itemData);
         }
      }

      /*
       * Import launchers
       */
      final ArrayList<ImportLauncher> importLauchers = _dialogEasyConfig.importLaunchers;
      importLauchers.clear();

      for (final TableItem tableItem : _ilViewer.getTable().getItems()) {

         final Object itemData = tableItem.getData();

         if (itemData instanceof ImportLauncher) {
            importLauchers.add((ImportLauncher) itemData);
         }
      }
   }

   private void update_UI_From_Model_IC() {

      if (_selectedIC == null) {
         return;
      }

      _isInUIUpdate = true;
      {
         _txtIC_ConfigName.setText(_selectedIC.name);

         _chkIC_CreateBackup.setSelection(_selectedIC.isCreateBackup);
         _chkIC_DeleteDeviceFiles.setSelection(_selectedIC.isDeleteDeviceFiles);
         _chkIC_TurnOffWatching.setSelection(_selectedIC.isTurnOffWatching);

         _comboIC_BackupFolder.setText(_selectedIC.getBackupFolder());
         _comboIC_DeviceFolder.setText(_selectedIC.getDeviceFolder());
         _comboIC_DeviceType.select(_selectedIC.getDeviceType());

         _txtIC_DeviceFiles.setText(_selectedIC.fileGlobPattern);
         _lblIC_DeleteFilesInfo.setText(createUIText_MovedFiles());
      }
      _isInUIUpdate = false;
   }

   private void update_UI_From_Model_IL() {

      if (_selectedIL == null) {
         return;
      }

      _isInUIUpdate = true;
      {
         final double distance = getMarkerDistanceValue(_selectedIL);
         final double distance10 = distance * 10;
         final int distanceValue = (int) (distance10 + 0.5);
         final String lastMarkerText = _selectedIL.lastMarkerText;

         _txtIL_ConfigName.setText(_selectedIL.name);
         _txtIL_ConfigDescription.setText(_selectedIL.description);
         _chkIL_SaveTour.setSelection(_selectedIL.isSaveTour);
         _chkIL_ShowInDashboard.setSelection(_selectedIL.isShowInDashboard);

         // last marker
         _chkIL_SetLastMarker.setSelection(_selectedIL.isSetLastMarker);
         _spinnerIL_LastMarkerDistance.setSelection(distanceValue);
         _txtIL_LastMarker.setText(lastMarkerText == null ? UI.EMPTY_STRING : lastMarkerText);

         // adjust temperature
         final int temperature = (int) (UI.convertTemperatureFromMetric(_selectedIL.tourAvgTemperature) + 0.5);
         _chkIL_AdjustTemperature.setSelection(_selectedIL.isAdjustTemperature);
         _spinnerIL_AvgTemperature.setSelection(temperature);
         _spinnerIL_TemperatureAdjustmentDuration.setSelection(_selectedIL.temperatureAdjustmentDuration);
         updateUI_TemperatureAdjustmentDuration();

         // Retrieve Weather Data
         _chkIL_RetrieveWeatherData.setSelection(_selectedIL.isRetrieveWeatherData);

         // adjust elevation
         _chkIL_ReplaceFirstTimeSliceElevation.setSelection(_selectedIL.isReplaceFirstTimeSliceElevation);

         // set elevation from SRTM data
         _chkIL_ReplaceElevationFromSRTM.setSelection(_selectedIL.isReplaceElevationFromSRTM);

         final Enum<TourTypeConfig> tourTypeConfig = _selectedIL.tourTypeConfig;
         final boolean isSetTourType = tourTypeConfig != null && _selectedIL.isSetTourType;

         // Set tour type
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
               createUI_566_IL_SpeedTourType_Fields();

               final ArrayList<SpeedTourType> speedTourTypes = _selectedIL.speedTourTypes;

               final int speedTTSize = speedTourTypes.size();

               for (int speedTTIndex = 0; speedTTIndex < speedTTSize; speedTTIndex++) {

                  final SpeedTourType speedTT = speedTourTypes.get(speedTTIndex);
                  final long tourTypeId = speedTT.tourTypeId;

                  final Spinner spinnerAvgSpeed = _spinnerTT_Speed_AvgSpeed[speedTTIndex];
                  final Link linkTourType = _linkTT_Speed_TourType[speedTTIndex];
                  final Label labelTourTypeIcon = _lblTT_Speed_TourTypeIcon[speedTTIndex];
                  final ComboViewerCadence comboCadence = _comboTT_Cadence[speedTTIndex];

                  // update UI
                  final double avgSpeed = (speedTT.avgSpeed / UI.UNIT_VALUE_DISTANCE) + 0.0001;
                  spinnerAvgSpeed.setSelection((int) avgSpeed);

                  if (tourTypeId == TourDatabase.ENTITY_IS_NOT_SAVED) {

                     // tour type is not yet set

                     linkTourType.setData(DATA_KEY_TOUR_TYPE_ID, null);
                     linkTourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
                     labelTourTypeIcon.setImage(null);

                  } else {

                     linkTourType.setData(DATA_KEY_TOUR_TYPE_ID, tourTypeId);
                     linkTourType.setText(
                           UI.LINK_TAG_START
                                 + net.tourbook.ui.UI.getTourTypeLabel(tourTypeId)
                                 + UI.LINK_TAG_END);
                     labelTourTypeIcon.setImage(TourTypeImage.getTourTypeImage(tourTypeId));
                  }

                  if (speedTT.cadenceMultiplier != null) {
                     comboCadence.setSelection(speedTT.cadenceMultiplier);
                  }

                  // keep references
                  labelTourTypeIcon.setData(DATA_KEY_SPEED_TOUR_TYPE_INDEX, speedTTIndex);
                  linkTourType.setData(DATA_KEY_SPEED_TOUR_TYPE_INDEX, speedTTIndex);
                  spinnerAvgSpeed.setData(DATA_KEY_SPEED_TOUR_TYPE_INDEX, speedTTIndex);
                  comboCadence.setData(DATA_KEY_SPEED_TOUR_TYPE_INDEX, speedTTIndex);
                  _actionTTSpeed_Delete[speedTTIndex].setData(speedTTIndex);

               }
            }
            _speedTourType_OuterContainer.setRedraw(true);

         } else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {

            TourType tourType = null;

            final TourType oneTourType = _selectedIL.oneTourType;
            if (oneTourType != null) {

               final long tourTypeId = oneTourType.getTypeId();
               tourType = TourDatabase.getTourType(tourTypeId);
            }

            if (_selectedIL.oneTourTypeCadence != null) {
               _comboIL_One_TourType_Cadence.setSelection(_selectedIL.oneTourTypeCadence);
            }

            updateUI_OneTourType(tourType);

         } else {

            // this is the default, a tour type is not set
         }

         showTourTypePage(tourTypeConfig);

      }
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
         _comboTT_Cadence = null;
      }
   }

   private void updateUI_OneTourType(final TourType tourType) {

      if (tourType == null) {

         _lblIL_One_TourTypeIcon.setImage(null);

         _linkTT_One_TourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
         _linkTT_One_TourType.setData(DATA_KEY_TOUR_TYPE_ID, null);

      } else {

         final Image image = TourTypeImage.getTourTypeImage(tourType.getTypeId());

         _lblIL_One_TourTypeIcon.setImage(image);

         _linkTT_One_TourType.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);
         _linkTT_One_TourType.setData(DATA_KEY_TOUR_TYPE_ID, tourType.getTypeId());
      }

      // update the model that the table displays the correct image
      update_Model_From_UI_OneTourType();

      redrawILViewer();
   }

   private void updateUI_TemperatureAdjustmentDuration() {

      final long duration = _spinnerIL_TemperatureAdjustmentDuration.getSelection();

      final Period durationPeriod = new Period(0, duration * 1000, _durationTemplate);

      _lblIL_TemperatureAdjustmentDuration_Unit.setText(durationPeriod.toString(UI.DEFAULT_DURATION_FORMATTER));
   }
}
