/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.geoCompare;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.EmptyContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.ReferenceTourManager;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.ui.views.tourCatalog.TourCatalogView_ComparedTour;
import net.tourbook.ui.views.tourCatalog.TourCatalogView_ReferenceTour;
import net.tourbook.ui.views.tourCatalog.TourCompareConfig;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class GeoCompareView extends ViewPart implements ITourViewer, IGeoCompareListener {

   public static final String            ID                            = "net.tourbook.ui.views.geoCompare.GeoCompareView"; //$NON-NLS-1$

   private static final int              DELAY_BEFORE_STARTING_COMPARE = 500;

   private static final int              UI_UPDATE_INTERVAL            = 1000;

   private static final String           STATE_IS_USE_APP_FILTER       = "STATE_IS_USE_APP_FILTER";                         //$NON-NLS-1$
   static final String                   STATE_DISTANCE_INTERVAL       = "STATE_DISTANCE_INTERVAL";                         //$NON-NLS-1$
   static final String                   STATE_GEO_ACCURACY            = "STATE_GEO_ACCURACY";                              //$NON-NLS-1$
   private static final String           STATE_SORT_COLUMN_DIRECTION   = "STATE_SORT_COLUMN_DIRECTION";                     //$NON-NLS-1$
   private static final String           STATE_SORT_COLUMN_ID          = "STATE_SORT_COLUMN_ID";                            //$NON-NLS-1$
   //
   static final int                      DEFAULT_DISTANCE_INTERVAL     = 100;
   static final int                      DEFAULT_GEO_ACCURACY          = 10_000;
   //
   private static final String           COLUMN_AVG_PULSE              = "avgPulse";                                        //$NON-NLS-1$
   private static final String           COLUMN_AVG_SPEED              = "avgSpeed";                                        //$NON-NLS-1$
   private static final String           COLUMN_GEO_DIFF               = "geoDiff";                                         //$NON-NLS-1$
   private static final String           COLUMN_GEO_DIFF_RELATIVE      = "geoDiffRelative";                                 //$NON-NLS-1$
   private static final String           COLUMN_SEQUENCE               = "sequence";                                        //$NON-NLS-1$
   private static final String           COLUMN_TOUR_START_DATE        = "tourStartDate";                                   //$NON-NLS-1$
   private static final String           COLUMN_TOUR_TITLE             = "tourTitle";                                       //$NON-NLS-1$
   //
   private static final IDialogSettings  _state                        = TourbookPlugin.getState(ID);
   private static final IPreferenceStore _prefStore                    = TourbookPlugin.getPrefStore();

   //
   private IPartListener2                 _partListener;
   private SelectionAdapter               _columnSortListener;
   private IPropertyChangeListener        _prefChangeListener;
   private ISelectionListener             _postSelectionListener;
   private ITourEventListener             _tourEventListener;
   private PostSelectionProvider          _postSelectionProvider;
   //
   private int                            _lastSelectionHash;
   //
   private AtomicInteger                  _workedTours             = new AtomicInteger();
   private AtomicInteger                  _runningId               = new AtomicInteger();
   //
   private long                           _workerExecutorId;
   //
   private boolean                        _isInUpdate;
   private long                           _lastUIUpdate;

   /**
    * Comparer items from the last comparison
    */
   private ArrayList<GeoPartComparerItem> _comparedTours           = new ArrayList<>();
   //
   private GeoPartComparerItem            _selectedComparerItem;
   //
   private int                            _compareData_NumGeoPartTours;
   private TourData                       _compareData_TourData;
   private long                           _compareData_TourId      = Long.MIN_VALUE;
   private int                            _compareData_FirstIndex;
   private int                            _compareData_LastIndex;
   private int[]                          _compareData_GeoGrid;
   private GeoPartItem                    _compareData_PreviousGeoPartItem;
   private long                           _compareData_RefId;
   private String                         _compareData_TourTitle;
   private boolean                        _compareData_IsUseAppFilter;
   //
   private long                           _lastCompare_TourId;
   private int                            _lastCompare_FirstIndex;
   private int                            _lastCompare_LastIndex;
   private int                            _lastCompare_DistanceInterval;
   private int                            _lastCompare_GeoAccuracy;
   private boolean                        _lastCompare_IsUseAppFilter;
   //
   private TableViewer                    _geoPartViewer;
   private ColumnManager                  _columnManager;
   private CompareResultComparator        _geoPartComparator       = new CompareResultComparator();
   //
   private int                            _distanceInterval;
   private int                            _geoAccuracy;
   private long                           _maxMinDiff;
   //
   private OpenDialogManager              _openDlgMgr              = new OpenDialogManager();
   private SlideoutGeoCompareOptions      _slideoutGeoCompareOptions;
   private GeoCompareState                _slideoutGeoCompareState = new GeoCompareState();
   //
   private PixelConverter                 _pc;
   //
   private ActionAppTourFilter            _actionAppTourFilter;
   private ActionOnOff                    _actionOnOff;
   private ActionGeoCompareOptions        _actionGeoCompareOptions;

   private final NumberFormat             _nf1                     = NumberFormat.getInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   /*
    * UI controls
    */
   private Composite _parent;
   private Composite _viewerContainer;
   //
   private PageBook  _pageBook;
   private Composite _pageContent;
   private Composite _pageMultipleTours;
   private Composite _pageNoData;
   private Label     _lblCompareStatus;
   //
   private Label     _lblNumTours;
   private Label     _lblNumGeoGrids;
   private Label     _lblNumSlices;
   private Label     _lblTitle;

   private class ActionAppTourFilter extends Action {

      public ActionAppTourFilter() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.GeoCompare_View_Action_AppFilter_Tooltip);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Filter));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Filter_Disabled));
      }

      @Override
      public void run() {
         onAction_AppFilter(isChecked());
      }
   }

   public class ActionGeoCompareOptions extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

