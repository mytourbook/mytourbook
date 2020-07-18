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
package net.tourbook.conconi;

import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Collections;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IChartLayer;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 * Show selected tours in a conconi test chart
 */
public class ConconiView extends ViewPart {

   public static final String  ID                           = "net.tourbook.conconi.ConconiView"; //$NON-NLS-1$

   private static final int    ADJUST_MAX_PULSE_VALUE       = 5;
   private static final int    ADJUST_MAX_POWER_VALUE       = 10;

   private static final String STATE_CONCONI_IS_LOG_SCALING = "STATE_CONCONI_LOG_SCALING";        //$NON-NLS-1$
   private static final String STATE_CONCONI_SCALING_FACTOR = "STATE_CONCONI_SCALING_FACTOR";     //$NON-NLS-1$

   private static final RGB    DEFAULT_RGB                  = new RGB(0xd0, 0xd0, 0xd0);

// SET_FORMATTING_OFF

	private static final String		GRID_PREF_PREFIX							= "GRID_CONCONI__";															//$NON-NLS-1$

	private static final String		GRID_IS_SHOW_VERTICAL_GRIDLINES		= (GRID_PREF_PREFIX	+ ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES);
	private static final String		GRID_IS_SHOW_HORIZONTAL_GRIDLINES	= (GRID_PREF_PREFIX	+ ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);
	private static final String		GRID_VERTICAL_DISTANCE					= (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE);
	private static final String		GRID_HORIZONTAL_DISTANCE				= (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE);

// SET_FORMATTING_ON

   private final IPreferenceStore  _prefStore              = TourbookPlugin.getPrefStore();
   private final IPreferenceStore  _commonPrefStore        = CommonActivator.getPrefStore();
   private final IDialogSettings   _state                  = TourbookPlugin.getState(ID);

   private IPartListener2          _partListener;
   private ISelectionListener      _postSelectionListener;
   private IPropertyChangeListener _prefChangeListener;
   private ITourEventListener      _tourEventListener;

   private ChartDataYSerie         _yDataPulse;
   private ConconiData             _conconiDataForSelectedTour;

   protected boolean               _isUpdateUI             = false;
   private boolean                 _isSelectionDisabled    = true;
   private boolean                 _isSaving;

   private TourData                _selectedTour;

   private int                     _originalTourDeflection = -1;
   private int                     _modifiedTourDeflection = -1;

   private PixelConverter          _pc;
   private FormToolkit             _tk;

   private ActionConconiOptions    _actionConconiOptions;

   private int                     _hintDefaultSpinnerWidth;

   /*
    * UI controls
    */
   private PageBook              _pageBook;
   private Composite             _page_NoTour;
   private Composite             _page_ConconiTest;

   private ArrayList<TourData>   _conconiTours;

   private Chart                 _chartConconiTest;
   private ChartLayerConconiTest _conconiLayer;

   private Combo                 _comboTests;
   private Scale                 _scaleDeflection;
   private Label                 _lblDeflactionPulse;
   private Label                 _lblDeflactionPower;

   private Button                _chkExtendedScaling;
   private Label                 _lblFactor;
   private Spinner               _spinFactor;

