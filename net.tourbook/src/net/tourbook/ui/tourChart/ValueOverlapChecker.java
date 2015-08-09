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

	private Rectangle	HIDDEN_RECT	= new Rectangle(-1000, -1000, 0, 0);

	private int			_numPreviousValues;
	private int			_numStackedValues;

	private Rectangle[]	_previousValues;

	public ValueOverlapChecker(final int numStackedValues) {

		_numPreviousValues = numStackedValues + 10;
		_numStackedValues = numStackedValues;

		_previousValues = new Rectangle[_numPreviousValues];

		// setup checker with rectangles which are not visible
		for (int rectIndex = 0; rectIndex < _previousValues.length; rectIndex++) {
			_previousValues[rectIndex] = HIDDEN_RECT;
		}
	}

	Rectangle getPreviousValue() {
		return _previousValues[0];
	}

	boolean intersectsNoValues(final Rectangle textRect) {

		for (int rectIndex = _numStackedValues; rectIndex < _numPreviousValues; rectIndex++) {
			if (_previousValues[rectIndex].intersects(textRect)) {
				return true;
			}
		}

		return false;
	}

	boolean intersectsWithValues(final Rectangle textRect) {

		for (int rectIndex = 0; rectIndex < _numStackedValues; rectIndex++) {
			if (_previousValues[rectIndex].intersects(textRect)) {
				return true;
			}
		}

		return false;
	}

	void setupNext(final Rectangle textRect) {

		for (int rectIndex = _numPreviousValues - 1; rectIndex > 0; rectIndex--) {
			_previousValues[rectIndex] = _previousValues[rectIndex - 1];
		}

		_previousValues[0] = textRect;
	}
}
