/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import net.tourbook.photo.Photo;

public class ChartPhoto {

	public final Photo	photo;

	/**
	 * Value on the x-axis
	 */
	public final double	xValue;

	/**
	 * Index in the data serie where a photo occures, there can be multiple photos at the same
	 * position.
	 */
	public final int	serieIndex;

	/**
	 * @param photo
	 * @param xValue
	 *            Value on the x-axis
	 * @param serieIndex
	 */
	public ChartPhoto(final Photo photo, final double xValue, final int serieIndex) {

		this.photo = photo;
		this.xValue = xValue;
		this.serieIndex = serieIndex;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ChartPhoto)) {
			return false;
		}
		final ChartPhoto other = (ChartPhoto) obj;
		if (photo == null) {
			if (other.photo != null) {
				return false;
			}
		} else if (!photo.equals(other.photo)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((photo == null) ? 0 : photo.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "ChartPhoto ["
				+ (" photo=" + photo + "{)},")
				+ (" xValue=" + xValue + "{)},")
				+ (" serieIndex=" + serieIndex)
				+ "]"
		//
		;
	}

}
