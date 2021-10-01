/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.EmptyContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.map2.view.Map2View;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.web.WEB;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
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
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.progress.UIJob;
import org.geotools.data.ows.OperationType;
import org.geotools.data.ows.Service;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.ows.wms.WMSRequest;

public class PrefPage_Map2_Providers extends PreferencePage implements IWorkbenchPreferencePage, ITourViewer {

   private static final String           APP_TRUE   = net.tourbook.Messages.App__True;

   public static final String            ID         = "de.byteholder.geoclipse.preferences.PrefPage_Map2_Providers"; //$NON-NLS-1$

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state     = TourbookPlugin.getState(ID);

   //

   private static final String       STATE_SORT_COLUMN_DIRECTION    = "STATE_SORT_COLUMN_DIRECTION";   //$NON-NLS-1$
   private static final String       STATE_SORT_COLUMN_ID           = "STATE_SORT_COLUMN_ID";          //$NON-NLS-1$

   private static final String       CHARACTER_0                    = "0";                             //$NON-NLS-1$

   private static final String       XML_EXTENSION                  = ".xml";                          //$NON-NLS-1$

   /**
    * max lenghth for map provider id and offline folder
    */
   private static final int          MAX_ID_LENGTH                  = 24;

   private static final String       IMPORT_FILE_PATH               = "MapProvider_ImportFilePath";    //$NON-NLS-1$
   private static final String       EXPORT_FILE_PATH               = "MapProvider_ExportFilePath";    //$NON-NLS-1$

   private static final String       COLUMN_CATEGORY                = "Category";                      //$NON-NLS-1$
   private static final String       COLUMN_DESCRIPTION             = "Description";                   //$NON-NLS-1$
   private static final String       COLUMN_IS_CONTAINS_HILLSHADING = "ContainsHillshading";           //$NON-NLS-1$
   private static final String       COLUMN_IS_TRANSPARENT_LAYER    = "IsTransparentLayer";            //$NON-NLS-1$
   private static final String       COLUMN_IS_VISIBLE              = "IsVisible";                     //$NON-NLS-1$
   private static final String       COLUMN_MAP_PROVIDER_NAME       = "MapProviderName";               //$NON-NLS-1$
   private static final String       COLUMN_MODIFIED                = "Modified";                      //$NON-NLS-1$
   private static final String       COLUMN_MP_TYPE                 = "MPType";                        //$NON-NLS-1$
   private static final String       COLUMN_NUM_Layers              = "NumLayers";                     //$NON-NLS-1$
   private static final String       COLUMN_OFFLINE_FILE_COUNTER    = "OfflineFileCounter";            //$NON-NLS-1$
   private static final String       COLUMN_OFFLINE_FILE_SIZE       = "OfflineFileSize";               //$NON-NLS-1$
   private static final String       COLUMN_OFFLINE_FOLDER_NAME     = "OfflineFolderName";             //$NON-NLS-1$
   private static final String       COLUMN_ONLINE_MAP_URL          = "OnlineMapUrl";                  //$NON-NLS-1$
   private static final String       COLUMN_TILE_URL                = "TileUrl";                       //$NON-NLS-1$

   private static final NumberFormat _nf                            = NumberFormat.getNumberInstance();
   static {
      _nf.setMinimumFractionDigits(2);
      _nf.setMaximumFractionDigits(2);
      _nf.setMinimumIntegerDigits(1);
   }

   private TableViewer                         _mpViewer;
   private MapProviderComparator               _mpComparator           = new MapProviderComparator();
   private ColumnManager                       _columnManager;

   private final MapProviderManager            _mpManager              = MapProviderManager.getInstance();

   /**
    * contains all visible map providers
    */
   private ArrayList<MP>                       _visibleMp;

   /**
    * Map provider's which are used when getting offline info
    */
   private final ArrayList<MP>                 _offlineJobMapProviders = new ArrayList<>();
   private IOfflineInfoListener                _offlineJobInfoListener;
   private Job                                 _offlineJobGetInfo;
   private MP                                  _offlineJobMp;
   private int                                 _offlineJobFileCounter;
   private int                                 _offlineJobFileSize;
   private int                                 _offlineJobFileCounterUIUpdate;

   /**
    * Is <code>true</code> when the job is canceled
    */
   private boolean                             _isOfflineJobCanceled   = true;
   private boolean                             _isOfflineJobRunning;

   /**
    * Map provider which is currently selected in the list
    */
   private MP                                  _selectedMapProvider;
   private MP                                  _newMapProvider;

   private boolean                             _isNewMapProvider;
   private boolean                             _isModifiedMapProvider;
   private boolean                             _isModifiedMapProviderList;

   private boolean                             _isInUIUpdate;
   private boolean                             _isValid                = true;

   private ModifyListener                      _modifyListener;
   private SelectionListener                   _columnSortListener;

   private boolean                             _isModifiedOfflineFolder;
   private boolean                             _isModifiedMapProviderId;

   private ActionRefreshOfflineInfoSelected    _actionRefreshSelected;
   private ActionRefreshOfflineInfoAll         _actionRefreshAll;
   private ActionCancelRefreshOfflineInfo      _actionCancelRefresh;
   private ActionRefreshOfflineInfoNotAssessed _actionRefreshNotAssessed;

   private PixelConverter                      _pc;

   /*
    * UI controls
    */
   private Composite  _viewerContainer;
   private DropTarget _wmsDropTarget;

   private Group      _groupDetails;

   private DropTarget _mpDropTarget;

   private Button     _btnAddMapProviderCustom;
   private Button     _btnAddMapProviderWms;
   private Button     _btnAddMapProviderMapProfile;
   private Button     _btnCancel;
   private Button     _btnDeleteMapProvider;
   private Button     _btnDeleteOfflineMap;
   private Button     _btnEdit;
   private Button     _btnExport;
   private Button     _btnImport;
   private Button     _btnUpdate;

   private Button     _chkIsIncludesHillshading;
   private Button     _chkIsTransparentLayer;

   private Label      _lblDescription;
   private Label      _lblDropTarget;
   private Label      _lblMpDropTarget;
   private Label      _lblOfflineFolderInfo;
   private Label      _lblLayers;

   private Link       _linkOnlineMap;

   private Text       _txtCategory;
   private Text       _txtDescription;
   private Text       _txtMapProviderName;
   private Text       _txtMapProviderId;
   private Text       _txtMapProviderType;
   private Text       _txtOfflineFolder;
   private Text       _txtOfflineInfoTotal;
   private Text       _txtOnlineMapUrl;
   private Text       _txtLayers;

   private class MapContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _visibleMp.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   private class MapProviderComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = COLUMN_MAP_PROVIDER_NAME;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final boolean isDescending = __sortDirection == DESCENDING;

         final MP mp1 = (MP) e1;
         final MP mp2 = (MP) e2;

