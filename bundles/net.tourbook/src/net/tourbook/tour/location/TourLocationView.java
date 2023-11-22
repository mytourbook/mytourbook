/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourLocation;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TableColumnFactory;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class TourLocationView extends ViewPart implements ITourViewer {

   public static final String      ID                              = "net.tourbook.tour.location.TourLocationView"; //$NON-NLS-1$

   private static final char       NL                              = UI.NEW_LINE;

   private static final String     STATE_SELECTED_SENSOR_INDICES   = "STATE_SELECTED_SENSOR_INDICES";               //$NON-NLS-1$
   private static final String     STATE_SORT_COLUMN_DIRECTION     = "STATE_SORT_COLUMN_DIRECTION";                 //$NON-NLS-1$
   private static final String     STATE_SORT_COLUMN_ID            = "STATE_SORT_COLUMN_ID";                        //$NON-NLS-1$

   private final IPreferenceStore  _prefStore                      = TourbookPlugin.getPrefStore();
   private final IPreferenceStore  _prefStore_Common               = CommonActivator.getPrefStore();
   private final IDialogSettings   _state                          = TourbookPlugin.getState(ID);

   private IPartListener2          _partListener;
   private IPropertyChangeListener _prefChangeListener;
   private IPropertyChangeListener _prefChangeListener_Common;
   private ITourEventListener      _tourPropertyListener;

   private TableViewer             _locationViewer;
   private LocationComparator      _locationComparator             = new LocationComparator();
   private ColumnManager           _columnManager;
   private SelectionAdapter        _columnSortListener;

   private List<LocationItem>      _allLocationItems               = new ArrayList<>();

   private MenuManager             _viewerMenuManager;
   private IContextMenuProvider    _tableViewerContextMenuProvider = new TableContextMenuProvider();

   private boolean                 _isInUIUpdate;

   private final NumberFormat      _nf1                            = NumberFormat.getNumberInstance();
   private final NumberFormat      _nf3                            = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);
   }

   /*
    * UI controls
    */
   private PixelConverter _pc;
   private Composite      _viewerContainer;

   private Menu           _tableContextMenu;

   private class LocationComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = TableColumnFactory.SENSOR_NAME_ID;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final LocationItem item1 = (LocationItem) e1;
         final LocationItem item2 = (LocationItem) e2;

         double rc = 0;

         // Determine which column and do the appropriate sort
