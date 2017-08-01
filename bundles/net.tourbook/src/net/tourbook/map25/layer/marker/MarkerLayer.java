/*
 * Original: org.oscim.layers.marker.MarkerLayer<Item>
 */
package net.tourbook.map25.layer.marker;

import org.oscim.core.Point;
import org.oscim.layers.Layer;
import org.oscim.map.Map;

/**
 * Draws a list of {@link MarkerMap25Interface} as markers to a map. The item with the lowest index
 * is drawn as last and therefore the 'topmost' marker. It also gets checked for onTap first. This
 * class is generic, because you then you get your custom item-class passed back in onTap(). << TODO
 */
public abstract class MarkerLayer extends Layer {

	protected final MarkerRenderer	mMarkerRenderer;
	protected Map25Marker			mFocusedItem;

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

	public MarkerLayer(final Map map, final MarkerSymbol defaultSymbol) {

		super(map);

		mMarkerRenderer = new ClusterMarkerRenderer(this, defaultSymbol);
		mRenderer = mMarkerRenderer;
	}

	/**
	 * Method by which subclasses create the actual Items. This will only be called from populate()
	 * we'll cache them for later use.
	 */
	protected abstract Map25Marker createItem(int i);

	/**
	 * @return the currently-focused item, or null if no item is currently focused.
	 */
	public Map25Marker getFocus() {

		return mFocusedItem;
	}

	/**
	 * Utility method to perform all processing on a new ItemizedOverlay. Subclasses provide Items
	 * through the createItem(int) method. The subclass should call this as soon as it has data,
	 * before anything else gets called.
	 */
	public final void populate() {

		mMarkerRenderer.populate(size());
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

	/**
	 * The number of items in this overlay.
	 */
	public abstract int size();

	public void update() {
		mMarkerRenderer.update();
	}
}
