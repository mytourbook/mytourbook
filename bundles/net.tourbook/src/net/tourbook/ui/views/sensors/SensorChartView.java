/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.sensors;

import java.util.ArrayList;
import java.util.Arrays;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.DelayedBarSelection_TourToolTip;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.MouseWheelMode;
import net.tourbook.chart.SelectionBarChart;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.DeviceSensor;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.TourTypeColorDefinition;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoIconToolTipProvider;
import net.tourbook.tour.TourInfoUI;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 * Shows battery values of a sensor in a chart
 */
public class SensorChartView extends ViewPart implements ITourProvider {

   public static final String  ID                            = "net.tourbook.ui.views.sensors.SensorChartView.ID"; //$NON-NLS-1$

   private static final String STATE_IS_SELECTED_TOUR_FILTER = "STATE_IS_SELECTED_TOUR_FILTER";                    //$NON-NLS-1$
   private static final String STATE_MOUSE_WHEEL_MODE        = "STATE_MOUSE_WHEEL_MODE";                           //$NON-NLS-1$
   private static final String STATE_SELECTED_TOUR_ID        = "STATE_SELECTED_TOUR_ID";                           //$NON-NLS-1$

// SET_FORMATTING_OFF

   private static final String      GRID_PREF_PREFIX                    = "GRID_SENSOR_CHART__";                                            //$NON-NLS-1$

   private static final String      GRID_IS_SHOW_VERTICAL_GRIDLINES     = (GRID_PREF_PREFIX  + ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES);
   private static final String      GRID_IS_SHOW_HORIZONTAL_GRIDLINES   = (GRID_PREF_PREFIX  + ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);
   private static final String      GRID_VERTICAL_DISTANCE              = (GRID_PREF_PREFIX  + ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE);
   private static final String      GRID_HORIZONTAL_DISTANCE            = (GRID_PREF_PREFIX  + ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE);

// SET_FORMATTING_ON

   private final IPreferenceStore          _prefStore               = TourbookPlugin.getPrefStore();
   private final IDialogSettings           _state                   = TourbookPlugin.getState(ID);

   private IPartListener2                  _partListener;
   private IPropertyChangeListener         _prefChangeListener;
   private ITourEventListener              _tourEventListener;

   private FormToolkit                     _tk;

   private boolean                         _isChartDisplayed;

   private DeviceSensor                    _selectedSensor;
   private SensorData                      _sensorData;
   private SensorDataProvider              _sensorDataProvider      = new SensorDataProvider();

   private Long                            _selectedTourId;

   private DelayedBarSelection_TourToolTip _tourToolTip;
   private TourInfoIconToolTipProvider     _tourInfoToolTipProvider = new TourInfoIconToolTipProvider();
   private TourInfoUI                      _tourInfoUI              = new TourInfoUI();

   private boolean                         _useTourFilter;
   private boolean                         _isInSelect;

   private OpenDialogManager               _openDlgMgr              = new OpenDialogManager();

   private ActionChartOptions              _actionChartOptions;
   private ActionTourFilter                _actionTourFilterOptions;

   /*
    * UI controls
    */
   private PageBook  _pageBook;

   private Composite _pageNoData;
   private Composite _pageNoBatteryData;

   private Chart     _sensorChart;

   private Composite _parent;

   private class ActionChartOptions extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutSensorChartOptions(_parent, toolbar, SensorChartView.this, _state, GRID_PREF_PREFIX);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ActionTourFilter extends ActionToolbarSlideout {

