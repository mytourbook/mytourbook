/*
 * Original: org.oscim.layers.marker.MarkerLayer<Item>
 */
package net.tourbook.map25.layer.marker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.oscim.core.Box;
import org.oscim.core.Point;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Draws a list of {@link MarkerMap25Interface} as markers to a map. The item with the lowest index
 * is drawn as last and therefore the 'topmost' marker. It also gets checked for onTap first. This
 * class is generic, because you then you get your custom item-class passed back in onTap(). << TODO
 */
public class MarkerLayer extends Layer implements GestureListener {

	private static final Logger		log					= LoggerFactory.getLogger(MarkerLayer.class);

	private final MarkerRenderer	mMarkerRenderer;
	private Map25Marker				mFocusedItem;
	private final List<Map25Marker>	mItemList			= new ArrayList<>();
	private final Point				mTmpPoint			= new Point();
	private int						mDrawnItemsLimit	= Integer.MAX_VALUE;

	private OnItemGestureListener	mOnItemGestureListener;

	private final ActiveItem		mActiveItemSingleTap;
	private final ActiveItem		mActiveItemLongPress;

	{
		mActiveItemSingleTap = new ActiveItem() {

			@Override
			public boolean run(final int index) {

				final MarkerLayer that = MarkerLayer.this;
				if (mOnItemGestureListener == null) {
					return false;
				}

				return onSingleTapUpHelper(
						index,
						that.mItemList.get(index));
			}
		};

		mActiveItemLongPress = new ActiveItem() {

			@Override
			public boolean run(final int index) {

				final MarkerLayer that = MarkerLayer.this;

				if (that.mOnItemGestureListener == null) {
					return false;
				}

				return onLongPressHelper(
						index,
						that.mItemList.get(index));
			}
		};
	}

	public static interface ActiveItem {

		public boolean run(int aIndex);
	}

	/**
	 * When the item is touched one of these methods may be invoked depending on the type of touch.
	 * Each of them returns true if the event was completely handled.
	 */
	public static interface OnItemGestureListener {

		public boolean onItemLongPress(int index, Map25Marker item);

		public boolean onItemSingleTapUp(int index, Map25Marker item);
	}

	/**
	 * TODO Interface definition for overlays that contain items that can be snapped to (for
	 * example, when the user invokes a zoom, this could be called allowing the user to snap the
	 * zoom to an interesting point.)
	 */
	public interface Snappable {

		/**
		 * Checks to see if the given x and y are close enough to an item resulting in snapping the
		 * current action (e.g. zoom) to the item.
		 *
		 * @param x
		 *            The x in screen coordinates.
		 * @param y
		 *            The y in screen coordinates.
		 * @param snapPoint
		 *            To be filled with the the interesting point (in screen coordinates) that is
		 *            closest to the given x and y. Can be untouched if not snapping.
		 * @return Whether or not to snap to the interesting point.
		 */
		boolean onSnapToItem(int x, int y, Point snapPoint);
	}

	public MarkerLayer(final Map map, final MarkerSymbol defaultSymbol, final OnItemGestureListener listener) {

		super(map);

		mMarkerRenderer = new MarkerRenderer(this, defaultSymbol);
		mRenderer = mMarkerRenderer;
		mOnItemGestureListener = listener;

		populate();
	}

