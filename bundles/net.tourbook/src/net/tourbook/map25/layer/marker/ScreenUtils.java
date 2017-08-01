/*
 * Original: org.oscim.layers.marker.utils.ScreenUtils
 */
package net.tourbook.map25.layer.marker;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;
import org.oscim.backend.canvas.Paint.FontFamily;
import org.oscim.backend.canvas.Paint.FontStyle;

/**
 * A simple utility class to make clustered markers functionality self-contained. Includes a method
 * to translate between DPs and PXs and a circular icon generator.
 */
public class ScreenUtils {

//	private static final Font					DEFAULT_FONT;
//	private static final Map<Attribute, Object>	TEXT_ATTRIBUTES	= new HashMap<>();

	static {

//		TEXT_ATTRIBUTES.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
//
//		DEFAULT_FONT = new Font("Arial", Font.PLAIN, 14).deriveFont(TEXT_ATTRIBUTES);
	}

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

			mText = text;

			setup(sizedp, foregroundColor, backgroundColor);
		}

		private void draw(final Canvas canvas) {

			final int halfsize = mSize >> 1;
			final int noneClippingRadius = halfsize - getPixels(2);

			// fill
			canvas.drawCircle(halfsize, halfsize, noneClippingRadius, mPaintCircle);

			// outline
			canvas.drawCircle(halfsize, halfsize, noneClippingRadius, mPaintBorder);

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

		/**
		 * @param symbolSizeDP
		 * @param foregroundColor
		 * @param backgroundColor
		 */
		private void setup(final int symbolSizeDP, final int foregroundColor, final int backgroundColor) {

			mSize = getPixels(symbolSizeDP);

			final int numDigits = mText.length();
			final int textSizeCluster = (int) (symbolSizeDP * 0.7f);

			final int textSize = numDigits > 2 //
					? (int) (textSizeCluster / (numDigits * 0.5))
					: textSizeCluster;

			mPaintText.setTextSize(getPixels(textSize));
			mPaintText.setColor(foregroundColor);

			// using a different font that the + is centered, with default it is not which is very ugly!
			mPaintText.setTypeface(FontFamily.MONOSPACE, FontStyle.BOLD);

			mPaintCircle.setColor(backgroundColor);
			mPaintCircle.setStyle(Paint.Style.FILL);

			mPaintBorder.setColor(foregroundColor);
			mPaintBorder.setStyle(Paint.Style.STROKE);
			mPaintBorder.setStrokeWidth(getPixels(2.0f));
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
