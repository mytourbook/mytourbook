package de.byteholder.geoclipse.map;

import java.awt.Rectangle;

import org.eclipse.swt.graphics.GC;

public class DirectPainterContext {

	/**
	 * GC in the onPaint event
	 */
	public GC			gc;

	/**
	 * viewport for the current map image
	 */
	public Rectangle	viewport;

}
