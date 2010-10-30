/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;

public class UI {

	private static final char[]	INVALID_FILENAME_CHARS	= new char[] { '\\', '/', ':', '*', '?', '"', '<', '>', '|', };

	public static final String	EMPTY_STRING			= "";																		//$NON-NLS-1$
	public static final String	SPACE					= " ";																		//$NON-NLS-1$
	public static final String	DOT						= ".";																		//$NON-NLS-1$
	public static final String	STRING_0				= "0";																		//$NON-NLS-1$

	/**
	 * The ellipsis is the string that is used to represent shortened text.
	 * 
	 * @since 3.0
	 */
	public static final String	ELLIPSIS				= "...";																	//$NON-NLS-1$

	public static final boolean	IS_OSX					= "carbon".equals(SWT.getPlatform()) || "cocoa".equals(SWT.getPlatform());	//$NON-NLS-1$ //$NON-NLS-2$
	public static final boolean	IS_LINUX				= "gtk".equals(SWT.getPlatform());											//$NON-NLS-1$

	/**
	 * contains a new line
	 */
	public static final String	NEW_LINE				= "\n";																	//$NON-NLS-1$

	/**
	 * contains 2 new lines
	 */
	public static final String	NEW_LINE2				= "\n\n";																	//$NON-NLS-1$

	public static final String	UTF_8					= "UTF-8";																	//$NON-NLS-1$

	public static void addSashColorHandler(final Sash sash) {

		sash.addMouseTrackListener(new MouseTrackListener() {

			@Override
			public void mouseEnter(final MouseEvent e) {
				sash.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
			}

			@Override
			public void mouseExit(final MouseEvent e) {
				sash.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}

			@Override
			public void mouseHover(final MouseEvent e) {}
		});

		sash.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// hide background when sash is dragged

				if (e.detail == SWT.DRAG) {
					sash.setBackground(null);
				} else {
					sash.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
				}
			}
		});
	}

	/**
	 * Opens the control context menu, the menue is aligned below the control to the right side
	 * 
	 * @param control
	 *            Controls which menu is opened
	 */
	public static void openControlMenu(final Control control) {

		final Rectangle rect = control.getBounds();
		Point pt = new Point(rect.x, rect.y + rect.height);
		pt = control.getParent().toDisplay(pt);

		final Menu contextMenu = control.getMenu();

		if (contextMenu != null && contextMenu.isDisposed() == false) {
			contextMenu.setLocation(pt.x, pt.y);
			contextMenu.setVisible(true);
		}
	}

	/**
	 * copy from {@link CTabItem}
	 * 
	 * @param gc
	 * @param text
	 * @param width
	 * @param isUseEllipses
	 * @return
	 */
	public static String shortenText(final GC gc, final String text, final int width, final boolean isUseEllipses) {
		return isUseEllipses ? //
				shortenText(gc, text, width, ELLIPSIS)
				: shortenText(gc, text, width, UI.EMPTY_STRING);
	}

	public static String shortenText(final GC gc, String text, final int width, final String ellipses) {

		if (gc.textExtent(text, 0).x <= width) {
			return text;
		}

		final int ellipseWidth = gc.textExtent(ellipses, 0).x;
		final int length = text.length();
		final TextLayout layout = new TextLayout(gc.getDevice());
		layout.setText(text);

		int end = layout.getPreviousOffset(length, SWT.MOVEMENT_CLUSTER);
		while (end > 0) {
			text = text.substring(0, end);
			final int l = gc.textExtent(text, 0).x;
			if (l + ellipseWidth <= width) {
				break;
			}
			end = layout.getPreviousOffset(end, SWT.MOVEMENT_CLUSTER);
		}
		layout.dispose();
		return end == 0 ? text.substring(0, 1) : text + ellipses;
	}

	/**
	 * copied from {@link Dialog} <br>
	 * <br>
	 * Shortens the given text <code>textValue</code> so that its width in pixels does not exceed
	 * the width of the given control. Overrides characters in the center of the original string
	 * with an ellipsis ("...") if necessary. If a <code>null</code> value is given,
	 * <code>null</code> is returned.
	 * 
	 * @param textValue
	 *            the original string or <code>null</code>
	 * @param control
	 *            the control the string will be displayed on
	 * @return the string to display, or <code>null</code> if null was passed in
	 * @since 3.0
	 */
	public static String shortenText(final String textValue, final Control control) {
		if (textValue == null) {
			return null;
		}
		final GC gc = new GC(control);
		final int maxWidth = control.getBounds().width - 5;
		final int maxExtent = gc.textExtent(textValue).x;
		if (maxExtent < maxWidth) {
			gc.dispose();
			return textValue;
		}
		final int length = textValue.length();
		final int charsToClip = Math.round(0.95f * length * (1 - ((float) maxWidth / maxExtent)));
		final int pivot = length / 2;
		int start = pivot - (charsToClip / 2);
		int end = pivot + (charsToClip / 2) + 1;
		while (start >= 0 && end < length) {
			final String s1 = textValue.substring(0, start);
			final String s2 = textValue.substring(end, length);
			final String s = s1 + ELLIPSIS + s2;
			final int l = gc.textExtent(s).x;
			if (l < maxWidth) {
				gc.dispose();
				return s;
			}
			start--;
			end++;
		}
		gc.dispose();
		return textValue;
	}

	public static String shortenText(	final String text,
										final Control control,
										final int width,
										final boolean isUseEllipses) {

		String shortText;
		final GC gc = new GC(control);
		{
			shortText = shortenText(gc, text, width, isUseEllipses);
		}
		gc.dispose();

		return shortText;
	}

	public static VerifyListener verifyFilenameInput() {
		return new VerifyListener() {
			@Override
			public void verifyText(final VerifyEvent e) {

				// check invalid chars
				for (final char invalidChar : INVALID_FILENAME_CHARS) {
					if (invalidChar == e.character) {
						e.doit = false;
						return;
					}
				}
			}
		};
	}

	public static void verifyIntegerInput(final Event e, final boolean canBeNegative) {

		// check backspace and del key
		if (e.character == SWT.BS || e.character == SWT.DEL) {
			return;
		}

		// check '-' key
		if (canBeNegative && e.character == '-') {
			return;
		}

		try {
			Integer.parseInt(e.text);
		} catch (final NumberFormatException ex) {
			e.doit = false;
		}
	}

	public static boolean verifyIntegerValue(final String valueString) {

		if (valueString.trim().length() == 0) {
			return false;
		}

		try {
			Integer.parseInt(valueString);
			return true;
		} catch (final NumberFormatException ex) {
			return false;
		}
	}

	public static VerifyListener verifyListenerInteger(final boolean canBeNegative) {

		return new VerifyListener() {
			@Override
			public void verifyText(final VerifyEvent e) {

				// check backspace and del key
				if (e.character == SWT.BS || e.character == SWT.DEL) {
					return;
				}

				// check '-' key
				if (canBeNegative && e.character == '-') {
					return;
				}

				try {
					Integer.parseInt(e.text);
				} catch (final NumberFormatException ex) {
					e.doit = false;
				}
			}
		};
	}

	public static VerifyListener verifyListenerTypeLong() {

		return new VerifyListener() {
			@Override
			public void verifyText(final VerifyEvent e) {
				if (e.text.equals(UI.EMPTY_STRING)) {
					return;
				}
				try {
					Long.parseLong(e.text);
				} catch (final NumberFormatException e1) {
					e.doit = false;
				}
			}
		};
	}

}
