/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import net.tourbook.common.CommonActivator;
import net.tourbook.common.preferences.ICommonPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

/**
 * Contains all color for one graph type.
 */
public class ColorDefinition {

	private final IPreferenceStore	_commonPrefStore	= CommonActivator.getPrefStore();

	private String					_prefName;
	private String					_visibleName;

	private GraphColorItem[]		_colorParts;

	private RGB						_lineColor;
	private RGB						_lineColorDefault;
	private RGB						_lineColorNew;

	private RGB						_gradientBright;
	private RGB						_gradientBrightDefault;
	private RGB						_gradientBrightNew;

	private RGB						_gradientDark;
	private RGB						_gradientDarkDefault;
	private RGB						_gradientDarkNew;

	private RGB						_textColor;
	private RGB						_textColorDefault;
	private RGB						_textColorNew;

	/*
	 * One color definition contains different profiles which are used depending on the current
	 * situation.
	 */
	private Map2ColorProfile		_mapColorProfile;
	private Map2ColorProfile		_mapColorProfile_Default;
	private Map2ColorProfile		_mapColorProfile_New;

	/**
	 * Sets the color for the default, current and changes
	 * 
	 * @param prefName
	 *            preference name
	 * @param visibleName
	 *            visible name
	 * @param defaultGradientBright
	 *            default bright gradient color
	 * @param defaultGradientDark
	 *            default dark gradient color
	 * @param defaultLineColor
	 *            default line color
	 * @param defaultTextColor
	 *            default text color
	 * @param defaultMapColorProfile
	 *            Map color configuration or <code>null</code> when not available.
	 */
	protected ColorDefinition(	final String prefName,
								final String visibleName,
								final RGB defaultGradientBright,
								final RGB defaultGradientDark,
								final RGB defaultLineColor,
								final RGB defaultTextColor,
								final Map2ColorProfile defaultMapColorProfile) {

		_prefName = prefName;
		_visibleName = visibleName;

		_gradientBrightDefault = defaultGradientBright;
		_gradientDarkDefault = defaultGradientDark;
		_lineColorDefault = defaultLineColor;
		_textColorDefault = defaultTextColor;

		_mapColorProfile_Default = defaultMapColorProfile;

		final String graphPrefName = getGraphPrefName();

		/*
		 * set gradient bright from pref store or default
		 */
		final String prefColorGradientBright = graphPrefName + GraphColorManager.PREF_COLOR_BRIGHT;
		if (_commonPrefStore.contains(prefColorGradientBright)) {
			_gradientBright = PreferenceConverter.getColor(_commonPrefStore, prefColorGradientBright);
		} else {
			_gradientBright = _gradientBrightDefault;
		}
		_gradientBrightNew = _gradientBright;

		/*
		 * gradient dark
		 */
		final String prefColorGradientDark = graphPrefName + GraphColorManager.PREF_COLOR_DARK;
		if (_commonPrefStore.contains(prefColorGradientDark)) {
			_gradientDark = PreferenceConverter.getColor(_commonPrefStore, prefColorGradientDark);
		} else {
			_gradientDark = _gradientDarkDefault;
		}
		_gradientDarkNew = _gradientDark;

		/*
		 * line color
		 */
		final String prefColorLine = graphPrefName + GraphColorManager.PREF_COLOR_LINE;
		if (_commonPrefStore.contains(prefColorLine)) {
			_lineColor = PreferenceConverter.getColor(_commonPrefStore, prefColorLine);
		} else {
			_lineColor = _lineColorDefault;
		}
		_lineColorNew = _lineColor;

		/*
		 * text color
		 */
		final String prefColorText = graphPrefName + GraphColorManager.PREF_COLOR_TEXT;
		if (_commonPrefStore.contains(prefColorText)) {
			_textColor = PreferenceConverter.getColor(_commonPrefStore, prefColorText);
		} else {
			_textColor = _textColorDefault;
		}
		_textColorNew = _textColor;
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		final ColorDefinition other = (ColorDefinition) obj;
		if (_prefName == null) {
			if (other._prefName != null) {
				return false;
			}
		} else if (!_prefName.equals(other._prefName)) {
			return false;
		}

		return true;
	}

	public RGB getDefaultGradientBright() {
		return _gradientBrightDefault;
	}

	public RGB getDefaultGradientDark() {
		return _gradientDarkDefault;
	}

	public RGB getDefaultLineColor() {
		return _lineColorDefault;
	}

	public Map2ColorProfile getDefaultMapColor() {
		return _mapColorProfile_Default;
	}

	public RGB getDefaultTextColor() {
		return _textColorDefault;
	}

	public RGB getGradientBright() {
		return _gradientBright;
	}

	public RGB getGradientDark() {
		return _gradientDark;
	}

	public GraphColorItem[] getGraphColorParts() {
		return _colorParts;
	}

	public String getGraphPrefName() {
		return ICommonPreferences.GRAPH_COLORS + _prefName + "."; //$NON-NLS-1$
	}

	public String getImageId() {
		return _prefName;
	}

	public RGB getLineColor() {
		return _lineColor;
	}

	public Map2ColorProfile getMapColor() {
		return _mapColorProfile;
	}

	public RGB getNewGradientBright() {
		return _gradientBrightNew;
	}

	public RGB getNewGradientDark() {
		return _gradientDarkNew;
	}

	public RGB getNewLineColor() {
		return _lineColorNew;
	}

	public Map2ColorProfile getNewMapColor() {
		return _mapColorProfile_New;
	}

	public RGB getNewTextColor() {
		return _textColorNew;
	}

	public String getPrefName() {
		return _prefName;
	}

	public RGB getTextColor() {
		return _textColor;
	}

	public String getVisibleName() {
		return _visibleName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_prefName == null) ? 0 : _prefName.hashCode());
		return result;
	}

	/**
	 * set color names for this color definition, the color names are children in the tree
	 * 
	 * @param children
	 */
	public void setColorNames(final GraphColorItem[] children) {
		_colorParts = children;
	}

	public void setGradientBright(final RGB gradientBright) {
		_gradientBright = gradientBright;
	}

	public void setGradientDark(final RGB gradientDark) {
		_gradientDark = gradientDark;
	}

	public void setLineColor(final RGB lineColor) {
		_lineColor = lineColor;
	}

	public void setMapColorProfile(final Map2ColorProfile mapColor) {
		_mapColorProfile = mapColor;
	}

	public void setNewGradientBright(final RGB newGradientBright) {
		_gradientBrightNew = newGradientBright;
	}

	public void setNewGradientDark(final RGB newGradientDark) {
		_gradientDarkNew = newGradientDark;
	}

	public void setNewLineColor(final RGB newLineColor) {
		_lineColorNew = newLineColor;
	}

	public void setNewMapColor(final Map2ColorProfile newMapColor) {
		_mapColorProfile_New = newMapColor;
	}

	public void setNewTextColor(final RGB _textColorNew) {
		this._textColorNew = _textColorNew;
	}

	public void setPrefName(final String prefName) {
		_prefName = prefName;
	}

	public void setTextColor(final RGB textColor) {
		_textColor = textColor;
	}

	public void setVisibleName(final String visibleName) {
		_visibleName = visibleName;
	}

}
