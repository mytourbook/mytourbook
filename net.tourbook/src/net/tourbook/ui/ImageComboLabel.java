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
package net.tourbook.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * This is a copy of CLabel but the indent is smaller
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */

/**
 * A Label which supports aligned text and/or an image and different border styles.
 * <p>
 * If there is not enough space a CLabel uses the following strategy to fit the information into the
 * available space:
 * 
 * <pre>
 * 		ignores the indent in left align mode
 * 		ignores the image and the gap
 * 		shortens the text by replacing the center portion of the label with an ellipsis
 * 		shortens the text by removing the center portion of the label
 * </pre>
 * <p>
 * <dl>
 * <dt><b>Styles:</b>
 * <dd>LEFT, RIGHT, CENTER, SHADOW_IN, SHADOW_OUT, SHADOW_NONE</dd>
 * <dt><b>Events:</b>
 * <dd></dd>
 * </dl>
 * </p>
 * <p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 */
public class ImageComboLabel extends Canvas {

	/** Gap between icon and text */
//	private static final int	GAP			= 5;
	private static final int	GAP			= 3;

	/** Left and right margins */
//	private static final int	INDENT		= 3;
	private static final int	INDENT		= 0;

	/** a string inserted in the middle of text that has been shortened */
	private static final String	ELLIPSIS	= "...";						//$NON-NLS-1$ // could use the ellipsis glyph on some platforms "\u2026"
	/** the alignment. Either CENTER, RIGHT, LEFT. Default is LEFT */
	private int					align		= SWT.LEFT;
	private int					hIndent		= INDENT;
	private int					vIndent		= INDENT;
	/** the current text */
	private String				text;
	/** the current icon */
	private Image				image;
	// The tooltip is used for two purposes - the application can set
	// a tooltip or the tooltip can be used to display the full text when the
	// the text has been truncated due to the label being too short.
	// The appToolTip stores the tooltip set by the application.  Control.tooltiptext
	// contains whatever tooltip is currently being displayed.
	private String				appToolTipText;

	private Image				backgroundImage;
	private Color[]				gradientColors;
	private int[]				gradientPercents;
	private boolean				gradientVertical;
	private Color				background;

	private static int			DRAW_FLAGS	= SWT.DRAW_MNEMONIC
													| SWT.DRAW_TAB
													| SWT.DRAW_TRANSPARENT
													| SWT.DRAW_DELIMITER;

	/**
	 * Constructs a new instance of this class given its parent and a style value describing its
	 * behavior and appearance.
	 * <p>
	 * The style value is either one of the style constants defined in class <code>SWT</code> which
	 * is applicable to instances of this class, or must be built by <em>bitwise OR</em>'ing
	 * together (that is, using the <code>int</code> "|" operator) two or more of those
	 * <code>SWT</code> style constants. The class description lists the style constants that are
	 * applicable to the class. Style bits are also inherited from superclasses.
	 * </p>
	 * 
	 * @param parent
	 *            a widget which will be the parent of the new instance (cannot be null)
	 * @param style
	 *            the style of widget to construct
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the parent</li>
	 *                </ul>
	 * @see SWT#LEFT
	 * @see SWT#RIGHT
	 * @see SWT#CENTER
	 * @see SWT#SHADOW_IN
	 * @see SWT#SHADOW_OUT
	 * @see SWT#SHADOW_NONE
	 * @see #getStyle()
	 */
	public ImageComboLabel(final Composite parent, int style) {
		super(parent, checkStyle(style));
		if ((style & (SWT.CENTER | SWT.RIGHT)) == 0) {
			style |= SWT.LEFT;
		}
		if ((style & SWT.CENTER) != 0) {
			align = SWT.CENTER;
		}
		if ((style & SWT.RIGHT) != 0) {
			align = SWT.RIGHT;
		}
		if ((style & SWT.LEFT) != 0) {
			align = SWT.LEFT;
		}

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent event) {
				onPaint(event);
			}
		});

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent event) {
				onDispose(event);
			}
		});

		addTraverseListener(new TraverseListener() {
			public void keyTraversed(final TraverseEvent event) {
				if (event.detail == SWT.TRAVERSE_MNEMONIC) {
					onMnemonic(event);
				}
			}
		});

		initAccessible();

	}

	/**
	 * Check the style bits to ensure that no invalid styles are applied.
	 */
	private static int checkStyle(int style) {
		if ((style & SWT.BORDER) != 0) {
			style |= SWT.SHADOW_IN;
		}
		final int mask = SWT.SHADOW_IN | SWT.SHADOW_OUT | SWT.SHADOW_NONE | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
		style = style & mask;
		style |= SWT.NO_FOCUS | SWT.DOUBLE_BUFFERED;
		return style;
	}

