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
package net.tourbook.preferences;

import net.tourbook.colors.ColorDefinition;
import net.tourbook.data.TourType;

import org.eclipse.swt.graphics.RGB;

public class TourTypeColorDefinition extends ColorDefinition implements Comparable<Object> {

	private static final RGB	DEFAULT_GRADIENT_BRIGHT	= new RGB(255, 255, 255);
	private static final RGB	DEFAULT_GRADIENT_DARK	= new RGB(255, 167, 199);
	private static final RGB	DEFAULT_LINE_COLOR		= new RGB(232, 152, 180);
	private static final RGB	DEFAULT_TEXT_COLOR		= new RGB(98, 23, 49);

	private TourType			_tourType;

	/**
	 * Create tour type color definition with default ugly color
	 * 
	 * @param tourType
	 * @param prefName
	 * @param visibleName
	 */
	public TourTypeColorDefinition(final TourType tourType, final String prefName, final String visibleName) {

		// rgb values must be cloned that each tour type has it's own color
		super(prefName, visibleName, //
				new RGB(DEFAULT_GRADIENT_BRIGHT.red, DEFAULT_GRADIENT_BRIGHT.green, DEFAULT_GRADIENT_BRIGHT.blue), //
				new RGB(DEFAULT_GRADIENT_DARK.red, DEFAULT_GRADIENT_DARK.green, DEFAULT_GRADIENT_DARK.blue),
				new RGB(DEFAULT_LINE_COLOR.red, DEFAULT_LINE_COLOR.green, DEFAULT_LINE_COLOR.blue),
				new RGB(DEFAULT_TEXT_COLOR.red, DEFAULT_TEXT_COLOR.green, DEFAULT_TEXT_COLOR.blue),
				null);

		_tourType = tourType;
	}

	public TourTypeColorDefinition(	final TourType tourType,
									final String prefName,
									final String visibleName,
									final RGB defaultGradientBright,
									final RGB defaultGradientDark,
									final RGB defaultLineColor,
									final RGB defaultTextColor) {

		super(prefName, visibleName, //
				defaultGradientBright,
				defaultGradientDark,
				defaultLineColor,
				defaultTextColor,
				null);

		_tourType = tourType;
	}

	public int compareTo(final Object obj) {

		if (obj instanceof TourTypeColorDefinition) {
			final TourTypeColorDefinition otherColorDef = (TourTypeColorDefinition) obj;
			return _tourType.compareTo(otherColorDef.getTourType());
		}

		return 0;
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof TourTypeColorDefinition)) {
			return false;
		}
		final TourTypeColorDefinition other = (TourTypeColorDefinition) obj;
		if (_tourType == null) {
			if (other._tourType != null) {
				return false;
			}
		} else if (!_tourType.equals(other._tourType)) {
			return false;
		}
		return true;
	}

	public TourType getTourType() {
		return _tourType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((_tourType == null) ? 0 : _tourType.hashCode());
		return result;
	}

	public void setTourType(final TourType fTourType) {
		this._tourType = fTourType;
	}
}
