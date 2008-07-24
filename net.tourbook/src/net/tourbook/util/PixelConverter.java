/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.util;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;

/**
 * Pixel conversion utility.
 * 
 * @since 3.0
 */
public class PixelConverter {

	private FontMetrics	fFontMetrics;

	public PixelConverter(final Control control) {
		final GC gc = new GC(control);
		gc.setFont(control.getFont());
		fFontMetrics = gc.getFontMetrics();
		gc.dispose();
	}

	public int convertWidthInCharsToPixels(final int chars) {
		return Dialog.convertWidthInCharsToPixels(fFontMetrics, chars);
	}

	public int convertHeightInCharsToPixels(final int chars) {
		return Dialog.convertHeightInCharsToPixels(fFontMetrics, chars);
	}

	public int convertHorizontalDLUsToPixels(final int dlus) {
		return Dialog.convertHorizontalDLUsToPixels(fFontMetrics, dlus);
	}

	public int convertVerticalDLUsToPixels(final int dlus) {
		return Dialog.convertVerticalDLUsToPixels(fFontMetrics, dlus);
	}
}
