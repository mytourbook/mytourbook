/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
 * Contains all colors for one graph type.
 */
public class ColorDefinition {

	private final IPreferenceStore	_commonPrefStore	= CommonActivator.getPrefStore();

	private String					_colorDefinitionId;
	private String					_visibleName;
	private String					_graphPrefNamePrefix;

	/**
	 * These are children in the tree viewer.
	 */
	private GraphColorItem[]		_graphColorItems;

	private RGB						_lineColor_Active;
	private RGB						_lineColor_Default;
	private RGB						_lineColor_New;

	private RGB						_gradientBright_Active;
	private RGB						_gradientBright_Default;
	private RGB						_gradientBright_New;

	private RGB						_gradientDark_Active;
	private RGB						_gradientDark_Default;
	private RGB						_gradientDark_New;

	private RGB						_textColor_Active;
	private RGB						_textColor_Default;
	private RGB						_textColor_New;

	/*
	 * One color definition contains different profiles which are used depending on the current
	 * situation.
	 */
	private Map2ColorProfile		_map2ColorProfile_Active;

	private Map2ColorProfile		_map2ColorProfile_Default;
	private Map2ColorProfile		_map2ColorProfile_New;

	/**
	 * Sets the color for the default, current and changes
	 * 
	 * @param colorDefinitionId
	 *            Unique id
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
	protected ColorDefinition(	final String colorDefinitionId,
								final String visibleName,
								final RGB defaultGradientBright,
								final RGB defaultGradientDark,
								final RGB defaultLineColor,
								final RGB defaultTextColor,
								final Map2ColorProfile defaultMapColorProfile) {

		_colorDefinitionId = colorDefinitionId;
		_visibleName = visibleName;

		_gradientBright_Default = defaultGradientBright;
		_gradientDark_Default = defaultGradientDark;
		_lineColor_Default = defaultLineColor;
		_textColor_Default = defaultTextColor;

		_map2ColorProfile_Default = defaultMapColorProfile;

		_graphPrefNamePrefix = ICommonPreferences.GRAPH_COLORS + _colorDefinitionId + "."; //$NON-NLS-1$

		/*
		 * set gradient bright from pref store or default
		 */
		final String prefColorGradientBright = getGraphPrefName(GraphColorManager.PREF_COLOR_BRIGHT);

		if (_commonPrefStore.contains(prefColorGradientBright)) {
			_gradientBright_Active = PreferenceConverter.getColor(_commonPrefStore, prefColorGradientBright);
		} else {
			_gradientBright_Active = _gradientBright_Default;
		}
		_gradientBright_New = _gradientBright_Active;

		/*
		 * gradient dark
		 */
		final String prefColorGradientDark = getGraphPrefName(GraphColorManager.PREF_COLOR_DARK);

		if (_commonPrefStore.contains(prefColorGradientDark)) {
			_gradientDark_Active = PreferenceConverter.getColor(_commonPrefStore, prefColorGradientDark);
		} else {
			_gradientDark_Active = _gradientDark_Default;
		}
		_gradientDark_New = _gradientDark_Active;

		/*
		 * line color
		 */
		final String prefColorLine = getGraphPrefName(GraphColorManager.PREF_COLOR_LINE);

		if (_commonPrefStore.contains(prefColorLine)) {
			_lineColor_Active = PreferenceConverter.getColor(_commonPrefStore, prefColorLine);
		} else {
			_lineColor_Active = _lineColor_Default;
		}
		_lineColor_New = _lineColor_Active;

		/*
		 * text color
		 */
		final String prefColorText = getGraphPrefName(GraphColorManager.PREF_COLOR_TEXT);

		if (_commonPrefStore.contains(prefColorText)) {
			_textColor_Active = PreferenceConverter.getColor(_commonPrefStore, prefColorText);
		} else {
			_textColor_Active = _textColor_Default;
		}
		_textColor_New = _textColor_Active;
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
		if (_colorDefinitionId == null) {
			if (other._colorDefinitionId != null) {
				return false;
			}
		} else if (!_colorDefinitionId.equals(other._colorDefinitionId)) {
			return false;
		}

