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
/**
 * @author Wolfgang Schramm created 11.07.2005
 */

package net.tourbook.chart;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Rectangle;

/**
 * the slider is moved on the x-axis and displays the current position in the slider label
 */
public class ChartXSlider {

	private static final int				SLIDER_TYPE_NONE		= 0;
	public static final int					SLIDER_TYPE_LEFT		= 1;
	public static final int					SLIDER_TYPE_RIGHT		= 2;

	/**
	 * position of the slider line within the slider (starting from left)
	 */
	public final static int					halfSliderHitLineHeight	= 10;
	public final static int					sliderHitLineHeight		= 2 * halfSliderHitLineHeight;

	private final Rectangle					_hitRectangle			= new Rectangle(0, 0, sliderHitLineHeight, 0);

	/**
	 * keep the ratio of sliderpos/clientwidth, this is needed when changing the client width, so we
	 * don't loose the accuracy for the position
	 */
	private float							_positionRatio;

	/**
	 * position of the slider line which is in the middle of the slider
	 */
	private int								_devVirtualSliderLinePos;

	/**
	 * offset between the left position of the slider hit rectangle and the mouse hit within it
	 */
	private int								_devXClickOffset;

	/**
	 * valuesIndex represents the position of the slider within values array
	 */
	private int								_valuesIndex;

	/**
	 * value of the slider on the x axis
	 */
	private float							_valueX;

	/**
	 * labelList contains a slider label for each graph
	 */
	private ArrayList<ChartXSliderLabel>	_labelList;

	private ChartComponentGraph				_chartGraph;

	int										sliderType				= SLIDER_TYPE_NONE;

	/**
	 * Constructor
	 * 
	 * @param devVirtualSliderLinePos
	 */
	ChartXSlider(final ChartComponentGraph graph, final int sliderPosition, final int sliderType) {

		_chartGraph = graph;
		this.sliderType = sliderType;

		moveToDevPosition(sliderPosition, true, true);
	}

	/**
	 * @return Returns the slider line position in the graph
	 */
	public int getDevVirtualSliderLinePos() {
		return _devVirtualSliderLinePos;
	}

	/**
	 * @return Returns the devXClickOffset.
	 */
	int getDevXClickOffset() {
		return _devXClickOffset;
	}

	/**
	 * @return Returns the hit rectangle
	 */
	Rectangle getHitRectangle() {

		final Rectangle rect = new Rectangle(
				_hitRectangle.x,
				_hitRectangle.y,
				_hitRectangle.width,
				_hitRectangle.height);

		rect.x -= _chartGraph.getDevGraphImageXOffset();

		return rect;
	}

	/**
	 * @return Returns a list with all slider labels
	 */
	public ArrayList<ChartXSliderLabel> getLabelList() {
		return _labelList;
	}

	/**
	 * @return the positionRatio
	 */
	float getPositionRatio() {
		return _positionRatio;
	}

	/**
	 * @return Returns the position of the slider in the values array
	 */
	public int getValuesIndex() {
		return _valuesIndex;
	}

	public float getValueX() {
		return _valueX;
	}

	/**
	 * When the width of the graph changed, the slider bar position must be adjusted
	 */
	void handleChartResize(final int devGraphHeight) {

		// resize the hit rectangle
		_hitRectangle.height = devGraphHeight;

		final int devVirtualGraphImageWidth = _chartGraph.getDevVirtualGraphImageWidth();

		if (sliderType == SLIDER_TYPE_RIGHT) {
			// position the right slider to the right side, this is done only
			// the first time
			sliderType = SLIDER_TYPE_NONE;

			// run the positioning after all is done, otherwise not all is
			// initialized
			_chartGraph.getDisplay().asyncExec(new Runnable() {
				public void run() {

					moveToDevPosition(devVirtualGraphImageWidth, true, true);

					_chartGraph.computeXSliderValue(ChartXSlider.this, devVirtualGraphImageWidth);
				}
			});

		} else {

			// reposition the slider line but keep the position ratio
			final float devSliderPos = devVirtualGraphImageWidth * _positionRatio;

			moveToDevPosition((int) (devSliderPos < 0 ? 0 : devSliderPos), true, false);
		}
	}

	/**
	 * Sets a new position for the sliderLine and also updates the slider/line rectangles and value.
	 * A Slider move event is fired
	 * 
	 * @param newDevVirtualSliderLinePos
	 */
	void moveToDevPosition(	int newDevVirtualSliderLinePos,
							final boolean adjustToImageWidth,
							final boolean adjustPositionRatio) {

		final int devVirtualGraphImageWidth = _chartGraph.getDevVirtualGraphImageWidth();
		/*
		 * the slider line can be out of bounds for the graph image, this can happen when the graph
		 * is auto-zoomed to the slider position in the mouse up event
		 */
		if (adjustToImageWidth) {
			newDevVirtualSliderLinePos = Math.min(devVirtualGraphImageWidth, Math.max(0, newDevVirtualSliderLinePos));
		}

		// reposition the hit rectangle
		_hitRectangle.x = newDevVirtualSliderLinePos - ChartXSlider.halfSliderHitLineHeight;

		// the devVirtualSliderLinePos must be set before the change event is
		// called, otherwise the event listener would get the old value
		final int oldPos = _devVirtualSliderLinePos;
		_devVirtualSliderLinePos = newDevVirtualSliderLinePos;

		if (adjustPositionRatio) {

			_positionRatio = (float) newDevVirtualSliderLinePos / (devVirtualGraphImageWidth - 0);

			// enforce max value
			_positionRatio = Math.min(_positionRatio, 1);
		}

		// System.out.println(("slider:newDevVirtualSliderLinePos " + newDevVirtualSliderLinePos)
		// + ("\tpositionRatio:" + positionRatio));

		// fire change event when the position has changed
		if (newDevVirtualSliderLinePos != oldPos) {
			_chartGraph._chart.fireSliderMoveEvent();
		}
	}

	public void reset() {

		final int devGraphRightBorder = _chartGraph.getDevVirtualGraphImageWidth();

		float devSliderPos = devGraphRightBorder * _positionRatio;
		devSliderPos = devSliderPos < 0 ? 0 : devSliderPos;

		moveToDevPosition((int) devSliderPos, true, true);

		_chartGraph.computeXSliderValue(ChartXSlider.this, (int) devSliderPos);
	}

	/**
	 * @param devXClickOffset
	 *            The devXClickOffset to set.
	 */
	void setDevXClickOffset(final int devXClickOffset) {
		_devXClickOffset = devXClickOffset;
	}

	void setLabelList(final ArrayList<ChartXSliderLabel> labelList) {
		_labelList = labelList;
	}

	void setSliderLineValueIndex(final int valueIndex, final int xValue) {

		_valuesIndex = valueIndex;
		_valueX = xValue;
	}

	/**
	 * valuesIndex is the position of the slider in the values array
	 * 
	 * @param valuesIndex
	 */
	public void setValuesIndex(final int valueIndex) {
		_valuesIndex = valueIndex;
	}

	void setValueX(final float sliderValueX) {
		_valueX = sliderValueX;
	}

}
