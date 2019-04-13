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

		private Paint	_fillPainter	= CanvasAdapter.newPaint();
		private Paint	_outlinePainter	= CanvasAdapter.newPaint();
		private Paint	_textPainter	= CanvasAdapter.newPaint();

		private int		_symbolSize;
		private String	_text;
		private float	_outlineWidth;
		private Bitmap _bitmapPoi = null;

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
		 * @param clusterOutlineSize
		 */
		public ClusterDrawable(	final int sizedp,
								final int foregroundColor,
								final int backgroundColor,
								final String text,
								final int symbolSizeWeight,
								final float clusterOutlineSize) {

			_text = text;
			_outlineWidth = clusterOutlineSize;

			setup(sizedp, foregroundColor, backgroundColor, symbolSizeWeight);
		}

		/**
		 * creates a cluster Bitmap.
		 * @param additionalBitmap , null when no additional bitmap wanted
		 * @return
		 */
		public Bitmap getBitmap(Bitmap additionalBitmap) {

			/**
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			 * <p>
			 * Painting with decimals is not working correctly because AWT do not support it, all is
			 * converted to integer. It took me some hours to not find a solution.
			 * <p>
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			 */
			final int outlineWidthInt = (int) Math.ceil(_outlineWidth);
			final int symbolSizeInt = (int) Math.ceil(_symbolSize);

			final float symbolRadius = symbolSizeInt / 2;

			final int bitmapSizeInt = symbolSizeInt + 2 * outlineWidthInt + 6;
			final int bitmapSize2Int = bitmapSizeInt / 2;

			final float outlineRadius = symbolRadius + _outlineWidth / 2;
			final float fillRadius = outlineRadius - _outlineWidth / 2;

			final int noClippingPos = bitmapSize2Int + 1;

			final Bitmap bitmap = CanvasAdapter.newBitmap(bitmapSizeInt, bitmapSizeInt, 0);
			final Canvas canvas = CanvasAdapter.newCanvas();
			canvas.setBitmap(bitmap);
			
		//	{  //(testing block
			   /*
			    * the following three or four lines displaying a transparent box.
			    * only for testing purposes, normally uncommented
			    */
		//	   int oldColor = _fillPainter.getColor();
		//	   _fillPainter.setColor(0x60ffffff);
		//	   canvas.drawCircle(0, 0, bitmapSizeInt*2, _fillPainter);
		//	   _fillPainter.setColor(oldColor);
		//	}

			// fill symbol
			canvas.drawCircle(noClippingPos, noClippingPos, fillRadius, _fillPainter);

			// draw outline
			if (_outlineWidth > 0) {
				canvas.drawCircle(noClippingPos, noClippingPos, outlineRadius, _outlinePainter);
			}
			
			// draw additional symbol
			if (additionalBitmap != null) {
			   if (additionalBitmap.getWidth() <= bitmapSizeInt) {
			      canvas.drawBitmap(additionalBitmap, (bitmapSizeInt / 2 - additionalBitmap.getWidth() / 2) + 1, (bitmapSizeInt / 2 - additionalBitmap.getHeight() / 2) + 1);
			   } else {
			      canvas.drawBitmapScaled(additionalBitmap);
			   }
			}

			// draw the number at the center
			canvas.drawText(
					_text,
					(canvas.getWidth() - _textPainter.getTextWidth(_text)) * 0.5f - 0.5f,
					(canvas.getHeight() + _textPainter.getTextHeight(_text)) * 0.5f,
					_textPainter);

			return bitmap;
		}

		public Bitmap  getBitmap() {
		   return getBitmap(null);
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

			final float defaultSymbolSize = getPixels(symbolSizeDP);

			final int numDigits = _text.length();

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

			_textPainter.setTextSize(getPixels(textSize));
			_textPainter.setColor(foregroundColor);

			// using a different font that the + is centered, with default it is not which is very ugly!
			_textPainter.setTypeface(FontFamily.MONOSPACE, FontStyle.BOLD);

			_fillPainter.setColor(backgroundColor);
			_fillPainter.setStyle(Paint.Style.FILL);

			_outlinePainter.setColor(foregroundColor);
			_outlinePainter.setStyle(Paint.Style.STROKE);
			_outlinePainter.setStrokeWidth(getPixels(_outlineWidth));
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
	public static float getPixels(final float dp) {

		return CanvasAdapter.dpi / CanvasAdapter.DEFAULT_DPI * dp;
	}
}
