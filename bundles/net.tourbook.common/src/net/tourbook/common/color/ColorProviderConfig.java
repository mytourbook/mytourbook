/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
 * This configuration provides info where the color provider is painting.
 */
public enum ColorProviderConfig {

	/**
	 * Color provider is used to draw a tour
	 */
	MAP3_TOUR,

	/**
	 * Color provider is used to draw without tour data, e.g. a viewer to select color profile.
	 */
	MAP3_PROFILE,

	/**
	 * Color provider is used to draw for the 2D map.
	 */
	MAP2,
}