//			return new SlideoutTourChartOptions(_parent, toolbar, TourChart.this, GRID_PREF_PREFIX);

         _slideoutGeoCompareOptions = new SlideoutGeoCompareOptions(_parent, toolbar, _state, GeoCompareView.this);

         return _slideoutGeoCompareOptions;
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ActionOnOff extends Action {

      public ActionOnOff() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.GeoCompare_View_Action_OnOff_Tooltip);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Turn_On));
      }

      @Override
      public void run() {
         onAction_OnOff(isChecked());
      }

      private void setIcon(final boolean isSelected) {

         // switch icon
         if (isSelected) {
            setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Turn_On));
         } else {
            setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Turn_Off));
         }
      }
   }

   private class CompareResultComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = COLUMN_GEO_DIFF;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final GeoPartComparerItem item1 = (GeoPartComparerItem) e1;
         final GeoPartComparerItem item2 = (GeoPartComparerItem) e2;

         boolean _isSortByTime = true;
         double rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnId) {

         case COLUMN_GEO_DIFF:
         case COLUMN_GEO_DIFF_RELATIVE:

            final long minDiffValue1 = item1.minDiffValue;
            final long minDiffValue2 = item2.minDiffValue;

            if (minDiffValue1 >= 0 && minDiffValue2 >= 0) {

               rc = minDiffValue1 - minDiffValue2;

            } else if (minDiffValue1 >= 0) {

               rc = -Integer.MAX_VALUE;

            } else if (minDiffValue2 >= 0) {

               rc = Integer.MAX_VALUE;

            } else {

               rc = minDiffValue1 - minDiffValue2;
            }

            break;

         case COLUMN_TOUR_START_DATE:

            // sorting by date is already set
            break;

         case COLUMN_AVG_PULSE:
            rc = item1.avgPulse - item2.avgPulse;
            break;

         case COLUMN_AVG_SPEED:
            rc = item1.avgSpeed - item2.avgSpeed;
            break;

         case COLUMN_TOUR_TITLE:
            rc = item1.tourTitle.compareTo(item2.tourTitle);
            break;

         case TableColumnFactory.MOTION_ALTIMETER_ID:
            rc = item1.avgAltimeter - item2.avgAltimeter;
            break;

         case TableColumnFactory.MOTION_DISTANCE_ID:
            rc = item1.distance - item2.distance;
            break;

         case TableColumnFactory.TIME__COMPUTED_MOVING_TIME_ID:
            rc = item1.movingTime - item2.movingTime;
            break;

         case TableColumnFactory.TIME__DEVICE_ELAPSED_TIME_ID:
            rc = item1.elapsedTime - item2.elapsedTime;
            break;

         default:
            _isSortByTime = true;
         }

         if (rc == 0 && _isSortByTime) {
            rc = item1.tourStartTimeMS - item2.tourStartTimeMS;
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

   private class CompareResultProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _comparedTours.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   public enum InvalidData {

      NoGeoData, MultipleTours
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == GeoCompareView.this) {
               setState_StopComparing();
            }
         }

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

            if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

               recompareTours();

            } else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

               // map options can have be changed
               _slideoutGeoCompareOptions.restoreState();

            } else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

