package net.tourbook.tour;

import net.tourbook.ui.HandlerUtil;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.ToolItem;

public class ActionHandlerXAxisTime extends TourChartActionHandler {

	public ActionHandlerXAxisTime() {
		fCommandId = TourChart.COMMAND_ID_X_AXIS_TIME;
	}

	@Override
	public Object execute(ExecutionEvent execEvent) throws ExecutionException {

		ToolItem toolItem = HandlerUtil.getToolItem(execEvent);

		if (toolItem == null) {
			return null;
		}

		boolean isChecked = toolItem.getSelection();

		if (fTourChart.actionXAxisTime(isChecked) == false) {
			return null;
		}

		/*
		 * toggle time and distance buttons
		 */
		final TourChartActionHandlerManager handlerManager = TourChartActionHandlerManager.getInstance();
		String commandId;

		commandId = TourChart.COMMAND_ID_X_AXIS_TIME;
		fTourChart.fActionProxies.get(commandId).setChecked(isChecked);
		handlerManager.updateUICheckState(commandId);

		commandId = TourChart.COMMAND_ID_X_AXIS_DISTANCE;
		fTourChart.fActionProxies.get(commandId).setChecked(!isChecked);
		handlerManager.updateUICheckState(commandId);

		return null;
	}

}
