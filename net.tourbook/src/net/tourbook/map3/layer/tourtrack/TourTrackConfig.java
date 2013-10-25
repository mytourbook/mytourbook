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
import net.tourbook.common.util.Util;
import net.tourbook.map3.Messages;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.RGB;

public class TourTrackConfig {

	private static final String			STATE_ALTITUDE_MODE						= "STATE_ALTITUDE_MODE";						//$NON-NLS-1$
	private static final String			STATE_ALTITUDE_OFFSET					= "STATE_ALTITUDE_OFFSET";						//$NON-NLS-1$
	private static final String			STATE_DIRECTION_ARROW_VERTICAL_DISTANCE	= "STATE_DIRECTION_ARROW_VERTICAL_DISTANCE";	//$NON-NLS-1$
	private static final String			STATE_DIRECTION_ARROW_SIZE				= "STATE_DIRECTION_ARROW_SIZE";				//$NON-NLS-1$
	private static final String			STATE_IS_ABSOLUTE_OFFSET				= "STATE_IS_ABSOLUTE_OFFSET";					//$NON-NLS-1$
	private static final String			STATE_IS_DRAW_VERTICALS					= "STATE_IS_DRAW_VERTICALS";					//$NON-NLS-1$
	private static final String			STATE_IS_EXTRUDE_PATH					= "STATE_IS_EXTRUDE_PATH";						//$NON-NLS-1$
	private static final String			STATE_IS_FOLLOW_TERRAIN					= "STATE_IS_FOLLOW_TERRAIN";					//$NON-NLS-1$
	private static final String			STATE_IS_SHOW_TRACK_POSITION			= "STATE_IS_SHOW_TRACK_POSITION";				//$NON-NLS-1$
	private static final String			STATE_INTERIOR_COLOR					= "STATE_INTERIOR_COLOR";						//$NON-NLS-1$
	private static final String			STATE_INTERIOR_COLOR_HOVERED			= "STATE_INTERIOR_COLOR_HOVERED";				//$NON-NLS-1$
	private static final String			STATE_INTERIOR_COLOR_HOV_SEL			= "STATE_INTERIOR_COLOR_HOV_SEL";				//$NON-NLS-1$
	private static final String			STATE_INTERIOR_COLOR_SELECTED			= "STATE_INTERIOR_COLOR_SELECTED";				//$NON-NLS-1$
	private static final String			STATE_INTERIOR_COLOR_MODE				= "STATE_INTERIOR_COLOR_MODE";					//$NON-NLS-1$
	private static final String			STATE_INTERIOR_COLOR_MODE_HOVERED		= "STATE_INTERIOR_COLOR_MODE_HOVERED";			//$NON-NLS-1$
	private static final String			STATE_INTERIOR_COLOR_MODE_HOVSEL		= "STATE_INTERIOR_COLOR_MODE_HOVSEL";			//$NON-NLS-1$
	private static final String			STATE_INTERIOR_COLOR_MODE_SELECTED		= "STATE_INTERIOR_COLOR_MODE_SELECTED";		//$NON-NLS-1$
	private static final String			STATE_INTERIOR_OPACITY					= "STATE_INTERIOR_OPACITY";					//$NON-NLS-1$
	private static final String			STATE_INTERIOR_OPACITY_HOVERED			= "STATE_INTERIOR_OPACITY_HOVERED";			//$NON-NLS-1$
	private static final String			STATE_INTERIOR_OPACITY_HOV_SEL			= "STATE_INTERIOR_OPACITY_HOV_SEL";			//$NON-NLS-1$
	private static final String			STATE_INTERIOR_OPACITY_SELECTED			= "STATE_INTERIOR_OPACITY_SELECTED";			//$NON-NLS-1$
	private static final String			STATE_NUMBER_OF_SUB_SEGMENTS			= "STATE_NUMBER_OF_SUB_SEGMENTS";				//$NON-NLS-1$
	private static final String			STATE_OUTLINE_COLOR						= "STATE_OUTLINE_COLOR";						//$NON-NLS-1$
	private static final String			STATE_OUTLINE_COLOR_HOVERED				= "STATE_OUTLINE_COLOR_HOVERED";				//$NON-NLS-1$
	private static final String			STATE_OUTLINE_COLOR_HOV_SEL				= "STATE_OUTLINE_COLOR_HOV_SEL";				//$NON-NLS-1$
	private static final String			STATE_OUTLINE_COLOR_SELECTED			= "STATE_OUTLINE_COLOR_SELECTED";				//$NON-NLS-1$
	private static final String			STATE_OUTLINE_COLOR_MODE				= "STATE_OUTLINE_COLOR_MODE";					//$NON-NLS-1$
	private static final String			STATE_OUTLINE_COLOR_MODE_HOVERED		= "STATE_OUTLINE_COLOR_MODE_HOVERED";			//$NON-NLS-1$
	private static final String			STATE_OUTLINE_COLOR_MODE_HOVSEL			= "STATE_OUTLINE_COLOR_MODE_HOVSEL";			//$NON-NLS-1$
	private static final String			STATE_OUTLINE_COLOR_MODE_SELECTED		= "STATE_OUTLINE_COLOR_MODE_SELECTED";			//$NON-NLS-1$
	private static final String			STATE_OUTLINE_OPACITY					= "STATE_OUTLINE_OPACITY";						//$NON-NLS-1$
	private static final String			STATE_OUTLINE_OPACITY_HOVERED			= "STATE_OUTLINE_OPACITY_HOVERED";				//$NON-NLS-1$
	private static final String			STATE_OUTLINE_OPACITY_HOV_SEL			= "STATE_OUTLINE_OPACITY_HOV_SEL";				//$NON-NLS-1$
	private static final String			STATE_OUTLINE_OPACITY_SELECTED			= "STATE_OUTLINE_OPACITY_SELECTED";			//$NON-NLS-1$
	private static final String			STATE_OUTLINE_WIDTH						= "STATE_OUTLINE_WIDTH";						//$NON-NLS-1$
	private static final String			STATE_PATH_RESOLUTION					= "STATE_PATH_RESOLUTION";						//$NON-NLS-1$
	private static final String			STATE_TRACK_POSITION_SIZE				= "STATE_TRACK_POSITION_SIZE";					//$NON-NLS-1$
	private static final String			STATE_TRACK_POSITION_SIZE_HOVERED		= "STATE_TRACK_POSITION_SIZE_HOVERED";			//$NON-NLS-1$
	private static final String			STATE_TRACK_POSITION_SIZE_SELECTED		= "STATE_TRACK_POSITION_SIZE_SELECTED";		//$NON-NLS-1$
	private static final String			STATE_TRACK_POSITION_THRESHOLD			= "STATE_TRACK_POSITION_THRESHOLD";			//$NON-NLS-1$

