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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorCacheSWT;
import net.tourbook.common.color.ColorUtil;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.ui.TextWrapPainter;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProviderAll;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
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
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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

public class CalendarGraph extends Canvas implements ITourProviderAll {

	private static final int					MIN_SCROLLABLE_WEEKS	= 12;

	private final TourDoubleClickState			_tourDoubleClickState	= new TourDoubleClickState();

	private ColorCacheSWT						_colorCache				= new ColorCacheSWT();
	//
	private ArrayList<RGB>						_rgbBright;
	private ArrayList<RGB>						_rgbDark;
	private ArrayList<RGB>						_rgbLine;
	private ArrayList<RGB>						_rgbText;
	//
	private int									_lastDayOfWeekToGoTo	= -1;
	//
	private List<FocusItem>						_allDayFocusItems		= new ArrayList<FocusItem>();
	private List<FocusItem>						_allTourFocusItems		= new ArrayList<FocusItem>();
	private boolean								_isHoveredModified		= false;
	private boolean								_isGraphClean			= false;
	//
	private CalendarSelectItem					_emptyItem				= new CalendarSelectItem(-1, ItemType.EMPTY);
	private CalendarSelectItem					_hoveredItem			= _emptyItem;
	private CalendarSelectItem					_selectedItem			= _emptyItem;
	private CalendarSelectItem					_lastSelectedItem		= _emptyItem;
	private CalendarSelectItem					_hoveredTour;
	//
	final private Rectangle						_nullRec				= null;
	//
	private CalendarView						_calendarView;
	private CalendarTourDataProvider			_dataProvider;
	private CalendarYearMonthContributionItem	_calendarYearMonthContributor;
	//
	private ListenerList						_selectionProvider		= new ListenerList();
	//
	/**
	 * Displayed weeks in the calendar which are before the first calendar tour or after today
	 * (which is the last calendar tour). When the displayed weeks are within the first/last tour
	 * then this is 0.
	 */
	private int									_scrollBar_OutsideWeeks;
	private int									_scrollBar_LastSelection;
	//
	/** Visible weeks in one column */
	private int									_numWeeksInOneColumn;
	//
	private boolean								_isYearColumn;
	private boolean								_isScrollbarInitialized;
	private boolean								_isInUpdateScrollbar;
	/**
	 * This rectangle contains all visible days except week no and week info area.
	 */
	private Rectangle[]							_calendarAllDaysRectangle;

	/**
	 * Contains the area for the current day date label
	 */
	private Rectangle							_dayDateLabelRect;

	/**
	 * Cache font height;
	 */
	private int									_defaultFontHeight;

	private int									_fontHeight_DateColumn;
	private int									_fontHeight_DayContent;
	private int									_fontHeight_DayHeader;
	private int									_fontHeight_YearHeader;
	/**
	 * Date of the first day of a week and the first day in the calendar viewport.
	 * <p>
	 * Date/time is necessary otherwise {@link Duration} will NOT work !!!
	 */
	private LocalDateTime						_firstViewportDay		= LocalDateTime
			.now()
			.with(getFirstDayOfWeek_SameOrPrevious());

	private LocalDateTime						_yearColumn_FirstYear	= LocalDateTime
			.now()
			.withMonth(1)
			.withDayOfMonth(1);
	//
	private LocalDateTime						_yearColumn_CurrentYear;
	private LocalDateTime						_yearColumn_NextYear;
	private LocalDate							_calendarFirstDay;
	private LocalDate							_calendarLastDay;
	private int									_nextWeekDateYPos;
	private int									_lastWeekDateYear;
	//
	private CalendarConfig						_currentConfig;
	//
	private TextWrapPainter						_textWrapPainter;
	private CalendarItemTransfer				_calendarItemTransfer	= new CalendarItemTransfer();
	//
	private int									_dndLastOperation;
	private LocalDate							_dragOverDate;
	//
	/*
	 * UI controls
	 */
	private Composite							_parent;
	//
	private Display								_display				= Display.getCurrent();
	//
	private Color								_black					= _display.getSystemColor(SWT.COLOR_BLACK);
	private Color								_gray					= _display.getSystemColor(SWT.COLOR_GRAY);
	private Color								_white					= _display.getSystemColor(SWT.COLOR_WHITE);
	private Color								_red					= _display.getSystemColor(SWT.COLOR_RED);
	private Color								_blue					= _display.getSystemColor(SWT.COLOR_BLUE);
	private Color								_darkGray				= _colorCache.getColor(0x404040);
	//
	private RGB									_day_TourBackgroundRGB;
	private RGB									_whiteRGB				= _white.getRGB();
	private RGB									_blackRGB				= _black.getRGB();
	//
	private Font								_boldFont;
	private Font								_fontDateColumn;
	private Font								_fontDayContent;
	private Font								_fontDayHeader;
	private Font								_fontYearHeader;
	//
	private Image								_calendarImage;
	//
	private DragSource							_dragSource;
	private DropTarget							_dropTarget;

	private class CalendarItemTransfer extends ByteArrayTransfer {

		private final String	TYPE_NAME	= "net.tourbook.ui.views.calendar.CalendarGraph";	//$NON-NLS-1$
		private final int		TYPE_ID		= registerType(TYPE_NAME);

		private CalendarItemTransfer() {}

		@Override
		protected int[] getTypeIds() {
			return new int[] { TYPE_ID };
		}

		@Override
		protected String[] getTypeNames() {
			return new String[] { TYPE_NAME };
		}

