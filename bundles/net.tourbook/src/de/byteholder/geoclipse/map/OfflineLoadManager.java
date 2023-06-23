/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.preferences.IMappingPreferences;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This is managing the loading for offline images
 */
class OfflineLoadManager {

   private static OfflineLoadManager                _instance;

   private static final ConcurrentLinkedQueue<Tile> _offlineTiles            = new ConcurrentLinkedQueue<>();

   private static boolean                           _isLoading               = false;

   private static final IPreferenceStore            _prefStore               = TourbookPlugin.getPrefStore();

   private MP                                       _mp;

   private String                                   _osTileCachePath;

   private final TileImageLoaderCallback            _tileImageLoaderCallback = new TileImageLoaderCallback_ForOfflineImages();

   /**
    * This callback is called when a tile image was loaded and is set into the tile
    */
   private final class TileImageLoaderCallback_ForOfflineImages implements TileImageLoaderCallback {

      @Override
      public void update(final Tile tile) {

         // update loading state
         final LinkedBlockingDeque<Tile> waitingQueue = MP.getTileWaitingQueue();

         if (waitingQueue.isEmpty()) {
            _isLoading = false;
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

      offlineTile.setImageLoaderCallback(_tileImageLoaderCallback);

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

   boolean deleteOfflineImage(final MP offlineMp, final Tile offlineTile) {

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
         StatusUtil.showStatus("error occurred when deleting offline image: " + tilePath.toOSString(), e); //$NON-NLS-1$
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
         StatusUtil.showStatus("error occurred when checking offline image", e); //$NON-NLS-1$
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
