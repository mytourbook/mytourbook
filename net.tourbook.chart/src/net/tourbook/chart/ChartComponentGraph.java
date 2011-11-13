/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * Draws the graph and axis into the canvas
 * 
 * @author Wolfgang Schramm
 */
public class ChartComponentGraph extends Canvas {

	private static final double			ZOOM_RATIO_FACTOR		= 1.3;

	private static final int			BAR_MARKER_WIDTH		= 16;

	private static final int[]			DOT_DASHES				= new int[] { 1, 1 };

	private static final NumberFormat	_nf						= NumberFormat.getNumberInstance();

	private static final RGB			_gridRGB				= new RGB(241, 239, 226);
	private static final RGB			_gridRGBMajor			= new RGB(222, 220, 208);

	private static final int[][]		_leftAccelerator		= new int[][] {
			{ -40, -200 },
			{ -30, -50 },
			{ -20, -10 },
			{ -10, -5 },
			{ 0, -2 }											};

//	_devXDraggedXSliderLine < -45 ? -200 : //
//		_devXDraggedXSliderLine < -30 ? -50 : //
//				_devXDraggedXSliderLine < -15 ? -10 : //
//						_devXDraggedXSliderLine < -8 ? -5 : //
//								// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//								// !!! move 2 instead of 1, with 1 it would sometimes not move, needs more investigation
//								// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//								-2;

	private static final int[][]		_rightAccelerator		= new int[][] {
			{ 10, 2 },
			{ 20, 5 },
			{ 30, 10 },
			{ 40, 50 },
			{ Integer.MAX_VALUE, 200 }							};

//	final int devXSliderLineRelative = _devXDraggedXSliderLine - getDevVisibleChartWidth();
//
//	devMouseOffset = //
//	devXSliderLineRelative < 8 ? 2 : //
//			devXSliderLineRelative < 15 ? 5 : //
//					devXSliderLineRelative < 30 ? 10 : //
//							devXSliderLineRelative < 45 ? 50 : //
//									200;

	Chart								_chart;
	private final ChartComponents		_chartComponents;

	/**
	 * This image contains the chart without additional layers.
	 */
	private Image						_chartImage;

	/**
	 * This image contains one single graph without title and x-axis with units.
	 * <p>
	 * This image was created to fix clipping bugs which occured when gradient filling was painted
	 * with a path.
	 */
	private Image						_graphImage;

	/**
	 * Contains layers like the x/y sliders, x-marker, selection or hovered bar.
	 */
	private Image						_sliderImage;

	/**
	 * Contains custom layers like the markers or tour segments which are painted in the foreground.
	 */
	private Image						_customFgLayerImage;

	/**
	 * 
	 */
	private ChartDrawingData			_chartDrawingData;

	/**
	 * drawing data which is used to draw the chart, when this list is empty, an error is displayed
	 */
	private ArrayList<GraphDrawingData>	_graphDrawingData		= new ArrayList<GraphDrawingData>();

	/**
	 * zoom ratio between the visible and the virtual chart width
	 */
	private double						_graphZoomRatio			= 1;

	/**
	 * Contains the width for a zoomed graph this includes also the invisible parts.
	 */
	private int							_xxDevGraphWidth;

	/**
	 * When the graph is zoomed, the chart shows only a part of the whole graph in the viewport.
	 * This value contains the left border of the viewport.
	 */
	private int							_xxDevViewPortOffset;

	/**
	 * ratio for the position where the chart starts on the left side within the virtual graph width
	 */
	private double						_zoomLeftBorderRatio;

	/**
	 * ratio where the mouse was double clicked, this position is used to zoom the chart with the
	 * mouse
	 */
	private double						_zoomCenterRatio;

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
	private boolean						_isCustomFgLayerDirty;

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

//	/**
//	 * Device position when the x-slider was dragged and the chart is zoomed
//	 */
//	private int							_devXDraggedXSliderLineZoomPosition;

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

	private Cursor						_cursorResizeTopDown;
	private Cursor						_cursorDragged;
	private Cursor						_cursorModeSlider;
	private Cursor						_cursorModeZoom;
	private Cursor						_cursorModeZoomMove;
	private Cursor						_cursorMove1x;
	private Cursor						_cursorMove2x;
	private Cursor						_cursorMove3x;
	private Cursor						_cursorMove4x;
	private Cursor						_cursorMove5x;

	private Color						_gridColor;
	private Color						_gridColorMajor;

	/**
	 * serie index for the hovered bar, the bar is hidden when -1;
	 */
	private int							_hoveredBarSerieIndex	= -1;

	private int							_hoveredBarValueIndex;
	private boolean						_isHoveredBarDirty;
	private ChartYSlider				_ySliderDragged;

	private int							_ySliderGraphX;

	private ChartYSlider				_hitYSlider;

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

	private float						_xMarkerValueDiff;

	/**
	 * <code>true</code> when the chart is dragged with the mouse
	 */
	private boolean						_isChartDragged			= false;

	/**
	 * <code>true</code> when the mouse button in down but not moved
	 */
	private boolean						_isChartDraggedStarted	= false;

	private Point						_draggedChartStartPos;

	private Point						_draggedChartDraggedPos;
	private boolean[]					_selectedBarItems;

	private final int[]					_drawCounter			= new int[1];

	private final ColorCache			_colorCache				= new ColorCache();
	int									_graphAlpha				= 0xe0;

	private boolean						_isSelectionVisible;

	/**
	 * Is <code>true</code> when this chart gained the focus, <code>false</code> when the focus is
	 * lost.
	 */
	private boolean						_isFocusActive;

	private boolean						_isSliderImageDirty;

	/*
	 * position of the mouse in the mouse down event
	 */
	private int							_devXMouseDown;
	private int							_devYMouseDown;

	private boolean						_isPaintDraggedImage	= false;

	/**
	 * is <code>true</code> when data for a graph is available
	 */
	private boolean						_isGraphVisible			= false;

	private ToolTipV1					_toolTipV1;

	/**
	 * Client area for this canvas
	 */
	Rectangle							_clientArea;

	private boolean						_isAutoScroll;

	private int[]						_autoScrollCounter		= new int[1];

	/**
	 * is <code>true</code> when the chart is panned
	 */
//	private boolean						fIsMoveMode				= false;

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

		_cursorResizeLeftRight = new Cursor(getDisplay(), SWT.CURSOR_SIZEWE);
		_cursorResizeTopDown = new Cursor(getDisplay(), SWT.CURSOR_SIZENS);
		_cursorDragged = new Cursor(getDisplay(), SWT.CURSOR_SIZEALL);

		_cursorModeSlider = createCursorFromImage(Messages.Image_cursor_mode_slider);
		_cursorModeZoom = createCursorFromImage(Messages.Image_cursor_mode_zoom);
		_cursorModeZoomMove = createCursorFromImage(Messages.Image_cursor_mode_zoom_move);

		_cursorMove1x = createCursorFromImage(Messages.Image_Cursor_Move1x);
		_cursorMove2x = createCursorFromImage(Messages.Image_Cursor_Move2x);
		_cursorMove3x = createCursorFromImage(Messages.Image_Cursor_Move3x);
		_cursorMove4x = createCursorFromImage(Messages.Image_Cursor_Move4x);
		_cursorMove5x = createCursorFromImage(Messages.Image_Cursor_Move5x);

		_gridColor = new Color(getDisplay(), _gridRGB);
		_gridColorMajor = new Color(getDisplay(), _gridRGBMajor);

		_chartComponents = (ChartComponents) parent;

		// setup the x-slider
		_xSliderA = new ChartXSlider(this, Integer.MIN_VALUE, ChartXSlider.SLIDER_TYPE_LEFT);
		_xSliderB = new ChartXSlider(this, Integer.MIN_VALUE, ChartXSlider.SLIDER_TYPE_RIGHT);

		_xSliderOnTop = _xSliderB;
		_xSliderOnBottom = _xSliderA;

		_toolTipV1 = new ToolTipV1(_chart);

		addListener();
		createContextMenu();

