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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.formatter.IValueFormatter;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.formatter.ValueFormatter_Number_1_0;
import net.tourbook.common.formatter.ValueFormatter_Number_1_1;
import net.tourbook.common.formatter.ValueFormatter_Number_1_2;
import net.tourbook.common.formatter.ValueFormatter_Number_1_3;
import net.tourbook.common.formatter.ValueFormatter_Time_HH;
import net.tourbook.common.formatter.ValueFormatter_Time_HHMM;
import net.tourbook.common.formatter.ValueFormatter_Time_HHMMSS;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class CalendarProfileManager {

// SET_FORMATTING_OFF
	//
	private static final String				VALUE_UNIT_K_CALORIES	= net.tourbook.ui.Messages.Value_Unit_KCalories;
	//
	private static final String				DEFAULT_PREFIX			= " : ";					//$NON-NLS-1$
	//
	private static final String				PROFILE_FILE_NAME		= "calendar-profiles.xml";			//$NON-NLS-1$
	//
	/**
	 * Version number is not yet used.
	 */
	private static final int				PROFILE_VERSION			= 1;
	//
	private static final Bundle				_bundle					= TourbookPlugin.getDefault().getBundle();
	private static final IPath				_stateLocation			= Platform.getStateLocation(_bundle);
	//
// SET_FORMATTING_ON
	//
	// common attributes
	private static final String				ATTR_ID									= "id";								//$NON-NLS-1$
	private static final String				ATTR_ACTIVE_PROFILE_ID					= "activeProfileId";				//$NON-NLS-1$
	private static final String				ATTR_PROFILE_NAME						= "profileName";					//$NON-NLS-1$
	//
	private static final String				ATTR_IS_DEFAULT_DEFAULT_ID				= "isDefaultDefaultId";				//$NON-NLS-1$
	private static final String				ATTR_IS_USER_PARENT_DEFAULT_ID			= "isUserParentDefaultId";			//$NON-NLS-1$
	private static final String				ATTR_PROFILE_DEFAULT_DEFAULT_ID			= "profileDefaultDefaultId";		//$NON-NLS-1$
	private static final String				ATTR_PROFILE_USER_DEFAULT_ID			= "profileUserDefaultId";			//$NON-NLS-1$
	private static final String				ATTR_PROFILE_USER_PARENT_DEFAULT_ID		= "profileUserParentDefaultId";		//$NON-NLS-1$
	//
	/*
	 * Root
	 */
	private static final String				TAG_ROOT								= "CalendarProfiles";				//$NON-NLS-1$
	private static final String				ATTR_PROFILE_VERSION					= "profileVersion";					//$NON-NLS-1$
	//
	/*
	 * Calendars
	 */
	private static final String				TAG_CALENDAR_PROFILE					= "CalendarProfile";				//$NON-NLS-1$
	private static final String				TAG_CALENDAR							= "Calendar";						//$NON-NLS-1$
	//
	private static final String				TAG_ALL_TOUR_FORMATTER					= "AllTourFormatter";				//$NON-NLS-1$
	private static final String				TAG_ALL_WEEK_FORMATTER					= "AllWeekFormatter";				//$NON-NLS-1$
	private static final String				TAG_ALTERNATE_MONTH_RGB					= "AlternateMonthRGB";				//$NON-NLS-1$
	private static final String				TAG_ALTERNATE_MONTH2_RGB				= "AlternateMonth2RGB";				//$NON-NLS-1$
	private static final String				TAG_CALENDAR_BACKGROUND_RGB				= "CalendarBackgroundRGB";			//$NON-NLS-1$
	private static final String				TAG_CALENDAR_FOREGROUND_RGB				= "CalendarForegroundRGB";			//$NON-NLS-1$
	private static final String				TAG_DAY_HOVERED_RGB						= "DayHoveredRGB";					//$NON-NLS-1$;
	private static final String				TAG_DAY_SELECTED_RGB					= "DaySelectedRGB";					//$NON-NLS-1$;
	private static final String				TAG_DAY_TODAY_RGB						= "DayTodayRGB";					//$NON-NLS-1$
	private static final String				TAG_FORMATTER							= "Formatter";						//$NON-NLS-1$
	private static final String				TAG_TOUR_BACKGROUND_1_RGB				= "TourBackground1RGB";				//$NON-NLS-1$;
	private static final String				TAG_TOUR_BACKGROUND_2_RGB				= "TourBackground2RGB";				//$NON-NLS-1$;
	private static final String				TAG_TOUR_BORDER_RGB						= "TourBorderRGB";					//$NON-NLS-1$;
	private static final String				TAG_TOUR_DRAGGED_RGB					= "TourDraggedRGB";					//$NON-NLS-1$
	private static final String				TAG_TOUR_HOVERED_RGB					= "TourHoveredRGB";					//$NON-NLS-1$;
	private static final String				TAG_TOUR_SELECTED_RGB					= "TourSelectedRGB";				//$NON-NLS-1$;
	private static final String				TAG_TOUR_CONTENT_RGB					= "TourContentRGB";					//$NON-NLS-1$;
	private static final String				TAG_TOUR_TITLE_RGB						= "TourTitleRGB";					//$NON-NLS-1$;
	private static final String				TAG_TOUR_VALUE_RGB						= "TourValueRGB";					//$NON-NLS-1$;
	private static final String				TAG_WEEK_VALUE_RGB						= "weekValueRGB";					//$NON-NLS-1$
	//
	private static final String				ATTR_IS_DAY_CONTENT_VERTICAL			= "isDayContentVertical";			//$NON-NLS-1$
	private static final String				ATTR_IS_HIDE_DAY_DATE_WHEN_NO_TOUR		= "isHideDayDateWhenNoTour";		//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_DATE_COLUMN				= "isShowDateColumn";				//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_DAY_DATE					= "isShowDayDate";					//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_DAY_DATE_WEEKEND_COLOR		= "isShowDayDateWeekendColor";		//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_SUMMARY_COLUMN				= "isShowSummaryColumn";			//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_TOUR_CONTENT				= "isShowTourContent";				//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_TOUR_VALUE_UNIT			= "isShowTourValueUnit";			//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_YEAR_COLUMNS				= "isShowYearColumns";				//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_VALUE						= "isShowValue";					//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_WEEK_VALUE_UNIT			= "isShowWeekValueUnit";			//$NON-NLS-1$
	private static final String				ATTR_IS_TOGGLE_MONTH_COLOR				= "isToggleMonthColor";				//$NON-NLS-1$
	private static final String				ATTR_IS_TRUNCATE_TOUR_TEXT				= "isTruncateTourText";				//$NON-NLS-1$
	private static final String				ATTR_IS_WEEK_ROW_HEIGHT					= "isWeekRowHeight";				//$NON-NLS-1$
	private static final String				ATTR_IS_YEAR_COLUMN_DAY_WIDTH			= "isYearColumnDayWidth";			//$NON-NLS-1$
	private static final String				ATTR_DATE_COLUMN_CONTENT				= "dateColumnContent";				//$NON-NLS-1$
	private static final String				ATTR_DATE_COLUMN_FONT					= "dateColumnFont";					//$NON-NLS-1$
	private static final String				ATTR_DATE_COLUMN_WIDTH					= "dateColumnWidth";				//$NON-NLS-1$
	private static final String				ATTR_DAY_DATE_FONT						= "dayDateFont";					//$NON-NLS-1$
	private static final String				ATTR_DAY_DATE_FORMAT					= "dayDateFormat";					//$NON-NLS-1$
	private static final String				ATTR_DAY_DATE_MARGIN_TOP				= "dayDateMarginTop";				//$NON-NLS-1$
	private static final String				ATTR_DAY_DATE_MARGIN_LEFT				= "dayDateMarginLeft";				//$NON-NLS-1$
	private static final String				ATTR_FORMATTER_ID						= "formatterId";					//$NON-NLS-1$
	private static final String				ATTR_FORMATTER_VALUE_FORMAT				= "formatterValueFormat";			//$NON-NLS-1$
	private static final String				ATTR_TOUR_BACKGROUND					= "tourBackground";					//$NON-NLS-1$
	private static final String				ATTR_TOUR_BACKGROUND_COLOR1				= "tourBackgroundColor1";			//$NON-NLS-1$
	private static final String				ATTR_TOUR_BACKGROUND_COLOR2				= "tourBackgroundColor2";			//$NON-NLS-1$
	private static final String				ATTR_TOUR_BORDER_WIDTH					= "tourBackgroundWidth";			//$NON-NLS-1$
	private static final String				ATTR_TOUR_BORDER						= "tourBorder";						//$NON-NLS-1$
	private static final String				ATTR_TOUR_BORDER_COLOR					= "tourBorderColor";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_BACKGROUND_WIDTH				= "tourBorderWidth";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_CONTENT_FONT					= "tourContentFont";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_CONTENT_COLOR					= "tourContentColor";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_DRAGGED_COLOR					= "tourDraggedColor";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_HOVERED_COLOR					= "tourHoveredColor";				//$NON-NLS-1$;
	private static final String				ATTR_TOUR_MARGIN_TOP					= "tourMarginTop";					//$NON-NLS-1$
	private static final String				ATTR_TOUR_MARGIN_LEFT					= "tourMarginLeft";					//$NON-NLS-1$
	private static final String				ATTR_TOUR_MARGIN_BOTTOM					= "tourMarginBottom";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_MARGIN_RIGHT					= "tourMarginRight";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_SELECTED_COLOR				= "tourSelectedColor";				//$NON-NLS-1$;
	private static final String				ATTR_TOUR_TITLE_COLOR					= "tourTitleColor";					//$NON-NLS-1$
	private static final String				ATTR_TOUR_TITLE_FONT					= "tourTitleFont";					//$NON-NLS-1$
	private static final String				ATTR_TOUR_TRUNCATED_LINES				= "tourTruncatedLines";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_VALUE_COLOR					= "tourValueColor";					//$NON-NLS-1$
	private static final String				ATTR_TOUR_VALUE_COLUMNS					= "tourValueColumns";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_VALUE_FONT					= "tourValueFont";					//$NON-NLS-1$
	private static final String				ATTR_USE_DRAGGED_SCROLLING				= "useDraggedScrolling";			//$NON-NLS-1$
	private static final String				ATTR_WEEK_COLUMN_WIDTH					= "weekColumnWidth";				//$NON-NLS-1$
	private static final String				ATTR_WEEK_HEIGHT						= "weekHeight";						//$NON-NLS-1$
	private static final String				ATTR_WEEK_MARGIN_TOP					= "weekMarginTop";					//$NON-NLS-1$
	private static final String				ATTR_WEEK_MARGIN_LEFT					= "weekMarginLeft";					//$NON-NLS-1$
	private static final String				ATTR_WEEK_MARGIN_BOTTOM					= "weekMarginBottom";				//$NON-NLS-1$
	private static final String				ATTR_WEEK_MARGIN_RIGHT					= "weekMarginRight";				//$NON-NLS-1$
	private static final String				ATTR_WEEK_ROWS							= "weekRows";						//$NON-NLS-1$
	private static final String				ATTR_WEEK_VALUE_COLOR					= "weekValueColor";					//$NON-NLS-1$
	private static final String				ATTR_WEEK_VALUE_FONT					= "weekValueFont";					//$NON-NLS-1$
	private static final String				ATTR_YEAR_COLUMNS						= "yearColumns";					//$NON-NLS-1$
	private static final String				ATTR_YEAR_COLUMNS_SPACING				= "yearColumnsSpacing";				//$NON-NLS-1$
	private static final String				ATTR_YEAR_COLUMNS_START					= "yearColumnsStart";				//$NON-NLS-1$
	private static final String				ATTR_YEAR_COLUMN_DAY_WIDTH				= "yearColumnDayWidth";				//$NON-NLS-1$
	private static final String				ATTR_YEAR_HEADER_FONT					= "yearHeaderFont";					//$NON-NLS-1$
	//
	static final DataFormatter				DEFAULT_EMPTY_FORMATTER;
	//
	static final DefaultId					DEFAULT_PROFILE_DEFAULT_ID				= DefaultId.DEFAULT;
	static final int						DEFAULT_DATE_COLUMN_WIDTH				= 50;
	static final DateColumnContent			DEFAULT_DATE_COLUMN_CONTENT				= DateColumnContent.MONTH;
	static final DayDateFormat				DEFAULT_DAY_DATE_FORMAT					= DayDateFormat.DAY;
	static final int						DEFAULT_DAY_DATE_MARGIN_TOP				= 0;
	static final int						DEFAULT_DAY_DATE_MARGIN_LEFT			= 0;
	static final boolean					DEFAULT_IS_SHOW_DAY_DATE_WEEKEND_COLOR	= false;
	static final boolean					DEFAULT_IS_TRUNCATE_TOUR_TEXT			= true;
	static final boolean					DEFAULT_IS_WEEK_ROW_HEIGHT				= false;
	static final boolean					DEFAULT_IS_YEAR_COLUMN_DAY_WIDTH		= false;
	static final int						DEFAULT_MARGIN_MIN						= -30;
	static final int						DEFAULT_MARGIN_MAX						= 30;
	static final int						DEFAULT_WEEK_COLUMN_WIDTH				= 100;
	static final RGB						DEFAULT_ALTERNATE_MONTH_RGB				= new RGB(60, 60, 60);
	static final RGB						DEFAULT_ALTERNATE_MONTH2_RGB			= new RGB(80, 80, 80);
	static final RGB						DEFAULT_CALENDAR_BACKGROUND_RGB			= new RGB(40, 40, 40);
	static final RGB						DEFAULT_CALENDAR_FOREBACKGROUND_RGB		= new RGB(200, 200, 200);
	static final RGB						DEFAULT_DAY_HOVERED_RGB					= new RGB(255, 255, 255);
	static final RGB						DEFAULT_DAY_SELECTED_RGB				= new RGB(128, 128, 128);
	static final RGB						DEFAULT_DAY_TODAY_RGB					= new RGB(255, 255, 0);
	//
	// tour background
	static final TourBackground				DEFAULT_TOUR_BACKGROUND					= TourBackground.FILL;
	static final int						DEFAULT_TOUR_BACKGROUND_WIDTH			= 3;
	static final TourBorder					DEFAULT_TOUR_BORDER						= TourBorder.NO_BORDER;
	static final int						DEFAULT_TOUR_BORDER_WIDTH				= 1;
	static final CalendarColor				DEFAULT_TOUR_BACKGROUND_COLOR1			= CalendarColor.DARK;
	static final CalendarColor				DEFAULT_TOUR_BACKGROUND_COLOR2			= CalendarColor.BRIGHT;
	static final CalendarColor				DEFAULT_TOUR_BORDER_COLOR				= CalendarColor.LINE;
	static final CalendarColor				DEFAULT_TOUR_COLOR						= CalendarColor.CONTRAST;
	static final CalendarColor				DEFAULT_TOUR_DRAGGED_COLOR				= CalendarColor.CUSTOM;
	static final CalendarColor				DEFAULT_TOUR_HOVERED_COLOR				= CalendarColor.CUSTOM;
	static final CalendarColor				DEFAULT_TOUR_SELECTED_COLOR				= CalendarColor.CUSTOM;
	static final RGB						DEFAULT_TOUR_BACKGROUND_1_RGB			= new RGB(64, 0, 255);
	static final RGB						DEFAULT_TOUR_BACKGROUND_2_RGB			= new RGB(128, 64, 0);
	static final RGB						DEFAULT_TOUR_BORDER_RGB					= new RGB(192, 128, 0);
	static final RGB						DEFAULT_TOUR_DRAGGED_RGB				= new RGB(255, 255, 0);
	static final RGB						DEFAULT_TOUR_HOVERED_RGB				= new RGB(255, 255, 255);
	static final RGB						DEFAULT_TOUR_SELECTED_RGB				= new RGB(192, 192, 192);
	//
	// tour content
	static final int						DEFAULT_TOUR_MARGIN_TOP					= -3;
	static final int						DEFAULT_TOUR_MARGIN_LEFT				= 1;
	static final int						DEFAULT_TOUR_MARGIN_BOTTOM				= 1;
	static final int						DEFAULT_TOUR_MARGIN_RIGHT				= -3;
	static final int						DEFAULT_TOUR_TRUNCATED_LINES			= 3;
	static final int						DEFAULT_TOUR_VALUE_COLUMNS				= 3;
	static final RGB						DEFAULT_TOUR_CONTENT_RGB				= new RGB(255, 192, 255);
	static final RGB						DEFAULT_TOUR_TITLE_RGB					= new RGB(191, 255, 203);
	static final RGB						DEFAULT_TOUR_VALUE_RGB					= new RGB(191, 193, 255);
	//
	static final int						DEFAULT_WEEK_HEIGHT						= 120;
	static final int						DEFAULT_WEEK_MARGIN_TOP					= -3;
	static final int						DEFAULT_WEEK_MARGIN_LEFT				= 1;
	static final int						DEFAULT_WEEK_MARGIN_BOTTOM				= 1;
	static final int						DEFAULT_WEEK_MARGIN_RIGHT				= -3;
	static final int						DEFAULT_WEEK_ROWS						= 10;
	static final CalendarColor				DEFAULT_WEEK_VALUE_COLOR				= CalendarColor.BRIGHT;
	static final RGB						DEFAULT_WEEK_VALUE_RGB					= new RGB(255, 0, 128);
	//
	static final int						DEFAULT_YEAR_COLUMN_DAY_WIDTH			= 50;
	static final int						DEFAULT_YEAR_COLUMNS					= 1;
	static final ColumnStart				DEFAULT_YEAR_COLUMNS_LAYOUT				= ColumnStart.CONTINUOUSLY;
	static final int						DEFAULT_YEAR_COLUMNS_SPACING			= 30;
	//
	//
	static final FormatterData[]			DEFAULT_TOUR_FORMATTER_DATA;
	static final FormatterData[]			DEFAULT_WEEK_FORMATTER_DATA;
	static final int						NUM_DEFAULT_TOUR_FORMATTER;
	static final int						NUM_DEFAULT_WEEK_FORMATTER;
	//
	/*
	 * min / MAX values
	 */
	static final int						CALENDAR_COLUMNS_SPACE_MIN				= 0;
	static final int						CALENDAR_COLUMNS_SPACE_MAX				= 300;
	static final int						DATE_COLUMN_WIDTH_MIN					= 1;
	static final int						DATE_COLUMN_WIDTH_MAX					= 200;
	static final int						TOUR_BACKGROUND_WIDTH_MIN				= 0;
	static final int						TOUR_BACKGROUND_WIDTH_MAX				= 100;
	static final int						TOUR_BORDER_WIDTH_MIN					= 0;
	static final int						TOUR_BORDER_WIDTH_MAX					= 100;
	static final int						TOUR_TRUNCATED_LINES_MIN				= 1;
	static final int						TOUR_TRUNCATED_LINES_MAX				= 10;
	static final int						TOUR_VALUE_COLUMNS_MIN					= 1;
	static final int						TOUR_VALUE_COLUMNS_MAX					= 5;
	static final int						WEEK_COLUMN_WIDTH_MIN					= 1;
	static final int						WEEK_COLUMN_WIDTH_MAX					= 200;
	static final int						WEEK_HEIGHT_MIN							= 2;
	static final int						WEEK_HEIGHT_MAX							= 1000;
	static final int						WEEK_ROWS_MIN							= 1;
	static final int						WEEK_ROWS_MAX							= 1000;
	static final int						YEAR_COLUMNS_MIN						= 1;
	static final int						YEAR_COLUMNS_MAX						= 100;
	static final int						YEAR_COLUMN_DAY_WIDTH_MIN				= 1;
	static final int						YEAR_COLUMN_DAY_WIDTH_MAX				= 500;

	private static final DataFormatter		_tourFormatter_Altitude;
	private static final DataFormatter		_tourFormatter_Distance;
	private static final DataFormatter		_tourFormatter_Energy_kcal;
	private static final DataFormatter		_tourFormatter_Energy_MJ;
	private static final DataFormatter		_tourFormatter_Pace;
	private static final DataFormatter		_tourFormatter_Speed;
	private static final DataFormatter		_tourFormatter_Time_Moving;
	private static final DataFormatter		_tourFormatter_Time_Paused;
	private static final DataFormatter		_tourFormatter_Time_Recording;
	private static final DataFormatter		_tourFormatter_TourDescription;
	private static final DataFormatter		_tourFormatter_TourTitle;

	private static final DataFormatter		_weekFormatter_Altitude;
	private static final DataFormatter		_weekFormatter_Distance;
	private static final DataFormatter		_weekFormatter_Energy_kcal;
	private static final DataFormatter		_weekFormatter_Energy_MJ;
	private static final DataFormatter		_weekFormatter_Pace;
	private static final DataFormatter		_weekFormatter_Speed;
	private static final DataFormatter		_weekFormatter_Time_Moving;
	private static final DataFormatter		_weekFormatter_Time_Paused;
	private static final DataFormatter		_weekFormatter_Time_Recording;

	static final DataFormatter[]			allTourContentFormatter;
	static final DataFormatter[]			allWeekFormatter;

// SET_FORMATTING_OFF
	//
	private static final IValueFormatter	_valueFormatter_Number_1_0				= new ValueFormatter_Number_1_0(false);
	private static final IValueFormatter	_valueFormatter_Number_1_1				= new ValueFormatter_Number_1_1(false);
	private static final IValueFormatter	_valueFormatter_Number_1_2				= new ValueFormatter_Number_1_2(false);
	private static final IValueFormatter	_valueFormatter_Number_1_3				= new ValueFormatter_Number_1_3(false);
	private static final IValueFormatter	_valueFormatter_Time_HH					= new ValueFormatter_Time_HH();
	private static final IValueFormatter	_valueFormatter_Time_HHMM				= new ValueFormatter_Time_HHMM();
	private static final IValueFormatter	_valueFormatter_Time_HHMMSS				= new ValueFormatter_Time_HHMMSS();
	//
	static {


		/*
		 * Formatter
		 */
		DEFAULT_EMPTY_FORMATTER 		= createFormatter_Empty();

		// Tour
		_tourFormatter_TourDescription	= createFormatter_Tour_Description();
		_tourFormatter_TourTitle	 	= createFormatter_Tour_Title();
		
		_tourFormatter_Altitude 		= createFormatter_Altitude();
		_tourFormatter_Distance 		= createFormatter_Distance();

		_tourFormatter_Pace 			= createFormatter_Pace();
		_tourFormatter_Speed 			= createFormatter_Speed();
		
		_tourFormatter_Energy_kcal 		= createFormatter_Energy_kcal();
		_tourFormatter_Energy_MJ 		= createFormatter_Energy_MJ();

		_tourFormatter_Time_Moving 		= createFormatter_Time_Moving();
		_tourFormatter_Time_Paused 		= createFormatter_Time_Paused();
		_tourFormatter_Time_Recording 	= createFormatter_Time_Recording();

		allTourContentFormatter = new DataFormatter[] {
				
				DEFAULT_EMPTY_FORMATTER,
				
				_tourFormatter_TourTitle,
				_tourFormatter_TourDescription,
				
				_tourFormatter_Distance,
				_tourFormatter_Altitude,
				
				_tourFormatter_Speed,
				_tourFormatter_Pace,
				
				_tourFormatter_Energy_kcal,
				_tourFormatter_Energy_MJ,
				
				_tourFormatter_Time_Recording,
				_tourFormatter_Time_Moving,
				_tourFormatter_Time_Paused,
		};
		
		// Week
		_weekFormatter_Altitude 		= createFormatter_Altitude();
		_weekFormatter_Distance 		= createFormatter_Distance();
		
		_weekFormatter_Pace 			= createFormatter_Pace();
		_weekFormatter_Speed 			= createFormatter_Speed();
		
		_weekFormatter_Energy_kcal 		= createFormatter_Energy_kcal();
		_weekFormatter_Energy_MJ 		= createFormatter_Energy_MJ();
		
		_weekFormatter_Time_Moving 		= createFormatter_Time_Moving();
		_weekFormatter_Time_Paused 		= createFormatter_Time_Paused();
		_weekFormatter_Time_Recording 	= createFormatter_Time_Recording();

		allWeekFormatter = new DataFormatter[] {
				
				DEFAULT_EMPTY_FORMATTER,
				
				_weekFormatter_Distance,
				_weekFormatter_Altitude,
				
				_weekFormatter_Speed,
				_weekFormatter_Pace,
				
				_weekFormatter_Energy_kcal,
				_weekFormatter_Energy_MJ,
				
				_weekFormatter_Time_Recording,
				_weekFormatter_Time_Moving,
				_weekFormatter_Time_Paused,
		};

		DEFAULT_TOUR_FORMATTER_DATA = new FormatterData[] {

			new FormatterData(true,		FormatterID.TOUR_TITLE,			_tourFormatter_TourTitle.getDefaultFormat()),
			new FormatterData(true,		FormatterID.TOUR_DESCRIPTION,	_tourFormatter_TourDescription.getDefaultFormat()),
			new FormatterData(true,		FormatterID.DISTANCE,			_tourFormatter_Distance.getDefaultFormat()),
			new FormatterData(true,		FormatterID.ALTITUDE,			_tourFormatter_Altitude.getDefaultFormat()),
			new FormatterData(true,		FormatterID.TIME_MOVING,		_tourFormatter_Time_Moving.getDefaultFormat()),
			new FormatterData(false,	FormatterID.EMPTY,				ValueFormat.DUMMY_VALUE),
			new FormatterData(false,	FormatterID.EMPTY,				ValueFormat.DUMMY_VALUE),
			new FormatterData(false,	FormatterID.EMPTY,				ValueFormat.DUMMY_VALUE),
		};
		
		NUM_DEFAULT_TOUR_FORMATTER = DEFAULT_TOUR_FORMATTER_DATA.length;

		DEFAULT_WEEK_FORMATTER_DATA = new FormatterData[] {
				
			new FormatterData(true,		FormatterID.DISTANCE,			_weekFormatter_Distance.getDefaultFormat()),
			new FormatterData(true,		FormatterID.ALTITUDE,			_weekFormatter_Altitude.getDefaultFormat()),
			new FormatterData(true,		FormatterID.SPEED,				_weekFormatter_Speed.getDefaultFormat()),
			new FormatterData(true,		FormatterID.PACE,				_weekFormatter_Pace.getDefaultFormat()),
			new FormatterData(true,		FormatterID.TIME_MOVING,		_weekFormatter_Time_Moving.getDefaultFormat()),
			new FormatterData(false,	FormatterID.EMPTY,				ValueFormat.DUMMY_VALUE),
		};
		
		NUM_DEFAULT_WEEK_FORMATTER = DEFAULT_WEEK_FORMATTER_DATA.length;
	}
	//
	//
	private static final DateColumn_ComboData[] _allDateColumn_ComboData =

		new DateColumn_ComboData[] {

			new DateColumn_ComboData(DateColumnContent.WEEK_NUMBER,	Messages.Calendar_Profile_DateColumn_WeekNumber),
			new DateColumn_ComboData(DateColumnContent.MONTH, 		Messages.Calendar_Profile_DateColumn_Month),
			new DateColumn_ComboData(DateColumnContent.YEAR, 		Messages.Calendar_Profile_DateColumn_Year),
		};

	private static final ColumnLayout_ComboData[] _allColumnLayout_ComboData =

		new ColumnLayout_ComboData[] {

			new ColumnLayout_ComboData(ColumnStart.CONTINUOUSLY, Messages.Calendar_Profile_ColumnLayout_Continuously),

			new ColumnLayout_ComboData(ColumnStart.JAN, TimeTools.month_Full[0]),
			new ColumnLayout_ComboData(ColumnStart.FEB, TimeTools.month_Full[1]),
			new ColumnLayout_ComboData(ColumnStart.MAR, TimeTools.month_Full[2]),
			new ColumnLayout_ComboData(ColumnStart.APR, TimeTools.month_Full[3]),
			new ColumnLayout_ComboData(ColumnStart.MAY, TimeTools.month_Full[4]),
			new ColumnLayout_ComboData(ColumnStart.JUN, TimeTools.month_Full[5]),
			new ColumnLayout_ComboData(ColumnStart.JUL, TimeTools.month_Full[6]),
			new ColumnLayout_ComboData(ColumnStart.AUG, TimeTools.month_Full[7]),
			new ColumnLayout_ComboData(ColumnStart.SEP, TimeTools.month_Full[8]),
			new ColumnLayout_ComboData(ColumnStart.OCT, TimeTools.month_Full[9]),
			new ColumnLayout_ComboData(ColumnStart.NOV, TimeTools.month_Full[10]),
			new ColumnLayout_ComboData(ColumnStart.DEC, TimeTools.month_Full[11]),

			// repeat continuously -> is more handier
			new ColumnLayout_ComboData(ColumnStart.CONTINUOUSLY, Messages.Calendar_Profile_ColumnLayout_Continuously),
		};

	private static final DayHeaderDateFormat_ComboData[] _allDateHeaderDateFormat_ComboData =

		new DayHeaderDateFormat_ComboData[] {

			new DayHeaderDateFormat_ComboData(DayDateFormat.DAY,
					NLS.bind(
							Messages.Calendar_Profile_DayHeaderDateFormat_Day,
							TimeTools.Formatter_Day.format(LocalDate.now()))),

			new DayHeaderDateFormat_ComboData(DayDateFormat.DAY_MONTH,				TimeTools.Formatter_DayMonth.format(LocalDate.now())),
			new DayHeaderDateFormat_ComboData(DayDateFormat.DAY_MONTH_YEAR,			TimeTools.Formatter_DayMonthYear.format(LocalDate.now())),
			new DayHeaderDateFormat_ComboData(DayDateFormat.AUTOMATIC,				Messages.Calendar_Profile_DayHeaderDateFormat_Automatic),
		};

	private static final TourBackground_ComboData[] _allTourBackground_ComboData =

		new TourBackground_ComboData[] {

			new TourBackground_ComboData(TourBackground.NO_BACKGROUND,			Messages.Calendar_Profile_TourBackground_NoBackground,
					false,
					false,
					false),

			new TourBackground_ComboData(TourBackground.FILL,					Messages.Calendar_Profile_TourBackground_Fill,
					true,
					false,
					false),

			new TourBackground_ComboData(TourBackground.CIRCLE,					Messages.Calendar_Profile_TourBackground_Circle,
					true,
					false,
					false),

			new TourBackground_ComboData(TourBackground.GRADIENT_HORIZONTAL,	Messages.Calendar_Profile_TourBackground_GradientHorizontal,
					true,
					true,
					false),

			new TourBackground_ComboData(TourBackground.GRADIENT_VERTICAL,		Messages.Calendar_Profile_TourBackground_GradientVertical,
					true,
					true,
					false),
			
			new TourBackground_ComboData(TourBackground.FILL_LEFT,				Messages.Calendar_Profile_TourBackground_Fill_Left,
					true,
					false,
					true),
			
			new TourBackground_ComboData(TourBackground.FILL_RIGHT,				Messages.Calendar_Profile_TourBackground_Fill_Right,
					true,
					false,
					true),
		};

	private static final TourBorder_ComboData[] _allTourBorder_ComboData =

		new TourBorder_ComboData[] {

			new TourBorder_ComboData(TourBorder.NO_BORDER,				Messages.Calendar_Profile_TourBorder_NoBorder,
					false,
					false),

			new TourBorder_ComboData(TourBorder.BORDER_ALL,				Messages.Calendar_Profile_TourBorder_All,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_TOP,				Messages.Calendar_Profile_TourBorder_Top,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_BOTTOM,			Messages.Calendar_Profile_TourBorder_Bottom,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_TOP_BOTTOM,		Messages.Calendar_Profile_TourBorder_TopBottom,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_LEFT,			Messages.Calendar_Profile_TourBorder_Left,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_RIGHT,			Messages.Calendar_Profile_TourBorder_Right,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_LEFT_RIGHT,		Messages.Calendar_Profile_TourBorder_LeftRight,
					true,
					true),
		};

	private static final CalendarColor_ComboData[] _allGraphColor_ComboData =

		new CalendarColor_ComboData[] {
	
			new CalendarColor_ComboData(CalendarColor.BRIGHT,			Messages.Calendar_Profile_Color_Bright),
			new CalendarColor_ComboData(CalendarColor.DARK,				Messages.Calendar_Profile_Color_Dark),
			new CalendarColor_ComboData(CalendarColor.LINE,				Messages.Calendar_Profile_Color_Line),
			new CalendarColor_ComboData(CalendarColor.TEXT,				Messages.Calendar_Profile_Color_Text),
			new CalendarColor_ComboData(CalendarColor.BLACK,			Messages.Calendar_Profile_Color_Black),
			new CalendarColor_ComboData(CalendarColor.WHITE,			Messages.Calendar_Profile_Color_White),
			new CalendarColor_ComboData(CalendarColor.CUSTOM,			Messages.Calendar_Profile_Color_Custom),
		};
	
	private static final ProfileDefaultId_ComboData[] _allAppDefault_ComboData =
			
		new ProfileDefaultId_ComboData[] {
				
			new ProfileDefaultId_ComboData(DefaultId.DEFAULT,			Messages.Calendar_Profile_AppDefault_Default),
			new ProfileDefaultId_ComboData(DefaultId.COMPACT,			Messages.Calendar_Profile_AppDefault_Compact),
			new ProfileDefaultId_ComboData(DefaultId.COMPACT_II,		Messages.Calendar_Profile_AppDefault_Compact_II),
			new ProfileDefaultId_ComboData(DefaultId.COMPACT_III,		Messages.Calendar_Profile_AppDefault_Compact_III),
			new ProfileDefaultId_ComboData(DefaultId.YEAR,				Messages.Calendar_Profile_AppDefault_Year),
			new ProfileDefaultId_ComboData(DefaultId.YEAR_II,			Messages.Calendar_Profile_AppDefault_Year_II),
			new ProfileDefaultId_ComboData(DefaultId.YEAR_III,			Messages.Calendar_Profile_AppDefault_Year_III),
			new ProfileDefaultId_ComboData(DefaultId.CLASSIC,			Messages.Calendar_Profile_AppDefault_Classic),
			new ProfileDefaultId_ComboData(DefaultId.USER_ID,			Messages.Calendar_Profile_AppDefault_UserDefault),
	};

	private static final DayContentColor_ComboData[] _allTourContentColor_ComboData =

		new DayContentColor_ComboData[] {

			new DayContentColor_ComboData(CalendarColor.CONTRAST,		Messages.Calendar_Profile_Color_Contrast),
			new DayContentColor_ComboData(CalendarColor.BRIGHT, 		Messages.Calendar_Profile_Color_Bright),
			new DayContentColor_ComboData(CalendarColor.DARK, 			Messages.Calendar_Profile_Color_Dark),
			new DayContentColor_ComboData(CalendarColor.LINE, 			Messages.Calendar_Profile_Color_Line),
			new DayContentColor_ComboData(CalendarColor.TEXT, 			Messages.Calendar_Profile_Color_Text),
			new DayContentColor_ComboData(CalendarColor.BLACK, 			Messages.Calendar_Profile_Color_Black),
			new DayContentColor_ComboData(CalendarColor.WHITE, 			Messages.Calendar_Profile_Color_White),
			new DayContentColor_ComboData(CalendarColor.CUSTOM, 		Messages.Calendar_Profile_Color_Custom),
		};
	//
// SET_FORMATTING_ON
	//
	/**
	 * Contains all calendar profiles which are loaded from a xml file.
	 */
	private static final ArrayList<CalendarProfile>			_allCalendarProfiles				= new ArrayList<>();
	private static final ArrayList<CalendarProfile>			_allDefaultDefaultProfiles			= new ArrayList<>();
	static {
		createProfile_0_AllDefaultDefaultProfiles(_allDefaultDefaultProfiles);
	}
	//
	private static CalendarProfile		_activeCalendarProfile;
	//
	private static String				_fromXml_ActiveCalendarProfileId;
	//
	private final static ListenerList	_profileListener	= new ListenerList();

	static class CalendarColor_ComboData {

		String			label;
		CalendarColor	color;

		public CalendarColor_ComboData(final CalendarColor color, final String label) {

			this.color = color;
			this.label = label;
		}

	}

	static class ColumnLayout_ComboData {

		String		label;
		ColumnStart	columnLayout;

		public ColumnLayout_ComboData(final ColumnStart columnLayout, final String label) {

			this.columnLayout = columnLayout;
			this.label = label;
		}
	}

	static class DateColumn_ComboData {

		String				label;
		DateColumnContent	dateColumn;

		public DateColumn_ComboData(final DateColumnContent dateColumn, final String label) {

			this.dateColumn = dateColumn;
			this.label = label;
		}
	}

	static class DayContentColor_ComboData {

		String			label;
		CalendarColor	dayContentColor;

		DayContentColor_ComboData(final CalendarColor dayContentColor, final String label) {

			this.label = label;
			this.dayContentColor = dayContentColor;
		}

	}

	static class DayHeaderDateFormat_ComboData {

		String			label;
		DayDateFormat	dayHeaderDateFormat;

		public DayHeaderDateFormat_ComboData(final DayDateFormat dayHeaderDateFormat, final String label) {

			this.dayHeaderDateFormat = dayHeaderDateFormat;
			this.label = label;
		}
	}

	interface ICalendarProfileListener {

		/**
		 * Calendar profile is modified, update the UI.
		 */
		abstract void profileIsModified();
	}

	static class ProfileDefaultId_ComboData {

		String		labelOrUserId;
		DefaultId	defaultId;
		boolean		isDefaultDefaultId;

		/**
		 * This constructor creates combo data for default default id's.
		 * 
		 * @param defaultId
		 * @param label
		 */
		ProfileDefaultId_ComboData(final DefaultId defaultId, final String label) {

			this.defaultId = defaultId;
			this.labelOrUserId = label;

			this.isDefaultDefaultId = true;
		}

		/**
		 * This constructor creates combo data which has a user default id.
		 * 
		 * @param userDefaultId
		 * @param defaultId
		 */
		ProfileDefaultId_ComboData(final String userDefaultId) {

			this.labelOrUserId = userDefaultId;
			this.defaultId = DefaultId.USER_ID;
		}

		@Override
		public String toString() {

			return "ProfileDefaultId_ComboData [\n" //$NON-NLS-1$

					+ "labelAndId=" + labelOrUserId + "\n" //$NON-NLS-1$ //$NON-NLS-2$
					+ "defaultId=" + defaultId + "\n" //$NON-NLS-1$ //$NON-NLS-2$
					+ "isDefaultDefaultId=" + isDefaultDefaultId + "\n" //$NON-NLS-1$ //$NON-NLS-2$

					+ "]"; //$NON-NLS-1$
		}

	}

	static class TourBackground_ComboData {

		TourBackground	tourBackground;
		String			label;

		boolean			isWidth;
		boolean			isColor1;
		boolean			isColor2;

		public TourBackground_ComboData(final TourBackground tourBackground,
										final String label,
										final boolean isColor1,
										final boolean isColor2,
										final boolean isWidth) {

			this.tourBackground = tourBackground;
			this.label = label;

			this.isWidth = isWidth;
			this.isColor1 = isColor1;
			this.isColor2 = isColor2;
		}
	}

	static class TourBorder_ComboData {

		TourBorder	tourBorder;
		String		label;

		boolean		isColor;
		boolean		isWidth;

		public TourBorder_ComboData(final TourBorder tourBorder,
									final String label,
									final boolean isColor,
									final boolean isWidth) {

			this.tourBorder = tourBorder;
			this.label = label;
			this.isColor = isColor;
			this.isWidth = isWidth;
		}
	}

	static void addProfileListener(final ICalendarProfileListener profileListener) {
		_profileListener.add(profileListener);
	}

	private static XMLMemento create_Root() {

		final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

		// date/time
		xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

		// plugin version
		final Version version = _bundle.getVersion();
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
		xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

		// profile version
		xmlRoot.putInteger(ATTR_PROFILE_VERSION, PROFILE_VERSION);

		return xmlRoot;
	}

	/**
	 * Altitude
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Altitude() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.ALTITUDE,
				Messages.Calendar_Profile_Value_Altitude,
				GraphColorManager.PREF_GRAPH_ALTITUDE) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.altitude > 0) {

					final float altitude = data.altitude / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;
					final String valueText = valueFormatter.printDouble(altitude);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_ALTITUDE + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.NUMBER_1_0;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {

						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_1 };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatId = valueFormat;
				valueFormatter = getFormatter_Number(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Distance
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Distance() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.DISTANCE,
				Messages.Calendar_Profile_Value_Distance,
				GraphColorManager.PREF_GRAPH_DISTANCE) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.distance > 0) {

					final double distance = data.distance / 1000.0 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

					final String valueText = valueFormatter.printDouble(distance);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_DISTANCE + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.NUMBER_1_0;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_2,
						ValueFormat.NUMBER_1_3 };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatId = valueFormat;
				valueFormatter = getFormatter_Number(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Empty
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Empty() {

		final DataFormatter dataFormatter = new DataFormatter(FormatterID.EMPTY) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {
				return UI.EMPTY_STRING;
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return null;
			}

			@Override
			public String getText() {
				return Messages.Calendar_Profile_Value_ShowNothing;
			}

			@Override
			public ValueFormat[] getValueFormats() {
				return null;
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {}
		};

		return dataFormatter;
	}

	/**
	 * Energy kcal
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Energy_kcal() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.ENERGY_KCAL,
				Messages.Calendar_Profile_Value_Energy_kcal,
				GraphColorManager.PREF_GRAPH_POWER) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				final int calories = data.calories;

				if (calories > 0) {

					final double kcal = calories / 1000.0;

					final String valueText = valueFormatter.printDouble(kcal);

					return isShowValueUnit
							? valueText + UI.SPACE + VALUE_UNIT_K_CALORIES + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.NUMBER_1_0;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_2,
						ValueFormat.NUMBER_1_3 };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatId = valueFormat;
				valueFormatter = getFormatter_Number(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Energy MJ
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Energy_MJ() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.ENERGY_MJ,
				Messages.Calendar_Profile_Value_Energy_MJ,
				GraphColorManager.PREF_GRAPH_POWER) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				final int calories = data.calories;

				if (calories > 0) {

					final double joule = calories * net.tourbook.ui.UI.UNIT_CALORIE_2_JOULE;
					final double megaJoule = joule / 1_000_000;

					final String valueText = valueFormatter.printDouble(megaJoule);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_JOULE_MEGA + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.NUMBER_1_0;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_2,
						ValueFormat.NUMBER_1_3 };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatId = valueFormat;
				valueFormatter = getFormatter_Number(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Pace
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Pace() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.PACE,
				Messages.Calendar_Profile_Value_Pace,
				GraphColorManager.PREF_GRAPH_PACE) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.recordingTime > 0 && data.distance > 0) {

					final float pace = data.distance == 0
							? 0
							: 1000 * data.recordingTime / data.distance * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

					final String valueText = UI.format_mm_ss((long) pace);

					return isShowValueUnit //
							? valueText + UI.SPACE + UI.UNIT_LABEL_PACE + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.PACE_MM_SS;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] { ValueFormat.PACE_MM_SS };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {}
		};

		return dataFormatter;
	}

	/**
	 * Speed
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Speed() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.SPEED,
				Messages.Calendar_Profile_Value_Speed,
				GraphColorManager.PREF_GRAPH_SPEED) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.distance > 0 && data.recordingTime > 0) {

					final float speed = data.distance == 0
							? 0
							: data.distance / (data.recordingTime / 3.6f);

					final String valueText = valueFormatter.printDouble(speed);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_SPEED + UI.SPACE
							: valueText + UI.SPACE;
				} else {

					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.NUMBER_1_0;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_2 };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatId = valueFormat;
				valueFormatter = getFormatter_Number(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Moving time
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Time_Moving() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.TIME_MOVING,
				Messages.Calendar_Profile_Value_MovingTime,
				GraphColorManager.PREF_GRAPH_TIME) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.recordingTime > 0) {

					final String valueText = valueFormatter.printLong(data.drivingTime);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_TIME + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.TIME_HH_MM;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.TIME_HH,
						ValueFormat.TIME_HH_MM,
						ValueFormat.TIME_HH_MM_SS };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatId = valueFormat;
				valueFormatter = getFormatter_Time(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Paused time
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Time_Paused() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.TIME_PAUSED,
				Messages.Calendar_Profile_Value_PausedTime,
				GraphColorManager.PREF_GRAPH_TIME) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.recordingTime > 0) {

					final String valueText = valueFormatter.printLong(data.recordingTime - data.drivingTime);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_TIME + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.TIME_HH_MM;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.TIME_HH,
						ValueFormat.TIME_HH_MM,
						ValueFormat.TIME_HH_MM_SS };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatId = valueFormat;
				valueFormatter = getFormatter_Time(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Recording time
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Time_Recording() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.TIME_RECORDING,
				Messages.Calendar_Profile_Value_RecordingTime,
				GraphColorManager.PREF_GRAPH_TIME) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.recordingTime > 0) {

					final String valueText = valueFormatter.printLong(data.recordingTime);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_TIME + UI.SPACE
							: valueText + UI.SPACE;

				} else {

					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.TIME_HH_MM;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.TIME_HH,
						ValueFormat.TIME_HH_MM,
						ValueFormat.TIME_HH_MM_SS };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatId = valueFormat;
				valueFormatter = getFormatter_Time(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Description
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Tour_Description() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.TOUR_DESCRIPTION,
				Messages.Calendar_Profile_Value_Description,
				UI.EMPTY_STRING) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				return data.tourDescription;
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.TEXT;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] { ValueFormat.TEXT };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Title
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Tour_Title() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.TOUR_TITLE,
				Messages.Calendar_Profile_Value_Title,
				UI.EMPTY_STRING) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				return data.tourTitle;
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.TEXT;
			}

			@Override
			public ValueFormat[] getValueFormats() {
				return new ValueFormat[] { ValueFormat.TEXT };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	private static void createProfile_0_AllDefaultDefaultProfiles(final ArrayList<CalendarProfile> allProfiles) {

		allProfiles.clear();

		allProfiles.add(createProfile_10_Default());

		allProfiles.add(createProfile_20_Compact());
		allProfiles.add(createProfile_22_Compact_II());
		allProfiles.add(createProfile_23_Compact_III());

		allProfiles.add(createProfile_50_Year());
		allProfiles.add(createProfile_52_Year_II());
		allProfiles.add(createProfile_54_Year_III());

		allProfiles.add(createProfile_99_Classic());

		for (final CalendarProfile profile : allProfiles) {
			profile.isDefaultDefault = true;
		}
	}

	private static CalendarProfile createProfile_10_Default() {

		final CalendarProfile profile = new CalendarProfile();

		profile.profileName = Messages.Calendar_Profile_Name_Default;

		// SET_FORMATTING_OFF

//		                                      Default

		profile.defaultId                     = DefaultId.DEFAULT;

		// layout
		profile.isToggleMonthColor            = false;
		profile.isWeekRowHeight               = true;
		profile.useDraggedScrolling           = false;
		profile.weekHeight                    = 120;
		profile.weekRows                      = 10;
		profile.alternateMonthRGB             = new RGB (60, 60, 60);
		profile.alternateMonth2RGB            = new RGB (80, 80, 80);
		profile.calendarBackgroundRGB         = new RGB (40, 40, 40);
		profile.calendarForegroundRGB         = new RGB (200, 200, 200);
		profile.dayHoveredRGB                 = new RGB (192, 192, 192);
		profile.daySelectedRGB                = new RGB (255, 255, 255);
		                                                                                           
		// 1. Date column
		profile.isShowDateColumn              = true;
		profile.dateColumnContent             = DateColumnContent.MONTH;
		profile.dateColumnFont                = CalendarProfile.createFont(1.7f, SWT.BOLD);
		profile.dateColumnWidth               = 50;
		                                                                                           
		// 2. Year columns
		profile.isShowYearColumns             = true;
		profile.isYearColumnDayWidth          = false;
		profile.yearColumns                   = 1;
		profile.yearColumnsSpacing            = 30;
		profile.yearColumnsStart              = ColumnStart.CONTINUOUSLY;
		profile.yearColumnDayWidth            = 50;
		profile.yearHeaderFont                = CalendarProfile.createFont(2.8f, SWT.BOLD);
		                                                                                           
		// 3. Week summary column
		profile.isShowSummaryColumn           = true;
		profile.isShowWeekValueUnit           = true;
		profile.weekColumnWidth               = 100;
		profile.weekMarginTop                 = 0;
		profile.weekMarginLeft                = 1;
		profile.weekMarginBottom              = 1;
		profile.weekMarginRight               = 0;
		profile.weekValueFont                 = CalendarProfile.createFont(1.2f, SWT.NORMAL);
		profile.weekValueColor                = CalendarColor.BRIGHT;
		profile.weekValueRGB                  = new RGB (255, 0, 128);
		                                                                                           
		// day date
		profile.isHideDayDateWhenNoTour       = true;
		profile.isShowDayDate                 = true;
		profile.isShowDayDateWeekendColor     = false;
		profile.dayDateMarginTop              = -4;
		profile.dayDateMarginLeft             = 3;
		profile.dayDateFont                   = CalendarProfile.createFont(1.6f, SWT.BOLD);
		profile.dayDateFormat                 = DayDateFormat.DAY;
		                                                                                           
		// day
		profile.isDayContentVertical          = true;
		                                                                                           
		// tour background
		profile.tourBackground                = TourBackground.FILL;
		profile.tourBackgroundWidth           = 3;
		profile.tourBorder                    = TourBorder.NO_BORDER;
		profile.tourBorderWidth               = 1;
		profile.tourBackground1Color          = CalendarColor.DARK;
		profile.tourBackground2Color          = CalendarColor.BRIGHT;
		profile.tourBorderColor               = CalendarColor.LINE;
		profile.tourHoveredColor              = CalendarColor.CUSTOM;
		profile.tourDraggedColor              = CalendarColor.CUSTOM;
		profile.tourSelectedColor             = CalendarColor.CUSTOM;
		profile.tourBackground1RGB            = new RGB (64, 0, 255);
		profile.tourBackground2RGB            = new RGB (128, 64, 0);
		profile.tourBorderRGB                 = new RGB (192, 128, 0);
		profile.tourDraggedRGB                = new RGB (255, 255, 0);
		profile.tourHoveredRGB                = new RGB (192, 192, 192);
		profile.tourSelectedRGB               = new RGB (255, 255, 255);
		                                                                                           
		// tour content
		profile.isShowTourContent             = true;
		profile.isShowTourValueUnit           = true;
		profile.isTruncateTourText            = true;
		profile.tourMarginTop                 = -1;
		profile.tourMarginLeft                = 1;
		profile.tourMarginBottom              = 1;
		profile.tourMarginRight               = 2;
		profile.tourTruncatedLines            = 2;
		profile.tourValueColumns              = 2;
		profile.tourContentFont               = CalendarProfile.createFont(0.9f, SWT.NORMAL);
		profile.tourTitleFont                 = CalendarProfile.createFont(1.2f, SWT.BOLD);
		profile.tourValueFont                 = CalendarProfile.createFont(1.0f, SWT.BOLD);
		profile.tourContentColor              = CalendarColor.CONTRAST;
		profile.tourTitleColor                = CalendarColor.CONTRAST;
		profile.tourValueColor                = CalendarColor.CONTRAST;
		profile.tourContentRGB                = new RGB (255, 192, 255);
		profile.tourTitleRGB                  = new RGB (191, 255, 203);
		profile.tourValueRGB                  = new RGB (191, 193, 255);


		profile.allTourFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.TOUR_TITLE,        ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.TOUR_DESCRIPTION,  ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		profile.allWeekFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.SPEED,             ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.PACE,              ValueFormat.PACE_MM_SS),
			new FormatterData(true,      FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		// SET_FORMATTING_ON

		return profile;
	}

	private static CalendarProfile createProfile_20_Compact() {

		final CalendarProfile profile = new CalendarProfile();

		profile.profileName = Messages.Calendar_Profile_Name_Compact;

		// SET_FORMATTING_OFF

//		                                      Compact

		profile.defaultId                     = DefaultId.COMPACT;

		// layout
		profile.isToggleMonthColor            = false;
		profile.isWeekRowHeight               = true;
		profile.useDraggedScrolling           = false;
		profile.weekHeight                    = 28;
		profile.weekRows                      = 10;
		profile.alternateMonthRGB             = new RGB (60, 60, 60);
		profile.alternateMonth2RGB            = new RGB (80, 80, 80);
		profile.calendarBackgroundRGB         = new RGB (40, 40, 40);
		profile.calendarForegroundRGB         = new RGB (200, 200, 200);
		profile.dayHoveredRGB                 = new RGB (192, 192, 192);
		profile.daySelectedRGB                = new RGB (255, 255, 255);
		                                                                                           
		// 1. Date column
		profile.isShowDateColumn              = true;
		profile.dateColumnContent             = DateColumnContent.MONTH;
		profile.dateColumnFont                = CalendarProfile.createFont(1.4f, SWT.BOLD);
		profile.dateColumnWidth               = 50;
		                                                                                           
		// 2. Year columns
		profile.isShowYearColumns             = true;
		profile.isYearColumnDayWidth          = false;
		profile.yearColumns                   = 1;
		profile.yearColumnsSpacing            = 30;
		profile.yearColumnsStart              = ColumnStart.CONTINUOUSLY;
		profile.yearColumnDayWidth            = 50;
		profile.yearHeaderFont                = CalendarProfile.createFont(1.9f, SWT.BOLD);
		                                                                                           
		// 3. Week summary column
		profile.isShowSummaryColumn           = true;
		profile.isShowWeekValueUnit           = false;
		profile.weekColumnWidth               = 50;
		profile.weekMarginTop                 = 0;
		profile.weekMarginLeft                = 1;
		profile.weekMarginBottom              = 1;
		profile.weekMarginRight               = 0;
		profile.weekValueFont                 = CalendarProfile.createFont(0.9f, SWT.NORMAL);
		profile.weekValueColor                = CalendarColor.BRIGHT;
		profile.weekValueRGB                  = new RGB (255, 0, 128);
		                                                                                           
		// day date
		profile.isHideDayDateWhenNoTour       = true;
		profile.isShowDayDate                 = false;
		profile.isShowDayDateWeekendColor     = false;
		profile.dayDateMarginTop              = -2;
		profile.dayDateMarginLeft             = 2;
		profile.dayDateFont                   = CalendarProfile.createFont(1.2f, SWT.BOLD);
		profile.dayDateFormat                 = DayDateFormat.DAY;
		                                                                                           
		// day
		profile.isDayContentVertical          = false;
		                                                                                           
		// tour background
		profile.tourBackground                = TourBackground.FILL;
		profile.tourBackgroundWidth           = 3;
		profile.tourBorder                    = TourBorder.NO_BORDER;
		profile.tourBorderWidth               = 1;
		profile.tourBackground1Color          = CalendarColor.DARK;
		profile.tourBackground2Color          = CalendarColor.BRIGHT;
		profile.tourBorderColor               = CalendarColor.LINE;
		profile.tourHoveredColor              = CalendarColor.CUSTOM;
		profile.tourDraggedColor              = CalendarColor.CUSTOM;
		profile.tourSelectedColor             = CalendarColor.CUSTOM;
		profile.tourBackground1RGB            = new RGB (64, 0, 255);
		profile.tourBackground2RGB            = new RGB (128, 64, 0);
		profile.tourBorderRGB                 = new RGB (192, 128, 0);
		profile.tourDraggedRGB                = new RGB (255, 255, 0);
		profile.tourHoveredRGB                = new RGB (192, 192, 192);
		profile.tourSelectedRGB               = new RGB (255, 255, 255);
		                                                                                           
		// tour content
		profile.isShowTourContent             = true;
		profile.isShowTourValueUnit           = false;
		profile.isTruncateTourText            = false;
		profile.tourMarginTop                 = -2;
		profile.tourMarginLeft                = 1;
		profile.tourMarginBottom              = 0;
		profile.tourMarginRight               = 2;
		profile.tourTruncatedLines            = 1;
		profile.tourValueColumns              = 3;
		profile.tourContentFont               = CalendarProfile.createFont(0.9f, SWT.NORMAL);
		profile.tourTitleFont                 = CalendarProfile.createFont(1.0f, SWT.BOLD);
		profile.tourValueFont                 = CalendarProfile.createFont(1.0f, SWT.BOLD);
		profile.tourContentColor              = CalendarColor.CONTRAST;
		profile.tourTitleColor                = CalendarColor.CONTRAST;
		profile.tourValueColor                = CalendarColor.CONTRAST;
		profile.tourContentRGB                = new RGB (255, 192, 255);
		profile.tourTitleRGB                  = new RGB (191, 255, 203);
		profile.tourValueRGB                  = new RGB (191, 193, 255);


		profile.allTourFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.TOUR_TITLE,        ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.TOUR_DESCRIPTION,  ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		profile.allWeekFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.SPEED,             ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.PACE,              ValueFormat.PACE_MM_SS),
			new FormatterData(false,     FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		// SET_FORMATTING_ON

		return profile;
	}

	private static CalendarProfile createProfile_22_Compact_II() {

		final CalendarProfile profile = new CalendarProfile();

		profile.profileName = Messages.Calendar_Profile_Name_Compact_II;

		// SET_FORMATTING_OFF

//		                                      Compact II

		profile.defaultId                     = DefaultId.COMPACT_II;

		// layout
		profile.isToggleMonthColor            = false;
		profile.isWeekRowHeight               = true;
		profile.useDraggedScrolling           = false;
		profile.weekHeight                    = 14;
		profile.weekRows                      = 10;
		profile.alternateMonthRGB             = new RGB (60, 60, 60);
		profile.alternateMonth2RGB            = new RGB (80, 80, 80);
		profile.calendarBackgroundRGB         = new RGB (40, 40, 40);
		profile.calendarForegroundRGB         = new RGB (200, 200, 200);
		profile.dayHoveredRGB                 = new RGB (192, 192, 192);
		profile.daySelectedRGB                = new RGB (255, 255, 255);
		                                                                                           
		// 1. Date column
		profile.isShowDateColumn              = true;
		profile.dateColumnContent             = DateColumnContent.MONTH;
		profile.dateColumnFont                = CalendarProfile.createFont(1.2f, SWT.BOLD);
		profile.dateColumnWidth               = 50;
		                                                                                           
		// 2. Year columns
		profile.isShowYearColumns             = true;
		profile.isYearColumnDayWidth          = false;
		profile.yearColumns                   = 1;
		profile.yearColumnsSpacing            = 30;
		profile.yearColumnsStart              = ColumnStart.CONTINUOUSLY;
		profile.yearColumnDayWidth            = 50;
		profile.yearHeaderFont                = CalendarProfile.createFont(1.7f, SWT.BOLD);
		                                                                                           
		// 3. Week summary column
		profile.isShowSummaryColumn           = true;
		profile.isShowWeekValueUnit           = false;
		profile.weekColumnWidth               = 50;
		profile.weekMarginTop                 = 0;
		profile.weekMarginLeft                = 1;
		profile.weekMarginBottom              = 1;
		profile.weekMarginRight               = 0;
		profile.weekValueFont                 = CalendarProfile.createFont(1.0f, SWT.NORMAL);
		profile.weekValueColor                = CalendarColor.BRIGHT;
		profile.weekValueRGB                  = new RGB (255, 0, 128);
		                                                                                           
		// day date
		profile.isHideDayDateWhenNoTour       = true;
		profile.isShowDayDate                 = false;
		profile.isShowDayDateWeekendColor     = false;
		profile.dayDateMarginTop              = -1;
		profile.dayDateMarginLeft             = 2;
		profile.dayDateFont                   = CalendarProfile.createFont(1.0f, SWT.BOLD);
		profile.dayDateFormat                 = DayDateFormat.DAY;
		                                                                                           
		// day
		profile.isDayContentVertical          = false;
		                                                                                           
		// tour background
		profile.tourBackground                = TourBackground.FILL;
		profile.tourBackgroundWidth           = 3;
		profile.tourBorder                    = TourBorder.NO_BORDER;
		profile.tourBorderWidth               = 1;
		profile.tourBackground1Color          = CalendarColor.DARK;
		profile.tourBackground2Color          = CalendarColor.BRIGHT;
		profile.tourBorderColor               = CalendarColor.LINE;
		profile.tourHoveredColor              = CalendarColor.CUSTOM;
		profile.tourDraggedColor              = CalendarColor.CUSTOM;
		profile.tourSelectedColor             = CalendarColor.CUSTOM;
		profile.tourBackground1RGB            = new RGB (64, 0, 255);
		profile.tourBackground2RGB            = new RGB (128, 64, 0);
		profile.tourBorderRGB                 = new RGB (192, 128, 0);
		profile.tourDraggedRGB                = new RGB (255, 255, 0);
		profile.tourHoveredRGB                = new RGB (192, 192, 192);
		profile.tourSelectedRGB               = new RGB (255, 255, 255);
		                                                                                           
		// tour content
		profile.isShowTourContent             = true;
		profile.isShowTourValueUnit           = false;
		profile.isTruncateTourText            = true;
		profile.tourMarginTop                 = -1;
		profile.tourMarginLeft                = 1;
		profile.tourMarginBottom              = 0;
		profile.tourMarginRight               = 2;
		profile.tourTruncatedLines            = 1;
		profile.tourValueColumns              = 3;
		profile.tourContentFont               = CalendarProfile.createFont(0.9f, SWT.NORMAL);
		profile.tourTitleFont                 = CalendarProfile.createFont(1.0f, SWT.NORMAL);
		profile.tourValueFont                 = CalendarProfile.createFont(1.0f, SWT.NORMAL);
		profile.tourContentColor              = CalendarColor.CONTRAST;
		profile.tourTitleColor                = CalendarColor.CONTRAST;
		profile.tourValueColor                = CalendarColor.CONTRAST;
		profile.tourContentRGB                = new RGB (255, 192, 255);
		profile.tourTitleRGB                  = new RGB (191, 255, 203);
		profile.tourValueRGB                  = new RGB (191, 193, 255);


		profile.allTourFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.TOUR_TITLE,        ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.TOUR_DESCRIPTION,  ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.TIME_MOVING,       ValueFormat.TIME_HH),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		profile.allWeekFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.SPEED,             ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.PACE,              ValueFormat.PACE_MM_SS),
			new FormatterData(false,     FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		// SET_FORMATTING_ON

		return profile;
	}

	private static CalendarProfile createProfile_23_Compact_III() {

		final CalendarProfile profile = new CalendarProfile();

		profile.profileName = Messages.Calendar_Profile_Name_Compact_III;

		// SET_FORMATTING_OFF

//		                                      Compact III

		profile.defaultId                     = DefaultId.COMPACT_III;

		// layout
		profile.isToggleMonthColor            = false;
		profile.isWeekRowHeight               = true;
		profile.useDraggedScrolling           = false;
		profile.weekHeight                    = 10;
		profile.weekRows                      = 10;
		profile.alternateMonthRGB             = new RGB (60, 60, 60);
		profile.alternateMonth2RGB            = new RGB (80, 80, 80);
		profile.calendarBackgroundRGB         = new RGB (40, 40, 40);
		profile.calendarForegroundRGB         = new RGB (200, 200, 200);
		profile.dayHoveredRGB                 = new RGB (192, 192, 192);
		profile.daySelectedRGB                = new RGB (255, 255, 255);
		                                                                                           
		// 1. Date column
		profile.isShowDateColumn              = true;
		profile.dateColumnContent             = DateColumnContent.MONTH;
		profile.dateColumnFont                = CalendarProfile.createFont(1.0f, SWT.NORMAL);
		profile.dateColumnWidth               = 50;
		                                                                                           
		// 2. Year columns
		profile.isShowYearColumns             = true;
		profile.isYearColumnDayWidth          = false;
		profile.yearColumns                   = 1;
		profile.yearColumnsSpacing            = 30;
		profile.yearColumnsStart              = ColumnStart.CONTINUOUSLY;
		profile.yearColumnDayWidth            = 50;
		profile.yearHeaderFont                = CalendarProfile.createFont(1.7f, SWT.BOLD);
		                                                                                           
		// 3. Week summary column
		profile.isShowSummaryColumn           = true;
		profile.isShowWeekValueUnit           = false;
		profile.weekColumnWidth               = 30;
		profile.weekMarginTop                 = -1;
		profile.weekMarginLeft                = 0;
		profile.weekMarginBottom              = -1;
		profile.weekMarginRight               = 0;
		profile.weekValueFont                 = CalendarProfile.createFont(0.8f, SWT.NORMAL);
		profile.weekValueColor                = CalendarColor.BRIGHT;
		profile.weekValueRGB                  = new RGB (255, 0, 128);
		                                                                                           
		// day date
		profile.isHideDayDateWhenNoTour       = true;
		profile.isShowDayDate                 = false;
		profile.isShowDayDateWeekendColor     = false;
		profile.dayDateMarginTop              = -2;
		profile.dayDateMarginLeft             = 1;
		profile.dayDateFont                   = CalendarProfile.createFont(0.8f, SWT.BOLD);
		profile.dayDateFormat                 = DayDateFormat.DAY;
		                                                                                           
		// day
		profile.isDayContentVertical          = false;
		                                                                                           
		// tour background
		profile.tourBackground                = TourBackground.FILL;
		profile.tourBackgroundWidth           = 3;
		profile.tourBorder                    = TourBorder.NO_BORDER;
		profile.tourBorderWidth               = 1;
		profile.tourBackground1Color          = CalendarColor.DARK;
		profile.tourBackground2Color          = CalendarColor.BRIGHT;
		profile.tourBorderColor               = CalendarColor.LINE;
		profile.tourHoveredColor              = CalendarColor.CUSTOM;
		profile.tourDraggedColor              = CalendarColor.CUSTOM;
		profile.tourSelectedColor             = CalendarColor.CUSTOM;
		profile.tourBackground1RGB            = new RGB (64, 0, 255);
		profile.tourBackground2RGB            = new RGB (128, 64, 0);
		profile.tourBorderRGB                 = new RGB (192, 128, 0);
		profile.tourDraggedRGB                = new RGB (255, 255, 0);
		profile.tourHoveredRGB                = new RGB (192, 192, 192);
		profile.tourSelectedRGB               = new RGB (255, 255, 255);
		                                                                                           
		// tour content
		profile.isShowTourContent             = false;
		profile.isShowTourValueUnit           = false;
		profile.isTruncateTourText            = true;
		profile.tourMarginTop                 = -2;
		profile.tourMarginLeft                = 1;
		profile.tourMarginBottom              = 0;
		profile.tourMarginRight               = 2;
		profile.tourTruncatedLines            = 1;
		profile.tourValueColumns              = 3;
		profile.tourContentFont               = CalendarProfile.createFont(0.8f, SWT.NORMAL);
		profile.tourTitleFont                 = CalendarProfile.createFont(0.8f, SWT.NORMAL);
		profile.tourValueFont                 = CalendarProfile.createFont(0.8f, SWT.NORMAL);
		profile.tourContentColor              = CalendarColor.CONTRAST;
		profile.tourTitleColor                = CalendarColor.CONTRAST;
		profile.tourValueColor                = CalendarColor.CONTRAST;
		profile.tourContentRGB                = new RGB (255, 192, 255);
		profile.tourTitleRGB                  = new RGB (191, 255, 203);
		profile.tourValueRGB                  = new RGB (191, 193, 255);


		profile.allTourFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.TOUR_TITLE,        ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.TOUR_DESCRIPTION,  ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.TIME_MOVING,       ValueFormat.TIME_HH),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		profile.allWeekFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.SPEED,             ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.PACE,              ValueFormat.PACE_MM_SS),
			new FormatterData(false,     FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		// SET_FORMATTING_ON

		return profile;
	}

	private static CalendarProfile createProfile_50_Year() {

		final CalendarProfile profile = new CalendarProfile();

		profile.profileName = Messages.Calendar_Profile_Name_Year;

		// SET_FORMATTING_OFF

//		                                      Year

		profile.defaultId                     = DefaultId.YEAR;

		// layout
		profile.isToggleMonthColor            = false;
		profile.isWeekRowHeight               = true;
		profile.useDraggedScrolling           = false;
		profile.weekHeight                    = 14;
		profile.weekRows                      = 10;
		profile.alternateMonthRGB             = new RGB (60, 60, 60);
		profile.alternateMonth2RGB            = new RGB (80, 80, 80);
		profile.calendarBackgroundRGB         = new RGB (40, 40, 40);
		profile.calendarForegroundRGB         = new RGB (200, 200, 200);
		profile.dayHoveredRGB                 = new RGB (192, 192, 192);
		profile.daySelectedRGB                = new RGB (255, 255, 255);
		                                                                                           
		// 1. Date column
		profile.isShowDateColumn              = false;
		profile.dateColumnContent             = DateColumnContent.MONTH;
		profile.dateColumnFont                = CalendarProfile.createFont(1.0f, SWT.BOLD);
		profile.dateColumnWidth               = 15;
		                                                                                           
		// 2. Year columns
		profile.isShowYearColumns             = true;
		profile.isYearColumnDayWidth          = true;
		profile.yearColumns                   = 5;
		profile.yearColumnsSpacing            = 20;
		profile.yearColumnsStart              = ColumnStart.JAN;
		profile.yearColumnDayWidth            = 14;
		profile.yearHeaderFont                = CalendarProfile.createFont(1.7f, SWT.BOLD);
		                                                                                           
		// 3. Week summary column
		profile.isShowSummaryColumn           = true;
		profile.isShowWeekValueUnit           = false;
		profile.weekColumnWidth               = 40;
		profile.weekMarginTop                 = -2;
		profile.weekMarginLeft                = 0;
		profile.weekMarginBottom              = 0;
		profile.weekMarginRight               = 0;
		profile.weekValueFont                 = CalendarProfile.createFont(1.0f, SWT.NORMAL);
		profile.weekValueColor                = CalendarColor.BRIGHT;
		profile.weekValueRGB                  = new RGB (255, 0, 128);
		                                                                                           
		// day date
		profile.isHideDayDateWhenNoTour       = true;
		profile.isShowDayDate                 = false;
		profile.isShowDayDateWeekendColor     = false;
		profile.dayDateMarginTop              = -1;
		profile.dayDateMarginLeft             = 4;
		profile.dayDateFont                   = CalendarProfile.createFont(1.0f, SWT.NORMAL);
		profile.dayDateFormat                 = DayDateFormat.DAY;
		                                                                                           
		// day
		profile.isDayContentVertical          = true;
		                                                                                           
		// tour background
		profile.tourBackground                = TourBackground.CIRCLE;
		profile.tourBackgroundWidth           = 3;
		profile.tourBorder                    = TourBorder.NO_BORDER;
		profile.tourBorderWidth               = 1;
		profile.tourBackground1Color          = CalendarColor.DARK;
		profile.tourBackground2Color          = CalendarColor.BRIGHT;
		profile.tourBorderColor               = CalendarColor.LINE;
		profile.tourHoveredColor              = CalendarColor.CUSTOM;
		profile.tourDraggedColor              = CalendarColor.CUSTOM;
		profile.tourSelectedColor             = CalendarColor.CUSTOM;
		profile.tourBackground1RGB            = new RGB (64, 0, 255);
		profile.tourBackground2RGB            = new RGB (128, 64, 0);
		profile.tourBorderRGB                 = new RGB (192, 128, 0);
		profile.tourDraggedRGB                = new RGB (255, 255, 0);
		profile.tourHoveredRGB                = new RGB (192, 192, 192);
		profile.tourSelectedRGB               = new RGB (255, 255, 255);
		                                                                                           
		// tour content
		profile.isShowTourContent             = false;
		profile.isShowTourValueUnit           = false;
		profile.isTruncateTourText            = false;
		profile.tourMarginTop                 = 1;
		profile.tourMarginLeft                = 0;
		profile.tourMarginBottom              = 0;
		profile.tourMarginRight               = -7;
		profile.tourTruncatedLines            = 1;
		profile.tourValueColumns              = 1;
		profile.tourContentFont               = CalendarProfile.createFont(0.9f, SWT.NORMAL);
		profile.tourTitleFont                 = CalendarProfile.createFont(1.2f, SWT.BOLD);
		profile.tourValueFont                 = CalendarProfile.createFont(1.0f, SWT.BOLD);
		profile.tourContentColor              = CalendarColor.CONTRAST;
		profile.tourTitleColor                = CalendarColor.CONTRAST;
		profile.tourValueColor                = CalendarColor.CONTRAST;
		profile.tourContentRGB                = new RGB (255, 192, 255);
		profile.tourTitleRGB                  = new RGB (191, 255, 203);
		profile.tourValueRGB                  = new RGB (191, 193, 255);


		profile.allTourFormatterData          = new FormatterData[] {

			new FormatterData(false,     FormatterID.TOUR_TITLE,        ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.TOUR_DESCRIPTION,  ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		profile.allWeekFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.SPEED,             ValueFormat.NUMBER_1_0),
			new FormatterData(false,     FormatterID.PACE,              ValueFormat.PACE_MM_SS),
			new FormatterData(false,     FormatterID.TIME_MOVING,       ValueFormat.TIME_HH),
			new FormatterData(false,     FormatterID.TIME_PAUSED,       ValueFormat.TIME_HH),

		};;

		// SET_FORMATTING_ON

		return profile;
	}

	private static CalendarProfile createProfile_52_Year_II() {

		final CalendarProfile profile = new CalendarProfile();

		profile.profileName = Messages.Calendar_Profile_Name_Year_II;

		// SET_FORMATTING_OFF

//		                                      Year II

		profile.defaultId                     = DefaultId.YEAR_II;

		// layout
		profile.isToggleMonthColor            = false;
		profile.isWeekRowHeight               = true;
		profile.useDraggedScrolling           = false;
		profile.weekHeight                    = 10;
		profile.weekRows                      = 10;
		profile.alternateMonthRGB             = new RGB (60, 60, 60);
		profile.calendarBackgroundRGB         = new RGB (40, 40, 40);
		profile.calendarForegroundRGB         = new RGB (200, 200, 200);
		profile.dayHoveredRGB                 = new RGB (192, 192, 192);
		profile.daySelectedRGB                = new RGB (255, 255, 255);
		                                                                                           
		// 1. Date column
		profile.isShowDateColumn              = false;
		profile.dateColumnContent             = DateColumnContent.MONTH;
		profile.dateColumnFont                = CalendarProfile.createFont(1.5f, SWT.BOLD);
		profile.dateColumnWidth               = 50;
		                                                                                           
		// 2. Year columns
		profile.isShowYearColumns             = true;
		profile.isYearColumnDayWidth          = true;
		profile.yearColumns                   = 20;
		profile.yearColumnsSpacing            = 0;
		profile.yearColumnsStart              = ColumnStart.JAN;
		profile.yearColumnDayWidth            = 10;
		profile.yearHeaderFont                = CalendarProfile.createFont(1.4f, SWT.BOLD);
		                                                                                           
		// 3. Week summary column
		profile.isShowSummaryColumn           = false;
		profile.isShowWeekValueUnit           = true;
		profile.weekColumnWidth               = 60;
		profile.weekMarginTop                 = -3;
		profile.weekMarginLeft                = 1;
		profile.weekMarginBottom              = 1;
		profile.weekMarginRight               = -3;
		profile.weekValueFont                 = CalendarProfile.createFont(1.2f, SWT.BOLD);
		profile.weekValueColor                = CalendarColor.TEXT;
		profile.weekValueRGB                  = new RGB (255, 0, 128);
		                                                                                           
		// day date
		profile.isHideDayDateWhenNoTour       = true;
		profile.isShowDayDate                 = false;
		profile.isShowDayDateWeekendColor     = false;
		profile.dayDateMarginTop              = 0;
		profile.dayDateMarginLeft             = 0;
		profile.dayDateFont                   = CalendarProfile.createFont(1.2f, SWT.BOLD);
		profile.dayDateFormat                 = DayDateFormat.DAY;
		                                                                                           
		// tour background
		profile.tourBackground                = TourBackground.CIRCLE;
		profile.tourBackgroundWidth           = 3;
		profile.tourBorder                    = TourBorder.BORDER_ALL;
		profile.tourBorderWidth               = 0;
		profile.tourBackground1Color          = CalendarColor.DARK;
		profile.tourBackground2Color          = CalendarColor.WHITE;
		profile.tourBorderColor               = CalendarColor.DARK;
		profile.tourHoveredColor              = CalendarColor.CUSTOM;
		profile.tourDraggedColor              = CalendarColor.CUSTOM;
		profile.tourSelectedColor             = CalendarColor.CUSTOM;
		profile.tourBackground1RGB            = new RGB (64, 0, 255);
		profile.tourBackground2RGB            = new RGB (128, 64, 0);
		profile.tourBorderRGB                 = new RGB (192, 128, 0);
		profile.tourDraggedRGB                = new RGB (255, 255, 0);
		profile.tourHoveredRGB                = new RGB (192, 192, 192);
		profile.tourSelectedRGB               = new RGB (255, 255, 255);
		                                                                                           
		// tour content
		profile.isShowTourContent             = false;
		profile.isShowTourValueUnit           = true;
		profile.isTruncateTourText            = true;
		profile.tourMarginTop                 = -3;
		profile.tourMarginLeft                = 1;
		profile.tourMarginBottom              = 1;
		profile.tourMarginRight               = -3;
		profile.tourTruncatedLines            = 2;
		profile.tourValueColumns              = 2;
		profile.tourContentFont               = CalendarProfile.createFont(0.9f, SWT.NORMAL);
		profile.tourTitleFont                 = CalendarProfile.createFont(1.2f, SWT.BOLD);
		profile.tourValueFont                 = CalendarProfile.createFont(1.0f, SWT.NORMAL);
		profile.tourContentColor              = CalendarColor.CONTRAST;
		profile.tourTitleColor                = CalendarColor.CONTRAST;
		profile.tourValueColor                = CalendarColor.CONTRAST;
		profile.tourContentRGB                = new RGB (255, 192, 255);
		profile.tourTitleRGB                  = new RGB (191, 255, 203);
		profile.tourValueRGB                  = new RGB (191, 193, 255);


		profile.allTourFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.TOUR_TITLE,        ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.TOUR_DESCRIPTION,  ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		profile.allWeekFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.SPEED,             ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.PACE,              ValueFormat.PACE_MM_SS),
			new FormatterData(true,      FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		// SET_FORMATTING_ON

		return profile;
	}

	private static CalendarProfile createProfile_54_Year_III() {

		final CalendarProfile profile = new CalendarProfile();

		profile.profileName = Messages.Calendar_Profile_Name_Year_III;

		// SET_FORMATTING_OFF

//		                                      Year III

		profile.defaultId                     = DefaultId.YEAR_III;
		// layout
		profile.isToggleMonthColor            = false;
		profile.isWeekRowHeight               = true;
		profile.useDraggedScrolling           = false;
		profile.weekHeight                    = 5;
		profile.weekRows                      = 10;
		profile.alternateMonthRGB             = new RGB (60, 60, 60);
		profile.calendarBackgroundRGB         = new RGB (40, 40, 40);
		profile.calendarForegroundRGB         = new RGB (200, 200, 200);
		profile.dayHoveredRGB                 = new RGB (192, 192, 192);
		profile.daySelectedRGB                = new RGB (255, 255, 255);
		                                                                                           
		// 1. Date column
		profile.isShowDateColumn              = false;
		profile.dateColumnContent             = DateColumnContent.MONTH;
		profile.dateColumnFont                = CalendarProfile.createFont(1.5f, SWT.BOLD);
		profile.dateColumnWidth               = 50;
		                                                                                           
		// 2. Year columns
		profile.isShowYearColumns             = true;
		profile.isYearColumnDayWidth          = true;
		profile.yearColumns                   = 20;
		profile.yearColumnsSpacing            = 0;
		profile.yearColumnsStart              = ColumnStart.JAN;
		profile.yearColumnDayWidth            = 5;
		profile.yearHeaderFont                = CalendarProfile.createFont(0.9f, SWT.NORMAL);
		                                                                                           
		// 3. Week summary column
		profile.isShowSummaryColumn           = false;
		profile.isShowWeekValueUnit           = true;
		profile.weekColumnWidth               = 60;
		profile.weekMarginTop                 = -3;
		profile.weekMarginLeft                = 1;
		profile.weekMarginBottom              = 1;
		profile.weekMarginRight               = -3;
		profile.weekValueColor                = CalendarColor.TEXT;
		profile.weekValueFont                 = CalendarProfile.createFont(1.2f, SWT.BOLD);
		                                                                                           
		// day date
		profile.isHideDayDateWhenNoTour       = true;
		profile.isShowDayDate                 = false;
		profile.isShowDayDateWeekendColor     = false;
		profile.dayDateFont                   = CalendarProfile.createFont(1.2f, SWT.BOLD);
		profile.dayDateFormat                 = DayDateFormat.DAY;
		                                                                                           
		// tour background
		profile.tourBackground                = TourBackground.FILL;
		profile.tourBackgroundWidth           = 3;
		profile.tourBorder                    = TourBorder.BORDER_ALL;
		profile.tourBorderWidth               = 1;
		profile.tourBackground1Color          = CalendarColor.DARK;
		profile.tourBackground2Color          = CalendarColor.WHITE;
		profile.tourBorderColor               = CalendarColor.DARK;
		profile.tourHoveredColor              = CalendarColor.CUSTOM;
		profile.tourDraggedColor              = CalendarColor.CUSTOM;
		profile.tourSelectedColor             = CalendarColor.CUSTOM;
		profile.tourBackground1RGB            = new RGB (64, 0, 255);
		profile.tourBackground2RGB            = new RGB (128, 64, 0);
		profile.tourBorderRGB                 = new RGB (192, 128, 0);
		profile.tourDraggedRGB                = new RGB (255, 255, 0);
		profile.tourHoveredRGB                = new RGB (192, 192, 192);
		profile.tourSelectedRGB               = new RGB (255, 255, 255);
		                                                                                           
		// tour content
		profile.isShowTourContent             = false;
		profile.isShowTourValueUnit           = true;
		profile.isTruncateTourText            = true;
		profile.tourMarginTop                 = -3;
		profile.tourMarginLeft                = 1;
		profile.tourMarginBottom              = 1;
		profile.tourMarginRight               = -3;
		profile.tourTruncatedLines            = 2;
		profile.tourValueColumns              = 2;
		profile.tourContentFont               = CalendarProfile.createFont(0.9f, SWT.NORMAL);
		profile.tourTitleFont                 = CalendarProfile.createFont(1.2f, SWT.BOLD);
		profile.tourValueFont                 = CalendarProfile.createFont(1.0f, SWT.NORMAL);
		profile.tourContentColor              = CalendarColor.CONTRAST;
		profile.tourTitleColor                = CalendarColor.CONTRAST;
		profile.tourValueColor                = CalendarColor.CONTRAST;
		profile.tourContentRGB                = new RGB (255, 192, 255);
		profile.tourTitleRGB                  = new RGB (191, 255, 203);
		profile.tourValueRGB                  = new RGB (191, 193, 255);


		profile.allTourFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.TOUR_TITLE,        ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.TOUR_DESCRIPTION,  ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		profile.allWeekFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.SPEED,             ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.PACE,              ValueFormat.PACE_MM_SS),
			new FormatterData(true,      FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		// SET_FORMATTING_ON

		return profile;
	}

	private static CalendarProfile createProfile_99_Classic() {

		final CalendarProfile profile = new CalendarProfile();

		profile.profileName = Messages.Calendar_Profile_Name_Classic;

		// SET_FORMATTING_OFF

//		                                      Classic

		profile.defaultId                     = DefaultId.CLASSIC;

		// layout
		profile.isToggleMonthColor            = true;
		profile.isWeekRowHeight               = false;
		profile.useDraggedScrolling           = false;
		profile.weekHeight                    = 150;
		profile.weekRows                      = 5;
		profile.alternateMonthRGB             = new RGB (240, 240, 240);
		profile.alternateMonth2RGB            = new RGB (225, 225, 225);
		profile.calendarBackgroundRGB         = new RGB (255, 255, 255);
		profile.calendarForegroundRGB         = new RGB (0, 0, 0);
		profile.dayHoveredRGB                 = new RGB (153, 153, 153);
		profile.daySelectedRGB                = new RGB (77, 77, 77);
		profile.dayTodayRGB                   = new RGB (0, 0, 255);
		                                                                                           
		// 1. Date column
		profile.isShowDateColumn              = true;
		profile.dateColumnContent             = DateColumnContent.WEEK_NUMBER;
		profile.dateColumnFont                = CalendarProfile.createFont(1.5f, SWT.BOLD);
		profile.dateColumnWidth               = 50;
		                                                                                           
		// 2. Year columns
		profile.isShowYearColumns             = false;
		profile.isYearColumnDayWidth          = false;
		profile.yearColumns                   = 2;
		profile.yearColumnsSpacing            = 30;
		profile.yearColumnsStart              = ColumnStart.CONTINUOUSLY;
		profile.yearColumnDayWidth            = 50;
		profile.yearHeaderFont                = CalendarProfile.createFont(2.2f, SWT.BOLD);
		                                                                                           
		// 3. Week summary column
		profile.isShowSummaryColumn           = true;
		profile.isShowWeekValueUnit           = true;
		profile.weekColumnWidth               = 100;
		profile.weekMarginTop                 = -3;
		profile.weekMarginLeft                = 1;
		profile.weekMarginBottom              = 1;
		profile.weekMarginRight               = -3;
		profile.weekValueFont                 = CalendarProfile.createFont(1.2f, SWT.BOLD);
		profile.weekValueColor                = CalendarColor.TEXT;
		profile.weekValueRGB                  = new RGB (255, 0, 128);
		                                                                                           
		// day date
		profile.isHideDayDateWhenNoTour       = false;
		profile.isShowDayDate                 = true;
		profile.isShowDayDateWeekendColor     = true;
		profile.dayDateMarginTop              = -3;
		profile.dayDateMarginLeft             = 2;
		profile.dayDateFont                   = CalendarProfile.createFont(1.2f, SWT.NORMAL);
		profile.dayDateFormat                 = DayDateFormat.AUTOMATIC;
		                                                                                           
		// day
		profile.isDayContentVertical          = true;
		                                                                                           
		// tour background
		profile.tourBackground                = TourBackground.FILL;
		profile.tourBackgroundWidth           = 3;
		profile.tourBorder                    = TourBorder.NO_BORDER;
		profile.tourBorderWidth               = 1;
		profile.tourBackground1Color          = CalendarColor.DARK;
		profile.tourBackground2Color          = CalendarColor.BRIGHT;
		profile.tourBorderColor               = CalendarColor.LINE;
		profile.tourHoveredColor              = CalendarColor.CUSTOM;
		profile.tourDraggedColor              = CalendarColor.CUSTOM;
		profile.tourSelectedColor             = CalendarColor.CUSTOM;
		profile.tourBackground1RGB            = new RGB (64, 0, 255);
		profile.tourBackground2RGB            = new RGB (128, 64, 0);
		profile.tourBorderRGB                 = new RGB (192, 128, 0);
		profile.tourDraggedRGB                = new RGB (0, 0, 255);
		profile.tourHoveredRGB                = new RGB (162, 162, 162);
		profile.tourSelectedRGB               = new RGB (53, 53, 53);
		                                                                                           
		// tour content
		profile.isShowTourContent             = true;
		profile.isShowTourValueUnit           = true;
		profile.isTruncateTourText            = true;
		profile.tourMarginTop                 = -2;
		profile.tourMarginLeft                = 1;
		profile.tourMarginBottom              = 1;
		profile.tourMarginRight               = -3;
		profile.tourTruncatedLines            = 2;
		profile.tourValueColumns              = 2;
		profile.tourContentFont               = CalendarProfile.createFont(0.9f, SWT.NORMAL);
		profile.tourTitleFont                 = CalendarProfile.createFont(1.3f, SWT.BOLD);
		profile.tourValueFont                 = CalendarProfile.createFont(1.2f, SWT.BOLD);
		profile.tourContentColor              = CalendarColor.CONTRAST;
		profile.tourTitleColor                = CalendarColor.CONTRAST;
		profile.tourValueColor                = CalendarColor.CONTRAST;
		profile.tourContentRGB                = new RGB (255, 192, 255);
		profile.tourTitleRGB                  = new RGB (0, 255, 192);
		profile.tourValueRGB                  = new RGB (255, 0, 64);


		profile.allTourFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.TOUR_TITLE,        ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.TOUR_DESCRIPTION,  ValueFormat.DUMMY_VALUE),
			new FormatterData(true,      FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		profile.allWeekFormatterData          = new FormatterData[] {

			new FormatterData(true,      FormatterID.ALTITUDE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.DISTANCE,          ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.SPEED,             ValueFormat.NUMBER_1_0),
			new FormatterData(true,      FormatterID.PACE,              ValueFormat.PACE_MM_SS),
			new FormatterData(true,      FormatterID.TIME_MOVING,       ValueFormat.TIME_HH_MM),
			new FormatterData(false,     FormatterID.EMPTY,             ValueFormat.DUMMY_VALUE),

		};;

		// SET_FORMATTING_ON

		return profile;
	}

	static CalendarProfile createProfileFromId(final CalendarProfile profile) {

		if (profile.defaultId == DefaultId.USER_ID) {

			CalendarProfile userProfile = null;

			for (final CalendarProfile allProfile : getAllCalendarProfiles()) {

				if (allProfile.isUserParentDefault && allProfile.userParentDefaultId.equals(profile.userDefaultId)) {

					userProfile = allProfile;
					break;
				}
			}

			final CalendarProfile clonedProfile = userProfile.clone();

			return clonedProfile;

		} else {

// SET_FORMATTING_OFF
		
			switch (profile.defaultId) {
			
			case CLASSIC:				return createProfile_99_Classic();
	
			case COMPACT:				return createProfile_20_Compact();
			case COMPACT_II:			return createProfile_22_Compact_II();
			case COMPACT_III:			return createProfile_23_Compact_III();
			
			case YEAR:					return createProfile_50_Year();
			case YEAR_II:				return createProfile_52_Year_II();
			case YEAR_III:				return createProfile_54_Year_III();
			
			case DEFAULT:
			default:
				// create default default
				return createProfile_10_Default();
		}
		
// SET_FORMATTING_ON
		}
	}

	static CalendarProfile getActiveCalendarProfile() {

		if (_activeCalendarProfile == null) {
			readProfileFromXml();
		}

		return _activeCalendarProfile;
	}

	/**
	 * @return Returns the index for the {@link #_activeCalendarProfile}, the index starts with 0.
	 */
	static int getActiveCalendarProfileIndex() {

		final CalendarProfile activeProfile = getActiveCalendarProfile();

		for (int profileIndex = 0; profileIndex < _allCalendarProfiles.size(); profileIndex++) {

			final CalendarProfile profile = _allCalendarProfiles.get(profileIndex);

			if (profile.equals(activeProfile)) {
				return profileIndex;
			}
		}

		// this case should not happen but ensure that a correct profile is set

//		setActiveCalendarProfile(_allCalendarProfiles.get(0));

		return 0;
	}

	static ProfileDefaultId_ComboData[] getAllAppDefault_ComboData() {
		return _allAppDefault_ComboData;
	}

	static ArrayList<CalendarProfile> getAllCalendarProfiles() {

		// ensure profiles are loaded
		getActiveCalendarProfile();

		return _allCalendarProfiles;
	}

	static ColumnLayout_ComboData[] getAllColumnLayout_ComboData() {
		return _allColumnLayout_ComboData;
	}

	static DateColumn_ComboData[] getAllDateColumnData() {
		return _allDateColumn_ComboData;
	}

	static DayHeaderDateFormat_ComboData[] getAllDayHeaderDateFormat_ComboData() {
		return _allDateHeaderDateFormat_ComboData;
	}

	static CalendarColor_ComboData[] getAllGraphColor_ComboData() {
		return _allGraphColor_ComboData;
	}

	static TourBackground_ComboData[] getAllTourBackground_ComboData() {
		return _allTourBackground_ComboData;
	}

	static TourBorder_ComboData[] getAllTourBorderData() {
		return _allTourBorder_ComboData;
	}

	static DayContentColor_ComboData[] getAllTourContentColor_ComboData() {
		return _allTourContentColor_ComboData;
	}

	private static IValueFormatter getFormatter_Number(final String formatName) {

		if (formatName.equals(ValueFormat.NUMBER_1_0.name())) {

			return _valueFormatter_Number_1_0;

		} else if (formatName.equals(ValueFormat.NUMBER_1_1.name())) {

			return _valueFormatter_Number_1_1;

		} else if (formatName.equals(ValueFormat.NUMBER_1_2.name())) {

			return _valueFormatter_Number_1_2;

		} else if (formatName.equals(ValueFormat.NUMBER_1_3.name())) {

			return _valueFormatter_Number_1_3;

		} else {

			// default

			return _valueFormatter_Number_1_0;
		}
	}

	private static IValueFormatter getFormatter_Time(final String formatName) {

		if (formatName.equals(ValueFormat.TIME_HH.name())) {

			return _valueFormatter_Time_HH;

		} else if (formatName.equals(ValueFormat.TIME_HH_MM.name())) {

			return _valueFormatter_Time_HHMM;

		} else if (formatName.equals(ValueFormat.TIME_HH_MM_SS.name())) {

			return _valueFormatter_Time_HHMMSS;

		} else {

			// default

			return _valueFormatter_Time_HHMMSS;
		}
	}

	private static CalendarProfile getProfile_Calendar() {

		CalendarProfile activeProfile = null;

		if (_fromXml_ActiveCalendarProfileId != null) {

			// ensure profile id belongs to a profile which is available

			for (final CalendarProfile profile : _allCalendarProfiles) {

				if (profile.id.equals(_fromXml_ActiveCalendarProfileId)) {

					activeProfile = profile;
					break;
				}
			}
		}

		if (activeProfile == null) {

			// a profile is not yet created

			StatusUtil.log("Created default profile for calendar properties");//$NON-NLS-1$

			createProfile_0_AllDefaultDefaultProfiles(_allCalendarProfiles);

			activeProfile = _allCalendarProfiles.get(0);
		}

		return activeProfile;
	}

	static String getProfileConfigName(final CalendarProfile profile, final String customProfileName) {

		final String profileName = customProfileName == null
				? profile.profileName
				: customProfileName;

		if (profile.isDefaultDefault) {

			return Messages.Slideout_CalendarOptions_Label_AppPrefix + DEFAULT_PREFIX + profileName;

		} else if (profile.isUserParentDefault) {

			return Messages.Slideout_CalendarOptions_Label_UserPrefix

					+ DEFAULT_PREFIX + profile.userParentDefaultId;
		}

		return profileName;
	}

	static String getProfileName(final CalendarProfile profile, final String customProfileName) {

		final String profileName = customProfileName == null
				? profile.profileName
				: customProfileName;

		if (profile.isDefaultDefault) {

			return Messages.Slideout_CalendarOptions_Label_AppPrefix + DEFAULT_PREFIX + profileName;

		} else if (profile.isUserParentDefault) {

			return Messages.Slideout_CalendarOptions_Label_UserPrefix

					+ DEFAULT_PREFIX + profile.userParentDefaultId
					+ DEFAULT_PREFIX + profileName

			;
		}

		return profileName;
	}

	private static File getProfileXmlFile() {

		final File layerFile = _stateLocation.append(PROFILE_FILE_NAME).toFile();

		return layerFile;
	}

	private static void parse_200_Calendars(final XMLMemento xmlRoot,
											final ArrayList<CalendarProfile> allCalendarProfiles) {

		if (xmlRoot == null) {
			return;
		}

		final XMLMemento xmlCalendars = (XMLMemento) xmlRoot.getChild(TAG_CALENDAR_PROFILE);

		if (xmlCalendars == null) {
			return;
		}

		_fromXml_ActiveCalendarProfileId = Util.getXmlString(xmlCalendars, ATTR_ACTIVE_PROFILE_ID, null);

		for (final IMemento mementoProfile : xmlCalendars.getChildren()) {

			final XMLMemento xmlProfile = (XMLMemento) mementoProfile;

			try {

				final String xmlProfileType = xmlProfile.getType();

				if (xmlProfileType.equals(TAG_CALENDAR)) {

					// <Calendar>

					final CalendarProfile profile = restoreProfile(xmlProfile);

					if (profile.defaultId == DefaultId.XML_DEFAULT) {

						/*
						 * The default id is unknown, this occured when switching vom 17.12.0 ->
						 * 17.12.1 but it can occure when data are corrup, setup as default
						 */
						profile.defaultId = DefaultId.DEFAULT;

						// this cannot be a default default
						profile.isDefaultDefault = false;
						profile.isUserParentDefault = false;
						profile.userParentDefaultId = UI.EMPTY_STRING;
					}

					// set profile default name
					if (profile.isDefaultDefault) {

						// overwrite default default profile name, it is readonly

						for (final CalendarProfile defaultProfile : _allDefaultDefaultProfiles) {

							if (profile.defaultId == defaultProfile.defaultId) {

								profile.profileName = defaultProfile.profileName;
								break;
							}
						}
					}

					allCalendarProfiles.add(profile);
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlProfile), e);
			}
		}
	}

	/**
	 * Read or create profile
	 * 
	 * @return
	 */
	private static void readProfileFromXml() {

		InputStreamReader reader = null;

		try {

			XMLMemento xmlRoot = null;

			// try to get layer structure from saved xml file
			final File layerFile = getProfileXmlFile();
			final String absoluteLayerPath = layerFile.getAbsolutePath();

			final File inputFile = new File(absoluteLayerPath);
			if (inputFile.exists()) {

				try {

					reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
					xmlRoot = XMLMemento.createReadRoot(reader);

				} catch (final Exception e) {
					// ignore
				}
			}

			// parse xml
			parse_200_Calendars(xmlRoot, _allCalendarProfiles);

			// ensure all app default profiles are available
			final ArrayList<CalendarProfile> missingDefaultDefaultProfiles = new ArrayList<>();
			for (final CalendarProfile defaultDefaultProfile : _allDefaultDefaultProfiles) {

				boolean isDefaultDefaultAvailable = false;

				for (final CalendarProfile restoredCalendarProfile : _allCalendarProfiles) {

					if (restoredCalendarProfile.isDefaultDefault == true
							&& restoredCalendarProfile.defaultId.equals(defaultDefaultProfile.defaultId)) {

						isDefaultDefaultAvailable = true;
						break;
					}
				}

				if (isDefaultDefaultAvailable == false) {
					missingDefaultDefaultProfiles.add(defaultDefaultProfile);
				}
			}
			_allCalendarProfiles.addAll(missingDefaultDefaultProfiles);

			// ensure profiles are created
			if (_allCalendarProfiles.size() == 0) {
				createProfile_0_AllDefaultDefaultProfiles(_allCalendarProfiles);
			}

			setActiveCalendarProfile(getProfile_Calendar(), false);

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {
			Util.close(reader);
		}
	}

	static void removeProfileListener(final ICalendarProfileListener profileListener) {
		_profileListener.remove(profileListener);
	}

	static void resetActiveCalendarProfile() {

		// get old values
		final String profileNameOld = _activeCalendarProfile.profileName;
		final DefaultId defaultIdOld = _activeCalendarProfile.defaultId;
		final boolean isDefaultDefaultOld = _activeCalendarProfile.isDefaultDefault;
		final boolean isUserParentDefaultOld = _activeCalendarProfile.isUserParentDefault;
		final String userParentDefaultIdOld = _activeCalendarProfile.userParentDefaultId;
		final String userDefaultIdOld = _activeCalendarProfile.userDefaultId;

		final int activeCalendarProfileIndex = getActiveCalendarProfileIndex();

		// remove old profile
		_allCalendarProfiles.remove(_activeCalendarProfile);

		// create new profile
		final CalendarProfile newProfile = createProfileFromId(_activeCalendarProfile);

		// default default is reset -> keep as default default
		newProfile.defaultId = defaultIdOld;
		newProfile.isDefaultDefault = isDefaultDefaultOld;
		newProfile.isUserParentDefault = isUserParentDefaultOld;
		newProfile.userParentDefaultId = userParentDefaultIdOld;
		newProfile.userDefaultId = userDefaultIdOld;

		// preserve old name
		if (isDefaultDefaultOld == false) {
			newProfile.profileName = profileNameOld;
		}

		// update model
		_allCalendarProfiles.add(activeCalendarProfileIndex, newProfile);

		setActiveCalendarProfile(newProfile, true);
	}

	private static CalendarProfile restoreProfile(final XMLMemento xmlProfile) {

		// !!! getFontData() MUST be created for EVERY font otherwise they use all the SAME font !!!
		final Font defaultFont = JFaceResources.getFontRegistry().defaultFont();

		final CalendarProfile profile = createProfile_10_Default();

// SET_FORMATTING_OFF
		
		// profile
		profile.id							= Util.getXmlString(xmlProfile,						ATTR_ID,								Long.toString(System.nanoTime()));
		profile.profileName					= Util.getXmlString(xmlProfile,						ATTR_PROFILE_NAME,						UI.EMPTY_STRING);
		//
		profile.isDefaultDefault			= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_DEFAULT_DEFAULT_ID,				false);
		profile.isUserParentDefault			= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_USER_PARENT_DEFAULT_ID,			false);
		profile.userDefaultId				= Util.getXmlString(xmlProfile,						ATTR_PROFILE_USER_DEFAULT_ID,			UI.EMPTY_STRING);
		profile.userParentDefaultId			= Util.getXmlString(xmlProfile,						ATTR_PROFILE_USER_PARENT_DEFAULT_ID,	UI.EMPTY_STRING);
		profile.defaultId					= (DefaultId) Util.getXmlEnum(xmlProfile,			ATTR_PROFILE_DEFAULT_DEFAULT_ID,		DefaultId.XML_DEFAULT);
		
		// layout
		profile.isToggleMonthColor			= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_TOGGLE_MONTH_COLOR,		true);
		profile.isWeekRowHeight				= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_WEEK_ROW_HEIGHT,		DEFAULT_IS_WEEK_ROW_HEIGHT);
		profile.useDraggedScrolling			= Util.getXmlBoolean(xmlProfile, 					ATTR_USE_DRAGGED_SCROLLING,		true);
		profile.weekHeight					= Util.getXmlInteger(xmlProfile, 					ATTR_WEEK_HEIGHT,				DEFAULT_WEEK_HEIGHT);
		profile.weekRows					= Util.getXmlInteger(xmlProfile, 					ATTR_WEEK_ROWS,					DEFAULT_WEEK_ROWS);
		profile.alternateMonthRGB			= Util.getXmlRgb(xmlProfile, 						TAG_ALTERNATE_MONTH_RGB,		DEFAULT_ALTERNATE_MONTH_RGB);
		profile.alternateMonth2RGB			= Util.getXmlRgb(xmlProfile, 						TAG_ALTERNATE_MONTH2_RGB,		DEFAULT_ALTERNATE_MONTH2_RGB);
		profile.calendarBackgroundRGB		= Util.getXmlRgb(xmlProfile, 						TAG_CALENDAR_BACKGROUND_RGB,	DEFAULT_CALENDAR_BACKGROUND_RGB);
		profile.calendarForegroundRGB		= Util.getXmlRgb(xmlProfile, 						TAG_CALENDAR_FOREGROUND_RGB,	DEFAULT_CALENDAR_FOREBACKGROUND_RGB);
		profile.dayHoveredRGB				= Util.getXmlRgb(xmlProfile,						TAG_DAY_HOVERED_RGB, 			DEFAULT_DAY_HOVERED_RGB);
		profile.daySelectedRGB				= Util.getXmlRgb(xmlProfile,						TAG_DAY_SELECTED_RGB,			DEFAULT_DAY_SELECTED_RGB);
		profile.dayTodayRGB					= Util.getXmlRgb(xmlProfile,						TAG_DAY_TODAY_RGB,				DEFAULT_DAY_TODAY_RGB);
		
		// 1. Date column
		profile.isShowDateColumn			= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_SHOW_DATE_COLUMN,		true);
		profile.dateColumnFont 				= Util.getXmlFont(xmlProfile, 						ATTR_DATE_COLUMN_FONT, 			defaultFont.getFontData()[0]);
		profile.dateColumnWidth				= Util.getXmlInteger(xmlProfile, 					ATTR_DATE_COLUMN_WIDTH,			DEFAULT_DATE_COLUMN_WIDTH);
		profile.dateColumnContent			= (DateColumnContent) Util.getXmlEnum(xmlProfile,	ATTR_DATE_COLUMN_CONTENT,		DateColumnContent.WEEK_NUMBER);
		
		// 2. Year columns
		profile.isShowYearColumns			= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_SHOW_YEAR_COLUMNS,		true);
		profile.isYearColumnDayWidth		= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_YEAR_COLUMN_DAY_WIDTH,		DEFAULT_IS_YEAR_COLUMN_DAY_WIDTH);
		profile.yearColumns					= Util.getXmlInteger(xmlProfile,					ATTR_YEAR_COLUMNS,				DEFAULT_YEAR_COLUMNS);
		profile.yearColumnsSpacing			= Util.getXmlInteger(xmlProfile, 					ATTR_YEAR_COLUMNS_SPACING,		DEFAULT_YEAR_COLUMNS_SPACING);
		profile.yearColumnsStart			= (ColumnStart) Util.getXmlEnum(xmlProfile,			ATTR_YEAR_COLUMNS_START,		DEFAULT_YEAR_COLUMNS_LAYOUT);
		profile.yearColumnDayWidth			= Util.getXmlInteger(xmlProfile, 					ATTR_YEAR_COLUMN_DAY_WIDTH,		DEFAULT_YEAR_COLUMN_DAY_WIDTH);
		profile.yearHeaderFont				= Util.getXmlFont(xmlProfile, 						ATTR_YEAR_HEADER_FONT,			defaultFont.getFontData()[0]);
		
		// 3. Week summary column
		profile.isShowSummaryColumn			= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_SHOW_SUMMARY_COLUMN,	true);
		profile.isShowWeekValueUnit			= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_SHOW_WEEK_VALUE_UNIT,	true);
		profile.weekColumnWidth				= Util.getXmlInteger(xmlProfile, 					ATTR_WEEK_COLUMN_WIDTH,			DEFAULT_WEEK_COLUMN_WIDTH);
		profile.weekMarginTop				= Util.getXmlInteger(xmlProfile, 					ATTR_WEEK_MARGIN_TOP,			DEFAULT_WEEK_MARGIN_TOP,		DEFAULT_MARGIN_MIN, DEFAULT_MARGIN_MAX);
		profile.weekMarginLeft				= Util.getXmlInteger(xmlProfile, 					ATTR_WEEK_MARGIN_LEFT,			DEFAULT_WEEK_MARGIN_LEFT,		DEFAULT_MARGIN_MIN, DEFAULT_MARGIN_MAX);
		profile.weekMarginBottom			= Util.getXmlInteger(xmlProfile, 					ATTR_WEEK_MARGIN_BOTTOM,		DEFAULT_WEEK_MARGIN_BOTTOM,		DEFAULT_MARGIN_MIN, DEFAULT_MARGIN_MAX);
		profile.weekMarginRight				= Util.getXmlInteger(xmlProfile, 					ATTR_WEEK_MARGIN_RIGHT,			DEFAULT_WEEK_MARGIN_RIGHT,		DEFAULT_MARGIN_MIN, DEFAULT_MARGIN_MAX);
		profile.weekValueColor		 		= (CalendarColor) Util.getXmlEnum(xmlProfile,		ATTR_WEEK_VALUE_COLOR,			DEFAULT_WEEK_VALUE_COLOR);
		profile.weekValueFont				= Util.getXmlFont(xmlProfile, 						ATTR_WEEK_VALUE_FONT,			defaultFont.getFontData()[0]);
		profile.weekValueRGB				= Util.getXmlRgb(xmlProfile,						TAG_WEEK_VALUE_RGB, 			DEFAULT_WEEK_VALUE_RGB);

		// day date
		profile.isHideDayDateWhenNoTour		= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_HIDE_DAY_DATE_WHEN_NO_TOUR,		true);
		profile.isShowDayDate				= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_SHOW_DAY_DATE,					true);
		profile.isShowDayDateWeekendColor	= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_SHOW_DAY_DATE_WEEKEND_COLOR,	DEFAULT_IS_SHOW_DAY_DATE_WEEKEND_COLOR);
		profile.dayDateMarginTop			= Util.getXmlInteger(xmlProfile, 					ATTR_DAY_DATE_MARGIN_TOP,				DEFAULT_DAY_DATE_MARGIN_TOP,		DEFAULT_MARGIN_MIN, DEFAULT_MARGIN_MAX);
		profile.dayDateMarginLeft			= Util.getXmlInteger(xmlProfile, 					ATTR_DAY_DATE_MARGIN_LEFT,				DEFAULT_DAY_DATE_MARGIN_LEFT,		DEFAULT_MARGIN_MIN, DEFAULT_MARGIN_MAX);
		profile.dayDateFont 				= Util.getXmlFont(xmlProfile, 						ATTR_DAY_DATE_FONT, 					defaultFont.getFontData()[0]);
		profile.dayDateFormat				= (DayDateFormat) Util.getXmlEnum(xmlProfile,		ATTR_DAY_DATE_FORMAT,					DEFAULT_DAY_DATE_FORMAT);
		
		// day layout
		profile.isDayContentVertical		= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_DAY_CONTENT_VERTICAL,	true);
		
		                                    
		// tour fill
		profile.tourBackgroundWidth			= Util.getXmlInteger(xmlProfile, 					ATTR_TOUR_BACKGROUND_WIDTH, 	DEFAULT_TOUR_BACKGROUND_WIDTH,	TOUR_BACKGROUND_WIDTH_MIN,	TOUR_BACKGROUND_WIDTH_MAX);
		profile.tourBorderWidth				= Util.getXmlInteger(xmlProfile, 					ATTR_TOUR_BORDER_WIDTH,		 	DEFAULT_TOUR_BORDER_WIDTH,		TOUR_BORDER_WIDTH_MIN,		TOUR_BORDER_WIDTH_MAX);
		profile.tourBackground 				= (TourBackground) Util.getXmlEnum(xmlProfile,		ATTR_TOUR_BACKGROUND,			DEFAULT_TOUR_BACKGROUND);
		profile.tourBorder 					= (TourBorder) Util.getXmlEnum(xmlProfile,			ATTR_TOUR_BORDER,				DEFAULT_TOUR_BORDER);
		profile.tourBackground1Color 		= (CalendarColor) Util.getXmlEnum(xmlProfile,		ATTR_TOUR_BACKGROUND_COLOR1,	DEFAULT_TOUR_BACKGROUND_COLOR1);
		profile.tourBackground2Color 		= (CalendarColor) Util.getXmlEnum(xmlProfile,		ATTR_TOUR_BACKGROUND_COLOR2,	DEFAULT_TOUR_BACKGROUND_COLOR2);
		profile.tourBorderColor 			= (CalendarColor) Util.getXmlEnum(xmlProfile,		ATTR_TOUR_BORDER_COLOR,			DEFAULT_TOUR_BORDER_COLOR);
		profile.tourDraggedColor			= (CalendarColor) Util.getXmlEnum(xmlProfile,		ATTR_TOUR_DRAGGED_COLOR, 		DEFAULT_TOUR_DRAGGED_COLOR);
		profile.tourHoveredColor			= (CalendarColor) Util.getXmlEnum(xmlProfile,		ATTR_TOUR_HOVERED_COLOR, 		DEFAULT_TOUR_HOVERED_COLOR);
		profile.tourSelectedColor			= (CalendarColor) Util.getXmlEnum(xmlProfile,		ATTR_TOUR_SELECTED_COLOR, 		DEFAULT_TOUR_SELECTED_COLOR);
		profile.tourBackground1RGB			= Util.getXmlRgb(xmlProfile,						TAG_TOUR_BACKGROUND_1_RGB, 		DEFAULT_TOUR_BACKGROUND_1_RGB);
		profile.tourBackground2RGB			= Util.getXmlRgb(xmlProfile,						TAG_TOUR_BACKGROUND_2_RGB,		DEFAULT_TOUR_BACKGROUND_2_RGB);
		profile.tourBorderRGB				= Util.getXmlRgb(xmlProfile,						TAG_TOUR_BORDER_RGB, 			DEFAULT_TOUR_BORDER_RGB);
		profile.tourDraggedRGB				= Util.getXmlRgb(xmlProfile,						TAG_TOUR_DRAGGED_RGB, 			DEFAULT_TOUR_DRAGGED_RGB);
		profile.tourHoveredRGB				= Util.getXmlRgb(xmlProfile,						TAG_TOUR_HOVERED_RGB, 			DEFAULT_TOUR_HOVERED_RGB);
		profile.tourSelectedRGB				= Util.getXmlRgb(xmlProfile,						TAG_TOUR_SELECTED_RGB, 			DEFAULT_TOUR_SELECTED_RGB);
		
		// tour content
		profile.isShowTourContent			= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_SHOW_TOUR_CONTENT,		true);
		profile.isShowTourValueUnit			= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_SHOW_TOUR_VALUE_UNIT,	true);
		profile.isTruncateTourText			= Util.getXmlBoolean(xmlProfile, 					ATTR_IS_TRUNCATE_TOUR_TEXT,		DEFAULT_IS_TRUNCATE_TOUR_TEXT);
		profile.tourMarginTop				= Util.getXmlInteger(xmlProfile, 					ATTR_TOUR_MARGIN_TOP,			DEFAULT_TOUR_MARGIN_TOP,		DEFAULT_MARGIN_MIN, DEFAULT_MARGIN_MAX);
		profile.tourMarginLeft				= Util.getXmlInteger(xmlProfile, 					ATTR_TOUR_MARGIN_LEFT,			DEFAULT_TOUR_MARGIN_LEFT,		DEFAULT_MARGIN_MIN, DEFAULT_MARGIN_MAX);
		profile.tourMarginBottom			= Util.getXmlInteger(xmlProfile, 					ATTR_TOUR_MARGIN_BOTTOM,		DEFAULT_TOUR_MARGIN_BOTTOM,		DEFAULT_MARGIN_MIN, DEFAULT_MARGIN_MAX);
		profile.tourMarginRight				= Util.getXmlInteger(xmlProfile, 					ATTR_TOUR_MARGIN_RIGHT,			DEFAULT_TOUR_MARGIN_RIGHT,		DEFAULT_MARGIN_MIN, DEFAULT_MARGIN_MAX);
		profile.tourTruncatedLines			= Util.getXmlInteger(xmlProfile, 					ATTR_TOUR_TRUNCATED_LINES,	 	DEFAULT_TOUR_TRUNCATED_LINES,	TOUR_TRUNCATED_LINES_MIN,	TOUR_TRUNCATED_LINES_MAX);
		profile.tourValueColumns			= Util.getXmlInteger(xmlProfile, 					ATTR_TOUR_VALUE_COLUMNS,	 	DEFAULT_TOUR_VALUE_COLUMNS,		TOUR_VALUE_COLUMNS_MIN,		TOUR_VALUE_COLUMNS_MAX);
		profile.tourContentFont 			= Util.getXmlFont(xmlProfile, 						ATTR_TOUR_CONTENT_FONT, 		defaultFont.getFontData()[0]);
		profile.tourTitleFont 				= Util.getXmlFont(xmlProfile, 						ATTR_TOUR_TITLE_FONT, 			defaultFont.getFontData()[0]);
		profile.tourValueFont				= Util.getXmlFont(xmlProfile, 						ATTR_TOUR_VALUE_FONT, 			defaultFont.getFontData()[0]);
		profile.tourContentColor			= (CalendarColor) Util.getXmlEnum(xmlProfile,		ATTR_TOUR_CONTENT_COLOR,		DEFAULT_TOUR_COLOR);
		profile.tourTitleColor				= (CalendarColor) Util.getXmlEnum(xmlProfile,		ATTR_TOUR_TITLE_COLOR,			DEFAULT_TOUR_COLOR);
		profile.tourValueColor				= (CalendarColor) Util.getXmlEnum(xmlProfile,		ATTR_TOUR_VALUE_COLOR,			DEFAULT_TOUR_COLOR);
		profile.tourContentRGB				= Util.getXmlRgb(xmlProfile,						TAG_TOUR_CONTENT_RGB, 			DEFAULT_TOUR_CONTENT_RGB);
		profile.tourTitleRGB				= Util.getXmlRgb(xmlProfile,						TAG_TOUR_TITLE_RGB, 			DEFAULT_TOUR_TITLE_RGB);
		profile.tourValueRGB				= Util.getXmlRgb(xmlProfile,						TAG_TOUR_VALUE_RGB, 			DEFAULT_TOUR_VALUE_RGB);

