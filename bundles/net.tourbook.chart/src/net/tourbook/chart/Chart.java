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
package net.tourbook.chart;

import java.util.HashMap;

import net.tourbook.common.UI;
import net.tourbook.common.form.ViewForm;
import net.tourbook.common.tooltip.IPinned_ToolTip;
import net.tourbook.common.util.ITourToolTipProvider;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Chart widget
 */
public class Chart extends ViewForm {

   private static final String                       ACTION_ID_MOVE_LEFT_SLIDER_HERE  = "ACTION_ID_MOVE_LEFT_SLIDER_HERE";  //$NON-NLS-1$
   private static final String                       ACTION_ID_MOVE_RIGHT_SLIDER_HERE = "ACTION_ID_MOVE_RIGHT_SLIDER_HERE"; //$NON-NLS-1$
   private static final String                       ACTION_ID_MOVE_SLIDERS_TO_BORDER = "ACTION_ID_MOVE_SLIDERS_TO_BORDER"; //$NON-NLS-1$
   private static final String                       ACTION_ID_ZOOM_FIT_GRAPH         = "ACTION_ID_ZOOM_FIT_GRAPH";         //$NON-NLS-1$
   private static final String                       ACTION_ID_ZOOM_IN_TO_SLIDER      = "ACTION_ID_ZOOM_IN_TO_SLIDER";      //$NON-NLS-1$

   static final int                                  NO_BAR_SELECTION                 = -1;

   public static final String                        CUSTOM_DATA_TOUR_ID              = "tourId";                           //$NON-NLS-1$

   public static final int                           SYNCH_MODE_NO                    = 0;
   public static final int                           SYNCH_MODE_BY_SCALE              = 1;
   public static final int                           SYNCH_MODE_BY_SIZE               = 2;

   private static final int                          MouseMove                        = 10;
   private static final int                          MouseDown                        = 20;
   private static final int                          MouseUp                          = 30;
   private static final int                          MouseDoubleClick                 = 40;
   private static final int                          MouseExit                        = 50;
   private static final int                          KeyDown                          = 110;
   private static final int                          ChartResized                     = 999;

   public static Color                               FOREGROUND_COLOR_GRID;
   public static Color                               FOREGROUND_COLOR_UNITS;

   private final ListenerList<IBarSelectionListener> _barSelectionListeners           = new ListenerList<>();
   private final ListenerList<IBarSelectionListener> _barDoubleClickListeners         = new ListenerList<>();
   private final ListenerList<IHoveredValueListener> _chartHoveredValueListener       = new ListenerList<>();
   private final ListenerList<IKeyListener>          _chartKeyListener                = new ListenerList<>();
   private final ListenerList<IMouseListener>        _chartMouseListener              = new ListenerList<>();
   private final ListenerList<IMouseListener>        _chartMouseMoveListener          = new ListenerList<>();
   private final ListenerList<IChartOverlay>         _chartOverlayListener            = new ListenerList<>();
   private final ListenerList<ISliderMoveListener>   _sliderMoveListeners             = new ListenerList<>();

   private ActionMouseWheelMode                      _action_MouseWheelMode;
   private ActionZoomIn                              _action_ZoomIn;
   private ActionZoomOut                             _action_ZoomOut;

   private ChartComponents                           _chartComponents;

   private Chart                                     _synchedChart;

   private ChartDataModel                            _chartDataModel;

   private IToolBarManager                           _toolbarMgr;
   private IChartContextProvider                     _chartContextProvider;

   private boolean                                   _isShowZoomActions               = false;
   private boolean                                   _isShowMouseMode                 = false;

   private Color                                     _backgroundColor;

   /**
    * listener which is called when the x-marker was dragged
    */
   IChartListener                                    _draggingListenerXMarker;

   private IHoveredValueTooltipListener              _hoveredValueTooltipListener;

   private HashMap<String, Action>                   _allChartActions;
   private boolean                                   _isFillToolbar                   = true;
   private boolean                                   _isToolbarCreated;

   private int                                       _barSelectionSerieIndex;
   private int                                       _barSelectionValueIndex;

   int                                               _synchMode;

   /**
    * <code>true</code> to start the bar chart at the bottom of the chart
    */
   private boolean                                   _isDrawBarChartAtBottom          = true;

   /**
    * minimum width in pixel for one unit, this is only an approximate value because the pixel is
    * rounded up or down to fit a rounded unit
    */
   protected int                                     gridVerticalDistance             = 30;
   protected int                                     gridHorizontalDistance           = 70;

   protected boolean                                 isShowHorizontalGridLines        = false;
   protected boolean                                 isShowVerticalGridLines          = false;

   /**
    * Transparency of the graph lines
    */
   protected int                                     graphTransparency_Line           = 0xFF;

   /**
    * Transparency of the graph fillings
    */
   protected int                                     graphTransparency_Filling        = 0xE0;

   /**
    * The graph transparency can be adjusted with this value. This value is multiplied with the
    * {@link #graphTransparency_Filling} and {@link #graphTransparency_Line}.
    * <p>
    * Opacity: 0.0 = transparent, 1.0 = opaque.
    */
   double                                            graphTransparencyAdjustment      = 1.0;

   /**
    * Antialiasing for the graph, can be {@link SWT#ON} or {@link SWT#OFF}.
    */
   public int                                        graphAntialiasing                = SWT.OFF;

   /*
    * Segment alternate color
    */
   protected boolean      isShowSegmentAlternateColor = true;
   protected RGB          segmentAlternateColor_Light = new RGB(0xf5, 0xf5, 0xf5);
   protected RGB          segmentAlternateColor_Dark  = new RGB(0x40, 0x40, 0x40);

