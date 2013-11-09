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

public class TourTrackConfig implements Cloneable {

	/*
	 * Altitude mode
	 */
	public static final ComboEntry[]	ALTITUDE_MODE					= {
			new ComboEntry(Messages.Track_Config_Altitude_Mode_Absolute, WorldWind.ABSOLUTE),
			new ComboEntry(Messages.Track_Config_Altitude_Mode_ClampToGround, WorldWind.CLAMP_TO_GROUND),
			new ComboEntry(Messages.Track_Config_Altitude_Mode_RelativeToGround, WorldWind.RELATIVE_TO_GROUND) //
																		};

//	/*
//	 * Path resolution
//	 */
//	public static final int				PATH_RESOLUTION_OPTIMIZED		= 0;
//	public static final int				PATH_RESOLUTION_ALL_POSITIONS	= 1;
//	public static final int				PATH_RESOLUTION_VIEWPORT		= 2;
//	public static final ComboEntry[]	PATH_RESOLUTION					= {
//			new ComboEntry(Messages.Track_Config_Path_Resolution_Optimized, PATH_RESOLUTION_OPTIMIZED),//
//			new ComboEntry(Messages.Track_Config_Path_Resolution_High, PATH_RESOLUTION_ALL_POSITIONS),
//			new ComboEntry(Messages.Track_Config_Path_Resolution_Viewport, PATH_RESOLUTION_VIEWPORT),//
//																		//
//																		};

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
			new ComboEntry(Messages.Track_Config_TrackColorMode_Solid, COLOR_MODE_SOLID_COLOR),
			new ComboEntry(Messages.Track_Config_TrackColorMode_Value, COLOR_MODE_TRACK_VALUE),
																		//
																		};

	/**
	 * Recreate tracks when <code>true</code>.
	 */
	private boolean						_isRecreateTracks;

	/*
	 * Set default values also here to ensure that a valid value is set. A default value would not
	 * be set when an xml tag is not available.
	 */

	public String						id								= Long.toString(System.nanoTime());
	public String						defaultId						= TourTrackConfigManager.DEFAULT_ID_DEFAULT;
	public String						name							= TourTrackConfigManager.CONFIG_NAME_UNKNOWN;

	// outline
	public double						outlineWidth					= TourTrackConfigManager.OUTLINE_WIDTH_DEFAULT;
	public int							outlineColorMode				= TourTrackConfigManager.OUTLINE_COLOR_MODE_NORMAL_DEFAULT;
	public int							outlineColorMode_Hovered		= TourTrackConfigManager.OUTLINE_COLOR_MODE_HOVERED_DEFAULT;
	public int							outlineColorMode_Selected		= TourTrackConfigManager.OUTLINE_COLOR_MODE_SELECTED_DEFAULT;
	public int							outlineColorMode_HovSel			= TourTrackConfigManager.OUTLINE_COLOR_MODE_HOV_SEL_DEFAULT;

	public RGB							outlineColor					= TourTrackConfigManager.RGB_NORMAL_DEFAULT;
	public RGB							outlineColor_Hovered			= TourTrackConfigManager.RGB_HOVERED_DEFAULT;
	public RGB							outlineColor_Selected			= TourTrackConfigManager.RGB_SELECTED_DEFAULT;
	public RGB							outlineColor_HovSel				= TourTrackConfigManager.RGB_HOV_SEL_DEFAULT;

	public double						outlineOpacity					= TourTrackConfigManager.OUTLINE_OPACITY_NORMAL_DEFAULT;
	public double						outlineOpacity_Hovered			= TourTrackConfigManager.OUTLINE_OPACITY_HOVERED_DEFAULT;
	public double						outlineOpacity_Selected			= TourTrackConfigManager.OUTLINE_OPACITY_SELECTED_DEFAULT;
	public double						outlineOpacity_HovSel			= TourTrackConfigManager.OUTLINE_OPACITY_HOV_SEL_DEFAULT;

	// interior
	public boolean						isShowInterior					= TourTrackConfigManager.IS_INTERIOR_VISIBLE_DEFAULT;
	public boolean						isDrawVerticals					= TourTrackConfigManager.IS_DRAW_VERTICALS_DEFAULT;

	public int							interiorColorMode				= TourTrackConfigManager.INTERIOR_COLOR_MODE_NORMAL_DEFAULT;
	public int							interiorColorMode_Hovered		= TourTrackConfigManager.INTERIOR_COLOR_MODE_HOVERED_DEFAULT;
	public int							interiorColorMode_Selected		= TourTrackConfigManager.INTERIOR_COLOR_MODE_SELECTED_DEFAULT;
	public int							interiorColorMode_HovSel		= TourTrackConfigManager.INTERIOR_COLOR_MODE_HOV_SEL_DEFAULT;

	public RGB							interiorColor					= TourTrackConfigManager.RGB_NORMAL_DEFAULT;
	public RGB							interiorColor_Hovered			= TourTrackConfigManager.RGB_HOVERED_DEFAULT;
	public RGB							interiorColor_Selected			= TourTrackConfigManager.RGB_SELECTED_DEFAULT;
	public RGB							interiorColor_HovSel			= TourTrackConfigManager.RGB_HOV_SEL_DEFAULT;

	public double						interiorOpacity					= TourTrackConfigManager.INTERIOR_OPACITY_NORMAL_DEFAULT;
	public double						interiorOpacity_Hovered			= TourTrackConfigManager.INTERIOR_OPACITY_HOVERED_DEFAULT;
	public double						interiorOpacity_Selected		= TourTrackConfigManager.INTERIOR_OPACITY_SELECTED_DEFAULT;
	public double						interiorOpacity_HovSel			= TourTrackConfigManager.INTERIOR_OPACITY_HOV_SEL_DEFAULT;

	// direction arrows
	public boolean						isShowDirectionArrows			= TourTrackConfigManager.IS_DIRECTION_ARROWS_VISIBLE_DEFAULT;
	public double						directionArrowDistance			= TourTrackConfigManager.DIRECTION_ARROW_VERTICAL_DISTANCE_DEFAULT;
	public double						directionArrowSize				= TourTrackConfigManager.DIRECTION_ARROW_SIZE_DEFAULT;

	// track positions
	public boolean						isShowTrackPosition				= TourTrackConfigManager.IS_SHOW_TRACK_POSITION_DEFAULT;
	public int							trackPositionThreshold			= TourTrackConfigManager.TRACK_POSITION_THRESHOLD_DEFAULT;
	public double						trackPositionSize				= TourTrackConfigManager.TRACK_POSITION_SIZE_NORMAL_DEFAULT;
	public double						trackPositionSize_Hovered		= TourTrackConfigManager.TRACK_POSITION_SIZE_HOVERED_DEFAULT;
	public double						trackPositionSize_Selected		= TourTrackConfigManager.TRACK_POSITION_SIZE_SELECTED_DEFAULT;
	public double						trackPositionSize_HovSel		= TourTrackConfigManager.TRACK_POSITION_SIZE_HOV_SEL_DEFAULT;

	/*
	 * Other properties
	 */
	public int							altitudeMode					= TourTrackConfigManager.ALTITUDE_MODE_DEFAULT;

	/**
	 * Is <code>true</code> when altitude offset is enabled.
	 */
	public boolean						isAltitudeOffset				= TourTrackConfigManager.IS_ALTITUDE_OFFSET_DEFAULT;

	public int							altitudeOffsetMode				= TourTrackConfigManager.ALTITUDE_OFFSET_MODE_DEFAULT;

	/**
	 * Vertical distance in m (meter).
	 */
	public int							altitudeOffsetDistanceAbsolute	= TourTrackConfigManager.ALTITUDE_OFFSET_ABSOLUTE_DEFAULT;
	public int							altitudeOffsetDistanceRelative	= TourTrackConfigManager.ALTITUDE_OFFSET_RELATIVE_DEFAULT;

	public boolean						isFollowTerrain					= TourTrackConfigManager.CONFIG_IS_FOLLOW_TERRAIN_DEFAULT;

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
	 * Compares this config with a previous config and sets a flag when tour tracks needs to be
	 * recreated.
	 * 
	 * @param previousConfig
	 *            Previous config, can be <code>null</code> which forces to recreate tour tracks.
	 */
	public void checkTrackRecreation(final TourTrackConfig previousConfig) {

		if (previousConfig == null) {

			_isRecreateTracks = true;

		} else {

			final boolean backupIsFollowTerrain = previousConfig.isFollowTerrain;

			/*
			 * check if tracks must be recreated
			 */
			if (//
			// follow terrain is  modified
			backupIsFollowTerrain != isFollowTerrain //
			) {

				_isRecreateTracks = true;

			} else {

				_isRecreateTracks = false;
			}
		}

//		System.out.println(UI.timeStampNano()
//				+ " ["
//				+ getClass().getSimpleName()
//				+ "] \t_isRecreateTracks: "
//				+ _isRecreateTracks);
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	/**
	 * Create a copy of this object.
	 * 
	 * @return a copy of this <code>Insets</code> object.
	 */
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (final CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError();
		}
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

	public boolean isRecreateTracks() {
		return _isRecreateTracks;
	}

}
