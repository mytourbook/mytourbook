/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

	private String			fPrefName;
	private String			fVisibleName;

	private GraphColor[]	fColorParts;

	private RGB				fDefaultLineColor;
	private RGB				fDefaultGradientDark;
	private RGB				fDefaultGradientBright;

	private RGB				fLineColor;
	private RGB				fGradientDark;
	private RGB				fGradientBright;

	private RGB				fNewLineColor;
	private RGB				fNewGradientDark;
	private RGB				fNewGradientBright;

	private LegendColor		fLegendColor;

	/**
	 * Sets the color for the default, current and changes
	 * 
	 * @param prefName
	 *        preference name
	 * @param visibleName
	 *        visible name
	 * @param defaultGradientBright
	 *        default bright gradient color
	 * @param defaultGradientDark
	 *        default dark gradient color
	 * @param defaultLineColor
	 *        default line color
	 * @param legendColor
	 *        legend color configuration or <code>null</code> when legend is not available
	 */
	protected ColorDefinition(String prefName, String visibleName, RGB defaultGradientBright, RGB defaultGradientDark,
			RGB defaultLineColor, LegendColor legendColor) {

		fPrefName = prefName;
		fVisibleName = visibleName;

		fDefaultGradientBright = defaultGradientBright;
		fDefaultGradientDark = defaultGradientDark;
		fDefaultLineColor = defaultLineColor;

		fLegendColor = legendColor;

		IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		String graphPrefName = getGraphPrefName();

		/*
		 * gradient bright
		 */
		String prefColorGradientBright = graphPrefName + GraphColorDefaults.PREF_COLOR_BRIGHT;
		if (prefStore.contains(prefColorGradientBright)) {
			fGradientBright = PreferenceConverter.getColor(prefStore, prefColorGradientBright);
		} else {
			fGradientBright = getDefaultGradientBright();
		}
		fNewGradientBright = getGradientBright();

		/*
		 * gradient dark
		 */
		String prefColorGradientDark = graphPrefName + GraphColorDefaults.PREF_COLOR_DARK;
		if (prefStore.contains(prefColorGradientDark)) {
			fGradientDark = PreferenceConverter.getColor(prefStore, prefColorGradientDark);
		} else {
			fGradientDark = getDefaultGradientDark();
		}
		fNewGradientDark = getGradientDark();

		/*
		 * line color
		 */
		String prefColorLine = graphPrefName + GraphColorDefaults.PREF_COLOR_LINE;
		if (prefStore.contains(prefColorLine)) {
			fLineColor = PreferenceConverter.getColor(prefStore, prefColorLine);
		} else {
			fLineColor = getDefaultLineColor();
		}
		fNewLineColor = getLineColor();
	}

	public GraphColor[] getGraphColorParts() {
		return fColorParts;
	}

	public RGB getDefaultGradientBright() {
		return fDefaultGradientBright;
	}

	public RGB getDefaultGradientDark() {
		return fDefaultGradientDark;
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
	public void setColorNames(GraphColor[] children) {
		fColorParts = children;
	}

	public void setGradientBright(RGB fGradientBright) {
		this.fGradientBright = fGradientBright;
	}

	public void setGradientDark(RGB fGradientDark) {
		this.fGradientDark = fGradientDark;
	}

	public void setLineColor(RGB fLineColor) {
		this.fLineColor = fLineColor;
	}

	public void setNewGradientBright(RGB fNewGradientBright) {
		this.fNewGradientBright = fNewGradientBright;
	}

	public void setNewGradientDark(RGB fNewGradientDark) {
		this.fNewGradientDark = fNewGradientDark;
	}

	public void setNewLineColor(RGB fNewLineColor) {
		this.fNewLineColor = fNewLineColor;
	}

	public void setPrefName(String fPrefName) {
		this.fPrefName = fPrefName;
	}

	public void setVisibleName(String fVisibleName) {
		this.fVisibleName = fVisibleName;
	}
}
