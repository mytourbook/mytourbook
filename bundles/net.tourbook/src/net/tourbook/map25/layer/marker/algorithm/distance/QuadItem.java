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

import java.util.Collections;
import java.util.Set;

import org.oscim.core.GeoPoint;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;

/**
 * Original:
 * {@link com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm.QuadItem<T>}
 * <p>
 * 
 * @param <T>
 */
public class QuadItem<T extends ClusterItem> implements PointQuadTree.Item, Cluster<T> {

	public final T			mClusterItem;

	public final Point		mPoint;

	private final GeoPoint	mPosition;

	private Set<T>			singletonSet;

	QuadItem(final T item) {

		mClusterItem = item;
		mPosition = item.getPosition();

		mPoint = MercatorProjection.project(mPosition, null);

		singletonSet = Collections.singleton(mClusterItem);
	}

	@Override
	public boolean equals(final Object other) {

		if (!(other instanceof QuadItem<?>)) {
			return false;
		}

		return ((QuadItem<?>) other).mClusterItem.equals(mClusterItem);
	}

	@Override
	public Set<T> getItems() {
		return singletonSet;
	}

	@Override
	public Point getPoint() {
		return mPoint;
	}

	@Override
	public GeoPoint getPosition() {
		return mPosition;
	}

	@Override
	public int getSize() {
		return 1;
	}

	@Override
	public int hashCode() {
		return mClusterItem.hashCode();
	}

	@Override
	public String toString() {

		return "\n" //$NON-NLS-1$

				+ "\tQuadItem\t\t[" //$NON-NLS-1$

				+ "mClusterItem=" + mClusterItem + ", " //$NON-NLS-1$ //$NON-NLS-2$
				//					+ "mPoint=" + mPoint + ", "
				//					+ "mPosition=" + mPosition + ", "
				//					+ "singletonSet=" + singletonSet

				+ "]"; //$NON-NLS-1$
	}
}