         long rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnId) {

         case COLUMN_CATEGORY:
            rc = mp1.getCategory().compareTo(mp2.getCategory());
            break;

         case COLUMN_DESCRIPTION:
            rc = mp1.getDescription().compareTo(mp2.getDescription());
            break;

         case COLUMN_IS_CONTAINS_HILLSHADING:

            rc = Boolean.compare(mp2.isIncludesHillshading(), mp1.isIncludesHillshading());
            break;

         case COLUMN_IS_TRANSPARENT_LAYER:

            rc = Boolean.compare(mp2.isTransparentLayer(), mp1.isTransparentLayer());
            break;

         case COLUMN_IS_VISIBLE:

            /*
             * Comparison is mp2->mp1 that the map providers are ascending sorted when visible is
             * ascending sorted
             */
            rc = Boolean.compare(mp2.isVisibleInUI(), mp1.isVisibleInUI());
            break;

         case COLUMN_MODIFIED:
            rc = mp1.getDateTimeModified_Long() - mp2.getDateTimeModified_Long();
            break;

         case COLUMN_MP_TYPE:
            rc = MapProviderManager.getMapProvider_TypeLabel(mp1).compareTo(MapProviderManager.getMapProvider_TypeLabel(mp2));
            break;

         case COLUMN_OFFLINE_FILE_COUNTER:
            rc = mp1.getOfflineFileCounter() - mp2.getOfflineFileCounter();
            break;

         case COLUMN_OFFLINE_FILE_SIZE:
            rc = mp1.getOfflineFileSize() - mp2.getOfflineFileSize();
            break;

         case COLUMN_OFFLINE_FOLDER_NAME:
            rc = mp1.getOfflineFolder().compareTo(mp2.getOfflineFolder());
            break;

         case COLUMN_ONLINE_MAP_URL:
            rc = mp1.getOnlineMapUrl().compareTo(mp2.getOnlineMapUrl());
            break;

         case COLUMN_TILE_URL:
            rc = MapProviderManager.getTileLayerInfo(mp1).compareTo(MapProviderManager.getTileLayerInfo(mp2));
            break;

         case COLUMN_NUM_Layers:

            if (mp1 instanceof MPWms && mp2 instanceof MPWms) {

               final MPWms wmsMP1 = (MPWms) mp1;
               final MPWms wmsMP2 = (MPWms) mp2;

               rc = wmsMP1.getAvailableLayers() - wmsMP2.getAvailableLayers();

            } else

            /**
             * This sorting is highly complicated:
             * <p>
             * - show map profiles at the top
             * - sort according to the number of layers
             * - do not show 1 layer for simple map providers
             * - second/third sorting should be category/mp name
             */
            if (mp1 instanceof MPProfile && mp2 instanceof MPProfile) {

               final MPProfile profileMP1 = (MPProfile) mp1;
               final MPProfile profileMP2 = (MPProfile) mp2;

               rc = profileMP1.getLayers() - profileMP2.getLayers();

            } else if (mp1 instanceof MPProfile) {

               if (isDescending) {
                  rc = +1;
               } else {
                  rc = -1;
               }

            } else if (mp2 instanceof MPProfile) {

               if (isDescending) {
                  rc = -1;
               } else {
                  rc = +1;
               }
            }

            break;

         case COLUMN_MAP_PROVIDER_NAME:
         default:
            rc = mp1.getName().compareTo(mp2.getName());
         }

         if (rc == 0) {

            // subsort 1 by category
            rc = mp1.getCategory().compareTo(mp2.getCategory());

            // subsort 2 by map provider
            if (rc == 0) {
               rc = mp1.getName().compareTo(mp2.getName());
            }
         }

         // if descending order, flip the direction
         if (isDescending) {
            rc = -rc;
         }

         /*
          * MUST return 1 or -1 otherwise long values are not sorted correctly.
          */
         return rc > 0 //
               ? 1
               : rc < 0 //
                     ? -1
                     : 0;
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {

         // force resorting when a name is renamed
         return true;
      }

      public void setSortColumn(final Widget widget) {

         final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
         final String columnId = columnDefinition.getColumnId();

         if (columnId == null) {
            return;
         }

         if (columnId.equals(__sortColumnId)) {

            // Same column as last sort -> select next sorting

            switch (__sortDirection) {
            case ASCENDING:
               __sortDirection = DESCENDING;
               break;

            case DESCENDING:
            default:
               __sortDirection = ASCENDING;
               break;
            }

         } else {

            // New column; do an ascent sorting

            __sortColumnId = columnId;
            __sortDirection = ASCENDING;
         }

         updateUI_SetSortDirection(__sortColumnId, __sortDirection);
      }
   }

   public PrefPage_Map2_Providers() {

      noDefaultAndApplyButton();

      _modifyListener = modifyEvent -> {
         if (_isInUIUpdate == false) {
            setMapProviderModified();
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

      _offlineJobInfoListener = mapProvider -> Display.getDefault().asyncExec(() -> {

         if ((_mpViewer == null) || _mpViewer.getTable().isDisposed()) {
            return;
         }

         _mpViewer.update(mapProvider, null);

         updateUI_OfflineInfo_Total();
      });

      MP.addOfflineInfoListener(_offlineJobInfoListener);
   }

   private String checkMapProviderId(final String factoryId) {

      String error = null;
      if ((factoryId == null) || (factoryId.length() == 0)) {
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

      if ((offlineFolder == null) || (offlineFolder.length() == 0)) {

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

         final IPath tileCacheBasePath = MapProviderManager.getTileCachePath();
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

      _isInUIUpdate = true;
      {
         if (_isModifiedMapProviderId == false) {
            _txtMapProviderId.setText(validText);
         }

         if (_isModifiedOfflineFolder == false) {
            _txtOfflineFolder.setText(validText);
         }
      }
      _isInUIUpdate = false;
   }

   @Override
   protected Control createContents(final Composite parent) {

      _visibleMp = _mpManager.getAllMapProviders(true);

      addListener();

      initUI(parent);
      initializeDialogUnits(parent);

      restoreState_BeforeUI();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      final Composite container = createUI(parent);

      // load viewer
      _mpViewer.setInput(new Object());

      restoreState();

      updateUI_OfflineInfo_Total();

      _mpViewer.getTable().setFocus();

      return container;
   }

//   private String checkBaseUrl(final String baseUrl) {
//
//      if (baseUrl == null || baseUrl.length() == 0) {
//         return Messages.pref_map_validationError_baseUrlIsRequired;
//      } else {
//
//         try {
//            new URL(baseUrl);
//         } catch (final MalformedURLException e) {
//            return Messages.pref_map_validationError_invalidUrl;
//         }
//      }
//
//      return null;
//   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).spacing(5, 0).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
      {
         createUI_10_Header(container);
         new Label(container, SWT.NONE);

         _viewerContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
         GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
         {
            createUI_20_Viewer(_viewerContainer);
         }

         createUI_30_Buttons(container);

         createUI_40_ReadTileSize(container);
         new Label(container, SWT.NONE);

         createUI_50_Details(container);
      }

      // spacer
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 20).applyTo(label);

      return container;
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite headerContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(headerContainer);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(headerContainer);
//         headerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         /*
          * label: map provider
          */
         final Label label = new Label(headerContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(label);
         label.setText(Messages.Pref_Map_Label_AvailableMapProvider);

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

   private void createUI_20_Viewer(final Composite parent) {

      // set colors for the viewer
      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
      final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

      /*
       * Create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults()//
            .hint(600, _pc.convertHeightInCharsToPixels(10))
            .grab(true, true)
            .applyTo(table);

      table.setLayout(new TableLayout());
      table.setHeaderVisible(true);
      table.setLinesVisible(false);
      table.setHeaderBackground(bgColor);
      table.setHeaderForeground(fgColor);

      table.addKeyListener(new KeyListener() {

         @Override
         public void keyPressed(final KeyEvent e) {

            if (e.keyCode == SWT.DEL) {

               /*
                * Delete map provider only when the delete button is enabled, otherwise internal map
                * providers can be deleted which is not good
                */
               if (_btnDeleteMapProvider.isEnabled()) {

                  onAction_MapProvider_Delete();
               }
            }
         }

         @Override
         public void keyReleased(final KeyEvent e) {}
      });

      net.tourbook.ui.UI.setTableSelectionColor(table);

      /*
       * Create table viewer
       */
      _mpViewer = new TableViewer(table);
      _mpViewer.setUseHashlookup(true);

      _mpViewer.setContentProvider(new MapContentProvider());
      _mpViewer.setComparator(_mpComparator);

      _columnManager.createColumns(_mpViewer);
      _columnManager.createHeaderContextMenu(table, new EmptyContextMenuProvider());

      _mpViewer.addSelectionChangedListener(this::onSelect_MapProvider);

      _mpViewer.addDoubleClickListener(doubleClickEvent -> {

         if (_selectedMapProvider instanceof MPWms) {

            openConfigDialog_Wms();

         } else if (_selectedMapProvider instanceof MPCustom) {

            openConfigDialog_Custom();

         } else if (_selectedMapProvider instanceof MPProfile) {

            openConfigDialog_MapProfile();

         } else {

            // select name
            _txtMapProviderName.setFocus();
            _txtMapProviderName.selectAll();
         }
      });

      updateUI_SetSortDirection(_mpComparator.__sortColumnId, _mpComparator.__sortDirection);

      // draw dark theme for the table
      net.tourbook.common.UI.setChildColors(table, fgColor, bgColor);
   }

   private void createUI_30_Buttons(final Composite container) {

      final Composite btnContainer = new Composite(container, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(btnContainer);
      GridLayoutFactory.fillDefaults().applyTo(btnContainer);
      {
         {
            /*
             * Button: edit
             */
            _btnEdit = new Button(btnContainer, SWT.NONE);
            _btnEdit.setText(Messages.Pref_Map_Button_Edit);
            _btnEdit.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  openConfigDialog();
               }
            });
            GridDataFactory.fillDefaults()//
                  .grab(true, false)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_btnEdit);
            setButtonLayoutData(_btnEdit);
         }
         {
            /*
             * Button: add custom map provider
             */
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
         }
         {
            /*
             * Button: add wms map provider
             */
            _btnAddMapProviderWms = new Button(btnContainer, SWT.NONE);
            _btnAddMapProviderWms.setText(Messages.Pref_Map_Button_AddMapProviderWms);
            _btnAddMapProviderWms.setToolTipText(Messages.Pref_Map_Button_AddMapProviderWms_Tooltip);
            _btnAddMapProviderWms.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onAction_MapProvider_AddWms();
               }
            });
            setButtonLayoutData(_btnAddMapProviderWms);
         }
         {
            /*
             * Button: add profile map provider
             */
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
         }
         {
            /*
             * WMS drag&drop target
             */
            _lblDropTarget = new Label(btnContainer, SWT.BORDER | SWT.WRAP | SWT.CENTER);
            _lblDropTarget.setText(Messages.Pref_Map_Label_WmsDropTarget);
            _lblDropTarget.setToolTipText(Messages.Pref_Map_Label_WmsDropTarget_Tooltip);
            GridDataFactory.fillDefaults().applyTo(_lblDropTarget);
            setWmsDropTarget(_lblDropTarget);
         }
         {
            /*
             * Button: delete offline map
             */
            _btnDeleteOfflineMap = new Button(btnContainer, SWT.NONE);
            _btnDeleteOfflineMap.setText(Messages.Pref_Map_Button_DeleteOfflineMap);
            _btnDeleteOfflineMap.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  deleteOfflineMap(_selectedMapProvider);
               }
            });
            setButtonLayoutData(_btnDeleteOfflineMap);
         }
         {
            /*
             * Button: import
             */
            _btnImport = new Button(btnContainer, SWT.NONE);
            _btnImport.setText(Messages.Pref_Map_Button_ImportMP);
            _btnImport.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onAction_MapProvider_Import();
               }
            });
            setButtonLayoutData(_btnImport);
            final GridData gd = setButtonLayoutData(_btnImport);
            gd.verticalIndent = 20;
         }
         {
            /*
             * Button: export
             */
            _btnExport = new Button(btnContainer, SWT.NONE);
            _btnExport.setText(Messages.Pref_Map_Button_ExportMP);
            _btnExport.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onAction_MapProvider_Export();
               }
            });
            setButtonLayoutData(_btnExport);
         }
         {
            /*
             * Drop target: Map provider
             */
            _lblMpDropTarget = new Label(btnContainer, SWT.BORDER | SWT.WRAP | SWT.CENTER);
            _lblMpDropTarget.setText(Messages.Pref_Map_Label_MapProviderDropTarget);
            _lblMpDropTarget.setToolTipText(Messages.Pref_Map_Label_MapProviderDropTarget_Tooltip);
            GridDataFactory.fillDefaults().applyTo(_lblMpDropTarget);

            _mpDropTarget = new DropTarget(_lblMpDropTarget, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);

            _mpDropTarget.setTransfer(URLTransfer.getInstance(), FileTransfer.getInstance());

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
                  Display.getDefault().asyncExec(() -> runnableDropMapProvider(event));
               }
            });
         }
         {
            /*
             * link: map provider support
             */
            final Link link = new Link(btnContainer, SWT.NONE);
            link.setText(Messages.Pref_Map_Link_MapProvider);
            link.setToolTipText(Messages.Pref_Map_Link_MapProvider_Tooltip);
            link.setEnabled(true);
            link.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  WEB.openUrl(Messages.External_Link_MapProviders);
               }
            });
            GridDataFactory.fillDefaults()//
                  .align(SWT.END, SWT.FILL)
                  .grab(true, false)
                  .applyTo(link);
         }
         {
            /*
             * button: delete map provider
             */
            _btnDeleteMapProvider = new Button(btnContainer, SWT.NONE);
            _btnDeleteMapProvider.setText(Messages.Pref_Map_Button_DeleteMapProvider);
            _btnDeleteMapProvider.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onAction_MapProvider_Delete();
               }
            });
            final GridData gd = setButtonLayoutData(_btnDeleteMapProvider);
            gd.grabExcessVerticalSpace = true;
            gd.verticalAlignment = SWT.END;
            gd.verticalIndent = 20;
         }

      }
   }

   private void createUI_40_ReadTileSize(final Composite parent) {

      /*
       * text: offline info
       */
      _txtOfflineInfoTotal = new Text(parent, SWT.READ_ONLY | SWT.TRAIL);
      GridDataFactory.fillDefaults().applyTo(_txtOfflineInfoTotal);
      _txtOfflineInfoTotal.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
   }

   private void createUI_50_Details(final Composite parent) {

      _groupDetails = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).span(2, 1).applyTo(_groupDetails);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupDetails);
      _groupDetails.setText(Messages.Pref_Map_Group_Detail_SelectedMapProvider);
      {
         createUI_52_Details_Details(_groupDetails);
         createUI_54_Details_Buttons(_groupDetails);
      }
   }

   private void createUI_52_Details_Details(final Group parent) {

      final VerifyListener verifyListener = verifyEvent -> verifyEvent.text = UI.createIdFromName(verifyEvent.text, MAX_ID_LENGTH);

      final SelectionListener selectionListener = widgetSelectedAdapter(selectionEvent -> setMapProviderModified());

      final int secondColumnIndent = 20;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
      {
         {
            /*
             * Map provider name
             */

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_Map_Label_MapProvider);

            // text: map provider
            _txtMapProviderName = new Text(container, SWT.BORDER);
            _txtMapProviderName.addModifyListener(modifyEvent -> {
               if (_isInUIUpdate) {
                  return;
               }
               createAutoText();
               setMapProviderModified();
            });
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_txtMapProviderName);
         }
         {
            /*
             * Common category
             */

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_Map_Label_Category);

            // text: map provider
            _txtCategory = new Text(container, SWT.BORDER);
            _txtCategory.addModifyListener(_modifyListener);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_txtCategory);
         }
         {
            /*
             * Online map
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_Map_Label_OnlineMap);

            // text
            _txtOnlineMapUrl = new Text(container, SWT.BORDER);
            _txtOnlineMapUrl.addModifyListener(modifyEvent -> {

               if (_isInUIUpdate) {
                  return;
               }

               // update link text
               _linkOnlineMap.setText(net.tourbook.common.UI.getLinkFromText(_txtOnlineMapUrl.getText()));
               _linkOnlineMap.setToolTipText(_txtMapProviderName.getText());

               _isModifiedOfflineFolder = true;
               setMapProviderModified();
            });
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .grab(true, false)
                  .applyTo(_txtOnlineMapUrl);

            // spacer
            new Label(container, SWT.NONE);

            // link
            _linkOnlineMap = new Link(container, SWT.NONE);
            _linkOnlineMap.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  WEB.openUrl(_txtOnlineMapUrl.getText());
               }
            });
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .grab(true, false)
                  .applyTo(_linkOnlineMap);
         }
         {
            /*
             * Description
             */

            // label: description
            _lblDescription = new Label(container, SWT.NONE);
            _lblDescription.setText(Messages.Pref_Map_Label_Description);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(_lblDescription);

            // text: description
            _txtDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
            _txtDescription.addModifyListener(_modifyListener);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .hint(_pc.convertWidthInCharsToPixels(50), _pc.convertHeightInCharsToPixels(5))
                  .grab(true, false)
                  .applyTo(_txtDescription);
         }
         {
            /*
             * Layers
             */

            // label
            _lblLayers = new Label(container, SWT.NONE);
            _lblLayers.setText(Messages.Pref_Map_Label_Layers);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(_lblLayers);

            // text
            _txtLayers = new Text(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .hint(_pc.convertWidthInCharsToPixels(50), _pc.convertHeightInCharsToPixels(3))
                  .grab(true, false)
                  .applyTo(_txtLayers);
         }
         {
            /*
             * Offline folder
             */

            // label: offline folder
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_Map_Label_OfflineFolder);

            // text: offline folder
            _txtOfflineFolder = new Text(container, SWT.BORDER);
            _txtOfflineFolder.setTextLimit(MAX_ID_LENGTH);
            _txtOfflineFolder.addVerifyListener(verifyListener);
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(30), SWT.DEFAULT)
                  .applyTo(_txtOfflineFolder);

            _txtOfflineFolder.addModifyListener(modifyEvent -> {

               if (_isInUIUpdate) {
                  return;
               }

               _isModifiedOfflineFolder = true;
               setMapProviderModified();

               // force offline folder to be reloaded
               _selectedMapProvider.setStateToReloadOfflineCounter();
            });

            // label: offline info
            _lblOfflineFolderInfo = new Label(container, SWT.TRAIL);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .grab(true, false)
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(_lblOfflineFolderInfo);
         }
         {
            /*
             * Unique id
             */

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_Map_Label_MapProviderId);

            // text: map provider id
            _txtMapProviderId = new Text(container, SWT.BORDER);
            _txtMapProviderId.setTextLimit(MAX_ID_LENGTH);
            _txtMapProviderId.addVerifyListener(verifyListener);
            GridDataFactory.fillDefaults().applyTo(_txtMapProviderId);

            _txtMapProviderId.addModifyListener(modifyEvent -> {
               if (_isInUIUpdate) {
                  return;
               }

               _isModifiedMapProviderId = true;
               setMapProviderModified();
            });
         }
         {
            /*
             * Checkbox: Includes topo
             */

            _chkIsIncludesHillshading = new Button(container, SWT.CHECK);
            _chkIsIncludesHillshading.setText(Messages.Pref_Map_Checkbox_IncludeHillshading);
            _chkIsIncludesHillshading.addSelectionListener(selectionListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .indent(secondColumnIndent, 0)
                  .applyTo(_chkIsIncludesHillshading);
         }
         {
            /*
             * Map provider type
             */

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_Map_Label_MapProviderType);

            // text: map provider type
            _txtMapProviderType = new Text(container, SWT.BORDER | SWT.READ_ONLY);
            _txtMapProviderType.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            GridDataFactory.fillDefaults().applyTo(_txtMapProviderType);
         }
         {
            /*
             * Checkbox: Is Layer
             */

            _chkIsTransparentLayer = new Button(container, SWT.CHECK);
            _chkIsTransparentLayer.setText(Messages.Pref_Map_Checkbox_IsTransparentLayer);
            _chkIsTransparentLayer.setToolTipText(Messages.Pref_Map_Checkbox_IsTransparentLayer_Tooltip);
            _chkIsTransparentLayer.addSelectionListener(selectionListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .indent(secondColumnIndent, 0)
                  .applyTo(_chkIsTransparentLayer);
         }
      }
   }

   private void createUI_54_Details_Buttons(final Group parent) {

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
               onAction_MapProvider_Update();
            }
         });
         setButtonLayoutData(_btnUpdate);

         // button: cancel
         _btnCancel = new Button(btnContainer, SWT.NONE);
         _btnCancel.setText(Messages.Pref_Map_Button_CancelMapProvider);
         _btnCancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onAction_MapProvider_Cancel();
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
   private void createWmsMapProvider(final String capsUrl) {

      // show loading... message
      setMessage(NLS.bind(Messages.pref_map_message_loadingWmsCapabilities, capsUrl), INFORMATION);

      // force message to be displayed
      final Shell shell = getShell();
      shell.redraw();
      shell.update();

      final MPWms wmsMapProvider = MapProviderManager.checkWms(null, capsUrl);
      if (wmsMapProvider == null) {

         // an error occurred, exception is already displayed

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

         _mpManager.addMapProvider(wmsMapProvider);

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

         _isInUIUpdate = true;
         {
            /*
             * set map provider fields
             */

            _txtMapProviderName.setText(providerName);
            _txtMapProviderId.setText(uniqueId);
            _txtOfflineFolder.setText(uniqueId);
            _txtDescription.setText(wmsAbstract);
            _txtLayers.setText(capsUrl);

            _lblOfflineFolderInfo.setText(UI.EMPTY_STRING);

            _chkIsIncludesHillshading.setSelection(false);
            _chkIsTransparentLayer.setSelection(false);
         }
         _isInUIUpdate = false;

         enableControls();

         errorControl.setFocus();
         if (errorControl instanceof Text) {
            ((Text) errorControl).selectAll();
         }
      }
   }

   /**
    * Create all columns
    */
   private void defineAllColumns() {

      defineColumn_00_IsVisible();
      defineColumn_10_MapProviderName();
      defineColumn_12_Category();
      defineColumn_22_Hillshading();
      defineColumn_24_TransparentLayer();
      defineColumn_30_MPType();
      defineColumn_38_TileUrl();
      defineColumn_40_OnlineMapUrl();
      defineColumn_50_Description();
      defineColumn_60_Layers();
      defineColumn_62_OfflinePath();
      defineColumn_70_OfflineFileCounter();
      defineColumn_72_OfflineFileSize();
      defineColumn_80_Modified();
   }

   /**
    * Column: Is visible
    */
   private void defineColumn_00_IsVisible() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_IS_VISIBLE, SWT.CENTER);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_IsVisible);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_IsVisible_Tooltip);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(mapProvider.isVisibleInUI() ? APP_TRUE : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Map provider
    */
   private void defineColumn_10_MapProviderName() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_MAP_PROVIDER_NAME, SWT.LEAD);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_MapProvider);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_MapProvider);

      colDef.setCanModifyVisibility(false);
      colDef.setIsColumnMoveable(false);
      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(30));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setImage(MapProviderManager.getMapProvider_TypeImage(mapProvider));
            cell.setText(mapProvider.getName());
         }
      });
   }

   /**
    * Column: Category
    */
   private void defineColumn_12_Category() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_CATEGORY, SWT.LEAD);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_Category);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_Category);
      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(16));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(mapProvider.getCategory());
         }
      });
   }

   /**
    * Column: Includes hillshading
    */
   private void defineColumn_22_Hillshading() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_IS_CONTAINS_HILLSHADING, SWT.LEAD);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_IsHillshading);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_IsHillshading_Tooltip);
      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(mapProvider.isIncludesHillshading() ? APP_TRUE : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Is transparent layer
    */
   private void defineColumn_24_TransparentLayer() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_IS_TRANSPARENT_LAYER, SWT.LEAD);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_IsTransparent);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_IsTransparent_Tooltip);
      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(mapProvider.isTransparentLayer() ? APP_TRUE : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: MP type
    */
   private void defineColumn_30_MPType() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_MP_TYPE, SWT.LEAD);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_MPType);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_MPType_Tooltip);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(MapProviderManager.getMapProvider_TypeLabel(mapProvider));
         }
      });
   }

   /**
    * Column: Tile url
    */
   private void defineColumn_38_TileUrl() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_TILE_URL, SWT.LEAD);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_TileUrl);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_TileUrl);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(16));
      colDef.setIsDefaultColumn();

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            final String tileLayerInfo = MapProviderManager.getTileLayerInfo(mapProvider)

                  // show profile url more readable
                  .replace(UI.NEW_LINE, UI.DASH_WITH_SPACE);

            cell.setText(tileLayerInfo);
         }
      });
   }

   /**
    * Column: Online map url
    */
   private void defineColumn_40_OnlineMapUrl() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_ONLINE_MAP_URL, SWT.LEAD);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_OnlineMapUrl);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_OnlineMapUrl);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((MP) cell.getElement()).getOnlineMapUrl());
         }
      });
   }

   /**
    * Column: Description
    */
   private void defineColumn_50_Description() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_DESCRIPTION, SWT.LEAD);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_Description);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_Description);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((MP) cell.getElement()).getDescription());
         }
      });
   }

   /**
    * Column: Number of layers
    */
   private void defineColumn_60_Layers() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_NUM_Layers, SWT.TRAIL);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_Layers);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_Layers);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(6));
      colDef.setIsDefaultColumn();

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            String numLayers = UI.EMPTY_STRING;

            final MP mapProvider = (MP) cell.getElement();
            if (mapProvider instanceof MPWms) {

               final MPWms wmsMP = (MPWms) mapProvider;

               numLayers = Integer.toString(wmsMP.getAvailableLayers());

            } else if (mapProvider instanceof MPProfile) {

               final MPProfile profileMP = (MPProfile) mapProvider;

               numLayers = Integer.toString(profileMP.getLayers());

            }
            cell.setText(numLayers);
         }
      });
   }

   /**
    * Column: offline path
    */
   private void defineColumn_62_OfflinePath() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_OFFLINE_FOLDER_NAME, SWT.LEAD);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_OfflinePath);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_OfflinePath);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((MP) cell.getElement()).getOfflineFolder());
         }
      });
   }

   /**
    * Column: Offline file counter
    */
   private void defineColumn_70_OfflineFileCounter() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_OFFLINE_FILE_COUNTER, SWT.TRAIL);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_OfflineFileCounter);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_OfflineFileCounter);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
      colDef.setIsDefaultColumn();

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final int offlineTileCounter = ((MP) cell.getElement()).getOfflineFileCounter();

            if (offlineTileCounter == MP.OFFLINE_INFO_NOT_READ) {
               cell.setText(Messages.pref_map_label_NA);
            } else if (offlineTileCounter > 0) {
               cell.setText(Integer.toString(offlineTileCounter));
            } else {
               cell.setText(UI.DASH_WITH_SPACE);
            }
         }
      });
   }

   /**
    * Column: Offline file size
    */
   private void defineColumn_72_OfflineFileSize() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_OFFLINE_FILE_SIZE, SWT.TRAIL);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_OfflineFileSize);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_OfflineFileSize);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
      colDef.setIsDefaultColumn();

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final long offlineTileSize = ((MP) cell.getElement()).getOfflineFileSize();

            if (offlineTileSize == MP.OFFLINE_INFO_NOT_READ) {
               cell.setText(Messages.pref_map_label_NA);
            } else if (offlineTileSize > 0) {
               cell.setText(_nf.format((float) offlineTileSize / 1024 / 1024));
            } else {
               cell.setText(UI.DASH_WITH_SPACE);
            }
         }
      });
   }

   /**
    * Column: Modified
    */
   private void defineColumn_80_Modified() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_MODIFIED, SWT.LEAD);

      colDef.setColumnName(Messages.Pref_Map2_Viewer_Column_Modified);
      colDef.setColumnHeaderToolTipText(Messages.Pref_Map2_Viewer_Column_Modified);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ZonedDateTime dtModified = ((MP) cell.getElement()).getDateTimeModified();

            if (dtModified == null) {
               cell.setText(UI.SPACE);
            } else {
               cell.setText(dtModified.format(TimeTools.Formatter_DateTime_S));
            }
         }
      });
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

      if (MapProviderManager.deleteOfflineMap(mp, false)) {

         mp.setStateToReloadOfflineCounter();

         // update viewer
         _mpViewer.update(mp, null);

         updateUI_OfflineInfo_Total();

         // clear map image cache
         mp.disposeTileImages();
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

         // update model
         _visibleMp.addAll(importedMPs);

         // update viewer
         _mpViewer.add(importedMPs.toArray(new MP[importedMPs.size()]));

         // select map provider in the viewer
         _mpViewer.setSelection(new StructuredSelection(importedMPs.get(0)), true);

         MapProviderManager.getInstance().writeMapProviderXml();
      }

      _mpViewer.getTable().setFocus();

