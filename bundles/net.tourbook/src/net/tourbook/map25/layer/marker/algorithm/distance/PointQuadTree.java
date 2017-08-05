/*
 * Original: com.google.maps.android.quadtree.PointQuadTree<T>
 */
package net.tourbook.map25.layer.marker.algorithm.distance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A quad tree which tracks items with a Point geometry. See http://en.wikipedia.org/wiki/Quadtree
 * for details on the data structure. This class is not thread safe.
 */
public class PointQuadTree<T extends PointQuadTree.Item> {

	/**
	 * Maximum number of elements to store in a quad before splitting.
	 */
	private final static int		MAX_ELEMENTS	= 50;

	/**
	 * Maximum depth.
	 */
	private final static int		MAX_DEPTH		= 40;

	/**
	 * The bounds of this quad.
	 */
	private final Bounds			mBounds;

	/**
	 * The depth of this quad in the tree.
	 */
	private final int				mDepth;

	/**
	 * The elements inside this quad, if any.
	 */
	private List<T>					mItems;

	/**
	 * Child quads.
	 */
	private List<PointQuadTree<T>>	mChildren		= null;

	public interface Item {
		public Point getPoint();
	}

	public PointQuadTree(final Bounds bounds) {
		this(bounds, 0);
	}

	private PointQuadTree(final Bounds bounds, final int depth) {
		mBounds = bounds;
		mDepth = depth;
	}

	/**
	 * Creates a new quad tree with specified bounds.
	 *
	 * @param minX
	 * @param maxX
	 * @param minY
	 * @param maxY
	 */
	public PointQuadTree(final double minX, final double maxX, final double minY, final double maxY) {
		this(new Bounds(minX, maxX, minY, maxY));
	}

	private PointQuadTree(final double minX, final double maxX, final double minY, final double maxY, final int depth) {
		this(new Bounds(minX, maxX, minY, maxY), depth);
	}

	/**
	 * Insert an item.
	 */
	public void add(final T item) {

		final Point point = item.getPoint();

		if (this.mBounds.contains(point.x, point.y)) {
			insert(point.x, point.y, item);
		}
	}

	/**
	 * Removes all points from the quadTree
	 */
	public void clear() {

		mChildren = null;

		if (mItems != null) {
			mItems.clear();
		}
	}

	private void insert(final double x, final double y, final T item) {

		if (this.mChildren != null) {
			if (y < mBounds.midY) {
				if (x < mBounds.midX) { // top left
					mChildren.get(0).insert(x, y, item);
				} else { // top right
					mChildren.get(1).insert(x, y, item);
				}
			} else {
				if (x < mBounds.midX) { // bottom left
					mChildren.get(2).insert(x, y, item);
				} else {
					mChildren.get(3).insert(x, y, item);
				}
			}
			return;
		}

		if (mItems == null) {
			mItems = new ArrayList<T>();
		}

		mItems.add(item);

		if (mItems.size() > MAX_ELEMENTS && mDepth < MAX_DEPTH) {
			split();
		}
	}

	private boolean remove(final double x, final double y, final T item) {

		if (this.mChildren != null) {
			if (y < mBounds.midY) {
				if (x < mBounds.midX) { // top left
					return mChildren.get(0).remove(x, y, item);
				} else { // top right
					return mChildren.get(1).remove(x, y, item);
				}
			} else {
				if (x < mBounds.midX) { // bottom left
					return mChildren.get(2).remove(x, y, item);
				} else {
					return mChildren.get(3).remove(x, y, item);
				}
			}
		} else {
			if (mItems == null) {
				return false;
			} else {
				return mItems.remove(item);
			}
		}
	}

	/**
	 * Remove the given item from the set.
	 *
	 * @return whether the item was removed.
	 */
	public boolean remove(final T item) {
		final Point point = item.getPoint();
		if (this.mBounds.contains(point.x, point.y)) {
			return remove(point.x, point.y, item);
		} else {
			return false;
		}
	}

	/**
	 * Search for all items within a given bounds.
	 */
	public Collection<T> search(final Bounds searchBounds) {

		final List<T> results = new ArrayList<T>();
		search(searchBounds, results);

		return results;
	}

	private void search(final Bounds searchBounds, final Collection<T> results) {

		if (!mBounds.intersects(searchBounds)) {
			return;
		}

		if (this.mChildren != null) {
			for (final PointQuadTree<T> quad : mChildren) {
				quad.search(searchBounds, results);
			}
		} else if (mItems != null) {
			if (searchBounds.contains(mBounds)) {
				results.addAll(mItems);
			} else {
				for (final T item : mItems) {
					if (searchBounds.contains(item.getPoint())) {
						results.add(item);
					}
				}
			}
		}
	}

	/**
	 * Split this quad.
	 */
	private void split() {

		mChildren = new ArrayList<PointQuadTree<T>>(4);
		mChildren.add(new PointQuadTree<T>(mBounds.minX, mBounds.midX, mBounds.minY, mBounds.midY, mDepth + 1));
		mChildren.add(new PointQuadTree<T>(mBounds.midX, mBounds.maxX, mBounds.minY, mBounds.midY, mDepth + 1));
		mChildren.add(new PointQuadTree<T>(mBounds.minX, mBounds.midX, mBounds.midY, mBounds.maxY, mDepth + 1));
		mChildren.add(new PointQuadTree<T>(mBounds.midX, mBounds.maxX, mBounds.midY, mBounds.maxY, mDepth + 1));

		final List<T> items = mItems;
		mItems = null;

		for (final T item : items) {
			// re-insert items into child quads.
			insert(item.getPoint().x, item.getPoint().y, item);
		}
	}
}
