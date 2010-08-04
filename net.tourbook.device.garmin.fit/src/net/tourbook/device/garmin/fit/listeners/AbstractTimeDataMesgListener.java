package net.tourbook.device.garmin.fit.listeners;

import java.util.ArrayList;

import net.tourbook.data.TimeData;

public abstract class AbstractTimeDataMesgListener {

    protected final ArrayList<TimeData> timeDataList;

    public AbstractTimeDataMesgListener(ArrayList<TimeData> timeDataList) {
	this.timeDataList = timeDataList;
    }

}
