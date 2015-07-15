/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

public class ChartTitle {

	public Long	tourId;

	public int	devX;
	public int	devY;

	public int	width;
	public int	height;

	public int	devYBottom;
	public int	devYTop;
	public int	devGraphWidth;

	public ChartTitle() {}

	@Override
	public String toString() {
		return "ChartTitle ["
				+ ("devX=" + devX + ", ")
				+ ("devY=" + devY + ", ")
				+ ("width=" + width + ", ")
				+ ("height=" + height)
				+ "]";
	}

}