   /**
    * Mouse wheel mode to move a x-slider, select a bar or zoom the chart
    */
   private MouseWheelMode _mouseWheelMode             = MouseWheelMode.Selection;

   private boolean        _isTopMenuPosition;

   /**
    * Is <code>true</code> when running in UI update, then events are not fired.
    */
   private boolean        _isInUpdateUI;

   /**
    * Chart widget
    */
   public Chart(final Composite parent, int style) {

      // remove border from the inner chart but set the border around the whole chart (with toolbar) when requested

      super(parent, removeBorder(style));

      if ((style & SWT.BORDER) != 0) {

         style = (style & ~SWT.BORDER);

         setBorderVisible(true);
      }

      setupColors();

      final GridLayout gl = new GridLayout(1, false);
      gl.marginWidth = 0;
      gl.marginHeight = 0;
      gl.verticalSpacing = 0;
      setLayout(gl);

      _chartComponents = new ChartComponents(this, style);
      setContent(_chartComponents);

      // set the default background color
      _backgroundColor = getDisplay().getSystemColor(SWT.COLOR_WHITE);
   }

   private static int removeBorder(final int style) {

      if ((style & SWT.BORDER) != 0) {

         // remove border from style
         return (style & ~SWT.BORDER);
      }

      return style;
   }

   public void addBarSelectionListener(final IBarSelectionListener listener) {
      _barSelectionListeners.add(listener);
   }

   public void addChartKeyListener(final IKeyListener keyListener) {
      _chartKeyListener.add(keyListener);
   }

   public void addChartMouseListener(final IMouseListener mouseListener) {
      _chartMouseListener.add(mouseListener);
   }

   public void addChartMouseMoveListener(final IMouseListener mouseListener) {
      _chartMouseMoveListener.add(mouseListener);
   }

   public void addChartOverlay(final IChartOverlay chartOverlay) {
      _chartOverlayListener.add(chartOverlay);
   }

   public void addDoubleClickListener(final IBarSelectionListener listener) {
      _barDoubleClickListeners.add(listener);
   }

   public void addHoveredValueListener(final IHoveredValueListener listener) {
      _chartHoveredValueListener.add(listener);
   }

   /**
    * Adds a listener when the vertical slider is moved
    *
    * @param listener
    */
   public void addSliderMoveListener(final ISliderMoveListener listener) {
      _sliderMoveListeners.add(listener);
   }

   public void addXMarkerDraggingListener(final IChartListener xMarkerDraggingListener) {
      _draggingListenerXMarker = xMarkerDraggingListener;
   }

   /**
    * create zoom/navigation actions which are managed by the chart
    */
   private void createActions() {

      createActions_10_ChartActions();

      if (_isFillToolbar && _isToolbarCreated == false) {
         _isToolbarCreated = true;
         fillToolbar(true);
      }
   }

   /**
    * Creates all chart actions
    */
   private void createActions_10_ChartActions() {

      // create actions only once
      if (_allChartActions != null) {
         return;
      }

      _allChartActions = new HashMap<>();

      _action_MouseWheelMode = new ActionMouseWheelMode(this);
      _action_ZoomIn = new ActionZoomIn(this);
      _action_ZoomOut = new ActionZoomOut(this);

      _allChartActions.put(ACTION_ID_MOVE_LEFT_SLIDER_HERE, new ActionMoveLeftSliderHere(this));
      _allChartActions.put(ACTION_ID_MOVE_RIGHT_SLIDER_HERE, new ActionMoveRightSliderHere(this));
      _allChartActions.put(ACTION_ID_MOVE_SLIDERS_TO_BORDER, new ActionMoveSlidersToBorder(this));
      _allChartActions.put(ACTION_ID_ZOOM_FIT_GRAPH, new ActionZoomFitGraph(this));
      _allChartActions.put(ACTION_ID_ZOOM_IN_TO_SLIDER, new ActionZoomToSlider(this));

      enableActions();
   }

   /**
    * Creates the chart actions
    */
   public void createChartActions() {

      createActions_10_ChartActions();
   }

   /**
    * @return
    */
   private SelectionChartInfo createChartInfo() {

      if (_chartComponents == null) {
         return null;
      }

      final ChartComponentGraph componentGraph = _chartComponents.getChartComponentGraph();
      final int hoveredLineValueIndex = componentGraph.getHovered_ValuePoint_Index();
      boolean isUseLeftSlider = false;
      if (hoveredLineValueIndex == -1) {

         // hovered line is not yet recognized
//			return null;

         isUseLeftSlider = true;
      }

      final ChartXSlider leftSlider = componentGraph.getLeftSlider();
      final ChartXSlider rightSlider = componentGraph.getRightSlider();

      final SelectionChartInfo chartInfo = new SelectionChartInfo(this);

      chartInfo.chartDataModel = _chartDataModel;
      chartInfo.chartDrawingData = _chartComponents.getChartDrawingData();

      chartInfo.leftSliderValuesIndex = leftSlider.getValuesIndex();
      chartInfo.rightSliderValuesIndex = rightSlider.getValuesIndex();

      if (isUseLeftSlider) {
         chartInfo.selectedSliderValuesIndex = chartInfo.leftSliderValuesIndex;
      } else {
         chartInfo.selectedSliderValuesIndex = hoveredLineValueIndex;
      }

      return chartInfo;
   }

   /**
    * disable all actions
    */
   private void disableAllActions() {

      if (_allChartActions != null) {

         for (final Action action : _allChartActions.values()) {
            action.setEnabled(false);
         }
      }
   }

