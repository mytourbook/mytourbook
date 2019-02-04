/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter.geo;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

/**
 * Slideout with the tour geo filters.
 */
public class SlideoutTourGeoFilter extends AdvancedSlideout implements ITourViewer {

   private static final String STATE_SORT_COLUMN_DIRECTION = "STATE_SORT_COLUMN_DIRECTION"; //$NON-NLS-1$
   private static final String STATE_SORT_COLUMN_ID        = "STATE_SORT_COLUMN_ID";        //$NON-NLS-1$

   private static final String COLUMN_CREATED_DATE_TIME    = "createdDateTime";             //$NON-NLS-1$
   private static final String COLUMN_GEO_PARTS            = "geoParts";                    //$NON-NLS-1$
   private static final String COLUMN_LATITUDE_1           = "latitude1";                   //$NON-NLS-1$
   private static final String COLUMN_LONGITUDE_1          = "longitude1";                  //$NON-NLS-1$
   private static final String COLUMN_LATITUDE_2           = "latitude2";                   //$NON-NLS-1$
   private static final String COLUMN_LONGITUDE_2          = "longitude2";                  //$NON-NLS-1$
   private static final String COLUMN_SEQUENCE             = "sequence";                    //$NON-NLS-1$

//   private final IPreferenceStore             _prefStore                  = TourbookPlugin.getPrefStore();
   private final IDialogSettings              _state;

   private TableViewer                        _geoFilterViewer;
   private CompareResultComparator            _geoPartComparator = new CompareResultComparator();
   private ColumnManager                      _columnManager;

   private final ArrayList<TourGeoFilterItem> _allGeoFilter      = TourGeoFilterManager.getAllGeoFilter();
   private TourGeoFilterItem                  _selectedFilter;

   private ToolItem                           _tourFilterItem;

//   private boolean                            _isInUpdate;

   private SelectionAdapter _columnSortListener;
//   private ModifyListener                     _defaultModifyListener;
//   private FocusListener                      _keepOpenListener;

   private final NumberFormat _nf2 = NumberFormat.getInstance();
   {
      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
   }

   /*
    * UI controls
    */
   private PixelConverter _pc;
   private Composite      _viewerContainer;

   private Button         _btnDeleteGeoFilter;
   private Button         _btnDeleteGeoFilterAll;

   private class CompareResultComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = COLUMN_LATITUDE_1;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final TourGeoFilterItem item1 = (TourGeoFilterItem) e1;
         final TourGeoFilterItem item2 = (TourGeoFilterItem) e2;

