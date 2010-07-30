package de.byteholder.geoclipse.map;

import java.io.InputStream;

import de.byteholder.geoclipse.logging.GeoException;

public interface ITileLoader {

	InputStream getTileImageStream(Tile tile) throws GeoException;

}
