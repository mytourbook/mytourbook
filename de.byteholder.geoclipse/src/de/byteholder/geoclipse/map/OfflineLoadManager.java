/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.map;

import java.io.File;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

import net.tourbook.util.StatusUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.preferences.IMappingPreferences;

/**
 * this will manage the loading for offline images
 */
public class OfflineLoadManager {

	private static OfflineLoadManager					_instance;

	private MP											_mp;

	private static final ConcurrentLinkedQueue<Tile>	_offlineTiles		= new ConcurrentLinkedQueue<Tile>();

	private final IPreferenceStore						_prefStore			= Activator
																					.getDefault()
																					.getPreferenceStore();

	private String										_osTileCachePath;

	private static boolean								_isLoading			= false;

	private final TileLoadObserver						_tileLoadObserver	= new TileLoadObserver();

	/**
	 * This observer is called in the {@link Tile} when a tile image is set into the tile
	 */
	private final class TileLoadObserver implements Observer {

		@Override
		public void update(final Observable observable, final Object arg) {

			if (observable instanceof Tile) {

				final Tile tile = (Tile) observable;

				tile.deleteObserver(this);

				// update loading state
				final LinkedBlockingDeque<Tile> waitingQueue = MP.getTileWaitingQueue();
				if (waitingQueue.size() == 0) {
					_isLoading = false;
				}
			}
		}
	}

	static OfflineLoadManager getInstance() {

		if (_instance == null) {
			_instance = new OfflineLoadManager();
		}

		return _instance;
	}

	/**
	 * @return Returns true when loading is in progress
	 */
	static boolean isLoading() {
		return _isLoading;
	}

	/**
	 * @param offlineMp
	 * @param offlineTile
	 * @return Return <code>true</code> when the offline image needs to be loaded,
	 *         <code>false</code> when the image is already available
	 */
	boolean addOfflineTile(final MP offlineMp, final Tile offlineTile) {

		if (isOfflineImageAvailable(offlineMp, offlineTile)) {
			return false;
		}

		_isLoading = true;

		_offlineTiles.add(offlineTile);

		_mp.putTileInWaitingQueue(offlineTile, false);

		offlineTile.addObserver(_tileLoadObserver);
 
		return true;
	}

	/**
	 * check and create tile cache path
	 */
	private boolean checkOfflinePath() {

		String workingDirectory;

		final boolean useDefaultLocation = _prefStore
				.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_DEFAULT_LOCATION);

		if (useDefaultLocation) {
			workingDirectory = Platform.getInstanceLocation().getURL().getPath();
		} else {
			workingDirectory = _prefStore.getString(IMappingPreferences.OFFLINE_CACHE_PATH);
		}

		if (new File(workingDirectory).exists() == false) {

			StatusUtil.showStatus("working directory is not available: " + workingDirectory); //$NON-NLS-1$
			return false;
		}

		final IPath tileCachePath = new Path(workingDirectory).append(TileImageCache.TILE_OFFLINE_CACHE_OS_PATH);

		if (tileCachePath.toFile().exists() == false) {
			if (tileCachePath.toFile().mkdirs() == false) {
				return false;
			}
		}

		_osTileCachePath = tileCachePath.toOSString();

		return true;
	}

	public boolean deleteOfflineImage(final MP offlineMp, final Tile offlineTile) {

		final IPath tilePath = offlineMp.getTileOSPath(_osTileCachePath, offlineTile);

		try {

			if (tilePath == null) {
				return false;
			}

			final File tileFile = tilePath.toFile();
			if (tileFile.exists()) {

				// offline image is available

				return tileFile.delete();
			}

		} catch (final Exception e) {
			StatusUtil.showStatus("error occured when deleding offline image: " + tilePath.toOSString(), e); //$NON-NLS-1$
		}

		return false;
	}

	boolean initialize(final MP mp) {

		if (_isLoading) {
			return false;
		}

		_mp = mp;

		return checkOfflinePath();
	}

	/**
	 * check if the image is available as offline image
	 * 
	 * @param offlineMp
	 */
	boolean isOfflineImageAvailable(final MP offlineMp, final Tile offlineTile) {

		try {

			final IPath tilePath = offlineMp.getTileOSPath(_osTileCachePath, offlineTile);
			if (tilePath == null) {
				return false;
			}

			final File tileFile = tilePath.toFile();
			if (tileFile.exists()) {

				// offline image is available

				return true;
			}

		} catch (final Exception e) {
			StatusUtil.showStatus("error occured when checking offline image", e); //$NON-NLS-1$
			return false;
		}

		return false;
	}

	void stopLoading() {

		_offlineTiles.clear();

		// stop loading images 
		_mp.resetAll(false);

		_isLoading = false;
	}

}
