package net.tourbook.ui.views.calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.joda.time.DateTime;

public class CalendarGraph extends Canvas {

	private Composite _parent;

	private Display		_display	= Display.getCurrent();

	private Color		_white		= _display.getSystemColor(SWT.COLOR_WHITE);
	private Color		_black		= _display.getSystemColor(SWT.COLOR_BLACK);
	private Color		_gray		= _display.getSystemColor(SWT.COLOR_GRAY);

	CalendarGraph(final CalendarForm calendarForm, final Composite parent, final int style) {

		super(parent, style);

		_parent = parent;

		addListener();

	}
	
	private void addListener() {

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent event) {
				drawCalendar(event.gc);
			}
		});

		addFocusListener(new FocusListener() {

			public void focusGained(final FocusEvent e) {
				redraw();
			}

			public void focusLost(final FocusEvent e) {
				redraw();
			}
		});

		addListener(SWT.Traverse, new Listener() {
			public void handleEvent(final Event event) {

				switch (event.detail) {
				case SWT.TRAVERSE_RETURN:
				case SWT.TRAVERSE_ESCAPE:
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
				case SWT.TRAVERSE_PAGE_NEXT:
				case SWT.TRAVERSE_PAGE_PREVIOUS:
					event.doit = true;
					break;
				}
			}
		});

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				// TODO
			}
		});

	}

	private void drawCalendar (final GC gc) {
		
		final Rectangle area = getClientArea();
		gc.setBackground(_white);
		gc.setForeground(_black);
		gc.fillRectangle(area);

		final int X = getSize().x - 1;
		final int Y = getSize().y - 1;

		final int numCols = 9;
		final int numRows = 6;

		final int header = ((Y / numRows) / 4); // header is 1/4 cell height - we display at most 3 workouts per day

		final float dX = (float) X / (float) numCols;
		final float dY = (float) Y / (float) numRows;

		// first draw the horizontal lines
		gc.setBackground(_white);
		gc.setForeground(_display.getSystemColor(SWT.COLOR_GRAY));
		for (int i = 0; i <= numRows; i++) {
			gc.drawLine(0, (int) (i * dY), X, (int) (i * dY));
		}

		// than draw the header boxes
		DateTime today = new DateTime();
		final int offsetX = (int) dX + 1;
		final int offsetY = 0 + 1;
		final int width = (int) (dX * (numCols - 2));
//		gc.setForeground(_display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
//		gc.setBackground(_display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
//		gc.setForeground(_display.getSystemColor(SWT.COLOR_GRAY));
//		gc.setBackground(_display.getSystemColor(SWT.COLOR_WHITE));
		gc.setForeground(_gray);
		gc.setBackground(_white);
		for (int i = 0; i < numRows; i++) {
			// attempt to build something looking 3D...
			final int oy = offsetY + (int) (i * dY);
			gc.fillGradientRectangle(offsetX, oy,          width,      (header / 4), true);
			gc.fillGradientRectangle(offsetX, oy + header, width, -3 * (header / 4), true);
			gc.setForeground(_black);
			for (int j = 0; j < 7; j++) {
				gc.drawText(today.toString("dd, MMM"), 5 + offsetX + (int) (j * dX), oy, true);
				today = today.plusDays(1);
			}
			gc.setForeground(_gray);
		}

		// and finally the vertical lines
		gc.setForeground(_display.getSystemColor(SWT.COLOR_GRAY));
		for (int i = 0; i <= numCols; i++) {
			gc.drawLine((int) (i * dX), 0, (int) (i * dX), Y);
		}
		
	}

}