      public ActionTourFilter() {

         super(CommonActivator.getThemedImageDescriptor(CommonImages.App_Filter),
               CommonActivator.getThemedImageDescriptor(CommonImages.App_Filter_Disabled));

         isToggleAction = true;
         notSelectedTooltip = Messages.Sensor_Chart_Action_TourQuickFilter_Tooltip;
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutSensorTourFilter(_parent, toolbar, SensorChartView.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {

         super.onSelect();

         onAction_TourFilter(getSelection());
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

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         /*
          * Create a new chart configuration when the colors has changed
          */
         if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)
               || property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

            updateChart();

         } else if (property.equals(GRID_HORIZONTAL_DISTANCE)
               || property.equals(GRID_VERTICAL_DISTANCE)
               || property.equals(GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
               || property.equals(GRID_IS_SHOW_VERTICAL_GRIDLINES)) {

            // grid has changed, update chart
            updateChartProperties();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (workbenchPart == SensorChartView.this) {
            return;
         }

         if (tourEventId == TourEventId.SELECTION_SENSOR && eventData instanceof SelectionSensor) {

            onSelectionChanged((SelectionSensor) eventData);

         } else if (tourEventId == TourEventId.TOUR_CHANGED) {

            // tour type could be modified

            updateChart();
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   /**
    * Close all opened dialogs except the opening dialog.
    *
    * @param openingDialog
    */
   public void closeOpenedDialogs(final IOpeningDialog openingDialog) {

      _openDlgMgr.closeOpenedDialogs(openingDialog);
   }

   private void createActions() {

      _sensorChart.createChartActions();

      _actionTourFilterOptions = new ActionTourFilter();
      _actionChartOptions = new ActionChartOptions();

      fillToolbar();
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);

      createUI(parent);
      createActions();

      addPartListener();
      addPrefListener();
      addTourEventListener();

      // restore must be run async otherwise the filter slideout action is not selected !
      parent.getShell().getDisplay().asyncExec(() -> {

         restoreState();
         enableActions();
      });
   }

   /**
    * @param toolTipProvider
    * @param parent
    * @param hoveredBar_VerticalIndex
    *           serieIndex
    * @param hoveredBar_HorizontalIndex
    *           valueIndex
    */
   private void createToolTipUI(final IToolTipProvider toolTipProvider,
                                final Composite parent,
                                final int serieIndex,
                                final int valueIndex) {

      final long tourId = _sensorData.allTourIds[valueIndex];

      TourData _tourData = null;
      if (tourId != -1) {

         // first get data from the tour id when it is set
         _tourData = TourManager.getInstance().getTourData(tourId);
      }

      if (_tourData == null) {

         // there are no data available

         _tourInfoUI.createUI_NoData(parent);

      } else {

         // tour data is available

         _tourInfoUI.createContentArea(parent, _tourData, toolTipProvider, this);

         _tourInfoUI.setActionsEnabled(true);
      }

      parent.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            _tourInfoUI.dispose();
         }
      });
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = net.tourbook.ui.UI.createPage(_tk, _pageBook, Messages.Sensor_Chart_Label_SensorIsNotSelected);
      _pageNoBatteryData = net.tourbook.ui.UI.createPage(_tk, _pageBook, Messages.Sensor_Chart_Label_SensorWithBatteryValuesIsNotSelected);

      _sensorChart = createUI_10_Chart();

      _pageBook.showPage(_pageNoData);
   }

   private Chart createUI_10_Chart() {

      final Chart sensorChart = new Chart(_pageBook, SWT.FLAT);

      sensorChart.setShowZoomActions(true);
      sensorChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);

      sensorChart.addBarSelectionListener((serieIndex, valueIndex) -> {

         if (_isInSelect) {
            return;
         }

         final long[] tourIds = _sensorData.allTourIds;

         if (tourIds != null && tourIds.length > 0) {

            if (valueIndex >= tourIds.length) {
               valueIndex = tourIds.length - 1;
            }

            _selectedTourId = tourIds[valueIndex];
            _tourInfoToolTipProvider.setTourId(_selectedTourId);

            // this view can be inactive -> selection is not fired with the SelectionProvider interface
            TourManager.fireEventWithCustomData(
                  TourEventId.TOUR_SELECTION,
                  new SelectionTourId(_selectedTourId),
                  getViewSite().getPart());
         }
      });

      /*
       * Set tour info icon into the left axis
       */
      _tourToolTip = new DelayedBarSelection_TourToolTip(sensorChart.getToolTipControl());
      _tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);

      // hide hovered image
      _tourToolTip.addHideListener(event -> sensorChart.getToolTipControl().afterHideToolTip());

      sensorChart.setTourInfoIconToolTipProvider(_tourInfoToolTipProvider);
      _tourInfoToolTipProvider.setActionsEnabled(true);

      // set chart properties
      updateChartProperties();

