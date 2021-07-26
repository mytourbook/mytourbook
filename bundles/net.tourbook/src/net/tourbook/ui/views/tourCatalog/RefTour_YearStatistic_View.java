/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartTitleSegmentConfig;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ArrayListToArray;
import net.tourbook.common.util.IToolTipHideListener;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoIconToolTipProvider;
import net.tourbook.tour.TourManager;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class RefTour_YearStatistic_View extends ViewPart {

   private static final String GRAPH_LABEL_HEARTBEAT      = net.tourbook.common.Messages.Graph_Label_Heartbeat;
   private static final String GRAPH_LABEL_HEARTBEAT_UNIT = net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;

   public static final String  ID                         = "net.tourbook.views.tourCatalog.yearStatisticView";     //$NON-NLS-1$

   static final String         STATE_NUMBER_OF_YEARS      = "numberOfYearsToDisplay";                               //$NON-NLS-1$

   private static final String GRID_PREF_PREFIX           = "GRID_REF_TOUR_YEAR_STATISTIC__";                       //$NON-NLS-1$

// SET_FORMATTING_OFF

   private static final String   GRID_IS_SHOW_VERTICAL_GRIDLINES     = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES);
   private static final String   GRID_IS_SHOW_HORIZONTAL_GRIDLINES   = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);
   private static final String   GRID_VERTICAL_DISTANCE              = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE);
   private static final String   GRID_HORIZONTAL_DISTANCE            = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE);

