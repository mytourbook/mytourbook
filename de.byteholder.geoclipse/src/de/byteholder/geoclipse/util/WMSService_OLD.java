/*
 * WMSService.java
 *
 * Created on October 7, 2006, 6:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */


package de.byteholder.geoclipse.util;

import de.byteholder.geoclipse.map.Mercator;


/**
 * A class that represents a WMS mapping service.
 * See http://en.wikipedia.org/wiki/Web_Map_Service for more information.
 * @author joshy
 */
public class WMSService_OLD {
	private String	baseUrl;
	private String	layer;

	/** Creates a new instance of WMSService */
	public WMSService_OLD() {
		// by default use a known nasa server
		setLayer("BMNG");
		setBaseUrl("http://wms.jpl.nasa.gov/wms.cgi?");
	}

	public WMSService_OLD(final String baseUrl, final String layer) {
		this.baseUrl = baseUrl;
		this.layer = layer;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getLayer() {
		return layer;
	}

	public void setBaseUrl(final String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public void setLayer(final String layer) {
		this.layer = layer;
	}

	public String toWMSURL(final int x, final int y, final int zoom, final int tileSize) {

		final String format = "image/jpeg";
//		final String layers = "BMNG";
		final String styles = "";
		final String srs = "EPSG:4326";
		final int ts = tileSize;

		final int circumference = widthOfWorldInPixels(zoom, tileSize);
		final double radius = circumference / (2 * Math.PI);
		final double ulx = Mercator.xToLong(x * ts, radius);
		final double uly = Mercator.yToLat(y * ts, radius);
		final double lrx = Mercator.xToLong((x + 1) * ts, radius);
		final double lry = Mercator.yToLat((y + 1) * ts, radius);
		final String bbox = ulx + "," + uly + "," + lrx + "," + lry;

		final String url = getBaseUrl()
				+ "version=1.1.1&request="
				+ ("GetMap&Layers=" + layer)
				+ ("&format=" + format)
				+ ("&BBOX=" + bbox)
				+ ("&width=" + ts)
				+ ("&height=" + ts)
				+ ("&SRS=" + srs)
				+ ("&Styles=" + styles)
				+ //"&transparent=TRUE"+
				"";
		return url;
	}

	private int widthOfWorldInPixels(final int zoom, final int TILE_SIZE) {
//        int TILE_SIZE = 256;
		final int tiles = (int) Math.pow(2, zoom);
		final int circumference = TILE_SIZE * tiles;
		return circumference;
	}

}
