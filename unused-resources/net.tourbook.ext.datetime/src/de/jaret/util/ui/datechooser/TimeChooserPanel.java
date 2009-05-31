/*
 *  File: TimeChooserPanel.java 
 *  Copyright (c) 2004-2007  Peter Kliem (Peter.Kliem@jaret.de)
 *  A commercial license is available, see http://www.jaret.de.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package de.jaret.util.ui.datechooser;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.jaret.util.date.JaretDate;
import de.jaret.util.swt.SwtGraphicsHelper;

/**
 * The TimeChooserPanel is intended to be used as a dropdown for the TimeFieldCombo (@see
 * de.jaret.swt.util.datechooser.TimeChooser). However if it seems useful it is possible to be used as a standalone
 * control for selecting a time.
 * <p>
 * Time is represented as the time part of a java.util.Date.
 * </p>
 * 
 * @author Peter Kliem
 * @version $Id: TimeChooserPanel.java 576 2007-10-03 12:57:38Z olk $
 */
public class TimeChooserPanel extends Composite implements MouseListener {
    /** currently selected time in this date. */
    protected Date _date;

    /** color for marking selected time in the panel. */
    protected static final Color MARKERCOLOR = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);

    /** the time grid canvas. */
    protected TimeGrid _timeGrid;

    /** listener list. */
    protected List<IDateChooserListener> _listenerList;

    /** currennt col width in the chooser. */
    protected int _columnWidth;
    /** current row heigth in the chooser. */
    protected int _rowHeight;

    /**
     * Constructor for the TimeChooserPanel.
     * 
     * @param parent Composite parent
     * @param style style bits selection will need a double click.
     */
    public TimeChooserPanel(Composite parent, int style) {
        super(parent, style);

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
                    int colIdx = event.x / _columnWidth;
                    switch (colIdx) {
                    case 0:
                        rollAMPM(count);
                        break;
                    case 1:
                        rollHours(count);
                        break;
                    case 2:
                        rollMinutes(count);
                        break;

                    default:
                        break;
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
     * {@inheritDoc}
     */
    public void dispose() {
        super.dispose();
    }

    /**
     * {@inheritDoc} Propagate the change to the elements.
     */
    public void setBackground(Color color) {
        super.setBackground(color);
        _timeGrid.setBackground(color);
    }

    /**
     * Handles the key events of the panel.
     * 
     * @param event KeyEvent to be processed
     */
    private void handleKeyPressed(KeyEvent event) {
        if ((event.stateMask & SWT.SHIFT) != 0) {
            switch (event.keyCode) {
            case SWT.ARROW_DOWN:
                rollMinutes(1);
                break;
            case SWT.ARROW_UP:
                rollMinutes(-1);
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
            case SWT.ARROW_DOWN:
                rollHours(1);
                break;
            case SWT.ARROW_UP:
                rollHours(-1);
                break;
            default:
                // do nothing
                break;
            }

        }
    }

    /**
     * intermediate change: update monthlabel etc.
     */
    private void intermediateChange() {
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
            redraw();
        }
    }

    /**
     * Get the selected Date.
     * 
     * @return the currently selected date.
     */
    public Date getDate() {
        return _date;
    }

    /**
     * {@inheritDoc} Also redraws the grid.
     */
    public void redraw() {
        super.redraw();
        _timeGrid.redraw();
    }

    /**
     * Create the controls that compose the timechooserpanel.
     */
    private void createControls() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        setLayout(gridLayout);

        // main time grid
        _timeGrid = new TimeGrid(this, SWT.NULL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        _timeGrid.setLayoutData(gd);
        _timeGrid.addMouseListener(this);
        _timeGrid.setBackground(getBackground());
    }

    /**
     * The TimeGrid is a private member class extending Canvas.
     * 
     * @author Peter Kliem
     * @version $Id: TimeChooserPanel.java 576 2007-10-03 12:57:38Z olk $
     */
    class TimeGrid extends Canvas {

        /**
         * Construct a time grid.
         * 
         * @param parent parent composite
         * @param style style bits
         */
        public TimeGrid(Composite parent, int style) {
            super(parent, style);
            addPaintListener(new PaintListener() {
                public void paintControl(PaintEvent event) {
                    onPaint(event);
                }

            });
        }

        protected Point _sizeCache;

        /**
         * {@inheritDoc}
         */
        public Point computeSize(int arg0, int arg1, boolean arg2) {
            if (_sizeCache == null) {
                GC gc = new GC(this);
                Point extent = gc.stringExtent(">=12");
                gc.dispose();
                _sizeCache = new Point(extent.x * 3, extent.y * 12);
            }

            return _sizeCache;
        }

        /**
         * Do any calculations necessary to support drawing.
         */
        public void updateInternals() {
            _columnWidth = getClientArea().width / 3;
            _rowHeight = getClientArea().height / 12;
        }

        /**
         * The paint method.
         * 
         * @param event the pain tevent
         */
        private void onPaint(PaintEvent event) {
            // do preparing calculations
            updateInternals();

            GC gc = event.gc;

            drawMarks(gc);

            for (int i = 0; i < 12; i++) {
                int y = i * _rowHeight;
                drawAMPM(gc, y, i);
                drawHour(gc, y, i);
                drawMinute(gc, y, i);
            }
            // int x = 0;
            // gc.drawLine(x, 0, x, getClientArea().height);
            // x += _columnWidth;
            // gc.drawLine(x, 0, x, getClientArea().height);
            // x += _columnWidth;
            // gc.drawLine(x, 0, x, getClientArea().height);
            // x += _columnWidth;
            // gc.drawLine(x, 0, x, getClientArea().height);
            // x += _columnWidth;
        }

        private void drawMarks(GC gc) {
            Color bg = gc.getBackground();
            gc.setBackground(MARKERCOLOR);
            JaretDate date = new JaretDate(_date);
            // 0/12
            int y = date.getHours() < 12 ? 0 : _rowHeight;
            Rectangle mark = new Rectangle(0, y, _columnWidth, _rowHeight);
            gc.fillRectangle(mark);
            // hour
            int hour = date.getHours();
            hour -= (hour >= 12 ? 12 : 0);
            y = hour * _rowHeight;
            mark = new Rectangle(_columnWidth, y, _columnWidth, _rowHeight);
            gc.fillRectangle(mark);
            // minutes
            y = date.getMinutes() * getClientArea().height / 60;
            mark = new Rectangle(2 * _columnWidth, y, 2 * _columnWidth, _rowHeight);
            gc.fillRectangle(mark);

            gc.setBackground(bg);
        }

        private void drawAMPM(GC gc, int y, int i) {
            if (i < 2) {
                int x = 0;
                String hour = i == 0 ? "<12" : ">=12";
                SwtGraphicsHelper.drawStringRightAlignedVTop(gc, hour, x + _columnWidth, y);
            }
        }

        private void drawMinute(GC gc, int y, int i) {
            int x = 2 * _columnWidth;
            String minute = i * 5 + "";
            SwtGraphicsHelper.drawStringRightAlignedVTop(gc, minute, x + _columnWidth, y);
        }

        private void drawHour(GC gc, int y, int i) {
            JaretDate date = new JaretDate(_date);
            int x = _columnWidth;
            String hour = date.getHours() < 12 ? (i + "") : ((i + 12) + "");
            SwtGraphicsHelper.drawStringRightAlignedVTop(gc, hour, x + _columnWidth, y);
        }

    }

    /**
     * {@inheritDoc}
     */
    public void mouseDoubleClick(MouseEvent event) {
        boolean success = setTimeByLocation(event.x, event.y);
        if (success) {
            fireDateChosen(getDate());
        }
    }

    /**
     * Set the time of the date by a clicked location.
     * 
     * @param x x
     * @param y y
     * @return true if the click was successful in selecting a new time
     */
    private boolean setTimeByLocation(int x, int y) {
        int colIdx = x / _columnWidth;
        int rowIdx = y / _rowHeight;
        boolean success = false;
        JaretDate d = new JaretDate(_date);

        switch (colIdx) {
        case 0:
            if (rowIdx == 0) {
                success = true;
                if (d.getHours() >= 12) {
                    d.setHours(d.getHours() - 12);
                }
            } else if (rowIdx == 1) {
                success = true;
                if (d.getHours() < 12) {
                    d.setHours(d.getHours() + 12);
                }
            }
            break;
        case 1:
            int h = d.getHours() < 12 ? rowIdx : rowIdx + 12;
            d.setHours(h);
            success = true;
            break;
        case 2:
            int min = rowIdx * 5;
            d.setMinutes(min);
            success = true;
            break;
        default:
            success = false;
            break;
        }

        if (success) {
            redraw();
            _date = d.getDate();
        }

        return success;
    }

    /**
     * Roll the minutes by count * 5 Minutes.
     * 
     * @param count units to roll
     */
    private void rollMinutes(int count) {
        JaretDate d = new JaretDate(_date);
        if (count < 0 && d.getMinutes() > 0) {
            int m = d.getMinutes() + count * 5;
            m = m < 0 ? 0 : m;
            d.setMinutes(m);
            _date = d.getDate();
            fireIntermediateChange(_date);
            redraw();
        } else if (count > 0 && d.getMinutes() < 59) {
            int m = d.getMinutes() + count * 5;
            m = m > 55 ? 55 : m;
            d.setMinutes(m);
            _date = d.getDate();
            fireIntermediateChange(_date);
            redraw();
        }

    }

    /**
     * Roll the hour field by count units. Will flip over the am/pm section if necessary.
     * 
     * @param count units to roll
     */
    private void rollHours(int count) {
        JaretDate d = new JaretDate(_date);
        if (count < 0 && d.getHours() > 0) {
            int h = d.getHours() + count;
            h = h < 0 ? 0 : h;
            d.setHours(h);
            _date = d.getDate();
            fireIntermediateChange(_date);
            redraw();
        } else if (count > 0 && d.getHours() < 23) {
            int h = d.getHours() + count;
            h = h > 23 ? 23 : h;
            d.setHours(h);
            _date = d.getDate();
            fireIntermediateChange(_date);
            redraw();
        }
    }

    /**
     * Roll the am/pm section by count units.
     * 
     * @param count units to roll
     */
    private void rollAMPM(int count) {
        JaretDate d = new JaretDate(_date);
        if (count < 0 && d.getHours() >= 12) {
            d.setHours(d.getHours() - 12);
            _date = d.getDate();
            fireIntermediateChange(_date);
            redraw();
        } else if (count > 0 && d.getHours() < 12) {
            d.setHours(d.getHours() + 12);
            _date = d.getDate();
            fireIntermediateChange(_date);
            redraw();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseDown(MouseEvent event) {
        boolean success = setTimeByLocation(event.x, event.y);
        if (success) {
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

}