		setCursorStyle();
	}

	/**
	 * execute the action which is defined when a bar is selected with the left mouse button
	 */
	private void actionSelectBars() {

		if (_hoveredBarSerieIndex < 0) {
			return;
		}

		boolean[] selectedBarItems;

		if (_graphDrawingData.size() == 0) {
			selectedBarItems = null;
		} else {

			final GraphDrawingData graphDrawingData = _graphDrawingData.get(0);
			final ChartDataXSerie xData = graphDrawingData.getXData();

			selectedBarItems = new boolean[xData._highValues[0].length];
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
			public void paintControl(final PaintEvent event) {

				if (_isChartDragged) {
					draw020DraggedChart(event.gc);
				} else {
					draw000Chart(event.gc);
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
			public void mouseMove(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseMove(e);
				}
			}
		});

		addMouseListener(new MouseListener() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseDoubleClick(e);
				}
			}

			public void mouseDown(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseDown(e);
				}
			}

			public void mouseUp(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseUp(e);
				}
			}
		});

		addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(final MouseEvent e) {
				// forceFocus();
			}

			public void mouseExit(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseExit(e);
				}
			}

			public void mouseHover(final MouseEvent e) {}
		});

		addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(final Event event) {
				onMouseWheel(event);
			}
		});

		addFocusListener(new FocusListener() {

			public void focusGained(final FocusEvent e) {

				setFocusToControl();

				_isFocusActive = true;
				_isSelectionDirty = true;
				redraw();
			}

			public void focusLost(final FocusEvent e) {
				_isFocusActive = false;
				_isSelectionDirty = true;
				redraw();
			}
		});

		addListener(SWT.Traverse, new Listener() {
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
			}
		});

		addListener(SWT.KeyDown, new Listener() {
			public void handleEvent(final Event event) {
				onKeyDown(event);
			}
		});

		addDisposeListener(new DisposeListener() {
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
		final ChartYSlider slider1 = yData.getYSliderTop();
		final ChartYSlider slider2 = yData.getYSliderBottom();

		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - drawingData.devGraphHeight;

		final float graphYBottom = drawingData.getGraphYBottom();
		final float scaleY = drawingData.getScaleY();

		final int devYSliderLine1 = slider1.getDevYSliderLine();
		final int devYSliderLine2 = slider2.getDevYSliderLine();

		final float graphValue1 = ((float) devYBottom - devYSliderLine1) / scaleY + graphYBottom;
		final float graphValue2 = ((float) devYBottom - devYSliderLine2) / scaleY + graphYBottom;

		float minValue;
		float maxValue;

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

		// the cursour could be outside of the chart, reset it
		setCursorStyle();

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
			final Float synchedChartMinValue = synchedChartMinMaxKeeper.getMinValues().get(yDataInfo);

			if (synchedChartMinValue != null) {
				synchedChartMinMaxKeeper.getMinValues().put(yDataInfo, minValue);
			}

			// adjust max value for the changed y-slider
			final Float synchedChartMaxValue = synchedChartMinMaxKeeper.getMaxValues().get(yDataInfo);

			if (synchedChartMaxValue != null) {
				synchedChartMinMaxKeeper.getMaxValues().put(yDataInfo, maxValue);
			}
		}

		computeChart();
	}

	/**
	 * when the chart was modified, recompute all
	 */
	private void computeChart() {
		getDisplay().asyncExec(new Runnable() {
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
		final int xSlider1Position = slider1.getHitRectangle().x;
		final int xSlider2Position = slider2.getHitRectangle().x;

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

	private int computeXMarkerValue(final float[] xValues,
									final int xmStartIndex,
									final float valueDiff,
									final float valueXMarkerPosition) {

		int valueIndex;
		float valueX = xValues[xmStartIndex];
		float valueHalf;

		/*
		 * get the marker positon for the next value
		 */
		if (valueDiff > 0) {

			// moved to the right

			for (valueIndex = xmStartIndex; valueIndex < xValues.length; valueIndex++) {

				valueX = xValues[valueIndex];
				valueHalf = ((valueX - xValues[Math.min(valueIndex + 1, xValues.length - 1)]) / 2);

				if (valueX >= valueXMarkerPosition + valueHalf) {
					break;
				}
			}

		} else {

			// moved to the left

			for (valueIndex = xmStartIndex; valueIndex >= 0; valueIndex--) {

				valueX = xValues[valueIndex];
				valueHalf = ((valueX - xValues[Math.max(0, valueIndex - 1)]) / 2);

				if (valueX < valueXMarkerPosition + valueHalf) {
					break;
				}
			}
		}

		return Math.max(0, Math.min(valueIndex, xValues.length - 1));
	}

	/**
	 * Get the x-axis value according to the slider position in the UI
	 * 
	 * @param slider
	 * @param devXSliderLinePosition
	 */
	void computeXSliderValue(final ChartXSlider slider, final int devXSliderLinePosition) {

		final ChartDataXSerie xData = getXData();

		if (xData == null) {
			return;
		}

		final float[][] xValueSerie = xData.getHighValues();

		if (xValueSerie.length == 0) {
			// data are not available
			return;
		}

		final float[] xDataValues = xValueSerie[0];
		final int serieLength = xDataValues.length;
		final int maxIndex = Math.max(0, serieLength - 1);

		/*
		 * The non time value (distance) is not linear, the value is increasing steadily but with
		 * different distance on the x axis. So first we have to find the nearest position in the
		 * values array and then interpolite from the found position to the slider position
		 */

		final float minValue = xData.getOriginalMinValue();
		final float maxValue = xData.getOriginalMaxValue();
		final float valueRange = maxValue > 0 ? (maxValue - minValue) : -(minValue - maxValue);

		final double positionRatio = (double) devXSliderLinePosition / _xxDevGraphWidth;
		int valueIndex = (int) (positionRatio * serieLength);

		// check array bounds
		valueIndex = Math.min(valueIndex, maxIndex);
		valueIndex = Math.max(valueIndex, 0);

		// sliderIndex points into the value array for the current slider position
		float xDataValue = xDataValues[valueIndex];

		// compute the value for the slider on the x-axis
		final double sliderValue = positionRatio * valueRange;

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

		slider.setValuesIndex(valueIndex);
		slider.setValueX(xDataValue);
	}

	/**
	 * create the context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				actionSelectBars();

				_toolTipV1.toolTip20Hide();

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

		final int devSliderLinePos = xSlider.getXXDevSliderLinePos() - _xxDevViewPortOffset;

		int sliderValuesIndex = xSlider.getValuesIndex();
		// final int valueX = slider.getValueX();

		final ArrayList<ChartXSliderLabel> labelList = new ArrayList<ChartXSliderLabel>();
		xSlider.setLabelList(labelList);

		final ScrollBar hBar = getHorizontalBar();
		final int hBarOffset = hBar.isVisible() ? hBar.getSelection() : 0;

		final int leftPos = hBarOffset;
		final int rightPos = leftPos + getDevVisibleChartWidth();

		// create slider label for each graph
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			final ChartDataYSerie yData = drawingData.getYData();
			final int labelFormat = yData.getSliderLabelFormat();
			final int valueDivisor = yData.getValueDivisor();

			if (labelFormat == ChartDataYSerie.SLIDER_LABEL_FORMAT_MM_SS) {

				// format: mm:ss

			} else {

				// use default format: ChartDataYSerie.SLIDER_LABEL_FORMAT_DEFAULT

				if (valueDivisor == 1) {
					_nf.setMinimumFractionDigits(0);
				} else if (valueDivisor == 10) {
					_nf.setMinimumFractionDigits(1);
				}
			}

			final ChartXSliderLabel label = new ChartXSliderLabel();
			labelList.add(label);

			// draw label on the left or on the right side of the slider,
			// depending on the slider position
			final float[] yValues = yData.getHighValues()[0];

			// make sure the slider value index is not of bounds, this can
			// happen when the data have changed
			sliderValuesIndex = Math.min(sliderValuesIndex, yValues.length - 1);

			final float yValue = yValues[sliderValuesIndex];
			// final int xAxisUnit = xData.getAxisUnit();
			final StringBuilder labelText = new StringBuilder();

			// create the slider text
			if (labelFormat == ChartDataYSerie.SLIDER_LABEL_FORMAT_MM_SS) {

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

			labelText.append(' ');
			labelText.append(yData.getUnitLabel());
			labelText.append(' ');

			// calculate position of the slider label
			final Point labelExtend = gc.stringExtent(labelText.toString());
			final int labelWidth = labelExtend.x + 0;
			int labelXPos = devSliderLinePos - labelWidth / 2;

			final int labelRightPos = labelXPos + labelWidth;

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

			label.text = labelText.toString();

			label.height = labelExtend.y - 5;
			label.width = labelWidth;

			label.x = labelXPos;
			label.y = drawingData.getDevYBottom() - drawingData.devGraphHeight - label.height;

			/*
			 * get the y position of the marker which marks the y value in the graph
			 */
			int yGraph = drawingData.getDevYBottom()
					- (int) ((yValue - drawingData.getGraphYBottom()) * drawingData.getScaleY())
					- 0;

			if (yValue < yData.getVisibleMinValue()) {
				yGraph = drawingData.getDevYBottom();
			}
			if (yValue > yData.getVisibleMaxValue()) {
				yGraph = drawingData.getDevYTop();
			}
			label.yGraph = yGraph;
		}
	}

	private void doAutoScroll() {

		final int AUTO_SCROLL_INTERVAL = 20; // 20ms == 25fps

		_isAutoScroll = true;

		_autoScrollCounter[0]++;

		getDisplay().timerExec(AUTO_SCROLL_INTERVAL, new Runnable() {

			final int	__runnableScrollCounter	= _autoScrollCounter[0];

			public void run() {

				if (__runnableScrollCounter != _autoScrollCounter[0]) {
					return;
				}

				if (isDisposed() || _xSliderDragged == null || _isAutoScroll == false) {
					_isAutoScroll = false;
					return;
				}

				/*
				 * the offset values are determined experimentally and depends on the mouse position
				 */
				int devMouseOffset = 0;
				if (_devXDraggedXSliderLine < 0) {

					// move to the left

					for (final int[] accelerator : _leftAccelerator) {
						if (_devXDraggedXSliderLine < accelerator[0]) {
							devMouseOffset = accelerator[1];
							break;
						}
					}

//					devMouseOffset = //
//							_devXDraggedXSliderLine < -45 ? -200 : //
//								_devXDraggedXSliderLine < -30 ? -50 : //
//									_devXDraggedXSliderLine < -15 ? -10 : //
//										_devXDraggedXSliderLine < -8 ? -5 : //
//											// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//											// !!! move 2 instead of 1, with 1 it would sometimes not move, needs more investigation
//											// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//											-2;
				} else {

					// move to the right

					final int devXSliderLineRelative = _devXDraggedXSliderLine - getDevVisibleChartWidth();

					for (final int[] accelerator : _rightAccelerator) {
						if (devXSliderLineRelative < accelerator[0]) {
							devMouseOffset = accelerator[1];
							break;
						}
					}

//					devMouseOffset = //
//					devXSliderLineRelative < 8 ? 2 : //
//							devXSliderLineRelative < 15 ? 5 : //
//									devXSliderLineRelative < 30 ? 10 : //
//											devXSliderLineRelative < 45 ? 50 : //
//													200;
				}

				doAutoScrollRunnable(this, AUTO_SCROLL_INTERVAL, devMouseOffset);
			}
		});

	}

	private void doAutoScrollRunnable(final Runnable runnable, final int autoScrollInterval, final int devMouseOffset) {

		// get new slider position
		final int devOldVirtualSliderLinePos = _xSliderDragged.getXXDevSliderLinePos();

		final int devNewVirtualSliderLinePos = devOldVirtualSliderLinePos + devMouseOffset;
		final int devNewGraphSliderLinePos = devNewVirtualSliderLinePos - _xxDevViewPortOffset;

		// move the slider
		moveXSlider(_xSliderDragged, devNewGraphSliderLinePos);

		// redraw slider
		_isSliderDirty = true;
		redraw();

		// redraw chart
		setChartPosition(_xSliderDragged, false);

		final boolean isRepeatScrollingLeft = _xxDevViewPortOffset > 1;
		final boolean isRepeatScrollingRight = _xxDevViewPortOffset + devNewGraphSliderLinePos < _xxDevGraphWidth;
		final boolean isRepeatScrolling = isRepeatScrollingLeft || isRepeatScrollingRight;

		// start scrolling again when the bounds have not been reached
		if (isRepeatScrolling) {
			getDisplay().timerExec(autoScrollInterval, runnable);
		} else {
			_isAutoScroll = false;
		}
	}

	private void doAutoZoomToXSliders() {
		if (_canAutoZoomToSlider) {

			// the graph can't be scrolled but the graph should be
			// zoomed to the x-slider positions

			// zoom into the chart
			getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!isDisposed()) {
						_chart.onExecuteZoomInWithSlider();
					}
				}
			});
		}
	}

	/**
	 * Paint event handler
	 * 
	 * <pre>
	 * Top-down sequence how the images are painted
	 * 
	 * {@link #_sliderImage}
	 * {@link #_customFgLayerImage}
	 * {@link #_chartImage}
	 * {@link #_graphImage}
	 * </pre>
	 * 
	 * @param gc
	 */
	private void draw000Chart(final GC gc) {

		if (_graphDrawingData == null || _graphDrawingData.size() == 0) {

			// fill the image area when there is no graphic
			gc.setBackground(_chart.getBackgroundColor());
			gc.fillRectangle(_clientArea);

			draw999ErrorMessage(gc);

			return;
		}

		if (_isChartDirty) {

			draw100ChartImage();

			if (_isPaintDraggedImage) {

				/*
				 * paint dragged chart until the chart is recomputed
				 */
				draw020DraggedChart(gc);
				return;
			}

			// prevent flickering the graph

			/*
			 * mac osx is still flickering, added the drawChartImage in version 1.0
			 */
			if (_chartImage != null) {

				final Image image = draw010ChartImage(gc);
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

			return;
		}

		/*
		 * if the graph was not drawn (because this is done in another thread) there is nothing to
		 * do
		 */
		if (_chartImage == null) {
			// fill the image area when there is no graphic
			gc.setBackground(_chart.getBackgroundColor());
			gc.fillRectangle(_clientArea);
			gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
			return;
		}

		draw300CustomFgLayerImage();
		draw010ChartImage(gc);
	}

	private Image draw010ChartImage(final GC gc) {

		final boolean isSliderImageVisible = _isXSliderVisible
				|| _isYSliderVisible
				|| _isXMarkerMoved
				|| _isSelectionVisible;

		if (isSliderImageVisible) {
			draw400SliderImage();
		}

		final Rectangle chartRect = _chartImage.getBounds();
		final ScrollBar hBar = getHorizontalBar();
		int imageScrollPosition = 0;

		if (chartRect.width < getDevVisibleChartWidth()) {

			// image is smaller than client area, the image is drawn in the top
			// left corner and the free are is painted with background color

			if (_isXSliderVisible && _sliderImage != null) {
				hBar.setVisible(false);
				fillImagePadding(gc, _sliderImage.getBounds());
			} else {
				fillImagePadding(gc, chartRect);
			}
		} else {
			if (hBar.isVisible()) {
				// move the image when the horizontal bar is visible
				imageScrollPosition = -hBar.getSelection();
			}
		}

		if (isSliderImageVisible) {
			if (_sliderImage != null) {
				gc.drawImage(_sliderImage, imageScrollPosition, 0);
			}
			return _sliderImage;

		} else {

			if (_chartImage != null) {
				gc.drawImage(_chartImage, imageScrollPosition, 0);
			}
			return _chartImage;
		}
	}

	private void draw020DraggedChart(final GC gc) {

		if (_draggedChartDraggedPos == null) {
			return;
		}

		final int devXDiff = _draggedChartDraggedPos.x - _draggedChartStartPos.x;
		final int devYDiff = 0;

		gc.setBackground(_chart.getBackgroundColor());

		if (devXDiff > 0) {
			gc.fillRectangle(0, devYDiff, devXDiff, _clientArea.height);
		} else {
			gc.fillRectangle(_clientArea.width + devXDiff, devYDiff, -devXDiff, _clientArea.height);
		}

		if (_customFgLayerImage != null && _customFgLayerImage.isDisposed() == false) {
			gc.drawImage(_customFgLayerImage, devXDiff, devYDiff);
		} else if (_sliderImage != null && _sliderImage.isDisposed() == false) {
			gc.drawImage(_sliderImage, devXDiff, devYDiff);
		} else if (_chartImage != null && _chartImage.isDisposed() == false) {
			gc.drawImage(_chartImage, devXDiff, devYDiff);
		}
	}

	/**
	 * draws the graphs into the chart/graph image
	 */
	private void draw100ChartImage() {

		_drawCounter[0]++;

		final Runnable drawRunnable = new Runnable() {

			final int	__runnableDrawCounter	= _drawCounter[0];

			public void run() {

//				final long startTime = System.currentTimeMillis();
//				// TODO remove SYSTEM.OUT.PRINTLN

				/*
				 * create the chart image only when a new onPaint event has not occured
				 */
				if (__runnableDrawCounter != _drawCounter[0]) {
					// a new onPaint event occured
					return;
				}

				if (isDisposed()) {
					// this widget is disposed
					return;
				}

				if (_graphDrawingData.size() == 0) {
					// drawing data are not set
					return;
				}

				final int devNonScrolledImageWidth = Math.max(
						ChartComponents.CHART_MIN_WIDTH,
						getDevVisibleChartWidth());

				final int devNewImageWidth = devNonScrolledImageWidth;

				/*
				 * the image size is adjusted to the client size but it must be within the min/max
				 * ranges
				 */
				final int devNewImageHeight = Math.max(
						ChartComponents.CHART_MIN_HEIGHT,
						Math.min(getDevVisibleGraphHeight(), ChartComponents.CHART_MAX_HEIGHT));

				/*
				 * when the image is the same size as the new we will redraw it only if it is set to
				 * dirty
				 */
				if (_isChartDirty == false && _chartImage != null) {

					final Rectangle oldBounds = _chartImage.getBounds();

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
				if (Util.canReuseImage(_chartImage, chartImageRect) == false) {
					_chartImage = Util.createImage(getDisplay(), _chartImage, chartImageRect);
				}

				/*
				 * graph image is only the part where ONE single graph is painted without any title
				 * or unit tick/values
				 */
				final int devGraphHeight = _graphDrawingData.get(0).devGraphHeight;
				final Rectangle graphImageRect = new Rectangle(0, 0, //
						devNewImageWidth,
						devGraphHeight < 1 ? 1 : devGraphHeight + 1); // ensure valid height

				if (Util.canReuseImage(_graphImage, graphImageRect) == false) {
					_graphImage = Util.createImage(getDisplay(), _graphImage, graphImageRect);
				}

				// create chart context
				final GC gcChart = new GC(_chartImage);
				final GC gcGraph = new GC(_graphImage);
				{
					gcChart.setFont(_chart.getFont());

					// fill background
					gcChart.setBackground(_chart.getBackgroundColor());
					gcChart.fillRectangle(_chartImage.getBounds());

					if (_chartComponents.errorMessage == null) {

						draw102GraphImage(gcChart, gcGraph);

					} else {

						// an error was set in the chart data model
						draw999ErrorMessage(gcChart);
					}
				}
				gcChart.dispose();
				gcGraph.dispose();

				// remove dirty status
				_isChartDirty = false;

				// dragged image will be painted until the graph image is recomputed
				_isPaintDraggedImage = false;

				// force the layer image to be redrawn
				_isSliderImageDirty = true;

				redraw();

//				final long endTime = System.currentTimeMillis();
//				System.out.println("Execution time : " + (endTime - startTime) + " ms   #:" + _drawCounter[0]);
//				// TODO remove SYSTEM.OUT.PRINTLN
			}
		};

		getDisplay().asyncExec(drawRunnable);
	}

	/**
	 * Draw all graphs, each graph is painted in the same canvas (gcGraph) which is painted in the
	 * the chart image (gcChart).
	 * 
	 * @param gcChart
	 * @param gcGraph
	 */
	private void draw102GraphImage(final GC gcChart, final GC gcGraph) {

		int graphIndex = 0;
		final int lastGraphIndex = _graphDrawingData.size() - 1;

		final Color chartBackgroundColor = _chart.getBackgroundColor();
		final Rectangle graphBounds = _graphImage.getBounds();

		// loop: all graphs in a chart
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			// fill background
			gcGraph.setBackground(chartBackgroundColor);
			gcGraph.fillRectangle(graphBounds);

			final int chartType = drawingData.getChartType();

			if (graphIndex == 0) {
				draw130XTitle(gcChart, drawingData);
			}

			draw120SegmentBg(gcGraph, drawingData);

			if (graphIndex == lastGraphIndex) {
				// draw the unit label and unit tick for the last graph
				draw132XUnitsAndVGrid(gcChart, gcGraph, drawingData, true);
			} else {
				draw132XUnitsAndVGrid(gcChart, gcGraph, drawingData, false);
			}

			// draw only the horizontal grid
			draw140XAsisHGrid(gcGraph, drawingData, false);

			// draw units and grid on the x and y axis
			switch (chartType) {
			case ChartDataModel.CHART_TYPE_LINE:
				draw200LineGraph(gcGraph, drawingData);
				draw204RangeMarker(gcGraph, drawingData);
				break;

			case ChartDataModel.CHART_TYPE_BAR:
				draw210BarGraph(gcGraph, drawingData);
				break;

			case ChartDataModel.CHART_TYPE_LINE_WITH_BARS:
				draw220LineWithBarGraph(gcGraph, drawingData);
				break;

			case ChartDataModel.CHART_TYPE_XY_SCATTER:
				draw230XYScatter(gcGraph, drawingData);
				break;

			default:
				break;
			}

			// draw only the x-axis, this is drawn lately because the graph can overwrite it
			draw140XAsisHGrid(gcGraph, drawingData, true);

			// draw graph image into the chart image
			gcChart.drawImage(_graphImage, 0, drawingData.getDevYTop());

			graphIndex++;
		}
	}

	private void draw120SegmentBg(final GC gc, final GraphDrawingData drawingData) {

		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();

		if (chartSegments == null) {
			return;
		}

		final int devYTop = 0;
		final int devYBottom = drawingData.devGraphHeight;

		final float scaleX = drawingData.getScaleX();

		final int[] startValues = chartSegments.valueStart;
		final int[] endValues = chartSegments.valueEnd;

		if (startValues == null || endValues == null) {
			return;
		}

		final Color alternateColor = new Color(gc.getDevice(), 0xf5, 0xf5, 0xf5); // efefef

		for (int segmentIndex = 0; segmentIndex < startValues.length; segmentIndex++) {

			if (segmentIndex % 2 == 1) {

				// draw segment background color for every second segment

				final int startValue = startValues[segmentIndex];
				final int endValue = endValues[segmentIndex];

				final int devXValueStart = (int) (scaleX * startValue) - _xxDevViewPortOffset;

				// adjust endValue to fill the last part of the segment
				final int devValueEnd = (int) (scaleX * (endValue + 1)) - _xxDevViewPortOffset;

				gc.setBackground(alternateColor);
				gc.fillRectangle(//
						devXValueStart,
						devYTop,
						devValueEnd - devXValueStart,
						devYBottom - devYTop);
			}
		}

		alternateColor.dispose();
	}

	private void draw130XTitle(final GC gc, final GraphDrawingData drawingData) {

		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();
		final int devYTitle = drawingData.getDevMarginTop();

		final int devGraphWidth = _chartComponents.getDevVisibleChartWidth();

		if (chartSegments == null) {

			/*
			 * draw default title, center within the chart
			 */

			final String title = drawingData.getXTitle();

			if (title == null || title.length() == 0) {
				return;
			}

			final int titleWidth = gc.textExtent(title).x;
			final int devXTitle = (devGraphWidth / 2) - (titleWidth / 2);

			gc.drawText(title, //
					devXTitle < 0 ? 0 : devXTitle,
					devYTitle,
					true);

		} else {

			/*
			 * draw title for each segment
			 */

			final float scaleX = drawingData.getScaleX();

			final int[] valueStart = chartSegments.valueStart;
			final int[] valueEnd = chartSegments.valueEnd;
			final String[] segmentTitles = chartSegments.segmentTitle;

			if (valueStart != null && valueEnd != null && segmentTitles != null) {

				int devXChartTitleEnd = -1;

				for (int segmentIndex = 0; segmentIndex < valueStart.length; segmentIndex++) {

					// draw the title in the center of the segment
					final String segmentTitle = segmentTitles[segmentIndex];
					if (segmentTitle != null) {

						final int devXSegmentStart = (int) (scaleX * valueStart[segmentIndex]) - _xxDevViewPortOffset;
						final int devXSegmentEnd = (int) (scaleX * (valueEnd[segmentIndex] + 1)) - _xxDevViewPortOffset;

						final int devXSegmentLength = devXSegmentEnd - devXSegmentStart;
						final int devXSegmentCenter = devXSegmentEnd - (devXSegmentLength / 2);
						final int devXTitleCenter = gc.textExtent(segmentTitle).x / 2;

						final int devX = devXSegmentCenter - devXTitleCenter;

						if (devX <= devXChartTitleEnd) {
							// skip title when it overlaps the previous title
							continue;
						}

						gc.drawText(segmentTitle, devX, devYTitle, false);

						devXChartTitleEnd = devXSegmentCenter + devXTitleCenter + 3;
					}
				}
			}
		}

	}

	/**
	 * Draw the unit label, tick and the vertical grid line for the x axis
	 * 
	 * @param gcChart
	 * @param gcGraph
	 * @param drawingData
	 * @param isDrawUnit
	 *            <code>true</code> indicate to draws the unit tick and unit label additional to the
	 *            unit grid line
	 */
	private void draw132XUnitsAndVGrid(	final GC gcChart,
										final GC gcGraph,
										final GraphDrawingData drawingData,
										final boolean isDrawUnit) {

		final Display display = getDisplay();

		final ArrayList<ChartUnit> xUnits = drawingData.getXUnits();

		final ChartDataXSerie xData = drawingData.getXData();
		final int devYBottom = drawingData.getDevYBottom();
		final int xUnitTextPos = drawingData.getXUnitTextPos();
		float scaleX = drawingData.getScaleX();
		final boolean isXUnitOverlapChecked = drawingData.isXUnitOverlapChecked();
		final boolean isDrawVerticalGrid = _chart.isShowVerticalGridLines;
		final boolean[] isDrawUnits = drawingData.isDrawUnits();

		final double devGraphWidth = drawingData.devVirtualGraphWidth;
		final double scalingFactor = xData.getScalingFactor();
		final double scalingMaxValue = xData.getScalingMaxValue();
		final boolean isExtendedScaling = scalingFactor != 1.0;
		final double extScaleX = ((devGraphWidth - 1) / Math.pow(scalingMaxValue, scalingFactor));

		// check if the x-units has a special scaling
		final float scaleUnitX = drawingData.getScaleUnitX();
		if (scaleUnitX != Float.MIN_VALUE) {
			scaleX = scaleUnitX;
		}

		// get distance between two units
		final float devUnitWidth = xUnits.size() > 1 //
				? ((xUnits.get(1).value * scaleX) - (xUnits.get(0).value * scaleX))
				: 0;

		int unitCounter = 0;
		final int devVisibleChartWidth = getDevVisibleChartWidth();

		boolean isFirstUnit = true;
		int devXLastUnitRightPosition = -1;

		final String unitLabel = drawingData.getXData().getUnitLabel();
		final int devUnitLabelWidth = gcChart.textExtent(unitLabel).x;

		gcChart.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
		gcGraph.setForeground(_gridColor);

		for (final ChartUnit xUnit : xUnits) {

			// get dev x-position for the unit tick
			int devXUnitTick;
			if (isExtendedScaling) {

				// extended scaling
				final double scaledUnitValue = ((Math.pow(xUnit.value, scalingFactor)) * extScaleX);
				devXUnitTick = (int) (scaledUnitValue);

			} else {
				// scale with devXOffset
				devXUnitTick = (int) (xUnit.value * scaleX) - _xxDevViewPortOffset;
			}

			/*
			 * skip units which are outside of the visible area
			 */
			if (devXUnitTick < 0) {
				continue;
			}
			if (devXUnitTick > devVisibleChartWidth) {
				break;
			}

			if (isDrawUnit) {

				boolean isBreakOuterLoop = false;

				while (true) {

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

						final int devXUnitCenter = ((int) devUnitWidth - devUnitValueWidth) / 2;
						int devXUnitLabelPosition = devXUnitTick + devXUnitCenter;

						if (devXUnitLabelPosition < 0) {
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

						if (isFirstUnit) {

							// draw first unit

							isFirstUnit = false;

							/*
							 * this is the first unit, do not center it on the unit tick, because it
							 * would be clipped on the left border
							 */
							int devXUnit = devXUnitValueDefaultPosition;
							if (devXUnit < 0) {
								devXUnit = 0;
							}

							gcChart.drawText(xUnit.valueLabel, devXUnit, devYBottom + 7, true);

							// draw unit label (km, mi, h)

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
								 * check if the unit value would be clipped at the right border,
								 * move it to the left to make it fully visible
								 */
								if ((devXUnitTick + devUnitValueWidth2) > devVisibleChartWidth) {

									devXUnitValueDefaultPosition = devVisibleChartWidth - devUnitValueWidth;

									// check if the unit value is overlapping the previous unit value
									if (devXUnitValueDefaultPosition <= devXLastUnitRightPosition + 2) {
										isBreakOuterLoop = true;
										break;
									}
								}

								if (devXUnitValueDefaultPosition > devXLastUnitRightPosition) {

									gcChart.drawText(
											xUnit.valueLabel,
											devXUnitValueDefaultPosition,
											devYBottom + 7,
											true);

									devXLastUnitRightPosition = devXUnitValueDefaultPosition + devUnitValueWidth + 2;
								}
							}
						}
					}
					break; // while(true)
				}

				if (isBreakOuterLoop) {
					break;
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
				gcGraph.drawLine(devXUnitTick, 0, devXUnitTick, drawingData.devGraphHeight);

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
	private void draw140XAsisHGrid(final GC gcGraph, final GraphDrawingData drawingData, final boolean isDrawOnlyXAsis) {

		final Display display = getDisplay();

		final ArrayList<ChartUnit> yUnits = drawingData.getYUnits();
		final int unitListSize = yUnits.size();

		final float scaleY = drawingData.getScaleY();
		final float graphYBottom = drawingData.getGraphYBottom();
		final int devGraphHeight = drawingData.devGraphHeight;
		final int devVisibleChartWidth = getDevVisibleChartWidth();

		final boolean isBottomUp = drawingData.getYData().isYAxisDirection();
		final boolean isTopDown = isBottomUp == false;

		final int devYTop = 0;
		final int devYBottom = devGraphHeight;

		final boolean isDrawHorizontalGrid = _chart.isShowHorizontalGridLines;

		int devY;
		int unitIndex = 0;

		// loop: all units
		for (final ChartUnit yUnit : yUnits) {

			final float unitValue = yUnit.value;
			final float devYUnit = ((unitValue - graphYBottom) * scaleY) + 0.5f;

			if (isBottomUp || unitListSize == 1) {
				devY = devYBottom - (int) devYUnit;
			} else {
				devY = devYTop + (int) devYUnit;
			}

			// check if a y-unit is on the x axis
			final boolean isXAxis = (isTopDown && unitIndex == unitListSize - 1) || //
					(isBottomUp && unitIndex == 0);

			if (isDrawOnlyXAsis) {

				// draw only the x-axis

				if (isXAxis) {

					gcGraph.setLineStyle(SWT.LINE_SOLID);
					gcGraph.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
					gcGraph.drawLine(0, devY, devVisibleChartWidth, devY);

					// only the x-axis needs to be drawn
					break;
				}

			} else {

				if (isXAxis == false && isDrawHorizontalGrid) {

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
			}

			unitIndex++;
		}
	}

	private void draw200LineGraph(final GC gcGraph, final GraphDrawingData graphDrawingData) {

		final ChartDataXSerie xData = graphDrawingData.getXData();
		final ChartDataYSerie yData = graphDrawingData.getYData();

		final float xValues[] = xData.getHighValues()[0];
		final float scaleX = graphDrawingData.getScaleX();

		final RGB rgbFg = yData.getRgbLine()[0];
		final RGB rgbBgDark = yData.getRgbDark()[0];
		final RGB rgbBgBright = yData.getRgbBright()[0];

		// get the horizontal offset for the graph
		int graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = (int) (Math.max(0, _xxDevViewPortOffset) / scaleX);
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = (int) (_xxDevViewPortOffset / scaleX);
		}

		if (xData.getSynchMarkerStartIndex() == -1) {

			// synch marker is not displayed

			draw202LineGraphSegment(
					gcGraph,
					graphDrawingData,
					0,
					xValues.length,
					rgbFg,
					rgbBgDark,
					rgbBgBright,
					_graphAlpha,
					graphValueOffset);

		} else {

			// draw synched tour

			final int xMarkerAlpha = 0xd0;
			final int noneMarkerAlpha = 0x60;

			// draw the x-marker
			draw202LineGraphSegment(
					gcGraph,
					graphDrawingData,
					xData.getSynchMarkerStartIndex(),
					xData.getSynchMarkerEndIndex() + 1,
					rgbFg,
					rgbBgDark,
					rgbBgBright,
					xMarkerAlpha,
					graphValueOffset);

			// draw segment before the marker
			draw202LineGraphSegment(
					gcGraph,
					graphDrawingData,
					0,
					xData.getSynchMarkerStartIndex() + 1,
					rgbFg,
					rgbBgDark,
					rgbBgBright,
					noneMarkerAlpha,
					graphValueOffset);

			// draw segment after the marker
			draw202LineGraphSegment(
					gcGraph,
					graphDrawingData,
					xData.getSynchMarkerEndIndex() - 0,
					xValues.length,
					rgbFg,
					rgbBgDark,
					rgbBgBright,
					noneMarkerAlpha,
					graphValueOffset);
		}
	}

	/**
	 * first we draw the graph into a path, the path is then drawn on the device with a
	 * transformation
	 * 
	 * @param gcSegment
	 * @param graphDrawingData
	 * @param startIndex
	 * @param endIndex
	 * @param rgbFg
	 * @param rgbBgDark
	 * @param rgbBgBright
	 * @param graphValueOffset
	 */
	private void draw202LineGraphSegment(	final GC gcSegment,
											final GraphDrawingData graphDrawingData,
											final int startIndex,
											final int endIndex,
											final RGB rgbFg,
											final RGB rgbBgDark,
											final RGB rgbBgBright,
											final int alphaValue,
											final float graphValueOffset) {

		final ChartDataXSerie xData = graphDrawingData.getXData();
		final ChartDataYSerie yData = graphDrawingData.getYData();

		final int graphFillMethod = yData.getGraphFillMethod();
		final float graphValueOffsetAdjusted = graphValueOffset;

		final float[][] yHighValues = yData.getHighValues();

		final float xValues[] = xData.getHighValues()[0];
		final float yValues[] = yHighValues[0];

		final boolean[] noLine = xData.getNoLine();

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

//		final int graphDiffNoLineToNext = yData.getDisabledLineToNext();

		// get top/bottom border values of the graph
		final float graphYBorderTop = graphDrawingData.getGraphYTop();
		final float graphYBorderBottom = graphDrawingData.getGraphYBottom();

		final float scaleX = graphDrawingData.getScaleX();
		final float scaleY = graphDrawingData.getScaleY();

		final boolean isShowSkippedValues = _chartDrawingData.chartDataModel.isNoLinesValuesDisplayed();
		final Display display = getDisplay();

		// path is scaled in device pixel
		final Path path = new Path(display);
		final Path path2 = isPath2 ? new Path(display) : null;

		final ArrayList<Point> skippedValues = new ArrayList<Point>();

		final int devCanvasHeight = graphDrawingData.devGraphHeight;
		final float devYGraphTop = scaleY * graphYBorderTop;
		final float devYGraphBottom = scaleY * graphYBorderBottom;// + 0.5f;

		/*
		 * 
		 */
		final float devY0Inverse = devCanvasHeight + devYGraphBottom;

		/*
		 * x-axis line with y==0
		 */
		float graphY_XAxisLine = 0;

		if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
				|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM) {

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
		final float devY_XAxisLine = scaleY * graphY_XAxisLine;

		final float graphXStart = xValues[startIndex] - graphValueOffsetAdjusted;
		final float graphYStart = yValues[startIndex];

//		int graphXPrev = graphXStart;
		float graphY1Prev = graphYStart;

		float devXPrev = graphXStart * scaleX;
		float devY1Prev = graphY1Prev * scaleY;

		final Rectangle chartRectangle = gcSegment.getClipping();
		final int devXWidth = chartRectangle.width;

		boolean isDrawFirstPoint = true;

		final int lastIndex = endIndex - 1;
		float devXPrevNoLine = 0;
		boolean isNoLine = false;

		int valueIndexFirstPoint = startIndex;
		int valueIndexLastPoint = startIndex;

		final int[] devXPositions = new int[endIndex];
		/*
		 * draw the lines into the paths
		 */
		final float devY0 = devY0Inverse - devY_XAxisLine;
		for (int valueIndex = startIndex; valueIndex < endIndex; valueIndex++) {

			// check array bounds
			if (valueIndex >= yValueLength) {
				break;
			}

			final float graphX = xValues[valueIndex] - graphValueOffsetAdjusted;
			final float devX = graphX * scaleX;

			final float graphY1 = yValues[valueIndex];
			final float devY1 = graphY1 * scaleY;

			float graphY2 = 0;
			float devY2 = 0;

			if (isPath2) {
				graphY2 = yValues2[valueIndex];
				devY2 = graphY2 * scaleY;
			}

			devXPositions[valueIndex] = (int) devX;

			// check if position is horizontal visible
			if (devX < 0) {

				// keep current position which is used as the painting starting point

				graphY1Prev = graphY1;

				devXPrev = devX;
				devY1Prev = devY1;

				valueIndexFirstPoint = valueIndex;

				continue;
			}

			/*
			 * draw first point
			 */
			if (isDrawFirstPoint) {

				// move to the first point

				isDrawFirstPoint = false;

				// set the point before devX==0 that the first line is not visible
				final float devXFirstPoint = devXPrev;

				if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
						|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM) {

					// start from the bottom of the graph

					path.moveTo(devXFirstPoint, devCanvasHeight);

				} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

					// start from the x-axis, y=0

					path.moveTo(devXFirstPoint, devY0);
				}

				path.lineTo(devXFirstPoint, devY0Inverse - devY1Prev);

				if (isPath2) {
					path2.moveTo(devXFirstPoint, devY0Inverse - devY2);
					path2.lineTo(devXFirstPoint, devY0Inverse - devY2);
				}
			}

			/*
			 * draw line to current point
			 */

			if ((int) devX != (int) devXPrev || graphY1 == 0 || graphY2 == 0) {

				// optimization: draw only ONE line for the current x-position
				// but draw to the 0 line otherwise it's possible that a triangle is painted

				if (noLine != null && noLine[valueIndex]) {

					/*
					 * draw NO line, but draw a line at the bottom or the x-axis with y=0
					 */

					if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
							|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM) {

						// start from the bottom of the graph

						path.lineTo(devXPrev, devCanvasHeight);
						path.lineTo(devX, devCanvasHeight);

					} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

						// start from the x-axis, y=0

						path.lineTo(devXPrev, devY0);
						path.lineTo(devX, devY0);
					}

					/*
					 * keep positions, because skipped values will be painted as dot outside of the
					 * path, but don't draw on the graph bottom or x-axis
					 */
					if (isShowSkippedValues) {

						final int devY = (int) (devY0Inverse - devY1);
						if (devY != devY0 && graphY1 != 0) {
							skippedValues.add(new Point((int) devX, devY));
						}
					}

					isNoLine = true;
					devXPrevNoLine = devX;

// path2 is not yet supported
//					if (isPath2) {
//						path.lineTo(devXPrev, devY0 - (graphY2 * scaleY));
//					}

				} else {

					// draw line to the current point

					// check if a NO line was painted
					if (isNoLine) {

						isNoLine = false;

						path.lineTo(devXPrevNoLine, devY0Inverse - devY1Prev);
					}

					path.lineTo(devX, devY0Inverse - devY1);

					if (isPath2) {
						path2.lineTo(devX, devY0Inverse - devY2);
					}
				}
			}

			/*
			 * draw last point
			 */
			if (valueIndex == lastIndex || //

					// check if last visible position + 1 is reached
					devX > devXWidth) {

				/*
				 * this is the last point for a filled graph
				 */

				path.lineTo(devX, devY0Inverse - devY1);

				// move path to the final point
				if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
						|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM) {

					// line to the bottom of the graph

					path.lineTo(devX, devCanvasHeight);

				} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

					// line to the x-axis, y=0

					path.lineTo(devX, devY0);
				}

				// moveTo() is necessary that the graph is filled correctly (to prevent a triangle filled shape)
				// finalize previous subpath
				path.moveTo(devX, 0);

				if (isPath2) {
					path2.lineTo(devX, devY0Inverse - devY2);
					path2.moveTo(devX, 0);
				}

				valueIndexLastPoint = valueIndex;

				break;
			}

