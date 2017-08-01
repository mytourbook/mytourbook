/*
 * Original: org.oscim.layers.marker.MarkerItem 
 */
package net.tourbook.map25.layer.marker;

import org.oscim.core.GeoPoint;

/**
 * Immutable class describing a GeoPoint with a Title and a Description.
 */
public class Map25Marker {

	public String			title;
	public String			description;

	public GeoPoint			geoPoint;

	protected MarkerSymbol	mMarker;

	/**
	 * @param title
	 *            this should be <b>singleLine</b> (no <code>'\n'</code> )
	 * @param description
	 *            a <b>multiLine</b> description ( <code>'\n'</code> possible)
	 */
	public Map25Marker(final String title, final String description, final GeoPoint geoPoint) {

		this.title = title;
		this.description = description;
		this.geoPoint = geoPoint;
	}

	public GeoPoint getGeoPoint() {
		return geoPoint;
	}

	public MarkerSymbol getMarkerSymbol() {
		return mMarker;
	}

}