	/**
	 * When a content sensitive action is performed the content item needs to be identified. This
	 * method does that and then performs the assigned task on that item.
	 *
	 * @return true if event is handled false otherwise
	 */
	protected boolean activateSelectedItems(final MotionEvent event, final ActiveItem task) {

		final int size = mItemList.size();
		if (size == 0) {
			return false;
		}

		final int eventX = (int) event.getX() - mMap.getWidth() / 2;
		final int eventY = (int) event.getY() - mMap.getHeight() / 2;
		final Viewport mapPosition = mMap.viewport();

		final Box box = mapPosition.getBBox(null, 128);
		box.map2mercator();
		box.scale(1E6);

		int nearest = -1;
		int inside = -1;
		double insideY = -Double.MAX_VALUE;

		/* squared dist: 50*50 pixel ~ 2mm on 400dpi */
		double dist = 2500;

		for (int i = 0; i < size; i++) {

			final Map25Marker item = mItemList.get(i);

			if (!box.contains(
					item.getGeoPoint().longitudeE6,
					item.getGeoPoint().latitudeE6)) {
				continue;
			}

			mapPosition.toScreenPoint(item.getGeoPoint(), mTmpPoint);

			final float dx = (float) (mTmpPoint.x - eventX);
			final float dy = (float) (mTmpPoint.y - eventY);

			MarkerSymbol it = item.getMarkerSymbol();

			if (it == null) {
				it = mMarkerRenderer.defaultMarkerSymbol;
			}

			if (it.isInside(dx, dy)) {
				if (mTmpPoint.y > insideY) {
					insideY = mTmpPoint.y;
					inside = i;
				}
			}
			if (inside >= 0) {
				continue;
			}

			final double d = dx * dx + dy * dy;
			if (d > dist) {
				continue;
			}

			dist = d;
			nearest = i;
		}

		if (inside >= 0) {
			nearest = inside;
		}

		if (nearest >= 0 && task.run(nearest)) {
			mMarkerRenderer.update();
			mMap.render();
			return true;
		}

		return false;
	}

	public void addItem(final int location, final Map25Marker item) {

		mItemList.add(location, item);
	}

	public boolean addItem(final Map25Marker item) {

		final boolean result = mItemList.add(item);
		populate();

		return result;
	}

	public boolean addItems(final Collection<Map25Marker> items) {

		final boolean result = mItemList.addAll(items);

		populate();

		return result;
	}

	protected Map25Marker createItem(final int index) {
		return mItemList.get(index);
	}

	/**
	 * @return the currently-focused item, or null if no item is currently focused.
	 */
	public Map25Marker getFocus() {

		return mFocusedItem;
	}

	public List<Map25Marker> getItemList() {
		return mItemList;
	}

	@Override
	public boolean onGesture(final Gesture g, final MotionEvent e) {

		if (g instanceof Gesture.Tap) {
			return activateSelectedItems(e, mActiveItemSingleTap);
		}

		if (g instanceof Gesture.LongPress) {
			return activateSelectedItems(e, mActiveItemLongPress);
		}

		return false;
	}

	protected boolean onLongPressHelper(final int index, final Map25Marker item) {

		return this.mOnItemGestureListener.onItemLongPress(index, item);
	}

	/**
	 * Each of these methods performs a item sensitive check. If the item is located its
	 * corresponding method is called. The result of the call is returned. Helper methods are
	 * provided so that child classes may more easily override behavior without resorting to
	 * overriding the ItemGestureListener methods.
	 */
	protected boolean onSingleTapUpHelper(final int index, final Map25Marker item) {

		return mOnItemGestureListener.onItemSingleTapUp(index, item);
	}

	/**
	 * Utility method to perform all processing on a new ItemizedOverlay. Subclasses provide Items
	 * through the createItem(int) method. The subclass should call this as soon as it has data,
	 * before anything else gets called.
	 */
	public final void populate() {

		mMarkerRenderer.populate(size());
	}

	public void removeAllItems() {
		removeAllItems(true);
	}

	public void removeAllItems(final boolean withPopulate) {

		mItemList.clear();

		if (withPopulate) {
			populate();
		}
	}

	public Map25Marker removeItem(final int position) {

		final Map25Marker result = mItemList.remove(position);
		populate();

		return result;
	}

	public boolean removeItem(final Map25Marker item) {

		final boolean result = mItemList.remove(item);
		populate();

		return result;
	}

	/**
	 * TODO If the given Item is found in the overlay, force it to be the current focus-bearer. Any
	 * registered {link ItemizedLayer#OnFocusChangeListener} will be notified. This does not move
	 * the map, so if the Item isn't already centered, the user may get confused. If the Item is not
	 * found, this is a no-op. You can also pass null to remove focus.
	 *
	 * @param item
	 */
	public void setFocus(final Map25Marker item) {

		mFocusedItem = item;
	}

	public void setOnItemGestureListener(final OnItemGestureListener listener) {

		mOnItemGestureListener = listener;
	}

	public int size() {
		return Math.min(mItemList.size(), mDrawnItemsLimit);
	}

	public void update() {
		mMarkerRenderer.update();
	}
}
