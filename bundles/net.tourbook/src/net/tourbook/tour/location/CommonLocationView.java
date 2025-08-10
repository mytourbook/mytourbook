/*******************************************************************************
 * Copyright (C) 2024, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.location;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnDefinitionFor1stVisibleAlignmentColumn;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourLocation;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TableColumnFactory;

import org.eclipse.e4.ui.di.PersistState;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.ViewPart;

public class CommonLocationView extends ViewPart implements ITourViewer {

   public static final String            ID                              = "net.tourbook.tour.location.CommonLocationView"; //$NON-NLS-1$
   //
   private static final String           COLUMN_CREATED_DATE_TIME        = "createdDateTime";                               //$NON-NLS-1$
   private static final String           COLUMN_LOCATION_NAME            = "locationName";                                  //$NON-NLS-1$
   private static final String           COLUMN_SEQUENCE                 = "sequence";                                      //$NON-NLS-1$
   private static final String           COLUMN_ZOOM_LEVEL               = "zoomLevel";                                     //$NON-NLS-1$
   //
   private static final String           STATE_SORT_COLUMN_DIRECTION     = "STATE_SORT_COLUMN_DIRECTION";                   //$NON-NLS-1$
   private static final String           STATE_SORT_COLUMN_ID            = "STATE_SORT_COLUMN_ID";                          //$NON-NLS-1$
   //
   private static final IPreferenceStore _prefStore                      = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                          = TourbookPlugin.getState(ID);
   //
   private IPropertyChangeListener       _prefChangeListener;
   //
   private TableViewer                   _mapCommonLocationViewer;
   private MapLocationComparator         _mapLocationComparator          = new MapLocationComparator();
   private ColumnManager                 _columnManager;
   private SelectionAdapter              _columnSortListener;
   //
   private MenuManager                   _viewerMenuManager;
   private IContextMenuProvider          _tableViewerContextMenuProvider = new TableContextMenuProvider();

   private List<TourLocation>            _allMapLocations                = CommonLocationManager.getCommonLocations();

   private TourLocationToolTip           _locationTooltip;

   private ActionDeleteLocation          _actionDeleteLocation;

   private final NumberFormat            _nf3                            = NumberFormat.getNumberInstance();
   {
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);
   }
   //
   private PixelConverter _pc;

   /*
    * UI controls
    */
   private Composite _viewerContainer;

   private Menu      _tableContextMenu;

   private class ActionDeleteLocation extends Action {

      public ActionDeleteLocation() {

         setText(Messages.Tour_Location_Action_DeleteCommonLocation);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
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
         return rc > 0
               ? 1
               : rc < 0
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

         _tableContextMenu = createUI_725_CommonLocation_CreateViewerContextMenu();

         return _tableContextMenu;
      }
   }

   /**
    * This class is used to show a tooltip only when this cell is hovered
    */
   public abstract class TooltipLabelProvider extends CellLabelProvider {}

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

   private void createActions() {

      _actionDeleteLocation = new ActionDeleteLocation();
   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager();
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(menuManager -> fillContextMenu(menuManager));
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);
      createMenuManager();

      restoreState_BeforeUI();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      createUI(parent);

      createActions();

      addPrefListener();

      // load viewer
      updateUI_Viewer();
   }

   private void createUI(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(_viewerContainer);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_722_CommonLocation_Table(_viewerContainer);
      }
   }

   private void createUI_722_CommonLocation_Table(final Composite parent) {

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

      _mapCommonLocationViewer.setUseHashlookup(true);
      _mapCommonLocationViewer.setContentProvider(new MapLocationContentProvider());
      _mapCommonLocationViewer.setComparator(_mapLocationComparator);

      _mapCommonLocationViewer.addSelectionChangedListener(selectionChangedEvent -> onLocation_Select(selectionChangedEvent));

      updateUI_SetSortDirection(
            _mapLocationComparator.__sortColumnId,
            _mapLocationComparator.__sortDirection);

      // set info tooltip provider
      _locationTooltip = new TourLocationToolTip(this);

      // ensure that tooltips are hidden
      table.addListener(SWT.MouseExit, (event) -> hideTooltip());

      createUI_724_CommonLocation_ContextMenu();
   }

   /**
    * Ceate the view context menus
    */
   private void createUI_724_CommonLocation_ContextMenu() {

      _tableContextMenu = createUI_725_CommonLocation_CreateViewerContextMenu();

      _columnManager.createHeaderContextMenu(

            (Table) _mapCommonLocationViewer.getControl(),
            _tableViewerContextMenuProvider);
   }

   private Menu createUI_725_CommonLocation_CreateViewerContextMenu() {

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
    * Column: Name
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

            final TourLocation tourLocation = (TourLocation) cell.getElement();

            if (UI.IS_SCRAMBLE_DATA) {

               cell.setText(UI.scrambleText(tourLocation.getMapName()));

            } else {

               cell.setText(tourLocation.getMapName());
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

   @Override
   public void dispose() {

      if (_prefChangeListener != null) {

         _prefStore.removePropertyChangeListener(_prefChangeListener);
      }

      super.dispose();
   }

   private void enableControls() {

      final int numSelectedLocations = getSelectedLocations().size();

      _actionDeleteLocation.setEnabled(numSelectedLocations > 0);
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

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSelectSortColumn(e);
         }
      };
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

      enableControls();

      // fire selection
      final IStructuredSelection selection = _mapCommonLocationViewer.getStructuredSelection();

      TourManager.fireEventWithCustomData(
            TourEventId.COMMON_LOCATION_SELECTION,
            selection.toList(),
            null);
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

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         _mapCommonLocationViewer.getTable().dispose();

         createUI_722_CommonLocation_Table(_viewerContainer);
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

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, TableColumnFactory.SENSOR_NAME_ID);
      final int sortDirection = Util.getStateInt(_state, STATE_SORT_COLUMN_DIRECTION, MapLocationComparator.ASCENDING);

      // update comparator
      _mapLocationComparator.__sortColumnId = sortColumnId;
      _mapLocationComparator.__sortDirection = sortDirection;
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

      _state.put(STATE_SORT_COLUMN_ID, _mapLocationComparator.__sortColumnId);
      _state.put(STATE_SORT_COLUMN_DIRECTION, _mapLocationComparator.__sortDirection);
   }

   @Override
   public void setFocus() {

      final Table table = _mapCommonLocationViewer.getTable();

      if (table.isDisposed()) {
         return;
      }

      table.setFocus();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * Reload model and select tour location
    *
    * @param tourLocation
    */
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

      _mapCommonLocationViewer.setSelection(selection, true);
      _mapCommonLocationViewer.getTable().showSelection();
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

   private void updateUI_Viewer() {

      _mapCommonLocationViewer.setInput(new Object[0]);
   }
}