//         switch (__sortColumnId) {
//
//         case TableColumnFactory.SENSOR_MANUFACTURER_NAME_ID:
//            rc = item1.location.getManufacturerName().compareTo(item2.location.getManufacturerName());
//            break;
//
//         case TableColumnFactory.SENSOR_PRODUCT_NAME_ID:
//            rc = item1.location.getProductName().compareTo(item2.location.getProductName());
//            break;
//
//         case TableColumnFactory.SENSOR_NAME_ID:
//         default:
//            rc = item1.location.getSensorName().compareTo(item2.location.getSensorName());
//         }
//
//         // 2nd sort by sensor custom name
//         if (rc == 0) {
//            rc = item1.location.getSensorName().compareTo(item2.location.getSensorName());
//         }
//
//         // 3nd sort by manufacturer name
//         if (rc == 0) {
//            rc = item1.location.getManufacturerName().compareTo(item2.location.getManufacturerName());
//         }
//
//         // 4nd sort by product name
//         if (rc == 0) {
//            rc = item1.location.getProductName().compareTo(item2.location.getProductName());
//         }

         // If descending order, flip the direction
         if (__sortDirection == DESCENDING) {
            rc = -rc;
         }

         /*
          * MUST return 1 or -1 otherwise long values are not sorted correctly.
          */
         return rc > 0
               ? 1
               : rc < 0
                     ? -1
                     : 0;
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

   private class LocationContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allLocationItems.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   class LocationItem {

      TourLocation location;

      @Override
      public boolean equals(final Object obj) {

         if (this == obj) {
            return true;
         }
         if (obj == null) {
            return false;
         }
         if (getClass() != obj.getClass()) {
            return false;
         }

         final LocationItem other = (LocationItem) obj;
         if (!getEnclosingInstance().equals(other.getEnclosingInstance())) {
            return false;
         }

         return location.getLocationId() == other.location.getLocationId();
      }

      private TourLocationView getEnclosingInstance() {
         return TourLocationView.this;
      }

      @Override
      public int hashCode() {

         final int prime = 31;
         int result = 1;
         result = prime * result + getEnclosingInstance().hashCode();
         result = prime * result + Objects.hash(location.getLocationId());

         return result;
      }

      @Override
      public String toString() {

         return UI.EMPTY_STRING

               + "SensorItem" + NL //                                                  //$NON-NLS-1$

               + "[" + NL //                                                           //$NON-NLS-1$

               + "sensor                     = " + location + NL //                      //$NON-NLS-1$
//               + "usedFirstTime              = " + usedFirstTime + NL //               //$NON-NLS-1$
//               + "usedLastTime               = " + usedLastTime + NL //                //$NON-NLS-1$
//               + "isBatteryLevelAvailable    = " + isBatteryLevelAvailable + NL //     //$NON-NLS-1$
//               + "isBatteryStatusAvailable   = " + isBatteryStatusAvailable + NL //    //$NON-NLS-1$
//               + "isBatteryVoltageAvailable  = " + isBatteryVoltageAvailable + NL //   //$NON-NLS-1$

               + "]" + NL //                                                           //$NON-NLS-1$
         ;
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

         _tableContextMenu = createUI_22_CreateViewerContextMenu();

         return _tableContextMenu;
      }

   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
      };

      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

               _locationViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

               _locationViewer.refresh();

//               /*
//                * the tree must be redrawn because the styled text does not show with the new color
//                */
//               _locationViewer.getTable().redraw();

