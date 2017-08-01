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

/**
 * Draws a list of {@link MarkerMap25Interface} as markers to a map. The item with the lowest index
 * is drawn as last and therefore the 'topmost' marker. It also gets checked for onTap first. This
 * class is generic, because you then you get your custom item-class passed back in onTap(). << TODO
 */
public class MarkerLayer extends Layer implements GestureListener {

	private final MarkerRenderer	_markerRenderer;

	private final List<MapMarker>		_allMarker			= new ArrayList<>();
	private MapMarker					_focusedMarker;

	private final Point				_tmpPoint			= new Point();
	private int						_drawnMarkerLimit	= Integer.MAX_VALUE;

	private OnItemGestureListener	_gestureListener;

	private final ActiveMarker		_activeMarker_SingleTap;
	private final ActiveMarker		_ActiveMarker_LongPress;

	{
		_activeMarker_SingleTap = new ActiveMarker() {

			@Override
			public boolean run(final int index) {

				final MarkerLayer that = MarkerLayer.this;

				if (_gestureListener == null) {
					return false;
				}

				return onSingleTapUpHelper(index, that._allMarker.get(index));
			}
		};

		_ActiveMarker_LongPress = new ActiveMarker() {

			@Override
			public boolean run(final int index) {

				final MarkerLayer that = MarkerLayer.this;

				if (that._gestureListener == null) {
					return false;
				}

				return onLongPressHelper(index, that._allMarker.get(index));
			}
		};
	}

	public static interface ActiveMarker {

		public boolean run(int aIndex);
	}

	/**
	 * When the item is touched one of these methods may be invoked depending on the type of touch.
	 * Each of them returns true if the event was completely handled.
	 */
	public static interface OnItemGestureListener {

		public boolean onItemLongPress(int index, MapMarker item);

		public boolean onItemSingleTapUp(int index, MapMarker item);
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

		mRenderer = _markerRenderer = new MarkerRenderer(this, defaultSymbol);

		_gestureListener = listener;

		populate();
	}

	/**
	 * When a content sensitive action is performed the content item needs to be identified. This
	 * method does that and then performs the assigned task on that item.
	 *
	 * @return true if event is handled false otherwise
	 */
	private boolean activateSelectedMarker(final MotionEvent event, final ActiveMarker task) {

		final int size = _allMarker.size();
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

		for (int markerIndex = 0; markerIndex < size; markerIndex++) {

			final MapMarker marker = _allMarker.get(markerIndex);

			if (!box.contains(
					marker.geoPoint.longitudeE6,
					marker.geoPoint.latitudeE6)) {

				continue;
			}

			mapPosition.toScreenPoint(marker.geoPoint, _tmpPoint);

			final float dx = (float) (_tmpPoint.x - eventX);
			final float dy = (float) (_tmpPoint.y - eventY);

			MarkerSymbol markerSymbol = marker.markerSymbol;

			if (markerSymbol == null) {
				markerSymbol = _markerRenderer.defaultMarkerSymbol;
			}

			if (markerSymbol.isInside(dx, dy)) {
				if (_tmpPoint.y > insideY) {
					insideY = _tmpPoint.y;
					inside = markerIndex;
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
			nearest = markerIndex;
		}

		if (inside >= 0) {
			nearest = inside;
		}

		if (nearest >= 0 && task.run(nearest)) {
			_markerRenderer.update();
			mMap.render();
			return true;
		}

		return false;
	}

	public void addItem(final int location, final MapMarker item) {

		_allMarker.add(location, item);
	}

	public boolean addItem(final MapMarker item) {

		final boolean result = _allMarker.add(item);
		populate();

		return result;
	}

	protected MapMarker getMarker(final int index) {
		return _allMarker.get(index);
	}

	/**
	 * @return the currently-focused item, or null if no item is currently focused.
	 */
	public MapMarker getFocus() {

		return _focusedMarker;
	}

	public List<MapMarker> getItemList() {
		return _allMarker;
	}

	@Override
	public boolean onGesture(final Gesture g, final MotionEvent e) {

		if (g instanceof Gesture.Tap) {
			return activateSelectedMarker(e, _activeMarker_SingleTap);
		}

		if (g instanceof Gesture.LongPress) {
			return activateSelectedMarker(e, _ActiveMarker_LongPress);
		}

		return false;
	}

	protected boolean onLongPressHelper(final int index, final MapMarker item) {

		return this._gestureListener.onItemLongPress(index, item);
	}

	/**
	 * Each of these methods performs a item sensitive check. If the item is located its
	 * corresponding method is called. The result of the call is returned. Helper methods are
	 * provided so that child classes may more easily override behavior without resorting to
	 * overriding the ItemGestureListener methods.
	 */
	protected boolean onSingleTapUpHelper(final int index, final MapMarker item) {

		return _gestureListener.onItemSingleTapUp(index, item);
	}

	/**
	 * Utility method to perform all processing on a new ItemizedOverlay. Subclasses provide Items
	 * through the createItem(int) method. The subclass should call this as soon as it has data,
	 * before anything else gets called.
	 */
	public final void populate() {

		_markerRenderer.populate(size());
	}

	public void replaceMarkers(final Collection<MapMarker> allMarkers) {

		_allMarker.clear();
		_allMarker.addAll(allMarkers);

		populate();
	}

	/**
	 * TODO If the given Item is found in the overlay, force it to be the current focus-bearer. Any
	 * registered {link ItemizedLayer#OnFocusChangeListener} will be notified. This does not move
	 * the map, so if the Item isn't already centered, the user may get confused. If the Item is not
	 * found, this is a no-op. You can also pass null to remove focus.
	 *
	 * @param item
	 */
	public void setFocus(final MapMarker item) {

		_focusedMarker = item;
	}

	public void setOnItemGestureListener(final OnItemGestureListener listener) {

		_gestureListener = listener;
	}

	public int size() {
		return Math.min(_allMarker.size(), _drawnMarkerLimit);
	}

	public void update() {
		_markerRenderer.update();
	}
}
