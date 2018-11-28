/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.part.PageBook;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.NIO;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.map25.Map25Provider;
import net.tourbook.map25.Map25ProviderManager;
import net.tourbook.map25.TileEncoding;

public class PrefPageMap25Provider extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String              ID                               = "net.tourbook.preferences.PrefPage_Map25_Provider"; //$NON-NLS-1$

   private static final String             STATE_LAST_SELECTED_MAP_PROVIDER = "STATE_LAST_SELECTED_MAP_PROVIDER";                 //$NON-NLS-1$

   /**
    * First encoding is the default.
    */
   private static final TileEncodingData[] _allTileEncoding                 = new TileEncodingData[] {

         new TileEncodingData(TileEncoding.VTM, Messages.Pref_Map25_Encoding_OpenScienceMap, false),
         new TileEncodingData(TileEncoding.MVT, Messages.Pref_Map25_Encoding_Mapzen, false),
         new TileEncodingData(TileEncoding.MF, Messages.Pref_Map25_Encoding_Mapsforge_Offline, true)
   };

   private final IDialogSettings           _state                           = TourbookPlugin.getState(ID);

   private ArrayList<Map25Provider>        _allMapProvider;
   //
   private ModifyListener                  _defaultModifyListener;
   private SelectionListener               _defaultSelectionListener;
   //
   private Map25Provider                   _newProvider;
   private Map25Provider                   _selectedMapProvider;
   //
   private boolean                         _isModified;
   private boolean                         _isMapProviderModified;
   private boolean                         _isInUpdateUI;
   //
   /**
    * Contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the differenct section to the same width
    */
   private final ArrayList<Control>        _firstColumnControls             = new ArrayList<>();
   //
   /*
    * UI Controls
    */
   private TableViewer _mapProviderViewer;
   //
   //
   private Composite _parent;
   private Composite _uiInnerContainer;

   private PageBook  _pageBook_OnOffline;
   private Composite _pageMaps_Online;
   private Composite _pageMaps_Offline;
   //
   private Button    _btnAddProvider;
   private Button    _btnCancel;
   private Button    _btnDeleteProvider;
   private Button    _btnMapFile;
   private Button    _btnThemeFile;
   private Button    _btnUpdateProvider;
   //
   private Button    _chkIsMapProviderEnabled;
   //
   private Combo     _comboTileEncoding;
   //
   private Label     _lblAPIKey;
   private Label     _lblDescription;
   private Label     _lblMapFilepath;
   private Label     _lblProviderName;
   private Label     _lblThemeFilepath;
   private Label     _lblThemeStyle;
   private Label     _lblTileEncoding;
   private Label     _lblTilePath;
   private Label     _lblTileUrl;
   private Label     _lblUrl;
   //
   private Text      _txtAPIKey;
   private Text      _txtDescription;
   private Text      _txtMapFilepath;
   private Text      _txtProviderName;
   private Text      _txtThemeFilepath;
   private Text      _txtThemeStyle;
   private Text      _txtTilePath;
   private Text      _txtTileUrl;
   private Text      _txtUrl;