		return true;
	}

	public String getColorDefinitionId() {
		return _colorDefinitionId;
	}

	public RGB getGradientBright_Active() {
		return _gradientBright_Active;
	}

	public RGB getGradientBright_Default() {
		return _gradientBright_Default;
	}

	public RGB getGradientBright_New() {
		return _gradientBright_New;
	}

	public RGB getGradientDark_Active() {
		return _gradientDark_Active;
	}

	public RGB getGradientDark_Default() {
		return _gradientDark_Default;
	}

	public RGB getGradientDark_New() {
		return _gradientDark_New;
	}

	public GraphColorItem[] getGraphColorItems() {
		return _graphColorItems;
	}

	public String getGraphPrefName(final String graphColorName) {

		final String graphPrefName = _graphPrefNamePrefix + graphColorName;

		return graphPrefName;
	}

	public RGB getLineColor_Active() {
		return _lineColor_Active;
	}

	public RGB getLineColor_Default() {
		return _lineColor_Default;
	}

	public RGB getLineColor_New() {
		return _lineColor_New;
	}

	public Map2ColorProfile getMap2Color_Active() {
		return _map2ColorProfile_Active;
	}

	public Map2ColorProfile getMap2Color_Default() {
		return _map2ColorProfile_Default;
	}

	public Map2ColorProfile getMap2Color_New() {
		return _map2ColorProfile_New;
	}

	public RGB getTextColor_Active() {
		return _textColor_Active;
	}

	public RGB getTextColor_Default() {
		return _textColor_Default;
	}

	public RGB getTextColor_New() {
		return _textColor_New;
	}

	public String getVisibleName() {
		return _visibleName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_colorDefinitionId == null) ? 0 : _colorDefinitionId.hashCode());
		return result;
	}

	private String logRGB(final RGB rgb) {

//		new RGB(0x5B, 0x5B, 0x5B),

		if (rgb == null) {
			return "null"; //$NON-NLS-1$
		}

		return "new RGB(" //$NON-NLS-1$
				+ "0x" + Integer.toHexString(rgb.red) + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "0x" + Integer.toHexString(rgb.green) + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "0x" + Integer.toHexString(rgb.blue) //$NON-NLS-1$
				+ "),"; //$NON-NLS-1$
	}

	/**
	 * Set color names for this color definition, the color names are children in the tree.
	 * 
	 * @param children
	 */
	public void setColorNames(final GraphColorItem[] children) {
		_graphColorItems = children;
	}

	public void setGradientBright_Active(final RGB gradientBright) {
		_gradientBright_Active = gradientBright;
	}

	public void setGradientBright_New(final RGB newGradientBright) {
		_gradientBright_New = newGradientBright;
	}

	public void setGradientDark_Active(final RGB gradientDark) {
		_gradientDark_Active = gradientDark;
	}

	public void setGradientDark_New(final RGB newGradientDark) {
		_gradientDark_New = newGradientDark;
	}

	public void setLineColor_Active(final RGB lineColor) {
		_lineColor_Active = lineColor;
	}

	public void setLineColor_New(final RGB newLineColor) {
		_lineColor_New = newLineColor;
	}

	public void setMap2Color_Active(final Map2ColorProfile mapColor) {
		_map2ColorProfile_Active = mapColor;
	}

	public void setMap2Color_New(final Map2ColorProfile newMapColor) {
		_map2ColorProfile_New = newMapColor;
	}

	public void setTextColor_Active(final RGB textColor) {
		_textColor_Active = textColor;
	}

	public void setTextColor_New(final RGB _textColorNew) {
		_textColor_New = _textColorNew;
	}

	public void setVisibleName(final String visibleName) {
		_visibleName = visibleName;
	}

	@Override
	public String toString() {

// SET_FORMATTING_OFF
		
		return "\nColorDefinition	\n" //$NON-NLS-1$

				+ "_colorDefinitionId			=" + _colorDefinitionId 		+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//				+ "_visibleName					=" + _visibleName 				+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//				+ "_graphPrefNamePrefix			=" + _graphPrefNamePrefix 		+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//				+ "_graphColorItems				=" + Arrays.toString(_graphColorItems) + "\n" //$NON-NLS-1$ //$NON-NLS-2$
//
//				+ "_lineColor_Active			=" + _lineColor_Active 			+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//				+ "_lineColor_New				=" + _lineColor_New 			+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//
//				+ "_gradientBright_Active		=" + _gradientBright_Active 	+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//				+ "_gradientBright_New			=" + _gradientBright_New 		+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//
//				+ "_gradientDark_Active			=" + _gradientDark_Active 		+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//				+ "_gradientDark_New			=" + _gradientDark_New 			+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//
//				+ "_textColor_Active			=" + _textColor_Active 			+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//				+ "_textColor_New				=" + _textColor_New 			+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//
//				+ "_map2ColorProfile_Active		=" + _map2ColorProfile_Active 	+ "\n" //$NON-NLS-1$ //$NON-NLS-2$
//				+ "_map2ColorProfile_New		=" + _map2ColorProfile_New 		+ "\n" //$NON-NLS-1$

				// Java code

				+ logRGB(_gradientBright_New)	+ "\n"	// _gradientBright_Default		= "  //$NON-NLS-1$
				+ logRGB(_gradientDark_New)		+ "\n"	// _gradientDark_Default		= "  //$NON-NLS-1$
				+ logRGB(_lineColor_New) 		+ "\n"	// _lineColor_Default			= "  //$NON-NLS-1$
				+ logRGB(_textColor_New) 		+ "\n"	// _textColor_Default			= "  //$NON-NLS-1$
				
//				+ "_map2ColorProfile_Default	= " + "\n" //$NON-NLS-1$ //$NON-NLS-2$
//				+ "]"; //$NON-NLS-1$
		;
// SET_FORMATTING_ON
	}
}
