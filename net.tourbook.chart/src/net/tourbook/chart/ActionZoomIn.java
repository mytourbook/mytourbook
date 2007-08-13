package net.tourbook.chart;

import org.eclipse.jface.action.Action;

public class ActionZoomIn extends Action {

	private Chart	fChart;

	public ActionZoomIn(Chart chart) {

		fChart = chart;

		setText(Messages.Action_zoom_in);
		setToolTipText(Messages.Action_zoom_in_tooltip);
		setImageDescriptor(Activator.getImageDescriptor(Messages.Image_zoom_in));
		setDisabledImageDescriptor(Activator.getImageDescriptor(Messages.Image_zoom_in_disabled));
	}

	@Override
	public void run() {
		fChart.onExecuteZoomIn();
	}

}
