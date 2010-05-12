/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

	private TourType			_tourType;

	/**
	 * Create tour type color definition with default ugly color
	 * 
	 * @param tourType
	 * @param prefName
	 * @param visibleName
	 */
	public TourTypeColorDefinition(final TourType tourType, final String prefName, final String visibleName) {

		super(prefName, visibleName, DEFAULT_GRADIENT_BRIGHT, DEFAULT_GRADIENT_DARK, DEFAULT_LINE_COLOR, null);

		_tourType = tourType;
	}

	public TourTypeColorDefinition(	final TourType tourType,
									final String prefName,
									final String visibleName,
									final RGB defaultGradientBright,
									final RGB defaultGradientDark,
									final RGB defaultLineColor) {

		super(prefName, visibleName, defaultGradientBright, defaultGradientDark, defaultLineColor, null);

		_tourType = tourType;
	}

	public int compareTo(final Object obj) {

		if (obj instanceof TourTypeColorDefinition) {
			final TourTypeColorDefinition otherColorDef = (TourTypeColorDefinition) obj;
			return _tourType.compareTo(otherColorDef.getTourType());
		}

		return 0;
	}

	public TourType getTourType() {
		return _tourType;
	}

	public void setTourType(final TourType fTourType) {
		this._tourType = fTourType;
	}
}
