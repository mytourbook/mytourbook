/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.rawData;

import static net.tourbook.ui.UI.getIconUrl;
import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.FileSystemManager;
import net.tourbook.common.NIO;
import net.tourbook.common.TourbookFileSystem;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.tooltip.ActionToolbarSlideoutAdv;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.ICloseOpenedDialogs;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ColumnProfile;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.NoAutoScalingImageDataProvider;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.data.FlatGainLoss;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.download.CloudDownloaderManager;
import net.tourbook.extension.download.TourbookCloudDownloader;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.extension.upload.ActionUpload;
import net.tourbook.importdata.DeviceImportState;
import net.tourbook.importdata.DialogEasyImportConfig;
import net.tourbook.importdata.EasyConfig;
import net.tourbook.importdata.EasyImportManager;
import net.tourbook.importdata.EasyLauncherUtils;
import net.tourbook.importdata.ImportConfig;
import net.tourbook.importdata.ImportLauncher;
import net.tourbook.importdata.ImportState_Easy;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.OSFile;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.SpeedTourType;
import net.tourbook.importdata.TourTypeConfig;
import net.tourbook.photo.ImageUtils;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageImport;
import net.tourbook.preferences.ViewContext;
import net.tourbook.tag.TagGroup;
import net.tourbook.tag.TagGroupManager;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.CadenceMultiplier;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogManager.AutoOpenEvent;
import net.tourbook.tour.TourLogState;
import net.tourbook.tour.TourLogView;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.tour.location.TourLocationManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ITourProviderAll;
import net.tourbook.ui.ITourProviderByID;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.action.TourActionCategory;
import net.tourbook.ui.action.TourActionManager;
import net.tourbook.ui.views.TableViewerTourInfoToolTip;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.ViewNames;
import net.tourbook.web.WEB;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.Period;
import org.joda.time.PeriodType;

/**
 * Tour import view
 */