	/*
	 * Altitude mode
	 */
	public static final ComboEntry[]	ALTITUDE_MODE							= {
			new ComboEntry(Messages.Track_Config_Altitude_Mode_Absolute, WorldWind.ABSOLUTE),
			new ComboEntry(Messages.Track_Config_Altitude_Mode_ClampToGround, WorldWind.CLAMP_TO_GROUND),
			new ComboEntry(Messages.Track_Config_Altitude_Mode_RelativeToGround, WorldWind.RELATIVE_TO_GROUND) //
																				};

	/*
	 * Path resolution
	 */
	public static final int				PATH_RESOLUTION_OPTIMIZED				= 0;
	public static final int				PATH_RESOLUTION_ALL_POSITIONS			= 1;
	public static final int				PATH_RESOLUTION_VIEWPORT				= 2;

	public static final ComboEntry[]	PATH_RESOLUTION							= {
			new ComboEntry(Messages.Track_Config_Path_Resolution_Optimized, PATH_RESOLUTION_OPTIMIZED),//
			new ComboEntry(Messages.Track_Config_Path_Resolution_High, PATH_RESOLUTION_ALL_POSITIONS),
			new ComboEntry(Messages.Track_Config_Path_Resolution_Viewport, PATH_RESOLUTION_VIEWPORT),//
																				//
																				};
	/*
	 * Tour track color mode
	 */

	/**
	 * Track is painted with track value color.
	 */
	public static final int				COLOR_MODE_TRACK_VALUE					= 0;

	/**
	 * Track is painted with a solid color.
	 */
	public static final int				COLOR_MODE_SOLID_COLOR					= 1;

