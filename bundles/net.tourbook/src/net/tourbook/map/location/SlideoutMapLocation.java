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
package net.tourbook.map.location;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
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
import net.tourbook.map2.view.Map2View;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.location.AddressLocationManager;
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
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Map location slideout
 */
public class SlideoutMapLocation extends AdvancedSlideout implements ITourViewer2 {

   private static final String     COLUMN_CREATED_DATE_TIME        = "createdDateTime";                           //$NON-NLS-1$
   private static final String     COLUMN_LOCATION_NAME            = "LocationName";                              //$NON-NLS-1$
   private static final String     COLUMN_SEQUENCE                 = "sequence";                                  //$NON-NLS-1$
   private static final String     COLUMN_ZOOM_LEVEL               = "zoomLevel";                                 //$NON-NLS-1$

   private static final String     STATE_SORT_COLUMN_DIRECTION     = "STATE_SORT_COLUMN_DIRECTION";               //$NON-NLS-1$
   private static final String     STATE_SORT_COLUMN_ID            = "STATE_SORT_COLUMN_ID";                      //$NON-NLS-1$

   private final IPreferenceStore  _prefStore                      = TourbookPlugin.getPrefStore();
   private IDialogSettings         _state_Map2;
   private IDialogSettings         _state_Slideout;

   private Map2View                _map2View;
   private ToolItem                _toolItem;

   private TableViewer             _mapLocationViewer;
   private MapLocationComparator   _mapLocationComparator          = new MapLocationComparator();
   private ColumnManager           _columnManager;
   private SelectionAdapter        _columnSortListener;

   private SelectionListener       _defaultSelectionListener;
   private IPropertyChangeListener _prefChangeListener;

   private MenuManager             _viewerMenuManager;
   private IContextMenuProvider    _tableViewerContextMenuProvider = new TableContextMenuProvider();
   private ActionDeleteLocation    _actionDeleteLocation;

   private List<TourLocation>      _allMapLocations                = AddressLocationManager.getAddressLocations();

   private TourLocationToolTip     _locationTooltip;

   private PixelConverter          _pc;

   private final NumberFormat      _nf3                            = NumberFormat.getNumberInstance();
   {
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);
   }

   /*
    * UI controls
    */
   private Composite _viewerContainer;

   private Button    _btnDelete;

   private Button    _chkIsShowMapLocations_BoundingBox;
   private Button    _chkIsShowTourLocations;
   private Button    _chkIsShowAddressLocations;

   private Menu      _tableContextMenu;

   private class ActionDeleteLocation extends Action {

      public ActionDeleteLocation() {

         setText(Messages.Tour_Location_Action_DeleteAddressLocation);

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

         _tableContextMenu = createUI_85_CreateViewerContextMenu();

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
   public SlideoutMapLocation(final ToolItem toolItem,
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

            _mapLocationViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
            _mapLocationViewer.refresh();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
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
      enableControls();

      AddressLocationManager.setMapLocationSlideout(this);
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
         createUI_10_Options(shellContainer);
         createUI_80_LocationViewer(shellContainer);
         createUI_90_Actions(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
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
         {
            /*
             * Show address locations
             */
            _chkIsShowAddressLocations = new Button(container, SWT.CHECK);
            _chkIsShowAddressLocations.setText(Messages.Slideout_MapLocation_Checkbox_ShowAddressLocations);
            _chkIsShowAddressLocations.setToolTipText(Messages.Slideout_MapLocation_Checkbox_ShowAddressLocations_Tooltip);
            _chkIsShowAddressLocations.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowAddressLocations);
         }
      }

      // reorder tabbing
      container.setTabList(new Control[] {

            _chkIsShowAddressLocations,
            _chkIsShowTourLocations,

            _chkIsShowMapLocations_BoundingBox,
      });
   }

   private void createUI_80_LocationViewer(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_MapLocation_Label_AddressLocations);
         }
         {
            _viewerContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
            GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
            {
               createUI_82_Table(_viewerContainer);
            }
         }

