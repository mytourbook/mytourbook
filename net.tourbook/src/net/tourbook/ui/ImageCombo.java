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
package net.tourbook.ui;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.swt.widgets.Widget;

/**
 * The ImageCombo class represents a selectable user interface object that combines a text field and
 * a table and issues notification when an item is selected from the table.
 * <p>
 * <b>GTK Corrections:</b>
 * <dl>
 * <dt>Incorrect Border</dt>
 * <dd>(Most) GTK themes draw the Text border, rather than a border around the whole combo, thus
 * CCombo stands out as distinctly non-native - especially GNOME's new standard "ClearLooks" which
 * shows the gray background between Text and Arrow and border</dd>
 * <dt>Arrow gets the focus:</dt>
 * <dd>It used to take two tabs to cross a CCombo. Solution: if the arrow gets a SWT.FOCUS_IN event,
 * ignore it and setFocus to the Text</dd>
 * <dt>When dropped, clicking the arrow closes and immediately re-drops:</dt>
 * <dd>see comment in the popup shell's SWT.DEACTIVATE event</dd>
 * </dl>
 * </p>
 * <p>
 * Note that although this class is a subclass of <code>Composite</code>, it does not make sense to
 * add children to it, or set a layout on it.
 * </p>
 * <dl>
 * <dt><b>Styles:</b>
 * <dd>BORDER, READ_ONLY, FLAT</dd>
 * <dt><b>Events:</b>
 * <dd>Selection</dd>
 * </dl>
 */
public final class ImageCombo extends Composite {

	private static final boolean	gtk					= "gtk".equals(SWT.getPlatform());	//$NON-NLS-1$
	private boolean					dontDrop			= false;

	ImageComboLabel					fLabel;
	Table							table;
	int								visibleItemCount	= 5;
	Shell							popup;
	Button							arrow;
	boolean							hasFocus;
	Listener						listener, filter;
	Color							foreground, background;
	Font							font;

	static int checkStyle(final int style) {
		final int mask = gtk
				? SWT.BORDER | SWT.READ_ONLY | SWT.FLAT | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT
				: SWT.BORDER | SWT.READ_ONLY | SWT.FLAT | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
		return style & mask;
	}

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
	 * @see SWT#BORDER
	 * @see SWT#READ_ONLY
	 * @see SWT#FLAT
	 * @see Widget#getStyle()
	 */
	public ImageCombo(final Composite parent, int style) {

		super(parent, style = checkStyle(style));

		int textStyle = SWT.SINGLE;
		if (gtk) {
			textStyle |= SWT.BORDER;
		}
		if ((style & SWT.READ_ONLY) != 0) {
			textStyle |= SWT.READ_ONLY;
		}
		if ((style & SWT.FLAT) != 0) {
			textStyle |= SWT.FLAT;
		}
		fLabel = new ImageComboLabel(this, SWT.SHADOW_NONE | SWT.FLAT);
		fLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		int arrowStyle = SWT.ARROW | SWT.DOWN;
		if ((style & SWT.FLAT) != 0) {
			arrowStyle |= SWT.FLAT;
		}
		arrow = new Button(this, arrowStyle);

		listener = new Listener() {
			public void handleEvent(final Event event) {

				if (popup == event.widget) {
					onPopupEvent(event);
					return;
				}
				if (fLabel == event.widget) {
					onTextEvent(event);
					return;
				}
				if (table == event.widget) {
					onTableEvent(event);
					return;
				}
				if (arrow == event.widget) {
					onArrowEvent(event);
					return;
				}
				if (ImageCombo.this == event.widget) {
					onComboEvent(event);
					return;
				}
				if (getShell() == event.widget) {
					handleFocus(SWT.FocusOut);
				}
			}
		};

		filter = new Listener() {
			public void handleEvent(final Event event) {
				final Shell shell = ((Control) event.widget).getShell();
				if (shell == ImageCombo.this.getShell()) {
					handleFocus(SWT.FocusOut);
				}
			}
		};

		final int[] comboEvents = { SWT.Dispose, SWT.Move, SWT.Resize };
		for (final int comboEvent : comboEvents) {
			this.addListener(comboEvent, listener);
		}

		final int[] labelEvents = {
				SWT.KeyDown,
				SWT.KeyUp,
				SWT.Modify,
				SWT.MouseDown,
				SWT.MouseUp,
				SWT.Traverse,
				SWT.FocusIn,
				SWT.MouseWheel };
		for (final int labelEvent : labelEvents) {
			fLabel.addListener(labelEvent, listener);
		}

		final int[] arrowEvents = { SWT.Selection, SWT.FocusIn };
		for (final int arrowEvent : arrowEvents) {
			arrow.addListener(arrowEvent, listener);
		}

		createPopup(-1);
		initAccessible();
	}

