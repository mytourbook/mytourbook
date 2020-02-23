/*******************************************************************************
 * Copyright (C) 2019 Frédéric Bard and Contributors
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
package net.tourbook.ui.views.performanceModelingChart;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.MinMaxKeeper_YData;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.trainingstress.ITrainingStressDataListener;
import net.tourbook.ui.UI;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class PerformanceModelingChartView extends ViewPart {

   public static final String  ID                                    = "net.tourbook.ui.views.performanceModelingChart.PerformanceModelingChartView"; //$NON-NLS-1$

   private static final int    HR_LEFT_MIN_BORDER                    = 0;
   private static final int    HR_RIGHT_MAX_BORDER                   = 230;

   private static final String STATE_HR_CHART_LEFT_BORDER            = "HrLeftChartBorder";                                                           //$NON-NLS-1$
   private static final String STATE_HR_CHART_RIGHT_BORDER           = "HrRightChartBorder";                                                          //$NON-NLS-1$
   private static final String STATE_IS_SHOW_ALL_STRESS_SCORE_VALUES = "IsShowAllStressScoreValues";                                                  //$NON-NLS-1$
   private static final String STATE_IS_SYNC_VERTICAL_CHART_SCALING  = "IsSyncVerticalChartScaling";                                                  //$NON-NLS-1$

   private static final String GRID_PREF_PREFIX                      = "GRID_TRAINING__";                                                             //$NON-NLS-1$

// SET_FORMATTING_OFF

   private static final String         GRID_IS_SHOW_VERTICAL_GRIDLINES     = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES);
   private static final String         GRID_IS_SHOW_HORIZONTAL_GRIDLINES   = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);
   private static final String         GRID_VERTICAL_DISTANCE              = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE);
   private static final String         GRID_HORIZONTAL_DISTANCE            = (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE);

// SET_FORMATTING_ON

   private final IPreferenceStore         _prefStore    = TourbookPlugin.getPrefStore();
   private final IDialogSettings          _state        = TourbookPlugin.getState(ID);

   private IPropertyChangeListener        _prefChangeListener;
   private ITrainingStressDataListener    _trainingStressDataListener;

   private ModifyListener                 _defaultSpinnerModifyListener;
   private SelectionAdapter               _defaultSpinnerSelectionListener;
   private MouseWheelListener             _defaultSpinnerMouseWheelListener;

   private TourPerson                     _currentPerson;

   private LocalDate                      _oldestEntryDate;
   private LocalDate                      _newestEntryDate;

   private boolean                        _isUpdateUI;
   private boolean                        _isShowAllValues;
   private boolean                        _isSynchChartVerticalValues;

   private ToolBarManager                 _headerToolbarManager;

   private ActionShowAllStressScoreValues _actionShowAllStressScoreValues;
   private ActionSynchChartScale          _actionSynchVerticalChartScaling;
   private ActionTrainingOptions          _actionTrainingOptions;

   private double[]                       _xSeriePulse;

   private final MinMaxKeeper_YData       _minMaxKeeper = new MinMaxKeeper_YData();

   private final NumberFormat             _nf1          = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   /*
    * UI controls/resources
    */
   private FormToolkit _tk;

   /*
    * Pagebook for the predicted performance view
    */
   private PageBook  _pageBook;
   private Composite _page_TrainingStressScores;
   private Composite _page_NoPerson;

   private Composite _toolbar;
   private Chart     _chartPerformanceModelingData;

   private Spinner   _spinnerHrLeft;
   private Spinner   _spinnerHrRight;
   private Label     _lblHrMin;
   private Label     _lblHrMax;

   /*
    * none UI
    */

   private class ActionTrainingOptions extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutTrainingOptions(_pageBook, toolbar, GRID_PREF_PREFIX, PerformanceModelingChartView.this);
      }
   }

   public PerformanceModelingChartView() {

   }

   void actionShowAllStressScoreValues() {

      _isShowAllValues = true;//_actionShowAllStressScoreValues.isChecked();

      updateUI_10_stressScoreValuesFromModel();
   }

   void actionSynchChartScale() {

      _isSynchChartVerticalValues = _actionSynchVerticalChartScaling.isChecked();

      if (_isSynchChartVerticalValues == false) {
         _minMaxKeeper.resetMinMax();
      }

      updateUI_10_stressScoreValuesFromModel();
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            /*
             * set a new chart configuration when the preferences has changed
             */
            if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)
                  || property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

               onModifyPerson();

            } else if (property.equals(GRID_HORIZONTAL_DISTANCE)
                  || property.equals(GRID_VERTICAL_DISTANCE)
                  || property.equals(GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
                  || property.equals(GRID_IS_SHOW_VERTICAL_GRIDLINES)
            //
            ) {

               setChartProperties();

               // grid has changed, update chart
               updateUI_10_stressScoreValuesFromModel();
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void addTrainingStressDataListener() {
      //TODO when the user is changed, unregister the previous user from the listener and register
      //final the new one

      _currentPerson = TourbookPlugin.getActivePerson();

      if (_currentPerson != null && _currentPerson.getPerformanceModelingData() != null) {
         _currentPerson.getPerformanceModelingData().addTrainingStressDataListener(
               new ITrainingStressDataListener() {
                  @Override
                  public void trainingStressDataIsModified() {
                     updateUI_40_performanceModelingChart();
                  }
               });
      }
   }

   private void clearView() {

      _currentPerson = null;

      _chartPerformanceModelingData.updateChart(null, false);

      _pageBook.showPage(_page_NoPerson);
      enableControls();
   }

   private void createActions() {

      _actionShowAllStressScoreValues = new ActionShowAllStressScoreValues(this);
      _actionSynchVerticalChartScaling = new ActionSynchChartScale(this);
      _actionTrainingOptions = new ActionTrainingOptions();
   }

   @Override
   public void createPartControl(final Composite parent) {

      createUI(parent);

      createActions();
      fillToolbar();

      // show default page
      _pageBook.showPage(_page_NoPerson);

      addPrefListener();
      addTrainingStressDataListener();

      restoreState();

      updateUI_10_stressScoreValuesFromModel();

   }

   private ChartToolTipInfo createToolTipInfo(final int serieIndex, final int valueIndex) {

      final int oldestYear = 2011;//_statFirstYear - _statNumberOfYears + 1;

      final LocalDate monthDate = LocalDate.of(oldestYear, 1, 1).plusMonths(valueIndex);

      final String monthText = Month
            .of(monthDate.getMonthValue())
            .getDisplayName(TextStyle.FULL, Locale.getDefault());

      /*
       * tool tip: title
       */
      final StringBuilder sbTitle = new StringBuilder();
      sbTitle.append("Score of the day");

      /*
       * tool tip: label
       */
      final StringBuilder toolTipLabel = new StringBuilder();
      toolTipLabel.append("Score");
      toolTipLabel.append(UI.NEW_LINE);
      toolTipLabel.append("234");

      /*
       * create tool tip info
       */

      final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
      toolTipInfo.setTitle(sbTitle.toString());
      toolTipInfo.setLabel(toolTipLabel.toString());

      return toolTipInfo;
   }

   private void createUI(final Composite parent) {

      initUI(parent);

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()//
            .spacing(0, 0)
            .applyTo(container);
      {
         createUI_10_Toolbar(container);
         createUI_20_PerformanceModelingChart(container);
      }

   }

   private void createUI_10_Toolbar(final Composite parent) {

      _toolbar = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .align(SWT.BEGINNING, SWT.FILL)
            .applyTo(_toolbar);
      GridLayoutFactory.fillDefaults()//
            .numColumns(9)
            .margins(3, 3)
            .applyTo(_toolbar);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         /*
          * label: hr min
          */
         _lblHrMin = new Label(_toolbar, SWT.NONE);
         GridDataFactory.fillDefaults()//
//               .indent(5, 0)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_lblHrMin);
         _lblHrMin.setText(Messages.Training_View_Label_LeftChartBorder);
         _lblHrMin.setToolTipText(Messages.Training_View_Label_LeftChartBorder_Tooltip);

         /*
          * spinner: hr min
          */
         _spinnerHrLeft = new Spinner(_toolbar, SWT.BORDER);
         _spinnerHrLeft.setMinimum(HR_LEFT_MIN_BORDER);
         _spinnerHrLeft.setMaximum(HR_RIGHT_MAX_BORDER);
         _spinnerHrLeft.addModifyListener(_defaultSpinnerModifyListener);
         _spinnerHrLeft.addSelectionListener(_defaultSpinnerSelectionListener);
         _spinnerHrLeft.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

         /*
          * label: hr max
          */
         _lblHrMax = new Label(_toolbar, SWT.NONE);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_lblHrMax);
         _lblHrMax.setText(Messages.Training_View_Label_RightChartBorder);
         _lblHrMax.setToolTipText(Messages.Training_View_Label_RightChartBorder_Tooltip);

         /*
          * spinner: hr max
          */
         _spinnerHrRight = new Spinner(_toolbar, SWT.BORDER);
         _spinnerHrRight.setMinimum(HR_LEFT_MIN_BORDER);
         _spinnerHrRight.setMaximum(HR_RIGHT_MAX_BORDER);
         _spinnerHrRight.addModifyListener(_defaultSpinnerModifyListener);
         _spinnerHrRight.addSelectionListener(_defaultSpinnerSelectionListener);
         _spinnerHrRight.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

         // label: vertical separator
         final Label label = new Label(_toolbar, SWT.SEPARATOR | SWT.VERTICAL);
         label.setText(UI.EMPTY_STRING);

         /*
          * toolbar actions
          */
         final ToolBar toolbar = new ToolBar(_toolbar, SWT.FLAT);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(toolbar);
         _headerToolbarManager = new ToolBarManager(toolbar);

      }

   }

   private void createUI_20_PerformanceModelingChart(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

      _page_TrainingStressScores = createUI_20_TrainingStressScores(_pageBook);

      _page_NoPerson = UI.createPage(_tk, _pageBook, Messages.UI_Label_No_Person_Is_Selected);
   }

   private Composite createUI_20_TrainingStressScores(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .applyTo(container);
      GridLayoutFactory.fillDefaults()//
            .numColumns(3)
            .spacing(0, 0)
            .applyTo(container);
      container.setBackground(_tk.getColors().getBackground());
      {
         createUI_30_TrainingStressScoresChart(container);
      }

      return container;
   }

   private void createUI_30_TrainingStressScoresChart(final Composite parent) {

      /*
       * chart
       */
      _chartPerformanceModelingData = new Chart(parent, SWT.FLAT);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartPerformanceModelingData);

      setChartProperties();

      _chartPerformanceModelingData.addBarSelectionListener(new IBarSelectionListener() {
         @Override
         public void selectionChanged(final int serieIndex, final int valueIndex) {

//               _postSelectionProvider.setSelection(selection);
         }
      });
   }

   @Override
   public void dispose() {

      if (_tk != null) {
         _tk.dispose();
      }

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void enableControls() {

      final boolean isCustomScaling = _isShowAllValues == false;

      _spinnerHrLeft.setEnabled(isCustomScaling);
      _spinnerHrRight.setEnabled(isCustomScaling);
      _lblHrMin.setEnabled(isCustomScaling);
      _lblHrMax.setEnabled(isCustomScaling);

      _actionSynchVerticalChartScaling.setEnabled(isCustomScaling);
      _actionShowAllStressScoreValues.setEnabled(true);//isHrZoneAvailable);
      _actionTrainingOptions.setEnabled(true);//isHrZoneAvailable);
   }

   private void fillToolbar() {

      /*
       * View toolbar
       */
      /*
       * final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
       * tbm.add(_actionTrainingOptions);
       * // update toolbar which creates the slideout
       * tbm.update(true);
       */
      /*
       * Header toolbar
       */
      _headerToolbarManager.add(_actionShowAllStressScoreValues);
      _headerToolbarManager.add(_actionSynchVerticalChartScaling);

      _headerToolbarManager.update(true);
   }

   private LocalDate findExtremeDates(final HashMap<LocalDate, ArrayList<Long>> entries, final boolean oldest) {
      Map.Entry<LocalDate, ArrayList<Long>> oldestEntry = null;
      Map.Entry<LocalDate, ArrayList<Long>> newestEntry = null;

      for (final Map.Entry<LocalDate, ArrayList<Long>> entry : entries.entrySet()) {

         if (oldest && (oldestEntry == null || entry.getKey().compareTo(oldestEntry.getKey()) < 0)) {
            oldestEntry = entry;
         }
         if (!oldest && (newestEntry == null || entry.getKey().compareTo(newestEntry.getKey()) > 0)) {
            newestEntry = entry;
         }
      }

      return oldest ? oldestEntry.getKey() : newestEntry.getKey();
   }

   private void initUI(final Composite parent) {

      _tk = new FormToolkit(parent.getDisplay());

      _defaultSpinnerModifyListener = new ModifyListener() {
         @Override
         public void modifyText(final ModifyEvent e) {
            if (_isUpdateUI) {
               return;
            }
            onModifyHrBorder();
         }
      };

      _defaultSpinnerSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            if (_isUpdateUI) {
               return;
            }
            onModifyHrBorder();
         }
      };

      _defaultSpinnerMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            Util.adjustSpinnerValueOnMouseScroll(event);
            if (_isUpdateUI) {
               return;
            }
            onModifyHrBorder();
         }
      };

   }

   private void onModifyHrBorder() {

      int left = _spinnerHrLeft.getSelection();
      int right = _spinnerHrRight.getSelection();

      _isUpdateUI = true;
      {
         if (left == HR_LEFT_MIN_BORDER && right == HR_LEFT_MIN_BORDER) {

            right++;
            _spinnerHrRight.setSelection(right);

         } else if (left == HR_RIGHT_MAX_BORDER && right == HR_RIGHT_MAX_BORDER) {

            left--;
            _spinnerHrLeft.setSelection(left);

         } else if (left >= right) {

            left = right - 1;
            _spinnerHrLeft.setSelection(left);
         }
      }
      _isUpdateUI = false;

      updateUI_10_stressScoreValuesFromModel();
   }

   /**
    * A new person was selected
    */
   private void onModifyPerson() {

      clearView();

      // update ui to show the current person datashowTour();
      updateUI_10_stressScoreValuesFromModel();
   }

   private void restoreState() {

      _isShowAllValues = Util.getStateBoolean(_state, STATE_IS_SHOW_ALL_STRESS_SCORE_VALUES, false);
      _actionShowAllStressScoreValues.setChecked(_isShowAllValues);

      _isSynchChartVerticalValues = Util.getStateBoolean(_state, STATE_IS_SYNC_VERTICAL_CHART_SCALING, false);
      _actionSynchVerticalChartScaling.setChecked(_isSynchChartVerticalValues);

      _isUpdateUI = true;
      {
         _spinnerHrLeft.setSelection(Util.getStateInt(_state, STATE_HR_CHART_LEFT_BORDER, 60));
         _spinnerHrRight.setSelection(Util.getStateInt(_state, STATE_HR_CHART_RIGHT_BORDER, 200));
      }
      _isUpdateUI = false;
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_HR_CHART_LEFT_BORDER, _spinnerHrLeft.getSelection());
      _state.put(STATE_HR_CHART_RIGHT_BORDER, _spinnerHrRight.getSelection());

      _state.put(STATE_IS_SHOW_ALL_STRESS_SCORE_VALUES, _actionShowAllStressScoreValues.isChecked());
      _state.put(STATE_IS_SYNC_VERTICAL_CHART_SCALING, _actionSynchVerticalChartScaling.isChecked());
   }

   private void setChartProperties() {

      UI.updateChartProperties(_chartPerformanceModelingData, GRID_PREF_PREFIX);

      // show title
      _chartPerformanceModelingData.getChartTitleSegmentConfig().isShowSegmentTitle = true;
   }

   @Override
   public void setFocus() {

   }

   private void updateUI(final long tourId) {

      if (_currentPerson == null) {
         // optimize
         return;
      }

      updateUI_20();
   }

   /**
    */
   private void updateUI_10_stressScoreValuesFromModel() {

      enableControls();

      _currentPerson = TourbookPlugin.getActivePerson();

      /*
       * check person
       */
      if (_currentPerson == null) {

         _pageBook.showPage(_page_NoPerson);
         return;
      }

      /*
       * required data are available
       */

      // display page for the selected chart
      _pageBook.showPage(_page_TrainingStressScores);
      updateUI_40_performanceModelingChart();

   }

   private void updateUI_20() {

      if (_currentPerson == null) {
         // nothing to do
         return;
      }

      updateUI_10_stressScoreValuesFromModel();
   }

   /**
    * Updates the chart displaying :
    * - Stress scores
    * - Fitness values
    * - Fatigues values
    * - Performance modeling values
    */
   private void updateUI_40_performanceModelingChart() {

      //TourManager.GETALL TOURS

      // We get the govssentries
      if (_currentPerson.getPerformanceModelingData() == null ||
            _currentPerson.getPerformanceModelingData().getGovssEntries() == null) {
         return;
      }

      final HashMap<LocalDate, ArrayList<Long>> govssEntries = _currentPerson.getPerformanceModelingData().getGovssEntries();

      // We find he oldest date

      //We create an array fo which its capacity is the number of days between the first date and the last date

      _oldestEntryDate = findExtremeDates(govssEntries, true);
      _newestEntryDate = findExtremeDates(govssEntries, false);
      final int numberOfDays = (int) ChronoUnit.DAYS.between(_oldestEntryDate, _newestEntryDate) + 1;

      final RGB[] rgbBright = new RGB[numberOfDays];
      final RGB[] rgbDark = new RGB[numberOfDays];
      final RGB[] rgbLine = new RGB[numberOfDays];

      for (int index = 0; index < numberOfDays; ++index) {

         rgbDark[index] = new RGB(0, 255, 255);
         rgbBright[index] = new RGB(0, 255, 255);
         rgbLine[index] = new RGB(0, 255, 255);
      }

      /*
       * create x-data series
       */
      _xSeriePulse = new double[numberOfDays];

      final int[] colorIndex = new int[numberOfDays];

      for (int index = 0; index < numberOfDays; index++) {

         _xSeriePulse[index] = index;
      }

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.LINE);
      chartDataModel.setIsGraphOverlapped(true);

