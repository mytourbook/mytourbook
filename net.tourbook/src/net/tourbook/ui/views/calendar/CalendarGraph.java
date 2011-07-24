package net.tourbook.ui.views.calendar;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

public class CalendarGraph extends Canvas {

	private Composite							_parent;

	private static IPreferenceStore				_prefStore			= TourbookPlugin.getDefault().getPreferenceStore();
	private Display								_display			= Display.getCurrent();

	private Color								_black				= _display.getSystemColor(SWT.COLOR_BLACK);
	private Color								_gray				= _display.getSystemColor(SWT.COLOR_GRAY);
	private Color								_white				= _display.getSystemColor(SWT.COLOR_WHITE);
	private Color								_red				= _display.getSystemColor(SWT.COLOR_RED);
	private Color								_green				= _display.getSystemColor(SWT.COLOR_GREEN);
	private Color								_blue				= _display.getSystemColor(SWT.COLOR_BLUE);

	private NavigationStyle						_navigationStyle	= NavigationStyle.PHYSICAL;

	private ArrayList<RGB>						_rgbBright;
	private ArrayList<RGB>						_rgbDark;
	private ArrayList<RGB>						_rgbLine;

	private DateTime							_dt					= new DateTime();
	private int									_numWeeksDisplayed	= 5;

	private List<ObjectLocation>				_tourFocus;
	private List<ObjectLocation>				_dayFocus;

	private Image								_image				= null;
	private Image								_highlight			= null;
	private boolean								_highlightRemoved	= false;
	private boolean								_highlightChanged	= false;
	private boolean								_graphClean			= false;

	private Selection							_noItem					= new Selection(
																				new Long(-1),
																				SelectionType.NONE);
	private Selection							_selectedItem			= _noItem;
	private Selection							_highlightedItem		= _noItem;
	private Selection							_lastSelection			= _noItem;

	final private Rectangle						_nullRec			= null;

	private CalendarTourDataProvider			_dataProvider;
	private CalendarYearMonthContributionItem	_calendarYearMonthContributor;

	private ListenerList						_selectionProvider	= new ListenerList();

	private int									_scrollBarShift;
	private int									_scrollBarLastSelection;
	private boolean								_scrollDebug = false;

	private int									_numberOfToursPerDay	= 3;
	private boolean								_dynamicTourFieldSize	= true;

	final static private long					_WEEK_MILLIS			= (1000 * 60 * 60 * 24 * 7);
	final static private int					_MIN_SCROLLABLE_WEEKS	= 12;

	private class Day {

		private long	dayId;

		Day(final DateTime date) {
			this.dayId = (long) date.getYear() * 1000 + date.getDayOfYear();
		}

		Day(final long dayId) {
			this.dayId = dayId;
		}

	};

	enum NavigationStyle {
		LOGICAL, PHYSICAL
	}

	private class ObjectLocation {

		private Rectangle	r;
		private Object		o;
		private long		id;

		ObjectLocation(final Rectangle r, final long id, final Object o) {
			this.o = o;
			this.id = id;
			this.r = r;
		}

	}

	class Selection {

		Long			id;
		SelectionType	type;

		Selection(final Long id, final SelectionType type) {
			this.id = id;
			this.type = type;
		}

		@Override
		public boolean equals(final Object o) {
			if (o instanceof Selection) {
				return (((Selection) o).id == this.id) && (((Selection) o).type == this.type);
			}
			return super.equals(o);
		}

		boolean isTour() {
			return (id != 0 && type == SelectionType.TOUR);
		}

	}

	enum SelectionType {
		YEAR, MONHT, WEEK, DAY, TOUR, NONE
	}

	CalendarGraph(final Composite parent, final int style) {

		super(parent, style);

		_parent = parent;

		_dataProvider = CalendarTourDataProvider.getInstance();

		_rgbBright = new ArrayList<RGB>();
		_rgbDark = new ArrayList<RGB>();
		_rgbLine = new ArrayList<RGB>();

		/*
		 * color index 1...n+1: tour type colors
		 */
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		for (final TourType tourType : tourTypes) {
			_rgbBright.add(tourType.getRGBBright());
			_rgbDark.add(tourType.getRGBDark());
			_rgbLine.add(tourType.getRGBLine());
		}

//		final DateTime dt = new DateTime();
//
//		gotoYear(dt.getYear());
//		gotoMonth(dt.getMonthOfYear());

		addListener();

	}

