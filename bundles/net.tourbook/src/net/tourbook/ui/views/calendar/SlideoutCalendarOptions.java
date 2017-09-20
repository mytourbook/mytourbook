/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.ui.views.calendar.CalendarView.TourInfoFormatter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;

/**
 * Properties slideout for the calendar view.
 */
public class SlideoutCalendarOptions extends ToolbarSlideout {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private SelectionAdapter		_defaultSelectionAdapter;
	private SelectionAdapter		_weekValueListener;
	private FocusListener			_keepOpenListener;

	private ActionOpenPrefDialog	_actionPrefDialog;
	private Action					_actionRestoreDefaults;

	/*
	 * UI controls
	 */
	private CalendarView			_calendarView;

	private Combo					_comboTour_Value_1;
	private Combo					_comboTour_Value_2;
	private Combo					_comboTour_Value_3;

	private Combo					_comboTour_Format_1_1;
	private Combo					_comboTour_Format_1_2;
	private Combo					_comboTour_Format_2_1;
	private Combo					_comboTour_Format_2_2;
	private Combo					_comboTour_Format_3_1;
	private Combo					_comboTour_Format_3_2;

	private Combo					_comboWeek_Value_1;
	private Combo					_comboWeek_Value_2;
	private Combo					_comboWeek_Value_3;
	private Combo					_comboWeek_Value_4;
	private Combo					_comboWeek_Value_5;

	private Combo					_comboWeek_Format_1;
	private Combo					_comboWeek_Format_2;
	private Combo					_comboWeek_Format_3;
	private Combo					_comboWeek_Format_4;
	private Combo					_comboWeek_Format_5;

	/**
	 * @param ownerControl
	 * @param toolBar
	 * @param tourChart
	 * @param gridPrefPrefix
	 */
	public SlideoutCalendarOptions(	final Control ownerControl,
									final ToolBar toolBar,
									final CalendarView tourChart) {

		super(ownerControl, toolBar);

		_calendarView = tourChart;
	}

