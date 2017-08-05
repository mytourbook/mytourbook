/*
 * Original: org.oscim.layers.marker.InternalItem
 */
package net.tourbook.map25.layer.marker;

/**
 * The internal representation of a marker.
 */
class ProjectedMarker {

	MapMarker	mapMarker;

	boolean		isVisible;
	boolean		isModified;

	float		x;
	float		y;

	/**
	 * Projected X position 0...1
	 */
	double		projectedX;

	/**
	 * Projected Y position 0...1
	 */
	double		projectedY;

	/**
	 * Projected cluster X position 0...1 when {@link #clusterSize} > 0
	 */
	double		projectedClusterX;

	/**
	 * Projected cluster Y position 0...1 when {@link #clusterSize} > 0
	 */
	double		projectedClusterY;

	float		dy;

	/**
	 * If this is true, this item is hidden (because it's represented by another InternalItem acting
	 * as cluster.
	 */
	boolean		isClusteredOut;

	/**
	 * If this is >0, this item will be displayed as a cluster circle, with size clusterSize+1.
	 */
	int			clusterSize;

	@Override
	public String toString() {
		return "\n" + x + ":" + y + " / " + dy + " " + isVisible;
	}
}
