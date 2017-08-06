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

/**
 * Original: {@link com.google.maps.android.clustering.algo.StaticCluster}
 * <p>
 * A cluster whose center is determined upon creation.
 */
public class StaticCluster<T extends ClusterItem> implements Cluster<T> {

	private final LatLng	mCenter;
	private final List<T>	mItems	= new ArrayList<T>();

	public StaticCluster(final LatLng center) {
		mCenter = center;
	}

	public boolean add(final T t) {
		return mItems.add(t);
	}

	@Override
	public boolean equals(final Object other) {

		if (!(other instanceof StaticCluster<?>)) {
			return false;
		}

		return ((StaticCluster<?>) other).mCenter.equals(mCenter)
				&& ((StaticCluster<?>) other).mItems.equals(mItems);
	}

	@Override
	public Collection<T> getItems() {
		return mItems;
	}

	@Override
	public LatLng getPosition() {
		return mCenter;
	}

	@Override
	public int getSize() {
		return mItems.size();
	}

	@Override
	public int hashCode() {
		return mCenter.hashCode() + mItems.hashCode();
	}

	public boolean remove(final T t) {
		return mItems.remove(t);
	}

	@Override
	public String toString() {

		return "StaticCluster{" +
				"mCenter=" + mCenter +
				", mItems.size=" + mItems.size() +
				'}';
	}
}
