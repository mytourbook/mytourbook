/*
 *  File: DateChooserPanel.java 
 *  Copyright (c) 2004-2007  Peter Kliem (Peter.Kliem@jaret.de)
 *  A commercial license is available, see http://www.jaret.de.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package de.jaret.util.ui.datechooser;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import de.jaret.util.date.holidayenumerator.HolidayEnumerator;
import de.jaret.util.swt.SwtGraphicsHelper;

/**
 * The DateChooserPanel is intended to be used as a dropdown for the DateFieldCombo (@see
 * de.jaret.swt.util.datechooser.DateFieldCombo). However if it seems useful it is possible to be used as a standalone
 * control for selecting a date.
 * 
 * It features
 * <ul>
 * <li>selectable locale which is used for the determination of the first day of the week and the weekday/months
 * abbreviations</li>
 * <li>display of week of the year selectable</li>
 * <li>keyboard control (cursor keys navigate in the day panel, SHIFT-Cursor-left/right navigate month, ESC cancels, t
 * sets the date to the current date)</li>
 * <li>actions by the users can be watched by a
 * 
 * @see de.jaret.swt.util.datechooser.IDateChooserListener (intermdiate change, cancel, selection)</li>
 * <li>optional a
 * @see de.jaret.util.date.HolidayEnumerator can be set for highlighting holidays in the day panel</li>
 * </ul>
 * TODO selectable font
 * @author Peter Kliem
 * @version $Id: DateChooserPanel.java 576 2007-10-03 12:57:38Z olk $
 */
public class DateChooserPanel extends Composite implements MouseListener {
    /** currently selected date. */
    protected Date _date;

    /** date format symbols instance. */
    protected static DateFormatSymbols _dateFormatSymbols;

    /** calendar instance. */
    protected static Calendar _calendar;

