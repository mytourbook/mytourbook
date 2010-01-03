package de.byteholder.geoclipse.map;

import org.eclipse.swt.graphics.GC;

public abstract class AbstractPainter<T> implements Painter<T> {
 
	protected T	fMap;

	/**
	 * Dispose resources in the {@link Painter}
	 */
	protected abstract void dispose();

	protected abstract void doPaint(GC gc, T map);

	/**
	 * @param gc
	 * @param map
	 * @param tile
	 * @param partedTileSize
	 * @param parts
	 * @return Returns <code>true</code> when painting was done
	 */
	protected abstract boolean doPaint(GC gc, T map, Tile tile, int parts);

	public void paint(final GC gc, final T parameter) {
		fMap = parameter;
		doPaint(gc, fMap);
	}

	/**
	 * @param gc
	 * @param map
	 * @param tile
	 * @param tileSize
	 * @param parts
	 *            number or parts, this is an odd number where the requested tile image is in the
	 *            middle of a square with parts*parts drawing tiles
	 * @param transparentColor
	 * @return
	 */
	public boolean paint(final GC gc, final T map, final Tile tile, final int parts) {
		fMap = map;
		return doPaint(gc, fMap, tile, parts);
	}
}
