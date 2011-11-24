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
/**
 * @author Wolfgang Schramm created 11.07.2005
 */
package net.tourbook.chart;

import org.eclipse.swt.graphics.Rectangle;

public class ChartYSlider {

	public static final int		SLIDER_TYPE_TOP			= 1;
	public static final int		SLIDER_TYPE_BOTTOM		= 2;

	/**
	 * height of the slider line which can be hit by the mouse
	 */
	public final static int		halfSliderHitLineHeight	= 10;
	public final static int		sliderHitLineHeight		= 2 * halfSliderHitLineHeight;

	private ChartDataYSerie		_yData;

	/**
	 * rectangle where the slider can be hit
	 */
	private Rectangle			_hitRectangle			= new Rectangle(0, 0, 0, sliderHitLineHeight);

	/**
	 * y position for the slider line
	 */
	private int					_devYSliderLine			= 0;

	/**
	 * offset between the top of the slider hit rectangle and the mouse hit within it
	 */
	int							devYClickOffset;

	private GraphDrawingData	_drawingData;

	/**
	 * Constructor
	 */
	ChartYSlider(final ChartDataYSerie yData) {
		_yData = yData;
	}

	/**
	 * @return Returns the devYSliderLine.
	 */
	public int getDevYSliderLine() {
		return _devYSliderLine;
	}

	/**
	 * @return Returns the drawingData.
	 */
	public GraphDrawingData getDrawingData() {
		return _drawingData;
	}

	/**
	 * @return Returns the hitRectangle.
	 */
	public Rectangle getHitRectangle() {
		return _hitRectangle;
	}

	/**
	 * @return Returns the yData.
	 */
	public ChartDataYSerie getYData() {
		return _yData;
	}

	/**
	 * Resize the slider after the chart was resizes
	 * 
	 * @param drawingData
	 */
	void handleChartResize(final GraphDrawingData drawingData, final int sliderType) {

		_drawingData = drawingData;

		final int devGraphHeight = drawingData.devGraphHeight;
		final int devYBottom = drawingData.getDevYBottom();

		if (sliderType == SLIDER_TYPE_BOTTOM) {
			// set the slider and hit rectangle at the bottom of the chart
			_devYSliderLine = devYBottom;
		} else {
			// set the slider and hit rectangle at the top of the chart
			_devYSliderLine = devYBottom - devGraphHeight;
		}

		_hitRectangle.y = _devYSliderLine - ChartYSlider.halfSliderHitLineHeight;
		_hitRectangle.width = drawingData.devVirtualGraphWidth - 1;
	}

	/**
	 * set y value for the slider line, this is done when the mouse was moved
	 * 
	 * @param devYSliderLine
	 *            The devYSliderLine to set.
	 */
	public void setDevYSliderLine(final int devYSliderLine) {

		_devYSliderLine = devYSliderLine;

		_hitRectangle.y = devYSliderLine - ChartYSlider.halfSliderHitLineHeight;
	}

	@Override
	public String toString() {
		return "ChartYSlider ["
				+ "devYSliderLine="
				+ _devYSliderLine
				+ ", "
				+ "devYClickOffset="
				+ devYClickOffset
				+ ", "
				+ "hitRectangle="
				+ _hitRectangle
				+ "]";
	}
}
