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
/*
 * Administrator 10.08.2005
 */
package net.tourbook.ui.tourChart;

import net.tourbook.common.graphics.Line2D;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

public class SegmenterSegment {

	static final int	EXPANDED_HOVER_SIZE		= 12;
	static final int	EXPANDED_HOVER_SIZE2	= EXPANDED_HOVER_SIZE / 2;

	boolean				isVisible;

	/**
	 * index in the data serie
	 */
	int					serieIndex;

	/**
	 * x-position in graph units
	 */
	double				graphX;

	/**
	 * Painted position for the label.
	 */
	Rectangle			paintedLabel;

	/**
	 * {@link Integer#MIN_VALUE} is a flag that this value is not yet set.
	 */
	int					paintedX1				= Integer.MIN_VALUE;
	int					paintedX2;
	int					paintedY1;
	int					paintedY2;

	RGB					paintedRGB;

	int					xSliderSerieIndexLeft;
	int					xSliderSerieIndexRight;

	int					segmentIndex;

	Rectangle			hoveredLabelRect;
	Rectangle			hoveredLineRect;
	Line2D				hoveredLineShape;

	long				tourId;

	SegmenterSegment() {}

	/**
	 * Get an area from the segment display position which can be hovered and the tooltip do not get
	 * hidden.
	 * 
	 * @param control
	 * @param displayCursorLocation
	 * @return
	 */
	public boolean isInNoHideArea(final Control control, final Point displayCursorLocation) {

		if (hoveredLineShape == null) {
			return false;
		}
//
//		final Point segmentDisplayPosition = control.toDisplay(devXSegment, 0);
//
//		isInNoHideArea = hoveredLineShape.intersects(
//				displayCursorLocation.x,
//				displayCursorLocation.y,
//				NO_HIDE_HOVER_SIZE,
//				NO_HIDE_HOVER_SIZE);
//
//		final Rectangle segmentArea = new Rectangle(
//				segmentDisplayPosition.x,
//				segmentDisplayPosition.y,
//				devSegmentWidth,
//				devYTitle + titleHeight);

		return false;
	}

	@Override
	public String toString() {
		return "SegmenterSegment [" //
//				+ ("paintedLabel=" + paintedLabel + ", ")
				+ ("segmentIndex=" + segmentIndex + ", ")
//				+ ("serieIndex=" + serieIndex + ", ")
				+ "]"; //$NON-NLS-1$
	}
}
