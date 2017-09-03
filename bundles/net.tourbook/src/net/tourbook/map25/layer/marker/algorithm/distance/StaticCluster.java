/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tourbook.map25.layer.marker.algorithm.distance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.oscim.core.GeoPoint;

/**
 * Original: {@link com.google.maps.android.clustering.algo.StaticCluster}
 * <p>
 * A cluster whose center is determined upon creation.
 */
public class StaticCluster<T extends ClusterItem> implements Cluster<T> {

	private final GeoPoint	_geoCenter;

	private final List<T>	_clusterItems	= new ArrayList<T>();

	public StaticCluster(final GeoPoint center) {
		_geoCenter = center;
	}

	public boolean add(final T t) {
		return _clusterItems.add(t);
	}

	@Override
	public boolean equals(final Object other) {

		if (!(other instanceof StaticCluster<?>)) {
			return false;
		}

		return ((StaticCluster<?>) other)._geoCenter.equals(_geoCenter)
				&& ((StaticCluster<?>) other)._clusterItems.equals(_clusterItems);
	}

	@Override
	public Collection<T> getItems() {
		return _clusterItems;
	}

	@Override
	public GeoPoint getPosition() {
		return _geoCenter;
	}

	@Override
	public int getSize() {
		return _clusterItems.size();
	}

	@Override
	public int hashCode() {
		return _geoCenter.hashCode() + _clusterItems.hashCode();
	}

	public boolean remove(final T t) {
		return _clusterItems.remove(t);
	}

	@Override
	public String toString() {
		return "\n" //$NON-NLS-1$

				+ "\tStaticCluster	[" //$NON-NLS-1$

				+ "_clusterItems.size=" + _clusterItems.size() + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "_geoCenter=" + _geoCenter + ", " //$NON-NLS-1$ //$NON-NLS-2$

				+ "]"; //$NON-NLS-1$
	}
}
