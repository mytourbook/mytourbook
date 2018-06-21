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
package net.tourbook.common.color;

/**
 * Graph id for tour tracks which are displayed in a 2D/3D map.
 * <p>
 * This id defines, which tour data are displayed, e.g. altitude, speed...
 */
public enum MapGraphId {

	Altitude,
	//
	Gradient,
	//
	Pulse,
	//
	Speed,
	//
	Pace,
	//
	HrZone,

	// other graph id's
	Altimeter,

	Cadence,

	Power,

	Temperature,

	/** Running dynamics */
//	StepLength,
}