// SET_FORMATTING_ON

		// tour formatter
		final FormatterData[] tourFormatterData = restoreProfile_FormatterData(
				xmlProfile,
				TAG_ALL_TOUR_FORMATTER,
				profile);
		if (tourFormatterData != null) {
			profile.allTourFormatterData = tourFormatterData;
		}

		// week formatter
		final FormatterData[] weekFormatterData = restoreProfile_FormatterData(
				xmlProfile,
				TAG_ALL_WEEK_FORMATTER,
				profile);
		if (weekFormatterData != null) {
			profile.allWeekFormatterData = weekFormatterData;
		}

		return profile;
	}

	private static FormatterData[] restoreProfile_FormatterData(final XMLMemento xmlProfile,
																final String tagAllFormatter,
																final CalendarProfile profile) {

		final XMLMemento xmlAllFormatter = (XMLMemento) xmlProfile.getChild(tagAllFormatter);
		if (xmlAllFormatter != null) {

			final ArrayList<FormatterData> allFormatterData = new ArrayList<>();

			for (final IMemento xmlFormatterData : xmlAllFormatter.getChildren()) {

				final boolean isEnabled = Util.getXmlBoolean(xmlFormatterData, ATTR_IS_SHOW_VALUE, true);

				final FormatterID id = (FormatterID) Util.getXmlEnum(
						xmlFormatterData,
						ATTR_FORMATTER_ID,
						FormatterID.EMPTY);

				final ValueFormat valueFormat = (ValueFormat) Util.getXmlEnum(
						xmlFormatterData,
						ATTR_FORMATTER_VALUE_FORMAT,
						ValueFormat.DUMMY_VALUE);

				allFormatterData.add(new FormatterData(isEnabled, id, valueFormat));
			}

			return allFormatterData.toArray(new FormatterData[allFormatterData.size()]);
		}

		return null;
	}

	private static void saveProfile(final CalendarProfile profile, final IMemento xmlProfile) {

// SET_FORMATTING_OFF
					
		// profile
		xmlProfile.putString(		ATTR_ID, 								profile.id);
		xmlProfile.putString(		ATTR_PROFILE_NAME, 						profile.profileName);
		//
		xmlProfile.putBoolean(		ATTR_IS_DEFAULT_DEFAULT_ID, 			profile.isDefaultDefault);
		xmlProfile.putBoolean(		ATTR_IS_USER_PARENT_DEFAULT_ID, 		profile.isUserParentDefault);
		xmlProfile.putString(		ATTR_PROFILE_USER_DEFAULT_ID,			profile.userDefaultId);
		xmlProfile.putString(		ATTR_PROFILE_USER_PARENT_DEFAULT_ID,	profile.userParentDefaultId);
		Util.setXmlEnum(xmlProfile,	ATTR_PROFILE_DEFAULT_DEFAULT_ID,		profile.defaultId);
		
		// layout
		xmlProfile.putBoolean(		ATTR_IS_TOGGLE_MONTH_COLOR, 			profile.isToggleMonthColor);
		xmlProfile.putBoolean(		ATTR_IS_WEEK_ROW_HEIGHT,	 			profile.isWeekRowHeight);
		xmlProfile.putBoolean(		ATTR_USE_DRAGGED_SCROLLING, 			profile.useDraggedScrolling);
		xmlProfile.putInteger(		ATTR_WEEK_HEIGHT, 						profile.weekHeight);
		xmlProfile.putInteger(		ATTR_WEEK_ROWS, 						profile.weekRows);
		Util.setXmlRgb(xmlProfile,	TAG_ALTERNATE_MONTH_RGB, 				profile.alternateMonthRGB);
		Util.setXmlRgb(xmlProfile,	TAG_ALTERNATE_MONTH2_RGB, 				profile.alternateMonth2RGB);
		Util.setXmlRgb(xmlProfile,	TAG_CALENDAR_BACKGROUND_RGB, 			profile.calendarBackgroundRGB);
		Util.setXmlRgb(xmlProfile,	TAG_CALENDAR_FOREGROUND_RGB, 			profile.calendarForegroundRGB);
		Util.setXmlRgb(xmlProfile,	TAG_DAY_HOVERED_RGB, 					profile.dayHoveredRGB);
		Util.setXmlRgb(xmlProfile,	TAG_DAY_SELECTED_RGB, 					profile.daySelectedRGB);
		Util.setXmlRgb(xmlProfile,	TAG_DAY_TODAY_RGB,	 					profile.dayTodayRGB);
		
		// 1. Date column
		xmlProfile.putBoolean(		ATTR_IS_SHOW_DATE_COLUMN, 				profile.isShowDateColumn);
		xmlProfile.putInteger(		ATTR_DATE_COLUMN_WIDTH, 				profile.dateColumnWidth);
		Util.setXmlEnum(xmlProfile,	ATTR_DATE_COLUMN_CONTENT, 				profile.dateColumnContent);
		Util.setXmlFont(xmlProfile,	ATTR_DATE_COLUMN_FONT, 					profile.dateColumnFont);

		// 2. Year columns
		xmlProfile.putBoolean(		ATTR_IS_SHOW_YEAR_COLUMNS, 				profile.isShowYearColumns);
		xmlProfile.putBoolean(		ATTR_IS_YEAR_COLUMN_DAY_WIDTH, 			profile.isYearColumnDayWidth);
		xmlProfile.putInteger(		ATTR_YEAR_COLUMNS, 						profile.yearColumns);
		xmlProfile.putInteger(		ATTR_YEAR_COLUMNS_SPACING, 				profile.yearColumnsSpacing);
		xmlProfile.putInteger(		ATTR_YEAR_COLUMN_DAY_WIDTH, 			profile.yearColumnDayWidth);
		Util.setXmlEnum(xmlProfile,	ATTR_YEAR_COLUMNS_START, 				profile.yearColumnsStart);
		Util.setXmlFont(xmlProfile,	ATTR_YEAR_HEADER_FONT, 					profile.yearHeaderFont);
		
		// 3. Week summary column
		xmlProfile.putBoolean(		ATTR_IS_SHOW_SUMMARY_COLUMN, 			profile.isShowSummaryColumn);
		xmlProfile.putBoolean(		ATTR_IS_SHOW_WEEK_VALUE_UNIT, 			profile.isShowWeekValueUnit);
		xmlProfile.putInteger(		ATTR_WEEK_COLUMN_WIDTH, 				profile.weekColumnWidth);
		xmlProfile.putInteger(		ATTR_WEEK_MARGIN_TOP, 					profile.weekMarginTop);
		xmlProfile.putInteger(		ATTR_WEEK_MARGIN_LEFT, 					profile.weekMarginLeft);
		xmlProfile.putInteger(		ATTR_WEEK_MARGIN_BOTTOM, 				profile.weekMarginBottom);
		xmlProfile.putInteger(		ATTR_WEEK_MARGIN_RIGHT, 				profile.weekMarginRight);
		Util.setXmlEnum(xmlProfile,	ATTR_WEEK_VALUE_COLOR, 					profile.weekValueColor);
		Util.setXmlFont(xmlProfile,	ATTR_WEEK_VALUE_FONT, 					profile.weekValueFont);
		Util.setXmlRgb(xmlProfile,	TAG_WEEK_VALUE_RGB, 					profile.weekValueRGB);
		
		// day date
		xmlProfile.putBoolean(		ATTR_IS_HIDE_DAY_DATE_WHEN_NO_TOUR, 	profile.isHideDayDateWhenNoTour);
		xmlProfile.putBoolean(		ATTR_IS_SHOW_DAY_DATE, 					profile.isShowDayDate);
		xmlProfile.putBoolean(		ATTR_IS_SHOW_DAY_DATE_WEEKEND_COLOR, 	profile.isShowDayDateWeekendColor);
		xmlProfile.putInteger(		ATTR_DAY_DATE_MARGIN_TOP, 				profile.dayDateMarginTop);
		xmlProfile.putInteger(		ATTR_DAY_DATE_MARGIN_LEFT, 				profile.dayDateMarginLeft);
		Util.setXmlEnum(xmlProfile,	ATTR_DAY_DATE_FORMAT, 					profile.dayDateFormat);
		Util.setXmlFont(xmlProfile,	ATTR_DAY_DATE_FONT, 					profile.dayDateFont);
		
		// day layout
		xmlProfile.putBoolean(		ATTR_IS_DAY_CONTENT_VERTICAL, 			profile.isDayContentVertical);

		// tour background
		xmlProfile.putInteger(		ATTR_TOUR_BACKGROUND_WIDTH, 			profile.tourBackgroundWidth);
		xmlProfile.putInteger(		ATTR_TOUR_BORDER_WIDTH, 				profile.tourBorderWidth);
		Util.setXmlEnum(xmlProfile,	ATTR_TOUR_BACKGROUND, 					profile.tourBackground);
		Util.setXmlEnum(xmlProfile,	ATTR_TOUR_BACKGROUND_COLOR1, 			profile.tourBackground1Color);
		Util.setXmlEnum(xmlProfile,	ATTR_TOUR_BACKGROUND_COLOR2, 			profile.tourBackground2Color);
		Util.setXmlEnum(xmlProfile,	ATTR_TOUR_BORDER, 						profile.tourBorder);
		Util.setXmlEnum(xmlProfile,	ATTR_TOUR_BORDER_COLOR, 				profile.tourBorderColor);
		Util.setXmlEnum(xmlProfile,	ATTR_TOUR_DRAGGED_COLOR, 				profile.tourDraggedColor);
		Util.setXmlEnum(xmlProfile,	ATTR_TOUR_HOVERED_COLOR, 				profile.tourHoveredColor);
		Util.setXmlEnum(xmlProfile,	ATTR_TOUR_SELECTED_COLOR, 				profile.tourSelectedColor);
		Util.setXmlRgb(xmlProfile,	TAG_TOUR_BACKGROUND_1_RGB, 				profile.tourBackground1RGB);
		Util.setXmlRgb(xmlProfile,	TAG_TOUR_BACKGROUND_2_RGB, 				profile.tourBackground2RGB);
		Util.setXmlRgb(xmlProfile,	TAG_TOUR_BORDER_RGB, 					profile.tourBorderRGB);
		Util.setXmlRgb(xmlProfile,	TAG_TOUR_DRAGGED_RGB, 					profile.tourDraggedRGB);
		Util.setXmlRgb(xmlProfile,	TAG_TOUR_HOVERED_RGB, 					profile.tourHoveredRGB);
		Util.setXmlRgb(xmlProfile,	TAG_TOUR_SELECTED_RGB, 					profile.tourSelectedRGB);

		// tour content
		xmlProfile.putBoolean(		ATTR_IS_SHOW_TOUR_CONTENT,				profile.isShowTourContent);
		xmlProfile.putBoolean(		ATTR_IS_SHOW_TOUR_VALUE_UNIT,			profile.isShowTourValueUnit);
		xmlProfile.putBoolean(		ATTR_IS_TRUNCATE_TOUR_TEXT,				profile.isTruncateTourText);
		xmlProfile.putInteger(		ATTR_TOUR_MARGIN_TOP, 					profile.tourMarginTop);
		xmlProfile.putInteger(		ATTR_TOUR_MARGIN_LEFT, 					profile.tourMarginLeft);
		xmlProfile.putInteger(		ATTR_TOUR_MARGIN_BOTTOM, 				profile.tourMarginBottom);
		xmlProfile.putInteger(		ATTR_TOUR_MARGIN_RIGHT, 				profile.tourMarginRight);
		xmlProfile.putInteger(		ATTR_TOUR_TRUNCATED_LINES, 				profile.tourTruncatedLines);
		xmlProfile.putInteger(		ATTR_TOUR_VALUE_COLUMNS, 				profile.tourValueColumns);
		Util.setXmlFont(xmlProfile,	ATTR_TOUR_CONTENT_FONT, 				profile.tourContentFont);
		Util.setXmlFont(xmlProfile,	ATTR_TOUR_TITLE_FONT, 					profile.tourTitleFont);
		Util.setXmlFont(xmlProfile,	ATTR_TOUR_VALUE_FONT, 					profile.tourValueFont);
		Util.setXmlEnum(xmlProfile,	ATTR_TOUR_CONTENT_COLOR, 				profile.tourContentColor);
		Util.setXmlEnum(xmlProfile,	ATTR_TOUR_TITLE_COLOR, 					profile.tourTitleColor);
		Util.setXmlEnum(xmlProfile,	ATTR_TOUR_VALUE_COLOR, 					profile.tourValueColor);
		Util.setXmlRgb(xmlProfile,	TAG_TOUR_CONTENT_RGB, 					profile.tourContentRGB);
		Util.setXmlRgb(xmlProfile,	TAG_TOUR_TITLE_RGB, 					profile.tourTitleRGB);
		Util.setXmlRgb(xmlProfile,	TAG_TOUR_VALUE_RGB, 					profile.tourValueRGB);
		
// SET_FORMATTING_ON

		// formatter
		saveProfile_FormatterData(xmlProfile.createChild(TAG_ALL_TOUR_FORMATTER), profile.allTourFormatterData);
		saveProfile_FormatterData(xmlProfile.createChild(TAG_ALL_WEEK_FORMATTER), profile.allWeekFormatterData);
	}

	private static void saveProfile_FormatterData(	final IMemento xmlAllTourFormatter,
													final FormatterData[] allFormatterData) {

		for (final FormatterData formatterData : allFormatterData) {

			final IMemento xmlFormatter = xmlAllTourFormatter.createChild(TAG_FORMATTER);

			xmlFormatter.putBoolean(ATTR_IS_SHOW_VALUE, formatterData.isEnabled);
			Util.setXmlEnum(xmlFormatter, ATTR_FORMATTER_ID, formatterData.id);
			Util.setXmlEnum(xmlFormatter, ATTR_FORMATTER_VALUE_FORMAT, formatterData.valueFormat);
		}
	}

	static void saveState() {

		if (_activeCalendarProfile == null) {

			// this can happen when not yet used

			return;
		}

		final XMLMemento xmlRoot = create_Root();

		saveState_Calendars(xmlRoot);

		Util.writeXml(xmlRoot, getProfileXmlFile());
	}

	/**
	 * Calendars
	 */
	private static void saveState_Calendars(final XMLMemento xmlRoot) {

		final IMemento xmlCalendars = xmlRoot.createChild(TAG_CALENDAR_PROFILE);
		{
			xmlCalendars.putString(ATTR_ACTIVE_PROFILE_ID, _activeCalendarProfile.id);

			for (final CalendarProfile profile : _allCalendarProfiles) {
				saveState_Calendars_Profile(profile, xmlCalendars);
			}
		}
	}

	private static void saveState_Calendars_Profile(final CalendarProfile profile, final IMemento xmlCalendars) {

		// <Calendar>
		final IMemento xmlProfile = xmlCalendars.createChild(TAG_CALENDAR);
		{
			saveProfile(profile, xmlProfile);
		}
	}

	/**
	 * Set a new/modified profile
	 * 
	 * @param modifiedProfile
	 *            Modified or selected profile
	 * @param isFireModifyEvent
	 *            Fire modify event when <code>true</code>
	 */
	static void setActiveCalendarProfile(final CalendarProfile modifiedProfile, final boolean isFireModifyEvent) {

		_activeCalendarProfile = modifiedProfile;

		updateFormatterValueFormat();

		if (isFireModifyEvent) {

			final Object[] allListener = _profileListener.getListeners();

			// fire modify event
			for (final Object listener : allListener) {
				((ICalendarProfileListener) listener).profileIsModified();
			}
		}
	}

	/**
	 * Update value formatter
	 */
	static void updateFormatterValueFormat() {

		/*
		 * Tour formatter
		 */
		for (final FormatterData formatterData : _activeCalendarProfile.allTourFormatterData) {

			if (!formatterData.isEnabled) {
				continue;
			}

			final ValueFormat valueFormat = formatterData.valueFormat;

			switch (formatterData.id) {

			case ALTITUDE:
				_tourFormatter_Altitude.setValueFormat(valueFormat);
				break;

			case DISTANCE:
				_tourFormatter_Distance.setValueFormat(valueFormat);
				break;

			case ENERGY_KCAL:
				_tourFormatter_Energy_kcal.setValueFormat(valueFormat);
				break;

			case ENERGY_MJ:
				_tourFormatter_Energy_MJ.setValueFormat(valueFormat);
				break;

			case PACE:
				_tourFormatter_Pace.setValueFormat(valueFormat);
				break;

			case SPEED:
				_tourFormatter_Speed.setValueFormat(valueFormat);
				break;

			case TIME_MOVING:
				_tourFormatter_Time_Moving.setValueFormat(valueFormat);
				break;

			case TIME_PAUSED:
				_tourFormatter_Time_Paused.setValueFormat(valueFormat);
				break;

			case TIME_RECORDING:
				_tourFormatter_Time_Recording.setValueFormat(valueFormat);
				break;

			default:
				break;
			}
		}

		/*
		 * Week formatter
		 */
		for (final FormatterData formatterData : _activeCalendarProfile.allWeekFormatterData) {

			if (!formatterData.isEnabled) {
				continue;
			}

			final ValueFormat valueFormat = formatterData.valueFormat;

			switch (formatterData.id) {

			case ALTITUDE:
				_weekFormatter_Altitude.setValueFormat(valueFormat);
				break;

			case DISTANCE:
				_weekFormatter_Distance.setValueFormat(valueFormat);
				break;

			case ENERGY_KCAL:
				_weekFormatter_Energy_kcal.setValueFormat(valueFormat);
				break;

			case ENERGY_MJ:
				_weekFormatter_Energy_MJ.setValueFormat(valueFormat);
				break;

			case PACE:
				_weekFormatter_Pace.setValueFormat(valueFormat);
				break;

			case SPEED:
				_weekFormatter_Speed.setValueFormat(valueFormat);
				break;

			case TIME_MOVING:
				_weekFormatter_Time_Moving.setValueFormat(valueFormat);
				break;

			case TIME_PAUSED:
				_weekFormatter_Time_Paused.setValueFormat(valueFormat);
				break;

			case TIME_RECORDING:
				_weekFormatter_Time_Recording.setValueFormat(valueFormat);
				break;

			default:
				break;
			}
		}
	}

}
