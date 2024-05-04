/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnDefinitionFor1stVisibleAlignmentColumn;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer2;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourLocation;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.location.CommonLocationManager;
import net.tourbook.tour.location.TourLocationToolTip;
import net.tourbook.ui.TableColumnFactory;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Slideout for all 2D map locations and marker
 */
public class SlideoutMap2_LocationAndMarker extends AdvancedSlideout implements ITourViewer2, IColorSelectorListener {

   private static final String     COLUMN_CREATED_DATE_TIME        = "createdDateTime";                         //$NON-NLS-1$
   private static final String     COLUMN_LOCATION_NAME            = "LocationName";                            //$NON-NLS-1$
   private static final String     COLUMN_SEQUENCE                 = "sequence";                                //$NON-NLS-1$
   private static final String     COLUMN_ZOOM_LEVEL               = "zoomLevel";                               //$NON-NLS-1$
   //
   private static final String     STATE_SELECTED_TAB              = "STATE_SELECTED_TAB";                      //$NON-NLS-1$
   private static final String     STATE_SORT_COLUMN_DIRECTION     = "STATE_SORT_COLUMN_DIRECTION";             //$NON-NLS-1$
   private static final String     STATE_SORT_COLUMN_ID            = "STATE_SORT_COLUMN_ID";                    //$NON-NLS-1$
   //
   private final IPreferenceStore  _prefStore                      = TourbookPlugin.getPrefStore();
   private IDialogSettings         _state_Map2;
   private IDialogSettings         _state_Slideout;
   //
   private Map2View                _map2View;
   private ToolItem                _toolItem;
   //
   private TableViewer             _mapCommonLocationViewer;
   private MapLocationComparator   _mapLocationComparator          = new MapLocationComparator();
   private ColumnManager           _columnManager;
   private SelectionAdapter        _columnSortListener;
   //
   private SelectionListener       _defaultSelectionListener;
   private SelectionListener       _markerSelectionListener;
   private IPropertyChangeListener _markerPropertyChangeListener;
   private MouseWheelListener      _markerMouseWheelListener;
   private FocusListener           _keepOpenListener;
   private IPropertyChangeListener _prefChangeListener;
   //
   private MenuManager             _viewerMenuManager;
   private IContextMenuProvider    _tableViewerContextMenuProvider = new TableContextMenuProvider();
   private ActionDeleteLocation    _actionDeleteLocation;
   //
   private List<TourLocation>      _allMapLocations                = CommonLocationManager.getCommonLocations();
   //
   private TourLocationToolTip     _locationTooltip;
   //
   private PixelConverter          _pc;
   //
   private final NumberFormat      _nf3                            = NumberFormat.getNumberInstance();
   {
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);
   }
   //
   private GridDataFactory _spinnerGridData;
   //
   /*
    * UI controls
    */
   private Composite             _viewerContainer;
   //
   private Menu                  _tableContextMenu;
   //
   private CTabFolder            _tabFolder;
   private CTabItem              _tabOptions;
   private CTabItem              _tabCommonLocations;
   private CTabItem              _tabTourMarkers;
   //
   private Button                _btnDeleteCommonLocation;
   private Button                _btnSwapClusterSymbolColor;
   private Button                _btnSwapMarkerColor;
   //
   private Button                _chkIsClusterSymbolAntialiased;
   private Button                _chkIsClusterTextAntialiased;
   private Button                _chkIsFillClusterSymbol;
   private Button                _chkIsMarkerClustered;
   private Button                _chkIsShowCommonLocations;
   private Button                _chkIsShowMapLocations_BoundingBox;
   private Button                _chkIsShowTourLocations;
   private Button                _chkIsShowTourMarker;
   //
   private Label                 _lblClusterGrid_Size;
   private Label                 _lblClusterSymbol;
   private Label                 _lblClusterSymbol_Size;
   private Label                 _lblMarkerColor;
   private Label                 _lblMarkerOpacity;
   private Label                 _lblMarkerSize;
   //
   private Spinner               _spinnerClusterGrid_Size;
   private Spinner               _spinnerClusterOutline_Width;
   private Spinner               _spinnerClusterSymbol_Size;
   private Spinner               _spinnerMarkerOutline_Opacity;
   private Spinner               _spinnerMarkerFill_Opacity;
   private Spinner               _spinnerMarkerOutline_Size;
   private Spinner               _spinnerMarkerSymbol_Size;
   //
   private ColorSelectorExtended _colorMarkerSymbol_Outline;
   private ColorSelectorExtended _colorMarkerSymbol_Fill;
   private ColorSelectorExtended _colorClusterSymbol_Outline;
   private ColorSelectorExtended _colorClusterSymbol_Fill;

   private class ActionDeleteLocation extends Action {

      public ActionDeleteLocation() {

         setText(Messages.Tour_Location_Action_DeleteCommonLocation);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete_Disabled));
      }

      @Override
      public void run() {

         onLocation_Delete();
      }
   }

   private class MapLocationComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = COLUMN_CREATED_DATE_TIME;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final TourLocation location1 = (TourLocation) e1;
         final TourLocation location2 = (TourLocation) e2;

         boolean _isSortByTime = true;
         double rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnId) {

         case COLUMN_LOCATION_NAME:
            rc = location1.display_name.compareTo(location2.display_name);
            break;

         case COLUMN_CREATED_DATE_TIME:

            // sorting by date is already set
            break;

         case COLUMN_ZOOM_LEVEL:
            rc = location1.zoomlevel - location2.zoomlevel;
            break;

         case TableColumnFactory.LOCATION_GEO_BOUNDING_BOX_WIDTH_ID:
            rc = location1.boundingBoxWidth - location2.boundingBoxWidth;
            break;

         case TableColumnFactory.LOCATION_GEO_BOUNDING_BOX_HEIGHT_ID:
            rc = location1.boundingBoxHeight - location2.boundingBoxHeight;
            break;

         case TableColumnFactory.LOCATION_GEO_LATITUDE_ID:

            rc = location1.latitudeE6_Normalized - location2.latitudeE6_Normalized;

            if (rc == 0) {
               rc = location1.longitudeE6_Normalized - location2.longitudeE6_Normalized;
            }

            break;

         case TableColumnFactory.LOCATION_GEO_LONGITUDE_ID:

            rc = location1.longitudeE6_Normalized - location2.longitudeE6_Normalized;

            if (rc == 0) {
               rc = location1.latitudeE6_Normalized - location2.latitudeE6_Normalized;
            }

            break;

         default:
            _isSortByTime = true;
         }

         if (rc == 0 && _isSortByTime) {
            rc = location1.getCreatedMS() - location2.getCreatedMS();
         }

         // if descending order, flip the direction
         if (__sortDirection == DESCENDING) {
            rc = -rc;
         }

         /*
          * MUST return 1 or -1 otherwise long values are not sorted correctly
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

         if (columnId.equals(__sortColumnId)) {

            // Same column as last sort; toggle the direction

            __sortDirection = 1 - __sortDirection;

         } else {

            // New column; do an ascent sorting

            __sortColumnId = columnId;
            __sortDirection = ASCENDING;
         }

         updateUI_SetSortDirection(__sortColumnId, __sortDirection);
      }
   }

   private class MapLocationContentProvider implements IStructuredContentProvider {

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allMapLocations.toArray();
      }
   }

   public class TableContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_tableContextMenu != null) {
            _tableContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _tableContextMenu;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _tableContextMenu = createUI_525_CreateViewerContextMenu();

         return _tableContextMenu;
      }
   }

   /**
    * This class is used to show a tooltip only when this cell is hovered
    */
   public abstract class TooltipLabelProvider extends CellLabelProvider {}

   /**
    * @param map2State
    * @param map2View
    * @param map2View
    * @param ownerControl
    * @param toolBar
    */
   public SlideoutMap2_LocationAndMarker(final ToolItem toolItem,
                                         final IDialogSettings map2State,
                                         final IDialogSettings slideoutState,
                                         final Map2View map2View) {

      super(toolItem.getParent(), slideoutState, new int[] { 325, 400, 325, 400 });

      _toolItem = toolItem;

      _state_Map2 = map2State;
      _state_Slideout = slideoutState;
      _map2View = map2View;

      setTitleText(Messages.Slideout_MapLocation_Label_Title);

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _mapCommonLocationViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
            _mapCommonLocationViewer.refresh();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   @Override
   public void close() {

      CommonLocationManager.setMapLocationSlideout(null);

      super.close();
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      setIsAnotherDialogOpened(isDialogOpened);
   }

   private void createActions() {

      _actionDeleteLocation = new ActionDeleteLocation();
   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager();
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(menuManager -> fillContextMenu(menuManager));
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);
      createMenuManager();

      restoreState_BeforeUI();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state_Slideout);
      defineAllColumns();

      createUI(parent);
      createActions();

      addPrefListener();

      // load viewer
      updateUI_Viewer();

      restoreState();
      restoreTabFolder();

      enableControls();

      CommonLocationManager.setMapLocationSlideout(this);
   }

   /**
    * Create a list with all available map providers, sorted by preference settings
    */

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
//      shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
         GridLayoutFactory.fillDefaults()
