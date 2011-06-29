package net.tourbook.ui.views.calendar;

import java.util.ArrayList;
import java.util.HashMap;

public class CalendarTourData {

	long[]							tourIds;
	long[]							typeIds;

	int[]							typeColorIndex;

	int[]							tourYearValues;
	int[]							tourMonthValues;
	int[]							weekValues;

	int[]							tourTimeStartValues;
	int[]							tourTimeEndValues;

	int[]							tourAltitudeValues;
	int[]							tourDistanceValues;

	ArrayList<Integer>				tourRecordingTimeValues;
	ArrayList<Integer>				tourDrivingTimeValues;

	ArrayList<String>				tourTitle;
	ArrayList<String>				tourDescription;

	/**
	 * hashmap contains the tags for the tour where the key is the tour ID
	 */
	HashMap<Long, ArrayList<Long>>	tagIds;
}
