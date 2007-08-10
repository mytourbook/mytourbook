package net.tourbook.tour;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.menus.UIElement;

/**
 * Actionhandler for actions in a tour chart
 */
public abstract class TourChartActionHandler extends AbstractHandler implements IElementUpdater {

	String						fCommandId;
	TourChart					fTourChart;

	private boolean				fisEnabled;
	private boolean				fIsChecked;

//	private IHandlerActivation	fHandlerActivation;

	/**
	 * Update the UI for the action when the enablement state was changed
	 */
	public void fireHandlerChanged() {
		fireHandlerChanged(new HandlerEvent(this, true, false));
	}

	public String getCommandId() {
		return fCommandId;
	}

	public boolean isEnabled() {
		return fisEnabled;
	}

	/**
	 * Sets the internal check state for the action handler, to update the UI, the ICommandService
	 * refreshElements(*) method must be called
	 * 
	 * @param isEnabled
	 */
	public void setChecked(boolean isChecked) {

		if (fIsChecked != isChecked) {
			fIsChecked = isChecked;
		}
	}

	/**
	 * Sets the intenal enablement state for the action handler, to update the UI,
	 * <code>fireHandlerChanged</code> must be called
	 * 
	 * @param isEnabled
	 */
	public void setEnabled(boolean isEnabled) {

		if (fisEnabled != isEnabled) {
			fisEnabled = isEnabled;
		}
	}

	public void setHandlerActivation(IHandlerActivation handlerActivation) {

// handlerActivation is currently disabled because it's currently not used

//		fHandlerActivation = handlerActivation;
	}

	public void setTourChart(TourChart tourChart) {
		fTourChart = tourChart;
	}

	@SuppressWarnings("unchecked")
	public void updateElement(UIElement element, Map parameters) {

		// update check state in the UI
		element.setChecked(fIsChecked);
	}

}