//            } else if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {
//
//               // reselect current sensor that the sensor chart (when opened) is reloaded
//
//               final StructuredSelection selection = getViewerSelection();
//
//               _locationViewer.setSelection(selection, true);
//
//               final Table table = _locationViewer.getTable();
//               table.showSelection();
            }
         }
      };

      _prefChangeListener_Common = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

               // measurement system has changed

               _columnManager.saveState(_state);
               _columnManager.clearColumns();

               defineAllColumns();

               _locationViewer = (TableViewer) recreateViewer(_locationViewer);
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   private void addTourEventListener() {

      _tourPropertyListener = (part, eventId, eventData) -> {

         if (part == TourLocationView.this) {
            return;
         }

         if (eventId == TourEventId.UPDATE_UI
               || eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

            // new locations could be added

            reloadViewer();
         }
      };

      TourManager.getInstance().addTourEventListener(_tourPropertyListener);
   }

   private void createActions() {

   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
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

      addPrefListener();
      addPartListener();
      addTourEventListener();

      createActions();
      fillToolbar();

      BusyIndicator.showWhile(parent.getDisplay(), () -> {

         loadAllLocations();

         updateUI_SetViewerInput();

         restoreState_WithUI();
      });
   }

   private void createUI(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_10_LocationViewer(_viewerContainer);
      }
   }

   private void createUI_10_LocationViewer(final Composite parent) {

      /*
       * Create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);
      table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

//      table.addKeyListener(keyPressedAdapter(keyEvent -> {
//
//         if (keyEvent.keyCode == SWT.DEL) {
//            onAction_DeleteSensor();
//         }
//      }));

      /*
       * Create table viewer
       */
      _locationViewer = new TableViewer(table);

      _columnManager.createColumns(_locationViewer);

      _locationViewer.setUseHashlookup(true);
      _locationViewer.setContentProvider(new LocationContentProvider());
      _locationViewer.setComparator(_locationComparator);

      _locationViewer.addSelectionChangedListener(selectionChangedEvent -> onLocation_Select());
//      _locationViewer.addDoubleClickListener(doubleClickEvent -> onAction_OpenSensorChart());

      updateUI_SetSortDirection(
            _locationComparator.__sortColumnId,
            _locationComparator.__sortDirection);

      createUI_20_ContextMenu();
   }

   /**
    * create the views context menu
    */
   private void createUI_20_ContextMenu() {

      _tableContextMenu = createUI_22_CreateViewerContextMenu();

      final Table table = (Table) _locationViewer.getControl();

      _columnManager.createHeaderContextMenu(table, _tableViewerContextMenuProvider);
   }

   private Menu createUI_22_CreateViewerContextMenu() {

      final Table table = (Table) _locationViewer.getControl();
      final Menu tableContextMenu = _viewerMenuManager.createContextMenu(table);

      return tableContextMenu;
   }

   private void defineAllColumns() {

      defineColumn_Location_Name();
   }

   /**
    * Column: Location name
    */
   private void defineColumn_Location_Name() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_NAME.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final LocationItem locationItem = (LocationItem) cell.getElement();
            cell.setText(locationItem.location.display_name);
         }
      });
   }

   @Override
   public void dispose() {

      getViewSite().getPage().removePartListener(_partListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      TourManager.getInstance().removeTourEventListener(_tourPropertyListener);

      super.dispose();
   }

   private void enableActions() {

      final LocationItem selectedLocationItem = getSelectedLocationItem();
      final boolean isLocationSelected = selectedLocationItem != null;

//      _action_OpenSensorChartView.setEnabled(isSensorSelected);
//      _action_DeleteSensor.setEnabled(isSensorSelected);
//      _action_EditSensor.setEnabled(isSensorSelected);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      /*
       * Fill menu
       */

//      menuMgr.add(_action_EditSensor);
//      menuMgr.add(_action_OpenSensorChartView);
//      menuMgr.add(_action_DeleteSensor);

      enableActions();
   }

   private void fillToolbar() {

//      final IActionBars actionBars = getViewSite().getActionBars();

      /*
       * Fill view menu
       */
//      final IMenuManager menuMgr = actionBars.getMenuManager();

      /*
       * Fill view toolbar
       */
//      final IToolBarManager tbm = actionBars.getToolBarManager();

   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   private LocationItem getSelectedLocationItem() {

      final IStructuredSelection selection = _locationViewer.getStructuredSelection();
      final Object firstElement = selection.getFirstElement();

      if (firstElement != null) {

         return ((LocationItem) firstElement);
      }

      return null;
   }

   /**
    * @param sortColumnId
    *
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _locationViewer.getTable().getColumns();

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
      return _locationViewer;
   }

   private StructuredSelection getViewerSelection() {

      return (StructuredSelection) _locationViewer.getSelection();
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onColumn_Select(e);
         }
      };
   }

   private void loadAllLocations() {

      _allLocationItems.clear();

      PreparedStatement statement = null;
      ResultSet result = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                       //$NON-NLS-1$

               + "name," + NL //                     1  //$NON-NLS-1$
               + "display_name" + NL //             2  //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_TOUR_LOCATION + NL //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);
         result = statement.executeQuery();

         while (result.next()) {

            final LocationItem locationItem = new LocationItem();

            // create detached tour location
            final TourLocation tourLocation = locationItem.location = new TourLocation();

            _allLocationItems.add(locationItem);

//SET_FORMATTING_OFF

            tourLocation.name          = result.getString(1);
            tourLocation.display_name  = result.getString(2);

//SET_FORMATTING_ON
         }

      } catch (final SQLException e) {
         SQL.showException(e);
      } finally {
         Util.closeSql(statement);
         Util.closeSql(result);
      }
   }

   private void onColumn_Select(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            // update viewer with new sorting
            _locationComparator.setSortColumn(e.widget);
            _locationViewer.refresh();
         }
         updateUI_SelectLocation(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   private void onLocation_Select() {

      if (_isInUIUpdate) {
         return;
      }

      final IStructuredSelection selection = _locationViewer.getStructuredSelection();
      final Object firstElement = selection.getFirstElement();

      if (firstElement == null) {
         return;
      }

      final TourLocation selectedLocation = ((LocationItem) firstElement).location;

      // this view could be inactive -> selection is not fired with the SelectionProvider interface
//      TourManager.fireEventWithCustomData(
//            TourEventId.SELECTION_SENSOR,
//            new SelectionSensor(selectedLocation, null),
//            this);
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            _locationViewer.getTable().dispose();

            createUI_10_LocationViewer(_viewerContainer);

            // update UI
            _viewerContainer.layout();

            // update the viewer
            updateUI_SetViewerInput();
         }
         updateUI_SelectLocation(selectionBackup);
      }
      _viewerContainer.setRedraw(true);

      _locationViewer.getTable().setFocus();

      return _locationViewer;
   }

   @Override
   public void reloadViewer() {

      loadAllLocations();

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            updateUI_SetViewerInput();
         }
         updateUI_SelectLocation(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, TableColumnFactory.SENSOR_NAME_ID);
      final int sortDirection = Util.getStateInt(_state, STATE_SORT_COLUMN_DIRECTION, LocationComparator.ASCENDING);

      // update comparator
      _locationComparator.__sortColumnId = sortColumnId;
      _locationComparator.__sortDirection = sortDirection;
   }

   private void restoreState_WithUI() {

      /*
       * Restore selected location
       */
      final String[] allViewerIndices = _state.getArray(STATE_SELECTED_SENSOR_INDICES);

      if (allViewerIndices != null) {

         final ArrayList<Object> allLocations = new ArrayList<>();

         for (final String viewerIndex : allViewerIndices) {

            Object location = null;

            try {
               final int index = Integer.parseInt(viewerIndex);
               location = _locationViewer.getElementAt(index);
            } catch (final NumberFormatException e) {
               // just ignore
            }

            if (location != null) {
               allLocations.add(location);
            }
         }

         if (allLocations.size() > 0) {

            _viewerContainer.getDisplay().timerExec(

                  /*
                   * When this value is too small, then the chart axis could not be painted
                   * correctly with the dark theme during the app startup
                   */
                  1000,

                  () -> {

                     _locationViewer.setSelection(new StructuredSelection(allLocations.toArray()), true);

                     enableActions();
                  });
         }
      }

      enableActions();
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

      _state.put(STATE_SORT_COLUMN_ID, _locationComparator.__sortColumnId);
      _state.put(STATE_SORT_COLUMN_DIRECTION, _locationComparator.__sortDirection);

      // keep selected tours
      Util.setState(_state, STATE_SELECTED_SENSOR_INDICES, _locationViewer.getTable().getSelectionIndices());
   }

   @Override
   public void setFocus() {
      _locationViewer.getTable().setFocus();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * Select and reveal tour marker item.
    *
    * @param selection
    * @param checkedElements
    */
   private void updateUI_SelectLocation(final ISelection selection) {

      _isInUIUpdate = true;
      {
         _locationViewer.setSelection(selection, true);

         final Table table = _locationViewer.getTable();
         table.showSelection();
      }
      _isInUIUpdate = false;
   }

   /**
    * Set the sort column direction indicator for a column.
    *
    * @param sortColumnId
    * @param isAscendingSort
    */
   private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

      final Table table = _locationViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(sortDirection == LocationComparator.ASCENDING ? SWT.UP : SWT.DOWN);
   }

   private void updateUI_SetViewerInput() {

      _isInUIUpdate = true;
      {
         _locationViewer.setInput(new Object[0]);
      }
      _isInUIUpdate = false;
   }
}