   /**
    * Dispose colors which are used to paint the graphs.
    */
   public void disposeColors() {

      _chartComponents.getChartComponentGraph().disposeColors();
   }

   void enableActions() {

      if (_allChartActions == null) {
         return;
      }

      final ChartComponentGraph chartComponentGraph = _chartComponents.getChartComponentGraph();

      final boolean canZoomOut = chartComponentGraph.getZoomRatio() > 1;
      final boolean canZoomIn = chartComponentGraph.getXXDevGraphWidth() < ChartComponents.CHART_MAX_WIDTH;

      _action_ZoomIn.setEnabled(canZoomIn);
      _action_ZoomOut.setEnabled(canZoomOut);

      // zoom in to slider has no limits but when there are more than 10000 units, the units are not displayed
      _allChartActions.get(ACTION_ID_ZOOM_IN_TO_SLIDER).setEnabled(true);

      // fit to graph is always enabled because the y-slider can change the chart
      _allChartActions.get(ACTION_ID_ZOOM_FIT_GRAPH).setEnabled(true);

      _allChartActions.get(ACTION_ID_MOVE_LEFT_SLIDER_HERE).setEnabled(true);
      _allChartActions.get(ACTION_ID_MOVE_RIGHT_SLIDER_HERE).setEnabled(true);
      _allChartActions.get(ACTION_ID_MOVE_SLIDERS_TO_BORDER).setEnabled(true);

      _action_MouseWheelMode.setEnabled(true);
   }

   void fillContextMenu(final IMenuManager menuMgr,
                        final ChartXSlider leftSlider,
                        final ChartXSlider rightSlider,
                        final int hoveredBarSerieIndex,
                        final int hoveredBarValueIndex,
                        final int mouseDownDevPositionX,
                        final int mouseDownDevPositionY) {

      if (_allChartActions == null) {
         return;
      }

      // check if this is slider context
      final boolean isSliderContext = leftSlider != null || rightSlider != null;
      final boolean showOnlySliderContext = isSliderContext
            && _chartContextProvider != null
            && _chartContextProvider.showOnlySliderContextMenu();

      if (_chartContextProvider != null && showOnlySliderContext == false && _isTopMenuPosition) {
         _chartContextProvider.fillContextMenu(menuMgr, mouseDownDevPositionX, mouseDownDevPositionY);
      }

      fillContextMenu_ChartDefault(menuMgr, leftSlider, rightSlider, hoveredBarSerieIndex, hoveredBarValueIndex);

      if (_chartContextProvider != null && showOnlySliderContext == false && _isTopMenuPosition == false) {
         menuMgr.add(new Separator());
         _chartContextProvider.fillContextMenu(menuMgr, mouseDownDevPositionX, mouseDownDevPositionY);
      }
   }

