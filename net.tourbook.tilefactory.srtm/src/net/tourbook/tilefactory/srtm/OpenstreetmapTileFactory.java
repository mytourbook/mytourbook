package net.tourbook.tilefactory.srtm;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import de.byteholder.geoclipse.map.DefaultTileFactory;
import de.byteholder.geoclipse.map.TileFactoryInfo;

/**
 * @author Michael Kanis
 */
public class OpenstreetmapTileFactory extends DefaultTileFactory {

	private static OpenStreetMapTileFactoryInfo	info	= new OpenStreetMapTileFactoryInfo();

	private static class OpenStreetMapTileFactoryInfo extends TileFactoryInfo {

		private static final String	FACTORY_ID		= "osm";
		private static final String	FACTORY_NAME	= "OpenStreetMap";
		private static final String	FACTORY_OS_NAME	= "osm";

		private static final String	SEPARATOR		= "/";

		private static final int	MIN_ZOOM		= 0;
		private static final int	MAX_ZOOM		= 17;
		private static final int	TOTAL_ZOOM		= 17;

		private static final String	BASE_URL		= "http://tile.openstreetmap.org";
		private static final String	FILE_EXT		= "png";

		public OpenStreetMapTileFactoryInfo() {

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

	}

	public OpenstreetmapTileFactory() {
		super(info);
	}

}
