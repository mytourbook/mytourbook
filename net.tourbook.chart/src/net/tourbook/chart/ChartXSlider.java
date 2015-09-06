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
/**
 * @author Wolfgang Schramm created 11.07.2005
 */
package net.tourbook.chart;

import java.util.ArrayList;

import net.tourbook.common.RectangleLong;

/**
 * The slider is moved on the x-axis and displays the current position in the slider label.
 */
public class ChartXSlider {

	private static final int				SLIDER_TYPE_NONE	= 0;
	public static final int					SLIDER_TYPE_LEFT	= 1;
	public static final int					SLIDER_TYPE_RIGHT	= 2;

	/**
	 * position of the slider line within the slider (starting from left)
	 */
	public final static int					SLIDER_LINE_WIDTH	= 10;

	int										sliderType			= SLIDER_TYPE_NONE;

	private final RectangleLong				_hitRectangle		= new RectangleLong(0, 0, SLIDER_LINE_WIDTH * 2, 0);

	/**
	 * keep the ratio of sliderpos/clientwidth, this is needed when changing the client width, so we
	 * don't loose the accuracy for the position
	 */
	private double							_positionRatio;

	/**
	 * Position of the slider line which is in the middle of the slider
	 */
	private long							_xxDevSliderLinePos;

	/**
	 * Contains the offset between the left position of the slider hit rectangle and the mouse hit
	 * within it.
	 */
	private long							_devXClickOffset;

	/**
	 * Contains the position of the slider within values array
	 */
	private int								_valueIndex;

	/**
	 * labelList contains a slider label for each graph
	 */
	private ArrayList<ChartXSliderLabel>	_labelList;

	private ChartComponentGraph				_chartGraph;

	/**
	 * Constructor
	 * 
	 * @param devVirtualSliderLinePos
	 */
	ChartXSlider(final ChartComponentGraph graph, final int xxDevSliderPosition, final int sliderType) {

		_chartGraph = graph;
		this.sliderType = sliderType;

		moveToXXDevPosition(xxDevSliderPosition, true, true, false);
	}

	/**
	 * @return Returns the devXClickOffset.
	 */
	long getDevXClickOffset() {
		return _devXClickOffset;
	}

