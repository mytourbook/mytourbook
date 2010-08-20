package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitActivityContext;

import com.garmin.fit.LapMesg;
import com.garmin.fit.LapMesgListener;

public class LapMesgListenerImpl extends AbstractMesgListener implements LapMesgListener {

	public LapMesgListenerImpl(FitActivityContext context) {
		super(context);
	}

	@Override
	public void onMesg(LapMesg mesg) {}
}
