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

package net.tourbook.data;

public class TourSegment {

	public int		serieIndexStart;
	public int		serieIndexEnd;

	public int		recordingTime;
	public int		drivingTime;
	public int		breakTime;
	public int		timeTotal;

	public int		distanceDiff;
	public int		distanceTotal;

	public int		altitudeUp;
	public int		altitudeDown;
	public int		altitudeDiff;

	public int		altitudeUpH;
	public int		altitudeDownH;

	public float	speed;
	public float	gradient;
	public int		power;

	public int		pace;
	public int		paceDiff;
	public int		pulse;
	public int		pulseDiff;

}
