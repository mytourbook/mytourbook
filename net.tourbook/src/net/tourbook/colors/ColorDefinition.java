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
package net.tourbook.colors;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.mapping.LegendColor;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class ColorDefinition {

	private String				_prefName;
	private String				_visibleName;

	private GraphColorItem[]	_colorParts;

	private RGB					_lineColor;
	private RGB					_defaultLineColor;
	private RGB					_newLineColor;

	private RGB					_gradientBright;
	private RGB					_defaultGradientBright;
	private RGB					_newGradientBright;

	private RGB					_gradientDark;
	private RGB					_defaultGradientDark;
	private RGB					_newGradientDark;

	private LegendColor			_legendColor;
	private LegendColor			_defaultLegendColor;
	private LegendColor			_newLegendColor;

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
	 * @param defaultLegendColor
	 *            legend color configuration or <code>null</code> when legend is not available
	 */
	protected ColorDefinition(	final String prefName,
								final String visibleName,
								final RGB defaultGradientBright,
								final RGB defaultGradientDark,
								final RGB defaultLineColor,
								final LegendColor defaultLegendColor) {

		_prefName = prefName;
		_visibleName = visibleName;

		_defaultGradientBright = defaultGradientBright;
		_defaultGradientDark = defaultGradientDark;
		_defaultLineColor = defaultLineColor;

		_defaultLegendColor = defaultLegendColor;

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		final String graphPrefName = getGraphPrefName();

		/*
		 * set gradient bright from pref store or default
		 */
		final String prefColorGradientBright = graphPrefName + GraphColorProvider.PREF_COLOR_BRIGHT;
		if (prefStore.contains(prefColorGradientBright)) {
			_gradientBright = PreferenceConverter.getColor(prefStore, prefColorGradientBright);
		} else {
			_gradientBright = _defaultGradientBright;
		}
		_newGradientBright = _gradientBright;

		/*
		 * gradient dark
		 */
		final String prefColorGradientDark = graphPrefName + GraphColorProvider.PREF_COLOR_DARK;
		if (prefStore.contains(prefColorGradientDark)) {
			_gradientDark = PreferenceConverter.getColor(prefStore, prefColorGradientDark);
		} else {
			_gradientDark = _defaultGradientDark;
		}
		_newGradientDark = _gradientDark;

		/*
		 * line color
		 */
		final String prefColorLine = graphPrefName + GraphColorProvider.PREF_COLOR_LINE;
		if (prefStore.contains(prefColorLine)) {
			_lineColor = PreferenceConverter.getColor(prefStore, prefColorLine);
		} else {
			_lineColor = _defaultLineColor;
		}
		_newLineColor = _lineColor;
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
		return _defaultGradientBright;
	}

	public RGB getDefaultGradientDark() {
		return _defaultGradientDark;
	}

	public LegendColor getDefaultLegendColor() {
		return _defaultLegendColor;
	}

	public RGB getDefaultLineColor() {
		return _defaultLineColor;
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
		return ITourbookPreferences.GRAPH_COLORS + _prefName + "."; //$NON-NLS-1$
	}

	public String getImageId() {
		return _prefName;
	}

	public LegendColor getLegendColor() {
		return _legendColor;
	}

	public RGB getLineColor() {
		return _lineColor;
	}

	public RGB getNewGradientBright() {
		return _newGradientBright;
	}

	public RGB getNewGradientDark() {
		return _newGradientDark;
	}

	public LegendColor getNewLegendColor() {
		return _newLegendColor;
	}

	public RGB getNewLineColor() {
		return _newLineColor;
	}

	public String getPrefName() {
		return _prefName;
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

	public void setLegendColor(final LegendColor legendColor) {
		_legendColor = legendColor;
	}

	public void setLineColor(final RGB lineColor) {
		_lineColor = lineColor;
	}

	public void setNewGradientBright(final RGB newGradientBright) {
		_newGradientBright = newGradientBright;
	}

	public void setNewGradientDark(final RGB newGradientDark) {
		_newGradientDark = newGradientDark;
	}

	public void setNewLegendColor(final LegendColor newLegendColor) {
		_newLegendColor = newLegendColor;
	}

	public void setNewLineColor(final RGB newLineColor) {
		_newLineColor = newLineColor;
	}

	public void setPrefName(final String prefName) {
		_prefName = prefName;
	}

	public void setVisibleName(final String visibleName) {
		_visibleName = visibleName;
	}
}
