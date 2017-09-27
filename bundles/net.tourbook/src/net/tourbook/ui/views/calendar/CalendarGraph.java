/*******************************************************************************
 * Copyright (C) 2011-2017 Matthias Helmling and Contributors
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
package net.tourbook.ui.views.calendar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorCacheSWT;
import net.tourbook.common.color.ColorUtil;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProviderAll;
import net.tourbook.ui.views.calendar.CalendarView.TourInfoFormatter;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
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
import org.joda.time.Days;

public class CalendarGraph extends Canvas implements ITourProviderAll {

	private static final IPreferenceStore		_prefStore				= CommonActivator.getPrefStore();

	private static final long					_WEEK_MILLIS			= (1000 * 60 * 60 * 24 * 7);

	private static final int					_MIN_SCROLLABLE_WEEKS	= 12;

// SET_FORMATTING_OFF
	private TourInfoFormatter[]					_tourInfoFormatter			= new TourInfoFormatter[CalendarView.numberOfInfoLines];
	private WeekSummaryFormatter[]				_weekSummaryFormatter		= new WeekSummaryFormatter[CalendarView.numberOfSummaryLines];
// SET_FORMATTING_ON

	private final TourDoubleClickState			_tourDoubleClickState	= new TourDoubleClickState();
	private ColorCacheSWT						_colorCache				= new ColorCacheSWT();

	private ArrayList<RGB>						_rgbBright;
	private ArrayList<RGB>						_rgbDark;
	private ArrayList<RGB>						_rgbLine;
	private ArrayList<RGB>						_rgbText;

	/**
	 * Date of the first visible day in the calendar viewport.
	 */
	private DateTime							_firstDay				= new DateTime();

	private int									_lastDayOfWeekToGoTo	= -1;

	private List<ObjectLocation>				_tourFocus;
	private List<ObjectLocation>				_dayFocus;

	private boolean								_isHighlightChanged		= false;
	private boolean								_isGraphClean			= false;

	private CalendarItem						_noItem					= new CalendarItem(-1, SelectionType.NONE);
	private CalendarItem						_selectedItem			= _noItem;
	private CalendarItem						_highlightedItem		= _noItem;
	private CalendarItem						_lastSelection			= _noItem;
	private CalendarItem						_hoveredTour;

	final private Rectangle						_nullRec				= null;

	private CalendarView						_calendarView;
	private CalendarTourDataProvider			_dataProvider;

	private CalendarYearMonthContributionItem	_calendarYearMonthContributor;
	private ListenerList						_selectionProvider		= new ListenerList();

	private int									_scrollBarShift;
	private int									_scrollBarLastSelection;

	private int									_numToursPerDay			= 3;
	private int									_numVisibleWeeks;

	private boolean								_isDynamicTourFieldSize	= true;
	private boolean								_isScrollbarInitialized;
	private boolean								_useTextColorForTourInfoText;
	private boolean								_useBlackForHighlightTourInfoText;

	/**
	 * This rectangle contains all visible days except week no and week info area.
	 */
	private Rectangle							_calendarAllDaysRectangle;

	/**
	 * Cache font height;
	 */
	private int									_fontHeight;
	private int									_fontAverageCharWidth;

	private LocalDate							_calendarFirstDay;
	private LocalDate							_calendarLastDay;

	private int									_lastWeekDateYPos;
	private int									_lastWeekDateYear;
	private int									_rowHeight;

	/*
	 * UI controls
	 */
	private Composite							_parent;
	private Display								_display				= Display.getCurrent();

	private Color								_black					= _display.getSystemColor(SWT.COLOR_BLACK);
	private Color								_gray					= _display.getSystemColor(SWT.COLOR_GRAY);
	private Color								_white					= _display.getSystemColor(SWT.COLOR_WHITE);
	private Color								_red					= _display.getSystemColor(SWT.COLOR_RED);
	private Color								_blue					= _display.getSystemColor(SWT.COLOR_BLUE);

	private Color								_darkGray				= _colorCache.getColor(0x404040);

	private FontMetrics							_fontMetrics;
	private Font								_boldFont;

	private Image								_image;
	private Image								_highlight;

	class CalendarItem {

		long			id;
		SelectionType	type;

		Rectangle		itemRectangle;

		CalendarItem(final long id, final SelectionType type) {

			this.id = id;
			this.type = type;
		}

		@Override
		public boolean equals(final Object o) {

			if (o instanceof CalendarItem) {

				final boolean isIdEqual = ((CalendarItem) o).id == this.id;
				final boolean isTypeEqual = ((CalendarItem) o).type == this.type;
				final boolean isEqual = isIdEqual && isTypeEqual;

				return isEqual;
			}

			return super.equals(o);
		}

		boolean isTour() {

			return (id > 0 && type == SelectionType.TOUR);
		}

		@Override
		public String toString() {
			return "Selection [" //$NON-NLS-1$
					+ "id=" + id + ", " //$NON-NLS-1$ //$NON-NLS-2$
					+ "type=" + type + ", " //$NON-NLS-1$ //$NON-NLS-2$
					//					+ "itemRectangle=" + itemRectangle
					+ "]"; //$NON-NLS-1$
		}

	};

	private class Day {

		private long dayId;

		Day(final DateTime date) {

			final Days days = Days.daysBetween(new DateTime(0), date);
			this.dayId = days.getDays();
		}

		Day(final long dayId) {
			this.dayId = dayId;
		}
	}

	private class ObjectLocation {

		private Rectangle	rect;
		private Object		o;
		private long		id;

		ObjectLocation(final Rectangle r, final long id, final Object o) {

			this.o = o;
			this.id = id;
			this.rect = r;
		}
	}

	enum SelectionType {
		YEAR, MONHT, WEEK, DAY, TOUR, NONE
	}

	CalendarGraph(final Composite parent, final int style, final CalendarView calendarView) {

		super(parent, style);

		_parent = parent;
		_calendarView = calendarView;

		final GC gc = new GC(parent);
		{
			gc.setFont(JFaceResources.getDialogFont());
			_fontMetrics = gc.getFontMetrics();
			_fontHeight = _fontMetrics.getHeight();
			_fontAverageCharWidth = _fontMetrics.getAverageCharWidth();
		}
		gc.dispose();

		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		_dataProvider = CalendarTourDataProvider.getInstance();
		_dataProvider.setCalendarGraph(this);

		_rgbBright = new ArrayList<RGB>();
		_rgbDark = new ArrayList<RGB>();
		_rgbLine = new ArrayList<RGB>();
		_rgbText = new ArrayList<RGB>();

		updateTourTypeColors();

		addListener();
	}

	private void addListener() {

		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent event) {

				drawCalendar(event.gc);

				// fix problem that the scrollbar is initially not correctly setup
				if (!_isScrollbarInitialized) {
					_isScrollbarInitialized = true;
					scrollBarUpdate();
				}
			}
		});

		addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				if (_selectedItem.isTour()) {
					_tourDoubleClickState.canEditTour = true;
					_tourDoubleClickState.canOpenTour = true;
					_tourDoubleClickState.canQuickEditTour = true;
					_tourDoubleClickState.canEditMarker = true;
					_tourDoubleClickState.canAdjustAltitude = true;
					TourManager.getInstance().tourDoubleClickAction(CalendarGraph.this, _tourDoubleClickState);
				}

			}

			@Override
			public void mouseDown(final MouseEvent e) {
				onMouseMove(e);
			}

			@Override
			public void mouseUp(final MouseEvent e) {
				// onMouseMove(e);
			}

		});

		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(final MouseEvent e) {
				onMouseMove(e);
			}
		});

		addFocusListener(new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {
				// System.out.println("Focus gained");
				// redraw();
			}

			@Override
			public void focusLost(final FocusEvent e) {
				// System.out.println("Focus lost");
				// redraw();
			}
		});

		addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(final TraverseEvent e) {
				switch (e.detail) {
				case SWT.TRAVERSE_RETURN:
					if (_selectedItem.isTour()) {
						_tourDoubleClickState.canEditTour = true;
						_tourDoubleClickState.canOpenTour = true;
						_tourDoubleClickState.canQuickEditTour = true;
						_tourDoubleClickState.canEditMarker = true;
						_tourDoubleClickState.canAdjustAltitude = true;
						TourManager.getInstance().tourDoubleClickAction(CalendarGraph.this, _tourDoubleClickState);
					} else {
						gotoNextTour();
					}
					break;
				case SWT.TRAVERSE_ESCAPE:
					_selectedItem = _noItem;
					redraw();
					break;
				case SWT.TRAVERSE_PAGE_NEXT:
				case SWT.TRAVERSE_PAGE_PREVIOUS:
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
					e.doit = true;
					break;
				}
			}
		});

		addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				switch (event.keyCode) {
				case SWT.ARROW_LEFT:
				case 'h':
					gotoPrevTour();
					break;

				case SWT.ARROW_RIGHT:
				case 'l':
					gotoNextTour();
					break;

				case SWT.ARROW_UP:
				case 'k':
					if (_selectedItem.isTour()) {
						gotoTourSameWeekday(-1);
					} else {
						gotoPrevWeek();
					}
					break;

				case SWT.ARROW_DOWN:
				case 'j':
					if (_selectedItem.isTour()) {
						gotoTourSameWeekday(+1);
					} else {
						gotoNextWeek();
					}
					break;

				case SWT.PAGE_DOWN:
				case 'n':
					gotoNextScreen();
					break;

				case SWT.PAGE_UP:
				case 'p':
					gotoPrevScreen();
					break;

				case SWT.HOME:
				case '.':
					gotoToday();
					break;
				case SWT.END:

				case ',':
					gotoFirstTour();
					break;

				case ' ':
					if (_selectedItem.isTour()) {
						_selectedItem = _noItem;
						redraw();
					} else {
						gotoPrevTour();
					}
					break;
				}
			}
		});

		addListener(SWT.MouseWheel, new Listener() {
			@Override
			public void handleEvent(final Event event) {

				final Point p = new Point(event.x, event.y);

				if (_calendarAllDaysRectangle.contains(p)) {

					if (event.count > 0) {
						if (_selectedItem.isTour()) {
							gotoPrevTour();
						} else {
							gotoPrevWeek();
						}
					} else {
						if (_selectedItem.isTour()) {
							gotoNextTour();
						} else {
							gotoNextWeek();
						}
					}

				} else { // left or right column, scroll by pages

					if (event.count > 0) {
						gotoPrevScreen();
					} else {
						gotoNextScreen();
					}
				}
				event.doit = false;
			}
		});

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				_colorCache.dispose();
			}
		});

		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent event) {

				_isGraphClean = false;
				redraw();
			}
		});

		addFocusListener(new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {
				// redraw();
			}

			@Override
			public void focusLost(final FocusEvent e) {
				// redraw();
			}
		});

		addMouseTrackListener(new MouseTrackListener() {
			@Override
			public void mouseEnter(final MouseEvent e) {
				// forceFocus();
			}

			@Override
			public void mouseExit(final MouseEvent e) {
				redraw();
			}

			@Override
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

	void draw() {

		_isGraphClean = false;
		redraw();
	}

	/**
	 * Paint calendar
	 * 
	 * @param gc
	 */
	private void drawCalendar(GC gc) {

		final int dayLabelXOffset = 1;

		final Rectangle canvas = getClientArea();
		final int canvasWidth = canvas.width;
		final int canvasHeight = canvas.height;

		if (_isGraphClean && _image != null) {

			_highlight = new Image(getDisplay(), canvasWidth, canvasHeight);
			{
				final GC gcHighLight = new GC(_highlight);
				gcHighLight.drawImage(_image, 0, 0);

				drawSelection(gcHighLight);

				if (_isHighlightChanged) {

					drawHighLight(gcHighLight);

					_isHighlightChanged = false;
				}

				gcHighLight.dispose();
				gc.drawImage(_highlight, 0, 0);

			}
			_highlight.dispose();

			return;
		}

		final CalendarConfig config = CalendarConfigManager.getActiveCalendarConfig();

		_lastWeekDateYPos = -1;
		_lastWeekDateYear = -1;

		if (_image != null && !_image.isDisposed()) {
			_image.dispose();
		}

		// get the first day of the viewport
		DateTime currentDate = new DateTime(_firstDay);
		_image = new Image(getDisplay(), canvasWidth, canvasHeight);

		// one col left and right of the week + 7 week days
		final int numDayColumns = 7;
		_rowHeight = config.weekHeight;

		final int numRows = canvasHeight / _rowHeight
				// add another week that the bottom is not empty
				+ 1;

		_numVisibleWeeks = numRows
				// remove additional week, it may be invisible when height is very small
				- 1;

		// keep calendar viewport dates
		_calendarFirstDay = LocalDate.of(
				currentDate.getYear(),
				currentDate.getMonthOfYear(),
				currentDate.getDayOfMonth());
		_calendarLastDay = _calendarFirstDay.plusWeeks(numRows).minusDays(1);

		_calendarView.updateUI_Title(_calendarFirstDay, _calendarLastDay);

		final GC oldGc = gc;
		gc = new GC(_image);

		final Color alternate = _colorCache.getColor(0xf0f0f0);

		_tourFocus = new ArrayList<ObjectLocation>();
		_dayFocus = new ArrayList<ObjectLocation>();

		CalendarTourData[] data;

		final Font normalFont = gc.getFont();

		gc.setBackground(_white);
		gc.setForeground(_black);
		gc.fillRectangle(canvas);

		float dayWidth = (float) canvasWidth / (float) numDayColumns;

		int dateColumnWidth = 0;
		if (config.isShowDateColumn) {
			dateColumnWidth = config.dateColumnWidth * _fontAverageCharWidth;
		}

		int summaryColumnWidth = 0;
		if (config.isShowSummaryColumn) {
			summaryColumnWidth = config.summaryColumnWidth * _fontAverageCharWidth;
		}

		dayWidth = (float) (canvasWidth - dateColumnWidth - summaryColumnWidth) / numDayColumns;

		_calendarAllDaysRectangle = new Rectangle(dateColumnWidth, 0, (int) (7 * dayWidth), canvasHeight);

		final long todayDayId = (new Day(new DateTime())).dayId;

		// a rough guess about the max size of the label
		gc.setFont(_boldFont);
		final Point[] headerSizes = {
				gc.stringExtent("22. May 99"), //$NON-NLS-1$
				gc.stringExtent("22. May"), //$NON-NLS-1$
				gc.stringExtent("22") }; //$NON-NLS-1$
		gc.setFont(normalFont);

		final String[] headerFormats = {
				"dd. MMM yy", //$NON-NLS-1$
				"dd. MMM", //$NON-NLS-1$
				"dd" }; //$NON-NLS-1$
		String headerFormat = UI.EMPTY_STRING;

		// Find a format for the day header which fits into the rectangle available;
		int headerSizeIndex = 0;
		while (headerSizeIndex < headerSizes.length && headerSizes[headerSizeIndex].x > (dayWidth - dayLabelXOffset)) {
			headerSizeIndex++;
		}
		headerSizeIndex = Math.min(headerSizeIndex, headerSizes.length - 1); // if the cell is smaller than the shortest format (no index 'g' was found) we use the shortest format and relay on clipping
		headerFormat = headerFormats[headerSizeIndex];

		final int dayLabelWidth = headerSizes[headerSizeIndex].x;
		int dayLabelHeight = headerSizes[headerSizeIndex].y;

		long dayId = (new Day(currentDate)).dayId; // we use simple ids

		// Weeks
		for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {

			final int rowTop = rowIndex * _rowHeight;

			// Days per week
			Rectangle dayRec = null;
			final LocalDate week1stDay = LocalDate.of(
					currentDate.getYear(),
					currentDate.getMonthOfYear(),
					currentDate.getDayOfMonth()); // save the first day of this week as a pointer to this week

			if (dateColumnWidth > 0) {

				final Rectangle infoRec = new Rectangle(0, rowTop, dateColumnWidth, _rowHeight);
				drawWeek_Date(gc, currentDate, infoRec, config.dateColumnContent);
			}

			for (int dayIndex = 0; dayIndex < 7; dayIndex++) {

				final int posX = dateColumnWidth + (int) (dayIndex * dayWidth);
				final int posXNext = dateColumnWidth + (int) ((dayIndex + 1) * dayWidth);

				dayRec = new Rectangle(posX, rowTop, (posXNext - posX), _rowHeight);
				final Day day = new Day(dayId);
				_dayFocus.add(new ObjectLocation(dayRec, dayId, day));
				dayId = day.dayId + 1;
				final int weekDay = currentDate.getDayOfWeek();

				gc.setBackground(_white);

				// Day background rectangle
				if ((currentDate.getMonthOfYear() % 2) == 1) {

					gc.setBackground(alternate);
					gc.fillRectangle(dayRec.x, dayRec.y + 1, dayRec.width, dayRec.height - 1);
				}

				data = _dataProvider.getCalendarDayData(
						currentDate.getYear(),
						currentDate.getMonthOfYear(),
						currentDate.getDayOfMonth());

				// Day header box
				if (config.isShowDayHeader) {

					Color headerColor;
					if (day.dayId == todayDayId) {
						headerColor = (_blue);
					} else if (weekDay == DateTimeConstants.SATURDAY || weekDay == DateTimeConstants.SUNDAY) {
						headerColor = (_red);
					} else {
						headerColor = (_darkGray);
					}

					// gradient background
					gc.setForeground(_gray);
					gc.fillGradientRectangle(posX, rowTop, dayRec.width + 1, dayLabelHeight, true); // no clue why I've to add 1 to the width, looks like a bug on Linux and does not hurt as we overwrite with the vertial line at the end anyway

					// day header label
					gc.setFont(_boldFont);
					gc.setForeground(headerColor);
					gc.setClipping(posX, rowTop, dayRec.width, dayLabelHeight); // this clipping should only kick in if shortest label format is still longer than the cell width

					gc.drawText(
							currentDate.toString(headerFormat),
							posXNext - dayLabelWidth - dayLabelXOffset,
							rowTop,
							true);

					gc.setFont(normalFont);
					gc.setClipping(_nullRec);

				} else {

					dayLabelHeight = 0;
				}

				drawDayTours(
						gc,
						data,
						new Rectangle(
								dayRec.x,
								dayRec.y + dayLabelHeight,
								dayRec.width,
								dayRec.height - dayLabelHeight));

				currentDate = currentDate.plusDays(1);
			}

			if (summaryColumnWidth > 0) {

				final int X1 = dateColumnWidth + (int) (7 * dayWidth);
				final int X2 = X1 + summaryColumnWidth;

				final Rectangle weekRec = new Rectangle(X1, rowTop, (X2 - X1), _rowHeight);

				final CalendarTourData weekSummary = _dataProvider.getCalendarWeekSummaryData(
						week1stDay,
						_calendarView);

				if (weekSummary.loadingState == LoadingState.IS_LOADED && weekSummary.numTours > 0) {
					drawWeek_Summary(gc, weekSummary, weekRec);
				}
			}

		}
		gc.setFont(normalFont);

		// and finally the vertical lines
		gc.setForeground(_display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		for (int i = 0; i <= 7; i++) {
			gc.drawLine(
					dateColumnWidth + (int) (i * dayWidth),
					0,
					dateColumnWidth + (int) (i * dayWidth),
					canvasHeight);
		}

		// draw the selection on top of our calendar graph image so we can reuse that image
		_highlight = new Image(getDisplay(), canvasWidth, canvasHeight);
		gc = new GC(_highlight);
		gc.drawImage(_image, 0, 0);
		drawSelection(gc);
		oldGc.drawImage(_highlight, 0, 0);
		_highlight.dispose();

		oldGc.dispose();
		gc.dispose();

		updateUI_YearMonth(currentDate);

		_isGraphClean = true;
	}

	private void drawDayTours(final GC gc, final CalendarTourData[] data, final Rectangle rec) {

		int max;

		if (0 == _numToursPerDay) {
			max = data.length;
		} else {
			max = _isDynamicTourFieldSize ? data.length : Math.min(_numToursPerDay, data.length);
		}

		for (int i = 0; i < max; i++) {

			int ddy;

			if (0 == _numToursPerDay) {
				ddy = rec.height / data.length;
			} else {
				final int dy = rec.height / _numToursPerDay;
				// narrow the tour fields to fit if more than _numberOfTourPerDay tours
				ddy = _isDynamicTourFieldSize ? (data.length <= _numToursPerDay ? dy : (_numToursPerDay * dy)
						/ data.length) : dy;
			}

			// final Rectangle tour = new Rectangle(rec.x + 1, rec.y + rec.height - (i + 1) * ddy, (rec.width - 2), ddy - 1);
			final int k = max - i; // morning top, evening button
			final Rectangle tour = new Rectangle(rec.x + 1, rec.y + rec.height - k * ddy, (rec.width - 2), ddy - 1);
			final Rectangle focus = new Rectangle(tour.x - 1, tour.y - 1, tour.width + 2, tour.height + 2);

			_tourFocus.add(new ObjectLocation(focus, data[i].tourId, data[i]));

			drawTour_Info(gc, tour, data[i], false);
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

					drawHighlightedTour(gc, (CalendarTourData) (ol.o), ol.rect);

				} else if (ol.o instanceof Day) {

					// gc.setAlpha(0xd0); // like statistics
					gc.setAlpha(0xa0);
					gc.setBackground(_white);
					gc.setForeground(_gray);
					gc.fillGradientRectangle(
							ol.rect.x - 4,
							ol.rect.y - 4,
							ol.rect.width + 9,
							ol.rect.height + 9,
							false);
					gc.drawRoundRectangle(ol.rect.x - 5, ol.rect.y - 5, ol.rect.width + 10, ol.rect.height + 10, 6, 6);
					gc.setAlpha(0xFF);
				}

				return;
			}
		}
	}

	private void drawHighlightedTour(final GC gc, final CalendarTourData data, final Rectangle rec) {

		gc.setAlpha(0xd0);
		gc.setBackground(_colorCache.getColor(_rgbBright.get(data.typeColorIndex).hashCode()));
		gc.setForeground(_colorCache.getColor(_rgbDark.get(data.typeColorIndex).hashCode()));
		gc.fillGradientRectangle(rec.x - 4, rec.y - 4, rec.width + 9, rec.height + 9, false);
		gc.setForeground(_colorCache.getColor(_rgbLine.get(data.typeColorIndex).hashCode()));
		gc.drawRoundRectangle(rec.x - 5, rec.y - 5, rec.width + 10, rec.height + 10, 6, 6);
		gc.setAlpha(0xFF);

		// focus is 1 pixel larger than tour rectangle
		final Rectangle r = new Rectangle(rec.x + 1, rec.y + 1, rec.width - 2, rec.height - 2);

		// only fill in text if the tour rectangle has a reasonable size
		Color color;
		if (_useBlackForHighlightTourInfoText) {
			color = _black;
		} else {
			color = _colorCache.getColor(_rgbText.get(data.typeColorIndex).hashCode());
		}

		drawTour_InfoText(gc, r, data, color);

	}

	private void drawSelectedDay(final GC gc, final Rectangle r) {

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

	private void drawSelectedTour(final GC gc, final CalendarTourData data, final Rectangle rec) {

		final Color lineColor = _colorCache.getColor(_rgbLine.get(data.typeColorIndex).hashCode());

		// - red box -
		gc.setBackground(_red);
		// gc.setBackground(lineColor);
		gc.fillRectangle(rec.x - 4, rec.y - 4, rec.width + 9, rec.height + 9);
		// gc.setForeground(_red);
		gc.setForeground(lineColor);
		gc.drawRoundRectangle(rec.x - 5, rec.y - 5, rec.width + 10, rec.height + 10, 6, 6);

		// focus is 1 pixel larger than tour rectangle
		final Rectangle r = new Rectangle(rec.x + 1, rec.y + 1, rec.width - 2, rec.height - 2);
		drawTour_Info(gc, r, data, true);
		return;

	}

	private void drawSelection(final GC gc) {

		if (!_lastSelection.equals(_selectedItem)) {
			fireSelectionEvent(_selectedItem);
			_lastSelection = _selectedItem;
		}

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
					drawSelectedTour(gc, (CalendarTourData) (ol.o), ol.rect);
				} else if (ol.o instanceof Day) {
					drawSelectedDay(gc, ol.rect);
				}
				return;
			}
		}
	}

	private void drawTour_Info(final GC gc, final Rectangle r, final CalendarTourData data, final boolean highlight) {

		final Color line = _colorCache.getColor(_rgbLine.get(data.typeColorIndex).hashCode());

		gc.setForeground(line);
		gc.drawRectangle(r);

		final RGB rgbBackground = _rgbBright.get(data.typeColorIndex);

		gc.setBackground(_colorCache.getColor(rgbBackground.hashCode()));
		gc.setForeground(_colorCache.getColor(_rgbDark.get(data.typeColorIndex).hashCode()));

		gc.fillGradientRectangle(r.x + 1, r.y + 1, r.width - 1, r.height - 1, false);

		// only fill in text if the tour rectangle has a reasonable size
		Color fg;

		if (highlight && _useBlackForHighlightTourInfoText) {
			fg = _black;
		} else if (_useTextColorForTourInfoText) {
			fg = _colorCache.getColor(_rgbText.get(data.typeColorIndex).hashCode());
		} else {

			final RGB contrastRGB = ColorUtil.getContrastRGB(rgbBackground);

			fg = _colorCache.getColor(contrastRGB.hashCode());
		}

		drawTour_InfoText(gc, r, data, fg);

	}

	private void drawTour_InfoText(final GC gc, final Rectangle r, final CalendarTourData data, final Color fg) {

		gc.setForeground(fg);
		gc.setClipping(r.x + 1, r.y, r.width - 2, r.height);

		int y = r.y + 1;
		final int minToShow = (2 * _fontHeight / 3);

		String prevInfo = null;

		for (int formatterIndex = 0; formatterIndex < _tourInfoFormatter.length //
				&& y < r.y + r.height - minToShow; formatterIndex++) {

			final String info = _tourInfoFormatter[formatterIndex].format(data);

			// Prevent that the same text is displayed multiple times.
			if (info.length() > 0 && info.equals(prevInfo) == false) {

				// this is another text

				gc.drawText(info, r.x + 2, y, true);

				y += _fontHeight;
			}

			prevInfo = info;
		}

		gc.setClipping(_nullRec);
	}

	private void drawWeek_Date(	final GC gc,
								final DateTime dt,
								final Rectangle rec,
								final DateColumnContent dateColumnContent) {

		final Font normalFont = gc.getFont();

		gc.setForeground(_darkGray);
		gc.setBackground(_white);

		gc.setFont(_boldFont);

		final int posX = rec.x + 4;
		final int posY = rec.y + 2;

		switch (dateColumnContent) {
		case MONTH:

			// draw month

			if (dt.minusDays(1).getMonthOfYear() != dt.plusDays(6).getMonthOfYear()) {

				// a new month started on this week

				// prevent overlapping
				if (posY > _lastWeekDateYPos + _fontHeight - 2) {

					final String monthText = dt.plusDays(6).toString("MMM"); //$NON-NLS-1$

					gc.drawText(monthText, posX, posY);

					_lastWeekDateYPos = posY;
				}
			}

			break;

		case YEAR:

			final int year = dt.getYear();

			// prevent to repeat the year
			if (year != _lastWeekDateYear) {

				// prevent overlapping
				if (posY > _lastWeekDateYPos + _fontHeight - 2) {

					gc.drawText(Integer.toString(year), posX, posY);

					_lastWeekDateYear = year;
					_lastWeekDateYPos = posY;
				}
			}

			break;

		case WEEK_NUMBER:
		default:

			// default is week number

			// prevent overlapping
			if (posY > _lastWeekDateYPos + _fontHeight - 2) {

				gc.drawText(Integer.toString(dt.getWeekOfWeekyear()), posX, posY);

				_lastWeekDateYPos = posY;
			}

			break;
		}

		gc.setFont(normalFont);
	}

	private void drawWeek_Summary(final GC gc, final CalendarTourData data, final Rectangle weekRec) {

		gc.setClipping(weekRec);
		gc.setBackground(_white);

		final int xr = weekRec.x + weekRec.width - 1;
		final int xl = weekRec.x + 2;
		int xx;
		int y = weekRec.y + 1;
		String text;
		final boolean doClip = true;

		Point extent;
		final int maxLength = weekRec.width - 2;

		final Font normalFont = gc.getFont();

		gc.setFont(_boldFont);

		for (final WeekSummaryFormatter formatter : _weekSummaryFormatter) {

			gc.setForeground(_colorCache.getColor(formatter.getColor().hashCode()));

			text = formatter.format(data);

			if (text.length() > 0 && y < (weekRec.y + weekRec.height)) {

				extent = gc.stringExtent(text);
				xx = xr - extent.x;

				if (extent.x > maxLength) {
					if (doClip && text.contains(UI.SPACE1)) {
						text = text.substring(0, text.lastIndexOf(UI.SPACE));
						xx = xr - gc.stringExtent(text).x;
					} else {
						xx = xl;
					}
				}

				gc.drawText(text, xx, y);
			}

			y += _fontHeight;
		}

		gc.setFont(normalFont);
		gc.setClipping(_nullRec);
	}

	void fireSelectionEvent(final CalendarItem selection) {

		final Object[] listeners = _selectionProvider.getListeners();
		for (final Object listener2 : listeners) {
			final ICalendarSelectionProvider listener = (ICalendarSelectionProvider) listener2;
			SafeRunnable.run(new SafeRunnable() {
				@Override
				public void run() {
					listener.selectionChanged(selection);
				}
			});
		}
	}

	void fireSelectionEvent(final SelectionType type, final long id) {

		final Object[] listeners = _selectionProvider.getListeners();
		for (final Object listener2 : listeners) {
			final ICalendarSelectionProvider listener = (ICalendarSelectionProvider) listener2;
			SafeRunnable.run(new SafeRunnable() {
				@Override
				public void run() {
					listener.selectionChanged(_selectedItem);
				}
			});
		}
	}

	@Override
	public ArrayList<TourData> getAllSelectedTours() {
		return getSelectedTours();
	}

	/**
	 * @return Returns the first day which is displayed in the calendar view.
	 */
	LocalDate getCalendarFirstDay() {
		return _calendarFirstDay;
	}

	/**
	 * @return Returns the last day which is displayed in the calendar view.
	 */
	LocalDate getCalendarLastDay() {
		return _calendarLastDay;
	}

	public DateTime getFirstDay() {
		return _firstDay;
	}

	private int getFirstDayOfWeek() {

		final int firstDayOfWeek = _prefStore.getInt(ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);

//		int firstDayOfWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);
//		firstDayOfWeek--; // the prefStore is using Calendar Constants (SO=1 ... SA=7) we are using Joda (MO=1 .. SO=7)
//		if (firstDayOfWeek < 1) {
//			firstDayOfWeek = 7;
//		}

		return firstDayOfWeek;

	}

	/**
	 * @return Returns the hovered tour or <code>null</code> when a tour is not hovered.
	 */
	CalendarItem getHoveredTour() {

		return _hoveredTour;
	}

	public int getNumberOfToursPerDay() {
		return _numToursPerDay;
	}

	public long getSelectedTourId() {
		if (_selectedItem.isTour()) {
			return _selectedItem.id;
		} else {
			return _noItem.id;
		}
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();
		if (_selectedItem.isTour()) {
			selectedTourData.add(TourManager.getInstance().getTourData(_selectedItem.id));
		}
		return selectedTourData;
	}

	public int getTourInfoFormatterIndex(final int line) {
		return _tourInfoFormatter[line].index;
	}

	public boolean getTourInfoUseHighlightTextBlack() {
		return _useBlackForHighlightTourInfoText;
	}

	public boolean getTourInfoUseTextColor() {
		return _useTextColorForTourInfoText;
	}

	public int getWeekSummaryFormatter(final int line) {
		return _weekSummaryFormatter[line].index;
	}

	public void gotoDate(final DateTime dt) {

		_firstDay = dt;
		_firstDay = _firstDay.minusWeeks(_numVisibleWeeks / 2); // center date on screen
		_firstDay = _firstDay.withDayOfWeek(getFirstDayOfWeek()); // set first day to start of week

		_isGraphClean = false;

		redraw();
		scrollBarUpdate();
	}

	public void gotoFirstTour() {

		_firstDay = _dataProvider.getFirstDateTime();
		gotoDate(_firstDay);
	}

	public void gotoMonth(final int month) {

		_firstDay = _firstDay.withMonthOfYear(month); // scroll to first day of the week containing the first day of this month
		_firstDay = _firstDay.withDayOfMonth(1);
		_firstDay = _firstDay.withDayOfWeek(getFirstDayOfWeek()); // set first day to start of week

		_isGraphClean = false;

		redraw();
		scrollBarUpdate();
	}

	public void gotoNextScreen() {

		_firstDay = _firstDay.plusWeeks(_numVisibleWeeks);
		_isGraphClean = false;

		redraw();
		scrollBarUpdate();
	}

	public void gotoNextTour() {
		gotoTourOffset(+1);
	}

	public void gotoNextWeek() {

		_firstDay = _firstDay.plusWeeks(1);
		_isGraphClean = false;

		redraw();
		scrollBarUpdate();
	}

	public void gotoPrevScreen() {

		_firstDay = _firstDay.minusWeeks(_numVisibleWeeks);
		_isGraphClean = false;

		redraw();
		scrollBarUpdate();
	}

	public void gotoPrevTour() {
		gotoTourOffset(-1);
	}

	public void gotoPrevWeek() {

		_firstDay = _firstDay.minusWeeks(1);
		_isGraphClean = false;

		redraw();
		scrollBarUpdate();
	}

	public void gotoToday() {

		_firstDay = new DateTime();

		final Days d = Days.daysBetween(new DateTime(0), _firstDay);
		_selectedItem = new CalendarItem(d.getDays(), SelectionType.DAY);

		gotoDate(_firstDay);

	}

	public void gotoTourId(final long tourId) {

		final DateTime dt = _dataProvider.getCalendarTourDateTime(tourId);

		_selectedItem = new CalendarItem(tourId, SelectionType.TOUR);

		if (dt.isBefore(_firstDay) || dt.isAfter(_firstDay.plusWeeks(_numVisibleWeeks))) {
			_isGraphClean = false;
			gotoDate(dt);
		} else {
			redraw();
		}
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
				_selectedItem = new CalendarItem(_tourFocus.get(0).id, SelectionType.TOUR);
				offset--;
			} else {
				_selectedItem = new CalendarItem(
						_tourFocus.get(_tourFocus.size() - 1).id,
						SelectionType.TOUR);
				offset++;
			}
		}

		boolean visible = false;
		int index = 0;
		for (final ObjectLocation ol : _tourFocus) {
			if (_selectedItem.id == ol.id) {
				visible = true; // the selection is visible
				break;
			}
			index++;
		}
		if (!visible) { // if we are scrolling tours forward and the selection got invisible start at the first tour again
			if (offset > 0) {
				index = -1;
			}
		}
		final int newIndex = index + offset;
		if (newIndex < 0) {
			gotoPrevWeek();
			return;
		} else if (newIndex >= _tourFocus.size()) {
			gotoNextWeek();
			return;
		} else {
			_selectedItem = new CalendarItem(_tourFocus.get(newIndex).id, SelectionType.TOUR);
			_isGraphClean = false;
			redraw();
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
				_selectedItem = new CalendarItem(_tourFocus.get(0).id, SelectionType.TOUR);
			} else {
				_selectedItem = new CalendarItem(
						_tourFocus.get(_tourFocus.size() - 1).id,
						SelectionType.TOUR);
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

		int dayOfWeekToGoTo = -1;
		if (null != ctd) {
			dayOfWeekToGoTo = ctd.dayOfWeek;
		} else if (_lastDayOfWeekToGoTo >= 0) { // selection scrolled out of view
			dayOfWeekToGoTo = _lastDayOfWeekToGoTo;
			index = direction > 0 ? 0 : _tourFocus.size();
		}

		if (dayOfWeekToGoTo >= 0) {
			index += direction;
			for (int i = index; i >= 0 && i < _tourFocus.size(); i += direction) {
				final ObjectLocation ol = _tourFocus.get(i);
				if (dayOfWeekToGoTo == ((CalendarTourData) (ol.o)).dayOfWeek) {
					_selectedItem = new CalendarItem(ol.id, SelectionType.TOUR);
					_lastDayOfWeekToGoTo = dayOfWeekToGoTo;
					redraw();
					return;
				}
			}
		} else {
			// selected Item is not on the screen any more
			_selectedItem = _noItem;
		}

		if (direction < 0) {
			gotoPrevWeek();
		} else {
			gotoNextWeek();
		}

	}

	public void gotoYear(final int year) {

		_firstDay = _firstDay.withYear(year);
		// scroll to first day of the week
		_firstDay = _firstDay.withDayOfWeek(getFirstDayOfWeek());

		_isGraphClean = false;

		redraw();
		scrollBarUpdate();
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

		final CalendarItem oldHighlight = _highlightedItem;
		final CalendarItem oldSelection = _selectedItem;

		if (1 == event.button || 3 == event.button) {
			_selectedItem = _noItem;
		}

		// reset hover state
		_hoveredTour = null;

		boolean isTourHovered = false;
		boolean isDayHovered = false;

		for (final ObjectLocation ol : _tourFocus) {
			if (ol.rect.contains(event.x, event.y)) {

				final long id = ol.id;

				if (1 == event.button || 3 == event.button) {

					_selectedItem = new CalendarItem(id, SelectionType.TOUR);

				} else if (oldHighlight.id != id) { // a new object is highlighted

					_highlightedItem = new CalendarItem(id, SelectionType.TOUR);
				}

				_hoveredTour = new CalendarItem(id, SelectionType.TOUR);
				_hoveredTour.itemRectangle = ol.rect;

				isTourHovered = true;

				break;
			}
		}

		if (!isTourHovered) {
			for (final ObjectLocation ol : _dayFocus) {

				if (ol.rect.contains(event.x, event.y)) {

					final long id = ol.id;

					if (1 == event.button || 3 == event.button) {

						_selectedItem = new CalendarItem(id, SelectionType.DAY);

					} else if (oldHighlight.id != id) { // a new object is highlighted

						_highlightedItem = new CalendarItem(id, SelectionType.DAY);
					}

					isDayHovered = true;

					break;
				}
			}
		}

		if (!oldSelection.equals(_selectedItem)) { // highlight selection -> redraw calendar

			redraw();
			return;
		}

		if (!isDayHovered && !isTourHovered) { // only draw base calendar, skip highlighting

			redraw();
			return;
		}

		_isHighlightChanged = !oldHighlight.equals(_highlightedItem);
		if (_isHighlightChanged) { // only draw the highlighting on top of the calendar image
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

		if (_scrollBarShift != 0) {

			if (_scrollBarLastSelection == selectableMax) {
				sb.setSelection(selectableMax);
			} else if (_scrollBarLastSelection == selectableMin) {
				sb.setSelection(selectableMin);
			}

			if (change > 0 && _scrollBarShift < 0) {

				// ensure we are not shifting over "0"
				_scrollBarShift = Math.min(0, _scrollBarShift + change);

			} else if (change < 0 && _scrollBarShift > 0) {

				_scrollBarShift = Math.max(0, _scrollBarShift + change);

			} else {
				_scrollBarShift += change;
			}

		} else {

			// do we need start shifting the scroll bar ?

			if (selection < selectableMin) {

				// we are at the upper border

				sb.setSelection(selectableMin);
				_scrollBarShift += change;
			}

			if (selection > selectableMax) {

				// we are at the lower border

				sb.setSelection(selectableMax);
				_scrollBarShift += change;

			}
		}

		_scrollBarLastSelection = sb.getSelection();

		// goto the selected week
		_firstDay = scrollBarStart().plusDays(selection * 7);

		_isGraphClean = false;
		redraw();
	}

	public void refreshCalendar() {

		_dataProvider.invalidate();
		_isGraphClean = false;

		redraw();
	}

	public void removeSelection() {

		if (!_selectedItem.equals(_noItem)) {

			_selectedItem = _noItem;
			_isGraphClean = false;

			redraw();
		}
	}

	void removeSelectionListener(final ICalendarSelectionProvider listener) {
		_selectionProvider.remove(listener);
	}

	private DateTime scrollBarEnd() {

		final DateTime dt = new DateTime().plusWeeks(_scrollBarShift);

		// ensure the date return is a "FirstDayOfTheWeek" !!!
		final DateTime endDate = dt.plusWeeks(1).withDayOfWeek(getFirstDayOfWeek());

		return endDate;
	}

	private DateTime scrollBarStart() {

		// DateTime dt = _dataProvider.getFirstDateTime().plusWeeks(_scrollBarShift);
		DateTime dt = _dataProvider.getFirstDateTime();
		final DateTime now = new DateTime();
		final int weeks = (int) ((now.getMillis() - dt.getMillis()) / _WEEK_MILLIS);
		if (weeks < _MIN_SCROLLABLE_WEEKS) { // ensure the scrollable area has a reasonable size
			dt = now.minusWeeks(_MIN_SCROLLABLE_WEEKS);
		}
		dt = dt.plusWeeks(_scrollBarShift);

		// ensure the date return is a "FirstDayOfTheWeek" !!!
		final DateTime startDate = dt.minusWeeks(1).withDayOfWeek(getFirstDayOfWeek());

		return startDate;
	}

	private void scrollBarUpdate() {

		_scrollBarShift = 0;

		final ScrollBar sb = _parent.getVerticalBar();

		final long dt1 = scrollBarStart().getMillis();
		final long dt2 = _firstDay.getMillis();
		final long dt3 = scrollBarEnd().getMillis();

		int maxWeeks = (int) ((dt3 - dt1) / _WEEK_MILLIS);
		final int thumbSize = Math.max(_numVisibleWeeks, maxWeeks / 20); // ensure the thumb isn't getting to small

		int thisWeek;

		if (dt2 < dt1) {

			// shift negative

			_scrollBarShift = (int) ((dt2 - dt1) / _WEEK_MILLIS);
			thisWeek = 1;

		} else if (dt2 > dt3) {

			// shift positive

			_scrollBarShift = (int) ((dt2 - dt3) / _WEEK_MILLIS);
			thisWeek = maxWeeks - 1;

		} else {

			thisWeek = (int) ((dt2 - dt1) / _WEEK_MILLIS);
		}

		maxWeeks += thumbSize;

		sb.setMinimum(0);
		sb.setMaximum(maxWeeks);
		sb.setThumb(thumbSize);
		sb.setPageIncrement(_numVisibleWeeks);
		sb.setSelection(thisWeek);

		_scrollBarLastSelection = thisWeek;
	}

	public void setFirstDay(final DateTime dt) {

		_firstDay = dt;

//		_graphClean = false;
//		redraw();
//		scrollBarUpdate();
	}

	void setLinked(final boolean linked) {
		if (false == linked) {
			_selectedItem = _noItem;
			_isGraphClean = false;
			redraw();
		}
	}

	void setNumberOfToursPerDay(final int numberOfToursPerDay) {

		_numToursPerDay = numberOfToursPerDay;
		_isGraphClean = false;

		redraw();
	}

	public void setSelectionTourId(final long selectedTourId) {
		this._selectedItem = new CalendarItem(selectedTourId, SelectionType.TOUR);
	}

	void setTourFieldSizeDynamic(final boolean dynamicTourFieldSize) {
		_isGraphClean = false;
		_isDynamicTourFieldSize = dynamicTourFieldSize;
		redraw();
	}

	public void setTourInfoFormatter(final int line, final TourInfoFormatter formatter) {
		_tourInfoFormatter[line] = formatter;
		_isGraphClean = false;
		redraw();
	};

	public void setTourInfoUseHighlightTextBlack(final boolean checked) {
		_useBlackForHighlightTourInfoText = checked;
		// as this is only affecting the highlightning no redraw should be necessary
		// _graphClean = false;
		// redraw();
	}

	public void setTourInfoUseLineColor(final boolean checked) {

		_useTextColorForTourInfoText = checked;
		_isGraphClean = false;

		redraw();
	}

	public void setWeekSummaryFormatter(final int line, final WeekSummaryFormatter formatter) {

		_weekSummaryFormatter[line] = formatter;
		_isGraphClean = false;

		redraw();
	}

	public void setYearMonthContributor(final CalendarYearMonthContributionItem calendarYearMonthContribuor) {
		_calendarYearMonthContributor = calendarYearMonthContribuor;

	}

	void updateTourTypeColors() {

		_rgbBright.clear();
		_rgbDark.clear();
		_rgbLine.clear();
		_rgbText.clear();

		// default colors for no tour type
		_rgbBright.add(_white.getRGB());
		_rgbDark.add(_gray.getRGB());
		_rgbLine.add(_darkGray.getRGB());
		_rgbText.add(_darkGray.getRGB());

		/*
		 * color index 1...n+1: tour type colors
		 */
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		for (final TourType tourType : tourTypes) {
			_rgbBright.add(tourType.getRGBBright());
			_rgbDark.add(tourType.getRGBDark());
			_rgbLine.add(tourType.getRGBLine());
			_rgbText.add(tourType.getRGBText());
		}
	}

	void updateUI_Layout() {

		// invalidate layout
		_isGraphClean = false;

		redraw();
	}

	private void updateUI_YearMonth(final DateTime currentDate) {
		/**
		 * Update month/year dropdown box.
		 * <p>
		 * Look at the 1st day of the week after the first day displayed because if we go to a
		 * specific month we ensure that the first day of the month is displayed in the first line,
		 * meaning the first day in calendar normally contains a day of the *previous* month
		 */
		if (_calendarYearMonthContributor.getSelectedYear() != currentDate.plusDays(7).getYear()) {
			_calendarYearMonthContributor.selectYear(currentDate.getYear());
		}
		if (_calendarYearMonthContributor.getSelectedMonth() != currentDate.plusDays(7).getMonthOfYear()) {
			_calendarYearMonthContributor.selectMonth(currentDate.getMonthOfYear());
		}
	}

}
