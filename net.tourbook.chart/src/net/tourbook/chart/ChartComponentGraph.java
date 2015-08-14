/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import net.tourbook.common.PointLong;
import net.tourbook.common.RectangleLong;
import net.tourbook.common.UI;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * Draws the graph and axis into the canvas
 * 
 * @author Wolfgang Schramm
 */
public class ChartComponentGraph extends Canvas {

	private static final double			ZOOM_RATIO_FACTOR				= 1.3;

	private static final int			BAR_MARKER_WIDTH				= 16;

	private static final int[]			DOT_DASHES						= new int[] { 1, 1 };

	private static final NumberFormat	_nf								= NumberFormat.getNumberInstance();

	private static final RGB			_gridRGB						= new RGB(230, 230, 230);
	private static final RGB			_gridRGBMajor					= new RGB(220, 220, 220);

	private static final int[][]		_leftAccelerator				= new int[][] {
			{ -40, -200 },
			{ -30, -50 },
			{ -20, -10 },
			{ -10, -5 },
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			// !!! move 2 instead of 1, with 1 it would sometimes not move, needs more investigation
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			{ 0, -2 }													};

	private static final int[][]		_rightAccelerator				= new int[][] {
			{ 10, 2 },
			{ 20, 5 },
			{ 30, 10 },
			{ 40, 50 },
			{ Integer.MAX_VALUE, 200 }									};

	private final LineAttributes		LINE_DASHED						= new LineAttributes(5);
	{
		LINE_DASHED.dashOffset = 3;
		LINE_DASHED.style = SWT.LINE_CUSTOM;
		LINE_DASHED.dash = new float[] { 1f, 2f };
		LINE_DASHED.width = 1f;
	}
	Chart								_chart;

	private final ChartComponents		_chartComponents;

	/**
	 * This image contains one single graph without title and x-axis with units.
	 * <p>
	 * This image was created to fix clipping bugs which occured when gradient filling was painted
	 * with a path.
	 */
	private Image						_chartImage_10_Graphs;

	/**
	 * This image contains the chart without additional layers.
	 */
	private Image						_chartImage_20_Chart;

	/**
	 * Contains custom layers like the markers or tour segments which are painted in the foreground.
	 */
	private Image						_chartImage_30_Custom;

	/**
	 * Contains layers like the x/y sliders, x-marker, selection or hovered line/bar.
	 */
	private Image						_chartImage_40_Overlay;

	/**
	 * 
	 */
	private ChartDrawingData			_chartDrawingData;

	/**
	 * drawing data which is used to draw the chart, when this list is empty, an error is displayed
	 */
	private ArrayList<GraphDrawingData>	_allGraphDrawingData			= new ArrayList<GraphDrawingData>();
	private ArrayList<GraphDrawingData>	_revertedGraphDrawingData;

	/**
	 * Zoom ratio between the visible and the virtual chart width, is 1.0 when not zoomed, is > 1
	 * when zoomed.
	 */
	private double						_graphZoomRatio					= 1;

	/**
	 * Contains the width for a zoomed graph this includes also the invisible parts.
	 */
	private long						_xxDevGraphWidth;

	/**
	 * When the graph is zoomed, the chart shows only a part of the whole graph in the viewport.
	 * This value contains the left border of the viewport.
	 */
	long								_xxDevViewPortLeftBorder;

	/**
	 * ratio for the position where the chart starts on the left side within the virtual graph width
	 */
	private double						_zoomRatioLeftBorder;

	/**
	 * ratio where the mouse was double clicked, this position is used to zoom the chart with the
	 * mouse
	 */
	private double						_zoomRatioCenter;

	/**
	 * when the slider is dragged and the mouse up event occures, the graph is zoomed to the sliders
	 * when set to <code>true</code>
	 */
	boolean								_canAutoZoomToSlider;

	/**
	 * when <code>true</code> the vertical sliders will be moved to the border when the chart is
	 * zoomed
	 */
	boolean								_canAutoMoveSliders;

	/**
	 * true indicates the graph needs to be redrawn in the paint event
	 */
	private boolean						_isChartDirty;

	/**
	 * true indicates the slider needs to be redrawn in the paint event
	 */
	private boolean						_isSliderDirty;

	/**
	 * when <code>true</code> the custom layers above the graph image needs to be redrawn in the
	 * next paint event
	 */
	private boolean						_isCustomLayerImageDirty;

	/**
	 * set to <code>true</code> when the selection needs to be redrawn
	 */
	private boolean						_isSelectionDirty;

	/**
	 * status for the x-slider, <code>true</code> indicates, the slider is visible
	 */
	private boolean						_isXSliderVisible;

	/**
	 * true indicates that the y-sliders is visible
	 */
	private boolean						_isYSliderVisible;

	/*
	 * chart slider
	 */
	private final ChartXSlider			_xSliderA;
	private final ChartXSlider			_xSliderB;

	/**
	 * xSliderDragged is set when the slider is being dragged, otherwise it is to <code>null</code>
	 */
	private ChartXSlider				_xSliderDragged;

	/**
	 * This is the slider which is drawn on top of the other, this is normally the last dragged
	 * slider
	 */
	private ChartXSlider				_xSliderOnTop;

	/**
	 * this is the slider which is below the top slider
	 */
	private ChartXSlider				_xSliderOnBottom;

	/**
	 * contains the x-slider when the mouse is over it, or <code>null</code> when the mouse is not
	 * over it
	 */
	private ChartXSlider				_mouseOverXSlider;

	/**
	 * Contains the slider which has the focus.
	 */
	private ChartXSlider				_selectedXSlider;

	/**
	 * Device position of the x-slider line when the slider is dragged. The position can be outside
	 * of the viewport which causes autoscrolling.
	 */
	private int							_devXDraggedXSliderLine;

	/**
	 * Mouse device position when autoscrolling is done with the mouse but without a x-slider
	 */
	private int							_devXAutoScrollMousePosition	= Integer.MIN_VALUE;

	/**
	 * list for all y-sliders
	 */
	private ArrayList<ChartYSlider>		_ySliders;

	/**
	 * contextLeftSlider is set when the right mouse button was clicked and the left slider was hit
	 */
	private ChartXSlider				_contextLeftSlider;

	/**
	 * contextRightSlider is set when the right mouse button was clicked and the right slider was
	 * hit
	 */
	private ChartXSlider				_contextRightSlider;

	/**
	 * cursor when the graph can be resizes
	 */
	private Cursor						_cursorResizeLeftRight;

	private Cursor						_cursorArrow;
	private Cursor						_cursorDragged;
	private Cursor						_cursorDragXSlider_ModeZoom;
	private Cursor						_cursorDragXSlider_ModeSlider;
	private Cursor						_cursorHoverXSlider;
	private Cursor						_cursorModeZoom;
	private Cursor						_cursorModeZoomMove;
	private Cursor						_cursorModeSlider;
	private Cursor						_cursorMove1x;
	private Cursor						_cursorMove2x;
	private Cursor						_cursorMove3x;
	private Cursor						_cursorMove4x;
	private Cursor						_cursorMove5x;
	private Cursor						_cursorResizeTopDown;
	private Cursor						_cursorXSliderLeft;
	private Cursor						_cursorXSliderRight;

	private Color						_gridColor;
	private Color						_gridColorMajor;

	/**
	 * Contains a {@link ChartSegment} when a tour title area is hovered, otherwise
	 * <code>null</code> .
	 */
	private ChartSegment				_hoveredSegment;

	/**
	 * serie index for the hovered bar, the bar is hidden when -1;
	 */
	private int							_hoveredBarSerieIndex			= -1;

	private int							_hoveredBarValueIndex;
	private boolean						_isHoveredBarDirty;

	private ToolTipV1					_hoveredBarToolTip;

	private boolean						_isHoveredLineVisible			= false;
	private int							_hoveredValuePointIndex			= -1;

	private ArrayList<RectangleLong[]>	_lineFocusRectangles			= new ArrayList<RectangleLong[]>();
	private ArrayList<PointLong[]>		_lineDevPositions				= new ArrayList<PointLong[]>();

	/**
	 * Tooltip for value points, can be <code>null</code> when not set.
	 */
	IValuePointToolTip					valuePointToolTip;

	private ChartYSlider				_hitYSlider;
	private ChartYSlider				_ySliderDragged;
	private int							_ySliderGraphX;

	private boolean						_isSetXSliderPositionLeft;
	private boolean						_isSetXSliderPositionRight;

	/**
	 * <code>true</code> when the x-marker is moved with the mouse
	 */
	private boolean						_isXMarkerMoved;

	/**
	 * x-position when the x-marker was started to drag
	 */
	private int							_devXMarkerDraggedStartPos;

	/**
	 * x-position when the x-marker is moved
	 */
	private int							_devXMarkerDraggedPos;

	private int							_movedXMarkerStartValueIndex;
	private int							_movedXMarkerEndValueIndex;

	private double						_xMarkerValueDiff;

	/**
	 * <code>true</code> when the chart is dragged with the mouse
	 */
	private boolean						_isChartDragged					= false;

	/**
	 * <code>true</code> when the mouse button in down but not moved
	 */
	private boolean						_isChartDraggedStarted			= false;

	private Point						_draggedChartStartPos;
	private Point						_draggedChartDraggedPos;

	private boolean[]					_selectedBarItems;

	private final int[]					_drawAsyncCounter				= new int[1];

	private boolean						_isAutoScroll;
	private boolean						_isDisableHoveredLineValueIndex;
	private int[]						_autoScrollCounter				= new int[1];

	private final ColorCache			_colorCache						= new ColorCache();

	private boolean						_isSelectionVisible;

	/**
	 * Is <code>true</code> when this chart gained the focus, <code>false</code> when the focus is
	 * lost.
	 */
	private boolean						_isFocusActive;

	private boolean						_isOverlayDirty;

	/**
	 * widget relative position of the mouse in the mouse down event
	 */
	private int							_devXMouseDown;
	private int							_devYMouseDown;
	private int							_devXMouseMove;
	private int							_devYMouseMove;

	private boolean						_isPaintDraggedImage			= false;

	/**
	 * is <code>true</code> when data for a graph is available
	 */
	private boolean						_isGraphVisible					= false;

	/**
	 * Client area for this canvas
	 */
	Rectangle							_clientArea;

	/**
	 * After a resize the custom overlay must be recomputed
	 */
	private int							_isCustomOverlayInvalid;

	private PixelConverter				_pc;

	/**
	 * Is <code>true</code> when a chart can be overlapped. The overlap feature is currently
	 * supported for graphs which all have the chart type ChartType.LINE.
	 */
	boolean								_canChartBeOverlapped;

	/**
	 * Is <code>true</code> when overlapped graphs are enabled.
	 */
	boolean								_isChartOverlapped;

	/**
	 * Cache font to improve performance.
	 */
	private Font						_uiFont;

	/**
	 * Configuration how the chart title is displayed.
	 */
	ChartSegmentConfig					chartSegmentConfig				= new ChartSegmentConfig();

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the parent of this control.
	 * @param style
	 *            the style of this control.
	 */
	ChartComponentGraph(final Chart chartWidget, final Composite parent, final int style) {

		// create composite with horizontal scrollbars
		super(parent, SWT.H_SCROLL | SWT.NO_BACKGROUND);

		_chart = chartWidget;
		_uiFont = _chart.getFont();

		_pc = new PixelConverter(_chart);

		_cursorResizeLeftRight = new Cursor(getDisplay(), SWT.CURSOR_SIZEWE);
		_cursorResizeTopDown = new Cursor(getDisplay(), SWT.CURSOR_SIZENS);
		_cursorDragged = new Cursor(getDisplay(), SWT.CURSOR_SIZEALL);
		_cursorArrow = new Cursor(getDisplay(), SWT.CURSOR_ARROW);

		_cursorModeSlider = createCursorFromImage(Messages.Image_cursor_mode_slider);
		_cursorModeZoom = createCursorFromImage(Messages.Image_cursor_mode_zoom);
		_cursorModeZoomMove = createCursorFromImage(Messages.Image_cursor_mode_zoom_move);
		_cursorDragXSlider_ModeZoom = createCursorFromImage(Messages.Image_Cursor_DragXSlider_ModeZoom);
		_cursorDragXSlider_ModeSlider = createCursorFromImage(Messages.Image_Cursor_DragXSlider_ModeSlider);
		_cursorHoverXSlider = createCursorFromImage(Messages.Image_Cursor_Hover_XSlider);

		_cursorMove1x = createCursorFromImage(Messages.Image_Cursor_Move1x);
		_cursorMove2x = createCursorFromImage(Messages.Image_Cursor_Move2x);
		_cursorMove3x = createCursorFromImage(Messages.Image_Cursor_Move3x);
		_cursorMove4x = createCursorFromImage(Messages.Image_Cursor_Move4x);
		_cursorMove5x = createCursorFromImage(Messages.Image_Cursor_Move5x);

		_cursorXSliderLeft = createCursorFromImage(Messages.Image_Cursor_X_Slider_Left);
		_cursorXSliderRight = createCursorFromImage(Messages.Image_Cursor_X_Slider_Right);

		_gridColor = new Color(getDisplay(), _gridRGB);
		_gridColorMajor = new Color(getDisplay(), _gridRGBMajor);

		_chartComponents = (ChartComponents) parent;

		// setup the x-slider
		_xSliderA = new ChartXSlider(this, Integer.MIN_VALUE, ChartXSlider.SLIDER_TYPE_LEFT);
		_xSliderB = new ChartXSlider(this, Integer.MIN_VALUE, ChartXSlider.SLIDER_TYPE_RIGHT);

		_xSliderOnTop = _xSliderB;
		_xSliderOnBottom = _xSliderA;

		_hoveredBarToolTip = new ToolTipV1(_chart);

		addListener();
		createContextMenu();

		final Point devMouse = this.toControl(getDisplay().getCursorLocation());
		setCursorStyle(devMouse.y);
	}

	/**
	 * execute the action which is defined when a bar is selected with the left mouse button
	 */
	private void actionSelectBars() {

		if (_hoveredBarSerieIndex < 0) {
			return;
		}

		boolean[] selectedBarItems;

		if (_allGraphDrawingData.size() == 0) {
			selectedBarItems = null;
		} else {

			final GraphDrawingData graphDrawingData = _allGraphDrawingData.get(0);
			final ChartDataXSerie xData = graphDrawingData.getXData();

			selectedBarItems = new boolean[xData._highValuesDouble[0].length];
			selectedBarItems[_hoveredBarValueIndex] = true;
		}

		setSelectedBars(selectedBarItems);

		_chart.fireBarSelectionEvent(_hoveredBarSerieIndex, _hoveredBarValueIndex);
	}