         boolean _isSortByTime = true;
         double rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnId) {

         case COLUMN_GEO_PARTS:
            rc = item1.numGeoParts - item2.numGeoParts;
            break;

         case COLUMN_LATITUDE_1:
            rc = item1.latitude1 - item2.latitude1;
            break;

         case COLUMN_LONGITUDE_1:
            rc = item1.longitude1 - item2.longitude1;
            break;

         case COLUMN_LATITUDE_2:
            rc = item1.latitude2 - item2.latitude2;
            break;

         case COLUMN_LONGITUDE_2:
            rc = item1.longitude2 - item2.longitude2;
            break;

         case COLUMN_CREATED_DATE_TIME:

            // sorting by date is already set
            break;

         default:
            _isSortByTime = true;
         }

         if (rc == 0 && _isSortByTime) {
            rc = item1.createdMS - item2.createdMS;
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

   private class GeoFilterProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allGeoFilter.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   public SlideoutTourGeoFilter(final ToolItem toolItem,
                                final IDialogSettings state) {

      super(toolItem.getParent(), state, new int[] { 900, 200, 900, 200 });

      _state = state;
      _tourFilterItem = toolItem;

      setShellFadeOutDelaySteps(30);
      setTitleText(Messages.Slideout_TourGeoFilter_Label_Title);
   }

   @Override
   protected boolean canCloseShell(final Shell[] openedShells) {

      /*
       * Linux creates a shell in DateTime widget which prevents to close the slideout, accept this
       * "shell".
       */

//      for (final Shell shell : openedShells) {
//
////			Util.dumpChildren(shell, 1);
//
//         for (final Control child : shell.getChildren()) {
//
//            final String controlText = child.toString();
//
////				System.out.println(this.getClass().getName() + "\tcontrolText:" + controlText);
//
//         }
//      }

      return true;
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      /*
       * Reset to a valid state when the slideout is opened again
       */
      _selectedFilter = null;

      initUI(parent);
      restoreState_BeforeUI();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      initUI(parent);

      createUI(parent);

      // load viewer
      updateUI_Viewer();

      restoreState();
      enableControls();
   }

   private void createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .applyTo(container);
         GridLayoutFactory.swtDefaults().applyTo(container);
         {
            _viewerContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
            GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
            {
               createUI_100_FilterViewer(_viewerContainer);
            }

            createUI_200_ViewerActions(container);
         }
      }
   }

   private void createUI_100_FilterViewer(final Composite parent) {

      /*
       * create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION /* | SWT.MULTI /* | SWT.BORDER */);
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

      /*
       * create table viewer
       */
      _geoFilterViewer = new TableViewer(table);

      _columnManager.createColumns(_geoFilterViewer);

      _geoFilterViewer.setUseHashlookup(true);
      _geoFilterViewer.setContentProvider(new GeoFilterProvider());
      _geoFilterViewer.setComparator(_geoPartComparator);

      _geoFilterViewer.addSelectionChangedListener(new ISelectionChangedListener() {

         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onGeoFilter_Select(event);
         }
      });

      _geoFilterViewer.addDoubleClickListener(new IDoubleClickListener() {

         @Override
         public void doubleClick(final DoubleClickEvent event) {
//          onBookmark_Rename(true);
         }
      });

      _geoFilterViewer.getTable().addKeyListener(new KeyListener() {

         @Override
         public void keyPressed(final KeyEvent e) {

            switch (e.keyCode) {

            case SWT.DEL:
               onGeoFilter_Delete();
               break;

            default:
               break;
            }
         }

         @Override
         public void keyReleased(final KeyEvent e) {}
      });

      updateUI_SetSortDirection(//
            _geoPartComparator.__sortColumnId,
            _geoPartComparator.__sortDirection);

      createUI_102_ContextMenu();
   }

   /**
    * Ceate the view context menus
    */
   private void createUI_102_ContextMenu() {

      final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$

      menuMgr.setRemoveAllWhenShown(true);

      menuMgr.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {
//          fillContextMenu(manager);
         }
      });

      /**
       * THIS IS NOT WORKING, IT CAUSES AN
       * <p>
       * java.lang.IllegalArgumentException: Widget has the wrong parent
       * <p>
       * which needs more time for investigation
       */
