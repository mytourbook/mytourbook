/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.common;

import java.nio.charset.Charset;

import net.tourbook.common.weather.IWeather;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;
import org.epics.css.dal.Timestamp;
import org.epics.css.dal.Timestamp.Format;

public class UI {

	private static final char[]			INVALID_FILENAME_CHARS			= new char[] {
			'\\',
			'/',
			':',
			'*',
			'?',
			'"',
			'<',
			'>',
			'|',														};

	public static final String			EMPTY_STRING					= "";																		//$NON-NLS-1$
	public static final String			SPACE							= " ";																		//$NON-NLS-1$
	public static final String			SPACE2							= "  ";																	//$NON-NLS-1$
	public static final String			SPACE4							= "    ";																	//$NON-NLS-1$
	public static final String			STRING_0						= "0";																		//$NON-NLS-1$

	public static final String			SYMBOL_AVERAGE					= "\u00f8";																//$NON-NLS-1$
	public static final String			SYMBOL_AVERAGE_WITH_SPACE		= "\u00f8 ";																//$NON-NLS-1$
	public static final String			SYMBOL_DASH						= "\u2212";																//$NON-NLS-1$
	public static final String			SYMBOL_DIFFERENCE				= "\u0394";																//$NON-NLS-1$
	public static final String			SYMBOL_DIFFERENCE_WITH_SPACE	= "\u0394 ";																//$NON-NLS-1$
	public static final String			SYMBOL_DOUBLE_HORIZONTAL		= "\u2550";																//$NON-NLS-1$
	public static final String			SYMBOL_DOUBLE_VERTICAL			= "\u2551";																//$NON-NLS-1$
	public static final String			SYMBOL_DEGREE					= "\u00B0";																//$NON-NLS-1$
	public static final String			SYMBOL_ELLIPSIS					= "\u2026";																//$NON-NLS-1$
	public static final String			SYMBOL_INFINITY					= "\u221E";																//$NON-NLS-1$
	public static final String			SYMBOL_SUM_WITH_SPACE			= "\u2211 ";																//$NON-NLS-1$
	public static final String			SYMBOL_TAU						= "\u03c4";																//$NON-NLS-1$

	public static final String			SYMBOL_DOT						= ".";																		//$NON-NLS-1$
	public static final String			SYMBOL_COLON					= ":";																		//$NON-NLS-1$
	public static final String			SYMBOL_EQUAL					= "=";																		//$NON-NLS-1$
	public static final String			SYMBOL_GREATER_THAN				= ">";																		//$NON-NLS-1$
	public static final String			SYMBOL_LESS_THAN				= "<";																		//$NON-NLS-1$
	public static final String			SYMBOL_PERCENTAGE				= "%";																		//$NON-NLS-1$
	public static final String			SYMBOL_WIND_WITH_SPACE			= "W ";																	//$NON-NLS-1$
	public static final String			SYMBOL_EXCLAMATION_POINT		= "!";																		//$NON-NLS-1$

	/*
	 * labels for the different measurement systems
	 */
	public static final String			UNIT_MBYTES						= "MByte";																	//$NON-NLS-1$

	/**
	 * The ellipsis is the string that is used to represent shortened text.
	 * 
	 * @since 3.0
	 */
	public static final String			ELLIPSIS						= "...";																	//$NON-NLS-1$

	public static final boolean			IS_LINUX						= "gtk".equals(SWT.getPlatform());											//$NON-NLS-1$
	public static final boolean			IS_OSX							= "carbon".equals(SWT.getPlatform()) || "cocoa".equals(SWT.getPlatform());	//$NON-NLS-1$ //$NON-NLS-2$
	public static final boolean			IS_WIN							= "win32".equals(SWT.getPlatform()) || "wpf".equals(SWT.getPlatform());	//$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * contains a new line
	 */
	public static final String			NEW_LINE						= "\n";																	//$NON-NLS-1$

