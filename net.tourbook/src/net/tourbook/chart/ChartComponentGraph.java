/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.ColorCache;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

/**
 * Draws the graph and axis into the canvas
 * 
 * @author Wolfgang Schramm
 */
public class ChartComponentGraph extends Canvas {

	private static final int			BAR_MARKER_WIDTH		= 16;
	/**
	 * the factor is multiplied whth the visible graph width, so that the
	 * sliders are indented from the border to be good visible
	 */
	private static final double			ZOOM_REDUCING_FACTOR	= 0.1;
	private static final double			ZOOM_WITH_PARTS_RATIO	= 0.8;

	final static NumberFormat			nf						= NumberFormat.getNumberInstance();

	Chart								fChart;
	private ChartComponents				fChartComponents;

	/*
	 * images for the chart and slider
	 */
	private Image						fGraphCoreImage;
	private Image						fGraphLayerImage;

	private Image						fCumstomLayerImage;

	private int							fHorizontalScrollBarPos;

	/**
	 * drawing data which is used to draw the chart
	 */
	private ArrayList<ChartDrawingData>	fDrawingData			= new ArrayList<ChartDrawingData>();

	/**
	 * defines in how many parts the graph is divided
	 */
	private int							graphZoomParts			= 1;
	private float						graphZoomPartPosition;

	/**
	 * zoom ratio between the visible and the virtual chart width
	 */
	private float						graphZoomRatio			= 1;

	/**
	 * when the graph is zoomed and <code>canScrollZoomedChart</code> is
	 * <code>true</code>, the graph can be wider than the visial part. This
	 * field contains the width for the image when the whole tour would be
	 * displayed and not only a part
	 */
	private int							devVirtualGraphImageWidth;

	/**
	 * when the zoomed graph can't be scrolled the chart image could be wider
	 * than the visible part, this field contains the offset for the start of
	 * the visible chart
	 */
	private int							devGraphImageXOffset;

	/**
	 * the zoomed chart can be scrolled when set to <code>true</code>, for a
	 * zoomed chart, the chart image can be wider than the visible part and can
	 * be scrolled
	 */
	boolean								canScrollZoomedChart;

	/**
	 * when the slider is dragged and the mouse up event occures, the graph is
	 * zoomed to the sliders when set to <code>true</code>
	 */
	boolean								canAutoZoomToSlider;

	/**
	 * true indicates the graph needs to be redrawn in the paint event
	 */
	private boolean						fIsGraphDirty;

	/**
	 * true indicates the slider needs to be redrawn in the paint event
	 */
	private boolean						fIsSliderDirty;

	/**
	 * when set to <code>true</code> the custom layers above the graph image
	 * needs a redraw in the next paint event
	 */
	private boolean						isCustomLayerDirty;

	/**
	 * set to <code>true</code> when the selection needs to be redrawn
	 */
	private boolean						fIsSelectionDirty;

	/**
	 * status for the x-slider, <code>true</code> indicates, the slider is
	 * visible
	 */
	private boolean						isXSliderVisible;

	/**
	 * true indicates that the y-sliders is visible
	 */
	private boolean						isYSliderVisible;

	/*
	 * chart slider
	 */
	private ChartXSlider				xSliderA;
	private ChartXSlider				xSliderB;

	/**
	 * xSliderDragged is set when the slider is being dragged, otherwise it is
	 * to <code>null</code>
	 */
	private ChartXSlider				xSliderDragged;

	/**
	 * This is the slider which is drawn on top of the other, this is normally
	 * the last dragged slider
	 */
	private ChartXSlider				xSliderOnTop;

	/**
	 * this is the slider which is below the top slider
	 */
	private ChartXSlider				xSliderOnBottom;

	/**
	 * contains the x-slider when the mouse is over it, or <code>null</code>
	 * when the mouse is not over it
	 */
	private ChartXSlider				fMouseOverXSlider;

	/**
	 * slider which has the focus
	 */
	private ChartXSlider				fSelectedXSlider;

	/**
	 * device position of the slider line when the slider is dragged
	 */
	private int							devXScrollSliderLine;

	/**
	 * list for all y-sliders
	 */
	protected ArrayList<ChartYSlider>	ySliders;

	/**
	 * scroll position for the horizontal bar after the graph was zoomed
	 */
	private boolean						scrollToLeftSlider;

	/**
	 * <code>true</code> to scroll to the x-data selection
	 */
	private boolean						fScrollToSelection;

	/**
	 * contextLeftSlider is set not null when the right mouse button was clicked
	 * and the left slider was hit
	 */
	private ChartXSlider				contextLeftSlider;

	/**
	 * contextRightSlider is set not null when the right mouse button was
	 * clicked and the right slider was hit
	 */
	private ChartXSlider				contextRightSlider;

	/**
	 * cursor when the graph can be resizes
	 */
	private Cursor						cursorResizeLeftRight;
	private Cursor						cursorResizeTopDown;
	private Cursor						cursorMoveXMarker;
	private Cursor						cursorHand05x;
	private Cursor						cursorHand;
	private Cursor						cursorHand2x;
	private Cursor						cursorHand5x;

	private static final RGB			gridRGB					= new RGB(241, 239, 226);

	private Color						gridColor;

	/**
	 * is set true when the graph is being moved with the mouse
	 */
	private boolean						isGraphScrolled;

	/**
	 * position where the graph scrolling started
	 */
	private int							startPosScrollbar;
	private int							startPosDev;

	private float						scrollAcceleration;

	/**
	 * offset when the chart is in autoscroll mode
	 */
	private int							autoScrollOffset;

	private boolean						fIsAutoScrollActive;

	private Shell						toolTipShell;
	private Label						toolTipLabel;
	private Listener					toolTipListener;
	private final int[]					toolTipEvents			= new int[] {
			SWT.MouseExit,
			SWT.MouseHover,
			SWT.MouseMove,
			SWT.MouseDown,
			SWT.DragDetect										};

	/**
	 * serie index for the hovered bar, when set to -1, the bar is hidden
	 */
	private int							fHoveredBarSerieIndex	= -1;
	private int							fHoveredBarValueIndex;
	private boolean						fIsHoveredBarDirty;

	private ChartYSlider				ySliderDragged;

	private int							ySliderGraphX;

	private ChartYSlider				hitYSlider;

	/**
	 * <code>true</code> when the x-marker is moved with the mouse
	 */
	private boolean						fIsXMarkerMoved;

	/**
	 * x-position when the x-marker was started to drag
	 */
	private int							fDevXMarkerDraggedStartPos;

	/**
	 * x-position when the x-marker is moved
	 */
	private int							fDevXMarkerDraggedPos;

	private int							fMovedXMarkerStartValueIndex;
	private int							fMovedXMarkerEndValueIndex;
	private int							fForcedXMarkerValueDiff;

	private boolean[]					fSelectedBarItems;

	private final int[]					fDrawCounter			= new int[1];

	private final ColorCache			fColorCache				= new ColorCache();

	private boolean						fIsSelectionVisible;
	private boolean						fIsFocusActive;
	private boolean						fScrollSmoothly;
	private int							fSmoothScrollEndPosition;
	protected boolean					fIsSmoothScrollingActive;
	protected int						fSmoothScrollCurrentPosition;

	int									fGraphAlpha				= 0xC0;

	/**
	 * Constructor
	 * 
	 * @param parent
	 *        the parent of this control.
	 * @param style
	 *        the style of this control.
	 */
	ChartComponentGraph(final Chart chartWidget, final Composite parent, final int style) {

		// create composite with horizontal scrollbars
		super(parent, SWT.H_SCROLL | SWT.NO_BACKGROUND);

		fChart = chartWidget;

		cursorResizeLeftRight = new Cursor(getDisplay(), SWT.CURSOR_SIZEWE);
		cursorResizeTopDown = new Cursor(getDisplay(), SWT.CURSOR_SIZENS);
		cursorMoveXMarker = new Cursor(getDisplay(), SWT.CURSOR_SIZEALL);

		cursorHand05x = createCursorFromImage("cursor-hand-05x.gif");
		cursorHand = createCursorFromImage("cursor-hand.gif");
		cursorHand2x = createCursorFromImage("cursor-hand-2x.gif");
		cursorHand5x = createCursorFromImage("cursor-hand-5x.gif");

		gridColor = new Color(getDisplay(), gridRGB);

		fChartComponents = (ChartComponents) parent;

		// setup the x-slider
		xSliderA = new ChartXSlider(this, Integer.MIN_VALUE, ChartXSlider.SLIDER_TYPE_LEFT);

		xSliderB = new ChartXSlider(this, Integer.MIN_VALUE, ChartXSlider.SLIDER_TYPE_RIGHT);

		xSliderOnTop = xSliderB;
		xSliderOnBottom = xSliderA;

		addListener();
		createContextMenu();
	}

	void adjustWidthToZoomMarker(	final int devChartImageWidth,
									final float graphZoomRatio,
									final int devGraphOffset) {

		this.graphZoomRatio = graphZoomRatio;
		devVirtualGraphImageWidth = devChartImageWidth;

		devGraphImageXOffset = devGraphOffset;

		xSliderA.moveToDevPosition(devGraphOffset + 0, false, true);
		xSliderB.moveToDevPosition(devChartImageWidth, false, true);
	}

	private void calculateSliderForContextMenu(final int devX, final int devY, final int graphX) {

		ChartXSlider slider1 = null;
		ChartXSlider slider2 = null;

		// reset the context slider
		contextLeftSlider = null;
		contextRightSlider = null;

		// check if a slider or the slider line was hit
		if (xSliderA.getHitRectangle().contains(graphX, devY)) {
			slider1 = xSliderA;
		}

		if (xSliderB.getHitRectangle().contains(graphX, devY)) {
			slider2 = xSliderB;
		}

		if (slider1 == null && slider2 == null) {
			// no slider was hit
			return;
		}

		if (slider1 != null && slider2 == null) {
			// only slider 1 was hit
			contextLeftSlider = slider1;
			return;
		}
		if (slider2 != null && slider1 == null) {
			// only slider 2 was hit
			contextLeftSlider = slider2;
			return;
		}

		// both sliders were hit

		final int xSlider1 = slider1.getHitRectangle().x;
		final int xSlider2 = slider2.getHitRectangle().x;

		if (xSlider1 == xSlider2) {
			// both sliders are at the same position
			contextLeftSlider = slider1;
			return;
		}
		if (xSlider1 < xSlider2) {
			contextLeftSlider = slider1;
			contextRightSlider = slider2;
		} else {
			contextLeftSlider = slider2;
			contextRightSlider = slider1;
		}
	}

