package net.tourbook.tour;

import net.tourbook.ui.HandlerUtil;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerXAxisTime extends TCActionHandler {

	public ActionHandlerXAxisTime() {
		fCommandId = TourChart.COMMAND_ID_X_AXIS_TIME;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		Boolean isItemChecked = HandlerUtil.isItemChecked(execEvent);

		if (isItemChecked == null) {
			return null;
		}

		if (fTourChart.onExecuteXAxisTime(isItemChecked) == false) {
			return null;
		}

		// toggle time and distance buttons
		fTourChart.setCommandChecked(TourChart.COMMAND_ID_X_AXIS_TIME, isItemChecked);
		fTourChart.setCommandChecked(TourChart.COMMAND_ID_X_AXIS_DISTANCE, !isItemChecked);

		return null;
	}

}
