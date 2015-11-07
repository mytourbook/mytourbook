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

import net.tourbook.common.UI;
import net.tourbook.ui.views.rawData.RawDataView;

public class ImportConfig {

	public boolean					isLiveUpdate		= true;

	public String					backupFolder		= UI.EMPTY_STRING;
	public String					deviceFolder		= UI.EMPTY_STRING;

	public int						numHorizontalTiles	= RawDataView.NUM_HORIZONTAL_TILES_DEFAULT;
	public int						tileSize			= RawDataView.TILE_SIZE_DEFAULT;

	/** Background opacity in %. */
	public int						backgroundOpacity	= 5;

	public ArrayList<TourTypeItem>	tourTypeItems		= new ArrayList<>();

	/**
	 * Contains files which are available in the device folder but they are not available in the
	 * tour database.
	 */
	public ArrayList<String>		notImportedFiles;
}
