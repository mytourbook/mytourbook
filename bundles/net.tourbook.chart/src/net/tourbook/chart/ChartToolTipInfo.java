/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

	private String	_title;
	private String	_label;

	private boolean	_isDisplayed	= false;
	private boolean	_isReposition	= false;

	public String getLabel() {
		return _label;
	}

	public String getTitle() {
		return _title;
	}

	boolean isDisplayed() {
		return _isDisplayed;
	}

	boolean isReposition() {
		return _isReposition;
	}

	public void setIsDisplayed(final boolean isDisplayed) {
		_isDisplayed = isDisplayed;
	}

	public void setLabel(final String label) {
		this._label = label;
	}

	public void setReposition(final boolean isReposition) {
		_isReposition = isReposition;
	}

	public void setTitle(final String title) {
		this._title = title;
	}

}
