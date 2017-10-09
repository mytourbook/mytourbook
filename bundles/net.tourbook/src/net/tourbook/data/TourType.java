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
package net.tourbook.data;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;

import org.eclipse.swt.graphics.RGB;

@Entity
public class TourType implements Comparable<Object> {

	public static final int		DB_LENGTH_NAME				= 100;

	/** Width/height of the tour type image. */
	public static final int		TOUR_TYPE_IMAGE_SIZE		= 16;

	/** Color which is transparent in the tour type image. */
	public static final RGB		TRANSPARENT_COLOR			= new RGB(0x01, 0xfe, 0x00);

	public static final long	IMAGE_KEY_DIALOG_SELECTION	= -2;

	/**
	 * manually created marker or imported marker create a unique id to identify them, saved marker
	 * are compared with the marker id
	 */
	private static int			_createCounter				= 0;

	/**
	 * contains the entity id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long				typeId						= TourDatabase.ENTITY_IS_NOT_SAVED;

	@Basic(optional = false)
	private String				name;
	private short				colorBrightRed;
	private short				colorBrightGreen;

	private short				colorBrightBlue;
	private short				colorDarkRed;
	private short				colorDarkGreen;

	private short				colorDarkBlue;
	private short				colorLineRed;
	private short				colorLineGreen;

	private short				colorLineBlue;
	private short				colorTextRed;
	private short				colorTextGreen;

	private short				colorTextBlue;

	/**
	 * unique id for manually created tour types because the {@link #typeId} is -1 when it's not
	 * persisted
	 */
	@Transient
	private long				_createId					= 0;

	/**
	 * default constructor used in ejb
	 */
	public TourType() {}

	public TourType(final String name) {

		this.name = name;

		_createId = ++_createCounter;
	}

	@Override
	public int compareTo(final Object other) {

		// default sorting for tour types is by name

		if (other instanceof TourType) {
			final TourType otherTourType = (TourType) other;
			return name.compareTo(otherTourType.getName());
		}

		return 0;
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

		final TourType other = (TourType) obj;

		if (_createId == 0) {

			// tour type is from the database
			if (typeId != other.typeId) {
				return false;
			}
		} else {

			// tour type was create or imported
			if (_createId != other._createId) {
				return false;
			}
		}

		return true;
	}

	public long getCreateId() {
		return _createId;
	}

	/**
	 * @return Returns the name for the tour type
	 */
	public String getName() {
		return name;
	}

	public RGB getRGBBright() {

//		final int red = (colorBrightRed + colorDarkRed) / 2;
//		final int green = (colorBrightGreen + colorDarkGreen) / 2;
//		final int blue = (colorBrightBlue + colorDarkBlue) / 2;
//
//		return new RGB(red, green, blue);

		return new RGB(colorBrightRed, colorBrightGreen, colorBrightBlue);
	}

	public RGB getRGBDark() {

//		final int red = (colorBrightRed + colorDarkRed) / 2;
//		final int green = (colorBrightGreen + colorDarkGreen) / 2;
//		final int blue = (colorBrightBlue + colorDarkBlue) / 2;
//
//		return new RGB(red, green, blue);

		return new RGB(colorDarkRed, colorDarkGreen, colorDarkBlue);
	}

	public RGB getRGBLine() {
		return new RGB(colorLineRed, colorLineGreen, colorLineBlue);
	}

	public RGB getRGBText() {
		return new RGB(colorTextRed, colorTextGreen, colorTextBlue);
	}

	/**
	 * @return Returns the type id, this can be the saved type id or
	 *         {@link TourDatabase#ENTITY_IS_NOT_SAVED}
	 */
	public long getTypeId() {
		return typeId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_createId ^ (_createId >>> 32));
		result = prime * result + (int) (typeId ^ (typeId >>> 32));
		return result;
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

	/**
	 * @param bright
	 * @param dark
	 * @param line
	 * @param text
	 */
	public void setColors(final RGB bright, final RGB dark, final RGB line, final RGB text) {

		setColorBright(bright);
		setColorDark(dark);
		setColorLine(line);
		setColorText(text);
	}

	public void setColorText(final RGB rgbText) {
		colorTextRed = (short) rgbText.red;
		colorTextGreen = (short) rgbText.green;
		colorTextBlue = (short) rgbText.blue;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "TourType [typeId=" + typeId + ", name=" + name + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