//      final Table table = _geoFilterViewer.getTable();
//      final Menu tableHeaderContextMenu = menuMgr.createContextMenu(table);
//
//      _columnManager.createHeaderContextMenu(table, tableHeaderContextMenu);
   }

   private void createUI_200_ViewerActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults()
            .margins(0, 0)
            .numColumns(2)
            .applyTo(container);
      {
         {
            /*
             * Button: delete geo filter
             */
            _btnDeleteGeoFilter = new Button(container, SWT.NONE);
            _btnDeleteGeoFilter.setText(Messages.App_Action_Delete);
            _btnDeleteGeoFilter.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onGeoFilter_Delete();
               }
            });
            UI.setButtonLayoutData(_btnDeleteGeoFilter);
         }
         {
            /*
             * Button: delete all geo filter
             */
            _btnDeleteGeoFilterAll = new Button(container, SWT.NONE);
            _btnDeleteGeoFilterAll.setText(Messages.App_Action_Delete_All);
            _btnDeleteGeoFilterAll.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onGeoFilter_Delete_All();
               }
            });
            UI.setButtonLayoutData(_btnDeleteGeoFilterAll);
         }
      }
   }

   private void defineAllColumns() {

      defineColumn_00_SequenceNumber();
      defineColumn_10_Created();
      defineColumn_20_NumGeoParts();

      defineColumn_50_Latitude1();
      defineColumn_52_Longitude1();

      defineColumn_60_Latitude2();
      defineColumn_62_Longitude2();
   }

   private void defineColumn_00_SequenceNumber() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_SEQUENCE, SWT.TRAIL);

      colDef.setColumnLabel(Messages.GeoCompare_View_Column_SequenceNumber_Label);
      colDef.setColumnHeaderText(Messages.GeoCompare_View_Column_SequenceNumber_Header);
      colDef.setColumnHeaderToolTipText(Messages.GeoCompare_View_Column_SequenceNumber_Label);

      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final int indexOf = _geoFilterViewer.getTable().indexOf((TableItem) cell.getItem());

            cell.setText(Integer.toString(indexOf + 1));
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
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilterItem item = (TourGeoFilterItem) cell.getElement();
            final ZonedDateTime created = item.created;

            cell.setText(created.format(TimeTools.Formatter_DateTime_SM));
         }
      });
   }

   /**
    * Column: Number of geo parts
    */
   private void defineColumn_20_NumGeoParts() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_GEO_PARTS, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_NumGeoParts_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_NumGeoParts_Header);
      colDef.setColumnHeaderToolTipText(Messages.Slideout_TourGeoFilter_Column_NumGeoParts_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(6));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilterItem item = (TourGeoFilterItem) cell.getElement();

            cell.setText(Integer.toString(item.numGeoParts));
         }
      });
   }

   /**
    * Column: Latitude 1 - top/left
    */
   private void defineColumn_50_Latitude1() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LATITUDE_1, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_Latitude1_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_Latitude1_Header);
      colDef.setColumnHeaderToolTipText(Messages.Slideout_TourGeoFilter_Column_Latitude1_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilterItem item = (TourGeoFilterItem) cell.getElement();

            cell.setText(_nf2.format(item.latitude1));
         }
      });
   }

   /**
    * Column: Longitude 1 - top/left
    */
   private void defineColumn_52_Longitude1() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LONGITUDE_1, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_Longitude1_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_Longitude1_Header);
      colDef.setColumnHeaderToolTipText(Messages.Slideout_TourGeoFilter_Column_Longitude1_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilterItem item = (TourGeoFilterItem) cell.getElement();

            cell.setText(_nf2.format(item.longitude1));
         }
      });
   }

   /**
    * Column: Latitude 2 - top/left
    */
   private void defineColumn_60_Latitude2() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LATITUDE_2, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_Latitude2_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_Latitude2_Header);
      colDef.setColumnHeaderToolTipText(Messages.Slideout_TourGeoFilter_Column_Latitude2_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilterItem item = (TourGeoFilterItem) cell.getElement();

            cell.setText(_nf2.format(item.latitude2));
         }
      });
   }

   /**
    * Column: Longitude 2 - top/left
    */
   private void defineColumn_62_Longitude2() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LONGITUDE_2, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_Longitude2_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_Longitude2_Header);
      colDef.setColumnHeaderToolTipText(Messages.Slideout_TourGeoFilter_Column_Longitude2_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilterItem item = (TourGeoFilterItem) cell.getElement();

            cell.setText(_nf2.format(item.longitude2));
         }
      });
   }

   private void enableControls() {

      final boolean isFilterSelected = _selectedFilter != null;

      _btnDeleteGeoFilter.setEnabled(isFilterSelected);
      _btnDeleteGeoFilterAll.setEnabled(_allGeoFilter.size() > 0);
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _tourFilterItem.getBounds();
      final Point itemDisplayPosition = _tourFilterItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   /**
    * @param sortColumnId
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _geoFilterViewer.getTable().getColumns();

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
      return _geoFilterViewer;
   }

   private StructuredSelection getViewerSelection() {

      return (StructuredSelection) _geoFilterViewer.getSelection();
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSelect_SortColumn(e);
         }
      };

//      _defaultModifyListener = new ModifyListener() {
//         @Override
//         public void modifyText(final ModifyEvent e) {
////            onProfile_Modify();
//         }
//      };
//
//      _keepOpenListener = new FocusListener() {
//
//         @Override
//         public void focusGained(final FocusEvent e) {
//
//            /*
//             * This will fix the problem that when the list of a combobox is displayed, then the
//             * slideout will disappear :-(((
//             */
//            setIsKeepOpenInternally(true);
//         }
//
//         @Override
//         public void focusLost(final FocusEvent e) {
//            setIsKeepOpenInternally(false);
//         }
//      };
   }

   @Override
   protected void onFocus() {

//      if (_selectedProfile != null
//            && _selectedProfile.name != null
//            && _selectedProfile.name.equals(Messages.Tour_Filter_Default_ProfileName)) {
//
//         // default profile is selected, make it easy to rename it
//
//         _txtProfileName.selectAll();
//         _txtProfileName.setFocus();
//
//      } else {
//
//         _profileViewer.getTable().setFocus();
//      }
   }

   private void onGeoFilter_Delete() {

      final Object filterItem = _geoFilterViewer.getStructuredSelection().getFirstElement();

      if (filterItem == null) {
         return;
      }

      // update model
      _allGeoFilter.remove(filterItem);

      // update UI
      final Table filterTable = _geoFilterViewer.getTable();
      final int selectionIndex = filterTable.getSelectionIndex();

      _geoFilterViewer.remove(filterItem);

      // select next filter item
      final int nextIndex = Math.min(filterTable.getItemCount() - 1, selectionIndex);
      if (nextIndex < 0) {

         _selectedFilter = null;

         // fire event to hide geo grid
         TourManager.fireEventWithCustomData(TourEventId.MAP_SHOW_GEO_GRID, null, null);

      } else {

         // set new selection this will also fire the event

         _geoFilterViewer.setSelection(new StructuredSelection(_geoFilterViewer.getElementAt(nextIndex)));
      }

      enableControls();

      // set focus back to table
      _geoFilterViewer.getTable().setFocus();
   }

   private void onGeoFilter_Delete_All() {

      final Object filterItem = _geoFilterViewer.getStructuredSelection().getFirstElement();

      if (filterItem == null) {
         return;
      }

      // update model
      _allGeoFilter.clear();

      // update UI
      _geoFilterViewer.refresh();

      // fire event to hide geo grid
      TourManager.fireEventWithCustomData(TourEventId.MAP_SHOW_GEO_GRID, null, null);

      enableControls();

      // set focus back to table
      _geoFilterViewer.getTable().setFocus();
   }

   private void onGeoFilter_Select(final SelectionChangedEvent event) {

      final Object selectedItem = event.getStructuredSelection().getFirstElement();

      _selectedFilter = (TourGeoFilterItem) selectedItem;

      TourManager.fireEventWithCustomData(TourEventId.MAP_SHOW_GEO_GRID, _selectedFilter, null);

      TourGeoFilterManager.selectFilter(_selectedFilter);

      enableControls();
   }

   private void onSelect_SortColumn(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            // update viewer with new sorting
            _geoPartComparator.setSortColumn(e.widget);
            _geoFilterViewer.refresh();
         }
         updateUI_SelectGeoFilterItem(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         _geoFilterViewer.getTable().dispose();

         createUI_100_FilterViewer(_viewerContainer);
         _viewerContainer.layout();

         // update the viewer
         reloadViewer();
      }
      _viewerContainer.setRedraw(true);

      return _geoFilterViewer;
   }

   public void refreshViewer() {

      _geoFilterViewer.refresh();
   }

   @Override
   public void reloadViewer() {

      updateUI_Viewer();
   }

   private void restoreState() {

   }

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, COLUMN_CREATED_DATE_TIME);
      final int sortDirection = Util.getStateInt(
            _state,
            STATE_SORT_COLUMN_DIRECTION,
            CompareResultComparator.DESCENDING);

      // update comparator
      _geoPartComparator.__sortColumnId = sortColumnId;
      _geoPartComparator.__sortDirection = sortDirection;
   }

   @Override
   protected void saveState() {

      _state.put(STATE_SORT_COLUMN_ID, _geoPartComparator.__sortColumnId);
      _state.put(STATE_SORT_COLUMN_DIRECTION, _geoPartComparator.__sortDirection);

      _columnManager.saveState(_state);

      super.saveState();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * Select and reveal a compare item item.
    *
    * @param selection
    */
   private void updateUI_SelectGeoFilterItem(final ISelection selection) {

//      _isInUpdate = true;
      {
         _geoFilterViewer.setSelection(selection, true);

         _geoFilterViewer.getTable().showSelection();
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

      final Table table = _geoFilterViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(sortDirection == CompareResultComparator.ASCENDING ? SWT.UP : SWT.DOWN);
   }

   private void updateUI_Viewer() {

      _geoFilterViewer.setInput(new Object[0]);
   }

}
