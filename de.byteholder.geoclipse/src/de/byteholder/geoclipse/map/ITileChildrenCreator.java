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
	 * Draw parent image by drawing all children over each other. When all children have errors, an
	 * image with the background color is returned
	 * 
	 * @param parentTile
	 * @param childTile
	 * @return Returns the image data for the parent image and the status if the image was drawn
	 */
	public ParentImageStatus getParentImage(Tile parentTile, Tile childTile);
}
