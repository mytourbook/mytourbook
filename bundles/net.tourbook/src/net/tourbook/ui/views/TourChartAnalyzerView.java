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
package net.tourbook.ui.views;

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartDrawingData;
import net.tourbook.chart.ColorCache;
import net.tourbook.chart.ComputeChartValue;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.chart.Util;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartView;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourChartAnalyzerView extends ViewPart {

   public static final String          ID               = "net.tourbook.views.TourChartAnalyzer"; //$NON-NLS-1$

   private static final int            LAYOUT_1_COLUMNS = 0;
   private static final int            LAYOUT_2_COLUMNS = 1;
   private static final int            LAYOUT_3_COLUMNS = 2;
   private static final int            LAYOUT_6_COLUMNS = 3;

   private final IPreferenceStore      _prefStore       = TourbookPlugin.getPrefStore();

   private IPartListener2              _partListener;
   private ISelectionListener          _postSelectionListener;
   private IPropertyChangeListener     _prefChangeListener;
   private ITourEventListener          _tourEventListener;

   private ChartDataModel              _chartDataModel;
   private ChartDrawingData            _chartDrawingData;
   private ArrayList<GraphDrawingData> _graphDrawingData;

   private final ArrayList<GraphInfo>  _graphInfos      = new ArrayList<>();

   private final ColorCache            _colorCache      = new ColorCache();

   private SelectionChartInfo          _chartInfo;

   private int                         _layoutFormat;

   private int                         _valueIndexLeftBackup;
   private int                         _valueIndexRightBackup;

   /**
    * space between columns
    */
   int                                 _columnSpacing   = 1;

   private boolean                     _isPartVisible   = false;

   private PixelConverter              _pc;

   private int                         _valueIndexRightLast;
   private int                         _valueIndexLeftLast;

   private long                        _lastUpdateUITime;
   private int[]                       _updateCounter   = new int[] { 0 };

   /*
    * UI controls
    */
   private Color             _bgColorHeader;
   private Font              _fontBold;

   private PageBook          _pageBook;

   private Composite         _innerScContainer;
   private Composite         _partContainer;
   private Composite         _pageAnalyzer;
   private Composite         _pageNoData;

   private ScrolledComposite _scrolledContainer;

   public TourChartAnalyzerView() {
      super();
   }

   private void addListeners() {

      final IWorkbenchPage page = getSite().getPage();

      _partContainer.addControlListener(controlResizedAdapter(controlEvent -> onResizeUI()));

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
            onSelectionChanged(selection);
         }
      };
      page.addPostSelectionListener(_postSelectionListener);
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) instanceof TourChartView) {

               // TOUR CHART is closed, hide tour chart analyzer data
               clearView();
            }
         }

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {
            if (partRef.getPart(false) == TourChartAnalyzerView.this) {
               _isPartVisible = false;
            }
         }

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {

            /**
             * partVisible event is NOT fired when the app is opened even when the part is
             * displayed, this is a workaround.
             */
            if (partRef.getPart(false) == TourChartAnalyzerView.this) {
               _isPartVisible = true;
            }
         }

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {
            if (partRef.getPart(false) == TourChartAnalyzerView.this) {
               _isPartVisible = true;
            }
         }
      };

      getSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListeners() {

      _prefChangeListener = event -> {

         final String property = event.getProperty();

         if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

            // dispose old colors
            _colorCache.dispose();

            // force a redraw
            _valueIndexLeftLast = -1;

            updateInfo(_chartInfo, false);
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == TourChartAnalyzerView.this) {
               return;
            }

            if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      _chartInfo = null;

      if (_pageBook != null && _pageBook.isDisposed() == false) {
         _pageBook.showPage(_pageNoData);
      }
   }

   @Override
   public void createPartControl(final Composite parent) {

      createUI(parent);

      addPartListener();
      addListeners();
      addPrefListeners();
      addTourEventListener();

      _pageBook.showPage(_pageNoData);

      showTour();
   }

   /**
    * @param parent
    */
   private void createUI(final Composite parent) {

      initUI(parent);

      _partContainer = parent;

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = UI.createUI_PageNoData(_pageBook, Messages.TourAnalyzer_Label_NoTourOrChart);

      _pageAnalyzer = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_pageAnalyzer);
