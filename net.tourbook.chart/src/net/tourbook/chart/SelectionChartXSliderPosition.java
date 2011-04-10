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

import org.eclipse.jface.viewers.ISelection;

/**
 * contains the value index for the sliders
 */
public class SelectionChartXSliderPosition implements ISelection {

	public static final int	IGNORE_SLIDER_POSITION			= -1;
	public static final int	SLIDER_POSITION_AT_CHART_BORDER	= -2;

	private int				_beforeLeftSliderIndex			= IGNORE_SLIDER_POSITION;
	private int				_leftSliderValueIndex			= IGNORE_SLIDER_POSITION;
	private int				_rightSliderValueIndex			= IGNORE_SLIDER_POSITION;

	private boolean			_isCenterSliderPosition			= false;

	private Chart			_chart;

	/**
	 * When <code>true</code> the start index must be adjusted to the next time slice, this bug
	 * exists since the beginning but is visible since the break time is visualized.
	 */
	private boolean			_isAdjustStartIndex;

	public SelectionChartXSliderPosition(final Chart chart, final int leftValueIndex, final int rightValueIndex) {

		_chart = chart;

		_leftSliderValueIndex = leftValueIndex;
		_rightSliderValueIndex = rightValueIndex;
	}

	public SelectionChartXSliderPosition(	final Chart chart,
											final int startIndex,
											final int endIndex,
											final boolean isAdjustStartIndex) {

		this(chart, startIndex, endIndex);

		_isAdjustStartIndex = isAdjustStartIndex;

	}

	public SelectionChartXSliderPosition(	final Chart chart,
											final int serieIndex0,
											final int serieIndex1,
											final int serieIndex2,
											final boolean centerSliderPosition) {

		this(chart, serieIndex1, serieIndex2);

		_beforeLeftSliderIndex = serieIndex0;

		_isCenterSliderPosition = centerSliderPosition;
	}

	public int getBeforeLeftSliderIndex() {
		return _beforeLeftSliderIndex;
	}

	public Chart getChart() {
		return _chart;
	}

	public int getLeftSliderValueIndex() {
		return _leftSliderValueIndex;
	}

	public int getRightSliderValueIndex() {
		return _rightSliderValueIndex;
	}

	public boolean isAdjustStartIndex() {
		return _isAdjustStartIndex;
	}

	public boolean isCenterSliderPosition() {
		return _isCenterSliderPosition;
	}

	public boolean isEmpty() {
		return false;
	}

	public void setChart(final Chart chart) {
		_chart = chart;
	}

}
