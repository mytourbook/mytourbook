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

import java.time.ZonedDateTime;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnDefinitionFor1stVisibleAlignmentColumn;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.data.TourLocation;
import net.tourbook.tour.location.MapLocationManager;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Map location slideout
 */
public class SlideoutMapLocation extends AdvancedSlideout implements ITourViewer {

   private static final String           COLUMN_CREATED_DATE_TIME = "createdDateTime";                   //$NON-NLS-1$
   private static final String           COLUMN_FILTER_NAME       = "filterName";                        //$NON-NLS-1$
   private static final String           COLUMN_GEO_PARTS         = "geoParts";                          //$NON-NLS-1$
   private static final String           COLUMN_LATITUDE_1        = "latitude1";                         //$NON-NLS-1$
   private static final String           COLUMN_LATITUDE_2        = "latitude2";                         //$NON-NLS-1$
   private static final String           COLUMN_LONGITUDE_1       = "longitude1";                        //$NON-NLS-1$
   private static final String           COLUMN_LONGITUDE_2       = "longitude2";                        //$NON-NLS-1$
   private static final String           COLUMN_SEQUENCE          = "sequence";                          //$NON-NLS-1$
   private static final String           COLUMN_ZOOM_LEVEL        = "zoomLevel";                         //$NON-NLS-1$

   private static final IPreferenceStore _prefStore               = TourbookPlugin.getPrefStore();
   private IDialogSettings               _state;

   private PixelConverter                _pc;

   private TableViewer                   _mapLocationViewer;
   private CompareResultComparator       _mapLocationComparator   = new CompareResultComparator();
   private ColumnManager                 _columnManager;
   private SelectionAdapter              _columnSortListener;

   private ToolItem                      _toolItem;

   List<TourLocation>                    _allMapLocations         = MapLocationManager.getMapLocations();

   /*
    * UI controls
    */
   private Composite _viewerContainer;

   private class CompareResultComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = COLUMN_LATITUDE_1;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final TourLocation location1 = (TourLocation) e1;
         final TourLocation location2 = (TourLocation) e2;

