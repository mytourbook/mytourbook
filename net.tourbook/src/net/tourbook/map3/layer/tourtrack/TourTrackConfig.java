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
import net.tourbook.map3.Messages;

import org.eclipse.swt.graphics.RGB;

public class TourTrackConfig {

	/*
	 * Altitude mode
	 */
	public static final ComboEntry[]	ALTITUDE_MODE					= {
			new ComboEntry(Messages.Track_Config_Altitude_Mode_Absolute, WorldWind.ABSOLUTE),
			new ComboEntry(Messages.Track_Config_Altitude_Mode_ClampToGround, WorldWind.CLAMP_TO_GROUND),
			new ComboEntry(Messages.Track_Config_Altitude_Mode_RelativeToGround, WorldWind.RELATIVE_TO_GROUND) //
																		};

	/*
	 * Path resolution
	 */
	public static final int				PATH_RESOLUTION_OPTIMIZED		= 0;

	public static final int				PATH_RESOLUTION_ALL_POSITIONS	= 1;
	public static final int				PATH_RESOLUTION_VIEWPORT		= 2;
	public static final ComboEntry[]	PATH_RESOLUTION					= {
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
	public static final int				COLOR_MODE_TRACK_VALUE			= 0;

	/**
	 * Track is painted with a solid color.
	 */
	public static final int				COLOR_MODE_SOLID_COLOR			= 1;

	public static final ComboEntry[]	TRACK_COLOR_MODE				= {
			new ComboEntry(Messages.Track_Config_TrackColorMode_Value, COLOR_MODE_TRACK_VALUE),
			new ComboEntry(Messages.Track_Config_TrackColorMode_Solid, COLOR_MODE_SOLID_COLOR),//

																		};

	/**
	 * Recreate tracks when <code>true</code>.
	 */
	public boolean						isRecreateTracks;

	public String						id;
	public String						defaultId;
	public String						name;

	public double						outlineWidth;
	public int							outlineColorMode;
	public int							outlineColorMode_Hovered;
	public int							outlineColorMode_Selected;
	public int							outlineColorMode_HovSel;
	public RGB							outlineColor;
	public RGB							outlineColor_Hovered;
	public RGB							outlineColor_Selected;
	public RGB							outlineColor_HovSel;
	public double						outlineOpacity;
	public double						outlineOpacity_Hovered;
	public double						outlineOpacity_Selected;
	public double						outlineOpacity_HovSel;

	// interior
	public boolean						isShowInterior;
	public boolean						isDrawVerticals;
	public int							interiorColorMode;
	public int							interiorColorMode_Hovered;
	public int							interiorColorMode_Selected;
	public int							interiorColorMode_HovSel;
	public RGB							interiorColor;
	public RGB							interiorColor_Hovered;
	public RGB							interiorColor_Selected;
	public RGB							interiorColor_HovSel;
	public double						interiorOpacity;
	public double						interiorOpacity_Hovered;
	public double						interiorOpacity_Selected;
	public double						interiorOpacity_HovSel;

	// direction arrows
	public boolean						isShowDirectionArrows;
	public double						directionArrowDistance;
	public double						directionArrowSize;

	// track positions
	public boolean						isShowTrackPosition;
	public double						trackPositionSize;
	public double						trackPositionSize_Hovered;
	public double						trackPositionSize_Selected;
	public double						trackPositionSize_HovSel;
	public int							trackPositionThreshold;

	// other properties
	public int							altitudeMode;

	/**
	 * Vertical distance in m (meter).
	 */
	public int							altitudeVerticalOffset;

	/**
	 * Is <code>true</code> when altitude offset is enabled.
	 */
	public boolean						isAbsoluteOffset;

	public boolean						isFollowTerrain;
	public int							pathResolution;

	TourTrackConfig() {}

	public static int getValidAltitudeModeValue(final int stateAltitudeMode) {

		for (final ComboEntry altiMode : ALTITUDE_MODE) {
			if (altiMode.value == stateAltitudeMode) {
				return altiMode.value;
			}
		}

		// return default value
		return WorldWind.CLAMP_TO_GROUND;
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

}
