package net.tourbook.ui.views.calendar;

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

public class CalendarGraph extends Canvas {

	private Composite _parent;

	private static IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();
	private Display		_display	= Display.getCurrent();

	private Color		_white		= _display.getSystemColor(SWT.COLOR_WHITE);
	private Color		_black		= _display.getSystemColor(SWT.COLOR_BLACK);
	private Color		_gray		= _display.getSystemColor(SWT.COLOR_GRAY);

	private ArrayList<RGB>			_rgbBright;
	private ArrayList<RGB>			_rgbDark;
	private ArrayList<RGB>			_rgbLine;

	private DateTime				_dt			= new DateTime();
	private int						_month		= _dt.getMonthOfYear();
	private int						_year		= _dt.getYear();
	

	CalendarGraph(final CalendarForm calendarForm, final Composite parent, final int style) {

		super(parent, style);

		_parent = parent;

		_rgbBright = new ArrayList<RGB>();
		_rgbDark = new ArrayList<RGB>();
		_rgbLine = new ArrayList<RGB>();

		/*
		 * color index 1...n+1: tour type colors
		 */
		final ArrayList<TourType> tourTypes = TourDatabase.getActiveTourTypes();
		for (final TourType tourType : tourTypes) {
			_rgbBright.add(tourType.getRGBBright());
			_rgbDark.add(tourType.getRGBDark());
			_rgbLine.add(tourType.getRGBLine());
		}

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

	public void back() {
		// _dt = _dt.minusMonths(1);
		_month--;
		if (_month < 1) {
			_month = 12;
			_year--;
		}
		redraw();
	}

	private void drawCalendar (final GC gc) {
		
		int firstDayOfWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);
		firstDayOfWeek--; // the prefStore is using Calendar Constants (SO=1 ... SA=7) we are using Joda (MO=1 .. SO=7)
		if (firstDayOfWeek < 1) {
			firstDayOfWeek = 7;
		}
		
		final CalendarTourDataProvider dataProvider = CalendarTourDataProvider.getInstance();
		CalendarTourData data;

		_dt = _dt.withYear(_year);
		_dt = _dt.withMonthOfYear(_month);
		// scroll to first day of the week containing the first day of this month
		_dt = _dt.withDayOfMonth(1);
		_dt = _dt.withDayOfWeek(firstDayOfWeek);

		final Font normalFont = gc.getFont();
		final FontData fd[] = normalFont.getFontData();
		fd[0].setStyle(SWT.BOLD);
		final Font boldFont = new Font(_display, fd[0]);

		final int fontHeight = fd[0].getHeight() + 2;

		final Rectangle area = getClientArea();
		gc.setBackground(_white);
		gc.setForeground(_black);
		gc.fillRectangle(area);

		final int XX = getSize().x - 1;
		final int YY = getSize().y - 1;

		final int numCols = 9;
		final int numRows = 5;

		final int header = (YY / 4 / numRows); // header is 1/4 cell height - we display at most 3 workouts per day

		final float dX = (float) XX / (float) numCols;
		final float dY = (float) YY / (float) numRows;

		// first draw the horizontal lines
		gc.setBackground(_white);
		gc.setForeground(_display.getSystemColor(SWT.COLOR_GRAY));
		for (int i = 0; i <= numRows; i++) {
			gc.drawLine(0, (int) (i * dY), XX, (int) (i * dY));
		}

		// than draw the header boxes
		final int weekWidth = (int) (dX * (numCols - 2)); // complete width of one week
		gc.setBackground(_white);
		gc.setForeground(_gray);
		for (int i = 0; i < numRows; i++) {
			final int Y1 = (int) ((i + 0) * dY);
			final int Y2 = (int) ((i + 1) * dY);
			gc.setForeground(_gray);
			gc.setBackground(_white);
			// -------- Day Header Box (one Week) --------
			// attempt to build something looking 3D...
			// gc.fillGradientRectangle(1 + (int) dX, Y1, weekWidth, (header / 4), true);
			// gc.fillGradientRectangle(1 + (int) dX, Y1 + header, weekWidth, -3 * (header / 4), true);
			gc.fillGradientRectangle(1 + (int) dX, Y1, weekWidth, header, true);
			for (int j = 0; j < 7; j++) {
				final int X1 = (int) ((j + 1) * dX);
				final int X2 = (int) ((j + 2) * dX);
				final int weekDay = _dt.getDayOfWeek();
				if (weekDay == DateTimeConstants.SATURDAY || weekDay == DateTimeConstants.SUNDAY) {
					gc.setForeground(_display.getSystemColor(SWT.COLOR_BLUE));
				} else {
					gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_GRAY));
				}
				gc.setBackground(_white);
				gc.setFont(boldFont);
				// -------- Day Header Box Text (one Day) --------
				if (_month != _dt.getMonthOfYear()) {
					gc.setBackground(_display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
					gc.fillRectangle(X1, Y1, (X2 - X1), (Y2 - Y1));
				}
				gc.drawText(_dt.toString("dd. MMM yy"), X1 + 5, Y1, true);
				_dt = _dt.plusDays(1);
				data = dataProvider.getTourTimeData(_dt.getYear(), _dt.getMonthOfYear(), _dt.getDayOfMonth() - 1);
				gc.setFont(normalFont);
				int y = Y2;
				final int dy = (Y2 - Y1) / 4;
				for (int k = 0; k < data.tourIds.length; k++) {
					// TODO create each color only once (private array( and dispose
					gc.setBackground(new Color(_display, _rgbBright.get(data.typeColorIndex[k])));
					gc.setForeground(new Color(_display, _rgbDark.get(data.typeColorIndex[k])));
					gc.fillGradientRectangle(X1, y, (X2 - X1), -dy, false);
					final Color lineColor = new Color(_display, _rgbLine.get(data.typeColorIndex[k]));
					gc.setForeground(lineColor);
					gc.drawRectangle(X1 + 1, y, (X2 - X1 - 2), -dy - 2);
					String title = data.tourTitle.get(k);
					title =  title == null  ?  "No Title" : title;
					// gc.setForeground(_black);
					// gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_GRAY));
					gc.setForeground(lineColor);
					gc.setClipping(X1 + 2, y, (X2 - X1 - 4), -dy - 4);
					gc.drawText(title, X1 + 4, y - dy + 2, true);
					final Rectangle rec = null;
					gc.setClipping(rec);
					y -= dy + 1;

				}
			}
		}
		gc.setFont(normalFont);

		// and finally the vertical lines
		gc.setForeground(_display.getSystemColor(SWT.COLOR_GRAY));
		for (int i = 0; i <= numCols; i++) {
			gc.drawLine((int) (i * dX), 0, (int) (i * dX), YY);
		}
		
		boldFont.dispose();

	}

	public void forward() {
		// _dt = _dt.plusMonths(1);
		_month++;
		if (_month > 12) {
			_month = 1;
			_year++;
		}
		redraw();
	}

	public void setDate(final DateTime date) {
		// TODO Auto-generated method stub
	}

}
