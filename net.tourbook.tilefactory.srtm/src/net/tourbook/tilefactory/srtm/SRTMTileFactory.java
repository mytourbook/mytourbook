/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/
package net.tourbook.tilefactory.srtm;

import net.tourbook.ext.srtm.ElevationColor;
import net.tourbook.ext.srtm.ElevationLayer;
import net.tourbook.ext.srtm.GeoLat;
import net.tourbook.ext.srtm.GeoLon;
import net.tourbook.ext.srtm.NumberForm;
import net.tourbook.ext.srtm.PrefPageSRTMColors;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
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

	private static SRTMTileFactoryInfo	info			= new SRTMTileFactoryInfo();

	/**
	 * cache for tile images
	 */
	final static ElevationColor			elevationColor	= new ElevationColor();

	private static class SRTMTileFactoryInfo extends TileFactoryInfo implements ITilePainter {

		private static final String		FACTORY_ID		= "srtm";				//$NON-NLS-1$
		private static final String		FACTORY_NAME	= "SRTM";				//$NON-NLS-1$
		private static final String		FACTORY_OS_NAME	= "srtm";				//$NON-NLS-1$

		private static final String		SEPARATOR		= "/";					//$NON-NLS-1$

		private static final int		MIN_ZOOM		= 0;
		private static final int		MAX_ZOOM		= 17;
		private static final int		TOTAL_ZOOM		= 17;

		private static final String		BASE_URL		= "file://dummy";		//$NON-NLS-1$
		private static final String		FILE_EXT		= "png";				//$NON-NLS-1$

		// initialize SRTM loading
		public final NumberForm			numberForm		= new NumberForm();
		private final ElevationLayer	elevationLayer	= new ElevationLayer();

		public SRTMTileFactoryInfo() {

			super(MIN_ZOOM, MAX_ZOOM, TOTAL_ZOOM, 256, true, true, BASE_URL, "x", "y", "z"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
					.append(PrefPageSRTMColors.getSelectedProfile().getProfilePath())
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

		// TODO
		@Override
		public int hashCode() {
			return elevationColor.hashCode();
		}

		public ImageData[] paintTile(final Tile tile) {

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

					// elevation is used at every grid-th pixel in both directions; 
					// the other values are interpolated
					// i.e. it gives the resolution of the image!
					final int grid = elevationColor.getGrid();

					System.out.println(Messages.getString("srtm_tile_factory_painting_tile") //$NON-NLS-1$
							+ "(L=" //$NON-NLS-1$
							+ elevationLayer.getName()
							+ ", G=" //$NON-NLS-1$
							+ grid
							+ ", X=" //$NON-NLS-1$
							+ tileX
							+ ", Y=" //$NON-NLS-1$
							+ tileY
							+ ", Z=" //$NON-NLS-1$
							+ tileZoom
							+ ")"); //$NON-NLS-1$

					double lon = 0.;
					double lat = 0.;
					final double pi = Math.PI;
					final double twoPi = 2 * pi;
					final double constMx1 = 360. / pi;
					final double constMx2 = twoPi / mapPower;
					final double constMy = 360. / mapPower;
					int mapStartX = tileX * tileSize;
					final int mapStartY = tileY * tileSize;
					RGB rgb;

					if (grid == 1) {

						double elevOld = 0;
						final boolean isShadowState = elevationColor.isShadowState();
						final int drawStartX = isShadowState ? -1 : 0;
						mapStartX += drawStartX;
						final double lonStart = constMy * mapStartX - 180.; // Mercator

						for (int drawY = 0, mapY = mapStartY; drawY < tileSize; drawY++, mapY++) {

							lat = constMx1 * Math.atan(Math.exp(pi - constMx2 * mapY)) - 90.; // Mercator
							lon = lonStart;

							for (int drawX = drawStartX; drawX < tileSize; drawX++, lon += constMy) {

								final double elev = elevationLayer.getElevation(new GeoLat(lat), new GeoLon(lon));

								if (drawX == -1) {
									elevOld = elev;
									continue;
								}

								if (isShadowState && elev < elevOld)
									rgb = elevationColor.getDarkerRGB((int) elev);
								else
									rgb = elevationColor.getRGB((int) elev);
								elevOld = elev;

								final Color color = new Color(display, rgb);
								gc.setForeground(color);
								gc.drawPoint(drawX, drawY);

								color.dispose();
							}
						}

					} else { // grid > 1

						final int gridQuot = grid - 1;
						double lonOld = 0.;
						double latOld = 0.;

						for (int pixelY = 0, mapY = mapStartY; pixelY <= tileSize; pixelY += grid, mapY += grid, latOld = lat) {

							lat = constMx1 * Math.atan(Math.exp(pi - constMx2 * mapY)) - 90.; // Mercator

							for (int pixelX = 0, mapX = mapStartX; pixelX <= tileSize; pixelX += grid, mapX += grid, lonOld = lon) {

								lon = constMy * mapX - 180.; // Mercator
								if (pixelX == 0 || pixelY == 0)
									continue;

								final double elev00 = elevationLayer.getElevation(new GeoLat(latOld),
										new GeoLon(lonOld));
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

										rgb = elevationColor.getRGB((int) elev);
										final Color color = new Color(display, rgb);

										gc.setForeground(color);
										gc.drawPoint(drawX, drawY);

										color.dispose();
									}
								}
							}
						}
					}

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