//               .extendedMargins(5, 5, 0, 5)
               .applyTo(container);
         {
            _tabFolder = new CTabFolder(container, SWT.TOP);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_tabFolder);
            GridLayoutFactory.fillDefaults().applyTo(_tabFolder);

            {
               _tabOptions = new CTabItem(_tabFolder, SWT.NONE);
               _tabOptions.setText(Messages.Slideout_MapLocation_Tab_Options);
               _tabOptions.setControl(createUI_100_Tab_Options(_tabFolder));

               _tabTourMarkers = new CTabItem(_tabFolder, SWT.NONE);
               _tabTourMarkers.setText(Messages.Slideout_MapLocation_Tab_TourMarkers);
               _tabTourMarkers.setControl(createUI_200_Tab_TourMarker(_tabFolder));

               _tabCommonLocations = new CTabItem(_tabFolder, SWT.NONE);
               _tabCommonLocations.setText(Messages.Slideout_MapLocation_Tab_CommonLocations);
               _tabCommonLocations.setControl(createUI_500_Tab_CommonLocations(_tabFolder));
            }
         }
      }

      return shellContainer;
   }

   private Control createUI_100_Tab_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 5, 0).numColumns(1).applyTo(container);
      {
         createUI_110_Options(container);

      }

      return container;
   }

   private void createUI_110_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Show tour locations
             */
            _chkIsShowTourLocations = new Button(container, SWT.CHECK);
            _chkIsShowTourLocations.setText(Messages.Slideout_MapLocation_Checkbox_ShowTourLocations);
            _chkIsShowTourLocations.setToolTipText(Messages.Slideout_MapLocation_Checkbox_ShowTourLocations_Tooltip);
            _chkIsShowTourLocations.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowTourLocations);
         }
         {
            /*
             * Show location bounding box
             */
            _chkIsShowMapLocations_BoundingBox = new Button(container, SWT.CHECK);
            _chkIsShowMapLocations_BoundingBox.setText(Messages.Slideout_MapLocation_Checkbox_ShowLocationBoundingBox);
            _chkIsShowMapLocations_BoundingBox.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowMapLocations_BoundingBox);
         }

      }

      // reorder tabbing
