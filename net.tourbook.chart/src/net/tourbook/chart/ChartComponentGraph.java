/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

/**
 * Draws the graph and axis into the canvas
 * 
 * @author Wolfgang Schramm
 */
public class ChartComponentGraph extends Canvas {

	private static final float			ZOOM_RATIO_FACTOR		= 1.3f;

	/**
	 * maximum width in pixel for the width of the tooltip
	 */
	private static final int			MAX_TOOLTIP_WIDTH		= 500;

	private static final int			BAR_MARKER_WIDTH		= 16;

	/**
	 * the factor is multiplied whth the visible graph width, so that the sliders are indented from
	 * the border to be good visible
	 */
	private static final double			ZOOM_REDUCING_FACTOR	= 0.1;

	private static final NumberFormat	_nf						= NumberFormat.getNumberInstance();

	private static final RGB			_gridRGB				= new RGB(241, 239, 226);

	Chart								_chart;
	private final ChartComponents		_chartComponents;

	/**
	 * contains the graphs without additional layers
	 */
	private Image						_graphImage;

	/**
	 * contains additional layers, like the x/y sliders, x-marker, selection or hovered bar
	 */
	private Image						_layerImage;

	/**
	 * contains custom layers like the markers, tour segments
	 */
	private Image						_cumstomLayerImage;

	private int							_horizontalScrollBarPos;

	/**
	 * drawing data which is used to draw the chart
	 */
	private ArrayList<ChartDrawingData>	_drawingData			= new ArrayList<ChartDrawingData>();

	/**
	 * zoom ratio between the visible and the virtual chart width
	 */
	private float						_graphZoomRatio			= 1;

	/**
	 * when the graph is zoomed and and can be scrolled, <code>canScrollZoomedChart</code> is
	 * <code>true</code>, the graph can be wider than the visial part. This field contains the width
	 * for the image when the whole tour would be displayed and not only a part
	 */
	private int							_devVirtualGraphImageWidth;

	/**
	 * when the zoomed graph can't be scrolled the chart image can be wider than the visible part,
	 * this field contains the offset for the start of the visible chart
	 */
	private int							_devGraphImageXOffset;

	/**
	 * ratio for the position where the chart starts on the left side within the virtual graph width
	 */
	private float						_xOffsetZoomRatio;

	/**
	 * ratio where the mouse was double clicked, this position is used to zoom the chart with the
	 * mouse
	 */
	private float						_xOffsetMouseZoomInRatio;

	/**
	 * the zoomed chart can be scrolled when set to <code>true</code>, for a zoomed chart, the chart
	 * image can be wider than the visible part and can be scrolled
	 */
	boolean								_canScrollZoomedChart;

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
	private boolean						_isGraphDirty;

	/**
	 * true indicates the slider needs to be redrawn in the paint event
	 */
	private boolean						_isSliderDirty;

	/**
	 * when set to <code>true</code> the custom layers above the graph image needs a redraw in the
	 * next paint event
	 */
	private boolean						_isCustomLayerDirty;

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
	 * slider which has the focus
	 */
	private ChartXSlider				_selectedXSlider;

	/**
	 * device position of the slider line when the slider is dragged
	 */
	private int							_devXScrollSliderLine;

	/**
	 * list for all y-sliders
	 */
	private ArrayList<ChartYSlider>		_ySliders;

	/**
	 * scroll position for the horizontal bar after the graph was zoomed
	 */
	private boolean						_isScrollToLeftSlider;

	/**
	 * <code>true</code> to scroll to the x-data selection
	 */
	private boolean						_isScrollToSelection;

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
	private Cursor						_cursorHand05x;
	private Cursor						_cursorHand;
	private Cursor						_cursorHand2x;
	private Cursor						_cursorHand5x;
	private Cursor						_cursorModeSlider;
	private Cursor						_cursorModeZoom;
	private Cursor						_cursorModeZoomMove;

	private Color						_gridColor;

	/**
	 * is set true when the graph is being moved with the mouse
	 */
	private boolean						_isGraphScrolled;

	/**
	 * position where the graph scrolling started
	 */
	private int							_startPosScrollbar;

	private int							_startPosDev;
	private float						_scrollAcceleration;

	/**
	 * offset when the chart is in autoscroll mode
	 */
	private int							_autoScrollOffset;

	private boolean						_isAutoScrollActive;

	/*
	 * tool tip resources
	 */
	private Shell						_toolTipShell;
	private Composite					_toolTipContainer;
	private Label						_toolTipTitle;
	private Label						_toolTipLabel;
	private Listener					_toolTipListener;

	private final int[]					_toolTipEvents			= new int[] {
			SWT.MouseExit,
			SWT.MouseHover,
			SWT.MouseMove,
			SWT.MouseDown,
			SWT.DragDetect										};
	/**
	 * serie index for the hovered bar, the bar is hidden when -1;
	 */
	private int							_hoveredBarSerieIndex	= -1;

	private int							_hoveredBarValueIndex;
	private boolean						_isHoveredBarDirty;
	private int							_toolTipHoverSerieIndex;
	private int							_toolTipHoverValueIndex;
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
	private int							_xMarkerValueDiff;

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

	private boolean						_isFocusActive;
	private boolean						_scrollSmoothly;
	private int							_smoothScrollEndPosition;
	private boolean						_isSmoothScrollingActive;
	private int							_smoothScrollCurrentPosition;

	private boolean						_isLayerImageDirty;

	private ChartToolTipInfo			_toolTipInfo;

	/*
	 * position of the mouse in the mouse down event
	 */
	private int							_mouseDownDevPositionX;
	private int							_mouseDownDevPositionY;

	private boolean						_isPaintDraggedImage	= false;

	/**
	 * is <code>true</code> when data for a graph is available
	 */
	private boolean						_isGraphVisible			= false;

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

		_cursorHand05x = createCursorFromImage(Messages.Image_cursor_hand_05x);
		_cursorHand = createCursorFromImage(Messages.Image_cursor_hand_10x);
		_cursorHand2x = createCursorFromImage(Messages.Image_cursor_hand_20x);
		_cursorHand5x = createCursorFromImage(Messages.Image_cursor_hand_50x);

		_cursorModeSlider = createCursorFromImage(Messages.Image_cursor_mode_slider);
		_cursorModeZoom = createCursorFromImage(Messages.Image_cursor_mode_zoom);
		_cursorModeZoomMove = createCursorFromImage(Messages.Image_cursor_mode_zoom_move);

		_gridColor = new Color(getDisplay(), _gridRGB);

		_chartComponents = (ChartComponents) parent;

		// setup the x-slider
		_xSliderA = new ChartXSlider(this, Integer.MIN_VALUE, ChartXSlider.SLIDER_TYPE_LEFT);
		_xSliderB = new ChartXSlider(this, Integer.MIN_VALUE, ChartXSlider.SLIDER_TYPE_RIGHT);

		_xSliderOnTop = _xSliderB;
		_xSliderOnBottom = _xSliderA;

		addListener();
		createContextMenu();

