package net.tourbook.tour;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.menus.UIElement;

/**
 * Actionhandler for actions in a tour chart
 */
public abstract class TCActionHandler extends AbstractHandler implements IElementUpdater {

	String			fCommandId;
	TourChart		fTourChart;

	private boolean	fisEnabled;
	private boolean	fIsChecked;

	/**
	 * Update the UI enablement state for an action handler
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
	 * Sets the internal check state for the action handler, to update the UI
	 * {@link ICommandService#refreshElements(String, Map)} method must be called
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
	 * {@link #fireHandlerChanged()} must be called
	 * 
	 * @param isEnabled
	 */
	public void setEnabled(boolean isEnabled) {

		if (fisEnabled != isEnabled) {
			fisEnabled = isEnabled;
		}
	}

	/**
	 * keep the handler activation token
	 * 
	 * @param handlerActivation
	 */
	public void setHandlerActivation(IHandlerActivation handlerActivation) {

	/*
	 * handlerActivation is currently disabled because it's currently not used
	 */

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