	private void createActions() {

	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI();

		createActions();

		final Composite ui = createUI(parent);

		setupUI();

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory
					.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);
				createUI_40_TourInfo(container);
				createUI_50_WeekSummary(container);
			}
		}

		return shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.Slideout_CalendarOptions_Label_Title);
		MTFont.setBannerFont(label);
	}

	private void createUI_40_TourInfo(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_TourInfo);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(group);
		{
			/*
			 * 1. Line
			 */

			// label
			final Label label = new Label(group, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_N_Line, 1));

			// value
			_comboTour_Value_1 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Value_1.setVisibleItemCount(20);
			_comboTour_Value_1.addSelectionListener(_defaultSelectionAdapter);
			_comboTour_Value_1.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_1_1 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_1_1.setVisibleItemCount(20);
			_comboTour_Format_1_1.addSelectionListener(_defaultSelectionAdapter);
			_comboTour_Format_1_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_1_2 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_1_2.setVisibleItemCount(20);
			_comboTour_Format_1_2.addSelectionListener(_defaultSelectionAdapter);
			_comboTour_Format_1_2.addFocusListener(_keepOpenListener);
		}
		{
			/*
			 * 2. Line
			 */
			final Label label = new Label(group, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_N_Line, 2));

			_comboTour_Value_2 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Value_2.setVisibleItemCount(20);
			_comboTour_Value_2.addSelectionListener(_defaultSelectionAdapter);
			_comboTour_Value_2.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_2_1 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_2_1.setVisibleItemCount(20);
			_comboTour_Format_2_1.addSelectionListener(_defaultSelectionAdapter);
			_comboTour_Format_2_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_2_2 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_2_2.setVisibleItemCount(20);
			_comboTour_Format_2_2.addSelectionListener(_defaultSelectionAdapter);
			_comboTour_Format_2_2.addFocusListener(_keepOpenListener);
		}
		{
			/*
			 * 3. Line
			 */
			final Label label = new Label(group, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_N_Line, 3));

			_comboTour_Value_3 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Value_3.setVisibleItemCount(20);
			_comboTour_Value_3.addSelectionListener(_defaultSelectionAdapter);
			_comboTour_Value_3.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_3_1 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_3_1.setVisibleItemCount(20);
			_comboTour_Format_3_1.addSelectionListener(_defaultSelectionAdapter);
			_comboTour_Format_3_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_3_2 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_3_2.setVisibleItemCount(20);
			_comboTour_Format_3_2.addSelectionListener(_defaultSelectionAdapter);
			_comboTour_Format_3_2.addFocusListener(_keepOpenListener);
		}
	}

	private void createUI_50_WeekSummary(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_WeekSummary);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		{
			/*
			 * 1. Line
			 */

			// label
			final Label label = new Label(group, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_N_Line, 1));

			// value
			_comboWeek_Value_1 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboWeek_Value_1.setVisibleItemCount(20);
			_comboWeek_Value_1.addSelectionListener(_weekValueListener);
			_comboWeek_Value_1.addFocusListener(_keepOpenListener);

			// value format
			_comboWeek_Format_1 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboWeek_Format_1.setVisibleItemCount(20);
			_comboWeek_Format_1.addSelectionListener(_defaultSelectionAdapter);
			_comboWeek_Format_1.addFocusListener(_keepOpenListener);
		}
		{
			/*
			 * 2. Line
			 */
			final Label label = new Label(group, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_N_Line, 2));

			_comboWeek_Value_2 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboWeek_Value_2.setVisibleItemCount(20);
			_comboWeek_Value_2.addSelectionListener(_weekValueListener);
			_comboWeek_Value_2.addFocusListener(_keepOpenListener);

			// value format
			_comboWeek_Format_2 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboWeek_Format_2.setVisibleItemCount(20);
			_comboWeek_Format_2.addSelectionListener(_defaultSelectionAdapter);
			_comboWeek_Format_2.addFocusListener(_keepOpenListener);
		}
		{
			/*
			 * 3. Line
			 */
			final Label label = new Label(group, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_N_Line, 3));

			_comboWeek_Value_3 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboWeek_Value_3.setVisibleItemCount(20);
			_comboWeek_Value_3.addSelectionListener(_weekValueListener);
			_comboWeek_Value_3.addFocusListener(_keepOpenListener);

			// value format
			_comboWeek_Format_3 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboWeek_Format_3.setVisibleItemCount(20);
			_comboWeek_Format_3.addSelectionListener(_defaultSelectionAdapter);
			_comboWeek_Format_3.addFocusListener(_keepOpenListener);
		}
		{
			/*
			 * 4. Line
			 */
			final Label label = new Label(group, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_N_Line, 4));

			_comboWeek_Value_4 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboWeek_Value_4.setVisibleItemCount(20);
			_comboWeek_Value_4.addSelectionListener(_weekValueListener);
			_comboWeek_Value_4.addFocusListener(_keepOpenListener);

			// value format
			_comboWeek_Format_4 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboWeek_Format_4.setVisibleItemCount(20);
			_comboWeek_Format_4.addSelectionListener(_defaultSelectionAdapter);
			_comboWeek_Format_4.addFocusListener(_keepOpenListener);
		}
		{
			/*
			 * 5. Line
			 */
			final Label label = new Label(group, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_N_Line, 5));

			_comboWeek_Value_5 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboWeek_Value_5.setVisibleItemCount(20);
			_comboWeek_Value_5.addSelectionListener(_weekValueListener);
			_comboWeek_Value_5.addFocusListener(_keepOpenListener);

			// value format
			_comboWeek_Format_5 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboWeek_Format_5.setVisibleItemCount(20);
			_comboWeek_Format_5.addSelectionListener(_defaultSelectionAdapter);
			_comboWeek_Format_5.addFocusListener(_keepOpenListener);
		}
	}

	private void fillWeekFormats(final Widget widget, final Combo comboWeek_Value, final Combo comboWeek_Format) {

		if (widget != comboWeek_Value) {

			// another combo fired the event

			return;
		}

		comboWeek_Format.removeAll();

		final int selectedIndex = comboWeek_Value.getSelectionIndex();

		if (selectedIndex < 0) {
			return;
		}

		final WeekSummaryFormatter selectedFormatter = _calendarView.tourWeekSummaryFormatter[selectedIndex];
		final ValueFormat[] valueFormats = selectedFormatter.getValueFormats();

		if (valueFormats == null) {
			return;
		}

		final ValueFormat defaultFormat = selectedFormatter.getDefaultFormat();
		int defaultIndex = -0;

		for (int formatIndex = 0; formatIndex < valueFormats.length; formatIndex++) {

			final ValueFormat valueFormat = valueFormats[formatIndex];
			comboWeek_Format.add(ColumnManager.getValueFormatterName(valueFormat));

			if (valueFormat == defaultFormat) {
				defaultIndex = formatIndex;
			}
		}

		comboWeek_Format.select(defaultIndex);
	}

	private void initUI() {

		_defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChange_UI();
			}
		};

		_weekValueListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChange_WeekValue(e.widget);
			}
		};

		_keepOpenListener = new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {

				/*
				 * This will fix the problem that when the list of a combobox is displayed, then the
				 * slideout will disappear :-(((
				 */
				setIsAnotherDialogOpened(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				setIsAnotherDialogOpened(false);
			}
		};

	}

	private void onChange_UI() {

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tonChange_UI"));
		// TODO remove SYSTEM.OUT.PRINTLN

		saveState();
	}

	private void onChange_WeekValue(final Widget widget) {

		fillWeekFormats(widget, _comboWeek_Value_1, _comboWeek_Format_1);
		fillWeekFormats(widget, _comboWeek_Value_2, _comboWeek_Format_2);
		fillWeekFormats(widget, _comboWeek_Value_3, _comboWeek_Format_3);
		fillWeekFormats(widget, _comboWeek_Value_4, _comboWeek_Format_4);
		fillWeekFormats(widget, _comboWeek_Value_5, _comboWeek_Format_5);
	}

	private void restoreState() {

	}

	private void saveState() {

	}

	private void setupUI() {

		/*
		 * Fill Combos
		 */

		for (final TourInfoFormatter tourFormatter : _calendarView.tourInfoFormatter) {

			_comboTour_Value_1.add(tourFormatter.getText());
			_comboTour_Value_2.add(tourFormatter.getText());
			_comboTour_Value_3.add(tourFormatter.getText());
		}

		for (final WeekSummaryFormatter weekFormatter : _calendarView.tourWeekSummaryFormatter) {

			_comboWeek_Value_1.add(weekFormatter.getText());
			_comboWeek_Value_2.add(weekFormatter.getText());
			_comboWeek_Value_3.add(weekFormatter.getText());
			_comboWeek_Value_4.add(weekFormatter.getText());
			_comboWeek_Value_5.add(weekFormatter.getText());
		}

		final String formatName_HH = ColumnManager.getValueFormatterName(ValueFormat.TIME_HH);
		final String formatName_HH_MM = ColumnManager.getValueFormatterName(ValueFormat.TIME_HH_MM);
		final String formatName_HH_MM_SS = ColumnManager.getValueFormatterName(ValueFormat.TIME_HH_MM_SS);

		final String formatName_1_0 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_0);
		final String formatName_1_1 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_1);
		final String formatName_1_2 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_2);
		final String formatName_1_3 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_3);

	}

}
