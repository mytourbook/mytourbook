/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.referenceTour;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartTitleSegmentConfig;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ArrayListToArray;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoIconToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.geoCompare.GeoCompareData;
import net.tourbook.ui.views.geoCompare.GeoCompareEventId;
import net.tourbook.ui.views.geoCompare.GeoCompareManager;
import net.tourbook.ui.views.geoCompare.GeoComparedTour;
import net.tourbook.ui.views.geoCompare.IGeoCompareListener;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class ReferenceTimelineView extends ViewPart implements IGeoCompareListener {

   public static final String  ID                                = "net.tourbook.views.tourCatalog.yearStatisticView"; //$NON-NLS-1$

   private static final char   NL                                = UI.NEW_LINE;
   private static final char   FIELD_DELIMITER                   = UI.TAB;

   private static final String STATE_IS_SHOW_ALL_VALUES          = "STATE_IS_SHOW_ALL_VALUES";                         //$NON-NLS-1$
   private static final String STATE_IS_SYNC_MIN_MAX_VALUES      = "STATE_IS_SYNC_MIN_MAX_VALUES";                     //$NON-NLS-1$
   private static final String STATE_NUMBER_OF_VISIBLE_YEARS     = "STATE_NUMBER_OF_VISIBLE_YEARS";                    //$NON-NLS-1$
   static final String         STATE_RELATIVE_BAR_HEIGHT         = "STATE_RELATIVE_BAR_HEIGHT";                        //$NON-NLS-1$
   static final int            STATE_RELATIVE_BAR_HEIGHT_DEFAULT = 20;
   static final int            STATE_RELATIVE_BAR_HEIGHT_MIN     = 1;
   static final int            STATE_RELATIVE_BAR_HEIGHT_MAX     = 100;
   static final String         STATE_SHOW_ALTIMETER_AVG          = "STATE_SHOW_ALTIMETER_AVG";                         //$NON-NLS-1$
   static final boolean        STATE_SHOW_ALTIMETER_AVG_DEFAULT  = true;
   static final String         STATE_SHOW_PULSE_AVG              = "STATE_SHOW_PULSE_AVG";                             //$NON-NLS-1$
   static final boolean        STATE_SHOW_PULSE_AVG_DEFAULT      = true;
   static final String         STATE_SHOW_PULSE_AVG_MAX          = "STATE_SHOW_PULSE_AVG_MAX";                         //$NON-NLS-1$
   static final boolean        STATE_SHOW_PULSE_AVG_MAX_DEFAULT  = false;
   static final String         STATE_SHOW_SPEED_AVG              = "STATE_SHOW_SPEED_AVG";                             //$NON-NLS-1$
   static final boolean        STATE_SHOW_SPEED_AVG_DEFAULT      = true;
   static final String         STATE_SHOW_PACE_AVG               = "STATE_SHOW_PACE_AVG";                              //$NON-NLS-1$
   static final boolean        STATE_SHOW_PACE_AVG_DEFAULT       = true;

// SET_FORMATTING_OFF

   private static final String   PREF_PREFIX                         = "GRID_REF_TOUR_YEAR_STATISTIC__";                   //$NON-NLS-1$

   private static final String   GRID_IS_SHOW_VERTICAL_GRIDLINES     = PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES;
   private static final String   GRID_IS_SHOW_HORIZONTAL_GRIDLINES   = PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES;
   private static final String   GRID_VERTICAL_DISTANCE              = PREF_PREFIX + ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE;
   private static final String   GRID_HORIZONTAL_DISTANCE            = PREF_PREFIX + ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE;

// SET_FORMATTING_ON

   private static final IDialogSettings  _state            = TourbookPlugin.getState(ID);
   private static final IPreferenceStore _prefStore        = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore _prefStore_Common = CommonActivator.getPrefStore();

   private static final NumberFormat     _nf1              = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   private IPropertyChangeListener _prefChangeListener;
   private IPropertyChangeListener _prefChangeListener_Common;
   private ISelectionListener      _postSelectionListener;
   private PostSelectionProvider   _postSelectionProvider;

   private int                     _numVisibleYears;
   private int[]                   _allVisibleYears;
   private int[]                   _allNumberOfDaysInYear;

   /**
    * This is the last year (on the right side) which is displayed in the statistics
    */
   private int                     _lastVisibleYear    = TimeTools.now().getYear();

   /**
    * Contains all years which the user can select as start year in the combo box
    */
   private ArrayList<Integer>      _allSelectableYears = new ArrayList<>();

   /*
    * Statistic values for all visible years
    */
   private ArrayList<TVIRefTour_ComparedTour> _statValues_AllTours          = new ArrayList<>();
   private ArrayList<Integer>                 _statValues_DOYValues         = new ArrayList<>();

   private ArrayList<Float>                   _statValues_AvgAltimeter_High = new ArrayList<>();
   private ArrayList<Float>                   _statValues_AvgAltimeter_Low  = new ArrayList<>();
   private ArrayList<Float>                   _statValues_AvgPace_High      = new ArrayList<>();
   private ArrayList<Float>                   _statValues_AvgPace_Low       = new ArrayList<>();
   private ArrayList<Float>                   _statValues_AvgSpeed_High     = new ArrayList<>();
   private ArrayList<Float>                   _statValues_AvgSpeed_Low      = new ArrayList<>();
   private ArrayList<Float>                   _statValues_AvgPulse_Low      = new ArrayList<>();
   private ArrayList<Float>                   _statValues_AvgPulse_High     = new ArrayList<>();
   private ArrayList<Float>                   _statValues_MaxPulse          = new ArrayList<>();

   private int                                _barRelativeHeight;

   /**
    * Reference tour item for which the statistic is displayed. This statistic can display only
    * compared tours for ONE reference tour.
    */
   private TVIRefTour_RefTourItem             _currentRefItem;

   /**
    * Selection which is thrown by the year statistic
    */
   private StructuredSelection                _currentSelection;

   /**
    * When <code>null</code> then the elevation compare type icon in {@link #_iconCompareType} is
    * displayed
    */
   private GeoCompareData                     _currentGeoCompareData;

   private ITourEventListener                 _tourEventListener;

   private boolean                            _isGeoCompare;
   private boolean                            _isShowAllValues;
   private boolean                            _isSynchMinMaxValue;

   /**
    * Contains the index in {@link #_statValues_AllTours} for the currently selected tour.
    */
   private int                                _selectedTourIndex;

   private ActionCopyValuesIntoClipboard      _actionCopyValuesIntoClipboard;
   private ActionShowAllValues                _actionShowAllValues;
   private ActionSyncMinMaxValues             _actionSyncMinMaxValues;
   private ActionTimelineOptions              _actionYearStatOptions;

   private ReferenceTimeline_TourTooltip      _tourToolTip;
   private TourInfoIconToolTipProvider        _tourInfoToolTipProvider      = new TourInfoIconToolTipProvider();

   private PixelConverter                     _pc;

   /*
    * UI controls
    */
   private PageBook  _pageBook;
   private Composite _pageSelectRefTour;
   private Composite _pageYearChart;
   private Composite _headerContainer;

   private Chart     _yearChart;

   private Combo     _comboLastVisibleYear;

   private Label     _lblRefTourTitle;

   private Spinner   _spinnerNumberOfVisibleYears;

// SET_FORMATTING_OFF

   private Image     _viewImage_ElevationCompare        = TourbookPlugin.getThemedImageDescriptor(Images.TourCompare_Timeline_Elevation).createImage();
   private Image     _viewImage_GeoCompare              = TourbookPlugin.getThemedImageDescriptor(Images.TourCompare_Timeline_Geo).createImage();


// SET_FORMATTING_ON

   private class ActionCopyValuesIntoClipboard extends Action {

      private ActionCopyValuesIntoClipboard() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setToolTipText(Messages.Tour_StatisticValues_Action_CopyIntoClipboard_Tooltip);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Copy));
         setDisabledImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Copy_Disabled));
      }

      @Override
      public void run() {

         onAction_CopyIntoClipboard();
      }
   }

   private class ActionShowAllValues extends Action {

      public ActionShowAllValues() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         setToolTipText(Messages.Reference_Timeline_Action_ShowAllValues_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.RefTour_Statistic_Show_All_Values));
      }

      @Override
      public void run() {

         onAction_ShowAllValues(isChecked());
      }
   }

   private class ActionSyncMinMaxValues extends Action {

      public ActionSyncMinMaxValues() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         // Use the same scaling for different years, people or tour types,
         // this makes it easier to compare numbers
         setToolTipText(Messages.Reference_Timeline_Action_SyncMinMaxValues_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.SyncStatistics));
      }

      @Override
      public void runWithEvent(final Event event) {

         onAction_SyncMinMaxValues(event, isChecked());
      }

   }

   private class ActionTimelineOptions extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutReferenceTimelineOptions(ReferenceTimelineView.this, _pageBook, toolbar, PREF_PREFIX, _state);
      }
   }

   public ReferenceTimelineView() {}

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)

               || property.equals(GRID_HORIZONTAL_DISTANCE)
               || property.equals(GRID_VERTICAL_DISTANCE)
               || property.equals(GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
               || property.equals(GRID_IS_SHOW_VERTICAL_GRIDLINES)) {

            updateUI_YearChart();
         }
      };

      _prefChangeListener_Common = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

            // measurement system has changed -> recreate the chart

            _yearChart.dispose();
            createUI_30_Chart(_pageYearChart);

            _pageYearChart.layout();

            updateUI_YearChart();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   /**
    * Listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = (part, selection) -> {

         // prevent to listen to a selection which is originated by this year chart

         if (selection != _currentSelection) {
            onSelectionChanged(selection);
         }
      };

      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part,
                                 final TourEventId propertyId,
                                 final Object propertyData) {

            if (propertyId == TourEventId.COMPARE_TOUR_CHANGED
                  && propertyData instanceof TourPropertyCompareTourChanged) {

               final TourPropertyCompareTourChanged compareTourProperty = (TourPropertyCompareTourChanged) propertyData;

               if (compareTourProperty.isDataSaved) {
                  updateUI_YearChart();
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   /**
    * Set/restore min/max values.
    *
    * @param yData
    * @param minMaxValues
    */
   private void adjustMinMaxValues(final ChartDataYSerie yData, final float[] minMaxValues) {

      final float dataMinValue = (float) yData.getVisibleMinValue();
      final float dataMaxValue = (float) yData.getVisibleMaxValue();

      if (dataMinValue == 0 && dataMaxValue == 0) {

         // data are not valid, this prevents to set wrong min/max values

         return;
      }

      if (_isSynchMinMaxValue) {

         if (minMaxValues[0] == Float.MIN_VALUE
               || minMaxValues[1] == Float.MAX_VALUE) {

            // min/max values have not yet been set

            /*
             * Set the min value 10% below the computed so that the lowest value is not at the
             * bottom
             */
            yData.setVisibleMinValue(dataMinValue);
            yData.setVisibleMaxValue(dataMaxValue);

            minMaxValues[0] = dataMinValue;
            minMaxValues[1] = dataMaxValue;

         } else {

            // set min/max to previous values

            yData.setVisibleMinValue(minMaxValues[0]);
            yData.setVisibleMaxValue(minMaxValues[1]);
         }

      } else {

         // set min/max to the data min/max values

         yData.setVisibleMinValue(dataMinValue);
         yData.setVisibleMaxValue(dataMaxValue);
      }
   }

   private void createActions() {

      _actionCopyValuesIntoClipboard = new ActionCopyValuesIntoClipboard();
      _actionShowAllValues = new ActionShowAllValues();
      _actionSyncMinMaxValues = new ActionSyncMinMaxValues();
      _actionYearStatOptions = new ActionTimelineOptions();

      fillActionBars();
   }

   /**
    * Create segments for the chart
    */
   private ChartStatisticSegments createChartSegments() {

      final double[] segmentStart = new double[_numVisibleYears];
      final double[] segmentEnd = new double[_numVisibleYears];
      final String[] segmentTitle = new String[_numVisibleYears];

      final int firstYear = getFirstVisibleYear();
      int yearDaysSum = 0;

      // create segments for each year
      for (int yearDayIndex = 0; yearDayIndex < _allNumberOfDaysInYear.length; yearDayIndex++) {

         final int yearDays = _allNumberOfDaysInYear[yearDayIndex];

         segmentStart[yearDayIndex] = yearDaysSum;
         segmentEnd[yearDayIndex] = yearDaysSum + yearDays - 1;
         segmentTitle[yearDayIndex] = Integer.toString(firstYear + yearDayIndex);

         yearDaysSum += yearDays;
      }

      final ChartStatisticSegments chartSegments = new ChartStatisticSegments();
      chartSegments.segmentStartValue = segmentStart;
      chartSegments.segmentEndValue = segmentEnd;
      chartSegments.segmentTitle = segmentTitle;

      chartSegments.years = _allVisibleYears;
      chartSegments.yearDays = _allNumberOfDaysInYear;
      chartSegments.allValues = yearDaysSum;

      return chartSegments;
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);

      createUI(parent);

      addSelectionListener();
      addTourEventListener();
      addPrefListener();
      GeoCompareManager.addGeoCompareEventListener(this);

      createActions();

      // set selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      _pageBook.showPage(_pageSelectRefTour);

      restoreState();

      // restore selection
      onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

      parent.getDisplay().asyncExec(() -> enableControls());
   }

   /**
    * All items from the ref tour are from type {@link TVIRefTour_YearItem}
    *
    * @param firstVisibleYear
    * @param isShowLatestYear
    * @param isShowLatestYear
    */
   private void createStatisticData_10_WithYearCategories(int firstVisibleYear, final boolean isShowLatestYear) {

      final Object[] allItems = _currentRefItem.getFetchedChildrenAsArray();

      if (allItems != null
            && allItems.length > 0
            && allItems[0] instanceof TVIRefTour_YearItem
            && allItems[allItems.length - 1] instanceof TVIRefTour_YearItem) {

         final int firstYear = ((TVIRefTour_YearItem) allItems[0]).year;
         final int lastYear = ((TVIRefTour_YearItem) allItems[allItems.length - 1]).year;

         if (_isShowAllValues) {

            firstVisibleYear = firstYear;
            _lastVisibleYear = lastYear;

            _numVisibleYears = lastYear - firstYear + 1;
            _spinnerNumberOfVisibleYears.setSelection(_numVisibleYears);

            // update year data
            setYearData();

         } else if (isShowLatestYear) {

            // get the last year when it's forced

            /*
             * Use current years when the new items are in the current range, otherwise adjust the
             * years
             */
            if (lastYear <= _lastVisibleYear && firstYear >= _lastVisibleYear - _numVisibleYears) {

               // new years are within the current year range

            } else {

               // overwrite last year
               _lastVisibleYear = lastYear;
            }
         }
      }

      /*
       * Create data for all years
       */
      for (final Object yearItemObj : allItems) {

         if (yearItemObj instanceof TVIRefTour_YearItem) {

            final TVIRefTour_YearItem yearItem = (TVIRefTour_YearItem) yearItemObj;

            // check if the year can be displayed

            final int yearItemYear = yearItem.year;

            if (yearItemYear >= firstVisibleYear && yearItemYear <= _lastVisibleYear) {

               // loop: all tours
               final Object[] allTourItems = yearItem.getFetchedChildrenAsArray();

               for (final Object tourItemObj : allTourItems) {

                  if (tourItemObj instanceof TVIRefTour_ComparedTour) {

                     final TVIRefTour_ComparedTour tourItem = (TVIRefTour_ComparedTour) tourItemObj;

                     createStatisticData_50_OneTour(tourItem);
                  }
               }
            }
         }
      }
   }

   /**
    * All items from the ref tour are from type {@link TVIRefTour_ComparedTour}
    *
    * @param firstVisibleYear
    * @param isShowLatestYear
    */
   private void createStatisticData_20_WithoutYearCategories(int firstVisibleYear) {

      final Object[] allItems = _currentRefItem.getFetchedChildrenAsArray();

      if (_isShowAllValues
            && allItems != null
            && allItems.length > 0
            && allItems[0] instanceof TVIRefTour_ComparedTour
            && allItems[allItems.length - 1] instanceof TVIRefTour_ComparedTour) {

         final int firstYear = ((TVIRefTour_ComparedTour) allItems[0]).year;
         final int lastYear = ((TVIRefTour_ComparedTour) allItems[allItems.length - 1]).year;

         firstVisibleYear = firstYear;
         _lastVisibleYear = lastYear;

         _numVisibleYears = lastYear - firstYear + 1;
         _spinnerNumberOfVisibleYears.setSelection(_numVisibleYears);

         // update year data
         setYearData();
      }

      // loop: all tours
      for (final Object item : allItems) {

         if (item instanceof TVIRefTour_ComparedTour) {

            final TVIRefTour_ComparedTour tourItem = (TVIRefTour_ComparedTour) item;

            final int tourYear = tourItem.year;

            if (tourYear >= firstVisibleYear && tourYear <= _lastVisibleYear) {

               createStatisticData_50_OneTour(tourItem);
            }
         }
      }
   }

   private void createStatisticData_30_FromGeoData(int firstVisibleYear,
                                                   final GeoCompareData geoCompareData) {

      final List<GeoComparedTour> allFilteredItems = geoCompareData.allGeoComparedTours_Filtered;

      final List<GeoComparedTour> allItems = allFilteredItems != null
            ? allFilteredItems
            : geoCompareData.allGeoComparedTours;

      final ArrayList<GeoComparedTour> allSortedTours = new ArrayList<>();
      allSortedTours.addAll(allItems);

      // sort with default comparator GeoComparedTour.compareTo() which is sorting by date/time
      allSortedTours.sort(null);

      if (_isShowAllValues
            && allSortedTours != null
            && allSortedTours.size() > 0) {

         final int lastIndex = allSortedTours.size() - 1;

         final int firstYear = allSortedTours.get(0).tourYear;
         final int lastYear = allSortedTours.get(lastIndex).tourYear;

         firstVisibleYear = firstYear;
         _lastVisibleYear = lastYear;

         _numVisibleYears = lastYear - firstYear + 1;
         _spinnerNumberOfVisibleYears.setSelection(_numVisibleYears);

         // update year data
         setYearData();
      }

      for (final GeoComparedTour geoComparedTour : allSortedTours) {

         if (geoComparedTour.isGeoCompareDone == false) {

            // this tour is not yet compared in the background

            continue;
         }

         final int tourYear = geoComparedTour.tourYear;

         if (tourYear >= firstVisibleYear && tourYear <= _lastVisibleYear) {

            final TVIRefTour_ComparedTour comparedTourItem = new TVIRefTour_ComparedTour(null);

// SET_FORMATTING_OFF

            comparedTourItem.geoCompareTour     = geoComparedTour;

            comparedTourItem.tourTitle          = geoComparedTour.tourTitle;
            comparedTourItem.tourDate           = geoComparedTour.tourStartTime.toLocalDate();

            comparedTourItem.avgSpeed           = geoComparedTour.avgSpeed;
            comparedTourItem.avgPace            = geoComparedTour.avgPace;

            comparedTourItem.avgPulse           = geoComparedTour.avgPulse;
            comparedTourItem.maxPulse           = geoComparedTour.maxPulse;

            comparedTourItem.avgAltimeter       = geoComparedTour.avgAltimeter;

// SET_FORMATTING_ON

            createStatisticData_50_OneTour(comparedTourItem);
         }
      }
   }

   private void createStatisticData_50_OneTour(final TVIRefTour_ComparedTour comparedTourItem) {

// SET_FORMATTING_OFF

      final LocalDate tourDate   = comparedTourItem.tourDate;
      final float avgAltimeter   = comparedTourItem.getAvgAltimeter();
      final float avgPace        = comparedTourItem.getTourPace() * UI.UNIT_VALUE_DISTANCE;
      final float avgPulse       = comparedTourItem.getAvgPulse();
      final float avgSpeed       = comparedTourItem.getTourSpeed() / UI.UNIT_VALUE_DISTANCE;

      _statValues_AllTours          .add(comparedTourItem);

      _statValues_DOYValues         .add(getYearDOYs(tourDate.getYear()) + tourDate.getDayOfYear() - 1);

      _statValues_AvgSpeed_Low      .add(avgSpeed - avgSpeed / 100 * _barRelativeHeight);
      _statValues_AvgSpeed_High     .add(avgSpeed);
      _statValues_AvgPace_Low       .add(avgPace - avgPace / 100 * _barRelativeHeight);
      _statValues_AvgPace_High      .add(avgPace);

      _statValues_AvgPulse_Low      .add(avgPulse - avgPulse / 100 * _barRelativeHeight);
      _statValues_AvgPulse_High     .add(avgPulse);
      _statValues_MaxPulse          .add(comparedTourItem.getMaxPulse());

      _statValues_AvgAltimeter_Low  .add(avgAltimeter - avgAltimeter / 100 * _barRelativeHeight);
      _statValues_AvgAltimeter_High .add(avgAltimeter);

// SET_FORMATTING_ON
   }

   /**
    * @param toolTipProvider
    * @param parent
    * @param serieIndex
    * @param valueIndex
    * @param frequencyStatistic
    */
   private void createToolTipUI(final IToolTipProvider toolTipProvider,
                                final Composite parent,
                                int valueIndex) {

      if (valueIndex >= _statValues_DOYValues.size()) {
         valueIndex -= _statValues_DOYValues.size();
      }

      if (_statValues_DOYValues == null || valueIndex >= _statValues_DOYValues.size()) {
         return;
      }

      /*
       * Get day/month/year
       */
      final int firstYear = getFirstVisibleYear();
      final int tourDOY = _statValues_DOYValues.get(valueIndex);

      final ZonedDateTime tourDate = ZonedDateTime
            .of(firstYear, 1, 1, 0, 0, 0, 1, TimeTools.getDefaultTimeZone())
            .plusDays(tourDOY);

      final String title = tourDate.format(TimeTools.Formatter_Date_F);

      new ReferenceTimeline_TourTooltipUI().createContentArea(

            parent,
            toolTipProvider,

            title,

            _statValues_AvgAltimeter_High.get(valueIndex),
            _statValues_AvgPulse_High.get(valueIndex),
            _statValues_MaxPulse.get(valueIndex),

            _statValues_AvgSpeed_High.get(valueIndex),
            _statValues_AvgPace_High.get(valueIndex)

      );
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageSelectRefTour = UI.createUI_PageNoData(_pageBook, Messages.Reference_Timeline_Page_SelectReferenceTour);

      _pageYearChart = createUI_10_PageYearChart(_pageBook);
   }

   private Composite createUI_10_PageYearChart(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         createUI_20_Header(container);
         createUI_30_Chart(container);
      }

      return container;
   }

   /**
    * Header
    */
   private void createUI_20_Header(final Composite parent) {

      _headerContainer = new Composite(parent, SWT.NONE);
      _headerContainer.setBackground(ThemeUtil.getDefaultBackgroundColor_Table());
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.FILL, SWT.FILL)
            .applyTo(_headerContainer);
      GridLayoutFactory.fillDefaults()
            .numColumns(3)
            .margins(3, 3)
            .applyTo(_headerContainer);
