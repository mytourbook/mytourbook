package net.tourbook.chart;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerPartNext extends ActionHandler {

	public ActionHandlerPartNext() {
		fCommandId = Chart.COMMAND_ID_PART_NEXT;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		fChart.onExecutePartNext();
		
		return null;
	}

}
