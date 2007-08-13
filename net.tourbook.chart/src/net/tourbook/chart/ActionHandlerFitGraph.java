package net.tourbook.chart;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerFitGraph extends ActionHandler {

	public ActionHandlerFitGraph() {
		fCommandId = Chart.COMMAND_ID_FIT_GRAPH;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		fChart.onExecuteFitGraph();
		
		return null;
	}

}
