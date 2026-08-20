/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.photo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.photo.TourPhotoReference;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.Messages;
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
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.part.ViewPart;

public class TourPhotosAllView extends ViewPart implements ITourProvider, ITourViewer {

   public static final String      ID                              = "net.tourbook.tour.photo.TourPhotosAllView"; //$NON-NLS-1$
   //
   private static final char       NL                              = UI.NEW_LINE;
   //
   private static final String     COLUMN_DATE                     = "Date";                                      //$NON-NLS-1$
   private static final String     COLUMN_LATITUDE                 = "Latitude";                                  //$NON-NLS-1$
   private static final String     COLUMN_LONGITUDE                = "Longitude";                                 //$NON-NLS-1$
   private static final String     COLUMN_PHOTO_ID                 = "PhotoId";                                   //$NON-NLS-1$
   private static final String     COLUMN_IMAGE_FILE_PATH          = "ImageFilePath";                             //$NON-NLS-1$
   private static final String     COLUMN_TOUR_ID                  = "TourId";                                    //$NON-NLS-1$
   private static final String     COLUMN_TIME                     = "Time";                                      //$NON-NLS-1$
   //
   private static final String     STATE_SELECTED_PHOTO_ITEM       = "STATE_SELECTED_PHOTO_ITEM";                 //$NON-NLS-1$
   private static final String     STATE_SORT_COLUMN_DIRECTION     = "STATE_SORT_COLUMN_DIRECTION";               //$NON-NLS-1$
   private static final String     STATE_SORT_COLUMN_ID            = "STATE_SORT_COLUMN_ID";                      //$NON-NLS-1$
   //
   private final IPreferenceStore  _prefStore                      = TourbookPlugin.getPrefStore();
   private final IPreferenceStore  _prefStore_Common               = CommonActivator.getPrefStore();
   private final IDialogSettings   _state                          = TourbookPlugin.getState(ID);
   //
   private PostSelectionProvider   _postSelectionProvider;
   //
   private ISelectionListener      _postSelectionListener;
   private IPropertyChangeListener _prefChangeListener;
   private IPropertyChangeListener _prefChangeListener_Common;
   private ITourEventListener      _tourEventListener;
   //
   private MenuManager             _viewerMenuManager;
   private IContextMenuProvider    _tableViewerContextMenuProvider = new TableContextMenuProvider();
   //
   private TableViewer             _photoViewer;
   private PhotoComparator         _photoComparator                = new PhotoComparator();
   private ColumnManager           _columnManager;
   private SelectionAdapter        _columnSortListener;
   //
   private List<TourPhotoItem>     _allPhotoItems                  = new ArrayList<>();
   //
   private boolean                 _isInUpdate;
   //
   private final DateFormat        _dateFormatter                  = DateFormat.getDateInstance(DateFormat.SHORT);
   private final DateFormat        _timeFormatter                  = DateFormat.getTimeInstance(DateFormat.SHORT);
   //
   private final NumberFormat      _nf6                            = NumberFormat.getNumberInstance();
   {
      _nf6.setMinimumFractionDigits(6);
      _nf6.setMaximumFractionDigits(6);
   }

   /*
    * UI controls
    */
   private PixelConverter _pc;
   private Composite      _viewerContainer;

   private Menu           _tableContextMenu;

   private class PhotoComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = COLUMN_TOUR_ID;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final TourPhotoItem m1 = (TourPhotoItem) e1;
         final TourPhotoItem m2 = (TourPhotoItem) e2;

