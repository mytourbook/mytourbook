/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.srtm.tilefactory;

import net.tourbook.srtm.ElevationColor;
import net.tourbook.srtm.ElevationLayer;
import net.tourbook.srtm.GeoLat;
import net.tourbook.srtm.GeoLon;
import net.tourbook.srtm.NumberForm;
import net.tourbook.srtm.PrefPageSRTMColors;
import net.tourbook.srtm.SRTMProfile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import de.byteholder.geoclipse.map.ITilePainter;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.mapprovider.MPPlugin;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;

/**
 * @author Michael Kanis
 * @author Alfred Barten
 */
public class SRTMMapProvider extends MPPlugin implements ITilePainter {

	private static ElevationColor	elevationColor;

	private static final String		FACTORY_ID		= "srtm";									//$NON-NLS-1$
	private static final String		OFFLINE_FOLDER	= "srtm";									//$NON-NLS-1$
 
	private static final String		FACTORY_NAME	= Messages.SRTM_MapProvider_Name;
	private static final String		DESCRIPTION		= Messages.SRTM_MapProvider_Description;

	private static final int		MIN_ZOOM		= 0;
	private static final int		MAX_ZOOM		= 17;


	// initialize SRTM loading
	public final NumberForm			numberForm		= new NumberForm();
	private final ElevationLayer	elevationLayer	= new ElevationLayer();

	public SRTMMapProvider() {

		setId(FACTORY_ID);
		setName(FACTORY_NAME);
		setDescription(DESCRIPTION);
		setZoomLevel(MIN_ZOOM, MAX_ZOOM);

		if (elevationColor == null) {
			elevationColor = new ElevationColor(this);
		}
	}

	@Override
	public void doPostCreation(final Tile tile) {

		/*
		 * add profile as custom data to the tile
		 */
		final SRTMProfile selectedProfile = PrefPageSRTMColors.getSelectedProfile();
		tile.setData(selectedProfile);
	}

	public int[][] drawTile(final Tile tile) {

		final SRTMProfile srtmProfile = (SRTMProfile) tile.getData();

		final int tileSize = tile.getMP().getTileSize();
		final int tileX = tile.getX();
		final int tileY = tile.getY();
		final int tileZoom = tile.getZoom();

		final int zoomPower = (int) Math.pow(2., tileZoom);
		final int mapPower = zoomPower * tileSize;

		final int[][] rgbData = new int[tileSize][tileSize];

		elevationLayer.setZoom(tileZoom);

		// elevation is used at every grid-th pixel in both directions; 
		// the other values are interpolated
		// i.e. it gives the resolution of the image!
		final int grid = srtmProfile.getResolutionValue();

		System.out.println(Messages.SRTM_MapProvider_Log_PaintingTile + ("(L=" + elevationLayer.getName())//$NON-NLS-1$
				+ (", G=" + grid)//$NON-NLS-1$
				+ (", X=" + tileX) //$NON-NLS-1$
				+ (", Y=" + tileY) //$NON-NLS-1$
				+ (", Z=" + tileZoom) //$NON-NLS-1$
				+ (")")); //$NON-NLS-1$

		double lon = 0.;
		double lat = 0.;
		final double pi = Math.PI;
		final double twoPi = 2 * pi;
		final double constMx1 = 360. / pi;
		final double constMx2 = twoPi / mapPower;
		final double constMy = 360. / mapPower;
		int mapStartX = tileX * tileSize;
		final int mapStartY = tileY * tileSize;
		int rgb;

		if (grid == 1) {

			double elevOld = 0;
			final boolean isShadowState = srtmProfile.isShadowState();
			final int drawStartX = isShadowState ? -1 : 0;
			mapStartX += drawStartX;
			final double lonStart = constMy * mapStartX - 180.; // Mercator
			final GeoLat geoLat = new GeoLat();
			final GeoLon geoLon = new GeoLon();

			for (int drawY = 0, mapY = mapStartY; drawY < tileSize; drawY++, mapY++) {

				lat = constMx1 * Math.atan(Math.exp(pi - constMx2 * mapY)) - 90.; // Mercator
				geoLat.set(lat);
				lon = lonStart;

				for (int drawX = drawStartX; drawX < tileSize; drawX++, lon += constMy) {
					geoLon.set(lon);
					final double elev = elevationLayer.getElevation(geoLat, geoLon);

					if (drawX == -1) {
						elevOld = elev;
						continue;
					}

					if (isShadowState && elev < elevOld) {
						rgb = srtmProfile.getShadowRGB((int) elev);
					} else {
						rgb = srtmProfile.getRGB((int) elev);
					}

					rgbData[drawY][drawX] = rgb;

					elevOld = elev;
				}
			}

		} else { // grid > 1

			final int gridQuot = grid - 1;

			final GeoLat geoLat = new GeoLat();
			final GeoLat geoLatOld = new GeoLat();
			final GeoLon geoLon = new GeoLon();
			final GeoLon geoLonOld = new GeoLon();
			for (int pixelY = 0, mapY = mapStartY; pixelY <= tileSize; pixelY += grid, mapY += grid, geoLatOld.set(lat)) {

				lat = constMx1 * Math.atan(Math.exp(pi - constMx2 * mapY)) - 90.; // Mercator
				geoLat.set(lat);

				for (int pixelX = 0, mapX = mapStartX; pixelX <= tileSize; pixelX += grid, mapX += grid, geoLonOld
						.set(lon)) {

					lon = constMy * mapX - 180.; // Mercator
					geoLon.set(lon);
					if (pixelX == 0 || pixelY == 0)
						continue;

					final double elev00 = elevationLayer.getElevation(geoLatOld, geoLonOld);
					final double elev01 = elevationLayer.getElevation(geoLatOld, geoLon);
					final double elev10 = elevationLayer.getElevation(geoLat, geoLonOld);
					final double elev11 = elevationLayer.getElevation(geoLat, geoLon);

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

							rgb = srtmProfile.getRGB((int) elev);

							rgbData[drawY][drawX] = rgb;
						}
					}
				}
			}
		}

		return rgbData;
	}

	@Override
	public String getId() {
		return FACTORY_ID;
	}

	@Override
	public String getName() {
		return FACTORY_NAME;
	}

	@Override
	public String getOfflineFolder() {
		return OFFLINE_FOLDER;
	}

	@Override
	public IPath getTileOSPath(final String fullPath, final Tile tile) {

		// get profile specific tile path
		String tilePath = ((SRTMProfile) tile.getData()).getTilePath();
		if (tilePath == null) {
			tilePath = UI.EMPTY_STRING;
		}

		return new Path(fullPath)//
				.append(OFFLINE_FOLDER)
				.append(tilePath)
				.append(Integer.toString(tile.getZoom()))
				.append(Integer.toString(tile.getX()))
				.append(Integer.toString(tile.getY()))
				.addFileExtension(MapProviderManager.FILE_EXTENSION_PNG);
	}

	@Override
	public ITilePainter getTilePainter() {
		return this;
	}
}