		setDefaultCursor();
	}

	/**
	 * execute the action which is defined when a bar is selected with the left mouse button
	 */
	private void actionSelectBars() {

		if (_hoveredBarSerieIndex < 0) {
			return;
		}

		boolean[] selectedBarItems;

		if (_drawingData.size() == 0) {
			selectedBarItems = null;
		} else {

			final ChartDrawingData chartDrawingData = _drawingData.get(0);
			final ChartDataXSerie xData = chartDrawingData.getXData();

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
					paintDraggedChart(event.gc);
				} else {
					paintChart(event.gc);
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

		_toolTipListener = new Listener() {
			public void handleEvent(final Event event) {
				switch (event.type) {
				case SWT.MouseHover:
				case SWT.MouseMove:
					if (updateToolTip(event.x, event.y)) {
						break;
					}
					// FALL THROUGH
				case SWT.MouseExit:
				case SWT.MouseDown:
					hideToolTip();
					break;
				}
			}
		};
	}

	private void adjustYSlider() {

		/*
		 * check if the y slider was outside of the bounds, recompute the chart when necessary
		 */

		final ChartDrawingData drawingData = _ySliderDragged.getDrawingData();

		final ChartDataYSerie yData = _ySliderDragged.getYData();
		final ChartYSlider slider1 = yData.getYSliderTop();
		final ChartYSlider slider2 = yData.getYSliderBottom();

		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - drawingData.getDevGraphHeight();
		final int graphYBottom = drawingData.getGraphYBottom();
		final float scaleY = drawingData.getScaleY();

		final int graphValue1 = (int) ((devYBottom - slider1.getDevYSliderLine()) / scaleY) + graphYBottom;
		final int graphValue2 = (int) ((devYBottom - slider2.getDevYSliderLine()) / scaleY) + graphYBottom;

		int minValue;
		int maxValue;

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
		setDefaultCursor();

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
			final Integer synchedChartMinValue = synchedChartMinMaxKeeper.getMinValues().get(yDataInfo);

			if (synchedChartMinValue != null) {
				synchedChartMinMaxKeeper.getMinValues().put(yDataInfo, minValue);
			}

			// adjust max value for the changed y-slider
			final Integer synchedChartMaxValue = synchedChartMinMaxKeeper.getMaxValues().get(yDataInfo);

			if (synchedChartMaxValue != null) {
				synchedChartMinMaxKeeper.getMaxValues().put(yDataInfo, maxValue);
			}
		}

		computeChart();
	}

	/**
	 */
	private void computeAutoScrollOffset() {

		final int visibleGraphWidth = getDevVisibleChartWidth();
		final ScrollBar hBar = getHorizontalBar();

		_autoScrollOffset = 0;

		// no auto scrolling if the slider is within the client area
		if (_devXScrollSliderLine >= 0 && _devXScrollSliderLine < visibleGraphWidth) {
			return;
		}

		final int scrollScale = 1;

		if (_devXScrollSliderLine < -1 && hBar.getSelection() > 0) {
			// graph can be scrolled to the left
			_autoScrollOffset = _devXScrollSliderLine * scrollScale;
		}
		if (_devXScrollSliderLine > -1 && hBar.getSelection() < (hBar.getMaximum() - hBar.getThumb())) {
			// graph can be scrolled to the right
			_autoScrollOffset = (_devXScrollSliderLine - visibleGraphWidth) * scrollScale;
		}
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

	private void computeSliderForContextMenu(final int devX, final int devY, final int graphX) {

		ChartXSlider slider1 = null;
		ChartXSlider slider2 = null;

		// reset the context slider
		_contextLeftSlider = null;
		_contextRightSlider = null;

		// check if a slider or the slider line was hit
		if (_xSliderA.getHitRectangle().contains(graphX, devY)) {
			slider1 = _xSliderA;
		}

		if (_xSliderB.getHitRectangle().contains(graphX, devY)) {
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

	private int computeXMarkerValue(final int[] xValues,
									final int xmStartIndex,
									final int valueDiff,
									final int valueXMarkerPosition) {

		int valueIndex;
		int valueX = xValues[xmStartIndex];
		int valueHalf;

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
	 * Computes the value of the x axis according to the slider position
	 * 
	 * @param slider
	 * @param devXSliderLinePosition
	 */
	void computeXSliderValue(final ChartXSlider slider, final int devXSliderLinePosition) {

		final ChartDataXSerie xData = getXData();

		if (xData == null) {
			return;
		}

		final int[][] xValueSerie = xData.getHighValues();

		if (xValueSerie.length == 0) {
			// data are not available
			return;
		}

		final int[] xValues = xValueSerie[0];
		final int serieLength = xValues.length;
		final int maxIndex = Math.max(0, serieLength - 1);

		int valueIndex;
		int xValue;

		/*
		 * disabled because gps data can have non-linear time, 15.01.2008 Wolfgang
		 */
//		final int xAxisUnit = xData.getAxisUnit();
//
//		if (xAxisUnit == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND) {
//
//			/*
//			 * For a linear x axis the slider value is also linear
//			 */
//
//			final float widthScale = (float) (maxIndex) / fDevVirtualGraphImageWidth;
//
//			// ensure the index is not out of bounds
//			valueIndex = (int) Math.max(0, Math.min(devXSliderLinePosition * widthScale, maxIndex));
//
//			xValue = xValues[valueIndex];
//
//		} else
		{

			/*
			 * The non time value (distance) is not linear, the value is increasing steadily but
			 * with different distance on the x axis. So first we have to find the nearest position
			 * in the values array and then interpolite from the found position to the slider
			 * position
			 */

			final int minValue = xData.getVisibleMinValue();
			final int maxValue = xData.getVisibleMaxValue();
			final int valueRange = maxValue > 0 ? (maxValue - minValue) : -(minValue - maxValue);

			final float positionRatio = (float) devXSliderLinePosition / _devVirtualGraphImageWidth;
			valueIndex = (int) (positionRatio * serieLength);

			// enforce array bounds
			valueIndex = Math.min(valueIndex, maxIndex);
			valueIndex = Math.max(valueIndex, 0);

			// sliderIndex points into the value array for the current slider
			// position
			xValue = xValues[valueIndex];

			// compute the value for the slider on the x-axis
			final int sliderValue = (int) (positionRatio * valueRange);

			if (xValue == sliderValue) {

				// nothing to do

			} else if (sliderValue > xValue) {

				/*
				 * in the value array move towards the end to find the position where the value of
				 * the slider corresponds with the value in the value array
				 */

				while (sliderValue > xValue) {

					xValue = xValues[valueIndex++];

					// check if end of the x-data are reached
					if (valueIndex == serieLength) {
						break;
					}
				}
				valueIndex--;
				xValue = xValues[valueIndex];

			} else {

				// valueX > valueSlider

				while (sliderValue < xValue) {

					// check if beginning of the x-data are reached
					if (valueIndex == 0) {
						break;
					}

					xValue = xValues[--valueIndex];
				}
			}

			// enforce maxIndex
			valueIndex = Math.min(valueIndex, maxIndex);
			xValue = xValues[valueIndex];

			// xValue = valueIndex * 1000;
			// valueX = valueSlider * 1000;
			// xValue = (int) (slider.getPositionRatio() * 1000000000);
		}

		slider.setValuesIndex(valueIndex);
		slider.setValueX(xValue);
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
				hideToolTip();

				_chart.fillContextMenu(
						menuMgr,
						_contextLeftSlider,
						_contextRightSlider,
						_hoveredBarSerieIndex,
						_hoveredBarValueIndex,
						_mouseDownDevPositionX,
						_mouseDownDevPositionY);
			}
		});

		setMenu(menuMgr.createContextMenu(this));
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
	 * @param slider
	 */
	private void createXSliderLabel(final GC gc, final ChartXSlider slider) {

		final int devSliderLinePos = slider.getDevVirtualSliderLinePos() - getDevGraphImageXOffset();

		int sliderValuesIndex = slider.getValuesIndex();
		// final int valueX = slider.getValueX();

		final ArrayList<ChartXSliderLabel> labelList = new ArrayList<ChartXSliderLabel>();
		slider.setLabelList(labelList);

		final ScrollBar hBar = getHorizontalBar();
		final int hBarOffset = hBar.isVisible() ? hBar.getSelection() : 0;

		final int leftPos = hBarOffset;
		final int rightPos = leftPos + getDevVisibleChartWidth();

		// create slider label for each graph
		for (final ChartDrawingData drawingData : _drawingData) {

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
			final int[] yValues = yData.getHighValues()[0];

			// make sure the slider value index is not of bounds, this can
			// happen when the data have changed
			sliderValuesIndex = Math.min(sliderValuesIndex, yValues.length - 1);

			final int yValue = yValues[sliderValuesIndex];
			// final int xAxisUnit = xData.getAxisUnit();
			final StringBuilder labelText = new StringBuilder();

			// create the slider text
			if (labelFormat == ChartDataYSerie.SLIDER_LABEL_FORMAT_MM_SS) {

				// format: mm:ss

				labelText.append(Util.format_mm_ss(yValue));

			} else {

				// use default format: ChartDataYSerie.SLIDER_LABEL_FORMAT_DEFAULT

				if (valueDivisor == 1) {
					labelText.append(_nf.format(yValue));
				} else {
					labelText.append(_nf.format((float) yValue / valueDivisor));
				}
			}

			labelText.append(' ');
			labelText.append(yData.getUnitLabel());
			labelText.append(' ');

			// calculate position of the slider label
			final Point labelExtend = gc.stringExtent(labelText.toString());
			final int labelWidth = labelExtend.x + 4;
			int labelXPos = devSliderLinePos - labelWidth / 2;

			final int labelRightPos = labelXPos + labelWidth;

			if (slider == _xSliderDragged) {
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
					if (_canScrollZoomedChart) {

						if (labelRightPos > _devVirtualGraphImageWidth) {
							labelXPos = _devVirtualGraphImageWidth - labelWidth - 1;
						}

					} else {
						if (labelRightPos > getDevVisibleChartWidth()) {
							labelXPos = getDevVisibleChartWidth() - labelWidth - 1;
						}
					}
				}
			}

			label.setText(labelText.toString());
			label.setHeight(labelExtend.y - 5);
			label.setWidth(labelWidth);
			label.setX(labelXPos);

			label.setY(drawingData.getDevYBottom() - drawingData.getDevGraphHeight() - label.getHeight());

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
			label.setYGraph(yGraph);
		}
	}

	/**
	 * Autoscroll to the left or right if the mouse is outside of the clientrect
	 */
	private void doAutoScroll() {

		final ScrollBar hBar = getHorizontalBar();
		final Display display = getDisplay();
		final int autoScrollInterval = 1;
		final int visibleGraphWidth = getDevVisibleChartWidth();

		if (hBar.isVisible() == false) {

			// no horizontal scrollbar, the graph fits into the client area

			// make sure the sliders are at the border of the visible area
			if (_devXScrollSliderLine < 0) {
				moveXSlider(_xSliderDragged, 0);
			} else {
				if (_devXScrollSliderLine > visibleGraphWidth - 1) {
					moveXSlider(_xSliderDragged, visibleGraphWidth - 1);
				}
			}
			// redraw slider
			_isSliderDirty = true;

			redraw();

		} else {

			// scrollbar is visible, graph is wider than the client area

			// make sure the sliders are at the border of the visible area
			// before auto scrolling starts
			if (_devXScrollSliderLine < 0) {
				moveXSlider(_xSliderDragged, hBar.getSelection());
			} else {
				if (_devXScrollSliderLine >= visibleGraphWidth - 1) {
					moveXSlider(_xSliderDragged, hBar.getSelection() + visibleGraphWidth - 1);
				}
			}
			// redraw slider
			_isSliderDirty = true;
			redraw();

			computeAutoScrollOffset();

			if (_autoScrollOffset != 0) {

				// the graph can be scrolled

				// ensure that only one instance will run
				_isAutoScrollActive = true;

				// start auto scrolling
				display.timerExec(autoScrollInterval, new Runnable() {

					public void run() {

						if (isDisposed() || _xSliderDragged == null) {
							_isAutoScrollActive = false;
							return;
						}

						// scroll the horizontal scroll bar
						final ScrollBar hBar = getHorizontalBar();

						computeAutoScrollOffset();

						hBar.setSelection(hBar.getSelection() + _autoScrollOffset);

						// scroll the slider
						moveXSlider(_xSliderDragged, _xSliderDragged.getDevVirtualSliderLinePos() + _autoScrollOffset);

						// redraw slider
						_isSliderDirty = true;
						redraw();

						// start scrolling again if the bounds have not been
						// reached
						if (_autoScrollOffset != 0) {
							display.timerExec(autoScrollInterval, this);
						} else {
							_isAutoScrollActive = false;
						}
					}
				});

			}
		}
	}

	/**
	 * Draws a bar graph, this requires that drawingData.getChartData2ndValues does not return null,
	 * if null is returned, a line graph will be drawn instead
	 * 
	 * @param gc
	 * @param drawingData
	 */
	private void drawBarGraph(final GC gc, final ChartDrawingData drawingData) {

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
		final int graphYBottom = drawingData.getGraphYBottom();
		final boolean axisDirection = yData.isYAxisDirection();

		// get the horizontal offset for the graph
		int graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a synch marker is not set, draw it normally
			graphValueOffset = (int) (Math.max(0, _devGraphImageXOffset) / scaleX);
		} else {
			// adjust the start position to the synch marker position
			graphValueOffset = (int) (_devGraphImageXOffset / scaleX);
		}

		// get the top/bottom of the graph
		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - drawingData.getDevGraphHeight();

		gc.setClipping(0, devYTop, gc.getClipping().width, devYBottom - devYTop);

		final int xValues[] = xData.getHighValues()[0];
		final int yHighSeries[][] = yData.getHighValues();
		final int yLowSeries[][] = yData.getLowValues();

		final int serieLength = yHighSeries.length;
		final int valueLength = xValues.length;

		// keep the bar rectangles
		final Rectangle[][] barRecangles = new Rectangle[serieLength][valueLength];
		final Rectangle[][] barFocusRecangles = new Rectangle[serieLength][valueLength];
		drawingData.setBarRectangles(barRecangles);
		drawingData.setBarFocusRectangles(barFocusRecangles);

		// keep the height for stacked bar charts
		final int devHeightSummary[] = new int[valueLength];

		final int devBarWidthOriginal = drawingData.getBarRectangleWidth();
		final int devBarWidth = Math.max(1, devBarWidthOriginal);

		final int serieLayout = yData.getChartLayout();
		final int devBarRectangleStartXPos = drawingData.getDevBarRectangleXPos();

		// loop: all data series
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			final int yHighValues[] = yHighSeries[serieIndex];
			int yLowValues[] = null;
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

			// loop: all values in the current serie
			for (int valueIndex = 0; valueIndex < valueLength; valueIndex++) {

				// get the x position
				final int devXPos = (int) ((xValues[valueIndex] - graphValueOffset) * scaleX) + devBarXPos;

//				final int devBarWidthSelected = devBarWidth;
//				final int devBarWidth2 = devBarWidthSelected / 2;
//
//				int devXPosSelected = devXPos;
//
//				// center the bar
//				if (devBarWidthSelected > 1 && barPosition == ChartDrawingData.BAR_POS_CENTER) {
//					devXPosSelected -= devBarWidth2;
//				}

				int valueYLow;
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
				final int valueYHigh = yHighValues[valueIndex];

				final int barHeight = (Math.max(valueYHigh, valueYLow) - Math.min(valueYHigh, valueYLow));
				if (barHeight == 0) {
					continue;
				}

				final int devBarHeight = (int) (barHeight * scaleY);

				// get the old y position for stacked bars
				int devYPreviousHeight = 0;
				if (serieLayout == ChartDataYSerie.BAR_LAYOUT_STACKED) {
					devYPreviousHeight = devHeightSummary[valueIndex];
				}

				// get the y position
				int devYPos;
				if (axisDirection) {
					devYPos = devYBottom - ((int) ((valueYHigh - graphYBottom) * scaleY) + devYPreviousHeight);
				} else {
					devYPos = devYTop + ((int) ((valueYLow - graphYBottom) * scaleY) + devYPreviousHeight);
				}

				final Rectangle barShape = new Rectangle(devXPos, devYPos, devBarWidthPositioned, devBarHeight);

				final int colorIndex = colorsIndex[serieIndex][valueIndex];
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
				if (devBarWidthOriginal > 0) {

					gc.setForeground(colorBright);
					gc.fillGradientRectangle(barShape.x, barShape.y, barShape.width, barShape.height, false);

					gc.setForeground(colorLine);
					gc.drawRectangle(barShape);

				} else {

					gc.setForeground(colorLine);
					gc.drawLine(barShape.x, barShape.y, barShape.x, (barShape.y + barShape.height));
				}

				barRecangles[serieIndex][valueIndex] = barShape;
				barFocusRecangles[serieIndex][valueIndex] = new Rectangle(//
						devXPos - 2,
						(devYPos - 2),
						devBarWidthPositioned + 4,
						(devBarHeight + 7));

				// keep the height for the bar
				devHeightSummary[valueIndex] += devBarHeight;
			}
		}

		// reset clipping
		gc.setClipping((Rectangle) null);
	}

	private void drawBarSelection(final GC gc, final ChartDrawingData drawingData) {

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
			gc
					.fillRoundRectangle(
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

	private void drawCustomLayerImage() {

		// the layer above image is the same size as the graph image
		final Rectangle graphRect = _graphImage.getBounds();

		// ensure correct image size
		if (graphRect.width <= 0 || graphRect.height <= 0) {
			return;
		}

		/*
		 * when the existing image is the same size as the new image, we will redraw it only if it's
		 * set to dirty
		 */
		if (_isCustomLayerDirty == false && _cumstomLayerImage != null) {

			final Rectangle oldBounds = _cumstomLayerImage.getBounds();

			if (oldBounds.width == graphRect.width && oldBounds.height == graphRect.height) {
				return;
			}
		}

		if (Util.canReuseImage(_cumstomLayerImage, graphRect) == false) {
			_cumstomLayerImage = Util.createImage(getDisplay(), _cumstomLayerImage, graphRect);
		}

		final GC gc = new GC(_cumstomLayerImage);

		gc.fillRectangle(graphRect);

		/*
		 * copy the image with the graphs into the custom layer image, the custom layers are drawn
		 * on top of the graphs
		 */
//		if (fIsGraphDirty == false) {
		gc.drawImage(_graphImage, 0, 0);
//		}

		for (final ChartDrawingData drawingData : _drawingData) {
			for (final IChartLayer layer : drawingData.getYData().getCustomLayers()) {
				layer.draw(gc, drawingData, _chart);
			}
		}

		gc.dispose();

		_isCustomLayerDirty = false;
	}

	/**
	 * draws the graph into the graph image
	 */
	private void drawGraphImage() {

		_drawCounter[0]++;

		final Runnable imageThread = new Runnable() {

			final int	fRunnableDrawCounter	= _drawCounter[0];

			public void run() {

//				long startTime = System.currentTimeMillis();

				/*
				 * create the chart image only when a new onPaint event has not occured
				 */
				if (fRunnableDrawCounter != _drawCounter[0]) {
					// a new onPaint event occured
					return;
				}

				if (isDisposed()) {
					// this widget is disposed
					return;
				}

				final int devNonScrolledImageWidth = Math.max(
						ChartComponents.CHART_MIN_WIDTH,
						getDevVisibleChartWidth());

				final int devNewImageWidth = _canScrollZoomedChart
						? _devVirtualGraphImageWidth
						: devNonScrolledImageWidth;

				/*
				 * the image size is adjusted to the client size but it must be within the min/max
				 * ranges
				 */
				final int devNewImageHeight = Math.max(ChartComponents.CHART_MIN_HEIGHT, Math.min(
						getDevVisibleGraphHeight(),
						ChartComponents.CHART_MAX_HEIGHT));

				/*
				 * when the image is the same size as the new we will redraw it only if it is set to
				 * dirty
				 */
				if (_isGraphDirty == false && _graphImage != null) {

					final Rectangle oldBounds = _graphImage.getBounds();

					if (oldBounds.width == devNewImageWidth && oldBounds.height == devNewImageHeight) {
						return;
					}
				}

				final Rectangle imageRect = new Rectangle(0, 0, devNewImageWidth, devNewImageHeight);

				// ensure correct image size
				if (imageRect.width <= 0 || imageRect.height <= 0) {
					return;
				}

				if (Util.canReuseImage(_graphImage, imageRect) == false) {

					// create image on which the graph is drawn
					_graphImage = Util.createImage(getDisplay(), _graphImage, imageRect);
				}

				// create graphics context
				final GC gc = new GC(_graphImage);

				gc.setFont(_chart.getFont());

				// fill background
				gc.setBackground(_chart.getBackgroundColor());
				gc.fillRectangle(_graphImage.getBounds());

				// draw all graphs
				int graphIndex = 0;

				// loop: all graphs
				for (final ChartDrawingData drawingData : _drawingData) {

					if (graphIndex == 0) {
						drawXTitle(gc, drawingData);
					}

					drawSegments(gc, drawingData);

					if (graphIndex == _drawingData.size() - 1) {
						// draw the unit label and unit tick for the last graph
						drawXUnitsAndGrid(gc, drawingData, true, true);
					} else {
						drawXUnitsAndGrid(gc, drawingData, false, true);
					}

					drawHorizontalGridlines(gc, drawingData);

					// draw units and grid on the x and y axis
					switch (drawingData.getChartType()) {
					case ChartDataModel.CHART_TYPE_LINE:
						drawLineGraph(gc, drawingData);
						drawRangeMarker(gc, drawingData);
						break;

					case ChartDataModel.CHART_TYPE_BAR:
						drawBarGraph(gc, drawingData);
						break;

					case ChartDataModel.CHART_TYPE_LINE_WITH_BARS:
						drawLineWithBarGraph(gc, drawingData);
						break;

					default:
						break;
					}

					graphIndex++;
				}

				gc.dispose();

				// remove dirty status
				_isGraphDirty = false;

				// dragged image will be painted until the graph image is recomputed
				_isPaintDraggedImage = false;

				/*
				 * force the layer image to be redrawn
				 */
				_isLayerImageDirty = true;

				redraw();

//				long endTime = System.currentTimeMillis();
//				System.out.println("Execution time : " + (endTime - startTime) + " ms");
			}
		};

		getDisplay().asyncExec(imageThread);
	}

	/**
	 * draw the vertical gridlines
	 * 
	 * @param gc
	 * @param drawingData
	 */
	private void drawHorizontalGridlines(final GC gc, final ChartDrawingData drawingData) {

		final Display display = getDisplay();

		final int devYBottom = drawingData.getDevYBottom();
		final ArrayList<ChartUnit> unitList = drawingData.getYUnits();

		int unitCount = 0;

		final float scaleY = drawingData.getScaleY();
		final int graphYBottom = drawingData.getGraphYBottom();
		final int devGraphHeight = drawingData.getDevGraphHeight();
		final boolean yAxisDirection = drawingData.getYData().isYAxisDirection();

		final int devYTop = devYBottom - devGraphHeight;

		// loop: all units
		for (final ChartUnit unit : unitList) {

			int devY;
			if (yAxisDirection || (unitList.size() == 1)) {
				// bottom->top
				devY = devYBottom - (int) ((unit.value - graphYBottom) * scaleY);
			} else {
				// top->bottom
				devY = devYTop + (int) ((unit.value - graphYBottom) * scaleY);
			}

			if ((yAxisDirection == false && unitCount == unitList.size() - 1) || (yAxisDirection && unitCount == 0)) {

				// draw x-axis

				gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
				gc.setLineStyle(SWT.LINE_SOLID);

			} else {

				// draw gridlines

				gc.setForeground(_gridColor);
			}

			gc.drawLine(0, devY, _devVirtualGraphImageWidth, devY);

			unitCount++;
		}
	}

	private void drawHoveredBar(final GC gc) {

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
		for (final ChartDrawingData drawingData : _drawingData) {

			// get the chart data
			final ChartDataYSerie yData = drawingData.getYData();
			final int serieLayout = yData.getChartLayout();
			final int[][] colorsIndex = yData.getColorsIndex();

			// get the colors
			final RGB[] rgbLine = yData.getRgbLine();
			final RGB[] rgbDark = yData.getRgbDark();
			final RGB[] rgbBright = yData.getRgbBright();

			final int devYBottom = drawingData.getDevYBottom();
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

	/**
	 * draws the layer image which contains the custom layer image
	 */
	private void drawLayerImage() {

		if (_cumstomLayerImage == null) {
			return;
		}

		// the slider image is the same size as the graph image
		final Rectangle graphRect = _cumstomLayerImage.getBounds();

		/*
		 * check if the layer image needs to be redrawn
		 */
		if (_isLayerImageDirty == false
				&& _isSliderDirty == false
				&& _isSelectionDirty == false
				&& _isHoveredBarDirty == false
				&& _layerImage != null) {

			final Rectangle oldBounds = _layerImage.getBounds();
			if (oldBounds.width == graphRect.width && oldBounds.height == graphRect.height) {
				return;
			}
		}

		// ensure correct image size
		if (graphRect.width <= 0 || graphRect.height <= 0) {
			return;
		}

		if (Util.canReuseImage(_layerImage, graphRect) == false) {
			_layerImage = Util.createImage(getDisplay(), _layerImage, graphRect);
		}

		if (_layerImage.isDisposed()) {
			return;
		}

		final GC gc = new GC(_layerImage);

		// copy the graph image into the slider image, the slider will be drawn
		// on top of the graph
		gc.fillRectangle(graphRect);
//		if (fIsGraphDirty == false) {
		gc.drawImage(_cumstomLayerImage, 0, 0);
//		}

		/*
		 * draw x/y-sliders
		 */
		if (_isXSliderVisible) {
			createXSliderLabel(gc, _xSliderOnTop);
			createXSliderLabel(gc, _xSliderOnBottom);
			updateXSliderYPosition();

			drawXSlider(gc, _xSliderOnBottom);
			drawXSlider(gc, _xSliderOnTop);

		}
		if (_isYSliderVisible) {
			drawYSliders(gc);
		}
		_isSliderDirty = false;

		if (_isXMarkerMoved) {
			drawXMarker(gc);
		}

		if (_isSelectionVisible) {
			drawSelection(gc);
		}

		if (_isHoveredBarDirty) {
			drawHoveredBar(gc);
			_isHoveredBarDirty = false;
		}

		gc.dispose();

		_isLayerImageDirty = false;
	}

	private void drawLineGraph(final GC gc, final ChartDrawingData drawingData) {

		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();

		final int xValues[] = xData.getHighValues()[0];
		final float scaleX = drawingData.getScaleX();

		final RGB rgbFg = yData.getRgbLine()[0];
		final RGB rgbBg1 = yData.getRgbDark()[0];
		final RGB rgbBg2 = yData.getRgbBright()[0];

		// get the horizontal offset for the graph
		int graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = (int) (Math.max(0, _devGraphImageXOffset) / scaleX);
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = (int) (_devGraphImageXOffset / scaleX);
		}

		if (xData.getSynchMarkerStartIndex() == -1) {

			// synch marker is not displayed

			drawLineGraphSegment(
					gc,
					drawingData,
					0,
					xValues.length,
					rgbFg,
					rgbBg1,
					rgbBg2,
					_graphAlpha,
					graphValueOffset);

		} else {

			// draw synched tour

			final int xMarkerAlpha = 0xd0;
			final int noneMarkerAlpha = 0x60;

			// draw the x-marker
			drawLineGraphSegment(
					gc,
					drawingData,
					xData.getSynchMarkerStartIndex(),
					xData.getSynchMarkerEndIndex() + 1,
					rgbFg,
					rgbBg1,
					rgbBg2,
					xMarkerAlpha,
					graphValueOffset);

			// draw segment before the marker
			drawLineGraphSegment(
					gc,
					drawingData,
					0,
					xData.getSynchMarkerStartIndex() + 1,
					rgbFg,
					rgbBg1,
					rgbBg2,
					noneMarkerAlpha,
					graphValueOffset);

			// draw segment after the marker
			drawLineGraphSegment(
					gc,
					drawingData,
					xData.getSynchMarkerEndIndex() - 0,
					xValues.length,
					rgbFg,
					rgbBg1,
					rgbBg2,
					noneMarkerAlpha,
					graphValueOffset);
		}
	}

	/**
	 * first we draw the graph into a path, the path is then drawn on the device with a
	 * transformation
	 * 
	 * @param gc
	 * @param drawingData
	 * @param startIndex
	 * @param endIndex
	 * @param rgbFg
	 * @param rgbBg
	 * @param rgbBg2
	 * @param graphValueOffset
	 */
	private void drawLineGraphSegment(	final GC gc,
										final ChartDrawingData drawingData,
										final int startIndex,
										final int endIndex,
										final RGB rgbFg,
										final RGB rgbBg1,
										final RGB rgbBg2,
										final int alphaValue,
										final int graphValueOffset) {

		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();
		final int graphFillMethod = yData.getGraphFillMethod();

		final int[][] highValues = yData.getHighValues();

		final int xValues[] = xData.getHighValues()[0];
		final int yValues[] = highValues[0];

		// check array bounds
		if (startIndex >= xValues.length) {
			return;
		}

		final boolean isPath2 = highValues.length > 1;
		int[] yValues2 = null;
		if (isPath2) {
			yValues2 = highValues[1];
		}

		final int graphYBottom = drawingData.getGraphYBottom();
		final int graphYTop = drawingData.getGraphYTop();

		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

		final Display display = getDisplay();
		final Path path = new Path(display);
		final Path path2 = isPath2 ? new Path(display) : null;

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();

		// virtual 0 line for the y-axis of the chart in dev units
		final float devY0 = devYBottom + (scaleY * graphYBottom);

		float devXPrev = xValues[startIndex] * scaleX;

		final Rectangle chartRectangle = gc.getClipping();

		// draw the lines into the path
		for (int xValueIndex = startIndex; xValueIndex < endIndex; xValueIndex++) {

			// check array bounds
			if (xValueIndex >= yValues.length) {
				break;
			}

			int graphX = xValues[xValueIndex];

			if (_canScrollZoomedChart == false) {
				graphX -= graphValueOffset;
			}

			int yValue = yValues[xValueIndex];
			int yValue2 = 0;

			// force the bottom and top value not to drawn over the border
			if (yValue < graphYBottom) {
				yValue = graphYBottom;
			}
			if (yValue > graphYTop) {
				yValue = graphYTop;
			}

			if (path2 != null) {

				yValue2 = yValues2[xValueIndex];

				// force the bottom and top value not to drawn over the border
				if (yValue2 < graphYBottom) {
					yValue2 = graphYBottom;
				}
				if (yValue2 > graphYTop) {
					yValue2 = graphYTop;
				}
			}

			final float devX = graphX * scaleX;

			/*
			 * draw first point
			 */
			if (xValueIndex == startIndex) {

				// move to the first point

				if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM) {

					// start from the bottom of the chart
					path.moveTo(devX, devY0 - (graphYBottom * scaleY));

				} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

					// start from the x-axis
					final int graphXAxisLine = graphYBottom > 0 ? graphYBottom : graphYTop < 0 ? graphYTop : 0;
					path.moveTo(devX, devY0 - (graphXAxisLine * scaleY));
				}

				if (path2 != null) {
					path2.moveTo(devX, devY0 - (yValue2 * scaleY));
				}
			}

			// optimize to draw only one line starting at the current x-position
			if (devX != devXPrev) {

				// draw line to the next point
				path.lineTo(devX, devY0 - (yValue * scaleY));

				if (path2 != null) {
					path2.lineTo(devX, devY0 - (yValue2 * scaleY));
				}
			}

			/*
			 * draw last point
			 */
			if ((xValueIndex == endIndex - 1 && //
			(graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM || //
			graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO))) {

				/*
				 * this is the last point for a filled graph, draw the line to the x-axis
				 */

				int graphXAxisLine = 0;

				if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM) {
					graphXAxisLine = graphYBottom > 0 ? graphYBottom : graphYTop < 0 ? graphYTop : graphYBottom;

				} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

					graphXAxisLine = graphYBottom > 0 ? graphYBottom : graphYTop < 0 ? graphYTop : 0;
				}

				path.lineTo(devX, devY0 - (graphXAxisLine * scaleY));
				path.moveTo(devX, devY0 - (graphXAxisLine * scaleY));

				if (path2 != null) {
					path.lineTo(devX, devY0 - (yValue2 * scaleY));
					path.moveTo(devX, devY0 - (yValue2 * scaleY));
				}
			}

			devXPrev = devX;
		}

		final Color colorFg = new Color(display, rgbFg);
		final Color colorBg1 = new Color(display, rgbBg1);
		final Color colorBg2 = new Color(display, rgbBg2);

		gc.setAntialias(SWT.OFF);
		gc.setAlpha(alphaValue);

		gc.setForeground(colorBg1);
		gc.setBackground(colorBg2);

		final int devGraphHeight = drawingData.getDevGraphHeight();

		int graphWidth = xValues[Math.min(xValues.length - 1, endIndex)];
		if (_canScrollZoomedChart == false) {
			graphWidth -= graphValueOffset;
		}

		/*
		 * force a max width because on linux the fill will not be drawn
		 */
		final int devChartWidth = Math.min(0x7fff, (int) (graphWidth * scaleX));

		// fill the graph
		if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM) {

			/*
			 * adjust the fill gradient in the hight, otherwise the fill is not in the whole
			 * rectangle
			 */

			gc.setClipping(path);

			gc.fillGradientRectangle(
					0,
					(int) (devY0 - (graphYBottom * scaleY)) + 1,
					devChartWidth + 5,
					-devGraphHeight,
					true);

			gc.setClipping(chartRectangle);

		} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

			final int graphFillYBottom = graphYBottom > 0 ? graphYBottom : 0;

			gc.setClipping(path);

			/*
			 * fill above 0 line
			 */
			gc.fillGradientRectangle(0, (int) (devY0 - (graphFillYBottom * scaleY)) + 1, devChartWidth, (int) -Math
					.min(devGraphHeight, Math.abs(devYTop - devY0)), true);

			/*
			 * fill below 0 line
			 */
			gc.setForeground(colorBg2);
			gc.setBackground(colorBg1);

			gc.fillGradientRectangle(
					0,
					devYBottom,
					devChartWidth,
					(int) -Math.min(devGraphHeight, devYBottom - devY0),
					true);

			gc.setClipping(chartRectangle);
		}

		// draw the line of the graph
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		gc.setForeground(colorFg);
		// gc.setBackground(colorBg1);
		gc.drawPath(path);

		// dispose resources
		colorFg.dispose();
		colorBg1.dispose();
		colorBg2.dispose();

		path.dispose();

		/*
		 * draw path2 above the other graph
		 */
		if (path2 != null) {

			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			gc.drawPath(path2);

			path2.dispose();
		}

		gc.setAlpha(0xFF);
	}

	/**
	 * Draws a bar graph, this requires that drawingData.getChartData2ndValues does not return null,
	 * if null is returned, a line graph will be drawn instead
	 * 
	 * @param gc
	 * @param drawingData
	 */
	private void drawLineWithBarGraph(final GC gc, final ChartDrawingData drawingData) {

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
		final int graphYBottom = drawingData.getGraphYBottom();
		final boolean axisDirection = yData.isYAxisDirection();
		final int barPosition = drawingData.getBarPosition();

		// get the horizontal offset for the graph
		int graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = (int) (Math.max(0, _devGraphImageXOffset) / scaleX);
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = (int) (_devGraphImageXOffset / scaleX);
		}

		// get the top/bottom of the graph
		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - drawingData.getDevGraphHeight();

		// virtual 0 line for the y-axis of the chart in dev units
