package net.tourbook.tilefactory.srtm;

//import net.tourbook.colors.ColorDefinition;
//import net.tourbook.colors.GraphColorProvider;
import net.tourbook.ext.srtm.ElevationLayer;
import net.tourbook.ext.srtm.GeoLat;
import net.tourbook.ext.srtm.GeoLon;
import net.tourbook.ext.srtm.NumberForm;
//import net.tourbook.mapping.LegendColor;
//import net.tourbook.mapping.LegendConfig;
//import net.tourbook.mapping.LegendProvider;
//import net.tourbook.ui.UI;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
//import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.map.DefaultTileFactory;
import de.byteholder.geoclipse.map.ITilePainter;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.TileFactoryInfo;

/**
 * @author Michael Kanis
 * @author Alfred Barten
 */
public class SRTMTileFactory extends DefaultTileFactory {

	private static SRTMTileFactoryInfo	info	= new SRTMTileFactoryInfo();

	/**
	 * cache for tile images
	 */

	private static class SRTMTileFactoryInfo extends TileFactoryInfo implements ITilePainter {

		private static final String		FACTORY_ID		= "srtm";
		private static final String		FACTORY_NAME	= "SRTM";
		private static final String		FACTORY_OS_NAME	= "srtm";

		private static final String		SEPARATOR		= "/";

		private static final int		MIN_ZOOM		= 0;
		private static final int		MAX_ZOOM		= 17;
		private static final int		TOTAL_ZOOM		= 17;

		private static final String		BASE_URL		= "file://dummy";
		private static final String		FILE_EXT		= "png";

		// initialize SRTM loading
		public final NumberForm			numberForm		= new NumberForm();
		private final ElevationLayer	elevationLayer	= new ElevationLayer();

		public SRTMTileFactoryInfo() {

			super(MIN_ZOOM, MAX_ZOOM, TOTAL_ZOOM, 256, true, true, BASE_URL, "x", "y", "z");
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
		public IPath getTileOSPath(final String fullPath, final int x, final int y, final int zoomLevel) {

			return new Path(fullPath).append(FACTORY_OS_NAME)
					.append(Integer.toString(zoomLevel))
					.append(Integer.toString(x))
					.append(Integer.toString(y))
					.addFileExtension(FILE_EXT);
		}

		@Override
		public ITilePainter getTilePainter() {
			return this;
		}

		@Override
		public String getTileUrl(final int x, final int y, final int zoom) {
			final StringBuilder url = new StringBuilder(this.getBaseURL()).append(SEPARATOR)
					.append(zoom)
					.append(SEPARATOR)
					.append(x)
					.append(SEPARATOR)
					.append(y)
					.append('.')
					.append(FILE_EXT);

			return url.toString();
		}

		public ImageData[] paintTile(final Tile tile) {

//			final GraphColorProvider colorProvider = GraphColorProvider.getInstance();
//			final ColorDefinition colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_MAP_SRTM);
//			final LegendColor legendColor = colorDefinition.getNewLegendColor();
//
//			final LegendProvider legendProvider = new LegendProvider(new LegendConfig(), legendColor, -1);
//			legendProvider.setLegendColorValues(new Rectangle(0, 0, 20, 20), 300, 1800, UI.EMPTY_STRING);

			final Display display = Display.getDefault();
			final ImageData[] paintedImageData = new ImageData[1];

			BusyIndicator.showWhile(display, new Runnable() {
				public void run() {

					final int tileSize = getTileSize();
					final int tileX = tile.getX();
					final int tileY = tile.getY();
					final int tileZoom = tile.getZoom();
					final int zoomPower = (int) Math.pow(2., tileZoom);
					final int mapPower = zoomPower * tileSize;

					final Image paintedImage = new Image(display, tileSize, tileSize);
					final GC gc = new GC(paintedImage);

					elevationLayer.setZoom(tileZoom);

					System.out.println(">>> Start painting tile " + elevationLayer.getName() + "(" + tileX + ", " + tileY + ", " + tileZoom + ")");

					// elevation is used at every grid-th pixel in both directions; 
					// the other values are interpolated
					// i.e. it gives the resolution of the image!
					final int grid = 4;
					final int gridQuot = grid - 1;
					double lon = 0.;
					double lonOld = 0.;
					double lat = 0.;
					double latOld = 0.;

					for (int pixelY = 0, mapY = tileY * tileSize; pixelY <= tileSize; pixelY += grid, mapY += grid, latOld = lat) {

						// TODO how to do that using Mercator class method yToLong??  
						lat = 360.
						* Math.atan(Math.exp(2 * Math.PI * (0.5 - (double) mapY / mapPower)))
						/ Math.PI
						- 90.; // Mercator

						for (int pixelX = 0, mapX = tileX * tileSize; pixelX <= tileSize; pixelX += grid, mapX += grid, lonOld = lon) {

							// lon = 2. * Math.PI * (Mercator.xToLat(mapX, mapPower) + 180.) - 180.; Using Mercator class is not simpler either!  
							lon = 360. * mapX / mapPower - 180.; // Mercator
							if (pixelX == 0 || pixelY == 0)
								continue;

							final double elev00 = elevationLayer.getElevation(new GeoLat(latOld), new GeoLon(lonOld));
							final double elev01 = elevationLayer.getElevation(new GeoLat(latOld), new GeoLon(lon));
							final double elev10 = elevationLayer.getElevation(new GeoLat(lat), new GeoLon(lonOld));
							final double elev11 = elevationLayer.getElevation(new GeoLat(lat), new GeoLon(lon));

							// interpolate elevation over this quad
							final double elevGridX0 = (elev01 - elev00) / gridQuot;
							final double elevGridX1 = (elev11 - elev10) / gridQuot;
							final double elevGridY0 = (elev10 - elev00) / gridQuot;
							// double elevGridY1 = (elev11 - elev01)/gridQuot; last elev in double for-loop gives this value
							final double elevGridX = (elevGridX1 - elevGridX0) / gridQuot;
							double elevStart = elev00;
							double elevGridXAdd = elevGridX0;

							for (int drawY = pixelY - grid; drawY < pixelY; drawY++, elevStart += elevGridY0, elevGridXAdd += elevGridX) {

								double elev = elevStart;
								for (int drawX = pixelX - grid; drawX < pixelX; drawX++, elev += elevGridXAdd) {

//										int elevInt = (int) (elev / 2) % 256;
									int elevInt = (int) (elev / 1000 * 256);

									// range check
									// elevInt = (elevInt < 0) ? -elevInt : elevInt;
									elevInt = elevInt > 0xff ? 0xff : elevInt;
									int elevRed = elevInt <= 0 ? 0 : elevInt;
									int elevGreen = elevInt <= 0 ? 0 : elevInt;
									int elevBlue = elevInt <= 0 ? 255 : elevInt;
									
									final Color color = new Color(display, elevRed, elevGreen, elevBlue);

//										final Color color = legendProvider.getValueColor((int) elev);

									gc.setForeground(color);
									gc.drawPoint(drawX, drawY);

									color.dispose();
								}
							}
						}
					}

					System.out.println(">>> Finish painting tile");

					gc.dispose();

					paintedImageData[0] = paintedImage.getImageData();
					paintedImage.dispose();

				}
			});

			return paintedImageData;
		}
	}

	public SRTMTileFactory() {
		super(info);
	}
}
