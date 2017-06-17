/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.tourbook.map.vtm;

import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.layers.tile.TileLoader;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.VectorTileLoader;
import org.oscim.map.Map;
import org.oscim.utils.Utils;

public class OsmTileLayerMT extends VectorTileLayer {

	private static final int	MAX_ZOOMLEVEL	= 17;
	private static final int	MIN_ZOOMLEVEL	= 1;

	// limit is increased that zooming the map will not always reload the tiles
	private static final int	CACHE_LIMIT		= 1000;

	private static class OsmTileLoader extends VectorTileLoader {

		/*
		 * Replace tags that should only be matched by key in RenderTheme to avoid caching
		 * RenderInstructions for each way of the same type only with different name. Maybe this
		 * should be done within RenderTheme, also allowing to set these replacement rules in theme
		 * file.
		 */
		private static final TagReplacement[]	mTagReplacement	= {
				new TagReplacement(Tag.KEY_NAME),
				new TagReplacement(Tag.KEY_HOUSE_NUMBER),
				new TagReplacement(Tag.KEY_REF),
				new TagReplacement(Tag.KEY_HEIGHT),
				new TagReplacement(Tag.KEY_MIN_HEIGHT)
		};

		private final TagSet					mFilteredTags;

		OsmTileLoader(final VectorTileLayer tileLayer) {

			super(tileLayer);
			mFilteredTags = new TagSet();
		}

		@Override
		protected TagSet filterTags(final TagSet tagSet) {

			final Tag[] tags = tagSet.tags;

			mFilteredTags.clear();

			O: for (int i = 0, n = tagSet.numTags; i < n; i++) {
				final Tag t = tags[i];

				for (final TagReplacement replacement : mTagReplacement) {
					if (Utils.equals(t.key, replacement.key)) {

						mFilteredTags.add(replacement.tag);
						continue O;
					}
				}

				mFilteredTags.add(t);
			}

			return mFilteredTags;
		}
	}

	static class TagReplacement {

		String	key;
		Tag		tag;

		public TagReplacement(final String key) {
			this.key = key;
			this.tag = new Tag(key, null);
		}
	}

	public OsmTileLayerMT(final Map map) {
		this(map, MIN_ZOOMLEVEL, MAX_ZOOMLEVEL);
	}

	public OsmTileLayerMT(final Map map, final int zoomMin, final int zoomMax) {

		super(map, CACHE_LIMIT);
//		mTileManager.setZoomLevel(zoomMin, zoomMax);
	}

	@Override
	protected TileLoader createLoader() {
		return new OsmTileLoader(this);
	}
}