// this annoying
//      if (importedMPs != null) {
//         // show the imported map provider in the config dialog
//         openConfigDialog();
//      }
   }

   private void doImportMP_Multiple(final ArrayList<String> allFilesPaths) {

      boolean isImported = false;
      MP firstMP = null;

      // loop: all import files
      for (final String importFilePath : allFilesPaths) {

         // create map provider from xml file
         final ArrayList<MP> importedMPs = MapProviderManager.getInstance().importMapProvider(importFilePath);

         if (importedMPs != null) {

            if (firstMP == null) {
               firstMP = importedMPs.get(0);
            }

            // update model
            _visibleMp.addAll(importedMPs);

            // update viewer
            _mpViewer.add(importedMPs.toArray(new MP[importedMPs.size()]));

            isImported = true;
         }
      }

      if (isImported) {

         MapProviderManager.getInstance().writeMapProviderXml();

         // select map provider in the viewer
         _mpViewer.setSelection(new StructuredSelection(firstMP), true);

         _mpViewer.getTable().setFocus();
      }
   }

   private void enableControls() {

      // focus get's lost when a map provider is modified the first time
      final Control focusControl = Display.getCurrent().getFocusControl();

      /*
       * validate UI data
       */
      validateMapProvider(_txtMapProviderName.getText().trim(),
            _txtMapProviderId.getText().trim(),
            _txtOfflineFolder
                  .getText()
                  .trim());

      final boolean isMapProvider = _selectedMapProvider != null;

      final boolean isExistingMapProvider = isMapProvider
            && (_isNewMapProvider == false)
            && (_isModifiedMapProvider == false);

      final boolean isOfflineJobStopped = _isOfflineJobRunning == false;

      final boolean canDeleteOfflineMap = isMapProvider
            && (_selectedMapProvider.getOfflineFileCounter() > 0)
            && isOfflineJobStopped;

      final boolean isOfflinePath = MapProviderManager.getTileCachePath() != null;

      final boolean isCustomMapProvider = _selectedMapProvider instanceof MPCustom;
      final boolean isMapProfile = _selectedMapProvider instanceof MPProfile;
      final boolean isWmsMapProvider = _selectedMapProvider instanceof MPWms;

      final boolean isNonePluginMapProvider = isCustomMapProvider || isWmsMapProvider || isMapProfile;
      final boolean isNoProfileMapProvider = isCustomMapProvider || isWmsMapProvider;
      final boolean canEditFields = _isNewMapProvider || isNonePluginMapProvider;

      _mpViewer.getTable().setEnabled(isExistingMapProvider);

      _chkIsIncludesHillshading.setEnabled(canEditFields);
      _chkIsTransparentLayer.setEnabled(canEditFields && isNoProfileMapProvider);
      _txtCategory.setEnabled(canEditFields);
      _txtDescription.setEnabled(canEditFields);
      _txtMapProviderId.setEnabled(canEditFields);
      _txtMapProviderName.setEnabled(canEditFields);
      _txtOfflineFolder.setEnabled(canEditFields);
      _txtOnlineMapUrl.setEnabled(canEditFields);

      // map provider list actions
      _btnAddMapProviderCustom.setEnabled(isExistingMapProvider);
      _btnAddMapProviderWms.setEnabled(isExistingMapProvider);
      _btnAddMapProviderMapProfile.setEnabled(isExistingMapProvider);
      _btnEdit.setEnabled(isExistingMapProvider && isNonePluginMapProvider);
      _btnDeleteMapProvider.setEnabled(isExistingMapProvider && isNonePluginMapProvider);
      _btnDeleteOfflineMap.setEnabled(isExistingMapProvider && canDeleteOfflineMap);
      _btnImport.setEnabled(isExistingMapProvider);
      _btnExport.setEnabled(isExistingMapProvider && isNonePluginMapProvider);

      _lblDropTarget.setEnabled((_isModifiedMapProvider == false) && (_isNewMapProvider == false));
      _lblMpDropTarget.setEnabled((_isModifiedMapProvider == false) && (_isNewMapProvider == false));

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

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
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
         updateUI_OfflineInfo();
      }

      for (final File file : listOfFiles) {
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
    * @return Returns the next map provider or <code>null</code> when there is no WMS map provider
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
    * @param sortColumnId
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _mpViewer.getTable().getColumns();

      for (final TableColumn column : allColumns) {

         final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

         if (columnId != null && columnId.equals(sortColumnId)) {
            return column;
         }
      }

      return allColumns[0];
   }

   @Override
   public ColumnViewer getViewer() {
      return _mpViewer;
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _columnSortListener = widgetSelectedAdapter(this::onSelect_SortColumn);
   }

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

      InputStream inputStream = null;

      try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFilePathName))) {

         final URL url = new URL(address);
         final URLConnection urlConnection = url.openConnection();
         inputStream = urlConnection.getInputStream();

         final byte[] buffer = new byte[1024];
         int numRead;

         while ((numRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, numRead);
         }

         return true;

      } catch (final Exception e) {
         StatusUtil.showStatus(e.getMessage(), e);
      } finally {

         try {
            if (inputStream != null) {
               inputStream.close();
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

   private void onAction_MapProvider_AddWms() {

      final IInputValidator inputValidator = newText -> {
         try {
            // check url
            new URL(newText);
         } catch (final MalformedURLException e) {
            return Messages.Wms_Error_InvalidUrl;
         }

         return null;
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
   private void onAction_MapProvider_Cancel() {

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

   private void onAction_MapProvider_Delete() {

      final int selectionIndex = _mpViewer.getTable().getSelectionIndex();
      if (selectionIndex < 0) {
         // nothing is selected
         return;
      }

      if (MessageDialog.openConfirm(
            Display.getCurrent().getActiveShell(),
            Messages.pref_map_dlg_confirmDeleteMapProvider_title,
            NLS.bind(Messages.pref_map_dlg_confirmDeleteMapProvider_message, _selectedMapProvider.getName())) == false) {

         return;
      }

      // get map provider which will be selected when the current will be removed
      Object nextSelectedMapProvider = _mpViewer.getElementAt(selectionIndex + 1);
      if (nextSelectedMapProvider == null) {
         nextSelectedMapProvider = _mpViewer.getElementAt(selectionIndex - 1);
      }

      // delete offline files
      deleteOfflineMapFiles(_selectedMapProvider);

      // remove from viewer
      _mpViewer.remove(_selectedMapProvider);

      // remove from model
      _mpManager.remove(_selectedMapProvider);
      _visibleMp.remove(_selectedMapProvider);

      // select another map provider at the same position
      if (nextSelectedMapProvider != null) {
         _mpViewer.setSelection(new StructuredSelection(nextSelectedMapProvider));
         _mpViewer.getTable().setFocus();
      }

      // custom map provider list must be updated
      MapProviderManager.getInstance().writeMapProviderXml();
   }

   private void onAction_MapProvider_Export() {

      final FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
      dialog.setText(Messages.Pref_Map_Dialog_Export_Title);

      dialog.setFilterPath(_prefStore.getString(EXPORT_FILE_PATH));

      dialog.setFilterExtensions(new String[] { "*.*", "xml" });//$NON-NLS-1$ //$NON-NLS-2$
      dialog.setFilterNames(new String[] {
            Messages.PrefPageMapProviders_Pref_Map_FileDialog_AllFiles,
            Messages.PrefPageMapProviders_Pref_Map_FileDialog_XmlFiles });

      final ZonedDateTime today = TimeTools.now();

      // add leading 0 when necessary
      final String month = CHARACTER_0 + Integer.toString(today.getMonthValue());
      final String day = CHARACTER_0 + Integer.toString(today.getDayOfMonth());

      final String currentDate = //
            UI.DASH
                  + Integer.toString(today.getYear())
                  + UI.DASH
                  + month.substring(month.length() - 2, month.length())
                  + UI.DASH
                  + day.substring(day.length() - 2, day.length());

      dialog.setFileName(_selectedMapProvider.getId() + currentDate + XML_EXTENSION);

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

   private void onAction_MapProvider_Import() {

      final FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);

      fileDialog.setText(Messages.Pref_Map_Dialog_Import_Title);
      fileDialog.setFilterPath(_prefStore.getString(IMPORT_FILE_PATH));

      fileDialog.setFilterExtensions(new String[] { "*.*", "xml" });//$NON-NLS-1$ //$NON-NLS-2$
      fileDialog.setFilterNames(new String[] {
            Messages.PrefPageMapProviders_Pref_Map_FileDialog_AllFiles,
            Messages.PrefPageMapProviders_Pref_Map_FileDialog_XmlFiles });

      fileDialog.setFileName(_selectedMapProvider.getId() + XML_EXTENSION);

      // open file dialog
      final String firstFilePathName = fileDialog.open();

      // check if user canceled the dialog
      if (firstFilePathName == null) {
         return;
      }

      // get folder path from file path
      final java.nio.file.Path firstFilePath = Paths.get(firstFilePathName);
      final String filePathFolder = firstFilePath.getParent().toString();

      // keep last selected path
      _prefStore.setValue(IMPORT_FILE_PATH, filePathFolder);

      /*
       * Convert file names into file paths
       */
      final String[] allSelectedFileNames = fileDialog.getFileNames();
      final ArrayList<String> allFilesPaths = new ArrayList<>();

      for (final String fileName : allSelectedFileNames) {

         final java.nio.file.Path filePath = Paths.get(filePathFolder, fileName);

         allFilesPaths.add(filePath.toString());
      }

      doImportMP_Multiple(allFilesPaths);
   }

   private void onAction_MapProvider_Update() {

      updateModelFromUI();
      enableControls();
   }

   private void onSelect_MapProvider(final SelectionChangedEvent event) {

      final Object firstElement = ((StructuredSelection) event.getSelection()).getFirstElement();
      if (firstElement instanceof MP) {

         final MP mapProvider = (MP) firstElement;

         _selectedMapProvider = mapProvider;

         updateUI_MapProviderInfo(mapProvider);

         // show selected map provider also in the map
         final Map2View map2View = MapProviderManager.getMap2View();
         if (map2View != null) {
            map2View.showMapProvider(_selectedMapProvider);
         }

         enableControls();
      }
   }

   private void onSelect_SortColumn(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = _mpViewer.getSelection();

         // toggle sorting
         _mpComparator.setSortColumn(e.widget);
         _mpViewer.refresh();

         // reselect selection
         _mpViewer.setSelection(selectionBackup, true);
         _mpViewer.getTable().showSelection();
      }
      _viewerContainer.setRedraw(true);
   }

   private void openConfigDialog() {

      if (_isModifiedMapProvider) {
         // update is necessary when the map provider is modified
         if (updateModelFromUI() == false) {
            return;
         }
      }

      if (_selectedMapProvider instanceof MPWms) {

         openConfigDialog_Wms();

      } else if (_selectedMapProvider instanceof MPCustom) {

         openConfigDialog_Custom();

      } else if (_selectedMapProvider instanceof MPProfile) {

         openConfigDialog_MapProfile();
      }

      // set focus back to table
      _mpViewer.getTable().setFocus();
   }

   private void openConfigDialog_Custom() {

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
            _visibleMp = _mpManager.getAllMapProviders(true);

            updateModelFromUI();
         }

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }
   }

   private void openConfigDialog_MapProfile() {

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
            _visibleMp = _mpManager.getAllMapProviders(true);

            updateModelFromUI();
         }

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }
   }

   private void openConfigDialog_Wms() {

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
            _visibleMp = _mpManager.getAllMapProviders(true);

            updateModelFromUI();
         }

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }
   }

   @Override
   public boolean performCancel() {

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

            saveMapProviders(true);
         }
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

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = _mpViewer.getSelection();
         {
            _mpViewer.getTable().dispose();

            createUI_20_Viewer(_viewerContainer);

            // update UI
            _viewerContainer.layout();

            // update the viewer
            reloadViewer();
         }
         updateUI_ReselectItems(selectionBackup);
      }
      _viewerContainer.setRedraw(true);