//					updateUI_GeoAccuracy();
            }
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   /**
    * Listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            if (isIgnorePart(part)) {
               return;
            }

            onSelectionChanged(selection);
         }
      };
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (isIgnorePart(part)) {
               return;
            }

            if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView(final InvalidData invalidData) {

      setState_StopComparing();

      switch (invalidData) {

      case MultipleTours:
         _pageBook.showPage(_pageMultipleTours);
         break;

      case NoGeoData:
      default:
         _pageBook.showPage(_pageNoData);
         break;
      }
   }

   /**
    * Close all opened dialogs except the opening dialog.
    *
    * @param openingDialog
    */
   public void closeOpenedDialogs(final IOpeningDialog openingDialog) {
      _openDlgMgr.closeOpenedDialogs(openingDialog);
   }

   /**
    * @param tourData
    * @param leftIndex
    * @param rightIndex
    * @param refId
    *           Reference tour id or <code>-1</code> when not available
    */
   private void compare_10_Compare(final TourData tourData,
                                   final int leftIndex,
                                   final int rightIndex,
                                   final long refId) {

      if (GeoCompareManager.isGeoComparing() == false) {

         // ignore slider position
         return;
      }

      /*
       * !!! This must be very early because the comparison is running with a delay which could
       * cause that an old is running !!!
       */
      final int runnableRunningId = _runningId.incrementAndGet();

      /*
       * Show no data page
       */
      final double[] latSerie = tourData.latitudeSerie;
      if (latSerie == null) {

         clearView(InvalidData.NoGeoData);

         return;
      }

      /*
       * Validate first/last indices
       */
      int firstIndex = leftIndex < rightIndex ? leftIndex : rightIndex;
      int lastIndex = leftIndex > rightIndex ? leftIndex : rightIndex;
      if (firstIndex < 0) {
         firstIndex = 0;
      }
      if (lastIndex > latSerie.length) {
         lastIndex = latSerie.length;
      }

      final long tourId = tourData.getTourId();

      // skip same data and continue current comparison
      if (_compareData_TourId == tourId
            && _compareData_FirstIndex == leftIndex
            && _compareData_LastIndex == rightIndex
            && _lastCompare_GeoAccuracy == _geoAccuracy
            && _lastCompare_DistanceInterval == _distanceInterval
            && _lastCompare_IsUseAppFilter == _compareData_IsUseAppFilter) {

         return;
      }

      /*
       * Keep index AFTER checking otherwise wrong values are used in the runnable !!!
       */
      final int compareFirstIndex = firstIndex;
      final int compareLastIndex = lastIndex;

      /*
       * New data should be compared
       */
      GeoPartTourLoader.stopLoading(_compareData_PreviousGeoPartItem);

      updateUI_GeoItem(null);

      // delay tour comparator, moving the slider can occur very often
      _parent.getDisplay().timerExec(DELAY_BEFORE_STARTING_COMPARE, new Runnable() {

         private int __runningId = runnableRunningId;

         @Override
         public void run() {

            if (_parent.isDisposed()) {
               return;
            }

            final int currentId = _runningId.get();

            if (__runningId != currentId) {

               // a newer runnable is created

               return;
            }

            _compareData_TourId = tourId;
            _compareData_TourData = tourData;
            _compareData_RefId = refId;
            _compareData_FirstIndex = compareFirstIndex;
            _compareData_LastIndex = compareLastIndex;

            compare_20_SetupComparing();
         }
      });
   }

   private void compare_20_SetupComparing() {

      // 1. get geo grid from lat/lon first/last index
      _compareData_GeoGrid = _compareData_TourData.computeGeo_Grid(
            _compareData_FirstIndex,
            _compareData_LastIndex);

      if (_compareData_GeoGrid == null) {

         _pageBook.showPage(_pageNoData);

         return;
      }

      _pageBook.showPage(_pageContent);

      /*
       * Update UI
       */
      _comparedTours.clear();

      // reset max diff
      _maxMinDiff = -1;

      updateUI_Viewer();

      updateUI_State_Progress(-1, -1);
      updateUI_HideFalsePositive();

      _compareData_TourTitle = TourManager.getTourTitleDetailed(_compareData_TourData);

      compare_30_StartComparing();
   }

   /**
    * Start comparing with data from geo compare fields
    */
   private void compare_30_StartComparing() {

      // check if comparing is already finished
      if (_lastCompare_TourId == _compareData_TourId
            && _lastCompare_FirstIndex == _compareData_FirstIndex
            && _lastCompare_LastIndex == _compareData_LastIndex
            && _lastCompare_GeoAccuracy == _geoAccuracy
            && _lastCompare_DistanceInterval == _distanceInterval) {

         // comparing is finished for the requested data

         return;
      }

      /*
       * Create geo data which should be compared
       */
      final NormalizedGeoData normalizedGeoData = _compareData_TourData.computeGeo_NormalizeLatLon(
            _compareData_FirstIndex,
            _compareData_LastIndex,
            _geoAccuracy,
            _distanceInterval);

      // load tour id's in the geo parts
      final GeoPartItem newGeoPartItem = GeoPartTourLoader.loadToursFromGeoParts(
            _compareData_GeoGrid,
            normalizedGeoData,
            _compareData_IsUseAppFilter,
            _compareData_PreviousGeoPartItem,
            this);

      newGeoPartItem.refId = _compareData_RefId;

      _compareData_PreviousGeoPartItem = newGeoPartItem;

      /*
       * Set slideout info
       */
      _slideoutGeoCompareState = new GeoCompareState();

      _slideoutGeoCompareState.numSlices = _compareData_LastIndex - _compareData_FirstIndex;
      _slideoutGeoCompareState.numGeoGrids = _compareData_GeoGrid.length;
      _slideoutGeoCompareState.normalizedDistance = normalizedGeoData.normalizedDistance;

      _slideoutGeoCompareOptions.updateUI_StateValues(_slideoutGeoCompareState);
      updateUI_StateValues();

   }

   void compare_40_CompareTours(final GeoPartItem geoPartItem) {

      _compareData_NumGeoPartTours = geoPartItem.tourIds.length;

      if (_compareData_NumGeoPartTours == 0) {

         // update UI
         Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {

               if (_parent.isDisposed()) {
                  return;
               }

               // this can happen when the tour filter is active and no tours are found -> show empty result

               _comparedTours.clear();

               updateUI_State_Progress(0, 0);
               updateUI_Viewer();

               updateUI_GeoItem(geoPartItem);
            }
         });

         return;
      }

      final long workerExecutorId[] = { 0 };

      _workedTours.set(0);

      _workerExecutorId = geoPartItem.executorId;
      workerExecutorId[0] = _workerExecutorId;

      GeoCompareManager.compareGeoTours(geoPartItem, this);

      // update UI
      Display.getDefault().asyncExec(new Runnable() {
         @Override
         public void run() {

            if (_parent.isDisposed()) {
               return;
            }

            if (workerExecutorId[0] != _workerExecutorId) {
               // skip old tasks
               return;
            }

            updateUI_GeoItem(geoPartItem);
         }
      });

   }

   void compare_50_TourIsCompared(final GeoPartComparerItem comparerItem) {

      final GeoPartItem geoPartItem = comparerItem.geoPartItem;

      if (geoPartItem.isCanceled || geoPartItem.executorId != _workerExecutorId) {
         return;
      }

      _comparedTours = geoPartItem.comparedTours;

      final int workedTours = _workedTours.incrementAndGet();

      final long now = System.currentTimeMillis();

      // update UI not too often until comparison is done
      if (now - _lastUIUpdate < UI_UPDATE_INTERVAL && workedTours != _compareData_NumGeoPartTours) {
         return;
      }

      // get previous selected item
      final GeoPartComparerItem[] reselectedItem = { null };
      if (geoPartItem.isReselectedInUI == false) {

         geoPartItem.isReselectedInUI = true;

         if (_selectedComparerItem != null) {

            for (final GeoPartComparerItem reselectComparerItem : _comparedTours) {

               if (reselectComparerItem.tourId == _selectedComparerItem.tourId) {
                  reselectedItem[0] = reselectComparerItem;
               }
            }
         }
      }

      // reset paused time
      _lastUIUpdate = now;

      Display.getDefault().asyncExec(new Runnable() {
         @Override
         public void run() {

            if (_parent.isDisposed()) {
               return;
            }

            updateUI_State_Progress(workedTours, _compareData_NumGeoPartTours);

            // fire geo part compare result
            GeoCompareManager.fireEvent(
                  GeoCompareEventId.COMPARE_GEO_PARTS,
                  comparerItem.geoPartItem,
                  GeoCompareView.this);

            if (workedTours == _compareData_NumGeoPartTours) {
               compare_60_AllIsCompared(geoPartItem);
            }

            updateUI_Viewer();

            // reselect previous selection
            if (reselectedItem[0] != null) {

               _geoPartViewer.setSelection(new StructuredSelection(reselectedItem), true);
               _geoPartViewer.getTable().showSelection();

//					// focus can have changed when resorted, set focus to the selected item
//					int selectedIndex = 0;
//					final Table table = _profileViewer.getTable();
//					final TableItem[] items = table.getItems();
//					for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
//
//						final TableItem tableItem = items[itemIndex];
//
//						if (tableItem.getData() == selectedProfile) {
//							selectedIndex = itemIndex;
//						}
//					}
//					table.setSelection(selectedIndex);
//					table.showSelection();

            }
         }

      });
   }

   private void compare_60_AllIsCompared(final GeoPartItem geoPartItem) {

      /*
       * Keep state after compare is done
       */
      _lastCompare_TourId = _compareData_TourId;
      _lastCompare_FirstIndex = _compareData_FirstIndex;
      _lastCompare_LastIndex = _compareData_LastIndex;
      _lastCompare_GeoAccuracy = _geoAccuracy;
      _lastCompare_DistanceInterval = _distanceInterval;
      _lastCompare_IsUseAppFilter = _compareData_IsUseAppFilter;

      /*
       * Get max mindiff value
       */
      _maxMinDiff = 0;
      for (final GeoPartComparerItem comparerItem : geoPartItem.comparedTours) {

         if (comparerItem.minDiffValue > _maxMinDiff) {
            _maxMinDiff = comparerItem.minDiffValue;
         }
      }

      // make sure the selection is visible
//		if (_selectedComparerItem != null) {
//
////			_geoPartViewer.setSelection(new StructuredSelection(_selectedComparerItem), true);
      _geoPartViewer.getTable().showSelection();
//		}

      updateUI_HideFalsePositive();
   }

   /**
    * @param tourData
    * @return Returns <code>true</code> when tour comparing could be started, otherwise
    *         <code>false</code>
    */
   private boolean compareWholeTour(final TourData tourData) {

      // is currently disabled because it is slowing down
      return false;

//		if (tourData == null) {
//			return false;
//		}
//
//		// skip manual tours
//		if (tourData.isManualTour()) {
//			return false;
//		}
//
//		// compare the whole tour from 0 to max time slices
//		final int numTimeSlices = tourData.timeSerie.length - 1;
//		if (numTimeSlices < 1) {
//			return false;
//		}
//
//		/*
//		 * Convert real ref tour into a geo compare ref tour that the behaviour is the same, however
//		 * this will disable features in the tour compare chart but this is already very complex.
//		 */
//
//		final long geoCompareRefId = ReferenceTourManager.createGeoCompareRefTour(
//				tourData,
//				0,
//				numTimeSlices);
//
//		compare_10_Compare(
//				tourData,
//				0,
//				numTimeSlices,
//				geoCompareRefId);
//
//		return true;
   }

   private void createActions() {

      _actionAppTourFilter = new ActionAppTourFilter();
      _actionOnOff = new ActionOnOff();
      _actionGeoCompareOptions = new ActionGeoCompareOptions();
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);
      restoreState_BeforeUI();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      createUI(parent);
      createActions();

      fillToolbar();

      addPartListener();
      addPrefListener();
      addTourEventListener();
      addSelectionListener();
      GeoCompareManager.addGeoCompareEventListener(this);

      // set selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      restoreState();

      restoreSelection();

      _pageBook.showPage(_pageNoData);
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = UI.createUI_PageNoData(_pageBook, Messages.GeoCompare_View_PageText_NoTourWithGeoData);
      _pageMultipleTours = UI.createUI_PageNoData(
            _pageBook,
            Messages.GeoCompare_View_PageText_MultipleToursNotSupported);

      _pageContent = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_pageContent);
      {
         final Composite container = new Composite(_pageContent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
         {
            createUI_10_Comparator(container);

            _viewerContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
            GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
            {
               createUI_80_TableViewer(_viewerContainer);
            }
         }
      }
   }

   private void createUI_10_Comparator(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         {
            /*
             * Label: Tour title
             */

            _lblTitle = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblTitle);
         }

