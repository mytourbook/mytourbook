/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.common.ui;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/*
 * This is partly copied from Snippet133
 */
public class TextWrapPainter {

	private StringBuilder	_wordbuffer	= new StringBuilder();

	private int				_lineHeight;
	private int				_tabWidth;

	private int				_devLeftMargin;
	private int				_devRightMargin;
	private int				_devX;
	private int				_devY;

	private String			_tabText;

	private boolean			_is1stPainted;

	{
		/*
		 * Create a buffer for computing tab width.
		 */
		final int tabSize = 4;
		final StringBuilder tabBuffer = new StringBuilder(tabSize);
		for (int i = 0; i < tabSize; i++) {
			tabBuffer.append(' ');
		}

		_tabText = tabBuffer.toString();
	}

	/**
	 * @param gc
	 * @param textToPrint
	 *            Text which is printed
	 * @param devX
	 *            Left margin
	 * @param devY
	 *            Top margin
	 * @param viewportWidth
	 *            Viewport width
	 * @param viewportHeight
	 *            Viewport height
	 * @param fontHeight
	 * @param noOverlapRect
	 */
	public void drawText(	final GC gc,
							final String textToPrint,
							final int devX,
							final int devY,
							final int viewportWidth,
							final int viewportHeight,
							final int fontHeight,
							final Rectangle noOverlapRect) {

		_tabWidth = gc.stringExtent(_tabText).x;
		_lineHeight = fontHeight;

		_devX = _devLeftMargin = devX;
		_devY = devY;

		_is1stPainted = false;

		_devRightMargin = devX + viewportWidth;
		final int bottom = devY + viewportHeight;

		// truncate buffer
		_wordbuffer.setLength(0);

		int index = 0;
		final int end = textToPrint.length();

		while (index < end) {

			final char c = textToPrint.charAt(index);
			index++;

			if (c != 0) {

				if (c == 0x0a || c == 0x0d) {

					if (c == 0x0d && index < end && textToPrint.charAt(index) == 0x0a) {
						index++; // if this is cr-lf, skip the lf
					}

					printWordBuffer(gc, noOverlapRect);
					newline();

					if (_devY > bottom) {
						break;
					}

				} else {

					if (c != '\t') {
						_wordbuffer.append(c);
					}

					if (Character.isWhitespace(c) || c == '/' || c == ',' || c == '&' || c == '-') {

						printWordBuffer(gc, noOverlapRect);

						if (c == '\t') {
							_devX += _tabWidth;
						}
					}
				}
			}
		}

		// print final buffer
		printWordBuffer(gc, noOverlapRect);
	}

	private void newline() {

		_devX = _devLeftMargin;
		_devY += _lineHeight;
	}

	private void printWordBuffer(final GC gc, final Rectangle noOverlapRect) {

		if (_wordbuffer.length() > 0) {

			final String word = _wordbuffer.toString();
			final Point wordExtent = gc.stringExtent(word);
			final int devWordWidth = wordExtent.x;
			final int devWordHeight = wordExtent.y;

			if (noOverlapRect != null) {

				final int max = 5;
				int current = 0;

				while (current++ < max) {

					final Rectangle wordRect = new Rectangle(_devX, _devY, devWordWidth, devWordHeight);

					if (wordRect.intersects(noOverlapRect)) {
						newline();
					}
				}

			}

			if (_devX + devWordWidth > _devRightMargin) {

				// do not draw a newline on the 1st line
				if (_is1stPainted) {

					// word doesn't fit on current line, so wrap
					newline();
				}
			}

			gc.drawString(word, _devX, _devY, true);

			_is1stPainted = true;

//			System.out.println("x:" + _devX + "\ty:" + _devY + "\t" + word);
//// TODO remove SYSTEM.OUT.PRINTLN

			_devX += devWordWidth;

			// truncate buffer
			_wordbuffer.setLength(0);
		}
	}
}
