package net.tourbook.chart;

import org.eclipse.jface.action.Action;

public class ActionZoomOut extends Action {

	private Chart	fChart;

	public ActionZoomOut(Chart chart) {

		fChart = chart;

		setText(Messages.Action_zoom_out);
		setToolTipText(Messages.Action_zoom_out_tooltip);
		setImageDescriptor(Activator.getImageDescriptor(Messages.Image_zoom_out));
		setDisabledImageDescriptor(Activator.getImageDescriptor(Messages.Image_zoom_out_disabled));
	}

	@Override
	public void run() {
		fChart.onExecuteZoomOut();
	}
}
