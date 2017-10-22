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
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.IFontEditorListener;
import net.tourbook.common.font.SimpleFontEditor;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.Util;
import net.tourbook.ui.views.calendar.CalendarConfigManager.CalendarColorData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.ColumnLayoutData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.DateColumnData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.DayContentColorData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.DayHeaderDateFormatData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.ICalendarConfigProvider;
import net.tourbook.ui.views.calendar.CalendarConfigManager.TourBackgroundData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.TourBorderData;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Properties slideout for the calendar view.
 */
public class SlideoutCalendarOptions extends AdvancedSlideout implements ICalendarConfigProvider,
		IColorSelectorListener {

//	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private IFontEditorListener		_defaultFontEditorListener;
	private MouseWheelListener		_defaultMouseWheelListener;
	private IPropertyChangeListener	_defaultPropertyChangeListener;
	private SelectionAdapter		_defaultSelectionListener;
	private FocusListener			_keepOpenListener;
	private SelectionAdapter		_weekValueListener;

//	private ActionOpenPrefDialog	_actionPrefDialog;
//	private Action					_actionRestoreDefaults;

	private boolean					_isUpdateUI;

	private PixelConverter			_pc;
	private int						_subItemIndent;

	/*
	 * This is a hack to vertical center the font label, otherwise it will be complicated to set it
	 * correctly
	 */
	private int						_fontLabelVIndent	= 5;

	/*
	 * UI controls
	 */
	private CalendarView			_calendarView;

	private Button					_btnReset;

	private ColorSelectorExtended	_colorAlternateMonthColor;

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

	private Button					_chkIsHideDayDateWhenNoTour;
	private Button					_chkIsShowDateColumn;
	private Button					_chkIsShowDayDateWeekendColor;
	private Button					_chkIsShowDayDate;
	private Button					_chkIsShowSummaryColumn;
	private Button					_chkIsShowMonthWithAlternateColor;
	private Button					_chkIsShowYearColumns;

	private Combo					_comboColumnLayout;
	private Combo					_comboConfigName;
	private Combo					_comboDateColumn;
	private Combo					_comboDayContentColor;
	private Combo					_comboDayHeaderDateFormat;
	private Combo					_comboTourBackground;
	private Combo					_comboTourBackgroundColor1;
	private Combo					_comboTourBackgroundColor2;
	private Combo					_comboTourBorder;
	private Combo					_comboTourBorderColor;

	private Label					_lblDateColumnContent;
	private Label					_lblDateColumnFont;
	private Label					_lblDateColumnWidth;
	private Label					_lblDayHeaderFont;
	private Label					_lblDayHeaderFormat;
	private Label					_lblNumYearColumn;
	private Label					_lblSummaryColumnWidth;
	private Label					_lblYearColumnHeaderFont;
	private Label					_lblYearColumnSpacing;
	private Label					_lblYearColumnStart;

	private SimpleFontEditor		_fontEditorDayContent;
	private SimpleFontEditor		_fontEditorDayDate;
	private SimpleFontEditor		_fontEditorDateColumn;
	private SimpleFontEditor		_fontEditorYearColumnHeader;

	private Spinner					_spinnerNumYearColumns;
	private Spinner					_spinnerYearColumnSpacing;
	private Spinner					_spinnerDateColumnWidth;
	private Spinner					_spinnerSummaryColumnWidth;
	private Spinner					_spinnerTourBackgroundWidth;
	private Spinner					_spinnerTourBorderWidth;
	private Spinner					_spinnerWeekHeight;

	private Text					_textConfigName;

	private ToolItem				_toolItem;

	/**
	 * @param ownerControl
	 * @param toolItem
	 * @param state
	 * @param calendarView
	 * @param gridPrefPrefix
	 */
	public SlideoutCalendarOptions(	final ToolItem toolItem,
									final IDialogSettings state,
									final CalendarView calendarView) {

		super(toolItem.getParent(), state, null);

		_calendarView = calendarView;
		_toolItem = toolItem;

		setTitleText(Messages.Slideout_CalendarOptions_Label_Title);
		setSlideoutLocation(SlideoutLocation.BELOW_RIGHT);
	}

	@Override
	public void colorDialogOpened(final boolean isDialogOpened) {

		setIsAnotherDialogOpened(isDialogOpened);
	}

	private void createActions() {

	}

	@Override
	protected void createSlideoutContent(final Composite parent) {

		createActions();
		createUI(parent);

		fillUI();
		fillUI_Config();

		restoreState();
	}

	@Override
	protected void createTitleBarControls(final Composite parent) {

		// this method is called 1st

		initUI(parent);

		{
			/*
			 * Combo: Configuration
			 */
			_comboConfigName = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
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
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
					.indent(20, 0)
					.hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
					.applyTo(_comboConfigName);
		}
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory
					.fillDefaults()//
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_200_DateColumn(container);
				createUI_300_DayDate(container);
				createUI_400_DayContent(container);
				createUI_500_SummaryColumn(container);

				createUI_800_YearColumns(container);
				createUI_900_Layout(container);
				createUI_999_Configuration(container);
			}
		}

		return shellContainer;
	}

	private void createUI_200_DateColumn(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_DateColumn);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Date column
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
			}

			final Composite container = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				{
					/*
					 * Column width
					 */

					// label
					_lblDateColumnWidth = new Label(container, SWT.NONE);
					_lblDateColumnWidth.setText(Messages.Slideout_CalendarOptions_Label_DateColumn_Width);
					_lblDateColumnWidth.setToolTipText(
							Messages.Slideout_CalendarOptions_Label_DateColumn_Width_Tooltip);
					GridDataFactory
							.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.indent(_subItemIndent, 0)
							.applyTo(_lblDateColumnWidth);

					// size
					_spinnerDateColumnWidth = new Spinner(container, SWT.BORDER);
					_spinnerDateColumnWidth.setMinimum(1);
					_spinnerDateColumnWidth.setMaximum(200);
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
					_lblDateColumnContent = new Label(container, SWT.NONE);
					_lblDateColumnContent.setText(Messages.Slideout_CalendarOptions_Label_DateColumnContent);
					_lblDateColumnContent.setToolTipText(
							Messages.Slideout_CalendarOptions_Label_DateColumnContent_Tooltip);
					GridDataFactory
							.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.indent(_subItemIndent, 0)
							.applyTo(_lblDateColumnContent);

					// value
					_comboDateColumn = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
					_comboDateColumn.setVisibleItemCount(20);
					_comboDateColumn.addSelectionListener(_defaultSelectionListener);
					_comboDateColumn.addFocusListener(_keepOpenListener);
				}
			}

			final Composite containerRight = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(containerRight);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerRight);
