/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import java.io.Serializable;

public class SwimData implements Serializable {

	private static final long	serialVersionUID	= 1L;

	public long						absoluteTime;

	/**
	 * Swimming data: Activity e.g. active, idle. Contains {@link Short#MIN_VALUE} when value is not
	 * set.
	 */
	public short					swim_LengthType	= Short.MIN_VALUE;

	/**
	 * Swimming data: Swimming cadence in strokes/min. Contains {@link Short#MIN_VALUE} when value is
	 * not set.
	 */
	public short					swim_Cadence		= Short.MIN_VALUE;

	/**
	 * Swimming data: Number of strokes. Contains {@link Short#MIN_VALUE} when value is not set.
	 */
	public short					swim_Strokes		= Short.MIN_VALUE;

	/**
	 * Swimming data: Stroke style e.g. freestyle, breaststroke. Contains {@link Short#MIN_VALUE}
	 * when value is not set.
	 */
	public short					swim_StrokeStyle	= Short.MIN_VALUE;

}