   private class ActionConconiOptions extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutConconiOptions(_pageBook, toolbar, GRID_PREF_PREFIX, ConconiView.this);
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

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            /*
             * set a new chart configuration when the preferences has changed
             */
            if (property.equals(GRID_HORIZONTAL_DISTANCE)
                  || property.equals(GRID_VERTICAL_DISTANCE)
                  || property.equals(GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
                  || property.equals(GRID_IS_SHOW_VERTICAL_GRIDLINES)
            //
            ) {

               // grid has changed, update chart
               updateChartProperties();
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   /**
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            if (part == ConconiView.this) {
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

            if (part == ConconiView.this) {
               return;
            }

            if (eventId == TourEventId.TOUR_SELECTION && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      _conconiTours = null;
      _selectedTour = null;

      if (_chartConconiTest != null) {
         _chartConconiTest.updateChart(null, false);
      }

      _pageBook.showPage(_page_NoTour);
   }

   private void createActions() {

      _actionConconiOptions = new ActionConconiOptions();

   }

   /**
    * @param conconiTours
    *           contains all tours which are displayed in the conconi chart, they can be valid or
    *           invalid
    * @param markedTour
    *           contains tour which should be marked in the chart, when <code>null</code> the first
    *           tour will be marked
    * @return
    */
   private ChartDataModel createChartDataModelConconiTest(final ArrayList<TourData> conconiTours, TourData markedTour) {

      // reset data
      _conconiDataForSelectedTour = null;

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.XY_SCATTER);

      final int serieLengthRaw = conconiTours.size();

      final TourData[] toursArray = conconiTours.toArray(new TourData[serieLengthRaw]);
      final ArrayList<TourData> validTourList = new ArrayList<>();

      /*
       * get all tours which has valid data
       */
      for (int serieIndex = 0; serieIndex < serieLengthRaw; serieIndex++) {

         final TourData tourData = toursArray[serieIndex];

         final float[] tdPowerSerie = tourData.getPowerSerie();
         final float[] tdPulseSerie = tourData.pulseSerie;

         // check if required data series are available
         if (tdPowerSerie != null && tdPowerSerie.length != 0 && tdPulseSerie != null && tdPulseSerie.length != 0) {
            validTourList.add(tourData);
         }
      }

      final int validDataLength = validTourList.size();
      final int lastTourIndex = validDataLength - 1;

      // display error when required data are not available
      if (validDataLength == 0) {

         chartDataModel.setErrorMessage(Messages.Conconi_Chart_InvalidData);

         return chartDataModel;
      }

      // ensure a tour is marked
      if (markedTour == null) {
         markedTour = validTourList.get(0);
      }

      _selectedTour = markedTour;

      final String prefGraphName = ICommonPreferences.GRAPH_COLORS + GraphColorManager.PREF_GRAPH_HEARTBEAT + UI.SYMBOL_DOT;

      final RGB rgbPrefLine = PreferenceConverter.getColor(//
            _commonPrefStore,
            prefGraphName + GraphColorManager.PREF_COLOR_LINE);

      final RGB rgbPrefDark = PreferenceConverter.getColor(//
            _commonPrefStore,
            prefGraphName + GraphColorManager.PREF_COLOR_DARK);

      final RGB rgbPrefBright = PreferenceConverter.getColor(//
            _commonPrefStore,
            prefGraphName + GraphColorManager.PREF_COLOR_BRIGHT);

      final double[][] powerSerie = new double[validDataLength][];
      final double[][] pulseSerie = new double[validDataLength][];
      final RGB[] rgbLine = new RGB[validDataLength];
      final RGB[] rgbDark = new RGB[validDataLength];
      final RGB[] rgbBright = new RGB[validDataLength];

      final TourData[] validTours = validTourList.toArray(new TourData[validTourList.size()]);
      int markedIndex = 0;
      float maxXValue = 0;

      /*
       * create data series which contain valid data, reduce data that the highes value for an x
       * value is displayed
       */
      for (int tourIndex = 0; tourIndex < validDataLength; tourIndex++) {

         final TourData tourData = validTours[tourIndex];

         final float[] tourPowerSerie = tourData.getPowerSerie();
         final float[] tourPulseSerie = tourData.pulseSerie;

         // check if required data series are available
         if (tourPowerSerie != null
               && tourPowerSerie.length != 0
               && tourPulseSerie != null
               && tourPulseSerie.length != 0) {

            final TDoubleArrayList maxXValues = new TDoubleArrayList();
            final TDoubleArrayList maxYValues = new TDoubleArrayList();

            float lastMaxY = Float.MIN_VALUE;
            float currentXValue = tourPowerSerie[0];

            // loop: all values in the current serie
            for (int valueIndex = 0; valueIndex < tourPowerSerie.length; valueIndex++) {

               // check array bounds
               if (valueIndex >= tourPulseSerie.length) {
                  break;
               }

               final float xValue = tourPowerSerie[valueIndex];
               final float yValue = tourPulseSerie[valueIndex];

               if (xValue == currentXValue) {

                  // get maximum y value for the same x value

                  if (yValue > lastMaxY) {
                     lastMaxY = yValue;
                  }

               } else {

                  // next x value is displayed, keep last max y

                  maxXValues.add(currentXValue);
                  maxYValues.add(lastMaxY);

                  currentXValue = xValue;
                  lastMaxY = yValue;
               }

               // get max x value
               if (currentXValue > maxXValue) {
                  maxXValue = currentXValue;
               }
            }

            // get last value
            maxXValues.add(currentXValue);
            maxYValues.add(lastMaxY);

            powerSerie[tourIndex] = maxXValues.toArray();
            pulseSerie[tourIndex] = maxYValues.toArray();

            /*
             * marked tour is displayed with pulse color
             */
            if (tourData.equals(markedTour)) {

               // get index of marked tour
               markedIndex = tourIndex;

               rgbLine[tourIndex] = rgbPrefLine;
               rgbDark[tourIndex] = rgbPrefDark;
               rgbBright[tourIndex] = rgbPrefBright;

            } else {

               rgbLine[tourIndex] = DEFAULT_RGB;
               rgbDark[tourIndex] = DEFAULT_RGB;
               rgbBright[tourIndex] = DEFAULT_RGB;
            }
         }
      }

      /*
       * swap last tour with marked tour that the marked tour is painted at last to be not covered
       * by other tours
       */
      final double[] markedPowerSerie = powerSerie[markedIndex];
      final double[] markedPulseSerie = pulseSerie[markedIndex];
      final RGB markedRgbLine = rgbLine[markedIndex];
      final RGB markedRgbDark = rgbDark[markedIndex];
      final RGB markedRgbBright = rgbBright[markedIndex];

      powerSerie[markedIndex] = powerSerie[lastTourIndex];
      pulseSerie[markedIndex] = pulseSerie[lastTourIndex];
      rgbLine[markedIndex] = rgbLine[lastTourIndex];
      rgbDark[markedIndex] = rgbDark[lastTourIndex];
      rgbBright[markedIndex] = rgbBright[lastTourIndex];

      powerSerie[lastTourIndex] = markedPowerSerie;
      pulseSerie[lastTourIndex] = markedPulseSerie;
      rgbLine[lastTourIndex] = markedRgbLine;
      rgbDark[lastTourIndex] = markedRgbDark;
      rgbBright[lastTourIndex] = markedRgbBright;

      /*
       * power
       */
      final ChartDataXSerie xDataPower = new ChartDataXSerie(powerSerie);
      xDataPower.setLabel(net.tourbook.common.Messages.Graph_Label_Power);
      xDataPower.setUnitLabel(net.tourbook.common.Messages.Graph_Label_Power_Unit);

      /*
       * double is not yet supported for the y-axis
       */
      final float[][] pulseSerieFloat = Util.convertDoubleToFloat(pulseSerie);

      /*
       * pulse
       */
      _yDataPulse = new ChartDataYSerie(ChartType.XY_SCATTER, pulseSerieFloat);
      _yDataPulse.setYTitle(net.tourbook.common.Messages.Graph_Label_Heartbeat);
      _yDataPulse.setUnitLabel(net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit);
      _yDataPulse.setDefaultRGB(rgbPrefLine);
      _yDataPulse.setRgbLine(rgbLine);
      _yDataPulse.setRgbDark(rgbDark);
      _yDataPulse.setRgbBright(rgbBright);

// check x-data visible min value
      //adjust min/max values that the chart do not stick to a border
      xDataPower.forceXAxisMinValue(0);
      xDataPower.forceXAxisMaxValue(xDataPower.getVisibleMaxValue() + ADJUST_MAX_POWER_VALUE);

      _yDataPulse.forceYAxisMinValue(_yDataPulse.getVisibleMinValue() - ADJUST_MAX_PULSE_VALUE);
      _yDataPulse.forceYAxisMaxValue(_yDataPulse.getVisibleMaxValue() + ADJUST_MAX_PULSE_VALUE);

      // setup chart data model
      chartDataModel.setXData(xDataPower);
      chartDataModel.addYData(_yDataPulse);

      // create conconi data for the selected tour
      _conconiDataForSelectedTour = createConconiData(powerSerie[lastTourIndex], pulseSerie[lastTourIndex]);

      if (_chkExtendedScaling.getSelection()) {
         xDataPower.setScalingFactors(//
               (double) _spinFactor.getSelection() / 10,
               maxXValue + ADJUST_MAX_POWER_VALUE);
      }

      /*
       * updata layer for regression lines
       */
      final ArrayList<IChartLayer> chartCustomLayers = new ArrayList<>();
      chartCustomLayers.add(_conconiLayer);

      _yDataPulse.setCustomForegroundLayers(chartCustomLayers);
      _yDataPulse.setCustomData(TourManager.CUSTOM_DATA_CONCONI_TEST, _conconiDataForSelectedTour);

      return chartDataModel;
   }

   private ConconiData createConconiData(final double[] powerSerie, final double[] pulseSerie) {

      final TDoubleArrayList maxXValues = new TDoubleArrayList();
      final TDoubleArrayList maxYValues = new TDoubleArrayList();

      double lastMaxY = Double.MIN_VALUE;
      double currentXValue = powerSerie[0];

      // loop: all values in the current serie
      for (int valueIndex = 0; valueIndex < powerSerie.length; valueIndex++) {

         // check array bounds
         if (valueIndex >= pulseSerie.length) {
            break;
         }

         final double xValue = powerSerie[valueIndex];
         final double yValue = pulseSerie[valueIndex];

         if (xValue == currentXValue) {

            // get maximum y value for the same x value

            if (yValue > lastMaxY) {
               lastMaxY = yValue;
            }

         } else {

            // next x value is displayed, keep last max y

            maxXValues.add(currentXValue);
            maxYValues.add(lastMaxY);

            currentXValue = xValue;
            lastMaxY = yValue;
         }
      }

      // get last value
      maxXValues.add(currentXValue);
      maxYValues.add(lastMaxY);

      final ConconiData conconiData = new ConconiData();
      conconiData.maxXValues = maxXValues;
      conconiData.maxYValues = maxYValues;

      return conconiData;
   }

   @Override
   public void createPartControl(final Composite parent) {

      createUI(parent);

      createActions();
      fillToolbar();

      restoreState();
      enableControls();

      addPrefListener();
      addTourEventListener();
      addSelectionListener();
      addPartListener();

      // show conconi chart from selection service
      onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

      // check if tour chart is displayed
      if (_conconiTours == null) {
         showTourFromTourProvider();
      }
   }

   private void createUI(final Composite parent) {

      initUI(parent);

      _pageBook = new PageBook(parent, SWT.NONE);

      _page_NoTour = net.tourbook.ui.UI.createPage(_tk, _pageBook, Messages.UI_Label_TourIsNotSelected);

      createUI_10_ConconiTest(_pageBook);
   }

   private void createUI_10_ConconiTest(final Composite parent) {

      _page_ConconiTest = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_page_ConconiTest);
      GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(_page_ConconiTest);
//		_pageConconiTest.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         createUI_20_Options(_page_ConconiTest);
         createUI_60_ConconiChart(_page_ConconiTest);
      }
   }

   private void createUI_20_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(2).applyTo(container);