//      _headerContainer.setBackground(UI.SYS_COLOR_GREEN);
      {
         {
            /*
             * Ref tour title
             */
            _lblRefTourTitle = new Label(_headerContainer, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblRefTourTitle);
//            _lblRefTourTitle.setBackground(UI.SYS_COLOR_RED);
         }
         {
            /*
             * Last visible year
             */
            _comboLastVisibleYear = new Combo(_headerContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboLastVisibleYear.setToolTipText(Messages.Reference_Timeline_Combo_LastYears_Tooltip);
            _comboLastVisibleYear.setVisibleItemCount(50);

            _comboLastVisibleYear.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> onSelect_LastVisibleYear()));

            _comboLastVisibleYear.addTraverseListener(traverseEvent -> {
               if (traverseEvent.detail == SWT.TRAVERSE_RETURN) {
                  onSelect_LastVisibleYear();
               }
            });

            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .hint(_pc.convertWidthInCharsToPixels(UI.IS_OSX ? 12 : UI.IS_LINUX ? 12 : 5), SWT.DEFAULT)
                  .applyTo(_comboLastVisibleYear);
         }
         {
            /*
             * Number of visible years
             */
            _spinnerNumberOfVisibleYears = new Spinner(_headerContainer, SWT.BORDER);
            _spinnerNumberOfVisibleYears.setMinimum(1);
            _spinnerNumberOfVisibleYears.setMaximum(100);
            _spinnerNumberOfVisibleYears.setIncrement(1);
            _spinnerNumberOfVisibleYears.setPageIncrement(5);
            _spinnerNumberOfVisibleYears.setToolTipText(Messages.Reference_Timeline_Combo_NumberOfYears_Tooltip);

            _spinnerNumberOfVisibleYears.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> onSelect_NumberOfVisibleYears()));

            _spinnerNumberOfVisibleYears.addMouseWheelListener(mouseEvent -> {

               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 1);
               onSelect_NumberOfVisibleYears();
            });
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerNumberOfVisibleYears);
         }
      }
   }

   /**
    * Year chart
    */
   private void createUI_30_Chart(final Composite parent) {

      _yearChart = new Chart(parent, SWT.NONE);
      _yearChart.addBarSelectionListener((serieIndex, valueIndex) -> onSelect_ComparedTour(valueIndex));
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_yearChart);

      // set tour info icon into the left axis
      _tourToolTip = new ReferenceTimeline_TourTooltip(_yearChart.getToolTipControl());
      _tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);
      _tourToolTip.addHideListener(event -> {

         // hide hovered image
         _yearChart.getToolTipControl().afterHideToolTip();
      });

      _yearChart.setTourInfoIconToolTipProvider(_tourInfoToolTipProvider);
      _tourInfoToolTipProvider.setActionsEnabled(true);

   }

   @Override
   public void dispose() {

      UI.disposeResource(_viewImage_ElevationCompare);
      UI.disposeResource(_viewImage_GeoCompare);

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      TourManager.getInstance().removeTourEventListener(_tourEventListener);
      GeoCompareManager.removeGeoCompareListener(this);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      super.dispose();
   }

   private void enableControls() {

      final boolean canSelectYears = _isShowAllValues == false;

      _comboLastVisibleYear.setEnabled(canSelectYears);
      _spinnerNumberOfVisibleYears.setEnabled(canSelectYears);
   }

   private void fillActionBars() {

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionShowAllValues);
      tbm.add(_actionSyncMinMaxValues);
      tbm.add(_actionCopyValuesIntoClipboard);
      tbm.add(_actionYearStatOptions);

      tbm.update(true);
   }

   @Override
   public void geoCompareEvent(final IWorkbenchPart part, final GeoCompareEventId eventId, final Object eventData) {

      switch (eventId) {

      case TOUR_IS_GEO_COMPARED:

         if (eventData instanceof GeoCompareData) {

            final GeoCompareData geoCompareData = (GeoCompareData) eventData;

            // update year statistic with provided geo compare data
            updateUI_YearChart(geoCompareData, false);
         }

         break;

      case SET_COMPARING_ON:
         break;

      case SET_COMPARING_OFF:

         _currentGeoCompareData = null;
         updateUI_YearChart();

         break;
      default:
         break;
      }

   }

   private int getFirstVisibleYear() {

      return _lastVisibleYear - _numVisibleYears + 1;
   }

   /**
    * @param currentYear
    * @param numberOfYears
    * @return Returns the number of days between {@link #fLastYear} and currentYear
    */
   int getYearDOYs(final int selectedYear) {

      int yearDOYs = 0;
      int yearIndex = 0;

      final int firstVisibleYear = getFirstVisibleYear();

      for (int currentYear = firstVisibleYear; currentYear < selectedYear; currentYear++) {

         if (currentYear == selectedYear) {
            return yearDOYs;
         }

         yearDOYs += _allNumberOfDaysInYear[yearIndex];

         yearIndex++;
      }

      return yearDOYs;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   public TVIRefTour_ComparedTour navigateTour(final boolean isNextTour) {

      final int numTours = _statValues_AllTours.size();

      if (numTours < 2) {
         return null;
      }

      int navIndex;
      if (isNextTour) {

         // get nexttour

         if (_selectedTourIndex >= numTours - 1) {

            navIndex = 0;

         } else {

            navIndex = _selectedTourIndex + 1;
         }

      } else {

         // get previous tour

         if (_selectedTourIndex <= 0) {

            navIndex = numTours - 1;

         } else {

            navIndex = _selectedTourIndex - 1;
         }
      }

      return _statValues_AllTours.get(navIndex);
   }

   private void onAction_CopyIntoClipboard() {

      final StringBuilder sb = new StringBuilder();

      /*
       * Data Title
       */
      sb.append(_isGeoCompare
            ? Messages.Tour_Compare_ViewName_GeoCompareTimeline
            : Messages.Tour_Compare_ViewName_ElevationCompareTimeline);

      sb.append(NL);
      sb.append(NL);

      /*
       * Header
       */
      sb.append("Date"); //$NON-NLS-1$
      sb.append(FIELD_DELIMITER);

      sb.append("Avg Speed"); //$NON-NLS-1$
      sb.append(FIELD_DELIMITER);

      sb.append("Avg Pace"); //$NON-NLS-1$
      sb.append(FIELD_DELIMITER);

      sb.append("Avg Altimeter"); //$NON-NLS-1$
      sb.append(FIELD_DELIMITER);

      sb.append("Avg Heart Rate"); //$NON-NLS-1$
      sb.append(FIELD_DELIMITER);

      sb.append("Max Heart Rate"); //$NON-NLS-1$
      sb.append(FIELD_DELIMITER);

      sb.append("Tour Title"); //$NON-NLS-1$
      sb.append(FIELD_DELIMITER);

      sb.append(NL);

      /*
       * Data
       */
      for (final TVIRefTour_ComparedTour comparedTour : _statValues_AllTours) {

         sb.append(TimeTools.Formatter_Date_S.format(comparedTour.tourDate));
         sb.append(FIELD_DELIMITER);

         sb.append(comparedTour.avgSpeed);
         sb.append(FIELD_DELIMITER);

         sb.append(comparedTour.avgPace);
         sb.append(FIELD_DELIMITER);

         sb.append(comparedTour.avgAltimeter);
         sb.append(FIELD_DELIMITER);

         sb.append(comparedTour.avgPulse);
         sb.append(FIELD_DELIMITER);

         sb.append(comparedTour.maxPulse);
         sb.append(FIELD_DELIMITER);

         sb.append(comparedTour.tourTitle);
         sb.append(FIELD_DELIMITER);

         sb.append(NL);
      }

      UI.copyTextIntoClipboard(sb.toString(), Messages.App_Action_CopyDataIntoClipboard_CopyIsDone);
   }

   private void onAction_ShowAllValues(final boolean isShowAllValues) {

      _isShowAllValues = isShowAllValues;

      updateUI_YearChart();

      enableControls();
   }

   private void onAction_SyncMinMaxValues(final Event event, final boolean isSyncMinMaxValue) {

      if (UI.isCtrlKey(event)) {
         _currentRefItem.resetMinMaxValues();
      }

      _isSynchMinMaxValue = isSyncMinMaxValue;

      updateUI_YearChart();
   }

   /**
    * A compared tour is selected in the chart
    *
    * @param valueIndex
    */
   private void onSelect_ComparedTour(final int valueIndex) {

      if (_statValues_AllTours.isEmpty()) {

         _tourInfoToolTipProvider.setTourId(-1);
         return;
      }

      // ensure list size
      _selectedTourIndex = Math.min(valueIndex, _statValues_AllTours.size() - 1);

      // select tour in the tour viewer and show tour in compared tour char
      final TVIRefTour_ComparedTour comparedTour = _statValues_AllTours.get(_selectedTourIndex);
      _currentSelection = new StructuredSelection(comparedTour);
      _postSelectionProvider.setSelection(_currentSelection);

      final long tourId = comparedTour.geoCompareTour != null
            ? comparedTour.geoCompareTour.tourId
            : comparedTour.getTourId();

      _tourInfoToolTipProvider.setTourId(tourId);
   }

   void onSelect_LastVisibleYear() {

      // get last visible year
      _lastVisibleYear = _allSelectableYears.get(_comboLastVisibleYear.getSelectionIndex());

      // update year data
      setYearData();

      updateUI_YearChart_WithCurrentGeoData();
   }

   /**
    * Update statistic by setting number of visible years
    */
   void onSelect_NumberOfVisibleYears() {

      // get selected tour
      long selectedTourId = 0;

      if (_statValues_AllTours.isEmpty()) {
         selectedTourId = -1;

      } else {

         final int selectedTourIndex = Math.min(_selectedTourIndex, _statValues_AllTours.size() - 1);
         selectedTourId = _statValues_AllTours.get(selectedTourIndex).getTourId();
      }

      _numVisibleYears = _spinnerNumberOfVisibleYears.getSelection();

      setYearData();

      updateUI_YearChart_WithCurrentGeoData();

      // reselect last selected tour
      selectTourInYearChart(selectedTourId);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (selection instanceof SelectionReferenceTourView) {

         final SelectionReferenceTourView refTourItem = (SelectionReferenceTourView) selection;

         final TVIRefTour_RefTourItem refItem = refTourItem.getRefItem();
         if (refItem != null) {

            // reference tour is selected

            _currentRefItem = refItem;

            if (refTourItem.isFromElevationCompare()) {
               _currentGeoCompareData = null;
            }

            updateUI_YearChart(true);

         } else {

            // show statistic for a specific year

            final TVIRefTour_YearItem yearItem = refTourItem.getYearItem();
            if (yearItem != null) {

               _currentRefItem = yearItem.getRefItem();

               // overwrite last year
               _lastVisibleYear = yearItem.year;

               // update year data
               setYearData();

               updateUI_YearChart();
            }
         }

         // select tour in the statistic
         final Long compTourId = refTourItem.getCompTourId();
         if (compTourId != null) {

            selectTourInYearChart(compTourId);

         } else {

            // select first tour for the youngest year
            int yearIndex = 0;
            for (final TVIRefTour_ComparedTour tourItem : _statValues_AllTours) {

               if (tourItem.tourDate.getYear() == _lastVisibleYear) {
                  break;
               }

               yearIndex++;
            }

            final int allTourSize = _statValues_AllTours.size();

            if (allTourSize > 0 && yearIndex < allTourSize) {
               selectTourInYearChart(_statValues_AllTours.get(yearIndex).getTourId());
            }
         }

      } else if (selection instanceof StructuredSelection) {

         final StructuredSelection structuredSelection = (StructuredSelection) selection;

         if (structuredSelection.size() > 0) {

            final Object firstElement = structuredSelection.getFirstElement();

            if (firstElement instanceof TVIRefTour_ComparedTour) {

               final TVIRefTour_ComparedTour compareItem = (TVIRefTour_ComparedTour) firstElement;

               boolean isUpdateYearChart = false;

               if (compareItem.geoCompareTour == null && _currentGeoCompareData != null) {

                  _currentGeoCompareData = null;

                  isUpdateYearChart = true;
               }

               // get year item
               final TreeViewerItem compareParentItem = compareItem.getParentItem();
               if (compareParentItem instanceof TVIRefTour_YearItem) {

                  final TVIRefTour_YearItem yearItem = (TVIRefTour_YearItem) compareParentItem;

                  // get ref tour item
                  final TreeViewerItem yearParentItem = yearItem.getParentItem();
                  if (yearParentItem instanceof TVIRefTour_RefTourItem) {

                     final TVIRefTour_RefTourItem refTourItem = (TVIRefTour_RefTourItem) yearParentItem;

                     final long refId = refTourItem.refId;

                     if (_currentRefItem == null) {

                        // create new ref item for the ref tour
                        _currentRefItem = ElevationCompareManager.createCatalogRefItem(refId);

                        isUpdateYearChart = true;

                     } else {

                        if (_currentRefItem.refId != refId) {

                           // the current statistic do not show the ref tour for the compared tour
                           // -> show also the ref tour

                           _currentRefItem = refTourItem;

                           isUpdateYearChart = true;
                        }
                     }
                  }
               }

               if (isUpdateYearChart) {

                  updateUI_YearChart();
               }

               // select tour in the year chart
               final Long tourId = compareItem.getTourId();
               if (tourId != null) {
                  selectTourInYearChart(tourId);
               }

            } else if (firstElement instanceof GeoComparedTour) {

               final GeoComparedTour geoComparedTour = (GeoComparedTour) firstElement;

               // update year statistic with provided geo compare data
               updateUI_YearChart(geoComparedTour.geoCompareData, false);

               selectTourInYearChart(geoComparedTour);

            } else if (firstElement instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour compareResult = (TVIElevationCompareResult_ComparedTour) firstElement;

               final Long tourId = compareResult.getTourId();
               if (tourId != null) {

                  final RefTourItem refTour = compareResult.refTour;
                  final long refId = refTour.refId;

                  if (_currentRefItem == null || _currentRefItem.refId != refId

                  // ensure that geo compare is not displayed
                        || _currentGeoCompareData != null) {

                     // the current statistic do not show the ref tour for the compared tour
                     // -> first show the ref tour

                     // create new ref item for the ref tour
                     _currentRefItem = ElevationCompareManager.createCatalogRefItem(refId);

                     _currentGeoCompareData = null;

                     updateUI_YearChart();
                  }

                  selectTourInYearChart(tourId);
               }
            }
         }

      } else if (selection instanceof SelectionRemovedComparedTours) {

         final SelectionRemovedComparedTours removedCompTours = (SelectionRemovedComparedTours) selection;

         final ArrayList<ElevationCompareResult> allCompareResults = removedCompTours.removedComparedTours;

         if (allCompareResults.size() > 0) {

            // create new ref item which do not contain the removed compared tours

            final ElevationCompareResult compareResult = allCompareResults.get(0);
            final long refTourId = compareResult.refTourId;

            _currentRefItem = ElevationCompareManager.createCatalogRefItem(refTourId);

            updateUI_YearChart();
         }

      } else if (selection instanceof SelectionPersistedCompareResults) {

         final SelectionPersistedCompareResults savedCompTours = (SelectionPersistedCompareResults) selection;

         final ArrayList<TVIElevationCompareResult_ComparedTour> persistedCompareResults = savedCompTours.persistedCompareResults;

         if (persistedCompareResults.size() > 0) {

            // create new ref item which contains the newly persisted compared tours

            final TVIElevationCompareResult_ComparedTour tviCompareResultComparedTour = persistedCompareResults.get(0);
            final RefTourItem refTour = tviCompareResultComparedTour.refTour;
            final long savedRefId = refTour.refId;

            _currentRefItem = ElevationCompareManager.createCatalogRefItem(savedRefId);

            updateUI_YearChart();
         }
      }
   }

   private void restoreState() {

      _isShowAllValues = Util.getStateBoolean(_state, STATE_IS_SHOW_ALL_VALUES, false);
      _isSynchMinMaxValue = Util.getStateBoolean(_state, STATE_IS_SYNC_MIN_MAX_VALUES, false);

      _actionShowAllValues.setChecked(_isShowAllValues);
      _actionSyncMinMaxValues.setChecked(_isSynchMinMaxValue);

      final int numVisibleYears = Util.getStateInt(_state, ReferenceTimelineView.STATE_NUMBER_OF_VISIBLE_YEARS, 3, 1, 100);
      _numVisibleYears = numVisibleYears;
      _spinnerNumberOfVisibleYears.setSelection(numVisibleYears);

      setYearData();
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_IS_SHOW_ALL_VALUES, _isShowAllValues);
      _state.put(STATE_IS_SYNC_MIN_MAX_VALUES, _isSynchMinMaxValue);
      _state.put(STATE_NUMBER_OF_VISIBLE_YEARS, _numVisibleYears);
   }

   private void selectTourInYearChart(final GeoComparedTour geoComparedTour) {

      if (_statValues_AllTours.isEmpty()) {

         _tourInfoToolTipProvider.setTourId(-1);

         return;
      }

      final int numTours = _statValues_AllTours.size();
      final boolean[] selectedTours = new boolean[numTours];

      for (int tourIndex = 0; tourIndex < numTours; tourIndex++) {

         final TVIRefTour_ComparedTour comparedItem = _statValues_AllTours.get(tourIndex);

         if (comparedItem.geoCompareTour == geoComparedTour) {

            selectedTours[tourIndex] = true;
         }
      }

      _yearChart.setSelectedBars(selectedTours);
   }

   /**
    * Select tour in the year chart and update selectable bars
    *
    * @param tourIdToSelect
    *           tour id which should be selected
    */
   private void selectTourInYearChart(final long tourIdToSelect) {

      if (_statValues_AllTours.isEmpty()) {

         _tourInfoToolTipProvider.setTourId(-1);

         return;
      }

      final int numTours = _statValues_AllTours.size();
      final boolean[] selectedTours = new boolean[numTours];
      boolean isTourSelected = false;

      for (int tourIndex = 0; tourIndex < numTours; tourIndex++) {

         final TVIRefTour_ComparedTour comparedItem = _statValues_AllTours.get(tourIndex);

         if (comparedItem.getTourId() == tourIdToSelect) {

            selectedTours[tourIndex] = true;
            isTourSelected = true;
         }
      }

      if (isTourSelected == false && selectedTours.length > 0) {

         // a tour is not selected, select first tour

// disable it can be confusing when the wrong tour is selected
//         selectedTours[0] = true;
      }

      _yearChart.setSelectedBars(selectedTours);
   }

   @Override
   public void setFocus() {

      _yearChart.setFocus();
   }

   /**
    * Get data for each displayed year
    */
   private void setYearData() {

      _allVisibleYears = new int[_numVisibleYears];
      _allNumberOfDaysInYear = new int[_numVisibleYears];

      final int firstYear = getFirstVisibleYear();

      int yearIndex = 0;
      for (int currentYear = firstYear; currentYear <= _lastVisibleYear; currentYear++) {

         _allVisibleYears[yearIndex] = currentYear;
         _allNumberOfDaysInYear[yearIndex] = TimeTools.getNumberOfDaysWithYear(currentYear);

         yearIndex++;
      }
   }

   private void updateUI_YearChart() {

      updateUI_YearChart(false);
   }

   private void updateUI_YearChart(final boolean isShowLatestYear) {

      GeoCompareData geoCompareData = null;

      // use current geo compare data when is has the same ref id
      if (_currentRefItem != null
            && _currentGeoCompareData != null

            && _currentRefItem.refId == _currentGeoCompareData.refTour_RefId) {

         geoCompareData = _currentGeoCompareData;
      }

      updateUI_YearChart(geoCompareData, isShowLatestYear);
   }

   /**
    * Show statistic for several years
    *
    * @param geoCompareData
    * @param isShowLatestYear
    *           Shows the latest year and the years before
    */
   private void updateUI_YearChart(final GeoCompareData geoCompareData,
                                   final boolean isShowLatestYear) {

      if (geoCompareData != null && _currentRefItem == null) {

         _currentRefItem = new TVIRefTour_RefTourItem();

         _currentRefItem.numTours = geoCompareData.allGeoComparedTours_Filtered.size();
         _currentRefItem.label = geoCompareData.comparedTour_TourTitle;
      }

      if (_currentRefItem == null) {

         _pageBook.showPage(_pageSelectRefTour);

         return;
      }

      _pageBook.showPage(_pageYearChart);

      /*
       * Reset statistic values
       */
      _statValues_AllTours.clear();
      _statValues_DOYValues.clear();

      _statValues_AvgAltimeter_Low.clear();
      _statValues_AvgAltimeter_High.clear();
      _statValues_AvgPulse_Low.clear();
      _statValues_AvgPulse_High.clear();
      _statValues_MaxPulse.clear();

      _statValues_AvgSpeed_Low.clear();
      _statValues_AvgSpeed_High.clear();
      _statValues_AvgPace_Low.clear();
      _statValues_AvgPace_High.clear();

      int firstVisibleYear = getFirstVisibleYear();

      _barRelativeHeight = Util.getStateInt(_state,
            ReferenceTimelineView.STATE_RELATIVE_BAR_HEIGHT,
            ReferenceTimelineView.STATE_RELATIVE_BAR_HEIGHT_DEFAULT,
            ReferenceTimelineView.STATE_RELATIVE_BAR_HEIGHT_MIN,
            ReferenceTimelineView.STATE_RELATIVE_BAR_HEIGHT_MAX);

      // keep/remove current geo compare data
      _currentGeoCompareData = geoCompareData;

      if (geoCompareData != null) {

         createStatisticData_30_FromGeoData(firstVisibleYear, geoCompareData);

      } else if (ElevationCompareManager.getReferenceTour_ViewLayout() == ElevationCompareManager.REF_TOUR_VIEW_LAYOUT_WITH_YEAR_CATEGORIES) {

         // compared tours are displayed with year categories

         createStatisticData_10_WithYearCategories(firstVisibleYear, isShowLatestYear);

      } else {

         // compared tours are displayed without year categories

         createStatisticData_20_WithoutYearCategories(firstVisibleYear);
      }

      // first visible year could be changed when all values are displayed
      firstVisibleYear = getFirstVisibleYear();

      final ChartDataModel chartModel = new ChartDataModel(ChartType.BAR);

      final ChartDataXSerie xData = new ChartDataXSerie(ArrayListToArray.integerToDouble(_statValues_DOYValues));
      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_DAY);
      xData.setChartSegments(createChartSegments());
      chartModel.setXData(xData);

      /**
       * Speed
       */
      if (Util.getStateBoolean(_state, STATE_SHOW_SPEED_AVG, STATE_SHOW_SPEED_AVG_DEFAULT)) {

         // set the bar low/high data
         final ChartDataYSerie yDataSpeed = new ChartDataYSerie(
               ChartType.BAR,
               ArrayListToArray.toFloat(_statValues_AvgSpeed_Low),
               ArrayListToArray.toFloat(_statValues_AvgSpeed_High),
               true);

         final float[] minMaxValues = _currentRefItem.avgSpeed_MinMax;
         yDataSpeed.setSliderMinMaxValue(minMaxValues);
         adjustMinMaxValues(yDataSpeed, minMaxValues);

         TourManager.setBarColors(yDataSpeed, GraphColorManager.PREF_GRAPH_SPEED);
         TourManager.setGraphColors(yDataSpeed, GraphColorManager.PREF_GRAPH_SPEED);

         yDataSpeed.setYTitle(OtherMessages.GRAPH_LABEL_SPEED);
         yDataSpeed.setUnitLabel(UI.UNIT_LABEL_SPEED);
         yDataSpeed.setShowYSlider(true);

         /*
          * ensure that painting of the bar is started at the bottom and not at the visible min
          * which is above the bottom !!!
          */
//         yDataSpeed.setGraphFillMethod(ChartDataYSerie.BAR_DRAW_METHOD_BOTTOM);

         chartModel.addYData(yDataSpeed);
      }

      /**
       * Pace
       */
      if (Util.getStateBoolean(_state, STATE_SHOW_PACE_AVG, STATE_SHOW_PACE_AVG_DEFAULT)) {

         // set the bar low/high data
         final ChartDataYSerie yDataPace = new ChartDataYSerie(
               ChartType.BAR,
               ArrayListToArray.toFloat(_statValues_AvgPace_Low),
               ArrayListToArray.toFloat(_statValues_AvgPace_High),
               true);

         final float[] minMaxValues = _currentRefItem.avgPace_MinMax;
         yDataPace.setSliderMinMaxValue(minMaxValues);
         adjustMinMaxValues(yDataPace, minMaxValues);

         TourManager.setBarColors(yDataPace, GraphColorManager.PREF_GRAPH_PACE);
         TourManager.setGraphColors(yDataPace, GraphColorManager.PREF_GRAPH_PACE);

         yDataPace.setAxisUnit(ChartDataSerie.AXIS_UNIT_MINUTE_SECOND);
         yDataPace.setYTitle(OtherMessages.GRAPH_LABEL_PACE);
         yDataPace.setUnitLabel(UI.UNIT_LABEL_PACE);
         yDataPace.setShowYSlider(true);
         yDataPace.setYAxisDirection(!_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SHOW_PACE_GRAPH_INVERTED));

         /*
          * ensure that painting of the bar is started at the bottom and not at the visible min
          * which is above the bottom !!!
          */