   private void fillContextMenu_ChartDefault(final IMenuManager menuMgr,
                                             final ChartXSlider leftSlider,
                                             final ChartXSlider rightSlider,
                                             final int hoveredBarSerieIndex,
                                             final int hoveredBarValueIndex) {

      /*
       * Mouse wheel action
       */
      // set text for mouse wheel mode
      if (_mouseWheelMode.equals(MouseWheelMode.Selection)) {

         // mouse mode: slider
         _action_MouseWheelMode.setText(Messages.Action_mouse_mode_zoom);

      } else {

         // mouse mode: zoom
         _action_MouseWheelMode.setText(Messages.Action_mouse_mode_slider);
      }

      if (_chartDataModel.getChartType() == ChartType.BAR) {

         /*
          * Create menu for bar charts
          */

         // get the context provider from the data model
         final IChartContextProvider barChartContextProvider =
               (IChartContextProvider) _chartDataModel.getCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER);

         if (barChartContextProvider != null) {
            barChartContextProvider.fillBarChartContextMenu(menuMgr, hoveredBarSerieIndex, hoveredBarValueIndex);
         }

         if (_isShowZoomActions) {

            menuMgr.add(new Separator());
            menuMgr.add(_action_MouseWheelMode);
            menuMgr.add(_allChartActions.get(ACTION_ID_ZOOM_FIT_GRAPH));
         }

      } else {

         /*
          * Create menu for line charts
          */

         // fill slider context menu
         if (_chartContextProvider != null) {

            menuMgr.add(new Separator());
            _chartContextProvider.fillXSliderContextMenu(menuMgr, leftSlider, rightSlider);
         }

         if (_isShowZoomActions) {

            menuMgr.add(new Separator());
            menuMgr.add(_action_MouseWheelMode);
            menuMgr.add(_allChartActions.get(ACTION_ID_MOVE_LEFT_SLIDER_HERE));
            menuMgr.add(_allChartActions.get(ACTION_ID_MOVE_RIGHT_SLIDER_HERE));
            menuMgr.add(_allChartActions.get(ACTION_ID_MOVE_SLIDERS_TO_BORDER));
            menuMgr.add(_allChartActions.get(ACTION_ID_ZOOM_IN_TO_SLIDER));
            menuMgr.add(_allChartActions.get(ACTION_ID_ZOOM_FIT_GRAPH));
         }
      }
   }

   /**
    * put the actions into the internal toolbar
    *
    * @param refreshToolbar
    */
   public void fillToolbar(final boolean refreshToolbar) {

      if (_allChartActions == null) {
         return;
      }

      if (_isShowZoomActions || _isShowMouseMode) {

         // add the action to the toolbar
         final IToolBarManager tbm = getToolBarManager();

         if (_isShowZoomActions) {

            tbm.add(new Separator());

            tbm.add(_action_ZoomIn);
            tbm.add(_action_ZoomOut);
         }

         if (refreshToolbar) {
            tbm.update(true);
         }
      }
   }

   void fireEvent_BarSelection(final int serieIndex, final int valueIndex) {

      _barSelectionSerieIndex = serieIndex;
      _barSelectionValueIndex = valueIndex;

      final Object[] listeners = _barSelectionListeners.getListeners();
      for (final Object listener2 : listeners) {
         final IBarSelectionListener listener = (IBarSelectionListener) listener2;
         listener.selectionChanged(serieIndex, valueIndex);
      }
   }

   void fireEvent_ChartDoubleClick(final int serieIndex, final int valueIndex) {

      _barSelectionSerieIndex = serieIndex;
      _barSelectionValueIndex = valueIndex;

      final Object[] listeners = _barDoubleClickListeners.getListeners();
      for (final Object listener2 : listeners) {
         final IBarSelectionListener listener = (IBarSelectionListener) listener2;
         listener.selectionChanged(serieIndex, valueIndex);
      }
   }

   private void fireEvent_ChartMouse(final ChartMouseEvent mouseEvent) {

      final Object[] listeners = _chartMouseListener.getListeners();
      for (final Object listener : listeners) {

         switch (mouseEvent.type) {
         case Chart.ChartResized:
            ((IMouseListener) listener).chartResized();
            break;

         case Chart.MouseExit:
            ((IMouseListener) listener).mouseExit();
            break;

         case Chart.MouseMove:
            ((IMouseListener) listener).mouseMove(mouseEvent);
            break;

         case Chart.MouseDown:
            ((IMouseListener) listener).mouseDown(mouseEvent);
            break;

         case Chart.MouseUp:
            ((IMouseListener) listener).mouseUp(mouseEvent);
            break;

         case Chart.MouseDoubleClick:
            ((IMouseListener) listener).mouseDoubleClick(mouseEvent);
            break;

         default:
            break;
         }

         if (mouseEvent.isWorked) {
            return;
         }
      }
   }

   private void fireEvent_ChartMouseMove(final ChartMouseEvent mouseEvent) {

      final Object[] listeners = _chartMouseMoveListener.getListeners();
      for (final Object listener : listeners) {

         ((IMouseListener) listener).mouseMove(mouseEvent);

         if (mouseEvent.isWorked) {
            return;
         }
      }
   }

   void fireEvent_HoveredValue(final int hoveredValuePointIndex) {

      final Object[] allListeners = _chartHoveredValueListener.getListeners();

      for (final Object listener : allListeners) {
         ((IHoveredValueListener) listener).hoveredValue(hoveredValuePointIndex);
      }
   }

   private void fireEvent_Key(final ChartKeyEvent keyEvent) {

      final Object[] listeners = _chartKeyListener.getListeners();
      for (final Object listener : listeners) {

         switch (keyEvent.type) {
         case Chart.KeyDown:
            ((IKeyListener) listener).keyDown(keyEvent);
            break;

         default:
            break;
         }

         if (keyEvent.isWorked) {
            return;
         }
      }
   }

   public void fireEvent_SliderMove() {

      if (_isInUpdateUI) {
         return;
      }

      final SelectionChartInfo chartInfo = createChartInfo();

      final Object[] listeners = _sliderMoveListeners.getListeners();
      for (final Object listener2 : listeners) {
         final ISliderMoveListener listener = (ISliderMoveListener) listener2;
         listener.sliderMoved(chartInfo);
      }
   }

   /**
    * @return Returns action to set the mouse wheel mode
    */
   public ActionMouseWheelMode getAction_MouseWheelMode() {
      return _action_MouseWheelMode;
   }

   public Color getBackgroundColor() {
      return _backgroundColor;
   }

   public Boolean getCanAutoMoveSliders() {
      return _chartComponents.getChartComponentGraph()._canAutoMoveSliders;
   }

   public boolean getCanAutoZoomToSlider() {
      return _chartComponents.getChartComponentGraph()._canAutoZoomToSlider;
   }

   public ChartComponents getChartComponents() {
      return _chartComponents;
   }

   /**
    * @return Returns the data model for the chart
    */
   public ChartDataModel getChartDataModel() {
      return _chartDataModel;
   }

   public ChartDrawingData getChartDrawingData() {
      return _chartComponents.getChartDrawingData();
   }

   /**
    * Return information about the chart
    *
    * @return
    */
   public SelectionChartInfo getChartInfo() {
      return createChartInfo();
   }

   Object[] getChartOverlays() {
      return _chartOverlayListener.getListeners();
   }

   public ChartTitleSegmentConfig getChartTitleSegmentConfig() {
      return _chartComponents.componentGraph.chartTitleSegmentConfig;
   }

   /**
    * @return Returns the index in the data series which is hovered with the mouse or
    *         <code>-1</code> when a value is not hovered.
    */
   public int getHoveredValuePointIndex() {
      return _chartComponents.getChartComponentGraph().getHovered_ValuePoint_Index();
   }

   IHoveredValueTooltipListener getHoveredValueTooltipListener() {
      return _hoveredValueTooltipListener;
   }

   public int getLeftAxisWidth() {
      return _chartComponents.getYAxisWidthLeft();
   }

   /**
    * @return Returns the left slider
    */
   public ChartXSlider getLeftSlider() {
      return _chartComponents.getChartComponentGraph().getLeftSlider();
   }

   /**
    * @return Returns margin between the upper most graph and the top. This can include the chart
    *         title height and/or horizontal slider label height.
    */
   public int getMarginTop() {
      return _chartComponents.getDevChartMarginTop();
   }

   public MouseWheelMode getMouseWheelMode() {
      return _mouseWheelMode;
   }

   /**
    * @return Return the right slider
    */
   public ChartXSlider getRightSlider() {
      return _chartComponents.getChartComponentGraph().getRightSlider();
   }

   public ISelection getSelection() {

      if (_chartDataModel == null) {
         return null;
      }

      if (_chartDataModel.getChartType() == ChartType.BAR) {
         return new SelectionBarChart(_barSelectionSerieIndex, _barSelectionValueIndex);
      }

      return null;
   }

   /**
    * @return Returns <code>true</code> to start the bars at the bottom of the chart
    */
   boolean getStartAtChartBottom() {
      return _isDrawBarChartAtBottom;
   }

   /**
    * Returns the toolbar for the chart, if no toolbar manager is set with setToolbarManager, the
    * manager will be created and the toolbar is on top of the chart.
    * <p>
    * A border is painted between the chart and toolbar because {@link ViewForm} draws this line in
    * the onPaint() method.
    *
    * @return
    */
   public IToolBarManager getToolBarManager() {

      if (_toolbarMgr == null) {

         // create the toolbar and put it on top of the chart
         final ToolBar toolBarControl = new ToolBar(this, SWT.FLAT/* | SWT.WRAP */);
         setTopRight(toolBarControl);

         // toolBarControl.addListener(SWT.Resize, new Listener() {
         // public void handleEvent(Event e) {
         //
// wrap the tool bar on resize
         // Rectangle rect = getClientArea();
         // Point size = toolBarControl.computeSize(rect.width, SWT.DEFAULT);
         // toolBarControl.setSize(size);
         //
         // }
         // });

         // create toolbar manager
         _toolbarMgr = new ToolBarManager(toolBarControl);
      }

      return _toolbarMgr;
   }

   /**
    * @return
    * @return Returns control for which the tool tip is created
    */
   public ChartComponentAxis getToolTipControl() {
      return getChartComponents().getAxisLeft();
   }

   protected Control getValuePointControl() {
      return _chartComponents.getChartComponentGraph();
   }

   /**
    * returns the value index for the x-sliders
    */
   public SelectionChartXSliderPosition getXSliderPosition() {

      final ChartComponentGraph chartGraph = _chartComponents.getChartComponentGraph();

      return new SelectionChartXSliderPosition(//
            this,
            chartGraph.getLeftSlider().getValuesIndex(),
            chartGraph.getRightSlider().getValuesIndex());
   }

   public long getXXDevViewPortLeftBorder() {
      return _chartComponents.getChartComponentGraph().getXXDevViewPortLeftBorder();
   }

   protected void handleTooltipMouseEvent(final Event event, final Point mouseDisplayPosition) {
      _chartComponents.getChartComponentGraph().handleTooltipMouseEvent(event, mouseDisplayPosition);
   }

   public boolean isInUpdateUI() {
      return _isInUpdateUI;
   }

   /**
    * @return Returns <code>true</code> when the x-sliders are visible
    */
   public boolean isXSliderVisible() {
      return _chartComponents.devSliderBarHeight != 0;
   }

   void onExecuteMouseWheelMode(final MouseWheelMode mouseWheelMode) {
      setMouseWheelMode(mouseWheelMode);
   }

   void onExecuteMoveLeftSliderHere() {
      _chartComponents.getChartComponentGraph().moveLeftSliderHere();
   }

   void onExecuteMoveRightSliderHere() {
      _chartComponents.getChartComponentGraph().moveRightSliderHere();
   }

   public void onExecuteMoveSlidersToBorder() {
      _chartComponents.getChartComponentGraph().moveSlidersToBorderWithoutCheck();
   }

   protected void onExecuteZoomFitGraph() {

      _chartDataModel.resetMinMaxValues();

      _chartComponents.getChartComponentGraph().zoomOutFitGraph();
   }

   void onExecuteZoomIn(final double accelerator) {

      if (_chartComponents.devSliderBarHeight == 0) {
         _chartComponents.getChartComponentGraph().zoomInWithoutSlider();
         _chartComponents.onResize();
      } else {
         _chartComponents.getChartComponentGraph().zoomInWithMouse(Integer.MIN_VALUE, accelerator);
      }
   }

   /**
    * Zoom to the vertical sliders
    */
   public void onExecuteZoomInWithSlider() {

      _chartComponents.getChartComponentGraph().zoomInWithSlider();
      _chartComponents.onResize();
   }

   public void onExecuteZoomOut(final boolean updateChart, final double accelerator) {

      if (_chartDataModel == null) {
         return;
      }

      _chartComponents.getChartComponentGraph().zoomOutWithMouse(updateChart, Integer.MIN_VALUE, accelerator);
   }

   void onExternalChartResize() {

      fireEvent_ChartMouse(
            new ChartMouseEvent(//
                  Chart.ChartResized,
                  System.currentTimeMillis(),
                  0,
                  0));
   }

   ChartKeyEvent onExternalKeyDown(final Event event) {

      final ChartKeyEvent keyEvent = new ChartKeyEvent(Chart.KeyDown, event.keyCode, event.stateMask);

      fireEvent_Key(keyEvent);

      return keyEvent;
   }

   ChartMouseEvent onExternalMouseDoubleClick(final long eventTime, final int devXMouse, final int devYMouse) {

      final ChartMouseEvent event = new ChartMouseEvent(//
            Chart.MouseDoubleClick,
            eventTime,
            devXMouse,
            devYMouse);

      fireEvent_ChartMouse(event);

      return event;
   }

   ChartMouseEvent onExternalMouseDown(final long eventTime,
                                       final int devXMouse,
                                       final int devYMouse,
                                       final int stateMask) {

      final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseDown, eventTime, devXMouse, devYMouse, stateMask);

      fireEvent_ChartMouse(event);

      return event;
   }

   void onExternalMouseExit(final long eventTime) {

      fireEvent_ChartMouse(new ChartMouseEvent(Chart.MouseExit, eventTime, 0, 0));
   }

   ChartMouseEvent onExternalMouseMove(final long eventTime, final int devXMouse, final int devYMouse) {

      final ChartMouseEvent event = new ChartMouseEvent(//
            Chart.MouseMove,
            eventTime,
            devXMouse,
            devYMouse);

      fireEvent_ChartMouse(event);

      return event;
   }

   ChartMouseEvent onExternalMouseMoveImportant(final long eventTime, final int devXMouse, final int devYMouse) {

      final ChartMouseEvent event = new ChartMouseEvent(//
            Chart.MouseMove,
            eventTime,
            devXMouse,
            devYMouse);

      fireEvent_ChartMouseMove(event);

      return event;
   }

   ChartMouseEvent onExternalMouseUp(final long eventTime, final int devXMouse, final int devYMouse) {

      final ChartMouseEvent event = new ChartMouseEvent(//
            Chart.MouseUp,
            eventTime,
            devXMouse,
            devYMouse);

      fireEvent_ChartMouse(event);

      return event;
   }

   void onHideContextMenu(final MenuEvent e, final Control menuParentControl) {

      if (_chartContextProvider != null) {
         _chartContextProvider.onHideContextMenu(e, menuParentControl);
      }
   }

   void onShowContextMenu(final MenuEvent menuEvent, final Control menuParentControl) {

      if (_chartContextProvider != null) {
         _chartContextProvider.onShowContextMenu(menuEvent, menuParentControl);
      }
   }

   /**
    * make the graph dirty and redraw it
    */
   public void redrawChart() {
      _chartComponents.getChartComponentGraph().redrawChart();
   }

   /**
    * Make the custom layers dirty and redraw it.
    */
   public void redrawLayer() {
      _chartComponents.getChartComponentGraph().redrawLayer();
   }

   public void removeChartKeyListener(final IKeyListener keyListener) {
      _chartKeyListener.remove(keyListener);
   }

   public void removeChartMouseListener(final IMouseListener mouseListener) {
      _chartMouseListener.remove(mouseListener);
   }

   public void removeChartMouseMoveListener(final IMouseListener mouseListener) {
      _chartMouseMoveListener.remove(mouseListener);
   }

   public void removeChartOverlay(final IChartOverlay chartOverlay) {
      _chartOverlayListener.remove(chartOverlay);
   }

   public void removeDoubleClickListener(final IBarSelectionListener listener) {
      _barDoubleClickListeners.remove(listener);
   }

   public void removeHoveredValueListener(final IHoveredValueListener listener) {
      _chartHoveredValueListener.remove(listener);
   }

   public void removeSelectionChangedListener(final IBarSelectionListener listener) {
      _barSelectionListeners.remove(listener);
   }

   public void resetGraphAlpha() {
      graphTransparencyAdjustment = 1;
   }

   /**
    * Do a resize for all chart components which creates new drawing data
    */
   public void resizeChart() {
      _chartComponents.onResize();
   }

   /**
    * Set the background color for the chart, the default is SWT.COLOR_WHITE
    *
    * @param backgroundColor
    *           The backgroundColor to set.
    */
   public void setBackgroundColor(final Color backgroundColor) {
      _backgroundColor = backgroundColor;
   }

   /**
    * Set the option to move the sliders to the border when the chart is zoomed
    *
    * @param canMoveSlidersWhenZoomed
    */
   public void setCanAutoMoveSliders(final boolean canMoveSlidersWhenZoomed) {
      _chartComponents.getChartComponentGraph().setCanAutoMoveSlidersWhenZoomed(canMoveSlidersWhenZoomed);
   }

   /**
    * set the option to auto zoom the chart
    *
    * @param canZoomToSliderOnMouseUp
    */
   public void setCanAutoZoomToSlider(final boolean canZoomToSliderOnMouseUp) {
      _chartComponents.getChartComponentGraph().setCanAutoZoomToSlider(canZoomToSliderOnMouseUp);
   }

   public void setChartOverlayDirty() {
      _chartComponents.getChartComponentGraph().setChartOverlayDirty();
   }

   public void setContextProvider(final IChartContextProvider chartContextProvider) {
      _chartContextProvider = chartContextProvider;
   }

   /**
    * @param chartContextProvider
    * @param isTopMenuPosition
    *           When <code>true</code> the context menu will be positioned before the chart menu
    *           actions.
    */
   public void setContextProvider(final IChartContextProvider chartContextProvider, final boolean isTopMenuPosition) {

      _chartContextProvider = chartContextProvider;
      _isTopMenuPosition = isTopMenuPosition;
   }

   protected void setDataModel(final ChartDataModel chartDataModel) {
      _chartDataModel = chartDataModel;
   }

   /**
    * Set <code>false</code> to not draw the bars at the bottom of the chart
    *
    * @param fDrawBarCharttAtBottom
    */
   public void setDrawBarChartAtBottom(final boolean fDrawBarCharttAtBottom) {
      this._isDrawBarChartAtBottom = fDrawBarCharttAtBottom;
   }

   /**
    * Display an error message instead of the chart.
    *
    * @param errorMessage
    */
   public void setErrorMessage(final String errorMessage) {

      final ChartDataModel emptyModel = new ChartDataModel(ChartType.LINE);

      _chartComponents.setErrorMessage(errorMessage);

      _chartDataModel = emptyModel;
      _chartComponents.setModel(emptyModel, false);

      disableAllActions();
   }

   @Override
   public boolean setFocus() {

      /*
       * set focus to the graph component
       */
      return _chartComponents.getChartComponentGraph().setFocus();
   }

   /**
    * Adjust the alpha value for the filling operation, this value is multiplied with
    * {@link #graphTransparency_Filling} and {@link #graphTransparency_Line} which is set in the
    * tour
    * chart preference page.
    * <p>
    * Opacity: 0.0 = transparent, 1.0 = opaque.
    *
    * @param adjustment
    */
   public void setGraphAlpha(final double adjustment) {
      graphTransparencyAdjustment = adjustment;
   }

   public void setHovered_ValuePoint_Index(final int hoveredValuePointIndex) {
      _chartComponents.getChartComponentGraph().setHovered_ValuePoint_Index(hoveredValuePointIndex);
   }

   /**
    * Set hovered tour in the {@link ChartComponentGraph}.
    *
    * @param chartTitleSegment
    */
   public void setHoveredTitleSegment(final ChartTitleSegment chartTitleSegment) {
      _chartComponents.getChartComponentGraph().setHoveredTitleSegment(chartTitleSegment);
   }

   protected void setHoveredValueTooltipListener(final IHoveredValueTooltipListener hoveredValuePointListener) {
      _hoveredValueTooltipListener = hoveredValuePointListener;
   }

   public void setIsInUpdateUI(final boolean isInUpdateUI) {
      _isInUpdateUI = isInUpdateUI;
   }

   public void setLineSelectionPainter(final ILineSelectionPainter lineSelectionPainter) {
      _chartComponents.getChartComponentGraph().setLineSelectionPainter(lineSelectionPainter);
   }

   /**
    * Sets the mouse wheel mode
    */
   public void setMouseWheelMode(final MouseWheelMode mouseWheelMode) {

      _mouseWheelMode = mouseWheelMode;

      updateUI_MouseWheelMode();

      _chartComponents.getChartComponentGraph().setCursorStyle();
   }

   /**
    * Select (highlight) the bar in the bar chart
    *
    * @param selectedItems
    *           items in the x-data serie which should be selected, can be <code>null</code> to
    *           deselect the bar
    */
   public void setSelectedBars(final boolean[] selectedItems) {

      // set default value
      _barSelectionSerieIndex = 0;
      _barSelectionValueIndex = 0;

      if (selectedItems != null) {

         // get selected bar
         for (int itemIndex = 0; itemIndex < selectedItems.length; itemIndex++) {
            if (selectedItems[itemIndex]) {
               _barSelectionValueIndex = itemIndex;
               break;
            }
         }
      }

      _chartComponents.getChartComponentGraph().setSelectedBars(selectedItems);

      fireEvent_BarSelection(0, _barSelectionValueIndex);
   }

   public void setSelectedLines(final boolean isSelectionVisible) {
      _chartComponents.getChartComponentGraph().setSelectedLines(isSelectionVisible);
   }

   /**
    * Make the mouse mode button visible
    */
   public void setShowMouseMode() {
      _isShowMouseMode = true;
   }

   /**
    * @param isSliderVisible
    *           <code>true</code> shows the sliders
    */
   public void setShowSlider(final boolean isSliderVisible) {
      _chartComponents.setSliderVisible(isSliderVisible);
   }

   /**
    * @param isVisible
    *           When <code>true</code> then the area between the vertical x-sliders are filled with
    *           a translucent background color.
    */
   public void setShowXSliderArea(final boolean isVisible) {
      _chartComponents.componentGraph.setXSliderAreaVisible(isVisible);
   }

   public void setShowZoomActions(final boolean isShowZoomActions) {
      _isShowZoomActions = isShowZoomActions;
   }

   /**
    * set the synch configuration which is used when the chart is drawn/resized
    *
    * @param synchConfigIn
    *           set <code>null</code> to disable the synchronization
    */
   public void setSynchConfig(final SynchConfiguration synchConfigIn) {
      _chartComponents.setSynchConfig(synchConfigIn);
   }

   /**
    * Set's the {@link SynchConfiguration} listener, this is a {@link Chart} which will be notified
    * when this chart is resized, <code>null</code> will disable the synchronisation
    *
    * @param chartWidget
    */
   public void setSynchedChart(final Chart chartWidget) {
      _synchedChart = chartWidget;
   }

   protected void setSynchMode(final int synchMode) {
      _synchMode = synchMode;
   }

   /**
    * @param toolbarMgr
    * @param isFillToolbar
    *           set <code>false</code> when the toolbar will be filled with
    *           {@link Chart#fillToolbar(boolean)} from externally, when <code>true</code> the
    *           toolbar will be filled when the chart is updated
    */
   public void setToolBarManager(final IToolBarManager toolbarMgr, final boolean isFillToolbar) {

      _toolbarMgr = toolbarMgr;
      _isFillToolbar = isFillToolbar;
   }

   public void setTourInfoIconToolTipProvider(final ITourToolTipProvider tourToolTip) {

      // set tour info icon into the left axis
      getToolTipControl().setTourToolTipProvider(tourToolTip);
   }

   private void setupColors() {

      // color must be set lately otherwise the dark theme could not be initialized

//    final int grayColor = 111;
//    gcGraph.setForeground(new Color(grayColor, grayColor, grayColor));

      FOREGROUND_COLOR_GRID = UI.IS_DARK_THEME
            ? new Color(99, 99, 99)
            : new Color(222, 222, 222);

      FOREGROUND_COLOR_UNITS = UI.IS_DARK_THEME
            ? new Color(155, 155, 155)
            : new Color(133, 133, 133);
   }

   public void setValuePointToolTipProvider(final IPinned_ToolTip valuePointToolTip) {
      _chartComponents.componentGraph.valuePointToolTip = valuePointToolTip;
   }

   /**
    * sets the position of the x-sliders
    *
    * @param sliderPosition
    */
   public void setXSliderPosition(final SelectionChartXSliderPosition sliderPosition) {
      _chartComponents.setXSliderPosition(sliderPosition, true);
   }

   /**
    * sets the position of the x-sliders
    *
    * @param sliderPosition
    */
   public void setXSliderPosition(final SelectionChartXSliderPosition sliderPosition, final boolean isFireEvent) {

      // check if the position is for this chart
      if (sliderPosition.getChart() == this) {
         _chartComponents.setXSliderPosition(sliderPosition, isFireEvent);
      }
   }

   /**
    * Enable/disable the zoom in/out action
    *
    * @param isEnabled
    */
   public void setZoomActionsEnabled(final boolean isEnabled) {

      final ChartComponentGraph chartComponentGraph = _chartComponents.getChartComponentGraph();

      final boolean canZoomIn = chartComponentGraph.getXXDevGraphWidth() < ChartComponents.CHART_MAX_WIDTH;
      final boolean canZoomOut = chartComponentGraph.getZoomRatio() > 1;

      _action_ZoomIn.setEnabled(canZoomIn && isEnabled);
      _action_ZoomOut.setEnabled(canZoomOut);

      _allChartActions.get(ACTION_ID_ZOOM_FIT_GRAPH).setEnabled(isEnabled);
   }

   public void switchSlidersTo2ndXData() {
      _chartComponents.getChartComponentGraph().switchSlidersTo2ndXData();
   }

   /**
    * synchronize the charts
    */
   protected void synchronizeChart() {

      if (_synchedChart == null) {
         return;
      }

      getDisplay().asyncExec(() -> _synchedChart.setSynchConfig(_chartComponents._synchConfigOut));
   }

   /**
    * Sets a new data model for the chart and redraws it, NULL will hide the chart
    *
    * @param chartDataModel
    * @param isShowAllData
    *           set <code>true</code> to show the entire data in the chart, otherwise the min max
    *           values will be kept
    */
   public void updateChart(final ChartDataModel chartDataModel, final boolean isShowAllData) {

      updateChart(chartDataModel, true, isShowAllData);
   }

   /**
    * Set a new data model for the chart and redraws it, NULL will hide the chart.
    * <p>
    * This method sets the data for the chart and creates it.
    *
    * @param chartDataModel
    * @param isResetSelection
    *           <code>true</code> will reset the last selection in the chart.
    * @param isShowAllData
    *           When <code>true</code> then the entire data in the chart is displayed, otherwise the
    *           min max values will be kept.
    */
   public void updateChart(final ChartDataModel chartDataModel,
                           final boolean isResetSelection,
                           final boolean isShowAllData) {

      if (chartDataModel == null || //
            (chartDataModel.getYData().isEmpty() //

                  // history do not have Y values
                  && chartDataModel.getChartType() != ChartType.HISTORY) //
      ) {

         final ChartDataModel emptyModel = new ChartDataModel(ChartType.LINE);

         if (chartDataModel != null) {
            String errorMessage = chartDataModel.getErrorMessage();
            if (errorMessage == null) {

               /*
                * display error message that the user is not confuses when a graph is not displayed
                */
               errorMessage = Messages.Error_Message_001_Default;
            }
            _chartComponents.setErrorMessage(errorMessage);
         }

         _chartDataModel = emptyModel;
         _chartComponents.setModel(emptyModel, false);

         disableAllActions();

         return;
      }

      // reset error
      _chartComponents.setErrorMessage(null);

      _chartDataModel = chartDataModel;

      createActions();

      _chartComponents.setModel(chartDataModel, isShowAllData);

      enableActions();

      // reset last selected x-data
      if (isResetSelection) {
         setSelectedBars(null);
      }

      // update chart info view
      fireEvent_SliderMove();
   }

   /**
    * Updates only the custom layers which performance is much faster than a chart update.
    */
   public void updateCustomLayers() {
      _chartComponents.updateCustomLayers();
   }

   /**
    * Update different properties and refresh the chart.
    *
    * @param horizontalGrid
    * @param verticalGrid
    * @param isHGridVisible
    * @param isVGridVisible
    * @param isAlternateColor
    * @param rgb
    */
   public void updateProperties(final int horizontalGrid,
                                final int verticalGrid,
                                final boolean isHGridVisible,
                                final boolean isVGridVisible,
                                final boolean isAlternateColor,
                                final RGB rgbAlternateColor_Light,
                                final RGB rgbAlternateColor_Dark) {

      gridHorizontalDistance = horizontalGrid;
      gridVerticalDistance = verticalGrid;

      isShowHorizontalGridLines = isHGridVisible;
      isShowVerticalGridLines = isVGridVisible;

      isShowSegmentAlternateColor = isAlternateColor;
      segmentAlternateColor_Light = rgbAlternateColor_Light;
      segmentAlternateColor_Dark = rgbAlternateColor_Dark;

      _chartComponents.onResize();
   }

   private void updateUI_MouseWheelMode() {

      if (_action_MouseWheelMode != null) {

         _action_MouseWheelMode.setMouseWheelMode(_mouseWheelMode);
      }
   }

   public void zoomOut() {
      onExecuteZoomFitGraph();
   }

}