	public static final ComboEntry[]	TRACK_COLOR_MODE						= {
			new ComboEntry(Messages.Track_Config_TrackColorMode_Value, COLOR_MODE_TRACK_VALUE),
			new ComboEntry(Messages.Track_Config_TrackColorMode_Solid, COLOR_MODE_SOLID_COLOR),//
																				};

	/**
	 * Recreate tracks when <code>true</code>.
	 */
	public boolean						isRecreateTracks;

	/*
	 * Configurations which are saved in the state.
	 */

	public int							pathResolution;

	public int							altitudeMode;

	/**
	 * Vertical distance in m (meter).
	 */
	public int							altitudeVerticalOffset;

	/**
	 * Is <code>true</code> when altitude offset is enabled.
	 */
	public boolean						isAbsoluteOffset;
	public boolean						isExtrudePath;
	public boolean						isDrawVerticals;
	public boolean						isFollowTerrain;
	public boolean						isShowTrackPosition;

	public double						outlineWidth;

	public int							outlineColorMode;
	public int							outlineColorMode_Hovered;
	public int							outlineColorMode_HovSel;
	public int							outlineColorMode_Selected;
	public RGB							outlineColor;
	public RGB							outlineColor_Hovered;
	public RGB							outlineColor_HovSel;
	public RGB							outlineColor_Selected;
	public double						outlineOpacity;
	public double						outlineOpacity_Hovered;
	public double						outlineOpacity_HovSel;
	public double						outlineOpacity_Selected;

	public RGB							interiorColor;
	public int							interiorColorMode;
	public int							interiorColorMode_Hovered;
	public int							interiorColorMode_HovSel;
	public int							interiorColorMode_Selected;
	public RGB							interiorColor_Hovered;
	public RGB							interiorColor_HovSel;
	public RGB							interiorColor_Selected;
	public double						interiorOpacity;
	public double						interiorOpacity_Hovered;
	public double						interiorOpacity_HovSel;
	public double						interiorOpacity_Selected;

	// UI is currently disabled, subsegments == 0
	public int							numSubsegments;

	public double						directionArrowDistance;
	public double						directionArrowSize;
	public double						trackPositionSize;
	public double						trackPositionSize_Hovered;
	public double						trackPositionSize_Selected;
	public double						trackPositionThreshold;

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

		for (final ComboEntry altiMode : ALTITUDE_MODE) {
			if (altiMode.value == stateAltitudeMode) {
				return altiMode.value;
			}
		}

