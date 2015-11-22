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
package net.tourbook.common.color;

import org.eclipse.swt.graphics.RGB;

public class GraphColorItem {

	private ColorDefinition	_colorDefinition;

	private String			_colorPrefName;
	private String			_visibleName;

	private String			_colorId;

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

		_colorId = _colorDefinition.getColorDefinitionId() + "." + _colorPrefName;
	}

	public ColorDefinition getColorDefinition() {
		return _colorDefinition;
	}

	public String getColorId() {
		return _colorId;
	}

	public String getName() {
		return _visibleName;
	}

	public RGB getRGB() {

		if (GraphColorManager.PREF_COLOR_LINE.equals(_colorPrefName)) {

			return _colorDefinition.getLineColor_New();

		} else if (GraphColorManager.PREF_COLOR_TEXT.equals(_colorPrefName)) {

			return _colorDefinition.getTextColor_New();

		} else if (GraphColorManager.PREF_COLOR_BRIGHT.equals(_colorPrefName)) {

			return _colorDefinition.getGradientBright_New();

		} else {

			return _colorDefinition.getGradientDark_New();
		}
	}

	/**
	 * @return Returns <code>true</code> when this {@link GraphColorItem} represents a
	 *         {@link Map2ColorProfile}
	 */
	public boolean isMapColor() {
		return _isMapColor;
	}

	public void setName(final String name) {
		_visibleName = name;
	}

	public void setRGB(final RGB rgb) {

		if (GraphColorManager.PREF_COLOR_LINE.equals(_colorPrefName)) {

			_colorDefinition.setLineColor_New(rgb);

		} else if (GraphColorManager.PREF_COLOR_TEXT.equals(_colorPrefName)) {

			_colorDefinition.setTextColor_New(rgb);

		} else if (GraphColorManager.PREF_COLOR_BRIGHT.equals(_colorPrefName)) {

			_colorDefinition.setGradientBright_New(rgb);

		} else {

			_colorDefinition.setGradientDark_New(rgb);
		}
	}
}