public class RawDataView extends ViewPart implements

      ITourProviderAll,
      ITourViewer3,
      ITourProviderByID,
      ICloseOpenedDialogs {

   public static final String ID = "net.tourbook.views.rawData.RawDataView"; //$NON-NLS-1$

   private static final char  NL = UI.NEW_LINE;

   // db state
   private static final String           IMAGE_ASSIGN_MERGED_TOUR                  = "IMAGE_ASSIGN_MERGED_TOUR";               //$NON-NLS-1$
   private static final String           IMAGE_DATABASE                            = "IMAGE_DATABASE";                         //$NON-NLS-1$
   //
   private static final String           IMAGE_DATABASE_OTHER_PERSON               = "IMAGE_DATABASE_OTHER_PERSON";            //$NON-NLS-1$
   private static final String           IMAGE_DELETE                              = "IMAGE_DELETE";                           //$NON-NLS-1$
   private static final String           IMAGE_ICON_PLACEHOLDER                    = "IMAGE_ICON_PLACEHOLDER";                 //$NON-NLS-1$
   // import state
   private static final String           IMAGE_STATE_DELETE                        = "IMAGE_STATE_DELETE";                     //$NON-NLS-1$
   private static final String           IMAGE_STATE_MOVED                         = "IMAGE_STATE_MOVED";                      //$NON-NLS-1$
   // OLD UI
   private static final String           IMAGE_DATA_TRANSFER                       = "IMAGE_DATA_TRANSFER";                    //$NON-NLS-1$
   private static final String           IMAGE_DATA_TRANSFER_DIRECT                = "IMAGE_DATA_TRANSFER_DIRECT";             //$NON-NLS-1$
   private static final String           IMAGE_IMPORT_FROM_FILES                   = "IMAGE_IMPORT_FROM_FILES";                //$NON-NLS-1$
   private static final String           IMAGE_NEW_UI                              = "IMAGE_NEW_UI";                           //$NON-NLS-1$
   // simple easy import
   private static final String           IMAGE_DEVICE_FOLDER_ERROR                 = "IMAGE_DEVICE_FOLDER_ERROR";              //$NON-NLS-1$
   private static final String           IMAGE_DEVICE_FOLDER_IS_CHECKING           = "IMAGE_DEVICE_FOLDER_IS_CHECKING";        //$NON-NLS-1$
   private static final String           IMAGE_DEVICE_FOLDER_NOT_SETUP             = "IMAGE_DEVICE_FOLDER_NOT_SETUP";          //$NON-NLS-1$
   private static final String           IMAGE_DEVICE_FOLDER_OFF                   = "IMAGE_DEVICE_FOLDER_OFF";                //$NON-NLS-1$
   private static final String           IMAGE_DEVICE_FOLDER_OK                    = "IMAGE_DEVICE_FOLDER_OK";                 //$NON-NLS-1$
   static final String                   IMAGE_DEVICE_TURN_ON                      = "IMAGE_DEVICE_TURN_ON";                   //$NON-NLS-1$
   static final String                   IMAGE_DEVICE_TURN_OFF                     = "IMAGE_DEVICE_TURN_OFF";                  //$NON-NLS-1$
   static final String                   IMAGE_STATE_OK                            = "IMAGE_STATE_OK";                         //$NON-NLS-1$
   static final String                   IMAGE_STATE_ERROR                         = "IMAGE_STATE_ERROR";                      //$NON-NLS-1$
   //
   private static final String           HTML_TD                                   = "<td>";                                   //$NON-NLS-1$
   private static final String           HTML_TD_SPACE                             = "<td ";                                   //$NON-NLS-1$
   private static final String           HTML_TD_END                               = "</td>";                                  //$NON-NLS-1$
   private static final String           HTML_TR                                   = "<tr>";                                   //$NON-NLS-1$
   private static final String           HTML_TR_END                               = "</tr>";                                  //$NON-NLS-1$
   //
   private static final String           JS_FUNCTION_ON_SELECT_IMPORT_CONFIG       = "onSelectImportConfig";                   //$NON-NLS-1$
   //
   private static final String           WEB_RESOURCE_TITLE_FONT                   = "Nunito-Bold.ttf";                        //$NON-NLS-1$
   private static final String           WEB_RESOURCE_TOUR_IMPORT_BG_IMAGE         = "mytourbook-icon.svg";                    //$NON-NLS-1$
   private static final String           WEB_RESOURCE_TOUR_IMPORT_CSS              = "tour-import.css";                        //$NON-NLS-1$
   private static final String           WEB_RESOURCE_TOUR_IMPORT_CSS3             = "tour-import-css3.css";                   //$NON-NLS-1$
   //
   private static final String           CSS_IMPORT_BACKGROUND                     = "div.import-background";                  //$NON-NLS-1$
   private static final String           CSS_IMPORT_TILE                           = "a.import-tile";                          //$NON-NLS-1$
   //
   public static final String            STATE_DEFAULT_CADENCE_MULTIPLIER          = "STATE_DEFAULT_CADENCE_MULTIPLIER";       //$NON-NLS-1$
   public static final CadenceMultiplier STATE_DEFAULT_CADENCE_MULTIPLIER_DEFAULT  = CadenceMultiplier.RPM;
   public static final String            STATE_IMPORT_UI                           = "importUI";                               //$NON-NLS-1$
   public static final ImportUI          STATE_IMPORT_UI_DEFAULT                   = ImportUI.EASY_IMPORT_FANCY;
   private static final String           STATE_IMPORTED_FILENAMES                  = "importedFilenames";                      //$NON-NLS-1$
   private static final String           STATE_SELECTED_TOUR_INDICES               = "SelectedTourIndices";                    //$NON-NLS-1$
   //
   public static final String            STATE_IS_CHECKSUM_VALIDATION              = "isChecksumValidation";                   //$NON-NLS-1$
   public static final boolean           STATE_IS_CHECKSUM_VALIDATION_DEFAULT      = true;
   public static final String            STATE_IS_CONVERT_WAYPOINTS                = "STATE_IS_CONVERT_WAYPOINTS";             //$NON-NLS-1$
   public static final boolean           STATE_IS_CONVERT_WAYPOINTS_DEFAULT        = true;
   public static final String            STATE_IS_CREATE_TOUR_ID_WITH_TIME         = "isCreateTourIdWithTime";                 //$NON-NLS-1$
   public static final boolean           STATE_IS_CREATE_TOUR_ID_WITH_TIME_DEFAULT = false;
   public static final String            STATE_IS_IGNORE_INVALID_FILE              = "isIgnoreInvalidFile";                    //$NON-NLS-1$
   public static final boolean           STATE_IS_IGNORE_INVALID_FILE_DEFAULT      = true;
   public static final String            STATE_IS_MERGE_TRACKS                     = "isMergeTracks";                          //$NON-NLS-1$
   public static final boolean           STATE_IS_MERGE_TRACKS_DEFAULT             = false;
   private static final String           STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED    = "STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED"; //$NON-NLS-1$
   public static final String            STATE_IS_SET_BODY_WEIGHT                  = "isSetBodyWeight";                        //$NON-NLS-1$
   public static final boolean           STATE_IS_SET_BODY_WEIGHT_DEFAULT          = true;
   //
   private static final String           HREF_TOKEN                                = "#";                                      //$NON-NLS-1$
   private static final String           PAGE_ABOUT_BLANK                          = "about:blank";                            //$NON-NLS-1$
   /**
    * This is necessary otherwise XULrunner in Linux do not fire a location change event.
    */
   private static final String           HTTP_DUMMY                                = "http://dummy";                           //$NON-NLS-1$
   private static final String           HTML_STYLE_TITLE_VERTICAL_PADDING         = "style='padding-top:10px;'";              //$NON-NLS-1$
   private static String                 ACTION_DEVICE_IMPORT                      = "DeviceImport";                           //$NON-NLS-1$
   private static String                 ACTION_DEVICE_WATCHING_ON_OFF             = "DeviceOnOff";                            //$NON-NLS-1$
   private static final String           ACTION_IMPORT_FROM_FILES                  = "ImportFromFiles";                        //$NON-NLS-1$
   private static final String           ACTION_OLD_UI                             = "OldUI";                                  //$NON-NLS-1$
   private static final String           ACTION_SERIAL_PORT_CONFIGURED             = "SerialPortConfigured";                   //$NON-NLS-1$
   private static final String           ACTION_SERIAL_PORT_DIRECTLY               = "SerialPortDirectly";                     //$NON-NLS-1$
   private static final String           ACTION_SETUP_EASY_IMPORT                  = "SetupEasyImport";                        //$NON-NLS-1$
   //
   private static final String           DOM_CLASS_DEVICE_ON                       = "deviceOn";                               //$NON-NLS-1$
   private static final String           DOM_CLASS_DEVICE_OFF                      = "deviceOff";                              //$NON-NLS-1$
   private static final String           DOM_CLASS_DEVICE_ON_ANIMATED              = "deviceOnAnimated";                       //$NON-NLS-1$
   private static final String           DOM_CLASS_DEVICE_OFF_ANIMATED             = "deviceOffAnimated";                      //$NON-NLS-1$
   //
   private static final String           DOM_ID_DEVICE_ON_OFF                      = "deviceOnOff";                            //$NON-NLS-1$
   private static final String           DOM_ID_DEVICE_STATE                       = "deviceState";                            //$NON-NLS-1$
   private static final String           DOM_ID_IMPORT_CONFIG                      = "importConfig";                           //$NON-NLS-1$
   private static final String           DOM_ID_IMPORT_TILES                       = "importTiles";                            //$NON-NLS-1$
   //
   private static String                 HREF_ACTION_DEVICE_IMPORT;
   private static String                 HREF_ACTION_DEVICE_WATCHING_ON_OFF;
   private static String                 HREF_ACTION_IMPORT_FROM_FILES;
   private static String                 HREF_ACTION_OLD_UI;
   private static String                 HREF_ACTION_SERIAL_PORT_CONFIGURED;
   private static String                 HREF_ACTION_SERIAL_PORT_DIRECTLY;
   private static String                 HREF_ACTION_SETUP_EASY_IMPORT;
   //
   private static final String           LOG_TOUR_DETAILS                          = "%s · %.0f s · %5.1f Δ %s";               //$NON-NLS-1$
   //
   static {
      //
// SET_FORMATTING_OFF

      HREF_ACTION_DEVICE_IMPORT           = HREF_TOKEN + ACTION_DEVICE_IMPORT;
      HREF_ACTION_DEVICE_WATCHING_ON_OFF  = HREF_TOKEN + ACTION_DEVICE_WATCHING_ON_OFF;
      HREF_ACTION_IMPORT_FROM_FILES       = HREF_TOKEN + ACTION_IMPORT_FROM_FILES;
      HREF_ACTION_OLD_UI                  = HREF_TOKEN + ACTION_OLD_UI;
      HREF_ACTION_SERIAL_PORT_CONFIGURED  = HREF_TOKEN + ACTION_SERIAL_PORT_CONFIGURED;
      HREF_ACTION_SERIAL_PORT_DIRECTLY    = HREF_TOKEN + ACTION_SERIAL_PORT_DIRECTLY;
      HREF_ACTION_SETUP_EASY_IMPORT       = HREF_TOKEN + ACTION_SETUP_EASY_IMPORT + HREF_TOKEN;

// SET_FORMATTING_ON
   }
   //
   private static boolean                      _isStopWatchingStoresThread;
   public static volatile ReentrantLock        THREAD_WATCHER_LOCK = new ReentrantLock();
   //
   private static ThreadPoolExecutor           _saveTour_Executor;
   private static ArrayBlockingQueue<TourData> _saveTour_Queue     = new ArrayBlockingQueue<>(Util.NUMBER_OF_PROCESSORS);
   private static CountDownLatch               _saveTour_CountDownLatch;

   static {

      final ThreadFactory threadFactory = runnable -> {

         final Thread thread = new Thread(runnable, "Saving imported tours");//$NON-NLS-1$

         thread.setPriority(Thread.MIN_PRIORITY);
         thread.setDaemon(true);

         return thread;
      };

      _saveTour_Executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Util.NUMBER_OF_PROCESSORS, threadFactory);
   }
   //
   private final IPreferenceStore           _prefStore                      = TourbookPlugin.getPrefStore();
   private final IPreferenceStore           _prefStore_Common               = CommonActivator.getPrefStore();
   private final IDialogSettings            _state                          = TourbookPlugin.getState(ID);
   private final IDialogSettings            _stateSimpleUI                  = TourbookPlugin.getState(ID + "_SimpleUI"); //$NON-NLS-1$
   //
   private RawDataManager                   _rawDataMgr                     = RawDataManager.getInstance();

   private TableViewer                      _tourViewer;
   private TableViewerTourInfoToolTip       _tourInfoToolTip;
   private ColumnManager                    _tourViewer_ColumnManager;
   private SelectionListener                _columnSortListener;
   private TableColumnDefinition            _timeZoneOffsetColDef;
   private TourViewer_Comparator            _tourViewer_Comparator;
   //
   private TableColumnDefinition            _colDef_TourTypeImage;
   private TableColumnDefinition            _colDef_WeatherClouds;
   private int                              _columnIndex_TourTypeImage      = -1;
   private int                              _columnIndex_WeatherClouds      = -1;
   private int                              _columnWidth_TourTypeImage;
   private int                              _columnWidth_WeatherClouds;
   //
   private String                           _columnId_DeviceName;
   private String                           _columnId_ImportFileName;
   private String                           _columnId_Marker;
   private String                           _columnId_TimeZone;
   private String                           _columnId_Title;
   private String                           _columnId_TourStartDate;
   //
   private PostSelectionProvider            _postSelectionProvider;
   private IPartListener2                   _partListener;
   private ISelectionListener               _postSelectionListener;
   private IPropertyChangeListener          _prefChangeListener;
   private IPropertyChangeListener          _prefChangeListener_Common;
   private ITourEventListener               _tourEventListener;
   //
   private TagMenuManager                   _tagMenuManager;
   private TourTypeMenuManager              _tourTypeMenuManager;
   private MenuManager                      _tourViewer_MenuManager;
   private IContextMenuProvider             _tourViewer_ContextMenuProvider = new TourViewer_ContextMenuProvider();
   //
   private HashMap<String, Object>          _allTourActions_Edit;
   private HashMap<String, Object>          _allTourActions_Export;
   //
   private ActionClearView                  _actionClearView;
   private ActionOpenTourLogView            _actionOpenTourLogView;
   private ActionDeleteTourFiles            _actionDeleteTourFile;
   private ActionExport                     _actionExportTour;
   private ActionEditQuick                  _actionEditQuick;
   private ActionEditTour                   _actionEditTour;
   private ActionJoinTours                  _actionJoinTours;
   private ActionMergeIntoMenu              _actionMergeIntoTour;
   private ActionMergeTour                  _actionMergeTour;
   private ActionOpenTour                   _actionOpenTour;
   private ActionOpenMarkerDialog           _actionOpenMarkerDialog;
   private ActionOpenAdjustAltitudeDialog   _actionOpenAdjustAltitudeDialog;
   private ActionOpenPrefDialog             _actionEditImportPreferences;
   private ActionReimportTours              _actionReimportTours;
   private ActionRemoveTour                 _actionRemoveTour;
   private ActionRemoveToursWhenClosed      _actionRemoveToursWhenClosed;
   private ActionSaveTourInDatabase         _actionSaveTour;
   private ActionSaveTourInDatabase         _actionSaveTourWithPerson;
   private ActionSetupImport                _actionSetupImport;
   private ActionSetTourTypeMenu            _actionSetTourType;
   private ActionSimpleUI_DeviceState       _actionSimpleUI_DeviceState;
   private ActionSimpleUI_StartEasyImport   _actionSimpleUI_StartEasyImport;
   private ActionSimpleUI_StartStopWatching _actionSimpleUI_StartStopWatching;
   private ActionToggleFossilOrEasyImport   _actionToggleImportUI;
   private ActionUpload                     _actionUploadTour;
   //
   private TourPerson                       _activePerson;
   private TourPerson                       _newActivePerson;
   //
   private boolean                          _isPartVisible                  = false;
   private boolean                          _isViewerPersonDataDirty        = false;
   //
   private final NumberFormat               _nf1;
   private final NumberFormat               _nf3;
   //
   private final PeriodType                 _durationTemplate;
   {
      _nf1 = NumberFormat.getNumberInstance();
      _nf3 = NumberFormat.getNumberInstance();

      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);

      _durationTemplate = PeriodType.yearMonthDayTime()

            // hide these components
            .withMillisRemoved();
   }
   //
   private boolean                       _isToolTipInDate;
   private boolean                       _isToolTipInTime;
   private boolean                       _isToolTipInTitle;
   private boolean                       _isToolTipInTags;
   //
   private TourDoubleClickState          _tourDoubleClickState       = new TourDoubleClickState();
   //
   private Thread                        _watchingStoresThread;
   private Thread                        _watchingFolderThread;
   private WatchService                  _folderWatcher;
   private AtomicBoolean                 _isWatchingStores           = new AtomicBoolean();
   private AtomicBoolean                 _isDeviceStateUpdateDelayed = new AtomicBoolean();
   private ReentrantLock                 WATCH_LOCK                  = new ReentrantLock();
   //
   private HashMap<String, Image>        _configImages               = new HashMap<>();
   private HashMap<String, Integer>      _configImageHash            = new HashMap<>();
   //
   private boolean                       _isBrowserCompleted;
   private boolean                       _isInFancyUIStartup;
   private boolean                       _isInUpdate;
   //
   private ImportUI                      _importUI;

   /**
    * When <code>false</code> then the background WatchStores task must set it valid. Only when it
    * is valid then the device state icon displays the state, otherwise it shows a waiting icon.
    */
   private boolean                       _isDeviceStateValid;
   private boolean                       _isRunDashboardAnimation    = true;
   private boolean                       _isShowWatcherAnimation;
   private boolean                       _isUpdateDeviceState        = true;
   //
   private String                        _cssFonts;
   private String                        _cssFromFile;
   //
   private String                        _imageUrl_Device_TurnOff;
   private String                        _imageUrl_Device_TurnOn;
   private String                        _imageUrl_DeviceFolder_OK;
   private String                        _imageUrl_DeviceFolder_Off;
   private String                        _imageUrl_DeviceFolder_Error;
   private String                        _imageUrl_DeviceFolder_IsChecking;
   private String                        _imageUrl_DeviceFolder_NotSetup;
   private String                        _imageUrl_ImportFromFile;
   private String                        _imageUrl_SerialPort_Configured;
   private String                        _imageUrl_SerialPort_Directly;
   private String                        _imageUrl_State_AdjustTemperature;
   private String                        _imageUrl_State_RetrieveTourLocation;
   private String                        _imageUrl_State_RetrieveWeatherData;
   private String                        _imageUrl_State_Error;
   private String                        _imageUrl_State_OK;
   private String                        _imageUrl_State_MovedFiles;
   private String                        _imageUrl_State_SaveTour;
   private String                        _imageUrl_State_TourMarker;
   private String                        _imageUrl_State_TourTags;
   //
   private PixelConverter                _pc;
   private List<TourbookCloudDownloader> _cloudDownloadersList       = CloudDownloaderManager.getCloudDownloaderList();
   //
   private DialogEasyImportConfig        _dialogImportConfig;

   /*
    * Simple easy import
    */
   private TableViewer                    _simpleUI_ImportLauncher_Viewer;
   private SimpleUI_ImportLauncher_Viewer _simpleUI_ImportLauncher_ColumnViewer        = new SimpleUI_ImportLauncher_Viewer();
   private ColumnManager                  _simpleUI_ImportLauncher_ColumnManager;
   private IContextMenuProvider           _simpleUI_ImportLauncher_ContextMenuProvider = new SimpleUI_ImportLauncher_ContextMenuProvider();
   private MenuManager                    _simpleUI_ImportLauncher_MenuManager;
   private EasyLauncherUtils              _simpleUI_ImportLauncher_Utils               = new EasyLauncherUtils();
   //
   private int                            _simpleUI_ColumnIndexConfigImage;
   private long                           _lastSimpleUIConfigSelection;
   //
   private OpenDialogManager              _openDialogManager                           = new OpenDialogManager();

   /*
    * Resources
    */
   private ImageRegistry _images;

   /*
    * UI controls
    */
   private Browser   _browser;

   private PageBook  _topPage_PageBook;
   private Composite _topPage_ImportViewer;
   private Composite _topPage_Startup;

   private Composite _topPage_ImportUI_EasyImport_Fancy;
   private Composite _topPage_ImportUI_EasyImport_Simple;
   private Composite _topPage_ImportUI_FossilUI;

   private PageBook  _easyImportFancy_PageBook;
   private Composite _easyImportFancy_Page_NoBrowser;
   private Composite _easyImportFancy_Page_WithBrowser;

   private Composite _parent;
   private Composite _tourViewer_Container;
   private Composite _simpleUI_ViewerContainer;

   private Combo     _comboSimpleUI_Config;

   private Label     _lblSimpleUI_EasyImportTitle;
   private Label     _lblSimpleUI_NumNotImportedFiles;

   private Link      _linkImport;

   private Text      _txtNoBrowser;

   private Menu      _tourViewer_ContextMenu;
   private Menu      _simpleUI_ImportLauncher_ContextMenu;

   private class ActionSimpleUI_DeviceState extends ActionToolbarSlideoutAdv {

      private SlideoutDeviceState __slideoutDeviceState;

      public ActionSimpleUI_DeviceState() {

         super(_images.get(IMAGE_DEVICE_FOLDER_OK),
               _images.get(IMAGE_DEVICE_FOLDER_OFF));
      }

      @Override
      protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

         __slideoutDeviceState = new SlideoutDeviceState(toolItem, _state, RawDataView.this);

         return __slideoutDeviceState;
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect(final SelectionEvent selectionEvent) {

         /*
          * Must be closed and not only hidden otherwise it can be blocking the opened dialog when
          * the slideout is set to keep opened
          */
         __slideoutDeviceState.close();

         onSelect_SetupEasyImport(-1);
      }
   }

   private class ActionSimpleUI_StartEasyImport extends Action {

      public ActionSimpleUI_StartEasyImport() {

         setText(Messages.Import_Data_Action_StartEasyImport);
      }

      @Override
      public void run() {

         runEasyImport();
      }
   }

   private class ActionSimpleUI_StartStopWatching extends Action {

      public ActionSimpleUI_StartStopWatching() {

         setToolTipText(Messages.Import_Data_HTML_DeviceOn_Tooltip);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.RawData_Device_TurnOn));
      }

      @Override
      public void run() {

         onSelect_DeviceWatching_OnOff();
      }
   }

   private class ActionToggleFossilOrEasyImport extends Action {

      public ActionToggleFossilOrEasyImport() {

         setToolTipText(Messages.Import_Data_Action_ImportUI_Tooltip);
         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Import_UI_Easy_Fancy));
      }

      @Override
      public void runWithEvent(final Event event) {

         actionToggle_ImportUI(event);
      }
   }

   private enum ImportUI {

      /**
       * Dashboard with the browser
       */
      EASY_IMPORT_FANCY,

      /**
       * Dashboard with SWT widget, to fix issues when the browser is sometimes not working
       * correctly
       */
      EASY_IMPORT_SIMPLE,

      /**
       * Just a simple import of files without easy import features
       */
      FOSSIL
   }

   private class JS_OnSelectImportConfig extends BrowserFunction {

      JS_OnSelectImportConfig(final Browser browser, final String name) {
         super(browser, name);
      }

      @Override
      public Object function(final Object[] arguments) {

         final int selectedIndex = ((Number) arguments[0]).intValue();

         _parent.getDisplay().asyncExec(() -> onSelect_ImportConfig_Fancy(selectedIndex));

//// this can be used to show created JS in the debugger
//         if (true) {
//            throw new RuntimeException();
//         }

         return null;
      }
   }

   private class SimpleUI_ImportLauncher_ContentProvider implements IStructuredContentProvider {

      public SimpleUI_ImportLauncher_ContentProvider() {}

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {

         final ArrayList<ImportLauncher> configItems = getEasyConfig().importLaunchers;

         return configItems.toArray(new ImportLauncher[configItems.size()]);
      }

      @Override
      public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
   }

   private class SimpleUI_ImportLauncher_ContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_simpleUI_ImportLauncher_ContextMenu != null) {
            _simpleUI_ImportLauncher_ContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _simpleUI_ImportLauncher_ContextMenu;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _simpleUI_ImportLauncher_ContextMenu = createUI_62_SimpleUI_CreateContextMenu();

         return _simpleUI_ImportLauncher_ContextMenu;
      }

   }

   public class SimpleUI_ImportLauncher_Viewer implements ITourViewer {

      @Override
      public ColumnManager getColumnManager() {
         return _simpleUI_ImportLauncher_ColumnManager;
      }

      @Override
      public ColumnViewer getViewer() {
         return _simpleUI_ImportLauncher_Viewer;
      }

      @Override
      public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

         _simpleUI_ViewerContainer.setRedraw(false);
         {
            final ISelection selection = _simpleUI_ImportLauncher_Viewer.getSelection();

            _simpleUI_ImportLauncher_Viewer.getTable().dispose();

            createUI_57_SimpleUI_ViewerTable(_simpleUI_ViewerContainer);
            _simpleUI_ViewerContainer.layout();

            // update viewer
            reloadViewer();

            _simpleUI_ImportLauncher_Viewer.setSelection(selection);
         }
         _simpleUI_ViewerContainer.setRedraw(true);

         return _simpleUI_ImportLauncher_Viewer;
      }

      @Override
      public void reloadViewer() {

         _simpleUI_ImportLauncher_Viewer.setInput(this);
      }

      @Override
      public void updateColumnHeader(final ColumnDefinition colDef) {}
   }

   private class SimpleUI_ImportLauncher_ViewerFilter extends ViewerFilter {

      @Override
      public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

         if (element instanceof ImportLauncher) {

            final ImportLauncher importLauncher = (ImportLauncher) element;

            if (importLauncher.isShowInDashboard) {
               return true;
            }
         }

         return false;
      }
   }

   private class TourViewer_Comparator extends ViewerComparator {

      static final int         ASCENDING  = 0;
      private static final int DESCENDING = 1;

      private String           __sortColumnId;
      private int              __sortDirection;

      @Override
      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

         final TourData tourData1 = ((TourData) obj1);
         final TourData tourData2 = ((TourData) obj2);

         int result = 0;

         if (__sortColumnId.equals(_columnId_TourStartDate)) {

            // date/time

            result = tourData1.getTourStartTimeMS() > tourData2.getTourStartTimeMS() ? 1 : -1;

         } else if (__sortColumnId.equals(_columnId_Title)) {

            // title

            result = tourData1.getTourTitle().compareTo(tourData2.getTourTitle());

         } else if (__sortColumnId.equals(_columnId_Marker)) {

            // marker

            final int numMarker1 = tourData1.getTourMarkers().size();
            final int numMarker2 = tourData2.getTourMarkers().size();
            final int numWayPoints1 = tourData1.getTourWayPoints().size();
            final int numWayPoints2 = tourData2.getTourWayPoints().size();

            final int num1 = numMarker1 + numWayPoints1;
            final int num2 = numMarker2 + numWayPoints2;

            if (num1 > 0 && num2 > 0) {

               result = num1 > num2 ? 1 : -1;

            } else {

               // prevent java.lang.IllegalArgumentException: Comparison method violates its general contract!

               if (num1 > 0) {

                  result = 1;

               } else if (num2 > 0) {

                  result = -1;
               }
            }

         } else if (__sortColumnId.equals(_columnId_ImportFileName)) {

            // file name

            final String importFilePath1 = tourData1.getImportFilePath();
            final String importFilePath2 = tourData2.getImportFilePath();

            if (importFilePath1 != null && importFilePath2 != null) {

               result = importFilePath1.compareTo(importFilePath2);
            }

         } else if (__sortColumnId.equals(_columnId_DeviceName)) {

            // device name

            result = tourData1.getDeviceName().compareTo(tourData2.getDeviceName());

         } else if (__sortColumnId.equals(_columnId_TimeZone)) {

            // time zone

            final String timeZoneId1 = tourData1.getTimeZoneId();
            final String timeZoneId2 = tourData2.getTimeZoneId();

            if (timeZoneId1 != null && timeZoneId2 != null) {

               final int zoneCompareResult = timeZoneId1.compareTo(timeZoneId2);

               result = zoneCompareResult;

            } else if (timeZoneId1 != null) {

               result = 1;

            } else if (timeZoneId2 != null) {

               result = -1;
            }
         }

         // do a 2nd sorting by date/time
         if (result == 0) {

            result = tourData1.getTourStartTimeMS() > tourData2.getTourStartTimeMS()
                  ? 1
                  : -1;
         }

         // if descending order, flip the direction
         if (__sortDirection == DESCENDING) {
            result = -result;
         }

         return result;
      }

      /**
       * Does the sort. If it's a different column from the previous sort, do an ascending sort. If
       * it's the same column as the last sort, toggle the sort direction.
       *
       * @param widget
       *           Column widget
       */
      private void setSortColumn(final Widget widget) {

         final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
         final String columnId = columnDefinition.getColumnId();

         if (columnId.equals(__sortColumnId)) {

            // Same column as last sort; toggle the direction

            __sortDirection = 1 - __sortDirection;

         } else {

            // New column; do an ascent sorting

            __sortColumnId = columnId;
            __sortDirection = ASCENDING;
         }

         updateUI_ShowSortDirection(__sortColumnId, __sortDirection);
      }

   }

   private class TourViewer_ContentProvider implements IStructuredContentProvider {

      public TourViewer_ContentProvider() {}

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {
         return (Object[]) (parent);
      }

      @Override
      public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
   }

   private class TourViewer_ContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_tourViewer_ContextMenu != null) {
            _tourViewer_ContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _tourViewer_ContextMenu;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _tourViewer_ContextMenu = createUI_96_CreateViewerContextMenu();

         return _tourViewer_ContextMenu;
      }

   }

   public static boolean isStopWatchingStoresThread() {
      return _isStopWatchingStoresThread;
   }

   void actionClearView() {

      // force the state icon to be updated
      _isDeviceStateValid = false;
      thread_FolderWatcher_Activate();

      // remove all tours
      _rawDataMgr.removeAllTours();

      reloadViewer();

      _postSelectionProvider.setSelection(new SelectionDeletedTours());

      // don't throw the selection again
      _postSelectionProvider.clearSelection();
   }

   /**
    * Delete selected tour files.
    */
   void actionDeleteTourFiles() {

      final ArrayList<TourData> selectedTours = getAnySelectedTours();

      runEasyImport_100_DeleteTourFiles(true, selectedTours, null, false);
   }

   void actionMergeTours(final TourData mergeFromTour, final TourData mergeIntoTour) {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      // backup data
      final Long backupMergeSourceTourId = mergeIntoTour.getMergeSourceTourId();
      final Long backupMergeTargetTourId = mergeIntoTour.getMergeTargetTourId();

      // set tour data and tour id from which the tour is merged
      mergeIntoTour.setMergeSourceTourId(mergeFromTour.getTourId());
      mergeIntoTour.setMergeTargetTourId(null);

      // set temp data, this is required by the dialog because the merge from tour could not be saved
      mergeIntoTour.setMergeSourceTour(mergeFromTour);

      if (new DialogMergeTours(Display.getCurrent().getActiveShell(), mergeFromTour, mergeIntoTour).open() != Window.OK) {

         // dialog is canceled, restore modified values

         mergeIntoTour.setMergeSourceTourId(backupMergeSourceTourId);
         mergeIntoTour.setMergeTargetTourId(backupMergeTargetTourId);
      }

      // reset temp tour data
      mergeIntoTour.setMergeSourceTour(null);
   }

   /**
    * Remove all tours from the raw data view which are selected
    */
   void actionRemoveTour() {

      final IStructuredSelection selection = ((IStructuredSelection) _tourViewer.getSelection());
      if (selection.isEmpty()) {
         return;
      }

      /*
       * convert selection to array
       */
      final Object[] selectedItems = selection.toArray();
      final TourData[] selectedTours = new TourData[selection.size()];
      for (int i = 0; i < selectedItems.length; i++) {
         selectedTours[i] = (TourData) selectedItems[i];
      }

      _rawDataMgr.removeTours(selectedTours);

      _postSelectionProvider.clearSelection();

      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, RawDataView.this);

      // update the table viewer
      reloadViewer();
   }

   void actionSaveTour(final TourPerson person) {

      TourLogManager.addLog(

            TourLogState.DEFAULT,

            Messages.Log_Tour_SaveTours,

            TourLogView.CSS_LOG_TITLE);

      saveImportedTours(getAnySelectedTours(), person);
   }

   /**
    * Toggle between different UI's
    *
    * @param event
    */
   private void actionToggle_ImportUI(final Event event) {

      final boolean isForward = UI.isCtrlKey(event) == false;

// SET_FORMATTING_OFF

      boolean isFancy  = false;
      boolean isSimple = false;
      boolean isFossil = false;

      switch (_importUI) {

      case EASY_IMPORT_FANCY:

         if (isForward) {     isSimple = true;  }
         else {               isFossil = true;  }

         break;

      case EASY_IMPORT_SIMPLE:

         if (isForward) {     isFossil = true;  }
         else {               isFancy  = true;  }

         break;

      case FOSSIL:
      default:

         if (isForward) {     isFancy  = true;  }
         else {               isSimple = true;  }

         break;
      }

      if (isFancy) {

         onSelect_ImportUI(ImportUI.EASY_IMPORT_FANCY);

      } else if (isSimple) {

         onSelect_ImportUI(ImportUI.EASY_IMPORT_SIMPLE);

      } else if (isFossil) {

         onSelect_ImportUI(ImportUI.FOSSIL);
      }

// SET_FORMATTING_ON
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == RawDataView.this) {

               // remove all tours
               _rawDataMgr.removeAllTours();

               TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, RawDataView.this);
            }
         }

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {
            if (RawDataView.this == partRef.getPart(false)) {
               _isPartVisible = false;
            }
         }

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {
            if (RawDataView.this == partRef.getPart(false)) {

               _isPartVisible = true;

               if (_isViewerPersonDataDirty || (_newActivePerson != _activePerson)) {

                  reloadViewer();
                  updateViewerPersonData();

                  _newActivePerson = _activePerson;
                  _isViewerPersonDataDirty = false;
               }
            }
         }
      };
      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {
            if (_isPartVisible) {
               updateViewerPersonData();
            } else {
               // keep new active person until the view is visible
               _newActivePerson = TourbookPlugin.getActivePerson();
            }

         } else if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

            _actionSaveTour.resetPeopleList();

         } else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            // tour type images can have been changed
            disposeConfigImages();

            // update tour type in the raw data
            _rawDataMgr.updateTourData_InImportView_FromDb(null);

            _tourViewer.refresh();

         } else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

            updateToolTipState();

         } else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _tourViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

            _tourViewer.refresh();

            /*
             * the tree must be redrawn because the styled text does not show with the new color
             */
            _tourViewer.getTable().redraw();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);

      /*
       * Common preferences
       */
      _prefChangeListener_Common = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.TIME_ZONE_LOCAL_ID)) {

            recreateViewer();

         } else if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

            // measurement system has changed

            recreateViewer();
         }
      };

      // register the listener
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   private void addSelectionListener() {

      _postSelectionListener = (workbenchPart, selection) -> {

         if (workbenchPart == RawDataView.this) {
            return;
         }

         onSelectionChanged(selection);
      };
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (part, eventId, eventData) -> {

         if (part == RawDataView.this) {
            return;
         }
         if (_isInUpdate) {
            return;
         }

         if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

            // update modified tours
            final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
            if (modifiedTours != null) {

               // update model
               _rawDataMgr.updateTourDataModel(modifiedTours);

               // update viewer
               _tourViewer.update(modifiedTours.toArray(), null);

               // fix refresh after a tour type is modified
               _tourViewer.getTable().redraw();

               // remove old selection, old selection can have the same tour but with old data
               _postSelectionProvider.clearSelection();
            }

         } else if (eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

            // save imported file names
            final ConcurrentHashMap<String, String> importedFiles = _rawDataMgr.getImportedFiles();

            _state.put(STATE_IMPORTED_FILENAMES, importedFiles.keySet().toArray(String[]::new));

            if (!RawDataManager.isReimportingActive() &&
                  !RawDataManager.isDeleteValuesActive()) {

               /*
                * Re-import files because computed values could be changed, e.g. elevation gain
                */

               reimportAllImportFiles(false);
            }

         } else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

            _rawDataMgr.updateTourData_InImportView_FromDb(null);

            reloadViewer();
         }
      };
      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   /**
    * Close all opened dialogs except the opening dialog.
    *
    * @param openingDialog
    */
   @Override
   public void closeOpenedDialogs(final IOpeningDialog openingDialog) {

      _openDialogManager.closeOpenedDialogs(openingDialog);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionClearView                    = new ActionClearView(this);
      _actionDeleteTourFile               = new ActionDeleteTourFiles(this);
      _actionEditTour                     = new ActionEditTour(this);
      _actionEditQuick                    = new ActionEditQuick(this);
      _actionExportTour                   = new ActionExport(this);
      _actionJoinTours                    = new ActionJoinTours(this);
      _actionMergeIntoTour                = new ActionMergeIntoMenu(this);
      _actionMergeTour                    = new ActionMergeTour(this);
      _actionOpenAdjustAltitudeDialog     = new ActionOpenAdjustAltitudeDialog(this);
      _actionOpenTourLogView              = new ActionOpenTourLogView();
      _actionOpenMarkerDialog             = new ActionOpenMarkerDialog(this, true);
      _actionOpenTour                     = new ActionOpenTour(this);
      _actionReimportTours                = new ActionReimportTours(this);
      _actionRemoveTour                   = new ActionRemoveTour(this);
      _actionRemoveToursWhenClosed        = new ActionRemoveToursWhenClosed();
      _actionSaveTour                     = new ActionSaveTourInDatabase(this, false);
      _actionSaveTourWithPerson           = new ActionSaveTourInDatabase(this, true);
      _actionSetupImport                  = new ActionSetupImport(this);
      _actionSetTourType                  = new ActionSetTourTypeMenu(this);
      _actionToggleImportUI               = new ActionToggleFossilOrEasyImport();
      _actionUploadTour                   = new ActionUpload(this);
      _actionSimpleUI_DeviceState         = new ActionSimpleUI_DeviceState();
      _actionSimpleUI_StartEasyImport     = new ActionSimpleUI_StartEasyImport();
      _actionSimpleUI_StartStopWatching   = new ActionSimpleUI_StartStopWatching();

      _allTourActions_Edit    = new HashMap<>();
      _allTourActions_Export  = new HashMap<>();

      _allTourActions_Edit.put(_actionEditQuick                   .getClass().getName(),  _actionEditQuick);
      _allTourActions_Edit.put(_actionEditTour                    .getClass().getName(),  _actionEditTour);
      _allTourActions_Edit.put(_actionOpenMarkerDialog            .getClass().getName(),  _actionOpenMarkerDialog);
      _allTourActions_Edit.put(_actionOpenAdjustAltitudeDialog    .getClass().getName(),  _actionOpenAdjustAltitudeDialog);
//    _allTourActions_Edit.put(_actionSetStartEndLocation         .getClass().getName(),  _actionSetStartEndLocation);
      _allTourActions_Edit.put(_actionOpenTour                    .getClass().getName(),  _actionOpenTour);
//    _allTourActions_Edit.put(_actionDuplicateTour               .getClass().getName(),  _actionDuplicateTour);
//    _allTourActions_Edit.put(_actionCreateTourMarkers           .getClass().getName(),  _actionCreateTourMarkers);
      _allTourActions_Edit.put(_actionMergeTour                   .getClass().getName(),  _actionMergeTour);
      _allTourActions_Edit.put(_actionJoinTours                   .getClass().getName(),  _actionJoinTours);

//      menuMgr.add(new Separator());
//      menuMgr.add(_actionEditQuick);
//      menuMgr.add(_actionEditTour);
//      menuMgr.add(_actionOpenMarkerDialog);
//      menuMgr.add(_actionOpenAdjustAltitudeDialog);
//      menuMgr.add(_actionOpenTour);
//      menuMgr.add(_actionMergeTour);
//      menuMgr.add(_actionJoinTours);


      _allTourActions_Export.put(_actionUploadTour                .getClass().getName(),  _actionUploadTour);
      _allTourActions_Export.put(_actionExportTour                .getClass().getName(),  _actionExportTour);
//    _allTourActions_Export.put(_actionExportViewCSV             .getClass().getName(),  _actionExportViewCSV);
//    _allTourActions_Export.put(_actionPrintTour                 .getClass().getName(),  _actionPrintTour);

//      menuMgr.add(_actionUploadTour);
//      menuMgr.add(_actionExportTour);

//    _allTourActions_Adjust.put(_actionAdjustTourValues          .getClass().getName(),  _actionAdjustTourValues);
//    _allTourActions_Adjust.put(_actionDeleteTourValues          .getClass().getName(),  _actionDeleteTourValues);
//    _allTourActions_Adjust.put(_actionReimport_Tours            .getClass().getName(),  _actionReimport_Tours);
//    _allTourActions_Adjust.put(_actionSetOtherPerson            .getClass().getName(),  _actionSetOtherPerson);
//    _allTourActions_Adjust.put(_actionDeleteTourMenu            .getClass().getName(),  _actionDeleteTourMenu);

// SET_FORMATTING_ON

      TourActionManager.setAllViewActions(ID,
            _allTourActions_Edit.keySet(),
            _allTourActions_Export.keySet(),
            _tagMenuManager.getAllTagActions().keySet(),
            _tourTypeMenuManager.getAllTourTypeActions().keySet());

      _actionEditImportPreferences = new ActionOpenPrefDialog(
            Messages.Import_Data_Action_EditImportPreferences,
            PrefPageImport.ID,
            // set custom data
            ID);
   }

   /**
    * Create css from the import configurations.
    *
    * @return
    */
   private String createCSS_Custom() {

      /*
       * Import background image
       */
      final EasyConfig easyConfig = getEasyConfig();

      final int itemSize = easyConfig.tileSize;
      final int opacity = easyConfig.backgroundOpacity;
      final int animationDuration = easyConfig.animationDuration;
      final int crazyFactor = easyConfig.animationCrazinessFactor;

      String bgImage = UI.EMPTY_STRING;
      if (opacity > 0) {

         /*
          * Show image only when it is visible, opacity > 0
          */

         final File webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_BG_IMAGE);
         final String webContent = Util.readContentFromFile(webFile.getAbsolutePath());
         final String base64Encoded = Base64.getEncoder().encodeToString(webContent.getBytes());

         bgImage = CSS_IMPORT_BACKGROUND + NL

               + "{" + NL //                                                           //$NON-NLS-1$
               + "   background:             url('data:image/svg+xml;base64," + base64Encoded + "');" + NL //$NON-NLS-1$ //$NON-NLS-2$
               + "   background-repeat:      no-repeat;" + NL //                       //$NON-NLS-1$
               + "   background-size:        contain;" + NL //                         //$NON-NLS-1$
               + "   background-position:    center center;" + NL //                   //$NON-NLS-1$
               + "   opacity:                " + (float) opacity / 100 + ";" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
               + "}" + NL; //                                                          //$NON-NLS-1$
      }

      String animation = UI.EMPTY_STRING;
      if (_isRunDashboardAnimation && animationDuration > 0 && UI.IS_WIN) {

         // run animation only once
         _isRunDashboardAnimation = false;

         final double rotateX = Math.random() * 10 * crazyFactor;
         final double rotateY = Math.random() * 10 * crazyFactor;

         animation = UI.EMPTY_STRING//

               + "body" + NL //                                         //$NON-NLS-1$
               + "{" + NL //                                            //$NON-NLS-1$
               + "   animation:                    fadeinBody;" + NL // //$NON-NLS-1$
               + "   animation-duration:           " + _nf1.format(animationDuration / 10.0) + "s;" + NL // //$NON-NLS-1$ //$NON-NLS-2$
               + "   animation-timing-function:    ease;" + NL //       //$NON-NLS-1$
               + "}" + NL //                                            //$NON-NLS-1$

               + "@keyframes fadeinBody" + NL //                        //$NON-NLS-1$
               + "{" + NL //                                            //$NON-NLS-1$

               + "   from" + NL //                                      //$NON-NLS-1$
               + "   {" + NL //                                         //$NON-NLS-1$
               + "      opacity:             0;" + NL //                //$NON-NLS-1$
               + "      background-color:    ButtonFace;" + NL //       //$NON-NLS-1$
               + "      transform:           rotateX(" + (int) rotateX + "deg) rotateY(" + (int) rotateY + "deg);   " + NL // //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
               + "   }" + NL //                                         //$NON-NLS-1$

               + "   to" + NL //                                        //$NON-NLS-1$
               + "   {" + NL //                                         //$NON-NLS-1$
               + "      opacity:             1;" + NL //                //$NON-NLS-1$
               + "   }" + NL //                                         //$NON-NLS-1$
               + "}" + NL; //                                           //$NON-NLS-1$
      }

      /*
       * Tile size
       */
      final String tileSize = UI.EMPTY_STRING
            //
            + CSS_IMPORT_TILE + NL

            + "{" + NL //                                      //$NON-NLS-1$
            + "   min-height: " + itemSize + "px;" + NL //     //$NON-NLS-1$ //$NON-NLS-2$
            + "   max-height: " + itemSize + "px;" + NL //     //$NON-NLS-1$ //$NON-NLS-2$
            + "   min-width:  " + itemSize + "px;" + NL //     //$NON-NLS-1$ //$NON-NLS-2$
            + "   max-width:  " + itemSize + "px;" + NL //     //$NON-NLS-1$ //$NON-NLS-2$
            + "}" + NL; //                                     //$NON-NLS-1$

      /*
       * State tooltip
       */
      final String stateTooltip = UI.EMPTY_STRING

            + ".stateTooltip" + NL //                          //$NON-NLS-1$
            + "{" + NL //                                      //$NON-NLS-1$
            + "   width:" + easyConfig.stateToolTipWidth + "px;" + NL // //$NON-NLS-1$ //$NON-NLS-2$
            + "}" + NL; //                                     //$NON-NLS-1$

      /*
       * CSS
       */
      final String customCSS = UI.EMPTY_STRING

            + "<style>" + NL //     //$NON-NLS-1$
            + WEB.createCSS_Scrollbar()
            + animation
            + bgImage
            + tileSize
            + stateTooltip
            + "</style>" + NL; //   //$NON-NLS-1$

      return customCSS;
   }

   private String createHTML() {

//      Force Internet Explorer to not use compatibility mode. Internet Explorer believes that websites under
//      several domains (including "ibm.com") require compatibility mode. You may see your web application run
//      normally under "localhost", but then fail when hosted under another domain (e.g.: "ibm.com").
//      Setting "IE=Edge" will force the latest standards mode for the version of Internet Explorer being used.
//      This is supported for Internet Explorer 8 and later. You can also ease your testing efforts by forcing
//      specific versions of Internet Explorer to render using the standards mode of previous versions. This
//      prevents you from exploiting the latest features, but may offer you compatibility and stability. Lookup
//      the online documentation for the "X-UA-Compatible" META tag to find which value is right for you.

      final String html = UI.EMPTY_STRING

            + "<!DOCTYPE html>" + NL //   // ensure that IE is using the newest version and not the quirk mode //$NON-NLS-1$
            + "<html style='height: 100%; width: 100%; margin: 0px; padding: 0px;'>" + NL //    //$NON-NLS-1$
            + "<head>" + NL + createHTML_10_Head() + NL + "</head>" + NL //                     //$NON-NLS-1$ //$NON-NLS-2$
            + "<body>" + NL + createHTML_20_Body() + NL + "</body>" + NL //                     //$NON-NLS-1$ //$NON-NLS-2$
            + "</html>"; //                                                                     //$NON-NLS-1$

      return html;
   }

   private String createHTML_10_Head() {

      final String html = UI.EMPTY_STRING

            + "   <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />" + NL //   //$NON-NLS-1$
            + "   <meta http-equiv='X-UA-Compatible' content='IE=edge' />" + NL //                 //$NON-NLS-1$
            + _cssFonts
            + _cssFromFile
            + createCSS_Custom()
            + "" + NL; // //$NON-NLS-1$

      return html;
   }

   private String createHTML_20_Body() {

      final EasyConfig easyConfig = getEasyConfig();

      final boolean isShowSimpleImport = easyConfig.isShowTile_CloudApps
            || easyConfig.isShowTile_Files
            || easyConfig.isShowTile_FossilUI
            || easyConfig.isShowTile_SerialPort
            || easyConfig.isShowTile_SerialPortWithConfig;

      final StringBuilder sb = new StringBuilder();

      sb.append("<div class='import-container'>" + NL); //$NON-NLS-1$
      {
         /*
          * Very tricky: When a parent has an opacity, a child cannot modify it. Therefore the
          * different divs with position relative/absolute. It took me some time to find/implement
          * this tricky but simple solution.
          */
         sb.append("<div class='import-background'></div>" + NL); //$NON-NLS-1$

         sb.append("<div class='import-content'>" + NL); //$NON-NLS-1$
         {
            /*
             * Easy Import
             */
            createHTML_50_Easy_Header(sb);
            createHTML_80_Easy_Tiles(sb);

            /*
             * Simple Import
             */
            if (isShowSimpleImport) {

               sb.append("<div class='get-tours-title title'>" + NL); //$NON-NLS-1$
               sb.append(UI.SPACE3 + Messages.Import_Data_HTML_GetTours + NL);
               sb.append("</div>" + NL); //$NON-NLS-1$

               createHTML_90_SimpleImport(sb, easyConfig);
            }
         }
         sb.append("</div>" + NL); //$NON-NLS-1$
      }
      sb.append("</div>" + NL); //$NON-NLS-1$

      return sb.toString();
   }

   private void createHTML_50_Easy_Header(final StringBuilder sb) {

      final String watchClass = isWatchingOn() ? DOM_CLASS_DEVICE_ON : DOM_CLASS_DEVICE_OFF;

      final String html = UI.EMPTY_STRING

            + "<div class='auto-import-header'>" + NL //          //$NON-NLS-1$
            + "   <table border=0><tbody><tr>" + NL //            //$NON-NLS-1$

            // device on/off
            + "      <td>" + NL //                                //$NON-NLS-1$
            + "         <div id='" + DOM_ID_DEVICE_ON_OFF + "'>" + createHTML_52_Device_OnOff() + "</div>" + NL // //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "      </td>" + NL //                               //$NON-NLS-1$

            // title
            + "      <td><span class='title'>" + Messages.Import_Data_HTML_EasyImport + "</span></td>" + NL // //$NON-NLS-1$ //$NON-NLS-2$

            // device folder
            + "      <td>" + NL //                                //$NON-NLS-1$
            + "         <div id='" + DOM_ID_DEVICE_STATE + "' style='padding-left:25px;' class='" + watchClass + "'>" + NL // //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + createHTML_54_DeviceFolder()
            + "         </div>" + NL //                           //$NON-NLS-1$
            + "      </td>" + NL //                               //$NON-NLS-1$

            // selected config
            + "      <td>" //$NON-NLS-1$
            + "         <div id='" + DOM_ID_IMPORT_CONFIG + "' class='" + watchClass + "'>" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + createHTML_60_SelectImportConfig()
            + "         </div>" //                                //$NON-NLS-1$
            + "      </td>" //                                    //$NON-NLS-1$

            + "   </tr></tbody></table>" + NL //                  //$NON-NLS-1$
            + "</div>" + NL; //                                   //$NON-NLS-1$

      sb.append(html);
   }

   private String createHTML_52_Device_OnOff() {

      final boolean isWatchingOn = isWatchingOn();

      String tooltip = isWatchingOn
            ? Messages.Import_Data_HTML_DeviceOff_Tooltip
            : Messages.Import_Data_HTML_DeviceOn_Tooltip;

      tooltip = UI.replaceHTML_NewLine(tooltip);

      // show red image when off
      final String imageUrl = isWatchingOn
            ? _imageUrl_Device_TurnOn
            : _imageUrl_Device_TurnOff;

      final String hrefAction = HTTP_DUMMY + HREF_ACTION_DEVICE_WATCHING_ON_OFF;
      final String onOffImage = createHTML_BgImageStyle(imageUrl);

      final String html = UI.EMPTY_STRING

            + "<a class='onOffIcon dash-action'" //                  //$NON-NLS-1$
            + "title='" + tooltip + "'" //                           //$NON-NLS-1$ //$NON-NLS-2$
            + " href='" + hrefAction + "'" //                        //$NON-NLS-1$ //$NON-NLS-2$
            + ">" //                                                 //$NON-NLS-1$

            + "<div class='stateIcon' " + onOffImage + "></div>" //  //$NON-NLS-1$ //$NON-NLS-2$

            + "</a>"; //                                             //$NON-NLS-1$

      return html;
   }

   private String createHTML_54_DeviceFolder() {

      final String hrefSetupAction = HTTP_DUMMY + HREF_ACTION_SETUP_EASY_IMPORT;

      final EasyConfig easyConfig = getEasyConfig();
      final ImportConfig importConfig = easyConfig.getActiveImportConfig();

      String html = null;

      final boolean isWatchAnything = importConfig.isWatchAnything();

      if (isWatchingOn() == false) {

         // watching is off

         final String stateImage = createHTML_BgImageStyle(_imageUrl_DeviceFolder_Off);
         final String htmlTooltip = Messages.Import_Data_HTML_WatchingIsOff;

         html = "" + NL //                                                 //$NON-NLS-1$

               + "<a class='importState dash-action'" //                   //$NON-NLS-1$
               + " href='" + HTTP_DUMMY + "'" //                           //$NON-NLS-1$ //$NON-NLS-2$
               + ">" //                                                    //$NON-NLS-1$

               + "<div class='stateIcon' " + stateImage + ">" + NL //      //$NON-NLS-1$ //$NON-NLS-2$
               + "   <div class='stateIconValue'></div>" + NL //           //$NON-NLS-1$
               + "</div>" + NL //                                          //$NON-NLS-1$
               + "<div class='stateTooltip stateTooltipMessage'>" + htmlTooltip + "</div>" + NL // //$NON-NLS-1$ //$NON-NLS-2$

               + "</a>" + NL; //                                           //$NON-NLS-1$

      } else if (isWatchAnything && _isDeviceStateValid) {

         html = createHTML_55_DeviceFolder_IsValid(easyConfig, hrefSetupAction);

      } else {

         /*
          * On startup, set the folder state without device info because this is retrieved in a
          * background thread, if not, it is blocking the UI !!!
          */

         final String stateImage = createHTML_BgImageStyle(isWatchAnything
               ? _imageUrl_DeviceFolder_IsChecking
               : _imageUrl_DeviceFolder_NotSetup);

         final String htmlTooltip = isWatchAnything
               ? Messages.Import_Data_HTML_AcquireDeviceInfo
               : Messages.Import_Data_HTML_NothingIsWatched;

         html = "" + NL //                                                 //$NON-NLS-1$

               + "<a class='importState dash-action'" //                   //$NON-NLS-1$
               + " href='" + hrefSetupAction + "'" //                      //$NON-NLS-1$ //$NON-NLS-2$
               + ">" + NL //                                               //$NON-NLS-1$

               + "<div class='stateIcon' " + stateImage + ">" + NL //      //$NON-NLS-1$ //$NON-NLS-2$
               + "   <div class='stateIconValue'></div>" + NL //           //$NON-NLS-1$
               + "</div>" + NL //                                          //$NON-NLS-1$
               + "<div class='stateTooltip stateTooltipMessage'>" + htmlTooltip + "</div>" + NL // //$NON-NLS-1$ //$NON-NLS-2$

               + "</a>" + NL; //                                           //$NON-NLS-1$
      }

      return html;
   }

   private String createHTML_55_DeviceFolder_IsValid(final EasyConfig easyConfig, final String hrefAction) {

      final ImportConfig importConfig = easyConfig.getActiveImportConfig();

      final String deviceOSFolder = importConfig.getDeviceOSFolder();
      final ArrayList<OSFile> notImportedFiles = easyConfig.notImportedFiles;

      final int numDeviceFiles = easyConfig.numDeviceFiles;
      final int numMovedFiles = easyConfig.movedFiles.size();
      final int numNotImportedFiles = notImportedFiles.size();
      final int numAllFiles = numDeviceFiles + numMovedFiles;

      final boolean isDeviceFolderOK = isOSFolderValid(deviceOSFolder);
      boolean isFolderOK = true;

      final StringBuilder sb = new StringBuilder();
      sb.append("<table border=0><tbody>"); //$NON-NLS-1$

      /*
       * Title
       */
      sb.append(HTML_TR);
      sb.append("<td colspan=2 class='importConfigTitle'>" + importConfig.name + HTML_TD_END); //$NON-NLS-1$
      sb.append(HTML_TR_END);

      /*
       * Backup folder
       */
      final boolean isCreateBackup = importConfig.isCreateBackup;
      if (isCreateBackup) {

         final String htmlBackupFolder = UI.replaceHTML_BackSlash(importConfig.getBackupFolder());

         // check OS folder
         final String backupOSFolder = importConfig.getBackupOSFolder();
         final boolean isBackupFolderOK = isOSFolderValid(backupOSFolder);
         isFolderOK &= isBackupFolderOK;

         final String folderTitle = Messages.Import_Data_HTML_Title_Backup;
         String folderInfo = null;

         /*
          * Show back folder info only when device folder is OK because they are related together.
          */
         if (isDeviceFolderOK) {

            final int numNotBackedUpFiles = easyConfig.notBackedUpFiles.size();

            folderInfo = numNotBackedUpFiles == 0
                  ? NLS.bind(Messages.Import_Data_HTML_AllFilesAreBackedUp, numDeviceFiles)
                  : NLS.bind(Messages.Import_Data_HTML_NotBackedUpFiles, numNotBackedUpFiles, numDeviceFiles);

         }

         createHTML_56_FolderState(
               sb,
               htmlBackupFolder,
               isBackupFolderOK,
               false,
               folderTitle,
               folderInfo);
      }

      /*
       * Device folder
       */
      {
         final String htmlDeviceFolder = UI.replaceHTML_BackSlash(importConfig.getDeviceFolder());

         final boolean isTopMargin = importConfig.isCreateBackup;

         final String folderTitle = Messages.Import_Data_HTML_Title_Device;
         final String folderInfo = numNotImportedFiles == 0
               ? NLS.bind(Messages.Import_Data_HTML_AllFilesAreImported, numAllFiles)
               : NLS.bind(Messages.Import_Data_HTML_NotImportedFiles, numNotImportedFiles, numAllFiles);

         createHTML_56_FolderState(
               sb,
               htmlDeviceFolder,
               isDeviceFolderOK,
               isTopMargin,
               folderTitle,
               folderInfo);

         isFolderOK &= isDeviceFolderOK;
      }

      /*
       * Moved files
       */
      if (numMovedFiles > 0) {

         sb.append(HTML_TR);

         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderTitle'>"); //$NON-NLS-1$
         sb.append(Messages.Import_Data_HTML_Title_Moved);
         sb.append(HTML_TD_END);

         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderLocation'>"); //$NON-NLS-1$
         sb.append(NLS.bind(Messages.Import_Data_HTML_MovedFiles, numMovedFiles));
         sb.append(HTML_TD_END);

         sb.append(HTML_TR_END);
      }

      /*
       * Device files
       */
      {
         final String deviceFiles = importConfig.fileGlobPattern.trim().length() == 0
               ? ImportConfig.DEVICE_FILES_DEFAULT
               : importConfig.fileGlobPattern;

         sb.append(HTML_TR);

         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderTitle'>"); //$NON-NLS-1$
         sb.append(Messages.Import_Data_HTML_Title_Files);
         sb.append(HTML_TD_END);

         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderLocation'>"); //$NON-NLS-1$
         sb.append(deviceFiles);
         sb.append(HTML_TD_END);

         sb.append(HTML_TR_END);
      }

      /*
       * 100. Delete device files
       */
      {
         final boolean isDeleteDeviceFiles = importConfig.isDeleteDeviceFiles;

         final String deleteFiles = isDeleteDeviceFiles
               ? Messages.Import_Data_HTML_DeleteFilesYES
               : Messages.Import_Data_HTML_DeleteFilesNO;

         sb.append(HTML_TR);

         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderTitle'>"); //$NON-NLS-1$
         sb.append(Messages.Import_Data_HTML_Title_Delete);
         sb.append(HTML_TD_END);

         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderLocation'>"); //$NON-NLS-1$
         sb.append(deleteFiles);
         sb.append(HTML_TD_END);

         sb.append(HTML_TR_END);
      }

      /*
       * 101. Turn off device watching
       */
      {
         final boolean isWatchingOff = importConfig.isTurnOffWatching;

         final String watchingText = isWatchingOff
               ? Messages.Import_Data_HTML_WatchingOff
               : Messages.Import_Data_HTML_WatchingOn;

         // show red image when off
         final String imageUrl = isWatchingOff
               ? _imageUrl_Device_TurnOff
               : _imageUrl_Device_TurnOn;

         final String onOffImage = createHTML_BgImage(imageUrl);

         sb.append(HTML_TR);

         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + ">"); //$NON-NLS-1$
         sb.append("   <div class='action-button-25' style='" + onOffImage + "'></div>"); //$NON-NLS-1$ //$NON-NLS-2$
         sb.append(HTML_TD_END);
         sb.append("<td class='folderInfo' " + HTML_STYLE_TITLE_VERTICAL_PADDING + ">" + watchingText + HTML_TD_END); //$NON-NLS-1$ //$NON-NLS-2$

         sb.append(HTML_TR_END);
      }

      sb.append("</tbody></table>"); //$NON-NLS-1$

      /*
       * Import files
       */
      if (numNotImportedFiles > 0) {
         createHTML_58_NotImportedFiles(sb, notImportedFiles);
      }

      final String htmlTooltip = sb.toString();

      /*
       * State values
       */
      final String imageUrl = isFolderOK
            ? _imageUrl_DeviceFolder_OK
            : _imageUrl_DeviceFolder_Error;

      final String stateImage = createHTML_BgImageStyle(imageUrl);
      final String stateIconValue = isDeviceFolderOK ? Integer.toString(numNotImportedFiles) : UI.EMPTY_STRING;

      /*
       * Show overflow scrollbar ONLY when more than 10 entries are available because it looks ugly.
       */
      final String cssOverflow = numNotImportedFiles > 10 //
            ? "style='overflow-y: scroll;'" //$NON-NLS-1$
            : UI.EMPTY_STRING;

      final String html = "" + NL //                                       //$NON-NLS-1$

            + "<a class='importState dash-action'" //                      //$NON-NLS-1$
            + " href='" + hrefAction + "'" //                              //$NON-NLS-1$ //$NON-NLS-2$
            + ">" //                                                       //$NON-NLS-1$

            + "   <div class='stateIcon' " + stateImage + ">" + NL //      //$NON-NLS-1$ //$NON-NLS-2$
            + "      <div class='stateIconValue'>" + stateIconValue + "</div>" + NL // //$NON-NLS-1$ //$NON-NLS-2$
            + "   </div>" + NL //                                          //$NON-NLS-1$
            + "   <div class='stateTooltip' " + cssOverflow + ">" + htmlTooltip + "</div>" + NL // //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            + "</a>" + NL; //                                              //$NON-NLS-1$

      return html;
   }

   /**
    * @param sb
    * @param folderLocation
    * @param isOSFolderValid
    * @param isTopMargin
    * @param folderTitle
    * @param folderInfo
    */
   private void createHTML_56_FolderState(final StringBuilder sb,
                                          final String folderLocation,
                                          final boolean isOSFolderValid,
                                          final boolean isTopMargin,
                                          final String folderTitle,
                                          final String folderInfo) {

      String htmlErrorState;
      String htmlFolderInfo;

      if (isOSFolderValid) {

         htmlErrorState = UI.EMPTY_STRING;
         htmlFolderInfo = folderInfo == null
               ? UI.EMPTY_STRING
               : "<span class='folderInfo'>" + folderInfo + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$

      } else {

         htmlErrorState = "<div class='folderError'>" + Messages.Import_Data_HTML_FolderIsNotAvailable + "</div>"; //$NON-NLS-1$ //$NON-NLS-2$
         htmlFolderInfo = UI.EMPTY_STRING;
      }

      final String paddingTop = isTopMargin
            ? HTML_STYLE_TITLE_VERTICAL_PADDING
            : UI.EMPTY_STRING;

      final String imageUrl = isOSFolderValid
            ? _imageUrl_State_OK
            : _imageUrl_State_Error;

      final String folderStateIcon = "<img src='" //$NON-NLS-1$
            + imageUrl
            + "' style='padding-left:5px; vertical-align:text-bottom;'>"; //$NON-NLS-1$

      sb.append(HTML_TR);
      sb.append(HTML_TD_SPACE + paddingTop + " class='folderTitle'>" + folderTitle + HTML_TD_END); //$NON-NLS-1$
      sb.append(HTML_TD_SPACE + paddingTop + " class='folderLocation'>" + folderLocation + folderStateIcon); //$NON-NLS-1$
      sb.append(htmlErrorState);
      sb.append(HTML_TD_END);
      sb.append(HTML_TR_END);

      sb.append(HTML_TR);
      sb.append("<td></td>"); //$NON-NLS-1$
      sb.append(HTML_TD + htmlFolderInfo + HTML_TD_END);
      sb.append(HTML_TR_END);

   }

   private void createHTML_58_NotImportedFiles(final StringBuilder sb, final ArrayList<OSFile> notImportedFiles) {

      sb.append("<table border=0 class='deviceList'><tbody>"); //$NON-NLS-1$

      final EasyConfig easyConfig = getEasyConfig();

      for (final OSFile deviceFile : notImportedFiles) {

         final String fileMoveState = deviceFile.isBackupImportFile
               ? Messages.Import_Data_HTML_Title_Moved_State
               : UI.EMPTY_STRING;

         String filePathName = UI.replaceHTML_BackSlash(deviceFile.getPath().getParent().toString());
         final ZonedDateTime modifiedTime = TimeTools.getZonedDateTime(deviceFile.modifiedTime);

         // am/pm contains a space which can break the line
         final String nbspTime = modifiedTime.format(TimeTools.Formatter_Time_S).replace(UI.SPACE1, WEB.NONE_BREAKING_SPACE);
         final String nbspFileName = deviceFile.getFileName().replace(UI.SPACE1, WEB.NONE_BREAKING_SPACE);

         sb.append(HTML_TR);

         sb.append("<td width=1 class='column'>"); //$NON-NLS-1$
         sb.append(fileMoveState);
         sb.append(HTML_TD_END);

         sb.append("<td class='column content'>"); //$NON-NLS-1$
         sb.append(nbspFileName);
         sb.append(HTML_TD_END);

         sb.append("<td class='column right'>"); //$NON-NLS-1$
         sb.append(modifiedTime.format(TimeTools.Formatter_Date_S));
         sb.append(HTML_TD_END);

         sb.append("<td class='column right'>"); //$NON-NLS-1$
         sb.append(nbspTime);
         sb.append(HTML_TD_END);

         sb.append("<td class='right'>"); //$NON-NLS-1$
         sb.append(deviceFile.size);
         sb.append(HTML_TD_END);

         // this is useful for debugging
         if (easyConfig.stateToolTipDisplayAbsoluteFilePath) {

            if (NIO.isTourBookFileSystem(filePathName)) {

               final TourbookFileSystem tourbookFileSystem = FileSystemManager.getTourbookFileSystem(filePathName);

               filePathName = filePathName.replace(tourbookFileSystem.getId(), tourbookFileSystem.getDisplayId());
            }

            final String nbspFilePathName = UI.EMPTY_STRING

                  // add additonal space before the text otherwise it is too narrow to the previous column
                  + WEB.NONE_BREAKING_SPACE
                  + WEB.NONE_BREAKING_SPACE

                  + filePathName.replace(UI.SPACE1, WEB.NONE_BREAKING_SPACE);

            sb.append("<td class='column content'>"); //$NON-NLS-1$
            sb.append(nbspFilePathName);
            sb.append(HTML_TD_END);
         }

         sb.append(HTML_TR_END);
      }

      sb.append("</tbody></table>"); //$NON-NLS-1$
   }

   private String createHTML_60_SelectImportConfig() {

      final String onChange = "onchange='" + JS_FUNCTION_ON_SELECT_IMPORT_CONFIG + "(this.selectedIndex)'"; //$NON-NLS-1$ //$NON-NLS-2$

      final EasyConfig easyConfig = getEasyConfig();
      final ImportConfig selectedConfig = easyConfig.getActiveImportConfig();

      final StringBuilder sb = new StringBuilder();

      sb.append("<div class='selectConfigContainer'>"); //$NON-NLS-1$
      sb.append("   <select class='selectConfig dash-action' " + onChange + ">"); //$NON-NLS-1$ //$NON-NLS-2$

      for (final ImportConfig importConfig : easyConfig.importConfigs) {

         final String isSelected = importConfig.equals(selectedConfig)
               ? "selected" //$NON-NLS-1$
               : UI.EMPTY_STRING;

         sb.append("      <option class='selectOption' " + isSelected + ">" + importConfig.name + "</option>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
      sb.append("   </select>"); //$NON-NLS-1$
      sb.append("</div>"); //$NON-NLS-1$

      return sb.toString();
   }

   private void createHTML_80_Easy_Tiles(final StringBuilder sb) {

      final EasyConfig easyConfig = getEasyConfig();

      final ArrayList<ImportLauncher> allImportLauncher = easyConfig.importLaunchers;
      final ArrayList<ImportConfig> allImportConfigs = easyConfig.importConfigs;

      if (allImportLauncher.isEmpty() || allImportConfigs.isEmpty()) {

         // this case should not happen
         TourLogManager.log_EXCEPTION_WithStacktrace(new Exception("Import config/launcher are not setup correctly."));//$NON-NLS-1$

         return;
      }

      // get available launcher
      int availableLauncher = 0;
      for (final ImportLauncher importLauncher : allImportLauncher) {
         if (importLauncher.isShowInDashboard) {
            availableLauncher++;
         }
      }

      int tileIndex = 0;
      int numHorizontalTiles = easyConfig.numHorizontalTiles;
      boolean isTrOpen = false;

      // reduce max tiles, otherwise there would be a BIG gap between tiles
      if (availableLauncher < numHorizontalTiles) {
         numHorizontalTiles = availableLauncher;
      }

      final String watchClass = isWatchingOn()
            ? DOM_CLASS_DEVICE_ON
            : DOM_CLASS_DEVICE_OFF;

      sb.append("<table border=0" //$NON-NLS-1$
            + " id='" + DOM_ID_IMPORT_TILES + "'" //$NON-NLS-1$ //$NON-NLS-2$
            + " style='margin-top:5px;'" //$NON-NLS-1$
            + " class='" + watchClass + "'" //$NON-NLS-1$ //$NON-NLS-2$
            + "><tbody>" + NL); //$NON-NLS-1$

      for (final ImportLauncher importLauncher : allImportLauncher) {

         if (importLauncher.isShowInDashboard) {

            if (tileIndex % numHorizontalTiles == 0) {
               sb.append("<tr>" + NL); //$NON-NLS-1$
               isTrOpen = true;
            }

            // enforce equal column width
            sb.append("<td style='width:" + 100 / numHorizontalTiles + "%' class='import-tile'>" + NL); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(createHTML_82_Easy_Tile(importLauncher));
            sb.append("</td>" + NL); //$NON-NLS-1$

            if (tileIndex % numHorizontalTiles == numHorizontalTiles - 1) {
               sb.append("</tr>" + NL); //$NON-NLS-1$
               isTrOpen = false;
            }

            tileIndex++;
         }
      }

      if (isTrOpen) {
         sb.append("</tr>" + NL); //$NON-NLS-1$
      }

      sb.append("</tbody></table>" + NL); //$NON-NLS-1$
   }

   private String createHTML_82_Easy_Tile(final ImportLauncher importTile) {

      /*
       * Tooltip
       */
      final String tooltip = createHTML_84_TileTooltip(importTile);

      /*
       * Tile image
       */
      final Image tileImage = getImportConfigImage(importTile, true);
      String htmlImage = UI.EMPTY_STRING;
      if (tileImage != null) {

         final byte[] pngImageData = ImageUtils.formatImage(tileImage, SWT.IMAGE_PNG);
         final String base64Encoded = Base64.getEncoder().encodeToString(pngImageData);

         htmlImage = "<img src='data:image/png;base64," + base64Encoded + "'>"; //$NON-NLS-1$ //$NON-NLS-2$
      }

      /*
       * Tile HTML
       */
      final String href = HTTP_DUMMY + HREF_ACTION_DEVICE_IMPORT + HREF_TOKEN + importTile.getId();

      final String htmlAnnotationImages = createHTML_86_AnnotationImages(importTile);

      final String html = UI.EMPTY_STRING

            + "<a href='" + href + "' title='" + tooltip + "' class='import-tile'>" + NL //     //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "   <div class='import-tile-image'>" + htmlImage + "</div>" + NL //               //$NON-NLS-1$ //$NON-NLS-2$
            + "   <div class='import-tile-config'>" + htmlAnnotationImages + "</div>" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
            + "</a>" + NL //                                                                    //$NON-NLS-1$
      ;

      return html;
   }

   private String createHTML_84_TileTooltip(final ImportLauncher importLauncher) {

      final StringBuilder sb = new StringBuilder();

      final String tileName = importLauncher.name.trim();
      final String tileDescription = importLauncher.description.trim();
      final String tourTypeText = EasyLauncherUtils.getTourTypeText(importLauncher, tileName);

      {
         // tour type name

         if (tileName.length() > 0) {

            sb.append(tileName);
            sb.append(NL);
         }
      }
      {
         // tile description

         if (tileDescription.length() > 0) {

            sb.append(NL);
            sb.append(tileDescription);
            sb.append(NL);
         }
      }
      {
         // tour type text

         if (tourTypeText.length() > 0) {

            sb.append(NL);
            sb.append(tourTypeText);
            sb.append(NL);
         }
      }
      {
         // tag group tags

         if (importLauncher.isSetTags()) {

            EasyLauncherUtils.getTagGroupText(importLauncher, sb);

         } else {

            sb.append(NL);
            sb.append(Messages.Import_Data_HTML_SetTourTags_NO.formatted());
         }
      }
      {
         // 2nd last time slice marker

         sb.append(NL);
         sb.append(importLauncher.isRemove2ndLastTimeSliceMarker
               ? Messages.Import_Data_HTML_Remove2dLastTimeSliceMarker_Yes
               : Messages.Import_Data_HTML_Remove2dLastTimeSliceMarker_No);
      }
      {
         // last marker

         final double distance = importLauncher.lastMarkerDistance / 1000.0 / UI.UNIT_VALUE_DISTANCE;

         final String distanceValue = _nf1.format(distance) + UI.SPACE1 + UI.UNIT_LABEL_DISTANCE;

         sb.append(NL);
         sb.append(importLauncher.isSetLastMarker
               ? NLS.bind(Messages.Import_Data_HTML_LastMarker_Yes, distanceValue, importLauncher.lastMarkerText)
               : Messages.Import_Data_HTML_LastMarker_No);
      }
      {
         // adjust temperature

         sb.append(NL);

         if (importLauncher.isAdjustTemperature) {

            final float temperature = UI.convertTemperatureFromMetric(importLauncher.tourAvgTemperature);

            final String temperatureText = NLS.bind(Messages.Import_Data_HTML_AdjustTemperature_Yes,
                  new Object[] {
                        getDurationText(importLauncher),
                        _nf1.format(temperature),
                        UI.UNIT_LABEL_TEMPERATURE });

            sb.append(temperatureText);

         } else {

            sb.append(Messages.Import_Data_HTML_AdjustTemperature_No);
         }
      }
      {
         // adjust elevation

         sb.append(NL);

         sb.append(importLauncher.isReplaceFirstTimeSliceElevation
               ? Messages.Import_Data_HTML_ReplaceFirstTimeSliceElevation_Yes
               : Messages.Import_Data_HTML_ReplaceFirstTimeSliceElevation_No);
      }
      {
         // set elevation from SRTM

         sb.append(NL);

         sb.append(importLauncher.isReplaceElevationFromSRTM
               ? Messages.Import_Data_HTML_ReplaceElevationFromSRTM_Yes
               : Messages.Import_Data_HTML_ReplaceElevationFromSRTM_No);
      }
      {
         // retrieve weather data

         sb.append(NL);

         sb.append(importLauncher.isRetrieveWeatherData
               ? Messages.Import_Data_HTML_RetrieveWeatherData_Yes
               : Messages.Import_Data_HTML_RetrieveWeatherData_No);
      }
      {
         // retrieve tour location

         sb.append(NL);

         sb.append(importLauncher.isRetrieveTourLocation
               ? Messages.Import_Data_HTML_RetrieveTourLocation_Yes
               : Messages.Import_Data_HTML_RetrieveTourLocation_No);
      }
      {
         // save tour

         sb.append(NL);

         sb.append(importLauncher.isSaveTour
               ? Messages.Import_Data_HTML_SaveTour_Yes
               : Messages.Import_Data_HTML_SaveTour_No);
      }
      {
         // delete device files

         sb.append(NL);

         sb.append(getEasyConfig().getActiveImportConfig().isDeleteDeviceFiles
               ? Messages.Import_Data_HTML_DeleteDeviceFiles_Yes
               : Messages.Import_Data_HTML_DeleteDeviceFiles_No);
      }

      return sb.toString();
   }

   private String createHTML_86_AnnotationImages(final ImportLauncher importLauncher) {

      /*
       * Save tour
       */
      String htmlSaveTour = UI.EMPTY_STRING;
      if (importLauncher.isSaveTour) {

         final String stateImage = createHTML_BgImage(_imageUrl_State_SaveTour);

         htmlSaveTour = createHTML_TileAnnotation(stateImage);
      }

      /*
       * Adjust temperature
       */
      String htmlAdjustTemperature = UI.EMPTY_STRING;
      if (importLauncher.isAdjustTemperature) {

         final String stateImage = createHTML_BgImage(_imageUrl_State_AdjustTemperature);

         htmlAdjustTemperature = createHTML_TileAnnotation(stateImage);
      }

      /*
       * Retrieve Weather Data
       */
      String htmlRetrieveWeatherData = UI.EMPTY_STRING;
      if (importLauncher.isRetrieveWeatherData) {

         final String stateImage = createHTML_BgImage(_imageUrl_State_RetrieveWeatherData);

         htmlRetrieveWeatherData = createHTML_TileAnnotation(stateImage);
      }

      /*
       * Retrieve tour location
       */
      String htmlRetrieveTourLocation = UI.EMPTY_STRING;
      if (importLauncher.isRetrieveTourLocation) {

         final String stateImage = createHTML_BgImage(_imageUrl_State_RetrieveTourLocation);

         htmlRetrieveTourLocation = createHTML_TileAnnotation(stateImage);
      }

      /*
       * Marker
       */
      String htmlLastMarker = UI.EMPTY_STRING;
      if (importLauncher.isSetLastMarker) {

         final String stateImage = createHTML_BgImage(_imageUrl_State_TourMarker);

         htmlLastMarker = createHTML_TileAnnotation(stateImage);
      }

      /*
       * Tags
       */
      String htmlTourTags = UI.EMPTY_STRING;
      if (importLauncher.isSetTags()) {

         final String stateImage = createHTML_BgImage(_imageUrl_State_TourTags);

         htmlTourTags = createHTML_TileAnnotation(stateImage);
      }

      /*
       * Delete device files
       */
      String htmlDeleteFiles = UI.EMPTY_STRING;
      if (getEasyConfig().getActiveImportConfig().isDeleteDeviceFiles) {

         final String stateImage = createHTML_BgImage(_imageUrl_State_MovedFiles);

         htmlDeleteFiles = createHTML_TileAnnotation(stateImage);
      }

      final StringBuilder sb = new StringBuilder();

      // order is reverted that it looks in the correct order
      sb.append(htmlDeleteFiles);
      sb.append(htmlSaveTour);
      sb.append(htmlRetrieveTourLocation);
      sb.append(htmlRetrieveWeatherData);
      sb.append(htmlAdjustTemperature);
      sb.append(htmlLastMarker);
      sb.append(htmlTourTags);

      sb.append("<div style='float:left;'>" + importLauncher.name + "</div>"); //$NON-NLS-1$ //$NON-NLS-2$

      return sb.toString();
   }

   private void createHTML_90_SimpleImport(final StringBuilder sb, final EasyConfig easyConfig) {

      sb.append("<div class='get-tours-items'>" + NL); //$NON-NLS-1$
      sb.append("   <table><tbody><tr>" + NL); //$NON-NLS-1$
      {
         if (easyConfig.isShowTile_Files) {

            createHTML_92_TileAction(
                  sb,
                  Messages.Import_Data_HTML_ImportFromFiles_Action,
                  Messages.Import_Data_HTML_ImportFromFiles_ActionTooltip,
                  HTTP_DUMMY + HREF_ACTION_IMPORT_FROM_FILES,
                  _imageUrl_ImportFromFile);
         }

         if (easyConfig.isShowTile_CloudApps) {

            for (final TourbookCloudDownloader cloudDownloader : _cloudDownloadersList) {

               createHTML_92_TileAction(
                     sb,
                     cloudDownloader.getName(),
                     cloudDownloader.getTooltip(),
                     HTTP_DUMMY + HREF_TOKEN + cloudDownloader.getId(),
                     cloudDownloader.getIconUrl());
            }
         }

         if (easyConfig.isShowTile_SerialPort) {

            createHTML_92_TileAction(
                  sb,
                  Messages.Import_Data_HTML_ReceiveFromSerialPort_ConfiguredAction,
                  Messages.Import_Data_HTML_ReceiveFromSerialPort_ConfiguredLink,
                  HTTP_DUMMY + HREF_ACTION_SERIAL_PORT_CONFIGURED,
                  _imageUrl_SerialPort_Configured);
         }

         if (easyConfig.isShowTile_SerialPortWithConfig) {

            createHTML_92_TileAction(
                  sb,
                  Messages.Import_Data_HTML_ReceiveFromSerialPort_DirectlyAction,
                  Messages.Import_Data_HTML_ReceiveFromSerialPort_DirectlyLink,
                  HTTP_DUMMY + HREF_ACTION_SERIAL_PORT_DIRECTLY,
                  _imageUrl_SerialPort_Directly);
         }

         if (easyConfig.isShowTile_FossilUI) {

            createHTML_92_TileAction(
                  sb,
                  Messages.Import_Data_HTML_Action_OldUI,
                  Messages.Import_Data_HTML_Action_OldUI_Tooltip,
                  HTTP_DUMMY + HREF_ACTION_OLD_UI,
                  null);
         }
      }
      sb.append("   </tr></tbody></table>" + NL); // //$NON-NLS-1$
      sb.append("</div>" + NL); //$NON-NLS-1$

   }

   private void createHTML_92_TileAction(final StringBuilder sb,
                                         final String name,
                                         final String tooltip,
                                         final String href,
                                         final String imageUrl) {

      String htmlImage = UI.EMPTY_STRING;

      if (imageUrl != null) {

         htmlImage = "style='" // //$NON-NLS-1$

               + "background-image:   url(" + imageUrl + ");" + NL //$NON-NLS-1$ //$NON-NLS-2$
               + "background-size:    16px;" + NL //$NON-NLS-1$

               + "'"; //$NON-NLS-1$
      }

      final String validTooltip = tooltip.replace("'", UI.EMPTY_STRING); //$NON-NLS-1$

      final String html = UI.EMPTY_STRING

            + HTML_TD
            + "<a href='" + href + "' title='" + validTooltip + "' class='import-tile'>" + NL //   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "   <div class='import-tile-image action-button' " + htmlImage + "></div>" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
            + "   <div class='import-tile-config'>" + name + "</div>" + NL //                      //$NON-NLS-1$ //$NON-NLS-2$
            + "</a>" + NL //                                                                       //$NON-NLS-1$
            + HTML_TD_END;

      sb.append(html);
   }

   private String createHTML_BgImage(final String imageUrl) {

      final String encodedImageUrl = WEB.encodeSpace(imageUrl);

      return "background-image: url(" + encodedImageUrl + ");"; //$NON-NLS-1$ //$NON-NLS-2$
   }

   private String createHTML_BgImageStyle(final String imageUrl) {

      final String bgImage = createHTML_BgImage(imageUrl);

      return "style='" + bgImage + "'"; //$NON-NLS-1$ //$NON-NLS-2$
   }

   private String createHTML_TileAnnotation(final String imageBgStyle) {

      return "<div style='float: right;" + imageBgStyle + "' class='action-button-16'></div>"; //$NON-NLS-1$ //$NON-NLS-2$
   }

   private void createMenuManager() {

      _tagMenuManager = new TagMenuManager(this, true);
      _tourTypeMenuManager = new TourTypeMenuManager(this);

      _tourViewer_MenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _tourViewer_MenuManager.setRemoveAllWhenShown(true);
      _tourViewer_MenuManager.addMenuListener(menuManager -> fillTourViewer_ContextMenu(menuManager));

      _simpleUI_ImportLauncher_MenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _simpleUI_ImportLauncher_MenuManager.setRemoveAllWhenShown(true);
      _simpleUI_ImportLauncher_MenuManager.addMenuListener(menuManager -> fillSimpleUI_ContextMenu(menuManager));
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);
      createMenuManager();

      // define all columns
      _tourViewer_ColumnManager = new ColumnManager(this, _state);
      _tourViewer_ColumnManager.setIsCategoryAvailable(true);
      defineAllColumns();

      createActions();
      createUI(parent);

      fillUI();
      fillToolbar();

      addPartListener();
      addSelectionListener();
      addPrefListener();
      addTourEventListener();

      // set this view part as selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      _activePerson = TourbookPlugin.getActivePerson();

      // prevent that the UI start is run twice, 2nd would be in the part listener
      _newActivePerson = _activePerson;

      enableActions();
      restoreState();

      // the part visible listener shows the top page also
      updateUI_1_TopPage(true);
   }

   private void createResources_Image() {

      _images = new ImageRegistry();

// SET_FORMATTING_OFF

      /*
       * Database
       */
      _images.put(IMAGE_DATABASE,                  TourbookPlugin.getImageDescriptor      (Images.Saved_Tour));
      _images.put(IMAGE_DATABASE_OTHER_PERSON,     TourbookPlugin.getThemedImageDescriptor(Images.Saved_Tour_OtherPerson));
      _images.put(IMAGE_ASSIGN_MERGED_TOUR,        TourbookPlugin.getThemedImageDescriptor(Images.Saved_MergedTour));
      _images.put(IMAGE_ICON_PLACEHOLDER,          TourbookPlugin.getImageDescriptor      (Images.App_EmptyIcon_Placeholder));
      _images.put(IMAGE_DELETE,                    TourbookPlugin.getImageDescriptor      (Images.App_Delete));

      /*
       * Import state
       */
      _images.put(IMAGE_STATE_DELETE,              TourbookPlugin.getImageDescriptor      (Images.State_DeletedTour_View));
      _images.put(IMAGE_STATE_MOVED,               TourbookPlugin.getImageDescriptor      (Images.State_MovedTour_View));

      /*
       * Data transfer
       */
      _images.put(IMAGE_DATA_TRANSFER,             TourbookPlugin.getImageDescriptor      (Images.RawData_Transfer));
      _images.put(IMAGE_DATA_TRANSFER_DIRECT,      TourbookPlugin.getImageDescriptor      (Images.RawData_TransferDirect));
      _images.put(IMAGE_IMPORT_FROM_FILES,         TourbookPlugin.getImageDescriptor      (Images.Import_Files));
      _images.put(IMAGE_NEW_UI,                    TourbookPlugin.getThemedImageDescriptor(Images.Import_DashboardUI));

      /*
       * Simple easy UI
       */
      _images.put(IMAGE_DEVICE_FOLDER_ERROR,       TourbookPlugin.getImageDescriptor      (Images.RawData_DeviceFolder_Error));
      _images.put(IMAGE_DEVICE_FOLDER_IS_CHECKING, TourbookPlugin.getImageDescriptor      (Images.RawData_DeviceFolder_IsChecking));
      _images.put(IMAGE_DEVICE_FOLDER_NOT_SETUP,   TourbookPlugin.getImageDescriptor      (Images.RawData_DeviceFolder_NotSetup));
      _images.put(IMAGE_DEVICE_FOLDER_OFF,         TourbookPlugin.getImageDescriptor      (Images.RawData_DeviceFolder_Off));
      _images.put(IMAGE_DEVICE_FOLDER_OK,          TourbookPlugin.getImageDescriptor      (Images.RawData_DeviceFolder_OK));
      _images.put(IMAGE_DEVICE_TURN_ON,            TourbookPlugin.getImageDescriptor      (Images.RawData_Device_TurnOn));
      _images.put(IMAGE_DEVICE_TURN_OFF,           TourbookPlugin.getImageDescriptor      (Images.RawData_Device_TurnOff));
      _images.put(IMAGE_STATE_OK,                  TourbookPlugin.getThemedImageDescriptor(Images.State_OK));
      _images.put(IMAGE_STATE_ERROR,               TourbookPlugin.getThemedImageDescriptor(Images.State_Error));

// SET_FORMATTING_ON
   }

   private void createResources_Web() {

      try {

         /*
          * Font css must be on the top
          */
         final File fontFile = WEB.getResourceFile(WEB_RESOURCE_TITLE_FONT);

         final Path path = Paths.get(fontFile.getAbsolutePath());
         final byte[] data = Files.readAllBytes(path);
         final String base64Encoded = Base64.getEncoder().encodeToString(data);

         _cssFonts = "<style>" + NL //                               //$NON-NLS-1$

               + "@font-face" + NL //                                //$NON-NLS-1$
               + "{" + NL //                                         //$NON-NLS-1$
               + "   font-family:   'Nunito-Bold';" + NL //          //$NON-NLS-1$
               + "   font-weight:   700;" + NL //                    //$NON-NLS-1$
               + "   font-style:    bold;" + NL //                 //$NON-NLS-1$
               + "   src:           url(data:font/truetype;charset=utf-8;base64," + base64Encoded + ") format('truetype');" //$NON-NLS-1$ //$NON-NLS-2$
               + "}" + NL //                                         //$NON-NLS-1$

               + "</style>" + NL //                                  //$NON-NLS-1$
         ;

         /*
          * Webpage css
          */
         File webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_CSS);
         final String css = Util.readContentFromFile(webFile.getAbsolutePath());

         String css3 = UI.EMPTY_STRING;

         /*
          * Eclipse 3.8 with Linux has only a limited css3 support -> DISABLED
          */
         if (UI.IS_WIN) {
            webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_CSS3);
            css3 = Util.readContentFromFile(webFile.getAbsolutePath());
         }

         _cssFromFile = UI.EMPTY_STRING

               + "<style>" + NL //        //$NON-NLS-1$
               + css
               + css3
               + "</style>" + NL //      //$NON-NLS-1$
         ;

// SET_FORMATTING_OFF

         /*
          * Image urls
          */
         _imageUrl_ImportFromFile               = getIconUrl(Images.Import_Files);
         _imageUrl_SerialPort_Configured        = getIconUrl(Images.RawData_Transfer);
         _imageUrl_SerialPort_Directly          = getIconUrl(Images.RawData_TransferDirect);

         _imageUrl_State_AdjustTemperature      = getIconUrl(Images.State_AdjustTemperature);
         _imageUrl_State_RetrieveTourLocation   = getIconUrl(Images.State_RetrieveTourLocation);
         _imageUrl_State_RetrieveWeatherData    = getIconUrl(Images.State_RetrieveWeatherData);
         _imageUrl_State_Error                  = getIconUrl(Images.State_Error);
         _imageUrl_State_OK                     = getIconUrl(Images.State_OK);
         _imageUrl_State_MovedFiles             = getIconUrl(Images.State_MovedTour);
         _imageUrl_State_SaveTour               = getIconUrl(Images.State_SaveTour);
         _imageUrl_State_TourMarker             = getIconUrl(Images.State_TourMarker);
         _imageUrl_State_TourTags               = getIconUrl(Images.State_TourTags);

         _imageUrl_Device_TurnOff               = getIconUrl(Images.RawData_Device_TurnOff);
         _imageUrl_Device_TurnOn                = getIconUrl(Images.RawData_Device_TurnOn);

         _imageUrl_DeviceFolder_OK              = getIconUrl(Images.RawData_DeviceFolder_OK);
         _imageUrl_DeviceFolder_Off             = getIconUrl(Images.RawData_DeviceFolder_Off);
         _imageUrl_DeviceFolder_Error           = getIconUrl(Images.RawData_DeviceFolder_Error);
         _imageUrl_DeviceFolder_IsChecking      = getIconUrl(Images.RawData_DeviceFolder_IsChecking);
         _imageUrl_DeviceFolder_NotSetup        = getIconUrl(Images.RawData_DeviceFolder_NotSetup);

// SET_FORMATTING_ON

      } catch (final IOException e) {
         TourLogManager.log_EXCEPTION_WithStacktrace(e);
      }
   }

   private String createTagGroupText(final ImportLauncher importLauncher) {

      final StringBuilder sb = new StringBuilder();

      final String tourTagGroupID = importLauncher.tourTagGroupID;

      final TagGroup tagGroup = TagGroupManager.getTagGroup(tourTagGroupID);
      final Set<TourTag> allTags = TagGroupManager.getTags(tourTagGroupID);

      if (tagGroup == null || allTags == null) {

         return UI.EMPTY_STRING;
      }

      final List<TourTag> sortedTags = new ArrayList<>(allTags);
      Collections.sort(sortedTags);

      final StringBuilder sbTags = new StringBuilder();

      for (int tagIndex = 0; tagIndex < sortedTags.size(); tagIndex++) {

         final TourTag tourTag = sortedTags.get(tagIndex);

         if (tagIndex > 0) {
            sbTags.append(UI.SPACE2 + UI.SYMBOL_BULLET + UI.SPACE2);
         }

         sbTags.append(tourTag.getTagName());
      }

      sb.append("\"%s\" : %s".formatted(tagGroup.name, sbTags.toString())); //$NON-NLS-1$

      return sb.toString();
   }

   private void createUI(final Composite parent) {

      _topPage_PageBook = new PageBook(parent, SWT.NONE);

// SET_FORMATTING_OFF

      _topPage_Startup                    = createUI_00_Page_Startup    (_topPage_PageBook);
      _topPage_ImportUI_FossilUI          = createUI_10_Page_FossilUI   (_topPage_PageBook);
      _topPage_ImportUI_EasyImport_Simple = createUI_50_Page_SimpleUI   (_topPage_PageBook);

      _topPage_ImportViewer               = createUI_90_Page_TourViewer (_topPage_PageBook);

// SET_FORMATTING_ON

      _topPage_PageBook.showPage(_topPage_Startup);
   }

   /**
    * This page is displayed until the first page of the browser is loaded.
    *
    * @param parent
    *
    * @return
    */
   private Composite createUI_00_Page_Startup(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      {
//         final Label label = new Label(container, SWT.NONE);
//         GridDataFactory.fillDefaults().grab(true, true).align(SWT.CENTER, SWT.CENTER).applyTo(label);
//         label.setText("TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST "); //$NON-NLS-1$
//      }

      return container;
   }

   private Composite createUI_10_Page_FossilUI(final Composite parent) {

      final int defaultWidth = 300;

      final GridDataFactory linkGridData = GridDataFactory.fillDefaults()
            .hint(defaultWidth, SWT.DEFAULT)
            .align(SWT.FILL, SWT.CENTER)
            .grab(true, false)
            .indent(0, 10);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         {
            /*
             * Import info
             */
            final Label label = new Label(container, SWT.WRAP);
            label.setText(Messages.Import_Data_OldUI_Label_Info);
            GridDataFactory.fillDefaults()
                  .hint(defaultWidth, SWT.DEFAULT)
                  .grab(true, false)
                  .span(2, 1)
                  .applyTo(label);
         }
         {
            /*
             * Import
             */
            // icon
            final CLabel iconImport = new CLabel(container, SWT.NONE);
            iconImport.setImage(_images.get(IMAGE_IMPORT_FROM_FILES));
            GridDataFactory.fillDefaults()
                  .indent(0, 10)
                  .align(SWT.CENTER, SWT.BEGINNING)
                  .applyTo(iconImport);

            // link
            _linkImport = new Link(container, SWT.NONE);
            _linkImport.setText(Messages.Import_Data_OldUI_Link_Import);
            _linkImport.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> _rawDataMgr.actionImportFromFile()));
            linkGridData.applyTo(_linkImport);
         }
         {
            /*
             * Data transfer
             */
            // icon
            final CLabel iconTransfer = new CLabel(container, SWT.NONE);
            iconTransfer.setImage(_images.get(IMAGE_DATA_TRANSFER));
            GridDataFactory.fillDefaults()
                  .align(SWT.CENTER, SWT.BEGINNING)
                  .indent(0, 10)
                  .applyTo(iconTransfer);

            // link
            final Link linkTransfer = new Link(container, SWT.NONE);
            linkTransfer.setText(Messages.Import_Data_OldUI_Link_ReceiveFromSerialPort_Configured);
            linkTransfer.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> _rawDataMgr.actionImportFromDevice()));
            linkGridData.applyTo(linkTransfer);
         }
         {
            /*
             * Direct data transfer
             */
            // icon
            final CLabel iconDirectTransfer = new CLabel(container, SWT.NONE);
            iconDirectTransfer.setImage(_images.get(IMAGE_DATA_TRANSFER_DIRECT));
            GridDataFactory.fillDefaults()
                  .align(SWT.CENTER, SWT.BEGINNING)
                  .indent(0, 10)
                  .applyTo(iconDirectTransfer);

            // link
            final Link linkTransferDirect = new Link(container, SWT.NONE);
            linkTransferDirect.setText(Messages.Import_Data_OldUI_Link_ReceiveFromSerialPort_Directly);
            linkTransferDirect.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> _rawDataMgr.actionImportFromDeviceDirect()));
            linkGridData.applyTo(linkTransferDirect);
         }
         {
            /*
             * New UI
             */
            // icon
            final CLabel icon = new CLabel(container, SWT.NONE);
            icon.setImage(_images.get(IMAGE_NEW_UI));
            GridDataFactory.fillDefaults().indent(0, 10).applyTo(icon);

            // link
            final Link link = new Link(container, SWT.NONE);
            link.setText(Messages.Import_Data_OldUI_Link_ShowNewUI);
            link.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> onSelect_ImportUI(ImportUI.EASY_IMPORT_FANCY)));
            linkGridData.applyTo(link);
         }
         {
            /*
             * Hint
             */
            final Label label = new Label(container, SWT.WRAP);
            label.setText(Messages.Import_Data_OldUI_Label_Hint);
            GridDataFactory.fillDefaults()
                  .hint(defaultWidth, SWT.DEFAULT)
                  .grab(true, true)
                  .align(SWT.FILL, SWT.END)
                  .indent(0, 10)
                  .span(2, 1)
                  .applyTo(label);
         }
      }

      return container;
   }

   private Composite createUI_20_Page_EasyImporFancy(final Composite parent) {

      final Color bgColor = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

      _easyImportFancy_PageBook = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_easyImportFancy_PageBook);

      /*
       * Page: Browser error
       */
      _easyImportFancy_Page_NoBrowser = new Composite(_easyImportFancy_PageBook, SWT.NONE);
      _easyImportFancy_Page_NoBrowser.setBackground(bgColor);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_easyImportFancy_Page_NoBrowser);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_easyImportFancy_Page_NoBrowser);
      {
         _txtNoBrowser = new Text(_easyImportFancy_Page_NoBrowser, SWT.WRAP | SWT.READ_ONLY);
         _txtNoBrowser.setText(Messages.UI_Label_BrowserCannotBeCreated);
         _txtNoBrowser.setBackground(bgColor);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .align(SWT.FILL, SWT.BEGINNING)
               .applyTo(_txtNoBrowser);
      }

      /*
       * Page: Browser for fancy UI
       */
      _easyImportFancy_Page_WithBrowser = new Composite(_easyImportFancy_PageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_easyImportFancy_Page_WithBrowser);
      {
         createUI_22_Browser(_easyImportFancy_Page_WithBrowser);
      }

      return _easyImportFancy_PageBook;
   }

   private void createUI_22_Browser(final Composite parent) {

      try {

         try {

            // use default browser
            _browser = new Browser(parent, SWT.NONE);

            // initial setup
            _browser.setRedraw(false);

         } catch (final Exception e) {

//            /*
//             * Use mozilla browser, this is necessary for Linux when default browser fails
//             * however the XULrunner needs to be installed.
//             */
//            _browser = new Browser(parent, SWT.MOZILLA);
         }

         if (_browser != null) {

            GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

            new JS_OnSelectImportConfig(_browser, JS_FUNCTION_ON_SELECT_IMPORT_CONFIG);

            _browser.addLocationListener(new LocationAdapter() {

               @Override
               public void changed(final LocationEvent event) {

//                  _browser.removeLocationListener(this);
//                  function.dispose();
               }

               @Override
               public void changing(final LocationEvent event) {
                  onBrowser_LocationChanging(event);
               }
            });

            _browser.addProgressListener(new ProgressAdapter() {
               @Override
               public void completed(final ProgressEvent event) {

                  onBrowser_Completed();

               }
            });
         }

      } catch (final SWTError e) {

         _txtNoBrowser.setText(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, e.getMessage()));

      } finally {

         showFailbackUI();
      }
   }

   private Composite createUI_50_Page_SimpleUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
      {
         createUI_51_SimpleUI_Header(container);
         createUI_56_SimpleUI_Viewer(container);
      }

      return container;
   }

   private void createUI_51_SimpleUI_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(5)
            .spacing(15, 5)
            .applyTo(container);
