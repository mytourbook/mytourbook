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

import java.text.NumberFormat;
import java.util.Locale;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class CalendarProfile implements Cloneable {

	private static int					_cloneCounter;
	private static final boolean		_isLogCalendarProfile	= System.getProperty("logCalendarProfile") != null;	//$NON-NLS-1$

	/**
	 * <b>VERY IMPORTANT</b>
	 * <p>
	 * When using 'simple' formatting WITH the locale de then a , (comma) is set and not a . (dot)
	 * <p>
	 * -> this is not a float !!!
	 */
	private final static NumberFormat	_nf1					= NumberFormat.getNumberInstance(Locale.US);

	static {

		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	/*
	 * Set default values also here to ensure that a valid value is set. A default value would not
	 * be set when an xml tag is not available.
	 */

// SET_FORMATTING_OFF
	
	// profile
	String					id							= Long.toString(System.nanoTime());
	String					profileName					= Messages.Calendar_Profile_Name_Default;
	DefaultId				defaultId					= CalendarProfileManager.DEFAULT_PROFILE_DEFAULT_ID;
	boolean					isDefaultDefault			= false;
	boolean					isUserDefault				= false;
	String					userDefaultId				= UI.EMPTY_STRING;
	//
	// layout
	boolean					isToggleMonthColor			= false;
	boolean					isWeekRowHeight				= CalendarProfileManager.DEFAULT_IS_WEEK_ROW_HEIGHT;
	boolean					useDraggedScrolling			= false;
	RGB 					alternateMonthRGB			= CalendarProfileManager.DEFAULT_ALTERNATE_MONTH_RGB;
	RGB 					alternateMonth2RGB			= CalendarProfileManager.DEFAULT_ALTERNATE_MONTH2_RGB;
	RGB						calendarBackgroundRGB		= CalendarProfileManager.DEFAULT_CALENDAR_BACKGROUND_RGB;
	RGB						calendarForegroundRGB		= CalendarProfileManager.DEFAULT_CALENDAR_FOREBACKGROUND_RGB;
	RGB						dayHoveredRGB				= CalendarProfileManager.DEFAULT_DAY_HOVERED_RGB;
	RGB						daySelectedRGB				= CalendarProfileManager.DEFAULT_DAY_SELECTED_RGB;
	int						weekHeight					= CalendarProfileManager.DEFAULT_WEEK_HEIGHT;
	int						weekRows					= CalendarProfileManager.DEFAULT_WEEK_ROWS;
	//
	// 1. Date column
	boolean					isShowDateColumn			= true;
	DateColumnContent		dateColumnContent			= CalendarProfileManager.DEFAULT_DATE_COLUMN_CONTENT;
	FontData				dateColumnFont				= createFont(1.7f, SWT.BOLD);
	int						dateColumnWidth				= CalendarProfileManager.DEFAULT_DATE_COLUMN_WIDTH;
	//
	// 2. Year columns
	boolean 				isShowYearColumns			= true;
	boolean 				isYearColumnDayWidth		= CalendarProfileManager.DEFAULT_IS_YEAR_COLUMN_DAY_WIDTH;
	int						yearColumns					= CalendarProfileManager.DEFAULT_YEAR_COLUMNS;
	int 					yearColumnsSpacing			= CalendarProfileManager.DEFAULT_YEAR_COLUMNS_SPACING;
	ColumnStart				yearColumnsStart			= CalendarProfileManager.DEFAULT_YEAR_COLUMNS_LAYOUT;
	int 					yearColumnDayWidth			= CalendarProfileManager.DEFAULT_YEAR_COLUMN_DAY_WIDTH;
	FontData 				yearHeaderFont				= createFont(2.8f, SWT.BOLD);
	//
	// 3. Week summary column
	boolean					isShowSummaryColumn			= true;
	boolean 				isShowWeekValueUnit			= true;
	FormatterData[]			allWeekFormatterData		= CalendarProfileManager.DEFAULT_WEEK_FORMATTER_DATA;
	int						weekColumnWidth				= CalendarProfileManager.DEFAULT_WEEK_COLUMN_WIDTH;
	int						weekMarginTop				= CalendarProfileManager.DEFAULT_WEEK_MARGIN_TOP;
	int						weekMarginLeft				= CalendarProfileManager.DEFAULT_WEEK_MARGIN_LEFT;
	int						weekMarginBottom			= CalendarProfileManager.DEFAULT_WEEK_MARGIN_BOTTOM;
	int						weekMarginRight				= CalendarProfileManager.DEFAULT_WEEK_MARGIN_RIGHT;
	CalendarColor			weekValueColor				= CalendarProfileManager.DEFAULT_WEEK_VALUE_COLOR;
	FontData				weekValueFont				= createFont(1.2f, SWT.BOLD);
	RGB 					weekValueRGB				= CalendarProfileManager.DEFAULT_WEEK_VALUE_RGB;
	//
	// day date
	boolean					isHideDayDateWhenNoTour		= true;
	boolean					isShowDayDate				= false;
	boolean 				isShowDayDateWeekendColor	= CalendarProfileManager.DEFAULT_IS_SHOW_DAY_DATE_WEEKEND_COLOR;
	FontData				dayDateFont					= createFont(1.2f, SWT.BOLD);
	DayDateFormat			dayDateFormat				= CalendarProfileManager.DEFAULT_DAY_DATE_FORMAT;
	int 					dayDateMarginTop			= CalendarProfileManager.DEFAULT_DAY_DATE_MARGIN_TOP;
	int 					dayDateMarginLeft			= CalendarProfileManager.DEFAULT_DAY_DATE_MARGIN_LEFT;
	//
	// tour background
	TourBackground			tourBackground				= CalendarProfileManager.DEFAULT_TOUR_BACKGROUND;
	CalendarColor			tourBackground1Color		= CalendarProfileManager.DEFAULT_TOUR_BACKGROUND_COLOR1;
	CalendarColor			tourBackground2Color		= CalendarProfileManager.DEFAULT_TOUR_BACKGROUND_COLOR2;
	RGB						tourBackground1RGB			= CalendarProfileManager.DEFAULT_TOUR_BACKGROUND_1_RGB;
	RGB						tourBackground2RGB			= CalendarProfileManager.DEFAULT_TOUR_BACKGROUND_2_RGB;
	int						tourBackgroundWidth			= CalendarProfileManager.DEFAULT_TOUR_BACKGROUND_WIDTH;
	TourBorder 				tourBorder					= CalendarProfileManager.DEFAULT_TOUR_BORDER;
	int		 				tourBorderWidth				= CalendarProfileManager.DEFAULT_TOUR_BORDER_WIDTH;
	CalendarColor 			tourBorderColor				= CalendarProfileManager.DEFAULT_TOUR_BORDER_COLOR;
	CalendarColor			tourDraggedColor			= CalendarProfileManager.DEFAULT_TOUR_DRAGGED_COLOR;
	CalendarColor			tourHoveredColor			= CalendarProfileManager.DEFAULT_TOUR_HOVERED_COLOR;
	CalendarColor			tourSelectedColor			= CalendarProfileManager.DEFAULT_TOUR_SELECTED_COLOR;
	RGB						tourBorderRGB				= CalendarProfileManager.DEFAULT_TOUR_BORDER_RGB;
	RGB						tourDraggedRGB				= CalendarProfileManager.DEFAULT_TOUR_DRAGGED_RGB;
	RGB						tourHoveredRGB				= CalendarProfileManager.DEFAULT_TOUR_HOVERED_RGB;
	RGB						tourSelectedRGB				= CalendarProfileManager.DEFAULT_TOUR_SELECTED_RGB;
	//
	// tour content
	boolean 				isShowTourContent			= true;
	boolean					isShowTourValueUnit			= true;
	boolean					isTruncateTourText			= CalendarProfileManager.DEFAULT_IS_TRUNCATE_TOUR_TEXT;
	FormatterData[]			allTourFormatterData		= CalendarProfileManager.DEFAULT_TOUR_FORMATTER_DATA;
	int						tourMarginTop				= CalendarProfileManager.DEFAULT_TOUR_MARGIN_TOP;
	int						tourMarginLeft				= CalendarProfileManager.DEFAULT_TOUR_MARGIN_LEFT;
	int						tourMarginBottom			= CalendarProfileManager.DEFAULT_TOUR_MARGIN_BOTTOM;
	int						tourMarginRight				= CalendarProfileManager.DEFAULT_TOUR_MARGIN_RIGHT;
	int						tourTruncatedLines			= CalendarProfileManager.DEFAULT_TOUR_TRUNCATED_LINES;
	int 					tourValueColumns			= CalendarProfileManager.DEFAULT_TOUR_VALUE_COLUMNS;
	FontData				tourContentFont				= createFont(0.9f, SWT.NORMAL);
	FontData				tourTitleFont				= createFont(1.2f, SWT.BOLD);
	FontData				tourValueFont				= createFont(1.1f, SWT.BOLD);
	CalendarColor			tourContentColor			= CalendarProfileManager.DEFAULT_TOUR_COLOR;
	CalendarColor 			tourTitleColor				= CalendarProfileManager.DEFAULT_TOUR_COLOR;
	CalendarColor			tourValueColor				= CalendarProfileManager.DEFAULT_TOUR_COLOR;
	RGB						tourContentRGB				= CalendarProfileManager.DEFAULT_TOUR_CONTENT_RGB;
	RGB						tourTitleRGB				= CalendarProfileManager.DEFAULT_TOUR_TITLE_RGB;
	RGB						tourValueRGB				= CalendarProfileManager.DEFAULT_TOUR_VALUE_RGB;


	
// SET_FORMATTING_ON

	/**
	 * @param relSize
	 * @param style
	 * @return
	 */
	static FontData createFont(final float relSize, final int style) {

		final Display display = Display.getDefault();

		// !!! getFontData() MUST be created for EVERY font otherwise they use all the SAME font !!!
		final FontData[] fontData = display.getSystemFont().getFontData();

		for (final FontData element : fontData) {

			element.setHeight((int) (element.getHeight() * relSize));
			element.setStyle(style);

			break;
		}

		return fontData[0];
	}

	private static FontData createFont(final FontData otherFontData) {

		final Display display = Display.getDefault();

		// !!! getFontData() MUST be created for EVERY font otherwise they use all the SAME font !!!
		final FontData[] fontData = display.getSystemFont().getFontData();

		final FontData firstFontData = fontData[0];

		firstFontData.setHeight(otherFontData.getHeight());
		firstFontData.setStyle(otherFontData.getStyle());

		return firstFontData;
	}

	@Override
	protected CalendarProfile clone() {

		CalendarProfile clonedProfile = null;

		try {

			clonedProfile = (CalendarProfile) super.clone();

			clonedProfile.id = Long.toString(System.nanoTime());

			// create a unique name
			clonedProfile.profileName = profileName + UI.SPACE + ++_cloneCounter;

			clonedProfile.yearHeaderFont = createFont(yearHeaderFont);
			clonedProfile.dateColumnFont = createFont(dateColumnFont);
			clonedProfile.dayDateFont = createFont(dayDateFont);
			clonedProfile.tourContentFont = createFont(tourContentFont);
			clonedProfile.tourTitleFont = createFont(tourTitleFont);
			clonedProfile.tourValueFont = createFont(tourValueFont);
			clonedProfile.weekValueFont = createFont(weekValueFont);

			clonedProfile.allTourFormatterData = clone_FormatterData(allTourFormatterData);
			clonedProfile.allWeekFormatterData = clone_FormatterData(allWeekFormatterData);

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedProfile;
	}

	private FormatterData[] clone_FormatterData(final FormatterData[] allFormatterData) {

		final FormatterData[] clonedFormatterData = new FormatterData[allFormatterData.length];

		for (int formatterIndex = 0; formatterIndex < allFormatterData.length; formatterIndex++) {

			final FormatterData formatterData = allFormatterData[formatterIndex];

			clonedFormatterData[formatterIndex] = formatterData.clone();
		}

		return clonedFormatterData;
	}

	void dump() {

		if (_isLogCalendarProfile == false) {
			return;
		}

		final CalendarProfile profile = this;

		final StringBuilder sb = new StringBuilder();

// SET_FORMATTING_OFF

        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("// SET_FORMATTING_OFF");                                                                         //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("//                                      " + profile.profileName + "\n");                         //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("profile.defaultId                     = DefaultId." + defaultId                        + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("// layout                                                                                  \n"); //$NON-NLS-1$
        sb.append("profile.isToggleMonthColor            = " + isToggleMonthColor                         + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.isWeekRowHeight               = " + isWeekRowHeight                            + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.useDraggedScrolling           = " + useDraggedScrolling                        + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.weekHeight                    = " + weekHeight                                 + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.weekRows                      = " + weekRows                                   + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.alternateMonthRGB             = " + dump_RGB(profile.alternateMonthRGB)        + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.alternateMonth2RGB            = " + dump_RGB(profile.alternateMonth2RGB)       + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.calendarBackgroundRGB         = " + dump_RGB(profile.calendarBackgroundRGB)    + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.calendarForegroundRGB         = " + dump_RGB(profile.calendarForegroundRGB)    + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.dayHoveredRGB                 = " + dump_RGB(profile.dayHoveredRGB)            + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.daySelectedRGB                = " + dump_RGB(profile.daySelectedRGB)           + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("                                                                                           \n"); //$NON-NLS-1$
        sb.append("// 1. Date column                                                                          \n"); //$NON-NLS-1$
        sb.append("profile.isShowDateColumn              = " + isShowDateColumn                           + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.dateColumnContent             = DateColumnContent." + dateColumnContent        + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.dateColumnFont                = " + dump_Font(dateColumnFont)                  + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.dateColumnWidth               = " + dateColumnWidth                            + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("                                                                                           \n"); //$NON-NLS-1$
        sb.append("// 2. Year columns                                                                         \n"); //$NON-NLS-1$
        sb.append("profile.isShowYearColumns             = " + isShowYearColumns                          + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.isYearColumnDayWidth          = " + isYearColumnDayWidth                       + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.yearColumns                   = " + yearColumns                                + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.yearColumnsSpacing            = " + yearColumnsSpacing                         + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.yearColumnsStart              = ColumnStart." + yearColumnsStart               + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.yearColumnDayWidth            = " + yearColumnDayWidth                         + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.yearHeaderFont                = " + dump_Font(yearHeaderFont)                  + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("                                                                                           \n"); //$NON-NLS-1$
        sb.append("// 3. Week summary column                                                                  \n"); //$NON-NLS-1$
        sb.append("profile.isShowSummaryColumn           = " + isShowSummaryColumn                        + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.isShowWeekValueUnit           = " + isShowWeekValueUnit                        + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.weekColumnWidth               = " + weekColumnWidth                            + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.weekMarginTop                 = " + weekMarginTop                              + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.weekMarginLeft                = " + weekMarginLeft                             + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.weekMarginBottom              = " + weekMarginBottom                           + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.weekMarginRight               = " + weekMarginRight                            + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.weekValueFont                 = " + dump_Font(weekValueFont)                   + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.weekValueColor                = CalendarColor." + weekValueColor               + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.weekValueRGB                  = " + dump_RGB(profile.weekValueRGB)             + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("                                                                                           \n"); //$NON-NLS-1$
        sb.append("// day date                                                                                \n"); //$NON-NLS-1$
        sb.append("profile.isHideDayDateWhenNoTour       = " + isHideDayDateWhenNoTour                    + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.isShowDayDate                 = " + isShowDayDate                              + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.isShowDayDateWeekendColor     = " + isShowDayDateWeekendColor                  + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.dayDateMarginTop              = " + dayDateMarginTop                           + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.dayDateMarginLeft             = " + dayDateMarginLeft                          + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.dayDateFont                   = " + dump_Font(dayDateFont)                     + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.dayDateFormat                 = DayDateFormat." + dayDateFormat                + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("                                                                                           \n"); //$NON-NLS-1$
        sb.append("// tour background                                                                         \n"); //$NON-NLS-1$
        sb.append("profile.tourBackground                = TourBackground." + tourBackground              + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourBackgroundWidth           = " + tourBackgroundWidth                        + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourBorder                    = TourBorder." + tourBorder                      + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourBorderWidth               = " + tourBorderWidth                            + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourBackground1Color          = CalendarColor." + tourBackground1Color         + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourBackground2Color          = CalendarColor." + tourBackground2Color         + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourBorderColor               = CalendarColor." + tourBorderColor              + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourHoveredColor              = CalendarColor." + tourHoveredColor             + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourDraggedColor              = CalendarColor." + tourDraggedColor             + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourSelectedColor             = CalendarColor." + tourSelectedColor            + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourBackground1RGB            = " + dump_RGB(profile.tourBackground1RGB)       + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourBackground2RGB            = " + dump_RGB(profile.tourBackground2RGB)       + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourBorderRGB                 = " + dump_RGB(profile.tourBorderRGB)            + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourDraggedRGB                = " + dump_RGB(profile.tourDraggedRGB)           + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourHoveredRGB                = " + dump_RGB(profile.tourHoveredRGB)           + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourSelectedRGB               = " + dump_RGB(profile.tourSelectedRGB)          + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("                                                                                           \n"); //$NON-NLS-1$
        sb.append("// tour content                                                                            \n"); //$NON-NLS-1$
        sb.append("profile.isShowTourContent             = " + isShowTourContent                          + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.isShowTourValueUnit           = " + isShowTourValueUnit                        + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.isTruncateTourText            = " + isTruncateTourText                         + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourMarginTop                 = " + tourMarginTop                              + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourMarginLeft                = " + tourMarginLeft                             + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourMarginBottom              = " + tourMarginBottom                           + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourMarginRight               = " + tourMarginRight                            + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourTruncatedLines            = " + tourTruncatedLines                         + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourValueColumns              = " + tourValueColumns                           + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourContentFont               = " + dump_Font(tourContentFont)                 + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourTitleFont                 = " + dump_Font(tourTitleFont)                   + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourValueFont                 = " + dump_Font(tourValueFont)                   + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourContentColor              = CalendarColor." + tourContentColor             + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourTitleColor                = CalendarColor." + tourTitleColor               + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourValueColor                = CalendarColor." + tourValueColor               + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourContentRGB                = " + dump_RGB(profile.tourContentRGB)           + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourTitleRGB                  = " + dump_RGB(profile.tourTitleRGB)             + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("profile.tourValueRGB                  = " + dump_RGB(profile.tourValueRGB)             + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("profile.allTourFormatterData          = " + dump_Formatter(allTourFormatterData, "tour") + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("profile.allWeekFormatterData          = " + dump_Formatter(allWeekFormatterData, "week") + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("// SET_FORMATTING_ON");                                                                          //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$
        sb.append("\n");                                                                                            //$NON-NLS-1$

// SET_FORMATTING_ON

		System.out.print(sb.toString());
	}

	private String dump_Font(final FontData fontData) {

		final Display display = Display.getDefault();

		// !!! getFontData() MUST be created for EVERY font otherwise they use all the SAME font !!!
		final FontData[] allSystemFontData = display.getSystemFont().getFontData();
		final FontData systemFontData = allSystemFontData[0];

		final float fontHeight = (float) fontData.getHeight() / (float) systemFontData.getHeight() + 0.05f;
		final String fontHeightText = _nf1.format(fontHeight);

		final int fontStyle = fontData.getStyle();
		String fontStyleText;

		if (fontStyle == SWT.BOLD) {
			fontStyleText = "SWT.BOLD"; //$NON-NLS-1$
		} else if (fontStyle == SWT.ITALIC) {
			fontStyleText = "SWT.ITALIC"; //$NON-NLS-1$
		} else {
			fontStyleText = "SWT.NORMAL"; //$NON-NLS-1$
		}

		return String.format("CalendarProfile.createFont(%sf, %s)", fontHeightText, fontStyleText); //$NON-NLS-1$
	}

	private String dump_Formatter(final FormatterData[] allFormatterData, final String tourOrWeek) {

		final StringBuilder sb = new StringBuilder();

		sb.append("new FormatterData[] {"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		for (final FormatterData formatterData : allFormatterData) {

			final String isEnabled = formatterData.isEnabled ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$

			String formatterID = UI.EMPTY_STRING;

			switch (formatterData.id) {

			case ALTITUDE:
				formatterID = FormatterID.ALTITUDE.name();

				break;

			case DISTANCE:
				formatterID = FormatterID.DISTANCE.name();
				break;

			case PACE:
				formatterID = FormatterID.PACE.name();
				break;

			case SPEED:
				formatterID = FormatterID.SPEED.name();
				break;

			case TIME_MOVING:
				formatterID = FormatterID.TIME_MOVING.name();
				break;

			case TIME_PAUSED:
				formatterID = FormatterID.TIME_PAUSED.name();
				break;

			case TIME_RECORDING:
				formatterID = FormatterID.TIME_RECORDING.name();
				break;

			case TOUR_DESCRIPTION:
				formatterID = FormatterID.TOUR_DESCRIPTION.name();
				break;

			case TOUR_TITLE:
				formatterID = FormatterID.TOUR_TITLE.name();
				break;

			case EMPTY:
			default:
				formatterID = FormatterID.EMPTY.name();
				break;
			}

			sb.append(
					String.format(
							"\tnew FormatterData(%-10s %-30s ValueFormat.%s),\n", //$NON-NLS-1$
							isEnabled + ",", //$NON-NLS-1$
							"FormatterID." + formatterID + ",", //$NON-NLS-1$ //$NON-NLS-2$
							formatterData.valueFormat.name()));

		}

		sb.append("\n"); //$NON-NLS-1$
		sb.append("};"); //$NON-NLS-1$

		return sb.toString();
	}

	private String dump_RGB(final RGB rgb) {

		return "new RGB (" + rgb.red + ", " + rgb.green + ", " + rgb.blue + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final CalendarProfile other = (CalendarProfile) obj;

		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());

		return result;
	}

	@Override
	public String toString() {
		return "CalendarProfile [name=" + profileName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