// SET_FORMATTING_ON

   private static final boolean          _isOSX            = net.tourbook.common.UI.IS_OSX;
   private static final boolean          _isLinux          = net.tourbook.common.UI.IS_LINUX;

   private static final IDialogSettings  _state            = TourbookPlugin.getState("TourCatalogViewYearStatistic"); //$NON-NLS-1$
   private static final IPreferenceStore _prefStore        = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore _prefStore_Common = CommonActivator.getPrefStore();

   private IPropertyChangeListener       _prefChangeListener;
   private IPropertyChangeListener       _prefChangeListener_Common;
   private IPartListener2                _partListener;
   private ISelectionListener            _postSelectionListener;
   private PostSelectionProvider         _postSelectionProvider;

   private NumberFormat                  _nf1              = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   private int[]                             _displayedYears;
   private int[]                             _numberOfDaysInYear;

   /**
    * Years which the user can select as start year in the combo box
    */
   private ArrayList<Integer>                _comboYears              = new ArrayList<>();

   /**
    * Contains all {@link TVICatalogComparedTour} tour objects for all years
    */
   private ArrayList<TVICatalogComparedTour> _statValues_AllTours     = new ArrayList<>();

   /**
    * Day of year values for all displayed years<br>
    * DOY...Day Of Year
    */
   private ArrayList<Integer>                _statValues_DOYValues    = new ArrayList<>();

   /**
    * Tour speed for all years
    */
   private ArrayList<Float>                  _statValues_TourSpeed    = new ArrayList<>();

   /**
    * Average pulse for all years
    */
   private ArrayList<Float>                  _statValues_AvgPulse     = new ArrayList<>();

   /**
    * this is the last year (on the right side) which is displayed in the statistics
    */
   private int                               _lastYear                = TimeTools.now().getYear();

   /**
    * Reference tour item for which the statistic is displayed. This statistic can display only
    * compared tours for ONE reference tour.
    */
   private TVICatalogRefTourItem             _currentRefItem;

   /**
    * selection which is thrown by the year statistic
    */
   private StructuredSelection               _currentSelection;

   private ITourEventListener                _tourEventListener;

   private boolean                           _isSynchMaxValue;

   private int                               _numberOfYears;

   /**
    * Contains the index in {@link #_statValues_AllTours} for the currently selected tour.
    */
   private int                               _selectedTourIndex;

   private IAction                           _actionSynchChartScale;
   private ActionToolbarSlideout             _actionYearStatOptions;

   private YearStatisticTourToolTip          _tourToolTip;
   private TourInfoIconToolTipProvider       _tourInfoToolTipProvider = new TourInfoIconToolTipProvider();

   private PixelConverter                    _pc;

   /*
    * UI controls
    */
   private PageBook  _pageBook;
   private Composite _pageChart;
   private Composite _pageNoChart;

   private Chart     _yearChart;

   private Combo     _cboLastYear;
   private Combo     _cboNumberOfYears;

   private Label     _labelRefTourTitle;

   private class ActionYearStatisticOptions extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new Slideout_YearStatisticOptions(_pageBook, toolbar, GRID_PREF_PREFIX);
      }
   }

   public RefTour_YearStatistic_View() {}

   void actionSynchScale(final boolean isSynchMaxValue) {
      _isSynchMaxValue = isSynchMaxValue;
      updateUI_YearChart(false);
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

            if (property.equals(GRID_HORIZONTAL_DISTANCE)
                  || property.equals(GRID_VERTICAL_DISTANCE)
                  || property.equals(GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
                  || property.equals(GRID_IS_SHOW_VERTICAL_GRIDLINES)) {

               updateUI_YearChart(false);
            }
         }
      };

      _prefChangeListener_Common = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

               // measurement system has changed -> recreate the chart

               _yearChart.dispose();
               createUI_30_Chart(_pageChart);

               _pageChart.layout();

               updateUI_YearChart(false);
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   /**
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            // prevent to listen to a selection which is originated by this year chart

            if (selection != _currentSelection) {
               onSelectionChanged(selection);
            }
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
                  updateUI_YearChart(false);
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   /**
    * Set/restore min/max values.
    */
   private void computeMinMaxValues(final ChartDataYSerie yData) {

      final TVICatalogRefTourItem refItem = _currentRefItem;

      final float minValue = (float) yData.getVisibleMinValue();
      final float maxValue = (float) yData.getVisibleMaxValue();

      final float dataMinValue = minValue;// - (prevMinValue / 100);
      final float dataMaxValue = maxValue;// + (prevMaxValue / 100);

      if (_isSynchMaxValue) {

         if (refItem.yearMapMinValue == Float.MIN_VALUE) {

            // min/max values have not yet been saved

            /*
             * set the min value 10% below the computed so that the lowest value is not at the
             * bottom
             */
            yData.setVisibleMinValue(dataMinValue);
            yData.setVisibleMaxValue(dataMaxValue);

            refItem.yearMapMinValue = dataMinValue;
            refItem.yearMapMaxValue = dataMaxValue;

         } else {

            /*
             * restore min/max values, but make sure min/max values for the current graph are
             * visible and not outside of the chart
             */

            refItem.yearMapMinValue = Math.min(refItem.yearMapMinValue, dataMinValue);
            refItem.yearMapMaxValue = Math.max(refItem.yearMapMaxValue, dataMaxValue);

            yData.setVisibleMinValue(refItem.yearMapMinValue);
            yData.setVisibleMaxValue(refItem.yearMapMaxValue);
         }

      } else {
         yData.setVisibleMinValue(dataMinValue);
         yData.setVisibleMaxValue(dataMaxValue);
      }
   }

   private void createActions() {

      _actionSynchChartScale = new ActionSynchYearScale(this);
      _actionYearStatOptions = new ActionYearStatisticOptions();

      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
      tbm.add(_actionSynchChartScale);
      tbm.add(_actionYearStatOptions);

      tbm.update(true);
   }

   /**
    * create segments for the chart
    */
   private ChartStatisticSegments createChartSegments() {

      final double[] segmentStart = new double[_numberOfYears];
      final double[] segmentEnd = new double[_numberOfYears];
      final String[] segmentTitle = new String[_numberOfYears];

      final int firstYear = getFirstYear();
      int yearDaysSum = 0;

      // create segments for each year
      for (int yearDayIndex = 0; yearDayIndex < _numberOfDaysInYear.length; yearDayIndex++) {

         final int yearDays = _numberOfDaysInYear[yearDayIndex];

         segmentStart[yearDayIndex] = yearDaysSum;
         segmentEnd[yearDayIndex] = yearDaysSum + yearDays - 1;
         segmentTitle[yearDayIndex] = Integer.toString(firstYear + yearDayIndex);

         yearDaysSum += yearDays;
      }

      final ChartStatisticSegments chartSegments = new ChartStatisticSegments();
      chartSegments.segmentStartValue = segmentStart;
      chartSegments.segmentEndValue = segmentEnd;
      chartSegments.segmentTitle = segmentTitle;

      chartSegments.years = _displayedYears;
      chartSegments.yearDays = _numberOfDaysInYear;
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
      addPartListener();

      createActions();

      // set selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      _pageBook.showPage(_pageNoChart);

      restoreState();

      // restore selection
      onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());
   }

   private void createStatisticData_WithoutYearCategories(final int firstYear, final boolean isShowLatestYear) {

      final Object[] allItems = _currentRefItem.getFetchedChildrenAsArray();

      // loop: all tours
      for (final Object item : allItems) {

         if (item instanceof TVICatalogComparedTour) {

            final TVICatalogComparedTour tourItem = (TVICatalogComparedTour) item;

            final int tourYear = tourItem.year;

            if (tourYear >= firstYear && tourYear <= _lastYear) {

               final LocalDate tourDate = tourItem.tourDate;

               _statValues_AllTours.add(tourItem);
               _statValues_DOYValues.add(getYearDOYs(tourDate.getYear()) + tourDate.getDayOfYear() - 1);

               _statValues_AvgPulse.add(tourItem.getAvgPulse());
               _statValues_TourSpeed.add(tourItem.getTourSpeed() / UI.UNIT_VALUE_DISTANCE);
            }
         }
      }
   }

   private void createStatisticData_WithYearCategories(final int firstYear, final boolean isShowLatestYear) {

      final Object[] allYearItems = _currentRefItem.getFetchedChildrenAsArray();

      // get the last year when it's forced
      if (isShowLatestYear && allYearItems != null && allYearItems.length > 0) {

         final Object firstItem = allYearItems[0];
         final Object lastItem = allYearItems[allYearItems.length - 1];

         if (lastItem instanceof TVICatalogYearItem) {

            final int newFirstYear = ((TVICatalogYearItem) firstItem).year;
            final int newLastYear = ((TVICatalogYearItem) lastItem).year;

            /*
             * Use current years when the new items are in the current range, otherwise adjust the
             * years
             */
            if (newLastYear <= _lastYear && newFirstYear >= _lastYear - _numberOfYears) {

               // new years are within the current year range

            } else {

               // overwrite last year
               _lastYear = newLastYear;
            }
         }
      }


      /**
       * Create data for all years
       */
      for (final Object yearItemObj : allYearItems) {

         if (yearItemObj instanceof TVICatalogYearItem) {

            final TVICatalogYearItem yearItem = (TVICatalogYearItem) yearItemObj;

            // check if the year can be displayed
            final int yearItemYear = yearItem.year;
            if (yearItemYear >= firstYear && yearItemYear <= _lastYear) {

               // loop: all tours
               final Object[] tourItems = yearItem.getFetchedChildrenAsArray();
               for (final Object tourItemObj : tourItems) {
                  if (tourItemObj instanceof TVICatalogComparedTour) {

                     final TVICatalogComparedTour tourItem = (TVICatalogComparedTour) tourItemObj;

                     final LocalDate tourDate = tourItem.tourDate;

                     _statValues_AllTours.add(tourItem);
                     _statValues_DOYValues.add(getYearDOYs(tourDate.getYear()) + tourDate.getDayOfYear() - 1);

                     _statValues_AvgPulse.add(tourItem.getAvgPulse());
                     _statValues_TourSpeed.add(tourItem.getTourSpeed() / UI.UNIT_VALUE_DISTANCE);
                  }
               }
            }
         }
      }
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
      final int firstYear = getFirstYear();
      final int tourDOY = _statValues_DOYValues.get(valueIndex);

      final ZonedDateTime tourDate = ZonedDateTime
            .of(firstYear, 1, 1, 0, 0, 0, 1, TimeTools.getDefaultTimeZone())
            .plusDays(tourDOY);
      final String title = tourDate.format(TimeTools.Formatter_Date_F);

      new RefTour_YearStatistic_TooltipUI().createContentArea(

            parent,
            toolTipProvider,

            title,

            _statValues_AvgPulse.get(valueIndex),
            _statValues_TourSpeed.get(valueIndex));
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoChart = net.tourbook.ui.UI.createPage(_pageBook, Messages.tourCatalog_view_label_year_not_selected);

      _pageChart = createUI_10_PageYearChart(_pageBook);
   }

   private Composite createUI_10_PageYearChart(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(1).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         createUI_20_Toolbar(container);
         createUI_30_Chart(container);
      }

      return container;
   }

   /**
    * Toolbar
    */
   private void createUI_20_Toolbar(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.FILL, SWT.FILL)
            .applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(4)
            .margins(3, 3)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Last year
             */
            _cboLastYear = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            _cboLastYear.setToolTipText(Messages.Year_Statistic_Combo_LastYears_Tooltip);
            _cboLastYear.setVisibleItemCount(50);
            _cboLastYear.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectYear()));
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(_isOSX ? 12 : _isLinux ? 12 : 5), SWT.DEFAULT)
                  .applyTo(_cboLastYear);
         }
         {
            /*
             * Number of years
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Year_Statistic_Label_NumberOfYears);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(10, 0)
                  .applyTo(label);

            // combo
            _cboNumberOfYears = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            _cboNumberOfYears.setToolTipText(Messages.Year_Statistic_Combo_NumberOfYears_Tooltip);
            _cboNumberOfYears.setVisibleItemCount(50);
            _cboNumberOfYears.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectNumberOfYears(getSelectedYears())));
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(_isOSX ? 8 : _isLinux ? 8 : 4), SWT.DEFAULT)
                  .applyTo(_cboNumberOfYears);
         }
         {
            /*
             * Ref tour title
             */
            _labelRefTourTitle = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_labelRefTourTitle);
         }
      }
   }

   /**
    * year chart
    */
   private void createUI_30_Chart(final Composite parent) {

      _yearChart = new Chart(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_yearChart);

      _yearChart.addBarSelectionListener(new IBarSelectionListener() {
         @Override
         public void selectionChanged(final int serieIndex, final int valueIndex) {

            if (_statValues_AllTours.isEmpty()) {
               _tourInfoToolTipProvider.setTourId(-1);
               return;
            }

            // ensure list size
            _selectedTourIndex = Math.min(valueIndex, _statValues_AllTours.size() - 1);

            // select tour in the tour viewer & show tour in compared tour char
            final TVICatalogComparedTour tourCatalogComparedTour = _statValues_AllTours.get(_selectedTourIndex);
            _currentSelection = new StructuredSelection(tourCatalogComparedTour);
            _postSelectionProvider.setSelection(_currentSelection);

            _tourInfoToolTipProvider.setTourId(tourCatalogComparedTour.getTourId());
         }
      });

      // set tour info icon into the left axis
      _tourToolTip = new YearStatisticTourToolTip(_yearChart.getToolTipControl());
      _tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);
      _tourToolTip.addHideListener(new IToolTipHideListener() {
         @Override
         public void afterHideToolTip(final Event event) {
            // hide hovered image
            _yearChart.getToolTipControl().afterHideToolTip();
         }
      });

      _yearChart.setTourInfoIconToolTipProvider(_tourInfoToolTipProvider);
      _tourInfoToolTipProvider.setActionsEnabled(true);

   }

   @Override
   public void dispose() {

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      getViewSite().getPage().removePartListener(_partListener);
      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      super.dispose();
   }

   private int getFirstYear() {
      return _lastYear - _numberOfYears + 1;
   }

   private int getSelectedYears() {
      return _cboNumberOfYears.getSelectionIndex() + 1;
   }

   /**
    * @param currentYear
    * @param numberOfYears
    * @return Returns the number of days between {@link #fLastYear} and currentYear
    */
   int getYearDOYs(final int selectedYear) {

      int yearDOYs = 0;
      int yearIndex = 0;

      final int firstYear = getFirstYear();

      for (int currentYear = firstYear; currentYear < selectedYear; currentYear++) {

         if (currentYear == selectedYear) {
            return yearDOYs;
         }

         yearDOYs += _numberOfDaysInYear[yearIndex];

         yearIndex++;
      }

      return yearDOYs;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   /**
    * get numbers for each year <br>
    * <br>
    * all years into {@link #fYears} <br>
    * number of day's into {@link #_numberOfDaysInYear} <br>
    * number of week's into {@link #fYearWeeks}
    */
   void initYearNumbers() {

   }

   TVICatalogComparedTour navigateTour(final boolean isNextTour) {

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

   private void onSelectionChanged(final ISelection selection) {

      if (selection instanceof SelectionTourCatalogView) {

         final SelectionTourCatalogView tourCatalogItem = (SelectionTourCatalogView) selection;

         final TVICatalogRefTourItem refItem = tourCatalogItem.getRefItem();
         if (refItem != null) {

            // reference tour is selected

            _currentRefItem = refItem;
            updateUI_YearChart(true);

         } else {

            // show statistic for a specific year

            final TVICatalogYearItem yearItem = tourCatalogItem.getYearItem();
            if (yearItem != null) {

               _currentRefItem = yearItem.getRefItem();

               // overwrite last year
               _lastYear = yearItem.year;

               // update year data
               setYearData();

               updateUI_YearChart(false);
            }
         }

         // select tour in the statistic
         final Long compTourId = tourCatalogItem.getCompTourId();
         if (compTourId != null) {

            selectTourInYearChart(compTourId);

         } else {

            // select first tour for the youngest year
            int yearIndex = 0;
            for (final TVICatalogComparedTour tourItem : _statValues_AllTours) {

               if (tourItem.tourDate.getYear() == _lastYear) {
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

            if (firstElement instanceof TVICatalogComparedTour) {

               final TVICatalogComparedTour compareItem = (TVICatalogComparedTour) firstElement;

               // get year item
               final TreeViewerItem compareParentItem = compareItem.getParentItem();
               if (compareParentItem instanceof TVICatalogYearItem) {

                  final TVICatalogYearItem yearItem = (TVICatalogYearItem) compareParentItem;

                  // get ref tour item
                  final TreeViewerItem yearParentItem = yearItem.getParentItem();
                  if (yearParentItem instanceof TVICatalogRefTourItem) {

                     final TVICatalogRefTourItem refTourItem = (TVICatalogRefTourItem) yearParentItem;

                     final long refId = refTourItem.refId;

                     if (_currentRefItem == null) {

                        // create new ref item for the ref tour
                        _currentRefItem = TourCompareManager.createCatalogRefItem(refId);

                        updateUI_YearChart(false);

                     } else {

                        if (_currentRefItem.refId != refId) {

                           // the current statistic do not show the ref tour for the compared tour
                           // -> show also the ref tour

                           _currentRefItem = refTourItem;

                           updateUI_YearChart(false);
                        }
                     }
                  }
               }

               // select tour in the year chart
               final Long tourId = compareItem.getTourId();
               if (tourId != null) {
                  selectTourInYearChart(tourId);
               }

            } else if (firstElement instanceof TVICompareResultComparedTour) {

               final TVICompareResultComparedTour compareResult = (TVICompareResultComparedTour) firstElement;

               final Long tourId = compareResult.getTourId();
               if (tourId != null) {

                  final RefTourItem refTour = compareResult.refTour;

                  final long refId = refTour.refId;
                  if (_currentRefItem == null || _currentRefItem.refId != refId) {

                     // the current statistic do not show the ref tour for the compared tour
                     // -> first show the ref tour

                     // create new ref item for the ref tour
                     _currentRefItem = TourCompareManager.createCatalogRefItem(refId);

                     updateUI_YearChart(false);
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

            _currentRefItem = TourCompareManager.createCatalogRefItem(refTourId);

            updateUI_YearChart(false);
         }

      } else if (selection instanceof SelectionPersistedCompareResults) {

         final SelectionPersistedCompareResults savedCompTours = (SelectionPersistedCompareResults) selection;

         final ArrayList<TVICompareResultComparedTour> persistedCompareResults = savedCompTours.persistedCompareResults;

         if (persistedCompareResults.size() > 0) {

            // create new ref item which contains the newly persisted compared tours

            final TVICompareResultComparedTour tviCompareResultComparedTour = persistedCompareResults.get(0);
            final RefTourItem refTour = tviCompareResultComparedTour.refTour;
            final long savedRefId = refTour.refId;

            _currentRefItem = TourCompareManager.createCatalogRefItem(savedRefId);

            updateUI_YearChart(false);
         }
      }
   }

   /**
    * Update statistic by setting number of years
    *
    * @param numberOfYears
    */
   private void onSelectNumberOfYears(final int numberOfYears) {

      // get selected tour
      long selectedTourId = 0;
      if (_statValues_AllTours.isEmpty()) {
         selectedTourId = -1;
      } else {
         final int selectedTourIndex = Math.min(_selectedTourIndex, _statValues_AllTours.size() - 1);
         selectedTourId = _statValues_AllTours.get(selectedTourIndex).getTourId();
      }

      _numberOfYears = numberOfYears;
      setYearData();

      updateUI_YearChart(false);

      // reselect last selected tour
      selectTourInYearChart(selectedTourId);
   }

   private void onSelectYear() {

      // overwrite last year
      _lastYear = _comboYears.get(_cboLastYear.getSelectionIndex());

      // update year data
      setYearData();

      updateUI_YearChart(false);
   }

   private void restoreState() {

      // fill combo box
      for (int numYears = 1; numYears <= 50; numYears++) {
         _cboNumberOfYears.add(Integer.toString(numYears));
      }

      // select previous value
      final int selectedYear = Util.getStateInt(_state, RefTour_YearStatistic_View.STATE_NUMBER_OF_YEARS, 3);
      _cboNumberOfYears.select(Math.min(selectedYear - 1, _cboNumberOfYears.getItemCount() - 1));

      _numberOfYears = getSelectedYears();

      setYearData();
   }

   @PersistState
   private void saveState() {

      // save number of years which are displayed
      _state.put(STATE_NUMBER_OF_YEARS, _numberOfYears);
   }

   /**
    * select the tour in the year map chart
    *
    * @param tourIdToSelect
    *           tour id which should be selected
    */
   private void selectTourInYearChart(final long tourIdToSelect) {

      if (_statValues_AllTours.isEmpty()) {
         _tourInfoToolTipProvider.setTourId(-1);
         return;
      }

      final int tourLength = _statValues_AllTours.size();
      final boolean[] selectedTours = new boolean[tourLength];
      boolean isTourSelected = false;

      for (int tourIndex = 0; tourIndex < tourLength; tourIndex++) {

         final TVICatalogComparedTour comparedItem = _statValues_AllTours.get(tourIndex);

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
    * get data for each displayed year
    */
   private void setYearData() {

      _displayedYears = new int[_numberOfYears];
      _numberOfDaysInYear = new int[_numberOfYears];

      final int firstYear = getFirstYear();

      int yearIndex = 0;
      for (int currentYear = firstYear; currentYear <= _lastYear; currentYear++) {

         _displayedYears[yearIndex] = currentYear;
         _numberOfDaysInYear[yearIndex] = TimeTools.getNumberOfDaysWithYear(currentYear);

         yearIndex++;
      }
   }

   /**
    * Show statistic for several years
    *
    * @param isShowLatestYear
    *           Shows the latest year and the years before
    */
   private void updateUI_YearChart(final boolean isShowLatestYear) {

      if (_currentRefItem == null) {

         _pageBook.showPage(_pageNoChart);

         return;
      }

      _pageBook.showPage(_pageChart);

      /*
       * Reset statistic values
       */
      _statValues_AllTours.clear();
      _statValues_DOYValues.clear();

      _statValues_AvgPulse.clear();
      _statValues_TourSpeed.clear();

      final int firstYear = getFirstYear();

      if (TourCompareManager.getReferenceTour_ViewLayout() == TourCompareManager.REF_TOUR_VIEW_LAYOUT_WITH_YEAR_CATEGORIES) {

         // with year categories

         createStatisticData_WithYearCategories(firstYear, isShowLatestYear);

      } else {

         // without year categories

         createStatisticData_WithoutYearCategories(firstYear, isShowLatestYear);
      }


      final ChartDataModel chartModel = new ChartDataModel(ChartType.BAR);

      final ChartDataXSerie xData = new ChartDataXSerie(ArrayListToArray.integerToDouble(_statValues_DOYValues));
      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_DAY);
      xData.setChartSegments(createChartSegments());
      chartModel.setXData(xData);

      /**
       * Speed
       */
      // set the bar low/high data
      final ChartDataYSerie yDataSpeed = new ChartDataYSerie(
            ChartType.BAR,
            ArrayListToArray.toFloat(_statValues_TourSpeed),
            true);

      computeMinMaxValues(yDataSpeed);

      TourManager.setBarColors(yDataSpeed, GraphColorManager.PREF_GRAPH_SPEED);
      TourManager.setGraphColors(yDataSpeed, GraphColorManager.PREF_GRAPH_SPEED);

      yDataSpeed.setYTitle(Messages.tourCatalog_view_label_year_chart_title);
      yDataSpeed.setUnitLabel(UI.UNIT_LABEL_SPEED);
      yDataSpeed.setShowYSlider(true);

      /*
       * ensure that painting of the bar is started at the bottom and not at the visible min which
       * is above the bottom !!!
       */
      yDataSpeed.setGraphFillMethod(ChartDataYSerie.BAR_DRAW_METHOD_BOTTOM);

      chartModel.addYData(yDataSpeed);

      /**
       * Pulse
       */
      // set the bar low/high data
      final ChartDataYSerie yDataPulse = new ChartDataYSerie(
            ChartType.BAR,
            ArrayListToArray.toFloat(_statValues_AvgPulse),
            true);

      computeMinMaxValues(yDataPulse);

      TourManager.setBarColors(yDataPulse, GraphColorManager.PREF_GRAPH_HEARTBEAT);
      TourManager.setGraphColors(yDataPulse, GraphColorManager.PREF_GRAPH_HEARTBEAT);

      yDataPulse.setYTitle(GRAPH_LABEL_HEARTBEAT);
      yDataPulse.setUnitLabel(GRAPH_LABEL_HEARTBEAT_UNIT);
      yDataPulse.setShowYSlider(true);

      /*
       * ensure that painting of the bar is started at the bottom and not at the visible min which
       * is above the bottom !!!
       */
      yDataPulse.setGraphFillMethod(ChartDataYSerie.BAR_DRAW_METHOD_BOTTOM);

      chartModel.addYData(yDataPulse);

      /**
       * Setup UI
       */
      // set tool tip info
      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {

         @Override
         public void createToolTipUI(final IToolTipProvider toolTipProvider, final Composite parent, final int serieIndex, final int valueIndex) {

            RefTour_YearStatistic_View.this.createToolTipUI(toolTipProvider, parent, valueIndex);
         }
      });

      net.tourbook.ui.UI.updateChartProperties(_yearChart, GRID_PREF_PREFIX);

      final ChartTitleSegmentConfig ctsConfig = _yearChart.getChartTitleSegmentConfig();
      ctsConfig.isShowSegmentTitle = true;

      // show the data in the chart
      _yearChart.updateChart(chartModel, false, true);

      /*
       * update start year combo box
       */
      _cboLastYear.removeAll();
      _comboYears.clear();

      for (int year = firstYear - 1; year <= _lastYear + _numberOfYears; year++) {
         _cboLastYear.add(Integer.toString(year));
         _comboYears.add(year);
      }

      _cboLastYear.select(_numberOfYears - 0);

      _labelRefTourTitle.setText(_currentRefItem.label);
   }
}
