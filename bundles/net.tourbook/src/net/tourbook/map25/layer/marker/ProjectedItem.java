/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.map25.layer.marker;

/**
 * The internal representation of a marker, this can be a normal map marker or a cluster.
 * <p>
 * Original: {@link org.oscim.layers.marker.InternalItem}
 */
class ProjectedItem {

	MapMarker		mapMarker;

	boolean			isVisible;
	boolean			isModified;

	/**
	 * Map X position,
	 * 
	 * <pre>
	 * map center     0
	 * bottom-right   +x +y
	 * top-left       -x -y
	 * </pre>
	 */
	float			mapX;

	/**
	 * Map Y position
	 * 
	 * <pre>
	 * map center     0
	 * bottom-right   +x +y
	 * top-left       -x -y
	 * </pre>
	 */
	float			mapY;

	/**
	 * Projected X position <code>0...1</code>
	 */
	double			projectedX;

	/**
	 * Projected Y position <code>0...1</code>
	 */
	double			projectedY;

	/**
	 * Projected cluster Y position <code>0...1</code>, this is set when {@link #clusterSize} > 0
	 */
	double			projectedClusterX;

	/**
	 * Projected cluster Y position <code>0...1</code>, this is set when {@link #clusterSize} > 0
	 */
	double			projectedClusterY;

	float			dy;

	/**
	 * If this is true, this item is hidden (because it's represented by another InternalItem acting
	 * as cluster.
	 */
	boolean			isClusteredOut;

	/**
	 * When <code>> 0</code>, this item is a cluster and is displayed with a number of
	 * <code>clusterSize + 1</code>.
	 */
	int				clusterSize;

	/**
	 * When <code>true</code> then this item is contained in the cluster, this will correct the
	 * number of clusters.
	 */
	public boolean	isInGridCluster;

	@Override
	public String toString() {
		return "\n" + mapX + ":" + mapY + " / " + dy + " " + isVisible;
	}
}