//      container.setBackground(UI.SYS_COLOR_CYAN);
      {
         {
            /*
             * Action: Start/stop watching import folder
             */
            UI.createToolbarAction(container, _actionSimpleUI_StartStopWatching);
         }
         {
            /*
             * Title: Easy Import
             */
            _lblSimpleUI_EasyImportTitle = new Label(container, SWT.NONE);
            _lblSimpleUI_EasyImportTitle.setText(Messages.Import_Data_HTML_EasyImport);
            _lblSimpleUI_EasyImportTitle.setForeground(UI.SYS_COLOR_WIDGET_DARK_SHADOW);
            MTFont.setBannerFont(_lblSimpleUI_EasyImportTitle);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblSimpleUI_EasyImportTitle);
         }
         {
            /*
             * Action: Device state
             */
            UI.createToolbarAction(container, _actionSimpleUI_DeviceState);
         }
         {
            /*
             * Label: Number of not imported files
             */
            _lblSimpleUI_NumNotImportedFiles = new Label(container, SWT.CENTER);
            _lblSimpleUI_NumNotImportedFiles.setText(UI.EMPTY_STRING);
            _lblSimpleUI_NumNotImportedFiles.setToolTipText(Messages.Import_Data_Label_NumNotImportedFiles);
            GridDataFactory.fillDefaults()
                  .align(SWT.CENTER, SWT.CENTER)
                  .hint(_pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)

                  // adjust position otherwise it is not exactly centered
                  .indent(-4, 0)

                  .applyTo(_lblSimpleUI_NumNotImportedFiles);

//            _lblSimpleUI_NumNotImportedFiles.setBackground(UI.SYS_COLOR_YELLOW);
         }
         {
            /*
             * Combo: Configuration
             */
            _comboSimpleUI_Config = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboSimpleUI_Config.setToolTipText(Messages.Import_Data_Combo_SimpleUIConfig_Tooltip);
            _comboSimpleUI_Config.setVisibleItemCount(20);
            _comboSimpleUI_Config.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_ImportConfig_SimpleUI()));
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_comboSimpleUI_Config);
         }
      }
   }

   private void createUI_56_SimpleUI_Viewer(final Composite parent) {

      // define all columns for this viewer
      _simpleUI_ImportLauncher_ColumnManager = new ColumnManager(_simpleUI_ImportLauncher_ColumnViewer, _stateSimpleUI);
      _simpleUI_ImportLauncher_Utils.defineAllColumns(_simpleUI_ImportLauncher_ColumnManager, _pc);

      _simpleUI_ViewerContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(_simpleUI_ViewerContainer);
      GridLayoutFactory.fillDefaults().applyTo(_simpleUI_ViewerContainer);
//      _viewerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         createUI_57_SimpleUI_ViewerTable(_simpleUI_ViewerContainer);
      }
   }

   private void createUI_57_SimpleUI_ViewerTable(final Composite parent) {

      /*
       * Create table
       */
      final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);

      /*
       * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
       * critical for performance that these methods be as efficient as possible.
       */
      final Listener paintListener = event -> {

         if (event.type == SWT.MeasureItem || event.type == SWT.PaintItem) {

            onPaint_SimpleUIViewer(event);
         }
      };
      table.addListener(SWT.MeasureItem, paintListener);
      table.addListener(SWT.PaintItem, paintListener);

      /*
       * Create viewer
       */
      _simpleUI_ImportLauncher_Viewer = new TableViewer(table);

      _simpleUI_ImportLauncher_ColumnManager.createColumns(_simpleUI_ImportLauncher_Viewer);
