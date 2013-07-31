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
import org.eclipse.swt.graphics.RGB;

public class TourTrackConfig {

	private static final String			STATE_ALTITUDE_MODE				= "STATE_ALTITUDE_MODE";			//$NON-NLS-1$
	private static final String			STATE_ALTITUDE_OFFSET			= "STATE_ALTITUDE_OFFSET";			//$NON-NLS-1$
	private static final String			STATE_IS_ABSOLUTE_OFFSET		= "STATE_IS_ABSOLUTE_OFFSET";		//$NON-NLS-1$
	private static final String			STATE_IS_DRAW_VERTICALS			= "STATE_IS_DRAW_VERTICALS";		//$NON-NLS-1$
	private static final String			STATE_IS_EXTRUDE_PATH			= "STATE_IS_EXTRUDE_PATH";			//$NON-NLS-1$
	private static final String			STATE_IS_FOLLOW_TERRAIN			= "STATE_IS_FOLLOW_TERRAIN";		//$NON-NLS-1$
	private static final String			STATE_IS_SHOW_TRACK_POSITION	= "STATE_IS_SHOW_TRACK_POSITION";	//$NON-NLS-1$
	private static final String			STATE_INTERIOR_COLOR			= "STATE_INTERIOR_COLOR";			//$NON-NLS-1$
	private static final String			STATE_INTERIOR_OPACITY			= "STATE_INTERIOR_OPACITY";		//$NON-NLS-1$
	private static final String			STATE_NUMBER_OF_SUB_SEGMENTS	= "STATE_NUMBER_OF_SUB_SEGMENTS";	//$NON-NLS-1$
	private static final String			STATE_OUTLINE_COLOR				= "STATE_OUTLINE_COLOR";			//$NON-NLS-1$
	private static final String			STATE_OUTLINE_OPACITY			= "STATE_OUTLINE_OPACITY";			//$NON-NLS-1$
	private static final String			STATE_OUTLINE_WIDTH				= "STATE_OUTLINE_WIDTH";			//$NON-NLS-1$
	private static final String			STATE_PATH_TYPE					= "STATE_PATH_TYPE";				//$NON-NLS-1$
	private static final String			STATE_TRACK_POSITION_SIZE		= "STATE_TRACK_POSITION_SIZE";		//$NON-NLS-1$

	public static final AltitudeMode[]	ALTITUDE_MODE					= {
			new AltitudeMode(Messages.Track_Config_Altitude_Mode_ClampToGround, WorldWind.CLAMP_TO_GROUND),
			new AltitudeMode(Messages.Track_Config_Altitude_Mode_Absolute, WorldWind.ABSOLUTE),
			new AltitudeMode(Messages.Track_Config_Altitude_Mode_RelativeToGround, WorldWind.RELATIVE_TO_GROUND) //
																		};

	public static final PathType[]		PATH_TYPE						= {
			new PathType(Messages.Track_Config_Path_Type_Linear, AVKey.LINEAR),
			new PathType(Messages.Track_Config_Path_Type_GreatCircle, AVKey.GREAT_CIRCLE),
			new PathType(Messages.Track_Config_Path_Type_RHumbLine, AVKey.RHUMB_LINE), //
																		};

	/**
	 * Recreate tracks when <code>true</code>.
	 */
	public boolean						isRecreateTracks;

	/*
	 * Configurations which are saved in the state.
	 */
	public int							altitudeMode;

	/**
	 * Vertical distance in m (meter).
	 */
	public int							altitudeOffsetDistance;

	/**
	 * Is <code>true</code> when altitude offset is enabled.
	 */
	public boolean						isAbsoluteOffset;
	public boolean						isExtrudePath;
	public boolean						isDrawVerticals;
	public boolean						isFollowTerrain;
	public boolean						isShowTrackPosition;

	public RGB							interiorColor;
	public double						interiorOpacity;

	public RGB							outlineColor;
	public double						outlineOpacity;
	public double						outlineWidth;

	public String						pathType;
	public double						trackPositionSize;
	public int							numSubsegments;

	TourTrackConfig(final IDialogSettings state) {

		restoreState(state);
	}

	/**
	 * @return Returns altitude mode index for the current altitude mode value.
	 */
	public int getAltitudeModeIndex() {

		for (int valueIndex = 0; valueIndex < ALTITUDE_MODE.length; valueIndex++) {
			if (ALTITUDE_MODE[valueIndex].value == altitudeMode) {
				return valueIndex;
			}
		}

		// return default value
		return 0;
	}

	private int getAltitudeModeValue(final int stateAltitudeMode) {

		for (final AltitudeMode altiMode : ALTITUDE_MODE) {
			if (altiMode.value == stateAltitudeMode) {
				return altiMode.value;
			}
		}

		// return default value
		return WorldWind.CLAMP_TO_GROUND;
	}

	public int getPathTypeIndex() {

		for (int valueIndex = 0; valueIndex < PATH_TYPE.length; valueIndex++) {
			if (PATH_TYPE[valueIndex].value.equals(pathType)) {
				return valueIndex;
			}
		}

		// return default value
		return 0;
	}

	private String getPathTypeValue(final String statePathType) {

		for (final PathType pathType : PATH_TYPE) {
			if (pathType.value.equals(statePathType)) {
				return pathType.value;
			}
		}

		// return default value
		return AVKey.LINEAR;
	}

	/**
	 * Set track configuration with default values.
	 */
	public void reset() {

		restoreState(null);

		isRecreateTracks = true;
	}

