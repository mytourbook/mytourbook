package de.byteholder.geoclipse.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * cache for tiles
 */
public class TileCache {

	private static final int						MAX_TILE_CACHE_ENTRIES	= 2000;

	private final ConcurrentHashMap<String, Tile>	tileCache				= new ConcurrentHashMap<String, Tile>();
	private final ConcurrentLinkedQueue<String>		tileCacheFifo			= new ConcurrentLinkedQueue<String>();

	public void add(final String tileKey, final Tile tile) {

		final int cacheSize = tileCacheFifo.size();
		if (cacheSize > MAX_TILE_CACHE_ENTRIES) {

			// remove cached tiles 
			for (int cacheIndex = MAX_TILE_CACHE_ENTRIES; cacheIndex < cacheSize; cacheIndex++) {

				final Tile removedTile = tileCache.remove(tileCacheFifo.poll());

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
		}

		tileCache.put(tileKey, tile);
		tileCacheFifo.add(tileKey);
	}

	public synchronized void clear() {

		final Collection<Tile> tiles = tileCache.values();
		for (final Tile tile : tiles) {

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
	}

	public Tile get(final String tileKey) {
		return tileCache.get(tileKey);
	}

	/**
	 * Removes tile children to prevent memory leaks
	 * 
	 * @param tileChildren
	 */
	private void removeTileChildren(final ArrayList<Tile> tileChildren) {

		for (final Tile tileChild : tileChildren) {

			// remove orphan child
			tileCache.remove(tileChild.getTileKey());

			tileChild.setParentTile(null);
		}

		tileChildren.clear();
	}

	/**
	 * Reset overlay status and image for all tiles in the cache
	 */
	public synchronized void resetOverlays() {

		final Collection<Tile> tiles = tileCache.values();
		for (final Tile tile : tiles) {
			tile.resetOverlay();
		}
	}

	public void resetTileImageAvailability() {
		final Collection<Tile> tiles = tileCache.values();
		for (final Tile tile : tiles) {
			tile.setIsOfflineImageAvailable(false);
		}
	}

}