	/**
	 * Adds the argument to the end of the receiver's list.
	 * 
	 * @param string
	 *            the new item
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the string is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @see #add(String,int)
	 */
	public void add(final String string, final Image image) {
		checkWidget();
		if (string == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		final TableItem newItem = new TableItem(this.table, SWT.NONE);

		newItem.setText(string);

		if (image != null && image.isDisposed() == false) {
			newItem.setImage(image);
		}
	}

	/**
	 * Adds the argument to the receiver's list at the given zero-relative index.
	 * <p>
	 * Note: To add an item at the end of the list, use the result of calling
	 * <code>getItemCount()</code> as the index or use <code>add(String)</code>.
	 * </p>
	 * 
	 * @param string
	 *            the new item
	 * @param index
	 *            the index for the item
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the string is null</li>
	 *                <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of
	 *                elements in the list (inclusive)</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @see #add(String)
	 */
	public void add(final String string, final Image image, final int index) {
		checkWidget();
		if (string == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		final TableItem newItem = new TableItem(this.table, SWT.NONE, index);
		if (image != null) {
			newItem.setImage(image);
		}
	}

	/**
	 * Adds the listener to the collection of listeners who will be notified when the receiver's
	 * text is modified, by sending it one of the messages defined in the
	 * <code>ModifyListener</code> interface.
	 * 
	 * @param listener
	 *            the listener which should be notified
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @see ModifyListener
	 * @see #removeModifyListener
	 */
	public void addModifyListener(final ModifyListener listener) {
		checkWidget();
		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		final TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Modify, typedListener);
	}

	/**
	 * Adds the listener to the collection of listeners who will be notified when the receiver's
	 * selection changes, by sending it one of the messages defined in the
	 * <code>SelectionListener</code> interface.
	 * <p>
	 * <code>widgetSelected</code> is called when the combo's list selection changes.
	 * <code>widgetDefaultSelected</code> is typically called when ENTER is pressed the combo's text
	 * area.
	 * </p>
	 * 
	 * @param listener
	 *            the listener which should be notified
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @see SelectionListener
	 * @see #removeSelectionListener
	 * @see SelectionEvent
	 */
	public void addSelectionListener(final SelectionListener listener) {
		checkWidget();
		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		final TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Selection, typedListener);
		addListener(SWT.DefaultSelection, typedListener);
	}

	/**
	 * Sets the selection in the receiver's text field to an empty selection starting just before
	 * the first character. If the text field is editable, this has the effect of placing the i-beam
	 * at the start of the text.
	 * <p>
	 * Note: To clear the selected items in the receiver's list, use <code>deselectAll()</code>.
	 * </p>
	 * 
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @see #deselectAll
	 */
	public void clearSelection() {
		checkWidget();
//		text.clearSelection();
		table.deselectAll();
	}

	@Override
	public Point computeSize(final int wHint, final int hHint, final boolean changed) {
		checkWidget();
		int width = 0, height = 0;
		final String[] items = getStringsFromTable();
		int textWidth = 0;
		final GC gc = new GC(fLabel);
		final int spacer = gc.stringExtent(" ").x; //$NON-NLS-1$
		for (final String item : items) {
			textWidth = Math.max(gc.stringExtent(item).x, textWidth);
		}
		gc.dispose();
		final Point textSize = fLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
		final Point arrowSize = arrow.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
		final Point listSize = table.computeSize(wHint, SWT.DEFAULT, changed);
		final int borderWidth = getBorderWidth();

		height = Math.max(hHint, Math.max(textSize.y, arrowSize.y) + 2 * borderWidth);
		width = Math.max(wHint, Math.max(textWidth + 2 * spacer + arrowSize.x + 2 * borderWidth, listSize.x));
		return new Point(width, height);
	}

	void createPopup(final int selectionIndex) {

		// create shell and list
		popup = new Shell(getShell(), SWT.NO_TRIM | SWT.ON_TOP);

		final int style = getStyle();
		int listStyle = SWT.SINGLE | SWT.V_SCROLL | SWT.FULL_SELECTION;
		if ((style & SWT.FLAT) != 0) {
			listStyle |= SWT.FLAT;
		}
		if ((style & SWT.RIGHT_TO_LEFT) != 0) {
			listStyle |= SWT.RIGHT_TO_LEFT;
		}
		if ((style & SWT.LEFT_TO_RIGHT) != 0) {
			listStyle |= SWT.LEFT_TO_RIGHT;
		}
		// create a table instead of a list.
		table = new Table(popup, listStyle);
		if (font != null) {
			table.setFont(font);
		}
		if (foreground != null) {
			table.setForeground(foreground);
		}
		if (background != null) {
			table.setBackground(background);
		}

		final int[] popupEvents = { SWT.Close, SWT.Paint, SWT.Deactivate };
		for (final int popupEvent : popupEvents) {
			popup.addListener(popupEvent, listener);
		}

		final int[] listEvents = {
				SWT.MouseUp,
				SWT.Selection,
				SWT.Traverse,
				SWT.KeyDown,
				SWT.KeyUp,
				SWT.FocusIn,
				SWT.Dispose };
		for (final int listEvent : listEvents) {
			table.addListener(listEvent, listener);
		}

		if (selectionIndex != -1) {
			table.setSelection(selectionIndex);
		}
	}

	/**
	 * Deselects the item at the given zero-relative index in the receiver's list. If the item at
	 * the index was already deselected, it remains deselected. Indices that are out of range are
	 * ignored.
	 * 
	 * @param index
	 *            the index of the item to deselect
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public void deselect(final int index) {
		checkWidget();
		table.deselect(index);
	}

	/**
	 * Deselects all selected items in the receiver's list.
	 * <p>
	 * Note: To clear the selection in the receiver's text field, use <code>clearSelection()</code>.
	 * </p>
	 * 
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @see #clearSelection
	 */
	public void deselectAll() {
		checkWidget();
		table.deselectAll();
	}

