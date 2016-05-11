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
package net.tourbook.importdata;

import java.util.ArrayList;

public class EasyConfig {

	static final int					ANIMATION_DURATION_DEFAULT				= 20;									// seconds/10
	static final int					ANIMATION_DURATION_MIN					= 0;
	static final int					ANIMATION_DURATION_MAX					= 100;									// ->10 seconds

	static final int					ANIMATION_CRAZINESS_FACTOR_DEFAULT		= 1;
	static final int					ANIMATION_CRAZINESS_FACTOR_MIN			= -100;
	static final int					ANIMATION_CRAZINESS_FACTOR_MAX			= 100;

	static final int					BACKGROUND_OPACITY_DEFAULT				= 5;
	static final int					BACKGROUND_OPACITY_MIN					= 0;
	static final int					BACKGROUND_OPACITY_MAX					= 100;

	static final int					HORIZONTAL_TILES_DEFAULT				= 5;
	static final int					HORIZONTAL_TILES_MIN					= 1;
	static final int					HORIZONTAL_TILES_MAX					= 50;

	static final boolean				LIVE_UPDATE_DEFAULT						= true;

	static final int					STATE_TOOLTIP_WIDTH_DEFAULT				= 500;
	static final int					STATE_TOOLTIP_WIDTH_MIN					= 300;
	static final int					STATE_TOOLTIP_WIDTH_MAX					= 2000;

	static final int					TILE_SIZE_DEFAULT						= 100;
	static final int					TILE_SIZE_MIN							= 20;
	static final int					TILE_SIZE_MAX							= 300;

	/*
	 * Launcher config
	 */
	static final int					LAST_MARKER_DISTANCE_DEFAULT			= 2000;								// 2 km
	static final int					LAST_MARKER_DISTANCE_MIN				= 0;
	static final int					LAST_MARKER_DISTANCE_MAX				= 10000;								// 10 km

	static final int					TOUR_TYPE_AVG_SPEED_DEFAULT				= 0;
	static final int					TOUR_TYPE_AVG_SPEED_MIN					= 0;
	static final int					TOUR_TYPE_AVG_SPEED_MAX					= 3000;

	public static final int				TEMPERATURE_ADJUSTMENT_DURATION_DEFAULT	= 12 * 60;								// 12 minutes

	public static final int				TEMPERATURE_AVG_TEMPERATURE_DEFAULT		= 15;									// 15 °C
	public static final int				TEMPERATURE_AVG_TEMPERATURE_MIN			= 0;									// 0 °C
	public static final int				TEMPERATURE_AVG_TEMPERATURE_MAX			= 100;									// 30 °C / 100 °F

	/*
	 * Dash fields
	 */
	public boolean						isLiveUpdate							= LIVE_UPDATE_DEFAULT;

	public int							numHorizontalTiles						= HORIZONTAL_TILES_DEFAULT;
	public int							tileSize								= TILE_SIZE_DEFAULT;

	/** Background opacity in %. */
	public int							backgroundOpacity						= BACKGROUND_OPACITY_DEFAULT;

	/** Duration in seconds/10 */
	public int							animationDuration						= ANIMATION_DURATION_DEFAULT;
	public int							animationCrazinessFactor				= ANIMATION_CRAZINESS_FACTOR_DEFAULT;

	public int							stateToolTipWidth						= STATE_TOOLTIP_WIDTH_DEFAULT;

	/*
	 * Launcher fields
	 */
	public ArrayList<ImportLauncher>	importLaunchers							= new ArrayList<>();
	public ArrayList<ImportConfig>		importConfigs							= new ArrayList<>();

	private ImportConfig				_activeImportConfig;

	/** Files which are not yet backed up. */
	public ArrayList<String>			notBackedUpFiles						= new ArrayList<>();

	/** Number of files in the device folder. */
	public int							numDeviceFiles;

	/**
	 * Contains files which are available in the device AND backup folder but they are not yet
	 * available in the tour database.
	 */
	public ArrayList<OSFile>			notImportedFiles						= new ArrayList<>();

	/** Contains files which are available in the backup folder but not in the device folder. */
	public ArrayList<OSFile>			movedFiles;

	/**
	 * @return Returns the active import config which is used when importing tours.
	 */
	public ImportConfig getActiveImportConfig() {
		return _activeImportConfig;
	}

	public void setActiveImportConfig(final ImportConfig activeImportConfig) {
		_activeImportConfig = activeImportConfig;
	}
}
