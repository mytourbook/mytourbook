/*
 * Original: com.google.maps.android.geometry.Bounds
 */
package net.tourbook.map25.layer.marker.algorithm.distance;

/**
 * Represents an area in the cartesian plane.
 */
public class Bounds {

	public final double	minX;
	public final double	minY;

	public final double	maxX;
	public final double	maxY;

	public final double	midX;
	public final double	midY;

	public Bounds(final double minX, final double maxX, final double minY, final double maxY) {

		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;

		midX = (minX + maxX) / 2;
		midY = (minY + maxY) / 2;
	}

	public boolean contains(final Bounds bounds) {
		return bounds.minX >= minX && bounds.maxX <= maxX && bounds.minY >= minY && bounds.maxY <= maxY;
	}

	public boolean contains(final double x, final double y) {
		return minX <= x && x <= maxX && minY <= y && y <= maxY;
	}

	public boolean contains(final Point point) {
		return contains(point.x, point.y);
	}

	public boolean intersects(final Bounds bounds) {
		return intersects(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY);
	}

	public boolean intersects(final double minX, final double maxX, final double minY, final double maxY) {
		return minX < this.maxX && this.minX < maxX && minY < this.maxY && this.minY < maxY;
	}
}
