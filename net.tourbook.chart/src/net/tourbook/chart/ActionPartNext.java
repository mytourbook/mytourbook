package net.tourbook.chart;

import org.eclipse.jface.action.Action;

public class ActionPartNext extends Action {

	private Chart	fChart;

	public ActionPartNext(Chart chart) {

		fChart = chart;

		setText(Messages.Action_next_month);
		setToolTipText(Messages.Action_next_month_tooltip);
		setImageDescriptor(Activator.getImageDescriptor(Messages.Image_arrow_right));
		setDisabledImageDescriptor(Activator.getImageDescriptor(Messages.Image_arrow_right_disabled));
	}

	@Override
	public void run() {
		fChart.onExecutePartNext();
	}
}