//      setFocusToMPViewer();

      return _mpViewer;
   }

   @Override
   public void reloadViewer() {

      updateUI_SetViewerInput();
   }

   private void restoreState() {

      /*
       * offline info
       */
      final boolean isReadTileInfo = _prefStore.getBoolean(IMappingPreferences.MAP_FACTORY_IS_READ_TILE_SIZE);
//      fChkReadTileSize.setSelection(isReadTileInfo);

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

   private void restoreState_BeforeUI() {

      // update sorting comparator
      final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, COLUMN_MAP_PROVIDER_NAME);
      final int sortDirection = Util.getStateInt(_state, STATE_SORT_COLUMN_DIRECTION, MapProviderComparator.ASCENDING);

      _mpComparator.__sortColumnId = sortColumnId;
      _mpComparator.__sortDirection = sortDirection;
   }

   private void runnableDropMapProvider(final DropTargetEvent event) {

      final TransferData transferDataType = event.currentDataType;

      if (FileTransfer.getInstance().isSupportedType(transferDataType)) {

         Assert.isTrue(event.data instanceof String[]);

         final String[] paths = (String[]) event.data;
         Assert.isTrue(paths.length > 0);

         for (final String importFilePath : paths) {

            if (isXmlFile(importFilePath)) {
               doImportMP(importFilePath);
            }
         }

      } else if (URLTransfer.getInstance().isSupportedType(transferDataType)) {

         final String dndData = (String) event.data;

         // linux has 2 lines: 1: url, 2. text
         final String[] urlSplitted = dndData.split(UI.NEW_LINE);
         if (urlSplitted.length == 0) {
            return;
         }

         final String url = urlSplitted[0];

         if (isXmlFile(url) == false) {
            return;
         }

         String tempFilePath = null;

         try {

            // create temp file name
            final java.nio.file.Path tempFile = Files.createTempFile("MapProvider_", XML_EXTENSION);//$NON-NLS-1$

            tempFilePath = tempFile.toString();

            // load file from internet
            if (loadFile(url, tempFilePath) == false) {
               return;
            }

            doImportMP(tempFilePath);

         } catch (final IOException e) {
            StatusUtil.showStatus(e);
         } finally {

            // delete temp file
            deleteFile(tempFilePath);
         }

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

         if (_isModifiedMapProviderList && (isSaveMapProvider == false)) {

            isSaveOtherMapProviders = MessageDialogNoClose.openQuestion(
                  Display.getCurrent().getActiveShell(),
                  Messages.pref_map_dlg_saveModifiedMapProvider_title,
                  Messages.pref_map_dlg_saveOtherMapProvider_message);
         }
      }

      if (isSaveNeeded || isSaveMapProvider || isSaveOtherMapProviders) {

         MapProviderManager.getInstance().writeMapProviderXml();

         _isModifiedMapProviderList = false;

         return true;
      }

      return false;
   }

   private void saveState() {

      if (_columnManager == null) {

         // this happened when another pref page had an error and this pref page was selected

         return;
      }

      _state.put(STATE_SORT_COLUMN_ID, _mpComparator.__sortColumnId);
      _state.put(STATE_SORT_COLUMN_DIRECTION, _mpComparator.__sortDirection);

      // selected map provider
      if (_selectedMapProvider != null) {
         _prefStore.setValue(IMappingPreferences.MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER, _selectedMapProvider.getId());
      }

      _columnManager.saveState(_state);
   }

   private void setEmptyMapProviderUI(final MP mapProvider) {

      _isNewMapProvider = true;
      _newMapProvider = mapProvider;

      _isInUIUpdate = true;
      {
         /*
          * set map provider fields empty
          */
         _chkIsIncludesHillshading.setSelection(false);
         _chkIsTransparentLayer.setSelection(false);

         _linkOnlineMap.setText(UI.EMPTY_STRING);

         _txtCategory.setText(UI.EMPTY_STRING);
         _txtDescription.setText(UI.EMPTY_STRING);
         _txtMapProviderId.setText(UI.EMPTY_STRING);
         _txtMapProviderName.setText(UI.EMPTY_STRING);
         _txtOfflineFolder.setText(UI.EMPTY_STRING);
         _txtOnlineMapUrl.setText(UI.EMPTY_STRING);
         _txtLayers.setText(UI.EMPTY_STRING);

         // map provider type
         if (mapProvider instanceof MPCustom) {
            _txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Custom);
         } else if (mapProvider instanceof MPWms) {
            _txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Wms);
         } else if (mapProvider instanceof MPProfile) {
            _txtMapProviderType.setText(Messages.Pref_Map_ProviderType_MapProfile);
         }

         _lblOfflineFolderInfo.setText(UI.EMPTY_STRING);
      }
      _isInUIUpdate = false;

      enableControls();

      _txtMapProviderName.setFocus();
   }

   private void setMapProviderModified() {

      _isModifiedMapProvider = true;

      enableControls();
   }

   private void setWmsDropTarget(final Label label) {

      _wmsDropTarget = new DropTarget(label, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
      _wmsDropTarget.setTransfer(URLTransfer.getInstance(), TextTransfer.getInstance());

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
    *           when set this map provider will be updated, when <code>null</code> only the
    *           offline info of the not updated map providers will be updated
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

      if (_offlineJobMapProviders.isEmpty()) {
         // nothing to do
         return;
      }

      // check cache path
      final IPath tileCacheBasePath = MapProviderManager.getTileCachePath();
      if (tileCacheBasePath == null) {
         return;
      }

      // disable delete offline button
      _isOfflineJobRunning = true;
      enableControls();

      // remove total tile info
      updateUI_OfflineInfo_Total();

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
               if (tileCacheDir.exists() && (_isOfflineJobCanceled == false)) {
                  getFilesInfo(tileCacheDir.listFiles());
               } else {
                  skipReading = true;
               }

               if (skipReading) {

                  // prevent reading files again

                  updateUI_OfflineInfo();
                  continue;
               }

               if (_isOfflineJobCanceled) {
                  // set result invalid
                  _offlineJobFileCounter = MP.OFFLINE_INFO_NOT_READ;
                  _offlineJobFileSize = MP.OFFLINE_INFO_NOT_READ;
               }

               updateUI_OfflineInfo();

               if (_isOfflineJobCanceled) {
                  break;
               }
            }

            _isOfflineJobRunning = false;

            Display.getDefault().syncExec(() -> {

               // enable offline delete button
               enableControls();

               updateUI_OfflineInfo_Total();
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

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

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
      if ((oldOfflineFolder != null) && (oldOfflineFolder.equals(offlineFolder) == false)) {

         // offline folder has changed, delete files in the old offline folder

         deleteOfflineMapFiles(mapProvider);
      }

      // check if id is modified
      if ((oldFactoryId != null) && (oldFactoryId.equals(mpId) == false)) {

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
      mapProvider.setCategory(_txtCategory.getText());
      mapProvider.setDateTimeModified(TimeTools.now());
      mapProvider.setDescription(_txtDescription.getText().trim());
      mapProvider.setId(mpId);
      mapProvider.setIsIncludesHillshading(_chkIsIncludesHillshading.getSelection());
      mapProvider.setIsTransparentLayer(_chkIsTransparentLayer.getSelection());
      mapProvider.setName(mpName);
      mapProvider.setOfflineFolder(offlineFolder);
      mapProvider.setOnlineMapUrl(_txtOnlineMapUrl.getText());

      if (_isNewMapProvider) {

         _isNewMapProvider = false;

         // update model
         _visibleMp.add(mapProvider);
         _mpManager.addMapProvider(mapProvider);

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

   private void updateUI_MapProviderInfo(final MP mapProvider) {

      _isInUIUpdate = true;
      {
         final String onlineMapUrl = mapProvider.getOnlineMapUrl();

         // common fields
         _chkIsIncludesHillshading.setSelection(mapProvider.isIncludesHillshading());
         _chkIsTransparentLayer.setSelection(mapProvider.isTransparentLayer());
         _linkOnlineMap.setText(net.tourbook.common.UI.getLinkFromText(onlineMapUrl));
         _linkOnlineMap.setToolTipText(mapProvider.getName());
         _txtCategory.setText(mapProvider.getCategory());
         _txtDescription.setText(mapProvider.getDescription());
         _txtLayers.setText(MapProviderManager.getTileLayerInfo(mapProvider));
         _txtMapProviderId.setText(mapProvider.getId());
         _txtMapProviderName.setText(mapProvider.getName());
         _txtOnlineMapUrl.setText(onlineMapUrl);

         // offline folder
         final String tileOSFolder = mapProvider.getOfflineFolder();
         if (tileOSFolder == null) {
            _txtOfflineFolder.setText(UI.EMPTY_STRING);
         } else {
            _txtOfflineFolder.setText(tileOSFolder);
         }

         if (mapProvider instanceof MPWms) {

            // wms map provider

            _txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Wms);

         } else if (mapProvider instanceof MPCustom) {

            // custom map provider

            _txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Custom);

         } else if (mapProvider instanceof MPProfile) {

            // map profile

            _txtMapProviderType.setText(Messages.Pref_Map_ProviderType_MapProfile);

         } else if (mapProvider instanceof MPPlugin) {

            // plugin map provider

            _txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Plugin);
         }

         updateUI_OfflineInfo_Detail(mapProvider);
      }
      _isInUIUpdate = false;
   }

   private void updateUI_OfflineInfo() {

      Display.getDefault().syncExec(() -> {

         // check if UI is available
         if (_mpViewer.getTable().isDisposed() || (_offlineJobMp == null)) {
            return;
         }

         // update model
         _offlineJobMp.setOfflineFileCounter(_offlineJobFileCounter);
         _offlineJobMp.setOfflineFileSize(_offlineJobFileSize);

         // update viewer
         _mpViewer.update(_offlineJobMp, null);

         // update info detail when the selected map provider is currently in the job
         if ((_selectedMapProvider != null) && _selectedMapProvider.equals(_offlineJobMp)) {
            updateUI_OfflineInfo_Detail(_selectedMapProvider);
         }
      });
   }

   /**
    * update offline info detail
    */
   private void updateUI_OfflineInfo_Detail(final MP mapProvider) {

      final int offlineTileCounter = mapProvider.getOfflineFileCounter();
      final long offlineTileSize = mapProvider.getOfflineFileSize();

      final StringBuilder sb = new StringBuilder();

      if (offlineTileCounter == MP.OFFLINE_INFO_NOT_READ) {

         sb.append(Messages.Pref_Map_Label_NotRetrieved);

      } else if ((offlineTileCounter > 0) && (offlineTileSize > 0)) {

         sb.append(Integer.toString(offlineTileCounter));
         sb.append(UI.SPACE);
         sb.append(Messages.Pref_Map_Label_Files);
         sb.append(UI.DASH_WITH_SPACE);
         sb.append(_nf.format((float) offlineTileSize / 1024 / 1024));
         sb.append(UI.SPACE);
         sb.append(UI.MBYTES);

      } else {

         sb.append(Messages.Pref_Map_Label_NotAvailable);
      }

      _lblOfflineFolderInfo.setText(sb.toString());
   }

   private void updateUI_OfflineInfo_Total() {

      if ((_txtOfflineInfoTotal == null) || _txtOfflineInfoTotal.isDisposed()) {
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
            sbTotal.append(Messages.Pref_Map_Label_Files);
            sbTotal.append(UI.DASH_WITH_SPACE);
            sbTotal.append(_nf.format((float) tileSize / 1024 / 1024));
            sbTotal.append(UI.SPACE);
            sbTotal.append(UI.MBYTES);
         }
      }

      _txtOfflineInfoTotal.setText(sbTotal.toString());
   }

   /**
    * Select and reveal the previous items.
    *
    * @param selection
    */
   private void updateUI_ReselectItems(final ISelection selection) {

      _isInUIUpdate = true;
      {
         _mpViewer.setSelection(selection, true);

         final Table table = _mpViewer.getTable();
         table.showSelection();
      }
      _isInUIUpdate = false;
   }

   /**
    * Set the sort column direction indicator for a column
    *
    * @param sortColumnId
    * @param isAscendingSort
    */
   private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

      final int direction =
            sortDirection == MapProviderComparator.ASCENDING ? SWT.UP
                  : sortDirection == MapProviderComparator.DESCENDING ? SWT.DOWN
                        : SWT.NONE;

      final Table table = _mpViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(direction);
   }

   private void updateUI_SetViewerInput() {

      _isInUIUpdate = true;
      {
         _mpViewer.setInput(new Object[0]);
      }
      _isInUIUpdate = false;
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
      if ((mapProviderName == null) || (mapProviderName.length() == 0)) {
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