		@Override
		protected void javaToNative(final Object data, final TransferData transferData) {

			if (!(data instanceof CalendarItemTransferData)) {
				return;
			}

			try {

				final CalendarItemTransferData calendarTransferData = (CalendarItemTransferData) data;

				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				final DataOutputStream dataOut = new DataOutputStream(out);

				final CalendarTourData calendarTourData = calendarTransferData.calendarTourData;

				dataOut.writeLong(calendarTransferData.tourId);
				dataOut.writeInt(calendarTourData.typeColorIndex);

				dataOut.close();

				super.javaToNative(out.toByteArray(), transferData);

			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		protected Object nativeToJava(final TransferData transferData) {

			try {

				final byte[] bytes = (byte[]) super.nativeToJava(transferData);
				final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
				final DataInputStream dataIn = new DataInputStream(in);

				final CalendarTourData calendarTourData = new CalendarTourData();

				final long tourId = dataIn.readLong();
				calendarTourData.typeColorIndex = dataIn.readInt();

				return new CalendarItemTransferData(tourId, calendarTourData);

			} catch (final IOException e) {
				e.printStackTrace();
			}

			//can't get here
			return null;
		}
	};

	private class CalendarItemTransferData {

		private long				tourId;
		private CalendarTourData	calendarTourData;

		public CalendarItemTransferData(final long tourId, final CalendarTourData calendarTourData) {

			this.tourId = tourId;
			this.calendarTourData = calendarTourData;
		}

		@Override
		public String toString() {
			return "CalendarItemTransferData [tourId=" + tourId + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	/**
	 * Selected or highlighted item
	 */
	class CalendarSelectItem {

		long				id;
		ItemType			type;

		Rectangle			itemRectangle;

		/**
		 * Currently only manual created tours can be dragged and dropped.
		 */
		boolean				canItemBeDragged;
		boolean				isDragOverItem;

		CalendarTourData	calendarTourData;

		private CalendarSelectItem(final long id, final ItemType type) {

			this.id = id;
			this.type = type;
		}

		@Override
		public boolean equals(final Object o) {

			if (o instanceof CalendarSelectItem) {

				final boolean isIdEqual = ((CalendarSelectItem) o).id == this.id;
				final boolean isTypeEqual = ((CalendarSelectItem) o).type == this.type;

				return isIdEqual && isTypeEqual;
			}

			return super.equals(o);
		}

		boolean isTour() {

			return (id > 0 && type == ItemType.TOUR);
		}

		@Override
		public String toString() {
			return "Selection [" //$NON-NLS-1$
					+ "id=" + id + ", " //$NON-NLS-1$ //$NON-NLS-2$
					+ "type=" + type + ", " //$NON-NLS-1$ //$NON-NLS-2$
					//					+ "itemRectangle=" + itemRectangle
					+ "]"; //$NON-NLS-1$
		}

	}

	private class DayItem {

		private long dayId;

		DayItem(final LocalDate date) {

			this.dayId = ChronoUnit.DAYS.between(LocalDate.now(), date);
		}

		DayItem(final long dayId) {

			this.dayId = dayId;
		}
	}

	private class FocusItem {

		Rectangle			rect;
		long				id;

		CalendarTourData	calendarTourData;
		DayItem				dayItem;
		LocalDate			dayDate;

		private FocusItem(final Rectangle r, final long id, final CalendarTourData calendarTourData) {

			this.rect = r;
			this.id = id;

			this.calendarTourData = calendarTourData;
		}

		private FocusItem(final Rectangle r, final long id, final DayItem dayItem, final LocalDate dayDate) {

			this.rect = r;
			this.id = id;

			this.dayItem = dayItem;
			this.dayDate = dayDate;
		}

		@Override
		public String toString() {
			return "ItemLocation [" //$NON-NLS-1$

					+ "rect=" + rect + ", " //$NON-NLS-1$ //$NON-NLS-2$
					+ "id=" + id + ", " //$NON-NLS-1$ //$NON-NLS-2$
					+ "calendarTourData=" + calendarTourData + ", " //$NON-NLS-1$ //$NON-NLS-2$
					+ "dayItem=" + dayItem + ", " //$NON-NLS-1$ //$NON-NLS-2$

					+ "]"; //$NON-NLS-1$
		}
	}

	enum ItemType {

		EMPTY, //

		DAY, //
		TOUR, //
	}

	CalendarGraph(final Composite parent, final int style, final CalendarView calendarView) {

		super(parent, style);

		_parent = parent;
		_calendarView = calendarView;

		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		_textWrapPainter = new TextWrapPainter();

		final GC gc = new GC(parent);
		{
			gc.setFont(JFaceResources.getDialogFont());

			final FontMetrics fontMetrics = gc.getFontMetrics();

			_defaultFontHeight = fontMetrics.getHeight();
//			_defaultFontAverageCharWidth = fontMetrics.getAverageCharWidth();
		}
		gc.dispose();

		_dataProvider = CalendarTourDataProvider.getInstance();
		_dataProvider.setCalendarGraph(this);

		_rgbBright = new ArrayList<RGB>();
		_rgbDark = new ArrayList<RGB>();
		_rgbLine = new ArrayList<RGB>();
		_rgbText = new ArrayList<RGB>();

		updateTourTypeColors();

		addListener();
		addDragDrop();
	}

	private void addDragDrop() {

		final int allDND_Operations = DND.DROP_COPY | DND.DROP_MOVE;
		final Transfer[] transferTypes = new Transfer[] { _calendarItemTransfer };

		// Allow data to be copied or moved from the drag source
		_dragSource = new DragSource(this, allDND_Operations);
		_dragSource.setTransfer(transferTypes);

		_dragSource.addDragListener(new DragSourceListener() {

			@Override
			public void dragFinished(final DragSourceEvent event) {}

			@Override
			public void dragSetData(final DragSourceEvent event) {

				// Provide the data of the requested type.
				if (_selectedItem.canItemBeDragged
						&& _selectedItem.type == ItemType.TOUR
						&& _calendarItemTransfer.isSupportedType(event.dataType)) {

					event.data = new CalendarItemTransferData(_selectedItem.id, _selectedItem.calendarTourData);
				}
			}

			@Override
			public void dragStart(final DragSourceEvent event) {

				event.doit = _selectedItem.canItemBeDragged && _selectedItem.type == ItemType.TOUR;
			}
		});

		// Allow data to be copied or moved to the drop target
		_dropTarget = new DropTarget(this, allDND_Operations);
		_dropTarget.setTransfer(transferTypes);

		_dropTarget.addDropListener(new DropTargetListener() {

			@Override
			public void dragEnter(final DropTargetEvent event) {}

			@Override
			public void dragLeave(final DropTargetEvent event) {}

			@Override
			public void dragOperationChanged(final DropTargetEvent event) {

				// highly complicated to keep the operation and changing it
				if (event.detail != DND.DROP_NONE) {

					// adjust last operation
					_dndLastOperation = event.detail;
				}
			}

			@Override
			public void dragOver(final DropTargetEvent event) {

				final CalendarSelectItem oldHoveredItem = _hoveredItem;

				if (_calendarItemTransfer.isSupportedType(event.currentDataType)) {

					final Object data = _calendarItemTransfer.nativeToJava(event.currentDataType);
					if (data instanceof CalendarItemTransferData) {

						final Point dragOverPosition = CalendarGraph.this.toControl(event.x, event.y);

						for (final FocusItem dayItem : _allDayFocusItems) {

							if (dayItem.rect.contains(dragOverPosition.x, dragOverPosition.y)) {

								// a day item is hovered

								// highly complicated to keep the operation and changing it
								if (event.detail == DND.DROP_NONE) {

									// recovery last operation
									event.detail = _dndLastOperation;
								} else {
									// keep last operation
									_dndLastOperation = event.detail;
								}

								// keep drag date
								_dragOverDate = dayItem.dayDate;

								final CalendarItemTransferData transferData = (CalendarItemTransferData) data;

								_hoveredItem = new CalendarSelectItem(dayItem.id, ItemType.DAY);

								_hoveredItem.isDragOverItem = true;
								_hoveredItem.calendarTourData = transferData.calendarTourData;

								_isHoveredModified = !oldHoveredItem.equals(_hoveredItem);

								// only draw highlighting when modified
								if (_isHoveredModified) {
									redraw();
								}

								return;
							}
						}
					}
				}

				/*
				 * Hide previous drag over item
				 */
				_hoveredItem = _emptyItem;
				_isHoveredModified = !oldHoveredItem.equals(_hoveredItem);

				// only draw highlighting when modified
				if (_isHoveredModified) {
					redraw();
				}

				// disable drop
				event.detail = DND.DROP_NONE;
			}

			@Override
			public void drop(final DropTargetEvent event) {

				if (_calendarItemTransfer.isSupportedType(event.currentDataType)) {

					final Object data = _calendarItemTransfer.nativeToJava(event.currentDataType);
					if (data instanceof CalendarItemTransferData) {

						final CalendarItemTransferData transferData = (CalendarItemTransferData) data;

						onDropTour(transferData.tourId, event);
					}
				}
			}

			@Override
			public void dropAccept(final DropTargetEvent event) {

			}
		});
	}

	private void addListener() {

		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent event) {

				drawCalendar(event.gc);

				// fix problem that the scrollbar is initially not correctly setup
				if (!_isScrollbarInitialized) {

					_isScrollbarInitialized = true;

					scrollBar_updateScrollbar();
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
						scroll_Tour(true);
					}
					break;
				case SWT.TRAVERSE_ESCAPE:
					_selectedItem = _emptyItem;
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
					scroll_Tour(false);
					break;

				case SWT.ARROW_RIGHT:
				case 'l':
					scroll_Tour(true);
					break;

				case SWT.ARROW_UP:
				case 'k':
					if (_selectedItem.isTour()) {
						gotoTour_SameWeekday(-1);
					} else {
						scroll_Week(true);
					}
					break;

				case SWT.ARROW_DOWN:
				case 'j':
					if (_selectedItem.isTour()) {
						gotoTour_SameWeekday(+1);
					} else {
						scroll_Week(false);
					}
					break;

				case SWT.PAGE_DOWN:
				case 'n':
					scroll_Screen(false);
					break;

				case SWT.PAGE_UP:
				case 'p':
					scroll_Screen(true);
					break;

				case SWT.HOME:
				case ',':
					gotoTour_First();
					break;

				case SWT.END:
				case '.':
					gotoDate_Today();
					break;

				case ' ':
					if (_selectedItem.isTour()) {
						_selectedItem = _emptyItem;
						redraw();
					} else {
						scroll_Tour(false);
					}
					break;
				}
			}
		});

		addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseScrolled(final MouseEvent event) {

				final Point p = new Point(event.x, event.y);
				boolean isInDayArea = false;

				// check if mouse is in the areas which contains the days
				for (final Rectangle dateRectangle : _calendarAllDaysRectangle) {
					if (dateRectangle.contains(p)) {
						isInDayArea = true;
						break;
					}
				}

				if (isInDayArea) {

					final boolean isTour = _selectedItem.isTour();

					if (event.count > 0) {
						if (isTour) {
							scroll_Tour(false);
						} else {
							scroll_Week(true);
						}
					} else {
						if (isTour) {
							scroll_Tour(true);
						} else {
							scroll_Week(false);
						}
					}

				} else {

					// left or right column, scroll by pages

					if (event.count > 0) {
						scroll_Screen(true);
					} else {
						scroll_Screen(false);
					}
				}
			}
		});

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
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
				scrollBar_onScroll(event);
			}
		});

	}

	void addSelectionProvider(final ICalendarSelectionProvider provider) {
		_selectionProvider.add(provider);
	}

	private void disposeFonts() {

		_fontDateColumn = UI.disposeResource(_fontDateColumn);
		_fontDayContent = UI.disposeResource(_fontDayContent);
		_fontDayHeader = UI.disposeResource(_fontDayHeader);
		_fontYearHeader = UI.disposeResource(_fontYearHeader);
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
	private void drawCalendar(final GC calendarGC) {

		final Rectangle canvas = getClientArea();
		final int canvasWidth = canvas.width;
		final int canvasHeight = canvas.height;

		if (_isGraphClean && _calendarImage != null) {

			final Image hoveredImage = new Image(getDisplay(), canvasWidth, canvasHeight);
			final GC gcHovered = new GC(hoveredImage);
			{
				gcHovered.drawImage(_calendarImage, 0, 0);

				drawSelection(gcHovered);

				if (_isHoveredModified) {

					drawHovered(gcHovered);

					_isHoveredModified = false;
				}

				calendarGC.drawImage(hoveredImage, 0, 0);
			}
			gcHovered.dispose();
			hoveredImage.dispose();

			return;
		}

		_currentConfig = CalendarConfigManager.getActiveCalendarConfig();

		if (_calendarImage != null && !_calendarImage.isDisposed()) {
			_calendarImage.dispose();
		}

		_calendarImage = new Image(getDisplay(), canvasWidth, canvasHeight);

		_isYearColumn = _currentConfig.isShowYearColumns;

		// one col left and right of the week + 7 week days
		final int numYearColumns = _isYearColumn ? _currentConfig.numYearColumns : 1;
		final int numDayColumns = 7;

		final int weekHeight = _currentConfig.weekHeight;

		// set year header font
		getFont_YearHeader();

		final int yearHeaderHeight = _isYearColumn ? _fontHeight_YearHeader + 20 : 0;
		int numVisibleRows = (canvasHeight - yearHeaderHeight) / weekHeight;

		if (_isYearColumn) {

			// adjust column start

			if (_currentConfig.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

				_yearColumn_CurrentYear = _yearColumn_FirstYear;

				// set first day to start of week
				_firstViewportDay = _yearColumn_CurrentYear.with(getFirstDayOfWeek_SameOrPrevious());

			} else {

				_yearColumn_FirstYear = yearColumn_getFirstDayOfMonth(_yearColumn_FirstYear);
				_yearColumn_CurrentYear = _yearColumn_FirstYear;
				_yearColumn_NextYear = _yearColumn_CurrentYear.plusYears(1);

				// set first day to start of week
				_firstViewportDay = _yearColumn_CurrentYear.with(getFirstDayOfWeek_SameOrPrevious());

				final LocalDateTime firstCalendarDay_NextYear = _yearColumn_NextYear

						// set first day to start of week
						.with(getFirstDayOfWeek_SameOrPrevious());

				/*
				 * adjust number of weeks
				 */
				final int numYearWeeks = (int) ChronoUnit.WEEKS.between(
						_firstViewportDay,
						firstCalendarDay_NextYear.plusWeeks(1).with(getFirstDayOfWeek_SameOrNext()));

				if (numYearWeeks < numVisibleRows) {

					// less weeks in one year than visible rows -> show weeks for only one year

					numVisibleRows = numYearWeeks;
				}
			}
		}

		_numWeeksInOneColumn = numVisibleRows;

		LocalDate currentDate = _firstViewportDay.toLocalDate();

		/*
		 * Set calendar viewport dates
		 */
		_calendarFirstDay = currentDate;

		if (_isYearColumn) {

			if (_currentConfig.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

				_calendarLastDay = _calendarFirstDay.plusWeeks(numVisibleRows * numYearColumns).minusDays(1);

			} else {

				_calendarLastDay = _yearColumn_FirstYear

						.toLocalDate()
						.plusYears(numYearColumns)
						.minusDays(1)

						.with(getFirstDayOfWeek_SameOrNext())
						.minusDays(1);
			}

		} else {

			_calendarLastDay = _calendarFirstDay.plusWeeks(numVisibleRows * numYearColumns).minusDays(1);
		}

		_calendarView.updateUI_Title(_calendarFirstDay, _calendarLastDay);

		final GC gc = new GC(_calendarImage);

		final Color monthAlternateColor = _colorCache.getColor(_currentConfig.alternateMonthRGB);

		_allTourFocusItems.clear();
		_allDayFocusItems.clear();

		final Font normalFont = gc.getFont();

		gc.setBackground(_white);
		gc.setForeground(_black);
		gc.fillRectangle(canvas);

		int dateColumnWidth = 0;
		if (_currentConfig.isShowDateColumn) {
			dateColumnWidth = _currentConfig.dateColumnWidth;//* _defaultFontAverageCharWidth;
		}

		int summaryColumnWidth = 0;
		if (_currentConfig.isShowSummaryColumn) {
			summaryColumnWidth = _currentConfig.summaryColumnWidth;// * _defaultFontAverageCharWidth;
		}

		final int columnSpacing = _currentConfig.yearColumnsSpacing;
		final int allColumnSpace = (numYearColumns - 1) * columnSpacing;
		final int calendarColumnWidth = (canvasWidth - allColumnSpace) / numYearColumns;
		final float dayWidth = (float) (calendarColumnWidth - dateColumnWidth - summaryColumnWidth) / numDayColumns;

		_calendarAllDaysRectangle = new Rectangle[numYearColumns];

		final long todayDayId = (new DayItem(LocalDate.now())).dayId;

		Font dayDateFont = null;
		int dayDateHeight = 0;
		if (_currentConfig.isShowDayDate) {
			dayDateFont = getFont_DayHeader();
			dayDateHeight = _fontHeight_DayHeader;
		}

		final int dayLabelRightBorder = 4;

		final DateTimeFormatter dayDateFormatter = getUI_DayDateFormatter(
				_currentConfig,
				gc,
				dayWidth,
				dayLabelRightBorder);

		// we use simple ids
		long dayId = new DayItem(currentDate).dayId;

		for (int columnIndex = 0; columnIndex < numYearColumns; columnIndex++) {

			_nextWeekDateYPos = 0;

			final int columnColumSpacing = columnIndex == 0 ? 0 : columnSpacing;
			final int calendarColumnOffset = columnIndex * calendarColumnWidth + columnColumSpacing * columnIndex;

			_calendarAllDaysRectangle[columnIndex] = new Rectangle(
					calendarColumnOffset + dateColumnWidth,
					0,
					(int) (7 * dayWidth),
					canvasHeight);

			if (_isYearColumn) {

				// move to the next year

				if (_currentConfig.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

					_yearColumn_CurrentYear = currentDate.atStartOfDay();

				} else {

					_yearColumn_CurrentYear = columnIndex == 0 ? _yearColumn_FirstYear : _yearColumn_NextYear;
					_yearColumn_NextYear = _yearColumn_CurrentYear.plusYears(1);

					// set first day to start of week
					currentDate = _yearColumn_CurrentYear.with(getFirstDayOfWeek_SameOrPrevious()).toLocalDate();

					final LocalDateTime firstCalendarDay_NextYear = _yearColumn_NextYear

							// set first day to start of week
							.with(getFirstDayOfWeek_SameOrPrevious());

					/*
					 * Adjust number of weeks
					 */

					final int numYearWeeks = (int) ChronoUnit.WEEKS.between(
							currentDate,
							firstCalendarDay_NextYear) + 1;

					if (numYearWeeks < numVisibleRows) {

						// less weeks in one year than visible rows -> show weeks for only one year

						numVisibleRows = numYearWeeks;
					}

					_numWeeksInOneColumn = numYearWeeks;
				}

				/*
				 * Year header
				 */
				final Rectangle yearHeaderRect = new Rectangle(
						calendarColumnOffset + dateColumnWidth,
						0,
						calendarColumnWidth - dateColumnWidth - summaryColumnWidth,
						yearHeaderHeight);

				drawWeek_YearHeader(gc, yearHeaderRect);
			}

			// week rows
			for (int rowIndex = 0; rowIndex < numVisibleRows; rowIndex++) {

				final int rowTop = yearHeaderHeight + rowIndex * weekHeight;

				// keep 1st day of the week to get week data in the summary column
				final LocalDate week1stDay = currentDate;

				// draw date column
				if (dateColumnWidth > 0) {

					drawWeek_DateColumn(
							gc,
							currentDate,
							rowTop,
							calendarColumnOffset,
							_currentConfig.dateColumnContent,
							rowIndex == 0);
				}

				for (int dayIndex = 0; dayIndex < 7; dayIndex++) {

					final int dayPosX = calendarColumnOffset + dateColumnWidth + (int) (dayIndex * dayWidth);
					final int dayPosXNext = calendarColumnOffset + dateColumnWidth + (int) ((dayIndex + 1) * dayWidth);

					// rectangle for the whole day cell
					final Rectangle dayRect = new Rectangle(//
							dayPosX,
							rowTop,
							dayPosXNext - dayPosX,
							weekHeight);

					final DayItem day = new DayItem(dayId);
					_allDayFocusItems.add(new FocusItem(dayRect, dayId, day, currentDate));
					dayId = day.dayId + 1;

					gc.setFont(normalFont);
					gc.setBackground(_white);

					// Day background with alternate color
					if (_currentConfig.isToggleMonthColor && currentDate.getMonthValue() % 2 == 1) {

						gc.setBackground(monthAlternateColor);
						gc.fillRectangle(//
								dayRect.x,
								dayRect.y,
								dayRect.width - 1,
								dayRect.height - 1);
					}

					// get date label width
					final String dayDateLabel = dayDateFormatter.format(currentDate);

					gc.setFont(dayDateFont);
					final Point labelExtent = gc.textExtent(dayDateLabel);
					final int dayLabelWidth = labelExtent.x;
					final int dayLabelHeight = labelExtent.y;

					final int labelWidthWithOffset = dayLabelWidth + dayLabelRightBorder;
					int dateLabelPosX = dayPosXNext - labelWidthWithOffset;

					_dayDateLabelRect = null;
					if (_currentConfig.isShowDayDate) {
						_dayDateLabelRect = new Rectangle(dateLabelPosX, rowTop, dayLabelWidth, dayLabelHeight);
					}

					final CalendarTourData[] calendarData = _dataProvider.getCalendarDayData(currentDate);

					final boolean isCalendarDataAvailable = calendarData.length > 0;

					final boolean isShowDayDate = _currentConfig.isHideDayDateWhenNoTour == false //
							|| _currentConfig.isHideDayDateWhenNoTour && isCalendarDataAvailable;

					if (isCalendarDataAvailable) {

						// tours are available

						drawDay(gc, calendarData, dayRect);
					}

					// draw day date AFTER the tour is painted
					if (_currentConfig.isShowDayDate && isShowDayDate) {

						// this clipping should only kick in if shortest label format is still longer than the cell width
						gc.setClipping(dayPosX, rowTop, dayRect.width, dayDateHeight);

						final int weekDay = currentDate.getDayOfWeek().getValue();

						final boolean isWeekendColor = _currentConfig.isShowDayDateWeekendColor //
								// ISO: 6 == saturday, 7 == sunday
								&& (weekDay == 6 || weekDay == 7);

						boolean isDateTransparent = true;
						Color dayDateForegroundColor;

						if (day.dayId == todayDayId) {

							dayDateForegroundColor = (_blue);

						} else if (isWeekendColor) {

							dayDateForegroundColor = (_red);

						} else if (isCalendarDataAvailable) {

							dayDateForegroundColor = getColor_ForDayContent(calendarData[0]);

						} else {

							dayDateForegroundColor = (_darkGray);
						}

						if (isWeekendColor && isCalendarDataAvailable) {

							// paint outline, red is not very good visible with a dark background

							isDateTransparent = false;

							// draw without additional background on the right side
							dateLabelPosX += 2;

							gc.setBackground(_white);

// outline is difficult to read
//
//							gc.setForeground(_white);
//
//							gc.drawText(dayDateLabel, dateLabelPosX + 1, rowTop + 1, true);
//							gc.drawText(dayDateLabel, dateLabelPosX + 1, rowTop - 1, true);
//							gc.drawText(dayDateLabel, dateLabelPosX - 1, rowTop + 1, true);
//							gc.drawText(dayDateLabel, dateLabelPosX - 1, rowTop - 1, true);
						}

						// day header label
						gc.setFont(dayDateFont);
						gc.setForeground(dayDateForegroundColor);
						gc.drawText(
								dayDateLabel,
								dateLabelPosX,
								rowTop,
								isDateTransparent);

						gc.setClipping(_nullRec);
					}

					currentDate = currentDate.plusDays(1);
				}

				if (summaryColumnWidth > 0) {

					final int devX = calendarColumnOffset + dateColumnWidth + (int) (7 * dayWidth);

					final Rectangle weekRec = new Rectangle(devX, rowTop, summaryColumnWidth, weekHeight);

					final CalendarTourData weekSummary = _dataProvider.getCalendarWeekSummaryData(
							week1stDay,
							_calendarView);

					if (weekSummary.loadingState == LoadingState.IS_LOADED && weekSummary.numTours > 0) {
						drawWeek_Summary(gc, weekSummary, weekRec);
					}
				}

			}
		}

		// draw the selection on top of our calendar graph image so we can reuse that image
		final Image selectedImage = new Image(getDisplay(), canvasWidth, canvasHeight);
		final GC selectedGC = new GC(selectedImage);
		{
			selectedGC.drawImage(_calendarImage, 0, 0);

			drawSelection(selectedGC);

			calendarGC.drawImage(selectedImage, 0, 0);
		}
		selectedImage.dispose();
		selectedGC.dispose();

		updateUI_YearMonthCombo(currentDate);

		_isGraphClean = true;
	}

	private void drawDay(	final GC gc,
							final CalendarTourData[] allCalendarTourData,
							final Rectangle dayRect) {

		gc.setFont(getFont_DayContent());

		final int numTours = allCalendarTourData.length;
		final int tourHeight = dayRect.height / numTours;
		final int remainingHeight = dayRect.height % numTours;

		for (int tourIndex = 0; tourIndex < numTours; tourIndex++) {

			final int devY = dayRect.y + tourIndex * tourHeight;

			int tourCellHeight = tourHeight - 1;

			// prevent that the bottom is not painted with the fill color
			if (tourIndex == numTours - 1) {
				// the last tour is using the remaining height
				tourCellHeight += remainingHeight;
			}

			final Rectangle tourRect = new Rectangle(
					dayRect.x,
					devY,
					dayRect.width - 1,
					tourCellHeight);

			final Rectangle focusRect = new Rectangle(
					tourRect.x - 1,
					tourRect.y - 1,
					tourRect.width + 2,
					tourRect.height + 2);

			final CalendarTourData calendarTourData = allCalendarTourData[tourIndex];

			final FocusItem tourFocusItem = new FocusItem(focusRect, calendarTourData.tourId, calendarTourData);

			_allTourFocusItems.add(tourFocusItem);

			drawDay_Tour(gc, calendarTourData, tourRect);
		}
	}

	private void drawDay_Tour(	final GC gc,
								final CalendarTourData data,
								final Rectangle tourRect) {

		final int devX = tourRect.x;
		final int devY = tourRect.y;

		final int cellWidth = tourRect.width;
		final int cellHeight = tourRect.height;

		final int devXRight = devX + cellWidth - 1;
		final int devYBottom = devY + cellHeight - 1;

		drawDay_TourBackground(gc, data, devX, devY, cellWidth, cellHeight, devXRight, devYBottom);
		drawDay_TourBorder(gc, data, devX, devY, cellWidth, cellHeight, devXRight, devYBottom);
		drawDay_TourText(gc, tourRect, data);
	}

	/**
	 * Tour background
	 * 
	 * @param data
	 * @return Background RGB or <code>null</code> when background is not painted
	 */
	private void drawDay_TourBackground(final GC gc,
										final CalendarTourData data,
										final int devX,
										final int devY,
										final int cellWidth,
										final int cellHeight,
										final int devXRight,
										final int devYBottom) {

		final int tourBackgroundWidth = _currentConfig.tourBackgroundWidth;

		final int marginWidth = (int) (tourBackgroundWidth <= 10 ? tourBackgroundWidth //
				: cellWidth * (tourBackgroundWidth / 100.0));

		boolean isGradient = false;
		boolean isVertical = false;

		_day_TourBackgroundRGB = getColor_CalendarRGB(_currentConfig.tourBackgroundColor1, data);

		final Color backgroundColor = _colorCache.getColor(_day_TourBackgroundRGB.hashCode());

		switch (_currentConfig.tourBackground) {

		case FILL:
			gc.setBackground(backgroundColor);
			gc.fillRectangle(devX, devY, cellWidth, cellHeight);
			break;

		case FILL_LEFT:
			gc.setBackground(backgroundColor);
			gc.fillRectangle(devX, devY, marginWidth, cellHeight);
			break;

		case FILL_RIGHT:
			gc.setBackground(backgroundColor);
			gc.fillRectangle(devX + cellWidth - marginWidth, devY, marginWidth, cellHeight);
			break;

		case CIRCLE:
			final int ovalSize = Math.min(cellWidth, cellHeight);
			gc.setAntialias(SWT.ON);
			gc.setBackground(backgroundColor);
			gc.fillOval(
					devX + cellWidth / 2 - ovalSize / 2,
					devY,
					ovalSize,
					ovalSize);
			break;

		case GRADIENT_HORIZONTAL:
			isGradient = true;
			gc.setForeground(backgroundColor);
			gc.setBackground(_colorCache.getColor(getColor_CalendarRGB(_currentConfig.tourBackgroundColor2, data)));
			break;

		case GRADIENT_VERTICAL:
			isGradient = true;
			isVertical = true;
			gc.setForeground(backgroundColor);
			gc.setBackground(_colorCache.getColor(getColor_CalendarRGB(_currentConfig.tourBackgroundColor2, data)));
			break;

		case NO_BACKGROUND:
			_day_TourBackgroundRGB = null;
			break;
		}

		if (isGradient) {

			gc.fillGradientRectangle(
					devX,
					devY,
					cellWidth,
					cellHeight,
					isVertical);
		}
	}

	/**
	 * Tour border
	 */
	private void drawDay_TourBorder(final GC gc,
									final CalendarTourData data,
									final int devX,
									final int devY,
									final int cellWidth,
									final int cellHeight,
									final int devXRight,
									final int devYBottom) {

		final RGB tourBorderRGB = getColor_CalendarRGB(_currentConfig.tourBorderColor, data);
		final Color line = _colorCache.getColor(tourBorderRGB.hashCode());

		gc.setForeground(line);
		gc.setBackground(line);

		final int tourBorderWidth = _currentConfig.tourBorderWidth;

		final int borderWidth = (int) (tourBorderWidth <= 10 ? tourBorderWidth //
				: cellWidth * (tourBorderWidth / 100.0));

		final int borderHeight = (int) (tourBorderWidth <= 10 ? tourBorderWidth //
				: cellHeight * (tourBorderWidth / 100.0));

		boolean isTop = false;
		boolean isLeft = false;
		boolean isRight = false;
		boolean isBottom = false;

		switch (_currentConfig.tourBorder) {

		case BORDER_ALL:
			isTop = true;
			isBottom = true;
			isLeft = true;
			isRight = true;
			break;

		case BORDER_TOP:
			isTop = true;
			break;

		case BORDER_BOTTOM:
			isBottom = true;
			break;

		case BORDER_TOP_BOTTOM:
			isTop = true;
			isBottom = true;
			break;

		case BORDER_LEFT_RIGHT:
			isLeft = true;
			isRight = true;
			break;

		case BORDER_LEFT:
			isLeft = true;
			break;

		case BORDER_RIGHT:
			isRight = true;
			break;

		case NO_BORDER:
		default:
			break;
		}

		if (isTop) {
			gc.fillRectangle(//
					devX,
					devY,
					cellWidth,
					borderHeight);
		}

		if (isBottom) {
			gc.fillRectangle(//
					devX,
					devY + cellHeight - borderHeight,
					cellWidth,
					borderHeight);
		}

		if (isLeft) {
			gc.fillRectangle(//
					devX,
					devY,
					borderWidth,
					cellHeight);
		}

		if (isRight) {
			gc.fillRectangle(//
					devX + cellWidth - borderWidth,
					devY,
					borderWidth,
					cellHeight);
		}
	}

	private void drawDay_TourText(	final GC gc,
									final Rectangle tourRect,
									final CalendarTourData data) {

		final Color fg = getColor_ForDayContent(data);

		gc.setForeground(fg);
		gc.setClipping(tourRect.x, tourRect.y, tourRect.width, tourRect.height);

		final String tourTitle = data.tourTitle;
		final String tourDescription = data.tourDescription;
		final String infoText = tourTitle == null //
				? tourDescription == null //
						? UI.EMPTY_STRING //
						: tourDescription
				: tourTitle;

		if (infoText.length() > 0) {

			final int topBorder = 0;
			final int leftBorder = 2;

			_textWrapPainter.drawText(
					gc,
					infoText,
					tourRect.x + leftBorder,
					tourRect.y + topBorder,
					tourRect.width - leftBorder,
					tourRect.height - topBorder,
					_fontHeight_DayContent,
					_dayDateLabelRect);
		}

		gc.setClipping(_nullRec);
	}

	private void drawHovered(final GC gc) {

		List<FocusItem> allFocusItems;

		if (_hoveredItem.type == ItemType.TOUR) {
			allFocusItems = _allTourFocusItems;
		} else if (_hoveredItem.type == ItemType.DAY) {
			allFocusItems = _allDayFocusItems;
		} else {
			return;
		}

		for (final FocusItem focusItem : allFocusItems) {

			if (focusItem.id == _hoveredItem.id) {

				final Rectangle itemRectangle = focusItem.rect;

				if (focusItem.calendarTourData instanceof CalendarTourData) {

					// draw tour

					final CalendarTourData calendarTourData = (focusItem.calendarTourData);

					drawHovered_Tour(gc, calendarTourData, itemRectangle);

				} else if (focusItem.dayItem instanceof DayItem) {

					// draw day

					if (_hoveredItem.isDragOverItem) {

						// draw drag over outline

						final CalendarTourData calendarTourData = _hoveredItem.calendarTourData;
						final int typeColorIndex = calendarTourData.typeColorIndex;

						final int lineWidth = 7;
						final int lineWidth2 = lineWidth / 2;

						gc.setLineWidth(lineWidth);
						gc.setBackground(_colorCache.getColor(_rgbLine.get(typeColorIndex).hashCode()));

						gc.drawRectangle(
								itemRectangle.x - 2 - lineWidth2,
								itemRectangle.y - 2 - lineWidth2,
								itemRectangle.width + 2 + lineWidth2 * 2,
								itemRectangle.height + 2 + lineWidth2 * 2);

					} else {

						// draw day hovered

						gc.setAlpha(0xa0);

						gc.setBackground(_white);
						gc.setForeground(_gray);

						gc.fillGradientRectangle(
								itemRectangle.x - 4,
								itemRectangle.y - 4,
								itemRectangle.width + 9,
								itemRectangle.height + 9,
								false);

						gc.drawRoundRectangle(
								itemRectangle.x - 5,
								itemRectangle.y - 5,
								itemRectangle.width + 10,
								itemRectangle.height + 10,
								6,
								6);

						gc.setAlpha(0xFF);
					}
				}

				return;
			}
		}
	}

	private void drawHovered_Tour(final GC gc, final CalendarTourData data, final Rectangle rec) {

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
//		Color color;
//		if (_useBlackForHighlightTourInfoText) {
//			color = _black;
//		} else {
//			color = _colorCache.getColor(_rgbText.get(data.typeColorIndex).hashCode());
//		}

		drawDay_TourText(gc, r, data);

	}

	private void drawSelection(final GC gc) {

		if (!_lastSelectedItem.equals(_selectedItem)) {

			fireSelectionEvent(_selectedItem);

			_lastSelectedItem = _selectedItem;
		}

		List<FocusItem> allFocusItems;

		if (_selectedItem.type == ItemType.TOUR) {
			allFocusItems = _allTourFocusItems;
		} else if (_selectedItem.type == ItemType.DAY) {
			allFocusItems = _allDayFocusItems;
		} else {
			return;
		}

		for (final FocusItem focusItem : allFocusItems) {
			if (focusItem.id == _selectedItem.id) {

				if (focusItem.calendarTourData instanceof CalendarTourData) {

					drawSelection_Tour(gc, (focusItem.calendarTourData), focusItem.rect);

				} else if (focusItem.dayItem instanceof DayItem) {

					drawSelection_Day(gc, focusItem.rect);
				}

				return;
			}
		}
	}

	private void drawSelection_Day(final GC gc, final Rectangle r) {

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

	private void drawSelection_Tour(final GC gc, final CalendarTourData data, final Rectangle rec) {

		final Color lineColor = _colorCache.getColor(_rgbLine.get(data.typeColorIndex).hashCode());

		// - red box -
//		gc.setBackground(_red);
//		// gc.setBackground(lineColor);
//		gc.fillRectangle(rec.x - 4, rec.y - 4, rec.width + 9, rec.height + 9);
//		// gc.setForeground(_red);

		gc.setLineWidth(6);
		gc.setForeground(lineColor);
		gc.drawRoundRectangle(rec.x - 5, rec.y - 5, rec.width + 10, rec.height + 10, 6, 6);

		// focus is 1 pixel larger than tour rectangle
//		final Rectangle tourRect = new Rectangle(rec.x + 1, rec.y + 1, rec.width - 2, rec.height - 2);
//		drawDay_Tour(gc, data, tourRect);

		return;
	}

	private void drawWeek_DateColumn(	final GC gc,
										final LocalDate currentDate,
										final int rowTop,
										final int calendarColumnOffset,
										final DateColumnContent dateColumnContent,
										final boolean isFirstRow) {

		gc.setForeground(_darkGray);
		gc.setBackground(_white);

		gc.setFont(getFont_DateColumn());

		final int posX = calendarColumnOffset + 2;
		final int thisWeekYPos = rowTop;

		// prevent paint overlapping
		final boolean isInLastWeek = thisWeekYPos < _nextWeekDateYPos;

		if (isInLastWeek) {
			return;
		}

		final int nextWeekYPos = thisWeekYPos + _fontHeight_DateColumn + 0;

//		if (_currentConfig.isShowYearColumns
//				&& _currentConfig.yearColumnsStart == ColumnStart.CONTINUOUSLY) {
//
//			// draw year always in first row
//
//			final boolean isInJanuary = currentDate.getMonthValue() == 1
//					|| currentDate.plusDays(6).getMonthValue() == 1;
//
//			final int yearColumnYear = _yearColumn_CurrentYear.getYear();
//
//			if (isInJanuary && _lastWeekDateYear != yearColumnYear) {
//
//				gc.drawString(Integer.toString(yearColumnYear), posX, thisWeekYPos, true);
//
//				_lastWeekDateYear = yearColumnYear;
//				_nextWeekDateYPos = nextWeekYPos;
//
//				return;
//			}
//		}

		switch (dateColumnContent) {
		case MONTH:

			// draw month

			if (currentDate.minusDays(1).getMonthValue() != currentDate.plusDays(6).getMonthValue()) {

				// a new month started on this week

				final String monthLabel = TimeTools.Formatter_Month.format(currentDate.plusDays(6));

				gc.drawString(monthLabel, posX, thisWeekYPos, true);

				_nextWeekDateYPos = nextWeekYPos;
			}

			break;

		case YEAR:

			final int year = currentDate.getYear();

			// prevent to repeat the year
			if (year != _lastWeekDateYear) {

				gc.drawString(Integer.toString(year), posX, thisWeekYPos, true);

				_lastWeekDateYear = year;
				_nextWeekDateYPos = nextWeekYPos;
			}

			break;

		case WEEK_NUMBER:
		default:

			// default is week number

			final int week = currentDate.get(TimeTools.calendarWeek.weekOfWeekBasedYear());

			gc.drawString(Integer.toString(week), posX, thisWeekYPos, true);

			_nextWeekDateYPos = nextWeekYPos;

			break;
		}
	}

	private void drawWeek_Summary(	final GC gc,
									final CalendarTourData calendarTourData,
									final Rectangle weekRec) {

		final int xr = weekRec.x + weekRec.width - 1;
		final int xl = weekRec.x + 2;
		int posX;
		int posY = weekRec.y + 1;
		final boolean doClip = true;

		Point extent;
		final int maxLength = weekRec.width - 2;

		final Font normalFont = gc.getFont();

		gc.setFont(_boldFont);
		gc.setClipping(weekRec);
		gc.setBackground(_white);

		for (final WeekFormatterData formatterData : _currentConfig.allWeekFormatterData) {

			final WeekFormatter formatter = getFormatter(formatterData.id);

			gc.setForeground(_colorCache.getColor(formatter.getColor().hashCode()));

			String text = formatter.format(
					calendarTourData,
					formatterData.valueFormat,
					_currentConfig.isShowWeekValueUnit);

			if (text.length() > 0 && posY < (weekRec.y + weekRec.height)) {

				extent = gc.stringExtent(text);
				posX = xr - extent.x;

				if (extent.x > maxLength) {
					if (doClip && text.contains(UI.SPACE1)) {
						text = text.substring(0, text.lastIndexOf(UI.SPACE));
						posX = xr - gc.stringExtent(text).x;
					} else {
						posX = xl;
					}
				}

				gc.drawText(text, posX, posY);
			}

			posY += _defaultFontHeight;
		}

		gc.setFont(normalFont);
		gc.setClipping(_nullRec);
	}

	private void drawWeek_YearHeader(final GC gc, final Rectangle yearHeaderRect) {

		final String yearText = Integer.toString(_yearColumn_CurrentYear.getYear());

		gc.setForeground(_darkGray);
		gc.setBackground(_white);

		gc.setFont(getFont_YearHeader());

		final Point textSize = gc.textExtent(yearText);

		final int posX = yearHeaderRect.x + yearHeaderRect.width / 2 - textSize.x / 2;
		final int posY = yearHeaderRect.y + 10;

		gc.drawString(yearText, posX, posY);

//		gc.setForeground(_red);
//		gc.drawRectangle(yearHeaderRect);
	}

	void fireSelectionEvent(final CalendarSelectItem selection) {

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

	void fireSelectionEvent(final ItemType type, final long id) {

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

	private RGB getColor_CalendarRGB(final CalendarColor imageColor, final CalendarTourData data) {

		switch (imageColor) {
		case BRIGHT:
			return _rgbBright.get(data.typeColorIndex);

		case LINE:
			return _rgbLine.get(data.typeColorIndex);

		case WHITE:
			return _whiteRGB;

		case BLACK:
			return _blackRGB;

		case DARK:
		default:
			return _rgbDark.get(data.typeColorIndex);
		}
	}

	private Color getColor_ForDayContent(final CalendarTourData data) {

		switch (_currentConfig.dayContentColor) {

		case BRIGHT:
			return _colorCache.getColor(_rgbBright.get(data.typeColorIndex));

		case DARK:
			return _colorCache.getColor(_rgbDark.get(data.typeColorIndex));

		case LINE:
			return _colorCache.getColor(_rgbLine.get(data.typeColorIndex));

		case CONTRAST:

			if (_day_TourBackgroundRGB == null) {

				return _black;

			} else {

				final RGB tourBackgroundRGB = getColor_CalendarRGB(_currentConfig.tourBackgroundColor1, data);
				final RGB contrastRGB = ColorUtil.getContrastRGB(tourBackgroundRGB);

				return _colorCache.getColor(contrastRGB.hashCode());
			}

		case WHITE:
			return _white;

		case BLACK:
		default:
			return _black;
		}
	}

	public LocalDateTime getFirstDay() {
		return _firstViewportDay;
	}

	private TemporalAdjuster getFirstDayOfWeek_SameOrNext() {

		return TemporalAdjusters.nextOrSame(TimeTools.getFirstDayOfWeek());
	}

	private TemporalAdjuster getFirstDayOfWeek_SameOrPrevious() {

		return TemporalAdjusters.previousOrSame(TimeTools.getFirstDayOfWeek());
	}

	private Font getFont_DateColumn() {

		if (_fontDateColumn == null) {

			final FontData fontData = CalendarConfigManager.getActiveCalendarConfig().dateColumnFont;

			_fontDateColumn = new Font(_display, fontData);

			final GC gc = new GC(_display);
			{
				gc.setFont(_fontDateColumn);
				_fontHeight_DateColumn = gc.getFontMetrics().getHeight();
			}
			gc.dispose();
		}

		return _fontDateColumn;
	}

	private Font getFont_DayContent() {

		if (_fontDayContent == null) {

			final FontData fontData = CalendarConfigManager.getActiveCalendarConfig().dayContentFont;

			_fontDayContent = new Font(_display, fontData);

			final GC gc = new GC(_display);
			{
				gc.setFont(_fontDayContent);
				_fontHeight_DayContent = gc.getFontMetrics().getHeight();
			}
			gc.dispose();
		}

		return _fontDayContent;
	}

	private Font getFont_DayHeader() {

		if (_fontDayHeader == null) {

			final FontData fontData = CalendarConfigManager.getActiveCalendarConfig().dayDateFont;

			_fontDayHeader = new Font(_display, fontData);

			final GC gc = new GC(_display);
			{
				gc.setFont(_fontDayHeader);
				_fontHeight_DayHeader = gc.getFontMetrics().getHeight();
			}
			gc.dispose();
		}

		return _fontDayHeader;
	}

	private Font getFont_YearHeader() {

		if (_fontYearHeader == null) {

			final FontData fontData = CalendarConfigManager.getActiveCalendarConfig().yearHeaderFont;

			_fontYearHeader = new Font(_display, fontData);

			final GC gc = new GC(_display);
			{
				gc.setFont(_fontYearHeader);
				_fontHeight_YearHeader = gc.getFontMetrics().getHeight();
			}
			gc.dispose();
		}

		return _fontYearHeader;
	}

	private WeekFormatter getFormatter(final WeekFormatterID id) {

		for (final WeekFormatter formatter : CalendarConfigManager.allWeekFormatter) {

			if (id == formatter.id) {
				return formatter;
			}
		}

		return CalendarConfigManager.DEFAULT_WEEK_SUMMARY_FORMATTER;
	}

	/**
	 * @return Returns the hovered tour or <code>null</code> when a tour is not hovered.
	 */
	CalendarSelectItem getHoveredTour() {

		return _hoveredTour;
	}

	public long getSelectedTourId() {
		if (_selectedItem.isTour()) {
			return _selectedItem.id;
		} else {
			return _emptyItem.id;
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

	private DateTimeFormatter getUI_DayDateFormatter(	final CalendarConfig config,
														final GC gc,
														final float cellWidth,
														final int dayLabelXOffset) {

		DateTimeFormatter headerFormatter;

		switch (config.dayDateFormat) {
		case DAY:

			headerFormatter = TimeTools.Formatter_Day;
			break;

		case DAY_MONTH:

			headerFormatter = TimeTools.Formatter_DayMonth;
			break;

		case DAY_MONTH_YEAR:

			headerFormatter = TimeTools.Formatter_DayMonthYear;
			break;

		case AUTOMATIC:
		default:

			final DateTimeFormatter[] allHeaderFormatter = {

					TimeTools.Formatter_DayMonthYear,
					TimeTools.Formatter_DayMonth,
					TimeTools.Formatter_Day,
			};

			// a rough guess about the max size of the label
			gc.setFont(_boldFont);
			final Point[] headerDateSizes = {

					gc.stringExtent("22. MMM 99"), //$NON-NLS-1$
					gc.stringExtent("22. MMM"), //$NON-NLS-1$
					gc.stringExtent("22"), //$NON-NLS-1$

//					gc.stringExtent(TimeTools.Formatter_DayMonthYear.format(currentDate)),
//					gc.stringExtent(TimeTools.Formatter_DayMonth.format(currentDate)),
//					gc.stringExtent(TimeTools.Formatter_Day.format(currentDate)),
			};

			// Find a format for the day header which fits into the rectangle available;
			int headerSizeIndex = 0;
			while (headerSizeIndex < headerDateSizes.length //
					&& headerDateSizes[headerSizeIndex].x > (cellWidth - dayLabelXOffset)) {
				headerSizeIndex++;
			}

			// if the cell is smaller than the shortest format (no index 'g' was found)
			// we use the shortest format and relay on clipping
			headerSizeIndex = Math.min(headerSizeIndex, headerDateSizes.length - 1);

			headerFormatter = allHeaderFormatter[headerSizeIndex];

			break;
		}

		return headerFormatter;
	}

	public void gotoDate(final LocalDate date) {

		_firstViewportDay = date//

				// center date vertically on screen
				.minusWeeks(_numWeeksInOneColumn / 2)

				//				// set first day to start of week
				//				.with(getFirstDayOfWeek_SameOrNext())

				// set default time
				.atStartOfDay();

		updateUI();
	}

	public void gotoDate_Month(final int month) {

		// scroll to first day of the week containing the first day of this month

		_firstViewportDay = _firstViewportDay//

				// set month and 1st day
				.withMonth(month)
				.withDayOfMonth(1)

				// set first day to start of week
				.with(getFirstDayOfWeek_SameOrNext())

				// show the whole requested month
				//				.minusMonths(1)

				// show the requested month at the bottom of the calendar
				.minusWeeks(_numWeeksInOneColumn)

		;

		_yearColumn_FirstYear = _yearColumn_FirstYear.withMonth(month);

		updateUI();
	}

	public void gotoDate_Today() {

		_firstViewportDay = LocalDateTime
				.now()

				.with(getFirstDayOfWeek_SameOrPrevious());

		_selectedItem = new CalendarSelectItem(0, ItemType.DAY);

		gotoDate(_firstViewportDay.toLocalDate());
	}

	public void gotoDate_Year(final int year) {

		_firstViewportDay = _firstViewportDay
				.withYear(year)

				// scroll to first day of the week
				.with(getFirstDayOfWeek_SameOrNext());

		_yearColumn_FirstYear = _yearColumn_FirstYear.withYear(year);

		updateUI();
	}

	public void gotoTour_First() {

		_firstViewportDay = _dataProvider

				.getFirstTourDateTime()

				// set first day to start of week
				.with(getFirstDayOfWeek_SameOrPrevious());

		gotoDate(_firstViewportDay.toLocalDate());
	}

	public void gotoTour_Id(final long tourId) {

		final LocalDateTime dt = _dataProvider.getCalendarTourDateTime(tourId);

		_selectedItem = new CalendarSelectItem(tourId, ItemType.TOUR);

		if (dt.isBefore(_firstViewportDay) || dt.isAfter(_firstViewportDay.plusWeeks(_numWeeksInOneColumn))) {

			_isGraphClean = false;
			gotoDate(dt.toLocalDate());

		} else {
			redraw();
		}
	}

	private void gotoTour_Offset(int offset) {

		if (_allTourFocusItems.size() < 1) {
			if (offset < 0) {
				scroll_Week(false);
			} else {
				scroll_Week(true);
			}
			return;
		}

		if (!_selectedItem.isTour()) { // if no tour is selected, count from first/last tour and select this tour
			if (offset > 0) {
				_selectedItem = new CalendarSelectItem(_allTourFocusItems.get(0).id, ItemType.TOUR);
				offset--;
			} else {
				_selectedItem = new CalendarSelectItem(
						_allTourFocusItems.get(_allTourFocusItems.size() - 1).id,
						ItemType.TOUR);
				offset++;
			}
		}

		boolean visible = false;
		int index = 0;
		for (final FocusItem ol : _allTourFocusItems) {
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
			scroll_Week(false);
			return;
		} else if (newIndex >= _allTourFocusItems.size()) {
			scroll_Week(true);
			return;
		} else {
			_selectedItem = new CalendarSelectItem(_allTourFocusItems.get(newIndex).id, ItemType.TOUR);

			_isGraphClean = false;
			redraw();
		}

	}

	private void gotoTour_SameWeekday(final int direction) {

		if (_allTourFocusItems.size() < 1) {

			if (direction < 0) {
				scroll_Week(true);
			} else {
				scroll_Week(false);
			}
			return;
		}

		if (!_selectedItem.isTour()) { // if no tour is selected, count from first/last tour and select this tour
			if (direction > 0) {
				_selectedItem = new CalendarSelectItem(_allTourFocusItems.get(0).id, ItemType.TOUR);
			} else {
				_selectedItem = new CalendarSelectItem(
						_allTourFocusItems.get(_allTourFocusItems.size() - 1).id,
						ItemType.TOUR);
			}
			redraw();
			return;
		}

		int index = 0;
		CalendarTourData ctd = null;
		for (final FocusItem focusItem : _allTourFocusItems) {
			if (_selectedItem.id == focusItem.id) {
				ctd = (focusItem.calendarTourData);
				break;
			}
			index++;
		}

		int dayOfWeekToGoTo = -1;
		if (null != ctd) {
			dayOfWeekToGoTo = ctd.dayOfWeek;
		} else if (_lastDayOfWeekToGoTo >= 0) { // selection scrolled out of view
			dayOfWeekToGoTo = _lastDayOfWeekToGoTo;
			index = direction > 0 ? 0 : _allTourFocusItems.size();
		}

		if (dayOfWeekToGoTo >= 0) {
			index += direction;
			for (int i = index; i >= 0 && i < _allTourFocusItems.size(); i += direction) {

				final FocusItem focusItem = _allTourFocusItems.get(i);

				if (dayOfWeekToGoTo == (focusItem.calendarTourData).dayOfWeek) {

					_selectedItem = new CalendarSelectItem(focusItem.id, ItemType.TOUR);
					_lastDayOfWeekToGoTo = dayOfWeekToGoTo;

					redraw();

					return;
				}
			}

		} else {

			// selected Item is not on the screen any more
			_selectedItem = _emptyItem;
		}

		if (direction < 0) {
			scroll_Week(true);
		} else {
			scroll_Week(false);
		}

	}

	private void onDispose() {

		_colorCache.dispose();

		_dragSource.dispose();
		_dropTarget.dispose();

		disposeFonts();
	}

	private void onDropTour(final long tourId, final DropTargetEvent event) {

		final TourData dragedTourData = TourManager.getInstance().getTourData(tourId);

		Assert.isNotNull(dragedTourData);

		// adjust tour start date
		final ZonedDateTime tourStartTime = dragedTourData.getTourStartTime();
		final ZonedDateTime newTourStartTime = tourStartTime//
				.withYear(_dragOverDate.getYear())
				.withMonth(_dragOverDate.getMonthValue())
				.withDayOfMonth(_dragOverDate.getDayOfMonth());

		if (tourStartTime.toLocalDate().toEpochDay() == newTourStartTime.toLocalDate().toEpochDay()) {

			// it is the same day -> do nothing
			// -> this could be improved that the drop operation is disabled

		} else {

			if (event.detail == DND.DROP_MOVE) {

				// move tour to another date

				dragedTourData.setTourStartTime(newTourStartTime);

				TourManager.saveModifiedTour(dragedTourData);

				TourLogManager.logDefault(
						NLS.bind(
								Messages.Log_Tour_MoveTour,
								TimeTools.Formatter_Date_M.format(tourStartTime),
								TimeTools.Formatter_Date_M.format(newTourStartTime)));

			} else if (event.detail == DND.DROP_COPY) {

				// copy tour

				try {

					final TourData tourDataCopy = (TourData) dragedTourData.clone();

					// set tour start date/time AFTER tour is copied !!!
					tourDataCopy.setTourStartTime(newTourStartTime);

					// tour id must be created after the tour date/time is set
					tourDataCopy.createTourId();

					TourManager.saveModifiedTour(tourDataCopy);

					TourLogManager.logDefault(
							NLS.bind(
									Messages.Log_Tour_CopyTour,
									TimeTools.Formatter_Date_M.format(tourStartTime),
									TimeTools.Formatter_Date_M.format(newTourStartTime)));

				} catch (final CloneNotSupportedException e) {
					StatusUtil.log(e);
				}
			}
		}
	}

	/**
	 * Mouse move event handler
	 * 
	 * @param event
	 */
	private void onMouseMove(final MouseEvent event) {

		if (_calendarImage == null || _calendarImage.isDisposed()) {
			return;
		}

		final CalendarSelectItem oldHoveredItem = _hoveredItem;
		final CalendarSelectItem oldSelectedItem = _selectedItem;

		if (1 == event.button || 3 == event.button) {
			_selectedItem = _emptyItem;
		}

		// reset hover state
		_hoveredTour = null;

		boolean isTourHovered = false;
		boolean isDayHovered = false;

		for (final FocusItem tourFocusItem : _allTourFocusItems) {

			if (tourFocusItem.rect.contains(event.x, event.y)) {

				final long id = tourFocusItem.id;

				if (1 == event.button || 3 == event.button) {

					final CalendarTourData calendarTourData = tourFocusItem.calendarTourData;

					_selectedItem = new CalendarSelectItem(id, ItemType.TOUR);

					_selectedItem.calendarTourData = calendarTourData;
					_selectedItem.canItemBeDragged = calendarTourData.isManualTour;

				} else if (oldHoveredItem.id != id) {

					// a new item is hovered

					_hoveredItem = new CalendarSelectItem(id, ItemType.TOUR);
				}

				_hoveredTour = new CalendarSelectItem(id, ItemType.TOUR);
				_hoveredTour.itemRectangle = tourFocusItem.rect;

				isTourHovered = true;

				break;
			}
		}

		if (!isTourHovered) {

			for (final FocusItem itemLocation : _allDayFocusItems) {

				if (itemLocation.rect.contains(event.x, event.y)) {

					final long id = itemLocation.id;

					if (1 == event.button || 3 == event.button) {

						_selectedItem = new CalendarSelectItem(id, ItemType.DAY);

					} else if (oldHoveredItem.id != id) { // a new object is highlighted

						_hoveredItem = new CalendarSelectItem(id, ItemType.DAY);
					}

					isDayHovered = true;

					break;
				}
			}
		}

		if (!oldSelectedItem.equals(_selectedItem)) {

			// highlight selection -> redraw calendar

			redraw();
			return;
		}

		if (!isDayHovered && !isTourHovered) {

			// only draw base calendar, skip highlighting

			redraw();
			return;
		}

		_isHoveredModified = !oldHoveredItem.equals(_hoveredItem);
		if (_isHoveredModified) {

			// only draw the highlighting on top of the calendar image
			redraw();
		}

		return;
	}

	public void refreshCalendar() {

		_dataProvider.invalidate();
		_isGraphClean = false;

		redraw();
	}

	public void removeSelection() {

		if (!_selectedItem.equals(_emptyItem)) {

			_selectedItem = _emptyItem;
			_isGraphClean = false;

			redraw();
		}
	}

	void removeSelectionListener(final ICalendarSelectionProvider listener) {
		_selectionProvider.remove(listener);
	}

	public void scroll_Screen(final boolean isNext) {

		final boolean useDraggedScrolling = _currentConfig.useDraggedScrolling;

		final int numPageWeeks = _numWeeksInOneColumn / 2;

		if (_isYearColumn) {

			if (_currentConfig.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

				LocalDateTime yearColumn_WithAdjustedWeeks;

				if (isNext) {
					yearColumn_WithAdjustedWeeks = useDraggedScrolling
							? _yearColumn_FirstYear.plusWeeks(numPageWeeks)
							: _yearColumn_FirstYear.minusWeeks(numPageWeeks);
				} else {
					yearColumn_WithAdjustedWeeks = useDraggedScrolling
							? _yearColumn_FirstYear.minusWeeks(numPageWeeks)
							: _yearColumn_FirstYear.plusWeeks(numPageWeeks);
				}

				_yearColumn_FirstYear = yearColumn_WithAdjustedWeeks

						// must start ALWAYS with 1st of month
						.withDayOfMonth(1);

			} else {

				// scroll year column

				if (isNext) {
					_yearColumn_FirstYear = useDraggedScrolling
							? _yearColumn_FirstYear.plusYears(1)
							: _yearColumn_FirstYear.minusYears(1);
				} else {

					_yearColumn_FirstYear = useDraggedScrolling
							? _yearColumn_FirstYear.minusYears(1)
							: _yearColumn_FirstYear.plusYears(1);
				}
			}

		} else {

			if (isNext) {
				_firstViewportDay = useDraggedScrolling
						? _firstViewportDay.plusWeeks(numPageWeeks)
						: _firstViewportDay.minusWeeks(numPageWeeks);
			} else {
				_firstViewportDay = useDraggedScrolling
						? _firstViewportDay.minusWeeks(numPageWeeks)
						: _firstViewportDay.plusWeeks(numPageWeeks);
			}
		}

		updateUI();
	}

	public void scroll_Tour(final boolean isNext) {

		gotoTour_Offset(isNext ? +1 : -1);
	}

	public void scroll_Week(final boolean isNext) {

		final boolean useDraggedScrolling = _currentConfig.useDraggedScrolling;

		if (_isYearColumn) {

			if (_currentConfig.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

				if (isNext) {
					_yearColumn_FirstYear = useDraggedScrolling
							? _yearColumn_FirstYear.plusMonths(1)
							: _yearColumn_FirstYear.minusMonths(1);
				} else {
					_yearColumn_FirstYear = useDraggedScrolling
							? _yearColumn_FirstYear.minusMonths(1)
							: _yearColumn_FirstYear.plusMonths(1);
				}

			} else {

				// scroll year column

				if (isNext) {
					_yearColumn_FirstYear = useDraggedScrolling
							? _yearColumn_FirstYear.plusYears(1)
							: _yearColumn_FirstYear.minusYears(1);
				} else {

					_yearColumn_FirstYear = useDraggedScrolling
							? _yearColumn_FirstYear.minusYears(1)
							: _yearColumn_FirstYear.plusYears(1);
				}
			}

		} else {

			if (isNext) {
				_firstViewportDay = useDraggedScrolling
						? _firstViewportDay.plusWeeks(1)
						: _firstViewportDay.minusWeeks(1);
			} else {
				_firstViewportDay = useDraggedScrolling
						? _firstViewportDay.minusWeeks(1)
						: _firstViewportDay.plusWeeks(1);
			}
		}

		updateUI();
	}

	private LocalDate scrollBar_getEndOfTours() {

		final LocalDate endDate = LocalDate//

				.now()

				.with(getFirstDayOfWeek_SameOrNext())

				// remove these week otherwise the last week is at the top of the calendar and not at the bottom
				//				.minusWeeks(_numWeeksInOneColumn)

				.plusWeeks(_scrollBar_OutsideWeeks)

		;

		return endDate;
	}

	private LocalDate scrollBar_getStartOfTours() {

		LocalDate firstTourDate = _dataProvider.getFirstTourDateTime().toLocalDate();
		final LocalDate today = LocalDateTime.now().toLocalDate();

		final long availableTourWeeks = (today.toEpochDay() - firstTourDate.toEpochDay()) / 7;

		// ensure the scrollable area has a reasonable size
		if (availableTourWeeks < MIN_SCROLLABLE_WEEKS) {
			firstTourDate = today.minusWeeks(MIN_SCROLLABLE_WEEKS);
		}

		// ensure the date return is a "FirstDayOfTheWeek" !!!
		final LocalDate startDate = firstTourDate//
//				.minusWeeks(1)

				.with(getFirstDayOfWeek_SameOrNext())

				.plusWeeks(_scrollBar_OutsideWeeks)

		;

		return startDate;
	}

	/**
	 * Called when scrolled with the scrollbar slider
	 * 
	 * @param event
	 */
	private void scrollBar_onScroll(final SelectionEvent event) {

		if (_isInUpdateScrollbar) {

			// prevent additional scrolling

			_isInUpdateScrollbar = false;

			return;
		}

		final ScrollBar sb = _parent.getVerticalBar();

		final int selectableMin = 0 + 1;
		final int selectableMax = sb.getMaximum() - sb.getThumb() - 1;

		final int currentSelection = sb.getSelection();
		final int selectionDiff = currentSelection - _scrollBar_LastSelection;

		if (_scrollBar_OutsideWeeks == 0) {

			// scrolled is inside tour weeks

			if (currentSelection < selectableMin) {

				// we are at the upper border

//				sb.setSelection(selectableMin);
				_scrollBar_OutsideWeeks += selectionDiff;
			}

			if (currentSelection > selectableMax) {

				// we are at the lower border

//				sb.setSelection(selectableMax);
				_scrollBar_OutsideWeeks += selectionDiff;

			}

		} else {

			// scrolled is outside of the tour weeks

			if (_scrollBar_LastSelection == selectableMax) {
//				sb.setSelection(selectableMax);
			} else if (_scrollBar_LastSelection == selectableMin) {
//				sb.setSelection(selectableMin);
			}

			if (selectionDiff > 0 && _scrollBar_OutsideWeeks < 0) {

				// ensure we are not shifting over "0"
				_scrollBar_OutsideWeeks = Math.min(0, _scrollBar_OutsideWeeks + selectionDiff);

			} else if (selectionDiff < 0 && _scrollBar_OutsideWeeks > 0) {

				_scrollBar_OutsideWeeks = Math.max(0, _scrollBar_OutsideWeeks + selectionDiff);

			} else {
				_scrollBar_OutsideWeeks += selectionDiff;
			}

		}

		_scrollBar_LastSelection = sb.getSelection();

		// goto the selected week
		_firstViewportDay = scrollBar_getStartOfTours().atStartOfDay().plusDays(currentSelection * 7);

		_isGraphClean = false;
		redraw();

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] onScroll") //$NON-NLS-1$ //$NON-NLS-2$
//						+ ("\tselection: " + sb.getSelection()) //$NON-NLS-1$
//						+ ("\toutsideWeeks: " + _scrollBar_OutsideWeeks) //$NON-NLS-1$
//						+ ("\tmax: " + sb.getMaximum()) //$NON-NLS-1$
//						+ ("\tthumb: " + sb.getThumb()) //$NON-NLS-1$
//						+ ("\t_numWeeksInOneColumn: " + _numWeeksInOneColumn) //$NON-NLS-1$
//						+ ("\tfirstDay: " + _firstViewportDay.toLocalDate()) //$NON-NLS-1$
////				+ ("\t: " + )
//		);
//// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void scrollBar_updateScrollbar() {

		_scrollBar_OutsideWeeks = 0;

		final long scrollStartEpochDay = scrollBar_getStartOfTours().toEpochDay();
		final long scrollEndEpochDay = scrollBar_getEndOfTours().toEpochDay();

		final int tourWeeks = (int) ((scrollEndEpochDay - scrollStartEpochDay) / 7);

		// ensure max contains all visible weeks in the viewport
		int scrollbarMax = Math.max(_numWeeksInOneColumn, tourWeeks) + 2;

		// ensure the thumb isn't getting to small
		final int thumbSize = Math.max(_numWeeksInOneColumn, scrollbarMax / 20);

		final long firstViewportDay = _firstViewportDay.toLocalDate().toEpochDay();
		int scrollbarSelection;

		if (firstViewportDay < scrollStartEpochDay) {

			// shift negative

			_scrollBar_OutsideWeeks = (int) ((firstViewportDay - scrollStartEpochDay) / 7);
			scrollbarSelection = 1;//1;

		} else if (firstViewportDay > scrollEndEpochDay) {

			// shift positive

			_scrollBar_OutsideWeeks = (int) ((firstViewportDay - scrollEndEpochDay) / 7);
			scrollbarSelection = scrollbarMax - 1;

		} else {

			scrollbarSelection = (int) ((firstViewportDay - scrollStartEpochDay) / 7);
		}

		// scrollbars and thums are complicated !!!
		scrollbarMax += thumbSize;

		// update scrollbar
		_isInUpdateScrollbar = true;
		_parent.getVerticalBar().setValues(

				scrollbarSelection,

				0, // min
				scrollbarMax, // max

				thumbSize,

				1, // increment
				_numWeeksInOneColumn / 2 // page increment
		);

		_scrollBar_LastSelection = scrollbarSelection;

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] upScroll") //$NON-NLS-1$ //$NON-NLS-2$
//						+ ("\tselection: " + scrollbarSelection) //$NON-NLS-1$
//						+ ("\toutsideWeeks: " + _scrollBar_OutsideWeeks) //$NON-NLS-1$
//						+ ("\tmax: " + scrollbarMax) //$NON-NLS-1$
//						+ ("\tthumb: " + thumbSize) //$NON-NLS-1$
//						+ ("\t_numWeeksInOneColumn: " + _numWeeksInOneColumn) //$NON-NLS-1$
//						+ ("\tfirstDay: " + _firstViewportDay.toLocalDate()) //$NON-NLS-1$
////				+ ("\t: " + )
//		);
//// TODO remove SYSTEM.OUT.PRINTLN
	}

	public void setFirstDay(final LocalDate dt) {

		_firstViewportDay = dt//

				// set default time
				.atStartOfDay()

				// set first day to start of week
				.with(getFirstDayOfWeek_SameOrPrevious());
	}

	void setLinked(final boolean linked) {

		if (false == linked) {

			_selectedItem = _emptyItem;

			_isGraphClean = false;
			redraw();
		}
	}

	public void setSelectionTourId(final long selectedTourId) {

		_selectedItem = new CalendarSelectItem(selectedTourId, ItemType.TOUR);
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

	private void updateUI() {

		_isGraphClean = false;
		redraw();

		// run async that the calendar is drawn first which sets some fields
		getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				scrollBar_updateScrollbar();
			}
		});
	}

	/**
	 * @param isResetUIResources
	 *            When <code>true</code> then UI resources will be reset and recreated by the next
	 *            drawing.
	 */
	void updateUI_Layout(final boolean isResetUIResources) {

		if (isResetUIResources) {
			disposeFonts();
		}

		// invalidate layout
		updateUI();
	}

	/**
	 * Update month/year dropdown box.
	 * <p>
	 * Look at the 1st day of the week after the first day displayed because if we go to a specific
	 * month we ensure that the first day of the month is displayed in the first line, meaning the
	 * first day in calendar normally contains a day of the *previous* month
	 */
	private void updateUI_YearMonthCombo(final LocalDate currentDate) {

		_calendarYearMonthContributor.setDate(currentDate);
	}

	private LocalDateTime yearColumn_getFirstDayOfMonth(final LocalDateTime currentFirstDay) {

		final LocalDateTime firstDayOfMonth = currentFirstDay.with(TemporalAdjusters.firstDayOfMonth());

		switch (_currentConfig.yearColumnsStart) {

		case JAN:
			return firstDayOfMonth.withMonth(Month.JANUARY.getValue());

		case FEB:
			return firstDayOfMonth.withMonth(Month.FEBRUARY.getValue());

		case MAR:
			return firstDayOfMonth.withMonth(Month.MARCH.getValue());

		case APR:
			return firstDayOfMonth.withMonth(Month.APRIL.getValue());

		case MAY:
			return firstDayOfMonth.withMonth(Month.MAY.getValue());

		case JUN:
			return firstDayOfMonth.withMonth(Month.JUNE.getValue());

		case JUL:
			return firstDayOfMonth.withMonth(Month.JULY.getValue());

		case AUG:
			return firstDayOfMonth.withMonth(Month.AUGUST.getValue());

		case SEP:
			return firstDayOfMonth.withMonth(Month.SEPTEMBER.getValue());

		case OCT:
			return firstDayOfMonth.withMonth(Month.OCTOBER.getValue());

		case NOV:
			return firstDayOfMonth.withMonth(Month.NOVEMBER.getValue());

		case DEC:
			return firstDayOfMonth.withMonth(Month.DECEMBER.getValue());

		default:
			break;
		}

		// this is used for continuously column start
		return currentFirstDay;
	}

}
