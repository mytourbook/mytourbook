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
package net.tourbook.importdata;

import java.util.ArrayList;

import net.tourbook.common.NIO;
import net.tourbook.common.UI;

public class ImportConfig {

	static final int						ANIMATION_DURATION_DEFAULT			= 20;									// seconds/10
	static final int						ANIMATION_DURATION_MIN				= 0;
	static final int						ANIMATION_DURATION_MAX				= 100;									// ->10 seconds

	static final int						ANIMATION_CRAZINESS_FACTOR_DEFAULT	= 1;
	static final int						ANIMATION_CRAZINESS_FACTOR_MIN		= -100;
	static final int						ANIMATION_CRAZINESS_FACTOR_MAX		= 100;

	static final int						BACKGROUND_OPACITY_DEFAULT			= 5;
	static final int						BACKGROUND_OPACITY_MAX				= 100;
	static final int						BACKGROUND_OPACITY_MIN				= 0;

	static final int						HORIZONTAL_TILES_DEFAULT			= 5;
	static final int						HORIZONTAL_TILES_MIN				= 1;
	static final int						HORIZONTAL_TILES_MAX				= 50;

	static final int						LAST_MARKER_DISTANCE_MIN			= 0;
	static final int						LAST_MARKER_DISTANCE_MAX			= 100;									// km/10

	static final int						TILE_SIZE_DEFAULT					= 80;
	static final int						TILE_SIZE_MIN						= 20;
	static final int						TILE_SIZE_MAX						= 300;

	public boolean							isLiveUpdate						= true;

	private String							_backupFolder						= UI.EMPTY_STRING;
	private String							_deviceFolder						= UI.EMPTY_STRING;

	/** When <code>true</code> then a backup of the tour file is done. */
	public boolean							isCreateBackup						= true;

	public int								numHorizontalTiles					= HORIZONTAL_TILES_DEFAULT;
	public int								tileSize							= TILE_SIZE_DEFAULT;

	/** Background opacity in %. */
	public int								backgroundOpacity					= BACKGROUND_OPACITY_DEFAULT;

	/** Duration in seconds/10 */
	public int								animationDuration					= ANIMATION_DURATION_DEFAULT;
	public int								animationCrazinessFactor			= ANIMATION_CRAZINESS_FACTOR_DEFAULT;

	public ArrayList<DeviceImportLauncher>	deviceImportLaunchers				= new ArrayList<>();

	/** Files which are not yet backed up. */
	public ArrayList<String>				notBackedUpFiles					= new ArrayList<>();

	/** Number of files in the device folder. */
	public int								numDeviceFiles;

	/**
	 * Contains files which are available in the device folder but they are not available in the
	 * tour database.
	 */
	public ArrayList<OSFile>				notImportedFiles					= new ArrayList<>();

	public boolean							isUpdateDeviceState					= true;

	/**
	 * When <code>true</code> prevent that a default launcher is created.
	 */
	public boolean							isLastLauncherRemoved;

	public String getBackupFolder() {
		return _backupFolder;
	}

	/**
	 * @return Returns the backup OS folder or <code>null</code> when not available.
	 */
	public String getBackupOSFolder() {

		if (isCreateBackup) {
			return getOSFolder(_backupFolder);
		}

		return null;
	}

	public String getDeviceFolder() {
		return _deviceFolder;
	}

	/**
	 * @return Returns the device OS folder or <code>null</code> when not available.
	 */
	public String getDeviceOSFolder() {
		return getOSFolder(_deviceFolder);
	}

	private String getOSFolder(final String folder) {

		if (folder == null || folder.trim().length() == 0) {
			return null;
		}

		if (NIO.isDeviceNameFolder(folder)) {

			return NIO.convertToOSPath(folder);

		} else {

			return folder;
		}
	}

	/**
	 * @return Returns <code>true</code> when the device or backup folder should be watched.
	 *         <p>
	 *         The folders are not checked in the filesystem only the definition. This prevents
	 *         delays when accessing the fs.
	 */
	public boolean isWatchAnything() {

		final boolean isWatch_DeviceFolder = _deviceFolder != null && _deviceFolder.trim().length() > 0;
		final boolean isWatch_BackupFolder = _backupFolder != null && _backupFolder.trim().length() > 0;

		return isWatch_DeviceFolder || (isCreateBackup && isWatch_BackupFolder);
	}

	public void setBackupFolder(final String backupFolder) {

		_backupFolder = backupFolder;
	}

	public void setDeviceFolder(final String deviceFolder) {

		_deviceFolder = deviceFolder;
	}
}
