package de.byteholder.geoclipse.map;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import net.tourbook.common.map.GeoPosition;

import de.byteholder.geoclipse.mapprovider.MP;

public class CylindricalProjection extends Projection {

	private static final String	PROJECTION_ID	= "cyl";	//$NON-NLS-1$

	@Override
	public Point geoToPixel(final GeoPosition c, final int zoom, final MP mp) {

		final int tileSize = mp.getTileSize();

		// calc the pixels per degree
		final Dimension mapSizeInTiles = mp.getMapTileSize(zoom);

		final double size_in_pixels = mapSizeInTiles.getWidth() * tileSize;
		final double ppd = size_in_pixels / 360;

		// the center of the world
		final double centerX = tileSize * mapSizeInTiles.getWidth() / 2;
		final double centerY = tileSize * mapSizeInTiles.getHeight() / 2;

		final double x = c.longitude * ppd + centerX;
		final double y = -c.latitude * ppd + centerY;

		return new Point((int) x, (int) y);
	}

	private Point2D.Double geoToPixelDouble(final GeoPosition c, final int zoom, final MP mp) {

		final int tileSize = mp.getTileSize();

		// calc the pixels per degree
		final Dimension mapSizeInTiles = mp.getMapTileSize(zoom);

		final double size_in_pixels = mapSizeInTiles.width * tileSize;
		final double ppd = size_in_pixels / 360;

		// the center of the world
		final double centerX = tileSize * mapSizeInTiles.width / 2;
		final double centerY = tileSize * mapSizeInTiles.height / 2;

		final double x = c.longitude * ppd + centerX;
		final double y = -c.latitude * ppd + centerY;

		return new Point2D.Double(x, y);
	}

	@Override
	public double getHorizontalDistance(final GeoPosition position1,
										final GeoPosition position2,
										final int zoom,
										final MP mp) {

		final Double devPos1 = geoToPixelDouble(position1, zoom, mp);
		final Double devPos2 = geoToPixelDouble(position2, zoom, mp);

		return devPos1.x - devPos2.x;
	}

	@Override
	public String getId() {
		return PROJECTION_ID;
	}

	@Override
	public GeoPosition pixelToGeo(final Point2D pixel, final int zoom, final MP mp) {

		final int tileSize = mp.getTileSize();

		// calc the pixels per degree
		final Dimension mapSizeInTiles = mp.getMapTileSize(zoom);
		final double size_in_pixels = mapSizeInTiles.getWidth() * tileSize;
		final double ppd = size_in_pixels / 360;

		// the center of the world
		final double centerX = tileSize * mapSizeInTiles.getWidth() / 2;
		final double centerY = tileSize * mapSizeInTiles.getHeight() / 2;

		final double lon = (pixel.getX() - centerX) / ppd;
		final double lat = -(pixel.getY() - centerY) / ppd;

		return new GeoPosition(lat, lon);
	}

}