	private void addListener() {

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent event) {
				drawCalendar(event.gc);
			}
		});

		addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				// TODO Auto-generated method stub

			}

			public void mouseDown(final MouseEvent e) {
				onMouseMove(e);
			}

			@Override
			public void mouseUp(final MouseEvent e) {
				// onMouseMove(e);
			}

		});

		addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(final MouseEvent e) {
				onMouseMove(e);
			}
		});

		addFocusListener(new FocusListener() {

			public void focusGained(final FocusEvent e) {
				// System.out.println("Focus gained");
				// redraw();
			}

			public void focusLost(final FocusEvent e) {
				// System.out.println("Focus lost");
				// redraw();
			}
		});

		addListener(SWT.Traverse, new Listener() {
			public void handleEvent(final Event event) {
				switch (event.detail) {
				case SWT.TRAVERSE_PAGE_NEXT:
				case SWT.TRAVERSE_PAGE_PREVIOUS:
					// case SWT.TRAVERSE_RETURN:
				case SWT.TRAVERSE_ESCAPE:
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
					event.doit = true;
					break;
				}
			}
		});

		addListener(SWT.KeyDown, new Listener() {
			public void handleEvent(final Event event) {
				switch (event.keyCode) {
				case SWT.ARROW_LEFT:
					switch (_navigationStyle) {
					case PHYSICAL:
						gotoTourOtherWeekday(-1);
						break;
					case LOGICAL:
						gotoPrevTour();
						break;
					}
					break;
				case SWT.ARROW_RIGHT:
					switch (_navigationStyle) {
					case PHYSICAL:
						gotoTourOtherWeekday(+1);
						break;
					case LOGICAL:
						gotoNextTour();
						break;
					}
					break;
				case SWT.ARROW_UP:
					switch (_navigationStyle) {
					case PHYSICAL:
						gotoTourSameWeekday(-1);
						break;
					case LOGICAL:
						gotoPrevWeek();
						break;
					}
					break;
				case SWT.ARROW_DOWN:
					switch (_navigationStyle) {
					case PHYSICAL:
						gotoTourSameWeekday(+1);
						break;
					case LOGICAL:
						gotoNextWeek();
						break;
					}
					break;
				case SWT.PAGE_DOWN:
					gotoNextScreen();
					break;
				case SWT.PAGE_UP:
					gotoPrevScreen();
					break;
				case SWT.HOME:
					gotoToday();
					break;
				case SWT.END:
					gotoFirstTour();
					break;
				}
			}
		});

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				// TODO
			}
		});

		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent event) {
				_graphClean = false;
				redraw();
			}
		});

		addFocusListener(new FocusListener() {

			public void focusGained(final FocusEvent e) {
				// redraw();
			}

			public void focusLost(final FocusEvent e) {
				// _highlightRemoved = true;
				// redraw();
			}
		});

		addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(final MouseEvent e) {
				// forceFocus();
			}

			public void mouseExit(final MouseEvent e) {
				_highlightRemoved = true;
				redraw();
			}

			public void mouseHover(final MouseEvent e) {}
		});

		final ScrollBar sb = _parent.getVerticalBar();
		sb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onScroll(event);
			}
		});

	}

	void addSelectionProvider(final ICalendarSelectionProvider provider) {
		_selectionProvider.add(provider);
	}

	private void drawCalendar(GC gc) {

		final int dayLabelXOffset = 4;

		final int XX = getSize().x;
		final int YY = getSize().y;

//		System.out.println(_graphClean ? "clean!" : "NOT clean!");
//		System.out.println(_highlightChanged ? "HL changed!" : "HL NOT changed");
//		System.out.println(_highlightRemoved ? "HL removed!" : "HL NOT removed");
//		System.out.println("-----------");

		if (_graphClean && _highlightRemoved && _image != null) {
			gc.drawImage(_image, 0, 0);
			_highlightedItem = _noItem;
			_highlightRemoved = false;
			return;
		}

		if (_graphClean && _highlightChanged && _image != null) {
			final GC oldGc = gc;
			_highlight = new Image(getDisplay(), XX, YY);
			gc = new GC(_highlight);
			gc.drawImage(_image, 0, 0);
			drawHighLight(gc);
			gc.dispose();
			oldGc.drawImage(_highlight, 0, 0);
			_highlightChanged = false;
			return;
		}

		if (_image != null && !_image.isDisposed()) {
			_image.dispose();
		}

		DateTime date = new DateTime(_dt);
		_image = new Image(getDisplay(), XX, YY);

		// update month/year dropdown box
		// look at the 1st day of the week after the first day displayed because if we go to
		// a specific month we ensure that the first day of the month is displayed in
		// the first line, meaning the first day in calendar normally contains a day
		// of the *previous* month
		if (_calendarYearMonthContributor.getSelectedYear() != _dt.plusDays(7).getYear()) {
			_calendarYearMonthContributor.selectYear(_dt.getYear());
		}
		if (_calendarYearMonthContributor.getSelectedMonth() != _dt.plusDays(7).getMonthOfYear()) {
			_calendarYearMonthContributor.selectMonth(_dt.getMonthOfYear());
		}

		final GC oldGc = gc;
		gc = new GC(_image);

		final int numCols = 9; // one col left and right of the week + 7 week days
		final int numRows = _numWeeksDisplayed; // number of weeks per moth displayed

		// final Color alternate = new Color(gc.getDevice(), 0xf5, 0xf5, 0xf5);
		final Color alternate = new Color(gc.getDevice(), 0xf0, 0xf0, 0xf0);

		_tourFocus = new ArrayList<ObjectLocation>();
		_dayFocus = new ArrayList<ObjectLocation>();

		CalendarTourData[] data;

		final Font normalFont = gc.getFont();
		final FontData fd[] = normalFont.getFontData();
		fd[0].setStyle(SWT.BOLD);
		final Font boldFont = new Font(_display, fd[0]);

		final Rectangle area = getClientArea();
		gc.setBackground(_white);
		gc.setForeground(_black);
		gc.fillRectangle(area);

		final float dX = (float) XX / (float) numCols;
		final float dY = (float) YY / (float) numRows;

		// first draw the horizontal lines
		gc.setBackground(_white);
		gc.setForeground(_gray);
		for (int i = 0; i <= numRows; i++) {
			gc.drawLine(0, (int) (i * dY), XX, (int) (i * dY));
		}

//		final Rectangle selectedRec = null;
//		final CalendarTourData selectedTour = null;
//		final boolean doSelection = false;

		final long todayDayId = (new Day(new DateTime())).dayId;

		gc.setFont(boldFont);
		final Point[] headerSizes = {
				gc.stringExtent("22. Dec 99"),
				gc.stringExtent("22. Dec"),
				gc.stringExtent("22") };
		gc.setFont(normalFont);

		final String[] headerFormats = { "dd. MMM yy", "dd. MMM", "dd" };
		String headerFormat = "";

		// Find a format for the day header which fits into the rectangle available;
		int g = 0;
		while (g < headerSizes.length && headerSizes[g].x > (dX - dayLabelXOffset)) {
			g++;
		}
		g = Math.min(g, headerSizes.length - 1); // if the cell is smaller than the shortest format (no index 'g' was found) we use the shortest format and relay on clipping
		// if (headerSizes[g].y < dY) {
		// 	headerFormat = headerFormats[g];
		// }
		headerFormat = headerFormats[g];
		final int dayLabelWidht = headerSizes[g].x;
		final int dayLabelHeight = headerSizes[g].y;

		// Weeks
		for (int i = 0; i < numRows; i++) {
			final int Y1 = (int) (i * dY);
			final int Y2 = (int) ((i + 1) * dY);

			// Days per week
			long dayId = (new Day(date)).dayId; // we use simple ids
			for (int j = 1; j < 8; j++) { // col 0 is for weekinfo, the week itself starts at col 1
				final int X1 = (int) (j * dX);
				final int X2 = (int) ((j + 1) * dX);
				final Rectangle dayRec = new Rectangle(X1, Y1, (X2 - X1), (Y2 - Y1));
				final Day day = new Day(dayId);
				_dayFocus.add(new ObjectLocation(dayRec, dayId, day));
				dayId = day.dayId + 1;
				final int weekDay = date.getDayOfWeek();
				
				gc.setBackground(_white);

				// Day background rectangle
				if ((date.getMonthOfYear() % 2) == 1) {
					gc.setBackground(alternate);
					gc.fillRectangle(dayRec);
				}

				// Day header box
				gc.setForeground(_gray);
				gc.fillGradientRectangle(X1, Y1, dayRec.width + 1, dayLabelHeight, true); // no clue why I've to add 1 to the width, looks like a bug on Linux and does not hurt as we overwrite with the vertial line at the end anyway

				// Day header label
				gc.setFont(boldFont);
				if (day.dayId == todayDayId) {
					gc.setForeground(_blue);
				} else if (weekDay == DateTimeConstants.SATURDAY || weekDay == DateTimeConstants.SUNDAY) {
					gc.setForeground(_red);
				} else {
					gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_GRAY));
				}
				gc.setClipping(X1, Y1, dayRec.width, dayLabelHeight); // this clipping should only kick in if shortest label format is still longer than the cell width
				gc.drawText(date.toString(headerFormat), X2 - dayLabelWidht - dayLabelXOffset, Y1, true);
				data = _dataProvider.getCalendarDayData(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth());
				gc.setFont(normalFont);
				gc.setClipping(_nullRec);

				drawDayTours(gc, data, new Rectangle(dayRec.x, dayRec.y + dayLabelHeight, dayRec.width, dayRec.height
						- dayLabelHeight));

				date = date.plusDays(1);
			}
		}
		gc.setFont(normalFont);

		// and finally the vertical lines
		gc.setForeground(_display.getSystemColor(SWT.COLOR_GRAY));
		for (int i = 0; i <= numCols; i++) {
			gc.drawLine((int) (i * dX), 0, (int) (i * dX), YY);
		}

		drawSelection(gc);
		if (!_lastSelection.equals(_selectedItem)) {
			fireSelectionEvent(_selectedItem);
			_lastSelection = _selectedItem;
		}

		boldFont.dispose();

		gc.dispose();
		oldGc.drawImage(_image, 0, 0);


		_graphClean = true;

	}

	private void drawDayTours(final GC gc, final CalendarTourData[] data, final Rectangle rec) {

		final int max = _numberOfToursPerDay == 0 ? data.length : _dynamicTourFieldSize ? data.length : Math.min(
				_numberOfToursPerDay,
				data.length);
		for (int i = max - 1; i >= 0; i--) { // morning top, evening button
			int ddy;
			if (_numberOfToursPerDay == 0) {
				ddy = rec.height / data.length;
			} else {
				final int dy = rec.height / _numberOfToursPerDay;
				// narrow the tour fields to fit if more than _numberOfTourPerDay tours
				ddy = _dynamicTourFieldSize ? (data.length <= _numberOfToursPerDay ? dy : (_numberOfToursPerDay * dy)
						/ data.length) : dy;
			}
			final Rectangle tour = new Rectangle(rec.x + 1, rec.y + rec.height - (i + 1) * ddy, (rec.width - 2), ddy - 1);
			final Rectangle focus = new Rectangle(tour.x - 1, tour.y - 1, tour.width + 2, tour.height + 2);
			_tourFocus.add(new ObjectLocation(focus, data[i].tourId, data[i]));
			// TODO create each color only once (private array) and dispose
			gc.setBackground(new Color(_display, _rgbBright.get(data[i].typeColorIndex)));
			gc.setForeground(new Color(_display, _rgbDark.get(data[i].typeColorIndex)));
			gc.fillGradientRectangle(tour.x + 1, tour.y + 1, tour.width - 1, tour.height - 1, false);
			final Color lineColor = new Color(_display, _rgbLine.get(data[i].typeColorIndex));
			gc.setForeground(lineColor);
			gc.drawRectangle(tour);
			String title = data[i].tourTitle;
			title = title == null ? "No Title" : title;
			// gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_GRAY));
			gc.setForeground(lineColor);
			// gc.setForeground(_black);
			gc.setClipping(focus.x + 2, focus.y, focus.width - 4, focus.height - 2);
			gc.drawText(title, tour.x + 2, tour.y, true);
			gc.setClipping(_nullRec);
		}

	}

	private void drawHighLight(final GC gc) {

		List<ObjectLocation> objects;
		if (_highlightedItem.type == SelectionType.TOUR) {
			objects = _tourFocus;
		} else if (_highlightedItem.type == SelectionType.DAY) {
			objects = _dayFocus;
		} else {
			return;
		}

		for (final ObjectLocation ol : objects) {
			if (ol.id == _highlightedItem.id) {
				if (ol.o instanceof CalendarTourData) {
					highlightTour(gc, (CalendarTourData) (ol.o), ol.r);
				} else if (ol.o instanceof Day) {
					// gc.setAlpha(0xd0); // like statistics
					gc.setAlpha(0xa0);
					gc.setBackground(_white);
					gc.setForeground(_gray);
					gc.fillGradientRectangle(ol.r.x - 4, ol.r.y - 4, ol.r.width + 9, ol.r.height + 9, false);
					gc.drawRoundRectangle(ol.r.x - 5, ol.r.y - 5, ol.r.width + 10, ol.r.height + 10, 6, 6);
				}
				return;
			}
		}
	}

	private void drawSelectedDay (final GC gc, final Rectangle r) {
		// gc.setAlpha(0xd0); // like statistics
		//	gc.setAlpha(0xa0);
		gc.setBackground(_blue);
		gc.setForeground(_blue);
		// gc.fillGradientRectangle(r.x - 4, r.y - 4, r.width + 9, r.height + 9, false);
		// gc.drawRoundRectangle(r.x - 5, r.y - 5, r.width + 10, r.height + 10, 6, 6);
		final int oldLw = gc.getLineWidth();
		gc.setLineWidth(4);
		gc.drawRoundRectangle(r.x - 2, r.y - 2, r.width + 5, r.height + 5, 6, 6);
		gc.setLineWidth(oldLw);
	}

	// TODO review
	private void drawSelectedTour(final GC gc, final CalendarTourData data, final Rectangle rec) {

		final Color lineColor = new Color(_display, _rgbLine.get(data.typeColorIndex));

		gc.fillGradientRectangle(rec.x - 4, rec.y - 4, rec.width + 9, rec.height + 9, false);
		gc.setBackground(_red);
		// gc.setBackground(lineColor);
		gc.setForeground(lineColor);
		gc.fillRectangle(rec.x - 4, rec.y - 4, rec.width + 9, rec.height + 9);
		gc.drawRoundRectangle(rec.x - 5, rec.y - 5, rec.width + 10, rec.height + 10, 6, 6);

		gc.setBackground(new Color(_display, _rgbBright.get(data.typeColorIndex)));
		gc.setForeground(new Color(_display, _rgbDark.get(data.typeColorIndex)));

		gc.fillGradientRectangle(rec.x + 1, rec.y + 1, rec.width - 2, rec.height - 2, false);
		gc.setForeground(lineColor);
		gc.drawRectangle(rec.x + 1, rec.y + 1, rec.width - 2, rec.height - 2);

		String title = data.tourTitle;
		gc.setForeground(_black);
		title = title == null ? "No Title" : title;
		gc.setClipping(rec.x + 2, rec.y, rec.width - 4, rec.height - 2);
		gc.drawText(title, rec.x + 3, rec.y + 1, true);
		gc.setClipping(_nullRec);
	}

	private void drawSelection(final GC gc) {

		List<ObjectLocation> objects;
		if (_selectedItem.type == SelectionType.TOUR) {
			objects = _tourFocus;
		} else if (_selectedItem.type == SelectionType.DAY) {
			objects = _dayFocus;
		} else {
			return;
		}

		for (final ObjectLocation ol : objects) {
			if (ol.id == _selectedItem.id) {
				if (ol.o instanceof CalendarTourData) {
					drawSelectedTour(gc, (CalendarTourData) (ol.o), ol.r);
				} else if (ol.o instanceof Day) {
					drawSelectedDay(gc, ol.r);
				}
				return;
			}
		}
	}

	void fireSelectionEvent(final Selection selection) {

		final Object[] listeners = _selectionProvider.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ICalendarSelectionProvider listener = (ICalendarSelectionProvider) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectionChanged(selection);
				}
			});
		}
	}

	void fireSelectionEvent(final SelectionType type, final long id) {

		final Object[] listeners = _selectionProvider.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ICalendarSelectionProvider listener = (ICalendarSelectionProvider) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectionChanged(_selectedItem);
				}
			});
		}
	}

	public DateTime getFirstDay() {
		return _dt;
	}

	private int getFirstDayOfWeek() {

		int firstDayOfWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);
		firstDayOfWeek--; // the prefStore is using Calendar Constants (SO=1 ... SA=7) we are using Joda (MO=1 .. SO=7)
		if (firstDayOfWeek < 1) {
			firstDayOfWeek = 7;
		}
		return firstDayOfWeek;

	}

	public int getNumberOfToursPerDay() {
		return _numberOfToursPerDay;
	}

	public Long getSelectedTour() {
		if (_selectedItem.isTour()) {
			return _selectedItem.id;
		} else {
			return _noItem.id;
		}
	}

	public int getZoom() {
		return _numWeeksDisplayed;
	}

	public void gotoDate(final DateTime dt) {

		_dt = dt;
		_dt = _dt.minusWeeks(_numWeeksDisplayed / 2); // center date on screen
		_dt = _dt.withDayOfWeek(getFirstDayOfWeek()); // set first day to start of week

		_graphClean = false;
		redraw();
		scrollBarUpdate();
	}

	public void gotoFirstTour() {
		_dt = _dataProvider.getFirstDateTime();
		gotoDate(_dt);
	}

	public void gotoMonth(final int month) {

		_dt = _dt.withMonthOfYear(month); // scroll to first day of the week containing the first day of this month
		_dt = _dt.withDayOfMonth(1);
		_dt = _dt.withDayOfWeek(getFirstDayOfWeek()); // set first day to start of week

		_graphClean = false;
		redraw();
		scrollBarUpdate();
	}

	public void gotoNextScreen() {
		_dt = _dt.plusWeeks(_numWeeksDisplayed);
		_graphClean = false;
		redraw();
		scrollBarUpdate();
	}

	public void gotoNextTour() {
		gotoTourOffset(+1);
	}

	public void gotoNextWeek() {
		_dt = _dt.plusWeeks(1);
		redraw();
		scrollBarUpdate();
	}

	public void gotoPrevScreen() {
		_dt = _dt.minusWeeks(_numWeeksDisplayed);
		_graphClean = false;
		redraw();
		scrollBarUpdate();
	}

	public void gotoPrevTour() {
		gotoTourOffset(-1);
	}

	public void gotoPrevWeek() {
		_dt = _dt.minusWeeks(1);
		redraw();
		scrollBarUpdate();
	}

	public void gotoToday() {

		_dt = new DateTime();

		_selectedItem = new Selection(new Long(_dt.getYear() * 1000 + _dt.getDayOfYear()), SelectionType.DAY);
		gotoDate(_dt);

	}

	public void gotoTourId(final Long tourId) {

		final DateTime dt = _dataProvider.getCalendarTourDateTime(tourId);

		_selectedItem = new Selection(tourId, SelectionType.TOUR);
		_graphClean = false;
		gotoDate(dt);
	}

	private void gotoTourOffset(int offset) {

		if (_tourFocus.size() < 1) {
			if (offset < 0) {
				gotoPrevWeek();
			} else {
				gotoNextWeek();
			}
			return;
		}

		if (!_selectedItem.isTour()) { // if no tour is selected, count from first/last tour and select this tour
			if (offset > 0) {
				_selectedItem = new Selection(_tourFocus.get(0).id, SelectionType.TOUR);
				offset--;
			} else {
				_selectedItem = new Selection(new Long(_tourFocus.get(_tourFocus.size() - 1).id), SelectionType.TOUR);
				offset++;
			}
		}

		int index = 0;
		for (final ObjectLocation ol : _tourFocus) {
			if (_selectedItem.id == ol.id) {
				break;
			}
			index++;
		}
		final int newIndex = index + offset;
		if (newIndex < 0) {
			gotoPrevWeek();
			return;
		} else if (newIndex >= _tourFocus.size()) {
			gotoNextWeek();
			return;
		} else {
			_selectedItem = new Selection(_tourFocus.get(newIndex).id, SelectionType.TOUR);
			redraw();
		}

	}

	private void gotoTourOtherWeekday(final int direction) {

		if (_tourFocus.size() < 1) {
			if (direction < 0) {
				gotoPrevWeek();
			} else {
				gotoNextWeek();
			}
			return;
		}

		if (!_selectedItem.isTour()) { // if no tour is selected, count from first/last tour and select this tour
			if (direction > 0) {
				_selectedItem = new Selection(_tourFocus.get(0).id, SelectionType.TOUR);
			} else {
				_selectedItem = new Selection(new Long(_tourFocus.get(_tourFocus.size() - 1).id), SelectionType.TOUR);
			}
			redraw();
			return;
		}

		int index = 0;
		CalendarTourData ctd = null;
		for (final ObjectLocation ol : _tourFocus) {
			if (_selectedItem.id == ol.id) {
				ctd = (CalendarTourData) (ol.o);
				break;
			}
			index++;
		}
		if (null == ctd) {
			return;
		}
		index += direction;
		for (int i = index; i >= 0 && i < _tourFocus.size(); i += direction) {
			final ObjectLocation ol = _tourFocus.get(i);
			if (ctd.dayOfWeek != ((CalendarTourData) (ol.o)).dayOfWeek) {
				_selectedItem.id = ol.id;
				redraw();
				return;
			}
		}

		if (direction < 0) {
			gotoPrevWeek();
		} else {
			gotoNextWeek();
		}

	}

	private void gotoTourSameWeekday(final int direction) {

		if (_tourFocus.size() < 1) {
			if (direction < 0) {
				gotoPrevWeek();
			} else {
				gotoNextWeek();
			}
			return;
		}

		if (!_selectedItem.isTour()) { // if no tour is selected, count from first/last tour and select this tour
			if (direction > 0) {
				_selectedItem = new Selection(_tourFocus.get(0).id, SelectionType.TOUR);
			} else {
				_selectedItem = new Selection(new Long(_tourFocus.get(_tourFocus.size() - 1).id), SelectionType.TOUR);
			}
			redraw();
			return;
		}

		int index = 0;
		CalendarTourData ctd = null;
		for (final ObjectLocation ol : _tourFocus) {
			if (_selectedItem.id == ol.id) {
				ctd = (CalendarTourData) (ol.o);
				break;
			}
			index++;
		}
		if (null == ctd) {
			return;
		}
		index += direction;
		for (int i = index; i >= 0 && i < _tourFocus.size(); i += direction) {
			final ObjectLocation ol = _tourFocus.get(i);
			if (ctd.dayOfWeek == ((CalendarTourData) (ol.o)).dayOfWeek) {
				_selectedItem = new Selection(ol.id, SelectionType.TOUR);
				redraw();
				return;
			}
		}

		if (direction < 0) {
			gotoPrevWeek();
		} else {
			gotoNextWeek();
		}

	}

	public void gotoYear(final int year) {

		_dt = _dt.withYear(year);
		// scroll to first day of the week
		_dt = _dt.withDayOfWeek(getFirstDayOfWeek());

		_graphClean = false;
		redraw();
		scrollBarUpdate();
	}

	private void highlightTour(final GC gc, final CalendarTourData data, final Rectangle rec) {
		gc.setAlpha(0xd0);
		gc.setBackground(new Color(_display, _rgbBright.get(data.typeColorIndex)));
		gc.setForeground(new Color(_display, _rgbDark.get(data.typeColorIndex)));
		gc.fillGradientRectangle(rec.x - 4, rec.y - 4, rec.width + 9, rec.height + 9, false);
		gc.setForeground(new Color(_display, _rgbLine.get(data.typeColorIndex)));
		gc.drawRoundRectangle(rec.x - 5, rec.y - 5, rec.width + 10, rec.height + 10, 6, 6);
		String title = data.tourTitle;
		gc.setForeground(_black);
		title = title == null ? "No Title" : title;
		gc.setClipping(rec.x + 2, rec.y, rec.width - 4, rec.height - 2);
		gc.drawText(title, rec.x + 3, rec.y + 1, true);
		gc.setClipping(_nullRec);
	}

	/**
	 * Mouse move event handler
	 * 
	 * @param event
	 */
	private void onMouseMove(final MouseEvent event) {

		if (null == _image) {
			return;
		}
		if (_image.isDisposed()) {
			return;
		}

		if (_tourFocus == null) {
			return;
		}

		final Selection oldHighlight = _highlightedItem;
		final Selection oldSelection = _selectedItem;

		if (1 == event.button || 3 == event.button) {
			_selectedItem = _noItem;
		}

		boolean tourFound = false;
		boolean dayFound = false;
		long id;
		for (final ObjectLocation ol : _tourFocus) {
			if (ol.r.contains(event.x, event.y)) {
				id = ol.id;
				if (1 == event.button) {
					if (oldSelection.id == ol.id) {
						_selectedItem = _noItem; // deselect if already selected
					} else {
						_selectedItem = new Selection(id, SelectionType.TOUR);
					}
					_graphClean = false;
				} else if (3 == event.button) {
					_graphClean = false;
					_selectedItem = new Selection(id, SelectionType.TOUR);
				} else if (oldHighlight.id != id) { // a new object is highlighted
					_highlightedItem = new Selection(id, SelectionType.TOUR);
				}
				tourFound = true;
				break;
			}
		}

		if (!tourFound) {
			for (final ObjectLocation ol : _dayFocus) {
				if (ol.r.contains(event.x, event.y)) {
					id = ol.id;
					if (1 == event.button) {
						if (oldSelection.id == ol.id) {
							_selectedItem = _noItem; // deselect if already selected
						} else {
							_selectedItem = new Selection(id, SelectionType.DAY);
						}
						_graphClean = false;
					} else if (3 == event.button) {
						_graphClean = false;
						_selectedItem = new Selection(id, SelectionType.DAY);
					} else if (oldHighlight.id != id) { // a new object is highlighted
						_highlightedItem = new Selection(id, SelectionType.DAY);
					}
					dayFound = true;
					break;
				}
			}
		}

		if (!oldSelection.equals(_selectedItem)) { // highlight selection -> redraw calendar
			_graphClean = false;
			redraw();
			return;
		}

		if (!dayFound && !tourFound) { // only draw base calendar, skip highlighting
			_highlightRemoved = true;
			redraw();
			return;
		}

		_highlightChanged = (!oldHighlight.equals(_highlightedItem));
		if (_highlightChanged) { // only draw the highlighting on top of the calendar image
			redraw();
		}
		return;
	}

	private void onScroll(final SelectionEvent event) {

		final ScrollBar sb = _parent.getVerticalBar();

		final int selectableMax = sb.getMaximum() - sb.getThumb() - 1;
		final int selectableMin = 0 + 1;
		final int selection = sb.getSelection();
		final int change = selection - _scrollBarLastSelection;

		if (_scrollDebug) {
			System.out.println("Last Selection: " + _scrollBarLastSelection + " - New Selection: " + selection);
		}

		if (_scrollBarShift != 0) {
			if (_scrollBarLastSelection == selectableMax) {
				sb.setSelection(selectableMax);
			} else if (_scrollBarLastSelection == selectableMin) {
				sb.setSelection(selectableMin);
			}
			if (change > 0 && _scrollBarShift < 0) { // ensure we are not shifting over "0"
				_scrollBarShift = Math.min(0, _scrollBarShift + change);
			} else if (change < 0 && _scrollBarShift > 0) {
				_scrollBarShift = Math.max(0, _scrollBarShift + change);
			} else {
				_scrollBarShift += change;
			}
		} else { // do we need start shifting the scroll bar ?
			if (selection < selectableMin) { // we are at the upper border
				sb.setSelection(selectableMin);
				_scrollBarShift += change;
			}
			if (selection > selectableMax) { // we are at the lower border
				sb.setSelection(selectableMax);
				_scrollBarShift += change;

			}
		}

		// selection = sb.getSelection();
		// final DateTime dt1 = scrollBarStart();
		// final DateTime dt2 = scrollBarEnd();
		// int weeks = (int) ((dt2.getMillis() - dt1.getMillis()) / (1000 * 60 * 60 * 24 * 7));
		// final int thumbSize = Math.max(_numWeeksDisplayed, weeks / 20); // ensure the thumb isn't getting to small
		// sb.setThumb(thumbSize);
		// weeks += thumbSize;
		// sb.setMinimum(0);
		// sb.setMaximum(weeks);
		// sb.setPageIncrement(_numWeeksDisplayed);

		if (_scrollDebug) {
			System.out.println("SbarShift: " + _scrollBarShift + " - Selected Week: " + selection);
		}

		_scrollBarLastSelection = sb.getSelection();

		// goto the selected week
		_dt = scrollBarStart().plusDays(selection * 7);

		_graphClean = false;
		redraw();
	}

	public void refreshCalendar() {
		_dataProvider.invalidate();
		_graphClean = false;
		redraw();
	}

	public void removeSelection() {
		if (!_selectedItem.equals(_noItem)) {
			_selectedItem = _noItem;
			_graphClean = false;
			redraw();
		}
		// TODO Auto-generated method stub
		
	}

	void removeSelectionListener(final ICalendarSelectionProvider listener) {
		_selectionProvider.remove(listener);
	}

	private DateTime scrollBarEnd() {
		final DateTime dt = new DateTime().plusWeeks(_scrollBarShift);

		// ensure the date return is a "FirstDayOfTheWeek" !!!
		return dt.plusWeeks(1).withDayOfWeek(getFirstDayOfWeek());
	}

	private DateTime scrollBarStart() {
		DateTime dt = _dataProvider.getFirstDateTime().plusWeeks(_scrollBarShift);
		final DateTime now = new DateTime();
		final int weeks = (int) ((now.getMillis() - dt.getMillis()) / _WEEK_MILLIS);
		if (weeks < _MIN_SCROLLABLE_WEEKS) { // ensure the scrollable area has a reasonable size
			dt = now.minusWeeks(_MIN_SCROLLABLE_WEEKS);
		}

		// ensure the date return is a "FirstDayOfTheWeek" !!!
		return dt.minusWeeks(1).withDayOfWeek(getFirstDayOfWeek());
	}

	private void scrollBarUpdate() {
		_scrollBarShift = 0;
		final ScrollBar sb = _parent.getVerticalBar();
		final long dt1 = scrollBarStart().getMillis();
		final long dt2 = _dt.getMillis();
		final long dt3 = scrollBarEnd().getMillis();
		int maxWeeks = (int) ((dt3 - dt1) / _WEEK_MILLIS);
		final int thumbSize = Math.max(_numWeeksDisplayed, maxWeeks / 20); // ensure the thumb isn't getting to small
		sb.setThumb(thumbSize);
		int thisWeek;
		if (dt2 < dt1) {
			// shift negative
			_scrollBarShift = (int) ((dt2 - dt1) / _WEEK_MILLIS);
			thisWeek = 1;
		} else if (dt2 > dt3) {
			// shift positive
			_scrollBarShift = (int) ((dt2 - dt3) / _WEEK_MILLIS);
			thisWeek = maxWeeks - 1;
		} else  {
			thisWeek = (int) ((dt2 - dt1) / _WEEK_MILLIS);
		}
		maxWeeks += thumbSize;
		sb.setMinimum(0);
		sb.setMaximum(maxWeeks);
		sb.setPageIncrement(_numWeeksDisplayed);
		sb.setSelection(thisWeek);
		_scrollBarLastSelection = thisWeek;
	}

	public void setFirstDay(final DateTime dt) {

		_dt = dt;
		_graphClean = false;
		redraw();
		scrollBarUpdate();
	}

	void setLinked(final boolean linked) {
		if (false == linked) {
			_selectedItem = _noItem;
			_graphClean = false;
			redraw();
		}
	}

	void setNavigationStyle(final NavigationStyle navigationStyle) {
		_navigationStyle = navigationStyle;
	}

	void setNumberOfToursPerDay(final int numberOfToursPerDay) {
		_numberOfToursPerDay = numberOfToursPerDay;
		_graphClean = false;
		redraw();
	}

	public void setSelectionTourId(final Long selectedTourId) {
		this._selectedItem = new Selection(selectedTourId, SelectionType.TOUR);
	}

	void setTourFieldSizeDynamic(final boolean dynamicTourFieldSize) {
		_graphClean = false;
		_dynamicTourFieldSize = dynamicTourFieldSize;
		redraw();
	}

	public void setYearMonthContributor(final CalendarYearMonthContributionItem calendarYearMonthContribuor) {
		_calendarYearMonthContributor = calendarYearMonthContribuor;

	}

	void setZoom(final int numWeeksDisplayed) {
		_numWeeksDisplayed = numWeeksDisplayed;
		redraw();
	}

	void zoomIn() {
		_numWeeksDisplayed = _numWeeksDisplayed > 1 ? --_numWeeksDisplayed : _numWeeksDisplayed;
		redraw();
	}

	void zoomOut() {
		_numWeeksDisplayed++;
		redraw();
	}

}