//			graphXPrev = graphX;
			devXPrev = devX;

			devY1Prev = devY1;
		}

		final Color colorLine = new Color(display, rgbFg);
		final Color colorBgDark = new Color(display, rgbBgDark);
		final Color colorBgBright = new Color(display, rgbBgBright);

		gcSegment.setAntialias(SWT.OFF);
		gcSegment.setAlpha(alphaValue);

		final float graphWidth = xValues[Math.min(xValueLength - 1, endIndex)] - graphValueOffsetAdjusted;

		/*
		 * force a max width because the fill will not be drawn on Linux
		 */
		final int devGraphWidth = Math.min(0x7fff, (int) (graphWidth * scaleX));

		gcSegment.setClipping(path);

		/*
		 * fill the graph
		 */
		if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM) {

			/*
			 * adjust the fill gradient in the height, otherwise the fill is not in the whole
			 * rectangle
			 */

			gcSegment.setForeground(colorBgDark);
			gcSegment.setBackground(colorBgBright);

			gcSegment.fillGradientRectangle(//
					0,
					devCanvasHeight,
					devGraphWidth,
					-devCanvasHeight,
					true);

		} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

			/*
			 * fill above 0 line
			 */

			gcSegment.setForeground(colorBgDark);
			gcSegment.setBackground(colorBgBright);

			gcSegment.fillGradientRectangle(//
					0,
					(int) devY0,
					devGraphWidth,
					-(int) (devYGraphTop - devY_XAxisLine),
					true);

			/*
			 * fill below 0 line
			 */
			gcSegment.setForeground(colorBgBright);
			gcSegment.setBackground(colorBgDark);

			gcSegment.fillGradientRectangle(//
					0,
					devCanvasHeight, // start from the graph bottom
					devGraphWidth,
					-(int) Math.min(devCanvasHeight, devCanvasHeight - devY0Inverse),
					true);

		} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM) {

			final IFillPainter customFillPainter = yData.getCustomFillPainter();

			if (customFillPainter != null) {

				gcSegment.setForeground(colorBgDark);
				gcSegment.setBackground(colorBgBright);

				customFillPainter.draw(
						gcSegment,
						graphDrawingData,
						_chart,
						devXPositions,
						valueIndexFirstPoint,
						valueIndexLastPoint);
			}
		}

		// reset clipping that the line is drawn everywere
		gcSegment.setClipping((Rectangle) null);

		gcSegment.setBackground(colorLine);

		/*
		 * paint skipped values
		 */
		if (isShowSkippedValues && skippedValues.size() > 0) {
			for (final Point skippedPoint : skippedValues) {
				gcSegment.fillRectangle(skippedPoint.x, skippedPoint.y, 2, 2);
			}
		}

		/*
		 * draw line along the path
		 */
		// set line style
		gcSegment.setLineStyle(SWT.LINE_SOLID);
