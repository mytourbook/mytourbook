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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.Util;
import net.tourbook.ui.views.calendar.CalendarView.TourInfoFormatter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;

/**
 * Properties slideout for the calendar view.
 */
public class SlideoutCalendarOptions extends ToolbarSlideout {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private SelectionAdapter		_defaultSelectionListener;
	private MouseWheelListener		_defaultMouseWheelListener;
	private SelectionAdapter		_weekValueListener;
	private FocusListener			_keepOpenListener;

	private ActionOpenPrefDialog	_actionPrefDialog;
	private Action					_actionRestoreDefaults;

	private boolean					_isUpdateUI;

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private CalendarView			_calendarView;

	private Button					_btnReset;

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

	private Combo					_comboConfigName;

	private Spinner					_spinnerWeekHeight;

	private Text					_textConfigName;

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

		initUI(parent);

		createActions();

		final Composite ui = createUI(parent);

		fillUI();
		fillUI_Config();

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
				createUI_20_Layout(container);
				createUI_40_TourInfo(container);
				createUI_50_WeekSummary(container);

				createUI_999_ConfigName(container);
			}
		}

		return shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Label: Slideout title
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Title);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
				MTFont.setBannerFont(label);
			}
			{
				/*
				 * Combo: Configuration
				 */
				_comboConfigName = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
				_comboConfigName.setVisibleItemCount(20);
				_comboConfigName.addFocusListener(_keepOpenListener);
				_comboConfigName.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectConfig();
					}
				});
				GridDataFactory
						.fillDefaults()
						//						.grab(true, false)
						.align(SWT.BEGINNING, SWT.CENTER)
						.indent(20, 0)
						.hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
						.applyTo(_comboConfigName);
			}
			{
				/*
				 * Button: Reset
				 */
				_btnReset = new Button(container, SWT.PUSH);
				_btnReset.setText(Messages.App_Action_Reset);
				_btnReset.setToolTipText(Messages.App_Action_ResetConfig_Tooltip);
				_btnReset.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectConfig_Default(e);
					}
				});
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.align(SWT.END, SWT.CENTER)
						.applyTo(_btnReset);
			}
		}
	}

	private void createUI_20_Layout(final Composite parent) {

		// label: symbol
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Slideout_CalendarOptions_Label_RowHeight);
		label.setToolTipText(Messages.Slideout_CalendarOptions_Label_RowHeight_Tooltip);

		// size
		_spinnerWeekHeight = new Spinner(parent, SWT.BORDER);
		_spinnerWeekHeight.setMinimum(CalendarConfigManager.CELL_HEIGHT_MIN);
		_spinnerWeekHeight.setMaximum(CalendarConfigManager.CELL_HEIGHT_MAX);
		_spinnerWeekHeight.setIncrement(1);
		_spinnerWeekHeight.setPageIncrement(10);
		_spinnerWeekHeight.addSelectionListener(_defaultSelectionListener);
		_spinnerWeekHeight.addMouseWheelListener(_defaultMouseWheelListener);

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
			_comboTour_Value_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Value_1.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_1_1 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_1_1.setVisibleItemCount(20);
			_comboTour_Format_1_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_1_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_1_2 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_1_2.setVisibleItemCount(20);
			_comboTour_Format_1_2.addSelectionListener(_defaultSelectionListener);
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
			_comboTour_Value_2.addSelectionListener(_defaultSelectionListener);
			_comboTour_Value_2.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_2_1 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_2_1.setVisibleItemCount(20);
			_comboTour_Format_2_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_2_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_2_2 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_2_2.setVisibleItemCount(20);
			_comboTour_Format_2_2.addSelectionListener(_defaultSelectionListener);
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
			_comboTour_Value_3.addSelectionListener(_defaultSelectionListener);
			_comboTour_Value_3.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_3_1 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_3_1.setVisibleItemCount(20);
			_comboTour_Format_3_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_3_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_3_2 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_3_2.setVisibleItemCount(20);
			_comboTour_Format_3_2.addSelectionListener(_defaultSelectionListener);
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
			_comboWeek_Format_1.addSelectionListener(_defaultSelectionListener);
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
			_comboWeek_Format_2.addSelectionListener(_defaultSelectionListener);
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
			_comboWeek_Format_3.addSelectionListener(_defaultSelectionListener);
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
			_comboWeek_Format_4.addSelectionListener(_defaultSelectionListener);
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
			_comboWeek_Format_5.addSelectionListener(_defaultSelectionListener);
			_comboWeek_Format_5.addFocusListener(_keepOpenListener);
		}
	}

	private void createUI_999_ConfigName(final Composite parent) {

		/*
		 * Name
		 */
		{
			/*
			 * Label
			 */
			final Label lable = new Label(parent, SWT.NONE);
			lable.setText(Messages.Slideout_Map25MarkerOptions_Label_Name);
			lable.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_Name_Tooltip);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(lable);

			/*
			 * Text
			 */
			_textConfigName = new Text(parent, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_textConfigName);
			_textConfigName.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					onModifyName();
				}
			});
		}
	}

	private void enableControls() {
		// TODO Auto-generated method stub

	}

	private void fillUI() {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{

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
		_isUpdateUI = backupIsUpdateUI;
	}
	private void fillUI_Config() {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{
			_comboConfigName.removeAll();

			for (final CalendarConfig config : CalendarConfigManager.getAllCalendarConfigs()) {
				_comboConfigName.add(config.name);
			}
		}
		_isUpdateUI = backupIsUpdateUI;
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

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyConfig();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onModifyConfig();
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

	private void onChange_WeekValue(final Widget widget) {

		fillWeekFormats(widget, _comboWeek_Value_1, _comboWeek_Format_1);
		fillWeekFormats(widget, _comboWeek_Value_2, _comboWeek_Format_2);
		fillWeekFormats(widget, _comboWeek_Value_3, _comboWeek_Format_3);
		fillWeekFormats(widget, _comboWeek_Value_4, _comboWeek_Format_4);
		fillWeekFormats(widget, _comboWeek_Value_5, _comboWeek_Format_5);
	}

	private void onModifyConfig() {

		saveState();

		enableControls();

		updateUI();
	}

	private void onModifyName() {

		if (_isUpdateUI) {
			return;
		}

		// update text in the combo
		final int selectedIndex = _comboConfigName.getSelectionIndex();

		_comboConfigName.setItem(selectedIndex, _textConfigName.getText());

		saveState();
	}

	private void onSelectConfig() {

		final int selectedIndex = _comboConfigName.getSelectionIndex();
		final ArrayList<CalendarConfig> allConfigurations = CalendarConfigManager.getAllCalendarConfigs();

		final CalendarConfig selectedConfig = allConfigurations.get(selectedIndex);
		final CalendarConfig activeConfig = CalendarConfigManager.getActiveCalendarConfig();

		if (selectedConfig.equals(activeConfig)) {

			// config has not changed
			return;
		}

		// keep data from previous config
		saveState();

		CalendarConfigManager.setActiveCalendarConfig(selectedConfig);

		restoreState();

		enableControls();

		updateUI();
	}

	private void onSelectConfig_Default(final SelectionEvent selectionEvent) {

		if (Util.isCtrlKeyPressed(selectionEvent)) {

			// reset All configurations

			CalendarConfigManager.resetAllCalendarConfigurations();

			fillUI_Config();

		} else {

			// reset active config

			CalendarConfigManager.resetActiveCalendarConfiguration();
		}

		restoreState();
		enableControls();

		updateUI();
	}

	private void restoreState() {

		_isUpdateUI = true;
		{
			final CalendarConfig config = CalendarConfigManager.getActiveCalendarConfig();

			// get active config AFTER getting the index because this could change the active config
			final int activeConfigIndex = CalendarConfigManager.getActiveCalendarConfigIndex();

			_comboConfigName.select(activeConfigIndex);
			_textConfigName.setText(config.name);

			_spinnerWeekHeight.setSelection(config.weekHeight);
		}
		_isUpdateUI = false;
	}

	private void saveState() {

		// update config

		final CalendarConfig config = CalendarConfigManager.getActiveCalendarConfig();

		config.name = _textConfigName.getText();

		config.weekHeight = _spinnerWeekHeight.getSelection();
	}


	private void updateUI() {

//		_map25View.getMapApp().onModifyMarkerConfig();
	}

}