		// return default value
		return WorldWind.CLAMP_TO_GROUND;
	}

	public int getColorModeIndex(final int colorMode) {

		for (int valueIndex = 0; valueIndex < TRACK_COLOR_MODE.length; valueIndex++) {
			if (TRACK_COLOR_MODE[valueIndex].value == colorMode) {
				return valueIndex;
			}
		}

		// return default value
		return 0;
	}

	public int getPathResolutionIndex() {

		for (int valueIndex = 0; valueIndex < PATH_RESOLUTION.length; valueIndex++) {
			if (PATH_RESOLUTION[valueIndex].value == pathResolution) {
				return valueIndex;
			}
		}

		// return default value
		return 0;
	}

	/**
	 * Set track configuration with default values.
	 */
	public void reset() {

		// restore from default values
		restoreState(null);

		isRecreateTracks = true;
	}

	/**
	 * Set track configuration from state, when not available set defaults.
	 * 
	 * @param state
	 */
	private void restoreState(final IDialogSettings state) {

		final RGB defaultRGB = new RGB(0xFF, 0xFF, 0x0);
		final RGB hoveredRGB = new RGB(0x0, 0xFF, 0xff);
		final RGB hovSelRGB = new RGB(0xff, 0x0, 0xff);
		final RGB selectedRGB = new RGB(0xFF, 0xff, 0xff);

		directionArrowDistance = Util.getStateDouble(state, STATE_DIRECTION_ARROW_VERTICAL_DISTANCE, 2.0);
		directionArrowSize = Util.getStateDouble(state, STATE_DIRECTION_ARROW_SIZE, 40.0);

		pathResolution = Util.getStateInt(state, STATE_PATH_RESOLUTION, PATH_RESOLUTION_OPTIMIZED);

		// outline
		outlineWidth = Util.getStateDouble(state, STATE_OUTLINE_WIDTH, 1.0);
		outlineColorMode = Util.getStateInt(state, STATE_OUTLINE_COLOR_MODE, COLOR_MODE_TRACK_VALUE);
		outlineColorMode_Hovered = Util.getStateInt(state, STATE_OUTLINE_COLOR_MODE_HOVERED, COLOR_MODE_TRACK_VALUE);
		outlineColorMode_HovSel = Util.getStateInt(state, STATE_OUTLINE_COLOR_MODE_HOVSEL, COLOR_MODE_TRACK_VALUE);
		outlineColorMode_Selected = Util.getStateInt(state, STATE_OUTLINE_COLOR_MODE_SELECTED, COLOR_MODE_TRACK_VALUE);
		outlineColor = Util.getStateColor(state, STATE_OUTLINE_COLOR, defaultRGB);
		outlineColor_Hovered = Util.getStateColor(state, STATE_OUTLINE_COLOR_HOVERED, hoveredRGB);
		outlineColor_HovSel = Util.getStateColor(state, STATE_OUTLINE_COLOR_HOV_SEL, hovSelRGB);
		outlineColor_Selected = Util.getStateColor(state, STATE_OUTLINE_COLOR_SELECTED, selectedRGB);
		outlineOpacity = Util.getStateDouble(state, STATE_OUTLINE_OPACITY, 0.7);
		outlineOpacity_Hovered = Util.getStateDouble(state, STATE_OUTLINE_OPACITY_HOVERED, 1.0);
		outlineOpacity_HovSel = Util.getStateDouble(state, STATE_OUTLINE_OPACITY_HOV_SEL, 1.0);
		outlineOpacity_Selected = Util.getStateDouble(state, STATE_OUTLINE_OPACITY_SELECTED, 1.0);

		// curtain
		isExtrudePath = Util.getStateBoolean(state, STATE_IS_EXTRUDE_PATH, true);
		interiorColorMode = Util.getStateInt(state, STATE_INTERIOR_COLOR_MODE, COLOR_MODE_TRACK_VALUE);
		interiorColorMode_Hovered = Util.getStateInt(state, STATE_INTERIOR_COLOR_MODE_HOVERED, COLOR_MODE_TRACK_VALUE);
		interiorColorMode_HovSel = Util.getStateInt(state, STATE_INTERIOR_COLOR_MODE_HOVSEL, COLOR_MODE_TRACK_VALUE);
		interiorColorMode_Selected = Util
				.getStateInt(state, STATE_INTERIOR_COLOR_MODE_SELECTED, COLOR_MODE_TRACK_VALUE);
		interiorColor = Util.getStateColor(state, STATE_INTERIOR_COLOR, defaultRGB);
		interiorColor_Hovered = Util.getStateColor(state, STATE_INTERIOR_COLOR_HOVERED, hoveredRGB);
		interiorColor_HovSel = Util.getStateColor(state, STATE_INTERIOR_COLOR_HOV_SEL, hovSelRGB);
		interiorColor_Selected = Util.getStateColor(state, STATE_INTERIOR_COLOR_SELECTED, selectedRGB);
		interiorOpacity = Util.getStateDouble(state, STATE_INTERIOR_OPACITY, 0.0);
		interiorOpacity_Hovered = Util.getStateDouble(state, STATE_INTERIOR_OPACITY_HOVERED, 0.2);
		interiorOpacity_HovSel = Util.getStateDouble(state, STATE_INTERIOR_OPACITY_HOV_SEL, 0.2);
		interiorOpacity_Selected = Util.getStateDouble(state, STATE_INTERIOR_OPACITY_SELECTED, 0.2);

		// altitude
		altitudeMode = getAltitudeModeValue(Util.getStateInt(state, STATE_ALTITUDE_MODE, WorldWind.ABSOLUTE));
		isAbsoluteOffset = Util.getStateBoolean(state, STATE_IS_ABSOLUTE_OFFSET, true);
		altitudeVerticalOffset = Util.getStateInt(state, STATE_ALTITUDE_OFFSET, 50);
		isFollowTerrain = Util.getStateBoolean(state, STATE_IS_FOLLOW_TERRAIN, true);

		// track position
		isShowTrackPosition = Util.getStateBoolean(state, STATE_IS_SHOW_TRACK_POSITION, true);
		trackPositionSize = Util.getStateDouble(state, STATE_TRACK_POSITION_SIZE, 8.0);
		trackPositionSize_Hovered = Util.getStateDouble(state, STATE_TRACK_POSITION_SIZE_HOVERED, 12.0);
		trackPositionSize_Selected = Util.getStateDouble(state, STATE_TRACK_POSITION_SIZE_SELECTED, 10.0);
		trackPositionThreshold = Util.getStateDouble(state, STATE_TRACK_POSITION_THRESHOLD, 10);

		// verticals
		isDrawVerticals = Util.getStateBoolean(state, STATE_IS_DRAW_VERTICALS, false);

		// path + segments
		numSubsegments = Util.getStateInt(state, STATE_NUMBER_OF_SUB_SEGMENTS, 0);
	}

	void saveState(final IDialogSettings state) {

		state.put(STATE_DIRECTION_ARROW_VERTICAL_DISTANCE, directionArrowDistance);
		state.put(STATE_DIRECTION_ARROW_SIZE, directionArrowSize);

		state.put(STATE_PATH_RESOLUTION, pathResolution);

		state.put(STATE_OUTLINE_WIDTH, outlineWidth);
		state.put(STATE_OUTLINE_COLOR_MODE, outlineColorMode);
		state.put(STATE_OUTLINE_COLOR_MODE_HOVERED, outlineColorMode_Hovered);
		state.put(STATE_OUTLINE_COLOR_MODE_HOVSEL, outlineColorMode_HovSel);
		state.put(STATE_OUTLINE_COLOR_MODE_SELECTED, outlineColorMode_Selected);
		Util.setState(state, STATE_OUTLINE_COLOR, outlineColor);
		Util.setState(state, STATE_OUTLINE_COLOR_HOVERED, outlineColor_Hovered);
		Util.setState(state, STATE_OUTLINE_COLOR_HOV_SEL, outlineColor_HovSel);
		Util.setState(state, STATE_OUTLINE_COLOR_SELECTED, outlineColor_Selected);
		state.put(STATE_OUTLINE_OPACITY, outlineOpacity);
		state.put(STATE_OUTLINE_OPACITY_HOVERED, outlineOpacity_Hovered);
		state.put(STATE_OUTLINE_OPACITY_HOV_SEL, outlineOpacity_HovSel);
		state.put(STATE_OUTLINE_OPACITY_SELECTED, outlineOpacity_Selected);

		state.put(STATE_ALTITUDE_MODE, altitudeMode);
		state.put(STATE_ALTITUDE_OFFSET, altitudeVerticalOffset);

		state.put(STATE_IS_ABSOLUTE_OFFSET, isAbsoluteOffset);
		state.put(STATE_IS_DRAW_VERTICALS, isDrawVerticals);
		state.put(STATE_IS_EXTRUDE_PATH, isExtrudePath);
		state.put(STATE_IS_FOLLOW_TERRAIN, isFollowTerrain);

		state.put(STATE_OUTLINE_COLOR_MODE, interiorColorMode);
		Util.setState(state, STATE_INTERIOR_COLOR, interiorColor);
		Util.setState(state, STATE_INTERIOR_COLOR_HOVERED, interiorColor_Hovered);
		Util.setState(state, STATE_INTERIOR_COLOR_HOV_SEL, interiorColor_HovSel);
		Util.setState(state, STATE_INTERIOR_COLOR_SELECTED, interiorColor_Selected);
		state.put(STATE_INTERIOR_OPACITY, interiorOpacity);
		state.put(STATE_INTERIOR_OPACITY_HOVERED, interiorOpacity_Hovered);
		state.put(STATE_INTERIOR_OPACITY_HOV_SEL, interiorOpacity_HovSel);
		state.put(STATE_INTERIOR_OPACITY_SELECTED, interiorOpacity_Selected);

		state.put(STATE_IS_SHOW_TRACK_POSITION, isShowTrackPosition);
		state.put(STATE_TRACK_POSITION_SIZE, trackPositionSize);
		state.put(STATE_TRACK_POSITION_SIZE_HOVERED, trackPositionSize_Hovered);
		state.put(STATE_TRACK_POSITION_SIZE_SELECTED, trackPositionSize_Selected);
		state.put(STATE_TRACK_POSITION_THRESHOLD, trackPositionThreshold);

		state.put(STATE_NUMBER_OF_SUB_SEGMENTS, numSubsegments);
	}

}
