package net.tourbook.ui.views.calendar;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

public class CalendarYearMonthContributionItem extends ControlContribution {

	private final boolean						_isOSX						= net.tourbook.util.UI.IS_OSX;
	private final boolean						_isLinux					= net.tourbook.util.UI.IS_LINUX;
	
	private Combo	_cboYear;
	private Combo	_cboMonth;
	private ArrayList<Integer>	_cboYearValues;
	private ArrayList<Integer>	_cboMonthValues;
	private HashMap<Integer, Integer>	_cboYearKeys;
	private HashMap<Integer, Integer>	_cboMonthKeys;
	
	private CalendarGraph		_calendarGraph;

	private static final String	ID	= "net.tourbook.calendar.yearmonthselector"; //$NON-NLS-1$

	protected CalendarYearMonthContributionItem() {
		this(ID);
	}

	protected CalendarYearMonthContributionItem(final CalendarGraph calendarGraph) {
		this(ID);
		_calendarGraph = calendarGraph;
	}

	protected CalendarYearMonthContributionItem(final String id) {
		super(id);
	}

	@Override
	protected Control createControl(final Composite parent) {
		
		final PixelConverter pc = new PixelConverter(parent);

		Composite content;

		content = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(content);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.extendedMargins(0, 0, 0, 1)
				.spacing(0, 0)
				.applyTo(content);

		// label
		// final Label label = new Label(content, SWT.NONE);
		// GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).indent(10, 0).applyTo(label);
		// label.setText("Go: ");

		// month combo
		_cboMonth = new Combo(content, SWT.DROP_DOWN | SWT.READ_ONLY);
		_cboMonth.setToolTipText("Go to month");
		_cboMonth.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectMonth();
			}
		});

		// year combo
		_cboYear = new Combo(content, SWT.DROP_DOWN);
		_cboYear.setToolTipText("Got to year");
		_cboYear.setVisibleItemCount(20);
		GridDataFactory.fillDefaults()//
				.hint(pc.convertWidthInCharsToPixels(_isOSX ? 12 : _isLinux ? 12 : 5), SWT.DEFAULT)
				.applyTo(_cboYear);
		_cboYear.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectYear();
			}
		});
		_cboYear.addTraverseListener(new TraverseListener() {

			@Override
			public void keyTraversed(final TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN) {
					onSelectYear();
				}

			}

		});

		fillMonthComboBox();
		fillYearComboBox();

		// TODO
		// addPrefListener();
		// TODO
		// reselectLastPerson();

		return content;

	}

	private void fillMonthComboBox() {

		DateTime dt = new DateTime();
		final int thisMonth = dt.getMonthOfYear();
		dt = dt.withMonthOfYear(DateTimeConstants.JANUARY);

		_cboMonthValues = new ArrayList<Integer>();
		_cboMonthKeys = new HashMap<Integer, Integer>();

		for (int i = 0; i < 12; i++) {
			// _cboMonth.add(dt.toString("MMMM"));
			_cboMonth.add(dt.toString("MMM")); // make the toolbar fit more likely into one line
			_cboMonthValues.add(dt.getMonthOfYear());
			_cboMonthKeys.put(dt.getMonthOfYear(), i);
			dt = dt.plusMonths(1);
		}

		_cboMonth.select(thisMonth - 1);

	}

	private void fillYearComboBox() {

		final int thisYear = (new DateTime()).getYear();
		
		_cboYearValues = new ArrayList<Integer>();
		_cboYearKeys = new HashMap<Integer, Integer>();

		for (int i = thisYear, j = 0; j < 20; i--, j++) {
			_cboYear.add("" + i);
			_cboYearValues.add(i);
			_cboYearKeys.put(i, j);
		}

		_cboYear.select(0);

	}

	public int getSelectedMonth() {
		final int index = _cboMonth.getSelectionIndex();
		if (index > 0) {
			return _cboMonthValues.get(index);
		}
		return -1;
	}

	public int getSelectedYear() {
		final int index = _cboYear.getSelectionIndex();
		if (index > 0) {
			return _cboYearValues.get(index);
		} else {
			try {
				return Integer.parseInt(_cboYear.getText());
			} catch (final Exception e) { // TODO specify exception
				// ignore
			}
		}
		return -1;
	}

	protected void onSelectMonth() {
		final int month = _cboMonthValues.get(_cboMonth.getSelectionIndex());
		if (null != _calendarGraph) {
			_calendarGraph.gotoMonth(month);
		}

	}

	protected void onSelectYear() {
		final int index = _cboYear.getSelectionIndex();
		if (index >= 0) {
			final int year = _cboYearValues.get(_cboYear.getSelectionIndex());
			if (null != _calendarGraph) {
				_calendarGraph.gotoYear(year);
			}
		} else {
			final String yearString = _cboYear.getText();
			try {
				final int year = Integer.parseInt(yearString);
				_calendarGraph.gotoYear(year);
			} catch (final Exception e) { // TODO specify Execption
				// do nothing
			}
		}
	}

	public void selectMonth(final int month) {
		if (_cboMonthKeys.containsKey(month)) {
			final int index = _cboMonthKeys.get(month);
			if (index < _cboMonth.getItemCount()) {
				_cboMonth.select(_cboMonthKeys.get(month));
			}
		}
	}

	public void selectYear(final int year) {
		boolean foundInList = false;
		if (_cboYearKeys.containsKey(year)) {
			final int index = _cboYearKeys.get(year);
			if (index < _cboYear.getItemCount()) {
				_cboYear.select(_cboYearKeys.get(year));
				foundInList = true;
			}
		}
		if (!foundInList) {
			_cboYear.setText("" + year);
		}
	}

	public void setGraph(final CalendarGraph calendarGraph) {
		this._calendarGraph = calendarGraph;
	}

}