//			createUI_50_HideFalsePositive(container);

         createUI_30_Info(container);

         {
            /*
             * Label: Compare status
             */

            _lblCompareStatus = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblCompareStatus);
         }
      }
   }

   private void createUI_30_Info(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
//				.grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(6).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

      {
         {
            /*
             * Number of tours
             */
            {
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.GeoCompare_View_Label_PossibleTours);

            }
            {
               _lblNumTours = new Label(container, SWT.NONE);
               _lblNumTours.setText(UI.EMPTY_STRING);
               GridDataFactory
                     .fillDefaults()
                     .grab(true, false)
                     //							.align(SWT.END, SWT.FILL)
                     .applyTo(_lblNumTours);
            }
         }
         {
            /*
             * Number of geo parts
             */
            {
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.GeoCompare_View_Label_GeoParts);
               label.setToolTipText(Messages.GeoCompare_View_Label_GeoParts_Tooltip);

            }
            {
               _lblNumGeoGrids = new Label(container, SWT.NONE);
               _lblNumGeoGrids.setText(UI.EMPTY_STRING);
               GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumGeoGrids);
            }
         }
         {
            /*
             * Number of time slices
             */
            {
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.GeoCompare_View_Label_TimeSlices);

            }
            {
               _lblNumSlices = new Label(container, SWT.NONE);
               _lblNumSlices.setText(UI.EMPTY_STRING);
               GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumSlices);
            }
         }
      }
   }

