/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
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
package net.tourbook.map25.layer.labeling;

import org.oscim.core.Tile;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.geom.GeometryUtils;
import org.oscim.utils.geom.LineClipper;

/**
 * Original: {@link org.oscim.layers.tile.vector.labeling.WayDecorator}
 */
public final class WayDecorator {

	private WayDecorator() {
		throw new IllegalStateException();
	}

	public static void renderText(	final LineClipper clipper,
									final float[] coordinates,
									final String string,
									final TextStyle textStyle,
									final int pos,
									final int len,
									final LabelTileData labelTileData) {

		//TextItem items = textItems;
		TextItem textItem = null;

		// calculate the way name length plus some margin of safety
		float labelWidth = -1;
		final float minWidth = Tile.SIZE / 10;

		//final int min = 0;
		//final int max = Tile.SIZE;

		// find way segments long enough to draw the way name on them
		for (int i = pos; i < pos + len - 2; i += 2) {

			// get the first way point coordinates
			final float prevX = coordinates[i + 0];
			final float prevY = coordinates[i + 1];

			final byte edge = 0;

			// get the current way point coordinates
			float curX = coordinates[i + 2];
			float curY = coordinates[i + 3];

			int last = i;

			// calculate the length of the current segment (Euclidian distance)
			float vx = prevX - curX;
			float vy = prevY - curY;
			if (vx == 0 && vy == 0) {
				continue;
			}

			float a = (float) Math.sqrt(vx * vx + vy * vy);

			// only if not cur segment crosses edge
			if (edge < (1 << 4)) {

				vx /= a;
				vy /= a;

				// add additional segments if possible
				for (int j = i + 4; j < pos + len; j += 2) {
					final float nextX = coordinates[j + 0];
					final float nextY = coordinates[j + 1];

					float wx = nextX - curX;
					float wy = nextY - curY;
					if (wx == 0 && wy == 0) {
						continue;
					}

					final float area = GeometryUtils.area(prevX, prevY, curX, curY, nextX, nextY);

					if (area > 1000) {
						//log.debug("b: " + string + " " + area );
						break;
					}

					a = (float) Math.sqrt(wx * wx + wy * wy);
					wx /= a;
					wy /= a;

					// avoid adding short segments that add much area
					if (area / 2 > a * a) {
						//log.debug("a: " +string + " " + area + " " + a*a);
						break;
					}

					final float ux = vx + wx;
					final float uy = vy + wy;
					final float diff = wx * uy - wy * ux;

					// maximum angle between segments
					if (diff > 0.1 || diff < -0.1) {
						//log.debug("c: " + string + " " + area );
						break;
					}
					curX = nextX;
					curY = nextY;
					last = j - 2;
				}

				vx = curX - prevX;
				vy = curY - prevY;
				a = (float) Math.sqrt(vx * vx + vy * vy);
			}

			final float segmentLength = a;

			if (segmentLength < minWidth) {
				continue;
			}

			if (labelWidth < 0) {
				labelWidth = textStyle.paint.measureText(string);
			}

			if (segmentLength < labelWidth * 0.50) {
				continue;
			}

			float x1, y1, x2, y2;

			if (prevX < curX) {
				x1 = prevX;
				y1 = prevY;
				x2 = curX;
				y2 = curY;
			} else {
				x1 = curX;
				y1 = curY;
				x2 = prevX;
				y2 = prevY;
			}

			final TextItem n = TextItem.pool.get();

			// link items together
			//if (t != null) {
			//    t.n1 = n;
			//    n.n2 = t;
			//}

			textItem = n;
			textItem.x = x1 + (x2 - x1) / 2f;
			textItem.y = y1 + (y2 - y1) / 2f;
			textItem.string = string;
			textItem.text = textStyle;
			textItem.width = labelWidth;
			textItem.x1 = x1;
			textItem.y1 = y1;
			textItem.x2 = x2;
			textItem.y2 = y2;
			textItem.length = (short) segmentLength;

			textItem.edges = edge;
			labelTileData.labels.push(textItem);

			i = last;
		}
	}
}
