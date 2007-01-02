/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

import org.eclipse.swt.graphics.RGB;

public class GraphColor {

	private ColorDefinition	fParent;

	private String			fPrefName;
	private String			fName;

	public GraphColor(ColorDefinition parent, String prefName, String name) {

		fParent = parent;

		fPrefName = prefName;
		fName = name;
	}

	public String getColorId() {
		return fParent.getPrefName() + "." + fPrefName;
	}

	RGB getDefaultRGB() {
		return fPrefName.compareTo(GraphColors.PREF_COLOR_LINE) == 0
				? fParent.getDefaultLineColor()
				: fPrefName.compareTo(GraphColors.PREF_COLOR_DARK) == 0 ? fParent
						.getDefaultGradientDark() : fParent.getDefaultGradientBright();
	}

	RGB getRGB() {
		return fPrefName.compareTo(GraphColors.PREF_COLOR_LINE) == 0 ? fParent
				.getLineColor() : fPrefName.compareTo(GraphColors.PREF_COLOR_DARK) == 0
				? fParent.getGradientDark()
				: fParent.getGradientBright();
	}

	public RGB getNewRGB() {
		return fPrefName.compareTo(GraphColors.PREF_COLOR_LINE) == 0
				? fParent.getNewLineColor()
				: fPrefName.compareTo(GraphColors.PREF_COLOR_DARK) == 0 ? fParent
						.getNewGradientDark() : fParent.getNewGradientBright();
	}

	public void setNewRGB(RGB rgb) {
		if (fPrefName.compareTo(GraphColors.PREF_COLOR_LINE) == 0) {
			fParent.setNewLineColor(rgb);
		} else if (fPrefName.compareTo(GraphColors.PREF_COLOR_DARK) == 0) {
			fParent.setNewGradientDark(rgb);
		} else {
			fParent.setNewGradientBright(rgb);
		}
	}

	public void setParent(ColorDefinition fParent) {
		this.fParent = fParent;
	}

	public ColorDefinition getParent() {
		return fParent;
	}

	void setPrefName(String fPrefName) {
		this.fPrefName = fPrefName;
	}

	String getPrefName() {
		return fPrefName;
	}

	public void setName(String fName) {
		this.fName = fName;
	}

	public String getName() {
		return fName;
	}
}
