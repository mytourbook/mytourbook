/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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

public class TourSegment {

	/** Is <code>true</code> when this segment is the totals segment. */
	public boolean	isTotal;

	public int		serieIndex_Start;
	public int		serieIndex_End;

	public int		time_Recording;
	public int		time_Driving;
	public int		time_Break;
	public int		time_Total;

	public float	distance_Diff;
	public float	distance_Total;

	public float	altitude_Segment_Up;
	public float	altitude_Segment_Down;
	public float	altitude_Segment_Border_Diff;
	public float	altitude_Segment_Computed_Diff;

	public float	altitude_Summarized_Border_Up;
	public float	altitude_Summarized_Border_Down;
	public float	altitude_Summarized_Computed_Up;
	public float	altitude_Summarized_Computed_Down;

	public float	cadence;
	public float	gradient;
	public float	power;
	public float	speed;

	public float	pace;
	public float	pace_Diff;
	public float	pulse;
	public float	pulse_Diff;
}
