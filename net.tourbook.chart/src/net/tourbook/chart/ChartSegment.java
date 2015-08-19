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

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

public class ChartSegment {

	public static final int	TITLE_HOVER_MARGIN	= 5;

	private Long			_tourId;

	public int				devXTitle;
	public int				devYTitle;

	public int				titleWidth;
	public int				titleHeight;

	public int				devYGraphTop;
	public int				devGraphWidth;

	public int				devXSegment;
	public int				devSegmentWidth;

	public ChartSegment() {}

	/**
	 * Get an area from the segment display position which can be hovered and the tooltip do not get
	 * hidden.
	 * 
	 * @param control
	 * @return
	 */
	public Rectangle getNoHideArea(final Control control) {

		final Point segmentDisplayPosition = control.toDisplay(devXSegment, 0);

		final Rectangle segmentArea = new Rectangle(
				segmentDisplayPosition.x,
				segmentDisplayPosition.y,
				devSegmentWidth,
				devYTitle + titleHeight);

		return segmentArea;
	}

	/**
	 * @return Returns ID of a tour or <code>null</code> when not available.
	 */
	public Long getTourId() {
		return _tourId;
	}

	public void setTourId(final Long tourId) {
		_tourId = tourId;
	}

	@Override
	public String toString() {
		return "TourSegment ["
//				+ ("devX=" + devXTitle + ", ")
//				+ ("devY=" + devYTitle + ", ")
//				+ ("width=" + titleWidth + ", ")
//				+ ("height=" + titleHeight+", ")
				+ ("tourId=" + _tourId + ", ")
				+ "]";
	}
}
