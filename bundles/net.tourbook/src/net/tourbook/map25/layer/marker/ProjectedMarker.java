/*
 * Original: org.oscim.layers.marker.InternalItem
 */
package net.tourbook.map25.layer.marker;

/**
 * The internal representation of a marker.
 */
class ProjectedMarker {

	Map25Marker	item;

	boolean		visible;
	boolean		changes;

	float		x;
	float		y;

	double		px;
	double		py;

	float		dy;

	/**
	 * Extension to the above class for clustered items. This could be a separate 1st level class,
	 * but it is included here not to pollute the source tree with tiny new files. It only adds a
	 * couple properties to InternalItem, and the semantics "InternalItem.Clustered" are not bad.
	 */
	static class Clustered extends ProjectedMarker {

		/**
		 * If this is >0, this item will be displayed as a cluster circle, with size clusterSize+1.
		 */
		int		clusterSize;

		/**
		 * If this is true, this item is hidden (because it's represented by another InternalItem
		 * acting as cluster.
		 */
		boolean	clusteredOut;
	}

	@Override
	public String toString() {
		return "\n" + x + ":" + y + " / " + dy + " " + visible;
	}
}
