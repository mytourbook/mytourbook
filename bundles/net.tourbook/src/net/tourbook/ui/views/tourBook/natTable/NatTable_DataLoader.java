/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook.natTable;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;

import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.SQL;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.views.tourBook.LazyTourLoaderItem;
import net.tourbook.ui.views.tourBook.TVITourBookItem;
import net.tourbook.ui.views.tourBook.TVITourBookTour;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;
import org.eclipse.swt.widgets.Display;

public class NatTable_DataLoader {

   private static final char                              NL                    = net.tourbook.common.UI.NEW_LINE;

   private static final String                            SQL_ASCENDING         = "ASC";                           //$NON-NLS-1$
   private static final String                            SQL_DESCENDING        = "DESC";                          //$NON-NLS-1$

   private static final String                            SQL_DEFAULT_FIELD     = "TourStartTime";                 //$NON-NLS-1$

   /**
    * Dummy field name for fields which currently cannot be sorted in the NatTable.
    */
   public static final String                             FIELD_WITHOUT_SORTING = "FIELD_WITHOUT_SORTING";         //$NON-NLS-1$

   private static final int                               FETCH_SIZE            = 1000;

   private static final ExecutorService                   _loadingExecutor      = createExecuter_TourLoading();
   private static final ExecutorService                   _rowIndexExecutor     = createExecuter_TourId_RowIndex();

   private ConcurrentHashMap<Integer, Integer>            _pageNumbers_Fetched  = new ConcurrentHashMap<>();
   private ConcurrentHashMap<Integer, LazyTourLoaderItem> _pageNumbers_Loading  = new ConcurrentHashMap<>();

   /**
    * Relation between row position and tour item
    * <p>
    * Key: Row position<br>
    * Value: {@link TVITourBookTour}
    */
   private ConcurrentHashMap<Integer, TVITourBookTour>    _fetchedTourItems     = new ConcurrentHashMap<>();

   /**
    * Relation between tour id and row position
    * <p>
    * Key: Tour ID <br>
    * Value: Row position
    */
   private ConcurrentHashMap<Long, Integer>               _fetchedTourIndex     = new ConcurrentHashMap<>();
   private final LinkedBlockingDeque<LazyTourLoaderItem>  _loaderWaitingQueue   = new LinkedBlockingDeque<>();

   private String[]                                       _allSortColumnIds;
   private ArrayList<SortDirectionEnum>                   _allSortDirections;

   private ArrayList<String>                              _allSqlSortFields     = new ArrayList<>();
   private ArrayList<String>                              _allSqlSortDirections = new ArrayList<>();

   /**
    * Contains all columns (also hidden columns), sorted in the order how they are displayed in the
    * UI.
    */
   public ArrayList<ColumnDefinition>                     allSortedColumns;

   /**
    * Number of all tours for the current lazy loader
    */
   private int                                            _numAllTourItems      = -1;

   private TourBookView                                   _tourBookView;

   private ColumnManager                                  _columnManager;
   /**
    * Contains all tour id's for the current tour filter and tour sorting, this is used
    * to get the row index for a tour.
    */
   private long[]                                         _allLoadedTourIds;

   public NatTable_DataLoader(final TourBookView tourBookView, final ColumnManager columnManager) {

      _tourBookView = tourBookView;
      _columnManager = columnManager;

      createColumnHeaderData();
   }

   private static ExecutorService createExecuter_TourId_RowIndex() {

      final ThreadFactory threadFactory = new ThreadFactory() {

         @Override
         public Thread newThread(final Runnable r) {

            final Thread thread = new Thread(r, "NatTable_DataLoader: Loading row indices");//$NON-NLS-1$

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);

            return thread;
         }
      };

      return Executors.newSingleThreadExecutor(threadFactory);
   }

   private static ExecutorService createExecuter_TourLoading() {

      final ThreadFactory threadFactory = new ThreadFactory() {

         @Override
         public Thread newThread(final Runnable r) {

            final Thread thread = new Thread(r, "NatTable_DataLoader: Loading tours");//$NON-NLS-1$

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);

            return thread;
         }
      };

