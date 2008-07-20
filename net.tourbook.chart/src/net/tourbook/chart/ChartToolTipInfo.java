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
package net.tourbook.chart;

public class ChartToolTipInfo {

	private String	title;
	private String	label;

	private boolean	fIsDisplayed	= false;
	private boolean	fIsReposition	= false;

	public String getLabel() {
		return label;
	}

	public String getTitle() {
		return title;
	}

	boolean isDisplayed() {
		return fIsDisplayed;
	}

	boolean isReposition() {
		return fIsReposition;
	}

	public void setIsDisplayed(boolean isDisplayed) {
		fIsDisplayed = isDisplayed;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setReposition(boolean isReposition) {
		fIsReposition = isReposition;
	}

}