    /** default color for marking selected date in the panel. */
    protected static final Color DEFAULTMARKERCOLOR = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);

    /** default color for drawing holidays. */
    protected static final Color DEFAULTHOLIDAYCOLOR = Display.getCurrent().getSystemColor(SWT.COLOR_RED);

    /** default color for drawing special days. */
    protected static final Color DEFAULTSPECIALDAYCOLOR = Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);

    /** actual color for the background marking. */
    protected Color _markerColor = DEFAULTMARKERCOLOR;
    /** actual color for the foreground of holidays. */
    protected Color _holidayColor = DEFAULTHOLIDAYCOLOR;
    /** actual color for the foreground of special days. */
    protected Color _specialDayColor = DEFAULTSPECIALDAYCOLOR;

    /**
     * true: a columnn showing the number of the week in the year should be displayed.
     */
    protected boolean _displayWeeks = false;

    /** true: a single click will select. */
    protected boolean _oneClickSelection;

    /** holiday enumerator. */
    protected HolidayEnumerator _holidayEnumerator;

    /** provider for additional day information. */
    protected IAdditionalDayInformationProvider _dayInformationProvider;

    /** label displaying the month. */
    protected Label _monthLabel;

    /** label displaying teh current date. */
    protected Label _todayLabel;

    /** increment month button. */
    protected Button _incMonthButton;

    /** decrement month button. */
    protected Button _decMonthButton;

    /** the daygrid canvas. */
    protected DayGrid _dayGrid;

    /** listener list. */
    protected List<IDateChooserListener> _listenerList;

    /** locale for the panel. */
    protected Locale _locale;

    /** font for bold weekday headings. */
    private Font _weekdayFont;
    /** font for the smaller weeknumbers. */
    private Font _weekNumberFont;

    /**
     * Constructor for the DateChooserPanel.
     * 
     * @param parent Composite parent
     * @param style style bits
     * @param oneClickSelection if true, a single mouse click will be considered a valid selection. Otherwise a
     * selection will need a double click.
     * @param displayWeeks if true the panel will display a week of the year column
     * @param locale Locale to be used
     */
    public DateChooserPanel(Composite parent, int style, boolean oneClickSelection, boolean displayWeeks, Locale locale) {
        super(parent, style);
        _oneClickSelection = oneClickSelection;
        _displayWeeks = displayWeeks;
        _locale = locale;

        // helpers needed to be initialized with the correct locale
        _dateFormatSymbols = new DateFormatSymbols(_locale);
        _calendar = new GregorianCalendar(_locale);

        // create the controls for the chooser panel
        createControls();

        // to avoid a null date set the current date
        setDate(new Date());

        // key listener for keyboard control
        addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent event) {
                handleKeyPressed(event);
            }

            public void keyReleased(KeyEvent arg0) {
            }
        });

        // mousewheel
        Listener listener = new Listener() {
            public void handleEvent(Event event) {
                switch (event.type) {
                case SWT.MouseWheel:
                    int count = -event.count / 3;
                    if ((event.stateMask & SWT.SHIFT) != 0) {
                        rollMonth(count);
                    } else {
                        rollDay(count);
                    }
                    break;
                default:
                    throw new RuntimeException("unsupported event");

                }
            }
        };

        addListener(SWT.MouseWheel, listener);
    }

    /**
     * Constructor for the DateChooserPanel. OneClickselection defaults to <code>false</code>, displayWeeks defaults
     * to <code>true</code>. The Locale used is the default locale.
     * 
     * @param parent Composite parent
     * @param style style bits
     */
    public DateChooserPanel(Composite parent, int style) {
        this(parent, style, false, true, Locale.getDefault());
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        super.dispose();
        if (_weekdayFont != null) {
            _weekdayFont.dispose();
        }
        if (_weekNumberFont != null) {
            _weekNumberFont.dispose();
        }
    }

    /**
     * {@inheritDoc} Propagate the change to the elements.
     */
    public void setBackground(Color color) {
        super.setBackground(color);
        _monthLabel.setBackground(color);
        _todayLabel.setBackground(color);
        _dayGrid.setBackground(color);
    }

    /**
     * Handles the key events of the panel.
     * 
     * @param event KeyEvent to be processed
     */
    private void handleKeyPressed(KeyEvent event) {
        if ((event.stateMask & SWT.SHIFT) != 0) {
            switch (event.keyCode) {
            case SWT.ARROW_RIGHT:
                incMonth();
                break;
            case SWT.ARROW_LEFT:
                decMonth();
                break;

            default:
                // do nothing
                break;
            }
        } else {
            switch (event.keyCode) {
            case SWT.ESC:
                fireChoosingCanceled();
                break;
            case SWT.CR:
                fireDateChosen(getDate());
                break;
            case SWT.ARROW_RIGHT:
                incDay();
                break;
            case SWT.ARROW_LEFT:
                decDay();
                break;
            case SWT.ARROW_DOWN:
                incWeek();
                break;
            case SWT.ARROW_UP:
                decWeek();
                break;
            case 't':
            case 'T':
                today();
                break;

            default:
                // do nothing
                break;
            }

        }
    }

    /**
     * Increase month.
     */
    private void incMonth() {
        _calendar.add(Calendar.MONTH, 1);
        intermediateChange();
    }

    /**
     * Decrease month.
     * 
     */
    private void decMonth() {
        _calendar.add(Calendar.MONTH, -1);
        intermediateChange();
    }

    /**
     * Add or subtract a number of month.
     * 
     * @param count number of month to add/substract
     */
    private void rollMonth(int count) {
        _calendar.add(Calendar.MONTH, count);
        intermediateChange();
    }

    /**
     * Increase day.
     * 
     */
    private void incDay() {
        _calendar.add(Calendar.DAY_OF_MONTH, 1);
        intermediateChange();
    }

    /**
     * Decrease day.
     * 
     */
    private void decDay() {
        _calendar.add(Calendar.DAY_OF_MONTH, -1);
        intermediateChange();
    }

    /**
     * Add or substract a number of days.
     * 
     * @param count number of days
     */
    private void rollDay(int count) {
        _calendar.add(Calendar.DAY_OF_MONTH, count);
        intermediateChange();
    }

    /**
     * Iincrease week.
     * 
     */
    private void incWeek() {
        _calendar.add(Calendar.DAY_OF_MONTH, 7);
        intermediateChange();
    }

    /**
     * Decrease week.
     * 
     */
    private void decWeek() {
        _calendar.add(Calendar.DAY_OF_MONTH, -7);
        intermediateChange();
    }

    /**
     * Set the selected date to today.
     * 
     */
    private void today() {
        setDate(new Date());
        fireIntermediateChange(getDate());
    }

    /**
     * intermediate change: update monthlabel etc.
     */
    private void intermediateChange() {
        updateMonthLabel();
        redraw();
        fireIntermediateChange(getDate());
        forceFocus(); // return the focus to the chooser panel (key listening)
    }

    /**
     * Set the currently selected date. A value of <code>null</code> will be transformed to the current date.
     * 
     * @param date Date to be displayed
     */
    public void setDate(Date date) {
        // null is not really a displayable date .. use a current date instead
        if (date == null) {
            date = new Date();
        }
        if (!date.equals(_date)) {
            _date = date;
            _calendar.setTime(_date);
            updateMonthLabel();
            redraw();
        }
    }

    /**
     * Get the selected Date.
     * 
     * @return the currently selected date.
     */
    public Date getDate() {
        _date = _calendar.getTime();
        return _date;
    }

    /**
     * {@inheritDoc} Also redraws the grid.
     */
    public void redraw() {
        super.redraw();
        _dayGrid.redraw();
    }

    /**
     * Create the controls that compose the datechooserpanel.
     */
    private void createControls() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        setLayout(gridLayout);

        // month dec/inc and label
        _decMonthButton = new Button(this, SWT.ARROW | SWT.LEFT);
        GridData gd = new GridData();
        _decMonthButton.setLayoutData(gd);
        _decMonthButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent arg0) {
                decMonth();
            }

        });

        _monthLabel = new Label(this, SWT.CENTER);
        _monthLabel.setText(getMonthText());
        gd = new GridData(GridData.FILL_HORIZONTAL);
        _monthLabel.setLayoutData(gd);
        _monthLabel.setBackground(getBackground());

        _incMonthButton = new Button(this, SWT.ARROW | SWT.RIGHT);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        _incMonthButton.setLayoutData(gd);
        _incMonthButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent arg0) {
                incMonth();
            }
        });

        // main day grid
        _dayGrid = new DayGrid(this, SWT.NULL);
        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 3;
        _dayGrid.setLayoutData(gd);
        _dayGrid.addMouseListener(this);
        _dayGrid.setBackground(getBackground());

        // today label
        _todayLabel = new Label(this, SWT.NULL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        _todayLabel.setLayoutData(gd);
        _todayLabel.setBackground(getBackground());

        // init he text of the today label with the current locale
        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, _locale);
        _todayLabel.setText(df.format(new Date()));

        _todayLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                today();
            }
        });

    }

    /**
     * Upate the month label.
     * 
     */
    private void updateMonthLabel() {
        _monthLabel.setText(getMonthText());
    }

    /**
     * Assemble text for the month label (month + year).
     * 
     * @return text for the label
     */
    private String getMonthText() {
        int month = _calendar.get(Calendar.MONTH);
        int year = _calendar.get(Calendar.YEAR);
        String monthShort = _dateFormatSymbols.getShortMonths()[month];
        return monthShort + " " + year;
    }

    /**
     * The DayGrid is a private member class extending Canvas. It draws the grid of days and the headings. Whenever the
     * date in the surrounding DateChooserPanel is changed the method <code>updateInternals</code> has to be called to
     * update all calculated fields.
     * 
     * @author Peter Kliem
     * @version $Id: DateChooserPanel.java 576 2007-10-03 12:57:38Z olk $
     */
    class DayGrid extends Canvas {

        /**
         * Construct a day grid.
         * 
         * @param parent parent composite
         * @param style style bits
         */
        public DayGrid(Composite parent, int style) {
            super(parent, style);
            addPaintListener(new PaintListener() {
                public void paintControl(PaintEvent event) {
                    onPaint(event);
                }

            });
            addListener(SWT.Traverse, new Listener() {
                public void handleEvent(Event event) {
                    handleTraverse(event);
                }
            });
            addMouseTrackListener(new MouseTrackAdapter() {
                public void mouseHover(MouseEvent event) {
                    // If the mouse hovers, determine if the date marks a
                    // special day
                    // and set the appropriate tooltip. Otherwise clear the
                    // tooltip.
                    Date date = dateForLocation(event.x, event.y);
                    if (date == null) {
                        setToolTipText(null);
                    } else {
                        setToolTipText(getTooltipText(date));
                    }

                }

            });
        }

        /**
         * Create the tooltip text for a date.
         * 
         * @param date date
         * @return the tooltip text or <code>null</code>
         */
        protected String getTooltipText(Date date) {
            if (date == null) {
                return null;
            }
            if (_dayInformationProvider != null) {
                String text = _dayInformationProvider.getToolTipText(date);
                if (text != null) {
                    return text;
                }
            }
            if (_holidayEnumerator != null) {
                String text = _holidayEnumerator.getDayName(date);
                if (text != null) {
                    return text;
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public Point computeSize(int arg0, int arg1, boolean arg2) {
            // TODO calculate size
            return new Point(200, 150);
        }

        void handleTraverse(Event event) {
            /*
             * if (isSingleLine() && event.detail == SWT.TRAVERSE_TAB_NEXT) { event.doit = true; }
             */}

        int width;

        int height;

        int columnWidth;

        int rowHeight;

        int posOfFirstInMonth;

        int daysInMonth;

        int day;

        int month;

        int weekColumnWidth;

        /**
         * Do any calculations necessary to support drawing.
         */
        public void updateInternals() {
            // width and heigth of the panel
            width = getClientArea().width;
            height = getClientArea().height;

            /**
             * Calculation of the outline of the month and the day to display. Some of these are only necessary if the
             * month has changed. This optimization is an open todo. TODO move some calculations
             */
            daysInMonth = _calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            day = _calendar.get(Calendar.DAY_OF_MONTH);
            month = _calendar.get(Calendar.MONTH);
            // int weekday = _calendar.get(Calendar.DAY_OF_WEEK);
            // int dayPos = getWeekdayPos(weekday);

            Calendar tmpCalendar = (Calendar) _calendar.clone();
            tmpCalendar.set(Calendar.DAY_OF_MONTH, 1);
            posOfFirstInMonth = getWeekdayPos(tmpCalendar.get(Calendar.DAY_OF_WEEK));

            // calculate the column width and row height
            weekColumnWidth = _displayWeeks ? width / 8 : 0;
            columnWidth = (width - weekColumnWidth) / 7;
            rowHeight = height / 7;

        }

        /**
         * Retrieve the font to use for the weekday labels.
         * 
         * @param gc GC
         * @return the font
         */
        protected Font getWeekdayFont(GC gc) {
            if (_weekdayFont == null) {
                _weekdayFont = new Font(Display.getCurrent(), gc.getFont().getFontData()[0].getName(), gc.getFont()
                        .getFontData()[0].getHeight(), SWT.BOLD);
            }
            return _weekdayFont;
        }

        /**
         * Retrieve the font to use for the week number labels.
         * 
         * @param gc GC
         * @return the font
         */
        protected Font getWeekNumberFont(GC gc) {
            if (_weekNumberFont == null) {
                _weekNumberFont = new Font(Display.getCurrent(), gc.getFont().getFontData()[0].getName(), (int) (gc
                        .getFont().getFontData()[0].getHeight() * 0.9), SWT.NORMAL);
            }
            return _weekNumberFont;
        }

        /**
         * The paint method. TODO clipping rect optimizations
         * 
         * @param event the pain tevent
         */
        private void onPaint(PaintEvent event) {
            // do preparing calculations
            updateInternals();

            // weekdays are coded from 1 to 7 beginning with sunday
            GC gc = event.gc;
            Font oldFont = gc.getFont();
            gc.setFont(getWeekdayFont(gc));

            for (int weekday = 1; weekday < 8; weekday++) {
                int pos = getWeekdayPos(weekday);
                boolean isWeekend = false;
                if (weekday == Calendar.SATURDAY || weekday == Calendar.SUNDAY) {
                    isWeekend = true;
                }
                Color color = isWeekend ? Display.getCurrent().getSystemColor(SWT.COLOR_RED) : Display.getCurrent()
                        .getSystemColor(SWT.COLOR_BLACK);
                drawCell(gc, pos, 0, getWeekdayString(weekday), color, false);
            }
            gc.setFont(oldFont);

            int[] weeks = new int[6];
            Calendar tmpCalendar = new GregorianCalendar();
            tmpCalendar.setTime(_date);
            tmpCalendar.set(Calendar.DAY_OF_MONTH, 1);
            tmpCalendar.add(Calendar.DAY_OF_MONTH, -posOfFirstInMonth);
            for (int dy = 0; dy < 6; dy++) {
                weeks[dy] = tmpCalendar.get(Calendar.WEEK_OF_YEAR);
                for (int dx = 0; dx < 7; dx++) {
                    int paintDay = tmpCalendar.get(Calendar.DAY_OF_MONTH);
                    int paintMonth = tmpCalendar.get(Calendar.MONTH);
                    // determine the foreground color of the cell
                    Color color = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
                    if (paintMonth != month) {
                        color = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
                    } else if (_holidayEnumerator != null) {
                        if (_holidayEnumerator.isHoliday(tmpCalendar.getTime())) {
                            color = _holidayColor;
                        } else if (_holidayEnumerator.isSpecialDay(tmpCalendar.getTime())) {
                            color = _specialDayColor;
                        }
                    }
                    Font normalFont = gc.getFont();
                    // handle bold painting if told so by an additional information provider
                    if (_dayInformationProvider != null && _dayInformationProvider.renderBold(tmpCalendar.getTime())) {
                        // bold font
                        gc.setFont(getWeekdayFont(gc));
                    } else {
                        gc.setFont(normalFont);
                    }
                    drawCell(gc, dx, dy + 1, Integer.toString(paintDay), color, paintDay == day && paintMonth == month);
                    tmpCalendar.add(Calendar.DAY_OF_MONTH, 1);
                    gc.setFont(normalFont);
                }
            }
            oldFont = gc.getFont();
            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
            gc.setFont(getWeekNumberFont(gc));
            for (int dy = 0; dy < 6; dy++) {
                int y = (dy + 1) * rowHeight;
                int rx = columnWidth - (int) (columnWidth * 0.2);
                SwtGraphicsHelper.drawStringRightAlignedVCenter(gc, Integer.toString(weeks[dy]), rx, y + rowHeight / 2);
            }
            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));

            gc.setFont(oldFont);

        }

        /**
         * Draw a cell on the panel.
         * 
         * @param gc GC
         * @param pos x
         * @param row y
         * @param string String to paint
         * @param foreground foreground color
         * @param mark is marked
         */
        private void drawCell(GC gc, int pos, int row, String string, Color foreground, boolean mark) {
            int x = pos * columnWidth + weekColumnWidth;
            int y = row * rowHeight;
            Color oldFG = gc.getForeground();
            Color oldBG = gc.getBackground();
            gc.setForeground(foreground);
            if (mark) {
                gc.setBackground(_markerColor);
                gc.fillRectangle(x, y, columnWidth - 1, rowHeight - 1);
            }
            SwtGraphicsHelper.drawStringCenteredVCenter(gc, string, x, x + columnWidth, y + rowHeight / 2);
            gc.setForeground(oldFG);
            gc.setBackground(oldBG);
        }

        /**
         * Calculate the day at position x,y.
         * 
         * @param x x coordinate
         * @param y ycoordinate
         * @return the day (first = 1) or -1 if no valid day could be determined
         */
        protected int dayForLocation(int x, int y) {
            int row = y / rowHeight - 1;
            int col = (x - weekColumnWidth) / columnWidth;

            int day = row * 7 + col + 1;
            day -= posOfFirstInMonth;
            if (day < 1 || day > _calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                day = -1;
            }
            return day;
        }

        /**
         * Calculate the date for a given location.
         * 
         * @param x x coordinate
         * @param y y coordinate
         * @return the date for the location
         */
        protected Date dateForLocation(int x, int y) {
            int row = y / rowHeight - 1;
            int col = (x - weekColumnWidth) / columnWidth;

            int day = row * 7 + col + 1;
            day -= posOfFirstInMonth;
            Calendar tmpCalendar = new GregorianCalendar();
            tmpCalendar.setTime(getDate());
            tmpCalendar.set(Calendar.DAY_OF_MONTH, 1);
            tmpCalendar.add(Calendar.DAY_OF_MONTH, day - 1);
            return tmpCalendar.getTime();
        }

        /**
         * Retrieve short weekday representation.
         * 
         * @param weekday coded weekday
         * @return short string representaion
         */
        private String getWeekdayString(int weekday) {
            return _dateFormatSymbols.getShortWeekdays()[weekday];
        }

        /**
         * @param weekday coded weekday (1-7)
         * @return column position in the day grid (0-6)
         */
        private int getWeekdayPos(int weekday) {
            int firstDay = _calendar.getFirstDayOfWeek();
            int pos = weekday - firstDay;
            if (pos < 0) {
                pos += 7;
            }
            return pos;
        }

    }

    /**
     * {@inheritDoc}
     */
    public void mouseDoubleClick(MouseEvent event) {
        boolean success = setDateByLocation(event.x, event.y);
        if (success) {
            fireDateChosen(getDate());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseDown(MouseEvent event) {
        boolean success = setDateByLocation(event.x, event.y);
        if (_oneClickSelection && success) {
            fireDateChosen(getDate());
        } else {
            fireIntermediateChange(getDate());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseUp(MouseEvent event) {
        // nothing to do
    }

    /**
     * Sets the calendar day for a given location. This will succeed if a valid day (i.e. a day in the current month is
     * selected).
     * 
     * @param x x coordinate
     * @param y y coordinate
     * @return true if a valid day could be selected
     */
    protected boolean setDateByLocation(int x, int y) {
        int day = _dayGrid.dayForLocation(x, y);
        if (day > 0) {
            _calendar.set(Calendar.DAY_OF_MONTH, day);
            _dayGrid.redraw();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add a DateChooserListener to be informed about changes.
     * 
     * @param listener the DateChooserListener to be added
     */
    public synchronized void addDateChooserListener(IDateChooserListener listener) {
        if (_listenerList == null) {
            _listenerList = new Vector<IDateChooserListener>();
        }
        _listenerList.add(listener);
    }

    /**
     * Remove a DateChooserListener.
     * 
     * @param listener the DateChooserListener to be removed
     */
    public synchronized void remDateChooserListener(IDateChooserListener listener) {
        if (_listenerList == null) {
            return;
        }
        _listenerList.remove(listener);
    }

    /**
     * Inform listeners about a chosing action.
     * 
     * @param date date chosen
     */
    protected void fireDateChosen(Date date) {
        if (_listenerList != null) {
            for (IDateChooserListener listener : _listenerList) {
                listener.dateChosen(date);
            }
        }
    }

    /**
     * Inform listeners about an intermediate date change.
     * 
     * @param date date currently selected
     */
    protected void fireIntermediateChange(Date date) {
        if (_listenerList != null) {
            for (IDateChooserListener listener : _listenerList) {
                listener.dateIntermediateChange(date);
            }
        }
    }

    /**
     * Inform listeners about cancellation f the choosing.
     */
    protected void fireChoosingCanceled() {
        if (_listenerList != null) {
            for (IDateChooserListener listener : _listenerList) {
                listener.choosingCanceled();
            }
        }
    }

    /**
     * Retrieve the HolidayEnumerator.
     * 
     * @return Returns the HolidayEnumerator.
     */
    public HolidayEnumerator getHolidayEnumerator() {
        return _holidayEnumerator;
    }

    /**
     * Set the HolidayEnumerator. A value of <code>null</code> is valid meaning no HolidayEnumerator should be used.
     * 
     * @param holidayEnumerator The HolidayEnumerator to set.
     */
    public void setHolidayEnumerator(HolidayEnumerator holidayEnumerator) {
        _holidayEnumerator = holidayEnumerator;
        redraw();
    }

    /**
     * Retrieve the additional information provider.
     * 
     * @return the information provider
     */
    public IAdditionalDayInformationProvider getAdditionalDayInformationProvider() {
        return _dayInformationProvider;
    }

    /**
     * Set an additional provider for day information.
     * 
     * @param informationProvider the information provider
     */
    public void setAdditionalDayInformationProvider(IAdditionalDayInformationProvider informationProvider) {
        _dayInformationProvider = informationProvider;
        redraw();
    }

    /**
     * @return true if weeks should be displayed
     */
    public boolean isDisplayWeeks() {
        return _displayWeeks;
    }

    /**
     * Set whether a column showing the week of the year should be displayed.
     * 
     * @param displayWeeks if set to true a column with the number of the week in the year is displayed.
     */
    public void setDisplayWeeks(boolean displayWeeks) {
        _displayWeeks = displayWeeks;
        redraw();
    }

    /**
     * @return true if a single click will select the date
     */
    public boolean isOneClickSelection() {
        return _oneClickSelection;
    }

    /**
     * Set whether a single or a double click will select the date.
     * 
     * @param oneClickSelection if set to true one click will select the date. If set to false the date selection
     * requires a double click.
     */
    public void setOneClickSelection(boolean oneClickSelection) {
        _oneClickSelection = oneClickSelection;
    }

    /**
     * Retrieve the color used for painting the background of the marked day.
     * 
     * @return the marker color
     */
    public Color getMarkerColor() {
        return _markerColor;
    }

    /**
     * Set the color to paint the background of the marked day.
     * 
     * @param markerColor color for the marker
     */
    public void setMarkerColor(Color markerColor) {
        _markerColor = markerColor;
    }

    /**
     * Retrieve the color used for painting the foreground of a holiday.
     * 
     * @return the holiday color
     */
    public Color getHolidayColor() {
        return _holidayColor;
    }

    /**
     * Set the color to paint the foreground of holidays.
     * 
     * @param holidayColor holiday color
     */
    public void setHolidayColor(Color holidayColor) {
        _holidayColor = holidayColor;
    }

    /**
     * Retrieve the color used for painting the foreground of a special day.
     * 
     * @return the special day color
     */
    public Color getSpecialDayColor() {
        return _specialDayColor;
    }

    /**
     * Set the color to paint the foreground of special days.
     * 
     * @param specialDayColor color for special days
     */
    public void setSpecialDayColor(Color specialDayColor) {
        _specialDayColor = specialDayColor;
    }
}