	/**
	 * hookup all listeners
	 */
	private void addListener() {

		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent event) {

				if (_isChartDragged) {
					drawSync_020_DraggedChart(event.gc);
				} else {

//					final long start = System.nanoTime();
//					System.out.println();
//					System.out.println("onPaint\tstart\t");
//					// TODO remove SYSTEM.OUT.PRINTLN

					drawSync_000_onPaint(event.gc, event.time & 0xFFFFFFFFL);

//					System.out.println("onPaint\tend\t" + (((double) System.nanoTime() - start) / 1000000) + "ms");
//					System.out.println();
//					// TODO remove SYSTEM.OUT.PRINTLN
				}
			}
		});

		// horizontal scrollbar
		final ScrollBar horizontalBar = getHorizontalBar();
		horizontalBar.setEnabled(false);
		horizontalBar.setVisible(false);
		horizontalBar.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onScroll(event);
			}
		});

		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseMove(e.time & 0xFFFFFFFFL, e.x, e.y);
				}
			}
		});

		addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseDoubleClick(e);
				}
			}

			@Override
			public void mouseDown(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseDown(e);
				}
			}

			@Override
			public void mouseUp(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseUp(e);
				}
			}
		});

		addMouseTrackListener(new MouseTrackListener() {
			@Override
			public void mouseEnter(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseEnter(e);
				}
			}

			@Override
			public void mouseExit(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseExit(e);
				}
			}

			@Override
			public void mouseHover(final MouseEvent e) {}
		});

		addListener(SWT.MouseWheel, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				onMouseWheel(event, false, false);
			}
		});

		addFocusListener(new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {

				setFocusToControl();

				_isFocusActive = true;
				_isSelectionDirty = true;

				redraw();
			}

			@Override
			public void focusLost(final FocusEvent e) {

				_isFocusActive = false;
				_isSelectionDirty = true;

				redraw();
			}
		});

		addListener(SWT.Traverse, new Listener() {
			@Override
			public void handleEvent(final Event event) {

				switch (event.detail) {
				case SWT.TRAVERSE_RETURN:
				case SWT.TRAVERSE_ESCAPE:
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
				case SWT.TRAVERSE_PAGE_NEXT:
				case SWT.TRAVERSE_PAGE_PREVIOUS:
					event.doit = true;
					break;
				}
			}
		});

		addControlListener(new ControlListener() {

			@Override
			public void controlMoved(final ControlEvent e) {}

			@Override
			public void controlResized(final ControlEvent e) {

				_clientArea = getClientArea();

				_isDisableHoveredLineValueIndex = true;
			}
		});

		addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				onKeyDown(event);
			}
		});

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

	}

	private void adjustYSlider() {

		/*
		 * check if the y slider was outside of the bounds, recompute the chart when necessary
		 */

		final GraphDrawingData drawingData = _ySliderDragged.getDrawingData();

		final ChartDataYSerie yData = _ySliderDragged.getYData();
		final ChartYSlider slider1 = yData.getYSlider1();
		final ChartYSlider slider2 = yData.getYSlider2();

		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - drawingData.devGraphHeight;

		final float graphYBottom = drawingData.getGraphYBottom();
		final double scaleY = drawingData.getScaleY();

		final int devYSliderLine1 = slider1.getDevYSliderLine();
		final int devYSliderLine2 = slider2.getDevYSliderLine();

		final double graphValue1 = (((double) devYBottom - devYSliderLine1) / scaleY + graphYBottom);
		final double graphValue2 = (((double) devYBottom - devYSliderLine2) / scaleY + graphYBottom);

		// get value which was adjusted
		if (_ySliderDragged == slider1) {
			yData.adjustedYValue = (float) graphValue1;
		} else if (_ySliderDragged == slider2) {
			yData.adjustedYValue = (float) graphValue2;
		} else {
			// this case should not happen
			System.out.println("y-slider is not set correctly\t");//$NON-NLS-1$
			return;
		}

		double minValue;
		double maxValue;

		if (graphValue1 < graphValue2) {

			minValue = graphValue1;
			maxValue = graphValue2;

			// position the lower slider to the bottom of the chart
			slider1.setDevYSliderLine(devYBottom);
			slider2.setDevYSliderLine(devYTop);

		} else {

			// graphValue1 >= graphValue2

			minValue = graphValue2;
			maxValue = graphValue1;

			// position the upper slider to the top of the chart
			slider1.setDevYSliderLine(devYTop);
			slider2.setDevYSliderLine(devYBottom);
		}
		yData.setVisibleMinValue(minValue);
		yData.setVisibleMaxValue(maxValue);

		_ySliderDragged = null;

		// the cursor could be outside of the chart, reset it
//		setCursorStyle();

		/*
		 * the hited slider could be outsite of the chart, hide the labels on the slider
		 */
		_hitYSlider = null;

		/*
		 * when the chart is synchronized, the y-slider position is modified, so we overwrite the
		 * synchronized chart y-slider positions until the zoom in marker is overwritten
		 */
		final SynchConfiguration synchedChartConfig = _chartComponents._synchConfigSrc;

		if (synchedChartConfig != null) {

			final ChartYDataMinMaxKeeper synchedChartMinMaxKeeper = synchedChartConfig.getYDataMinMaxKeeper();

			// get the id for the changed y-slider
			final Integer yDataInfo = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);

			// adjust min value for the changed y-slider
			final HashMap<Integer, Double> minKeeper = synchedChartMinMaxKeeper.getMinValues();
			final Double synchedChartMinValue = minKeeper.get(yDataInfo);

			if (synchedChartMinValue != null) {
				minKeeper.put(yDataInfo, minValue);
			}

			// adjust max value for the changed y-slider
			final HashMap<Integer, Double> maxKeeper = synchedChartMinMaxKeeper.getMaxValues();
			final Double synchedChartMaxValue = maxKeeper.get(yDataInfo);

			if (synchedChartMaxValue != null) {
				maxKeeper.put(yDataInfo, maxValue);
			}
		}

		computeChart();
	}

	/**
	 * @param allGraphDrawingData
	 * @return Returns <code>true</code> when a chart can be overlapped.
	 *         <p>
	 *         The overlap feature is currently supported for graphs which all have the chart type
	 *         {@link ChartType#LINE}.
	 */
	private boolean canChartBeOverlapped(final ArrayList<GraphDrawingData> allGraphDrawingData) {

		for (final GraphDrawingData graphDrawingData : allGraphDrawingData) {

			final ChartType chartType = graphDrawingData.getChartType();

			if (chartType != ChartType.LINE && chartType != ChartType.HORIZONTAL_BAR) {
				return false;
			}
		}

		return true;
	}

	/**
	 * when the chart was modified, recompute all
	 */
	private void computeChart() {
		getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!isDisposed()) {
					_chartComponents.onResize();
				}
			}
		});
	}

	private void computeSliderForContextMenu(final int devX, final int devY) {

		ChartXSlider slider1 = null;
		ChartXSlider slider2 = null;

		// reset the context slider
		_contextLeftSlider = null;
		_contextRightSlider = null;

		// check if a slider or the slider line was hit
		if (_xSliderA.getHitRectangle().contains(devX, devY)) {
			slider1 = _xSliderA;
		}

		if (_xSliderB.getHitRectangle().contains(devX, devY)) {
			slider2 = _xSliderB;
		}

		/*
		 * check if a slider was hit
		 */
		if (slider1 == null && slider2 == null) {
			// no slider was hit
			return;
		}

		/*
		 * check if one slider was hit, when yes, the leftslider is set and the right slider is null
		 */
		if (slider1 != null && slider2 == null) {
			// only slider 1 was hit
			_contextLeftSlider = slider1;
			return;
		}
		if (slider2 != null && slider1 == null) {
			// only slider 2 was hit
			_contextLeftSlider = slider2;
			return;
		}

		/*
		 * both sliders are hit
		 */
		final long xSlider1Position = slider1.getHitRectangle().x;
		final long xSlider2Position = slider2.getHitRectangle().x;

		if (xSlider1Position == xSlider2Position) {
			// both sliders are at the same position
			_contextLeftSlider = slider1;
			return;
		}
		if (xSlider1Position < xSlider2Position) {
			_contextLeftSlider = slider1;
			_contextRightSlider = slider2;
		} else {
			_contextLeftSlider = slider2;
			_contextRightSlider = slider1;
		}
	}

	private int computeXMarkerValue(final double[] xValues,
									final int xmStartIndex,
									final double valueDraggingDiff,
									final double valueXStartWithOffset) {

		int valueIndex;
		double valueX = xValues[xmStartIndex];
		double valueHalf;

		/*
		 * get the marker positon for the next value
		 */
		if (valueDraggingDiff > 0) {

			// moved to the right

			for (valueIndex = xmStartIndex; valueIndex < xValues.length; valueIndex++) {

				valueX = xValues[valueIndex];
				valueHalf = ((valueX - xValues[Math.min(valueIndex + 1, xValues.length - 1)]) / 2);

				if (valueX >= valueXStartWithOffset + valueHalf) {
					break;
				}
			}

		} else {

			// moved to the left

			for (valueIndex = xmStartIndex; valueIndex >= 0; valueIndex--) {

				valueX = xValues[valueIndex];
				valueHalf = ((valueX - xValues[Math.max(0, valueIndex - 1)]) / 2);

				if (valueX < valueXStartWithOffset + valueHalf) {
					break;
				}
			}
		}

		return Math.max(0, Math.min(valueIndex, xValues.length - 1));
	}

	/**
	 * create the context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {

				actionSelectBars();

				_hoveredBarToolTip.toolTip20Hide();
				hideTooltip();

				// get cursor location relativ to this graph canvas
				final Point devMouse = toControl(getDisplay().getCursorLocation());

				computeSliderForContextMenu(devMouse.x, devMouse.y);

				_chart.fillContextMenu(
						menuMgr,
						_contextLeftSlider,
						_contextRightSlider,
						_hoveredBarSerieIndex,
						_hoveredBarValueIndex,
						_devXMouseDown,
						_devYMouseDown);
			}
		});

		final Menu contextMenu = menuMgr.createContextMenu(this);

		contextMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(final MenuEvent e) {
				_chart.onHideContextMenu(e, ChartComponentGraph.this);
			}

			@Override
			public void menuShown(final MenuEvent e) {
				_chart.onShowContextMenu(e, ChartComponentGraph.this);
			}
		});

		setMenu(contextMenu);
	}

	/**
	 * Create a cursor resource from an image file
	 * 
	 * @param imageName
	 * @return
	 */
	private Cursor createCursorFromImage(final String imageName) {

		Image cursorImage = null;
		final ImageDescriptor imageDescriptor = Activator.getImageDescriptor(imageName);

		if (imageDescriptor == null) {

			final String resourceName = "icons/" + imageName;//$NON-NLS-1$

			final ClassLoader classLoader = getClass().getClassLoader();

			final InputStream imageStream = classLoader == null
					? ClassLoader.getSystemResourceAsStream(resourceName)
					: classLoader.getResourceAsStream(resourceName);

			if (imageStream == null) {
				return null;
			}

			cursorImage = new Image(Display.getCurrent(), imageStream);

		} else {

			cursorImage = imageDescriptor.createImage();
		}

		final Cursor cursor = new Cursor(getDisplay(), cursorImage.getImageData(), 0, 0);

		cursorImage.dispose();

		return cursor;
	}

	/**
	 * Creates the label(s) and the position for each graph
	 * 
	 * @param gc
	 * @param xSlider
	 */
	private void createXSliderLabel(final GC gc, final ChartXSlider xSlider) {

		final int devSliderLinePos = (int) (xSlider.getXXDevSliderLinePos() - _xxDevViewPortLeftBorder);

		int sliderValueIndex = xSlider.getValuesIndex();
		// final int valueX = slider.getValueX();

		final ArrayList<ChartXSliderLabel> labelList = new ArrayList<ChartXSliderLabel>();
		xSlider.setLabelList(labelList);

		final ScrollBar hBar = getHorizontalBar();
		final int hBarOffset = hBar.isVisible() ? hBar.getSelection() : 0;

		final int leftPos = hBarOffset;
		final int rightPos = leftPos + getDevVisibleChartWidth();

		// create slider label for each graph
		for (final GraphDrawingData drawingData : _allGraphDrawingData) {

			final ChartDataYSerie yData = drawingData.getYData();
			final int labelFormat = yData.getSliderLabelFormat();
			final int valueDivisor = yData.getValueDivisor();
			final int displayedDigits = yData.getDisplayedFractionalDigits();
			final float[][] allYValues = yData.getHighValuesFloat();
			final ISliderLabelProvider sliderLabelProvider = yData.getSliderLabelProvider();

			final boolean isSliderLabelProviderAvailable = sliderLabelProvider != null;

			if (labelFormat == ChartDataYSerie.SLIDER_LABEL_FORMAT_MM_SS || isSliderLabelProviderAvailable) {

				// format: mm:ss or custom label provider

			} else {

				// use default format: ChartDataYSerie.SLIDER_LABEL_FORMAT_DEFAULT

				_nf.setMinimumFractionDigits(displayedDigits);
				_nf.setMaximumFractionDigits(displayedDigits);
			}

			final ChartXSliderLabel label = new ChartXSliderLabel();
			labelList.add(label);

			// draw label on the left or on the right side of the slider,
			// depending on the slider position
			final float[] yValues = allYValues[0];

			// make sure the slider value index is not of bounds, this can
			// happen when the data have changed
			sliderValueIndex = Math.min(sliderValueIndex, yValues.length - 1);

			final float yValue = yValues[sliderValueIndex];
			// final int xAxisUnit = xData.getAxisUnit();
			final StringBuilder labelText = new StringBuilder();

			// create the slider text
			if (isSliderLabelProviderAvailable) {

				labelText.append(sliderLabelProvider.getLabel(sliderValueIndex));

			} else if (labelFormat == ChartDataYSerie.SLIDER_LABEL_FORMAT_MM_SS) {

				// format: mm:ss

				labelText.append(Util.format_mm_ss((long) yValue));

			} else {

				// use default format: ChartDataYSerie.SLIDER_LABEL_FORMAT_DEFAULT

				if (valueDivisor == 1) {
					labelText.append(_nf.format(yValue));
				} else {
					labelText.append(_nf.format(yValue / valueDivisor));
				}
			}

			final String unitLabel = yData.getUnitLabel();
			if (unitLabel.length() > 0) {
				labelText.append(' ');
				labelText.append(unitLabel);
			}
			labelText.append(' ');

			// calculate position of the slider label
			final Point labelExtend = gc.stringExtent(labelText.toString());
			final int labelWidth = labelExtend.x + 0;
			int labelXPos = devSliderLinePos - labelWidth / 2;

			final long labelRightPos = labelXPos + labelWidth;

			if (xSlider == _xSliderDragged) {
				/*
				 * current slider is the dragged slider, clip the slider label position at the
				 * viewport
				 */
				if (labelXPos < leftPos) {
					labelXPos += (leftPos - labelXPos);
				} else if (labelRightPos >= rightPos) {
					labelXPos = rightPos - labelWidth - 1;
				}

			} else {
				/*
				 * current slider is not dragged, clip the slider label position at the chart bounds
				 */
				if (labelXPos < 0) {

					labelXPos = 0;

				} else {

					/*
					 * show the whole label when the slider is on the right border
					 */
					if (labelRightPos > getDevVisibleChartWidth()) {
						labelXPos = getDevVisibleChartWidth() - labelWidth - 1;
					}
				}
			}

// show also value index for debugging
//			label.text = labelText.toString() + " " + xSlider.getValuesIndex();
			label.text = labelText.toString();

			label.height = labelExtend.y - 5;
			label.width = labelWidth;

			label.x = labelXPos;
			label.y = drawingData.getDevYBottom() - drawingData.devGraphHeight - label.height;

			/*
			 * get the y position of the marker which marks the y value in the graph
			 */
			int devYGraph = drawingData.getDevYBottom()
					- (int) ((yValue - drawingData.getGraphYBottom()) * drawingData.getScaleY());

			if (yValue < yData.getVisibleMinValue()) {
				devYGraph = drawingData.getDevYBottom();
			}
			if (yValue > yData.getVisibleMaxValue()) {
				devYGraph = drawingData.getDevYTop();
			}
			label.devYGraph = devYGraph;
		}
	}

	void disposeColors() {
		_colorCache.dispose();
	}

	/**
	 * @param eventTime
	 */
	private void doAutoScroll(final long eventTime) {

// this is not working the mouse can't sometime not be zoomed to the border, depending on the mouse speed
//		/*
//		 * check if the mouse has reached the left or right border
//		 */
//		if (_graphDrawingData == null
//				|| _graphDrawingData.size() == 0
//				|| _hoveredLineValueIndex == 0
//				|| _hoveredLineValueIndex == _graphDrawingData.get(0).getXData()._highValues.length - 1) {
//
//			_isAutoScroll = false;
//
//			return;
//		}

		final int AUTO_SCROLL_INTERVAL = 50; // 20ms == 50fps

		_isAutoScroll = true;

		_autoScrollCounter[0]++;

		getDisplay().timerExec(AUTO_SCROLL_INTERVAL, new Runnable() {

			final int	__runnableScrollCounter	= _autoScrollCounter[0];

			@Override
			public void run() {

				if (__runnableScrollCounter != _autoScrollCounter[0]) {
					return;
				}

				if (isDisposed()
						|| _isAutoScroll == false
						|| (_xSliderDragged == null && _devXAutoScrollMousePosition == Integer.MIN_VALUE)) {

					// make sure that autoscroll/automove is disabled

					_isAutoScroll = false;
					_devXAutoScrollMousePosition = Integer.MIN_VALUE;

					return;
				}

				_isDisableHoveredLineValueIndex = true;

				/*
				 * the offset values are determined experimentally and depends on the mouse position
				 */
				int devMouseOffset = 0;

				if (_devXAutoScrollMousePosition != Integer.MIN_VALUE) {

					boolean isLeft;

					if (_devXAutoScrollMousePosition < 0) {

						// autoscroll the graph to the left

						isLeft = true;

						if (_xxDevViewPortLeftBorder == 0) {

							// left border is already reached, auto scrolling is not necessary
							return;
						}

						for (final int[] accelerator : _leftAccelerator) {
							if (_devXAutoScrollMousePosition < accelerator[0]) {
								devMouseOffset = accelerator[1];
								break;
							}
						}

					} else {

						// autoscroll the graph to the right

						isLeft = false;

						final int devXAutoScrollMousePositionRelative = _devXAutoScrollMousePosition
								- getDevVisibleChartWidth();

						for (final int[] accelerator : _rightAccelerator) {
							if (devXAutoScrollMousePositionRelative < accelerator[0]) {
								devMouseOffset = accelerator[1];
								break;
							}
						}
					}

					doAutoScroll_10_RunnableScrollGraph(this, AUTO_SCROLL_INTERVAL, devMouseOffset, isLeft, eventTime);

				} else {

					if (_devXDraggedXSliderLine < 0) {

						// move x-slider to the left

						for (final int[] accelerator : _leftAccelerator) {
							if (_devXDraggedXSliderLine < accelerator[0]) {
								devMouseOffset = accelerator[1];
								break;
							}
						}

					} else {

						// move x-slider to the right

						final int devXSliderLineRelative = _devXDraggedXSliderLine - getDevVisibleChartWidth();

						for (final int[] accelerator : _rightAccelerator) {
							if (devXSliderLineRelative < accelerator[0]) {
								devMouseOffset = accelerator[1];
								break;
							}
						}
					}

					doAutoScroll_20_RunnableMoveSlider(this, AUTO_SCROLL_INTERVAL, devMouseOffset);
				}
			}
		});
	}

	private void doAutoScroll_10_RunnableScrollGraph(	final Runnable runnable,
														final int autoScrollInterval,
														final int devMouseOffset,
														final boolean isLeft,
														final long eventTime) {

		final long xxDevNewPosition = _xxDevViewPortLeftBorder + devMouseOffset;

		// reposition chart
		setChartPosition(xxDevNewPosition);

		// check if scrolling can be redone
		final long devXRightBorder = _xxDevGraphWidth - getDevVisibleChartWidth();

		boolean isRepeatScrolling;
		if (isLeft) {
			isRepeatScrolling = _xxDevViewPortLeftBorder > 1;
		} else {
			isRepeatScrolling = xxDevNewPosition < devXRightBorder;
		}

		// start scrolling again when the bounds have not been reached
		if (isRepeatScrolling) {
			getDisplay().timerExec(autoScrollInterval, runnable);
		} else {
			_isAutoScroll = false;
		}

		doAutoScroll_30_UpdateHoveredListener(eventTime);
	}

	private void doAutoScroll_20_RunnableMoveSlider(final Runnable runnable,
													final int autoScrollInterval,
													final int devMouseOffset) {

		// get new slider position
		final long xxDevOldSliderLinePos = _xSliderDragged.getXXDevSliderLinePos();
		final long xxDevNewSliderLinePos2 = xxDevOldSliderLinePos + devMouseOffset;
		final long xxDevNewSliderLinePos3 = xxDevNewSliderLinePos2 - _xxDevViewPortLeftBorder;

		// move the slider
		moveXSlider(_xSliderDragged, xxDevNewSliderLinePos3);

		// redraw slider
		_isSliderDirty = true;
		redraw();

		// redraw chart
		setChartPosition(_xSliderDragged, false);

		final boolean isRepeatScrollingLeft = _xxDevViewPortLeftBorder > 1;
		final boolean isRepeatScrollingRight = _xxDevViewPortLeftBorder + xxDevNewSliderLinePos3 < _xxDevGraphWidth;
		final boolean isRepeatScrolling = isRepeatScrollingLeft || isRepeatScrollingRight;

		// start scrolling again when the bounds have not been reached
		if (isRepeatScrolling) {
			getDisplay().timerExec(autoScrollInterval, runnable);
		} else {
			_isAutoScroll = false;
		}
	}

	private void doAutoScroll_30_UpdateHoveredListener(final long eventTime) {

		final IHoveredValueListener hoveredListener = _chart._hoveredListener;

		if (_isHoveredLineVisible || hoveredListener != null) {

			final int hoveredLineValueIndexBACKUP = _hoveredValuePointIndex;

			setHoveredLineValue();

			if (_hoveredValuePointIndex != -1) {

				final PointLong devHoveredValueDevPosition = getHoveredValueDevPosition();

				if (hoveredListener != null) {

					/**
					 * this is very tricky:
					 * <p>
					 * the last mouse move position is used
					 */

					hoveredListener.hoveredValue(
							eventTime,
							_devXMouseMove,
							_devYMouseMove,
							_hoveredValuePointIndex,
							devHoveredValueDevPosition);
				}
			}

			_hoveredValuePointIndex = hoveredLineValueIndexBACKUP;
		}
	}

	private void doAutoZoomToXSliders() {

		if (_canAutoZoomToSlider) {

			// the graph can't be scrolled but the graph should be
			// zoomed to the x-slider positions

			// zoom into the chart
			getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isDisposed()) {
						_chart.onExecuteZoomInWithSlider();
					}
				}
			});
		}
	}

	/**
	 * Draw the graphs into the chart+graph image
	 * 
	 * @return Return <code>true</code> when the graph was painted directly and not in async mode
	 */
	private boolean drawAsync_100_StartPainting() {

//		// get time when the redraw of the may is requested
//		final long requestedRedrawTime = System.currentTimeMillis();

		_drawAsyncCounter[0]++;

//		if (requestedRedrawTime > _lastChartDrawingTime + 100) {
//
//			// force a redraw
//
//			drawAsync101DoPainting();
//
//			return true;
//
//		} else {
//
		getDisplay().asyncExec(new Runnable() {

			final int	__runnableBgCounter	= _drawAsyncCounter[0];

			@Override
			public void run() {

				/*
				 * create the chart image only when a new onPaint event has not occured
				 */
				if (__runnableBgCounter != _drawAsyncCounter[0]) {
					// a new onPaint event occured
					return;
				}

				drawAsync_101_DoPainting();
			}
		});
//		}

		return false;
	}

	private void drawAsync_101_DoPainting() {

//		final long startTime = System.nanoTime();
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (isDisposed()) {
			// this widget is disposed
			return;
		}

		if (_allGraphDrawingData.size() == 0) {
			// drawing data are not set
			return;
		}

		// ensure minimum size
		final int devNewImageWidth = Math.max(ChartComponents.CHART_MIN_WIDTH, getDevVisibleChartWidth());

		/*
		 * the image size is adjusted to the client size but it must be within the min/max ranges
		 */
		final int devNewImageHeight = Math.max(
				ChartComponents.CHART_MIN_HEIGHT,
				Math.min(getDevVisibleGraphHeight(), ChartComponents.CHART_MAX_HEIGHT));

		/*
		 * when the image is the same size as the new we will redraw it only if it is set to dirty
		 */
		if (_isChartDirty == false && _chartImage_20_Chart != null) {

			final Rectangle oldBounds = _chartImage_20_Chart.getBounds();

			if (oldBounds.width == devNewImageWidth && oldBounds.height == devNewImageHeight) {
				return;
			}
		}

		final Rectangle chartImageRect = new Rectangle(0, 0, devNewImageWidth, devNewImageHeight);

		// ensure correct image size
		if (chartImageRect.width <= 0 || chartImageRect.height <= 0) {
			return;
		}

		// create image on which the graph is drawn
		if (Util.canReuseImage(_chartImage_20_Chart, chartImageRect) == false) {
			_chartImage_20_Chart = Util.createImage(getDisplay(), _chartImage_20_Chart, chartImageRect);
		}

		/*
		 * The graph image is only a part where ONE single graph is painted without any title or
		 * unit tick/values
		 */
		final GraphDrawingData graphDrawingData = _allGraphDrawingData.get(0);
		final int devGraphHeight = graphDrawingData.devGraphHeight;
		final Rectangle graphImageRect = new Rectangle(0, 0, //
				devNewImageWidth,
				devGraphHeight < 1 ? 1 : devGraphHeight + 1); // ensure valid height

		if (Util.canReuseImage(_chartImage_10_Graphs, graphImageRect) == false) {
			_chartImage_10_Graphs = Util.createImage(getDisplay(), _chartImage_10_Graphs, graphImageRect);
		}

		// create chart context
		final GC gcChart = new GC(_chartImage_20_Chart);
		final GC gcGraph = new GC(_chartImage_10_Graphs);
		{
			gcChart.setFont(_uiFont);

			// fill background

			if (graphDrawingData.getChartType() == ChartType.HISTORY) {

				final Color historyColor = new Color(gcChart.getDevice(), 0xf0, 0xf0, 0xf0);
				{
					gcChart.setBackground(historyColor);
				}
				historyColor.dispose();

			} else {
				gcChart.setBackground(_chart.getBackgroundColor());
			}
			gcChart.fillRectangle(_chartImage_20_Chart.getBounds());

			if (_chartComponents.errorMessage == null) {

				drawAsync_110_GraphImage(gcChart, gcGraph);

			} else {

				// an error was set in the chart data model
				drawSyncBg_999_ErrorMessage(gcChart);
			}
		}
		gcChart.dispose();
		gcGraph.dispose();

		// remove dirty status
		_isChartDirty = false;

		// dragged image will be painted until the graph image is recomputed
		_isPaintDraggedImage = false;

		// force the overlay image to be redrawn
		_isOverlayDirty = true;

		redraw();

//		final long endTime = System.nanoTime();
//		System.out.println(UI.timeStampNano()
//				+ " drawAsync100: "
//				+ (((double) endTime - startTime) / 1000000)
//				+ " ms   #:"
//				+ _drawAsyncCounter[0]);
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	/**
	 * Draw all graphs, each graph is painted in the same canvas (gcGraph) which is painted in the
	 * the chart image (gcChart).
	 * 
	 * @param gcChart
	 * @param gcGraph
	 */
	private void drawAsync_110_GraphImage(final GC gcChart, final GC gcGraph) {

		int graphNo = 0;
		final int lastGraphNo = _allGraphDrawingData.size();

		final boolean isStackedChart = !_isChartOverlapped;

		ArrayList<GraphDrawingData> allGraphDrawingData;
		if (_canChartBeOverlapped && _isChartOverlapped) {
			allGraphDrawingData = _revertedGraphDrawingData;
		} else {
			allGraphDrawingData = _allGraphDrawingData;
		}

		// reset line positions, they are set when a line graph is painted
		_lineDevPositions.clear();
		_lineFocusRectangles.clear();

		final Color chartBackgroundColor = _chart.getBackgroundColor();
		final Rectangle graphBounds = _chartImage_10_Graphs.getBounds();

		// loop: all graphs in the chart
		for (final GraphDrawingData graphDrawingData : allGraphDrawingData) {

			graphNo++;

			final boolean isFirstGraph = graphNo == 1;
			final boolean isLastGraph = graphNo == lastGraphNo;

			final boolean isFirstOverlappedGraph = _isChartOverlapped && graphNo == 1;
			final boolean isLastOverlappedGraph = _isChartOverlapped && isLastGraph;

			final boolean isDrawBackground = !_canChartBeOverlapped
					|| (_canChartBeOverlapped && (isStackedChart || isFirstOverlappedGraph));

			final boolean isDrawXUnits = !_canChartBeOverlapped
					|| (_canChartBeOverlapped && (isStackedChart || isLastOverlappedGraph));

			final boolean isDrawGrid = !_canChartBeOverlapped
					|| (_canChartBeOverlapped && (isStackedChart || isFirstOverlappedGraph));

			boolean isDrawTitle;
			if (_canChartBeOverlapped && _isChartOverlapped) {
				isDrawTitle = isLastGraph;
			} else {
				isDrawTitle = isFirstGraph;
			}

			// fill background
			if (isDrawBackground) {

				gcGraph.setBackground(chartBackgroundColor);
				gcGraph.fillRectangle(graphBounds);

				if (_chart.isShowSegmentAlternateColor) {
					drawAsync_150_SegmentBackground(gcGraph, graphDrawingData);
				}
			}

			if (isDrawTitle) {
				drawAsync_200_XTitle(gcChart, graphDrawingData);
			}

			if (isDrawGrid) {

				// draw horizontal grid
				drawAsync_220_HGrid(gcGraph, graphDrawingData);
			}

			if (isDrawXUnits) {

				if (isLastGraph) {
					// draw the unit label and unit tick for the last graph
					drawAsync_210_XUnits_VGrid(gcChart, gcGraph, graphDrawingData, true);
				} else {
					drawAsync_210_XUnits_VGrid(gcChart, gcGraph, graphDrawingData, false);
				}
			}

			if (chartSegmentConfig.isShowSegmentSeparator) {
				drawAsync_240_TourSements(gcGraph, graphDrawingData);
			}

			// draw units and grid on the x and y axis
			final ChartType chartType = graphDrawingData.getChartType();

			if (chartType == ChartType.LINE) {

				drawAsync_500_LineGraph(gcGraph, graphDrawingData, isLastGraph);
				drawAsync_520_RangeMarker(gcGraph, graphDrawingData);

			} else if (chartType == ChartType.BAR) {

				drawAsync_530_BarGraph(gcGraph, graphDrawingData);

			} else if (chartType == ChartType.LINE_WITH_BARS) {

				drawAsync_540_LineWithBarGraph(gcGraph, graphDrawingData);

			} else if (chartType == ChartType.XY_SCATTER) {

				drawAsync_550_XYScatter(gcGraph, graphDrawingData);

			} else if (chartType == ChartType.HORIZONTAL_BAR) {

				drawAsync_560_HorizontalBar(gcGraph, graphDrawingData, isLastGraph);

			} else if (chartType == ChartType.HISTORY) {

				drawAsync_600_History(gcGraph, graphDrawingData);
			}

			// draw only the x-axis, this is drawn lately because the graph can overwrite it
			drawAsync_230_XAxis(gcGraph, graphDrawingData);

			// draw graph image into the chart image
			gcChart.drawImage(_chartImage_10_Graphs, 0, graphDrawingData.getDevYTop());

//			System.out.println("20 <- 10\tdrawAsync110GraphImage");
//			// TODO remove SYSTEM.OUT.PRINTLN
		}

		if (_canChartBeOverlapped && _isChartOverlapped) {

			// Revert sequence otherwise they are painted wrong.

			Collections.reverse(_lineDevPositions);
			Collections.reverse(_lineFocusRectangles);
		}

		if (valuePointToolTip != null && _hoveredValuePointIndex != -1) {

			final PointLong hoveredLinePosition = getHoveredValueDevPosition();

			valuePointToolTip.setValueIndex(
					_hoveredValuePointIndex,
					_devXMouseMove,
					_devYMouseMove,
					hoveredLinePosition,
					_graphZoomRatio);
		}
	}

	/**
	 * Draw segment background
	 * 
	 * @param gc
	 * @param drawingData
	 */
	private void drawAsync_150_SegmentBackground(final GC gc, final GraphDrawingData drawingData) {

		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();

		if (chartSegments == null) {
			return;
		}

		final int devYTop = 0;
		final int devYBottom = drawingData.devGraphHeight;

		final double scaleX = drawingData.getScaleX();

		final double[] segmentStartValue = chartSegments.segmentStartValue;
		final double[] segmentEndValue = chartSegments.segmentEndValue;

		if (segmentStartValue == null || segmentEndValue == null) {
			return;
		}

		final Color alternateColor = new Color(gc.getDevice(), _chart.segmentAlternateColor);
		{
			for (int segmentIndex = 0; segmentIndex < segmentStartValue.length; segmentIndex++) {

				if (segmentIndex % 2 == 1) {

					// draw segment background color for every second segment

					final double startValue = segmentStartValue[segmentIndex];
					final double endValue = segmentEndValue[segmentIndex];

					final int devXValueStart = (int) ((scaleX * startValue) - _xxDevViewPortLeftBorder);

					// adjust endValue to fill the last part of the segment
					final int devValueEnd = (int) (scaleX * (endValue + 1) - _xxDevViewPortLeftBorder);

					gc.setBackground(alternateColor);
					gc.fillRectangle(//
							devXValueStart,
							devYTop,
							devValueEnd - devXValueStart,
							devYBottom - devYTop);
				}
			}
		}
		alternateColor.dispose();
	}

	private void drawAsync_200_XTitle(final GC gc, final GraphDrawingData graphDrawingData) {

		final Display display = Display.getCurrent();

		final int devYBottom = graphDrawingData.getDevYBottom();
		final int devYGraphTop = devYBottom - graphDrawingData.devGraphHeight;

		final ChartDataXSerie xData = graphDrawingData.getXData();
		final ChartSegments chartSegments = xData.getChartSegments();
		final HistoryTitle historyTitle = xData.getHistoryTitle();

		final int devYTitle = _chartDrawingData.devMarginTop;

		final ArrayList<ChartSegment> tourSegments = _chartDrawingData.tourSegments;

		final int devGraphWidth = getDevVisibleChartWidth();

		if (historyTitle != null) {

			/*
			 * draw title for each history top segment
			 */

			final double scaleX = graphDrawingData.getScaleX();

			final ArrayList<Long> graphStartValues = historyTitle.graphStart;
			final ArrayList<Long> graphEndValues = historyTitle.graphEnd;
			final ArrayList<String> titleTextList = historyTitle.titleText;

			if (graphStartValues != null && titleTextList != null) {

				final int xUnitTextPos = graphDrawingData.getTitleTextPosition();

				for (int graphIndex = 0; graphIndex < graphStartValues.size(); graphIndex++) {

					final String titleText = titleTextList.get(graphIndex);
					final long graphStart = graphStartValues.get(graphIndex);
					final long graphEnd = graphEndValues.get(graphIndex);

					final double devXSegmentStart = (scaleX * graphStart - _xxDevViewPortLeftBorder);
					final double devXSegmentEnd = ((scaleX * graphEnd - _xxDevViewPortLeftBorder)) - 1.0;

					final double devXSegmentLength = devXSegmentEnd - devXSegmentStart;
					final double devXSegmentCenter = devXSegmentStart + (devXSegmentLength / 2);

					final int devXTitleCenter = gc.textExtent(titleText).x / 2;
					int devX;
					if (xUnitTextPos == GraphDrawingData.X_UNIT_TEXT_POS_CENTER) {

						// draw between 2 units

						devX = (int) (devXSegmentCenter - devXTitleCenter);

					} else {

						// draw in the center of the tick

						devX = (int) (devXSegmentStart - devXTitleCenter);
					}

					gc.drawText(titleText, devX, devYTitle, false);

//					// debug: draw segments
//					final int devYGraphBottom = graphDrawingData.devGraphHeight;
//
//					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//					gc.drawLine((int) devXSegmentStart, 0, (int) devXSegmentStart, devYGraphBottom);
//
//					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
//					gc.drawLine((int) devXSegmentEnd, 0, (int) devXSegmentEnd, devYGraphBottom);
				}
			}

		} else if (chartSegments != null) {

			/*
			 * draw title for each chart segment
			 */

			final double scaleX = graphDrawingData.getScaleX();
			final int devVisibleChartWidth = graphDrawingData.getChartDrawingData().devVisibleChartWidth;

			final double[] valueStart = chartSegments.segmentStartValue;
			final double[] valueEnd = chartSegments.segmentEndValue;
			final String[] segmentTitles = chartSegments.segmentTitle;
			final Object[] segmentCustomData = chartSegments.segmentCustomData;

			final boolean isZoomed = getZoomRatio() > 1.0;

			if (valueStart != null && valueEnd != null && segmentTitles != null) {

				final int titlePadding = 5;
				int devXTitleEnd = -1;
				boolean isFirstSegment = true;

				for (int segmentIndex = 0; segmentIndex < valueStart.length; segmentIndex++) {

					final String segmentTitle = segmentTitles[segmentIndex];

					if (segmentTitle == null) {
						continue;
					}

					final int devXSegmentStart = (int) (scaleX * valueStart[segmentIndex] - _xxDevViewPortLeftBorder);
					final int devXSegmentEnd = (int) (scaleX * (valueEnd[segmentIndex] + 1) - _xxDevViewPortLeftBorder);

					if (devXSegmentEnd < 0) {
						// segment is to the left of the left border
						continue;
					}

					if (devXSegmentStart > devVisibleChartWidth) {
						// segment is to the right of the right border
						break;
					}

					final Point textExtent = gc.textExtent(segmentTitle);
					final int titleWidth = textExtent.x;
					final int titleHeight = textExtent.y;
					final int titleWidth2 = titleWidth / 2;

					final int visibleSegmentStart = Math.max(0, devXSegmentStart);
					final int visibleSegmentEnd = Math.min(devXSegmentEnd, devVisibleChartWidth);
					final int visibleSegmentWidth = visibleSegmentEnd - visibleSegmentStart;

					// center in the middle of the visible segment
					int devXTitle = visibleSegmentStart + visibleSegmentWidth / 2 - titleWidth2;

					if (isFirstSegment && isZoomed == false) {

						// title for the first segment is always displayed when not zoomed

						isFirstSegment = false;

						// ensure title is not truncated at the left border
						if (devXTitle < 0) {
							devXTitle = 0;
						}
					}

					if (chartSegmentConfig.isShowSegmentTitle) {

						// ensure that the title do not overlap a previous title
						if (devXTitle > devXTitleEnd) {

							// keep position when the title is drawn
							devXTitleEnd = devXTitle + titleWidth + titlePadding;

							gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
							gc.drawText(segmentTitle, devXTitle, devYTitle, false);
						}
					}

//					if (chartSegmentConfig.isShowSegmentSeparator) {
//
//						// draw segment start line but not for the first segment
//						if (segmentIndex != 0) {
//
//							gc.setLineAttributes(vertLineLA);
//							gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
//
////							gc.drawLine(devXSegmentStart, 1, devXSegmentStart, devYTitle - 1);
//							gc.drawLine(//
//									devXSegmentStart,
//									devYTitle + titleHeight,
//									devXSegmentStart,
//									devYBottom);
//						}
//					}

					if (segmentCustomData != null && segmentCustomData[segmentIndex] instanceof Long) {

						final ChartSegment chartSegment = new ChartSegment();

						chartSegment.tourId = (Long) segmentCustomData[segmentIndex];

						chartSegment.devXTitle = devXTitle;
						chartSegment.devYTitle = devYTitle;

						chartSegment.titleWidth = titleWidth;
						chartSegment.titleHeight = titleHeight;

						chartSegment.devYGraphTop = devYGraphTop;
						chartSegment.devGraphWidth = _chartDrawingData.devVisibleChartWidth;

						chartSegment.devXSegment = visibleSegmentStart;
						chartSegment.devSegmentWidth = visibleSegmentWidth;

						tourSegments.add(chartSegment);
					}
				}
			}

		} else {

			/*
			 * draw default title, center within the chart
			 */

			String title = graphDrawingData.getXTitle();
			if (title == null) {

				// ensure the tour segment is visible

				title = UI.EMPTY_STRING;
			}

			final Point textExtent = gc.textExtent(title);
			final int titleWidth = textExtent.x;
			final int titleHeight = textExtent.y;

			final int devXTitle = (devGraphWidth / 2) - (titleWidth / 2);

			if (chartSegmentConfig.isShowSegmentTitle) {

				gc.drawText(title, //
						devXTitle < 0 ? 0 : devXTitle,
						devYTitle,
						true);
			}

			final Object tourIdObject = _chartDrawingData.chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
			if (tourIdObject instanceof Long) {

				final ChartSegment tourSegment = new ChartSegment();

				tourSegment.tourId = (Long) tourIdObject;

				tourSegment.devXTitle = devXTitle;
				tourSegment.devYTitle = devYTitle;

				tourSegment.titleWidth = titleWidth;
				tourSegment.titleHeight = titleHeight;

				tourSegment.devYGraphTop = devYGraphTop;
				tourSegment.devGraphWidth = _chartDrawingData.devVisibleChartWidth;

				tourSegment.devXSegment = 0;
				tourSegment.devSegmentWidth = _chartDrawingData.devVisibleChartWidth;

				tourSegments.add(tourSegment);
			}
		}
	}

	/**
	 * Draw the unit label, tick and the vertical grid line for the x axis
	 * 
	 * @param gcChart
	 * @param gcGraph
	 * @param graphDrawingData
	 * @param isDrawUnit
	 *            <code>true</code> indicate to draws the unit tick and unit label additional to the
	 *            unit grid line
	 */
	private void drawAsync_210_XUnits_VGrid(final GC gcChart,
											final GC gcGraph,
											final GraphDrawingData graphDrawingData,
											final boolean isDrawUnit) {

		final Display display = getDisplay();

		final ArrayList<ChartUnit> xUnits = graphDrawingData.getXUnits();

		final ChartDataXSerie xData = graphDrawingData.getXData();
		final int devYBottom = graphDrawingData.getDevYBottom();
		final int xUnitTextPos = graphDrawingData.getXUnitTextPos();
		double scaleX = graphDrawingData.getScaleX();

		final boolean isHistory = graphDrawingData.getChartType() == ChartType.HISTORY;
		final boolean isDrawVerticalGrid = _chart.isShowVerticalGridLines || isHistory;
		final boolean[] isDrawUnits = graphDrawingData.isDrawUnits();
		final boolean isXUnitOverlapChecked = graphDrawingData.isXUnitOverlapChecked();
		final double devGraphWidth = graphDrawingData.devVirtualGraphWidth;
		final boolean isCheckUnitBorderOverlap = graphDrawingData.isCheckUnitBorderOverlap();

		final double scalingFactor = xData.getScalingFactor();
		final double scalingMaxValue = xData.getScalingMaxValue();
		final boolean isExtendedScaling = scalingFactor != 1.0;
		final double extScaleX = ((devGraphWidth - 1) / Math.pow(scalingMaxValue, scalingFactor));

		// check if the x-units has a special scaling
		final double scaleUnitX = graphDrawingData.getScaleUnitX();
		if (scaleUnitX != Double.MIN_VALUE) {
			scaleX = scaleUnitX;
		}

		int unitCounter = 0;
		final int devVisibleChartWidth = getDevVisibleChartWidth();

		boolean isFirstUnit = true;
		int devXLastUnitRightPosition = -1;

		final String unitLabel = graphDrawingData.getXData().getUnitLabel();
		final int devUnitLabelWidth = gcChart.textExtent(unitLabel).x;

		gcChart.setForeground(isHistory //
				? display.getSystemColor(SWT.COLOR_BLACK)
				: display.getSystemColor(SWT.COLOR_DARK_GRAY));

		gcGraph.setForeground(_gridColor);

		final int xUnitSize = xUnits.size();
		int devNextXUnitTick = Integer.MIN_VALUE;
		int devUnitWidth = 0;
		ChartUnit nextXUnit = null;

		unitLoop:

		for (int unitIndex = 0; unitIndex < xUnitSize; unitIndex++) {

			/*
			 * get unit tick position and the width to the next unit tick
			 */
			int devXUnitTick;
			ChartUnit xUnit = null;
			if (nextXUnit != null) {

				xUnit = nextXUnit;

				devXUnitTick = devNextXUnitTick;

			} else {

				// this is the first unit

				xUnit = xUnits.get(unitIndex);

				// get dev x-position for the unit tick
				if (isExtendedScaling) {

					// extended scaling
					final double scaledUnitValue = ((Math.pow(xUnit.value, scalingFactor)) * extScaleX);
					devXUnitTick = (int) (scaledUnitValue);

				} else {
					// scale with devXOffset
					devXUnitTick = (int) (scaleX * xUnit.value - _xxDevViewPortLeftBorder);
				}
			}

			if (unitIndex < xUnitSize - 1) {

				nextXUnit = xUnits.get(unitIndex + 1);

				// get dev x-position for the unit tick
				if (isExtendedScaling) {

					// extended scaling
					final double scaledUnitValue = ((Math.pow(nextXUnit.value, scalingFactor)) * extScaleX);
					devNextXUnitTick = (int) (scaledUnitValue);

				} else {
					// scale with devXOffset
					devNextXUnitTick = (int) (scaleX * nextXUnit.value - _xxDevViewPortLeftBorder);
				}

				devUnitWidth = devNextXUnitTick - devXUnitTick;
			}

			/*
			 * skip units which are outside of the visible area
			 */
			if (devXUnitTick < 0 && devNextXUnitTick < 0) {
				continue;
			}
			if (devXUnitTick > devVisibleChartWidth) {
				break;
			}

			if (isDrawUnit) {

				/*
				 * draw unit tick
				 */
				if (devXUnitTick > 0 && (isDrawUnits == null || isDrawUnits[unitCounter])) {

					// draw unit tick, don't draw it on the vertical 0 line

					gcChart.setLineStyle(SWT.LINE_SOLID);
					gcChart.drawLine(devXUnitTick, devYBottom, devXUnitTick, devYBottom + 5);
				}

				/*
				 * draw unit value
				 */
				final int devUnitValueWidth = gcChart.textExtent(xUnit.valueLabel).x;

				if (devUnitWidth != 0 && xUnitTextPos == GraphDrawingData.X_UNIT_TEXT_POS_CENTER) {

					/*
					 * draw unit value BETWEEN two units
					 */

					final int devXUnitCenter = (devUnitWidth - devUnitValueWidth) / 2;
					int devXUnitLabelPosition = devXUnitTick + devXUnitCenter;

					if (isCheckUnitBorderOverlap && devXUnitLabelPosition < 0) {
						// position could be < 0 for the first unit
						devXUnitLabelPosition = 0;
					}

					if (isXUnitOverlapChecked == false && devXUnitLabelPosition <= devXLastUnitRightPosition) {

						// skip unit when it overlaps the previous unit

					} else {

						gcChart.drawText(xUnit.valueLabel, devXUnitLabelPosition, devYBottom + 7, true);

						devXLastUnitRightPosition = devXUnitLabelPosition + devUnitValueWidth + 0;
					}

				} else {

					/*
					 * draw unit value in the MIDDLE of the unit tick
					 */

					final int devUnitValueWidth2 = devUnitValueWidth / 2;
					int devXUnitValueDefaultPosition = devXUnitTick - devUnitValueWidth2;

					if (devXUnitTick + devUnitValueWidth2 < 0) {
						// unit label is not visible
						continue;
					}

					if (isFirstUnit) {

						// draw first unit

						isFirstUnit = false;

						/*
						 * this is the first unit, do not center it on the unit tick, because it
						 * would be clipped on the left border
						 */
						int devXUnit = devXUnitValueDefaultPosition;

						if (isCheckUnitBorderOverlap && devXUnit < 0) {
							devXUnit = 0;
						}

						gcChart.drawText(xUnit.valueLabel, devXUnit, devYBottom + 7, true);

						/*
						 * draw unit label (km, mi, h)
						 */
						final int devXUnitLabel = devXUnit + devUnitValueWidth + 2;

						gcChart.drawText(unitLabel,//
								devXUnitLabel,
								devYBottom + 7,
								true);

						devXLastUnitRightPosition = devXUnitLabel + devUnitLabelWidth + 2;

					} else {

						// draw subsequent units

						if (devXUnitValueDefaultPosition >= 0) {

							/*
							 * check if the unit value would be clipped at the right border, move it
							 * to the left to make it fully visible
							 */
							if ((devXUnitTick + devUnitValueWidth2) > devVisibleChartWidth) {

								devXUnitValueDefaultPosition = devVisibleChartWidth - devUnitValueWidth;

								// check if the unit value is overlapping the previous unit value
								if (devXUnitValueDefaultPosition <= devXLastUnitRightPosition + 2) {
									break unitLoop;
								}
							}

							if (devXUnitValueDefaultPosition > devXLastUnitRightPosition) {

								gcChart.drawText(xUnit.valueLabel, devXUnitValueDefaultPosition, devYBottom + 7, true);

								devXLastUnitRightPosition = devXUnitValueDefaultPosition + devUnitValueWidth + 2;
							}
						}
					}
				}
			}

			// draw vertical gridline but not on the vertical 0 line
			if (devXUnitTick > 0 && isDrawVerticalGrid) {

				if (xUnit.isMajorValue) {
					gcGraph.setLineStyle(SWT.LINE_SOLID);
					gcGraph.setForeground(_gridColorMajor);
				} else {
					/*
					 * line width is a complicated topic, when it's not set the gridlines of the
					 * first graph is different than the subsequent graphs, but setting it globally
					 * degrades performance dramatically
					 */
//					gcGraph.setLineWidth(0);
					gcGraph.setLineDash(DOT_DASHES);
					gcGraph.setForeground(_gridColor);
				}
				gcGraph.drawLine(devXUnitTick, 0, devXUnitTick, graphDrawingData.devGraphHeight);

			}

			unitCounter++;
		}
	}

	/**
	 * draw the horizontal gridlines or the x-axis
	 * 
	 * @param gcGraph
	 * @param drawingData
	 * @param isDrawOnlyXAsis
	 */
	private void drawAsync_220_HGrid(final GC gcGraph, final GraphDrawingData drawingData) {

		if (_chart.isShowHorizontalGridLines == false) {
			// h-grid is not visible
			return;
		}

		final ArrayList<ChartUnit> yUnits = drawingData.getYUnits();
		final int unitListSize = yUnits.size();

		final double scaleY = drawingData.getScaleY();
		final float graphYBottom = drawingData.getGraphYBottom();
		final int devGraphHeight = drawingData.devGraphHeight;
		final int devVisibleChartWidth = getDevVisibleChartWidth();

		final boolean isBottomUp = drawingData.getYData().isYAxisDirection();
		final boolean isTopDown = isBottomUp == false;

		final int devYTop = 0;
		final int devYBottom = devGraphHeight;

		int devY;
		int unitIndex = 0;

		// loop: all units
		for (final ChartUnit yUnit : yUnits) {

			final double unitValue = yUnit.value;
			final double devYUnit = (((unitValue - graphYBottom) * scaleY) + 0.5);

			if (isBottomUp || unitListSize == 1) {
				devY = devYBottom - (int) devYUnit;
			} else {
				devY = devYTop + (int) devYUnit;
			}

			// check if a y-unit is on the x axis
			final boolean isXAxis = (isTopDown && unitIndex == unitListSize - 1) || //
					(isBottomUp && unitIndex == 0);

			if (isXAxis == false) {

				// draw gridlines

				if (yUnit.isMajorValue) {
					gcGraph.setLineStyle(SWT.LINE_SOLID);
					gcGraph.setForeground(_gridColorMajor);
				} else {
					gcGraph.setLineDash(DOT_DASHES);
					gcGraph.setForeground(_gridColor);
				}
				gcGraph.drawLine(0, devY, devVisibleChartWidth, devY);
			}

			unitIndex++;
		}
	}

	/**
	 * draw the horizontal gridlines or the x-axis
	 * 
	 * @param gcGraph
	 * @param drawingData
	 * @param isDrawOnlyXAsis
	 */
	private void drawAsync_230_XAxis(final GC gcGraph, final GraphDrawingData drawingData) {

		final Display display = getDisplay();

		final ArrayList<ChartUnit> yUnits = drawingData.getYUnits();
		final int unitListSize = yUnits.size();

		final double scaleY = drawingData.getScaleY();
		final float graphYBottom = drawingData.getGraphYBottom();
		final int devGraphHeight = drawingData.devGraphHeight;
		final int devVisibleChartWidth = getDevVisibleChartWidth();

		final boolean isBottomUp = drawingData.getYData().isYAxisDirection();
		final boolean isTopDown = isBottomUp == false;

		final int devYTop = 0;
		final int devYBottom = devGraphHeight;

		int devY;
		int unitIndex = 0;

		// loop: all units
		for (final ChartUnit yUnit : yUnits) {

			final double unitValue = yUnit.value;
			final double devYUnit = (((unitValue - graphYBottom) * scaleY) + 0.5);

			if (isBottomUp || unitListSize == 1) {
				devY = devYBottom - (int) devYUnit;
			} else {
				devY = devYTop + (int) devYUnit;
			}

			// check if a y-unit is on the x axis
			final boolean isXAxis = (isTopDown && unitIndex == unitListSize - 1) //
					|| (isBottomUp && unitIndex == 0);

			if (isXAxis) {

				gcGraph.setLineStyle(SWT.LINE_SOLID);
				gcGraph.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
				gcGraph.drawLine(0, devY, devVisibleChartWidth, devY);

				// only the x-axis needs to be drawn
				break;
			}

			unitIndex++;
		}
	}

	private void drawAsync_240_TourSements(final GC gcChart, final GraphDrawingData graphDrawingData) {

		final ArrayList<ChartSegment> tourSegments = _chartDrawingData.tourSegments;

		if (tourSegments.size() == 1) {
			// ignore one tour chart
			return;
		}

		final Display display = getDisplay();

		gcChart.setLineAttributes(LINE_DASHED);
		gcChart.setForeground(display.getSystemColor(SWT.COLOR_GRAY));

		for (int segmentIndex = 0; segmentIndex < tourSegments.size(); segmentIndex++) {

			final ChartSegment chartSegment = tourSegments.get(segmentIndex);
			if (chartSegmentConfig.isShowSegmentSeparator) {

				// draw segment start line but not for the first segment
				if (segmentIndex != 0) {

//					gc.drawLine(devXSegmentStart, 1, devXSegmentStart, devYTitle - 1);
					gcChart.drawLine(//
							chartSegment.devXSegment,
							0,//graphDrawingData.getDevYTop(),
							chartSegment.devXSegment,
							graphDrawingData.getDevYBottom());
				}
			}
		}

		gcChart.setLineStyle(SWT.LINE_SOLID);
	}

	private void drawAsync_500_LineGraph(	final GC gcGraph,
											final GraphDrawingData graphDrawingData,
											final boolean isTopGraph) {

		final ChartDataXSerie xData = graphDrawingData.getXData();
		final ChartDataYSerie yData = graphDrawingData.getYData();

		final int serieSize = xData.getHighValuesDouble()[0].length;
		final double scaleX = graphDrawingData.getScaleX();

		// create line hovered positions
		_lineFocusRectangles.add(new RectangleLong[serieSize]);
		_lineDevPositions.add(new PointLong[serieSize]);
		_isHoveredLineVisible = true;

		final RGB rgbFg = yData.getRgbLine()[0];
		final RGB rgbBgDark = yData.getRgbDark()[0];
		final RGB rgbBgBright = yData.getRgbBright()[0];

		// get the horizontal offset for the graph
		float graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = (float) (Math.max(0, _xxDevViewPortLeftBorder) / scaleX);
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = (float) (_xxDevViewPortLeftBorder / scaleX);
		}

		final int synchMarkerStartIndex = xData.getSynchMarkerStartIndex();
		final int synchMarkerEndIndex = xData.getSynchMarkerEndIndex();

		if (synchMarkerStartIndex == -1) {

			// synch marker is not displayed

			final int graphLineAlpha = getAlphaLine();
			final int graphFillingAlpha = getAlphaFill(isTopGraph);

			drawAsync_510_LineGraphSegment(
					gcGraph,
					graphDrawingData,
					0,
					serieSize,
					rgbFg,
					rgbBgDark,
					rgbBgBright,
					graphLineAlpha,
					graphFillingAlpha,
					graphValueOffset);

		} else {

			// draw synched tour

			final double noneMarkerAlpha = 0.4;

			final int noneMarkerLineAlpha = (int) (_chart.graphTransparencyLine * noneMarkerAlpha);
			final int noneMarkerFillingAlpha = (int) (_chart.graphTransparencyFilling * noneMarkerAlpha);

			// draw graph without marker
			drawAsync_510_LineGraphSegment(
					gcGraph,
					graphDrawingData,
					0,
					serieSize,
					rgbFg,
					rgbBgDark,
					rgbBgBright,
					noneMarkerLineAlpha,
					noneMarkerFillingAlpha,
					graphValueOffset);

			// draw the x-marker
			drawAsync_510_LineGraphSegment(
					gcGraph,
					graphDrawingData,
					synchMarkerStartIndex,
					synchMarkerEndIndex + 0,
					rgbFg,
					rgbBgDark,
					rgbBgBright,
					_chart.graphTransparencyLine,
					_chart.graphTransparencyFilling,
					graphValueOffset);
		}
	}

	/**
	 * First draw the graph into a path, the path is then drawn on the device with a transformation.
	 * 
	 * @param gc
	 * @param graphDrawingData
	 * @param startIndex
	 * @param endIndex
	 * @param rgbFg
	 * @param rgbBgDark
	 * @param rgbBgBright
	 * @param graphLineAlpha
	 * @param graphFillingAlpha
	 * @param graphValueOffset
	 * @param isFillGraph
	 * @param isSegmented
	 *            When <code>true</code> the whole graph is painted with several segments, otherwise
	 *            the whole graph is painted once.
	 */
	private void drawAsync_510_LineGraphSegment(final GC gc,
												final GraphDrawingData graphDrawingData,
												final int startIndex,
												final int endIndex,
												final RGB rgbFg,
												final RGB rgbBgDark,
												final RGB rgbBgBright,
												final int graphLineAlpha,
												final int graphFillingAlpha,
												final double graphValueOffset) {

		final ChartDataXSerie xData = graphDrawingData.getXData();
		final ChartDataYSerie yData = graphDrawingData.getYData();

		final float[][] yHighValues = yData.getHighValuesFloat();

		final double[] xValues = xData.getHighValuesDouble()[0];
		final float yValues[] = yHighValues[0];

		final int graphFillMethod = yData.getGraphFillMethod();
		final boolean[] noFill = xData.getNoLine();
		final boolean[] lineGaps = yData.getLineGaps();

		// check array bounds
		final int xValueLength = xValues.length;
		if (startIndex >= xValueLength) {
			return;
		}
		final int yValueLength = yValues.length;

		/*
		 * 2nd path is currently used to draw the SRTM altitude line
		 */
		final boolean isPath2 = yHighValues.length > 1;
		float[] yValues2 = null;
		if (isPath2) {
			yValues2 = yHighValues[1];
		}

		// get top/bottom border values of the graph
		final float graphYBorderTop = graphDrawingData.getGraphYTop();
		final float graphYBorderBottom = graphDrawingData.getGraphYBottom();
		final int devYTop = graphDrawingData.getDevYTop();
		final int devChartHeight = getDevVisibleGraphHeight();

		final double scaleX = graphDrawingData.getScaleX();
		final double scaleY = graphDrawingData.getScaleY();

// this feature also needs that the y-axis is scaled accordingly -> this not yet implemted
//
//		if (_canChartBeOverlapped && _isChartOverlapped) {
//
//			// reduce scale for overlapped graphs
//
//			if (!isTopGraph) {
//
//				scaleY *= 0.6;
//			}
//		}

		final Display display = getDisplay();

		// path is scaled in device pixel
		final Path path = new Path(display);
		final Path path2 = isPath2 ? new Path(display) : null;

		final boolean isShowSkippedValues = _chartDrawingData.chartDataModel.isNoLinesValuesDisplayed();
		final ArrayList<Point> skippedValues = new ArrayList<Point>();

		final int devGraphHeight = graphDrawingData.devGraphHeight;
		final float devYGraphTop = (float) (scaleY * graphYBorderTop);
		final float devYGraphBottom = (float) (scaleY * graphYBorderBottom);

		final RectangleLong[] lineFocusRectangles = _lineFocusRectangles.get(_lineFocusRectangles.size() - 1);
		final PointLong[] lineDevPositions = _lineDevPositions.get(_lineDevPositions.size() - 1);
		RectangleLong prevLineRect = null;

		/*
		 * 
		 */
		final float devY0Inverse = devGraphHeight + devYGraphBottom;

		/*
		 * x-axis line with y==0
		 */
		float graphY_XAxisLine = 0;

		if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
				|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM
//				|| graphFillMethod == ChartDataYSerie.FILL_METHOD_NO
		) {

			graphY_XAxisLine = graphYBorderBottom > 0 //
					? graphYBorderBottom
					: graphYBorderTop < 0 //
							? graphYBorderTop
							: graphYBorderBottom;

		} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

			graphY_XAxisLine = graphYBorderBottom > 0 ? graphYBorderBottom //
					: graphYBorderTop < 0 ? graphYBorderTop //
							: 0;
		}
		final float devY_XAxisLine = (float) (scaleY * graphY_XAxisLine);

		final double graphXStart = xValues[startIndex] - graphValueOffset;
		final float graphYStart = yValues[startIndex];

		float graphY1Prev = graphYStart;

		double devXPrev = (scaleX * graphXStart);
		float devY1Prev = (float) (scaleY * graphY1Prev);

		double devXPrevNoLine = 0;
		boolean isNoLine = false;
		boolean isLineGap = false;

		final Rectangle chartRectangle = gc.getClipping();
		final int devXVisibleWidth = chartRectangle.width;

		boolean isDrawFirstPoint = true;

		final int lastIndex = endIndex - 1;

		int valueIndexFirstPoint = startIndex;
		int valueIndexLastPoint = startIndex;
		int prevValueIndex = startIndex;

		/*
		 * set the hovered index only ONCE because when autoscrolling is done to the right side this
		 * can cause that the last value is used for the hovered index instead of the previous
		 * before the last
		 */
		boolean isSetHoveredIndex = false;

		final long[] devXPositions = new long[endIndex];
		final float devY0 = devY0Inverse - devY_XAxisLine;

		/*
		 * draw the lines into the paths
		 */
		for (int valueIndex = startIndex; valueIndex < endIndex; valueIndex++) {

			// check array bounds
			if (valueIndex >= yValueLength) {
				break;
			}

			final double graphX = xValues[valueIndex] - graphValueOffset;
			final double devX = graphX * scaleX;
			final float devXf = (float) devX;

			final float graphY1 = yValues[valueIndex];
			final float devY1 = (float) (graphY1 * scaleY);

			float graphY2 = 0;
			float devY2 = 0;

			if (isPath2) {
				graphY2 = yValues2[valueIndex];
				devY2 = (float) (graphY2 * scaleY);
			}

			devXPositions[valueIndex] = (long) devX;

			// check if position is horizontal visible
			if (devX < 0) {

				// keep current position which is used as the painting starting point

				graphY1Prev = graphY1;

				devXPrev = devX;
				devY1Prev = devY1;

				valueIndexFirstPoint = valueIndex;
				prevValueIndex = valueIndex;

				continue;
			}

			/*
			 * draw first point
			 */
			if (isDrawFirstPoint) {

				// move to the first point

				isDrawFirstPoint = false;

				// set first point before devX==0 that the first line is not visible but correctly painted
				final double devXFirstPoint = devXPrev;
				float devXFirstPointF = (float) devXPrev;

				if (devXFirstPointF <= 0.0f) {
					/*
					 * Hide the first line from the bottom to the first value point by setting the
					 * position into the hidden area.
					 */
					devXFirstPointF -= 1f;
				}

				float devYStart = 0;

				if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
						|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM
						|| graphFillMethod == ChartDataYSerie.FILL_METHOD_NO) {

					// start from the bottom of the graph

					devYStart = devGraphHeight;

				} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

					// start from the x-axis, y=0

					devYStart = devY0;
				}

				final float devY = devY0Inverse - devY1Prev;

				path.moveTo((int) devXFirstPointF, (int) devYStart);
				path.lineTo((int) devXFirstPointF, (int) devY);

				if (isPath2) {
					path2.moveTo(devXFirstPointF, devY0Inverse - devY2);
					path2.lineTo(devXFirstPointF, devY0Inverse - devY2);
				}

				/*
				 * set line hover positions for the first point
				 */
				final long devXRect = (long) devXFirstPoint;

				final RectangleLong currentRect = new RectangleLong(devXRect, 0, 1, devChartHeight);
				final PointLong currentPoint = new PointLong(devXRect, (long) (devYTop + devY));

				lineDevPositions[valueIndexFirstPoint] = currentPoint;
				lineFocusRectangles[valueIndexFirstPoint] = currentRect;

				prevLineRect = currentRect;
			}

			/*
			 * draw line to current point
			 */
			if ((long) devX != (long) devXPrev || graphY1 == 0 || (isPath2 && graphY2 == 0)) {

				// optimization: draw only ONE line for the current x-position
				// but draw to the 0 line otherwise it's possible that a triangle is painted

				float devY = 0;

				if (lineGaps != null && lineGaps[valueIndex]) {

					isLineGap = true;

					// keep correct position that the hovered line dev position is painted at the correct position
					devY = devY0Inverse - devY1;

				} else if (noFill != null && noFill[valueIndex]) {

					/*
					 * draw NO line, but draw a line at the bottom or the x-axis with y=0
					 */

					if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
							|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM
							|| graphFillMethod == ChartDataYSerie.FILL_METHOD_NO) {

						// start from the bottom of the graph

						devY = devGraphHeight;

					} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

						// start from the x-axis, y=0

						devY = devY0;
					}

					/*
					 * Don't draw "fill" line when graph is not filled
					 */
					if (graphFillMethod != ChartDataYSerie.FILL_METHOD_NO) {

						path.lineTo((int) devXPrev, (int) devY);
						path.lineTo((int) devXf, (int) devY);
					}

					/*
					 * keep positions, because skipped values will be painted as a dot outside of
					 * the path, but don't draw on the graph bottom or x-axis
					 */
					if (isShowSkippedValues) {

						final int devYSkipped = (int) (devY0Inverse - devY1);
						if (devYSkipped != devY0 && graphY1 != 0) {
							skippedValues.add(new Point((int) devX, devYSkipped));
						}
					}

					isNoLine = true;
					devXPrevNoLine = devX;

					// keep correct position that the hovered line dev position is painted at the correct position
					devY = devY0Inverse - devY1;

				} else {

					/*
					 * draw line to the current point
					 */

					if (isLineGap) {

						isLineGap = false;

						devY = devY0Inverse - devY1;

						path.moveTo(devXf, devY);

					} else {

						// check if a NO line was painted
						if (isNoLine) {

							isNoLine = false;

							path.lineTo((int) devXPrevNoLine, (int) (devY0Inverse - devY1Prev));
						}

						devY = devY0Inverse - devY1;

						path.lineTo(devXf, devY);

						if (isPath2) {
							path2.lineTo(devXf, devY0Inverse - devY2);
						}
					}
				}

				/*
				 * set line hover positions
				 */
				final double devXDiff = (devX - devXPrev) / 2;
				final double devXDiffWidth = devXDiff < 1 ? 1 : (devXDiff + 0.5);
				final double devXRect = devX - devXDiffWidth;

				// add the right part of the rectangle width into the previous rectangle
				prevLineRect.width += devXDiffWidth + 1;

				// check if hovered line is hit, this check is an inline for .contains(...)
				if (isSetHoveredIndex == false && prevLineRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredValuePointIndex = prevValueIndex;
					isSetHoveredIndex = true;
				}

				final RectangleLong currentRect = new RectangleLong(
						(long) devXRect,
						0,
						(long) (devXDiffWidth + 1),
						devChartHeight);
				final PointLong currentPoint = new PointLong((long) devX, (long) (devYTop + devY));

				lineDevPositions[valueIndex] = currentPoint;
				lineFocusRectangles[valueIndex] = currentRect;

				prevLineRect = currentRect;
			}

			/*
			 * draw last point
			 */
			if (valueIndex == lastIndex || //

					// check if last visible position + 1 is reached
					devX > devXVisibleWidth) {

				/*
				 * this is the last point for a filled graph
				 */

				final float devY = devY0Inverse - devY1;

				path.lineTo((int) devXf, (int) devY);

				// move path to the final point
				if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
						|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM
						|| graphFillMethod == ChartDataYSerie.FILL_METHOD_NO) {

					// draw line to the bottom of the graph

					path.lineTo((int) devXf, devGraphHeight);

				} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

					// draw line to the x-axis, y=0

					path.lineTo((int) devXf, (int) devY0);
				}

				// moveTo() is necessary that the graph is filled correctly (to prevent a triangle filled shape)
				// finalize previous subpath
				path.moveTo((int) devXf, 0);

				if (isPath2) {
					path2.lineTo(devXf, devY0Inverse - devY2);
					path2.moveTo(devXf, 0);
				}

				valueIndexLastPoint = valueIndex;

				/*
				 * set line rectangle
				 */
				final double devXDiff = (devX - devXPrev) / 2;
				final long devXDiffWidth = devXDiff < 1 ? 1 : (int) (devXDiff + 0.5);
				final long devXRect = (long) (devX - devXDiffWidth);

				// set right part of the rectangle width into the previous rectangle
				prevLineRect.width += devXDiffWidth;

				// check if hovered line is hit, this check is an inline for .contains(...)
				if (isSetHoveredIndex == false && prevLineRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredValuePointIndex = valueIndex;
					isSetHoveredIndex = true;
				}

				final RectangleLong lastRect = new RectangleLong(devXRect, 0, devXDiffWidth + 1, devChartHeight);
				final PointLong lastPoint = new PointLong((long) devX, devYTop + (long) devY);

				lineDevPositions[valueIndex] = lastPoint;
				lineFocusRectangles[valueIndex] = lastRect;

				if (isSetHoveredIndex == false && lastRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredValuePointIndex = valueIndex;
				}

				break;
			}

			devXPrev = devX;
			devY1Prev = devY1;
			prevValueIndex = valueIndex;
		}

		final Color colorLine = new Color(display, rgbFg);
		final Color colorBgDark = new Color(display, rgbBgDark);
		final Color colorBgBright = new Color(display, rgbBgBright);

		final double graphWidth = xValues[Math.min(xValueLength - 1, endIndex)] - graphValueOffset;

		/**
		 * force a max width because the fill will not be drawn on Linux
		 */
		final int devGraphWidth = Math.min(0x7fff, (int) (graphWidth * scaleX));

		gc.setAntialias(_chart.graphAntialiasing);
		gc.setAlpha(graphFillingAlpha);
		gc.setClipping(path);

		/*
		 * fill the graph
		 */
		if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM) {

			/*
			 * adjust the fill gradient in the height, otherwise the fill is not in the whole
			 * rectangle
			 */

			gc.setForeground(colorBgDark);
			gc.setBackground(colorBgBright);

			gc.fillGradientRectangle(//
					0,
					devGraphHeight,
					devGraphWidth,
					-devGraphHeight,
					true);

		} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

			/*
			 * fill above 0 line
			 */

			gc.setForeground(colorBgDark);
			gc.setBackground(colorBgBright);

			gc.fillGradientRectangle(//
					0,
					(int) devY0,
					devGraphWidth,
					-(int) (devYGraphTop - devY_XAxisLine),
					true);

			/*
			 * fill below 0 line
			 */
			gc.setForeground(colorBgBright);
			gc.setBackground(colorBgDark);

			gc.fillGradientRectangle(//
					0,
					devGraphHeight, // start from the graph bottom
					devGraphWidth,
					-(int) Math.min(devGraphHeight, devGraphHeight - devY0Inverse),
					true);

		} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM) {

			final IFillPainter customFillPainter = yData.getCustomFillPainter();

			if (customFillPainter != null) {

				gc.setForeground(colorBgDark);
				gc.setBackground(colorBgBright);

				customFillPainter.draw(
						gc,
						graphDrawingData,
						_chart,
						devXPositions,
						valueIndexFirstPoint,
						valueIndexLastPoint);
			}
		}

		// reset clipping that the line is drawn everywere
		gc.setClipping((Rectangle) null);

		gc.setBackground(colorLine);

		/*
		 * paint skipped values
		 */
		if (isShowSkippedValues && skippedValues.size() > 0) {
			for (final Point skippedPoint : skippedValues) {
				gc.fillRectangle(skippedPoint.x, skippedPoint.y, 2, 2);
			}
		}

		/*
		 * draw line along the path
		 */
		gc.setAlpha(graphLineAlpha);

		// set line style
		gc.setLineStyle(SWT.LINE_SOLID);

		// draw the line of the graph
		gc.setForeground(colorLine);

		gc.drawPath(path);

		// dispose resources
		colorLine.dispose();
		colorBgDark.dispose();
		colorBgBright.dispose();

		path.dispose();

		/*
		 * draw path2 above the other graph, this is currently used to draw the srtm graph
		 */
		if (path2 != null) {

			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			gc.drawPath(path2);

			path2.dispose();
		}

		gc.setAlpha(0xFF);
		gc.setAntialias(SWT.OFF);
	}

	private void drawAsync_520_RangeMarker(final GC gc, final GraphDrawingData drawingData) {

		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();

		final int[] startIndex = xData.getRangeMarkerStartIndex();
		final int[] endIndex = xData.getRangeMarkerEndIndex();

		if (startIndex == null) {
			return;
		}

		final double scaleX = drawingData.getScaleX();

		final RGB rgbFg = yData.getRgbLine()[0];
		final RGB rgbBg1 = yData.getRgbDark()[0];
		final RGB rgbBg2 = yData.getRgbBright()[0];

		// get the horizontal offset for the graph
		double graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = (Math.max(0, _xxDevViewPortLeftBorder) / scaleX);
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = (_xxDevViewPortLeftBorder / scaleX);
		}

		int graphFillingAlpha = (int) (_chart.graphTransparencyFilling * 0.5);
		int graphLineAlpha = (int) (_chart.graphTransparencyFilling * 0.5);

		graphFillingAlpha = graphFillingAlpha < 0 ? 0 : graphFillingAlpha > 255 ? 255 : graphFillingAlpha;
		graphLineAlpha = graphLineAlpha < 0 ? 0 : graphLineAlpha > 255 ? 255 : graphLineAlpha;

		int runningIndex = 0;
		for (final int markerStartIndex : startIndex) {

			// draw range marker
			drawAsync_510_LineGraphSegment(gc, //
					drawingData,
					markerStartIndex,
					endIndex[runningIndex] + 1,
					rgbFg,
					rgbBg1,
					rgbBg2,
					graphLineAlpha,
					graphFillingAlpha,
					graphValueOffset);

			runningIndex++;
		}
	}

	/**
	 * Draws a bar graph, this requires that drawingData.getChartData2ndValues does not return null,
	 * if null is returned, a line graph will be drawn instead
	 * 
	 * @param gcGraph
	 * @param drawingData
	 */
	private void drawAsync_530_BarGraph(final GC gcGraph, final GraphDrawingData drawingData) {

		// get the chart data
		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();
		final int[][] colorsIndex = yData.getColorsIndex();
		final int graphFillMethod = yData.getGraphFillMethod();

		gcGraph.setLineStyle(SWT.LINE_SOLID);

		// get the colors
		final RGB[] rgbLine = yData.getRgbLine();
		final RGB[] rgbDark = yData.getRgbDark();
		final RGB[] rgbBright = yData.getRgbBright();

		// get the chart values
		final double scaleX = drawingData.getScaleX();
		final double scaleY = drawingData.getScaleY();
		final float graphYBorderBottom = drawingData.getGraphYBottom();
		final boolean isBottomTop = yData.isYAxisDirection();

		// get the horizontal offset for the graph
		float graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a synch marker is not set, draw it normally
			graphValueOffset = (float) (Math.max(0, _xxDevViewPortLeftBorder) / scaleX);
		} else {
			// adjust the start position to the synch marker position
			graphValueOffset = (float) (_xxDevViewPortLeftBorder / scaleX);
		}

		final int devGraphCanvasHeight = drawingData.devGraphHeight;

		/*
		 * Get the top/bottom for the graph, a chart can contain multiple canvas. Canvas is the area
		 * where the graph is painted.
		 */
		final int devYCanvasBottom = devGraphCanvasHeight;
		final int devYCanvasTop = 0;

		final int devYChartBottom = drawingData.getDevYBottom();
		final int devYChartTop = devYChartBottom - devGraphCanvasHeight;

		final double[] xValues = xData.getHighValuesDouble()[0];
		final float yHighSeries[][] = yData.getHighValuesFloat();
		final float yLowSeries[][] = yData.getLowValuesFloat();

		final int serieLength = yHighSeries.length;
		final int valueLength = xValues.length;

		// keep the bar rectangles for all canvas
		final Rectangle[][] barRecangles = new Rectangle[serieLength][valueLength];
		final Rectangle[][] barFocusRecangles = new Rectangle[serieLength][valueLength];
		drawingData.setBarRectangles(barRecangles);
		drawingData.setBarFocusRectangles(barFocusRecangles);

		// keep the height for stacked bar charts
		final int devHeightSummary[] = new int[valueLength];

		final int devBarWidthOriginal = drawingData.getBarRectangleWidth();
		final int devBarWidth = Math.max(1, devBarWidthOriginal);
		final int devBarWidth2 = devBarWidth / 2;

		final int serieLayout = yData.getChartLayout();
		final int devBarRectangleStartXPos = drawingData.getDevBarRectangleXPos();
		final int barPosition = drawingData.getBarPosition();

		// loop: all data series
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			final float yHighValues[] = yHighSeries[serieIndex];
			float yLowValues[] = null;
			if (yLowSeries != null) {
				yLowValues = yLowSeries[serieIndex];
			}

			int devBarXPos = devBarRectangleStartXPos;
			int devBarWidthPositioned = devBarWidth;

			// reposition the rectangle when the bars are beside each other
			if (serieLayout == ChartDataYSerie.BAR_LAYOUT_BESIDE) {
				devBarXPos += serieIndex * devBarWidth;
				devBarWidthPositioned = devBarWidth - 1;
			}

			int devXPosNextBar = 0;

			// loop: all values in the current serie
			for (int valueIndex = 0; valueIndex < valueLength; valueIndex++) {

				// get the x position
				int devXPos = (int) ((xValues[valueIndex] - graphValueOffset) * scaleX) + devBarXPos;

				// center the bar
				if (devBarWidth > 1 && barPosition == GraphDrawingData.BAR_POS_CENTER) {
					devXPos -= devBarWidth2;
				}

				float valueYLow = 0;

				if (graphFillMethod == ChartDataYSerie.BAR_DRAW_METHOD_BOTTOM) {

					// draw at the bottom
					valueYLow = graphYBorderBottom;

				} else {

					if (yLowValues == null) {
						valueYLow = (float) yData.getVisibleMinValue();
					} else {
						// check array bounds
						if (valueIndex >= yLowValues.length) {
							break;
						}
						valueYLow = yLowValues[valueIndex];
					}
				}

				// check array bounds
				if (valueIndex >= yHighValues.length) {
					break;
				}
				final float valueYHigh = yHighValues[valueIndex];

				final float barHeight = (Math.max(valueYHigh, valueYLow) - Math.min(valueYHigh, valueYLow));
				if (barHeight == 0) {
					continue;
				}

				final int devBarHeight = (int) (barHeight * scaleY);

				// get the old y position for stacked bars
				int devYPreviousHeight = 0;
				if (serieLayout == ChartDataYSerie.BAR_LAYOUT_STACKED) {
					devYPreviousHeight = devHeightSummary[valueIndex];
				}

				/*
				 * get y positions
				 */
				int devYPosChart;
				int devYPosCanvas;
				if (isBottomTop) {

					final int devYBar = (int) ((valueYHigh - graphYBorderBottom) * scaleY) + devYPreviousHeight;

					devYPosChart = devYChartBottom - devYBar;
					devYPosCanvas = devYCanvasBottom - devYBar;

				} else {
					final int devYBar = (int) ((valueYLow - graphYBorderBottom) * scaleY) + devYPreviousHeight;

					devYPosChart = devYChartTop + devYBar;
					devYPosCanvas = devYCanvasTop + devYBar;
				}

				int devXPosShape = devXPos;
				int devShapeBarWidth = devBarWidthPositioned;

				/*
				 * make sure the bars do not overlap
				 */
				if (serieLayout != ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE) {
					if (devXPosNextBar > 0) {
						if (devXPos < devXPosNextBar) {

							// bars do overlap

							final int devDiff = devXPosNextBar - devXPos;

							devXPosShape = devXPos + devDiff;
							devShapeBarWidth = devBarWidthPositioned - devDiff;
						}
					}
				}
				devXPosNextBar = devXPos + devBarWidthPositioned;

				/*
				 * get colors
				 */
				final int colorIndex = colorsIndex[serieIndex][valueIndex];
				final RGB rgbBrightDef = rgbBright[colorIndex];
				final RGB rgbDarkDef = rgbDark[colorIndex];
				final RGB rgbLineDef = rgbLine[colorIndex];

				final Color colorBright = getColor(rgbBrightDef);
				final Color colorDark = getColor(rgbDarkDef);
				final Color colorLine = getColor(rgbLineDef);

				gcGraph.setBackground(colorDark);

				/*
				 * draw bar
				 */
				final Rectangle barShapeCanvas = new Rectangle(
						devXPosShape,
						devYPosCanvas,
						devShapeBarWidth,
						devBarHeight);

				if (devBarWidthOriginal > 0) {

					gcGraph.setForeground(colorBright);
					gcGraph.fillGradientRectangle(
							barShapeCanvas.x,
							barShapeCanvas.y,
							barShapeCanvas.width,
							barShapeCanvas.height,
							false);

					gcGraph.setForeground(colorLine);
					gcGraph.drawRectangle(barShapeCanvas);

				} else {

					gcGraph.setForeground(colorLine);
					gcGraph.drawLine(
							barShapeCanvas.x,
							barShapeCanvas.y,
							barShapeCanvas.x,
							(barShapeCanvas.y + barShapeCanvas.height));
				}

				barRecangles[serieIndex][valueIndex] = new Rectangle( //
						devXPosShape,
						devYPosChart,
						devShapeBarWidth,
						devBarHeight);

				barFocusRecangles[serieIndex][valueIndex] = new Rectangle(//
						devXPosShape - 2,
						(devYPosChart - 2),
						devShapeBarWidth + 4,
						(devBarHeight + 7));

				// keep the height for the bar
				devHeightSummary[valueIndex] += devBarHeight;
			}
		}

		// reset clipping
		gcGraph.setClipping((Rectangle) null);
	}

	/**
	 * Draws a bar graph, this requires that drawingData.getChartData2ndValues does not return null,
	 * if null is returned, a line graph will be drawn instead
	 * 
	 * @param gc
	 * @param drawingData
	 */
	private void drawAsync_540_LineWithBarGraph(final GC gc, final GraphDrawingData drawingData) {

		// get the chart data
		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();
		final int[][] colorsIndex = yData.getColorsIndex();

		gc.setLineStyle(SWT.LINE_SOLID);

		// get the colors
		final RGB[] rgbLine = yData.getRgbLine();
		final RGB[] rgbDark = yData.getRgbDark();
		final RGB[] rgbBright = yData.getRgbBright();

		// get the chart values
		final double scaleX = drawingData.getScaleX();
		final double scaleY = drawingData.getScaleY();
		final float graphYBottom = drawingData.getGraphYBottom();
		final boolean axisDirection = yData.isYAxisDirection();
//		final int barPosition = drawingData.getBarPosition();

		// get the horizontal offset for the graph
		float graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = (float) (Math.max(0, _xxDevViewPortLeftBorder) / scaleX);
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = (float) (_xxDevViewPortLeftBorder / scaleX);
		}

		// get the top/bottom of the graph
		final int devYTop = 0;
		final int devYBottom = drawingData.devGraphHeight;

		// virtual 0 line for the y-axis of the chart in dev units