	/**
	 */
	private void computeAutoScrollOffset() {

		final int visibleGraphWidth = getDevVisibleChartWidth();
		final ScrollBar hBar = getHorizontalBar();

		autoScrollOffset = 0;

		// no auto scrolling if the slider is within the client area
		if (devXScrollSliderLine >= 0 && devXScrollSliderLine < visibleGraphWidth) {
			return;
		}

		final int scrollScale = 1;

		if (devXScrollSliderLine < -1 && hBar.getSelection() > 0) {
			// graph can be scrolled to the left
			autoScrollOffset = devXScrollSliderLine * scrollScale;
		}
		if (devXScrollSliderLine > -1
				&& hBar.getSelection() < (hBar.getMaximum() - hBar.getThumb())) {
			// graph can be scrolled to the right
			autoScrollOffset = (devXScrollSliderLine - visibleGraphWidth) * scrollScale;
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
		 * get the marker positon to the next value
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
			// no data are available
			return;
		}

		final int[] xValues = xValueSerie[0];
		final int valuesLength = xValues.length;
		final int maxIndex = Math.max(0, valuesLength - 1);

		int valueIndex;
		int xValue;
		final int xAxisUnit = xData.getAxisUnit();

		if (xAxisUnit == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND) {

			/*
			 * For a linear x axis the slider value is also linear
			 */

			final float widthScale = (float) (maxIndex) / devVirtualGraphImageWidth;

			// ensure the index is not out of bounds
			valueIndex = (int) Math.max(0, Math.min(devXSliderLinePosition * widthScale, maxIndex));

			xValue = xValues[valueIndex];

		} else {

			/*
			 * The non time value (distance) is not linear, the value is
			 * increasing steadily but with different distance on the x axis. So
			 * first we have to find the nearest position in the values array
			 * and then interpolite from the found position to the slider
			 * position
			 */

			final int minValue = xData.getMinValue();
			final int maxValue = xData.getMaxValue();
			final int valueRange = maxValue > 0 ? (maxValue - minValue) : -(minValue - maxValue);

			final float posIndex = (float) devXSliderLinePosition / devVirtualGraphImageWidth;

			// enforce maxIndex
			valueIndex = (int) Math.min((posIndex * valuesLength), maxIndex);

			// sliderIndex points into the value array for the current slider
			// position
			xValue = xValues[valueIndex];

			// sliderValue contains the value for the slider on the x-axis
			final int sliderValue = (int) (posIndex * valueRange);

			if (xValue == sliderValue) {

				// nothing to do

			} else if (sliderValue > xValue) {

				/*
				 * in the value array move towards the end to find the position
				 * where the value of the slider corresponds with the value in
				 * the value array
				 */

				while (sliderValue > xValue) {

					xValue = xValues[valueIndex++];

					// check if end of the x-data are reached
					if (valueIndex == valuesLength) {
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
				fChart.fillMenu(menuMgr, contextLeftSlider, contextRightSlider);
			}
		});
		final Menu menu = menuMgr.createContextMenu(this);
		setMenu(menu);
	}

	/**
	 * Create a cursor resource from an image file
	 * 
	 * @param imageName
	 * @return
	 */
	private Cursor createCursorFromImage(final String imageName) {

		final Image cursorImage = TourbookPlugin.getImageDescriptor(imageName).createImage();

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

		final int devSliderLinePos = slider.getDevVirtualSliderLinePos()
				- getDevGraphImageXOffset();

		int sliderValuesIndex = slider.getValuesIndex();
		// final int valueX = slider.getValueX();

		final ArrayList<ChartXSliderLabel> labelList = new ArrayList<ChartXSliderLabel>();
		slider.setLabelList(labelList);

		final ScrollBar hBar = getHorizontalBar();
		final int hBarOffset = hBar.isVisible() ? hBar.getSelection() : 0;

		final int leftPos = hBarOffset;
		final int rightPos = leftPos + getDevVisibleChartWidth();

		// create slider label for each graph
		for (final ChartDrawingData drawingData : fDrawingData) {

			// final ChartDataXSerie xData = drawingData.getXData();
			final ChartDataYSerie yData = drawingData.getYData();
			final int valueDivisor = yData.getValueDivisor();
			if (valueDivisor == 0) {
				nf.setMinimumFractionDigits(0);
			} else if (valueDivisor == 10) {
				nf.setMinimumFractionDigits(1);
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
			// labelText.append(ChartUtil
			// .formatValue(valueX, xAxisUnit, xData.getValueDivisor(), true));
			// labelText.append(' ');
			// labelText.append(xData.getUnitLabel());
			// labelText.append(" ");

			labelText.append(nf.format((float) yValue / valueDivisor));
			labelText.append(' ');
			labelText.append(yData.getUnitLabel());
			labelText.append(' ');

			// calculate position of the slider label
			final Point labelExtend = gc.stringExtent(labelText.toString());
			final int labelWidth = labelExtend.x + 4;
			int labelXPos = devSliderLinePos - labelWidth / 2;

			final int labelRightPos = labelXPos + labelWidth;

			if (slider == xSliderDragged) {
				/*
				 * current slider is the dragged slider, clip the slider label
				 * position at the viewport
				 */
				if (labelXPos < leftPos) {
					labelXPos += (leftPos - labelXPos);
				} else if (labelRightPos >= rightPos) {
					labelXPos = rightPos - labelWidth - 1;
				}

			} else {
				/*
				 * current slider is not dragged, clip the slider label position
				 * at the chart bounds
				 */
				if (labelXPos < 0) {

					labelXPos = 0;

				} else {

					/*
					 * show the whole label when the slider is on the right
					 * border
					 */
					if (canScrollZoomedChart) {

						if (labelRightPos > devVirtualGraphImageWidth) {
							labelXPos = devVirtualGraphImageWidth - labelWidth - 1;
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

			label.setY(drawingData.getDevYBottom()
					- drawingData.getDevGraphHeight()
					- label.getHeight());

			/*
			 * get the y position of the marker which marks the y value in the
			 * graph
			 */
			int yGraph = drawingData.getDevYBottom()
					- (int) ((float) (yValue - drawingData.getGraphYBottom()) * drawingData
							.getScaleY())
					- 0;

			if (yValue < yData.getMinValue()) {
				yGraph = drawingData.getDevYBottom();
			}
			if (yValue > yData.getMaxValue()) {
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
			if (devXScrollSliderLine < 0) {
				moveXSlider(xSliderDragged, 0);
			} else {
				if (devXScrollSliderLine > visibleGraphWidth - 1) {
					moveXSlider(xSliderDragged, visibleGraphWidth - 1);
				}
			}
			// redraw slider
			fIsSliderDirty = true;

			redraw();

		} else {

			// scrollbar is visible, graph is wider than the client area

			// make sure the sliders are at the border of the visible area
			// before auto scrolling starts
			if (devXScrollSliderLine < 0) {
				moveXSlider(xSliderDragged, hBar.getSelection());
			} else {
				if (devXScrollSliderLine >= visibleGraphWidth - 1) {
					moveXSlider(xSliderDragged, hBar.getSelection() + visibleGraphWidth - 1);
				}
			}
			// redraw slider
			fIsSliderDirty = true;
			redraw();

			computeAutoScrollOffset();

			if (autoScrollOffset != 0) {

				// the graph can be scrolled

				// ensure that only one instance will run
				fIsAutoScrollActive = true;

				// start auto scrolling
				display.timerExec(autoScrollInterval, new Runnable() {

					public void run() {

						if (isDisposed() || xSliderDragged == null) {
							fIsAutoScrollActive = false;
							return;
						}

						// scroll the horizontal scroll bar
						final ScrollBar hBar = getHorizontalBar();

						computeAutoScrollOffset();

						hBar.setSelection(hBar.getSelection() + autoScrollOffset);

						// scroll the slider
						moveXSlider(xSliderDragged, xSliderDragged.getDevVirtualSliderLinePos()
								+ autoScrollOffset);

						// redraw slider
						fIsSliderDirty = true;
						redraw();

						// start scrolling again if the bounds have not been
						// reached
						if (autoScrollOffset != 0) {
							display.timerExec(autoScrollInterval, this);
						} else {
							fIsAutoScrollActive = false;
						}
					}
				});

			}
		}
	}

	/**
	 * Draws a bar graph, this requires that drawingData.getChartData2ndValues
	 * does not return null, if null is returned, a line graph will be drawn
	 * instead
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
		final int barPosition = drawingData.getBarPosition();

		// get the top/bottom of the graph
		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - drawingData.getDevGraphHeight();

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

		final int devBarWidth = Math.max(1, drawingData.getBarRectangleWidth());
		int devBarPos = drawingData.getBarRectanglePos();

		final int serieLayout = yData.getChartLayout();

		// loop: all data series
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			final int yHighValues[] = yHighSeries[serieIndex];
			int yLowValues[] = null;
			if (yLowSeries != null) {
				yLowValues = yLowSeries[serieIndex];
			}

			// reposition the rectangle when the bars are beside each other
			if (serieLayout == ChartDataYSerie.BAR_LAYOUT_BESIDE) {
				devBarPos += serieIndex * devBarWidth;
			}

			// loop: all values in the current serie
			for (int valueIndex = 0; valueIndex < valueLength; valueIndex++) {

				// get the x position
				final int devXPos = (int) (xValues[valueIndex] * scaleX) + devBarPos;

				final int devBarWidthSelected = devBarWidth;
				final int devBarWidth2 = devBarWidthSelected / 2;

				int devXPosSelected = devXPos;

				// center the bar
				if (devBarWidthSelected > 1 && barPosition == ChartDrawingData.BAR_POS_CENTER) {
					devXPosSelected -= devBarWidth2;
				}

				// get the bar height
				final int valueYLow = yLowValues == null
						? yData.getMinValue()
						: yLowValues[valueIndex];

				final int valueYHigh = yHighValues[valueIndex];

				final int barHeight = (Math.max(valueYHigh, valueYLow) - Math.min(
						valueYHigh,
						valueYLow));

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
					devYPos = devYBottom
							- ((int) ((valueYHigh - graphYBottom) * scaleY) + devYPreviousHeight);
				} else {
					devYPos = devYTop
							+ ((int) ((valueYLow - graphYBottom) * scaleY) + devYPreviousHeight);
				}

				final Rectangle barShape = new Rectangle(
						devXPos,
						devYPos,
						devBarWidth,
						devBarHeight);

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

				if (devBarWidth > 1) {

					gc.setForeground(colorBright);
					gc.fillGradientRectangle(
							barShape.x,
							barShape.y,
							barShape.width,
							barShape.height,
							false);

					gc.setForeground(colorLine);
					gc.drawRectangle(barShape);

				} else {

					gc.setForeground(colorLine);
					gc.drawLine(barShape.x, barShape.y, barShape.x, (barShape.y + barShape.height));
				}

				// save the bar position
				barRecangles[serieIndex][valueIndex] = barShape;
				barFocusRecangles[serieIndex][valueIndex] = new Rectangle(
						devXPos - 2,
						devYPos - 2,
						devBarWidth + 4,
						devBarHeight + 7);

				// keep the height for the bar
				devHeightSummary[valueIndex] += devBarHeight;
			}
		}
	}

	private void drawBarSelection(final GC gc, final ChartDrawingData drawingData) {

		// check if multiple bars are selected
		boolean drawSelection = false;
		int selectedIndex = 0;
		if (fSelectedBarItems != null) {
			int selectionIndex = 0;
			for (final boolean isBarSelected : fSelectedBarItems) {
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

		final int markerWidth = BAR_MARKER_WIDTH;
		final int markerWidth2 = markerWidth / 2;

		gc.setLineStyle(SWT.LINE_SOLID);

		// loop: all data series
		for (int serieIndex = 0; serieIndex < barRectangeleSeries.length; serieIndex++) {

			// get selected rectangle
			final Rectangle barRectangle = barRectangeleSeries[serieIndex][selectedIndex];

			if (barRectangle == null) {
				continue;
			}

			/*
			 * keep the selected bar because it will be drawn after all other
			 * bars are drawn
			 */
			final Rectangle barShapeSelected = new Rectangle(
					(barRectangle.x - markerWidth2),
					(barRectangle.y - markerWidth2),
					(barRectangle.width + markerWidth),
					(barRectangle.height + markerWidth));

			final int colorIndex = colorsIndex[serieIndex][selectedIndex];
			final RGB rgbBrightDef = rgbBright[colorIndex];
			final RGB rgbDarkDef = rgbDark[colorIndex];
			final RGB rgbLineDef = rgbLine[colorIndex];

			final Color colorBrightSelected = getColor(rgbBrightDef);
			final Color colorDarkSelected = getColor(rgbDarkDef);
			final Color colorLineSelected = getColor(rgbLineDef);

			/*
			 * bar was selected, draw the selected bar
			 */

			// do't write into the x-axis units which also contains the
			// selection marker
			if (barShapeSelected.y + barShapeSelected.height > devYBottom) {
				barShapeSelected.height = devYBottom - barShapeSelected.y;
			}

			// draw the selection darker when the focus is set
			if (fIsFocusActive) {
				gc.setAlpha(0xb0);
			} else {
				gc.setAlpha(0x70);
			}

			// fill bar background
			// gc.setForeground(colorBrightSelected);
			// gc.setBackground(colorDarkSelected);
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
			// if (fIsFocusActive) {
			// gc.drawFocus(
			// barShapeSelected.x,
			// barShapeSelected.y,
			// barShapeSelected.width,
			// barShapeSelected.height);
			// } else {

			gc.drawRoundRectangle(
					barShapeSelected.x,
					barShapeSelected.y,
					barShapeSelected.width,
					barShapeSelected.height,
					4,
					4);
			// }

			/*
			 * draw a marker below the x-axis to make the selection more visible
			 */
			if (fIsFocusActive) {

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
	/**
	 * Paint event handler
	 * 
	 * @param gc
	 */
	private void drawChart(final GC gc) {

		if (fDrawingData == null || fDrawingData.isEmpty()) {
			// fill the image area when there is no graphic
			gc.setBackground(fChart.getBackgroundColor());
			gc.fillRectangle(getClientArea());
			return;
		}

		if (fIsGraphDirty) {
			drawGraphImage();

			// prevent flickering the graph
			return;
		}

		// if the graph was not drawn (because this was done in another thread)
		// there is nothing to do
		if (fGraphCoreImage == null) {
			// fill the image area when there is no graphic
			gc.setBackground(fChart.getBackgroundColor());
			gc.fillRectangle(getClientArea());
			return;
		}

		// calculate the scrollbars before the sliders are created
		updateHorizontalBar();

		drawCustomLayers();

		final boolean isLayerImageVisible = isXSliderVisible
				|| isYSliderVisible
				|| fIsXMarkerMoved
				|| fIsSelectionVisible;

		if (isLayerImageVisible) {
			drawLayerImage();
		}

		final Rectangle graphRect = fGraphCoreImage.getBounds();
		final ScrollBar hBar = getHorizontalBar();
		int imageScrollPosition = 0;

		if (graphRect.width < getDevVisibleChartWidth()) {
			// image is smaller than client area, the image is drawn in the top
			// left corner and the free are is painted with background color

			if (isXSliderVisible) {
				hBar.setVisible(false);
				fillImagePadding(gc, fGraphLayerImage.getBounds());
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
			gc.drawImage(fGraphLayerImage, imageScrollPosition, 0);
		} else {
			gc.drawImage(fGraphCoreImage, imageScrollPosition, 0);
		}
	}

	private void drawCustomLayers() {

		// the layer above image is the same size as the graph image
		final Rectangle graphRect = fGraphCoreImage.getBounds();

		// when the existing image is the same size as the new image, we will
		// redraw it only if it's set to dirty
		if (isCustomLayerDirty == false && fCumstomLayerImage != null) {

			final Rectangle oldBounds = fCumstomLayerImage.getBounds();

			if (oldBounds.width == graphRect.width && oldBounds.height == graphRect.height) {
				return;
			}
		}

		if (ChartUtil.canReuseImage(fCumstomLayerImage, graphRect) == false) {
			fCumstomLayerImage = ChartUtil.createImage(getDisplay(), fCumstomLayerImage, graphRect);
		}

		final GC gc = new GC(fCumstomLayerImage);

		/*
		 * copy the graph image into the custom layer image, the custom layers
		 * are drawn on top of the graphs
		 */
		gc.fillRectangle(graphRect);
		gc.drawImage(fGraphCoreImage, 0, 0);

		for (final ChartDrawingData drawingData : fDrawingData) {
			for (final IChartLayer layer : drawingData.getYData().getCustomLayers()) {
				layer.draw(gc, drawingData, fChart);
			}
		}

		gc.dispose();

		isCustomLayerDirty = false;
	}

	/**
	 * draw the chart into the image
	 */
	private void drawGraphImage() {

		fDrawCounter[0]++;

		final Runnable imageThread = new Runnable() {

			final int	fCurrentDrawCounter	= fDrawCounter[0];

			public void run() {

				/*
				 * create the chart image only when a new onPaint event has not
				 * occured
				 */

				if (fCurrentDrawCounter != fDrawCounter[0]) {
					return;
				}

				if (isDisposed()) {
					return;
				}

				int devNonScrolledImageWidth = Math.max(
						ChartComponents.CHART_MIN_WIDTH,
						getDevVisibleChartWidth());

				int devImageWidth = canScrollZoomedChart
						? devVirtualGraphImageWidth
						: devNonScrolledImageWidth;

				/*
				 * the image size is adjusted to the client size but it must be
				 * within the min/max ranges
				 */
				final int devImageHeight = Math.max(ChartComponents.CHART_MIN_HEIGHT, Math.min(
						getVisibleGraphHeight(),
						ChartComponents.CHART_MAX_HEIGHT));

				/*
				 * when the image is the same size as the new we will redraw it
				 * only if it is set to dirty
				 */
				if (fIsGraphDirty == false && fGraphCoreImage != null) {

					Rectangle oldBounds = fGraphCoreImage.getBounds();

					if (oldBounds.width == devImageWidth && oldBounds.height == devImageHeight) {
						return;
					}
				}

				Display display = getDisplay();
				Rectangle imageRect = new Rectangle(0, 0, devImageWidth, devImageHeight);

				if (ChartUtil.canReuseImage(fGraphCoreImage, imageRect) == false) {

					// create image on which the graph is drawn
					fGraphCoreImage = ChartUtil.createImage(display, fGraphCoreImage, imageRect);
				}

				int chartType = fChart.getChartDataModel().getChartType();

				// create graphics context
				GC gc = new GC(fGraphCoreImage);

				gc.setFont(fChart.getFont());

				// fill background
				gc.setBackground(fChart.getBackgroundColor());
				gc.fillRectangle(fGraphCoreImage.getBounds());

				// draw all graphs
				int graphIndex = 0;

				// loop: all graphs
				for (ChartDrawingData drawingData : fDrawingData) {

					if (graphIndex == 0) {
						drawXTitle(gc, drawingData);
					}

					// draw units and grid on the x and y axis
					switch (chartType) {
					case ChartDataModel.CHART_TYPE_LINE:
						if (graphIndex == fDrawingData.size() - 1) {
							// draw the unit label and unit tick only on the
							// last graph
							drawXUnits(gc, drawingData, true, true);
						} else {
							drawXUnits(gc, drawingData, false, true);
						}
						drawHorizontalGridlines(gc, drawingData);
						drawLineGraph(gc, drawingData);
						break;

					case ChartDataModel.CHART_TYPE_BAR:
						if (graphIndex == fDrawingData.size() - 1) {
							// draw the unit label and unit tick only on the
							// last graph
							drawXUnits(gc, drawingData, true, true);
						} else {
							drawXUnits(gc, drawingData, false, true);
						}
						drawHorizontalGridlines(gc, drawingData);
						drawBarGraph(gc, drawingData);
						break;

					default:
						break;
					}

					graphIndex++;
				}

				gc.dispose();

				// force a redraw
				fIsGraphDirty = false;

				// fIsSliderDirty = true;
				// isLayerAboveDirty = true;
				// fIsSelectionDirty = true;

				redraw();
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

		final int yDevBottom = drawingData.getDevYBottom();

		final ArrayList<ChartUnit> unitList = drawingData.getYUnits();

		int unitCount = 0;
		final float scaleY = drawingData.getScaleY();
		final int graphYBottom = drawingData.getGraphYBottom();
		final int devGraphHeight = drawingData.getDevGraphHeight();
		final boolean yAxisDirection = drawingData.getYData().isYAxisDirection();

		final int yDevTop = yDevBottom - devGraphHeight;

		// loop: all units
		for (final ChartUnit unit : unitList) {

			int devY;
			if (yAxisDirection || (unitList.size() == 1)) {
				devY = yDevBottom - (int) ((float) (unit.value - graphYBottom) * scaleY);
			} else {
				devY = yDevTop + (int) ((float) (unit.value - graphYBottom) * scaleY);
			}

			if ((yAxisDirection == false && unitCount == unitList.size() - 1)
					|| (yAxisDirection && unitCount == 0)) {

				// draw x-axis

				gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
				gc.setLineStyle(SWT.LINE_SOLID);

			} else {

				// draw gridlines

				gc.setForeground(gridColor);
			}

			gc.drawLine(0, devY, devVirtualGraphImageWidth, devY);

			unitCount++;
		}
	}

	private void drawHoveredBar(final GC gc) {

		// check if hovered bar is disabled
		if (fHoveredBarSerieIndex == -1) {
			return;
		}

		// draw only bar chars
		if (fChart.getChartDataModel().getChartType() != ChartDataModel.CHART_TYPE_BAR) {
			return;
		}

		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setAlpha(0xd0);

		// loop: all graphs
		for (final ChartDrawingData drawingData : fDrawingData) {

			// get the chart data
			final ChartDataYSerie yData = drawingData.getYData();
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
				final Rectangle hoveredRectangle = barRectangeleSeries[serieIndex][fHoveredBarValueIndex];

				if (hoveredRectangle == null) {
					continue;
				}

				final int colorIndex = colorsIndex[serieIndex][fHoveredBarValueIndex];
				final RGB rgbBrightDef = rgbBright[colorIndex];
				final RGB rgbDarkDef = rgbDark[colorIndex];
				final RGB rgbLineDef = rgbLine[colorIndex];

				final Color colorBright = getColor(rgbBrightDef);
				final Color colorDark = getColor(rgbDarkDef);
				final Color colorLine = getColor(rgbLineDef);

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
	 * @return
	 */
	private void drawLayerImage() {

		// the slider image is the same size as the graph image
		final Rectangle graphRect = fCumstomLayerImage.getBounds();

		/*
		 * when the existing slider image has the same size as a new slider
		 * image, it will redrawn if it's set to dirty
		 */
		if (fIsSliderDirty == false
				&& fIsSelectionDirty == false
				&& fIsHoveredBarDirty == false
				&& fGraphLayerImage != null) {

			final Rectangle oldBounds = fGraphLayerImage.getBounds();
			if (oldBounds.width == graphRect.width && oldBounds.height == graphRect.height) {
				return;
			}
		}
		// System.out.println("draw layer");

		if (ChartUtil.canReuseImage(fGraphLayerImage, graphRect) == false) {
			fGraphLayerImage = ChartUtil.createImage(getDisplay(), fGraphLayerImage, graphRect);
		}

		if (fGraphLayerImage.isDisposed()) {
			return;
		}

		final GC gc = new GC(fGraphLayerImage);

		// copy the graph image into the slider image, the slider will be drawn
		// on top of the graph
		gc.fillRectangle(graphRect);
		gc.drawImage(fCumstomLayerImage, 0, 0);

		/*
		 * draw x/y-sliders
		 */
		if (isXSliderVisible) {
			createXSliderLabel(gc, xSliderOnTop);
			createXSliderLabel(gc, xSliderOnBottom);
			updateXSliderYPosition();

			drawXSlider(gc, xSliderOnBottom);
			drawXSlider(gc, xSliderOnTop);

		}
		if (isYSliderVisible) {
			drawYSliders(gc);
		}
		fIsSliderDirty = false;

		if (fIsXMarkerMoved) {
			drawXMarkerDragged(gc);
		}

		if (fIsSelectionVisible) {
			drawSelection(gc);
		}

		if (fIsHoveredBarDirty) {
			drawHoveredBar(gc);
			fIsHoveredBarDirty = false;
		}

		gc.dispose();

	}

	private void drawLineGraph(final GC gc, final ChartDrawingData drawingData) {

		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();
		final int xValues[] = xData.getHighValues()[0];
		final RGB rgbFg = yData.getRgbLine()[0];
		final RGB rgbBg1 = yData.getRgbDark()[0];
		final RGB rgbBg2 = yData.getRgbBright()[0];

		final float scaleX = drawingData.getScaleX();

		final ZoomMarkerPosition zoomMarkerPositionIn = fChartComponents.zoomMarkerPositionIn;
		int graphValueOffset;

		if (zoomMarkerPositionIn == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = (int) (Math.max(0, devGraphImageXOffset) / scaleX);
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = (int) (devGraphImageXOffset / scaleX);
		}

		if (xData.getXMarkerStartIndex() == -1) {
			drawLineGraphSegment(
					gc,
					drawingData,
					0,
					xValues.length,
					rgbFg,
					rgbBg1,
					rgbBg2,
					fGraphAlpha,
					graphValueOffset);

		} else {

			final int alphaXMarker = 0x80;

			// draw the marker
			drawLineGraphSegment(gc, drawingData, xData.getXMarkerStartIndex(), xData
					.getXMarkerEndIndex() + 1, rgbFg, rgbBg1, rgbBg2, fGraphAlpha, graphValueOffset);

			// draw segment before the marker
			drawLineGraphSegment(
					gc,
					drawingData,
					0,
					xData.getXMarkerStartIndex() + 1,
					rgbFg,
					rgbBg1,
					rgbBg2,
					alphaXMarker,
					graphValueOffset);

			// draw segment after the marker
			drawLineGraphSegment(
					gc,
					drawingData,
					xData.getXMarkerEndIndex() - 0,
					xValues.length,
					rgbFg,
					rgbBg1,
					rgbBg2,
					alphaXMarker,
					graphValueOffset);
		}
	}
	/**
	 * first we draw the graph into a path, the path is then drawn on the device
	 * with a transformation
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

		final Display display = getDisplay();
		final Path path = new Path(display);

		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();
		final int graphFillMethod = yData.getGraphFillMethod();

		final int xValues[] = xData.getHighValues()[0];
		final int yValues[] = yData.getHighValues()[0];

		final int graphYBottom = drawingData.getGraphYBottom();
		final int graphYTop = drawingData.getGraphYTop();

		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

		final boolean isXMarker = alphaValue == fGraphAlpha;

		// draw the lines into the path
		for (int xValueIndex = startIndex; xValueIndex < endIndex; xValueIndex++) {

			// make sure the x-index is not higher than the yValues length
			if (xValueIndex >= yValues.length) {
				return;
			}

			int xValue = xValues[xValueIndex];

			if (canScrollZoomedChart == false) {
				xValue -= graphValueOffset;
			}

			int yValue = yValues[xValueIndex];

			// force the bottom and top value not to drawn over the border
			if (yValue < graphYBottom) {
				yValue = graphYBottom;
			}
			if (yValue > graphYTop) {
				yValue = graphYTop;
			}

			if (xValueIndex == startIndex) {

				// move to the first point

				if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM) {

					// start from the bottom of the chart
					path.moveTo(xValue, graphYBottom);

				} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

					final int lineBottom = graphYBottom > 0 ? graphYBottom : graphYTop < 0
							? graphYTop
							: 0;
					// start from the x-axis
					path.moveTo(xValue, lineBottom);
				}

			}

			// draw the line to the first point
			path.lineTo(xValue, yValue);

			if ((graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM || graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO)
					&& (xValueIndex == endIndex - 1)) {
				/*
				 * this is the last point for a filled graph, draw the line to
				 * the x-axis
				 */

				int lineBottom;
				
				if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM) {
					lineBottom = graphYBottom > 0 ? graphYBottom : graphYTop < 0
							? graphYTop
							: graphYBottom;

				} else {

					// case: graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO

					lineBottom = graphYBottom > 0 ? graphYBottom : graphYTop < 0
							? graphYTop
							: 0;
				}

				path.lineTo(xValue, lineBottom);
				path.moveTo(xValue, lineBottom);
			}
		}

		final Color colorFg = new Color(display, rgbFg);
		final Color colorBg1 = new Color(display, rgbBg1);
		final Color colorBg2 = new Color(display, rgbBg2);

		// create transformation to transform the chart data into device
		// coordinates
		final Transform tr = new Transform(display);

		tr.translate(0, (float) drawingData.getDevYBottom() + (scaleY * graphYBottom));
		tr.scale(scaleX, -scaleY);

		// draw graph with transformation
		// gc.setLineStyle(SWT.LINE_SOLID);
		gc.setTransform(tr);

		gc.setForeground(colorBg1);
		gc.setBackground(colorBg2);
		gc.setAlpha(alphaValue);

		// fill the graph
		if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM) {

			int width = xValues[endIndex - 1];
			final int height = (int) ((float) drawingData.getDevGraphHeight() / scaleY);

			if (canScrollZoomedChart == false) {
				width -= graphValueOffset;
			}

			/*
			 * adjust the fill gradient in the hight, otherwise the fill is not
			 * in the whole rectangle
			 */
			gc.setClipping(path);
			gc.fillGradientRectangle(0, graphYBottom - 1, width, height + 1, true);
			gc.setClipping((Rectangle) null);

		} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

			int width = xValues[endIndex - 1];
			final int height = (int) ((float) drawingData.getDevGraphHeight() / scaleY);

			if (canScrollZoomedChart == false) {
				width -= graphValueOffset;
			}

			gc.setClipping(path);

			final int fillY = graphYBottom > 0 ? graphYBottom : 0;
			final int fillHeight = height + graphYBottom;

			gc.fillGradientRectangle(0, fillY, width, fillHeight, true);

			gc.setForeground(colorBg2);
			gc.setBackground(colorBg1);
			gc.fillGradientRectangle(0, graphYBottom, width, -graphYBottom, true);

			gc.setClipping((Rectangle) null);
		}

		gc.setForeground(colorFg);
		// gc.setAlpha(0xb0);
		// gc.setAntialias(SWT.ON);

		// if (isXMarker) {
		// gc.setAlpha(0x60);
		// } else {
		// // gc.setAlpha(alphaValue);
		// }

		gc.setLineStyle(SWT.LINE_SOLID);
		gc.drawPath(path);

		colorFg.dispose();
		colorBg1.dispose();
		colorBg2.dispose();

		path.dispose();
		tr.dispose();

		gc.setAlpha(0xFF);
		gc.setTransform(null);
	}

	private void drawSelection(final GC gc) {

		fIsSelectionDirty = false;

		final int chartType = fChart.getChartDataModel().getChartType();

		// loop: all graphs
		for (final ChartDrawingData drawingData : fDrawingData) {
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

	private void drawXMarkerDragged(final GC gc) {

		final Display display = getDisplay();
		final Color colorXMarker = new Color(display, 255, 153, 0);

		final int devDraggingDiff = fDevXMarkerDraggedPos - fDevXMarkerDraggedStartPos;

		// draw slider for each graph
		for (final ChartDrawingData drawingData : fDrawingData) {

			final int graphTop = drawingData.getDevYBottom() - drawingData.getDevGraphHeight();
			final int graphBottom = drawingData.getDevYBottom();

			final ChartDataXSerie xData = drawingData.getXData();
			final float scaleX = drawingData.getScaleX();
			final int valueDraggingDiff = (int) ((float) devDraggingDiff / scaleX);

			final int xmStartIndex = xData.getXMarkerStartIndex();
			final int xmEndIndex = xData.getXMarkerEndIndex();

			final int[] xValues = xData.getHighValues()[0];
			final int valueXStart = xValues[xmStartIndex];
			final int valueXEnd = xValues[xmEndIndex];
			// fForcedXMarkerValueDiff = valueXEnd - valueXStart;

			final int devXStart = (int) (scaleX * valueXStart - devGraphImageXOffset);
			final int devXEnd = (int) (scaleX * valueXEnd - devGraphImageXOffset);
			int devMovedXStart = devXStart;
			int devMovedXEnd = devXEnd;

			final int valueXStartWithOffset = valueXStart + valueDraggingDiff;
			final int valueXEndWithOffset = valueXEnd + valueDraggingDiff;

			fMovedXMarkerStartValueIndex = computeXMarkerValue(
					xValues,
					xmStartIndex,
					valueDraggingDiff,
					valueXStartWithOffset);

			devMovedXStart = (int) (scaleX * xValues[fMovedXMarkerStartValueIndex] - devGraphImageXOffset);

			fMovedXMarkerEndValueIndex = computeXMarkerValue(
					xValues,
					xmEndIndex,
					valueDraggingDiff,
					valueXEndWithOffset);

			devMovedXEnd = (int) (scaleX * xValues[fMovedXMarkerEndValueIndex] - devGraphImageXOffset);

			/*
			 * when the moved x-marker is on the right or the left border, make
			 * sure that the x-markers don't get too small
			 */
			final int valueMovedDiff = xValues[fMovedXMarkerEndValueIndex]
					- xValues[fMovedXMarkerStartValueIndex];

			if (fMovedXMarkerStartValueIndex == 0 && valueMovedDiff < fForcedXMarkerValueDiff) {

				/*
				 * the x-marker is moved to the left so the most left x-marker
				 * is on the first position
				 */

				int valueIndex;

				for (valueIndex = 0; valueIndex < xValues.length; valueIndex++) {
					if (xValues[valueIndex] >= fForcedXMarkerValueDiff) {
						break;
					}
				}

				fMovedXMarkerEndValueIndex = valueIndex;

			} else if (fMovedXMarkerEndValueIndex == xValues.length - 1
					&& valueMovedDiff < fForcedXMarkerValueDiff) {

				/*
				 * the x-marker is moved to the right so the most right x-marker
				 * is on the last position
				 */

				int valueIndex;
				final int valueFirstIndex = xValues[xValues.length - 1] - fForcedXMarkerValueDiff;

				for (valueIndex = xValues.length - 1; valueIndex >= 0; valueIndex--) {
					if (xValues[valueIndex] <= valueFirstIndex) {
						break;
					}
				}

				fMovedXMarkerStartValueIndex = valueIndex;
			}

			if (valueMovedDiff > fForcedXMarkerValueDiff) {

				/*
				 * force the value diff for the x-marker, the moved value diff
				 * can't be wider then one value index
				 */

				final int valueStart = xValues[fMovedXMarkerStartValueIndex];
				int valueIndex;
				for (valueIndex = fMovedXMarkerEndValueIndex - 0; valueIndex >= 0; valueIndex--) {
					if (xValues[valueIndex] - valueStart < fForcedXMarkerValueDiff) {
						valueIndex++;
						break;
					}
				}
				valueIndex = Math.min(valueIndex, xValues.length - 1);

				// System.out.println("old: "
				// + fMovedXMarkerEndValueIndex
				// + " new: "
				// + valueIndex);

				fMovedXMarkerEndValueIndex = valueIndex;
			}

			fMovedXMarkerEndValueIndex = Math.min(fMovedXMarkerEndValueIndex, xValues.length - 1);

			devMovedXStart = (int) (scaleX * xValues[fMovedXMarkerStartValueIndex] - devGraphImageXOffset);
			devMovedXEnd = (int) (scaleX * xValues[fMovedXMarkerEndValueIndex] - devGraphImageXOffset);

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

			gc.setAlpha(0xFF);
		}

		colorXMarker.dispose();
	}

	/**
	 * @param gc
	 * @param slider
	 */
	private void drawXSlider(final GC gc, final ChartXSlider slider) {

		final Display display = getDisplay();

		final int devSliderLinePos = slider.getDevVirtualSliderLinePos()
				- getDevGraphImageXOffset();

		final int grayColorIndex = 60;
		final Color colorTxt = new Color(display, grayColorIndex, grayColorIndex, grayColorIndex);

		int labelIndex = 0;

		final ArrayList<ChartXSliderLabel> labelList = slider.getLabelList();

		// draw slider for each graph
		for (final ChartDrawingData drawingData : fDrawingData) {

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
			final boolean isSliderHovered = fMouseOverXSlider != null
					&& fMouseOverXSlider == slider;

			/*
			 * when the mouse is over the slider, the slider is drawn in darker
			 * color
			 */
			// draw slider line
			if ((fIsFocusActive && fSelectedXSlider == slider) || isSliderHovered) {
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
			if (slider == getLeftSlider()) {
				// left slider
				gc.setBackground(colorDark);
				gc.setForeground(colorBright);
			} else {
				// right slider
				gc.setBackground(colorBright);
				gc.setForeground(colorDark);
			}

			// draw label background
			gc.fillGradientRectangle(labelX + 1, labelY, labelWidth - 1, labelHeight, false);

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
			if (fIsFocusActive && slider == fSelectedXSlider) {

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

		final String title = drawingData.getXTitle();

		if (title == null || title.length() == 0) {
			return;
		}

		final int devYTitle = drawingData.getDevMarginTop();

		final int devGraphWidth = canScrollZoomedChart
				? drawingData.getDevGraphWidth()
				: fChartComponents.getDevVisibleChartWidth();

		final Point titleExtend = gc.textExtent(title);

		gc.drawText(title, (devGraphWidth / 2) - (titleExtend.x / 2), (devYTitle),// +
				// (titleExtend.y
				// /
				// 2)),
				true);
	}

	/**
	 * Draw the unit label, tick and the vertical grid line for the x axis
	 * 
	 * @param gc
	 * @param drawingData
	 * @param drawUnit
	 *        <code>true</code> indicate to draws the unit tick and unit label
	 *        additional to the unit grid line
	 * @param draw0Unit
	 *        <code>true</code> indicate to draw the unit at the 0 position
	 */
	private void drawXUnits(final GC gc,
							final ChartDrawingData drawingData,
							final boolean drawUnit,
							final boolean draw0Unit) {

		final Display display = getDisplay();

		final ArrayList<ChartUnit> units = drawingData.getXUnits();

		final int devY = drawingData.getDevYBottom();
		final int unitPos = drawingData.getXUnitTextPos();
		final float scaleX = drawingData.getScaleX();

		// compute the distance between two units
		final int devUnitWidth = (int) (units.size() > 1 ? ((units.get(1).value * scaleX) - (units
				.get(0).value * scaleX)) : 0);

		int devXOffset = 0;
		int unitOffset = 0;
		int unitCounter = 0;

		if (canScrollZoomedChart == false && devGraphImageXOffset > 0) {
			// calculate the unit offset
			unitOffset = devGraphImageXOffset / devUnitWidth;
			devXOffset -= devGraphImageXOffset % devUnitWidth;
		}

		for (final ChartUnit unit : units) {

			/*
			 * skipt the units which are not displayed
			 */
			if (unitCounter < unitOffset) {
				devXOffset -= devUnitWidth;
				unitCounter++;
				continue;
			}

			// dev x-position for the unit tick
			final int devXUnitTick = devXOffset + (int) (unit.value * scaleX);

			final Point unitTextExtend = gc.textExtent(unit.label);

			// the first unit is not drawn because it would be clipped at the
			// left border of the chart canvas
			if (unitCounter > 0 || (unitCounter == 0 && draw0Unit)) {

				if (drawUnit) {

					gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));

					// draw the unit tick
					if (unitCounter > 0) {
						gc.setLineStyle(SWT.LINE_SOLID);
						gc.drawLine(devXUnitTick, devY, devXUnitTick, devY + 5);
					}

					// draw the unit text
					if (devUnitWidth != 0 && unitPos == ChartDrawingData.XUNIT_TEXT_POS_CENTER) {

						// draw the unit text between two units

						final int devXUnitCentered = Math.max(
								0,
								((devUnitWidth - unitTextExtend.x) / 2) + 0);

						gc.drawText(unit.label, devXUnitTick + devXUnitCentered, devY + 7, true);

					} else {

						// draw the unit text in the middle of the unit tick

						if (unitCounter == 0) {
							// do not center the unit text
							gc.drawText(unit.label, devXUnitTick, devY + 7, true);
						} else {
							// center the unit text
							gc.drawText(
									unit.label,
									devXUnitTick - (unitTextExtend.x / 2),
									devY + 7,
									true);
						}
					}
				}

				// draw the vertical gridline
				if (unitCounter > 0) {

					gc.setForeground(gridColor);
					gc.drawLine(devXUnitTick, devY, devXUnitTick, devY
							- drawingData.getDevGraphHeight());
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

		for (final ChartYSlider ySlider : ySliders) {

			if (hitYSlider == ySlider) {

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

				final int devYValue = (int) ((devYBottom - devYSliderLine) / drawingData
						.getScaleY())
						+ drawingData.getGraphYBottom();

				// create the slider text
				labelText.append(ChartUtil.formatValue(devYValue, yData.getAxisUnit(), yData
						.getValueDivisor(), true));
				labelText.append(' ');
				labelText.append(yData.getUnitLabel());
				labelText.append("  ");
				final String label = labelText.toString();

				final Point labelExtend = gc.stringExtent(label);

				final int labelHeight = labelExtend.y - 2;
				final int labelWidth = labelExtend.x + 0;
				final int labelX = ySliderGraphX - labelWidth - 5;
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

	void enforceChartImageMinMaxWidth() {

		if (graphZoomParts != 1) {

			zoomWithParts(graphZoomParts, graphZoomPartPosition);

		} else {

			final int devVisibleChartWidth = getDevVisibleChartWidth();

			final int devImageWidth = (int) (graphZoomRatio * devVisibleChartWidth);
			final int chartMinWidth = fChart.getChartDataModel().getChartMinWidth();

			if (canScrollZoomedChart) {

				// enforce min/max width for the chart
				final int devMinWidth = Math.max(
						Math.max(devVisibleChartWidth, chartMinWidth),
						ChartComponents.CHART_MIN_WIDTH);

				final int devMaxWidth = Math.min(devImageWidth, ChartComponents.CHART_MAX_WIDTH);

				devVirtualGraphImageWidth = Math.max(devMinWidth, devMaxWidth);

			} else {

				// enforce min width for the chart
				final int devMinWidth = Math.max(
						Math.max(devVisibleChartWidth, chartMinWidth),
						ChartComponents.CHART_MIN_WIDTH);

				devVirtualGraphImageWidth = Math.max(devMinWidth, devImageWidth);
			}
		}
	}

	/**
	 * Fills the surrounding area of an rectangle with background color
	 * 
	 * @param gc
	 * @param imageRect
	 */
	private void fillImagePadding(final GC gc, final Rectangle imageRect) {

		final int clientHeight = getVisibleGraphHeight();
		final int visibleGraphWidth = getDevVisibleChartWidth();

		gc.setBackground(fChart.getBackgroundColor());

		gc.fillRectangle(imageRect.width, 0, visibleGraphWidth, clientHeight);
		gc.fillRectangle(0, imageRect.height, visibleGraphWidth, clientHeight);
	}

	/**
	 * @param rgb
	 * @return Returns the color from the color cache, the color must not be
	 *         disposed this is done when the cache is disposed
	 */
	private Color getColor(final RGB rgb) {

		final String colorKey = rgb.toString();

		final Color color = fColorCache.get(colorKey);

		if (color == null) {
			return fColorCache.put(colorKey, rgb);
		} else {
			return color;
		}
	}

	/**
	 * @return
	 */
	int getDevGraphImageXOffset() {
		return devGraphImageXOffset;
	}

	/**
	 * @return the devVirtualGraphImageWidth
	 */
	int getDevVirtualGraphImageWidth() {
		return devVirtualGraphImageWidth;
	}

	/**
	 * @return Returns the visible width of the chart graph
	 */
	private int getDevVisibleChartWidth() {
		return fChartComponents.getDevVisibleChartWidth() - 0;
	}

	/**
	 * @return Returns the left slider
	 */
	ChartXSlider getLeftSlider() {
		return xSliderA.getDevVirtualSliderLinePos() < xSliderB.getDevVirtualSliderLinePos()
				? xSliderA
				: xSliderB;
	}

	/**
	 * @return Returns the right most slider
	 */
	ChartXSlider getRightSlider() {
		return xSliderA.getDevVirtualSliderLinePos() < xSliderB.getDevVirtualSliderLinePos()
				? xSliderB
				: xSliderA;
	}

	private String getToolTipText(final int x, final int y) {

		if (fHoveredBarSerieIndex != -1) {

			// get the method which computes the bar info
			final IChartInfoProvider computeBarInfo = (IChartInfoProvider) fChart
					.getChartDataModel()
					.getCustomData(ChartDataModel.BAR_INFO_PROVIDER);

			if (computeBarInfo != null) {
				return computeBarInfo.getInfo(fHoveredBarSerieIndex, fHoveredBarValueIndex);
			}
		}

		return null;
	}

	/**
	 * @return Returns the visible height of the chart graph
	 */
	private int getVisibleGraphHeight() {
		return fChartComponents.getDevVisibleChartHeight();
	}
	/**
	 * Returns the size of the graph for the given bounds, the size will be
	 * reduced when the scrollbars are visible
	 * 
	 * @param bounds
	 *        is the size of the receiver where the chart can be drawn
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
		final int y = (canScrollZoomedChart && devVirtualGraphImageWidth > width)
				? (height - horizontalBarHeight)
				: height;

		return new Point(x, y);
	}

	/**
	 * @return Returns the x-Data in the drawing data list
	 */
	private ChartDataXSerie getXData() {
		if (fDrawingData.size() == 0) {
			return null;
		} else {
			return fDrawingData.get(0).getXData();
		}
	}

	private ChartDrawingData getXDrawingData() {
		return fDrawingData.get(0);
	}

	void hideToolTip() {

		if (toolTipShell == null || toolTipShell.isDisposed()) {
			return;
		}

		toolTipShell.setVisible(false);
	}

	/**
	 * hookup all listeners
	 */
	private void addListener() {

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent event) {
				drawChart(event.gc);
			}
		});

		// horizontal scrollbar
		final ScrollBar horizontalBar = getHorizontalBar();
		horizontalBar.setEnabled(false);
		horizontalBar.setVisible(false);
		horizontalBar.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				onScroll(event);
			}
		});

		addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(final MouseEvent e) {
				if (fDrawingData != null) {
					onMouseMove(e);
				}
			}
		});

		addMouseListener(new MouseListener() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (fDrawingData != null) {
					onMouseDoubleClick(e);
				}
			}
			public void mouseDown(final MouseEvent e) {
				if (fDrawingData != null) {
					onMouseDown(e);
				}
			}
			public void mouseUp(final MouseEvent e) {
				if (fDrawingData != null) {
					onMouseUp(e);
				}
			}
		});

		addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(final MouseEvent e) {
			// forceFocus();
			}

			public void mouseExit(final MouseEvent e) {
				if (fDrawingData != null) {
					onMouseExit(e);
				}
			}
			public void mouseHover(final MouseEvent e) {}
		});

		addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(final Event event) {

				if (event.count < 0) {
					event.keyCode = SWT.ARROW_RIGHT;
				} else {
					event.keyCode = SWT.ARROW_LEFT;
				}
				fChartComponents.handleLeftRightEvent(event);
				/*
				 * prevent scrolling the scrollbar, scrolling is done by the
				 * chart itself
				 */
				event.doit = false;

			}
		});

		addFocusListener(new FocusListener() {

			public void focusGained(final FocusEvent e) {

				// System.out.println("component graph: focusGained");

				setFocusToControl();

				fIsFocusActive = true;
				fIsSelectionDirty = true;
				redraw();
			}

			public void focusLost(final FocusEvent e) {
				fIsFocusActive = false;
				fIsSelectionDirty = true;
				redraw();
			}
		});

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		toolTipListener = new Listener() {
			public void handleEvent(final Event event) {
				switch (event.type) {
				case SWT.MouseHover:
				case SWT.MouseMove:
					if (updateToolTip(event.x, event.y))
						break;
					// FALL THROUGH
				case SWT.MouseExit:
				case SWT.MouseDown:
					hideToolTip();
					break;
				}
			}
		};
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
		for (final ChartDrawingData drawingData : fDrawingData) {

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

						// showBarInfo = true;

						// keep the hovered bar index
						fHoveredBarSerieIndex = serieIndex;
						fHoveredBarValueIndex = valueIndex;

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

			if (fHoveredBarSerieIndex != -1) {

				/*
				 * hide last hovered bar, because the last hovered bar is
				 * visible
				 */

				// set status: no bar is hovered
				fHoveredBarSerieIndex = -1;

				// force redraw
				isBarHit = true;
			}
		}

		return isBarHit;
	}

	private boolean isXMarkerHit(final int devXGraph) {

		final ChartDataXSerie xData = getXData();
		final int xMarkerStartIndex = xData.getXMarkerStartIndex();
		final int xMarkerEndIndex = xData.getXMarkerEndIndex();

		if (xMarkerStartIndex == -1) {
			// x-marker is not set
			return false;
		}

		final int[] xValues = xData.getHighValues()[0];
		final float scaleX = getXDrawingData().getScaleX();

		final int devXMarkerStart = (int) (xValues[xMarkerStartIndex] * scaleX - devGraphImageXOffset);
		final int devXMarkerEnd = (int) (xValues[xMarkerEndIndex] * scaleX - devGraphImageXOffset);

		if (devXGraph >= devXMarkerStart && devXGraph <= devXMarkerEnd) {
			return true;
		}

		return false;
	}

	private ChartXSlider isXSliderHit(final int devYMouse, final int devXGraph) {

		ChartXSlider xSlider = null;

		if (xSliderA.getHitRectangle().contains(devXGraph, devYMouse)) {
			xSlider = xSliderA;
		} else if (xSliderB.getHitRectangle().contains(devXGraph, devYMouse)) {
			xSlider = xSliderB;
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

		if (ySliders == null) {
			return null;
		}

		for (final ChartYSlider ySlider : ySliders) {
			if (ySlider.getHitRectangle().contains(graphX, devY)) {
				hitYSlider = ySlider;
				return ySlider;
			}
		}

		if (hitYSlider != null) {

			// redraw the sliders to hide the labels
			hitYSlider = null;
			fIsSliderDirty = true;
			redraw();
		}

		return null;
	}

	void moveToNextPart() {
		if (graphZoomPartPosition < graphZoomParts - 1) {
			graphZoomPartPosition++;
			zoomWithParts(graphZoomParts, graphZoomPartPosition);
			redraw();
		}
	}

	void moveToPrevPart() {
		if (graphZoomPartPosition > 0) {
			graphZoomPartPosition--;
			zoomWithParts(graphZoomParts, graphZoomPartPosition);
			redraw();
		}
	}

	/**
	 * move the x-slider to the next right or left position in the x-data
	 * 
	 * @param event
	 */
	void moveXSlider(final Event event) {

		if (fSelectedXSlider == null) {
			final ChartXSlider leftSlider = getLeftSlider();
			if (leftSlider != null) {
				// set default slider
				fSelectedXSlider = leftSlider;
			} else {
				return;
			}
			;
		}

		if ((event.stateMask & SWT.SHIFT) != 0 && (event.stateMask & SWT.CONTROL) == 0) {
			// select the other slider
			fSelectedXSlider = fSelectedXSlider == xSliderA ? xSliderB : xSliderA;
			fIsSliderDirty = true;
			redraw();
			return;
		}

		int valueIndex = fSelectedXSlider.getValuesIndex();
		final int[] xValues = getXData().getHighValues()[0];

		int sliderDiff = (event.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
		sliderDiff *= (event.stateMask & SWT.SHIFT) != 0 ? 5 : 1;
		switch (event.keyCode) {
		case SWT.ARROW_RIGHT:

			valueIndex += sliderDiff;

			if (valueIndex >= xValues.length) {
				valueIndex = 0;
			}
			break;

		case SWT.ARROW_LEFT:

			valueIndex -= sliderDiff;

			if (valueIndex < 0) {
				valueIndex = xValues.length - 1;
			}

			break;
		}

		setXSliderValueIndex(fSelectedXSlider, valueIndex);

		// hide the cursor
		setCursor(null);

		redraw();
	}

	/**
	 * Move the slider to a new position
	 * 
	 * @param xSlider
	 *        Current slider
	 * @param devSliderLinePos
	 *        x coordinate for the slider line within the graph, this can be
	 *        outside of the visible graph
	 */
	private void moveXSlider(final ChartXSlider xSlider, int devSliderLinePos) {

		devSliderLinePos += devGraphImageXOffset;

		/*
		 * adjust the line position the the min/max width of the graph image
		 */
		devSliderLinePos = Math.min(devVirtualGraphImageWidth, Math.max(0, devSliderLinePos));

		computeXSliderValue(xSlider, devSliderLinePos);

		// set new slider line position
		xSlider.moveToDevPosition(devSliderLinePos, true, true);
	}

	private void moveYSlider(final ChartYSlider ySlider, final int graphX, final int devY) {

		final int devYSliderLine = devY
				- ySlider.getDevYClickOffset()
				+ ChartYSlider.halfSliderHitLineHeight;

		ySlider.setDevYSliderLine(graphX, devYSliderLine);
	}

	/**
	 * Dispose event handler
	 */
	private void onDispose() {

		// dispose resources
		cursorResizeLeftRight = ChartUtil.disposeResource(cursorResizeLeftRight);
		cursorResizeTopDown = ChartUtil.disposeResource(cursorResizeTopDown);
		cursorMoveXMarker = ChartUtil.disposeResource(cursorMoveXMarker);
		cursorHand05x = ChartUtil.disposeResource(cursorHand05x);
		cursorHand = ChartUtil.disposeResource(cursorHand);
		cursorHand2x = ChartUtil.disposeResource(cursorHand2x);
		cursorHand5x = ChartUtil.disposeResource(cursorHand5x);

		fGraphCoreImage = ChartUtil.disposeResource(fGraphCoreImage);
		fGraphLayerImage = ChartUtil.disposeResource(fGraphLayerImage);
		fCumstomLayerImage = ChartUtil.disposeResource(fCumstomLayerImage);

		gridColor = ChartUtil.disposeResource(gridColor);

		// dispose tooltip
		if (toolTipShell != null) {
			hideToolTip();
			for (int i = 0; i < toolTipEvents.length; i++) {
				removeListener(toolTipEvents[i], toolTipListener);
			}
			toolTipShell.dispose();
			toolTipShell = null;
			toolTipLabel = null;
		}

		fColorCache.dispose();
	}

	private void onMouseDoubleClick(final MouseEvent e) {

		if (fHoveredBarSerieIndex != -1) {

			// execute the action which is defined when a bar is selected
			// with the left mouse button

			fChart.fireChartDoubleClick(fHoveredBarSerieIndex, fHoveredBarValueIndex);
		}
	}

	/**
	 * Mouse down event handler
	 * 
	 * @param event
	 */
	private void onMouseDown(final MouseEvent event) {

		if (event.button != 1) {
			return;
		}

		final ScrollBar hBar = getHorizontalBar();
		final int hBarOffset = hBar.isVisible() ? hBar.getSelection() : 0;

		final int devXMouse = event.x;
		final int devYMouse = event.y;
		final int devXGraph = hBarOffset + devXMouse;

		// check if a x-slider was hit
		xSliderDragged = null;
		if (xSliderA.getHitRectangle().contains(devXGraph, devYMouse)) {
			xSliderDragged = xSliderA;
		} else if (xSliderB.getHitRectangle().contains(devXGraph, devYMouse)) {
			xSliderDragged = xSliderB;
		}

		if (xSliderDragged != null) {

			// x-slider was hit and can now be dragged on a mouse move event

			xSliderOnTop = xSliderDragged;
			xSliderOnBottom = xSliderOnTop == xSliderA ? xSliderB : xSliderA;

			// set the hit offset for the mouse click
			xSliderDragged.setDevXClickOffset(devXGraph - xSliderDragged.getHitRectangle().x);

			// the hit x-slider is now the selected x-slider
			fSelectedXSlider = xSliderDragged;
			fIsSelectionVisible = true;
			fIsSliderDirty = true;
			redraw();

		} else {

			// a x-slider wasn't dragged

			// check if a y-slider was hit
			ySliderDragged = isYSliderHit(devXGraph, devYMouse);

			if (ySliderDragged != null) {

				// y-slider was hit
				ySliderDragged.setDevYClickOffset(devYMouse - ySliderDragged.getHitRectangle().y);

			} else if (fHoveredBarSerieIndex != -1) {

				// execute the action which is defined when a bar is selected
				// with the left mouse button

				boolean[] selectedBarItems;

				if (fDrawingData.size() == 0) {
					selectedBarItems = null;
				} else {

					final ChartDrawingData chartDrawingData = fDrawingData.get(0);
					final ChartDataXSerie xData = chartDrawingData.getXData();

					selectedBarItems = new boolean[xData.fHighValues[0].length];
					selectedBarItems[fHoveredBarValueIndex] = true;
				}

				setSelectedBars(selectedBarItems);

				fChart.fireBarSelectionEvent(fHoveredBarSerieIndex, fHoveredBarValueIndex);

			} else if (hBar.isVisible()) {

				// start scrolling the graph if the scrollbar is visible

				isGraphScrolled = true;

				startPosScrollbar = hBarOffset;
				startPosDev = devXMouse;

				setupScrollCursor(devXMouse, devYMouse);

			} else {

				/*
				 * start to move the x-marker, when a dragging listener and the
				 * x-marker is set
				 */
				if (fChart.fXMarkerDraggingListener != null && isXMarkerHit(devXGraph)) {

					fIsXMarkerMoved = getXData().getXMarkerStartIndex() != -1;

					if (fIsXMarkerMoved) {

						fDevXMarkerDraggedStartPos = devXMouse;
						fDevXMarkerDraggedPos = devXMouse;
						fForcedXMarkerValueDiff = fChart.fXMarkerDraggingListener
								.getXMarkerValueDiff();

						fIsSliderDirty = true;
						redraw();
					}
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

		if (isGraphScrolled) {

			isGraphScrolled = false;

		} else if (xSliderDragged == null) {

			// hide the y-slider labels
			if (hitYSlider != null) {
				hitYSlider = null;

				fIsSliderDirty = true;
				redraw();
			}
		}

		if (fMouseOverXSlider != null) {
			// mouse left the x-slider
			fMouseOverXSlider = null;
			fIsSliderDirty = true;
			redraw();
		}

		setCursor(null);
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

		// System.out.println(event.stateMask+" "+event.x+" "+event.y);
		boolean isChartDirty = false;

		if (isGraphScrolled) {

			// graph is scrolled by the mouse

			final int scrollDiff = devXMouse - startPosDev;
			final int scrollPos = (int) (startPosScrollbar - (scrollDiff * scrollAcceleration));

			setupScrollCursor(devXMouse, devYMouse);

			// adjust the scroll position if the mouse is moved outside the
			// bounds
			if (scrollPos < 0) {
				startPosScrollbar = hBarOffset;
				startPosDev = devXMouse;
			} else {

				final int maxSelection = hBar.getMaximum() - hBar.getThumb();

				if (scrollPos > maxSelection) {
					startPosScrollbar = hBarOffset;
					startPosDev = devXMouse;
				}
			}

			hBar.setSelection(scrollPos);

			redraw();

		} else if (isXSliderVisible && xSliderDragged != null) {

			// x-slider is dragged

			// keep position of the slider line
			devXScrollSliderLine = devXMouse
					- xSliderDragged.getDevXClickOffset()
					+ ChartXSlider.halfSliderHitLineHeight;

			if (canScrollZoomedChart) {

				// the graph can be scrolled

				if (devXScrollSliderLine > -1 && devXScrollSliderLine < getDevVisibleChartWidth()) {

					// slider is within the visible area, no autoscrolling is
					// done

					moveXSlider(xSliderDragged, devXGraph
							- xSliderDragged.getDevXClickOffset()
							+ ChartXSlider.halfSliderHitLineHeight);

					fIsSliderDirty = true;
					isChartDirty = true;

				} else {

					// slider is outside the visible area, auto scroll the
					// slider and graph when this is not yet done
					if (fIsAutoScrollActive == false) {
						doAutoScroll();
					}
				}

			} else {

				// the graph can't be scrolled

				moveXSlider(
						xSliderDragged,
						(devXGraph - xSliderDragged.getDevXClickOffset() + ChartXSlider.halfSliderHitLineHeight));

				fIsSliderDirty = true;
				isChartDirty = true;
			}

		} else if (isYSliderVisible && ySliderDragged != null) {

			// y-slider is dragged

			moveYSlider(ySliderDragged, devXGraph, devYMouse);
			ySliderGraphX = devXGraph;

			fIsSliderDirty = true;
			isChartDirty = true;

		} else if (fIsXMarkerMoved) {

			fDevXMarkerDraggedPos = devXGraph;

			fIsSliderDirty = true;
			isChartDirty = true;

		} else {

			// set the cursor shape depending on the mouse location

			ChartXSlider xSlider;

			if (isXSliderVisible && (xSlider = isXSliderHit(devYMouse, devXGraph)) != null) {

				// mouse is over an x-slider

				if (fMouseOverXSlider != xSlider) {

					// a new x-slider is hovered

					fMouseOverXSlider = xSlider;

					// set cursor
					setCursor(cursorResizeLeftRight);

					// hide the y-slider
					hitYSlider = null;

					fIsSliderDirty = true;
					isChartDirty = true;
				}

			} else if (fMouseOverXSlider != null) {

				// mouse left the x-slider

				fMouseOverXSlider = null;
				fIsSliderDirty = true;
				isChartDirty = true;

			} else if (isYSliderVisible && isYSliderHit(devXGraph, devYMouse) != null) {

				// cursor is within a y-slider

				setCursor(cursorResizeTopDown);

				// show the y-slider labels
				ySliderGraphX = devXGraph;

				fIsSliderDirty = true;
				isChartDirty = true;

			} else if (fChart.fXMarkerDraggingListener != null && isXMarkerHit(devXGraph)) {

				setCursor(cursorMoveXMarker);

			} else {

				if (isBarHit(devYMouse, devXGraph)) {

					fIsHoveredBarDirty = true;
					isChartDirty = true;

					setCursor(null);

				} else if (hBar.isVisible()) {
					// horizontal bar is visible, show the scroll cursor
					setupScrollCursor(devXMouse, devYMouse);
				} else {
					// hide the cursor
					setCursor(null);
				}

			}
		}

		if (isChartDirty) {
			redraw();
		}
	}

	/**
	 * Mouse up event handler
	 * 
	 * @param event
	 */
	private void onMouseUp(final MouseEvent event) {

		final ScrollBar hBar = getHorizontalBar();
		final int hBarOffset = hBar.isVisible() ? hBar.getSelection() : 0;

		final int devX = event.x;
		final int devY = event.y;
		final int graphX = hBarOffset + devX;

		if (isGraphScrolled) {
			isGraphScrolled = false;
		} else {

			if (xSliderDragged != null) {

				// stop dragging the slider
				xSliderDragged = null;

				if (canScrollZoomedChart == false && canAutoZoomToSlider) {

					// the graph can't be scrolled but the graph should be
					// zoomed to the x-slider positions

					// zoom into the chart
					getDisplay().asyncExec(new Runnable() {
						public void run() {
							if (!isDisposed()) {
								fChart.zoomInWithSlider();
							}
						}
					});

				}

			} else if (ySliderDragged != null)

				adjustYSlider();

			else if (fIsXMarkerMoved) {

				fIsXMarkerMoved = false;

				fIsSliderDirty = true;
				redraw();

				// call the listener which is registered for dragged x-marker
				if (fChart.fXMarkerDraggingListener != null) {
					fChart.fXMarkerDraggingListener.xMarkerMoved(
							fMovedXMarkerStartValueIndex,
							fMovedXMarkerEndValueIndex);
				}
			}

			// show scroll cursor if mouse up was not over the slider
			if (xSliderA.getHitRectangle().contains(graphX, devY)
					|| xSliderB.getHitRectangle().contains(graphX, devY)) {
				if (getHorizontalBar().isVisible()) {
					setupScrollCursor(devX, devY);
				}
			}
		}

		if (event.button == 3) {
			calculateSliderForContextMenu(devX, devY, graphX);
		}
	}

	private void adjustYSlider() {

		/*
		 * check if the y slider was outside of the bounds, recompute the chart
		 * when necessary
		 */

		final ChartDrawingData drawingData = ySliderDragged.getDrawingData();

		final ChartDataYSerie yData = ySliderDragged.getYData();
		final ChartYSlider slider1 = yData.getYSliderTop();
		final ChartYSlider slider2 = yData.getYSliderBottom();

		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - drawingData.getDevGraphHeight();
		final int graphYBottom = drawingData.getGraphYBottom();
		final float scaleY = drawingData.getScaleY();

		final int graphValue1 = (int) ((devYBottom - slider1.getDevYSliderLine()) / scaleY)
				+ graphYBottom;

		final int graphValue2 = (int) ((devYBottom - slider2.getDevYSliderLine()) / scaleY)
				+ graphYBottom;

		int minValue;
		int maxValue;

		if (graphValue1 < graphValue2) {

			minValue = graphValue1;
			maxValue = graphValue2;

			yData.setMinValue(minValue);
			yData.setMaxValue(maxValue);

			// position the lower slider to the bottom of the chart
			slider1.setDevYSliderLine(devYBottom);
			slider2.setDevYSliderLine(devYTop);

		} else {

			// graphValue1 >= graphValue2

			minValue = graphValue2;
			maxValue = graphValue1;

			yData.setMinValue(minValue);
			yData.setMaxValue(maxValue);

			// position the upper slider to the top of the chart
			slider1.setDevYSliderLine(devYTop);
			slider2.setDevYSliderLine(devYBottom);
		}

		ySliderDragged = null;

		// the cursour could be outside of the chart, reset it
		setCursor(null);

		/*
		 * the hited slider could be outsite of the chart, hide the labels on
		 * the slider
		 */
		hitYSlider = null;

		/*
		 * when the zoom in marker is set, it would overwrite the changed
		 * y-slider position, so we overwrite the zoom in marker y-slider
		 * positions until the zoom in marker is overwritten
		 */
		if (fChartComponents.zoomMarkerPositionIn != null) {

			final ChartYDataMinMaxKeeper zoomMarkerPositionIn = fChartComponents.zoomMarkerPositionIn.yDataMinMaxKeeper;

			// get the id for the changed y-slider
			final Integer yDataInfo = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);

			// adjust min value for the changed y-slider
			final Integer inMinValue = zoomMarkerPositionIn.getMinValues().get(yDataInfo);

			if (inMinValue != null) {
				zoomMarkerPositionIn.getMinValues().put(yDataInfo, minValue);
			}

			// adjust max value for the changed y-slider
			final Integer inMaxValue = zoomMarkerPositionIn.getMaxValues().get(yDataInfo);

			if (inMaxValue != null) {
				zoomMarkerPositionIn.getMaxValues().put(yDataInfo, maxValue);
			}
		}

		// the chart was modified, recompute all
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!isDisposed()) {
					// fUpdateImageOffset = false;
					fChartComponents.onResize();
				}
			}
		});
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
		fIsSelectionDirty = true;
		redraw();
	}

	public void redrawChart() {
		if (isDisposed()) {
			return;
		}
		fIsGraphDirty = true;
		redraw();
	}

	/**
	 * select the next bar item
	 */
	int selectNextBarItem() {

		int selectedIndex = Chart.NO_BAR_SELECTION;

		if (fSelectedBarItems == null || fSelectedBarItems.length == 0) {
			return selectedIndex;
		}

		// find selected Index, reset last selected bar item(s)
		for (int index = 0; index < fSelectedBarItems.length; index++) {
			if (selectedIndex == Chart.NO_BAR_SELECTION && fSelectedBarItems[index]) {
				selectedIndex = index;
			}
			fSelectedBarItems[index] = false;
		}

		if (selectedIndex == Chart.NO_BAR_SELECTION) {

			// a bar item is not selected, select first
			selectedIndex = 0;

		} else {

			// select next bar item

			if (selectedIndex == fSelectedBarItems.length - 1) {
				/*
				 * last bar item is currently selected, select the first bar
				 * item
				 */
				selectedIndex = 0;
			} else {
				// select next bar item
				selectedIndex++;
			}
		}

		fSelectedBarItems[selectedIndex] = true;

		fScrollSmoothly = true;

		redrawBarSelection();

		return selectedIndex;
	}

	/**
	 * select the previous bar item
	 */
	int selectPreviousBarItem() {

		int selectedIndex = Chart.NO_BAR_SELECTION;

		// make sure that selectable bar items are available
		if (fSelectedBarItems == null || fSelectedBarItems.length == 0) {
			return selectedIndex;
		}

		// find selected item, reset last selected bar item(s)
		for (int index = 0; index < fSelectedBarItems.length; index++) {
			// get the first selected item if there are many selected
			if (selectedIndex == -1 && fSelectedBarItems[index]) {
				selectedIndex = index;
			}
			fSelectedBarItems[index] = false;
		}

		if (selectedIndex == Chart.NO_BAR_SELECTION) {

			// a bar item is not selected, select first
			selectedIndex = 0;

		} else {

			// select next bar item

			if (selectedIndex == 0) {
				/*
				 * first bar item is currently selected, select the last bar
				 * item
				 */
				selectedIndex = fSelectedBarItems.length - 1;
			} else {
				// select previous bar item
				selectedIndex = selectedIndex - 1;
			}
		}

		fSelectedBarItems[selectedIndex] = true;

		fScrollSmoothly = true;

		redrawBarSelection();

		return selectedIndex;
	}

	/**
	 * @param canAutoZoomToSlider
	 *        the canAutoZoomToSlider to set
	 */
	void setCanAutoZoomToSlider(final boolean canAutoZoomToSlider) {

		this.canAutoZoomToSlider = canAutoZoomToSlider;

		/*
		 * an auto-zoomed chart can't be scrolled
		 */
		if (canAutoZoomToSlider) {
			this.canScrollZoomedChart = false;
		}
	}

	/**
	 * @param canScrollZoomedChart
	 *        the canScrollZoomedChart to set
	 */
	void setCanScrollZoomedChart(final boolean canScrollZoomedGraph) {

		this.canScrollZoomedChart = canScrollZoomedGraph;

		/*
		 * a scrolled chart can't have the option to auto-zoom when the slider
		 * is dragged
		 */
		if (canScrollZoomedGraph) {
			this.canAutoZoomToSlider = false;
		}
	}

	/**
	 * sets a new configuration for the graph, the whole graph will be recreated
	 */
	void setDrawingData(final ArrayList<ChartDrawingData> drawingData) {

		// create empty list if list is not available, so we do not need
		// to check for null and isEmpty
		fDrawingData = drawingData;

		// force all graphics to be recreated
		fIsGraphDirty = true;
		fIsSliderDirty = true;
		isCustomLayerDirty = true;
		fIsSelectionDirty = true;

		// hide previous tooltip
		hideToolTip();

		// force the graph to be repainted
		redraw();
	}

	public boolean setFocus() {

		if (setFocusToControl()) {

			// the focus is now visible at the chart

			if (isFocusControl() == false) {
				forceFocus();
			}
			return true;

		} else {
			return false;
		}
	}

	/**
	 * Set the focus to a control depending on the chart type
	 * 
	 * @return Returns <code>true</code> when the focus was set
	 */
	private boolean setFocusToControl() {

		if (fDrawingData == null || fDrawingData.isEmpty()) {
			// we can't get the focus
			return false;
		}

		boolean isFocus = false;

		switch (fChart.getChartDataModel().getChartType()) {
		case ChartDataModel.CHART_TYPE_LINE:

			if (fSelectedXSlider == null) {
				// set focus to the left slider when x-sliders are visible
				if (isXSliderVisible) {
					fSelectedXSlider = getLeftSlider();
					isFocus = true;
				}
			} else if (fSelectedXSlider != null) {
				isFocus = true;
			}

			break;

		case ChartDataModel.CHART_TYPE_BAR:

			if (fSelectedBarItems == null || fSelectedBarItems.length == 0) {

				setSelectedBars(null);

			} else {

				// set focus to selected x-data

				int selectedIndex = -1;

				// find selected Index, reset last selected bar item(s)
				for (int index = 0; index < fSelectedBarItems.length; index++) {
					if (selectedIndex == -1 && fSelectedBarItems[index]) {
						selectedIndex = index;
					}
					fSelectedBarItems[index] = false;
				}

				if (selectedIndex == -1) {

					// a bar item is not selected, select first

					fSelectedBarItems[0] = true;

					fChart.fireBarSelectionEvent(0, 0);

				} else {

					// select last selected bar item

					fSelectedBarItems[selectedIndex] = true;
				}

				redrawBarSelection();
			}

			isFocus = true;

			break;
		}

		if (isFocus) {
			// System.out.println("setFocus");
			fChart.fireFocusEvent();
		}

		return isFocus;
	}

	void setSelectedBars(final boolean[] selectedItems) {

		if (selectedItems == null) {

			// set focus to first bar item

			if (fDrawingData.size() == 0) {
				fSelectedBarItems = null;
			} else {

				final ChartDrawingData chartDrawingData = fDrawingData.get(0);
				final ChartDataXSerie xData = chartDrawingData.getXData();

				fSelectedBarItems = new boolean[xData.fHighValues[0].length];
				// fSelectedXData[0] = true;
			}

		} else {

			fSelectedBarItems = selectedItems;
		}

		fScrollToSelection = true;
		fScrollSmoothly = true;

		fIsSelectionVisible = true;

		// disable zoom with parts
		graphZoomParts = 1;

		redrawBarSelection();
	}

	/**
	 * Set the scrolling cursor according to the vertical position of the mouse
	 * 
	 * @param devX
	 * @param devY
	 *        vertical coordinat of the mouse in the graph
	 */
	private void setupScrollCursor(final int devX, final int devY) {

		final int height = getVisibleGraphHeight();
		final int height4 = height / 4;
		final int height2 = height / 2;

		final float oldValue = scrollAcceleration;

		scrollAcceleration = devY < height4 ? 0.25f : devY < height2 ? 1 : devY > height - height4
				? 10
				: 2;

		// set cursor according to the position
		if (scrollAcceleration == 0.25) {
			setCursor(cursorHand05x);
		} else if (scrollAcceleration == 1) {
			setCursor(cursorHand);
		} else if (scrollAcceleration == 2) {
			setCursor(cursorHand2x);
		} else {
			setCursor(cursorHand5x);
		}

		/*
		 * when the acceleration has changed, the start positions for scrolling
		 * the graph must be set to the current location
		 */
		if (oldValue != scrollAcceleration) {
			startPosScrollbar = getHorizontalBar().getSelection();
			startPosDev = devX;
		}
	}

	/**
	 * Set the value index for a slider and move the slider to this position
	 * 
	 * @param slider
	 * @param valueIndex
	 */
	void setXSliderValueIndex(final ChartXSlider slider, int valueIndex) {

		final int[] xValues = getXData().getHighValues()[0];

		// adjust the slider index to the array bounds
		valueIndex = valueIndex < 0 ? 0 : valueIndex > (xValues.length - 1)
				? xValues.length - 1
				: valueIndex;

		slider.setValuesIndex(valueIndex);
		slider.setValueX(xValues[valueIndex]);

		final int linePos = (int) (devVirtualGraphImageWidth * (float) xValues[valueIndex] / xValues[xValues.length - 1]);
		slider.moveToDevPosition(linePos, true, true);

		fIsSliderDirty = true;
	}

	/**
	 * makes the slider visible, a slider is only drawn into the chart if a
	 * slider was created with createSlider
	 * 
	 * @param isXSliderVisible
	 */
	void setXSliderVisible(final boolean isSliderVisible) {
		this.isXSliderVisible = isSliderVisible;
	}

	private void showToolTip(final int x, final int y) {

		if (toolTipShell == null) {
			toolTipShell = new Shell(getShell(), SWT.ON_TOP | SWT.TOOL);
			toolTipLabel = new Label(toolTipShell, SWT.LEAD);
			final Display display = toolTipShell.getDisplay();

			toolTipLabel.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
			toolTipLabel.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

			for (int i = 0; i < toolTipEvents.length; i++) {
				addListener(toolTipEvents[i], toolTipListener);
			}
		}

		if (updateToolTip(x, y)) {
			toolTipShell.setVisible(true);
		} else {
			hideToolTip();
		}
	}

	/**
	 * switch the sliders to the 2nd x-data (switch between time and distance)
	 */
	void switchSlidersTo2ndXData() {
		switchSliderTo2ndXData(xSliderA);
		switchSliderTo2ndXData(xSliderB);
	}

	/**
	 * set the slider to the 2nd x-data and keep the slider on the same xValue
	 * position as before, this can cause to the situation, that the right
	 * slider gets unvisible/unhitable or the painted graph can have a white
	 * space on the right side
	 * 
	 * @param slider
	 *        the slider which gets changed
	 */
	private void switchSliderTo2ndXData(final ChartXSlider slider) {

		final int[] xValues = fDrawingData.get(0).getXData2nd().getHighValues()[0];
		int valueIndex = slider.getValuesIndex();

		if (valueIndex >= xValues.length) {
			valueIndex = xValues.length - 1;
			slider.setValuesIndex(valueIndex);
		}

		try {
			slider.setValueX(xValues[valueIndex]);

			final int linePos = (int) (devVirtualGraphImageWidth * ((float) ((float) xValues[valueIndex] / xValues[xValues.length - 1])));

			slider.moveToDevPosition(linePos, true, true);

		} catch (final ArrayIndexOutOfBoundsException e) {
			// ignore
		}
	}

	void updateChartLayers() {

		if (isDisposed()) {
			return;
		}

		isCustomLayerDirty = true;
		fIsSliderDirty = true;
		redraw();
	}

	/**
	 * Setup for the horizontal scrollbar
	 */
	private void updateHorizontalBar() {

		// if (fGraphCoreImage == null
		// || fGraphCoreImage.isDisposed()
		// || fDrawingData.isEmpty()) {
		// return;
		// }

		final ScrollBar hBar = getHorizontalBar();

		if (canScrollZoomedChart == false) {
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

		if (devVirtualGraphImageWidth > clientWidth) {

			// chart image is wider than the client area, show the scrollbar

			hBarMaximum = devVirtualGraphImageWidth;
			isHBarVisible = true;

			if (scrollToLeftSlider) {
				hBarSelection = Math.min(xSliderA.getDevVirtualSliderLinePos(), xSliderB
						.getDevVirtualSliderLinePos());
				hBarSelection -= (float) (((float) clientWidth * ZOOM_REDUCING_FACTOR) / 2.0);

				scrollToLeftSlider = false;

			} else if (fScrollToSelection) {

				// scroll to the selected x-data

				if (fSelectedBarItems != null) {

					for (int selectedIndex = 0; selectedIndex < fSelectedBarItems.length; selectedIndex++) {

						if (fSelectedBarItems[selectedIndex]) {

							// selected position was found
							final ChartDrawingData chartDrawingData = fDrawingData.get(0);
							final int[] xValues = chartDrawingData.getXData().fHighValues[0];

							final float xPosition = xValues[selectedIndex]
									* chartDrawingData.getScaleX();

							hBarSelection = (int) xPosition - (clientWidth / 2);

							break;
						}
					}

					if (fScrollSmoothly == false) {

						/*
						 * reset scroll to selection, this is only needed once
						 * when it's enable or when smooth scrolling is done
						 */
						fScrollToSelection = false;
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
		 * scroll bar selection (position of the graph) must be set AFTER the
		 * scrollbar is set visible/enabled
		 */
		if (fHorizontalScrollBarPos >= 0) {
			hBar.setSelection(fHorizontalScrollBarPos);
			fHorizontalScrollBarPos = -1;
		} else {

			if (fScrollSmoothly) {

				scrollSmoothly(hBarSelection);

			} else {

				// initialize smooth scroll start position
				fSmoothScrollCurrentPosition = hBarSelection;

				hBar.setSelection(hBarSelection);
			}
		}
	}

	private void scrollSmoothly(final int barSelection) {

		final Display display = Display.getCurrent();
		final int scrollInterval = 10;

		fSmoothScrollEndPosition = Math.abs(barSelection);

		if (fIsSmoothScrollingActive == false) {

			fIsSmoothScrollingActive = true;

			/*
			 * start smooth scrolling after the chart is drawn, because the
			 * caller for this method is in the paint event
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
			fIsSmoothScrollingActive = false;
			return;
		}

		final int scrollDiff = Math.abs(fSmoothScrollEndPosition - fSmoothScrollCurrentPosition);

		// start scrolling again if the position was not
		// reached
		if (scrollDiff > scrollDiffMax) {

			if (fSmoothScrollCurrentPosition < fSmoothScrollEndPosition) {
				fSmoothScrollCurrentPosition += scrollDiffMax;
			} else {
				fSmoothScrollCurrentPosition -= scrollDiffMax;
			}

			// scroll the graph
			fHorizontalScrollBarPos = fSmoothScrollCurrentPosition;
			redraw();

			display.timerExec(scrollInterval, runnable);

		} else {

			fIsSmoothScrollingActive = false;
			fScrollToSelection = false;

			/*
			 * the else part is being called in the initializing, I don't know
			 * why then the scrollDiff == 0
			 */
			if (scrollDiff != 0) {
				fScrollSmoothly = false;
			}
		}
	}

	public void updateSelectionOnResize() {

	// force the hovered bar(s) to be hidden
	// fHoveredBarSerieIndex = -1;
	}

	/**
	 * Resize the sliders after the graph was resized
	 */
	void updateSlidersOnResize() {

		/*
		 * update all x-sliders
		 */

		if (canScrollZoomedChart) {

			// the image can be scrolled

			devGraphImageXOffset = 0;

		} else {

			// the graph image can't be scrolled, show only the zoomed part

			final int visibleGraphWidth = getDevVisibleChartWidth();

			// calculate new virtual graph width
			devVirtualGraphImageWidth = (int) (graphZoomRatio * visibleGraphWidth);

			if (graphZoomRatio == 1.0) {
				// with the ration 1.0 the graph is not zoomed
				devGraphImageXOffset = 0;
			} else {
				// the graph is zoomed, only a part is displayed which starts at
				// the offset for the left slider
				// if (fUpdateImageOffset) {
				devGraphImageXOffset = (int) (getLeftSlider().getPositionRatio() * devVirtualGraphImageWidth);
				// System.out.println("updateSlidersOnResize: "
				// + devGraphImageXOffset
				// + " "
				// + devVirtualGraphImageWidth);
				// }
			}
		}

		// reset image offset update
		// fUpdateImageOffset = true;

		final int visibleGraphHeight = getVisibleGraphHeight();
		xSliderA.handleChartResize(visibleGraphHeight);
		xSliderB.handleChartResize(visibleGraphHeight);

		/*
		 * update all y-sliders
		 */
		ySliders = new ArrayList<ChartYSlider>();

		// loop: get all y-sliders from all graphs
		for (final ChartDrawingData drawingData : fDrawingData) {

			final ChartDataYSerie yData = drawingData.getYData();

			if (yData.isShowYSlider()) {

				final ChartYSlider sliderTop = yData.getYSliderTop();
				final ChartYSlider sliderBottom = yData.getYSliderBottom();

				sliderTop.handleChartResize(drawingData, ChartYSlider.SLIDER_TYPE_TOP);
				ySliders.add(sliderTop);

				sliderBottom.handleChartResize(drawingData, ChartYSlider.SLIDER_TYPE_BOTTOM);
				ySliders.add(sliderBottom);

				isYSliderVisible = true;
			}
		}
	}

	private boolean updateToolTip(final int x, final int y) {

		final String tooltip = getToolTipText(x, y);

		if (tooltip == null) {
			return false;
		}

		if (tooltip.equals(toolTipLabel.getText())) {
			return true;
		}

		toolTipLabel.setText(tooltip);
		final Point labelSize = toolTipLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		labelSize.x += 2;
		labelSize.y += 2;
		toolTipLabel.setSize(labelSize);
		toolTipShell.pack();

		/*
		 * On some platforms, there is a minimum size for a shell which may be
		 * greater than the label size. To avoid having the background of the
		 * tip shell showing around the label, force the label to fill the
		 * entire client area.
		 */
		final Rectangle area = toolTipShell.getClientArea();
		toolTipLabel.setSize(area.width, area.height);

		/*
		 * Position the tooltip and ensure that it is not located off the
		 * screen.
		 */
		final Point cursorLocation = getDisplay().getCursorLocation();
		// Assuming cursor is 21x21 because this is the size of
		// the arrow cursor on Windows
		final int cursorHeight = 21;
		final Point size = toolTipShell.getSize();
		final Rectangle rect = getMonitor().getBounds();
		final Point pt = new Point(cursorLocation.x, cursorLocation.y + cursorHeight + 2);
		pt.x = Math.max(pt.x, rect.x);
		if (pt.x + size.x > rect.x + rect.width)
			pt.x = rect.x + rect.width - size.x;
		if (pt.y + size.y > rect.y + rect.height) {
			pt.y = cursorLocation.y - 2 - size.y;
		}
		toolTipShell.setLocation(pt);
		return true;
	}

	/**
	 * adjust the y-position for the bottom label when the top label is drawn
	 * over it
	 */
	private void updateXSliderYPosition() {

		int labelIndex = 0;
		final ArrayList<ChartXSliderLabel> onTopLabels = xSliderOnTop.getLabelList();
		final ArrayList<ChartXSliderLabel> onBotLabels = xSliderOnBottom.getLabelList();

		for (final ChartXSliderLabel onTopLabel : onTopLabels) {

			final ChartXSliderLabel onBotLabel = onBotLabels.get(labelIndex);

			final int onTopWidth2 = onTopLabel.getWidth() / 2;
			final int onTopDevX = onTopLabel.getX();
			final int onBotWidth2 = onBotLabel.getWidth() / 2;
			final int onBotDevX = onBotLabel.getX();

			if (onTopDevX + onTopWidth2 > onBotDevX - onBotWidth2
					&& onTopDevX - onTopWidth2 < onBotDevX + onBotWidth2) {
				onBotLabel.setY(onBotLabel.getY() + onBotLabel.getHeight());
			}
			labelIndex++;
		}
	}

	/**
	 * Zoom into the graph with the ratio 1:2
	 */
	void zoomInWithoutSlider() {

		final int visibleGraphWidth = getDevVisibleChartWidth();
		final int graphImageWidth = devVirtualGraphImageWidth;

		final int minChartWidth = fChart.getChartDataModel().getChartMinWidth();
		final int maxChartWidth = ChartComponents.CHART_MAX_WIDTH;

		if (graphImageWidth >= minChartWidth && graphImageWidth <= maxChartWidth) {

			// chart is within the range which can be zoomed in

			if (graphImageWidth * 2 > maxChartWidth) {
				/*
				 * the double zoomed graph would be wider than the max width,
				 * reduce it to the max width
				 */
				graphZoomRatio = maxChartWidth / visibleGraphWidth;
				devVirtualGraphImageWidth = maxChartWidth;
			} else {
				graphZoomRatio = graphZoomRatio * 2;
				devVirtualGraphImageWidth = graphImageWidth * 2;
			}

			// disable zoom with parts
			graphZoomParts = 1;

			fScrollToSelection = true;

			/*
			 * scroll smoothly is started when a bar is selected, otherwise it
			 * is disabled
			 */
			fScrollSmoothly = false;
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

		boolean updateImageOffset = false;

		if (devSliderDiff == 0) {
			// no difference between the slider
			graphZoomRatio = 1;
			devVirtualGraphImageWidth = devVisibleChartWidth;
			devGraphImageXOffset = 0;

			scrollToLeftSlider = false;

		} else {

			if (canScrollZoomedChart) {

				// the image can be scrolled

				final int graphWidth = graphZoomRatio == 1
						? devVisibleChartWidth
						: devVirtualGraphImageWidth;

				graphZoomRatio = (float) ((float) graphWidth * (1 - ZOOM_REDUCING_FACTOR) / devSliderDiff);
				devVirtualGraphImageWidth = (int) ((float) devVisibleChartWidth * graphZoomRatio);
				devGraphImageXOffset = 0;

				scrollToLeftSlider = true;

			} else {

				/*
				 * the graph image can't be scrolled, show only the zoomed part
				 * which is defined between the two sliders
				 */

				// calculate new graph ratio
				graphZoomRatio = (float) (devVirtualGraphImageWidth) / (devSliderDiff);

				// adjust rounding problems
				graphZoomRatio = (graphZoomRatio * devVisibleChartWidth) / devVisibleChartWidth;

				updateImageOffset = true;
			}
		}

		// set the new graph width
		enforceChartImageMinMaxWidth();

		// update the width in the sliders
		final int visibleGraphHeight = getVisibleGraphHeight();

		leftSlider.handleChartResize(visibleGraphHeight);
		rightSlider.handleChartResize(visibleGraphHeight);

		// the offset must be calculated after the left slider is resized
		if (updateImageOffset) {
			// devGraphImageXOffset = (int) (getLeftSlider().getPositionRatio()
			// * devVirtualGraphImageWidth);
		}
	}

	/**
	 * Zooms out of the graph
	 */
	void zoomOut() {

		final int visibleGraphWidth = getDevVisibleChartWidth();
		final int visibleGraphHeight = getVisibleGraphHeight();

		// reset the data which influence the computed graph image width
		graphZoomRatio = 1;
		devVirtualGraphImageWidth = visibleGraphWidth;
		devGraphImageXOffset = 0;

		// reset zoom parts
		graphZoomParts = 1;
		graphZoomPartPosition = 0;

		/*
		 * scroll smoothly is started when a bar is selected, otherwise it is
		 * disabled
		 */
		fScrollSmoothly = false;

		// set the new graph width
		enforceChartImageMinMaxWidth();

		// reposition the sliders
		xSliderA.handleChartResize(visibleGraphHeight);
		xSliderB.handleChartResize(visibleGraphHeight);
	}

	/**
	 * zoom to the provided x-slider position
	 * 
	 * @param sliderPosition
	 */
	void zoomToXSliderPosition(final SelectionChartXSliderPosition sliderPosition) {

		final int[] xValues = getXData().getHighValues()[0];
		final int maxValue = xValues[xValues.length - 1];
		final int visibleGraphWidth = getDevVisibleChartWidth();

		// get slider values
		final int valueIndex1 = sliderPosition.slider1ValueIndex;
		final int valueIndex2 = sliderPosition.slider2ValueIndex;
		final int value1 = xValues[valueIndex1];
		final int value2 = xValues[valueIndex2];

		// set the new slider value/index
		xSliderA.setSliderLineValueIndex(valueIndex1, value1);
		xSliderB.setSliderLineValueIndex(valueIndex2, value2);

		final int valueDiff = Math.abs(value1 - value2);

		// calculate new graph ratio
		graphZoomRatio = (float) maxValue / valueDiff;

		// calculate new virtual graph width
		devVirtualGraphImageWidth = (int) (graphZoomRatio * visibleGraphWidth);

		// set the sliders to a new position
		final int virtualSlider1Pos = (int) ((float) (devVirtualGraphImageWidth * value1) / maxValue);
		final int virtualSlider2Pos = (int) ((float) (devVirtualGraphImageWidth * value2) / maxValue);

		/*
		 * calculate the offset for the left slider, the graph is drawn starting
		 * at this position
		 */
		devGraphImageXOffset = Math.min(virtualSlider1Pos, virtualSlider2Pos);
		xSliderA.moveToDevPosition(virtualSlider1Pos, true, true);
		xSliderB.moveToDevPosition(virtualSlider2Pos, true, true);
	}

	/**
	 * zoom in where parts defines how the width of the graph will be splitted
	 * and position defines which part is shown
	 * 
	 * @param ratio
	 */
	void zoomWithParts(final int parts, final float position) {

		canScrollZoomedChart = true;

		final int devVisibleGraphWidth = getDevVisibleChartWidth();
		final int maxChartWidth = ChartComponents.CHART_MAX_WIDTH;

		final int graphWidth = Math.min(maxChartWidth, devVisibleGraphWidth * parts);

		// reduce the width so that more than one part will be visible in the
		// clientarea
		devVirtualGraphImageWidth = (int) ((float) graphWidth * ZOOM_WITH_PARTS_RATIO);
		graphZoomRatio = Math.max(1, (float) devVirtualGraphImageWidth / devVisibleGraphWidth);

		final int partWidth = devVirtualGraphImageWidth / parts;
		final int partBorder = devVisibleGraphWidth - partWidth;

		// position the graph in the scrollbar
		if (position == 0) {
			fHorizontalScrollBarPos = 0;
		} else {
			fHorizontalScrollBarPos = (int) ((partWidth * position - 1) - (partBorder / 2));
		}

		graphZoomParts = parts;
		graphZoomPartPosition = position;
	}

	/**
	 * set the slider position when the data model has changed
	 */
	public void resetSliders() {

		// first get the left/right slider
		final ChartXSlider leftSlider = getLeftSlider();
		final ChartXSlider rightSlider = getRightSlider();

		/*
		 * reset the sliders, the temp sliders are used so that the same slider
		 * is not reset twice
		 */
		leftSlider.reset();
		rightSlider.reset();
	}

}