         boolean _isSortByTime = true;
         double rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnId) {

//         case COLUMN_GEO_PARTS:
//            rc = location1.numGeoParts - location2.numGeoParts;
//            break;
//
//         case COLUMN_LATITUDE_1:
//            rc = location1.geoLocation_TopLeft.latitude - location2.geoLocation_TopLeft.latitude;
//            break;
//
//         case COLUMN_LONGITUDE_1:
//            rc = location1.geoLocation_TopLeft.longitude - location2.geoLocation_TopLeft.longitude;
//            break;
//
//         case COLUMN_LATITUDE_2:
//            rc = location1.geoLocation_BottomRight.latitude - location2.geoLocation_BottomRight.latitude;
//            break;
//
//         case COLUMN_LONGITUDE_2:
//            rc = location1.geoLocation_BottomRight.longitude - location2.geoLocation_BottomRight.longitude;
//            break;
//
//         case COLUMN_FILTER_NAME:
//            rc = location1.filterName.compareTo(location2.filterName);
//            break;

         case COLUMN_CREATED_DATE_TIME:

            // sorting by date is already set
            break;

//         case COLUMN_ZOOM_LEVEL:
//            rc = location1.mapZoomLevel - location2.mapZoomLevel;
//            break;

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

   /**
    * @param ownerControl
    * @param toolBar
    * @param map2View
    * @param map2State
    */
   public SlideoutMapLocation(final ToolItem toolItem,
                              final IDialogSettings state) {

      super(toolItem.getParent(), state, new int[] { 325, 400, 325, 400 });

      _toolItem = toolItem;

      _state = state;

      setTitleText(Messages.Slideout_MapLocation_Label_Title);

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      createUI(parent);

      // load viewer
      updateUI_Viewer();

      restoreState();
      enableControls();

      MapLocationManager.setMapLocationSlideout(this);
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
         createUI_600_FilterViewer(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_600_FilterViewer(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourGeoFilter_Label_History);
         }
         {
            _viewerContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
            GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
            {
               createUI_610_FilterViewer_Table(_viewerContainer);
            }
         }

//         createUI_680_ViewerActions(container);
      }
   }

   private void createUI_610_FilterViewer_Table(final Composite parent) {

      /*
       * Create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);
      table.setLinesVisible(false);

      /*
       * It took a while that the correct listener is set and also the checked item is fired and not
       * the wrong selection.
       */
      table.addListener(SWT.Selection, new Listener() {

         @Override
         public void handleEvent(final Event event) {
//            onGeoPart_Select(event);
         }
      });

      table.addKeyListener(new KeyListener() {

         @Override
         public void keyPressed(final KeyEvent e) {

            switch (e.keyCode) {

            case SWT.DEL:
//               onGeoFilter_Delete();
               break;

            default:
               break;
            }
         }

         @Override
         public void keyReleased(final KeyEvent e) {}
      });

      /*
       * create table viewer
       */
      _mapLocationViewer = new TableViewer(table);

      // very important: the editing support must be set BEFORE the columns are created
//      _colDef_FilterName.setEditingSupport(new FilterName_EditingSupport(_geoFilterViewer));

      UI.setCellEditSupport(_mapLocationViewer);

      _columnManager.createColumns(_mapLocationViewer);
      _columnManager.setSlideoutShell(this);

      _mapLocationViewer.setUseHashlookup(true);
      _mapLocationViewer.setContentProvider(new MapLocationContentProvider());
      _mapLocationViewer.setComparator(_mapLocationComparator);

//      _geoFilterViewer.addSelectionChangedListener(selectionChangedEvent -> onGeoFilter_Select(selectionChangedEvent));
//      _geoFilterViewer.addDoubleClickListener(doubleClickEvent -> onGeoFilter_ToggleReadEditMode());

      updateUI_SetSortDirection(
            _mapLocationComparator.__sortColumnId,
            _mapLocationComparator.__sortDirection);

      createUI_620_ContextMenu();
   }

   /**
    * Ceate the view context menus
    */
   private void createUI_620_ContextMenu() {

      final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$

      menuMgr.setRemoveAllWhenShown(true);

      menuMgr.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {
//          fillContextMenu(manager);
         }
      });

      /**
       * ColumnManager context menu is set in {@link #beforeHideToolTip()} but this works only until
       * the column manager is recreating the viewer.
       * <p>
       * The slideout must be reopened that the context menu is working again :-(
       */

      /*
       * Set context menu after a viewer reload, even more complicated
       */
      _columnManager.createHeaderContextMenu(_mapLocationViewer.getTable(), null, getRRShellWithResize());

   }

   private void defineAllColumns() {

      defineColumn_00_SequenceNumber();
      defineColumn_05_FilterName();
      defineColumn_10_Created();

      new ColumnDefinitionFor1stVisibleAlignmentColumn(_columnManager);
   }

   private void defineColumn_00_SequenceNumber() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_SEQUENCE, SWT.TRAIL);

      colDef.setColumnLabel(Messages.GeoCompare_View_Column_SequenceNumber_Label);
      colDef.setColumnHeaderText(Messages.GeoCompare_View_Column_SequenceNumber_Header);
      colDef.setColumnHeaderToolTipText(Messages.GeoCompare_View_Column_SequenceNumber_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));

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
   private void defineColumn_05_FilterName() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_FILTER_NAME, SWT.LEAD);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_FilterName_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_FilterName_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourLocation item = (TourLocation) cell.getElement();

            if (UI.IS_SCRAMBLE_DATA) {

               cell.setText(UI.scrambleText(item.display_name));

            } else {

               cell.setText(item.display_name);
            }
         }
      });
   }

   /**
    * Column: Created
    */
   private void defineColumn_10_Created() {

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

            cell.setText(created.format(TimeTools.Formatter_DateTime_SM));
         }
      });
   }

   private void enableControls() {

   }

   @Override
   public ColumnManager getColumnManager() {

      return _columnManager;
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
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

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSelect_SortColumn(e);
         }
      };
   }

   @Override
   protected void onDispose() {

      MapLocationManager.setMapLocationSlideout(null);

      super.onDispose();
   }

   @Override
   protected void onFocus() {

   }

   private void onSelect_SortColumn(final SelectionEvent e) {

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

         createUI_610_FilterViewer_Table(_viewerContainer);
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

   }

   @Override
   protected void saveState() {

      _columnManager.saveState(_state);

      super.saveState();
   }

   @Override
   protected void saveState_BeforeDisposed() {

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
      table.setSortDirection(sortDirection == CompareResultComparator.ASCENDING ? SWT.UP : SWT.DOWN);
   }

   private void updateUI_Viewer() {

      _mapLocationViewer.setInput(new Object[0]);
   }

}
