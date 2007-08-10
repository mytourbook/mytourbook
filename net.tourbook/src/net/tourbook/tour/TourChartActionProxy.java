package net.tourbook.tour;

import org.eclipse.jface.action.Action;

public class TourChartActionProxy {

	/**
	 * when the action is <code>null</code> an action handler is used
	 */
	private Action	fAction;

	private boolean	fUseInternalChartActionBar;

	private String	fCommandId;

	private boolean	fIsEnabled		= true;
	private boolean	fIsChecked;

	private boolean	fIsGraphAction	= false;

	public TourChartActionProxy(TourChart tourChart, String commandId, Action action) {

		fCommandId = commandId;
		fAction = action;

		fUseInternalChartActionBar = tourChart.isUseInternalActionBar();
	}

	public Action getAction() {
		return fAction;
	}

	public String getCommandId() {
		return fCommandId;
	}

	public boolean isChecked() {
		return fIsChecked;
	}

	public boolean isEnabled() {
		return fIsEnabled;
	}

	public boolean isGraphAction() {
		return fIsGraphAction;
	}

	public void setChecked(boolean isChecked) {

		// keep check state for this action 
		fIsChecked = isChecked;

		if (fUseInternalChartActionBar) {
			fAction.setChecked(isChecked);
		} else {
			final TourChartActionHandler actionHandler = TourChartActionHandlerManager.getInstance()
					.getActionHandler(fCommandId);
			actionHandler.setChecked(isChecked);
		}
	}

	public void setEnabled(boolean isEnabled) {

		// keep enabled state for this action 
		fIsEnabled = isEnabled;

		if (fUseInternalChartActionBar) {
			fAction.setEnabled(isEnabled);
		} else {
			final TourChartActionHandler actionHandler = TourChartActionHandlerManager.getInstance()
					.getActionHandler(fCommandId);
			actionHandler.setEnabled(isEnabled);
		}
	}

	public void setIsGraphAction() {
		fIsGraphAction = true;
	}

}
