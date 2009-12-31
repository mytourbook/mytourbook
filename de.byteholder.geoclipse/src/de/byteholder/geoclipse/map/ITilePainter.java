package de.byteholder.geoclipse.map;

import org.eclipse.swt.graphics.RGB;

public interface ITilePainter {

	/**
	 * Draws a tile
	 * 
	 * @param tile
	 * @return RGB data of the drawn tile
	 */
	RGB[][] drawTile(Tile tile);

}