	void dropDown(final boolean drop) {

		if (drop == isDropped()) {
			return;
		}

		if (!drop) {

			// hide drop down popup

			popup.setVisible(false);

			if (!isDisposed() /* && arrow.isFocusControl() */) {
//				fLabel.setFocus();
				fLabel.forceFocus();
			}
			return;
		}

		if (getShell() != popup.getParent()) {
			final int selectionIndex = table.getSelectionIndex();
			table.removeListener(SWT.Dispose, listener);
			popup.dispose();
			popup = null;
			table = null;
			createPopup(selectionIndex);
		}

		final Point size = getSize();
		int itemCount = table.getItemCount();
		itemCount = (itemCount == 0) ? visibleItemCount : Math.min(visibleItemCount, itemCount);
		final int itemHeight = table.getItemHeight() * itemCount;
		final Point listSize = table.computeSize(SWT.DEFAULT, itemHeight, false);
		table.setBounds(1, 1, Math.max(size.x - 2, listSize.x), listSize.y);

		final int index = table.getSelectionIndex();
		if (index != -1) {
			table.setTopIndex(index);
		}

		final Display display = getDisplay();
		final Rectangle listRect = table.getBounds();
		final Rectangle parentRect = display.map(getParent(), null, getBounds());
		final Point comboSize = getSize();
		final Rectangle displayRect = getMonitor().getClientArea();
		final int width = Math.max(comboSize.x, listRect.width + 2);
		final int height = listRect.height + 2;
		final int x = parentRect.x;
		int y = parentRect.y + comboSize.y;
		if (y + height > displayRect.y + displayRect.height) {
			y = parentRect.y - height;
		}
		popup.setBounds(x, y, width, height);
		popup.setVisible(true);

		table.setFocus();
	}

	/*
	 * Return the Label immediately preceding the receiver in the z-order, or null if none.
	 */
	Label getAssociatedLabel() {
		final Control[] siblings = getParent().getChildren();
		for (int i = 0; i < siblings.length; i++) {
			if (siblings[i] == ImageCombo.this) {
				if (i > 0 && siblings[i - 1] instanceof Label) {
					return (Label) siblings[i - 1];
				}
			}
		}
		return null;
	}

	@Override
	public Control[] getChildren() {
		checkWidget();
		return new Control[0];
	}

//	/**
//	 * Gets the editable state.
//	 * 
//	 * @return whether or not the reciever is editable
//	 * @exception SWTException
//	 *            <ul>
//	 *            <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
//	 *            <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
//	 *            receiver</li>
//	 *            </ul>
//	 * @since 3.0
//	 */
//	public boolean getEditable() {
//		checkWidget();
//		return text.getEditable();
//	}

	/**
	 * Returns the item at the given, zero-relative index in the receiver's list. Throws an
	 * exception if the index is out of range.
	 * 
	 * @param index
	 *            the index of the item to return
	 * @return the item at the given index
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of
	 *                elements in the list minus 1 (inclusive)</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public TableItem getItem(final int index) {
		checkWidget();
		return this.table.getItem(index);
	}

	/**
	 * Returns the number of items contained in the receiver's list.
	 * 
	 * @return the number of items
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public int getItemCount() {
		checkWidget();
		return table.getItemCount();
	}

	/**
	 * Returns the height of the area which would be used to display <em>one</em> of the items in
	 * the receiver's list.
	 * 
	 * @return the height of one item
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public int getItemHeight() {
		checkWidget();
		return table.getItemHeight();
	}

	/**
	 * Returns an array of <code>String</code>s which are the items in the receiver's list.
	 * <p>
	 * Note: This is not the actual structure used by the receiver to maintain its list of items, so
	 * modifying the array will not affect the receiver.
	 * </p>
	 * 
	 * @return the items in the receiver's list
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public TableItem[] getItems() {
		checkWidget();
		return table.getItems();
	}

	char getMnemonic(final String string) {
		int index = 0;
		final int length = string.length();
		do {
			while ((index < length) && (string.charAt(index) != '&')) {
				index++;
			}
			if (++index >= length) {
				return '\0';
			}
			if (string.charAt(index) != '&') {
				return string.charAt(index);
			}
			index++;
		}
		while (index < length);
		return '\0';
	}

//	/**
//	 * Returns a <code>Point</code> whose x coordinate is the start of the selection in the
//	 * receiver's text field, and whose y coordinate is the end of the selection. The returned
//	 * values are zero-relative. An "empty" selection as indicated by the the x and y coordinates
//	 * having the same value.
//	 * 
//	 * @return a point representing the selection start and end
//	 * @exception SWTException
//	 *            <ul>
//	 *            <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
//	 *            <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
//	 *            receiver</li>
//	 *            </ul>
//	 */
//	public Point getSelection() {
//		checkWidget();
//		return text.getSelection();
//	}

