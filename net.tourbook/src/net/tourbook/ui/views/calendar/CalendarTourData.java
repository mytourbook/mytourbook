package net.tourbook.ui.views.calendar;

import java.util.ArrayList;


public class CalendarTourData {

	long	tourId;
	long	typeId;

	int		typeColorIndex;

	int				year;
	int				month;
	int				day;
	int				week;
	int				dayOfWeek;

	int				startTime;
	int				endTime;

	int				altitude;
	int				distance;

	int				recordingTime;
	int				drivingTime;

	String			tourTitle;
	String			tourDescription;


	ArrayList<Long>	tagIds;

}
