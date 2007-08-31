package net.tourbook.tour;

import net.tourbook.ui.HandlerUtil;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerShowStartTime extends TCActionHandler {

	public ActionHandlerShowStartTime() {
		fCommandId = TourChart.COMMAND_ID_SHOW_START_TIME;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		Boolean isItemChecked = HandlerUtil.isItemChecked(execEvent);

		if (isItemChecked != null) {
			fTourChart.onExecuteShowStartTime(isItemChecked);
		}

		return null;
	}
}
