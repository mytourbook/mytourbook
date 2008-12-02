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

package net.tourbook.data;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import net.tourbook.database.TourDatabase;

import org.eclipse.swt.graphics.RGB;

@Entity
public class TourType {

	/**
	 * contains the entity id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long	typeId	= TourDatabase.ENTITY_IS_NOT_SAVED;

	@Basic(optional = false)
	private String	name;

	private short	colorBrightRed;
	private short	colorBrightGreen;
	private short	colorBrightBlue;

	private short	colorDarkRed;
	private short	colorDarkGreen;
	private short	colorDarkBlue;

	private short	colorLineRed;
	private short	colorLineGreen;
	private short	colorLineBlue;

	/**
	 * default constructor used in ejb
	 */
	public TourType() {}

	public TourType(final String name) {
		this.name = name;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof TourType) {
			return ((TourType) obj).typeId == typeId;
		}
		return false;
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
	 * @return Returns the type id, this can be the saved type id or
	 *         {@link TourDatabase#ENTITY_IS_NOT_SAVED}
	 */
	public long getTypeId() {
		return typeId;
	}

	public void setColorBright(final RGB rgbBright) {
		colorBrightRed = (short) rgbBright.red;
		colorBrightGreen = (short) rgbBright.green;
		colorBrightBlue = (short) rgbBright.blue;
	}

	public void setColorDark(final RGB rgbDark) {
		colorDarkRed = (short) rgbDark.red;
		colorDarkGreen = (short) rgbDark.green;
		colorDarkBlue = (short) rgbDark.blue;
	}

	public void setColorLine(final RGB rgbLine) {
		colorLineRed = (short) rgbLine.red;
		colorLineGreen = (short) rgbLine.green;
		colorLineBlue = (short) rgbLine.blue;
	}

	public void setName(final String name) {
		this.name = name;
	}

}
