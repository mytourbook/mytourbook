/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.ComputeChartValue;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPhoto;
import net.tourbook.database.MyTourbookException;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.RawDataManager.TourValueType;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoGallery;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.TourPhotoReference;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20Item;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageViews;
import net.tourbook.srtm.IPreferences;
import net.tourbook.srtm.PrefPageSRTMData;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProviderAll;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.tourChart.GraphBackgroundSource;
import net.tourbook.ui.tourChart.GraphBackgroundStyle;
import net.tourbook.ui.tourChart.IValueLabelProvider;
import net.tourbook.ui.tourChart.PulseGraph;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.ui.tourChart.TourChartView;
import net.tourbook.ui.tourChart.X_AXIS_START_TIME;
import net.tourbook.ui.views.IWeatherProvider;
import net.tourbook.ui.views.TourChartAnalyzerInfo;
import net.tourbook.ui.views.collateTours.CollatedToursView;
import net.tourbook.ui.views.rawData.RawDataView;
import net.tourbook.ui.views.tourBook.TourBookView;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;
import net.tourbook.weather.TourWeatherRetriever;

import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class TourManager {

   private static final String SYS_PROP__LOG_RR_ERRORS = "logRRErrors";                                      //$NON-NLS-1$
   private static boolean      _isLogging_RR_Errors    = System.getProperty(SYS_PROP__LOG_RR_ERRORS) != null;

   static {
      if (_isLogging_RR_Errors) {
         Util.logSystemProperty_IsEnabled(TourManager.class, SYS_PROP__LOG_RR_ERRORS, "R-R errors are logged"); //$NON-NLS-1$
      }
   }

   private static final String GRAPH_LABEL_ALTIMETER                           = net.tourbook.common.Messages.Graph_Label_Altimeter;
   private static final String GRAPH_LABEL_ALTITUDE                            = net.tourbook.common.Messages.Graph_Label_Altitude;
   private static final String GRAPH_LABEL_CADENCE                             = net.tourbook.common.Messages.Graph_Label_Cadence;
   private static final String GRAPH_LABEL_CADENCE_UNIT                        = net.tourbook.common.Messages.Graph_Label_Cadence_Unit;
   private static final String GRAPH_LABEL_CADENCE_UNIT_SPM                    = net.tourbook.common.Messages.Graph_Label_Cadence_Unit_Spm;
   private static final String GRAPH_LABEL_CADENCE_UNIT_RPM_SPM                = net.tourbook.common.Messages.Graph_Label_Cadence_Unit_RpmSpm;
   private static final String GRAPH_LABEL_GEARS                               = net.tourbook.common.Messages.Graph_Label_Gears;
   private static final String GRAPH_LABEL_GEO_COMPARE_UNIT                    = net.tourbook.common.Messages.Graph_Label_Geo_Compare_Unit;
   private static final String GRAPH_LABEL_GRADIENT                            = net.tourbook.common.Messages.Graph_Label_Gradient;
   private static final String GRAPH_LABEL_GRADIENT_UNIT                       = net.tourbook.common.Messages.Graph_Label_Gradient_Unit;
   private static final String GRAPH_LABEL_HEARTBEAT                           = net.tourbook.common.Messages.Graph_Label_Heartbeat;
   private static final String GRAPH_LABEL_HEARTBEAT_UNIT                      = net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;
   private static final String GRAPH_LABEL_PACE                                = net.tourbook.common.Messages.Graph_Label_Pace;
   private static final String GRAPH_LABEL_PACE_SUMMARIZED                     = net.tourbook.common.Messages.Graph_Label_Pace_Summarized;
   private static final String GRAPH_LABEL_POWER                               = net.tourbook.common.Messages.Graph_Label_Power;
   private static final String GRAPH_LABEL_POWER_UNIT                          = net.tourbook.common.Messages.Graph_Label_Power_Unit;
   private static final String GRAPH_LABEL_SPEED                               = net.tourbook.common.Messages.Graph_Label_Speed;
   private static final String GRAPH_LABEL_SPEED_SUMMARIZED                    = net.tourbook.common.Messages.Graph_Label_Speed_Summarized;
   private static final String GRAPH_LABEL_TEMPERATURE                         = net.tourbook.common.Messages.Graph_Label_Temperature;
   private static final String GRAPH_LABEL_TOUR_COMPARE                        = net.tourbook.common.Messages.Graph_Label_Tour_Compare;
   private static final String GRAPH_LABEL_TOUR_COMPARE_UNIT                   = net.tourbook.common.Messages.Graph_Label_Tour_Compare_Unit;
   private static final String GRAPH_LABEL_RUN_DYN_STANCE_TIME                 = net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTime;
   private static final String GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCE         = net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTimeBalance;
   private static final String GRAPH_LABEL_RUN_DYN_STEP_LENGTH                 = net.tourbook.common.Messages.Graph_Label_RunDyn_StepLength;
   private static final String GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION        = net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalOscillation;
   private static final String GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO              = net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalRatio;
   private static final String GRAPH_LABEL_SWIM_STROKES                        = net.tourbook.common.Messages.Graph_Label_Swim_Strokes;
   private static final String GRAPH_LABEL_SWIM_SWOLF                          = net.tourbook.common.Messages.Graph_Label_Swim_Swolf;
   //
   static final String         LOG_TEMP_ADJUST_001_START                       = Messages.Log_TemperatureAdjustment_001_Start;
   static final String         LOG_TEMP_ADJUST_002_END                         = Messages.Log_TemperatureAdjustment_002_End;
   private static final String LOG_TEMP_ADJUST_003_TOUR_CHANGES                = Messages.Log_TemperatureAdjustment_003_TourChanges;
   private static final String LOG_TEMP_ADJUST_005_TOUR_IS_TOO_SHORT           = Messages.Log_TemperatureAdjustment_005_TourIsTooShort;
   public static final String  LOG_TEMP_ADJUST_006_IS_ABOVE_TEMPERATURE        = Messages.Log_TemperatureAdjustment_006_IsAboveTemperature;
   private static final String LOG_TEMP_ADJUST_010_NO_TEMPERATURE_DATA_SERIE   = Messages.Log_TemperatureAdjustment_010_NoTemperatureDataSeries;
   private static final String LOG_TEMP_ADJUST_011_NO_TIME_DATA_SERIE          = Messages.Log_TemperatureAdjustment_011_NoTimeDataSeries;
   //
   private static final String LOG_RETRIEVE_WEATHER_DATA_001_START             = Messages.Log_RetrieveWeatherData_001_Start;
   private static final String LOG_RETRIEVE_WEATHER_DATA_002_END               = Messages.Log_RetrieveWeatherData_002_End;
   private static final String LOG_RETRIEVE_WEATHER_DATA_010_NO_GPS_DATA_SERIE = Messages.Log_RetrieveWeatherData_010_NoGpsDataSeries;
   //
   public static final String  CUSTOM_DATA_TOUR_DATA                           = "tourData";                                                         //$NON-NLS-1$
   public static final String  CUSTOM_DATA_TOUR_CHART_CONFIGURATION            = "tourChartConfig";                                                  //$NON-NLS-1$
   //
   public static final String  CUSTOM_DATA_ALTIMETER                           = "altimeter";                                                        //$NON-NLS-1$
   public static final String  CUSTOM_DATA_ALTITUDE                            = "altitude";                                                         //$NON-NLS-1$
   public static final String  CUSTOM_DATA_CADENCE                             = "cadence";                                                          //$NON-NLS-1$
   private static final String CUSTOM_DATA_DISTANCE                            = "distance";                                                         //$NON-NLS-1$
   public static final String  CUSTOM_DATA_GEAR_RATIO                          = "gearRatio";                                                        //$NON-NLS-1$
   public static final String  CUSTOM_DATA_GRADIENT                            = "gradient";                                                         //$NON-NLS-1$
   public static final String  CUSTOM_DATA_HISTORY                             = "history";                                                          //$NON-NLS-1$
   public static final String  CUSTOM_DATA_PACE                                = "pace";                                                             //$NON-NLS-1$
   public static final String  CUSTOM_DATA_PACE_SUMMARIZED                     = "pace-summarized";                                                  //$NON-NLS-1$
   public static final String  CUSTOM_DATA_POWER                               = "power";                                                            //$NON-NLS-1$
   public static final String  CUSTOM_DATA_PULSE                               = "pulse";                                                            //$NON-NLS-1$
   public static final String  CUSTOM_DATA_SPEED                               = "speed";                                                            //$NON-NLS-1$
   public static final String  CUSTOM_DATA_SPEED_SUMMARIZED                    = "speed-summarized";                                                 //$NON-NLS-1$
   public static final String  CUSTOM_DATA_TEMPERATURE                         = "temperature";                                                      //$NON-NLS-1$
   private static final String CUSTOM_DATA_TIME                                = "time";                                                             //$NON-NLS-1$
   public static final String  CUSTOM_DATA_RUN_DYN_STANCE_TIME                 = "runDyn_RunDyn_StanceTime";                                         //$NON-NLS-1$
   public static final String  CUSTOM_DATA_RUN_DYN_STANCE_TIME_BALANCE         = "runDyn_RunDyn_StanceTimeBalance";                                  //$NON-NLS-1$
   public static final String  CUSTOM_DATA_RUN_DYN_STEP_LENGTH                 = "runDyn_RunDyn_StepLength";                                         //$NON-NLS-1$
   public static final String  CUSTOM_DATA_RUN_DYN_VERTICAL_OSCILLATION        = "runDyn_RunDyn_VerticalOscillation";                                //$NON-NLS-1$
   public static final String  CUSTOM_DATA_RUN_DYN_VERTICAL_RATIO              = "runDyn_RunDyn_VerticalRatio";                                      //$NON-NLS-1$
   public static final String  CUSTOM_DATA_SWIM_STROKES                        = "swim_Strokes";                                                     //$NON-NLS-1$
   public static final String  CUSTOM_DATA_SWIM_SWOLF                          = "swim_Swolf";                                                       //$NON-NLS-1$

   public static final String  CUSTOM_DATA_ANALYZER_INFO                       = "analyzerInfo";                                                     //$NON-NLS-1$
   public static final String  CUSTOM_DATA_SEGMENT_VALUES                      = "segmentValues";                                                    //$NON-NLS-1$
   public static final String  CUSTOM_DATA_CONCONI_TEST                        = "CUSTOM_DATA_CONCONI_TEST";                                         //$NON-NLS-1$
   //
   public static final String  X_AXIS_TIME                                     = "time";                                                             //$NON-NLS-1$
   public static final String  X_AXIS_DISTANCE                                 = "distance";                                                         //$NON-NLS-1$
   //
   private static final String FORMAT_MM_SS                                    = "%d:%02d";                                                          //$NON-NLS-1$
   public static final String  GEAR_RATIO_FORMAT                               = "%1.2f";                                                            //$NON-NLS-1$
   public static final String  GEAR_TEETH_FORMAT                               = "%2d : %2d";                                                        //$NON-NLS-1$
   static final String         GEAR_VALUE_FORMAT                               = GEAR_TEETH_FORMAT + " - " + GEAR_RATIO_FORMAT;                      //$NON-NLS-1$
   //
   public static final int     GRAPH_ALTITUDE                                  = 1000;
   public static final int     GRAPH_SPEED                                     = 1001;
   public static final int     GRAPH_ALTIMETER                                 = 1002;
   public static final int     GRAPH_PULSE                                     = 1003;
   public static final int     GRAPH_TEMPERATURE                               = 1004;
   public static final int     GRAPH_CADENCE                                   = 1005;
   public static final int     GRAPH_GRADIENT                                  = 1006;
   public static final int     GRAPH_POWER                                     = 1007;
   public static final int     GRAPH_PACE                                      = 1008;
   public static final int     GRAPH_GEARS                                     = 1009;
   public static final int     GRAPH_SPEED_SUMMARIZED                          = 1010;
   public static final int     GRAPH_PACE_SUMMARIZED                           = 1011;

   public static final int     GRAPH_RUN_DYN_STANCE_TIME                       = 1100;
   public static final int     GRAPH_RUN_DYN_STANCE_TIME_BALANCED              = 1101;
   public static final int     GRAPH_RUN_DYN_STEP_LENGTH                       = 1102;
   public static final int     GRAPH_RUN_DYN_VERTICAL_OSCILLATION              = 1103;
   public static final int     GRAPH_RUN_DYN_VERTICAL_RATIO                    = 1104;

   public static final int     GRAPH_SWIM_STROKES                              = 1200;
   public static final int     GRAPH_SWIM_SWOLF                                = 1201;

   public static final int     GRAPH_TRAINING_EFFECT_AEROB                     = 1300;
   public static final int     GRAPH_TRAINING_EFFECT_ANAEROB                   = 1301;
   public static final int     GRAPH_TRAINING_PERFORMANCE                      = 1302;

   public static final int     GRAPH_TOUR_COMPARE                              = 2000;
   //
   //
   /**
    * Contains all graph id's which are displayed as a graph in the tour chart and correspond to a
    * graph action
    */
   private static final int[]                            _allGraphIDs                      = new int[] {

         GRAPH_ALTITUDE,
         GRAPH_SPEED,
         GRAPH_SPEED_SUMMARIZED,
         GRAPH_ALTIMETER,
         GRAPH_PULSE,
         GRAPH_TEMPERATURE,
         GRAPH_CADENCE,
         GRAPH_GRADIENT,
         GRAPH_POWER,
         GRAPH_PACE,
         GRAPH_PACE_SUMMARIZED,
         GRAPH_GEARS,

         GRAPH_RUN_DYN_STANCE_TIME,
         GRAPH_RUN_DYN_STANCE_TIME_BALANCED,
         GRAPH_RUN_DYN_STEP_LENGTH,
         GRAPH_RUN_DYN_VERTICAL_OSCILLATION,
         GRAPH_RUN_DYN_VERTICAL_RATIO,

         GRAPH_SWIM_STROKES,
         GRAPH_SWIM_SWOLF,

         GRAPH_TOUR_COMPARE
   };

   private static final IPreferenceStore                 _prefStore                        = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore                 _prefStore_Common                 = CommonActivator.getPrefStore();

   private static TourManager                            _instance;

   private static final StringBuilder                    _sbFormatter                      = new StringBuilder();
   private static final Formatter                        _formatter                        = new Formatter(_sbFormatter);
   //
   private static final ListenerList<ITourEventListener> _tourEventListeners               = new ListenerList<>(ListenerList.IDENTITY);
   private static final ListenerList<ITourSaveListener>  _tourSaveListeners                = new ListenerList<>(ListenerList.IDENTITY);

   public static final String                            cadenceZonesTimes_StatementUpdate = UI.EMPTY_STRING

         + "UPDATE " + TourDatabase.TABLE_TOUR_DATA                                                                                    // //$NON-NLS-1$

         + " SET"                                                                                                                      // //$NON-NLS-1$

         + " cadenceZone_SlowTime=?, "                                                                                                 // //$NON-NLS-1$
         + " cadenceZone_FastTime=?, "                                                                                                 // //$NON-NLS-1$
         + " cadenceZones_DelimiterValue=? "                                                                                           // //$NON-NLS-1$

         + " WHERE tourId=?";                                                                                                          // //$NON-NLS-1$

   //
   /**
    * contains the instance of the {@link TourDataEditorView} or <code>null</code> when this part is
    * not opened
    */
   private static TourDataEditorView       _tourDataEditorInstance;
   //
   private static LabelProviderMMSS        _labelProviderMMSS = new LabelProviderMMSS();
   private static LabelProviderInt         _labelProviderInt  = new LabelProviderInt();
   //
   private static volatile TourData        _joined_TourData;
   private static int                      _joined_TourIds_Hash;
   private static volatile List<TourData>  _allLoaded_TourData;
   private static int                      _allLoaded_TourData_Hash;
   private static long                     _allLoaded_TourData_Key;
   private static int                      _allLoaded_TourIds_Hash;
   //
   private static ThreadPoolExecutor       _loadingTour_Executor;
   private static ArrayBlockingQueue<Long> _loadingTour_Queue = new ArrayBlockingQueue<>(Util.NUMBER_OF_PROCESSORS);
   private static CountDownLatch           _loadingTour_CountDownLatch;
   //
   static {

      final ThreadFactory loadingThreadFactory = runnable -> {

         final Thread thread = new Thread(runnable, "Loading tours from DB");//$NON-NLS-1$

         thread.setPriority(Thread.MIN_PRIORITY);
         thread.setDaemon(true);

         return thread;
      };

      _loadingTour_Executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Util.NUMBER_OF_PROCESSORS, loadingThreadFactory);
   }
   //
   private ComputeChartValue   _computeAvg_Altimeter;
   private ComputeChartValue   _computeAvg_Altitude;
   private ComputeChartValue   _computeAvg_Cadence;
   private ComputeChartValue   _computeAvg_Gradient;
   private ComputeChartValue   _computeAvg_Pace;
   private ComputeChartValue   _computeAvg_Power;
   private ComputeChartValue   _computeAvg_Pulse;
   private ComputeChartValue   _computeAvg_Speed;
   //
   private ComputeChartValue   _computeAvg_RunDyn_StanceTime;
   private ComputeChartValue   _computeAvg_RunDyn_StanceTimeBalance;
   private ComputeChartValue   _computeAvg_RunDyn_StepLength;
   private ComputeChartValue   _computeAvg_RunDyn_VerticalOscillation;
   private ComputeChartValue   _computeAvg_RunDyn_VerticalRatio;
   //
   private final TourDataCache _tourDataCache;

   /**
    * tour chart which shows the selected tour
    */
   private TourChart           _activeTourChart;

   private static class LabelProviderInt implements IValueLabelProvider {

      @Override
      public String getLabel(final float graphValue) {
         return Integer.toString((int) graphValue);
      }
   }

   private static class LabelProviderMMSS implements IValueLabelProvider {

      @Override
      public String getLabel(final float graphValue) {

         _sbFormatter.setLength(0);

         if (graphValue < 0) {
            _sbFormatter.append(UI.SYMBOL_DASH);
         }

         final long timeAbsolute = (long) (graphValue < 0 ? 0 - graphValue : graphValue);

         return _formatter.format(
               FORMAT_MM_SS, //
               (timeAbsolute / 60),
               (timeAbsolute % 60))
               .toString();
      }
   }

   private TourManager() {

      final int cacheSize = _prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE);
      if (cacheSize > 0) {

         _tourDataCache = new TourDataCache(cacheSize);

      } else {

         /**
          * VERY IMPORTANT
          * <p>
          * When cache size is 0, each time when the same tour is requested from the tour manager, a
          * new TourData entity is created. So each opened view gets a new tourdata for the same
          * tour which causes LOTs of troubles.
          */
         _tourDataCache = new TourDataCache(10);
      }

      createAvgCallbacks();

      _prefStore.addPropertyChangeListener(propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

            /*
             * multiple tours can have the wrong person for hr zones
             */

            _joined_TourData = null;
            _allLoaded_TourData = null;
         }
      });
   }

   /**
    * @param tourData
    * @param avgTemperature
    * @return Returns <code>true</code> when the tour is modified, otherwise <code>false</code>.
    */
   public static boolean adjustTemperature(final TourData tourData, final int durationTime) {

      final int[] timeSerie = tourData.timeSerie;
      final float[] temperatureSerie = tourData.temperatureSerie;

      // ensure data are available
      if (temperatureSerie == null) {

         TourLogManager.subLog_ERROR(
               String.format(//
                     LOG_TEMP_ADJUST_010_NO_TEMPERATURE_DATA_SERIE,
                     getTourDateTimeShort(tourData)));

         return false;
      }

      if (timeSerie == null) {

         TourLogManager.subLog_ERROR(
               String.format(//
                     LOG_TEMP_ADJUST_011_NO_TIME_DATA_SERIE,
                     getTourDateTimeShort(tourData)));

         return false;
      }

      /*
       * Get initial temperature
       */
      float initialTemperature = Integer.MIN_VALUE;

      for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

         final int relativeTime = timeSerie[serieIndex];

         if (relativeTime > durationTime) {
            initialTemperature = temperatureSerie[serieIndex];
            break;
         }
      }

      // an initial temperature could not be computed because the tour is too short
      if (initialTemperature == Integer.MIN_VALUE) {

         TourLogManager.subLog_ERROR(
               String.format(//
                     LOG_TEMP_ADJUST_005_TOUR_IS_TOO_SHORT,
                     getTourDateTimeShort(tourData)));

         return false;
      }

      /*
       * Adjust temperature
       */
      for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

         final int relativeTime = timeSerie[serieIndex];

         if (relativeTime > durationTime) {
            break;
         }

         temperatureSerie[serieIndex] = initialTemperature;
      }

      final float oldAvgTemperature = tourData.getWeather_Temperature_Average_Device();

      tourData.computeAvg_Temperature();

      final float newAvgTemperature = tourData.getWeather_Temperature_Average_Device();

      TourLogManager.subLog_OK(String.format(
            LOG_TEMP_ADJUST_003_TOUR_CHANGES,
            getTourDateTimeShort(tourData),
            oldAvgTemperature,
            newAvgTemperature,
            newAvgTemperature - oldAvgTemperature));

      return true;
   }

   /**
    * Compares two {@link TourData}
    *
    * @param tourData1
    * @param tourData2
    * @return Returns <code>true</code> when they are the same, otherwise this is an internal error
    * @throws MyTourbookException
    *            throws an exception when {@link TourData} are corrupted
    */
   public static boolean checkTourData(final TourData tourData1, final TourData tourData2) throws MyTourbookException {

      if (tourData1 == null || tourData2 == null) {
         return true;
      }

      final long tourId1 = tourData1.getTourId().longValue();
      final long tourId2 = tourData2.getTourId().longValue();
      final String message = NLS.bind(Messages.TourManager_Dialog_OutOfSyncError_Message,
            tourData2.toStringWithHash(),
            tourData1.toStringWithHash());

      if (tourId1 == tourId2 && tourData1 != tourData2) {

         MessageDialog.openError(Display.getDefault().getActiveShell(),
               Messages.TourManager_Dialog_OutOfSyncError_Title,
               message);

         throw new MyTourbookException(message);
      }

      return true;
   }

   public static void clearMultipleTourData() {

      _joined_TourData = null;
      _allLoaded_TourData = null;
   }

   /**
    * Computes time (seconds) spent in each cadence zone (slow and fast) for several given tours.
    *
    * @param conn
    * @param selectedTours
    * @return Returns <code>true</code> when values are computed or <code>false</code> when nothing
    *         was done.
    * @throws SQLException
    */
   public static boolean computeCadenceZonesTimes(final Connection conn,
                                                  final List<TourData> selectedTours) throws SQLException {
      boolean isUpdated = false;

      try (PreparedStatement stmtUpdate = conn.prepareStatement(cadenceZonesTimes_StatementUpdate)) {

         int numComputedTour = 0;
         int numNotComputedTour = 0;

         // loop over all tours and compute each cadence zone time
         for (final TourData tourData : selectedTours) {

            final boolean timeComputed = tourData.computeCadenceZonesTimes();
            if (!timeComputed) {

               numNotComputedTour++;

            } else {

               // update cadence zones times in the database
               stmtUpdate.setInt(1, tourData.getCadenceZone_SlowTime());
               stmtUpdate.setInt(2, tourData.getCadenceZone_FastTime());
               stmtUpdate.setInt(3, tourData.getCadenceZones_DelimiterValue());
               stmtUpdate.setLong(4, tourData.getTourId());

               stmtUpdate.executeUpdate();

               isUpdated = true;
               numComputedTour++;
            }
         }

         TourLogManager.subLog_OK(NLS.bind(
               Messages.Log_ComputeCadenceZonesTimes_010_Success,
               numComputedTour));

         if (numNotComputedTour >= 0) {
            TourLogManager.subLog_ERROR(NLS.bind(
                  Messages.Log_ComputeCadenceZonesTimes_011_NoSuccess,
                  numNotComputedTour));
         }
      } catch (final SQLException e) {
         SQL.showException(e);
      }

      return isUpdated;
   }

   /**
    * Computes distance values from geo position.
    *
    * @param tourDataList
    * @return Returns <code>true</code> when distance values are computed and {@link TourData} are
    *         updated but not yet saved.
    */
   public static boolean computeDistanceValuesFromGeoPosition(final List<TourData> tourDataList) {

      if (tourDataList == null || tourDataList.isEmpty()) {
         return false;
      }

      if (MessageDialog.openConfirm(
            Display.getCurrent().getActiveShell(),
            Messages.TourEditor_Dialog_ComputeDistanceValues_Title,
            NLS.bind(Messages.TourEditor_Dialog_ComputeDistanceValues_Message, UI.UNIT_LABEL_DISTANCE)) == false) {
         return false;
      }

      final boolean[] retValue = { false };

      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         for (final TourData tourData : tourDataList) {

            final boolean isComputed = computeDistanceValuesFromGeoPosition(tourData);

            if (isComputed) {
               retValue[0] = true;
            }
         }
      });

      return retValue[0];
   }

   public static boolean computeDistanceValuesFromGeoPosition(final TourData tourData) {

      final double[] latSerie = tourData.latitudeSerie;
      final double[] lonSerie = tourData.longitudeSerie;

      if (latSerie == null) {
         return false;
      }

      final float[] distanceSerie = new float[latSerie.length];

      double distance = 0;
      double latStart = latSerie[0];
      double lonStart = lonSerie[0];

      // compute distance for every time slice
      for (int serieIndex = 1; serieIndex < latSerie.length; serieIndex++) {

         final double latEnd = latSerie[serieIndex];
         final double lonEnd = lonSerie[serieIndex];

         /*
          * vincenty algorithm is much more accurate compared with haversine
          */
//         final double distDiff = Util.distanceHaversine(latStart, lonStart, latEnd, lonEnd);
         final double distDiff = MtMath.distanceVincenty(latStart, lonStart, latEnd, lonEnd);

         distance += distDiff;
         distanceSerie[serieIndex] = (float) distance;

         latStart = latEnd;
         lonStart = lonEnd;
      }

      // update tour distance which is displayed in views/tour editor
      tourData.setTourDistance((int) distance);

      // set distance in markers
      final Set<TourMarker> allTourMarker = tourData.getTourMarkers();
      if (allTourMarker != null) {

         for (final TourMarker tourMarker : allTourMarker) {
            final float markerDistance = distanceSerie[tourMarker.getSerieIndex()];
            tourMarker.setDistance(markerDistance);
         }
      }

      tourData.distanceSerie = distanceSerie;

      return true;
   }

   /**
    * @param tourData
    * @param startIndex
    * @param endIndex
    * @return Returns the elapsed time
    */
   public static int computeTourDeviceTime_Elapsed(final TourData tourData, final int startIndex, final int endIndex) {

      final float[] distanceSerie = tourData.getMetricDistanceSerie();
      final int[] timeSerie = tourData.timeSerie;

      if (timeSerie == null
            || timeSerie.length == 0
            || startIndex >= distanceSerie.length
            || endIndex >= distanceSerie.length
            || startIndex > endIndex) {
         return 0;
      }

      return timeSerie[endIndex] - timeSerie[startIndex];
   }

   /**
    * @param tourData
    * @param startIndex
    * @param endIndex
    * @return Returns the metric speed or 0 when not available.
    */
   public static float computeTourSpeed(final TourData tourData, final int startIndex, final int endIndex) {

      final float[] distanceSerie = tourData.getMetricDistanceSerie();
      final int[] timeSerie = tourData.timeSerie;

      if (timeSerie == null
            || timeSerie.length == 0
            || startIndex >= distanceSerie.length
            || endIndex >= distanceSerie.length) {
         return 0;
      }

      final float distance = distanceSerie[endIndex] - distanceSerie[startIndex];
      final int time = Math.max( //
            0,
            timeSerie[endIndex] - timeSerie[startIndex] - tourData.getBreakTime(startIndex, endIndex));

      return time == 0 ? 0 : distance / time * 3.6f;
   }

   /**
    * Create a tour chart configuration by reading the settings from the pref store.
    *
    * @param state
    * @return Returns a new tour chart configuration.
    */
   public static TourChartConfiguration createDefaultTourChartConfig(final IDialogSettings state) {

      final TourChartConfiguration tcc = new TourChartConfiguration(true, state);

      /*
       * convert graph ids from the preferences into visible graphs in the chart panel configuration
       */
      final String[] prefGraphIds = StringToArrayConverter.convertStringToArray(_prefStore.getString(ITourbookPreferences.GRAPH_VISIBLE));

      for (final String prefGraphId : prefGraphIds) {
         tcc.addVisibleGraph(Integer.parseInt(prefGraphId));
      }

      // set the unit which is shown on the x-axis
      final boolean isShowTime = _prefStore.getString(ITourbookPreferences.GRAPH_X_AXIS).equals(X_AXIS_TIME);
      tcc.isShowTimeOnXAxis = isShowTime;
      tcc.isShowTimeOnXAxisBackup = isShowTime;

      final boolean isTourStartTime = _prefStore.getBoolean(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME);
      tcc.xAxisTime = isTourStartTime ? X_AXIS_START_TIME.TOUR_START_TIME : X_AXIS_START_TIME.START_WITH_0;

      tcc.updateZoomOptions();

      return tcc;
   }

   /**
    * Create one {@link TourData} for multiple tours.
    *
    * @param tourIds
    * @return
    */
   public static TourData createJoinedTourData(final List<Long> tourIds) {

      // check if the requested data are already available
      final int tourIds_Hash = tourIds.hashCode();

      if (_joined_TourData != null && tourIds_Hash == _joined_TourIds_Hash) {
         return _joined_TourData;
      }

      final ArrayList<TourData> allMultipleTours = new ArrayList<>();
      final ArrayList<TourData> validatedMultipleTours = new ArrayList<>();

      loadTourData(tourIds, allMultipleTours, false);

      // sort tours by start date/time
      Collections.sort(
            allMultipleTours,
            (tourData1, tourData2) -> tourData1.getTourStartTimeMS() < tourData2.getTourStartTimeMS()
                  ? -1 : 1);

      int numTimeSlices = 0;
      int numSwimTimeSlices = 0;

      // get tours which have data series
      for (final TourData tourData : allMultipleTours) {

         final int[] timeSerie = tourData.timeSerie;

         // ignore tours which have no data series
         if (timeSerie != null && timeSerie.length > 0) {

            validatedMultipleTours.add(tourData);
            numTimeSlices += timeSerie.length;
         }

         final int[] swimTimeSerie = tourData.swim_Time;

         if (swimTimeSerie != null && swimTimeSerie.length > 0) {
            numSwimTimeSlices += swimTimeSerie.length;
         }
      }

      final TourData joinedTourData = new TourData();

      joinedTourData.setupMultipleTour();

      final int numTours = validatedMultipleTours.size();

      final float[] cadenceSerieData = new float[numTimeSlices];
      joinedTourData.setCadenceSerie(cadenceSerieData);

// SET_FORMATTING_OFF

      final int[] toTimeSerie                   = joinedTourData.timeSerie                         = new int[numTimeSlices];
      final float[] toAltitudeSerie             = joinedTourData.altitudeSerie                     = new float[numTimeSlices];
      final float[] toCadenceSerie              = cadenceSerieData;
      final float[] toDistanceSerie             = joinedTourData.distanceSerie                     = new float[numTimeSlices];
      final long[] toGearSerie                  = joinedTourData.gearSerie                         = new long[numTimeSlices];
      final double[] toLatitudeSerie            = joinedTourData.latitudeSerie                     = new double[numTimeSlices];
      final double[] toLongitudeSerie           = joinedTourData.longitudeSerie                    = new double[numTimeSlices];
      final float[] toPowerSerie                = new float[numTimeSlices];
      final float[] toPulseSerie                = joinedTourData.pulseSerie                        = new float[numTimeSlices];
      final float[] toPulseSerie_FromTime       = joinedTourData.pulseSerie_FromTime               = new float[numTimeSlices];
      final float[] toTemperaturSerie           = joinedTourData.temperatureSerie                  = new float[numTimeSlices];

      final short[] toRunDyn_StanceTime         = joinedTourData.runDyn_StanceTime                 = new short[numTimeSlices];
      final short[] toRunDyn_StanceTimeBalance  = joinedTourData.runDyn_StanceTimeBalance          = new short[numTimeSlices];
      final short[] toRunDyn_StepLength         = joinedTourData.runDyn_StepLength                 = new short[numTimeSlices];
      final short[] toRunDyn_VertOscillation    = joinedTourData.runDyn_VerticalOscillation        = new short[numTimeSlices];
      final short[] toRunDyn_VertRatio          = joinedTourData.runDyn_VerticalRatio              = new short[numTimeSlices];

      final short[] toswim_LengthType           = joinedTourData.swim_LengthType                   = new short[numSwimTimeSlices];
      final short[] toSwim_Cadence              = joinedTourData.swim_Cadence                      = new short[numSwimTimeSlices];
      final short[] toSwim_Strokes              = joinedTourData.swim_Strokes                      = new short[numSwimTimeSlices];
      final short[] toSwim_StrokeStyle          = joinedTourData.swim_StrokeStyle                  = new short[numSwimTimeSlices];
      final int[] toSwim_Time                   = joinedTourData.swim_Time                         = new int[numSwimTimeSlices];

      final Long[] allTourIds                   = joinedTourData.multipleTourIds                   = new Long[numTours];
      final float[] allTours_CadenceMultiplier  = joinedTourData.multipleTours_CadenceMultiplier   = new float[numTours];
      final int[] allStartIndex                 = joinedTourData.multipleTourStartIndex            = new int[numTours];
      final ZonedDateTime[] allStartTime        = joinedTourData.multipleTourZonedStartTime        = new ZonedDateTime[numTours];
      final ArrayList<TourMarker> allTourMarker = joinedTourData.multipleTourMarkers               = new ArrayList<>();
      final List<List<Long>> allTourPauses      = joinedTourData.multipleTourPauses                = new ArrayList<>();
      final String[] allTourTitle               = joinedTourData.multipleTourTitles                = new String[numTours];
      final int[] allTourMarkerNumbers          = joinedTourData.multipleNumberOfMarkers           = new int[numTours];
      final int[] allTourPausesNumbers          = joinedTourData.multipleNumberOfPauses            = new int[numTours];
      final int[] allSwimStartIndex             = joinedTourData.multipleSwimStartIndex            = new int[numTours];

// SET_FORMATTING_ON

      final HashSet<TourPhoto> allTourPhoto = new HashSet<>();

      // fixing IndexOutOfBoundsException: Index: 0, Size: 0
      if (numTours == 0) {
         return joinedTourData;
      }

      int toStartIndex = 0;
      int toSwimStartIndex = 0;
      int tourDeviceTime_Elapsed = 0;
      int tourDeviceTime_Recorded = 0;
      int tourDeviceTime_Paused = 0;
      float tourDistance = 0;
      float tourAltUp = 0;
      float tourAltDown = 0;

      boolean isAltitudeSerie = false;
      boolean isCadenceSerie = false;
      boolean isDistanceSerie = false;
      boolean isGearSerie = false;
      boolean isLatLonSerie = false;
      boolean isPowerSerie = false;
      boolean isPulseSerie = false;
      boolean isPulseSerie_FromTime = false;
      boolean isTempSerie = false;

      boolean isRunDyn_StanceTime = false;
      boolean isRunDyn_StanceTimeBalance = false;
      boolean isRunDyn_StepLength = false;
      boolean isRunDyn_VerticalOscillation = false;
      boolean isRunDyn_VerticalRatio = false;

      boolean isswim_LengthType = false;
      boolean isSwim_Cadence = false;
      boolean isSwim_Strokes = false;
      boolean isSwim_StrokeStyle = false;
      boolean isSwim_Time = false;

      boolean isFirstTour = true;

      boolean isCadenceRpm = false;
      boolean isCadenceSpm = false;

      // loop: all tours
      for (int tourIndex = 0; tourIndex < numTours; tourIndex++) {

         final TourData fromTourData = validatedMultipleTours.get(tourIndex);

         final int[] fromTimeSerie = fromTourData.timeSerie;

         final float[] fromAltitudeSerie = fromTourData.altitudeSerie;
         final float[] fromCadenceSerie = fromTourData.getCadenceSerieWithMuliplier();
         final float[] fromDistanceSerie = fromTourData.distanceSerie;
         final long[] fromGearSerie = fromTourData.gearSerie;
         final double[] fromLatitudeSerie = fromTourData.latitudeSerie;
         final double[] fromLongitudeSerie = fromTourData.longitudeSerie;
         final float[] fromPulseSerie = fromTourData.pulseSerie;
         final float[] fromPulse_BpmFromRRIntervals = fromTourData.getPulse_AvgBpmFromRRIntervals();
         final float[] fromTemperaturSerie = fromTourData.temperatureSerie;

         final short[] fromRunDyn_StanceTime = fromTourData.runDyn_StanceTime;
         final short[] fromRunDyn_StanceTimeBalance = fromTourData.runDyn_StanceTimeBalance;
         final short[] fromRunDyn_StepLength = fromTourData.runDyn_StepLength;
         final short[] fromRunDyn_VertOscillation = fromTourData.runDyn_VerticalOscillation;
         final short[] fromRunDyn_VertRatio = fromTourData.runDyn_VerticalRatio;

         final short[] fromswim_LengthType = fromTourData.swim_LengthType;
         final short[] fromSwim_Cadence = fromTourData.swim_Cadence;
         final short[] fromSwim_Strokes = fromTourData.swim_Strokes;
         final short[] fromSwim_StrokeStyle = fromTourData.swim_StrokeStyle;
         final int[] fromSwim_Time = fromTourData.swim_Time;

         final int fromSerieLength = fromTimeSerie.length;
         final int fromSwimSerieLength = fromSwim_Time == null ? 0 : fromSwim_Time.length;

         /*
          * Copy time serie
          */
         if (isFirstTour) {

            // first time serie

            isFirstTour = false;

            // copy data series from the first tour
            System.arraycopy(fromTimeSerie, 0, toTimeSerie, toStartIndex, fromSerieLength);
            if (fromSwim_Time != null) {
               isSwim_Time = true;
               System.arraycopy(fromSwim_Time, 0, toSwim_Time, toSwimStartIndex, fromSwimSerieLength);
            }

            if (fromDistanceSerie != null) {

               isDistanceSerie = true;
               System.arraycopy(fromDistanceSerie, 0, toDistanceSerie, toStartIndex, fromSerieLength);
            }

         } else {

            // 2nd + other time series

            // adjust relative time series
            for (int serieIndex = 0; serieIndex < fromSerieLength; serieIndex++) {
               toTimeSerie[toStartIndex + serieIndex] = tourDeviceTime_Elapsed + fromTimeSerie[serieIndex];
            }
            if (fromSwim_Time != null) {
               isSwim_Time = true;
               for (int swimSerieIndex = 0; swimSerieIndex < fromSwimSerieLength; swimSerieIndex++) {
                  toSwim_Time[toSwimStartIndex + swimSerieIndex] = tourDeviceTime_Elapsed + fromSwim_Time[swimSerieIndex];
               }
            }

            // adjust relative distance
            if (fromDistanceSerie != null) {

               isDistanceSerie = true;

               for (int serieIndex = 0; serieIndex < fromSerieLength; serieIndex++) {
                  toDistanceSerie[toStartIndex + serieIndex] = tourDistance + fromDistanceSerie[serieIndex];
               }

            } else {

               /*
                * Distance serie is not available but fill the distance with the previous distance
                * otherwise the graph has a strange gap and the lines go to x=0
                */
               for (int serieIndex = 0; serieIndex < fromSerieLength; serieIndex++) {
                  toDistanceSerie[toStartIndex + serieIndex] = tourDistance;
               }
            }
         }

         /*
          * Copy other data series
          */
         if (fromAltitudeSerie != null) {
            isAltitudeSerie = true;
            System.arraycopy(fromAltitudeSerie, 0, toAltitudeSerie, toStartIndex, fromSerieLength);
         }

         if (fromCadenceSerie != null) {

            isCadenceSerie = true;
            System.arraycopy(fromCadenceSerie, 0, toCadenceSerie, toStartIndex, fromSerieLength);

            isCadenceRpm |= !fromTourData.isCadenceSpm();
            isCadenceSpm |= fromTourData.isCadenceSpm();
         }

         if (fromGearSerie != null) {
            isGearSerie = true;
            System.arraycopy(fromGearSerie, 0, toGearSerie, toStartIndex, fromSerieLength);
         }
         if (fromLatitudeSerie != null) {
            isLatLonSerie = true;
            System.arraycopy(fromLatitudeSerie, 0, toLatitudeSerie, toStartIndex, fromSerieLength);
            System.arraycopy(fromLongitudeSerie, 0, toLongitudeSerie, toStartIndex, fromSerieLength);
         }
         if (fromPulseSerie != null) {
            isPulseSerie = true;
            System.arraycopy(fromPulseSerie, 0, toPulseSerie, toStartIndex, fromSerieLength);
         }
         if (fromPulse_BpmFromRRIntervals != null) {
            isPulseSerie_FromTime = true;
            System.arraycopy(fromPulse_BpmFromRRIntervals, 0, toPulseSerie_FromTime, toStartIndex, fromSerieLength);
         }
         if (fromTemperaturSerie != null) {
            isTempSerie = true;
            System.arraycopy(fromTemperaturSerie, 0, toTemperaturSerie, toStartIndex, fromSerieLength);
         }

         // power is a special data serie
         if (fromTourData.isPowerSerieFromDevice() && fromTourData.getPowerSerie() != null) {
            isPowerSerie = true;
            System.arraycopy(fromTourData.getPowerSerie(), 0, toPowerSerie, toStartIndex, fromSerieLength);
         }

         /*
          * Running dynamics
          */
         if (fromRunDyn_StanceTime != null) {
            isRunDyn_StanceTime = true;
            System.arraycopy(fromRunDyn_StanceTime, 0, toRunDyn_StanceTime, toStartIndex, fromSerieLength);
         }
         if (fromRunDyn_StanceTimeBalance != null) {
            isRunDyn_StanceTimeBalance = true;
            System.arraycopy(fromRunDyn_StanceTimeBalance, 0, toRunDyn_StanceTimeBalance, toStartIndex, fromSerieLength);
         }
         if (fromRunDyn_StepLength != null) {
            isRunDyn_StepLength = true;
            System.arraycopy(fromRunDyn_StepLength, 0, toRunDyn_StepLength, toStartIndex, fromSerieLength);
         }
         if (fromRunDyn_VertOscillation != null) {
            isRunDyn_VerticalOscillation = true;
            System.arraycopy(fromRunDyn_VertOscillation, 0, toRunDyn_VertOscillation, toStartIndex, fromSerieLength);
         }
         if (fromRunDyn_VertRatio != null) {
            isRunDyn_VerticalRatio = true;
            System.arraycopy(fromRunDyn_VertRatio, 0, toRunDyn_VertRatio, toStartIndex, fromSerieLength);
         }

         /*
          * Swimming
          */
         if (fromswim_LengthType != null) {
            isswim_LengthType = true;
            System.arraycopy(fromswim_LengthType, 0, toswim_LengthType, toSwimStartIndex, fromSwimSerieLength);
         }
         if (fromSwim_Cadence != null) {
            isSwim_Cadence = true;
            System.arraycopy(fromSwim_Cadence, 0, toSwim_Cadence, toSwimStartIndex, fromSwimSerieLength);
         }
         if (fromSwim_Strokes != null) {
            isSwim_Strokes = true;
            System.arraycopy(fromSwim_Strokes, 0, toSwim_Strokes, toSwimStartIndex, fromSwimSerieLength);
         }
         if (fromSwim_StrokeStyle != null) {
            isSwim_StrokeStyle = true;
            System.arraycopy(fromSwim_StrokeStyle, 0, toSwim_StrokeStyle, toSwimStartIndex, fromSwimSerieLength);
         }

         allTourIds[tourIndex] = fromTourData.getTourId();

         // tour marker
         final ArrayList<TourMarker> fromTourMarker = fromTourData.getTourMarkersSorted();
         allTourMarker.addAll(fromTourMarker);
         allTourMarkerNumbers[tourIndex] = fromTourMarker.size();

         // tour pauses
         final long[] pausedTime_Start = fromTourData.getPausedTime_Start();

         if (pausedTime_Start != null) {

            final long[] pausedTime_End = fromTourData.getPausedTime_End();
            final long[] pausedTime_Data = fromTourData.getPausedTime_Data();

            for (int index = 0; index < pausedTime_Start.length; ++index) {

               final List<Long> fromTourPausesList = new ArrayList<>();

               final long pauseData = pausedTime_Data == null

                     // pause data are not available -> it will be displayed as an auto-pause
                     ? -1

                     : pausedTime_Data[index];

               fromTourPausesList.add(pausedTime_Start[index]);
               fromTourPausesList.add(pausedTime_End[index]);
               fromTourPausesList.add(pauseData);

               allTourPauses.add(fromTourPausesList);
            }
            allTourPausesNumbers[tourIndex] = pausedTime_Start.length;
         }

         // photos
         final Set<TourPhoto> fromTourPhotos = fromTourData.getTourPhotos();
         allTourPhoto.addAll(fromTourPhotos);

         /*
          * Keep data for each tour
          */
         allStartIndex[tourIndex] = toStartIndex;
         toStartIndex += fromSerieLength;

         allSwimStartIndex[tourIndex] = toSwimStartIndex;
         toSwimStartIndex += fromSwimSerieLength;

         // summarize tour distance
         if (fromDistanceSerie != null) {
            tourDistance += fromDistanceSerie[fromSerieLength - 1];
         }

         // summarize elapsed time
         final int fromTourEnd = fromTimeSerie[fromSerieLength - 1];
         tourDeviceTime_Elapsed += fromTourEnd;

         // summarize altitude up/down
         tourAltUp += fromTourData.getTourAltUp();
         tourAltDown += fromTourData.getTourAltDown();

         // tour titles
         final long tourStartTime = fromTourData.getTourStartTimeMS();
         allTourTitle[tourIndex] = TimeTools.getZonedDateTime(tourStartTime).format(TimeTools.Formatter_Date_S);
         allStartTime[tourIndex] = fromTourData.getTourStartTime();

         // cadence multiplier
         allTours_CadenceMultiplier[tourIndex] = fromTourData.getCadenceMultiplier();

         /*
          * Add 1 otherwise the next tour has the same start time as the previous tour end time,
          * this is because it starts with 0.
          */
         tourDeviceTime_Elapsed++;

         tourDeviceTime_Recorded += fromTourData.getTourDeviceTime_Recorded();
         tourDeviceTime_Paused += fromTourData.getTourDeviceTime_Paused();
      }

      /*
       * Remove data series when not available
       */
      if (!isAltitudeSerie) {
         joinedTourData.altitudeSerie = null;
      }
      if (!isCadenceSerie) {
         joinedTourData.setCadenceSerie(null);
      }
      if (!isDistanceSerie) {
         joinedTourData.distanceSerie = null;
      }
      if (!isGearSerie) {
         joinedTourData.gearSerie = null;
      }
      if (!isLatLonSerie) {
         joinedTourData.latitudeSerie = null;
         joinedTourData.longitudeSerie = null;
      }
      if (isPowerSerie) {
         joinedTourData.setPowerSerie(toPowerSerie);
      }
      if (!isPulseSerie) {
         joinedTourData.pulseSerie = null;
      }
      if (!isPulseSerie_FromTime) {
         joinedTourData.pulseSerie_FromTime = null;
      }
      if (!isTempSerie) {
         joinedTourData.temperatureSerie = null;
      }

      /*
       * Running dynamics
       */
      if (isRunDyn_StanceTime == false) {
         joinedTourData.clear_RunDyn_StanceTime();
      }
      if (isRunDyn_StanceTimeBalance == false) {
         joinedTourData.clear_RunDyn_StanceTimeBalance();
      }
      if (isRunDyn_StepLength == false) {
         joinedTourData.clear_RunDyn_StepLength();
      }
      if (isRunDyn_VerticalOscillation == false) {
         joinedTourData.clear_RunDyn_VerticalOscillation();
      }
      if (isRunDyn_VerticalRatio == false) {
         joinedTourData.clear_RunDyn_VerticalRatio();
      }

      /*
       * Swimming
       */
      if (isswim_LengthType == false) {
         joinedTourData.clear_swim_LengthType();
      }
      if (isSwim_Cadence == false) {
         joinedTourData.clear_Swim_Cadence();
      }
      if (isSwim_Strokes == false) {
         joinedTourData.clear_Swim_Strokes();
      }
      if (isSwim_StrokeStyle == false) {
         joinedTourData.clear_Swim_StrokeStyle();
      }
      if (isSwim_Time == false) {
         joinedTourData.clear_Swim_Time();
      }

      setupMultiTourMarker(joinedTourData);
      joinedTourData.setTourPhotos(allTourPhoto, null);

      final TourData firstTour = validatedMultipleTours.get(0);
      final ZonedDateTime tourStartTime = TimeTools.getZonedDateTime(firstTour.getTourStartTimeMS());

      joinedTourData.setTourStartTime(tourStartTime);
      joinedTourData.setTourDeviceTime_Elapsed(tourDeviceTime_Elapsed);
      joinedTourData.setTourDeviceTime_Recorded(tourDeviceTime_Recorded);
      joinedTourData.setTourDeviceTime_Paused(tourDeviceTime_Paused);
      joinedTourData.setTourDistance(tourDistance);

      // computing these values is VERY CPU intensive because of the DP algorithm
      joinedTourData.setTourAltUp(tourAltUp);
      joinedTourData.setTourAltDown(tourAltDown);

      joinedTourData.computeTourMovingTime();
      joinedTourData.computeComputedValues();

      joinedTourData.multipleTour_IsCadenceRpm = isCadenceRpm;
      joinedTourData.multipleTour_IsCadenceSpm = isCadenceSpm;

      _joined_TourData = joinedTourData;
      _joined_TourIds_Hash = tourIds.hashCode();

      return joinedTourData;
   }

   /**
    * Create segments for the chart, each tour is a segment. Segments are used to draw different
    * background colors.
    *
    * @param tourData
    * @param chartDataModel
    */
   private static void createStatisticSegments(final TourData tourData, final ChartDataModel chartDataModel) {

      final ChartDataXSerie xData = chartDataModel.getXData();
      final double[] xValues = xData.getHighValuesDouble()[0];

      if (xValues == null) {
         return;
      }

      final int[] multipleTourStartIndex = tourData.multipleTourStartIndex;
      final int[] timeSerie = tourData.timeSerie;

      final int numberOfTours = multipleTourStartIndex.length;

      final double[] segmentStartValue = new double[numberOfTours];
      final double[] segmentEndValue = new double[numberOfTours];

      for (int tourIndex = 0; tourIndex < numberOfTours; tourIndex++) {

         final int tourStartIndex = multipleTourStartIndex[tourIndex];

         int tourEndIndex;

         if (tourIndex == numberOfTours - 1) {

            // last tour
            tourEndIndex = timeSerie.length - 1;

         } else {
            tourEndIndex = multipleTourStartIndex[tourIndex + 1] - 1;
         }

         if (tourEndIndex == -1) {
            tourEndIndex = tourStartIndex;
         }

         segmentStartValue[tourIndex] = xValues[tourStartIndex];
         segmentEndValue[tourIndex] = xValues[tourEndIndex];
      }

      final ChartStatisticSegments chartSegments = new ChartStatisticSegments();

      chartSegments.segmentStartValue = segmentStartValue;
      chartSegments.segmentEndValue = segmentEndValue;

      chartSegments.segmentTitle = tourData.multipleTourTitles;
      chartSegments.segmentCustomData = tourData.multipleTourIds;

      xData.setChartSegments(chartSegments);
   }

   public static void fireEvent(final TourEventId tourEventId) {

      final Object[] allListeners = _tourEventListeners.getListeners();
      for (final Object listener : allListeners) {
         fireEvent_Final((ITourEventListener) listener, tourEventId, null, null);
      }
   }

   public static void fireEvent(final TourEventId tourEventId, final IWorkbenchPart part) {

      final Object[] allListeners = _tourEventListeners.getListeners();
      for (final Object listener : allListeners) {
         fireEvent_Final((ITourEventListener) listener, tourEventId, part, null);
      }
   }

   public static void fireEvent(final TourEventId tourEventId, final TourEvent tourEvent) {

      final Object[] allListeners = _tourEventListeners.getListeners();
      for (final Object listener : allListeners) {
         fireEvent_Final((ITourEventListener) listener, tourEventId, null, tourEvent);
      }
   }

   public static void fireEvent(final TourEventId tourEventId, final TourEvent tourEvent, final IWorkbenchPart part) {

      final Object[] allListeners = _tourEventListeners.getListeners();
      for (final Object listener : allListeners) {
         fireEvent_Final((ITourEventListener) listener, tourEventId, part, tourEvent);
      }
   }

   /**
    * Fire {@link TourEventId} and check if the fired event also must do some actions in the
    * {@link TourManager}.
    *
    * @param listener
    * @param tourEventId
    * @param part
    * @param customData
    */
   private static void fireEvent_Final(final ITourEventListener listener,
                                       final TourEventId tourEventId,
                                       final IWorkbenchPart part,
                                       final Object customData) {

      if (tourEventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

         _joined_TourData = null;
      }

      listener.tourChanged(part, tourEventId, customData);
   }

   public static void fireEventWithCustomData(final TourEventId tourEventId,
                                              final Object customData,
                                              final IWorkbenchPart part) {

//      System.out.println(
//            (UI.timeStampNano() + " [" + TourManager.class.getSimpleName() + "] fireEventWithCustomData()()")
//                  + ("\ttourEventId: " + tourEventId)
//                  + ("\tpart: " + part)
//                  + ("\tcustomData: " + customData)
////            + ("\t: " + )
//      );

      final Object[] allListeners = _tourEventListeners.getListeners();
      for (final Object listener : allListeners) {
         fireEvent_Final((ITourEventListener) listener, tourEventId, part, customData);
      }
   }

   /**
    * Generates a string containing the percentages of time spent in each cadence zone ("slow" vs
    * "fast")
    *
    * @param cadenceZoneSlowTime
    *           The time spent (in seconds) in the "slow" cadence zone.
    * @param cadenceZoneFastTime
    *           The time spent (in seconds) in the "fast" cadence zone.
    * @return Returns a string of this format : "33 - 64"
    */
   public static String generateCadenceZones_TimePercentages(final int cadenceZoneSlowTime, final int cadenceZoneFastTime) {

      String cadenceZonesPercentages = UI.EMPTY_STRING;

      final int totalCadenceTime = cadenceZoneSlowTime + cadenceZoneFastTime;
      if (totalCadenceTime > 0) {

         final int cadenceZone_SlowPercentage = Math.round(cadenceZoneSlowTime * 100f / totalCadenceTime);
         final int cadenceZone_FastPercentage = Math.round(cadenceZoneFastTime * 100f / totalCadenceTime);

         cadenceZonesPercentages = cadenceZone_SlowPercentage + " - " + cadenceZone_FastPercentage; //$NON-NLS-1$
      }

      return cadenceZonesPercentages;
   }

   /**
    * Try to get the tour chart and/or editor from the active part.
    *
    * @param tourData
    * @return Returns the {@link TourChart} for the requested {@link TourData}
    */
   public static TourChart getActiveTourChart(final TourData tourData) {

      // get tour chart from the active editor part
      for (final IWorkbenchWindow wbWindow : PlatformUI.getWorkbench().getWorkbenchWindows()) {
         for (final IWorkbenchPage wbPage : wbWindow.getPages()) {

            final IEditorPart activeEditor = wbPage.getActiveEditor();
            if (activeEditor instanceof TourEditor) {

               /*
                * check if the tour data in the editor is the same
                */
               final TourChart tourChart = ((TourEditor) activeEditor).getTourChart();
               final TourData tourChartTourData = tourChart.getTourData();
               if (tourChartTourData == tourData) {

                  try {
                     checkTourData(tourData, tourChartTourData);
                  } catch (final MyTourbookException e) {
                     e.printStackTrace();
                  }
               }
            }
         }
      }

      // get tour chart from the tour chart view
      for (final IWorkbenchWindow wbWindow : PlatformUI.getWorkbench().getWorkbenchWindows()) {
         for (final IWorkbenchPage wbPage : wbWindow.getPages()) {

            final IViewReference viewRef = wbPage.findViewReference(TourChartView.ID);
            if (viewRef != null) {

               final IViewPart view = viewRef.getView(false);
               if (view instanceof TourChartView) {

                  final TourChartView tourChartView = ((TourChartView) view);

                  /*
                   * check if the tour data in the tour chart is the same
                   */
                  final TourChart tourChart = tourChartView.getTourChart();
                  final TourData tourChartTourData = tourChart.getTourData();
                  if (tourChartTourData == tourData) {
                     try {
                        checkTourData(tourData, tourChartTourData);
                     } catch (final MyTourbookException e) {
                        e.printStackTrace();
                     }

                     return tourChart;
                  }
               }
            }
         }
      }

      return null;
   }

   public static int[] getAllGraphIDs() {
      return _allGraphIDs;
   }

   /**
    * Get color for a graph from the common pref store.
    *
    * @param graphName
    * @param colorProfileName
    *           Can be any of <br>
    *           {@link GraphColorManager#PREF_COLOR_GRADIENT_BRIGHT},<br>
    *           {@link GraphColorManager#PREF_COLOR_GRADIENT_DARK}<br>
    *           {@link GraphColorManager#PREF_COLOR_LINE_LIGHT}<br>
    *           {@link GraphColorManager#PREF_COLOR_MAPPING}<br>
    *           {@link GraphColorManager#PREF_COLOR_TEXT_LIGHT}.
    * @return
    */
   public static RGB getGraphColor(final String graphName, final String colorProfileName) {

      final String prefGraphName = ICommonPreferences.GRAPH_COLORS + graphName + UI.SYMBOL_DOT;

      final RGB color = PreferenceConverter.getColor(_prefStore_Common, prefGraphName + colorProfileName);

      return color;
   }

   public static TourManager getInstance() {

      if (_instance == null) {
         _instance = new TourManager();
      }

      return _instance;
   }

   public static IValueLabelProvider getLabelProviderInt() {
      return _labelProviderInt;
   }

   public static IValueLabelProvider getLabelProviderMMSS() {
      return _labelProviderMMSS;
   }

   /**
    * Searches all tour providers in the workbench and return tours which are selected.
    *
    * @return Returns tour id's or <code>null</code> when tours are not found
    */
   public static ArrayList<TourData> getSelectedTours() {

      return getSelectedTours(false);
   }

   /**
    * @param isOnlyGeoTour
    *           When <code>true</code> then only tours with latitude/longitude will be returned,
    *           otherwise all tours will be returned.
    * @return
    */
   public static ArrayList<TourData> getSelectedTours(final boolean isOnlyGeoTour) {

      final IWorkbenchWindow[] wbWindows = PlatformUI.getWorkbench().getWorkbenchWindows();

      // get all tourProviders
      for (final IWorkbenchWindow wbWindow : wbWindows) {

         final IWorkbenchPage[] pages = wbWindow.getPages();
         for (final IWorkbenchPage wbPage : pages) {

            final IViewReference[] viewRefs = wbPage.getViewReferences();
            for (final IViewReference viewRef : viewRefs) {

               final IViewPart view = viewRef.getView(false);
               if (view instanceof ITourProvider) {

                  final ITourProvider tourProvider = (ITourProvider) view;
                  final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();

                  if (selectedTours != null) {

                     if (isOnlyGeoTour) {

                        // return only geo tours

                        final ArrayList<TourData> geoTours = new ArrayList<>();

                        for (final TourData tourData : selectedTours) {

                           final double[] latitudeSerie = tourData.latitudeSerie;

                           if (latitudeSerie != null && latitudeSerie.length > 0) {
                              geoTours.add(tourData);
                           }
                        }

                        if (geoTours.size() > 0) {
                           return geoTours;
                        }

                     } else {

                        // return all tours

                        if (selectedTours.size() > 0) {

                           /*
                            * a tour provider is found which also provides tours
                            */
                           return selectedTours;
                        }
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   public static ISelection getSelectedToursSelection() {

      ISelection selection = null;

      final ArrayList<TourData> selectedTours = getSelectedTours();

      if (selectedTours != null && selectedTours.size() > 0) {

         if (selectedTours.size() > 1) {

            // convert tourdata into tour id's because this is better 'consumed'

            final ArrayList<Long> tourIds = new ArrayList<>();
            for (final TourData tourData : selectedTours) {
               tourIds.add(tourData.getTourId());
            }

            selection = new SelectionTourIds(tourIds);

         } else {

            selection = new SelectionTourData(selectedTours.get(0));
         }
      }

      return selection;
   }

   /**
    * Get a tour from the cache, the cache is necessary because getting a tour from the database
    * creates always a new instance.
    * <p>
    * This is a shortcut for {@link TourManager.getInstance().getTourData(tourId)}.
    *
    * @param requestedTourId
    * @return Returns the tour data for the tour id or <code>null</code> when tour is not in the
    *         database.
    */
   public static TourData getTour(final Long requestedTourId) {

      return getInstance().getTourData(requestedTourId);
   }

   /**
    * @return Returns the instance of the {@link TourDataEditorView} or <code>null</code> when this
    *         part is not opened
    */
   public static TourDataEditorView getTourDataEditor() {
      return _tourDataEditorInstance;
   }

   public static String getTourDateFull(final TourData tourData) {

      return tourData.getTourStartTime().format(TimeTools.Formatter_Date_F);
   }

   private static String getTourDateLong(final ZonedDateTime tourDate) {

      return tourDate.format(TimeTools.Formatter_Date_L);
   }

   /**
    * @return returns the date of this tour
    */
   public static String getTourDateShort(final TourData tourData) {

      if (tourData == null) {
         return UI.EMPTY_STRING;
      }

      return tourData.getTourStartTime().format(TimeTools.Formatter_Date_S);
   }

   /**
    * @return Returns a tour title which contains: full date + full time
    */
   public static String getTourDateTimeFull(final TourData tourData) {

      return getTourDateFull(tourData) //
            + UI.DASH_WITH_SPACE
            + getTourTimeShort(tourData);
   }

   public static String getTourDateTimeShort(final TourData tourData) {

      return tourData.getTourStartTime().format(TimeTools.Formatter_DateTime_S);
   }

   /**
    * @return returns the date of this tour
    */
   public static String getTourTimeShort(final TourData tourData) {

      return tourData.getTourStartTime().format(TimeTools.Formatter_Time_S);
   }

   private static String getTourTimeShort(final ZonedDateTime tourDate) {

      return tourDate.format(TimeTools.Formatter_Time_S);
   }

   /**
    * @return returns the title of this tour
    */
   public static String getTourTitle(final TourData tourData) {

      if (tourData == null) {
         return UI.EMPTY_STRING;
      }

      return getTourDateLong(tourData.getTourStartTime())//
            + UI.DASH_WITH_SPACE
            + getTourTimeShort(tourData);
   }

   public static String getTourTitle(final ZonedDateTime tourDateTime) {

      final String weekDay = tourDateTime.format(TimeTools.Formatter_Weekday_L);

      return weekDay //
            + UI.COMMA_SPACE
            + getTourDateLong(tourDateTime)
            + UI.DASH_WITH_SPACE
            + getTourTimeShort(tourDateTime);
   }

   /**
    * @return returns the detailed title of this tour which contains:<br>
    *         date + time + title as it is displayed in the tour chart
    */
   public static String getTourTitleDetailed(final TourData tourData) {

      if (tourData == null) {
         return UI.EMPTY_STRING;
      }

      final String tourTitle = tourData.getTourTitle();

      return getTourDateFull(tourData)
            + UI.DASH_WITH_SPACE
            + getTourTimeShort(tourData)
            + ((tourTitle.length() == 0) ? UI.EMPTY_STRING : UI.DASH_WITH_SPACE + tourTitle);
   }

   /**
    * @param tourData
    * @return Returns the tour title for multiple tours
    */
   public static String getTourTitleMultiple(final TourData tourData) {

      final String[] multipleTourTitles = tourData.multipleTourTitles;

      if (multipleTourTitles == null || multipleTourTitles.length < 2) {
         return UI.EMPTY_STRING;
      }

      final String firstTour = multipleTourTitles[0];
      final String lastTour = multipleTourTitles[multipleTourTitles.length - 1];

      return firstTour + UI.DASH_WITH_SPACE + lastTour;
   }

   public static Object[] getTourViewerSelectedTourIds(final ITourViewer3 tourViewer) {

      Object[] selectedItems = null;

      if (tourViewer instanceof TourBookView) {

         selectedItems = (((TourBookView) tourViewer).getSelectedTourIDs()).toArray();

      } else if (tourViewer instanceof CollatedToursView) {

         selectedItems = (((CollatedToursView) tourViewer).getSelectedTourIDs()).toArray();

      } else if (tourViewer instanceof RawDataView) {

         selectedItems = (((RawDataView) tourViewer).getSelectedTourIDs()).toArray();
      }

      return selectedItems;
   }

   /**
    * Checks if {@link TourData} can be painted
    *
    * @param tourData
    * @return <code>true</code> when {@link TourData} contains a tour which can be painted in the
    *         map
    */
   public static boolean isLatLonAvailable(final TourData tourData) {

      if (tourData == null) {
         return false;
      }

      // check if coordinates are available

      final double[] longitudeSerie = tourData.longitudeSerie;
      final double[] latitudeSerie = tourData.latitudeSerie;

      if ((longitudeSerie == null)
            || (longitudeSerie.length == 0)
            || (latitudeSerie == null)
            || (latitudeSerie.length == 0)) {

         return false;
      }

      return true;
   }

   /**
    * @return <code>true</code> when the user has validated the SRTM server account
    */
   private static boolean isSrtmAccountValid() {

      String message = null;
      String focusField = null;

      final String password = _prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_PASSWORD);
      final String username = _prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_USER_NAME);

      if (password.trim().length() == 0 || username.trim().length() == 0) {

         message = Messages.SRTM_Download_Info_UsernamePasswordIsEmpty;
         focusField = PrefPageSRTMData.FOCUS_USER_NAME;
      }

      final long validationDate = _prefStore.getLong(IPreferences.NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE);
      if (message == null && validationDate < 0) {

         message = Messages.SRTM_Download_Info_NoDownloadValidation;
         focusField = PrefPageSRTMData.FOCUS_VALIDATE_DOWNLOAD;
      }

      if (message != null && message.length() > 0) {

         // SRTM download is not valid

         final Shell shell = Display.getCurrent().getActiveShell();

         MessageDialog.openInformation(shell, Messages.SRTM_Download_Dialog_SRTMDownloadValidation_Title, message);

         // show SRTM pref page
         PreferencesUtil.createPreferenceDialogOn(
               shell,
               PrefPageSRTMData.ID,
               null,

               // set focus to a control
               focusField).open();

         return false;
      }

      return true;
   }

   /**
    * Checks if a tour in the {@link TourDataEditorView} is modified and shows the editor when it's
    * modified. A message dialog informs the user about the modified tour and that the requested
    * actions cannot be done.
    *
    * @return Returns <code>true</code> when the tour is modified in the {@link TourDataEditorView}
    */
   public static boolean isTourEditorModified() {
      return isTourEditorModified(true);
   }

   /**
    * Checks if a tour in the {@link TourDataEditorView} is modified and shows the editor when it's
    * modified. A message dialog informs the user about the modified tour and that the requested
    * actions cannot be done.
    *
    * @param isOpenEditor
    *           When <code>true</code> then the tour editor is displayed.
    * @return Returns <code>true</code> when the tour is modified in the {@link TourDataEditorView}
    */
   public static boolean isTourEditorModified(final boolean isOpenEditor) {

      final TourDataEditorView tourDataEditor = getTourDataEditor();
      if (tourDataEditor != null && tourDataEditor.isDirty()) {

         if (isOpenEditor) {
            openTourEditor(true);
         }

         MessageDialog.openInformation(
               Display.getCurrent().getActiveShell(),
               Messages.dialog_is_tour_editor_modified_title,
               Messages.dialog_is_tour_editor_modified_message);

         return true;
      }

      return false;
   }

   /**
    * @return Returns <code>true</code> when a weather provider has been selected
    *         and properly configured.
    */
   public static boolean isWeatherRetrievalActivated() {

      final String weatherProviderId = _prefStore.getString(ITourbookPreferences.WEATHER_WEATHER_PROVIDER_ID);

      switch (weatherProviderId) {

      case IWeatherProvider.WEATHER_PROVIDER_OPENWEATHERMAP_ID:
      case IWeatherProvider.WEATHER_PROVIDER_WEATHERAPI_ID:
         return true;
      case IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE_ID:
         return StringUtils.hasContent(_prefStore.getString(ITourbookPreferences.WEATHER_API_KEY));
      case IWeatherProvider.Pref_Weather_Provider_None:
      default:
         return false;
      }
   }

   /**
    * Loads multiple tour data from the database and shows a progressbar when it takes longer.
    *
    * @param allTourIds
    * @param allTourData
    *           Contains loaded {@link TourData} for all tour ids which pass the lat/lon check.
    * @param isCheckLatLon
    *           When <code>true</code> only tours with lat/lon will be returned, otherwise all tours
    *           will be returned.
    * @return Returns a unique key for all {@link TourData}.
    */
   public static long loadTourData(final List<Long> allTourIds,
                                   final List<TourData> allTourData,
                                   final boolean isCheckLatLon) {

      // check if the requested data are already available
      final int allTourIds_Hash = allTourIds.hashCode();
      final int allTourData_Hash = allTourData.hashCode();

      if (_allLoaded_TourData != null
            && allTourIds_Hash == _allLoaded_TourIds_Hash
            && allTourData_Hash == _allLoaded_TourData_Hash) {

         allTourData.clear();
         allTourData.addAll(_allLoaded_TourData);

         return _allLoaded_TourData_Key;
      }

      allTourData.clear();

      final long startTime = System.currentTimeMillis();
      boolean isLongDuration = false;

      // create a unique key for all tours
      final AtomicLong newOverlayKey = new AtomicLong();
      final int[] tourIndex = { 0 };
      final int numTourIds = allTourIds.size();

      while (tourIndex[0] < numTourIds) {

         final Long tourId = allTourIds.get(tourIndex[0]);
         loadTourData_OneTour(tourId, allTourData, isCheckLatLon, newOverlayKey);

         /*
          * Check if this is a long duration -> run with progress monitor and concurrent
          */
         final long runDuration = System.currentTimeMillis() - startTime;
         if (runDuration > 1000) {
            isLongDuration = true;
            ++tourIndex[0];
            break;
         }

         ++tourIndex[0];
      }

      if (isLongDuration && tourIndex[0] < numTourIds) {

         try {

            final IRunnableWithProgress runnable = new IRunnableWithProgress() {
               @Override
               public void run(final IProgressMonitor monitor)
                     throws InvocationTargetException, InterruptedException {

                  /*
                   * Setup concurrency
                   */
                  _loadingTour_CountDownLatch = new CountDownLatch(numTourIds - tourIndex[0]);
                  _loadingTour_Queue.clear();

                  final ConcurrentHashMap<Long, TourData> allTourData_Concurrent = new ConcurrentHashMap<>();

                  /*
                   * Setup monitor
                   */
                  long lastUpdateTime = startTime;

                  final AtomicInteger numWorkedTours = new AtomicInteger(tourIndex[0]);
                  int numLastWorked = 0;

                  monitor.beginTask(Messages.Tour_Data_LoadTourData_Monitor, numTourIds);

                  while (tourIndex[0] < numTourIds) {

                     if (monitor.isCanceled()) {

                        /*
                         * Count down all, that the loading task can finish but process loaded tours
                         */
                        long numCounts = _loadingTour_CountDownLatch.getCount();
                        while (numCounts-- > 0) {
                           _loadingTour_CountDownLatch.countDown();
                        }

                        break;
                     }

                     /*
                      * Update monitor
                      */
                     final long currentTime = System.currentTimeMillis();
                     final long timeDiff = currentTime - lastUpdateTime;

                     // reduce logging
                     if (timeDiff > 500) {

                        lastUpdateTime = currentTime;

                        final int numWorked = numWorkedTours.get();

                        // "{0} / {1} - {2} % - {3} Δ"
                        UI.showWorkedInProgressMonitor(monitor, numWorked, numTourIds, numLastWorked);

                        numLastWorked = numWorked;
                     }

                     final Long tourId = allTourIds.get(tourIndex[0]);

                     loadTourData_Concurrent(

                           tourId,
                           allTourData_Concurrent,
                           numWorkedTours,
                           isCheckLatLon,
                           newOverlayKey,
                           monitor);

                     ++tourIndex[0];
                  }

                  // wait until all loadings are performed
                  _loadingTour_CountDownLatch.await();

                  // add new loaded tours
                  for (final Entry<Long, TourData> entry : allTourData_Concurrent.entrySet()) {
                     allTourData.add(entry.getValue());
                  }
               }
            };

            /*
             * Ensure to run in the app shell that a slideoutshell can get hidden without hiding the
             * progress dialog, complicated !
             */
            new ProgressMonitorDialog(TourbookPlugin.getAppShell()).run(true, true, runnable);

         } catch (final InvocationTargetException | InterruptedException e) {
            StatusUtil.showStatus(e);
            Thread.currentThread().interrupt();
         }
      }

      _allLoaded_TourIds_Hash = allTourIds.hashCode();
      _allLoaded_TourData = allTourData;
      _allLoaded_TourData_Hash = allTourData.hashCode();
      _allLoaded_TourData_Key = newOverlayKey.get();

      return _allLoaded_TourData_Key;
   }

   private static void loadTourData_Concurrent(final Long tourId,
                                               final ConcurrentHashMap<Long, TourData> allTourData_Concurrent,
                                               final AtomicInteger numWorkedTours,
                                               final boolean isCheckLatLon,
                                               final AtomicLong newOverlayKey,
                                               final IProgressMonitor monitor) {

      try {

         // put tour ID (queue item) into the queue AND wait when it is full

         _loadingTour_Queue.put(tourId);

      } catch (final InterruptedException e) {

         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      _loadingTour_Executor.submit(() -> {

         try {

            // get last added item
            final Long queueItem_TourId = _loadingTour_Queue.poll();

            if (queueItem_TourId != null) {

               loadTourData_OneTour_Concurrent(

                     queueItem_TourId,
                     allTourData_Concurrent,
                     isCheckLatLon,
                     newOverlayKey);
            }

         } finally {

            if (monitor != null) {
               monitor.worked(1);
            }

            numWorkedTours.incrementAndGet();

            _loadingTour_CountDownLatch.countDown();
         }
      });
   }

   private static void loadTourData_OneTour(final Long tourId,
                                            final List<TourData> allTourData,
                                            final boolean isCheckLatLon,
                                            final AtomicLong newOverlayKey) {

      final TourData tourData = getInstance().getTourData(tourId);

      if (tourData == null) {

         // this happened when switching tour type during normal startup but all was not yet loaded

         return;
      }

      if (isCheckLatLon == false || isLatLonAvailable(tourData)) {

         // keep tour data for each tour id
         allTourData.add(tourData);

         // update key for all tours
         newOverlayKey.getAndAdd(tourData.getTourId());
      }
   }

   private static void loadTourData_OneTour_Concurrent(final Long tourId,
                                                       final ConcurrentHashMap<Long, TourData> allTourData,
                                                       final boolean isCheckLatLon,
                                                       final AtomicLong newOverlayKey) {

      final TourData tourData = getInstance().getTourData(tourId);

      if (tourData == null) {

         // this happened when switching tour type during normal startup but all was not yet loaded

         return;
      }

      if (isCheckLatLon == false || isLatLonAvailable(tourData)) {

         // keep tour data for each tour id
         allTourData.put(tourData.getTourId(), tourData);

         // update key for all tours
         newOverlayKey.getAndAdd(tourData.getTourId());
      }
   }

   /**
    * @param isActive
    * @return
    */
   public static TourDataEditorView openTourEditor(final boolean isActive) {

      final TourDataEditorView[] tourDataEditorView = { null };

      /*
       * must be run in the UI thread because PlatformUI.getWorkbench().getActiveWorkbenchWindow()
       * returns null in none UI threads
       */
      Display.getDefault().syncExec(() -> {

         try {

            final IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

            final IWorkbenchPage page = activeWorkbenchWindow.getActivePage();

            final IViewPart viewPart = page.showView(TourDataEditorView.ID, null, IWorkbenchPage.VIEW_VISIBLE);

            if (viewPart instanceof TourDataEditorView) {

               tourDataEditorView[0] = (TourDataEditorView) viewPart;

               if (isActive) {

                  page.showView(TourDataEditorView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

               } else if (page.isPartVisible(viewPart) == false || isActive) {

                  page.bringToTop(viewPart);
               }

// HINT: this does not restore the part when it's in a fast view
//
//         final IWorkbenchPartReference partRef = page.getReference(viewPart);
//         final int partState = page.getPartState(partRef);
//         page.setPartState(partRef, IWorkbenchPage.STATE_MAXIMIZED);
//         page.setPartState(partRef, IWorkbenchPage.STATE_RESTORED);

            }

         } catch (final PartInitException e) {
            StatusUtil.log(e);
         }
      });

      return tourDataEditorView[0];
   }

   /**
    * Remove time slices from {@link TourData}
    *
    * @param tourData
    * @param firstIndex
    * @param lastIndex
    * @param isRemoveTime
    * @param isAdjustTourStartTime
    */
   public static void removeTimeSlices(final TourData tourData,
                                       final int firstIndex,
                                       final int lastIndex,
                                       final boolean isRemoveTime,
                                       final boolean isAdjustTourStartTime) {

      // this must be done with the original timeSerie
      removeTourPauses(tourData, firstIndex, lastIndex, isRemoveTime);

      // this must be done before the time series are modified
      removeTimeSlices_TimeAndDistance(
            tourData,
            firstIndex,
            lastIndex,
            isRemoveTime,
            isAdjustTourStartTime);

      short[] shortSerie;
      int[] intSerie;
      long[] longSerie;
      float[] floatSerie;
      double[] doubleSerie;

      floatSerie = tourData.altitudeSerie;
      if (floatSerie != null) {
         tourData.altitudeSerie = removeTimeSlices_Float(floatSerie, firstIndex, lastIndex);
      }

      floatSerie = tourData.getCadenceSerie();
      if (floatSerie != null) {
         tourData.setCadenceSerie(removeTimeSlices_Float(floatSerie, firstIndex, lastIndex));
      }

      floatSerie = tourData.distanceSerie;
      if (floatSerie != null) {
         tourData.distanceSerie = removeTimeSlices_Float(floatSerie, firstIndex, lastIndex);
      }

      longSerie = tourData.gearSerie;
      if (longSerie != null) {
         tourData.setGears(removeTimeSlices_Long(longSerie, firstIndex, lastIndex));
      }

      floatSerie = tourData.pulseSerie;
      if (floatSerie != null) {
         tourData.pulseSerie = removeTimeSlices_Float(floatSerie, firstIndex, lastIndex);
      }

      floatSerie = tourData.temperatureSerie;
      if (floatSerie != null) {
         tourData.temperatureSerie = removeTimeSlices_Float(floatSerie, firstIndex, lastIndex);
      }

      intSerie = tourData.timeSerie;
      if (intSerie != null) {

         tourData.timeSerie = removeTimeSlices_Integer(intSerie, firstIndex, lastIndex);

         // time double serie is cleaned up further down in tourData.clearComputedSeries();
      }

      doubleSerie = tourData.latitudeSerie;
      if (doubleSerie != null) {
         tourData.latitudeSerie = removeTimeSlices_Double(doubleSerie, firstIndex, lastIndex);
      }
      doubleSerie = tourData.longitudeSerie;
      if (doubleSerie != null) {
         tourData.longitudeSerie = removeTimeSlices_Double(doubleSerie, firstIndex, lastIndex);

         tourData.computeGeo_Bounds();
      }

      /*
       * Running dynamics
       */
      shortSerie = tourData.runDyn_StanceTime;
      if (shortSerie != null) {
         tourData.runDyn_StanceTime = removeTimeSlices_Short(shortSerie, firstIndex, lastIndex);
      }
      shortSerie = tourData.runDyn_StanceTimeBalance;
      if (shortSerie != null) {
         tourData.runDyn_StanceTimeBalance = removeTimeSlices_Short(shortSerie, firstIndex, lastIndex);
      }
      shortSerie = tourData.runDyn_StepLength;
      if (shortSerie != null) {
         tourData.runDyn_StepLength = removeTimeSlices_Short(shortSerie, firstIndex, lastIndex);
      }
      shortSerie = tourData.runDyn_VerticalOscillation;
      if (shortSerie != null) {
         tourData.runDyn_VerticalOscillation = removeTimeSlices_Short(shortSerie, firstIndex, lastIndex);
      }
      shortSerie = tourData.runDyn_VerticalRatio;
      if (shortSerie != null) {
         tourData.runDyn_VerticalRatio = removeTimeSlices_Short(shortSerie, firstIndex, lastIndex);
      }

      /*
       * get speed/power data when it's from the device
       */
      final boolean isDevicePowerSerie = tourData.isPowerSerieFromDevice();
      final boolean isDeviceSpeedSerie = tourData.isSpeedSerieFromDevice();
      if (isDevicePowerSerie) {
         floatSerie = tourData.getPowerSerie();
         if (floatSerie != null) {
            tourData.setPowerSerie(removeTimeSlices_Float(floatSerie, firstIndex, lastIndex));
         }
      }
      if (isDeviceSpeedSerie) {
         floatSerie = tourData.getSpeedSerieFromDevice();
         if (floatSerie != null) {
            tourData.setSpeedSerie(removeTimeSlices_Float(floatSerie, firstIndex, lastIndex));
         }
      }

      // reset computed data series and clear cached world positions
      tourData.clearComputedSeries();
      tourData.clearWorldPositions();

      // segments must be recomputed
      tourData.segmentSerieIndex = null;
      tourData.segmentSerieIndex2nd = null;

      removeTourMarkers(tourData, firstIndex, lastIndex, isRemoveTime);
   }

   private static double[] removeTimeSlices_Double(final double[] dataSerie,
                                                   final int firstIndex,
                                                   final int lastIndex) {

      final int oldSerieLength = dataSerie.length;
      final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

      final double[] newDataSerie = new double[newSerieLength];

      if (firstIndex == 0) {

         // delete from start, copy data by skipping removed slices
         System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, 0, newSerieLength);

      } else if (lastIndex == oldSerieLength - 1) {

         // delete until the end
         System.arraycopy(dataSerie, 0, newDataSerie, 0, newSerieLength);

      } else {

         // delete somewhere in the middle

         // copy start segment
         System.arraycopy(dataSerie, 0, newDataSerie, 0, firstIndex);

         // copy end segment
         final int copyLength = oldSerieLength - (lastIndex + 1);
         System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, firstIndex, copyLength);
      }

      return newDataSerie;
   }

   private static float[] removeTimeSlices_Float(final float[] dataSerie, final int firstIndex, final int lastIndex) {

      final int oldSerieLength = dataSerie.length;
      final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

      final float[] newDataSerie = new float[newSerieLength];

      if (firstIndex == 0) {

         // delete from start, copy data by skipping removed slices
         System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, 0, newSerieLength);

      } else if (lastIndex == oldSerieLength - 1) {

         // delete until the end
         System.arraycopy(dataSerie, 0, newDataSerie, 0, newSerieLength);

      } else {

         // delete somewhere in the middle

         // copy start segment
         System.arraycopy(dataSerie, 0, newDataSerie, 0, firstIndex);

         // copy end segment
         final int copyLength = oldSerieLength - (lastIndex + 1);
         System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, firstIndex, copyLength);
      }

      return newDataSerie;
   }

   private static int[] removeTimeSlices_Integer(final int[] oldDataSerie, final int firstIndex, final int lastIndex) {

      final int oldSerieLength = oldDataSerie.length;
      final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

      final int[] newDataSerie = new int[newSerieLength];

      if (firstIndex == 0) {

         // delete from start, copy data by skipping removed slices
         System.arraycopy(oldDataSerie, lastIndex + 1, newDataSerie, 0, newSerieLength);

      } else if (lastIndex == oldSerieLength - 1) {

         // delete until the end
         System.arraycopy(oldDataSerie, 0, newDataSerie, 0, newSerieLength);

      } else {

         // delete somewhere in the middle

         // copy start segment
         System.arraycopy(oldDataSerie, 0, newDataSerie, 0, firstIndex);

         // copy end segment
         final int copyLength = oldSerieLength - (lastIndex + 1);
         System.arraycopy(oldDataSerie, lastIndex + 1, newDataSerie, firstIndex, copyLength);
      }

      return newDataSerie;
   }

   private static long[] removeTimeSlices_Long(final long[] dataSerie, final int firstIndex, final int lastIndex) {

      final int oldSerieLength = dataSerie.length;
      final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

      final long[] newDataSerie = new long[newSerieLength];

      if (firstIndex == 0) {

         // delete from start, copy data by skipping removed slices
         System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, 0, newSerieLength);

      } else if (lastIndex == oldSerieLength - 1) {

         // delete until the end
         System.arraycopy(dataSerie, 0, newDataSerie, 0, newSerieLength);

      } else {

         // delete somewhere in the middle

         // copy start segment
         System.arraycopy(dataSerie, 0, newDataSerie, 0, firstIndex);

         // copy end segment
         final int copyLength = oldSerieLength - (lastIndex + 1);
         System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, firstIndex, copyLength);
      }

      return newDataSerie;
   }

   private static short[] removeTimeSlices_Short(final short[] oldDataSerie, final int firstIndex, final int lastIndex) {

      final int oldSerieLength = oldDataSerie.length;
      final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

      final short[] newDataSerie = new short[newSerieLength];

      if (firstIndex == 0) {

         // delete from start, copy data by skipping removed slices
         System.arraycopy(oldDataSerie, lastIndex + 1, newDataSerie, 0, newSerieLength);

      } else if (lastIndex == oldSerieLength - 1) {

         // delete until the end
         System.arraycopy(oldDataSerie, 0, newDataSerie, 0, newSerieLength);

      } else {

         // delete somewhere in the middle

         // copy start segment
         System.arraycopy(oldDataSerie, 0, newDataSerie, 0, firstIndex);

         // copy end segment
         final int copyLength = oldSerieLength - (lastIndex + 1);
         System.arraycopy(oldDataSerie, lastIndex + 1, newDataSerie, firstIndex, copyLength);
      }

      return newDataSerie;
   }

   private static void removeTimeSlices_TimeAndDistance(final TourData tourData,
                                                        final int firstIndex,
                                                        final int lastIndex,
                                                        final boolean isRemoveTime,
                                                        final boolean isAdjustTourStartTime) {

      final int[] timeSerie = tourData.timeSerie;
      final float[] distSerie = tourData.distanceSerie;

      if (timeSerie == null || timeSerie.length == 0) {
         return;
      }

      /*
       * Check if lastIndex is the last time slice, this will already remove time and distance
       */
      if (lastIndex == timeSerie.length - 1) {
         return;
      }

      final int timeFirstIndex = timeSerie[firstIndex];
      final int timeNextIndex = timeSerie[lastIndex + 1];

      int timeDiff = 0;
      if (isRemoveTime) {
         timeDiff = timeNextIndex - timeFirstIndex;
      }

      float distDiff = -1;
      if (distSerie != null) {
         distDiff = distSerie[lastIndex + 1] - distSerie[firstIndex];
      }

      // update remaining time and distance data series
      for (int serieIndex = lastIndex + 1; serieIndex < timeSerie.length; serieIndex++) {

         if (isRemoveTime) {
            timeSerie[serieIndex] = timeSerie[serieIndex] - timeDiff;
         }

         if (distDiff != -1) {
            distSerie[serieIndex] = distSerie[serieIndex] - distDiff;
         }
      }

      if (isRemoveTime && isAdjustTourStartTime) {

         final ZonedDateTime tourStartTime = tourData.getTourStartTime();
         final ZonedDateTime newTourStartTime = tourStartTime.plusSeconds(timeDiff);

         tourData.setTourStartTime(newTourStartTime);
      }
   }

   /**
    * Removes markers which are deleted and updates marker serie index which are positioned after
    * the deleted time slices
    *
    * @param tourData
    * @param firstSerieIndex
    * @param lastSerieIndex
    * @param isRemoveTime
    */
   private static void removeTourMarkers(final TourData tourData,
                                         final int firstSerieIndex,
                                         final int lastSerieIndex,
                                         final boolean isRemoveTime) {

      // check if markers are available
      final Set<TourMarker> allTourMarkers = tourData.getTourMarkers();
      if (allTourMarkers.isEmpty()) {
         return;
      }

      /*
       * Remove deleted markers
       */
      final TourMarker[] markerCloneList = allTourMarkers.toArray(new TourMarker[allTourMarkers.size()]);
      for (final TourMarker tourMarker : markerCloneList) {

         final int markerSerieIndex = tourMarker.getSerieIndex();

         if ((markerSerieIndex >= firstSerieIndex) && (markerSerieIndex <= lastSerieIndex)) {
            allTourMarkers.remove(tourMarker);
         }
      }

      /*
       * Update marker index in the remaining markers
       */
      final int diffSerieIndex = lastSerieIndex - firstSerieIndex + 1;
      final long tourStartTimeMS = tourData.getTourStartTimeMS();

      final int[] timeSerie = tourData.timeSerie;
      final float[] distSerie = tourData.distanceSerie;

      for (final TourMarker tourMarker : allTourMarkers) {

         final int markerSerieIndex = tourMarker.getSerieIndex();

         // check if the current marker is positioned after the removed time slices
         if (markerSerieIndex > lastSerieIndex) {

            final int serieIndex = markerSerieIndex - diffSerieIndex;
            tourMarker.setSerieIndex(serieIndex);

            if (isRemoveTime) {

               if (timeSerie != null) {
                  final int relativeTime = timeSerie[serieIndex];
                  tourMarker.setTime(relativeTime, tourStartTimeMS + (relativeTime * 1000));
               }

               if (distSerie != null) {
                  tourMarker.setDistance(distSerie[serieIndex]);
               }
            }
         }
      }
   }

   /**
    * Remove tour pauses and adjust the paused times when <code>isRemoveTime == true</code>
    *
    * @param tourData
    * @param firstRemovedIndex
    *           First index which is removed
    * @param lastRemovedIndex
    *           Last index which is removed
    * @param isRemoveTime
    */
   private static void removeTourPauses(final TourData tourData,
                                        final int firstRemovedIndex,
                                        final int lastRemovedIndex,
                                        final boolean isRemoveTime) {

      final int[] timeSerie = tourData.timeSerie;
      final long[] allPausedTime_Start = tourData.getPausedTime_Start();

      if (timeSerie == null
            || timeSerie.length == 0
            || allPausedTime_Start == null
            || allPausedTime_Start.length == 0) {

         // there are no time slices or tour pauses -> nothing to do

         return;
      }

      final int numPauses = allPausedTime_Start.length;
      final int numTimeSlices = timeSerie.length;

      final long[] allPausedTime_End = tourData.getPausedTime_End();
      final long tourStartTime = tourData.getTourStartTimeMS();

      final int lastRemovedIndexForDiff = Math.min(lastRemovedIndex + 1, numTimeSlices - 1);

      final long sliceTime_RemoveStart = timeSerie[firstRemovedIndex];
      final long sliceTime_RemoveEnd = timeSerie[lastRemovedIndex];
      final long sliceTime_RemoveEndDiff = timeSerie[lastRemovedIndexForDiff];

      final long removedTimeDiff = sliceTime_RemoveEndDiff - sliceTime_RemoveStart;

      final LongArrayList allPausedTimeAdjusted_Start = new LongArrayList();
      final LongArrayList allPausedTimeAdjusted_End = new LongArrayList();

      // loop: all pauses
      for (int pauseIndex = 0; pauseIndex < numPauses; pauseIndex++) {

         final long pauseTime_StartMS = allPausedTime_Start[pauseIndex];
         final long pauseTime_EndMS = allPausedTime_End[pauseIndex];

         long pauseTime_Start = (pauseTime_StartMS - tourStartTime) / 1000;
         long pauseTime_End = (pauseTime_EndMS - tourStartTime) / 1000;

         if (pauseTime_Start < sliceTime_RemoveStart) {

            // pauses are before the removed slices -> they will be unmodified

            allPausedTimeAdjusted_Start.add(pauseTime_StartMS);
            allPausedTimeAdjusted_End.add(pauseTime_EndMS);

         } else if (pauseTime_Start > sliceTime_RemoveEnd) {

            // pauses are after the removed slices -> adjust time when requested

            if (isRemoveTime) {

               pauseTime_Start -= removedTimeDiff;
               pauseTime_End -= removedTimeDiff;
            }

            allPausedTimeAdjusted_Start.add(tourStartTime + pauseTime_Start * 1000);
            allPausedTimeAdjusted_End.add(tourStartTime + pauseTime_End * 1000);
         }
      }

      // update model
      tourData.setPausedTime_Start(allPausedTimeAdjusted_Start.toArray());
      tourData.setPausedTime_End(allPausedTimeAdjusted_End.toArray());
   }

   /**
    * @param tourDataList
    * @return Returns <code>true</code> when the tour is modified, otherwise <code>false</code>.
    */
   public static List<TourData> retrieveWeatherData(final List<TourData> tourDataList) {

      final List<TourData> modifiedTours = new ArrayList<>();

      if (tourDataList == null || tourDataList.size() == 0) {
         return modifiedTours;
      }

      TourLogManager.showLogView();
      final long start = System.currentTimeMillis();

      BusyIndicator.showWhile(Display.getCurrent(),
            () -> {

               final String weatherProvider = _prefStore.getString(
                     ITourbookPreferences.WEATHER_WEATHER_PROVIDER_ID);

               TourLogManager.subLog_INFO(NLS.bind(
                     LOG_RETRIEVE_WEATHER_DATA_001_START,
                     weatherProvider));

               for (final TourData tourData : tourDataList) {

                  // ensure data is available
                  if (tourData.latitudeSerie == null || tourData.longitudeSerie == null) {

                     TourLogManager.subLog_ERROR(String.format(
                           LOG_RETRIEVE_WEATHER_DATA_010_NO_GPS_DATA_SERIE,
                           getTourDateTimeShort(tourData)));

                     continue;
                  }

                  if (TourWeatherRetriever.retrieveWeatherData(tourData, weatherProvider)) {

                     modifiedTours.add(tourData);
                  }
               }
            });

      TourLogManager.subLog_INFO(String.format(
            LOG_RETRIEVE_WEATHER_DATA_002_END,
            (System.currentTimeMillis() - start) / 1000.0));

      return modifiedTours;
   }

   /**
    * Saves tours which have been modified and updates the tour data editor, a notification is fired
    * when the data are saved.
    * <p>
    * If a tour is opened in the {@link TourDataEditorView}, the tour will be saved only when the
    * tour is not dirty, if the tour is dirty, saving is not done.
    * <p>
    * The event {@link TourEventId#TOUR_CHANGED} is fired always.
    *
    * @param tourData
    *           modified tour
    * @return Returns the persisted {@link TourData}
    */
   public static TourData saveModifiedTour(final TourData tourData) {
      return saveModifiedTour(tourData, true);
   }

   /**
    * @param tourData
    * @param isFireNotification
    *           When <code>true</code>, a notification is fired when the data are saved
    * @return Returns the saved {@link TourData} or <code>null</code> when saving fails
    */
   public static TourData saveModifiedTour(final TourData tourData, final boolean isFireNotification) {

      final ArrayList<TourData> modifiedTours = new ArrayList<>();
      modifiedTours.add(tourData);

      final ArrayList<TourData> savedTourData = saveModifiedTours(modifiedTours, isFireNotification);

      if (savedTourData.isEmpty()) {
         return null;
      } else {
         return savedTourData.get(0);
      }
   }

   /**
    * Saves tours which have been modified and updates the tour data editor, fires a
    * {@link TourEventId#TOUR_CHANGED} event.<br>
    * <br>
    * If a tour is openend in the {@link TourDataEditorView}, the tour will be saved only when the
    * tour is not dirty, if the tour is dirty, saving is not done. The change event is always fired.
    *
    * @param modifiedTours
    *           modified tours
    * @return Returns a list with all persisted {@link TourData}
    */
   public static ArrayList<TourData> saveModifiedTours(final List<TourData> modifiedTours) {
      return saveModifiedTours(modifiedTours, true);
   }

   /**
    * Saves tours which have been modified and updates the tour data editor, fires a
    * {@link TourEventId#TOUR_CHANGED} event.<br>
    * <br>
    * If a tour is openend in the {@link TourDataEditorView}, the tour will be saved only when the
    * tour is not dirty, if the tour is dirty, saving is not done.
    *
    * @param modifiedTours
    *           modified tours
    * @param canFireNotification
    *           when <code>true</code>, a notification is fired when the data are saved
    * @return a list with all persisted {@link TourData}
    */
   private static ArrayList<TourData> saveModifiedTours(final List<TourData> modifiedTours,
                                                        final boolean canFireNotification) {

      // reset multiple tour data cache
      _joined_TourData = null;

      final ArrayList<TourData> savedTours = new ArrayList<>();

      if (modifiedTours.isEmpty()) {
         // there is nothing modified
         return savedTours;
      }

      final TourData[] tourDataEditorSavedTour = { null };
      final boolean[] doFireChangeEvent = { false };

      if (modifiedTours.size() == 1) {

         // no progress when only 1 tour is saved

         saveModifiedTours_OneTour(savedTours, tourDataEditorSavedTour, doFireChangeEvent, modifiedTours.get(0));

      } else {

         try {

            final IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
               @Override
               public void run(final IProgressMonitor monitor)
                     throws InvocationTargetException, InterruptedException {

                  int saveCounter = 0;
                  final int tourSize = modifiedTours.size();

                  monitor.beginTask(Messages.Tour_Data_SaveTour_Monitor, tourSize);

                  for (final TourData tourData : modifiedTours) {

                     monitor.subTask(
                           NLS.bind(//
                                 Messages.Tour_Data_SaveTour_MonitorSubtask,
                                 ++saveCounter,
                                 tourSize));

                     saveModifiedTours_OneTour(savedTours, tourDataEditorSavedTour, doFireChangeEvent, tourData);

                     monitor.worked(1);
                  }
               }
            };

            new ProgressMonitorDialog(TourbookPlugin.getAppShell()).run(true, false, saveRunnable);

         } catch (final InvocationTargetException | InterruptedException e) {
            StatusUtil.showStatus(e);
            Thread.currentThread().interrupt();
         }
      }

      if (canFireNotification && doFireChangeEvent[0]) {

         final TourEvent tourEvent = new TourEvent(savedTours);
         tourEvent.tourDataEditorSavedTour = tourDataEditorSavedTour[0];

         fireEvent(TourEventId.TOUR_CHANGED, tourEvent);

         // when tours are saved then the photo annotation can be changed -> update photo gallery
         PhotoManager.updatePicDirGallery();
      }

      return savedTours;
   }

   private static void saveModifiedTours_OneTour(final ArrayList<TourData> savedTours,
                                                 final TourData[] tourDataEditorSavedTour,
                                                 final boolean[] doFireChangeEvent,
                                                 final TourData tourData) {
      boolean doSaveTour = false;
      TourData savedTour = null;

      final TourDataEditorView tourDataEditor = getTourDataEditor();
      if (tourDataEditor != null) {

         final TourData tourDataInEditor = tourDataEditor.getTourData();

         try {
            checkTourData(tourData, tourDataInEditor);
         } catch (final MyTourbookException e) {
            // error is already displayed, just log it
            e.printStackTrace();
            return;
         }

         if (tourDataInEditor == tourData) {

            // selected tour is in the tour data editor

            if (tourDataEditor.isDirty()) {

               // tour in the editor is already dirty, tour MUST BE SAVED IN THE TOUR EDITOR

               savedTour = tourData;

               /*
                * make the tour data editor visible, it could be hidden and confuses the user when
                * the changes are not visible
                */
               openTourEditor(false);

            } else {

               /*
                * tour in the editor is not dirty, save tour and update editor ui
                */

               savedTour = TourDatabase.saveTour(tourData, true);

               /*
                * set flag for the tour data editor that the tour is saved and the ui is updated
                */
               tourDataEditorSavedTour[0] = savedTour;
            }

            /*
             * update UI in the tour data editor with the modified tour data
             */
            tourDataEditor.updateUI(savedTour);

            doFireChangeEvent[0] = true;

         } else {

            // tour is not in the tour editor

            doSaveTour = true;
         }
      } else {

         // tour is not in the tour editor

         doSaveTour = true;
      }

      if (doSaveTour) {

         // save the tour
         savedTour = TourDatabase.saveTour(tourData, true);

         doFireChangeEvent[0] = true;
      }

      if (savedTour != null) {

         savedTours.add(savedTour);

         // keep the current map position that a saved tour is not centered with default size
         savedTour.mapZoomLevel = tourData.mapZoomLevel;
         savedTour.mapCenterPositionLatitude = tourData.mapCenterPositionLatitude;
         savedTour.mapCenterPositionLongitude = tourData.mapCenterPositionLongitude;
      }
   }

   /**
    * Set the graph colors from the pref store
    *
    * @param yData
    * @param graphName
    */
   public static void setBarColors(final ChartDataYSerie yData, final String graphName) {

      final String prefGraphName = ICommonPreferences.GRAPH_COLORS + graphName + UI.SYMBOL_DOT;

      // get colors from common pref store

      final String prefColorLine = UI.IS_DARK_THEME
            ? GraphColorManager.PREF_COLOR_LINE_DARK
            : GraphColorManager.PREF_COLOR_LINE_LIGHT;

      final RGB rgbGradient_Dark = PreferenceConverter.getColor(_prefStore_Common, prefGraphName + GraphColorManager.PREF_COLOR_GRADIENT_DARK);
      final RGB rgbGradient_Bright = PreferenceConverter.getColor(_prefStore_Common, prefGraphName + GraphColorManager.PREF_COLOR_GRADIENT_BRIGHT);
      final RGB rgbLineColor = PreferenceConverter.getColor(_prefStore_Common, prefGraphName + prefColorLine);

      yData.setRgbBar_Gradient_Dark(new RGB[] { rgbGradient_Dark });
      yData.setRgbBar_Gradient_Bright(new RGB[] { rgbGradient_Bright });
      yData.setRgbBar_Line(new RGB[] { rgbLineColor });
   }

   public static boolean setElevationValuesFromSRTM(final List<TourData> allTourData) {

      if (allTourData == null || allTourData.isEmpty()) {
         return false;
      }

      if (isSrtmAccountValid() == false) {
         return false;
      }

      final Display display = Display.getCurrent();

      if (MessageDialog.openConfirm(
            display.getActiveShell(),
            Messages.TourEditor_Dialog_SetAltitudeFromSRTM_Title,
            Messages.TourEditor_Dialog_SetAltitudeFromSRTM_Message) == false) {

         return false;
      }

      TourLogManager.showLogView();

      TourLogManager.addLog(
            TourLogState.DEFAULT,
            Messages.Log_SetElevationFromSRTM_01,
            TourLogView.CSS_LOG_TITLE);

      final boolean[] returnValue = { false };

      BusyIndicator.showWhile(display, () -> {

         TourData oldTourDataDummyClone = null;

         for (final TourData tourData : allTourData) {

            oldTourDataDummyClone = RawDataManager.getInstance().createTourDataDummyClone(
                  Arrays.asList(TourValueType.TIME_SLICES__ELEVATION),
                  tourData);

            TourLogManager.subLog_OK(TourManager.getTourDateTimeShort(tourData));

            final boolean isReplaced = tourData.replaceAltitudeWithSRTM(true);

            RawDataManager.displayTourModifiedDataDifferences(
                  TourValueType.TIME_SLICES__ELEVATION,
                  oldTourDataDummyClone,
                  tourData);

            returnValue[0] = returnValue[0] || isReplaced;
         }
      });

      return returnValue[0];
   }

   /**
    * Set graph colors
    *
    * @param yData
    * @param graphName
    */
   public static void setGraphColors(final ChartDataYSerie yData, final String graphName) {

      final String defaultColorName = ICommonPreferences.GRAPH_COLORS + graphName + UI.SYMBOL_DOT;

      final String prefColorLineThemed = UI.IS_DARK_THEME
            ? GraphColorManager.PREF_COLOR_LINE_DARK
            : GraphColorManager.PREF_COLOR_LINE_LIGHT;

      final String prefColorTextThemed = UI.IS_DARK_THEME
            ? GraphColorManager.PREF_COLOR_TEXT_DARK
            : GraphColorManager.PREF_COLOR_TEXT_LIGHT;

// SET_FORMATTING_OFF

      // put the colors into the chart data
      yData.setRgbGraph_Gradient_Bright(PreferenceConverter.getColor(_prefStore_Common, defaultColorName + GraphColorManager.PREF_COLOR_GRADIENT_BRIGHT));
      yData.setRgbGraph_Gradient_Dark(  PreferenceConverter.getColor(_prefStore_Common, defaultColorName + GraphColorManager.PREF_COLOR_GRADIENT_DARK));
      yData.setRgbGraph_Line(           PreferenceConverter.getColor(_prefStore_Common, defaultColorName + prefColorLineThemed));
      yData.setRgbGraph_Text(           PreferenceConverter.getColor(_prefStore_Common, defaultColorName + prefColorTextThemed));

// SET_FORMATTING_ON

   }

   public static void setTourDataEditor(final TourDataEditorView tourDataEditorView) {
      _tourDataEditorInstance = tourDataEditorView;
   }

   private static void setupMultiTourMarker(final TourData tourData) {

      final int[] multipleStartTimeIndex = tourData.multipleTourStartIndex;
      final int[] multipleNumberOfMarkers = tourData.multipleNumberOfMarkers;

      // fixing ArrayIndexOutOfBoundsException: 0
      if (multipleStartTimeIndex.length == 0) {
         return;
      }

      int tourIndex = 0;
      int numberOfMultiMarkers = 0;
      int tourSerieIndex = 0;

      // setup first multiple tour
      tourSerieIndex = multipleStartTimeIndex[tourIndex];
      numberOfMultiMarkers = multipleNumberOfMarkers[tourIndex];

      final ArrayList<TourMarker> allTourMarkers = tourData.multipleTourMarkers;

      for (int markerIndex = 0; markerIndex < allTourMarkers.size(); markerIndex++) {

         while (markerIndex >= numberOfMultiMarkers) {

            // setup next tour

            tourIndex++;

            if (tourIndex <= multipleStartTimeIndex.length - 1) {

               tourSerieIndex = multipleStartTimeIndex[tourIndex];
               numberOfMultiMarkers += multipleNumberOfMarkers[tourIndex];
            }
         }

         final TourMarker tourMarker = allTourMarkers.get(markerIndex);
         final int xAxisSerieIndex = tourSerieIndex + tourMarker.getSerieIndex();

         tourMarker.setMultiTourSerieIndex(xAxisSerieIndex);
      }
   }

   /**
    * Add/save selected photos in it's tours.
    *
    * @param photoGallery
    */
   public static void tourPhoto_Add(final PhotoGallery photoGallery) {

      if (isTourEditorModified()) {
         return;
      }

      int numHistoryTourPhotos = 0;

      // key: tour id, photo id
      final HashMap<Long, HashMap<String, TourPhoto>> allToursWithPhotos = new HashMap<>();

      // loop: all selected photos in the gallery
      final Collection<GalleryMT20Item> allGalleryItems = photoGallery.getGallerySelection();
      for (final GalleryMT20Item galleryItem : allGalleryItems) {

         final Photo selectedPhoto = galleryItem.photo;

         final long linkTourId = selectedPhoto.getLinkTourId();

         if (linkTourId == Long.MIN_VALUE) {

            // this is a history tour -> photo cannot be saved

            numHistoryTourPhotos++;

            continue;
         }

         final TourData tourData = getTour(linkTourId);
         if (tourData != null) {

            // photo belongs to this tour

            // check by image file name if the photo already exist in the tour
            boolean isPhotoSavedInTour = false;
            for (final TourPhoto tourPhoto : tourData.getTourPhotos()) {
               if (tourPhoto.getImageFilePathName().equals(selectedPhoto.imageFilePathName)) {
                  isPhotoSavedInTour = true;
                  break;
               }
            }

            if (isPhotoSavedInTour) {
               continue;
            }

            // create a new tour photo

            final TourPhoto tourPhoto = new TourPhoto(tourData, selectedPhoto);

            // set adjusted time / geo location
            tourPhoto.setAdjustedTime(selectedPhoto.adjustedTime_Camera);
            tourPhoto.setGeoLocation(
                  selectedPhoto.getLinkLatitude(),
                  selectedPhoto.getLinkLongitude());

            HashMap<String, TourPhoto> tourWithPhotos = allToursWithPhotos.get(linkTourId);
            if (tourWithPhotos == null) {
               tourWithPhotos = new HashMap<>();
               allToursWithPhotos.put(linkTourId, tourWithPhotos);
            }

            tourWithPhotos.put(tourPhoto.getImageFilePathName(), tourPhoto);
         }
      }

      // show message that photos can be saved only in real tours
      if (numHistoryTourPhotos > 0 && _prefStore.getBoolean(ITourbookPreferences.TOGGLE_STATE_SHOW_HISTORY_TOUR_SAVE_WARNING) == false) {

         final MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(
               Display.getCurrent().getActiveShell(),
               Messages.Photos_AndTours_Dialog_CannotSaveHistoryTour_Title,
               Messages.Photos_AndTours_Dialog_CannotSaveHistoryTour_Message,
               Messages.App_ToggleState_DoNotShowAgain,
               false, // toggle default state
               null,
               null);

         // save toggle state
         _prefStore.setValue(
               ITourbookPreferences.TOGGLE_STATE_SHOW_HISTORY_TOUR_SAVE_WARNING,
               dialog.getToggleState());
      }

      for (final Long tourId : allToursWithPhotos.keySet()) {

         final TourData tourData = getTour(tourId);

         final HashMap<String, TourPhoto> tourWithPhotos = allToursWithPhotos.get(tourId);

         final Collection<TourPhoto> tourPhotos = tourWithPhotos.values();

         tourData.addPhotos(tourPhotos);
      }
   }

   /**
    * Remove selected photos from it's tours.
    *
    * @param photoGallery
    */
   public static void tourPhoto_Remove(final PhotoGallery photoGallery) {

      if (isTourEditorModified()) {
         return;
      }

      int numPhotos = 0;

      // key: tour id, photo id
      final HashMap<Long, HashMap<Long, TourPhoto>> allToursWithTourPhotos = new HashMap<>();

      // loop: all selected photos in the gallery
      final Collection<GalleryMT20Item> tourPhotos2Remove = photoGallery.getGallerySelection();
      for (final GalleryMT20Item galleryItem : tourPhotos2Remove) {

         final Photo removedPhoto = galleryItem.photo;

         final Collection<TourPhotoReference> removedPhotoRefs = removedPhoto.getTourPhotoReferences().values();

         // loop: all tour references in a photo
         for (final TourPhotoReference photoTourRef : removedPhotoRefs) {

            final long removedTourId = photoTourRef.tourId;
            final long removedPhotoId = photoTourRef.photoId;

            final TourData tourData = getTour(removedTourId);
            if (tourData != null) {

               // photo is from this tour

               // loop: all tour photos
               for (final TourPhoto tourPhoto : tourData.getTourPhotos()) {

                  if (tourPhoto.getPhotoId() == removedPhotoId) {

                     // photo is in tour photo collection -> remove it

                     HashMap<Long, TourPhoto> allTourIdPhotos = allToursWithTourPhotos.get(removedTourId);

                     if (allTourIdPhotos == null) {
                        allTourIdPhotos = new HashMap<>();
                        allToursWithTourPhotos.put(removedTourId, allTourIdPhotos);
                     }

                     final TourPhoto prevTourPhoto = allTourIdPhotos.put(removedPhotoId, tourPhoto);
                     if (prevTourPhoto == null) {
                        numPhotos++;
                     }

                     break;
                  }
               }
            }
         }
      }

      if (numPhotos == 0) {
         return;
      }

      // remove photos from this tour and save it

      final MessageDialog dialog = new MessageDialog(

            Display.getDefault().getActiveShell(),

            Messages.Photos_AndTours_Dialog_RemovePhotos_Title,
            null, // no title image

            NLS.bind(Messages.Photos_AndTours_Dialog_RemovePhotos_Message,
                  numPhotos,
                  allToursWithTourPhotos.size()),

            MessageDialog.CONFIRM,

            0, // default index

            Messages.App_Action_RemoveTourPhotos,
            Messages.App_Action_Cancel);

      if (dialog.open() == IDialogConstants.OK_ID) {

         /*
          * Remove tour reference from the photo, this MUST be done after the user has
          * confirmed the removal otherwise the photo do not have a tour reference when the dialog
          * is canceled
          */

         // loop: all selected photos in the gallery
         for (final GalleryMT20Item galleryPhotoItem : tourPhotos2Remove) {

            final Photo removedGalleryPhoto = galleryPhotoItem.photo;

            final Collection<TourPhotoReference> removedPhotoRefs = removedGalleryPhoto.getTourPhotoReferences().values();

            // loop: all tour references in a photo
            for (final TourPhotoReference tourPhotoReference : removedPhotoRefs) {

               final long removedTourId = tourPhotoReference.tourId;
               final long removedPhotoId = tourPhotoReference.photoId;

               final HashMap<Long, TourPhoto> allTourIdPhotos = allToursWithTourPhotos.get(removedTourId);

               if (allTourIdPhotos != null) {

                  // loop: all current tour photos
                  for (final TourPhoto tourIdPhoto : allTourIdPhotos.values()) {

                     if (tourIdPhoto.getPhotoId() == removedPhotoId) {

                        // photo is in tour photo collection -> remove it

                        removedGalleryPhoto.removeTour(removedTourId);

                        break;
                     }
                  }
               }
            }
         }

         for (final Long tourId : allToursWithTourPhotos.keySet()) {

            final TourData tourData = getTour(tourId);

            final HashMap<Long, TourPhoto> tourWithPhotos = allToursWithTourPhotos.get(tourId);

            final Collection<TourPhoto> tourPhotos = tourWithPhotos.values();

            tourData.removePhotos(tourPhotos);
         }
      }
   }

   public void addTourEventListener(final ITourEventListener listener) {
      _tourEventListeners.add(listener);
   }

   /**
    * Tour save listeners will be called to save tours before the application is shut down
    *
    * @param listener
    */
   public void addTourSaveListener(final ITourSaveListener listener) {
      _tourSaveListeners.add(listener);
   }

   /**
    * Remove all {@link TourData} from the cache so they need to be reloaded the next time with
    * {@link #getTourData} from the database.
    * <p>
    * When this method is called and a tour is modified in the tour editor, the calling method is
    * responsible to update the tour in the tour editor:
    */
   public void clearTourDataCache() {

      _tourDataCache.clear();

      if (_tourDataEditorInstance != null && _tourDataEditorInstance.isDirty()) {

         final TourData tourDataInEditor = _tourDataEditorInstance.getTourData();
         if (tourDataInEditor != null) {

            // keep modified tour in cache
            _tourDataCache.put(tourDataInEditor.getTourId(), tourDataInEditor);
         }
      }
   }

   /**
    * Clip values when a minimum distance is fallen short of
    *
    * @param tourData
    */
   private void computeValueClipping(final TourData tourData) {

      final int[] timeSerie = tourData.timeSerie;

      final float[] gradientSerie = tourData.gradientSerie;
      final float[] speedSerie = tourData.getSpeedSerie();
      final float[] paceSerie = tourData.getPaceSerie();
      final float[] altimeterSerie = tourData.getAltimeterSerie();
      final double[] distanceSerie = tourData.getDistanceSerieDouble();

      if ((timeSerie == null || timeSerie.length == 0)
            || speedSerie == null
            || paceSerie == null
            || altimeterSerie == null
            || distanceSerie == null
            || gradientSerie == null) {
         return;
      }

      int clippingTime;
      if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_CLIPPING)) {
         // use custom clipping
         clippingTime = _prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_VALUE_CLIPPING_TIMESLICE);
      } else {
         // use internal clipping, value was evaluated with experiments
         clippingTime = 15;
      }

      float paceClipping;
      if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_PACE_CLIPPING)) {
         // use custom clipping
         paceClipping = _prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_PACE_CLIPPING_VALUE);
      } else {
         // use internal clipping, value was evaluated with experiments
         paceClipping = 15;
      }

      paceClipping /= 10;

      final int serieLength = timeSerie.length;
      final int serieLengthLast = serieLength - 1;

      final int deviceTimeInterval = tourData.getDeviceTimeInterval();
      if (deviceTimeInterval > 0) {

         /*
          * clipping for constant time intervals
          */

         final int slices = Math.max(1, clippingTime / deviceTimeInterval);

         for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

            // adjust index to the array size
            int sliceIndex = serieIndex + slices;
            final int sliceIndexMax = ((0 >= sliceIndex) ? 0 : sliceIndex);
            sliceIndex = (sliceIndexMax <= serieLengthLast) ? sliceIndexMax : serieLengthLast;

            final double distance = distanceSerie[sliceIndex] - distanceSerie[serieIndex];

            if (distance == 0) {
               altimeterSerie[serieIndex] = 0;
               gradientSerie[serieIndex] = 0;
               speedSerie[serieIndex] = 0;
            }

            // remove peaks in pace
            if (speedSerie[serieIndex] <= paceClipping) {
               paceSerie[serieIndex] = 0;
            }
         }

      } else {

         /*
          * clipping for variable time intervals
          */

         for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

            // adjust index to the array size
            final int serieIndexPrev = serieIndex - 1;
            int lowIndex = ((0 >= serieIndexPrev) ? 0 : serieIndexPrev);

            int timeDiff = timeSerie[serieIndex] - timeSerie[lowIndex];
            double distDiff = 0;

            while (timeDiff < clippingTime) {

               // make sure to be in the array range
               if (lowIndex < 1) {
                  break;
               }

               lowIndex--;

               timeDiff = timeSerie[serieIndex] - timeSerie[lowIndex];
            }

            distDiff = distanceSerie[serieIndex] - distanceSerie[lowIndex];

            if (distDiff == 0) {
               altimeterSerie[serieIndex] = 0;
               gradientSerie[serieIndex] = 0;
               speedSerie[serieIndex] = 0;
            }

            // remove peaks in pace
            if (speedSerie[serieIndex] <= paceClipping) {
               paceSerie[serieIndex] = 0;
            }
         }
      }
   }

   /**
    * create the callbacks which compute the average
    */
   private void createAvgCallbacks() {

      /*
       * Compute the average speed in km/h between the two sliders
       */
      _computeAvg_Speed = new ComputeChartValue() {

         @Override
         public float compute() {

            final Object customDataDistance = chartModel.getCustomData(CUSTOM_DATA_DISTANCE);
            if (customDataDistance == null) {
               return 0;
            }

            final double[] timeValues = ((ChartDataXSerie) (chartModel.getCustomData(CUSTOM_DATA_TIME))).getHighValuesDouble()[0];
            final double[] distanceValues = ((ChartDataXSerie) (customDataDistance)).getHighValuesDouble()[0];
            if (timeValues == null) {
               return 0;
            }

            final double leftDistance = distanceValues[valueIndexLeft];
            final double rightDistance = distanceValues[valueIndexRight];
            final double leftTime = timeValues[valueIndexLeft];
            final double rightTime = timeValues[valueIndexRight];

            if (leftTime == rightTime) {

               // left and right slider are at the same position
               return 0;

            } else {

               final double time = Math.max(0, rightTime - leftTime - breakTime);
               if (time == 0) {
                  return 0;
               }

               final double distance = rightDistance - leftDistance;
               final double speed = distance / time * 3.6;

               return (float) speed;
            }
         }
      };

      /*
       * Compute the average pace between two sliders
       */
      _computeAvg_Pace = new ComputeChartValue() {

         @Override
         public float compute() {

            final Object customDataDistance = chartModel.getCustomData(CUSTOM_DATA_DISTANCE);
            if (customDataDistance == null) {
               return 0;
            }

            final double[] timeValues = ((ChartDataXSerie) (chartModel.getCustomData(CUSTOM_DATA_TIME))).getHighValuesDouble()[0];
            final double[] distanceValues = ((ChartDataXSerie) (customDataDistance)).getHighValuesDouble()[0];
            if (timeValues == null) {
               return 0;
            }

            final double leftDistance = distanceValues[valueIndexLeft];
            final double rightDistance = distanceValues[valueIndexRight];
            final double leftTime = timeValues[valueIndexLeft];
            final double rightTime = timeValues[valueIndexRight];

            if (leftTime == rightTime) {

               // left and right slider are at the same position
               return 0;

            } else {

               final double distance = rightDistance - leftDistance;
               if (distance == 0) {
                  return 0;
               }

               final double time = Math.max(0, rightTime - leftTime - breakTime);
               return (float) (time * 1000 / distance);
            }
         }
      };

      /*
       * Compute the average altimeter speed between the two sliders
       */
      _computeAvg_Altimeter = new ComputeChartValue() {

         @Override
         public float compute() {

            final Object customDataAltitude = chartModel.getCustomData(CUSTOM_DATA_ALTITUDE);
            if (customDataAltitude == null) {
               return 0;
            }

            final double[] timeValues = ((ChartDataXSerie) (chartModel.getCustomData(CUSTOM_DATA_TIME))).getHighValuesDouble()[0];
            if (timeValues == null) {
               return 0;
            }

            final float[] altitudeValues = ((ChartDataYSerie) (customDataAltitude)).getHighValuesFloat()[0];

            final float leftAltitude = altitudeValues[valueIndexLeft];
            final float rightAltitude = altitudeValues[valueIndexRight];
            final double leftTime = timeValues[valueIndexLeft];
            final double rightTime = timeValues[valueIndexRight];

            if (leftTime == rightTime) {

               // left and right slider are at the same position
               return 0;

            } else {

               final double time = Math.max(0, rightTime - leftTime - breakTime);
               if (time == 0) {
                  return 0;
               }

               return (float) (((rightAltitude - leftAltitude) / time) * 3600);
            }
         }
      };

      _computeAvg_Altitude = new ComputeChartValue() {

         @Override
         public float compute() {

            final TourData tourData = (TourData) chartModel.getCustomData(CUSTOM_DATA_TOUR_DATA);

            return tourData.computeAvg_Altitude(valueIndexLeft, valueIndexRight);
         }
      };

      /*
       * Compute the average altimeter speed between the two sliders
       */
      _computeAvg_Gradient = new ComputeChartValue() {

         @Override
         public float compute() {

            final Object customDataAltitude = chartModel.getCustomData(CUSTOM_DATA_ALTITUDE);
            final Object customDataDistance = chartModel.getCustomData(CUSTOM_DATA_DISTANCE);
            if (customDataAltitude == null || customDataDistance == null) {
               return 0;
            }

            final float[] altitudeValues = ((ChartDataYSerie) (customDataAltitude)).getHighValuesFloat()[0];
            final double[] distanceValues = ((ChartDataXSerie) (customDataDistance)).getHighValuesDouble()[0];

            final float leftAltitude = altitudeValues[valueIndexLeft];
            final float rightAltitude = altitudeValues[valueIndexRight];
            final double leftDistance = distanceValues[valueIndexLeft];
            final double rightDistance = distanceValues[valueIndexRight];

            if (leftDistance == rightDistance) {
               // left and right slider are at the same position
               return 0;
            } else {
               return (float) ((rightAltitude - leftAltitude) / (rightDistance - leftDistance) * 100);
            }
         }
      };

      /*
       * Compute the average cadence between the two sliders.
       */
      _computeAvg_Cadence = new ComputeChartValue() {

         @Override
         public float compute() {

            if (valueIndexLeft == valueIndexRight) {
               // left and right slider are at the same position
               return 0;
            }

            final TourData tourData = (TourData) chartModel.getCustomData(CUSTOM_DATA_TOUR_DATA);

            return tourData.computeAvg_CadenceSegment(valueIndexLeft, valueIndexRight);
         }
      };

      /*
       * Compute the average pulse between two sliders.
       */
      _computeAvg_Pulse = new ComputeChartValue() {

         @Override
         public float compute() {

            if (valueIndexLeft == valueIndexRight) {
               // left and right slider are at the same position
               return 0;
            }

            final TourData tourData = (TourData) chartModel.getCustomData(CUSTOM_DATA_TOUR_DATA);

            return tourData.computeAvg_PulseSegment(valueIndexLeft, valueIndexRight);
         }
      };

      _computeAvg_RunDyn_StanceTime = new ComputeChartValue() {

         @Override
         public float compute() {

            final TourData tourData = (TourData) chartModel.getCustomData(CUSTOM_DATA_TOUR_DATA);

            final float metricValue = tourData.computeAvg_FromValues(tourData.runDyn_StanceTime,
                  valueIndexLeft,
                  valueIndexRight);

            return metricValue;
         }
      };

      _computeAvg_RunDyn_StanceTimeBalance = new ComputeChartValue() {

         @Override
         public float compute() {

            final TourData tourData = (TourData) chartModel.getCustomData(CUSTOM_DATA_TOUR_DATA);

            final float metricValue = tourData.computeAvg_FromValues(tourData.runDyn_StanceTimeBalance,
                  valueIndexLeft,
                  valueIndexRight);

            return metricValue / TourData.RUN_DYN_DATA_MULTIPLIER;
         }
      };

      _computeAvg_RunDyn_StepLength = new ComputeChartValue() {

         @Override
         public float compute() {

            final TourData tourData = (TourData) chartModel.getCustomData(CUSTOM_DATA_TOUR_DATA);

            final float metricValue = tourData.computeAvg_FromValues(tourData.runDyn_StepLength,
                  valueIndexLeft,
                  valueIndexRight);

            return metricValue * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;
         }
      };

      _computeAvg_RunDyn_VerticalOscillation = new ComputeChartValue() {

         @Override
         public float compute() {

            final TourData tourData = (TourData) chartModel.getCustomData(CUSTOM_DATA_TOUR_DATA);

            final float metricValue = tourData.computeAvg_FromValues(tourData.runDyn_VerticalOscillation,
                  valueIndexLeft,
                  valueIndexRight);

            return metricValue * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH / TourData.RUN_DYN_DATA_MULTIPLIER;
         }
      };

      _computeAvg_RunDyn_VerticalRatio = new ComputeChartValue() {

         @Override
         public float compute() {

            final TourData tourData = (TourData) chartModel.getCustomData(CUSTOM_DATA_TOUR_DATA);

            final float metricValue = tourData.computeAvg_FromValues(tourData.runDyn_VerticalRatio,
                  valueIndexLeft,
                  valueIndexRight);

            return metricValue / TourData.RUN_DYN_DATA_MULTIPLIER;
         }
      };
   }

   /**
    * Creates a chart data model from the tour data.
    *
    * @param tourData
    *           data which contains the tour data
    * @param tourChartProperty
    * @return
    */
   public ChartDataModel createChartDataModel(final TourData tourData, final TourChartConfiguration tcc) {

      return createChartDataModel_10(tourData, tcc, false);
   }

   public ChartDataModel createChartDataModel(final TourData tourData,
                                              final TourChartConfiguration tcc,
                                              final boolean hasPropertyChanged) {

      return createChartDataModel_10(tourData, tcc, hasPropertyChanged);
   }

   private ChartDataModel createChartDataModel_10(final TourData tourData,
                                                  final TourChartConfiguration tcc,
                                                  final boolean hasPropertyChanged) {

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.LINE);

      chartDataModel.setTitle(getTourTitleDetailed(tourData));

      /*
       * TIME SERIE is a MUST data serie
       */
      final boolean isTimeSerie = tourData.timeSerie != null && tourData.timeSerie.length > 0;
      final boolean isHistorySerie = tourData.timeSerieHistory != null && tourData.timeSerieHistory.length > 0;

      if (isTimeSerie == false && isHistorySerie == false) {
         return chartDataModel;
      }

      final String prefColorName_Distance = ICommonPreferences.GRAPH_COLORS + GraphColorManager.PREF_GRAPH_DISTANCE + UI.SYMBOL_DOT;
      final String prefColorName_Time = ICommonPreferences.GRAPH_COLORS + GraphColorManager.PREF_GRAPH_TIME + UI.SYMBOL_DOT;

      final String prefColorTextThemed = UI.IS_DARK_THEME
            ? GraphColorManager.PREF_COLOR_TEXT_DARK
            : GraphColorManager.PREF_COLOR_TEXT_LIGHT;

      final RGB rgbText_Distance = PreferenceConverter.getColor(_prefStore_Common, prefColorName_Distance + prefColorTextThemed);
      final RGB rgbText_Time = PreferenceConverter.getColor(_prefStore_Common, prefColorName_Time + prefColorTextThemed);

      if (hasPropertyChanged) {
         tourData.clearComputedSeries();
      }

      tourData.computeSpeedSeries();
      tourData.computeAltimeterGradientSerie();

      computeValueClipping(tourData);

      /*
       * Distance
       */
      final double[] distanceSerie = tourData.getDistanceSerieDouble();
      ChartDataXSerie xDataDist = null;
      if (distanceSerie != null) {
         xDataDist = new ChartDataXSerie(distanceSerie);
         xDataDist.setLabel(Messages.tour_editor_label_distance);
         xDataDist.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
         xDataDist.setValueDivisor(1000);
         xDataDist.setRgbGraph_Text(rgbText_Distance);

         // do not show average values but show the other values with 3 digits
         xDataDist.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(false, false, null, 3));
      }

      /*
       * Time
       */
      final ChartDataXSerie xDataTime = new ChartDataXSerie(tourData.getTimeSerieWithTimeZoneAdjusted());
      xDataTime.setLabel(Messages.tour_editor_label_time);
      xDataTime.setUnitLabel(Messages.tour_editor_label_time_unit);
      xDataTime.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_OPTIONAL_SECOND);
      xDataTime.setRgbGraph_Text(rgbText_Time);

      /*
       * Show the distance on the x-axis when a distance is available, otherwise the time is
       * displayed
       */
      boolean isShowTimeOnXAxis;
      if (xDataDist == null) {
         isShowTimeOnXAxis = true;
         tcc.isForceTimeOnXAxis = true;
      } else {
         isShowTimeOnXAxis = tcc.isShowTimeOnXAxisBackup;
         tcc.isForceTimeOnXAxis = false;
      }
      tcc.isShowTimeOnXAxis = isShowTimeOnXAxis;

      if (isShowTimeOnXAxis) {

         // time is displayed on the X axis

         chartDataModel.setXData(xDataTime);
         chartDataModel.addXyData(xDataTime);

         if (xDataDist != null) {
            chartDataModel.setXData2nd(xDataDist);
            chartDataModel.addXyData(xDataDist);
         }

         final ZonedDateTime tourStartTime = tourData.getTourStartTime();

         // set date/time when x-axis starts
         xDataTime.setHistoryStartDateTime(tourStartTime);

         /*
          * When time is displayed, the x-axis can show the start time starting from 0 or from the
          * current time of the day
          */
         final int tourTimeOfDay = tourStartTime.get(ChronoField.SECOND_OF_DAY);

         final int photoTimeOfDay = tourTimeOfDay - tourData.getPhotoTimeAdjustment();

         final X_AXIS_START_TIME configXAxisTime = tcc.xAxisTime;
         final double xAxisStartValue = configXAxisTime == X_AXIS_START_TIME.START_WITH_0
               ? 0
               : configXAxisTime == X_AXIS_START_TIME.TOUR_START_TIME
                     ? tourTimeOfDay
                     : photoTimeOfDay;

         xDataTime.setUnitStartValue(xAxisStartValue);

         xDataTime.setIsTimeSerieWithTimeZoneAdjustment(tourData.isTimeSerieWithTimeZoneAdjustment());

      } else {

         // distance is available and is displayed on the x axis

         chartDataModel.setXData(xDataDist);
         chartDataModel.setXData2nd(xDataTime);

         chartDataModel.addXyData(xDataDist);
         chartDataModel.addXyData(xDataTime);
      }

      /*
       * Don't draw a (visible) line when a break occures, break time can be minutes, hours or days.
       * This feature prevents to draw triangles between 2 value points
       */
      xDataTime.setNoLine(tourData.getBreakTimeSerie());

      ChartType chartType;
      final String chartTypeName = _prefStore.getString(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE);
      try {
         chartType = ChartType.valueOf(ChartType.class, chartTypeName);
      } catch (final Exception e) {
         // set default value
         chartType = ChartType.LINE;
      }

      /*
       * Graph background
       */

      // HR zones can be displayed when they are available
      final boolean canShowBackground_HrZones = tcc.canShowBackground_HrZones = tourData.getNumberOfHrZones() > 0;

      // swim style can be displayed when they are available
      final boolean canShowBackground_SwimStyle = tcc.canShowBackground_SwimStyle = tourData.swim_Time != null;

      final String prefGraphBgSource = _prefStore.getString(ITourbookPreferences.GRAPH_BACKGROUND_SOURCE);
      final String prefGraphBgStyle = _prefStore.getString(ITourbookPreferences.GRAPH_BACKGROUND_STYLE);

      GraphBackgroundSource graphBgSource = (GraphBackgroundSource) Util.getEnumValue(prefGraphBgSource,
            TourChartConfiguration.GRAPH_BACKGROUND_SOURCE_DEFAULT);
      final GraphBackgroundStyle graphBgStyle = (GraphBackgroundStyle) Util.getEnumValue(prefGraphBgStyle,
            TourChartConfiguration.GRAPH_BACKGROUND_STYLE_DEFAULT);

      final boolean prefShow_HrZone = GraphBackgroundSource.HR_ZONE.name().equals(prefGraphBgSource);
      final boolean prefShow_SwimStyle = GraphBackgroundSource.SWIMMING_STYLE.name().equals(prefGraphBgSource);

      // check if data are available for the requested background source
      if (prefShow_HrZone) {

         if (canShowBackground_HrZones == false) {

            // hr zones cannot be displayed -> show default
            graphBgSource = GraphBackgroundSource.DEFAULT;
         }

      } else if (prefShow_SwimStyle && canShowBackground_SwimStyle == false) {

         // swimming style cannot be displayed -> show default
         graphBgSource = GraphBackgroundSource.DEFAULT;
      }

      tcc.graphBackground_Source = graphBgSource;
      tcc.graphBackground_Style = graphBgStyle;

      final boolean isHrZoneDisplayed = canShowBackground_HrZones && tcc.isBackgroundStyle_HrZone();
      final boolean isSwimStyleDisplayed = canShowBackground_SwimStyle && tcc.isBackgroundStyle_SwimmingStyle();
      final boolean useCustomBackground = isHrZoneDisplayed || isSwimStyleDisplayed;

