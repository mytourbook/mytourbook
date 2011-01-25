package net.tourbook.export.openlayers;

/**
 * Value object for velocity context containing map view info
 */
public class MapContext {

	private double lat;
	private double lon;
	private int zoom;

	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLon() {
		return lon;
	}
	public void setLon(double lon) {
		this.lon = lon;
	}
	public int getZoom() {
		return zoom;
	}
	public void setZoom(int zoom) {
		this.zoom = zoom;
	}
}
