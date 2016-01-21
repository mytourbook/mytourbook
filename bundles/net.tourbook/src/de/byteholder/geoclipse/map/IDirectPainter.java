package de.byteholder.geoclipse.map;

public interface IDirectPainter {

	/**
	 * The paint method is called when the map get's an onPaint event. Therefor this method should
	 * be optimized that it takes a short time
	 * 
	 * @param directMappingPainterContext
	 */
	public abstract void paint(DirectPainterContext directPainterContext);

	/**
	 * Dispose resources in the {@link IDirectPainter}
	 */
	public abstract void dispose();
}
