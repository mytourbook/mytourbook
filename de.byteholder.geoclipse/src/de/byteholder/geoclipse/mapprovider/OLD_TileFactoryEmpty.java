/*
 * EmptyTileFactory.java
 *
 * Created on June 7, 2006, 4:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package de.byteholder.geoclipse.mapprovider;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.TileFactoryImpl;
import de.byteholder.geoclipse.map.TileFactoryInfo_OLD;

/**
 * A null implementation of TileFactory. Draws empty areas.
 * 
 * @author joshy
 */
public class OLD_TileFactoryEmpty extends TileFactoryImpl {

	private static final String		FACTORY_ID			= "empty-tile-factory";				//$NON-NLS-1$
	private static final String		FACTORY_NAME		= Messages.Tile_Factory_Name_Hidden;

	private static final int		TILE_SIZE			= 256;

	private static TileFactoryInfo_OLD	emptyFactoryInfo	= new EmptyTileFactoryInfo();

	private Image					tileImage			= createTileImage();
	private MP_OLD						fMp;

	private static class EmptyTileFactoryInfo extends TileFactoryInfo_OLD {

		public EmptyTileFactoryInfo() {
			super(FACTORY_ID, 0, 17, 18, TILE_SIZE, true, true, "", "x", "y", "z"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		@Override
		public String getFactoryID() {
			return FACTORY_ID;
		}

		@Override
		public String getFactoryName() {
			return FACTORY_NAME;
		}

		@Override
		public String getTileOSFolder() {
			return null;
		}

		@Override
		public IPath getTileOSPath(final String fullPath, final int x, final int y, final int zoomLevel, final Tile tile) {
			return null;
		}

		@Override
		public String getTileUrl(final Tile tile) {
			return null;
		}

		@Override
		public boolean isMapEmpty() {
			return true;
		}

	}

	private static Image createTileImage() {

		final Display display = Display.getCurrent();
		final Image image = new Image(display, TILE_SIZE, TILE_SIZE);

		// set background to OpenStreetmap background color
		final Color tileColor = new Color(display, 0xf1, 0xee, 0xe8);
		final GC gc = new GC(image);
		{
			gc.setBackground(tileColor);
			gc.fillRectangle(0, 0, TILE_SIZE, TILE_SIZE);
		}
		gc.dispose();
		tileColor.dispose();

		return image;
	}

	public TileFactoryEmpty() {
		super(emptyFactoryInfo);
	}

	@Override
	public void dispose() {

		tileImage.dispose();

		super.dispose();
	}

	@Override
	public MP_OLD getMP() {
		return fMp;
	}

	/**
	 * Returns the tile that is located at the given tilePoint for this zoom. For example, if
	 * getMapSize() returns 10x20 for this zoom, and the tilePoint is (3,5), then the appropriate
	 * tile will be located and returned.
	 * 
	 * @param tilePoint
	 * @param zoom
	 * @return
	 */
	@Override
	public Tile getTile(int tilePositionX, final int tilePositionY, final int zoom) {

		/*
		 * create tile key, wrap the tiles horizontally --> mod the x with the max width and use
		 * that
		 */
		final int numTilesWide = (int) getMapSize(zoom).getWidth();
		if (tilePositionX < 0) {
			tilePositionX = numTilesWide - (Math.abs(tilePositionX) % numTilesWide);
		}
		tilePositionX = tilePositionX % numTilesWide;

		final String tileKey = Tile
				.getTileKey(null, tilePositionX, tilePositionY, zoom, null, null, projection.getId());

		/*
		 * check if tile can be used from the tile cache
		 */
		Tile tile = getTileCache().get(tileKey);
		if (tile != null) {
			return tile;
		}

		tile = new Tile(null, tilePositionX, tilePositionY, zoom, null);
		tile.setMapImage(tileImage);
		getTileCache().add(tileKey, tile);

		return tile;
	}

	public int getTileSize(final int zoom) {
		return TILE_SIZE;
	}

	@Override
	public void setMapProvider(final MP_OLD mapProvider) {
		fMp = mapProvider;
	}
}