         boolean _isSortByTime = false;
         double rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnId) {

         case COLUMN_LATITUDE:
            rc = m1.latitude - m2.latitude;
            if (rc == 0) {
               rc = m1.longitude - m2.longitude;
            }
            _isSortByTime = true;
            break;

         case COLUMN_LONGITUDE:
            rc = m1.longitude - m2.longitude;
            if (rc == 0) {
               rc = m1.latitude - m2.latitude;
            }
            _isSortByTime = true;
            break;

         case COLUMN_PHOTO_ID:
            rc = m1.photoId - m2.photoId;
            break;

         case COLUMN_TOUR_ID:
            rc = m1.tourId - m2.tourId;
            break;

         case COLUMN_DATE:
         case COLUMN_TIME:
            rc = m1.imageExifTime - m2.imageExifTime;
            break;

         case COLUMN_IMAGE_FILE_PATH:
         default:
            rc = m1.imageFilePathName.compareTo(m2.imageFilePathName);
            _isSortByTime = true;
         }

         if (rc == 0 && _isSortByTime) {
            rc = m1.imageExifTime - m2.imageExifTime;
         }

         // If descending order, flip the direction
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

   private class PhotoContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allPhotoItems.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   private class TableContextMenuProvider implements IContextMenuProvider {

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

   private class TourPhotoItem {

      int    sequence;
      long   photoId;
      long   tourId;

      String imageFilePathName;

      double latitude;
      double longitude;

      long   imageExifTime;

      public TourPhotoItem(final int sequence) {

         this.sequence = sequence;
      }

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

         final TourPhotoItem other = (TourPhotoItem) obj;
         if (!getEnclosingInstance().equals(other.getEnclosingInstance())) {
            return false;
         }

         return photoId == other.photoId;
      }

      private TourPhotosAllView getEnclosingInstance() {
         return TourPhotosAllView.this;
      }

      @Override
      public int hashCode() {

         final int prime = 31;
         int result = 1;

         result = prime * result + getEnclosingInstance().hashCode();
         result = prime * result + Objects.hash(photoId);

         return result;
      }

      @Override
      public String toString() {

         return UI.EMPTY_STRING

               + "TourPhotoItem" + NL //                                //$NON-NLS-1$

               + " imageFilePathName = " + imageFilePathName + NL //    //$NON-NLS-1$
         ;
      }

   }

   public TourPhotosAllView() {
      super();
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _photoViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

            _photoViewer.refresh();

            /*
             * the tree must be redrawn because the styled text does not show with the new color
             */
            _photoViewer.getTable().redraw();
         }
      };

      _prefChangeListener_Common = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

            // measurement system has changed

            _columnManager.saveState(_state);
            _columnManager.clearColumns();

            defineAllColumns();

            _photoViewer = (CheckboxTableViewer) recreateViewer(_photoViewer);
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   /**
    * Listen for events when a tour is selected or other selection events are fired
    */
   private void addSelectionListener() {

      _postSelectionListener = (workbenchPart, selection) -> {

         if (workbenchPart == TourPhotosAllView.this) {
            return;
         }

         onSelectionChanged(selection);
      };

      getViewSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (workbenchPart == TourPhotosAllView.this) {
            return;
         }

         if (_isInUpdate) {
            return;
         }

         if ((tourEventId == TourEventId.TOUR_CHANGED) && (eventData instanceof final TourEvent tourEventData)) {

            final ArrayList<TourData> modifiedTours = tourEventData.getModifiedTours();
            if (modifiedTours != null) {

               // update modified tour

               reloadViewer();
            }

         } else if (tourEventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

            reloadViewer();
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void createActions() {

   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(manager -> fillContextMenu(manager));
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

      addTourEventListener();
      addPrefListener();
      addSelectionListener();

      // set selection provider
      getViewSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      createActions();
      fillToolbar();

      BusyIndicator.showWhile(parent.getDisplay(), () -> {

         loadAllPhotos();

         updateUI_SetViewerInput();

         restoreState_WithUI();
      });
   }

   private void createUI(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_10_PhotoViewer(_viewerContainer);
      }
   }

   private void createUI_10_PhotoViewer(final Composite parent) {

      /*
       * create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);
      table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      /*
       * It took a while that the correct listener is set and also the checked item is fired and not
       * the wrong selection.
       */
      table.addListener(SWT.Selection, event -> onPhotoItem_Select(event));

      /*
       * create table viewer
       */
      _photoViewer = new CheckboxTableViewer(table);

      _columnManager.createColumns(_photoViewer);

      _photoViewer.setUseHashlookup(true);
      _photoViewer.setContentProvider(new PhotoContentProvider());
      _photoViewer.setComparator(_photoComparator);

      updateUI_SetSortDirection(
            _photoComparator.__sortColumnId,
            _photoComparator.__sortDirection);

      createUI_20_ContextMenu();
   }

   /**
    * create the views context menu
    */
   private void createUI_20_ContextMenu() {

      _tableContextMenu = createUI_22_CreateViewerContextMenu();

      final Table table = (Table) _photoViewer.getControl();

      _columnManager.createHeaderContextMenu(table, _tableViewerContextMenuProvider);
   }

   private Menu createUI_22_CreateViewerContextMenu() {

      final Table table = (Table) _photoViewer.getControl();
      final Menu tableContextMenu = _viewerMenuManager.createContextMenu(table);

      return tableContextMenu;
   }

   private void defineAllColumns() {

      defineColumn_Sequence();
      defineColumn_ImageFilePath();

      defineColumn_Date();
      defineColumn_Time();

      defineColumn_Latitude();
      defineColumn_Longitude();

      defineColumn_PhotoId();
      defineColumn_TourId();
   }

   /**
    * Column: Date
    */
   private void defineColumn_Date() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_DATE, SWT.TRAIL);

      colDef.setColumnName(Messages.ColumnFactory_Waypoint_Date);

      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoItem photoItem = (TourPhotoItem) cell.getElement();

            cell.setText(_dateFormatter.format(photoItem.imageExifTime));
         }
      });
   }

   /**
    * Column: Image file path
    */
   private void defineColumn_ImageFilePath() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_IMAGE_FILE_PATH, SWT.LEAD);

      colDef.setColumnName("Image Filepath");

      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoItem photoItem = (TourPhotoItem) cell.getElement();
            cell.setText(photoItem.imageFilePathName);
         }
      });
   }

   /**
    * Column: Latitude
    */
   private void defineColumn_Latitude() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LATITUDE, SWT.TRAIL);

      colDef.setColumnName(Messages.ColumnFactory_latitude);
      colDef.setIsDefaultColumn();

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(11));
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            String valueText;
            final double latitude = ((TourPhotoItem) cell.getElement()).latitude;

            if (latitude == TourDatabase.DEFAULT_DOUBLE) {
               valueText = UI.EMPTY_STRING;
            } else {
               valueText = _nf6.format(latitude);
            }

            cell.setText(valueText);
         }
      });
   }

   /**
    * Column: Longitude
    */
   private void defineColumn_Longitude() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LONGITUDE, SWT.TRAIL);

      colDef.setColumnName(Messages.ColumnFactory_longitude);
      colDef.setIsDefaultColumn();

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(11));
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            String valueText;
            final double longitude = ((TourPhotoItem) cell.getElement()).longitude;

            if (longitude == TourDatabase.DEFAULT_DOUBLE) {
               valueText = UI.EMPTY_STRING;
            } else {
               valueText = _nf6.format(longitude);
            }

            cell.setText(valueText);
         }
      });
   }

   /**
    * Column: Photo ID
    */
   private void defineColumn_PhotoId() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_PHOTO_ID, SWT.LEAD);

      colDef.setColumnName("Photo ID");

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            cell.setText(Long.toString(((TourPhotoItem) cell.getElement()).photoId));
         }
      });
   }

   /**
    * Column: #
    */
   private void defineColumn_Sequence() {

      final ColumnDefinition colDef = TableColumnFactory.DATA_SEQUENCE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setIsColumnMoveable(false);
      colDef.setColumnSelectionListener(_columnSortListener);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoItem photoItem = (TourPhotoItem) cell.getElement();

            // the sequence is starting by 0
            cell.setText(Integer.toString(photoItem.sequence + 1));
         }
      });
   }

   /**
    * Column: Time
    */
   private void defineColumn_Time() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_TIME, SWT.TRAIL);

      colDef.setColumnName(Messages.ColumnFactory_tour_time_label_hhmmss);

      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoItem photoItem = (TourPhotoItem) cell.getElement();

            cell.setText(_timeFormatter.format(photoItem.imageExifTime));
         }
      });

   }

   /**
    * Column: TourID
    */
   private void defineColumn_TourId() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_TOUR_ID, SWT.LEAD);

      colDef.setColumnName(Messages.ColumnFactory_TourId);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(22));
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            cell.setText(Long.toString(((TourPhotoItem) cell.getElement()).tourId));
         }
      });
   }

   @Override
   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      getViewSite().getPage().removePostSelectionListener(_postSelectionListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      super.dispose();
   }

   private void enableActions() {

   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      enableActions();
   }

   private void fillToolbar() {

   }

   private void fireSelection(final IStructuredSelection selection) {

      // get unique tour ids
      final HashSet<Long> allTourIds = new HashSet<>();
      for (final Object name : selection) {
         allTourIds.add(((TourPhotoItem) name).tourId);
      }

      final SelectionTourIds selectionTourIds = new SelectionTourIds(new ArrayList<>(allTourIds));

      _isInUpdate = true;
      {
         _postSelectionProvider.setSelection(selectionTourIds);
      }
      _isInUpdate = false;
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final ArrayList<TourData> selectedTours = new ArrayList<>();

      final StructuredSelection selection = getViewerSelection();

      for (final Object element : selection) {

         if (element instanceof final TourPhotoItem photoItem) {

            // get TourData from the photo item

            // get tour by id
            final TourData tourData = TourManager.getInstance().getTourData(photoItem.tourId);

            if (tourData != null) {
               selectedTours.add(tourData);
            }
         }
      }

      return selectedTours;
   }

   /**
    * @param sortColumnId
    *
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _photoViewer.getTable().getColumns();

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
      return _photoViewer;
   }

   private StructuredSelection getViewerSelection() {

      return (StructuredSelection) _photoViewer.getSelection();
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSortColumn(e);
         }
      };
   }

   private void loadAllPhotos() {

//      final long start = System.nanoTime();

      _allPhotoItems.clear();

      PreparedStatement statement = null;
      ResultSet result = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                              //$NON-NLS-1$

               + "photoId," + NL //                         1  //$NON-NLS-1$
               + TourDatabase.KEY_TOUR + "," + NL //        2  //$NON-NLS-1$

               + "imageFilePathName," + NL //               3  //$NON-NLS-1$

               + "latitude," + NL //                        4  //$NON-NLS-1$
               + "longitude," + NL //                       5  //$NON-NLS-1$

               + "imageExifTime" + NL //                    6  //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_TOUR_PHOTO + NL //$NON-NLS-1$

               + "ORDER BY imageFilePathName" + NL //          //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);
         result = statement.executeQuery();

         int sequence = 0;

         while (result.next()) {

            final TourPhotoItem photoItem = new TourPhotoItem(sequence++);

            _allPhotoItems.add(photoItem);

// SET_FORMATTING_OFF

            final String dbimageFilePathName = result.getString(3);

            photoItem.photoId                = result.getLong(1);
            photoItem.tourId                 = result.getLong(2);
            photoItem.imageFilePathName      = dbimageFilePathName == null ? UI.EMPTY_STRING : dbimageFilePathName;
            photoItem.latitude               = result.getDouble(4);
            photoItem.longitude              = result.getDouble(5);
            photoItem.imageExifTime          = result.getLong(6);

// SET_FORMATTING_ON
         }

      } catch (final SQLException e) {
         SQL.showException(e);
      } finally {
         Util.closeSql(statement);
         Util.closeSql(result);
      }

//      System.out.println((UI.timeStampNano() + " " + this.getClass().getName() + " \t")
//            + (((float) (System.nanoTime() - start) / 1000000) + " ms"));
//      // remove SYSTEM.OUT.PRINTLN
   }

   private void onPhotoItem_Select(final Event event) {

      if (_isInUpdate) {
         return;
      }

      final IStructuredSelection selection = _photoViewer.getStructuredSelection();

      fireSelection(selection);
   }

   /**
    * Select the tour photo item in the viewer
    *
    * @param photoSelection
    */
   private void onPhotoSelection(final PhotoSelection photoSelection) {

      final ArrayList<Photo> allGalleryPhotos = photoSelection.galleryPhotos;

      Long photoId = null;

      allPhotoLoop:

      // get first tour id
      for (final Photo photo : allGalleryPhotos) {

         final HashMap<Long, TourPhotoReference> allTourPhotoReferences = photo.getTourPhotoReferences();

         for (final TourPhotoReference tourPhotoReference : allTourPhotoReferences.values()) {

            photoId = tourPhotoReference.photoId;

            break allPhotoLoop;
         }
      }

      if (photoId == null) {
         return;
      }

      // find photo in all photo items
      TourPhotoItem selectPhotoItem = null;
      for (final TourPhotoItem photoItem : _allPhotoItems) {
         if (photoItem.photoId == photoId) {
            selectPhotoItem = photoItem;
            break;
         }
      }

      if (selectPhotoItem != null) {

         updateUI_SelectPhoto(new StructuredSelection(selectPhotoItem));
      }
   }

   private void onSelectionChanged(final ISelection selection) {

      if (_isInUpdate || selection == null) {
         return;
      }

      if (selection instanceof SelectionDeletedTours) {

         reloadViewer();

      } else if (selection instanceof final PhotoSelection photoSelection) {

         onPhotoSelection(photoSelection);
      }
   }

   private void onSortColumn(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            // update viewer with new sorting
            _photoComparator.setSortColumn(e.widget);
            _photoViewer.refresh();
         }
         updateUI_SelectPhoto(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            _photoViewer.getTable().dispose();

            createUI_10_PhotoViewer(_viewerContainer);

            // update UI
            _viewerContainer.layout();

            // update the viewer
            updateUI_SetViewerInput();
         }
         updateUI_SelectPhoto(selectionBackup);
      }
      _viewerContainer.setRedraw(true);

      _photoViewer.getTable().setFocus();

      return _photoViewer;
   }

   @Override
   public void reloadViewer() {

      loadAllPhotos();

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            updateUI_SetViewerInput();
         }
         updateUI_SelectPhoto(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, COLUMN_IMAGE_FILE_PATH);
      final int sortDirection = Util.getStateInt(_state, STATE_SORT_COLUMN_DIRECTION, PhotoComparator.ASCENDING);

      // update comparator
      _photoComparator.__sortColumnId = sortColumnId;
      _photoComparator.__sortDirection = sortDirection;
   }

   private void restoreState_WithUI() {

      /*
       * Select photo item
       */
      final long statePhotoId = Util.getStateLong(_state,
            STATE_SELECTED_PHOTO_ITEM,
            TourDatabase.ENTITY_IS_NOT_SAVED);

      if (statePhotoId != TourDatabase.ENTITY_IS_NOT_SAVED) {

         // select photo item by its ID
         for (final TourPhotoItem photoItem : _allPhotoItems) {

            if (photoItem.photoId == statePhotoId) {

               updateUI_SelectPhoto(new StructuredSelection(photoItem));

               enableActions();

               return;
            }
         }
      }

   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

      _state.put(STATE_SORT_COLUMN_ID, _photoComparator.__sortColumnId);
      _state.put(STATE_SORT_COLUMN_DIRECTION, _photoComparator.__sortDirection);

      /*
       * Selected photo item
       */
      long photoId = TourDatabase.ENTITY_IS_NOT_SAVED;
      final StructuredSelection selection = getViewerSelection();
      final Object firstItem = selection.getFirstElement();

      if (firstItem instanceof final TourPhotoItem photoItem) {
         photoId = photoItem.photoId;
      }
      _state.put(STATE_SELECTED_PHOTO_ITEM, photoId);
   }

   @Override
   public void setFocus() {
      _photoViewer.getTable().setFocus();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * Select and reveal tour photo item
    *
    * @param selection
    * @param checkedElements
    */
   private void updateUI_SelectPhoto(final ISelection selection) {

      _isInUpdate = true;
      {
         _photoViewer.setSelection(selection, true);

         final Table table = _photoViewer.getTable();
         table.showSelection();
      }
      _isInUpdate = false;
   }

   /**
    * Set the sort column direction indicator for a column.
    *
    * @param sortColumnId
    * @param isAscendingSort
    */
   private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

      final int swtDirection = sortDirection == PhotoComparator.ASCENDING ? SWT.UP : SWT.DOWN;

      final Table table = _photoViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(swtDirection);
   }

   private void updateUI_SetViewerInput() {

      _isInUpdate = true;
      {
         _photoViewer.setInput(new Object[0]);
      }
      _isInUpdate = false;
   }

}