//      container.setTabList(new Control[] {
//
//            _chkIsShowCommonLocations,
//            _chkIsShowTourLocations,
//
//            _chkIsShowMapLocations_BoundingBox,
//      });
   }

   private Control createUI_200_Tab_TourMarker(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 5, 0).numColumns(1).applyTo(container);
      {
         {
            /*
             * Show tour marker
             */
            _chkIsShowTourMarker = new Button(container, SWT.CHECK);
            _chkIsShowTourMarker.setText(Messages.Slideout_Map25MarkerOptions_Checkbox_IsShowTourMarker);
            _chkIsShowTourMarker.addSelectionListener(_markerSelectionListener);
         }
         {
            /*
             * Antialias text
             */
            _chkIsClusterTextAntialiased = new Button(container, SWT.CHECK);
            _chkIsClusterTextAntialiased.setText("Antialias text painting");
            _chkIsClusterTextAntialiased.addSelectionListener(_markerSelectionListener);
         }
         {
            /*
             * Antialias symbol
             */
            _chkIsClusterSymbolAntialiased = new Button(container, SWT.CHECK);
            _chkIsClusterSymbolAntialiased.setText("Antialias symbol painting");
            _chkIsClusterSymbolAntialiased.addSelectionListener(_markerSelectionListener);
         }

         final Group group = new Group(container, SWT.NONE);
         group.setText(Messages.Slideout_Map25MarkerOptions_Group_MarkerLayout);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
         GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
         {
            createUI_220_Marker_Point(group);
            createUI_230_Marker_Cluster(group);
         }
      }

      return container;
   }

   private void createUI_220_Marker_Point(final Composite parent) {

//      {
//         // label: symbol
//         _lblMarkerColor = new Label(parent, SWT.NONE);
//         _lblMarkerColor.setText(Messages.Slideout_Map25MarkerOptions_Label_MarkerColor);
//         _lblMarkerColor.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_MarkerColor_Tooltip);
//         GridDataFactory.fillDefaults()
//               .align(SWT.FILL, SWT.CENTER)
//               .applyTo(_lblMarkerColor);
//
//         final Composite container = new Composite(parent, SWT.NONE);
//         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
//         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//         {
//            // outline color
//            _colorMarkerSymbol_Outline = new ColorSelectorExtended(container);
//            _colorMarkerSymbol_Outline.addListener(_markerPropertyChangeListener);
//            _colorMarkerSymbol_Outline.addOpenListener(this);
//
//            // fill color
//            _colorMarkerSymbol_Fill = new ColorSelectorExtended(container);
//            _colorMarkerSymbol_Fill.addListener(_markerPropertyChangeListener);
//            _colorMarkerSymbol_Fill.addOpenListener(this);
//
//            // button: swap color
//            _btnSwapMarkerColor = new Button(container, SWT.PUSH);
//            _btnSwapMarkerColor.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
//            _btnSwapMarkerColor.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
//            _btnSwapMarkerColor.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSwapMarkerColor()));
//         }
//      }
//      {
//         /*
//          * Opacity
//          */
//         // label
//         _lblMarkerOpacity = new Label(parent, SWT.NONE);
//         _lblMarkerOpacity.setText(Messages.Slideout_Map25MarkerOptions_Label_MarkerOpacity);
//         _lblMarkerOpacity.setToolTipText(NLS.bind(Messages.Slideout_Map25MarkerOptions_Label_MarkerOpacity_Tooltip, UI.TRANSFORM_OPACITY_MAX));
//         GridDataFactory.fillDefaults()
//               .align(SWT.FILL, SWT.CENTER)
//               .applyTo(_lblMarkerOpacity);
//
//         /*
//          * Symbol
//          */
//         final Composite container = new Composite(parent, SWT.NONE);
//         GridDataFactory.fillDefaults()
//               .grab(true, false)
//               .applyTo(container);
//         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//         {
//            {
//               // spinner: outline
//               _spinnerMarkerOutline_Opacity = new Spinner(container, SWT.BORDER);
//               _spinnerMarkerOutline_Opacity.setMinimum(0);
//               _spinnerMarkerOutline_Opacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
//               _spinnerMarkerOutline_Opacity.setIncrement(1);
//               _spinnerMarkerOutline_Opacity.setPageIncrement(10);
//               _spinnerMarkerOutline_Opacity.addSelectionListener(_markerSelectionListener);
//               _spinnerMarkerOutline_Opacity.addMouseWheelListener(_markerMouseWheelListener);
//               _spinnerGridData.applyTo(_spinnerMarkerOutline_Opacity);
//            }
//            {
//               // spinner: fill
//               _spinnerMarkerFill_Opacity = new Spinner(container, SWT.BORDER);
//               _spinnerMarkerFill_Opacity.setMinimum(0);
//               _spinnerMarkerFill_Opacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
//               _spinnerMarkerFill_Opacity.setIncrement(1);
//               _spinnerMarkerFill_Opacity.setPageIncrement(10);
//               _spinnerMarkerFill_Opacity.addSelectionListener(_markerSelectionListener);
//               _spinnerMarkerFill_Opacity.addMouseWheelListener(_markerMouseWheelListener);
//               _spinnerGridData.applyTo(_spinnerMarkerFill_Opacity);
//            }
//         }
//      }
//      {
//         /*
//          * Size
//          */
//
//         // label: size
//         _lblMarkerSize = new Label(parent, SWT.NONE);
//         _lblMarkerSize.setText(Messages.Slideout_Map25MarkerOptions_Label_MarkerSize);
//         _lblMarkerSize.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_MarkerSize_Tooltip);
//         GridDataFactory.fillDefaults()
//               .align(SWT.FILL, SWT.CENTER)
//               .applyTo(_lblMarkerSize);
//
//         final Composite container = new Composite(parent, SWT.NONE);
//         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
//         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//         {
//            // outline size
//            _spinnerMarkerOutline_Size = new Spinner(container, SWT.BORDER);
//            _spinnerMarkerOutline_Size.setMinimum((int) Map2ConfigManager.MARKER_OUTLINE_SIZE_MIN);
//            _spinnerMarkerOutline_Size.setMaximum((int) Map2ConfigManager.MARKER_OUTLINE_SIZE_MAX);
//            _spinnerMarkerOutline_Size.setIncrement(1);
//            _spinnerMarkerOutline_Size.setPageIncrement(10);
//            _spinnerMarkerOutline_Size.addSelectionListener(_markerSelectionListener);
//            _spinnerMarkerOutline_Size.addMouseWheelListener(_markerMouseWheelListener);
//            _spinnerGridData.applyTo(_spinnerMarkerOutline_Size);
//
//            // symbol size
//            _spinnerMarkerSymbol_Size = new Spinner(container, SWT.BORDER);
//            _spinnerMarkerSymbol_Size.setMinimum(Map2ConfigManager.MARKER_SYMBOL_SIZE_MIN);
//            _spinnerMarkerSymbol_Size.setMaximum(Map2ConfigManager.MARKER_SYMBOL_SIZE_MAX);
//            _spinnerMarkerSymbol_Size.setIncrement(1);
//            _spinnerMarkerSymbol_Size.setPageIncrement(10);
//            _spinnerMarkerSymbol_Size.addSelectionListener(_markerSelectionListener);
//            _spinnerMarkerSymbol_Size.addMouseWheelListener(_markerMouseWheelListener);
//            _spinnerGridData.applyTo(_spinnerMarkerSymbol_Size);
//         }
//      }
   }

   private void createUI_230_Marker_Cluster(final Composite parent) {

      final int clusterIndent = UI.FORM_FIRST_COLUMN_INDENT;

      {
         /*
          * Cluster
          */

         // checkbox: Is clustering
         _chkIsMarkerClustered = new Button(parent, SWT.CHECK);
         _chkIsMarkerClustered.setText(Messages.Slideout_Map25MarkerOptions_Checkbox_IsMarkerClustering);
         _chkIsMarkerClustered.addSelectionListener(_markerSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .indent(0, 10)
               .applyTo(_chkIsMarkerClustered);
      }
      {
         /*
          * Cluster size
          */
         {
            // label
            _lblClusterGrid_Size = new Label(parent, SWT.NONE);
            _lblClusterGrid_Size.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterGridSize);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(clusterIndent, 0)
                  .applyTo(_lblClusterGrid_Size);

            // spinner
            _spinnerClusterGrid_Size = new Spinner(parent, SWT.BORDER);
            _spinnerClusterGrid_Size.setMinimum(0);
            _spinnerClusterGrid_Size.setMaximum(Map2ConfigManager.CLUSTER_GRID_MAX_SIZE);
            _spinnerClusterGrid_Size.setIncrement(1);
            _spinnerClusterGrid_Size.setPageIncrement(10);
            _spinnerClusterGrid_Size.addSelectionListener(_markerSelectionListener);
            _spinnerClusterGrid_Size.addMouseWheelListener(_markerMouseWheelListener);
         }
      }
      {
         /*
          * Cluster symbol size
          */
         {
            // label
            _lblClusterSymbol_Size = new Label(parent, SWT.NONE);
            _lblClusterSymbol_Size.setText(Messages.Slideout_MapLocation_Label_ClusterSymbolSize);
            _lblClusterSymbol_Size.setToolTipText(Messages.Slideout_MapLocation_Label_ClusterSize_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(clusterIndent, 0)
                  .applyTo(_lblClusterSymbol_Size);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
            {
               // spinner: symbol size
               _spinnerClusterSymbol_Size = new Spinner(container, SWT.BORDER);
               _spinnerClusterSymbol_Size.setToolTipText(Messages.Slideout_MapLocation_Label_ClusterSize_Tooltip);
               _spinnerClusterSymbol_Size.setMinimum(1);
               _spinnerClusterSymbol_Size.setMaximum(Map2ConfigManager.CLUSTER_SYMBOL_SIZE_MAX);
               _spinnerClusterSymbol_Size.setIncrement(1);
               _spinnerClusterSymbol_Size.setPageIncrement(10);
               _spinnerClusterSymbol_Size.addSelectionListener(_markerSelectionListener);
               _spinnerClusterSymbol_Size.addMouseWheelListener(_markerMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerClusterSymbol_Size);

               // outline width
               _spinnerClusterOutline_Width = new Spinner(container, SWT.BORDER);
               _spinnerClusterOutline_Width.setToolTipText(Messages.Slideout_MapLocation_Label_ClusterSize_Tooltip);
               _spinnerClusterOutline_Width.setMinimum(Map2ConfigManager.CLUSTER_OUTLINE_WIDTH_MIN);
               _spinnerClusterOutline_Width.setMaximum(Map2ConfigManager.CLUSTER_OUTLINE_WIDTH_MAX);
               _spinnerClusterOutline_Width.setIncrement(1);
               _spinnerClusterOutline_Width.setPageIncrement(10);
               _spinnerClusterOutline_Width.addSelectionListener(_markerSelectionListener);
               _spinnerClusterOutline_Width.addMouseWheelListener(_markerMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerClusterOutline_Width);
            }
         }
      }
      {
         /*
          * Symbol color
          */
         {
            // label
            _lblClusterSymbol = new Label(parent, SWT.NONE);
            _lblClusterSymbol.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor);
            _lblClusterSymbol.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(clusterIndent, 0)
                  .applyTo(_lblClusterSymbol);
         }

         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // foreground color
            _colorClusterSymbol_Outline = new ColorSelectorExtended(container);
            _colorClusterSymbol_Outline.addListener(_markerPropertyChangeListener);
            _colorClusterSymbol_Outline.addOpenListener(this);
            _colorClusterSymbol_Outline.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor_Tooltip);

            // foreground color
            _colorClusterSymbol_Fill = new ColorSelectorExtended(container);
            _colorClusterSymbol_Fill.addListener(_markerPropertyChangeListener);
            _colorClusterSymbol_Fill.addOpenListener(this);
            _colorClusterSymbol_Fill.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor_Tooltip);

            // button: swap color
            _btnSwapClusterSymbolColor = new Button(container, SWT.PUSH);
            _btnSwapClusterSymbolColor.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapClusterSymbolColor.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapClusterSymbolColor.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSwapClusterColor()));
         }
      }
      {
         /*
          * Fill symbol
          */
         {
            // checkbox: fill cluster symbol
            _chkIsFillClusterSymbol = new Button(parent, SWT.CHECK);
            _chkIsFillClusterSymbol.setText(Messages.Slideout_MapLocation_Checkbox_FillClusterSymbol);
            _chkIsFillClusterSymbol.addSelectionListener(_markerSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(clusterIndent, 0)
                  .applyTo(_chkIsFillClusterSymbol);
         }
      }
   }

   private Control createUI_500_Tab_CommonLocations(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 5, 0).numColumns(1).applyTo(container);
      {
         createUI_510_LocationViewer(container);
         createUI_520_Actions(container);
      }

      return container;
   }

   private void createUI_510_LocationViewer(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            /*
             * Show common locations
             */
            _chkIsShowCommonLocations = new Button(container, SWT.CHECK);
            _chkIsShowCommonLocations.setText(Messages.Slideout_MapLocation_Checkbox_ShowCommonLocations);
            _chkIsShowCommonLocations.setToolTipText(Messages.Slideout_MapLocation_Checkbox_ShowCommonLocations_Tooltip);
            _chkIsShowCommonLocations.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().applyTo(_chkIsShowCommonLocations);
         }
         {
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_MapLocation_Label_CommonLocations);
         }
         {
            _viewerContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
            GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
            {
               createUI_522_Table(_viewerContainer);
            }
         }