	/**
	 * Returns the zero-relative index of the item which is currently selected in the receiver's
	 * list, or -1 if no item is selected.
	 * 
	 * @return the index of the selected item
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public int getSelectionIndex() {
		checkWidget();
		return table.getSelectionIndex();
	}

	String[] getStringsFromTable() {
		final String[] items = new String[this.table.getItems().length];
		for (int i = 0, n = items.length; i < n; i++) {
			items[i] = this.table.getItem(i).getText();
		}
		return items;
	}

	@Override
	public int getStyle() {
		int style = super.getStyle();
		style &= ~SWT.READ_ONLY;
//		if (!text.getEditable()) {
//			style |= SWT.READ_ONLY;
//		}
		return style;
	}

	/**
	 * Returns a string containing a copy of the contents of the receiver's text field.
	 * 
	 * @return the receiver's text
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public String getText() {
		checkWidget();
		return fLabel.getText();
	}

////	/**
////	 * Returns the height of the receivers's text field.
////	 * 
////	 * @return the text height
////	 * @exception SWTException
////	 *            <ul>
////	 *            <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
////	 *            <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
////	 *            receiver</li>
////	 *            </ul>
////	 */
////	public int getTextHeight() {
////		checkWidget();
////		return text.getLineHeight();
////	}
//
//	/**
//	 * Returns the maximum number of characters that the receiver's text field is capable of
//	 * holding. If this has not been changed by <code>setTextLimit()</code>, it will be the
//	 * constant <code>Combo.LIMIT</code>.
//	 * 
//	 * @return the text limit
//	 * @exception SWTException
//	 *            <ul>
//	 *            <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
//	 *            <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
//	 *            receiver</li>
//	 *            </ul>
//	 */
//	public int getTextLimit() {
//		checkWidget();
//		return text.getTextLimit();
//	}

	/**
	 * Gets the number of items that are visible in the drop down portion of the receiver's list.
	 * 
	 * @return the number of items that are visible
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @since 3.0
	 */
	public int getVisibleItemCount() {
		checkWidget();
		return visibleItemCount;
	}

