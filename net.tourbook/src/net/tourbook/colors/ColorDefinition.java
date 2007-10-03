/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class ColorDefinition {

	private String			fPrefName;
	private String			fVisibleName;

	private GraphColor[]	fChildren;

	private RGB				fDefaultLineColor;
	private RGB				fDefaultGradientDark;
	private RGB				fDefaultGradientBright;

	private RGB				fLineColor;
	private RGB				fGradientDark;
	private RGB				fGradientBright;

	private RGB				fNewLineColor;
	private RGB				fNewGradientDark;
	private RGB				fNewGradientBright;

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
	 */
	protected ColorDefinition(String prefName, String visibleName,
			RGB defaultGradientBright, RGB defaultGradientDark, RGB defaultLineColor) {

		fPrefName = prefName;
		fVisibleName = visibleName;

		setDefaultGradientBright(defaultGradientBright);
		setDefaultGradientDark(defaultGradientDark);
		setDefaultLineColor(defaultLineColor);

		IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		String graphPrefName = getGraphPrefName();

		/*
		 * gradient bright
		 */
		String prefColorGradientBright = graphPrefName + GraphColors.PREF_COLOR_BRIGHT;
		if (prefStore.contains(prefColorGradientBright)) {
			setGradientBright(PreferenceConverter.getColor(
					prefStore,
					prefColorGradientBright));
		} else {
			setGradientBright(getDefaultGradientBright());
		}
		setNewGradientBright(getGradientBright());

		/*
		 * gradient dark
		 */
		String prefColorGradientDark = graphPrefName + GraphColors.PREF_COLOR_DARK;
		if (prefStore.contains(prefColorGradientDark)) {
			setGradientDark(PreferenceConverter
					.getColor(prefStore, prefColorGradientDark));
		} else {
			setGradientDark(getDefaultGradientDark());
		}
		setNewGradientDark(getGradientDark());

		/*
		 * line color
		 */
		String prefColorLine = graphPrefName + GraphColors.PREF_COLOR_LINE;
		if (prefStore.contains(prefColorLine)) {
			setLineColor(PreferenceConverter.getColor(prefStore, prefColorLine));
		} else {
			setLineColor(getDefaultLineColor());
		}
		setNewLineColor(getLineColor());
	}

	public void setColorNames(GraphColor[] children) {
		fChildren = children;
	}

	public String getGraphPrefName() {
		return ITourbookPreferences.GRAPH_COLORS + fPrefName + "."; //$NON-NLS-1$
	}

	public void setDefaultLineColor(RGB fDefaultLineColor) {
		this.fDefaultLineColor = fDefaultLineColor;
	}

	public RGB getDefaultLineColor() {
		return fDefaultLineColor;
	}

	public void setDefaultGradientDark(RGB fDefaultGradientDark) {
		this.fDefaultGradientDark = fDefaultGradientDark;
	}

	public RGB getDefaultGradientDark() {
		return fDefaultGradientDark;
	}

	public void setDefaultGradientBright(RGB fDefaultGradientBright) {
		this.fDefaultGradientBright = fDefaultGradientBright;
	}

	public RGB getDefaultGradientBright() {
		return fDefaultGradientBright;
	}

	public void setLineColor(RGB fLineColor) {
		this.fLineColor = fLineColor;
	}

	public RGB getLineColor() {
		return fLineColor;
	}

	public void setGradientDark(RGB fGradientDark) {
		this.fGradientDark = fGradientDark;
	}

	public RGB getGradientDark() {
		return fGradientDark;
	}

	public void setGradientBright(RGB fGradientBright) {
		this.fGradientBright = fGradientBright;
	}

	public RGB getGradientBright() {
		return fGradientBright;
	}

	public void setNewLineColor(RGB fNewLineColor) {
		this.fNewLineColor = fNewLineColor;
	}

	public RGB getNewLineColor() {
		return fNewLineColor;
	}

	public void setNewGradientDark(RGB fNewGradientDark) {
		this.fNewGradientDark = fNewGradientDark;
	}

	public RGB getNewGradientDark() {
		return fNewGradientDark;
	}

	public void setNewGradientBright(RGB fNewGradientBright) {
		this.fNewGradientBright = fNewGradientBright;
	}

	public RGB getNewGradientBright() {
		return fNewGradientBright;
	}

	public String getImageId() {
		return fPrefName;
	}

	public void setPrefName(String fPrefName) {
		this.fPrefName = fPrefName;
	}

	public String getPrefName() {
		return fPrefName;
	}

	public void setVisibleName(String fVisibleName) {
		this.fVisibleName = fVisibleName;
	}

	public String getVisibleName() {
		return fVisibleName;
	}

	public void setChildren(GraphColor[] fChildren) {
		this.fChildren = fChildren;
	}

	public GraphColor[] getChildren() {
		return fChildren;
	}
}