//	private void createUI_50_HideFalsePositive(final Composite parent) {
//
//		final SelectionAdapter falsePositiveListener = new SelectionAdapter() {
//			@Override
//			public void widgetSelected(final SelectionEvent e) {
//				onChange_HideFalsePositive();
//			}
//		};
//
//		/*
//		 * Hide false positive tours
//		 */
//		{
//			/*
//			 * Checkbox: live update
//			 */
//			_chkHideFalsePositive = new Button(parent, SWT.CHECK);
//			_chkHideFalsePositive.setText("&Hide false positive");
//			_chkHideFalsePositive.setToolTipText(
//					"Hide tours which are found but do not contain the requested tour part");
//			_chkHideFalsePositive.addSelectionListener(falsePositiveListener);
//		}
//
//		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory
//				.fillDefaults()
//				.grab(true, false)
//				.indent(_pc.convertHorizontalDLUsToPixels(6), SWT.DEFAULT)
//				.applyTo(container);
//		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		{
//			{
//				/*
//				 * Scale: False positive in %
//				 */
//				_scaleHideFalsePositive = new Scale(container, SWT.NONE);
//				_scaleHideFalsePositive.setIncrement(1);
//				_scaleHideFalsePositive.setPageIncrement(10);
//				_scaleHideFalsePositive.setMinimum(1);
//				_scaleHideFalsePositive.setMaximum(100);
//				_scaleHideFalsePositive.addSelectionListener(falsePositiveListener);
//				GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleHideFalsePositive);
//			}
//			{
//				/*
//				 * Label: %
//				 */
//				_lblHideFalsePositiveValue = new Label(container, SWT.NONE);
//				GridDataFactory
//						.fillDefaults()
//						.align(SWT.FILL, SWT.CENTER)
//						.hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
//						.applyTo(_lblHideFalsePositiveValue);
//			}
//		}
//	}

   private void createUI_80_TableViewer(final Composite parent) {

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
            onGeoPart_Select(event);
         }
      });
      /*
       * create table viewer
       */
      _geoPartViewer = new TableViewer(table);

      _columnManager.createColumns(_geoPartViewer);

      _geoPartViewer.setUseHashlookup(true);
      _geoPartViewer.setContentProvider(new CompareResultProvider());
      _geoPartViewer.setComparator(_geoPartComparator);

      _geoPartViewer.addSelectionChangedListener(new ISelectionChangedListener() {

         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelect_ComparerItem(event);
         }
      });

      _geoPartViewer.addDoubleClickListener(new IDoubleClickListener() {

         @Override
         public void doubleClick(final DoubleClickEvent event) {
//				onBookmark_Rename(true);
         }
      });

      _geoPartViewer.getTable().addKeyListener(new KeyListener() {

         @Override
         public void keyPressed(final KeyEvent e) {

            switch (e.keyCode) {

            case SWT.DEL:
//					onBookmark_Delete();
               break;

            case SWT.F2:
//					onBookmark_Rename(false);
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

      createUI_82_ContextMenu();
   }

   /**
    * Create the view context menus
    */
   private void createUI_82_ContextMenu() {

      final Table table = _geoPartViewer.getTable();

      _columnManager.createHeaderContextMenu(table, new EmptyContextMenuProvider());
   }

   private void defineAllColumns() {

      defineColumn_00_SequenceNumber();
      defineColumn_GeoDiff();
      defineColumn_GeoDiff_Relative();
      defineColumn_Time_TourStartDate();
      defineColumn_Motion_AvgSpeed();
      defineColumn_Motion_Altimeter();
      defineColumn_Motion_Distance();

      defineColumn_Body_AvgPulse();

      defineColumn_Time_MovingTime();
      defineColumn_Time_ElapsedTime();

      defineColumn_Tour_Type();
      defineColumn_Tour_Title();

//		defineColumn_80_StartIndex();
//		defineColumn_82_EndIndex();
//		defineColumn_84_IndexDiff();
   }

   private void defineColumn_00_SequenceNumber() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_SEQUENCE, SWT.TRAIL);

      colDef.setColumnLabel(Messages.GeoCompare_View_Column_SequenceNumber_Label);
      colDef.setColumnHeaderText(Messages.GeoCompare_View_Column_SequenceNumber_Header);
      colDef.setColumnHeaderToolTipText(Messages.GeoCompare_View_Column_SequenceNumber_Label);

      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(6));

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final int indexOf = _geoPartViewer.getTable().indexOf((TableItem) cell.getItem());

            cell.setText(Integer.toString(indexOf + 1));
         }
      });

   }

   /**
    * column: average pulse
    */
   private void defineColumn_Body_AvgPulse() {

      final ColumnDefinition colDef = TableColumnFactory.BODY_AVG_PULSE.createColumn(_columnManager, _pc);

      // overwrite column id to identify the column when table is sorted
      colDef.setColumnId(COLUMN_AVG_PULSE);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

            colDef.printDetailValue(cell, item.avgPulse);
         }
      });
   }

   /**
    * Column: Geo Diff
    */
   private void defineColumn_GeoDiff() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_GEO_DIFF, SWT.TRAIL);

      colDef.setColumnLabel(Messages.GeoCompare_View_Column_GeoDiff_Label);
      colDef.setColumnHeaderText(Messages.GeoCompare_View_Column_GeoDiff_Header);
      colDef.setColumnHeaderToolTipText(Messages.GeoCompare_View_Column_GeoDiff_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();
            final long minDiffValue = item.minDiffValue;

            final String minDiffText = minDiffValue == -2
                  ? UI.ELLIPSIS
                  : minDiffValue == -1
                        ? Messages.App_Label_NotAvailable_Shortcut
                        : Long.toString(minDiffValue);

            cell.setText(minDiffText);
         }
      });
   }

   /**
    * Column: Geo Diff
    */
   private void defineColumn_GeoDiff_Relative() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager,
            COLUMN_GEO_DIFF_RELATIVE,
            SWT.TRAIL);

      colDef.setColumnLabel(Messages.GeoCompare_View_Column_GeoDiff_Relative_Label);
      colDef.setColumnHeaderText(Messages.GeoCompare_View_Column_GeoDiff_Relative_Header);
      colDef.setColumnHeaderToolTipText(Messages.GeoCompare_View_Column_GeoDiff_Relative_Tooltip);

      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();
            final long minDiffValue = item.minDiffValue;

            final String minDiffText = minDiffValue == -2 || _maxMinDiff == -1
                  ? UI.ELLIPSIS
                  : minDiffValue == -1
                        ? Messages.App_Label_NotAvailable_Shortcut
                        : _nf1.format((float) minDiffValue / _maxMinDiff * 100);

            cell.setText(minDiffText);
         }
      });
   }

   /**
    * Column: Vertical speed (VAM average ascent speed)
    */
   private void defineColumn_Motion_Altimeter() {

      final TableColumnDefinition colDef = TableColumnFactory.MOTION_ALTIMETER.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

            final double value = item.avgAltimeter;

            colDef.printDetailValue(cell, value);
         }
      });
   }

   /**
    * column: Tour start date
    */
   private void defineColumn_Motion_AvgSpeed() {

      final ColumnDefinition colDef = TableColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager, _pc);

      // overwrite column id to identify the column when table is sorted
      colDef.setColumnId(COLUMN_AVG_SPEED);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

            final double speed = item.avgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            colDef.printDetailValue(cell, speed);
         }
      });
   }

   /**
    * Column: Distance
    */
   private void defineColumn_Motion_Distance() {

      final TableColumnDefinition colDef = TableColumnFactory.MOTION_DISTANCE.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

            final double value = item.distance
                  / 1000.0
                  / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            colDef.printDetailValue(cell, value);
         }
      });
   }

   /**
    * Column: Elapsed time (h)
    */
   private void defineColumn_Time_ElapsedTime() {

      final TableColumnDefinition colDef = TableColumnFactory.TIME__DEVICE_ELAPSED_TIME.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

            final long value = item.elapsedTime;

            colDef.printLongValue(cell, value, true);
         }
      });
   }

   /**
    * Column: Moving time (h)
    */
   private void defineColumn_Time_MovingTime() {

      final TableColumnDefinition colDef = TableColumnFactory.TIME__COMPUTED_MOVING_TIME.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

            final long value = item.movingTime;

            colDef.printLongValue(cell, value, true);
         }
      });
   }

   /**
    * column: Tour start date
    */
   private void defineColumn_Time_TourStartDate() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_START_DATE.createColumn(_columnManager, _pc);

      // overwrite column id to identify the column when table is sorted
      colDef.setColumnId(COLUMN_TOUR_START_DATE);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

            final ZonedDateTime tourStartTime = item.tourStartTime;
            cell.setText(
                  tourStartTime == null
                        ? UI.EMPTY_STRING
                        : tourStartTime.format(TimeTools.Formatter_Date_S));
         }
      });
   }

   /**
    * Column: Tour title
    */
   private void defineColumn_Tour_Title() {

      final ColumnDefinition colDef = TableColumnFactory.TOUR_TITLE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setColumnId(COLUMN_TOUR_TITLE);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

            cell.setText(item.tourTitle);
         }
      });
   }

   /**
    * Column: Tour type image
    */
   private void defineColumn_Tour_Type() {

      final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourType tourType = ((GeoPartComparerItem) cell.getElement()).tourType;

            if (tourType == null) {
               cell.setImage(TourTypeImage.getTourTypeImage(TourDatabase.ENTITY_IS_NOT_SAVED));
            } else {

               final long tourTypeId = tourType.getTypeId();
               final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourTypeId);

               cell.setImage(tourTypeImage);
            }
         }
      });
   }