//         yDataPace.setGraphFillMethod(ChartDataYSerie.BAR_DRAW_METHOD_BOTTOM);

         chartModel.addYData(yDataPace);
      }

      /**
       * Altimeter (VAM)
       */
      if (Util.getStateBoolean(_state, STATE_SHOW_ALTIMETER_AVG, STATE_SHOW_ALTIMETER_AVG_DEFAULT)) {

         // set the bar low/high data
         final ChartDataYSerie yDataAltimeter = new ChartDataYSerie(
               ChartType.BAR,
               ArrayListToArray.toFloat(_statValues_AvgAltimeter_Low),
               ArrayListToArray.toFloat(_statValues_AvgAltimeter_High),
               true);

         final float[] minMaxValues = _currentRefItem.avgAltimeter_MinMax;
         yDataAltimeter.setSliderMinMaxValue(minMaxValues);
         adjustMinMaxValues(yDataAltimeter, minMaxValues);

         TourManager.setBarColors(yDataAltimeter, GraphColorManager.PREF_GRAPH_ALTIMETER);
         TourManager.setGraphColors(yDataAltimeter, GraphColorManager.PREF_GRAPH_ALTIMETER);

         yDataAltimeter.setYTitle(OtherMessages.GRAPH_LABEL_ALTIMETER);
         yDataAltimeter.setUnitLabel(UI.UNIT_LABEL_ALTIMETER);
         yDataAltimeter.setShowYSlider(true);

         /*
          * ensure that painting of the bar is started at the bottom and not at the visible min
          * which is above the bottom !!!
          */
//         yDataAltimeter.setGraphFillMethod(ChartDataYSerie.BAR_DRAW_METHOD_BOTTOM);

         chartModel.addYData(yDataAltimeter);
      }

      /**
       * Avg pulse
       */
      if (Util.getStateBoolean(_state, STATE_SHOW_PULSE_AVG, STATE_SHOW_PULSE_AVG_DEFAULT)) {

         // set the bar low/high data
         final ChartDataYSerie yDataMaxPulse = new ChartDataYSerie(
               ChartType.BAR,
               ArrayListToArray.toFloat(_statValues_AvgPulse_Low),
               ArrayListToArray.toFloat(_statValues_AvgPulse_High),
               true);

         final float[] minMaxValues = _currentRefItem.maxPulse_MinMax;
         yDataMaxPulse.setSliderMinMaxValue(minMaxValues);
         adjustMinMaxValues(yDataMaxPulse, minMaxValues);

         TourManager.setBarColors(yDataMaxPulse, GraphColorManager.PREF_GRAPH_HEARTBEAT);
         TourManager.setGraphColors(yDataMaxPulse, GraphColorManager.PREF_GRAPH_HEARTBEAT);

         yDataMaxPulse.setYTitle(OtherMessages.GRAPH_LABEL_HEARTBEAT_AVG);
         yDataMaxPulse.setUnitLabel(OtherMessages.GRAPH_LABEL_HEARTBEAT_UNIT);
         yDataMaxPulse.setShowYSlider(true);

         /*
          * Ensure that painting of the bar is started at the bottom and not at the visible min
          * which is above the bottom !!!
          */
//         yDataMaxPulse.setGraphFillMethod(ChartDataYSerie.BAR_DRAW_METHOD_BOTTOM);

         chartModel.addYData(yDataMaxPulse);
      }

      /**
       * Avg/max pulse
       */
      if (Util.getStateBoolean(_state, STATE_SHOW_PULSE_AVG_MAX, STATE_SHOW_PULSE_AVG_MAX_DEFAULT)) {

         // set the bar low/high data
         final ChartDataYSerie yDataMaxPulse = new ChartDataYSerie(
               ChartType.BAR,
               ArrayListToArray.toFloat(_statValues_AvgPulse_High),
               ArrayListToArray.toFloat(_statValues_MaxPulse),
               true);

         final float[] minMaxValues = _currentRefItem.maxPulse_MinMax;
         yDataMaxPulse.setSliderMinMaxValue(minMaxValues);
         adjustMinMaxValues(yDataMaxPulse, minMaxValues);

         TourManager.setBarColors(yDataMaxPulse, GraphColorManager.PREF_GRAPH_HEARTBEAT);
         TourManager.setGraphColors(yDataMaxPulse, GraphColorManager.PREF_GRAPH_HEARTBEAT);

         yDataMaxPulse.setYTitle(OtherMessages.GRAPH_LABEL_HEARTBEAT_AVG_MAX);
         yDataMaxPulse.setUnitLabel(OtherMessages.GRAPH_LABEL_HEARTBEAT_UNIT);
         yDataMaxPulse.setShowYSlider(true);

         /*
          * Ensure that painting of the bar is started at the bottom and not at the visible min
          * which is above the bottom !!!
          */
//         yDataMaxPulse.setGraphFillMethod(ChartDataYSerie.BAR_DRAW_METHOD_BOTTOM);

         chartModel.addYData(yDataMaxPulse);
      }

      /**
       * Setup UI
       */
      // set tool tip info
      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {

         @Override
         public void createToolTipUI(final IToolTipProvider toolTipProvider, final Composite parent, final int serieIndex, final int valueIndex) {

            ReferenceTimelineView.this.createToolTipUI(toolTipProvider, parent, valueIndex);
         }
      });

      net.tourbook.ui.UI.updateChartProperties(_yearChart, PREF_PREFIX);

      final ChartTitleSegmentConfig ctsConfig = _yearChart.getChartTitleSegmentConfig();
      ctsConfig.isShowSegmentTitle = true;

      // show the data in the chart
      _yearChart.updateChart(chartModel, false, false);

      // reset selectable bars otherwise some bars on the right side could not be selectable !!!
      _yearChart.setSelectedBars(null);

      /*
       * Update start year combo box
       */
      final Combo comboLastVisibleYear = _comboLastVisibleYear;

      comboLastVisibleYear.removeAll();
      _allSelectableYears.clear();

      for (int year = firstVisibleYear - 1; year <= _lastVisibleYear + _numVisibleYears; year++) {

         _allSelectableYears.add(year);
         comboLastVisibleYear.add(Integer.toString(year));
      }

      comboLastVisibleYear.select(_numVisibleYears);

      _lblRefTourTitle.setText(_currentRefItem.label);
      _lblRefTourTitle.setForeground(ThemeUtil.getDefaultForegroundColor_Table());

      // update view icon/name
      if (geoCompareData != null) {

         _isGeoCompare = true;

         setTitleImage(_viewImage_GeoCompare);
         setPartName(Messages.Tour_Compare_ViewName_GeoCompareTimeline);

      } else {

         _isGeoCompare = false;

         setTitleImage(_viewImage_ElevationCompare);
         setPartName(Messages.Tour_Compare_ViewName_ElevationCompareTimeline);
      }

      // set background again otherwise the original is displayed
      _headerContainer.setBackground(ThemeUtil.getDefaultBackgroundColor_Table());

      // layout is needed otherwise the horizontal centered text is not displayed
      _lblRefTourTitle.getParent().layout(true, true);
   }

   void updateUI_YearChart_WithCurrentGeoData() {

      updateUI_YearChart(_currentGeoCompareData, false);
   }
}
