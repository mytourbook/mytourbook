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
import net.tourbook.common.font.IFontEditorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.font.SimpleFontEditor;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.Util;
import net.tourbook.ui.views.calendar.CalendarConfigManager.DateColumnData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.DayHeaderDateFormatData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.DayHeaderLayoutData;
import net.tourbook.ui.views.calendar.CalendarView.TourInfoFormatter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
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
import org.eclipse.swt.graphics.FontData;
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
	private int						_subItemIndent;

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

	private Button					_chkIsShowDateColumn;
	private Button					_chkIsShowDayHeader;
	private Button					_chkIsShowDayHeaderBold;
	private Button					_chkIsShowSummaryColumn;

	private Combo					_comboConfigName;
	private Combo					_comboDateColumn;
	private Combo					_comboDayHeaderDateFormat;
	private Combo					_comboDayHeaderLayout;

	private Label					_lblDateColumnContent;
	private Label					_lblDateColumnFont;
	private Label					_lblDateColumnWidth;
	private Label					_lblDayHeaderFormat;
	private Label					_lblDayHeaderLayout;
	private Label					_lblSummaryColumnWidth;

	private Spinner					_spinnerDateColumnWidth;
	private Spinner					_spinnerSummaryColumnWidth;
	private Spinner					_spinnerWeekHeight;

	private Text					_textConfigName;

	private SimpleFontEditor		_fontEditorDateColumn;

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

				createUI_200_InfoSummary(container);
				createUI_400_Day(container);
				createUI_500_WeekSummary(container);
				createUI_800_WeekLayout(container);

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

	private void createUI_200_InfoSummary(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(2)
				//				.spacing(20, LayoutConstants.getSpacing().y)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			createUI_210_DateColumn(container);
			createUI_220_SummaryColumn(container);
		}
	}

	private void createUI_210_DateColumn(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_DateColumn);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			/*
			 * Info column
			 */
			// checkbox
			_chkIsShowDateColumn = new Button(group, SWT.CHECK);
			_chkIsShowDateColumn.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowDateColumn);
			_chkIsShowDateColumn.setToolTipText(
					Messages.Slideout_CalendarOptions_Checkbox_IsShowDateColumn_Tooltip);
			_chkIsShowDateColumn.addSelectionListener(_defaultSelectionListener);
			GridDataFactory
					.fillDefaults()//
					.span(2, 1)
					.applyTo(_chkIsShowDateColumn);

			{
				/*
				 * Column width
				 */

				// label
				_lblDateColumnWidth = new Label(group, SWT.NONE);
				_lblDateColumnWidth.setText(Messages.Slideout_CalendarOptions_Label_DateColumn_Width);
				_lblDateColumnWidth.setToolTipText(
						Messages.Slideout_CalendarOptions_Label_DateColumn_Width_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblDateColumnWidth);

				// size
				_spinnerDateColumnWidth = new Spinner(group, SWT.BORDER);
				_spinnerDateColumnWidth.setMinimum(1);
				_spinnerDateColumnWidth.setMaximum(50);
				_spinnerDateColumnWidth.setIncrement(1);
				_spinnerDateColumnWidth.setPageIncrement(10);
				_spinnerDateColumnWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerDateColumnWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Date content
				 */

				// label
				_lblDateColumnContent = new Label(group, SWT.NONE);
				_lblDateColumnContent.setText(Messages.Slideout_CalendarOptions_Label_DateColumnContent);
				_lblDateColumnContent.setToolTipText(
						Messages.Slideout_CalendarOptions_Label_DateColumnContent_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblDateColumnContent);

				// value
				_comboDateColumn = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboDateColumn.setVisibleItemCount(20);
				_comboDateColumn.addSelectionListener(_defaultSelectionListener);
				_comboDateColumn.addFocusListener(_keepOpenListener);
			}
			{
				/*
				 * Font
				 */

				// label
				_lblDateColumnFont = new Label(group, SWT.NONE);
				_lblDateColumnFont.setText(Messages.Slideout_CalendarOptions_Label_DateColumnFont);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblDateColumnFont);

				// value
				_fontEditorDateColumn = new SimpleFontEditor(group, SWT.NONE);
				_fontEditorDateColumn.addFontListener(new IFontEditorListener() {

					@Override
					public void fontDialogOpened(final boolean isDialogOpened) {
						setIsAnotherDialogOpened(isDialogOpened);
					}

					@Override
					public void fontSelected(final FontData font) {
						onModifyConfig();
					}
				});
