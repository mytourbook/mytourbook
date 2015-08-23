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
 * @author Wolfgang Schramm Created: 08.08.2015
 */
package net.tourbook.ui.tourChart;

import org.eclipse.swt.graphics.Rectangle;

/**
 * Implemented this algorithm first with simplified rectangles then with an array.
 */
public class ValueOverlapChecker {

	private Rectangle	INITIAL_HIDDEN_RECT	= new Rectangle(-1000, -1000, 0, 0);

	/**
	 * Contains all (previous) areas.
	 */
	private int			_numRects;

	/**
	 * Contains areas which are checked and rearranged.
	 */
	private int			_numPreviousValues;

	private Rectangle[]	_prevValues;

	public ValueOverlapChecker(final int numStackedValues) {

		_numRects = numStackedValues + 20;

		// +1 is necessary otherwise the segmenter values are not displayed
		_numPreviousValues = numStackedValues + 1;

		_prevValues = new Rectangle[_numRects];

		// setup checker with rectangles which are not visible
		for (int rectIndex = 0; rectIndex < _numRects; rectIndex++) {
			_prevValues[rectIndex] = INITIAL_HIDDEN_RECT;
		}
	}

	Rectangle getValidRect(	final Rectangle textRect,
							final boolean isValueUp,
							final int textHeight,
							final String valueText) {

		Rectangle validRect = null;

		int yDiff;
		if (isValueUp) {
			yDiff = -textHeight;
		} else {
			yDiff = textHeight;
		}

		/*
		 * Debugging
		 */
		if (valueText.equals("13")) {
			int a = 0;
			a++;
		}

		int rectCounter = 0;
		for (int rectIndex = 0; rectIndex < _numRects; rectIndex++) {

			final Rectangle prevRect = _prevValues[rectIndex];

			if (prevRect.intersects(textRect)) {

				textRect.y = prevRect.y + yDiff;

				validRect = null;
				rectIndex = -1;

				rectCounter++;
				if (rectCounter >= _numPreviousValues) {
					return validRect;
				}

			} else {

				validRect = textRect;
			}
		}

		return validRect;
	}

	void setupNext(final Rectangle textRect, final boolean isValueUp) {

		for (int rectIndex = _numRects - 1; rectIndex > 0; rectIndex--) {
			_prevValues[rectIndex] = _prevValues[rectIndex - 1];
		}

		_prevValues[0] = textRect;
	}
}
