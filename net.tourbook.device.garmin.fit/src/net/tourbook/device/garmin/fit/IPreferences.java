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
package net.tourbook.device.garmin.fit;

public interface IPreferences {

	String	FIT_TEMPERATURE_ADJUSTMENT			= "FIT_TEMPERATURE_ADJUSTMENT";		//$NON-NLS-1$

	String	FIT_IS_IGNORE_LAST_MARKER			= "FIT_IS_IGNORE_LAST_MARKER";			//$NON-NLS-1$
	String	FIT_IGNORE_LAST_MARKER_TIME_SLICES	= "FIT_IGNORE_LAST_MARKER_TIME_SLICES"; //$NON-NLS-1$

	String	FIT_IS_REMOVE_EXCEEDED_TIME_SLICE	= "FIT_IS_REMOVE_EXCEEDED_TIME_SLICE";	//$NON-NLS-1$
	String	FIT_EXCEEDED_TIME_SLICE_DURATION	= "FIT_EXCEEDED_TIME_SLICE_DURATION";	//$NON-NLS-1$

}