      return sensorChart;
   }

   @Override
   public void dispose() {

      saveState();

      if (_tk != null) {
         _tk.dispose();
      }

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      getViewSite().getPage().removePartListener(_partListener);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      super.dispose();
   }

   private void enableActions() {

      _sensorChart.getAction_MouseWheelMode().setEnabled(_isChartDisplayed);
      _sensorChart.setZoomActionsEnabled(_isChartDisplayed);
   }

   /*
    * Fill view toolbar
    */
   private void fillToolbar() {

      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_sensorChart.getAction_MouseWheelMode());
      tbm.add(_actionTourFilterOptions);
      tbm.add(_actionChartOptions);

      // update that actions are fully created otherwise action enable will fail
      tbm.update(true);
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      if (_selectedTourId == null) {
         return null;
      }

      final ArrayList<TourData> selectedTours = new ArrayList<>();

      selectedTours.add(TourManager.getInstance().getTourData(_selectedTourId));

      return selectedTours;
   }

   Shell getShell() {
      return _parent.getShell();
   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _tk = new FormToolkit(parent.getDisplay());
   }

   private void onAction_TourFilter(final boolean isSelected) {

      _useTourFilter = isSelected;

      updateChart();
   }

   private void onSelectionChanged(final ISelection selection) {

      if (selection instanceof SelectionSensor) {

         final SelectionSensor sensorSelection = (SelectionSensor) selection;

         _selectedSensor = sensorSelection.getSensor();

         // prevent reselection
         _isInSelect = true;
         {
            // 1. show sensor
            updateChart();

            // 2. select tour
            final Long tourId = sensorSelection.getTourId();
            if (tourId != null) {

               _selectedTourId = tourId;
               selectTour(_selectedTourId);
            }
         }
         _isInSelect = false;
      }
   }

   private void restoreState() {

      _selectedTourId = Util.getStateLong(_state, STATE_SELECTED_TOUR_ID, -1);
      _useTourFilter = Util.getStateBoolean(_state, STATE_IS_SELECTED_TOUR_FILTER, false);

      _actionTourFilterOptions.setSelection(_useTourFilter);

      /*
       * Select tour even when tour ID == -1 because this will set the selected bar selection flags
       */
      selectTour(_selectedTourId);

      /*
       * Set mouse wheel mode
       */
      final Enum<MouseWheelMode> mouseWheelMode = Util.getStateEnum(_state, STATE_MOUSE_WHEEL_MODE, MouseWheelMode.Selection);
      _sensorChart.setMouseWheelMode((MouseWheelMode) mouseWheelMode);
      _sensorChart.getAction_MouseWheelMode().setMouseWheelMode((MouseWheelMode) mouseWheelMode);
   }

   private void saveState() {

      final ISelection selection = _sensorChart.getSelection();

      if (_sensorData != null
            && _sensorData.allTourIds != null
            && _sensorData.allTourIds.length > 0
            && selection instanceof SelectionBarChart) {

         final long selectedTourId = _sensorData.allTourIds[((SelectionBarChart) selection).valueIndex];

         _state.put(STATE_SELECTED_TOUR_ID, Long.toString(selectedTourId));
      }

      // mouse wheel mode
      final MouseWheelMode mouseWheelMode = _sensorChart.getAction_MouseWheelMode().getMouseWheelMode();
      Util.setStateEnum(_state, STATE_MOUSE_WHEEL_MODE, mouseWheelMode);

      _state.put(STATE_IS_SELECTED_TOUR_FILTER, _actionTourFilterOptions.getSelection());
   }

   /**
    * @param tourId
    *           Tour ID to select, can also be an invalid value, then the first tour is selected
    * @return
    */
   private boolean selectTour(final Long tourId) {

      if (_sensorData == null
            || tourId == null) {
         return false;
      }

      final long[] allTourIds = _sensorData.allTourIds;
      final int numTours = allTourIds.length;

      if (numTours == 0) {

         _selectedTourId = null;
         _tourInfoToolTipProvider.setTourId(-1);

         return false;
      }

      final boolean[] allSelectedBars = new boolean[numTours];

      boolean isTourSelected = false;

      // find the tour which has the same tourId as the selected tour
      for (int tourIndex = 0; tourIndex < numTours; tourIndex++) {

         if (allTourIds[tourIndex] == tourId) {

            allSelectedBars[tourIndex] = true;
            isTourSelected = true;

            _selectedTourId = tourId;
            _tourInfoToolTipProvider.setTourId(_selectedTourId);

            break;
         }
      }

      if (isTourSelected == false) {

         // select first tour

         allSelectedBars[0] = true;

         _selectedTourId = allTourIds[0];
         _tourInfoToolTipProvider.setTourId(_selectedTourId);
      }

      _sensorChart.setSelectedBars(allSelectedBars);

      return isTourSelected;
   }

   private void setChartProviders(final ChartDataModel chartModel) {

      final IChartInfoProvider chartInfoProvider = new IChartInfoProvider() {

         @Override
         public void createToolTipUI(final IToolTipProvider toolTipProvider, final Composite parent, final int serieIndex, final int valueIndex) {
            SensorChartView.this.createToolTipUI(toolTipProvider, parent, serieIndex, valueIndex);
         }
      };

      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, chartInfoProvider);

      // set the menu context provider
      chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER, new SensorChartContextProvider(_sensorChart, this));
   }

   @Override
   public void setFocus() {

      _sensorChart.setFocus();
   }

   private void setTourTypeColors(final ChartDataYSerie yData, final String graphName) {

      TourManager.setGraphColors(yData, graphName);

      /*
       * Set tour type colors
       */
      final ArrayList<RGB> rgbGradient_Bright = new ArrayList<>();
      final ArrayList<RGB> rgbGradient_Dark = new ArrayList<>();
      final ArrayList<RGB> rgbLine = new ArrayList<>();

      final ArrayList<TourType> allTourTypes = TourDatabase.getAllTourTypes();

      if (allTourTypes.size() == 0) {

         /**
          * Tour types are not available
          * <p>
          * -> set tour type colors otherwise an exception is thrown when painting the bar graphs
          */

         rgbGradient_Bright.add(TourTypeColorDefinition.DEFAULT_GRADIENT_BRIGHT);
         rgbGradient_Dark.add(TourTypeColorDefinition.DEFAULT_GRADIENT_DARK);

         rgbLine.add(TourTypeColorDefinition.DEFAULT_LINE_COLOR);

      } else {

         // tour types are available

         for (final TourType tourType : allTourTypes) {

            rgbGradient_Bright.add(tourType.getRGB_Gradient_Bright());
            rgbGradient_Dark.add(tourType.getRGB_Gradient_Dark());

            rgbLine.add(tourType.getRGB_Line_Themed());
         }
      }

      // put the colors into the chart data
      yData.setRgbBar_Gradient_Bright(rgbGradient_Bright.toArray(new RGB[rgbGradient_Bright.size()]));
      yData.setRgbBar_Gradient_Dark(rgbGradient_Dark.toArray(new RGB[rgbGradient_Dark.size()]));
      yData.setRgbBar_Line(rgbLine.toArray(new RGB[rgbLine.size()]));
   }

   private void setupColors(final SensorData sensorData, final ChartDataYSerie yData) {

      yData.setColorIndex(new int[][] { sensorData.allTypeColorIndices });

      setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_SENSOR);
   }

   void updateChart() {

      if (_selectedSensor == null) {

         enableActions();

         return;
      }

      _sensorData = _sensorDataProvider.getData(_selectedSensor.getSensorId(), _useTourFilter, _state);

      if (_sensorData.allTourIds.length == 0) {

         _isChartDisplayed = false;

         _pageBook.showPage(_pageNoBatteryData);

      } else {

         _isChartDisplayed = true;

         _pageBook.showPage(_sensorChart);
         updateChart(_sensorData);
      }

      enableActions();
   }

   private void updateChart(final SensorData sensorData) {

// SET_FORMATTING_OFF

      final boolean isShowBatteryLevel    = Util.getStateBoolean(_state, SlideoutSensorChartOptions.STATE_IS_SHOW_BATTERY_LEVEL,    SlideoutSensorChartOptions.STATE_IS_SHOW_BATTERY_LEVEL_DEFAULT);
      final boolean isShowBatteryStatus   = Util.getStateBoolean(_state, SlideoutSensorChartOptions.STATE_IS_SHOW_BATTERY_STATUS,   SlideoutSensorChartOptions.STATE_IS_SHOW_BATTERY_STATUS_DEFAULT);
      final boolean isShowBatteryVoltage  = Util.getStateBoolean(_state, SlideoutSensorChartOptions.STATE_IS_SHOW_BATTERY_VOLTAGE,  SlideoutSensorChartOptions.STATE_IS_SHOW_BATTERY_VOLTAGE_DEFAULT);

// SET_FORMATTING_ON

      /*
       * Create sensor colors
       */
      final GraphColorManager colorMgr = GraphColorManager.getInstance();

      final RGB rgbLine = colorMgr.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_SENSOR).getLineColor_Active_Themed();
      final RGB rgbGradientBright = colorMgr.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_SENSOR).getGradientBright_Active();
      final RGB rgbGradientDark = colorMgr.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_SENSOR).getGradientDark_Active();

      final int[] allXValues_ByTime = sensorData.allXValues_ByTime;
      final int numValues = allXValues_ByTime.length;

      final RGB[] allRGBLine = new RGB[numValues];
      final RGB[] allRGBGradientBright = new RGB[numValues];
      final RGB[] allRGBGradientDark = new RGB[numValues];

      Arrays.fill(allRGBLine, rgbLine);
      Arrays.fill(allRGBGradientBright, rgbGradientBright);
      Arrays.fill(allRGBGradientDark, rgbGradientDark);

      final ChartDataModel chartModel = new ChartDataModel(ChartType.BAR);

      /*
       * Set x-axis values
       */
      final double[] allXValues_Double = Util.convertIntToDouble(allXValues_ByTime);

      final ChartDataXSerie xData = new ChartDataXSerie(allXValues_Double);

      xData.setAxisUnit(ChartDataSerie.X_AXIS_UNIT_HISTORY);
      xData.setHistoryStartDateTime(sensorData.firstDateTime);

      chartModel.setXData(xData);

      final String sensorLabel = _selectedSensor.getLabel();

      /*
       * Set y-axis values: Level
       */
      if (isShowBatteryLevel && sensorData.isAvailable_Level) {

         final ChartDataYSerie yDataLevel = new ChartDataYSerie(
               ChartType.BAR,
               sensorData.allBatteryLevel_End,
               sensorData.allBatteryLevel_Start,
               true);

         yDataLevel.setYTitle(String.format(Messages.Sensor_Chart_GraphLabel_BatteryLevel, sensorLabel));
         yDataLevel.setUnitLabel(UI.SYMBOL_PERCENTAGE);
         yDataLevel.setShowYSlider(true);

         setupColors(sensorData, yDataLevel);

         chartModel.addYData(yDataLevel);
      }

      /*
       * Set y-axis values: Voltage
       */
      if (isShowBatteryVoltage && sensorData.isAvailable_Voltage) {

         final ChartDataYSerie yDataVoltage = new ChartDataYSerie(
               ChartType.BAR,
               sensorData.allBatteryVoltage_End,
               sensorData.allBatteryVoltage_Start,
               true);

         yDataVoltage.setYTitle(String.format(Messages.Sensor_Chart_GraphLabel_BatteryVoltage, sensorLabel));
         yDataVoltage.setUnitLabel(UI.UNIT_VOLTAGE);
         yDataVoltage.setShowYSlider(true);
         yDataVoltage.setSetMinMax_0Values(true);

         setupColors(sensorData, yDataVoltage);

         chartModel.addYData(yDataVoltage);
      }

      /*
       * Set y-axis values: Status
       */
      if (isShowBatteryStatus && sensorData.isAvailable_Status) {

         final ChartDataYSerie yDataStatus = new ChartDataYSerie(
               ChartType.BAR,
               sensorData.allBatteryStatus_End,
               sensorData.allBatteryStatus_Start,
               true);

         yDataStatus.setYTitle(String.format(Messages.Sensor_Chart_GraphLabel_BatteryStatus, sensorLabel));
         yDataStatus.setUnitLabel(UI.SYMBOL_NUMBER_SIGN);
         yDataStatus.setShowYSlider(true);

         setupColors(sensorData, yDataStatus);

         chartModel.addYData(yDataStatus);
      }

      /*
       * Setup other properties
       */

      // set dummy title that the history labels are not truncated
      chartModel.setTitle(UI.SPACE1);

      // because the first and last values are dummy values, skip them when navigated
      chartModel.setSkipNavigationForFirstLastValues(true);

      setChartProviders(chartModel);

      // show the data in the chart
      _sensorChart.updateChart(chartModel, false, true);

      // try to select the previously selected tour
      selectTour(_selectedTourId);
   }

   private void updateChartProperties() {

      net.tourbook.ui.UI.updateChartProperties(_sensorChart, GRID_PREF_PREFIX);
   }
}