//   private Composite parent;

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

      private TileEncoding __encoding;
      private String       __text;
      private boolean      __isOffline;

      public TileEncodingData(final TileEncoding encoding, final String text, final boolean isOffline) {

         __encoding = encoding;
         __text = text;
         __isOffline = isOffline;
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

         _uiInnerContainer = new Composite(container, SWT.NONE);
         GridDataFactory
               .fillDefaults()//
               .grab(true, true)
               .applyTo(_uiInnerContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_uiInnerContainer);
         {
            createUI_10_Provider_Viewer(_uiInnerContainer);
            createUI_20_Provider_Actions(_uiInnerContainer);

            createUI_30_Details(_uiInnerContainer);
            createUI_90_Details_Actions(_uiInnerContainer);
         }

         // placeholder
         new Label(container, SWT.NONE);
      }

      // with e4 the layouts are not yet set -> NPE's -> run async which worked
      parent.getShell().getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {

            // compute width for all controls and equalize column width for the different sections
            container.layout(true, true);
            UI.setEqualizeColumWidths(_firstColumnControls);

            // this must be layouted otherwise the initial layout is not as is should be
            container.layout(true, true);
         }
      });

      return container;
   }

   private void createUI_10_Provider_Viewer(final Composite parent) {

      final TableColumnLayout tableLayout = new TableColumnLayout();

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      layoutContainer.setLayout(tableLayout);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(convertWidthInCharsToPixels(100), convertHeightInCharsToPixels(10))
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
            onSelect_MapProvider();
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

            _chkIsMapProviderEnabled = new Button(container, SWT.CHECK);
            _chkIsMapProviderEnabled.setText(Messages.Pref_Map25_Provider_Checkbox_IsEnabled);
            _chkIsMapProviderEnabled.setToolTipText(Messages.Pref_Map25_Provider_Checkbox_IsEnabled_Tooltip);
            _chkIsMapProviderEnabled.addSelectionListener(_defaultSelectionListener);
            GridDataFactory
                  .fillDefaults()//
                  .grab(true, false)
                  //						.span(2, 1)
                  .applyTo(_chkIsMapProviderEnabled);
         }
         {
            /*
             * Field: Provider name
             */
            _lblProviderName = new Label(container, SWT.NONE);
            _lblProviderName.setText(Messages.Pref_Map25_Provider_Label_ProviderName);
            _firstColumnControls.add(_lblProviderName);

            _txtProviderName = new Text(container, SWT.BORDER);
            GridDataFactory
                  .fillDefaults()//
                  .grab(true, false)
                  .applyTo(_txtProviderName);
            _txtProviderName.addModifyListener(_defaultModifyListener);
         }
         {
            /*
             * Field: Tile Encoding
             */
            _lblTileEncoding = new Label(container, SWT.NONE);
            _lblTileEncoding.setText(Messages.Pref_Map25_Provider_Label_TileEncoding);
            _firstColumnControls.add(_lblTileEncoding);

            _comboTileEncoding = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
//          GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(_comboTileEncoding);
            _comboTileEncoding.setVisibleItemCount(20);
            _comboTileEncoding.addSelectionListener(new SelectionAdapter() {

               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_TileEncoding();
               }
            });

            // fill combobox
            for (final TileEncodingData encodingData : _allTileEncoding) {
               _comboTileEncoding.add(encodingData.__text);
            }
         }

         _pageBook_OnOffline = new PageBook(container, SWT.NONE);
         GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(_pageBook_OnOffline);
         {
            _pageMaps_Online = createUI_50_Maps_Online(_pageBook_OnOffline);
            _pageMaps_Offline = createUI_52_Maps_Offline(_pageBook_OnOffline);
         }
         // set any page
         _pageBook_OnOffline.showPage(_pageMaps_Offline);

         {
            /*
             * Field: Description
             */
            _lblDescription = new Label(container, SWT.NONE);
            _lblDescription.setText(Messages.Pref_Map25_Provider_Label_Description);
            GridDataFactory
                  .fillDefaults()//
                  .align(SWT.FILL, SWT.BEGINNING)
                  .applyTo(_lblDescription);
            _firstColumnControls.add(_lblDescription);

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

   private Composite createUI_50_Maps_Online(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Field: Url
             */
            _lblUrl = new Label(container, SWT.NONE);
            _lblUrl.setText(Messages.Pref_Map25_Provider_Label_Url);
            _firstColumnControls.add(_lblUrl);

            _txtUrl = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtUrl);
            _txtUrl.addModifyListener(_defaultModifyListener);
         }

         {
            /*
             * Field: Tile path
             */
            _lblTilePath = new Label(container, SWT.NONE);
            _lblTilePath.setText(Messages.Pref_Map25_Provider_Label_TilePath);
            _firstColumnControls.add(_lblTilePath);

            _txtTilePath = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtTilePath);
            _txtTilePath.addModifyListener(_defaultModifyListener);
         }
         {
            /*
             * Field: Tile Url
             */
            _lblTileUrl = new Label(container, SWT.NONE);
            _lblTileUrl.setText(Messages.Pref_Map25_Provider_Label_TileUrl);
            _firstColumnControls.add(_lblTileUrl);

            _txtTileUrl = new Text(container, SWT.READ_ONLY);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtTileUrl);
         }
         {
            /*
             * Field: API key
             */
            _lblAPIKey = new Label(container, SWT.NONE);
            _lblAPIKey.setText(Messages.Pref_Map25_Provider_Label_APIKey);
            _firstColumnControls.add(_lblAPIKey);

            _txtAPIKey = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtAPIKey);
            _txtAPIKey.addModifyListener(_defaultModifyListener);
         }
      }

      return container;
   }

   private Composite createUI_52_Maps_Offline(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Field: Map file
             */
            _lblMapFilepath = new Label(container, SWT.NONE);
            _lblMapFilepath.setText(Messages.Pref_Map25_Provider_Label_MapFilepath);
            _firstColumnControls.add(_lblMapFilepath);

            final Composite containerMapFile = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerMapFile);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerMapFile);
            {
               {
                  _txtMapFilepath = new Text(containerMapFile, SWT.BORDER);
                  _txtMapFilepath.addModifyListener(_defaultModifyListener);
                  GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtMapFilepath);
               }

               {
                  /*
                   * Button: browse...
                   */
                  _btnMapFile = new Button(containerMapFile, SWT.PUSH);
                  _btnMapFile.setText(Messages.app_btn_browse);
                  _btnMapFile.addSelectionListener(new SelectionAdapter() {
                     @Override
                     public void widgetSelected(final SelectionEvent e) {
                        onSelect_Filename_Map();
                     }
                  });
                  GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_btnMapFile);
                  setButtonLayoutData(_btnMapFile);
               }
            }
         }
         {
            /*
             * Field: Theme filepath
             */
            _lblThemeFilepath = new Label(container, SWT.NONE);
            _lblThemeFilepath.setText(Messages.Pref_Map25_Provider_Label_ThemeFilepath);
            _firstColumnControls.add(_lblThemeFilepath);

            final Composite containerThemeFile = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerThemeFile);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerThemeFile);
            {
               {
                  _txtThemeFilepath = new Text(containerThemeFile, SWT.BORDER);
                  _txtThemeFilepath.addModifyListener(_defaultModifyListener);
                  GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtThemeFilepath);
               }

               {
                  /*
                   * Button: browse...
                   */
                  _btnThemeFile = new Button(containerThemeFile, SWT.PUSH);
                  _btnThemeFile.setText(Messages.app_btn_browse);
                  _btnThemeFile.addSelectionListener(new SelectionAdapter() {
                     @Override
                     public void widgetSelected(final SelectionEvent e) {
                        onSelect_Filename_Theme();
                     }
                  });
                  GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_btnThemeFile);
                  setButtonLayoutData(_btnThemeFile);
               }
            }
         }
         {
            /*
             * Field: Theme style
             */
            _lblThemeStyle = new Label(container, SWT.NONE);
            _lblThemeStyle.setText(Messages.Pref_Map25_Provider_Label_ThemeStyle);
            _firstColumnControls.add(_lblThemeStyle);

            _txtThemeStyle = new Text(container, SWT.BORDER);
            _txtThemeStyle.addModifyListener(_defaultModifyListener);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtThemeStyle);
         }
      }

      return container;
   }

   private void createUI_90_Details_Actions(final Composite parent) {

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
          * Column: Enabled
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
         tableLayout.setColumnData(tc, new ColumnWeightData(2, minWidth));
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
         tableLayout.setColumnData(tc, new ColumnWeightData(2, minWidth));
      }
      {
         /*
          * Column: Url / Map filename
          */
         tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
         tc = tvc.getColumn();
         tc.setText(Messages.Pref_Map25_Provider_Column_Url);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final Map25Provider map25Provider = (Map25Provider) cell.getElement();

               cell.setText(map25Provider.isOfflineMap
                     ? map25Provider.mapFilepath
                     : map25Provider.url);
            }
         });
         tableLayout.setColumnData(tc, new ColumnWeightData(7, minWidth));
      }
      {
         /*
          * Column: Tile path / Theme filepath
          */
         tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
         tc = tvc.getColumn();
         tc.setText(Messages.Pref_Map25_Provider_Column_TilePath);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final Map25Provider map25Provider = (Map25Provider) cell.getElement();

               cell.setText(map25Provider.isOfflineMap
                     ? map25Provider.themeFilepath
                     : map25Provider.tilePath);
            }
         });
         tableLayout.setColumnData(tc, new ColumnWeightData(7, minWidth));
      }
      {
         /*
          * Column: API key / Theme style
          */
         tvc = new TableViewerColumn(_mapProviderViewer, SWT.LEAD);
         tc = tvc.getColumn();
         tc.setText(Messages.Pref_Map25_Provider_Column_APIKey);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final Map25Provider map25Provider = (Map25Provider) cell.getElement();

               cell.setText(map25Provider.isOfflineMap
                     ? map25Provider.themeStyle
                     : map25Provider.apiKey);
            }
         });
         tableLayout.setColumnData(tc, new ColumnWeightData(3, minWidth));
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

   @Override
   public void dispose() {

      _firstColumnControls.clear();

      super.dispose();
   }

   private void enableControls() {

      final boolean isSelected = _selectedMapProvider != null;
      final boolean isEnabled = _selectedMapProvider.isEnabled || _chkIsMapProviderEnabled.getSelection();
      final boolean isNew = _newProvider != null;
      final boolean canEdit = isEnabled || isNew;

      final boolean isDefaultProvider = _selectedMapProvider.isDefault;
      final boolean isCustomProvider = isDefaultProvider == false;

      final boolean isValid = isDataValid();

      _mapProviderViewer.getTable().setEnabled(!_isMapProviderModified && isValid);

      _btnAddProvider.setEnabled(!_isMapProviderModified && isValid);
      _btnDeleteProvider.setEnabled(isCustomProvider && isSelected && !isNew && !_isMapProviderModified);

      _btnCancel.setEnabled(_isMapProviderModified);
      _btnMapFile.setEnabled(canEdit);
      _btnThemeFile.setEnabled(canEdit);
      _btnUpdateProvider.setEnabled(_isMapProviderModified && isValid);

      _chkIsMapProviderEnabled.setEnabled(isCustomProvider && (isSelected || isNew));

      _comboTileEncoding.setEnabled(canEdit);

      _lblAPIKey.setEnabled(canEdit);
      _lblDescription.setEnabled(canEdit);
      _lblMapFilepath.setEnabled(canEdit);
      _lblProviderName.setEnabled(canEdit);
      _lblThemeFilepath.setEnabled(canEdit);
      _lblThemeStyle.setEnabled(canEdit);
      _lblTileEncoding.setEnabled(canEdit);
      _lblTilePath.setEnabled(canEdit);
      _lblTileUrl.setEnabled(canEdit);
      _lblUrl.setEnabled(canEdit);

      _txtAPIKey.setEnabled(canEdit);
      _txtDescription.setEnabled(canEdit);
      _txtMapFilepath.setEnabled(canEdit);
      _txtThemeFilepath.setEnabled(canEdit);
      _txtThemeStyle.setEnabled(canEdit);
      _txtProviderName.setEnabled(canEdit);
      _txtTilePath.setEnabled(canEdit);
      _txtTileUrl.setEnabled(canEdit);
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

   private TileEncodingData getSelectedEncoding() {

      final int selectedIndex = _comboTileEncoding.getSelectionIndex();

      if (selectedIndex < 0) {

         // return default
         return _allTileEncoding[0];
      }

      return _allTileEncoding[selectedIndex];
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

      _parent = parent;
   }

   /**
    * @return Returns <code>true</code> when person is valid, otherwise <code>false</code>.
    */
   private boolean isDataValid() {

      final boolean isNewProvider = _newProvider != null;

      if (isNewProvider || _isMapProviderModified) {

         if (getSelectedEncoding().__isOffline) {

            // validate offline map

            if (_txtMapFilepath.getText().trim().equals(UI.EMPTY_STRING)) {
               setErrorMessage(Messages.Pref_Map25_Provider_Error_MapFilename_IsRequired);
               return false;

            } else if (_txtThemeFilepath.getText().trim().equals(UI.EMPTY_STRING)) {
               setErrorMessage(Messages.Pref_Map25_Provider_Error_ThemeFilename_IsRequired);
               return false;
            }

         } else {

            // validate online map

            if (_txtProviderName.getText().trim().equals(UI.EMPTY_STRING)) {
               setErrorMessage(Messages.Pref_Map25_Provider_Error_ProviderNameIsRequired);
               return false;

            } else if (_txtUrl.getText().trim().equals(UI.EMPTY_STRING)) {
               setErrorMessage(Messages.Pref_Map25_Provider_Error_UrlIsRequired);
               return false;

            } else if (_txtTilePath.getText().trim().equals(UI.EMPTY_STRING)) {
               setErrorMessage(Messages.Pref_Map25_Provider_Error_TilePathIsRequired);
               return false;
            }
         }

         /*
          * Check that at least 1 map provider is enabled
          */
         final boolean isCurrentEnabled = _chkIsMapProviderEnabled.getSelection();
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
         }
      }

      setErrorMessage(null);

      return true;
   }

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

      _newProvider.isEnabled = true;

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

   private void onProvider_Update() {

      if (isDataValid() == false) {
         return;
      }

      updateModelAndUI();
      enableControls();

      _mapProviderViewer.getTable().setFocus();
   }

   private void onSelect_Filename_Map() {

      String mapFile_Foldername = null;

      final String userPathname = _txtMapFilepath.getText();
      final Path mapFilepath = NIO.getPath(userPathname);

      if (mapFilepath != null) {

         final Path mapFile_Folder = mapFilepath.getParent();
         if (mapFile_Folder != null) {

            mapFile_Foldername = mapFile_Folder.toString();
         }
      }

      final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
      dialog.setText(Messages.Pref_Map25_Dialog_MapFilename_Title);

      dialog.setFilterPath(mapFile_Foldername);
      dialog.setFileName("*." + Map25ProviderManager.MAPSFORGE_MAP_FILE_EXTENTION);//$NON-NLS-1$
      dialog.setFilterExtensions(new String[] { Map25ProviderManager.MAPSFORGE_MAP_FILE_EXTENTION });

      final String selectedFilepath = dialog.open();

      if (selectedFilepath != null) {

         setErrorMessage(null);

         // update UI
         _txtMapFilepath.setText(selectedFilepath);
      }
   }

   private void onSelect_Filename_Theme() {

      String mapStyle_Foldername = null;

      final String userPathname = _txtThemeFilepath.getText();
      final Path mapStyle_Filepath = NIO.getPath(userPathname);

      if (mapStyle_Filepath != null) {

         final Path mapStyle_Folder = mapStyle_Filepath.getParent();
         if (mapStyle_Folder != null) {

            mapStyle_Foldername = mapStyle_Folder.toString();
         }
      }

      final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
      dialog.setText(Messages.Pref_Map25_Dialog_MapStyleFilename_Title);

      dialog.setFilterPath(mapStyle_Foldername);
      dialog.setFileName("*." + Map25ProviderManager.MAPSFORGE_STYLE_FILE_EXTENTION);//$NON-NLS-1$
      dialog.setFilterExtensions(new String[] { Map25ProviderManager.MAPSFORGE_STYLE_FILE_EXTENTION });

      final String selectedFilepath = dialog.open();

      if (selectedFilepath == null) {
         // dialog is canceled
      }

      setErrorMessage(null);

      // update UI
      _txtThemeFilepath.setText(selectedFilepath);
   }

   private void onSelect_MapProvider() {

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

   private void onSelect_TileEncoding() {

      updateUI_TileEncoding();

      onProvider_Modify();
   }

   private void parseThemeStyles() {

      //java.util.List<Item> mf_styles = mf_style_parser.readConfig("C:\\Users\\top\\BTSync\\oruxmaps\\mapstyles\\ELV4\\Elevate.xml");

      final MapsforgeStyleParser mfStyleParser = new MapsforgeStyleParser();
      final java.util.List<Style> mf_styles = mfStyleParser.readXML(_txtTilePath.getText().trim());

      //System.out.println("####### PrefPageMap25MapsforgeProvider Stylecount: " + mf_styles.size());

      String styles = "Aviable Styles: ";

      for (final Style style : mf_styles) {

         styles += style.getXmlLayer();
         styles += " (";
         styles += style.getName(Locale.getDefault().toString());
         styles += "),";
         //System.out.println(item.getXmlLayer());
      }
      styles += "all";
      //System.out.println("####### PrefPageMap25MapsforgeProvider isDataValid: " + styles);
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

   /**
    * Update map provider
    */
   private void updateModelData(final Map25Provider mapProvider) {

      final TileEncodingData selectedEncoding = getSelectedEncoding();

      mapProvider.name = _txtProviderName.getText();
      mapProvider.description = _txtDescription.getText();
      mapProvider.isEnabled = _chkIsMapProviderEnabled.getSelection();

      final boolean isOffline = selectedEncoding.__isOffline;
      mapProvider.isOfflineMap = isOffline;

      if (isOffline) {

         mapProvider.mapFilepath = _txtMapFilepath.getText();
         mapProvider.themeFilepath = _txtThemeFilepath.getText();
         mapProvider.themeStyle = _txtThemeStyle.getText();

      } else {

         mapProvider.url = _txtUrl.getText();
         mapProvider.tilePath = _txtTilePath.getText();
         mapProvider.apiKey = _txtAPIKey.getText();
      }

      mapProvider.tileEncoding = selectedEncoding.__encoding;
   }

   private void updateUI_Data() {

      final String tileUrl = _txtUrl.getText() + _txtTilePath.getText();

      _txtTileUrl.setText(tileUrl);
   }

   private void updateUI_FromProvider(final Map25Provider mapProvider) {

      _isInUpdateUI = true;
      {
         if (mapProvider == null) {

            _chkIsMapProviderEnabled.setSelection(false);

            _txtDescription.setText(UI.EMPTY_STRING);
            _txtProviderName.setText(UI.EMPTY_STRING);

            _txtAPIKey.setText(UI.EMPTY_STRING);
            _txtUrl.setText(UI.EMPTY_STRING);
            _txtTilePath.setText(UI.EMPTY_STRING);

            _txtMapFilepath.setText(UI.EMPTY_STRING);
            _txtThemeFilepath.setText(UI.EMPTY_STRING);
            _txtThemeStyle.setText(UI.EMPTY_STRING);

         } else {

            _chkIsMapProviderEnabled.setSelection(mapProvider.isEnabled);

            _txtDescription.setText(mapProvider.description);
            _txtProviderName.setText(mapProvider.name);

            if (mapProvider.isOfflineMap) {

               _txtAPIKey.setText(UI.EMPTY_STRING);
               _txtUrl.setText(UI.EMPTY_STRING);
               _txtTilePath.setText(UI.EMPTY_STRING);

               _txtMapFilepath.setText(mapProvider.mapFilepath);
               _txtThemeFilepath.setText(mapProvider.themeFilepath);
               _txtThemeStyle.setText(mapProvider.themeStyle);

            } else {

               _txtAPIKey.setText(mapProvider.apiKey);
               _txtUrl.setText(mapProvider.url);
               _txtTilePath.setText(mapProvider.tilePath);

               _txtMapFilepath.setText(UI.EMPTY_STRING);
               _txtThemeFilepath.setText(UI.EMPTY_STRING);
               _txtThemeStyle.setText(UI.EMPTY_STRING);
            }

            _comboTileEncoding.select(getEncodingIndex(mapProvider.tileEncoding));
         }

         updateUI_TileEncoding();
         updateUI_Data();
      }
      _isInUpdateUI = false;
   }

   /**
    * Show tile encoding fields
    */
   private void updateUI_TileEncoding() {

      final TileEncodingData selectedEncodingData = getSelectedEncoding();

      if (selectedEncodingData.__isOffline) {
         _pageBook_OnOffline.showPage(_pageMaps_Offline);
      } else {
         _pageBook_OnOffline.showPage(_pageMaps_Online);
      }

      // pages can have different heights
      _uiInnerContainer.layout(true, true);
   }

}