//    _simpleUI_ImportLauncher_ColumnManager.createHeaderContextMenu(table, new EmptyContextMenuProvider());

      _simpleUI_ColumnIndexConfigImage = _simpleUI_ImportLauncher_Utils.getColDef_TourTypeImage().getCreateIndex();

      _simpleUI_ImportLauncher_Viewer.setUseHashlookup(true);
      _simpleUI_ImportLauncher_Viewer.setContentProvider(new SimpleUI_ImportLauncher_ContentProvider());
      _simpleUI_ImportLauncher_Viewer.addFilter(new SimpleUI_ImportLauncher_ViewerFilter());

      _simpleUI_ImportLauncher_Viewer.addDoubleClickListener(doubleClickEvent -> runEasyImport());

      createUI_60_SimpleUI_ContextMenu();
   }

   /**
    * Create the launcher context menu
    */
   private void createUI_60_SimpleUI_ContextMenu() {

      _simpleUI_ImportLauncher_ContextMenu = createUI_62_SimpleUI_CreateContextMenu();

      final Table table = (Table) _simpleUI_ImportLauncher_Viewer.getControl();

      _simpleUI_ImportLauncher_ColumnManager.createHeaderContextMenu(table, _simpleUI_ImportLauncher_ContextMenuProvider);
   }

   private Menu createUI_62_SimpleUI_CreateContextMenu() {

      final Table table = (Table) _simpleUI_ImportLauncher_Viewer.getControl();
      final Menu tableContextMenu = _simpleUI_ImportLauncher_MenuManager.createContextMenu(table);

      return tableContextMenu;
   }

   private Composite createUI_90_Page_TourViewer(final Composite parent) {

      _tourViewer_Container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_tourViewer_Container);
      {
         createUI_92_TourViewer(_tourViewer_Container);
      }

      return _tourViewer_Container;
   }

   /**
    * @param parent
    */
   private void createUI_92_TourViewer(final Composite parent) {

      // table
      final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

      table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      table.setHeaderVisible(true);
      table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      _tourViewer = new TableViewer(table);
      _tourViewer_ColumnManager.createColumns(_tourViewer);

      // table viewer
      _tourViewer.setContentProvider(new TourViewer_ContentProvider());
      _tourViewer.setComparator(_tourViewer_Comparator);

      _tourViewer.addDoubleClickListener(doubleClickEvent -> {

         final Object firstElement = ((IStructuredSelection) _tourViewer.getSelection()).getFirstElement();

         if (firstElement instanceof TourData) {
            TourManager.getInstance().tourDoubleClickAction(RawDataView.this, _tourDoubleClickState);
         }
      });

      _tourViewer.addSelectionChangedListener(selectionChangedEvent -> {

         if (_isInUpdate) {
            return;
         }

         fireSelectedTour();
      });

      // set tour info tooltip provider
      _tourInfoToolTip = new TableViewerTourInfoToolTip(_tourViewer);

      /*
       * Setup tour comparator
       */
      _tourViewer_Comparator.__sortColumnId = _columnId_TourStartDate;
      _tourViewer_Comparator.__sortDirection = TourViewer_Comparator.ASCENDING;

      // show the sorting indicator in the viewer
      updateUI_ShowSortDirection(
            _tourViewer_Comparator.__sortColumnId,
            _tourViewer_Comparator.__sortDirection);

      createUI_93_ColumnImages(table);
      createUI_94_ContextMenu();
   }

   private void createUI_93_ColumnImages(final Table table) {

      boolean isColumnVisible = false;
      final ControlListener controlResizedAdapter = controlResizedAdapter(controlEvent -> onResize_SetWidthForImageColumn());

      // update column index which is needed for repainting
      final ColumnProfile activeProfile = _tourViewer_ColumnManager.getActiveProfile();
      _columnIndex_TourTypeImage = activeProfile.getColumnIndex(_colDef_TourTypeImage.getColumnId());
      _columnIndex_WeatherClouds = activeProfile.getColumnIndex(_colDef_WeatherClouds.getColumnId());

      final int numColumns = table.getColumns().length;

      // add column resize listener
      if (_columnIndex_TourTypeImage >= 0 && _columnIndex_TourTypeImage < numColumns) {

         isColumnVisible = true;
         table.getColumn(_columnIndex_TourTypeImage).addControlListener(controlResizedAdapter);
      }

      if (_columnIndex_WeatherClouds >= 0 && _columnIndex_WeatherClouds < numColumns) {

         isColumnVisible = true;
         table.getColumn(_columnIndex_WeatherClouds).addControlListener(controlResizedAdapter);
      }

      // add table resize listener
      if (isColumnVisible) {

         /*
          * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
          * critical for performance that these methods be as efficient as possible.
          */
         final Listener paintListener = event -> {

            if (event.type == SWT.PaintItem) {

               onPaint_TableViewer(event);
            }
         };

         table.addControlListener(controlResizedAdapter);
         table.addListener(SWT.PaintItem, paintListener);
      }
   }

   /**
    * create the views context menu
    */
   private void createUI_94_ContextMenu() {

      _tourViewer_ContextMenu = createUI_96_CreateViewerContextMenu();

      final Table table = (Table) _tourViewer.getControl();

      _tourViewer_ColumnManager.createHeaderContextMenu(table, _tourViewer_ContextMenuProvider);

      // this is from the beginning of the MT development and may not be needed
      getSite().registerContextMenu(_tourViewer_MenuManager, _tourViewer);
   }

   private Menu createUI_96_CreateViewerContextMenu() {

      final Table table = (Table) _tourViewer.getControl();
      final Menu tableContextMenu = _tourViewer_MenuManager.createContextMenu(table);

      tableContextMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuHidden(final MenuEvent e) {
            _tagMenuManager.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {
            _tagMenuManager.onShowMenu(menuEvent, table, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
         }
      });

      return tableContextMenu;
   }

   /**
    * Defines all columns for the table viewer in the column manager, the sequence defines the
    * default columns
    *
    * @param parent
    */
   private void defineAllColumns() {

      defineColumn_State_Import();
      defineColumn_State_Database();

      defineColumn_Time_TourDate();
      defineColumn_Time_TourStartTime();
      defineColumn_Time_ElapsedTime();
      defineColumn_Time_MovingTime();

      defineColumn_Time_TimeZone();
      defineColumn_Time_TimeZoneDifference();

      defineColumn_Tour_Type();
      defineColumn_Tour_TypeText();
      defineColumn_Tour_Title();
      defineColumn_Tour_Tags();
      defineColumn_Tour_Marker();

      defineColumn_Motion_Distance();
      defineColumn_Motion_AvgSpeed();
      defineColumn_Motion_AvgPace();

      defineColumn_Altitude_Up();
      defineColumn_Altitude_Down();

      defineColumn_Weather_Clouds();

      defineColumn_Body_Calories();

      defineColumn_Device_Name();
      defineColumn_Device_Profile();

      defineColumn_Data_ImportFileName();
      defineColumn_Data_ImportFilePath();
      defineColumn_Date_TimeInterval();

      // must be called before the columns are created but after they are defined
      updateUI_TourViewerColumns();
   }

   /**
    * column: altitude down
    */
   private void defineColumn_Altitude_Down() {

      final ColumnDefinition colDef = TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_DOWN.createColumn(
            _tourViewer_ColumnManager,
            _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final double dbValue = ((TourData) cell.getElement()).getTourAltDown();
            final double value = -dbValue / UI.UNIT_VALUE_ELEVATION;

            colDef.printValue_0(cell, value);
         }
      });
   }

   /**
    * column: altitude up
    */
   private void defineColumn_Altitude_Up() {

      final ColumnDefinition colDef = TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_UP.createColumn(
            _tourViewer_ColumnManager,
            _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final double dbValue = ((TourData) cell.getElement()).getTourAltUp();
            final double value = dbValue / UI.UNIT_VALUE_ELEVATION;

            colDef.printValue_0(cell, value);
         }
      });
   }

   /**
    * column: calories (cal)
    */
   private void defineColumn_Body_Calories() {

      final TableColumnDefinition colDef = TableColumnFactory.BODY_CALORIES.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourData element = (TourData) cell.getElement();
            final long value = element.getCalories() / 1000L;

            cell.setText(FormatManager.formatNumber_0(value));
         }
      });
   }

   /**
    * column: import file name
    */
   private void defineColumn_Data_ImportFileName() {

      final ColumnDefinition colDef = TableColumnFactory.DATA_IMPORT_FILE_NAME.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourData tourData = (TourData) cell.getElement();
            final String importFileName = tourData.getImportFileName();

            if (importFileName != null) {
               cell.setText(importFileName);
            }
         }
      });

      _columnId_ImportFileName = colDef.getColumnId();
   }

   /**
    * column: import file path
    */
   private void defineColumn_Data_ImportFilePath() {

      final ColumnDefinition colDef = TableColumnFactory.DATA_IMPORT_FILE_PATH.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourData tourData = (TourData) cell.getElement();
            final String importFilePath = tourData.getImportFilePath();

            if (importFilePath != null) {
               cell.setText(importFilePath);
            }
         }
      });
   }

   /**
    * column: time interval
    */
   private void defineColumn_Date_TimeInterval() {

      final ColumnDefinition colDef = TableColumnFactory.DATA_TIME_INTERVAL.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            cell.setText(Integer.toString(((TourData) cell.getElement()).getDeviceTimeInterval()));
         }
      });
   }

   /**
    * column: device name
    */
   private void defineColumn_Device_Name() {

      final ColumnDefinition colDef = TableColumnFactory.DEVICE_NAME.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourData tourData = (TourData) cell.getElement();

            final String deviceName = tourData.getDeviceName();
            final String firmwareVersion = tourData.getDeviceFirmwareVersion();

            final String name = firmwareVersion.length() == 0//
                  ? deviceName
                  : deviceName + UI.SPACE + UI.SYMBOL_BRACKET_LEFT + firmwareVersion + UI.SYMBOL_BRACKET_RIGHT;

            cell.setText(name);
         }
      });

      _columnId_DeviceName = colDef.getColumnId();
   }

   /**
    * column: device profile
    */
   private void defineColumn_Device_Profile() {

      final ColumnDefinition colDef = TableColumnFactory.DEVICE_PROFILE.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            cell.setText(((TourData) cell.getElement()).getDeviceModeName());
         }
      });
   }

   /**
    * column: average pace
    */
   private void defineColumn_Motion_AvgPace() {

      final ColumnDefinition colDef = TableColumnFactory.MOTION_AVG_PACE.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourData tourData = (TourData) cell.getElement();

            final float tourDistance = tourData.getTourDistance();
            final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);
            final long time = isPaceAndSpeedFromRecordedTime ? tourData.getTourDeviceTime_Recorded() : tourData.getTourComputedTime_Moving();

            final float pace = tourDistance == 0 ? //
                  0
                  : time * 1000 / tourDistance * UI.UNIT_VALUE_DISTANCE;

            if (pace == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(UI.format_mm_ss((long) pace));
            }
         }
      });
   }

   /**
    * column: avg speed
    */
   private void defineColumn_Motion_AvgSpeed() {

      final ColumnDefinition colDef = TableColumnFactory.MOTION_AVG_SPEED.createColumn(_tourViewer_ColumnManager, _pc);

      // show avg speed to verify the tour type by speed
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourData tourData = ((TourData) cell.getElement());
            final float tourDistance = tourData.getTourDistance();

            final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);
            final long time = isPaceAndSpeedFromRecordedTime ? tourData.getTourDeviceTime_Recorded() : tourData.getTourComputedTime_Moving();

            double value = 0;

            if (time != 0) {
               value = tourDistance / time * 3.6 / UI.UNIT_VALUE_DISTANCE;
            }

            colDef.printDetailValue(cell, value);
         }
      });
   }

   /**
    * column: distance (km/mile)
    */
   private void defineColumn_Motion_Distance() {

      final ColumnDefinition colDef = TableColumnFactory.MOTION_DISTANCE.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final double tourDistance = ((TourData) cell.getElement()).getTourDistance();
            final double value = tourDistance / 1000 / UI.UNIT_VALUE_DISTANCE;

            colDef.printDetailValue(cell, value);
         }
      });
   }

   /**
    * Column: Database state
    */
   private void defineColumn_State_Database() {

      final ColumnDefinition colDef = TableColumnFactory.STATE_DB_STATUS.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            // show the database indicator for the person who owns the tour

            final TourData tourData = (TourData) cell.getElement();

            cell.setImage(getStateImage_Db(tourData));
         }
      });
   }

   /**
    * Column: Import state
    */
   private void defineColumn_State_Import() {

      final ColumnDefinition colDef = TableColumnFactory.STATE_IMPORT_STATE.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourData tourData = (TourData) cell.getElement();
            final Image stateImage = getStateImage_Import(tourData);

            cell.setImage(stateImage);
         }
      });
   }

   /**
    * column: elapsed time
    */
   private void defineColumn_Time_ElapsedTime() {

      final ColumnDefinition colDef = TableColumnFactory.TIME__DEVICE_ELAPSED_TIME.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final long value = ((TourData) cell.getElement()).getTourDeviceTime_Elapsed();

            colDef.printDetailValue(cell, value);
         }
      });
   }

   /**
    * column: moving time
    */
   private void defineColumn_Time_MovingTime() {

      final ColumnDefinition colDef = TableColumnFactory.TIME__COMPUTED_MOVING_TIME.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final long value = ((TourData) cell.getElement()).getTourComputedTime_Moving();

            colDef.printDetailValue(cell, value);
         }
      });
   }

   /**
    * column: Timezone
    */
   private void defineColumn_Time_TimeZone() {

      final TableColumnDefinition colDef = TableColumnFactory.TIME_TIME_ZONE.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourData tourData = (TourData) cell.getElement();
            final String timeZoneId = tourData.getTimeZoneId();

            cell.setText(timeZoneId == null ? UI.EMPTY_STRING : timeZoneId);
         }
      });

      _columnId_TimeZone = colDef.getColumnId();
   }

   /**
    * column: Timezone difference
    */
   private void defineColumn_Time_TimeZoneDifference() {

      _timeZoneOffsetColDef = TableColumnFactory.TIME_TIME_ZONE_DIFFERENCE.createColumn(_tourViewer_ColumnManager, _pc);

      _timeZoneOffsetColDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourData tourData = (TourData) cell.getElement();
            final TourDateTime tourDateTime = tourData.getTourDateTime();

            cell.setText(tourDateTime.timeZoneOffsetLabel);
         }
      });
   }

   /**
    * column: date
    */
   private void defineColumn_Time_TourDate() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_DATE.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);
      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInDate == false) {
               return null;
            }

            return ((TourData) cell.getElement()).getTourId();
         }

         @Override
         public void update(final ViewerCell cell) {

            final TourData tourData = (TourData) cell.getElement();

            cell.setText(TourManager.getTourDateShort(tourData));
         }
      });

      _columnId_TourStartDate = colDef.getColumnId();
   }

   /**
    * column: time
    */
   private void defineColumn_Time_TourStartTime() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_START_TIME.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);
      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInTime == false) {
               return null;
            }

            return ((TourData) cell.getElement()).getTourId();
         }

         @Override
         public void update(final ViewerCell cell) {

            final TourData tourData = (TourData) cell.getElement();

            final ValueFormat valueFormatter = colDef.getValueFormat_Detail();

            if (valueFormatter.equals(ValueFormat.TIME_HH_MM_SS)) {

               cell.setText(tourData.getTourStartTime().format(TimeTools.Formatter_Time_M));

            } else {

               cell.setText(TourManager.getTourTimeShort(tourData));
            }
         }
      });
   }

   /**
    * column: markers
    */
   private void defineColumn_Tour_Marker() {

      final ColumnDefinition colDef = TableColumnFactory.TOUR_NUM_MARKERS.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourData tourData = (TourData) cell.getElement();

            final Set<TourMarker> tourMarker = tourData.getTourMarkers();
            final Set<TourWayPoint> wayPoints = tourData.getTourWayPoints();

            if (tourMarker == null && wayPoints == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {

               int size = 0;
               if (tourMarker != null) {
                  size = tourMarker.size();
               }
               if (wayPoints != null) {
                  size += wayPoints.size();
               }
               cell.setText(size == 0 ? UI.EMPTY_STRING : Integer.toString(size));
            }
         }
      });

      _columnId_Marker = colDef.getColumnId();
   }

   /**
    * column: tags
    */
   private void defineColumn_Tour_Tags() {

      final ColumnDefinition colDef = TableColumnFactory.TOUR_TAGS.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInTags == false) {
               return null;
            }

            return ((TourData) cell.getElement()).getTourId();
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TourData tourData = (TourData) element;

            final Set<TourTag> tourTags = tourData.getTourTags();

            if (tourTags.isEmpty()) {

               // the tags could have been removed, set empty field

               cell.setText(UI.EMPTY_STRING);

            } else {

               // convert the tags into a list of tag ids

               cell.setText(TourDatabase.getTagNames(tourTags));
            }
         }
      });
   }

   /**
    * column: tour title
    */
   private void defineColumn_Tour_Title() {

      final ColumnDefinition colDef = TableColumnFactory.TOUR_TITLE.createColumn(_tourViewer_ColumnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);
      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInTitle == false) {
               return null;
            }

            return ((TourData) cell.getElement()).getTourId();
         }

         @Override
         public void update(final ViewerCell cell) {
            final TourData tourData = (TourData) cell.getElement();
            cell.setText(tourData.getTourTitle());
         }
      });

      _columnId_Title = colDef.getColumnId();
   }

   /**
    * Column: Tour type image
    */
   private void defineColumn_Tour_Type() {

      _colDef_TourTypeImage = TableColumnFactory.TOUR_TYPE.createColumn(_tourViewer_ColumnManager, _pc);

      _colDef_TourTypeImage.setIsDefaultColumn();
      _colDef_TourTypeImage.setLabelProvider(new CellLabelProvider() {

         // !!! When using cell.setImage() then it is not centered !!!
         // !!! Set dummy label provider, otherwise an error occures !!!
         @Override
         public void update(final ViewerCell cell) {}
      });
   }

   /**
    * column: tour type text
    */
   private void defineColumn_Tour_TypeText() {

      final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE_TEXT.createColumn(_tourViewer_ColumnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourType tourType = ((TourData) cell.getElement()).getTourType();
            if (tourType == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(tourType.getName());
            }
         }
      });
   }

   /**
    * Column: Cloud image
    */
   private void defineColumn_Weather_Clouds() {

      _colDef_WeatherClouds = TableColumnFactory.WEATHER_CLOUDS.createColumn(_tourViewer_ColumnManager, _pc);

      _colDef_WeatherClouds.setIsDefaultColumn();
      _colDef_WeatherClouds.setLabelProvider(new CellLabelProvider() {

         // !!! When using cell.setImage() then it is not centered !!!
         // !!! Set dummy label provider, otherwise an error occures !!!
         @Override
         public void update(final ViewerCell cell) {}
      });
   }

   private void deleteFile(final ArrayList<String> deletedFiles,
                           final ArrayList<String> notDeletedFiles,
                           final String fileFolder,
                           final String fileName,
                           final TourLogState importLogState) {

      if (fileFolder == null || fileFolder.trim().length() == 0) {
         // there is no folder
         return;
      }

      Path filePath = null;

      try {

         filePath = Paths.get(fileFolder, fileName);
         final String filePathName = filePath.toString();

         Files.delete(filePath);

         deletedFiles.add(filePathName);

         TourLogManager.addSubLog(importLogState, filePathName);

      } catch (final Exception e) {

         // file can be invalid

         final String fileNamePath = '"' + fileFolder + '"' + UI.SPACE + '"' + fileName + '"';
         notDeletedFiles.add(fileNamePath);

         TourLogManager.subLog_ERROR(fileNamePath);
      }
   }

   @Override
   public void dispose() {

      resetEasyImport(false);

      _images.dispose();

      // don't throw the selection again
      _postSelectionProvider.clearSelection();

      getViewSite().getPage().removePartListener(_partListener);
      getSite().getPage().removeSelectionListener(_postSelectionListener);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      disposeConfigImages();

      FileSystemManager.closeFileSystems();

      super.dispose();
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

   public void doLiveUpdate(final DialogEasyImportConfig dialogImportConfig) {

      updateModel_EasyConfig_Dashboard(dialogImportConfig.getModifiedConfig());

      _isDeviceStateValid = false;
      updateUI_2_EasyImport_Fancy();
   }

   private void enableActions() {

      final boolean isTourImported = _rawDataMgr.getImportedTours().values().size() > 0;

      final StructuredSelection selection = (StructuredSelection) _tourViewer.getSelection();

      int numSavedTours = 0;
      int numUnsavedTours = 0;
      int numSelectedTours = 0;

      // contains all tours which are selected and not deleted
      int numSelectedNotDeletedTours = 0;

      TourData firstSavedTour = null;
      TourData firstValidTour = null;

      for (final Object treeItem : selection) {
         if (treeItem instanceof TourData) {

            numSelectedTours++;

            final TourData tourData = (TourData) treeItem;
            if (tourData.getTourPerson() == null) {

               // tour is not saved

               if (tourData.isTourDeleted == false) {

                  // tour is not deleted, deleted tours are ignored

                  numUnsavedTours++;
                  numSelectedNotDeletedTours++;
               }

            } else {

               if (numSavedTours == 0) {
                  firstSavedTour = tourData;
               }

               numSavedTours++;
               numSelectedNotDeletedTours++;
            }

            if (numSelectedNotDeletedTours == 1) {
               firstValidTour = tourData;
            }
         }
      }

      final boolean isSavedTourSelected = numSavedTours > 0;
      final boolean isOneSavedAndNotDeleteTour = numSelectedNotDeletedTours == 1 && numSavedTours == 1;
      final boolean isOneSelectedNotDeleteTour = numSelectedNotDeletedTours == 1;

      final ArrayList<TourType> allTourTypes = TourDatabase.getAllTourTypes();
      final ArrayList<Long> allUsedTagIds = new ArrayList<>();

      long existingTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;
      boolean isOneTourSelected;

      if (firstSavedTour != null && numSavedTours == 1) {

         // one tour is selected

         isOneTourSelected = true;

         final TourType tourType = firstSavedTour.getTourType();
         existingTourTypeId = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId();

         final Set<TourTag> allUsedTags = firstSavedTour.getTourTags();
         if (allUsedTags != null && allUsedTags.size() > 0) {

            // tour contains at least one tag
            for (final TourTag tourTag : allUsedTags) {
               allUsedTagIds.add(tourTag.getTagId());
            }
         }

      } else {

         // multiple tours are selected

         isOneTourSelected = false;
      }

      // action: save tour with person
      final TourPerson person = TourbookPlugin.getActivePerson();
      if (person != null) {
         _actionSaveTourWithPerson.setText(NLS.bind(
               Messages.import_data_action_save_tour_with_person,
               person.getName()));
         _actionSaveTourWithPerson.setPerson(person);
      }
      _actionSaveTourWithPerson.setEnabled((person != null) && (numUnsavedTours > 0));

      // action: save tour...
      if (selection.size() == 1) {
         _actionSaveTour.setText(Messages.import_data_action_save_tour_for_person);
      } else {
         _actionSaveTour.setText(Messages.import_data_action_save_tours_for_person);
      }
      _actionSaveTour.setEnabled(numUnsavedTours > 0);

      // action: merge tour ... into ...
      if (isOneSelectedNotDeleteTour) {

         final StringBuilder sb = new StringBuilder()
               .append(UI.EMPTY_STRING)
               .append(TourManager.getTourDateShort(firstValidTour))
               .append(UI.DASH_WITH_SPACE)
               .append(TourManager.getTourTimeShort(firstValidTour))
               .append(UI.DASH_WITH_SPACE)
               .append(firstValidTour.getDeviceName());

         _actionMergeIntoTour.setText(NLS.bind(Messages.import_data_action_assignMergedTour, sb.toString()));

      } else {
         // tour cannot be merged, display default text
         _actionMergeIntoTour.setText(Messages.import_data_action_assignMergedTour_default);
      }
      _actionMergeIntoTour.setEnabled(isOneSelectedNotDeleteTour);

      _actionMergeTour.setEnabled(isOneSavedAndNotDeleteTour && (firstSavedTour.getMergeSourceTourId() != null));

// SET_FORMATTING_OFF

      _actionDeleteTourFile               .setEnabled(isTourImported);
      _actionEditTour                     .setEnabled(isOneSavedAndNotDeleteTour);
      _actionEditQuick                    .setEnabled(isOneSavedAndNotDeleteTour);
      _actionExportTour                   .setEnabled(numSelectedNotDeletedTours > 0);
      _actionJoinTours                    .setEnabled(numSelectedNotDeletedTours > 1);
      _actionOpenTour                     .setEnabled(isOneSavedAndNotDeleteTour);
      _actionOpenMarkerDialog             .setEnabled(isOneSavedAndNotDeleteTour);
      _actionOpenAdjustAltitudeDialog     .setEnabled(isOneSavedAndNotDeleteTour);
      _actionReimportTours                .setEnabled(numSelectedTours > 0);
      _actionRemoveTour                   .setEnabled(numSelectedTours > 0);
      _actionUploadTour                   .setEnabled(numSelectedNotDeletedTours > 0);

      // import setup
      _actionClearView                    .setEnabled(isTourImported);
      _actionSetupImport                  .setEnabled(isTourImported == false);
      _actionToggleImportUI               .setEnabled(isTourImported == false);

      // actions for tags/tour types
      _actionSetTourType.setEnabled(isSavedTourSelected && (allTourTypes.size() > 0));
      _tagMenuManager.enableTagActions(isSavedTourSelected, isOneTourSelected, allUsedTagIds);
      _tourTypeMenuManager.enableTourTypeActions(isSavedTourSelected, existingTourTypeId);

      // set double click state
      _tourDoubleClickState.canEditTour         = isOneSavedAndNotDeleteTour;
      _tourDoubleClickState.canQuickEditTour    = isOneSavedAndNotDeleteTour;
      _tourDoubleClickState.canEditMarker       = isOneSavedAndNotDeleteTour;
      _tourDoubleClickState.canAdjustAltitude   = isOneSavedAndNotDeleteTour;
      _tourDoubleClickState.canOpenTour         = isOneSelectedNotDeleteTour;

// SET_FORMATTING_ON
   }

   private void enableSimpleUI(final boolean isEnabled) {

      if (_parent.isDisposed()) {
         return;
      }

      final Table viewerTable = _simpleUI_ImportLauncher_Viewer.getTable();

// SET_FORMATTING_OFF

      _actionSimpleUI_DeviceState      .setEnabled(isEnabled);
      _comboSimpleUI_Config            .setEnabled(isEnabled);
      _lblSimpleUI_EasyImportTitle     .setEnabled(isEnabled);
      _lblSimpleUI_NumNotImportedFiles .setEnabled(isEnabled);

      viewerTable                      .setEnabled(isEnabled);

// SET_FORMATTING_ON

      if (UI.IS_DARK_THEME) {

         viewerTable.setHeaderForeground(isEnabled
               ? ThemeUtil.getDefaultForegroundColor_TableHeader()
               : ThemeUtil.getDefaultBackgroundColor_Combo());

         viewerTable.setHeaderBackground(isEnabled
               ? ThemeUtil.getDefaultBackgroundColor_TableHeader()
               : ThemeUtil.getDefaultBackgroundColor_Table());

      } else {

         // bright theme

         viewerTable.setHeaderForeground(isEnabled
               ? UI.SYS_COLOR_WIDGET_FOREGROUND
               : UI.SYS_COLOR_WIDGET_DARK_SHADOW);

         viewerTable.setHeaderBackground(UI.SYS_COLOR_WIDGET_BACKGROUND);

         _topPage_ImportUI_EasyImport_Simple.setBackground(isEnabled
               ? UI.SYS_COLOR_LIST_BACKGROUND
               : UI.SYS_COLOR_WIDGET_BACKGROUND);
      }
   }

   private void fillSimpleUI() {

      final EasyConfig easyConfig = getEasyConfig();

      for (final ImportConfig importConfig : easyConfig.importConfigs) {
         _comboSimpleUI_Config.add(importConfig.name);
      }
   }

   private void fillSimpleUI_ContextMenu(final IMenuManager menuManager) {

      // update import action text
      final Object firstElement = _simpleUI_ImportLauncher_Viewer.getStructuredSelection().getFirstElement();
      if (firstElement instanceof ImportLauncher) {

         _actionSimpleUI_StartEasyImport.setText(NLS.bind(

               Messages.Import_Data_Action_StartEasyImport,
               ((ImportLauncher) firstElement).name));
      }

      menuManager.add(_actionSimpleUI_StartEasyImport);
   }

   private void fillToolbar() {

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionSaveTourWithPerson);
      tbm.add(_actionSaveTour);
      tbm.add(new Separator());

      // place for import and transfer actions
      tbm.add(new GroupMarker("import")); //$NON-NLS-1$
      tbm.add(new Separator());

      tbm.add(_actionToggleImportUI);
      tbm.add(_actionClearView);
      tbm.add(_actionOpenTourLogView);
      tbm.add(_actionSetupImport);

      /*
       * fill view menu
       */
      final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

      menuMgr.add(_actionRemoveToursWhenClosed);
      menuMgr.add(_actionEditImportPreferences);

   }

   private void fillTourViewer_ContextMenu(final IMenuManager menuMgr) {

      // hide tour info tooltip, this is displayed when the mouse context menu should be created
      _tourInfoToolTip.hide();

      if (TourbookPlugin.getActivePerson() != null) {
         menuMgr.add(_actionSaveTourWithPerson);
      }
      menuMgr.add(_actionSaveTour);
      menuMgr.add(_actionMergeIntoTour);

      // export actions
      menuMgr.add(new Separator());
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EXPORT, _allTourActions_Export, this);

      menuMgr.add(_actionReimportTours);
      menuMgr.add(_actionEditImportPreferences);
      menuMgr.add(_actionRemoveTour);
      menuMgr.add(_actionDeleteTourFile);

      // edit actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EDIT, _allTourActions_Edit, this);

      // tour type actions
      _tourTypeMenuManager.fillContextMenu_WithActiveActions(menuMgr, this);

      // tour tag actions
      _tagMenuManager.fillTagMenu_WithActiveActions(menuMgr, this);

      // add standard group which allows other plug-ins to contribute here
      menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

      // customize this context menu
      TourActionManager.fillContextMenu_CustomizeAction(menuMgr)

            // set pref page custom data that actions from this view can be identified
            .setPrefData(new ViewContext(ID, ViewNames.VIEW_NAME_TOUR_IMPORT));

      enableActions();
   }

   private void fillUI() {

      fillSimpleUI();
   }

   private void fireSelectedTour() {

      if (_parent != null && _parent.isDisposed()) {

         // this happens when import view is closed from setFocus() method

         return;
      }

      final IStructuredSelection selection = (IStructuredSelection) _tourViewer.getSelection();
      final TourData tourData = (TourData) selection.getFirstElement();

      enableActions();

      if (tourData != null) {
         _postSelectionProvider.setSelection(new SelectionTourData(null, tourData));
      }
   }

   private int getActiveEasyConfigSelectionIndex() {

      final EasyConfig easyConfig = getEasyConfig();

      final ImportConfig activeEasyConfig = easyConfig.getActiveImportConfig();
      final ArrayList<ImportConfig> allConfigs = easyConfig.importConfigs;

      for (int configIndex = 0; configIndex < allConfigs.size(); configIndex++) {

         if (allConfigs.get(configIndex) == activeEasyConfig) {

            return configIndex;
         }
      }

      return 0;
   }

   @Override
   public ArrayList<TourData> getAllSelectedTours() {

      final TourManager tourManager = TourManager.getInstance();

      // get selected tours
      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

      final ArrayList<TourData> selectedTourData = new ArrayList<>();

      // loop: all selected tours
      for (final Object tourItem : selectedTours) {

         if (tourItem instanceof TourData) {

            final TourData tourData = (TourData) tourItem;

            if (tourData.isTourDeleted) {
               // skip deleted tour
               continue;
            }

            if (tourData.getTourPerson() == null) {

               // tour is not saved
               selectedTourData.add(tourData);

            } else {

               /*
                * get the data from the database because the tag names could be changed and this is
                * not reflected in the tours which are displayed in the raw data view
                */

               final TourData tourDataInDb = tourManager.getTourData(tourData.getTourId());

               if (tourDataInDb != null) {
                  selectedTourData.add(tourDataInDb);
               }
            }
         }
      }

      return selectedTourData;
   }

   private ArrayList<Long> getAllTourIds(final ArrayList<TourData> allTourData) {

      final ArrayList<Long> allTourIds = new ArrayList<>(allTourData.size());

      for (final TourData tourData : allTourData) {
         allTourIds.add(tourData.getTourId());
      }

      return allTourIds;
   }

   /**
    * @return Returns all tours which are selected in the import view
    */
   private ArrayList<TourData> getAnySelectedTours() {

      final ArrayList<TourData> selectedTours = new ArrayList<>();

      // get selected tours, this must be outside of the runnable !!!
      final IStructuredSelection selection = ((IStructuredSelection) _tourViewer.getSelection());

      for (final Object name : selection) {
         selectedTours.add((TourData) name);
      }

      return selectedTours;
   }

   @Override
   public ColumnManager getColumnManager() {
      return _tourViewer_ColumnManager;
   }

   private String getDurationText(final ImportLauncher importLauncher) {

      final int duration = importLauncher.temperatureAdjustmentDuration;
      final Period durationPeriod = new Period(0, duration * 1000L, _durationTemplate);

      return durationPeriod.toString(UI.DEFAULT_DURATION_FORMATTER);
   }

   EasyConfig getEasyConfig() {

      return EasyImportManager.getInstance().getEasyConfig();
   }

   public ImageRegistry getImages() {

      return _images;
   }

   public Image getImportConfigImage(final ImportLauncher importConfig, final boolean isDarkTransparentColor) {

      final int configWidth = importConfig.imageWidth;

      int configWidthScaled;
      int imageSizeScaled;

      if (UI.IS_WIN) {

         configWidthScaled = (int) (configWidth * UI.HIDPI_SCALING * UI.HIDPI_SCALING);

         imageSizeScaled = (int) (TourType.TOUR_TYPE_IMAGE_SIZE * UI.HIDPI_SCALING * UI.HIDPI_SCALING);

      } else {

         configWidthScaled = (int) (configWidth * UI.HIDPI_SCALING);

         imageSizeScaled = (int) (TourType.TOUR_TYPE_IMAGE_SIZE * UI.HIDPI_SCALING);
      }

      if (configWidthScaled == 0) {
         return null;
      }

      final long configId = importConfig.getId();
      final String configImageId = Long.toString(configId) + UI.SPACE + Boolean.toString(isDarkTransparentColor);

      Image configImage = _configImages.get(configImageId);

      if (isConfigImageValid(configImage, importConfig, configImageId)) {
         return configImage;
      }

      /*
       * Config image is not yet created
       */

      final Display display = _parent.getDisplay();

      final Enum<TourTypeConfig> tourTypeConfig = importConfig.tourTypeConfig;

      if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

         // draw multiple tour types in one image

         final ArrayList<SpeedTourType> allSpeedVertices = importConfig.speedTourTypes;

         final ImageData swtImageData = new ImageData(
               configWidthScaled,
               imageSizeScaled,
               24,
               new PaletteData(0xFF0000, 0xFF00, 0xFF));

         swtImageData.alphaData = new byte[configWidthScaled * imageSizeScaled];

         final Image tempImage = new Image(display, new NoAutoScalingImageDataProvider(swtImageData));
         {
            final GC gcTempImage = new GC(tempImage);
            {
//               gcTempImage.setBackground(UI.SYS_COLOR_GREEN);
//               gcTempImage.fillRectangle(tempImage.getBounds());

               for (int speedIndex = 0; speedIndex < allSpeedVertices.size(); speedIndex++) {

                  final SpeedTourType vertex = allSpeedVertices.get(speedIndex);

                  final Image ttImage = TourTypeImage.getTourTypeImage(vertex.tourTypeId);

                  final float devX = TourType.TOUR_TYPE_IMAGE_SIZE * speedIndex;

                  gcTempImage.drawImage(ttImage, (int) devX, 0);
               }
            }
            gcTempImage.dispose();

            ImageData tempImageData;

            if (UI.IS_WIN) {

               tempImageData = tempImage.getImageData();

            } else {

               // convert into image data, the trick is to use the device zoom otherwise it is not working !!!
               tempImageData = tempImage.getImageData(DPIUtil.getDeviceZoom());
            }

            configImage = new Image(display, new NoAutoScalingImageDataProvider(tempImageData));
         }
         tempImage.dispose();

      } else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {

         // draw one tour type in an image

         final TourType tourType = importConfig.oneTourType;

         if (tourType != null) {

            final ImageData swtImageData = new ImageData(
                  configWidthScaled,
                  imageSizeScaled,
                  24,
                  new PaletteData(0xFF0000, 0xFF00, 0xFF));

            swtImageData.alphaData = new byte[configWidthScaled * imageSizeScaled];

            final Image tempImage = new Image(display, new NoAutoScalingImageDataProvider(swtImageData));
            {
               /*
                * Paint tour type image into the displayed image
                */

               final GC gcTempImage = new GC(tempImage);
               {
//                  gcTempImage.setBackground(UI.SYS_COLOR_GREEN);
//                  gcTempImage.fillRectangle(tempImage.getBounds());

                  final Image ttImage = TourTypeImage.getTourTypeImage(tourType.getTypeId());
                  gcTempImage.drawImage(ttImage, 0, 0);
               }
               gcTempImage.dispose();

               ImageData tempImageData;

               if (UI.IS_WIN) {

                  tempImageData = tempImage.getImageData();

               } else {

                  // convert into image data, the trick is to use the device zoom otherwise it is not working !!!
                  tempImageData = tempImage.getImageData(DPIUtil.getDeviceZoom());
               }

               configImage = new Image(display, new NoAutoScalingImageDataProvider(tempImageData));

            }
            tempImage.dispose();
         }

      } else {

         // this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED
      }

      // keep image in the cache
      final Image oldImage = _configImages.put(configImageId, configImage);

      UI.disposeResource(oldImage);

      _configImageHash.put(configImageId, importConfig.imageHash);

      return configImage;
   }

   @Override
   public PostSelectionProvider getPostSelectionProvider() {
      return _postSelectionProvider;
   }

   @Override
   public Set<Long> getSelectedTourIDs() {

      final LinkedHashSet<Long> tourIds = new LinkedHashSet<>();

      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());
      for (final Object viewItem : selectedTours) {

         if (viewItem instanceof TourData) {
            tourIds.add(((TourData) viewItem).getTourId());
         }
      }

      return tourIds;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final TourManager tourManager = TourManager.getInstance();

      // get selected tours
      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

      final ArrayList<TourData> selectedTourData = new ArrayList<>();

      // loop: all selected tours
      for (final Object tourItem : selectedTours) {

         if (tourItem instanceof TourData) {

            final TourData tourData = (TourData) tourItem;

            /*
             * only tours are added which are saved in the database
             */
            if (tourData.getTourPerson() != null) {

               /*
                * get the data from the database because the tag names could be changed and this is
                * not reflected in the tours which are displayed in the raw data view
                */
               final TourData tourDataInDb = tourManager.getTourData(tourData.getTourId());
               if (tourDataInDb != null) {
                  selectedTourData.add(tourDataInDb);
               }
            }
         }
      }

      return selectedTourData;
   }

   /**
    * @param sortColumnId
    *
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _tourViewer.getTable().getColumns();

      for (final TableColumn column : allColumns) {

         final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

         if (columnId.equals(sortColumnId)) {
            return column;
         }
      }

      return allColumns[0];
   }

   Image getStateImage_Db(final TourData tourData) {

      final TourPerson tourPerson = tourData.getTourPerson();
      final long activePersonId = _activePerson == null ? -1 : _activePerson.getPersonId();

      if (tourData.isTourDeleted) {

         return _images.get(IMAGE_DELETE);

      } else if (tourData.getMergeTargetTourId() != null) {

         return _images.get(IMAGE_ASSIGN_MERGED_TOUR);

      } else if (tourPerson == null) {

         return _images.get(IMAGE_ICON_PLACEHOLDER);

      } else if (tourPerson.getPersonId() == activePersonId) {

         return _images.get(IMAGE_DATABASE);

      } else {

         return _images.get(IMAGE_DATABASE_OTHER_PERSON);
      }
   }

   private Image getStateImage_Import(final TourData tourData) {

      if (tourData.isTourFileDeleted) {

         return _images.get(IMAGE_STATE_DELETE);

      } else if (tourData.isTourFileMoved) {

         return _images.get(IMAGE_STATE_MOVED);
      }

      return null;
   }

   @Override
   public ColumnViewer getViewer() {
      return _tourViewer;
   }

   private void initUI(final Composite parent) {

      _parent = parent;
      _pc = new PixelConverter(parent);

      createResources_Image();
      createResources_Web();

      _tourViewer_Comparator = new TourViewer_Comparator();
      _columnSortListener = widgetSelectedAdapter(event -> onSelect_SortColumn(event));
   }

   /**
    * @param image
    * @param importConfig
    * @param configImageId
    *
    * @return Returns <code>true</code> when the image is valid, returns <code>false</code> when the
    *         profile image must be created,
    */
   private boolean isConfigImageValid(final Image image,
                                      final ImportLauncher importConfig,
                                      final String configImageId) {

      if (image == null || image.isDisposed()) {

         return false;
      }

      final Integer imageHash = _configImageHash.get(configImageId);

      if (imageHash == null || imageHash != importConfig.imageHash) {

         image.dispose();

         return false;
      }

      return true;
   }

   boolean isOSFolderValid(final String osFolder) {

      try {

         if (osFolder != null && osFolder.trim().length() > 0) {

            final Path folderPath = NIO.getDeviceFolderPath(osFolder);

            // device folder exists
            return folderPath != null && Files.exists(folderPath);
         }

      } catch (final Exception e) {}

      return false;
   }

   /**
    * @return Returns <code>true</code> when the devices are watched.
    */
   private boolean isWatchingOn() {

      return _watchingStoresThread != null;
   }

   private void onBrowser_Completed() {

      _isBrowserCompleted = true;

      if (_isInFancyUIStartup) {

         _isInFancyUIStartup = false;

         // a redraw MUST be done otherwise nothing is displayed
         _browser.setRedraw(true);

         // set focus that clicking on an action works the 1st and not the 2nd time
         _browser.setFocus();

         // dashboard is visible, activate background task
         setWatcher_1_On();

         // make the import tiles visible otherwise they are 'hidden' after the startup
         _isShowWatcherAnimation = true;
         updateUI_WatcherAnimation(isWatchingOn()
               ? DOM_CLASS_DEVICE_ON_ANIMATED
               : DOM_CLASS_DEVICE_OFF_ANIMATED);
      }

      if (_isDeviceStateUpdateDelayed.getAndSet(false)) {
         updateUI_DeviceState_DOM();
      }
   }

   private void onBrowser_LocationChanging(final LocationEvent event) {

      final String location = event.location;

      final String[] locationParts = location.split(HREF_TOKEN);

      if (locationParts.length > 1) {

         // finalize loading of the browser page and then start the action
         _browser.getDisplay().asyncExec(() -> onBrowser_LocationChanging_Runnable(locationParts));
      }

      // prevent to load a new url
      if (location.equals(PAGE_ABOUT_BLANK) == false) {

         // about:blank is the initial page

         event.doit = false;
      }
   }

   private void onBrowser_LocationChanging_Runnable(final String[] locationParts) {

      final String hrefAction = locationParts[1];

      if (ACTION_DEVICE_IMPORT.equals(hrefAction)) {

         final long tileId = Long.parseLong(locationParts[2]);

         runEasyImport(tileId);

      } else if (ACTION_SETUP_EASY_IMPORT.equals(hrefAction)) {

         onSelect_SetupEasyImport(-1);

      } else if (ACTION_DEVICE_WATCHING_ON_OFF.equals(hrefAction)) {

         onSelect_DeviceWatching_OnOff();

      } else if (ACTION_IMPORT_FROM_FILES.equals(hrefAction)) {

         _rawDataMgr.actionImportFromFile();

      } else if (ACTION_SERIAL_PORT_CONFIGURED.equals(hrefAction)) {

         _rawDataMgr.actionImportFromDevice();

      } else if (ACTION_SERIAL_PORT_DIRECTLY.equals(hrefAction)) {

         _rawDataMgr.actionImportFromDeviceDirect();

      } else if (ACTION_OLD_UI.equals(hrefAction)) {

         onSelect_ImportUI(ImportUI.FOSSIL);

      } else {

         //We look for the cloud downloader that matches
         //the action in {@link hrefAction} and execute its
         //{@link TourbookCloudDownloader#downloadTours} method.
         _cloudDownloadersList.stream()
               .filter(cd -> cd.getId().equals(hrefAction))
               .forEach(TourbookCloudDownloader::downloadTours);
      }
   }

   private void onPaint_SimpleUIViewer(final Event event) {

      if (event.index != _simpleUI_ColumnIndexConfigImage) {
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
//         event.height = PROFILE_IMAGE_HEIGHT;

         break;

      case SWT.PaintItem:

         final Image image = getImportConfigImage(importLauncher, UI.IS_DARK_THEME);

         if (image != null && !image.isDisposed()) {

            final Rectangle rect = image.getBounds();

            final int x = event.x + event.width;
            final int yOffset = Math.max(0, (event.height - rect.height) / 2);

            event.gc.drawImage(image, x, event.y + yOffset);
         }

         break;
      }
   }

   private void onPaint_TableViewer(final Event event) {

      // paint column image

      final int columnIndex = event.index;

      if (columnIndex == _columnIndex_TourTypeImage) {

         onPaint_TableViewer_TourTypeImage(event);

      } else if (columnIndex == _columnIndex_WeatherClouds) {

         onPaint_TableViewer_WeatherClouds(event);
      }
   }

   private void onPaint_TableViewer_TourTypeImage(final Event event) {

      final Object itemData = event.item.getData();

      if (itemData instanceof TourData) {

         final TourData tourData = (TourData) itemData;
         final TourType tourType = tourData.getTourType();

         if (tourType != null) {

            final long tourTypeId = tourType.getTypeId();
            final Image image = TourTypeImage.getTourTypeImage(tourTypeId);

            if (image != null) {

               UI.paintImage(

                     event,
                     image,
                     _columnWidth_TourTypeImage,
                     _colDef_TourTypeImage.getColumnStyle(), // horizontal alignment
                     TourTypeImage.getHorizontalOffset());
            }
         }
      }
   }

   private void onPaint_TableViewer_WeatherClouds(final Event event) {

      final Object itemData = event.item.getData();

      if (itemData instanceof TourData) {

         final TourData tourData = (TourData) itemData;

         final String weatherClouds = tourData.getWeather_Clouds();
         if (weatherClouds == null) {

            // paint nothing

         } else {

            final Image image = UI.IMAGE_REGISTRY.get(weatherClouds);

            if (image == null) {

               // paint text left aligned

               event.gc.drawText(weatherClouds, event.x, event.y, false);

            } else {

               final int alignment = _colDef_WeatherClouds.getColumnStyle();

               UI.paintImage(event, image, _columnWidth_WeatherClouds, alignment, 0);
            }
         }
      }
   }

   private void onResize_SetWidthForImageColumn() {

      if (_colDef_TourTypeImage != null) {

         final TableColumn tableColumn = _colDef_TourTypeImage.getTableColumn();

         if (tableColumn != null && tableColumn.isDisposed() == false) {

            _columnWidth_TourTypeImage = tableColumn.getWidth();
         }
      }

      if (_colDef_WeatherClouds != null) {

         final TableColumn tableColumn = _colDef_WeatherClouds.getTableColumn();

         if (tableColumn != null && tableColumn.isDisposed() == false) {

            _columnWidth_WeatherClouds = tableColumn.getWidth();
         }
      }
   }

   private void onSelect_DeviceWatching_OnOff() {

      if (_importUI == ImportUI.EASY_IMPORT_FANCY) {

         _isShowWatcherAnimation = true;
      }

      if (isWatchingOn()) {

         // stop watching

         setWatcher_2_Off();

      } else {

         // start watching

         setWatcher_1_On();
      }

      updateUI_DeviceState();
   }

   /**
    * Import config is selected in the dashboard.
    *
    * @param selectedIndex
    */
   private void onSelect_ImportConfig_Fancy(final int selectedIndex) {

      final EasyConfig easyConfig = getEasyConfig();

      final ImportConfig selectedConfig = easyConfig.importConfigs.get(selectedIndex);

      setWatcher_2_Off();
      {
         easyConfig.setActiveImportConfig(selectedConfig);

         _isDeviceStateValid = false;

         updateUI_2_EasyImport_Fancy();
      }
      setWatcher_1_On();

      updateUI_2_EasyImport_Fancy();

      // update also the simple UI to be in sync with the fancy UI
      updateUI_2_EasyImport_Simple();
   }

   /**
    * Import config is selected in the simple UI.
    */
   private void onSelect_ImportConfig_SimpleUI() {

      /**
       * When the import config selection is done too frequently, it can block the UI because of the
       * different threads/watcher
       * <p>
       * -> delay UI
       */
      final long now = TimeTools.nowInMilliseconds();

      // check if selection is delayed
      if (now < _lastSimpleUIConfigSelection + 1000) {

         // reselect last config

         _comboSimpleUI_Config.getDisplay().asyncExec(() -> {

            final int activeEasyConfigSelectionIndex = getActiveEasyConfigSelectionIndex();

            _comboSimpleUI_Config.select(activeEasyConfigSelectionIndex);
         });

         return;
      }

      _lastSimpleUIConfigSelection = now;

      final EasyConfig easyConfig = getEasyConfig();
      final ImportConfig activeEasyConfig = easyConfig.getActiveImportConfig();

      final int selectionIndex = Math.max(0, _comboSimpleUI_Config.getSelectionIndex()); // get valid index
      final ImportConfig selectedConfig = easyConfig.importConfigs.get(selectionIndex);

      if (selectedConfig == activeEasyConfig) {

         // ignore the same config

         return;
      }

      setWatcher_2_Off();
      {
         easyConfig.setActiveImportConfig(selectedConfig);

         _isDeviceStateValid = false;

         // close slideout to remove old content
         _actionSimpleUI_DeviceState.__slideoutDeviceState.close();
      }
      setWatcher_1_On();
   }

   private void onSelect_ImportUI(final ImportUI importUI) {

      _importUI = importUI;

      if (importUI == ImportUI.EASY_IMPORT_FANCY) {

         updateUI_1_TopPage(true);

         showFailbackUI();

      } else {

         resetEasyImport(true);

         updateUI_1_TopPage(true);
      }

      updateUI_ImportUI_Action();
   }

   /**
    * @param selectedTab
    *           Tab which should be selected when config dialog is opened, -1 will select the
    *           restored tab index.
    */
   void onSelect_SetupEasyImport(final int selectedTab) {

      // prevent that the dialog is opened multiple times, this occurred when testing
      if (_dialogImportConfig != null) {
         return;
      }

      final Shell shell = Display.getDefault().getActiveShell();

      final EasyConfig easyConfig = getEasyConfig();

      _dialogImportConfig = new DialogEasyImportConfig(shell, easyConfig, this, selectedTab);

      if (_dialogImportConfig.open() == Window.OK) {

         // keep none live update values

         final EasyConfig modifiedEasyConfig = _dialogImportConfig.getModifiedConfig();

         easyConfig.setActiveImportConfig(modifiedEasyConfig.getActiveImportConfig());

         easyConfig.importConfigs.clear();
         easyConfig.importConfigs.addAll(modifiedEasyConfig.importConfigs);

         easyConfig.importLaunchers.clear();
         easyConfig.importLaunchers.addAll(modifiedEasyConfig.importLaunchers);

         updateModel_EasyConfig_Dashboard(modifiedEasyConfig);

         _isDeviceStateValid = false;

         updateUI_2_EasyImport_Fancy();
         updateUI_2_EasyImport_Simple();

         _simpleUI_ImportLauncher_Viewer.refresh();
      }

      _dialogImportConfig = null;
   }

   private void onSelect_SortColumn(final SelectionEvent e) {

      _tourViewer_Container.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = _tourViewer.getSelection();

         // update viewer with new sorting
         _tourViewer_Comparator.setSortColumn(e.widget);
         _tourViewer.refresh();

         // reselect
         _isInUpdate = true;
         {
            _tourViewer.setSelection(selectionBackup, true);
            _tourViewer.getTable().showSelection();
         }
         _isInUpdate = false;
      }
      _tourViewer_Container.setRedraw(true);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (selection instanceof SelectionDeletedTours) {

         // tours are deleted

         _postSelectionProvider.clearSelection();

         final SelectionDeletedTours tourSelection = (SelectionDeletedTours) selection;
         final ArrayList<ITourItem> removedTours = tourSelection.removedTours;

         if (removedTours.isEmpty()) {
            return;
         }

         removeTours(removedTours);

         if (_isPartVisible) {

            _rawDataMgr.updateTourData_InImportView_FromDb(null);

            // force the store watcher to update the device state
            _isDeviceStateValid = false;
//            thread_FolderWatcher_Activate();

            // update the table viewer
            reloadViewer();

         } else {

            _isViewerPersonDataDirty = true;
         }

         // it's important to update this state otherwise deleted tours are not counted !!!
         updateUI_DeviceState();
      }
   }

   private void recreateViewer() {

      _tourViewer_ColumnManager.saveState(_state);
      _tourViewer_ColumnManager.clearColumns();
      defineAllColumns();

      _tourViewer = (TableViewer) recreateViewer(_tourViewer);

      _isDeviceStateValid = false;
      updateUI_2_EasyImport_Fancy();
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _topPage_ImportViewer.setRedraw(false);
      {
         _tourViewer.getTable().dispose();
         createUI_92_TourViewer(_topPage_ImportViewer);
         _topPage_ImportViewer.layout();

         // update the viewer
         reloadViewer();
      }
      _topPage_ImportViewer.setRedraw(true);

      return _tourViewer;
   }

   /**
    * Update {@link TourData} from the database for all imported tours, displays a progress dialog.
    *
    * @param canCancelProcess
    */
   private void reimportAllImportFiles(final boolean canCancelProcess) {

      final String[] prevImportedFiles = _state.getArray(STATE_IMPORTED_FILENAMES);
      if ((prevImportedFiles == null) || (prevImportedFiles.length == 0)) {
         return;
      }

      // Log re-import
      TourLogManager.addLog(
            TourLogState.DEFAULT,
            Messages.Log_Reimport_PreviousFiles,
            TourLogView.CSS_LOG_TITLE);

      final long start = System.currentTimeMillis();

      TourLogManager.showLogView(AutoOpenEvent.TOUR_IMPORT);

      try {
         new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(

               true,
               canCancelProcess,

               monitor -> {

                  final ImportState_Process importState_Process = new ImportState_Process();

                  reimportAllImportFiles_Runnable(
                        monitor,
                        prevImportedFiles,
                        canCancelProcess,
                        importState_Process);

                  // fix: org.eclipse.swt.SWTException: Invalid thread access
                  _parent.getDisplay().syncExec(() -> importState_Process.runPostProcess());
               });

      } catch (final Exception e) {

         TourLogManager.log_EXCEPTION_WithStacktrace(e);
         Thread.currentThread().interrupt();

      } finally {

         TourLogManager.log_DEFAULT(String.format(
               Messages.Log_Reimport_PreviousFiles_End,
               (System.currentTimeMillis() - start) / 1000.0));
      }
   }

   /**
    * re-import previous imported tours
    *
    * @param monitor
    * @param importedFiles
    * @param canCancelProcess
    * @param importState_Process
    */
   private void reimportAllImportFiles_Runnable(final IProgressMonitor monitor,
                                                final String[] importedFiles,
                                                final boolean canCancelProcess,
                                                final ImportState_Process importState_Process) {

      int workedDone = 0;
      final int workedAll = importedFiles.length;

      if (monitor != null) {
         monitor.beginTask(Messages.import_data_importTours_task, workedAll);
      }

      final ArrayList<String> notImportedFiles = new ArrayList<>();

      _rawDataMgr.getImportedTours().clear();

      int numImportedFiles = 0;

      // loop: import all files
      for (final String fileName : importedFiles) {

         if (monitor != null) {
            monitor.worked(1);
            monitor.subTask(NLS.bind(Messages.import_data_importTours_subTask,
                  new Object[] { workedDone++, workedAll, fileName }));
         }

         final File file = new File(fileName);
         if (file.exists()) {

            final ImportState_File importState_File = _rawDataMgr.importTours_FromOneFile(

                  file, //                         importFile
                  null, //                         destinationPath
                  null, //                         fileCollision
                  false, //                        isBuildNewFileNames
                  true, //                         isTourDisplayedInImportView
                  new HashMap<>(),
                  importState_Process //
            );

            if (importState_File.isFileImportedWithValidData) {

               TourLogManager.subLog_OK(fileName);
               numImportedFiles++;

            } else {

               if (importState_File.isImportLogged == false) {

                  // do default logging

                  TourLogManager.subLog_ERROR(fileName);
               }

               notImportedFiles.add(fileName);
            }
         }

         if (canCancelProcess && monitor.isCanceled()) {

            // stop importing but process imported tours

            break;
         }
      }

      if (numImportedFiles > 0) {

         _rawDataMgr.updateTourData_InImportView_FromDb(monitor);

         Display.getDefault().asyncExec(() -> {

            reloadViewer();

            /*
             * Restore selected tour
             */
            final String[] viewerIndices = _state.getArray(STATE_SELECTED_TOUR_INDICES);

            if (viewerIndices != null) {

               final ArrayList<Object> viewerTourData = new ArrayList<>();

               for (final String viewerIndex : viewerIndices) {

                  Object tourData = null;

                  try {
                     final int index = Integer.parseInt(viewerIndex);
                     tourData = _tourViewer.getElementAt(index);
                  } catch (final NumberFormatException e) {
                     // just ignore
                  }

                  if (tourData != null) {
                     viewerTourData.add(tourData);
                  }
               }

               if (viewerTourData.size() > 0) {
                  _tourViewer.setSelection(new StructuredSelection(viewerTourData.toArray()), true);
               }
            }
         });
      }

      // show error log
      if (notImportedFiles.size() > 0) {
         TourLogManager.showLogView(AutoOpenEvent.TOUR_IMPORT);
      }
   }

   /**
    * This will also activate/deactivate the folder/store watcher.
    *
    * @see net.tourbook.common.util.ITourViewer#reloadViewer()
    */
   @Override
   public void reloadViewer() {

      updateUI_1_TopPage(false);

      // update tour viewer
      final Object[] rawData = _rawDataMgr.getImportedTours().values().toArray();
      _tourViewer.setInput(rawData);

      enableActions();
   }

   private void removeTours(final ArrayList<ITourItem> removedTours) {

      final Map<Long, TourData> tourMap = _rawDataMgr.getImportedTours();

      for (final ITourItem tourItem : removedTours) {

         final TourData tourData = tourMap.get(tourItem.getTourId());
         if (tourData != null) {

            // when a tour was deleted the person in the tour data must be removed
            tourData.setTourPerson(null);

            // remove tour properties
            tourData.setTourType(null);
            tourData.setTourTitle(UI.EMPTY_STRING);
            tourData.setTourTags(new HashSet<>());

            /**
             * when a remove tour is saved again, this will cause the exception: <br>
             * detached entity passed to persist: net.tourbook.data.TourMarker<br>
             * I didn't find a workaround, so this tour cannot be saved again until it is reloaded
             * from the file
             */
            tourData.isTourDeleted = true;
         }
      }
   }

   private void resetEasyImport(final boolean isUpdateUI) {

      setWatcher_2_Off(isUpdateUI);

      EasyImportManager.getInstance().reset();
   }

   private void restoreState() {

      _importUI = (ImportUI) Util.getStateEnum(_state, STATE_IMPORT_UI, STATE_IMPORT_UI_DEFAULT);

      _actionRemoveToursWhenClosed.setChecked(Util.getStateBoolean(_state,
            STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED,
            true));

      // restore: set merge tracks status before the tours are imported
      final boolean isMergeTracks = _state.getBoolean(STATE_IS_MERGE_TRACKS);
      _rawDataMgr.setMergeTracks(isMergeTracks);

      // restore: set merge tracks status before the tours are imported
      final boolean isCreateTourIdWithTime = _state.getBoolean(STATE_IS_CREATE_TOUR_ID_WITH_TIME);
      _rawDataMgr.setState_CreateTourIdWithTime(isCreateTourIdWithTime);

      // restore: set ignore invalid files status before the tours are imported
      final boolean isIgnoreInvalidFile = _state.getBoolean(STATE_IS_IGNORE_INVALID_FILE);
      _rawDataMgr.setState_IsIgnoreInvalidFile(isIgnoreInvalidFile);

      // restore: set body weight status before the tours are imported
      final boolean isSetBodyWeight = Util.getStateBoolean(_state, STATE_IS_SET_BODY_WEIGHT, STATE_IS_SET_BODY_WEIGHT_DEFAULT);
      _rawDataMgr.setState_IsSetBodyWeight(isSetBodyWeight);

      final CadenceMultiplier defaultCadenceMultiplier = (CadenceMultiplier) Util.getStateEnum(_state,
            STATE_DEFAULT_CADENCE_MULTIPLIER,
            STATE_DEFAULT_CADENCE_MULTIPLIER_DEFAULT);
      _rawDataMgr.setState_DefaultCadenceMultiplier(defaultCadenceMultiplier);

      // restore: is checksum validation
      final boolean isValidation = _state.getBoolean(STATE_IS_CHECKSUM_VALIDATION);
      _rawDataMgr.setIsHAC4_5_ChecksumValidation(isValidation);

      updateToolTipState();
      updateUI_ImportUI_Action();

      // simple easy import
      _comboSimpleUI_Config.select(getActiveEasyConfigSelectionIndex());
      _simpleUI_ImportLauncher_Viewer.setInput(new Object());

      Display.getCurrent().asyncExec(() -> reimportAllImportFiles(true));
   }

   /**
    * Launch easy import from the simple UI
    */
   private void runEasyImport() {

      // get selected launcher
      final Object firstElement = _simpleUI_ImportLauncher_Viewer.getStructuredSelection().getFirstElement();
      if (firstElement instanceof ImportLauncher) {

         /*
          * Log import start
          */
         TourLogManager.addLog(
               TourLogState.DEFAULT,
               EasyImportManager.LOG_EASY_IMPORT_000_IMPORT_START,
               TourLogView.CSS_LOG_TITLE);

         runEasyImport_001_WithLauncher((ImportLauncher) firstElement);
      }
   }

   /**
    * Launch easy import from the fancy UI
    *
    * @param launcherId
    */
   private void runEasyImport(final long launcherId) {

      /*
       * Log import start
       */
      TourLogManager.addLog(
            TourLogState.DEFAULT,
            EasyImportManager.LOG_EASY_IMPORT_000_IMPORT_START,
            TourLogView.CSS_LOG_TITLE);

      if (isWatchingOn() == false) {

         /*
          * It can be dangerous pressing an import tile and the UI is dimmed, so it's almost
          * invisible what is clicked.
          */

         return;
      }

      /*
       * Get import launcher
       */
      final EasyConfig easyConfig = getEasyConfig();
      ImportLauncher importLauncher = null;

      for (final ImportLauncher launcher : easyConfig.importLaunchers) {
         if (launcher.getId() == launcherId) {
            importLauncher = launcher;
            break;
         }
      }

      if (importLauncher == null) {

         // this should not occur
         return;
      }

      runEasyImport_001_WithLauncher(importLauncher);
   }

   private void runEasyImport_001_WithLauncher(final ImportLauncher importLauncher) {

      final EasyConfig easyConfig = getEasyConfig();
      final ImportConfig importConfig = easyConfig.getActiveImportConfig();

      final long start = System.currentTimeMillis();

      /*
       * Check person
       */
      TourPerson person = null;
      if (importLauncher.isSaveTour) {

         person = TourbookPlugin.getActivePerson();

         if (person == null) {

            MessageDialog.openError(
                  _parent.getShell(),
                  Messages.Import_Data_Dialog_EasyImport_Title,
                  Messages.Import_Data_Dialog_NoActivePersion_Message);
            return;
         }
      }

      // clear old tours which can cause problems when they are reimported
//      TourManager.getInstance().clearTourDataCache();

      /*
       * Run easy import
       */
      ImportState_Easy importState_Easy = null;

      final ImportState_Process importState_Process = new ImportState_Process()

            .setIsEasyImport(true);

      if (easyConfig.isLogDetails == false) {

         // disable logging, the default is to log details

         importState_Process

               .setIsLog_DEFAULT(false)
               .setIsLog_INFO(false)
               .setIsLog_OK(false)

         ;
      }

      TourLogManager.showLogView(AutoOpenEvent.TOUR_IMPORT);

      try {

         // disable state update during import, this causes lots of problems !!!
         _isUpdateDeviceState = false;

         importState_Easy = EasyImportManager.getInstance().runImport(importLauncher, importState_Process);

      } finally {

         _isUpdateDeviceState = true;
      }

      /*
       * Update viewer with newly imported files
       */
      final Collection<TourData> importedToursCollection = RawDataManager.getInstance().getImportedTours().values();
      final ArrayList<TourData> importedTours = new ArrayList<>(importedToursCollection);

      final boolean isIgnoreInvalidFile = RawDataManager.isIgnoreInvalidFile();

      try {

         // stop all other actions when canceled
         if (importState_Easy.isImportCanceled) {
            return;
         }

         // open import config dialog to solve problems
         if (importState_Easy.isOpenSetup) {

            _parent.getDisplay().asyncExec(() -> onSelect_SetupEasyImport(0));

            return;
         }

         /*
          * 4.1 Remove 2nd last time slice marker
          */
         if (importLauncher.isRemove2ndLastTimeSliceMarker) {
            runEasyImport_0041_Remove2ndLastTimesliceMarker(importLauncher, importedTours);
         }

         /*
          * 4.2 Set last marker text
          */
         if (importLauncher.isSetLastMarker) {
            runEasyImport_0042_SetLastMarker(importLauncher, importedTours);
         }

         /*
          * 5. Adjust temperature
          */
         if (importLauncher.isAdjustTemperature) {
            runEasyImport_005_AdjustTemperature(importLauncher, importedTours);
         }

         /*
          * 6. Adjust elevation
          */
         if (importLauncher.isReplaceFirstTimeSliceElevation) {
            runEasyImport_006_ReplaceFirstTimeSliceElevation(importLauncher, importedTours);
         }

         /*
          * 7. Replace elevation up/down from SRTM values
          */
         if (importLauncher.isReplaceElevationFromSRTM) {
            runEasyImport_007_ReplaceElevationFromSRTM(importLauncher, importedTours);
         }

         /*
          * 8. Set tour tags from a group
          */
         if (importLauncher.isSetTourTagGroup) {
            runEasyImport_008_SetTourTags(importLauncher, importedTours);
         }

         /*
          * 50. Retrieve weather data
          */
         if (importLauncher.isRetrieveWeatherData) {
            runEasyImport_050_RetrieveWeatherData(importLauncher, importedTours);
         }

         /*
          * 51. Retrieve tour location
          */
         if (importLauncher.isRetrieveTourLocation) {
            runEasyImport_051_RetrieveTourLocation(importLauncher, importedTours);
         }

         ArrayList<TourData> importedAndSavedTours;

         /*
          * 99. Save imported tours
          */
         if (importLauncher.isSaveTour) {

            importedAndSavedTours = runEasyImport_099_SaveTour(person, importedTours);

         } else {

            importedAndSavedTours = _rawDataMgr.getImportedTours_AsList();
         }

         /*
          * 100. Delete device files
          */
         if (importConfig.isDeleteDeviceFiles) {

            // use newly saved/not saved tours

            final String[] invalidFilesSet = _rawDataMgr.getInvalidFilesList().keySet().toArray(String[]::new);

            final String[] invalidFiles = isIgnoreInvalidFile
                  ? invalidFilesSet
                  : null;

            runEasyImport_100_DeleteTourFiles(false, importedAndSavedTours, invalidFiles, true);
         }

         /*
          * 101. Turn watching off
          */
         if (importConfig.isTurnOffWatching) {

            TourLogManager.log_DEFAULT(EasyImportManager.LOG_EASY_IMPORT_101_TURN_WATCHING_OFF);

            setWatcher_2_Off();
         }

         /*
          * Log import end
          */
         TourLogManager.log_DEFAULT(String.format(
               EasyImportManager.LOG_EASY_IMPORT_999_IMPORT_END,
               (System.currentTimeMillis() - start) / 1000.0));

      } finally {

         // update viewer when required

         Display.getDefault().asyncExec(() -> importState_Process.runPostProcess());

         if (importState_Easy.isUpdateImportViewer) {

            _tourViewer.update(importedToursCollection.toArray(), null);

            selectFirstTour();
         }
      }

      if (isIgnoreInvalidFile) {
         _rawDataMgr.clearInvalidFilesList();
      }
   }

   private void runEasyImport_0041_Remove2ndLastTimesliceMarker(final ImportLauncher importLauncher,
                                                                final ArrayList<TourData> allImportedTours) {

      TourLogManager.log_DEFAULT(EasyImportManager.LOG_EASY_IMPORT_0041_REMOVE_2ND_LAST_TIMESLICE_MARKER);

      for (final TourData tourData : allImportedTours) {

         final Set<TourMarker> allTourMarkers = tourData.getTourMarkers();
         final int numMarkers = allTourMarkers.size();

         // check if markers are available
         if (numMarkers == 0) {
            continue;
         }

         final int numTimeSlices = tourData.timeSerie.length;

         final int timeSerie2ndLastIndex = numTimeSlices - 2;
         final int timeSerie3rdLastIndex = numTimeSlices - 3;

         for (final TourMarker tourMarker : allTourMarkers) {

            final int markerSerieIndex = tourMarker.getSerieIndex();

            if (markerSerieIndex == timeSerie2ndLastIndex
                  || markerSerieIndex == timeSerie3rdLastIndex) {

               allTourMarkers.remove(tourMarker);

               tourData.resetSortedMarkers();

               TourLogManager.subLog_DEFAULT(TourManager.getTourDateTimeShort(tourData));

               break;
            }
         }
      }
   }

   private void runEasyImport_0042_SetLastMarker(final ImportLauncher importLauncher,
                                                 final ArrayList<TourData> importedTours) {

      final String lastMarkerText = importLauncher.lastMarkerText;
      if (lastMarkerText == null || lastMarkerText.trim().length() == 0) {
         // there is nothing to do
         return;
      }

      TourLogManager.log_DEFAULT(EasyImportManager.LOG_EASY_IMPORT_0042_SET_LAST_MARKER);

      final int ilLastMarkerDistance = importLauncher.lastMarkerDistance;

      for (final TourData tourData : importedTours) {

         // check if distance is available
         final float[] distancSerie = tourData.distanceSerie;
         if (distancSerie == null || distancSerie.length == 0) {
            continue;
         }

         final ArrayList<TourMarker> tourMarkers = tourData.getTourMarkersSorted();
         final int numMarkers = tourMarkers.size();

         // check if markers are available
         if (numMarkers == 0) {
            continue;
         }

         // get last marker
         final TourMarker lastMarker = tourMarkers.get(numMarkers - 1);

         final int markerIndex = lastMarker.getSerieIndex();

         final float lastMarkerDistance = distancSerie[markerIndex];
         final float tourDistance = tourData.getTourDistance();
         final float distanceDiff = tourDistance - lastMarkerDistance;

         if (distanceDiff <= ilLastMarkerDistance) {

            // this marker is in the range of the last marker distance -> set the tour marker text

            lastMarker.setLabel(lastMarkerText);

            TourLogManager.subLog_DEFAULT(TourManager.getTourDateTimeShort(tourData));
         }
      }
   }

   private void runEasyImport_005_AdjustTemperature(final ImportLauncher importLauncher,
                                                    final ArrayList<TourData> importedTours) {

      final float avgMinimumTemperature = importLauncher.tourAvgTemperature;
      final float temperature = UI.convertTemperatureFromMetric(avgMinimumTemperature);
      final int durationTime = importLauncher.temperatureAdjustmentDuration;

      // "5. Adjust tour start temperature values - {0} < {1} {2}"
      TourLogManager.log_DEFAULT(NLS.bind(
            EasyImportManager.LOG_EASY_IMPORT_005_ADJUST_TEMPERATURE,
            new Object[] {
                  getDurationText(importLauncher),
                  _nf1.format(temperature),
                  UI.UNIT_LABEL_TEMPERATURE }));

      for (final TourData tourData : importedTours) {

         final float oldTourAvgTemperature = tourData.getWeather_Temperature_Average_Device();

         // skip tours which avg temperature is above the minimum avg temperature
         if (oldTourAvgTemperature > avgMinimumTemperature) {

            // "%s . . . %.2f > %.0f °C"
            TourLogManager.subLog_DEFAULT(

                  TourManager.LOG_TEMP_ADJUST_006_IS_ABOVE_TEMPERATURE.formatted(

                        TourManager.getTourDateTimeShort(tourData),
                        oldTourAvgTemperature,
                        avgMinimumTemperature));

            continue;
         }

         TourManager.adjustTemperature(tourData, durationTime);
      }
   }

   private void runEasyImport_006_ReplaceFirstTimeSliceElevation(final ImportLauncher importLauncher,
                                                                 final ArrayList<TourData> importedTours) {

      // "6. Replace first time slice elevation value"
      TourLogManager.log_DEFAULT(EasyImportManager.LOG_EASY_IMPORT_006_ADJUST_ELEVATION);

      for (final TourData tourData : importedTours) {

         final float[] altitudeSerie = tourData.altitudeSerie;

         if (altitudeSerie == null || altitudeSerie.length < 2) {

            continue;
         }

         final float firstElevation = altitudeSerie[0];
         final float secondElevation = altitudeSerie[1];

         final int[] timeSerie = tourData.timeSerie;
         final float timeDiff = timeSerie[1];

         final float elevationDiff = Math.abs(firstElevation - secondElevation);
         final float timeElevationDiff = elevationDiff / timeDiff;

         if (timeElevationDiff > 0.5) {

            // adjust elevation

            altitudeSerie[0] = secondElevation;

            // discard computed elevation values
            tourData.clearAltitudeSeries();

            tourData.computeAltitudeUpDown();
            tourData.computeComputedValues();

            // "%s - %.1f Δ %s"
            TourLogManager.subLog_OK(String.format(
                  LOG_TOUR_DETAILS,
                  TourManager.getTourDateTimeShort(tourData),
                  timeDiff,
                  elevationDiff,
                  UI.UNIT_LABEL_ELEVATION));
         } else {

            // "%s - %.1f Δ %s"
            TourLogManager.subLog_DEFAULT(String.format(
                  LOG_TOUR_DETAILS,
                  TourManager.getTourDateTimeShort(tourData),
                  timeDiff,
                  elevationDiff,
                  UI.UNIT_LABEL_ELEVATION));
         }
      }
   }

   private void runEasyImport_007_ReplaceElevationFromSRTM(final ImportLauncher importLauncher,
                                                           final ArrayList<TourData> importedTours) {

      // "7. Replace elevation up/down total values from SRTM data"
      TourLogManager.log_DEFAULT(EasyImportManager.LOG_EASY_IMPORT_007_REPLACE_ELEVATION_FROM_SRTM);

      for (final TourData tourData : importedTours) {

         final int oldElevationUp = tourData.getTourAltUp();
         final int oldElevationDown = tourData.getTourAltDown();

         final FlatGainLoss elevationUpDown = tourData.computeAltitudeUpDown_FromSRTM();

         if (elevationUpDown == null) {

            TourLogManager.subLog_ERROR(String.format(Messages.Log_ReplaceElevationUpDown_SRTMDataNotAvailable,
                  TourManager.getTourDateTimeShort(tourData)));

         } else {

            TourLogManager.subLog_DEFAULT(String.format(Messages.Log_ReplaceElevationUpDown_ValuesAreReplaced,

                  TourManager.getTourDateTimeShort(tourData),

                  oldElevationUp,
                  elevationUpDown.elevationGain,

                  oldElevationDown,
                  elevationUpDown.elevationLoss));
         }
      }
   }

   private void runEasyImport_008_SetTourTags(final ImportLauncher importLauncher,
                                              final ArrayList<TourData> importedTours) {

      final String tourTagGroupID = importLauncher.tourTagGroupID;
      final TagGroup tagGroup = TagGroupManager.getTagGroup(tourTagGroupID);

      if (tagGroup == null) {
         return;
      }

      // "8. Set tour tags from the tag group %s"
      TourLogManager.log_DEFAULT(EasyImportManager.LOG_EASY_IMPORT_008_SET_TOUR_TAGS.formatted(createTagGroupText(importLauncher)));

      for (final TourData tourData : importedTours) {

         tourData.setTourTags(tagGroup.tourTags);
      }
   }

   private void runEasyImport_050_RetrieveWeatherData(final ImportLauncher importLauncher,
                                                      final List<TourData> importedTours) {

      // "50. Retrieve Weather Data"

      TourLogManager.log_DEFAULT(NLS.bind(
            EasyImportManager.LOG_EASY_IMPORT_050_RETRIEVE_WEATHER_DATA,
            new Object[] {
                  getDurationText(importLauncher),
                  UI.UNIT_LABEL_TEMPERATURE }));

      TourManager.retrieveWeatherData(importedTours);
   }

   private void runEasyImport_051_RetrieveTourLocation(final ImportLauncher importLauncher,
                                                       final ArrayList<TourData> importedTours) {

      // 51. Retrieve tour locations
      TourLogManager.log_DEFAULT(EasyImportManager.LOG_EASY_IMPORT_051_RETRIEVE_TOUR_LOCATION);

      final long start = System.currentTimeMillis();

      TourLocationManager.setTourLocations(

            importedTours,
            importLauncher.tourLocationProfile, // locationProfile

            true, // isSetStartLocation
            true, // isSetEndLocation

            false, // isOneAction
            null, // oneActionLocation

            false, // isSaveTour
            true // isLogLocation
      );

      // location data retrieved in %.1f s
      TourLogManager.subLog_DEFAULT(String.format(
            Messages.Log_TourLocation_Retrieve_End,
            (System.currentTimeMillis() - start) / 1000.0));

   }

   private ArrayList<TourData> runEasyImport_099_SaveTour(final TourPerson person, final ArrayList<TourData> importedTours) {

      // "99. Save tours"
      TourLogManager.log_DEFAULT(EasyImportManager.LOG_EASY_IMPORT_099_SAVE_TOUR);

      return saveImportedTours(importedTours, person);
   }

   /**
    * @param isDeleteAllFiles
    *           When <code>true</code> then all files (device and backup) will be deleted. Otherwise
    *           only device files will be deleted without any confirmation dialog, the backup files
    *           are not touched, this feature is used to move device files to the backup folder.
    */
   private void runEasyImport_100_DeleteTourFiles(final boolean isDeleteAllFiles,
                                                  final ArrayList<TourData> allTourData,
                                                  final String[] invalidFiles,
                                                  final boolean isEasyImport) {

      // open log view always when tour files are deleted
      TourLogManager.showLogView(AutoOpenEvent.DELETE_SOMETHING);

      final String css = isEasyImport
            ? UI.EMPTY_STRING
            : TourLogView.CSS_LOG_TITLE;

      final String message = isEasyImport
            ? EasyImportManager.LOG_EASY_IMPORT_100_DELETE_TOUR_FILES
            : Messages.Log_Import_DeleteTourFiles;

      TourLogManager.addLog(TourLogState.DEFAULT, message, css);

      if (isDeleteAllFiles) {

         if (MessageDialog.openConfirm(
               _parent.getShell(),
               Messages.Import_Data_Dialog_DeleteTourFiles_Title,
               Messages.Import_Data_Dialog_DeleteTourFiles_Message) == false) {
            return;
         }

         if (MessageDialog.openConfirm(
               _parent.getShell(),
               Messages.Import_Data_Dialog_DeleteTourFiles_Title,
               Messages.Import_Data_Dialog_DeleteTourFiles_LastChance_Message) == false) {
            return;
         }
      }

      final ArrayList<String> deletedFiles = new ArrayList<>();
      final ArrayList<String> notDeletedFiles = new ArrayList<>();

      final IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            int saveCounter = 0;

            int selectionSize = allTourData.size();
            selectionSize += invalidFiles != null ? invalidFiles.length : 0;

            monitor.beginTask(Messages.Import_Data_Monitor_DeleteTourFiles, selectionSize);

            // loop: all selected tours, selected tours can already be saved
            for (final TourData tourData : allTourData) {

               monitor.subTask(NLS.bind(
                     Messages.Import_Data_Monitor_DeleteTourFiles_Subtask,
                     ++saveCounter,
                     selectionSize));

               monitor.worked(1);

               if (tourData.isBackupImportFile && isDeleteAllFiles == false) {

                  /*
                   * Do not delete files which are imported from the backup folder
                   */

                  continue;
               }

               final String originalFilePath = tourData.importFilePathOriginal;

               // this is the backup folder when a backup is created
               final String importFilePath = tourData.getImportFilePath();
               final String importFileName = tourData.getImportFileName();

               // delete backup files
               if (isDeleteAllFiles) {
                  deleteFile(
                        deletedFiles,
                        notDeletedFiles,
                        importFilePath,
                        importFileName,
                        TourLogState.EASY_IMPORT_DELETE_BACKUP);
               }

               // delete device files
               deleteFile(
                     deletedFiles,
                     notDeletedFiles,
                     originalFilePath,
                     importFileName,
                     TourLogState.EASY_IMPORT_DELETE_DEVICE);

               // set state
               if (isDeleteAllFiles) {
                  tourData.isTourFileDeleted = true;
               } else {
                  tourData.isTourFileMoved = true;
               }

            }

            if (invalidFiles != null) {

               for (final String invalidFilePath : invalidFiles) {

                  deleteFile(
                        deletedFiles,
                        notDeletedFiles,
                        Paths.get(invalidFilePath).getParent().toString(),
                        Paths.get(invalidFilePath).getFileName().toString(),
                        TourLogState.EASY_IMPORT_DELETE_DEVICE);

                  monitor.worked(1);
               }
            }
         }
      };

      try {

         new ProgressMonitorDialog(_parent.getShell()).run(true, false, saveRunnable);

      } catch (InvocationTargetException | InterruptedException e) {
         TourLogManager.log_EXCEPTION_WithStacktrace(e);
      }

      // show delete state in UI
      _tourViewer.update(allTourData.toArray(), null);

      /*
       * Log deleted files
       */
      TourLogManager.log_DEFAULT(String.format(
            Messages.Log_Import_DeleteTourFiles_End,
            deletedFiles.size(),
            notDeletedFiles.size()));
   }

   /**
    * Save imported tours
    *
    * @param allTourData
    * @param person
    *
    * @return Returns saved {@link TourData}
    */
   private ArrayList<TourData> saveImportedTours(final ArrayList<TourData> allTourData, final TourPerson person) {

      final int numTours = allTourData.size();
      final ArrayList<TourData> allSavedTours = new ArrayList<>();

      if (numTours == 0) {

         // nothing to do

         return allSavedTours;
      }

      final long start = System.currentTimeMillis();

      /*
       * Check if new tour tags / types were created, when yes the UI must be updated afterwards
       * otherwise e.g. new tour types are not available in the tour type filter !!!
       */
      boolean isNewTourType = false;
      boolean isNewTourTags = false;
      for (final TourData tourData : allTourData) {

         /*
          * Check tour type
          */
         final TourType tourType = tourData.getTourType();
         if (tourType != null) {

            if (tourType.getTypeId() == -1) {
               isNewTourType = true;
            }
         }

         /*
          * Check tour tags
          */
         for (final TourTag tourTag : tourData.getTourTags()) {

            if (tourTag.getTagId() == -1) {
               isNewTourTags = true;
               break;
            }
         }
      }

      /*
       * Setup concurrency
       */
      _saveTour_CountDownLatch = new CountDownLatch(numTours);
      _saveTour_Queue.clear();

      final ArrayBlockingQueue<TourData> allSavedToursConcurrent = new ArrayBlockingQueue<>(numTours);

      final IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            final AtomicInteger numSavedTours = new AtomicInteger();

            final long startTime = System.currentTimeMillis();
            long lastUpdateTime = startTime;

            monitor.beginTask(Messages.Tour_Data_SaveTour_Monitor, numTours);

            // loop: all selected tours, selected tours can already be saved
            for (final TourData tourData : allTourData) {

               final long currentTime = System.currentTimeMillis();
               final long timeDiff = currentTime - lastUpdateTime;

               // reduce logging
               if (timeDiff > 500) {

                  lastUpdateTime = currentTime;

                  monitor.subTask(NLS.bind(Messages.Tour_Data_SaveTour_MonitorSubtask, numSavedTours.get(), numTours));
               }

               saveImportedTours_10_Concurrent(
                     tourData,
                     person,
                     allSavedToursConcurrent,
                     monitor,
                     numSavedTours);
            }

            // wait until all re-imports are performed
            _saveTour_CountDownLatch.await();
         }
      };

      try {

         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, saveRunnable);

      } catch (final Exception e) {

         TourLogManager.log_EXCEPTION_WithStacktrace(e);
         Thread.currentThread().interrupt();

      } finally {

         // get all saved tour data
         allSavedToursConcurrent.drainTo(allSavedTours);

         TourLogManager.subLog_DEFAULT(String.format(Messages.Log_Tour_SaveTours_End,
               allSavedTours.size(),
               (System.currentTimeMillis() - start) / 1000.0));

         // update e.g. fulltext index
         TourDatabase.saveTour_PostSaveActions_Concurrent_2_ForAllTours(getAllTourIds(allSavedTours));

         saveImportedTours_30_PostActions(allSavedTours, isNewTourType, isNewTourTags);
      }

      return allSavedTours;
   }

   private void saveImportedTours_10_Concurrent(final TourData tourData,
                                                final TourPerson person,
                                                final ArrayBlockingQueue<TourData> allSavedToursConcurrent,
                                                final IProgressMonitor monitor,
                                                final AtomicInteger numSavedTours) {

      // workaround for hibernate problems
      if (tourData.isTourDeleted) {
         monitor.worked(1);
         numSavedTours.getAndIncrement();
         _saveTour_CountDownLatch.countDown();

         return;
      }

      if (tourData.getTourPerson() != null) {

         /*
          * Tour is already saved, resaving cannot be done in the import view it can be done in the
          * tour editor
          */
         monitor.worked(1);
         numSavedTours.getAndIncrement();
         _saveTour_CountDownLatch.countDown();

         return;
      }

      try {

         // put tour data into the queue AND wait when it is full

         _saveTour_Queue.put(tourData);

      } catch (final InterruptedException e) {

         TourLogManager.log_EXCEPTION_WithStacktrace(e);
         Thread.currentThread().interrupt();
      }

      _saveTour_Executor.submit(() -> {

         try {

            // get last added tour
            final TourData queueItem_TourData = _saveTour_Queue.poll();

            if (queueItem_TourData != null) {

               saveImportedTours_20_Concurrent_OneTour(tourData, person, allSavedToursConcurrent);
            }

//            TourLogManager.addSubLog(TourLogState.TOUR_SAVED,
//                  String.format(TourLogManager.LOG_TOUR_SAVE_TOURS_FILE,
//                        tourData.getTourStartTime().format(TimeTools.Formatter_DateTime_S),
//                        tourData.getImportFilePathNameText()));

         } finally {

            monitor.worked(1);
            numSavedTours.getAndIncrement();

            _saveTour_CountDownLatch.countDown();
         }
      });
   }

   /**
    * @param tourData
    *           {@link TourData} which is not yet saved.
    * @param person
    *           Person for which the tour is being saved.
    * @param allSavedTours
    *           The saved tour is added to this list.
    */
   private void saveImportedTours_20_Concurrent_OneTour(final TourData tourData,
                                                        final TourPerson person,
                                                        final ArrayBlockingQueue<TourData> allSavedTours) {

      // a saved tour needs a person
      tourData.setTourPerson(person);

      // set weight from person
      if (RawDataManager.isSetBodyWeight()) {
         tourData.setBodyWeight(person.getWeight());
      }

      tourData.setTourBike(person.getTourBike());

      final TourData savedTour = TourDatabase.saveTour_Concurrent(tourData, true);

      if (savedTour != null) {

         allSavedTours.add(savedTour);

         // update fields which are not saved but used in the UI and easy setup
         savedTour.isTourFileDeleted = tourData.isTourFileDeleted;
         savedTour.isTourFileMoved = tourData.isTourFileMoved;
         savedTour.isBackupImportFile = tourData.isBackupImportFile;
         savedTour.importFilePathOriginal = tourData.importFilePathOriginal;
      }
   }

   /**
    * After tours are saved, the internal structures and ui viewers must be updated
    *
    * @param savedTours
    *           contains the saved {@link TourData}
    * @param isNewTourTags
    * @param isNewTourType
    */
   private void saveImportedTours_30_PostActions(final ArrayList<TourData> savedTours,
                                                 final boolean isNewTourType,
                                                 final boolean isNewTourTags) {

      // update viewer, fire selection event
      if (savedTours.isEmpty()) {
         return;
      }

      final ArrayList<Long> savedToursIds = new ArrayList<>();

      // update raw data map with the saved tour data
      final Map<Long, TourData> rawDataMap = _rawDataMgr.getImportedTours();
      for (final TourData tourData : savedTours) {

         final Long tourId = tourData.getTourId();

         rawDataMap.put(tourId, tourData);
         savedToursIds.add(tourId);
      }

      /*
       * The selection provider can contain old tour data which conflicts with the tour data in the
       * tour data editor
       */
      _postSelectionProvider.clearSelection();

      // update import viewer
      reloadViewer();

      enableActions();

      if (isNewTourType) {

         TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED, Math.random());
      }

      if (isNewTourTags) {

         TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
      }

      /*
       * Notify all views, it is not checked if the tour data editor is dirty because newly saved
       * tours can not be modified in the tour data editor
       */
      TourManager.fireEventWithCustomData(TourEventId.UPDATE_UI, new SelectionTourIds(savedToursIds), this);
   }

   @PersistState
   private void saveState() {

      // check if UI is disposed
      if (_parent.isDisposed()) {
         return;
      }

      EasyImportManager.getInstance().saveEasyConfig(getEasyConfig());

      Util.setStateEnum(_state, STATE_IMPORT_UI, _importUI);

      /*
       * save imported file names
       */
      final boolean isRemoveToursWhenClosed = _actionRemoveToursWhenClosed.isChecked();
      String[] stateImportedFiles;
      if (isRemoveToursWhenClosed) {
         stateImportedFiles = new String[] {};
      } else {
         final ConcurrentHashMap<String, String> importedFiles = _rawDataMgr.getImportedFiles();
         stateImportedFiles = importedFiles.keySet().toArray(String[]::new);

      }
      _state.put(STATE_IMPORTED_FILENAMES, stateImportedFiles);
      _state.put(STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED, isRemoveToursWhenClosed);

      // keep selected tours
      Util.setState(_state, STATE_SELECTED_TOUR_INDICES, _tourViewer.getTable().getSelectionIndices());

      _tourViewer_ColumnManager.saveState(_state);
      _simpleUI_ImportLauncher_ColumnManager.saveState(_stateSimpleUI);
   }

   /**
    * Select first tour in the import view.
    */
   public void selectFirstTour() {

      final TourData firstTourData = (TourData) _tourViewer.getElementAt(0);
      if (firstTourData != null) {
         _tourViewer.setSelection(new StructuredSelection(firstTourData), true);
      }
   }

   /**
    * Passing the focus request to the viewer's control.
    */
   @Override
   public void setFocus() {

      /*
       * When imported tours are available then the import viewer page will ALLWAYS be displayed.
       */
      final int numImportedTours = _rawDataMgr.getImportedTours().size();
      if (numImportedTours > 0) {

         final Table table = _tourViewer.getTable();

         if (table.isDisposed()) {

            // this occurred when testing
            return;
         }

         table.setFocus();

      } else {

         switch (_importUI) {

         case EASY_IMPORT_FANCY:

            if (_browser != null) {
               _browser.setFocus();
            }

            break;

         case EASY_IMPORT_SIMPLE:

            _simpleUI_ImportLauncher_Viewer.getTable().setFocus();

            break;

         case FOSSIL:
         default:

            _topPage_PageBook.setFocus();

            break;
         }
      }

      if (_postSelectionProvider.getSelection() == null) {

         // fire a selected tour when the selection provider was cleared sometime before
         Display.getCurrent().asyncExec(() -> fireSelectedTour());
      }
   }

   private void setWatcher_1_On() {

      if (isWatchingOn()) {

         // do not start twice
         return;
      }

      if (_importUI == ImportUI.EASY_IMPORT_FANCY) {

         updateUI_WatcherAnimation(DOM_CLASS_DEVICE_ON_ANIMATED);

      } else {

         // simple UI

         enableSimpleUI(true);
      }

      thread_WatchStores_Start();
      thread_FolderWatcher_Activate();
   }

   private void setWatcher_2_Off() {

      setWatcher_2_Off(true);
   }

   /**
    * @param isUpdateUI
    */
   private void setWatcher_2_Off(final boolean isUpdateUI) {

      if (isWatchingOn()) {

         /*
          * !!! Store watching must be canceled before the watch folder thread because it could
          * launch a new watch folder thread !!!
          */
         thread_WatchStores_Cancel();

         // thread_WatchFolders(false);

         try {

            if (_watchingStoresThread != null) {
               _watchingStoresThread.join();
            }

            if (isUpdateUI) {

               if (_importUI == ImportUI.EASY_IMPORT_FANCY) {

                  updateUI_WatcherAnimation(DOM_CLASS_DEVICE_OFF_ANIMATED);

               } else {

                  // simple UI

                  updateUI_DeviceState_SimpleUI();

                  enableSimpleUI(false);
               }
            }

         } catch (final InterruptedException e) {
            TourLogManager.log_EXCEPTION_WithStacktrace(e);
         }
      }
   }

   private void showFailbackUI() {

      if (_browser == null || _browser.isDisposed()) {

         // show OLD UI after 5 seconds
         Display.getDefault().timerExec(5000, () -> {

            if (_parent.isDisposed()) {
               return;
            }

            // check again because the browser could be set
            if (_browser == null || _browser.isDisposed()) {

               onSelect_ImportUI(ImportUI.EASY_IMPORT_SIMPLE);
            }
         });
      }
   }

   private void thread_FolderWatcher_Activate() {

      // activate store watching
      _isWatchingStores.set(true);

      // activate folder watching
      thread_WatchFolders(true);
   }

   private void thread_FolderWatcher_Deactivate() {

      // deactivate background tasks

      _isWatchingStores.set(false);

      thread_WatchFolders(false);
   }

   /**
    * Retrieve files from the device folder and update the UI.
    */
   private void thread_UpdateDeviceState() throws InterruptedException {

      final EasyConfig importConfig = getEasyConfig();

      if (importConfig.getActiveImportConfig().isWatchAnything()) {

         EasyImportManager.getInstance().checkImportedFiles(true);
         updateUI_DeviceState();
      }
   }

   /**
    * @param isStartWatching
    *           When <code>true</code> a new watcher is restarted, otherwise this thread is
    *           canceled.
    */
   private void thread_WatchFolders(final boolean isStartWatching) {

      WATCH_LOCK.lock();
      try {

         // cancel previous watcher
         thread_WatchFolders_Cancel();

         if (_watchingFolderThread != null) {
            throw new RuntimeException();
         }

         if (isWatchingOn() == false) {
            // watching store is off -> do not watch folders
            return;
         }

         if (isStartWatching) {

            final Runnable runnable = thread_WatchFolders_Runnable();

            _watchingFolderThread = new Thread(runnable, "WatchDeviceFolder - " + TimeTools.now()); //$NON-NLS-1$
            _watchingFolderThread.setDaemon(true);
            _watchingFolderThread.start();
         }

      } finally {
         WATCH_LOCK.unlock();
      }
   }

   /**
    * <b>!!! This thread is not interrupted it could cause SQL exceptions !!!</b>
    */
   private void thread_WatchFolders_Cancel() {

      if (_watchingFolderThread != null) {

         try {
            if (_folderWatcher != null) {

               try {
                  THREAD_WATCHER_LOCK.lock();
                  _folderWatcher.close();
               } catch (final IOException e) {
                  TourLogManager.log_EXCEPTION_WithStacktrace(e);
               } finally {
                  _watchingFolderThread.interrupt(); // (rtdog) CancelWatchfolders
                  THREAD_WATCHER_LOCK.unlock();

                  //  This join could be interrupted and throw spurious exception
                  //  It could also hang on a STORE_LOCK deadlock
                  _watchingFolderThread.join(10000); // unlock then join
               }
            }
         } catch (final InterruptedException e) {
            // TourLogManager.logEx(e); // This is expected condition, don't need to log.
         } finally {
            _folderWatcher = null;
            _watchingFolderThread = null;
         }
      }
   }

   /**
    * Note : A suppress warning is added because the resource "tourbookFileSystem"
    * is actually closed in this method {@link #dispose()}
    *
    * @return
    */
   private Runnable thread_WatchFolders_Runnable() {

      return () -> {

         WatchService folderWatcher = null;
         WatchKey watchKey = null;

         try {

            final EasyConfig easyConfig = getEasyConfig();
            final ImportConfig importConfig = easyConfig.getActiveImportConfig();

            /*
             * Check device folder
             */
            boolean isDeviceFolderValid = false;
            final String deviceFolder = importConfig.getDeviceOSFolder();

            final FileSystem tourbookFileSystem = NIO.isTourBookFileSystem(
                  deviceFolder)
                        ? FileSystemManager.getFileSystem(deviceFolder) : null;

            // keep watcher local because it could be set to null !!!
            folderWatcher = _folderWatcher =
                  tourbookFileSystem != null
                        ? tourbookFileSystem.newWatchService()
                        : FileSystems.getDefault().newWatchService();

            if (deviceFolder != null) {

               try {

                  final Path deviceFolderPath = NIO.getDeviceFolderPath(deviceFolder);

                  if (Files.exists(deviceFolderPath)) {

                     isDeviceFolderValid = true;

                     deviceFolderPath.register(folderWatcher,
                           StandardWatchEventKinds.ENTRY_CREATE,
                           StandardWatchEventKinds.ENTRY_DELETE);
                  }

               } catch (final Exception e1) {}
            }

            if (isDeviceFolderValid) {

               Thread.currentThread().setName("WatchingDeviceFolder: " + deviceFolder + " - " + TimeTools.now()); //$NON-NLS-1$ //$NON-NLS-2$

            } else {

               // the device folder is not available, update the folder state

               updateUI_DeviceState();

               return;
            }

            /*
             * Check backup folder
             */
            final String backupFolder = importConfig.getBackupOSFolder();
            if (backupFolder != null) {

               try {

                  final Path watchBackupFolder = Paths.get(backupFolder);

                  if (Files.exists(watchBackupFolder)) {

                     watchBackupFolder.register(folderWatcher,
                           StandardWatchEventKinds.ENTRY_CREATE,
                           StandardWatchEventKinds.ENTRY_DELETE);
                  }

               } catch (final Exception e2) {}
            }

            do {

               // wait for the next event
               watchKey = folderWatcher.take();

               if (Thread.currentThread().isInterrupted()) {
                  Thread.currentThread().interrupt();
                  throw new InterruptedException(); // Needed because DropboxFileWatcher take() doesn't throw interruptedException when interrupted
               }

               /*
                * Events MUST be polled otherwise this will stay in an endless loop.
                */
               @SuppressWarnings("unused")
               final List<WatchEvent<?>> polledEvents = watchKey.pollEvents();

//// log events, they are not used
//                  for (final WatchEvent<?> event : polledEvents) {
//
//                     final WatchEvent.Kind<?> kind = event.kind();
//
//                     System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//                           + (String.format("Event: %s\tFile: %s", kind, event.context())));
//                     // remove SYSTEM.OUT.PRINTLN
//                  }

               // do not update the device state when the import is running otherwise the import file list can be wrong
               if (_isUpdateDeviceState) {
                  thread_UpdateDeviceState();
               }

            }
            while (watchKey.reset());

         } catch (final InterruptedException | ClosedWatchServiceException e3) {
            // no-op
            Thread.currentThread().interrupt();
         } catch (final Exception e4) {
            TourLogManager.log_EXCEPTION_WithStacktrace(e4);
         } finally {

            try {
               if (watchKey != null) {
                  watchKey.cancel();
               }

               if (folderWatcher != null) {
                  folderWatcher.close();
               }
            } catch (final Exception e5) {
               TourLogManager.log_EXCEPTION_WithStacktrace(e5);
            }
         }
      };
   }

   /**
    * Thread cannot be interrupted, it could cause SQL exceptions, so set flag and wait.
    */
   private void thread_WatchStores_Cancel() {

      _isDeviceStateValid = false;
      _isStopWatchingStoresThread = true;

      // run with progress, duration can be 0...10 seconds
      try {

         final IRunnableWithProgress runnable = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

               try {

                  // fixed NPE
                  if (_watchingStoresThread != null) {

                     monitor.beginTask(Messages.Import_Data_Task_CloseDeviceInfo, IProgressMonitor.UNKNOWN);

                     final int waitingTime = 10000; // in ms

                     THREAD_WATCHER_LOCK.lock();
                     {
                        _watchingStoresThread.interrupt();
                     }
                     THREAD_WATCHER_LOCK.unlock();

                     _watchingStoresThread.join(waitingTime); // must unlock then join

                     if (_watchingStoresThread.isAlive()) {

                        StatusUtil.logInfo(NLS.bind(
                              Messages.Import_Data_Task_CloseDeviceInfo_CannotClose,
                              waitingTime / 1000));
                     }
                  }

               } catch (final InterruptedException e) {
                  TourLogManager.log_EXCEPTION_WithStacktrace(e);
               } finally {
                  _watchingStoresThread = null;
               }
            }
         };

         new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, false, runnable);

      } catch (InvocationTargetException | InterruptedException e) {
         TourLogManager.log_EXCEPTION_WithStacktrace(e);
      }

   }

   private void thread_WatchStores_Start() {

      _watchingStoresThread = new Thread("WatchingStores") { //$NON-NLS-1$

         @Override
         public void run() {

            while (true) {

               try {

                  // check if this thread should be stopped
                  if (_isStopWatchingStoresThread) {
                     _isStopWatchingStoresThread = false;
                     break;
                  }

                  // check if polling is currently enabled
                  if (_isWatchingStores.get()) {

                     final EasyConfig importConfig = getEasyConfig();

                     // check if anything should be watched
                     if (importConfig.getActiveImportConfig().isWatchAnything()) {

                        final boolean isCheckFiles = _isDeviceStateValid == false;

                        final DeviceImportState importState = EasyImportManager.getInstance().checkImportedFiles(isCheckFiles);

                        if (importState.areTheSameStores == false || isCheckFiles) {
                           thread_WatchFolders(true);
                        }

                        if (importState.areFilesRetrieved || isCheckFiles) {

                           updateUI_DeviceState();
                        }

                        _isDeviceStateValid = true;
                     }
                  }

                  Thread.sleep(1000);

               } catch (final InterruptedException e) {

                  if (_isStopWatchingStoresThread) {

                     // an interrupt request is performed

                     _isStopWatchingStoresThread = false;

                     break;
                  }

                  // interrupt();

               } catch (final Exception e) {
                  TourLogManager.log_EXCEPTION_WithStacktrace(e);
               }
            }

            _isStopWatchingStoresThread = false;

            // StoreWatcher going down, need to take down DeviceFolderWatcher
            thread_WatchFolders_Cancel();
         }
      };

      _isDeviceStateValid = false;
      _watchingStoresThread.setDaemon(true);
      _watchingStoresThread.start();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * Keep live update values, other values MUST already have been set.
    */
   private void updateModel_EasyConfig_Dashboard(final EasyConfig modifiedConfig) {

      final EasyConfig easyConfig = getEasyConfig();

      if (easyConfig.animationDuration != modifiedConfig.animationDuration
            || easyConfig.animationCrazinessFactor != modifiedConfig.animationCrazinessFactor) {

         // run animation only when it was modified
         _isRunDashboardAnimation = true;
      }

// SET_FORMATTING_OFF

      easyConfig.animationCrazinessFactor             = modifiedConfig.animationCrazinessFactor;
      easyConfig.animationDuration                    = modifiedConfig.animationDuration;
      easyConfig.backgroundOpacity                    = modifiedConfig.backgroundOpacity;
      easyConfig.isLiveUpdate                         = modifiedConfig.isLiveUpdate;
      easyConfig.isLogDetails                         = modifiedConfig.isLogDetails;
      easyConfig.isShowTile_CloudApps                 = modifiedConfig.isShowTile_CloudApps;
      easyConfig.isShowTile_Files                     = modifiedConfig.isShowTile_Files;
      easyConfig.isShowTile_FossilUI                  = modifiedConfig.isShowTile_FossilUI;
      easyConfig.isShowTile_SerialPort                = modifiedConfig.isShowTile_SerialPort;
      easyConfig.isShowTile_SerialPortWithConfig      = modifiedConfig.isShowTile_SerialPortWithConfig;
      easyConfig.numHorizontalTiles                   = modifiedConfig.numHorizontalTiles;
      easyConfig.stateToolTipDisplayAbsoluteFilePath  = modifiedConfig.stateToolTipDisplayAbsoluteFilePath;
      easyConfig.stateToolTipWidth                    = modifiedConfig.stateToolTipWidth;
      easyConfig.tileSize                             = modifiedConfig.tileSize;

// SET_FORMATTING_ON

      EasyImportManager.getInstance().saveEasyConfig(easyConfig);
   }

   private void updateToolTipState() {

      _isToolTipInDate = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_DATE);
      _isToolTipInTime = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TIME);
      _isToolTipInTitle = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TITLE);
      _isToolTipInTags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TAGS);
   }

   /**
    * Set top page.
    */
   private void updateUI_1_TopPage(final boolean isInStartUp) {

      /*
       * When imported tours are available then the import viewer page will ALLWAYS be displayed.
       */
      final int numImportedTours = _rawDataMgr.getImportedTours().size();
      if (numImportedTours > 0) {

         thread_FolderWatcher_Deactivate();

         _topPage_PageBook.showPage(_topPage_ImportViewer);

      } else {

         switch (_importUI) {

         case EASY_IMPORT_FANCY:

            /*
             * !!! Run async that the first page in the top pagebook is visible and to prevent
             * flickering when the view toolbar is first drawn on the left side of the view !!!
             */

            _parent.getDisplay().asyncExec(() -> {

               _isInFancyUIStartup = isInStartUp;

               /*
                * Create new UI only when it is used to prevent that the app is crashing when
                * another import UI is used
                */
               if (_topPage_ImportUI_EasyImport_Fancy == null) {

                  _topPage_ImportUI_EasyImport_Fancy = createUI_20_Page_EasyImporFancy(_topPage_PageBook);
               }

               _topPage_PageBook.showPage(_topPage_ImportUI_EasyImport_Fancy);

               // create dashboard UI
               updateUI_2_EasyImport_Fancy();

               if (_browser == null) {

                  // deactivate background task

                  setWatcher_2_Off();
               }

               // the watcher is started in onBrowser_Completed
            });
            break;

         case EASY_IMPORT_SIMPLE:

            _topPage_PageBook.showPage(_topPage_ImportUI_EasyImport_Simple);

            if (isWatchingOn()) {

               _simpleUI_ImportLauncher_Viewer.getTable().setFocus();
            }

            if (isInStartUp) {

               setWatcher_1_On();

               updateUI_DeviceState_SimpleUI();
            }

            break;

         case FOSSIL:
         default:

            _topPage_PageBook.showPage(_topPage_ImportUI_FossilUI);

            _linkImport.setFocus();

            break;
         }
      }
   }

   /**
    * Set/create dashboard page.
    */
   private void updateUI_2_EasyImport_Fancy() {

      if (_easyImportFancy_PageBook == null) {

         /*
          * This occurs when the app is started the first time and the measurement selection dialog
          * fires an event
          */

         return;
      }

      final boolean isBrowserAvailable = _browser != null;

      // set dashboard page
      _easyImportFancy_PageBook.showPage(isBrowserAvailable
            ? _easyImportFancy_Page_WithBrowser
            : _easyImportFancy_Page_NoBrowser);

      if (isBrowserAvailable) {

         // the html is created completely for every UI update
         final String html = createHTML();

         _isBrowserCompleted = false;

         _browser.setText(html);
      }
   }

   /**
    * Update simple UI
    */
   private void updateUI_2_EasyImport_Simple() {

      _comboSimpleUI_Config.removeAll();

      fillSimpleUI();

      _comboSimpleUI_Config.select(getActiveEasyConfigSelectionIndex());
   }

   private void updateUI_DeviceState() {

      if (_parent.isDisposed()) {
         return;
      }

      // must be running in the UI thread, is called from other threads
      _parent.getDisplay().asyncExec(() -> {

         if (_importUI == ImportUI.EASY_IMPORT_FANCY) {

            if (_browser != null && _browser.isDisposed() == false) {

               if (_isBrowserCompleted) {
                  updateUI_DeviceState_DOM();
               } else {
                  _isDeviceStateUpdateDelayed.set(true);
               }
            }
         }

         // update simple UI always to be in sync with the fancy UI
         updateUI_DeviceState_SimpleUI();
      });
   }

   private void updateUI_DeviceState_DOM() {

// SET_FORMATTING_OFF

      final String htmlDeviceOnOff  = createHTML_52_Device_OnOff();
      String jsDeviceOnOff          = UI.replaceJS_QuotaMark(htmlDeviceOnOff);
      jsDeviceOnOff                 = UI.replaceHTML_NewLine(jsDeviceOnOff);

      final String htmlDeviceState  = createHTML_54_DeviceFolder();
      String jsDeviceState          = UI.replaceJS_QuotaMark(htmlDeviceState);
      jsDeviceState                 = UI.replaceHTML_NewLine(jsDeviceState);

// SET_FORMATTING_ON

      final String js = NL

            + "var htmlDeviceOnOff=\"" + jsDeviceOnOff + "\";" + NL //                                         //$NON-NLS-1$ //$NON-NLS-2$
            + "document.getElementById(\"" + DOM_ID_DEVICE_ON_OFF + "\").innerHTML = htmlDeviceOnOff;" + NL // //$NON-NLS-1$ //$NON-NLS-2$

            + "var htmlDeviceState =\"" + jsDeviceState + "\";" + NL //                                        //$NON-NLS-1$ //$NON-NLS-2$
            + "document.getElementById(\"" + DOM_ID_DEVICE_STATE + "\").innerHTML = htmlDeviceState;" + NL //  //$NON-NLS-1$ //$NON-NLS-2$
      ;

      final boolean isSuccess = _browser.execute(js);

      if (isSuccess == false) {
         System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //$NON-NLS-1$ //$NON-NLS-2$
               + ("\tupdateDOM_DeviceState: " + isSuccess + js)); //$NON-NLS-1$
      }
   }

   private void updateUI_DeviceState_SimpleUI() {

      final ToolItem deviceState_ActionToolItem = _actionSimpleUI_DeviceState.getActionToolItem();

      if (deviceState_ActionToolItem == null) {

         /*
          * This happend during development when closing the import view. It may be possible that
          * this UI was not yet displayed.
          */

         return;
      }

      /*
       * Device watching On/Off
       */
      final boolean isWatchingOn = isWatchingOn();

      String tooltip = isWatchingOn
            ? Messages.Import_Data_HTML_DeviceOff_Tooltip
            : Messages.Import_Data_HTML_DeviceOn_Tooltip;

      // show red image when off
      final String imageDescriptor = isWatchingOn
            ? Images.RawData_Device_TurnOn
            : Images.RawData_Device_TurnOff;

      _actionSimpleUI_StartStopWatching.setToolTipText(tooltip);
      _actionSimpleUI_StartStopWatching.setImageDescriptor(TourbookPlugin.getImageDescriptor(imageDescriptor));

      /*
       * Device state/folder
       */
      final EasyConfig easyConfig = getEasyConfig();
      final ImportConfig importConfig = easyConfig.getActiveImportConfig();

      final boolean isWatchAnything = importConfig.isWatchAnything();

      if (isWatchingOn == false) {

         // watching is off

         _actionSimpleUI_DeviceState.notSelectedTooltip = Messages.Import_Data_HTML_WatchingIsOff;
         deviceState_ActionToolItem.setImage(_images.get(IMAGE_DEVICE_FOLDER_OFF));

      } else if (isWatchAnything && _isDeviceStateValid) {

         updateUI_DeviceState_SimpleUI_IsValid();

      } else {

         /*
          * On startup, set the folder state without device info because this is retrieved in a
          * background thread, if not, it is blocking the UI !!!
          */

         final Image stateImage = isWatchAnything
               ? _images.get(IMAGE_DEVICE_FOLDER_IS_CHECKING)
               : _images.get(IMAGE_DEVICE_FOLDER_NOT_SETUP);

         tooltip = isWatchAnything
               ? Messages.Import_Data_HTML_AcquireDeviceInfo
               : Messages.Import_Data_HTML_NothingIsWatched;

         _actionSimpleUI_DeviceState.notSelectedTooltip = tooltip;
         deviceState_ActionToolItem.setImage(stateImage);
      }
   }

   private void updateUI_DeviceState_SimpleUI_IsValid() {

      final EasyConfig easyConfig = getEasyConfig();

      final ImportConfig importConfig = easyConfig.getActiveImportConfig();

      final String deviceOSFolder = importConfig.getDeviceOSFolder();
      final ArrayList<OSFile> notImportedFiles = easyConfig.notImportedFiles;

      final int numNotImportedFiles = notImportedFiles.size();

      final boolean isDeviceFolderOK = isOSFolderValid(deviceOSFolder);
      boolean isFolderOK = true;

      /*
       * Backup folder
       */
      final boolean isCreateBackup = importConfig.isCreateBackup;
      if (isCreateBackup) {

         // check OS folder
         final String backupOSFolder = importConfig.getBackupOSFolder();
         final boolean isBackupFolderOK = isOSFolderValid(backupOSFolder);
         isFolderOK &= isBackupFolderOK;
      }

      isFolderOK &= isDeviceFolderOK;

      /*
       * Update UI
       */
      final Image stateImage = isFolderOK
            ? _images.get(IMAGE_DEVICE_FOLDER_OK)
            : _images.get(IMAGE_DEVICE_FOLDER_ERROR);

      final String numNotImportedFilesAsText = isDeviceFolderOK
            ? Integer.toString(numNotImportedFiles)
            : UI.EMPTY_STRING;

      _actionSimpleUI_DeviceState.notSelectedTooltip = UI.EMPTY_STRING;
      _actionSimpleUI_DeviceState.getActionToolItem().setImage(stateImage);

      _lblSimpleUI_NumNotImportedFiles.setText(numNotImportedFilesAsText);

      // close state slideout to remove old content
      _actionSimpleUI_DeviceState.__slideoutDeviceState.close();
   }

   private void updateUI_ImportUI_Action() {

// SET_FORMATTING_OFF

      switch (_importUI) {

      case EASY_IMPORT_FANCY:

         _actionToggleImportUI.setImageDescriptor(          TourbookPlugin.getThemedImageDescriptor(Images.Import_UI_Easy_Fancy));

         break;

      case EASY_IMPORT_SIMPLE:

         _actionToggleImportUI.setImageDescriptor(          TourbookPlugin.getThemedImageDescriptor(Images.Import_UI_Easy_Simple));

         break;

      case FOSSIL:
      default:

         _actionToggleImportUI.setImageDescriptor(          TourbookPlugin.getThemedImageDescriptor(Images.Import_UI_Fossil));

         break;
      }

// SET_FORMATTING_ON
   }

   /**
    * Set the sort column direction indicator for a column.
    *
    * @param sortColumnId
    * @param isAscendingSort
    */
   private void updateUI_ShowSortDirection(final String sortColumnId, final int sortDirection) {

      final Table table = _tourViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(sortDirection == TourViewer_Comparator.ASCENDING ? SWT.UP : SWT.DOWN);
   }

   private void updateUI_TourViewerColumns() {

      // set tooltip text
      final String timeZone = _prefStore_Common.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID);

      final String timeZoneTooltip = NLS.bind(OtherMessages.COLUMN_FACTORY_TIME_ZONE_DIFF_TOOLTIP, timeZone);

      _timeZoneOffsetColDef.setColumnHeaderToolTipText(timeZoneTooltip);
   }

   private void updateUI_WatcherAnimation(final String domClassState) {

      if (_importUI != ImportUI.EASY_IMPORT_FANCY) {
         return;
      }

      if (_isShowWatcherAnimation
            && _browser != null
            && _browser.isDisposed() == false
            && _isBrowserCompleted) {

         _isShowWatcherAnimation = false;

         final String js = UI.EMPTY_STRING

               + "document.getElementById(\"" + DOM_ID_IMPORT_TILES + "\").className ='" + domClassState + "';" + NL //    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
               + "document.getElementById(\"" + DOM_ID_DEVICE_STATE + "\").className ='" + domClassState + "';" + NL //    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
               + "document.getElementById(\"" + DOM_ID_IMPORT_CONFIG + "\").className ='" + domClassState + "';" + NL //   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         ;

         _browser.execute(js);
      }
   }

   /**
    * when the active person was modified, the view must be updated
    */
   private void updateViewerPersonData() {

      _activePerson = TourbookPlugin.getActivePerson();

      // update person in save action
      enableActions();

      // update person in the raw data
      _rawDataMgr.updateTourData_InImportView_FromDb(null);

      _tourViewer.refresh();
   }
}
