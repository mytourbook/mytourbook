package net.tourbook.tour;

import net.tourbook.ui.HandlerUtil;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerCanScrollChart extends TCActionHandler {

	public ActionHandlerCanScrollChart() {
		fCommandId = TourChart.COMMAND_ID_CAN_SCROLL_CHART;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		Boolean isItemChecked = HandlerUtil.isItemChecked(execEvent);

		if (isItemChecked == null) {
			return null;
		}

		fTourChart.setCanScrollZoomedChart(isItemChecked);

		// apply setting to the chart
		if (isItemChecked) {
			fTourChart.zoomInWithSlider();
		} else {
			fTourChart.zoomOut(true);
		}

		fTourChart.updateZoomOptionActionHandlers();

		return null;
	}

}
