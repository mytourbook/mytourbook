package de.byteholder.geoclipse.map;


public interface ITilePainter {

	/**
	 * Draws a tile
	 * 
	 * @param tile
	 * @return RGB data of the drawn tile
	 */
	int[][] drawTile(Tile tile);

}
