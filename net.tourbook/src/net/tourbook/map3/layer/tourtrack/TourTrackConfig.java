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

	private static final String				STATE_ALTITUDE_MODE					= "STATE_ALTITUDE_MODE";				//$NON-NLS-1$
	private static final String				STATE_ALTITUDE_OFFSET				= "STATE_ALTITUDE_OFFSET";				//$NON-NLS-1$
	private static final String				STATE_IS_ABSOLUTE_OFFSET			= "STATE_IS_ABSOLUTE_OFFSET";			//$NON-NLS-1$
	private static final String				STATE_IS_DRAW_VERTICALS				= "STATE_IS_DRAW_VERTICALS";			//$NON-NLS-1$
	private static final String				STATE_IS_EXTRUDE_PATH				= "STATE_IS_EXTRUDE_PATH";				//$NON-NLS-1$
	private static final String				STATE_IS_FOLLOW_TERRAIN				= "STATE_IS_FOLLOW_TERRAIN";			//$NON-NLS-1$
	private static final String				STATE_IS_SHOW_TRACK_POSITION		= "STATE_IS_SHOW_TRACK_POSITION";		//$NON-NLS-1$
	private static final String				STATE_INTERIOR_COLOR				= "STATE_INTERIOR_COLOR";				//$NON-NLS-1$
	private static final String				STATE_INTERIOR_COLOR_HOVERED		= "STATE_INTERIOR_COLOR_HOVERED";		//$NON-NLS-1$
	private static final String				STATE_INTERIOR_COLOR_HOV_SEL		= "STATE_INTERIOR_COLOR_HOV_SEL";		//$NON-NLS-1$
	private static final String				STATE_INTERIOR_COLOR_SELECTED		= "STATE_INTERIOR_COLOR_SELECTED";		//$NON-NLS-1$
	private static final String				STATE_INTERIOR_OPACITY				= "STATE_INTERIOR_OPACITY";			//$NON-NLS-1$
	private static final String				STATE_INTERIOR_OPACITY_HOVERED		= "STATE_INTERIOR_OPACITY_HOVERED";	//$NON-NLS-1$
	private static final String				STATE_INTERIOR_OPACITY_HOV_SEL		= "STATE_INTERIOR_OPACITY_HOV_SEL";	//$NON-NLS-1$
	private static final String				STATE_INTERIOR_OPACITY_SELECTED		= "STATE_INTERIOR_OPACITY_SELECTED";	//$NON-NLS-1$
	private static final String				STATE_NUMBER_OF_SUB_SEGMENTS		= "STATE_NUMBER_OF_SUB_SEGMENTS";		//$NON-NLS-1$
	private static final String				STATE_OUTLINE_COLOR_HOV_SEL			= "STATE_OUTLINE_COLOR_HOV_SEL";		//$NON-NLS-1$
	private static final String				STATE_OUTLINE_COLOR_HOVERED			= "STATE_OUTLINE_COLOR_HOVERED";		//$NON-NLS-1$
	private static final String				STATE_OUTLINE_COLOR_SELECTED		= "STATE_OUTLINE_COLOR_SELECTED";		//$NON-NLS-1$
	private static final String				STATE_OUTLINE_OPACITY_HOV_SEL		= "STATE_OUTLINE_OPACITY_HOV_SEL";		//$NON-NLS-1$
	private static final String				STATE_OUTLINE_OPACITY_HOVERED		= "STATE_OUTLINE_OPACITY_HOVERED";		//$NON-NLS-1$
	private static final String				STATE_OUTLINE_OPACITY_SELECTED		= "STATE_OUTLINE_OPACITY_SELECTED";	//$NON-NLS-1$
	private static final String				STATE_OUTLINE_WIDTH					= "STATE_OUTLINE_WIDTH";				//$NON-NLS-1$
	private static final String				STATE_PATH_RESOLUTION				= "STATE_PATH_RESOLUTION";				//$NON-NLS-1$
	private static final String				STATE_TRACK_POSITION_SIZE			= "STATE_TRACK_POSITION_SIZE";			//$NON-NLS-1$
	private static final String				STATE_TRACK_POSITION_SIZE_HOVERED	= "STATE_TRACK_POSITION_SIZE_HOVERED";	//$NON-NLS-1$
	private static final String				STATE_TRACK_POSITION_SIZE_SELECTED	= "STATE_TRACK_POSITION_SIZE_SELECTED"; //$NON-NLS-1$
	private static final String				STATE_TRACK_POSITION_THRESHOLD		= "STATE_TRACK_POSITION_THRESHOLD";	//$NON-NLS-1$

	public static final AltitudeMode[]		ALTITUDE_MODE						= {
			new AltitudeMode(Messages.Track_Config_Altitude_Mode_ClampToGround, WorldWind.CLAMP_TO_GROUND),
			new AltitudeMode(Messages.Track_Config_Altitude_Mode_Absolute, WorldWind.ABSOLUTE),
			new AltitudeMode(Messages.Track_Config_Altitude_Mode_RelativeToGround, WorldWind.RELATIVE_TO_GROUND) //
																				};

	public static final int					PATH_RESOLUTION_ALL_POSITIONS		= 0;
	public static final int					PATH_RESOLUTION_OPTIMIZED			= 1;
	public static final int					PATH_RESOLUTION_VIEWPORT			= 2;

	public static final PathResolution[]	PATH_RESOLUTION						= {
			new PathResolution(Messages.Track_Config_Path_Resolution_High, PATH_RESOLUTION_ALL_POSITIONS),
			new PathResolution(Messages.Track_Config_Path_Resolution_Optimized, PATH_RESOLUTION_OPTIMIZED),//
			new PathResolution(Messages.Track_Config_Path_Resolution_Viewport, PATH_RESOLUTION_VIEWPORT),//
																				//
																				};

	/**
	 * Recreate tracks when <code>true</code>.
	 */
	public boolean							isRecreateTracks;

	/*
	 * Configurations which are saved in the state.
	 */

	public int								pathResolution;

	public int								altitudeMode;

	/**
	 * Vertical distance in m (meter).
	 */
	public int								altitudeOffsetDistance;

	/**
	 * Is <code>true</code> when altitude offset is enabled.
	 */
	public boolean							isAbsoluteOffset;
	public boolean							isExtrudePath;
	public boolean							isDrawVerticals;
	public boolean							isFollowTerrain;
	public boolean							isShowTrackPosition;

	public RGB								interiorColor;
	public RGB								interiorColorHovered;
	public RGB								interiorColorHovSel;
	public RGB								interiorColorSelected;
	public double							interiorOpacity;
	public double							interiorOpacityHovered;
	public double							interiorOpacityHovSel;
	public double							interiorOpacitySelected;

	public RGB								outlineColorHovered;
	public RGB								outlineColorHovSel;
	public RGB								outlineColorSelected;
	public double							outlineOpacityHovered;
	public double							outlineOpacityHovSel;
	public double							outlineOpacitySelected;

	public double							outlineWidth;

	// UI is currently disabled, subsegments == 0
	public int								numSubsegments;

	public double							trackPositionSize;
	public double							trackPositionSizeHovered;
	public double							trackPositionSizeSelected;
	public double							trackPositionThreshold;

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

		pathResolution = Util.getStateInt(state, STATE_PATH_RESOLUTION, PATH_RESOLUTION_OPTIMIZED);

		outlineWidth = Util.getStateDouble(state, STATE_OUTLINE_WIDTH, 1.0);
		outlineColorHovSel = Util.getStateColor(state, STATE_OUTLINE_COLOR_HOV_SEL, hovSelRGB);
		outlineColorHovered = Util.getStateColor(state, STATE_OUTLINE_COLOR_HOVERED, hoveredRGB);
		outlineColorSelected = Util.getStateColor(state, STATE_OUTLINE_COLOR_SELECTED, selectedRGB);
		outlineOpacityHovSel = Util.getStateDouble(state, STATE_OUTLINE_OPACITY_HOV_SEL, 1.0);
		outlineOpacityHovered = Util.getStateDouble(state, STATE_OUTLINE_OPACITY_HOVERED, 1.0);
		outlineOpacitySelected = Util.getStateDouble(state, STATE_OUTLINE_OPACITY_SELECTED, 1.0);

		// altitude
		altitudeMode = getAltitudeModeValue(Util.getStateInt(state, STATE_ALTITUDE_MODE, WorldWind.ABSOLUTE));
		isAbsoluteOffset = Util.getStateBoolean(state, STATE_IS_ABSOLUTE_OFFSET, true);
		altitudeOffsetDistance = Util.getStateInt(state, STATE_ALTITUDE_OFFSET, 50);
		isFollowTerrain = Util.getStateBoolean(state, STATE_IS_FOLLOW_TERRAIN, true);

		// track position
		isShowTrackPosition = Util.getStateBoolean(state, STATE_IS_SHOW_TRACK_POSITION, true);
		trackPositionSize = Util.getStateDouble(state, STATE_TRACK_POSITION_SIZE, 8.0);
		trackPositionSizeHovered = Util.getStateDouble(state, STATE_TRACK_POSITION_SIZE_HOVERED, 12.0);
		trackPositionSizeSelected = Util.getStateDouble(state, STATE_TRACK_POSITION_SIZE_SELECTED, 10.0);
		trackPositionThreshold = Util.getStateDouble(state, STATE_TRACK_POSITION_THRESHOLD, 10);

		// curtain
		isExtrudePath = Util.getStateBoolean(state, STATE_IS_EXTRUDE_PATH, true);
		interiorColor = Util.getStateColor(state, STATE_INTERIOR_COLOR, defaultRGB);
		interiorColorHovered = Util.getStateColor(state, STATE_INTERIOR_COLOR_HOVERED, hoveredRGB);
		interiorColorHovSel = Util.getStateColor(state, STATE_INTERIOR_COLOR_HOV_SEL, hovSelRGB);
		interiorColorSelected = Util.getStateColor(state, STATE_INTERIOR_COLOR_SELECTED, selectedRGB);
		interiorOpacity = Util.getStateDouble(state, STATE_INTERIOR_OPACITY, 0.0);
		interiorOpacityHovered = Util.getStateDouble(state, STATE_INTERIOR_OPACITY_HOVERED, 0.2);
		interiorOpacityHovSel = Util.getStateDouble(state, STATE_INTERIOR_OPACITY_HOV_SEL, 0.2);
		interiorOpacitySelected = Util.getStateDouble(state, STATE_INTERIOR_OPACITY_SELECTED, 0.2);

		// verticals
		isDrawVerticals = Util.getStateBoolean(state, STATE_IS_DRAW_VERTICALS, false);

		// path + segments
		numSubsegments = Util.getStateInt(state, STATE_NUMBER_OF_SUB_SEGMENTS, 0);
	}

	void saveState(final IDialogSettings state) {

		state.put(STATE_PATH_RESOLUTION, pathResolution);

		state.put(STATE_OUTLINE_WIDTH, outlineWidth);
		state.put(STATE_OUTLINE_OPACITY_HOV_SEL, outlineOpacityHovSel);
		state.put(STATE_OUTLINE_OPACITY_HOVERED, outlineOpacityHovered);
		state.put(STATE_OUTLINE_OPACITY_SELECTED, outlineOpacitySelected);
		Util.setState(state, STATE_OUTLINE_COLOR_HOV_SEL, outlineColorHovSel);
		Util.setState(state, STATE_OUTLINE_COLOR_HOVERED, outlineColorHovered);
		Util.setState(state, STATE_OUTLINE_COLOR_SELECTED, outlineColorSelected);

		state.put(STATE_ALTITUDE_MODE, altitudeMode);
		state.put(STATE_ALTITUDE_OFFSET, altitudeOffsetDistance);

		state.put(STATE_IS_ABSOLUTE_OFFSET, isAbsoluteOffset);
		state.put(STATE_IS_DRAW_VERTICALS, isDrawVerticals);
		state.put(STATE_IS_EXTRUDE_PATH, isExtrudePath);
		state.put(STATE_IS_FOLLOW_TERRAIN, isFollowTerrain);

		state.put(STATE_INTERIOR_OPACITY, interiorOpacity);
		state.put(STATE_INTERIOR_OPACITY_HOVERED, interiorOpacityHovered);
		state.put(STATE_INTERIOR_OPACITY_HOV_SEL, interiorOpacityHovSel);
		state.put(STATE_INTERIOR_OPACITY_SELECTED, interiorOpacitySelected);
		Util.setState(state, STATE_INTERIOR_COLOR, interiorColor);
		Util.setState(state, STATE_INTERIOR_COLOR_HOVERED, interiorColorHovered);
		Util.setState(state, STATE_INTERIOR_COLOR_HOV_SEL, interiorColorHovSel);
		Util.setState(state, STATE_INTERIOR_COLOR_SELECTED, interiorColorSelected);

		state.put(STATE_IS_SHOW_TRACK_POSITION, isShowTrackPosition);
		state.put(STATE_TRACK_POSITION_SIZE, trackPositionSize);
		state.put(STATE_TRACK_POSITION_SIZE_HOVERED, trackPositionSizeHovered);
		state.put(STATE_TRACK_POSITION_SIZE_SELECTED, trackPositionSizeSelected);
		state.put(STATE_TRACK_POSITION_THRESHOLD, trackPositionThreshold);

		state.put(STATE_NUMBER_OF_SUB_SEGMENTS, numSubsegments);
	}

}
