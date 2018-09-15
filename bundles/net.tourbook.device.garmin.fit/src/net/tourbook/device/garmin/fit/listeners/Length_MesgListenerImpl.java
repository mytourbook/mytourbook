package net.tourbook.device.garmin.fit.listeners;

import com.garmin.fit.LengthMesg;
import com.garmin.fit.LengthMesgListener;

import net.tourbook.device.garmin.fit.FitContext;

/**
 *
 */
public class Length_MesgListenerImpl extends AbstractMesgListener implements LengthMesgListener {

	public Length_MesgListenerImpl(final FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(final LengthMesg mesg) {
		context.getContextData().onMesg_Length(mesg);
	}

}