	/**
	 * contains 2 new lines
	 */
	public static final String			NEW_LINE2						= "\n\n";																	//$NON-NLS-1$

	public static final String			UTF_8							= "UTF-8";																	//$NON-NLS-1$
	public static final String			UTF_16							= "UTF-16";																//$NON-NLS-1$
	public static final String			ISO_8859_1						= "ISO-8859-1";															//$NON-NLS-1$

	public static final Charset			UTF8_CHARSET					= Charset.forName("UTF-8");												//$NON-NLS-1$

	/**
	 * layout hint for a description field
	 */
	public static final int				DEFAULT_DESCRIPTION_WIDTH		= 350;

	/*
	 * image keys for images which are stored in the image registry
	 */
	public static final String			IMAGE_EMPTY_16					= "_empty16";																//$NON-NLS-1$

	public final static ImageRegistry	IMAGE_REGISTRY;

	static {

		IMAGE_REGISTRY = Activator.getDefault().getImageRegistry();

		IMAGE_REGISTRY.put(IMAGE_EMPTY_16, Activator.getImageDescriptor(Messages.Image___Empty16));

		// weather images
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_CLEAR, //
				Activator.getImageDescriptor(Messages.Image__weather_sunny));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_PART_CLOUDS, //
				Activator.getImageDescriptor(Messages.Image__weather_cloudy));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_OVERCAST, //
				Activator.getImageDescriptor(Messages.Image__weather_clouds));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_LIGHTNING,//
				Activator.getImageDescriptor(Messages.Image__weather_lightning));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_RAIN,//
				Activator.getImageDescriptor(Messages.Image__weather_rain));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_SNOW,//
				Activator.getImageDescriptor(Messages.Image__weather_snow));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_SCATTERED_SHOWERS,//
				Activator.getImageDescriptor(Messages.Image__Weather_ScatteredShowers));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_SEVERE_WEATHER_ALERT,//
				Activator.getImageDescriptor(Messages.Image__Weather_Severe));

	}

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
	 * @return Returns a cursor which is hidden. This cursor must be disposed.
	 */
	public static Cursor createHiddenCursor() {

		// create a cursor with a transparent image

		final Display display = Display.getDefault();

		final Color white = display.getSystemColor(SWT.COLOR_WHITE);
		final Color black = display.getSystemColor(SWT.COLOR_BLACK);

		final PaletteData palette = new PaletteData(new RGB[] { white.getRGB(), black.getRGB() });

		final ImageData sourceData = new ImageData(16, 16, 1, palette);
		sourceData.transparentPixel = 0;

		return new Cursor(display, sourceData, 0, 0);
	}

	/**
	 * @param degreeDirection
	 * @return Returns cardinal direction
	 */
	public static String getCardinalDirectionText(final int degreeDirection) {

		return IWeather.windDirectionText[getCardinalDirectionTextIndex(degreeDirection)];
	}

