package net.tourbook.chart;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerPartPrevious extends ActionHandler {

	public ActionHandlerPartPrevious() {
		fCommandId = Chart.COMMAND_ID_PART_PREVIOUS;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		fChart.onExecutePartPrevious();
		
		return null;
	}

}
