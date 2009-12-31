package de.byteholder.geoclipse.map;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public interface ITileChildrenCreator {

	/**
	 * Creates sub tiles
	 * 
	 * @param parentTile
	 * @param loadingTiles
	 */
	public ArrayList<Tile> createTileChildren(Tile parentTile, ConcurrentHashMap<String, Tile> loadingTiles);

	/**
	 * @param parentTile
	 * @param childTile
	 * @return Returns the image data for the parent image
 	 */
	public ParentImageStatus getParentImage(Tile parentTile, Tile childTile);
}
