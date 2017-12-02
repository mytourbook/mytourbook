/*******************************************************************************
 * Copyright (C) 2011,2017 Matthias Helmling and Contributors
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

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;

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

	private static final String	ID			= "net.tourbook.calendar.yearmonthselector";	//$NON-NLS-1$
	//
	private final boolean		_isOSX		= UI.IS_OSX;
	private final boolean		_isLinux	= UI.IS_LINUX;
	//
	private ArrayList<Integer>	_allYearValues;
	//
	private CalendarGraph		_calendarGraph;

	/*
	 * UI controls
	 */
	private Combo				_comboYear;
	private Combo				_comboMonth;

	protected CalendarYearMonthContributionItem(final CalendarGraph calendarGraph) {

		super(ID);

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
				/*
				 * Month
				 */

				// combo
				_comboMonth = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboMonth.setToolTipText(Messages.Calendar_View_Combo_Month_Tooltip);
				_comboMonth.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectDate();
					}
				});
			}
			{
				/*
				 * Year
				 */

				// combo
				_comboYear = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboYear.setToolTipText(Messages.Calendar_View_Combo_Year_Tooltip);
				_comboYear.setVisibleItemCount(50);
				GridDataFactory
						.fillDefaults()//
						.hint(pc.convertWidthInCharsToPixels(_isOSX ? 12 : _isLinux ? 12 : 5), SWT.DEFAULT)
						.applyTo(_comboYear);

				_comboYear.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectDate();
					}
				});

				_comboYear.addTraverseListener(new TraverseListener() {

					@Override
					public void keyTraversed(final TraverseEvent e) {
						if (e.detail == SWT.TRAVERSE_RETURN) {
							onSelectDate();
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

		LocalDate date = LocalDate.now();
		final int thisMonth = date.getMonthValue();
		date = date.withMonth(1);

		for (int monthIndex = 0; monthIndex < 12; monthIndex++) {

			_comboMonth.add(TimeTools.Formatter_Month.format(date));

			date = date.plusMonths(1);
		}

		// select this month
		_comboMonth.select(thisMonth - 1);

	}

	private void fillYearComboBox() {

		final int thisYear = LocalDate.now().getYear();

		_allYearValues = new ArrayList<Integer>();

		final LocalDateTime firstTourDateTime = CalendarTourDataProvider.getInstance().getFirstTourDateTime();
		final int firstYear = firstTourDateTime.getYear();

		for (int year = firstYear; year <= thisYear; year++) {

			_comboYear.add(Integer.toString(year));
			_allYearValues.add(year);
		}

		// select last year
		_comboYear.select(_allYearValues.size() - 1);
	}

	private void onSelectDate() {

		int yearIndex = _comboYear.getSelectionIndex();
		if (yearIndex < 0) {
			yearIndex = 0;
		}

		final int selectedYear = _allYearValues.get(yearIndex);
		final int selectedMonth = _comboMonth.getSelectionIndex() + 1;

		_calendarGraph.gotoDate(LocalDate.of(selectedYear, selectedMonth, 1), false);
	}

	void setDate(final LocalDate requestedDate) {

		final int requestedYear = requestedDate.getYear();

		if (requestedYear < _allYearValues.get(0)) {

			// year is before available years

			// select first date
			_comboMonth.select(0);
			_comboYear.select(0);

		} else if (requestedYear > _allYearValues.get(_allYearValues.size() - 1)) {

			// year is after the available years

			// select last date
			_comboMonth.select(11);
			_comboYear.select(_allYearValues.size() - 1);

		} else {

			// year is available

			for (int yearIndex = 0; yearIndex < _allYearValues.size(); yearIndex++) {

				final int currentYear = _allYearValues.get(yearIndex);

				if (currentYear == requestedYear) {

					final int requestedMonth = requestedDate.getMonthValue();

					_comboMonth.select(requestedMonth - 1);
					_comboYear.select(yearIndex);

					break;
				}
			}
		}

	}

}