//protected void checkSubclass () {
//	String name = getClass().getName ();
//	String validName = CLabel.class.getName();
//	if (!validName.equals(name)) {
//		SWT.error (SWT.ERROR_INVALID_SUBCLASS);
//	}
//}

	/*
	 * Return the lowercase of the first non-'&' character following an '&' character in the given
	 * string. If there are no '&' characters in the given string, return '\0'.
	 */
	char _findMnemonic(final String string) {
		if (string == null) {
			return '\0';
		}
		int index = 0;
		final int length = string.length();
		do {
			while (index < length && string.charAt(index) != '&') {
				index++;
			}
			if (++index >= length) {
				return '\0';
			}
			if (string.charAt(index) != '&') {
				return Character.toLowerCase(string.charAt(index));
			}
			index++;
		}
		while (index < length);
		return '\0';
	}

	@Override
	public Point computeSize(final int wHint, final int hHint, final boolean changed) {
		checkWidget();
		final Point e = getTotalSize(image, text);
		if (wHint == SWT.DEFAULT) {
			e.x += 2 * hIndent;
		} else {
			e.x = wHint;
		}
		if (hHint == SWT.DEFAULT) {
			e.y += 2 * vIndent;
		} else {
			e.y = hHint;
		}
		return e;
	}

	/**
	 * Draw a rectangle in the given colors.
	 */
	private void drawBevelRect(	final GC gc,
								final int x,
								final int y,
								final int w,
								final int h,
								final Color topleft,
								final Color bottomright) {
		gc.setForeground(bottomright);
		gc.drawLine(x + w, y, x + w, y + h);
		gc.drawLine(x, y + h, x + w, y + h);

		gc.setForeground(topleft);
		gc.drawLine(x, y, x + w - 1, y);
		gc.drawLine(x, y, x, y + h - 1);
	}

	/**
	 * Returns the alignment. The alignment style (LEFT, CENTER or RIGHT) is returned.
	 * 
	 * @return SWT.LEFT, SWT.RIGHT or SWT.CENTER
	 */
	public int getAlignment() {
		//checkWidget();
		return align;
	}

	/**
	 * Return the CLabel's image or <code>null</code>.
	 * 
	 * @return the image of the label or null
	 */
	public Image getImage() {
		//checkWidget();
		return image;
	}

	@Override
	public int getStyle() {
		int style = super.getStyle();
		switch (align) {
		case SWT.RIGHT:
			style |= SWT.RIGHT;
			break;
		case SWT.CENTER:
			style |= SWT.CENTER;
			break;
		case SWT.LEFT:
			style |= SWT.LEFT;
			break;
		}
		return style;
	}

	/**
	 * Return the Label's text.
	 * 
	 * @return the text of the label or null
	 */
	public String getText() {
		//checkWidget();
		return text;
	}

	@Override
	public String getToolTipText() {
		checkWidget();
		return appToolTipText;
	}

	/**
	 * Compute the minimum size.
	 */
	private Point getTotalSize(final Image image, final String text) {
		final Point size = new Point(0, 0);

		if (image != null) {
			final Rectangle r = image.getBounds();
			size.x += r.width;
			size.y += r.height;
		}

		final GC gc = new GC(this);
		if (text != null && text.length() > 0) {
			final Point e = gc.textExtent(text, DRAW_FLAGS);
			size.x += e.x;
			size.y = Math.max(size.y, e.y);
			if (image != null) {
				size.x += GAP;
			}
		} else {
			size.y = Math.max(size.y, gc.getFontMetrics().getHeight());
		}
		gc.dispose();

		return size;
	}

	private void initAccessible() {
		final Accessible accessible = getAccessible();
		accessible.addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getHelp(final AccessibleEvent e) {
				e.result = getToolTipText();
			}

			@Override
			public void getKeyboardShortcut(final AccessibleEvent e) {
				final char mnemonic = _findMnemonic(ImageComboLabel.this.text);
				if (mnemonic != '\0') {
					e.result = "Alt+" + mnemonic; //$NON-NLS-1$
				}
			}

			@Override
			public void getName(final AccessibleEvent e) {
				e.result = getText();
			}
		});

		accessible.addAccessibleControlListener(new AccessibleControlAdapter() {
			@Override
			public void getChildAtPoint(final AccessibleControlEvent e) {
				e.childID = ACC.CHILDID_SELF;
			}

			@Override
			public void getChildCount(final AccessibleControlEvent e) {
				e.detail = 0;
			}

			@Override
			public void getLocation(final AccessibleControlEvent e) {
				final Rectangle rect = getDisplay().map(getParent(), null, getBounds());
				e.x = rect.x;
				e.y = rect.y;
				e.width = rect.width;
				e.height = rect.height;
			}

			@Override
			public void getRole(final AccessibleControlEvent e) {
				e.detail = ACC.ROLE_LABEL;
			}

			@Override
			public void getState(final AccessibleControlEvent e) {
				e.detail = ACC.STATE_READONLY;
			}
		});
	}

	void onDispose(final DisposeEvent event) {
		gradientColors = null;
		gradientPercents = null;
		backgroundImage = null;
		text = null;
		image = null;
		appToolTipText = null;
	}

	void onMnemonic(final TraverseEvent event) {
		final char mnemonic = _findMnemonic(text);
		if (mnemonic == '\0') {
			return;
		}
		if (Character.toLowerCase(event.character) != mnemonic) {
			return;
		}
		Composite control = this.getParent();
		while (control != null) {
			final Control[] children = control.getChildren();
			int index = 0;
			while (index < children.length) {
				if (children[index] == this) {
					break;
				}
				index++;
			}
			index++;
			if (index < children.length) {
				if (children[index].setFocus()) {
					event.doit = true;
					event.detail = SWT.TRAVERSE_NONE;
				}
			}
			control = control.getParent();
		}
	}

	void onPaint(final PaintEvent event) {
		final Rectangle rect = getClientArea();
		if (rect.width == 0 || rect.height == 0) {
			return;
		}

		boolean shortenText = false;
		final String t = text;
		Image img = image;
		final int availableWidth = Math.max(0, rect.width - 2 * hIndent);
		Point extent = getTotalSize(img, t);
		if (extent.x > availableWidth) {
			img = null;
			extent = getTotalSize(img, t);
			if (extent.x > availableWidth) {
				shortenText = true;
			}
		}

		final GC gc = event.gc;
		final String[] lines = text == null ? null : splitString(text);

		// shorten the text
		if (shortenText) {
			extent.x = 0;
			for (int i = 0; i < lines.length; i++) {
				final Point e = gc.textExtent(lines[i], DRAW_FLAGS);
				if (e.x > availableWidth) {
					lines[i] = shortenText(gc, lines[i], availableWidth);
					extent.x = Math.max(extent.x, getTotalSize(null, lines[i]).x);
				} else {
					extent.x = Math.max(extent.x, e.x);
				}
			}
			if (appToolTipText == null) {
				super.setToolTipText(text);
			}
		} else {
			super.setToolTipText(appToolTipText);
		}

		// determine horizontal position
		int x = rect.x + hIndent;
		if (align == SWT.CENTER) {
			x = (rect.width - extent.x) / 2;
		}
		if (align == SWT.RIGHT) {
			x = rect.width - hIndent - extent.x;
		}

		// draw a background image behind the text
		try {
			if (backgroundImage != null) {
				// draw a background image behind the text
				final Rectangle imageRect = backgroundImage.getBounds();
				// tile image to fill space
				gc.setBackground(getBackground());
				gc.fillRectangle(rect);
				int xPos = 0;
				while (xPos < rect.width) {
					int yPos = 0;
					while (yPos < rect.height) {
						gc.drawImage(backgroundImage, xPos, yPos);
						yPos += imageRect.height;
					}
					xPos += imageRect.width;
				}
			} else if (gradientColors != null) {
				// draw a gradient behind the text
				final Color oldBackground = gc.getBackground();
				if (gradientColors.length == 1) {
					if (gradientColors[0] != null) {
						gc.setBackground(gradientColors[0]);
					}
					gc.fillRectangle(0, 0, rect.width, rect.height);
				} else {
					final Color oldForeground = gc.getForeground();
					Color lastColor = gradientColors[0];
					if (lastColor == null) {
						lastColor = oldBackground;
					}
					int pos = 0;
					for (int i = 0; i < gradientPercents.length; ++i) {
						gc.setForeground(lastColor);
						lastColor = gradientColors[i + 1];
						if (lastColor == null) {
							lastColor = oldBackground;
						}
						gc.setBackground(lastColor);
						if (gradientVertical) {
							final int gradientHeight = (gradientPercents[i] * rect.height / 100) - pos;
							gc.fillGradientRectangle(0, pos, rect.width, gradientHeight, true);
							pos += gradientHeight;
						} else {
							final int gradientWidth = (gradientPercents[i] * rect.width / 100) - pos;
							gc.fillGradientRectangle(pos, 0, gradientWidth, rect.height, false);
							pos += gradientWidth;
						}
					}
					if (gradientVertical && pos < rect.height) {
						gc.setBackground(getBackground());
						gc.fillRectangle(0, pos, rect.width, rect.height - pos);
					}
					if (!gradientVertical && pos < rect.width) {
						gc.setBackground(getBackground());
						gc.fillRectangle(pos, 0, rect.width - pos, rect.height);
					}
					gc.setForeground(oldForeground);
				}
				gc.setBackground(oldBackground);
			} else {
				if (background != null || (getStyle() & SWT.DOUBLE_BUFFERED) == 0) {
					gc.setBackground(getBackground());
					gc.fillRectangle(rect);
				}
			}
		} catch (final SWTException e) {
			if ((getStyle() & SWT.DOUBLE_BUFFERED) == 0) {
				gc.setBackground(getBackground());
				gc.fillRectangle(rect);
			}
		}

		// draw border
		final int style = getStyle();
		if ((style & SWT.SHADOW_IN) != 0 || (style & SWT.SHADOW_OUT) != 0) {
			paintBorder(gc, rect);
		}

		// draw the image
		if (img != null) {
			final Rectangle imageRect = img.getBounds();
			gc.drawImage(img,
					0,
					0,
					imageRect.width,
					imageRect.height,
					x,
					(rect.height - imageRect.height) / 2,
					imageRect.width,
					imageRect.height);
			x += imageRect.width + GAP;
			extent.x -= imageRect.width + GAP;
		}
		// draw the text
		if (lines != null) {
			final int lineHeight = gc.getFontMetrics().getHeight();
			final int textHeight = lines.length * lineHeight;
			int lineY = Math.max(vIndent, rect.y + (rect.height - textHeight) / 2);
			gc.setForeground(getForeground());
			for (final String line : lines) {
				int lineX = x;
				if (lines.length > 1) {
					if (align == SWT.CENTER) {
						final int lineWidth = gc.textExtent(line, DRAW_FLAGS).x;
						lineX = x + Math.max(0, (extent.x - lineWidth) / 2);
					}
					if (align == SWT.RIGHT) {
						final int lineWidth = gc.textExtent(line, DRAW_FLAGS).x;
						lineX = Math.max(x, rect.x + rect.width - hIndent - lineWidth);
					}
				}
				gc.drawText(line, lineX, lineY, DRAW_FLAGS);
				lineY += lineHeight;
			}
		}
	}

	/**
	 * Paint the Label's border.
	 */
	private void paintBorder(final GC gc, final Rectangle r) {
		final Display disp = getDisplay();

		Color c1 = null;
		Color c2 = null;

		final int style = getStyle();
		if ((style & SWT.SHADOW_IN) != 0) {
			c1 = disp.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
			c2 = disp.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);
		}
		if ((style & SWT.SHADOW_OUT) != 0) {
			c1 = disp.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
			c2 = disp.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
		}

		if (c1 != null && c2 != null) {
			gc.setLineWidth(1);
			drawBevelRect(gc, r.x, r.y, r.width - 1, r.height - 1, c1, c2);
		}
	}

	/**
	 * Set the alignment of the CLabel. Use the values LEFT, CENTER and RIGHT to align image and
	 * text within the available space.
	 * 
	 * @param align
	 *            the alignment style of LEFT, RIGHT or CENTER
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                <li>ERROR_INVALID_ARGUMENT - if the value of align is not one of SWT.LEFT,
	 *                SWT.RIGHT or SWT.CENTER</li>
	 *                </ul>
	 */
	public void setAlignment(final int align) {
		checkWidget();
		if (align != SWT.LEFT && align != SWT.RIGHT && align != SWT.CENTER) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		if (this.align != align) {
			this.align = align;
			redraw();
		}
	}

	@Override
	public void setBackground(final Color color) {
		super.setBackground(color);
		// Are these settings the same as before?
		if (backgroundImage == null && gradientColors == null && gradientPercents == null) {
			if (color == null) {
				if (background == null) {
					return;
				}
			} else {
				if (color.equals(background)) {
					return;
				}
			}
		}
		background = color;
		backgroundImage = null;
		gradientColors = null;
		gradientPercents = null;
		redraw();
	}

	/**
	 * Specify a gradient of colours to be drawn in the background of the CLabel.
	 * <p>
	 * For example, to draw a gradient that varies from dark blue to blue and then to white and
	 * stays white for the right half of the label, use the following call to setBackground:
	 * </p>
	 * 
	 * <pre>
	 * clabel.setBackground(new Color[] {
	 * 		display.getSystemColor(SWT.COLOR_DARK_BLUE),
	 * 		display.getSystemColor(SWT.COLOR_BLUE),
	 * 		display.getSystemColor(SWT.COLOR_WHITE),
	 * 		display.getSystemColor(SWT.COLOR_WHITE) }, new int[] { 25, 50, 100 });
	 * </pre>
	 * 
	 * @param colors
	 *            an array of Color that specifies the colors to appear in the gradient in order of
	 *            appearance from left to right; The value <code>null</code> clears the background
	 *            gradient; the value <code>null</code> can be used inside the array of Color to
	 *            specify the background color.
	 * @param percents
	 *            an array of integers between 0 and 100 specifying the percent of the width of the
	 *            widget at which the color should change; the size of the percents array must be
	 *            one less than the size of the colors array.
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                <li>ERROR_INVALID_ARGUMENT - if the values of colors and percents are not
	 *                consistent</li>
	 *                </ul>
	 */
	public void setBackground(final Color[] colors, final int[] percents) {
		setBackground(colors, percents, false);
	}

	/**
	 * Specify a gradient of colours to be drawn in the background of the CLabel.
	 * <p>
	 * For example, to draw a gradient that varies from dark blue to white in the vertical,
	 * direction use the following call to setBackground:
	 * </p>
	 * 
	 * <pre>
	 * clabel.setBackground(new Color[] { display.getSystemColor(SWT.COLOR_DARK_BLUE), display.getSystemColor(SWT.COLOR_WHITE) },
	 * 		new int[] { 100 },
	 * 		true);
	 * </pre>
	 * 
	 * @param colors
	 *            an array of Color that specifies the colors to appear in the gradient in order of
	 *            appearance from left/top to right/bottom; The value <code>null</code> clears the
	 *            background gradient; the value <code>null</code> can be used inside the array of
	 *            Color to specify the background color.
	 * @param percents
	 *            an array of integers between 0 and 100 specifying the percent of the width/height
	 *            of the widget at which the color should change; the size of the percents array
	 *            must be one less than the size of the colors array.
	 * @param vertical
	 *            indicate the direction of the gradient. True is vertical and false is horizontal.
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                <li>ERROR_INVALID_ARGUMENT - if the values of colors and percents are not
	 *                consistent</li>
	 *                </ul>
	 * @since 3.0
	 */
	public void setBackground(Color[] colors, int[] percents, final boolean vertical) {
		checkWidget();
		if (colors != null) {
			if (percents == null || percents.length != colors.length - 1) {
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			}
			if (getDisplay().getDepth() < 15) {
				// Don't use gradients on low color displays
				colors = new Color[] { colors[colors.length - 1] };
				percents = new int[] {};
			}
			for (int i = 0; i < percents.length; i++) {
				if (percents[i] < 0 || percents[i] > 100) {
					SWT.error(SWT.ERROR_INVALID_ARGUMENT);
				}
				if (i > 0 && percents[i] < percents[i - 1]) {
					SWT.error(SWT.ERROR_INVALID_ARGUMENT);
				}
			}
		}

		// Are these settings the same as before?
		final Color background = getBackground();
		if (backgroundImage == null) {
			if ((gradientColors != null) && (colors != null) && (gradientColors.length == colors.length)) {
				boolean same = false;
				for (int i = 0; i < gradientColors.length; i++) {
					same = (gradientColors[i] == colors[i])
							|| ((gradientColors[i] == null) && (colors[i] == background))
							|| ((gradientColors[i] == background) && (colors[i] == null));
					if (!same) {
						break;
					}
				}
				if (same) {
					for (int i = 0; i < gradientPercents.length; i++) {
						same = gradientPercents[i] == percents[i];
						if (!same) {
							break;
						}
					}
				}
				if (same && this.gradientVertical == vertical) {
					return;
				}
			}
		} else {
			backgroundImage = null;
		}
		// Store the new settings
		if (colors == null) {
			gradientColors = null;
			gradientPercents = null;
			gradientVertical = false;
		} else {
			gradientColors = new Color[colors.length];
			for (int i = 0; i < colors.length; ++i) {
				gradientColors[i] = (colors[i] != null) ? colors[i] : background;
			}
			gradientPercents = new int[percents.length];
			for (int i = 0; i < percents.length; ++i) {
				gradientPercents[i] = percents[i];
			}
			gradientVertical = vertical;
		}
		// Refresh with the new settings
		redraw();
	}

	/**
	 * Set the image to be drawn in the background of the label.
	 * 
	 * @param image
	 *            the image to be drawn in the background
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public void setBackground(final Image image) {
		checkWidget();
		if (image == backgroundImage) {
			return;
		}
		if (image != null) {
			gradientColors = null;
			gradientPercents = null;
		}
		backgroundImage = image;
		redraw();

	}

	@Override
	public void setFont(final Font font) {
		super.setFont(font);
		redraw();
	}

	/**
	 * Set the label's Image. The value <code>null</code> clears it.
	 * 
	 * @param image
	 *            the image to be displayed in the label or null
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public void setImage(final Image image) {
		checkWidget();
		if (image != this.image) {
			this.image = image;
			redraw();
		}
	}

	/**
	 * Set the label's text. The value <code>null</code> clears it.
	 * 
	 * @param text
	 *            the text to be displayed in the label or null
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public void setText(String text) {
		checkWidget();
		if (text == null) {
			text = UI.EMPTY_STRING;
		}
		if (!text.equals(this.text)) {
			this.text = text;
			redraw();
		}
	}

	@Override
	public void setToolTipText(final String string) {
		super.setToolTipText(string);
		appToolTipText = super.getToolTipText();
	}

	/**
	 * Shorten the given text <code>t</code> so that its length doesn't exceed the given width. The
	 * default implementation replaces characters in the center of the original string with an
	 * ellipsis ("..."). Override if you need a different strategy.
	 * 
	 * @param gc
	 *            the gc to use for text measurement
	 * @param t
	 *            the text to shorten
	 * @param width
	 *            the width to shorten the text to, in pixels
	 * @return the shortened text
	 */
	protected String shortenText(final GC gc, final String t, final int width) {
		if (t == null) {
			return null;
		}
		final int w = gc.textExtent(ELLIPSIS, DRAW_FLAGS).x;
		if (width <= w) {
			return t;
		}
		final int l = t.length();
		int max = l / 2;
		int min = 0;
		int mid = (max + min) / 2 - 1;
		if (mid <= 0) {
			return t;
		}
		while (min < mid && mid < max) {
			final String s1 = t.substring(0, mid);
			final String s2 = t.substring(l - mid, l);
			final int l1 = gc.textExtent(s1, DRAW_FLAGS).x;
			final int l2 = gc.textExtent(s2, DRAW_FLAGS).x;
			if (l1 + w + l2 > width) {
				max = mid;
				mid = (max + min) / 2;
			} else if (l1 + w + l2 < width) {
				min = mid;
				mid = (max + min) / 2;
			} else {
				min = max;
			}
		}
		if (mid == 0) {
			return t;
		}
		return t.substring(0, mid) + ELLIPSIS + t.substring(l - mid, l);
	}

	private String[] splitString(final String text) {
		String[] lines = new String[1];
		int start = 0, pos;
		do {
			pos = text.indexOf('\n', start);
			if (pos == -1) {
				lines[lines.length - 1] = text.substring(start);
			} else {
				final boolean crlf = (pos > 0) && (text.charAt(pos - 1) == '\r');
				lines[lines.length - 1] = text.substring(start, pos - (crlf ? 1 : 0));
				start = pos + 1;
				final String[] newLines = new String[lines.length + 1];
				System.arraycopy(lines, 0, newLines, 0, lines.length);
				lines = newLines;
			}
		}
		while (pos != -1);
		return lines;
	}
}