// SET_FORMATTING_OFF

      final ChartDataYSerie yDataElevation         = createModelData_Elevation(        tourData, chartDataModel, chartType, useCustomBackground, tcc);
      final ChartDataYSerie yDataPulse             = createModelData_Heartbeat(        tourData, chartDataModel, chartType, useCustomBackground, tcc, isShowTimeOnXAxis);
      final ChartDataYSerie yDataSpeed             = createModelData_Speed(            tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yDataSpeed_Summarized  = createModelData_Speed_Summarized( tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yDataPace              = createModelData_Pace(             tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yDataPace_Summarized   = createModelData_Pace_Summarized(  tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yDataPower             = createModelData_Power(            tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yDataAltimeter         = createModelData_Altimeter(        tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yDataGradient          = createModelData_Gradient(         tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yDataCadence           = createModelData_Cadence(          tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yDataGears             = createModelData_Gears(            tourData, chartDataModel);
      final ChartDataYSerie yDataTemperature       = createModelData_Temperature(      tourData, chartDataModel, chartType, useCustomBackground);

      final ChartDataYSerie yDataTourCompare       = createModelData_TourCompare(tourData, chartDataModel, chartType, tcc);

      final ChartDataYSerie yData_RunDyn_StanceTime            = createModelData_RunDyn_StanceTime(         tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yData_RunDyn_StanceTimeBalance     = createModelData_RunDyn_StanceTimeBalance(  tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yData_RunDyn_StepLength            = createModelData_RunDyn_StepLength(         tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yData_RunDyn_VerticalOscillation   = createModelData_RunDyn_VerticalOscillation(tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yData_RunDyn_VerticalRatio         = createModelData_RunDyn_VerticalRatio(      tourData, chartDataModel, chartType, useCustomBackground);

      final ChartDataYSerie yData_Swim_Strokes                 = createModelData_Swim_Strokes(              tourData, chartDataModel, chartType, useCustomBackground);
      final ChartDataYSerie yData_Swim_Swolf                   = createModelData_Swim_Swolf(                tourData, chartDataModel, chartType, useCustomBackground);

// SET_FORMATTING_ON

      /*
       * all visible graphs are added as y-data to the chart data model in the sequence as they were
       * activated
       */
      for (final int actionId : tcc.getVisibleGraphs()) {

         switch (actionId) {
         case GRAPH_ALTITUDE:
            if (yDataElevation != null) {
               chartDataModel.addYData(yDataElevation);
               chartDataModel.setCustomData(CUSTOM_DATA_ALTITUDE, yDataElevation);
            }
            break;

         case GRAPH_PULSE:
            if (yDataPulse != null) {
               chartDataModel.addYData(yDataPulse);
               chartDataModel.setCustomData(CUSTOM_DATA_PULSE, yDataPulse);
            }
            break;

         case GRAPH_SPEED:
            if (yDataSpeed != null) {
               chartDataModel.addYData(yDataSpeed);
               chartDataModel.setCustomData(CUSTOM_DATA_SPEED, yDataSpeed);
            }
            break;

         case GRAPH_SPEED_SUMMARIZED:
            if (yDataSpeed != null) {
               chartDataModel.addYData(yDataSpeed_Summarized);
               chartDataModel.setCustomData(CUSTOM_DATA_SPEED_SUMMARIZED, yDataSpeed_Summarized);
            }
            break;

         case GRAPH_PACE:
            if (yDataPace != null) {
               chartDataModel.addYData(yDataPace);
               chartDataModel.setCustomData(CUSTOM_DATA_PACE, yDataPace);
            }
            break;

         case GRAPH_PACE_SUMMARIZED:
            if (yDataPace != null) {
               chartDataModel.addYData(yDataPace_Summarized);
               chartDataModel.setCustomData(CUSTOM_DATA_PACE, yDataPace_Summarized);
            }
            break;

         case GRAPH_POWER:
            if (yDataPower != null) {
               chartDataModel.addYData(yDataPower);
               chartDataModel.setCustomData(CUSTOM_DATA_POWER, yDataPower);
            }
            break;

         case GRAPH_ALTIMETER:
            if (yDataAltimeter != null) {
               chartDataModel.addYData(yDataAltimeter);
               chartDataModel.setCustomData(CUSTOM_DATA_ALTIMETER, yDataAltimeter);
            }
            break;

         case GRAPH_GRADIENT:
            if (yDataGradient != null) {
               chartDataModel.addYData(yDataGradient);
               chartDataModel.setCustomData(CUSTOM_DATA_GRADIENT, yDataGradient);
            }
            break;

         case GRAPH_CADENCE:
            if (yDataCadence != null) {
               chartDataModel.addYData(yDataCadence);
               chartDataModel.setCustomData(CUSTOM_DATA_CADENCE, yDataCadence);
            }
            break;

         case GRAPH_GEARS:
            if (yDataGears != null) {
               chartDataModel.addYData(yDataGears);
               chartDataModel.setCustomData(CUSTOM_DATA_GEAR_RATIO, yDataGears);
            }
            break;

         case GRAPH_TEMPERATURE:
            if (yDataTemperature != null) {
               chartDataModel.addYData(yDataTemperature);
               chartDataModel.setCustomData(CUSTOM_DATA_TEMPERATURE, yDataTemperature);
            }
            break;

         case GRAPH_TOUR_COMPARE:
            if (yDataTourCompare != null) {
               chartDataModel.addYData(yDataTourCompare);
            }
            break;

         case GRAPH_RUN_DYN_STANCE_TIME:
            if (yData_RunDyn_StanceTime != null) {
               chartDataModel.addYData(yData_RunDyn_StanceTime);
               chartDataModel.setCustomData(CUSTOM_DATA_RUN_DYN_STANCE_TIME, yData_RunDyn_StanceTime);
            }
            break;

         case GRAPH_RUN_DYN_STANCE_TIME_BALANCED:
            if (yData_RunDyn_StanceTimeBalance != null) {
               chartDataModel.addYData(yData_RunDyn_StanceTimeBalance);
               chartDataModel.setCustomData(CUSTOM_DATA_RUN_DYN_STANCE_TIME_BALANCE, yData_RunDyn_StanceTimeBalance);
            }
            break;

         case GRAPH_RUN_DYN_STEP_LENGTH:
            if (yData_RunDyn_StepLength != null) {
               chartDataModel.addYData(yData_RunDyn_StepLength);
               chartDataModel.setCustomData(CUSTOM_DATA_RUN_DYN_STEP_LENGTH, yData_RunDyn_StepLength);
            }
            break;

         case GRAPH_RUN_DYN_VERTICAL_OSCILLATION:
            if (yData_RunDyn_VerticalOscillation != null) {
               chartDataModel.addYData(yData_RunDyn_VerticalOscillation);
               chartDataModel.setCustomData(CUSTOM_DATA_RUN_DYN_VERTICAL_OSCILLATION, yData_RunDyn_VerticalOscillation);
            }
            break;

         case GRAPH_RUN_DYN_VERTICAL_RATIO:
            if (yData_RunDyn_VerticalRatio != null) {
               chartDataModel.addYData(yData_RunDyn_VerticalRatio);
               chartDataModel.setCustomData(CUSTOM_DATA_RUN_DYN_VERTICAL_RATIO, yData_RunDyn_VerticalRatio);
            }
            break;

         case GRAPH_SWIM_STROKES:
            if (yData_Swim_Strokes != null) {
               chartDataModel.addYData(yData_Swim_Strokes);
               chartDataModel.setCustomData(CUSTOM_DATA_SWIM_STROKES, yData_Swim_Strokes);
            }
            break;

         case GRAPH_SWIM_SWOLF:
            if (yData_Swim_Swolf != null) {
               chartDataModel.addYData(yData_Swim_Swolf);
               chartDataModel.setCustomData(CUSTOM_DATA_SWIM_SWOLF, yData_Swim_Swolf);
            }
            break;

         default:
            break;
         }
      }

      if (isHistorySerie) {

         chartDataModel.setChartType(ChartType.HISTORY);

         xDataTime.setAxisUnit(ChartDataSerie.X_AXIS_UNIT_HISTORY);

         // set date/time when x-axis starts
         xDataTime.setHistoryStartDateTime(tourData.getTourStartTime());

         // history do not have any y-data, create dummy values
         final float[] yHistorySerie = new float[tourData.timeSerieHistory.length];
         final ChartDataYSerie yDataHistory = createChartDataSerie(yHistorySerie, ChartType.HISTORY);

         yDataHistory.setAxisUnit(ChartDataSerie.AXIS_UNIT_HISTORY);

         setGraphColors(yDataHistory, GraphColorManager.PREF_GRAPH_HISTORY);

         chartDataModel.addXyData(yDataHistory);
         chartDataModel.addYData(yDataHistory);
         chartDataModel.setCustomData(CUSTOM_DATA_HISTORY, yDataHistory);

      } else if (tourData.isMultipleTours()) {

         createStatisticSegments(tourData, chartDataModel);
      }

      chartDataModel.setIsGraphOverlapped(tcc.isGraphOverlapped);
      chartDataModel.setShowNoLineValues(tcc.isShowBreaktimeValues);
      chartDataModel.setShowValuePointValue(tcc.isShowValuePointValue);

      chartDataModel.setCustomData(CUSTOM_DATA_TIME, xDataTime);
      chartDataModel.setCustomData(CUSTOM_DATA_DISTANCE, xDataDist);

      chartDataModel.setCustomData(CUSTOM_DATA_TOUR_DATA, tourData);
      chartDataModel.setCustomData(Chart.CUSTOM_DATA_TOUR_ID, tourData.getTourId());

      chartDataModel.setCustomData(CUSTOM_DATA_TOUR_CHART_CONFIGURATION, tcc);

      return chartDataModel;
   }

   private ChartDataYSerie createChartDataSerie(final float[] dataSerie, final ChartType chartType) {
      return new ChartDataYSerie(chartType, dataSerie, false);
   }

   private ChartDataYSerie createChartDataSerie(final float[][] dataSerie, final ChartType chartType) {
      return new ChartDataYSerie(chartType, dataSerie);
   }

   /**
    * 0 values will be ignored when computing min/maxvalues.
    *
    * @param dataSerie
    * @param chartType
    * @return
    */
   private ChartDataYSerie createChartDataSerieNoZero(final float[] dataSerie, final ChartType chartType) {
      return new ChartDataYSerie(chartType, dataSerie, true);
   }

   /**
    * 0 values will be ignored when computing min/maxvalues.
    *
    * @param dataSerie
    * @param chartType
    * @return
    */
   private ChartDataYSerie createChartDataSerieNoZero(final float[][] dataSerie, final ChartType chartType) {
      return new ChartDataYSerie(chartType, dataSerie, true);
   }

   /**
    * Altimeter
    */
   private ChartDataYSerie createModelData_Altimeter(final TourData tourData,
                                                     final ChartDataModel chartDataModel,
                                                     final ChartType chartType,
                                                     final boolean useGraphBgStyle) {

      final float[] altimeterSerie = tourData.getAltimeterSerie();
      ChartDataYSerie yDataAltimeter = null;
      if (altimeterSerie != null) {

         yDataAltimeter = createChartDataSerieNoZero(altimeterSerie, chartType);

         yDataAltimeter.setYTitle(GRAPH_LABEL_ALTIMETER);
         yDataAltimeter.setUnitLabel(UI.UNIT_LABEL_ALTIMETER);
         yDataAltimeter.setShowYSlider(true);
         yDataAltimeter.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTIMETER);
         yDataAltimeter.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, false, _computeAvg_Altimeter, 0));

         if (useGraphBgStyle) {
            yDataAltimeter.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataAltimeter.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
         }

         setGraphColors(yDataAltimeter, GraphColorManager.PREF_GRAPH_ALTIMETER);
         chartDataModel.addXyData(yDataAltimeter);

         /*
          * adjust min/max altitude when it's defined in the pref store
          */
         if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_ALTIMETER_IS_MIN_ENABLED)) {

            final int minValue = _prefStore.getInt(ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE);

            yDataAltimeter.forceYAxisMinValue(minValue + TourChart.MIN_ADJUSTMENT);
         }

         if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED)) {

            final double maxValue = _prefStore.getInt(ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE);

            final double maxAdjust = 1e-2;
            // set max value after min value, adjust max otherwise values above the max are painted
            yDataAltimeter.forceYAxisMaxValue(
                  maxValue > 0 //
                        ? maxValue - maxAdjust
                        : maxValue + maxAdjust);
         }

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataAltimeter,
               1,
               1e-2,
               ITourbookPreferences.GRAPH_ALTIMETER_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE,
               ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE);
      }
      return yDataAltimeter;
   }

   /**
    * Cadence
    */
   private ChartDataYSerie createModelData_Cadence(final TourData tourData,
                                                   final ChartDataModel chartDataModel,
                                                   final ChartType chartType,
                                                   final boolean useGraphBgStyle) {

      final float[] cadenceSerie = tourData.getCadenceSerieWithMuliplier();
      ChartDataYSerie yDataCadence = null;
      if (cadenceSerie != null) {

         final String cadenceUnit;

         if (tourData.isMultipleTours()) {

            final boolean isRpm = tourData.multipleTour_IsCadenceRpm;
            final boolean isSpm = tourData.multipleTour_IsCadenceSpm;

            cadenceUnit = isRpm && isSpm
                  ? GRAPH_LABEL_CADENCE_UNIT_RPM_SPM //
                  : isSpm //
                        ? GRAPH_LABEL_CADENCE_UNIT_SPM
                        : GRAPH_LABEL_CADENCE_UNIT;

         } else {

            cadenceUnit = tourData.isCadenceSpm() //
                  ? GRAPH_LABEL_CADENCE_UNIT_SPM
                  : GRAPH_LABEL_CADENCE_UNIT;
         }

         yDataCadence = createChartDataSerieNoZero(cadenceSerie, chartType);

         yDataCadence.setYTitle(GRAPH_LABEL_CADENCE);
         yDataCadence.setUnitLabel(cadenceUnit);
         yDataCadence.setShowYSlider(true);
         yDataCadence.setDisplayedFractionalDigits(1);
         yDataCadence.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_CADENCE);
         yDataCadence.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, _computeAvg_Cadence));
         yDataCadence.setSliderLabelProvider(new SliderLabelProvider_Cadence(cadenceSerie));

         if (useGraphBgStyle) {
            yDataCadence.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataCadence.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
         }

         setGraphColors(yDataCadence, GraphColorManager.PREF_GRAPH_CADENCE);
         chartDataModel.addXyData(yDataCadence);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataCadence,
               1,
               0,
               ITourbookPreferences.GRAPH_CADENCE_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_CADENCE_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_CADENCE_MIN_VALUE,
               ITourbookPreferences.GRAPH_CADENCE_MAX_VALUE);
      }
      return yDataCadence;
   }

   /**
    * Elevation
    */
   private ChartDataYSerie createModelData_Elevation(final TourData tourData,
                                                     final ChartDataModel chartDataModel,
                                                     final ChartType chartType,
                                                     final boolean useGraphBgStyle,
                                                     final TourChartConfiguration tcc) {
      ChartDataYSerie yDataElevation = null;

      final float[] altitudeSerie = tourData.getAltitudeSmoothedSerie(true);
      if (altitudeSerie != null) {

         if (tourData.isSRTMAvailable()) {

            tcc.canShowSRTMData = true;

            if (tcc.isSRTMDataVisible) {

               final float[] srtmDataSerie = tourData.getSRTMSerie(tcc.isShowSrtm1Values);
               if (srtmDataSerie != null) {

                  // create altitude dataserie and adjust min/max values with with the srtm values
                  yDataElevation = createChartDataSerie(
                        new float[][] { altitudeSerie, srtmDataSerie },
                        chartType);
               }
            }

         } else {

            // SRTM data are not available
            tcc.canShowSRTMData = false;
         }

         if (yDataElevation == null) {
            yDataElevation = createChartDataSerieNoZero(altitudeSerie, chartType);
         }

         yDataElevation.setYTitle(GRAPH_LABEL_ALTITUDE);
         yDataElevation.setUnitLabel(UI.UNIT_LABEL_ELEVATION);
         yDataElevation.setShowYSlider(true);
         yDataElevation.setDisplayedFractionalDigits(2);
         yDataElevation.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTITUDE);
         yDataElevation.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, false, _computeAvg_Altitude, 0));

         if (useGraphBgStyle) {
            yDataElevation.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataElevation.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
         }

         setGraphColors(yDataElevation, GraphColorManager.PREF_GRAPH_ALTITUDE);
         chartDataModel.addXyData(yDataElevation);

         // adjust pulse min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataElevation,
               1,
               0,
               ITourbookPreferences.GRAPH_ALTITUDE_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_ALTITUDE_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_ALTITUDE_MIN_VALUE,
               ITourbookPreferences.GRAPH_ALTITUDE_MAX_VALUE);
      }
      return yDataElevation;
   }

   /**
    * Gears
    */
   private ChartDataYSerie createModelData_Gears(final TourData tourData, final ChartDataModel chartDataModel) {

      final float[][] gearSerie = tourData.getGears();
      ChartDataYSerie yDataGears = null;
      if (gearSerie != null) {

         final float[][] chartGearSerie = new float[][] {

               // gear ratio
               gearSerie[0],

               // front gear number, starting with 1 for the large chainwheel (Kettenblatt)
               gearSerie[3] //
         };

         yDataGears = createChartDataSerieNoZero(chartGearSerie, ChartType.HORIZONTAL_BAR);

         yDataGears.setYTitle(GRAPH_LABEL_GEARS);
         yDataGears.setShowYSlider(true);
         yDataGears.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_GEARS);
         yDataGears.setSliderLabelProvider(new SliderLabelProvider_Gear(gearSerie));

//         yDataGears.setLineGaps(tourData.getCadenceGaps());

         setGraphColors(yDataGears, GraphColorManager.PREF_GRAPH_GEAR);
         chartDataModel.addXyData(yDataGears);
      }
      return yDataGears;
   }

   /**
    * Gradient
    */
   private ChartDataYSerie createModelData_Gradient(final TourData tourData,
                                                    final ChartDataModel chartDataModel,
                                                    final ChartType chartType,
                                                    final boolean useGraphBgStyle) {

      final float[] gradientSerie = tourData.gradientSerie;
      ChartDataYSerie yDataGradient = null;
      if (gradientSerie != null) {

         yDataGradient = createChartDataSerie(gradientSerie, chartType);

         yDataGradient.setYTitle(GRAPH_LABEL_GRADIENT);
         yDataGradient.setUnitLabel(GRAPH_LABEL_GRADIENT_UNIT);
         yDataGradient.setShowYSlider(true);
         yDataGradient.setDisplayedFractionalDigits(1);
         yDataGradient.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_GRADIENT);
         yDataGradient.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, true, _computeAvg_Gradient, 1));

         if (useGraphBgStyle) {
            yDataGradient.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataGradient.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
         }

         setGraphColors(yDataGradient, GraphColorManager.PREF_GRAPH_GRADIENT);
         chartDataModel.addXyData(yDataGradient);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataGradient,
               1,
               TourChart.MAX_ADJUSTMENT,
               ITourbookPreferences.GRAPH_GRADIENT_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_GRADIENT_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE,
               ITourbookPreferences.GRAPH_GRADIENT_MAX_VALUE);
      }
      return yDataGradient;
   }

   /**
    * Heartbeat
    *
    * @param isShowTimeOnXAxis
    */
   private ChartDataYSerie createModelData_Heartbeat(final TourData tourData,
                                                     final ChartDataModel chartDataModel,
                                                     final ChartType chartType,
                                                     final boolean useGraphBgStyle,
                                                     final TourChartConfiguration tcc,
                                                     final boolean isShowTimeOnXAxis) {

      ChartDataYSerie yDataPulse = null;

      final float[] pulseSerie = tourData.getPulse_SmoothedSerie();
      final float[] avgBpmFromRRIntervals = tourData.getPulse_AvgBpmFromRRIntervals();

      final boolean isAvailable_PulseSerie = pulseSerie != null;
      final boolean isAvailable_AvgBpmFromRRIntervals = avgBpmFromRRIntervals != null;

      tcc.canShowPulseSerie = isAvailable_PulseSerie;
      tcc.canShowPulseTimeSerie = isAvailable_AvgBpmFromRRIntervals;

      // set graph/line according to the selection
      switch (tcc.pulseGraph) {

      case RR_AVERAGE_ONLY:

         if (isAvailable_AvgBpmFromRRIntervals) {
            yDataPulse = createChartDataSerie(new float[][] { avgBpmFromRRIntervals }, chartType);
         }
         break;

      case RR_INTERVALS_ONLY:
      case RR_INTERVALS___2ND_RR_AVERAGE:
      case RR_INTERVALS___2ND_DEVICE_BPM:

         final int[] allRRTimesInMilliseconds = tourData.pulseTime_Milliseconds; //    length: numRRTimes

         if (isAvailable_AvgBpmFromRRIntervals && allRRTimesInMilliseconds != null) {

            // data serie which has the same number of slices as the time serie
            final float[] dataSerie = tcc.pulseGraph == PulseGraph.RR_INTERVALS___2ND_DEVICE_BPM
                  ? pulseSerie
                  : avgBpmFromRRIntervals;

            if (isShowTimeOnXAxis == false) {

               // distance is displayed on the x-axis
               //
               // -> too complicated to show the correct RR graph
               // -> show RR avg or device bpm graph

               yDataPulse = createChartDataSerieNoZero(dataSerie, ChartType.LINE);

               break;
            }

            final ChartType rrChartType = tcc.pulseGraph == PulseGraph.RR_INTERVALS_ONLY

                  ? ChartType.VARIABLE_X_AXIS

                  // used for RR average and device bpm
                  : ChartType.VARIABLE_X_AXIS_WITH_2ND_LINE;

            // set a data serie for the 2nd line when it's displayed,
            // when not displayed it is used to compute visible graph min/max values
            yDataPulse = createChartDataSerieNoZero(dataSerie, rrChartType);

            final int[] timeSerie = tourData.timeSerie; //                                length: numTimeSlices

            // length: numTimeSlices
            final int[] timeSerie_WithRRIndex = tourData.pulseTime_TimeIndex;

            final int numTimeSlices = timeSerie.length;
            final int numRRTimes = allRRTimesInMilliseconds.length;

            final DoubleArrayList xData_PulseTime = new DoubleArrayList((int) (numRRTimes * 1.05));
            final FloatArrayList yData_PulseTime = new FloatArrayList((int) (numRRTimes * 1.05));

            /**
             * Synchronize 2 time series which can contain gaps
             *
             * <pre>
             *
             * x-axis     0----1----2----3----4----5----6----7----10----11----12----13---15-  seconds
             *
             * RR times   ------------320---1434-------762---763-------561--785-------1006--  milliseconds
             *
             * .
             * </pre>
             */

            boolean areInitialRRValuesInvalid = false;

            /*
             * Adjust all values with the first value, otherwise the graph looks like it is shifted
             * compared with the device bpm value. This value helps sometimes, but sometimes not.
             */
            double firstValueOffset = 0;
            for (final int rrTimeMS : allRRTimesInMilliseconds) {
               final double rrSliceTime = rrTimeMS / 1000.0;
               if (rrSliceTime > 0) {
                  firstValueOffset = rrSliceTime;
                  break;
               }
            }

            int rrIndex = 0;

            double xAxisRRTime = 0;

            // loop: x-axis time
            for (int timeIndex = 0; timeIndex < numTimeSlices; timeIndex++) {

               final int xAxisTime = timeSerie[timeIndex];
               final int rrIndex_FromTimeSerie = timeSerie_WithRRIndex[timeIndex];

               if (rrIndex_FromTimeSerie == -1) {

                  // RR time index is not valid

                  if (xAxisRRTime == 0) {

                     // keep time before RR values are available

                     areInitialRRValuesInvalid = true;
                  }

               } else {

                  // RR time index is valid

                  // check if the first RR values are invalid
                  if (areInitialRRValuesInvalid && xAxisRRTime == 0) {

                     // initial RR values are invalid -> adjust start of x-axis RR time

                     xAxisRRTime += xAxisTime;

                     xData_PulseTime.add(xAxisTime);
                     yData_PulseTime.add(0);
                  }

                  final int minXAxisDiff = 1;

                  if (xAxisRRTime < xAxisTime) {

                     while (rrIndex < numRRTimes

                           && rrIndex <= rrIndex_FromTimeSerie

                           && xAxisRRTime < xAxisTime

                     ) {

                        final double rrSliceTime = allRRTimesInMilliseconds[rrIndex] / 1000.0;

                        xAxisRRTime += rrSliceTime;
                        final float bpmFromRRTime = (float) (60.0f / rrSliceTime);

                        final double xAxisDiff = xAxisRRTime - xAxisTime;

                        if (xAxisDiff > 5) {

                           /**
                            * <code>
                            *
                            * timeIndex               = 8767
                            * rrIndex                 = 12054
                            * rrIndex_FromTimeSerie   = 12054
                            * xAxisRRTime             = 16349.82
                            * xAxisTime               = 16339
                            *
                            * </code>
                            */

                           xAxisRRTime = xAxisTime;

                           if (_isLogging_RR_Errors) {
                              System.out.println((System.currentTimeMillis() + " xAxisDiff 1: " + xAxisDiff));//$NON-NLS-1$
                           }
                        }

                        xData_PulseTime.add(xAxisRRTime - firstValueOffset);
                        yData_PulseTime.add(bpmFromRRTime);

                        rrIndex++;
                     }

                     if (xAxisRRTime < xAxisTime - minXAxisDiff) {

                        // RR time is still too small -> adjust RR time to x-Axis time

                        xAxisRRTime += xAxisTime - xAxisRRTime;

                        float yPulseTime = 0;
                        if (_isLogging_RR_Errors) {

                           yPulseTime = -100;

                        } else {

                           // show previous time

                           final int numPulseTimes = yData_PulseTime.size();

                           if (numPulseTimes > 0) {
                              yPulseTime = yData_PulseTime.get(numPulseTimes - 1);
                           }
                        }

                        xData_PulseTime.add(xAxisRRTime - firstValueOffset);
                        yData_PulseTime.add(yPulseTime);
                     }

                  } else if (xAxisRRTime > xAxisTime) {

                     // this occured when bpm < 60

                     while (rrIndex < numRRTimes // check array bounds

                           && rrIndex <= rrIndex_FromTimeSerie

                           && xAxisRRTime > xAxisTime

                     ) {

                        final double rrSliceTime = allRRTimesInMilliseconds[rrIndex] / 1000.0;

                        xAxisRRTime += rrSliceTime;
                        final float bpmFromRRTime = (float) (60.0f / rrSliceTime);

                        xData_PulseTime.add(xAxisRRTime - firstValueOffset);
                        yData_PulseTime.add(bpmFromRRTime);

                        rrIndex++;
                     }

                     final double xAxisDiff = xAxisRRTime - xAxisTime;
                     if (xAxisDiff > 5) {

                        /**
                         * <code>
                         *
                         * timeIndex               = 8767
                         * rrIndex                 = 12054
                         * rrIndex_FromTimeSerie   = 12054
                         * xAxisRRTime             = 16349.82
                         * xAxisTime               = 16339
                         *
                         * </code>
                         */

                        xAxisRRTime = xAxisTime;

                        if (_isLogging_RR_Errors) {
                           System.out.println((System.currentTimeMillis() + " xAxisDiff 2: " + xAxisDiff));//$NON-NLS-1$
                        }
                     }

                     if (xAxisRRTime - minXAxisDiff > xAxisTime) {

                        // RR time is still too small -> adjust RR time to x-Axis time

                        rrIndex = rrIndex_FromTimeSerie;

                        xAxisRRTime += xAxisTime - xAxisRRTime;

                        float yPulseTime = 0;
                        if (_isLogging_RR_Errors) {

                           yPulseTime = 500;

                        } else {

                           // show previous time

                           final int numPulseTimes = yData_PulseTime.size();

                           if (numPulseTimes > 0) {
                              yPulseTime = yData_PulseTime.get(numPulseTimes - 1);
                           }
                        }

                        xData_PulseTime.add(xAxisRRTime - firstValueOffset);
                        yData_PulseTime.add(yPulseTime);
                     }
                  }
               }
            }

            // set variable xy data after the arrays are filled up
            chartDataModel.setVariableXYData(
                  xData_PulseTime.toArray(),
                  yData_PulseTime.toArray(),
                  timeSerie_WithRRIndex);
         }

         break;

      case DEVICE_BPM_ONLY:

         if (isAvailable_PulseSerie) {
            yDataPulse = createChartDataSerieNoZero(pulseSerie, chartType);
         }
         break;

      case RR_AVERAGE___2ND_DEVICE_BPM:

         if (isAvailable_PulseSerie && isAvailable_AvgBpmFromRRIntervals) {

            yDataPulse = createChartDataSerie(new float[][]

            {
                  avgBpmFromRRIntervals,
                  pulseSerie
            },
                  chartType);
         }
         break;

      case DEVICE_BPM___2ND_RR_AVERAGE:
      default:

         if (isAvailable_PulseSerie && isAvailable_AvgBpmFromRRIntervals) {

            yDataPulse = createChartDataSerie(new float[][]

            {
                  pulseSerie,
                  avgBpmFromRRIntervals
            },
                  chartType);
         }
      }

      // when user selection could not be applied, try to use the remaining possibilities
      if (yDataPulse == null) {

         if (isAvailable_PulseSerie) {

            yDataPulse = createChartDataSerieNoZero(pulseSerie, chartType);

         } else if (isAvailable_AvgBpmFromRRIntervals) {

            yDataPulse = createChartDataSerieNoZero(avgBpmFromRRIntervals, chartType);
         }
      }

      if (yDataPulse != null) {

         yDataPulse.setYTitle(GRAPH_LABEL_HEARTBEAT);
         yDataPulse.setUnitLabel(GRAPH_LABEL_HEARTBEAT_UNIT);
         yDataPulse.setShowYSlider(true);
         yDataPulse.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PULSE);
         yDataPulse.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, _computeAvg_Pulse));

         if (useGraphBgStyle) {
            yDataPulse.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataPulse.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
         }

         setGraphColors(yDataPulse, GraphColorManager.PREF_GRAPH_HEARTBEAT);
         chartDataModel.addXyData(yDataPulse);

         // adjust  min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataPulse,
               1,
               0,
               ITourbookPreferences.GRAPH_PULSE_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_PULSE_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_PULSE_MIN_VALUE,
               ITourbookPreferences.GRAPH_PULSE_MAX_VALUE);
      }

      return yDataPulse;
   }

   /**
    * Pace
    */
   private ChartDataYSerie createModelData_Pace(final TourData tourData,
                                                final ChartDataModel chartDataModel,
                                                final ChartType chartType,
                                                final boolean useGraphBgStyle) {

      ChartDataYSerie yDataPace = null;

      final float[] paceSerie = tourData.getPaceSerieSeconds();
      if (paceSerie != null) {

         yDataPace = createChartDataSerieNoZero(paceSerie, chartType);

         yDataPace.setYTitle(GRAPH_LABEL_PACE);
         yDataPace.setUnitLabel(UI.UNIT_LABEL_PACE);
         yDataPace.setShowYSlider(true);
         yDataPace.setAxisUnit(ChartDataSerie.AXIS_UNIT_MINUTE_SECOND);
         yDataPace.setSliderLabelFormat(ChartDataYSerie.SLIDER_LABEL_FORMAT_MM_SS);
         yDataPace.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PACE);
         yDataPace.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, false, _computeAvg_Pace, 1));
         yDataPace.setYAxisDirection(!_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SHOW_PACE_GRAPH_INVERTED));

         if (useGraphBgStyle) {
            yDataPace.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataPace.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
         }

         setGraphColors(yDataPace, GraphColorManager.PREF_GRAPH_PACE);
         chartDataModel.addXyData(yDataPace);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataPace,
               60,
               0,
               ITourbookPreferences.GRAPH_PACE_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_PACE_MIN_VALUE,
               ITourbookPreferences.GRAPH_PACE_MAX_VALUE);
      }

      return yDataPace;
   }

   /**
    * Pace summarized average
    */
   private ChartDataYSerie createModelData_Pace_Summarized(final TourData tourData,
                                                           final ChartDataModel chartDataModel,
                                                           final ChartType chartType,
                                                           final boolean useGraphBgStyle) {

      ChartDataYSerie yDataPace = null;

      final float[] paceSerie = tourData.getPaceSerie_Summarized_Seconds();
      if (paceSerie != null) {

         final TourChartAnalyzerInfo analyzerInfo = new TourChartAnalyzerInfo(true, false, null, 1);

         yDataPace = createChartDataSerieNoZero(paceSerie, chartType);

         yDataPace.setYTitle(GRAPH_LABEL_PACE_SUMMARIZED);
         yDataPace.setUnitLabel(UI.UNIT_LABEL_PACE);
         yDataPace.setShowYSlider(true);
         yDataPace.setAxisUnit(ChartDataSerie.AXIS_UNIT_MINUTE_SECOND);
         yDataPace.setSliderLabelFormat(ChartDataYSerie.SLIDER_LABEL_FORMAT_MM_SS);
         yDataPace.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PACE_SUMMARIZED);
         yDataPace.setCustomData(CUSTOM_DATA_ANALYZER_INFO, analyzerInfo);
         yDataPace.setYAxisDirection(!_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SHOW_PACE_GRAPH_INVERTED));

         if (useGraphBgStyle) {
            yDataPace.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataPace.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
         }

         setGraphColors(yDataPace, GraphColorManager.PREF_GRAPH_PACE);
         chartDataModel.addXyData(yDataPace);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataPace,
               60,
               0,
               ITourbookPreferences.GRAPH_PACE_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_PACE_MIN_VALUE,
               ITourbookPreferences.GRAPH_PACE_MAX_VALUE);
      }

      return yDataPace;
   }

   /**
    * Power
    */
   private ChartDataYSerie createModelData_Power(final TourData tourData,
                                                 final ChartDataModel chartDataModel,
                                                 final ChartType chartType,
                                                 final boolean useGraphBgStyle) {

      final float[] powerSerie = tourData.getPowerSerie();
      ChartDataYSerie yDataPower = null;
      if (powerSerie != null) {

         yDataPower = createChartDataSerieNoZero(powerSerie, chartType);

         yDataPower.setYTitle(GRAPH_LABEL_POWER);
         yDataPower.setUnitLabel(GRAPH_LABEL_POWER_UNIT);
         yDataPower.setShowYSlider(true);
         yDataPower.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_POWER);
         yDataPower.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, false, _computeAvg_Power, 0));

         if (useGraphBgStyle) {
            yDataPower.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataPower.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
         }

         setGraphColors(yDataPower, GraphColorManager.PREF_GRAPH_POWER);
         chartDataModel.addXyData(yDataPower);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataPower,
               1,
               0,
               ITourbookPreferences.GRAPH_POWER_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_POWER_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_POWER_MIN_VALUE,
               ITourbookPreferences.GRAPH_POWER_MAX_VALUE);
      }

      return yDataPower;
   }

   /**
    * Running Dynamics: Stance time
    */
   private ChartDataYSerie createModelData_RunDyn_StanceTime(final TourData tourData,
                                                             final ChartDataModel chartDataModel,
                                                             final ChartType chartType,
                                                             final boolean useGraphBgStyle) {

      ChartDataYSerie yDataSerie = null;

      final float[] dataSerie = tourData.getRunDyn_StanceTime();
      if (dataSerie != null) {

         yDataSerie = createChartDataSerie(dataSerie, chartType);

         yDataSerie.setYTitle(GRAPH_LABEL_RUN_DYN_STANCE_TIME);
         yDataSerie.setUnitLabel(UI.UNIT_MS);
         yDataSerie.setShowYSlider(true);
         yDataSerie.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_RUN_DYN_STANCE_TIME);
         yDataSerie.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, _computeAvg_RunDyn_StanceTime));

         if (useGraphBgStyle) {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_NO);
         }

         setGraphColors(yDataSerie, GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME);
         chartDataModel.addXyData(yDataSerie);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataSerie,
               1,
               0,
               ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_MIN_VALUE,
               ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_MAX_VALUE);
      }

      return yDataSerie;
   }

   /**
    * Running Dynamics: Stance time balance
    */
   private ChartDataYSerie createModelData_RunDyn_StanceTimeBalance(final TourData tourData,
                                                                    final ChartDataModel chartDataModel,
                                                                    final ChartType chartType,
                                                                    final boolean useGraphBgStyle) {

      ChartDataYSerie yDataSerie = null;

      final float[] dataSerie = tourData.getRunDyn_StanceTimeBalance();
      if (dataSerie != null) {

         yDataSerie = createChartDataSerie(dataSerie, chartType);

         yDataSerie.setYTitle(GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCE);
         yDataSerie.setUnitLabel(UI.UNIT_PERCENT);
         yDataSerie.setDisplayedFractionalDigits(1);
         yDataSerie.setShowYSlider(true);
         yDataSerie.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_RUN_DYN_STANCE_TIME_BALANCED);
         yDataSerie.setCustomData(CUSTOM_DATA_ANALYZER_INFO,
               new TourChartAnalyzerInfo(true, true, _computeAvg_RunDyn_StanceTimeBalance, 1));

         if (useGraphBgStyle) {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_NO);
         }

         setGraphColors(yDataSerie, GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME_BALANCED);
         chartDataModel.addXyData(yDataSerie);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataSerie,
               1,
               0,
               ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MIN_VALUE,
               ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MAX_VALUE);
      }

      return yDataSerie;
   }

   /**
    * Running Dynamics: Step length
    */
   private ChartDataYSerie createModelData_RunDyn_StepLength(final TourData tourData,
                                                             final ChartDataModel chartDataModel,
                                                             final ChartType chartType,
                                                             final boolean useGraphBgStyle) {

      ChartDataYSerie yDataSerie = null;

      final float[] dataSerie = tourData.getRunDyn_StepLength();
      if (dataSerie != null) {

         yDataSerie = createChartDataSerie(dataSerie, chartType);

         yDataSerie.setYTitle(GRAPH_LABEL_RUN_DYN_STEP_LENGTH);
         yDataSerie.setUnitLabel(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
         yDataSerie.setShowYSlider(true);
         yDataSerie.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_RUN_DYN_STEP_LENGTH);
         yDataSerie.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, _computeAvg_RunDyn_StepLength));

         if (useGraphBgStyle) {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_NO);
         }

         setGraphColors(yDataSerie, GraphColorManager.PREF_GRAPH_RUN_DYN_STEP_LENGTH);
         chartDataModel.addXyData(yDataSerie);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataSerie,
               UI.UNIT_VALUE_DISTANCE_MM_OR_INCH,
               0.1,
               ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_MIN_VALUE,
               ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_MAX_VALUE);
      }

      return yDataSerie;
   }

   /**
    * Running Dynamics: Vertical oscillation
    */
   private ChartDataYSerie createModelData_RunDyn_VerticalOscillation(final TourData tourData,
                                                                      final ChartDataModel chartDataModel,
                                                                      final ChartType chartType,
                                                                      final boolean useGraphBgStyle) {

      ChartDataYSerie yDataSerie = null;

      final float[] dataSerie = tourData.getRunDyn_VerticalOscillation();
      if (dataSerie != null) {

         yDataSerie = createChartDataSerie(dataSerie, chartType);

         yDataSerie.setYTitle(GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION);
         yDataSerie.setUnitLabel(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
         yDataSerie.setDisplayedFractionalDigits(1);
         yDataSerie.setShowYSlider(true);
         yDataSerie.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_RUN_DYN_VERTICAL_OSCILLATION);
         yDataSerie.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, true, _computeAvg_RunDyn_VerticalOscillation, 1));

         if (useGraphBgStyle) {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_NO);
         }

         setGraphColors(yDataSerie, GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_OSCILLATION);
         chartDataModel.addXyData(yDataSerie);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataSerie,
               UI.UNIT_VALUE_DISTANCE_MM_OR_INCH,
               0.1,
               ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MIN_VALUE,
               ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MAX_VALUE);
      }

      return yDataSerie;
   }

   /**
    * Running Dynamics: Vertical ratio
    */
   private ChartDataYSerie createModelData_RunDyn_VerticalRatio(final TourData tourData,
                                                                final ChartDataModel chartDataModel,
                                                                final ChartType chartType,
                                                                final boolean useGraphBgStyle) {

      ChartDataYSerie yDataSerie = null;

      final float[] dataSerie = tourData.getRunDyn_VerticalRatio();
      if (dataSerie != null) {

         yDataSerie = createChartDataSerie(dataSerie, chartType);

         yDataSerie.setYTitle(GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO);
         yDataSerie.setUnitLabel(UI.UNIT_PERCENT);
         yDataSerie.setDisplayedFractionalDigits(1);
         yDataSerie.setShowYSlider(true);
         yDataSerie.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_RUN_DYN_VERTICAL_RATIO);
         yDataSerie.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, true, _computeAvg_RunDyn_VerticalRatio, 1));

         if (useGraphBgStyle) {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_NO);
         }

         setGraphColors(yDataSerie, GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_RATIO);
         chartDataModel.addXyData(yDataSerie);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataSerie,
               1,
               0,
               ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_MIN_VALUE,
               ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_MAX_VALUE);
      }

      return yDataSerie;
   }

   /**
    * Speed
    */
   private ChartDataYSerie createModelData_Speed(final TourData tourData,
                                                 final ChartDataModel chartDataModel,
                                                 final ChartType chartType,
                                                 final boolean useGraphBgStyle) {

      ChartDataYSerie yDataSpeed = null;

      final float[] speedSerie = tourData.getSpeedSerie();
      if (speedSerie != null) {

         yDataSpeed = createChartDataSerieNoZero(speedSerie, chartType);

         yDataSpeed.setYTitle(GRAPH_LABEL_SPEED);
         yDataSpeed.setUnitLabel(UI.UNIT_LABEL_SPEED);
         yDataSpeed.setShowYSlider(true);
         yDataSpeed.setDisplayedFractionalDigits(1);
         yDataSpeed.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_SPEED);
         yDataSpeed.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, true, _computeAvg_Speed, 1));

         if (useGraphBgStyle) {
            yDataSpeed.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataSpeed.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
         }

         setGraphColors(yDataSpeed, GraphColorManager.PREF_GRAPH_SPEED);
         chartDataModel.addXyData(yDataSpeed);

         // adjust  min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataSpeed,
               1,
               0,
               ITourbookPreferences.GRAPH_SPEED_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_SPEED_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_SPEED_MIN_VALUE,
               ITourbookPreferences.GRAPH_SPEED_MAX_VALUE);
      }

      return yDataSpeed;
   }

   /**
    * Speed summarized average
    */
   private ChartDataYSerie createModelData_Speed_Summarized(final TourData tourData,
                                                            final ChartDataModel chartDataModel,
                                                            final ChartType chartType,
                                                            final boolean useGraphBgStyle) {

      ChartDataYSerie yDataSpeed = null;

      final float[] speedSerie = tourData.getSpeedSerie_Summarized();
      if (speedSerie != null) {

         final TourChartAnalyzerInfo analyzerInfo = new TourChartAnalyzerInfo(true, true, null, 2);

         yDataSpeed = createChartDataSerieNoZero(speedSerie, chartType);

         yDataSpeed.setYTitle(GRAPH_LABEL_SPEED_SUMMARIZED);
         yDataSpeed.setUnitLabel(UI.UNIT_LABEL_SPEED);
         yDataSpeed.setShowYSlider(true);
         yDataSpeed.setDisplayedFractionalDigits(2);
         yDataSpeed.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_SPEED_SUMMARIZED);
         yDataSpeed.setCustomData(CUSTOM_DATA_ANALYZER_INFO, analyzerInfo);

         if (useGraphBgStyle) {
            yDataSpeed.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataSpeed.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
         }

         setGraphColors(yDataSpeed, GraphColorManager.PREF_GRAPH_SPEED);
         chartDataModel.addXyData(yDataSpeed);

         // adjust  min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataSpeed,
               1,
               0,
               ITourbookPreferences.GRAPH_SPEED_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_SPEED_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_SPEED_MIN_VALUE,
               ITourbookPreferences.GRAPH_SPEED_MAX_VALUE);
      }

      return yDataSpeed;
   }

   /**
    * Swimming: Strokes
    */
   private ChartDataYSerie createModelData_Swim_Strokes(final TourData tourData,
                                                        final ChartDataModel chartDataModel,
                                                        final ChartType chartType,
                                                        final boolean useGraphBgStyle) {

      ChartDataYSerie yDataSerie = null;

      final float[] dataSerie = tourData.getSwim_Strokes();
      if (dataSerie != null) {

         yDataSerie = createChartDataSerie(dataSerie, chartType);

         yDataSerie.setYTitle(GRAPH_LABEL_SWIM_STROKES);
         yDataSerie.setUnitLabel(GRAPH_LABEL_SWIM_STROKES);
         yDataSerie.setShowYSlider(true);
         yDataSerie.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_SWIM_STROKES);
//         yDataSerie.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, _computeAvg_RunDyn_StanceTime));

         if (useGraphBgStyle) {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_NO);
         }

         setGraphColors(yDataSerie, GraphColorManager.PREF_GRAPH_SWIM_STROKES);
         chartDataModel.addXyData(yDataSerie);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataSerie,
               1,
               0,
               ITourbookPreferences.GRAPH_SWIM_STROKES_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_SWIM_STROKES_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_SWIM_STROKES_MIN_VALUE,
               ITourbookPreferences.GRAPH_SWIM_STROKES_MAX_VALUE);
      }

      return yDataSerie;
   }

   /**
    * Swimming: Swolf
    */
   private ChartDataYSerie createModelData_Swim_Swolf(final TourData tourData,
                                                      final ChartDataModel chartDataModel,
                                                      final ChartType chartType,
                                                      final boolean useGraphBgStyle) {

      ChartDataYSerie yDataSerie = null;

      final float[] dataSerie = tourData.getSwim_Swolf();
      if (dataSerie != null) {

         yDataSerie = createChartDataSerie(dataSerie, chartType);

         yDataSerie.setYTitle(GRAPH_LABEL_SWIM_SWOLF);
         yDataSerie.setUnitLabel(GRAPH_LABEL_SWIM_SWOLF);
         yDataSerie.setShowYSlider(true);
         yDataSerie.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_SWIM_SWOLF);
//         yDataSerie.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, _computeAvg_RunDyn_StanceTime));

         if (useGraphBgStyle) {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataSerie.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_NO);
         }

         setGraphColors(yDataSerie, GraphColorManager.PREF_GRAPH_SWIM_SWOLF);
         chartDataModel.addXyData(yDataSerie);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataSerie,
               1,
               0,
               ITourbookPreferences.GRAPH_SWIM_SWOLF_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_SWIM_SWOLF_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_SWIM_SWOLF_MIN_VALUE,
               ITourbookPreferences.GRAPH_SWIM_SWOLF_MAX_VALUE);
      }

      return yDataSerie;
   }

   /**
    * Temperature
    */
   private ChartDataYSerie createModelData_Temperature(final TourData tourData,
                                                       final ChartDataModel chartDataModel,
                                                       final ChartType chartType,
                                                       final boolean useGraphBgStyle) {

      final float[] temperatureSerie = tourData.getTemperatureSerie();
      ChartDataYSerie yDataTemperature = null;
      if (temperatureSerie != null) {

         yDataTemperature = createChartDataSerie(temperatureSerie, chartType);

         yDataTemperature.setYTitle(GRAPH_LABEL_TEMPERATURE);
         yDataTemperature.setUnitLabel(UI.UNIT_LABEL_TEMPERATURE);
         yDataTemperature.setShowYSlider(true);
         yDataTemperature.setDisplayedFractionalDigits(1);
         yDataTemperature.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_TEMPERATURE);
         yDataTemperature.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, true));

         if (useGraphBgStyle) {
            yDataTemperature.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
         } else {
            yDataTemperature.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
         }

         setGraphColors(yDataTemperature, GraphColorManager.PREF_GRAPH_TEMPTERATURE);
         chartDataModel.addXyData(yDataTemperature);

         // adjust min/max values when it's defined in the pref store
         setVisibleForcedValues(
               yDataTemperature,
               1,
               0,
               ITourbookPreferences.GRAPH_TEMPERATURE_IS_MIN_ENABLED,
               ITourbookPreferences.GRAPH_TEMPERATURE_IS_MAX_ENABLED,
               ITourbookPreferences.GRAPH_TEMPERATURE_MIN_VALUE,
               ITourbookPreferences.GRAPH_TEMPERATURE_MAX_VALUE);
      }

      return yDataTemperature;
   }

   /**
    * Tour compare altitude or lat/lon difference
    */
   private ChartDataYSerie createModelData_TourCompare(final TourData tourData,
                                                       final ChartDataModel chartDataModel,
                                                       final ChartType chartType,
                                                       final TourChartConfiguration tcc) {

      final float[] tourCompareSerie = tourData.tourCompareSerie;
      ChartDataYSerie yDataTourCompare = null;
      if (tourCompareSerie != null && tourCompareSerie.length > 0 && tcc.canShowTourCompareGraph) {

         yDataTourCompare = createChartDataSerie(tourCompareSerie, chartType);

         yDataTourCompare.setYTitle(GRAPH_LABEL_TOUR_COMPARE);
         yDataTourCompare.setUnitLabel(tcc.isGeoCompareDiff
               ? GRAPH_LABEL_GEO_COMPARE_UNIT
               : GRAPH_LABEL_TOUR_COMPARE_UNIT);
         yDataTourCompare.setShowYSlider(true);
         yDataTourCompare.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
         yDataTourCompare.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_TOUR_COMPARE);

         setGraphColors(yDataTourCompare, GraphColorManager.PREF_GRAPH_TOUR_COMPARE);
         chartDataModel.addXyData(yDataTourCompare);
      }

      return yDataTourCompare;
   }

   public TourChart getActiveTourChart() {
      return _activeTourChart;
   }

   /**
    * @param tourIds
    * @return Returns a list with {@link TourData} for all tour ids. <code>Null</code> is returned
    *         when {@link TourData} are not available.
    */
   public List<TourData> getTourData(final List<Long> tourIds) {

      final ArrayList<TourData> tourDataList = new ArrayList<>();

      for (final Long tourId : tourIds) {
         final TourData tourData = getTourData(tourId);
         if (tourData != null) {
            tourDataList.add(tourData);
         }
      }

      return tourDataList.isEmpty() ? null : tourDataList;
   }

   /**
    * Fills a list with {@link TourData} from it's id's.
    *
    * @param allTourData
    * @param tourIds
    */
   public void getTourData(final List<TourData> allTourData, final List<Long> tourIds) {

      for (final Long tourId : tourIds) {
         allTourData.add(getTourData(tourId));
      }
   }

   /**
    * Get a tour from the cache, the cache is necessary because getting a tour from the database
    * creates always a new instance.
    *
    * @param requestedTourId
    * @return Returns the tour data for the tour id or <code>null</code> when tour is not in the
    *         database.
    */
   public TourData getTourData(final Long requestedTourId) {

      if (requestedTourId == null) {
         return null;
      }

      /*
       * get tour from tour editor when it contains the requested tour
       */
      if (_tourDataEditorInstance != null) {

         final TourData tourDataInEditor = _tourDataEditorInstance.getTourData();
         if (tourDataInEditor != null && tourDataInEditor.getTourId().equals(requestedTourId)) {

            // cache tour data
            _tourDataCache.put(tourDataInEditor.getTourId(), tourDataInEditor);

            return tourDataInEditor;
         }
      }

      /*
       * get tour from cache or database
       */
      TourData existingTourData = null;
      final TourData tourDataInCache = _tourDataCache.get(requestedTourId);

      if (tourDataInCache != null) {
         existingTourData = tourDataInCache;
      } else {

         final TourData tourDataFromDb = TourDatabase.getTourFromDb(requestedTourId);

         if (tourDataFromDb == null) {

            // try to get tour from raw data manager
            final TourData tourDataFromRawManager = RawDataManager
                  .getInstance()
                  .getImportedTours()
                  .get(requestedTourId);

            return tourDataFromRawManager;
         }

         // cache tour data
         _tourDataCache.put(tourDataFromDb.getTourId(), tourDataFromDb);

         existingTourData = tourDataFromDb;
      }

      if (existingTourData != null) {
         replaceTourInTourEditor(existingTourData);
      }

      return existingTourData;
   }

   /**
    * Get a tour from the database and keep it in the cache
    *
    * @param tourId
    * @return Returns the tour data for the tour id or <code>null</code> when the tour is not in the
    *         database
    */
   public TourData getTourDataFromDb(final Long tourId) {

      if (tourId == null) {
         return null;
      }

      final TourData tourDataFromDb = TourDatabase.getTourFromDb(tourId);
      if (tourDataFromDb == null) {
         return null;
      }

      // keep the tour data
      updateTourInCache(tourDataFromDb);

      return tourDataFromDb;
   }

   /**
    * Opens the tour for the given tour id
    *
    * @param tourId
    */
   public void openTourInEditorArea(final Long tourId) {

      if (tourId == null) {
         return;
      }

      try {
         PlatformUI//
               .getWorkbench()
               .getActiveWorkbenchWindow()
               .getActivePage()
               .openEditor(new TourEditorInput(tourId), TourEditor.ID, true);

      } catch (final PartInitException e) {
         e.printStackTrace();
      }
   }

   public void removeAllToursFromCache() {

//      final ArrayList<TourData> modifiedTour = new ArrayList<TourData>(fTourDataCache.values());

      _tourDataCache.clear();

      // notify listener to reload the tours
      /*
       * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! this is not
       * working because the tour data editor does not reload the tour
       * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
       */
//      fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(modifiedTour));
   }

   public void removeTourEventListener(final ITourEventListener listener) {
      if (listener != null) {
         _tourEventListeners.remove(listener);
      }
   }

   /**
    * Removes {@link TourData} with the tourId from the cache.
    * <p>
    * When the tour is requested next time with {@link #getTourData(Long)}, is will be loaded from
    * the database.
    *
    * @param tourId
    */
   public void removeTourFromCache(final Long tourId) {

      _tourDataCache.remove(tourId);
   }

   public void removeTourSaveListener(final ITourSaveListener listener) {
      if (listener != null) {
         _tourSaveListeners.remove(listener);
      }
   }

   /**
    * Check tour in tour editor. When tour is modified and it contains a wrong tourData instance,
    * show an error, otherwise replace (silently) the tour data in the editor
    */
   private void replaceTourInTourEditor(final TourData tourDataForEditor) {

      if (tourDataForEditor == null || _tourDataEditorInstance == null) {
         return;
      }

      final TourData tourDataInEditor = _tourDataEditorInstance.getTourData();
      if (tourDataInEditor == null) {
         return;
      }

      final long tourIdInEditor = tourDataInEditor.getTourId().longValue();
      final long tourIdForEditor = tourDataForEditor.getTourId().longValue();
      if (tourIdInEditor != tourIdForEditor) {
         // tour editor contains another tour
         return;
      }

      /*
       * tour editor contains the same tour
       */

      if (tourDataInEditor == tourDataForEditor) {
         return;
      }

      /*
       * tour editor contains the wrong tour data instance
       */
      if (_tourDataEditorInstance.isDirty()) {

         MessageDialog.openError(Display.getCurrent().getActiveShell(),
               Messages.TourManager_Dialog_OutOfSyncError_Title,
               NLS.bind(Messages.TourManager_Dialog_OutOfSyncError_Message,
                     tourDataForEditor.toStringWithHash(),
                     tourDataInEditor.toStringWithHash()));

      } else {

         /*
          * silently replace tour data in editor
          */
         _tourDataEditorInstance.setTourData(tourDataForEditor);
      }
   }

   public void resetMapPositions() {

      for (final TourData tourData : _tourDataCache.getCache().values()) {
         tourData.mapCenterPositionLatitude = Double.MIN_VALUE;
         tourData.mapCenterPositionLongitude = Double.MIN_VALUE;
      }
   }

   /**
    * Before the application is shut down, the tour save listeners are called to save unsaved data.
    *
    * @return Returns <code>true</code> when the tours have been saved or false when it was canceled
    *         by the user
    */
   public boolean saveTours() {

      final Object[] allListeners = _tourSaveListeners.getListeners();
      for (final Object tourSaveListener : allListeners) {
         if (((ITourSaveListener) tourSaveListener).saveTour() == false) {
            return false;
         }
      }

      return true;
   }

   public void setActiveTourChart(final TourChart tourChart) {
      _activeTourChart = tourChart;
   }

   /**
    * @param yData
    * @param valueMultiplier
    *           Will be multiplied with the pref store value.
    * @param maxValueAdjustment
    *           Will be added/subtracted from the pref max value.
    * @param prefName_IsMinEnabled
    * @param prefName_IsMaxEnabled
    * @param prefName_MinValue
    * @param prefName_MaxValue
    */
   private void setVisibleForcedValues(final ChartDataYSerie yData,
                                       final float valueMultiplier,
                                       final double maxValueAdjustment,
                                       final String prefName_IsMinEnabled,
                                       final String prefName_IsMaxEnabled,
                                       final String prefName_MinValue,
                                       final String prefName_MaxValue) {

      if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED) == false) {

         // min/max is not enabled
         return;
      }

      if (_prefStore.getBoolean(prefName_IsMinEnabled)) {

         final int prefMinValue = _prefStore.getInt(prefName_MinValue);

         final float minValue = prefMinValue * valueMultiplier;
         final double forcedMinValue = minValue > 0 //
               ? minValue + maxValueAdjustment
               : minValue - maxValueAdjustment;

         yData.forceYAxisMinValue(forcedMinValue);
      }

      // set max value after min value, adjust max otherwise values above the max are painted
      if (_prefStore.getBoolean(prefName_IsMaxEnabled)) {

         final int prefMaxValue = _prefStore.getInt(prefName_MaxValue);

         final float maxValue = prefMaxValue * valueMultiplier;
         final double forcedMaxValue = maxValue > 0 //
               ? maxValue - maxValueAdjustment
               : maxValue + maxValueAdjustment;

         yData.forceYAxisMaxValue(forcedMaxValue);
      }
   }

   /**
    * @param yData
    * @param valueMultiplier
    *           Will be multiplied with the pref store value.
    * @param maxValueAdjustment
    *           Will be added/subtracted from the pref max value.
    * @param prefName_IsMinEnabled
    * @param prefName_IsMaxEnabled
    * @param prefName_MinValue
    * @param prefName_MaxValue
    */
   private void setVisibleForcedValues(final ChartDataYSerie yData,
                                       final int valueMultiplier,
                                       final double maxValueAdjustment,
                                       final String prefName_IsMinEnabled,
                                       final String prefName_IsMaxEnabled,
                                       final String prefName_MinValue,
                                       final String prefName_MaxValue) {

      if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED) == false) {

         // min/max is not enabled
         return;
      }

      if (_prefStore.getBoolean(prefName_IsMinEnabled)) {

         final int prefMinValue = _prefStore.getInt(prefName_MinValue);

         yData.forceYAxisMinValue(prefMinValue * valueMultiplier);
      }

      // set max value after min value, adjust max otherwise values above the max are painted
      if (_prefStore.getBoolean(prefName_IsMaxEnabled)) {

         final int prefMaxValue = _prefStore.getInt(prefName_MaxValue);

         final int maxValue = prefMaxValue * valueMultiplier;

         if (maxValueAdjustment == 0) {

            yData.forceYAxisMaxValue(maxValue);

         } else {

            yData.forceYAxisMaxValue(maxValue > 0

                  ? maxValue - maxValueAdjustment
                  : maxValue + maxValueAdjustment);
         }
      }
   }

   /**
    * Do custom actions when a tour in a table/tree/chart is double clicked
    *
    * @param tourProvider
    * @param tourDoubleClickState
    */
   public void tourDoubleClickAction(final ITourProvider tourProvider,
                                     final TourDoubleClickState tourDoubleClickState) {

      ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();
      if (selectedTours.isEmpty()) {

         if (tourProvider instanceof ITourProviderAll) {
            final ITourProviderAll allTourProvider = (ITourProviderAll) tourProvider;
            selectedTours = allTourProvider.getAllSelectedTours();

            if (selectedTours.isEmpty()) {
               return;
            }
         } else {
            return;
         }
      }

      final String action = _prefStore.getString(ITourbookPreferences.VIEW_DOUBLE_CLICK_ACTIONS);
      final TourData firstTour = selectedTours.get(0);

      String actionInfo = null;

      if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_ADJUST_ALTITUDE)) {

         if (tourDoubleClickState.canAdjustAltitude) {
            ActionOpenAdjustAltitudeDialog.doAction(tourProvider, false);
         } else {
            actionInfo = Messages.PrefPage_ViewActions_Label_DoubleClick_AdjustAltitude;
         }

      } else if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_EDIT_MARKER)) {

         if (tourDoubleClickState.canEditMarker) {
            ActionOpenMarkerDialog.doAction(tourProvider, true, null);
         } else {
            actionInfo = Messages.PrefPage_ViewActions_Label_DoubleClick_EditMarker;
         }

      } else if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_EDIT_TOUR)) {

         if (tourDoubleClickState.canEditTour) {
            ActionEditTour.doAction(tourProvider);
         } else {
            actionInfo = Messages.PrefPage_ViewActions_Label_DoubleClick_EditTour;
         }

      } else if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_OPEN_TOUR_IN_EDIT_AREA)) {

         if (tourDoubleClickState.canOpenTour && firstTour != null) {
            getInstance().openTourInEditorArea(firstTour.getTourId());
         } else {
            actionInfo = Messages.PrefPage_ViewActions_Label_DoubleClick_OpenTour;
         }

      } else if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_NONE_NO_WARNING)) {

         // do nothing and don't show a warning

      } else if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_NONE)) {

         // do nothing but show info that this can be customized

         MessageDialog.openInformation(
               Display.getCurrent().getActiveShell(),
               Messages.Dialog_DoubleClickAction_NoAction_Title,
               NLS.bind(Messages.Dialog_DoubleClickAction_NoAction_Message, actionInfo));

      } else {

         // default is quick edit

         if (tourDoubleClickState.canQuickEditTour) {
            ActionEditQuick.doAction(tourProvider);
         } else {
            actionInfo = Messages.PrefPage_ViewActions_Label_DoubleClick_QuickEdit;
         }
      }

      if (actionInfo != null) {
         MessageDialog.openInformation(
               Display.getCurrent().getActiveShell(),
               Messages.Dialog_DoubleClickAction_InvalidAction_Title,
               NLS.bind(Messages.Dialog_DoubleClickAction_InvalidAction_Message, actionInfo));
      }
   }

   public void updateTourInCache(final TourData tourData) {

      if (tourData == null) {
         return;
      }

      _tourDataCache.put(tourData.getTourId(), tourData);

      replaceTourInTourEditor(tourData);
   }
}