//	/**
//	 * Column: Start index
//	 */
//	private void defineColumn_80_StartIndex() {
//
//		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "startIndex", SWT.TRAIL);
//
//		colDef.setColumnLabel("Start Idx");
//		colDef.setColumnHeaderText("Start Idx");
//
//		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));
//
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();
//
//				cell.setText(Integer.toString(item.tourFirstIndex));
//			}
//		});
//	}
//
//	/**
//	 * Column: End index
//	 */
//	private void defineColumn_82_EndIndex() {
//
//		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "endIndex", SWT.TRAIL);
//
//		colDef.setColumnLabel("End Idx");
//		colDef.setColumnHeaderText("End Idx");
//
//		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));
//
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();
//
//				cell.setText(Integer.toString(item.tourLastIndex));
//			}
//		});
//	}
//
//	/**
//	 * Column: End index
//	 */
//	private void defineColumn_84_IndexDiff() {
//
//		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "indexDiff", SWT.TRAIL); //$NON-NLS-1$
//
//		colDef.setColumnLabel(Messages.GeoCompare_View_Column_IndexDiff);
//		colDef.setColumnHeaderText(Messages.GeoCompare_View_Column_IndexDiff);
//
//		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));
//
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();
//
//				cell.setText(Integer.toString(item.tourLastIndex - item.tourFirstIndex));
//			}
//		});
//	}

   @Override
   public void dispose() {

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      getSite().getPage().removePartListener(_partListener);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);
      GeoCompareManager.removeGeoCompareListener(this);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void enableControls() {

      final boolean isCompareEnabled = GeoCompareManager.isGeoComparing();

      _actionAppTourFilter.setEnabled(isCompareEnabled);

//		_geoPartViewer.getTable().setEnabled(_isCompareEnabled);
   }

