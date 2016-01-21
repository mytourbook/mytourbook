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
package net.tourbook.data;

public class TourSegment {

	public int		serieIndexStart;
	public int		serieIndexEnd;

	public int		recordingTime;
	public int		drivingTime;
	public int		breakTime;
	public int		timeTotal;

	public float	distanceDiff;
	public float	distanceTotal;

	public float	altitudeUpHour;
	public float	altitudeUpSummarizedBorder;
	public float	altitudeUpSummarizedComputed;

	public float	altitudeDownHour;
	public float	altitudeDownSummarizedBorder;
	public float	altitudeDownSummarizedComputed;

	public float	altitudeDiffSegmentBorder;
	public float	altitudeDiffSegmentComputed;

	public float	cadence;
	public float	gradient;
	public float	power;
	public float	speed;

	public float	pace;
	public float	paceDiff;
	public float	pulse;
	public float	pulseDiff;
}