//		_page_ConconiTest.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         createUI_30_Test(container);
         createUI_40_ExtendedScaling(container);
      }
   }

   private void createUI_30_Test(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()//
            .numColumns(2)
            .spacing(0, 0)
            .extendedMargins(3, 3, 3, 0)
            .applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         /*
          * label: tour
          */
         Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Conconi_Chart_Label_Tour);
         label.setToolTipText(Messages.Conconi_Chart_Label_Tour_Tooltip);

         /*
          * combo: tour date/time
          */
         _comboTests = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
         GridDataFactory
               .fillDefaults()
               .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
               .grab(true, false)
               .applyTo(_comboTests);
         _comboTests.setVisibleItemCount(20);
         _comboTests.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelectTour();
            }
         });

         /*
          * label: deflaction point
          */
         label = new Label(container, SWT.NONE);
         label.setText(Messages.Conconi_Chart_DeflactionPoint);

         final Composite deflContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(deflContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(deflContainer);
//			deflContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
         {
            /*
             * scale: deflection point
             */
            _scaleDeflection = new Scale(deflContainer, SWT.HORIZONTAL);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleDeflection);
            _scaleDeflection.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectDeflection();
               }
            });

            final Composite containerValues = new Composite(deflContainer, SWT.NONE);
            GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.CENTER).applyTo(containerValues);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerValues);
            //		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
            {
               // label: heartbeat value
               _lblDeflactionPulse = new Label(containerValues, SWT.TRAIL);
               GridDataFactory
                     .fillDefaults()
                     .align(SWT.FILL, SWT.CENTER)
                     .hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
                     .applyTo(_lblDeflactionPulse);

               // label: heartbeat unit
               label = new Label(containerValues, SWT.NONE);
               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
               label.setText(net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit);

               // label: power value
               _lblDeflactionPower = new Label(containerValues, SWT.TRAIL);
               GridDataFactory
                     .fillDefaults()
                     .align(SWT.FILL, SWT.CENTER)
                     .hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
                     .applyTo(_lblDeflactionPower);

               // label: power unit
               label = new Label(containerValues, SWT.NONE);
               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
               label.setText(UI.UNIT_POWER);
            }
         }
      }
   }

   private void createUI_40_ExtendedScaling(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
      {
         /*
          * checkbox: log scaling
          */
         _chkExtendedScaling = new Button(container, SWT.CHECK);
         GridDataFactory//
               .fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .span(2, 1)
               .applyTo(_chkExtendedScaling);
         _chkExtendedScaling.setText(Messages.Conconi_Chart_Chk_LogScaling);
         _chkExtendedScaling.setToolTipText(Messages.Conconi_Chart_Chk_LogScaling_Tooltip);
         _chkExtendedScaling.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               enableControls();
               updateChart_30_NewTour(_selectedTour);
            }
         });

         /*
          * label: factor
          */
         _lblFactor = new Label(container, SWT.NONE);
         GridDataFactory.fillDefaults()//
               .indent(16, 0)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_lblFactor);
         _lblFactor.setText(Messages.Conconi_Chart_Label_ScalingFactor);

         /*
          * spinner: factor
          */
         _spinFactor = new Spinner(container, SWT.BORDER);
         GridDataFactory.fillDefaults()//
               .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_spinFactor);
         _spinFactor.setMinimum(1);
         _spinFactor.setMaximum(100);
         _spinFactor.setDigits(1);

         _spinFactor.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
               if (_isUpdateUI) {
                  return;
               }
               updateChart_30_NewTour(_selectedTour);
            }
         });
         _spinFactor.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               if (_isUpdateUI) {
                  return;
               }
               updateChart_30_NewTour(_selectedTour);
            }
         });
         _spinFactor.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseScrolled(final MouseEvent event) {
               Util.adjustSpinnerValueOnMouseScroll(event);
               if (_isUpdateUI) {
                  return;
               }
               updateChart_30_NewTour(_selectedTour);
            }
         });

      }
   }

   /**
    * chart: conconi test
    */
   private void createUI_60_ConconiChart(final Composite parent) {

      _chartConconiTest = new Chart(parent, SWT.FLAT);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartConconiTest);

      // set chart properties
      updateChartProperties();

      _conconiLayer = new ChartLayerConconiTest();
   }

   @Override
   public void dispose() {

      getViewSite().getPage().removePartListener(_partListener);
      getSite().getPage().removeSelectionListener(_postSelectionListener);
      TourManager.getInstance().removeTourEventListener(_tourEventListener);
      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void enableControls() {

      final boolean isExtScaling = _chkExtendedScaling.getSelection();

      _lblFactor.setEnabled(isExtScaling);
      _spinFactor.setEnabled(isExtScaling);
   }

   private void fillToolbar() {

      /*
       * View toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
      tbm.add(_actionConconiOptions);

      // update toolbar which creates the slideout
      tbm.update(true);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
      _tk = new FormToolkit(parent.getDisplay());

      _hintDefaultSpinnerWidth = UI.IS_LINUX ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(UI.IS_OSX ? 10 : 5);
   }

   private void onSelectDeflection() {

      // update conconi data
      final int newDeflection = _scaleDeflection.getSelection();
      _conconiDataForSelectedTour.selectedDeflection = newDeflection;
      _yDataPulse.setCustomData(TourManager.CUSTOM_DATA_CONCONI_TEST, _conconiDataForSelectedTour);

      _modifiedTourDeflection = newDeflection;

      updateUI_20_ConconiValues();
   }

   private void onSelectionChanged(final ISelection selection) {

      if (_isSaving) {
         return;
      }

      if (_pageBook != null && _pageBook.isDisposed()) {
         return;
      }

      if (selection instanceof SelectionTourData) {

         final TourData tourData = ((SelectionTourData) selection).getTourData();
         if (tourData != null) {
            updateChart_20(tourData);
         }

      } else if (selection instanceof SelectionTourIds) {

         final SelectionTourIds selectionTourId = (SelectionTourIds) selection;
         final ArrayList<Long> tourIds = selectionTourId.getTourIds();
         if (tourIds != null && tourIds.size() > 0) {
            updateChart_12(tourIds);
         }

      } else if (selection instanceof SelectionTourId) {

         final SelectionTourId selectionTourId = (SelectionTourId) selection;
         final Long tourId = selectionTourId.getTourId();

         updateChart_10(tourId);

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
      }
   }

   private void onSelectTour() {

      if (_isSelectionDisabled) {
         return;
      }

      int selectedIndex = _comboTests.getSelectionIndex();
      if (selectedIndex == -1) {
         selectedIndex = 0;
      }

      updateChart_30_NewTour(_conconiTours.get(selectedIndex));
   }

   private void restoreState() {

      _isUpdateUI = true;
      {
         _chkExtendedScaling.setSelection(_state.getBoolean(STATE_CONCONI_IS_LOG_SCALING));
         _spinFactor.setSelection(Util.getStateInt(_state, STATE_CONCONI_SCALING_FACTOR, 20));
      }
      _isUpdateUI = false;
   }

   @PersistState
   private void saveState() {

      // check if UI is disposed
      if (_pageBook.isDisposed()) {
         return;
      }

      saveTour();

      _state.put(STATE_CONCONI_IS_LOG_SCALING, _chkExtendedScaling.getSelection());
      _state.put(STATE_CONCONI_SCALING_FACTOR, _spinFactor.getSelection());
   }

   private void saveTour() {

      if (_originalTourDeflection == _modifiedTourDeflection) {
         return;
      }

      _selectedTour.setConconiDeflection(_modifiedTourDeflection);

      TourData savedTour;
      _isSaving = true;
      {
         savedTour = TourManager.saveModifiedTour(_selectedTour);
      }
      _isSaving = false;

      if (savedTour != null) {

         _selectedTour = savedTour;

         // replace tour with saved tour
         final TourData[] conconiTours = _conconiTours.toArray(new TourData[_conconiTours.size()]);
         for (int tourIndex = 0; tourIndex < conconiTours.length; tourIndex++) {
            if (conconiTours[tourIndex].equals(savedTour)) {
               _conconiTours.set(tourIndex, savedTour);
               break;
            }
         }
      }

      _originalTourDeflection = _modifiedTourDeflection = -1;
   }

   @Override
   public void setFocus() {

      if (_page_ConconiTest != null && _page_ConconiTest.isVisible()) {
         _chartConconiTest.setFocus();
      }
   }

   private void showTourFromTourProvider() {

      _pageBook.showPage(_page_NoTour);

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
            if (_conconiTours != null) {
               return;
            }

            final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
            if (selectedTours != null && selectedTours.size() > 0) {
               updateChart_22(selectedTours);
            }
         }
      });
   }

   private void updateChart_10(final Long tourId) {

      final ArrayList<Long> tourIds = new ArrayList<>();
      tourIds.add(tourId);

      updateChart_12(tourIds);
   }

   private void updateChart_12(final ArrayList<Long> tourIds) {
      updateChart_22(TourManager.getInstance().getTourData(tourIds));
   }

   private void updateChart_20(final TourData tourData) {

      if (tourData == null) {
         return;
      }

      final ArrayList<TourData> tourDataList = new ArrayList<>();
      tourDataList.add(tourData);

      updateChart_22(tourDataList);
   }

   private void updateChart_22(final ArrayList<TourData> tourDataList) {

      /*
       * tour editor is not opened because it can cause a recursive attempt to active a part in the
       * middle of activating a part
       */
      if (tourDataList == null || tourDataList.size() == 0 || TourManager.isTourEditorModified(false)) {
         // nothing to do
         clearView();
         return;
      }

      // sort tours by date/time
      Collections.sort(tourDataList);

      _conconiTours = tourDataList;

      updateUI_10_SetupConconi();
      updateChart_30_NewTour(null);

      _pageBook.showPage(_page_ConconiTest);

      return;
   }

   /**
    * @param markedTour
    *           contains a tour which is marked in the conconi chart
    */
   private void updateChart_30_NewTour(final TourData markedTour) {

      // save modified tour before chart data for a new tour is created
      saveTour();

      if (_conconiTours == null) {
         // bugfix for: http://sourceforge.net/tracker/?func=detail&atid=890601&aid=3269916&group_id=179799
         _pageBook.showPage(_page_NoTour);
         return;
      }

      final ChartDataModel conconiChartDataModel = createChartDataModelConconiTest(_conconiTours, markedTour);

      updateUI_12_SetupNewTour();

      _chartConconiTest.updateChart(conconiChartDataModel, true, true);

      /*
       * force the chart to be repainted because updating the conconi layer requires that the chart
       * is already painted (it requires drawing data)
       */
      _chartConconiTest.resizeChart();
      updateUI_20_ConconiValues();
   }

   private void updateChartProperties() {

      net.tourbook.ui.UI.updateChartProperties(_chartConconiTest, GRID_PREF_PREFIX);
   }

   private void updateUI_10_SetupConconi() {

      _isSelectionDisabled = true;
      {
         /*
          * tour combo box
          */
         _comboTests.removeAll();

         for (final TourData tourData : _conconiTours) {
            _comboTests.add(TourManager.getTourTitleDetailed(tourData));
         }

         _comboTests.select(0);
         _comboTests.setEnabled(_conconiTours.size() > 1);
      }
      _isSelectionDisabled = false;
   }

   private void updateUI_12_SetupNewTour() {

      if (_conconiDataForSelectedTour == null) {
         _scaleDeflection.setEnabled(false);
         return;
      }

      // update deflection scale
      final int maxDeflection = _conconiDataForSelectedTour.maxXValues.size();
      final int lastXIndex = maxDeflection - 1;
      _scaleDeflection.setEnabled(true);
      _scaleDeflection.setMaximum(maxDeflection > 0 ? lastXIndex : 0);

      // ensure that too much scale ticks are displayed
      final int pageIncrement = maxDeflection < 20 ? 1 : maxDeflection < 100 ? 5 : maxDeflection < 1000 ? 50 : 100;

      _scaleDeflection.setPageIncrement(pageIncrement);

      int tourDeflection = _selectedTour.getConconiDeflection();

      // ensure deflection is within x-values
      if (tourDeflection > lastXIndex) {
         tourDeflection = lastXIndex;
      }
      if (tourDeflection < 0) {
         tourDeflection = 0;
      }

      _originalTourDeflection = _modifiedTourDeflection = tourDeflection;

      // update conconi data
      _conconiDataForSelectedTour.selectedDeflection = tourDeflection;

      // update UI
      _scaleDeflection.setSelection(tourDeflection);

   }

   private void updateUI_20_ConconiValues() {

      if (_conconiDataForSelectedTour == null) {
         return;
      }

      // deflation values
      final int scaleIndex = _scaleDeflection.getSelection();
      final int pulseValue = (int) _conconiDataForSelectedTour.maxYValues.get(scaleIndex);
      final int powerValue = (int) _conconiDataForSelectedTour.maxXValues.get(scaleIndex);

      _lblDeflactionPulse.setText(Integer.toString(pulseValue));
      _lblDeflactionPower.setText(Integer.toString(powerValue));

      // update conconi layer
      _chartConconiTest.updateCustomLayers();
   }

}