// this conversion is not working for all png images, found SWT2Dutil.java
//
//	/**
//	 * Converts a Swing BufferedImage into a lightweight ImageData object for SWT
//	 *
//	 * @param bufferedImage
//	 *            the image to be converted
//	 * @param originalImagePathName
//	 * @return An ImageData that represents the same image as bufferedImage
//	 */
//	public static ImageData convertAWTimageIntoSWTimage(final BufferedImage bufferedImage, final String imagePathName) {
//
//		try {
//
//			if (bufferedImage.getColorModel() instanceof DirectColorModel) {
//				final DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
//				final PaletteData palette = new PaletteData(
//						colorModel.getRedMask(),
//						colorModel.getGreenMask(),
//						colorModel.getBlueMask());
//				final ImageData data = new ImageData(
//						bufferedImage.getWidth(),
//						bufferedImage.getHeight(),
//						colorModel.getPixelSize(),
//						palette);
//				final WritableRaster raster = bufferedImage.getRaster();
//				final int[] pixelArray = new int[3];
//				for (int y = 0; y < data.height; y++) {
//					for (int x = 0; x < data.width; x++) {
//						raster.getPixel(x, y, pixelArray);
//						final int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
//						data.setPixel(x, y, pixel);
//					}
//				}
//				return data;
//
//			} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
//
//				final IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
//				final int size = colorModel.getMapSize();
//				final byte[] reds = new byte[size];
//				final byte[] greens = new byte[size];
//				final byte[] blues = new byte[size];
//				colorModel.getReds(reds);
//				colorModel.getGreens(greens);
//				colorModel.getBlues(blues);
//				final RGB[] rgbs = new RGB[size];
//				for (int i = 0; i < rgbs.length; i++) {
//					rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
//				}
//				final PaletteData palette = new PaletteData(rgbs);
//				final ImageData data = new ImageData(
//						bufferedImage.getWidth(),
//						bufferedImage.getHeight(),
//						colorModel.getPixelSize(),
//						palette);
//				data.transparentPixel = colorModel.getTransparentPixel();
//				final WritableRaster raster = bufferedImage.getRaster();
//				final int[] pixelArray = new int[1];
//				for (int y = 0; y < data.height; y++) {
//					for (int x = 0; x < data.width; x++) {
//						raster.getPixel(x, y, pixelArray);
//						data.setPixel(x, y, pixelArray[0]);
//					}
//				}
//				return data;
//
//			} else if (bufferedImage.getColorModel() instanceof ComponentColorModel) {
//
//				final ComponentColorModel colorModel = (ComponentColorModel) bufferedImage.getColorModel();
//
//				//ASSUMES: 3 BYTE BGR IMAGE TYPE
//
//				final PaletteData palette = new PaletteData(0x0000FF, 0x00FF00, 0xFF0000);
//				final ImageData data = new ImageData(
//						bufferedImage.getWidth(),
//						bufferedImage.getHeight(),
//						colorModel.getPixelSize(),
//						palette);
//
//				//This is valid because we are using a 3-byte Data model with no transparent pixels
//				data.transparentPixel = -1;
//
//				final WritableRaster raster = bufferedImage.getRaster();
//
////				final int[] pixelArray = new int[3];
//				final int[] pixelArray = colorModel.getComponentSize();
//
//				for (int y = 0; y < data.height; y++) {
//					for (int x = 0; x < data.width; x++) {
//						raster.getPixel(x, y, pixelArray);
//						final int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
//						data.setPixel(x, y, pixel);
//					}
//				}
//				return data;
//			}
//
//		} catch (final Exception e) {
//
//			System.out.println(NLS.bind(//
//					UI.timeStamp() + "Cannot convert AWT image into SWT image: {0}",
//					imagePathName));
//		}
//
//		return null;
//	}

	/**
	 * @param degreeDirection
	 * @return Returns cardinal direction index for {@link IWeather#windDirectionText}
	 */
	public static int getCardinalDirectionTextIndex(final int degreeDirection) {

		final float degree = (degreeDirection + 22.5f) / 45.0f;

		final int directionIndex = ((int) degree) % 8;

		return directionIndex;
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

	public static String timeStamp() {
		return (new Timestamp()).toString(Format.Log);
	}

	public static String timeStampNano() {
		return (new Timestamp()).toString();
	}

	public static void updateScrolledContent(final Composite composite) {

		Composite child = composite;
		Composite parent = composite.getParent();

		while (parent != null) {

			// go up until the first scrolled container

			if (parent instanceof ScrolledComposite) {

				final ScrolledComposite scrolledContainer = (ScrolledComposite) parent;

				/*
				 * update layout: both methods must be called because the size can be modified and a
				 * layout with resized controls MUST be done !!!!
				 */
				scrolledContainer.setMinSize(child.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				scrolledContainer.layout(true, true);

				break;
			}

			child = parent;
			parent = parent.getParent();
		}
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
