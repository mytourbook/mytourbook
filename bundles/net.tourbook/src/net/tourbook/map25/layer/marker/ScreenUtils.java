/*
 * Copyright 2017 nebular
 * Copyright 2017 devemux86
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
package net.tourbook.map25.layer.marker;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;

/**
 * A simple utility class to make clustered markers functionality self-contained. Includes a method
 * to translate between DPs and PXs and a circular icon generator.
 */
public class ScreenUtils {

	public static class ClusterDrawable {

		private Paint	mPaintText		= CanvasAdapter.newPaint();
		private Paint	mPaintCircle	= CanvasAdapter.newPaint();
		private Paint	mPaintBorder	= CanvasAdapter.newPaint();

		private int		mSize;
		private String	mText;

		/**
		 * Generates a circle with a number inside
		 *
		 * @param sizedp
		 *            Size in DPs
		 * @param foregroundColor
		 *            Foreground
		 * @param backgroundColor
		 *            Background
		 * @param text
		 *            Text inside. Will only work for a single character!
		 */
		public ClusterDrawable(	final int sizedp,
								final int foregroundColor,
								final int backgroundColor,
								final String text) {

			setup(sizedp, foregroundColor, backgroundColor);
			setText(text);
		}

		private void draw(final Canvas canvas) {

			final int halfsize = mSize >> 1;

			// fill
			canvas.drawCircle(halfsize, halfsize, halfsize - 2, mPaintCircle);

			// outline
			canvas.drawCircle(halfsize, halfsize, halfsize - 2, mPaintBorder);

			// draw the number at the center
			canvas.drawText(
					mText,
					(canvas.getWidth() - mPaintText.getTextWidth(mText)) * 0.5f,
					(canvas.getHeight() + mPaintText.getTextHeight(mText)) * 0.5f,
					mPaintText);
		}

		public Bitmap getBitmap() {

			int width = mSize, height = mSize;

			width = width > 0 ? width : 1;
			height = height > 0 ? height : 1;

			final Bitmap bitmap = CanvasAdapter.newBitmap(width, height, 0);
			final Canvas canvas = CanvasAdapter.newCanvas();
			canvas.setBitmap(bitmap);

			draw(canvas);

			return bitmap;
		}

		private void setText(final String text) {
			mText = text;
		}

		private void setup(final int sizedp, final int foregroundColor, final int backgroundColor) {

			mSize = getPixels(sizedp);

			mPaintText.setTextSize(getPixels((int) (sizedp * 0.6666666)));
			mPaintText.setColor(foregroundColor);

			mPaintCircle.setColor(backgroundColor);
			mPaintCircle.setStyle(Paint.Style.FILL);

			mPaintBorder.setColor(foregroundColor);
			mPaintBorder.setStyle(Paint.Style.STROKE);
			mPaintBorder.setStrokeWidth(2.0f * CanvasAdapter.dpi / CanvasAdapter.DEFAULT_DPI);
		}
	}

	/**
	 * Get pixels from DPs
	 *
	 * @param dp
	 *            Value in DPs
	 * @return Value in PX according to screen density
	 */
	public static int getPixels(final float dp) {

		return (int) (CanvasAdapter.dpi / CanvasAdapter.DEFAULT_DPI * dp);
	}
}
