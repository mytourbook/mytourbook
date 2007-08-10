package net.tourbook.tour;

import java.util.ArrayList;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;

/**
 * Handler for graphs in a tour chart
 */
class ActionHandlerGraph extends TourChartActionHandler implements IElementUpdater {

	private int	fGraphId;

	public ActionHandlerGraph(int graphId, String commandId) {

		fGraphId = graphId;
		fCommandId = commandId;
	}

	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		TourChartConfiguration chartConfig = fTourChart.fTourChartConfig;
		final ArrayList<Integer> visibleGraphs = chartConfig.getVisibleGraphs();

		boolean isThisGraphVisible = visibleGraphs.contains(fGraphId);

		// check that at least one graph is visible
		if (isThisGraphVisible && visibleGraphs.size() == 1) {

			// this is a toggle button so the check status must be reset

			TourChartActionHandlerManager.getInstance().updateUICheckState(fCommandId);

			return null;
		}

		if (!isThisGraphVisible) {
			// add the graph to the visible list
			chartConfig.addVisibleGraph(fGraphId);
		} else {
			// remove the graph from the visible list
			chartConfig.removeVisibleGraph(fGraphId);
		}

		fTourChart.updateActionState();
		fTourChart.updateChart(true);

		return null;
	}

}
