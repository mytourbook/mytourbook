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

import java.util.Collection;
import java.util.Set;

/**
 * Original: {@link com.google.maps.android.clustering.algo.Algorithm<T>}
 * <p>
 * Logic for computing clusters
 */
public interface Algorithm<T extends ClusterItem> {

	void addItem(T item);

	void addItems(Collection<T> items);

	void clearItems();

	Set<? extends Cluster<T>> getClusters(double zoom);

	Collection<T> getItems();

	void removeItem(T item);
}
