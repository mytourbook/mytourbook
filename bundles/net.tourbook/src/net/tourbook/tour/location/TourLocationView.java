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

      defineColumn_10_Name();
      defineColumn_10_DisplayName();

      defineColumn_20_Continent();
      defineColumn_20_Country();
      defineColumn_20_CountryCode();

      defineColumn_30_Region();
      defineColumn_30_State();
      defineColumn_30_StateDistrict();
      defineColumn_30_County();

      defineColumn_40_Municipality();
      defineColumn_40_City();
      defineColumn_40_Town();
      defineColumn_40_Village();

      defineColumn_45_CityDistrict();
      defineColumn_45_District();
      defineColumn_45_Borough();
      defineColumn_45_Suburb();
      defineColumn_45_Subdivision();

      defineColumn_50_Hamlet();
      defineColumn_50_Croft();
      defineColumn_50_IsolatedDwelling();

      defineColumn_55_Neighbourhood();
      defineColumn_55_Allotments();
      defineColumn_55_Quarter();

      defineColumn_60_CityBlock();
      defineColumn_60_Residential();
      defineColumn_60_Farm();
      defineColumn_60_Farmyard();
      defineColumn_60_Industrial();
      defineColumn_60_Commercial();
      defineColumn_60_Retail();

      defineColumn_65_Road();

      defineColumn_70_HouseName();
      defineColumn_70_HouseNumber();

      defineColumn_80_Aerialway();
      defineColumn_80_Aeroway();
      defineColumn_80_Amenity();
      defineColumn_80_Boundary();
      defineColumn_80_Bridge();
      defineColumn_80_Club();
      defineColumn_80_Craft();
      defineColumn_80_Emergency();
      defineColumn_80_Historic();
      defineColumn_80_Landuse();
      defineColumn_80_Leisure();
      defineColumn_80_ManMade();
      defineColumn_80_Military();
      defineColumn_80_MountainPass();
      defineColumn_80_Natural();
      defineColumn_80_Office();
      defineColumn_80_Place();
      defineColumn_80_Railway();
      defineColumn_80_Shop();
      defineColumn_80_Tourism();
      defineColumn_80_Tunnel();
      defineColumn_80_Waterway();

      defineColumn_90_Postcode();
   }

   private void defineColumn_10_DisplayName() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_DisplayName.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.display_name);
         }
      });
   }

   private void defineColumn_10_Name() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Name.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.name);
         }
      });
   }

   private void defineColumn_20_Continent() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Continent.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.continent);
         }
      });
   }

   private void defineColumn_20_Country() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Country.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.country);
         }
      });
   }

   private void defineColumn_20_CountryCode() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_CountryCode.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.country_code);
         }
      });
   }

   private void defineColumn_30_County() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_County.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.county);
         }
      });
   }

   private void defineColumn_30_Region() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Region.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.region);
         }
      });
   }

   private void defineColumn_30_State() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_State.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.state);
         }
      });
   }

   private void defineColumn_30_StateDistrict() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_StateDistrict.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.state_district);
         }
      });
   }

   private void defineColumn_40_City() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_City.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.city);
         }
      });
   }

   private void defineColumn_40_Municipality() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Municipality.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.municipality);
         }
      });
   }

   private void defineColumn_40_Town() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Town.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.town);
         }
      });
   }

   private void defineColumn_40_Village() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Village.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.village);
         }
      });
   }

   private void defineColumn_45_Borough() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Borough.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.borough);
         }
      });
   }

   private void defineColumn_45_CityDistrict() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_CityDistrict.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.city_district);
         }
      });
   }

   private void defineColumn_45_District() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_District.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.district);
         }
      });
   }

   private void defineColumn_45_Subdivision() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Subdivision.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.subdivision);
         }
      });
   }

   private void defineColumn_45_Suburb() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Suburb.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.suburb);
         }
      });
   }

   private void defineColumn_50_Croft() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Croft.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.croft);
         }
      });
   }

   private void defineColumn_50_Hamlet() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Hamlet.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.hamlet);
         }
      });
   }

   private void defineColumn_50_IsolatedDwelling() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_IsolatedDwelling.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.isolated_dwelling);
         }
      });
   }

   private void defineColumn_55_Allotments() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Allotments.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.allotments);
         }
      });
   }

   private void defineColumn_55_Neighbourhood() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Neighbourhood.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.neighbourhood);
         }
      });
   }

   private void defineColumn_55_Quarter() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Quarter.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.quarter);
         }
      });
   }

   private void defineColumn_60_CityBlock() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_CityBlock.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.city_block);
         }
      });
   }

   private void defineColumn_60_Commercial() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Commercial.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.commercial);
         }
      });
   }

   private void defineColumn_60_Farm() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Farm.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.farm);
         }
      });
   }

   private void defineColumn_60_Farmyard() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Farmyard.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.farmyard);
         }
      });
   }

   private void defineColumn_60_Industrial() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Industrial.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.industrial);
         }
      });
   }

   private void defineColumn_60_Residential() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Residential.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.residential);
         }
      });
   }

   private void defineColumn_60_Retail() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Retail.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.retail);
         }
      });
   }

   private void defineColumn_65_Road() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Road.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.road);
         }
      });
   }

   private void defineColumn_70_HouseName() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_HouseName.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.house_name);
         }
      });
   }

   private void defineColumn_70_HouseNumber() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_HouseNumber.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.house_number);
         }
      });
   }

   private void defineColumn_80_Aerialway() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Aerialway.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.aerialway);
         }
      });
   }

   private void defineColumn_80_Aeroway() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Aeroway.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.aeroway);
         }
      });
   }

   private void defineColumn_80_Amenity() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Amenity.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.amenity);
         }
      });
   }

   private void defineColumn_80_Boundary() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Boundary.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.boundary);
         }
      });
   }

   private void defineColumn_80_Bridge() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Bridge.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.bridge);
         }
      });
   }

   private void defineColumn_80_Club() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Club.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.club);
         }
      });
   }

   private void defineColumn_80_Craft() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Craft.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.craft);
         }
      });
   }

   private void defineColumn_80_Emergency() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Emergency.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.emergency);
         }
      });
   }

   private void defineColumn_80_Historic() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Historic.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.historic);
         }
      });
   }

   private void defineColumn_80_Landuse() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Landuse.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.landuse);
         }
      });
   }

   private void defineColumn_80_Leisure() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Leisure.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.leisure);
         }
      });
   }

   private void defineColumn_80_ManMade() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_ManMade.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.man_made);
         }
      });
   }

   private void defineColumn_80_Military() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Military.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.military);
         }
      });
   }

   private void defineColumn_80_MountainPass() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_MountainPass.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.mountain_pass);
         }
      });
   }

   private void defineColumn_80_Natural() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Natural.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.natural2);
         }
      });
   }

   private void defineColumn_80_Office() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Office.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.office);
         }
      });
   }

   private void defineColumn_80_Place() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Place.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.place);
         }
      });
   }

   private void defineColumn_80_Railway() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Railway.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.railway);
         }
      });
   }

   private void defineColumn_80_Shop() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Shop.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.shop);
         }
      });
   }

   private void defineColumn_80_Tourism() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Tourism.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.tourism);
         }
      });
   }

   private void defineColumn_80_Tunnel() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Tunnel.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.tunnel);
         }
      });
   }

   private void defineColumn_80_Waterway() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Waterway.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.waterway);
         }
      });
   }

   private void defineColumn_90_Postcode() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_Postcode.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).location.postcode);
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

               + "name," + NL //                      1  //$NON-NLS-1$
               + "display_name," + NL //              2  //$NON-NLS-1$

               + "continent," + NL //                 3  //$NON-NLS-1$
               + "country," + NL //                   4  //$NON-NLS-1$
               + "country_code," + NL //              5  //$NON-NLS-1$

               + "region," + NL //                    6  //$NON-NLS-1$
               + "state," + NL //                     7  //$NON-NLS-1$
               + "state_district," + NL //            8  //$NON-NLS-1$
               + "county," + NL //                    9  //$NON-NLS-1$

               + "municipality," + NL //              10 //$NON-NLS-1$
               + "city," + NL //                      11 //$NON-NLS-1$
               + "town," + NL //                      12 //$NON-NLS-1$
               + "village," + NL //                   13 //$NON-NLS-1$

               + "city_district," + NL //             14 //$NON-NLS-1$
               + "district," + NL //                  15 //$NON-NLS-1$
               + "borough," + NL //                   16 //$NON-NLS-1$
               + "suburb," + NL //                    17 //$NON-NLS-1$
               + "subdivision," + NL //               18 //$NON-NLS-1$

               + "hamlet," + NL //                    19 //$NON-NLS-1$
               + "croft," + NL //                     20 //$NON-NLS-1$
               + "isolated_dwelling," + NL //         21 //$NON-NLS-1$

               + "neighbourhood," + NL //             22 //$NON-NLS-1$
               + "allotments," + NL //                23 //$NON-NLS-1$
               + "quarter," + NL //                   24 //$NON-NLS-1$

               + "city_block," + NL //                25 //$NON-NLS-1$
               + "residential," + NL //               26 //$NON-NLS-1$
               + "farm," + NL //                      27 //$NON-NLS-1$
               + "farmyard," + NL //                  28 //$NON-NLS-1$
               + "industrial," + NL //                29 //$NON-NLS-1$
               + "commercial," + NL //                30 //$NON-NLS-1$
               + "retail," + NL //                    31 //$NON-NLS-1$

               + "road," + NL //                      32 //$NON-NLS-1$

               + "house_number," + NL //              33 //$NON-NLS-1$
               + "house_name," + NL //                34 //$NON-NLS-1$

               + "aerialway," + NL //                 35 //$NON-NLS-1$
               + "aeroway," + NL //                   36 //$NON-NLS-1$
               + "amenity," + NL //                   37 //$NON-NLS-1$
               + "boundary," + NL //                  38 //$NON-NLS-1$
               + "bridge," + NL //                    39 //$NON-NLS-1$
               + "club," + NL //                      40 //$NON-NLS-1$
               + "craft," + NL //                     41 //$NON-NLS-1$
               + "emergency," + NL //                 42 //$NON-NLS-1$
               + "historic," + NL //                  43 //$NON-NLS-1$
               + "landuse," + NL //                   44 //$NON-NLS-1$
               + "leisure," + NL //                   45 //$NON-NLS-1$
               + "man_made," + NL //                  46 //$NON-NLS-1$
               + "military," + NL //                  47 //$NON-NLS-1$
               + "mountain_pass," + NL //             48 //$NON-NLS-1$
               + "natural2," + NL //                  49 //$NON-NLS-1$
               + "office," + NL //                    50 //$NON-NLS-1$
               + "place," + NL //                     51 //$NON-NLS-1$
               + "railway," + NL //                   52 //$NON-NLS-1$
               + "shop," + NL //                      53 //$NON-NLS-1$
               + "tourism," + NL //                   54 //$NON-NLS-1$
               + "tunnel," + NL //                    55 //$NON-NLS-1$
               + "waterway," + NL //                  56 //$NON-NLS-1$

               + "postcode" + NL //                  57 //$NON-NLS-1$

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

            tourLocation.name                = result.getString(1);
            tourLocation.display_name        = result.getString(2);

            tourLocation.continent           = result.getString(3);
            tourLocation.country             = result.getString(4);
            tourLocation.country_code        = result.getString(5);

            tourLocation.region              = result.getString(6);
            tourLocation.state               = result.getString(7);
            tourLocation.state_district      = result.getString(8);
            tourLocation.county              = result.getString(9);

            tourLocation.municipality        = result.getString(10);
            tourLocation.city                = result.getString(11);
            tourLocation.town                = result.getString(12);
            tourLocation.village             = result.getString(13);

            tourLocation.city_district       = result.getString(14);
            tourLocation.district            = result.getString(15);
            tourLocation.borough             = result.getString(16);
            tourLocation.suburb              = result.getString(17);
            tourLocation.subdivision         = result.getString(18);

            tourLocation.hamlet              = result.getString(19);
            tourLocation.croft               = result.getString(20);
            tourLocation.isolated_dwelling   = result.getString(21);

            tourLocation.neighbourhood       = result.getString(22);
            tourLocation.allotments          = result.getString(23);
            tourLocation.quarter             = result.getString(24);

            tourLocation.city_block          = result.getString(25);
            tourLocation.residential         = result.getString(26);
            tourLocation.farm                = result.getString(27);
            tourLocation.farmyard            = result.getString(28);
            tourLocation.industrial          = result.getString(29);
            tourLocation.commercial          = result.getString(30);
            tourLocation.retail              = result.getString(31);

            tourLocation.road                = result.getString(32);

            tourLocation.house_number        = result.getString(33);
            tourLocation.house_name          = result.getString(34);

            tourLocation.aerialway           = result.getString(35);
            tourLocation.aeroway             = result.getString(36);
            tourLocation.amenity             = result.getString(37);
            tourLocation.boundary            = result.getString(38);
            tourLocation.bridge              = result.getString(39);
            tourLocation.club                = result.getString(40);
            tourLocation.craft               = result.getString(41);
            tourLocation.emergency           = result.getString(42);
            tourLocation.historic            = result.getString(43);
            tourLocation.landuse             = result.getString(44);
            tourLocation.leisure             = result.getString(45);
            tourLocation.man_made            = result.getString(46);
            tourLocation.military            = result.getString(47);
            tourLocation.mountain_pass       = result.getString(48);
            tourLocation.natural2            = result.getString(49);
            tourLocation.office              = result.getString(50);
            tourLocation.place               = result.getString(51);
            tourLocation.railway             = result.getString(52);
            tourLocation.shop                = result.getString(53);
            tourLocation.tourism             = result.getString(54);
            tourLocation.tunnel              = result.getString(55);
            tourLocation.waterway            = result.getString(56);

            tourLocation.postcode            = result.getString(57);

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