// !!! newCachedThreadPool is not working, part of the view is not updated !!!
//
//      final ExecutorService loadingExecutor = Executors.newCachedThreadPool(threadFactory);
//
//      final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) loadingExecutor;
//
//      threadPoolExecutor.setKeepAliveTime(1, TimeUnit.SECONDS);
//      threadPoolExecutor.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());
//
//      return loadingExecutor;

      return Executors.newSingleThreadExecutor(threadFactory);
   }

   private void createColumnHeaderData() {

      allSortedColumns = _columnManager.getVisibleAndSortedColumns();
   }

   private int[] createRowIndicesFromTourIds(final ArrayList<Long> allRequestedTourIds) {

      final int numRequestedTourIds = allRequestedTourIds.size();
      if (numRequestedTourIds == 0) {

         // nothing more to do

         return new int[0];
      }

      final TIntArrayList allRowIndices = new TIntArrayList();
      final int numAllAvailableTourIds = _allLoadedTourIds.length;

      // loop: all requested tour id's
      for (final Long requestedTourId : allRequestedTourIds) {

         // loop: all available tour id's
         for (int rowPosition = 0; rowPosition < numAllAvailableTourIds; rowPosition++) {

            final long loadedTourId = _allLoadedTourIds[rowPosition];

            if (loadedTourId == requestedTourId) {

               allRowIndices.add(rowPosition);

               // keep relation also for already fetched tour id's
               _fetchedTourIndex.put(requestedTourId, rowPosition);

               break;
            }
         }
      }

      return allRowIndices.toArray();
   }

   private String createSqlOrderBy() {

      final int numOrderFields = _allSqlSortFields.size();

      if (numOrderFields == 0) {
         return UI.EMPTY_STRING;
      }

      final StringBuilder sb = new StringBuilder();

      sb.append(" ORDER BY ");//$NON-NLS-1$

      for (int fieldIndex = 0; fieldIndex < numOrderFields; fieldIndex++) {

         final String fieldName = _allSqlSortFields.get(fieldIndex);
         final String sortDirection = _allSqlSortDirections.get(fieldIndex);

         if (fieldIndex > 0) {
            // separate from previous order item
            sb.append(UI.COMMA_SPACE + NL);
         }

         sb.append(fieldName + UI.SPACE + sortDirection); //
      }

      sb.append(NL);

      return sb.toString();
   }

   private int fetchNumberOfTours() {

      final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);

      // get number of tours
      final String sql = NL

            + "SELECT COUNT(*)" //$NON-NLS-1$
            + " FROM " + TourDatabase.TABLE_TOUR_DATA //$NON-NLS-1$

            + " WHERE 1=1" + NL // //$NON-NLS-1$
            + sqlFilter.getWhereClause() + NL;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

//       TourDatabase.enableRuntimeStatistics(conn);

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         // set filter parameters
         sqlFilter.setParameters(prepStmt, 1);

         final ResultSet result = prepStmt.executeQuery();

         // get first result
         result.next();

         // get first value
         return result.getInt(1);

