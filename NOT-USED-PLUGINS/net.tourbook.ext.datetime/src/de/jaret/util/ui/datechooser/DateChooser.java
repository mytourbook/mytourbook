/*
 *  File: DateChooser.java 
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.jaret.util.date.JaretDate;
import de.jaret.util.date.holidayenumerator.HolidayEnumerator;

/**
 * A date field with an attached datechooser in a combobox style. The date chooser used is
 * 
 * @see de.jaret.swt.util.datechooser.DateChooserPanel. The popup is displayed using a shell. The creation of the shell
 * and the chooserpanel is lazy. The control offers three strategies for dealing with unparsable input in the textfield:
 * KEEP_AND_MARK: keep the unparsable input, do not modify the date value and mark the field. This is the default
 * behaviour allowing the user to step back and correct the input without slowing down the input process.
 * RESET_TO_LASTVALID: reset the input to the last valid date entered or given RESET_TO_NULL: reset the value to null
 * 
 * The DateFieldCombo features
 * <ul>
 * <li>selectable locale which is used for the determination of the first day of the week and the weekday/months
 * abbreviations</li>
 * <li>display of week of the year selectable</li>
 * <li>keyboard control (cursor keys navigate in the day panel, SHIFT-Cursor-left/right navigate month, ESC cancels, t
 * sets the date to the current date), ctrl-space will open dropdown, RETURN selects</li>
 * <li>roll fields in the edited field with cursor up/down (if a FieldIdentfier is supplied)</li>
 * <li>mousewheel on textfieled. roll field if field identifier is set and feature is enabled (default:true)</li>
 * <li>mousewheel on panel. roll day; use shift+wheel to roll months</li>
 * <li>optional a
 * @see de.jaret.util.date.HolidayEnumerator can be set for highlighting holidays in the day panel</li>
 * <li>can be wired with a time chooser to keep date and time in sync</li>
 * </ul>
 * 
 * @author Peter Kliem
 * @version $Id: DateChooser.java 587 2007-10-14 12:32:10Z olk $
 */
public class DateChooser extends Composite implements FocusListener, IDateChooserListener {
    /** Invalid input behaviour: keep the textual input and mark the field. */
    public static final int KEEP_AND_MARK = 0;

    /** Invalid input behaviour: reset the date to the last valid input. */
    public static final int RESET_TO_LASTVALID = 1;

    /** Invalid input behaviour: reset the date to <code>null</code>. */
    public static final int RESET_TO_NULL = 2;

    /** Color used to mark invalid input. */
    public static final Color MARKER_COLOR = Display.getCurrent().getSystemColor(SWT.COLOR_RED);

    /** divisor for the mousewheel ticks. */
    protected static final int MOUSEWHEEL_DIVISOR = 3;

    /** behaviour on invalid input. */
    protected int _invalidInputBehaviour = KEEP_AND_MARK;

    /** if true: editable. */
    protected boolean _editable = true;

    /** if true: enabled. */
    private boolean _enabled = true;

    /** if true: select all in textfield on focus gain. */
    private boolean _selectAllOnFocusGained = true;

    /**
     * If true mousewheel will roll in the textfield.
     */
    private boolean _textfieldMouseWheelEnable = true;

    /** listener list of interestedlisteners. */
    protected List<IDateChooserListener> _listenerList;

    /** the date value manipulated by the control. */
    protected Date _date = new Date();

    /** dateformat used to format the date in the textfield. */
    protected DateFormat _dateformat;

    /** locale used by the control. */
    protected Locale _locale;

    /** text field widgets used. */
    protected Text _textField;

    /** dropdown button. */
    protected Button _dropdownButton;

    /** shell for the drop down. */
    protected Shell _dropDownShell;

    /** dropped state. */
    protected boolean _dropped = false;

    /** flag regeistering that drop down is about to happen. */
    boolean _goingToDropDown = false;

    /** DateChooserPanel in the dropdown. */
    protected DateChooserPanel _chooserPanel;

    /** Enumerator for identifying holidays. */
    protected HolidayEnumerator _holidayEnumerator;

    /** Provier for additional day information. */
    protected IAdditionalDayInformationProvider _dayInformationProvider;