//		final float devChartY0Line = (float) devYBottom + (scaleY * graphYBottom);

		gc.setClipping(0, devYTop, gc.getClipping().width, devYBottom - devYTop);

		final double[] xValues = xData.getHighValuesDouble()[0];
		final float yHighSeries[][] = yData.getHighValuesFloat();
//		final int yLowSeries[][] = yData.getLowValues();

		final int serieLength = yHighSeries.length;
		final int valueLength = xValues.length;

		final int devBarWidthComputed = drawingData.getBarRectangleWidth();
		final int devBarWidth = Math.max(1, devBarWidthComputed);

		final int devBarXPos = drawingData.getDevBarRectangleXPos();

		// loop: all data series
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			final float yHighValues[] = yHighSeries[serieIndex];
//			int yLowValues[] = null;
//			if (yLowSeries != null) {
//				yLowValues = yLowSeries[serieIndex];
//			}

			// loop: all values in the current serie
			for (int valueIndex = 0; valueIndex < valueLength; valueIndex++) {

				// get the x position
				final int devXPos = (int) ((xValues[valueIndex] - graphValueOffset) * scaleX) + devBarXPos;

//				final int devBarWidthSelected = devBarWidth;
//				final int devBarWidth2 = devBarWidthSelected / 2;

//				int devXPosSelected = devXPos;
//
//				// center the bar
//				if (devBarWidthSelected > 1 && barPosition == GraphDrawingData.BAR_POS_CENTER) {
//					devXPosSelected -= devBarWidth2;
//				}

				// get the bar height
				final float graphYLow = graphYBottom;
				final float graphYHigh = yHighValues[valueIndex];

				final float graphBarHeight = Math.max(graphYHigh, graphYLow) - Math.min(graphYHigh, graphYLow);

				// skip bars which have no height
				if (graphBarHeight == 0) {
					continue;
				}

				final int devBarHeight = (int) (graphBarHeight * scaleY);

				// get the y position
				int devYPos;
				if (axisDirection) {
					devYPos = devYBottom - ((int) ((graphYHigh - graphYBottom) * scaleY));
				} else {
					devYPos = devYTop + ((int) ((graphYLow - graphYBottom) * scaleY));
				}

				final Rectangle barShape = new Rectangle(devXPos, devYPos, devBarWidth, devBarHeight);

				final int colorSerieIndex = colorsIndex.length >= serieIndex ? colorsIndex.length - 1 : serieIndex;
				final int colorIndex = colorsIndex[colorSerieIndex][valueIndex];

				final RGB rgbBrightDef = rgbBright[colorIndex];
				final RGB rgbDarkDef = rgbDark[colorIndex];
				final RGB rgbLineDef = rgbLine[colorIndex];

				final Color colorBright = getColor(rgbBrightDef);
				final Color colorDark = getColor(rgbDarkDef);
				final Color colorLine = getColor(rgbLineDef);

				gc.setBackground(colorDark);

				/*
				 * draw bar
				 */
				if (devBarWidthComputed > 0) {

					gc.setForeground(colorBright);
					gc.fillGradientRectangle(barShape.x, barShape.y, barShape.width, barShape.height, false);

					gc.setForeground(colorLine);
					gc.drawRectangle(barShape);

				} else {

					gc.setForeground(colorLine);
					gc.drawLine(barShape.x, barShape.y, barShape.x, (barShape.y + barShape.height));
				}
			}
		}

		// reset clipping
		gc.setClipping((Rectangle) null);
	}

	/**
	 * Draws a bar graph, this requires that drawingData.getChartData2ndValues does not return null,
	 * if null is returned, a line graph will be drawn instead
	 * <p>
	 * <b> Zooming the chart is not yet supported for this charttype because logarithmic scaling is
	 * very complex for a zoomed chart </b>
	 * 
	 * @param gc
	 * @param drawingData
	 */
	private void drawAsync_550_XYScatter(final GC gc, final GraphDrawingData drawingData) {

		// get chart data
		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();
		final double scaleX = drawingData.getScaleX();
		final double scaleY = drawingData.getScaleY();
		final float graphYBottom = drawingData.getGraphYBottom();
		final double devGraphWidth = drawingData.devVirtualGraphWidth;

		final double scalingFactor = xData.getScalingFactor();
		final double scalingMaxValue = xData.getScalingMaxValue();
		final boolean isExtendedScaling = scalingFactor != 1.0;
		final double scaleXExtended = ((devGraphWidth - 1) / Math.pow(scalingMaxValue, scalingFactor));

		// get colors
		final RGB[] rgbLine = yData.getRgbLine();

		// get the top/bottom of the graph
		final int devYTop = 0;
		final int devYBottom = drawingData.devGraphHeight;

//		gc.setAntialias(SWT.ON);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setClipping(0, devYTop, gc.getClipping().width, devYBottom - devYTop);

		final double[][] xSeries = xData.getHighValuesDouble();
		final float[][] ySeries = yData.getHighValuesFloat();
		final int size = 6;
		final int size2 = size / 2;

		for (int serieIndex = 0; serieIndex < xSeries.length; serieIndex++) {

			final double[] xValues = xSeries[serieIndex];
			final float yHighValues[] = ySeries[serieIndex];

			gc.setBackground(getColor(rgbLine[serieIndex]));

			// loop: all values in the current serie
			for (int valueIndex = 0; valueIndex < xValues.length; valueIndex++) {

				// check array bounds
				if (valueIndex >= yHighValues.length) {
					break;
				}

				final double xValue = xValues[valueIndex];
				final float yValue = yHighValues[valueIndex];

				// get the x/y positions
				int devX;
				if (isExtendedScaling) {
					devX = (int) ((Math.pow(xValue, scalingFactor)) * scaleXExtended);
				} else {
					devX = (int) (scaleX * xValue);
				}

				final int devY = devYBottom - ((int) (scaleY * (yValue - graphYBottom)));

				// draw shape
//				gc.fillRectangle(devXPos - size2, devYPos - size2, size, size);
				gc.fillOval(devX - size2, devY - size2, size, size);
			}
		}

		// reset clipping/antialias
		gc.setClipping((Rectangle) null);
		gc.setAntialias(SWT.OFF);
	}

	/**
	 * Is used for drawing gear values.
	 * 
	 * @param gc
	 * @param graphDrawingData
	 * @param isTopGraph
	 */
	private void drawAsync_560_HorizontalBar(	final GC gc,
												final GraphDrawingData graphDrawingData,
												final boolean isTopGraph) {

		final ChartDataXSerie xData = graphDrawingData.getXData();
		final ChartDataYSerie yData = graphDrawingData.getYData();

		final float[][] yHighValues = yData.getHighValuesFloat();

		final double[] xValues = xData.getHighValuesDouble()[0];
		final float yValues[] = yHighValues[0];
		final float yValues2[] = yHighValues[1];

		final int serieSize = xValues.length;

		// create line hovered positions
		_lineFocusRectangles.add(new RectangleLong[serieSize]);
		_lineDevPositions.add(new PointLong[serieSize]);
		_isHoveredLineVisible = true;

		// check array bounds
//		final int xValueLength = xValues.length;
		final int yValueLength = yValues.length;

		// get top/bottom border values of the graph
		final float graphYBorderBottom = graphDrawingData.getGraphYBottom();
		final int devYTop = graphDrawingData.getDevYTop();
		final int devChartHeight = getDevVisibleGraphHeight();

		final double scaleX = graphDrawingData.getScaleX();
		final double scaleY = graphDrawingData.getScaleY();

		// get the horizontal offset for the graph
		float graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = (float) (Math.max(0, _xxDevViewPortLeftBorder) / scaleX);
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = (float) (_xxDevViewPortLeftBorder / scaleX);
		}

		final Display display = getDisplay();

		final int devGraphHeight = graphDrawingData.devGraphHeight;
		final float devYGraphBottom = (float) (scaleY * graphYBorderBottom);

		final RectangleLong[] lineFocusRectangles = _lineFocusRectangles.get(_lineFocusRectangles.size() - 1);
		final PointLong[] lineDevPositions = _lineDevPositions.get(_lineDevPositions.size() - 1);
		RectangleLong prevLineRect = null;

		final float devY0Inverse = devGraphHeight + devYGraphBottom;

		final double graphXStart = xValues[0] - graphValueOffset;
		final float graphYStart = yValues[0];

		float graphYPrev = graphYStart;

		double devXPrev = (scaleX * graphXStart);
		float devYPrev = (float) (scaleY * graphYPrev);

		final int devXOverlap = (int) (0.5 * scaleX) + 0;
		final int devXOverlap2 = devXOverlap * 2 + 0;

		final Rectangle chartRectangle = gc.getClipping();
		final int devXVisibleWidth = chartRectangle.width;

		boolean isDrawFirstPoint = true;

		final int valueSize = xValues.length;
		final int lastIndex = valueSize - 1;

		int valueIndexFirstPoint = 0;
		int prevValueIndex = 0;

		/*
		 * set the hovered index only ONCE because when autoscrolling is done to the right side this
		 * can cause that the last value is used for the hovered index instead of the previous
		 * before the last
		 */
		boolean isSetHoveredIndex = false;

		int barXStart = 0;
		int barXEnd = 0;
		int barY = 0;
		final int barHeight = 5;
		final int barHeight2 = barHeight / 2;

		// get the colors
		final RGB[] rgbLine = yData.getRgbLine();
		final RGB[] rgbDark = yData.getRgbDark();
		final RGB[] rgbBright = yData.getRgbBright();

		final RGB[] rgbText = yData.getRgbText();

		final Color colorText = new Color(display, rgbText[0]);
		final Color colorLine = new Color(display, rgbLine[0]);
		final Color colorBgDark = new Color(display, rgbDark[0]);
		final Color colorBgBright = new Color(display, rgbBright[0]);

		gc.setLineStyle(SWT.LINE_SOLID);

		int alphaFill = getAlphaFill(isTopGraph);
		alphaFill = 0xd0;
		gc.setAntialias(_chart.graphAntialiasing);
		gc.setAlpha(alphaFill);

		gc.setForeground(colorBgBright);
		gc.setBackground(colorBgDark);

		boolean isPrevInvalid = true;

		/*
		 * draw the lines into the paths
		 */
		for (int valueIndex = 0; valueIndex < valueSize; valueIndex++) {

			// check array bounds
			if (valueIndex >= yValueLength) {
				break;
			}

			final float graphY = yValues[valueIndex];

			if (graphY != graphY) {

				// value is NaN

				isPrevInvalid = true;
				isDrawFirstPoint = true;

				continue;
			}

			final double graphX = xValues[valueIndex] - graphValueOffset;
			final double devX = graphX * scaleX;

			final float devY = (float) (graphY * scaleY);

			// check if position is horizontal visible
			if (devX < 0 || isPrevInvalid) {

				// keep current position which is used as the starting point for painting

				isPrevInvalid = false;

				graphYPrev = graphY;

				devXPrev = devX;
				devYPrev = devY;
				prevValueIndex = valueIndex;

				valueIndexFirstPoint = valueIndex;

				continue;
			}

			/*
			 * draw first point
			 */
			if (isDrawFirstPoint) {

				// move to the first point

				isDrawFirstPoint = false;

				// set first point before devX==0 that the first line is not visible but correctly painted
				final double devXFirstPoint = devXPrev;

				final float devYInverse = devY0Inverse - devYPrev;

				barXStart = (int) devXFirstPoint;
				barY = (int) devYInverse;

				/*
				 * set line hover positions for the first point
				 */
				final long devXRect = (long) devXFirstPoint;

				final RectangleLong currentRect = new RectangleLong(devXRect, 0, 1, devChartHeight);
				final PointLong currentPoint = new PointLong(devXRect, (long) (devYTop + devYInverse));

				lineDevPositions[valueIndexFirstPoint] = currentPoint;
				lineFocusRectangles[valueIndexFirstPoint] = currentRect;

				prevLineRect = currentRect;
			}

			/*
			 * draw bar when value has changed
			 */
			{
				final float devYBar = devY0Inverse - devY;

				if (devY == devYPrev) {

					// y has not changed

				} else {

					// y has changed

					Color bgColor;
					if (valueIndex == 0) {

						// set initial value

						bgColor = yValues2[0] == 1 ? colorBgBright : colorBgDark;

					} else {

						if (yValues2[valueIndex - 1] == 1) {

							// grosses Kettenblatt

							bgColor = colorBgBright;

						} else {

							// kleines Kettenblatt

							bgColor = colorBgDark;
						}
					}
					gc.setBackground(bgColor);

					int barWidth = barXEnd - barXStart + devXOverlap2;

					// ensure bar is painted
					if (barWidth < 1) {
						barWidth = 1;
					}

					gc.fillRectangle(//
							barXStart - devXOverlap,
							barY - barHeight2,
							barWidth,
							barHeight);

					barXStart = (int) devX;
					barY = (int) devYBar;
				}

				barXEnd = (int) devX;

				/*
				 * set line hover positions
				 */
				final double devXDiff = (devX - devXPrev) / 2;
				final double devXDiffWidth = devXDiff < 1 ? 1 : (devXDiff + 0.5);
				final double devXRect = devX - devXDiffWidth;

				// add the right part of the rectangle width into the previous rectangle
				prevLineRect.width += devXDiffWidth + 1;

				// check if hovered line is hit
				if (isSetHoveredIndex == false && prevLineRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredValuePointIndex = prevValueIndex;
					isSetHoveredIndex = true;
				}

				final RectangleLong currentRect = new RectangleLong(
						(long) devXRect,
						0,
						(long) (devXDiffWidth + 1),
						devChartHeight);
				final PointLong currentPoint = new PointLong((long) devX, (long) (devYTop + devYBar));

				lineDevPositions[valueIndex] = currentPoint;
				lineFocusRectangles[valueIndex] = currentRect;

				prevLineRect = currentRect;
			}

			/*
			 * draw last bar
			 */
			if (valueIndex == lastIndex || //

					// check if last visible position + 1 is reached
					devX > devXVisibleWidth) {

				/*
				 * this is the last point for a filled graph
				 */

				int barWidth = barXEnd - barXStart + devXOverlap2;

				// ensure bar is painted
				if (barWidth < 1) {
					barWidth = 1;
				}

				Color bgColor;
				if (valueIndex == 0) {

					// set initial value

					bgColor = yValues2[0] == 1 ? colorBgBright : colorBgDark;

				} else {

					if (yValues2[valueIndex - 1] == 1) {

						// grosses Kettenblatt

						bgColor = colorBgBright;

					} else {

						// kleines Kettenblatt

						bgColor = colorBgDark;
					}
				}
				gc.setBackground(bgColor);

				gc.fillRectangle(//
						barXStart - devXOverlap,
						barY - barHeight2,
						barWidth,
						barHeight);

				/*
				 * set line rectangle
				 */
				final double devXDiff = (devX - devXPrev) / 2;
				final long devXDiffWidth = devXDiff < 1 ? 1 : (int) (devXDiff + 0.5);
				final long devXRect = (long) (devX - devXDiffWidth);

				// set right part of the rectangle width into the previous rectangle
				prevLineRect.width += devXDiffWidth;

				// check if hovered line is hit
				if (isSetHoveredIndex == false && prevLineRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredValuePointIndex = valueIndex;
					isSetHoveredIndex = true;
				}

				final float devYInverse = devY0Inverse - devY;

				final RectangleLong lastRect = new RectangleLong(devXRect, 0, devXDiffWidth + 1, devChartHeight);
				final PointLong lastPoint = new PointLong((long) devX, devYTop + (long) devYInverse);

				lineDevPositions[valueIndex] = lastPoint;
				lineFocusRectangles[valueIndex] = lastRect;

				if (isSetHoveredIndex == false && lastRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredValuePointIndex = valueIndex;
				}

				break;
			}

			devXPrev = devX;
			devYPrev = devY;
			prevValueIndex = valueIndex;
		}

		/**
		 * force a max width because the fill will not be drawn on Linux
		 */
