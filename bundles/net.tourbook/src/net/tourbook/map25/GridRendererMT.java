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
package net.tourbook.map25;

import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.TextBucket;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.TextStyle;

public class GridRendererMT extends BucketRenderer {

	private final TextBucket		mTextBucket;
	private final TextStyle			_textStyle;
	private final LineBucket		mLineBucket;
	private final GeometryBuffer	mLines;

	private int						mCurX, mCurY, mCurZ;

	public GridRendererMT() {
		this(1);
	}

	public GridRendererMT(final float scale) {

		this(
				1,

				new LineStyle(Color.GRAY, 1.2f * scale, Cap.BUTT),

				TextStyle
						.builder()
						.fontSize(26 * scale)
						.fontStyle(Paint.FontStyle.NORMAL)
						.color(Color.RED)
						.build());
	}

	public GridRendererMT(final int numLines, final LineStyle lineStyle, final TextStyle textStyle) {

		final int size = Tile.SIZE;

		/* not needed to set but we know: 16 lines 'a' two points */
		mLines = new GeometryBuffer(2 * 16, 16);

		final float pos = -size * 4;

		/* 8 vertical lines */
		for (int i = 0; i < 8 * numLines; i++) {
			final float x = pos + i * size / numLines;
			mLines.startLine();
			mLines.addPoint(x, pos);
			mLines.addPoint(x, pos + size * 8);
		}

		/* 8 horizontal lines */
		for (int j = 0; j < 8 * numLines; j++) {
			final float y = pos + j * size / numLines;
			mLines.startLine();
			mLines.addPoint(pos, y);
			mLines.addPoint(pos + size * 8, y);
		}

		_textStyle = textStyle;

		mLineBucket = new LineBucket(0);
		mLineBucket.line = lineStyle;

		if (_textStyle != null) {
			mTextBucket = new TextBucket();
			mTextBucket.next = mLineBucket;
		} else {
			mTextBucket = null;
			mLineBucket.addLine(mLines);
			buckets.set(mLineBucket);
		}
	}

	private void addLabels(final int x, final int y, final int z, final MapPosition mapPosition) {

		final int s = Tile.SIZE;

		final int tileZ = 1 << z;
		final float lineHeight = _textStyle.fontSize + 1;

		final TextBucket textBucket = mTextBucket;
		textBucket.clear();

		for (int yy = -2; yy < 2; yy++) {
			for (int xx = -2; xx < 2; xx++) {

				final int tileX = x + xx;
				final int tileY = y + yy;

				final double latitude = MercatorProjection.toLatitude((double) tileY / tileZ);
				final double longitude = MercatorProjection.toLongitude((double) tileX / tileZ);

				final String labelTile = String.format("%d / %d / %d", z, tileX, tileY); //$NON-NLS-1$

				final String labelLat = String.format("lat %.4f", latitude); //$NON-NLS-1$
				final String labelLon = String.format("lon %.4f", longitude); //$NON-NLS-1$

//				final String labelProjectedX = String.format("x %.18f", mapPosition.x);
//				final String labelProjectedY = String.format("y %.18f", mapPosition.y);

				final int textX = s * xx + s / 2;
				final int textY = s * yy + s / 2;

				TextItem textItem = TextItem.pool.get();
				textItem.set(textX, textY, labelTile, _textStyle);
				textBucket.addText(textItem);

				/*
				 * Lat/Lon
				 */
				textItem = TextItem.pool.get();
				textItem.set(textX, textY + lineHeight, labelLat, _textStyle);
				textBucket.addText(textItem);

				textItem = TextItem.pool.get();
				textItem.set(textX, textY + lineHeight * 2, labelLon, _textStyle);
				textBucket.addText(textItem);

// THIS IS DISABLED BECAUSE IT HAS THE SAME VALUE FOR ALL TILES
//
//				/*
//				 * Projected x/y
//				 */
//				textItem = TextItem.pool.get();
//				textItem.set(textX, textY + lineHeight * 3, labelProjectedX, _textStyle);
//				textBucket.addText(textItem);
//
//				textItem = TextItem.pool.get();
//				textItem.set(textX, textY + lineHeight * 4, labelProjectedY, _textStyle);
//				textBucket.addText(textItem);
			}
		}
	}

	@Override
	public void update(final GLViewport viewport) {

		final MapPosition mapPosition = viewport.pos;

		/*
		 * scale coordinates relative to current 'zoom-level' to get the position as the nearest
		 * tile coordinate
		 */
		final int z = 1 << mapPosition.zoomLevel;
		final int x = (int) (mapPosition.x * z);
		final int y = (int) (mapPosition.y * z);

		/* update buckets when map moved by at least one tile */
		if (x == mCurX && y == mCurY && z == mCurZ) {
			return;
		}

		mCurX = x;
		mCurY = y;
		mCurZ = z;

		mMapPosition.copy(mapPosition);
		mMapPosition.x = (double) x / z;
		mMapPosition.y = (double) y / z;
		mMapPosition.scale = z;

		if (_textStyle != null) {

			buckets.set(mTextBucket);

			addLabels(x, y, mapPosition.zoomLevel, mapPosition);

			mLineBucket.addLine(mLines);
			buckets.prepare();
			setReady(false);
		}

		if (!isReady()) {
			compile();
		}
	}
}
