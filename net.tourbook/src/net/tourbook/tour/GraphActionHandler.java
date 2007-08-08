package net.tourbook.tour;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

public class GraphActionHandler extends AbstractHandler implements IElementUpdater {

	private boolean		fisEnabled;
	private boolean		fIsChecked;

	private TourChart	fTourChart;
	private int			fMapId;

	public GraphActionHandler(TourChart tourChart, int mapId) {

		fTourChart = tourChart;
		fMapId = mapId;

		setChecked(tourChart.fTourChartConfig.getVisibleGraphs().contains(mapId));
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {

		TourChartConfiguration chartConfig = fTourChart.fTourChartConfig;

		boolean isGraphVisible = chartConfig.getVisibleGraphs().contains(fMapId);

		if (!isGraphVisible) {
			// add the graph to the list
			chartConfig.addVisibleGraph(fMapId);
		} else {
			// remove the graph from the list
			chartConfig.removeVisibleGraph(fMapId);
		}

		fTourChart.enableActions();
		fTourChart.updateChart(true);

		return null;
	}

	/**
	 * update the UI when the enablement state was changed
	 */
	public void fireHandlerChanged() {
		fireHandlerChanged(new HandlerEvent(this, true, false));
	}

	public boolean isEnabled() {
		return fisEnabled;
	}

	public void setChecked(boolean isChecked) {

		if (fIsChecked != isChecked) {
			fIsChecked = isChecked;
		}
	}

	public void setEnabled(boolean isEnabled) {

		if (fisEnabled != isEnabled) {
			fisEnabled = isEnabled;
		}
	}

	@SuppressWarnings("unchecked")
	public void updateElement(UIElement element, Map parameters) {

		// update check state in the UI
		element.setChecked(fIsChecked);
	}
}
