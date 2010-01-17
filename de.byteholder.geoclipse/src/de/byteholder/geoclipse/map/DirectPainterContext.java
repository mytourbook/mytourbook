package de.byteholder.geoclipse.map;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

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
