package net.tourbook.tour;

import net.tourbook.ui.HandlerUtil;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerXAxisDistance extends TCActionHandler {

	public ActionHandlerXAxisDistance() {
		fCommandId = TourChart.COMMAND_ID_X_AXIS_DISTANCE;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		Boolean isItemChecked = HandlerUtil.isItemChecked(execEvent);

		if (isItemChecked != null) {
			fTourChart.onExecuteXAxisDistance(isItemChecked);
		}

		return null;
	}

}