	/**
	 * @return Returns the hit rectangle, rectangle.x contains the left border of the viewport.
	 */
	RectangleLong getHitRectangle() {

		final RectangleLong rect = new RectangleLong(
				_hitRectangle.x,
				_hitRectangle.y,
				_hitRectangle.width,
				_hitRectangle.height);

		rect.x -= _chartGraph.getXXDevViewPortLeftBorder();

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
	double getPositionRatio() {
		return _positionRatio;
	}

	/**
	 * @return Returns the position of the slider in the values array
	 */
	public int getValuesIndex() {
		return _valueIndex;
	}

	/**
	 * @return Returns the slider line position in the graph
	 */
	public long getXXDevSliderLinePos() {
		return _xxDevSliderLinePos;
	}

	/**
	 * When the width of the graph changed, the slider bar position must be adjusted.
	 * 
	 * @param devGraphHeight
	 * @param isFireEvent
	 */
	void handleChartResize(final int devGraphHeight, final boolean isFireEvent) {

		// resize the hit rectangle
		_hitRectangle.height = devGraphHeight;

		if (sliderType == SLIDER_TYPE_RIGHT) {
			// position the right slider to the right side, this is done only
			// the first time
			sliderType = SLIDER_TYPE_NONE;

			// run the positioning after all is done, otherwise not all is
			// initialized
			_chartGraph.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {

					/**
					 * Get position here because it can be changed !!!
					 */
					final long xxDevGraphWidth = _chartGraph.getXXDevGraphWidth();

					moveToXXDevPosition(xxDevGraphWidth, true, true, true, isFireEvent);
				}
			});

		} else {

			// reposition the slider line but keep the position ratio

			final long xxDevGraphWidth = _chartGraph.getXXDevGraphWidth();
			final double devSliderPos = xxDevGraphWidth * _positionRatio;
			final double xxDevLinePos = devSliderPos < 0 ? 0 : devSliderPos;

			moveToXXDevPosition(xxDevLinePos, true, false, false, isFireEvent);
		}
	}

	void moveToXXDevPosition(	final double xxDevLinePos,
								final boolean isAdjustToImageWidth,
								final boolean isAdjustPositionRatio,
								final boolean isUpdateValueFromRatio) {

		moveToXXDevPosition(//
				xxDevLinePos,
				isAdjustToImageWidth,
				isAdjustPositionRatio,
				isUpdateValueFromRatio,
				true);
	}

	/**
	 * Sets a new position for the sliderLine and also updates the slider/line rectangles and value.
	 * A Slider move event is fired
	 * 
	 * @param xxDevLinePos
	 */
	void moveToXXDevPosition(	double xxDevLinePos,
								final boolean isAdjustToImageWidth,
								final boolean isAdjustPositionRatio,
								final boolean isUpdateValueFromRatio,
								final boolean isFireEvent) {

		final long xxDevGraphWidth = _chartGraph.getXXDevGraphWidth();

		if (xxDevGraphWidth == 0) {
			return;
		}

		/*
		 * the slider line can be out of bounds for the graph image, this can happen when the graph
		 * is auto-zoomed to the slider position in the mouse up event
		 */
		if (isAdjustToImageWidth) {
			xxDevLinePos = Math.min(xxDevGraphWidth, Math.max(0, xxDevLinePos));
		}

		// reposition the hit rectangle
		_hitRectangle.x = (long) (xxDevLinePos - ChartXSlider.SLIDER_LINE_WIDTH);

		// the devVirtualSliderLinePos must be set before the change event is
		// called, otherwise the event listener would get the old value
		final long xxDevOldPos = _xxDevSliderLinePos;
		_xxDevSliderLinePos = (long) xxDevLinePos;

		if (isAdjustPositionRatio) {

			_positionRatio = xxDevLinePos / (xxDevGraphWidth - 0);

			// enforce max value
			_positionRatio = Math.min(_positionRatio, 1);

			if (isUpdateValueFromRatio) {
				_chartGraph.setXSliderValue_FromRatio(ChartXSlider.this);
			}
		}

		// fire change event when the position has changed
		if (isFireEvent && (long) xxDevLinePos != xxDevOldPos) {
			_chartGraph._chart.fireSliderMoveEvent();
		}
	}

	public void reset() {

		final long xxDevGraphWidth = _chartGraph.getXXDevGraphWidth();

		double xxDevSliderPos = xxDevGraphWidth * _positionRatio;
		xxDevSliderPos = xxDevSliderPos < 0 ? 0 : xxDevSliderPos;

		_chartGraph.setXSliderValue_FromRatio(this);
		moveToXXDevPosition(xxDevSliderPos, true, true, false);
	}

	/**
	 * @param devXClickOffset
	 *            The devXClickOffset to set.
	 */
	void setDevXClickOffset(final long devXClickOffset) {
		_devXClickOffset = devXClickOffset;
	}

	void setLabelList(final ArrayList<ChartXSliderLabel> labelList) {
		_labelList = labelList;
	}

	/**
	 * Set position of the slider within value array.
	 * 
	 * @param valueIndex
	 */
	void setValueIndex(final int valueIndex) {

		_valueIndex = valueIndex;
	}

	@Override
	public String toString() {
		return "ChartXSlider [" //$NON-NLS-1$
				+ ("_positionRatio=" + _positionRatio + " ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("_valueIndex=" + _valueIndex + " ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("_xxDevSliderLinePos=" + _xxDevSliderLinePos + " ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("sliderType=" + sliderType + " ") //$NON-NLS-1$ //$NON-NLS-2$
				+ "]"; //$NON-NLS-1$
	}

}
