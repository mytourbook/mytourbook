/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.common.color;

import org.eclipse.swt.graphics.RGB;

public class GraphColorItem {

	private ColorDefinition	_colorDefinition;

	private String			_colorPrefName;
	private String			_visibleName;

	/**
	 * Is <code>true</code> when this {@link GraphColorItem} is used as for a map color.
	 */
	private boolean			_isMapColor;

	public GraphColorItem(	final ColorDefinition colorDefinition,
							final String colorPrefName,
							final String visibleName,
							final boolean isMapColor) {

		_colorDefinition = colorDefinition;

		_colorPrefName = colorPrefName;
		_visibleName = visibleName;

		_isMapColor = isMapColor;
	}

	public ColorDefinition getColorDefinition() {
		return _colorDefinition;
	}

	public String getColorId() {
		return _colorDefinition.getPrefName() + "." + _colorPrefName; //$NON-NLS-1$
	}

	public String getName() {
		return _visibleName;
	}

	public RGB getNewRGB() {

		if (_colorPrefName.compareTo(GraphColorManager.PREF_COLOR_LINE) == 0) {
			return _colorDefinition.getNewLineColor();

		} else if (_colorPrefName.compareTo(GraphColorManager.PREF_COLOR_TEXT) == 0) {
			return _colorDefinition.getNewTextColor();

		} else if (_colorPrefName.compareTo(GraphColorManager.PREF_COLOR_BRIGHT) == 0) {
			return _colorDefinition.getNewGradientBright();

		} else {
			return _colorDefinition.getNewGradientDark();
		}
	}

	String getPrefName() {
		return _colorPrefName;
	}

	/**
	 * @return Returns <code>true</code> when this {@link GraphColorItem} represents a
	 *         {@link Map2ColorProfile}
	 */
	public boolean isMapColor() {
		return _isMapColor;
	}

	public void setName(final String fName) {
		this._visibleName = fName;
	}

	public void setNewRGB(final RGB rgb) {

		if (_colorPrefName.compareTo(GraphColorManager.PREF_COLOR_LINE) == 0) {

			_colorDefinition.setNewLineColor(rgb);

		} else if (_colorPrefName.compareTo(GraphColorManager.PREF_COLOR_TEXT) == 0) {

			_colorDefinition.setNewTextColor(rgb);

		} else if (_colorPrefName.compareTo(GraphColorManager.PREF_COLOR_BRIGHT) == 0) {

			_colorDefinition.setNewGradientBright(rgb);

		} else {
			_colorDefinition.setNewGradientDark(rgb);
		}
	}

	void setPrefName(final String fPrefName) {
		this._colorPrefName = fPrefName;
	}
}