//         createUI_680_ViewerActions(container);
      }
   }

   private void createUI_520_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.END, SWT.FILL)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Button: Delete
             */
            _btnDeleteCommonLocation = new Button(container, SWT.PUSH);
            _btnDeleteCommonLocation.setText(Messages.App_Action_Delete);
            _btnDeleteCommonLocation.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onLocation_Delete()));

            // set button default width
            UI.setButtonLayoutData(_btnDeleteCommonLocation);
         }
      }
   }

   private void createUI_522_Table(final Composite parent) {

      /*
       * Create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI);

      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);
      table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      table.addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> {

         if (keyEvent.keyCode == SWT.DEL) {
            onLocation_Delete();
         }
      }));

      /*
       * Create table viewer
       */
      _mapCommonLocationViewer = new TableViewer(table);

      _columnManager.createColumns(_mapCommonLocationViewer);
      _columnManager.setSlideoutShell(this);

      _mapCommonLocationViewer.setUseHashlookup(true);
      _mapCommonLocationViewer.setContentProvider(new MapLocationContentProvider());
      _mapCommonLocationViewer.setComparator(_mapLocationComparator);

      _mapCommonLocationViewer.addSelectionChangedListener(selectionChangedEvent -> onLocation_Select(selectionChangedEvent));
//    _mapLocationViewer.addDoubleClickListener(doubleClickEvent -> onGeoFilter_ToggleReadEditMode());

      updateUI_SetSortDirection(
            _mapLocationComparator.__sortColumnId,
            _mapLocationComparator.__sortDirection);

      // set info tooltip provider
      _locationTooltip = new TourLocationToolTip(this);

      // ensure that tooltips are hidden
      table.addListener(SWT.MouseExit, (event) -> hideTooltip());

      createUI_524_ContextMenu();
   }

   /**
    * Ceate the view context menus
    */
   private void createUI_524_ContextMenu() {

      _tableContextMenu = createUI_525_CreateViewerContextMenu();

      _columnManager.createHeaderContextMenu(

            (Table) _mapCommonLocationViewer.getControl(),
            _tableViewerContextMenuProvider);
   }

   private Menu createUI_525_CreateViewerContextMenu() {

      final Table table = (Table) _mapCommonLocationViewer.getControl();
      final Menu tableContextMenu = _viewerMenuManager.createContextMenu(table);

      return tableContextMenu;
   }

   private void defineAllColumns() {

      defineColumn_00_SequenceNumber();
      defineColumn_05_LocationName();
      defineColumn_30_Zoomlevel();
      defineColumn_40_BoundingBox_Width();
      defineColumn_42_BoundingBox_Height();

      defineColumn_Geo_20_Latitude();
      defineColumn_Geo_22_Longitude();

      defineColumn_99_Created();

      new ColumnDefinitionFor1stVisibleAlignmentColumn(_columnManager);
   }

   private void defineColumn_00_SequenceNumber() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_SEQUENCE, SWT.TRAIL);

      colDef.setColumnLabel(Messages.GeoCompare_View_Column_SequenceNumber_Label);
      colDef.setColumnHeaderText(Messages.GeoCompare_View_Column_SequenceNumber_Header);
      colDef.setColumnHeaderToolTipText(Messages.GeoCompare_View_Column_SequenceNumber_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final int indexOf = _mapCommonLocationViewer.getTable().indexOf((TableItem) cell.getItem());

            cell.setText(Integer.toString(indexOf + 1));
         }
      });

   }

   /**
    * Column: Number of geo parts
    */
   private void defineColumn_05_LocationName() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LOCATION_NAME, SWT.LEAD);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_FilterName_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_FilterName_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new TooltipLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourLocation item = (TourLocation) cell.getElement();

            if (UI.IS_SCRAMBLE_DATA) {

               cell.setText(UI.scrambleText(item.name));

            } else {

               cell.setText(item.name);
            }
         }
      });
   }

   /**
    * Column: Zoomlevel
    */
   private void defineColumn_30_Zoomlevel() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_ZOOM_LEVEL, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Map_Bookmark_Column_ZoomLevel2_Tooltip);
      colDef.setColumnHeaderText(Messages.Map_Bookmark_Column_ZoomLevel2);
      colDef.setColumnHeaderToolTipText(Messages.Map_Bookmark_Column_ZoomLevel2_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(5));

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourLocation item = (TourLocation) cell.getElement();

            cell.setText(Integer.toString(item.zoomlevel + 0));
         }
      });
   }

   private void defineColumn_40_BoundingBox_Width() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_BOUNDING_BOX_WIDTH.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourLocation tourLocation = (TourLocation) cell.getElement();

            cell.setText(FormatManager.formatNumber_0(tourLocation.boundingBoxWidth));
         }
      });
   }

   private void defineColumn_42_BoundingBox_Height() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_BOUNDING_BOX_HEIGHT.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourLocation tourLocation = ((TourLocation) cell.getElement());

            cell.setText(FormatManager.formatNumber_0(tourLocation.boundingBoxHeight));
         }
      });
   }

   /**
    * Column: Created
    */
   private void defineColumn_99_Created() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_CREATED_DATE_TIME, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_Created_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_Created_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourLocation item = (TourLocation) cell.getElement();
            final ZonedDateTime created = item.getCreated();

            if (created != null) {

               cell.setText(created.format(TimeTools.Formatter_DateTime_SM));
            }
         }
      });
   }

   private void defineColumn_Geo_20_Latitude() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_LATITUDE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(_nf3.format(((TourLocation) cell.getElement()).latitude));
         }
      });
   }

   private void defineColumn_Geo_22_Longitude() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_LONGITUDE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(_nf3.format(((TourLocation) cell.getElement()).longitude));
         }
      });
   }

   private void enableControls() {

// SET_FORMATTING_OFF


      /*
       * Tour marker
       */
      final boolean isShowTourMarker      = _chkIsShowTourMarker.getSelection();
      final boolean isMarkerClustered     = _chkIsMarkerClustered.getSelection();

      final boolean isShowClusteredMarker = isShowTourMarker && isMarkerClustered;

      _btnSwapClusterSymbolColor       .setEnabled(isShowClusteredMarker);

      _chkIsClusterSymbolAntialiased   .setEnabled(isShowTourMarker);
      _chkIsClusterTextAntialiased     .setEnabled(isShowTourMarker);
      _chkIsMarkerClustered            .setEnabled(isShowTourMarker);
      _chkIsFillClusterSymbol          .setEnabled(isShowClusteredMarker);

      _lblClusterGrid_Size             .setEnabled(isShowClusteredMarker);
      _lblClusterSymbol                .setEnabled(isShowClusteredMarker);
      _lblClusterSymbol_Size           .setEnabled(isShowClusteredMarker);

      _spinnerClusterGrid_Size         .setEnabled(isShowClusteredMarker);
      _spinnerClusterSymbol_Size       .setEnabled(isShowClusteredMarker);
      _spinnerClusterOutline_Width     .setEnabled(isShowClusteredMarker);

      _colorClusterSymbol_Fill         .setEnabled(isShowClusteredMarker);
      _colorClusterSymbol_Outline      .setEnabled(isShowClusteredMarker);

      /*
       * Common locations
       */
      final List<TourLocation> allSelectedLocations = getSelectedLocations();

      final boolean isShowCommonLocations       = _chkIsShowCommonLocations.getSelection();
      final boolean isCommonLocationSelected    = isShowCommonLocations && allSelectedLocations.size() > 0;

      _btnDeleteCommonLocation.setEnabled(isCommonLocationSelected);

      _mapCommonLocationViewer.getTable().setEnabled(isShowCommonLocations);

// SET_FORMATTING_ON

      updateUI_TabLabel();
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(_actionDeleteLocation);

      enableControls();
   }

   @Override
   public ColumnManager getColumnManager() {

      return _columnManager;
   }

   public TableViewer getLocationViewer() {

      return _mapCommonLocationViewer;
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private List<TourLocation> getSelectedLocations() {

      @SuppressWarnings("unchecked")
      final List<TourLocation> allSelectedLocations = _mapCommonLocationViewer.getStructuredSelection().toList();

      return allSelectedLocations;
   }

   /**
    * @param sortColumnId
    *
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _mapCommonLocationViewer.getTable().getColumns();

      for (final TableColumn column : allColumns) {

         final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

         if (columnId.equals(sortColumnId)) {
            return column;
         }
      }

      return allColumns[0];
   }

   @Override
   public ColumnViewer getViewer() {
      return _mapCommonLocationViewer;
   }

   /**
    * Hide the tooltip when mouse is not hovering the tooltip and the mouse have exited the view
    */
   private void hideTooltip() {

      _viewerContainer.getDisplay().timerExec(100, () -> {

         if (_locationTooltip.isMouseHovered() == false) {

            _locationTooltip.hide();
         }
      });
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      // force spinner controls to have the same width
      _spinnerGridData = GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(3), SWT.DEFAULT);

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSelectSortColumn(e);
         }
      };

      _markerSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModifyMarkerConfig());
      _markerPropertyChangeListener = propertyChangeEvent -> onModifyMarkerConfig();
      _markerMouseWheelListener = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 1);
         onModifyMarkerConfig();
      };

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
         }
      };
   }

   @Override
   public boolean isColumn0Visible(final ColumnViewer columnViewer) {

      final TableColumn[] allColumns = _mapCommonLocationViewer.getTable().getColumns();

      if (allColumns.length > 0) {

         final TableColumn column = allColumns[0];
         final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

         if (ColumnDefinitionFor1stVisibleAlignmentColumn.COLUMN_ID.equals(columnId)) {
            return true;
         }
      }

      return false;
   }

   private void onChangeUI() {

// SET_FORMATTING_OFF


      _state_Map2.put(Map2View.STATE_IS_SHOW_COMMON_LOCATIONS,             _chkIsShowCommonLocations.getSelection());
      _state_Map2.put(Map2View.STATE_IS_SHOW_TOUR_LOCATIONS,               _chkIsShowTourLocations.getSelection());

      _state_Map2.put(Map2View.STATE_IS_SHOW_LOCATION_BOUNDING_BOX,    _chkIsShowMapLocations_BoundingBox.getSelection());

// SET_FORMATTING_ON

      repaintMap();

      enableControls();
   }

   @Override
   protected void onDispose() {

      if (_prefChangeListener != null) {

         _prefStore.removePropertyChangeListener(_prefChangeListener);
      }

      super.onDispose();
   }

   @Override
   protected void onFocus() {

   }

   private void onLocation_Delete() {

      final List<TourLocation> allSelectedLocations = getSelectedLocations();

      // update model
      if (CommonLocationManager.deleteLocations(allSelectedLocations) == false) {
         return;
      }

      /*
       * Deletion was performed -> update viewer
       */

      final Table table = _mapCommonLocationViewer.getTable();

      // get index for selected location
      final int lastLocationIndex = table.getSelectionIndex();

      // reload viewer
      reloadViewer();

      // get next location
      TourLocation nextLocationItem = (TourLocation) _mapCommonLocationViewer.getElementAt(lastLocationIndex);

      if (nextLocationItem == null) {
         nextLocationItem = (TourLocation) _mapCommonLocationViewer.getElementAt(lastLocationIndex - 1);
      }

      // select next location
      if (nextLocationItem != null) {
         _mapCommonLocationViewer.setSelection(new StructuredSelection(nextLocationItem), true);
      }

      table.setFocus();

      TourManager.fireEventWithCustomData(
            TourEventId.COMMON_LOCATION_SELECTION,
            null,
            null);
   }

   private void onLocation_Select(final SelectionChangedEvent selectionChangedEvent) {

      final IStructuredSelection selection = _mapCommonLocationViewer.getStructuredSelection();

      if (selection.isEmpty()) {
         return;
      }

      enableControls();

      // fire selection
      TourManager.fireEventWithCustomData(
            TourEventId.COMMON_LOCATION_SELECTION,
            selection.toList(),
            null);
   }

   private void onModifyMarkerConfig() {

      saveMarkerConfig();

      enableControls();

      repaintMap();
   }

   private void onSelectSortColumn(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
//         final ISelection selectionBackup = getViewerSelection();
         final ISelection selectionBackup = _mapCommonLocationViewer.getStructuredSelection();
         {
            // update viewer with new sorting
            _mapLocationComparator.setSortColumn(e.widget);
            _mapCommonLocationViewer.refresh();
         }
         updateUI_SelectMapLocationItem(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   private void onSwapClusterColor() {

      final Map2MarkerConfig config = Map2ConfigManager.getActiveMarkerConfig();

      final RGB fgColor = config.clusterOutline_RGB;
      final RGB bgColor = config.clusterFill_RGB;

      config.clusterOutline_RGB = bgColor;
      config.clusterFill_RGB = fgColor;

      config.setupColors();

      restoreState();
      repaintMap();
   }

   private void onSwapMarkerColor() {

//      final Map2MarkerConfig config = Map2ConfigManager.getActiveMarkerConfig();
//
//      final RGB fgColor = config.markerOutline_Color;
//      final RGB bgColor = config.markerFill_Color;
//
//      config.markerOutline_Color = bgColor;
//      config.markerFill_Color = fgColor;
//
//      restoreState();
//      onModifyMarkerConfig();
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         _mapCommonLocationViewer.getTable().dispose();

         createUI_522_Table(_viewerContainer);
         _viewerContainer.layout();

         // update the viewer
         reloadViewer();
      }
      _viewerContainer.setRedraw(true);

      return _mapCommonLocationViewer;
   }

   @Override
   public void reloadViewer() {

      updateUI_Viewer();
   }

   private void repaintMap() {

      _map2View.getMap().paint();
   }

   private void restoreState() {

      _chkIsShowCommonLocations.setSelection(Util.getStateBoolean(_state_Map2,
            Map2View.STATE_IS_SHOW_COMMON_LOCATIONS,
            Map2View.STATE_IS_SHOW_COMMON_LOCATIONS_DEFAULT));

      _chkIsShowMapLocations_BoundingBox.setSelection(Util.getStateBoolean(_state_Map2,
            Map2View.STATE_IS_SHOW_LOCATION_BOUNDING_BOX,
            Map2View.STATE_IS_SHOW_LOCATION_BOUNDING_BOX_DEFAULT));

      _chkIsShowTourLocations.setSelection(Util.getStateBoolean(_state_Map2,
            Map2View.STATE_IS_SHOW_TOUR_LOCATIONS,
            Map2View.STATE_IS_SHOW_TOUR_LOCATIONS_DEFAULT));

      updateUI_TabLabel();

      /*
       * Tour marker
       */
      final Map2MarkerConfig config = Map2ConfigManager.getActiveMarkerConfig();

// SET_FORMATTING_OFF

      _chkIsShowTourMarker             .setSelection( config.isShowTourMarker);


      _chkIsMarkerClustered            .setSelection( config.isMarkerClustered);
      _chkIsClusterSymbolAntialiased   .setSelection( config.isClusterSymbolAntialiased);
      _chkIsClusterTextAntialiased     .setSelection( config.isClusterTextAntialiased);
      _chkIsFillClusterSymbol          .setSelection( config.isFillClusterSymbol);

      _spinnerClusterGrid_Size         .setSelection( config.clusterGridSize);
      _spinnerClusterOutline_Width     .setSelection( config.clusterOutline_Width);
      _spinnerClusterSymbol_Size       .setSelection( config.clusterSymbol_Size);

      _colorClusterSymbol_Fill         .setColorValue(config.clusterFill_RGB);
      _colorClusterSymbol_Outline      .setColorValue(config.clusterOutline_RGB);

// SET_FORMATTING_ON
   }

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state_Slideout, STATE_SORT_COLUMN_ID, TableColumnFactory.SENSOR_NAME_ID);
      final int sortDirection = Util.getStateInt(_state_Slideout, STATE_SORT_COLUMN_DIRECTION, MapLocationComparator.ASCENDING);

      // update comparator
      _mapLocationComparator.__sortColumnId = sortColumnId;
      _mapLocationComparator.__sortDirection = sortDirection;
   }

   private void restoreTabFolder() {

      _tabFolder.setSelection(Util.getStateInt(_state_Slideout, STATE_SELECTED_TAB, 0));
   }

   private void saveMarkerConfig() {

      final Map2MarkerConfig config = Map2ConfigManager.getActiveMarkerConfig();

// SET_FORMATTING_OFF

      config.isShowTourMarker             = _chkIsShowTourMarker           .getSelection();


      config.isMarkerClustered            = _chkIsMarkerClustered          .getSelection();
      config.isClusterSymbolAntialiased   = _chkIsClusterSymbolAntialiased .getSelection();
      config.isClusterTextAntialiased     = _chkIsClusterTextAntialiased   .getSelection();
      config.isFillClusterSymbol          = _chkIsFillClusterSymbol        .getSelection();

      config.clusterGridSize              = _spinnerClusterGrid_Size       .getSelection();
      config.clusterOutline_Width         = _spinnerClusterOutline_Width   .getSelection();
      config.clusterSymbol_Size           = _spinnerClusterSymbol_Size     .getSelection();
      config.clusterFill_RGB              = _colorClusterSymbol_Fill       .getColorValue();
      config.clusterOutline_RGB           = _colorClusterSymbol_Outline    .getColorValue();

// SET_FORMATTING_ON

      config.setupColors();
   }

   @Override
   protected void saveState() {

      _columnManager.saveState(_state_Slideout);

      _state_Slideout.put(STATE_SELECTED_TAB, _tabFolder.getSelectionIndex());

      _state_Slideout.put(STATE_SORT_COLUMN_ID, _mapLocationComparator.__sortColumnId);
      _state_Slideout.put(STATE_SORT_COLUMN_DIRECTION, _mapLocationComparator.__sortDirection);

      super.saveState();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   public void updateUI() {

      restoreState();
   }

   public void updateUI(final TourLocation tourLocation) {

      _mapCommonLocationViewer.refresh();

      _mapCommonLocationViewer.setSelection(new StructuredSelection(tourLocation), true);
      _mapCommonLocationViewer.getTable().showSelection();
   }

   /**
    * Select and reveal a compare item item.
    *
    * @param selection
    */
   private void updateUI_SelectMapLocationItem(final ISelection selection) {

//      _isInUpdate = true;
      {
         _mapCommonLocationViewer.setSelection(selection, true);
         _mapCommonLocationViewer.getTable().showSelection();

//       // focus can have changed when resorted, set focus to the selected item
//       int selectedIndex = 0;
//       final Table table = _geoFilterViewer.getTable();
//       final TableItem[] items = table.getItems();
//       for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
//
//          final TableItem tableItem = items[itemIndex];
//
//          if (tableItem.getData() == selectedProfile) {
//             selectedIndex = itemIndex;
//          }
//       }
//       table.setSelection(selectedIndex);
//       table.showSelection();

      }
//      _isInUpdate = false;
   }

   /**
    * Set the sort column direction indicator for a column.
    *
    * @param sortColumnId
    * @param isAscendingSort
    */
   private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

      final Table table = _mapCommonLocationViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(sortDirection == MapLocationComparator.ASCENDING ? SWT.UP : SWT.DOWN);
   }

   private void updateUI_TabLabel() {

      final boolean isShowCommonLocations = _chkIsShowCommonLocations.getSelection();
      final boolean isShowTourMarker = _chkIsShowTourMarker.getSelection();

      // show a flag in the tab label when content is enabled

      _tabCommonLocations.setText(isShowCommonLocations
            ? UI.SYMBOL_STAR + UI.SPACE + Messages.Slideout_MapLocation_Tab_CommonLocations
            : Messages.Slideout_MapLocation_Tab_CommonLocations);

      _tabTourMarkers.setText(isShowTourMarker
            ? UI.SYMBOL_STAR + UI.SPACE + Messages.Slideout_MapLocation_Tab_TourMarkers
            : Messages.Slideout_MapLocation_Tab_TourMarkers);
   }

   private void updateUI_Viewer() {

      _mapCommonLocationViewer.setInput(new Object[0]);
   }

}