	void handleFocus(final int type) {
		if (isDisposed()) {
			return;
		}
		switch (type) {
		case SWT.FocusIn: {

			if (hasFocus) {
				return;
			}

			hasFocus = true;

			fLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION));

			final Shell shell = getShell();
			shell.removeListener(SWT.Deactivate, listener);
			shell.addListener(SWT.Deactivate, listener);
			final Display display = getDisplay();
			display.removeFilter(SWT.FocusIn, filter);
			display.addFilter(SWT.FocusIn, filter);
			final Event e = new Event();
			notifyListeners(SWT.FocusIn, e);

			break;
		}
		case SWT.FocusOut: {
			if (!hasFocus) {
				return;
			}
			final Control focusControl = getDisplay().getFocusControl();
			if (focusControl == arrow || focusControl == table || focusControl == fLabel) {
				return;
			}
			hasFocus = false;

			fLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

			final Shell shell = getShell();
			shell.removeListener(SWT.Deactivate, listener);
			final Display display = getDisplay();
			display.removeFilter(SWT.FocusIn, filter);
			final Event e = new Event();
			notifyListeners(SWT.FocusOut, e);
			break;
		}
		}
	}

	/**
	 * Searches the receiver's list starting at the first item (index 0) until an item is found that
	 * is equal to the argument, and returns the index of that item. If no item is found, returns
	 * -1.
	 * 
	 * @param string
	 *            the search item
	 * @return the index of the item
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the string is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public int indexOf(final String string) {
		checkWidget();
		if (string == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		return Arrays.asList(getStringsFromTable()).indexOf(string);
	}

	void initAccessible() {
		final AccessibleAdapter accessibleAdapter = new AccessibleAdapter() {
			@Override
			public void getHelp(final AccessibleEvent e) {
				e.result = getToolTipText();
			}

			@Override
			public void getKeyboardShortcut(final AccessibleEvent e) {
				String shortcut = null;
				final Label label = getAssociatedLabel();
				if (label != null) {
					final String text = label.getText();
					if (text != null) {
						final char mnemonic = getMnemonic(text);
						if (mnemonic != '\0') {
							shortcut = "Alt+" + mnemonic; //$NON-NLS-1$
						}
					}
				}
				e.result = shortcut;
			}

			@Override
			public void getName(final AccessibleEvent e) {
				String name = null;
				final Label label = getAssociatedLabel();
				if (label != null) {
					name = stripMnemonic(label.getText());
				}
				e.result = name;
			}
		};
		getAccessible().addAccessibleListener(accessibleAdapter);
		fLabel.getAccessible().addAccessibleListener(accessibleAdapter);
		table.getAccessible().addAccessibleListener(accessibleAdapter);

		arrow.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getHelp(final AccessibleEvent e) {
				e.result = getToolTipText();
			}

			@Override
			public void getKeyboardShortcut(final AccessibleEvent e) {
				e.result = "Alt+Down Arrow"; //$NON-NLS-1$
			}

			@Override
			public void getName(final AccessibleEvent e) {
				e.result = isDropped() ? SWT.getMessage("SWT_Close") : SWT.getMessage("SWT_Open"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});

//		getAccessible().addAccessibleTextListener(new AccessibleTextAdapter() {
//			@Override
//			public void getCaretOffset(AccessibleTextEvent e) {
//				e.offset = text.getCaretPosition();
//			}
//		});

		getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
			@Override
			public void getChildAtPoint(final AccessibleControlEvent e) {
				final Point testPoint = toControl(e.x, e.y);
				if (getBounds().contains(testPoint)) {
					e.childID = ACC.CHILDID_SELF;
				}
			}

			@Override
			public void getChildCount(final AccessibleControlEvent e) {
				e.detail = 0;
			}

			@Override
			public void getLocation(final AccessibleControlEvent e) {
				final Rectangle location = getBounds();
				final Point pt = toDisplay(location.x, location.y);
				e.x = pt.x;
				e.y = pt.y;
				e.width = location.width;
				e.height = location.height;
			}

			@Override
			public void getRole(final AccessibleControlEvent e) {
				e.detail = ACC.ROLE_COMBOBOX;
			}

			@Override
			public void getState(final AccessibleControlEvent e) {
				e.detail = ACC.STATE_NORMAL;
			}

			@Override
			public void getValue(final AccessibleControlEvent e) {
				e.result = getText();
			}
		});

//		text.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
//			@Override
//			public void getRole(AccessibleControlEvent e) {
//				e.detail = text.getEditable() ? ACC.ROLE_TEXT : ACC.ROLE_LABEL;
//			}
//		});

		arrow.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
			@Override
			public void getDefaultAction(final AccessibleControlEvent e) {
				e.result = isDropped() ? SWT.getMessage("SWT_Close") : SWT.getMessage("SWT_Open"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
	}

	void internalLayout(final boolean changed) {
		if (isDropped()) {
			dropDown(false);
		}
		final Rectangle rect = getClientArea();
		final int width = rect.width;
		final int height = rect.height;
		final Point arrowSize = arrow.computeSize(SWT.DEFAULT, height, changed);
		fLabel.setBounds(0, 0, width - arrowSize.x, height);
		arrow.setBounds(width - arrowSize.x, 0, arrowSize.x, arrowSize.y);
	}

	boolean isDropped() {
		return popup.getVisible();
	}

	@Override
	public boolean isFocusControl() {
		checkWidget();
		if (fLabel.isFocusControl() || arrow.isFocusControl() || table.isFocusControl() || popup.isFocusControl()) {
			return true;
		}
		return super.isFocusControl();
	}

	void onArrowEvent(final Event event) {
		switch (event.type) {
		case SWT.FocusIn: {
			if (gtk) {
				setFocus();
				return;
			}
			handleFocus(SWT.FocusIn);
			break;
		}
		case SWT.Selection: {
			if (gtk) {
				if (!dontDrop) {
					dropDown(!isDropped());
				}
				dontDrop = false;
			} else {
				dropDown(!isDropped());
			}
			break;
		}
		}
	}

	void onComboEvent(final Event event) {
		switch (event.type) {
		case SWT.Dispose:
			if (popup != null && !popup.isDisposed()) {
				table.removeListener(SWT.Dispose, listener);
				popup.dispose();
			}
			final Shell shell = getShell();
			shell.removeListener(SWT.Deactivate, listener);
			final Display display = getDisplay();
			display.removeFilter(SWT.FocusIn, filter);
			popup = null;
			fLabel = null;
			table = null;
			arrow = null;
			break;

		case SWT.Move:
		case SWT.TRAVERSE_TAB_NEXT:
		case SWT.TRAVERSE_TAB_PREVIOUS:
			dropDown(false);
			break;

		case SWT.Resize:
			internalLayout(false);
			break;
		}
	}

	void onPopupEvent(final Event event) {

		switch (event.type) {

		case SWT.Paint:
			// draw black rectangle around list
			final Rectangle listRect = table.getBounds();
			final Color black = getDisplay().getSystemColor(SWT.COLOR_BLACK);
			event.gc.setForeground(black);
			event.gc.drawRectangle(0, 0, listRect.width + 1, listRect.height + 1);
			break;

		case SWT.Close:
			event.doit = false;
			dropDown(false);
			break;

		case SWT.Deactivate:
			// when the popup shell is deactivated by clicking the button,
			// we receive two Deactivate events on Win32, whereas on GTK
			// we first receive one Deactivation event from the shell and
			// then a Selection event from the button.
			// as a work-around, set a flag (dontDrop) if running GTK
			if (gtk) {
				final Point loc = arrow.toControl(getDisplay().getCursorLocation());
				final Point size = arrow.getSize();
				if ((loc.x >= 0) && (loc.y >= 0) && (loc.x < size.x) && (loc.y < size.y)) {
					dontDrop = true;
				}
			}
			dropDown(false);
			break;
		}
	}

	void onTableEvent(final Event event) {
		switch (event.type) {
		case SWT.Dispose:
			if (getShell() != popup.getParent()) {
				final int selectionIndex = table.getSelectionIndex();
				popup = null;
				table = null;
				createPopup(selectionIndex);
			}
			break;
		case SWT.FocusIn: {
			handleFocus(SWT.FocusIn);
			break;
		}
		case SWT.MouseUp: {
			if (event.button != 1) {
				return;
			}
			dropDown(false);
			break;
		}
		case SWT.Selection: {
			final int index = table.getSelectionIndex();
			if (index == -1) {
				return;
			}

			/*
			 * update label text & image
			 */
			final TableItem tableItem = table.getItem(index);
			fLabel.setText(tableItem.getText());

			final Image tableImage = tableItem.getImage();
			if (tableImage != null && tableImage.isDisposed() == false) {
				fLabel.setImage(tableImage);
			}

			table.setSelection(index);

			final Event e = new Event();
			e.time = event.time;
			e.stateMask = event.stateMask;
			e.doit = event.doit;
			notifyListeners(SWT.Selection, e);
			event.doit = e.doit;
			break;
		}

		case SWT.Traverse: {

			switch (event.detail) {
			case SWT.TRAVERSE_RETURN:
			case SWT.TRAVERSE_ESCAPE:
			case SWT.TRAVERSE_ARROW_PREVIOUS:
			case SWT.TRAVERSE_ARROW_NEXT:
				event.doit = false;
				break;

			case SWT.TRAVERSE_TAB_NEXT:
			case SWT.TRAVERSE_TAB_PREVIOUS:
				dropDown(false);
				return;
			}

			final Event e = new Event();
			e.time = event.time;
			e.detail = event.detail;
			e.doit = event.doit;
			e.character = event.character;
			e.keyCode = event.keyCode;
			notifyListeners(SWT.Traverse, e);
			event.doit = e.doit;
			event.detail = e.detail;
			break;
		}
		case SWT.KeyUp: {
			final Event e = new Event();
			e.time = event.time;
			e.character = event.character;
			e.keyCode = event.keyCode;
			e.stateMask = event.stateMask;
			notifyListeners(SWT.KeyUp, e);
			break;
		}
		case SWT.KeyDown: {
			if (event.character == SWT.ESC) {
				// Escape key cancels popup list
				dropDown(false);
			}
			if ((event.stateMask & SWT.ALT) != 0 && (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN)) {
				dropDown(false);
			}
			if (event.character == SWT.CR) {
				// Enter causes default selection
				dropDown(false);
				final Event e = new Event();
				e.time = event.time;
				e.stateMask = event.stateMask;
				notifyListeners(SWT.DefaultSelection, e);
			}
			// At this point the widget may have been disposed.
			// If so, do not continue.
			if (isDisposed()) {
				break;
			}
			final Event e = new Event();
			e.time = event.time;
			e.character = event.character;
			e.keyCode = event.keyCode;
			e.stateMask = event.stateMask;
			notifyListeners(SWT.KeyDown, e);
			break;

		}
		}
	}

