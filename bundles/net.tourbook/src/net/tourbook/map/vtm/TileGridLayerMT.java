/*
 * Copyright 2012 Hannes Janetzek
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

import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.layers.GenericLayer;
import org.oscim.map.Map;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.TextStyle;

public class TileGridLayerMT extends GenericLayer {

	public TileGridLayerMT(final Map map) {
		super(map, new GridRendererMT());
	}

	public TileGridLayerMT(final Map map, final float scale) {
		super(map, new GridRendererMT(scale));
	}

	public TileGridLayerMT(final Map map, final int color, final float width, final int repeat) {
		super(map, new GridRendererMT(repeat, new LineStyle(color, width, Cap.BUTT), null));
	}

	public TileGridLayerMT(final Map map, final int color, final float width, final TextStyle text, final int repeat) {
		super(map, new GridRendererMT(repeat, new LineStyle(color, width, Cap.BUTT), text));
	}
}