//		gcSegment.setLineWidth(1);

		// draw the line of the graph
		gcSegment.setForeground(colorLine);

//		gcGraph.setAlpha(0x80);
		gcSegment.drawPath(path);

		// dispose resources
		colorLine.dispose();
		colorBgDark.dispose();
		colorBgBright.dispose();

		path.dispose();

		/*
		 * draw path2 above the other graph
		 */
		if (path2 != null) {

			gcSegment.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			gcSegment.drawPath(path2);

			path2.dispose();
		}

		gcSegment.setAlpha(0xFF);
	}

	private void draw204RangeMarker(final GC gc, final GraphDrawingData drawingData) {

		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();

		final int[] startIndex = xData.getRangeMarkerStartIndex();
		final int[] endIndex = xData.getRangeMarkerEndIndex();

		if (startIndex == null) {
			return;
		}

		final float scaleX = drawingData.getScaleX();

		final RGB rgbFg = yData.getRgbLine()[0];
		final RGB rgbBg1 = yData.getRgbDark()[0];
		final RGB rgbBg2 = yData.getRgbBright()[0];

		// get the horizontal offset for the graph
		float graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = Math.max(0, _xxDevViewPortOffset) / scaleX;
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = _xxDevViewPortOffset / scaleX;
		}

		int runningIndex = 0;
		for (final int markerStartIndex : startIndex) {

			// draw range marker
			draw202LineGraphSegment(gc, //
					drawingData,
					markerStartIndex,
					endIndex[runningIndex] + 1,
					rgbFg,
					rgbBg1,
					rgbBg2,
					0x40,
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
	private void draw210BarGraph(final GC gcGraph, final GraphDrawingData drawingData) {

		// get the chart data
		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();
		final int[][] colorsIndex = yData.getColorsIndex();

		gcGraph.setLineStyle(SWT.LINE_SOLID);

		// get the colors
		final RGB[] rgbLine = yData.getRgbLine();
		final RGB[] rgbDark = yData.getRgbDark();
		final RGB[] rgbBright = yData.getRgbBright();

		// get the chart values
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();
		final float graphYBorderBottom = drawingData.getGraphYBottom();
		final boolean axisDirection = yData.isYAxisDirection();

		// get the horizontal offset for the graph
		float graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a synch marker is not set, draw it normally
			graphValueOffset = Math.max(0, _xxDevViewPortOffset) / scaleX;
		} else {
			// adjust the start position to the synch marker position
			graphValueOffset = _xxDevViewPortOffset / scaleX;
		}

		final int devCanvasHeight = drawingData.devGraphHeight;

		/*
		 * Get the top/bottom for the graph, a chart can contain multiple canvas. Canvas is the area
		 * where the graph is painted.
		 */
		final int devYCanvasBottom = devCanvasHeight;
		final int devYCanvasTop = 0;

		final int devYChartBottom = drawingData.getDevYBottom();
		final int devYChartTop = devYChartBottom - devCanvasHeight;

		final float xValues[] = xData.getHighValues()[0];
		final float yHighSeries[][] = yData.getHighValues();
		final float yLowSeries[][] = yData.getLowValues();

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

				float valueYLow;
				if (yLowValues == null) {
					valueYLow = yData.getVisibleMinValue();
				} else {
					// check array bounds
					if (valueIndex >= yLowValues.length) {
						break;
					}
					valueYLow = yLowValues[valueIndex];
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
				if (axisDirection) {

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
	private void draw220LineWithBarGraph(final GC gc, final GraphDrawingData drawingData) {

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
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();
		final float graphYBottom = drawingData.getGraphYBottom();
		final boolean axisDirection = yData.isYAxisDirection();
//		final int barPosition = drawingData.getBarPosition();

		// get the horizontal offset for the graph
		float graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = Math.max(0, _xxDevViewPortOffset) / scaleX;
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = _xxDevViewPortOffset / scaleX;
		}

		// get the top/bottom of the graph
		final int devYTop = 0;
		final int devYBottom = drawingData.devGraphHeight;

		// virtual 0 line for the y-axis of the chart in dev units
//		final float devChartY0Line = (float) devYBottom + (scaleY * graphYBottom);

		gc.setClipping(0, devYTop, gc.getClipping().width, devYBottom - devYTop);

		final float xValues[] = xData.getHighValues()[0];
		final float yHighSeries[][] = yData.getHighValues();
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
	private void draw230XYScatter(final GC gc, final GraphDrawingData drawingData) {

		// get chart data
		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();
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

		final float[][] xSeries = xData.getHighValues();
		final float[][] ySeries = yData.getHighValues();
		final int size = 6;
		final int size2 = size / 2;

		for (int serieIndex = 0; serieIndex < xSeries.length; serieIndex++) {

			final float xValues[] = xSeries[serieIndex];
			final float yHighValues[] = ySeries[serieIndex];

			gc.setBackground(getColor(rgbLine[serieIndex]));

			// loop: all values in the current serie
			for (int valueIndex = 0; valueIndex < xValues.length; valueIndex++) {

				// check array bounds
				if (valueIndex >= yHighValues.length) {
					break;
				}

				final float xValue = xValues[valueIndex];
				final float yValue = yHighValues[valueIndex];

				// get the x/y positions
				int devX;
				if (isExtendedScaling) {
					devX = (int) ((Math.pow(xValue, scalingFactor)) * scaleXExtended);
				} else {
					devX = (int) (xValue * scaleX);
				}

				final int devY = devYBottom - ((int) ((yValue - graphYBottom) * scaleY));

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
	 * Draws custom foreground layers on top of the graphs.
	 */
	private void draw300CustomFgLayerImage() {

		// the layer image has the same size as the graph image
		final Rectangle chartRect = _chartImage.getBounds();

		// ensure correct image size
		if (chartRect.width <= 0 || chartRect.height <= 0) {
			return;
		}

		/*
		 * when the existing image is the same size as the new image, we will redraw it only if it's
		 * set to dirty
		 */
		if (_isCustomFgLayerDirty == false && _customFgLayerImage != null) {

			final Rectangle oldBounds = _customFgLayerImage.getBounds();

			if (oldBounds.width == chartRect.width && oldBounds.height == chartRect.height) {
				return;
			}
		}

		if (Util.canReuseImage(_customFgLayerImage, chartRect) == false) {
			_customFgLayerImage = Util.createImage(getDisplay(), _customFgLayerImage, chartRect);
		}

		final GC gcCustomFgLayer = new GC(_customFgLayerImage);
		{
			gcCustomFgLayer.fillRectangle(chartRect);

			/*
			 * draw the chart image with the graphs into the custom layer image, the custom
			 * foreground layers are drawn on top of the graphs
			 */
			gcCustomFgLayer.drawImage(_chartImage, 0, 0);

			for (final GraphDrawingData graphDrawingData : _graphDrawingData) {

				final ArrayList<IChartLayer> customFgLayers = graphDrawingData.getYData().getCustomForegroundLayers();

				for (final IChartLayer layer : customFgLayers) {
					layer.draw(gcCustomFgLayer, graphDrawingData, _chart);
				}
			}
		}
		gcCustomFgLayer.dispose();

		_isCustomFgLayerDirty = false;
	}

	/**
	 * draws the slider image which contains the custom layer image
	 */
	private void draw400SliderImage() {

		if (_customFgLayerImage == null) {
			return;
		}

		// the slider image is the same size as the graph image
		final Rectangle graphRect = _customFgLayerImage.getBounds();

		// check if the slider image redraw is necessary
		if (_isSliderImageDirty == false
				&& _isSliderDirty == false
				&& _isSelectionDirty == false
				&& _isHoveredBarDirty == false
				&& _sliderImage != null) {

			final Rectangle oldBounds = _sliderImage.getBounds();
			if (oldBounds.width == graphRect.width && oldBounds.height == graphRect.height) {
				return;
			}
		}

		// ensure correct image size
		if (graphRect.width <= 0 || graphRect.height <= 0) {
			return;
		}

		if (Util.canReuseImage(_sliderImage, graphRect) == false) {
			_sliderImage = Util.createImage(getDisplay(), _sliderImage, graphRect);
		}

		if (_sliderImage.isDisposed()) {
			return;
		}

		final GC gcSlider = new GC(_sliderImage);
		{
			// copy the graph image into the slider image, the slider will be drawn
			// on top of the graph
			gcSlider.fillRectangle(graphRect);
			gcSlider.drawImage(_customFgLayerImage, 0, 0);

			/*
			 * draw x/y-sliders
			 */
			if (_isXSliderVisible) {
				createXSliderLabel(gcSlider, _xSliderOnTop);
				createXSliderLabel(gcSlider, _xSliderOnBottom);
				updateXSliderYPosition();

				draw410XSlider(gcSlider, _xSliderOnBottom);
				draw410XSlider(gcSlider, _xSliderOnTop);
			}
			if (_isYSliderVisible) {
				draw420YSliders(gcSlider);
			}
			_isSliderDirty = false;

			if (_isXMarkerMoved) {
				draw430XMarker(gcSlider);
			}

			if (_isSelectionVisible) {
				draw440Selection(gcSlider);
			}

			if (_isHoveredBarDirty) {
				draw450HoveredBar(gcSlider);
				_isHoveredBarDirty = false;
			}
		}
		gcSlider.dispose();

		_isSliderImageDirty = false;
	}

	/**
	 * @param gcGraph
	 * @param slider
	 */
	private void draw410XSlider(final GC gcGraph, final ChartXSlider slider) {

		final Display display = getDisplay();

		final int devSliderLinePos = slider.getXXDevSliderLinePos() - _xxDevViewPortOffset;

		final int grayColorIndex = 60;
		final Color colorTxt = new Color(display, grayColorIndex, grayColorIndex, grayColorIndex);

		int labelIndex = 0;

		final ArrayList<ChartXSliderLabel> labelList = slider.getLabelList();

		// draw slider for each graph
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			final ChartDataYSerie yData = drawingData.getYData();
			final ChartXSliderLabel label = labelList.get(labelIndex);

			final Color colorLine = new Color(display, yData.getRgbLine()[0]);
			final Color colorBright = new Color(display, yData.getRgbBright()[0]);
			final Color colorDark = new Color(display, yData.getRgbDark()[0]);

			final int labelHeight = label.height;
			final int labelWidth = label.width;
			final int labelX = label.x;
			final int labelY = label.y;

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
//			gcGraph.setLineDash(DOT_DASHES);
			gcGraph.setLineDash(new int[] { 4, 1, 4, 1 });
			gcGraph.drawLine(devSliderLinePos, labelY + labelHeight, devSliderLinePos, devYBottom);

			/*
			 * left and right slider have different label backgrounds
			 */
//			if (slider == getLeftSlider()) {
//				// left slider
			gcGraph.setBackground(colorDark);
			gcGraph.setForeground(colorBright);
//			} else {
//				// right slider
//				gc.setBackground(colorBright);
//				gc.setForeground(colorDark);
//			}

			// draw label background
//			gc.fillGradientRectangle(labelX + 1, labelY, labelWidth - 1, labelHeight, false);
//			gc.fillRectangle(labelX + 1, labelY, labelWidth - 1, labelHeight);

			// draw label border
			gcGraph.setForeground(colorLine);
			gcGraph.setLineStyle(SWT.LINE_SOLID);
			gcGraph.drawRoundRectangle(labelX, labelY - 4, labelWidth, labelHeight + 3, 4, 4);

			// draw slider label
			gcGraph.setAlpha(0xff);
			gcGraph.setForeground(colorTxt);
			gcGraph.drawText(label.text, labelX + 2, labelY - 5, true);

			// draw a tiny marker on the graph
			gcGraph.setBackground(colorLine);
			gcGraph.fillRectangle(devSliderLinePos - 3, label.yGraph - 2, 7, 3);

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

			labelIndex++;
		}

		colorTxt.dispose();
	}

	/**
	 * @param gcGraph
	 * @param slider
	 */
	private void draw420YSliders(final GC gcGraph) {

		final Display display = getDisplay();

		final int grayColorIndex = 60;
		final Color colorTxt = new Color(display, grayColorIndex, grayColorIndex, grayColorIndex);

		final int devXChartWidth = getDevVisibleChartWidth();

		for (final ChartYSlider ySlider : _ySliders) {

			if (_hitYSlider == ySlider) {

				final ChartDataYSerie yData = ySlider.getYData();

				final Color colorLine = new Color(display, yData.getRgbLine()[0]);
				final Color colorBright = new Color(display, yData.getRgbBright()[0]);
				final Color colorDark = new Color(display, yData.getRgbDark()[0]);

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

				final float devYValue = (devYBottom - devYSliderLine)
						/ drawingData.getScaleY()
						+ drawingData.getGraphYBottom();

				// create the slider text
				labelText.append(Util.formatValue(devYValue, yData.getAxisUnit(), yData.getValueDivisor(), true));
				labelText.append(' ');
				labelText.append(yData.getUnitLabel());
				labelText.append("  "); //$NON-NLS-1$
				final String label = labelText.toString();

				final Point labelExtend = gcGraph.stringExtent(label);

				final int labelHeight = labelExtend.y - 2;
				final int labelWidth = labelExtend.x + 0;
				final int labelX = _ySliderGraphX - labelWidth - 5;
				final int labelY = devYLabelPos - labelHeight;

				// draw label background
				gcGraph.setForeground(colorBright);
				gcGraph.setBackground(colorDark);
				gcGraph.setAlpha(0xb0);
				gcGraph.fillGradientRectangle(labelX, labelY, labelWidth, labelHeight, true);

				// draw label border
				gcGraph.setAlpha(0xa0);
				gcGraph.setForeground(colorLine);
				gcGraph.drawRectangle(labelX, labelY, labelWidth, labelHeight);
				gcGraph.setAlpha(0xff);

				// draw label text
				gcGraph.setForeground(colorTxt);
				gcGraph.drawText(label, labelX + 2, labelY - 2, true);

				// draw slider line
				gcGraph.setForeground(colorLine);
				gcGraph.setLineDash(DOT_DASHES);
				gcGraph.drawLine(0, devYLabelPos, devXChartWidth, devYLabelPos);

				colorLine.dispose();
				colorBright.dispose();
				colorDark.dispose();
			}
		}

		colorTxt.dispose();
	}

	private void draw430XMarker(final GC gc) {

		final Display display = getDisplay();
		final Color colorXMarker = new Color(display, 255, 153, 0);

		final int devDraggingDiff = _devXMarkerDraggedPos - _devXMarkerDraggedStartPos;

		// draw x-marker for each graph
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			final ChartDataXSerie xData = drawingData.getXData();
			final float scaleX = drawingData.getScaleX();

			final float valueDraggingDiff = devDraggingDiff / scaleX;

			final int synchStartIndex = xData.getSynchMarkerStartIndex();
			final int synchEndIndex = xData.getSynchMarkerEndIndex();

			final float[] xValues = xData.getHighValues()[0];
			final float valueXStart = xValues[synchStartIndex];
			final float valueXEnd = xValues[synchEndIndex];

			final int devXStart = (int) (scaleX * valueXStart - _xxDevViewPortOffset);
			final int devXEnd = (int) (scaleX * valueXEnd - _xxDevViewPortOffset);
			int devMovedXStart = devXStart;
			int devMovedXEnd = devXEnd;

			final float valueXStartWithOffset = valueXStart + valueDraggingDiff;
			final float valueXEndWithOffset = valueXEnd + valueDraggingDiff;

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

			devMovedXStart = (int) (scaleX * xValues[_movedXMarkerStartValueIndex] - _xxDevViewPortOffset);
			devMovedXEnd = (int) (scaleX * xValues[_movedXMarkerEndValueIndex] - _xxDevViewPortOffset);

			/*
			 * when the moved x-marker is on the right or the left border, make sure that the
			 * x-markers don't get too small
			 */
			final float valueMovedDiff = xValues[_movedXMarkerEndValueIndex] - xValues[_movedXMarkerStartValueIndex];

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
				final float valueFirstIndex = xValues[xValues.length - 1] - _xMarkerValueDiff;

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

				final float valueStart = xValues[_movedXMarkerStartValueIndex];
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

			devMovedXStart = (int) (scaleX * xValues[_movedXMarkerStartValueIndex] - _xxDevViewPortOffset);
			devMovedXEnd = (int) (scaleX * xValues[_movedXMarkerEndValueIndex] - _xxDevViewPortOffset);

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

	private void draw440Selection(final GC gc) {

		_isSelectionDirty = false;

		final int chartType = _chart.getChartDataModel().getChartType();

		// loop: all graphs
		for (final GraphDrawingData drawingData : _graphDrawingData) {
			switch (chartType) {
			case ChartDataModel.CHART_TYPE_LINE:
				// drawLineSelection(gc, drawingData);
				break;

			case ChartDataModel.CHART_TYPE_BAR:
				draw442BarSelection(gc, drawingData);
				break;

			default:
				break;
			}
		}
	}

	private void draw442BarSelection(final GC gc, final GraphDrawingData drawingData) {

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
//				gc.setAlpha(0xb0);
				gc.setAlpha(0xf0);
			} else {
//				gc.setAlpha(0x70);
				gc.setAlpha(0xa0);
			}

			// fill bar background
			gc.setForeground(colorDarkSelected);
			gc.setBackground(colorBrightSelected);

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

	private void draw450HoveredBar(final GC gc) {

		// check if hovered bar is disabled
		if (_hoveredBarSerieIndex == -1) {
			return;
		}

		// draw only bar chars
		if (_chart.getChartDataModel().getChartType() != ChartDataModel.CHART_TYPE_BAR) {
			return;
		}

		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setAlpha(0xd0);

		// loop: all graphs
		for (final GraphDrawingData drawingData : _graphDrawingData) {

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
				gc.setForeground(colorDark);
				gc.setBackground(colorBright);

				gc.fillGradientRectangle(
						hoveredBarShape.x + 1,
						hoveredBarShape.y + 1,
						hoveredBarShape.width - 1,
						hoveredBarShape.height - 1,
						true);

				// draw bar border
				gc.setForeground(colorLine);
				gc.drawRoundRectangle(
						hoveredBarShape.x,
						hoveredBarShape.y,
						hoveredBarShape.width,
						hoveredBarShape.height,
						4,
						4);
			}
		}

		gc.setAlpha(0xff);
	}

//	void enforceChartImageMinMaxWidth() {
//
////		if (graphZoomParts != 1) {
////
//////			zoomWithParts(graphZoomParts, graphZoomPartPosition, true);
////
////		} else {
//
//		final int devVisibleChartWidth = getDevVisibleChartWidth();
//
//		final int devImageWidth = (int) (fGraphZoomRatio * devVisibleChartWidth);
//		final int chartMinWidth = fChart.getChartDataModel().getChartMinWidth();
//
//		if (canScrollZoomedChart) {
//
//			// enforce min/max width for the chart
//			final int devMinWidth = Math.max(Math.max(devVisibleChartWidth, chartMinWidth),
//					ChartComponents.CHART_MIN_WIDTH);
//
//			final int devMaxWidth = Math.min(devImageWidth, ChartComponents.CHART_MAX_WIDTH);
//
//			fDevVirtualGraphImageWidth = Math.max(devMinWidth, devMaxWidth);
//
//		} else {
//
//			// enforce min width for the chart
//			final int devMinWidth = Math.max(Math.max(devVisibleChartWidth, chartMinWidth),
//					ChartComponents.CHART_MIN_WIDTH);
//
//			fDevVirtualGraphImageWidth = Math.max(devMinWidth, devImageWidth);
//		}
////		}
//	}

	private void draw999ErrorMessage(final GC gc) {

		final String errorMessage = _chartComponents.errorMessage;
		if (errorMessage != null) {
			gc.drawText(errorMessage, 0, 10);
		}
	}

	/**
	 * Fills the surrounding area of an rectangle with background color
	 * 
	 * @param gc
	 * @param imageRect
	 */
	private void fillImagePadding(final GC gc, final Rectangle imageRect) {

		final int clientHeight = getDevVisibleGraphHeight();
		final int visibleGraphWidth = getDevVisibleChartWidth();

		gc.setBackground(_chart.getBackgroundColor());

		gc.fillRectangle(imageRect.width, 0, visibleGraphWidth, clientHeight);
		gc.fillRectangle(0, imageRect.height, visibleGraphWidth, clientHeight);
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
			return _colorCache.createColor(colorKey, rgb);
		} else {
			return color;
		}
	}

	/**
	 * @return Returns the viewport (visible width) of the chart graph
	 */
	private int getDevVisibleChartWidth() {
		return _chartComponents.getDevVisibleChartWidth();
	}

	/**
	 * @return Returns the visible height of the chart graph
	 */
	private int getDevVisibleGraphHeight() {
		return _chartComponents.getDevVisibleChartHeight();
	}

	public double getGraphZoomRatio() {
		return _graphZoomRatio;
	}

	/**
	 * @return Returns the left slider
	 */
	ChartXSlider getLeftSlider() {
		return _xSliderA.getXXDevSliderLinePos() < _xSliderB.getXXDevSliderLinePos() ? _xSliderA : _xSliderB;
	}

	/**
	 * @return Returns the right most slider
	 */
	ChartXSlider getRightSlider() {
		return _xSliderA.getXXDevSliderLinePos() < _xSliderB.getXXDevSliderLinePos() ? _xSliderB : _xSliderA;
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
		if (_graphDrawingData.size() == 0) {
			return null;
		} else {
			return _graphDrawingData.get(0).getXData();
		}
	}

	private GraphDrawingData getXDrawingData() {
		return _graphDrawingData.get(0);
	}

	/**
	 * @return Returns the virtual graph image width, this is the width of the graph image when the
	 *         full graph would be displayed
	 */
	int getXXDevGraphWidth() {
		return _xxDevGraphWidth;
	}

	/**
	 * @return When the graph is zoomed, the chart shows only a part of the whole graph in the
	 *         viewport. Returns the left border of the viewport.
	 */
	int getXXDevViewPortOffset() {
		return _xxDevViewPortOffset;
	}

	private void handleChartResizeForSliders() {

		// update the width in the sliders
		final int visibleGraphHeight = getDevVisibleGraphHeight();

		getLeftSlider().handleChartResize(visibleGraphHeight);
		getRightSlider().handleChartResize(visibleGraphHeight);
	}

	/**
	 * check if mouse has moved over a bar
	 * 
	 * @param devY
	 * @param graphX
	 */
	private boolean isBarHit(final int devY, final int graphX) {

		boolean isBarHit = false;

		// loop: all graphs
		for (final GraphDrawingData drawingData : _graphDrawingData) {

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
					if (barInfoFocus != null && barInfoFocus.contains(graphX, devY)) {

						// keep the hovered bar index
						_hoveredBarSerieIndex = serieIndex;
						_hoveredBarValueIndex = valueIndex;

						_toolTipV1.toolTip10Show(graphX, 100, serieIndex, valueIndex);

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

			_toolTipV1.toolTip20Hide();

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

		final float[] xValues = xData.getHighValues()[0];
		final float scaleX = getXDrawingData().getScaleX();

		final int devXMarkerStart = (int) (xValues[Math.min(synchMarkerStartIndex, xValues.length - 1)] * scaleX - _xxDevViewPortOffset);
		final int devXMarkerEnd = (int) (xValues[Math.min(synchMarkerEndIndex, xValues.length - 1)] * scaleX - _xxDevViewPortOffset);

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

		for (final ChartYSlider ySlider : _ySliders) {
			if (ySlider.getHitRectangle().contains(graphX, devY)) {
				_hitYSlider = ySlider;
				return ySlider;
			}
		}

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
		final int xxDevLeftPosition = _xxDevViewPortOffset + _devXMouseDown;

		computeXSliderValue(leftSlider, xxDevLeftPosition);

		leftSlider.moveToXXDevPosition(xxDevLeftPosition, true, true);

		setZoomInPosition();

		_isSliderDirty = true;
		redraw();
	}

	/**
	 * Move right slider to the mouse down position
	 */
	void moveRightSliderHere() {

		final ChartXSlider rightSlider = getRightSlider();
		final int xxDevRightPosition = _xxDevViewPortOffset + _devXMouseDown;

		computeXSliderValue(rightSlider, xxDevRightPosition);

		rightSlider.moveToXXDevPosition(xxDevRightPosition, true, true);

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
		final int xxDevLeftPosition = _xxDevViewPortOffset + 2;

		computeXSliderValue(leftSlider, xxDevLeftPosition);
		leftSlider.moveToXXDevPosition(xxDevLeftPosition, true, true);

		/*
		 * adjust right slider
		 */
		final int xxDevRightPosition = _xxDevViewPortOffset + getDevVisibleChartWidth() - 2;

		computeXSliderValue(rightSlider, xxDevRightPosition);
		rightSlider.moveToXXDevPosition(xxDevRightPosition, true, true);

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
	private void moveXSlider(final ChartXSlider xSlider, final int devXSliderLinePos) {

		int xxDevSliderLinePos = _xxDevViewPortOffset + devXSliderLinePos;

		/*
		 * adjust the line position the the min/max width of the graph image
		 */
		xxDevSliderLinePos = Math.min(_xxDevGraphWidth, Math.max(0, xxDevSliderLinePos));

		computeXSliderValue(xSlider, xxDevSliderLinePos);

		// set new slider line position
		xSlider.moveToXXDevPosition(xxDevSliderLinePos, true, true);
	}

	private void moveYSlider(final ChartYSlider ySlider, final int devXGraph, final int devYMouse) {

		final int devYSliderLine = devYMouse - ySlider.getDevYClickOffset() + ChartYSlider.halfSliderHitLineHeight;

		ySlider.setDevYSliderLine(devXGraph, devYSliderLine);
	}

	/**
	 * Dispose event handler
	 */
	private void onDispose() {

		// dispose resources
		_cursorResizeLeftRight = Util.disposeResource(_cursorResizeLeftRight);
		_cursorResizeTopDown = Util.disposeResource(_cursorResizeTopDown);
		_cursorDragged = Util.disposeResource(_cursorDragged);
		_cursorModeSlider = Util.disposeResource(_cursorModeSlider);
		_cursorModeZoom = Util.disposeResource(_cursorModeZoom);
		_cursorModeZoomMove = Util.disposeResource(_cursorModeZoomMove);

		_cursorMove1x = Util.disposeResource(_cursorMove1x);
		_cursorMove2x = Util.disposeResource(_cursorMove2x);
		_cursorMove3x = Util.disposeResource(_cursorMove3x);
		_cursorMove4x = Util.disposeResource(_cursorMove4x);
		_cursorMove5x = Util.disposeResource(_cursorMove5x);

		_chartImage = Util.disposeResource(_chartImage);
		_graphImage = Util.disposeResource(_graphImage);
		_sliderImage = Util.disposeResource(_sliderImage);
		_customFgLayerImage = Util.disposeResource(_customFgLayerImage);

		_gridColor = Util.disposeResource(_gridColor);
		_gridColorMajor = Util.disposeResource(_gridColorMajor);

		_toolTipV1.dispose();

		_colorCache.dispose();
	}

	private void onKeyDown(final Event event) {

		switch (_chart.getChartDataModel().getChartType()) {
		case ChartDataModel.CHART_TYPE_BAR:
			_chartComponents.selectBarItem(event);
			break;

		case ChartDataModel.CHART_TYPE_LINE:

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

			break;

		default:
			break;
		}
	}

	/**
	 * move the x-slider with the keyboard
	 * 
	 * @param event
	 */
	private void onKeyDownMoveXSlider(final Event event) {

		/*
		 * keyboard events behaves different than the mouse event, shift & ctrl can be set in both
		 * event fields
		 */
		boolean isShift = (event.stateMask & SWT.SHIFT) != 0 || (event.keyCode & SWT.SHIFT) != 0;
		boolean isCtrl = (event.stateMask & SWT.CTRL) != 0 || (event.keyCode & SWT.CTRL) != 0;

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
//		if (isShift && (event.stateMask & SWT.CTRL) == 0) {
			_selectedXSlider = _selectedXSlider == _xSliderA ? _xSliderB : _xSliderA;
			_isSliderDirty = true;
			redraw();

			return;
		}

		// accelerate with page up/down
		if (event.keyCode == SWT.PAGE_UP || event.keyCode == SWT.PAGE_DOWN) {
			isCtrl = true;
			isShift = true;
		}

		// accelerate slider move speed depending on shift/ctrl key
		int valueIndexDiff = isCtrl ? 10 : 1;
		valueIndexDiff *= isShift ? 10 : 1;

		int valueIndex = _selectedXSlider.getValuesIndex();
		final float[] xValues = getXData().getHighValues()[0];

		boolean isMoveSlider = false;

		if (isShift && isCtrl) {

			/*
			 * this will reposition the x-slider to the exact value position in the graph, the Ctrl
			 * key must be pressed first before the Shift key otherwise the slider is toggles
			 */
			isMoveSlider = true;

		} else {

			switch (event.keyCode) {
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
			setCursorStyle();
		}
	}

	void onMouseDoubleClick(final MouseEvent e) {

		// stop dragging the x-slider
		_xSliderDragged = null;

		if (_hoveredBarSerieIndex != -1) {

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
				final double xxDevMousePosInChart = _xxDevViewPortOffset + e.x;
				_zoomCenterRatio = xxDevMousePosInChart / _xxDevGraphWidth;

				zoomInWithMouse();
			}
		}
	}

	/**
	 * Mouse down event handler
	 * 
	 * @param event
	 */
	private void onMouseDown(final MouseEvent event) {

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

			if (event.button == 3) {

				// right button is pressed

				computeSliderForContextMenu(devXMouse, devYMouse);
			}
			return;
		}

		// use external mouse event listener
		if (_chart.isMouseDownExternalPre(devXMouse, devYMouse)) {
			return;
		}

		if (_xSliderDragged != null) {

			// keep x-slider
			final ChartXSlider xSlider = _xSliderDragged;

			// stop dragging the slider
			_xSliderDragged = null;

			// set mouse zoom double click position
			final double xxDevMousePosInChart = _xxDevViewPortOffset + devXMouse;
			_zoomCenterRatio = xxDevMousePosInChart / _xxDevGraphWidth;

			/*
			 * make sure that the slider is exactly positioned where the value is displayed in the
			 * graph
			 */
			setXSliderValueIndex(xSlider, xSlider.getValuesIndex(), false);

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

				// a x-slider is hit and can now be dragged with a mouse move event

				_xSliderOnTop = _xSliderDragged;
				_xSliderOnBottom = _xSliderOnTop == _xSliderA ? _xSliderB : _xSliderA;

				// set the hit offset for the mouse click
				_xSliderDragged.setDevXClickOffset(devXMouse - _xxDevViewPortOffset); //_xSliderDragged.getHitRectangle().x);

				// the hit x-slider is now the selected x-slider
				_selectedXSlider = _xSliderDragged;
				_isSelectionVisible = true;
				_isSliderDirty = true;

				redraw();

			} else {

				// a x-slider isn't dragged

				// check if a y-slider was hit
				_ySliderDragged = isYSliderHit(devXMouse, devYMouse);

				if (_ySliderDragged != null) {

					// y-slider was hit

					_ySliderDragged.setDevYClickOffset(devYMouse - _ySliderDragged.getHitRectangle().y);

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

				} else if (_isXSliderVisible && _xSliderDragged == null && (isShift || isCtrl)) {

					// start dragging the x-slider

					if (isCtrl) {
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

					_xSliderDragged.setDevXClickOffset(devXMouse - _xxDevViewPortOffset); //_xSliderDragged.getHitRectangle().x);

					// keep position of the slider line
					final int devXSliderLinePos = devXMouse;
					_devXDraggedXSliderLine = devXSliderLinePos;

					moveXSlider(_xSliderDragged, devXSliderLinePos);

					_isSliderDirty = true;
					redraw();

				} else {

					// do post processing when no other ations are done

					if (_graphZoomRatio > 1) {

						// start moving the chart

						/*
						 * to prevent flickering with the double click event, dragged started is
						 * used
						 */
						_isChartDraggedStarted = true;

						_draggedChartStartPos = new Point(event.x, event.y);

						/*
						 * set also the move position because when changing the data model, the old
						 * position will be used and the chart is painted on the wrong position on
						 * mouse down
						 */
						_draggedChartDraggedPos = _draggedChartStartPos;

						setCursor(_cursorDragged);
					}
				}
			}
		}

		setCursorStyle();
	}

	/**
	 * Mouse down event in the x-axis area
	 * 
	 * @param event
	 */
	void onMouseDownAxis(final MouseEvent event) {

		if (_xSliderDragged != null) {

			// stop dragging the slider
			_xSliderDragged = null;

			doAutoZoomToXSliders();
		}

	}

	/**
	 * Mouse exit event handler
	 * 
	 * @param event
	 */
	private void onMouseExit(final MouseEvent event) {

		_toolTipV1.toolTip20Hide();

		if (_isAutoScroll) {
			// stop autoscrolling
			_isAutoScroll = false;

		} else if (_xSliderDragged == null) {

			// hide the y-slider labels
			if (_hitYSlider != null) {
				_hitYSlider = null;

				_isSliderDirty = true;
				redraw();
			}
		}

		if (_mouseOverXSlider != null) {
			// mouse left the x-slider
			_mouseOverXSlider = null;
			_isSliderDirty = true;
			redraw();
		}

		setCursorStyle();
	}

	/**
	 * @param mouseEvent
	 * @return Returns <code>true</code> when the mouse event was handled.
	 */
	boolean onMouseExitAxis(final MouseEvent mouseEvent) {

		if (_isAutoScroll) {

			// stop autoscrolling
			_isAutoScroll = false;

			// hide move cursor
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
	 * @param event
	 */
	private void onMouseMove(final MouseEvent event) {

		final int devXMouse = event.x;
		final int devYMouse = event.y;

		boolean isChartDirty = false;

		if (_isXSliderVisible && _xSliderDragged != null) {

			// x-slider is dragged

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
				isChartDirty = true;

			} else {

				/*
				 * slider is outside the visible area, auto scroll the slider and graph when this is
				 * not yet done
				 */
				if (_isAutoScroll == false) {
					doAutoScroll();
				}
			}

		} else if (_isChartDraggedStarted || _isChartDragged) {

			// chart is dragged with the mouse

			_isChartDraggedStarted = false;
			_isChartDragged = true;

			_draggedChartDraggedPos = new Point(event.x, event.y);

			isChartDirty = true;

		} else if (_isYSliderVisible && _ySliderDragged != null) {

			// y-slider is dragged

			moveYSlider(_ySliderDragged, devXMouse, devYMouse);
			_ySliderGraphX = devXMouse;

			_isSliderDirty = true;
			isChartDirty = true;

		} else if (_isXMarkerMoved) {

			_devXMarkerDraggedPos = devXMouse;

			_isSliderDirty = true;
			isChartDirty = true;

		} else {

			ChartXSlider xSlider;

			if (_chart.isMouseMoveExternal(devXMouse, devYMouse)) {

				// set the cursor shape depending on the mouse location
				setCursor(_cursorDragged);

			} else if (_isXSliderVisible && (xSlider = isXSliderHit(devXMouse, devYMouse)) != null) {

				// mouse is over an x-slider

				if (_mouseOverXSlider != xSlider) {

					// a new x-slider is hovered

					_mouseOverXSlider = xSlider;

					// hide the y-slider
					_hitYSlider = null;

					_isSliderDirty = true;
					isChartDirty = true;
				}

				// set cursor
				setCursor(_cursorResizeLeftRight);

			} else if (_mouseOverXSlider != null) {

				// mouse left the x-slider

				_mouseOverXSlider = null;
				_isSliderDirty = true;
				isChartDirty = true;

			} else if (_isYSliderVisible && isYSliderHit(devXMouse, devYMouse) != null) {

				// cursor is within a y-slider

				setCursor(_cursorResizeTopDown);

				// show the y-slider labels
				_ySliderGraphX = devXMouse;

				_isSliderDirty = true;
				isChartDirty = true;

			} else if (_chart._draggingListenerXMarker != null && isSynchMarkerHit(devXMouse)) {

				setCursor(_cursorDragged);

			} else if (isBarHit(devYMouse, devXMouse)) {

				_isHoveredBarDirty = true;
				isChartDirty = true;

				setCursorStyle();

			} else {

				setCursorStyle();
			}
		}

		if (isChartDirty) {
			redraw();
		}
	}

	/**
	 * @param mouseEvent
	 * @return Returns <code>true</code> when the mouse event was handled.
	 */
	boolean onMouseMoveAxis(final MouseEvent mouseEvent) {

		if (_isXSliderVisible && _xSliderDragged != null) {

			// x-slider is dragged

			if (mouseEvent.widget instanceof ChartComponentAxis) {

				final ChartComponentAxis axisComponent = (ChartComponentAxis) mouseEvent.widget;
				final Rectangle clientArea = axisComponent.getAxisClientArea();

				if (clientArea != null) {

					// set dragged x-slider position and do autoscrolling

					final int devXMouse = mouseEvent.x;
					final Cursor cursor;

					if (axisComponent == _chartComponents.getAxisLeft()) {

						// left x-axis

						_devXDraggedXSliderLine = -clientArea.width + devXMouse;

						cursor = //
						_devXDraggedXSliderLine < _leftAccelerator[0][0] ? _cursorMove5x : //
								_devXDraggedXSliderLine < _leftAccelerator[1][0] ? _cursorMove4x : //
										_devXDraggedXSliderLine < _leftAccelerator[2][0] ? _cursorMove3x : //
												_devXDraggedXSliderLine < _leftAccelerator[3][0] ? _cursorMove2x : //
														_cursorMove1x;

//						devMouseOffset = //
//						_devXDraggedXSliderLine < -45 ? -200 : //
//							_devXDraggedXSliderLine < -30 ? -50 : //
//								_devXDraggedXSliderLine < -15 ? -10 : //
//									_devXDraggedXSliderLine < -8 ? -5 : //
//										// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//										// !!! move 2 instead of 1, with 1 it would sometimes not move, needs more investigation
//										// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//										-2;
					} else {

						// right x-axis

						_devXDraggedXSliderLine = getDevVisibleChartWidth() + devXMouse;

						cursor = //
						devXMouse < _rightAccelerator[0][0] ? _cursorMove1x : //
								devXMouse < _rightAccelerator[1][0] ? _cursorMove2x : //
										devXMouse < _rightAccelerator[2][0] ? _cursorMove3x : //
												devXMouse < _rightAccelerator[3][0] ? _cursorMove4x : //
														_cursorMove5x;

//						final int devXSliderLineRelative = _devXDraggedXSliderLine - getDevVisibleChartWidth();
//
//						for (final int[] accelerator : _rightAccelerator) {
//							if (devXSliderLineRelative < accelerator[0]) {
//								devMouseOffset = accelerator[1];
//								break;
//							}
//						}

//						devMouseOffset = //
//						devXSliderLineRelative < 8 ? 2 : //
//								devXSliderLineRelative < 15 ? 5 : //
//										devXSliderLineRelative < 30 ? 10 : //
//												devXSliderLineRelative < 45 ? 50 : //
//														200;

					}

					axisComponent.setCursor(cursor);

					if (_isAutoScroll == false) {
						doAutoScroll();
					}

					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Mouse up event handler
	 * 
	 * @param event
	 */
	private void onMouseUp(final MouseEvent event) {

		final int devXMouse = event.x;
		final int devYMouse = event.y;

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

		} else {

			if (_chart.isMouseUpExternal(devXMouse, devYMouse)) {
				return;
			}

//			if (_xSliderDragged != null) {
//
//				// stop dragging the slider
//				_xSliderDragged = null;
//
//				doAutoZoomToXSliders();
//
//			} else

			if (_ySliderDragged != null) {

				adjustYSlider();

			} else if (_isXMarkerMoved) {

				_isXMarkerMoved = false;

				_isSliderDirty = true;
				redraw();

				// call the listener which is registered for dragged x-marker
				if (_chart._draggingListenerXMarker != null) {
					_chart._draggingListenerXMarker.xMarkerMoved(
							_movedXMarkerStartValueIndex,
							_movedXMarkerEndValueIndex);
				}

			} else if (_isChartDragged || _isChartDraggedStarted) {

				// chart was moved with the mouse

				_isChartDragged = false;
				_isChartDraggedStarted = false;

				setCursorStyle();

				updateDraggedChart(_draggedChartDraggedPos.x - _draggedChartStartPos.x);
			}
		}
	}

	void onMouseWheel(final Event event) {

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
				event.keyCode = SWT.ARROW_RIGHT;
			} else {
				event.keyCode = SWT.ARROW_LEFT;
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
					public void run() {

						zoomInWithSlider();
						_chartComponents.onResize();
						if (event.count < 0) {}
					}
				});
			}

		} else {

			// mouse mode: zoom chart

			final boolean isCtrl = (event.stateMask & SWT.CONTROL) != 0;
			final boolean isShift = (event.stateMask & SWT.SHIFT) != 0;

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

				if (event.count < 0) {
					zoomOutWithMouse(true);
				} else {
					zoomInWithMouse();
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

	/**
	 * Move a zoomed chart so that the slider is visible.
	 * 
	 * @param slider
	 * @param isCenterSliderPosition
	 */
	private void setChartPosition(final ChartXSlider slider, final boolean isCenterSliderPosition) {

		if (_graphZoomRatio == 1) {
			// nothing to do
			return;
		}

		final int xxDevSliderLinePos = slider.getXXDevSliderLinePos();

		final int devXViewPortWidth = getDevVisibleChartWidth();
		double xxDevOffset = xxDevSliderLinePos;

		if (isCenterSliderPosition) {

			xxDevOffset = xxDevSliderLinePos - devXViewPortWidth / 2;

		} else {

			/*
			 * check if the slider is in the visible area
			 */
			if (xxDevSliderLinePos < _xxDevViewPortOffset) {

				xxDevOffset = xxDevSliderLinePos + 1;

			} else if (xxDevSliderLinePos > _xxDevViewPortOffset + devXViewPortWidth) {

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

			_zoomLeftBorderRatio = xxDevOffset / _xxDevGraphWidth;

			/*
			 * reposition the mouse zoom position
			 */
			final float xOffsetMouse = _xxDevViewPortOffset + devXViewPortWidth / 2;
			_zoomCenterRatio = xOffsetMouse / _xxDevGraphWidth;

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
		_zoomCenterRatio = (double) xxDevSliderLinePos / _xxDevGraphWidth;
	}

	void setCursorStyle() {

		final ChartDataModel chartDataModel = _chart.getChartDataModel();
		if (chartDataModel == null) {
			return;
		}

		final int chartType = chartDataModel.getChartType();

		if (chartType == ChartDataModel.CHART_TYPE_LINE || chartType == ChartDataModel.CHART_TYPE_LINE_WITH_BARS) {

			if (_xSliderDragged != null) {

				// x-slider is dragged
				setCursor(_cursorModeZoomMove);

			} else {

				if (_chart.getMouseMode().equals(Chart.MOUSE_MODE_SLIDER)) {
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
	 * sets a new configuration for the graph, the whole graph will be recreated
	 */
	void setDrawingData(final ChartDrawingData chartDrawingData) {

		_chartDrawingData = chartDrawingData;

		// create empty list if list is not available, so we do not need
		// to check for null and isEmpty
		_graphDrawingData = chartDrawingData.graphDrawingData;

		_isGraphVisible = _graphDrawingData != null && _graphDrawingData.isEmpty() == false;

		// force all graphics to be recreated
		_isChartDirty = true;
		_isSliderDirty = true;
		_isCustomFgLayerDirty = true;
		_isSelectionDirty = true;

		// hide previous tooltip
		_toolTipV1.toolTip20Hide();

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

//	/**
//	 * Set the scrolling cursor according to the vertical position of the mouse
//	 *
//	 * @param devX
//	 * @param devY
//	 *            vertical coordinat of the mouse in the graph
//	 */
//	private void setupScrollCursor(final int devX, final int devY) {
//
//		final int height = getDevVisibleGraphHeight();
//		final int height4 = height / 4;
//		final int height2 = height / 2;
//
//		final float oldValue = _scrollAcceleration;
//
//		_scrollAcceleration = devY < height4 ? 0.25f : devY < height2 ? 1 : devY > height - height4 ? 10 : 2;
//
//		// set cursor according to the position
//		if (_scrollAcceleration == 0.25) {
//			setCursor(_cursorHand05x);
//		} else if (_scrollAcceleration == 1) {
//			setCursor(_cursorHand);
//		} else if (_scrollAcceleration == 2) {
//			setCursor(_cursorHand2x);
//		} else {
//			setCursor(_cursorHand5x);
//		}
//
//		/*
//		 * when the acceleration has changed, the start positions for scrolling the graph must be
//		 * set to the current location
//		 */
//		if (oldValue != _scrollAcceleration) {
//			_startPosScrollbar = getHorizontalBar().getSelection();
//			_startPosDev = devX;
//		}
//	}

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

		switch (_chart.getChartDataModel().getChartType()) {
		case ChartDataModel.CHART_TYPE_LINE:

			if (_selectedXSlider == null) {
				// set focus to the left slider when x-sliders are visible
				if (_isXSliderVisible) {
					_selectedXSlider = getLeftSlider();
					isFocus = true;
				}
			} else if (_selectedXSlider != null) {
				isFocus = true;
			}

			break;

		case ChartDataModel.CHART_TYPE_BAR:

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

			break;
		}

		if (isFocus) {
			_chart.fireFocusEvent();
		}

		ChartManager.getInstance().setActiveChart(isFocus ? _chart : null);

		return isFocus;
	}

	void setGraphSize(final int xxDevGraphWidth, final int xxDevViewPortOffset, final float graphZoomRatio) {

		_xxDevGraphWidth = xxDevGraphWidth;
		_xxDevViewPortOffset = xxDevViewPortOffset;
		_graphZoomRatio = graphZoomRatio;

		_xSliderA.moveToXXDevPosition(xxDevViewPortOffset, false, true);
		_xSliderB.moveToXXDevPosition(xxDevGraphWidth, false, true);
	}

	void setSelectedBars(final boolean[] selectedItems) {

		if (selectedItems == null) {

			// set focus to first bar item

			if (_graphDrawingData.size() == 0) {
				_selectedBarItems = null;
			} else {

				final GraphDrawingData graphDrawingData = _graphDrawingData.get(0);
				final ChartDataXSerie xData = graphDrawingData.getXData();

				_selectedBarItems = new boolean[xData._highValues[0].length];
			}

		} else {

			_selectedBarItems = selectedItems;
		}

		_isSelectionVisible = true;

		redrawBarSelection();
	}

	/**
	 * set the ratio at which the zoomed chart starts
	 */
	private void setXOffsetZoomRatio() {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		double devXOffset;

		if (_xSliderDragged != null /*
									 * && _devXDraggedXSliderLineZoomPosition !=
									 * _devXDraggedXSliderLine
									 */) {

//			//set zoom position and prevent that when the mouse was not moved, zooming will NOT be done
//			_devXDraggedXSliderLineZoomPosition = _devXDraggedXSliderLine;

			// set zoom center to the dragged slider line position
//			System.out.println(_devXDraggedXSliderLine + "\t");
//			// TODO remove SYSTEM.OUT.PRINTLN

			final double xxDevMousePosInChart = _xxDevViewPortOffset + _devXDraggedXSliderLine;
			_zoomCenterRatio = xxDevMousePosInChart / _xxDevGraphWidth;

			devXOffset = _zoomCenterRatio * _xxDevGraphWidth;

		} else {

			devXOffset = _zoomCenterRatio * _xxDevGraphWidth;
			devXOffset -= devVisibleChartWidth / 2;
		}

//		System.out.println("_zoomCenterRatio: " + _zoomCenterRatio);
//		// TODO remove SYSTEM.OUT.PRINTLN

		// adjust left border
		devXOffset = Math.max(devXOffset, 0);

		// adjust right border
		devXOffset = Math.min(devXOffset, _xxDevGraphWidth - devVisibleChartWidth);

		_zoomLeftBorderRatio = devXOffset / _xxDevGraphWidth;
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

		final float[] xValues = xData.getHighValues()[0];

		// adjust the slider index to the array bounds
		valueIndex = valueIndex < 0 ? //
				0
				: valueIndex > (xValues.length - 1) ? //
						xValues.length - 1
						: valueIndex;

		final float xValue = xValues[valueIndex];
		final double xxDevLinePos = (double) _xxDevGraphWidth * xValue / xValues[xValues.length - 1];

		slider.setValuesIndex(valueIndex);
		slider.setValueX(xValue);

		slider.moveToXXDevPosition((int) xxDevLinePos, true, true);

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

	private void setZoomInPosition() {

		// get left+right slider
		final ChartXSlider leftSlider = getLeftSlider();
		final ChartXSlider rightSlider = getRightSlider();

		final int devLeftVirtualSliderLinePos = leftSlider.getXXDevSliderLinePos();

		final int devZoomInPosInChart = devLeftVirtualSliderLinePos
				+ ((rightSlider.getXXDevSliderLinePos() - devLeftVirtualSliderLinePos) / 2);

		_zoomCenterRatio = (double) devZoomInPosInChart / _xxDevGraphWidth;
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

		if (_graphDrawingData.size() == 0) {
			return;
		}

		final GraphDrawingData graphDrawingData = _graphDrawingData.get(0);
		if (graphDrawingData == null) {
			return;
		}

		final ChartDataXSerie data2nd = graphDrawingData.getXData2nd();

		if (data2nd == null) {
			return;
		}

		final float[] xValues = data2nd.getHighValues()[0];
		int valueIndex = slider.getValuesIndex();

		if (valueIndex >= xValues.length) {
			valueIndex = xValues.length - 1;
			slider.setValuesIndex(valueIndex);
		}

		try {
			slider.setValueX(xValues[valueIndex]);

			final int linePos = (int) (_xxDevGraphWidth * (xValues[valueIndex] / xValues[xValues.length - 1]));

			slider.moveToXXDevPosition(linePos, true, true);

		} catch (final ArrayIndexOutOfBoundsException e) {
			// ignore
		}
	}

	void updateCustomLayers() {

		if (isDisposed()) {
			return;
		}

		_isCustomFgLayerDirty = true;
		_isSliderDirty = true;

		redraw();
	}

	private void updateDraggedChart(final int devXDiff) {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		double devXOffset = _xxDevViewPortOffset - devXDiff;

		// adjust left border
		devXOffset = Math.max(devXOffset, 0);

		// adjust right border
		devXOffset = Math.min(devXOffset, _xxDevGraphWidth - devVisibleChartWidth);

		_zoomLeftBorderRatio = devXOffset / _xxDevGraphWidth;

		/*
		 * reposition the mouse zoom position
		 */
		double xOffsetMouse = _zoomCenterRatio * _xxDevGraphWidth;
		xOffsetMouse = xOffsetMouse - devXDiff;
		_zoomCenterRatio = xOffsetMouse / _xxDevGraphWidth;

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
		_xxDevGraphWidth = (int) (_graphZoomRatio * devVisibleChartWidth);

		if (_graphZoomRatio == 1.0) {
			// with the ration 1.0 the graph is not zoomed
			_xxDevViewPortOffset = 0;
		} else {
			// the graph is zoomed, only a part is displayed which starts at
			// the offset for the left slider
			_xxDevViewPortOffset = (int) (_zoomLeftBorderRatio * _xxDevGraphWidth);
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
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			final ChartDataYSerie yData = drawingData.getYData();

			if (yData.isShowYSlider()) {

				final ChartYSlider sliderTop = yData.getYSliderTop();
				final ChartYSlider sliderBottom = yData.getYSliderBottom();

				sliderTop.handleChartResize(drawingData, ChartYSlider.SLIDER_TYPE_TOP);
				_ySliders.add(sliderTop);

				sliderBottom.handleChartResize(drawingData, ChartYSlider.SLIDER_TYPE_BOTTOM);
				_ySliders.add(sliderBottom);

				_isYSliderVisible = true;
			}
		}
	}

	/**
	 * sets the min/max values for the y-axis that the visible area will be filled with the chart
	 */
	void updateVisibleMinMaxValues() {

		final ChartDataModel chartDataModel = _chartComponents.getChartDataModel();
		final ChartDataXSerie xData = chartDataModel.getXData();
		final ArrayList<ChartDataYSerie> yDataList = chartDataModel.getYData();

		if (xData == null) {
			return;
		}

		final float[][] xValueSerie = xData.getHighValues();

		if (xValueSerie.length == 0) {
			// data are not available
			return;
		}

		final float[] xValues = xValueSerie[0];
		final float lastXValue = xValues[xValues.length - 1];
		final double valueVisibleArea = lastXValue / _graphZoomRatio;

		final double valueLeftBorder = lastXValue * _zoomLeftBorderRatio;
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
			final float xValue = xValues[serieIndex];
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

			final float[][] yValueSeries = yData.getHighValues();
			final float yValues[] = yValueSeries[0];

			// ensure array bounds
			final int yValuesLastIndex = yValues.length - 1;
			xValueIndexLeft = Math.min(xValueIndexLeft, yValuesLastIndex);
			xValueIndexLeft = Math.max(xValueIndexLeft, 0);
			xValueIndexRight = Math.min(xValueIndexRight, yValuesLastIndex);
			xValueIndexRight = Math.max(xValueIndexRight, 0);

			float minValue = yValues[xValueIndexLeft];
			float maxValue = yValues[xValueIndexLeft];

			for (final float[] yValueSerie : yValueSeries) {

				if (yValueSerie == null) {
					continue;
				}

				for (int valueIndex = xValueIndexLeft; valueIndex <= xValueIndexRight; valueIndex++) {

					final float yValue = yValueSerie[valueIndex];

					if (yValue < minValue) {
						minValue = yValue;
					}
					if (yValue > maxValue) {
						maxValue = yValue;
					}
				}
			}

			if (yData.isForceMinValue() == false && minValue != 0) {
				yData.setVisibleMinValue(minValue);
			}

			if (yData.isForceMaxValue() == false && maxValue != 0) {
				yData.setVisibleMaxValue(maxValue);
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

	void zoomInWithMouse() {

		final int devVisibleChartWidth = getDevVisibleChartWidth();
		final int devMaxChartWidth = ChartComponents.CHART_MAX_WIDTH;

		if (_xxDevGraphWidth <= devMaxChartWidth) {

			// chart can be zoomed in

			final double zoomedInRatio = _graphZoomRatio * ZOOM_RATIO_FACTOR;
			final int devZoomedInWidth = (int) (devVisibleChartWidth * zoomedInRatio);

			if (devZoomedInWidth > devMaxChartWidth) {

				// the zoomed graph would be wider than the max width, reduce it to the max width
				_graphZoomRatio = (double) devMaxChartWidth / devVisibleChartWidth;
				_xxDevGraphWidth = devMaxChartWidth;

			} else {

				_graphZoomRatio = zoomedInRatio;
				_xxDevGraphWidth = devZoomedInWidth;
			}

			setXOffsetZoomRatio();
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
		final int graphImageWidth = _xxDevGraphWidth;

		final int maxChartWidth = ChartComponents.CHART_MAX_WIDTH;

		if (graphImageWidth <= maxChartWidth) {

			// chart is within the range which can be zoomed in

			if (graphImageWidth * ZOOM_RATIO_FACTOR > maxChartWidth) {
				/*
				 * the double zoomed graph would be wider than the max width, reduce it to the max
				 * width
				 */
				_graphZoomRatio = maxChartWidth / visibleGraphWidth;
				_xxDevGraphWidth = maxChartWidth;
			} else {
				_graphZoomRatio = _graphZoomRatio * ZOOM_RATIO_FACTOR;
				_xxDevGraphWidth = (int) (graphImageWidth * ZOOM_RATIO_FACTOR);
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
		final int devVirtualLeftSliderPos = leftSlider.getXXDevSliderLinePos();
		final int devVirtualRightSliderPos = rightSlider.getXXDevSliderLinePos();

		// difference between left and right slider
		final float devSliderDiff = devVirtualRightSliderPos - devVirtualLeftSliderPos - 0;

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
			_zoomLeftBorderRatio = getLeftSlider().getPositionRatio();

			// set the center of the chart for the position when zooming with the mouse
			final double devVirtualWidth = _graphZoomRatio * devVisibleChartWidth;
			final double devXOffset = _zoomLeftBorderRatio * devVirtualWidth;
			final int devCenterPos = (int) (devXOffset + devVisibleChartWidth / 2);
			_zoomCenterRatio = devCenterPos / devVirtualWidth;
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
		_zoomLeftBorderRatio = 0;

		_xxDevGraphWidth = getDevVisibleChartWidth();
		_xxDevViewPortOffset = 0;

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
	 * @param updateChart
	 */
	void zoomOutWithMouse(final boolean updateChart) {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		if (_graphZoomRatio > ZOOM_RATIO_FACTOR) {

			_graphZoomRatio = _graphZoomRatio / ZOOM_RATIO_FACTOR;
			_xxDevGraphWidth = (int) (_graphZoomRatio * devVisibleChartWidth);

			setXOffsetZoomRatio();

			handleChartResizeForSliders();
			updateVisibleMinMaxValues();

			if (updateChart) {
				_chartComponents.onResize();
			}

		} else {

			if (_graphZoomRatio != 1) {

				_graphZoomRatio = 1;
				_xxDevGraphWidth = devVisibleChartWidth;

				setXOffsetZoomRatio();

				handleChartResizeForSliders();
				updateVisibleMinMaxValues();

				if (updateChart) {
					_chartComponents.onResize();
				}
			}
		}

		moveSlidersToBorder();

		_chart.enableActions();
	}
}
