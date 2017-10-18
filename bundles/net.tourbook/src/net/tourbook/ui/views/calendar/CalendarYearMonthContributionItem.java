/*******************************************************************************
 * Copyright (C) 2011  Matthias Helmling and Contributors
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.common.time.TimeTools;
import net.tourbook.ui.UI;

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

public class CalendarYearMonthContributionItem extends ControlContribution {

	private static final String			ID			= "net.tourbook.calendar.yearmonthselector";	//$NON-NLS-1$
	private final boolean				_isOSX		= net.tourbook.common.UI.IS_OSX;

	private final boolean				_isLinux	= net.tourbook.common.UI.IS_LINUX;
	private Combo						_cboYear;
	private Combo						_cboMonth;
	private ArrayList<Integer>			_cboYearValues;
	private ArrayList<Integer>			_cboMonthValues;
	private HashMap<Integer, Integer>	_cboYearKeys;

	private HashMap<Integer, Integer>	_cboMonthKeys;

	private CalendarGraph				_calendarGraph;

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

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(2)
				.extendedMargins(0, 10, 0, 0)
				//				.spacing(0, 0)
				.applyTo(container);
		{
			{
				// month combo
				_cboMonth = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_cboMonth.setToolTipText(Messages.Calendar_View_Combo_Month_Tooltip);
				_cboMonth.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectMonth();
					}
				});
			}
			{
				// year combo
				_cboYear = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_cboYear.setToolTipText(Messages.Calendar_View_Combo_Year_Tooltip);
				_cboYear.setVisibleItemCount(20);
				GridDataFactory
						.fillDefaults()//
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
			}
		}

		fillMonthComboBox();
		fillYearComboBox();

		return container;

	}

	private void fillMonthComboBox() {

		LocalDate dt = LocalDate.now();
		final int thisMonth = dt.getMonthValue();
		dt = dt.withMonth(1);

		_cboMonthValues = new ArrayList<Integer>();
		_cboMonthKeys = new HashMap<Integer, Integer>();

		for (int monthIndex = 0; monthIndex < 12; monthIndex++) {

			// make the toolbar fit more likely into one line //$NON-NLS-1$
			_cboMonth.add(TimeTools.Formatter_Month.format(dt));

			_cboMonthValues.add(dt.getMonthValue());
			_cboMonthKeys.put(dt.getMonthValue(), monthIndex);

			dt = dt.plusMonths(1);
		}

		_cboMonth.select(thisMonth - 1);

	}

	private void fillYearComboBox() {

		final int thisYear = (LocalDate.now()).getYear();

		_cboYearValues = new ArrayList<Integer>();
		_cboYearKeys = new HashMap<Integer, Integer>();

		// for (int i = thisYear, j = 0; j < 21; i--, j++) {
		// changed to be consistent with the statistic charts

		final LocalDateTime firstTourDateTime = CalendarTourDataProvider.getInstance().getFirstTourDateTime();
		final int firstYear = firstTourDateTime.getYear();
		final int years = Math.max(thisYear - firstYear, 1);

		for (int i = thisYear - years, j = 0; j <= years; i++, j++) {
			_cboYear.add(UI.EMPTY_STRING + i);
			_cboYearValues.add(i);
			_cboYearKeys.put(i, j);
		}

		_cboYear.select(years - 1);

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
			_cboYear.setText(UI.EMPTY_STRING + year);
		}
	}

	public void setGraph(final CalendarGraph calendarGraph) {
		this._calendarGraph = calendarGraph;
	}

}
