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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Sash;

public class UI {

	private static final char[]	INVALID_FILENAME_CHARS	= new char[] { '\\', '/', ':', '*', '?', '"', '<', '>', '|', };

	public static final String	EMPTY_STRING			= "";																		//$NON-NLS-1$
	public static final String	SPACE					= " ";																		//$NON-NLS-1$
	public static final String	STRING_0				= "0";																		//$NON-NLS-1$

	public static final boolean	IS_OSX					= "carbon".equals(SWT.getPlatform()) || "cocoa".equals(SWT.getPlatform());	//$NON-NLS-1$

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

			public void mouseEnter(final MouseEvent e) {
				sash.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
			}

			public void mouseExit(final MouseEvent e) {
				sash.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}

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

	public static VerifyListener verifyFilenameInput() {
		return new VerifyListener() {
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