//				GridDataFactory
//						.fillDefaults()//
//						.align(SWT.FILL, SWT.CENTER)
////						.indent(_subItemIndent, 0)
//						.applyTo(_fontEditorDateColumn);
			}
		}
	}

	private void createUI_220_SummaryColumn(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_SummaryColumn);
		GridDataFactory
				.fillDefaults()//
				//				.align(SWT.FILL, SWT.FILL)
				.grab(true, true)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				/*
				 * Summary column
				 */

				// checkbox
				_chkIsShowSummaryColumn = new Button(group, SWT.CHECK);
				_chkIsShowSummaryColumn.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowSummaryColumn);
				_chkIsShowSummaryColumn.setToolTipText(
						Messages.Slideout_CalendarOptions_Checkbox_IsShowSummaryColumn_Tooltip);
				_chkIsShowSummaryColumn.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkIsShowSummaryColumn);
			}
			{
				/*
				 * Column width
				 */

				// label
				_lblSummaryColumnWidth = new Label(group, SWT.NONE);
				_lblSummaryColumnWidth.setText(Messages.Slideout_CalendarOptions_Label_SummaryColumn_Width);
				_lblSummaryColumnWidth.setToolTipText(
						Messages.Slideout_CalendarOptions_Label_SummaryColumn_Width_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblSummaryColumnWidth);

				// size
				_spinnerSummaryColumnWidth = new Spinner(group, SWT.BORDER);
				_spinnerSummaryColumnWidth.setMinimum(1);
				_spinnerSummaryColumnWidth.setMaximum(50);
				_spinnerSummaryColumnWidth.setIncrement(1);
				_spinnerSummaryColumnWidth.setPageIncrement(10);
				_spinnerSummaryColumnWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerSummaryColumnWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void createUI_400_Day(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_Day);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			createUI_410_DayHeader(group);
			createUI_450_TourInfo(group);
		}
	}

	private void createUI_410_DayHeader(final Composite parent) {

		{
			/*
			 * Day header
			 */

			// checkbox
			_chkIsShowDayHeader = new Button(parent, SWT.CHECK);
			_chkIsShowDayHeader.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowDayHeader);
			_chkIsShowDayHeader.addSelectionListener(_defaultSelectionListener);
			GridDataFactory
					.fillDefaults()//
					.align(SWT.FILL, SWT.BEGINNING)
					.span(2, 1)
					.applyTo(_chkIsShowDayHeader);
		}

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(2)
				.spacing(20, LayoutConstants.getSpacing().y)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			createUI_412_1st_Column(container);
			createUI_412_2nd_Column(container);
		}
	}

	private void createUI_412_1st_Column(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			{
				/*
				 * Day date format
				 */

				// label
				_lblDayHeaderFormat = new Label(container, SWT.NONE);
				_lblDayHeaderFormat.setText(Messages.Slideout_CalendarOptions_Label_DayHeaderFormat);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblDayHeaderFormat);

				// value
				_comboDayHeaderDateFormat = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboDayHeaderDateFormat.setVisibleItemCount(20);
				_comboDayHeaderDateFormat.addSelectionListener(_defaultSelectionListener);
				_comboDayHeaderDateFormat.addFocusListener(_keepOpenListener);
			}
			{
				/*
				 * Layout
				 */

				// label
				_lblDayHeaderLayout = new Label(container, SWT.NONE);
				_lblDayHeaderLayout.setText(Messages.Slideout_CalendarOptions_Label_DayHeaderLayout);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblDayHeaderLayout);

				// value
				_comboDayHeaderLayout = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboDayHeaderLayout.setVisibleItemCount(20);
				_comboDayHeaderLayout.addSelectionListener(_defaultSelectionListener);
				_comboDayHeaderLayout.addFocusListener(_keepOpenListener);
			}
		}
	}

	private void createUI_412_2nd_Column(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Bold font
				 */

				// checkbox
				_chkIsShowDayHeaderBold = new Button(container, SWT.CHECK);
				_chkIsShowDayHeaderBold.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowDayHeaderBold);
				_chkIsShowDayHeaderBold.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						//					.indent(_subItemIndent, 0)
						.span(2, 1)
						.applyTo(_chkIsShowDayHeaderBold);
			}
		}
	}

	private void createUI_450_TourInfo(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.span(2, 1)
				.indent(0, 10)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
		{
			/*
			 * 1. Line
			 */

			// label
			final Label label = new Label(container, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_N_Line, 1));

			// value
			_comboTour_Value_1 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Value_1.setVisibleItemCount(20);
			_comboTour_Value_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Value_1.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_1_1 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_1_1.setVisibleItemCount(20);
			_comboTour_Format_1_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_1_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_1_2 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_1_2.setVisibleItemCount(20);
			_comboTour_Format_1_2.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_1_2.addFocusListener(_keepOpenListener);
		}
		{
			/*
			 * 2. Line
			 */
			final Label label = new Label(container, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_N_Line, 2));

			_comboTour_Value_2 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Value_2.setVisibleItemCount(20);
			_comboTour_Value_2.addSelectionListener(_defaultSelectionListener);
			_comboTour_Value_2.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_2_1 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_2_1.setVisibleItemCount(20);
			_comboTour_Format_2_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_2_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_2_2 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_2_2.setVisibleItemCount(20);
			_comboTour_Format_2_2.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_2_2.addFocusListener(_keepOpenListener);
		}
		{
			/*
			 * 3. Line
			 */
			final Label label = new Label(container, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_N_Line, 3));

			_comboTour_Value_3 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Value_3.setVisibleItemCount(20);
			_comboTour_Value_3.addSelectionListener(_defaultSelectionListener);
			_comboTour_Value_3.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_3_1 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_3_1.setVisibleItemCount(20);
			_comboTour_Format_3_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_3_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_3_2 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_3_2.setVisibleItemCount(20);
			_comboTour_Format_3_2.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_3_2.addFocusListener(_keepOpenListener);
		}
	}

	private void createUI_500_WeekSummary(final Composite parent) {

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

	private void createUI_800_WeekLayout(final Composite parent) {

		{
			// label: symbol
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Slideout_CalendarOptions_Label_RowHeight);
			label.setToolTipText(Messages.Slideout_CalendarOptions_Label_RowHeight_Tooltip);

			// size
			_spinnerWeekHeight = new Spinner(parent, SWT.BORDER);
			_spinnerWeekHeight.setMinimum(CalendarConfigManager.WEEK_HEIGHT_MIN);
			_spinnerWeekHeight.setMaximum(CalendarConfigManager.WEEK_HEIGHT_MAX);
			_spinnerWeekHeight.setIncrement(1);
			_spinnerWeekHeight.setPageIncrement(10);
			_spinnerWeekHeight.addSelectionListener(_defaultSelectionListener);
			_spinnerWeekHeight.addMouseWheelListener(_defaultMouseWheelListener);
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
			lable.setText(Messages.Slideout_CalendarOptions_Label_Name);
			lable.setToolTipText(Messages.Slideout_CalendarOptions_Label_Name_Tooltip);
			// Name for the currently selected tour marker configuration

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

		final boolean isShowInfoColumn = _chkIsShowDateColumn.getSelection();
		final boolean isShowSummaryColumn = _chkIsShowSummaryColumn.getSelection();
		final boolean isShowDayHeader = _chkIsShowDayHeader.getSelection();

		// date column
		_comboDateColumn.setEnabled(isShowInfoColumn);
		_lblDateColumnContent.setEnabled(isShowInfoColumn);
		_lblDateColumnWidth.setEnabled(isShowInfoColumn);
		_spinnerDateColumnWidth.setEnabled(isShowInfoColumn);

		// summary column
		_lblSummaryColumnWidth.setEnabled(isShowSummaryColumn);
		_spinnerSummaryColumnWidth.setEnabled(isShowSummaryColumn);

		// day
		_chkIsShowDayHeaderBold.setEnabled(isShowDayHeader);
		_comboDayHeaderDateFormat.setEnabled(isShowDayHeader);
		_comboDayHeaderLayout.setEnabled(isShowDayHeader);
		_lblDayHeaderFormat.setEnabled(isShowDayHeader);
		_lblDayHeaderLayout.setEnabled(isShowDayHeader);
	}

	private void fillUI() {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{

			/*
			 * Fill Combos
			 */

			for (final DateColumnData data : CalendarConfigManager.getAllDateColumnData()) {
				_comboDateColumn.add(data.label);
			}

			for (final DayHeaderDateFormatData data : CalendarConfigManager.getAllDayHeaderDateFormatData()) {
				_comboDayHeaderDateFormat.add(data.label);
			}

			for (final DayHeaderLayoutData data : CalendarConfigManager.getAllDayHeaderLayoutData()) {
				_comboDayHeaderLayout.add(data.label);
			}

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

//			final String formatName_HH = ColumnManager.getValueFormatterName(ValueFormat.TIME_HH);
//			final String formatName_HH_MM = ColumnManager.getValueFormatterName(ValueFormat.TIME_HH_MM);
//			final String formatName_HH_MM_SS = ColumnManager.getValueFormatterName(ValueFormat.TIME_HH_MM_SS);
//
//			final String formatName_1_0 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_0);
//			final String formatName_1_1 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_1);
//			final String formatName_1_2 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_2);
//			final String formatName_1_3 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_3);

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

	private int getDateColumnIndex(final DateColumnContent infoColumn) {

		final DateColumnData[] allInfoColumnData = CalendarConfigManager.getAllDateColumnData();

		for (int dataIndex = 0; dataIndex < allInfoColumnData.length; dataIndex++) {

			final DateColumnData infoColumnData = allInfoColumnData[dataIndex];

			if (infoColumnData.dateColumn.equals(infoColumn)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getDayHeaderDateFormatIndex(final DayHeaderDateFormat requestedData) {

		final DayHeaderDateFormatData[] allData = CalendarConfigManager.getAllDayHeaderDateFormatData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final DayHeaderDateFormatData Data = allData[dataIndex];

			if (Data.dayHeaderDateFormat.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getDayHeaderLayoutIndex(final DayHeaderLayout requestedData) {

		final DayHeaderLayoutData[] allData = CalendarConfigManager.getAllDayHeaderLayoutData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final DayHeaderLayoutData Data = allData[dataIndex];

			if (Data.dayHeaderLayout.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private DateColumnContent getSelectedDateColumn() {

		final int selectedIndex = _comboDateColumn.getSelectionIndex();

		if (selectedIndex < 0) {
			return DateColumnContent.WEEK_NUMBER;
		}

		final DateColumnData selectedInfoColumnData = CalendarConfigManager.getAllDateColumnData()[selectedIndex];

		return selectedInfoColumnData.dateColumn;
	}

	private DayHeaderDateFormat getSelectedDayHeaderFormat() {

		final int selectedIndex = _comboDayHeaderDateFormat.getSelectionIndex();

		if (selectedIndex < 0) {
			return DayHeaderDateFormat.AUTOMATIC;
		}

		final DayHeaderDateFormatData selectedData = CalendarConfigManager
				.getAllDayHeaderDateFormatData()[selectedIndex];

		return selectedData.dayHeaderDateFormat;
	}

	private DayHeaderLayout getSelectedDayHeaderLayout() {

		final int selectedIndex = _comboDayHeaderLayout.getSelectionIndex();

		if (selectedIndex < 0) {
			return DayHeaderLayout.WEEK_NUMBER;
		}

		final DayHeaderLayoutData selectedData = CalendarConfigManager.getAllDayHeaderLayoutData()[selectedIndex];

		return selectedData.dayHeaderLayout;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
		_subItemIndent = _pc.convertHorizontalDLUsToPixels(12);

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

		updateUI();
	}

	private void restoreState() {

		_isUpdateUI = true;
		{
			final CalendarConfig config = CalendarConfigManager.getActiveCalendarConfig();

			// get active config AFTER getting the index because this could change the active config
			final int activeConfigIndex = CalendarConfigManager.getActiveCalendarConfigIndex();

			// config
			_comboConfigName.select(activeConfigIndex);
			_textConfigName.setText(config.name);

			// day header
			_chkIsShowDayHeader.setSelection(config.isShowDayHeader);
			_chkIsShowDayHeaderBold.setSelection(config.isShowDayHeaderBold);
			_comboDayHeaderDateFormat.select(getDayHeaderDateFormatIndex(config.dayHeaderFormat));
			_comboDayHeaderLayout.select(getDayHeaderLayoutIndex(config.dayHeaderLayout));

			// date column
			_chkIsShowDateColumn.setSelection(config.isShowDateColumn);
			_spinnerDateColumnWidth.setSelection(config.dateColumnWidth);
			_comboDateColumn.select(getDateColumnIndex(config.dateColumnContent));
			_fontEditorDateColumn.setSelection(config.dateColumnFont);

			// summary column
			_chkIsShowSummaryColumn.setSelection(config.isShowSummaryColumn);
			_spinnerSummaryColumnWidth.setSelection(config.summaryColumnWidth);

			// layout
			_spinnerWeekHeight.setSelection(config.weekHeight);
		}
		_isUpdateUI = false;

		enableControls();
	}

	private void saveState() {

		// update config

		final CalendarConfig config = CalendarConfigManager.getActiveCalendarConfig();

		// config
		config.name = _textConfigName.getText();

		// day header
		config.isShowDayHeader = _chkIsShowDayHeader.getSelection();
		config.isShowDayHeaderBold = _chkIsShowDayHeaderBold.getSelection();
		config.dayHeaderFormat = getSelectedDayHeaderFormat();
		config.dayHeaderLayout = getSelectedDayHeaderLayout();

		// date column
		config.isShowDateColumn = _chkIsShowDateColumn.getSelection();
		config.dateColumnContent = getSelectedDateColumn();
		config.dateColumnWidth = _spinnerDateColumnWidth.getSelection();
		config.dateColumnFont = _fontEditorDateColumn.getSelection();

		// summary column
		config.isShowSummaryColumn = _chkIsShowSummaryColumn.getSelection();
		config.summaryColumnWidth = _spinnerSummaryColumnWidth.getSelection();

		// layout
		config.weekHeight = _spinnerWeekHeight.getSelection();

		_calendarView.updateUI_CalendarConfig();
	}

	private void updateUI() {

		_calendarView.updateUI_Layout();
	}

}
