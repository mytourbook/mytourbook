package de.byteholder.geoclipse.map;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * cache for tiles
 */
public class TileCache {

	private static final int						MAX_TILE_CACHE_ENTRIES	= 1048;

	private final ConcurrentHashMap<String, Tile>	tileCache				= new ConcurrentHashMap<String, Tile>();
	private final ConcurrentLinkedQueue<String>		tileCacheFifo			= new ConcurrentLinkedQueue<String>();

	public void add(final String tileKey, final Tile tile) {

		final int cacheSize = tileCacheFifo.size();
		if (cacheSize > MAX_TILE_CACHE_ENTRIES) {

			// remove cache items 
			for (int cacheIndex = MAX_TILE_CACHE_ENTRIES; cacheIndex < cacheSize; cacheIndex++) {
				final String head = tileCacheFifo.poll();
				tileCache.remove(head);
			}
		}

		tileCache.put(tileKey, tile);
		tileCacheFifo.add(tileKey);
	}

	public synchronized void clear() {
		tileCache.clear();
		tileCacheFifo.clear();
	}

	public Tile get(final String tileKey) {
		return tileCache.get(tileKey);
	}

	/**
	 * Removes the tile from the tile cache
	 * 
	 * @param tileKey
	 */
	public void remove(final String tileKey) {

		final Tile removedTile = tileCache.remove(tileKey);
		if (removedTile != null) {
			tileCacheFifo.remove(removedTile);
		}
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