//			containerRight.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				{
					/*
					 * Font
					 */

					// label
					_lblDateColumnFont = new Label(containerRight, SWT.NONE);
					_lblDateColumnFont.setText(Messages.Slideout_CalendarOptions_Label_DateColumnFont);
					GridDataFactory
							.fillDefaults()//
							.indent(0, _fontLabelVIndent)
							.applyTo(_lblDateColumnFont);

					// value
					_fontEditorDateColumn = new SimpleFontEditor(containerRight, SWT.NONE);
					_fontEditorDateColumn.addFontListener(_defaultFontEditorListener);
					GridDataFactory.fillDefaults().grab(true, true).applyTo(_fontEditorDateColumn);
				}
			}
		}
	}

	private void createUI_300_DayDate(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_DayDate);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			{
				/*
				 * Day date
				 */

				// checkbox
				_chkIsShowDayDate = new Button(group, SWT.CHECK);
				_chkIsShowDayDate.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowDayDate);
				_chkIsShowDayDate.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkIsShowDayDate);
			}
			{
				final Composite container = new Composite(group, SWT.NONE);
//				container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.span(2, 1)
						.applyTo(container);
				GridLayoutFactory
						.fillDefaults()//
						.numColumns(2)
						.spacing(10, LayoutConstants.getSpacing().y)
						.applyTo(container);
				{
					createUI_320_Col1(container);
					createUI_330_Col2(container);
				}
			}
		}
	}

	private void createUI_320_Col1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
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
				 * Hide day when empty
				 */

				// checkbox
				_chkIsHideDayDateWhenNoTour = new Button(container, SWT.CHECK);
				_chkIsHideDayDateWhenNoTour.setText(Messages.Slideout_CalendarOptions_Checkbox_IsHideDayWhenEmpty);
				_chkIsHideDayDateWhenNoTour.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.indent(_subItemIndent, 0)
						.span(2, 1)
						.applyTo(_chkIsHideDayDateWhenNoTour);
			}
			{
				/*
				 * Show weekend color
				 */

				// checkbox
				_chkIsShowDayDateWeekendColor = new Button(container, SWT.CHECK);
				_chkIsShowDayDateWeekendColor.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowDayWeekendColor);
				_chkIsShowDayDateWeekendColor.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.indent(_subItemIndent, 0)
						.span(2, 1)
						.applyTo(_chkIsShowDayDateWeekendColor);
			}
		}
	}

	private void createUI_330_Col2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		{
			{
				/*
				 * Font
				 */

				// label
				_lblDayHeaderFont = new Label(container, SWT.NONE);
				_lblDayHeaderFont.setText(Messages.Slideout_CalendarOptions_Label_DayDateFont);
				GridDataFactory
						.fillDefaults()//
						.grab(true, true)
						.indent(0, _fontLabelVIndent)
						.applyTo(_lblDayHeaderFont);

				// value
				_fontEditorDayDate = new SimpleFontEditor(container, SWT.NONE);
				_fontEditorDayDate.addFontListener(_defaultFontEditorListener);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_fontEditorDayDate);
			}
		}
	}

	private void createUI_400_DayContent(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_DayContent);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(5).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		{
			{
				/*
				 * Tour background
				 */

				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_TourBackground);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);

				// value
				_comboTourBackground = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTourBackground.setVisibleItemCount(20);
				_comboTourBackground.addSelectionListener(_defaultSelectionListener);
				_comboTourBackground.addFocusListener(_keepOpenListener);

				// combo color 1
				_comboTourBackgroundColor1 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTourBackgroundColor1.setVisibleItemCount(20);
				_comboTourBackgroundColor1.addSelectionListener(_defaultSelectionListener);

				// combo color 2
				_comboTourBackgroundColor2 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTourBackgroundColor2.setVisibleItemCount(20);
				_comboTourBackgroundColor2.addSelectionListener(_defaultSelectionListener);

				// background width
				_spinnerTourBackgroundWidth = new Spinner(group, SWT.BORDER);
				_spinnerTourBackgroundWidth.setMinimum(0);
				_spinnerTourBackgroundWidth.setMaximum(100);
				_spinnerTourBackgroundWidth.setIncrement(1);
				_spinnerTourBackgroundWidth.setPageIncrement(10);
				_spinnerTourBackgroundWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerTourBackgroundWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Tour border
				 */

				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_TourBorder);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);

				// value
				_comboTourBorder = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTourBorder.setVisibleItemCount(20);
				_comboTourBorder.addSelectionListener(_defaultSelectionListener);
				_comboTourBorder.addFocusListener(_keepOpenListener);

				// combo color
				_comboTourBorderColor = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTourBorderColor.setVisibleItemCount(20);
				_comboTourBorderColor.addSelectionListener(_defaultSelectionListener);

				// spacer
				new Label(group, SWT.NONE);

				// border width
				_spinnerTourBorderWidth = new Spinner(group, SWT.BORDER);
				_spinnerTourBorderWidth.setMinimum(0);
				_spinnerTourBorderWidth.setMaximum(100);
				_spinnerTourBorderWidth.setIncrement(1);
				_spinnerTourBorderWidth.setPageIncrement(10);
				_spinnerTourBorderWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerTourBorderWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Font
				 */

				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_DayContentFont);
				GridDataFactory
						.fillDefaults()//
						//						.grab(true, true)
						.indent(0, _fontLabelVIndent)
						.applyTo(label);

				// font/size
				_fontEditorDayContent = new SimpleFontEditor(group, SWT.NONE);
				_fontEditorDayContent.addFontListener(_defaultFontEditorListener);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_fontEditorDayContent);

				// combo color
				_comboDayContentColor = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboDayContentColor.setVisibleItemCount(20);
				_comboDayContentColor.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.span(3, 1)
						.applyTo(_comboDayContentColor);
			}
			{
				/*
				 * Month alternate color
				 */
				// checkbox
				_chkIsShowMonthWithAlternateColor = new Button(group, SWT.CHECK);
				_chkIsShowMonthWithAlternateColor.setText(
						Messages.Slideout_CalendarOptions_Checkbox_IsShowMonthWithAlternatingColor);
				_chkIsShowMonthWithAlternateColor.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.span(4, 1)
						.applyTo(_chkIsShowMonthWithAlternateColor);
			}
			{
				/*
				 * Alternate color
				 */

				// Color selector
				_colorAlternateMonthColor = new ColorSelectorExtended(group);
				GridDataFactory
						.swtDefaults()//
						.grab(false, true)
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.applyTo(_colorAlternateMonthColor.getButton());

				_colorAlternateMonthColor.addOpenListener(this);
				_colorAlternateMonthColor.addListener(_defaultPropertyChangeListener);
			}

		}
	}

	private void createUI_500_SummaryColumn(final Composite parent) {

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
				_spinnerSummaryColumnWidth.setMaximum(200);
				_spinnerSummaryColumnWidth.setIncrement(1);
				_spinnerSummaryColumnWidth.setPageIncrement(10);
				_spinnerSummaryColumnWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerSummaryColumnWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void createUI_800_YearColumns(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_YearColumns);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				.spacing(10, LayoutConstants.getSpacing().y)
				.applyTo(group);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Year columns
				 */

				// checkbox
				_chkIsShowYearColumns = new Button(group, SWT.CHECK);
				_chkIsShowYearColumns.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowYearColumns);
				_chkIsShowYearColumns.setToolTipText(
						Messages.Slideout_CalendarOptions_Checkbox_IsShowYearColumns_Tooltip);
				_chkIsShowYearColumns.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkIsShowYearColumns);
			}
			createUI_810_Col1(group);
			createUI_850_Col2(group);
		}
	}

	private void createUI_810_Col1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Number of year columns
				 */

				// label
				_lblNumYearColumn = new Label(container, SWT.NONE);
				_lblNumYearColumn.setText(Messages.Slideout_CalendarOptions_Label_NumYearColumns);
				_lblNumYearColumn.setToolTipText(Messages.Slideout_CalendarOptions_Label_NumYearColumns_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblNumYearColumn);

				// spinner: columns
				_spinnerNumYearColumns = new Spinner(container, SWT.BORDER);
				_spinnerNumYearColumns.setMinimum(CalendarConfigManager.YEAR_COLUMNS_MIN);
				_spinnerNumYearColumns.setMaximum(CalendarConfigManager.YEAR_COLUMNS_MAX);
				_spinnerNumYearColumns.setIncrement(1);
				_spinnerNumYearColumns.setPageIncrement(2);
				_spinnerNumYearColumns.addSelectionListener(_defaultSelectionListener);
				_spinnerNumYearColumns.addMouseWheelListener(_defaultMouseWheelListener);

			}
			{
				/*
				 * Year columns space
				 */

				// label
				_lblYearColumnSpacing = new Label(container, SWT.NONE);
				_lblYearColumnSpacing.setText(Messages.Slideout_CalendarOptions_Label_YearColumnsSpace);
				_lblYearColumnSpacing.setToolTipText(
						Messages.Slideout_CalendarOptions_Label_YearColumnsSpace_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblYearColumnSpacing);

				// spinner: columns
				_spinnerYearColumnSpacing = new Spinner(container, SWT.BORDER);
				_spinnerYearColumnSpacing.setMinimum(CalendarConfigManager.CALENDAR_COLUMNS_SPACE_MIN);
				_spinnerYearColumnSpacing.setMaximum(CalendarConfigManager.CALENDAR_COLUMNS_SPACE_MAX);
				_spinnerYearColumnSpacing.setIncrement(1);
				_spinnerYearColumnSpacing.setPageIncrement(10);
				_spinnerYearColumnSpacing.addSelectionListener(_defaultSelectionListener);
				_spinnerYearColumnSpacing.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Column start
				 */

				// label
				_lblYearColumnStart = new Label(container, SWT.NONE);
				_lblYearColumnStart.setText(Messages.Slideout_CalendarOptions_Label_YearColumnsStart);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblYearColumnStart);

				// value
				_comboColumnLayout = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboColumnLayout.setVisibleItemCount(20);
				_comboColumnLayout.addSelectionListener(_defaultSelectionListener);
				_comboColumnLayout.addFocusListener(_keepOpenListener);
			}
		}
	}

	private void createUI_850_Col2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Year header
				 */

				// label
				_lblYearColumnHeaderFont = new Label(container, SWT.NONE);
				_lblYearColumnHeaderFont.setText(Messages.Slideout_CalendarOptions_Label_YearHeaderFont);
				GridDataFactory
						.fillDefaults()//
						.indent(0, _fontLabelVIndent)
						.applyTo(_lblYearColumnHeaderFont);

				// font/size
				_fontEditorYearColumnHeader = new SimpleFontEditor(container, SWT.NONE);
				_fontEditorYearColumnHeader.addFontListener(_defaultFontEditorListener);
				GridDataFactory
						.fillDefaults()//
						.grab(true, true)
						.applyTo(_fontEditorYearColumnHeader);
			}
		}
	}

	private void createUI_900_Layout(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_Layout);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				.spacing(10, LayoutConstants.getSpacing().y)
				.applyTo(group);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Week height
				 */
				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_RowHeight);
				label.setToolTipText(Messages.Slideout_CalendarOptions_Label_RowHeight_Tooltip);

				// spinner: height
				_spinnerWeekHeight = new Spinner(group, SWT.BORDER);
				_spinnerWeekHeight.setMinimum(CalendarConfigManager.WEEK_HEIGHT_MIN);
				_spinnerWeekHeight.setMaximum(CalendarConfigManager.WEEK_HEIGHT_MAX);
				_spinnerWeekHeight.setIncrement(1);
				_spinnerWeekHeight.setPageIncrement(10);
				_spinnerWeekHeight.addSelectionListener(_defaultSelectionListener);
				_spinnerWeekHeight.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void createUI_980_TourInfo(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
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

	private void createUI_981_WeekSummary(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_WeekSummary);
		GridDataFactory.fillDefaults().applyTo(group);
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

	private void createUI_999_Configuration(final Composite parent) {

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
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(lable);
		}

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Text
				 */
				_textConfigName = new Text(container, SWT.BORDER);
				_textConfigName.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(final ModifyEvent e) {
						onModifyName();
					}
				});
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.grab(true, false)
						.applyTo(_textConfigName);
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
						//						.grab(true, false)
						.align(SWT.END, SWT.CENTER)
						.applyTo(_btnReset);
			}
		}
	}

	private void enableControls() {

		final boolean isShowDateColumn = _chkIsShowDateColumn.getSelection();
		final boolean isShowSummaryColumn = _chkIsShowSummaryColumn.getSelection();
		final boolean isShowDayDate = _chkIsShowDayDate.getSelection();
		final boolean isYearColumns = _chkIsShowYearColumns.getSelection();

		// date column
		_comboDateColumn.setEnabled(isShowDateColumn);
		_fontEditorDateColumn.setEnabled(isShowDateColumn);
		_lblDateColumnContent.setEnabled(isShowDateColumn);
		_lblDateColumnFont.setEnabled(isShowDateColumn);
		_lblDateColumnWidth.setEnabled(isShowDateColumn);
		_spinnerDateColumnWidth.setEnabled(isShowDateColumn);

		// summary column
		_lblSummaryColumnWidth.setEnabled(isShowSummaryColumn);
		_spinnerSummaryColumnWidth.setEnabled(isShowSummaryColumn);

		// day date
		_chkIsShowDayDateWeekendColor.setEnabled(isShowDayDate);
		_chkIsHideDayDateWhenNoTour.setEnabled(isShowDayDate);
		_comboDayHeaderDateFormat.setEnabled(isShowDayDate);
		_fontEditorDayDate.setEnabled(isShowDayDate);
		_lblDayHeaderFormat.setEnabled(isShowDayDate);
		_lblDayHeaderFont.setEnabled(isShowDayDate);

		// day content
		final TourBackgroundData selectedTourBackgroundData = getSelectedTourBackgroundData();
		final TourBorderData selectedTourBorderData = getSelectedTourBorderData();

		_comboTourBackgroundColor1.setEnabled(selectedTourBackgroundData.isColor1);
		_comboTourBackgroundColor2.setEnabled(selectedTourBackgroundData.isColor2);
		_spinnerTourBackgroundWidth.setEnabled(selectedTourBackgroundData.isWidth);

		_comboTourBorderColor.setEnabled(selectedTourBorderData.isColor);
		_spinnerTourBorderWidth.setEnabled(selectedTourBorderData.isWidth);

		// layout
		_comboColumnLayout.setEnabled(isYearColumns);
		_fontEditorYearColumnHeader.setEnabled(isYearColumns);
		_lblYearColumnHeaderFont.setEnabled(isYearColumns);
		_lblYearColumnSpacing.setEnabled(isYearColumns);
		_lblYearColumnStart.setEnabled(isYearColumns);
		_lblNumYearColumn.setEnabled(isYearColumns);
		_spinnerNumYearColumns.setEnabled(isYearColumns);
		_spinnerYearColumnSpacing.setEnabled(isYearColumns);
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

			for (final ColumnLayoutData data : CalendarConfigManager.getAllColumnLayoutData()) {
				_comboColumnLayout.add(data.label);
			}

			/*
			 * Tour background
			 */
			for (final TourBackgroundData data : CalendarConfigManager.getAllTourBackgroundData()) {
				_comboTourBackground.add(data.label);
			}
			for (final CalendarColorData data : CalendarConfigManager.getAllCalendarColorData()) {
				_comboTourBackgroundColor1.add(data.label);
			}
			for (final CalendarColorData data : CalendarConfigManager.getAllCalendarColorData()) {
				_comboTourBackgroundColor2.add(data.label);
			}

			/*
			 * Tour border
			 */
			for (final TourBorderData data : CalendarConfigManager.getAllTourBorderData()) {
				_comboTourBorder.add(data.label);
			}
			for (final CalendarColorData data : CalendarConfigManager.getAllCalendarColorData()) {
				_comboTourBorderColor.add(data.label);
			}

			for (final DayContentColorData data : CalendarConfigManager.getAllDayContentColorData()) {
				_comboDayContentColor.add(data.label);
			}

//			for (final TourInfoFormatter tourFormatter : _calendarView.tourInfoFormatter) {
//
//				_comboTour_Value_1.add(tourFormatter.getText());
//				_comboTour_Value_2.add(tourFormatter.getText());
//				_comboTour_Value_3.add(tourFormatter.getText());
//			}
//
//			for (final WeekSummaryFormatter weekFormatter : _calendarView.tourWeekSummaryFormatter) {
//
//				_comboWeek_Value_1.add(weekFormatter.getText());
//				_comboWeek_Value_2.add(weekFormatter.getText());
//				_comboWeek_Value_3.add(weekFormatter.getText());
//				_comboWeek_Value_4.add(weekFormatter.getText());
//				_comboWeek_Value_5.add(weekFormatter.getText());
//			}

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

	private int getCalendarColorIndex(final CalendarColor requestedData) {

		final CalendarColorData[] allData = CalendarConfigManager.getAllCalendarColorData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final CalendarColorData data = allData[dataIndex];

			if (data.color.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getCalendarColumLayoutIndex(final ColumnStart requestedData) {

		final ColumnLayoutData[] allData = CalendarConfigManager.getAllColumnLayoutData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final ColumnLayoutData data = allData[dataIndex];

			if (data.columnLayout.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getDateColumnIndex(final DateColumnContent requestedData) {

		final DateColumnData[] allInfoColumnData = CalendarConfigManager.getAllDateColumnData();

		for (int dataIndex = 0; dataIndex < allInfoColumnData.length; dataIndex++) {

			final DateColumnData data = allInfoColumnData[dataIndex];

			if (data.dateColumn.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getDayContentColorIndex(final CalendarColor requestedData) {

		final DayContentColorData[] allData = CalendarConfigManager.getAllDayContentColorData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final DayContentColorData data = allData[dataIndex];

			if (data.dayContentColor.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getDayHeaderDateFormatIndex(final DayDateFormat requestedData) {

		final DayHeaderDateFormatData[] allData = CalendarConfigManager.getAllDayHeaderDateFormatData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final DayHeaderDateFormatData data = allData[dataIndex];

			if (data.dayHeaderDateFormat.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	@Override
	protected Rectangle getParentBounds() {

		final Rectangle itemBounds = _toolItem.getBounds();
		final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

		itemBounds.x = itemDisplayPosition.x;
		itemBounds.y = itemDisplayPosition.y;

		return itemBounds;
	}

	private CalendarColor getSelectedCalendarColor(final Combo comboColor) {

		final int selectedIndex = comboColor.getSelectionIndex();

		final CalendarColorData[] allCalendarColorData = CalendarConfigManager.getAllCalendarColorData();

		if (selectedIndex < 0) {
			return allCalendarColorData[0].color;
		}

		return allCalendarColorData[selectedIndex].color;
	}

	private ColumnStart getSelectedColumnLayout() {

		final int selectedIndex = _comboColumnLayout.getSelectionIndex();

		if (selectedIndex < 0) {
			return ColumnStart.CONTINUOUSLY;
		}

		final ColumnLayoutData data = CalendarConfigManager.getAllColumnLayoutData()[selectedIndex];

		return data.columnLayout;
	}

	private DateColumnContent getSelectedDateColumn() {

		final int selectedIndex = _comboDateColumn.getSelectionIndex();

		if (selectedIndex < 0) {
			return DateColumnContent.WEEK_NUMBER;
		}

		final DateColumnData selectedInfoColumnData = CalendarConfigManager.getAllDateColumnData()[selectedIndex];

		return selectedInfoColumnData.dateColumn;
	}

	private DayContentColorData getSelectedDayContentColor() {

		final int selectedIndex = _comboDayContentColor.getSelectionIndex();

		final DayContentColorData[] allData = CalendarConfigManager.getAllDayContentColorData();

		if (selectedIndex < 0) {

			for (final DayContentColorData data : allData) {

				if (data.dayContentColor == CalendarConfigManager.DEFAULT_DAY_CONTENT_COLOR) {
					return data;
				}
			}

			// return default default
			return allData[0];
		}

		return allData[selectedIndex];
	}

	private DayDateFormat getSelectedDayDateFormat() {

		final int selectedIndex = _comboDayHeaderDateFormat.getSelectionIndex();

		if (selectedIndex < 0) {
			return DayDateFormat.AUTOMATIC;
		}

		final DayHeaderDateFormatData selectedData = CalendarConfigManager
				.getAllDayHeaderDateFormatData()[selectedIndex];

		return selectedData.dayHeaderDateFormat;
	}

	private TourBackgroundData getSelectedTourBackgroundData() {

		final int selectedIndex = _comboTourBackground.getSelectionIndex();

		final TourBackgroundData[] allTourBackgroundData = CalendarConfigManager.getAllTourBackgroundData();

		if (selectedIndex < 0) {

			for (final TourBackgroundData data : allTourBackgroundData) {

				if (data.tourBackground == CalendarConfigManager.DEFAULT_TOUR_BACKGROUND) {
					return data;
				}
			}

			// return default default
			return allTourBackgroundData[0];
		}

		return allTourBackgroundData[selectedIndex];
	}

	private TourBorderData getSelectedTourBorderData() {

		final int selectedIndex = _comboTourBorder.getSelectionIndex();

		final TourBorderData[] allTourBorderData = CalendarConfigManager.getAllTourBorderData();

		if (selectedIndex < 0) {

			for (final TourBorderData data : allTourBorderData) {

				if (data.tourBorder == CalendarConfigManager.DEFAULT_TOUR_BORDER) {
					return data;
				}
			}

			// return default default
			return allTourBorderData[0];
		}

		return allTourBorderData[selectedIndex];
	}

	private int getTourBackgroundIndex(final TourBackground requestedData) {

		final TourBackgroundData[] allData = CalendarConfigManager.getAllTourBackgroundData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final TourBackgroundData data = allData[dataIndex];

			if (data.tourBackground.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getTourBorderIndex(final TourBorder requestedData) {

		final TourBorderData[] allData = CalendarConfigManager.getAllTourBorderData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final TourBorderData data = allData[dataIndex];

			if (data.tourBorder.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
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

		_defaultPropertyChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
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
				setIsKeepOpenInternally(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				setIsKeepOpenInternally(false);
			}
		};

		_defaultFontEditorListener = new IFontEditorListener() {

			@Override
			public void fontDialogOpened(final boolean isDialogOpened) {
				setIsKeepOpenInternally(isDialogOpened);
			}

			@Override
			public void fontSelected(final FontData font) {
				onModifyConfig();
			}
		};

		CalendarConfigManager.setConfigProvider(this);

		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				CalendarConfigManager.setConfigProvider((SlideoutCalendarOptions) null);
			}
		});
	}

	private void onChange_WeekValue(final Widget widget) {

		fillWeekFormats(widget, _comboWeek_Value_1, _comboWeek_Format_1);
		fillWeekFormats(widget, _comboWeek_Value_2, _comboWeek_Format_2);
		fillWeekFormats(widget, _comboWeek_Value_3, _comboWeek_Format_3);
		fillWeekFormats(widget, _comboWeek_Value_4, _comboWeek_Format_4);
		fillWeekFormats(widget, _comboWeek_Value_5, _comboWeek_Format_5);
	}

	@Override
	protected void onFocus() {

		_comboConfigName.setFocus();
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

		CalendarConfigManager.setActiveCalendarConfig(selectedConfig, this);

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

			// day date
			_chkIsHideDayDateWhenNoTour.setSelection(config.isHideDayDateWhenNoTour);
			_chkIsShowDayDate.setSelection(config.isShowDayDate);
			_chkIsShowDayDateWeekendColor.setSelection(config.isShowDayDateWeekendColor);
			_comboDayHeaderDateFormat.select(getDayHeaderDateFormatIndex(config.dayDateFormat));
			_comboDayContentColor.select(getDayContentColorIndex(config.dayContentColor));
			_fontEditorDayDate.setSelection(config.dayDateFont);

			// day content
			_colorAlternateMonthColor.setColorValue(config.alternateMonthRGB);
			_chkIsShowMonthWithAlternateColor.setSelection(config.isToggleMonthColor);
			_comboTourBackground.select(getTourBackgroundIndex(config.tourBackground));
			_comboTourBackgroundColor1.select(getCalendarColorIndex(config.tourBackgroundColor1));
			_comboTourBackgroundColor2.select(getCalendarColorIndex(config.tourBackgroundColor2));
			_comboTourBorder.select(getTourBorderIndex(config.tourBorder));
			_comboTourBorderColor.select(getCalendarColorIndex(config.tourBorderColor));
			_fontEditorDayContent.setSelection(config.dayContentFont);
			_spinnerTourBackgroundWidth.setSelection(config.tourBackgroundWidth);
			_spinnerTourBorderWidth.setSelection(config.tourBorderWidth);

			// date column
			_chkIsShowDateColumn.setSelection(config.isShowDateColumn);
			_spinnerDateColumnWidth.setSelection(config.dateColumnWidth);
			_comboDateColumn.select(getDateColumnIndex(config.dateColumnContent));
			_fontEditorDateColumn.setSelection(config.dateColumnFont);

			// summary column
			_chkIsShowSummaryColumn.setSelection(config.isShowSummaryColumn);
			_spinnerSummaryColumnWidth.setSelection(config.summaryColumnWidth);

			// year columns
			_chkIsShowYearColumns.setSelection(config.isShowYearColumns);
			_comboColumnLayout.select(getCalendarColumLayoutIndex(config.yearColumnsStart));
			_spinnerNumYearColumns.setSelection(config.numYearColumns);
			_spinnerYearColumnSpacing.setSelection(config.yearColumnsSpacing);
			_fontEditorYearColumnHeader.setSelection(config.yearHeaderFont);

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

		// day date
		config.isHideDayDateWhenNoTour = _chkIsHideDayDateWhenNoTour.getSelection();
		config.isShowDayDate = _chkIsShowDayDate.getSelection();
		config.isShowDayDateWeekendColor = _chkIsShowDayDateWeekendColor.getSelection();
		config.dayDateFormat = getSelectedDayDateFormat();
		config.dayDateFont = _fontEditorDayDate.getSelection();

		// day content
		config.alternateMonthRGB = _colorAlternateMonthColor.getColorValue();
		config.dayContentColor = getSelectedDayContentColor().dayContentColor;
		config.dayContentFont = _fontEditorDayContent.getSelection();
		config.isToggleMonthColor = _chkIsShowMonthWithAlternateColor.getSelection();
		config.tourBackground = getSelectedTourBackgroundData().tourBackground;
		config.tourBackgroundColor1 = getSelectedCalendarColor(_comboTourBackgroundColor1);
		config.tourBackgroundColor2 = getSelectedCalendarColor(_comboTourBackgroundColor2);
		config.tourBackgroundWidth = _spinnerTourBackgroundWidth.getSelection();
		config.tourBorder = getSelectedTourBorderData().tourBorder;
		config.tourBorderColor = getSelectedCalendarColor(_comboTourBorderColor);
		config.tourBorderWidth = _spinnerTourBorderWidth.getSelection();

		// date column
		config.isShowDateColumn = _chkIsShowDateColumn.getSelection();
		config.dateColumnContent = getSelectedDateColumn();
		config.dateColumnWidth = _spinnerDateColumnWidth.getSelection();
		config.dateColumnFont = _fontEditorDateColumn.getSelection();

		// summary column
		config.isShowSummaryColumn = _chkIsShowSummaryColumn.getSelection();
		config.summaryColumnWidth = _spinnerSummaryColumnWidth.getSelection();

		// year columns
		config.isShowYearColumns = _chkIsShowYearColumns.getSelection();
		config.numYearColumns = _spinnerNumYearColumns.getSelection();
		config.yearColumnsStart = getSelectedColumnLayout();
		config.yearColumnsSpacing = _spinnerYearColumnSpacing.getSelection();
		config.yearHeaderFont = _fontEditorYearColumnHeader.getSelection();

		// layout
		config.weekHeight = _spinnerWeekHeight.getSelection();

		_calendarView.updateUI_CalendarConfig();
	}

	private void updateUI() {

		_calendarView.updateUI_Layout(true);
	}

	@Override
	public void updateUI_CalendarConfig() {

		fillUI_Config();

		restoreState();

		updateUI();
	}

}
