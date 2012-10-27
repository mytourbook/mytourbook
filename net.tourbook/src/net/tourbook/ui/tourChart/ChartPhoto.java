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

import net.tourbook.photo.PhotoWrapper;

public class ChartPhoto {

	public PhotoWrapper	photoWrapper;

	/**
	 * Value on the x-axis
	 */
	public double		xValue;

	/**
	 * Index in the data serie where a photo occures, there can be multiple photos at the same
	 * position.
	 */
	public int			serieIndex;

	/**
	 * @param photoWrapper
	 * @param xValue
	 *            Value on the x-axis
	 * @param serieIndex
	 */
	public ChartPhoto(final PhotoWrapper photoWrapper, final double xValue, final int serieIndex) {

		this.photoWrapper = photoWrapper;
		this.xValue = xValue;
		this.serieIndex = serieIndex;
	}

	@Override
	public String toString() {
		return "ChartPhoto xValue="
				+ xValue
				+ "{)}, serieIndex="
				+ serieIndex
				+ "{)}, photoWrapper="
				+ photoWrapper
				;
	}

}
