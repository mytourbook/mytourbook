/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.ui;

import org.eclipse.swt.graphics.GC;

/*
 * this is partly copied from Snippet133 
 */
public class TextWrapPainter {

	private StringBuilder	sb	= new StringBuilder();

	private int				devLineHeight;
	private int				devTabWidth;

	private int				devLeftMargin;
	private int				devRightMargin;
	private int				devX;
	private int				devY;

	private String			tabs;
	{
		/*
		 * Create a buffer for computing tab width.
		 */
		final int tabSize = 4;
		final StringBuilder tabBuffer = new StringBuilder(tabSize);
		for (int i = 0; i < tabSize; i++) {
			tabBuffer.append(' ');
		}

		tabs = tabBuffer.toString();
	}

	private void newline() {
		devX = devLeftMargin;
		devY += devLineHeight;
	}

	/**
	 * @param gc
	 * @param text
	 *            Text which is printed
	 * @param devTextX
	 * @param devTextY
	 * @param devWidth
	 */
	public void printText(	final GC gc,
							final String textToPrint,
							final int devTextX,
							final int devTextY,
							final int devWidth) {

		devTabWidth = gc.stringExtent(tabs).x;
		devLineHeight = gc.getFontMetrics().getHeight();

		devX = devLeftMargin = devTextX;
		devY = devTextY;

		devRightMargin = devTextX + devWidth;

		// truncate buffer
		sb.setLength(0);

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

					printWordBuffer(gc);
					newline();

				} else {

					if (c != '\t') {
						sb.append(c);
					}

					if (Character.isWhitespace(c) || c == '/' || c == ',' || c == '&' || c == '-') {

						printWordBuffer(gc);

						if (c == '\t') {
							devX += devTabWidth;
						}
					}
				}
			}
		}

		// print final buffer
		printWordBuffer(gc);
	}

	private void printWordBuffer(final GC gc) {

		if (sb.length() > 0) {

			final String word = sb.toString();
			final int devWordWidth = gc.stringExtent(word).x;

			if (devX + devWordWidth > devRightMargin) {
				// word doesn't fit on current line, so wrap 
				newline();
			}

			if (devX != devLeftMargin) {
				// add additional space to display the / correctly
				devX++;
				devX++;
			}

			gc.drawString(word, devX, devY, false);

//			System.out.println("x:" + devX + "\ty:" + devY + "\t" + word);
// TODO remove SYSTEM.OUT.PRINTLN

			devX += devWordWidth;

			// truncate buffer
			sb.setLength(0);
		}
	}
}
