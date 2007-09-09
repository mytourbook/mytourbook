package net.tourbook.chart;

import org.eclipse.jface.action.Action;

public class ActionFitGraph extends Action {

	private Chart	fChart;

	public ActionFitGraph(Chart chart) {

		fChart = chart;

		setText(Messages.Action_zoom_fit_to_graph);
		setToolTipText(Messages.Action_zoom_fit_to_graph_tooltip);

		setImageDescriptor(Activator.getImageDescriptor(Messages.Image_zoom_fit_to_graph));
		setDisabledImageDescriptor(Activator.getImageDescriptor(Messages.Image_zoom_fit_to_graph_disabled));
	}

	@Override
	public void run() {
		fChart.onExecuteFitGraph();
	}

}
