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
package net.tourbook.common.preferences;

public interface ICommonPreferences {

	/*
	 * Colors
	 */
	public static final String	GRAPH_COLORS							= "graph.colors.";							//$NON-NLS-1$

	public static final String	DISPLAY_FORMAT_ALTITUDE					= "DISPLAY_FORMAT_ALTITUDE";				//$NON-NLS-1$
	public static final String	DISPLAY_FORMAT_CADENCE					= "DISPLAY_FORMAT_CADENCE";				//$NON-NLS-1$
	public static final String	DISPLAY_FORMAT_DISTANCE					= "DISPLAY_FORMAT_DISTANCE";				//$NON-NLS-1$
	public static final String	DISPLAY_FORMAT_POWER					= "DISPLAY_FORMAT_POWER";					//$NON-NLS-1$
	public static final String	DISPLAY_FORMAT_PULSE					= "DISPLAY_FORMAT_PULSE";					//$NON-NLS-1$
	public static final String	DISPLAY_FORMAT_SPEED					= "DISPLAY_FORMAT_SPEED";					//$NON-NLS-1$

	public static final String	DISPLAY_FORMAT_DRIVING_TIME				= "DISPLAY_FORMAT_DRIVING_TIME";			//$NON-NLS-1$
	public static final String	DISPLAY_FORMAT_PAUSED_TIME				= "DISPLAY_FORMAT_PAUSED_TIME";			//$NON-NLS-1$
	public static final String	DISPLAY_FORMAT_RECORDING_TIME			= "DISPLAY_FORMAT_RECORDING_TIME";			//$NON-NLS-1$

	public static final String	DISPLAY_FORMAT_IS_LIVE_UPDATE			= "DISPLAY_FORMAT_IS_LIVE_UPDATE";			//$NON-NLS-1$

	/*
	 * Timezone
	 */
	public static final String	TIME_ZONE_ACTIVE_ZONE					= "TIME_ZONE_ACTIVE_ZONE";					//$NON-NLS-1$
	public static final String	TIME_ZONE_IS_LIVE_UPDATE				= "TIME_ZONE_IS_LIVE_UPDATE";				//$NON-NLS-1$
	public static final String	TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE		= "TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE";		//$NON-NLS-1$
	public static final String	TIME_ZONE_LOCAL_ID						= "TIME_ZONE_LOCAL_ID";					//$NON-NLS-1$
	public static final String	TIME_ZONE_LOCAL_ID_1					= "TIME_ZONE_LOCAL_ID_1";					//$NON-NLS-1$
	public static final String	TIME_ZONE_LOCAL_ID_2					= "TIME_ZONE_LOCAL_ID_2";					//$NON-NLS-1$
	public static final String	TIME_ZONE_LOCAL_ID_3					= "TIME_ZONE_LOCAL_ID_3";					//$NON-NLS-1$

	/*
	 * Calendar week
	 */
	/** MO=1 .. SO=7 */
	public static final String	CALENDAR_WEEK_FIRST_DAY_OF_WEEK			= "CALENDAR_WEEK_FIRST_DAY_OF_WEEK";		//$NON-NLS-1$
	public static final String	CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK	= "CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK";	//$NON-NLS-1$

}
