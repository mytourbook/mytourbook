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
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
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
	private boolean								_isGraphDirty			= true;
	private boolean								_isHoveredModified;
	private boolean								_isHoveredPainted;
	private boolean								_isFocusGained;
	//
	private CalendarSelectItem					_emptyItem				= new CalendarSelectItem(-1, ItemType.EMPTY);
	private CalendarSelectItem					_hoveredItem			= _emptyItem;
	private CalendarSelectItem					_selectedItem			= _emptyItem;
	private CalendarSelectItem					_lastSelectedItem		= _emptyItem;
	private CalendarSelectItem					_hoveredTour;
	//
	private Rectangle							_calendarCanvas;
	final private Rectangle						_nullRec				= null;
	//
	private CalendarView						_calendarView;
	private CalendarTourDataProvider			_dataProvider;
	private CalendarYearMonthContributionItem	_calendarYearMonthContributor;
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
	private int									_numYearColumns;
	//
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

	private int									_fontHeight_DateColumn;
	private int									_fontHeight_DayHeader;
	private int									_fontHeight_TourContent;
	private int									_fontHeight_TourTitle;
	private int									_fontHeight_TourValue;
	private int									_fontHeight_WeekValue;
	private int									_fontHeight_YearHeader;
	//
	/**
	 * First day which is visible in the calendar.
	 * <p>
	 * Date/time is necessary otherwise {@link Duration} will NOT work !!!
	 */
	private LocalDateTime						_firstVisibleDay		= LocalDateTime
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
	private CalendarProfile						_currentProfile;
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
	private Color								_calendarFgColor;
	private Color								_calendarBgColor;
	//
	private RGB									_day_TourBackgroundRGB;
	private RGB									_whiteRGB				= _white.getRGB();
	private RGB									_blackRGB				= _black.getRGB();
	//
	private Font								_boldFont;
	private Font								_fontDateColumn;
	private Font								_fontDayHeader;
	private Font								_fontTourContent;
	private Font								_fontTourTitle;
	private Font								_fontTourValue;
	private Font								_fontWeekValue;
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

	private enum ItemType {

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

		_dataProvider = CalendarTourDataProvider.getInstance();
		_dataProvider.setCalendarGraph(this);

		_rgbBright = new ArrayList<RGB>();
		_rgbDark = new ArrayList<RGB>();
		_rgbLine = new ArrayList<RGB>();
		_rgbText = new ArrayList<RGB>();

		// setup profile BEFORE updating tour type colors !!!
		setupProfile();

		updateTourTypeColors();

		addListener();
		addListener_Key();
		addListener_Mouse();

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

						for (final FocusItem dayFocusItem : _allDayFocusItems) {

							if (dayFocusItem.rect.contains(dragOverPosition.x, dragOverPosition.y)) {

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
								_dragOverDate = dayFocusItem.dayDate;

								final CalendarItemTransferData transferData = (CalendarItemTransferData) data;

								_hoveredItem = new CalendarSelectItem(dayFocusItem.id, ItemType.DAY);

								_hoveredItem.isDragOverItem = true;
								_hoveredItem.calendarTourData = transferData.calendarTourData;

								setHoveredModified(!oldHoveredItem.equals(_hoveredItem));

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
				setHoveredModified(!oldHoveredItem.equals(_hoveredItem));

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

		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent event) {

				_isGraphDirty = true;

				redraw();
			}
		});

		addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(final FocusEvent e) {

				_isFocusGained = true;

				redraw();
			}

			@Override
			public void focusLost(final FocusEvent e) {

				_isFocusGained = false;

				redraw();
			}
		});
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	private void addListener_Key() {

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
						scroll_Tour(1);
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

				final int keyCode = event.keyCode;

				switch (keyCode) {

				/*
				 * Next/previous tour
				 */
				case SWT.ARROW_LEFT:
				case 'h':
					scroll_Tour(-1);
					break;

				case SWT.ARROW_RIGHT:
				case 'l':
					scroll_Tour(1);
					break;

				/*
				 * Up/down tour/day
				 */
				case SWT.ARROW_UP:
				case 'k':
					if (_selectedItem.isTour()) {
						gotoTour_SameWeekday(-1);
					} else {
						scroll_ByDate_WithKeys(-1);
					}
					break;

				case SWT.ARROW_DOWN:
				case 'j':
					if (_selectedItem.isTour()) {
						gotoTour_SameWeekday(1);
					} else {
						scroll_ByDate_WithKeys(1);
					}
					break;

				case SWT.PAGE_UP:
				case 'p':
					scroll_WithKey_Screen(-1);
					break;

				case SWT.PAGE_DOWN:
				case 'n':
					scroll_WithKey_Screen(1);
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
						scroll_Tour(1);
					}
					break;

				/*
				 * Zoom
				 */
				case '+':
				case '-':
				case SWT.KEYPAD_ADD:
				case SWT.KEYPAD_SUBTRACT:

					if (UI.isCtrlKey(event)) {

						int direction;

						if (keyCode == '+' || keyCode == SWT.KEYPAD_ADD) {
							direction = 1;
						} else {
							direction = -1;
						}

						zoom(event, direction);
					}
					break;

				case '0':
				case SWT.KEYPAD_0:
					if (UI.isCtrlKey(event)) {

						zoom_ToDefault();
					}

					break;
				}
			}
		});
	}

	private void addListener_Mouse() {

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				onMouse_DoubleClick();
			}

			@Override
			public void mouseDown(final MouseEvent e) {
				onMouse_MoveDown(e);
			}
		});

		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(final MouseEvent e) {
				onMouse_MoveDown(e);
			}
		});

		addMouseTrackListener(new MouseTrackAdapter() {

			@Override
			public void mouseExit(final MouseEvent e) {
				onMouse_Exit();
			}
		});

		addListener(SWT.MouseVerticalWheel, new Listener() {

			@Override
			public void handleEvent(final Event event) {
				onMouse_Wheel(event);
			}
		});

		_parent.getVerticalBar().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				scrollBar_onScroll(event);
			}
		});
	}

	private void disposeFonts() {

		_fontDateColumn = UI.disposeResource(_fontDateColumn);
		_fontDayHeader = UI.disposeResource(_fontDayHeader);
		_fontTourContent = UI.disposeResource(_fontTourContent);
		_fontTourTitle = UI.disposeResource(_fontTourTitle);
		_fontTourValue = UI.disposeResource(_fontTourValue);
		_fontWeekValue = UI.disposeResource(_fontWeekValue);
		_fontYearHeader = UI.disposeResource(_fontYearHeader);
	}

	void draw() {

		_isGraphDirty = true;
		redraw();
	}

	/**
	 * Paint calendar
	 * 
	 * @param gc
	 */
	private void drawCalendar(final GC calendarGC) {

		_calendarCanvas = getClientArea();
		final int canvasWidth = _calendarCanvas.width;
		final int canvasHeight = _calendarCanvas.height;

		final int calendarMargin = 5;
		final int calendarWidth = canvasWidth - calendarMargin * 2;
		final int calendarHeight = canvasHeight - calendarMargin;

		/*
		 * In focus control and focus gained can be different, for win7 it depends if a dialog was
		 * opened with the mouse or keyboard !!!
		 */
		final boolean isFocus = isFocusControl() || _isFocusGained;

		if (_isGraphDirty == false && _calendarImage != null) {

			// graph image must not be updated, draw additional only selection/hovered/focus

			final Image hoveredImage = new Image(getDisplay(), canvasWidth, canvasHeight);
			final GC gcHovered = new GC(hoveredImage);
			{
				gcHovered.drawImage(_calendarImage, 0, 0);

				drawSelection(gcHovered, isFocus);

				/*
				 * Complex: Ensure with _isHoveredPainted that the hovered state is also painted
				 * even when _isHoveredModified is set back to false. This problem occured when a
				 * tour tooltip is displayed (and took some hours to fix this problem) !!!
				 */
				if (_isHoveredModified || _isHoveredPainted == false) {

					drawHovered(gcHovered);

					setHoveredModified(false);
				}

				calendarGC.drawImage(hoveredImage, 0, 0);
			}
			gcHovered.dispose();
			hoveredImage.dispose();

			return;
		}

		// set state very early that data loading can reset it
		_isGraphDirty = false;

		// set profile and all its parameters
		setupProfile();

		if (_calendarImage != null && !_calendarImage.isDisposed()) {
			_calendarImage.dispose();
		}

		_calendarImage = new Image(getDisplay(), canvasWidth, canvasHeight);

		final GC gc = new GC(_calendarImage);

		gc.setForeground(_black);
		gc.setBackground(_calendarBgColor);
		gc.fillRectangle(_calendarCanvas);

		final Font normalFont = gc.getFont();

		final boolean useYearColumns = _currentProfile.isShowYearColumns;

		// set year header font
		getFont_YearHeader();

		final int yearHeaderHeight = useYearColumns
				? _fontHeight_YearHeader + 20
				: calendarMargin;

		final int calendarHeightWithoutYearHeader = calendarHeight - yearHeaderHeight;

		final int weekHeight = _currentProfile.isWeekRowHeight
				? _currentProfile.weekHeight
				: Math.max(
						CalendarProfileManager.WEEK_HEIGHT_MIN,
						calendarHeightWithoutYearHeader / _currentProfile.weekRows);

		int numVisibleRows = calendarHeightWithoutYearHeader / weekHeight;

		if (useYearColumns) {

			// adjust column start

			if (_currentProfile.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

				_yearColumn_CurrentYear = _yearColumn_FirstYear;

				// set first day to start of week
				_firstVisibleDay = _yearColumn_CurrentYear.with(getFirstDayOfWeek_SameOrPrevious());

			} else {

				_yearColumn_FirstYear = yearColumn_getFirstDayOfMonth(_yearColumn_FirstYear);
				_yearColumn_CurrentYear = _yearColumn_FirstYear;
				_yearColumn_NextYear = _yearColumn_CurrentYear.plusYears(1);

				// set first day to start of week
				_firstVisibleDay = _yearColumn_CurrentYear.with(getFirstDayOfWeek_SameOrPrevious());

				final LocalDateTime firstCalendarDay_NextYear = _yearColumn_NextYear

						// set first day to start of week
						.with(getFirstDayOfWeek_SameOrPrevious());

				/*
				 * adjust number of weeks
				 */
				final int numYearWeeks = (int) ChronoUnit.WEEKS.between(
						_firstVisibleDay,
						firstCalendarDay_NextYear.plusWeeks(1).with(getFirstDayOfWeek_SameOrNext()));

				if (numYearWeeks < numVisibleRows) {

					// less weeks in one year than visible rows -> show weeks for only one year

					numVisibleRows = numYearWeeks;
				}
			}
		}

		_numWeeksInOneColumn = numVisibleRows;

		final Color monthAlternateColor = _colorCache.getColor(_currentProfile.alternateMonthRGB);

		_allTourFocusItems.clear();
		_allDayFocusItems.clear();

		int dateColumnWidth = 0;
		if (_currentProfile.isShowDateColumn) {
			dateColumnWidth = _currentProfile.dateColumnWidth;
		}

		int summaryColumnWidth = 0;
		if (_currentProfile.isShowSummaryColumn) {
			summaryColumnWidth = _currentProfile.weekColumnWidth;
		}

		final int yearColumnsSpacing = _currentProfile.yearColumnsSpacing;
		final int yearColumnDayWidth = _currentProfile.yearColumnDayWidth;

		final int numDayColumns = 7;
		final int yearColumnWidth = dateColumnWidth + (yearColumnDayWidth * numDayColumns) + summaryColumnWidth;

		_numYearColumns = 1;
		if (useYearColumns) {

			if (_currentProfile.isYearColumnDayWidth) {

				_numYearColumns = calendarWidth / yearColumnWidth;

			} else {
				_numYearColumns = _currentProfile.yearColumns;
			}
		}

		final int allColumnSpace = (_numYearColumns - 1) * yearColumnsSpacing;
		int calendarColumnWidth;
		if (_currentProfile.isYearColumnDayWidth) {

			calendarColumnWidth = yearColumnWidth;

		} else {

			calendarColumnWidth = (calendarWidth - allColumnSpace) / _numYearColumns;
		}

		final float dayWidth = (float) (calendarColumnWidth - dateColumnWidth - summaryColumnWidth) / numDayColumns;

		_calendarAllDaysRectangle = new Rectangle[_numYearColumns];

		final long todayDayId = (new DayItem(LocalDate.now())).dayId;

		Font dayDateFont = null;
		int dayDateHeight = 0;
		if (_currentProfile.isShowDayDate) {
			dayDateFont = getFont_DayHeader();
			dayDateHeight = _fontHeight_DayHeader;
		}

		final int dayLabelRightBorder = 4;

		final DateTimeFormatter dayDateFormatter = getUI_DayDateFormatter(
				_currentProfile,
				gc,
				dayWidth,
				dayLabelRightBorder);

		LocalDate currentDate = _firstVisibleDay.toLocalDate();

		/*
		 * Set calendar viewport dates
		 */
		_calendarFirstDay = currentDate;

		if (useYearColumns) {

			if (_currentProfile.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

				_calendarLastDay = _calendarFirstDay.plusWeeks(numVisibleRows * _numYearColumns).minusDays(1);

			} else {

				_calendarLastDay = _yearColumn_FirstYear

						.toLocalDate()
						.plusYears(_numYearColumns)
						.minusDays(1)

						.with(getFirstDayOfWeek_SameOrNext())
						.minusDays(1);
			}

		} else {

			_calendarLastDay = _calendarFirstDay.plusWeeks(numVisibleRows * _numYearColumns).minusDays(1);
		}

		_calendarView.updateUI_Title(_calendarFirstDay, _calendarLastDay);

		// we use simple ids
		long dayId = new DayItem(currentDate).dayId;

		for (int columnIndex = 0; columnIndex < _numYearColumns; columnIndex++) {

			_nextWeekDateYPos = 0;

			final int columnColumSpacing = columnIndex == 0 ? 0 : yearColumnsSpacing;

			final int calendarColumnOffset = calendarMargin
					+ calendarColumnWidth * columnIndex
					+ columnColumSpacing * columnIndex;

			_calendarAllDaysRectangle[columnIndex] = new Rectangle(
					calendarColumnOffset + dateColumnWidth,
					0,
					(int) (7 * dayWidth),
					calendarHeight);

			if (useYearColumns) {

				// move to the next year

				if (_currentProfile.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

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
							_currentProfile.dateColumnContent,
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

					final DayItem dayItem = new DayItem(dayId);
					_allDayFocusItems.add(new FocusItem(dayRect, dayId, dayItem, currentDate));

					dayId = dayItem.dayId + 1;

					gc.setFont(normalFont);
//					gc.setBackground(_white);

					// Day background with alternate color
					if (_currentProfile.isToggleMonthColor && currentDate.getMonthValue() % 2 == 1) {

						gc.setBackground(monthAlternateColor);
						gc.fillRectangle(//
								dayRect.x,
								dayRect.y,
								dayRect.width - 1,
								dayRect.height - 1);
					}

					_dayDateLabelRect = null;
					Rectangle dayDateMargin = null;
					String dayDateLabel = UI.EMPTY_STRING;
					int dateLabelPosX = 0;

					if (_currentProfile.isShowDayDate) {

						// get date label width
						dayDateLabel = UI.SPACE1 + dayDateFormatter.format(currentDate) + UI.SPACE1;

						gc.setFont(dayDateFont);
						final Point labelExtent = gc.stringExtent(dayDateLabel);
						final int dayLabelWidth = labelExtent.x;
						final int dayLabelHeight = labelExtent.y;

						final int labelWidthWithOffset = dayLabelWidth + dayLabelRightBorder;
						dateLabelPosX = dayPosXNext - labelWidthWithOffset;

						final int marginTop = _currentProfile.dayDateMarginTop;
						final int marginBottom = 0;
						final int marginLeft = _currentProfile.dayDateMarginLeft;
						final int marginRight = 0;

						dayDateMargin = new Rectangle(//
								dateLabelPosX + marginLeft,
								rowTop + marginTop,
								dayLabelWidth - marginLeft - marginRight,
								dayLabelHeight - marginTop - marginBottom);

						_dayDateLabelRect = new Rectangle(
								dayDateMargin.x,
								dayDateMargin.y,
								dayDateMargin.width,
								dayDateMargin.height);

					}

					final CalendarTourData[] calendarData = _dataProvider.getCalendarDayData(currentDate);

					final boolean isCalendarDataAvailable = calendarData.length > 0;

					final boolean isShowDayDate = _currentProfile.isHideDayDateWhenNoTour == false //
							|| _currentProfile.isHideDayDateWhenNoTour && isCalendarDataAvailable;

					if (isCalendarDataAvailable) {

						// tours are available

						drawDay(gc, calendarData, dayRect);
					}

					// draw day date AFTER the tour is painted
					if (_currentProfile.isShowDayDate && isShowDayDate) {

						// this clipping should only kick in if shortest label format is still longer than the cell width
						gc.setClipping(dayPosX, rowTop, dayRect.width, dayDateHeight);

						final int weekDay = currentDate.getDayOfWeek().getValue();

						final boolean isWeekendColor = _currentProfile.isShowDayDateWeekendColor //
								// ISO: 6 == saturday, 7 == sunday
								&& (weekDay == 6 || weekDay == 7);

						boolean isDateTransparent = true;
						Color dayDateForegroundColor;

						if (dayItem.dayId == todayDayId) {

							dayDateForegroundColor = (_blue);

						} else if (isWeekendColor) {

							dayDateForegroundColor = (_red);

						} else if (isCalendarDataAvailable) {

							dayDateForegroundColor = getColor_Tour(
									calendarData[0],
									_currentProfile.tourContentColor,
									_currentProfile.tourContentRGB);

						} else {

							dayDateForegroundColor = _calendarFgColor;
						}

						if (isWeekendColor && isCalendarDataAvailable) {

							// paint outline, red is not very good visible with a dark background

							isDateTransparent = false;

							// draw without additional background on the right side
							dateLabelPosX += 2;

							gc.setBackground(_calendarBgColor);
						}

						// day header label
						gc.setFont(dayDateFont);
						gc.setForeground(dayDateForegroundColor);
						gc.drawText(
								dayDateLabel,
								dayDateMargin.x, //dateLabelPosX,
								dayDateMargin.y, //rowTop,
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

			drawSelection(selectedGC, isFocus);

			calendarGC.drawImage(selectedImage, 0, 0);
		}
		selectedImage.dispose();
		selectedGC.dispose();

		updateUI_YearMonthCombo();

		if (_isGraphDirty) {

			// graph is dirty again, this can occure when data are loaded -> rescedule a new update

			getDisplay().timerExec(5, new Runnable() {
				@Override
				public void run() {
					redraw();
				}
			});
		}
	}

	private void drawDay(	final GC gc,
							final CalendarTourData[] allCalendarTourData,
							final Rectangle dayRect) {

		// setup font to set the font height !!!
		getFont_TourTitle();

		gc.setFont(getFont_TourContent());

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
		drawDay_TourConent(gc, tourRect, data);
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

		final int tourBackgroundWidth = _currentProfile.tourBackgroundWidth;

		final int marginWidth = (int) (tourBackgroundWidth <= 10 ? tourBackgroundWidth //
				: cellWidth * (tourBackgroundWidth / 100.0));

		boolean isGradient = false;
		boolean isVertical = false;

		_day_TourBackgroundRGB = getColor_Graph(
				data,
				_currentProfile.tourBackground1Color,
				_currentProfile.tourBackground1RGB);

		final Color backgroundColor = _colorCache.getColor(_day_TourBackgroundRGB);

		switch (_currentProfile.tourBackground) {

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
			gc.setBackground(
					_colorCache.getColor(
							getColor_Graph(
									data,
									_currentProfile.tourBackground2Color,
									_currentProfile.tourBackground2RGB)));
			break;

		case GRADIENT_VERTICAL:
			isGradient = true;
			isVertical = true;
			gc.setForeground(backgroundColor);
			gc.setBackground(
					_colorCache.getColor(
							getColor_Graph(
									data,
									_currentProfile.tourBackground2Color,
									_currentProfile.tourBackground2RGB)));
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

		final RGB tourBorderRGB = getColor_Graph(
				data,
				_currentProfile.tourBorderColor,
				_currentProfile.tourBorderRGB);
		final Color line = _colorCache.getColor(tourBorderRGB);

		gc.setForeground(line);
		gc.setBackground(line);

		final int tourBorderWidth = _currentProfile.tourBorderWidth;

		final int borderWidth = (int) (tourBorderWidth <= 10 ? tourBorderWidth //
				: cellWidth * (tourBorderWidth / 100.0));

		final int borderHeight = (int) (tourBorderWidth <= 10 ? tourBorderWidth //
				: cellHeight * (tourBorderWidth / 100.0));

		boolean isTop = false;
		boolean isLeft = false;
		boolean isRight = false;
		boolean isBottom = false;

		switch (_currentProfile.tourBorder) {

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

	private void drawDay_TourConent(final GC gc,
									final Rectangle tourRect,
									final CalendarTourData calendarTourData) {

		if (!_currentProfile.isShowTourContent) {
			return;
		}

		final int marginTop = _currentProfile.tourMarginTop;
		final int marginBottom = _currentProfile.tourMarginBottom;
		final int marginLeft = _currentProfile.tourMarginLeft;
		final int marginRight = _currentProfile.tourMarginRight;

		final Rectangle marginRect = new Rectangle(//
				tourRect.x + marginLeft,
				tourRect.y + marginTop,
				tourRect.width - marginLeft - marginRight,
				tourRect.height - marginTop - marginBottom);

		gc.setClipping(marginRect.x, marginRect.y, marginRect.width, marginRect.height);

		int valuePosY = marginRect.y;
		final int posYBottom = marginRect.y + marginRect.height;

		final int numValueColumns = _currentProfile.tourValueColumns;
		final int columnWidth = marginRect.width / numValueColumns;
		int currentColumn = 0;

		boolean isTextValue = false;
		int lastPaintedY = 0;
		int lastHeight = -1;

		for (final FormatterData formatterData : _currentProfile.allTourFormatterData) {

			if (formatterData.isEnabled == false /* || formatterData.id == FormatterID.EMPTY */) {

				// formatter is not valid
				continue;
			}

			final int maxValueHeight = posYBottom - valuePosY;
			if (maxValueHeight < 1) {

				// there is no space any more to draw text
				break;
			}

			final DataFormatter formatter = getValueFormatter(
					formatterData.id,
					CalendarProfileManager.allTourContentFormatter);

			final String valueText = formatter.format(
					calendarTourData,
					formatterData.valueFormat,
					_currentProfile.isShowTourValueUnit);

			final boolean isEmptyFormatter = formatterData.id == FormatterID.EMPTY;

			if (valueText != null && valueText.length() > 0 || isEmptyFormatter) {

				final boolean isTourTitle = formatter.id == FormatterID.TOUR_TITLE;
				final boolean isTourContent = formatter.id == FormatterID.TOUR_DESCRIPTION;
				final boolean isUseColumns = !isTourTitle && !isTourContent && numValueColumns > 1;

				final boolean wasTextValue = isTextValue;
				isTextValue = isTourTitle || isTourContent;

				// skip 1st painted line
				if (lastHeight > -1) {

					// adjust y position accoring which was painted before and which is painted now

					if (isTextValue && wasTextValue == false) {
						valuePosY = lastPaintedY + lastHeight;
					}
				}

				boolean isTruncateTourText = _currentProfile.isTruncateTourText;
				int tourTruncatedLines = _currentProfile.tourTruncatedLines;
				final int fontHeight;

				if (isTourTitle) {

					fontHeight = _fontHeight_TourTitle;

					gc.setFont(getFont_TourTitle());
					gc.setForeground(
							getColor_Tour(
									calendarTourData,
									_currentProfile.tourTitleColor,
									_currentProfile.tourTitleRGB));

				} else if (isTourContent) {

					fontHeight = _fontHeight_TourContent;

					gc.setFont(getFont_TourContent());
					gc.setForeground(
							getColor_Tour(
									calendarTourData,
									_currentProfile.tourContentColor,
									_currentProfile.tourContentRGB));

				} else {

					// tour value

					fontHeight = _fontHeight_TourValue;

					gc.setFont(getFont_TourValue());
					gc.setForeground(
							getColor_Tour(
									calendarTourData,
									_currentProfile.tourValueColor,
									_currentProfile.tourValueRGB));

					// do not truncate values, it looks ugly when the value unit is at the next line
					isTruncateTourText = true;
					tourTruncatedLines = 1;
				}

				// reset title font/color -> content font/color
				if (isTourTitle) {}

				int valuePosX;
				int valueWidth;

				if (isUseColumns) {

					// complicated formatting with >1 columns

					final int columnOffset = currentColumn * columnWidth;

					valueWidth = columnWidth;
					valuePosX = marginRect.x + columnOffset;

				} else {

					// easy formatting with 1 column

					valuePosX = marginRect.x;
					valueWidth = marginRect.width;
				}

				gc.setClipping(valuePosX, valuePosY, valueWidth, maxValueHeight);

				_textWrapPainter.drawText(
						gc,
						valueText,

						valuePosX,
						valuePosY,

						valueWidth,
						maxValueHeight,

						fontHeight,
						_dayDateLabelRect,

						isTruncateTourText,
						tourTruncatedLines);

				if (_textWrapPainter.isPainted() || isEmptyFormatter) {

					lastPaintedY = _textWrapPainter.getLastPaintedY();
					lastHeight = fontHeight;

					if (isUseColumns == false || currentColumn >= numValueColumns - 1) {

						// move to the next line

						valuePosY = lastPaintedY + lastHeight;
					}

					// advance to the next column
					if (isUseColumns) {
						currentColumn = currentColumn >= numValueColumns - 1 ? 0 : currentColumn + 1;
					}

					// reset columns (start from 1st column) after title or description is painted
					if (isTextValue) {
						currentColumn = 0;
					}
				}
			}
		}

		gc.setClipping(_nullRec);
	}

	private void drawFocus(final GC gc) {

		gc.setForeground(_red);
		gc.drawFocus(
				_calendarCanvas.x,
				_calendarCanvas.y,
				_calendarCanvas.width,
				_calendarCanvas.height);
	}

	private void drawHovered(final GC gc) {

		_isHoveredPainted = true;

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

					// tour is hovered

					final Color color = getColor_Tour(
							focusItem.calendarTourData,
							_currentProfile.tourHoveredColor,
							_currentProfile.tourHoveredRGB);

					drawItem_Marked(gc, itemRectangle, color, true, false);

				} else if (focusItem.dayItem instanceof DayItem) {

					// day is hovered

					if (_hoveredItem.isDragOverItem) {

						// draw dragged

						final Color color = getColor_Tour(
								_hoveredItem.calendarTourData,
								_currentProfile.tourDraggedColor,
								_currentProfile.tourDraggedRGB);

						drawItem_Marked(gc, itemRectangle, color, true, false);

					} else {

						// draw day hovered

						final Color color = _colorCache.getColor(_currentProfile.dayHoveredRGB);

						drawItem_Marked(gc, itemRectangle, color, false, false);
					}
				}

				return;
			}
		}
	}

	private void drawItem_Marked(	final GC gc,
									final Rectangle itemRectangle,
									final Color color,
									final boolean isTour,
									final boolean isFocus) {

		final int lineWidth = isTour ? 7 : 3;
		final int lineWidth2 = lineWidth / 2;

		gc.setLineWidth(lineWidth);
		gc.setForeground(color);

		gc.drawRectangle(
				itemRectangle.x - 1 - lineWidth2,
				itemRectangle.y - 1 - lineWidth2,
				itemRectangle.width + 1 + lineWidth2 * 2,
				itemRectangle.height + 1 + lineWidth2 * 2);

		if (isFocus) {

			gc.drawFocus(
					itemRectangle.x - 1 - lineWidth,
					itemRectangle.y - 1 - lineWidth,
					itemRectangle.width + 1 + lineWidth * 2,
					itemRectangle.height + 1 + lineWidth * 2);
		}
	}

	private void drawSelection(final GC gc, final boolean isFocus) {

		if (!_lastSelectedItem.equals(_selectedItem)) {

			if (_selectedItem.type == ItemType.TOUR) {

				// fire ONLY tours

				final long tourId = _selectedItem.id;

				_calendarView.fireSelection(tourId);
			}

			_lastSelectedItem = _selectedItem;
		}

		List<FocusItem> allFocusItems;

		if (_selectedItem.type == ItemType.TOUR) {
			allFocusItems = _allTourFocusItems;
		} else if (_selectedItem.type == ItemType.DAY) {
			allFocusItems = _allDayFocusItems;
		} else {

			if (isFocus) {
				drawFocus(gc);
			}

			return;
		}

		for (final FocusItem focusItem : allFocusItems) {
			if (focusItem.id == _selectedItem.id) {

				if (focusItem.calendarTourData instanceof CalendarTourData) {

					// tour is selected

					final Color color = getColor_Tour(
							focusItem.calendarTourData,
							_currentProfile.tourSelectedColor,
							_currentProfile.tourSelectedRGB);

					drawItem_Marked(gc, focusItem.rect, color, true, isFocus);

				} else if (focusItem.dayItem instanceof DayItem) {

					// day is selected

					final Color color = _colorCache.getColor(_currentProfile.daySelectedRGB);

					drawItem_Marked(gc, focusItem.rect, color, false, isFocus);

				} else {

					if (isFocus) {
						drawFocus(gc);
					}
				}

				return;
			}
		}

// this seems to be confusing me
//
//		/*
//		 * Selected item is not available any more in the focus items, this occures when scrolled
//		 * with the mouse -> deselect item
//		 */
//		_selectedItem = _emptyItem;

		if (isFocus) {
			drawFocus(gc);
		}
	}

	private void drawWeek_DateColumn(	final GC gc,
										final LocalDate currentDate,
										final int rowTop,
										final int calendarColumnOffset,
										final DateColumnContent dateColumnContent,
										final boolean isFirstRow) {

		gc.setForeground(_calendarFgColor);
		gc.setFont(getFont_DateColumn());

		final int posX = calendarColumnOffset + 2;
		final int thisWeekYPos = rowTop;

		// prevent paint overlapping
		final boolean isInLastWeek = thisWeekYPos < _nextWeekDateYPos;

		if (isInLastWeek) {
			return;
		}

		final int nextWeekYPos = thisWeekYPos + _fontHeight_DateColumn + 0;

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
									final Rectangle weekRect) {

		final int marginTop = _currentProfile.weekMarginTop;
		final int marginBottom = _currentProfile.weekMarginBottom;
		final int marginLeft = _currentProfile.weekMarginLeft;
		final int marginRight = _currentProfile.weekMarginRight;

		final Rectangle marginRect = new Rectangle(//
				weekRect.x + marginLeft,
				weekRect.y + marginTop,
				weekRect.width - marginLeft - marginRight,
				weekRect.height - marginTop - marginBottom);

		final int posXRight = marginRect.x + marginRect.width;
		final int posXLeft = marginRect.x;

		int posY = marginRect.y;

		final int maxLength = weekRect.width;

		final Font normalFont = gc.getFont();

		gc.setFont(getFont_WeekValue());
		gc.setClipping(marginRect);

		gc.setBackground(_calendarBgColor);

		for (final FormatterData formatterData : _currentProfile.allWeekFormatterData) {

			if (formatterData.isEnabled && formatterData.id != FormatterID.EMPTY) {

				// a valid formatter is set

				final DataFormatter formatter = getValueFormatter(
						formatterData.id,
						CalendarProfileManager.allWeekFormatter);

				gc.setForeground(_colorCache.getColor(getColor_Week(formatter, _currentProfile.weekValueRGB)));

				String text = formatter.format(
						calendarTourData,
						formatterData.valueFormat,
						_currentProfile.isShowWeekValueUnit);

				if (text.length() > 0 && posY < (marginRect.y + marginRect.height)) {

					final Point textSize = gc.stringExtent(text);
					int posX = posXRight - textSize.x;

					if (textSize.x > maxLength) {

						// remove unit when not enough horizontal space is available
						if (text.contains(UI.SPACE1)) {
							text = text.substring(0, text.lastIndexOf(UI.SPACE));
							posX = posXRight - gc.stringExtent(text).x;
						} else {
							posX = posXLeft;
						}
					}

					gc.drawText(text, posX, posY);
				}

				posY += _fontHeight_WeekValue;
			}
		}

		gc.setFont(normalFont);
		gc.setClipping(_nullRec);
	}

	private void drawWeek_YearHeader(final GC gc, final Rectangle yearHeaderRect) {

		final String yearText = Integer.toString(
				_yearColumn_CurrentYear

						// move to end of the week that the year is displayed for the last day of the week
						.plusDays(6)

						.getYear());

		gc.setForeground(_calendarFgColor);
		gc.setBackground(_calendarBgColor);

		gc.setFont(getFont_YearHeader());

		final Point textSize = gc.stringExtent(yearText);

		final int posX = yearHeaderRect.x + yearHeaderRect.width / 2 - textSize.x / 2;
		final int posY = yearHeaderRect.y + 10;

		gc.drawString(yearText, posX, posY);

//		gc.setForeground(_red);
//		gc.drawRectangle(yearHeaderRect);
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

	private RGB getColor_Graph(final CalendarTourData data, final CalendarColor graphColor, final RGB customRGB) {

		final int typeColorIndex = data.typeColorIndex;

		switch (graphColor) {
		case BRIGHT:
			return _rgbBright.get(typeColorIndex);

		case LINE:
			return _rgbLine.get(typeColorIndex);

		case TEXT:
			return _rgbText.get(typeColorIndex);

		case WHITE:
			return _whiteRGB;

		case BLACK:
			return _blackRGB;

		case CUSTOM:
			return customRGB;

		case DARK:
		default:
			return _rgbDark.get(typeColorIndex);
		}
	}

	private Color getColor_Tour(final CalendarTourData data, final CalendarColor tourColor, final RGB customRGB) {

		final int typeColorIndex = data.typeColorIndex;

		switch (tourColor) {

		case BRIGHT:
			return _colorCache.getColor(_rgbBright.get(typeColorIndex));

		case DARK:
			return _colorCache.getColor(_rgbDark.get(typeColorIndex));

		case LINE:
			return _colorCache.getColor(_rgbLine.get(typeColorIndex));

		case TEXT:
			return _colorCache.getColor(_rgbText.get(typeColorIndex));

		case CONTRAST:

			if (_day_TourBackgroundRGB == null) {

				return _black;

			} else {

				final RGB tourBackgroundRGB = getColor_Graph(data, _currentProfile.tourBackground1Color, customRGB);
				final RGB contrastRGB = ColorUtil.getContrastRGB(tourBackgroundRGB);

				return _colorCache.getColor(contrastRGB);
			}

		case CUSTOM:
			return _colorCache.getColor(customRGB);

		case WHITE:
			return _white;

		case BLACK:
		default:
			return _black;
		}
	}

	private RGB getColor_Week(final DataFormatter formatter, final RGB customRGB) {

		final CalendarColor weekValueColor = _currentProfile.weekValueColor;

		switch (weekValueColor) {

		case BRIGHT:
		case DARK:
		case LINE:
		case TEXT:
			return formatter.getGraphColor(weekValueColor);

		case CUSTOM:
			return customRGB;

		case WHITE:
			return _whiteRGB;

		case BLACK:
		default:
			return _blackRGB;
		}
	}

	public LocalDateTime getFirstDay() {
		return _firstVisibleDay;
	}

	private TemporalAdjuster getFirstDayOfWeek_SameOrNext() {

		return TemporalAdjusters.nextOrSame(TimeTools.getFirstDayOfWeek());
	}

	private TemporalAdjuster getFirstDayOfWeek_SameOrPrevious() {

		return TemporalAdjusters.previousOrSame(TimeTools.getFirstDayOfWeek());
	}

	private Font getFont_DateColumn() {

		if (_fontDateColumn == null) {

			final FontData fontData = CalendarProfileManager.getActiveCalendarProfile().dateColumnFont;

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

	private Font getFont_DayHeader() {

		if (_fontDayHeader == null) {

			final FontData fontData = CalendarProfileManager.getActiveCalendarProfile().dayDateFont;

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

	private Font getFont_TourContent() {

		if (_fontTourContent == null) {

			final FontData fontData = CalendarProfileManager.getActiveCalendarProfile().tourContentFont;

			_fontTourContent = new Font(_display, fontData);

			final GC gc = new GC(_display);
			{
				gc.setFont(_fontTourContent);
				_fontHeight_TourContent = gc.getFontMetrics().getHeight();
			}
			gc.dispose();
		}

		return _fontTourContent;
	}

	private Font getFont_TourTitle() {

		if (_fontTourTitle == null) {

			final FontData fontData = CalendarProfileManager.getActiveCalendarProfile().tourTitleFont;

			_fontTourTitle = new Font(_display, fontData);

			final GC gc = new GC(_display);
			{
				gc.setFont(_fontTourTitle);
				_fontHeight_TourTitle = gc.getFontMetrics().getHeight();
			}
			gc.dispose();
		}

		return _fontTourTitle;
	}

	private Font getFont_TourValue() {

		if (_fontTourValue == null) {

			final FontData fontData = CalendarProfileManager.getActiveCalendarProfile().tourValueFont;

			_fontTourValue = new Font(_display, fontData);

			final GC gc = new GC(_display);
			{
				gc.setFont(_fontTourValue);
				_fontHeight_TourValue = gc.getFontMetrics().getHeight();
			}
			gc.dispose();
		}

		return _fontTourValue;
	}

	private Font getFont_WeekValue() {

		if (_fontWeekValue == null) {

			final FontData fontData = CalendarProfileManager.getActiveCalendarProfile().weekValueFont;

			_fontWeekValue = new Font(_display, fontData);

			final GC gc = new GC(_display);
			{
				gc.setFont(_fontWeekValue);
				_fontHeight_WeekValue = gc.getFontMetrics().getHeight();
			}
			gc.dispose();
		}

		return _fontWeekValue;
	}

	private Font getFont_YearHeader() {

		if (_fontYearHeader == null) {

			final FontData fontData = CalendarProfileManager.getActiveCalendarProfile().yearHeaderFont;

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

	private DateTimeFormatter getUI_DayDateFormatter(	final CalendarProfile profile,
														final GC gc,
														final float cellWidth,
														final int dayLabelXOffset) {

		DateTimeFormatter headerFormatter;

		switch (profile.dayDateFormat) {
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

	private DataFormatter getValueFormatter(final FormatterID id, final DataFormatter[] allFormatter) {

		for (final DataFormatter formatter : allFormatter) {

			if (id == formatter.id) {
				return formatter;
			}
		}

		return CalendarProfileManager.DEFAULT_EMPTY_FORMATTER;
	}

	/**
	 * @param date
	 * @param isCenterDate
	 */
	void gotoDate(final LocalDate date, final boolean isCenterDate) {

		// set default time
		final LocalDateTime requestedDateTime = date.atStartOfDay();

		if (_currentProfile.isShowYearColumns) {

			if (_currentProfile.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

				if (isCenterDate) {

					_yearColumn_FirstYear = requestedDateTime

							// center date vertically on screen
							.minusWeeks(_numWeeksInOneColumn / 2);

				} else {

					_yearColumn_FirstYear = requestedDateTime;
				}

			} else {

				// scroll year column

				if (isCenterDate) {

					_yearColumn_FirstYear = requestedDateTime
							// center date horizontally on screen
							.minusYears(_numYearColumns / 2);

				} else {

					_yearColumn_FirstYear = requestedDateTime;
				}
			}

		} else {

			if (isCenterDate) {

				_firstVisibleDay = requestedDateTime

						// center date vertically on screen
						.minusWeeks(_numWeeksInOneColumn / 2);

			} else {

				_firstVisibleDay = requestedDateTime;
			}
		}

		updateUI();
	}

	void gotoDate_Today() {

		_firstVisibleDay = LocalDateTime.now().with(getFirstDayOfWeek_SameOrPrevious());

		_yearColumn_FirstYear = _firstVisibleDay;

		final Long tourId = _dataProvider.getTodaysTourId();
		if (tourId != null) {

			// select todays tour when available
			_selectedItem = new CalendarSelectItem(tourId, ItemType.TOUR);

		} else {

			// select day

			_selectedItem = new CalendarSelectItem(0, ItemType.DAY);
		}

		gotoDate(_firstVisibleDay.toLocalDate(), true);
	}

	private void gotoTour(int direction) {

		if (_allTourFocusItems.size() < 1) {

			// there are no tours -> scroll a week

			if (direction < 0) {
				scroll_ByDate_WithKeys(-1);
			} else {
				scroll_ByDate_WithKeys(1);
			}
			return;
		}

		/*
		 * If no tour is selected, count from first/last tour and select this tour
		 */
		if (_selectedItem.isTour() == false) {

			// a day is selected

			if (direction < 0) {

				// select first tour

				direction++;
				_selectedItem = new CalendarSelectItem(_allTourFocusItems.get(0).id, ItemType.TOUR);

			} else {

				// select last tour

				direction--;
				_selectedItem = new CalendarSelectItem(
						_allTourFocusItems.get(_allTourFocusItems.size() - 1).id,
						ItemType.TOUR);
			}
		}

		boolean visible = false;
		int index = 0;
		for (final FocusItem tourFocusItem : _allTourFocusItems) {

			if (_selectedItem.id == tourFocusItem.id) {

				// the selection is visible
				visible = true;
				break;
			}
			index++;
		}

		/*
		 * If we are scrolling tours forward and the selection got invisible start at the first tour
		 * again
		 */
		if (!visible) {
			if (direction > 0) {
				index = -1;
			}
		}

		final int newIndex = index + direction;
		if (newIndex < 0) {

			scroll_ByDate_WithKeys(-1);

		} else if (newIndex >= _allTourFocusItems.size()) {

			scroll_ByDate_WithKeys(1);

		} else {

			_selectedItem = new CalendarSelectItem(_allTourFocusItems.get(newIndex).id, ItemType.TOUR);

			_isGraphDirty = true;
			redraw();
		}
	}

	private void gotoTour_First() {

		final LocalDateTime firstTourDateTime = _dataProvider.getFirstTourDateTime();

		_firstVisibleDay = firstTourDateTime

				// set first day to start of week
				.with(getFirstDayOfWeek_SameOrPrevious());

		final Long tourId = _dataProvider.getFirstTourId();
		if (tourId != null) {

			// select first tour
			_selectedItem = new CalendarSelectItem(tourId, ItemType.TOUR);

		} else {

			// select day

			final long dayId = new DayItem(firstTourDateTime.toLocalDate()).dayId;
			_selectedItem = new CalendarSelectItem(dayId, ItemType.DAY);
		}

		gotoDate(_firstVisibleDay.toLocalDate(), true);
	}

	void gotoTour_Id(final long tourId) {

		final LocalDateTime dt = _dataProvider.getCalendarTourDateTime(tourId);

		_selectedItem = new CalendarSelectItem(tourId, ItemType.TOUR);

		if (dt.isBefore(_firstVisibleDay) || dt.isAfter(_firstVisibleDay.plusWeeks(_numWeeksInOneColumn))) {

			_isGraphDirty = true;
			gotoDate(dt.toLocalDate(), true);

		} else {
			redraw();
		}
	}

	private void gotoTour_SameWeekday(final int direction) {

		if (_allTourFocusItems.size() < 1) {

			scroll_ByDate_WithKeys(direction);

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

		scroll_ByDate_WithKeys(direction);
		redraw();
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

	private void onMouse_DoubleClick() {

		if (_selectedItem.isTour()) {

			// do double click action

			_tourDoubleClickState.canEditTour = true;
			_tourDoubleClickState.canOpenTour = true;
			_tourDoubleClickState.canQuickEditTour = true;
			_tourDoubleClickState.canEditMarker = true;
			_tourDoubleClickState.canAdjustAltitude = true;

			TourManager.getInstance().tourDoubleClickAction(CalendarGraph.this, _tourDoubleClickState);
		}
	}

	private void onMouse_Exit() {

		/*
		 * Remove hovered item that when entering, the same item is displayed again otherwise it
		 * will not work
		 */
		_hoveredItem = _emptyItem;

		redraw();
	}

	/**
	 * Mouse move/down event handler
	 * 
	 * @param event
	 */
	private void onMouse_MoveDown(final MouseEvent event) {

		if (_calendarImage == null || _calendarImage.isDisposed()) {
			return;
		}

		final CalendarSelectItem oldHoveredItem = _hoveredItem;
		final CalendarSelectItem oldSelectedItem = _selectedItem;

		if (1 == event.button || 3 == event.button) {

			// reset selection
			_selectedItem = _emptyItem;
		}

		// reset hover state
		_hoveredTour = null;

		boolean isTourHovered = false;
		boolean isDayHovered = false;

		// check if a tour is hovered
		for (final FocusItem tourFocusItem : _allTourFocusItems) {

			if (tourFocusItem.rect.contains(event.x, event.y)) {

				final long id = tourFocusItem.id;

				if (1 == event.button || 3 == event.button) {

					// set new selection

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

			// check if a day is hovered
			for (final FocusItem dayFocusItem : _allDayFocusItems) {

				if (dayFocusItem.rect.contains(event.x, event.y)) {

					final long dayId = dayFocusItem.id;

					if (1 == event.button || 3 == event.button) {

						_selectedItem = new CalendarSelectItem(dayId, ItemType.DAY);

					} else if (oldHoveredItem.id != dayId) { // a new object is highlighted

						_hoveredItem = new CalendarSelectItem(dayId, ItemType.DAY);
					}

					isDayHovered = true;

					break;
				}
			}
		}

		if (!oldSelectedItem.equals(_selectedItem)) {

			// selection is modified -> redraw calendar

			redraw();
			return;
		}

		if (!isDayHovered && !isTourHovered) {

			// only draw base calendar, remove highlighting when it was set

			redraw();
			return;
		}

		setHoveredModified(!oldHoveredItem.equals(_hoveredItem));
		if (_isHoveredModified) {

			// only draw the highlighting on top of the calendar image
			redraw();
		}

		return;
	}

	private void onMouse_Wheel(final Event event) {

		final int direction = event.count > 0 ? 1 : -1;

		if (UI.isCtrlKey(event)) {

			// zoom calendar

			zoom(event, direction);

		} else {

			// scroll calendar

			final Point mousePosition = new Point(event.x, event.y);
			boolean isInDayArea = false;

			// check if mouse is in the areas which contains the days
			for (final Rectangle dateRectangle : _calendarAllDaysRectangle) {
				if (dateRectangle.contains(mousePosition)) {
					isInDayArea = true;
					break;
				}
			}

			if (isInDayArea) {

				if (_selectedItem.isTour()) {

					// scroll tour
					scroll_Tour(direction * -1);

				} else {

					// scroll week
					scroll_WithWheel_Weeks(direction);
				}

			} else {

				// left or right column, scroll by pages

				scroll_WithWheel_Screen(direction);
			}
		}

		/**
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 * <p>
		 * This will fix scrollbar flickering because the scrollbar in this canvas is now disabled
		 * for automatically scrolling AND this is the reason why the MouseWheelListener is NOT used
		 * <p>
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */
		event.doit = false;
	}

	void refreshCalendar() {

		_dataProvider.invalidate();
		_isGraphDirty = true;

		redraw();
	}

	public void removeSelection() {

		if (!_selectedItem.equals(_emptyItem)) {

			_selectedItem = _emptyItem;
			_isGraphDirty = true;

			redraw();
		}
	}

	void scroll_ByDate_WithKeys(final int direction) {

		if (_currentProfile.isShowYearColumns) {

			_yearColumn_FirstYear = _currentProfile.yearColumnsStart == ColumnStart.CONTINUOUSLY

					// scroll continuously
					? _yearColumn_FirstYear.plusWeeks(direction)

					// scroll year column
					: _yearColumn_FirstYear.plusYears(direction);

		} else {

			_firstVisibleDay = _firstVisibleDay.plusWeeks(direction);
		}

		updateUI();
	}

	private void scroll_Tour(final int direction) {

		gotoTour(direction);
	}

	private void scroll_WithKey_Screen(final int direction) {

		// scroll half of the screen
		final int numPageWeeks = Math.max(1, _numWeeksInOneColumn / 2);
		final int scrollWeeks = direction > 0 ? numPageWeeks : -numPageWeeks;

		if (_currentProfile.isShowYearColumns) {

			if (_currentProfile.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

				_yearColumn_FirstYear = _yearColumn_FirstYear.plusWeeks(scrollWeeks);

			} else {

				// scroll year column

				_yearColumn_FirstYear = _yearColumn_FirstYear.plusYears(direction);
			}

		} else {

			_firstVisibleDay = _firstVisibleDay.plusWeeks(scrollWeeks);
		}

		updateUI();
	}

	void scroll_WithWheel_Screen(final int direction) {

		final boolean useDraggedScrolling = _currentProfile.useDraggedScrolling;

		// scroll half of the screen
		final int numPageWeeks = Math.max(1, _numWeeksInOneColumn / 2);
		final int scrollWeeks = direction > 0 ? numPageWeeks : -numPageWeeks;

		if (_currentProfile.isShowYearColumns) {

			if (_currentProfile.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

				_yearColumn_FirstYear = useDraggedScrolling
						? _yearColumn_FirstYear.plusWeeks(scrollWeeks)
						: _yearColumn_FirstYear.minusWeeks(scrollWeeks);

//				_yearColumn_FirstYear = yearColumn_WithAdjustedWeeks
//
//						// must start ALWAYS with 1st of month
//						.withDayOfMonth(1);

			} else {

				// scroll year column

				_yearColumn_FirstYear = useDraggedScrolling
						? _yearColumn_FirstYear.plusYears(direction)
						: _yearColumn_FirstYear.minusYears(direction);
			}

		} else {

			_firstVisibleDay = useDraggedScrolling
					? _firstVisibleDay.plusWeeks(scrollWeeks)
					: _firstVisibleDay.minusWeeks(scrollWeeks);
		}

		updateUI();
	}

	void scroll_WithWheel_Weeks(final int direction) {

		final boolean useDraggedScrolling = _currentProfile.useDraggedScrolling;

		if (_currentProfile.isShowYearColumns) {

			if (_currentProfile.yearColumnsStart == ColumnStart.CONTINUOUSLY) {

				_yearColumn_FirstYear = useDraggedScrolling
						? _yearColumn_FirstYear.plusWeeks(direction)
						: _yearColumn_FirstYear.minusWeeks(direction);

			} else {

				// scroll year column

				_yearColumn_FirstYear = useDraggedScrolling
						? _yearColumn_FirstYear.plusYears(direction)
						: _yearColumn_FirstYear.minusYears(direction);
			}

		} else {

			_firstVisibleDay = useDraggedScrolling
					? _firstVisibleDay.plusWeeks(direction)
					: _firstVisibleDay.minusWeeks(direction);
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

		final ScrollBar scrollbar = _parent.getVerticalBar();

		final int selectableMin = 0 + 1;
		final int selectableMax = scrollbar.getMaximum() - scrollbar.getThumb() - 1;

		final int currentSelection = scrollbar.getSelection();
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

		_scrollBar_LastSelection = scrollbar.getSelection();

		// goto the selected week
		_firstVisibleDay = scrollBar_getStartOfTours().atStartOfDay().plusDays(currentSelection * 7);

		_yearColumn_FirstYear = _firstVisibleDay;

		_isGraphDirty = true;
		redraw();

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] onScroll") //$NON-NLS-1$ //$NON-NLS-2$
//						+ ("\tselection: " + scrollbar.getSelection()) //$NON-NLS-1$
//						+ ("\toutsideWeeks: " + _scrollBar_OutsideWeeks) //$NON-NLS-1$
//						+ ("\tmax: " + scrollbar.getMaximum()) //$NON-NLS-1$
//						+ ("\tthumb: " + scrollbar.getThumb()) //$NON-NLS-1$
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

		final long firstViewportDay = _firstVisibleDay.toLocalDate().toEpochDay();
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
		final ScrollBar scrollbar = _parent.getVerticalBar();

		scrollbar.setThumb(thumbSize);
		scrollbar.setPageIncrement(thumbSize);

		scrollbar.setMinimum(0);
		scrollbar.setMaximum(scrollbarMax);

		scrollbar.setSelection(scrollbarSelection);

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

	void setFirstDay(final LocalDate dt) {

		_firstVisibleDay = dt//

				// set default time
				.atStartOfDay()

				// set first day to start of week
				.with(getFirstDayOfWeek_SameOrPrevious());

		_yearColumn_FirstYear = dt

				// set default time
				.atStartOfDay()

				// move after the first week, otherwise the previous year could be set !!!
				.plusWeeks(1)

				// 1.1.
				.withMonth(1)
				.withDayOfMonth(1);

	}

	private void setHoveredModified(final boolean isModified) {

		_isHoveredModified = isModified;

		if (isModified) {
			_isHoveredPainted = false;
		}
	}

	void setLinked(final boolean linked) {

		if (false == linked) {

			_selectedItem = _emptyItem;

			_isGraphDirty = true;
			redraw();
		}
	}

	public void setSelectionTourId(final long selectedTourId) {

		_selectedItem = new CalendarSelectItem(selectedTourId, ItemType.TOUR);
	}

	private void setupProfile() {

		_currentProfile = CalendarProfileManager.getActiveCalendarProfile();

		// setup calendar foreground and background color
		_calendarFgColor = _colorCache.getColor(_currentProfile.calendarForegroundRGB);
		_calendarBgColor = _colorCache.getColor(_currentProfile.calendarBackgroundRGB);
	}

	void setYearMonthContributor(final CalendarYearMonthContributionItem calendarYearMonthContribuor) {

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
		_rgbLine.add(_calendarFgColor.getRGB());
		_rgbText.add(_calendarFgColor.getRGB());

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

		if (this.isDisposed()) {
			return;
		}

		/*
		 * Do a redraw always, it occured when selecting another profile the UI is not updated
		 */
		redraw();

		if (_isGraphDirty) {
			// redraw is already forced
			return;
		}

		_isGraphDirty = true;

		// run async that the calendar is drawn first which sets some fields
		getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (CalendarGraph.this.isDisposed()) {
					return;
				}

				scrollBar_updateScrollbar();
			}
		});
	}

	void updateUI_AfterDataLoading() {

		if (_isGraphDirty) {
			// redraw is already forced
			return;
		}

		_isGraphDirty = true;

		// update UI
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (CalendarGraph.this.isDisposed()) {
					return;
				}

				// invalidate layout
				updateUI();
			}
		});

	}

	/**
	 * @param isResetFonts
	 *            When <code>true</code> then fonts will be reset and recreated by the next drawing.
	 */
	void updateUI_Layout(final boolean isResetFonts) {

		if (_isGraphDirty) {
			// redraw is already forced
			return;
		}

		if (isResetFonts) {
			disposeFonts();
		}

		// invalidate layout
		updateUI();
	}

	private void updateUI_YearMonthCombo() {

		/**
		 * Update month/year dropdown box.
		 * <p>
		 * Look at the 1st day of the week after the first day displayed because if we go to a
		 * specific month we ensure that the first day of the month is displayed in the first line,
		 * meaning the first day in calendar normally contains a day of the *previous* month
		 */
		_calendarYearMonthContributor.setDate(_calendarFirstDay.plusWeeks(1), _currentProfile);
	}

	private LocalDateTime yearColumn_getFirstDayOfMonth(final LocalDateTime currentFirstDay) {

		final LocalDateTime firstDayOfMonth = currentFirstDay.with(TemporalAdjusters.firstDayOfMonth());

		switch (_currentProfile.yearColumnsStart) {

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

	private void zoom(final Event event, final int direction) {

		boolean isShiftKey;

		if (UI.IS_OSX) {
			isShiftKey = (event.stateMask & SWT.MOD3) > 0;
		} else {
			isShiftKey = (event.stateMask & SWT.MOD2) > 0;
		}

		// accelerate  Shift key
		final int accelerator = isShiftKey ? 10 : 1;

		final int zoomValue = direction * accelerator;

		if (_currentProfile.isWeekRowHeight) {

			_currentProfile.weekHeight = Math.min(
					CalendarProfileManager.WEEK_HEIGHT_MAX,
					Math.max(CalendarProfileManager.WEEK_HEIGHT_MIN, _currentProfile.weekHeight + zoomValue));

		} else {

			_currentProfile.weekRows = Math.min(
					CalendarProfileManager.WEEK_ROWS_MAX,
					Math.max(CalendarProfileManager.WEEK_ROWS_MIN, _currentProfile.weekRows + zoomValue));
		}

		// update slideout
		final SlideoutCalendarOptions slideout = _calendarView.getConfigSlideout();
		slideout.restoreState_Profile();

		updateUI();
	}

	private void zoom_ToDefault() {

		final CalendarProfile dummyProfile = CalendarProfileManager.createProfileFromId(_currentProfile.appDefaultId);

		if (_currentProfile.isWeekRowHeight) {

			_currentProfile.weekHeight = dummyProfile.weekHeight;

		} else {

			_currentProfile.weekRows = dummyProfile.weekRows;
		}

		// update slideout
		final SlideoutCalendarOptions slideout = _calendarView.getConfigSlideout();
		slideout.restoreState_Profile();

		updateUI();
	}

}
