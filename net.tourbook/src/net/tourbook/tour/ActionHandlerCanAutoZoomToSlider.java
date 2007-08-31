package net.tourbook.tour;

import net.tourbook.ui.HandlerUtil;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerCanAutoZoomToSlider extends TCActionHandler {

	public ActionHandlerCanAutoZoomToSlider() {
		fCommandId = TourChart.COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		Boolean isItemChecked = HandlerUtil.isItemChecked(execEvent);

		if (isItemChecked != null) {
			fTourChart.onExecuteCanAutoZoomToSlider(isItemChecked);
		}

		return null;
	}

}
