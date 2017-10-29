/*******************************************************************************
 * Copyright (C) 2011 Matthias Helmling and Contributors
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
	private ArrayList<Integer>	_yearValues;
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
						onSelectMonth();
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
						onSelectYear();
					}
				});

				_comboYear.addTraverseListener(new TraverseListener() {

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

		_yearValues = new ArrayList<Integer>();

		final LocalDateTime firstTourDateTime = CalendarTourDataProvider.getInstance().getFirstTourDateTime();
		final int firstYear = firstTourDateTime.getYear();

		for (int year = firstYear; year <= thisYear; year++) {

			_comboYear.add(Integer.toString(year));
			_yearValues.add(year);
		}

		// select last year
		_comboYear.select(_yearValues.size() - 1);
	}

	public int getSelectedMonth() {

		return _comboMonth.getSelectionIndex() + 1;
	}

	public int getSelectedYear() {

		final int selectedYearIndex = _comboYear.getSelectionIndex();

		return _yearValues.get(selectedYearIndex);
	}

	private void onSelectMonth() {

		System.out.println(
				(net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] onSelectMonth"));
// TODO remove SYSTEM.OUT.PRINTLN

//		final int month = _comboMonth.getSelectionIndex() + 1;
//
//		_calendarGraph.gotoDate_Month(month);
	}

	private void onSelectYear() {

		System.out.println(
				(net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] onSelectYear"));
// TODO remove SYSTEM.OUT.PRINTLN

//		final int index = _comboYear.getSelectionIndex();
//		if (index >= 0) {
//
//			final int year = _yearValues.get(_comboYear.getSelectionIndex());
//			_calendarGraph.gotoDate_Year(year);
//
//		} else {
//
//			final String yearString = _comboYear.getText();
//			try {
//				final int year = Integer.parseInt(yearString);
//				_calendarGraph.gotoDate_Year(year);
//			} catch (final Exception e) { // TODO specify Execption
//				// do nothing
//			}
//		}
	}

	void setDate(final LocalDate requestedDate) {

		final int year = requestedDate.getYear();

		if (year < _yearValues.get(0)) {

			// year is before available years

			// select first date
			_comboMonth.select(0);
			_comboYear.select(0);

		} else if (year > _yearValues.get(_yearValues.size() - 1)) {

			// year is after the available years

			// select last date
			_comboMonth.select(11);
			_comboYear.select(_yearValues.size() - 1);

		} else {

			// year is available
		}

	}

}
