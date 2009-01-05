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
package net.tourbook.mapping;

import net.tourbook.data.TourData;

import org.eclipse.jface.viewers.ISelection;

public class SelectionMapPosition implements ISelection {

	private boolean		centerSliderPosition;
	private TourData	tourData;
	private int			valueIndex1;
	private int			valueIndex2;

	public SelectionMapPosition(final TourData tourData,
								final int valueIndex1,
								final int valueIndex2,
								final boolean centerSliderPosition) {

		this.tourData = tourData;
		this.valueIndex1 = valueIndex1;
		this.valueIndex2 = valueIndex2;
		this.centerSliderPosition = centerSliderPosition;
	}

	public int getSlider1ValueIndex() {
		return valueIndex1;
	}

	public int getSlider2ValueIndex() {
		return valueIndex2;
	}

	public TourData getTourData() {
		return tourData;
	}

	public boolean isCenterSliderPosition() {
		return centerSliderPosition;
	}

	public boolean isEmpty() {
		return false;
	}

}
