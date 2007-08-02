package net.tourbook.chart.actions;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class HandlerZoomOut extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {

		System.out.println(event.getCommand().getId());

		Chart activeChart = ChartManager.getInstance().getActiveChart();

		if (activeChart == null) {
			return null;
		}

		return null;
	}

	public boolean isEnabled() {
		
		Chart activeChart = ChartManager.getInstance().getActiveChart();

		if (activeChart == null) {
			return false;
		}

		boolean isZoomOutEnabled = true;

		return isZoomOutEnabled;
	}

}
