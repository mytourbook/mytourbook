/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import net.tourbook.mapping.LegendColor;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class ColorDefinition {

	private String				fPrefName;
	private String				fVisibleName;

	private GraphColorItem[]	fColorParts;

	private RGB					fLineColor;
	private RGB					fDefaultLineColor;
	private RGB					fNewLineColor;

	private RGB					fGradientBright;
	private RGB					fDefaultGradientBright;
	private RGB					fNewGradientBright;

	private RGB					fGradientDark;
	private RGB					fDefaultGradientDark;
	private RGB					fNewGradientDark;

	private LegendColor			fLegendColor;
	private LegendColor			fDefaultLegendColor;
	private LegendColor			fNewLegendColor;

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

		fPrefName = prefName;
		fVisibleName = visibleName;

		fDefaultGradientBright = defaultGradientBright;
		fDefaultGradientDark = defaultGradientDark;
		fDefaultLineColor = defaultLineColor;

		fDefaultLegendColor = defaultLegendColor;

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		final String graphPrefName = getGraphPrefName();

		/*
		 * set gradient bright from pref store or default
		 */
		final String prefColorGradientBright = graphPrefName + GraphColorProvider.PREF_COLOR_BRIGHT;
		if (prefStore.contains(prefColorGradientBright)) {
			fGradientBright = PreferenceConverter.getColor(prefStore, prefColorGradientBright);
		} else {
			fGradientBright = fDefaultGradientBright;
		}
		fNewGradientBright = fGradientBright;

		/*
		 * gradient dark
		 */
		final String prefColorGradientDark = graphPrefName + GraphColorProvider.PREF_COLOR_DARK;
		if (prefStore.contains(prefColorGradientDark)) {
			fGradientDark = PreferenceConverter.getColor(prefStore, prefColorGradientDark);
		} else {
			fGradientDark = fDefaultGradientDark;
		}
		fNewGradientDark = fGradientDark;

		/*
		 * line color
		 */
		final String prefColorLine = graphPrefName + GraphColorProvider.PREF_COLOR_LINE;
		if (prefStore.contains(prefColorLine)) {
			fLineColor = PreferenceConverter.getColor(prefStore, prefColorLine);
		} else {
			fLineColor = fDefaultLineColor;
		}
		fNewLineColor = fLineColor;
	}

	public RGB getDefaultGradientBright() {
		return fDefaultGradientBright;
	}

	public RGB getDefaultGradientDark() {
		return fDefaultGradientDark;
	}

	public LegendColor getDefaultLegendColor() {
		return fDefaultLegendColor;
	}

	public RGB getDefaultLineColor() {
		return fDefaultLineColor;
	}

	public RGB getGradientBright() {
		return fGradientBright;
	}

	public RGB getGradientDark() {
		return fGradientDark;
	}

	public GraphColorItem[] getGraphColorParts() {
		return fColorParts;
	}

	public String getGraphPrefName() {
		return ITourbookPreferences.GRAPH_COLORS + fPrefName + "."; //$NON-NLS-1$
	}

	public String getImageId() {
		return fPrefName;
	}

	public LegendColor getLegendColor() {
		return fLegendColor;
	}

	public RGB getLineColor() {
		return fLineColor;
	}

	public RGB getNewGradientBright() {
		return fNewGradientBright;
	}

	public RGB getNewGradientDark() {
		return fNewGradientDark;
	}

	public LegendColor getNewLegendColor() {
		return fNewLegendColor;
	}

	public RGB getNewLineColor() {
		return fNewLineColor;
	}

	public String getPrefName() {
		return fPrefName;
	}

	public String getVisibleName() {
		return fVisibleName;
	}

	/**
	 * set color names for this color definition, the color names are children in the tree
	 * 
	 * @param children
	 */
	public void setColorNames(final GraphColorItem[] children) {
		fColorParts = children;
	}

	public void setGradientBright(final RGB gradientBright) {
		fGradientBright = gradientBright;
	}

	public void setGradientDark(final RGB gradientDark) {
		fGradientDark = gradientDark;
	}

	public void setLegendColor(final LegendColor legendColor) {
		fLegendColor = legendColor;
	}

	public void setLineColor(final RGB lineColor) {
		fLineColor = lineColor;
	}

	public void setNewGradientBright(final RGB newGradientBright) {
		fNewGradientBright = newGradientBright;
	}

	public void setNewGradientDark(final RGB newGradientDark) {
		fNewGradientDark = newGradientDark;
	}

	public void setNewLegendColor(final LegendColor newLegendColor) {
		fNewLegendColor = newLegendColor;
	}

	public void setNewLineColor(final RGB newLineColor) {
		fNewLineColor = newLineColor;
	}

	public void setPrefName(final String prefName) {
		fPrefName = prefName;
	}

	public void setVisibleName(final String visibleName) {
		fVisibleName = visibleName;
	}
}
