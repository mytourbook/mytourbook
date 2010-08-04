package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TourData;

import com.garmin.fit.FileCreatorMesg;
import com.garmin.fit.FileCreatorMesgListener;

public class FileCreatorMesgListenerImpl extends AbstractTourDataMesgListener implements FileCreatorMesgListener {

    public FileCreatorMesgListenerImpl(TourData tourData) {
	super(tourData);
    }

    @Override
    public void onMesg(FileCreatorMesg mesg) {
	Integer softwareVersion = mesg.getSoftwareVersion();
	if (softwareVersion != null) {
	    // TODO
	}
    }

}