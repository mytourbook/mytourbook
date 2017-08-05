/*
 * Original: com.google.maps.android.geometry.Point
 */
package net.tourbook.map25.layer.marker.algorithm.distance;

public class Point {

	public final double	x;
	public final double	y;

	public Point(final double x, final double y) {

		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "Point{" +
				"x=" + x +
				", y=" + y +
				'}';
	}
}
