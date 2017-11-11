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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;

public class CalendarConfig {

	/*
	 * Set default values also here to ensure that a valid value is set. A default value would not
	 * be set when an xml tag is not available.
	 */

// SET_FORMATTING_OFF
	
//	private static final FontData	DEFAULT_FONT		= JFaceResources.getFontRegistry().defaultFont().getFontData()[0];
//	private static final FontData	DEFAULT_BOLD		= JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT).getFontData()[0];
	

	// config
	String					id							= Long.toString(System.nanoTime());
	String					defaultId					= CalendarConfigManager.CONFIG_DEFAULT_ID_1;
	String					name						= CalendarConfigManager.CONFIG_DEFAULT_ID_1;
	                        
	// day date
	boolean					isHideDayDateWhenNoTour		= false;
	boolean 				isShowDayDateWeekendColor	= CalendarConfigManager.DEFAULT_IS_SHOW_DAY_DATE_WEEKEND_COLOR;
	FontData				dayDateFont					= JFaceResources.getFontRegistry().defaultFont().getFontData()[0];
	DayDateFormat			dayDateFormat				= CalendarConfigManager.DEFAULT_DAY_DATE_FORMAT;
	                        
	// day
	boolean					isShowDayDate				= true;
	boolean					isToggleMonthColor			= true;
	RGB 					alternateMonthRGB			= CalendarConfigManager.DEFAULT_ALTERNATE_MONTH_RGB;
	FontData				dayContentFont				= JFaceResources.getFontRegistry().defaultFont().getFontData()[0];
	CalendarColor			dayContentColor				= CalendarConfigManager.DEFAULT_DAY_CONTENT_COLOR;
	                                                            
	// date column
	boolean					isShowDateColumn			= true;
	DateColumnContent		dateColumnContent			= CalendarConfigManager.DEFAULT_DATE_COLUMN_CONTENT;
	FontData				dateColumnFont				= JFaceResources.getFontRegistry().defaultFont().getFontData()[0];
	int						dateColumnWidth				= CalendarConfigManager.DEFAULT_DATE_COLUMN_WIDTH;
	
	// tour
	TourBackground			tourBackground				= CalendarConfigManager.DEFAULT_TOUR_BACKGROUND;
	CalendarColor			tourBackgroundColor1		= CalendarConfigManager.DEFAULT_TOUR_BACKGROUND_COLOR1;
	CalendarColor			tourBackgroundColor2		= CalendarConfigManager.DEFAULT_TOUR_BACKGROUND_COLOR2;
	int						tourBackgroundWidth			= CalendarConfigManager.DEFAULT_TOUR_BACKGROUND_WIDTH;
	TourBorder 				tourBorder					= CalendarConfigManager.DEFAULT_TOUR_BORDER;
	CalendarColor 			tourBorderColor				= CalendarConfigManager.DEFAULT_TOUR_BORDER_COLOR;
	int		 				tourBorderWidth				= CalendarConfigManager.DEFAULT_TOUR_BORDER_WIDTH;

	// tour content
	FormatterData[]			allTourFormatterData		= CalendarConfigManager.DEFAULT_TOUR_FORMATTER_DATA;
	boolean 				isShowTourContent			= true;
	boolean					isShowTourValueUnit			= true;
	int 					numTourValueColumns			= CalendarConfigManager.DEFAULT_TOUR_VALUE_COLUMNS;
	                        
	// year columns
	boolean 				isShowYearColumns			= true;
	int						numYearColumns				= CalendarConfigManager.DEFAULT_NUM_YEAR_COLUMNS;
	int 					yearColumnsSpacing			= CalendarConfigManager.DEFAULT_YEAR_COLUMNS_SPACING;
	ColumnStart				yearColumnsStart			= CalendarConfigManager.DEFAULT_YEAR_COLUMNS_LAYOUT;
	FontData 				yearHeaderFont				= JFaceResources.getFontRegistry().defaultFont().getFontData()[0];
	                        
	// layout
	int						weekHeight					= CalendarConfigManager.DEFAULT_WEEK_HEIGHT;
	boolean					useDraggedScrolling			= false;

	// week summary column
	FormatterData[]			allWeekFormatterData		= CalendarConfigManager.DEFAULT_WEEK_FORMATTER_DATA;
	boolean					isShowSummaryColumn			= true;
	boolean 				isShowWeekValueUnit			= true;
	int						summaryColumnWidth			= CalendarConfigManager.DEFAULT_SUMMARY_COLUMN_WIDTH;
	CalendarColor			weekValueColor				= CalendarConfigManager.DEFAULT_WEEK_VALUE_COLOR;
	FontData 				weekValueFont				= JFaceResources.getFontRegistry().defaultFont().getFontData()[0];

// SET_FORMATTING_ON

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

		final CalendarConfig other = (CalendarConfig) obj;

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
		return "CalendarConfig [name=" + name + "]";
	}

}
