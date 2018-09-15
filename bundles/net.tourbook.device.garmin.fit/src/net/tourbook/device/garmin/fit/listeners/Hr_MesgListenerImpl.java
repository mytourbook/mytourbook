package net.tourbook.device.garmin.fit.listeners;

import com.garmin.fit.HrMesg;
import com.garmin.fit.HrMesgListener;

import net.tourbook.device.garmin.fit.FitContext;

/**
 *
 */
public class Hr_MesgListenerImpl extends AbstractMesgListener implements HrMesgListener {

	public Hr_MesgListenerImpl(final FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(final HrMesg mesg) {
		context.getContextData().onMesg_Hr(mesg);
	}

}