    /** FieldIdentifier used for field rolling. */
    protected IFieldIdentifier _fieldIdentifier;

    /** initial bg color of the textfield. */
    private Color _textfieldBGColor;

    /** flag to help keeping focus listeners happy. */
    private boolean _hasFocus = false;

    /** timechooser to sync time of the handled date with. */
    protected TimeChooser _timeChooser;

    /** true if th einput is valid. */
    protected boolean _hasValidInput = true;
    
    /**
     * Constructor for the datefieldcombo.
     * 
     * @param parent Composite parent
     * @param style style
     * @param locale locale to be used
     */
    public DateChooser(Composite parent, int style, Locale locale) {
        super(parent, style);
        _locale = locale;
        _dateformat = DateFormat.getDateInstance(DateFormat.MEDIUM, _locale);
        _dateformat.setLenient(false);
        createControls();
        updateTextField(_date);

        addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                onDispose();
            }
        });

    }

    /**
     * Constructor using the default locale.
     * 
     * @param parent Composite parent
     * @param style style
     */
    public DateChooser(Composite parent, int style) {
        this(parent, style, Locale.getDefault());
    }

    /**
     * Retrieve the used dateformat.
     * 
     * @return the dateformat used to format and parse the date.
     */
    public DateFormat getDateFormat() {
        return _dateformat;
    }

    /**
     * Set the dateformat used to parse and format the date in the text field. Default is the MEDIUM style DateInstance
     * for the given locale.
     * 
     * @param dateformat dateformat to use.
     */
    public void setDateFormat(DateFormat dateformat) {
        _dateformat = dateformat;
        updateTextField(_date);
    }

    /**
     * create the controls (a text field and the drop down button).
     */
    private void createControls() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);

        _textField = new Text(this, SWT.BORDER | SWT.RIGHT);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        _textField.setLayoutData(gd);
        // add this as Focuslistener to parse the date when loosing focus
        _textField.addFocusListener(this);
        // save the bg color for resetting mark
        _textfieldBGColor = _textField.getBackground();

        // KeyListener to toggle drop down on ctrl-space
        _textField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent keyEvent) {
                if ((keyEvent.stateMask & SWT.CTRL) != 0 && keyEvent.keyCode == 32) {
                    setDropped(!isDropped());
                    keyEvent.doit = false;
                } else if (keyEvent.keyCode == SWT.CR) {
                    // cr to leave the field
                    _textField.traverse(SWT.TRAVERSE_TAB_NEXT);
                } else if (keyEvent.keyCode == SWT.ARROW_UP) {
                    rollField(1);
                    keyEvent.doit = false;
                } else if (keyEvent.keyCode == SWT.ARROW_DOWN) {
                    rollField(-1);
                    keyEvent.doit = false;
                }
            }
        });

        // mousewheel rolling of fields
        Listener listener = new Listener() {

            public void handleEvent(Event event) {
                switch (event.type) {
                case SWT.MouseWheel:
                    int count = -event.count / MOUSEWHEEL_DIVISOR;
                    if (_textfieldMouseWheelEnable) {
                        rollField(count);
                    }
                    break;
                default:
                    throw new RuntimeException("unsupported event");

                }
            }
        };

        addListener(SWT.MouseWheel, listener);

        _dropdownButton = new Button(this, SWT.ARROW | SWT.DOWN); // | (style & SWT.FLAT)
        gd = new GridData();
        _dropdownButton.setLayoutData(gd);
        // SelectionListener for the dropdown button toggles dropdown state
        _dropdownButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent arg0) {
                setDropped(!isDropped());
            }
        });

    }

    /**
     * dispose has to take care of some additional disposals.
     */
    public void onDispose() {
        if (_dropDownShell != null) {
            _dropDownShell.dispose();
        }
    }

    /**
     * Roll the field (if identifiable) by the given delta.
     * 
     * @param delta delta to roll the field
     */
    private void rollField(int delta) {
        if (_fieldIdentifier != null) {
            if (validateInput()) {
                // proceed only if the current input is valid
                int caretpos = _textField.getCaretPosition();
                int field = _fieldIdentifier.getField(_textField.getText(), caretpos);
                if (field != -1) {
                    Calendar cal = new GregorianCalendar(_locale);
                    cal.setTime(_date);
                    cal.roll(field, delta);
                    setDate(cal.getTime());
                }
            }
        }
    }

    /**
     * Check whether the drop down is dropped down.
     * 
     * @return true if the dropdow is dropped down
     */
    public boolean isDropped() {
        return _dropped;
    }

    /**
     * Set the state of the dropdown.
     * 
     * @param dropped if true the dropdowbn will be displayed.
     */
    public void setDropped(boolean dropped) {
        if (dropped != _dropped) {
            _dropped = dropped;
            if (_dropped && _editable && _enabled) {
                if (_dropDownShell == null) {
                    _dropDownShell = createDropDown();
                }

                _goingToDropDown = true;

                _chooserPanel.setDate(getDate());
                Point size = _dropdownButton.getSize();
                Point dispLocation = toDisplay(_dropdownButton.getLocation());
                Point dropDownSize = _dropDownShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                int dsWidth = dropDownSize.x;
                int dsHeight = dropDownSize.y;
                int locx = dispLocation.x + size.x - dsWidth;
                int locy = dispLocation.y + size.y + 3;
                // corrections if the display would not be complete
                // x
                if (locx < 0) {
                    locx = 0;
                } else if (locx + dsWidth > Display.getCurrent().getBounds().width) {
                    locx = Display.getCurrent().getBounds().width - dsWidth;
                }
                // y
                if (locy + dsHeight > Display.getCurrent().getBounds().height) {
                    locy = dispLocation.y - dsHeight - 3;
                }

                _dropDownShell.setLocation(locx, locy);
                _dropDownShell.setSize(dsWidth, dsHeight);
                _dropDownShell.pack();
                _dropDownShell.layout(true);

                _dropDownShell.setVisible(true);
                _dropDownShell.setActive();
                _chooserPanel.forceFocus();
                resetMark(); // in case of an invalid input reset the amrk since the input continues
            } else if (_dropDownShell != null) {
                _dropDownShell.setVisible(false);
                _goingToDropDown = false;
                _textField.setFocus();
            }
        }

    }

    /**
     * Create the dropdown shell and the chooser panel.
     * 
     * @return the created Shell
     */
    private Shell createDropDown() {
        Shell dropDown = new Shell(getShell(), SWT.NO_TRIM | SWT.BORDER);
        dropDown.setLayout(new FillLayout());
        _chooserPanel = new DateChooserPanel(dropDown, SWT.NULL | SWT.BORDER, false, true, _locale);
        _chooserPanel.setDate(getDate());
        _chooserPanel.addDateChooserListener(this);
        _chooserPanel.setHolidayEnumerator(_holidayEnumerator);
        _chooserPanel.setAdditionalDayInformationProvider(_dayInformationProvider);
        _chooserPanel.setBackground(getBackground());

        _chooserPanel.addFocusListener(this);
        /*
         * The dropdown should be hidden if the focus gets assigned to any other component. This is accomplished by
         * using a ShellListener and hide the dropdown on deactivation. The drawback of this mechanism is that the mouse
         * event of the deactivation will get lost. This seems to be acceptable.
         */
        dropDown.addShellListener(new ShellAdapter() {
            public void shellDeactivated(ShellEvent event) {
                setDropped(false);
            }
        });
        return dropDown;
    }

    /**
     * Get the time part of a set time cchooder and set it to the date.
     * 
     * @param date date to correct
     * @return corrected date (if a time choser is set)
     */
    private Date correctTime(Date date) {
        if (_timeChooser != null) {
            JaretDate d = new JaretDate(date);
            JaretDate time = new JaretDate(_timeChooser.getDateInternal());
            d.setHours(time.getHours());
            d.setMinutes(time.getMinutes());
            date = d.getDate();
        }
        return date;
    }

    /**
     * Retrieve the current selected date. Corrects the time from a time chooser if set.
     * 
     * @return Returns the date.
     */
    public Date getDate() {
        _date = correctTime(_date);
        return _date;
    }

    /**
     * Retrieve the current date without syncing with a time chooser.
     * 
     * @return current selected date
     */
    protected Date getDateInternal() {
        return _date;
    }

    /**
     * Set the date.
     * 
     * @param date The date to set.
     */
    public void setDate(Date date) {
        _date = date;
        updateTextField(_date);
    }

    /**
     * Update the text in the textfield.
     * 
     * @param date date to update the field with
     */
    private void updateTextField(Date date) {
        if (date != null) {
            int caretpos = _textField.getCaretPosition(); // save caretpos
            _textField.setText(_dateformat.format(date));
            _textField.setSelection(caretpos, caretpos); // restore caretpos
        } else {
            _textField.setText("");
        }
    }

    /**
     * Set the input in the textfield direct.
     * 
     * @param text new text of the textfield
     */
    public void setText(String text) {
        _textField.setText(text);
    }

    /**
     * Select the text fields contents.
     * 
     */
    public void selectAll() {
        _textField.selectAll();
    }

    /**
     * Set the selection on the textfield.
     * 
     * @param pos position
     */
    public void setSelection(int pos) {
        _textField.setSelection(pos);
    }

    /**
     * Clear selection on the textfield.
     */
    public void clearSelection() {
        _textField.clearSelection();
    }

    /**
     * Cut operation of the textfield.
     */
    public void cut() {
        _textField.cut();
    }

    /**
     * Copy operation of the textfield.
     */
    public void copy() {
        _textField.copy();
    }

    /**
     * Paste operation of the textfield.
     */
    public void paste() {
        _textField.paste();
    }

    /**
     * {@inheritDoc} The textfield will get the focus.
     */
    public boolean setFocus() {
        super.setFocus();
        return _textField.setFocus();
    }

    /**
     * {@inheritDoc} The textfield will get the focus.
     */
    public boolean forceFocus() {
        return _textField.forceFocus();
    }

    /**
     * Access to the embedded textfield widget.
     * 
     * @return the textfield
     */
    public Text getTextField() {
        return _textField;
    }

    /**
     * Retrieve he holiday enumerator used.
     * 
     * @return the holiday enumerator
     */
    public HolidayEnumerator getHolidayEnumerator() {
        return _holidayEnumerator;
    }

    /**
     * Set a HolidayEnumerator for marking holidays in the day panel. A value of <code>null</code> is valid indicating
     * no HolidayEnumerator to be used.
     * 
     * @param holidayEnumerator HolidayEnumerator to be used.
     */
    public void setHolidayEnumerator(HolidayEnumerator holidayEnumerator) {
        _holidayEnumerator = holidayEnumerator;
        if (_chooserPanel != null) {
            _chooserPanel.setHolidayEnumerator(_holidayEnumerator);
        }
    }

    /**
     * Retrieve the additional information provider.
     * 
     * @return the additional information provider
     */
    public IAdditionalDayInformationProvider getAdditionalDayInformationProvider() {
        return _dayInformationProvider;
    }

    /**
     * Set an additional information provider for days.
     * 
     * @param dayInformationProvider the information provider
     */
    public void setAdditionalDayInformationProvider(IAdditionalDayInformationProvider dayInformationProvider) {
        _dayInformationProvider = dayInformationProvider;
        if (_chooserPanel != null) {
            _chooserPanel.setAdditionalDayInformationProvider(dayInformationProvider);
        }
    }

    // DateChooserListener
    /**
     * {@inheritDoc} If a date has been chosen in the panel, close dropdaown, selection finished.
     */
    public void dateChosen(Date date) {
        setDate(_chooserPanel.getDate());
        setDropped(false);
        fireDateChosen(correctTime(date));
    }

    /**
     * {@inheritDoc} Propagate cancelling.
     */
    public void choosingCanceled() {
        updateTextField(_date);
        setDropped(false);
        fireChoosingCanceled();
    }

    /**
     * {@inheritDoc} Do an update on the textfield.
     */
    public void dateIntermediateChange(Date date) {
        updateTextField(date);
        fireIntermediateChange(correctTime(date));
    }
  
    /**
     * {@inheritDoc} Do nothing.
     */
    public void inputInvalid() {
        // nothing to do: the panel will never fire an invalid event
    }

    // End of DateChooser Listener

    // FocusListener
    /**
     * {@inheritDoc} On gaining focus on the textfield, select its content. If the datechooser does not already own the
     * focus, notify other listeners.
     */
    public void focusGained(FocusEvent evt) {
        // on gaining focus select the textfield contents if configured
        if (evt.widget.equals(_textField) && _selectAllOnFocusGained) {
            _textField.selectAll();
        }
        if (!_hasFocus) {
            _hasFocus = true;
            super.notifyListeners(SWT.FocusIn, new Event());
        }
    }

    /**
     * {@inheritDoc} On loosing focus validate the input and check whether the focus will be going to the dropdown. In
     * latter case do not notify other listeners.
     */
    public void focusLost(FocusEvent evt) {
        // on loosing focus parse the text entered
        validateInput();

        // check whether the click occured over the dropdown button and deduce this will not modify
        // the focus state
        if (Display.getCurrent().getCursorControl() == _dropdownButton) {
            _goingToDropDown = true;
        }

        if (_hasFocus && !_goingToDropDown) {
            _hasFocus = false;
            super.notifyListeners(SWT.FocusOut, new Event());
        }

    }

    // End of FocusListener
    /**
     * Validate the input currently present in the textfield. Resets a mark if set and handles input behaviour for
     * invalid inputs.
     * 
     * @return true if valid
     */
    public boolean validateInput() {
        boolean valid = false;
        String text = _textField.getText();
        Date date = null;
        try {
            date = _dateformat.parse(text);
            valid = true;
        } catch (ParseException e) {
            // nothing to do here
        }
        if (date != null) {
            // parsing successful
            setDate(date);
            resetMark(); // in case a mark has been set
            // fireDateChosen(date);
        } else {
            switch (_invalidInputBehaviour) {
            case KEEP_AND_MARK:
                setMark();
                break;
            case RESET_TO_LASTVALID:
                updateTextField(_date);
                break;
            case RESET_TO_NULL:
                setDate(null);
                break;

            default:
                throw new RuntimeException("Invalid InputBehaviour set");
            }
        }
        if (!valid && _hasValidInput) {
            _hasValidInput = valid;
            fireInputInvalid();
        }
        _hasValidInput = valid;
        return valid;
    }

    /**
     * Reset the background color of the textfield.
     */
    private void resetMark() {
        _textField.setBackground(_textfieldBGColor);
    }

    /**
     * Set the background color of the textfield to the marker color.
     * 
     */
    private void setMark() {
        _textField.setBackground(MARKER_COLOR);
    }

    /**
     * @return Returns the invalidInputBehaviour.
     */
    public int getInvalidInputBehaviour() {
        return _invalidInputBehaviour;
    }

    /**
     * @param invalidInputBehaviour The invalidInputBehaviour to set.
     */
    public void setInvalidInputBehaviour(int invalidInputBehaviour) {
        _invalidInputBehaviour = invalidInputBehaviour;
    }

    /**
     * @return Returns the editable state.
     */
    public boolean isEditable() {
        return _editable;
    }

    /**
     * Set the editable state. If set to false the textfiled be set to editable(false) and the dropdown will be
     * disabled.
     * 
     * @param editable The editable state to be set.
     */
    public void setEditable(boolean editable) {
        _editable = editable;
        _textField.setEditable(editable);
        setDropped(false);
    }

    /**
     * @return the enabled state of the widget.
     */
    public boolean isEnabled() {
        return _enabled;
    }

    /**
     * Set the enabled state of the widget.
     * 
     * @param enabled the enabled state to set
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        _enabled = enabled;
        _textField.setEnabled(enabled);
        _dropdownButton.setEnabled(enabled);
        setDropped(false);
    }

    /**
     * Set the onClick property of the date chooser panel.
     * 
     * @param oneClick if set to true a single click will selct the date.
     */
    public void setOneClickSelection(boolean oneClick) {
        // chooser panel need not to be instantiated by now
        if (_dropDownShell == null) {
            _dropDownShell = createDropDown();
        }
        _chooserPanel.setOneClickSelection(oneClick);
    }

    /**
     * Get the oneClick property of the date chooser panel.
     * 
     * @return true if a single click will select the date
     */
    public boolean isOneClickSelection() {
        // chooser panel needs not to be instantiated by now
        if (_dropDownShell == null) {
            _dropDownShell = createDropDown();
        }
        return _chooserPanel.isOneClickSelection();
    }

    /**
     * Return the chooser panel used by the DateChooser.
     * 
     * @return DateChooserPanel used by the date chooser.
     */
    public DateChooserPanel getDateChooserPanel() {
        // chooser panel need not to be instantiated by now
        if (_dropDownShell == null) {
            _dropDownShell = createDropDown();
        }
        return _chooserPanel;
    }

    /**
     * Add a DateChooserListener to be informed about changes.
     * 
     * @param listener the DateChooserListener to be added
     */
    public void addDateChooserListener(IDateChooserListener listener) {
        if (_listenerList == null) {
            _listenerList = new ArrayList<IDateChooserListener>();
        }
        _listenerList.add(listener);
    }

    /**
     * Remove a DateChooserListener.
     * 
     * @param listener the DateChooserListener to be removed
     */
    public void remDateChooserListener(IDateChooserListener listener) {
        if (_listenerList == null) {
            return;
        }
        _listenerList.remove(listener);
    }

    /**
     * Inform listeners that a date has been chosen.
     * 
     * @param date chosen date
     */
    protected void fireDateChosen(Date date) {
        if (_listenerList != null) {
            for (IDateChooserListener listener : _listenerList) {
                listener.dateChosen(date);
            }
        }
    }

    /**
     * Inform listeners about an intermediate change of the date.
     * 
     * @param date current date
     */
    protected void fireIntermediateChange(Date date) {
        if (_listenerList != null) {
            for (IDateChooserListener listener : _listenerList) {
                listener.dateIntermediateChange(date);
            }
        }
    }

    /**
     * Inform listeners that the choosing has been cancelled.
     */
    protected void fireChoosingCanceled() {
        if (_listenerList != null) {
            for (IDateChooserListener listener : _listenerList) {
                listener.choosingCanceled();
            }
        }
    }

    /**
     * Inform listeners that the current input has become invalid.
     */
    protected void fireInputInvalid() {
        if (_listenerList != null) {
            for (IDateChooserListener listener : _listenerList) {
                listener.inputInvalid();
            }
        }
    }

    /**
     * @return Returns the selectAllOnFocusGained.
     */
    public boolean isSelectAllOnFocusGained() {
        return _selectAllOnFocusGained;
    }

    /**
     * @param selectAllOnFocusGained The selectAllOnFocusGained to set.
     */
    public void setSelectAllOnFocusGained(boolean selectAllOnFocusGained) {
        _selectAllOnFocusGained = selectAllOnFocusGained;
    }

    /**
     * @return Returns the fieldIdentifier.
     */
    public IFieldIdentifier getFieldIdentifier() {
        return _fieldIdentifier;
    }

    /**
     * @param fieldIdentifier The fieldIdentifier to set.
     */
    public void setFieldIdentifier(IFieldIdentifier fieldIdentifier) {
        _fieldIdentifier = fieldIdentifier;
    }

    /**
     * Retrieve state of mousewheel support on textfield.
     * 
     * @return true if enabled
     */
    public boolean isTextfieldMouseWheelEnable() {
        return _textfieldMouseWheelEnable;
    }

    /**
     * Enable/Disable mousewheel for rolling on text field. Default is true.
     * 
     * @param mouseWheelEnable true for enable
     */
    public void setTextfieldMouseWheelEnable(boolean mouseWheelEnable) {
        _textfieldMouseWheelEnable = mouseWheelEnable;
    }

    /**
     * Get the timechooser set for time synchronization.
     * 
     * @return timechooser or <code>null</code>
     */
    public TimeChooser getTimeChooser() {
        return _timeChooser;
    }

    /**
     * Set a timechoser to synchronize the time of the chosen date with.
     * 
     * @param timeChooser time chooser
     */
    public void setTimeChooser(TimeChooser timeChooser) {
        _timeChooser = timeChooser;
    }

}