//		final double graphWidth = xValues[Math.min(xValueLength - 1, lastIndex)] - graphValueOffset;
//		final int devGraphWidth = Math.min(0x7fff, (int) (graphWidth * scaleX));

//		gc.fillGradientRectangle(//
//				0,
//				devGraphHeight,
//				devGraphWidth,
//				-devGraphHeight,
//				true);

		// dispose resources
		colorLine.dispose();
		colorBgDark.dispose();
		colorBgBright.dispose();
		colorText.dispose();

		gc.setAlpha(0xFF);
		gc.setAntialias(SWT.OFF);
	}

	private void drawAsync_600_History(final GC gcGraph, final GraphDrawingData graphDrawingData) {

		final ChartDataXSerie xData = graphDrawingData.getXData();

		final double[] xValues = xData.getHighValuesDouble()[0];
		final int serieSize = xValues.length;

		// setup hovered positions
		_lineFocusRectangles.add(new RectangleLong[serieSize]);
		_lineDevPositions.add(new PointLong[serieSize]);
		_isHoveredLineVisible = true;

		// check array bounds
		final int xValueLength = xValues.length;
		if (0 >= xValueLength) {
			return;
		}

		// get top/bottom border values of the graph
		final int devGraphHeight = getDevVisibleGraphHeight();

		final double scaleX = graphDrawingData.getScaleX();

		final RectangleLong[] lineFocusRectangles = _lineFocusRectangles.get(_lineFocusRectangles.size() - 1);
		final PointLong[] lineDevPositions = _lineDevPositions.get(_lineDevPositions.size() - 1);
		RectangleLong prevLineRect = null;

		final double graphValueOffset = (Math.max(0, _xxDevViewPortLeftBorder) / scaleX);
		final double graphXStart = xValues[0] - graphValueOffset;
		double devXPrev = scaleX * graphXStart;

		final Rectangle chartRectangle = gcGraph.getClipping();
		final int devXVisibleWidth = chartRectangle.width;

		boolean isDrawFirstPoint = true;

		final int lastIndex = serieSize - 1;

		int valueIndexFirstPoint = 0;
		int prevValueIndex = 0;

		/*
		 * set the hovered index only ONCE because when autoscrolling is done to the right side this
		 * can cause that the last value is used for the hovered index instead of the previous
		 * before the last
		 */
		boolean isSetHoveredIndex = false;

		/*
		 * draw the lines into the paths
		 */
		for (int valueIndex = 0; valueIndex < serieSize; valueIndex++) {

			final double graphX = xValues[valueIndex] - graphValueOffset;
			final double devX = (graphX * scaleX);

			// check if position is horizontal visible
			if (devX < 0) {

				// keep current position which is used as the painting starting point

				devXPrev = devX;

				valueIndexFirstPoint = valueIndex;
				prevValueIndex = valueIndex;

				continue;
			}

			/*
			 * draw first point
			 */
			if (isDrawFirstPoint) {

				// move to the first point

				isDrawFirstPoint = false;

				// set first point before devX==0 that the first line is not visible but correctly painted
				final double devXFirstPoint = devXPrev;

				/*
				 * set line hover positions for the first point
				 */
				final long devXRect = (long) devXFirstPoint;

				final RectangleLong currentRect = new RectangleLong(devXRect, 0, 1, devGraphHeight);
				final PointLong currentPoint = new PointLong(devXRect, 0);

				lineDevPositions[valueIndexFirstPoint] = currentPoint;
				lineFocusRectangles[valueIndexFirstPoint] = currentRect;

				prevLineRect = currentRect;
			}

			/*
			 * draw line to current point
			 */
			if ((int) devX != (int) devXPrev) {

				// optimization: draw only ONE line for the current x-position
				// but draw to the 0 line otherwise it's possible that a triangle is painted

				/*
				 * set line hover positions
				 */
				final double devXDiff = (devX - devXPrev) / 2;
				final long devXDiffWidth = (long) (devXDiff < 1 ? 1 : (devXDiff + 0.5));
				final long devXRect = (long) (devX - devXDiffWidth);

				// add the right part of the rectangle width into the previous rectangle
				prevLineRect.width += devXDiffWidth + 1;

				// check if hovered line is hit, this check is an inline for .contains(...)
				if (isSetHoveredIndex == false && prevLineRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredValuePointIndex = prevValueIndex;
					isSetHoveredIndex = true;
				}

				final RectangleLong currentRect = new RectangleLong(devXRect, 0, devXDiffWidth + 1, devGraphHeight);
				final PointLong currentPoint = new PointLong((long) devX, 0);

				lineDevPositions[valueIndex] = currentPoint;
				lineFocusRectangles[valueIndex] = currentRect;

				prevLineRect = currentRect;
			}

			/*
			 * draw last point
			 */
			if (valueIndex == lastIndex || //

					// check if last visible position + 1 is reached
					devX > devXVisibleWidth) {

				/*
				 * set line rectangle
				 */
				final double devXDiff = (devX - devXPrev) / 2;
				final long devXDiffWidth = (long) (devXDiff < 1 ? 1 : (devXDiff + 0.5));
				final long devXRect = (long) (devX - devXDiffWidth);

				// set right part of the rectangle width into the previous rectangle
				prevLineRect.width += devXDiffWidth;

				// check if hovered line is hit, this check is an inline for .contains(...)
				if (isSetHoveredIndex == false && prevLineRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredValuePointIndex = valueIndex;
					isSetHoveredIndex = true;
				}

				final RectangleLong lastRect = new RectangleLong(devXRect, 0, devXDiffWidth + 1, devGraphHeight);
				final PointLong lastPoint = new PointLong((long) devX, 0);

				lineDevPositions[valueIndex] = lastPoint;
				lineFocusRectangles[valueIndex] = lastRect;

				if (isSetHoveredIndex == false && lastRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredValuePointIndex = valueIndex;
				}

				break;
			}

			devXPrev = devX;
			prevValueIndex = valueIndex;
		}
	}

	/**
	 * Paint event handler
	 * 
	 * <pre>
	 * Top-down sequence how the images are painted
	 * 
	 * {@link #_chartImage_40_Overlay}
	 * {@link #_chartImage_30_Custom}
	 * {@link #_chartImage_20_Chart}
	 * {@link #_chartImage_10_Graphs}
	 * </pre>
	 * 
	 * @param gc
	 * @param eventTime
	 */
	private void drawSync_000_onPaint(final GC gc, final long eventTime) {

//		final long startTime = System.nanoTime();
//		// TODO remove SYSTEM.OUT.PRINTLN
//
//		System.out.println(UI.timeStampNano() + " drawSync_000_onPaint: START");
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (_allGraphDrawingData == null || _allGraphDrawingData.size() == 0) {

			// fill the image area when there is no graphic
			gc.setBackground(_chart.getBackgroundColor());
			gc.fillRectangle(_clientArea);

			drawSyncBg_999_ErrorMessage(gc);

			return;
		}

		boolean isPaintedDirectly = false;

		if (_isChartDirty) {

			// draw chart

			isPaintedDirectly = drawAsync_100_StartPainting();

			if (isPaintedDirectly == false) {

				/*
				 * paint dragged chart until the chart is recomputed
				 */
				if (_isPaintDraggedImage) {
					drawSync_020_DraggedChart(gc);
					return;
				}

				/*
				 * mac osx is still flickering, added the drawChartImage in version 1.0
				 */
				if (_chartImage_20_Chart != null) {

					final Image image = drawSync_010_ImageChart(gc, eventTime);
					if (image == null) {
						return;
					}

					final int gcHeight = _clientArea.height;
					final int imageHeight = image.getBounds().height;

					if (gcHeight > imageHeight) {

						// fill the gap between the image and the drawable area
						gc.setBackground(_chart.getBackgroundColor());
						gc.fillRectangle(0, imageHeight, _clientArea.width, _clientArea.height - imageHeight);

					} else {
						gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
					}
				} else {
					gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
			}
		}

		if (_isChartDirty == false || isPaintedDirectly) {

			/*
			 * if the graph is not yet drawn (because this is done in another thread) there is
			 * nothing to do
			 */
			if (_chartImage_20_Chart == null) {
				// fill the image area when there is no graphic
				gc.setBackground(_chart.getBackgroundColor());
				gc.fillRectangle(_clientArea);
				gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
				return;
			}

			// redraw() is done in async painting but NOT after images are painted
			if (isPaintedDirectly) {

//				System.out.println("isPaintedDirectly\t");
//				// TODO remove SYSTEM.OUT.PRINTLN
			}

			drawSync_300_Image30Custom();
			drawSync_010_ImageChart(gc, eventTime);
		}

//		final long endTime = System.nanoTime();
//		System.out.println(UI.timeStampNano()
//				+ " drawSync_000_onPaint: END  "
//				+ (((double) endTime - startTime) / 1000000)
//				+ " ms   #:"
//				+ _drawAsyncCounter[0]);
//		System.out.println(UI.timeStampNano() + " \t");
//		System.out.println(UI.timeStampNano() + " \t");
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private Image drawSync_010_ImageChart(final GC gc, final long eventTime) {

		final boolean isOverlayImageVisible = _isXSliderVisible
				|| _isYSliderVisible
				|| _isXMarkerMoved
				|| _isSelectionVisible
				|| _isHoveredLineVisible;

		if (isOverlayImageVisible) {

			drawSync_400_OverlayImage(eventTime);

			if (_chartImage_40_Overlay != null) {
//				System.out.println("gc <- 40\tdrawSync010ImageChart");
//				// TODO remove SYSTEM.OUT.PRINTLN

				gc.drawImage(_chartImage_40_Overlay, 0, 0);
			}
			return _chartImage_40_Overlay;

		} else {

			if (_chartImage_20_Chart != null) {
//				System.out.println("gc <- 20");
//				// TODO remove SYSTEM.OUT.PRINTLN

				gc.drawImage(_chartImage_20_Chart, 0, 0);
			}
			return _chartImage_20_Chart;
		}
	}

	private void drawSync_020_DraggedChart(final GC gc) {

		if (_draggedChartDraggedPos == null) {
			return;
		}

		final int devXDiff = _draggedChartDraggedPos.x - _draggedChartStartPos.x;
		final int devYDiff = 0;

		/*
		 * draw background that the none painted areas do not look ugly
		 */
		gc.setBackground(_chart.getBackgroundColor());

		if (devXDiff > 0) {
			gc.fillRectangle(0, devYDiff, devXDiff, _clientArea.height);
		} else {
			gc.fillRectangle(_clientArea.width + devXDiff, devYDiff, -devXDiff, _clientArea.height);
		}

		if (_chartImage_40_Overlay != null && _chartImage_40_Overlay.isDisposed() == false) {

			gc.drawImage(_chartImage_40_Overlay, devXDiff, devYDiff);

		} else if (_chartImage_30_Custom != null && _chartImage_30_Custom.isDisposed() == false) {

			gc.drawImage(_chartImage_30_Custom, devXDiff, devYDiff);

		} else if (_chartImage_20_Chart != null && _chartImage_20_Chart.isDisposed() == false) {

			gc.drawImage(_chartImage_20_Chart, devXDiff, devYDiff);
		}
	}

	/**
	 * Draws custom foreground layers on top of the graphs.
	 */
	private void drawSync_300_Image30Custom() {

		// the layer image has the same size as the graph image
		final Rectangle chartRect = _chartImage_20_Chart.getBounds();

		// ensure correct image size
		if (chartRect.width <= 0 || chartRect.height <= 0) {
			return;
		}

		/*
		 * when the existing image is the same size as the new image, we will redraw it only if it's
		 * set to dirty
		 */
		if (_isCustomLayerImageDirty == false && _chartImage_30_Custom != null) {

			final Rectangle oldBounds = _chartImage_30_Custom.getBounds();

			if (oldBounds.width == chartRect.width && oldBounds.height == chartRect.height) {
				return;
			}
		}

		if (Util.canReuseImage(_chartImage_30_Custom, chartRect) == false) {
			_chartImage_30_Custom = Util.createImage(getDisplay(), _chartImage_30_Custom, chartRect);
		}

		final GC gcCustom = new GC(_chartImage_30_Custom);
		try {

			gcCustom.fillRectangle(chartRect);

			/*
			 * draw the chart image with the graphs into the custom layer image, the custom
			 * foreground layers are drawn on top of the graphs
			 */
			gcCustom.drawImage(_chartImage_20_Chart, 0, 0);

//			System.out.println("30 <- 20\tdrawSync300Image30Custom");
//			// TODO remove SYSTEM.OUT.PRINTLN

			for (final GraphDrawingData graphDrawingData : _allGraphDrawingData) {

				final ArrayList<IChartLayer> customFgLayers = graphDrawingData.getYData().getCustomForegroundLayers();

				for (final IChartLayer layer : customFgLayers) {
					layer.draw(gcCustom, graphDrawingData, _chart, _pc);
				}
			}

		} finally {
			gcCustom.dispose();
		}

		_isCustomLayerImageDirty = false;
	}

	/**
	 * Draws the overlays into the graph (fg layer image) slider image which contains the custom
	 * layer image
	 * 
	 * @param eventTime
	 */
	private void drawSync_400_OverlayImage(final long eventTime) {

		if (_chartImage_30_Custom == null) {
			return;
		}

//		final long start = System.nanoTime();

		// the slider image is the same size as the graph image
		final Rectangle graphImageRect = _chartImage_30_Custom.getBounds();

		// check if an overlay image redraw is necessary
		if (_isOverlayDirty == false
				&& _isSliderDirty == false
				&& _isSelectionDirty == false
				&& _isHoveredBarDirty == false
				&& _hoveredSegment == null
				&& _hoveredValuePointIndex == -1
				&& _chartImage_40_Overlay != null) {

			final Rectangle oldBounds = _chartImage_40_Overlay.getBounds();
			if (oldBounds.width == graphImageRect.width && oldBounds.height == graphImageRect.height) {

				// overlay image is not dirty

				return;
			}
		}

		// ensure correct image size
		if (graphImageRect.width <= 0 || graphImageRect.height <= 0) {
			return;
		}

		if (Util.canReuseImage(_chartImage_40_Overlay, graphImageRect) == false) {
			_chartImage_40_Overlay = Util.createImage(getDisplay(), _chartImage_40_Overlay, graphImageRect);
		}

		if (_chartImage_40_Overlay.isDisposed()) {
			return;
		}

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tdrawSync_400_OverlayImage"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		final GC gcOverlay = new GC(_chartImage_40_Overlay);
		{
			/*
			 * copy the graph image into the slider image, the slider will be drawn on top of the
			 * graph
			 */
			gcOverlay.fillRectangle(graphImageRect);
			gcOverlay.drawImage(_chartImage_30_Custom, 0, 0);

			/*
			 * draw x/y-sliders
			 */
			if (_isXSliderVisible) {
				createXSliderLabel(gcOverlay, _xSliderOnTop);
				createXSliderLabel(gcOverlay, _xSliderOnBottom);
				updateXSliderYPosition();

				drawSync_410_XSlider(gcOverlay, _xSliderOnBottom);
				drawSync_410_XSlider(gcOverlay, _xSliderOnTop);
			}
			if (_isYSliderVisible) {
				drawSync_420_YSliders(gcOverlay);
			}
			_isSliderDirty = false;

			if (_isXMarkerMoved) {
				drawSync_430_XMarker(gcOverlay);
			}

			if (_isSelectionVisible) {
				drawSync_440_Selection(gcOverlay);
			}

			if (_isHoveredBarDirty) {
				drawSync_450_HoveredBar(gcOverlay);
				_isHoveredBarDirty = false;
			}

			if (_hoveredValuePointIndex != -1 && _lineDevPositions.size() > 0) {

				// hovered lines are set -> draw it
				drawSync_460_HoveredLine(gcOverlay);
			}

			if (_hoveredSegment != null) {
				drawSync_462_HoveredSegment(gcOverlay);
			}

			if (_isOverlayDirty) {
				drawSync_470_CustomOverlay(gcOverlay, eventTime);
				_isOverlayDirty = false;
			}

		}
		gcOverlay.dispose();

//		System.out.println("time\t" + ((float) (System.nanoTime() - start) / 1000000) + " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	/**
	 * @param gcGraph
	 * @param slider
	 */
	private void drawSync_410_XSlider(final GC gcGraph, final ChartXSlider slider) {

		final Display display = getDisplay();

		final boolean isGraphOverlapped = _chartDrawingData.chartDataModel.isGraphOverlapped();

		final int devSliderLinePos = (int) (slider.getXXDevSliderLinePos() - _xxDevViewPortLeftBorder);

		final int grayColorIndex = 60;
		final Color colorTxt = new Color(display, grayColorIndex, grayColorIndex, grayColorIndex);

		int graphNo = 0;

		final ArrayList<ChartXSliderLabel> labelList = slider.getLabelList();

		// draw slider for each graph
		for (final GraphDrawingData drawingData : _allGraphDrawingData) {

			graphNo++;

			/*
			 * Draw only for the last overlapped graph
			 */
			final ChartType chartType = drawingData.getChartType();
			final boolean isLastOverlappedGraph = isGraphOverlapped
					&& graphNo == _allGraphDrawingData.size()
					&& (chartType == ChartType.LINE || chartType == ChartType.HORIZONTAL_BAR);

			if (isGraphOverlapped && isLastOverlappedGraph == false) {
				continue;
			}

			final ChartDataYSerie yData = drawingData.getYData();
			final ChartXSliderLabel label = labelList.get(graphNo - 1);

			final Color colorLine = new Color(display, yData.getRgbLine()[0]);
			final Color colorBright = new Color(display, yData.getRgbBright()[0]);
			final Color colorDark = new Color(display, yData.getRgbDark()[0]);

			final int labelHeight = label.height;
			final int labelWidth = label.width;
			final int devXLabel = label.x;
			final int devYLabel = label.y;

			final int devYBottom = drawingData.getDevYBottom();
			final boolean isSliderHovered = _mouseOverXSlider != null && _mouseOverXSlider == slider;

			/*
			 * when the mouse is over the slider, the slider is painted in a darker color
			 */

			// draw slider line
			if ((_isFocusActive && _selectedXSlider == slider) || isSliderHovered) {
				gcGraph.setAlpha(0xd0);
			} else {
				gcGraph.setAlpha(0x60);
			}
			gcGraph.setForeground(colorLine);
			gcGraph.setLineDash(new int[] { 4, 1, 4, 1 });
			gcGraph.drawLine(devSliderLinePos, devYLabel + labelHeight, devSliderLinePos, devYBottom);

			gcGraph.setBackground(colorDark);
			gcGraph.setForeground(colorBright);

			// draw label border
			gcGraph.setForeground(colorLine);
			gcGraph.setLineStyle(SWT.LINE_SOLID);
			gcGraph.drawRoundRectangle(devXLabel, devYLabel - 4, labelWidth, labelHeight + 3, 4, 4);

			// draw slider label
			gcGraph.setAlpha(0xff);
			gcGraph.setForeground(colorTxt);
			gcGraph.drawText(label.text, devXLabel + 2, devYLabel - 5, true);

			// draw a tiny marker on the graph
			gcGraph.setBackground(colorLine);
			gcGraph.fillRectangle(devSliderLinePos - 3, label.devYGraph - 2, 7, 3);

			/*
			 * draw a marker below the x-axis to make the selection more visible
			 */
			if (_isFocusActive && slider == _selectedXSlider) {

				final int markerWidth = BAR_MARKER_WIDTH;
				final int markerWidth2 = markerWidth / 2;

				final int devMarkerXPos = devSliderLinePos - markerWidth2;

				final int[] marker = new int[] {
						devMarkerXPos,
						devYBottom + 1 + markerWidth2,
						devMarkerXPos + markerWidth2,
						devYBottom + 1,
						devMarkerXPos + markerWidth,
						devYBottom + 1 + markerWidth2 };

				gcGraph.setAlpha(0xc0);
				gcGraph.setLineStyle(SWT.LINE_SOLID);

				// draw background
				gcGraph.setBackground(colorDark);
				gcGraph.fillPolygon(marker);

				// draw border
				gcGraph.setForeground(colorLine);
				gcGraph.drawPolygon(marker);

				gcGraph.setAlpha(0xff);
			}

			colorLine.dispose();
			colorBright.dispose();
			colorDark.dispose();
		}

		colorTxt.dispose();
	}

	/**
	 * Draw the y-slider which it hit.
	 * 
	 * @param gcGraph
	 */
	private void drawSync_420_YSliders(final GC gcGraph) {

		if (_hitYSlider == null) {
			return;
		}

		final Display display = getDisplay();

//		final int grayColorIndex = 60;
//		final Color colorTxt = new Color(display, grayColorIndex, grayColorIndex, grayColorIndex);

		final int devXChartWidth = getDevVisibleChartWidth();

		for (final ChartYSlider ySlider : _ySliders) {

			if (_hitYSlider == ySlider) {

				final ChartDataYSerie yData = ySlider.getYData();

				final Color colorLine = new Color(display, yData.getRgbLine()[0]);
				final Color colorBright = new Color(display, yData.getRgbBright()[0]);
				final Color colorDark = new Color(display, yData.getRgbDark()[0]);
				final Color colorText = new Color(display, yData.getRgbText()[0]);

				final GraphDrawingData drawingData = ySlider.getDrawingData();
				final int devYBottom = drawingData.getDevYBottom();
				final int devYTop = devYBottom - drawingData.devGraphHeight;

				final int devYSliderLine = ySlider.getDevYSliderLine();

				// set the label and line NOT outside of the chart
				int devYLabelPos = devYSliderLine;

				if (devYSliderLine > devYBottom) {
					devYLabelPos = devYBottom;
				} else if (devYSliderLine < devYTop) {
					devYLabelPos = devYTop;
				}

				// ySlider is the slider which was hit by the mouse, draw the
				// slider label

				final StringBuilder labelText = new StringBuilder();

				final float devYValue = (float) (((double) devYBottom - devYSliderLine) / drawingData.getScaleY() + drawingData
						.getGraphYBottom());

				final String unitLabel = yData.getUnitLabel();

				// create the slider text
				labelText.append(Util.formatValue(devYValue, yData.getAxisUnit(), yData.getValueDivisor(), true));
				if (unitLabel.length() > 0) {
					labelText.append(UI.SPACE);
					labelText.append(unitLabel);
				}
				labelText.append(UI.SPACE);
				final String label = labelText.toString();

				final Point labelExtend = gcGraph.stringExtent(label);

				final int labelHeight = labelExtend.y - 2;
				final int labelWidth = labelExtend.x + 1;
				final int labelX = _ySliderGraphX - labelWidth - 5;
				final int labelY = devYLabelPos - labelHeight;

				// draw label background
				gcGraph.setForeground(colorBright);
				gcGraph.setBackground(colorDark);
//				gcGraph.setAlpha(0xb0);
				gcGraph.fillGradientRectangle(labelX, labelY, labelWidth, labelHeight, true);

				// draw label border
//				gcGraph.setAlpha(0xa0);
				gcGraph.setForeground(colorLine);
				gcGraph.drawRectangle(labelX, labelY, labelWidth, labelHeight);
//				gcGraph.setAlpha(0xff);

				// draw label text
				gcGraph.setForeground(colorText);
				gcGraph.drawText(label, labelX + 3, labelY - 0, true);

				// draw slider line
				gcGraph.setForeground(colorLine);
				gcGraph.setLineDash(DOT_DASHES);
				gcGraph.drawLine(0, devYLabelPos, devXChartWidth, devYLabelPos);

				colorLine.dispose();
				colorBright.dispose();
				colorDark.dispose();
				colorText.dispose();

				// only 1 y-slider can be hit
				break;
			}
		}

//		colorTxt.dispose();
	}

	private void drawSync_430_XMarker(final GC gc) {

		final Display display = getDisplay();
		final Color colorXMarker = new Color(display, 255, 153, 0);

		final int devDraggingDiff = _devXMarkerDraggedPos - _devXMarkerDraggedStartPos;

		// draw x-marker for each graph
		for (final GraphDrawingData drawingData : _allGraphDrawingData) {

			final ChartDataXSerie xData = drawingData.getXData();

			final double scaleX = drawingData.getScaleX();
			final double valueDraggingDiff = devDraggingDiff / scaleX;

			final int synchStartIndex = xData.getSynchMarkerStartIndex();
			final int synchEndIndex = xData.getSynchMarkerEndIndex();

			final double[] xValues = xData.getHighValuesDouble()[0];
			final double valueXStart = xValues[synchStartIndex];
			final double valueXEnd = xValues[synchEndIndex];

			final int devXStart = (int) (scaleX * valueXStart - _xxDevViewPortLeftBorder);
			final int devXEnd = (int) (scaleX * valueXEnd - _xxDevViewPortLeftBorder);
			int devMovedXStart = devXStart;
			int devMovedXEnd = devXEnd;

			final double valueXStartWithOffset = valueXStart + valueDraggingDiff;
			final double valueXEndWithOffset = valueXEnd + valueDraggingDiff;

			_movedXMarkerStartValueIndex = computeXMarkerValue(
					xValues,
					synchStartIndex,
					valueDraggingDiff,
					valueXStartWithOffset);

			_movedXMarkerEndValueIndex = computeXMarkerValue(
					xValues,
					synchEndIndex,
					valueDraggingDiff,
					valueXEndWithOffset);

			devMovedXStart = (int) (scaleX * xValues[_movedXMarkerStartValueIndex] - _xxDevViewPortLeftBorder);
			devMovedXEnd = (int) (scaleX * xValues[_movedXMarkerEndValueIndex] - _xxDevViewPortLeftBorder);

			/*
			 * when the moved x-marker is on the right or the left border, make sure that the
			 * x-markers don't get too small
			 */
			final double valueMovedDiff = xValues[_movedXMarkerEndValueIndex] - xValues[_movedXMarkerStartValueIndex];

			/*
			 * adjust start and end position
			 */
			if (_movedXMarkerStartValueIndex == 0 && valueMovedDiff < _xMarkerValueDiff) {

				/*
				 * the x-marker is moved to the left, the most left x-marker is on the first
				 * position
				 */

				int valueIndex;

				for (valueIndex = 0; valueIndex < xValues.length; valueIndex++) {
					if (xValues[valueIndex] >= _xMarkerValueDiff) {
						break;
					}
				}

				_movedXMarkerEndValueIndex = valueIndex;

			} else if (_movedXMarkerEndValueIndex == xValues.length - 1 && valueMovedDiff < _xMarkerValueDiff) {

				/*
				 * the x-marker is moved to the right, the most right x-marker is on the last
				 * position
				 */

				int valueIndex;
				final double valueFirstIndex = xValues[xValues.length - 1] - _xMarkerValueDiff;

				for (valueIndex = xValues.length - 1; valueIndex > 0; valueIndex--) {
					if (xValues[valueIndex] <= valueFirstIndex) {
						break;
					}
				}

				_movedXMarkerStartValueIndex = valueIndex;
			}

			if (valueMovedDiff > _xMarkerValueDiff) {

				/*
				 * force the value diff for the x-marker, the moved value diff can't be wider then
				 * one value index
				 */

				final double valueStart = xValues[_movedXMarkerStartValueIndex];
				int valueIndex;
				for (valueIndex = _movedXMarkerEndValueIndex - 0; valueIndex >= 0; valueIndex--) {
					if (xValues[valueIndex] - valueStart < _xMarkerValueDiff) {
						valueIndex++;
						break;
					}
				}
				valueIndex = Math.min(valueIndex, xValues.length - 1);

				_movedXMarkerEndValueIndex = valueIndex;
			}

			_movedXMarkerEndValueIndex = Math.min(_movedXMarkerEndValueIndex, xValues.length - 1);

			devMovedXStart = (int) (scaleX * xValues[_movedXMarkerStartValueIndex] - _xxDevViewPortLeftBorder);
			devMovedXEnd = (int) (scaleX * xValues[_movedXMarkerEndValueIndex] - _xxDevViewPortLeftBorder);

			final int devYTop = drawingData.getDevYBottom() - drawingData.devGraphHeight;
			final int devYBottom = drawingData.getDevYBottom();

			// draw moved x-marker
			gc.setForeground(colorXMarker);
			gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));

			gc.setAlpha(0x80);

			gc.fillGradientRectangle(//
					devMovedXStart,
					devYBottom,
					devMovedXEnd - devMovedXStart,
					devYTop - devYBottom,
					true);

			gc.drawLine(devMovedXStart, devYTop, devMovedXStart, devYBottom);
			gc.drawLine(devMovedXEnd, devYTop, devMovedXEnd, devYBottom);

			gc.setAlpha(0xff);
		}

		colorXMarker.dispose();
	}

	private void drawSync_440_Selection(final GC gc) {

		_isSelectionDirty = false;

		final ChartType chartType = _chart.getChartDataModel().getChartType();

		// loop: all graphs
		for (final GraphDrawingData drawingData : _allGraphDrawingData) {

			if (chartType == ChartType.LINE) {

//				drawLineSelection(gc, drawingData);

			} else if (chartType == ChartType.BAR) {

				drawSync_442_BarSelection(gc, drawingData);
			}
		}
	}

	private void drawSync_442_BarSelection(final GC gc, final GraphDrawingData drawingData) {

		// check if multiple bars are selected
		boolean drawSelection = false;
		int selectedIndex = 0;
		if (_selectedBarItems != null) {
			int selectionIndex = 0;
			for (final boolean isBarSelected : _selectedBarItems) {
				if (isBarSelected) {
					if (drawSelection == false) {
						drawSelection = true;
						selectedIndex = selectionIndex;
					} else {
						drawSelection = false;
						return;
					}
				}
				selectionIndex++;
			}
		}

		if (drawSelection == false) {
			return;
		}

		/*
		 * a bar is selected
		 */

		// get the chart data
		final ChartDataYSerie yData = drawingData.getYData();
		final int[][] colorsIndex = yData.getColorsIndex();

		// get the colors
		final RGB[] rgbLine = yData.getRgbLine();
		final RGB[] rgbDark = yData.getRgbDark();
		final RGB[] rgbBright = yData.getRgbBright();

		final int devYBottom = drawingData.getDevYBottom();
//		final int devYBottom = drawingData.devGraphHeight;

		final Rectangle[][] barRectangeleSeries = drawingData.getBarRectangles();

		if (barRectangeleSeries == null) {
			return;
		}

		final int markerWidth = BAR_MARKER_WIDTH;
		final int barThickness = 1;
		final int markerWidth2 = markerWidth / 2;

		gc.setLineStyle(SWT.LINE_SOLID);

		// loop: all data series
		for (int serieIndex = 0; serieIndex < barRectangeleSeries.length; serieIndex++) {

			// get selected rectangle
			final Rectangle[] barRectangles = barRectangeleSeries[serieIndex];
			if (barRectangles == null || selectedIndex >= barRectangles.length) {
				continue;
			}

			final Rectangle barRectangle = barRectangles[selectedIndex];
			if (barRectangle == null) {
				continue;
			}

			/*
			 * current bar is selected, draw the selected bar
			 */

			final Rectangle barShapeSelected = new Rectangle(
					(barRectangle.x - markerWidth2),
					(barRectangle.y - markerWidth2),
					(barRectangle.width + markerWidth),
					(barRectangle.height + markerWidth));

			final Rectangle barBarSelected = new Rectangle(
					barRectangle.x - 1,
					barRectangle.y - barThickness,
					barRectangle.width + barThickness,
					barRectangle.height + 2 * barThickness);

			final int colorIndex = colorsIndex[serieIndex][selectedIndex];
			final RGB rgbBrightDef = rgbBright[colorIndex];
			final RGB rgbDarkDef = rgbDark[colorIndex];
			final RGB rgbLineDef = rgbLine[colorIndex];

			final Color colorBrightSelected = getColor(rgbBrightDef);
			final Color colorDarkSelected = getColor(rgbDarkDef);
			final Color colorLineSelected = getColor(rgbLineDef);

			// do't write into the x-axis units which also contains the
			// selection marker
			if (barShapeSelected.y + barShapeSelected.height > devYBottom) {
				barShapeSelected.height = devYBottom - barShapeSelected.y;
			}

			// draw the selection darker when the focus is set
			if (_isFocusActive) {
				gc.setAlpha(0xf0);
			} else {
				gc.setAlpha(0xa0);
			}

			// fill bar background
			gc.setForeground(colorDarkSelected);
			gc.setBackground(colorBrightSelected);

			if (barShapeSelected.height < 0) {

				// bar is below the x-axis, just draw a simple line

				gc.setForeground(colorLineSelected);
				gc.drawLine(//
						barShapeSelected.x,
						devYBottom + 1,
						barShapeSelected.x + barShapeSelected.width,
						devYBottom + 1);
			} else {

				gc.fillGradientRectangle(
						barShapeSelected.x + 1,
						barShapeSelected.y + 1,
						barShapeSelected.width - 1,
						barShapeSelected.height - 1,
						true);

				// draw bar border
				gc.setForeground(colorLineSelected);
				gc.drawRoundRectangle(
						barShapeSelected.x,
						barShapeSelected.y,
						barShapeSelected.width,
						barShapeSelected.height,
						4,
						4);

				// draw bar thicker
				gc.setBackground(colorDarkSelected);
				gc.fillRoundRectangle(//
						barBarSelected.x,
						barBarSelected.y,
						barBarSelected.width,
						barBarSelected.height,
						2,
						2);
			}

			/*
			 * draw a marker below the x-axis to make the selection more visible
			 */
			if (_isFocusActive) {

				final int devMarkerXPos = barRectangle.x + (barRectangle.width / 2) - markerWidth2;

				final int[] marker = new int[] {
						devMarkerXPos,
						devYBottom + 1 + markerWidth2,
						devMarkerXPos + markerWidth2,
						devYBottom + 1,
						devMarkerXPos + markerWidth - 0,
						devYBottom + 1 + markerWidth2 };

				// draw background
				gc.setBackground(colorDarkSelected);
				gc.fillPolygon(marker);

				// draw border
				gc.setForeground(colorLineSelected);
				gc.drawPolygon(marker);

				gc.setAlpha(0xff);
			}
		}
	}

	private void drawSync_450_HoveredBar(final GC gcOverlay) {

		// check if hovered bar is disabled
		if (_hoveredBarSerieIndex == -1) {
			return;
		}

		// draw only bar chars
		if (_chart.getChartDataModel().getChartType() != ChartType.BAR) {
			return;
		}

		gcOverlay.setLineStyle(SWT.LINE_SOLID);
		gcOverlay.setAlpha(0xd0);

		// loop: all graphs
		for (final GraphDrawingData drawingData : _allGraphDrawingData) {

			// get the chart data
			final ChartDataYSerie yData = drawingData.getYData();
			final int serieLayout = yData.getChartLayout();
			final int[][] colorsIndex = yData.getColorsIndex();

			// get the colors
			final RGB[] rgbLine = yData.getRgbLine();
			final RGB[] rgbDark = yData.getRgbDark();
			final RGB[] rgbBright = yData.getRgbBright();

			final int devYBottom = drawingData.getDevYBottom();
//			final int devYBottom = drawingData.devGraphHeight;

			final Rectangle[][] barRectangeleSeries = drawingData.getBarRectangles();

			final int markerWidth = BAR_MARKER_WIDTH;
			final int markerWidth2 = markerWidth / 2;

			// loop: all data series
			for (int serieIndex = 0; serieIndex < barRectangeleSeries.length; serieIndex++) {

				// get hovered rectangle
				final Rectangle hoveredRectangle = barRectangeleSeries[serieIndex][_hoveredBarValueIndex];

				if (hoveredRectangle == null) {
					continue;
				}

				if (serieIndex != _hoveredBarSerieIndex) {
					continue;
				}

				final int colorIndex = colorsIndex[serieIndex][_hoveredBarValueIndex];
				final RGB rgbBrightDef = rgbBright[colorIndex];
				final RGB rgbDarkDef = rgbDark[colorIndex];
				final RGB rgbLineDef = rgbLine[colorIndex];

				final Color colorBright = getColor(rgbBrightDef);
				final Color colorDark = getColor(rgbDarkDef);
				final Color colorLine = getColor(rgbLineDef);

				if (serieLayout != ChartDataYSerie.BAR_LAYOUT_STACKED) {

				}

				final Rectangle hoveredBarShape = new Rectangle(
						(hoveredRectangle.x - markerWidth2),
						(hoveredRectangle.y - markerWidth2),
						(hoveredRectangle.width + markerWidth),
						(hoveredRectangle.height + markerWidth));

				// do't write into the x-axis units which also contains the
				// selection marker
				if (hoveredBarShape.y + hoveredBarShape.height > devYBottom) {
					hoveredBarShape.height = devYBottom - hoveredBarShape.y;
				}

				// fill bar background
				gcOverlay.setForeground(colorDark);
				gcOverlay.setBackground(colorBright);

				gcOverlay.fillGradientRectangle(
						hoveredBarShape.x + 1,
						hoveredBarShape.y + 1,
						hoveredBarShape.width - 1,
						hoveredBarShape.height - 1,
						true);

				// draw bar border
				gcOverlay.setForeground(colorLine);
				gcOverlay.drawRoundRectangle(
						hoveredBarShape.x,
						hoveredBarShape.y,
						hoveredBarShape.width,
						hoveredBarShape.height,
						4,
						4);
			}
		}

		gcOverlay.setAlpha(0xff);
	}

	private void drawSync_460_HoveredLine(final GC gcOverlay) {

		int graphIndex = 0;

		// draw value point marker
		final int devOffsetFill = _pc.convertHorizontalDLUsToPixels(8); // 10;
		final int devOffsetPoint = _pc.convertHorizontalDLUsToPixels(2);// 3;

		gcOverlay.setAntialias(SWT.ON);

		// loop: all graphs
		for (final GraphDrawingData drawingData : _allGraphDrawingData) {

			// draw only line graphs
			if (_chart.getChartDataModel().getChartType() != ChartType.LINE) {
				continue;
			}

			// get the chart data
			final ChartDataYSerie yData = drawingData.getYData();
			final int[][] colorsIndex = yData.getColorsIndex();
			final int[] lineColorIndex = colorsIndex[0];

			/*
			 * get hovered rectangle
			 */
			// check bounds
			if (_lineDevPositions.size() - 1 < graphIndex) {
				return;
			}

			// check bounds
			final PointLong[] lineDevPositions = _lineDevPositions.get(graphIndex);
			if (lineDevPositions.length - 1 < graphIndex) {
				return;
			}

			// check color index
			if (_hoveredValuePointIndex > lineColorIndex.length - 1) {
				return;
			}

			final RectangleLong[] lineFocusRectangles = _lineFocusRectangles.get(graphIndex);

			final PointLong devPosition = lineDevPositions[_hoveredValuePointIndex];
			final RectangleLong hoveredRectangle = lineFocusRectangles[_hoveredValuePointIndex];

			// check if hovered line positions are set
			if (hoveredRectangle == null || devPosition == null) {
				continue;
			}

			int devX;
			final int devVisibleChartWidth = getDevVisibleChartWidth();
			Color colorLine;

			/*
			 * paint the points which are outside of the visible area at the border with gray color
			 */
			if (devPosition.x < 0) {

				devX = 0;
				colorLine = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);

			} else if (devPosition.x > devVisibleChartWidth) {

				devX = devVisibleChartWidth;
				colorLine = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);

			} else {

				devX = (int) devPosition.x;

				// get the colors
				final RGB[] rgbLine = yData.getRgbLine();
				final int colorIndex = lineColorIndex[_hoveredValuePointIndex];

				final RGB rgbLineDef = rgbLine[colorIndex];
				colorLine = getColor(rgbLineDef);
			}

			gcOverlay.setBackground(colorLine);

			gcOverlay.setAlpha(0x40);
			gcOverlay.fillOval(//
					devX - devOffsetFill + 1,
					(int) (devPosition.y - devOffsetFill + 1),
					devOffsetFill * 2,
					devOffsetFill * 2);

			gcOverlay.setAlpha(0xff);
			gcOverlay.fillOval(//
					devX - devOffsetPoint + 1,
					(int) (devPosition.y - devOffsetPoint + 1),
					devOffsetPoint * 2,
					devOffsetPoint * 2);

//			// debug: draw hovered rectangle
//			gcOverlay.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//			gcOverlay.drawRectangle(
//					(int) hoveredRectangle.x,
//					(int) hoveredRectangle.y,
//					(int) hoveredRectangle.width,
//					(int) hoveredRectangle.height);

			// move to next graph
			graphIndex++;
		}

		gcOverlay.setAntialias(SWT.OFF);
	}

	private void drawSync_462_HoveredSegment(final GC gcOverlay) {

		gcOverlay.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		gcOverlay.setAlpha(0x10);

		gcOverlay.fillRectangle(//
				_hoveredSegment.devXSegment,
				0,
				_hoveredSegment.devSegmentWidth,
				_hoveredSegment.devYTitle + _hoveredSegment.titleHeight);

		if (chartSegmentConfig.isMultipleSegments) {

			// show hovered segment only when multiple tours are displayed otherwise it is a bit confusing

			for (final GraphDrawingData graphDrawingData : _allGraphDrawingData) {

				final int devYTop = graphDrawingData.getDevYBottom() - graphDrawingData.devGraphHeight;
				final int devYBottom = graphDrawingData.getDevYBottom();

				gcOverlay.fillRectangle(//
						_hoveredSegment.devXSegment,
						devYBottom,
						_hoveredSegment.devSegmentWidth,
						devYTop - devYBottom);
			}
		}

		// reset alpha
		gcOverlay.setAlpha(0xff);
	}

	private void drawSync_470_CustomOverlay(final GC gcOverlay, final long eventTime) {

		/*
		 * custom overlay must be checked 2x because it is fired 2 times before the photo groups are
		 * set correctly
		 */

		if (_isCustomOverlayInvalid == 99) {

			_isCustomOverlayInvalid = 1;

			return;
		}

		if (_isCustomOverlayInvalid == 1) {

			_isCustomOverlayInvalid = 0;

			// check if a custom overlay needs to be painted
//			_isOverlayDirty = _chart.getCustomOverlayState(eventTime, _devXMouseMove, _devYMouseMove);
		}

		if (_isOverlayDirty) {

			for (final GraphDrawingData graphDrawingData : _allGraphDrawingData) {

				final ArrayList<IChartLayer> customFgLayers = graphDrawingData.getYData().getCustomForegroundLayers();

				/**
				 * Draw overlay only when a graph contains overlay layers to ensure that a overlay
				 * is painted not for the "wrong" graphs.
				 * <p>
				 * This feature is used to optimize painting is currently used for tour markers and
				 * photos.
				 */
				if (customFgLayers.size() == 0) {
					continue;
				}

				for (final Object item : _chart.getChartOverlays()) {
					if (item instanceof IChartOverlay) {
						((IChartOverlay) item).drawOverlay(gcOverlay, graphDrawingData);
					}
				}
			}
		}
	}

	private void drawSyncBg_999_ErrorMessage(final GC gc) {

		final String errorMessage = _chartComponents.errorMessage;
		if (errorMessage != null) {
			gc.drawText(errorMessage, 0, 10);
		}
	}

	private int getAlphaFill(final boolean isTopGraph) {

		int graphFillingAlpha = (int) (_chart.graphTransparencyFilling * _chart.graphTransparencyAdjustment);

		if (_canChartBeOverlapped && _isChartOverlapped) {

			// reduce opacity for overlapped graphs

			if (isTopGraph) {

				graphFillingAlpha *= 0.6;

			} else {

				graphFillingAlpha *= 0.2;
			}
		}

		// check ranges
		graphFillingAlpha = graphFillingAlpha < 0 ? 0 : graphFillingAlpha > 255 ? 255 : graphFillingAlpha;

		return graphFillingAlpha;
	}

	private int getAlphaLine() {

		int graphLineAlpha = (int) (_chart.graphTransparencyLine * _chart.graphTransparencyAdjustment);

		// check ranges
		graphLineAlpha = graphLineAlpha < 0 ? 0 : graphLineAlpha > 255 ? 255 : graphLineAlpha;

		return graphLineAlpha;
	}

	/**
	 * @param rgb
	 * @return Returns the color from the color cache, the color must not be disposed this is done
	 *         when the cache is disposed
	 */
	private Color getColor(final RGB rgb) {

// !!! this is a performance bottleneck !!!
//		final String colorKey = rgb.toString();

		final String colorKey = Integer.toString(rgb.hashCode());
		final Color color = _colorCache.get(colorKey);

		if (color == null) {
			return _colorCache.getColor(colorKey, rgb);
		} else {
			return color;
		}
	}

	/**
	 * @return Returns the viewport (visible width) of the chart graph
	 */
	int getDevVisibleChartWidth() {
		return _chartComponents.getDevVisibleChartWidth();
	}

	/**
	 * @return Returns the visible height of the chart graph
	 */
	private int getDevVisibleGraphHeight() {
		return _chartComponents.getDevVisibleChartHeight();
	}

	private PointLong getHoveredValueDevPosition() {

		final PointLong[] lineDevPositions = _lineDevPositions.get(0);
		PointLong lineDevPos = lineDevPositions[_hoveredValuePointIndex];

		boolean isAdjusted = false;

		/*
		 * it happened, that lineDevPos was null
		 */
		if (lineDevPos == null) {

			int lineDevIndex = _hoveredValuePointIndex;

			// check forward
			while (lineDevIndex < lineDevPositions.length - 1) {

				lineDevPos = lineDevPositions[++lineDevIndex];

				if (lineDevPos != null) {
					_hoveredValuePointIndex = lineDevIndex;
					isAdjusted = true;
					break;
				}
			}

			if (lineDevPos == null) {

				lineDevIndex = _hoveredValuePointIndex;

				// check backward
				while (lineDevIndex > 0) {

					lineDevPos = lineDevPositions[--lineDevIndex];

					if (lineDevPos != null) {
						_hoveredValuePointIndex = lineDevIndex;
						isAdjusted = true;
						break;
					}
				}
			}
		}

		if (isAdjusted) {
			// force repaining
			_isOverlayDirty = true;
		}

		return lineDevPos;
	}

	/**
	 * @return Returns the index in the data series which is hovered with the mouse or
	 *         <code>-1</code> when a value is not hovered.
	 */
	int getHoveredValuePointIndex() {
		return _hoveredValuePointIndex;
	}

	/**
	 * @return Returns the left slider
	 */
	ChartXSlider getLeftSlider() {

		final long posSliderA = _xSliderA.getXXDevSliderLinePos();
		final long posSliderB = _xSliderB.getXXDevSliderLinePos();

		return posSliderA < posSliderB ? _xSliderA : _xSliderB;
	}

	/**
	 * @return Returns the right most slider
	 */
	ChartXSlider getRightSlider() {

		final long posSliderA = _xSliderA.getXXDevSliderLinePos();
		final long posSliderB = _xSliderB.getXXDevSliderLinePos();

		return posSliderA < posSliderB ? _xSliderB : _xSliderA;
	}

	ChartXSlider getSelectedSlider() {

		final ChartXSlider slider = _selectedXSlider;

		if (slider == null) {
			return getLeftSlider();
		}
		return slider;
	}

	/**
	 * @return Returns the x-Data in the drawing data list
	 */
	private ChartDataXSerie getXData() {
		if (_allGraphDrawingData.size() == 0) {
			return null;
		} else {
			return _allGraphDrawingData.get(0).getXData();
		}
	}

	private GraphDrawingData getXDrawingData() {
		return _allGraphDrawingData.get(0);
	}

	/**
	 * @return Returns the virtual graph image width, this is the width of the graph image when the
	 *         full graph would be displayed
	 */
	long getXXDevGraphWidth() {
		return _xxDevGraphWidth;
	}

	/**
	 * @return When the graph is zoomed, the chart shows only a part of the whole graph in the
	 *         viewport. Returns the left border of the viewport.
	 */
	long getXXDevViewPortLeftBorder() {
		return _xxDevViewPortLeftBorder;
	}

	double getZoomRatio() {
		return _graphZoomRatio;
	}

	double getZoomRatioLeftBorder() {
		return _zoomRatioLeftBorder;
	}

	private void handleChartResizeForSliders() {

		// update the width in the sliders
		final int visibleGraphHeight = getDevVisibleGraphHeight();

		getLeftSlider().handleChartResize(visibleGraphHeight);
		getRightSlider().handleChartResize(visibleGraphHeight);
	}

	/**
	 * Mouse event occured in the value point tooltip, move the slider and/or hovered line (value
	 * point) accordingly.
	 * 
	 * @param event
	 * @param mouseDisplayPosition
	 */
	void handleTooltipMouseEvent(final Event event, final Point mouseDisplayPosition) {

		switch (event.type) {
		case SWT.MouseMove:

			final Point controlPos = toControl(mouseDisplayPosition);
			final Rectangle clientRect = getClientArea();

			if (clientRect.contains(controlPos)) {

				_isAutoScroll = false;

				onMouseMove(event.time & 0xFFFFFFFFL, controlPos.x, controlPos.y);

			} else {
				onMouseMoveAxis(new MouseEvent(event));
			}

			break;

		case SWT.MouseEnter:

			// simulate a mouse move to do autoscrolling
			onMouseMoveAxis(new MouseEvent(event));

			break;

		case SWT.MouseExit:

			break;

		case SWT.MouseVerticalWheel:
			onMouseWheel(event, false, false);
			break;

		default:
			break;
		}
	}

	private void hideTooltip() {

		final IHoveredValueListener hoveredListener = _chart.getHoveredListener();
		if (hoveredListener != null) {

			// hide value point tooltip
			hoveredListener.hideTooltip();
		}
	}

	/**
	 * check if mouse has moved over a bar
	 * 
	 * @param devX
	 * @param devY
	 */
	private boolean isBarHit(final int devX, final int devY) {

		boolean isBarHit = false;

		// loop: all graphs
		for (final GraphDrawingData drawingData : _allGraphDrawingData) {

			final Rectangle[][] barFocusRectangles = drawingData.getBarFocusRectangles();
			if (barFocusRectangles == null) {
				break;
			}

			final int serieLength = barFocusRectangles.length;

			// find the rectangle which is hovered by the mouse
			for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

				final Rectangle[] serieRectangles = barFocusRectangles[serieIndex];

				for (int valueIndex = 0; valueIndex < serieRectangles.length; valueIndex++) {

					final Rectangle barInfoFocus = serieRectangles[valueIndex];

					// test if the mouse is within a bar focus rectangle
					if (barInfoFocus != null && barInfoFocus.contains(devX, devY)) {

						// keep the hovered bar index
						_hoveredBarSerieIndex = serieIndex;
						_hoveredBarValueIndex = valueIndex;

						_hoveredBarToolTip.toolTip10Show(devX, 100, serieIndex, valueIndex);

						isBarHit = true;
						break;
					}
				}
				if (isBarHit) {
					break;
				}
			}

			if (isBarHit) {
				break;
			}
		}

		if (isBarHit == false) {

			_hoveredBarToolTip.toolTip20Hide();

			if (_hoveredBarSerieIndex != -1) {

				/*
				 * hide last hovered bar, because the last hovered bar is visible
				 */

				// set status: no bar is hovered
				_hoveredBarSerieIndex = -1;

				// force redraw
				isBarHit = true;
			}
		}

		return isBarHit;
	}

	private boolean isInXSliderSetArea(final int devYMouse) {

		final int devVisibleChartHeight = _chartComponents.getDevVisibleChartHeight();
		final int devSetArea = (int) Math.min(100, devVisibleChartHeight * 0.3);

		Cursor cursor = null;

		if (devYMouse < devSetArea) {

			cursor = _cursorXSliderLeft;

			_isSetXSliderPositionLeft = true;
			_isSetXSliderPositionRight = false;

		} else if (devYMouse > (devVisibleChartHeight - devSetArea)) {

			cursor = _cursorXSliderRight;

			_isSetXSliderPositionLeft = false;
			_isSetXSliderPositionRight = true;
		}

		if (cursor != null) {

			setCursor(cursor);

			return true;

		} else {

			_isSetXSliderPositionLeft = false;
			_isSetXSliderPositionRight = false;

			return false;
		}
	}

	/**
	 * @param devXGraph
	 * @return Returns <code>true</code> when the synch marker was hit
	 */
	private boolean isSynchMarkerHit(final int devXGraph) {

		final ChartDataXSerie xData = getXData();

		if (xData == null) {
			return false;
		}

		final int synchMarkerStartIndex = xData.getSynchMarkerStartIndex();
		final int synchMarkerEndIndex = xData.getSynchMarkerEndIndex();

		if (synchMarkerStartIndex == -1) {
			// synch marker is not set
			return false;
		}

		final double[] xValues = xData.getHighValuesDouble()[0];
		final double scaleX = getXDrawingData().getScaleX();

		final int devXMarkerStart = (int) (xValues[Math.min(synchMarkerStartIndex, xValues.length - 1)] * scaleX - _xxDevViewPortLeftBorder);
		final int devXMarkerEnd = (int) (xValues[Math.min(synchMarkerEndIndex, xValues.length - 1)] * scaleX - _xxDevViewPortLeftBorder);

		if (devXGraph >= devXMarkerStart && devXGraph <= devXMarkerEnd) {
			return true;
		}

		return false;
	}

	private ChartXSlider isXSliderHit(final int devXMouse, final int devYMouse) {

		ChartXSlider xSlider = null;

		if (_xSliderA.getHitRectangle().contains(devXMouse, devYMouse)) {
			xSlider = _xSliderA;
		} else if (_xSliderB.getHitRectangle().contains(devXMouse, devYMouse)) {
			xSlider = _xSliderB;
		}

		return xSlider;
	}

	/**
	 * check if the mouse hit an y-slider and returns the hit slider
	 * 
	 * @param graphX
	 * @param devY
	 * @return
	 */
	private ChartYSlider isYSliderHit(final int graphX, final int devY) {

		if (_ySliders == null) {
			return null;
		}

		final boolean isGraphOverlapped = _chartDrawingData.chartDataModel.isGraphOverlapped();
		final boolean isStackedChart = !isGraphOverlapped;

		int graphNo = 0;
		final int lastGraph = _ySliders.size();

		for (final ChartYSlider ySlider : _ySliders) {

			graphNo++;

			// there are 2 y-sliders for each graph
			final boolean isLastGraph = graphNo == lastGraph || graphNo == lastGraph - 1;
			final boolean isLastOverlappedGraph = isGraphOverlapped && isLastGraph;

			final boolean canDoHitChecking = !_canChartBeOverlapped
					|| (_canChartBeOverlapped && (isStackedChart || isLastOverlappedGraph));

			if (canDoHitChecking) {

				if (ySlider.getHitRectangle().contains(graphX, devY)) {

					_hitYSlider = ySlider;
					return ySlider;
				}
			}
		}

		// hide previously hitted y-slider
		if (_hitYSlider != null) {

			// redraw the sliders to hide the labels
			_hitYSlider = null;
			_isSliderDirty = true;
			redraw();
		}

		return null;
	}

	/**
	 * Move left slider to the mouse down position
	 */
	void moveLeftSliderHere() {

		final ChartXSlider leftSlider = getLeftSlider();
		final long xxDevLeftPosition = _xxDevViewPortLeftBorder + _devXMouseDown;

		setXSliderValue_FromHoveredValuePoint(leftSlider);
		leftSlider.moveToXXDevPosition(xxDevLeftPosition, true, true, false);

		setZoomInPosition();

		_isSliderDirty = true;
		redraw();
	}

	/**
	 * Move right slider to the mouse down position
	 */
	void moveRightSliderHere() {

		final ChartXSlider rightSlider = getRightSlider();
		final long xxDevRightPosition = _xxDevViewPortLeftBorder + _devXMouseDown;

		setXSliderValue_FromHoveredValuePoint(rightSlider);
		rightSlider.moveToXXDevPosition(xxDevRightPosition, true, true, false);

		setZoomInPosition();

		_isSliderDirty = true;
		redraw();
	}

	private void moveSlidersToBorder() {

		if (_canAutoMoveSliders == false) {
			return;
		}

		moveSlidersToBorderWithoutCheck();
	}

	void moveSlidersToBorderWithoutCheck() {

		/*
		 * get the sliders first before they are moved
		 */
		final ChartXSlider leftSlider = getLeftSlider();
		final ChartXSlider rightSlider = getRightSlider();

		/*
		 * adjust left slider
		 */
		final long xxDevLeftPosition = _xxDevViewPortLeftBorder + 2;

		setXSliderValue_FromHoveredValuePoint(leftSlider);
		leftSlider.moveToXXDevPosition(xxDevLeftPosition, true, true, false);

		/*
		 * adjust right slider
		 */
		final long xxDevRightPosition = _xxDevViewPortLeftBorder + getDevVisibleChartWidth() - 2;

		setXSliderValue_FromHoveredValuePoint(rightSlider);
		rightSlider.moveToXXDevPosition(xxDevRightPosition, true, true, false);

		_isSliderDirty = true;
		redraw();
	}

	/**
	 * Move the slider to a new position
	 * 
	 * @param xSlider
	 *            Current slider
	 * @param xxDevSliderLinePos
	 *            x coordinate for the slider line within the graph, this can be outside of the
	 *            visible graph
	 */
	private void moveXSlider(final ChartXSlider xSlider, final long devXSliderLinePos) {

		long xxDevSliderLinePos = _xxDevViewPortLeftBorder + devXSliderLinePos;

		/*
		 * adjust the line position the the min/max width of the graph image
		 */
		xxDevSliderLinePos = Math.min(_xxDevGraphWidth, Math.max(0, xxDevSliderLinePos));

		// set new slider line position
		setXSliderValue_FromHoveredValuePoint(xSlider);
		xSlider.moveToXXDevPosition(xxDevSliderLinePos, true, true, false);
	}

	/**
	 * Dispose event handler
	 */
	private void onDispose() {

		// dispose resources
		_cursorResizeLeftRight = Util.disposeResource(_cursorResizeLeftRight);
		_cursorResizeTopDown = Util.disposeResource(_cursorResizeTopDown);
		_cursorDragged = Util.disposeResource(_cursorDragged);
		_cursorArrow = Util.disposeResource(_cursorArrow);
		_cursorModeSlider = Util.disposeResource(_cursorModeSlider);
		_cursorModeZoom = Util.disposeResource(_cursorModeZoom);
		_cursorModeZoomMove = Util.disposeResource(_cursorModeZoomMove);
		_cursorDragXSlider_ModeZoom = Util.disposeResource(_cursorDragXSlider_ModeZoom);
		_cursorDragXSlider_ModeSlider = Util.disposeResource(_cursorDragXSlider_ModeSlider);
		_cursorHoverXSlider = Util.disposeResource(_cursorHoverXSlider);

		_cursorMove1x = Util.disposeResource(_cursorMove1x);
		_cursorMove2x = Util.disposeResource(_cursorMove2x);
		_cursorMove3x = Util.disposeResource(_cursorMove3x);
		_cursorMove4x = Util.disposeResource(_cursorMove4x);
		_cursorMove5x = Util.disposeResource(_cursorMove5x);

		_cursorXSliderLeft = Util.disposeResource(_cursorXSliderLeft);
		_cursorXSliderRight = Util.disposeResource(_cursorXSliderRight);

		_chartImage_20_Chart = Util.disposeResource(_chartImage_20_Chart);
		_chartImage_10_Graphs = Util.disposeResource(_chartImage_10_Graphs);
		_chartImage_40_Overlay = Util.disposeResource(_chartImage_40_Overlay);
		_chartImage_30_Custom = Util.disposeResource(_chartImage_30_Custom);

		_gridColor = Util.disposeResource(_gridColor);
		_gridColorMajor = Util.disposeResource(_gridColorMajor);

		_hoveredBarToolTip.dispose();

		_colorCache.dispose();
	}

	private void onKeyDown(final Event event) {

		final ChartType chartType = _chart.getChartDataModel().getChartType();

		if (chartType == ChartType.BAR) {

			_chartComponents.selectBarItem(event);

		} else if (chartType == ChartType.LINE) {

			switch (event.character) {
			case '+':
				_chart.onExecuteZoomIn();
				break;

			case '-':
				_chart.onExecuteZoomOut(true);
				break;

			default:
				onKeyDownMoveXSlider(event);
			}
		}
	}

	/**
	 * move the x-slider with the keyboard
	 * 
	 * @param event
	 */
	private void onKeyDownMoveXSlider(final Event event) {

		final int keyCode = event.keyCode;

		/*
		 * keyboard events behaves different than the mouse event, shift & ctrl can be set in both
		 * event fields
		 */
		boolean isShift = (event.stateMask & SWT.SHIFT) != 0 || (keyCode & SWT.SHIFT) != 0;
		boolean isCtrl = (event.stateMask & SWT.CTRL) != 0 || (keyCode & SWT.CTRL) != 0;

		// ensure a slider is selected
		if (_selectedXSlider == null) {
			final ChartXSlider leftSlider = getLeftSlider();
			if (leftSlider != null) {
				// set default slider
				_selectedXSlider = leftSlider;
			} else {
				return;
			}
		}

		// toggle selected slider with the shift key
		if (isShift && isCtrl == false) {
			_selectedXSlider = _selectedXSlider == _xSliderA ? _xSliderB : _xSliderA;
			_isSliderDirty = true;
			redraw();

			return;
		}

		// accelerate with page up/down
		if (keyCode == SWT.PAGE_UP || keyCode == SWT.PAGE_DOWN) {
			isCtrl = true;
			isShift = true;
		}

		// accelerate slider move speed depending on shift/ctrl key
		int valueIndexDiff = isCtrl ? 10 : 1;
		valueIndexDiff *= isShift ? 10 : 1;

		int valueIndex = _selectedXSlider.getValuesIndex();
		final double[] xValues = getXData().getHighValuesDouble()[0];

		boolean isMoveSlider = false;

//		if (isShift && isCtrl) {
//
//			/*
//			 * this will reposition the x-slider to the exact value position in the graph, the Ctrl
//			 * key must be pressed first before the Shift key otherwise the slider is toggles
//			 */
//			isMoveSlider = true;
//
//		} else
		{

			switch (keyCode) {
			case SWT.PAGE_DOWN:
			case SWT.ARROW_RIGHT:

				valueIndex += valueIndexDiff;

				// wrap around
				if (valueIndex >= xValues.length) {
					valueIndex = 0;
				}

				isMoveSlider = true;

				break;

			case SWT.PAGE_UP:
			case SWT.ARROW_LEFT:

				valueIndex -= valueIndexDiff;

				// wrap around
				if (valueIndex < 0) {
					valueIndex = xValues.length - 1;
				}

				isMoveSlider = true;

				break;

			case SWT.HOME:

				valueIndex = 0;

				isMoveSlider = true;

				break;

			case SWT.END:

				valueIndex = xValues.length - 1;

				isMoveSlider = true;

				break;
			}
		}

		if (isMoveSlider) {

			setXSliderValueIndex(_selectedXSlider, valueIndex, false);

			redraw();
			setCursorStyle(event.y);
		}
	}

	void onMouseDoubleClick(final MouseEvent e) {

		final long eventTime = e.time & 0xFFFFFFFFL;
		final int devXMouse = e.x;
		final int devYMouse = e.y;

		// stop dragging the x-slider
		_xSliderDragged = null;

		@SuppressWarnings("unused")
		ChartMouseEvent mouseEvent;

		if ((mouseEvent = _chart.onExternalMouseDoubleClick(eventTime, devXMouse, devYMouse)).isWorked) {

//			setChartCursor(mouseEvent.cursor);

//			_isOverlayDirty = true;
//			isRedraw = true;
//
//			canShowHoveredTooltip = true;

		} else if (_hoveredBarSerieIndex != -1) {

			/*
			 * execute the action which is defined when a bar is selected with the left mouse button
			 */

			_chart.fireChartDoubleClick(_hoveredBarSerieIndex, _hoveredBarValueIndex);

		} else {

			if ((e.stateMask & SWT.CONTROL) != 0) {

				// toggle mouse mode

				if (_chart.getMouseMode().equals(Chart.MOUSE_MODE_SLIDER)) {

					// switch to mouse zoom mode
					_chart.setMouseMode(false);

				} else {

					// switch to mouse slider mode
					_chart.setMouseMode(true);
				}

			} else {

				if (_chart.getMouseMode().equals(Chart.MOUSE_MODE_SLIDER)) {

					// switch to mouse zoom mode
					_chart.setMouseMode(false);
				}

				// mouse mode: zoom chart

				/*
				 * set position where the double click occured, this position will be used when the
				 * chart is zoomed
				 */
				final double xxDevMousePosInChart = _xxDevViewPortLeftBorder + e.x;
				_zoomRatioCenter = xxDevMousePosInChart / _xxDevGraphWidth;

				zoomInWithMouse(Integer.MIN_VALUE);
			}
		}
	}

	/**
	 * Mouse down event handler
	 * 
	 * @param event
	 */
	private void onMouseDown(final MouseEvent event) {

		hideTooltip();

		// zoom out to show the whole chart with the button on the left side
		if (event.button == 4) {
			_chart.onExecuteZoomFitGraph();
			return;
		}

		final int devXMouse = event.x;
		final int devYMouse = event.y;

		final boolean isShift = (event.stateMask & SWT.SHIFT) != 0;
		final boolean isCtrl = (event.stateMask & SWT.CTRL) != 0;

		_devXMouseDown = devXMouse;
		_devYMouseDown = devYMouse;

		// show slider context menu
		if (event.button != 1) {

			// stop dragging the x-slider
			_xSliderDragged = null;

			// prevent that after the context menu is closed with a mouse click, the x-slider starts dragging
			_isSetXSliderPositionLeft = false;
			_isSetXSliderPositionRight = false;

			if (event.button == 3) {

				// right button is pressed

// this was disabled because it is not working on OSX, partly it works,
// propably depending on the mouse: OSX mouse or Logitech mouse
//				computeSliderForContextMenu(devXMouse, devYMouse);
			}
			return;
		}

		// use external mouse event listener
		ChartMouseEvent mouseEvent;
		if ((mouseEvent = _chart.onExternalMouseDownPre(event.time & 0xFFFFFFFFL, devXMouse, devYMouse)).isWorked) {

			setChartCursor(mouseEvent.cursor);
			return;
		}

		if (_xSliderDragged != null) {

			// x-slider is dragged

			// keep x-slider
			final ChartXSlider xSlider = _xSliderDragged;

			// stop dragging the slider
			_xSliderDragged = null;

			// set mouse zoom double click position
			final double xxDevMousePosInChart = _xxDevViewPortLeftBorder + devXMouse;
			_zoomRatioCenter = xxDevMousePosInChart / _xxDevGraphWidth;

			/*
			 * make sure that the slider is exactly positioned where the value is displayed in the
			 * graph
			 */
			setXSliderValueIndex(xSlider, _hoveredValuePointIndex, false);

			_isSliderDirty = true;
			redraw();

		} else {

			// check if a x-slider was hit
			_xSliderDragged = null;
			if (_xSliderA.getHitRectangle().contains(devXMouse, devYMouse)) {
				_xSliderDragged = _xSliderA;
			} else if (_xSliderB.getHitRectangle().contains(devXMouse, devYMouse)) {
				_xSliderDragged = _xSliderB;
			}

			if (_xSliderDragged != null) {

				// x-slider is dragged, stop dragging

				_xSliderOnTop = _xSliderDragged;
				_xSliderOnBottom = _xSliderOnTop == _xSliderA ? _xSliderB : _xSliderA;

				// set the hit offset for the mouse click
				_xSliderDragged.setDevXClickOffset(devXMouse - _xxDevViewPortLeftBorder);

				// the hit x-slider is now the selected x-slider
				_selectedXSlider = _xSliderDragged;
				_isSelectionVisible = true;
				_isSliderDirty = true;

				redraw();

			} else if (_ySliderDragged != null) {

				// y-slider is dragged, stop dragging

				adjustYSlider();

			} else if ((_ySliderDragged = isYSliderHit(devXMouse, devYMouse)) != null) {

				// start y-slider dragging

				_ySliderDragged.devYClickOffset = (int) (devYMouse - _ySliderDragged.getHitRectangle().y);

			} else if (_hoveredBarSerieIndex != -1) {

				actionSelectBars();

			} else if (_chart._draggingListenerXMarker != null && isSynchMarkerHit(devXMouse)) {

				/*
				 * start to move the x-marker, when a dragging listener and the x-marker was hit
				 */

				_isXMarkerMoved = getXData().getSynchMarkerStartIndex() != -1;

				if (_isXMarkerMoved) {

					_devXMarkerDraggedStartPos = devXMouse;
					_devXMarkerDraggedPos = devXMouse;
					_xMarkerValueDiff = _chart._draggingListenerXMarker.getXMarkerValueDiff();

					_isSliderDirty = true;
					redraw();
				}

			} else if (_isXSliderVisible //
					//
					// x-slider is NOT dragged
					&& _xSliderDragged == null
					//
					&& (isShift || isCtrl || _isSetXSliderPositionLeft || _isSetXSliderPositionRight)) {

				// position the x-slider and start dragging it

				if (_isSetXSliderPositionLeft) {
					_xSliderDragged = getLeftSlider();
				} else if (_isSetXSliderPositionRight) {
					_xSliderDragged = getRightSlider();
				} else if (isCtrl) {
					// ctrl is pressed -> left slider
					_xSliderDragged = getRightSlider();
				} else {
					// shift is pressed -> right slider
					_xSliderDragged = getLeftSlider();
				}

				_xSliderOnTop = _xSliderDragged;
				_xSliderOnBottom = _xSliderOnTop == _xSliderA ? _xSliderB : _xSliderA;

				// the left x-slider is now the selected x-slider
				_selectedXSlider = _xSliderDragged;
				_isSelectionVisible = true;

				/*
				 * move the left slider to the mouse down position
				 */

				_xSliderDragged.setDevXClickOffset(devXMouse - _xxDevViewPortLeftBorder);

				// keep position of the slider line
				final int devXSliderLinePos = devXMouse;
				_devXDraggedXSliderLine = devXSliderLinePos;

				moveXSlider(_xSliderDragged, devXSliderLinePos);

				_isSliderDirty = true;
				redraw();

			} else if (_graphZoomRatio > 1) {

				// start dragging the chart

				/*
				 * to prevent flickering with the double click event, dragged started is used
				 */
				_isChartDraggedStarted = true;

				_draggedChartStartPos = new Point(event.x, event.y);

				/*
				 * set also the move position because when changing the data model, the old position
				 * will be used and the chart is painted on the wrong position on mouse down
				 */
				_draggedChartDraggedPos = _draggedChartStartPos;
			}
		}

		setCursorStyle(devYMouse);
	}

	/**
	 * Mouse down event in the x-axis area
	 * 
	 * @param event
	 */
	void onMouseDownAxis(final MouseEvent event) {

		hideTooltip();

		if (_xSliderDragged != null) {

			// stop dragging the slider
			_xSliderDragged = null;

			doAutoZoomToXSliders();
		}
	}

	private void onMouseEnter(final MouseEvent mouseEvent) {

		if (_ySliderDragged != null) {

			_hitYSlider = _ySliderDragged;

			_isSliderDirty = true;
			redraw();
		}

	}

	void onMouseEnterAxis(final MouseEvent event) {

		// simulate a mouse move to do autoscrolling
		onMouseMoveAxis(event);
	}

	/**
	 * Mouse exit event handler
	 * 
	 * @param event
	 */
	private void onMouseExit(final MouseEvent event) {

		_chart.onExternalMouseExit(event.time);

		_hoveredBarToolTip.toolTip20Hide();

		boolean isRedraw = false;

		if (_isAutoScroll) {

			// stop autoscrolling
			_isAutoScroll = false;

		} else if (_xSliderDragged == null) {

			// hide the y-slider labels
			if (_hitYSlider != null) {
				_hitYSlider = null;

				_isSliderDirty = true;

				isRedraw = true;
			}
		}

		if (_mouseOverXSlider != null) {
			// mouse left the x-slider
			_mouseOverXSlider = null;
			_isSliderDirty = true;

			isRedraw = true;
		}

		setCursorStyle(event.y);

		if (isRedraw) {
			redraw();
		}
	}

	/**
	 * @param mouseEvent
	 * @return Returns <code>true</code> when the mouse event was handled.
	 */
	boolean onMouseExitAxis(final MouseEvent mouseEvent) {

		if (_isAutoScroll) {

			// stop autoscrolling with x-slider
			_isAutoScroll = false;

			// stop autoscrolling without x-slider
			_devXAutoScrollMousePosition = Integer.MIN_VALUE;

			// hide move/scroll cursor
			if (mouseEvent.widget instanceof ChartComponentAxis) {
				((ChartComponentAxis) mouseEvent.widget).setCursor(null);
			}

			return true;
		}

		return false;
	}

	/**
	 * Mouse move event handler
	 * 
	 * @param eventTime
	 * @param eventTime
	 */
	private void onMouseMove(final long eventTime, final int devXMouse, final int devYMouse) {

		_devXMouseMove = devXMouse;
		_devYMouseMove = devYMouse;

		boolean isRedraw = false;
		boolean canShowHoveredValueTooltip = false;

		if (_isXSliderVisible && _xSliderDragged != null) {

			// x-slider is dragged

			canShowHoveredValueTooltip = true;

			// keep position of the slider line
			_devXDraggedXSliderLine = devXMouse;

			/*
			 * when the x-slider is outside of the visual graph in horizontal direction, the graph
			 * can be scrolled with the mouse
			 */
			final int devVisibleChartWidth = getDevVisibleChartWidth();
			if (_devXDraggedXSliderLine > -1 && _devXDraggedXSliderLine < devVisibleChartWidth) {

				// slider is within the visible area, autoscrolling is NOT done

				// autoscroll could be active, disable it
				_isAutoScroll = false;

				moveXSlider(_xSliderDragged, devXMouse);

				_isSliderDirty = true;
				isRedraw = true;

			} else {

				/*
				 * slider is outside the visible area, auto scroll the slider and graph when this is
				 * not yet done
				 */
				if (_isAutoScroll == false) {
					doAutoScroll(eventTime);
				}
			}

		} else if (_isChartDraggedStarted || _isChartDragged) {

			// chart is dragged with the mouse

			_isChartDraggedStarted = false;
			_isChartDragged = true;

			_draggedChartDraggedPos = new Point(devXMouse, devYMouse);

			isRedraw = true;

		} else if (_isYSliderVisible && _ySliderDragged != null) {

			// y-slider is dragged

			final int devYSliderLine = devYMouse
					- _ySliderDragged.devYClickOffset
					+ ChartYSlider.halfSliderHitLineHeight;

			_ySliderDragged.setDevYSliderLine(devYSliderLine);
			_ySliderGraphX = devXMouse;

			_isSliderDirty = true;
			isRedraw = true;

		} else if (_isXMarkerMoved) {

			// X-Marker is dragged

			_devXMarkerDraggedPos = devXMouse;

			_isSliderDirty = true;
			isRedraw = true;

		} else {

			ChartXSlider xSlider;

			final ChartMouseEvent externalMouseEvent = _chart.onExternalMouseMove(eventTime, devXMouse, devYMouse);

			if (externalMouseEvent.isWorked) {

				setChartCursor(externalMouseEvent.cursor);

				_isOverlayDirty = true;
				isRedraw = true;

				canShowHoveredValueTooltip = true;

			} else if (_isXSliderVisible && (xSlider = isXSliderHit(devXMouse, devYMouse)) != null) {

				// mouse is over an x-slider

				if (_mouseOverXSlider != xSlider) {

					// a new x-slider is hovered

					_mouseOverXSlider = xSlider;

					// hide the y-slider
					_hitYSlider = null;

					_isSliderDirty = true;
					isRedraw = true;
				}

				// set cursor
				setCursor(_cursorResizeLeftRight);

				canShowHoveredValueTooltip = true;

			} else if (_mouseOverXSlider != null) {

				// mouse has left the x-slider

				_mouseOverXSlider = null;
				_isSliderDirty = true;
				isRedraw = true;

				canShowHoveredValueTooltip = true;

			} else if (_isYSliderVisible && isYSliderHit(devXMouse, devYMouse) != null) {

				// cursor is within a y-slider

				setCursor(_cursorResizeTopDown);

				// show the y-slider labels
				_ySliderGraphX = devXMouse;

				_isSliderDirty = true;
				isRedraw = true;

				canShowHoveredValueTooltip = true;

			} else if (_chart._draggingListenerXMarker != null && isSynchMarkerHit(devXMouse)) {

				setCursor(_cursorDragged);

			} else if (_isXSliderVisible && isInXSliderSetArea(devYMouse)) {

				// cursor is already set

				canShowHoveredValueTooltip = true;

			} else if (isBarHit(devXMouse, devYMouse)) {

				_isHoveredBarDirty = true;
				isRedraw = true;

				setCursorStyle(devYMouse);

			} else {

				canShowHoveredValueTooltip = true;

				setCursorStyle(devYMouse);
			}
		}

		final IHoveredValueListener hoveredListener = _chart._hoveredListener;

		if (_isHoveredLineVisible || hoveredListener != null) {

			setHoveredLineValue();

			if (_hoveredValuePointIndex != -1) {

				final PointLong devHoveredValueDevPosition = getHoveredValueDevPosition();

				if (_isHoveredLineVisible) {

					if (valuePointToolTip != null) {

						valuePointToolTip.setValueIndex(
								_hoveredValuePointIndex,
								_devXMouseMove,
								_devYMouseMove,
								devHoveredValueDevPosition,
								_graphZoomRatio);
					}

					/*
					 * this redraw is necessary otherwise a hovered photo displayed as none hovered
					 * when mouse is not hovering a photo
					 */
					isRedraw = true;
				}

				if (hoveredListener != null && canShowHoveredValueTooltip) {

					hoveredListener.hoveredValue(
							eventTime,
							_devXMouseMove,
							_devYMouseMove,
							_hoveredValuePointIndex,
							devHoveredValueDevPosition);
				}
			}
		}

		if (isRedraw) {
			redraw();
		}
	}

	/**
	 * @param mouseEvent
	 * @return Returns <code>true</code> when the mouse event was been handled.
	 */
	boolean onMouseMoveAxis(final MouseEvent mouseEvent) {

		ChartComponentAxis axisComponent = null;
		int axisWidth = 0;

		int devXMouse = mouseEvent.x;
		int devYMouse = mouseEvent.y;
		final Widget mouseWidget = mouseEvent.widget;

		final ChartComponentAxis axisLeft = _chartComponents.getAxisLeft();
		final ChartComponentAxis axisRight = _chartComponents.getAxisRight();

		boolean isMouseFromRightToolTip = false;
		boolean isMouseFromLeftToolTip = false;

		if (mouseWidget instanceof ChartComponentAxis) {

			axisComponent = (ChartComponentAxis) mouseWidget;
			axisWidth = axisComponent.getAxisClientArea().width;

		} else if (valuePointToolTip != null) {

			// check if the event widget is from the tooltip

			// get tooltip shell
			final Shell ttShell = valuePointToolTip.getToolTipShell();
			if (ttShell != null) {

				if (mouseWidget instanceof Control) {
					final Control control = (Control) mouseWidget;

					if (control.getShell() == ttShell) {

						/*
						 * this event is from the value point tooltip, the control is the tooltip
						 */

						final Point screenTTMouse = control.toDisplay(devXMouse, devYMouse);

						final Point leftAxisScreen = axisLeft.toDisplay(0, 0);
						final Point leftAxisSize = axisLeft.getSize();

						final Rectangle leftAxisRect = new Rectangle(
								leftAxisScreen.x,
								leftAxisScreen.y,
								leftAxisSize.x,
								leftAxisSize.y);

						if (leftAxisRect.contains(screenTTMouse)) {

							// mouse is moved above the left axis

							final Point devLeftAxis = axisLeft.toControl(screenTTMouse);
							devXMouse = devLeftAxis.x;
							devYMouse = devLeftAxis.y;
							axisComponent = axisLeft;
							axisWidth = leftAxisSize.x;

							isMouseFromLeftToolTip = true;

						} else {

							final Point rightAxisScreen = axisRight.toDisplay(0, 0);
							final Point rightAxisSize = axisRight.getSize();

							final Rectangle rightAxisRect = new Rectangle(
									rightAxisScreen.x,
									rightAxisScreen.y,
									rightAxisSize.x,
									rightAxisSize.y);

							if (rightAxisRect.contains(screenTTMouse)) {

								// mouse is moved above the right axis

								final Point devRightAxis = axisRight.toControl(screenTTMouse);
								devXMouse = devRightAxis.x;
								devYMouse = devRightAxis.y;
								axisComponent = axisRight;
								axisWidth = rightAxisSize.x;

								isMouseFromRightToolTip = true;
							}
						}
					}
				}
			}
		}

		final boolean isLeftAxis = axisComponent == axisLeft;
		final int marginBottom = _chartComponents.getMarginBottomStartingFromTop();

		if (axisWidth == 0 || axisComponent == null //

				// chart is not zoomed
				|| _graphZoomRatio == 1

				/*
				 * ensure the mouse is moved from the graph from the tooltip or is moved over the
				 * tooltip when it's above the y-axis
				 */
				&& isMouseFromLeftToolTip == false
				&& isMouseFromRightToolTip == false

				// mouse is above the bottom margin
				|| devYMouse < marginBottom
		//
		) {

			// disable autoscroll
			_isAutoScroll = false;

			if (axisComponent != null) {
				axisComponent.setCursor(null);
			}

			return false;
		}

		final Cursor cursor;

		if (_isXSliderVisible && _xSliderDragged != null) {

			// x-slider is dragged, do autoscroll the graph with the mouse

			if (isLeftAxis) {

				// left x-axis

				_devXDraggedXSliderLine = -axisWidth + devXMouse;

				cursor = //
				_devXDraggedXSliderLine < _leftAccelerator[0][0] ? _cursorMove5x : //
						_devXDraggedXSliderLine < _leftAccelerator[1][0] ? _cursorMove4x : //
								_devXDraggedXSliderLine < _leftAccelerator[2][0] ? _cursorMove3x : //
										_devXDraggedXSliderLine < _leftAccelerator[3][0] ? _cursorMove2x : //
												_cursorMove1x;

			} else {

				// right x-axis

				_devXDraggedXSliderLine = getDevVisibleChartWidth() + devXMouse;

				cursor = //
				devXMouse < _rightAccelerator[0][0] ? _cursorMove1x : //
						devXMouse < _rightAccelerator[1][0] ? _cursorMove2x : //
								devXMouse < _rightAccelerator[2][0] ? _cursorMove3x : //
										devXMouse < _rightAccelerator[3][0] ? _cursorMove4x : //
												_cursorMove5x;
			}

		} else {

			// do autoscroll the graph with the moved mouse

			// set mouse position and do autoscrolling

			if (isLeftAxis) {

				// left x-axis

				_devXAutoScrollMousePosition = -axisWidth + devXMouse;

				cursor = //
				_devXAutoScrollMousePosition < _leftAccelerator[0][0] ? _cursorMove5x : //
						_devXAutoScrollMousePosition < _leftAccelerator[1][0] ? _cursorMove4x : //
								_devXAutoScrollMousePosition < _leftAccelerator[2][0] ? _cursorMove3x : //
										_devXAutoScrollMousePosition < _leftAccelerator[3][0] ? _cursorMove2x : //
												_cursorMove1x;

			} else {

				// right x-axis

				_devXAutoScrollMousePosition = getDevVisibleChartWidth() + devXMouse;

				cursor = //
				devXMouse < _rightAccelerator[0][0] ? _cursorMove1x : //
						devXMouse < _rightAccelerator[1][0] ? _cursorMove2x : //
								devXMouse < _rightAccelerator[2][0] ? _cursorMove3x : //
										devXMouse < _rightAccelerator[3][0] ? _cursorMove4x : //
												_cursorMove5x;
			}
		}

		axisComponent.setCursor(cursor);

		if (_isAutoScroll == false) {
			// start scrolling when not yet done
			doAutoScroll(mouseEvent.time & 0xFFFFFFFFL);
		}

		return true;
	}

	/**
	 * Mouse up event handler
	 * 
	 * @param event
	 */
	private void onMouseUp(final MouseEvent event) {

		final int devXMouse = event.x;
		final int devYMouse = event.y;

		ChartMouseEvent mouseEvent;

		if (_isAutoScroll) {

			// stop auto scolling
			_isAutoScroll = false;

			/*
			 * make sure that the sliders are at the border of the visible area are at the border
			 */
			if (_devXDraggedXSliderLine < 0) {
				moveXSlider(_xSliderDragged, 0);
			} else {
				final int devVisibleChartWidth = getDevVisibleChartWidth();
				if (_devXDraggedXSliderLine > devVisibleChartWidth - 1) {
					moveXSlider(_xSliderDragged, devVisibleChartWidth - 1);
				}
			}

			// disable dragging
			_xSliderDragged = null;

			// redraw slider
			_isSliderDirty = true;
			redraw();

		} else if (_ySliderDragged != null) {

			// y-slider is dragged, stop dragging

			adjustYSlider();

		} else if (_isXMarkerMoved) {

			_isXMarkerMoved = false;

			_isSliderDirty = true;
			redraw();

			// call the listener which is registered for dragged x-marker
			if (_chart._draggingListenerXMarker != null) {
				_chart._draggingListenerXMarker.xMarkerMoved(_movedXMarkerStartValueIndex, _movedXMarkerEndValueIndex);
			}

		} else if (_isChartDragged || _isChartDraggedStarted) {

			// chart was moved with the mouse

			_isChartDragged = false;
			_isChartDraggedStarted = false;

			updateDraggedChart(_draggedChartDraggedPos.x - _draggedChartStartPos.x);

		} else if ((mouseEvent = _chart.onExternalMouseUp(event.time & 0xFFFFFFFFL, devXMouse, devYMouse)).isWorked) {

			setChartCursor(mouseEvent.cursor);
			return;
		}

		setCursorStyle(devYMouse);
	}

	void onMouseWheel(final Event event, final boolean isEventFromAxis, final boolean isLeftAxis) {

		if (_isGraphVisible == false) {
			return;
		}

		if (_chart.getMouseMode().equals(Chart.MOUSE_MODE_SLIDER)) {

			// mouse mode: move slider

			/**
			 * when a slider in a graph is moved with the mouse wheel the direction is the same as
			 * when the mouse wheel is scrolling in the tour editor:
			 * <p>
			 * wheel up -> tour editor up
			 */
			if (event.count < 0) {
				event.keyCode |= SWT.ARROW_RIGHT;
			} else {
				event.keyCode |= SWT.ARROW_LEFT;
			}

			/*
			 * set focus when the mouse is over the chart and the mousewheel is scrolled, this will
			 * also activate the part with the chart component
			 */
			if (isFocusControl() == false) {
				forceFocus();
			}

			onKeyDown(event);

			if (_canAutoZoomToSlider) {

				/*
				 * zoom the chart
				 */
				Display.getCurrent().asyncExec(new Runnable() {
					@Override
					public void run() {

						zoomInWithSlider();
						_chartComponents.onResize();

//						if (event.count < 0) {}
					}
				});
			}

		} else {

			// mouse mode: zoom chart

//			hideTooltip();

			final boolean isCtrl = (event.stateMask & SWT.CONTROL) != 0;
			final boolean isShift = (event.stateMask & SWT.SHIFT) != 0;

//			final boolean isShift = (event.stateMask & SWT.SHIFT) != 0 || (event.keyCode & SWT.SHIFT) != 0;
//			final boolean isCtrl = (event.stateMask & SWT.CTRL) != 0 || (event.keyCode & SWT.CTRL) != 0;

			if (isCtrl || isShift) {

				// scroll the chart

				int devXDiff = 0;
				if (event.count < 0) {
					devXDiff = -10;
				} else {
					devXDiff = 10;
				}

				if (isShift) {
					devXDiff *= 10;
				}

				updateDraggedChart(devXDiff);

			} else {

				// zoom the chart

				if (isEventFromAxis) {
					// set zoom center position to the left or right side, that
					_zoomRatioCenter = isLeftAxis ? 0.0 : 1.0;
				}

				if (event.count < 0) {
					zoomOutWithMouse(true, _devXMouseMove);
				} else {
					zoomInWithMouse(_devXMouseMove);
				}

				moveSlidersToBorder();
			}
		}

		/*
		 * prevent scrolling the scrollbar, scrolling is done by the chart itself
		 */
		event.doit = false;
	}

	/**
	 * Scroll event handler
	 * 
	 * @param event
	 */
	private void onScroll(final SelectionEvent event) {
		redraw();
	}

	/**
	 * make the graph dirty and redraw it
	 * 
	 * @param isGraphDirty
	 */
	void redrawBarSelection() {

		if (isDisposed()) {
			return;
		}

		_isSelectionDirty = true;
		redraw();
	}

	void redrawChart() {

		if (isDisposed()) {
			return;
		}

		_isChartDirty = true;
		redraw();
	}

	void redrawLayer() {

		if (isDisposed()) {
			return;
		}

		_isCustomLayerImageDirty = true;

		redraw();
	}

	/**
	 * set the slider position when the data model has changed
	 */
	void resetSliders() {

		// first get the left/right slider
		final ChartXSlider leftSlider = getLeftSlider();
		final ChartXSlider rightSlider = getRightSlider();

		/*
		 * reset the sliders, the temp sliders are used so that the same slider is not reset twice
		 */
		leftSlider.reset();
		rightSlider.reset();

//		_isSliderDirty = true;
//		redraw();
	}

	/**
	 * select the next bar item
	 */
	int selectBarItemNext() {

		int selectedIndex = Chart.NO_BAR_SELECTION;

		if (_selectedBarItems == null || _selectedBarItems.length == 0) {
			return selectedIndex;
		}

		// find selected Index and reset last selected bar item(s)
		for (int index = 0; index < _selectedBarItems.length; index++) {
			if (selectedIndex == Chart.NO_BAR_SELECTION && _selectedBarItems[index]) {
				selectedIndex = index;
			}
			_selectedBarItems[index] = false;
		}

		if (selectedIndex == Chart.NO_BAR_SELECTION) {

			// a bar item is not selected, select first
			selectedIndex = 0;

		} else {

			// select next bar item

			if (selectedIndex == _selectedBarItems.length - 1) {
				/*
				 * last bar item is currently selected, select the first bar item
				 */
				selectedIndex = 0;
			} else {
				// select next bar item
				selectedIndex++;
			}
		}

		_selectedBarItems[selectedIndex] = true;

		redrawBarSelection();

		return selectedIndex;
	}

	/**
	 * select the previous bar item
	 */
	int selectBarItemPrevious() {

		int selectedIndex = Chart.NO_BAR_SELECTION;

		// make sure that selectable bar items are available
		if (_selectedBarItems == null || _selectedBarItems.length == 0) {
			return selectedIndex;
		}

		// find selected item, reset last selected bar item(s)
		for (int index = 0; index < _selectedBarItems.length; index++) {
			// get the first selected item if there are many selected
			if (selectedIndex == -1 && _selectedBarItems[index]) {
				selectedIndex = index;
			}
			_selectedBarItems[index] = false;
		}

		if (selectedIndex == Chart.NO_BAR_SELECTION) {

			// a bar item is not selected, select first
			selectedIndex = 0;

		} else {

			// select next bar item

			if (selectedIndex == 0) {
				/*
				 * first bar item is currently selected, select the last bar item
				 */
				selectedIndex = _selectedBarItems.length - 1;
			} else {
				// select previous bar item
				selectedIndex = selectedIndex - 1;
			}
		}

		_selectedBarItems[selectedIndex] = true;

		redrawBarSelection();

		return selectedIndex;
	}

	void setCanAutoMoveSlidersWhenZoomed(final boolean canMoveSlidersWhenZoomed) {
		_canAutoMoveSliders = canMoveSlidersWhenZoomed;
	}

	/**
	 * @param canAutoZoomToSlider
	 *            the canAutoZoomToSlider to set
	 */
	void setCanAutoZoomToSlider(final boolean canAutoZoomToSlider) {

		_canAutoZoomToSlider = canAutoZoomToSlider;
	}

	private void setChartCursor(final ChartCursor cursor) {

		if (cursor == null) {
			return;
		}

		switch (cursor) {
		case Arrow:
			setCursor(_cursorArrow);
			break;

		case Dragged:
			setCursor(_cursorDragged);
			break;

		default:
			setCursor(null);
			break;
		}
	}

	void setChartOverlayDirty() {

		_isOverlayDirty = true;

		redraw();
	}

	/**
	 * Move a zoomed chart so that the slider is visible.
	 * 
	 * @param slider
	 * @param isCenterSliderPosition
	 */
	private void setChartPosition(final ChartXSlider slider, final boolean isCenterSliderPosition) {

		if (_graphZoomRatio == 1) {
			// chart is not zoomed, nothing to do
			return;
		}

		final long xxDevSliderLinePos = slider.getXXDevSliderLinePos();

		final int devXViewPortWidth = getDevVisibleChartWidth();
		final long xxDevCenter = xxDevSliderLinePos - devXViewPortWidth / 2;

		double xxDevOffset = xxDevSliderLinePos;

		if (isCenterSliderPosition) {

			xxDevOffset = xxDevCenter;

		} else {

			/*
			 * check if the slider is in the visible area
			 */
			if (xxDevSliderLinePos < _xxDevViewPortLeftBorder) {

				xxDevOffset = xxDevSliderLinePos + 1;

			} else if (xxDevSliderLinePos > _xxDevViewPortLeftBorder + devXViewPortWidth) {

				xxDevOffset = xxDevSliderLinePos - devXViewPortWidth;
			}
		}

		if (xxDevOffset != xxDevSliderLinePos) {

			/*
			 * slider is not visible
			 */

			// check left border
			xxDevOffset = Math.max(xxDevOffset, 0);

			// check right border
			xxDevOffset = Math.min(xxDevOffset, _xxDevGraphWidth - devXViewPortWidth);

			_zoomRatioLeftBorder = xxDevOffset / _xxDevGraphWidth;

			/*
			 * reposition the mouse zoom position
			 */
			final double xOffsetMouse = _xxDevViewPortLeftBorder + devXViewPortWidth / 2;
			_zoomRatioCenter = xOffsetMouse / _xxDevGraphWidth;

			updateVisibleMinMaxValues();

			/*
			 * prevent to display the old chart image
			 */
			_isChartDirty = true;

			_chartComponents.onResize();
		}

		/*
		 * set position where the double click occured, this position will be used when the chart is
		 * zoomed
		 */
		_zoomRatioCenter = (double) xxDevSliderLinePos / _xxDevGraphWidth;
	}

	/**
	 * Move a zoomed chart to a new position
	 * 
	 * @param xxDevNewPosition
	 */
	private void setChartPosition(final long xxDevNewPosition) {

		if (_graphZoomRatio == 1) {
			// chart is not zoomed, nothing to do
			return;
		}

		final int devXViewPortWidth = getDevVisibleChartWidth();
		double xxDevNewPosition2 = xxDevNewPosition;

		if (xxDevNewPosition < _xxDevViewPortLeftBorder) {

			xxDevNewPosition2 = xxDevNewPosition + 1;

		} else if (xxDevNewPosition > _xxDevViewPortLeftBorder + devXViewPortWidth) {

			xxDevNewPosition2 = xxDevNewPosition - devXViewPortWidth;
		}

		// check left border
		xxDevNewPosition2 = Math.max(xxDevNewPosition2, 0);

		// check right border
		xxDevNewPosition2 = Math.min(xxDevNewPosition2, _xxDevGraphWidth - devXViewPortWidth);

		_zoomRatioLeftBorder = xxDevNewPosition2 / _xxDevGraphWidth;

		// reposition the mouse zoom position
		final double xOffsetMouse = _xxDevViewPortLeftBorder + devXViewPortWidth / 2;
		_zoomRatioCenter = xOffsetMouse / _xxDevGraphWidth;

		updateVisibleMinMaxValues();

		/*
		 * prevent to display the old chart image
		 */
		_isChartDirty = true;

		_chartComponents.onResize();

		/*
		 * set position where the double click occured, this position will be used when the chart is
		 * zoomed
		 */
		_zoomRatioCenter = (double) xxDevNewPosition / _xxDevGraphWidth;
	}

	void setCursorStyle(final int devYMouse) {

		final ChartDataModel chartDataModel = _chart.getChartDataModel();
		if (chartDataModel == null) {
			return;
		}

		final ChartType chartType = chartDataModel.getChartType();

		if (chartType == ChartType.LINE || chartType == ChartType.LINE_WITH_BARS) {

			final boolean isMouseModeSlider = _chart.getMouseMode().equals(Chart.MOUSE_MODE_SLIDER);

			if (_xSliderDragged != null) {

				// x-slider is dragged
				if (isMouseModeSlider) {
					setCursor(_cursorDragXSlider_ModeSlider);
				} else {
					setCursor(_cursorDragXSlider_ModeZoom);
				}

			} else if (_ySliderDragged != null) {

				// y-slider is dragged

				setCursor(_cursorResizeTopDown);

			} else if (_isChartDragged || _isChartDraggedStarted) {

				// chart is dragged
				setCursor(_cursorDragged);

			} else {

				// nothing is dragged

				if (isMouseModeSlider) {
					setCursor(_cursorModeSlider);
				} else {
					setCursor(_cursorModeZoom);
				}
			}
		} else {
			setCursor(null);
		}
	}

	/**
	 * Set a new configuration for the graph, the whole graph will be recreated. This method is
	 * called when the chart canvas is resized, chart is zoomed or scrolled which requires that the
	 * chart is recreated.
	 */
	void setDrawingData(final ChartDrawingData chartDrawingData) {

		_chartDrawingData = chartDrawingData;

		// create empty list if list is not available, so we do not need
		// to check for null and isEmpty
		_allGraphDrawingData = chartDrawingData.graphDrawingData;

		_isGraphVisible = _allGraphDrawingData != null && _allGraphDrawingData.isEmpty() == false;

		_canChartBeOverlapped = canChartBeOverlapped(_allGraphDrawingData);
		_isChartOverlapped = _chartDrawingData.chartDataModel.isGraphOverlapped();

		if (_canChartBeOverlapped && _isChartOverlapped) {

			/*
			 * Revert sequence that the top graph is painted as last and not as the first that it
			 * will overlap the other graphs.
			 */

			_revertedGraphDrawingData = new ArrayList<GraphDrawingData>();

			for (final GraphDrawingData graphDrawingData : _allGraphDrawingData) {
				_revertedGraphDrawingData.add(graphDrawingData);
			}

			Collections.reverse(_revertedGraphDrawingData);
		}

		// force all graphics to be recreated
		_isChartDirty = true;
		_isSliderDirty = true;
		_isCustomLayerImageDirty = true;
		_isSelectionDirty = true;

		if (_isDisableHoveredLineValueIndex) {
			/*
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			 * prevent setting new positions until the chart is redrawn otherwise the slider has the
			 * value index -1, the chart is flickering when autoscrolling and the map is WRONGLY /
			 * UGLY positioned
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			 */
			_isDisableHoveredLineValueIndex = false;
		} else {

			// prevent using old value index which can cause bound exceptions
			_hoveredValuePointIndex = -1;
			_lineDevPositions.clear();
			_lineFocusRectangles.clear();
		}

		// hide previous tooltip
		_hoveredBarToolTip.toolTip20Hide();

		// force the graph to be repainted
		redraw();
	}

	@Override
	public boolean setFocus() {

		boolean isFocus = false;

		if (setFocusToControl()) {

			// check if the chart has the focus
			if (isFocusControl()) {
				isFocus = true;
			} else {
				if (forceFocus()) {
					isFocus = true;
				}
			}
		}

		return isFocus;
	}

	/**
	 * Set the focus to a control depending on the chart type
	 * 
	 * @return Returns <code>true</code> when the focus was set
	 */
	private boolean setFocusToControl() {

		if (_isGraphVisible == false) {
			// we can't get the focus
			return false;
		}

		boolean isFocus = false;

		final ChartType chartType = _chart.getChartDataModel().getChartType();
		if (chartType == ChartType.LINE) {

			if (_selectedXSlider == null) {

				// set focus to the left slider when x-sliders are visible
				if (_isXSliderVisible) {

					_selectedXSlider = getLeftSlider();
					isFocus = true;
				}

			} else if (_selectedXSlider != null) {

				isFocus = true;
			}

		} else if (chartType == ChartType.BAR) {

			if (_selectedBarItems == null || _selectedBarItems.length == 0) {

				setSelectedBars(null);

			} else {

				// set focus to selected x-data

				int selectedIndex = -1;

				// find selected Index, reset last selected bar item(s)
				for (int index = 0; index < _selectedBarItems.length; index++) {
					if (selectedIndex == -1 && _selectedBarItems[index]) {
						selectedIndex = index;
					}
					_selectedBarItems[index] = false;
				}

				if (selectedIndex == -1) {

					// a bar item is not selected, select first

// disabled, 11.4.2008 wolfgang
//					fSelectedBarItems[0] = true;
//
//					fChart.fireBarSelectionEvent(0, 0);

				} else {

					// select last selected bar item

					_selectedBarItems[selectedIndex] = true;
				}

				redrawBarSelection();
			}
			isFocus = true;
		}

//		if (isFocus) {
//			_chart.fireFocusEvent();
//		}

		return isFocus;
	}

	void setGraphSize(final int xxDevGraphWidth, final int xxDevViewPortOffset, final double graphZoomRatio) {

		_xxDevGraphWidth = xxDevGraphWidth;
		_xxDevViewPortLeftBorder = xxDevViewPortOffset;
		_graphZoomRatio = graphZoomRatio;

		_xSliderA.moveToXXDevPosition(xxDevViewPortOffset, false, true, false);
		_xSliderB.moveToXXDevPosition(xxDevGraphWidth, false, true, false);
	}

	/**
	 * Check if mouse has moved over a line value and sets {@link #_hoveredValuePointIndex} to the
	 * value index or <code>-1</code> when focus rectangle is not hit.
	 */
	private void setHoveredLineValue() {

		if (_lineDevPositions.size() == 0) {
			return;
		}

		RectangleLong lineRect = null;

		for (final RectangleLong[] lineFocusRectangles : _lineFocusRectangles) {

			// find the line rectangle which is hovered by the mouse
			for (int valueIndex = 0; valueIndex < lineFocusRectangles.length; valueIndex++) {

				lineRect = lineFocusRectangles[valueIndex];

				// test if the mouse is within a bar focus rectangle
				if (lineRect != null) {

					// inline for lineRect.contains
					if ((_devXMouseMove >= lineRect.x)
							&& (_devYMouseMove >= lineRect.y)
							&& _devXMouseMove < (lineRect.x + lineRect.width)
							&& _devYMouseMove < (lineRect.y + lineRect.height)) {

						// keep the hovered line index
						_hoveredValuePointIndex = valueIndex;

						return;
					}
				}
			}
		}

		// reset index
		_hoveredValuePointIndex = -1;
	}

	void setHoveredTour(final ChartSegment tourSegment) {

		_hoveredSegment = tourSegment;

		redraw();
	}

	void setSelectedBars(final boolean[] selectedItems) {

		if (selectedItems == null) {

			// set focus to first bar item

			if (_allGraphDrawingData.size() == 0) {
				_selectedBarItems = null;
			} else {

				final GraphDrawingData graphDrawingData = _allGraphDrawingData.get(0);
				final ChartDataXSerie xData = graphDrawingData.getXData();

				_selectedBarItems = new boolean[xData._highValuesDouble[0].length];
			}

		} else {

			_selectedBarItems = selectedItems;
		}

		_isSelectionVisible = true;

		redrawBarSelection();
	}

	/**
	 * Set the value index in the X-slider for the hovered position.
	 * 
	 * @param xSlider
	 */
	void setXSliderValue_FromHoveredValuePoint(final ChartXSlider xSlider) {

		final ChartDataXSerie xData = getXData();

		if (xData == null) {
			return;
		}

		final double[][] xValueSerie = xData.getHighValuesDouble();

		if (xValueSerie.length == 0) {
			// data are not available
			return;
		}

		final double[] xDataValues = xValueSerie[0];

		if (_hoveredValuePointIndex == -1 || _hoveredValuePointIndex >= xDataValues.length) {

			// this happens when a new tour is displayed

			return;
		}

		xSlider.setValueIndex(_hoveredValuePointIndex);
	}

	/**
	 * Set the value index in the X-slider for the current slider position ratio.
	 * <p>
	 * The distance values (and time values with breaks) are not linear, the value is increasing
	 * steadily but with different distance on the x axis. So first we have to find the nearest
	 * position in the values array and then interpolite from the found position to the slider
	 * position.
	 * 
	 * @param xSlider
	 */
	void setXSliderValue_FromRatio(final ChartXSlider xSlider) {

		final ChartDataXSerie xData = getXData();

		if (xData == null) {
			return;
		}

		final double[][] xValueSerie = xData.getHighValuesDouble();

		if (xValueSerie.length == 0) {
			// data are not available
			return;
		}

		final double[] xDataValues = xValueSerie[0];
		final int serieLength = xDataValues.length;
		final int maxIndex = Math.max(0, serieLength - 1);

		/*
		 */

		final double minValue = xData.getOriginalMinValue();
		final double maxValue = xData.getOriginalMaxValue();
		final double valueRange = maxValue > 0 ? (maxValue - minValue) : -(minValue - maxValue);

		final double posRatio = xSlider.getPositionRatio();
		int valueIndex = (int) (posRatio * serieLength);

		// check array bounds
		valueIndex = Math.min(valueIndex, maxIndex);
		valueIndex = Math.max(valueIndex, 0);

		// sliderIndex points into the value array for the current slider position
		double xDataValue = xDataValues[valueIndex];

		// compute the value for the slider on the x-axis
		final double sliderValue = posRatio * valueRange;

		if (xDataValue == sliderValue) {

			// nothing to do

		} else if (sliderValue > xDataValue) {

			/*
			 * in the value array move towards the end to find the position where the value of the
			 * slider corresponds with the value in the value array
			 */

			while (sliderValue > xDataValue) {

				xDataValue = xDataValues[valueIndex++];

				// check if end of the x-data are reached
				if (valueIndex == serieLength) {
					break;
				}
			}
			valueIndex--;
			xDataValue = xDataValues[valueIndex];

		} else {

			/*
			 * xDataValue > sliderValue
			 */

			while (sliderValue < xDataValue) {

				// check if beginning of the x-data are reached
				if (valueIndex == 0) {
					break;
				}

				xDataValue = xDataValues[--valueIndex];
			}
		}

		/*
		 * This is a bit of a hack because at some positions the value is too small. Solving the
		 * problem in the algorithm would take more time than using this hack.
		 */
		if (xDataValue < sliderValue) {
			valueIndex++;
		}

		// check array bounds
		valueIndex = Math.min(valueIndex, maxIndex);
		xDataValue = xDataValues[valueIndex];

		// !!! debug values !!!
//		xValue = valueIndex * 1000;
//		xValue = (int) (slider.getPositionRatio() * 1000000000);

		xSlider.setValueIndex(valueIndex);
	}

	/**
	 * Set value index for a slider and move the slider to this position, the slider will be made
	 * visible.
	 * 
	 * @param slider
	 * @param valueIndex
	 * @param isCenterSliderPosition
	 * @param isBorderOffset
	 */
	void setXSliderValueIndex(final ChartXSlider slider, int valueIndex, final boolean isCenterSliderPosition) {

		final ChartDataXSerie xData = getXData();

		if (xData == null) {
			return;
		}

		final double[] xValues = xData.getHighValuesDouble()[0];
		final int xValueLastIndex = xValues.length - 1;

		// adjust the slider index to the array bounds
		valueIndex = valueIndex < 0 ? //
				0
				: valueIndex > xValueLastIndex ? //
						xValueLastIndex
						: valueIndex;

		final double xValue = xValues[valueIndex];
		final double lastXValue = xValues[xValueLastIndex];
		final double xxDevLinePos = _xxDevGraphWidth * xValue / lastXValue;

		slider.setValueIndex(valueIndex);
		slider.moveToXXDevPosition(xxDevLinePos, true, true, false);

		setChartPosition(slider, isCenterSliderPosition);

		_isSliderDirty = true;
	}

	/**
	 * makes the slider visible, a slider is only drawn into the chart if a slider was created with
	 * createSlider
	 * 
	 * @param isXSliderVisible
	 */
	void setXSliderVisible(final boolean isSliderVisible) {
		_isXSliderVisible = isSliderVisible;
	}

	/**
	 * Set ratio when the mouse is double clicked, this position is used to zoom the chart with the
	 * mouse.
	 */
	private void setZoomInPosition() {

		// get left+right slider
		final ChartXSlider leftSlider = getLeftSlider();
		final ChartXSlider rightSlider = getRightSlider();

		final long devLeftVirtualSliderLinePos = leftSlider.getXXDevSliderLinePos();

		final long devZoomInPosInChart = devLeftVirtualSliderLinePos
				+ ((rightSlider.getXXDevSliderLinePos() - devLeftVirtualSliderLinePos) / 2);

		_zoomRatioCenter = (double) devZoomInPosInChart / _xxDevGraphWidth;
	}

	/**
	 * Set left border ratio for the zoomed chart, zoomed graph width and ratio is already set.
	 */
	private void setZoomRatioLeftBorder() {

		final int devViewPortWidth = getDevVisibleChartWidth();

		double xxDevPosition = _zoomRatioCenter * _xxDevGraphWidth;
		xxDevPosition -= devViewPortWidth / 2;

		// ensure left border bounds
		xxDevPosition = Math.max(xxDevPosition, 0);

		// ensure right border by setting the left border value
		final long leftBorder = _xxDevGraphWidth - devViewPortWidth;
		xxDevPosition = Math.min(xxDevPosition, leftBorder);

		_zoomRatioLeftBorder = xxDevPosition / _xxDevGraphWidth;
	}

	/**
	 * switch the sliders to the 2nd x-data (switch between time and distance)
	 */
	void switchSlidersTo2ndXData() {
		switchSliderTo2ndXData(_xSliderA);
		switchSliderTo2ndXData(_xSliderB);
	}

	/**
	 * set the slider to the 2nd x-data and keep the slider on the same xValue position as before,
	 * this can cause to the situation, that the right slider gets unvisible/unhitable or the
	 * painted graph can have a white space on the right side
	 * 
	 * @param slider
	 *            the slider which gets changed
	 */
	private void switchSliderTo2ndXData(final ChartXSlider slider) {

		if (_allGraphDrawingData.size() == 0) {
			return;
		}

		final GraphDrawingData graphDrawingData = _allGraphDrawingData.get(0);
		if (graphDrawingData == null) {
			return;
		}

		final ChartDataXSerie data2nd = graphDrawingData.getXData2nd();

		if (data2nd == null) {
			return;
		}

		final double[] xValues = data2nd.getHighValuesDouble()[0];
		int valueIndex = slider.getValuesIndex();

		if (valueIndex >= xValues.length) {
			valueIndex = xValues.length - 1;
			slider.setValueIndex(valueIndex);
		}

		try {

			final double linePos = _xxDevGraphWidth * (xValues[valueIndex] / xValues[xValues.length - 1]);

			slider.moveToXXDevPosition(linePos, true, true, false);

		} catch (final ArrayIndexOutOfBoundsException e) {
			// ignore
		}
	}

	void updateChartSize() {

		if (valuePointToolTip == null) {
			return;
		}

		final int marginTop = _chartComponents.getDevChartMarginTop();
		final int marginBottom = _chartComponents.getDevChartMarginBottom();

		valuePointToolTip.setChartMargins(marginTop, marginBottom);

		// update custom overlay
		_isOverlayDirty = true;
		_isCustomOverlayInvalid = 99;
	}

	void updateCustomLayers() {

		if (isDisposed()) {
			return;
		}

		_isCustomLayerImageDirty = true;
		_isSliderDirty = true;

		redraw();
	}

	private void updateDraggedChart(final int devXDiff) {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		double devXOffset = _xxDevViewPortLeftBorder - devXDiff;

		// adjust left border
		devXOffset = Math.max(devXOffset, 0);

		// adjust right border
		devXOffset = Math.min(devXOffset, _xxDevGraphWidth - devVisibleChartWidth);

		_zoomRatioLeftBorder = devXOffset / _xxDevGraphWidth;

		/*
		 * reposition the mouse zoom position
		 */
		_zoomRatioCenter = ((_zoomRatioCenter * _xxDevGraphWidth) - devXDiff) / _xxDevGraphWidth;

		updateVisibleMinMaxValues();

		/*
		 * draw the dragged image until the graph image is recomuted
		 */
		_isPaintDraggedImage = true;

		_chartComponents.onResize();

		moveSlidersToBorder();
	}

	void updateGraphSize() {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		// calculate new virtual graph width
		_xxDevGraphWidth = (long) (_graphZoomRatio * devVisibleChartWidth);

		if (_graphZoomRatio == 1.0) {
			// with the ration 1.0 the graph is not zoomed
			_xxDevViewPortLeftBorder = 0;
		} else {
			// the graph is zoomed, only a part is displayed which starts at
			// the offset for the left slider
			_xxDevViewPortLeftBorder = (long) (_zoomRatioLeftBorder * _xxDevGraphWidth);
		}
	}

	/**
	 * Resize the sliders after the graph was resized
	 */
	void updateSlidersOnResize() {

		/*
		 * update all x-sliders
		 */
		final int visibleGraphHeight = getDevVisibleGraphHeight();
		_xSliderA.handleChartResize(visibleGraphHeight);
		_xSliderB.handleChartResize(visibleGraphHeight);

		/*
		 * update all y-sliders
		 */
		_ySliders = new ArrayList<ChartYSlider>();

		// loop: get all y-sliders from all graphs
		for (final GraphDrawingData drawingData : _allGraphDrawingData) {

			final ChartDataYSerie yData = drawingData.getYData();

			if (yData.isShowYSlider()) {

				final ChartYSlider sliderTop = yData.getYSlider1();
				final ChartYSlider sliderBottom = yData.getYSlider2();

				sliderTop.handleChartResize(drawingData, ChartYSlider.SLIDER_TYPE_TOP);
				_ySliders.add(sliderTop);

				sliderBottom.handleChartResize(drawingData, ChartYSlider.SLIDER_TYPE_BOTTOM);
				_ySliders.add(sliderBottom);

				_isYSliderVisible = true;
			}
		}
	}

	/**
	 * Set min/max values for the x/y-axis that the visible area will be filled with the chart
	 */
	void updateVisibleMinMaxValues() {

		final ChartDataModel chartDataModel = _chartComponents.getChartDataModel();
		final ChartDataXSerie xData = chartDataModel.getXData();
		final ArrayList<ChartDataYSerie> yDataList = chartDataModel.getYData();

		if (xData == null) {
			return;
		}

		final double[][] xValueSerie = xData.getHighValuesDouble();

		if (xValueSerie.length == 0) {
			// data are not available
			return;
		}

		final double[] xValues = xValueSerie[0];
		final double lastXValue = xValues[xValues.length - 1];
		final double valueVisibleArea = lastXValue / _graphZoomRatio;

		final double valueLeftBorder = lastXValue * _zoomRatioLeftBorder;
		double valueRightBorder = valueLeftBorder + valueVisibleArea;

		// make sure right is higher than left
		if (valueLeftBorder >= valueRightBorder) {
			valueRightBorder = valueLeftBorder + 1;
		}

		/*
		 * get value index for the left and right border of the visible area
		 */
		int xValueIndexLeft = 0;
		for (int serieIndex = 0; serieIndex < xValues.length; serieIndex++) {

			final double xValue = xValues[serieIndex];

			if (xValue == valueLeftBorder) {
				xValueIndexLeft = serieIndex;
				break;
			}

			if (xValue > valueLeftBorder) {
				xValueIndexLeft = serieIndex == 0 ? //
						0
						// get index from last invisible value
						: serieIndex - 1;
				break;
			}
		}

		int xValueIndexRight = xValueIndexLeft;
		for (; xValueIndexRight < xValues.length; xValueIndexRight++) {
			if (xValues[xValueIndexRight] > valueRightBorder) {
				break;
			}
		}

		/*
		 * get visible min/max value for the x-data serie which fills the visible area in the chart
		 */
		// ensure array bounds
		final int xValuesLastIndex = xValues.length - 1;
		xValueIndexLeft = Math.min(xValueIndexLeft, xValuesLastIndex);
		xValueIndexLeft = Math.max(xValueIndexLeft, 0);
		xValueIndexRight = Math.min(xValueIndexRight, xValuesLastIndex);
		xValueIndexRight = Math.max(xValueIndexRight, 0);

		xData.setVisibleMinValue(xValues[xValueIndexLeft]);
		xData.setVisibleMaxValue(xValues[xValueIndexRight]);

		/*
		 * get min/max value for each y-data serie to fill the visible area with the chart
		 */
		for (final ChartDataYSerie yData : yDataList) {

			final float[][] yValueSeries = yData.getHighValuesFloat();
			final float yValues[] = yValueSeries[0];

			// ensure array bounds
			final int yValuesLastIndex = yValues.length - 1;
			xValueIndexLeft = Math.min(xValueIndexLeft, yValuesLastIndex);
			xValueIndexLeft = Math.max(xValueIndexLeft, 0);
			xValueIndexRight = Math.min(xValueIndexRight, yValuesLastIndex);
			xValueIndexRight = Math.max(xValueIndexRight, 0);

			float dataMinValue = yValues[xValueIndexLeft];
			float dataMaxValue = yValues[xValueIndexLeft];

			for (final float[] yValueSerie : yValueSeries) {

				if (yValueSerie == null) {
					continue;
				}

				for (int valueIndex = xValueIndexLeft; valueIndex <= xValueIndexRight; valueIndex++) {

					final float yValue = yValueSerie[valueIndex];

					if (yValue < dataMinValue) {
						dataMinValue = yValue;
					}
					if (yValue > dataMaxValue) {
						dataMaxValue = yValue;
					}
				}
			}

			if (yData.isForceMinValue()) {

				/*
				 * Prevent that data values which are small than forced min are not painted but
				 * increase the visible min values when the data values are larger than the forced
				 * min value.
				 */

				final double forcedMinValue = yData.getVisibleMinValueForced();

				if (dataMinValue > forcedMinValue) {

					// data min is larger than forced min -> use data min value

					yData.setVisibleMinValue(dataMinValue);

				} else {

					// set forced min value

					yData.setVisibleMinValue(forcedMinValue);
				}

			} else if (dataMinValue != 0) {

				// min is not forced

				yData.setVisibleMinValue(dataMinValue);
			}

			if (yData.isForceMaxValue()) {

				/*
				 * Prevent that data values which are larger than forced max are not painted but
				 * reduce the visible max values when the data values are below the forced max
				 * value.
				 */

				final double forcedMaxValue = yData.getVisibleMaxValueForced();

				if (dataMaxValue < forcedMaxValue) {

					// data max value is below forced max value -> use data max value

					yData.setVisibleMaxValue(dataMaxValue);

				} else {

					// set forced max value

					yData.setVisibleMaxValue(forcedMaxValue);
				}

			} else if (dataMaxValue != 0) {

				// max is not forced

				yData.setVisibleMaxValue(dataMaxValue);
			}
		}
	}

	/**
	 * adjust the y-position for the bottom label when the top label is drawn over it
	 */
	private void updateXSliderYPosition() {

		int labelIndex = 0;
		final ArrayList<ChartXSliderLabel> onTopLabels = _xSliderOnTop.getLabelList();
		final ArrayList<ChartXSliderLabel> onBotLabels = _xSliderOnBottom.getLabelList();

		for (final ChartXSliderLabel onTopLabel : onTopLabels) {

			final ChartXSliderLabel onBotLabel = onBotLabels.get(labelIndex);

			final int onTopWidth2 = onTopLabel.width / 2;
			final int onTopDevX = onTopLabel.x;
			final int onBotWidth2 = onBotLabel.width / 2;
			final int onBotDevX = onBotLabel.x;

			if (onTopDevX + onTopWidth2 > onBotDevX - onBotWidth2 && onTopDevX - onTopWidth2 < onBotDevX + onBotWidth2) {
				onBotLabel.y = onBotLabel.y + onBotLabel.height + 5;
			}
			labelIndex++;
		}
	}

	/**
	 * @param devXMousePosition
	 *            This relative mouse position is used to keep the position when zoomed in, when set
	 *            to {@link Integer#MIN_VALUE} this value is ignored.
	 */
	void zoomInWithMouse(final int devXMousePosition) {

		if (_xxDevGraphWidth <= ChartComponents.CHART_MAX_WIDTH) {

			// chart can be zoomed in

			final int devViewPortWidth = getDevVisibleChartWidth();
			final double devViewPortWidth2 = (double) devViewPortWidth / 2;

			final double newZoomRatio = _graphZoomRatio * ZOOM_RATIO_FACTOR;
			final long xxDevNewGraphWidth = (long) (devViewPortWidth * newZoomRatio);

			if (_xSliderDragged != null) {

				// set zoom center so that the dragged x-slider keeps position when zoomed in

				final double xxDevSlider = _xxDevViewPortLeftBorder + _devXDraggedXSliderLine;
				final double sliderRatio = xxDevSlider / _xxDevGraphWidth;
				final double xxDevNewSlider = sliderRatio * xxDevNewGraphWidth;
				final double xxDevNewZoomRatioCenter = xxDevNewSlider - _devXDraggedXSliderLine + devViewPortWidth2;

				_zoomRatioCenter = xxDevNewZoomRatioCenter / xxDevNewGraphWidth;

			} else if (devXMousePosition != Integer.MIN_VALUE) {

//				if (_zoomRatioCenter == 1.0) {
//
//					// keep ratio, this ratio is used when mouse is in the right axis component and is zooming
//
//				} else {
//				}

				final double xxDevSlider = _xxDevViewPortLeftBorder + devXMousePosition;
				final double sliderRatio = xxDevSlider / _xxDevGraphWidth;
				final double xxDevNewSlider = sliderRatio * xxDevNewGraphWidth;
				final double xxDevNewZoomRatioCenter = xxDevNewSlider - devXMousePosition + devViewPortWidth2;

				_zoomRatioCenter = xxDevNewZoomRatioCenter / xxDevNewGraphWidth;
			}

			if (xxDevNewGraphWidth > ChartComponents.CHART_MAX_WIDTH) {

				// ensure max size
				_graphZoomRatio = (double) ChartComponents.CHART_MAX_WIDTH / devViewPortWidth;
				_xxDevGraphWidth = ChartComponents.CHART_MAX_WIDTH;

			} else {

				_graphZoomRatio = newZoomRatio;
				_xxDevGraphWidth = xxDevNewGraphWidth;
			}

			setZoomRatioLeftBorder();
			handleChartResizeForSliders();

			updateVisibleMinMaxValues();
			moveSlidersToBorder();

			_chartComponents.onResize();
		}

		_chart.enableActions();
	}

	/**
	 * Zoom into the graph with the ratio {@link #ZOOM_RATIO_FACTOR}
	 */
	void zoomInWithoutSlider() {

		final int visibleGraphWidth = getDevVisibleChartWidth();
		final long graphImageWidth = _xxDevGraphWidth;

		final long maxChartWidth = ChartComponents.CHART_MAX_WIDTH;

		if (graphImageWidth <= maxChartWidth) {

			// chart is within the range which can be zoomed in

			if (graphImageWidth * ZOOM_RATIO_FACTOR > maxChartWidth) {

				/*
				 * the double zoomed graph would be wider than the max width, reduce it to the max
				 * width
				 */
				_graphZoomRatio = (double) maxChartWidth / visibleGraphWidth;
				_xxDevGraphWidth = maxChartWidth;

			} else {

				_graphZoomRatio = _graphZoomRatio * ZOOM_RATIO_FACTOR;
				_xxDevGraphWidth = (long) (graphImageWidth * ZOOM_RATIO_FACTOR);
			}

			_chart.enableActions();
		}
	}

	/**
	 * Zoom into the graph to the left and right slider
	 */
	void zoomInWithSlider() {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		/*
		 * offset for the left slider
		 */
		final ChartXSlider leftSlider = getLeftSlider();
		final ChartXSlider rightSlider = getRightSlider();

		// get position of the sliders within the graph
		final long devVirtualLeftSliderPos = leftSlider.getXXDevSliderLinePos();
		final long devVirtualRightSliderPos = rightSlider.getXXDevSliderLinePos();

		// difference between left and right slider
		final double devSliderDiff = devVirtualRightSliderPos - devVirtualLeftSliderPos - 0;

		if (devSliderDiff == 0) {

			// no difference between the slider
			_graphZoomRatio = 1;
			_xxDevGraphWidth = devVisibleChartWidth;

		} else {

			/*
			 * the graph image can't be scrolled, show only the zoomed part which is defined between
			 * the two sliders
			 */

			// calculate new graph ratio
			_graphZoomRatio = (_xxDevGraphWidth) / (devSliderDiff);

			// adjust rounding problems
			_graphZoomRatio = (_graphZoomRatio * devVisibleChartWidth) / devVisibleChartWidth;

			// set the position (ratio) at which the zoomed chart starts
			_zoomRatioLeftBorder = getLeftSlider().getPositionRatio();

			// set the center of the chart for the position when zooming with the mouse
			final double devVirtualWidth = _graphZoomRatio * devVisibleChartWidth;
			final double devXOffset = _zoomRatioLeftBorder * devVirtualWidth;
			final double devCenterPos = devXOffset + devVisibleChartWidth / 2;
			_zoomRatioCenter = devCenterPos / devVirtualWidth;
		}

		handleChartResizeForSliders();

		updateVisibleMinMaxValues();

		_chart.enableActions();
	}

	/**
	 * Zooms out of the graph
	 */
	void zoomOutFitGraph() {

		// reset the data which influence the computed graph image width
		_graphZoomRatio = 1;
		_zoomRatioLeftBorder = 0;

		_xxDevGraphWidth = getDevVisibleChartWidth();
		_xxDevViewPortLeftBorder = 0;

		// reposition the sliders
		final int visibleGraphHeight = getDevVisibleGraphHeight();
		_xSliderA.handleChartResize(visibleGraphHeight);
		_xSliderB.handleChartResize(visibleGraphHeight);

		_chart.enableActions();

		_chartComponents.onResize();
	}

	/**
	 * Zooms out of the graph
	 * 
	 * @param isUpdateChart
	 * @param devXMousePosition
	 *            This relative mouse position is used to keep the position when zoomed in, when set
	 *            to {@link Integer#MIN_VALUE} this value is ignored.
	 */
	void zoomOutWithMouse(final boolean isUpdateChart, final int devXMousePosition) {

		final int devViewPortWidth = getDevVisibleChartWidth();
		final double devViewPortWidth2 = (double) devViewPortWidth / 2;

		if (_graphZoomRatio > ZOOM_RATIO_FACTOR) {

			// graph is zoomed

			final double newZoomRatio = _graphZoomRatio / ZOOM_RATIO_FACTOR;
			final long xxDevNewGraphWidth = (long) (newZoomRatio * devViewPortWidth);

			if (_xSliderDragged != null) {

				/**
				 * Set zoom center so that the dragged x-slider keeps position when zoomed out.
				 * <p>
				 * This formula preserves the slider ratio for a resized graph but is using the
				 * zoomed center to preserve the x-slider position
				 * <p>
				 * very complicated and it took me some time to get this formula
				 */

				// get slider ratio before the graph width is resized
				final double xxDevSlider = _xxDevViewPortLeftBorder + _devXDraggedXSliderLine;
				final double sliderRatio = xxDevSlider / _xxDevGraphWidth;
				final double xxDevNewSlider = sliderRatio * xxDevNewGraphWidth;
				final double xxDevNewZoomRatioCenter = xxDevNewSlider - _devXDraggedXSliderLine + devViewPortWidth2;

				_zoomRatioCenter = xxDevNewZoomRatioCenter / xxDevNewGraphWidth;

			} else if (devXMousePosition != Integer.MIN_VALUE) {

				final double xxDevSlider = _xxDevViewPortLeftBorder + devXMousePosition;
				final double sliderRatio = xxDevSlider / _xxDevGraphWidth;
				final double xxDevNewSlider = sliderRatio * xxDevNewGraphWidth;
				final double xxDevNewZoomRatioCenter = xxDevNewSlider - devXMousePosition + devViewPortWidth2;

				_zoomRatioCenter = xxDevNewZoomRatioCenter / xxDevNewGraphWidth;
			}

			_graphZoomRatio = newZoomRatio;
			_xxDevGraphWidth = xxDevNewGraphWidth;

			setZoomRatioLeftBorder();

			handleChartResizeForSliders();
			updateVisibleMinMaxValues();

			if (isUpdateChart) {
				_chartComponents.onResize();
			}

		} else {

			if (_graphZoomRatio != 1) {

				// show whole graph in the chart

				_graphZoomRatio = 1;
				_xxDevGraphWidth = devViewPortWidth;

				setZoomRatioLeftBorder();

				handleChartResizeForSliders();
				updateVisibleMinMaxValues();

				if (isUpdateChart) {
					_chartComponents.onResize();
				}
			}
		}

		moveSlidersToBorder();

		_chart.enableActions();
	}

}