//	private void enableControls_HideFalsePositive() {
//
//		final boolean isHideFalsePositive = _chkHideFalsePositive.getSelection();
//		final boolean isShowHideFalsePositive = isHideFalsePositive && _isComparingDone;
//
//		_chkHideFalsePositive.setEnabled(_isComparingDone);
//
//		_lblHideFalsePositiveValue.setEnabled(isShowHideFalsePositive);
//		_scaleHideFalsePositive.setEnabled(isShowHideFalsePositive);
//	}

   private void fillToolbar() {

      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionOnOff);
      tbm.add(_actionAppTourFilter);
      tbm.add(_actionGeoCompareOptions);

      tbm.update(true);
   }

   private void fireSelection(final ISelection selection) {

      // fire selection for the selected geo part tour
      _postSelectionProvider.setSelection(selection);
   }

   @Override
   public void geoCompareEvent(final IWorkbenchPart part, final GeoCompareEventId eventId, final Object eventData) {

      if (part == GeoCompareView.this) {
         return;
      }

      switch (eventId) {
      case COMPARE_GEO_PARTS:
         break;

      case SET_COMPARING_ON:
      case SET_COMPARING_OFF:

         onAction_OnOff(eventId == GeoCompareEventId.SET_COMPARING_ON);
      }
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   GeoCompareState getSlideoutState() {
      return _slideoutGeoCompareState;
   }

   /**
    * @param sortColumnId
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _geoPartViewer.getTable().getColumns();

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
      return _geoPartViewer;
   }

   private StructuredSelection getViewerSelection() {

      return (StructuredSelection) _geoPartViewer.getSelection();
   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _pc = new PixelConverter(parent);

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSelect_SortColumn(e);
         }
      };
   }

   private boolean isIgnorePart(final IWorkbenchPart part) {

      // ignore own part
      if (part == GeoCompareView.this) {
         return true;
      }

      // ignore other parts to prevent geo part comparing !!!
      if (part instanceof TourCatalogView_ComparedTour || part instanceof TourCatalogView_ReferenceTour) {
         return true;
      }

      return false;
   }

   private void onAction_AppFilter(final boolean isSelected) {

      _compareData_IsUseAppFilter = isSelected;

      recompareTours();
   }

   /**
    * @param isOn
    *           Turn comparing ON/OFF
    * @param isDoRecomparing
    */
   private void onAction_OnOff(final boolean isOn) {

      _actionOnOff.setIcon(isOn);

      if (isOn) {

         // enable comparing

         recompareTours();

      } else {

         // cancel comparing

         setState_StopComparing();
         updateUI_State_CancelComparing();
      }

      GeoCompareManager.setGeoComparing(isOn, this);

      enableControls();
   }

   void onChange_CompareParameter() {

      _geoAccuracy = Util.getStateInt(
            _state,
            GeoCompareView.STATE_GEO_ACCURACY,
            GeoCompareView.DEFAULT_GEO_ACCURACY);

      _distanceInterval = Util.getStateInt(
            _state,
            GeoCompareView.STATE_DISTANCE_INTERVAL,
            GeoCompareView.DEFAULT_DISTANCE_INTERVAL);

      if (_lastCompare_GeoAccuracy != _geoAccuracy || _lastCompare_DistanceInterval != _distanceInterval) {

         // accuracy is modified

         recompareTours();
      }
   }

//	private void onChange_HideFalsePositive() {
//
//		enableControls_HideFalsePositive();
//		updateUI_HideFalsePositive();
//	}

   private void onGeoPart_Select(final Event event) {

      if (_isInUpdate) {
         return;
      }
   }

   private void onSelect_ComparerItem(final SelectionChangedEvent event) {

      final ISelection selection = event.getSelection();
      final Object firstElement = ((StructuredSelection) selection).getFirstElement();

      _selectedComparerItem = (GeoPartComparerItem) firstElement;

      fireSelection(selection);
   }

   private void onSelect_SortColumn(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            // update viewer with new sorting
            _geoPartComparator.setSortColumn(e.widget);
            _geoPartViewer.refresh();
         }
         updateUI_SelectCompareItem(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (selection == null) {
         return;
      }

      final int selectionHash = selection.hashCode();
      if (_lastSelectionHash == selectionHash) {

         /*
          * Last selection has not changed, this can occure when the app lost the focus and got the
          * focus again.
          */
         return;
      }

      _lastSelectionHash = selectionHash;

      if (selection instanceof SelectionChartInfo) {

         final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

         if (GeoCompareManager.isGeoComparing() == false) {
            return;
         }

         TourData tourData = null;

         final Chart chart = chartInfo.getChart();
         if (chart instanceof TourChart) {

            final TourChart tourChart = (TourChart) chart;
            tourData = tourChart.getTourData();
         }

         if (tourData != null && tourData.isMultipleTours()) {

            // multiple tours are selected

         } else {

            // use old behaviour

            final ChartDataModel chartDataModel = chartInfo.chartDataModel;
            if (chartDataModel != null) {

               final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
               if (tourId instanceof Long) {

                  tourData = TourManager.getInstance().getTourData((Long) tourId);
                  if (tourData == null) {

                     // tour is not in the database, try to get it from the raw data manager

                     final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
                     tourData = rawData.get(tourId);
                  }
               }
            }
         }

         if (tourData != null) {

            if (tourData.isMultipleTours()) {

               // multiple tours are not supported

               clearView(InvalidData.MultipleTours);

            } else {

               final long geoCompareRefId = ReferenceTourManager.createGeoCompareRefTour(
                     tourData,
                     chartInfo.leftSliderValuesIndex,
                     chartInfo.rightSliderValuesIndex);

               compare_10_Compare(
                     tourData,
                     chartInfo.leftSliderValuesIndex,
                     chartInfo.rightSliderValuesIndex,
                     geoCompareRefId);
            }
         }

      } else if (selection instanceof SelectionChartXSliderPosition) {

         final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;
         final Chart chart = xSliderPos.getChart();
         if (chart == null) {
            return;
         }

         final ChartDataModel chartDataModel = chart.getChartDataModel();

         final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
         if (tourId instanceof Long) {

            final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
            if (tourData != null) {

               if (tourData.isMultipleTours()) {

                  // multiple tours are not supported

                  clearView(InvalidData.MultipleTours);

               } else {

                  final int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
                  int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();

                  rightSliderValueIndex =
                        rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
                              ? leftSliderValueIndex
                              : rightSliderValueIndex;

                  final long geoCompareRefId = ReferenceTourManager.createGeoCompareRefTour(
                        tourData,
                        leftSliderValueIndex,
                        rightSliderValueIndex);

                  compare_10_Compare(
                        tourData,
                        leftSliderValueIndex,
                        rightSliderValueIndex,
                        geoCompareRefId);
               }
            }
         }

      } else if (selection instanceof SelectionTourCatalogView) {

         showRefTour(((SelectionTourCatalogView) selection).getRefId());

      } else if (selection instanceof SelectionTourData) {

         final SelectionTourData tourDataSelection = (SelectionTourData) selection;

         final TourData selectionTourData = tourDataSelection.getTourData();

         compareWholeTour(selectionTourData);

      } else if (selection instanceof SelectionTourId) {

         final SelectionTourId selectionTourId = (SelectionTourId) selection;
         final Long tourId = selectionTourId.getTourId();

         compareWholeTour(TourManager.getInstance().getTourData(tourId));

      } else if (selection instanceof SelectionTourIds) {

         // only 1 tour can be compared

         final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();

         if (tourIds != null) {

            for (final Long tourId : tourIds) {

               final TourData tourData = TourManager.getInstance().getTourData(tourId);
               if (compareWholeTour(tourData)) {
                  break;
               }
            }
         }

      } else if (selection instanceof StructuredSelection) {

         final Object firstElement = ((StructuredSelection) selection).getFirstElement();

         if (firstElement instanceof TVICatalogComparedTour) {

            showRefTour(((TVICatalogComparedTour) firstElement).getRefId());

         } else if (firstElement instanceof TVICompareResultComparedTour) {

            showRefTour(((TVICompareResultComparedTour) firstElement).refTour.refId);
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView(InvalidData.NoGeoData);
      }

   }

   private void recompareTours() {

      if (_compareData_GeoGrid != null && GeoCompareManager.isGeoComparing()) {

         compare_30_StartComparing();
      }
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         _geoPartViewer.getTable().dispose();

         createUI_80_TableViewer(_viewerContainer);
         _viewerContainer.layout();

         // update the viewer
         reloadViewer();
      }
      _viewerContainer.setRedraw(true);

      return _geoPartViewer;
   }

   @Override
   public void reloadViewer() {

      updateUI_Viewer();
   }

   private void restoreSelection() {

      // try to use selection from selection service
      onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

      if (_compareData_TourData == null) {

         _pageBook.showPage(_pageNoData);

         // a tour is not displayed, find a tour provider which provides a tour
         Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {

               // validate widget
               if (_pageBook.isDisposed()) {
                  return;
               }

               /*
                * check if tour was set from a selection provider
                */
               if (_compareData_TourData != null) {
                  return;
               }

               final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
               if (selectedTours != null && selectedTours.size() > 0) {

                  for (final TourData tourData : selectedTours) {

                     if (compareWholeTour(tourData)) {
                        break;
                     }
                  }
               }
            }
         });
      }
   }

   private void restoreState() {

      final boolean isCompareEnabled = GeoCompareManager.isGeoComparing();

      _actionOnOff.setIcon(isCompareEnabled);
      _actionOnOff.setChecked(isCompareEnabled);

      _compareData_IsUseAppFilter = Util.getStateBoolean(_state, STATE_IS_USE_APP_FILTER, true);
      _actionAppTourFilter.setChecked(_compareData_IsUseAppFilter);

      _geoAccuracy = Util.getStateInt(_state, STATE_GEO_ACCURACY, DEFAULT_GEO_ACCURACY);
      _distanceInterval = Util.getStateInt(_state, STATE_DISTANCE_INTERVAL, DEFAULT_DISTANCE_INTERVAL);

      enableControls();
   }

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, COLUMN_GEO_DIFF);
      final int sortDirection = Util.getStateInt(
            _state,
            STATE_SORT_COLUMN_DIRECTION,
            CompareResultComparator.ASCENDING);

      // update comparator
      _geoPartComparator.__sortColumnId = sortColumnId;
      _geoPartComparator.__sortDirection = sortDirection;
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_IS_USE_APP_FILTER, _compareData_IsUseAppFilter);

      _state.put(STATE_SORT_COLUMN_ID, _geoPartComparator.__sortColumnId);
      _state.put(STATE_SORT_COLUMN_DIRECTION, _geoPartComparator.__sortDirection);

      _columnManager.saveState(_state);
   }

   @Override
   public void setFocus() {

      _geoPartViewer.getTable().setFocus();
   }

   private void setState_StopComparing() {

      GeoPartTourLoader.stopLoading(_compareData_PreviousGeoPartItem);

      // reset last id that the same compare can be restarted
//		_compareData_TourId = Long.MIN_VALUE;
   }

   private void showRefTour(final long refId) {

      final TourCompareConfig tourCompareConfig = ReferenceTourManager.getTourCompareConfig(refId);

      if (tourCompareConfig == null) {
         return;
      }

      final TourData tourData = tourCompareConfig.getRefTourData();

      if (tourData != null) {

         final TourReference refTour = tourCompareConfig.getRefTour();

         /*
          * Convert real ref tour into a geo compare ref tour that the behaviour is the same,
          * however this will disable features in the tour compare chart but this is already very
          * complex.
          */

         final long geoCompareRefId = ReferenceTourManager.createGeoCompareRefTour(
               tourData,
               refTour.getStartValueIndex(),
               refTour.getEndValueIndex());

         compare_10_Compare(
               tourData,
               refTour.getStartValueIndex(),
               refTour.getEndValueIndex(),
               geoCompareRefId);
      }
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   private void updateUI_GeoItem(final GeoPartItem geoItem) {

      if (geoItem == null) {

         _lblTitle.setText(UI.EMPTY_STRING);

         _slideoutGeoCompareState.isReset = true;

      } else {

         _lblTitle.setText(_compareData_TourTitle);

         _slideoutGeoCompareState.numTours = geoItem.tourIds.length;

      }

      _slideoutGeoCompareOptions.updateUI_StateValues(_slideoutGeoCompareState);
      updateUI_StateValues();
   }

   private void updateUI_HideFalsePositive() {

//		enableControls_HideFalsePositive();
//
//		if (_isComparingDone) {
//
//			final int hidePosValue = _scaleHideFalsePositive.getSelection();
//
//			_lblHideFalsePositiveValue.setText(Integer.toString(hidePosValue));
//		}
   }

   /**
    * Select and reveal a compare item item.
    *
    * @param selection
    */
   private void updateUI_SelectCompareItem(final ISelection selection) {

      _isInUpdate = true;
      {
         _geoPartViewer.setSelection(selection, true);

         _geoPartViewer.getTable().showSelection();
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

      final Table table = _geoPartViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(sortDirection == CompareResultComparator.ASCENDING ? SWT.UP : SWT.DOWN);
   }

   private void updateUI_State_CancelComparing() {

      _lblCompareStatus.setText(Messages.GeoCompare_View_State_ComparingIsCanceled);
   }

   private void updateUI_State_Progress(final int workedTours, final int numTours) {

      if (workedTours == -1 && numTours == -1) {

         _lblCompareStatus.setText(Messages.GeoCompare_View_State_StartComparing);

      } else if (workedTours == numTours) {

         _lblCompareStatus.setText(String.format(Messages.GeoCompare_View_State_CompareResult, numTours));

      } else {

         _lblCompareStatus.setText(NLS.bind("Comparing tours: {0} / {1}", workedTours, numTours)); //$NON-NLS-1$
      }
   }

   private void updateUI_StateValues() {

      if (_slideoutGeoCompareState.isReset) {

         _lblNumGeoGrids.setText(UI.EMPTY_STRING);
         _lblNumSlices.setText(UI.EMPTY_STRING);
         _lblNumTours.setText(UI.EMPTY_STRING);

      } else {

         _lblNumGeoGrids.setText(Integer.toString(_slideoutGeoCompareState.numGeoGrids));
         _lblNumSlices.setText(Integer.toString(_slideoutGeoCompareState.numSlices));
         _lblNumTours.setText(Integer.toString(_slideoutGeoCompareState.numTours));
      }

   }

   private void updateUI_Viewer() {

      _geoPartViewer.setInput(new Object[0]);
   }
}
