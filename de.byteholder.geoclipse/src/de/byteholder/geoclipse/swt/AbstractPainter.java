package de.byteholder.geoclipse.swt;

import org.eclipse.swt.graphics.GC;

import de.byteholder.geoclipse.map.Tile;

public abstract class AbstractPainter<T> implements Painter<T> {

	protected T	parameter;

	/**
	 * Dispose resources in the {@link Painter}
	 */
	protected abstract void dispose();

	protected abstract void doPaint(GC gc, T parameter);

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
		this.parameter = parameter;
		doPaint(gc, this.parameter);
	}

	/**
	 * @param gc
	 * @param parameter
	 * @param tile
	 * @param tileSize
	 * @param parts
	 *            number or parts, this is an odd number where the requested tile image is in the
	 *            middle of a square with parts*parts drawing tiles
	 * @param transparentColor
	 * @return
	 */
	public boolean paint(final GC gc, final T parameter, final Tile tile, final int parts) {
		this.parameter = parameter;
		return doPaint(gc, this.parameter, tile, parts);
	}
}