	/**
	 * Set track configuration from state, when not available set defaults.
	 * 
	 * @param state
	 */
	private void restoreState(final IDialogSettings state) {
//
//		outlineWidth = Util.getStateDouble(state, STATE_OUTLINE_WIDTH, 5.0);
//
//		// altitude
//		altitudeMode = getAltitudeModeValue(Util.getStateInt(state, STATE_ALTITUDE_MODE, WorldWind.CLAMP_TO_GROUND));
//		isAbsoluteOffset = Util.getStateBoolean(state, STATE_IS_ABSOLUTE_OFFSET, false);
//		altitudeOffsetDistance = Util.getStateInt(state, STATE_ALTITUDE_OFFSET, 0);
//		isFollowTerrain = Util.getStateBoolean(state, STATE_IS_FOLLOW_TERRAIN, true);
//
//		// track position
//		isShowTrackPosition = Util.getStateBoolean(state, STATE_IS_SHOW_TRACK_POSITION, false);
//		trackPositionSize = Util.getStateDouble(state, STATE_TRACK_POSITION_SIZE, 2.0);
//
//		// curtain
//		isExtrudePath = Util.getStateBoolean(state, STATE_IS_EXTRUDE_PATH, false);
//		interiorColor = Util.getStateColor(state, STATE_INTERIOR_COLOR, new RGB(0xFF, 0xE3, 0x5D));
//		interiorOpacity = Util.getStateDouble(state, STATE_INTERIOR_OPACITY, 0.5);
//
//		// verticals
//		isDrawVerticals = Util.getStateBoolean(state, STATE_IS_DRAW_VERTICALS, false);
//		outlineColor = Util.getStateColor(state, STATE_OUTLINE_COLOR, new RGB(0xFA, 0xEA, 0x9B));
//		outlineOpacity = Util.getStateDouble(state, STATE_OUTLINE_OPACITY, 0.5);
//
//		// path + segments
//		pathType = getPathTypeValue(Util.getStateString(state, STATE_PATH_TYPE, AVKey.LINEAR));
//		numSubsegments = Util.getStateInt(state, STATE_NUMBER_OF_SUB_SEGMENTS, 0);

		outlineWidth = Util.getStateDouble(state, STATE_OUTLINE_WIDTH, 1.0);

		// altitude
		altitudeMode = getAltitudeModeValue(Util.getStateInt(state, STATE_ALTITUDE_MODE, WorldWind.ABSOLUTE));
		isAbsoluteOffset = Util.getStateBoolean(state, STATE_IS_ABSOLUTE_OFFSET, true);
		altitudeOffsetDistance = Util.getStateInt(state, STATE_ALTITUDE_OFFSET, 50);
		isFollowTerrain = Util.getStateBoolean(state, STATE_IS_FOLLOW_TERRAIN, true);

		// track position
		isShowTrackPosition = Util.getStateBoolean(state, STATE_IS_SHOW_TRACK_POSITION, true);
		trackPositionSize = Util.getStateDouble(state, STATE_TRACK_POSITION_SIZE, 5.0);

		// curtain
		isExtrudePath = Util.getStateBoolean(state, STATE_IS_EXTRUDE_PATH, true);
		interiorColor = Util.getStateColor(state, STATE_INTERIOR_COLOR, new RGB(0xFF, 0xE3, 0x5D));
		interiorOpacity = Util.getStateDouble(state, STATE_INTERIOR_OPACITY, 0.0);

		// verticals
		isDrawVerticals = Util.getStateBoolean(state, STATE_IS_DRAW_VERTICALS, true);
		outlineColor = Util.getStateColor(state, STATE_OUTLINE_COLOR, new RGB(0xFF, 0xFF, 0xFF));
		outlineOpacity = Util.getStateDouble(state, STATE_OUTLINE_OPACITY, 0.1);

		// path + segments
		pathType = getPathTypeValue(Util.getStateString(state, STATE_PATH_TYPE, AVKey.LINEAR));
		numSubsegments = Util.getStateInt(state, STATE_NUMBER_OF_SUB_SEGMENTS, 0);
	}

	void saveState(final IDialogSettings state) {

		state.put(STATE_ALTITUDE_MODE, altitudeMode);
		state.put(STATE_ALTITUDE_OFFSET, altitudeOffsetDistance);

		state.put(STATE_IS_ABSOLUTE_OFFSET, isAbsoluteOffset);
		state.put(STATE_IS_DRAW_VERTICALS, isDrawVerticals);
		state.put(STATE_IS_EXTRUDE_PATH, isExtrudePath);
		state.put(STATE_IS_FOLLOW_TERRAIN, isFollowTerrain);

		state.put(STATE_INTERIOR_OPACITY, interiorOpacity);
		Util.setState(state, STATE_INTERIOR_COLOR, interiorColor);

		state.put(STATE_OUTLINE_OPACITY, outlineOpacity);
		state.put(STATE_OUTLINE_WIDTH, outlineWidth);
		Util.setState(state, STATE_OUTLINE_COLOR, outlineColor);

		state.put(STATE_IS_SHOW_TRACK_POSITION, isShowTrackPosition);
		state.put(STATE_TRACK_POSITION_SIZE, trackPositionSize);

		state.put(STATE_PATH_TYPE, pathType);
		state.put(STATE_NUMBER_OF_SUB_SEGMENTS, numSubsegments);
	}

}
