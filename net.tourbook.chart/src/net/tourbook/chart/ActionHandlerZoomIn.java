package net.tourbook.chart;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerZoomIn extends ActionHandler {

	public ActionHandlerZoomIn() {
		fCommandId = Chart.COMMAND_ID_ZOOM_IN;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		fChart.onExecuteZoomIn();
		
		return null;
	}

}
