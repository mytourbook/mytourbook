package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TourData;

public abstract class AbstractTourDataMesgListener {

    protected final TourData tourData;

    public AbstractTourDataMesgListener(TourData tourData) {
	this.tourData = tourData;
    }

}