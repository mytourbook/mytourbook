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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * cache for tiles
 */
public class TileCache {

	private int										fMaxTiles		= 10;

	private final ConcurrentHashMap<String, Tile>	tileCache		= new ConcurrentHashMap<String, Tile>();
	private final ConcurrentLinkedQueue<String>		tileCacheFifo	= new ConcurrentLinkedQueue<String>();

	public TileCache(final int maxTiles) {
		fMaxTiles = maxTiles;
	}

	public void add(final String tileKey, final Tile tile) {

		// check if space is available in the cache
		final int cacheSize = tileCacheFifo.size();
		if (cacheSize > fMaxTiles) {

			// remove cached tiles 
			for (int cacheIndex = fMaxTiles; cacheIndex < cacheSize; cacheIndex++) {
				removeTile(tileCacheFifo.poll());
			}
		}

		tileCache.put(tileKey, tile);
		tileCacheFifo.add(tileKey);
	}

	public Tile get(final String tileKey) {
		return tileCache.get(tileKey);
	}

	public void remove(final String tileKey) {

		tileCacheFifo.remove(tileKey);

		removeTile(tileKey);
	}

	/**
	 * Removes all tiles
	 */
	public synchronized void removeAll() {

		Tile checkedTile = null;

		final Collection<Tile> tiles = tileCache.values();
		for (final Tile tile : tiles) {

			if (checkedTile == null) {
				checkedTile = tile;
			}

			// remove children to prevent memory leaks
			final ArrayList<Tile> tileChildren = tile.getChildren();
			if (tileChildren != null) {

				for (final Tile tileChild : tileChildren) {
					tileChild.setParentTile(null);
				}

				tileChildren.clear();
			}
		}

		tileCache.clear();
		tileCacheFifo.clear();

		if (checkedTile != null) {
			checkedTile = null;
		}
	}

	/**
	 * Removes all tiles which are a parent of child tiles
	 */
	public void removeParentTiles() {

		for (final Tile tile : tileCache.values()) {

			/*
			 * check if this is a parent tile, child tiles are not removed to prevent
			 * loading them again
			 */
			final ArrayList<Tile> tileChildren = tile.getChildren();
			if (tileChildren != null) {

				// set parent to null in each child because the parent will be removed
				for (final Tile tileChild : tileChildren) {
					tileChild.setParentTile(null);
				}

				// remove parent 
				final String parentTileKey = tile.getTileKey();
				tileCache.remove(parentTileKey);
				tileCacheFifo.remove(parentTileKey);
			}
		}
	}

	private void removeTile(final String tileKey) {

		final Tile removedTile = tileCache.remove(tileKey);

		if (removedTile == null) {
			return;
		}

		ArrayList<Tile> tileChildren = removedTile.getChildren();
		if (tileChildren != null) {

			// this is a parent tile, remove also all child tiles

			removeTileChildren(tileChildren);

		} else {

			final Tile parentTile = removedTile.getParentTile();

			if (parentTile == null) {

				// this is a 'normal' tile without parent or children

			} else {

				// this is a child tile

				tileChildren = parentTile.getChildren();
				if (tileChildren != null) {
					removeTileChildren(tileChildren);
				}
			}
		}
	}

	/**
	 * Removes tile children to prevent memory leaks
	 * 
	 * @param tileChildren
	 */
	private void removeTileChildren(final ArrayList<Tile> tileChildren) {

		for (final Tile tileChild : tileChildren) {

			if (tileChild.isLoading()) {
				continue;
			}

			// remove orphan child
			tileCache.remove(tileChild.getTileKey());

			tileChild.setParentTile(null);
		}

		tileChildren.clear();
	}

	/**
	 * Reset overlay state for all tiles in the cache
	 */
	public void resetOverlays() {

		for (final Tile tile : tileCache.values()) {
			tile.resetOverlay();
		}
	}

	public void resetTileImageAvailability() {

		for (final Tile tile : tileCache.values()) {
			tile.setIsOfflineImageAvailable(false);
		}
	}

	/**
	 * Stop downloading tiles
	 */
	public void stopLoadingTiles() {

		for (final Tile tile : tileCache.values()) {

			if (tile.isLoading()) {

				// reset loading state
				tile.setLoading(false);

				final Future<?> future = tile.getFuture();

				if (future != null) {

					if (future.isCancelled() == false) {
						future.cancel(true);
					}
				}
			}
		}
	}

}