//		final float devChartY0Line = (float) devYBottom + (scaleY * graphYBottom);

		gc.setClipping(0, devYTop, gc.getClipping().width, devYBottom - devYTop);

		final int xValues[] = xData.getHighValues()[0];
		final int yHighSeries[][] = yData.getHighValues();
//		final int yLowSeries[][] = yData.getLowValues();

		final int serieLength = yHighSeries.length;
		final int valueLength = xValues.length;

		final int devBarWidthComputed = drawingData.getBarRectangleWidth();
		final int devBarWidth = Math.max(1, devBarWidthComputed);

		final int devBarXPos = drawingData.getDevBarRectangleXPos();

		// loop: all data series
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			final int yHighValues[] = yHighSeries[serieIndex];
//			int yLowValues[] = null;
//			if (yLowSeries != null) {
//				yLowValues = yLowSeries[serieIndex];
//			}

			// loop: all values in the current serie
			for (int valueIndex = 0; valueIndex < valueLength; valueIndex++) {

				// get the x position
				final int devXPos = (int) ((xValues[valueIndex] - graphValueOffset) * scaleX) + devBarXPos;

				final int devBarWidthSelected = devBarWidth;
				final int devBarWidth2 = devBarWidthSelected / 2;

				int devXPosSelected = devXPos;

				// center the bar
				if (devBarWidthSelected > 1 && barPosition == ChartDrawingData.BAR_POS_CENTER) {
					devXPosSelected -= devBarWidth2;
				}

				// get the bar height
				final int graphYLow = graphYBottom;
				final int graphYHigh = yHighValues[valueIndex];

				final int graphBarHeight = (Math.max(graphYHigh, graphYLow) - Math.min(graphYHigh, graphYLow));

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

	private void drawRangeMarker(final GC gc, final ChartDrawingData drawingData) {

//		final RGB colorRangeMarker = new RGB(0, 200, 200);

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
		int graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = (int) (Math.max(0, _devGraphImageXOffset) / scaleX);
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = (int) (_devGraphImageXOffset / scaleX);
		}

		int runningIndex = 0;
		for (final int markerStartIndex : startIndex) {

			// draw range marker
			drawLineGraphSegment(gc, //
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

	private void drawSegments(final GC gc, final ChartDrawingData drawingData) {

		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();

		if (chartSegments == null) {
			return;
		}

		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = drawingData.getDevYTop();
		final float scaleX = drawingData.getScaleX();

		final int[] startValues = chartSegments.valueStart;
		final int[] endValues = chartSegments.valueEnd;

		if (startValues == null || endValues == null) {
			return;
		}

		final Color alternateColor = new Color(gc.getDevice(), 0xf5, 0xf5, 0xf5); // efefef

		for (int segmentIndex = 0; segmentIndex < startValues.length; segmentIndex++) {

			if (segmentIndex % 2 == 1) {

				// draw segment background color

				final int startValue = startValues[segmentIndex];
				final int endValue = endValues[segmentIndex];

				final int devValueStart = (int) (scaleX * startValue) - _devGraphImageXOffset;

				// adjust endValue to fill the last part of the segment
				final int devValueEnd = (int) (scaleX * (endValue + 1)) - _devGraphImageXOffset;

				gc.setBackground(alternateColor);
				gc.fillRectangle(devValueStart, //
						devYTop,
						devValueEnd - devValueStart,
						devYBottom - devYTop);
			}
		}

		alternateColor.dispose();
	}

	private void drawSelection(final GC gc) {

		_isSelectionDirty = false;

		final int chartType = _chart.getChartDataModel().getChartType();

		// loop: all graphs
		for (final ChartDrawingData drawingData : _drawingData) {
			switch (chartType) {
			case ChartDataModel.CHART_TYPE_LINE:
				// drawLineSelection(gc, drawingData);
				break;

			case ChartDataModel.CHART_TYPE_BAR:
				drawBarSelection(gc, drawingData);
				break;

			default:
				break;
			}
		}
	}

	private void drawXMarker(final GC gc) {

		final Display display = getDisplay();
		final Color colorXMarker = new Color(display, 255, 153, 0);

		final int devDraggingDiff = _devXMarkerDraggedPos - _devXMarkerDraggedStartPos;

		// draw x-marker for each graph
		for (final ChartDrawingData drawingData : _drawingData) {

			final ChartDataXSerie xData = drawingData.getXData();
			final float scaleX = drawingData.getScaleX();

			final int valueDraggingDiff = (int) (devDraggingDiff / scaleX);

			final int synchStartIndex = xData.getSynchMarkerStartIndex();
			final int synchEndIndex = xData.getSynchMarkerEndIndex();

			final int[] xValues = xData.getHighValues()[0];
			final int valueXStart = xValues[synchStartIndex];
			final int valueXEnd = xValues[synchEndIndex];
			// fXMarkerValueDiff = valueXEnd - valueXStart;

			final int devXStart = (int) (scaleX * valueXStart - _devGraphImageXOffset);
			final int devXEnd = (int) (scaleX * valueXEnd - _devGraphImageXOffset);
			int devMovedXStart = devXStart;
			int devMovedXEnd = devXEnd;

			final int valueXStartWithOffset = valueXStart + valueDraggingDiff;
			final int valueXEndWithOffset = valueXEnd + valueDraggingDiff;

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

			devMovedXStart = (int) (scaleX * xValues[_movedXMarkerStartValueIndex] - _devGraphImageXOffset);
			devMovedXEnd = (int) (scaleX * xValues[_movedXMarkerEndValueIndex] - _devGraphImageXOffset);

			/*
			 * when the moved x-marker is on the right or the left border, make sure that the
			 * x-markers don't get too small
			 */
			final int valueMovedDiff = xValues[_movedXMarkerEndValueIndex] - xValues[_movedXMarkerStartValueIndex];

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
				final int valueFirstIndex = xValues[xValues.length - 1] - _xMarkerValueDiff;

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

				final int valueStart = xValues[_movedXMarkerStartValueIndex];
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

			devMovedXStart = (int) (scaleX * xValues[_movedXMarkerStartValueIndex] - _devGraphImageXOffset);
			devMovedXEnd = (int) (scaleX * xValues[_movedXMarkerEndValueIndex] - _devGraphImageXOffset);

			final int graphTop = drawingData.getDevYBottom() - drawingData.getDevGraphHeight();
			final int graphBottom = drawingData.getDevYBottom();

			// draw moved x-marker
			gc.setForeground(colorXMarker);
			gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));

			gc.setAlpha(0x80);

			gc.fillGradientRectangle(
					devMovedXStart,
					graphBottom,
					devMovedXEnd - devMovedXStart,
					graphTop - graphBottom,
					true);

			gc.drawLine(devMovedXStart, graphTop, devMovedXStart, graphBottom);
			gc.drawLine(devMovedXEnd, graphTop, devMovedXEnd, graphBottom);

			gc.setAlpha(0xff);
		}

		colorXMarker.dispose();
	}

	/**
	 * @param gc
	 * @param slider
	 */
	private void drawXSlider(final GC gc, final ChartXSlider slider) {

		final Display display = getDisplay();

		final int devSliderLinePos = slider.getDevVirtualSliderLinePos() - getDevGraphImageXOffset();

		final int grayColorIndex = 60;
		final Color colorTxt = new Color(display, grayColorIndex, grayColorIndex, grayColorIndex);

		int labelIndex = 0;

		final ArrayList<ChartXSliderLabel> labelList = slider.getLabelList();

		// draw slider for each graph
		for (final ChartDrawingData drawingData : _drawingData) {

			final ChartDataYSerie yData = drawingData.getYData();
			final ChartXSliderLabel label = labelList.get(labelIndex);

			final Color colorLine = new Color(display, yData.getRgbLine()[0]);
			final Color colorBright = new Color(display, yData.getRgbBright()[0]);
			final Color colorDark = new Color(display, yData.getRgbDark()[0]);

			final int labelHeight = label.getHeight();
			final int labelWidth = label.getWidth();
			final int labelX = label.getX();
			final int labelY = label.getY();

			final int devYBottom = drawingData.getDevYBottom();
			final boolean isSliderHovered = _mouseOverXSlider != null && _mouseOverXSlider == slider;

			/*
			 * when the mouse is over the slider, the slider is drawn in darker color
			 */
			// draw slider line
			if ((_isFocusActive && _selectedXSlider == slider) || isSliderHovered) {
				gc.setAlpha(0xd0);
			} else {
				gc.setAlpha(0x60);
			}
			gc.setForeground(colorLine);
			gc.setLineStyle(SWT.LINE_DOT);
			gc.drawLine(devSliderLinePos, labelY + labelHeight, devSliderLinePos, devYBottom);

			/*
			 * left and right slider have different label backgrounds
			 */
//			if (slider == getLeftSlider()) {
//				// left slider
			gc.setBackground(colorDark);
			gc.setForeground(colorBright);
//			} else {
//				// right slider
//				gc.setBackground(colorBright);
//				gc.setForeground(colorDark);
//			}

			// draw label background
//			gc.fillGradientRectangle(labelX + 1, labelY, labelWidth - 1, labelHeight, false);
			gc.fillRectangle(labelX + 1, labelY, labelWidth - 1, labelHeight);

			// draw label border
			gc.setForeground(colorLine);
			gc.setLineStyle(SWT.LINE_SOLID);
			gc.drawRoundRectangle(labelX, labelY, labelWidth, labelHeight, 4, 4);

			// draw slider label
			gc.setForeground(colorTxt);
			gc.drawText(label.getText(), labelX + 2, labelY - 2, true);

			// draw a tiny marker on the graph
			gc.setAlpha(0xff);
			gc.setBackground(colorLine);
			gc.fillRectangle(devSliderLinePos - 3, label.getYGraph() - 2, 7, 3);

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

				gc.setAlpha(0xc0);
				gc.setLineStyle(SWT.LINE_SOLID);

				// draw background
				gc.setBackground(colorDark);
				gc.fillPolygon(marker);

				// draw border
				gc.setForeground(colorLine);
				gc.drawPolygon(marker);

				gc.setAlpha(0xff);
			}

			colorLine.dispose();
			colorBright.dispose();
			colorDark.dispose();

			labelIndex++;
		}

		colorTxt.dispose();
	}

	private void drawXTitle(final GC gc, final ChartDrawingData drawingData) {

		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();
		final int devYTitle = drawingData.getDevMarginTop();

		final int devGraphWidth = _canScrollZoomedChart ? drawingData.getDevGraphWidth() : _chartComponents
				.getDevVisibleChartWidth();

		if (chartSegments == null) {

			/*
			 * draw default title, center within the chart
			 */

			final String title = drawingData.getXTitle();

			if (title == null || title.length() == 0) {
				return;
			}

			gc.drawText(title, //
					(devGraphWidth / 2) - (gc.textExtent(title).x / 2),
					(devYTitle),
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

				for (int segmentIndex = 0; segmentIndex < valueStart.length; segmentIndex++) {

					final int devValueStart = (int) (scaleX * valueStart[segmentIndex]) - _devGraphImageXOffset;
					final int devValueEnd = (int) (scaleX * (valueEnd[segmentIndex] + 1)) - _devGraphImageXOffset;

					final String segmentTitle = segmentTitles[segmentIndex];

//					gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

					// draw the title in the center of the segment
					if (segmentTitle != null) {
						gc
								.drawText(segmentTitle, //
										devValueEnd
												- ((devValueEnd - devValueStart) / 2)
												- (gc.textExtent(segmentTitle).x / 2),
										devYTitle,
										false);
					}
				}
			}
		}

	}

	/**
	 * Draw the unit label, tick and the vertical grid line for the x axis
	 * 
	 * @param gc
	 * @param drawingData
	 * @param drawUnit
	 *            <code>true</code> indicate to draws the unit tick and unit label additional to the
	 *            unit grid line
	 * @param draw0Unit
	 *            <code>true</code> indicate to draw the unit at the 0 position
	 */
	private void drawXUnitsAndGrid(	final GC gc,
									final ChartDrawingData drawingData,
									final boolean drawUnit,
									final boolean draw0Unit) {

		final Display display = getDisplay();

		final ArrayList<ChartUnit> units = drawingData.getXUnits();

		final int devYBottom = drawingData.getDevYBottom();
		final int unitPos = drawingData.getXUnitTextPos();
		float scaleX = drawingData.getScaleX();

		// check if the x-units has a special scaling
		final float scaleUnitX = drawingData.getScaleUnitX();
		if (scaleUnitX != Float.MIN_VALUE) {
			scaleX = scaleUnitX;
		}

		// compute the distance between two units
		final float devUnitWidth = units.size() > 1 ? //
				((units.get(1).value * scaleX) - (units.get(0).value * scaleX))
				: 0;

		float devXOffset = 0;
		int unitCounterLeft = 0;
		int unitCounterRight = 0;
		int unitCounter = 0;
		int skippedUnits = 0;
		boolean isOptimized = false;

		final int devVisibleChartWidth = getDevVisibleChartWidth();
		final boolean isLineChart = _chartComponents.getChartDataModel().getChartType() != ChartDataModel.CHART_TYPE_BAR;

		if (isLineChart && _canScrollZoomedChart == false && _devGraphImageXOffset > 0) {
			// calculate the unit offset
			unitCounterLeft = (int) (_devGraphImageXOffset / devUnitWidth);
			devXOffset -= _devGraphImageXOffset % devUnitWidth;

			unitCounterRight = (int) ((_devGraphImageXOffset + devVisibleChartWidth) / devUnitWidth);

			isOptimized = true;
		}

		boolean isUnitLabelPrinted = false;

		for (final ChartUnit unit : units) {

			if (isOptimized) {

				/*
				 * skip units which are not displayed
				 */
				if (unitCounter < unitCounterLeft) {

					devXOffset -= devUnitWidth;

					unitCounter++;
					skippedUnits++;

					continue;
				}

				if (unitCounter > unitCounterRight) {
					break;
				}
			}

			// dev x-position for the unit tick
			final int devXUnitTick = (int) (devXOffset + (unit.value * scaleX));

			/*
			 * the first unit is not painted because it would clip at the left border of the chart
			 * canvas
			 */
			if ((unitCounter == 0 && draw0Unit) || unitCounter > 0) {

				if (drawUnit) {

					gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));

					// draw the unit tick
					if (unitCounter > 0) {
						gc.setLineStyle(SWT.LINE_SOLID);
						gc.drawLine(devXUnitTick, devYBottom, devXUnitTick, devYBottom + 5);
					}

					final Point unitValueExtend = gc.textExtent(unit.valueLabel);
					final int unitValueExtendX = unitValueExtend.x;

					// draw the unit value
					if (devUnitWidth != 0 && unitPos == ChartDrawingData.XUNIT_TEXT_POS_CENTER) {

						// draw the unit value BETWEEN two units

						final int devXUnitCentered = (int) Math.max(0, ((devUnitWidth - unitValueExtendX) / 2) + 0);

						gc.drawText(unit.valueLabel, devXUnitTick + devXUnitCentered, devYBottom + 7, true);

					} else {

						// draw the unit value in the MIDDLE of the unit tick

						/*
						 * when the chart is zoomed and not scrolled, prevent to clip the text at
						 * the left border
						 */
						final int unitValueExtend2 = unitValueExtendX / 2;
						if (unitCounter == 0 || devXUnitTick >= 0) {

							if (unitCounter == 0) {

								// this is the first unit, do not center it

								if (devXUnitTick == 0) {

									gc.drawText(unit.valueLabel, devXUnitTick, devYBottom + 7, true);

									// draw unit label (km, mi, h)
									if (isUnitLabelPrinted == false) {
										isUnitLabelPrinted = true;
										gc.drawText(drawingData.getXData().getUnitLabel(),//
												devXUnitTick + unitValueExtendX + 2,
												devYBottom + 7,
												true);
									}
								}

							} else {

								// center the unit text

								int devXUnitValue = devXUnitTick - unitValueExtend2;
								if (devXUnitValue >= 0) {

									if ((devXUnitTick + unitValueExtend2) > devVisibleChartWidth) {

										/*
										 * unit value would be clipped at the chart border, move it
										 * to the left to make it fully visible
										 */

										devXUnitValue = devVisibleChartWidth - unitValueExtendX;
									}

									gc.drawText(unit.valueLabel, devXUnitValue, devYBottom + 7, true);

									// draw unit label (km, mi, h)
									if (isUnitLabelPrinted == false) {

										isUnitLabelPrinted = true;

										gc.drawText(drawingData.getXData().getUnitLabel(),//
												devXUnitTick + unitValueExtend2 + 2,
												devYBottom + 7,
												true);
									}
								}
							}
						}
					}
				}

				// draw the vertical gridline
				if (unitCounter > 0) {

					gc.setForeground(_gridColor);
					gc.drawLine(devXUnitTick, devYBottom, devXUnitTick, devYBottom - drawingData.getDevGraphHeight());
				}
			}

			unitCounter++;
		}
	}

	/**
	 * @param gc
	 * @param slider
	 */
	private void drawYSliders(final GC gc) {

		final Display display = getDisplay();

		final int grayColorIndex = 60;
		final Color colorTxt = new Color(display, grayColorIndex, grayColorIndex, grayColorIndex);

		for (final ChartYSlider ySlider : _ySliders) {

			if (_hitYSlider == ySlider) {

				final ChartDataYSerie yData = ySlider.getYData();

				final Color colorLine = new Color(display, yData.getRgbLine()[0]);
				final Color colorBright = new Color(display, yData.getRgbBright()[0]);
				final Color colorDark = new Color(display, yData.getRgbDark()[0]);

				final ChartDrawingData drawingData = ySlider.getDrawingData();
				final int devYBottom = drawingData.getDevYBottom();
				final int devYTop = devYBottom - drawingData.getDevGraphHeight();

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

				final int devYValue = (int) ((devYBottom - devYSliderLine) / drawingData.getScaleY())
						+ drawingData.getGraphYBottom();

				// create the slider text
				labelText.append(Util.formatValue(devYValue, yData.getAxisUnit(), yData.getValueDivisor(), true));
				labelText.append(' ');
				labelText.append(yData.getUnitLabel());
				labelText.append("  "); //$NON-NLS-1$
				final String label = labelText.toString();

				final Point labelExtend = gc.stringExtent(label);

				final int labelHeight = labelExtend.y - 2;
				final int labelWidth = labelExtend.x + 0;
				final int labelX = _ySliderGraphX - labelWidth - 5;
				final int labelY = devYLabelPos - labelHeight;

				// draw label background
				gc.setForeground(colorBright);
				gc.setBackground(colorDark);
				gc.setAlpha(0xb0);
				gc.fillGradientRectangle(labelX, labelY, labelWidth, labelHeight, true);

				// draw label border
				gc.setAlpha(0xa0);
				gc.setForeground(colorLine);
				gc.drawRectangle(labelX, labelY, labelWidth, labelHeight);
				gc.setAlpha(0xff);

				// draw label text
				gc.setForeground(colorTxt);
				gc.drawText(label, labelX + 2, labelY - 2, true);

				// draw slider line
				final Rectangle hitRect = ySlider.getHitRectangle();
				gc.setForeground(colorLine);
				gc.setLineStyle(SWT.LINE_DOT);
				gc.drawLine(0, devYLabelPos, hitRect.width, devYLabelPos);

				// gc.setLineStyle(SWT.LINE_SOLID);

				colorLine.dispose();
				colorBright.dispose();
				colorDark.dispose();
			}
		}

		colorTxt.dispose();
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

// this is a performance bottleneck
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
	 * @return when the zoomed graph can't be scrolled the chart image can be wider than the visible
	 *         part. It returns the device offset to the start of the visible chart
	 */
	int getDevGraphImageXOffset() {
		return _devGraphImageXOffset;
	}

	/**
	 * @return Returns the virtual graph image width, this is the width of the graph image when the
	 *         full graph would be displayed
	 */
	int getDevVirtualGraphImageWidth() {
		return _devVirtualGraphImageWidth;
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

	/**
	 * @return Returns the visible width of the chart graph
	 */
	private int getDevVisibleChartWidth() {
		return _chartComponents.getDevVisibleChartWidth() - 0;
	}

	/**
	 * @return Returns the visible height of the chart graph
	 */
	private int getDevVisibleGraphHeight() {
		return _chartComponents.getDevVisibleChartHeight();
	}

	public float getGraphZoomRatio() {
		return _graphZoomRatio;
	}

	/**
	 * @return Returns the left slider
	 */
	ChartXSlider getLeftSlider() {
		return _xSliderA.getDevVirtualSliderLinePos() < _xSliderB.getDevVirtualSliderLinePos() ? _xSliderA : _xSliderB;
	}

	/**
	 * @return Returns the right most slider
	 */
	ChartXSlider getRightSlider() {
		return _xSliderA.getDevVirtualSliderLinePos() < _xSliderB.getDevVirtualSliderLinePos() ? _xSliderB : _xSliderA;
	}

	ChartXSlider getSelectedSlider() {

		final ChartXSlider slider = _selectedXSlider;

		if (slider == null) {
			return getLeftSlider();
		}
		return slider;
	}

	private ChartToolTipInfo getToolTipInfo(final int x, final int y) {

		if (_hoveredBarSerieIndex != -1) {

			// get the method which computes the bar info
			final IChartInfoProvider toolTipInfoProvider = (IChartInfoProvider) _chart
					.getChartDataModel()
					.getCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER);

			if (toolTipInfoProvider != null) {

				if (_toolTipHoverSerieIndex == _hoveredBarSerieIndex
						&& _toolTipHoverValueIndex == _hoveredBarValueIndex) {

					// tool tip is already displayed for the hovered bar

					if (_toolTipInfo != null) {
						_toolTipInfo.setIsDisplayed(true);
					}

				} else {

					_toolTipHoverSerieIndex = _hoveredBarSerieIndex;
					_toolTipHoverValueIndex = _hoveredBarValueIndex;

					_toolTipInfo = toolTipInfoProvider.getToolTipInfo(_hoveredBarSerieIndex, _hoveredBarValueIndex);
				}

				return _toolTipInfo;
			}
		}

		// reset tool tip hover index
		_toolTipHoverSerieIndex = -1;
		_toolTipHoverValueIndex = -1;

		return null;
	}

	/**
	 * Returns the size of the graph for the given bounds, the size will be reduced when the
	 * scrollbars are visible
	 * 
	 * @param bounds
	 *            is the size of the receiver where the chart can be drawn
	 * @return bounds for the chart without scrollbars
	 */
	Point getVisibleSizeWithHBar(final int width, final int height) {

		int horizontalBarHeight = 0;

		if (getHorizontalBar() != null) {
			// to get the size of the horizontal bar forces a resize event on
			// this component
			horizontalBarHeight += getHorizontalBar().getSize().y;
		}

		final int x = width;
		final int y = (_canScrollZoomedChart && _devVirtualGraphImageWidth > width)
				? (height - horizontalBarHeight)
				: height;

		return new Point(x, y);
	}

	/**
	 * @return Returns the x-Data in the drawing data list
	 */
	private ChartDataXSerie getXData() {
		if (_drawingData.size() == 0) {
			return null;
		} else {
			return _drawingData.get(0).getXData();
		}
	}

	private ChartDrawingData getXDrawingData() {
		return _drawingData.get(0);
	}

	private void handleChartResizeForSliders() {

		// update the width in the sliders
		final int visibleGraphHeight = getDevVisibleGraphHeight();

		getLeftSlider().handleChartResize(visibleGraphHeight);
		getRightSlider().handleChartResize(visibleGraphHeight);
	}

	void hideToolTip() {

		if (_toolTipShell == null || _toolTipShell.isDisposed()) {
			return;
		}

		if (_toolTipShell.isVisible()) {

			/*
			 * when hiding the tooltip, reposition the tooltip the next time when the tool tip is
			 * displayed
			 */
			_toolTipInfo.setReposition(true);

			_toolTipShell.setVisible(false);
		}
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
		for (final ChartDrawingData drawingData : _drawingData) {

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

						showToolTip(graphX, 100);

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

			hideToolTip();

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

		final int[] xValues = xData.getHighValues()[0];
		final float scaleX = getXDrawingData().getScaleX();

		final int devXMarkerStart = (int) (xValues[Math.min(synchMarkerStartIndex, xValues.length - 1)] * scaleX - _devGraphImageXOffset);
		final int devXMarkerEnd = (int) (xValues[Math.min(synchMarkerEndIndex, xValues.length - 1)] * scaleX - _devGraphImageXOffset);

		if (devXGraph >= devXMarkerStart && devXGraph <= devXMarkerEnd) {
			return true;
		}

		return false;
	}

	/**
	 * Check if the tooltip is too far away from the cursor position
	 * 
	 * @return Returns <code>true</code> when the cursor is too far away
	 */
	private boolean isToolTipWrongPositioned() {

		final Point cursorLocation = getDisplay().getCursorLocation();
		final Point toolTipLocation = _toolTipShell.getLocation();

		final int cursorAreaLength = 50;

		final Rectangle cursorArea = new Rectangle(cursorLocation.x - cursorAreaLength, cursorLocation.y
				- cursorAreaLength, 2 * cursorAreaLength, 2 * cursorAreaLength);

		if (cursorArea.contains(toolTipLocation)) {
			return false;
		} else {
			return true;
		}
	}

	private ChartXSlider isXSliderHit(final int devYMouse, final int devXGraph) {

		ChartXSlider xSlider = null;

		if (_xSliderA.getHitRectangle().contains(devXGraph, devYMouse)) {
			xSlider = _xSliderA;
		} else if (_xSliderB.getHitRectangle().contains(devXGraph, devYMouse)) {
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
	 * moves the right slider to the mouse position
	 */
	void moveLeftSliderHere() {

		final ChartXSlider leftSlider = getLeftSlider();
		final int devLeftPosition = _devGraphImageXOffset + _mouseDownDevPositionX;

		computeXSliderValue(leftSlider, devLeftPosition);
		leftSlider.moveToDevPosition(devLeftPosition, true, true);

		setZoomInPosition();

		_isSliderDirty = true;
		redraw();
	}

	/**
	 * moves the left slider to the mouse position
	 */
	void moveRightSliderHere() {

		final ChartXSlider rightSlider = getRightSlider();
		final int devRightPosition = _devGraphImageXOffset + _mouseDownDevPositionX;

		computeXSliderValue(rightSlider, devRightPosition);
		rightSlider.moveToDevPosition(devRightPosition, true, true);

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
		final int devLeftPosition = _devGraphImageXOffset + 2;

		computeXSliderValue(leftSlider, devLeftPosition);
		leftSlider.moveToDevPosition(devLeftPosition, true, true);

		/*
		 * adjust right slider
		 */
		final int devRightPosition = _devGraphImageXOffset + getDevVisibleChartWidth() - 2;

		computeXSliderValue(rightSlider, devRightPosition);
		rightSlider.moveToDevPosition(devRightPosition, true, true);

		_isSliderDirty = true;
		redraw();
	}

	/**
	 * Move the slider to a new position
	 * 
	 * @param xSlider
	 *            Current slider
	 * @param devSliderLinePos
	 *            x coordinate for the slider line within the graph, this can be outside of the
	 *            visible graph
	 */
	private void moveXSlider(final ChartXSlider xSlider, int devSliderLinePos) {

		devSliderLinePos += _devGraphImageXOffset;

		/*
		 * adjust the line position the the min/max width of the graph image
		 */
		devSliderLinePos = Math.min(_devVirtualGraphImageWidth, Math.max(0, devSliderLinePos));

		computeXSliderValue(xSlider, devSliderLinePos);

		// set new slider line position
		xSlider.moveToDevPosition(devSliderLinePos, true, true);
	}

	/**
	 * move the x-slider with the keyboard
	 * 
	 * @param event
	 */
	private void moveXSlider(final Event event) {

		boolean isShift = (event.stateMask & SWT.SHIFT) != 0;
		boolean isCtrl = (event.stateMask & SWT.CTRL) != 0;

		if (_selectedXSlider == null) {
			final ChartXSlider leftSlider = getLeftSlider();
			if (leftSlider != null) {
				// set default slider
				_selectedXSlider = leftSlider;
			} else {
				return;
			}
		}

		if (isShift && (event.stateMask & SWT.CTRL) == 0) {
			// select the other slider
			_selectedXSlider = _selectedXSlider == _xSliderA ? _xSliderB : _xSliderA;
			_isSliderDirty = true;
			redraw();
			return;
		}

		if (event.keyCode == SWT.PAGE_UP || event.keyCode == SWT.PAGE_DOWN) {
			isCtrl = true;
			isShift = true;
		}

		int valueIndex = _selectedXSlider.getValuesIndex();
		final int[] xValues = getXData().getHighValues()[0];

		// accelerate slider move speed
		int sliderDiff = isCtrl ? 10 : 1;
		sliderDiff *= isShift ? 10 : 1;

		switch (event.keyCode) {
		case SWT.PAGE_DOWN:
		case SWT.ARROW_RIGHT:

			valueIndex += sliderDiff;

			if (valueIndex >= xValues.length) {
				valueIndex = 0;
			}
			break;

		case SWT.PAGE_UP:
		case SWT.ARROW_LEFT:

			valueIndex -= sliderDiff;

			if (valueIndex < 0) {
				valueIndex = xValues.length - 1;
			}

			break;

		case SWT.HOME:

			valueIndex = 0;

			break;

		case SWT.END:

			valueIndex = xValues.length - 1;

			break;
		}

		setXSliderValueIndex(_selectedXSlider, valueIndex, false);

		redraw();
		setDefaultCursor();
	}

	private void moveYSlider(final ChartYSlider ySlider, final int graphX, final int devY) {

		final int devYSliderLine = devY - ySlider.getDevYClickOffset() + ChartYSlider.halfSliderHitLineHeight;

		ySlider.setDevYSliderLine(graphX, devYSliderLine);
	}

	/**
	 * Dispose event handler
	 */
	private void onDispose() {

		// dispose resources
		_cursorResizeLeftRight = Util.disposeResource(_cursorResizeLeftRight);
		_cursorResizeTopDown = Util.disposeResource(_cursorResizeTopDown);
		_cursorDragged = Util.disposeResource(_cursorDragged);
		_cursorHand05x = Util.disposeResource(_cursorHand05x);
		_cursorHand = Util.disposeResource(_cursorHand);
		_cursorHand2x = Util.disposeResource(_cursorHand2x);
		_cursorHand5x = Util.disposeResource(_cursorHand5x);
		_cursorModeSlider = Util.disposeResource(_cursorModeSlider);
		_cursorModeZoom = Util.disposeResource(_cursorModeZoom);
		_cursorModeZoomMove = Util.disposeResource(_cursorModeZoomMove);

		_graphImage = Util.disposeResource(_graphImage);
		_layerImage = Util.disposeResource(_layerImage);
		_cumstomLayerImage = Util.disposeResource(_cumstomLayerImage);

		_gridColor = Util.disposeResource(_gridColor);

		// dispose tooltip
		if (_toolTipShell != null) {
			hideToolTip();
			for (final int toolTipEvent : _toolTipEvents) {
				removeListener(toolTipEvent, _toolTipListener);
			}
			_toolTipShell.dispose();
			_toolTipShell = null;
			_toolTipContainer = null;
		}

		_colorCache.dispose();
	}

	private void onKeyDown(final Event event) {

		switch (_chart.getChartDataModel().getChartType()) {
		case ChartDataModel.CHART_TYPE_BAR:
			_chartComponents.selectBarItem(event);
			break;

		case ChartDataModel.CHART_TYPE_LINE:
			moveXSlider(event);
			break;

		default:
			break;
		}
	}

	void onMouseDoubleClick(final MouseEvent e) {

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
				final int devMousePosInChart = _devGraphImageXOffset + e.x;
				_xOffsetMouseZoomInRatio = (float) devMousePosInChart / _devVirtualGraphImageWidth;

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

		final ScrollBar hBar = getHorizontalBar();
		final int hBarOffset = hBar.isVisible() ? hBar.getSelection() : 0;

		final int devXMouse = event.x;
		final int devYMouse = event.y;
		final int devXGraph = hBarOffset + devXMouse;

		_mouseDownDevPositionX = event.x;
		_mouseDownDevPositionY = event.y;

		// show context menu
		if (event.button != 1) {
			if (event.button == 3) {
				computeSliderForContextMenu(devXMouse, devYMouse, devXGraph);
			}
			return;
		}

		if (_chart.isMouseDownExternalPre(devXMouse, devYMouse, devXGraph)) {
			return;
		}

		// check if a x-slider was hit
		_xSliderDragged = null;
		if (_xSliderA.getHitRectangle().contains(devXGraph, devYMouse)) {
			_xSliderDragged = _xSliderA;
		} else if (_xSliderB.getHitRectangle().contains(devXGraph, devYMouse)) {
			_xSliderDragged = _xSliderB;
		}

		if (_xSliderDragged != null) {

			// x-slider was hit and can now be dragged on a mouse move event

			_xSliderOnTop = _xSliderDragged;
			_xSliderOnBottom = _xSliderOnTop == _xSliderA ? _xSliderB : _xSliderA;

			// set the hit offset for the mouse click
			_xSliderDragged.setDevXClickOffset(devXGraph - _xSliderDragged.getHitRectangle().x);

			// the hit x-slider is now the selected x-slider
			_selectedXSlider = _xSliderDragged;
			_isSelectionVisible = true;
			_isSliderDirty = true;
			redraw();

		} else {

			// a x-slider isn't dragged

			// check if a y-slider was hit
			_ySliderDragged = isYSliderHit(devXGraph, devYMouse);

			if (_ySliderDragged != null) {

				// y-slider was hit

				_ySliderDragged.setDevYClickOffset(devYMouse - _ySliderDragged.getHitRectangle().y);

			} else if (_hoveredBarSerieIndex != -1) {

				actionSelectBars();

			} else if (hBar.isVisible()) {

				// start scrolling the graph if the scrollbar is visible

				_isGraphScrolled = true;

				_startPosScrollbar = hBarOffset;
				_startPosDev = devXMouse;

				setupScrollCursor(devXMouse, devYMouse);

			} else if (_chart._draggingListenerXMarker != null && isSynchMarkerHit(devXGraph)) {

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

			} else {

				// do post processing when no other ations are done

//				if (fChart.isMouseDownExternalPost(devXMouse, devYMouse, devXGraph)) {
//					return;
//				}

				if (_graphZoomRatio > 1) {

					// start moving the chart

					/*
					 * to prevent flickering with the double click event, dragged started is used
					 */
					_isChartDraggedStarted = true;

					_draggedChartStartPos = new Point(event.x, event.y);

					/*
					 * set also the move position because when changing the data model, the old
					 * position will be used and the chart is painted on the wrong position on mouse
					 * down
					 */
					_draggedChartDraggedPos = _draggedChartStartPos;

					setCursor(_cursorDragged);

				}
			}
		}
	}

	/**
	 * Mouse exit event handler
	 * 
	 * @param event
	 */
	private void onMouseExit(final MouseEvent event) {

		if (_isGraphScrolled) {

			_isGraphScrolled = false;

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

		setDefaultCursor();
	}

	/**
	 * Mouse move event handler
	 * 
	 * @param event
	 */
	private void onMouseMove(final MouseEvent event) {

		final ScrollBar hBar = getHorizontalBar();
		final int hBarOffset = hBar.isVisible() ? hBar.getSelection() : 0;

		final int devXMouse = event.x;
		final int devYMouse = event.y;
		final int devXGraph = hBarOffset + devXMouse;

		boolean isChartDirty = false;

		if (_isGraphScrolled) {

			// graph is scrolled by the mouse

			final int scrollDiff = devXMouse - _startPosDev;
			final int scrollPos = (int) (_startPosScrollbar - (scrollDiff * _scrollAcceleration));

			setupScrollCursor(devXMouse, devYMouse);

			// adjust the scroll position if the mouse is moved outside the
			// bounds
			if (scrollPos < 0) {
				_startPosScrollbar = hBarOffset;
				_startPosDev = devXMouse;
			} else {

				final int maxSelection = hBar.getMaximum() - hBar.getThumb();

				if (scrollPos > maxSelection) {
					_startPosScrollbar = hBarOffset;
					_startPosDev = devXMouse;
				}
			}

			hBar.setSelection(scrollPos);

			redraw();

		} else if (_isXSliderVisible && _xSliderDragged != null) {

			// x-slider is dragged

			// keep position of the slider line
			_devXScrollSliderLine = devXMouse
					- _xSliderDragged.getDevXClickOffset()
					+ ChartXSlider.halfSliderHitLineHeight;

			if (_canScrollZoomedChart) {

				// the graph can be scrolled

				if (_devXScrollSliderLine > -1 && _devXScrollSliderLine < getDevVisibleChartWidth()) {

					// slider is within the visible area, no autoscrolling is
					// done

					moveXSlider(_xSliderDragged, devXGraph
							- _xSliderDragged.getDevXClickOffset()
							+ ChartXSlider.halfSliderHitLineHeight);

					_isSliderDirty = true;
					isChartDirty = true;

				} else {

					// slider is outside the visible area, auto scroll the
					// slider and graph when this is not yet done
					if (_isAutoScrollActive == false) {
						doAutoScroll();
					}
				}

			} else {

				// the graph can't be scrolled

				moveXSlider(_xSliderDragged, devXGraph
						- _xSliderDragged.getDevXClickOffset()
						+ ChartXSlider.halfSliderHitLineHeight);

				_isSliderDirty = true;
				isChartDirty = true;

//				setChartPosition(xSliderDragged);
			}

		} else if (_isChartDraggedStarted || _isChartDragged) {

			// chart is dragged with the mouse

			_isChartDraggedStarted = false;
			_isChartDragged = true;

			_draggedChartDraggedPos = new Point(event.x, event.y);

			isChartDirty = true;

		} else if (_isYSliderVisible && _ySliderDragged != null) {

			// y-slider is dragged

			moveYSlider(_ySliderDragged, devXGraph, devYMouse);
			_ySliderGraphX = devXGraph;

			_isSliderDirty = true;
			isChartDirty = true;

		} else if (_isXMarkerMoved) {

			_devXMarkerDraggedPos = devXGraph;

			_isSliderDirty = true;
			isChartDirty = true;

		} else {

			// set the cursor shape depending on the mouse location

			ChartXSlider xSlider;

			if (_chart.isMouseMoveExternal(devXMouse, devYMouse, devXGraph)) {

				setCursor(_cursorDragged);

			} else if (_isXSliderVisible && (xSlider = isXSliderHit(devYMouse, devXGraph)) != null) {

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

			} else if (_isYSliderVisible && isYSliderHit(devXGraph, devYMouse) != null) {

				// cursor is within a y-slider

				setCursor(_cursorResizeTopDown);

				// show the y-slider labels
				_ySliderGraphX = devXGraph;

				_isSliderDirty = true;
				isChartDirty = true;

			} else if (_chart._draggingListenerXMarker != null && isSynchMarkerHit(devXGraph)) {

				setCursor(_cursorDragged);

			} else if (isBarHit(devYMouse, devXGraph)) {

				_isHoveredBarDirty = true;
				isChartDirty = true;

				setDefaultCursor();

			} else if (hBar.isVisible()) {

				// horizontal bar is visible, show the scroll cursor

				setupScrollCursor(devXMouse, devYMouse);

			} else {

				setDefaultCursor();
			}
		}

		if (isChartDirty) {
			redraw();
		}
	}

//	void onKeyUp(final Event event) {
//
//		final boolean isShift = (event.stateMask & SWT.SHIFT) != 0;
//		final boolean isCtrl = (event.stateMask & SWT.CTRL) != 0;
//
//		final boolean isMoveMode = isShift || isCtrl;
//
//		fIsMoveMode = !isMoveMode;
//
//		System.out.println(fIsMoveMode + " " + event.stateMask + " s:" + isShift + " c:" + isCtrl);
//		setDefaultCursor();
//	}

	/**
	 * Mouse up event handler
	 * 
	 * @param event
	 */
	private void onMouseUp(final MouseEvent event) {

		final ScrollBar hBar = getHorizontalBar();
		final int hBarOffset = hBar.isVisible() ? hBar.getSelection() : 0;

		final int devXMouse = event.x;
		final int devYMouse = event.y;
		final int devXGraph = hBarOffset + devXMouse;

		if (_isGraphScrolled) {
			_isGraphScrolled = false;
		} else {

			if (_chart.isMouseUpExternal(devXMouse, devYMouse, devXGraph)) {
				return;
			}

			if (_xSliderDragged != null) {

				// stop dragging the slider
				_xSliderDragged = null;

				if (_canScrollZoomedChart == false && _canAutoZoomToSlider) {

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

			} else if (_ySliderDragged != null) {

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

				setDefaultCursor();

				updateDraggedChart(_draggedChartDraggedPos.x - _draggedChartStartPos.x);
			}

			// show scroll cursor if mouse up was not over the slider
			if (_xSliderA.getHitRectangle().contains(devXGraph, devYMouse)
					|| _xSliderB.getHitRectangle().contains(devXGraph, devYMouse)) {
				if (getHorizontalBar().isVisible()) {
					setupScrollCursor(devXMouse, devYMouse);
				}
			}
		}
	}

	void onMouseWheel(final Event event) {

		if (_isGraphVisible == false) {
			return;
		}

		if (_chart.getMouseMode().equals(Chart.MOUSE_MODE_SLIDER)) {

			// mouse mode: move slider

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

			if ((event.stateMask & SWT.CONTROL) != 0 || (event.stateMask & SWT.SHIFT) != 0) {

				// scroll the chart

				int devXDiff = 0;
				if (event.count < 0) {
					devXDiff = -10;
				} else {
					devXDiff = 10;
				}

				if ((event.stateMask & SWT.SHIFT) != 0) {
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
	 * Paint event handler
	 * 
	 * @param gc
	 */
	private void paintChart(final GC gc) {

		final Rectangle clientArea = getClientArea();

		if (_drawingData == null || _drawingData.isEmpty()) {

			// fill the image area when there is no graphic
			gc.setBackground(_chart.getBackgroundColor());
			gc.fillRectangle(clientArea);

			return;
		}

		if (_isGraphDirty) {

			drawGraphImage();

			if (_isPaintDraggedImage) {

				/*
				 * paint dragged chart until the chart is recomputed
				 */
				paintDraggedChart(gc);
				return;
			}

			// prevent flickering the graph

			/*
			 * mac osx is still flickering, added the drawChartImage in version 1.0
			 */
			if (_graphImage != null) {

				final Image image = paintChartImage(gc);
				if (image == null) {
					return;
				}

				final int gcHeight = clientArea.height;
				final int imageHeight = image.getBounds().height;

				if (gcHeight > imageHeight) {

					// fill the gap between the image and the drawable area
					gc.setBackground(_chart.getBackgroundColor());
					gc.fillRectangle(0, imageHeight, clientArea.width, clientArea.height - imageHeight);

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
		if (_graphImage == null) {
			// fill the image area when there is no graphic
			gc.setBackground(_chart.getBackgroundColor());
			gc.fillRectangle(clientArea);
			gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
			return;
		}

		// calculate the scrollbars before the sliders are created
		updateHorizontalBar();

		drawCustomLayerImage();

		paintChartImage(gc);
	}

	private Image paintChartImage(final GC gc) {

		final boolean isLayerImageVisible = _isXSliderVisible
				|| _isYSliderVisible
				|| _isXMarkerMoved
				|| _isSelectionVisible;

		if (isLayerImageVisible) {
			drawLayerImage();
		}

		final Rectangle graphRect = _graphImage.getBounds();
		final ScrollBar hBar = getHorizontalBar();
		int imageScrollPosition = 0;

		if (graphRect.width < getDevVisibleChartWidth()) {

			// image is smaller than client area, the image is drawn in the top
			// left corner and the free are is painted with background color

			if (_isXSliderVisible && _layerImage != null) {
				hBar.setVisible(false);
				fillImagePadding(gc, _layerImage.getBounds());
			} else {
				fillImagePadding(gc, graphRect);
			}
		} else {
			if (hBar.isVisible()) {
				// move the image when the horizontal bar is visible
				imageScrollPosition = -hBar.getSelection();
			}
		}

		if (isLayerImageVisible) {
			if (_layerImage != null) {
				gc.drawImage(_layerImage, imageScrollPosition, 0);
			}
			return _layerImage;
		} else {
			if (_graphImage != null) {
				gc.drawImage(_graphImage, imageScrollPosition, 0);
			}
			return _graphImage;
		}
	}

	private void paintDraggedChart(final GC gc) {

		if (_draggedChartDraggedPos == null) {
			return;
		}

		final int devXDiff = _draggedChartDraggedPos.x - _draggedChartStartPos.x;
		final int devYDiff = 0;

		gc.setBackground(_chart.getBackgroundColor());

		final Rectangle clientArea = getClientArea();
		if (devXDiff > 0) {
			gc.fillRectangle(0, devYDiff, devXDiff, clientArea.height);
		} else {
			gc.fillRectangle(clientArea.width + devXDiff, devYDiff, -devXDiff, clientArea.height);
		}

		if (_cumstomLayerImage != null && _cumstomLayerImage.isDisposed() == false) {
			gc.drawImage(_cumstomLayerImage, devXDiff, devYDiff);
		} else if (_layerImage != null && _layerImage.isDisposed() == false) {
			gc.drawImage(_layerImage, devXDiff, devYDiff);
		} else if (_graphImage != null && _graphImage.isDisposed() == false) {
			gc.drawImage(_graphImage, devXDiff, devYDiff);
		}
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

		_isGraphDirty = true;
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

	private void scrollSmoothly(final int barSelection) {

		final Display display = Display.getCurrent();
		final int scrollInterval = 10;

		_smoothScrollEndPosition = Math.abs(barSelection);

		if (_isSmoothScrollingActive == false) {

			_isSmoothScrollingActive = true;

			/*
			 * start smooth scrolling after the chart is drawn, because the caller for this method
			 * is in the paint event
			 */
			display.asyncExec(new Runnable() {
				public void run() {
					display.timerExec(scrollInterval, new Runnable() {
						public void run() {
							scrollSmoothlyRunnable(this, scrollInterval);
						}
					});
				}
			});
		}
	}

	private void scrollSmoothlyRunnable(final Runnable runnable, final int scrollInterval) {

		final Display display = Display.getCurrent();
		final int scrollDiffMax = 5;

		if (isDisposed()) {
			_isSmoothScrollingActive = false;
			return;
		}

		final int scrollDiff = Math.abs(_smoothScrollEndPosition - _smoothScrollCurrentPosition);

		// start scrolling again if the position was not
		// reached
		if (scrollDiff > scrollDiffMax) {

			if (_smoothScrollCurrentPosition < _smoothScrollEndPosition) {
				_smoothScrollCurrentPosition += scrollDiffMax;
			} else {
				_smoothScrollCurrentPosition -= scrollDiffMax;
			}

			// scroll the graph
			_horizontalScrollBarPos = _smoothScrollCurrentPosition;
			redraw();

			display.timerExec(scrollInterval, runnable);

		} else {

			_isSmoothScrollingActive = false;
			_isScrollToSelection = false;

			/*
			 * the else part is being called in the initializing, I don't know why then the
			 * scrollDiff == 0
			 */
			if (scrollDiff != 0) {
				_scrollSmoothly = false;
			}
		}
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

		_scrollSmoothly = true;

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

		_scrollSmoothly = true;

		redrawBarSelection();

		return selectedIndex;
	}

	void setCanAutoMoveSlidersWhenZoomed(final boolean canMoveSlidersWhenZoomed) {
		this._canAutoMoveSliders = canMoveSlidersWhenZoomed;
	}

	/**
	 * @param canAutoZoomToSlider
	 *            the canAutoZoomToSlider to set
	 */
	void setCanAutoZoomToSlider(final boolean canAutoZoomToSlider) {

		this._canAutoZoomToSlider = canAutoZoomToSlider;

		/*
		 * an auto-zoomed chart can't be scrolled
		 */
		if (canAutoZoomToSlider) {
			this._canScrollZoomedChart = false;
		}
	}

	/**
	 * @param _canScrollZoomedChart
	 *            the canScrollZoomedChart to set
	 */
	void setCanScrollZoomedChart(final boolean canScrollZoomedGraph) {

		this._canScrollZoomedChart = canScrollZoomedGraph;

		/*
		 * a scrolled chart can't have the option to auto-zoom when the slider is dragged
		 */
		if (canScrollZoomedGraph) {
			this._canAutoZoomToSlider = false;
		}
	}

	/**
	 * Move a zoomed chart that the slider gets visible
	 * 
	 * @param slider
	 * @param centerSliderPosition
	 */
	private void setChartPosition(final ChartXSlider slider, final boolean centerSliderPosition) {

		if (_graphZoomRatio == 1) {
			// nothing to do
			return;
		}

		final int devSliderPos = slider.getDevVirtualSliderLinePos();

		final int devVisibleChartWidth = getDevVisibleChartWidth();
		float devXOffset = devSliderPos;

		if (centerSliderPosition) {

			devXOffset = devSliderPos - devVisibleChartWidth / 2;

		} else {

			/*
			 * check if the slider is in the visible area
			 */
			if (devSliderPos < _devGraphImageXOffset) {

				devXOffset = devSliderPos + 1;

			} else if (devSliderPos > _devGraphImageXOffset + devVisibleChartWidth) {

				devXOffset = devSliderPos - devVisibleChartWidth - 0;
			}
		}

		if (devXOffset != devSliderPos) {

			/*
			 * slider is not visible
			 */

			// check left border
			devXOffset = Math.max(devXOffset, 0);

			// check right border
			devXOffset = Math.min(devXOffset, _devVirtualGraphImageWidth - devVisibleChartWidth);

			_xOffsetZoomRatio = devXOffset / _devVirtualGraphImageWidth;

			/*
			 * reposition the mouse zoom position
			 */
			final float xOffsetMouse = _devGraphImageXOffset + devVisibleChartWidth / 2;
			_xOffsetMouseZoomInRatio = xOffsetMouse / _devVirtualGraphImageWidth;

			updateYDataMinMaxValues();

			/*
			 * prevent to display the old chart image
			 */
			_isGraphDirty = true;

			_chartComponents.onResize();
		}

		/*
		 * set position where the double click occured, this position will be used when the chart is
		 * zoomed
		 */
		_xOffsetMouseZoomInRatio = (float) devSliderPos / _devVirtualGraphImageWidth;
	}

	void setDefaultCursor() {

		final ChartDataModel chartDataModel = _chart.getChartDataModel();
		if (chartDataModel == null) {
			return;
		}

		final int chartType = chartDataModel.getChartType();

		if (chartType == ChartDataModel.CHART_TYPE_LINE || chartType == ChartDataModel.CHART_TYPE_LINE_WITH_BARS) {

			if (_chart.getMouseMode().equals(Chart.MOUSE_MODE_SLIDER)) {
				setCursor(_cursorModeSlider);
			} else {
//				if (fIsMoveMode) {
//					setCursor(fCursorModeZoomMove);
//				} else {
//					setCursor(fCursorModeZoom);
//				}
				setCursor(_cursorModeZoom);
			}
		} else {
			setCursor(null);
		}
	}

	/**
	 * sets a new configuration for the graph, the whole graph will be recreated
	 */
	void setDrawingData(final ArrayList<ChartDrawingData> drawingData) {

		// create empty list if list is not available, so we do not need
		// to check for null and isEmpty
		_drawingData = drawingData;

		_isGraphVisible = drawingData != null && drawingData.isEmpty() == false;

		// force all graphics to be recreated
		_isGraphDirty = true;
		_isSliderDirty = true;
		_isCustomLayerDirty = true;
		_isSelectionDirty = true;

		// hide previous tooltip
		hideToolTip();

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

	void setGraphImageWidth(final int devVirtualGraphImageWidth,
							final int devGraphImageXOffset,
							final float graphZoomRatio) {

		_devVirtualGraphImageWidth = devVirtualGraphImageWidth;
		_devGraphImageXOffset = devGraphImageXOffset;
		_graphZoomRatio = graphZoomRatio;

		_xSliderA.moveToDevPosition(devGraphImageXOffset, false, true);
		_xSliderB.moveToDevPosition(devVirtualGraphImageWidth, false, true);
	}

	void setSelectedBars(final boolean[] selectedItems) {

		if (selectedItems == null) {

			// set focus to first bar item

			if (_drawingData.size() == 0) {
				_selectedBarItems = null;
			} else {

				final ChartDrawingData chartDrawingData = _drawingData.get(0);
				final ChartDataXSerie xData = chartDrawingData.getXData();

				_selectedBarItems = new boolean[xData._highValues[0].length];
			}

		} else {

			_selectedBarItems = selectedItems;
		}

		_isScrollToSelection = true;
		_scrollSmoothly = true;

		_isSelectionVisible = true;

		redrawBarSelection();
	}

	/**
	 * Position the tooltip and ensure that it is not located off the screen.
	 */
	private void setToolTipPosition() {

		final Point cursorLocation = getDisplay().getCursorLocation();

		// Assuming cursor is 21x21 because this is the size of
		// the arrow cursor on Windows
		final int cursorHeight = 21;

		final Point tooltipSize = _toolTipShell.getSize();
		final Rectangle monitorRect = getMonitor().getBounds();
		final Point pt = new Point(cursorLocation.x, cursorLocation.y + cursorHeight + 2);

		pt.x = Math.max(pt.x, monitorRect.x);
		if (pt.x + tooltipSize.x > monitorRect.x + monitorRect.width) {
			pt.x = monitorRect.x + monitorRect.width - tooltipSize.x;
		}
		if (pt.y + tooltipSize.y > monitorRect.y + monitorRect.height) {
			pt.y = cursorLocation.y - 2 - tooltipSize.y;
		}

		_toolTipShell.setLocation(pt);
	}

	/**
	 * Set the scrolling cursor according to the vertical position of the mouse
	 * 
	 * @param devX
	 * @param devY
	 *            vertical coordinat of the mouse in the graph
	 */
	private void setupScrollCursor(final int devX, final int devY) {

		final int height = getDevVisibleGraphHeight();
		final int height4 = height / 4;
		final int height2 = height / 2;

		final float oldValue = _scrollAcceleration;

		_scrollAcceleration = devY < height4 ? 0.25f : devY < height2 ? 1 : devY > height - height4 ? 10 : 2;

		// set cursor according to the position
		if (_scrollAcceleration == 0.25) {
			setCursor(_cursorHand05x);
		} else if (_scrollAcceleration == 1) {
			setCursor(_cursorHand);
		} else if (_scrollAcceleration == 2) {
			setCursor(_cursorHand2x);
		} else {
			setCursor(_cursorHand5x);
		}

		/*
		 * when the acceleration has changed, the start positions for scrolling the graph must be
		 * set to the current location
		 */
		if (oldValue != _scrollAcceleration) {
			_startPosScrollbar = getHorizontalBar().getSelection();
			_startPosDev = devX;
		}
	}

	/**
	 * set the ratio at which the zoomed chart starts
	 */
	private void setXOffsetZoomRatio() {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		float devXOffset = _xOffsetMouseZoomInRatio * _devVirtualGraphImageWidth;
		devXOffset -= devVisibleChartWidth / 2;

		// adjust left border
		devXOffset = Math.max(devXOffset, 0);

		// adjust right border
		devXOffset = Math.min(devXOffset, _devVirtualGraphImageWidth - devVisibleChartWidth);

		_xOffsetZoomRatio = devXOffset / _devVirtualGraphImageWidth;
	}

	/**
	 * Set value index for a slider and move the slider to this position, slider will be made
	 * visible
	 * 
	 * @param slider
	 * @param valueIndex
	 * @param centerSliderPosition
	 */
	void setXSliderValueIndex(final ChartXSlider slider, int valueIndex, final boolean centerSliderPosition) {

		final ChartDataXSerie xData = getXData();

		if (xData == null) {
			return;
		}

		final int[] xValues = xData.getHighValues()[0];

		// adjust the slider index to the array bounds
		valueIndex = valueIndex < 0 ? 0 : valueIndex > (xValues.length - 1) ? xValues.length - 1 : valueIndex;

		slider.setValuesIndex(valueIndex);
		slider.setValueX(xValues[valueIndex]);

		final int linePos = (int) (_devVirtualGraphImageWidth * (float) xValues[valueIndex] / xValues[xValues.length - 1]);
		slider.moveToDevPosition(linePos, true, true);

		setChartPosition(slider, centerSliderPosition);

		_isSliderDirty = true;
	}

	/**
	 * makes the slider visible, a slider is only drawn into the chart if a slider was created with
	 * createSlider
	 * 
	 * @param isXSliderVisible
	 */
	void setXSliderVisible(final boolean isSliderVisible) {
		this._isXSliderVisible = isSliderVisible;
	}

	private void setZoomInPosition() {

		// get left+right slider
		final ChartXSlider leftSlider = getLeftSlider();
		final ChartXSlider rightSlider = getRightSlider();

		final int devLeftVirtualSliderLinePos = leftSlider.getDevVirtualSliderLinePos();

		final int devZoomInPosInChart = devLeftVirtualSliderLinePos
				+ ((rightSlider.getDevVirtualSliderLinePos() - devLeftVirtualSliderLinePos) / 2);

		_xOffsetMouseZoomInRatio = (float) devZoomInPosInChart / _devVirtualGraphImageWidth;
	}

	private void showToolTip(final int x, final int y) {

		if (_toolTipShell == null) {

			_toolTipShell = new Shell(getShell(), SWT.ON_TOP | SWT.TOOL);

			final Display display = _toolTipShell.getDisplay();
			final Color infoColorBackground = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
			final Color infoColorForeground = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

			_toolTipContainer = new Composite(_toolTipShell, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(2, 5, 2, 3).applyTo(_toolTipContainer);

			_toolTipContainer.setBackground(infoColorBackground);
			_toolTipContainer.setForeground(infoColorForeground);

			_toolTipTitle = new Label(_toolTipContainer, SWT.LEAD);
			_toolTipTitle.setBackground(infoColorBackground);
			_toolTipTitle.setForeground(infoColorForeground);
			_toolTipTitle.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

			_toolTipLabel = new Label(_toolTipContainer, SWT.LEAD | SWT.WRAP);
			_toolTipLabel.setBackground(infoColorBackground);
			_toolTipLabel.setForeground(infoColorForeground);

			for (final int toolTipEvent : _toolTipEvents) {
				addListener(toolTipEvent, _toolTipListener);
			}
		}

		if (updateToolTip(x, y)) {
			_toolTipShell.setVisible(true);
		} else {
			hideToolTip();
		}
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

		if (_drawingData.size() == 0) {
			return;
		}

		final ChartDrawingData chartDrawingData = _drawingData.get(0);
		if (chartDrawingData == null) {
			return;
		}

		final ChartDataXSerie data2nd = chartDrawingData.getXData2nd();

		if (data2nd == null) {
			return;
		}

		final int[] xValues = data2nd.getHighValues()[0];
		int valueIndex = slider.getValuesIndex();

		if (valueIndex >= xValues.length) {
			valueIndex = xValues.length - 1;
			slider.setValuesIndex(valueIndex);
		}

		try {
			slider.setValueX(xValues[valueIndex]);

			final int linePos = (int) (_devVirtualGraphImageWidth * (((float) xValues[valueIndex] / xValues[xValues.length - 1])));

			slider.moveToDevPosition(linePos, true, true);

		} catch (final ArrayIndexOutOfBoundsException e) {
			// ignore
		}
	}

	void updateChartLayers() {

		if (isDisposed()) {
			return;
		}

		_isCustomLayerDirty = true;
		_isSliderDirty = true;
		redraw();
	}

	private void updateDraggedChart(final int devXDiff) {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		float devXOffset = _devGraphImageXOffset - devXDiff;

		// adjust left border
		devXOffset = Math.max(devXOffset, 0);

		// adjust right border
		devXOffset = Math.min(devXOffset, _devVirtualGraphImageWidth - devVisibleChartWidth);

		_xOffsetZoomRatio = devXOffset / _devVirtualGraphImageWidth;

		/*
		 * reposition the mouse zoom position
		 */
		float xOffsetMouse = _xOffsetMouseZoomInRatio * _devVirtualGraphImageWidth;
		xOffsetMouse = xOffsetMouse - devXDiff;
		_xOffsetMouseZoomInRatio = xOffsetMouse / _devVirtualGraphImageWidth;

		updateYDataMinMaxValues();

		/*
		 * draw the dragged image until the graph image is recomuted
		 */
		_isPaintDraggedImage = true;

		_chartComponents.onResize();

		moveSlidersToBorder();
	}

	/**
	 * Setup for the horizontal scrollbar
	 */
	private void updateHorizontalBar() {

		final ScrollBar hBar = getHorizontalBar();

		if (_canScrollZoomedChart == false) {
			// the graph can't be scrolled, the scrollbar is hidden
			hBar.setVisible(false);
			hBar.setEnabled(false);
			return;
		}

		final int clientWidth = getDevVisibleChartWidth();

		// get current values of the scroll bar
		int hBarIncrement = hBar.getIncrement();
		int hBarPageIncrement = hBar.getPageIncrement();
		int hBarMaximum = hBar.getMaximum();
		boolean isHBarVisible = hBar.isVisible();
		int hBarSelection = hBar.getSelection();

		hBarIncrement = (clientWidth / 100);
		hBarPageIncrement = clientWidth;

		if (_devVirtualGraphImageWidth > clientWidth) {

			// chart image is wider than the client area, show the scrollbar

			hBarMaximum = _devVirtualGraphImageWidth;
			isHBarVisible = true;

			if (_isScrollToLeftSlider) {
				hBarSelection = Math
						.min(_xSliderA.getDevVirtualSliderLinePos(), _xSliderB.getDevVirtualSliderLinePos());
				hBarSelection -= (float) ((clientWidth * ZOOM_REDUCING_FACTOR) / 2.0);

				_isScrollToLeftSlider = false;

			} else if (_isScrollToSelection) {

				// scroll to the selected x-data

				if (_selectedBarItems != null) {

					for (int selectedIndex = 0; selectedIndex < _selectedBarItems.length; selectedIndex++) {

						if (_selectedBarItems[selectedIndex]) {

							// selected position was found
							final ChartDrawingData chartDrawingData = _drawingData.get(0);
							final int[] xValues = chartDrawingData.getXData()._highValues[0];

							final float xPosition = xValues[selectedIndex] * chartDrawingData.getScaleX();

							hBarSelection = (int) xPosition - (clientWidth / 2);

							break;
						}
					}

					if (_scrollSmoothly == false) {

						/*
						 * reset scroll to selection, this is only needed once when it's enable or
						 * when smooth scrolling is done
						 */
						_isScrollToSelection = false;
					}
				}
			}

		} else {

			isHBarVisible = false;
			hBarSelection = 0;
		}

		// at least one of the values changed - update all of them
		hBar.setIncrement(hBarIncrement);
		hBar.setPageIncrement(hBarPageIncrement);
		hBar.setMaximum(hBarMaximum);
		hBar.setThumb(clientWidth);
		hBar.setEnabled(isHBarVisible);
		hBar.setVisible(isHBarVisible);

		/*
		 * scroll bar selection (position of the graph) must be set AFTER the scrollbar is set
		 * visible/enabled
		 */
		if (_horizontalScrollBarPos >= 0) {
			hBar.setSelection(_horizontalScrollBarPos);
			_horizontalScrollBarPos = -1;
		} else {

			if (_scrollSmoothly) {

				scrollSmoothly(hBarSelection);

			} else {

				// initialize smooth scroll start position
				_smoothScrollCurrentPosition = hBarSelection;

				hBar.setSelection(hBarSelection);
			}
		}
	}

	void updateImageWidthAndOffset() {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		if (_canScrollZoomedChart) {

			// the image can be scrolled

			_devGraphImageXOffset = 0;

			_devVirtualGraphImageWidth = (int) (devVisibleChartWidth * _graphZoomRatio);

		} else {

			// calculate new virtual graph width
			_devVirtualGraphImageWidth = (int) (_graphZoomRatio * devVisibleChartWidth);

			if (_graphZoomRatio == 1.0) {
				// with the ration 1.0 the graph is not zoomed
				_devGraphImageXOffset = 0;
			} else {
				// the graph is zoomed, only a part is displayed which starts at
				// the offset for the left slider
				_devGraphImageXOffset = (int) (_xOffsetZoomRatio * _devVirtualGraphImageWidth);
			}
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
		for (final ChartDrawingData drawingData : _drawingData) {

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

	private boolean updateToolTip(final int x, final int y) {

		final ChartToolTipInfo tooltip = getToolTipInfo(x, y);

		if (tooltip == null) {
			return false;
		}

		if (tooltip.isDisplayed()) {

			// reposition the tool tip when necessary
			if (tooltip.isReposition() || isToolTipWrongPositioned()) {
				setToolTipPosition();
			}

			return true;
		}

		final String toolTipLabel = tooltip.getLabel();
		final String toolTipTitle = tooltip.getTitle();

		// check if the content has changed
		if (toolTipLabel.trim().equals(_toolTipLabel.getText().trim())
				&& toolTipTitle.trim().equals(_toolTipTitle.getText().trim())) {
			return true;
		}

		// title
		if (toolTipTitle != null) {
			_toolTipTitle.setText(toolTipTitle);
			_toolTipTitle.pack(true);
			_toolTipTitle.setVisible(true);
		} else {
			_toolTipTitle.setVisible(false);
		}

		// label
		_toolTipLabel.setText(toolTipLabel);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(_toolTipLabel);
		_toolTipLabel.pack(true);

		/*
		 * adjust width of the tooltip when it exeeds the maximum
		 */
		Point containerSize = _toolTipContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		if (containerSize.x > MAX_TOOLTIP_WIDTH) {

			GridDataFactory.fillDefaults().hint(MAX_TOOLTIP_WIDTH, SWT.DEFAULT).applyTo(_toolTipLabel);
			_toolTipLabel.pack(true);

			containerSize = _toolTipContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		}

		_toolTipContainer.setSize(containerSize);
		_toolTipShell.pack(true);

		/*
		 * On some platforms, there is a minimum size for a shell which may be greater than the
		 * label size. To avoid having the background of the tip shell showing around the label,
		 * force the label to fill the entire client area.
		 */
		final Rectangle area = _toolTipShell.getClientArea();
		_toolTipContainer.setSize(area.width, area.height);

		setToolTipPosition();

		return true;
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

			final int onTopWidth2 = onTopLabel.getWidth() / 2;
			final int onTopDevX = onTopLabel.getX();
			final int onBotWidth2 = onBotLabel.getWidth() / 2;
			final int onBotDevX = onBotLabel.getX();

			if (onTopDevX + onTopWidth2 > onBotDevX - onBotWidth2 && onTopDevX - onTopWidth2 < onBotDevX + onBotWidth2) {
				onBotLabel.setY(onBotLabel.getY() + onBotLabel.getHeight());
			}
			labelIndex++;
		}
	}

	/**
	 * sets the min/max values for the y-axis that the visible area will be filled with the chart
	 */
	void updateYDataMinMaxValues() {

		final ChartDataModel chartDataModel = _chartComponents.getChartDataModel();
		final ChartDataXSerie xData = chartDataModel.getXData();
		final ArrayList<ChartDataYSerie> yDataList = chartDataModel.getYData();

		if (xData == null) {
			return;
		}

		final int[][] xValueSerie = xData.getHighValues();

		if (xValueSerie.length == 0) {
			// data are not available
			return;
		}

		final int[] xValues = xValueSerie[0];
		final int serieLength = xValues.length;

		final int lastXValue = xValues[serieLength - 1];
		final int valueVisibleArea = (int) (lastXValue / _graphZoomRatio);

		final int valueLeftBorder = (int) (lastXValue * _xOffsetZoomRatio);
		int valueRightBorder = valueLeftBorder + valueVisibleArea;

		// make sure right is higher than left
		if (valueLeftBorder >= valueRightBorder) {
			valueRightBorder = valueLeftBorder + 1;
		}

		/*
		 * get value index for the left and right border of the visible area
		 */
		int valueIndexLeft = 0;
		for (final int xValue : xValues) {
			if (xValue > valueLeftBorder) {
				break;
			}
			valueIndexLeft++;
		}
		int valueIndexRight = valueIndexLeft;
		for (; valueIndexRight < xValues.length; valueIndexRight++) {
			if (xValues[valueIndexRight] > valueRightBorder) {
				break;
			}
		}

		/*
		 * get min/max value for each dataserie to fill the visible area with the chart
		 */
		for (final ChartDataYSerie yData : yDataList) {

			final int[][] yValueSeries = yData.getHighValues();
			final int yValues[] = yValueSeries[0];

			// ensure array bounds
			final int maxYValueIndex = yValues.length - 1;
			valueIndexLeft = Math.min(valueIndexLeft, maxYValueIndex);
			valueIndexLeft = Math.max(valueIndexLeft, 0);
			valueIndexRight = Math.min(valueIndexRight, maxYValueIndex);
			valueIndexRight = Math.max(valueIndexRight, 0);

			int minValue = yValues[valueIndexLeft];
			int maxValue = yValues[valueIndexLeft];

			for (final int[] yValueSerie : yValueSeries) {

				if (yValueSerie == null) {
					continue;
				}

				for (int valueIndex = valueIndexLeft; valueIndex < valueIndexRight; valueIndex++) {

					final int yValue = yValueSerie[valueIndex];

					if (yValue < minValue) {
						minValue = yValue;
					}
					if (yValue > maxValue) {
						maxValue = yValue;
					}
				}
			}

			if (yData.isForceMinValue() == false && minValue != 0) {
				yData.setVisibleMinValue(minValue - 1);
			}

			if (yData.isForceMaxValue() == false && maxValue != 0) {
				yData.setVisibleMaxValue(maxValue + 1);
			}
		}
	}

	void zoomInWithMouse() {

		if (_canScrollZoomedChart) {

			// the image can be scrolled

		} else {

			// chart can't be scrolled

			final int devVisibleChartWidth = getDevVisibleChartWidth();
			final int devMaxChartWidth = ChartComponents.CHART_MAX_WIDTH;

			if (_devVirtualGraphImageWidth <= devMaxChartWidth) {

				// chart is within the range which can be zoomed in

				final float zoomedInRatio = _graphZoomRatio * ZOOM_RATIO_FACTOR;
				final int devZoomedInWidth = (int) (devVisibleChartWidth * zoomedInRatio);

				if (devZoomedInWidth > devMaxChartWidth) {

					// the zoomed graph would be wider than the max width, reduce it to the max width
					_graphZoomRatio = (float) devMaxChartWidth / devVisibleChartWidth;
					_devVirtualGraphImageWidth = devMaxChartWidth;

				} else {

					_graphZoomRatio = zoomedInRatio;
					_devVirtualGraphImageWidth = devZoomedInWidth;
				}

				setXOffsetZoomRatio();
				handleChartResizeForSliders();

				updateYDataMinMaxValues();
				moveSlidersToBorder();

				_chartComponents.onResize();
			}
		}

		_chart.enableActions();
	}

	/**
	 * Zoom into the graph with the ratio {@link #ZOOM_RATIO_FACTOR}
	 */
	void zoomInWithoutSlider() {

		final int visibleGraphWidth = getDevVisibleChartWidth();
		final int graphImageWidth = _devVirtualGraphImageWidth;

		final int maxChartWidth = ChartComponents.CHART_MAX_WIDTH;

		if (graphImageWidth <= maxChartWidth) {

			// chart is within the range which can be zoomed in

			if (graphImageWidth * ZOOM_RATIO_FACTOR > maxChartWidth) {
				/*
				 * the double zoomed graph would be wider than the max width, reduce it to the max
				 * width
				 */
				_graphZoomRatio = maxChartWidth / visibleGraphWidth;
				_devVirtualGraphImageWidth = maxChartWidth;
			} else {
				_graphZoomRatio = _graphZoomRatio * ZOOM_RATIO_FACTOR;
				_devVirtualGraphImageWidth = (int) (graphImageWidth * ZOOM_RATIO_FACTOR);
			}

			_isScrollToSelection = true;

			/*
			 * scroll smoothly is started when a bar is selected, otherwise it is disabled
			 */
			_scrollSmoothly = false;

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
		final int devVirtualLeftSliderPos = leftSlider.getDevVirtualSliderLinePos();
		final int devVirtualRightSliderPos = rightSlider.getDevVirtualSliderLinePos();

		// difference between left and right slider
		final float devSliderDiff = devVirtualRightSliderPos - devVirtualLeftSliderPos - 0;

		if (devSliderDiff == 0) {
			// no difference between the slider
			_graphZoomRatio = 1;
			_devVirtualGraphImageWidth = devVisibleChartWidth;

			_isScrollToLeftSlider = false;

		} else {

			if (_canScrollZoomedChart) {

				// the image can be scrolled

				final int graphWidth = _graphZoomRatio == 1 ? devVisibleChartWidth : _devVirtualGraphImageWidth;

				_graphZoomRatio = (float) (graphWidth * (1 - ZOOM_REDUCING_FACTOR) / devSliderDiff);

				_isScrollToLeftSlider = true;

			} else {

				/*
				 * the graph image can't be scrolled, show only the zoomed part which is defined
				 * between the two sliders
				 */

				// calculate new graph ratio
				_graphZoomRatio = (_devVirtualGraphImageWidth) / (devSliderDiff);

				// adjust rounding problems
				_graphZoomRatio = (_graphZoomRatio * devVisibleChartWidth) / devVisibleChartWidth;

				// set the position (ratio) at which the zoomed chart starts
				_xOffsetZoomRatio = getLeftSlider().getPositionRatio();

				// set the center of the chart for the position when zooming with the mouse
				final float devVirtualWidth = _graphZoomRatio * devVisibleChartWidth;
				final float devXOffset = _xOffsetZoomRatio * devVirtualWidth;
				final int devCenterPos = (int) (devXOffset + devVisibleChartWidth / 2);
				_xOffsetMouseZoomInRatio = devCenterPos / devVirtualWidth;
			}
		}

		handleChartResizeForSliders();

		updateYDataMinMaxValues();

		_chart.enableActions();
	}

	/**
	 * Zooms out of the graph
	 */
	void zoomOutFitGraph() {

		// reset the data which influence the computed graph image width
		_graphZoomRatio = 1;
		_xOffsetZoomRatio = 0;

		_devVirtualGraphImageWidth = getDevVisibleChartWidth();
		_devGraphImageXOffset = 0;

		/*
		 * scroll smoothly is started when a bar is selected, otherwise it is disabled
		 */
		_scrollSmoothly = false;

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

		if (_canScrollZoomedChart) {

			// the image can be scrolled

			final int visibleGraphWidth = getDevVisibleChartWidth();
			final int visibleGraphHeight = getDevVisibleGraphHeight();

			// reset the data which influence the computed graph image width
			_graphZoomRatio = 1;
			_xOffsetZoomRatio = 0;

			_devVirtualGraphImageWidth = visibleGraphWidth;
			_devGraphImageXOffset = 0;

			/*
			 * scroll smoothly is started when a bar is selected, otherwise it is disabled
			 */
			_scrollSmoothly = false;

			// set the new graph width
			// enforceChartImageMinMaxWidth();

			// reposition the sliders
			_xSliderA.handleChartResize(visibleGraphHeight);
			_xSliderB.handleChartResize(visibleGraphHeight);

			if (updateChart) {
				_chartComponents.onResize();
			}

		} else {

			final int devVisibleChartWidth = getDevVisibleChartWidth();

			if (_graphZoomRatio > ZOOM_RATIO_FACTOR) {

				_graphZoomRatio = _graphZoomRatio / ZOOM_RATIO_FACTOR;
				_devVirtualGraphImageWidth = (int) (_graphZoomRatio * devVisibleChartWidth);

				setXOffsetZoomRatio();

				handleChartResizeForSliders();
				updateYDataMinMaxValues();

				if (updateChart) {
					_chartComponents.onResize();
				}

			} else {

				if (_graphZoomRatio != 1) {

					_graphZoomRatio = 1;
					_devVirtualGraphImageWidth = devVisibleChartWidth;

					setXOffsetZoomRatio();

					handleChartResizeForSliders();
					updateYDataMinMaxValues();

					if (updateChart) {
						_chartComponents.onResize();
					}
				}
			}

			moveSlidersToBorder();
		}

		_chart.enableActions();
	}

//	/**
//	 * zoom in where parts defines how the width of the graph will be splitted and position defines
//	 * which part is shown
//	 *
//	 * @param scrollSmoothly
//	 * @param ratio
//	 */
//	void zoomWithParts(final int parts, final float position, final boolean scrollSmoothly) {
//
//		canScrollZoomedChart = true;
//
//		final int devVisibleGraphWidth = getDevVisibleChartWidth();
//		final int graphWidth = Math.min(ChartComponents.CHART_MAX_WIDTH, devVisibleGraphWidth * parts);
//
//		// reduce the width so that more than one part will be visible in the
//		// clientarea
//		fDevVirtualGraphImageWidth = (int) (graphWidth * ZOOM_WITH_PARTS_RATIO);
//		fGraphZoomRatio = Math.max(1, (float) fDevVirtualGraphImageWidth / devVisibleGraphWidth);
//
//		final int partWidth = fDevVirtualGraphImageWidth / parts;
//		final int partBorder = devVisibleGraphWidth - partWidth;
//
//		// position the graph in the scrollbar
//		if (position == 0) {
//			fHorizontalScrollBarPos = 0;
//		} else {
//			fHorizontalScrollBarPos = (int) ((partWidth * position - 1) - (partBorder / 2));
//		}
//
//		graphZoomParts = parts;
//		graphZoomPartPosition = position;
//
//		fScrollSmoothly = scrollSmoothly;
//	}
}
