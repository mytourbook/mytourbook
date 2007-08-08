package net.tourbook.tour;

import org.eclipse.ui.handlers.IHandlerActivation;

public class GraphActionProxy {

	private GraphAction			fAction;

	private GraphActionHandler	fActionHandler;
	private IHandlerActivation	fHandlerActivation;

	private boolean				fUseInternalChartActionBar;

	private String				fCommandId;

	public GraphActionProxy(TourChart tourChart, int mapId, String label, String toolTip,
			String imageName, String commandId) {

		fUseInternalChartActionBar = tourChart.isUseInternalActionBar();
		fCommandId = commandId;

		if (fUseInternalChartActionBar) {
			fAction = new GraphAction(tourChart, mapId, label, toolTip, imageName);
		} else {
			fActionHandler = new GraphActionHandler(tourChart, mapId);
		}
	}

	public GraphAction getAction() {
		return fAction;
	}

	public GraphActionHandler getActionHandler() {
		return fActionHandler;
	}

	public void setChecked(boolean isChecked) {

		if (fUseInternalChartActionBar) {
			fAction.setChecked(isChecked);
		} else {
			fActionHandler.setChecked(isChecked);
		}
	}

	public void setEnabled(boolean isEnabled) {

		if (fUseInternalChartActionBar) {
			fAction.setEnabled(isEnabled);
		} else {
			fActionHandler.setEnabled(isEnabled);
		}
	}

	public void setHandlerActivation(IHandlerActivation handlerActivation) {
		fHandlerActivation = handlerActivation;
	}

	public IHandlerActivation getHandlerActivation() {
		return fHandlerActivation;
	}

	public String getCommandId() {
		return fCommandId;
	}

}
