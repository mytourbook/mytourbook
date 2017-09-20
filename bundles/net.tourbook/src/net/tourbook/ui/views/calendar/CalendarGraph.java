/*******************************************************************************
 * Copyright (C) 2011-2015 Matthias Helmling and Contributors
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

import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.color.ColorCacheSWT;
import net.tourbook.common.color.ColorUtil;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProviderAll;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.calendar.CalendarView.TourInfoFormatter;

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
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
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
import org.joda.time.Days;

public class CalendarGraph extends Canvas implements ITourProviderAll {

	private final static IPreferenceStore		_prefStoreCommon			= CommonActivator.getPrefStore();

	final static private long					_WEEK_MILLIS				= (1000 * 60 * 60 * 24 * 7);

	final static private int					_MIN_SCROLLABLE_WEEKS		= 12;

	private Composite							_parent;

	private Display								_display					= Display.getCurrent();

	private final TourDoubleClickState			_tourDoubleClickState		= new TourDoubleClickState();
	private ColorCacheSWT						_colorCache					= new ColorCacheSWT();
	private Color								_black						= _display.getSystemColor(SWT.COLOR_BLACK);
	private Color								_gray						= _display.getSystemColor(SWT.COLOR_GRAY);
	private Color								_white						= _display.getSystemColor(SWT.COLOR_WHITE);
	private Color								_red						= _display.getSystemColor(SWT.COLOR_RED);

//	private NavigationStyle						_navigationStyle	= NavigationStyle.PHYSICAL;

	//	private Color								_green				= _display.getSystemColor(SWT.COLOR_GREEN);
	private Color								_blue						= _display.getSystemColor(SWT.COLOR_BLUE);
	//	private Color								_magenta				= _display.getSystemColor(SWT.COLOR_MAGENTA);
//	private Color								_cyan					= _display.getSystemColor(SWT.COLOR_CYAN);
//	private Color								_darkGray				= _display.getSystemColor(SWT.COLOR_DARK_GRAY);
	private Color								_darkGray					= _colorCache.getColor(0x404040);
	private ArrayList<RGB>						_rgbBright;
	private ArrayList<RGB>						_rgbDark;

	private ArrayList<RGB>						_rgbLine;
	private ArrayList<RGB>						_rgbText;
	private DateTime							_dt							= new DateTime();
	private DateTime							_dt_normal					= _dt;
	private DateTime							_dt_tiny					= _dt;
	private int									_numWeeksNormalView			= 5;

	private int									_numWeeksTinyView			= 10;
	private int									_lastDayOfWeekToGoTo		= -1;

	private List<ObjectLocation>				_tourFocus;
	private List<ObjectLocation>				_dayFocus;
	private Image								_image						= null;
	private Image								_highlight					= null;

	private boolean								_highlightChanged			= false;
	private boolean								_isGraphClean				= false;
	private Selection							_noItem						= new Selection(
			new Long(-1),
			SelectionType.NONE);
	private Selection							_selectedItem				= _noItem;

	private Selection							_highlightedItem			= _noItem;

	private Selection							_lastSelection				= _noItem;
	final private Rectangle						_nullRec					= null;

	private CalendarTourDataProvider			_dataProvider;

	private CalendarYearMonthContributionItem	_calendarYearMonthContributor;
	private ListenerList						_selectionProvider			= new ListenerList();
	private int									_scrollBarShift;

	private int									_scrollBarLastSelection;
	private boolean								_scrollDebug				= false;

	private int									_numberOfToursPerDay		= 3;
	private boolean								_dynamicTourFieldSize		= true;

	private TourInfoFormatter[]					_tourInfoFormatter			=
			new TourInfoFormatter[CalendarView.numberOfInfoLines];
	private WeekSummaryFormatter[]				_weekSummaryFormatter		=
			new WeekSummaryFormatter[CalendarView.numberOfSummaryLines];

	private boolean								_useTextColorForTourInfoText;
	private boolean								_useBlackForHighlightTourInfoText;

	private Rectangle							_calendarAllDaysRectangle;
//	private Rectangle _calendarFirstWeekRectangle;
//	private Rectangle _calendarLastWeekRectangle;

	private String								_refText					= "Tour12";									//$NON-NLS-1$
	private boolean								_tinyLayout					= false;

	private boolean								_showDayNumberInTinyView	= false;
	Point										_refTextExtent;

	/**
	 * Cache font height;
	 */
	private int									_fontHeight;

	private class Day {

		private long dayId;

		Day(final DateTime date) {
			final Days d = Days.daysBetween(new DateTime(0), date);
			// this.dayId = (long) date.getYear() * 1000 + date.getDayOfYear();
			this.dayId = d.getDays();
		}

		Day(final long dayId) {
			this.dayId = dayId;
		}

	};

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
			return (id > 0 && type == SelectionType.TOUR);
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
		_rgbText = new ArrayList<RGB>();

		updateTourTypeColors();

