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

	private Type								_highlightType;
	private Long								_highlightId		= new Long(-1);

	final private Rectangle						_nullRec			= null;

	private CalendarTourDataProvider			_dataProvider;
	private CalendarYearMonthContributionItem	_calendarYearMonthContributor;

	private ListenerList						_selectionProvider	= new ListenerList();

	private Long								_selectedTourId		= new Long(-1);

	private int									_scrollBarShift;

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

	enum Type {
		YEAR, MONHT, WEEK, DAY, TOUR
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

		final int XX = getSize().x;
		final int YY = getSize().y;

//		System.out.println(_graphClean ? "clean!" : "NOT clean!");
//		System.out.println(_highlightChanged ? "HL changed!" : "HL NOT changed");
//		System.out.println(_highlightRemoved ? "HL removed!" : "HL NOT removed");
//		System.out.println("-----------");

		if (_graphClean && _highlightRemoved && _image != null) {
			gc.drawImage(_image, 0, 0);
			_highlightId = new Long(-1);
			_highlightRemoved = false;
			return;
		}

		if (_graphClean && _highlightChanged && _image != null) {
			final GC oldGc = gc;
			_highlight = new Image(getDisplay(), XX, YY);
			gc = new GC(_highlight);
			gc.drawImage(_image, 0, 0);
			redrawHighLight(gc);
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

		// final Color alternate = new Color(gc.getDevice(), 0xf5, 0xf5, 0xf5); // efefef
		final Color alternate = new Color(gc.getDevice(), 0xf0, 0xf0, 0xf0); // efefef

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

		final int header = (YY / 4 / numRows); // header is 1/4 cell height - we display at most 3 workouts per day

		final float dX = (float) XX / (float) numCols;
		final float dY = (float) YY / (float) numRows;

		// first draw the horizontal lines
		gc.setBackground(_white);
		gc.setForeground(_gray);
		for (int i = 0; i <= numRows; i++) {
			gc.drawLine(0, (int) (i * dY), XX, (int) (i * dY));
		}

		Rectangle selectedRec = null;
		CalendarTourData selectedTour = null;
		boolean doSelection = false;

		final long todayDayId = (new Day(new DateTime())).dayId + 1;

		// than draw the header boxes
		gc.setBackground(_white);
		gc.setForeground(_gray);
		/*
		 * Weeks in month
		 */
		for (int i = 0; i < numRows; i++) {
			final int Y1 = (int) (i * dY);
			final int Y2 = (int) ((i + 1) * dY);
			gc.setForeground(_gray);
			gc.setBackground(_white);
			gc.fillGradientRectangle(1 + (int) dX, Y1, (int) (dX * 7), header, true); // weekday header
			/*
			 * Days in week
			 */
			long nextDayId = (new Day(date)).dayId + 1; // simply incrementing days should be much faster...
			for (int j = 1; j < 8; j++) { // col 0 is for weekinfo, the week itself starts at col 1
				final int X1 = (int) (j * dX);
				final int X2 = (int) ((j + 1) * dX);
				final int dy = (Y2 - Y1) / 4;
				final Rectangle dayRec = new Rectangle(X1, Y1, (X2 - X1), (Y2 - Y1));
				final Day day = new Day(nextDayId);
				_dayFocus.add(new ObjectLocation(dayRec, nextDayId, day));
				nextDayId = day.dayId + 1;
				final int weekDay = date.getDayOfWeek();
				if (day.dayId == todayDayId) {
					gc.setForeground(_blue);
				} else if (weekDay == DateTimeConstants.SATURDAY || weekDay == DateTimeConstants.SUNDAY) {
					gc.setForeground(_red);
				} else {
					gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_GRAY));
				}
				gc.setBackground(_white);
				gc.setFont(boldFont);
				if ((date.getMonthOfYear() % 2) == 1) {
					gc.setBackground(alternate);
					gc.fillRectangle(dayRec.x, dayRec.y + dy - 2, dayRec.width, dayRec.height - dy + 2);
				}
				if (day.dayId == todayDayId) {
//					highlightToday(gc, dayRec);
					// TODO do highlighting of Today at the very end, like for the tours
//					gc.setBackground(_blue);
//					gc.fillRectangle(dayRec.x, dayRec.y + dy - 2, dayRec.width, dayRec.height - dy + 2);
				}
				gc.drawText(date.toString("dd. MMM yy"), X1 + 5, Y1, true);
				data = _dataProvider.getCalendarDayData(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth());
				gc.setFont(normalFont);
				/*
				 * Tours in day
				 */
				for (int k = data.length - 1; k >= 0; k--) { // morning top, evening button
					final int ddy = (data.length <= 3 ? dy : (3 * dy) / data.length); // narrow the tour fields to fit if more than 3 tours (default size 1/3)
					final Rectangle tour = new Rectangle(X1 + 1, Y2 - (k + 1) * ddy, (X2 - X1 - 2), ddy - 1);
					final Rectangle focus = new Rectangle(tour.x - 1, tour.y - 1, tour.width + 2, tour.height + 2);
					_tourFocus.add(new ObjectLocation(focus, data[k].tourId, data[k]));
					// TODO create each color only once (private array( and dispose
					gc.setBackground(new Color(_display, _rgbBright.get(data[k].typeColorIndex)));
					gc.setForeground(new Color(_display, _rgbDark.get(data[k].typeColorIndex)));
					gc.fillGradientRectangle(tour.x + 1, tour.y + 1, tour.width - 1, tour.height - 1, false);
					final Color lineColor = new Color(_display, _rgbLine.get(data[k].typeColorIndex));
					gc.setForeground(lineColor);
					gc.drawRectangle(tour);
					String title = data[k].tourTitle;
					title = title == null ? "No Title" : title;
					// gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_GRAY));
					gc.setForeground(lineColor);
					// gc.setForeground(_black);
					gc.setClipping(focus.x + 2, focus.y, focus.width - 4, focus.height - 2);
					gc.drawText(title, tour.x + 2, tour.y, true);
					gc.setClipping(_nullRec);
					if (data[k].tourId == _selectedTourId) {
						selectedRec = focus;
						selectedTour = data[k];
						doSelection = true;
					}
				}
				date = date.plusDays(1);
			}
		}
		gc.setFont(normalFont);

		// and finally the vertical lines
		gc.setForeground(_display.getSystemColor(SWT.COLOR_GRAY));
		for (int i = 0; i <= numCols; i++) {
			gc.drawLine((int) (i * dX), 0, (int) (i * dX), YY);
		}

		if (doSelection && selectedTour != null && selectedRec != null) {
			selectTour(gc, selectedTour, selectedRec);
			fireSelectionEvent(Type.TOUR, _selectedTourId);
		} else {
			_selectedTourId = new Long(-1); // we probably scrolled the selected tour out of the screen, deselect it
		}

		boldFont.dispose();

		gc.dispose();
		oldGc.drawImage(_image, 0, 0);

		_graphClean = true;

	}

	void fireSelectionEvent(final Type type, final long id) {

		final Object[] listeners = _selectionProvider.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ICalendarSelectionProvider listener = (ICalendarSelectionProvider) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectionChanged(type, id);
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

	public Long getSelectionTourId() {
		return _selectedTourId;
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

		gotoDate(_dt);

	}

	public void gotoTourId(final Long tourId) {

		final DateTime dt = _dataProvider.getCalendarTourDateTime(tourId);

		_selectedTourId = tourId;
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

		if (_selectedTourId == -1) { // if no tour is selected, count from first/last tour and select this tour
			if (offset > 0) {
				_selectedTourId = _tourFocus.get(0).id;
				offset--;
			} else {
				_selectedTourId = _tourFocus.get(_tourFocus.size() - 1).id;
				offset++;
			}
		}

		int index = 0;
		for (final ObjectLocation ol : _tourFocus) {
			if (_selectedTourId == ol.id) {
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
			_selectedTourId = _tourFocus.get(newIndex).id;
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

		if (_selectedTourId == -1) { // if no tour is selected, count from first/last tour and select this tour
			if (direction > 0) {
				_selectedTourId = _tourFocus.get(0).id;
			} else {
				_selectedTourId = _tourFocus.get(_tourFocus.size() - 1).id;
			}
			redraw();
			return;
		}

		int index = 0;
		CalendarTourData ctd = null;
		for (final ObjectLocation ol : _tourFocus) {
			if (_selectedTourId == ol.id) {
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
				_selectedTourId = ol.id;
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

		if (_selectedTourId == -1) { // if no tour is selected, count from first/last tour and select this tour
			if (direction > 0) {
				_selectedTourId = _tourFocus.get(0).id;
			} else {
				_selectedTourId = _tourFocus.get(_tourFocus.size() - 1).id;
			}
			redraw();
			return;
		}

		int index = 0;
		CalendarTourData ctd = null;
		for (final ObjectLocation ol : _tourFocus) {
			if (_selectedTourId == ol.id) {
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
				_selectedTourId = ol.id;
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

	private void highlightToday(final GC gc, final Rectangle r) {
		gc.setForeground(_blue);
		gc.setBackground(_blue);
		gc.fillRectangle(r.x - 4, r.y - 4, r.width + 9, r.height + 9);
		gc.drawRoundRectangle(r.x - 5, r.y - 5, r.width + 10, r.height + 10, 6, 6);
		
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

		final long oldId = _highlightId;
		final Type oldType = _highlightType;
		final Long oldSelectionID = _selectedTourId;
		if (1 == event.button || 3 == event.button) {
			_selectedTourId = new Long(-1);
		}

		boolean tourFound = false;
		boolean dayFound = false;
		long id;
		for (final ObjectLocation ol : _tourFocus) {
			if (ol.r.contains(event.x, event.y)) {
				id = ol.id;
				if (1 == event.button) {
					if (oldSelectionID == ol.id) {
						_selectedTourId = new Long(-1); // deselect if already selected
					} else {
						_selectedTourId = id;
					}
					_graphClean = false;
				} else if (3 == event.button) {
					_graphClean = false;
					_selectedTourId = id;
				} else if (_highlightType != Type.TOUR || id != _highlightId) { // a new object is highlighted
					_highlightType = Type.TOUR;
					_highlightId = id;
					// System.out.print("- show tooltip -");
				}
				tourFound = true;
				break;
			}
		}

		if (!tourFound) {
			for (final ObjectLocation ol : _dayFocus) {
				if (ol.r.contains(event.x, event.y)) {
					id = ol.id;
					if (_highlightType != Type.DAY || id != _highlightId) { // a new object is highlighted
						_highlightType = Type.DAY;
						_highlightId = id;
					}
					dayFound = true;
					break;
				}
			}
		}

		if (oldSelectionID != _selectedTourId) { // highlight selection -> redraw calendar
			_graphClean = false;
			redraw();
			return;
		}

		if (!dayFound && !tourFound) { // only draw base calendar, skip highlighting
			_highlightRemoved = true;
			redraw();
			return;
		}

		_highlightChanged = (oldType != _highlightType || oldId != _highlightId);
		if (_highlightChanged) { // only draw the highlighting on top of the calendar image
			redraw();
		}
		return;
	}

	/*
	 * Not perfect but working more or less. We use the first tour and today to define the calendar
	 * area we want to scroll. If a user is on one of both borders (first tour/today) he could still
	 * use the up- and down buttons in the scroll bar to scroll further down/up. We achieve this but
	 * adjusting the date associated with the min/max value of the scrollbar, thus we are shifting
	 * the scrollbar while keeping the scrollbar range. Updating this shift value is currently not
	 * done when a user selects a value in the year/month combo boxes....
	 */
	private void onScroll(final SelectionEvent event) {

		final ScrollBar sb = _parent.getVerticalBar();

		// if we've shifted the scrollbar keep shifting the scrollbar until the shift is 0
		if (_scrollBarShift != 0) {
			switch (event.detail) {
			case SWT.ARROW_DOWN:
				if (_scrollBarShift < 0) { // keep selection at minimum
					sb.setSelection(sb.getMinimum());
				}
				_scrollBarShift++;
				break;
			case SWT.ARROW_UP:
				if (_scrollBarShift > 0) { // keep selection at maximum
					sb.setSelection(sb.getMaximum());
				}
				_scrollBarShift--;
				break;
			default:
				_scrollBarShift = 0;
			}
		} else { // do we need start shifting the scrollbar ?
			if (sb.getSelection() <= 0) { // we are at the upper border
				if (event.detail == SWT.ARROW_UP) {
					_scrollBarShift--;
				}
			}
			if (sb.getSelection() >= sb.getMaximum() - sb.getThumb()) { // we are at the lower border
				if (event.detail == SWT.ARROW_DOWN) {
					_scrollBarShift++;
				}
			}
		}


		final int selectedWeek = sb.getSelection();
		final DateTime dt1 = scrollBarStart();
		final DateTime dt2 = scrollBarEnd();
		int weeks = (int) ((dt2.getMillis() - dt1.getMillis()) / (1000 * 60 * 60 * 24 * 7));
		final int thumbSize = Math.max(_numWeeksDisplayed, weeks / 20); // ensure the thumb isn't getting to small
		sb.setThumb(thumbSize);
		weeks += thumbSize;
		sb.setMinimum(0);
		sb.setMaximum(weeks);
		sb.setPageIncrement(_numWeeksDisplayed);
		
		System.out.println("SbarShift: " + _scrollBarShift + " - Selected Week: " + selectedWeek + " : " + weeks);
		
		// goto the selected week
		_dt = dt1.plusDays(selectedWeek * 7);
		_dt = _dt.withDayOfWeek(getFirstDayOfWeek()); // set first day to start of week

		_graphClean = false;
		redraw();
	}

	private void redrawHighLight(final GC gc) {

		List<ObjectLocation> objects;
		if (_highlightType == Type.TOUR) {
			objects = _tourFocus;
		} else if (_highlightType == Type.DAY) {
			objects = _dayFocus;
		} else {
			return;
		}

		for (final ObjectLocation ol : objects) {
			if (ol.id == _highlightId) {
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

	public void refreshCalendar() {
		_dataProvider.invalidate();
		_graphClean = false;
		redraw();
	}

	public void removeSelection() {
		if (_selectedTourId > 0) {
			_selectedTourId = new Long(1);
			_graphClean = false;
			redraw();
		}
		// TODO Auto-generated method stub
		
	}

	void removeSelectionListener(final ICalendarSelectionProvider listener) {
		_selectionProvider.remove(listener);
	}

	private DateTime scrollBarEnd() {
		return new DateTime().plusWeeks(_scrollBarShift);
	}

	private DateTime scrollBarStart() {
		return _dataProvider.getFirstDateTime().plusWeeks(_scrollBarShift);
	}

	private void scrollBarUpdate() {
		final ScrollBar sb = _parent.getVerticalBar();
		final DateTime dt1 = scrollBarStart();
		final DateTime dt2 = _dt;
		final DateTime dt3 = scrollBarEnd();
		int maxWeeks = (int) ((dt3.getMillis() - dt1.getMillis()) / (1000 * 60 * 60 * 24 * 7));
		final int thisWeek = (int) ((dt2.getMillis() - dt1.getMillis()) / (1000 * 60 * 60 * 24 * 7));
		final int thumbSize = Math.max(_numWeeksDisplayed, maxWeeks / 20); // ensure the thumb isn't getting to small
		sb.setThumb(thumbSize);
		maxWeeks += thumbSize;
		sb.setMinimum(0);
		sb.setMaximum(maxWeeks);
		sb.setPageIncrement(_numWeeksDisplayed);
		sb.setSelection(thisWeek);
		_scrollBarShift = 0;
	}

	private void selectTour(final GC gc, final CalendarTourData data, final Rectangle rec) {

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

	public void setFirstDay(final DateTime dt) {

		_dt = dt;
		_graphClean = false;
		redraw();
		scrollBarUpdate();
	}

	void setLinked(final boolean linked) {
		if (false == linked) {
			_selectedTourId = new Long(-1);
			_graphClean = false;
			redraw();
		}
	}

	void setNavigationStyle(final NavigationStyle navigationStyle) {
		_navigationStyle = navigationStyle;
	}

	public void setSelectionTourId(final Long selectedTourId) {
		this._selectedTourId = selectedTourId;
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
