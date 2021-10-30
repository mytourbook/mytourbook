/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.sensors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.Util;
import net.tourbook.data.DeviceSensor;
import net.tourbook.data.DeviceSensorType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TableColumnFactory;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IMenuListener;
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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
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

public class SensorView extends ViewPart implements ITourViewer {

   public static final String      ID                              = "net.tourbook.ui.views.sensors.SensorView.ID"; //$NON-NLS-1$

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

   private PostSelectionProvider   _postSelectionProvider;

   private TableViewer             _sensorViewer;
   private SensorComparator        _markerComparator               = new SensorComparator();
   private ColumnManager           _columnManager;
   private SelectionAdapter        _columnSortListener;

   private List<SensorItem>        _allSensorItems                 = new ArrayList<>();

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

   private class SensorComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = TableColumnFactory.SENSOR_NAME_ID;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final SensorItem item1 = (SensorItem) e1;
         final SensorItem item2 = (SensorItem) e2;

         double rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnId) {

         case TableColumnFactory.SENSOR_MANUFACTURER_NAME_ID:
            rc = item1.sensor.getManufacturerName().compareTo(item2.sensor.getManufacturerName());
            break;

         case TableColumnFactory.SENSOR_PRODUCT_NAME_ID:
            rc = item1.sensor.getProductName().compareTo(item2.sensor.getProductName());
            break;

         case TableColumnFactory.SENSOR_SERIAL_NUMBER_ID:

            final long serialNumber1AsLong = item1.sensor.getSerialNumberAsLong();
            final long serialNumber2AsLong = item2.sensor.getSerialNumberAsLong();

            if (serialNumber1AsLong != Long.MIN_VALUE && serialNumber2AsLong != Long.MIN_VALUE) {

               // first sort as number

               rc = serialNumber1AsLong - serialNumber2AsLong;

            } else {

               // secondly sord as string

               rc = item1.sensor.getSerialNumber().compareTo(item2.sensor.getSerialNumber());
            }

            break;

         case TableColumnFactory.SENSOR_STATE_BATTERY_LEVEL_ID:

            final int isBatteryLevelAvailable1 = item1.isBatteryLevelAvailable ? 1 : 0;
            final int isBatteryLevelAvailable2 = item2.isBatteryLevelAvailable ? 1 : 0;

            rc = isBatteryLevelAvailable1 - isBatteryLevelAvailable2;

            break;

         case TableColumnFactory.SENSOR_STATE_BATTERY_STATUS_ID:

            final int isBatteryStatusAvailable1 = item1.isBatteryStatusAvailable ? 1 : 0;
            final int isBatteryStatusAvailable2 = item2.isBatteryStatusAvailable ? 1 : 0;

            rc = isBatteryStatusAvailable1 - isBatteryStatusAvailable2;

            break;

         case TableColumnFactory.SENSOR_STATE_BATTERY_VOLTAGE_ID:

            final int isBatteryVoltageAvailable1 = item1.isBatteryVoltageAvailable ? 1 : 0;
            final int isBatteryVoltageAvailable2 = item2.isBatteryVoltageAvailable ? 1 : 0;

            rc = isBatteryVoltageAvailable1 - isBatteryVoltageAvailable2;

            break;

         case TableColumnFactory.SENSOR_TIME_FIRST_USED_ID:
            rc = item1.usedFirstTime - item2.usedFirstTime;
            break;

         case TableColumnFactory.SENSOR_TIME_LAST_USED_ID:
            rc = item1.usedLastTime - item2.usedLastTime;
            break;

         case TableColumnFactory.SENSOR_TYPE_ID:

            final DeviceSensorType sensorType1 = item1.sensor.getSensorType();
            final DeviceSensorType sensorType2 = item2.sensor.getSensorType();

            if (sensorType1 != null && sensorType2 != null) {
               rc = sensorType1.compareTo(sensorType2);
            }

            break;

         case TableColumnFactory.SENSOR_NAME_ID:
         default:
            rc = item1.sensor.getSensorName().compareTo(item2.sensor.getSensorName());
         }

         // 2nd sort by sensor custom name
         if (rc == 0) {
            rc = item1.sensor.getSensorName().compareTo(item2.sensor.getSensorName());
         }

         // 3nd sort by manufacturer name
         if (rc == 0) {
            rc = item1.sensor.getManufacturerName().compareTo(item2.sensor.getManufacturerName());
         }

         // 4nd sort by product name
         if (rc == 0) {
            rc = item1.sensor.getProductName().compareTo(item2.sensor.getProductName());
         }

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

   private class SensorContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allSensorItems.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   class SensorItem {

      DeviceSensor sensor;

      long         usedFirstTime;
      long         usedLastTime;

      boolean      isBatteryLevelAvailable;
      boolean      isBatteryStatusAvailable;
      boolean      isBatteryVoltageAvailable;

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

         final SensorItem other = (SensorItem) obj;
         if (!getEnclosingInstance().equals(other.getEnclosingInstance())) {
            return false;
         }

         return sensor.getSensorId() == other.sensor.getSensorId();
      }

      private SensorView getEnclosingInstance() {
         return SensorView.this;
      }

      @Override
      public int hashCode() {

         final int prime = 31;
         int result = 1;
         result = prime * result + getEnclosingInstance().hashCode();
         result = prime * result + Objects.hash(sensor.getSensorId());

         return result;
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

               _sensorViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

               _sensorViewer.refresh();

               /*
                * the tree must be redrawn because the styled text does not show with the new color
                */
               _sensorViewer.getTable().redraw();

            } else if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

               // reselect current sensor that the sensor chart (when opened) is reloaded

               final StructuredSelection selection = getViewerSelection();

               _sensorViewer.setSelection(selection, true);

               final Table table = _sensorViewer.getTable();
               table.showSelection();
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

               _sensorViewer = (TableViewer) recreateViewer(_sensorViewer);
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   private void addTourEventListener() {

      _tourPropertyListener = (part, eventId, eventData) -> {

         if (part == SensorView.this) {
            return;
         }

         if (eventId == TourEventId.UPDATE_UI
               || eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

            // new tours could be imported with new sensors

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
      _viewerMenuManager.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {

            fillContextMenu(manager);
         }
      });
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

      // set this view part as selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      createActions();
      fillToolbar();

      BusyIndicator.showWhile(parent.getDisplay(), () -> {

         loadAllSensors();

         updateUI_SetViewerInput();

         restoreState_WithUI();
      });
   }

   private void createUI(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_10_MarkerViewer(_viewerContainer);
      }
   }

   private void createUI_10_MarkerViewer(final Composite parent) {

      /*
       * Create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);
      table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      /*
       * Create table viewer
       */
      _sensorViewer = new TableViewer(table);

      _columnManager.createColumns(_sensorViewer);

      _sensorViewer.setUseHashlookup(true);
      _sensorViewer.setContentProvider(new SensorContentProvider());
      _sensorViewer.setComparator(_markerComparator);

      _sensorViewer.addSelectionChangedListener(selectionChangedEvent -> onSensor_Select());
      _sensorViewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(final DoubleClickEvent event) {
            onSensor_DoubleClick();
         }
      });

      updateUI_SetSortDirection(
            _markerComparator.__sortColumnId,
            _markerComparator.__sortDirection);

      createUI_20_ContextMenu();
   }

   /**
    * create the views context menu
    */
   private void createUI_20_ContextMenu() {

      _tableContextMenu = createUI_22_CreateViewerContextMenu();

      final Table table = (Table) _sensorViewer.getControl();

      _columnManager.createHeaderContextMenu(table, _tableViewerContextMenuProvider);
   }

   private Menu createUI_22_CreateViewerContextMenu() {

      final Table table = (Table) _sensorViewer.getControl();
      final Menu tableContextMenu = _viewerMenuManager.createContextMenu(table);

      return tableContextMenu;
   }

   private void defineAllColumns() {

      defineColumn_Sensor_Name();
      defineColumn_Sensor_Type();
      defineColumn_BatteryState_Level();
      defineColumn_BatteryState_Status();
      defineColumn_BatteryState_Voltage();
      defineColumn_Sensor_Description();
      defineColumn_Manufacturer_Name();
      defineColumn_Manufacturer_Number();
      defineColumn_Product_Name();
      defineColumn_Product_Number();
      defineColumn_SerialNumber();
      defineColumn_Time_FirstUsed();
      defineColumn_Time_LastUsed();
   }

   /**
    * Column: Battery state: 0...100 %
    */
   private void defineColumn_BatteryState_Level() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_STATE_BATTERY_LEVEL.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(sensorItem.isBatteryLevelAvailable
                  ? UI.SYMBOL_BOX
                  : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Battery state: OK, Low, ...
    */
   private void defineColumn_BatteryState_Status() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_STATE_BATTERY_STATUS.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(sensorItem.isBatteryStatusAvailable
                  ? UI.SYMBOL_BOX
                  : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Battery voltage state: 3.x Volt
    */
   private void defineColumn_BatteryState_Voltage() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_STATE_BATTERY_VOLTAGE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(sensorItem.isBatteryVoltageAvailable
                  ? UI.SYMBOL_BOX
                  : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Manufacturer name
    */
   private void defineColumn_Manufacturer_Name() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_MANUFACTURER_NAME.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(sensorItem.sensor.getManufacturerName());
         }
      });
   }

   /**
    * Column: Manufacturer number
    */
   private void defineColumn_Manufacturer_Number() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_MANUFACTURER_NUMBER.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(Integer.toString(sensorItem.sensor.getManufacturerNumber()));
         }
      });
   }

   /**
    * Column: Product name
    */
   private void defineColumn_Product_Name() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_PRODUCT_NAME.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(sensorItem.sensor.getProductName());
         }
      });
   }

   /**
    * Column: Product number
    */
   private void defineColumn_Product_Number() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_PRODUCT_NUMBER.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(Integer.toString(sensorItem.sensor.getProductNumber()));
         }
      });
   }

   /**
    * Column: Sensor description
    */
   private void defineColumn_Sensor_Description() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_DESCRIPTION.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(sensorItem.sensor.getDescription());
         }
      });
   }

   /**
    * Column: Sensor name
    */
   private void defineColumn_Sensor_Name() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_NAME.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(sensorItem.sensor.getSensorName());
         }
      });
   }

   /**
    * Column: Sensor type
    */
   private void defineColumn_Sensor_Type() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_TYPE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            final DeviceSensorType sensorType = sensorItem.sensor.getSensorType();

            cell.setText(sensorType == null
                  ? UI.EMPTY_STRING
                  : SensorManager.getSensorTypeName(sensorType));
         }
      });
   }

   /**
    * Column: Serial number
    */
   private void defineColumn_SerialNumber() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_SERIAL_NUMBER.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(sensorItem.sensor.getSerialNumber());
         }
      });
   }

   /**
    * Column: Used start time
    */
   private void defineColumn_Time_FirstUsed() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_TIME_FIRST_USED.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(TimeTools.Formatter_Date_S.format(TimeTools.toLocalDateTime(sensorItem.usedFirstTime)));
         }
      });
   }

   /**
    * Column: Used end time
    */
   private void defineColumn_Time_LastUsed() {

      final ColumnDefinition colDef = TableColumnFactory.SENSOR_TIME_LAST_USED.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final SensorItem sensorItem = (SensorItem) cell.getElement();
            cell.setText(TimeTools.Formatter_Date_S.format(TimeTools.toLocalDateTime(sensorItem.usedLastTime)));
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

   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      /*
       * Fill menu
       */

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

   /**
    * @param sortColumnId
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _sensorViewer.getTable().getColumns();

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
      return _sensorViewer;
   }

   private StructuredSelection getViewerSelection() {

      return (StructuredSelection) _sensorViewer.getSelection();
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

   private void loadAllSensors() {

      /*
       * Create sensor items for all sensors
       */
      final Map<Long, DeviceSensor> allDbDeviceSensors = TourDatabase.getAllDeviceSensors_BySensorID();
      final HashMap<Long, SensorItem> allSensorItems = new HashMap<>();

      for (final DeviceSensor sensor : allDbDeviceSensors.values()) {

         final SensorItem sensorItem = new SensorItem();
         sensorItem.sensor = sensor;

         allSensorItems.put(sensor.getSensorId(), sensorItem);
      }

      PreparedStatement statementMinMax = null;
      ResultSet resultMinMax = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         /*
          * Set used start/end time
          */
         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                //$NON-NLS-1$

               + "   DEVICESENSOR_SENSORID      ," + NL //                    1  //$NON-NLS-1$

               + "   Min(TourStartTime)         ," + NL //                    2  //$NON-NLS-1$
               + "   Max(TourStartTime)         ," + NL //                    3  //$NON-NLS-1$

               + "   Max(BatteryLevel_Start)    ," + NL //                    4  //$NON-NLS-1$
               + "   Max(BatteryLevel_End)      ," + NL //                    5  //$NON-NLS-1$
               + "   Max(BatteryStatus_Start)   ," + NL //                    6  //$NON-NLS-1$
               + "   Max(BatteryStatus_End)     ," + NL //                    7  //$NON-NLS-1$
               + "   Max(BatteryVoltage_Start)  ," + NL //                    8  //$NON-NLS-1$
               + "   Max(BatteryVoltage_End)     " + NL //                    9  //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_DEVICE_SENSOR_VALUE + NL //        //$NON-NLS-1$
               + "GROUP BY DEVICESENSOR_SENSORID" + NL //                        //$NON-NLS-1$
         ;

         statementMinMax = conn.prepareStatement(sql);
         resultMinMax = statementMinMax.executeQuery();

         while (resultMinMax.next()) {

            final long sensorId = resultMinMax.getLong(1);

            final SensorItem sensorItem = allSensorItems.get(sensorId);
            if (sensorItem == null) {

               // this should not happen

            } else {

// SET_FORMATTING_OFF
               final long dbUsedFirstTime       = resultMinMax.getLong(2);
               final long dbUsedLastTime        = resultMinMax.getLong(3);

               final float dbMaxLevel_Start     = resultMinMax.getFloat(4);
               final float dbMaxLevel_End       = resultMinMax.getFloat(5);
               final float dbMaxStatus_Start    = resultMinMax.getFloat(6);
               final float dbMaxStatus_End      = resultMinMax.getFloat(6);
               final float dbMaxVoltage_Start   = resultMinMax.getFloat(8);
               final float dbMaxVoltage_End     = resultMinMax.getFloat(9);
// SET_FORMATTING_ON

               sensorItem.usedFirstTime = dbUsedFirstTime;
               sensorItem.usedLastTime = dbUsedLastTime;

               sensorItem.isBatteryLevelAvailable = dbMaxLevel_Start > 0 | dbMaxLevel_End > 0;
               sensorItem.isBatteryStatusAvailable = dbMaxStatus_Start > 0 | dbMaxStatus_End > 0;
               sensorItem.isBatteryVoltageAvailable = dbMaxVoltage_Start > 0 | dbMaxVoltage_End > 0;
            }
         }

         _allSensorItems.clear();
         _allSensorItems.addAll(allSensorItems.values());

      } catch (final SQLException e) {

         SQL.showException(e);

      } finally {

         Util.closeSql(statementMinMax);
         Util.closeSql(resultMinMax);
      }
   }

   private void onColumn_Select(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            // update viewer with new sorting
            _markerComparator.setSortColumn(e.widget);
            _sensorViewer.refresh();
         }
         updateUI_SelectSensor(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   private void onSensor_DoubleClick() {

      final Object firstElement = _sensorViewer.getStructuredSelection().getFirstElement();

      if (firstElement != null) {

         final SensorItem sensorItem = (SensorItem) firstElement;

         final long sensorId = sensorItem.sensor.getSensorId();
         final DeviceSensor sensor = TourDatabase.getAllDeviceSensors_BySensorID().get(sensorId);

         if (new DialogSensor(_viewerContainer.getShell(), sensor).open() != Window.OK) {

            _sensorViewer.getTable().setFocus();

            return;
         }

         // update model
         final DeviceSensor savedSensor = TourDatabase.saveEntity(sensor, sensorId, DeviceSensor.class);
         sensorItem.sensor = savedSensor;

         // update UI - resort and preserve selection
         _viewerContainer.setRedraw(false);
         {
            // keep selection
            final ISelection selectionBackup = getViewerSelection();
            {
               _sensorViewer.refresh();
            }
            updateUI_SelectSensor(selectionBackup);
         }
         _viewerContainer.setRedraw(true);

         /*
          * When a sensor is modified, all tours with sensor values do contain the old sensor
          * instance
          */
         TourManager.getInstance().clearTourDataCache();
         TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED, this);
      }
   }

   private void onSensor_Select() {

      if (_isInUIUpdate) {
         return;
      }

      final IStructuredSelection selection = _sensorViewer.getStructuredSelection();
      final DeviceSensor sensor = ((SensorItem) selection.getFirstElement()).sensor;

      if (selection.getFirstElement() != null) {
         _postSelectionProvider.setSelection(new StructuredSelection(sensor));
      }
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            _sensorViewer.getTable().dispose();

            createUI_10_MarkerViewer(_viewerContainer);

            // update UI
            _viewerContainer.layout();

            // update the viewer
            updateUI_SetViewerInput();
         }
         updateUI_SelectSensor(selectionBackup);
      }
      _viewerContainer.setRedraw(true);

      _sensorViewer.getTable().setFocus();

      return _sensorViewer;
   }

   @Override
   public void reloadViewer() {

      loadAllSensors();

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            updateUI_SetViewerInput();
         }
         updateUI_SelectSensor(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, TableColumnFactory.SENSOR_NAME_ID);
      final int sortDirection = Util.getStateInt(_state, STATE_SORT_COLUMN_DIRECTION, SensorComparator.ASCENDING);

      // update comparator
      _markerComparator.__sortColumnId = sortColumnId;
      _markerComparator.__sortDirection = sortDirection;
   }

   private void restoreState_WithUI() {

      /*
       * Restore selected sensor
       */
      final String[] allViewerIndices = _state.getArray(STATE_SELECTED_SENSOR_INDICES);

      if (allViewerIndices != null) {

         final ArrayList<Object> allSensors = new ArrayList<>();

         for (final String viewerIndex : allViewerIndices) {

            Object sensor = null;

            try {
               final int index = Integer.parseInt(viewerIndex);
               sensor = _sensorViewer.getElementAt(index);
            } catch (final NumberFormatException e) {
               // just ignore
            }

            if (sensor != null) {
               allSensors.add(sensor);
            }
         }

         if (allSensors.size() > 0) {

            _viewerContainer.getDisplay().timerExec(

                  /*
                   * When this value is too small, then the chart axis could not be painted
                   * correctly with the dark theme during the app startup
                   */
                  500,

                  () -> {

                     _sensorViewer.setSelection(new StructuredSelection(allSensors.toArray()), true);
                  });
         }
      }

      enableActions();
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

      _state.put(STATE_SORT_COLUMN_ID, _markerComparator.__sortColumnId);
      _state.put(STATE_SORT_COLUMN_DIRECTION, _markerComparator.__sortDirection);

      // keep selected tours
      Util.setState(_state, STATE_SELECTED_SENSOR_INDICES, _sensorViewer.getTable().getSelectionIndices());
   }

   @Override
   public void setFocus() {
      _sensorViewer.getTable().setFocus();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * Select and reveal tour marker item.
    *
    * @param selection
    * @param checkedElements
    */
   private void updateUI_SelectSensor(final ISelection selection) {

      _isInUIUpdate = true;
      {
         _sensorViewer.setSelection(selection, true);

         final Table table = _sensorViewer.getTable();
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

      final Table table = _sensorViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(sortDirection == SensorComparator.ASCENDING ? SWT.UP : SWT.DOWN);
   }

   private void updateUI_SetViewerInput() {

      _isInUIUpdate = true;
      {
         _sensorViewer.setInput(new Object[0]);
      }
      _isInUIUpdate = false;
   }
}