//       TourDatabase.disableRuntimeStatistic(conn);

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return 0;
   }

   /**
    * @param hoveredRow
    * @return Returns the fetched tour by it's row index or <code>null</code> when tour is not yet
    *         fetched from the backend.
    */
   public TVITourBookTour getFetchedTour(final int hoveredRow) {

      return _fetchedTourItems.get(hoveredRow);
   }

   /**
    * @param tourId
    * @return Returns the tour index but only when it was already fetched (which was done for
    *         displayed tour), otherwise -1.
    */
   public int getFetchedTourIndex(final long tourId) {

      final Integer rowIndex = _fetchedTourIndex.get(tourId);
      if (rowIndex == null) {
         return -1;
      } else {
         return rowIndex;
      }
   }

   /**
    * @return Returns number of all tours for the current lazy loader
    */
   int getNumberOfTours() {

      if (_numAllTourItems == -1) {
         _numAllTourItems = fetchNumberOfTours();
      }

      return _numAllTourItems;
   }

   /**
    * Number of columns which are visible in the natTable
    */
   int getNumberOfVisibleColumns() {

      return allSortedColumns.size();
   }

   /**
    * @param allRequestedTourIds
    * @return Returns NatTable row indices from the requested tour id's.
    */
   public CompletableFuture<int[]> getRowIndexFromTourId(final ArrayList<Long> allRequestedTourIds) {

      if (_allLoadedTourIds == null) {

         // firstly load all tour id's

         return CompletableFuture.supplyAsync(() -> {

            loadAllTourIds();

            return createRowIndicesFromTourIds(allRequestedTourIds);

         }, _rowIndexExecutor);

      } else {

         return CompletableFuture.supplyAsync(() -> {

            return createRowIndicesFromTourIds(allRequestedTourIds);

         }, _rowIndexExecutor);
      }
   }

   public String[] getSortColumnIds() {
      return _allSortColumnIds;
   }

   public ArrayList<SortDirectionEnum> getSortDirections() {
      return _allSortDirections;
   }

   /**
    * Maps column field -> database field
    *
    * @param sortColumnId
    * @return Returns database field
    */
   public String getSqlField(final String sortColumnId) {

// SET_FORMATTING_OFF

      switch (sortColumnId) {

      /**
       * These fields have a database index
       */

      // tour date
      case TableColumnFactory.TIME_DATE_ID:                          return SQL_DEFAULT_FIELD;

      // tour time, THERE IS CURRENTLY NO DATE ONLY FIELD
      case TableColumnFactory.TIME_TOUR_START_TIME_ID:               return FIELD_WITHOUT_SORTING;

      case TableColumnFactory.TOUR_TITLE_ID:                         return "TourTitle";              //$NON-NLS-1$
      case TableColumnFactory.DATA_IMPORT_FILE_NAME_ID:              return "TourImportFileName";     //$NON-NLS-1$

      case TableColumnFactory.TIME_WEEK_NO_ID:                       return "StartWeek";              //$NON-NLS-1$
      case TableColumnFactory.TIME_WEEKYEAR_ID:                      return "StartWeekYear";          //$NON-NLS-1$

// these fields are not yet displayed in tourbook view but are available in tour data indicies
//
//    case TableColumnFactory.:                                      return "TourEndTime";            //$NON-NLS-1$
//    case TableColumnFactory.:                                      return "HasGeoData";             //$NON-NLS-1$

      /**
       * These fields have NO database index
       */

      /*
      * BODY
      */
      case TableColumnFactory.BODY_AVG_PULSE_ID:                     return "avgPulse";               //$NON-NLS-1$
      case TableColumnFactory.BODY_CALORIES_ID:                      return "calories";               //$NON-NLS-1$
      case TableColumnFactory.BODY_PULSE_MAX_ID:                     return "maxPulse";               //$NON-NLS-1$
      case TableColumnFactory.BODY_PERSON_ID:                        return "tourPerson_personId";    //$NON-NLS-1$
      case TableColumnFactory.BODY_RESTPULSE_ID:                     return "restPulse";              //$NON-NLS-1$
      case TableColumnFactory.BODY_WEIGHT_ID:                        return "bikerWeight";            //$NON-NLS-1$

      /*
       * DATA
       */
      case TableColumnFactory.DATA_DP_TOLERANCE_ID:                  return "dpTolerance";            //$NON-NLS-1$
//    case TableColumnFactory.DATA_IMPORT_FILE_NAME_ID:              // see indexed fields
      case TableColumnFactory.DATA_IMPORT_FILE_PATH_ID:              return "tourImportFilePath";     //$NON-NLS-1$
      case TableColumnFactory.DATA_NUM_TIME_SLICES_ID:               return "numberOfTimeSlices";     //$NON-NLS-1$
      case TableColumnFactory.DATA_TIME_INTERVAL_ID:                 return "deviceTimeInterval";     //$NON-NLS-1$

      /*
       * DEVICE
       */
      case TableColumnFactory.DEVICE_DISTANCE_ID:                    return "startDistance";          //$NON-NLS-1$
      case TableColumnFactory.DEVICE_NAME_ID:                        return "devicePluginName";       //$NON-NLS-1$

      /*
       * ELEVATION
       */
      case TableColumnFactory.ALTITUDE_AVG_CHANGE_ID:                return "avgAltitudeChange";      //$NON-NLS-1$
      case TableColumnFactory.ALTITUDE_MAX_ID:                       return "maxAltitude";            //$NON-NLS-1$
      case TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_DOWN_ID:    return "tourAltDown";            //$NON-NLS-1$
      case TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_UP_ID:      return "tourAltUp";              //$NON-NLS-1$

      /*
       * MOTION
       */
      case TableColumnFactory.MOTION_AVG_PACE_ID:                    return FIELD_WITHOUT_SORTING;
      case TableColumnFactory.MOTION_AVG_SPEED_ID:                   return FIELD_WITHOUT_SORTING;
      case TableColumnFactory.MOTION_DISTANCE_ID:                    return "tourDistance";           //$NON-NLS-1$
      case TableColumnFactory.MOTION_MAX_SPEED_ID:                   return "maxSpeed";               //$NON-NLS-1$

      /*
       * POWER
       */
      case TableColumnFactory.POWER_AVG_ID:                          return "power_Avg";              //$NON-NLS-1$
      case TableColumnFactory.POWER_MAX_ID:                          return "power_Max";              //$NON-NLS-1$
      case TableColumnFactory.POWER_NORMALIZED_ID:                   return "power_Normalized";       //$NON-NLS-1$
      case TableColumnFactory.POWER_TOTAL_WORK_ID:                   return "power_TotalWork";        //$NON-NLS-1$

      /*
       * POWERTRAIN
       */
      case TableColumnFactory.POWERTRAIN_AVG_CADENCE_ID:                            return "avgCadence";                          //$NON-NLS-1$
      case TableColumnFactory.POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS_ID:              return "power_AvgLeftPedalSmoothness";        //$NON-NLS-1$
      case TableColumnFactory.POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS_ID:          return "power_AvgLeftTorqueEffectiveness";    //$NON-NLS-1$
      case TableColumnFactory.POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS_ID:             return "power_AvgRightPedalSmoothness";       //$NON-NLS-1$
      case TableColumnFactory.POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS_ID:         return "power_AvgRightTorqueEffectiveness";   //$NON-NLS-1$
      case TableColumnFactory.POWERTRAIN_CADENCE_MULTIPLIER_ID:                     return "cadenceMultiplier";                   //$NON-NLS-1$
      case TableColumnFactory.POWERTRAIN_GEAR_FRONT_SHIFT_COUNT_ID:                 return "frontShiftCount";                     //$NON-NLS-1$
      case TableColumnFactory.POWERTRAIN_GEAR_REAR_SHIFT_COUNT_ID:                  return "rearShiftCount";                      //$NON-NLS-1$
      case TableColumnFactory.POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE_ID:               return "power_PedalLeftRightBalance";         //$NON-NLS-1$
      case TableColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES_ID:       return FIELD_WITHOUT_SORTING;
      case TableColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER_ID:   return FIELD_WITHOUT_SORTING;

      /*
       * RUNNING DYNAMICS
       */
      case TableColumnFactory.RUN_DYN_STANCE_TIME_AVG_ID:            return "runDyn_StanceTime_Avg";           //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_STANCE_TIME_MIN_ID:            return "runDyn_StanceTime_Min";           //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_STANCE_TIME_MAX_ID:            return "runDyn_StanceTime_Max";           //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_AVG_ID:    return "runDyn_StanceTimeBalance_Avg";    //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MIN_ID:    return "runDyn_StanceTimeBalance_Min";    //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MAX_ID:    return "runDyn_StanceTimeBalance_Max";    //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_STEP_LENGTH_AVG_ID:            return "runDyn_StepLength_Avg";           //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_STEP_LENGTH_MIN_ID:            return "runDyn_StepLength_Min";           //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_STEP_LENGTH_MAX_ID:            return "runDyn_StepLength_Max";           //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_AVG_ID:   return "runDyn_VerticalOscillation_Avg";  //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MIN_ID:   return "runDyn_VerticalOscillation_Min";  //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MAX_ID:   return "runDyn_VerticalOscillation_Max";  //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_VERTICAL_RATIO_AVG_ID:         return "runDyn_VerticalRatio_Avg";        //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_VERTICAL_RATIO_MIN_ID:         return "runDyn_VerticalRatio_Min";        //$NON-NLS-1$
      case TableColumnFactory.RUN_DYN_VERTICAL_RATIO_MAX_ID:         return "runDyn_VerticalRatio_Max";        //$NON-NLS-1$

      /*
       * SURFING
       */
      case TableColumnFactory.SURFING_MIN_DISTANCE_ID:               return "surfing_MinDistance";             //$NON-NLS-1$
      case TableColumnFactory.SURFING_MIN_SPEED_START_STOP_ID:       return "surfing_MinSpeed_StartStop";      //$NON-NLS-1$
      case TableColumnFactory.SURFING_MIN_SPEED_SURFING_ID:          return "surfing_MinSpeed_Surfing";        //$NON-NLS-1$
      case TableColumnFactory.SURFING_MIN_TIME_DURATION_ID:          return "surfing_MinTimeDuration";         //$NON-NLS-1$
      case TableColumnFactory.SURFING_NUMBER_OF_EVENTS_ID:           return "surfing_NumberOfEvents";          //$NON-NLS-1$

      /*
       * TIME
       */
      case TableColumnFactory.TIME_DRIVING_TIME_ID:                  return "tourDrivingTime";                 //$NON-NLS-1$
      case TableColumnFactory.TIME_PAUSED_TIME_ID:                   return FIELD_WITHOUT_SORTING;
      case TableColumnFactory.TIME_PAUSED_TIME_RELATIVE_ID:          return FIELD_WITHOUT_SORTING;
      case TableColumnFactory.TIME_RECORDING_TIME_ID:                return "tourRecordingTime";               //$NON-NLS-1$
      case TableColumnFactory.TIME_TIME_ZONE_ID:                     return "TimeZoneId";                      //$NON-NLS-1$
      case TableColumnFactory.TIME_TIME_ZONE_DIFFERENCE_ID:          return FIELD_WITHOUT_SORTING;
//    case TableColumnFactory.TIME_TOUR_START_TIME_ID:               // see indexed fields
      case TableColumnFactory.TIME_WEEK_DAY_ID:                      return FIELD_WITHOUT_SORTING;
//    case TableColumnFactory.TIME_WEEK_NO_ID:                       // see indexed fields
//    case TableColumnFactory.TIME_WEEKYEAR_ID:                      // see indexed fields

      /*
       * TOUR
       */
      case TableColumnFactory.TOUR_LOCATION_START_ID:                return "tourStartPlace";                              //$NON-NLS-1$
      case TableColumnFactory.TOUR_LOCATION_END_ID:                  return "tourEndPlace";                                //$NON-NLS-1$
      case TableColumnFactory.TOUR_NUM_MARKERS_ID:                   return FIELD_WITHOUT_SORTING;
      case TableColumnFactory.TOUR_NUM_PHOTOS_ID:                    return FIELD_WITHOUT_SORTING;
      case TableColumnFactory.TOUR_TAGS_ID:                          return FIELD_WITHOUT_SORTING;
//    case TableColumnFactory.TOUR_TITLE_ID:                         // see indexed fields
      case TableColumnFactory.TOUR_TYPE_ID:                          return "tourType_typeId";  // an icon is displayed    //$NON-NLS-1$
      case TableColumnFactory.TOUR_TYPE_TEXT_ID:                     return FIELD_WITHOUT_SORTING;

      /*
       * TRAINING
       */
      case TableColumnFactory.TRAINING_EFFECT_AEROB_ID:              return "training_TrainingEffect_Aerob";               //$NON-NLS-1$
      case TableColumnFactory.TRAINING_EFFECT_ANAEROB_ID:            return "training_TrainingEffect_Anaerob";             //$NON-NLS-1$
      case TableColumnFactory.TRAINING_FTP_ID:                       return "power_FTP";                                   //$NON-NLS-1$
      case TableColumnFactory.TRAINING_INTENSITY_FACTOR_ID:          return "power_IntensityFactor";                       //$NON-NLS-1$
      case TableColumnFactory.TRAINING_POWER_TO_WEIGHT_ID:           return FIELD_WITHOUT_SORTING;
      case TableColumnFactory.TRAINING_STRESS_SCORE_ID:              return "power_TrainingStressScore";                   //$NON-NLS-1$
      case TableColumnFactory.TRAINING_PERFORMANCE_LEVEL_ID:         return "training_TrainingPerformance";                //$NON-NLS-1$

      /*
       * WEATHER
       */
      case TableColumnFactory.WEATHER_CLOUDS_ID:                     return "weatherClouds";   // an icon is displayed     //$NON-NLS-1$
      case TableColumnFactory.WEATHER_TEMPERATURE_AVG_ID:            return "(DOUBLE(avgTemperature) / temperatureScale)"; //$NON-NLS-1$
      case TableColumnFactory.WEATHER_TEMPERATURE_MIN_ID:            return "weather_Temperature_Min";                     //$NON-NLS-1$
      case TableColumnFactory.WEATHER_TEMPERATURE_MAX_ID:            return "weather_Temperature_Max";                     //$NON-NLS-1$
      case TableColumnFactory.WEATHER_WIND_DIR_ID:                   return "weatherWindDir";                              //$NON-NLS-1$
      case TableColumnFactory.WEATHER_WIND_SPEED_ID:                 return "weatherWindSpd";                              //$NON-NLS-1$

      default:

         // ensure a valid field is returned, this case should not happen

         System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] getSqlField()" //$NON-NLS-1$ //$NON-NLS-2$
               + "\tsortColumnId: \"" + sortColumnId + "\"" //$NON-NLS-1$ //$NON-NLS-2$
               + " has not a valid sql field" //$NON-NLS-1$
               );

         return SQL_DEFAULT_FIELD;
      }

   // SET_FORMATTING_ON
   }

   /**
    * @param rowIndex
    * @return Returns tour item at the requested row index or <code>null</code> when not yet
    *         available. When tour is not yet loaded then the data will be fetched from the backend.
    */
   TVITourBookTour getTour(final int rowIndex) {

      final TVITourBookTour loadedTourItem = _fetchedTourItems.get(rowIndex);

      if (loadedTourItem != null) {

         // tour is loaded

         return loadedTourItem;
      }

      /*
       * Check if the tour is currently fetched
       */
      final int fetchKey = rowIndex / FETCH_SIZE;

      LazyTourLoaderItem loaderItem = _pageNumbers_Loading.get(fetchKey);

      if (loaderItem != null) {

         // tour is currently being loading -> wait until finished loading

         return null;
      }

      /*
       * Tour is not yet loaded or not yet loading -> load it now
       */

      loaderItem = new LazyTourLoaderItem();

      loaderItem.sqlOffset = fetchKey * FETCH_SIZE;
      loaderItem.fetchKey = fetchKey;

      _pageNumbers_Loading.put(fetchKey, loaderItem);

      _loaderWaitingQueue.add(loaderItem);

      _loadingExecutor.submit(new Runnable() {
         @Override
         public void run() {

            // get last added loader item
            final LazyTourLoaderItem loaderItem = _loaderWaitingQueue.pollFirst();

            if (loaderItem == null) {
               return;
            }

            if (loadPagedTourItems(loaderItem)) {

               // update UI

               final NatTable tourViewer_NatTable = _tourBookView.getTourViewer_NatTable();
               final Display display = tourViewer_NatTable.getDisplay();

               display.asyncExec(() -> {

                  if (tourViewer_NatTable.isDisposed()) {
                     return;
                  }

                  // do a simple redraw as it retrieves values from the model
                  tourViewer_NatTable.redraw();
               });
            }

            final int loaderItemFetchKey = loaderItem.fetchKey;

            _pageNumbers_Fetched.put(loaderItemFetchKey, loaderItemFetchKey);
            _pageNumbers_Loading.remove(loaderItemFetchKey);
         }
      });

      return null;
   }

   long getTourId(final int rowIndex) {

      if (_allLoadedTourIds == null) {

         return -1;

      } else {

         return _allLoadedTourIds[rowIndex];
      }
   }

   /**
    * Loads all tour id's for the current sort and tour filter
    *
    * @return
    */
   private void loadAllTourIds() {

      final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);

      final String sql = NL

            + "SELECT TOURID" //                                                       //$NON-NLS-1$

            + " FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //            //$NON-NLS-1$ //$NON-NLS-2$

            + " WHERE 1=1" + NL // //$NON-NLS-1$
            + sqlFilter.getWhereClause() + NL

            + createSqlOrderBy();

      final TLongArrayList allTourIds = new TLongArrayList();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         // set filter parameters
         sqlFilter.setParameters(prepStmt, 1);

         int rowIndex = 0;

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

            final long tourId = result.getLong(1);

            allTourIds.add(tourId);

            _fetchedTourIndex.put(tourId, rowIndex++);
         }

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }

      _allLoadedTourIds = allTourIds.toArray();
   }

   private boolean loadPagedTourItems(final LazyTourLoaderItem loaderItem) {

//      final long start = System.nanoTime();

      final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);

      final String sql = NL

            + "SELECT " //                                                             //$NON-NLS-1$

            + TVITourBookItem.SQL_ALL_TOUR_FIELDS + NL

            + " FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //            //$NON-NLS-1$ //$NON-NLS-2$

            + " WHERE 1=1" + NL // //$NON-NLS-1$
            + sqlFilter.getWhereClause() + NL

            + createSqlOrderBy()

            + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY" + NL //                          //$NON-NLS-1$
      ;