//		final DateTime dt = new DateTime();
//
//		gotoYear(dt.getYear());
//		gotoMonth(dt.getMonthOfYear());

		addListener();

	}

	private void addListener() {

		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent event) {
				drawCalendar(event.gc);
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
//					switch (_navigationStyle) {
//					case PHYSICAL:
//						gotoTourOtherWeekday(-1);
//						break;
//					case LOGICAL:
//						gotoPrevTour();
//						break;
//					}
					break;
				case SWT.ARROW_RIGHT:
				case 'l':
					gotoNextTour();
//					switch (_navigationStyle) {
//					case PHYSICAL:
//						gotoTourOtherWeekday(+1);
//						break;
//					case LOGICAL:
//						gotoNextTour();
//						break;
//					}
					break;
				case SWT.ARROW_UP:
				case 'k':
					if (_selectedItem.isTour()) {
						gotoTourSameWeekday(-1);
					} else {
						gotoPrevWeek();
					}
//					switch (_navigationStyle) {
//					case PHYSICAL:
//						gotoTourSameWeekday(-1);
//						break;
//					case LOGICAL:
//						gotoPrevWeek();
//						break;
//					}
					break;
				case SWT.ARROW_DOWN:
				case 'j':
					if (_selectedItem.isTour()) {
						gotoTourSameWeekday(+1);
					} else {
						gotoNextWeek();
					}
//					switch (_navigationStyle) {
//					case PHYSICAL:
//						gotoTourSameWeekday(+1);
//						break;
//					case LOGICAL:
//						gotoNextWeek();
//						break;
//					}
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
				case 'i':
					zoomIn();
					break;
				case 'o':
					zoomOut();
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

//		addMouseWheelListener(new MouseWheelListener() {
//			@Override
//			public void mouseScrolled(final MouseEvent event) {
//				Point p = new Point(event.x, event.y);
//				if (_calendarDaysRectangle.contains(p)) {
//					gotoNextTour();
//				} else {
//					gotoNextWeek();
//				}
//			}
//		});

		addListener(SWT.MouseWheel, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				final Point p = new Point(event.x, event.y);
				if (_calendarAllDaysRectangle.contains(p)) {
					if (event.count > 0) {
//						if (_calendarFirstWeekRectangle.contains(p) || _calendarLastWeekRectangle.contains(p)) {
//							gotoTourSameWeekday(-1);
//						} else {
//							gotoPrevTour();
//						}
						if (_selectedItem.isTour()) {
							gotoPrevTour();
						} else {
							gotoPrevWeek();
						}
					} else {
//						if (_calendarFirstWeekRectangle.contains(p) || _calendarLastWeekRectangle.contains(p)) {
//							gotoTourSameWeekday(1);
//						} else {
//							gotoNextTour();
//						}
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

	private void drawCalendar(GC gc) {

		final int dayLabelXOffset = 1;

		final int XX = getSize().x;
		final int YY = getSize().y;

//		System.out.println(_graphClean ? "clean!" : "NOT clean!");
//		System.out.println(_highlightChanged ? "HL changed!" : "HL NOT changed");
//		System.out.println("-----------");

		_fontHeight = gc.getFontMetrics().getHeight();

		if (_isGraphClean && _image != null) {

			final GC oldGc = gc;
			_highlight = new Image(getDisplay(), XX, YY);
			gc = new GC(_highlight);
			gc.drawImage(_image, 0, 0);

			drawSelection(gc);

			if (_highlightChanged) {
				drawHighLight(gc);
				_highlightChanged = false;
			}

			gc.dispose();
			oldGc.drawImage(_highlight, 0, 0);
			_highlight.dispose();
			return;
		}

		if (_scrollDebug) {
			System.out.println("Drawing year: " + _dt.getYear() + " week: " + _dt.getWeekOfWeekyear()); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (_calendarYearMonthContributor.getSelectedYear() != date.plusDays(7).getYear()) {
			_calendarYearMonthContributor.selectYear(date.getYear());
		}
		if (_calendarYearMonthContributor.getSelectedMonth() != date.plusDays(7).getMonthOfYear()) {
			_calendarYearMonthContributor.selectMonth(date.getMonthOfYear());
		}

		final GC oldGc = gc;
		gc = new GC(_image);

		_refTextExtent = gc.stringExtent(_refText);
		final boolean oldLayout = _tinyLayout;
		_tinyLayout = (_refTextExtent.x > XX / 9); // getNumOfWeeks needs the _tinuLayout set

		if (oldLayout != _tinyLayout) { // the layout style changed, try to restore weeks and make selection visible
			if (_tinyLayout) {
				_dt_normal = _dt;
				_dt = _dt_tiny;
			} else {
				_dt_tiny = _dt;
				_dt = _dt_normal;
			}
			scrollBarUpdate();
			if (_selectedItem.id > 0) {
				switch (_selectedItem.type) {
				case DAY:
					gotoDate(new DateTime(0).plusDays(_selectedItem.id.intValue()));
					return;
				case TOUR:
					gotoTourId(_selectedItem.id);
					return;
				}
			}
		}

		final int numCols = 9; // one col left and right of the week + 7 week days
		final int numRows = getNumOfWeeks(); // number of weeks per month displayed (make sure _tinyLayout is already defined!)

		final Color alternate = _colorCache.getColor(0xf0f0f0);

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

		final float dY = (float) YY / (float) numRows;
		float dX = (float) XX / (float) numCols;

		// keep the summary column at a minimal width and hide it completely if height goes blow usable value
		final int minSummaryWidth = _refTextExtent.x;
		final int minSummaryHeigth = (_refTextExtent.y * 2) / 3;
		int summaryWidth = 0;
		if (dY > minSummaryHeigth) {
			if (dX < minSummaryWidth) {
				summaryWidth = minSummaryWidth;
			} else {
				summaryWidth = (int) dX;
			}
		}

		final int minInfoWidth = _refTextExtent.x / 2;
		final int minInfoHeigth = (_refTextExtent.y / 3);
		int infoWidth = 0;
		if (dY > minInfoHeigth) {
			if (dX < minInfoWidth) {
				infoWidth = minInfoWidth;
			} else {
				infoWidth = (int) dX;
			}
		}

		dX = (float) (XX - summaryWidth - infoWidth) / (numCols - 2);

		_calendarAllDaysRectangle = new Rectangle(infoWidth, 0, (int) (7 * dX), YY);
//		_calendarFirstWeekRectangle = new Rectangle(infoWidth, 0, (int) (7 * dX), (int) dY);
//		_calendarLastWeekRectangle = new Rectangle( infoWidth, (int) ((getNumOfWeeks() - 1) * dY), (int) (7 * dX), (int) dY);

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
		// a rough guess about the max size of the label
		final Point[] headerSizes = { gc.stringExtent("22. May 99"), //$NON-NLS-1$
				gc.stringExtent("22. May"), //$NON-NLS-1$
				gc.stringExtent("22") }; //$NON-NLS-1$
		gc.setFont(normalFont);

		final String[] headerFormats = { "dd. MMM yy", "dd. MMM", "dd" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String headerFormat = UI.EMPTY_STRING;

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
		int dayLabelHeight = headerSizes[g].y;

		// _tinyLayout = (refTextExtent.x > dX || refTextExtent.y > dY - dayLabelHeight) ? true : false;

		DateTime weekDate;
		long dayId = (new Day(date)).dayId; // we use simple ids

		// Weeks
		for (int i = 0; i < numRows; i++) {

			final int Y1 = (int) (i * dY);
			final int Y2 = (int) ((i + 1) * dY);

			// Days per week
			Rectangle dayRec = null;
			weekDate = date; // save the first day of this week as a pointer to this week

			if (infoWidth > 0) {
				final Rectangle infoRec = new Rectangle(0, Y1, infoWidth, (Y2 - Y1));
				drawWeekInfo(gc, date, infoRec);
			}

			for (int j = 0; j < 7; j++) {

				final int X1 = infoWidth + (int) (j * dX);
				final int X2 = infoWidth + (int) ((j + 1) * dX);
//				final Rectangle dayRec = new Rectangle(X1, Y1, (X2 - X1), (Y2 - Y1));
				dayRec = new Rectangle(X1, Y1, (X2 - X1), (Y2 - Y1));
				final Day day = new Day(dayId);
				_dayFocus.add(new ObjectLocation(dayRec, dayId, day));
				dayId = day.dayId + 1;
				final int weekDay = date.getDayOfWeek();

				gc.setBackground(_white);

				// Day background rectangle
				if ((date.getMonthOfYear() % 2) == 1) {
					gc.setBackground(alternate);
					gc.fillRectangle(dayRec.x, dayRec.y + 1, dayRec.width, dayRec.height - 1);
				}

				data = _dataProvider.getCalendarDayData(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth());

				// Day header box
				if (!_tinyLayout) {
					gc.setForeground(_gray);
					gc.fillGradientRectangle(X1, Y1, dayRec.width + 1, dayLabelHeight, true); // no clue why I've to add 1 to the width, looks like a bug on Linux and does not hurt as we overwrite with the vertial line at the end anyway

					// Day header label
					gc.setFont(boldFont);
					if (day.dayId == todayDayId) {
						gc.setForeground(_blue);
					} else if (weekDay == DateTimeConstants.SATURDAY || weekDay == DateTimeConstants.SUNDAY) {
						gc.setForeground(_red);
					} else {
						gc.setForeground(_darkGray);
					}
					gc.setClipping(X1, Y1, dayRec.width, dayLabelHeight); // this clipping should only kick in if shortest label format is still longer than the cell width
					gc.drawText(date.toString(headerFormat), X2 - dayLabelWidht - dayLabelXOffset, Y1, true);
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
								dayRec.height
										- dayLabelHeight));

				if (_tinyLayout && _showDayNumberInTinyView) {
					if (day.dayId == todayDayId) {
						gc.setForeground(_blue);
					} else if (weekDay == DateTimeConstants.SATURDAY || weekDay == DateTimeConstants.SUNDAY) {
						gc.setForeground(_red);
					} else {
						gc.setForeground(_darkGray);
					}
					gc.setAlpha(0x50);
					gc.setFont(boldFont);
					gc.setClipping(dayRec);
					gc.drawText(date.toString(headerFormat), X2 - dayLabelWidht - dayLabelXOffset, Y1, true);
					gc.setFont(normalFont);
					gc.setClipping(_nullRec);
					gc.setAlpha(0xFF);
				}

				date = date.plusDays(1);
			}

			if (summaryWidth > 0) {
				final int X1 = infoWidth + (int) (7 * dX);
				final int X2 = X1 + summaryWidth;
				final Rectangle weekRec = new Rectangle(X1, Y1, (X2 - X1), (Y2 - Y1));
				final CalendarTourData weekSummary = _dataProvider.getCalendarWeekSummaryData(
						weekDate.getYear(),
						weekDate.getWeekOfWeekyear());
				if (weekSummary.numTours > 0) {
					drawWeekSummary(gc, weekSummary, weekRec);
				}
			}

		}
		gc.setFont(normalFont);

		// and finally the vertical lines
		gc.setForeground(_display.getSystemColor(SWT.COLOR_GRAY));
		for (int i = 0; i <= 7; i++) {
			gc.drawLine(infoWidth + (int) (i * dX), 0, infoWidth + (int) (i * dX), YY);
		}

		// draw the selection on top of our calendar graph image so we can reuse that image
		_highlight = new Image(getDisplay(), XX, YY);
		gc = new GC(_highlight);
		gc.drawImage(_image, 0, 0);
		drawSelection(gc);
		oldGc.drawImage(_highlight, 0, 0);
		_highlight.dispose();

		boldFont.dispose();
		oldGc.dispose();
		gc.dispose();

		_isGraphClean = true;

	}

	private void drawDayTours(final GC gc, final CalendarTourData[] data, final Rectangle rec) {

		int max;

		if ((0 == _numberOfToursPerDay) || _tinyLayout) {
			max = data.length;
		} else {
			max = _dynamicTourFieldSize ? data.length : Math.min(_numberOfToursPerDay, data.length);
		}

		for (int i = 0; i < max; i++) {

			int ddy;

			if ((0 == _numberOfToursPerDay) || _tinyLayout) {
				ddy = rec.height / data.length;
			} else {
				final int dy = rec.height / _numberOfToursPerDay;
				// narrow the tour fields to fit if more than _numberOfTourPerDay tours
				ddy = _dynamicTourFieldSize ? (data.length <= _numberOfToursPerDay ? dy : (_numberOfToursPerDay * dy)
						/ data.length) : dy;
			}

			// final Rectangle tour = new Rectangle(rec.x + 1, rec.y + rec.height - (i + 1) * ddy, (rec.width - 2), ddy - 1);
			final int k = max - i; // morning top, evening button
			final Rectangle tour = new Rectangle(rec.x + 1, rec.y + rec.height - k * ddy, (rec.width - 2), ddy - 1);
			final Rectangle focus = new Rectangle(tour.x - 1, tour.y - 1, tour.width + 2, tour.height + 2);

			_tourFocus.add(new ObjectLocation(focus, data[i].tourId, data[i]));

			drawTourInfo(gc, tour, data[i], false);
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
					drawHighlightedTour(gc, (CalendarTourData) (ol.o), ol.r);
				} else if (ol.o instanceof Day) {
					// gc.setAlpha(0xd0); // like statistics
					gc.setAlpha(0xa0);
					gc.setBackground(_white);
					gc.setForeground(_gray);
					gc.fillGradientRectangle(ol.r.x - 4, ol.r.y - 4, ol.r.width + 9, ol.r.height + 9, false);
					gc.drawRoundRectangle(ol.r.x - 5, ol.r.y - 5, ol.r.width + 10, ol.r.height + 10, 6, 6);
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
		if (!_tinyLayout) {
			Color color;
			if (_useBlackForHighlightTourInfoText) {
				color = _black;
			} else {
				color = _colorCache.getColor(_rgbText.get(data.typeColorIndex).hashCode());
			}
			drawTourInfoText(gc, r, data, color);
		}

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
		drawTourInfo(gc, r, data, true);
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
					drawSelectedTour(gc, (CalendarTourData) (ol.o), ol.r);
				} else if (ol.o instanceof Day) {
					drawSelectedDay(gc, ol.r);
				}
				return;
			}
		}
	}

	private void drawTourInfo(final GC gc, final Rectangle r, final CalendarTourData data, final boolean highlight) {

		final Color line = _colorCache.getColor(_rgbLine.get(data.typeColorIndex).hashCode());

		gc.setForeground(line);
		gc.drawRectangle(r);

		final RGB rgbBackground = _rgbBright.get(data.typeColorIndex);

		gc.setBackground(_colorCache.getColor(rgbBackground.hashCode()));
		gc.setForeground(_colorCache.getColor(_rgbDark.get(data.typeColorIndex).hashCode()));

		gc.fillGradientRectangle(r.x + 1, r.y + 1, r.width - 1, r.height - 1, false);

		// only fill in text if the tour rectangle has a reasonable size
		if (!_tinyLayout) {

			Color fg;

			if (highlight && _useBlackForHighlightTourInfoText) {
				fg = _black;
			} else if (_useTextColorForTourInfoText) {
				fg = _colorCache.getColor(_rgbText.get(data.typeColorIndex).hashCode());
			} else {

//				fg = _darkGray;

				final RGB contrastRGB = ColorUtil.getContrastRGB(rgbBackground);

				fg = _colorCache.getColor(contrastRGB.hashCode());
			}

			drawTourInfoText(gc, r, data, fg);
		}

	}

	private void drawTourInfoText(final GC gc, final Rectangle r, final CalendarTourData data, final Color fg) {

		gc.setForeground(fg);
		gc.setClipping(r.x + 1, r.y, r.width - 2, r.height);

		int y = r.y + 1;
		final int minToShow = (2 * _fontHeight / 3);

		String prevInfo = null;

		for (int formatterIndex = 0; formatterIndex < _tourInfoFormatter.length && y < r.y + r.height
				- minToShow; formatterIndex++) {

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

	private void drawWeekInfo(final GC gc, final DateTime dt, final Rectangle rec) {

		final Font normalFont = gc.getFont();
		final FontData fd[] = normalFont.getFontData();
		fd[0].setStyle(SWT.BOLD);
		// fd[0].setHeight(((rec.height) * 72 / _display.getDPI().y) / 4);
		final Font boldFont = new Font(_display, fd[0]);

		gc.setForeground(_darkGray);
		gc.setBackground(_white);

		gc.setFont(boldFont);
		String text;
		if (_tinyLayout) {
			if (dt.minusDays(1).getMonthOfYear() != dt.plusDays(6).getMonthOfYear()) { // a new month started on this week
				gc.setClipping(new Rectangle(rec.x, rec.y, rec.width, 4 * rec.height)); // clipp to the room left of this month
				text = dt.plusDays(6).toString("MMM"); //$NON-NLS-1$
				if (rec.width < (2 * _refTextExtent.x / 3)) {
					text = text.substring(0, 1);
				}
				gc.drawText(text, rec.x + 2, rec.y + 2);
				gc.setClipping(_nullRec);
			}
		} else {
			gc.drawText("" + dt.getWeekOfWeekyear(), rec.x + 4, rec.y + 2);//$NON-NLS-1$
		}
		gc.setFont(normalFont);

		boldFont.dispose();

	}

	private void drawWeekSummary(final GC gc, final CalendarTourData data, final Rectangle rec) {

		gc.setClipping(rec);
		gc.setBackground(_white);
		final int xr = rec.x + rec.width - 1;
		final int xl = rec.x + 2;
		int xx;
		int y = rec.y + 1;
		String text;
		final boolean doClip = true;

//		final int minToShow = (2 * fontHeight / 3);

		Point extent;
		final int maxLength = rec.width - 2;

		final Font normalFont = gc.getFont();
		final FontData fd[] = normalFont.getFontData();
		fd[0].setStyle(SWT.BOLD);
		final Font boldFont = new Font(_display, fd[0]);

		gc.setFont(boldFont);

		for (final WeekSummaryFormatter formatter : _weekSummaryFormatter) {
			gc.setForeground(_colorCache.getColor(formatter.getColor().hashCode()));
			text = formatter.format(data);
			// if (text.length() > 0 && y < rec.y + rec.height - minToShow) {
			if (text.length() > 0 && y < (rec.y + rec.height)) {
				extent = gc.stringExtent(text);
				xx = xr - extent.x;
				if (extent.x > maxLength) {
					if (doClip && text.contains(UI.SPACE)) {
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
		boldFont.dispose();

		gc.setClipping(_nullRec);
	}

	void fireSelectionEvent(final Selection selection) {

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

	public DateTime getFirstDay() {
		return _dt;
	}

	private int getFirstDayOfWeek() {

		final int firstDayOfWeek = _prefStoreCommon.getInt(ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);

//		int firstDayOfWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);
//		firstDayOfWeek--; // the prefStore is using Calendar Constants (SO=1 ... SA=7) we are using Joda (MO=1 .. SO=7)
//		if (firstDayOfWeek < 1) {
//			firstDayOfWeek = 7;
//		}

		return firstDayOfWeek;

	}

	public int getNumberOfToursPerDay() {
		return _numberOfToursPerDay;
	}

	int getNumOfWeeks() {
		if (_tinyLayout) {
			return _numWeeksTinyView;
		} else {
			return _numWeeksNormalView;
		}
	}

	public int getNumWeeksNormalLayout() {
		return _numWeeksNormalView;
	}

	public int getNumWeeksTinyLayout() {
		return _numWeeksTinyView;
	}

	public Long getSelectedTourId() {
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

	public boolean getShowDayNumberInTinyView() {
		return _showDayNumberInTinyView;
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

		_dt = dt;
		_dt = _dt.minusWeeks(getNumOfWeeks() / 2); // center date on screen
		_dt = _dt.withDayOfWeek(getFirstDayOfWeek()); // set first day to start of week

		_isGraphClean = false;
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

		_isGraphClean = false;
		redraw();
		scrollBarUpdate();
	}

	public void gotoNextScreen() {
		_dt = _dt.plusWeeks(getNumOfWeeks());
		_isGraphClean = false;
		redraw();
		scrollBarUpdate();
	}

	public void gotoNextTour() {
		gotoTourOffset(+1);
	}

	public void gotoNextWeek() {
		_dt = _dt.plusWeeks(1);
		_isGraphClean = false;
		redraw();
		scrollBarUpdate();
	}

	public void gotoPrevScreen() {
		_dt = _dt.minusWeeks(getNumOfWeeks());
		_isGraphClean = false;
		redraw();
		scrollBarUpdate();
	}

	public void gotoPrevTour() {
		gotoTourOffset(-1);
	}

	public void gotoPrevWeek() {
		_dt = _dt.minusWeeks(1);
		_isGraphClean = false;
		redraw();
		scrollBarUpdate();
	}

	public void gotoToday() {

		_dt = new DateTime();

		final Days d = Days.daysBetween(new DateTime(0), _dt);
		_selectedItem = new Selection((long) d.getDays(), SelectionType.DAY);
		gotoDate(_dt);

	}

	public void gotoTourId(final Long tourId) {

		final DateTime dt = _dataProvider.getCalendarTourDateTime(tourId);

		_selectedItem = new Selection(tourId, SelectionType.TOUR);

		if (dt.isBefore(_dt) || dt.isAfter(_dt.plusWeeks(getNumOfWeeks()))) {
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
				_selectedItem = new Selection(_tourFocus.get(0).id, SelectionType.TOUR);
				offset--;
			} else {
				_selectedItem = new Selection(new Long(_tourFocus.get(_tourFocus.size() - 1).id), SelectionType.TOUR);
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
			_selectedItem = new Selection(_tourFocus.get(newIndex).id, SelectionType.TOUR);
			_isGraphClean = false;
			redraw();
		}

	}

	/*
	 * private void gotoTourOtherWeekday(final int direction) { if (_tourFocus.size() < 1) { if
	 * (direction < 0) { gotoPrevWeek(); } else { gotoNextWeek(); } return; } if
	 * (!_selectedItem.isTour()) { // if no tour is selected, count from first/last tour and select
	 * this tour if (direction > 0) { _selectedItem = new Selection(_tourFocus.get(0).id,
	 * SelectionType.TOUR); } else { _selectedItem = new Selection(new
	 * Long(_tourFocus.get(_tourFocus.size() - 1).id), SelectionType.TOUR); } redraw(); return; }
	 * int index = 0; CalendarTourData ctd = null; for (final ObjectLocation ol : _tourFocus) { if
	 * (_selectedItem.id == ol.id) { ctd = (CalendarTourData) (ol.o); break; } index++; } if (null
	 * == ctd) { // selected tour scrolled out of the window, select first/last tour if
	 * (_tourFocus.size() > 0) { ObjectLocation ol; if (direction > 0) { ol = _tourFocus.get(0); }
	 * else { ol = _tourFocus.get(_tourFocus.size() - 1); } _selectedItem = new Selection(ol.id,
	 * SelectionType.TOUR); redraw(); return; } } if (null != ctd) { index += direction; for (int i
	 * = index; i >= 0 && i < _tourFocus.size(); i += direction) { final ObjectLocation ol =
	 * _tourFocus.get(i); // if (ctd.dayOfWeek != ((CalendarTourData) (ol.o)).dayOfWeek) { final
	 * CalendarTourData ctdNext = (CalendarTourData) (ol.o); if (ctd.day != ctdNext.day || ctd.week
	 * != ctdNext.week || ctd.year != ctdNext.year) { _selectedItem = new Selection(ol.id,
	 * SelectionType.TOUR); redraw(); return; } } } else { // selected Item is not on the screen any
	 * more // _selectedItem = _noItem; keep selection } if (direction < 0) { gotoPrevWeek(); } else
	 * { gotoNextWeek(); } }
	 */

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
					_selectedItem = new Selection(ol.id, SelectionType.TOUR);
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

		_dt = _dt.withYear(year);
		// scroll to first day of the week
		_dt = _dt.withDayOfWeek(getFirstDayOfWeek());

		_isGraphClean = false;
		redraw();
		scrollBarUpdate();
	}

	boolean isTinyLayout() {
		return _tinyLayout;
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
//					if (oldSelection.id == ol.id) {
//						 _selectedItem = _noItem; // deselect if already selected
//					} else {
//						_selectedItem = new Selection(id, SelectionType.TOUR);
//					}
					_selectedItem = new Selection(id, SelectionType.TOUR);
				} else if (3 == event.button) {
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
//						if (oldSelection.id == ol.id) {
//							 _selectedItem = _noItem; // deselect if already selected
//						} else {
//							_selectedItem = new Selection(id, SelectionType.DAY);
//						}
						_selectedItem = new Selection(id, SelectionType.DAY);
					} else if (3 == event.button) {
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
			redraw();
			return;
		}

		if (!dayFound && !tourFound) { // only draw base calendar, skip highlighting
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
			System.out.println("Last Selection: " + _scrollBarLastSelection + " - New Selection: " + selection); //$NON-NLS-1$ //$NON-NLS-2$
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
		// final int thumbSize = Math.max(getNumWeeks(), weeks / 20); // ensure the thumb isn't getting to small
		// sb.setThumb(thumbSize);
		// weeks += thumbSize;
		// sb.setMinimum(0);
		// sb.setMaximum(weeks);
		// sb.setPageIncrement(getNumWeeks());

		if (_scrollDebug) {
			System.out.println("SbarStart: " + scrollBarStart().getWeekOfWeekyear()); //$NON-NLS-1$
			System.out.println("SbarShift: " + _scrollBarShift + " - Selected Week: " + selection); //$NON-NLS-1$ //$NON-NLS-2$
		}

		_scrollBarLastSelection = sb.getSelection();

		// goto the selected week
		_dt = scrollBarStart().plusDays(selection * 7);

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
		return dt.plusWeeks(1).withDayOfWeek(getFirstDayOfWeek());
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
		return dt.minusWeeks(1).withDayOfWeek(getFirstDayOfWeek());
	}

	private void scrollBarUpdate() {
		_scrollBarShift = 0;
		final ScrollBar sb = _parent.getVerticalBar();
		final long dt1 = scrollBarStart().getMillis();
		final long dt2 = _dt.getMillis();
		final long dt3 = scrollBarEnd().getMillis();
		int maxWeeks = (int) ((dt3 - dt1) / _WEEK_MILLIS);
		final int thumbSize = Math.max(getNumOfWeeks(), maxWeeks / 20); // ensure the thumb isn't getting to small
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
		} else {
			thisWeek = (int) ((dt2 - dt1) / _WEEK_MILLIS);
		}
		maxWeeks += thumbSize;
		sb.setMinimum(0);
		sb.setMaximum(maxWeeks);
		sb.setPageIncrement(getNumOfWeeks());
		sb.setSelection(thisWeek);
		_scrollBarLastSelection = thisWeek;
	}

	public void setFirstDay(final DateTime dt) {

		_dt = dt;
		_dt_normal = dt;
		_dt_tiny = dt;
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

//	void setNavigationStyle(final NavigationStyle navigationStyle) {
//		_navigationStyle = navigationStyle;
//	}

	void setNumberOfToursPerDay(final int numberOfToursPerDay) {
		_numberOfToursPerDay = numberOfToursPerDay;
		_isGraphClean = false;
		redraw();
	}

	void setNumOfWeeks(final int numberOfWeeks) {
		if (_tinyLayout) {
			_numWeeksTinyView = numberOfWeeks;
		} else {
			_numWeeksNormalView = numberOfWeeks;
		}
	}

	void setNumWeeksNormalLayout(final int numberOfWeeksDisplayed) {
		_numWeeksNormalView = numberOfWeeksDisplayed;
	}

	void setNumWeeksTinyLayout(final int numberOfWeeksDisplayed) {
		_numWeeksTinyView = numberOfWeeksDisplayed;
	}

	public void setSelectionTourId(final Long selectedTourId) {
		this._selectedItem = new Selection(selectedTourId, SelectionType.TOUR);
	}

	public void setShowDayNumberInTinyView(final boolean checked) {
		_showDayNumberInTinyView = checked;
		_isGraphClean = false;
		redraw();
	}

	void setTourFieldSizeDynamic(final boolean dynamicTourFieldSize) {
		_isGraphClean = false;
		_dynamicTourFieldSize = dynamicTourFieldSize;
		redraw();
	}

	public void setTourInfoFormatter(final int line, final TourInfoFormatter formatter) {
		_tourInfoFormatter[line] = formatter;
		_isGraphClean = false;
		redraw();
	}

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
	};

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

	void zoomIn() {
		int numWeeksDisplayed = getNumOfWeeks();
		setNumOfWeeks(numWeeksDisplayed > 1 ? --numWeeksDisplayed : numWeeksDisplayed);
		_isGraphClean = false;
		redraw();
		scrollBarUpdate();
	}

	void zoomOut() {
		setNumOfWeeks(getNumOfWeeks() + 1);
		_isGraphClean = false;
		redraw();
		scrollBarUpdate();
	}

}
