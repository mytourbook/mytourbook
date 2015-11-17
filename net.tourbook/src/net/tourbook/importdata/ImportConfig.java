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
import net.tourbook.ui.views.rawData.RawDataView;

public class ImportConfig {

	public boolean							isLiveUpdate				= true;

	public String							backupFolder				= UI.EMPTY_STRING;
	public String							deviceFolder				= UI.EMPTY_STRING;

	/** When <code>true</code> then a backup of the tour file is done. */
	public boolean							isCreateBackup				= true;

	public int								numHorizontalTiles			= RawDataView.NUM_HORIZONTAL_TILES_DEFAULT;
	public int								tileSize					= RawDataView.TILE_SIZE_DEFAULT;

	/** Background opacity in %. */
	public int								backgroundOpacity			= 5;

	/** Duration in seconds/10 */
	public int								animationDuration			= 40;
	public int								animationCrazinessFactor	= 3;

	public ArrayList<DeviceImportLauncher>	deviceImportLaunchers		= new ArrayList<>();

	/** Files which are not yet backed up. */
	public ArrayList<String>				notBackedUpFiles			= new ArrayList<>();

	/** Number of files in the device folder. */
	public int								numDeviceFiles;

	/**
	 * Contains files which are available in the device folder but they are not available in the
	 * tour database.
	 */
	public ArrayList<OSFile>				notImportedFiles			= new ArrayList<>();

	public String getBackupOSFolder() {

		if (NIO.isDeviceNameFolder(backupFolder)) {

			return NIO.convertToOSPath(backupFolder);

		} else {

			return backupFolder;
		}
	}

	/**
	 * @return Returns the device OS folder or <code>null</code> when not available.
	 */
	public String getDeviceOSFolder() {

		if (NIO.isDeviceNameFolder(deviceFolder)) {

			return NIO.convertToOSPath(deviceFolder);

		} else {

			return deviceFolder;
		}
	}
}
