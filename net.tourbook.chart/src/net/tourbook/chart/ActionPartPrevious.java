package net.tourbook.chart;

import org.eclipse.jface.action.Action;

public class ActionPartPrevious extends Action {

	private Chart	fChart;

	public ActionPartPrevious(Chart chart) {

		fChart = chart;

		setText(Messages.Action_previous_month);
		setToolTipText(Messages.Action_previous_month_tooltip);
		setImageDescriptor(Activator.getImageDescriptor(Messages.Image_arrow_left));
		setDisabledImageDescriptor(Activator.getImageDescriptor(Messages.Image_arrow_left_disabled));
	}

	@Override
	public void run() {
		fChart.onExecutePartPrevious();
	}
}
