package net.tourbook.chart;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerZoomOut extends ActionHandler {

	public ActionHandlerZoomOut() {
		fCommandId = Chart.COMMAND_ID_ZOOM_OUT;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		fChart.onExecuteZoomOut();

		return null;
	}

}
