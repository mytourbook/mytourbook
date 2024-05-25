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

import de.byteholder.geoclipse.map.Map2;

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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Slideout for all 2D map locations and marker
 */
public class SlideoutMap2_MapPoints extends AdvancedSlideout implements ITourViewer2, IColorSelectorListener {

   private static final String     COLUMN_CREATED_DATE_TIME        = "createdDateTime";                         //$NON-NLS-1$
   private static final String     COLUMN_LOCATION_NAME            = "LocationName";                            //$NON-NLS-1$
   private static final String     COLUMN_SEQUENCE                 = "sequence";                                //$NON-NLS-1$
   private static final String     COLUMN_ZOOM_LEVEL               = "zoomLevel";                               //$NON-NLS-1$
   //
   private static final String     STATE_SELECTED_TAB              = "STATE_SELECTED_TAB";                      //$NON-NLS-1$
   private static final String     STATE_SORT_COLUMN_DIRECTION     = "STATE_SORT_COLUMN_DIRECTION";             //$NON-NLS-1$
   private static final String     STATE_SORT_COLUMN_ID            = "STATE_SORT_COLUMN_ID";                    //$NON-NLS-1$
   //
   /**
    * MUST be in sync with {@link #_allMarkerLabelLayout_Label}
    */
   private static MapLabelLayout[] _allMarkerLabelLayout_Value     = {

         MapLabelLayout.RECTANGLE_BOX,
         MapLabelLayout.BORDER_2_PIXEL,
         MapLabelLayout.BORDER_1_PIXEL,
         MapLabelLayout.SHADOW,
         MapLabelLayout.NONE,

   };
   //
   /**
    * MUST be in sync with {@link #_allMarkerLabelLayout_Value}
    */
   private static String[]         _allMarkerLabelLayout_Label     = {

         "Rectangle Box",
         "Border 2",
         "Border 1",
         "Shadow",
         "None",

   };
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
   private SelectionListener       _markerSelectionListener;
   private IPropertyChangeListener _markerPropertyChangeListener;
   private MouseWheelListener      _markerMouseWheelListener;
   private MouseWheelListener      _markerMouseWheelListener10;
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
   private CTabItem              _tabTourLocations;
   private CTabItem              _tabCommonLocations;
   private CTabItem              _tabOptions;
   private CTabItem              _tabTourMarkers;
   private CTabItem              _tabTourMarkerGroups;
   //
   private Button                _btnDeleteCommonLocation;
   private Button                _btnSwapClusterSymbolColor;
   private Button                _btnSwapLocationLabel_Color;
   private Button                _btnSwapLocationLabel_Hovered_Color;
   private Button                _btnSwapMarkerLabel_Color;
   private Button                _btnSwapMarkerLabel_Hovered_Color;
   //
   private Button                _chkIsClusterSymbolAntialiased;
   private Button                _chkIsClusterTextAntialiased;
   private Button                _chkIsDimMap;
   private Button                _chkIsFillClusterSymbol;
   private Button                _chkIsGroupDuplicatedMarkers;
   private Button                _chkIsMarkerClustered;
   private Button                _chkIsLabelAntialiased;
   private Button                _chkIsShowCommonLocations;
   private Button                _chkIsShowMapLocations_BoundingBox;
   private Button                _chkIsShowTourLocations;
   private Button                _chkIsShowTourMarker;
   private Button                _chkUseMapDimColor;
   //
   private Combo                 _comboLabelLayout;
   //
   private Label                 _lblClusterGrid_Size;
   private Label                 _lblClusterSymbol;
   private Label                 _lblClusterSymbol_Size;
   private Label                 _lblGroupDuplicatedMarkers;
   private Label                 _lblLabelGroupGridSize;
   private Label                 _lblVisibleLabels;
   private Label                 _lblLabelWrapLength;
   private Label                 _lblLocationLabel_Color;
   private Label                 _lblLocationLabel_HoveredColor;
   private Label                 _lblMarkerLabel_Color;
   private Label                 _lblMarkerLabel_HoveredColor;
   private Label                 _lblLabelBackgrouond;
   //
   private Spinner               _spinnerClusterGrid_Size;
   private Spinner               _spinnerClusterOutline_Width;
   private Spinner               _spinnerClusterSymbol_Size;
   private Spinner               _spinnerLabelDistributorMaxLabels;
   private Spinner               _spinnerLabelDistributorRadius;
   private Spinner               _spinnerLabelGroupGridSize;
   private Spinner               _spinnerLabelWrapLength;
   private Spinner               _spinnerMapDimValue;
   //
   private Text                  _txtGroupDuplicatedMarkers;
   //
   private ColorSelectorExtended _colorClusterSymbol_Fill;
   private ColorSelectorExtended _colorClusterSymbol_Outline;
   private ColorSelectorExtended _colorLocationLabel_Fill;
   private ColorSelectorExtended _colorLocationLabel_Fill_Hovered;
   private ColorSelectorExtended _colorLocationLabel_Outline;
   private ColorSelectorExtended _colorLocationLabel_Outline_Hovered;
   private ColorSelectorExtended _colorMarkerLabel_Fill;
   private ColorSelectorExtended _colorMarkerLabel_Fill_Hovered;
   private ColorSelectorExtended _colorMarkerLabel_Outline;
   private ColorSelectorExtended _colorMarkerLabel_Outline_Hovered;
   private ColorSelectorExtended _colorMapDimColor;
   private ColorSelectorExtended _colorMapTransparencyColor;
   //
   private Image                 _imageMapLocation_Common;
   private Image                 _imageMapLocation_Tour;
   private Image                 _imageTourMarker;
   private Image                 _imageTourMarkerGroups;

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
   public SlideoutMap2_MapPoints(final ToolItem toolItem,
                                         final IDialogSettings map2State,
                                         final IDialogSettings slideoutState,
                                         final Map2View map2View) {

      super(toolItem.getParent(), slideoutState, new int[] { 325, 400, 325, 400 });

      _toolItem = toolItem;

      _state_Map2 = map2State;
      _state_Slideout = slideoutState;
      _map2View = map2View;

      setTitleText(Messages.Slideout_MapPoints_Label_Title);

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
      fillUI();

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
               _tabTourMarkers = new CTabItem(_tabFolder, SWT.NONE);
               _tabTourMarkers.setImage(_imageTourMarker);
               _tabTourMarkers.setToolTipText(Messages.Slideout_MapPoints_Tab_TourMarkers);
               _tabTourMarkers.setControl(createUI_200_Tab_Marker(_tabFolder));

               _tabTourMarkerGroups = new CTabItem(_tabFolder, SWT.NONE);
               _tabTourMarkerGroups.setImage(_imageTourMarkerGroups);
               _tabTourMarkerGroups.setToolTipText("Tour marker groups");
               _tabTourMarkerGroups.setControl(createUI_250_Tab_Groups(_tabFolder));

               _tabTourLocations = new CTabItem(_tabFolder, SWT.NONE);
               _tabTourLocations.setImage(_imageMapLocation_Tour);
               _tabTourLocations.setToolTipText(Messages.Slideout_MapPoints_Tab_TourLocations);
               _tabTourLocations.setControl(createUI_300_Tab_TourLocations(_tabFolder));

               _tabCommonLocations = new CTabItem(_tabFolder, SWT.NONE);
               _tabCommonLocations.setImage(_imageMapLocation_Common);
               _tabCommonLocations.setToolTipText(Messages.Slideout_MapPoints_Tab_CommonLocations);
               _tabCommonLocations.setControl(createUI_500_Tab_CommonLocations(_tabFolder));

               _tabOptions = new CTabItem(_tabFolder, SWT.NONE);
               _tabOptions.setText("Options");
               _tabOptions.setControl(createUI_800_Tab_Options(_tabFolder));
            }
         }
      }

      return shellContainer;
   }

   private Control createUI_200_Tab_Marker(final Composite parent) {

      final Composite tabContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(tabContainer);
      GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 5, 0).numColumns(1).applyTo(tabContainer);
      {
         {
            /*
             * Show tour marker
             */
            _chkIsShowTourMarker = new Button(tabContainer, SWT.CHECK);
            _chkIsShowTourMarker.setText(Messages.Slideout_Map25MarkerOptions_Checkbox_IsShowTourMarker);
            _chkIsShowTourMarker.addSelectionListener(_markerSelectionListener);
         }

         final Composite container = new Composite(tabContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            createUI_220_Marker_Label(container);
            createUI_230_Marker_Cluster(container);
         }
      }

      return tabContainer;
   }

   private void createUI_220_Marker_Label(final Composite parent) {

      final GridDataFactory labelGridData = GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.CENTER)
            .indent(UI.FORM_FIRST_COLUMN_INDENT, 0);

      {
         /*
          * Marker label
          */
         {
            // label
            _lblMarkerLabel_Color = new Label(parent, SWT.NONE);
            _lblMarkerLabel_Color.setText(Messages.Slideout_MapPoints_Label_MarkerColor);
            _lblMarkerLabel_Color.setToolTipText(Messages.Slideout_MapPoints_Label_MarkerColor_Tooltip);
            labelGridData.applyTo(_lblMarkerLabel_Color);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // outline/text color
            _colorMarkerLabel_Outline = new ColorSelectorExtended(container);
            _colorMarkerLabel_Outline.addListener(_markerPropertyChangeListener);
            _colorMarkerLabel_Outline.addOpenListener(this);
            _colorMarkerLabel_Outline.setToolTipText(Messages.Slideout_MapPoints_Label_MarkerColor_Tooltip);

            // background color
            _colorMarkerLabel_Fill = new ColorSelectorExtended(container);
            _colorMarkerLabel_Fill.addListener(_markerPropertyChangeListener);
            _colorMarkerLabel_Fill.addOpenListener(this);
            _colorMarkerLabel_Fill.setToolTipText(Messages.Slideout_MapPoints_Label_MarkerColor_Tooltip);

            // button: swap color
            _btnSwapMarkerLabel_Color = new Button(container, SWT.PUSH);
            _btnSwapMarkerLabel_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapMarkerLabel_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapMarkerLabel_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> onSwapMarkerColor()));
         }
      }
      {
         /*
          * Hovered marker label
          */
         {
            // label
            _lblMarkerLabel_HoveredColor = new Label(parent, SWT.NONE);
            _lblMarkerLabel_HoveredColor.setText("&Hovered label");
            _lblMarkerLabel_HoveredColor.setToolTipText(Messages.Slideout_MapPoints_Label_MarkerColor_Tooltip);
            labelGridData.applyTo(_lblMarkerLabel_HoveredColor);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // outline/text color
            _colorMarkerLabel_Outline_Hovered = new ColorSelectorExtended(container);
            _colorMarkerLabel_Outline_Hovered.addListener(_markerPropertyChangeListener);
            _colorMarkerLabel_Outline_Hovered.addOpenListener(this);
            _colorMarkerLabel_Outline_Hovered.setToolTipText(Messages.Slideout_MapPoints_Label_MarkerColor_Tooltip);

            // background color
            _colorMarkerLabel_Fill_Hovered = new ColorSelectorExtended(container);
            _colorMarkerLabel_Fill_Hovered.addListener(_markerPropertyChangeListener);
            _colorMarkerLabel_Fill_Hovered.addOpenListener(this);
            _colorMarkerLabel_Fill_Hovered.setToolTipText(Messages.Slideout_MapPoints_Label_MarkerColor_Tooltip);

            // button: swap color
            _btnSwapMarkerLabel_Hovered_Color = new Button(container, SWT.PUSH);
            _btnSwapMarkerLabel_Hovered_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapMarkerLabel_Hovered_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapMarkerLabel_Hovered_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> onSwapMarkerHoveredColor()));
         }
      }
   }

   private void createUI_230_Marker_Cluster(final Composite parent) {

      final int firstColumnIndent = UI.FORM_FIRST_COLUMN_INDENT;

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
         // label
         _lblClusterGrid_Size = new Label(parent, SWT.NONE);
         _lblClusterGrid_Size.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterGridSize);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .indent(firstColumnIndent, 0)
               .applyTo(_lblClusterGrid_Size);

         // spinner
         _spinnerClusterGrid_Size = new Spinner(parent, SWT.BORDER);
         _spinnerClusterGrid_Size.setMinimum(0);
         _spinnerClusterGrid_Size.setMaximum(Map2ConfigManager.CLUSTER_GRID_SIZE_MAX);
         _spinnerClusterGrid_Size.setIncrement(1);
         _spinnerClusterGrid_Size.setPageIncrement(10);
         _spinnerClusterGrid_Size.addSelectionListener(_markerSelectionListener);
         _spinnerClusterGrid_Size.addMouseWheelListener(_markerMouseWheelListener10);
      }
      {
         /*
          * Cluster symbol size
          */
         {
            // label
            _lblClusterSymbol_Size = new Label(parent, SWT.NONE);
            _lblClusterSymbol_Size.setText(Messages.Slideout_MapPoints_Label_ClusterSymbolSize);
            _lblClusterSymbol_Size.setToolTipText(Messages.Slideout_MapPoints_Label_ClusterSize_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(firstColumnIndent, 0)
                  .applyTo(_lblClusterSymbol_Size);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
            {
               // spinner: symbol size
               _spinnerClusterSymbol_Size = new Spinner(container, SWT.BORDER);
               _spinnerClusterSymbol_Size.setToolTipText(Messages.Slideout_MapPoints_Label_ClusterSize_Tooltip);
               _spinnerClusterSymbol_Size.setMinimum(1);
               _spinnerClusterSymbol_Size.setMaximum(Map2ConfigManager.CLUSTER_SYMBOL_SIZE_MAX);
               _spinnerClusterSymbol_Size.setIncrement(1);
               _spinnerClusterSymbol_Size.setPageIncrement(10);
               _spinnerClusterSymbol_Size.addSelectionListener(_markerSelectionListener);
               _spinnerClusterSymbol_Size.addMouseWheelListener(_markerMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerClusterSymbol_Size);

               // outline width
               _spinnerClusterOutline_Width = new Spinner(container, SWT.BORDER);
               _spinnerClusterOutline_Width.setToolTipText(Messages.Slideout_MapPoints_Label_ClusterSize_Tooltip);
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
                  .indent(firstColumnIndent, 0)
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
            _chkIsFillClusterSymbol.setText(Messages.Slideout_MapPoints_Checkbox_FillClusterSymbol);
            _chkIsFillClusterSymbol.addSelectionListener(_markerSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(firstColumnIndent, 0)
                  .applyTo(_chkIsFillClusterSymbol);
         }
      }
      {
         /*
          * Antialias text
          */
         _chkIsClusterTextAntialiased = new Button(parent, SWT.CHECK);
         _chkIsClusterTextAntialiased.setText("Antialias text painting");
         _chkIsClusterTextAntialiased.addSelectionListener(_markerSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .indent(firstColumnIndent, 0)
               .applyTo(_chkIsClusterTextAntialiased);
      }
      {
         /*
          * Antialias symbol
          */
         _chkIsClusterSymbolAntialiased = new Button(parent, SWT.CHECK);
         _chkIsClusterSymbolAntialiased.setText("Antialias symbol painting");
         _chkIsClusterSymbolAntialiased.addSelectionListener(_markerSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .indent(firstColumnIndent, 0)
               .applyTo(_chkIsClusterSymbolAntialiased);
      }
   }

   private Control createUI_250_Tab_Groups(final Composite parent) {

      final Composite tabContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(tabContainer);
      GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 5, 0).numColumns(2).applyTo(tabContainer);
      {
         {
            /*
             * Group duplicate markers
             */
            _chkIsGroupDuplicatedMarkers = new Button(tabContainer, SWT.CHECK);
            _chkIsGroupDuplicatedMarkers.setText("&Group markers with the same label");
            _chkIsGroupDuplicatedMarkers.setToolTipText("");
            _chkIsGroupDuplicatedMarkers.addSelectionListener(_markerSelectionListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .applyTo(_chkIsGroupDuplicatedMarkers);

         }
         {
            /*
             * Label group grid size
             */

            // label
            _lblLabelGroupGridSize = new Label(tabContainer, SWT.NONE);
            _lblLabelGroupGridSize.setText("Group grid &size");
            _lblLabelGroupGridSize.setToolTipText("");
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(UI.FORM_FIRST_COLUMN_INDENT, 0).applyTo(_lblLabelGroupGridSize);

            // spinner
            _spinnerLabelGroupGridSize = new Spinner(tabContainer, SWT.BORDER);
            _spinnerLabelGroupGridSize.setMinimum(Map2ConfigManager.LABEL_WRAP_LENGTH_MIN);
            _spinnerLabelGroupGridSize.setMaximum(Map2ConfigManager.LABEL_WRAP_LENGTH_MAX);
            _spinnerLabelGroupGridSize.setIncrement(1);
            _spinnerLabelGroupGridSize.setPageIncrement(10);
            _spinnerLabelGroupGridSize.addSelectionListener(_markerSelectionListener);
            _spinnerLabelGroupGridSize.addMouseWheelListener(_markerMouseWheelListener10);
         }
         {
            // label
            _lblGroupDuplicatedMarkers = new Label(tabContainer, SWT.NONE);
            _lblGroupDuplicatedMarkers.setText("Marker &labels which are grouped");
            _lblGroupDuplicatedMarkers.setToolTipText("");
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .span(2, 1)
                  .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
                  .applyTo(_lblGroupDuplicatedMarkers);

            _txtGroupDuplicatedMarkers = new Text(tabContainer, SWT.MULTI | SWT.WRAP | SWT.BORDER);
            _txtGroupDuplicatedMarkers.setToolTipText("");
            _txtGroupDuplicatedMarkers.addFocusListener(FocusListener.focusLostAdapter(focusEvent -> onModifyMarkerConfig()));
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .span(2, 1)
                  .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
                  .applyTo(_txtGroupDuplicatedMarkers);
         }
      }

      return tabContainer;
   }

   private Control createUI_300_Tab_TourLocations(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 5, 0).numColumns(2).applyTo(container);
      {
         createUI_310_Location_Label(container);
      }

      return container;
   }

   private void createUI_310_Location_Label(final Composite parent) {

      final GridDataFactory labelGridData = GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.CENTER)
            .indent(UI.FORM_FIRST_COLUMN_INDENT, 0);

      {
         /*
          * Show tour locations
          */
         _chkIsShowTourLocations = new Button(parent, SWT.CHECK);
         _chkIsShowTourLocations.setText(Messages.Slideout_MapPoints_Checkbox_ShowTourLocations);
         _chkIsShowTourLocations.setToolTipText(Messages.Slideout_MapPoints_Checkbox_ShowTourLocations_Tooltip);
         _chkIsShowTourLocations.addSelectionListener(_markerSelectionListener);
         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowTourLocations);
      }
      {
         /*
          * Location label
          */
         {
            // label
            _lblLocationLabel_Color = new Label(parent, SWT.NONE);
            _lblLocationLabel_Color.setText("Location &label");
            _lblLocationLabel_Color.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);
            labelGridData.applyTo(_lblLocationLabel_Color);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // outline/text color
            _colorLocationLabel_Outline = new ColorSelectorExtended(container);
            _colorLocationLabel_Outline.addListener(_markerPropertyChangeListener);
            _colorLocationLabel_Outline.addOpenListener(this);
            _colorLocationLabel_Outline.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

            // background color
            _colorLocationLabel_Fill = new ColorSelectorExtended(container);
            _colorLocationLabel_Fill.addListener(_markerPropertyChangeListener);
            _colorLocationLabel_Fill.addOpenListener(this);
            _colorLocationLabel_Fill.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

            // button: swap color
            _btnSwapLocationLabel_Color = new Button(container, SWT.PUSH);
            _btnSwapLocationLabel_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapLocationLabel_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapLocationLabel_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> onSwapLocationColor()));
         }
      }
      {
         /*
          * Hovered location label
          */
         {
            // label
            _lblLocationLabel_HoveredColor = new Label(parent, SWT.NONE);
            _lblLocationLabel_HoveredColor.setText("&Hovered label");
            _lblLocationLabel_HoveredColor.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);
            labelGridData.applyTo(_lblLocationLabel_HoveredColor);
         }
         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // outline/text color
            _colorLocationLabel_Outline_Hovered = new ColorSelectorExtended(container);
            _colorLocationLabel_Outline_Hovered.addListener(_markerPropertyChangeListener);
            _colorLocationLabel_Outline_Hovered.addOpenListener(this);
            _colorLocationLabel_Outline_Hovered.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

            // background color
            _colorLocationLabel_Fill_Hovered = new ColorSelectorExtended(container);
            _colorLocationLabel_Fill_Hovered.addListener(_markerPropertyChangeListener);
            _colorLocationLabel_Fill_Hovered.addOpenListener(this);
            _colorLocationLabel_Fill_Hovered.setToolTipText(Messages.Slideout_MapPoints_Label_LocationColor_Tooltip);

            // button: swap color
            _btnSwapLocationLabel_Hovered_Color = new Button(container, SWT.PUSH);
            _btnSwapLocationLabel_Hovered_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapLocationLabel_Hovered_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapLocationLabel_Hovered_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> onSwapLocationHoveredColor()));
         }
      }
      {
         /*
          * Show location bounding box
          */
         _chkIsShowMapLocations_BoundingBox = new Button(parent, SWT.CHECK);
         _chkIsShowMapLocations_BoundingBox.setText(Messages.Slideout_MapPoints_Checkbox_ShowLocationBoundingBox);
         _chkIsShowMapLocations_BoundingBox.addSelectionListener(_markerSelectionListener);
         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowMapLocations_BoundingBox);
      }
   }

   private Control createUI_500_Tab_CommonLocations(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 5, 0).numColumns(1).applyTo(container);
      {
         createUI_510_Viewer(container);
         createUI_520_Actions(container);
      }

      return container;
   }

   private void createUI_510_Viewer(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            /*
             * Show common locations
             */
            _chkIsShowCommonLocations = new Button(container, SWT.CHECK);
            _chkIsShowCommonLocations.setText(Messages.Slideout_MapPoints_Checkbox_ShowCommonLocations);
            _chkIsShowCommonLocations.setToolTipText(Messages.Slideout_MapPoints_Checkbox_ShowCommonLocations_Tooltip);
            _chkIsShowCommonLocations.addSelectionListener(_markerSelectionListener);
            GridDataFactory.fillDefaults().applyTo(_chkIsShowCommonLocations);
         }
         {
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_MapPoints_Label_CommonLocations);
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

   private Control createUI_800_Tab_Options(final Composite parent) {

      final GridDataFactory gdLabel = GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER);
      final GridDataFactory gdSpan2 = GridDataFactory.fillDefaults().span(2, 1);

      final Composite tabContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(tabContainer);
      GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 5, 0).numColumns(2).applyTo(tabContainer);
      {
         {
            /*
             * Marker background
             */

            // label
            _lblLabelBackgrouond = new Label(tabContainer, SWT.NONE);
            _lblLabelBackgrouond.setText("Label &background");
            _lblLabelBackgrouond.setToolTipText("");
            gdLabel.applyTo(_lblLabelBackgrouond);

            // combo
            _comboLabelLayout = new Combo(tabContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboLabelLayout.setVisibleItemCount(20);
            _comboLabelLayout.addSelectionListener(_markerSelectionListener);
            _comboLabelLayout.addFocusListener(_keepOpenListener);
         }
         {
            /*
             * Visible labels
             */
            // label
            _lblVisibleLabels = new Label(tabContainer, SWT.NONE);
            _lblVisibleLabels.setText("&Visible labels");
            _lblVisibleLabels.setToolTipText("• Number of visible labels\n• Label spreader radius");
            gdLabel.applyTo(_lblVisibleLabels);

            final Composite container = new Composite(tabContainer, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
            {
               // number of visible labels
               _spinnerLabelDistributorMaxLabels = new Spinner(container, SWT.BORDER);
               _spinnerLabelDistributorMaxLabels.setMinimum(Map2ConfigManager.LABEL_DISTRIBUTOR_MAX_LABELS_MIN);
               _spinnerLabelDistributorMaxLabels.setMaximum(Map2ConfigManager.LABEL_DISTRIBUTOR_MAX_LABELS_MAX);
               _spinnerLabelDistributorMaxLabels.setIncrement(10);
               _spinnerLabelDistributorMaxLabels.setPageIncrement(100);
               _spinnerLabelDistributorMaxLabels.addSelectionListener(_markerSelectionListener);
               _spinnerLabelDistributorMaxLabels.addMouseWheelListener(_markerMouseWheelListener10);
               _spinnerLabelDistributorMaxLabels.setToolTipText(
                     "Number of ALL labels which are distributed within the map viewport or a hovered cluster. A large number can slow down performance");

               // label distributor radius
               _spinnerLabelDistributorRadius = new Spinner(container, SWT.BORDER);
               _spinnerLabelDistributorRadius.setMinimum(Map2ConfigManager.LABEL_DISTRIBUTOR_RADIUS_MIN);
               _spinnerLabelDistributorRadius.setMaximum(Map2ConfigManager.LABEL_DISTRIBUTOR_RADIUS_MAX);
               _spinnerLabelDistributorRadius.setIncrement(10);
               _spinnerLabelDistributorRadius.setPageIncrement(100);
               _spinnerLabelDistributorRadius.addSelectionListener(_markerSelectionListener);
               _spinnerLabelDistributorRadius.addMouseWheelListener(_markerMouseWheelListener10);
               _spinnerLabelDistributorRadius.setToolTipText("Radius for the displayed labels around the marker locations");
            }
         }
         {
            /*
             * Label wrapping
             */
            // label
            _lblLabelWrapLength = new Label(tabContainer, SWT.NONE);
            _lblLabelWrapLength.setText("Label wrap &length");
            _lblLabelWrapLength.setToolTipText("");
            gdLabel.applyTo(_lblLabelWrapLength);

            // spinner
            _spinnerLabelWrapLength = new Spinner(tabContainer, SWT.BORDER);
            _spinnerLabelWrapLength.setMinimum(Map2ConfigManager.LABEL_WRAP_LENGTH_MIN);
            _spinnerLabelWrapLength.setMaximum(Map2ConfigManager.LABEL_WRAP_LENGTH_MAX);
            _spinnerLabelWrapLength.setIncrement(1);
            _spinnerLabelWrapLength.setPageIncrement(10);
            _spinnerLabelWrapLength.addSelectionListener(_markerSelectionListener);
            _spinnerLabelWrapLength.addMouseWheelListener(_markerMouseWheelListener10);
         }
         {
            /*
             * Antialias marker text
             */
            _chkIsLabelAntialiased = new Button(tabContainer, SWT.CHECK);
            _chkIsLabelAntialiased.setText("&Antialias labels");
            _chkIsLabelAntialiased.addSelectionListener(_markerSelectionListener);
            gdSpan2.applyTo(_chkIsLabelAntialiased);
         }
         {
            /*
             * Dim map
             */
            final Composite dimContainer = new Composite(tabContainer, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(dimContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(dimContainer);
            {
               // checkbox
               _chkIsDimMap = new Button(dimContainer, SWT.CHECK);
               _chkIsDimMap.setText(Messages.Slideout_Map_Options_Checkbox_DimMap);
               _chkIsDimMap.addSelectionListener(_markerSelectionListener);

               // spinner
               _spinnerMapDimValue = new Spinner(dimContainer, SWT.BORDER);
               _spinnerMapDimValue.setToolTipText(Messages.Slideout_Map_Options_Spinner_DimValue_Tooltip);
               _spinnerMapDimValue.setMinimum(0);
               _spinnerMapDimValue.setMaximum(Map2View.MAX_DIM_STEPS);
               _spinnerMapDimValue.setIncrement(1);
               _spinnerMapDimValue.setPageIncrement(4);
               _spinnerMapDimValue.addSelectionListener(_markerSelectionListener);
               _spinnerMapDimValue.addMouseWheelListener(_markerMouseWheelListener);
               GridDataFactory.fillDefaults().indent(10, 0).applyTo(_spinnerMapDimValue);
            }

            // dimming color
            _colorMapDimColor = new ColorSelectorExtended(tabContainer);
            _colorMapDimColor.setToolTipText(Messages.Slideout_Map_Options_Color_DimColor_Tooltip);
            _colorMapDimColor.addListener(_markerPropertyChangeListener);
            _colorMapDimColor.addOpenListener(this);
         }
         {
            /*
             * Map transparency color
             */
            {
               final Label label = new Label(tabContainer, SWT.NONE);
               label.setText(Messages.Slideout_Map_Options_Label_MapTransparencyColor);
               label.setToolTipText(Messages.Slideout_Map_Options_Label_MapTransparencyColor_Tooltip);
               GridDataFactory.fillDefaults()
                     .align(SWT.BEGINNING, SWT.CENTER)
                     .applyTo(label);

               _colorMapTransparencyColor = new ColorSelectorExtended(tabContainer);
               _colorMapTransparencyColor.setToolTipText(Messages.Slideout_Map_Options_Label_MapTransparencyColor_Tooltip);
               _colorMapTransparencyColor.addListener(_markerPropertyChangeListener);
               _colorMapTransparencyColor.addOpenListener(this);
            }
            {
               /*
                * Use map dim color
                */
               _chkUseMapDimColor = new Button(tabContainer, SWT.CHECK);
               _chkUseMapDimColor.setText(Messages.Slideout_Map_Options_Checkbox_UseMapDimColor);
               _chkUseMapDimColor.addSelectionListener(_markerSelectionListener);
               gdSpan2.indent(16, 0).applyTo(_chkUseMapDimColor);
            }
         }
      }

      return tabContainer;
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

      final boolean isGroupDuplicatedMarkers = _chkIsGroupDuplicatedMarkers   .getSelection();
      final boolean isMarkerClustered        = _chkIsMarkerClustered          .getSelection();
      final boolean isShowTourMarker         = _chkIsShowTourMarker           .getSelection();
      final boolean isShowTourLocations      = _chkIsShowTourLocations        .getSelection();

      final boolean isDimMap                 = _chkIsDimMap                   .getSelection();
      final boolean isUseMapDimColor         = _chkUseMapDimColor             .getSelection();
      final boolean isUseTransparencyColor   = isUseMapDimColor == false;

      final boolean isGroupMarkers           = isShowTourMarker && isGroupDuplicatedMarkers;
      final boolean isShowClusteredMarker    = isShowTourMarker && isMarkerClustered;
      final boolean isShowLabels             = isShowTourMarker || isShowTourLocations;

      _colorMapTransparencyColor.setEnabled(isDimMap == false || isUseTransparencyColor);

      // label options
      _chkIsLabelAntialiased              .setEnabled(isShowLabels);
      _comboLabelLayout                   .setEnabled(isShowLabels);
      _lblLabelBackgrouond                .setEnabled(isShowLabels);
      _lblLabelWrapLength                 .setEnabled(isShowLabels);
      _lblVisibleLabels                   .setEnabled(isShowLabels);
      _spinnerLabelDistributorMaxLabels   .setEnabled(isShowLabels);
      _spinnerLabelDistributorRadius      .setEnabled(isShowLabels);

      _btnSwapClusterSymbolColor          .setEnabled(isShowClusteredMarker);
      _btnSwapMarkerLabel_Color           .setEnabled(isShowTourMarker);

      _chkIsClusterSymbolAntialiased      .setEnabled(isShowClusteredMarker);
      _chkIsClusterTextAntialiased        .setEnabled(isShowClusteredMarker);
      _chkIsFillClusterSymbol             .setEnabled(isShowClusteredMarker);

      _chkIsGroupDuplicatedMarkers        .setEnabled(isShowTourMarker);
      _chkIsMarkerClustered               .setEnabled(isShowTourMarker);

      _lblClusterGrid_Size                .setEnabled(isShowClusteredMarker);
      _lblClusterSymbol                   .setEnabled(isShowClusteredMarker);
      _lblClusterSymbol_Size              .setEnabled(isShowClusteredMarker);

      _lblLocationLabel_Color             .setEnabled(isShowTourMarker);
      _lblMarkerLabel_Color               .setEnabled(isShowTourMarker);
      _lblMarkerLabel_HoveredColor        .setEnabled(isShowTourMarker);

      _spinnerClusterGrid_Size            .setEnabled(isShowClusteredMarker);
      _spinnerClusterSymbol_Size          .setEnabled(isShowClusteredMarker);
      _spinnerClusterOutline_Width        .setEnabled(isShowClusteredMarker);

      _spinnerLabelWrapLength             .setEnabled(isShowTourMarker);

      _colorClusterSymbol_Fill            .setEnabled(isShowClusteredMarker);
      _colorClusterSymbol_Outline         .setEnabled(isShowClusteredMarker);
      _colorMarkerLabel_Fill              .setEnabled(isShowTourMarker);
      _colorMarkerLabel_Fill_Hovered      .setEnabled(isShowTourMarker);
      _colorMarkerLabel_Outline           .setEnabled(isShowTourMarker);
      _colorMarkerLabel_Outline_Hovered   .setEnabled(isShowTourMarker);

      // tour location
      _btnSwapLocationLabel_Color         .setEnabled(isShowTourLocations);
      _btnSwapLocationLabel_Hovered_Color .setEnabled(isShowTourLocations);
      _chkIsShowMapLocations_BoundingBox  .setEnabled(isShowTourLocations);
      _lblLocationLabel_Color             .setEnabled(isShowTourLocations);
      _lblLocationLabel_HoveredColor      .setEnabled(isShowTourLocations);
      _colorLocationLabel_Fill            .setEnabled(isShowTourLocations);
      _colorLocationLabel_Fill_Hovered    .setEnabled(isShowTourLocations);
      _colorLocationLabel_Outline         .setEnabled(isShowTourLocations);
      _colorLocationLabel_Outline_Hovered .setEnabled(isShowTourLocations);

      // groups
      _lblGroupDuplicatedMarkers          .setEnabled(isGroupMarkers);
      _lblLabelGroupGridSize              .setEnabled(isGroupMarkers);
      _spinnerLabelGroupGridSize          .setEnabled(isGroupMarkers);
      _txtGroupDuplicatedMarkers          .setEnabled(isGroupMarkers);

      _chkUseMapDimColor                  .setEnabled(isDimMap);
      _colorMapDimColor                   .setEnabled(isDimMap);
      _spinnerMapDimValue                 .setEnabled(isDimMap);

      /*
       * Common locations
       */
      final List<TourLocation> allSelectedLocations = getSelectedLocations();

      final boolean isShowCommonLocations       = _chkIsShowCommonLocations.getSelection();
      final boolean isCommonLocationSelected    = isShowCommonLocations && allSelectedLocations.size() > 0;

      _btnDeleteCommonLocation            .setEnabled(isCommonLocationSelected);
      _mapCommonLocationViewer.getTable() .setEnabled(isShowCommonLocations);

// SET_FORMATTING_ON

      updateUI_TabLabel();
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(_actionDeleteLocation);

      enableControls();
   }

   private void fillUI() {

      for (final String label : _allMarkerLabelLayout_Label) {
         _comboLabelLayout.add(label);
      }
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

   private MapLabelLayout getSelectedMarkerLabelLayout() {

      final int selectedIndex = _comboLabelLayout.getSelectionIndex();

      if (selectedIndex >= 0) {
         return _allMarkerLabelLayout_Value[selectedIndex];
      } else {
         return Map2ConfigManager.LABEL_LAYOUT_DEFAULT;
      }
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

      _imageMapLocation_Common = TourbookPlugin.getImageDescriptor(Images.MapLocation).createImage();
      _imageMapLocation_Tour = TourbookPlugin.getImageDescriptor(Images.MapLocation_Start).createImage();
      _imageTourMarker = TourbookPlugin.getImageDescriptor(Images.TourMarker).createImage();
      _imageTourMarkerGroups = TourbookPlugin.getImageDescriptor(Images.TourMarker_Group).createImage();

      // force spinner controls to have the same width
      _spinnerGridData = GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(3), SWT.DEFAULT);

//      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI());

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

      _markerMouseWheelListener10 = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
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

      UI.disposeResource(_imageMapLocation_Common);
      UI.disposeResource(_imageMapLocation_Tour);
      UI.disposeResource(_imageTourMarker);
      UI.disposeResource(_imageTourMarkerGroups);

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

   private void onSwapLocationColor() {

      final Map2MarkerConfig config = Map2ConfigManager.getActiveMarkerConfig();

      final RGB fgColor = config.locationOutline_RGB;
      final RGB bgColor = config.locationFill_RGB;

      config.locationOutline_RGB = bgColor;
      config.locationFill_RGB = fgColor;

      config.setupColors();

      restoreState();
      repaintMap();
   }

   private void onSwapLocationHoveredColor() {

      final Map2MarkerConfig config = Map2ConfigManager.getActiveMarkerConfig();

      final RGB fgColor = config.locationOutline_Hovered_RGB;
      final RGB bgColor = config.locationFill_Hovered_RGB;

      config.locationOutline_Hovered_RGB = bgColor;
      config.locationFill_Hovered_RGB = fgColor;

      config.setupColors();

      restoreState();
      repaintMap();
   }

   private void onSwapMarkerColor() {

      final Map2MarkerConfig config = Map2ConfigManager.getActiveMarkerConfig();

      final RGB fgColor = config.markerOutline_RGB;
      final RGB bgColor = config.markerFill_RGB;

      config.markerOutline_RGB = bgColor;
      config.markerFill_RGB = fgColor;

      config.setupColors();

      restoreState();
      repaintMap();
   }

   private void onSwapMarkerHoveredColor() {

      final Map2MarkerConfig config = Map2ConfigManager.getActiveMarkerConfig();

      final RGB fgColor = config.markerOutline_Hovered_RGB;
      final RGB bgColor = config.markerFill_Hovered_RGB;

      config.markerOutline_Hovered_RGB = bgColor;
      config.markerFill_Hovered_RGB = fgColor;

      config.setupColors();

      restoreState();
      repaintMap();
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

      final Map2 map2 = _map2View.getMap();

      map2.resetMarkerCluster();
      map2.paint();
   }

   private void restoreState() {

      _chkIsShowMapLocations_BoundingBox.setSelection(Util.getStateBoolean(_state_Map2,
            Map2View.STATE_IS_SHOW_LOCATION_BOUNDING_BOX,
            Map2View.STATE_IS_SHOW_LOCATION_BOUNDING_BOX_DEFAULT));

      /*
       * Tour marker
       */
      final Map2MarkerConfig config = Map2ConfigManager.getActiveMarkerConfig();

// SET_FORMATTING_OFF

      _chkIsShowCommonLocations           .setSelection( config.isShowCommonLocation);
      _chkIsShowTourLocations             .setSelection( config.isShowTourLocation);
      _chkIsShowTourMarker                .setSelection( config.isShowTourMarker);

      _chkIsClusterSymbolAntialiased      .setSelection( config.isClusterSymbolAntialiased);
      _chkIsClusterTextAntialiased        .setSelection( config.isClusterTextAntialiased);
      _chkIsFillClusterSymbol             .setSelection( config.isFillClusterSymbol);
      _chkIsGroupDuplicatedMarkers        .setSelection( config.isGroupDuplicatedMarkers);
      _chkIsMarkerClustered               .setSelection( config.isMarkerClustered);
      _chkIsLabelAntialiased              .setSelection( config.isLabelAntialiased);

      _spinnerClusterGrid_Size            .setSelection( config.clusterGridSize);
      _spinnerClusterOutline_Width        .setSelection( config.clusterOutline_Width);
      _spinnerClusterSymbol_Size          .setSelection( config.clusterSymbol_Size);
      _spinnerLabelDistributorMaxLabels   .setSelection( config.labelDistributorMaxLabels);
      _spinnerLabelDistributorRadius      .setSelection( config.labelDistributorRadius);
      _spinnerLabelGroupGridSize          .setSelection( config.groupGridSize);
      _spinnerLabelWrapLength             .setSelection( config.labelWrapLength);

      _colorClusterSymbol_Fill            .setColorValue(config.clusterFill_RGB);
      _colorClusterSymbol_Outline         .setColorValue(config.clusterOutline_RGB);
      _colorLocationLabel_Fill            .setColorValue(config.locationFill_RGB);
      _colorLocationLabel_Fill_Hovered    .setColorValue(config.locationFill_Hovered_RGB);
      _colorLocationLabel_Outline         .setColorValue(config.locationOutline_RGB);
      _colorLocationLabel_Outline_Hovered .setColorValue(config.locationOutline_Hovered_RGB);
      _colorMarkerLabel_Fill              .setColorValue(config.markerFill_RGB);
      _colorMarkerLabel_Fill_Hovered      .setColorValue(config.markerFill_Hovered_RGB);
      _colorMarkerLabel_Outline           .setColorValue(config.markerOutline_RGB);
      _colorMarkerLabel_Outline_Hovered   .setColorValue(config.markerOutline_Hovered_RGB);

      _txtGroupDuplicatedMarkers          .setText(      config.groupedMarkers);

      /*
       * Map dimming & transparency
       */
      _chkIsDimMap               .setSelection(    Util.getStateBoolean(_state_Map2,  Map2View.STATE_IS_MAP_DIMMED,                       Map2View.STATE_IS_MAP_DIMMED_DEFAULT));
      _chkUseMapDimColor         .setSelection(    Util.getStateBoolean(_state_Map2,  Map2View.STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR,  Map2View.STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR_DEFAULT));
      _colorMapDimColor          .setColorValue(   Util.getStateRGB(    _state_Map2,  Map2View.STATE_DIM_MAP_COLOR,                       Map2View.STATE_DIM_MAP_COLOR_DEFAULT));
      _colorMapTransparencyColor .setColorValue(   Util.getStateRGB(    _state_Map2,  Map2View.STATE_MAP_TRANSPARENCY_COLOR,              Map2View.STATE_MAP_TRANSPARENCY_COLOR_DEFAULT));
      _spinnerMapDimValue        .setSelection(    Util.getStateInt(    _state_Map2,  Map2View.STATE_DIM_MAP_VALUE,                       Map2View.STATE_DIM_MAP_VALUE_DEFAULT));

// SET_FORMATTING_ON

      selectMarkerLabelLayout(config.markerLabelLayout);

      updateUI_TabLabel();
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

      config.isShowCommonLocation         = _chkIsShowCommonLocations            .getSelection();
      config.isShowTourLocation           = _chkIsShowTourLocations              .getSelection();
      config.isShowTourMarker             = _chkIsShowTourMarker                 .getSelection();

      config.isGroupDuplicatedMarkers     = _chkIsGroupDuplicatedMarkers         .getSelection();
      config.groupedMarkers               = _txtGroupDuplicatedMarkers           .getText();

      config.isClusterSymbolAntialiased   = _chkIsClusterSymbolAntialiased       .getSelection();
      config.isClusterTextAntialiased     = _chkIsClusterTextAntialiased         .getSelection();
      config.isFillClusterSymbol          = _chkIsFillClusterSymbol              .getSelection();

      config.isMarkerClustered            = _chkIsMarkerClustered                .getSelection();
      config.isLabelAntialiased           = _chkIsLabelAntialiased               .getSelection();

      config.clusterGridSize              = _spinnerClusterGrid_Size             .getSelection();
      config.clusterOutline_Width         = _spinnerClusterOutline_Width         .getSelection();
      config.clusterSymbol_Size           = _spinnerClusterSymbol_Size           .getSelection();
      config.clusterFill_RGB              = _colorClusterSymbol_Fill             .getColorValue();
      config.clusterOutline_RGB           = _colorClusterSymbol_Outline          .getColorValue();

      config.labelDistributorMaxLabels    = _spinnerLabelDistributorMaxLabels    .getSelection();
      config.labelDistributorRadius       = _spinnerLabelDistributorRadius       .getSelection();
      config.groupGridSize                = _spinnerLabelGroupGridSize           .getSelection();
      config.labelWrapLength              = _spinnerLabelWrapLength              .getSelection();

      config.locationFill_RGB             = _colorLocationLabel_Fill             .getColorValue();
      config.locationFill_Hovered_RGB     = _colorLocationLabel_Fill_Hovered     .getColorValue();
      config.locationOutline_RGB          = _colorLocationLabel_Outline          .getColorValue();
      config.locationOutline_Hovered_RGB  = _colorLocationLabel_Outline_Hovered  .getColorValue();

      config.markerFill_RGB               = _colorMarkerLabel_Fill               .getColorValue();
      config.markerFill_Hovered_RGB       = _colorMarkerLabel_Fill_Hovered       .getColorValue();
      config.markerOutline_RGB            = _colorMarkerLabel_Outline            .getColorValue();
      config.markerOutline_Hovered_RGB    = _colorMarkerLabel_Outline_Hovered    .getColorValue();

      config.markerLabelLayout            = getSelectedMarkerLabelLayout();

      config.setupColors();

      /*
       * Map dimming & transparency
       */
      _state_Map2.put(Map2View.STATE_IS_MAP_DIMMED,                     _chkIsDimMap               .getSelection());
      _state_Map2.put(Map2View.STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR,_chkUseMapDimColor         .getSelection());

      _state_Map2.put(Map2View.STATE_DIM_MAP_VALUE,                     _spinnerMapDimValue        .getSelection());

      Util.setState(_state_Map2, Map2View.STATE_DIM_MAP_COLOR,          _colorMapDimColor          .getColorValue());
      Util.setState(_state_Map2, Map2View.STATE_MAP_TRANSPARENCY_COLOR, _colorMapTransparencyColor .getColorValue());

      _map2View.setupMapDimLevel();

// SET_FORMATTING_ON
   }

   @Override
   protected void saveState() {

      _columnManager.saveState(_state_Slideout);

      _state_Slideout.put(STATE_SELECTED_TAB, _tabFolder.getSelectionIndex());

      _state_Slideout.put(STATE_SORT_COLUMN_ID, _mapLocationComparator.__sortColumnId);
      _state_Slideout.put(STATE_SORT_COLUMN_DIRECTION, _mapLocationComparator.__sortDirection);

      super.saveState();
   }

   private void selectMarkerLabelLayout(final Enum<MapLabelLayout> filterOperator) {

      int selectionIndex = 0;

      for (int operatorIndex = 0; operatorIndex < _allMarkerLabelLayout_Value.length; operatorIndex++) {

         final MapLabelLayout tourFilterFieldOperator = _allMarkerLabelLayout_Value[operatorIndex];

         if (tourFilterFieldOperator.equals(filterOperator)) {
            selectionIndex = operatorIndex;
            break;
         }
      }

      _comboLabelLayout.select(selectionIndex);
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

   /**
    * Show a flag in the tab label when content is enabled
    */
   private void updateUI_TabLabel() {

// SET_FORMATTING_OFF

      final boolean isGroupDuplicatedMarkers    = _chkIsGroupDuplicatedMarkers.getSelection();
      final boolean isShowCommonLocations       = _chkIsShowCommonLocations.getSelection();
      final boolean isShowTourLocations         = _chkIsShowTourLocations.getSelection();
      final boolean isShowTourMarker            = _chkIsShowTourMarker.getSelection();

      _tabCommonLocations  .setText(isShowCommonLocations   ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourLocations    .setText(isShowTourLocations     ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourMarkers      .setText(isShowTourMarker        ? UI.SYMBOL_STAR : UI.EMPTY_STRING);
      _tabTourMarkerGroups .setText(isShowTourMarker && isGroupDuplicatedMarkers ? UI.SYMBOL_STAR : UI.EMPTY_STRING);

// SET_FORMATTING_ON
   }

   private void updateUI_Viewer() {

      _mapCommonLocationViewer.setInput(new Object[0]);
   }

}