//      System.out.println((System.currentTimeMillis() + " sql:" + sql));
//      // TODO remove SYSTEM.OUT.PRINTLN

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         int rowIndex = loaderItem.sqlOffset;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         // set filter parameters
         sqlFilter.setParameters(prepStmt, 1);

         // set other parameters
         int paramIndex = sqlFilter.getLastParameterIndex();

         prepStmt.setInt(paramIndex++, rowIndex);
         prepStmt.setInt(paramIndex++, FETCH_SIZE);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

            final TVITourBookTour tourItem = new TVITourBookTour(null, null);

            tourItem.tourId = result.getLong(1);

            TVITourBookItem.getTourDataFields(result, tourItem);

            final int natTableRowIndex = rowIndex++;

            _fetchedTourItems.put(natTableRowIndex, tourItem);
            _fetchedTourIndex.put(tourItem.tourId, natTableRowIndex);
         }

      } catch (final SQLException e) {

         SQL.showException(e, sql);

         return false;
      }

//      System.out.println((UI.timeStampNano() + " " + this.getClass().getName() + " \t")
//            + (((float) (System.nanoTime() - start) / 1000000) + " ms"));
//      // TODO remove SYSTEM.OUT.PRINTLN

      return true;
   }

   /**
    * Cleanup all loaded data that the next time they are newly fetched when requested.
    */
   public void resetTourItems() {

      for (final TVITourBookTour tourItem : _fetchedTourItems.values()) {
         tourItem.clearChildren();
      }

      _fetchedTourItems.clear();
      _fetchedTourIndex.clear();

      _pageNumbers_Fetched.clear();
      _pageNumbers_Loading.clear();

      _allLoadedTourIds = null;

      _numAllTourItems = -1;
   }

   /**
    * Sets sort column id/direction but first cleanup the previous loaded tours.
    *
    * @param allSortColumnIds
    * @param allSortDirections
    */
   public void setupSortColumns(final String[] allSortColumnIds, final ArrayList<SortDirectionEnum> allSortDirections) {

      // cleanup old fetched tours
      resetTourItems();

      _allSortColumnIds = allSortColumnIds;
      _allSortDirections = allSortDirections;

      _allSqlSortFields.clear();
      _allSqlSortDirections.clear();

      final int numSortColumns = allSortDirections.size();

      for (int columnIndex = 0; columnIndex < numSortColumns; columnIndex++) {

         final String sqlField = getSqlField(allSortColumnIds[columnIndex]);

         // ensure that the dummy field is not used in the sql statement, this should not happen but it did during development
         if (FIELD_WITHOUT_SORTING.equals(sqlField) == false) {

            final SortDirectionEnum sortDirectionEnum = allSortDirections.get(columnIndex);

            // skip field which are not sorted
            if (sortDirectionEnum.equals(SortDirectionEnum.NONE) == false) {

               /*
                * Set sort order
                */
               if (sortDirectionEnum == SortDirectionEnum.ASC) {
                  _allSqlSortDirections.add(SQL_ASCENDING);
               } else {
                  _allSqlSortDirections.add(SQL_DESCENDING);
               }

               /*
                * Set sort field
                */
               _allSqlSortFields.add(sqlField);
            }
         }
      }
   }

}
