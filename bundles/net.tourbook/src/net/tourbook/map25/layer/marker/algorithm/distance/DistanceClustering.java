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
package net.tourbook.map25.layer.marker.algorithm.distance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.oscim.core.Point;

/**
 * Original: {@link com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm}
 * <p>
 * A simple clustering algorithm with O(nlog n) performance. Resulting clusters are not
 * hierarchical.
 * <p/>
 * High level algorithm:<br>
 * 1. Iterate over items in the order they were added (candidate clusters).<br>
 * 2. Create a cluster with the center of the item. <br>
 * 3. Add all items that are within a certain distance to the cluster. <br>
 * 4. Move any items out of an existing cluster if they are closer to another cluster. <br>
 * 5. Remove those items from the list of candidate clusters.
 * <p/>
 * Clusters have the center of the first element (not the centroid of the items within it).
 */
public class DistanceClustering<T extends ClusterItem> {

	public static final int						MAX_DISTANCE_AT_ZOOM	= 100;								// essentially 100 dp.

	/**
	 * Any modifications should be synchronized on mQuadTree.
	 */
	private final Collection<QuadItem<T>>		mItems					= new ArrayList<QuadItem<T>>();

	/**
	 * Any modifications should be synchronized on mQuadTree.
	 */
	private final PointQuadTree<QuadItem<T>>	mQuadTree				= new PointQuadTree<QuadItem<T>>(
			0,
			1,
			0,
			1);

	public void addItems(final Collection<T> items) {

		synchronized (mQuadTree) {

			for (final T item : items) {

				final QuadItem<T> quadItem = new QuadItem<T>(item);

				mItems.add(quadItem);
				mQuadTree.add(quadItem);
			}
		}
	}

	public void clearItems() {

		synchronized (mQuadTree) {

			mItems.clear();
			mQuadTree.clear();
		}
	}

	private Bounds createBoundsFromSpan(final Point p, final double span) {

		// TODO: Use a span that takes into account the visual size of the marker, not just its
		// GeoPoint.
		final double halfSpan = span / 2;

		return new Bounds(
				p.x - halfSpan,
				p.x + halfSpan,
				p.y - halfSpan,
				p.y + halfSpan);
	}

	private double distanceSquared(final Point a, final Point b) {
		return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
	}

	public Set<? extends Cluster<T>> getClusters(final double zoom, final int clusterGridSize) {

		final int discreteZoom = (int) zoom;

//		final double zoomSpecificSpan = MAX_DISTANCE_AT_ZOOM / Math.pow(2, discreteZoom) / 256;
		final double zoomSpecificSpan = clusterGridSize / Math.pow(2, discreteZoom) / 256;

		final Set<Cluster<T>> results = new HashSet<Cluster<T>>();

		final Set<QuadItem<T>> visitedCandidates = new HashSet<QuadItem<T>>();
		final Map<QuadItem<T>, Double> distanceToCluster = new HashMap<QuadItem<T>, Double>();
		final Map<QuadItem<T>, StaticCluster<T>> itemToCluster = new HashMap<QuadItem<T>, StaticCluster<T>>();

		synchronized (mQuadTree) {

			for (final QuadItem<T> candidate : mItems) {

				if (visitedCandidates.contains(candidate)) {

					// Candidate is already part of another cluster

					continue;
				}

				final Bounds searchBounds = createBoundsFromSpan(candidate.getPoint(), zoomSpecificSpan);
				final Collection<QuadItem<T>> clusterItems = mQuadTree.search(searchBounds);

				if (clusterItems.size() == 1) {

					// Only the current marker is in range. Just add the single item to the results

					results.add(candidate);
					visitedCandidates.add(candidate);
					distanceToCluster.put(candidate, 0d);

					continue;
				}

				final StaticCluster<T> cluster = new StaticCluster<T>(candidate.mClusterItem.getPosition());

				results.add(cluster);

				for (final QuadItem<T> clusterItem : clusterItems) {

					final Double existingDistance = distanceToCluster.get(clusterItem);
					final double distance = distanceSquared(clusterItem.getPoint(), candidate.getPoint());

					if (existingDistance != null) {

						// Item already belongs to another cluster. Check if it's closer to this cluster

						if (existingDistance < distance) {
							continue;
						}

						// Move item to the closer cluster
						itemToCluster.get(clusterItem).remove(clusterItem.mClusterItem);
					}

					distanceToCluster.put(clusterItem, distance);
					cluster.add(clusterItem.mClusterItem);
					itemToCluster.put(clusterItem, cluster);
				}

				visitedCandidates.addAll(clusterItems);
			}
		}

		return results;
	}

	public Collection<T> getItems() {

		final List<T> items = new ArrayList<T>();

		synchronized (mQuadTree) {

			for (final QuadItem<T> quadItem : mItems) {
				items.add(quadItem.mClusterItem);
			}
		}

		return items;
	}

}