/*
 * final ChartDataModel newChartDataModel = TourManager.getInstance().createChartDataModel(
 * _tourData,
 * _tcc,
 * isPropertyChanged);
 */

//      chartDataModel.setTitle(TourManager.getTourDateTimeFull(_tourData));

      /*
       * x-axis: pulse
       */
      final ChartDataXSerie xData = new ChartDataXSerie(_xSeriePulse);
      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_NUMBER_CENTER);
      xData.setUnitLabel("date");//net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit);
      xData.setUnitStartValue(0);

      chartDataModel.setXData(xData);

      /*
       * y-axis: GOVSS values
       */
      final float[] predictedPerformanceValues = new float[numberOfDays];
      final HashMap<LocalDate, Integer> fitnessValuesSkiba = _currentPerson.getPerformanceModelingData().getFitnessValuesSkiba();
      LocalDate currentDatetorneame = _oldestEntryDate;
      for (int toto = 0; toto < numberOfDays; ++toto) {

         if (fitnessValuesSkiba.containsKey(currentDatetorneame)) {

            // g(t)
            predictedPerformanceValues[toto] = fitnessValuesSkiba.get(currentDatetorneame);

            //TODO h(t)

            //TODO p(t) = g(t) - h(t)
         }

         currentDatetorneame = currentDatetorneame.plusDays(1);
      }

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.LINE,
            predictedPerformanceValues,
            false);

      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setYTitle("GOVSS");//Messages.App_Label_H_MM);

      yData.setColorIndex(new int[][] { colorIndex });
      yData.setRgbLine(rgbLine);
      yData.setRgbBright(rgbBright);
      yData.setRgbDark(rgbDark);
      yData.setDefaultRGB(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY).getRGB());

      chartDataModel.addYData(yData);

      //TODO when MTB supports displaying both BAR and LINES at the same time
      // set tool tip info
      /*
       * chartDataModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new
       * IChartInfoProvider() {
       * @Override
       * public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
       * return createToolTipInfo(serieIndex, valueIndex);
       * }
       * });
       */

      /*
       * if (_isSynchChartVerticalValues && _isShowAllValues == false) {
       * _minMaxKeeper.setMinMaxValues(chartDataModel);
       * }
       */

      final float[] govssValues = new float[numberOfDays];

      for (final Map.Entry<LocalDate, ArrayList<Long>> entry : govssEntries.entrySet()) {
         final LocalDate currentDate = entry.getKey();
         final int index = (int) ChronoUnit.DAYS.between(_oldestEntryDate, currentDate);
         final ArrayList<Long> tourIds = entry.getValue();

         int totalGovssValue = 0;
         for (final Long tourId : tourIds) {
            final TourData currentTour = TourManager.getTour(tourId);

            if (currentTour == null) {
               continue;
            }

            totalGovssValue += TourManager.getTour(tourId).getGovss();
         }
         if (totalGovssValue > 10000 || totalGovssValue < 0) {
            govssValues[index] = 0;
         } else {
            govssValues[index] = totalGovssValue;
         }

      }

      final ChartDataYSerie dummyPerformanceDataYAxis = new ChartDataYSerie(
            ChartType.LINE,
            govssValues,
            false);

      dummyPerformanceDataYAxis.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setYTitle("GOVSS");//Messages.App_Label_H_MM);

      final RGB[] rgbBright2 = new RGB[numberOfDays];
      final RGB[] rgbDark2 = new RGB[numberOfDays];
      final RGB[] rgbLine2 = new RGB[numberOfDays];

      for (int index = 0; index < numberOfDays; ++index) {

         rgbDark2[index] = new RGB(203, 25, 37);
         rgbBright2[index] = new RGB(203, 25, 37);
         rgbLine2[index] = new RGB(203, 25, 37);
      }

      dummyPerformanceDataYAxis.setColorIndex(new int[][] { colorIndex });
      dummyPerformanceDataYAxis.setRgbLine(rgbLine2);
      dummyPerformanceDataYAxis.setRgbBright(rgbBright2);
      dummyPerformanceDataYAxis.setRgbDark(rgbDark2);
      dummyPerformanceDataYAxis.setDefaultRGB(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY).getRGB());

      chartDataModel.addYData(dummyPerformanceDataYAxis);

      // show the new data data model in the chart
      _chartPerformanceModelingData.updateChart(chartDataModel, false);
      // _chartPerformanceModelingData.up = TourManager.getInstance().getActivePerformanceModelingChartView();
   }

}