//    _pageAnalyzer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
//		_pageAnalyzer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
   }

   /**
    * This UI can only be created when {@link #_chartDataModel} is set.
    *
    * @param isForceRecreate
    */
   private void createUI_10_Analyzer(final boolean isForceRecreate) {

      // get layout format (number of columns)
      final int clientWidth = _partContainer.getClientArea().width;
      final int layoutFormat = clientWidth < _pc.convertHorizontalDLUsToPixels(100)
            //
            ? LAYOUT_1_COLUMNS
            : clientWidth < _pc.convertHorizontalDLUsToPixels(150)
                  //
                  ? LAYOUT_2_COLUMNS
                  : clientWidth < _pc.convertHorizontalDLUsToPixels(300)
                        //
                        ? LAYOUT_3_COLUMNS
                        : LAYOUT_6_COLUMNS;

      final int numColumns = layoutFormat == LAYOUT_1_COLUMNS //
            ? 2
            : layoutFormat == LAYOUT_2_COLUMNS //
                  ? 3
                  : layoutFormat == LAYOUT_3_COLUMNS ? 4 : 8;

      // check if UI needs to be created
      if (isForceRecreate //

            // check if layout has changed
            || layoutFormat != _layoutFormat) {

         _layoutFormat = layoutFormat;

         createUI_20_CreateAnalyzer(numColumns);

         _pageBook.showPage(_pageAnalyzer);
      }
   }

   private void createUI_20_CreateAnalyzer(final int numColumns) {

      // recreate the UI
      if (_scrolledContainer != null) {
         _scrolledContainer.dispose();
         _scrolledContainer = null;
      }

      // create scrolled container
      _scrolledContainer = new ScrolledComposite(_pageAnalyzer, SWT.V_SCROLL | SWT.H_SCROLL);
      _scrolledContainer.setExpandVertical(true);
      _scrolledContainer.setExpandHorizontal(true);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_scrolledContainer);

      _scrolledContainer.addControlListener(new ControlAdapter() {
         @Override
         public void controlResized(final ControlEvent e) {
            _scrolledContainer.setMinSize(_innerScContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
         }
      });

      // create inner container
      _innerScContainer = new Composite(_scrolledContainer, SWT.NONE);
      _innerScContainer.setBackground(_bgColorHeader);
//		_innerScContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
//		_innerScContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(numColumns).applyTo(_innerScContainer);

      _scrolledContainer.setContent(_innerScContainer);

      _columnSpacing = 1;

      switch (_layoutFormat) {
      case LAYOUT_1_COLUMNS:
         createUI_70_1_Columns();
         break;

      case LAYOUT_2_COLUMNS:
         createUI_70_2_Columns();
         break;

      case LAYOUT_3_COLUMNS:
         createUI_70_3_Columns();
         break;

      default:
         createUI_70_6_Columns();
         break;
      }
   }

   private void createUI_70_1_Columns() {

      _graphInfos.clear();

      // create graph info list
      for (final ChartDataSerie xyData : _chartDataModel.getXyData()) {
         _graphInfos.add(new GraphInfo(this, xyData, _innerScContainer));
      }

      // ----------------------------------------------------------------

      createUIHeader_10_Left();
      createUIHeader_UnitLabel();

      for (final GraphInfo graphInfo : _graphInfos) {
         graphInfo.createUI_Info_10_Left();
         graphInfo.createUI_Value_Unit();
      }

      // ----------------------------------------------------------------

      createVerticalBorder();

      createUIHeader_20_Right();
      createUIHeader_UnitLabel();

      for (final GraphInfo graphInfo : _graphInfos) {
         graphInfo.createUI_Info_20_Right();
         graphInfo.createUI_Value_Unit();
      }

      // ----------------------------------------------------------------

      createVerticalBorder();

      createUIHeader_50_Diff();
      createUIHeader_UnitLabel();

      for (final GraphInfo graphInfo : _graphInfos) {
         graphInfo.createUI_Info_50_Diff();
         graphInfo.createUI_Value_Unit();
      }

      // ----------------------------------------------------------------

      createVerticalBorder();

      createUIHeader_60_Avg();
      createUIHeader_UnitLabel();

      for (final GraphInfo graphInfo : _graphInfos) {
         graphInfo.createUI_Info_60_Avg();
         graphInfo.createUI_Value_Unit();
      }

      // ----------------------------------------------------------------

      createVerticalBorder();

      createUIHeader_30_Min();
      createUIHeader_UnitLabel();

      for (final GraphInfo graphInfo : _graphInfos) {
         graphInfo.createUI_Info_30_Min();
         graphInfo.createUI_Value_Unit();
      }

      // ----------------------------------------------------------------

      createVerticalBorder();

      createUIHeader_40_Max();
      createUIHeader_UnitLabel();

      for (final GraphInfo graphInfo : _graphInfos) {
         graphInfo.createUI_Info_40_Max();
         graphInfo.createUI_Value_Unit();
      }
   }

   private void createUI_70_2_Columns() {

      createUIHeader_10_Left();
      createUIHeader_20_Right();
      createUIHeader_UnitLabel();

      // add all graphs and the x axis to the layout
      _graphInfos.clear();
      for (final ChartDataSerie xyData : _chartDataModel.getXyData()) {

         final GraphInfo graphInfo = new GraphInfo(this, xyData, _innerScContainer);

         graphInfo.createUI_Info_10_Left();
         graphInfo.createUI_Info_20_Right();
         graphInfo.createUI_Value_Unit();

         _graphInfos.add(graphInfo);
      }

      // ----------------------------------------------------------------

      createVerticalBorder();

      createUIHeader_50_Diff();
      createUIHeader_60_Avg();
      createUIHeader_UnitLabel();

      for (final GraphInfo graphInfo : _graphInfos) {
         graphInfo.createUI_Info_50_Diff();
         graphInfo.createUI_Info_60_Avg();
         graphInfo.createUI_Value_Unit();
      }

      // ----------------------------------------------------------------

      createVerticalBorder();

      createUIHeader_30_Min();
      createUIHeader_40_Max();
      createUIHeader_UnitLabel();

      for (final GraphInfo graphInfo : _graphInfos) {
         graphInfo.createUI_Info_30_Min();
         graphInfo.createUI_Info_40_Max();
         graphInfo.createUI_Value_Unit();
      }
   }

   private void createUI_70_3_Columns() {

      createUIHeader_10_Left();
      createUIHeader_20_Right();
      createUIHeader_50_Diff();
      createUIHeader_UnitLabel();

      // add all graphs and the x axis to the layout
      _graphInfos.clear();
      for (final ChartDataSerie xyData : _chartDataModel.getXyData()) {

         final GraphInfo graphInfo = new GraphInfo(this, xyData, _innerScContainer);

         graphInfo.createUI_Info_10_Left();
         graphInfo.createUI_Info_20_Right();
         graphInfo.createUI_Info_50_Diff();
         graphInfo.createUI_Value_Unit();

         _graphInfos.add(graphInfo);
      }

      createVerticalBorder();

      createUIHeader_30_Min();
      createUIHeader_40_Max();
      createUIHeader_60_Avg();
      createUIHeader_UnitLabel();

      for (final GraphInfo graphInfo : _graphInfos) {
         graphInfo.createUI_Info_30_Min();
         graphInfo.createUI_Info_40_Max();
         graphInfo.createUI_Info_60_Avg();
         graphInfo.createUI_Value_Unit();
      }
   }

   private void createUI_70_6_Columns() {

      createUIHeader_ValueLabel();
      createUIHeader_10_Left();
      createUIHeader_20_Right();
      createUIHeader_30_Min();
      createUIHeader_40_Max();
      createUIHeader_50_Diff();
      createUIHeader_60_Avg();
      createUIHeader_UnitLabel();

      // add all graphs and the x axis to the layout
      _graphInfos.clear();
      for (final ChartDataSerie xyData : _chartDataModel.getXyData()) {

         final GraphInfo graphInfo = new GraphInfo(this, xyData, _innerScContainer);

         graphInfo.createUI_Value_Label();

         graphInfo.createUI_Info_10_Left();
         graphInfo.createUI_Info_20_Right();
         graphInfo.createUI_Info_30_Min();
         graphInfo.createUI_Info_40_Max();
         graphInfo.createUI_Info_50_Diff();
         graphInfo.createUI_Info_60_Avg();

         graphInfo.createUI_Value_Unit();

         _graphInfos.add(graphInfo);
      }
   }

   private void createUIHeader_10_Left() {

      final Label label = new Label(_innerScContainer, SWT.TRAIL);
      label.setText(Messages.TourAnalyzer_Label_left + UI.SPACE);
      label.setFont(_fontBold);
      label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
      label.setBackground(_bgColorHeader);
   }

   private void createUIHeader_20_Right() {

      final Label label = new Label(_innerScContainer, SWT.TRAIL);
      label.setText(Messages.TourAnalyzer_Label_right + UI.SPACE);
      label.setFont(_fontBold);
      label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
      label.setBackground(_bgColorHeader);
   }

   private void createUIHeader_30_Min() {

      final Label label = new Label(_innerScContainer, SWT.TRAIL);
      label.setText(Messages.TourAnalyzer_Label_minimum + UI.SPACE);
      label.setFont(_fontBold);
      label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
      label.setBackground(_bgColorHeader);
   }

   private void createUIHeader_40_Max() {

      final Label label = new Label(_innerScContainer, SWT.TRAIL);
      label.setText(Messages.TourAnalyzer_Label_maximum + UI.SPACE);
      label.setFont(_fontBold);
      label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
      label.setBackground(_bgColorHeader);
   }

   private void createUIHeader_50_Diff() {

      final Label label = new Label(_innerScContainer, SWT.TRAIL);
      label.setText(Messages.TourAnalyzer_Label_difference + UI.SPACE);
      label.setFont(_fontBold);
      label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
      label.setBackground(_bgColorHeader);
   }

   private void createUIHeader_60_Avg() {

      final Label label = new Label(_innerScContainer, SWT.TRAIL);
      label.setText(Messages.TourAnalyzer_Label_average + UI.SPACE);
      label.setFont(_fontBold);
      label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
      label.setBackground(_bgColorHeader);
   }

   private void createUIHeader_UnitLabel() {

      final Label label = new Label(_innerScContainer, SWT.LEFT);
      label.setText(UI.EMPTY_STRING);
      label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
      label.setBackground(_bgColorHeader);

      // spacer
//		final Canvas canvas = new Canvas(_innerScContainer, SWT.NONE);
//		GridDataFactory.fillDefaults()//
//				.align(SWT.BEGINNING, SWT.BEGINNING)
//				.hint(0, 0)
//				.applyTo(canvas);
   }

   private void createUIHeader_ValueLabel() {

      final Label label = new Label(_innerScContainer, SWT.NONE);
      label.setText(UI.SPACE + Messages.TourAnalyzer_Label_value);
      label.setFont(_fontBold);
      label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
      label.setBackground(_bgColorHeader);
   }

   private void createVerticalBorder() {

      GridData gd;

      final int columns = _layoutFormat == LAYOUT_1_COLUMNS
            ? 1
            : _layoutFormat == LAYOUT_2_COLUMNS
                  ? 2
                  : _layoutFormat == LAYOUT_3_COLUMNS
                        ? 3
                        : 7;

      gd = new GridData(SWT.FILL, SWT.FILL, false, false);
      gd.heightHint = 3;

      Label label;

      for (int columnIndex = 0; columnIndex < columns; columnIndex++) {

         label = new Label(_innerScContainer, SWT.NONE);
         label.setText(UI.SPACE1);
         label.setLayoutData(gd);
      }

      label = new Label(_innerScContainer, SWT.NONE);
      label.setText(UI.SPACE1);
      label.setLayoutData(gd);
   }

   @Override
   public void dispose() {

      final IWorkbenchPage page = getSite().getPage();

      page.removePostSelectionListener(_postSelectionListener);
      page.removePartListener(_partListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      _colorCache.dispose();

      super.dispose();
   }

   /**
    * @param rgb
    * @return Returns the color from the color cache, the color must not be disposed this is done
    *         when the cache is disposed
    */
   Color getColor(final RGB rgb) {

      final String colorKey = rgb.toString();

      final Color color = _colorCache.get(colorKey);

      if (color == null) {
         return _colorCache.getColor(colorKey, rgb);
      } else {
         return color;
      }
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _bgColorHeader = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
      _fontBold = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
   }

   private void onResizeUI() {

      if (_chartDataModel == null) {
         return;
      }

      updateInfo(_chartInfo, true);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (_isPartVisible == false) {
         return;
      }

      if (selection instanceof SelectionChartInfo) {

         updateInfo((SelectionChartInfo) selection, false);

      } else if (selection instanceof SelectionChartXSliderPosition) {

         updateInfo(((SelectionChartXSliderPosition) selection));

      } else if (selection instanceof SelectionTourData) {

         final SelectionTourData selectionTourData = (SelectionTourData) selection;

         TourChart tourChart = selectionTourData.getTourChart();

         if (tourChart == null) {
            tourChart = TourManager.getActiveTourChart(selectionTourData.getTourData());
         }

         if (tourChart != null) {
            updateInfo(tourChart.getChartInfo(), false);
         }

      } else if (selection instanceof SelectionTourId) {

         updateInfo();

      } else if (selection instanceof SelectionTourChart) {

         final TourChart tourChart = ((SelectionTourChart) selection).getTourChart();

         if (tourChart != null) {
            updateInfo(tourChart.getChartInfo(), false);
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
      }
   }

   @Override
   public void setFocus() {
      _pageBook.setFocus();
   }

   private void showTour() {

      final ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
      onSelectionChanged(selection);

      if (_chartInfo == null) {

//			_pageBook.showPage(_pageNoTour);

         // a tour is not displayed, find a tour provider which provides a tour
         Display.getCurrent().asyncExec(() -> {

            // validate widget
            if (_pageBook.isDisposed()) {
               return;
            }

            /*
             * check if tour was set from a selection provider
             */
            if (_chartInfo != null) {
               return;
            }

            final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();

            if (selectedTours != null && selectedTours.size() > 0) {

               final TourData selectedTour = selectedTours.get(0);
               final SelectionTourId tourSelection = new SelectionTourId(selectedTour.getTourId());

               onSelectionChanged(tourSelection);
            }
         });
      }
   }

   private void updateInfo() {

      /*
       * Run this delayed because the tour chart may not yet contain the data when a new tour is
       * selected.
       */

      _partContainer.getDisplay().asyncExec(() -> {

         final TourChart tourChart = TourManager.getInstance().getActiveTourChart();

         if (tourChart == null || tourChart.isDisposed()) {

            clearView();
            return;
         }

         updateInfo(tourChart.getChartInfo(), false);
      });
   }

   private void updateInfo(final SelectionChartInfo chartInfo, final boolean isForceRecreate) {

      if (chartInfo == null || _pageBook.isDisposed()) {

         clearView();

         return;
      }

      // get time when the redraw is requested
      final long requestedRedrawTime = System.currentTimeMillis();

      if (requestedRedrawTime > _lastUpdateUITime + 100) {

         // force a redraw

         updateUI_Runnable(chartInfo, isForceRecreate);

      } else {

         _updateCounter[0]++;

         _partContainer.getDisplay().asyncExec(new Runnable() {

            final int __runnableCounter = _updateCounter[0];

            @Override
            public void run() {

               // update UI delayed
               if (__runnableCounter != _updateCounter[0]) {
                  // a new update UI occurred
                  return;
               }

               updateUI_Runnable(chartInfo, isForceRecreate);
            }
         });
      }
   }

   /**
    * selection is not from a chart, so it's possible that the chart has not yet the correct slider
    * position, we create the chart info from the slider position
    */
   private void updateInfo(final SelectionChartXSliderPosition sliderPosition) {

      Chart chart = sliderPosition.getChart();

      if (chart == null) {

         final TourChart tourChart = TourManager.getInstance().getActiveTourChart();
         if (tourChart == null || tourChart.isDisposed()) {

            clearView();

            return;
         }

         chart = tourChart;
      }

      final SelectionChartInfo chartInfo = new SelectionChartInfo(chart);

      chartInfo.chartDataModel = chart.getChartDataModel();
      chartInfo.chartDrawingData = chart.getChartDrawingData();

      chartInfo.leftSliderValuesIndex = sliderPosition.getLeftSliderValueIndex();
      chartInfo.rightSliderValuesIndex = sliderPosition.getRightSliderValueIndex();

      updateInfo(chartInfo, false);
   }

   private void updateUI_EmptyValues(final GraphInfo graphInfo) {

      graphInfo.labelDiff.setText(UI.EMPTY_STRING);
      graphInfo.labelAvg.setText(UI.EMPTY_STRING);

      graphInfo.labelLeft.setText(UI.EMPTY_STRING);
      graphInfo.labelRight.setText(UI.EMPTY_STRING);

      graphInfo.labelMin.setText(UI.EMPTY_STRING);
      graphInfo.labelMax.setText(UI.EMPTY_STRING);

   }

   private void updateUI_Runnable(final SelectionChartInfo chartInfo, final boolean isForceRecreate) {

      if (_partContainer.isDisposed()) {
         return;
      }

      _chartInfo = chartInfo;

      // check if the layout needs to be recreated
      boolean isLayoutDirty = false;
      if (_chartDataModel != chartInfo.chartDataModel) {

         // data model changed, another data model can have other visible data
         isLayoutDirty = true;
      }

      // init vars which are used in createLayout()
      _chartDataModel = chartInfo.chartDataModel;
      _chartDrawingData = chartInfo.chartDrawingData;

      if (_chartDrawingData == null) {
         // this happened
         clearView();
         return;
      }

      _graphDrawingData = _chartDrawingData.graphDrawingData;

      if ((_graphDrawingData == null) || (_graphDrawingData.isEmpty()) || (_graphDrawingData.get(0) == null)) {
         // this happened
         clearView();
         return;
      }

      final boolean isForceUpdate = isForceRecreate || isLayoutDirty;

      createUI_10_Analyzer(isForceUpdate);

      updateUI_Values(chartInfo, isForceUpdate);

      // optimize layout
      if (isForceUpdate) {

         // refresh the layout after the data has changed
         _pageAnalyzer.layout();
      }

      _lastUpdateUITime = System.currentTimeMillis();
   }

   private void updateUI_Values(final SelectionChartInfo chartInfo, final boolean isForceUpdate) {

      final ChartDataXSerie xData = _graphDrawingData.get(0).getXData();

      int valuesIndexLeft = chartInfo.leftSliderValuesIndex;
      int valuesIndexRight = chartInfo.rightSliderValuesIndex;

      if (valuesIndexLeft == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
         valuesIndexLeft = _valueIndexLeftBackup;
      } else {
         _valueIndexLeftBackup = valuesIndexLeft;
      }
      if (valuesIndexRight == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
         valuesIndexRight = _valueIndexRightBackup;
      } else {
         _valueIndexRightBackup = valuesIndexRight;
      }

      /*
       * Optimize performance, this can probably be ignore because it must be deeply zoomed in to
       * see a performance gain.
       */
      if (isForceUpdate == false
            && _valueIndexLeftLast == valuesIndexLeft
            && _valueIndexRightLast == valuesIndexRight) {
         return;
      }

      _valueIndexLeftLast = valuesIndexLeft;
      _valueIndexRightLast = valuesIndexRight;

      // get break time
      final TourData tourData = (TourData) _chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);
      final int breakTime = tourData.getBreakTime(valuesIndexLeft, valuesIndexRight);

      for (final GraphInfo graphInfo : _graphInfos) {

         final ChartDataSerie serieData = graphInfo.chartData;

         TourChartAnalyzerInfo analyzerInfo = (TourChartAnalyzerInfo) serieData.getCustomData(TourManager.CUSTOM_DATA_ANALYZER_INFO);

         if (analyzerInfo == null) {
            // create default average object
            analyzerInfo = new TourChartAnalyzerInfo();
         }

         final int valueDecimals = analyzerInfo.getAvgDecimals();
         final int valueDivisor2 = (int) Math.pow(10, valueDecimals);

         final int unitType = serieData.getAxisUnit();
         final int valueDivisor = serieData.getValueDivisor();

         double[] values = null;
         if (serieData instanceof ChartDataYSerie) {

            final ChartDataYSerie yData = (ChartDataYSerie) serieData;
            values = yData.getHighValuesDouble()[0];

         } else if (serieData instanceof ChartDataXSerie) {

            final ChartDataXSerie graphXData = (ChartDataXSerie) serieData;
            values = graphXData.getHighValuesDouble()[0];
         }

         if (values == null) {

            updateUI_EmptyValues(graphInfo);
            continue;
         }

         final int endIndex = values.length - 1;
         if (valuesIndexLeft > endIndex) {
            valuesIndexLeft = endIndex;
         }
         if (valuesIndexRight > endIndex) {
            valuesIndexRight = endIndex;
         }

         valuesIndexLeft = Math.max(0, valuesIndexLeft);
         valuesIndexRight = Math.max(0, valuesIndexRight);

         // values at the left/right slider
         final double leftValue = values[valuesIndexLeft];
         final double rightValue = values[valuesIndexRight];

         if (Double.isNaN(leftValue) || Double.isNaN(rightValue)) {

            // this can happen when a data serie contains multiple tours and not all tours have all data
            updateUI_EmptyValues(graphInfo);
            continue;
         }

         int dataIndex = valuesIndexLeft;
         float avg = 0;
         int avgDiv = 0;
         double min = 0;
         double max = 0;

         /*
          * Compute min/max/avg values
          */
         while (dataIndex <= valuesIndexRight) {

            final double value = values[dataIndex];

            avg += value;
            avgDiv++;

            if (dataIndex == valuesIndexLeft) {

               // this is the first value in the dataseries, set initial value
               min = value;
               max = value;

            } else {

               // optimized for performance

               min = (value <= min) ? value : min; //Math.min(value, min);
               max = (value >= max) ? value : max; //Math.max(value, max);
            }

            dataIndex++;
         }

         /*
          * Compute average values
          */
         final ComputeChartValue computeAvg = analyzerInfo.getComputeChartValue();
         if (computeAvg != null) {

            // average is computed by a callback method

            computeAvg.valueIndexLeft = valuesIndexLeft;
            computeAvg.valueIndexRight = valuesIndexRight;
            computeAvg.xData = xData;
            computeAvg.yData = (ChartDataYSerie) serieData;
            computeAvg.chartModel = _chartDataModel;
            computeAvg.breakTime = breakTime;

            avg = computeAvg.compute();

         } else {

            if (avgDiv != 0) {
               avg = avg / avgDiv;
            }
         }

         /*
          * Update foreground color, otherwise the label is not displayed with the value color
          * because it will be overwritten by the dark theme
          */
         if (graphInfo.labelValueLabel != null) {
            graphInfo.labelValueLabel.setForeground(graphInfo.valueForegroundColor);
         }
         if (graphInfo.labelValueUnit != null) {
            graphInfo.labelValueUnit.setForeground(graphInfo.valueForegroundColor);
         }

         final Label lblLeft = graphInfo.labelLeft;
         final Label lblRight = graphInfo.labelRight;
         final Label lblMin = graphInfo.labelMin;
         final Label lblMax = graphInfo.labelMax;
         final Label lblAvg = graphInfo.labelAvg;
         final Label lblDiff = graphInfo.labelDiff;

         /*
          * Set values into the labels, optimize performance by displaying only changed values
          */

         /*
          * Left slider value
          */
         if (leftValue == 0) {

            graphInfo.prevLeftValue = 0;

            lblLeft.setText(UI.EMPTY_STRING);

         } else {

            if (graphInfo.prevLeftValue != leftValue) {

               graphInfo.prevLeftValue = leftValue;

               lblLeft.setText(Util.formatNumber(leftValue, unitType, valueDivisor, valueDecimals) + UI.SPACE);
               lblLeft.setForeground(graphInfo.valueForegroundColor);
            }
         }

         /*
          * Right slider value
          */
         if (rightValue == 0) {

            graphInfo.prevRightValue = 0;

            lblRight.setText(UI.EMPTY_STRING);

         } else {

            if (graphInfo.prevRightValue != rightValue) {

               graphInfo.prevRightValue = rightValue;

               lblRight.setText(Util.formatNumber(rightValue, unitType, valueDivisor, valueDecimals) + UI.SPACE);
               lblRight.setForeground(graphInfo.valueForegroundColor);
            }
         }

         /*
          * Min value
          */
         if (min == 0) {

            graphInfo.prevMinValue = 0;

            lblMin.setText(UI.EMPTY_STRING);

         } else {

            if (graphInfo.prevMinValue != min) {

               graphInfo.prevMinValue = min;

               lblMin.setText(Util.formatNumber(min, unitType, valueDivisor, valueDecimals) + UI.SPACE);
               lblMin.setForeground(graphInfo.valueForegroundColor);
            }
         }

         /*
          * Max value
          */
         if (max == 0) {

            graphInfo.prevMaxValue = 0;

            lblMax.setText(UI.EMPTY_STRING);

         } else {

            if (graphInfo.prevMaxValue != max) {

               graphInfo.prevMaxValue = max;

               lblMax.setText(Util.formatNumber(max, unitType, valueDivisor, valueDecimals) + UI.SPACE);
               lblMax.setForeground(graphInfo.valueForegroundColor);
            }
         }

         /*
          * Avg value
          */
         if (analyzerInfo.isShowAvg()) {

            float avgValue = avg;

            if (analyzerInfo.isShowAvgDecimals()) {

               avgValue *= valueDivisor2;

               if (graphInfo.prevAvgValue != (int) avgValue) {

                  graphInfo.prevAvgValue = (int) avgValue;

                  lblAvg.setText(Util.formatNumber(avgValue, unitType, valueDivisor2, valueDecimals) + UI.SPACE);
                  lblAvg.setForeground(graphInfo.valueForegroundColor);
               }

            } else {

               if (graphInfo.prevAvgValue != (int) avgValue) {

                  graphInfo.prevAvgValue = (int) avgValue;

                  lblAvg.setText(Util.formatValue((int) avgValue, unitType, valueDivisor, true) + UI.SPACE);
                  lblAvg.setForeground(graphInfo.valueForegroundColor);
               }
            }

         } else {

            graphInfo.prevAvgValue = Double.MIN_VALUE;

            lblAvg.setText(UI.EMPTY_STRING);
         }

         /*
          * Diff value
          */
         final double diffValue = rightValue - leftValue;

         if (diffValue == 0) {

            graphInfo.prevDiffValue = 0;
            lblDiff.setText(UI.EMPTY_STRING);

         } else {

            if (graphInfo.prevDiffValue != diffValue) {

               graphInfo.prevDiffValue = diffValue;

               lblDiff.setText(Util.formatNumber(diffValue, unitType, valueDivisor, valueDecimals) + UI.SPACE);
               lblDiff.setForeground(graphInfo.valueForegroundColor);
            }
         }
      }
   }
}