//         createUI_680_ViewerActions(container);
      }
   }

   private void createUI_82_Table(final Composite parent) {

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
      _mapLocationViewer = new TableViewer(table);

      _columnManager.createColumns(_mapLocationViewer);
      _columnManager.setSlideoutShell(this);

      _mapLocationViewer.setUseHashlookup(true);
      _mapLocationViewer.setContentProvider(new MapLocationContentProvider());
      _mapLocationViewer.setComparator(_mapLocationComparator);

      _mapLocationViewer.addSelectionChangedListener(selectionChangedEvent -> onLocation_Select(selectionChangedEvent));
//    _mapLocationViewer.addDoubleClickListener(doubleClickEvent -> onGeoFilter_ToggleReadEditMode());

      updateUI_SetSortDirection(
            _mapLocationComparator.__sortColumnId,
            _mapLocationComparator.__sortDirection);

      // set info tooltip provider
      _locationTooltip = new TourLocationToolTip(this);

      // ensure that tooltips are hidden
      table.addListener(SWT.MouseExit, (event) -> hideTooltip());

      createUI_84_ContextMenu();
   }

   /**
    * Ceate the view context menus
    */
   private void createUI_84_ContextMenu() {

      _tableContextMenu = createUI_85_CreateViewerContextMenu();

      _columnManager.createHeaderContextMenu(

            (Table) _mapLocationViewer.getControl(),
            _tableViewerContextMenuProvider);
   }

   private Menu createUI_85_CreateViewerContextMenu() {

      final Table table = (Table) _mapLocationViewer.getControl();
      final Menu tableContextMenu = _viewerMenuManager.createContextMenu(table);

      return tableContextMenu;
   }

   private void createUI_90_Actions(final Composite parent) {

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
            _btnDelete = new Button(container, SWT.PUSH);
            _btnDelete.setText(Messages.App_Action_Delete);
            _btnDelete.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onLocation_Delete()));

            // set button default width
            UI.setButtonLayoutData(_btnDelete);
         }
      }
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

            final int indexOf = _mapLocationViewer.getTable().indexOf((TableItem) cell.getItem());

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

      final List<TourLocation> allSelectedLocations = getSelectedLocations();

      final boolean isShowAddressLocations = _chkIsShowAddressLocations.getSelection();
      final boolean isShowTourLocations = _chkIsShowTourLocations.getSelection();
      final boolean isAddressLocationSelected = isShowAddressLocations && allSelectedLocations.size() > 0;

      _btnDelete.setEnabled(isAddressLocationSelected);

      _chkIsShowMapLocations_BoundingBox.setEnabled(isShowAddressLocations || isShowTourLocations);
      _mapLocationViewer.getTable().setEnabled(isShowAddressLocations);
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

      return _mapLocationViewer;
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
      final List<TourLocation> allSelectedLocations = _mapLocationViewer.getStructuredSelection().toList();

      return allSelectedLocations;
   }

   /**
    * @param sortColumnId
    *
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _mapLocationViewer.getTable().getColumns();

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
      return _mapLocationViewer;
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

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSelectSortColumn(e);
         }
      };
   }

   @Override
   public boolean isColumn0Visible(final ColumnViewer columnViewer) {

      final TableColumn[] allColumns = _mapLocationViewer.getTable().getColumns();

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

      _state_Map2.put(Map2View.STATE_IS_SHOW_LOCATIONS_ADDRESS, _chkIsShowAddressLocations.getSelection());
      _state_Map2.put(Map2View.STATE_IS_SHOW_LOCATIONS_TOUR, _chkIsShowTourLocations.getSelection());

      _state_Map2.put(Map2View.STATE_IS_SHOW_MAP_LOCATION_BOUNDING_BOX, _chkIsShowMapLocations_BoundingBox.getSelection());

      _map2View.updateState_Map2_Options();

      enableControls();
   }

   @Override
   protected void onDispose() {

      AddressLocationManager.setMapLocationSlideout(null);

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
      if (AddressLocationManager.deleteLocations(allSelectedLocations) == false) {
         return;
      }

      /*
       * Deletion was performed -> update viewer
       */

      final Table table = _mapLocationViewer.getTable();

      // get index for selected location
      final int lastLocationIndex = table.getSelectionIndex();

      // reload viewer
      reloadViewer();

      // get next location
      TourLocation nextLocationItem = (TourLocation) _mapLocationViewer.getElementAt(lastLocationIndex);

      if (nextLocationItem == null) {
         nextLocationItem = (TourLocation) _mapLocationViewer.getElementAt(lastLocationIndex - 1);
      }

      // select next location
      if (nextLocationItem != null) {
         _mapLocationViewer.setSelection(new StructuredSelection(nextLocationItem), true);
      }

      table.setFocus();

      TourManager.fireEventWithCustomData(
            TourEventId.ADDRESS_LOCATION_SELECTION,
            null,
            null);
   }

   private void onLocation_Select(final SelectionChangedEvent selectionChangedEvent) {

      final IStructuredSelection selection = _mapLocationViewer.getStructuredSelection();

      if (selection.isEmpty()) {
         return;
      }

      enableControls();

      // fire selection
      TourManager.fireEventWithCustomData(
            TourEventId.ADDRESS_LOCATION_SELECTION,
            selection.toList(),
            null);
   }

   private void onSelectSortColumn(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
//         final ISelection selectionBackup = getViewerSelection();
         final ISelection selectionBackup = _mapLocationViewer.getStructuredSelection();
         {
            // update viewer with new sorting
            _mapLocationComparator.setSortColumn(e.widget);
            _mapLocationViewer.refresh();
         }
         updateUI_SelectMapLocationItem(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         _mapLocationViewer.getTable().dispose();

         createUI_82_Table(_viewerContainer);
         _viewerContainer.layout();

         // update the viewer
         reloadViewer();
      }
      _viewerContainer.setRedraw(true);

      return _mapLocationViewer;
   }

   @Override
   public void reloadViewer() {

      updateUI_Viewer();
   }

   private void restoreState() {

      _chkIsShowAddressLocations.setSelection(Util.getStateBoolean(_state_Map2,
            Map2View.STATE_IS_SHOW_LOCATIONS_ADDRESS,
            Map2View.STATE_IS_SHOW_LOCATIONS_ADDRESS_DEFAULT));

      _chkIsShowTourLocations.setSelection(Util.getStateBoolean(_state_Map2,
            Map2View.STATE_IS_SHOW_LOCATIONS_TOUR,
            Map2View.STATE_IS_SHOW_LOCATIONS_TOUR_DEFAULT));

      _chkIsShowMapLocations_BoundingBox.setSelection(Util.getStateBoolean(_state_Map2,
            Map2View.STATE_IS_SHOW_MAP_LOCATION_BOUNDING_BOX,
            Map2View.STATE_IS_SHOW_MAP_LOCATION_BOUNDING_BOX_DEFAULT));

   }

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state_Slideout, STATE_SORT_COLUMN_ID, TableColumnFactory.SENSOR_NAME_ID);
      final int sortDirection = Util.getStateInt(_state_Slideout, STATE_SORT_COLUMN_DIRECTION, MapLocationComparator.ASCENDING);

      // update comparator
      _mapLocationComparator.__sortColumnId = sortColumnId;
      _mapLocationComparator.__sortDirection = sortDirection;
   }

   @Override
   protected void saveState() {

      _columnManager.saveState(_state_Slideout);

      _state_Slideout.put(STATE_SORT_COLUMN_ID, _mapLocationComparator.__sortColumnId);
      _state_Slideout.put(STATE_SORT_COLUMN_DIRECTION, _mapLocationComparator.__sortDirection);

      super.saveState();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   public void updateUI(final TourLocation tourLocation) {

      _mapLocationViewer.refresh();

      _mapLocationViewer.setSelection(new StructuredSelection(tourLocation), true);
      _mapLocationViewer.getTable().showSelection();
   }

   /**
    * Select and reveal a compare item item.
    *
    * @param selection
    */
   private void updateUI_SelectMapLocationItem(final ISelection selection) {

//      _isInUpdate = true;
      {
         _mapLocationViewer.setSelection(selection, true);
         _mapLocationViewer.getTable().showSelection();

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

      final Table table = _mapLocationViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(sortDirection == MapLocationComparator.ASCENDING ? SWT.UP : SWT.DOWN);
   }

   private void updateUI_Viewer() {

      _mapLocationViewer.setInput(new Object[0]);
   }

}
