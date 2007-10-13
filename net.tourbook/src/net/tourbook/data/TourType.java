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
package net.tourbook.data;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.eclipse.swt.graphics.RGB;

@Entity
public class TourType {

	public static final int	TOUR_TYPE_ID_NOT_DEFINED	= -1;
	public static final int	TOUR_TYPE_ID_ALL			= -2;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long			typeId						= TOUR_TYPE_ID_NOT_DEFINED;

	@Basic(optional = false)
	private String			name;

	private short			colorBrightRed;
	private short			colorBrightGreen;
	private short			colorBrightBlue;

	private short			colorDarkRed;
	private short			colorDarkGreen;
	private short			colorDarkBlue;

	private short			colorLineRed;
	private short			colorLineGreen;
	private short			colorLineBlue;

	/**
	 * default constructor used in ejb
	 */
	public TourType() {}

	public TourType(String name) {
		this.name = name;
	}

	public TourType(String name, int typeId) {
		this.name = name;
		this.typeId = typeId;
	}

	/**
	 * @return Returns the name for the tour type
	 */
	public String getName() {
		return name;
	}

	public RGB getRGBBright() {
		return new RGB(colorBrightRed, colorBrightGreen, colorBrightBlue);
	}

	public RGB getRGBDark() {
		return new RGB(colorDarkRed, colorDarkGreen, colorDarkBlue);
	}

	public RGB getRGBLine() {
		return new RGB(colorLineRed, colorLineGreen, colorLineBlue);
	}

	/**
	 * @return Returns the type id, this can be the saved type id or it can be
	 *         {@link TourType#TOUR_TYPE_ID_ALL} or {@link TourType#TOUR_TYPE_ID_NOT_DEFINED}
	 */
	public long getTypeId() {
		return typeId;
	}

	public void setColorBright(RGB rgbBright) {
		colorBrightRed = (short) rgbBright.red;
		colorBrightGreen = (short) rgbBright.green;
		colorBrightBlue = (short) rgbBright.blue;
	}

	public void setColorDark(RGB rgbDark) {
		colorDarkRed = (short) rgbDark.red;
		colorDarkGreen = (short) rgbDark.green;
		colorDarkBlue = (short) rgbDark.blue;
	}

	public void setColorLine(RGB rgbLine) {
		colorLineRed = (short) rgbLine.red;
		colorLineGreen = (short) rgbLine.green;
		colorLineBlue = (short) rgbLine.blue;
	}

	public void setName(String name) {
		this.name = name;
	}

}
