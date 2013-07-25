/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.layer.tourtrack;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Messages;

import org.eclipse.jface.dialogs.IDialogSettings;

public class TourTrackConfig {

	private static final String		STATE_ALTITUDE_MODE				= "STATE_ALTITUDE_MODE";			//$NON-NLS-1$
	private static final String		STATE_ALTITUDE_OFFSET			= "STATE_ALTITUDE_OFFSET";			//$NON-NLS-1$
	private static final String		STATE_IS_DRAW_VERTICALS			= "STATE_IS_DRAW_VERTICALS";		//$NON-NLS-1$
	private static final String		STATE_IS_EXTRUDE_PATH			= "STATE_IS_EXTRUDE_PATH";			//$NON-NLS-1$
	private static final String		STATE_IS_FOLLOW_TERRAIN			= "STATE_IS_FOLLOW_TERRAIN";		//$NON-NLS-1$
	private static final String		STATE_IS_SHOW_TRACK_POSITION	= "STATE_IS_SHOW_TRACK_POSITION";	//$NON-NLS-1$
	private static final String		STATE_INTERIOR_OPACITY			= "STATE_INTERIOR_OPACITY";		//$NON-NLS-1$
	private static final String		STATE_OUTLINE_OPACITY			= "STATE_OUTLINE_OPACITY";			//$NON-NLS-1$
	private static final String		STATE_OUTLINE_WIDTH				= "STATE_OUTLINE_WIDTH";			//$NON-NLS-1$
	private static final String		STATE_PATH_TYPE					= "STATE_PATH_TYPE";				//$NON-NLS-1$
	private static final String		STATE_TRACK_POSITION_SIZE		= "STATE_TRACK_POSITION_SIZE";		//$NON-NLS-1$

	/*
	 * Altitude mode
	 */
	public static final String[]	ALTITUDE_MODE_LABEL				= {
			Messages.Track_Config_Altitude_Mode_Absolute,
			Messages.Track_Config_Altitude_Mode_ClampToGround,
			Messages.Track_Config_Altitude_Mode_RelativeToGround,	};

	public static final int[]		ALTITUDE_MODE_VALUE				= {
			WorldWind.ABSOLUTE,
			WorldWind.CLAMP_TO_GROUND,
			WorldWind.RELATIVE_TO_GROUND,							};

	/*
	 * Path type
	 */
	public static final String[]	PATH_TYPE_LABEL					= {
			Messages.Track_Config_Path_Type_Linear,
			Messages.Track_Config_Path_Type_GreatCircle,
			Messages.Track_Config_Path_Type_RHumbLine,				};

	public static final String[]	PATH_TYPE_VALUE					= { //
																	AVKey.LINEAR, //
			AVKey.GREAT_CIRCLE,
			AVKey.RHUMB_LINE,										};

	public boolean					isRecreateTracks;

	/*
	 * Configurations which are saved in the state.
	 */
	public int						altitudeMode;
	public int						altitudeOffset;

	public boolean					isExtrudePath;
	public boolean					isDrawVerticals;
	public boolean					isFollowTerrain;
	public boolean					isShowTrackPosition;

	public String					pathType;
	public double					interiorOpacity;
//	public double					outlineOpacity;
	public double					outlineWidth;
	public double					trackPositionSize;

	TourTrackConfig(final IDialogSettings state) {

		restoreState(state);
	}

	/**
	 * @return Returns altitude mode index for the current altitude mode value.
	 */
	public int getAltitudeModeIndex() {

		for (int valueIndex = 0; valueIndex < ALTITUDE_MODE_VALUE.length; valueIndex++) {
			if (ALTITUDE_MODE_VALUE[valueIndex] == altitudeMode) {
				return valueIndex;
			}
		}

		// return default value
		return 0;
	}

	private int getAltitudeModeValue(final int stateAltitudeMode) {

		for (final int altiModeValue : ALTITUDE_MODE_VALUE) {
			if (altiModeValue == stateAltitudeMode) {
				return altiModeValue;
			}
		}

		// return default value
		return WorldWind.CLAMP_TO_GROUND;
	}

	public int getPathTypeIndex() {

		for (int valueIndex = 0; valueIndex < PATH_TYPE_VALUE.length; valueIndex++) {
			if (PATH_TYPE_VALUE[valueIndex].equals(pathType)) {
				return valueIndex;
			}
		}

		// return default value
		return 0;
	}

	private String getPathTypeValue(final String statePathType) {

		for (final String pathTypeValue : PATH_TYPE_VALUE) {
			if (pathTypeValue.equals(statePathType)) {
				return pathTypeValue;
			}
		}

		// return default value
		return AVKey.LINEAR;
	}

	/**
	 * Set track configuration from state, when not available set defaults
	 * 
	 * @param state
	 */
	private void restoreState(final IDialogSettings state) {

		altitudeMode = getAltitudeModeValue(Util.getStateInt(state, STATE_ALTITUDE_MODE, WorldWind.CLAMP_TO_GROUND));
		altitudeOffset = Util.getStateInt(state, STATE_ALTITUDE_OFFSET, 0);

		isDrawVerticals = Util.getStateBoolean(state, STATE_IS_DRAW_VERTICALS, false);
		isExtrudePath = Util.getStateBoolean(state, STATE_IS_EXTRUDE_PATH, false);
		isFollowTerrain = Util.getStateBoolean(state, STATE_IS_FOLLOW_TERRAIN, true);
		isShowTrackPosition = Util.getStateBoolean(state, STATE_IS_SHOW_TRACK_POSITION, false);

		interiorOpacity = Util.getStateDouble(state, STATE_INTERIOR_OPACITY, 0.5);
//		outlineOpacity = Util.getStateDouble(state, STATE_OUTLINE_OPACITY, 0.5);
		outlineWidth = Util.getStateDouble(state, STATE_OUTLINE_WIDTH, 5.0);
		pathType = getPathTypeValue(Util.getStateString(state, STATE_PATH_TYPE, AVKey.LINEAR));
		trackPositionSize = Util.getStateDouble(state, STATE_TRACK_POSITION_SIZE, 5.0);
	}

	void saveState(final IDialogSettings state) {

		state.put(STATE_ALTITUDE_MODE, altitudeMode);
		state.put(STATE_ALTITUDE_OFFSET, altitudeOffset);

		state.put(STATE_IS_DRAW_VERTICALS, isDrawVerticals);
		state.put(STATE_IS_EXTRUDE_PATH, isExtrudePath);
		state.put(STATE_IS_FOLLOW_TERRAIN, isFollowTerrain);
		state.put(STATE_IS_SHOW_TRACK_POSITION, isShowTrackPosition);

		state.put(STATE_INTERIOR_OPACITY, interiorOpacity);
//		state.put(STATE_OUTLINE_OPACITY, outlineOpacity);
		state.put(STATE_OUTLINE_WIDTH, outlineWidth);
		state.put(STATE_PATH_TYPE, pathType);
		state.put(STATE_TRACK_POSITION_SIZE, trackPositionSize);

	}

}
