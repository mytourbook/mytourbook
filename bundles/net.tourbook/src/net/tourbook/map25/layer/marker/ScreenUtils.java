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

		private Paint	_paintFill		= CanvasAdapter.newPaint();
		private Paint	_paintOutline	= CanvasAdapter.newPaint();
		private Paint	_paintText		= CanvasAdapter.newPaint();

		private int		_symbolSize;
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
		 * @param symbolSizeWeight
		 */
		public ClusterDrawable(	final int sizedp,
								final int foregroundColor,
								final int backgroundColor,
								final String text,
								final int symbolSizeWeight) {

			mText = text;

			setup(sizedp, foregroundColor, backgroundColor, symbolSizeWeight);
		}

		private void draw(final Canvas canvas) {

			final int halfsize = _symbolSize >> 1;
			final int noneClippingRadius = halfsize - getPixels(2);

			// fill
			canvas.drawCircle(halfsize, halfsize, noneClippingRadius, _paintFill);

			// outline
			canvas.drawCircle(halfsize, halfsize, noneClippingRadius, _paintOutline);

			// draw the number at the center
			canvas.drawText(
					mText,
					(canvas.getWidth() - _paintText.getTextWidth(mText)) * 0.5f - 0.5f,
					(canvas.getHeight() + _paintText.getTextHeight(mText)) * 0.5f,
					_paintText);
		}

		public Bitmap getBitmap() {

			int width = _symbolSize, height = _symbolSize;

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
		 * @param symbolSizeWeight
		 */
		private void setup(	final int symbolSizeDP,
							final int foregroundColor,
							final int backgroundColor,
							final int symbolSizeWeight) {

			final int defaultSymbolSize = getPixels(symbolSizeDP);

			final int numDigits = mText.length();

			float textSize;
			final double symbolSizeFactor = numDigits * symbolSizeWeight;

			switch (numDigits) {

			case 2:
				_symbolSize = (int) (defaultSymbolSize + symbolSizeFactor);
				textSize = (int) (_symbolSize / (numDigits * 0.8));

				break;

			case 3:
				_symbolSize = (int) (defaultSymbolSize + symbolSizeFactor);
				textSize = (int) (_symbolSize / (numDigits * 0.7));
				break;

			case 4:
				_symbolSize = (int) (defaultSymbolSize + symbolSizeFactor);
				textSize = (int) (_symbolSize / (numDigits * 0.66));
				break;

			case 5:
				_symbolSize = (int) (defaultSymbolSize + symbolSizeFactor);
				textSize = (int) (_symbolSize / (numDigits * 0.6));
				break;

			default:
				_symbolSize = (int) (defaultSymbolSize + symbolSizeFactor);
				textSize = (int) (_symbolSize / (numDigits * 1.5));
				break;
			}

			final float outlineWidth = 1.0f;//Math.min(1.0f, _symbolSize * 0.2f);

			_paintText.setTextSize(getPixels(textSize));
			_paintText.setColor(foregroundColor);

			// using a different font that the + is centered, with default it is not which is very ugly!
			_paintText.setTypeface(FontFamily.MONOSPACE, FontStyle.BOLD);

			_paintFill.setColor(backgroundColor);
			_paintFill.setStyle(Paint.Style.FILL);

			_paintOutline.setColor(foregroundColor);
			_paintOutline.setStyle(Paint.Style.STROKE);
			_paintOutline.setStrokeWidth(getPixels(outlineWidth));//getPixels(2.0f));
		}
	}

	/**
	 * Get pixels from DPs
	 *
	 * @param dp
	 *            Value in DPs "density-independent pixels"
	 *            {@link https://developer.android.com/guide/practices/screens_support.html}
	 * @return Value in PX according to screen density
	 */
	public static int getPixels(final float dp) {

		return (int) (CanvasAdapter.dpi / CanvasAdapter.DEFAULT_DPI * dp);
	}
}