//	addListener(SWT.MouseWheel, new Listener() {
//		public void handleEvent(final Event event) {
//
//			if (event.count < 0) {
//				event.keyCode = SWT.ARROW_RIGHT;
//			} else {
//				event.keyCode = SWT.ARROW_LEFT;
//			}
//
//			/*
//			 * set focus when the mouse is over the chart and the mousewheel is scrolled,
//			 */
//			if (isFocusControl() == false) {
//				forceFocus();
//			}
//
//			fChartComponents.handleLeftRightEvent(event);
//			/*
//			 * prevent scrolling the scrollbar, scrolling is done by the chart itself
//			 */
//			event.doit = false;
//
//		}
//	});

	void onTextEvent(final Event event) {

		// simulate key up/down with the mouse wheel
		if (event.type == SWT.MouseWheel) {
			event.type = SWT.KeyDown;
			if (event.count < 0) {
				event.keyCode = SWT.ARROW_DOWN;
			} else {
				event.keyCode = SWT.ARROW_UP;
			}
		}

		switch (event.type) {
		case SWT.FocusIn: {
			handleFocus(SWT.FocusIn);
			break;
		}

		case SWT.KeyDown: {
//			if (event.character == SWT.CR) {
//				dropDown(false);
//				Event e = new Event();
//				e.time = event.time;
//				e.stateMask = event.stateMask;
//				notifyListeners(SWT.DefaultSelection, e);
//			}

			if (event.character == SWT.CR) {
				final boolean dropped = isDropped();
				if (!dropped) {
					setFocus();
				}
				dropDown(!dropped);
				break;
			}

			//At this point the widget may have been disposed. If so, do not continue.
			if (isDisposed()) {
				break;
			}

			if (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN) {

				event.doit = false;

				if ((event.stateMask & SWT.ALT) != 0) {
					final boolean dropped = isDropped();
					if (!dropped) {
						setFocus();
					}
					dropDown(!dropped);
					break;
				}

				final int oldIndex = getSelectionIndex();
				if (event.keyCode == SWT.ARROW_UP) {
					select(Math.max(oldIndex - 1, 0));
				} else {
					select(Math.min(oldIndex + 1, getItemCount() - 1));
				}

				if (oldIndex != getSelectionIndex()) {
					final Event e = new Event();
					e.time = event.time;
					e.stateMask = event.stateMask;
					notifyListeners(SWT.Selection, e);
				}
				//At this point the widget may have been disposed.
				// If so, do not continue.
				if (isDisposed()) {
					break;
				}
			}

			// Further work : Need to add support for incremental search in 
			// pop up list as characters typed in text widget

			final Event e = new Event();
			e.time = event.time;
			e.character = event.character;
			e.keyCode = event.keyCode;
			e.stateMask = event.stateMask;
			notifyListeners(SWT.KeyDown, e);
			break;
		}

		case SWT.KeyUp: {
			final Event e = new Event();
			e.time = event.time;
			e.character = event.character;
			e.keyCode = event.keyCode;
			e.stateMask = event.stateMask;
			notifyListeners(SWT.KeyUp, e);
			break;
		}
		case SWT.Modify: {
			table.deselectAll();
			final Event e = new Event();
			e.time = event.time;
			notifyListeners(SWT.Modify, e);
			break;
		}
		case SWT.MouseDown: {
			if (event.button != 1) {
				return;
			}

			final boolean dropped = isDropped();
			if (!dropped) {
//				setFocus();
			}
			dropDown(!dropped);
			break;
		}
		case SWT.MouseUp: {
			if (event.button != 1) {
				return;
			}
			break;
		}

		case SWT.Traverse: {
			switch (event.detail) {
			case SWT.TRAVERSE_RETURN:
			case SWT.TRAVERSE_ARROW_PREVIOUS:
			case SWT.TRAVERSE_ARROW_NEXT:
				// The enter causes default selection and
				// the arrow keys are used to manipulate the list contents so
				// do not use them for traversal.
				event.doit = false;
				break;
			}

			final Event e = new Event();
			e.time = event.time;
			e.detail = event.detail;
			e.doit = event.doit;
			e.character = event.character;
			e.keyCode = event.keyCode;
			notifyListeners(SWT.Traverse, e);
//			event.doit = e.doit;
			event.doit = true;
			event.detail = e.detail;
			break;
		}
		}
	}

	@Override
	public void redraw() {
		super.redraw();
		fLabel.redraw();
		arrow.redraw();
		if (popup.isVisible()) {
			table.redraw();
		}
	}

	@Override
	public void redraw(final int x, final int y, final int width, final int height, final boolean all) {
		super.redraw(x, y, width, height, true);
	}

	/**
	 * Removes the item from the receiver's list at the given zero-relative index.
	 * 
	 * @param index
	 *            the index for the item
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of
	 *                elements in the list minus 1 (inclusive)</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public void remove(final int index) {
		checkWidget();
		table.remove(index);
	}

	/**
	 * Removes the items from the receiver's list which are between the given zero-relative start
	 * and end indices (inclusive).
	 * 
	 * @param start
	 *            the start of the range
	 * @param end
	 *            the end of the range
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_INVALID_RANGE - if either the start or end are not between 0 and the
	 *                number of elements in the list minus 1 (inclusive)</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public void remove(final int start, final int end) {
		checkWidget();
		table.remove(start, end);
	}

	/**
	 * Searches the receiver's list starting at the first item until an item is found that is equal
	 * to the argument, and removes that item from the list.
	 * 
	 * @param string
	 *            the item to remove
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the string is null</li>
	 *                <li>ERROR_INVALID_ARGUMENT - if the string is not found in the list</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public void remove(final String string) {
		checkWidget();
		if (string == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		int index = -1;
		for (int i = 0, n = table.getItemCount(); i < n; i++) {
			if (table.getItem(i).getText().equals(string)) {
				index = i;
				break;
			}
		}
		remove(index);
	}

	/**
	 * Removes all of the items from the receiver's list and clear the contents of receiver's text
	 * field.
	 * <p>
	 * 
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li> <li>
	 *                ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
	 *                receiver</li>
	 *                </ul>
	 */
	public void removeAll() {
		checkWidget();
		fLabel.setText(""); //$NON-NLS-1$
		table.removeAll();
	}

	/**
	 * Removes the listener from the collection of listeners who will be notified when the
	 * receiver's text is modified.
	 * 
	 * @param listener
	 *            the listener which should no longer be notified
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li> <li>
	 *                ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
	 *                receiver</li>
	 *                </ul>
	 * @see ModifyListener
	 * @see #addModifyListener
	 */
	public void removeModifyListener(final ModifyListener listener) {
		checkWidget();
		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		removeListener(SWT.Modify, listener);
	}

	/**
	 * Removes the listener from the collection of listeners who will be notified when the
	 * receiver's selection changes.
	 * 
	 * @param listener
	 *            the listener which should no longer be notified
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li> <li>
	 *                ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
	 *                receiver</li>
	 *                </ul>
	 * @see SelectionListener
	 * @see #addSelectionListener
	 */
	public void removeSelectionListener(final SelectionListener listener) {
		checkWidget();
		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		removeListener(SWT.Selection, listener);
		removeListener(SWT.DefaultSelection, listener);
	}

	/**
	 * Selects the item at the given zero-relative index in the receiver's list. If the item at the
	 * index was already selected, it remains selected. Indices that are out of range are ignored.
	 * 
	 * @param index
	 *            the index of the item to select
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li> <li>
	 *                ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
	 *                receiver</li>
	 *                </ul>
	 */
	public void select(final int index) {

		checkWidget();

		if (index == -1) {
			table.deselectAll();
			fLabel.setText(""); //$NON-NLS-1$
			return;
		}

		if (0 <= index && index < table.getItemCount()) {
			if (index != getSelectionIndex()) {

				final TableItem tableItem = table.getItem(index);

				fLabel.setText(tableItem.getText());

				final Image tableImage = tableItem.getImage();
				if (tableImage != null && tableImage.isDisposed() == false) {
					fLabel.setImage(tableImage);
				}

//				text.selectAll();
				table.setSelection(index);
				table.showSelection();
			}
		}
	}

	@Override
	public void setBackground(final Color color) {
		super.setBackground(color);
		background = color;
		if (fLabel != null) {
			fLabel.setBackground(color);
		}
		if (table != null) {
			table.setBackground(color);
		}
		if (arrow != null) {
			arrow.setBackground(color);
		}
	}

	/**
	 * Sets the editable state.
	 * 
	 * @param editable
	 *            the new editable state
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li> <li>
	 *                ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
	 *                receiver</li>
	 *                </ul>
	 * @since 3.0
	 */
	public void setEditable(final boolean editable) {
		checkWidget();
//		text.setEditable(editable);
	}

	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);
		if (popup != null) {
			popup.setVisible(false);
		}
		if (fLabel != null) {
			fLabel.setEnabled(enabled);
		}
		if (arrow != null) {
			arrow.setEnabled(enabled);
		}
	}

	@Override
	public boolean setFocus() {
		checkWidget();
		return fLabel.setFocus();
	}

	@Override
	public void setFont(final Font font) {
		super.setFont(font);
		this.font = font;
		fLabel.setFont(font);
		table.setFont(font);
		internalLayout(true);
	}

	@Override
	public void setForeground(final Color color) {
		super.setForeground(color);
		foreground = color;
		if (fLabel != null) {
			fLabel.setForeground(color);
		}
		if (table != null) {
			table.setForeground(color);
		}
		if (arrow != null) {
			arrow.setForeground(color);
		}
	}

	/**
	 * Sets the text of the item in the receiver's list at the given zero-relative index to the
	 * string argument. This is equivalent to <code>remove</code>'ing the old item at the index, and
	 * then <code>add</code>'ing the new item at that index.
	 * 
	 * @param index
	 *            the index for the item
	 * @param string
	 *            the new text for the item
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of
	 *                elements in the list minus 1 (inclusive)</li> <li>ERROR_NULL_ARGUMENT - if the
	 *                string is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li> <li>
	 *                ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
	 *                receiver</li>
	 *                </ul>
	 */
	public void setItem(final int index, final String string, final Image image) {
		checkWidget();
		remove(index);
		add(string, image, index);
	}

	/**
	 * Sets the receiver's list to be the given array of items.
	 * 
	 * @param items
	 *            the array of items
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the items array is null</li> <li>
	 *                ERROR_INVALID_ARGUMENT - if an item in the items array is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li> <li>
	 *                ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
	 *                receiver</li>
	 *                </ul>
	 */
	public void setItems(final String[] items) {
		checkWidget();
		this.table.removeAll();
		for (final String item : items) {
			add(item, null);
		}
//		if (!text.getEditable()) {
//			text.setText(""); //$NON-NLS-1$
//		}
	}

	/**
	 * Sets the layout which is associated with the receiver to be the argument which may be null.
	 * <p>
	 * Note : No Layout can be set on this Control because it already manages the size and position
	 * of its children.
	 * </p>
	 * 
	 * @param layout
	 *            the receiver's new layout or null
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	@Override
	public void setLayout(final Layout layout) {
		checkWidget();
		return;
	}

	/**
	 * Sets the selection in the receiver's text field to the range specified by the argument whose
	 * x coordinate is the start of the selection and whose y coordinate is the end of the
	 * selection.
	 * 
	 * @param selection
	 *            a point representing the new selection start and end
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the point is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public void setSelection(final Point selection) {
		checkWidget();
		if (selection == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
//		text.setSelection(selection.x, selection.y);
	}

	/**
	 * Sets the contents of the receiver's text field to the given string.
	 * <p>
	 * Note: The text field in a <code>Combo</code> is typically only capable of displaying a single
	 * line of text. Thus, setting the text to a string containing line breaks or other special
	 * characters will probably cause it to display incorrectly.
	 * </p>
	 * 
	 * @param string
	 *            the new text
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the string is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public void setText(final String string) {
		checkWidget();
		if (string == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		int index = -1;
		for (int i = 0, n = table.getItemCount(); i < n; i++) {
			if (table.getItem(i).getText().equals(string)) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			table.deselectAll();
			fLabel.setText(string);
			return;
		}

		fLabel.setText(string);
		table.setSelection(index);
		table.showSelection();
	}

//	/**
//	 * Sets the maximum number of characters that the receiver's text field is capable of holding to
//	 * be the argument.
//	 * 
//	 * @param limit
//	 *        new text limit
//	 * @exception IllegalArgumentException
//	 *            <ul>
//	 *            <li>ERROR_CANNOT_BE_ZERO - if the limit is zero</li>
//	 *            </ul>
//	 * @exception SWTException
//	 *            <ul>
//	 *            <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
//	 *            <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
//	 *            receiver</li>
//	 *            </ul>
//	 */
//	public void setTextLimit(int limit) {
//		checkWidget();
//		text.setTextLimit(limit);
//	}

	@Override
	public void setToolTipText(final String string) {
		checkWidget();
		super.setToolTipText(string);
		arrow.setToolTipText(string);
		fLabel.setToolTipText(string);
	}

	@Override
	public void setVisible(final boolean visible) {
		super.setVisible(visible);
		if (!visible) {
			popup.setVisible(false);
		}
	}

	/**
	 * Sets the number of items that are visible in the drop down portion of the receiver's list.
	 * 
	 * @param count
	 *            the new number of items to be visible
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @since 3.0
	 */
	public void setVisibleItemCount(final int count) {
		checkWidget();
		if (count < 0) {
			return;
		}
		visibleItemCount = count;
	}

	String stripMnemonic(final String string) {
		int index = 0;
		final int length = string.length();
		do {
			while ((index < length) && (string.charAt(index) != '&')) {
				index++;
			}
			if (++index >= length) {
				return string;
			}
			if (string.charAt(index) != '&') {
				return string.substring(0, index - 1) + string.substring(index, length);
			}
			index++;
		}
		while (index < length);
		return string;
	}
}
